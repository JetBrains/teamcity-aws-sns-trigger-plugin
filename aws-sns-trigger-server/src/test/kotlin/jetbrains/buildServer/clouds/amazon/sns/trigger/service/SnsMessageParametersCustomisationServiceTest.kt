package jetbrains.buildServer.clouds.amazon.sns.trigger.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import jetbrains.buildServer.ExtensionHolder
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SnsMessageParametersCustomisationServiceTest {
    @RelaxedMockK
    private lateinit var extensionHolderMock: ExtensionHolder

    private lateinit var testable: SnsMessageParametersCustomisationService

    private lateinit var messageAttributes: Map<String, Any?>

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setup() {
        testable = SnsMessageParametersCustomisationService(extensionHolderMock)
        messageAttributes = objectMapper.readValue(
            """
  {
    "bar" : {"Type":"Number","Value":"3.14"},
    "foo-bar" : {"Type":"String","Value":"[\"lol\", \"kek\"]"},
    "foo" : {"Type":"String","Value":"lol"},
    "foo_bar" : {"Type":"String.Array","Value":"[\"lol\", \"kek\"]"},
    "foo.bar" : {"Type":"Binary","Value":"YUdWc2JHOGdkMjl5YkdRaA=="}
  }
            """.trimIndent(),
            object : TypeReference<HashMap<String, Any?>>() {}
        )
    }

    @Test
    fun replacePlaceholdersWithValues() {
        val subj = "subjectValue"
        val body = "bodyValue"
        val foo = "lol"
        val bar = "3.14"
        val `foo-bar` = "[\"lol\", \"kek\"]"
        val foo_bar = "[\"lol\", \"kek\"]"
        val foobar = "YUdWc2JHOGdkMjl5YkdRaA=="

        val value = """%sns.message.subject% maybe even with %sns.message.body%  and with few attributes:
            %sns.message.attributes.foo%
            %sns.message.attributes.bar%
            %sns.message.attributes.foo-bar%
            %sns.message.attributes.foo_bar%
            %sns.message.attributes.foo.bar%
        """.trimIndent()

        val expected = """$subj maybe even with $body  and with few attributes:
            $foo
            $bar
            $`foo-bar`
            $foo_bar
            $foobar
        """.trimIndent()
        val message = SnsNotificationDto().apply {
            subject = subj
            message = body
            attributes = messageAttributes
        }

        val result = testable.replacePlaceholdersWithValues(value, message)
        Assertions.assertEquals(expected, result)
    }
}
