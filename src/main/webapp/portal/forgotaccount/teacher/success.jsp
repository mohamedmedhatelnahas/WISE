<%@ include file="../../include.jsp"%>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<link rel="shortcut icon" href="${contextPath}/<spring:theme code="favicon"/>" />
<title><spring:message code="forgotaccount.teacher.success.teacherForgottenPasswordConfirmationScreen"/></title>

<link href="${contextPath}/<spring:theme code="globalstyles"/>" media="screen" rel="stylesheet"  type="text/css" />

</head>
<body>
<div id="pageWrapper">
	
	<div id="page">
		
		<div id="pageContent" style="min-height:400px;">
			<div id="headerSmall">
				<a id="name" href="${contextPath}/index.html" title="<spring:message code="wiseHomepage" />"><spring:message code="wise" /></a>
			</div>
			
			<div class="infoContent">
				<div class="panelHeader"><spring:message code="forgotaccount.teacher.success.lostUsernamePassword"/></div>
				<div class="infoContentBox">
					<div>
						<spring:message code="forgotaccount.teacher.success.anEmailHasBeenSent"/>
					</div>
					<div>
						<spring:message code="forgotaccount.teacher.success.ifYouHaveAnyOtherProblemsOrQuestions"/> <a href="${contextPath}/contact/contactwise.html"> <spring:message code="forgotaccount.teacher.success.contactWISE"/></a>
					</div>
				</div>
				<a href="${contextPath}/index.html" title="<spring:message code="wiseHome" />"><spring:message code="returnHome"/></a>
			</div>
		</div>
	</div>
</div>
</body>
</html>