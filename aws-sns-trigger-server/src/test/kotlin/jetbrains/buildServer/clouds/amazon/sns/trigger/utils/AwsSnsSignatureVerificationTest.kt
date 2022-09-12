package jetbrains.buildServer.clouds.amazon.sns.trigger.utils

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsMessageType
import jetbrains.buildServer.clouds.amazon.sns.trigger.errors.AwsSnsHttpEndpointException
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants
import jetbrains.buildServer.http.HttpApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import java.security.SignatureException
import java.security.cert.CertificateException
import java.util.*

@ExtendWith(MockKExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class AwsSnsSignatureVerificationTest {
    @MockK
    private lateinit var serverApiMock: HttpApi

    @RelaxedMockK
    private lateinit var responseMock: HttpApi.Response

    private lateinit var testable: AwsSnsSignatureVerification

    @BeforeEach
    fun setup() {
        every { serverApiMock.get(any()) } returns responseMock
    }

    @Test
    fun isSubscriptionValid() {
        val certificate = getResourcesAsString("/signatureCert/${subscriptionPayload["MessageId"] as String}.pem")
        every { responseMock.body } returns certificate

        testable = AwsSnsSignatureVerification(
            SnsMessageType.SUBSCRIBE,
            subscriptionPayload,
            serverApiMock
        )

        assertTrue(testable.isValid)
    }

    @Test
    fun isNotificationValid() {
        val certificate = getResourcesAsString("/signatureCert/${notificationPayload["MessageId"] as String}.pem")
        every { responseMock.body } returns certificate

        testable = AwsSnsSignatureVerification(
            SnsMessageType.NOTIFICATION,
            notificationPayload,
            serverApiMock
        )

        assertTrue(testable.isValid)
    }

    @Test
    fun `throws can't decode sns signature exception`() {
        val customized = mutableMapOf<String, Any?>().also { it.putAll(notificationPayload) }
        customized[AwsSnsTriggerConstants.SIGNING_SIGNATURE_KEY] = "not a base64 string"

        testable = AwsSnsSignatureVerification(
            SnsMessageType.NOTIFICATION,
            customized,
            serverApiMock
        )

        val error = assertThrows(AwsSnsHttpEndpointException::class.java) {
            testable.isValid
        }

        assertEquals("Can't decode SNS message signature", error.message)
        assertTrue(error.cause is IllegalArgumentException)
    }

    @Test
    @Order(0)
    fun `throws can't get signature from sns message io exception`() {
        every { serverApiMock.get(any()) } throws IOException()

        testable = AwsSnsSignatureVerification(
            SnsMessageType.NOTIFICATION,
            notificationPayload,
            serverApiMock
        )

        val error = assertThrows(AwsSnsHttpEndpointException::class.java) {
            testable.isValid
        }

        assertEquals("Can't get signature certificate from SNS message", error.message)
        assertTrue(error.cause is IOException)
    }

    @Test
    @Order(0)
    fun `throws can't get signature from sns message certificate exception`() {
        testable = AwsSnsSignatureVerification(
            SnsMessageType.NOTIFICATION,
            notificationPayload,
            serverApiMock
        )

        val error = assertThrows(AwsSnsHttpEndpointException::class.java) {
            testable.isValid
        }

        assertEquals("Can't get signature certificate from SNS message", error.message)
        assertTrue(error.cause is CertificateException)
    }

    @Test
    fun `throws signature decoding failed illegal state exception`() {
        val certificate = getResourcesAsString("/signatureCert/${notificationPayload["MessageId"] as String}.pem")
        every { responseMock.body } returns certificate

        val customized = mutableMapOf<String, Any?>().also { it.putAll(notificationPayload) }
        customized[AwsSnsTriggerConstants.SIGNING_SIGNATURE_VERSION_KEY] = "42"

        testable = AwsSnsSignatureVerification(
            SnsMessageType.NOTIFICATION,
            customized,
            serverApiMock
        )

        val error = assertThrows(AwsSnsHttpEndpointException::class.java) {
            testable.isValid
        }

        assertEquals("Signature decoding failed", error.message)
        assertEquals("Invalid SignatureVersion value", error.cause?.message)
    }

    @Test
    fun `throws signature decoding failed signature exception`() {
        val certificate = getResourcesAsString("/signatureCert/${notificationPayload["MessageId"] as String}.pem")
        every { responseMock.body } returns certificate

        val customized = mutableMapOf<String, Any?>().also { it.putAll(notificationPayload) }
        customized[AwsSnsTriggerConstants.SIGNING_SIGNATURE_KEY] =
            Base64.getEncoder().encodeToString("not a signature".toByteArray())

        testable = AwsSnsSignatureVerification(
            SnsMessageType.NOTIFICATION,
            customized,
            serverApiMock
        )

        val error = assertThrows(AwsSnsHttpEndpointException::class.java) {
            testable.isValid
        }

        assertEquals("Signature decoding failed", error.message)
        assertTrue(error.cause is SignatureException)
    }
}
