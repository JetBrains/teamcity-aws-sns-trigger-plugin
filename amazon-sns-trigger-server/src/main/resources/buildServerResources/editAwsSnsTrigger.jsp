<%@ include file="include.jsp" %>
<%@ page import="jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants" %>
<%@ page import="jetbrains.buildServer.web.util.WebUtil" %>

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

<c:choose>
    <c:when test="${empty currentBuildTypeExternalId}">
        <tr>
            <td colspan="2">
                <em>Amazon SNS Trigger can be configured only in active Build Configuration. Templates aren't
                    supported.</em>
            </td>
        </tr>
    </c:when>
    <c:otherwise>
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
                <c:when test="${empty triggerUuid}">
                    <props:textProperty name="triggerUuid" value=""/>
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
        <span class="error" id="error_triggerBuildTypeExternalId"></span>

        <props:hiddenProperty name="triggerBuildTypeExternalId" value="${currentBuildTypeExternalId}"/>
    </c:otherwise>
</c:choose>

<script type="text/javascript">
    const triggerNameField = jQuery('#displayName');
    const triggerIdField = $('triggerUuid');
    const reference = jQuery('a.copy2Clipboard');

    function updateUrl() {
        if (triggerIdField) {
            const triggerId = triggerIdField.value;
            const url = reference.text().strip().replace(/\/([\w\-]+)?$/, "/");
            reference.text(url + triggerId);
        }
    }

    if (triggerNameField) triggerNameField.on('keydown', () => setTimeout(updateUrl, 200));

    setTimeout(updateUrl, 200)
</script>