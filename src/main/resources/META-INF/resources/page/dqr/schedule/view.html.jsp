<%--
  ~ dicom-query-retrieve: view.html.jsp
  ~ XNAT http://www.xnat.org
  ~ Copyright (c) 2005-2020, Washington University School of Medicine
  ~ All Rights Reserved
  ~
  ~ Released under the Simplified BSD.
  --%>

<!-- #*
<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="pg" tagdir="/WEB-INF/tags/page" %>
*# -->

<!-- #* <%--
The 'view.html' file should be formatted so it can be
included and parsed in both JSP and Velocity parent pages.
--%> *# -->

<!-- #*
<jsp:include page="/page/dqr/_incl/init.jsp"/>
<c:set var="SITE_ROOT" value="${sessionScope.siteRoot}"/>
<c:set var="pacsId" value="${not empty param.pacs ? fn:escapeXml(param.pacs) : ''}"/>
<c:set var="pacsLabel" value="${not empty param.label ? fn:escapeXml(param.label) : ''}"/>
*# -->

<!-- <%--
#set ($SITE_ROOT = $content.getURI(""))
#set ($pacsId = $!turbineUtils.escapeHTML($!data.getParameters().getString('pacs')))
#set ($pacsLabel = $!turbineUtils.escapeHTML($!data.getParameters().getString('label')))
--%> -->


<!-- #* <%--
Hide JSP tags from the Velocity parser.
--%> *# -->

<!-- #* -->
<!-- <c:catch var="jspError"> -->
    <!-- <pg:restricted msg="No access."> -->
        <!-- *# -->

        <header id="content-header">
            <div class="pad" style="padding:0;">
                <h2 style="margin:0 0 20px 0;">Utilization Schedule for <span class="pacs-label"></span> PACS</h2>
                <div class="info">
                    Use these settings to customize transfer load for specified time intervals when importing data
                    from PACS. Darker shades indicate a heavier transfer load, lighter shades indicate a lighter load.
                </div>
                <div class="info alt hidden">
                    Customize the data transfer utilization rate between {XNAT_name} and {PACS_name} by creating a
                    weekly schedule for uptime, downtime, and throttling of data transfer speeds
                </div>
            </div>
        </header>

        <style>

            /* style the dialog container? */
            .load-content { margin: 10px; }

            /* remove border and margin from panel in dialog */
            body.xnat .xnat-dialog .panel {
                border: none;
                margin: 0;
            }

        </style>

        <div id="pacs-schedule-view">
            <div class="pad">

                <header id="pacs-schedule-times">
                    <!-- hour indicators -->
                </header>

                <div id="pacs-schedule-days">
                    <!-- day rows -->
                </div>

            </div>
        </div>

        <script>

            window.pacsId =
                window.pacsId ||
                '${pacsId}' ||
                getQueryStringValue('pacs') ||
                getUrlHashValue('#pacs=');

            window.pacsLabel =
                window.pacsLabel ||
                '${pacsLabel}' ||
                getQueryStringValue('label') ||
                getUrlHashValue('#label=');

        </script>

        <!-- #* <%-- the #[[ ... ]]# syntax prevents Velocity from parsing the enclosed code --%> *# -->

        <!-- #[[ -->
        <script>
            $('.pacs-label').text('"' + window.pacsLabel + '"');
        </script>
        <!-- ]]# -->

        <script src="${SITE_ROOT}/scripts/xnat/plugin/dqr/schedule.js"></script>

        <!-- #* -->
    <!-- </pg:restricted> -->
<!-- </c:catch> -->
<!--
<c:if test="${not empty jspError}">
    ${jspError}
</c:if>
-->
<!-- *# -->
