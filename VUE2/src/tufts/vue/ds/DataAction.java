package tufts.vue.ds;

import tufts.Util;
import tufts.vue.LWComponent;
import tufts.vue.LWLink;
import edu.tufts.vue.metadata.VueMetadataElement;

import java.awt.Color;

import java.util.*;

/**
 * @version $Revision: 1.7 $ / $Date: 2009-02-22 19:27:00 $ / $Author: sfraize $
 * @author  Scott Fraize
 */

public class DataAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataAction.class);
    
    static List<LWLink> makeLinks(final Collection<LWComponent> linkTargets, LWComponent node, Field field)
    {
        Log.debug("makeLinks: " + field + "; " + node);

        if (field == null)
            return makeRowNodeLinks(linkTargets, node);
        
        final List<LWLink> links = Util.skipNullsArrayList();

        //final edu.tufts.vue.metadata.MetadataList metaData = node.getMetadataList();

        final String fieldName = field.getName();
        final String fieldValue = node.getDataValue(fieldName);
        //final String label = String.format("%s=%s", fieldName, fieldValue);
        //final String label = String.format("DataLink: %s='%s'", fieldName, fieldValue);

        for (LWComponent c : linkTargets) {
            if (c == node)
                continue;

            if (c.hasDataValue(fieldName, fieldValue)) {
                // if the target node c is schematic at all, it should only have
                // one piece of meta-data, and it should be an exact match already
                //boolean sameField = fieldName.equals(c.getSchematicFieldName());
                final boolean sameField = c.isDataValueNode();
                links.add(makeLink(node, c, fieldName, fieldValue, sameField ? Color.red : null));
            }
                
        }
            
        return links;
    }

    /** make links from row nodes (full data nodes) to any schematic field nodes found in the link targets,
     or between row nodes from different schema's that are considered "auto-joined" (e.g., a matching key field appears) */
    private static List<LWLink> makeRowNodeLinks(final Collection<LWComponent> linkTargets, final LWComponent rowNode)
    {
        final List<LWLink> links = Util.skipNullsArrayList();

        final Schema sourceSchema = rowNode.getDataSchema();
        final String sourceKeyField = sourceSchema.getKeyFieldName();
        final String sourceKeyValue = rowNode.getDataValue(sourceKeyField);
        
        for (LWComponent c : linkTargets) {

            if (c == rowNode) // never link to ourself
                continue;

            try {
            
                final Schema schema = c.getDataSchema();
            
                if (schema != null && sourceSchema != schema) {

                    //-----------------------------------------------------------------------------
                    // from different schemas: can do a join-based linking -- just try key field for now
                    //-----------------------------------------------------------------------------

                    if (c.hasDataValue(sourceKeyField, sourceKeyValue)) {
                        links.add(makeLink(c, rowNode, sourceKeyField, sourceKeyValue, Color.blue));
                    
                    } else {

                        // this is the semantic reverse of the above case
                    
                        final String targetKeyField = schema.getKeyFieldName();
                        final String targetKeyValue = c.getDataValue(targetKeyField);
                        if (rowNode.hasDataValue(targetKeyField, targetKeyValue)) {
                            links.add(makeLink(rowNode, c, targetKeyField, targetKeyValue, Color.blue));
                        }
                    }

                } else {

                    final String fieldName = c.getDataValueFieldName();
                
                    if (fieldName == null) // fieldName will be null if c isn't a data value node / has no schema
                        continue;
                
                    final String fieldValue = c.getDataValue(fieldName);
                
                    if (rowNode.hasDataValue(fieldName, fieldValue)) {
                        //final String label = String.format("RowLink: %s='%s'", fieldName, fieldValue);
                        //final String label = String.format("%s=%s", fieldName, fieldValue);
                        links.add(makeLink(c, rowNode, fieldName, fieldValue, null));
                    }
                }
            } catch (Throwable t) {
                Log.warn(t + "; processing target: " + c.getUniqueComponentTypeLabel());
            }
        }

        
        return links;
    }
        
//     /** make links from row nodes (full data nodes) to any schematic field nodes found in the link targets */
//     private static List<LWLink> makeRowNodeLinks(final Collection<LWComponent> linkTargets, LWComponent rowNode)
//     {
//         final List<LWLink> links = Util.skipNullsArrayList();
        
//         for (LWComponent c : linkTargets) {
//             if (c == rowNode)
//                 continue;

//             final String fieldName = c.getDataValueFieldName();

//             if (fieldName == null) {
//                 // fieldName will be null if c isn't a schematic field
//                 continue;
//             }

//             final String fieldValue = c.getDataValue(fieldName);
            
//             if (rowNode.hasDataValue(fieldName, fieldValue)) {
//                 //final String label = String.format("RowLink: %s='%s'", fieldName, fieldValue);
//                 //final String label = String.format("%s=%s", fieldName, fieldValue);
//                 links.add(makeLink(c, rowNode, fieldName, fieldValue, false));
//             }
                
//         }
        
//         return links;
//     }
    
    private static LWLink makeLink(LWComponent src,
                                   LWComponent dest,
                                   String fieldName,
                                   String fieldValue,
                                   Color specialColor)
    {
        if (src.hasLinkTo(dest)) {
            // don't create a link if there already is one of any kind
            return null;
        }
        
        LWLink link = new LWLink(src, dest);
        link.setArrowState(0);
        link.setStrokeColor(java.awt.Color.lightGray);
        if (specialColor != null) {
            link.mStrokeStyle.setTo(LWComponent.StrokeStyle.DASH3);
            link.setStrokeWidth(3);
            link.setStrokeColor(specialColor);
        }

        final String relationship = String.format("%s=%s", fieldName, fieldValue);
        link.setAsDataLink(relationship);

        return link;

    }

//     static List<LWLink> VMEmakeLinks(final Collection<LWComponent> linkTargets, LWComponent node, Field field)
//     {
//         //final Schema schema = field.getSchema();
//         final VueMetadataElement vme;

//         //if (node.hasClientData(Field.class))
//         if (field != null)
//             vme = node.getMetadataList().get(field.getName());
//         else
//             vme = null;
//         //final String key = vme.getValue();

//         //Log.debug(Util.tags(vme));

//         final List<LWLink> links = new ArrayList();

//         final edu.tufts.vue.metadata.MetadataList metaData = node.getMetadataList();

//         for (LWComponent c : linkTargets) {
//             if (c == node)
//                 continue;
//             //                 if (f == null)
//             //                     continue;
//             //                 Schema s = f.getSchema();
                
//             // TODO: don't want to check all meta-data: just check the FIELD meta-data for the new node
//             // (against all meta-data in other nodes)

//             if (vme == null) {

//                 // check our schema-node against only field nodes

//                 //                     // TODO: below needs to be persistent info, and if we try to add new data
//                 //                     // to a save file of one of our currently created maps, we'll link to everything.
//                 //                     if (c.hasClientData(Schema.class))
//                 //                         continue;

//                 if (c.isSchematicFieldNode())
//                     continue;

//                 // really, want to get the single, special field item from the the
//                 // currently inspecting node, and see if the current schema node
//                 // has the same piece of data (key/value pair)
//                 if (metaData.intersects(c.getMetadataList()))
//                     links.add(makeLink(node, c, null, false));
                    

//             } else if (c.getMetadataList().contains(vme)) {
//                 // check our field node against all schema and field nodes
                    
//                 final Field f = c.getClientData(Field.class);
//                 //links.add(makeLink(node, c, false));
//                 links.add(makeLink(node, c, null, f == field));
//             }
                    
//         }
            
//         return links;
//     }
    
}




