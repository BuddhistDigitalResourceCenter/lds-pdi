<%@page import="io.bdrc.ldspdi.service.*"%>
<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
#specs {
    font-family: "Trebuchet MS", Arial, Helvetica, sans-serif;
    border-collapse: collapse;
    width: 95%;
}

#specs td, #customers th {
    border: 1px solid #ddd;
    padding: 8px;
}

#specs tr:nth-child(even){background-color: #f2f2f2;}

#specs tr:hover {background-color: #ddd;}

#specs th {
    padding-top: 12px;
    padding-bottom: 12px;
    text-align: left;
    background-color: #4e7F50;
    color: white;
}
</style>
<script type="text/javascript">
function onto(){
    var x = document.getElementById("uri1").value+"."+document.getElementById("format").value;
    x = x.substring(5);
    window.location.assign(x);
}
function browse(){
    var x = document.getElementById("uri").value;
    x = x.substring(5);
    window.location.assign(x);
}
</script>
<title>${ServiceConfig.getProperty("brandName")} Public data Interface</title>
</head>
<body>
<h1>${ServiceConfig.getProperty("brandName")} Public Data Interface</h1>
        <p>This resource provides direct data access to the ${ServiceConfig.getProperty("ontName")} Library</p>        
    
<h2>Navigate through ${ServiceConfig.getProperty("ontName")}</h2>
<p>You can use this service to access the current ${ServiceConfig.getProperty("ontName")} and discover the data model:</p>
<p>ONTOLOGY SERVICE</p>
<div> Browse an ontology : 
<select id="uri">
<c:forEach items="${model.getOntos()}" var="k">
  <option value="${k}">${k}</option>  
</c:forEach>
</select>
<button onclick="javascript:browse();" type="button"> Browse </button>
</div> 

</br>
<div> View/download ontology files: 
<select id="uri1">
<c:forEach items="${model.getOntos()}" var="k">
  <option value="${k}">${k}</option>  
</c:forEach>
</select>
Format:
<select id="format">
  <option value="ttl">text/turtle=ttl</option>
  <option value="rdf">application/rdf+xml=rdf</option>
  <option value="owl">application/owl+xml=owl</option>
  <option value="json">application/json=json</option>
  <option value="jsonld">application/ld+json=jsonld</option>
  <option value="nt">application/n-triples=nt</option>
  <option value="trix">application/trix+xml=trix</option>
</select> 
<button onclick="javascript:onto();" type="button"> View </button>
</div>

<h2>Instructions</h2>
<p>Public queries are run via urls whose specifications are given below. However, you can get any resource turtle representation 
using this general url format:</p>
<p><a href="/resource/P1583.ttl">http://serverName:portNumber/resource/P1583</a></p>
<p>where P1583 is a BDRC resource ID</p>
<div align="center"><h2>Url specifications by query types</h2></div>
<div align="center">

<c:forEach items="${model.getKeys()}" var="k">
    <c:set var="val" value="${k}"/>
    <div align="center">
       <h2>${k}</h2>
       <table id="specs">       
            <tr>
                <th>Search type</th>
                <th>Return type</th>
                <th style="width:400px;">Result set</th>
                <th>Parameter(s)</th>
                <th>Url format</th>
            </tr>           
            <c:forEach items="${model.getTemplates(val)}" var="tmpl">
            <tr>
                <td><b>${tmpl.id}</b></td>
                <td>${tmpl.queryReturn}</td>
                <td style="width:400px;">${tmpl.queryResults}</td>
                <td>${tmpl.queryParams}</td>
                <td><a href="${tmpl.demoLink}">${tmpl.demoLink}</a></td>
            </tr>                          
            </c:forEach>
       </table>
    </div>
</c:forEach>
</div>
<br><br><hr><br>
<p align="center">Powered by <a href="https://github.com/buda-base/lds-pdi">ldspdi</a></p>
</body>
</html>