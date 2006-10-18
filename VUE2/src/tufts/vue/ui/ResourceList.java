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

package tufts.vue.ui;
import tufts.vue.DEBUG;

import tufts.vue.Resource;
import tufts.vue.VueResources;
import tufts.vue.gui.GUI;
import tufts.vue.gui.WidgetStack;

import java.util.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;


/**
 * A list of Resource's with their icons & title's that is selectable, draggable & double-clickable
 * for resource actions.  Also uses a "masking" data-model that can abbreviate the results
 * until a synthetic model item at the end of this shortened list is selected, at which
 * time the rest of the items are "unmaksed" and displayed.
 *
 * @version $Revision: 1.9 $ / $Date: 2006-10-18 17:50:52 $ / $Author: sfraize $
 */
public class ResourceList extends JList
    implements DragGestureListener, tufts.vue.ResourceSelection.Listener
{
    public static final Color DividerColor = VueResources.getColor("ui.resourceList.dividerColor", 204,204,204);
    
    private static ImageIcon DragIcon = tufts.vue.VueResources.getImageIcon("favorites.leafIcon");

    private static int PreviewItems = 4;
    private static int PreviewModelSize = PreviewItems + 1;

    private static int LeftInset = 2;
    private static int IconSize = 32;
    private static int IconTextGap = new JLabel().getIconTextGap();
    private static int RowHeight = IconSize + 5;

    private DefaultListModel mDataModel;

    private boolean isMaskingModel = false; // are we using a masking model?
    private boolean isMasking = false; // if using a masking model, is it currently masking most entries?

    private final String mName;

    private static class MsgLabel extends JLabel {
        MsgLabel(String txt) {
            super(txt);
            setFont(GUI.TitleFace);
            setForeground(WidgetStack.BottomGradient);
            setPreferredSize(new Dimension(getWidth(), RowHeight / 2));

            int leftInset = LeftInset + IconSize + IconTextGap;

            setBorder(new CompoundBorder(new MatteBorder(0,0,1,0, DividerColor),
                                         new EmptyBorder(0,leftInset,0,0)));
        }
    }

    private JLabel mMoreLabel = new MsgLabel("?"); // failsafe
    private JLabel mLessLabel = new MsgLabel("Show top " + PreviewItems);
    
    /**
     * A model that can intially "mask" out all but a set of initial
     * items to preview the contents of the list, and provide a
     * special list item at the end of the preview range, that when
     * selected, unmasks the rest of the items in the model.
     */
    private class MaskingModel extends javax.swing.DefaultListModel
    {
        public int getSize() {
            return isMasking ? Math.min(PreviewModelSize, size()) : size() + 1;
        }
        
        public Object getElementAt(int index) {
            if (isMasking) {
                if (index == PreviewItems) {
                    return mMoreLabel;
                } else if (index > PreviewItems)
                    return "MASKED INDEX " + index; // should never see this
            } else if (index == size()) {
                return mLessLabel;
            }
            return super.getElementAt(index);
        }

        private void expand() {
            isMasking = false;
            //fireIntervalAdded(this, PreviewItems, size() - 1);
            fireContentsChanged(this, PreviewItems, size());
        }
        private void collapse() {
            isMasking = true;
            fireIntervalRemoved(this, PreviewItems, size());
        }
    }
    
    public ResourceList(Collection resourceBag)
    {
        this(resourceBag, null);
    }
    
    public ResourceList(Collection resourceBag, String name)
    {
        setName(name);
        mName = name;
        setFixedCellHeight(RowHeight);
        setCellRenderer(new RowRenderer());
        
        // Set up the data-model

        //final javax.swing.DefaultListModel model;
        
        if (resourceBag.size() > PreviewModelSize) {
            mDataModel = new MaskingModel();
            isMaskingModel = true;
            mMoreLabel = new MsgLabel((resourceBag.size() - PreviewItems) + " more...");
            isMasking = true;
        } else
            mDataModel = new javax.swing.DefaultListModel();

        // can easily change this to faster ArrayList v.s. vector by subclassing AbstractListModel
        // We don't need synchronized as list only in use one at a time, by the awt.
        
        Iterator i = resourceBag.iterator();
        while (i.hasNext())
            mDataModel.addElement(i.next());
        
        setModel(mDataModel);

        // Set up the selection-model
        
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setSelectionModel(selectionModel);

        selectionModel.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (DEBUG.RESOURCE || DEBUG.SELECTION) {
                        System.out.println(ResourceList.this + " valueChanged: " + e
                                           + " index=" + getSelectedIndex()
                                           + " picked=" + getPicked());
                        if (e.getValueIsAdjusting())
                            System.out.println(ResourceList.this + " isAdjusting: ignoring");
                    }

                    if (e.getValueIsAdjusting())
                        return;
                    
                    if (DEBUG.SELECTION) tufts.Util.printStackTrace("ResourceList valueChanged " + ResourceList.this);
                    if (isMaskingModel) {
                        if (isMasking && getSelectedIndex() >= PreviewItems)
                            ((MaskingModel)mDataModel).expand();
                        else if (!isMasking && getSelectedIndex() == mDataModel.size()) {

                            // For now: do nothing: force them to click on the collapse
                            // (keyboarding to it creates a user-loop when holding the
                            // down arrow, where it collpases, goes back to the top, then
                            // selects till it expands, then selects down to end and collapses
                            // again...
                            
                            //((MaskingModel)model).collapse();
                            // "selected" item doesn't exist at this point, so nothing is "picked"
                            // this will follow with a second selection event, which will
                            // set the resource selection to null, as nothing is picked by default.

                            return;
                        }
                    }
                    if (getPicked() != null)
                        tufts.vue.VUE.getResourceSelection().setTo(getPicked(), ResourceList.this);
                }
            });

        setDragEnabled(false);

        // Set up double-click handler for displaying content
        
        addMouseListener(new tufts.vue.MouseAdapter("resourceList") {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        Resource r = getPicked();
                        if (r != null)
                            r.displayContent();
                    } else if (e.getClickCount() == 1) {
                        if (isMaskingModel && getSelectedValue() == mLessLabel)
                            ((MaskingModel)mDataModel).collapse();
                    }
                }
            });

        // attempt mouseDragged tracking ourselves...
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {    
                public void mouseDragged(java.awt.event.MouseEvent me) {
                    Resource picked = getPicked();
                    if (picked != null) {
                        Image image = null;
                        Object o = getSelectedValue();
                        if (getSelectedValue() instanceof ResourceIcon)
                            image = ((ResourceIcon)o).getImage(); // todo: more generic on Resource class
                        // TODO: If Resource's were uniquely atomic via a Factory and real ID's,
                        // we could maybe make the ResourceIcon simpler, and have the Resource itself
                        // cache the thumbnail image (and we could thusly also request it here, instead of
                        // from the ResourceIcon)
                        GUI.startSystemDrag(ResourceList.this, me, image, new GUI.ResourceTransfer(picked));
                    }
                }
            });
        
        tufts.vue.VUE.getResourceSelection().addListener(this);

        // Set up the drag handler

        /*

        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(this, // Component
                                                      DnDConstants.ACTION_COPY |
                                                      DnDConstants.ACTION_MOVE |
                                                      DnDConstants.ACTION_LINK,
                                                      this); // DragGestureListener
        */
    }

    public void removeNotify() {
        //tufts.Util.printStackTrace("removeNotify " + this);
        tufts.vue.VUE.getResourceSelection().removeListener(this);
    }

    /** ResourceSelection.Listener */
    public void resourceSelectionChanged(tufts.vue.ResourceSelection.Event e) {
        if (e.source == this)
            return;
        if (getPicked() == e.selected) {
            ; // do nothing; already selected
        } else {
            // TODO: if list contains selected item, select it!
            clearSelection();
        }
            
    }

    private Resource getPicked() {
        Object o = getSelectedValue();
        if (o instanceof Resource)
            return (Resource) o;
        else if (o instanceof ResourceIcon)
            return ((ResourceIcon)o).getResource();
        else
            return null;
        //return (Resource) getSelectedValue();
    }
    
    public void dragGestureRecognized(DragGestureEvent e)
    {
        if (getSelectedIndex() != -1) {
            Resource r = getPicked();
            if (DEBUG.DND || DEBUG.SELECTION) System.out.println("ResourceList: startDrag: " + r);
            e.startDrag(DragSource.DefaultCopyDrop, // cursor
                        DragIcon.getImage(),
                        new Point(-10,-10), // drag image offset
                        new tufts.vue.gui.GUI.ResourceTransfer(r),
                        new tufts.vue.gui.GUI.DragSourceAdapter());
        }
    }

    public String toString() {
        String tag;
        if (mName == null)
            tag = "";
        else
            tag = "[" + mName + "]";

        return "ResourceList@" + Integer.toHexString(hashCode()) + tag; 
    }

    private class RowRenderer extends DefaultListCellRenderer
    {
        RowRenderer() {
            //setOpaque(false); // selection stops working!
            //setFont(ResourceList.this.getFont()); // leave default label font
            
            // Border: 1 pix gray at bottom, then LeftInset in from left
            setBorder(new CompoundBorder(new MatteBorder(0,0,1,0, DividerColor),
                                         new EmptyBorder(0,LeftInset,0,0)));
            setAlignmentY(0.5f);
        }
        
        public Component getListCellRendererComponent(
        JList list,
        Object value,            // value to display
        int index,               // cell index
        boolean isSelected,      // is the cell selected
        boolean cellHasFocus)    // the list and the cell have the focus
        {
            if (value == mMoreLabel)
                return mMoreLabel;
            else if (value == mLessLabel)
                return mLessLabel;

            ResourceIcon icon;
            Resource r;

            // The model starts as a list of Resources, but if asked to render
            // we replace it with a ResourceIcon, with painter set to this JList.
            // (We can still get the Resource later from the ResourceIcon)
            
            if (value instanceof Resource) {
                r = (Resource) value;
                icon = new ResourceIcon(r, IconSize, IconSize, list);
                mDataModel.set(index, icon); // ideally, wouldn't want to trigger a model change tho...
            } else {
                icon = (ResourceIcon) value;
                r = icon.getResource();
            }
            
            setIcon(icon);

            if (false)
                setText("<HTML>" + r.getTitle());
            else
                setText(r.getTitle());
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            //setEnabled(list.isEnabled());
            return this;
        }
    }
    
}

