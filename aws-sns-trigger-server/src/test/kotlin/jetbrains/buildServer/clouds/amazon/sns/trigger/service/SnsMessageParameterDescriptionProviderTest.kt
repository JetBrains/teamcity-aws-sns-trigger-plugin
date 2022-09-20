package jetbrains.buildServer.clouds.amazon.sns.trigger.service

import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SnsMessageParameterDescriptionProviderTest {

    private lateinit var testable: SnsMessageParameterDescriptionProvider

    @BeforeEach
    fun setup() {
        testable = SnsMessageParameterDescriptionProvider()
    }

    @Test
    fun describeSubject() {
        val subjDescription = testable.describe(AwsSnsTriggerConstants.SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER)
        assertEquals("Subject value from SNS message (only provided by Amazon SNS Trigger)", subjDescription)
    }

    @Test
    fun describeBody() {
        val bodyDescription = testable.describe(AwsSnsTriggerConstants.SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER)
        assertEquals("Message body from SNS message (only provided by Amazon SNS Trigger)", bodyDescription)
    }
}
