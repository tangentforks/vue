/*
 * Copyright 2003-2007 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue.action;

import tufts.Util;
import tufts.vue.VueUtil;
import tufts.vue.VUE;
import tufts.vue.LWMap;
import tufts.vue.VueFileFilter;
import tufts.vue.VueResources;
import tufts.vue.XMLUnmarshalListener;
import tufts.vue.DEBUG;
import tufts.vue.gui.VueFileChooser;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.MarshalListener;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.UnmarshalListener;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.tufts.vue.preferences.PreferencesManager;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import java.net.URL;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;

/**
 * A class which defines utility methods for any of the action class.
 * Most of this code is for save/restore persistence thru castor XML.
 *
 * @version $Revision: 1.98 $ / $Date: 2008-02-25 20:49:49 $ / $Author: sfraize $
 * @author  Daisuke Fujiwara
 * @author  Scott Fraize
 */
// TODO: rename / relocate most of this code! -- SMF
public class ActionUtil
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ActionUtil.class);
    
    private final static String XML_MAPPING_CURRENT_VERSION_ID = VueResources.getString("mapping.lw.current_version");
    private final static URL XML_MAPPING_DEFAULT =      VueResources.getURL("mapping.lw.version_" + XML_MAPPING_CURRENT_VERSION_ID);
    private final static URL XML_MAPPING_UNVERSIONED =  VueResources.getURL("mapping.lw.version_none");
    private final static URL XML_MAPPING_OLD_RESOURCES =VueResources.getURL("mapping.lw.version_resource_fix");

    private final static String VUE_COMMENT_START = "<!-- Do Not Remove:";
    private final static String OUTPUT_ENCODING = "US-ASCII";
    private final static String DEFAULT_WINDOWS_ENCODING = "windows-1252"; // (a.k.a Cp1252) for reading pre ASCII enforced save files from Windows
    private final static String DEFAULT_MAC_ENCODING = "UTF-8"; // "MacRoman" not supported on Windows platform
    private final static String DEFAULT_INPUT_ENCODING = "UTF-8"; // safest default input encoding
    
    // Note: the encoding format of the incoming file will normally either be UTF-8 for
    // older VUE save files, or US-ASCII for newer files.  In any case, the encoding is
    // indicated in the <?xml> tag at the top of the file, and castor handles adjusting
    // for it.  If we want to write UTF-8 files, we have to be sure the stream that's
    // created to write the file is created with the same encoding, or we sometimes get
    // problems, depending on the platform.  We always READ (unmarshall) via a UTF-8
    // stream, no matter what, as US-ASCII will pass through a UTF-8 stream untouched,
    // and it will handle UTF-8 if that turns out to be the encoding.
    
    public ActionUtil() {}
    
    /**A static method which displays a file chooser for the user to choose which file to save into.
       It returns the selected file or null if the process didn't complete*/
    public static File selectFile(String title, final String fileType)
    {
        File picked = null;
        final VueFileChooser chooser = new VueFileChooser();
      JFileChooser choose = new JFileChooser();
     
        
        chooser.setDialogTitle(title);
        chooser.setAcceptAllFileFilterUsed(false);    
        //chooser.set
        chooser.addPropertyChangeListener(new PropertyChangeListener()
        {
			public void propertyChange(PropertyChangeEvent arg0) {				
				if (arg0.getPropertyName() == VueFileChooser.FILE_FILTER_CHANGED_PROPERTY)
				{
				
					String baseName = null;
			        if (VUE.getActiveMap().getFile() == null)
			        	baseName = VUE.getActiveMap().getLabel();
			        else
			        {			        	
			        	baseName = VUE.getActiveMap().getLabel();
			    		if (baseName.indexOf(".") > 0)
			    			baseName = VUE.getActiveMap().getLabel().substring(0, baseName.lastIndexOf("."));
			    		baseName = baseName.replaceAll("\\*","") + "-copy";
			        }
			     
			        if (fileType == null)
			        {
			        	chooser.setSelectedFile(new File(baseName.replaceAll("\\*", "")));
			        }
				}
			}
        });
        
        if (fileType != null && !fileType.equals("export"))
         chooser.setFileFilter(new VueFileFilter(fileType)); 
        else if (fileType != null && fileType.equals("export"))
        {
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.JPEG_DESCRIPTION));
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.PNG_DESCRIPTION));
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.SVG_DESCRIPTION));        	
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.IMS_DESCRIPTION));
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.IMAGEMAP_DESCRIPTION));
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.ZIP_DESCRIPTION));
        }
        else
        {
            VueFileFilter defaultFilter = new VueFileFilter(VueFileFilter.VUE_DESCRIPTION);
            
            chooser.addChoosableFileFilter(defaultFilter);  
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.VPK_DESCRIPTION));
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.IMAGEMAP_DESCRIPTION));
            chooser.addChoosableFileFilter(new VueFileFilter("PDF"));
            
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.JPEG_DESCRIPTION));
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.PNG_DESCRIPTION));
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.SVG_DESCRIPTION));
            //chooser.addChoosableFileFilter(new VueFileFilter("html"));
            
            chooser.addChoosableFileFilter(new VueFileFilter("RDF"));
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.IMS_DESCRIPTION));
            chooser.addChoosableFileFilter(new VueFileFilter(VueFileFilter.ZIP_DESCRIPTION));
            
            //chooser.addChoosableFileFilter(new VueFileFilter("HTML Outline", "htm"));
            
            chooser.setFileFilter(defaultFilter); 
        }
         
    //    JPanel p1 = (JPanel)chooser.getComponent(2);
      //  JPanel p2 = (JPanel)p1.getComponent(2);
       // JPanel p3 = (JPanel)p2.getComponent(2);
       // JComboBox box = (JComboBox)p3.getComponent(3);
        //box.g
       // box.setRenderer(new PaddedCellRenderer());
        
        if(VueUtil.isCurrentDirectoryPathSet()) 
          chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        
        int option = chooser.showDialog(VUE.getDialogParentAsFrame(), "Save");
        
        if (option == VueFileChooser.APPROVE_OPTION) 
        {
            picked = chooser.getSelectedFile();
            
            String fileName = picked.getAbsolutePath();
            //String extension = chooser.getFileFilter().getDescription();
              String extension = ((VueFileFilter)chooser.getFileFilter()).getExtensions()[0];  
            //if it isn't a file name with the right extension 
            if (!fileName.endsWith("." + extension)) {
                fileName += "." + extension;
                picked = new File(fileName);
            }
            
            if (picked.exists()) {
                int n = JOptionPane.showConfirmDialog(null, VueResources.getString("replaceFile.text") + " \'" + picked.getName() + "\'", 
                        VueResources.getString("replaceFile.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                  
                if (n == JOptionPane.NO_OPTION)
                    picked = null;
            } 
            
            if (picked != null)
                VueUtil.setCurrentDirectoryPath(picked.getParent());
        }
        
        return picked;
    }
    
   
    /**A static method which displays a file chooser for the user to choose which file to open.
       It returns the selected file or null if the process didn't complete
    TODO BUG: do not allow more than one dialog open at a time -- two "Ctrl-O" in quick succession
    will open two open file dialogs. */
    
    public static File openFile(String title, String extension)
    {
        File file = null;              
        
        VueFileChooser chooser = null;        
    	
    	if (!Util.isMacPlatform())
    	{
    		chooser = new VueFileChooser();
    		if (VueUtil.isCurrentDirectoryPathSet()) 
    			chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));
    	}
    	else
    	{
    		
    		if (VueUtil.isCurrentDirectoryPathSet()) 
    		{
    			/*
    			 * Despite Quaqua fixes in 3.9 you can still only set the 
    			 * current directory if you set it in the constructor, 
    			 * setCurrentDirectory fails to do anything but cause the
    			 * top bar and the panels to be out of sync.... -MK 10/29
    			 */
    			chooser = new VueFileChooser(new File(VueUtil.getCurrentDirectoryPath()));
    		}
    		else
    			chooser = new VueFileChooser();

    	}  
        
        int option = chooser.showOpenDialog(VUE.getDialogParent());
        
        if (option == VueFileChooser.APPROVE_OPTION) {
            final File chooserFile = chooser.getSelectedFile();
            if (chooserFile == null)
                return null;
            final String fileName;
            final String chosenPath = chooserFile.getAbsolutePath();
            
            // if they type a file name w/out an extension
            if (chooserFile.getName().indexOf('.') < 0)
                fileName = chosenPath + "." + extension;
            else
                fileName = chosenPath;

            file = new File(fileName);

            if (file.exists()) {
                VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
            } else {
                File dir = new File(chosenPath);
                if (dir.exists() && dir.isDirectory()) {
                    //System.out.println("chdir " + chosenPath);
                    VueUtil.setCurrentDirectoryPath(chosenPath);
                } else {
                    Log.debug("File '" + chosenPath + "' " + file + " can't  be found.");
                    tufts.vue.VueUtil.alert(chooser, "Could not find " + file, "File Not Found");
                }
                file = null;
            }
        }
        return file;
    }
    
    /**A static method which displays a file chooser for the user to choose which file to open.
    It returns the selected file or null if the process didn't complete
 	TODO BUG: do not allow more than one dialog open at a time -- two "Ctrl-O" in quick succession
 	will open two open file dialogs. */
 
    public static File[] openMultipleFiles(String title, String extension)
    {
    	File file = null;
     
    	VueFileChooser chooser = null;        
    	
    	if (!Util.isMacPlatform())
    	{
    		chooser = new VueFileChooser();
    		if (VueUtil.isCurrentDirectoryPathSet()) 
    			chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));
    	}
    	else
    	{
    		
    		if (VueUtil.isCurrentDirectoryPathSet()) 
    		{
    			/*
    			 * Despite Quaqua fixes in 3.9 you can still only set the 
    			 * current directory if you set it in the constructor, 
    			 * setCurrentDirectory fails to do anything but cause the
    			 * top bar and the panels to be out of sync.... -MK 10/29
    			 */
    			chooser = new VueFileChooser(new File(VueUtil.getCurrentDirectoryPath()));
    		}
    		else
    			chooser = new VueFileChooser();

    	}
    	chooser.setDialogTitle(title);
    	chooser.setMultiSelectionEnabled(true);
    	chooser.setFileFilter(new VueFileFilter(extension));
     
    	int option = chooser.showOpenDialog(VUE.getDialogParent());
     
    	if (option == VueFileChooser.APPROVE_OPTION) {
         final File[] chooserFile = chooser.getSelectedFiles();
         if (chooserFile == null)
             return null;
         final String fileName;
         
         if (chooserFile.length == 1)
         {
        	 //this scenario can only happen if there's only 1 file in the array...
         
        	 final String chosenPath = chooserFile[0].getAbsolutePath();
         
        	 // if they type a file name w/out an extension
        	 if (chooserFile[0].getName().indexOf('.') < 0)
        		 fileName = chosenPath + "." + extension;
        	 else
        		 fileName = chosenPath;

        	 chooserFile[0] = new File(fileName);

        	 if (chooserFile[0].exists()) {
        		 VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
        	 } else {
        		 File dir = new File(chosenPath);
        		 if (dir.exists() && dir.isDirectory()) {
                 //System.out.println("chdir " + chosenPath);
        			 VueUtil.setCurrentDirectoryPath(chosenPath);
        		 } else {
        			 Log.debug("File '" + chosenPath + "' " + file + " can't  be found.");
        			 tufts.vue.VueUtil.alert(chooser, "Could not find " + file, "File Not Found");
        		 }
        		 chooserFile[0] = null;
        	 }
         	}
         return chooserFile;
    	}
    	return null;
    }

    
    /**
     * Return the current mapping used for saving new VUE data.
     */
    public static Mapping getDefaultMapping()
    {
        Object result = _loadMapping(XML_MAPPING_DEFAULT);
        if (result instanceof Exception) {
            JOptionPane.showMessageDialog(null, "Mapping file error: will be unable to load or save maps!"
                                          + "\nMapping url: " + XML_MAPPING_DEFAULT
                                          + "\n" + result,
                                          "XML Mapping File Exception", JOptionPane.ERROR_MESSAGE);
        }
        return (Mapping) result;
    }

    public static Unmarshaller getDefaultUnmarshaller()
        throws org.exolab.castor.mapping.MappingException
    {
        return getDefaultUnmarshaller(null, "(unknown source)");
    }
    public static Unmarshaller getDefaultUnmarshaller(String sourceName)
        throws org.exolab.castor.mapping.MappingException
    {
        return getDefaultUnmarshaller(null, sourceName);
    }


	    
    
    /**
     * Return the default unmarshaller for VUE data, which includes an installed
     * unmarshall listener, which is required for the proper restoration of VUE objects.
     */
    public static Unmarshaller getDefaultUnmarshaller(Mapping mapping, String sourceName)
        throws org.exolab.castor.mapping.MappingException
    {
        if (mapping == null)
            mapping = getDefaultMapping();
        
        // todo: can cache this with it's mapping set (tho need to cache by mapping,
        // as we still have different mapping files for old versions of the VUE save file)
        Unmarshaller unmarshaller = new Unmarshaller();
        
        unmarshaller.setIgnoreExtraAttributes(true);
        unmarshaller.setIgnoreExtraElements(true);
        unmarshaller.setValidation(false);
        unmarshaller.setObjectFactory(new XMLObjectFactory());
        //unmarshaller.setWhitespacePreserve(true); // doesn't affect elements!  (e.g. <notes> foo bar </notes>)
        // HOWEVER: castor 0.9.7 now automatically encodes/decodes white space for attributes...
        /*
        Logger logger = new Logger(System.err);
        if (sourceName != null)
            logger.setPrefix("Castor " + sourceName);
        else
            logger.setPrefix("Castor");
        unmarshaller.setLogWriter(logger);
        */
        unmarshaller.setLogWriter(new PrintWriter(System.err));

        if (DEBUG.XML) unmarshaller.setDebug(true);
        
        unmarshaller.setUnmarshalListener(new VueUnmarshalListener());
        unmarshaller.setMapping(mapping);

        if (DEBUG.CASTOR || DEBUG.XML || DEBUG.IO)
            Log.debug("got default unmarshaller for mapping " + mapping + " source " + sourceName);

        return unmarshaller;
    }
    

    private static Mapping getMapping(URL mappingSource) {
        if (DEBUG.IO) System.out.println("Fetching mapping: " + mappingSource);
        Object result = _loadMapping(mappingSource);
        if (result instanceof Mapping)
            return (Mapping) result;
        else
            return null;
    }
    
    private static HashMap LoadedMappings = new HashMap();
    /** return's a Mapping if successful, or an Exception if not.
     * Results are cached (if load was successful) for future calls.*/
    private static Object _loadMapping(URL mappingSource)
    //throws java.io.IOException //, org.exolab.castor.mapping.MappingException
    {
        if (LoadedMappings.containsKey(mappingSource))
            return (Mapping) LoadedMappings.get(mappingSource);
        Mapping mapping = new Mapping();
        if (DEBUG.IO || DEBUG.INIT) Log.debug("Loading mapping " + mappingSource + "...");
        try {
            mapping.loadMapping(mappingSource);
        } catch (Exception e) { // MappingException or IOException
            e.printStackTrace();
            System.err.println("Failed to load mapping " + mappingSource);
            return e;
        }
        if (DEBUG.IO || DEBUG.INIT) Log.debug("Loaded mapping " + mappingSource);
        LoadedMappings.put(mappingSource, mapping);
        return mapping;
    }
    
    /**A static method which creates an appropriate marshaller and marshal the active map*/
    public static void marshallMap(File file)
    {
        marshallMap(file, tufts.vue.VUE.getActiveMap());
    }

    /**
     * Marshall the given map to XML and write it out to the given file.
     */
    public static void marshallMap(File file, LWMap map) {
        try {
            doMarshallMap(file, map);
        } catch (Throwable t) {
            if (t instanceof WrappedMarshallException)
                t = t.getCause();
            Log.error("marshalling: " + map + "; " + t);
            Util.printStackTrace(t);
            // until everyone has chance to update their code
            // to handle the exceptions, wrap this in a runtime exception.
            throw new RuntimeException(t);
        }
    }

    private static class WrappedMarshallException extends RuntimeException {
        WrappedMarshallException(Throwable cause) {
            super(cause);
        }
    }

    private static void doMarshallMap(File file, LWMap map)
        throws java.io.IOException,
               org.exolab.castor.mapping.MappingException,
               org.exolab.castor.xml.MarshalException,
               org.exolab.castor.xml.ValidationException
    {
        final String path = file.getAbsolutePath().replaceAll("%20"," ");
        final Writer writer;
        if (OUTPUT_ENCODING.equals("UTF-8") || OUTPUT_ENCODING.equals("UTF8")) {
            writer = new OutputStreamWriter(new FileOutputStream(path), OUTPUT_ENCODING);
        } else {
            writer = new FileWriter(path);
            // For the actual file writer we can use the default encoding because
            // we're marshalling specifically in US-ASCII.  E.g., because we direct
            // castor to fully encode any special characters via
            // setEncoding("US-ASCII"), we'll only have ASCII chars to write anyway,
            // and any default encoding will handle that...
                
        }

        marshallMapToWriter(writer, map, file);
        writer.close();
    }
    
    private static class VueMarshalListener implements MarshalListener {
        public boolean preMarshal(Object o) {
            //if (true||DEBUG.XML) Log.debug("VML  pre: " + Util.tags(o));
            //if (o instanceof tufts.vue.Resource)
            try {
                // TODO: create a ConditionalMarshalling interface for embedding this logic
                // in the client classes so it's not kept here.
                if (o instanceof tufts.vue.PropertyEntry && ((tufts.vue.PropertyEntry)o).getEntryKey().startsWith("@")) {
                    if (DEBUG.Enabled) Log.debug("Skipping " + Util.tags(o));
                    return false;
                } else {
                    if (DEBUG.XML) Log.debug("Marshalling " + Util.tags(o));
                    return true;
                }
            } catch (Throwable t) {
                Util.printStackTrace(t, "Marshalling condition failure on " + o);
            }
            return true;
        }
        public void postMarshal(Object o) {
            //if (true||DEBUG.XML) Log.debug("VML post: " + Util.tags(o));
        }
    }
    /**
     * Marshall the given map to the given Writer without touching the map in any
     * way.
     */
    public static void marshallMapToWriter(final LWMap map, final Writer writer)
        throws java.io.IOException,
               org.exolab.castor.mapping.MappingException,
               org.exolab.castor.xml.MarshalException,
               org.exolab.castor.xml.ValidationException
    {
        marshallMapToWriter(writer, map, null);
    }
    
    /**
     * @param file - if null, map state is untouched, otherwise, map state is updated
     */
    private static void marshallMapToWriter(final Writer writer, final LWMap map, final File file)
        throws java.io.IOException,
               org.exolab.castor.mapping.MappingException,
               org.exolab.castor.xml.MarshalException,
               org.exolab.castor.xml.ValidationException
    {

        Marshaller marshaller = null;
        writer.write(VUE_COMMENT_START
                     + " VUE mapping "
                     + "@version(" + XML_MAPPING_CURRENT_VERSION_ID + ")"
                     + " " + XML_MAPPING_DEFAULT
                     + " -->\n");
        writer.write(VUE_COMMENT_START
                     + " Saved date " + new java.util.Date()
                     + " by " + VUE.getSystemProperty("user.name")
                     + " on platform " + VUE.getSystemProperty("os.name")
                     + " " + VUE.getSystemProperty("os.version")
                     + " in JVM " + VUE.getSystemProperty("java.runtime.version")
                     + " -->\n");
        writer.write(VUE_COMMENT_START
                     + " Saving version " + tufts.vue.Version.WhatString
                     + " -->\n");
        if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Wrote VUE header to " + writer);
        marshaller = new Marshaller(writer);
        //marshaller.setDebug(DEBUG.CASTOR);
        marshaller.setEncoding(OUTPUT_ENCODING);
        // marshal as document (default): make sure we add at top: <?xml version="1.0" encoding="<encoding>"?>
        marshaller.setMarshalAsDocument(true);
        marshaller.setNoNamespaceSchemaLocation("none");
        marshaller.setMarshalListener(new VueMarshalListener());
        // setting to "none" gets rid of all the spurious tags like these:
        // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"

        //marshaller.setDoctype("foo", "bar"); // not in 0.9.4.3, must wait till we can run 0.9.5.3+

        /*
          marshaller.setMarshalListener(new MarshalListener() {
          public boolean preMarshal(Object o) {
          System.out.println(" preMarshal " + o.getClass().getName() + " " + o);
          return true;
          }
          public void postMarshal(Object o) {
          System.out.println("postMarshal " + o.getClass().getName() + " " + o);
          }
          });
        */

        //marshaller.setRootElement("FOOBIE"); // overrides name of root element
            
        marshaller.setMapping(getDefaultMapping());

        //----------------------------------------------------------------------------------------
        // 
        // 2007-10-01 SMF -- turning off validation during marshalling now required
        // w/castor-1.1.2.1-xml.jar, otherwise, for some unknown reason, LWLink's
        // with any connected endpoints cause validation exceptions when attempting to
        // save.  E.g, from a map with one node and one link connected to it:
        //
        // ValidationException: The following exception occured while validating field: childList of class:
        // tufts.vue.LWMap: The object associated with IDREF "LWNode[2         "New Node"  +415,+24 69x22]" of type
        // class tufts.vue.LWNode has no ID!;
        // - location of error: XPATH: /LW-MAP
        // The object associated with IDREF "LWNode[2         "New Node"  +415,+24 69x22]" of type class tufts.vue.LWNode has no ID!
        //
        // Even tho the node's getID() is correctly returning "2"
        //
        marshaller.setValidation(false); 
        //----------------------------------------------------------------------------------------
            
        /*
          Logger logger = new Logger(System.err);
          logger.setPrefix("Castor ");
          marshaller.setLogWriter(logger);
        */
        marshaller.setLogWriter(new PrintWriter(System.err));

        // Make modifications to the map at the last minute, so any prior exceptions leave the map untouched.

        final int oldModelVersion = map.getModelVersion();
        final File oldSaveFile = map.getFile();
        if (file != null) {
            map.setModelVersion(LWMap.getCurrentModelVersion());
            // note that if this file is different from it's last save file, this
            // operation may cause any/all of the resources in the map to be
            // updated before returning.
            map.setFile(file);
        }
            
        //if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Marshalling " + map + " ...");
        Log.debug("marshalling " + map + " ...");

        try {
            marshaller.marshal(map);
            Log.debug("marshalled " + map + " to " + writer + "; file=" + file);
            writer.flush();
            //if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Completed marshalling " + map);
        } catch (Throwable t) {
            try {
                if (file != null) {
                    // revert map model version & save file
                    map.setModelVersion(oldModelVersion);
                    map.setFile(oldSaveFile);
                }
            } catch (Throwable tx) {
                Util.printStackTrace(tx);
            } finally {
                throw new WrappedMarshallException(t);
            }
        }
            
        if (file != null) {
            map.markAsSaved();
            Log.debug("saved " + map + " to " + file);
        }

        //map.setFile(file);

        //if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Wrote " + file);

    }

    private static class VueUnmarshalListener implements UnmarshalListener {
        public void initialized(Object o) {
            //if (DEBUG.XML) System.out.println("**** VUL initialized " + o.getClass().getName() + " " + tos(o));
            if (DEBUG.XML) Log.debug("VUL  initialized: " + Util.tags(o));
            if (o instanceof XMLUnmarshalListener)
                ((XMLUnmarshalListener)o).XML_initialized();
        }
        public void attributesProcessed(Object o) {
            //if (DEBUG.XML) System.out.println("      got attributes " + o.getClass().getName() + " " + tos(o));
            if (DEBUG.XML) Log.debug("VUL   attributes: " + Util.tags(o));
        }
        public void unmarshalled(Object o) {
            //if (DEBUG.XML||DEBUG.CASTOR) System.out.println("VUL unmarshalled " + o.getClass().getName() + " " + tos(o));
            if (DEBUG.XML||DEBUG.CASTOR) Log.debug("VUL unmarshalled: " + Util.tags(o));
            
            if (o instanceof XMLUnmarshalListener)
                ((XMLUnmarshalListener)o).XML_completed();
        }
        public void fieldAdded(String name, Object parent, Object child) {
            if (DEBUG.XML){
                Log.debug("VUL   fieldAdded: parent: " + Util.tags(parent) + " newChild[" + name + "] " + Util.tags(child) + "\n");
                //System.out.println("VUL fieldAdded: parent: " + parent.getClass().getName() + "\t" + tos(parent) + "\n"
                //+ "             new child: " +  child.getClass().getName() + " \"" + name + "\" " + tos(child) + "\n");
            }
            if (parent instanceof XMLUnmarshalListener)
                ((XMLUnmarshalListener)parent).XML_fieldAdded(name, child);
            if (child instanceof XMLUnmarshalListener)
                ((XMLUnmarshalListener)child).XML_addNotify(name, parent);
        }

//         // exception trapping toString in case the object isn't initialized enough
//         // for it's toString to work...
//         private String tos(Object o) {
//             if (o == null)
//                 return "<null-object>";
            
//             String s = o.getClass().getName() + " ";
//             //String s = null;
//             String txt = null;
//             try {
//                 txt = o.toString();
//                 if (
//             } catch (Throwable t) {
//                 txt = t.toString();
//                 // "[" + t.toString() + "]";
//             }
//             return s;
//         }
    }

    /** Unmarshall a LWMap from the given file (XML map data) */
    public static LWMap unmarshallMap(File file)
        throws IOException
    {
//         if (file.isDirectory())
//             throw new Error("Is a directory, not a map: " + file);
        return unmarshallMap(file.toURL());
        //return unmarshallMap(file.toURI().toURL());
    }

    /** Unmarshall a LWMap from the given URL (XML map data) */
    /*
    public static LWMap unmarshallMap(java.net.URL url)
        throws java.io.IOException
    {
        //return unmarshallMap(url, getDefaultMapping());
        return unmarshallMap(url, null);
    }
    */

    /** Unmarshall a LWMap from the given URL using the given mapping */
    /*
    private static LWMap unmarshallMap(java.net.URL url, Mapping mapping)
        throws java.io.IOException
    {
        return unmarshallMap(url, mapping, DEFAULT_INPUT_ENCODING);
    }
    */

    public static LWMap unmarshallMap(java.net.URL url)
        throws IOException
    {
        // We scan for lines at top of file that are comments.  If there are NO comment lines, the
        // file is of one of our original save formats that is not versioned, and that may need
        // special processing for the Resource class to Resource interface change over.  If there
        // are comments, the version instance of the string "@version(##)" will set the version ID
        // to ##, and we'll use the mapping appropriate for that version of the save file.

        // We ALWAYS read with an input encoding of UTF-8, even if the XML was written with a
        // US-ASCII encoding.  This is because pure ascii will translate fine through UTF-8, but in
        // case it winds up being that the XML was written out my the marshaller with a UTF-8
        // encoding, we're covered.

        // (tho maybe with very old save files with platform specific encodings, (e.g, MacRoman or
        // windows-1255/Cp1255) we'll lose a special char here or there, such as left-quote /
        // right-quote).
        
        if (DEBUG.CASTOR || DEBUG.IO) {
            Log.debug("\nunmarshallMap: " + url);
            //Util.printStackTrace("UM " + url);
        }

        final BufferedReader reader;
        
        if ("file".equals(url.getProtocol())) {
            File file = new File(url.getPath());
            if (file.isDirectory())
                throw new MapException("is directory");
            reader = new BufferedReader(new FileReader(file));
        } else {
            reader = new BufferedReader(new InputStreamReader(tufts.vue.UrlAuthentication.getAuthenticatedStream(url)));
            //reader = new BufferedReader(new InputStreamReader(url.openStream()));
        }
        
        String firstNonCommentLine;
        String versionID = null;
        boolean savedOnWindowsPlatform = false;
        boolean savedOnMacPlatform = false;
        String guessedEncoding = null;
        Mapping mapping = null;
    
        // We need to skip past the comments to position the reader at the <?xml line for
        // unmarshalling to start.  Also, we look at these comments to determine version of the
        // mapping to use, as well as if it's a pre VUE 1.5 (August 2006) save file, in which case
        // we must guess an encoding, and re-open the file using an InputStreamReader with the
        // proper encoding.

        String savingVersion = "unknown VUE version";

        for (;;) {
            reader.mark(2048); // a single comment line can't be longer than this...
            String line = reader.readLine();
            if (line == null) {
                System.err.println("Unexpected end-of-stream in [" + url + "]");
                throw new java.io.IOException("end of stream in " + url);
            }
            if (DEBUG.CASTOR || DEBUG.IO) Log.debug("Scanning[" + line + "]");
            if (line.startsWith("<!--") == false) {
                // we should have just hit thie "<?xml ..." line -- done with comments
                firstNonCommentLine = line;
                break;
            }

            if (line.startsWith(VUE_COMMENT_START + " Saved")) {

                // The "saved on platform" comments were never expected to be used functionally
                // (only for debug), so determining if the save file was written on a Windows box
                // it's not 100% reliable: e.g., if a user somehow had the name "platform Windows",
                // we would mistake this "saved by" for a "saved on", but we're just going to take
                // this risk -- this is just a workaround backward compat hack because castor
                // wasn't naming the real encoding in it's XML output (turns out it was always
                // puting UTF-8), even if it was using the default Windows encoding of
                // Cp1252/windows-1252.
                
                //if (DEBUG.IO) System.out.println("scanning for Windows platform...");
                if (line.indexOf("platform Windows") > 0) {
                    if (DEBUG.IO) System.out.println(url + " was saved in the Windows environment");
                    savedOnWindowsPlatform = true;
                } else if (line.indexOf("platform Mac") > 0) {
                    if (DEBUG.IO) System.out.println(url + " was saved in the Mac environment");
                    savedOnMacPlatform = true;
                }
            } else if (line.startsWith(VUE_COMMENT_START + " Saving version")) {
                if (DEBUG.IO) System.out.println("Found saving version line: " + line);
                final int savingVersionIndex = line.indexOf("VUE");
                if (savingVersionIndex > 0) {
                    savingVersion = line.substring(line.indexOf("VUE"), line.length());
                    if (savingVersion.indexOf("-->") > 10)
                        savingVersion = savingVersion.substring(0, savingVersion.indexOf("-->"));
                    savingVersion = savingVersion.trim();
                } else {
                    Log.warn(url + ": unknown saving version XML comment [" + line + "]");
                }
                if (DEBUG.IO) System.out.println("Saving version: [" + savingVersion + "]");
            }
                
            
            
            // Scan the comment line for a version tag to base our mapping on:
            int idx;
            if ((idx = line.indexOf("@version(")) >= 0) {
                String s = line.substring(idx);
                //System.out.println("Found version start:" + s);
                int x = s.indexOf(')');
                if (x > 0) {
                    versionID = s.substring(9,x);
                    if (DEBUG.CASTOR || DEBUG.IO) Log.debug(url + "; Found mapping version ID[" + versionID + "]");
                    if (versionID.equals(XML_MAPPING_CURRENT_VERSION_ID)) {
                        mapping = getDefaultMapping();
                    } else {
                        URL mappingURL = VueResources.getURL("mapping.lw.version_" + versionID);
                        if (mappingURL == null) {
                            Log.error("Failed to find mapping for version tag [" + versionID + "], attempting default.");
                            mapping = getDefaultMapping();
                        } else {
                            mapping = getMapping(mappingURL);
                        }
                    }
                }
            }
        }
        reader.close();

        if (firstNonCommentLine.startsWith("<?xml")) {
            // Check to see if we need to use a non-default input encoding.
            // NOTE: We make sure we only attempt guessedEncoding if the given encoding is
            // the default input encoding: otherwise assume we're here recursively,
            // after already guessing at an encoding (otherwise, we'll loop, and blow stack)
            if (DEBUG.IO) System.out.println("XML head [" + firstNonCommentLine + "]");
            
            if (firstNonCommentLine.indexOf("encoding=\"UTF-8\"") > 0) {

                boolean localEncoding = false;
                
                // If encoding is UTF-8, this a 2nd generation save file (mapping is
                // versioned, but not all US-ASCII encoding): the actual encoding is
                // unknown: make a guess as how to best handle it.  This is our rule: if
                // we're on the SAME platform as the save file, assuming the local
                // encoding (assume it's the same user, on the same machine, and the
                // current default system encoding is the same one that was active when
                // the file was originally saved).  If we're on a different platform,
                // assume a default encoding for that platform.
                
                if (Util.isWindowsPlatform()) {
                    if (savedOnWindowsPlatform) {
                        localEncoding = true;
                    } else if (savedOnMacPlatform)
                        guessedEncoding = DEFAULT_MAC_ENCODING;
                    else
                        guessedEncoding = DEFAULT_WINDOWS_ENCODING;
                } else if (Util.isMacPlatform()) {
                    if (savedOnMacPlatform)
                        localEncoding = true;
                    else if (savedOnWindowsPlatform)
                        guessedEncoding = DEFAULT_WINDOWS_ENCODING;
                    else
                        guessedEncoding = DEFAULT_MAC_ENCODING;
                }
                
                if (localEncoding)
                    guessedEncoding = Util.getDefaultPlatformEncoding();
                    
                Log.info(url + "; assuming "
                             + (localEncoding ? "LOCAL " : "PLATFORM DEFAULT ")
                             + "\'" + guessedEncoding + "\' charset encoding");

                // Note: doing this is a real tradeoff amongst bugs: any old save file
                // that had fancy unicode characters in UTF, such as something in a
                // japanese charset, will be screwed by this, so we optimizing for what
                // we think is the most likely case.  if this becomes a real problem, we
                // could introduce a special convert dialog.  Also, pre US-ASCII save
                // files could have different strings in them saved in many DIFFERENT
                // charsets (e.g., japanese, UTF, etc), and it's complete luck as to
                // when those charsets would each be properly handled.
                
            }
        } else {
            Log.warn("Missing XML header in [" + firstNonCommentLine + "]");
        }

        boolean oldFormat = false;

        if (versionID == null && mapping == null) {
            oldFormat = true;
            Log.info(url + "; save file is of old pre-versioned type.");
            mapping = getMapping(XML_MAPPING_UNVERSIONED);
        }
        
        if (mapping == null)
            mapping = getDefaultMapping();        

        final String encoding = guessedEncoding == null ? DEFAULT_INPUT_ENCODING : guessedEncoding;

        return unmarshallMap(url, mapping, encoding, oldFormat, savingVersion);
    }


    private static LWMap unmarshallMap(final java.net.URL url, Mapping mapping, String charsetEncoding, boolean allowOldFormat, String savingVersion)
      //throws IOException, org.exolab.castor.mapping.MappingException, org.exolab.castor.xml.ValidationException
        throws IOException
    {
        LWMap map = null;

        //if (DEBUG.CASTOR || DEBUG.IO) System.out.println("UNMARSHALLING: " + url + " charset=" + charsetEncoding);
        Log.debug("unmarshalling: " + url + "; charset=" + charsetEncoding);

        final InputStream urlStream;

        if ("file".equals(url.getProtocol()))
            urlStream = url.openStream();
        else
            urlStream = tufts.vue.UrlAuthentication.getAuthenticatedStream(url);
        
        final BufferedReader reader = new BufferedReader(new InputStreamReader(urlStream, charsetEncoding));

        // Skip over comments to get to start of XML

        for (;;) {
            reader.mark(2048); // a single comment line can't be longer than this...
            String line = reader.readLine();
            if (line == null) {
                System.err.println("Unexpected end-of-stream in [" + url + "]");
                throw new java.io.IOException("end of stream in " + url);
            }
            if (line.startsWith("<!--") == false) {
                // we should have just hit thie "<?xml ..." line -- done with comments
                break;
            }
            if (DEBUG.CASTOR || DEBUG.IO) System.out.println("Skipping[" + line + "]");
        }

        // Reset the reader to the start of the last line read, which should be the <?xml line,
        // which is what castor needs to see at start (it can't handle ignoring comments...)
        reader.reset();

        final String sourceName = url.toString();

        try {
            Unmarshaller unmarshaller = getDefaultUnmarshaller(mapping, sourceName);

            // unmarshall the map:
            
            try {
                map = (LWMap) unmarshaller.unmarshal(new InputSource(reader));
            } catch (org.exolab.castor.xml.MarshalException me) {
                //if (allowOldFormat && me.getMessage().endsWith("tufts.vue.Resource")) {
                //if (allowOldFormat && me.getMessage().indexOf("Unable to instantiate tufts.vue.Resource") >= 0) {
                // 2007-10-01 SMF: rev forward the special exception to check for once again in new castor version: castor-1.1.2.1-xml.jar
                if (allowOldFormat && me.getMessage().indexOf("tufts.vue.Resource can no longer be constructed") >= 0) {
                    System.err.println("ActionUtil.unmarshallMap: " + me);
                    System.err.println("Attempting specialized MapResource mapping for old format.");
                    // NOTE: delicate recursion here: won't loop as long as we pass in a non-null mapping.
                    return unmarshallMap(url, getMapping(XML_MAPPING_OLD_RESOURCES), charsetEncoding, false, savingVersion);
                } else
                    throw me;
            }
            
            reader.close();

            final File file = new File(url.getFile());
            final String fileName = file.getName();

            map.setFile(file); // VUE-713: do this always:

            if (map.getModelVersion() > LWMap.getCurrentModelVersion()) {
                VueUtil.alert(String.format("The file %s was saved in a newer version of VUE than is currently running.\n"
                                            + "\nThe data model in this map is #%d, and this version of VUE only understands up to model #%d.\n",
                                            file, map.getModelVersion(), LWMap.getCurrentModelVersion())
                              + "\nVersion of VUE that saved this file:\n        " + savingVersion
                              + "\nCurrent running version of VUE:\n        " + "VUE: built " + tufts.vue.Version.AllInfo
                                + " (public v" + VueResources.getString("vue.version") + ")"
                              + "\n"
                              + "\nThis version of VUE may not display this map properly.  Saving"
                              + "\nthis map in this version of VUE may result in a corrupted map."
                              ,
                              String.format("Version Warning: %s", fileName));

                map.setLabel(fileName + " (as available)");
                // Skip setting the file: this will force save-as if they try to save.
            } else {

// VUE-713: don't do this conditionallly
//                 // This setFile also sets the label name, so it appears as a modification in the map.
//                 // So be sure to do completeXMLRestore last, as it will reset the modification count.
//                 if (map.getModelVersion() < 1) {
//                     map.setLabel(file.getName());
//                     // force save as for old maps as they will no longer work in old stable versions of VUE (1.5 & prior)
//                     // if they're saved in this new version of VUE.
//                 } else {
//                     map.setFile(file);
//                 }
                
                if (DEBUG.DATA) map.setLabel("|" + map.getModelVersion() + "| " + map.getLabel());
            }
                

            Log.debug("unmarshalled: " + map);
            // Note that map.setFile must have been done before map.completeXMLResource is called.
            map.completeXMLRestore();
            Log.debug("restored: " + map);
        }
        catch (Exception e) {
            tufts.Util.printStackTrace(e, "Exception restoring map from [" + url + "]: " + e.getClass().getName());
            map = null;
            throw new Error("Exception restoring map from [" + url + "]", e);
        }

        
        return map;
    }

}

