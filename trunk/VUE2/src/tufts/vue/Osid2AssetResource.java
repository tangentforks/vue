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
 * OsidAssetResource.java
 *
 * Created on March 24, 2004, 11:21 AM
 */

package tufts.vue;

/** A wrapper for an implementation of the Repository OSID.  A osid.dr.Asset which can be used as the user 
*  object in a DefaultMutableTreeNode.  It implements the Resource interface specification.
*/

public class Osid2AssetResource extends MapResource
{
    private static final org.osid.shared.Type BrowsePartType = new edu.tufts.vue.util.Type("mit.edu","partStructure","URL");
    private static final org.osid.shared.Type ThumbnailPartType = new edu.tufts.vue.util.Type("mit.edu","partStructure","thumbnail");
    private static final org.osid.shared.Type LargeImagePartType = new edu.tufts.vue.util.Type("mit.edu","partStructure","largeImage");


    private static final org.osid.shared.Type FedoraImagePartType = // TODO: temporary hack for Fedora (also: note differing authority conventions!
        new edu.tufts.vue.util.Type("edu.mit","partStructure","mediumImage");
    
    private osid.OsidOwner owner = null;
    private org.osid.OsidContext context = null;
    private org.osid.repository.Asset asset = null;
    private String icon = null;
	
    //    private osid.dr.Asset asset;
    //    private CastorFedoraObject castorFedoraObject;  // stripped version of fedora object for saving and restoring in castor will work only with this implementation of DR API.
	
    // default constructor needed for Castor
    public Osid2AssetResource() {}
	
    public String getLoadString()
    {
        return getTitle();
    }
	
    public void setLoadString() {}
	
    public Osid2AssetResource(org.osid.repository.Asset asset, org.osid.OsidContext context)
        throws org.osid.repository.RepositoryException 
    {
        super();
        try {
            this.context = context;
            this.asset = asset;
            getProperties().holdChanges();
            setAsset(asset);
        } catch (Throwable t) {
            System.out.println(t.getMessage());
        } finally {
            getProperties().releaseChanges();
        }
    }
    
    /**
       The Resoource Title maps to the Asset DisplayName.
       The Resource Spec maps to the value in an info field with a published Id.  This should be changed
       to a field with a published name and a published InfoStructure Type after the OSID changes.
    */
	
