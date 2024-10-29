

package jetbrains.buildServer.clouds.amazon.sns.trigger.dto;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
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
          @NotNull Instant timestamp,
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

  @NotNull
  public Instant getTimestamp() {
    return myTimestamp;
  }

  public void setTimestamp(@NotNull Instant timestamp) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SnsNotificationDto that = (SnsNotificationDto)o;
    return Objects.equals(myUnsubscribeUrl, that.myUnsubscribeUrl) && Objects.equals(myMessageId, that.myMessageId) &&
           Objects.equals(mySubscriptionArn, that.mySubscriptionArn) && Objects.equals(mySubject, that.mySubject) &&
           Objects.equals(myMessage, that.myMessage) && Objects.equals(myTimestamp, that.myTimestamp) &&
           Objects.equals(myAttributes, that.myAttributes) && Objects.equals(myTopic, that.myTopic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myUnsubscribeUrl, myMessageId, mySubscriptionArn, mySubject, myMessage, myTimestamp, myAttributes, myTopic);
  }
}
