<%@ page session="false" %>
<%
  response.setStatus(302);
  response.setHeader("Location", "/flowdesk/");
  response.setHeader("Cache-Control", "no-store");
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta http-equiv="refresh" content="0;url=/flowdesk/">
<title>FlowDesk</title>
<script>location.replace('/flowdesk/');</script>
</head>
<body>
<p><a href="/flowdesk/">FlowDesk로 이동</a></p>
</body>
</html>
