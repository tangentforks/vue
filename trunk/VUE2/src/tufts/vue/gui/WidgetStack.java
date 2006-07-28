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

import tufts.Util;
import tufts.vue.DEBUG;
import tufts.vue.VueResources;

import java.beans.PropertyChangeEvent;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.tools.xjc.generator.unmarshaller.automaton.Alphabet.SuperClass;

/**
 * A vertical stack of collapsable/expandable regions containing arbitrary JComponent's.
 *
 * Note that the ultimate behaviour of the stack will be very dependent on the
 * the preferredSize/maximumSize/minimumSize settings on the contained JComponent's.
 *
 * @version $Revision: 1.29 $ / $Date: 2006-07-28 20:55:31 $ / $Author: mike $
 * @author Scott Fraize
 */
public class WidgetStack extends Widget
{
    private final JPanel mGridBag;
    private final GridBagConstraints _gbc = new GridBagConstraints();
    private final Insets ExpandedTitleBarInsets = GUI.EmptyInsets;
    private final Insets CollapsedTitleBarInsets = new Insets(0,0,1,0);
    private final GridBagLayout mLayout;
    private final JComponent mDefaultExpander;
    private final ArrayList mWidgets = new ArrayList();

    private WidgetTitle mLockedWidget = null;
    private int mExpanderCount = 0;
    private int mExpandersOpen = 0;

    private Dimension mMinSize = new Dimension();

    public WidgetStack() {
        this("<>");
    }        

    public WidgetStack(String name) {
        super(name);
        mLayout = new GridBagLayout();
        mGridBag = this;
        setLayout(mLayout);

        if (DEBUG.BOXES) setBorder(new javax.swing.border.LineBorder(Color.cyan, 4));

        _gbc.gridwidth = GridBagConstraints.REMAINDER; // last in line as only one column
        _gbc.anchor = GridBagConstraints.NORTH;
        _gbc.weightx = 1;                              // always expand horizontally
        _gbc.gridx = 0;
        _gbc.gridy = 0;

        // We now add a component "guaranteed" to be at the bottom of the stack
        // (gridy=64), that starts invisible, but when all other vertical expanders
        // become invisible (are closed), this component will be set to visible (tho it
        // will display nothing), and will eat up any leftover vertical space at the
        // bottom of gridbag (it has a non-zero weighty), so if everything is collapsed,
        // the titleBar's all go to the top, instead of the middle.  (In a grid bag,
        // there must always be at least one visible component with a non-zero gridy
        // value, or everything is clumped in the center of the display area).
        
        GridBagConstraints c = (GridBagConstraints) _gbc.clone();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.SOUTH;
        c.weighty = 1;
        c.gridy = 64; 
        mDefaultExpander = new JPanel(new BorderLayout());
        mDefaultExpander.setMinimumSize(new Dimension(0,0));
        mDefaultExpander.setPreferredSize(new Dimension(0,0));
        if (DEBUG.BOXES) {
            mDefaultExpander.setOpaque(true);
            mDefaultExpander.setBackground(Color.darkGray);
            JLabel l = new JLabel("WidgetStack: veritcal expander", JLabel.CENTER);
            l.setForeground(Color.white);
            mDefaultExpander.add(l);
        }
        mDefaultExpander.setVisible(false);
        add(mDefaultExpander, c);

        mMinSize.width = 100;

        // todo: need to set min size on whole stack (nitems * title height)
        // and have DockWindow respect this.
    }


    /**
     * At least one pane MUST have a non-zero vertical expansion
     * weight (usually values between 0.0 and 1.0: meaninful only
     * relative to each other), otherwise the panes will all clump
     * together in the middle.
     */
    
