package io.bdrc.ldspdi.rest.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;

import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ontology.service.core.OntClassModel;

@Path("/")
@Template
public class DemoResources {
    
    public static Logger log=Logger.getLogger(PublicDataResource.class.getName());
    
    public DemoResources() {
        super();
        log.addHandler(new ConsoleHandler());
        ResourceConfig config=new ResourceConfig(DemoResources.class);
        config.register(LoggingFeature.class);
        config.property(JspMvcFeature.TEMPLATE_BASE_PATH, "").register(JspMvcFeature.class);
    } 
    
    @GET 
    @Produces(MediaType.TEXT_HTML)    
    public Viewable getJsonContext() throws IOException{
        log.info("Call to getJsonContext()"); 
        //return ServiceConfig.JSONLD_CONTEXT_HTML; 
        return new Viewable("/index.jsp"); 
    } 
       
    @GET 
    @Path("/demo/{file}")
    @Produces(MediaType.TEXT_HTML)
    public Viewable templateRelative(@PathParam("file") String file) {
        log.info("Call to templateRelative()");
        log.info("Called file -->"+file);
        return new Viewable("/"+file);         
    } 
    
    @GET
    @Path("demo/ontology")
    @Produces("text/html")
    public Viewable getOntologyClassView(@QueryParam("classUri") String uri) {
        log.info("Call to getOntologyClassView()");          
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("model", new OntClassModel(uri)); 
        return new Viewable("/ontClass.jsp", map);        
    }
    

}
