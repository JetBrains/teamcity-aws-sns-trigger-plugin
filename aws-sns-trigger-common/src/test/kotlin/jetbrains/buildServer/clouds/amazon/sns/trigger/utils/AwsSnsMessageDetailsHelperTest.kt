package jetbrains.buildServer.clouds.amazon.sns.trigger.utils

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants
import jetbrains.buildServer.http.HttpApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import javax.servlet.http.HttpServletRequest

@ExtendWith(MockKExtension::class)
internal class AwsSnsMessageDetailsHelperTest {
    @MockK
    private lateinit var serverApiMock: HttpApi

    @RelaxedMockK
    private lateinit var httpServletRequestMock: HttpServletRequest

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
        val subscriptionResponseXml = getResourcesAsString("/xml/subscriptionResponse.xml")
        val responseMock = mockk<HttpApi.Response>()

        every { responseMock.body } returns subscriptionResponseXml
        every { serverApiMock.get(any()) } returns responseMock

        val result = AwsSnsMessageDetailsHelper.subscribe(
            subscriptionPayload,
            serverApiMock
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

        every { httpServletRequestMock.getHeader(AwsSnsTriggerConstants.AWS_SUBSCRIPTION_ARN_HEADER) } returns subArn
        every { httpServletRequestMock.getHeader(AwsSnsTriggerConstants.AWS_MESSAGE_ID_HEADER) } returns messId
        every { httpServletRequestMock.getHeader(AwsSnsTriggerConstants.AWS_TOPIC_ARN_HEADER) } returns topicArn

        val result = AwsSnsMessageDetailsHelper.convertToNotificationDto(
            httpServletRequestMock,
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
