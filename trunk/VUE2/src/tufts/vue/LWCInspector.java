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

package tufts.vue;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.*;


class LWCInspector extends javax.swing.JPanel
    implements VueConstants,
               LWSelection.Listener,
               LWComponent.Listener,
               ActionListener
{
    private JLabel idField = new JLabel();
    private JLabel locationField = new JLabel();
    private JLabel sizeField = new JLabel();
    private JLabel labelHex = new JLabel();
    private JTextField labelField = new JTextField(15);
    private JTextField widthField = new JTextField();
    private JTextField heightField = new JTextField();
    private JTextField xField = new JTextField();
    private JTextField yField = new JTextField();
    private JTextField transLocField = new JTextField();
    private JTextField bitsField = new JTextField();
    private JTextField zoomField = new JTextField();
    private JTextField categoryField = new JTextField();
    private JTextField resourceField = new JTextField();
    private JTextField notesField = new JTextField();
    //private JPanel extraPanel = new JPanel();
    private JTextArea log = new JTextArea() {
            int rows = 0;
            public void append(String s) {
                super.append(rows++ + " " + s + "\n");
            }
        };
    
    //private JTextArea notesField = new JTextArea(1, 20);

    private JPanel fieldPane = new JPanel();
    private JPanel resourceMetadataPanel = new JPanel();
    private JPanel metadataPane = new JPanel();

    private LinkedList<LWCEvent> events = new LinkedList();

    //String[] labels = { "ID", "<html><font color=red>Label</font></html>", "Category", "Resource", "Notes" };
    private Object[] labelTextPairs = {
        "-ID",      idField,
        "-Location",locationField,
        "-Size",    sizeField,
        "-TransLoc",    transLocField,
        "-HideBits",    bitsField,
        "Label",    labelField,
        "Width",    widthField,
        "Height",    heightField,
        "X",         xField,
        "Y",         yField,
        "Scale",    zoomField,
        "Resource", resourceField,
        //"Category", categoryField,
        //"-Notes",    notesField,
    };

    private java.util.Map<JTextField,LWComponent.Key> fieldKeys = new java.util.HashMap();

    private JCheckBox lockBtn = new JCheckBox("Lock");
    
    public LWCInspector()
    {
        //extraPanel.setLayout(new BorderLayout());
        //extraPanel.setSize(200,100);
        //extraPanel.add(new JLabel("foo"));

        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        fieldPane.setLayout(gridBag);

        /*
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        //gridbag.setConstraints(actionLabel, c);
        //textControlsPane.add(actionLabel);
        */

        if (!(notesField instanceof JTextField))
            notesField.setBorder(LineBorder.createGrayLineBorder());

        java.util.List pairsAndKeys = new java.util.ArrayList();

        pairsAndKeys.addAll(Arrays.asList(labelTextPairs));
        pairsAndKeys.addAll(LWComponent.Key.AllKeys);
        
        addLabelTextRows(pairsAndKeys, gridBag, fieldPane);
        //addLabelTextRows(labelTextPairs, gridBag, fieldPane);
        // settting metadata
        //setUpMetadataPane();

        setLayout(new BorderLayout());
        add(new JScrollPane(log), BorderLayout.NORTH);
        log.setFont(new Font("Lucida Sans Typewriter", Font.BOLD, 9));
        log.setRows(15);
        log.setEditable(false);
        add(fieldPane, BorderLayout.CENTER);
        add(lockBtn, BorderLayout.SOUTH);
        //add(metadataPane,BorderLayout.SOUTH);

        VUE.ModelSelection.addListener(this);
    }

    private void setUpMetadataPane() {
        BoxLayout layout = new BoxLayout(metadataPane,BoxLayout.Y_AXIS);
        metadataPane.setLayout(layout);
        metadataPane.add(resourceMetadataPanel);
    }

    private static final String KEY_KEY = "LWKEY";
    
    //private void addLabelTextRows(Object[] labelTextPairs,
    private void addLabelTextRows(java.util.List pairsAndKeys,
                                  GridBagLayout gridbag,
                                  Container container)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        int num = labelTextPairs.length;

        //for (int i = 0; i < num; i += 2) {
        Iterator i = pairsAndKeys.iterator();
        while (i.hasNext()) {
            Object item = i.next();
                
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;                       //reset to default
            c.anchor = GridBagConstraints.EAST;

            
            String txt;
            final JComponent field;

            if (item instanceof String) {
                txt = (String) item;
                field = (JComponent) i.next();
            } else {
                LWComponent.Key key = (LWComponent.Key) item;
                txt = key.name;
                final JTextField textField = new JTextField();
                fieldKeys.put(textField, key);
                field = textField;
            }
            
            //String txt = (String) labelTextPairs[i];
            //JComponent field = (JComponent) labelTextPairs[i+1];
            
            boolean readOnly = false;
            if (txt.startsWith("-")) {
                txt = txt.substring(1);
                readOnly = true;
            } 
            txt += ": ";

            JLabel label = new JLabel(txt);
            label.setFont(VueConstants.SmallFont);
            gridbag.setConstraints(label, c);
            container.add(label);

            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
          
            c.weightx = 1.0;

            field.setFont(VueConstants.FixedSmallFont);
            if (field instanceof JTextField)
                ((JTextField)field).addActionListener(this);
            gridbag.setConstraints(field, c);
            container.add(field);

            if (readOnly) {
                field.setBorder(new EmptyBorder(1,1,1,1));
                if (field instanceof JTextField) {
                    JTextField tf = (JTextField) field;
                    tf.setEditable(false);
                    tf.setFocusable(false);
                }
                if (VueUtil.isMacPlatform())
                    field.setBackground(SystemColor.control);
            }
        }
        /*
          JComponent field = new JLabel("Metadata");
          c.gridwidth = GridBagConstraints.RELATIVE;     //end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(field, c);
            container.add(field);
        */
    }
    

    /*
    String s =  key + " " + source;
        
        if (component != null && component != source)
            s += " c:" + component;
        //basic information.. if more information wants to be stringfied, need to code this part
        else if (components != null)
            s += " l:" + components;
        //s += " ArrayList";
              
        return s;
    */
    
    public void LWCChanged(LWCEvent e)
    {
        if (!isShowing())
            return;

        String extra = "";

        if (e.component != null && e.component != e.source)
            extra += " from:" + e.component;
        if (e.getComponents() != null)
            extra += " list:" + e.getComponents();

        log.append(String.format("%-21s %s" + extra, e.key, e.source));
        //System.out.println(this + " " + e);
        if (this.lwc != e.getSource())
            return;
        if (e.key == LWKey.Deleting) {
            this.lwc = null;
            //loadItem(null);
            setAllEnabled(false);
        } else if (e.getSource() != this) {
            loadItem(this.lwc);
            //log.setText(null);
        } //else
    }
    
    public void selectionChanged(LWSelection selection)
    {
        if (!lockBtn.isSelected())
            setSelection(selection);
    }

    private void loadText(JTextComponent c, String text)
    {
        String hasText = c.getText();
        // This prevents flashing where fields of
        // length greater the the visible area do
        // a flash-scroll when setting the text, even
        // if it's the same as what's there.
        if (hasText != text && !hasText.equals(text))
            c.setText(text);
    }

    private LWComponent lwc; // temporary
    public void setSelection(LWSelection selection)
    {
        //System.err.println("Inspector setSelection: " + sl);

        if (selection.size() == 1) {
            log.append("LOADED: " + selection.first() + "\n");
            loadItem(selection.first());
        } else {//  (!selection.isEmpty())
            log.append("LOADED: " + selection + "\n");
            loadSelection(selection);
        }
    }

    private void setAllEnabled(boolean tv)
    {
        int pairs = labelTextPairs.length;
        for (int i = 0; i < pairs; i += 2) {
            JComponent field = (JComponent) labelTextPairs[i+1];
            field.setEnabled(tv);
        }
    }

    private void disable(JTextComponent tc)
    {
        tc.setText("");
        tc.setEnabled(false);
    }
    private void disable(JLabel l)
    {
        l.setText("");
        l.setEnabled(false);
    }

    private void loadSelection(LWSelection selection)
    {
        if (selection.isEmpty()) {
            loadItem(VUE.getActiveMap());
            return;
        }
        setAllEnabled(true);

        String id = "";
        if (selection.allOfSameType())
            id += selection.first().getClass().getName();
        else
            id += "LWComponent";
        id += "[" + selection.size() + "]";
        
        LWComponent c = selection.first();
        // grab first in selection for moment
        idField.setText(id);
        labelField.setBackground(Color.white);
        loadText(labelField, "");
        loadText(categoryField, "");
        
        disable(notesField);
        disable(resourceField);
        disable(locationField);
        disable(sizeField);
    }

    private void loadLabel(LWComponent lwc) {
        Color c = lwc.getRenderFillColor();
        if (c == null || c.getAlpha() != 255)
            c = Color.gray;
        labelField.setBackground(c);
//         if (c.isTranslucent() || c.getRenderFillColor() == null)
//             labelField.setBackground(Color.gray);
//         else
//             labelField.setBackground(c.getRenderFillColor());
        loadText(labelField, lwc.getLabel());
    }

    
    private void loadItem(LWComponent lwc)
    {
        if (this.lwc != lwc) {
            if (this.lwc != null)
                this.lwc.removeLWCListener(this);
            this.lwc = lwc;
            if (this.lwc != null) {
                this.lwc.addLWCListener(this);
                setAllEnabled(true);
            } else
                setAllEnabled(false);
        }

        //System.out.println(this + " loadItem " + lwc);
        LWComponent c = this.lwc;
        if (c == null)
            return;

        setAllEnabled(true);
        //System.out.println(this + " loading " + c);

        if (c.getResource() != null)
            loadText(resourceField, c.getResource().toString());
        else
            loadText(resourceField, "");

        String id = c.getID();
        if (c.getParent() == null)
            id += " [PARENT IS NULL]";
        else {
            id += ", #" + c.getParent().getLayer(c);
            id += " in <" + c.getParent().toName() + ">";
            id += " links:" + c.getLinks().size();
            if (c instanceof LWContainer && c.hasChildren())
                id += " children:" + ((LWContainer)c).getChildList().size();
        }
        
        idField.setText(id);
        loadLabel(c);
        loadText(notesField, c.getNotes());
        
        locationField.setText("x: " + c.getX() + "   y: " + c.getY());
        String sizeText = String.format("%.0fx%.0f", c.getWidth(), c.getHeight());
        if (c.getScale() != 1f) {
            sizeText += "  z=" + c.getScale();
            sizeText += "  absZ=" + c.getMapScale();
        }
        if (!c.isAutoSized())
            sizeText += " userSize";
        sizeField.setText(sizeText);
        transLocField.setText(c.getLocalTransform().toString());
        bitsField.setText(c.getDescriptionOfSetBits());
        widthField.setText(String.format("%5.1f map(%5.1f)", c.getWidth(), c.getMapWidth()));
        heightField.setText(String.format("%5.1f map(%5.1f)", c.getHeight(), c.getMapHeight()));
        xField.setText(String.format("%5.1f map(%5.1f)", c.getX(), c.getMapX()));
        yField.setText(String.format("%5.1f map(%5.1f)", c.getY(), c.getMapY()));
        zoomField.setText(""+c.getScale());

        for (Map.Entry<JTextField,LWComponent.Key> e : fieldKeys.entrySet()) {
            final JTextField field = e.getKey();
            final LWComponent.Key key = e.getValue();
            if (c.supportsProperty(key))
                field.setText(key.getStringValue(c));
            else
                field.setText("<unsupported for " + c.getClass().getName() + ">");
        }

        //loading the metadata if it exists
        /*
        if(c.getResource() != null && c.getResource().getProperties() != null) {
            metadataPane.remove(resourceMetadataPanel);
            if(c.getResource().getType() == Resource.ASSET_FEDORA)
                resourceMetadataPanel = new PropertiesEditor(c.getResource().getProperties(), false);
            else
                resourceMetadataPanel = new PropertiesEditor(c.getResource().getProperties(), true);
            metadataPane.add(resourceMetadataPanel);
        }
        */
        /*
        if (false&&c.labelBox != null) {
            //extraPanel.add(p);
            if (lastp != null) {
            //System.out.println("REMOVING " + lastp);
                VUE.toolPanel.remove(lastp);
            }
            JPanel np = new JPanel();
            //System.out.println("ADDING " + p);
            VUE.toolPanel.add(np, BorderLayout.CENTER);
            System.out.println("ADDING MTP TO VUE for " + c);
            np.add(c.labelBox);
            lastp = np;
        }
        */
    }

    LWSelection dummySelection = (LWSelection) new LWSelection().clone(); // clone so won't set selection bits
    private LWSelection getSelection() {
        if (VUE.getSelection().isEmpty()) {
            dummySelection.setTo(this.lwc);
            return dummySelection;
        } else
            return VUE.getSelection();
    }

    private void setWidths(String text)
        throws NumberFormatException
    {
        float w = Float.parseFloat(text);
        for (LWComponent c : getSelection())
            c.setSize(w, c.getHeight());
        //c.setAbsoluteSize(w, c.getAbsoluteHeight());
    }
    private void setHeights(String text)
        throws NumberFormatException
    {
        float h = Float.parseFloat(text);
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setSize(c.getWidth(), h);
            //c.setAbsoluteSize(c.getAbsoluteWidth(), h);
        }
    }
    private void setXs(String text)
        throws NumberFormatException
    {
        float v = Float.parseFloat(text);
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setLocation(v, c.getY());
        }
    }
    private void setYs(String text)
        throws NumberFormatException
    {
        float v = Float.parseFloat(text);
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setLocation(c.getX(), v);
        }
    }
    private void setScales(String text)
        throws NumberFormatException
    {
        float s = Float.parseFloat(text);
        Iterator i = getSelection().iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.setScale(s);
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        //tufts.macosx.Screen.dumpMainMenu();
        if (this.lwc == null)
            return;
        final String text = e.getActionCommand(); // text is in the action command??
        final JComponent src = (JTextComponent) e.getSource();
        final LWComponent c = this.lwc;
        //System.out.println("Inspector " + e);
        LWComponent.Key key;
        try {
            boolean set = true;
            if (src == labelField)          c.setLabel(text);
            //else if (src == categoryField)  c.setCategory(text);
            //else if (src == notesField)     c.setNotes(text);
            else if (src == widthField)         setWidths(text);
            else if (src == heightField)        setHeights(text);
            else if (src == zoomField)          setScales(text);
            else if (src == resourceField)      c.setResource(text);
            else if (src == xField)             setXs(text);
            else if (src == yField)             setYs(text);
            //            else if (src == strokeField) {
            //                float w = Float.parseFloat(text);
            //                c.setStrokeWidth(w);
            //            }
            //else if ((key = (LWComponent.Key) src.getClientProperty(KEY_KEY)) != null) {
            else if ((key = fieldKeys.get(src)) != null) {
                key.setStringValue(c, text);
            } else
                set = false;
            if (set)
                VUE.getUndoManager().mark();
            else
                return;
        } catch (Exception ex) {
            System.err.println(ex);
            System.err.println("LWCInspector: error setting property value ["+text+"] on " + src);
        }
        
        transferFocus(); // this isn't going to next field
        
        //todo: getNextFocusableComponent().requestFocus();
        // Try below with next-focus action for enter?
        //   component.getInputMap().put(aKeyStroke, aCommand);
        //   component.getActionMap().put(aCommmand, anAction);
        // Or: just have something else request focus.
        // Could even track to the next component ourself,
        // tho it seems rediculous to manage ourselves given
        // there's a BOODLE of awt code for this -- so
        // why isn't this dead easy???

    }

    public String toString()
    {
        return "LWCInspector@" + Integer.toHexString(hashCode());
    }


}
