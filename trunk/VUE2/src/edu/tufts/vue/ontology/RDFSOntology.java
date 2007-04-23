/*
 * RDFSOntology.java
 *
 * Created on April 20, 2007, 12:25 PM
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
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
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.ontology;

import edu.tufts.vue.style.*;

import java.util.*;
import java.net.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.Filter;


public class RDFSOntology extends Ontology{
    OntModel m = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM,null);
    /** Creates a new instance of RDFSOntology */
    public RDFSOntology() {
    }
    
    public RDFSOntology(URL ontUrl) {
        m.read(ontUrl.toString());
        setBase(ontUrl.toString());
        ExtendedIterator iter;
        readOntTypes(m.listNamedClasses());
        readOntTypes(m.listOntProperties());
    }
    
    public RDFSOntology(URL ontUrl,URL cssUrl) {
        m.read(ontUrl.toString());
        CSSParser parser = new CSSParser();
        Map<String,Style> styleMap = parser.parseToMap(cssUrl);
        ExtendedIterator iter = m.listOntProperties();
        setBase(ontUrl.toString());
        readOntTypes(iter,styleMap);
        readOntTypes(m.listNamedClasses(),styleMap);
    }
    public org.osid.shared.Type getType() {
        return OntologyType.RDFS_TYPE;
    }
}
