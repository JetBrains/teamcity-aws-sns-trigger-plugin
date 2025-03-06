<%@ include file="include.jsp" %>
<%@ page import="jetbrains.buildServer.buildTriggers.BuildTriggerDescriptor" %>
<%@ page import="jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants" %>
<%@ page import="jetbrains.buildServer.controllers.admin.projects.BuildTriggerInfo" %>
<%@ page import="jetbrains.buildServer.serverSide.CustomDataStorage" %>
<%@ page import="jetbrains.buildServer.serverSide.SBuildType" %>
<%@ page import="jetbrains.buildServer.serverSide.impl.PolledTriggerContextImpl" %>
<%@ page import="jetbrains.buildServer.web.util.WebUtil" %>
<%@ page import="org.apache.logging.log4j.util.Strings" %>
<%@ page import="java.util.Objects" %>

<jsp:useBean id="triggerDescriptorBean"
             type="jetbrains.buildServer.controllers.admin.projects.BuildTriggerDescriptorBean" scope="request"/>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="buildForm" type="jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm"
             scope="request"/>

<c:set var="currentProjectExternalId" value="${buildForm.project.externalId}"/>
<c:set var="currentBuildTypeExternalId" value="${buildForm.settingsBuildType.externalId}"/>

<c:set var="triggerUuid" value="${propertiesBean.properties['triggerUuid']}"/>
<c:set var="triggerName"
       value="${empty propertiesBean.properties['displayName'] ? triggerUuid : propertiesBean.properties['displayName']}"/>

<%--endpoint URL constructor--%>
<c:set var="urlPathPart" value="<%=AwsSnsTriggerConstants.getTriggerUrlPathPart()%>"/>
<c:set var="rootUrl" value="${WebUtil.getRootUrl(pageContext.request)}"/>
<c:set var="triggerUrl" value="${urlPathPart}/${currentProjectExternalId}/${currentBuildTypeExternalId}"/>
<c:set var="endpointUrl" value="${rootUrl}${triggerUrl}/${triggerUuid}"/>

<tr>
    <td colspan="2">
        <em>Amazon SNS Trigger will add a build to the queue when an SNS message received.</em>
    </td>
</tr>
<tr>
    <td><label for="displayName">Display name:</label></td>
    <td>
        <props:textProperty name="displayName" value="${triggerName}" className="longField"
                            style="width: 20em;"/>
        <span class="smallNote nowrap">Provide a name to distinguish this trigger from others.</span>
        <span class="error" id="error_displayName"></span>
    </td>
</tr>
<td><label for="triggerUuid">Trigger ID:</label><l:star/></td>
<td>
    <c:choose>
        <c:when test="${empty triggerUuid or empty currentBuildTypeExternalId}">
            <props:textProperty name="triggerUuid" value="${triggerUuid}"/>
            <span class="smallNote">This ID is used in URL (see below).</span>
            <script type="application/javascript">
                BS.AdminActions.prepareCustomIdGenerator('awsConnection', 'triggerUuid', 'displayName');
            </script>
        </c:when>
        <c:otherwise>
            <props:hiddenProperty name="triggerUuid" value="${triggerUuid}"/>
            <label style="word-break: break-all;">${triggerUuid}</label>
        </c:otherwise>
    </c:choose>
    <span class="error" id="error_triggerUuid"></span>
</td>

<c:if test="${not empty currentBuildTypeExternalId}">
    <l:settingsGroup title="HTTP(S) Endpoint"/>
    <tr>
        <td colspan="2">
            <label class="rightLabel">Use this endpoint for HTTP(S) <a
                    href="https://console.aws.amazon.com/sns/v3/home#/create-subscription">subscription</a>:
            </label><br>
            <bs:copy2ClipboardLink dataId="triggerUrl" title="Copy to clipboard" stripTags="true">
                <c:out value="${endpointUrl}"/>
            </bs:copy2ClipboardLink>
        </td>
    </tr>

    <%
        String topicArn = null;
        String topicSubscriptionArn = null;
        String topicUnsubscriptionUrl = null;

        BuildTriggerInfo triggerInfo = triggerDescriptorBean.getSelectedTrigger();

        if (triggerInfo != null) {
            BuildTriggerDescriptor triggerDescriptor = triggerInfo.getOriginalTrigger();
            SBuildType buildType = buildForm.getSettingsBuildType();

            if (triggerDescriptor != null && buildType != null) {
                CustomDataStorage cds = PolledTriggerContextImpl.getCustomDataStorage(buildType, triggerDescriptor);
                topicArn = cds.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_TOPIC_ARN);
                topicSubscriptionArn = cds.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_SUBSCRIPTION_ARN);
                topicUnsubscriptionUrl = cds.getValue(AwsSnsTriggerConstants.TRIGGER_STORE_CURRENT_UNSUBSCRIBE_URL);
            }
        }
    %>

    <c:if test="<%=triggerInfo != null%>">
        <l:settingsGroup title="Subscription Info"/>
        <c:choose>
            <c:when test="<%=Strings.isBlank(topicSubscriptionArn)%>">
                <tr>
                    <td><label>Pending subscription...</label></td>
                </tr>
            </c:when>
            <c:otherwise>
                <tr>
                    <td><label>Topic ARN:</label></td>
                    <td><label style="word-break: break-all;"><%=topicArn%>
                    </label></td>
                </tr>
                <tr>
                    <td><label>Subscription ARN:</label></td>
                    <td><label style="word-break: break-all;"><%=topicSubscriptionArn%>
                    </label></td>
                </tr>
                <c:if test="<%=Objects.nonNull(topicUnsubscriptionUrl)%>">
                    <tr>
                        <td>
                            <a href="<%=topicUnsubscriptionUrl%>">Unsubscribe</a>
                        </td>
                    </tr>
                </c:if>
            </c:otherwise>
        </c:choose>
    </c:if>

    <script type="text/javascript">
        {
            const triggerNameField = jQuery('#displayName');
            const triggerIdField = jQuery('#triggerUuid');
            const reference = jQuery('a.copy2Clipboard');

            function updateUrl() {
                if (triggerIdField) {
                    const triggerId = triggerIdField.val();
                    const url = reference.text().strip().replace(/\/([\w\-._]+)?$/, "/");
                    reference.text(url + triggerId);
                }
            }

            if (triggerNameField) triggerNameField.on('keydown', () => setTimeout(updateUrl, 200));
            if (triggerIdField) triggerIdField.on('keydown', () => setTimeout(updateUrl, 200));

            setTimeout(updateUrl, 200)
        }
    </script>
</c:if>
