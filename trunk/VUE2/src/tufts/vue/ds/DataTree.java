/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

package tufts.vue.ds;

import tufts.vue.VUE;
import tufts.vue.LWComponent;
import static tufts.vue.LWComponent.Flag;
import tufts.vue.LWNode;
import tufts.vue.LWLink;
import tufts.vue.LWMap;
import tufts.vue.Actions;
import tufts.vue.LWKey;
import tufts.vue.DrawContext;
import tufts.vue.gui.GUI;
import edu.tufts.vue.metadata.VueMetadataElement;
import tufts.Util;

import java.util.List;
import java.util.*;
import java.awt.*;
import java.awt.dnd.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

/**
 *
 * @version $Revision: 1.2 $ / $Date: 2008-10-08 01:12:06 $ / $Author: sfraize $
 * @author  Scott Fraize
 */

public class DataTree extends javax.swing.JTree
    implements DragGestureListener
               , LWComponent.Listener
               //,DragSourceListener
               //,TreeSelectionListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataTree.class);
    
    final Schema schema;

    public DataTree(Schema schema) {

        this.schema = schema;

        setCellRenderer(new DataRenderer());

        setModel(new DefaultTreeModel(buildTree(schema), false));

        java.awt.dnd.DragSource.getDefaultDragSource()
            .createDefaultDragGestureRecognizer
            (this,
             java.awt.dnd.DnDConstants.ACTION_COPY |
             java.awt.dnd.DnDConstants.ACTION_MOVE |
             java.awt.dnd.DnDConstants.ACTION_LINK,
             this);
        

        addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
                public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
                    if (e.isAddedPath() && e.getPath().getLastPathComponent() != null ) {
                        final DataNode treeNode = (DataNode) e.getPath().getLastPathComponent();
                        if (treeNode.hasStyle()) {
                            VUE.getSelection().setSource(DataTree.this);
                            VUE.getSelection().setSelectionSourceFocal(null); // prevents from ever drawing through on map
                            VUE.getSelection().setTo(treeNode.getStyle());
                        }
                        //VUE.setActive(LWComponent.class, this, node.styleNode);
                    }
                }
            });

        
        
    }

    /** build the model and return the root node */
    private TreeNode buildTree(final Schema schema)
    {
        DataNode root =
            new DataNode(null, null, "Data Set: " + schema.getName());
//             new DataNode(null, null,
//                          String.format("%s [%d %s]",
//                                        schema.getName(),
//                                        schema.getRowCount(),
//                                        "items"//isCSV ? "rows" : "items"));

        DataNode template = new TemplateNode(schema, this);

        root.add(template);
        

        for (Field field : schema.getFields()) {
            
            DataNode fieldNode = new DataNode(field, this, null);
            root.add(fieldNode);
            
//             if (field.uniqueValueCount() == schema.getRowCount()) {
//                 //Log.debug("SKIPPING " + f);
//                 continue;
//             }

            final Set values = field.getValues();

            // could add all style nodes to the schema node to be put in an internal layer for
            // persistance: either that or store them with the datasources, which
            // probably makes more sense.
            
            if (values.size() > 1) {

                final Map<String,Integer> valueCounts = field.getValueMap();
                
                //-----------------------------------------------------------------------------
                // Add the enumerated values
                //-----------------------------------------------------------------------------
                
                for (Map.Entry<String,Integer> e : valueCounts.entrySet()) {
                    //Log.debug("ADDING " + o);
                    //fieldNode.add(new DefaultMutableTreeNode(e.getKey() + "/" + e.getValue()));

                    if (e.getValue() == 1) {
                        fieldNode.add(new ValueNode(field, e.getKey(), String.format("<html><b>%s", e.getKey())));
                    } else {
                        fieldNode.add(new ValueNode(field, e.getKey(), String.format("<html><b>%s</b> (%s)", e.getKey(), e.getValue())));
                    }
                }
            }
            
        }
    
        return root;
    }

    public void dragGestureRecognized(DragGestureEvent e) {
        if (getSelectionPath() != null) {
            Log.debug("SELECTED: " + Util.tags(getSelectionPath().getLastPathComponent()));
            final DataNode treeNode = (DataNode) getSelectionPath().getLastPathComponent();
            //                          if (resource != null) 
            //                              GUI.startRecognizedDrag(e, resource, this);
                         
            //tufts.vue.gui.GUI.startRecognizedDrag(e, Resource.instance(node.value), null);

            // TODO: how are we going to persist these styles?  Most
            // natural way would be inside a hidden layer, but that's on
            // the MAP, which would mean each map would need it's schema
            // recorded with styled nodes, that can be hooked up
            // DIFFERENTLY to the data source panel.

            final LWComponent dragNode;
            final Field field = treeNode.field;

            if (treeNode.isValue()) {
                //dragNode = new LWNode(String.format(" %s: %s", field.getName(), treeNode.value));
                dragNode = makeValueNode(field, treeNode.getValue());
                //dragNode.setLabel(String.format(" %s: %s ", field.getName(), treeNode.value));
                //dragNode.setLabel(String.format(" %s ", field.getName());
            } else if (treeNode.isField()) {
                if (field.isPossibleKeyField())
                    return;
                dragNode = new LWNode(String.format("  %d unique  \n  '%s'  \n  values  ",
                                                    field.uniqueValueCount(),
                                                    field.getName()));
                dragNode.setClientData(java.awt.datatransfer.DataFlavor.stringFlavor,
                                       " ${" + field.getName() + "}");
            } else {
                assert treeNode instanceof TemplateNode;
                final Schema schema = treeNode.getSchema();
                dragNode = new LWNode(String.format("  '%s'  \n  dataset  \n  (%d items)  ",
                                                    schema.getName(),
                                                    schema.getRowCount()
                                                    ));
            }
                         
            dragNode.copyStyle(treeNode.getStyle(), ~LWKey.Label.bit);
            //dragNode.setFillColor(null);
            //dragNode.setStrokeWidth(0);
            if (!treeNode.isValue()) {
                dragNode.mFontSize.setTo(24);
                dragNode.mFontStyle.setTo(java.awt.Font.BOLD);
//                 dragNode.setClientData(LWComponent.ListFactory.class,
//                                        new NodeProducer(treeNode));
            }
            dragNode.setClientData(LWComponent.ListFactory.class,
                                   new NodeProducer(treeNode));


            //                          if (treeNode.field != null) {
            //                              dragNode.setClientData(Field.class, treeNode.field);
            //                              int i = 0;
            //                              final int max = treeNode.field.uniqueValueCount();
            //                              for (String value : treeNode.field.getValues()) {
            //                                  if (++i > 12) {
            //                                      dragNode.addChild(new LWNode("" + (max - i + 1) + " more..."));
            //                                      break;
            //                                  }
            //                                  LWNode n = new LWNode();
            //                                  n.setLabel(value);
            //                                  dragNode.addChild(n);
            //                              }
            //                          }
                         
            tufts.vue.gui.GUI.startRecognizedDrag(e, dragNode);

            //                         e.startDrag(DragSource.DefaultCopyDrop, null);
            //                          e.startDrag(DragSource.DefaultCopyDrop, // cursor
            //                                      null, //dragImage, // drag image
            //                                      new Point(offX,offY), // drag image offset
            //                                      new GUI.ResourceTransfer(resource),
            //                                      dsl);  // drag source listener
                         
                         
        }
    }

    private static String makeLabel(Field f, Object value) {
        //return String.format("%s:\n%s", f.getName(), value.toString());
        if (f.isKeyField())
            return value.toString();
        else
            return value.toString() + "  ";
        //return "  " + value.toString() + "  ";
    }

    private static LWComponent makeValueNode(Field field, String value) {
        
        LWComponent node = new LWNode(makeLabel(field, value));
        node.addMetaData(field.getName(), value);
        node.setClientData(Field.class, field);
        return node;

    }

    
    private static class NodeProducer implements LWComponent.ListFactory {

        private final DataNode treeNode;

        NodeProducer(DataNode n) {
            treeNode = n;
        }

        public java.util.List<LWComponent> produceNodes() {
            Log.debug("PRODUCING NODES FOR " + treeNode.field);
            final java.util.List<LWComponent> nodes = new ArrayList();
            final Field field = treeNode.field;
            final Schema schema = treeNode.getSchema();

            LWNode n = null;

            if (field == null || field.isPossibleKeyField()) {
                Log.debug("PRODUCING KEY FIELD NODES " + field);
                int i = 0;
                for (DataRow row : schema.getRows()) {
                    n = new LWNode();
                    n.setClientData(Schema.class, schema);
                    n.getMetadataList().add(row.entries());
                    if (field != null) {
                        final String value = row.getValue(field);
                        n.setLabel(makeLabel(field, value));
                    } else {
                        //n.setLabel(treeNode.getStyle().getLabel()); // applies initial style
                    }
                    nodes.add(n);
                    //Log.debug("setting meta-data for row " + (++i) + " [" + value + "]");
//                     for (Map.Entry<String,String> e : row.entries()) {
//                         // todo: this is slow: is updating UI components, setting cursors, etc, every time
//                         n.addMetaData(e.getKey(), e.getValue());
//                     }
                }
                Log.debug("PRODUCED META-DATA IN " + field);

            } else if (treeNode.isValue()) {
                
                nodes.add(makeValueNode(field, treeNode.getValue()));
                    
            } else {

                // is a column;
                
                for (String value : field.getValues())
                    nodes.add(makeValueNode(field, value));
            }

            
            for (LWComponent c : nodes) {
                c.setStyle(treeNode.getStyle());
            }

            Actions.MakeCircle.act(nodes);
            
            final java.util.List<LWComponent> links = new ArrayList();
            for (LWComponent c : nodes) {
                links.addAll(makeLinks(c, field));
            }
            //nodes.addAll(links);

            if (nodes.size() > 1)
                Actions.MakeCircle.act(nodes);
            
            //for (LWComponent c : nodes)c.setToNaturalSize();
            // todo: some problem editing template values: auto-size not being handled on label length shrinkage

            if (links.size() > 0)
                VUE.getActiveMap().getInternalLayer("*Data Links*").addChildren(links);

            return nodes;
        }


        List<LWLink> makeLinks(LWComponent node, Field field) {

            final LWMap map = VUE.getActiveMap(); // hack;
            //final Schema schema = field.getSchema();
            final VueMetadataElement vme;

            //if (node.hasClientData(Field.class))
            if (field != null)
                vme = node.getMetadataList().get(field.getName());
            else
                vme = null;
            //final String key = vme.getValue();

            Log.debug(Util.tags(vme));

            final List<LWLink> links = new ArrayList();

            final edu.tufts.vue.metadata.MetadataList metaData = node.getMetadataList();

            for (LWComponent c : map.getAllDescendents()) {
                if (c == node)
                    continue;
//                 if (f == null)
//                     continue;
//                 Schema s = f.getSchema();
                
                // TODO: don't want to check all meta-data: just check the FIELD meta-data for the new node
                // (against all meta-data in other nodes)

                if (vme == null) {

                    // check our schema-node against only field nodes

                    if (c.hasClientData(Schema.class))
                        continue;

                    // really, want to get the single, special field item from the the
                    // currently inspecting node, and see if the current schema node
                    // has the same piece of data (key/value pair)
                    if (metaData.intersects(c.getMetadataList()))
                        links.add(makeLink(node, c, false));
                    

                } else if (c.getMetadataList().contains(vme)) {
                    // check our field node against all schema and field nodes
                    
                    final Field f = c.getClientData(Field.class);
                    //links.add(makeLink(node, c, false));
                    links.add(makeLink(node, c, f == field));
                }
                    
            }
            
            return links;
        }
        
    }

    private static LWLink makeLink(LWComponent src, LWComponent dest, boolean sameField) {
        LWLink link = new LWLink(src, dest);
        link.setArrowState(0);
        if (sameField)
            link.mStrokeStyle.setTo(LWComponent.StrokeStyle.DASHED);
        return link;
    }

    public void LWCChanged(tufts.vue.LWCEvent e) {
        repaint();
    }


    private static String makeFieldLabel(final Field field)
    {
    
        final Set values = field.getValues();
        //Log.debug("EXPANDING " + colNode);

        //LWComponent schemaNode = new LWNode(schema.getName() + ": " + schema.getSource());
        // add all style nodes to the schema node to be put in an internal layer for
        // persistance: either that or store them with the datasources, which
        // probably makes more sense.

        String label = field.toString();
            
        if (values.size() == 0) {

            if (field.getMaxValueLength() == 0) {
                label = String.format("<html><b><font color=gray>%s", field.getName());
            } else {
                label = String.format("<html><b>%s (max size: %d bytes)",
                                      field.getName(), field.getMaxValueLength());
            }
        } else if (values.size() == 1) {
                
            label = String.format("<html><b>%s: <font color=green>%s",
                                  field.getName(), field.getValues().toArray()[0]);

        } else if (values.size() > 1) {

            final Map<String,Integer> valueCounts = field.getValueMap();
                
//             if (field.isPossibleKeyField())
//                 label = String.format("<html><i><b>%s</b> (%d)", field.getName(), field.uniqueValueCount());
//             else
                label = String.format("<html><b>%s</b> (%d)", field.getName(), field.uniqueValueCount());

        }

        return label;
    }

    
    private static LWComponent createStyleNode(final Field field, LWComponent.Listener repainter)
    {
        final LWComponent style;

        if (field.isPossibleKeyField()) {

            style = new LWNode(); // creates a rectangular node
            //style.setLabel(" ---");
            style.setFillColor(Color.red);
            style.setFont(DataFont);
        } else {
            //style = new LWNode(" ---"); // creates a round-rect node
            style = new LWNode(""); // creates a round-rect node
            style.setFillColor(Color.blue);
            style.setFont(EnumFont);
        }
        style.setFlag(Flag.INTERNAL);
        style.setFlag(Flag.DATA_STYLE); // must set before setting label, or template will atttempt to resolve
        style.setLabel("${" + field.getName() + "}");
        style.setNotes(String.format
                       ("Style node for field '%s' in data-set '%s'\n\nSource: %s\n\n%s\n\nvalues=%d; unique=%d; type=%s",
                        field.getName(),
                        field.getSchema().getName(),
                        field.getSchema().getSource(),
                        field.valuesDebug(),
                        field.valueCount(),
                        field.uniqueValueCount(),
                        field.getType()
                       ));
        style.setTextColor(Color.white);
        //style.disableProperty(LWKey.Label);
        style.addLWCListener(repainter);
        style.setFlag(Flag.STYLE); // set last to creation property sets don't attempt updates

        return style;
    }



        



