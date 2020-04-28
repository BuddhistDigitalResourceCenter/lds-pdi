package io.bdrc.ldspdi.ontology.service.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ontology.InverseFunctionalProperty;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.SymmetricProperty;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.service.ServiceConfig;

public class OntPropModel {

    final static Logger log = LoggerFactory.getLogger(OntPropModel.class);

    public static final String DOMAIN = "http://www.w3.org/2000/01/rdf-schema#domain";
    public static final String RANGE = "http://www.w3.org/2000/01/rdf-schema#range";
    public static final String LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
    public static final String COMMENT = "http://www.w3.org/2000/01/rdf-schema#comment";
    public static final String TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    public String uri;
    public String name;
    public String rdfType;
    public String rdfTypeUri;
    public String label;
    public String labelLang;
    public ArrayList<String> range;
    public String rangeUri;
    public ArrayList<String> domain;
    public String domainUri;
    public List<String> comments;
    public List<String[]> commentsLang;
    public OntProperty prop;
    OntModel ontMod;

    public OntPropModel(String uri, boolean global) {
        this.uri = uri;
        if (global) {
            this.name = OntData.ontAllMod.shortForm(uri);
            prop = OntData.ontAllMod.getOntProperty(uri);
            this.ontMod = OntData.ontAllMod;
        } else {
            this.name = OntData.ontMod.shortForm(uri);
            prop = OntData.ontMod.getOntProperty(uri);
            this.ontMod = OntData.ontMod;
        }
        comments = new ArrayList<>();
        commentsLang = new ArrayList<>();
        if (prop != null) {
            for (RDFNode node : prop.listComments(null).toList()) {
                comments.add(node.toString());
                commentsLang.add(new String[] { node.asLiteral().getString(), node.asLiteral().getLanguage() });
            }
        }
        StmtIterator it = null;
        if (global) {
            it = ((Model) ontMod).listStatements(ResourceFactory.createResource(uri), (Property) null, (RDFNode) null);

        } else {
            it = ((Model) ontMod).listStatements(ResourceFactory.createResource(uri), (Property) null, (RDFNode) null);

        }
        while (it.hasNext()) {
            Statement st = it.next();
            String pred = st.getPredicate().getURI();
            switch (pred) {
            case DOMAIN:
                domain = getRdfListElements(st);
                break;
            case RANGE:
                range = getRdfListElements(st);
                break;
            case LABEL:
                this.label = st.getObject().asLiteral().getString();
                this.labelLang = st.getObject().asLiteral().getLanguage();
                break;
            /*
             * case COMMENT: this.comment=st.getObject().asLiteral().getString()+comment;
             * this.commentLang=st.getObject().asLiteral().getLanguage(); break;
             */
            case TYPE:
                this.rdfTypeUri = st.getObject().asNode().getURI();
                this.rdfType = ontMod.shortForm(rdfTypeUri);
                break;
            }
        }
    }

    public OntPropModel(String uri, OntModel ontMod) {
        this.uri = uri;
        this.name = ontMod.shortForm(uri);
        prop = ontMod.getOntProperty(uri);
        this.ontMod = ontMod;
        comments = new ArrayList<>();
        commentsLang = new ArrayList<>();
        if (prop != null) {
            for (RDFNode node : prop.listComments(null).toList()) {
                comments.add(node.toString());
                commentsLang.add(new String[] { node.asLiteral().getString(), node.asLiteral().getLanguage() });
            }
        }
        StmtIterator it = null;
        it = ((Model) OntData.ontMod).listStatements(ResourceFactory.createResource(uri), (Property) null, (RDFNode) null);
        while (it.hasNext()) {
            Statement st = it.next();
            String pred = st.getPredicate().getURI();
            switch (pred) {
            case DOMAIN:
                domain = getRdfListElements(st);
                break;
            case RANGE:
                range = getRdfListElements(st);
                break;
            case LABEL:
                this.label = st.getObject().asLiteral().getString();
                this.labelLang = st.getObject().asLiteral().getLanguage();
                break;
            case TYPE:
                this.rdfTypeUri = st.getObject().asNode().getURI();
                this.rdfType = OntData.ontAllMod.shortForm(rdfTypeUri);
                break;
            }
        }
    }

