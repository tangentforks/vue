/*
 * AnalyzeCM.java
 *
 * Created on November 6, 2006, 11:53 AM
 *(<p><b>License and Copyright: </b>The contents of this file are subject to the
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
 * @author akumar03
 */


package tufts.vue.action;

import tufts.vue.*;
import edu.tufts.vue.compare.*;

import java.io.*;
import java.util.*;

import javax.swing.*;
import java.awt.event.*;


public class AnalyzeCM extends VueAction {
    
    private MergeMapsChooser mmc = null;
    
    /** Creates a new instance of AnalyzeCM */
    public AnalyzeCM(String label) {
        super(label);
    }
    
    public void actionPerformed(ActionEvent e) {
// Functionality moved to MergeMapsChooser Dialog and LWMergeMap 1/26/2007
/*        try {
            ArrayList<ConnectivityMatrix> list = new ArrayList();
            LWMap referenceMap = null;
            Iterator<LWMap> i =   VUE.getLeftTabbedPane().getAllMaps();
            while(i.hasNext()) {
                LWMap map = i.next();
                if(referenceMap == null)
                    referenceMap = map;
                list.add(new ConnectivityMatrix(map));
//                System.out.println("Map:"+map.getLabel());
            }
            VoteAggregate voteAggregate = new VoteAggregate(list);
//            System.out.println(voteAggregate);
            LWMap aggregate = new LWMergeMap("Vote Aggregate");
            Iterator children = referenceMap.getNodeIterator();
            while(children.hasNext()) {
                LWComponent comp = (LWComponent)children.next();
              //  System.out.print("Label: "+comp.getLabel()+" vote:"+voteAggregate.isNodeVoteAboveThreshold(comp.getLabel()));
                if(voteAggregate.isNodeVoteAboveThreshold(comp.getLabel())) {
                    LWNode node = (LWNode)comp.duplicate();
                   aggregate.addNode(node);
               }
            }
            Iterator children1 = aggregate.getNodeIterator();
            while(children1.hasNext()) {
                LWNode node1 = (LWNode)children1.next();
//                System.out.println("Processing node: "+node1.getLabel());
                Iterator children2 = aggregate.getNodeIterator();
                while(children2.hasNext()) {
                    LWNode node2 = (LWNode)children2.next();
                    if(node2 != node1) {
                        int c = voteAggregate.getConnection(node1.getLabel(),node2.getLabel());
                        if(c >0) {
                            aggregate.addLink(new LWLink(node1,node2));
//                            System.out.println("Adding Link between: "+node1.getLabel()+ " and "+ node2.getLabel());
                        }
                    }
                }
            }
            VUE.displayMap(aggregate);
            
        } catch(Exception ex) {
            ex.printStackTrace();
        }*/
        
        tufts.vue.gui.DockWindow w = MergeMapsChooser.getDockWindow();
        
        if(w==null)
        {
           mmc = new MergeMapsChooser();
           w = tufts.vue.gui.GUI.createDockWindow("Merge Maps",mmc);
           MergeMapsChooser.setDockWindow(w);  
        }
        
        if(!w.isVisible())
        {
          MergeMapsChooser.loadDefaultStyle();
          mmc.refreshSettings();
          w.setLocation(200,200);
          w.pack();
          w.setVisible(true);
        }
                
    }
    
    
}
