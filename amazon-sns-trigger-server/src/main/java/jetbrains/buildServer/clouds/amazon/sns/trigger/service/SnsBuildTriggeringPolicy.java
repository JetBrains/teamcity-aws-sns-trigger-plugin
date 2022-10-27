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
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerException;
import jetbrains.buildServer.buildTriggers.PolledBuildTrigger;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.BuildQueueImpl;
import jetbrains.buildServer.util.TimeIntervalAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SnsBuildTriggeringPolicy extends PolledBuildTrigger {
  public static final String DEFAULT_BRANCH = "";
  private static final long TWO_MINUTES_IN_MS = 2 * 60 * 1000;
  private final AwsSnsTriggeringContext myTriggeringContext;
  private final TimeIntervalAction myTimeIntervalAction;

  public SnsBuildTriggeringPolicy(@NotNull AwsSnsTriggeringContext triggeringContext) {
    myTriggeringContext = triggeringContext;
    myTimeIntervalAction = new TimeIntervalAction(TWO_MINUTES_IN_MS);
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
            context,
            myTriggeringContext.getObjectMapper(),
            contextLogger
    );

    myTimeIntervalAction.executeCustomAction(context.getCustomDataStorage()::refresh);

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
    contextLogger.info("Initializing the Amazon SNS trigger state");
    CustomDataStorage cds = context.getCustomDataStorage();
    CustomDataStorage tempStorageWithPossibleSubscription = getInBetweenActivationStorage(context);

    if (tempStorageWithPossibleSubscription.getValues() != null) {
      cds.putValues(tempStorageWithPossibleSubscription.getValues());
    }

    AwsSnsBuildTriggerState state = new AwsSnsBuildTriggerState(
            context,
            myTriggeringContext.getObjectMapper(),
            contextLogger
    );
    state.resetMessagesMap();
    myTimeIntervalAction.resetLastActionTime();
  }

  @Override
  public void triggerDeactivated(@NotNull PolledTriggerContext context) throws BuildTriggerException {
    // cds will be destroyed with deactivation process
    CustomDataStorage cds = context.getCustomDataStorage();
    CustomDataStorage storage = getInBetweenActivationStorage(context);

    // we need to store our subscription data for possible future activation
    if (cds.getValues() != null) {
      storage.putValues(cds.getValues());
    }
  }

  @NotNull
  private CustomDataStorage getInBetweenActivationStorage(@NotNull PolledTriggerContext context) {
    BuildTriggerDescriptor trd = context.getTriggerDescriptor();
    String subscriptionStorageId = trd.getBuildTriggerService().getClass().getName() + "_" + trd.getId() + "_sub";
    return context.getBuildType().getCustomDataStorage(subscriptionStorageId);
  }

  @Nullable
  @Override
  public Map<String, String> getTriggerStateProperties(@NotNull PolledTriggerContext context) {
    final Map<String, String> properties = context.getTriggerDescriptor().getProperties();
    Map<String, String> result = new HashMap<>(2);
    result.put(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY, properties.get(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY));
    result.put(AwsSnsTriggerConstants.TRIGGER_BUILDTYPE_EXTERNAL_ID_PROPERTY_KEY, properties.get(AwsSnsTriggerConstants.TRIGGER_BUILDTYPE_EXTERNAL_ID_PROPERTY_KEY));
    return result;
  }

}
