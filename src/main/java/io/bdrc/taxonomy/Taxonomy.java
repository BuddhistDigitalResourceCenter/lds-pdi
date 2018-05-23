package io.bdrc.taxonomy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;

import io.bdrc.ldspdi.utils.Node;

public class Taxonomy {
    
    public static ArrayList<Node<String>> allNodes=new ArrayList<>();
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Node<String> ROOT=new Node("http://purl.bdrc.io/resource/O9TAXTBRC201605");
    public final static String SUBCLASSOF="http://purl.bdrc.io/ontology/core/taxSubclassOf";
    public final static String HASSUBCLASS="http://purl.bdrc.io/ontology/core/taxHasSubclass";
    public final static String COUNT="http://purl.bdrc.io/ontology/tmp/count";
    public final static String PREFLABEL="http://www.w3.org/2004/02/skos/core#prefLabel";
    
    static {
        Triple t=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(Taxonomy.SUBCLASSOF),NodeFactory.createURI("http://purl.bdrc.io/resource/O9TAXTBRC201605"));
        Taxonomy.buildFullTree(t, 1, Taxonomy.ROOT);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Node buildFullTree(Triple t,int x,Node root) {
        Model mod=TaxModel.getModel();
        Graph mGraph =mod.getGraph();        
        ExtendedIterator<Triple> ext=mGraph.find(t); 
        Triple tp=null;
        while(ext.hasNext()) {            
            tp=ext.next();
            Node nn=new Node(tp.getSubject().getURI());
            allNodes.add(nn);
            root.addChild(nn);
            Triple ttp=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI(tp.getSubject().getURI()));
            buildFullTree(ttp,x+1,nn);
        }
        return root;
    }
    
       
    @SuppressWarnings("unchecked")
    public static LinkedList<String> getLeafToRootPath(String leaf) {
        LinkedList<String> linkedList = new LinkedList<String>();        
        Node<String> nodeLeaf=Taxonomy.getNode(leaf); 
        if(nodeLeaf==null) {
            return linkedList;
        }
        linkedList.addLast(nodeLeaf.getData());
        while(nodeLeaf!=null) {            
            linkedList.addLast(nodeLeaf.getParent().getData());
            nodeLeaf=Taxonomy.getNode(nodeLeaf.getParent().getData());
        }        
        return linkedList;
    }
        
    public static LinkedList<String> getRootToLeafPath(String leaf) {
        LinkedList<String> tmp= getLeafToRootPath(leaf);
        Collections.reverse(tmp);
        return tmp;
    }
       
    public static Graph getPartialLDTreeTriples(Node<String> root,HashSet<String> leafTopics,HashMap<String,Integer> topics){
        Model model=TaxModel.getModel();
        Graph modGraph=model.getGraph();
        Model mod=ModelFactory.createDefaultModel();
        Graph partialTree=mod.getGraph();
        String previous=ROOT.getData();        
        for(String uri:leafTopics) {
            LinkedList<String> ll=getRootToLeafPath(uri);            
            for(String node:ll) { 
                Integer count=topics.get(node);
                if(count==null) {
                    count=-1;
                }                
                if(!node.equals(previous) && !node.equals(ROOT.getData())) {
                    partialTree.add(new Triple(NodeFactory.createURI(node),
                            NodeFactory.createURI(COUNT),
                            NodeFactory.createLiteral(count.toString())));
                    partialTree.add(new Triple(NodeFactory.createURI(previous),NodeFactory.createURI(HASSUBCLASS),NodeFactory.createURI(node) ));
                }
                ExtendedIterator<Triple> label=modGraph.find(NodeFactory.createURI(node),NodeFactory.createURI(PREFLABEL),org.apache.jena.graph.Node.ANY);
                
                while(label.hasNext()){
                    partialTree.add(label.next());                    
                }
                previous=node;
            }
        }
        return partialTree;
    }
    
       
    @SuppressWarnings("rawtypes")
    public static Node getNode(String str) {
        for(Node n:allNodes) {            
            if(n.getData().equals(str)) {
                return n;
            }
        }
        return null;
    }

}
