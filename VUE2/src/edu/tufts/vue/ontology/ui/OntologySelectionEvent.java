
/*
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
 *
 *
 */

package edu.tufts.vue.ontology.ui;

/*
 * OntologySelectionEvent.java
 *
 * Created on May 18, 2007, 1:13 PM
 *
 * @author dhelle01
 */
public class OntologySelectionEvent
{
    
    private TypeList list;
    
    public OntologySelectionEvent(TypeList list)
    {
        this.list = list;
    }
    
    public TypeList getSelection()
    {
        return list;
    }
}
