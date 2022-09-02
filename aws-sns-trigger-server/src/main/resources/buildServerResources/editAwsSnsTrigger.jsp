<%@ include file="include.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="buildForm" type="jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm"
             scope="request"/>

<c:set var="currentProjectExternalId"
><c:if test="${not empty propertiesBean.properties['triggerProjectExternalId']}"
><c:out value="${propertiesBean.properties['triggerProjectExternalId']}"/></c:if
><c:if test="${empty propertiesBean.properties['triggerProjectExternalId']}"
><c:out value="${buildForm.project.externalId}"/></c:if></c:set>

<c:set var="currentBuildTypeExternalId"
><c:if test="${not empty propertiesBean.properties['triggerBuildTypeExternalId']}"
><c:out value="${propertiesBean.properties['triggerBuildTypeExternalId']}"/></c:if
><c:if test="${empty propertiesBean.properties['triggerBuildTypeExternalId']}"
><c:out value="${buildForm.settingsBuildType.externalId}"/></c:if></c:set>

<c:set var="triggerUuid" value="${propertiesBean.properties['triggerUuid']}"/>
<c:set var="urlPathPart" value="${propertiesBean.properties['urlPathPart']}"/>
<c:set var="hostPart" value="${propertiesBean.properties['rootUrl']}"/>

<c:set var="triggerUrl"
       value="${urlPathPart.replace('/**', '/')}${currentProjectExternalId}/${currentBuildTypeExternalId}"/>

<c:set var="endpointUrl"
><c:if test="${not empty propertiesBean.properties['triggerEndpointUrl']}"
><c:out value="${propertiesBean.properties['triggerEndpointUrl']}"/></c:if
><c:if test="${empty propertiesBean.properties['triggerEndpointUrl']}"
><c:out value="${hostPart}${triggerUrl}/${triggerUuid}"/></c:if></c:set>

<tr>
    <td colspan="2">
        <em>AWS SNS Trigger will add a build to the queue when an SNS message received.</em>
    </td>
</tr>

<l:settingsGroup title="HTTP(S) Endpoint"/>
<tr>
    <td colspan="2">
        <label class="rightLabel">Use this endpoint for HTTP(S) <a
                href="https://console.aws.amazon.com/sns/v3/home#/create-subscription">subscription</a>:
        </label><br>
        <bs:copy2ClipboardLink dataId="${endpointUrl}" title="Copy to clipboard" stripTags="true">
            <c:out value="${endpointUrl}"/>
        </bs:copy2ClipboardLink>
    </td>
</tr>

<props:hiddenProperty name="triggerEndpointUrl" value="${endpointUrl}"/>
<props:hiddenProperty name="triggerUuid" value="${triggerUuid}"/>
<props:hiddenProperty name="triggerProjectExternalId" value="${currentProjectExternalId}"/>
<props:hiddenProperty name="triggerBuildTypeExternalId" value="${currentBuildTypeExternalId}"/>

<script type="text/javascript">
</script>