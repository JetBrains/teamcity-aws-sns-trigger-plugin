package jetbrains.buildServer.clouds.amazon.sns.trigger.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import jetbrains.buildServer.ExtensionHolder
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants.SNS_MESSAGE_ATTRIBUTES_PARAMETER_PLACEHOLDER
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER
import jetbrains.buildServer.serverSide.BuildPromotion
import jetbrains.buildServer.serverSide.SBuild
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SnsMessageParametersCustomisationServiceTest {
    @RelaxedMockK
    private lateinit var extensionHolderMock: ExtensionHolder

    @MockK
    private lateinit var buildMock: SBuild

    @MockK
    private lateinit var buildPromotion: BuildPromotion

    private lateinit var testable: SnsMessageParametersCustomisationService

    @BeforeEach
    fun setup() {
        testable = SnsMessageParametersCustomisationService(extensionHolderMock)
        every { buildMock.buildPromotion } returns buildPromotion
    }

    @Test
    fun getParameters() {
        every { buildMock.buildOwnParameters } returns mapOf(
            SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER to "test subject",
            SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER to "test message",
            SNS_MESSAGE_ATTRIBUTES_PARAMETER_PLACEHOLDER + "foo" to "foo_value"
        )
        val result = testable.getParameters(buildMock, false)
        assertEquals("test subject", result[SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER])
        assertEquals("test message", result[SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER])
        assertEquals("foo_value", result[SNS_MESSAGE_ATTRIBUTES_PARAMETER_PLACEHOLDER + "foo"])
    }

    @Test
    fun getParametersEmulation() {
        every { buildMock.buildOwnParameters } returns emptyMap()
        val result = testable.getParameters(buildMock, true)
        assertEquals("???", result[SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER])
        assertEquals("???", result[SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER])
    }
}
