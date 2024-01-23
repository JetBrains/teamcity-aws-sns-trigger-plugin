

package jetbrains.buildServer.clouds.amazon.sns.trigger.controllers;

import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerService;
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto;
import jetbrains.buildServer.clouds.amazon.sns.trigger.errors.AwsSnsHttpEndpointException;
import jetbrains.buildServer.clouds.amazon.sns.trigger.service.SnsBuildTriggerService;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.AwsSnsMessageDetailsHelper;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.http.HttpApi;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jetbrains.buildServer.clouds.amazon.sns.trigger.service.SnsBuildTriggerService.TRIGGER_NAME;
import static jetbrains.buildServer.serverSide.impl.PolledTriggerContextImpl.getCustomDataStorage;

public class AwsSnsHttpEndpointController extends BaseAwsConnectionController {
  public static final String PATH = AwsSnsTriggerConstants.SNS_CONNECTION_CONTROLLER_URL;
  private final Pattern pathPattern = Pattern.compile(AwsSnsTriggerConstants.SNS_CONNECTION_CONTROLLER_URL_PATTERN);
  private final ProjectManager myProjectManager;
  private final HttpApi myServerApi;
  private final SecurityContextEx mySecurityContext;

  public AwsSnsHttpEndpointController(@NotNull SBuildServer server,
                                      @NotNull final WebControllerManager webControllerManager,
                                      @NotNull final ProjectManager projectManager,
                                      @NotNull final HttpApi serverApi,
                                      @NotNull final AuthorizationInterceptor authInterceptor,
                                      @NotNull final SecurityContextEx securityContext
  ) {
    super(server);
    myProjectManager = projectManager;
    myServerApi = serverApi;
    mySecurityContext = securityContext;
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

      String finalProjectId = projectId.trim();
      final String finalBuildTypeId = buildTypeId.trim();

      SProject project = mySecurityContext.runAsSystemUnchecked(() -> myProjectManager.findProjectByExternalId(finalProjectId));
      if (project == null) {
        throw new AwsSnsHttpEndpointException("Couldn't find the project with id: " + finalProjectId);
      }

      SBuildType buildType = mySecurityContext.runAsSystemUnchecked(() -> project.findBuildTypeByExternalId(finalBuildTypeId));
      if (buildType == null) {
        throw new AwsSnsHttpEndpointException("Couldn't find build type with id: " + finalBuildTypeId);
      }

      final String finalTriggerUuid = triggerUuid.trim();
      BuildTriggerDescriptor buildTrigger = buildType.getBuildTriggersCollection().stream()
              .filter(btd -> btd.getType().equals(TRIGGER_NAME) &&
                      btd.getProperties().get(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY) != null &&
                      btd.getProperties().get(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY).equals(finalTriggerUuid))
              .findFirst()
              .orElseThrow(() -> new AwsSnsHttpEndpointException("There are no suitable trigger in the build: " + finalBuildTypeId));

      // OK! we've defined project and the buildType with necessary buildTrigger
      // lets get into request details and find out what kind of request is it?
      if (isPost(request)) {
        doPost(request, buildType, buildTrigger);
      }
      // otherwise just ignore this message
    } catch (Exception error) {
      errors.addError("error_snsEndpointResolve", error.getMessage());
      writeErrorsAsJson(errors, response);
    }

    // this will return HTTP_CODE 200
    return null;
  }

  private void doPost(
          @NotNull final HttpServletRequest request,
          @NotNull final SBuildType buildType,
          @NotNull final BuildTriggerDescriptor buildTrigger
  ) throws AwsSnsHttpEndpointException {
    CustomDataStorage cds = getCustomDataStorage(buildType, buildTrigger);
    HashMap<String, Object> payload = readJson(request);

    if (payload != null && AwsSnsMessageDetailsHelper.isValidSignature(payload, myServerApi)) {
      if (AwsSnsMessageDetailsHelper.isSubscription(payload)) {
        handleSubscription(request.getHeader(AwsSnsTriggerConstants.AWS_TOPIC_ARN_HEADER), cds, payload);
      } else if (AwsSnsMessageDetailsHelper.isUnsubscribe(payload)) {
        handleUnsubscribe(cds);
      } else if (AwsSnsMessageDetailsHelper.isNotification(payload)) {
        handleNotification(buildTrigger, cds, AwsSnsMessageDetailsHelper.convertToNotificationDto(request, payload));
      }
    }
  }

  private void handleSubscription(
          @NotNull String currentTopicArn,
          @NotNull CustomDataStorage cds,
          @NotNull final Map<String, Object> payload
  ) throws AwsSnsHttpEndpointException {
    String arn = AwsSnsMessageDetailsHelper.subscribe(payload, myServerApi);
    cds.putValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_SUBSCRIPTION_ARN, arn);
    cds.putValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_TOPIC_ARN, currentTopicArn);
  }

  private void handleNotification(
          @NotNull BuildTriggerDescriptor buildTrigger,
          @NotNull CustomDataStorage cds,
          @NotNull SnsNotificationDto dto
  ) throws AwsSnsHttpEndpointException {
    String expectedArn = cds.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_SUBSCRIPTION_ARN);
    String currentArn = dto.getSubscriptionArn();
    String unsubscribeUrl = dto.getUnsubscribeUrl();

    if (expectedArn == null || !expectedArn.equals(currentArn)) {
      throw new AwsSnsHttpEndpointException("Trigger " + buildTrigger.getTriggerName() + " isn't subscribed to topic " + dto.getTopic());
    }

    cds.putValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_UNSUBSCRIBE_URL, unsubscribeUrl);

    BuildTriggerService bts = buildTrigger.getBuildTriggerService();
    if (bts instanceof SnsBuildTriggerService) {
      SnsBuildTriggerService btService = (SnsBuildTriggerService) bts;
      btService.registerMessage(dto, cds);
    }
  }

  private void handleUnsubscribe(@NotNull CustomDataStorage cds) {
    // cleanup
    cds.putValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_SUBSCRIPTION_ARN, null);
    cds.putValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_TOPIC_ARN, null);
    cds.putValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_UNSUBSCRIBE_URL, null);
    cds.putValue(AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES, null);
  }
}