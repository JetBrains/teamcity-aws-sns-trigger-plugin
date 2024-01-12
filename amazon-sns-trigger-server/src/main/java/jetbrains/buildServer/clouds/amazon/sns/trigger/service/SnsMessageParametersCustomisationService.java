

package jetbrains.buildServer.clouds.amazon.sns.trigger.service;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.parameters.BuildParametersProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SnsMessageParametersCustomisationService implements BuildParametersProvider {

  public SnsMessageParametersCustomisationService(@NotNull final ExtensionHolder extensionHolder) {
    extensionHolder.registerExtension(BuildParametersProvider.class, getClass().getName(), this);
  }

  @NotNull
  @Override
  public Map<String, String> getParameters(@NotNull SBuild build, boolean emulationMode) {
    final Map<String, String> customParameters = build.getBuildOwnParameters();
    Map<String, String> result = new HashMap<>();

    if (customParameters.containsKey(AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER) ||
            emulationMode) {
      result.put(AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER,
              getStringValue(customParameters,
                      AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER));
    }

    if (customParameters.containsKey(AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER) ||
            emulationMode) {
      result.put(AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER,
              getStringValue(customParameters,
                      AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER));
    }

    // SNS message attributes
    customParameters.keySet().stream()
            .filter(it -> it.startsWith(
                    AwsSnsTriggerConstants.SNS_MESSAGE_ATTRIBUTES_PARAMETER_PLACEHOLDER))
            .forEach(key -> {
              String value = getStringValue(customParameters, key);
              result.put(key, value);
            });
    return result;
  }

  private String getStringValue(@NotNull Map<String, String> params, @NotNull String key) {
    return params.getOrDefault(key, "???");
  }

  @NotNull
  @Override
  public Collection<String> getParametersAvailableOnAgent(@NotNull SBuild build) {
    return Arrays.asList(
      AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER,
      AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER
    );
  }

}