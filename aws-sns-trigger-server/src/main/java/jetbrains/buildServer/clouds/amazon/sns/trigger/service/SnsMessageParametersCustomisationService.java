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

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.parameters.ProjectParametersProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnsMessageParametersCustomisationService implements ProjectParametersProvider {
    private final Pattern snsParametersPattern = Pattern.compile(AwsSnsTriggerConstants.SNS_CUSTOM_PARAMETERS_PATTERN, Pattern.MULTILINE);

    public SnsMessageParametersCustomisationService(@NotNull final ExtensionHolder extensionHolder) {
        extensionHolder.registerExtension(ProjectParametersProvider.class, getClass().getName(), this);
    }

    @NotNull
    @Override
    public Collection<String> getAvailableParameters(@NotNull SProject sProject) {
        return Arrays.asList(
                AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER,
                AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER,
                AwsSnsTriggerConstants.SNS_MESSAGE_ATTRIBUTES_PARAMETER_PLACEHOLDER
        );
    }

    @NotNull
    public String replacePlaceholdersWithValues(@NotNull String value, @NotNull SnsNotificationDto snsMessage) {
        Matcher m = snsParametersPattern.matcher(value);
        String result = value;

        while (m.find()) {
            PlaceholderData placeholderData = safeGroup(m);
            String placeholder = placeholderData.getPlaceholder();
            String key = placeholderData.getKey();

            if (placeholder != null && key != null) {
                switch (key) {
                    case AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER:
                        String subject = Objects.toString(snsMessage.getSubject(), "");
                        result = result.replace(placeholder, subject);
                        break;
                    case AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER:
                        String body = Objects.toString(snsMessage.getMessage(), "");
                        result = result.replace(placeholder, body);
                        break;
                    default: // if not 'subject' or 'body' then it is an 'attributes'
                        Object attributeValueObj = null;
                        String attributeName = placeholderData.getAttributeName();
                        // check that attribute name defined properly
                        if (attributeName != null) {
                            // attribute name will be prepended with . character
                            // keep this in mind
                            Map<String, Object> messageAttributes = snsMessage.getAttributes();
                            if (messageAttributes != null) {
                                attributeValueObj = snsMessage.getAttributes().keySet()
                                        .stream()
                                        .filter(k -> ("." + k).equals(attributeName))
                                        .findFirst()
                                        .map(messageAttributes::get)
                                        .map(v -> ((Map<String, Object>) v).get("Value"))
                                        .orElse(null);
                            }
                        }

                        String attributeValueStr = Objects.toString(attributeValueObj, "");
                        result = result.replace(placeholder, attributeValueStr);
                        break;
                }
            }
        }

        return result;
    }

    @NotNull
    private PlaceholderData safeGroup(@NotNull Matcher matcher) {
        String placeholder = null;
        String key = null;
        String attributeName = null;
        try {
            placeholder = matcher.group();
            key = matcher.group(1);
            attributeName = matcher.group(3);
        } catch (Exception ignored) {
            // ignore
        }

        return new PlaceholderData(placeholder, key, attributeName);
    }

    private static class PlaceholderData {

        private final String myPlaceholder;
        private final String myKey;
        private final String myAttributeName;

        public PlaceholderData(@Nullable String placeholder, @Nullable String key, @Nullable String attributeName) {
            myPlaceholder = placeholder;
            myKey = key;
            myAttributeName = attributeName;
        }

        @Nullable
        public String getPlaceholder() {
            return myPlaceholder;
        }

        @Nullable
        public String getKey() {
            return myKey;
        }

        @Nullable
        public String getAttributeName() {
            return myAttributeName;
        }
    }
}
