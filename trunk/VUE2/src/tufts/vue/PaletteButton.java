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

import tufts.vue.gui.GUI;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;


/**
 *
 * This class provides a popup radio button selector component.
 * It is used for the main tool bar tool
 *
 * @author csb
 * @version $Revision: 1.17 $ / $Date: 2006-01-27 03:02:15 $ / $Author: sfraize $
 **/
public class PaletteButton extends JRadioButton //implements ActionListener
{
    /* this is thr eolumn threshold array to tell when to add another columng in the palette */
    static int mColThreshold[] = VueResources.getIntArray("menuFlowThreshold") ;
	
    // default offsets for drawing popup arrow via code
    public int mArrowSize = 3;
    public int mArrowHOffset  = -9;
    public int mArrowVOffset = -7;
	
    /** the popup overlay icons for up and down states **/
    public Icon mPopupIndicatorIconUp = null;
    public Icon mPopupIndicatorDownIcon = null;

    /** The currently selected palette item--if any **/
    protected PaletteButtonItem mCurSelection = null;
	
    /** the set of palette items for this buttons menu **/
    protected PaletteButtonItem [] mItems = null;
	
    /** does this button have a popup? **/
    protected boolean mHasPopup = false;
	
    /** do we need to draw an arrow via code **/
    protected boolean mDrawArrowByCode = false;
	
    /** the popup menu **/
    protected JPopupMenu mPopup = null;

    /** are we drawing the popup icon indcator with an image or in code? **/
    protected boolean mUseIconIndicator = true;
	
    /* the context object */
    private Object mContext = null;
	
    /** the current overlay popup indicator icon **/
    protected Icon mPopupIndicatorIcon = null;	
    protected Icon mPopupIndicatorUpIcon = null;

    private long lastHidden;

	
    /**
     * Creates a new PaletteButton with the passed array of items
     * as it's palette menu.
     * 
     *  It will preselect the first item in the array as
     *  its default selection and use its images for its own view.
     *
     * @param pItems  an array of PaletteButtonItems for the menu.
     **/
    public PaletteButton(PaletteButtonItem[] pItems)
    {
        if (pItems != null) {
            setPaletteButtonItems(pItems);
            setRolloverEnabled(true);
        }
        setBorder( null);
        setFocusable(false);
        GUI.applyToolbarColor(this);
    }
	
    /**
     * Creates a new PaletteButton with no menus
     **/
    public PaletteButton() {
        this(null);
    }
	
	
    /**
     * Sets a user context object.
     **/
    public void setContext( Object pContext) {
        mContext = pContext;
    }
	 
    /**
     * Gets teh user context object
     **/
    public Object getContext() {
        return mContext;
    }
	
    /**
     * setPopupIndicatorIcon
     * Sets the popup indicator icon icon
     *
     * @param pIcon the icon
     **
     public void setPopupIndicatorIcon( Icon pIcon) {
     mPopupIndicatorIcon = pIcon;
     }
	 
     /**
     * getPopupIndicatorIcon
     * Gets teh popup indicator icon
     * @return the icon
     **/
    public Icon getPopupIndicatorIcon() {
        return mPopupIndicatorIcon;
    }


    /**
     * This sets the state of the mUseArrowIcon property.  It is
     * used to tell how to draw the popup visual cue.  If true,
     * then it uses the image, otherwise, it draws the default
     * arrow.
     * 
     **/
    public void setPopupIconIndicatorEnabled( boolean pState) {
        mUseIconIndicator = pState;
    }
	
    /**
     * isPopupIconIndicatorEnabled
     * This method tells how to draw the popup arrow indicator
     * on the button in paint().  If some image should be used, then this
     * returns true.  If the default arrow that's drawn by code
     * should be used, this returns false.
     *
     * @ return boolean true if should use image to draw indictor; false if not
     **/
    public boolean isPopupIconIndicatorEnabled() {
        //return mUseIconIndicator;
        return false;
    }

