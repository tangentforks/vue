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

import tufts.vue.DEBUG;
import tufts.vue.VueAction;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * MenuButton
 *
 * A button that supports a drop-down menu and changes state based on the
 * currently selected drop-down menu item.  
 * The menu can be initialized from either an array of property values, or actions.
 * Property change events or action events (depending on the initialization type)
 * are fired when the menu selection changes.
 *
 * @author Scott Fraize
 * @version March 2004
 *
 */
public abstract class MenuButton extends JButton implements ActionListener
// todo: cleaner to get this to subclass from JMenu, and then cross-menu drag-rollover
// menu-popups would automatically work also.
{
    protected String mPropertyName;
    protected JPopupMenu mPopup;
    protected JMenuItem mEmptySelection;
    private Icon mButtonIcon;

    protected static final String ArrowText = "v ";
    private static boolean isButtonActionable = false;

    private boolean actionAreaClicked = false;

    public MenuButton()
    {
        //setIcon(sIcon);
        //mPopup = getPopupMenu();
        //buildMenu(pColors, pMenuNames, pHasCustom);
        //setBorder(new CompoundBorder(BorderFactory.createRaisedBevelBorder(), new EmptyBorder(3,3,3,3)));
        //setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(), new LineBorder(Color.blue, 6)));
        //setBorder(BorderFactory.createRaisedBevelBorder());
        
        final int borderIndent = 2;
        if (false) {
            //setBorder(BorderFactory.createEtchedBorder());
        } else {
            setOpaque(false);
            setBorder(null);
            //setBorder(new LineBorder(Color.blue, 2));
            //setBorder(new EmptyBorder(2,2,2,2));
            //setContentAreaFilled(false);
            //setRolloverIcon(new VueButtonIcon(null, VueButtonIcon.ROLLOVER, new Dimension(0,0)));
        }

        
        if (false) {
            setFont(tufts.vue.VueConstants.FONT_ICONIC);
            setText(ArrowText);
        }
        setFocusable(false);
        addActionListener(this);
        addMouseListener(new tufts.vue.MouseAdapter(toString()) {
                public void mousePressed(MouseEvent e) {
                    if (false&&/*getText() == ArrowText &&*/
                        isButtonActionable && getIcon() != null && e.getX() < getIcon().getIconWidth() + borderIndent) {
                        actionAreaClicked = true;
                    } else {
                        actionAreaClicked = false;
                        Component c = e.getComponent();
                        getPopupMenu().show(c, 0, (int) c.getBounds().getHeight());
                    }
                }
            });
    }

    public void actionPerformed(ActionEvent e) {
        if (DEBUG.TOOL||DEBUG.EVENTS) System.out.println(this + " " + e);
        if (actionAreaClicked)
            firePropertySetter();
    }

    /**
     * Set the raw icon used for displaying as a button (may be additionally decorated).
     * Intended for use during initialization.
     **/
    public void setButtonIcon(Icon i) {
        System.out.println("MenuButton " + this + " setButtonIcon " + i);
        if (DEBUG.Enabled) new Throwable().printStackTrace();
        _setIcon(mButtonIcon = i);
    }
    
    /*Intended for use during initialization OR then later for value changes.
    public void setOrResetButtonIcon(Icon i) {
        System.out.println("MenuButton " + this + " setButtonIcon " + i);
        new Throwable().printStackTrace();
        if (mButtonIcon == null && i != null) {
            _setIcon(mButtonIcon = i);
        } if (i instanceof BlobIcon && mButtonIcon instanceof BlobIcon) {
            // in this case, just transfer color from the given icon to
            // our already existing color.
            ((BlobIcon)mButtonIcon).setColor(((BlobIcon)i).getColor());
        } else {
            System.out.println("MenuButton " + this + " setButtonIcon: INITIALIZED");
            mButtonIcon = i;
        }
    }
    */
    
    protected Icon getButtonIcon() {
        return mButtonIcon;
    }

    /* override of JButton so can catch blob-icon 
    public void XsetIcon(Icon i) {
        if (mButtonIcon != null) {
            if (i instanceof BlobIcon && mButtonIcon instanceof BlobIcon)
                ((BlobIcon)mButtonIcon).setColor(((BlobIcon)i).getColor());
        } else {
            System.out.println("MenuButton " + this + " setIcon " + i);
            _setIcon(i);
        }
    }
    */
    
    private void _setIcon(Icon i) {
        if (false) {
            super.setIcon(i);
            super.setRolloverIcon(new VueButtonIcon(i, VueButtonIcon.ROLLOVER));
        } else {
            final int pad = 6;
            //Dimension d = new Dimension(i.getIconWidth()+pad, i.getIconHeight()+pad);
            Dimension d = new Dimension(21,21);
            VueButtonIcon.installGenerated(this, i, d);
            System.out.println("MenuButton " + this + " *** installed generated, setPreferredSize " + d);
            setPreferredSize(d);
        }
    }
    

    protected JPopupMenu getPopupMenu() {
        return mPopup;
    }
    
    public void setPropertyName(String pName) {
        mPropertyName = pName;
    }

    public String getPropertyName() {
        return mPropertyName;
    }

    public abstract void setPropertyValue(Object propertyValue);
    public abstract Object getPropertyValue();

    public void loadPropertyValue(tufts.vue.beans.VueBeanState state) {
        if (DEBUG.TOOL) System.out.println(this + " loading " + getPropertyName() + " from " + state);
        setPropertyValue(state.getPropertyValue(getPropertyName()));
    }
	
    /** factory method for subclasses -- build's an icon for menu items */
    protected Icon makeIcon(Object value) {
        return null;
    }
    
    /** override if there is a custom menu item */
    protected Object runCustomChooser() {
        return null;
    }
    
    /** @param values can be property values or actions (or even a mixture) */
    protected void buildMenu(Object[] values)
    {
        buildMenu(values, null, false); 
    }

    private final String mValueKey = "prop.value";
    
    /**
     * @param values can be property values or actions
     * @param names is optional
     * @param createCustom - add a "Custom" menu item that calls runCustomChooser
     *
     * If values are actions, the default handleValueSelection won't ever
     * do anything as a value wasn't set on the JMenuItem -- it's assumed
     * that the action is handling the value change.  In this case override
     * handleMenuSelection to change the buttons appearance after a selection change.
     */
    protected void buildMenu(Object[] values, String[] names, boolean createCustom)
    {
        mPopup = new JPopupMenu();
			
        //final String valueKey = getPropertyName() + ".value"; // propertyName usually not set at this point!
            
        final ActionListener menuItemAction =
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    handleMenuSelection(e);
                }};
            
        for (int i = 0; i < values.length; i++) {
            JMenuItem item;
            if (values[i] instanceof Action)
                item = new JMenuItem((Action)values[i]);
            else
                item = new JMenuItem();
            item.putClientProperty(mValueKey, values[i]);
            Icon icon = makeIcon(values[i]);
            if (icon != null)
                //item.setIcon(icon);
                item.setIcon(makeIcon(values[i]));
            if (names != null)
                item.setText(names[i]);
            item.addActionListener(menuItemAction);
            mPopup.add(item);
        }

        if (createCustom) {
            JMenuItem item = new JMenuItem("Custom..."); // todo: more control over this item
            item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { handleValueSelection(runCustomChooser()); }});
            mPopup.add(item);
        }

        mEmptySelection = new JMenuItem();
        mEmptySelection.setVisible(false);
        mPopup.add(mEmptySelection);
    }
		
    protected void handleMenuSelection(ActionEvent e) {
        if (false) {
            // this allows for generically copying the sub-menu icon up to the
            // button icon: for now we're only allow cases that use a named
            // property key, and the button icon get's updated via the property
            // change.
            // BUT: if a shape icon, will need this every time! How gross...
            Icon i = ((AbstractButton)e.getSource()).getIcon();
            if (i != null)
                setButtonIcon(i);
        }
        handleValueSelection(((JComponent)e.getSource()).getClientProperty(mValueKey));
    }
    
    protected void handleValueSelection(Object newPropertyValue) {
        if (newPropertyValue == null) // could be result of custom chooser
            return;
        Object oldValue = getPropertyValue();
        setPropertyValue(newPropertyValue);
        // don't bother to fire if prop value is an Action?
        firePropertyChanged(oldValue, newPropertyValue);
        repaint();
    }
	
    /** fire a property change event even if old & new values are the same */
    protected void firePropertyChanged(Object oldValue, Object newValue)
    {
        if (getPropertyName() != null) {
            PropertyChangeListener[] listeners = getPropertyChangeListeners();
            if (listeners.length > 0) {
                PropertyChangeEvent event = new PropertyChangeEvent(this, getPropertyName(), oldValue, newValue);
                for (int i = 0; i< listeners.length; i++) {
                    listeners[i].propertyChange(event);
                }
            }
        }
    }
    protected void firePropertySetter() {
        Object o = getPropertyValue();
        if (DEBUG.TOOL||DEBUG.EVENTS) System.out.println(this + " firePropertySetter " + o);
        if (o instanceof Action) {
            if (o instanceof VueAction)
                ((VueAction)o).fire(this);
            else {
                Action a = (Action) o;
                a.actionPerformed(new ActionEvent(this, 0, (String) a.getValue(Action.NAME)));
            }
        } else {
            firePropertyChanged(o, o);
        }
    }
	
    public void paint(java.awt.Graphics g) {
        ((java.awt.Graphics2D)g).setRenderingHint
            (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
             java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paint(g);
    }
	
    public String toString() {
        return getClass().getName() + "[" + mPropertyName + "]";
    }
}



	
    /*
     * paint( Graphics g)
     * Overrides paint method and renders an additional icon ontop of
     * of the normal rendering to indicate if this button contains
     * a popup handler.
     *
     * param Graphics g the Graphics.
     **/
    /*

    // default offsets for drawing popup arrow via code
    public int mArrowSize = 3;
    public int mArrowHOffset  = -9;
    public int mArrowVOffset = -7;
	
    // the popup overlay icons for up and down states
    public Icon mPopupIndicatorIconUp = null;
    public Icon mPopupIndicatorDownIcon = null;

    
    public void paint( java.awt.Graphics pGraphics) {
        super.paint( pGraphics);
        if (true) return;
        Dimension dim = getPreferredSize();
        Insets insets = getInsets();
		
        // now overlay the popup menu icon indicator
        // either from an icon or by brute painting
        if(  (! false ) 
             &&  (mPopup != null) 
             && ( !mPopup.isVisible() ) ) {
            // draw popup arrow
            Color saveColor = pGraphics.getColor();
            pGraphics.setColor( Color.black);
			
            int w = getWidth();
            int h = getHeight();
			
            int x1 = w + mArrowHOffset;
            int y = h + mArrowVOffset;
            int x2 = x1 + (mArrowSize * 2) -1;
			
            for(int i=0; i< mArrowSize; i++) { 
                pGraphics.drawLine(x1,y,x2,y);
                x1++;
                x2--;
                y++;
            }
            pGraphics.setColor( saveColor);
        }
        else  // Use provided popup overlay  icons
            if(   (mPopup != null) && ( !mPopup.isVisible() ) ) {
			
                Icon overlay = null;
                if( isSelected() ) {
				
                    overlay = mPopupIndicatorDownIcon;
                }
                if( overlay != null) {
                    overlay.paintIcon( this, pGraphics, insets.top, insets.left);
                }
            }
    }
    */
