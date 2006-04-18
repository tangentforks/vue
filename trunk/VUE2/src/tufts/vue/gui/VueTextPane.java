
package tufts.vue.gui;

import tufts.vue.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/** 
 * Provides an editable multi-line text panel for a named LWComponent property.
 * Automatically saves the text upon focus loss if there was any change,
 * and enters an undo entry.
 *
 * @author Scott Fraize
 * @version $Revision: 1.9 $ / $Date: 2006-04-18 20:46:44 $ / $Author: sfraize $
 */

// todo: create an abstract class for handling property & undo code, and subclass this and VueTextField from it.
// or: a handler/listner that can be attached to any text field.

// todo: consume all key events
// todo: rename LWTextPane, as is specific to to LWComponent text properties

public class VueTextPane extends JTextPane
    implements LWComponent.Listener
{
    private LWComponent lwc;
    private Object propertyKey;
    /** was a key pressed since we loaded the current text? */
    private boolean keyWasPressed = false; // TODO: also need to know if cut or paste happened!
    private boolean styledText = false;
    private String undoName;
    private String loadedText;
	
    public VueTextPane(LWComponent c, Object propertyKey, String undoName)
    {
        addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) { saveText(); }
            });

        if (c != null && propertyKey != null)
            attachProperty(c, propertyKey);
        setUndoName(undoName);

        GUI.installBorder(this);

        if (propertyKey != null)
            setName(propertyKey.toString());
        else if (undoName != null)
            setName(undoName);
    }

    public VueTextPane(String undoName) {
        this(null, null, undoName);
    }
    
    public VueTextPane() {
        this(null, null, null);
    }
    
    /** We override this to do nothing, so that default focus traversal keys are left in
     * place (and so you can't use TAB in this class.  See java 1.4 JEditorPane constructor
     * where it installs JComponent.getManagingFocus{Forward,Backward}TraversalKeys().
     * This doesn't work for java 1.5 -- will have to override LookAndFeel.installProperty
     * for that.
     */
    public void setFocusTraversalKeys(int id, java.util.Set keystrokes) {
        if (DEBUG.FOCUS) System.out.println(this + " ignoring setFocusTraversalKeys " + id + " " + keystrokes);
    }

    public void XsetName(String s) {
        tufts.Util.printStackTrace("setName " + s);
        super.setName(s);
    }
    
    protected void processKeyEvent(KeyEvent e) {
        if (DEBUG.KEYS && e.getID() == KeyEvent.KEY_PRESSED) System.out.println(e);
        // if any key activity, assume it may have changed
        // (to make sure we catch cut's and paste's as well newly input characters)
        keyWasPressed = true;
        super.processKeyEvent(e);
    }

    public void attachProperty(LWComponent c, Object key) {
        if (c == null || key == null)
            throw new IllegalArgumentException("component=" + c + " propertyKey="+key + " neither can be null");
        saveText();
        if (lwc == c && propertyKey == key)
            return;
        if (lwc != null)
            lwc.removeLWCListener(this);
        lwc = c;
        propertyKey = key;
        loadPropertyValue();
        lwc.addLWCListener(this, new Object[] { propertyKey, LWKey.Deleting } );
        keyWasPressed = false;
    }

    public void detachProperty() {
        if (lwc != null) {
            lwc.removeLWCListener(this);
            lwc = null;
        }
        setText("");
    }

    /** an optional special undo name for this property */
    public void setUndoName(String name) {
        undoName = name;
    }


    // TODO: DROP OF TEXT (this is a paste, but with no keypress!)
    protected void saveText() {
        final String currentText = getText();
        if (lwc != null && (keyWasPressed || !currentText.equals(loadedText))) {
            if (DEBUG.KEYS||DEBUG.TEXT) System.out.println(this + " saveText [" + getText() + "]");
            /*
            Document doc = getDocument();
            String text = null;
            try {
                if (DEBUG.KEYS) System.out.println(this + " saveText [" + doc.getText(0, doc.getLength()) + "]");
                java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();                
                //java.io.CharArrayWriter buf = new java.io.CharArrayWriter(); // RTFEditorKit won't write 16 bit characters.
                // But it turns out it still handles unicode via self-encoding the special chars.
                getEditorKit().write(buf, doc, 0, doc.getLength());
                text = buf.toString();
                if (DEBUG.KEYS) System.out.println(this + " EDITOR KIT OUTPUT [" + text + "]");
            } catch (Exception e) {
                e.printStackTrace();
            }
            lwc.setProperty(propertyKey, text);
            */
            lwc.setProperty(propertyKey, currentText);
            loadedText = currentText;
            if (undoName != null)
                VUE.markUndo(undoName);
            else
                VUE.markUndo();
        }	
    }

    private void loadPropertyValue() {
        String text = null;
        if (lwc != null) {
            try {
                text = (String) lwc.getPropertyValue(propertyKey);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("VueTextPane only handles properties of type String");
            }
            //setEditable(true);
            setEnabled(true);
            //setFocusable(true);
        } else {
            //setEditable(false);
            setEnabled(false);
            //setFocusable(false);
        }
        if (text == null) {
            setText("");
            loadedText = "";
        } else {
            setText(text);
            loadedText = text;
        }
    }
    
    public void LWCChanged(LWCEvent e)
    {
        if (e.getComponent() == lwc) {
            if (e.getWhat() == LWKey.Deleting) {
                lwc.removeLWCListener(this);
                lwc = null;
                propertyKey = null;
            }
            loadPropertyValue();
        }
    }


    private void enableStyledText() {
        if (styledText)
            return;
        styledText = true;
        String text = getText();
        System.out.println("text[" + text + "]");
        setContentType("text/rtf");
        try {
            read(new java.io.StringReader(text), "description");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //setText(text);
    }

    private class BoldAction extends StyledEditorKit.BoldAction {
        public void actionPerformed(ActionEvent e) {
            //enableStyledText();
	    super.actionPerformed(e);
        }
    }
    

    public void addNotify() {
        super.addNotify();

        if (getContentType().equalsIgnoreCase("text/rtf")) {
        
            int COMMAND = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;

            System.out.println("ADDING STYLED TEXT KEYMAP");
            Keymap parentMap = getKeymap();
            Keymap map = JTextComponent.addKeymap("MyFontStyleMap", parentMap);
            
            // Add CTRL-B for Bold
            KeyStroke boldStroke = KeyStroke.getKeyStroke(KeyEvent.VK_B, COMMAND, false);
            map.addActionForKeyStroke(boldStroke, new StyledEditorKit.BoldAction());
            
            // Add CTRL-I for Italic
            KeyStroke italicStroke = KeyStroke.getKeyStroke(KeyEvent.VK_I, COMMAND, false);
            map.addActionForKeyStroke(italicStroke, new StyledEditorKit.ItalicAction());
            
            // Add CTRL-U for Underline
            KeyStroke underlineStroke = KeyStroke.getKeyStroke(KeyEvent.VK_U, COMMAND, false);
            map.addActionForKeyStroke(underlineStroke, new StyledEditorKit.UnderlineAction());
            
            setKeymap(map);
        }
    }

    public static void main(String args[]) {
        VUE.init(args);
        VueUtil.displayComponent(new VueTextField("some text"));
        DockWindow w = GUI.createDockWindow("VueTextPane Test");
        javax.swing.JPanel panel = new javax.swing.JPanel();
        VueTextPane tp1 = new VueTextPane();
        VueTextPane tp2 = new VueTextPane();
        VueTextPane tp3 = new VueTextPane();
        panel.add(tp1);
        panel.add(tp2);
        panel.add(tp3);
        //vtp.setEditable(true);
        w.setContent(panel);
        w.setVisible(true);
        VueUtil.displayComponent(new VueTextPane(new LWMap("Test Map"), LWKey.Notes, null));
    }
    

    public String toString()
    {
        return "VueTextPane[" + propertyKey + " " + lwc + "]";
    }

}
