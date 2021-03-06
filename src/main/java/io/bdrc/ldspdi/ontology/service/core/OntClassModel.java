package io.bdrc.ldspdi.ontology.service.core;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 *
 * If this file is a derivation of another work the license header will appear below;
 * otherwise, this work is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This model is based on a URI for an OntClass in OntAccess.MODEL
 *
 * @author chris, marc
 *
 */
public class OntClassModel {

    final static Logger log = LoggerFactory.getLogger(OntClassModel.class);

    protected String uri;
    protected OntClass clazz;

    public OntClassModel(String uri) {
        this.uri = uri;
        clazz = OntData.ontAllMod.getOntClass(uri);
    }

    public OntClassModel(OntClass c) {

        this.uri = c.getURI();
        clazz = c;
    }

    public boolean isPresent() {
        return clazz != null;
    }

    /*
     * public boolean isRootClassModel() { return
     * OntData.getOntRootClasses().contains(this); }
     */

    public String getUri() {
        return uri;
    }

    public String getId() {
        return OntData.ontAllMod.shortForm(uri);
    }

    public boolean hasParent() {
        return clazz.getSuperClass() != null;
    }

    public String getShortName() {
        return clazz.getLocalName();
    }

    public ArrayList<OntClassModel> getParent() {
        if (clazz.getSuperClass() != null) {
            return new OntClassParent(uri).getParents();
        }
        return null;
    }

    public List<OntClassModel> getSubclasses() {
        List<OntClass> subs = clazz.listSubClasses(true).toList();
        List<OntClassModel> models = new ArrayList<>();

        for (OntClass c : subs) {
            if (!c.isAnon()) {
                models.add(new OntClassModel(c));
            }
        }
        Collections.sort(models, OntData.ontClassModelComparator);
        return models;
    }

    public List<OntClassModel> getSuperClasses() {
        List<OntClass> sups = clazz.listSuperClasses(true).toList();
        List<OntClassModel> models = new ArrayList<>();
        for (OntClass c : sups) {
            if (!c.isAnon()) {
                models.add(new OntClassModel(c));
            }
        }
        Collections.sort(models, OntData.ontClassModelComparator);
        return models;
    }

    @SuppressWarnings("unchecked")
    public List<Individual> getIndividuals() {
        ExtendedIterator<Individual> it = (ExtendedIterator<Individual>) clazz.listInstances(true);

        List<Individual> inds = it.toList();
        Collections.sort(inds, OntData.individualComparator);
        return inds;
    }

    public List<String> getLabels() {
        List<String> labels = new ArrayList<>();

        for (RDFNode node : clazz.listLabels(null).toList()) {
            labels.add(node.toString());
        }

        return labels;
    }

    public List<String[]> getLangLabels() {
        List<String[]> labels = new ArrayList<>();

        for (RDFNode node : clazz.listLabels(null).toList()) {
            labels.add(new String[] { node.asLiteral().getString(), node.asLiteral().getLanguage() });
        }
        return labels;
    }

    public List<String> getComments() {
        List<String> comments = new ArrayList<>();

        for (RDFNode node : clazz.listComments(null).toList()) {
            comments.add(node.toString());
        }

        return comments;
    }

    public List<String[]> getLangComments() {
        List<String[]> comments = new ArrayList<>();
        for (RDFNode node : clazz.listComments(null).toList()) {
            comments.add(new String[] { node.asLiteral().getString(), node.asLiteral().getLanguage() });
        }
        return comments;
    }

    /*
     * public List<OntProperty> getAllClassProperties() { ArrayList<OntProperty>
     * list = new ArrayList<>(); Triple tp = new Triple(Node.ANY,
     * ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#domain")
     * .asNode(), ResourceFactory.createResource(uri).asNode());
     * ExtendedIterator<Triple> ext = OntData.ontMod.getGraph().find(tp); while
     * (ext.hasNext()) { Triple tpp = ext.next(); String st =
     * tpp.getSubject().getURI(); OntProperty prop =
     * OntData.ontMod.getOntProperty(st); list.add(prop); } return list; }
     */

    public List<Property> getAdminAnnotProps() {
        ArrayList<Property> list = new ArrayList<>();
        StmtIterator it = clazz.listProperties();
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
        return clazz.getPropertyValue(p);
    }

}
