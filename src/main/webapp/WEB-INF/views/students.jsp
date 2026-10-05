<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
  <title>Students (XML + Hibernate)</title>
  <style>
    body { font-family: sans-serif; max-width: 720px; margin: 2rem auto; }
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #ccc; padding: 6px 10px; text-align: left; }
    form.add input { margin-right: 6px; }
  </style>
</head>
<body>
  <h1>Students <small>(classic Spring MVC + Hibernate, XML config)</small></h1>
  <table>
    <tr><th>ID</th><th>Name</th><th>Email</th><th>Course</th><th></th></tr>
    <c:forEach var="s" items="${students}">
      <tr>
        <td>${s.id}</td>
        <td><c:out value="${s.name}"/></td>
        <td><c:out value="${s.email}"/></td>
        <td><c:out value="${s.course}"/></td>
        <td>
          <form method="post" action="${pageContext.request.contextPath}/students/${s.id}/delete">
            <button type="submit">Delete</button>
          </form>
        </td>
      </tr>
    </c:forEach>
  </table>
  <h3>Add student</h3>
  <form class="add" method="post" action="${pageContext.request.contextPath}/students">
    <input name="name" placeholder="Name" required>
    <input name="email" placeholder="Email" required>
    <input name="course" placeholder="Course">
    <button type="submit">Add</button>
  </form>
  <p><a href="${pageContext.request.contextPath}/db-check">DB check</a></p>
</body>
</html>