final class XMLObjectFactory extends org.exolab.castor.util.DefaultObjectFactory {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(XMLObjectFactory.class);        
		
    @Override
    public Object createInstance(Class type, Object[] args) throws IllegalAccessException, InstantiationException {
        //System.err.println("VOF0 ASKED FOR " + type + " args=" + args);
        Log.warn("ASKED FOR " + type + " args=" + args);
        return this.createInstance(type, null, null);
    }

    @Override
    public Object createInstance(Class type) throws IllegalAccessException, InstantiationException {
        //System.err.println("VOF1 ASKED FOR " + type);
        Log.warn("ASKED FOR " + type);
        return this.createInstance(type, null, null);
    }

    @Override
    public Object createInstance(Class _type, Class[] argTypes, Object[] args)
        throws IllegalAccessException, InstantiationException
    {
        Class type = _type;
            
//         if (_type == tufts.vue.MapResource.class || _type == tufts.vue.CabinetResource.class)
//             type = tufts.vue.URLResource.class;

//         if (_type != type) {
//             Log.info("CONVERTED " + _type + " to " + type);
//         }

        //System.err.println("VOF ASKED FOR " + type + " argTypes=" + argTypes);
        //Object o = super.createInstance(type);
        final Object o = type.newInstance();
        if (DEBUG.IO || DEBUG.XML || DEBUG.CASTOR) {
            // don't use tags (allow toString to be called) -- unmarshalling can fail
            // if there are side-effects (!!!) due to calling it -- this happens
            // with a FavoritesDataSource in any case...
            Log.debug("new " + Util.tag(o)); 
            //System.err.println("new " + Util.tag(o));
        }
        return o;
    }
}

class MapException extends IOException {
    public MapException(String s) {
        super(s);
    }
}
    
    


class PaddedCellRenderer extends DefaultListCellRenderer
{	  
	   public Component  getListCellRendererComponent(JList list,
	         Object value, // value to display
	         int index,    // cell index
	         boolean iss,  // is selected
	         boolean chf)  // cell has focus?
	   {
		   super.getListCellRendererComponent(list, 
                   value, 
                   index, 
                   iss, 
                   chf);
		   
		    setText(((VueFileFilter)value).getDescription());
		   this.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));
		   
		  return this;
	   }			        	
}
