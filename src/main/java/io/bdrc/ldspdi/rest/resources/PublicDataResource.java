package io.bdrc.ldspdi.rest.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.text.StrSubstitutor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.vocabulary.SKOS;
import org.glassfish.jersey.server.mvc.Viewable;

import io.bdrc.ldspdi.sparql.PreparedQuery;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ontology.service.core.OntClassModel;
import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.jena.sttl.CompareComplex;
import io.bdrc.jena.sttl.ComparePredicates;
import io.bdrc.jena.sttl.STTLWriter;
import io.bdrc.ldspdi.parse.QueryFileParser;
import io.bdrc.ldspdi.service.ServiceConfig;

@Path("/")
public class PublicDataResource {

	QueryProcessor processor=new QueryProcessor();
	public String fusekiUrl=ServiceConfig.getProperty("fuseki");
	
	@GET	
	public Response getData(@Context UriInfo info) throws Exception{		
		String baseUri=info.getBaseUri().toString();
		MediaType media=new MediaType("text","html","utf-8");
		MultivaluedMap<String,String> mp=info.getQueryParameters();
		String filename= mp.getFirst("searchType")+".arq";		
		QueryFileParser qfp;
		final String query;
		
		qfp=new QueryFileParser(filename);
		String q=qfp.getQuery();
		String check=qfp.checkQueryArgsSyntax();
		if(check.length()>0) {
			throw new Exception("Exception : File->"+ filename+".arq; ERROR: "+check);
		}
		PreparedQuery prep_query=new PreparedQuery(q,mp);		
		query=prep_query.getPreparedQuery();		
		StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
            	// when prefix is null, QueryProcessor default prefix is used
            	String res=processor.getResource(query, null, true,baseUri);
            	os.write(res.getBytes());
            }
        };
		return Response.ok(stream,media).build();
		
	}
	
	@GET
    @Path("/ontology")
	@Produces("text/html")
	public Response getOntologyClassView(@QueryParam("classUri") String uri) {        
		MediaType media=new MediaType("text","html","utf-8");		
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("model", new OntClassModel(uri));
        return Response.ok(new Viewable("/ontClass.jsp", map),media).build();        
    }
	
	@GET
	@Path("/{res}")	
	public Response getResourceFile(@PathParam("res") final String res, @QueryParam("classUri") final String uri) {
		if(uri!=null) {
			Map<String, Object> map = new HashMap<String, Object>();
	        map.put("model", new OntClassModel(uri));
	        return Response.ok(new Viewable("/ontClass", map)).build();
		}
		MediaType media=new MediaType("text","turtle","utf-8");
		
		StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
            	// when prefix is null, QueryProcessor default prefix is used
            	Model model=processor.getResource(res,fusekiUrl,null);	
            	RDFWriter writer=getSTTLRDFWriter(model); 
            	writer.output(os);            		
            }
        };
		return Response.ok(stream,media).build();		
	}
	
	@GET
	@Path("/{res}.{ext}")	
	public Response getTypedResourceFile(
			@PathParam("res") final String res, 
			@DefaultValue("ttl") @PathParam("ext") final String format,
			@HeaderParam("fusekiUrl") final String fuseki,
			@HeaderParam("prefix") final String prefix) {
		
		if(fuseki !=null){
			fusekiUrl=fuseki;
		}
		MediaType media=new MediaType("text","turtle");
		if(isValidExtension(format)){
			String mime=ServiceConfig.getProperty("m"+format);
			String[] parts=mime.split(Pattern.quote("/"));
			media =new MediaType(parts[0],parts[1]);
		}
		StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
            	// when prefix is null, QueryProcessor default prefix is used*/
            	Model model=processor.getResource(res,fusekiUrl,prefix); 
            	if(isValidExtension(format)&& !format.equalsIgnoreCase("ttl")){
            		if(format.equalsIgnoreCase("jsonld")) {
            			Object jsonObject=JSONLDFormatter.modelToJsonObject(model, res);
            			JSONLDFormatter.jsonObjectToOutputStream(jsonObject, os);
            		}else {
            			model.write(os,ServiceConfig.getProperty(format));
            		}
            	}else{
            	    RDFWriter writer=getSTTLRDFWriter(model);            		
            		writer.output(os);            		            		
            	}            	 
            }
        };
		return Response.ok(stream,media).build();		
	}
	
	public boolean isValidExtension(String ext){
		return (ServiceConfig.getProperty(ext)!=null);
	}
	
	public RDFWriter getSTTLRDFWriter(Model m) throws IOException{
		Lang sttl = STTLWriter.registerWriter();
		SortedMap<String, Integer> nsPrio = ComparePredicates.getDefaultNSPriorities();
		nsPrio.put(SKOS.getURI(), 1);
		nsPrio.put("http://purl.bdrc.io/ontology/admin/", 5);
		nsPrio.put("http://purl.bdrc.io/ontology/toberemoved/", 6);
		List<String> predicatesPrio = CompareComplex.getDefaultPropUris();
		predicatesPrio.add("http://purl.bdrc.io/ontology/admin/logWhen");
		predicatesPrio.add("http://purl.bdrc.io/ontology/onOrAbout");
		predicatesPrio.add("http://purl.bdrc.io/ontology/noteText");
		org.apache.jena.sparql.util.Context ctx = new org.apache.jena.sparql.util.Context();
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities"), nsPrio);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"), 2);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities"), predicatesPrio);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "indentBase"), 3);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "predicateBaseWidth"), 12);
		RDFWriter w = RDFWriter.create().source(m.getGraph()).context(ctx).lang(sttl).build();
		return w;
	}	
}