//     public static final java.awt.datatransfer.DataFlavor DataFlavor =
//         tufts.vue.gui.GUI.makeDataFlavor(DataNode.class);

    private static final Font EnumFont = new Font("SansSerif", Font.BOLD, 14);
    private static final Font DataFont = new Font("SansSerif", Font.PLAIN, 12);
        

    private static class DataNode extends DefaultMutableTreeNode {

        final Field field;
        
        DataNode(Field field, LWComponent.Listener repainter, String description) {
            this.field = field;

            if (description == null) {
                if (field != null)
                    setDisplay(makeFieldLabel(field));
            } else
                setDisplay(description);

            if (field != null && field.isEnumerated() && !field.isPossibleKeyField())
                field.setStyleNode(createStyleNode(field, repainter));
        }
        
//         DataNode(String description) {
//             field = null;
//             setDisplay(description);
//         }

        protected DataNode(Field field) {
            this.field = field;
        }

        Schema getSchema() {
            return field.getSchema();
        }

        String getValue() {
            return null;
        }

        void setDisplay(String s) {
            setUserObject(s);  // sets display label
        }

        LWComponent getStyle() {
            return field == null ? null : field.getStyleNode();
        }

        boolean hasStyle() {
            //return isField();
            return field != null && field.getStyleNode() != null;
        }

        boolean isField() {
            return field != null;
            //return value == null && field != null;
        }
        boolean isValue() {
            //return value != null;
            return !isField();
        }

    }

    private static final class ValueNode extends DataNode {

        String value;

        ValueNode(Field field, String value, String label) {
            super(field);
            setDisplay(label);
            this.value = value;
        }
        
        String getValue() {
            return value;
        }
        @Override
        public boolean isField() { return false; }
        @Override
        public boolean hasStyle() { return false; }
//         @Override
//         public LWComponent getStyle() { return null; }
    }

    private static final class TemplateNode extends DataNode {

        Schema schema;

        TemplateNode(Schema schema, LWComponent.Listener repainter) {
            super(null,
                  repainter,
                  String.format("<html><b><font color=red>All Rows in '%s' (%d)", schema.getName(), schema.getRowCount()));
            this.schema = schema;
            LWComponent style = new LWNode();
            style.setFlag(Flag.INTERNAL);
            //style.setLabel("Name: [${Name}]\nRole: [${Role}]");
            String fmt = "";
            Field firstField = null;
            for (Field field : schema.getFields()) {
                if (firstField == null)
                    firstField = field;
                if (field.isPossibleKeyField()) {
                    fmt = "${" + field.getName() + "}";
                    break; // only take first key field found for now
//                     if (fmt.length() > 0)
//                         fmt += "\n";
//                     fmt += String.format("%s: ${%s}", f.getName(), f.getName());
                }
            }
            style.setFlag(Flag.DATA_STYLE);
            if (fmt.length() > 0)
                style.setLabel(fmt);
            else
                style.setLabel("${" + firstField.getName() + "}");
            style.setFont(DataFont);
            style.setTextColor(Color.white);
            style.setFillColor(Color.darkGray);
            style.setStrokeWidth(0);
            //style.disableProperty(LWKey.Notes);
            style.setNotes("Style for all " + schema.getRowCount() + " data items in " + schema.getName());
            style.setFlag(Flag.STYLE);

            schema.setStyleNode(style);
        }
        
        @Override
        Schema getSchema() {
            return schema;
        }
        @Override
        boolean isField() { return false; }
        @Override
        boolean isValue() { return false; }
        @Override
        boolean hasStyle() { return true; }
        @Override
        LWComponent getStyle() { return schema.getStyleNode(); }
    }

    private static class DataRenderer extends DefaultTreeCellRenderer {

        public Component getTreeCellRendererComponent(
                final JTree tree,
                final Object value,
                final boolean selected,
                final boolean expanded,
                final boolean leaf,
                final int row,
                final boolean hasFocus)
        {
            //Log.debug(Util.tags(value));
            final DataNode node = (DataNode) value;
            
            if (node.isField() && !leaf) {
                if (node.field.isPossibleKeyField())
                    //setForeground(Color.red);
                    setForeground(Color.black);
                else
                    setForeground(Color.blue);
            } else {
                setForeground(Color.black);
            }

            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (node.hasStyle()) {
                setIcon(FieldIconPainter.load(node.getStyle()));
//                 if (!leaf)
//                     setIcon(FieldIconPainter.load(node.getStyle()));
//                 else
//                     setIcon(EmptyIcon);
            } else {
                //if (!leaf) setIcon(EmptyIcon);
                // enumerated value
                //setIcon(null);
            }
            
            
            return this;
        }
    }

    private static final NodeIconPainter FieldIconPainter = new NodeIconPainter();

    
    private static final int IconWidth = 16;
    private static final int IconHeight = 16;
    private static final java.awt.geom.Rectangle2D IconSize
        = new java.awt.geom.Rectangle2D.Float(0,0,IconWidth,IconHeight);

    private static final Icon EmptyIcon = new GUI.EmptyIcon(IconWidth, IconHeight);
    
    private static class NodeIconPainter implements Icon {

        LWComponent node;

//         NodeIcon(LWComponent c) {
//             node = c;
//         }

        public Icon load(LWComponent c) {
            node = c;
            return this;
        }
        
        public int getIconWidth() { return IconWidth; }
        public int getIconHeight() { return IconHeight; }
        
        public void paintIcon(Component c, Graphics g, int x, int y) {
            //Log.debug("x="+x+", y="+y);
            
            ((java.awt.Graphics2D)g).setRenderingHint
                (java.awt.RenderingHints.KEY_ANTIALIASING,
                 java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
            node.drawFit(new DrawContext(g.create(), node),
                         IconSize,
                         0);
            //node.drawFit(g, x, y);
        }
    
    }
    
}
