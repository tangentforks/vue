/*
 * CabinetResource.java
 *
 * Created on January 23, 2004, 2:32 PM
 * Updated on February 03, 2004 - Added support for marshalling.
 */

package tufts.vue;
import osid.filing.*;
import java.util.*;
import java.io.*;
import java.net.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;

/**
 *  A wrapper for CabinetEntry objects which can be used as the user object in a 
 *  DefaultMutableTreeNode.  It implements the Resource interface specification.
 *
 * @author  Mark Norton
 */
public class CabinetResource extends MapResource{
    //  Filing metadata property keywords.
    public static final String MD_NAME = "md.filing.name";
    public static final String MD_TIME = "md.filing.time";
    public static final String MD_OWNER = "md.filing.owner";
    public static final String MD_READ = "md.filing.read";
    public static final String MD_WRITE = "md.filing.write";
    public static final String MD_APPEND = "md.filing.append";
    public static final String MD_LENGTH = "md.filing.length";
    public static final String MD_MIME = "md.filing.mime";
    
    private osid.filing.CabinetEntry entry = null;  //  This is not marshalled.
    
    private int type = Resource.NONE;           //  Resource type.
    private boolean selected = false;           //  Selection flag.
    private String spec = null;                 //  Object specification, usually URL.
    private String ext = null;                  //  Extension.
    private Properties properties = null;       //  Metadata.
    private String title = null;                //  Title.
    
    
    /**
     *  Create a new instance of CabinetResource.  This creator should be used when
     *  the resource is being unmarshalled.
     */
    public CabinetResource () {
        this.entry = entry;
        this.type = Resource.URL;
    }
    
    /** 
     *  Creates a new instance of CabinetResource .  This creator is used when the
     *  resource is part of a CabinetNode in a JTree.
     */
    public CabinetResource(osid.filing.CabinetEntry entry) {
        this.entry = entry;
        this.type = Resource.URL;
        
        //  Force information to be cached.
        this.getEntry();
        this.getExtension();
        //this.getProperties();
        this.getSpec();
        this.getTitle();
        
    }
    
