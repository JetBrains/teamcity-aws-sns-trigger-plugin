/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.trigger.controllers;

import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerService;
import jetbrains.buildServer.clouds.amazon.sns.trigger.SnsNotificationDto;
import jetbrains.buildServer.clouds.amazon.sns.trigger.errors.AwsSnsHttpEndpointException;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.AwsSnsHttpEndpointHelper;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.http.HttpApi;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.trigger.sns.SnsBuildTriggerService;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jetbrains.buildServer.clouds.amazon.sns.trigger.utils.AwsSnsHttpEndpointHelper.AWS_TOPIC_ARN_HEADER;
import static jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants.SNS_CONNECTION_CONTROLLER_URL;
import static jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants.SNS_CONNECTION_CONTROLLER_URL_PATTERN;
import static jetbrains.buildServer.serverSide.impl.PolledTriggerContextImpl.getCustomDataStorage;
import static jetbrains.buildServer.trigger.sns.SnsBuildTriggerService.*;

public class AwsSnsHttpEndpointController extends BaseAwsConnectionController {
  public static final String PATH = SNS_CONNECTION_CONTROLLER_URL;

  private final Pattern pathPattern = Pattern.compile(SNS_CONNECTION_CONTROLLER_URL_PATTERN);
  private final ProjectManager myProjectManager;
  private final HttpApi myServerApi;

  public AwsSnsHttpEndpointController(@NotNull SBuildServer server,
                                      @NotNull final WebControllerManager webControllerManager,
                                      @NotNull final ProjectManager projectManager,
                                      @NotNull final HttpApi serverApi,
                                      @NotNull final AuthorizationInterceptor authInterceptor
  ) {
    super(server);
    myProjectManager = projectManager;
    myServerApi = serverApi;
    webControllerManager.registerController(PATH, this);
    authInterceptor.addPathNotRequiringAuth(PATH);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
    final ActionErrors errors = new ActionErrors();

    try {
      final String fullPath = request.getServletPath() + request.getPathInfo();
      Matcher m = pathPattern.matcher(fullPath);
      if (!m.find()) {
        throw new AwsSnsHttpEndpointException("Invalid or Unsupported URL is given: " + fullPath);
      }

      String projectId = m.group(1);
      String buildTypeId = m.group(2);
      String triggerUuid = m.group(3);

      if (projectId == null || projectId.trim().isEmpty()) {
        throw new AwsSnsHttpEndpointException("No ProjectId given in the path");
      }

      if (buildTypeId == null || buildTypeId.trim().isEmpty()) {
        throw new AwsSnsHttpEndpointException("No BuildTypeId given in the path");
      }

      if (triggerUuid == null || triggerUuid.trim().isEmpty()) {
        throw new AwsSnsHttpEndpointException("No Trigger UUID given in the path");
      }

      projectId = projectId.trim();
      final String finalBuildTypeId = buildTypeId.trim();

      SProject project = myProjectManager.findProjectByExternalId(projectId);
      if (project == null) {
        throw new AwsSnsHttpEndpointException("Couldn't find the project with id: " + projectId);
      }

      SBuildType buildType = project.findBuildTypeByExternalId(finalBuildTypeId);
      if (buildType == null) {
        throw new AwsSnsHttpEndpointException("Couldn't find build type with id: " + finalBuildTypeId);
      }

      final String finalTriggerUuid = triggerUuid.trim();
      BuildTriggerDescriptor buildTrigger = buildType.getBuildTriggersCollection().stream()
              .filter(btd -> btd.getType().equals(TRIGGER_NAME) &&
                      btd.getProperties().get(TRIGGER_UUID_KEY) != null &&
                      btd.getProperties().get(TRIGGER_UUID_KEY).equals(finalTriggerUuid))
              .findFirst()
              .orElseThrow(() -> new AwsSnsHttpEndpointException("There are no suitable trigger in the build: " + finalBuildTypeId));

      // OK! we've defined project and the buildType with necessary buildTrigger
      // lets get into request details and find out what kind of request is it?
      if (isPost(request)) {
        CustomDataStorage cds = getCustomDataStorage(buildType, buildTrigger);
        HashMap<String, Object> payload = readJson(request);

        if (payload != null && AwsSnsHttpEndpointHelper.isValidSignature(payload)) {
          if (AwsSnsHttpEndpointHelper.isSubscription(payload)) {
            String arn = AwsSnsHttpEndpointHelper.subscribe(payload, myServerApi);
            cds.putValue(TRIGGER_CURRENT_SUBSCRIPTION_ARN, arn);
            cds.putValue(TRIGGER_CURRENT_TOPIC_ARN, request.getHeader(AWS_TOPIC_ARN_HEADER));
          } else if (AwsSnsHttpEndpointHelper.isUnsubscribe(payload)) {
            cds.putValue(TRIGGER_CURRENT_SUBSCRIPTION_ARN, null);
          } else if (AwsSnsHttpEndpointHelper.isNotification(payload)) {
            SnsNotificationDto dto = AwsSnsHttpEndpointHelper.convertToNotificaitonDto(request, payload);
            String expectedArn = cds.getValue(TRIGGER_CURRENT_SUBSCRIPTION_ARN);
            String currentArn = dto.getSubscriptionArn();
            String unsubscribeUrl = dto.getUnsubscribeUrl();

            if (expectedArn == null || !expectedArn.equals(currentArn)) {
              throw new AwsSnsHttpEndpointException("Trigger " + buildTrigger.getTriggerName() + " isn't subscribed to topic " + dto.getTopic());
            }

            cds.putValue(TRIGGER_CURRENT_UNSUBSCRIBE_URL, unsubscribeUrl);

            BuildTriggerService bts = buildTrigger.getBuildTriggerService();
            if (bts instanceof SnsBuildTriggerService) {
              SnsBuildTriggerService btService = (SnsBuildTriggerService) bts;
              btService.registerMessage(dto, cds);
            }
          }
        }
      }
      // otherwise just ignore this message
    } catch (Exception error) {
      errors.addError("error_snsEndpointResolve", error.getMessage());
      writeAsJson(errors, response, HttpServletResponse.SC_BAD_REQUEST);
    }

    // this will return HTTP_CODE 200
    return null;
  }
}
