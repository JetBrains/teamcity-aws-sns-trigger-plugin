package jetbrains.buildServer.trigger.sns

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verifyOrder
import jetbrains.buildServer.ExtensionHolder
import jetbrains.buildServer.clouds.amazon.sns.trigger.SnsNotificationDto
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants
import jetbrains.buildServer.serverSide.CustomDataStorage
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.WebLinks
import jetbrains.buildServer.serverSide.impl.PolledTriggerContextImpl
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SnsBuildTriggerServiceTest {
    @RelaxedMockK
    private lateinit var projectManagerMock: ProjectManager

    @MockK
    private lateinit var webLinksMock: WebLinks

    @RelaxedMockK
    private lateinit var extensionHolderMock: ExtensionHolder

    @MockK
    private lateinit var descriptorMock: PluginDescriptor

    private lateinit var triggerContext: AwsSnsTriggeringContext
    private lateinit var testable: SnsBuildTriggerService

    @BeforeEach
    fun startUp() {
        every { descriptorMock.getPluginResourcesPath(SnsBuildTriggerService.EDIT_PARAMS_URL) } returns SnsBuildTriggerService.EDIT_PARAMS_URL
        every { webLinksMock.getRootUrlByProjectExternalId(null) } returns "root/url"
        triggerContext = AwsSnsTriggeringContext(projectManagerMock, webLinksMock)
        testable = SnsBuildTriggerService(extensionHolderMock, descriptorMock, triggerContext)
    }

    @Test
    fun describeTrigger() {
        val cdsMock = mockk<CustomDataStorage>(relaxed = true)
        mockkStatic("jetbrains.buildServer.serverSide.impl.PolledTriggerContextImpl")
        every { PolledTriggerContextImpl.getCustomDataStorage(any(), any()) } returns cdsMock
        every { cdsMock.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_TOPIC_ARN) } returns null
        every { cdsMock.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_SUBSCRIPTION_ARN) } returns null

        val result = testable.describeTrigger(mockk(relaxed = true))
        assertFalse(result.isBlank())
    }

    @Test
    fun defaultTriggerProperties() {
        val props = testable.defaultTriggerProperties
        assertEquals(AwsSnsTriggerConstants.SNS_CONNECTION_CONTROLLER_URL, props?.get("urlPathPart"))
        assertEquals("root/url", props?.get("rootUrl"))
        assertFalse(props?.get(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY).isNullOrBlank())
    }

    @Test
    fun registerMessage() {
        val dto = SnsNotificationDto().apply { messageId = "some-id" }
        val cdsMock = mockk<CustomDataStorage>(relaxed = true)

        every { cdsMock.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_IS_ACTIVE) } returns "true"
        every { cdsMock.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES) } returns null

        testable.registerMessage(dto, cdsMock)

        verifyOrder {
            cdsMock.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_IS_ACTIVE)
            cdsMock.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES)
            cdsMock.putValue(AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES, any())
            cdsMock.flush()
        }
        confirmVerified(cdsMock)
    }
}
