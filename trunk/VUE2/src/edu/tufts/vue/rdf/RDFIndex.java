/**
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 */

/**
 *
 * RDFIndex.java
 *
 * @author akumar03
 * @author Daniel J. Heller
 *
 */

package edu.tufts.vue.rdf;

import java.util.*;
import java.io.*;
import java.net.*;
import edu.tufts.vue.metadata.*;
import tufts.vue.*;

import edu.tufts.vue.ontology.*;
import edu.tufts.vue.metadata.*;
import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.sparql.core.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.query.*;

public class RDFIndex extends ModelCom {
    public static final int MAX_SIZE = 60;
    public static final String INDEX_FILE = VueUtil.getDefaultUserFolder()+File.separator+VueResources.getString("rdf.index.file");
    public static final String VUE_ONTOLOGY = Constants.ONTOLOGY_URL+"#";
    com.hp.hpl.jena.rdf.model.Property idOf = createProperty(VUE_ONTOLOGY,Constants.ID);
    com.hp.hpl.jena.rdf.model.Property labelOf = createProperty(VUE_ONTOLOGY,Constants.LABEL);
    com.hp.hpl.jena.rdf.model.Property childOf = createProperty(VUE_ONTOLOGY,Constants.CHILD);
    com.hp.hpl.jena.rdf.model.Property authorOf = createProperty(VUE_ONTOLOGY,Constants.AUTHOR);
    com.hp.hpl.jena.rdf.model.Property hasTag = createProperty(VUE_ONTOLOGY,Constants.TAG);
    private static RDFIndex defaultIndex;
    
    public RDFIndex(com.hp.hpl.jena.graph.Graph base) {
        super(base);
    }
    public void index(LWMap map) {
        com.hp.hpl.jena.rdf.model.Resource mapR = this.createResource(Constants.RESOURCE_URL+map.getURI().toString());
        try {
            addProperty(mapR,idOf,map.getID());
            addProperty(mapR,authorOf,System.getProperty("user.name"));
            if(map.getLabel() != null){
                addProperty(mapR,labelOf,map.getLabel());
            }
            for(LWComponent comp: map.getAllDescendents()) {
                rdfize(comp,mapR);
            }
        } catch(Exception ex) {
            System.out.println("RDFIndex.index: "+ex);
        }
        System.out.println("Size of index:"+this.size());
    }
    
    public List<URI> search(String keyword) {
        List<URI> r = new ArrayList<URI>();
        //System.out.println("Searching for: "+keyword+ " size of index:"+this.size());
        String queryString =
                "PREFIX vue: <"+VUE_ONTOLOGY+">"+
                "SELECT ?resource " +
                "WHERE{" +
                "      ?resource ?x \""+keyword+ "\" } ";
        Query query = QueryFactory.create(queryString);
        QueryExecution qe = QueryExecutionFactory.create(query, this);
        ResultSet results = qe.execSelect();
        //System.out.println("Query: "+query+" result set:"+results);
        while(results.hasNext())  {
            QuerySolution qs = results.nextSolution();
            try {
                ///        System.out.println("Resource: "+qs.getResource("resource"));
                r.add(new URI(qs.getResource("resource").toString()));
            }catch(Throwable t) {
                t.printStackTrace();
            }
        }
        qe.close();
        return r;
    }
    
    public void save() {
        
    }
    
    public void read() {
        
        
    }
    
    public void rdfize(LWComponent component,com.hp.hpl.jena.rdf.model.Resource mapR) {
        com.hp.hpl.jena.rdf.model.Resource r = this.createResource(Constants.RESOURCE_URL+component.getURI().toString());
        try {
            addProperty(r,idOf,component.getID());
            if(component.getLabel() != null){
                addProperty(r,labelOf,component.getLabel());
            }
            com.hp.hpl.jena.rdf.model.Statement statement = this.createStatement(r,childOf,mapR);
            
            addStatement(statement);
            List<VueMetadataElement> metadata = component.getMetadataList().getMetadata();
            Iterator<VueMetadataElement> i = metadata.iterator();
            while(i.hasNext()) {
                VueMetadataElement element = i.next();
                statement = this.createStatement(r,hasTag,element.getObject().toString());
                addStatement(statement);
            }
        } catch(Exception ex) {
            System.out.println("RDFIndex.rdfize: "+ex);
        }
    }
    
    public void addStatement(com.hp.hpl.jena.rdf.model.Statement statement)  throws Exception {
        if(size() < MAX_SIZE) {
            super.add(statement);
        } else {
            throw new Exception("Size of index: "+size()+ " exceeds MAX_SIZE: "+MAX_SIZE);
        }
    }
    
    public void addProperty(com.hp.hpl.jena.rdf.model.Resource r, com.hp.hpl.jena.rdf.model.Property p,String value) throws Exception {
        if(size() <MAX_SIZE) {
            r.addProperty(p,value);
        } else {
            throw new Exception("Size of index: "+size()+ " exceeds MAX_SIZE: "+MAX_SIZE);
        }
    }
    public static String getUniqueId() {
        
        return edu.tufts.vue.util.GUID.generate();
    }
    
    public static RDFIndex getDefaultIndex() {
        if(defaultIndex == null) {
            return createDefaultIndex();
        } else {
            return defaultIndex;
        }
    }
    
    private static RDFIndex createDefaultIndex() {
        defaultIndex = new RDFIndex(com.hp.hpl.jena.graph.Factory.createGraphMem());
        try {
            File indexFile = new File(INDEX_FILE);
            if(indexFile.exists()) {
                defaultIndex.read(new FileReader(indexFile),Constants.RESOURCE_URL);
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        defaultIndex.search("one");
        return defaultIndex;
    }
    
}
