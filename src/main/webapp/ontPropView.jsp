<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="io.bdrc.ldspdi.ontology.service.core.*"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Ontology Property - ${model.getUri()}</title>
<style>
#specs {
    font-family: "Trebuchet MS", Arial, Helvetica, sans-serif;
    border-collapse: collapse;
    width: 95%;
}
#specs td, #customers th {
    border: 0px solid #ddd;
    padding: 8px;
}

#specs tr:nth-child(even){background-color: #f2f2f2;}

#specs tr:hover {background-color: #ddd;}

#specs th {
    padding-top: 12px;
    padding-left: 12px;
    padding-bottom: 12px;
    text-align: left;
    background-color: #4e7F50;
    color: white;
}
.lang {
    position: relative;
    margin: 0;
    vertical-align: -5px;
    padding: 3px;
    line-height: 0;
    font-size: 12px;
    color: #778899;
}
</style>
</head>
<body>
<h2>Ontology Property - <a href="${model.getUri()}">${model.getName()}</a></h2>
<b>Type:</b> <a href="${model.getRdfTypeUri()}">${model.getRdfType()}</a><br>
<b>Label:</b> ${model.getLabel()}<span class="lang">${model.getLabelLang()}</span><br>
<c:choose>
<c:when test="${model.isDomainInherited()}">
<b>Domain:</b> ${model.getDomain()}<br>
</c:when>
<c:otherwise>
<b>Domain:</b> <a href="${model.getDomainUri()}">${model.getDomain()}</a><br>
</c:otherwise>
</c:choose>
<c:choose>
<c:when test="${model.isRangeInherited()}">
<b>Range:</b> ${model.getRange()}<br>
</c:when>
<c:otherwise>
<b>Range:</b> <a href="${model.getRangeUri()}">${model.getRange()}</a><br>
</c:otherwise>
</c:choose>
<b>Comment:</b>${model.getComment()}<span class="lang">${model.getCommentLang()}</span><br>
<br>
<!-- SUB PROPS -->
    <c:if test = "${model.getAllSubProps().size()>0}">
        <h3>Sub properties: </h3>
        
        <c:forEach items="${model.getAllSubProps()}" var="prop"> 
        <table id="specs" style="width:60%;"> 
        <tr><th></th><th>${prop.getName()}</th></tr>          
            <tr><td><b>Uri:</b></td><td> <a href="${prop.getUri()}">${prop.getUri()}</a></td></tr>            
            <tr><td><b>Type:</b></td><td> <a href="${prop.getRdfTypeUri()}">${prop.getRdfType()}</a></td></tr>
            <tr><td><b>Label:</b></td><td> ${prop.getLabel()}<span class="lang">${prop.getLabelLang()}</span></td></tr>
            <c:choose>
				<c:when test="${prop.isDomainInherited()}">
				<tr><td><b>Domain:</b></td><td> ${prop.getDomain()}</td></tr>
				</c:when>
				<c:otherwise>
                <tr><td><b>Domain:</b></td><td> <a href="${prop.getDomainUri()}">${prop.getDomain()}</a></td></tr>
                </c:otherwise>
            </c:choose> 
            <c:choose>
				<c:when test="${prop.isRangeInherited()}">
				<tr><td><b>Range:</b></td><td> ${prop.getRange()}</td></tr>
				</c:when>
				<c:otherwise>           
                <tr><td><b>Range:</b></td><td> <a href="${prop.getRangeUri()}">${prop.getRange()}</a></td></tr>
                </c:otherwise>
            </c:choose>
            </table><br>          
        </c:forEach>
        
    </c:if> 
    
    <!-- PARENT PROPS -->
    <c:if test = "${model.getParentProps().size()>0}">
        <h3>Parent properties: </h3>
        
        <c:forEach items="${model.getParentProps()}" var="prop"> 
        <table id="specs" style="width:60%;"> 
        <tr><th></th><th>${prop.getName()}</th></tr>          
            <tr><td><b>Uri:</b></td><td> <a href="${prop.getUri()}">${prop.getUri()}</a></td></tr>            
            <tr><td><b>Type:</b></td><td> <a href="${prop.getRdfTypeUri()}">${prop.getRdfType()}</a></td></tr>
            <tr><td><b>Label:</b></td><td> ${prop.getLabel()}<span class="lang">${prop.getLabelLang()}</span></td></tr>
            <c:choose>
                <c:when test="${prop.isDomainInherited()}">
                <tr><td><b>Domain:</b></td><td> ${prop.getDomain()}</td></tr>
                </c:when>
                <c:otherwise>
                <tr><td><b>Domain:</b></td><td> <a href="${prop.getDomainUri()}">${prop.getDomain()}</a></td></tr>
                </c:otherwise>
            </c:choose>            
            <c:choose>
                <c:when test="${prop.isRangeInherited()}">
                <tr><td><b>Range:</b></td><td> ${prop.getRange()}</td></tr>
                </c:when>
                <c:otherwise>           
                <tr><td><b>Range:</b></td><td> <a href="${prop.getRangeUri()}">${prop.getRange()}</a></td></tr>
                </c:otherwise>
            </c:choose>
            </table><br>          
        </c:forEach>
        
    </c:if>     
</body>
</html>