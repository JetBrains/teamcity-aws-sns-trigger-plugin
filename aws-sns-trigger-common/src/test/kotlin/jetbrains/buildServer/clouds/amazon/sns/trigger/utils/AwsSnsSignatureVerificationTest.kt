package jetbrains.buildServer.clouds.amazon.sns.trigger.utils

import jetbrains.buildServer.clouds.amazon.sns.trigger.SnsMessageType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AwsSnsSignatureVerificationTest {

    private lateinit var testable: AwsSnsSignatureVerification

    @Test
    fun isSubscriptionValid() {
        testable = AwsSnsSignatureVerification(
            SnsMessageType.SUBSCRIBE,
            subscriptionPayload,
            serverApiStub.also {
                HttpResponse.data =
                    getResourcesAsString("/signatureCert/${subscriptionPayload["MessageId"] as String}.pem")
            }
        )

        assertTrue(testable.isValid)
    }

    @Test
    fun isNotificationValid() {
        testable = AwsSnsSignatureVerification(
            SnsMessageType.NOTIFICATION,
            notificationPayload,
            serverApiStub.also {
                HttpResponse.data =
                    getResourcesAsString("/signatureCert/${notificationPayload["MessageId"] as String}.pem")
            }
        )

        assertTrue(testable.isValid)
    }
}
