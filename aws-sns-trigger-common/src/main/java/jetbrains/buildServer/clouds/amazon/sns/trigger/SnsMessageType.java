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

package jetbrains.buildServer.clouds.amazon.sns.trigger;

import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public enum SnsMessageType {
    UNDEFINED(AwsSnsTriggerConstants.SNS_MT_UNDEFINED_STR),
    SUBSCRIBE(AwsSnsTriggerConstants.SNS_MT_SUBSCRIPTION_STR),
    UNSUBSRIBE(AwsSnsTriggerConstants.SNS_MT_UNSUBSCRIPTION_STR),
    NOTIFICATION(AwsSnsTriggerConstants.SNS_MT_NOTIFICATION_STR);

    private final String typeString;

    SnsMessageType(String typeString) {
        this.typeString = typeString;
    }

    @NotNull
    public static SnsMessageType asMessageType(@Nullable String messageType) {
        if (messageType == null || messageType.trim().isEmpty()) {
            return UNDEFINED;
        }

        String innerMessageType = messageType.trim();

        switch (innerMessageType) {
            case AwsSnsTriggerConstants.SNS_MT_SUBSCRIPTION_STR:
                return SUBSCRIBE;
            case AwsSnsTriggerConstants.SNS_MT_UNSUBSCRIPTION_STR:
                return UNSUBSRIBE;
            case AwsSnsTriggerConstants.SNS_MT_NOTIFICATION_STR:
                return NOTIFICATION;
            default:
                return UNDEFINED;
        }
    }

    @NotNull
    @Override
    public String toString() {
        return typeString;
    }

}
