package jetbrains.buildServer.clouds.amazon.sns.trigger.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import jetbrains.buildServer.buildTriggers.PolledTriggerContext
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants
import jetbrains.buildServer.serverSide.BuildCustomizerEx
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildTypeEx
import jetbrains.buildServer.serverSide.CustomDataStorage
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.WebLinks
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
class SnsBuildTriggeringPolicyTest {
    @RelaxedMockK
    private lateinit var customDataStorageMock: CustomDataStorage

    @MockK
    private lateinit var projectManagerMock: ProjectManager

    @MockK
    private lateinit var webLinksMock: WebLinks

    @RelaxedMockK
    private lateinit var buildCustomizerMock: BuildCustomizerEx

    @MockK
    private lateinit var buildPromotionMock: BuildPromotionEx

    @RelaxedMockK
    private lateinit var buildTypeMock: BuildTypeEx

    @RelaxedMockK
    private lateinit var parametersCustomizerMock: SnsMessageParametersCustomisationService

    private lateinit var triggerContext: AwsSnsTriggeringContext
    private lateinit var testable: SnsBuildTriggeringPolicy

    @BeforeEach
    fun startUp() {
        triggerContext =
            spyk(
                AwsSnsTriggeringContext(
                    projectManagerMock,
                    webLinksMock,
                    parametersCustomizerMock
                )
            )
        testable = spyk(SnsBuildTriggeringPolicy(triggerContext), recordPrivateCalls = true)
    }

    @Test
    fun triggerBuild() {
        val contextMock = mockk<PolledTriggerContext>(relaxed = true)
        val slot = slot<String>()
        val objectMapperMock = mockk<ObjectMapper>(relaxed = true)

        every { contextMock.customDataStorage } returns customDataStorageMock
        every { contextMock.createBuildCustomizer(null) } returns buildCustomizerMock
        every { contextMock.buildType } returns buildTypeMock
        every { customDataStorageMock.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES) } returns "something"
        every { buildCustomizerMock.createPromotion() } returns buildPromotionMock
        every { buildTypeMock.addToQueue(buildPromotionMock, capture(slot)) } returns mockk()
        every { triggerContext.objectMapper } returns objectMapperMock
        every {
            objectMapperMock.readValue(
                "something",
                any() as TypeReference<*>
            )
        } returns mutableMapOf("1" to SnsNotificationDto().apply { timestamp = Instant.now() })

        testable.triggerBuild(contextMock)
        assertTrue(slot.isCaptured)
        Assertions.assertEquals("##type='sns' triggerId='' queueMergingEnabled='false'", slot.captured)
        verify(exactly = 1) { customDataStorageMock.refresh() }
        verify(exactly = 3) { customDataStorageMock.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES) }
        verify(exactly = 1) { customDataStorageMock.putValue(AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES, null) }
        verify(exactly = 1) { customDataStorageMock.flush() }
        confirmVerified(customDataStorageMock)
    }

    @Test
    fun triggerActivated() {
        val context = mockk<PolledTriggerContext>(relaxed = true)
        every { context.customDataStorage } returns customDataStorageMock

        testable.triggerActivated(context)

        verifyOrder {
            customDataStorageMock.putValues(any())
            customDataStorageMock.putValue(AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES, null)
        }
        confirmVerified(customDataStorageMock)
    }
}
