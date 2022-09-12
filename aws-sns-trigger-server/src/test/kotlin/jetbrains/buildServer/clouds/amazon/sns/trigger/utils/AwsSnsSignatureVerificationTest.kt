package jetbrains.buildServer.clouds.amazon.sns.trigger.utils

import com.intellij.openapi.diagnostic.Logger
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsMessageType
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants
import jetbrains.buildServer.http.HttpApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
    fun `can't decode sns signature`() {
        val customized = mutableMapOf<String, Any?>().also { it.putAll(notificationPayload) }
        customized[AwsSnsTriggerConstants.SIGNING_SIGNATURE_KEY] = "not a base64 string"

        testable = AwsSnsSignatureVerification(
            SnsMessageType.NOTIFICATION,
            customized,
            serverApiMock
        )

        val messageSlot = slot<String>()
        val exceptionSlot = slot<java.lang.Exception>()
        val logMock = mockk<Logger>()
        every { logMock.warn(capture(messageSlot), capture(exceptionSlot)) } just runs

        testable.setLogger(logMock)
        assertFalse(testable.isValid)

        assertTrue(messageSlot.isCaptured)
        assertEquals("Signature verification failed", messageSlot.captured)

        assertTrue(exceptionSlot.isCaptured)
        assertTrue(exceptionSlot.captured is IllegalArgumentException)
    }

    @Test
    @Order(0)
    fun `can't get signature from sns message due to io exception`() {
        every { serverApiMock.get(any()) } throws IOException()

        testable = AwsSnsSignatureVerification(
            SnsMessageType.NOTIFICATION,
            notificationPayload,
            serverApiMock
        )

        val messageSlot = slot<String>()
        val exceptionSlot = slot<java.lang.Exception>()
        val logMock = mockk<Logger>()
        every { logMock.warn(capture(messageSlot), capture(exceptionSlot)) } just runs

        testable.setLogger(logMock)
        assertFalse(testable.isValid)

        assertTrue(messageSlot.isCaptured)
        assertEquals("Signature verification failed", messageSlot.captured)

        assertTrue(exceptionSlot.isCaptured)
        assertTrue(exceptionSlot.captured is IOException)
    }

    @Test
    @Order(0)
    fun `can't get signature from sns message certificate`() {
        testable = AwsSnsSignatureVerification(
            SnsMessageType.NOTIFICATION,
            notificationPayload,
            serverApiMock
        )

        val messageSlot = slot<String>()
        val exceptionSlot = slot<java.lang.Exception>()
        val logMock = mockk<Logger>()
        every { logMock.warn(capture(messageSlot), capture(exceptionSlot)) } just runs

        testable.setLogger(logMock)
        assertFalse(testable.isValid)

        assertTrue(messageSlot.isCaptured)
        assertEquals("Signature verification failed", messageSlot.captured)
        assertTrue(exceptionSlot.isCaptured)
        assertTrue(exceptionSlot.captured is CertificateException)
    }

    @Test
    fun `signature decoding failed due to illegal state exception`() {
        val certificate = getResourcesAsString("/signatureCert/${notificationPayload["MessageId"] as String}.pem")
        every { responseMock.body } returns certificate

        val customized = mutableMapOf<String, Any?>().also { it.putAll(notificationPayload) }
        customized[AwsSnsTriggerConstants.SIGNING_SIGNATURE_VERSION_KEY] = "42"

        testable = AwsSnsSignatureVerification(
            SnsMessageType.NOTIFICATION,
            customized,
            serverApiMock
        )

        val messageSlot = slot<String>()
        val exceptionSlot = slot<java.lang.Exception>()
        val logMock = mockk<Logger>()
        every { logMock.warn(capture(messageSlot), capture(exceptionSlot)) } just runs

        testable.setLogger(logMock)
        assertFalse(testable.isValid)

        assertTrue(messageSlot.isCaptured)
        assertEquals("Signature verification failed", messageSlot.captured)

        assertTrue(messageSlot.isCaptured)
        assertEquals("Invalid SignatureVersion value", exceptionSlot.captured.message)
    }

    @Test
    fun `signature decoding failed due to signature exception`() {
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

        val messageSlot = slot<String>()
        val exceptionSlot = slot<java.lang.Exception>()
        val logMock = mockk<Logger>()
        every { logMock.warn(capture(messageSlot), capture(exceptionSlot)) } just runs

        testable.setLogger(logMock)
        assertFalse(testable.isValid)

        assertTrue(messageSlot.isCaptured)
        assertEquals("Signature verification failed", messageSlot.captured)

        assertTrue(exceptionSlot.isCaptured)
        assertTrue(exceptionSlot.captured is SignatureException)
    }
}
