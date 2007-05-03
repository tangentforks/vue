
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

import com.sun.media.jai.opimage.URLRIF;
import edu.tufts.vue.style.*;
import edu.tufts.vue.ontology.*;

import java.awt.Component;
import java.awt.datatransfer.*;
import java.awt.geom.Rectangle2D;
import java.awt.dnd.*;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
//import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;

import tufts.vue.*;
import tufts.vue.gui.*;

/*
 * TypeList.java
 *
 * modelled on tufts.vue.ui.ResourceList, this List will show
 * mapping of styles to ontologies and allow easy transfer
 * to LWMap - editing of individual  styles should also be possible
 * through the VUE gui.
 *
 * Created on March 7, 2007, 1:05 PM
 *
 * @author dhelle01
 */
public class TypeList extends JList {
    
    public static int count = 0;
    
    public static final boolean DEBUG = false;
    
    //private static Ontology fedoraOntology;
    
    //private DefaultListModel mDataModel;
    private ListModel mDataModel;
    
    private LWComponent comp;

    private LWSelection selection;
    
    private Ontology ontology;
    
    private java.util.HashMap typeCache = new java.util.HashMap();
    private java.util.HashMap typeNoLabelCache = new java.util.HashMap();
    
    public TypeList() {
        
        setCellRenderer(new TypeCellRenderer());
        
        javax.swing.DefaultListSelectionModel selectionModel = new javax.swing.DefaultListSelectionModel();
        selectionModel.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        setSelectionModel(selectionModel);
        selectionModel.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e)
            {
                comp = createLWComponent(getSelectedValue());
            }
        });
        
        // probably always zero for now, see overriden setModel below
        if(getModel().getSize()!=0)
          setSelectedIndex(0);
        
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {    
                public void mouseDragged(java.awt.event.MouseEvent me) {
                    System.out.println("TypeList: mouse dragged");
                    //System.out.println("TypeList: selected label " + ((LWComponent)(getSelectedValue())).getLabel());
                    
                    if(comp == null)
                        return;
                    
                    // the try/catch produces an infinite loops of placements on the Viewer
                    //try
                    //{        
                      GUI.startLWCDrag(TypeList.this,
                                     me,
                                     comp,
                                      VUE.getActiveViewer().getTransferableHelper(comp));
                    //}
                    //catch(java.awt.dnd.InvalidDnDOperationException dnde)
                    //{
                    //    System.out.println("TypeList: invalid drag start: " + dnde);
                    //    me.consume();
                    //}
                 
                }
         });
    
    }
    
    public void setModel(ListModel m)
    {
        super.setModel(m);
        if(getModel().getSize()!=0)
          setSelectedIndex(0);
    }
    
    public LWComponent createLWComponent(Object type)
    {
        if(typeCache.containsKey(type))
        {
            return (LWComponent)typeCache.get(type);
        }
      
        LWComponent compFor = null;
        OntType ontType = null;
        if(type instanceof OntType)
        {
          ontType = (OntType)type;
          if(ontType !=null)
          {
            Style style = ontType.getStyle();
            if(isNode(ontType))  
            {
              String image = style.getAttribute("background-image");
              if(image != null)
              {
                 System.out.println("TypeList: image not null");
                 java.net.URL imageURL = null;
                 URLResource resource = null;
                 java.io.File file = null;
                 try
                 {
                    file = new java.io.File(image);
                    imageURL = file.toURL();//new URL(image);
                    resource = new URLResource(imageURL);
                 }
                 catch(java.net.MalformedURLException mue)
                 {
                    System.out.println("TypeList: MalformedURLException: " + mue);    
                 }
                 if(resource!=null)
                 {    
                   compFor = new LWImage();
                   compFor.setResource(resource);
                 }
                 else
                 {
                     compFor = new LWNode(ontType.getLabel());
                 }
              }
              else
                compFor = new LWNode(ontType.getLabel());
            }
            else
            {
              LWLink r = new LWLink();
              r.setHeadPoint(20,20);
              r.setTailPoint(80,30);
              r.setLabel(ontType.getLabel());
              compFor = r;
            }
            compFor.applyCSS(style);
          }
          else
            compFor = new LWNode("null ont type");
        }
        else
        {
          compFor = new LWNode("error");    
        }
        
        
        //put within condition check for non-null ontType above
        //if(ontType.getStyle()!=null)
        //    compFor.applyCSS(ontType.getStyle());
        
        //needed 
        //LWComponent noLabel = compFor.duplicate();
        //noLabel.setLabel("");
        //typeNoLabelCache.put(type,noLabel);
        
        //blocking reload of style, need to account for 
        //new style before reenabling
        //typeCache.put(type,compFor);
        
        return compFor;
    }
    
    
    public class LWComponentView extends javax.swing.JComponent
    {
        //private LWComponent comp;
        private OntType type;
        
        int[] xpoints = {40,40,45};
        int[] ypoints = {15,25,20};
        int numpoints = 3;
        
        //public LWComponentView(LWComponent component)
        public LWComponentView(/*OntType t*/)
        {
            //comp = component;
            //type = t;
           // setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.GRAY));
            setBorder(javax.swing.BorderFactory.createMatteBorder(0,0,1,0,
                                                                  tufts.vue.VueResources.getColor("ui.resourceList.dividerColor", 204,204,204)));
        }
        
        public void setType(OntType t)
        {
            type = t;
        }
        
        public java.awt.Dimension getPreferredSize()
        {
            //return new java.awt.Dimension(100,50);
            return new java.awt.Dimension(100,37);
        }
        
        public void paintComponent(java.awt.Graphics g)
        {
            //tufts.vue.DrawContext dc = new tufts.vue.DrawContext(g);
           // comp.draw(dc);
            java.awt.Color old = g.getColor();
            g.setColor(getBackground());
            g.fillRect(0,0,getWidth(),getHeight());
            g.setColor(old);
            if(!isNode(type))
            {
                
              //Style style = type.getStyle();
                  
              //float strokeWidth = 1.0f;     
              //if(style!= null && style.getAttribute("stroke-width") != null)
              //  strokeWidth = edu.tufts.vue.style.ShorthandParser.parseSize(style.getAttribute("stroke-width"));
                //strokeWidth = Float.parseFloat(style.getAttribute("stroke-width"));
              //System.out.println("typelist: stroke width: " + strokeWidth);
              //if(strokeWidth <= 1.1f)
              //{
               g.drawLine(10,20,40,20);
              /*}
              else
              {
               g.fillRect(10,20,30,(int)(strokeWidth));
               for(int i=0;i<ypoints.length;i++)
               {
                  ypoints[i] += (int)((strokeWidth+1)/2 );
               }
              }*/
              
               
               g.fillPolygon(xpoints,ypoints,numpoints);
            }
            else
            {
              old = g.getColor();
              Style style = type.getStyle();
              java.awt.Color fillColor = tufts.vue.VueResources.getColor("node.fillColor");
              if(style!=null)
              {
                String fillColorString = style.getAttribute("background");
                if(fillColorString!=null)
                {
                    try
                    {        
                      fillColor =  java.awt.Color.decode(fillColorString);
                    }
                    catch(NumberFormatException nfe)
                    {
                        System.out.println("TypeList: NumberFormatException " + nfe);
                    }
                }
              }
              g.setColor(fillColor);  
              //g.fillRoundRect(10,15,30,25,5,5);
              g.fillRoundRect(10,8,30,20,5,5);
              g.setColor(old);
            }
            if(type.getLabel()!=null);
              //g.drawString(type.getLabel(),60,20);
              g.drawString(type.getLabel(),60,18 - 2 +g.getFontMetrics().getHeight()/2);
              //g.drawString(type.getLabel(),60,18-g.getFontMetrics().getAscent()/2);
              //g.drawString(type.getLabel(),60,18);
              //g.drawString(comp.getLabel(),40,10);
              
                Style s = type.getStyle();
                if(s!=null)
                {
                  String icon = s.getAttribute("background-image");
                  //System.out.println("icon " + icon);
                  if(icon != null)
                  {
                    javax.swing.ImageIcon ii = new javax.swing.ImageIcon(icon);
                    //System.out.println("image icon: " + ii);
                    //p.add(new JLabel(ii));
                    //g.drawImage(ii.getImage(),200,10,40,40,null);
                    g.drawImage(ii.getImage(),200,5,26,26,null);
                  }
                }
        }
    }
    
    class TypeCellRenderer implements ListCellRenderer
    { 
        private JLabel label = new JLabel();
        
        private LWComponentView lwcv = new LWComponentView();
        
        public java.awt.Component getListCellRendererComponent(JList jList, Object value, int i, boolean isSelected, boolean hasFocus) 
        {            
            if(value == getSelectedValue())
            {
                //lwcv.setBackground(new java.awt.Color(230,230,230));
                lwcv.setBackground(tufts.vue.gui.GUI.getTextHighlightColor());
            }
            else
                lwcv.setBackground(new java.awt.Color(255,255,255));
            
            lwcv.setType((OntType)value);
            
            return lwcv;
        }
    }
    
    // actually only needed for RDF and OWL Ontology open actions which are not currently used in the main GUI
    // needs update to used threads/etc if returned to regular operation (see alternative method above)
    public void loadOntology(URL ontologyURL,URL cssURL,org.osid.shared.Type ontType,boolean fromFile)
    {
        ontology = OntManager.getOntManager().readOntologyWithStyle(ontologyURL,
                                                      cssURL,
                                                      ontType);
        
        //OntManager.getOntManager().getOntList().add(ontology);
        //System.out.println("TypeList: ontology list size: " + OntManager.getOntManager().getOntList().size());
        
        setModel(new OntologyTypeListModel(ontology));
        
        //fillList(ontology);
        
    } 
    
    public void loadOntology(final URL ontologyURL,final URL cssURL,final org.osid.shared.Type ontType,
                             final OntologyBrowser browser,final tufts.vue.gui.Widget widget)
    {
       Thread t = new Thread(){ 
        
         public void run()
         {
           
          JLabel loadingLabel = new JLabel("loading...");   
             
          widget.add(loadingLabel,java.awt.BorderLayout.NORTH);
             
          ontology = OntManager.getOntManager().readOntologyWithStyle(ontologyURL,
                                                      cssURL,
                                                      ontType);
                    
          setModel(new OntologyTypeListModel(ontology));
          
          widget.remove(loadingLabel);
          //repaint();
          //browser.addTypeList(TypeList.this,ontologyURL.getFile());
          browser.getViewer().getList().setSelectedValue(ontology,true);
          browser.getViewer().getList().updateUI();
          //browser.getViewer().repaint();
          //browser.revalidate();
         // browser.repaint();
         }
       };
       
       //setModel(new OntologyTypeListModel(ontology));
       // fillList(ontology);
       
       t.start();
       
        
    }
    
    
    /*public void loadOntology(String ontologyLocation,String cssLocation,org.osid.shared.Type ontType,boolean fromFile)
    {
        ontology = OntManager.getOntManager().readOntologyWithStyle(VueResources.getURL(ontologyLocation),
                                                      VueResources.getURL(cssLocation),
                                                      ontType);
        
        //OntManager.getOntManager().getOntList().add(ontology);
        
        //System.out.println("TypeList: ontology list size: " + OntManager.getOntManager().getOntList().size());
        
        //fillList(ontology);
        
        setModel(new OntologyTypeListModel(ontology));
        
    }*/
    
    /**
     *
     * May be handled by LWComponent in future.
     *
     * Right now OntManager appears to read only link.localname 
     * styles, so check for node.localname and let default
     * style get applied for now.
     *
     **/
    private static boolean isNode(OntType type)
    {        
        //return type.getType().equals(edu.tufts.vue.style.SelectorType.getNodeType());
        
        //System.out.println("tl: type.getStyle.getClass() " + type.getStyle().getClass());
               
        return (type.getStyle() instanceof NodeStyle);    
    }
    
    /*
    private void applyDefaultNodeStyle(LWNode node)
    {
        
    }
    
    private void addLink(OntType ontType,Ontology ontology, Style style)
    {
        
              LWLink link = new LWLink();
              link.setAutoSized(false);
              link.setLabel(ontType.getLabel());
              link.setHeadPoint(10,25);
              link.setTailPoint(60,30);
              link.setAbsoluteSize(100,50);
              link.setStrokeWidth(4.0f);
              link.setStrokeColor(java.awt.Color.RED);
              //applyDefaultLinkStyle(link);
              link.applyCSS(ontType.getStyle());
              //addType(link);
        
    }
    
    private void applyStyleSheetLinkStyle(LWLink link,Style style)
    {
              if(style.getAttribute("stroke-width") != null)
                link.setStrokeWidth(Float.parseFloat(style.getAttribute("stroke-width")));
    }
    
    private void applyDefaultLinkStyle(LWLink link)
    {
              link.setArrowState(LWLink.ARROW_TAIL);
              link.setWeight(1);
    }*/
    
    public static class OntologyTypeListModel implements ListModel
    {
        private Ontology ontology;
             
        public OntologyTypeListModel(Ontology ontology)
        {
          this.ontology = ontology;  
        }
        
        public Object getElementAt(int index)
        {
                if(DEBUG)
                {
                  OntType element = ontology.getOntTypes().get(index);
                
                  if(!element.getLabel().endsWith("***"))
                  {
                    element.setLabel(element.getLabel()+":"+index+"***");
                  }
                  if(ontology!=null)
                    return element;
                  else
                    return null;
                }
                else
                {
                  if(ontology!=null)
                    return ontology.getOntTypes().get(index);
                  else
                    return null;
                }
         }
            
         public int getSize()
         {
                //if(ontology!=null && ontology.getOntTypes().size() < 5000)
                if(ontology!=null)
                  return ontology.getOntTypes().size();
                //else if(ontology.getOntTypes().size() >= 5000)
                //    return 5000;
                else
                  return 0;
         }
            
         public void addListDataListener(ListDataListener listener)
         {

         }
            
         public void removeListDataListener(ListDataListener listener)
         {
                
         }
        
    }
    
    
    /**
     *
     * just for testing/demo purposes
     *
     */
    /*public static void main(String[] args)
    {
        
        // add a mouselistener to bring up a color chooser (and possibly
        // the text formatter or a generic one?).
        // attempt to edit an LWComponent directly on the List
        // if this works, try it all in VUE (and add ability to drag 
        // types to LWMap to create instances)
        
        //TypeList tlist = new TypeList();
        javax.swing.JPanel panel = createTestPanel(tlist);//new javax.swing.JFrame();
        javax.swing.JFrame f = new javax.swing.JFrame();
        f.getContentPane().add(panel);
        
        //show the frame
        f.setBounds(100,100,150,300);
        f.setVisible(true);
    }*/
    
    /*public static javax.swing.JPanel createTestPanel(TypeList tlist)
    {
        javax.swing.JPanel testPanel = new javax.swing.JPanel();
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(tlist);
        testPanel.add(scroll);
        
        //add some demo data to the List
        /*LWNode rectangle = new LWNode("Test Node");
        rectangle.setShape( new java.awt.Rectangle(5,5,50,20));
        LWLink link = new LWLink();
        link.setStartPoint(10,10);
        link.setEndPoint(90,10);
        link.setAbsoluteSize(100,15);
        link.setArrowState(LWLink.ARROW_BOTH);
        link.setWeight(2);
        link.setLabel("contains");
        LWNode oval = new LWNode("Test Node 2");
        oval.setShape(new java.awt.geom.Ellipse2D.Double(5,5,50,20));

        tlist.addType(rectangle);
        tlist.addType(link);
        tlist.addType(oval);*/
        
        //add Fedora types and an extra node
        

        
        //Ontology ontology = OntManager.getFedoraOntologyWithStyles();
        
        /*Ontology ontology = null;
        
        
        try
        {  
        
        System.out.println("fedora url: " + VueResources.getURL("fedora.ontology.url"));
        System.out.println("fedora url: " + VueResources.getString("fedora.ontology.url"));
        
         ontology = OntManager.getOntManager().readOntologyWithStyle(VueResources.getURL("fedora.ontology.rdf"),
                                                      //new java.net.URL("http://www.fedora.info/definitions/1/0/fedora-relsext-ontology.rdfs"),
                                                      VueResources.getURL("fedora.ontology.css"),
                                                      OntologyType.RDFS_TYPE);
          
         /*ontology = OntManager.getOntManager().readOntologyWithStyle(//VueResources.getURL("fedora.ontology.rdf"),
                                                      new java.net.URL("http://www.atl.lmco.com/projects/ontology/ontologies/animals/animalsA.owl"),
                                                      new java.net.URL("http://vue-dev.uit.tufts.edu/ontology/css/animalsAStyle.css"),
                                                      OntManager.OWL);*/
         
         
          /*ontology = OntManager.getOntManager().readOntologyWithStyle(//VueResources.getURL("fedora.ontology.rdf"),
                                                      new java.net.URL("http://vue-dev.uit.tufts.edu/ontology/season.rdfs.xml"),
                                                      new java.net.URL("http://vue-dev.uit.tufts.edu/ontology/css/season.css"),
                                                      OntManager.OWL);*/

          
        //}
        //catch(java.net.MalformedURLException urlException)
        /*catch(Exception urlException)
        {
            System.out.println("Ontology Manager: Malformed URL:" + urlException);
            VueUtil.alert("Ontology Load Failed - improper URL","Ontology Load Failed - improper URL");
            //ontology = OntManager.getFedoraOntologyWithStyles();
        }
        
        List<OntType> types = ontology.getOntTypes();
        
        System.out.println("TypeList: types size " + types.size());
        
        Iterator<OntType> iter = types.iterator();
        while(iter.hasNext())
        {
            OntType ot = iter.next();
            Style style = ot.getStyle();
            
            if(isNode(ot))
            {
                LWNode node = new LWNode();
                node.applyCSS(style);
                tlist.addType(node);
            }
            else
            {    
              LWLink link = new LWLink();
              link.setLabel(ot.getLabel() + "-->" + count);
              link.setHeadPoint(10,25);
              link.setTailPoint(140,25);
              link.setAbsoluteSize(150,50);
              //link.setArrowState(LWLink.ARROW_HEAD);
              //link.setWeight(Integer.parseInt(style.getAttribute("weight")));
              
              System.out.println("t1: test panel about to apply style for: " + ot.getLabel());
              System.out.println("tl: test panel applying style: " + ot.getStyle());
              
              link.applyCSS(style);
              tlist.addType(link);
            }
            
            
            //System.out.println("tl: ot: " + ot + " style: " + style.getName());
            /*System.out.println("tl: isNode: " + ot.getId() + " isNode?:" + isNode(ot));
            System.out.println("tl: description: " + ot.getComment());
            LWLink link = new LWLink();
            link.setLabel(ot.getLabel());
            link.setHeadPoint(10,25);
            link.setTailPoint(140,25);
            link.setAbsoluteSize(150,50);
            //link.setArrowState(LWLink.ARROW_HEAD);
            //link.setWeight(Integer.parseInt(style.getAttribute("weight")));
            link.applyCSS(style);
            tlist.addType(link);*/
        //}
        
        /*LWNode node = new LWNode("Fedora Object");
        //node.setLabel("Fedora Object");
        node.setAbsoluteSize(150,50);
        //node.setShape( new java.awt.Rectangle(5,5,135,45));
        NodeTool.SubTool st = NodeTool.getActiveSubTool();
        node.setShape(st.getShape());
        tlist.addType(node);
        
        return testPanel;
    }*/
    
}
