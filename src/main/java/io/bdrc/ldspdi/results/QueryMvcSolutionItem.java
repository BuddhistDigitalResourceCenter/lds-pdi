package io.bdrc.ldspdi.results;

import java.util.HashMap;
import java.util.List;

import org.apache.jena.atlas.io.StringWriterI;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.sparql.Prefixes;

public class QueryMvcSolutionItem {    
        
    public final static Logger log=LoggerFactory.getLogger(QueryMvcSolutionItem.class.getName());    
    public HashMap<String,String> dataRow;
    
    public QueryMvcSolutionItem(QuerySolution qs,List<String> headers) {
        NodeFormatterTTL nf=new NodeFormatterTTL(null,Prefixes.getPrefixMap());
        dataRow=new HashMap<>();                
        for(String key:headers) {
            RDFNode node=qs.get(key);
            if(node !=null) {
                if(node.isResource()) {
                    Resource res=node.asResource();
                    String Uri=res.getURI();
                    String tmp="";
                    if(node.asNode().isBlank()) {
                        tmp=res.getLocalName();
                    }
                    else {
                        if(Uri.startsWith("http://purl.bdrc.io/resource")) {
                            tmp="<a href=/resource/"+res.getLocalName()+"> "+res.getLocalName()+"</a>";
                        }else if(Uri.startsWith("http://purl.bdrc.io/ontology/core/")){
                            tmp="<a href=\""+Uri+"\"> "+res.getLocalName()+"</a>"; 
                        }
                    }
                    dataRow.put(key, tmp);                    
                } 
                if(node.isLiteral()) {                                      
                    StringWriterI sw=new StringWriterI();
                    nf.formatLiteral(sw, node.asNode());
                    sw.flush();
                    dataRow.put(key, sw.toString());                       
                }
                if(node.isAnon()) {
                    StringWriterI sw=new StringWriterI();
                    nf.formatBNode(sw, node.asNode());
                    dataRow.put(key, sw.toString());                    
                }                
            }else {
                dataRow.put(key, "");
            }
        }        
    }
        
    public HashMap<String, String> getDataRow() {
        return dataRow;
    }
    
    public String getValue(String key) {
        return dataRow.get(key);
    }


}