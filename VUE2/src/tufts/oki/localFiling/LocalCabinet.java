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
 * Cabinet.java
 *
 * Created on September 17, 2003, 10:04 AM
 *
 *  The software contained in this file is copyright 2003 by Mark J. Norton, all rights reserved.
 */

package tufts.oki.localFiling;
import java.io.*;
import java.util.*;
import tufts.oki.shared.*;
import osid.OsidException;

/**
 *  LocalCabinet corresponds to a directory in a local filing system.  Root cabinets are
 *  treated specially in that they have a rootBase.  The rootBase is the path to the root
 *  of the filing system.  Usually this is empty or contains the drive letter, if a PC.
 *
 * @author  Mark Norton
 *
 */
public class LocalCabinet extends LocalCabinetEntry implements osid.filing.Cabinet {
    
    /* parent is inherited from Cabinet Entry.  */
    private SortedSet children = null;
    private tufts.oki.shared.Properties properties = null;
    private boolean initialized = false;    //  True if expanded to include entries.
    private boolean open = false;           //  Indicates open or closed status for UI. 
    private File dir = null;                //  The local directory being modeled.
    private String rootBase = null;         //  Set if this is a root.
    
     /**
      * Initializes the feilds interited from CabinetEntry and adds a vector of children.
      * Creates a Properties object to hold properties associated with this cabinet.
      *
      * @author Mark Norton
      *
      */
    public LocalCabinet(String displayName, osid.shared.Agent agentOwner, osid.filing.Cabinet parent){
        super (displayName, agentOwner, parent);

        children = new TreeSet(new Comparator() {
            public int compare(Object o1,Object o2) {
                if(o1 instanceof LocalCabinetEntry && o2 instanceof LocalCabinetEntry) {
                    try {
                         return ((LocalCabinetEntry) o1).getDisplayName().compareToIgnoreCase(((LocalCabinetEntry) o2).getDisplayName());
                    } catch(Exception ex) {
                        ex.printStackTrace();
                        return -1;
                    }
                } else {
                    return o1.toString().compareToIgnoreCase(o2.toString());
                }
            }
            
        });
        //FilingCabinetType type = new FilingCabinetType();
        //properties = new Properties(type);

        //  Create a File for this cabinet.  Treat roots specially.
        dir = new File (displayName);
        //System.out.println ("LocalCabinet creator - display name: " + dir.getName());
        if (parent == null) {
            rootBase = dir.getPath();
            updateDisplayName (rootBase);
        }
        else
            updateDisplayName(dir.getName());
    }
    
   /**
    *   Add the entry to the list of children.  
    *
    *   @author Mark Norton
    */
    public void add(osid.filing.CabinetEntry entry, java.lang.String name) throws osid.filing.FilingException {
        //System.out.println ("add - new display name: " + name);
        /* Update the display name in entry with name.  Does this make sense?  */
        entry.updateDisplayName (name);
        
        /*  Add the element to the Vector array.  */
        children.add(entry);
    }

   /**
    *   Add the entry to the list of children.  
    *
    *   @author Mark Norton
    */
    public void add(osid.filing.CabinetEntry entry) throws osid.filing.FilingException {  
        /*  Add the element to the Vector array.  */
        children.add(entry);
    }

    /*  
     *  Prints a list of the cabinet entries to stdout.  Largely used for debugging purposes.
     *  This method is not part of the OSID interface definition for Cabinet.
     *
     *  @author Mark Norton
     */
    public void list () throws osid.filing.FilingException {
        if (children == null) {
            System.out.println ("The Cabinet is empty.");
        }
        int len = children.size();
 //       for (int i = 0; i < len; i++) {
        Iterator i = children.iterator();
        while(i.hasNext()) {
            LocalCabinetEntry entry = (LocalCabinetEntry) i.next();
            if (entry instanceof LocalCabinet)
                System.out.println ("Cabinet " + i + ":  " + entry.getDisplayName());
            else if (entry instanceof LocalByteStore)
                System.out.println ("Byte Store " + i + ":  " + entry.getDisplayName());
        }
  //      }
            
    }
    
    /**  
     *  Create a new ByteStore, add it to this Cabinet.
     *
     *  @author Mark Norton
     *
     *  @return A new ByteStore with the name provided and this cabinet as parent.
     */
    public osid.filing.ByteStore createByteStore(String name) {
        
        //System.out.println ("createByteStore - name: " + name);

        osid.filing.ByteStore bs = null;
        try {
             bs = new LocalByteStore(name, this);
             this.add (bs);
        }
        catch (osid.OsidException ex) {
        }
        return bs;
    }
    
