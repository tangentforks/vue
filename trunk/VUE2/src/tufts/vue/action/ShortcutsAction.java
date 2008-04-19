package tufts.vue.action;

import tufts.Util;
import tufts.vue.VUE;
import tufts.vue.DEBUG;
import tufts.vue.VueTool;
import tufts.vue.VueAction;
import tufts.vue.Actions;
import tufts.vue.VueConstants;
import tufts.vue.VueToolbarController;
import tufts.vue.gui.GUI;
import tufts.vue.gui.DockWindow;

import java.awt.Event;
import java.awt.event.*;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.JScrollPane;

/**
 * Produce a shortcuts window.
 *
 * @version $Revision: 1.3 $ / $Date: 2008-04-19 23:53:57 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class ShortcutsAction extends tufts.vue.VueAction
{
    private static DockWindow window;

    public ShortcutsAction() {
        super("Keyboard Shortcuts");
    }
    
    @Override
    public boolean isUserEnabled() { return true; }
        
    private boolean wasDebug;
    private JComponent content;

    /** display the shortcuts DockWindow (create it if needed) */
    public void act() {
        if (window == null)
            window = GUI.createDockWindow(VUE.getName() + " Short-Cut Keys");

        if (content == null || (wasDebug != DEBUG.Enabled)) {
            wasDebug = DEBUG.Enabled;
            content = buildShortcutsComponent();
            window.setContent(content);
        }
        window.pack(); // fit to widest line
        window.setVisible(true);
        window.toFront();
    }

    private static String keyCodeChar(int keyCode) {
        return keyCodeChar(keyCode, false);
    }
        
    private static String keyCodeChar(int keyCode, boolean lowerCase) {
            
        if (lowerCase && keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
            return String.valueOf((char)keyCode).toLowerCase();
        }
            
        if (keyCode == KeyEvent.VK_OPEN_BRACKET)
            return "[";
        else if (keyCode == KeyEvent.VK_CLOSE_BRACKET)
            return "]";
        else if (keyCode == 0)
            return "";
        else if (Util.isMacLeopard()) {
            if (keyCode == KeyEvent.VK_SPACE) // override the little "u" glyph
                return "Space";
            else if (keyCode == KeyEvent.VK_BACK_QUOTE) // override the hard to see '`'
                return "Back Quote";
        }

        return KeyEvent.getKeyText(keyCode);
    }

    static StringBuffer html;

    private static int BOLD = 1;
    private static int ITAL = 2;
    private static int RIGHT = 4;
    private static int CENTER = 8;
    private static int SPAN2 = 16;
    private static int SPAN3 = 32;
    private static int NO_EAST_GAP = 64;
    private static int NO_WEST_GAP = 128;
        
    private static void add(int bits, Object o, String... attr) {

        html.append("<td");

        if ((bits & SPAN2) != 0)
            html.append(" colspan=2");
        else if ((bits & SPAN3) != 0)
            html.append(" colspan=3");
            
        if ((bits & CENTER) != 0) // CENTER takes priority over RIGHT
            html.append(" align=center");
        else if ((bits & RIGHT) != 0)
            html.append(" align=right");

        if (attr != null) {
            for (String s : attr) {
                html.append(' ');
                html.append(s);
            }
        }
        html.append('>');
        
        if ((bits & NO_WEST_GAP) == 0)
            html.append("&nbsp;");

        if ((bits & BOLD) != 0) html.append("<b>");
        if ((bits & ITAL) != 0) html.append("<i>");
                
        html.append(o == null ? (DEBUG.Enabled?"null":"") : o.toString());
            
        // if ((bits & BOLD) != 0) html.append("</b>"); // is implied
        // if ((bits & ITAL) != 0) html.append("</i>"); // is implied
            
        if ((bits & NO_EAST_GAP) == 0)
            html.append("&nbsp;");

        //html.append("</td>"); // is implied
    }
    private static void add(Object o) {
        add(0, o);
    }

    private static void addGap() {
        add(" &nbsp;&nbsp; ");
        //add(" &nbsp; &nbsp; ");
        //add("x&nbsp;x&nbsp;x");
    }

    private static void addRow(int row) {
        if (row % 2 == 0) {
            if (Util.isMacPlatform())
                html.append("\n<tr bgcolor=#DDDDFF>");
            else
                html.append("\n<tr bgcolor=#FFFFFF>");
        } else {
            html.append('\n');
            html.append("<tr>");
        }
    }


    private static void addTable(String... attr) {
        int border = 0;
        if (DEBUG.Enabled)
            border = 1;

        html.append('\n');
        html.append("<table border=" + border + " cellpadding=2");

        if (attr != null) {
            for (String s : attr) {
                html.append(' ');
                html.append(s);
            }
        }
        html.append('>');
        html.append('\n');
        
    }
        
        

    private static final int SHIFT = Event.SHIFT_MASK + InputEvent.SHIFT_DOWN_MASK;
    private static final int CTRL = Event.CTRL_MASK + InputEvent.CTRL_DOWN_MASK;
    private static final int ALT = Event.ALT_MASK + InputEvent.ALT_DOWN_MASK;

    private static boolean hasOnlyShift(int mods)   { return (mods & SHIFT) == (mods | SHIFT); }
    private static boolean hasOnlyCtrl(int mods)    { return (mods & CTRL) == (mods | CTRL); }
    private static boolean hasOnlyAlt(int mods)     { return (mods & ALT) == (mods | ALT); }
        
    private static boolean hasOnlyOne(int mods) {
        return hasOnlyShift(mods) || hasOnlyAlt(mods) || hasOnlyCtrl(mods);
    }

    private static final String TitleColor = "#AAAAAA";
            

        
    static JComponent buildShortcutsComponent()
    {
        if (html == null) {
            if (DEBUG.Enabled)
                html = new StringBuffer(65536);
            else
                html = new StringBuffer(8192);
        }
        html.setLength(0);
        html.append("<html>");
            
        if (Util.isMacLeopard())
            addTable();
        else
            addTable("width=100%"); // fill to wider width of actions below
            
        int row = 0;
            
        if (DEBUG.Enabled) {
                
            //=============================================================================
            // DEBUG TOOLS Title/Header Line
            //=============================================================================
                
            html.append("<tr bgcolor=#00FFFF>");
            add(BOLD+ITAL, "TOOL ID");
            add(BOLD+ITAL, "ShortCut");
            add(BOLD+ITAL, "DownKey");
            add(BOLD+ITAL, "Name");
            add(BOLD+ITAL, VueTool.class);
            html.append("</tr>");
                
        } else {

            //=============================================================================
            // Production TOOLS Title/Header Line
            //=============================================================================

            html.append("<tr bgcolor=" + TitleColor + ">");
            add(BOLD+ITAL+CENTER, "Quick-Key");
            add(BOLD+ITAL+CENTER, "Key");
            add(BOLD+ITAL, "Tool");
            html.append("</tr>");
                
        }

        for (VueTool t : VueTool.getTools()) {

            if (t.getShortcutKey() == 0)
                continue;

            final char downKey = (char) t.getActiveWhileDownKeyCode();

            addRow(row++);
                
            if (DEBUG.Enabled) {
                    
                //-------------------------------------------------------
                // DEBUG TOOLS 
                //-------------------------------------------------------

                add(t.getID());
                add(BOLD+CENTER, t.getShortcutKey());
                add(BOLD+CENTER, keyCodeChar(downKey));
                //add(BOLD+CENTER, KeyStroke.getKeyStroke((char)downKey));
                add(BOLD, t.getToolName());
                add(t.getClass().getName());
                
            } else {

                //=======================================================
                // Production TOOLS 
                //=======================================================

                if (downKey == 0)
                    add("");
                else
                    add(BOLD+CENTER, keyCodeChar(downKey, true));
                
                add(BOLD+CENTER, t.getShortcutKey());
                add(BOLD, t.getToolName());
            }
        }

        html.append("</table><br>");
        //if (Util.isMacLeopard()) html.append("<center>");

        if (Util.isMacLeopard())
            addTable("width=100%"); // fill to wider width of tools above
        else
            addTable();
        
        if (DEBUG.Enabled) {

            //-------------------------------------------------------
            // DEBUG ACTION Title/Header Line
            //-------------------------------------------------------
                
            //                 html.append("</table><p>");
            //                 addTable(0);

            html.append("<tr bgcolor=#00FFFF>");
            add(BOLD+ITAL, "row");
            add(BOLD+ITAL, "mod bits");
            add(BOLD+ITAL, "mod text");
            if (Util.isMacLeopard()) add(BOLD, "<font size=-3>Leopard<br>&nbsp;Glyphs");
            add(BOLD+ITAL+CENTER, "Key");
            add(BOLD+ITAL, "ACTION NAME");
            add(BOLD+ITAL, KeyStroke.class);
            add(BOLD+ITAL, VueAction.class);
            html.append("</tr>");
        } else {

            //=======================================================
            // Production ACTION Title/Header Line
            //=======================================================
                
            html.append("<tr bgcolor=" + TitleColor + ">");

            if (Util.isMacLeopard())
                add(BOLD+ITAL+SPAN3+RIGHT, "Shortcut");
            else 
                add(BOLD+ITAL+SPAN2, "Shortcut Key");
            
            add(BOLD+ITAL, "Action");
            if (Util.isMacLeopard())
                addGap();
            html.append("</tr>");
                
        }

        // get action short-cuts

        row = 0;
        for (VueAction a : getAllActions()) {
                
            KeyStroke k = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
            if (k == null && !(DEBUG.Enabled && DEBUG.WORK))
                continue;
                
            String modNames = "";

            if (k != null) {
                modNames = KeyEvent.getKeyModifiersText(k.getModifiers());
                //if (modNames != null && modNames.length() > 0)
                //modNames += " ";
            }

            addRow(row++);
                    
            final int mods = k == null ? 0 : k.getModifiers();
            int goRight = hasOnlyOne(mods) ? RIGHT : 0;

            if (goRight != 0 && (mods & Actions.COMMAND) != 0) // not for the platform primary
                goRight = 0;
                    
            if (DEBUG.Enabled) {

                //-----------------------------------------------------------------------------
                // DEBUG ACTIONS
                //-----------------------------------------------------------------------------
                    
                add(RIGHT, row);
                if (k == null) {
                    add("");
                    add("");
                    if (Util.isMacPlatform()) add("");
                    add("");                        
                } else {
                    add(RIGHT+BOLD, Integer.toBinaryString(mods));

                    if (Util.isMacLeopard()) {
                        add(BOLD+goRight, get_MacOSX_Leopard_Modifier_Names(mods));
                        add(BOLD+goRight+(DEBUG.Enabled?0:CENTER), KeyEvent.getKeyModifiersText(mods));
                    } else {
                        add(BOLD+goRight, KeyEvent.getKeyModifiersText(mods)); 
                    }
                    add(BOLD+CENTER, keyCodeChar(k.getKeyCode()));
                }
                    
                add(BOLD, a.getPermanentActionName());
                add(k == null ? "" : k);
                add(a.getClass().getName());

            } else {

                //=============================================================================
                // Production ACTIONS
                //=============================================================================
                    
                if (Util.isMacLeopard()) {
                    //add(""); // for gap
                    addGap(); // for gap
                    add(BOLD + NO_EAST_GAP + goRight, get_MacOSX_Leopard_Modifier_Glyphs(mods));
                    add(BOLD + NO_WEST_GAP + CENTER, keyCodeChar(k.getKeyCode()));
                } else {
                    add(BOLD + goRight, KeyEvent.getKeyModifiersText(mods));
                    add(BOLD + NO_WEST_GAP, keyCodeChar(k.getKeyCode()));
                }
                add(a.getPermanentActionName());

                //if (Util.isMacLeopard()) add(""); // for gap: implied
                

            }

        }
            
        final javax.swing.JLabel t = new javax.swing.JLabel();
            
        if (DEBUG.Enabled)
            t.setFont(VueConstants.LargeFont);
        else
            t.setFont(VueConstants.MediumFont);

        if (DEBUG.Enabled) Log.debug("HTML size: " + ShortcutsAction.html.length());
        t.setText(html.toString());

        t.setOpaque(false);
        //t.setFocusable(false);
            
        return new JScrollPane(t,
                               JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                               JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                               );
    }

    // The Mac OSX Leopard JVM impl changed KeyEvent.getKeyModifiersText(mods) to return the actual
    // special mac glyphs representing these keys.  This replaces the old functionality
    // (swiped from the java source), in case we want to use it.
    private static String get_MacOSX_Leopard_Modifier_Names(int modifiers) {
        StringBuffer buf = new StringBuffer();
        if ((modifiers & InputEvent.META_MASK) != 0) {
            //buf.append(Toolkit.getProperty("AWT.meta", "Meta"));
            //buf.append("Command");
            buf.append("Apple");
            buf.append("+");
        }
        if ((modifiers & InputEvent.CTRL_MASK) != 0) {
            //buf.append(Toolkit.getProperty("AWT.control", "Ctrl"));
            buf.append("Ctrl");
            buf.append("+");
        }
        if ((modifiers & InputEvent.ALT_MASK) != 0) {
            //buf.append(Toolkit.getProperty("AWT.alt", "Alt"));
            buf.append("Alt");
            buf.append("+");
        }
        if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
            //buf.append(Toolkit.getProperty("AWT.shift", "Shift"));
            buf.append("Shift");
            buf.append("+");
        }
        if ((modifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
            //buf.append(Toolkit.getProperty("AWT.altGraph", "Alt Graph"));
            buf.append("Alt Graph");
            buf.append("+");
        }
        if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
            //buf.append(Toolkit.getProperty("AWT.button1", "Button1"));
            buf.append("Button1");
            buf.append("+");
        }
        if (buf.length() > 0) {
            buf.setLength(buf.length()-1); // remove trailing '+'
        }
        return buf.toString();
    }
    

    private static String get_MacOSX_Leopard_Modifier_Glyphs(int modifiers) {
        return KeyEvent.getKeyModifiersText(modifiers).replace('+', (char)0);
    }
            
    

    
    public static void main(String args[])
    {
        VUE.init(args);

        // Ensure the tools are loaded to we can see their shortcuts:
        VueToolbarController.getController();
        
        javax.swing.JFrame frame = new javax.swing.JFrame("vueParentWindow");

        // Ensure that all the Actions are instantiated so we can see them:
        tufts.vue.Actions.Delete.toString();
	         
        // Let us see the actual menu bar:
        frame.setJMenuBar(new tufts.vue.gui.VueMenuBar());
        frame.setVisible(true); // do this or we can't see the menu bar

        new ShortcutsAction().act();

        //         Log.info("creating...");
        //         DockWindow shortcuts = ShortcutsAction.createWindow();
        //         Log.info("showing...");
        //         shortcuts.pack(); // fit to HTML content
        //         shortcuts.setVisible(true);

        if (args.length > 1)
            System.out.println(ShortcutsAction.html);

    }
}
    
