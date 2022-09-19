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

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.buildTriggers.BuildTriggerException;
import jetbrains.buildServer.buildTriggers.PolledBuildTrigger;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.serverSide.BuildCustomizer;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.TriggeredByBuilder;
import jetbrains.buildServer.serverSide.impl.BuildQueueImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public class SnsBuildTriggeringPolicy extends PolledBuildTrigger {
  public static final String DEFAULT_BRANCH = "";
  private final AwsSnsTriggeringContext myTriggeringContext;

  public SnsBuildTriggeringPolicy(@NotNull AwsSnsTriggeringContext triggeringContext) {
    myTriggeringContext = triggeringContext;
  }

  private static void customizeWithSnsMessageData(@NotNull SnsNotificationDto latestSnsMessage, @NotNull TriggeredByBuilder builder) {
    if (latestSnsMessage.getSubject() != null) {
      builder.addParameter(AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER_KEY, latestSnsMessage.getSubject());
    }

    if (latestSnsMessage.getMessage() != null) {
      builder.addParameter(AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER_KEY, latestSnsMessage.getMessage());
    }

    if (latestSnsMessage.getAttributes() != null) {
      latestSnsMessage.getAttributes().forEach((key, value) -> {
        String stringValue = (String) ((Map<String, Object>) value).get("Value");
        builder.addParameter(AwsSnsTriggerConstants.SNS_MESSAGE_ATTRIBUTES_PARAMETER_PLACEHOLDER_KEY_PREFIX + key, stringValue);
      });
    }
  }

  private BuildPromotionEx createBuildPromotion(PolledTriggerContext context) {
    BuildCustomizer buildCustomizer = context.createBuildCustomizer(null);
    buildCustomizer.setDesiredBranchName(DEFAULT_BRANCH);
    return (BuildPromotionEx) buildCustomizer.createPromotion();
  }

  @Override
  public void triggerBuild(@NotNull PolledTriggerContext context) throws BuildTriggerException {
    final Logger contextLogger = context.getLogger();

    AwsSnsBuildTriggerState state = new AwsSnsBuildTriggerState(
            context.getCustomDataStorage(),
            myTriggeringContext.getObjectMapper(),
            contextLogger
    );

    if (!state.hasNewNotifications()) {
      contextLogger.debug("No new SNS messages registered");
      return;
    }

    Map<String, SnsNotificationDto> registeredMessages = state.getRegisteredMessages();
    SnsNotificationDto latestSnsMessage = getLatest(registeredMessages);
    Set<String> registeredMessagesIds = registeredMessages.keySet();

    BuildPromotionEx buildPromotion = createBuildPromotion(context);

    TriggeredByBuilder builder = new TriggeredByBuilder();
    builder.addParameter(TriggeredByBuilder.TYPE_PARAM_NAME, "sns");
    builder.addParameter(TriggeredByBuilder.TRIGGER_ID_PARAM_NAME, context.getTriggerDescriptor().getId());
    builder.addParameter(BuildQueueImpl.TRIGGERED_BY_QUEUE_OPTIMIZATION_ENABLED_PARAM, "false");
    // put SNS data to the triggered by properties
    customizeWithSnsMessageData(latestSnsMessage, builder);

    ((BuildTypeEx) context.getBuildType()).addToQueue(buildPromotion, builder.toString());

    state.persist(registeredMessagesIds);
  }

  private SnsNotificationDto getLatest(Map<String, SnsNotificationDto> registeredMessages) {
    return registeredMessages.values().stream().max(Comparator.comparing(SnsNotificationDto::getTimestamp))
            .orElseThrow(() -> new IllegalStateException("Comparator returned null for list of messages. This should never happen"));
  }

  @Override
  public void triggerActivated(@NotNull PolledTriggerContext context) throws BuildTriggerException {
    final Logger contextLogger = context.getLogger();
    contextLogger.info("Initializing the AWS SNS trigger state");
    AwsSnsBuildTriggerState state = new AwsSnsBuildTriggerState(
            context.getCustomDataStorage(),
            myTriggeringContext.getObjectMapper(),
            contextLogger
    );
    state.resetMessagesMap();
  }

  @Nullable
  @Override
  public Map<String, String> getTriggerStateProperties(@NotNull PolledTriggerContext context) {
    return null;
  }

}