    public void setAsset(org.osid.repository.Asset asset) //throws org.osid.repository.RepositoryException 
    {
        this.asset = asset;
        try {
            setAssetImpl(asset);
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t);
        }
        //if ((getSpec() == null) || (getSpec().trim().length() == 0)) {
        //setSpec( asset.getDisplayName() );
    }

    private static String quoteF(String s) {
        if (s == null || s.length() == 0)
            return "";
        else
            return "\"" + s + "\" ";
    }
    private static String quoteL(String s) {
        if (s == null || s.length() == 0)
            return "";
        else
            return " \"" + s + "\"";
    }

    private static String out(org.osid.shared.Type t) {
        return t.getAuthority() + "/" + t.getDomain() + "/" + t.getKeyword() + quoteL(t.getDescription())
            ;
    }

    private void setAssetImpl(org.osid.repository.Asset asset)
        throws org.osid.repository.RepositoryException, org.osid.shared.SharedException
    {
        //java.util.Properties osid_registry_properties = new java.util.Properties();
			
        setType(Resource.ASSET_OKIREPOSITORY);
        String displayName = asset.getDisplayName();
        setTitle(displayName);
        setProperty("title", displayName);
        org.osid.repository.RecordIterator recordIterator = asset.getRecords();
        while (recordIterator.hasNextRecord()) {
            org.osid.repository.Record record = recordIterator.nextRecord();
            org.osid.repository.PartIterator partIterator = record.getParts();
            String recordDesc = null;
            if (DEBUG.DR) {
                recordDesc = quoteF(record.getDisplayName()) + record
                    + "\nID= " + quoteF(record.getId().getIdString()) + record.getId()
                    + "\nRecordStruct=" + quoteF(record.getRecordStructure().getDisplayName())
                    + " RSid=" + quoteF(record.getRecordStructure().getId().getIdString())
                    + " " + record.getRecordStructure();
            }
            
            int partIndex = 0; // for debug
            while (partIterator.hasNextPart()) {
                org.osid.repository.Part part = partIterator.nextPart();
                org.osid.repository.PartStructure partStructure = part.getPartStructure();
                org.osid.shared.Type partStructureType = partStructure.getType();
                java.io.Serializable value = part.getValue();

                final String description = partStructureType.getDescription();

                if (DEBUG.DR) {
                    recordDesc += "\nPART" + partIndex++ + "="
                        +  "pstype(" + out(partStructureType) +  ")   "
                        + quoteF(part.getDisplayName()) + "  " + part
                        + (DEBUG.META ? (" partStructure name/desc=" + quoteF(partStructure.getDisplayName()) + quoteF(partStructure.getDescription())) : "")
                        ;
                }
                
                // metadata discovery, allow for Type descriptions

                String key;

                if (description != null && description.trim().length() > 0) {
                    key = description;
                    if (DEBUG.DR) key += "|d";
                } else {
                    key = partStructureType.getKeyword();
                    if (DEBUG.DR) key += "|k";
                    /*
                    if (DEBUG.DR) {
                        String idName = record.getId().getIdString();
                        if (idName == null || idName.trim().length() == 0 || idName.indexOf(':') >= 0)
                            key += "|k";
                        else
                            key += "." + idName;
                    }
                    */
                }                
					
                if (key == null) {
                    VUE.Log.warn(this + " Asset Part [" + part + "] has null key.");
                    continue;
                }
                
                if (value == null) {
                    VUE.Log.warn(this + " Asset Part [" + key + "] has null value.");
                    continue;
                }

                if (value instanceof String) {
                    String s = ((String)value).trim();
                
                    // Don't add field if it's empty
                    if (s.length() <= 0)
                        continue;
                    
                    if (s.startsWith("<p>") && s.endsWith("</p>")) {
                        // Ignore empty HTML paragraphs
                        String body = s.substring(3, s.length()-4);
                        if (body.trim().length() == 0) {
                            if (DEBUG.DR)
                                value = "[empty <p></p> ignored]";
                            else
                                continue;
                        }
                    }
                }
                
                addProperty(key, value);

                // TODO: Fedora OSID impl is a bit of a mess: most every part is a URL part type,
                // and it's only by virtue of the fact that the last one
                // we process HAPPENS to be the fullView, that this even works at all!
                
                if (BrowsePartType.isEqual(partStructureType)) {
                    setURL_Browse(value.toString());
                    //setSpec(s);
                    ////setPreview(new javax.swing.JLabel(new javax.swing.ImageIcon(new java.net.URL(s))));
                    //this.icon = s;
                } else if (ThumbnailPartType.isEqual(partStructureType)) {
                    setURL_Thumbnail(value.toString());

                    /*
                    if (value instanceof String) {
                        //setPreview(new javax.swing.JLabel(new javax.swing.ImageIcon(new java.net.URL((String)ser))));
                        this.icon = (String) value;
                    } else {
                        //setPreview(new javax.swing.JLabel(new javax.swing.ImageIcon((java.awt.Image)ser)));
                        //this.icon = new javax.swing.ImageIcon((java.awt.Image)ser);
                    }
                    */
                } else if (LargeImagePartType.isEqual(partStructureType) || FedoraImagePartType.isEqual(partStructureType)) {
                    setURL_Image(value.toString());
                    // handle large image
                }
            }
            
            if (DEBUG.DR) {
                addProperty("ZRECORD", recordDesc);
            }

        }
    }
    
    public org.osid.repository.Asset getAsset() 
    {
        return this.asset;
    }    
	
    public String getImageIcon()
    {
        return this.icon;
    }	
}