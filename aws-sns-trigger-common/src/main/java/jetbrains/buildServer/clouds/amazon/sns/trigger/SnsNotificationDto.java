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

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

public class SnsNotificationDto {
  private String myUnsubscribeUrl;
  private String myMessageId;
  private String mySubscriptionArn;
  private String mySubject;
  private String myMessage;
  private Instant myTimestamp;
  private Map<String, Object> myAttributes;
  private String myTopic;

  public SnsNotificationDto() {
  }

  public SnsNotificationDto(
          @Nullable String messageId,
          @Nullable String subscriptionArn,
          @Nullable String topic,
          @Nullable String subject,
          @Nullable String message,
          @Nullable Instant timestamp,
          @Nullable String unsubscribeUrl,
          @Nullable Map<String, Object> attributes
  ) {
    myMessageId = messageId;
    mySubscriptionArn = subscriptionArn;
    myTopic = topic;
    mySubject = subject;
    myMessage = message;
    myTimestamp = timestamp;
    myUnsubscribeUrl = unsubscribeUrl;
    myAttributes = attributes;
  }

  @Nullable
  public String getMessageId() {
    return myMessageId;
  }

  public void setMessageId(@Nullable String messageId) {
    myMessageId = messageId;
  }

  @Nullable
  public String getSubscriptionArn() {
    return mySubscriptionArn;
  }

  public void setSubscriptionArn(@Nullable String subscriptionArn) {
    mySubscriptionArn = subscriptionArn;
  }

  @Nullable
  public String getSubject() {
    return mySubject;
  }

  public void setSubject(@Nullable String subject) {
    mySubject = subject;
  }

  @Nullable
  public String getMessage() {
    return myMessage;
  }

  public void setMessage(@Nullable String message) {
    myMessage = message;
  }

  @Nullable
  public Instant getTimestamp() {
    return myTimestamp;
  }

  public void setTimestamp(@Nullable Instant timestamp) {
    myTimestamp = timestamp;
  }

  @Nullable
  public Map<String, Object> getAttributes() {
    return myAttributes;
  }

  public void setAttributes(@Nullable Map<String, Object> attributes) {
    myAttributes = attributes;
  }

  public String getTopic() {
    return myTopic;
  }

  public void setTopic(@Nullable String topic) {
    myTopic = topic;
  }

  public String getUnsubscribeUrl() {
    return myUnsubscribeUrl;
  }

  public void setUnsubscribeUrl(String unsubscribeUrl) {
    myUnsubscribeUrl = unsubscribeUrl;
  }
}
