package io.bdrc.ldspdi.ontology.service.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.EntityTag;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.restapi.exceptions.RestException;

public class OntData implements Runnable {    

    public static InfModel infMod;
    public static OntModel ontMod;
    public static OntModel ontAuthMod;
    public static OWLPropsCharacteristics owlCharacteristics;
    public final static Logger log=LoggerFactory.getLogger(OntData.class.getName());
    public static String JSONLD_CONTEXT;
    static EntityTag update;
    static Date lastUpdated;
    static String ont;
    public static HashMap<String,OntModel> models=new HashMap<>();
    public static HashMap<String,OntModel> modelsBase=new HashMap<>();
    final static Resource RDFPL = ResourceFactory.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral");

    public static void init(String ont) throws RestException {
        try {
        	if(ont==null) {
        		ont="default";
        	}
        	String url=ServiceConfig.getConfig().getOntology(ont).fileurl;
        	System.out.println("Url="+url);
            log.info("URL >> "+ServiceConfig.getConfig().getOntology(ont).fileurl);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            InputStream stream=connection.getInputStream();
            final Model m = ModelFactory.createDefaultModel();
            System.out.println("Ext="+MediaTypeUtils.getJenaFromExtension(url.substring(url.lastIndexOf('.')+1)));
            m.read(stream, "", MediaTypeUtils.getJenaFromExtension(url.substring(url.lastIndexOf('.')+1)));
            
            //m.read(stream, "", "TURTLE");
            stream.close();
            ontMod = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, m);
            /******RDFS model*********/
            InputStream st=OntData.class.getClassLoader().getResourceAsStream("rdfs.ttl");
            final Model m1 = ModelFactory.createDefaultModel();
            m1.read(st, "", "TURTLE");
            st.close();
            ontMod.add(m1);           
            /***************/
            //owlCharacteristics=new OWLPropsCharacteristics(ontMod);
            //rdf10tordf11(ontMod);
            readGithubJsonLDContext();
            models.put(ont, ontMod);
            modelsBase.put(ServiceConfig.getConfig().getOntology(ont).getBaseuri(), ontMod);
            String authUrl=ServiceConfig.getConfig().getOntology("auth").fileurl;
            log.info("URL >> "+authUrl);
            connection = (HttpURLConnection) new URL(authUrl).openConnection();
            stream=connection.getInputStream();
            final Model auth = ModelFactory.createDefaultModel();            
            auth.read(stream, "", MediaTypeUtils.getJenaFromExtension(authUrl.substring(authUrl.lastIndexOf('.')+1)));
            stream.close();
            ontAuthMod = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, auth);
            models.put("auth", ontAuthMod);
            modelsBase.put(ServiceConfig.getConfig().getOntology("auth").getBaseuri(), ontAuthMod);
            log.info("MODELS after Init>>"+models.keySet());
            log.info("MODELS Base after Init>>"+modelsBase.keySet());
        } catch (IOException io) {
            log.error("Error initializing OntModel", io);
        }
    }
    
    public static OntModel getOntModelByBase(String baseUri) throws RestException {
    	if(modelsBase.get(baseUri)==null && ServiceConfig.getConfig().getOntologyByBase(baseUri)!=null) {
    		OntData.init(ServiceConfig.getConfig().getOntologyByBase(baseUri).getName());
    		return OntData.ontMod;
    	}
    	if(ServiceConfig.getConfig().getOntologyByBase(baseUri)!=null) {
    		ontMod=models.get(ServiceConfig.getConfig().getOntologyByBase(baseUri).getName());
    		//return modelsBase.get(ServiceConfig.getConfig().getOntologyByBase(baseUri).getName());
    		return ontMod;
    	}else {
    		return null;
    	}
    	
    }
    
    public static OntModel setOntModel(String name) throws RestException {    	
    	if(models.get(name)==null && ServiceConfig.getConfig().getOntology(name)!=null) {
    		OntData.init(name);
    		return OntData.ontMod;
    	}
    	if(ServiceConfig.getConfig().getOntology(name)!=null) {
    		return models.get(name);
    	}else {
    		return null;
    	}
    	
    }

    public static void readGithubJsonLDContext() throws MalformedURLException, IOException {
        URL url = new URL("https://raw.githubusercontent.com/BuddhistDigitalResourceCenter/owl-schema/master/context.jsonld");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder st = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            st.append(line+"\n");
        }
        in.close();
        JSONLD_CONTEXT=st.toString();
        lastUpdated=Calendar.getInstance().getTime();
        update=new EntityTag(Long.toString(System.currentTimeMillis()));
    }

    public static EntityTag getEntityTag() {
        return update;
    }

    public static Date getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void run(){
        try {
        	ArrayList<String> names=ServiceConfig.getConfig().getValidNames();
        	for(String name:names) {         		
	            final String fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);	           
	            ontMod = OntData.setOntModel(name);
	            owlCharacteristics=new OWLPropsCharacteristics(ontMod);
	            log.info("OntModel Size >> "+ontMod.size());
	            infMod = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), ontMod.getBaseModel());
	            log.info("updating core ont model() >> "+ name);
	            QueryProcessor.updateOntology(infMod, fusekiUrl.substring(0,fusekiUrl.lastIndexOf('/'))+"/data", ServiceConfig.getConfig().getOntology(name).getGraph());
	            log.info("updated >>"+ServiceConfig.getConfig().getOntology(name).getGraph()); 
	            System.out.println("updated >>"+ServiceConfig.getConfig().getOntology(name).getGraph()); 
        	}
            readGithubJsonLDContext();
            //purge models map to force reloading of updated models
            models=new HashMap<>();
            modelsBase=new HashMap<>();
        }
        catch(Exception ex) {
            log.error("Error updating OntModel", ex);
        }
    }

    public static OWLPropsCharacteristics getOwlCharacteristics() {
        return owlCharacteristics;
    }

    public static Model describeUri(final String uri) {
        final String query = "describe  <"+uri+">";
        final QueryExecution qexec = QueryExecutionFactory.create(query, ontMod);
        return qexec.execDescribe();
    }

    public static ArrayList<OntProperty> getAllClassProps(String iri){
        OntClassModel clModel=new OntClassModel(iri);
        final ArrayList<OntProperty> list=new ArrayList<>();
        if(clModel.clazz!=null) {
            String qy="";
            try {
                qy=Prefixes.getPrefixesString() +" select distinct  ?clazz ?p\n" +
                        "where {\n" +
                        "bind (<"+iri+"> as ?base)\n" +
                        "graph <"+ServiceConfig.getConfig().getOntology(ont).getGraph() +">{\n" +
                        "?base rdfs:subClassOf+ ?clazz .\n" +
                        "{ ?p rdfs:domain/(owl:unionOf/rdf:rest*/rdf:first)* ?clazz . }\n" +
                        "}\n" +
                        "}";
            } catch (RestException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            final QueryExecution qexec=QueryProcessor.getResultSet(qy,null);
            final ResultSet res = qexec.execSelect() ;
            while(res.hasNext()) {
                final QuerySolution qs=res.next();
                final RDFNode node=qs.get("?p");
                OntResource rsc=ontMod.getOntResource(node.asResource().getURI());
                //System.out.println("NODE >> "+node.asResource()+ " OntProp >>"+ontMod.getOntResource(node.asResource().getURI()).isProperty());
                if(rsc.isProperty()) {
                    list.add(rsc.asProperty());
                }
                else {
                    System.out.println("Skipped "+node.asResource().getURI()+" property in getAllClassProps("+iri+")");
                }
            }
        }
        return list;
    }

    public static ArrayList<OntResource> getDomainUsages(final String uri) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" +
                " select distinct ?s where {" +
                "    ?s rdfs:domain <"+uri+"> ." +
                "} order by ?p ?s";
        final QueryExecution qexec = QueryExecutionFactory.create(query, ontMod);
        final ResultSet res = qexec.execSelect() ;
        final ArrayList<OntResource> list=new ArrayList<>();
        while(res.hasNext()) {
            final QuerySolution qs=res.next();
            final RDFNode node=qs.get("?s");
            list.add(ontMod.getOntResource(node.asResource().getURI()));
        }
        return list;
    }

    public static ArrayList<OntResource> getRangeUsages(final String uri) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" +
                " select distinct ?s ?p where {" +
                "    ?s rdfs:range <"+uri+"> ." +
                "} order by ?p ?s";
        final QueryExecution qexec = QueryExecutionFactory.create(query, ontMod);
        final ResultSet res = qexec.execSelect() ;
        final ArrayList<OntResource> list=new ArrayList<>();
        while(res.hasNext()) {
            final QuerySolution qs=res.next();
            final RDFNode node=qs.get("?s");
            list.add(ontMod.getOntResource(node.asResource().getURI()));
        }
        return list;
    }

    public static ArrayList<OntResource> getSubProps(final String uri) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" +
                " select distinct ?s ?p where {" +
                "    ?s rdfs:subPropertyOf <"+uri+"> ." +
                "} order by ?p ?s";
        final QueryExecution qexec = QueryExecutionFactory.create(query, ontMod);
        final ResultSet res = qexec.execSelect() ;
        final ArrayList<OntResource> list=new ArrayList<>();
        while(res.hasNext()) {
            final QuerySolution qs=res.next();
            final RDFNode node=qs.get("?s");
            list.add(ontMod.getOntResource(node.asResource().getURI()));
        }
        return list;
    }

    public static ArrayList<OntResource> getParentProps(final String uri) throws RestException {
        final String query="prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" +
                " select distinct ?s where {" +
                "   <"+uri+"> rdfs:subPropertyOf ?s ." +
                "} order by ?s";
        final QueryExecution qexec = QueryExecutionFactory.create(query, ontMod);
        final ResultSet res = qexec.execSelect() ;
        final ArrayList<OntResource> list=new ArrayList<>();
        while(res.hasNext()) {
            final QuerySolution qs=res.next();
            final RDFNode node=qs.get("?s");
            list.add(ontMod.getOntResource(node.asResource().getURI()));
        }
        return list;
    }

    public static ArrayList<OntResource> getSubClassesOf(final String uri) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" +
                " select distinct ?s where {" +
                "   ?s rdfs:subClassOf <"+uri+"> ." +
                "} order by ?s";
        final QueryExecution qexec = QueryExecutionFactory.create(query, ontMod);
        final ResultSet res = qexec.execSelect() ;
        final ArrayList<OntResource> list=new ArrayList<>();
        while(res.hasNext()) {
            final QuerySolution qs=res.next();
            final RDFNode node=qs.get("?s");
            list.add(ontMod.getOntResource(node.asResource().getURI()));
        }
        return list;
    }

    public static HashMap<String,ArrayList<OntResource>> getAllSubProps(final String uri) throws RestException {
        final ArrayList<OntResource> props=OntData.getDomainUsages(uri);
        final HashMap<String,ArrayList<OntResource>> map=new HashMap<>();
        for(final OntResource rs:props) {
            final ArrayList<OntResource> l=OntData.getSubProps(rs.getURI());
            if(l.size()>1) {
                map.put(rs.getURI(), l);
            }
        }
        return map;
    }

    public static boolean isClass(final String uri) {
    	if(ontMod.getOntResource(uri) !=null) {
    		return ontMod.getOntResource(uri).isClass();
    	}else {
    		return false;
    	}
    }

    public static int getNumPrefixes() {
        return ontMod.numPrefixes();
    }

    public static Map<String,String> getPrefixMap(){
        return ontMod.getNsPrefixMap();
    }

    public static int getNumRootClasses() {
        return getSimpleRootClasses().size();
    }

    /**
     * Returns a list of simple root OntClass(es). Simple means not defined as a Union or Restriction
     * and so on. The purpose is to provide the roots of a traversal of classes defined in the ontology.
     *
     * @return list of simple root OntClass(es)
     */
    public static List<OntClass> getSimpleRootClasses() {
        final List<OntClass> classes = ontMod.listHierarchyRootClasses().toList();
        final List<OntClass> rez = new ArrayList<>();
        for (final OntClass oc : classes) {
            if (oc.getURI() != null) {
                rez.add(oc);
            }
        }
        Collections.sort(rez, OntData.ontClassComparator);
        return rez;
    }

    public static List<OntClassModel> getOntRootClasses() {
        final List<OntClass> roots = getSimpleRootClasses();
        final List<OntClassModel> models = new ArrayList<>();
        for (final OntClass root : roots) {
            if(!root.isAnon()) {
                models.add(new OntClassModel(root));
            }
        }
        Collections.sort(models,OntData.ontClassModelComparator);
        return models;
    }

    public static ArrayList<OntClass> getAllClasses(){
        final ExtendedIterator<OntClass> it=ontMod.listClasses();
        final ArrayList<OntClass> classes=new ArrayList<>();
        while(it.hasNext()) {
            final OntClass ocl=it.next();
            if(ocl !=null && !ocl.isAnon()) {
                classes.add(ocl);
            }
        }
        Collections.sort(classes, OntData.ontClassComparator);
        return classes;
    }

    public static ArrayList<OntProperty> getAllProps(){
        final ExtendedIterator<OntProperty> it=ontMod.listAllOntProperties();
        final ArrayList<OntProperty> list=new ArrayList<>();
        while(it.hasNext()) {
            final OntProperty pr=it.next();
            if(pr!=null && pr.isProperty()) {
                list.add(pr);
            }
        }
        Collections.sort(list, OntData.propComparator);
        return list;
    }

    public static List<Individual> getAllIndividuals(){
        final List<Individual> indv =ontMod.listIndividuals().toList();
        Collections.sort(indv, individualComparator);
        return indv;
    }

    public final static Comparator<OntClass> ontClassComparator = new Comparator<OntClass>() {
        @Override
        public int compare(OntClass class1, OntClass class2) {
            if(Prefixes.getPrefix(class1.getNameSpace()).equals(Prefixes.getPrefix(class2.getNameSpace()))) {
                return class1.getLocalName().compareTo(class2.getLocalName());
            }
            return Prefixes.getPrefix(class1.getNameSpace()).compareTo(Prefixes.getPrefix(class2.getNameSpace()));
        }

    };

    public final static Comparator<OntClassModel> ontClassModelComparator = new Comparator<OntClassModel>() {
        @Override
        public int compare(OntClassModel class1, OntClassModel class2) {
            return ontClassComparator.compare(class1.clazz,class2.clazz);
        }

    };

    public final static Comparator<Individual> individualComparator = new Comparator<Individual>() {
        @Override
        public int compare(Individual class1, Individual class2) {
            return class1.getLocalName().compareTo(class2.getLocalName());
        }

    };

    public final static Comparator<OntProperty> propComparator = new Comparator<OntProperty>() {
        @Override
        public int compare(OntProperty prop1, OntProperty prop2) {
            if(Prefixes.getPrefix(prop1.getNameSpace()).equals(Prefixes.getPrefix(prop2.getNameSpace()))) {
                return prop1.getLocalName().compareTo(prop2.getLocalName());
            }
            return Prefixes.getPrefix(prop1.getNameSpace()).compareTo(Prefixes.getPrefix(prop2.getNameSpace()));
        }

    };

    public static void rdf10tordf11(OntModel o) {
        final ExtendedIterator<DatatypeProperty> it = o.listDatatypeProperties();
        while(it.hasNext()) {
            final DatatypeProperty p = it.next();
            if (p.hasRange(RDFPL)) {
                p.removeRange(RDFPL);
                p.addRange(RDF.langString);
            }
        }
        final ExtendedIterator<Restriction> it2 = o.listRestrictions();
        while(it2.hasNext()) {
            final Restriction r = it2.next();
            final Statement s = r.getProperty(OWL2.onDataRange); // is that code obvious? no
            if (s != null && s.getObject().asResource().equals(RDFPL)) {
                s.changeObject(RDF.langString);

            }
        }
    }
}
