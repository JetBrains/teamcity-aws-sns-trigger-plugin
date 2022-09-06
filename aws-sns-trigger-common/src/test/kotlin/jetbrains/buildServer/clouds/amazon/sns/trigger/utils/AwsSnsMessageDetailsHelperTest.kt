package jetbrains.buildServer.clouds.amazon.sns.trigger.utils

import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

internal class AwsSnsMessageDetailsHelperTest {
    @Test
    fun isSubscription() {
        assertTrue(AwsSnsMessageDetailsHelper.isSubscription(subscriptionPayload))
    }

    @Test
    fun isNotification() {
        assertTrue(AwsSnsMessageDetailsHelper.isNotification(notificationPayload))
    }

    @Test
    fun subscribe() {
        val result = AwsSnsMessageDetailsHelper.subscribe(
            subscriptionPayload,
            serverApiStub.also {
                HttpResponse.data =
                    getResourcesAsString("/xml/subscriptionResponse.xml")
            }
        )

        assertEquals("arn:aws:sns:us-west-2:123456789012:MyTopic:2bcfbf39-05c3-41de-beaa-fcfcc21c8f55", result)
    }

    @Test
    fun convertToNotificationDto() {
        val subArn = "arn:aws:sns:us-west-2:123456789012:MyTopic:2bcfbf39-05c3-41de-beaa-fcfcc21c8f55"
        val messId = "fab38dbf-9f2b-50f7-9897-d633977b095a"
        val topicArn = "arn:aws:sns:us-east-1:111:MyTopic"
        val timestamp =
            Instant.parse(notificationPayload[AwsSnsTriggerConstants.NOTIFICATION_TIMESTAMP_KEY] as CharSequence?)

        HttpServletRequestStub.setHeaders(
            mapOf(
                AwsSnsTriggerConstants.AWS_SUBSCRIPTION_ARN_HEADER to subArn,
                AwsSnsTriggerConstants.AWS_MESSAGE_ID_HEADER to messId,
                AwsSnsTriggerConstants.AWS_TOPIC_ARN_HEADER to topicArn
            )
        )
        val result = AwsSnsMessageDetailsHelper.convertToNotificationDto(
            HttpServletRequestStub,
            notificationPayload
        )

        assertEquals(subArn, result.subscriptionArn)
        assertEquals(messId, result.messageId)
        assertEquals(topicArn, result.topic)
        assertEquals(notificationPayload[AwsSnsTriggerConstants.NOTIFICATION_SUBJECT_KEY], result.subject)
        assertEquals(notificationPayload[AwsSnsTriggerConstants.NOTIFICATION_MESSAGE_KEY], result.message)
        assertEquals(timestamp, result.timestamp)
        assertEquals(notificationPayload[AwsSnsTriggerConstants.UNSUBSCRIBE_URL_KEY], result.unsubscribeUrl)
        assertEquals(notificationPayload[AwsSnsTriggerConstants.NOTIFICATION_ATTRIBUTES_KEY], result.attributes)
    }
}
