

package jetbrains.buildServer.clouds.amazon.sns.trigger.service;

import com.intellij.openapi.diagnostic.Logger;
import java.util.*;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerException;
import jetbrains.buildServer.buildTriggers.PolledBuildTrigger;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.CustomDataStorageWrapper;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.serverSide.BuildCustomizer;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.TriggeredByBuilder;
import jetbrains.buildServer.serverSide.impl.BuildQueueImpl;
import jetbrains.buildServer.util.TimeIntervalAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SnsBuildTriggeringPolicy extends PolledBuildTrigger {
  public static final String DEFAULT_BRANCH = "";
  private static final long TWO_MINUTES_IN_MS = 2 * 60 * 1000L;
  private final AwsSnsTriggeringContext myTriggeringContext;
  private final TimeIntervalAction myTimeIntervalAction;

  public SnsBuildTriggeringPolicy(@NotNull AwsSnsTriggeringContext triggeringContext) {
    myTriggeringContext = triggeringContext;
    myTimeIntervalAction = new TimeIntervalAction(TWO_MINUTES_IN_MS);
  }

  private Map<String, String> customizeWithSnsMessageData(@NotNull SnsNotificationDto latestSnsMessage) {
    Map<String, String> result = new HashMap<>();
    if (latestSnsMessage.getSubject() != null) {
      result.put(AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER, latestSnsMessage.getSubject());
    }

    if (latestSnsMessage.getMessage() != null) {
      result.put(AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER, latestSnsMessage.getMessage());
    }

    if (latestSnsMessage.getAttributes() != null) {
      latestSnsMessage.getAttributes().forEach((key, value) -> {
        String stringValue = (String) ((Map<String, Object>) value).get("Value");
        result.put(AwsSnsTriggerConstants.SNS_MESSAGE_ATTRIBUTES_PARAMETER_PLACEHOLDER + key, stringValue);
      });
    }

    return result;
  }

  private BuildPromotionEx createBuildPromotion(PolledTriggerContext context, @NotNull SnsNotificationDto latestSnsMessage) {
    BuildCustomizer buildCustomizer = context.createBuildCustomizer(null);
    buildCustomizer.setDesiredBranchName(DEFAULT_BRANCH);
    // put SNS data to build promotion custom parameters
    buildCustomizer.addParametersIfAbsent(customizeWithSnsMessageData(latestSnsMessage));
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

    // here it is safe to use context.getCustomDataStorage() without wrapper
    // because we are not going to put any data
    myTimeIntervalAction.executeCustomAction(context.getCustomDataStorage()::refresh);

    if (!state.hasNewNotifications()) {
      contextLogger.debug("No new SNS messages registered");
      return;
    }

    Map<String, SnsNotificationDto> registeredMessages = state.getRegisteredMessages();
    SnsNotificationDto latestSnsMessage = getLatest(registeredMessages);
    Set<String> registeredMessagesIds = registeredMessages.keySet();

    BuildPromotionEx buildPromotion = createBuildPromotion(context, latestSnsMessage);

    TriggeredByBuilder builder = new TriggeredByBuilder();
    builder.addParameter(TriggeredByBuilder.TYPE_PARAM_NAME, "sns");
    builder.addParameter(TriggeredByBuilder.TRIGGER_ID_PARAM_NAME, context.getTriggerDescriptor().getId());
    builder.addParameter(BuildQueueImpl.TRIGGERED_BY_QUEUE_OPTIMIZATION_ENABLED_PARAM, "false");

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
    CustomDataStorageWrapper cds = new CustomDataStorageWrapper(context.getCustomDataStorage());
    CustomDataStorageWrapper tempStorageWithPossibleSubscription = getInBetweenActivationStorage(context);

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
    CustomDataStorageWrapper cds = new CustomDataStorageWrapper(context.getCustomDataStorage());
    CustomDataStorageWrapper storage = getInBetweenActivationStorage(context);

    // we need to store our subscription data for possible future activation
    if (cds.getValues() != null) {
      storage.putValues(cds.getValues());
    }
  }

  @NotNull
  private CustomDataStorageWrapper getInBetweenActivationStorage(@NotNull PolledTriggerContext context) {
    BuildTriggerDescriptor trd = context.getTriggerDescriptor();
    String subscriptionStorageId = trd.getBuildTriggerService().getClass().getName() + "_" + trd.getId() + "_sub";
    return new CustomDataStorageWrapper(context.getBuildType().getCustomDataStorage(subscriptionStorageId));
  }

  @Nullable
  @Override
  public Map<String, String> getTriggerStateProperties(@NotNull PolledTriggerContext context) {
    final Map<String, String> properties = context.getTriggerDescriptor().getProperties();
    return Collections.singletonMap(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY, properties.get(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY));
  }

}