    /**
     * This method adds a new PaleeteButtonItem to the PaletteButton's
     * menu.
     *
     * @param pItem the new PaletteButtonItem to be added.
     **/
    public void addPaletteItem(PaletteButtonItem pItem) {
        if( mItems == null) {
            mItems = new PaletteButtonItem[1];
            mItems[0] = pItem;
        } else {
            int len = mItems.length;
            PaletteButtonItem newItems[] = new PaletteButtonItem[len+1];
            for(int i=0; i< len; i++) {
                newItems[i] = mItems[i];
            }
            newItems[len] = pItem;
            setPaletteButtonItems(newItems);
        }
    }
	
	
    /**
     * This removes an item from the popup menu
     * @param pItem the item to remove
     **/
    public void removePaletteItem( PaletteButtonItem pItem ) {
	 	
        if( mItems != null) {
            int len = mItems.length;
            PaletteButtonItem [] newItems = null;
            boolean found = false;
            int slot = 0;
            if( len > 1) 
                newItems = new PaletteButtonItem [len-1];
	 		
            for( int i=0; i< len; i++) {
                if( mItems[i].equals( pItem) ) {
                    found = true;
                }
                else {
                    newItems[slot] = mItems[i];
                    slot++;
                }
            }
            if( found) {
                if( len == 1)
                    newItems = null;
                setPaletteButtonItems( newItems);
            }
        }
    }
	
	
    /**
     * Sets the set of PaletteButtonItems for the popup menu
     * @param pItems the array of items.
     **/
    public void setPaletteButtonItems(PaletteButtonItem [] pItems) {
        if (DEBUG.INIT) System.out.println(this + " setPaletteButtonItems n=" + pItems.length);
        mItems = pItems;
        buildPalette();
    }
	 
    /**
     * getPaletteButtonItems
     * Gets the array of PaletteButtonItems
     * @return the array of items
     **/
    public PaletteButtonItem[] getPaletteButtonItems() {
        return mItems;
    }
	 
	 
	  
    /**
     * This method builds the PaletteButton's palette menu and sets up
     * all appropriate event listeners to handle menu selection.  It
     * also calculates the best layout fo rthe popup based on the
     * the number of items.
     **/
    protected void buildPalette() {
		
        // clear the old
        mPopup = null;
		
        if (mItems == null || mItems.length < 2) {
            mHasPopup = false;
            return;
        }
		 
        mHasPopup = true;
        int numItems = mItems.length;
		
        int cols = 0;
        while (mColThreshold[cols] < numItems && cols < mColThreshold.length)
            cols++;
        int rows = (numItems + (numItems % cols )) / cols ;

        if (rows < 3 && GUI.isMacBrushedMetal() && VueUtil.getJavaVersion() < 1.5f /*&& tiger */) { // total hack for now

            // JAVA BUG: there appears to be an absolute minimunm width & height
            // for pop-up's: approx 125 pixels wide, no smaller, and approx 34 pixels
            // tall, no smaller.
            //
            // This bug only shows up in Java 1.4.2 on Mac OS X Tiger (10.4+) if
            // we're using the Metal version of the Mac Aqua Look & Feel.
            // The default Mac Aqua L&F doesn't see the bug, and java 1.5 works fine.
            // 
            // Note this bug affects almost all pop-ups: even combo-box's!  (See
            // VUE font-size), as well as many roll-over / tool-tip pop-ups.
            // I'm guessing the bug is somewhere down in the PopupFactory or PopupMenuUI
            // or the java small-window caching code shared by pop-ups and tool-tips.
            //
            // SMF 2005-08-11

            // This forces the smaller menus we use into 1 row which use
            // up the forced width better.  These #'s hard-coded based
            // on our current usage...
            rows = 1;
            cols = 3;
        }
		
        //JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        mPopup = new PBPopupMenu(rows, cols);
        
        new GUI.PopupMenuHandler(this, mPopup); // installs mouse & popup listeners

        for (int i = 0; i < numItems; i++) {
            mPopup.add(mItems[i]);
            mItems[i].setPaletteButton(this);
            //mItems[i].addActionListener(this);
        }
        
        if (DEBUG.INIT)
            System.out.println("*** CREATED POPUP " + mPopup
                               + " margin=" + mPopup.getMargin()
                               + " layout=" + mPopup.getLayout()
                               );
	
    }
	
    /**
     *  setPropertiesFromItem
     * This method sets teh display properties of the button based on
     * the properties set in a PaletteButtonMenu item.  This allows the 
     * primary tool button to reflect the current selected item on the main toolbar.
     *
     * @param pItem - the PaletteButtonItem to use as the source
     **/

    public void setPropertiesFromItem(AbstractButton pItem) {
	
        this.setIcon( pItem.getIcon() );
        this.setPressedIcon( pItem.getPressedIcon() );
        this.setSelectedIcon( pItem.getSelectedIcon() );
        this.setRolloverIcon( pItem.getRolloverIcon() );
        this.setDisabledIcon( pItem.getDisabledIcon() );
		
        this.setRolloverEnabled( getRolloverIcon() != null);
    }
	
	
    public void setPopupOverlayUpIcon( Icon pIcon) {
        mPopupIndicatorUpIcon = pIcon;
    }
	
    public void setPopupOverlayDownIcon( Icon pIcon) {
        mPopupIndicatorDownIcon = pIcon;
    }
	
