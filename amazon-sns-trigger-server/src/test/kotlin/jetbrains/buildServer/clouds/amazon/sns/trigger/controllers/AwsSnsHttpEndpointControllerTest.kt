package jetbrains.buildServer.clouds.amazon.sns.trigger.controllers

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
import jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto
import jetbrains.buildServer.clouds.amazon.sns.trigger.service.SnsBuildTriggerService
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.AwsSnsMessageDetailsHelper
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants
import jetbrains.buildServer.controllers.AuthorizationInterceptor
import jetbrains.buildServer.http.HttpApi
import jetbrains.buildServer.serverSide.CustomDataStorage
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.SecurityContextEx
import jetbrains.buildServer.serverSide.ServerResponsibilityImpl
import jetbrains.buildServer.serverSide.impl.PolledTriggerContextImpl
import jetbrains.buildServer.serverSide.impl.auth.SecurityContextImpl
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@ExtendWith(MockKExtension::class)
class AwsSnsHttpEndpointControllerTest {

    private lateinit var testable: AwsSnsHttpEndpointController

    @Test
    fun doHandle() {
        val wcmMock = mockk<WebControllerManager>(relaxed = true)
        val pmMock = mockk<ProjectManager>()
        val saMock = mockk<HttpApi>()
        val aiMock = mockk<AuthorizationInterceptor>(relaxed = true)
        val projectMock = mockk<SProject>()
        val buildTypeMock = mockk<SBuildType>()
        val buildTriggerMock = mockk<BuildTriggerDescriptor>()
        val btdCollection: Collection<BuildTriggerDescriptor> = listOf(buildTriggerMock)

        every { pmMock.findProjectByExternalId(any()) } returns projectMock
        every { projectMock.findBuildTypeByExternalId(any()) } returns buildTypeMock
        every { buildTypeMock.buildTriggersCollection } returns btdCollection
        every { buildTriggerMock.type } returns SnsBuildTriggerService.TRIGGER_NAME

        testable =
            AwsSnsHttpEndpointController(
                mockk(),
                wcmMock,
                pmMock,
                saMock,
                aiMock,
                SecurityContextImpl(ServerResponsibilityImpl())
            )

        val reqMock = mockk<HttpServletRequest>(relaxed = true)
        val resMock = mockk<HttpServletResponse>()
        val cdsMock = mockk<CustomDataStorage>(relaxed = true)
        val sbtsMock = mockk<SnsBuildTriggerService>(relaxed = true)
        mockkStatic("jetbrains.buildServer.clouds.amazon.sns.trigger.utils.AwsSnsMessageDetailsHelper")
        mockkStatic("jetbrains.buildServer.serverSide.impl.PolledTriggerContextImpl")

        every { reqMock.pathInfo } returns "bs/app/trigger/sns/TeamcityGoogleStorage/TeamcityGoogleStorage_Build/2d1c326f-8d1a-479c-9d33-ba6a1afe2483"
        every { buildTriggerMock.properties } returns mapOf(AwsSnsTriggerConstants.TRIGGER_UUID_PROPERTY_KEY to "2d1c326f-8d1a-479c-9d33-ba6a1afe2483")
        every { reqMock.method } returns "POST"

        val stream = "{}".byteInputStream()
        every { reqMock.inputStream } returns object : ServletInputStream() {
            override fun read(): Int = stream.read()
            override fun isFinished(): Boolean = stream.available() <= 0
            override fun isReady(): Boolean = stream.available() > 0
            override fun setReadListener(listener: ReadListener?) = error("Not supported")
        }

        every { PolledTriggerContextImpl.getCustomDataStorage(buildTypeMock, buildTriggerMock) } returns cdsMock
        every { AwsSnsMessageDetailsHelper.isValidSignature(any(), any()) } returns true
        every { AwsSnsMessageDetailsHelper.isNotification(any()) } returns true
        val subscriptionArn = "arn:aws:sns:us-west-2:123456789012:MyTopic:2bcfbf39-05c3-41de-beaa-fcfcc21c8f55"
        every { AwsSnsMessageDetailsHelper.convertToNotificationDto(reqMock, any()) } returns SnsNotificationDto(
            "fab38dbf-9f2b-50f7-9897-d633977b095a",
            subscriptionArn,
            "arn:aws:sns:us-west-2:123456789012:MyTopic",
            "subj",
            "message",
            Instant.now(),
            null,
            null
        )
        every { cdsMock.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_SUBSCRIPTION_ARN) } returns subscriptionArn
        every { buildTriggerMock.buildTriggerService } returns sbtsMock

        val result = testable.doHandle(reqMock, resMock)
        assert(result == null)
    }
}
