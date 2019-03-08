<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<c:if test="${empty requestScope.hasInit}">

    <%-- set 'siteRoot' to the root of your web app --%>
    <c:set var="siteRoot" value="${pageContext.request.contextPath}" scope="session"/>

    <%-- add a leading slash if siteRoot is not empty and doesn't already start with a slash --%>
    <c:if test="${siteRoot != '' && !fn:startsWith(siteRoot,'/')}">
        <c:set var="siteRoot" value="/${pageContext.request.contextPath}" scope="session"/>
    </c:if>

    <c:set var="hasInit" value="true" scope="request"/>

</c:if>