    public void addPane(String title, JComponent widget, float verticalExpansionWeight) {
        //verticalExpansionWeight=0.0f;
        boolean isExpander = (verticalExpansionWeight != 0.0f);

        if (isExpander)
            mExpanderCount++;
        
        WidgetTitle titleBar = new WidgetTitle(title, widget, isExpander);
        mWidgets.add(titleBar);
        mMinSize.height = mWidgets.size() * (TitleHeight + 1);
        //setMinimumSize(mMinSize);
        
        if (DEBUG.WIDGET) {
            out("addPane:"
                + " expansionWeight=" + verticalExpansionWeight
                //+ " expanderCnt=" + mExpanderCount
                //+ " isExpander=" + isExpander
                + " [" + title + "] containing " + GUI.name(widget));
            GUI.dumpSizes(widget, "WidgetStack.addPane");
        }


        _gbc.weighty = 0;
        _gbc.fill = GridBagConstraints.HORIZONTAL;
        _gbc.insets = ExpandedTitleBarInsets;
        if (!isBooleanTrue(widget, TITLE_HIDDEN_KEY))
            mGridBag.add(titleBar, _gbc);
        
        _gbc.gridy++;
        _gbc.fill = GridBagConstraints.BOTH;
        _gbc.weighty = verticalExpansionWeight;
        _gbc.insets = GUI.EmptyInsets;

        if (false) {

            // if component has no border, add the default one
            // Actually, this also no good: what if a scroll-pane?
            if (widget.getBorder() == null && !(widget instanceof JScrollPane))
                widget.setBorder(GUI.WidgetInsetBorder);
            mGridBag.add(widget, _gbc);

            /*
            // Enforced white-space border
            JPanel widgetPanel = new JPanel(new BorderLayout());
            widgetPanel.setOpaque(false);
            widgetPanel.add(widget);
            widgetPanel.setBorder(GUI.WidgetInsetBorder);
            mGridBag.add(widgetPanel, _gbc);
            */
        } else {
            if (false && Widget.wantsScroller(widget)) {
                // Would need to handle seeing up through the contained
                // widget for property changes: e.g. hidden would
                // need to hide the scroller, not the widget.
                JScrollPane scroller = new JScrollPane(widget,
                                                       JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                       JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                mGridBag.add(scroller, _gbc);
            } else {
                mGridBag.add(widget, _gbc);
            }
        }
            

        _gbc.gridy++;

        //if (!widget.isPreferredSizeSet()) {// note: a java 1.5 api call only
        //if (!isExpander && !widget.isMinimumSizeSet())
        //    widget.setMinimumSize(widget.getPreferredSize());                
        
    }

    /**

     * The given widget *must* already have it's name set to be used as the title.

     * Note that if verticalExpansionWeight is zero, it is also important that the given
     * JComponent provides a reasonable preferredSize or minimumSize. Most Swing
     * components provide a reasonable preferredSize automatically, but pay attention to
     * layout managers that might not do such a good job of passing up this information.
     * Also of particular note are JScrollPanes, which by default usually will collapse
     * down to about nothing unless you manually set the pref or min sizes them (or, say a
     * container they're laid out in that they're expanding to fill).
     
     **/
    public void addPane(JComponent widget, float verticalExpansionWeight) {
        addPane(widget.getName(), widget, verticalExpansionWeight);
    }
    

    public void addPane(String title, JComponent widget) {
        addPane(title, widget, 1.0f);
    }

    public Widget addPane(String title) {
        Widget w = new Widget(title);
        addPane(w, 1.0f);
        return w;
    }

    /** @param c must have name set (setName) */
    public void addPane(JComponent c) {
        addPane(c, 1f);
    }

    public void addNotify() {
        //if (DEBUG.WIDGET) out("minSize " + mMinSize);
        updateDefaultExpander();
        super.addNotify();
        if (mExpanderCount == 0)
            if (DEBUG.Enabled) out("warning: no vertical expanders");
            //tufts.Util.printStackTrace("warning: no vertical expanding panes; WidgetStack will not layout properly");
        setName("in " + GUI.name(getParent()));
    }
    
    private void updateDefaultExpander() {
        //System.out.println("EXPANDERS OPEN: " + mExpandersOpen);
        if (mExpandersOpen == 0)
            mDefaultExpander.setVisible(true);
        else
            mDefaultExpander.setVisible(false);
    }

    private void updateLockingState() {
        /*
        if (DEBUG.WIDGET) out("updateLockingState: expanders open = " + mExpandersOpen);
        if (mExpandersOpen == 1) {
            findFirstOpenExpander().setLocked(true);
        } else if (mLockedWidget != null) {
            mLockedWidget.setLocked(false);
        }
        */
    }

    private WidgetTitle findFirstOpenExpander() {
        WidgetTitle w;
        Iterator i = mWidgets.iterator();
        while (i.hasNext()) {
            w = (WidgetTitle) i.next();
            if (w.isExpander && w.mExpanded && w.isVisible())
                return w;
        }
        return null;
    }

    public static final int TitleHeight = VueResources.getInt("gui.widget.title.height", 18);
    public static final Color
        TopGradient = VueResources.getColor("gui.widget.title.background.top", 108,149,221),
        BottomGradient = VueResources.getColor("gui.widget.title.background.bottom", 80,123,197);
    
    // Mac Finder top blue: Color(79,154,240);
    // Mac Finder bottom blue: Color(0,133,246);
    private static final GradientPaint Gradient
        = new GradientPaint(0,           0, TopGradient,
                            0, TitleHeight, BottomGradient);
    private static final GradientPaint GradientEmbedded
        = new GradientPaint(0,           0, new Color(79,154,240),
                            0, TitleHeight, new Color(0,133,246));

    //private static final char RightArrowChar = 0x25B6; // unicode "black right pointing triangle"
    //private static final char DownArrowChar = 0x25BC; // unicode "black down pointing triangle"
    private static final char RightArrowChar = DockWindow.RightArrowChar;
    private static final char DownArrowChar = DockWindow.DownArrowChar;

    private static final boolean isMac = tufts.Util.isMacPlatform();

    //private static final char Chevron = 0xBB; // unicode "right-pointing double angle quotation mark"

    class WidgetTitle extends Box implements java.beans.PropertyChangeListener {

        private final JLabel mTitle;
        private final MenuButton mMenuButton;
        private final JComponent mWidget;
        private final GUI.IconicLabel mIcon;
        private final RefreshButton refreshButton;
        private final HelpButton helpButton;
        
        private final boolean isExpander;

        private boolean isLocked = false;
        private boolean mExpanded = true;
        private boolean mEmbeddedStack = false;
        private boolean mTitleVisible = true; // if false, display widget, but not title (implies no user control)

        private Color mTopColor;
        private final GradientPaint mGradient;

        public WidgetTitle(String label, JComponent widget, boolean isExpander) {
            super(BoxLayout.X_AXIS);
            this.isExpander = isExpander;
            setName(label);
            setOpaque(true);
            mWidget = widget;
            mTitle = new JLabel(label);
            GUI.init(mTitle, "gui.widget.title");
            
            String localName = widget.getName();
            if (localName == null)
                localName = label;
            if (localName != null) {
                String localKey = "gui.widget.title." + localName;
                GUI.init(mTitle, localKey);
                mTopColor = VueResources.getColor(localKey + ".background.top");
                if (mTopColor != null) {
                    Color botColor = VueResources.getColor(localKey + ".background.bottom", BottomGradient);
                    mGradient = new GradientPaint(0,           0, mTopColor,
                                                  0, TitleHeight, botColor);
                } else {
                    mGradient = Gradient;
                }
                mTitle.setText(VueResources.getString(localKey + ".text", label));
            } else
                mGradient = Gradient;

            if (mTopColor == null)
                mTopColor = TopGradient;


            // TODO: merge with DockWindow for offset / std property
            add(Box.createHorizontalStrut(isMac ? 17 : 6));
//             int iconHeight = 10;
//             int iconWidth = 9;
//             int fontSize = 9;
//             mIcon = new GUI.IconicLabel(DownArrowChar, fontSize, Color.white, iconWidth, iconHeight);

            // TODO: merge with DockWindow code for same
            mIcon = new GUI.IconicLabel(DownArrowChar,
                                        16, // point-size
                                        Color.white,
                                        15, // fixed width
                                        10); // fixed height
            //if (isMac)
                 mIcon.setBorder(new javax.swing.border.EmptyBorder(0,0,1,0)); // t,l,b,r
             
            add(mIcon);
            
            add(Box.createHorizontalStrut(isMac ? 1 : 2));
            add(mTitle);

            add(Box.createGlue());
            
            refreshButton = new RefreshButton("refreshButton",null);
            add(refreshButton);
        
            helpButton = new HelpButton(null);
            add(helpButton);
            
            add(Box.createHorizontalStrut(isMac ? 1 : 2));
            
            mMenuButton = new MenuButton(null);
            add(mMenuButton);
                        
            setPreferredSize(new Dimension(50, TitleHeight));
            setMaximumSize(new Dimension(Short.MAX_VALUE, TitleHeight));
            setMinimumSize(new Dimension(50, TitleHeight));

            addMouseListener(new tufts.vue.MouseAdapter(label) {
                    //public void mouseClicked(MouseEvent e) { if (!isLocked) Widget.setExpanded(mWidget, !mExpanded); }
                    public void mouseClicked(MouseEvent e) { handleMouseClicked(); }
                    public void mousePressed(MouseEvent e) { if (!isLocked) mIcon.setForeground(mTopColor.brighter()); }
                    public void mouseReleased(MouseEvent e) { if (!isLocked) mIcon.setForeground(Color.white); }
                });

            // Check for property values set on the JComponent before being added to the
            // WidgetStack.  Important to handle EXPANSION_KEY before HIDDEN_KEY
            // (changing the expansion of something that is hidden is currently
            // undefined).  If the property is already set, we handle it via a fake
            // propertyChangeEvent.  If it isn't set, we set the default, which won't
            // trigger a recursive propertyChangeEvent as we haven't added us as a
            // property change listener yet.

            // todo: title-hidden currently only takes effect at init-time -- no dynamic update
            mTitleVisible = !isBooleanTrue(widget, TITLE_HIDDEN_KEY);
            
            Object expanded = widget.getClientProperty(EXPANSION_KEY);
            if (expanded != null) {
                propertyChange(new PropertyChangeEvent(this, EXPANSION_KEY, null, expanded));
            } else {
                // expanded by default
                setBoolean(widget, EXPANSION_KEY, true);
                handleWidgetDisplayChange(true); 
            }
                
            Object hidden = widget.getClientProperty(Widget.HIDDEN_KEY);
            if (hidden != null) {
                propertyChange(new PropertyChangeEvent(this, HIDDEN_KEY, null, hidden));
            } else {
                // not hidden by default
                setBoolean(widget, HIDDEN_KEY, false);
            }

            widget.addPropertyChangeListener(this);
            
        }

        public void addNotify() {
            super.addNotify();
            mEmbeddedStack = (SwingUtilities.getAncestorOfClass(WidgetStack.class, getParent()) != null);
        }

        private void setLocked(boolean locked) {
            if (isLocked == locked)
                return;
            isLocked = locked;
            if (locked) {
                mIcon.setForeground(mTopColor.darker());
                mLockedWidget = this;
            } else {
                mIcon.setForeground(Color.white);
                mLockedWidget = null;
            }
        }

        private void handleMouseClicked() {
            if (isLocked)
                return;
            Widget.setExpanded(mWidget, !mExpanded);
        }

        /** interface java.beans.PropertyChangeListener for contained component */
        public void propertyChange(java.beans.PropertyChangeEvent e) {
            final String key = e.getPropertyName();
        
            if (DEBUG.WIDGET && (true || !key.equals("ancestor")))
                out(GUI.name(e.getSource()) + " property \"" + key + "\" newValue=[" + GUI.name(e.getNewValue()) + "]");

            if (key == EXPANSION_KEY) {
                setWidgetExpanded( ((Boolean)e.getNewValue()).booleanValue() );
                
            } else if (key == HIDDEN_KEY) {
                setWidgetHidden( ((Boolean)e.getNewValue()).booleanValue() );

            } else if (key == MENU_ACTIONS_KEY) {
                mMenuButton.setMenuActions((Action[]) e.getNewValue());
                
            } else if (key == REFRESH_ACTION_KEY) {
                refreshButton.setAction((MouseListener)e.getNewValue());
                
            } else if (key == HELP_ACTION_KEY) {                
                helpButton.setHelpText((String)e.getNewValue());
            } else if (key.equals("name")) {
                mTitle.setText((String) e.getNewValue());
                
            } else if (key.endsWith("Size")) {

                Component src = (Component) e.getSource();
            
                if (DEBUG.WIDGET) GUI.dumpSizes(src);
                if (true||DEBUG.WIDGET) out("revalidate on size property change");
                revalidate();
                if (DEBUG.WIDGET) GUI.dumpSizes(src);
            }
        }

        /**
         * This method only does something if isExpander is true: track how many
         * expanding Widgets (in the GridBagLayout) are visible, beacuse when we get
         * down to 0, we need to add a default expander to take up the remaining space.
         */
        private void handleWidgetDisplayChange(boolean visible) {
            if (!isExpander)
                return;

            if (visible)
                mExpandersOpen++;
            else
                mExpandersOpen--;

            if (DEBUG.WIDGET) out("VISIBLE EXPANDER COUNT: " + mExpandersOpen);
            
            if (mExpandersOpen < 0 || mExpandersOpen > mExpanderCount)
                throw new IllegalStateException("WidgetStack: expanders claimed open: "
                                                + mExpandersOpen
                                                + ", expander count=" + mExpanderCount);

            // Could do: if only one widget open, change constraints on THAT guy to
            // expand...  Or, as soon as all expanders close, set remaining ones to
            // expand equally?  That could just get ugly tho (titles keep moving down
            // out from under your mouse -- this currently happens only on our last item
            // in the stack which isn't so bad the way we're using it, but for
            // everything?) Really need second tier expander marks for stuff
            // w/scroll-panes (use negative expansion weights?)  Subclassing
            // GridBagLayout might make this easier also.

            // new: if last expander open, and we close it,
            // open the next visible expander below, or if none, the
            // one above, or none, don't allow this to collapse.

            WidgetStack.this.updateDefaultExpander();
                
        }

        private void updateLockingStateLater() {
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                updateLockingState();
            }});
        }

        private void setWidgetHidden(boolean hide) {
            if (DEBUG.WIDGET) out("setWidgetHidden " + hide);

            if (mTitleVisible && isVisible() == !hide)
                return;

            setVisible(mTitleVisible && !hide);
            
            if (hide) {
                if (mExpanded) {
                    mWidget.setVisible(false);
                    handleWidgetDisplayChange(false);
                }
            } else if (mExpanded) {
                mWidget.setVisible(true);
                handleWidgetDisplayChange(true);
            }

            updateLockingStateLater();
        }
        

        private void setWidgetExpanded(boolean expanded) {
            if (DEBUG.WIDGET) out("setWidgetExpanded " + expanded);
            if (mExpanded == expanded)
                return;
            mExpanded = expanded;
            if (mExpanded) {
                mIcon.setText("" + DownArrowChar);
            } else {
                mIcon.setText("" + RightArrowChar);
            }

            handleWidgetDisplayChange(mExpanded);

            GridBagConstraints c = mLayout.getConstraints(this);
            c.insets = expanded ? ExpandedTitleBarInsets : CollapsedTitleBarInsets;
            mLayout.setConstraints(this, c);

            mWidget.setVisible(expanded);

            revalidate();
            
            updateLockingStateLater();
            
        }

        public void paint(Graphics g) {
            if (!isMac) {
                // this is on by default on the mac
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
            super.paint(g);
        }

        public void paintComponent(Graphics g) {
            paintGradient((Graphics2D)g);
            super.paintComponent(g);
        }

        private void paintGradient(Graphics2D g)
        {
            if (false && mEmbeddedStack)
                g.setPaint(GradientEmbedded);
            else
                g.setPaint(mGradient);
            g.fillRect(0, 0, getWidth(), TitleHeight);
        }


        private void out(Object o) {
            System.err.println(GUI.name(this) + " " + (o==null?"null":o.toString()));
        }

    
    }
    
    private void out(Object o) {
        System.err.println(GUI.name(this) + " " + (o==null?"null":o.toString()));
    }

    static class MenuButton extends JButton {
        private static final Color defaultColor = GUI.isMacAqua() ? Color.white : Color.black;
        private static final Color activeColor = GUI.isMacAqua() ? TopGradient.brighter() : Color.white;

        MenuButton(Action[] actions) {
            //super(iconChar, 18, defaultColor, TitleHeight, TitleHeight);
        	super();
        	setIcon(VueResources.getImageIcon("dockWindow.menuIcon.raw"));
        	setRolloverEnabled(true);
        	setRolloverIcon(VueResources.getImageIcon("dockWindow.menuIcon.hover"));
        	Insets noInsets=new Insets(5,0,0,0);
        	//store the icon you want to display in imageIcon		        		
        	setMargin(noInsets);			
        //	setBorder(BorderFactory.createEmptyBorder());
        	setContentAreaFilled(false);
            //setAlignmentY(0.5f);
            // todo: to keep manually picking a height and a bottom pad to get this
            // middle aligned is no good: will eventually want to use a TextLayout to
            // get precise bounds for center, and create as a real Icon
            setBorder(new javax.swing.border.EmptyBorder(3,3,3,3));
            setMenuActions(actions);
            
        }

        void setMenuActions(Action[] actions) {
            clearMenuActions();

            if (actions == null) {
                setVisible(false);
                return;
            }
            setVisible(true);

            new GUI.PopupMenuHandler(this, GUI.buildMenu(actions)) {
                public void mouseEntered(MouseEvent e) { setForeground(activeColor); }
                public void mouseExited(MouseEvent e) { setForeground(defaultColor); }
                public int getMenuX(Component c) { return c.getWidth(); }
                public int getMenuY(Component c) { return -getY(); } // 0 in parent
            };

            repaint();
        }

        private void clearMenuActions() {
            MouseListener[] ml = getMouseListeners();
            for (int i = 0; i < ml.length; i++) {
                if (ml[i] instanceof GUI.PopupMenuHandler)
                    removeMouseListener(ml[i]);
            }
        }

        
    }

    static class HelpButton extends VueLabel implements MouseListener {
        

        HelpButton(Action[] actions) {
            //super(iconChar, 18, defaultColor, TitleHeight, TitleHeight);
        	super();
        	setIcon(VueResources.getImageIcon("dockWindow.helpIcon.raw"));
        	this.setFocusable(true);
        	Insets noInsets=new Insets(5,0,0,0);

        	// todo: to keep manually picking a height and a bottom pad to get this
            // middle aligned is no good: will eventually want to use a TextLayout to
            // get precise bounds for center, and create as a real Icon
        	addMouseListener(this);
            setBorder(new javax.swing.border.EmptyBorder(3,3,3,3));
            setHelpText(null);
        }    
        
        public void setHelpText(String text) 
        {            
            if (text == null) {
            	this.setToolTipText(null);
                setVisible(false);
                return;
            }
            else
            {
            	this.setToolTipText(text);
            	setVisible(true);
            }
        }        
        
        public void mouseClicked(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		public void mousePressed(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		public void mouseReleased(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		public void mouseEntered(MouseEvent arg0) {			
			setIcon(VueResources.getImageIcon("dockWindow.helpIcon.hover"));
			repaint();
			
		}

		public void mouseExited(MouseEvent arg0) {
			setIcon(VueResources.getImageIcon("dockWindow.helpIcon.raw"));
			repaint();
			
		}                

    }
    static class RefreshButton extends JLabel implements MouseListener{
    	
    	private String iconChar;
    	
        RefreshButton(String icon, MouseListener action) {
        	super();
        	iconChar = icon;
        	setIcon(VueResources.getImageIcon(iconChar+".raw"));        	     
        	Insets noInsets=new Insets(0,0,0,0);
            setAlignmentY(0.5f);
            setBorder(new javax.swing.border.EmptyBorder(0,0,3,0));
            addMouseListener(this);
            setAction(action);           
            
        }

        public void setAction(MouseListener action) 
        {
            clearAction();

            if (action == null) {
                setVisible(false);
                return;
            }
            else
            {            	
            	addMouseListener(this);
            	addMouseListener(action);
            	setVisible(true);
            }

        }

        private void clearAction() {
            MouseListener[] ml = getMouseListeners();
            for (int i = 0; i < ml.length; i++) {                
                    removeMouseListener(ml[i]);
            }
        }

		public void mouseClicked(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		public void mousePressed(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		public void mouseReleased(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		public void mouseEntered(MouseEvent arg0) {			
			setIcon(VueResources.getImageIcon(iconChar+".hover"));
			
		}

		public void mouseExited(MouseEvent arg0) {
			setIcon(VueResources.getImageIcon(iconChar+".raw"));
			
		}                
    }

    public static void main(String args[])
    {
        // todo: appears to be a bug in GridBagLayout where if ALL
        // components are expanders, in can sometimes add a pixel
        // at the top of the freakin layout.  This example
        // was all weights of 1.0.  The pixel can come in and
        // out even during resizes: some sizes just trigger it...
        // Okay, even if NOT all are expanders it can fail.
        // Christ.  Yet our ResourcePanel stack and DRBrowser
        // stack work fine... Okay, thoes have only ONE expander...
        
        tufts.vue.VUE.init(args);
        
        WidgetStack s = new WidgetStack("Test");

        String[] names = new String[] { "One",
                                        "contentPreview",
                                        "contentInfo",
                                        "Four",
        };

        for (int i = 0; i < names.length; i++) {
            s.addPane(names[i], new JLabel(names[i], SwingConstants.CENTER), 1f);
        }
        //s.addPane("Fixed", new JLabel("fixed"), 0f);

        GUI.createDockWindow("WidgetStack Test", s).setVisible(true);
    }
        
}    

