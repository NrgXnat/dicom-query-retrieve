<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="org.nrg.xdat.security.helpers.UserHelper" %>
<%--<%@ page import="org.nrg.xdat.display.DisplayManager" %>--%>
<%--<%@ page import="org.nrg.xapi.rest.users.UsersApi" %>--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="pg" tagdir="/WEB-INF/tags/page" %>

<c:set var="SITE_ROOT" value="${sessionScope.siteRoot}"/>

<c:set var="loggedIn" value="false"/>
<c:set var="isAllowed" value="false"/>

<sec:authorize access="isFullyAuthenticated()">
    <c:set var="loggedIn" value="true"/>
    <sec:authorize access="hasAnyRole('ADMIN')">
        <c:set var="isAllowed" value="true"/>
    </sec:authorize>
</sec:authorize>

<%-- accept ?project=PROJ or ?id=PROJ or ?projectId=PROJ --%>
<c:set var="projectId" value="${not empty param.project ? param.project : (not empty param.id ? param.id : (not empty param.projectId ? param.projectId : ''))}"/>
<%-- escape passed param --%>
<c:set var="projectId" value="${fn:escapeXml(projectId)}"/>

<c:if test="${empty projectId}">
    <div class="error">Project not specified.</div>
</c:if>

<c:if test="${not empty projectId}">

    <%--@elvariable id="userHelper" type="org.nrg.xdat.security.helpers.UserHelper"--%>
    <c:if test="${userHelper.canEdit('xnat:subjectData/project', projectId)}">
        <c:set var="isAllowed" value="true"/>
        <script>window.isAllowed = true</script>
    </c:if>

    <c:if test="${isAllowed == false}">
        <c:if test="${loggedIn == false}">
            <div class="error">Not logged in. Redirecting...</div>
        </c:if>
        <c:if test="${loggedIn == true}">
            <div class="error">Not authorized. Redirecting...</div>
        </c:if>
        <script>
            window.setTimeout(function(){
                window.location.href = '${SITE_ROOT}/'
            }, 2000)
        </script>
    </c:if>

    <c:if test="${isAllowed == true}">
        <div id="page-body" class="hidden">
            <div class="pad">

                <%@ include file="view.html" %>

            </div>
        </div>
        <!-- /#page-body -->

        <div id="xnat-scripts"></div>
        <script>
            window.projectId =
                '${projectId}' ||
                getQueryStringValue('project');
        </script>

        <script src="${SITE_ROOT}/page/dqr/import/view.js"></script>

        <%--@elvariable id="displayManager" type="org.nrg.xdat.display.DisplayManager"--%>
        <%--<c:set var="subjectDisplay" value="${displayManager.singularDisplayNameForSubject}"/>--%>
        <%--<c:set var="sessionDisplay" value="${displayManager.singularDisplayNameForImageSession}"/>--%>
        <%--<script>--%>
            <%--window.subjectDisplay = '${subjectDisplay}' || 'Subject';--%>
            <%--window.sessionDisplay = '${sessionDisplay}' || 'Session';--%>
            <%--window.subjectDisplayLower = window.subjectDisplay.toLowerCase();--%>
            <%--window.sessionDisplayLower = window.sessionDisplay.toLowerCase();--%>
        <%--</script>--%>

    </c:if>
</c:if>
