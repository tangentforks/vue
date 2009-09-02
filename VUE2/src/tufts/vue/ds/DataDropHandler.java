package tufts.vue.ds;

import tufts.Util;
import tufts.vue.DEBUG;

import tufts.vue.MetaMap;
import tufts.vue.LWComponent;
import tufts.vue.LWNode;
import tufts.vue.MapDropTarget;
import static tufts.vue.MapDropTarget.*;
import tufts.vue.ds.DataTree.DataNode;
import tufts.vue.ds.DataTree.RowNode;
import tufts.vue.ds.DataTree.AllRowsNode;

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
    

/**
 * Once a Row, Field, or Value has been selected for dragging from the DataTree,
 * this handles what happens when it's dropped on the map.  What happends depends
 * on what it's dropped on.
 *
 * @version $Revision: 1.1 $ / $Date: 2009-09-02 16:28:40 $ / $Author: sfraize $
 * @author  Scott Fraize
 */

class DataDropHandler extends MapDropTarget.DropHandler
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataDropHandler.class);

    /** the dropping DataNode -- note that these are not VUE nodes, they're nodes from the VUE DataTree JTree impl */
    private final DataNode droppingDataItem;

    @Override public String toString() {
        return droppingDataItem.toString();
    }

    DataDropHandler(DataNode n, DataTree tree) {
        droppingDataItem = n;
    }

    /** DropHandler */
    @Override public DropIndication getIndication(LWComponent target, int requestAction) {

        final int acceptAction;
        
        if (target instanceof LWNode && target.isDataNode()) {
            acceptAction = java.awt.dnd.DnDConstants.ACTION_LINK; // indicate data-action
        }
        else if (target == null) {
            // default map drop
            acceptAction = requestAction;
        }
        else {
            //return DnDConstants.ACTION_COPY; // copy-only (e.g., no resource-link creation for "regular" nodes)
            // we're attempt to drop onto a non-data node:
            return DropIndication.rejected();
        }
        
        return new DropIndication(DROP_ACCEPT_DATA, acceptAction, target);
    }
    
            
    /** DropHandler */
    @Override public boolean handleDrop(DropContext drop)
    {
        final List<LWComponent> clusteringTargets = new ArrayList(); // nodes already on the map
            
        final List<LWComponent> newNodes; // nodes created

        if (drop.hit != null && drop.hit.isDataNode()) {

            newNodes = produceRelatedNodes(droppingDataItem, drop, clusteringTargets);

            // clearing hit/hitParent this will prevent MapDropTarget from
            // adding the nodes to the target node, and will then add them
            // directly to the map.  Todo: something cleaner (allow an
            // override in the DropHandler for what to do w/parenting)
            drop.hit = drop.hitParent = null;
            
        } else {
            Log.debug("PRODUCING NODES ON THE MAP (no drop target for filtering)");
                
            newNodes = produceAllDroppedNodes(droppingDataItem);
        }

        if (newNodes == null || newNodes.size() == 0)
            return false;

        layoutNodes(drop, newNodes, clusteringTargets);

        //-------------------------------------------------------
        // TODO: handle selection manually (not in MapDropTarget)
        // so we can add the field style to the selection in case
        // what was dropped is immediately styled
        //-------------------------------------------------------

        drop.items = newNodes; // tells MapDropTarget to add these to the map

        // tell MapDropTarget what to select:
        
        // todo: either make this always handled by the drop handler or make
        // a clean API for this (MapDropTargets has standard things to do
        // regarding application focus it is going to handle after we return,
        // and we don't want to be worrying about that in DropHandler's)

        if (clusteringTargets.size() > 0) {
            drop.select = new ArrayList(newNodes);
            drop.select.addAll(clusteringTargets);
        } else {
            drop.select = newNodes;
        }

        return true;
        
    }

    private void layoutNodes
        (final DropContext drop,
         final List<LWComponent> newNodes, 
         final List<LWComponent> clusteringTargets)
    {
        //-----------------------------------------------------------------------------
        // Currently, we must set node locations before adding any links, as when
        // the link-add events happen, the viewer may adjust the canvas size
        // to include room for the new links, which will all be linking to 0,0
        // unless the nodes have had their locations set, even if the nodes
        // are about to be re-laid out via a group clustering.
        //-----------------------------------------------------------------------------

        if (DEBUG.Enabled) Log.debug("NEW-DATA-NODES: " + Util.tags(newNodes));
                
        MapDropTarget.setCenterAt(newNodes, drop.location);

        boolean didAddLinks = DataAction.addDataLinksForNodes(drop.viewer.getMap(),
                                                              newNodes,
                                                              droppingDataItem.getField());

        if (clusteringTargets.size() > 0) {
            //tufts.vue.Actions.MakeCluster.doClusterAction(clusterNode, newNodes);                
            for (LWComponent center : clusteringTargets) {
                tufts.vue.Actions.MakeCluster.doClusterAction(center, center.getLinked());
            }
        } else if (drop.isLinkAction) {
            //tufts.vue.Actions.MakeCluster.act(newNodes); // TODO: GET THIS WORKING -- needs to work w/out a center
            tufts.vue.LayoutAction.filledCircle.act(newNodes); // TODO: this goes into infinite loops sometimes!
        } else {
            // TODO: pass isLinkAction to clusterNodes and sort out there 
            clusterNodes(newNodes, didAddLinks);
        }

    }
    

    private static List<LWComponent> produceAllDroppedNodes(final DataNode treeNode)
    {
        final Field field = treeNode.getField();
        final Schema schema = treeNode.getSchema();
            
        Log.debug("PRODUCING NODES FOR FIELD: " + field);
        Log.debug("                IN SCHEMA: " + schema);
            
        final java.util.List<LWComponent> nodes;

        LWNode n = null;

        if (treeNode instanceof RowNode) {

            Log.debug("PRODUCING SINGLE ROW NODE");
            nodes = DataAction.makeSingleRowNode(schema, treeNode.getRow());
                
            //} else if (treeNode.isRowNode()) {
        } else if (treeNode instanceof AllRowsNode) {

            List<LWComponent> _nodes = null;
            Log.debug("PRODUCING ALL DATA NODES");
            try {
                _nodes = DataAction.makeRowNodes(schema);
                Log.debug("PRODUCED ALL DATA NODES; nodeCount="+_nodes.size());
            } catch (Throwable t) {
                Util.printStackTrace(t);
            }

            nodes = _nodes;
                
        } else if (treeNode.isValue()) {
                
            Log.debug("PRODUCING A SINGLE VALUE NODE");
            // is a single value from a column
            nodes = Collections.singletonList(DataAction.makeValueNode(field, treeNode.getValue()));
                    
        } else {

            Log.debug("PRODUCING ALL VALUE NODES FOR FIELD: " + field);
            nodes = new ArrayList();

            // handle all the enumerated values for a column
                
            for (String value : field.getValues()) {
                nodes.add(DataAction.makeValueNode(field, value));
            }
        }

        return nodes;
    }


    /**
     * Combine the given tree node with the drop target (e.g., search/filter) to find
     * the new nodes to create
     */
    private static List<LWComponent> produceRelatedNodes
        (final DataNode treeNode,
         final DropContext drop,
         final List<LWComponent> clusteringTargets)
    {
        final List<LWComponent> dropTargets = new ArrayList();
        final Field dragField = treeNode.getField();
                
        Schema dragSchema = null;
        boolean draggingAllRows = false;
        if (treeNode instanceof AllRowsNode) {
            dragSchema = treeNode.getSchema();
            draggingAllRows = true;
        }

        if (drop.hit.isSelected()) {
            dropTargets.addAll(drop.viewer.getSelection());
        } else {
            dropTargets.add(drop.hit);
        }

        Log.debug("DATA ACTION ON " + drop.hit
                  + "\n\tdropTargets: " + Util.tags(dropTargets)
                  + "\n\t  dragField: " + dragField
                  + "\n\t dragSchema: " + dragSchema);

        //-----------------------------------------------------------------------------
        // TODO: "merge" action for VALUE nodes.
        // For value nodes with the same value, delete one, merge all links
        // For value nodes with with DIFFERENT keys and/or values, create a COMPOUND VALUE
        // node that either has multiple values, or multiple keys and values.
        // This will complicate the hell out of the search code tho.
        //-----------------------------------------------------------------------------
                
                
        final List <LWComponent> newNodes = new ArrayList();
        for (LWComponent dropTarget : dropTargets) {
            final MetaMap dropTargetData;
                    
            if (dropTarget.isDataNode()) {
                dropTargetData = dropTarget.getRawData();
                clusteringTargets.add(dropTarget);
                if (draggingAllRows) {
                    // TODO: dropTargetData instead of dropTarget?
                    newNodes.addAll(DataAction.makeRelatedRowNodes(dragSchema, dropTarget));
                }
                else {
                    newNodes.addAll(DataAction.makeRelatedNodes(dragField, dropTarget));
                }
//                 else if (dropTarget.isDataRowNode()) {
//                     // TODO: if dropTarget is a single value node, this makes no sense
//                     newNodes.addAll(DataAction.makeRelatedValueNodes(dragField, dropTargetData));
//                 }
//                 else { // if (dropTarget.isDataValueNode())
//                     Log.debug("UNIMPLEMENTED: hierarchy use case? relate linked to of "
//                               + dropTarget + " based on " + dragField,
//                               new Throwable("HERE"));
//                     // if a value node, find all ROW nodes connected to it, and color
//                     // them based on the VALUES from the dragged Field?
//                     // Or, add all the values nodes and recluster all of of the linked
//                     // items based on that -- this is the HIERARCHY USE CASE.
//                     return false;
//                 }
            } else if (dropTarget.hasResource()) {
                // TODO: what is dragField going to be?  Can we drag from the meta-data pane?
                dropTargetData = dropTarget.getResource().getProperties();
                newNodes.addAll(DataAction.makeRelatedValueNodes(dragField, dropTargetData));
            }
        }

        return newNodes;
    }
    
        
    private boolean clusterNodes(List<LWComponent> nodes, boolean newLinksAvailable) {

        if (DEBUG.Enabled) Log.debug("clusterNodes: " + Util.tags(nodes) + "; addedLinks=" + newLinksAvailable);
            
        try {

            if (nodes.size() > 1) {
                if (droppingDataItem.isRowNode()) {
                    tufts.vue.LayoutAction.random.act(nodes);
                } else if (newLinksAvailable) {

                    //                         // TODO: Use the fast clustering code if we can --  filledCircle can
                    //                         // be VERY slow, and sometimes hangs!
                    //                         // TODO: the center nodes still need to be laid out in the big grid!
                    //                         for (LWComponent center : nodes) {
                    //                             tufts.vue.Actions.MakeCluster.doClusterAction(center, center.getLinked());
                    //                         }
                        
                    // TODO: cluster will currently fail (NPE) if no data-links exist
                    // Note: this action will re-arrange all the data-nodes on the map
                    tufts.vue.LayoutAction.cluster.act(nodes);
                        
                } else
                    tufts.vue.LayoutAction.filledCircle.act(nodes);
                return true;
            }
        } catch (Throwable t) {
            Log.error("clustering failure: " + Util.tags(nodes), t);
        }
        return false;
    }
}
        