    /*  
     *  The oldByteStore is copied into the new ByteStore.
     *
     *  @author Mark Norton
     *
     *  @return A new ByteStore with the name provided and this cabinet as parent.
     */
    public osid.filing.ByteStore copyByteStore(String name, osid.filing.ByteStore oldByteStore) throws osid.filing.FilingException {

        osid.filing.ByteStore bs = (osid.filing.ByteStore) createByteStore (name);
        byte[] byte_store = ((LocalByteStore)oldByteStore).getBytes();
        byte[] new_store = (byte[])byte_store.clone();
        bs.write (new_store);
        return bs;
    }
    
    /**
     *  Currently unimplemented.  A lot depends on the eventual use of the filing
     *  system implemented by these classes.  Not all systems will have a restriction
     *  on available and used byes.
     *
     *  @author Mark Norton
     *
     *  @return The number of bytes available in this cabinet.
     */
    public long getAvailableBytes() throws osid.filing.FilingException {
         throw new osid.filing.FilingException (osid.filing.FilingException.UNIMPLEMENTED);
    }
        
    /**
     *  Currently unimplemented.  A lot depends on the eventual use of the filing
     *  system implemented by these classes.  Not all systems will have a restriction
     *  on available and used byes.
     *  <p>
     *  This could be implemented as the sum of of the sizes of the ByteStores in this
     *  directory, but its not clear what use that would be.
     *
     *  @author Mark Norton
     *
     *  @return The number of bytes used in this cabinet.
     */
    public long getUsedBytes() throws osid.filing.FilingException {
        throw new osid.filing.FilingException (osid.filing.FilingException.UNIMPLEMENTED);
    }
    
    /**
     *  Create a new cabinet entry with 
     *  the agentOwner of this new cabinet as this owner of this Cabinet.
     *
     *  @author Mark Norton
     *
     *  @return A new cabinet with the given displayName.
     */
    public osid.filing.Cabinet createCabinet(String displayName) throws osid.filing.FilingException {
        //System.out.println ("createCabinet - name: " + displayName);

        try {
             osid.shared.Agent agentOwner = super.getCabinetEntryAgent();
             LocalCabinet entry = new LocalCabinet(displayName, agentOwner, this);
             //entry.updateDisplayName(displayName);
            
            /*  Add the element to the Vector array.  */
            children.add(entry);
            return (osid.filing.Cabinet) entry;
         }
         catch (OsidException e1) {
             throw new osid.filing.FilingException (osid.filing.FilingException.OPERATION_FAILED);
             //throw e1;
         }
    }
    
   /**
    *   Creates an iterator which lists the entries in this cabinet. A check is made to
    *   see if this cabinet was previously opened.  If not, it is opened and initialized
    *   with the entries contained in it.
    *
    *   @author Mark Norton
    *
    *   @return Return an iterator for the entries in this cabinet.    
    */
    public osid.filing.CabinetEntryIterator entries() throws osid.filing.FilingException {
        if (!initialized) {
            //  Initialize the directory by getting all entries contained in it.
            String[] files = null;

            //System.out.println ("Open Directory: " + this.cwd.getDisplayName());
            files = dir.list();
            if (files == null)
                throw new osid.filing.FilingException (osid.filing.FilingException.NOT_A_CABINET);

            String rootBase = getRootBase();
            String path = rootBase + getFullName();

            //System.out.println ("openDirectory - path name: " + path);
            for (int i = 0; i < files.length; i++) {
                //File temp = new File (cwd.getPath(), files[i]);
                File temp = new File (rootBase+getFullName(), files[i]);

                //System.out.println ("openDirectory - new file: " + rootBase + getFullName() + files[i]);

                String absolute = null;
                if (isRootCabinet())
                    absolute = path + temp.getName();
                else
                    absolute = path + separator() + temp.getName();

                if (temp.isDirectory()) {

                    //System.out.println ("\tDir " + i + ": " + temp.getName() + "\t" + absolute);
                    //cwd.createCabinet (temp.getName());
                    createCabinet (absolute);
                }
                else if (temp.isFile()) {
                    //System.out.println ("\tFile " + i + ": " + temp.getName() + "\t" + absolute);
                    //cwd.createByteStore (temp.getName());
                    createByteStore (absolute);
                }

                //  Unknown cases are ignored.
            }

            //  Set flags.
            open = true;
            initialized = true;
        }
        
        return (osid.filing.CabinetEntryIterator) new LocalCabinetEntryIterator(children);
    }
    
