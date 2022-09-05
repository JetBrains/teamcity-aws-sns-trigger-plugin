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

package jetbrains.buildServer.clouds.amazon.sns.trigger.utils;

import jetbrains.buildServer.clouds.amazon.sns.trigger.SnsMessageType;
import jetbrains.buildServer.clouds.amazon.sns.trigger.SnsNotificationDto;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.http.HttpApi;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.time.Instant;
import java.util.Map;

public class AwsSnsMessageDetailsHelper {

  public static boolean isSubscription(@NotNull Map<String, Object> payload) {
    String messageType = (String) payload.get(AwsSnsTriggerConstants.MESSAGE_TYPE_KEY);
    return SnsMessageType.asMessageType(messageType).equals(SnsMessageType.SUBSCRIBE);
  }

  public static boolean isUnsubscribe(@NotNull Map<String, Object> payload) {
    String messageType = (String) payload.get(AwsSnsTriggerConstants.MESSAGE_TYPE_KEY);
    return SnsMessageType.asMessageType(messageType).equals(SnsMessageType.UNSUBSRIBE);
  }

  public static boolean isNotification(@NotNull Map<String, Object> payload) {
    String messageType = (String) payload.get(AwsSnsTriggerConstants.MESSAGE_TYPE_KEY);
    return SnsMessageType.asMessageType(messageType).equals(SnsMessageType.NOTIFICATION);
  }

  public static boolean isValidSignature(@NotNull Map<String, Object> payload, @NotNull HttpApi myServerApi) {
    String messageType = (String) payload.get(AwsSnsTriggerConstants.MESSAGE_TYPE_KEY);
    return new AwsSnsSignatureVerification(SnsMessageType.asMessageType(messageType), payload, myServerApi).isValid();
  }

  @Nullable
  public static String subscribe(@NotNull Map<String, Object> payload,
                                 @NotNull HttpApi serverApi) throws IOException, JDOMException {
    String subscribeUrl = (String) payload.get(AwsSnsTriggerConstants.SUBSCRIBE_URL_KEY);
    HttpApi.Response response = serverApi.get(subscribeUrl);
    String body = response.getBody();

    try (Reader bodyReader = new StringReader(body)) {
      XPath xpath = XPath.newInstance(AwsSnsTriggerConstants.SUBSCRIBE_CONFIRMATION_ARN_XPATH);
      return xpath.valueOf(new SAXBuilder().build(bodyReader));
    }
  }

  @NotNull
  public static SnsNotificationDto convertToNotificationDto(HttpServletRequest request, @NotNull Map<String, Object> payload) {
    String subscriptionArn = request.getHeader(AwsSnsTriggerConstants.AWS_SUBSCRIPTION_ARN_HEADER);
    String messageId = request.getHeader(AwsSnsTriggerConstants.AWS_MESSAGE_ID_HEADER);
    String topic = request.getHeader(AwsSnsTriggerConstants.AWS_TOPIC_ARN_HEADER);
    String subject = (String) payload.get(AwsSnsTriggerConstants.NOTIFICATION_SUBJECT_KEY);
    String message = (String) payload.get(AwsSnsTriggerConstants.NOTIFICATION_MESSAGE_KEY);
    Instant timestamp = Instant.parse((String) payload.get(AwsSnsTriggerConstants.NOTIFICATION_TIMESTAMP_KEY));
    String unsubscribeUrl = (String) payload.get(AwsSnsTriggerConstants.UNSUBSCRIBE_URL_KEY);
    Map<String, Object> attributes = (Map<String, Object>) payload.get(AwsSnsTriggerConstants.NOTIFICATION_ATTRIBUTES_KEY);

    return new SnsNotificationDto(
            messageId,
            subscriptionArn,
            topic,
            subject,
            message,
            timestamp,
            unsubscribeUrl,
            attributes
    );
  }

}
