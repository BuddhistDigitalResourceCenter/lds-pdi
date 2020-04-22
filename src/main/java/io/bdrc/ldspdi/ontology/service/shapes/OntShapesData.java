package io.bdrc.ldspdi.ontology.service.shapes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.core.OntPolicy;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;

public class OntShapesData implements Runnable {

    public final static Logger log = LoggerFactory.getLogger(OntShapesData.class);
    public static HashMap<String, Model> modelsBase = new HashMap<>();
    public static OntModel ontAllMod;

    public static void init() {
        try {
            OntPolicies.init();
            modelsBase = new HashMap<>();
            Model md = ModelFactory.createDefaultModel();
            OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
            OntDocumentManager odm = new OntDocumentManager(ServiceConfig.getProperty("ontShapesPoliciesUrl"));
            ontAllMod = ModelFactory.createOntologyModel(oms, md);
            Iterator<String> it = odm.listDocuments();
            System.out.println("SHAPES POLICIES >> " + OntPolicies.mapShapes);
            while (it.hasNext()) {
                String uri = it.next();
                log.info("OntManagerDoc : {}", uri);
                // OntModel om = odm.getOntology(uri, oms);
                String ss = uri.replace("purl.bdrc.io", "localhost:8080");
                OntPolicy op = OntPolicies.getShapeOntologyByBase(ss);
                log.info("POLICY from uri {} is {}", ss, op);
                Model om = ModelFactory.createDefaultModel();
                om.read(op.getFile(), "TTL");
                OntShapesData.addOntModelByBase(parseBaseUri(uri), om);
            }
            log.info("Done with OntShapesData initialization ! Uri set is {}", modelsBase.keySet());
        } catch (Exception ex) {
            log.error("Error updating OntShapesData Model", ex);
        }
    }

    private static String parseBaseUri(String s) {
        if (s.endsWith("/") || s.endsWith("#")) {
            s = s.substring(0, s.length() - 1);
        }
        s = s.replace("purl.bdrc.io", ServiceConfig.SERVER_ROOT);

        return s;
    }

    public static void addOntModelByBase(String baseUri, Model om) {
        modelsBase.put(baseUri, om);
    }

    public static Model getOntModelByBase(String baseUri) {
        return modelsBase.get(baseUri);
    }

    private static void updateFusekiDataset() throws RestException {
        String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        OntPolicies.init();
        HashMap<String, OntPolicy> policies = OntPolicies.getMapShapes();
        HashMap<String, Model> updates = new HashMap<>();
        for (String s : policies.keySet()) {
            OntPolicy op = policies.get(s);
            String graph = op.getGraph();
            Model m = (Model) updates.get(graph);
            if (m != null) {

                m.add((Model) getOntModelByBase(op.getBaseUri()));
            } else {
                m = (Model) getOntModelByBase(op.getBaseUri());
            }
            updates.put(graph, m);
        }
        for (String st : updates.keySet()) {
            // Display input Model
            if (st.contains("PersonShapes")) {
                System.out.println("************************************" + st);
                updates.get(st).write(System.out, "TURTLE");
            }
            QueryProcessor.updateOntology(updates.get(st), fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data", st, st + " update");
            // Display corresponding created named graph
            System.out.println("##########################################" + st);
            QueryProcessor.getAnyGraph(st, fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data").write(System.out, "TURTLE");

        }
    }

    public static void basicTest() {
        String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        String url = "https://raw.githubusercontent.com/buda-base/editor-templates/master/templates/core/person.shapes.ttl";
        Model m = ModelFactory.createDefaultModel();
        m.read(url, "TTL");
        System.out.println("MODEL SIZE=" + m.size());
        QueryProcessor.updateOntology(m, fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data", "http://purl.bdrc.io/graph/PersonShapesTest",
                " update");
    }

    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, RestException {
        ServiceConfig.init();
        OntShapesData.init();
        // OntData.init();
        for (String key : OntShapesData.modelsBase.keySet()) {
            System.out.println(key + " HAS GRAPH : >>" + (OntPolicies.getOntologyByBase(key).getGraph()));
        }
        for (String key : OntData.modelsBase.keySet()) {
            System.out.println(key + " HAS GRAPH : >>" + (OntPolicies.getOntologyByBase(key).getGraph()));
        }
        updateFusekiDataset();
        // basicTest();
    }

    @Override
    public void run() {
        init();
        try {
            updateFusekiDataset();
        } catch (RestException e) {
            // TODO Auto-generated catch block
            log.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

}
