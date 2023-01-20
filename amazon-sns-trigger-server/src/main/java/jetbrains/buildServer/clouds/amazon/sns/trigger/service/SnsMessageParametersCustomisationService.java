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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.parameters.BuildParametersProvider;
import org.jetbrains.annotations.NotNull;

public class SnsMessageParametersCustomisationService implements BuildParametersProvider {

  public SnsMessageParametersCustomisationService(@NotNull final ExtensionHolder extensionHolder) {
    extensionHolder.registerExtension(BuildParametersProvider.class, getClass().getName(), this);
  }

  @NotNull
  @Override
  public Map<String, String> getParameters(@NotNull SBuild build, boolean emulationMode) {
    final Map<String, String> customParameters = build.getBuildPromotion().getCustomParameters();
    Map<String, String> result = new HashMap<>();

    if (customParameters.containsKey(AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER_KEY) ||
        emulationMode) {
      result.put(AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER,
                 getStringValue(customParameters,
                                AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER_KEY));
    }

    if (customParameters.containsKey(AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER_KEY) ||
        emulationMode) {
      result.put(AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER,
                 getStringValue(customParameters,
                                AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER_KEY));
    }

    // SNS message attributes
    customParameters.keySet().stream()
                    .filter(it -> it.startsWith(
                      AwsSnsTriggerConstants.SNS_MESSAGE_ATTRIBUTES_PARAMETER_PLACEHOLDER_KEY_PREFIX))
                    .forEach(key -> {
                      String value = getStringValue(customParameters, key);
                      String newKey = key.replace(
                        AwsSnsTriggerConstants.SNS_MESSAGE_ATTRIBUTES_PARAMETER_PLACEHOLDER_KEY_PREFIX,
                        AwsSnsTriggerConstants.SNS_MESSAGE_ATTRIBUTES_PARAMETER_PLACEHOLDER
                      );
                      result.put(newKey, value);
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
