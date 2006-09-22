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

import tufts.Util;
import tufts.vue.*;
import tufts.vue.gui.*;
import tufts.vue.NotePanel;
import tufts.vue.filter.NodeFilterEditor;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;

import edu.tufts.vue.fsm.event.SearchEvent;
import edu.tufts.vue.fsm.event.SearchListener;

/**
 * Display information about the selected Resource, or LWComponent and it's Resource.
 *
 * @version $Revision: 1.33 $ / $Date: 2006-09-22 14:37:28 $ / $Author: mike $
 */

public class InspectorPane extends JPanel
    implements VueConstants, LWSelection.Listener, ResourceSelection.Listener, SearchListener
{
    private final Image NoImage = VueResources.getImage("NoImage");

    private final boolean isMacAqua = GUI.isMacAqua();

    // Resource panes
    private final SummaryPane mSummaryPane;
    private final MetaDataPane mResourceMetaData;
    private final ResourcePreview mPreview;
    
    // Node panes
    private final NotePanel mNotePanel = new NotePanel();
    private final UserMetaData mUserMetaData = new UserMetaData();
    //private final NodeTree mNodeTree = new NodeTree();
    
    private Resource mResource; // the current resource

    public InspectorPane()
    {
        super(new BorderLayout());
        setName("Info");

        mSummaryPane = new SummaryPane();
        //mResourceMetaData = new PropertiesEditor(false);
        mResourceMetaData = new MetaDataPane();
        mResourceMetaData.setName("contentInfo");
        mPreview = new ResourcePreview();

        WidgetStack stack = new WidgetStack();

        stack.addPane("Information",            mSummaryPane,           0f);
        stack.addPane("Content Preview",        mPreview,               0.3f);
        stack.addPane("Content Info",           mResourceMetaData,      1f);
        stack.addPane("Notes",                  mNotePanel,             1f);
        stack.addPane("Keywords",               mUserMetaData,          1f);
        //stack.addPane("Nested Nodes",           mNodeTree,              1f);

        Widget.setExpanded(mUserMetaData, false);
        //Widget.setExpanded(mResourceMetaData, false);
        //Widget.setExpanded(mNodeTree, false);
        
        add(stack, BorderLayout.CENTER);

        VUE.getSelection().addListener(this);
        VUE.getResourceSelection().addListener(this);
        
        Widget.setHelpAction(mSummaryPane,VueResources.getString("dockWindow.Info.summaryPane.helpText"));;
        Widget.setHelpAction(mPreview,VueResources.getString("dockWindow.Info.previewPane.helpText"));;
        Widget.setHelpAction(mResourceMetaData,VueResources.getString("dockWindow.Info.resourcePane.helpText"));;
        Widget.setHelpAction(mNotePanel,VueResources.getString("dockWindow.Info.notesPane.helpText"));;
        Widget.setHelpAction(mUserMetaData,VueResources.getString("dockWindow.Info.userPane.helpText"));;
        
    }

    public void resourceSelectionChanged(ResourceSelection.Event e)
    {    	
        if (e.selected == null)
            return;
        if (DEBUG.RESOURCE) out("resource selected: " + e.selected);
        showNodePanes(false);
        showResourcePanes(true);
        loadResource(e.selected);
    }

    public void selectionChanged(LWSelection selection) {  
        showNodePanes(true);
        if (selection.isEmpty() || selection.size() > 1) {        	
            loadResource(null);
            this.setEnabled(false);
            this.getParent().setEnabled(false);
            showNodePanes(false);
            showResourcePanes(false);
        } else {
        	this.setEnabled(true);
        	this.getParent().setEnabled(true);
            LWComponent c = selection.first();
            if (c.hasResource()) {
                loadResource(c.getResource());
                showResourcePanes(true);
            } else {
                showResourcePanes(false);
            }
            mSummaryPane.load(c);
            mUserMetaData.load(c);
            //mNodeTree.load(c);

            //setTypeName(mNotePanel, c, "Notes");
        }
    }

    private static void setTypeName(JComponent component, LWComponent c, String suffix) {
        component.setName(c.getComponentTypeLabel() + " " + suffix);
    }

    private void loadResource(final Resource r) {
        
        if (DEBUG.RESOURCE) out("loadResource: " + r);
        
        if (r == null)
            return;

        mResource = r;
        mResourceMetaData.loadResource(r);
        mPreview.loadResource(r);
        
        /*
        long size = r.getSize();
        String ss = "";
        if (size >= 0)
            ss = VueUtil.abbrevBytes(size);
        mSizeField.setText(ss);
        */
    }
    
    private void showNodePanes(boolean visible) {
        Widget.setHidden(mSummaryPane, !visible);
        Widget.setHidden(mNotePanel, !visible);
        Widget.setHidden(mUserMetaData, !visible);
        //Widget.setHidden(mNodeTree, !visible);
    }
    private void showResourcePanes(boolean visible) {
        Widget.setHidden(mResourceMetaData, !visible);
        Widget.setHidden(mPreview, !visible);
    }
    
    public void showKeywordView()
    {
    	if (!Widget.isHidden(mSummaryPane) && !Widget.isExpanded(mSummaryPane))
    		Widget.setExpanded(mSummaryPane, true);
    	if (!Widget.isHidden(mNotePanel) && Widget.isExpanded(mNotePanel))
    		Widget.setExpanded(mNotePanel, false);
    	if (!Widget.isHidden(mResourceMetaData) && Widget.isExpanded(mResourceMetaData))
    		Widget.setExpanded(mResourceMetaData, false);
    	if (!Widget.isHidden(mPreview) && Widget.isExpanded(mPreview))
    		Widget.setExpanded(mPreview, false);
    	if (!Widget.isHidden(mUserMetaData) && !Widget.isExpanded(mUserMetaData))
    		Widget.setExpanded(mUserMetaData, true);
    	
    }
    
    public void showNotesView()
    {
    	if (!Widget.isHidden(mSummaryPane) && !Widget.isExpanded(mSummaryPane))    		
    		Widget.setExpanded(mSummaryPane, true);
    	if (!Widget.isHidden(mNotePanel) && !Widget.isExpanded(mNotePanel))
    		Widget.setExpanded(mNotePanel, true);
    	if (!Widget.isHidden(mResourceMetaData) && Widget.isExpanded(mResourceMetaData))
    		Widget.setExpanded(mResourceMetaData, false);
    	if (!Widget.isHidden(mPreview) && Widget.isExpanded(mPreview))
    		Widget.setExpanded(mPreview, false);
    	if (!Widget.isHidden(mUserMetaData) && Widget.isExpanded(mUserMetaData))
    		Widget.setExpanded(mUserMetaData, false);
    	
    }
    
    public void showInfoView()
    {
    	if (!Widget.isHidden(mSummaryPane) && !Widget.isExpanded(mSummaryPane))
    		Widget.setExpanded(mSummaryPane, true);
    	if (!Widget.isHidden(mNotePanel) && !Widget.isExpanded(mNotePanel))
    		Widget.setExpanded(mNotePanel, true);
    	if (!Widget.isHidden(mResourceMetaData) && !Widget.isExpanded(mResourceMetaData))
    		Widget.setExpanded(mResourceMetaData, true);
    	if (!Widget.isHidden(mPreview) && !Widget.isExpanded(mPreview))
    		Widget.setExpanded(mPreview, true);
    	if (!Widget.isHidden(mUserMetaData) && Widget.isExpanded(mUserMetaData))
    		Widget.setExpanded(mUserMetaData, false);
    	
    }


    private class ResourcePreview extends tufts.vue.ui.PreviewPane
    {
        ResourcePreview() {
            setName("contentPreview");
        }
        void loadResource(Resource r) {
            super.loadResource(r);
            String title = r.getTitle();
            if (title == null)
                title = r.getProperty("title");
            // TODO: resource property lookups should be case insensitive
            if (title == null)
                title = r.getProperty("Title");
            if (title == null)
                title = "Content Preview";
            Widget.setTitle(this, title);
            setToolTipText(title);
        }
    }
    private class InlineTitleResourcePreview extends tufts.vue.ui.PreviewPane
    {
        private final JLabel mTitleField;
        //private final JTextPane mTitleField;
        //private final JTextArea mTitleField;
        //private final PreviewPane mPreviewPane = new PreviewPane();
        
        InlineTitleResourcePreview() {
            //super(new BorderLayout());

            // JTextArea -- no good (no HTML)
            //mTitleField = new JTextArea();
            //mTitleField.setEditable(false);

            // JTextPane -- no good, too fuckin hairy and slow (who needs an HTML editor?)
            //mTitleField = new JTextPane();
            //StyledDocument doc = new javax.swing.text.html.HTMLDocument();
            //mTitleField.setStyledDocument(doc);
            //mTitleField.setEditable(false);

            mTitleField = new JLabel("", JLabel.CENTER);

            //-------------------------------------------------------

            GUI.apply(GUI.TitleFace, mTitleField);
            mTitleField.setAlignmentX(0.5f);
            //mTitleField.setBorder(new LineBorder(Color.red));
                
            //mTitleField.setOpaque(false);
            //mTitleField.setBorder(new EmptyBorder(0,2,5,2));
            //mTitleField.setSize(200,50);
            //mTitleField.setPreferredSize(new Dimension(200,30));
            //mTitleField.setMaximumSize(new Dimension(Short.MAX_VALUE,Short.MAX_VALUE));
            //mTitleField.setMinimumSize(new Dimension(100, 30));


            //add(mPreviewPane, BorderLayout.CENTER);
            add(mTitleField, BorderLayout.SOUTH);
        }

        void loadResource(Resource r) {
            super.loadResource(r);
            String title = r.getTitle();

            //mPreviewPane.setVisible(false);
            
            if (title == null || title.length() < 1) {
                mTitleField.setVisible(false);
                return;
            }
            
            // Always use HTML, which creates auto line-wrapping for JLabels
            title = "<HTML><center>" + title;

            
            mTitleField.setVisible(true);

            if (true) {
                mTitleField.setText(title);
            } else { 
                //remove(mTitleField);
                out("OLD            size=" + mTitleField.getSize());
                out("OLD   preferredSize=" + mTitleField.getPreferredSize());
                mTitleField.setText(title);
                out("NOLAY          size=" + mTitleField.getSize());
                out("NOLAY preferredSize=" + mTitleField.getPreferredSize());
                //mTitleField.setSize(298, mTitleField.getHeight());
                //mTitleField.setSize(298, mTitleField.getHeight());
                //mTitleField.setPreferredSize(new Dimension(298, mTitleField.getHeight()));
                //mTitleField.setSize(mTitleField.getPreferredSize());
                //mTitleField.setSize(mTitleField.getPreferredSize());
                out("SETSZ          size=" + mTitleField.getSize());
                out("SETSZ preferredSize=" + mTitleField.getPreferredSize());
                //out("SETSZ preferredSize=" + mTitleField.getPreferredSize());
                mTitleField.setPreferredSize(null);
                //add(mTitleField, BorderLayout.SOUTH);
                //mTitleField.revalidate();
                //repaint();
                //mTitleField.setVisible(true);
            }

            //mPreviewPane.loadResource(mResource);
            //mPreviewPane.setVisible(true);
            //VUE.invokeAfterAWT(this);
        }

        /*
        public void run() {
            //mTitleField.revalidate();
            //mTitleField.setVisible(true);
            VUE.invokeAfterAWT(new Runnable() { public void run() {
                mPreviewPane.loadResource(mResource);
            }});
        }
        */
    }


    /*
    public static class NodeTree extends JPanel
    {
        private final OutlineViewTree tree;
        
        public NodeTree()
        {
            super(new BorderLayout());
            setName("Nested Nodes");
            tree = new OutlineViewTree();
            
            JScrollPane mTreeScrollPane = new JScrollPane(tree);
            mTreeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(mTreeScrollPane);
        }

        public void load(LWComponent c)
        {
            // if the tree is not intiliazed, hidden, or doesn't contain the given node,
            // then it switches the model of the tree using the given node
            
            if (!tree.isInitialized() || !isVisible() || !tree.contains(c)) {
                //panelLabel.setText("Node: " + pNode.getLabel());
                if (c instanceof LWContainer)
                    tree.switchContainer((LWContainer)c);
                else if (c instanceof LWLink)
                    tree.switchContainer(null);
            }
        }
    }
    */
    
    public static class UserMetaData extends JPanel
    {
        private NodeFilterEditor userMetaDataEditor = null;
        
        public UserMetaData()
        {
            super(new BorderLayout());
            //setBorder( BorderFactory.createEmptyBorder(10,10,10,6));

            // todo in VUE to create map before adding panels or have a model that
            // has selection loaded when map is added.
            // userMetaDataEditor = new NodeFilterEditor(mNode.getNodeFilter(),true);
            // add(userMetaDataEditor);
        }

        void load(LWComponent c) {
            //setTypeName(this, c, "Keywords");
            if (DEBUG.SELECTION) System.out.println("NodeFilterPanel.updatePanel: " + c);
            if (userMetaDataEditor != null) {
                //System.out.println("USER META SET: " + c.getNodeFilter());
                userMetaDataEditor.setNodeFilter(c.getNodeFilter());
            } else {
                if (VUE.getActiveMap() != null && c.getNodeFilter() != null) {
                    // NodeFilter bombs entirely if no active map, so don't let
                    // it mess us up if there isn't one.
                    userMetaDataEditor = new NodeFilterEditor(c.getNodeFilter(), true);
                    add(userMetaDataEditor, BorderLayout.CENTER);
                    //System.out.println("USER META DATA ADDED: " + userMetaDataEditor);
                }
            }
        }
    }

    private JLabel makeLabel(String s) {
        JLabel label = new JLabel(s);
        GUI.apply(GUI.LabelFace, label);
        //label.setBorder(new EmptyBorder(0,0,0, GUI.LabelGapRight));
        return label;
    }

    // summary fields
    /*
    private final Object[] labelTextPairs = {
        "-Title",   mTitleField,
        "-Where",   mWhereField,
        "-Size",    mSizeField,
    };
    */

    
    public class SummaryPane extends tufts.Util.JPanelAA
        implements Runnable
    {
        //final JTextArea labelValue = new JTextArea();
        final VueTextPane labelValue = new VueTextPane();
        final JScrollBar labelScrollBar;
        final VueTextField contentValue = new VueTextField();
        
        SummaryPane()
        {
            super(new GridBagLayout());
            //setBorder(new EmptyBorder(4, GUI.WidgetInsets.left, 4, 0));
            setBorder(GUI.WidgetInsetBorder);
            setName("nodeSummary");
            
            labelValue.setBorder(null);
            contentValue.setEditable(false);

            JScrollPane labelScroller = new JScrollPane(labelValue,
                                                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                                                        );
            labelScroller.setMinimumSize(new Dimension(70, 32)); // todo: get 2x row height from font
            labelScrollBar = labelScroller.getVerticalScrollBar();
            
            addLabelTextPairs(new Object[] {
                    "Label", labelScroller,
                    //"Content", contentValue,
                },
                this);
            
            //setPreferredSize(new Dimension(Short.MAX_VALUE,100));
            //setMinimumSize(new Dimension(200,90));
            //setMinimumSize(new Dimension(200,63));
            //setMaximumSize(new Dimension(Short.MAX_VALUE,63));
        }

        public void run() {
            labelScrollBar.setValue(0);
            labelScrollBar.setValueIsAdjusting(false);
        }


        void load(LWComponent c) {
            setTypeName(this, c, "Information");

            labelScrollBar.setValueIsAdjusting(true);
            labelValue.attachProperty(c, LWKey.Label);
            /*
            if (c.hasResource()) {
                contentValue.setText(c.getResource().toString());
            } else {
                contentValue.setText("");
            }
            */
            
            GUI.invokeAfterAWT(this);
            
            //out("ROWS " + labelValue.getRows() + " border=" + labelValue.getBorder());
        }
    }
    
    
        

    /**
     *
     * This works somewhat analogous to a JTable, except that the renderer's are persistent.
     * We fill a GridBagLayout with all the labels and value fields we might ever need, set their
     * layout constraints just right, then set the text values as properties come in, and setting
     * all the unused label's and fields invisible.  There is a maximum number of rows that can
     * be displayed (initally 20), but this number is doubled when exceeded.
     *
     */
    //----------------------------------------------------------------------------------------
    // Utility methods
    //----------------------------------------------------------------------------------------
    
    private void addLabelTextPairs(Object[] labelTextPairs, Container gridBag) {
        JLabel[] labels = new JLabel[labelTextPairs.length / 2];
        JComponent[] values = new JComponent[labels.length];
        for (int i = 0, x = 0; x < labels.length; i += 2, x++) {
            //out("ALTP[" + x + "] label=" + labelTextPairs[i] + " value=" + GUI.name(labelTextPairs[i+1]));
            String labelText = (String) labelTextPairs[i];
            labels[x] = new JLabel(labelText + ":");
            values[x] = (JComponent) labelTextPairs[i+1];
        }
        addLabelTextRows(0, labels, values, gridBag, GUI.LabelFace, GUI.ValueFace);
    }

    private final int topPad = 2;
    private final int botPad = 2;
    private final Insets labelInsets = new Insets(topPad, 0, botPad, GUI.LabelGapRight);
    private final Insets fieldInsets = new Insets(topPad, 0, botPad, GUI.FieldGapRight);
    
    /** labels & values must be of same length */
    private void addLabelTextRows(int starty,
                                  JLabel[] labels,
                                  JComponent[] values,
                                  Container gridBag,
                                  Font labelFace,
                                  Font fieldFace)
    {
        // Note that the resulting alignment ends up being somehow FONT dependent!
        // E.g., works great with Lucida Grand (MacOSX), but with system default,
        // if the field value is a wrapping JTextPane (thus gets taller as window
        // gets narrower), the first line of text rises slightly and is no longer
        // in line with it's label.
        
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        c.weighty = 0;
        c.gridheight = 1;

        
        for (int i = 0; i < labels.length; i++) {

            //out("ALTR[" + i + "] label=" + GUI.name(labels[i]) + " value=" + GUI.name(values[i]));
            
            boolean centerLabelVertically = false;
            final JLabel label = labels[i];
            final JComponent field = values[i];
            
            if (labelFace != null)
                GUI.apply(labelFace, label);

            if (field instanceof JTextComponent) {
                if (field instanceof JTextField)
                    centerLabelVertically = true;
//                 JTextComponent textField = (JTextComponent) field;
//                 editable = textField.isEditable();
//                 if (field instanceof JTextArea) {
//                     JTextArea textArea = (JTextArea) field;
//                     c.gridheight = textArea.getRows();
//                     } else if (field instanceof JTextField)
            } else {
                if (fieldFace != null)
                    GUI.apply(fieldFace, field);
            }
            
            //-------------------------------------------------------
            // Add the field label
            //-------------------------------------------------------
            
            c.gridx = 0;
            c.gridy = starty++;
            c.insets = labelInsets;
            c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last in row
            c.fill = GridBagConstraints.NONE; // the label never grows
            if (centerLabelVertically)
                c.anchor = GridBagConstraints.EAST;
            else
                c.anchor = GridBagConstraints.NORTHEAST;
            c.weightx = 0.0;                  // do not expand
            gridBag.add(label, c);

            //-------------------------------------------------------
            // Add the field value
            //-------------------------------------------------------
            
            c.gridx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;     // last in row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            //c.anchor = GridBagConstraints.NORTH;
            c.insets = fieldInsets;
            c.weightx = 1.0; // field value expands horizontally to use all space
            gridBag.add(field, c);

        }

        // add a default vertical expander to take up extra space
        // (so the above stack isn't vertically centered if it
        // doesn't fill the space).

        c.weighty = 1;
        c.weightx = 1;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        JComponent defaultExpander = new JPanel();
        defaultExpander.setPreferredSize(new Dimension(Short.MAX_VALUE, 1));
        if (DEBUG.BOXES) {
            defaultExpander.setOpaque(true);
            defaultExpander.setBackground(Color.red);
        } else
            defaultExpander.setOpaque(false);
        gridBag.add(defaultExpander, c);
    }
    
    private void loadText(JTextComponent c, String text) {
        String hasText = c.getText();
        // This prevents flashing where fields of
        // length greater the the visible area do
        // a flash-scroll when setting the text, even
        // if it's the same as what's there.
        if (hasText != text && !hasText.equals(text))
            c.setText(text);
    }
    private void loadText(JLabel c, String text) {
        String hasText = c.getText();
        // This prevents flashing where fields of
        // length greater the the visible area do
        // a flash-scroll when setting the text, even
        // if it's the same as what's there.
        if (hasText != text && !hasText.equals(text))
            c.setText(text);
    }
    
    private void out(Object o) {
        //System.out.println("Inspector: " + (o==null?"null":o.toString()));
        VUE.Log.debug("InspectorPane: " + (o==null?"null":o.toString()));
    }
    
    public static void displayTestPane(String rsrc)
    {
        //MapResource r = new MapResource("file:///System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Home");
        if (rsrc == null)
            rsrc = "file:///VUE/src/tufts/vue/images/splash_graphic_1.0.gif";

        InspectorPane p = new InspectorPane();
        LWComponent node = new LWNode("Test Node");
        node.setNotes("I am a note.");
        System.out.println("Loading resource[" + rsrc + "]");
        MapResource r = new MapResource(rsrc);
        System.out.println("Got resource " + r);
        r.setTitle("A Very Long Long Resource Title Ya Say");
        node.setResource(r);
        for (int i = 1; i < 6; i++)
            r.setProperty("field_" + i, "value_" + i);

        DockWindow w = null;
        if (false) {
            //ToolWindow w = VUE.createToolWindow("LWCInfoPanel", p);
            JScrollPane sp = new JScrollPane(p,
                                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                             //JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                                             );
            w = GUI.createDockWindow("Inspector", sp);
        } else {
            w = GUI.createDockWindow(p);
            //w = GUI.createDockWindow("Resource Inspector", p.mSummaryPane);
            //tufts.Util.displayComponent(p);
        }

        if (w != null) {
            w.setUpperRightCorner(GUI.GScreenWidth, GUI.GInsets.top);
            w.setVisible(true);
        }
        
        VUE.getSelection().setTo(node); // setLWComponent does diddly -- need this

    }

    
    public static void main(String args[]) {

        VUE.init(args);

        // Must have at least ONE active frame for our focus manager to work
        //new Frame("An Active Frame").setVisible(true);

        String rsrc = null;
        if (args.length > 0 && args[0].charAt(0) != '-')
            rsrc = args[0];

        Resource r = new URLResource("file:///VUE/src/tufts/vue/images/splash_graphic_1.0.gif");

        if (true) {
            displayTestPane(rsrc);
        } else {
            InspectorPane ip = new InspectorPane();
            VUE.getResourceSelection().setTo(r, "main::test");
            Widget.setExpanded(ip.mResourceMetaData, true);
            GUI.createDockWindow("Test Properties", ip).setVisible(true);
        }

        
    }

	public void searchPerformed(SearchEvent evt) {
		if ((VUE.getSelection().size() > 0) && (VUE.getResourceSelection().get() == null))
			return;
		else
		{
			showNodePanes(false);
			showResourcePanes(false);
			LWSelection selection = VUE.getSelection();
			
			LWComponent c = selection.first();
			if (c != null)
			{
             if (c.hasResource()) {
                loadResource(c.getResource());
                showNodePanes(true);
                showResourcePanes(true);                
             }
             else
                showNodePanes(true);
           
			}
		}
        
        
	}

    
    
}
