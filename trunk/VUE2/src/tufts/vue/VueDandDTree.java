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


/*
 * VueDandDTree.java
 *
 * Created on September 17, 2003, 11:41 AM
 */

package tufts.vue;
import tufts.google.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;

import javax.swing.border.*;
import javax.swing.tree.*;

import java.util.Vector;
import javax.swing.event.*;
import osid.dr.*;
import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;

import java.awt.geom.Point2D;

import java.util.Iterator;

/**
 *
 * @version $Revision: 1.35 $ / $Date: 2008-01-18 20:07:29 $ / $Author: mike $
 * @author  rsaigal
 */
public class VueDandDTree extends VueDragTree implements DropTargetListener {
    
    private static Icon nleafIcon = VueResources.getImageIcon("favorites.leafIcon") ;
    private static        Icon inactiveIcon = VueResources.getImageIcon("favorites.inactiveIcon") ;
    private static        Icon activeIcon = VueResources.getImageIcon("favorites.activeIcon") ;

    private final int ACCEPTABLE_DROP_TYPES =
            DnDConstants.ACTION_COPY |
            DnDConstants.ACTION_LINK |
            DnDConstants.ACTION_MOVE;
    
    private final boolean debug = true;
    private final int FAVORITES = Resource.FAVORITES;
    private final boolean sametree = true;
    private final int newfavoritesnode = 0;
    
    
    public VueDandDTree(FavoritesNode root){
        super(root);
        this.setRowHeight(0);
        this.setEditable(false);
        this.setShowsRootHandles(true);
        this.expandRow(0);
        this.setExpandsSelectedPaths(true);
        this.getModel().addTreeModelListener(new VueTreeModelListener());
        this. getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        VueDandDTreeCellRenderer renderer = new VueDandDTreeCellRenderer(this);
        this.setCellRenderer(renderer);
        new DropTarget(this, // component
                ACCEPTABLE_DROP_TYPES, // actions
                this);
        
    }
    
    public void insertInto(MutableTreeNode newNode)
    {
        DefaultTreeModel model = (DefaultTreeModel)this.getModel();
  	    ResourceNode o2 = (ResourceNode)getModel().getRoot();
		model.insertNodeInto(newNode, (MutableTreeNode)o2, (o2.getChildCount()));
    }
    
    public void drop(DropTargetDropEvent e ) {
        java.awt.Point dropLocation = e.getLocation();
        ResourceNode rootNode;
        boolean success = false;
        Transferable transfer = e.getTransferable();
        DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
        String resourceName = null;
        // java.util.List fileList = null;
        //java.util.List resourceList = null;
        java.util.List resourceList = null;
        java.util.List fileList = null;
        String droppedText = null;
        DataFlavor foundFlavor = null;
        Object foundData = null;
        
        if (debug) System.out.println("drop: found " + dataFlavors.length +  dataFlavors.toString());
        try {
            if (transfer.isDataFlavorSupported(Resource.DataFlavor)) {
                foundFlavor = Resource.DataFlavor;
                foundData = transfer.getTransferData(foundFlavor);
                resourceList = (java.util.List)foundData;
                
            } else if (transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                foundFlavor = DataFlavor.javaFileListFlavor;
                foundData = transfer.getTransferData(foundFlavor);
                fileList = (java.util.List)foundData;
            } else if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                foundFlavor = DataFlavor.stringFlavor;
                foundData = transfer.getTransferData(DataFlavor.stringFlavor);
                droppedText = (String)foundData;
            } else {
                System.out.println("TRANSFER: found no supported dataFlavors");
            }
        } catch (UnsupportedFlavorException ex) {
            ex.printStackTrace();
            System.err.println("TRANSFER: Transfer lied about supporting " + foundFlavor);
            e.dropComplete(false);
            return;
        } catch (ClassCastException ex) {
            ex.printStackTrace();
            System.err.println("TRANSFER: Transfer data did not match declared type! flavor="
                    + foundFlavor + " data=" + foundData.getClass());
            e.dropComplete(false);
            return;
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            System.err.println("TRANSFER: data no longer available");
            e.dropComplete(false);
            return;
        }
        