    /**
     *  Get a cabinet entry given its name.
     *
     *  @author Mark Norton
     *
     *  @return The cabinet entry with the desired display name.  Throws ITEM_DOES_NOT_EXIST if name is unknown.
     */
    public osid.filing.CabinetEntry getCabinetEntryByName(String name) throws osid.filing.FilingException {
         Iterator i = children.iterator();
        while(i.hasNext()) {
//        for (int i = 0; i < children.size(); i++) {
            LocalCabinetEntry entry = (LocalCabinetEntry) i.next();
            //System.out.println ("getCabinetEntryByName - scan: " + entry.getDisplayName());
            if (name.compareTo (entry.getDisplayName()) == 0) {
                return (osid.filing.CabinetEntry) entry;
            }
        }
        /*  ITEM_DOES_NOT_EXIST is not exactly the right sentiment for not finding the entry.  */
        throw new osid.filing.FilingException (osid.filing.FilingException.ITEM_DOES_NOT_EXIST);
    }
    
    /**
     *  Get a cabinet entry given its identifier.  Throws ITEM_DOES_NOT_EXIST if Id is unknown.
     *
     *  @author Mark Norton
     *
     *  @return The cabinet entry corresponding to the identifier passed.
     */
    public osid.filing.CabinetEntry getCabinetEntryById(osid.shared.Id id) throws osid.filing.FilingException {
 Iterator i = children.iterator();
        while(i.hasNext()) {
     //   for (int i = 0; i < children.size(); i++) {
            LocalCabinetEntry entry = (LocalCabinetEntry) i.next();
            try {
                if (id.isEqual (entry.getId())) {
                    return (osid.filing.CabinetEntry) entry;
                }
            }
            catch (osid.shared.SharedException ex) {
                /*  Not exactly sure what could go wrong with an Id comparison,
                 *  but the compiler insists on catching this exception.
                 *  This will fall through to ITEM_DOES_NOT_EXIST exception throw.
                 */
            }
        }
        /*  ITEM_DOES_NOT_EXIST is not exactly the right sentiment for not finding the entry.  */
        throw new osid.filing.FilingException (osid.filing.FilingException.ITEM_DOES_NOT_EXIST);
    }
    
    /**
     *  Return the properties associated with this cabinet as a HashMap.  This is better
     *  implmented using the expanded Properties objects defined elsewhere.
     *
     *  @author Mark Norton
     *
     *  @return The property set of the Properties object associated with this cabinet as a Map.
     */
    public java.util.Map getProperties() throws osid.filing.FilingException {
        HashMap map = properties.getPropertySet();
        return (Map) map;
    }
    
    /**
     *  Get the root cabinet of this cabinet.
     *
     *  @author Mark Norton
     *
     *  @return The root cabinet of this cabinet by searching up the parent links.
     */
    public osid.filing.Cabinet getRootCabinet() {
        LocalCabinet cab = this;
        while (cab.getParent() != null)
            cab = (LocalCabinet)cab.getParent();
        return cab;
    }
    
    /**
     *  If this cabinet is a root, return rootBase.  Otherwise find the root
     *  and return rootBase.
     *
     *  @author Mark Norton
     */
    public String getRootBase() {
        if (super.getParent() == null)
            return rootBase;
        else {
            LocalCabinet root = (LocalCabinet) getRootCabinet();
            return root.getRootBase();
        }
    }
    
    /**
     *  Currently, this always returns true, since cabinets are always defined to be
     *  listable.  This could be made a property instead.
     *
     *  @author Mark Norton
     *
     *  @return True if this cabinet is listable.
     */
    public boolean isListable() throws osid.filing.FilingException {
        return true;
    }
    
    /**
     *  Currently, this always returns true, since cabinets are defined to be managable
     *  in this implementation.  It could be made a property instead.
     *
     *  @author Mark Norton
     *
     *  @return True if this cabinet is managable.
     */
    public boolean isManageable() throws osid.filing.FilingException {
        return true;
    }
    
    /**
     *  Return true if this is a root cabinet, ie., its parent is null.
     *
     *  @author Mark Norton
     *
     *  @return True if this is a root cabinet (parent == null).
     */
    public boolean isRootCabinet() throws osid.filing.FilingException {
        return (this.getParent() == null);
    }
    
    /**
     *  Remove the entry indicated from the children of this cabinet.  The entry is
     *  identified by comparing the Ids of the entry passed to the Id of the children
     *  present.  This assumes that such Ids are globally unique.
     *  <p>
     *  No error is thrown if entry is not present.
     *
     *  @author Mark Norton
     */
    public void remove(osid.filing.CabinetEntry entry) throws osid.filing.FilingException {

        osid.shared.Id entry_id = entry.getId();
         Iterator i = children.iterator();
        while(i.hasNext()) {
//        for (int i = 0; i < children.size(); i++) {
            LocalCabinetEntry ent = (LocalCabinetEntry)i.next();
            try {
                if (entry_id.isEqual (ent.getId())) {
                    children.remove (entry);
                }
            }
            catch (osid.shared.SharedException ex) {
                /*  Unlikely that isEqual() will throw a SharedException.  */
            }
        }
    }
    
