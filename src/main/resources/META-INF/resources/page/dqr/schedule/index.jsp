<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="pg" tagdir="/WEB-INF/tags/page" %>

<%--
  ~ dicom-query-retrieve: index.jsp
  ~ XNAT http://www.xnat.org
  ~ Copyright (c) 2005-2020, Washington University School of Medicine
  ~ All Rights Reserved
  ~
  ~ Released under the Simplified BSD.
  --%>

<pg:wrapper>
    <pg:xnat>

        <jsp:include page="content.jsp"/>

    </pg:xnat>
</pg:wrapper>
