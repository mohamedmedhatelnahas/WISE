<%@ include file="../../include.jsp"%>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<link rel="shortcut icon" href="${contextPath}/<spring:theme code="favicon"/>" />
<title><spring:message code="wiseAdmin" /></title>

<link href="${contextPath}/<spring:theme code="globalstyles"/>" media="screen" rel="stylesheet"  type="text/css" />
<link href="${contextPath}/<spring:theme code="stylesheet"/>" media="screen" rel="stylesheet" type="text/css" />
<link href="${contextPath}/<spring:theme code="jquerystylesheet"/>" media="screen" rel="stylesheet" type="text/css" >
<link href="${contextPath}/<spring:theme code="superfishstylesheet"/>" rel="stylesheet" type="text/css" >

<script src="${contextPath}/<spring:theme code="jquerysource"/>" type="text/javascript"></script>
<script src="${contextPath}/<spring:theme code="jqueryuisource"/>" type="text/javascript"></script>
<script src="${contextPath}/<spring:theme code="superfishsource"/>" type="text/javascript"></script>
<script src="${contextPath}/<spring:theme code="generalsource" />" type="text/javascript"></script>

<style>
table th {
  font-weight:bold;
  border: 1px solid black;
  padding:3px;
}
table, tr, td {
  border: 1px solid black;
  padding:3px;
}
</style>
</head>
<body>
<div id="page">
<div id="pageContent" class="contentPanel">

<%@ include file="../../headermain.jsp"%>

<h5 style="color:#0000CC;"><a href="${contextPath}/admin/index.html"><spring:message code="returnToMainAdminPage" /></a></h5>

<br/>
	<c:choose>
		<c:when test="${fn:length(openedProjectIds) > 0}">
			<table>
				<tr>
					<th>Project Name (ID)</th>
					<th>Authors</th>
				</tr>
				<c:forEach var="openedProjectId" items="${openedProjectIds}">
					<tr>
						<td><a target=_blank href="../../previewproject.html?projectId=${openedProjectId}">${openedProjects[openedProjectId].name} (${openedProjectId})</a></td>
						<td><c:forEach var="sessionOpeningThisProject"
								items="${openedProjectsToSessions[openedProjectId]}">
								<c:set var="username" value="${loggedInTeachers[sessionOpeningThisProject].userDetails.username}"></c:set>
								<c:out value="${username}" /> | 
							<a href="../../j_acegi_switch_user?j_username=${username}">Log in as this user</a> |
							<a href="#" onclick="javascript:popup640('../../teacherinfo.html?userName=${username}');">info</a>
							</c:forEach>
						</td>
					</tr>
				</c:forEach>
			</table>
		</c:when>
		<c:otherwise>
			<c:out value="Nobody is authoring at this time." />
		</c:otherwise>
	</c:choose>
</div></div>

</body>
</html>