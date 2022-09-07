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

package jetbrains.buildServer.clouds.amazon.sns.trigger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerService;
import jetbrains.buildServer.buildTriggers.BuildTriggeringPolicy;
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static jetbrains.buildServer.serverSide.impl.PolledTriggerContextImpl.getCustomDataStorage;

public class SnsBuildTriggerService extends BuildTriggerService {
  public static final String TRIGGER_NAME = "awsSnsTrigger";
  public static final String TRIGGER_PRETTY_NAME = "AWS SNS Trigger";
  public static final String EDIT_PARAMS_URL = "editAwsSnsTrigger.jsp";

  private final String myEditParametersUrl;
  private final AwsSnsTriggeringContext myTriggeringContext;
  private final ObjectMapper myObjectMapper;

  public SnsBuildTriggerService(@NotNull final ExtensionHolder extensionHolder,
                                @NotNull final PluginDescriptor descriptor,
                                @NotNull final AwsSnsTriggeringContext triggeringContext
  ) {
    myEditParametersUrl = descriptor.getPluginResourcesPath(EDIT_PARAMS_URL);
    extensionHolder.registerExtension(BuildTriggerService.class, getClass().getName(), this);

    myTriggeringContext = triggeringContext;
    myObjectMapper = triggeringContext.getObjectMapper();
  }

  @NotNull
  @Override
  public String getName() {
    return TRIGGER_NAME;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return TRIGGER_PRETTY_NAME;
  }

  @NotNull
  @Override
  public String describeTrigger(@NotNull BuildTriggerDescriptor trigger) {
    StringBuilder sb = new StringBuilder();
    Map<String, String> properties = trigger.getProperties();

    String btExternalId = properties.get(AwsSnsTriggerConstants.TRIGGER_BUILDTYPE_EXTERNAL_ID_PROPERTY_KEY);
    SBuildType buildType = myTriggeringContext.getProjectManager().findBuildTypeByExternalId(btExternalId);

    sb.append("Trigger UUID: ").append(properties.get(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY));
    sb.append("\n");
    sb.append("Trigger URL: ")
            .append("\n")
            .append(properties.get(AwsSnsTriggerConstants.TRIGGER_ENDPOINT_URL_PROPERTY_KEY));

    if (buildType != null) {
      CustomDataStorage cds = getCustomDataStorage(buildType, trigger);
      String topicArn = cds.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_TOPIC_ARN);
      String topicSubscriptionArn = cds.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_SUBSCRIPTION_ARN);

      if (topicArn != null) {
        sb.append("\n")
                .append("Topic ARN:")
                .append("\n")
                .append(topicArn);
      }

      if (topicSubscriptionArn != null) {
        sb.append("\n")
                .append("Subscription ARN:")
                .append("\n")
                .append(topicSubscriptionArn);
      } else {
        sb.append("\n").append("Pending subscription...");
      }
    }

    return sb.toString();
  }

  @Nullable
  @Override
  public PropertiesProcessor getTriggerPropertiesProcessor() {
    return null;
  }

  @Nullable
  @Override
  public String getEditParametersUrl() {
    return myEditParametersUrl;
  }

  @Nullable
  @Override
  public Map<String, String> getDefaultTriggerProperties() {
    Map<String, String> defaults = new HashMap<>();
    defaults.put(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY, UUID.randomUUID().toString());
    defaults.put("urlPathPart", AwsSnsTriggerConstants.SNS_CONNECTION_CONTROLLER_URL);
    defaults.put("rootUrl", myTriggeringContext.getMyWebLinks().getRootUrlByProjectExternalId(null));
    return defaults;
  }

  @NotNull
  @Override
  public BuildTriggeringPolicy getBuildTriggeringPolicy() {
    return new AllBranchesAwsSnsBuildTrigger(myTriggeringContext);
  }

  @Override
  public boolean isMultipleTriggersPerBuildTypeAllowed() {
    return true;
  }

  @Override
  public boolean supportsBuildCustomization() {
    return true;
  }

  public void registerMessage(@NotNull SnsNotificationDto notificationDto, @NotNull CustomDataStorage cds) throws JsonProcessingException {
    if (!StringUtil.isTrue(cds.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_IS_ACTIVE))) return;

    String messagesMapAsString = cds.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES);

    HashMap<String, SnsNotificationDto> messagesMap;
    if (messagesMapAsString == null) {
      messagesMap = new HashMap<>();
    } else {
      messagesMap = myObjectMapper.readValue(messagesMapAsString, new TypeReference<HashMap<String, SnsNotificationDto>>() {
      });
    }

    messagesMap.put(notificationDto.getMessageId(), notificationDto);
    String updatedMessagesMapAsString = myObjectMapper.writeValueAsString(messagesMap);

    cds.putValue(AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES, updatedMessagesMapAsString);
    cds.flush();
  }
}
