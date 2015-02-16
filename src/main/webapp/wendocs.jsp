<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ page import="com.google.appengine.api.datastore.DatastoreService" %>
<%@ page import="com.google.appengine.api.datastore.DatastoreServiceFactory" %>
<%@ page import="com.google.appengine.api.datastore.Entity" %>
<%@ page import="com.google.appengine.api.datastore.FetchOptions" %>
<%@ page import="com.google.appengine.api.datastore.Key" %>
<%@ page import="com.google.appengine.api.datastore.KeyFactory" %>
<%@ page import="com.google.appengine.api.datastore.Query" %>
<%@ page import="java.util.List" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css"/>
</head>

<body>

<%
    String listName = request.getParameter("listName");
    if (listName == null) {
        listName = "default";
    }
    pageContext.setAttribute("listName", listName);
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    if (user != null) {
        pageContext.setAttribute("user", user);
%>

<p>Hello, ${fn:escapeXml(user.nickname)}! (You can
    <a href="<%= userService.createLogoutURL(request.getRequestURI()) %>">sign out</a>.)</p>
<%
} else {
%>
<p>Hello!
    <a href="<%= userService.createLoginURL(request.getRequestURI()) %>">Sign in</a>
    to include your name with greetings you post.</p>
<%
    }
%>

<%
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key listKey = KeyFactory.createKey("List", listName);
    // Run an ancestor query to ensure we see the most up-to-date
    // view of the Greetings belonging to the selected List.
    Query query = new Query("Greeting", listKey).addSort("date", Query.SortDirection.DESCENDING);
    List<Entity> greetings = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(5));
    if (greetings.isEmpty()) {
%>
<p>List '${fn:escapeXml(listName)}' has no messages.</p>
<%
    } else {
%>
<p>Messages in List '${fn:escapeXml(listName)}'.</p>
<%
        for (Entity greeting : greetings) {
            pageContext.setAttribute("greeting_content",
                    greeting.getProperty("content"));
            String author;
            if (greeting.getProperty("author_email") == null) {
                author = "An anonymous person";
            } else {
                author = (String)greeting.getProperty("author_email");
                String author_id = (String)greeting.getProperty("author_id");
                if (user != null && user.getUserId().equals(author_id)) {
                    author += " (You)";
                }
            }
            pageContext.setAttribute("greeting_user", author);
%>
<p><b>${fn:escapeXml(greeting_user)}</b> wrote:</p>
<blockquote>${fn:escapeXml(greeting_content)}</blockquote>
<%
        }
    }
%>

<form action="/updateList" method="post">
    <div><textarea name="content" rows="3" cols="60"></textarea></div>
    <div><input type="submit" value="Post Greeting"/></div>
    <input type="hidden" name="listName" value="${fn:escapeXml(listName)}"/>
</form>

<form action="/wendocs.jsp" method="get">
    <div><input type="text" name="listName" value="${fn:escapeXml(listName)}"/></div>
    <div><input type="submit" value="Switch List"/></div>
</form>

</body>
</html>