    public ArrayList<String> getRdfListElements(Statement st) {
        ArrayList<String> elts = new ArrayList<>();
        if (st.getObject().isURIResource()) {
            this.domainUri = st.getObject().asNode().getURI();
            elts.add(domainUri);
            return elts;
        }
        Resource rs = null;
        if (st.getObject().isAnon()) {
            rs = ontMod.createResource(new AnonId(st.getObject().asNode().getBlankNodeId()));
            StmtIterator stmt = rs.listProperties(ResourceFactory.createProperty("http://www.w3.org/2002/07/owl#unionOf"));
            while (stmt.hasNext()) {
                List<RDFNode> list = stmt.next().getObject().asResource().as(RDFList.class).asJavaList();
                for (RDFNode node : list) {
                    elts.add(node.asResource().getURI());
                }
            }
            return elts;
        }
        elts.add("Inherited");
        return elts;
    }

    public ArrayList<OntPropModel> getAllSubProps(boolean global) throws RestException {
        ArrayList<OntResource> res = OntData.getSubProps(uri, global);
        ArrayList<OntPropModel> list = new ArrayList<>();
        for (OntResource r : res) {
            list.add(new OntPropModel(r.getURI(), global));
        }
        return list;
    }

    public ArrayList<OntPropModel> getParentProps(boolean global) throws RestException {
        ArrayList<OntResource> res = OntData.getParentProps(uri, global);
        ArrayList<OntPropModel> list = new ArrayList<>();
        for (OntResource r : res) {
            if (r != null && r.isURIResource()) {
                list.add(new OntPropModel(r.getURI(), global));
            }
        }
        return list;
    }

    public String getSymmetricProp() {
        SymmetricProperty prop = OntData.ontMod.getSymmetricProperty(getUri());
        if (prop != null) {
            return prop.asProperty().getRDFType().getURI();
        }
        return null;
    }

    public String getInverseProp() {
        InverseFunctionalProperty prop = OntData.ontMod.getInverseFunctionalProperty(getUri());
        if (prop != null) {
            return prop.asProperty().getRDFType().getURI();
        }
        return null;
    }

    public String getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getRdfType() {
        return rdfType;
    }

    public String getLabel() {
        return label;
    }

    public ArrayList<String> getRange() {
        if (range == null) {
            range = new ArrayList<>();
            range.add("Inherited");
            return range;
        }
        return range;
    }

    public ArrayList<String> getDomain() {
        if (domain == null) {
            domain = new ArrayList<>();
            domain.add("Inherited");
            return domain;
        }
        return domain;
    }

    public boolean isDomainInherited() {
        return getDomain().contains("Inherited");
    }

    public boolean isRangeInherited() {
        return getRange().contains("Inherited");
    }

    public List<String> getComments() {
        return comments;
    }

    public String getLabelLang() {
        return labelLang;
    }

    public List<String[]> getCommentsLang() {
        return commentsLang;
    }

    public String getRdfTypeUri() {
        return rdfTypeUri;
    }

    public String getRangeUri() {
        return rangeUri;
    }

    public String getDomainUri() {
        return domainUri;
    }

    @Override
    public String toString() {
        return "OntPropModel [uri=" + uri + ", name=" + name + ", rdfType=" + rdfType + ", label=" + label + ", range=" + range + ", domain=" + domain
                + "]";
    }

    public List<Property> getAdminAnnotProps() {
        ArrayList<Property> list = new ArrayList<>();
        StmtIterator it = prop.listProperties();
        while (it.hasNext()) {
            Statement stmt = it.next();
            Property p = stmt.getPredicate();
            if (OntData.isAdminAnnotProp(p)) {
                list.add(p);
            }
        }
        return list;
    }

    public RDFNode getPropertyValue(Property p) {
        return prop.getPropertyValue(p);
    }

    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
        ServiceConfig.initForTests("http://buda1.bdrc.io:13180/fuseki/bdrcrw/query");
        OntData.init();
        OntPropModel mod = new OntPropModel("http://purl.bdrc.io/ontology/core/isRoot", true);
        System.out.println("ADMIN ANNOT PROPS >> " + OntData.adminAnnotProps);
        System.out.println(mod.getAdminAnnotProps());
    }

}