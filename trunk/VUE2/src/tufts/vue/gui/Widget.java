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


package tufts.vue.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseListener;

import tufts.vue.DEBUG;

import javax.swing.JComponent;

//import com.sun.xml.rpc.processor.modeler.j2ee.xml.javaXmlTypeMappingType;


/**

 * A convenice class for providing a wrapper for JComponents to go in
 * WidgetStack's or DockWindow's.

 * Most usefully this actually provides static methods so that any
 * existing JComponent can be treated as a Widget (it just needs to be
 * parented to a WidgetStack or DockWindow) without having to wrap it
 * in a Widget / make it a subclass.  This is doable because we need
 * only add a few properties to the JComponent (e.g., a title,
 * title-suffix) and change events can be iss issued to the parent via AWT
 * PropertyChangeEvents (e.g., expand/collapse, hide/show).
 
 *
 * @version $Revision: 1.14 $ / $Date: 2007-05-21 06:34:42 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class Widget extends javax.swing.JPanel
{
    static final String EXPANSION_KEY = "widget.expand";
    static final String HIDDEN_KEY = "widget.hide";
    static final String MENU_ACTIONS_KEY = "widget.menuActions";
    static final String HELP_ACTION_KEY = "widget.helpAction";
    static final String REFRESH_ACTION_KEY = "widget.refreshAction";
    static final String WANTS_SCROLLER_KEY = "widget.wantsScroller";
    static final String TITLE_HIDDEN_KEY = "widget.titleHidden";

    public static void setTitle(JComponent c, String title) {
        c.setName(title);
    }
    
    public static void setTitleHidden(JComponent c, boolean hidden) {
        setBoolean(c, TITLE_HIDDEN_KEY, hidden);
    }
    
    public static boolean isHidden(JComponent c)
    {
    	Boolean currentProp = (Boolean)c.getClientProperty(HIDDEN_KEY);
    	boolean current = currentProp == null ? false : currentProp.booleanValue();
    	return current;
    }
    /** Hide the entire widget, including it's title.  Do not affect expansion state. */
    public static void setHidden(JComponent c, boolean hidden) {
        // make sure instance method called in case it was overriden
        if (c instanceof Widget)
            ((Widget)c).setHidden(hidden);
        else
            setHiddenImpl(c, hidden);
    }
    protected static void setHiddenImpl(JComponent c, boolean hidden) {
        if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setHidden " + hidden);
        setBoolean(c, HIDDEN_KEY, hidden);
    }

    public static void setWantsScroller(JComponent c, boolean scroller) {
        if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setWantsScroller " + scroller);
        setBoolean(c, WANTS_SCROLLER_KEY, scroller);
    }
    
    
    /** Make sure the Widget is expanded (visible).  Containing java.awt.Window
     * will be made visible if it's not */
    public static void setExpanded(JComponent c, boolean expanded) {
        // make sure instance method called in case it was overriden
        if (c instanceof Widget)
            ((Widget)c).setExpanded(expanded);
        else
            setExpandedImpl(c, expanded);
    }
    
    public static boolean isExpanded(JComponent c)
    {
    	Boolean currentProp = (Boolean)c.getClientProperty(EXPANSION_KEY);
    	boolean current = currentProp == null ? false : currentProp.booleanValue();
    	return current;
    }
    protected static void setExpandedImpl(JComponent c, boolean expanded) {
        if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setExpanded " + expanded);
        //c.putClientProperty(EXPANSION_KEY, expanded ? Boolean.TRUE : Boolean.FALSE);

        setBoolean(c, EXPANSION_KEY, expanded);
        
        // We do NOT auto-display the containing window if startup is
        // underway, otherwise all sorts of stuff will show while the
        // windows are being pre-configed.
        
        if (expanded && !tufts.vue.VUE.isStartupUnderway()) {
            if (isBooleanTrue(c, HIDDEN_KEY) || !c.isVisible())
                setHidden(c, false);
            if (!DockWindow.AllWindowsHidden() && !tufts.vue.VUE.inNativeFullScreen())
                GUI.makeVisibleOnScreen(c);
        }

        //c.firePropertyChange("TESTPROPERTY", false, true);
    }
    
    public static void setHelpAction(JComponent c, String action)
    {
    //	if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setMenuAction " + action.toString());
        c.putClientProperty(HELP_ACTION_KEY, action);
    }
    
    public static void setRefreshAction(JComponent c, MouseListener action)
    {
    //	if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setMenuAction " + action.toString());
        c.putClientProperty(REFRESH_ACTION_KEY, action);
    }
    public static void setMenuActions(JComponent c, javax.swing.Action[] actions)
    {
        if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " Widget.setMenuActions " + java.util.Arrays.asList(actions));
        c.putClientProperty(MENU_ACTIONS_KEY, actions);
    }

    public static boolean isWidget(JComponent c) {
        return c != null && (c instanceof Widget || c.getClientProperty(EXPANSION_KEY) != null);
    }

    public static boolean wantsScroller(JComponent c) {
        return isBooleanTrue(c, WANTS_SCROLLER_KEY);
    }

    /** only set property if not already set
     * @return true if the value changed
     */
    protected static boolean setBoolean(JComponent c, String key, boolean newValue) {
        Boolean currentProp = (Boolean) c.getClientProperty(key);
        // default for property value not there is false:
        boolean current = currentProp == null ? false : currentProp.booleanValue();
        if (current != newValue) {
            if (DEBUG.WIDGET) System.out.println(GUI.name(c) + " WIDGET-CHANGE " + key + "=" + newValue);
            c.putClientProperty(key, newValue ? Boolean.TRUE : Boolean.FALSE);
            return true;
        } else
            return false;
    }

    protected static boolean isBooleanTrue(JComponent c, Object key) {
        Boolean boolProp = (Boolean) c.getClientProperty(key);
        return boolProp != null && boolProp.booleanValue();
    }
    
    // instance methods for when used as a subclassed wrapper of JPanel:
    
    /** Create a new empty Widget JPanel, with a default layout of BorderLayout */
    public Widget(String title) {
        super(new java.awt.BorderLayout());
        setName(title);
        if (DEBUG.BOXES) setBorder(new javax.swing.border.LineBorder(java.awt.Color.blue, 4));
    }        
    
    public Widget(String title, javax.swing.JButton blah) {
        super(new java.awt.BorderLayout());
        setName(title);        
        if (DEBUG.BOXES) setBorder(new javax.swing.border.LineBorder(java.awt.Color.blue, 4));
    }

    public final void setTitle(String title) {
        setTitle(this, title);
    }
    
    public void setExpanded(boolean expanded) {
        setExpandedImpl(this, expanded);
    }

    public void setHidden(boolean hidden) {
        setHiddenImpl(this, hidden);
    }

    public final void setTitleHidden(boolean hidden) {
        setTitleHidden(this, hidden);
    }

    public final void setMenuActions(javax.swing.Action[] actions) {
        setMenuActions(this, actions);
    }
    
    public final void setWantsScroller(boolean scroller) {
        setWantsScroller(this, scroller);
    }
    
    
}