    /**
     *  Check to see if the cabinet is empty.  This is largely used in remove and delete operations.
     *  Note:  This method is not part of the osid.filing interface definitions.
     *
     *  @author Mark Norton
     *
     *  @return True if this cabinet has no children of any kind.
     */
    public boolean isEmpty() {
        if (children == null)
            return true;
        if (children.size() == 0)
            return true;
        else
            return false;
    }
    
    /**
     *  Return the number of children this cabinent has.
     *  <p>
     *  This is an extension to osid.filing.Cabinet to support classes which imlement
     *  javax.swing.tree.TreeModel.
     *
     *  @author Mark Norton
     *
     *  @return the number of children in this cabinet.
     */
    public int getChildCount () {
        return children.size();
    }

    /**
     *  Check to see if this cabinet is open.
     *
     *  @author Mark Norton
     *
     *  @return true if the cabinet is open.
     */
    public boolean isOpen () {
        return this.open;
    }
    
    /**
     *  Set the open flag to the value given.
     *
     *  @author Mark Norton
     */
    public void setOpen (boolean flag) {
        this.open = flag;
    }
    
    /**
     *  Check to see if this cabinet is initialized.
     *
     *  @author Mark Norton
     *
     *  @return true if the cabinet is initialized.
     */
    public boolean isInitialized() {
        return this.initialized;
    }
    
    /**
     *  Set the initialized flag to the value given.
     *
     *  @author Mark Norton
     */
    public void setInitialized(boolean flag) {
        this.initialized = flag;
    }
    
    /**
     *  Get the file path to this cabinet entry.  The path name is built by walking
     *  the parent references all the way back to the root.
     *
     *  @author Mark Norton
     */
    public String xxgetPath () {
        return dir.getPath();
    }
    public String xxgetPath2() {
        Vector nodes = new Vector(100);
        String path = null;
        
        //  Collect the entry nodes between here and root.
        try {
            //nodes.add (getDisplayName());
            LocalCabinet ptr = this;
            while (ptr.getParent() != null) {
                nodes.add (ptr.getDisplayName());
                ptr = (LocalCabinet)ptr.getParent();
            }
            nodes.add ("/");  // add the root.
        }
        catch (osid.filing.FilingException ex1) {}
        
        //  Build the path name string.
        path = (String) nodes.elementAt(nodes.size()-1);
        for (int i = nodes.size()-2; i != -1; i--) {
            path += (String) nodes.elementAt(i) + "/";
        }
        
        return path;
    }

    /**
     *  Get the full file name for this entry, including path to local root.
     *  <p>
     *  Warning!  This name cannot be used to open local files, since it does
     *  not include rootBase.  The absolute name can be created by concatenating
     *  rootBase and getFullName().
     *
     *  @author Mark Norton
     */
    public String getFullName() {
        ArrayList nodes = new ArrayList(100);
        String path = null;
        //String sep = Character.toString(this.pathSeparatorChar());
        String sep = this.separator();
        
        //  Collect the entry nodes between here and root.
        try {
            //  Check for the root node case.
            if (this.getParent() == null) {
                return sep;
            }
            
            LocalCabinet ptr = this;
            while (ptr.getParent() != null) {
                nodes.add (0, ptr.getDisplayName());
                ptr = (LocalCabinet)ptr.getParent();
            }
            //nodes.add ("/");  // add the root.
        }
        catch (osid.filing.FilingException ex1) {}
        
        //  Build the path name string.
        path = sep + (String) nodes.get(0);
        for (int i = 1; i < nodes.size(); i++) {
            path += sep + (String) nodes.get(i);
        }
        
        return path;
    }    
    
    /**
     *  Get the File object associated with this cabinet entry.
     *
     *  @author Mark Norton
     */
    public File getFile() {
        return dir;
    }
    
    /**
     *  Return a URL string for this LocalByteStore.
     */
    public String getUrl() {
        String fn = getFile().getAbsolutePath();
        return "file://" + fn;
    }
    /**
     *  Get the string character that separates path nodes.
     */
    public String separator () {
        LocalCabinet root = null;

        root = (LocalCabinet) getRootCabinet();
        File file = root.getFile();
        return file.separator;
    }
    
    /**
     *  Get the char character that separates path nodes.
     */
    public char separatorChar () {
        LocalCabinet root = (LocalCabinet) getRootCabinet();
        File file = root.getFile();
        return file.separatorChar;
    }

    /**
     *  Rename the file corresponding to this byte store and update it's display
     *  name to the new name.
     *
     *  @author Mark Norton
     */
    public void rename (String absolute, String newName) throws osid.filing.FilingException {
        File dst = new File (absolute);
        dir.renameTo (dst);
        updateDisplayName (newName);
    }
}
