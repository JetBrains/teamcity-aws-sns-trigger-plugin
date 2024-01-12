

package jetbrains.buildServer.clouds.amazon.sns.trigger.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor;
import jetbrains.buildServer.buildTriggers.BuildTriggerService;
import jetbrains.buildServer.buildTriggers.BuildTriggeringPolicy;
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto;
import jetbrains.buildServer.clouds.amazon.sns.trigger.errors.AwsSnsHttpEndpointException;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SnsBuildTriggerService extends BuildTriggerService {
  public static final String TRIGGER_NAME = "awsSnsTrigger";
  public static final String TRIGGER_PRETTY_NAME = "Amazon SNS Trigger";
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
    String triggerId = properties.get(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY);
    String triggerName = properties.get(AwsSnsTriggerConstants.TRIGGER_NAME_PROPERTY_KEY);

    if (Strings.isBlank(triggerName)) {
      triggerName = triggerId;
    }

    sb.append("Display Name: ").append(triggerName);
    sb.append(System.lineSeparator());
    sb.append("Amazon SNS Trigger ID: ").append(triggerId);

    return sb.toString();
  }

  @Nullable
  @Override
  public PropertiesProcessor getTriggerPropertiesProcessor() {
    return properties -> {
      ArrayList<InvalidProperty> result = new ArrayList<>(2);
      String triggerId = properties.get(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY);

      if (Strings.isBlank(triggerId)) {
        result.add(new InvalidProperty(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY, "is mandatory"));
      }

      return result;
    };
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
    defaults.put(AwsSnsTriggerConstants.TRIGGER_NAME_PROPERTY_KEY, getDisplayName());
    return defaults;
  }

  @NotNull
  @Override
  public BuildTriggeringPolicy getBuildTriggeringPolicy() {
    return new SnsBuildTriggeringPolicy(myTriggeringContext);
  }

  @Override
  public boolean isMultipleTriggersPerBuildTypeAllowed() {
    return true;
  }

  @Override
  public boolean supportsBuildCustomization() {
    return true;
  }

  public void registerMessage(@NotNull SnsNotificationDto notificationDto, @NotNull CustomDataStorage cds) throws AwsSnsHttpEndpointException {
    String messagesMapAsString = cds.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES);

    try {
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
    } catch (Exception e) {
      throw new AwsSnsHttpEndpointException("Can't register incoming notification", e);
    }

    cds.flush();
  }
}