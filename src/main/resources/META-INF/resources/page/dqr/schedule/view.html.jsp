<!-- #*
<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="pg" tagdir="/WEB-INF/tags/page" %>
*# -->

<!-- #* <%-- -->
<!--
The 'view.html' file should be formatted so it can be
included and parsed in both JSP and Velocity parent pages.
-->
<!-- --%> *# -->

<!-- #*
<jsp:include page="/page/dqr/_incl/init.jsp"/>
<c:set var="SITE_ROOT" value="${sessionScope.siteRoot}"/>
<c:set var="pacsId" value="${not empty param.pacs ? param.pacs : ''}"/>
<c:set var="pacsLabel" value="${not empty param.label ? param.label : ''}"/>
*# -->

<!-- <%-- -->
#set ($SITE_ROOT = $content.getURI(""))
#set ($pacsId = $!turbineUtils.escapeHTML($!data.getParameters().getString('pacs')))
#set ($pacsLabel = $!turbineUtils.escapeHTML($!data.getParameters().getString('label')))
<!-- --%> -->


<!-- #* <%-- -->
Hide JSP tags from the Velocity parser.
<!-- --%> *# -->

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

            /* style the dialog conainer? */
            .load-content { margin: 10px; }

            #pacs-schedule-view { width: auto; }

            #pacs-schedule-view > .pad {
                padding: 20px; overflow: auto;
            }

            #pacs-schedule-view .time-block {
                display: inline-block;
                /*margin-right: 1px;*/
                overflow: visible;
                position: relative;
            }

            #pacs-schedule-times,
            #pacs-schedule-days {
                position: relative;
            }

            .day-row {
                margin-bottom: 15px;
                /*border-collapse: collapse; */
            }

            .day-row .day-label b {
                display: block;
                width: 50px;
                font-size: 16px;
                font-weight: normal;
                line-height: 60px;
                vertical-align: middle;
                background: #fff;
            }

            .day-row .day-hours {
                position: relative;
                /*padding: 2px 1px 2px 2px;*/
                /*overflow: auto;*/
                /*background: rgba(171, 226, 255, 0.5);*/
                background: #f0f0f0;
                border-right: 1px solid #acd2e6;
            }

            .day-row .time-block {
                position: absolute;
                /*float: left;*/
                /*margin-right: 1px;*/
            }

            .day-row .time-block-color {
                /*min-width: 38px;*/
                height: 60px;
                background: #5cc4ef;
                border: 1px solid #acd2e6;
                border-right: none;
                margin-left: -1px;
            }

            .day-row .thread-count {
                position: absolute;
                top: 5px;
                left: 5px;
                white-space: nowrap;
                z-index: 2;
                padding: 2px 8px 1px;
                background: #339933;
                color: #fff;
                border-radius: 16px;
                border: 1px solid #228822;
                font-size: 12px;
            }

            .day-row .thread-count:hover {
                z-index: 3;
                box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
            }

            .day-row .thread-count > span {
                display: none;
            }

            .day-row .thread-count:hover > span {
                display: inline;
            }

            .day-row .add-interval {
                padding: 20px 0 0 5px;
            }

            .day-row .add-interval i.fa {
                font-size: 24px;
                color: #228822;
            }

            .hours-label-row {
                position: relative;
                left: -20px;
                margin: 10px 0;
                /*border-collapse: collapse; */
            }

            .hours-label-row .day-label b {
                display: block;
                width: 50px;
                height: 20px;
                font-weight: normal;
            }

            .hours-label-row .time-block {
                margin-right: 1px;
            }

            .hours-label-row .time-block-label {
                width: 38px;
                height: 20px;
                background: #fff;
                text-align: center;
            }

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

        <script src="${SITE_ROOT}/scripts/dqr/schedule.js"></script>

        <!-- #* -->
    <!-- </pg:restricted> -->
<!-- </c:catch> -->
<!--
<c:if test="${not empty jspError}">
    ${jspError}
</c:if>
-->
<!-- *# -->
