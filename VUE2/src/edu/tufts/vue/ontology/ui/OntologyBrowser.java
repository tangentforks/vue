
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

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import tufts.vue.*;
import tufts.vue.gui.*;

/*
 * OntologyBrowser.java
 *
 * Created on April 5, 2007, 2:15 PM
 *
 * @author dhelle01
 */
public class OntologyBrowser extends JPanel {
    
    
    JPanel ontologiesPanel;

    final static DockWindow ontologyDock = tufts.vue.gui.GUI.createDockWindow("Ontologies");;
    DockWindow typeDock;
    private static boolean initialized = false;
    
    private ArrayList<OntologySelectionListener> ontologySelectionListenerList = new ArrayList<OntologySelectionListener>();
    
    private OntologyViewer ontologyViewer;
    
    private static OntologyBrowser singleton;

    final JComponent populatePane = new Widget("Populate Types") {
            private Component editor, result;
            {
                setOpaque(false);
            }
    };
    
    private WidgetStack resultsStack = new WidgetStack("types stack");
    
    private static TypeList selectedOntology = null;
    
    public DockWindow getDockWindow()
    {
        return ontologyDock;
    }
    
    public static TypeList getSelectedList()
    {
        return selectedOntology;
    }
    
    public Widget addTypeList(final edu.tufts.vue.ontology.ui.TypeList list,String name)
    {
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent)
            {
                selectedOntology = list;
                fireOntologySelectionChanged(list);
            }
        });
        
        Widget w = new Widget("Loading " + name);
        w.add(list);
        resultsStack.addPane(w);
        list.revalidate();
        w.revalidate();
        resultsStack.revalidate();
        revalidate();
        return w;
    }
    
    public static OntologyBrowser getBrowser()
    {
    	if (singleton == null)
    		singleton = new OntologyBrowser();
    	
        return singleton;
    }
    
   /* public static OntologyBrowser createBrowser(boolean delayedLoading,DockWindow ontologyDock,DockWindow typeDock)
    {
    	if (singleton == null)
    		return new OntologyBrowser(delayedLoading,ontologyDock,typeDock);
    	else
    	{
    		
    	}
    }
    */
    
    private OntologyBrowser()
    {
        ontologiesPanel = this;
        typeDock = null;
        ontologyDock.setResizeEnabled(false);
    }
    
    public void initializeBrowser(boolean delayedLoading, DockWindow typeDock) 
    {
        
        setLayout(new javax.swing.BoxLayout(this,javax.swing.BoxLayout.Y_AXIS));
        setName("Ontologies");
     
        this.typeDock = typeDock;
        this.ontologiesPanel = this;
        
        if(delayedLoading)
        {
            //TBD see DRBrowser for likely path that will be taken when loading ontologies at startup
            // e.g. fedora ontology
        }
        else
        {
            loadOntologyViewer();
        }
        
        populatePane.add(resultsStack);
        ((Widget)populatePane).setTitleHidden(true);
        
        buildSingleDockWindow();
          
        /*tufts.vue.VueAction addRDFSToBrowser = new tufts.vue.VueAction()
        {
              {
                  setActionName("Add RDFS Ontology");
              }
              
              public void actionPerformed(java.awt.event.ActionEvent e)
              {
                 // actually this shows up in the title of the Ontology Browser Dock Window..
                 //setName("Browser rdfs");
                 edu.tufts.vue.ontology.action.RDFSOntologyOpenAction rdfsooa = new edu.tufts.vue.ontology.action.RDFSOntologyOpenAction("Browser - RDFS");
                 rdfsooa.setViewer(ontologyViewer);
                 rdfsooa.actionPerformed(e);
                 rdfsooa.setViewer(null);
              }
        };
          
        tufts.vue.VueAction addOWLToBrowser = new tufts.vue.VueAction()
        {
              {
                  setActionName("Add OWL Ontology");
              }
              
              public void actionPerformed(java.awt.event.ActionEvent e)
              {
                 // shows up in Ontology Browser Dock Window title
                 //setName("Browser owl");
                 edu.tufts.vue.ontology.action.OwlOntologyOpenAction owlsooa = new edu.tufts.vue.ontology.action.OwlOntologyOpenAction("OWL - RDFS");
                 owlsooa.setViewer(ontologyViewer);
                 owlsooa.actionPerformed(e);
                 owlsooa.setViewer(null);
              }
        }; */
        
        tufts.vue.VueAction applyStyle = new tufts.vue.VueAction()
        {
            {
                setActionName("Import Style Sheet");
            }  
            
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
                chooser.showOpenDialog(OntologyBrowser.this);
                if(chooser.getSelectedFile()!=null)
                {
                    java.net.URL cssURL = null;
                    try
                    {        
                      cssURL = chooser.getSelectedFile().toURL();
                    }
                    catch(java.net.MalformedURLException mue)
                    {
                        System.out.println("trouble opening css file: " + mue);
                    }
                    int selectedOntology = getViewer().getList().getSelectedIndex();
                    ((edu.tufts.vue.ontology.Ontology)(getViewer().getList().getModel().getElementAt(selectedOntology))).applyStyle(cssURL);
                    resultsStack.updateUI();
                    getViewer().getList().updateUI();
                    // need getTypeList() method in order to refresh the typelist model...
                    // also need a list of typelist that corresponds to the ont list .. or maybe
                    // can just get the component at that location in the viewer?
                }    
            }
            
        };
          
        Action[] actions = {
            new edu.tufts.vue.ontology.action.OntologyOpenAction("Add an Ontology",this),
            applyStyle
            //new edu.tufts.vue.ontology.action.RDFSOntologyOpenAction("RDFS"),
            //new edu.tufts.vue.ontology.action.OwlOntologyOpenAction("OWL"),
            //addRDFSToBrowser,
            //addOWLToBrowser
        };
        tufts.vue.gui.Widget.setMenuActions(this,actions);
          
       // singleton = this;
        initialized = true;
        
        /*TypeList list = new TypeList();
        java.net.URL ontURL = VueResources.getURL("fedora.ontology.rdf");
        java.net.URL cssURL = VueResources.getURL("fedora.ontology.css");
        tufts.vue.gui.Widget w = addTypeList(list,"Fedora Ontology");//edu.tufts.vue.ontology.Ontology.getLabelFromUrl(ontURL.getFile()));
        //getViewer().getList().updateUI();
        list.loadOntology(ontURL,cssURL,OntologyChooser.getOntType(ontURL),this,w); 
        
        TypeList nodeList = new TypeList();
        ontURL = VueResources.getURL("fedora.support.ontology.rdf");
        cssURL = VueResources.getURL("fedora.support.ontology.css");
        tufts.vue.gui.Widget wForNode = addTypeList(nodeList,"Fedora Node Ontology");//edu.tufts.vue.ontology.Ontology.getLabelFromUrl(ontURL.getFile()));
        //getViewer().getList().updateUI();
        nodeList.loadOntology(ontURL,cssURL,OntologyChooser.getOntType(ontURL),this,wForNode); */
        
    }
    			
    public static boolean isInitialized()
    {
    	return initialized;
    }
    
    public void loadOntologyViewer()
    {
        //OntologyViewer ontologyViewer = new OntologyViewer(this);
        ontologyViewer = new OntologyViewer(this);
        ontologyViewer.setName("Ontology Viewer");
        ontologiesPanel.add(ontologyViewer);
        revalidate();
    }
    
    public OntologyViewer getViewer()
    {
        return ontologyViewer;
    }
    
    public static Object getSelectedOntology()
    {
        Object value =  getBrowser().getViewer().getList().getSelectedValue();
        return value + ":" + value.getClass();
    }
    
    public void buildSingleDockWindow()
    {
        
        WidgetStack stack = new WidgetStack(getName());

        Widget.setWantsScroller(stack, true);

        stack.addPane(ontologiesPanel, 0f);
        stack.addPane(populatePane,0f);
        
        ontologyDock.setContent(stack);
    }
    
    /*public JComponent getPopulatePane()
    {
        return populatePane;
    }*/
    
    public void addOntologySelectionListener(OntologySelectionListener osl)
    {
        ontologySelectionListenerList.add(osl);
    }
    
    public void removeOntologySelectionListener(OntologySelectionListener osl)
    {
        ontologySelectionListenerList.remove(osl);
    }
    
    private void fireOntologySelectionChanged(TypeList selection)
    {
        Iterator<OntologySelectionListener> i = ontologySelectionListenerList.iterator();
        while(i.hasNext())
        {
            OntologySelectionListener osl = i.next();
            osl.ontologySelected(new OntologySelectionEvent(selection));
        }
    }
    
}