    public void setOverlayIcons( Icon pUpIcon, Icon pDownIcon) {
        setPopupOverlayUpIcon( pUpIcon);
        setPopupOverlayDownIcon( pDownIcon);
    }
	
	
    public void setIcons( Icon pUp, Icon pDown, Icon pSelect, Icon pDisabled, 
                          Icon pRollover ) {
        this.setIcon( pUp);
        this.setPressedIcon( pDown);
        this.setSelectedIcon( pSelect);
        this.setDisabledIcon( pDisabled);
        this.setRolloverIcon( pRollover);
		
        this.setRolloverEnabled(  pRollover != null );
		
    }

    public void setSelected(boolean b) {
        //System.out.println(this + " setSelected " + b);
        super.setSelected(b);
    }
    protected void fireStateChanged() {
        //System.out.println("PaletteButton: fireStateChanged, selected="+isSelected() + " " + getIcon());
        super.fireStateChanged();
    }
    /**
     * Overrides paint method and renders an additional icon ontop of
     * of the normal rendering to indicate if this button contains
     * a popup handler.
     *
     * @param Graphics g the Graphics.
     **/
    public void paint(java.awt.Graphics g) {
        super.paint(g);
		
        Dimension dim = getPreferredSize();
        Insets insets = getInsets();
		
        // now overlay the popup menu icon indicator
        // either from an icon or by brute painting
        if( !isPopupIconIndicatorEnabled()
            && mPopup != null 
            && !mPopup.isVisible()
            ) {
            // draw popup arrow
            Color saveColor = g.getColor();
            g.setColor( Color.black);
			
            int w = getWidth();
            int h = getHeight();
			
            int x1 = w + mArrowHOffset;
            int y = h + mArrowVOffset;
            int x2 = x1 + (mArrowSize * 2) -1;
			
            //((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            //RenderingHints.VALUE_ANTIALIAS_ON);
            
            for(int i=0; i< mArrowSize; i++) { 
                g.drawLine(x1,y,x2,y);
                x1++;
                x2--;
                y++;
            }
            g.setColor( saveColor);
        }
        else  // Use provided popup overlay  icons
            if(   (mPopup != null) && ( !mPopup.isVisible() ) ) {
			
                Icon overlay = mPopupIndicatorUpIcon;
                if( isSelected() ) {
				
                    overlay = mPopupIndicatorDownIcon;
                }
                if( overlay != null) {
                    overlay.paintIcon( this, g, insets.top, insets.left);
                }
            }
    }
	
    /**
     * This method handles remote or direct  selection of a PaletteButtonItem
     *
     * It will update its own icons based on the selected item
     **/
    public void actionPerformed(ActionEvent e) {
        if (DEBUG.Enabled) System.out.println(this + " " + e);
        //if (DEBUG.TOOL) System.out.println(this + " calling doClick (probably vestigal)");
        //System.out.println(pEvent);
        // fake a click to handle radio selection after menu selection
        // this appears no longer to be needed, tho I'm leaving it in for
        // now just in case (todo cleanup: as part of tool VueTool / ToolController re-archtecting)
        // If we can really do away with this, it means VueTool no longer needs to subclass
        // AbstractAction in order to add all the buttons as action listeners.
        //doClick();		
    }

    public String toString() {
        return "PaletteButton[" + getContext() + "]";
    }
	
	
    /**
     * JPopupMenu subclass to set up appearance of pop-up menu.
     *
     * As of java 1.4.2 on Tiger (Mac OS X 10.4+) the layout
     * of this is broken (too much space).  OS X 10.3 works
     * in 1.4.2, and it works fine in java 1.5 on 10.4+.
     *
     **/
    public class PBPopupMenu extends JPopupMenu
                                     //implements MouseListener, PopupMenuListener
    {
        private boolean mIsVisibleLocked;
        private long lastHidden;

        public PBPopupMenu(int rows, int cols) {
            //setBorderPainted(false);
            setFocusable(false);
            GUI.applyToolbarColor(this);
            setBorder(new LineBorder(getBackground().darker().darker(), 1));

            GridLayout grid = new GridLayout(rows, cols);
            grid.setVgap(0);
            grid.setHgap(0);
            if (DEBUG.INIT) System.out.println("*** CREATED GRID LAYOUT " + grid.getRows() + "x" + grid.getColumns());
            setLayout(grid);
        }

        public void menuSelectionChanged(boolean isIncluded) {
            if (DEBUG.Enabled) System.out.println(this + " menuSelectionChanged included=" + isIncluded);
            super.menuSelectionChanged(isIncluded);
        }
        
        public String toString() {
            return "PBPopupMenu[" + getIcon() + "]";
        }
    }
    
	
	
}  // end of class PaletteButton






