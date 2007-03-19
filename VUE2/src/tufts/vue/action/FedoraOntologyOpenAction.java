
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

package tufts.vue.action;

import tufts.vue.*;
import tufts.vue.gui.DockWindow;

/*
 * FedoraOntologyOpenAction.java
 *
 * Created on March 12, 2007, 12:39 PM
 *
 * @author dhelle01
 */
public class FedoraOntologyOpenAction extends VueAction {
    
    /** Creates a new instance of FedoraOntologyOpenAction */
    public FedoraOntologyOpenAction(String label) {
        super(label);
    }
    
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        edu.tufts.vue.ontology.ui.TypeList typeList = new edu.tufts.vue.ontology.ui.TypeList();
        DockWindow typeWindow = tufts.vue.gui.GUI.createDockWindow("Fedora Ontology",edu.tufts.vue.ontology.ui.TypeList.createTestPanel(typeList));
        typeWindow.setLocation(200,100);
        typeWindow.pack();
        typeWindow.setVisible(true);
    }
}