        DefaultTreeModel model = (DefaultTreeModel)this.getModel();
        // New Favorites FolderNew Favorites Folder
        //if ((this.getPathForLocation(dropLocation.x, dropLocation.y) == null)){rootNode = (ResourceNode)model.getRoot();
        //System.out.println("loc"+"x"+dropLocation.x+"y"+dropLocation.y);
        //}
        //else
        
        
        if (dropLocation.x < 4) {
            rootNode = (ResourceNode)model.getRoot();
            System.out.println("loc"+"x"+dropLocation.x+"y"+dropLocation.y);
        } else if ((this.getPathForLocation(dropLocation.x, dropLocation.y) == null)){
            rootNode = (ResourceNode)model.getRoot();
            System.out.println("loc"+"x"+dropLocation.x+"y"+dropLocation.y);
        }else{
            rootNode = (ResourceNode)this.getPathForLocation(dropLocation.x, dropLocation.y).getLastPathComponent();
            System.out.println("loc1"+ dropLocation.x+dropLocation.y);
            if (rootNode == tufts.vue.VueDragTree.oldnode){//System.out.println("this is same");
                return;
            }
            boolean parentdrop  = false;
            if (rootNode.getParent() == tufts.vue.VueDragTree.oldnode){System.out.println("Cannot move a parent node into a child.. Can cause infinite loops");
            return;
            }
            
        }
        if (rootNode.getResource().getClientType() == FAVORITES){
            if (resourceList != null){
                java.util.Iterator iter = resourceList.iterator();
                while(iter.hasNext()) {
                    Resource resource = (Resource) iter.next();
                    if (DEBUG.DND) System.out.println("RESOURCE FOUND: " + resource+ " type ="+ resource.getClientType()+ " resource class:"+resource.getClass());
                    ResourceNode newNode;
                    if(resource.getClientType() == Resource.FILE) {
                    //if (resource.isLocalFile()) {
                        //newNode = CabinetNode.getCabinetNode(resource.getTitle(),new File(resource.getSpec()),rootNode,model);
                        newNode = new CabinetNode(resource,CabinetNode.LOCAL);
                        CabinetResource cr = (CabinetResource)newNode.getResource();
                        if(DEBUG.DND) System.out.println("CABINET RESOURCE: " + resource+ "Entry: "+cr.getEntry()+ "entry type:"+cr.getEntry().getClass()+" type:"+cr.getEntry());
                    } else {
                        newNode    =new  ResourceNode(resource);
                    }
                    this.setRootVisible(true);
                    model.insertNodeInto(newNode, rootNode, (rootNode.getChildCount()));
                    this.expandPath(new TreePath(rootNode.getPath()));
                    //this.expandRow(0);
                    this.setRootVisible(false);
                    
                }
            }else  if (fileList != null){
                java.util.Iterator iter = fileList.iterator();
                while(iter.hasNext()) {
                    File file = (File)iter.next();
                    System.out.println("File Drop: " +file);
                    try{
                        LocalFilingManager manager = new LocalFilingManager();   // get a filing manager
                        osid.shared.Agent agent = null;
                        LocalCabinet cab = LocalCabinet.instance(file.getAbsolutePath(),agent,null);
                        CabinetResource res = CabinetResource.create(cab);
                        CabinetEntry entry = res.getEntry();
                        CabinetNode cabNode = null;
                        if (entry instanceof RemoteCabinetEntry)
                            cabNode = new CabinetNode(res, CabinetNode.REMOTE);
                        else
                            
                            cabNode = new CabinetNode(res, CabinetNode.LOCAL);
                        this.setRootVisible(true);
                        model.insertNodeInto(cabNode, rootNode, (rootNode.getChildCount()));
                        cabNode.explore();
                    }catch (Exception ex){}
                    this.expandPath(new TreePath(rootNode.getPath()));
                    this.setRootVisible(false);
                    
                }
                
            }
            
            else  if (droppedText != null){
                
                ResourceNode newNode = new ResourceNode(Resource.getFactory().get(droppedText));
                this.setRootVisible(true);
                model.insertNodeInto(newNode, rootNode, (rootNode.getChildCount()));
                this.expandPath(new TreePath(rootNode.getPath()));
                //this.expandRow(0);
                this.setRootVisible(false);
                
                
            }
            
            if (e.isLocalTransfer()){
                tufts.vue.VUE.dropIsLocal = true;
                
                e.acceptDrop(DnDConstants.ACTION_MOVE);
                e.dropComplete(true);
                
            }
        } else{
            VueUtil.alert(null, "You can only add resources to a Favorites Folder", "Error Adding Resource to Favorites");
            
            //.dropComplete(false);
        }
        
        
    }
    private void insertSubTree(ResourceNode rootNode,ResourceNode cloneNode, DefaultTreeModel treeModel){
        int i; int childCount = rootNode.getChildCount();
        System.out.println("root" + rootNode +"childCount" + childCount);
        for (i = 0; i < childCount; i++){
            
            // ResourceNode newChildc = (ResourceNode)(((ResourceNode)(rootNode.getChildAt(i))).clone());
            ResourceNode newChild = (ResourceNode)(rootNode.getChildAt(i));
            ResourceNode newChildc = (ResourceNode)newChild.clone();
            treeModel.insertNodeInto(newChildc, cloneNode, i);
            insertSubTree(newChild,newChildc,treeModel);
            
        }
    }
    
    
    class VueTreeModelListener implements TreeModelListener {
        public void treeNodesChanged(TreeModelEvent e) {
            ResourceNode node;
            node = (ResourceNode)
            (e.getTreePath().getLastPathComponent());
            
            
        /*
         * If the event lists children, then the changed
         * node is the child of the node we've already
         * gotten.  Otherwise, the changed node and the
         * specified node are the same.
         */
            try {
                int index = e.getChildIndices()[0];
                node = (ResourceNode)
                (node.getChildAt(index));
            } catch (NullPointerException exc) {}
            
            System.out.println("The user has finished editing the node.");
            System.out.println("New value: " + node.getUserObject());
            
            MapResource resource = (MapResource)node.getResource();
            resource.setTitle(node.getUserObject().toString());
            clearSelection();
            
            
        }
        public void treeNodesInserted(TreeModelEvent e) {
        }
        public void treeNodesRemoved(TreeModelEvent e) {
        }
        public void treeStructureChanged(TreeModelEvent e) {
        }
    }

    class VueDandDTreeCellRenderer extends DefaultTreeCellRenderer
    {
        protected VueDandDTree tree;
        private boolean hasImageIcon;
        private Dimension imageIconPrefSize = new Dimension(Short.MAX_VALUE, 32+3);
        private Dimension defaultPrefSize = new Dimension(Short.MAX_VALUE, 20); // todo: common default for VueDragTree

        private Border lineBorder = new MatteBorder(1,0,0,0, tufts.vue.ui.ResourceList.DividerColor);
        private Border leftInsetBorder = lineBorder;
        //private Border leftInsetBorder = new CompoundBorder(lineBorder, new EmptyBorder(0,8,0,0));

        public VueDandDTreeCellRenderer(VueDandDTree pTree) {
            this.tree = pTree;
            setAlignmentY(0.5f);            
            tree.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseClicked(MouseEvent me){
                    if  (me.getClickCount() == 1) {
                        TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                    }
                }
                public void mouseMoved(MouseEvent me) {
                    TreePath treePath = tree.getPathForLocation(me.getX(), me.getY());
                }
            });
        }

        public Dimension getPreferredSize() {
            if (hasImageIcon)
                return imageIconPrefSize;
            else
                return defaultPrefSize;
        }
        
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded,boolean leaf,int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            final ResourceNode node = (ResourceNode) value;
            final int level = node.getLevel();

            //if (sel) setBackground(getTextSelectionColor());
            //else setBackground(Color.white);
            
            hasImageIcon = false;

            if ( !(node instanceof FileNode) && (node.getResource().getClientType() == FAVORITES)) {
                if (node.getChildCount() > 0 ) {
                    setIcon(activeIcon);
                } else {
                    setIcon(inactiveIcon);
                }
            } else if (leaf) {
                Icon icon = nleafIcon;
                final Resource r = node.getResource();
                //System.out.println("level " + node.getLevel() + " for " + r);
                if (level == 1 && r.isImage()) {
                    Icon i = r.getContentIcon(tree);
                    if (i != null) {
                        hasImageIcon = true;
                        icon = i;
                        //setIcon(icon);
                    }
                }
                setIcon(icon);
            } else {
                setIcon(activeIcon);
            }

            if (level != 1 || row == 0) {
                setBorder(null);
            } else if (hasImageIcon) {
                setBorder(lineBorder);
                //setIconTextGap(4);
            } else {
                setBorder(leftInsetBorder);
                //setIconTextGap(12);
            }
            
            return this;
        }
        
    }
    
    
    public void dragEnter(DropTargetDragEvent me) { }
    public void dragExit(DropTargetEvent e) {}
    public void dragOver(DropTargetDragEvent e) { }
    
    public void dropActionChanged(DropTargetDragEvent e) { }
    private String readTextFlavor(DataFlavor flavor, Transferable transfer) {
        java.io.Reader reader = null;
        String value = null;
        try {
            reader = flavor.getReaderForText(transfer);
            if (debug) System.out.println("\treader=" + reader);
            char buf[] = new char[512];
            int got = reader.read(buf);
            value = new String(buf, 0, got);
            if (debug) System.out.println("\t[" + value + "]");
            if (reader.read() != -1)
                System.out.println("there was more data in the reader");
        } catch (Exception e) {
            System.err.println("readTextFlavor: " + e);
        }
        return value;
    }
    public static void main(String args[]) {
        VUE.init(args);
        
        new Frame("An Active Frame").setVisible(true);
        Resource r = Resource.getFactory().get("http://www.tufts.edu/");
        VueDandDTree tree = new VueDandDTree(new FavoritesNode(r));
        tufts.Util.displayComponent(tree);
    }


	public void addResource(Resource resource) {
		// TODO Auto-generated method stub
		   ResourceNode rootNode = (ResourceNode)getModel().getRoot();
		   ResourceNode newNode = new ResourceNode(resource);
		   ((DefaultTreeModel) getModel()).insertNodeInto((MutableTreeNode)newNode,  (MutableTreeNode)rootNode, ((DefaultMutableTreeNode)rootNode).getChildCount());
		   this.expandPath(new TreePath(rootNode.getPath()));

	}
}




