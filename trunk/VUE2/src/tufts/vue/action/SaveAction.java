/*
 * -----------------------------------------------------------------------------
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/*
 * SaveAction.java
 *
 * Created on March 31, 2003, 1:33 PM
 */

package tufts.vue.action;

import javax.swing.*;

import java.awt.event.*;
import java.io.*;
import java.util.Iterator;
import java.util.Vector;

import tufts.vue.*;
import tufts.vue.gui.VueFrame;

/**
 * Save the currently active map.
 *
 * @author akumar03
 * @author Scott Fraize
 */
public class SaveAction extends VueAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SaveAction.class);

    private boolean saveAs = true;
    private boolean export = true;
    
    public SaveAction(String label, boolean saveType, boolean export){
        super(label, null, ":general/Save");
        setSaveAs(saveType);
        this.export = export;
    }
    
    public SaveAction(String label, boolean saveType)
    {
    	this(label,saveType,false);
    }
    public SaveAction(String label) {
        this(label, true,false);
    }
    
    public SaveAction() {
        this("Save", false,false);
    }
    
    public boolean isSaveAs() {
        return this.saveAs;
    }
    
    public void setSaveAs(boolean saveAs){
        this.saveAs = saveAs;
    }      
    
    /*    
    public void setFileName(String fileName) {
        file = new File(fileName);
    }
    
    public String getFileName() {
        return file.getAbsolutePath();
    }
    */
    
    private boolean inSave = false;
    public void actionPerformed(ActionEvent e)
    {
        if (inSave) // otherwise rapid Ctrl-S's will trigger multiple dialog boxes
            return;

        try {
            inSave = true;
            Log.info("Action["+e.getActionCommand()+"] invoked...");
            if (saveMap(tufts.vue.VUE.getActiveMap(), isSaveAs(),export))
                Log.info("Action["+e.getActionCommand()+"] completed.");
            else
                Log.info("Action["+e.getActionCommand()+"] aborted.");
        } finally {
            inSave = false;
        }
    }

    /**
     * @return true if success, false if not
     */
      
    public static boolean saveMap(LWMap map, boolean saveAs, boolean export)
    {
        System.out.println("SaveAction.saveMap: " + map);
        
        if (map == null)
            return false;
        
        File file = map.getFile();
        int response = -1;
        if (map.getSaveFileModelVersion() == 0) {

        	final Object[] defaultOrderButtons = { "Save a copy","Save"};
            response = JOptionPane.showOptionDialog
            (VUE.getDialogParent(),
        
       		"Saving "+ map.getLabel()+ " Sin this version of VUE will prevent older versions of VUE" +       				
              "\nfrom displaying it properly.  You may wish to save this map under a new name.",         
             "Version Notice: " + map.getLabel(),
             JOptionPane.YES_NO_OPTION,
             JOptionPane.PLAIN_MESSAGE,
             null,
             defaultOrderButtons,             
             "Save a copy"
             );
        }
        
        if (response ==0)
        	saveAs=true;
        if ((saveAs || file == null) && !export)
            //file = ActionUtil.selectFile("Save Map", "vue");
            file = ActionUtil.selectFile("Save Map", null);
        else if (export)
        	file = ActionUtil.selectFile("Export Map", "export");
        if (file == null)
            return false;
        
        try {
            VUE.activateWaitCursor();
            String name = file.getName().toLowerCase();

            System.out.println("Save name[" + name + "]");
            
            if (name.endsWith(".rli.xml")) {
                new IMSResourceList().convert(map,file);
            }
            else if (name.endsWith(".xml") || name.endsWith(".vue")) {
                ActionUtil.marshallMap(file, map);
            }
            else if (name.endsWith(".jpeg") || name.endsWith(".jpg"))
                ImageConversion.createActiveMapJpeg(file);
            else if (name.endsWith(".png"))
                ImageConversion.createActiveMapPng(file);
            else if (name.endsWith(".svg"))
                new SVGConversion().createSVG(file);
            
            else if (name.endsWith(".pdf"))
            {
            	PresentationNotes.createMapAsPDF(file);
                //new PDFTransform().convert(file);
            }
            else if (name.endsWith(".zip"))
            {   Vector resourceVector = new Vector();
            	Iterator i = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
            	while(i.hasNext()) {	
            		LWComponent component = (LWComponent) i.next();
            		System.out.println("Component:"+component+" has resource:"+component.hasResource());
            		if(component.hasResource() && (component.getResource() instanceof URLResource)){
                    
            			URLResource resource = (URLResource) component.getResource();                    
                
            			//   	if(resource.getType() == Resource.URL) {
            			try {
                        // File file = new File(new URL(resource.getSpec()).getFile());
                        if(resource.isLocalFile()) {
                        	String spec = resource.getSpec();                        	                        
                        	System.out.println(resource.getSpec());
                            Vector row = new Vector();
                            row.add(new Boolean(true));
                            row.add(resource);
                            row.add(new Long(file.length()));
                            row.add("Ready");
                            resourceVector.add(row);
                        }
            			}catch (Exception ex) {
            				System.out.println("Publisher.setLocalResourceVector: Resource "+resource.getSpec()+ ex);
            				ex.printStackTrace();
            			}                    
            		}                
            	}
            	File savedCMap =PublishUtil.createZip(map, resourceVector);
            	 InputStream istream = new BufferedInputStream(new FileInputStream(savedCMap));
                OutputStream ostream = new BufferedOutputStream(new FileOutputStream(file));
                int fileLength = (int)savedCMap.length();
                byte bytes[] = new  byte[fileLength];
                try
                {
                	while (istream.read(bytes,0,fileLength) != -1)
                		ostream.write(bytes,0,fileLength);
                }
                catch(Exception e)
                {
                	e.printStackTrace();
                }
                finally
                {
                	istream.close();
                	ostream.close();
                }
            }
            //else if (name.endsWith(".html"))
              //  new HTMLConversion().convert(file);
            
            //else if (name.endsWith(".imap"))
            else if (name.endsWith(".html")) {
                new ImageMap().createImageMap(file);
            }
//             else if (name.endsWith(".htm")) {
//                 writeHTMLOutline(map, file);
//             }
            else if(name.endsWith(".rdf"))
            {
               edu.tufts.vue.rdf.RDFIndex index = new edu.tufts.vue.rdf.RDFIndex();
               
               String selectionType = VueResources.getString("rdf.export.selection");
               
               if(selectionType.equals("ALL"))
               {
                 Iterator<LWMap> maps = VUE.getLeftTabbedPane().getAllMaps();
                 while(maps.hasNext())
                 {
                     index.index(maps.next());
                 }
               }
               else if(selectionType.equals("ACTIVE"))
               {
                 index.index(VUE.getActiveMap());  
               }    
               else
               {    
                 index.index(VUE.getActiveMap());
               }  
               FileWriter writer = new FileWriter(file);
               index.write(writer);
               writer.close();
            }
            

			// don't know this as not all the above stuff is passing
            // exceptions on to us!
            Log.debug("Save completed for " + file);
            VueFrame frame = (VueFrame)VUE.getMainWindow();
            String title = VUE.getName() + ": " + name;                      
            frame.setTitle(title);
            
            if (name.endsWith(".vue"))
            {
             RecentlyOpenedFilesManager rofm = RecentlyOpenedFilesManager.getInstance();
             rofm.updateRecentlyOpenedFiles(file.getAbsolutePath());
            }
            return true;

        } catch (Throwable t) {
            Log.error("Exception attempting to save file " + file + ": " + t);
            Throwable e = t;
            if (t.getCause() != null)
                e = t.getCause();
            if (e instanceof java.io.FileNotFoundException) {
                Log.error("Save Failed: " + e);
            } else {
                Log.error("Save failed for \"" + file + "\"; " + e);
                tufts.Util.printStackTrace(e);
            }
            if (e != t)
                Log.error("Exception attempting to save file " + file + ": " + e);
        } finally {
            VUE.clearWaitCursor();
        }

        return false;
    }

    private static void writeHTMLOutline(LWMap map, File file)
        throws IOException
    {
        PrintWriter out;
        if (file == null) {
            System.err.println("Writing to stdout");
            out = new PrintWriter(System.out, true);
        } else {
            System.out.println("Writing: " + file);
            out = new PrintWriter(file);
        }
            
        writeHTMLOutline(map, out);
        out.flush();
        out.close();
    }
    
    private static void writeHTMLOutline(LWMap map, PrintWriter out)
    {
        String title = map.getLabel();
        if (title.endsWith(".vue") || title.endsWith(".htm"))
            title = title.substring(0, title.length()-4);

        out.println("<html>");
        out.println("<head>");
        out.println("<title>" + title + "</title>");
        out.println("<!-- generated by " + tufts.vue.Version.WhatString + " -->");
        out.println("<!-- generated on " + new java.util.Date() + " from " + map.getFile() + " -->");
        out.println("</head>");
        out.println("<body>");
        //out.println("<h1>" + title + "</h1>");

        writeUnorderedNode(map, out);
        
        out.println("</body>");
        out.println("</html>");
    }

    // TODO: make successfully recursive to depth, with deeper ordered
    // lists, tracking level so we can use <h2> or whatever at top level,
    // shrinking as we go down.

    private static void writeUnorderedNode(LWComponent node, PrintWriter out)
    {
        final java.util.List<LWComponent> topChildren = node.getChildList();
        final LWComponent[] ordered = topChildren.toArray(new LWComponent[topChildren.size()]);
        java.util.Arrays.sort(ordered, LWComponent.GridSorter);

        for (LWComponent c : ordered) {
            if (c.hasLabel()) {
                out.print("<h2>");
                writeLabel(c, out);
                out.print("</h2>");
            }
            writeNode(c, out);
        }
    }
    
    private static void writeNode(LWComponent c, PrintWriter out) {
        if (c instanceof LWGroup || c instanceof LWSlide)
            writeUnorderedNode(c, out);
        else if (c instanceof LWLink)
            ;
        else
            writeOrderedChildren(c, out);
    }
    
    private static void writeOrderedChildren(LWComponent node, PrintWriter out)
    {
        
        if (node.hasChildren()) {
            out.println("<ul>");
            for (LWComponent c : node.getChildList()) {
                out.print("<li>");
                writeLabel(c, out);
                if (c.hasChildren())
                    writeNode(c, out);
                //writeNode(node, out);
                out.println();
            }
            out.println("</ul>");
        }
        
    }

    private static void writeLabel(LWComponent node, PrintWriter out)
    {

        if (node.hasResource())
            out.println("<a href=\"" + node.getResource() + "\">");

        java.awt.Font f = node.getFont();
        if (f.isBold()) out.print("<b>");
        if (f.isItalic()) out.print("<i>");

        boolean didFontColor = false;
        if (node.getTextColor() != null && !java.awt.Color.black.equals(node.getTextColor())) {
            out.print("<font color=\"" + node.getXMLtextColor() + "\">");
            didFontColor = true;
        }
        
        out.print(node.getLabel());
        if (f.isBold()) out.print("</b>");
        if (f.isItalic()) out.print("</i>");

        if (didFontColor)
            out.print("</font>");

        if (node.hasResource())
            out.print("</a>");
        

    }
    
    public static boolean saveMap(LWMap map) {
        return saveMap(map, false,false);
    }
    
    public static void main(String args[])
        throws IOException
    {
        //VUE.parseArgs(args);
        
        LWMap map = ActionUtil.unmarshallMap(new File(args[0]));
        System.err.println("Got map: " + map);

        if (args.length > 1)
            writeHTMLOutline(map, new File(args[1]));
        else
            writeHTMLOutline(map, (File) null);

        
//         LWMap map = new LWMap("test");
//         map.setFile(new File("test.xml"));
//         map.addNode(new LWNode("Test Node"));
//         System.err.println("Attempting to save test map " + map);
//         DEBUG.Enabled = DEBUG.INIT = true;
//         new SaveAction().saveMap(map, false);
    }

    
    /*
    public void actionPerformed_writes_over_other_saved_maps(ActionEvent e)
    {
        System.out.println("Action["+e.getActionCommand()+"] invoked...");
         
        boolean saveCondition = true;
        
        if (isSaveAs() || file == null)
        {
          file = ActionUtil.selectFile("Save Map", "xml");
          
          if (file == null)
              saveCondition = false;
        }
        
        if (saveCondition == true)
        {
            if (file.getName().endsWith(".xml")) {
                LWMap map = tufts.vue.VUE.getActiveMap();
                map.setLabel(file.getName());
                ActionUtil.marshallMap(file, map);
            }
          else if (file.getName().endsWith(".jpeg"))
            new ImageConversion().createJpeg(file);
          
          else if (file.getName().endsWith(".svg"))
            new SVGConversion().createSVG(file);
          
          else if (file.getName().endsWith(".pdf"))
            new PDFTransform().convert(file);
          
          else if (file.getName().endsWith(".html"))
            new HTMLConversion().convert(file);
          
          System.out.println("Saved " + getFileName());
        }
            
        System.out.println("Action["+e.getActionCommand()+"] completed.");
    }
    */

    
}