    /**
     *  Display the content associated with this resource.  For a cabinet resource,
     *  a call is made to show a URL.
     *
     *  @author Mark Norton
     */
    public void displayContent() {
        try {
            VueUtil.openURL(getSpec());
        }
        catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     *  Return the file extension for this resource.  In general, cabinets don't have
     *  an extension, since they are directories.  In those cases, the empty string is
     *  returned.
     *
     *  @author Mark Norton
     */
    public String getExtension() {
        
        //  Check for a restored resource.
        ext = "none";
        if (this.entry == null)
            return this.ext;
        else {
            URL url = null;
            try {
                url = new URL (getSpec());      //  Get the URL of this cabinet.
            }
            catch (java.net.MalformedURLException ex) {
                ex.printStackTrace();
            }

            File file =  new File (url.getFile());  //  Extract the file portion.
            if (file.isDirectory())
                this.ext = new String ("dir");              //  Directories don't have extensions.
            else {
                String name = file.getName();       //  Get the filename with out path.
               if(name.lastIndexOf('.')> -1) 
                this.ext = name.substring (name.lastIndexOf ('.'));  //  Extract extention.
            }
            return this.ext;
        }
    }
    
    /**
     *  Return the metadata properties associated with this object.  Metadata is extracted
     *  from the various flavors of CabinetEntry and collected into a Properties object,
     *  which is returned.  Each metadata element has a property keyword defined above.
     *
     *  @author Mark Norton
     */
    public java.util.Properties getProperties() {
        Properties props = new Properties();
        
        //  Check for a restored resource.
        if (this.entry == null)
            return this.properties;
        else {
            try {
                if (this.entry instanceof RemoteByteStore) {
                    RemoteByteStore bs = (RemoteByteStore) this.entry;
                    props.setProperty(CabinetResource.MD_NAME, bs.getDisplayName());
                    props.setProperty(CabinetResource.MD_TIME, bs.getLastAccessedTime().toString());
                    props.setProperty(CabinetResource.MD_OWNER, bs.getOwner().getDisplayName());
                    if (bs.isReadable())
                        props.setProperty(CabinetResource.MD_READ, "true");
                    else
                        props.setProperty(CabinetResource.MD_READ, "false");
                    if (bs.isWritable())
                        props.setProperty(CabinetResource.MD_WRITE, "true");
                    else
                        props.setProperty(CabinetResource.MD_WRITE, "false");
                    props.setProperty(CabinetResource.MD_LENGTH, String.valueOf(bs.length()));
                    props.setProperty(CabinetResource.MD_MIME, bs.getMimeType());

                }
                else if (this.entry instanceof RemoteCabinet) {
                    RemoteCabinet cab = (RemoteCabinet) this.entry;
                    props.setProperty(CabinetResource.MD_NAME, cab.getDisplayName());
                    props.setProperty(CabinetResource.MD_TIME, cab.getLastAccessedTime().toString());
                }
                else if (this.entry instanceof LocalByteStore) {
                    LocalByteStore bs = (LocalByteStore) this.entry;
                    props.setProperty(CabinetResource.MD_NAME, bs.getDisplayName());
                    props.setProperty(CabinetResource.MD_TIME, bs.getLastAccessedTime().toString());
                    props.setProperty(CabinetResource.MD_OWNER, bs.getOwner().getDisplayName());
                    if (bs.isReadable())
                        props.setProperty(CabinetResource.MD_READ, "true");
                    else
                        props.setProperty(CabinetResource.MD_READ, "false");
                    if (bs.isWritable())
                        props.setProperty(CabinetResource.MD_WRITE, "true");
                    else
                        props.setProperty(CabinetResource.MD_WRITE, "false");
                    props.setProperty(CabinetResource.MD_LENGTH, String.valueOf(bs.length()));
                    props.setProperty(CabinetResource.MD_MIME, bs.getMimeType());
                }
                else if (this.entry instanceof LocalCabinet) {
                    LocalCabinet cab = (LocalCabinet) this.entry;
                    props.setProperty(CabinetResource.MD_NAME, cab.getDisplayName());
                    props.setProperty(CabinetResource.MD_TIME, cab.getLastAccessedTime().toString());
                }
            }
            catch (osid.filing.FilingException ex1) {
                //  If we get an exception, just return what we got.
            }
            catch (osid.shared.SharedException ex2) {
                //  If we get an exception, just return what we got.
            }
            this.properties = props;            //  Cache the metadata.
            return props;
        }
    }
    
    /**
     *  Return the resource specification.  For cabinet resources, this is URL of either
     *  a local or remote file.
     *
     *  @author Mark Norton
     */
    public String getSpec() {
        //  Check for a restored resource.
        if (this.entry == null)
            return this.spec;
        else {
            //  Check for each of the four possible cases.
            if (this.entry instanceof tufts.oki.remoteFiling.RemoteByteStore)
                this.spec = ((RemoteByteStore)this.entry).getUrl();
            if (this.entry instanceof tufts.oki.remoteFiling.RemoteCabinet)
                this.spec = ((RemoteCabinet)this.entry).getUrl();
            if (this.entry instanceof tufts.oki.localFiling.LocalByteStore)
                this.spec = ((LocalByteStore)this.entry).getUrl();
            if (this.entry instanceof tufts.oki.localFiling.LocalCabinet)
                this.spec = ((LocalCabinet)this.entry).getUrl();

            //  Shouldn't ever get here, but handle it anyways.
            return this.spec;
        }
    }
    
    /**
     *  Return the title of this cabinet resource.  The cabinet entry display name is
     *  used as the title.
     *
     *  @author Mark Norton
     */
    public String getTitle() {
        if (this.entry == null)
            return this.title;
        else {
            try {
                this.title = entry.getDisplayName();
            }
            catch (osid.filing.FilingException ex) {
                //  This can't fail.
            }
            return this.title;
        }
    }
    
    /**
     *  Return the tool tip information for this resource.  This is currently stubbed
     *  to return an empty string.
     *
     *  @author Mark Norton
     */
    public String getToolTipInformation() {
        return new String ("");
    }
    
    /**
     *  Return the resource type.
     *
     *  @author Mark Norton
     */
    public int getType() {
        return this.type;
    }
    
    /**
     *  Return the selected flag.
     *
     *  @author Mark Norton
     */
    public boolean isSelected() {
        return selected;
    }
    
    /**
     *  Set the selected flag to the value provided.
     *
     *  @author Mark Norton
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     *  Return the cabinet entry associated with this resource.
     *
     *  @author Mark Norton
     */
    public osid.filing.CabinetEntry getEntry() {
        return this.entry;
    }
    
}
