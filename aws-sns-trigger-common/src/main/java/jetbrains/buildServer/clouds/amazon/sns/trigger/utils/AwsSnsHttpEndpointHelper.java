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

import jetbrains.buildServer.clouds.amazon.sns.trigger.SnsNotificationDto;
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

public class AwsSnsHttpEndpointHelper {
  public static final String AWS_TOPIC_ARN_HEADER = "x-amz-sns-topic-arn";
  public static final String NOTIFICATION_SUBJECT_KEY = "Subject";
  public static final String NOTIFICATION_MESSAGE_KEY = "Message";
  public static final String NOTIFICATION_TIMESTAMP_KEY = "Timestamp";
  public static final String NOTIFICATION_ATTRIBUTES_KEY = "MessageAttributes";
  private static final String MESSAGE_TYPE_KEY = "Type";
  private static final String SUBSCRIBE_URL_KEY = "SubscribeURL";
  private static final String UNSUBSCRIBE_URL_KEY = "UnsubscribeURL";
  private static final String SUBSCRIBE_CONFIRMATION_ARN_XPATH = "//*[local-name() = 'ConfirmSubscriptionResult']/*[local-name() = 'SubscriptionArn']";
  private static final String AWS_SUBSCRIPTION_ARN_HEADER = "x-amz-sns-subscription-arn";
  private static final String AWS_MESSAGE_ID_HEADER = "x-amz-sns-message-id";

  public static boolean isSubscription(@NotNull Map<String, Object> payload) {
    String messageType = (String) payload.get(MESSAGE_TYPE_KEY);
    return messageType != null &&
            messageType.equals(MessageType.SUBSCRIBE.toString());
  }

  public static boolean isUnsubscribe(@NotNull Map<String, Object> payload) {
    String messageType = (String) payload.get(MESSAGE_TYPE_KEY);
    return messageType != null &&
            messageType.equals(MessageType.UNSUBSRIBE.toString());
  }

  public static boolean isNotification(@NotNull Map<String, Object> payload) {
    String messageType = (String) payload.get(MESSAGE_TYPE_KEY);
    return messageType != null &&
            messageType.equals(MessageType.NOTIFICATION.toString());
  }

  public static boolean isValidSignature(@NotNull Map<String, Object> payload) {
    // TODO: signature validation
    return true;
  }

  @Nullable
  public static String subscribe(@NotNull Map<String, Object> payload,
                                 @NotNull HttpApi serverApi) throws IOException, JDOMException {
    String subscribeUrl = (String) payload.get(SUBSCRIBE_URL_KEY);
    HttpApi.Response response = serverApi.get(subscribeUrl);
    String body = response.getBody();

    try (Reader bodyReader = new StringReader(body)) {
      XPath xpath = XPath.newInstance(SUBSCRIBE_CONFIRMATION_ARN_XPATH);
      return xpath.valueOf(new SAXBuilder().build(bodyReader));
    }
  }

  @NotNull
  public static SnsNotificationDto convertToNotificaitonDto(HttpServletRequest request, @NotNull Map<String, Object> payload) {
    String subscriptionArn = request.getHeader(AWS_SUBSCRIPTION_ARN_HEADER);
    String messageId = request.getHeader(AWS_MESSAGE_ID_HEADER);
    String topic = request.getHeader(AWS_TOPIC_ARN_HEADER);
    String subject = (String) payload.get(NOTIFICATION_SUBJECT_KEY);
    String message = (String) payload.get(NOTIFICATION_MESSAGE_KEY);
    Instant timestamp = Instant.parse((String) payload.get(NOTIFICATION_TIMESTAMP_KEY));
    String unsubscribeUrl = (String) payload.get(UNSUBSCRIBE_URL_KEY);
    Map<String, Object> attributes = (Map<String, Object>) payload.get(NOTIFICATION_ATTRIBUTES_KEY);

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

  private enum MessageType {
    SUBSCRIBE("SubscriptionConfirmation"),
    UNSUBSRIBE("UnsubscribeConfirmation"),
    NOTIFICATION("Notification");

    private final String typeString;

    MessageType(String typeString) {
      this.typeString = typeString;
    }

    @Override
    public String toString() {
      return typeString;
    }
  }
}
