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

import static tufts.vue.VueConstants.*;
import static tufts.vue.LWPathway.Entry;
import static tufts.vue.MapViewer.*;

import tufts.Util;
import tufts.macosx.MacOSX;
import tufts.vue.gui.GUI;
import tufts.vue.gui.TextRow;

import java.util.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.*;
import javax.swing.*;

/**
 * Tool for presentation mode.
 *
 * Handles presentation mode key commands and screen decorations.
 *
 * @version $Revision: 1.418 $ / $Date: 2007/05/06 20:14:17 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */
public class PresentationTool extends VueTool
    implements LWComponent.Listener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(PresentationTool.class);
    
    private static final boolean AnimateAcrossMap = false;
    private static final boolean AnimateInOutMap = false;
    private static final boolean AnimateTransitions = AnimateAcrossMap || AnimateInOutMap;
    
    private static final String FORWARD = "FORWARD";
    private static final String BACKWARD = "BACKWARD";
    private static final boolean RECORD_BACKUP = true;
    private static final boolean BACKING_UP = false;

    private final ImageButton ExitButton = new ImageButton(VueResources.getImageIcon("pathwayTool.exitImageIcon")) {
            void doAction() {
                if (VUE.inFullScreen())
                    VUE.toggleFullScreen(false, true);
                VUE.setActive(VueTool.class, this, VueTool.getInstance(SelectionTool.class));
            }
            void trackVisible(MouseEvent e) {
                // set us visible if the given mouse even is between us and the lower right hand corner of the screen
                setVisible(e.getX() < (x+width) && e.getY() > y);
            }
            
        };
    
    private JButton mStartButton;
    private final JCheckBox mShowContext = new JCheckBox("Show Context");
    private final JCheckBox mToBlack = new JCheckBox("Black");
    private final JCheckBox mZoomLock = new JCheckBox("Lock 100%");
    
    private final Page NO_PAGE = new Page((LWComponent)null);
    
    private volatile Page mCurrentPage = NO_PAGE;
    private volatile Page mLastPage = NO_PAGE;
    private volatile LWPathway mPathway;
    private volatile Page mLastPathwayPage;
    private volatile LWComponent mFocal; // sync'd with MapViewer focal
    
    private volatile LWComponent mNextPage; // is this really "startPage"?
    //private LWLink mLastFollowed;
    //private int mPathwayIndex = 0;
    //private LWPathway.Entry mEntry;

    private static volatile boolean
        mFadeEffect = true,
        mShowNavigator = DEBUG.NAV,
        mShowNavNodes,
        mForceShowNavNodes,
        mDidAutoShowNavNodes,
        mScreenBlanked;

    private class ImageButton {

        final ImageIcon icon;
        final int width, height;

        int x, y;
        boolean visible;

        ImageButton(ImageIcon icon) {
            this.icon = icon;
            width = icon.getIconWidth();
            height = icon.getIconHeight();
        }

        boolean isVisible() { return visible; }

        /**
         * set us visible if the given mouse (motion) event is somewhere on the screen
         * such that we want to appear.  Default always makes the button visible.
         */
        void trackVisible(MouseEvent e) {
            setVisible(true);
        }
        
        void setVisible(boolean t) {
            if (visible != t) {
                visible = t;
                PresentationTool.this.repaint();
            }
        }

        void setLocation(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void draw(DrawContext dc) {
            dc.g.drawImage(icon.getImage(), x, y, null);
        }

        boolean contains(MouseEvent e) {
            return visible ? contains(e.getX(), e.getY()) : false;
        }
        
        boolean contains(int x, int y) {

            if (!visible)
                return false;
            
            return x >= this.x
                && y >= this.y
                && x <= this.x + width
                && y <= this.y + height;
        }

        void doAction() {}
    }

    /** a presentation moment (data for producing a single presentation screen) */
    private static class Page {
        final LWPathway.Entry entry;
        final LWComponent node;
        
        Page(LWPathway.Entry e) {
            entry = e;
            node = null;
        }
        Page(LWComponent c) {
            if (c instanceof LWSlide) {
                if (((LWSlide)c).getPathwayEntry() != null) {
                    entry = ((LWSlide)c).getPathwayEntry();
                    node = null;
                } else {
                    if (DEBUG.Enabled) Util.printStackTrace("creating page for slide w/out entry: " + c);
                    entry = null;
                    node = c;
                }
            } else {
                entry = null;
                node = c;
            }
        }

        /** @return the object we're actually going to draw on screen for this presentation moment */
        public LWComponent getPresentationFocal() {
            return entry == null ? node : entry.getFocal();
        }


        /** @return the map node that is the source of this presentation item.
         * May return null if the this is a pathway entry that was a combo slide.
         */
        public LWComponent getOriginalMapNode() {
            if (node != null)
                return node;
            else if (entry != null)
                return entry.node;
            return null;
        }

        public boolean isMapView() {
            if (entry == null)
                return node != null && node.getAncestorOfType(LWSlide.class) == null;
            else
                return entry.isMapView();
        }

        /** @return true if this is a map-view node that's on a pathway */
        public boolean isMapViewNode() {
            if (entry == null)
                return true; // changed 2007-10-22
              //return false; 
            else
                return entry.isMapView();
        }
        
        public boolean onPathway() {
            return entry != null;
        }

        public LWPathway pathway() {
            return entry == null ? null : entry.pathway;
        }

        /** @return true of this page is directly on the given pathway */
        public boolean onPathway(LWPathway p) {
            if (p == null)
                return false;
            else if (node != null)
                return node.inPathway(p);
            else if (entry != null)
                return entry.pathway == p;
            else
                return false;
        }
        
        /** @return true of this page is inside something on the current
         * pathway (e.g., inside a slide on the pathway).  Items ON the current pathway are excluded. */
        public boolean inPathway(LWPathway p) {
            if (p == null)
                return false;
            else if (node != null && !node.inPathway(p)) {
                // If the node for this page is embedded in a slide, and that slide
                // is in the pathway, consider us in-pathway.  We actually may NOT
                // want to do this, as it's a bit complicating...
                // TODO: handle map-nodes (groups) -- technically, we want
                // to check every ancestor for the path membership...
                final LWSlide slide = (LWSlide) node.getAncestorOfType(LWSlide.class);
                return slide != null && slide.inPathway(p);
            } else
                return false;
        }

        public boolean insideSlide() {
            return node != null && node.getParentOfType(LWSlide.class) != null;
        }

        public String getDisplayLabel() {
            if (node != null)
                return node.getDisplayLabel();
            else if (entry != null)
                return entry.getLabel();
            else {
                Log.warn("Page with no node or entry");
                return "<NO-NODE-OR-ENTRY!>";
            }
        }

        public String toString() {
            //String s = "PAGE: ";
            String s = "";
            if (node != null)
                s += node;
            else 
                s += entry;
            return s;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o instanceof Page) {
                final Page other = (Page) o;

                return entry == other.entry && node == other.node;
                           
                //return (entry != null && entry == other.entry) || (node != null && node == other.node);
                
//                 if (entry == other.entry && node == other.node)
//                     return true;
//                 else
//                     return node == other.node && isMapView() == other.entry.isMapView();
                
            } else if (o instanceof LWComponent) {
                final LWComponent c= (LWComponent) o;
                return node == c || (entry != null && entry.node == c);
            } else
                return false;
        }
    }

    /**

     * Implements a stack/queue that works like standard web browser forward/back page
     * visit lists: You can always go back in a straight line, and you can keep going
     * forward in a straight line until you push something else on the stack (browse off
     * the current already visited list), in which case everything forward of where you
     * are in the list is throw away.
     
     */
    private static class PageQueue<T> extends ArrayList<T>
    {
        int index;

        public boolean isEmpty() {
            // somehow, ArrayList is getting to a size == -1 condition,
            // and the default isEmpty checks size == 0, so it doesn't
            // think it's empty in that case!
            return size() <= 0;
        }
         	
        public void rollBack() {
            if (index > 0)
                index--;
            else
                throw new IndexOutOfBoundsException("index=" + index);
        }
        public void rollForward() {
            if (index < lastIndex())
                index++;
            else
                throw new IndexOutOfBoundsException("index=" + index + " lastIndex=" + lastIndex());
        }
        
        private final int lastIndex() {
            return size() - 1;
        }
        
        public T first() {
            return size() <= 0 ? null : get(0);
        }
        public T last() {
            return size() <= 0 ? null : get(lastIndex());
        }

        public T jumpFirst() {
            index = 0;
            return first();
        }
        public T jumpLast() {
            index = lastIndex();
            return last();
        }
        
        
        public void push(T o) {
            // never allow null in the stack, or the same item repeated on the top of the stack
            if (o == null)
                return;

            if (o.equals(next())) {
                rollForward();
            } else if (o.equals(current())) {
                ; // do nothing: never repeat items in the queue
            } else {
                if (!isEmpty() && index < lastIndex()) {
                    // toss out forward stack information whenever we push anything:
                    removeRange(index+1, size());
                }
                add(o);
                index = size() - 1;
            } 
        }
        
        public T current() {
            return isEmpty() ? null : get(index);
        }
        
        public boolean hasPrev() {
            return !isEmpty() && index > 0;
        }

        public boolean hasNext() {
            return index < lastIndex();
        }
        
        /**
         * @return the next item back in the queue without changing the queue position.
         * Will return null if hasPrev() returns false.
         */
        public T prev() {
            return hasPrev() ? get(index - 1) : null;
        }

        public T next() {
            return hasNext() ? get(index + 1) : null;
        }

        
        /** move the queue position back one, and return what's there */
        public T popPrev() {
            if (!hasPrev())
                throw new NullPointerException(this + " empty; can't pop");
            return get(--index);
        }

        public T popNext() {
            if (!hasNext())
                throw new NullPointerException(this + "index="+index + "; size="+size() + "; already at end");
            return get(++index);
        }
    }

    
    private static PresentationTool singleton;
    
    private final PageQueue<Page> mVisited = new PageQueue();
    
    private final List<NavNode> mNavNodes = new java.util.ArrayList();

    private static final Font NavFont = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font NavFontBold = new Font("SansSerif", Font.BOLD, 16);
    private static final Font NavFontDebug = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font NavFontBoldDebug = new Font("SansSerif", Font.BOLD, 10);
    private static final Font NavBoxFont = new Font("SansSerif", Font.PLAIN, 12);
    private static final Color NavFillColor = new Color(154,154,154);
    //private static final Color NavFillColor = new Color(64,64,64,96);
    private static final Color NavStrokeColor = new Color(96,93,93);
    //private static final Color NavTextColor = Color.darkGray;
    private static final Color NavTextColor = Color.black;

    private static final int SlideRoom = 30;
    
    private static final int BoxSize = 20;
    private static final int BoxGap = 5;

    private final class NavNode extends LWNode
    {
        static final int DefaultWidth = 200;
        static final int ActiveWidth = 300;
        static final int MaxWidth = ActiveWidth;
        
        final Page page; // destination page
        final String type; // for debug
        final boolean isOffEdge;
        final List<JumpBox> mJumpBoxes;

        /**
         * A sub-NavNode hit region for jumping to another view of the given node.
         * Generally this is a view of the node on another pathway, but also
         * includes support for the special case of the off-pathway, in-map
         * pure node.
         */
        private final class JumpBox extends java.awt.geom.RoundRectangle2D.Float
        {
            final LWPathway.Entry entry;
            final TextRow row;
            final Color color;
            
            JumpBox(LWPathway.Entry e, float x, float y) 
            {
                super(x, y, BoxSize, BoxSize, 5, 5);
                entry = e;
                if (entry != null) {
                    if (DEBUG.Enabled) {
                        String text = Integer.toString(entry.index() + 1); // index's are zero-based, so add 1
                        //if (entry.isLast()) text = "(" + text + ")";
                        row = TextRow.instance(text, NavBoxFont);
                    } else
                        row = null;
                    color = Util.alphaMix(entry.pathway.getColor(), Color.white);
                } else {
                    color = null;
                    row = null;
                }
                
            }

            Page getPage() {
                return entry == null ? new Page(page.getOriginalMapNode()) : new Page(entry);
            }
            
            
            void draw(DrawContext dc) 
            {
                final Color faint;
                
                if (color != null) {
                    dc.g.setColor(color);
                    dc.g.fill(this);
                    faint = color.darker();
                } else
                    faint = null;
                
                if (DEBUG.Enabled && entry != null && entry.isLast()) {
                    int left = (int) Math.round(super.x) + 4;
                    int right = (int) Math.round(super.x + super.width) - 4;
                    int bottom = (int) Math.round(super.y + super.height) - 4;
                    dc.g.setStroke(STROKE_ONE);
                    dc.g.setColor(faint);
                    dc.g.drawLine(left, bottom, right, bottom);
                }
                
                if (entry == null) {
                    dc.g.setStroke(page.entry == null ? STROKE_THREE : STROKE_ONE);
                } else if (page.entry != null && page.entry.pathway == entry.pathway)
                    dc.g.setStroke(STROKE_THREE);
                else
                    dc.g.setStroke(STROKE_ONE);
                dc.g.setColor(Color.darkGray);
                dc.g.draw(this);
                if (row != null) {
                    dc.g.setFont(NavBoxFont);
                    dc.g.setColor(faint);
                    row.drawCenter(dc, this);
                }
                
            }
            
            public String toString() {
                return "Box[" + entry.toString() + "]";
            }
        
        }
    
    
        private List<JumpBox> createPathwayJumpBoxes(LWComponent node)
        {
            final List<LWPathway.Entry> entries = node.getEntries();
            final List<JumpBox> boxes = new ArrayList(entries.size());

            //float x = getWidth() - (BoxSize + BoxGap) * entries.size();
            float x = getWidth() - (BoxSize + BoxGap + 5);

            if (isOffEdge)
                x += (NavLayout.InsetOffEdge - NavLayout.InsetIndent);

            final float y = (getHeight() - BoxSize) / 2f;

            boxes.add(new JumpBox(null, x, y)); // add the node-only pathway box
            x -= BoxSize + BoxGap;
            
            for (ListIterator<Entry> i = entries.listIterator(entries.size()); i.hasPrevious();) {
                final Entry e = i.previous();
                if (e.pathway.isDrawn()) {
                    boxes.add(new JumpBox(e, x, y));
                    x -= BoxSize + BoxGap;
                }
            }

            return boxes;

        }
        
        NavNode(Page destinationPage, boolean isOffEdge, boolean isLastPathwayPage, String type)
        {
            super(null);

            if (destinationPage == null)
                throw new IllegalArgumentException(getClass() + "; destination page is null");
            
            this.page = destinationPage;
            this.isOffEdge = isOffEdge;
            this.type = type;

            //if (DEBUG.WORK) out("new NavNode for page " + destinationPage + "; isLastPathway=" + isLastPathwayPage);


            final LWPathway pathway = null;
            String label = page.getDisplayLabel();
            
//             final LWPathway pathway;
//             if (destinationPage.entry != null)
//                 pathway = destinationPage.entry.pathway;
//             else
//                 pathway = null;
//             String label;
//             if (pathway != null)
//                 label = pathway.getLabel();
//             else
//                 label = page.getDisplayLabel();
//             if (pathway == null && destinationPage.equals(mVisited.prev()))
//                 label = "<- " + label;
            
            
            if (label.length() > 20)
                label = label.substring(0,18) + "...";
            
            //label = "    " + label + " ";
            setLabel(label);
            if (DEBUG.PRESENT)
                setFont(isLastPathwayPage ? NavFontBoldDebug : NavFontDebug);
            else
                setFont(isLastPathwayPage ? NavFontBold : NavFont);
            setTextColor(NavTextColor);
        
            if (false && pathway != null) {
                // Nav node for jumping to another pathway
                //final Color color = Util.alphaMix(pathway.getColor(), pathway.getMasterSlide().getFillColor());
                final Color color = Util.alphaMix(pathway.getColor(), Color.gray);
                //final Color color = pathway.getColor();
                mFillColor.setFixedAlpha(224);
                setFillColor(color);
                setTextColor(Color.black);
                setStrokeWidth(0);
                //setStrokeColor(color);
                //setStrokeWidth(3);
            } else {
                setStrokeColor(NavStrokeColor);
                setFillColor(NavFillColor);
                setStrokeWidth(1);
            }
            
            //setSyncSource(src); // TODO: these will never get GC'd, and will be updating for ever based on their source...
            //setShape(new RoundRectangle2D.Float(0,0, 10,10, 20,20)); // is default
            setAutoSized(false);

            int buttonWidth = isLastPathwayPage ? ActiveWidth : DefaultWidth;
            
            setSize(buttonWidth + SlideRoom, getHeight() + 6);

            final LWComponent node = page.getOriginalMapNode();
            // pull node from entry if node is null?
            if (node == null) {
                //Log.warn("NULL NODE in " + this);
                Util.printStackTrace("NULL NODE in " + this);
                mJumpBoxes = null;
            } else if (node.inPathway()) {
                mJumpBoxes = createPathwayJumpBoxes(node);
            } else {
                mJumpBoxes = null;
            }
            
        }

        Page hitPage(float x, float y) 
        {
            if (containsLocalCoord(x, y)) {
                if (mJumpBoxes != null) {
                    final float localX = x - getX();
                    final float localY = y - getY();
                    for (JumpBox box : mJumpBoxes) {
                        if (box.contains(localX, localY)) {
                            if (DEBUG.PRESENT || DEBUG.PICK) Log.debug("hit entry box " + box);
                            return box.getPage();
                        }
                    }
                }
                return this.page;
            } else
                return null;
        }
        

        @Override
        public void draw(DrawContext dc) {
            dc.setClipOptimized(false); // ensure all children draw even if not inside clip
            transformZero(dc.g);

            final LWComponent node = page.getOriginalMapNode();

            //if (node.inPathway()) {
            if (mJumpBoxes != null) {
                drawZero(dc.push()); dc.pop();
                drawPathwayJumpBoxes(dc);
                //drawPathwaySwatches(dc, node);
            } else {
                drawZero(dc);
            }

            if (DEBUG.Enabled) {
                dc.g.setFont(FONT_TINY);
                dc.g.setColor(Color.white);
                if (page.entry != null) {
                    dc.g.drawString("Entry: " + page.entry, -100, 0);
                    dc.g.drawString("Node: " + page.node, -100, 10);
                } else {
                    dc.g.drawString("Node: " + page.node, -100, 0);
                    dc.g.drawString("Entry: " + page.entry, -100, 10);
                }
                           
                dc.g.drawString("Type: " + type, -100, 20);
                    
            }
            
//             // Draw slide icon if there is one:
//             if (page.entry != null && page.entry.hasSlide()) {
//                 drawSlideIcon(dc);
//             }
        }


        private void drawPathwayJumpBoxes(DrawContext dc)  
        {
            for (JumpBox box : mJumpBoxes)
                box.draw(dc);
        }

//         private void drawPathwaySwatches(DrawContext dc, LWComponent node)  
//         {
//             final List<LWPathway> pathways = node.getPathways();

//             float x = getWidth() - (BoxSize + BoxGap) * pathways.size();

//             final float y = (getHeight() - BoxSize) / 2f;

//             x -= 10;
//             dc.g.setStroke(STROKE_ONE);
//             for (LWPathway p : pathways) {
//                 dc.g.setColor(Util.alphaMix(p.getColor(), Color.white));
//                 //dc.g.setColor(p.getColor());
//                 dc.g.fillRect((int)x, (int)y, BoxSize, BoxSize);
//                 dc.g.setColor(Color.darkGray);
//                 dc.g.drawRect((int)x, (int)y, BoxSize, BoxSize);
//                 x += BoxSize + BoxGap;
//             }
            
//         }
        
        private void drawSlideIcon(DrawContext dc) {
            final LWSlide slide = page.entry.getSlide();
            final double scale = (getHeight()-10) / slide.getHeight();
            final double iconHeight = slide.getHeight() * scale;
            //final double iconWidth = slide.getWidth() * scale;
            dc.g.translate(getWidth() - (SlideRoom+12),
                           (getHeight() - iconHeight)/2);
            dc.g.scale(scale, scale);
            dc.setBackgroundFill(null); // so if current slide is on screen, will still fill
            slide.drawZero(dc);
            //dc.g.setColor(Color.black);
            //dc.g.drawString("HELLO", 0, 0);
        }

        @Override
        public void setLocation(float x, float y) {
            super.takeLocation(x, y);
        }
        
        // force label at left (non-centered)                
        @Override
        protected final float relativeLabelX() { return -NavNodeX + 10; }

    }

    

    public PresentationTool() {
        super();
        if (singleton != null) 
            new Throwable("Warning: mulitple instances of " + this).printStackTrace();
        singleton = this;
        VueToolUtils.setToolProperties(this,"viewTool");	
        VUE.addActiveListener(LWPathway.Entry.class, this);
    }
    
    public void activeChanged(ActiveEvent e, LWPathway.Entry entry) {
        if (isActive()) {
            // only do this if this is the active tool,
            // as we use the globally active viewer
            // when we change the page!
            if (!entry.isPathway())
                setEntry(entry, BACKING_UP);
        }
     }
    
    @Override
    public JPanel createToolPanel() {
        //JPanel p = super.createToolPanel();
        JPanel p = new JPanel();
        mStartButton = new JButton("Start Presenting");
        mToBlack.setSelected(false);
        mShowContext.setSelected(true);
        add(p, mStartButton);
        //add(p, mShowContext);
        //add(p, mToBlack);
        //p.add(mZoomLock, 0);
        return p;
    }

    /** return the singleton instance of this class */
    public static PresentationTool getTool()
    {
        if (singleton == null) {
           // new Throwable("Warning: PresentationTool.getTool: class not initialized by VUE").printStackTrace();
            //throw new IllegalStateException("NodeTool.getTool: class not initialized by VUE");
            new PresentationTool();
        }
        return singleton;
    }
    
    private void add(JPanel p, AbstractButton c) {
        c.setFocusable(false);
        c.setFont(VueConstants.FONT_SMALL);
        if (c instanceof JCheckBox)
            c.setContentAreaFilled(false);
        p.add(c, 0);
        c.addActionListener(this);
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == mStartButton) {
            startPresentation();
            VUE.toggleFullScreen(true);
        } else if (ae.getSource() instanceof JCheckBox) {
            repaint("checkBox");
        } else
            super.actionPerformed(ae);
    }

    private void repaint() {
        repaint("");
    }
    
    private void repaint(String msg) {
        if (DEBUG.WORK) out("repaint " + msg);
        VUE.getActiveViewer().repaint();
    }

    
    private boolean onCurrentPathway() {
        final Page p = mCurrentPage;
        if (p == null || p == NO_PAGE || mPathway == null)
            return false;
        else
            return p.onPathway(mPathway);

        // This appears to be checking if we've descended into the contents of a pathway page...
        //|| (p.node != null && page.node.getParent() == mPathway); // page.node != entry.node !!
    }

    private boolean inCurrentPathway() {
        final Page page = mCurrentPage;
        if (page == null || page == NO_PAGE || mPathway == null)
            return false;
        else
            return page.inPathway(mPathway);
    }
    

    private void revisitPrior() { revisitPrior(false); }
    private void revisitPrior(boolean allTheWay) {
        if (DEBUG.PRESENT) out(String.format("revisitPrior%s", allTheWay ? " (AllTheWay)" : ""));
        
        if (allTheWay)
            setPage(mVisited.jumpFirst());
        else if (mVisited.hasPrev())
            setPage(mVisited.popPrev(), BACKING_UP);
    }
    
    private void revisitNext() { revisitNext(false); }
    private void revisitNext(boolean allTheWay) {
        if (allTheWay)
            setPage(mVisited.jumpLast());
        else if (mVisited.hasNext())
            setPage(mVisited.popNext(), BACKING_UP);
    }
    
    private void goBackward(boolean allTheWay)
    {
        if (onCurrentPathway()) {
            if (allTheWay) {
                // todo: skip to start of queue if been there instead
                // adding more to the end!
                setEntry(mPathway.getFirst());
            } else {
                setEntry(nextPathwayEntry(BACKWARD), BACKING_UP);
            }
        } else {
            revisitPrior(allTheWay);
        }
    }

    private static final boolean SINGLE_STEP = false;
    private static final boolean GUESSING = true;

    private void goForward(boolean allTheWay) { goForward(allTheWay, false); }
    private void goForward(boolean allTheWay, boolean guessing)
    {
        if (mCurrentPage == NO_PAGE && mNextPage == null) {
            
            startPresentation();
            
        } else if (onCurrentPathway()) {
            if (false && allTheWay) {
                // todo: skip to end of queue if been here instead
                // of blowing it away!
                // skipping far forward complicates the hell
                // out of maintaining a sane seeming backlist...
                setEntry(mPathway.getLast());
            } else {
                setEntry(nextPathwayEntry(FORWARD));
            }
        } else {
            if (guessing) {

                if (false && inCurrentPathway()) {
                    // This too hairy for now: we're attempting to allow "space"
                    // to auto-pop back up to a slide whose contents we've delved into,
                    // tho we really need to be checking the back-list to make sure
                    // the prior member is also either inside the current pathway,
                    // or is the item on the pathway itself.
                    revisitPrior();
                } else if (!mForceShowNavNodes) {
                    // if we hit space and there's nowhere to go, at leasts how them their options:
                    mForceShowNavNodes = mShowNavNodes = true;
                    mDidAutoShowNavNodes = true;
                    repaint("guessForward");
                }
            } else
                revisitNext(allTheWay); 
        }

    }

    @Override
    public PickContext initPick(PickContext pc, float x, float y) {
        // allow picking of node icons, group contents, etc as long as not a at top level map
        if (mFocal instanceof LWMap == false)
            pc.pickDepth = 1;
        return pc;
    }
    
    @Override
    public boolean handleKeyPressed(java.awt.event.KeyEvent e) {
        //out("handleKeyPressed " + e);
        final int keyCode = e.getKeyCode();
        final char keyChar = e.getKeyChar();

        boolean handled = true;

        final boolean amplified = e.isShiftDown();

        switch (keyCode) {
        case KeyEvent.VK_ENTER:

            // [2007-10-22:this comment still current? ]
            // MapViewer popFocal must have failed -- focal should be the map:
            // todo: all our VueTool handled calls that come from the MapViewer
            // should take a viewer as an argument...
            
            if (mFocal instanceof LWMap && mLastPage != null) {
                setPage(mLastPage);
            } else if (mVisited.hasPrev()) {
                setPage(mVisited.prev());
            } else if (mLastPathwayPage != null) {
                setPage(mLastPathwayPage);
            } else {
                // non-pathway navigation: just zoom back out to entire map
                VUE.getActiveViewer().fitToFocal(); // TODO: viewer should be passed in..
                // TODO: want to animate zoom!
                // Really need that Page object, probably at the MapViewer level,
                // that knows if we've EVER been zoomed and/or panned off the main focal,
                // so that we could "reload" the focal by zoom-fitting to it even
                // if it's the same focal
            }
            break;

        case KeyEvent.VK_SPACE:
            if (mLastPathwayPage != null && mCurrentPage.node instanceof LWMap)
                setPage(mLastPathwayPage);
            else
                goForward(SINGLE_STEP, GUESSING);
            break;            

        case KeyEvent.VK_BACK_QUOTE:
            // toggle showing the non-linear nav options:
            //mForceShowNavNodes = mShowNavNodes = !mForceShowNavNodes;
            mForceShowNavNodes = mShowNavNodes = !mShowNavNodes;
            repaint("toggleNav="+mShowNavNodes);
            break;
            
        case KeyEvent.VK_RIGHT:
            goForward(amplified);
            break;
        case KeyEvent.VK_LEFT:
            goBackward(amplified);
            break;
        case KeyEvent.VK_UP:
            if (DEBUG.Enabled) 
                revisitPrior(amplified);
            else
                handled = false;
            break;
        case KeyEvent.VK_DOWN:
            if (DEBUG.Enabled) {
                if (mVisited.hasNext())
                    revisitNext(amplified);
                else
                    goForward(amplified);
            } else
                handled = false;
            break;
            
        default:
            handled = false;
        }

        if (handled)
            return true;

        handled = true;

        switch (keyChar) {
            
//         case '1':
//             if (mPathway != null) {
//                 mPathwayIndex = 0;
//                 mNextPage = mPathway.getNodeEntry(0);
//                 forwardPage();
//             }
//             repaint();
//             break;
            
        case 'F':
            mFadeEffect = !mFadeEffect;
            break;
//         case 'C':
//         case 'N':
//             mShowContext.doClick();
//             break;
//         case 'B':
//             mToBlack.doClick();
//             break;
        case 'm':
            mShowNavigator = !mShowNavigator;
            repaint("showOverview");
            break;

        case 'p':
            PathwayPanel.TogglePathwayExclusiveFilter();
            break;
            
        case '+':
        case '=': // allow "non-shift-plus"
            if (mShowNavigator && OverviewMapSizeIndex < OverviewMapScales.length-1) {
                OverviewMapSizeIndex++;
                repaint("overviewBigger");
            } else
                handled = false;
            break;
        case '-':
        case '_': // allow "shift-minus" also
            if (mShowNavigator && OverviewMapSizeIndex > 0) {
                OverviewMapSizeIndex--;
                repaint("overviewSmaller");
            } else
                handled = false;
            break;
        default:
            handled = false;
        }

        //if (handled && DEBUG.KEYS) out("HANDLED " + keyChar);

        return handled;
    }


    @Override
    public boolean handleKeyReleased(java.awt.event.KeyEvent e) {
        return false;
    }
    
    //private boolean isPresenting() { return !mShowContext.isSelected(); }
    
    private static float[] OverviewMapScales = {8, 6, 4, 3, 2.5f, 2, 1.5f, 1};
    private static int OverviewMapSizeIndex = 5;
    private float mNavMapX, mNavMapY; // location of the overview navigator map
    private DrawContext mNavMapDC;

    
    /** Draw a ghosted panner */
    private void drawOverviewMap(DrawContext dc)
    {
        //sourceDC.setRawDrawing();
        //DrawContext dc = sourceDC.create();
        //dc.setRawDrawing();
        //final DrawContext dc = sourceDC;

        dc.setFrameDrawing();
        //dc.setPresenting(true);

        float overviewMapFraction = OverviewMapScales[OverviewMapSizeIndex];
        final Rectangle panner = new Rectangle(0,0,
                                               (int) (dc.frame.width / overviewMapFraction),
                                               (int) (dc.frame.height / overviewMapFraction));

        mNavMapX = dc.frame.width - panner.width;
        mNavMapY = dc.frame.height - panner.height;

        dc.g.translate(mNavMapX, mNavMapY);

        dc.g.setColor(Color.gray); // black or white depending on brightess of the fill?
        dc.g.fill(panner);

        // clip in case borders that might extend outside the panner if the fit is tight to the raw map shapes        
        dc.g.clipRect(0,0, panner.width, panner.height);
        //dc.setMasterClip(panner);
        
        //final LWComponent focused = mCurrentPage.isMapView() ? null : mCurrentPage.getOriginalMapNode();

        // the "focused" node is the node on the map we're going to show highlighted
        // -- it's the node our current slide belongs to
        
//         // This more or less allows panning to the node icon:
//         LWComponent focused = null;
//         if (mFocal instanceof LWSlide)
//             focused = ((LWSlide)mFocal).getSourceNode();
//         else if (mFocal instanceof LWMap == false)
//             focused = mFocal;

        final MapViewer viewer = VUE.getActiveViewer(); // TODO: pull from somewhere safer
        
        //LWComponent focused = mCurrentPage.getOriginalMapNode();
        LWComponent focused = mCurrentPage.getPresentationFocal();

        if (focused != null) {

            // if we're nav-clicking within a slide, the original map node is totally
            // uninteresting: it's just the node (e.g., an image) on the slide -- if
            // slides we're really on the map tho, we could in fact zoom to it normally.
            
            //final LWSlide slide = (LWSlide) focused.getAncestorOfType(LWSlide.class);
            // TODO: Test using getParent v.s. getAncestor...
            final LWSlide slide = (LWSlide) focused.getParentOfType(LWSlide.class);
            if (slide != null)
                focused = slide.getSourceNode();
        }

        dc.skipDraw = focused; // will be ignored!  We only pass GC into paintViewerIntoRectangle

        //final LWMap map = viewer.getMap();
        //dc.setDrawPathways(true);
        //map.drawFit(dc.push(), panner, 10); dc.pop();
        
        final Graphics2D pannerGC = (Graphics2D) dc.g.create();
        mNavMapDC = MapPanner.paintViewerIntoRectangle(null,
                                                       pannerGC,
                                                       viewer,
                                                       panner,
                                                       false); // draw reticle

        
        
        if (focused != null) {
            dc.g.setColor(Color.black);
            dc.setAlpha(0.5);
            dc.g.fill(panner);

            if (mCurrentPage.entry != null && mCurrentPage.entry.pathway != null) {
                // redraw the current pathway so it's highlighted
                mCurrentPage.entry.pathway.drawPathwayWithDots(mNavMapDC);
            }
            
            
//             if (false && mShowNavNodes) {
//                 // Also highlight connectd nav nodes if non-linear nav overlay is showing:
//                 for (NavNode nav : mNavNodes) {
//                     if (nav.page.node != null && nav.page.node != focused) {
//                         nav.page.node.draw(dc.push()); dc.pop();
//                     }
//                 }
//             }

            //if (!(focused instanceof LWSlide) && mCurrentPage.isMapView() && bounds != null) {
            if (focused instanceof LWMap == false) {
                final Rectangle2D.Float bounds = mCurrentPage.getOriginalMapNode().getPaintBounds();

                bounds.x -= 2;
                bounds.y -= 2;
                bounds.width += 4;
                bounds.height += 4;
                
//                 if (viewer.getFocal() instanceof LWSlide) {
//                     if (focused != null)
//                         //bounds = focused.getPaintBounds();
//                         bounds = focused.getBounds(); // could grab node icon bounds if they're drawing...
//                 } else {
//                     bounds = viewer.getVisibleMapBounds();
//                 }

                  
                // redraw the reticle at full brightness:
                mNavMapDC.setAntiAlias(true);
                
                if (DEBUG.WORK) out("overview showing map bounds for: " + mCurrentPage + " in " + viewer + " bounds " + bounds);
                //mNavMapDC.g.setColor(mCurrentPage.getPresentationFocal().getMap().getFillColor());
                //mNavMapDC.setAlpha(0.25);
                //mNavMapDC.g.fill(bounds);
                mNavMapDC.setAlpha(1);
                mNavMapDC.g.setColor(Color.red);
                mNavMapDC.g.setStroke(VueConstants.STROKE_TWO);
                mNavMapDC.g.draw(bounds);
            }
            
            
            if (DEBUG.WORK) out("overview drawing focused: " + focused);
            dc.setAlpha(1);            
            focused.draw(mNavMapDC.push()); mNavMapDC.pop();
            
            
        }

        mNavMapDC.dispose();
        pannerGC.dispose();
            
    }

    /** @return true to disable rollovers on the map */
    @Override
    public boolean handleMouseMoved(MapMouseEvent e)
    {
//         if (viewer.getFocal() instanceof LWMap) {
//             // never any nav nodes if a map is the focus
//             mShowNavNodes = false;
//             return true;
//         }
        
        ExitButton.trackVisible(e);

        if (mForceShowNavNodes) {
            // no state to change
            return true;
        }
        
        final MapViewer viewer = e.getViewer();
        final int width = viewer.getWidth();
        final int MouseRightActivationPixel = width - 40;
        final int MouseRightClearAfterActivationPixel = width - NavNode.MaxWidth;

        if (DEBUG.MOUSE) {
            Log.debug(String.format("CURX %d; Max %d; On pixel: %d, off pixel %d",
                                    e.getX(),
                                    width,
                                    MouseRightActivationPixel, 
                                    MouseRightClearAfterActivationPixel
                                ));
        }

            
        boolean handled = false;
        if (DEBUG.PICK && mShowNavigator)
            handled = debugTrackNavMapMouseOver(e);

        boolean oldShowNav = mShowNavNodes;

        if (e.getX() > MouseRightActivationPixel) {
            if (DEBUG.MOUSE) Log.debug("nav nodes on at mouse " + e.getX());
            mShowNavNodes = true;
            mForceShowNavNodes = false;
            if (DEBUG.MOUSE && oldShowNav) Log.debug("nav nodes should already be visible");
            
        } else {
            if (mShowNavNodes && !mForceShowNavNodes) {
                if (e.getX() < MouseRightClearAfterActivationPixel) {
                    mShowNavNodes = false;
                    if (DEBUG.MOUSE) Log.debug("nav nodes off " + e.getX());
                }
            }
        }

        if (oldShowNav != mShowNavNodes)
            repaint("mouseMove nav display change");

        return true;
    }

//     /** @return true to disable rollovers on the map */
//     @Override
//     public boolean handleMouseMoved(MapMouseEvent e)
//     {
//         boolean handled = false;
//         if (DEBUG.PICK && mShowNavigator)
//             handled = debugTrackNavMapMouseOver(e);

//         boolean oldShowNav = mShowNavNodes;

//         //int maxHeight = e.getViewer().getVisibleBounds().height;
//         //if (e.getY() > maxHeight - 40) {
//         if (e.getX() < 40) {
//             //if (DEBUG.PRESENT) out("nav nodes on " + e.getY() + " max=" + maxHeight);
//             mShowNavNodes = true;
//             mForceShowNavNodes = false;
//         } else {
//             if (mShowNavNodes && !mForceShowNavNodes) {
//                 if (e.getX() > 200)
//                     mShowNavNodes = false;
//             }
//             //if (DEBUG.PRESENT) out("nav nodes off " + e.getY() + " max=" + maxHeight);
//         }

//         if (oldShowNav != mShowNavNodes)
//             repaint("mouseMove nav display change");
//         //e.getViewer().repaint();

//         return true;
//     }
    

    private boolean debugTrackNavMapMouseOver(MapMouseEvent e)
    {
        //out(mNavMapDC.g.getClipBounds().toString());
        //if (mNavMapDC.g.getClipBounds().contains(e.getPoint())) {
        if (e.getX() > mNavMapX && e.getY() > mNavMapY) {
            //out("over map at " + e.getPoint());
            Point2D.Float mapPoint = new Point2D.Float(e.getX() - mNavMapX,
                                                       e.getY() - mNavMapY);

            out(mNavMapDC.toString());
            out("over nav map rect at " +  mapPoint);

            mapPoint.x -= mNavMapDC.offsetX;
            mapPoint.y -= mNavMapDC.offsetY;
            mapPoint.x /= mNavMapDC.zoom;
            mapPoint.y /= mNavMapDC.zoom;
                
            out("over nav map at " +  mapPoint);
            final MapViewer viewer = e.getViewer();
            final PickContext pc = new PickContext(viewer.getLastDC(), mapPoint.x, mapPoint.y);
            pc.root = viewer.getMap();
            final LWComponent hit = LWTraversal.PointPick.pick(pc);
            //out("hit=" + hit);
            if (hit != null)
                e.getViewer().setIndicated(hit);
            else
                e.getViewer().clearIndicated();
            return true;
        } else
            return false;
    }

    // todo: really, a total refocus, that handles the transition,
    // and knowing if a new focal needs loading
    private void focusUp(MapMouseEvent e) 
    {
        if (DEBUG.PRESENT) out("focusUp");
        
        final boolean toMap = e.isShiftDown();
        final boolean toLinks = e.isAltDown();
        final MapViewer viewer = e.getViewer();

        final LWComponent focal = viewer.getFocal();
        

        //if (toMap)
        viewer.popFocal(toMap);

//         if (toLinks && focal.hasLinks()) {
//             GUI.invokeAfterAWT(new Runnable() { public void run() {
//                 ZoomTool.setZoomFitRegion(viewer,
//                                           focal.getCenteredFanBounds(),
//                                           30,
//                                           true);
//             }});

//         }
        
    }
    
    
    @Override
    public boolean handleMousePressed(MapMouseEvent e)
    {
        if (ExitButton.contains(e)) {
            ExitButton.doAction();
            return true;
        }
        
        if (checkForNavNodeClick(e))
            return true;

        //if (e.isShiftDown())
        //    return false;

        final LWComponent hit = e.getPicked();
        
        if (DEBUG.PRESENT) out("handleMousePressed " + e.paramString() + " hit on " + hit);

        // TODO: currently optimized for random nav: totally screwing slides for the moment...

//         if (hit instanceof LWLink && !(hit.getParent() instanceof LWMap)) {
//             // do nothing for links inside slide or group for now (display is jumpy/messy for groups)
//             return true;
//         }

        if (hit == null) {
            focusUp(e);
            return true;
            //return false;
        }
        
        if (mCurrentPage.equals(hit)) {

            if (mCurrentPage.isMapViewNode()) {
                // a click on the current map-node page: stay put
                return false;
            }
            
            // hit on what what we just clicked on: backup,
            // but only if it's not a full pathway entry
            // (meant for intra-slide clicking)
            //if (mCurrentPage.entry == null) {

            // if we're non-linear nav off a pathway, revisit prior,
            // OTHERWISE, if general "browse", pop the focal
            if (mCurrentPage.insideSlide() || mLastPathwayPage == null) {
                focusUp(e);
            } else if (mVisited.hasPrev()) {
                
                // TODO: ONLY DO THIS IF PREVIOUS IS AN ANCESTOR OF CURRENT
                // (this could be tricky to figure out using pages tho...)
                // E.g., only do this if delving into a slide.
                // Current undesired behaviour: clicking on a map-view
                // page is auto-backing up!

                // 2007-10-22 For now: changed Page.isMapViewNode to return TRUE
                // if entry is null, so this code is cut off above -- the only
                // place isMapViewNode() is ever used.  
                
                revisitPrior();
            }
        } else {
            // todo: use tobe refocus code, which ZoomTool should also
            // use (hell, maybe this should all go in the MapViewer)
            setPage(hit);
        }
        return true;
    }

        
    private boolean checkForNavNodeClick(MapMouseEvent e) {

        if (!mShowNavNodes)
            return false;
        
        Page hitPage = null;
        
        for (NavNode nav : mNavNodes) {
            if (DEBUG.PICK) out("pickCheck " + nav + " point=" + e.getPoint() + " mapPoint=" + e.getMapPoint());
            hitPage = nav.hitPage(e.getX(), e.getY());
            if (hitPage != null) {
                if (DEBUG.PRESENT) out("HIT PAGE " + hitPage + "; on NavNode: " + nav);
                break;
            }
        }
        
        if (hitPage != null) {
            if (hitPage.equals(mVisited.prev())) {
                revisitPrior();
            } else {
                setPage(hitPage);
            }
            return true;
        } else
            return false;
        
            
//             if (nav.containsLocalCoord(e.getX(), e.getY())) {
//                 if (DEBUG.PRESENT) System.out.println("HIT " + nav);
//                 if (nav.page.equals(mVisited.prev())) {
//                     revisitPrior();
//                 } else {
//                     setPage(nav.page);
//                 }
//                 return true;
//             }
//         }
//         return false;
        
    }

    
//     @Override
//     public void handleFullScreen(boolean entering, boolean nativeMode) {
//         out("handleFullScreen: " + entering + " native=" + nativeMode);
//         if (entering) {
//             //if (nativeMode)
//                 VueAction.setAllActionsIgnored(true);
//         } else
//             VueAction.setAllActionsIgnored(false);
//     }

    @Override
    public void handleToolSelection(boolean selected, VueTool fromTool)
    {
        //handleFullScreen(selected && VUE.inFullScreen(), VUE.inNativeFullScreen());

        VueAction.setAllActionsIgnored(selected);
        
        if (!selected)
            return;

        ExitButton.setVisible(true);

        //GUI.refreshGraphicsInfo();
        //MouseRightActivationPixel = GUI.GScreenWidth - 40;
        //MouseRightClearAfterActivationPixel = GUI.GScreenWidth - 200;

        mCurrentPage = NO_PAGE;
        if (VUE.getSelection().size() == 1)
            mNextPage = VUE.getSelection().first();
        else
            mNextPage = null;

        VUE.getSelection().clear();
        //repaint();
    }

    private boolean startUnderway;
    public void startPresentation()
    {
        out("startPresentation");
        //new Throwable("FYI: startPresentation (debug)").printStackTrace();
        
        //if (DEBUG.PRESENT && DEBUG.META) tufts.Util.printStackTrace("startPresentation");
        
        //mShowContext.setSelected(false);
        mVisited.clear();

        //makeInvisible();        

        final LWPathway pathway = VUE.getActivePathway();

        if (!pathway.isVisible())
            pathway.setVisible(true);

        startUnderway = true;

        try {
        
            if (pathway != null && pathway.length() > 0) {

                final LWPathway.Entry entry = pathway.getCurrentEntry();
                if (entry != null && !entry.isPathway()) {
                    setEntry(entry);
                } else {
                    setEntry(pathway.getEntry(0));
                }
                
            } else {
                loadPathway(null);
                //mPathwayIndex = 0;
                if (VUE.getSelection().size() > 0)
                    setPage(VUE.getSelection().first());
                //             else if (mCurrentPage != NO_PAGE) // won't have any effect!
                //                 setPage(mCurrentPage);
                //             else
                //                 setPage(mNextPage);
            }
        } finally {
            startUnderway = false;
        }

        if (DEBUG.Enabled) out("startPresentation: completed");

        //makeVisibleLater();
    }
    

    
    /** @param direction either 1 or -1 */
    private LWPathway.Entry nextPathwayEntry(String direction)
    {
        if (mLastPathwayPage == null)
            return null;

        int index = mLastPathwayPage.entry.index();

        if (direction == FORWARD)
            index++;
        else
            index--;

        return mLastPathwayPage.entry.pathway.getEntry(index);
        
        
        /*
        //out("nextPathwayEntry " + direction);
        if (direction == BACKWARD && mPathwayIndex == 0)
            return null;
        
        if (direction == FORWARD)
            mPathwayIndex++;
        else
            mPathwayIndex--;

        final LWPathway.Entry nextEntry = mPathway.getEntry(mPathwayIndex);
        //out("Next pathway index: #" + mPathwayIndex + " = " + nextEntry);

        if (nextEntry == null) {
            //nextEntry = mPathway.getFirstEntry(nextPathwayNode);
            mPathwayIndex -= (direction == FORWARD ? 1 : -1);
        }
        //out("Next pathway slide/focal: " + nextEntry);
        return nextEntry;
        */
        
    }
    
    //private LWComponent guessNextPage() { return null; }
        
    private void setEntry(LWPathway.Entry e) {
        setEntry(e, RECORD_BACKUP);
    }
    
    /** this will jump us to the pathway for the entry, and set us viewing the given entry */
    private void setEntry(LWPathway.Entry entry, boolean recordBackup)
    {
        //out("setEntry " + entry);
        if (entry == null)
            return;
        //mEntry = e;
        //mPathway = entry.pathway;
        //mPathwayIndex = entry.index();
        setPage(new Page(entry), recordBackup);
        //setPage(new Page(mEntry), recordBackup);
        //setPage(mEntry.getFocal(), recordBackup);
        //VUE.setActive(LWPathway.Entry.class, this, entry);
    }

    private void setPage(LWComponent destination) {
        if (destination != null)
            setPage(new Page(destination), RECORD_BACKUP);
    }
    
    private void setPage(Page page) {
        setPage(page, RECORD_BACKUP);
    }

    private void loadPathway(LWPathway pathway) {
        //if (DEBUG.PRESENT) new Throwable("FYI, loadPathway: " + pathway).printStackTrace();
        if (DEBUG.PRESENT) out("loadPathway " + pathway);
        LWComponent.swapLWCListener(this, mPathway, pathway);
        mPathway = pathway;
    }

    public void LWCChanged(LWCEvent e) {
        // if the pathway changes it's filtering state, we'll get this event from it to know to repaint
        if (e.key == LWKey.Repaint || e.source instanceof LWMap)
            repaint(e.toString());
    }

    public void out(String s) {
        if (DEBUG.Enabled) Log.debug(s);
    }


    private void recordPageTransition(final Page page, boolean recordBackup)
    {
        if (DEBUG.WORK||DEBUG.PRESENT) {
            if (DEBUG.META) Util.printStackTrace("recordPageTransition");
            System.out.println("\n-----------------------------------------------------------------------------");
            out("  mCurrentPage: " + mCurrentPage);
            out("pageTransition: " + page);
            out(" mVisited.prev: " + mVisited.prev());
        }

        if (page == null) // for now
            return;

            
        // we're backing up:

        
        if (page.equals(mVisited.prev())) {
            
            // [too aggressive: if click again ]
            // ANY time we record a page transtion to what's one back on
            // the queue, treat it as a rollback, even if recordBackup is true.
            // This may be too agressive, but it's worth a try for now.
            if (DEBUG.PRESENT) out("ROLLBACK");
            mVisited.rollBack();
        } else if (recordBackup) {
            if (page.node instanceof LWMap)
                ; // don't record map views
            else
                mVisited.push(page);
        }

//         if (recordBackup) {
//             mVisited.push(page);
//         } else {
//             // we're backing up:
//             //System.out.println("\nCOMPARING:\n\t" + page + "\n\t" + mVisited.prev());
//             if (page.equals(mVisited.prev()))
//                 mVisited.rollBack();
//         }

        mLastPage = mCurrentPage;
        mCurrentPage = page;

        if (page.onPathway()) {
            if (page.pathway() != mPathway)
                loadPathway(page.pathway());
            //if (!VUE.inNativeFullScreen()) // don't send any events just in case
                VUE.setActive(LWPathway.Entry.class, this, page.entry);
            mLastPathwayPage = page;
            //mPathwayIndex = page.entry.index();
        }
        
        mNextPage = null;
        mNavNodes.clear();
    }
    
    

    private volatile boolean pageLoadingUnderway = false;
    private void setPage(final Page page, boolean recordBackup)
    {
        if (page == null) { // for now
            tufts.Util.printStackTrace("null page!");
            return;
        }
        
        if (page.equals(mCurrentPage)) {
            if (DEBUG.Enabled) Log.debug("current page matches new page: " + mCurrentPage + "; requested=" + page);
            return;
        }

        if (DEBUG.PRESENT) out("setPage " + page);
        
        recordPageTransition(page, recordBackup);

        
//         final boolean doSlideTransition;

//         if (mCurrentPage != null && mCurrentPage.entry != null && page.entry != null)
//             doSlideTransition = true;
//         else
//             doSlideTransition = false;

        final MapViewer viewer = VUE.getActiveViewer();
        
        viewer.clearTip(); // just in case

        pageLoadingUnderway = true;
        try {
            // the viewer will shortly call us right back with handleFocalSwitch:
            viewer.switchFocal(page.getPresentationFocal());
        } finally {
            pageLoadingUnderway = false;
        }


        /*
        if (doSlideTransition && mFadeEffect) {
        
            // It case there was a tip visible, we need to make sure
            // we wait for it to finish clearing before we move on, so
            // we need to put the rest of this in the queue.  (if we
            // don't do this, the screen fades out & comes back before
            // the map has panned)
            
            VUE.invokeAfterAWT(new Runnable() {
                    public void run() {
                        if (mFadeEffect)
                            makeInvisible();
                        zoomToFocal(page.getPresentationFocal(), false);
                        //zoomToFocal(page.getPresentationFocal(), !mFadeEffect);
                        if (mScreenBlanked)
                            makeVisibleLater();
                    }
                });
            
        } else {
            final boolean animate;

            // Figure out if this transition is across a continuous coordinate
            // region (so we can animate).  E.g., we're moving across the map,
            // or within a single slide.  Slides and the map they're a part of
            // exist in separate coordinate spaces, and we can't animate
            // across that boundary.
            
            final LWComponent oldFocal = mLastPage.getPresentationFocal();
            final LWComponent newFocal = mCurrentPage.getPresentationFocal();
            final LWComponent oldParent = oldFocal == null ? null : oldFocal.getParent();
            final LWComponent newParent = newFocal.getParent();
            final LWComponent lastSlideAncestor = oldFocal == null ? null : oldFocal.getAncestorOfType(LWPathway.class);
            final LWComponent thisSlideAncestor = newFocal.getAncestorOfType(LWPathway.class);
            
            if (oldParent == newParent || lastSlideAncestor == thisSlideAncestor) {
                
                animate = true;
                
                //if (oldParent == newParent && newParent instanceof LWMap && !(newFocal instanceof LWPortal)) {
                if (false && oldParent == newParent && newParent instanceof LWMap && !(newFocal instanceof LWPortal)) {
                    // temporarily load the map as the focal so we can
                    // see the animation across the map
                    if (DEBUG.WORK) out("loadFocal of parent for x-zoom: " + newParent);
                    viewer.loadFocal(newParent);
                }
            } else
                animate = false;
            
            //GUI.invokeAfterAWT(new Runnable() { public void run() {
            // don't invoke later: is allowing an overview map repaint in an intermediate state
            zoomToFocal(newFocal, animate);
            //}});
        }
        */
    }

    private Rectangle2D.Float getFocalBounds(LWComponent c) {
        return MapViewer.getFocalBounds(c);
    }

    // TODO: if we're currently animating a focal swith,
    // abort it -- and sycnrhonized as needed so as to
    // never have the focal be in an intermediate state

    @Override
    public boolean handleFocalSwitch(final MapViewer viewer,
                                     final LWComponent oldFocal,
                                     final LWComponent newFocal)
    {
        if (DEBUG.PRESENT) out("handleFocalSwitch in " + viewer
                               + "\n\tfrom: " + oldFocal
                               + "\n\t  to: " + newFocal);
        
        if (!pageLoadingUnderway)
            recordPageTransition(new Page(newFocal), true);
        
//         final boolean isSlideTransition;
//         if (mCurrentPage != null && mLastPage.entry != null && mCurrentPage.entry != null)
//             isSlideTransition = true;
//         else
//             isSlideTransition = false;

        mFocal = mCurrentPage.getPresentationFocal();

        // MapViewer auto-animate will only work if the new focal
        // is the map (it loads the focal, then does the anmiated transition).
        // (this is a pop-focal)
        
        // If we want to re-enable zoom-in animations, the below commented out code will
        // need to be cleaned up to compute the animating focal, tho this should be
        // simpler now that slide's are on the map as slide icons.
        
        final boolean animate = newFocal instanceof LWMap;
        
        if (mFadeEffect && !animate) {
            if (!startUnderway)
                makeInvisible();
            viewer.loadFocal(mFocal, FIT_FOCAL, NO_ANIMATE);
            if (!startUnderway)
                fadeUpLater();
        } else {
            viewer.loadFocal(mFocal, FIT_FOCAL, animate);
        }


        if (true) return true;

        //-----------------------------------------------------------------------------
        //
        // Below code is currently all dead...
        //
        //-----------------------------------------------------------------------------
        


//         if (isSlideTransition && mFadeEffect) {

//             // It case there was a tip visible, we need to make sure
//             // we wait for it to finish clearing before we move on, so
//             // we need to put the rest of this in the queue.  (if we
//             // don't do this, the screen fades out & comes back before
//             // the map has panned)
            
//             VUE.invokeAfterAWT(new Runnable() {
//                     public void run() {
//                         makeInvisible();
//                         mFocal = mCurrentPage.getPresentationFocal();
//                         viewer.loadFocal(mFocal, true, false);
//                         //zoomToFocal(page.getPresentationFocal(), false);
//                         //zoomToFocal(page.getPresentationFocal(), !mFadeEffect);
//                         if (mScreenBlanked)
//                             makeVisibleLater();
//                     }
//                 });

//             return true;
//         }

        if (startUnderway) {
            mFocal = newFocal;
            if (DEBUG.PRESENT) out("handleFocalSwitch: starting focal " + newFocal);
            viewer.loadFocal(newFocal, FIT_FOCAL, NO_ANIMATE);
            return true;
        }
            
            
        
        
        // Figure out if this transition is across a continuous coordinate
        // region (so we can animate).  E.g., we're moving across the map,
        // or within a single slide.  Slides and the map they're a part of
        // exist in separate coordinate spaces, and we can't animate
        // across that boundary.
//         final LWComponent oldParent = oldFocal == null ? null : oldFocal.getParent();
//         final LWComponent newParent = newFocal.getParent();
//         final LWComponent lastSlideAncestor = oldFocal == null ? null : oldFocal.getAncestorOfType(LWSlide.class);
//         final LWComponent thisSlideAncestor = newFocal.getAncestorOfType(LWSlide.class);


        if (AnimateTransitions) {
        
        LWComponent animatingFocal = null; // a TEMPORARY focal to animate across

//         if (lastSlideAncestor == thisSlideAncestor && thisSlideAncestor != null) 
//             animatingFocal = thisSlideAncestor;
//         else

        if (oldFocal.hasAncestor(newFocal)) {
            animatingFocal = newFocal;
        } else if (newFocal.hasAncestor(oldFocal)) {
            animatingFocal = oldFocal;
        } else if (oldFocal instanceof LWSlide || newFocal instanceof LWSlide)
            animatingFocal = newFocal.getMap();
        
//         if (oldFocal instanceof LWSlide || newFocal instanceof LWSlide)
//             animatingFocal = newFocal.getMap();
//         else if (oldFocal.hasAncestor(newFocal))
//             animatingFocal = newFocal;
        

        boolean loaded = false;
        if (animatingFocal != null) {
            // if the new focal is a parent of old focal, first
            // load it (without auto-zooming), so we can pan across
            // it while we animate
            viewer.loadFocal(animatingFocal, false, false);
            mFocal = newFocal;
            if (animatingFocal == newFocal)
                loaded = true;

            // now zoom to the current focal within the new parent focal: this should
            // have the effect of making the parent focal visible, while the viewer is
            // actually in the same place on the old focal within the new focal: (todo:
            // this would actually be good default MapViewer behavior: if load the focal
            // of any ancestor, leave is looking at the same absolute map location,
            // which means adjusting our offset within the new focal)
            
            ZoomTool.setZoomFitRegion(viewer,
                                      getFocalBounds(oldFocal),
                                      oldFocal.getFocalMargin(),
                                      false);
            

        }

        if (DEBUG.PRESENT) out("  animating focal: " + animatingFocal);
        if (DEBUG.PRESENT) out("destination focal: " + newFocal);
        
        if (animatingFocal instanceof LWSlide && newFocal == animatingFocal)
            animateToFocal(viewer, newFocal, true);
        else
            animateToFocal(viewer, newFocal, false);


        }
            
        //if (oldParent == newParent || lastSlideAncestor == thisSlideAncestor)

        //if (!loaded) {
            if (true) { // just in case...
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                mFocal = newFocal; // TODO: THREADSAFE?
                viewer.loadFocal(newFocal, true, false);
            }});
        }


        return true;
        
            
//         if (oldParent == newParent || lastSlideAncestor == thisSlideAncestor) {
                
//             animate = true;
            
//             //if (oldParent == newParent && newParent instanceof LWMap && !(newFocal instanceof LWPortal)) {
//             if (false && oldParent == newParent && newParent instanceof LWMap && !(newFocal instanceof LWPortal)) {
//                 // temporarily load the map as the focal so we can
//                 // see the animation across the map
//                 if (DEBUG.WORK) out("loadFocal of parent for x-zoom: " + newParent);
//                 viewer.loadFocal(newParent);
//             }
//         } else
//             animate = false;
        
//         //GUI.invokeAfterAWT(new Runnable() { public void run() {
//         // don't invoke later: is allowing an overview map repaint in an intermediate state
//         animateToFocal(viewer, newFocal);
//         //}});
//        return false;
    }
    
    
    private void animateToFocal(MapViewer viewer, LWComponent newFocal, boolean forceRawSlideBounds) {
        if (DEBUG.WORK) out("zoomToFocal: animating to " + newFocal);

        Rectangle2D focalBounds;

        //if (newFocal instanceof LWSlide && mFocal == newFocal)
        if (forceRawSlideBounds)
            focalBounds = newFocal.getBounds();
        else
            focalBounds = getFocalBounds(newFocal);

        final boolean animate;

        
        

        ZoomTool.setZoomFitRegion(viewer,
                                  //getFocalBounds(newFocal),
                                  focalBounds,
                                  newFocal.getFocalMargin(),
                                  AnimateAcrossMap);
    }

    /*
    private void zoomToFocal(LWComponent focal, boolean animate) {
        final MapViewer viewer = VUE.getActiveViewer();
        
        if (animate) {
            if (DEBUG.WORK) out("zoomToFocal: animating to " + focal);
            ZoomTool.setZoomFitRegion(viewer,
                                      focal.getBounds(),
                                      0,
                                      animate);
        }
        if (DEBUG.WORK) out("zoomToFocal: final viewer load: " + focal);
        viewer.loadFocal(focal);
    }
    */
    
    

    @Override
    public DrawContext getDrawContext(DrawContext dc) {
        dc.setPresenting(true);

        // If this is a transition along a pathway, we want to figure
        // that out so we can draw the pathways in the case, even
        // while animating (so you can see the travel along the
        // pathway stroke).  And what we REALLY want is to only draw
        // the pathway we're animating along (perhaps we could do that
        // here manually in postDraw, or even just draw the single
        // connector stroke between those two nodes!)
        
        //        if (mCurrentPage != null)
        //            dc.setDrawPathways(mCurrentPage.node instanceof LWMap);

        //out("getDrawContext");

        dc.setDrawPathways(dc.isAnimating() || mCurrentPage == null || mCurrentPage.node instanceof LWMap);
        
        //if (mShowNavNodes) dc.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));

        return dc;
    }

    private static final Color DefaultFill = new Color(32,32,32);
        
    
    @Override
    public void handlePreDraw(DrawContext dc, MapViewer viewer) {

        if (dc.focal instanceof LWMap || mPathway == null) {
            dc.fillBackground(DefaultFill);
            return;
        }

        if (mPathway != null) {
            if (mCurrentPage != null) {
                final LWSlide master = mPathway.getMasterSlide();
                boolean drawMaster = false;
                if (DEBUG.PRESENT) out("handlePreDraw: FILLING FOR ENTRY: " + mCurrentPage.entry);
                if (mCurrentPage.entry != null) {
                    dc.fillBackground(mCurrentPage.entry.getFullScreenFillColor(dc));
                    if (!mCurrentPage.entry.hasSlide())
                        drawMaster = true;
                } else {
                    dc.fillBackground(master.getFillColor());
                    // current page has no entry: we've navigated off pathway
                    // onto an arbitrary map node:
                    drawMaster = false;
                }
                if (drawMaster) {
                    if (DEBUG.PRESENT) out("handlePreDraw: DRAWING MASTER " + master);
                    master.drawFit(dc.create(), 0);
                    //master.drawIntoFrame(dc);
                }
            }
        }
    }
    
    @Override
    public void handlePostDraw(DrawContext dc, MapViewer viewer)
    {
        if (DEBUG.PRESENT) out("handlePostDraw " + viewer + "; showNav=" + mShowNavNodes);

        // TODO TODO: portal's are leaving a clip in place that can
        // prevent nav nodes / overview map from being seen / clip
        // them in the middle!
        
        if (mShowNavNodes) {
            
            // TODO: Will need a set of nav nodes per-viewer if this is to work in both
            // the main viewer and the slide viewer... for now, the slide viewer ignores
            // all drawing effects of the active tool, so we don't have to worry about
            // it.
        
            //dc.g.setComposite(AlphaComposite.Src);
            if (dc.isInteractive()) {
                drawNavNodes(dc.push()); dc.pop();
            } else
                out("NON-INTERACTIVE (no NavNodes)");
            
            if (mDidAutoShowNavNodes) {
                mShowNavNodes = mForceShowNavNodes = false;
                mDidAutoShowNavNodes = false;
            }
        }


        // Be sure to draw the navigator after the nav nodes, as navigator
        // display can depend on the current nav nodes, which are created
        // at draw time.
        if (mShowNavigator && mFocal instanceof LWMap == false) {
            drawOverviewMap(dc.push()); dc.pop();
        }

        if (dc.isInteractive() && ExitButton.isVisible()) {
            dc.setFrameDrawing();
            // only really need to set location when this tool actives, but just
            // in case the screen size should change...
            ExitButton.setLocation(30, dc.frame.height - (ExitButton.height+20)); 
            ExitButton.draw(dc);
        }
        
        if (DEBUG.NAV) {
            dc.setFrameDrawing();
            //dc.g.translate(dc.frame.x, dc.frame.y);
            //dc.g.setFont(VueConstants.FixedFont);
            dc.g.setFont(new Font("Lucida Sans Typewriter", Font.BOLD, 10));
            //dc.g.setColor(new Color(128,128,128,192));
            dc.g.setColor(Color.green);
            int y = 10;
            dc.g.drawString("       Frame: " + tufts.Util.out(dc.frame), 10, y+=15);
            dc.g.drawString("        Page: " + mCurrentPage, 10, y+=15);
            dc.g.drawString("      OnPath: " + onCurrentPathway(), 10, y+=15);
            dc.g.drawString("     Pathway: " + mPathway, 10, y+=15);
            dc.g.drawString("LastPathPage: " + mLastPathwayPage, 10, y+=15);
            dc.g.drawString("CurPageFocal: " + mCurrentPage.getPresentationFocal(), 10, y+=15);
            y+=5;

            dc.g.setFont(new Font("Lucida Sans Typewriter", Font.BOLD, 10));
            int i = 0;
            for (Page p : mVisited) {
                if (p == mVisited.current())
                    dc.g.setColor(Color.green);
                else
                    dc.g.setColor(Color.gray);
                dc.g.drawString(String.format("%2d %s", i, p), 10, y+=15);
                i++;
            }
            
            //dc.g.drawString("  Backup: " + mVisited.peek(), 10, y+=15);
            //dc.g.drawString("BackNode: " + mVisited.peekNode(), 10, y+=15);
        }
    }


    private void drawNavNodes(DrawContext dc)
    {
        LWComponent node = mCurrentPage.getOriginalMapNode();

        mNavNodes.clear();
        
//         if (node == null || (node.getLinks().size() == 0 && node.getPathways().size() < 2))
//             return;
        
        makeNavNodes(mCurrentPage, dc.getFrame());

        if (DEBUG.PRESENT) out("drawing nav nodes " + mNavNodes.size());
        
        dc.setDrawPathways(false);
        dc.setInteractive(false);
        for (LWComponent c : mNavNodes) {
            dc.setFrameDrawing(); // reset the GC each time, as draw translate it to local coords each time
            c.draw(dc);
        }
    }

    private static final int NavNodeX = -10; // clip the left edge of the round-rect

    private class NavLayout {
        static final int InsetOffEdge = -10;
        static final int InsetIndent = 7;
        static final int VerticalGap = 7;
        
        final float rightSide;
        int rightInset = InsetOffEdge;
        float y;

        boolean offEdgeNodes = true;

        final Object skip1, skip2;
        
        NavLayout(Rectangle frame, Object s1, Object s2) {
            skip1 = s1;
            skip2 = s2;
            rightSide = frame.x + frame.width;
            y = frame.y + VerticalGap;
        }

        void startIndentRegion() {
            offEdgeNodes = false;
            y += 30;
            rightInset = InsetIndent;
        }
            
        
        void addIfUnique(Page page, String debug) {
            if (!page.equals(skip1) && !page.equals(skip2))
                add(new NavNode(page, offEdgeNodes, false, debug));
        }

        void add(NavNode nn) {
            mNavNodes.add(nn);
            nn.setLocation((rightSide - nn.getWidth()) - rightInset, y);            
            y += nn.getHeight() + VerticalGap;
        }

        void setLastPathway(Page page) {
            add(new NavNode(page, offEdgeNodes, true, "last-path"));
        }

        private void add(Page page, String debug) {
            add(new NavNode(page, offEdgeNodes, false, debug));
        }
        
        
    }

    private void makeNavNodes(Page page, Rectangle frame)
    {
        if (DEBUG.PRESENT) out("makeNavNodes " + page);
        
        //final Page prev = mLastPage;
        final Page prev = mVisited.prev();
        
        final NavLayout layout = new NavLayout(frame, prev, mLastPathwayPage);

        if (prev != null && prev != NO_PAGE && !prev.equals(mLastPathwayPage))
            layout.add(prev, "back");
        
        // always add the current pathway at the top
        if (mLastPathwayPage != null)
            layout.setLastPathway(mLastPathwayPage);

        if (mCurrentPage.node != null && mCurrentPage.node.inVisiblePathway())
            layout.add(new Page(mCurrentPage.node), "loop-back");
        
        layout.startIndentRegion();

        final LWComponent mapNode = page.getOriginalMapNode();

        if (page.entry != null) {
            final LWPathway.Entry nextEntryThisPath = page.entry.next();
            if (nextEntryThisPath != null)
                layout.add(new Page(nextEntryThisPath), "next-path");
        }

        if (mapNode == null)
            return;
        
        for (LWPathway path : mapNode.getPathways()) {
            if (path.isDrawn() && path != mPathway) {
                LWPathway.Entry pathEntry = path.getFirstEntry(mapNode);
                LWPathway.Entry nextPathEntry = pathEntry.next();
                if (nextPathEntry != null)
                    layout.add(new Page(nextPathEntry), "other-path");
            }
        }

        //if (DEBUG.NAV) { /*make room for diagnostics*/ y += 200; }

        
        NavNode nav;
        for (LWLink link : mapNode.getLinks()) {
            // LWComponent farpoint = link.getFarNavPoint(mapNode); // adjust for arrow directionality
            LWComponent farpoint = link.getFarPoint(mapNode);
            if (DEBUG.WORK) out(mapNode + " found farpoint " + farpoint);
            if (farpoint != null && farpoint.isDrawn()) {
//                 if (false && link.hasLabel())
//                     nav = createNavNode(new Page(link));
//                 //nav = createNavNode(link, null); // just need to set syncSource to the farpoint
//                 else
//                     nav = createNavNode(new Page(farpoint));

                layout.addIfUnique(new Page(farpoint), "link");

//                 //nav = createNavNode(farpoint, null);
//                 mNavNodes.add(nav);
//                 y += nav.getHeight() + 5;
//                 nav.setLocation((rightSide - nav.getWidth()) - rightInset, y);
            }
        }
    }



    public static void makeInvisible() {

        // TODO: Would be best to route these through full-screen code so it can ignore these
        // requests while in a transition to full-screen, waiting for the first paint to
        // happen, tho checking for startUnderway where these are called works for us now.
        
        if (VueUtil.isMacPlatform() && VUE.inNativeFullScreen()) {
            //out("makeInvisible");
            try {
                MacOSX.makeMainInvisible();
                mScreenBlanked = true;
            } catch (Error e) {
                Log.error(e);
            }
        }
    }
    public static void fadeUp() {
        if (VueUtil.isMacPlatform() && VUE.inNativeFullScreen()) {
            try {
                if (DEBUG.PRESENT) Log.debug("fadeUp");
                //if (MacOSX.isMainInvisible())
                    MacOSX.fadeUpMainWindow();
                mScreenBlanked = false;
            } catch (Error e) {
                System.err.println(e);
            }
        }
    }
    public static void fadeUpLater() {
        if (VueUtil.isMacPlatform() && VUE.inNativeFullScreen()) {
            if (DEBUG.PRESENT) Log.debug("requesting fadeUp");
            VUE.invokeAfterAWT(new Runnable() { public void run() {
                fadeUp();
            }});
        }
    }

    
    /*

//     private LWComponent currentNode() {
//         if (mCurrentPage instanceof LWSlide)
//             return mEntry == null ? null : mEntry.node;
//         else
//             return mCurrentPage;
//     }

    public void handleSelectionChange(LWSelection s) {
        out("SELECTION CHANGE");
        // TODO: if active map changes, need to be sure to clear current page!
        if (s.size() == 1)
            mCurrentPage = s.first();
        
//         if (s.size() == 1)
//             mNextPage = s.first();
//         else
//             mNextPage = null;

    }
    */

//     private void followLink(LWComponent src, LWLink link) {
//         LWComponent linkingTo = link.getFarPoint(src);
//         mLastFollowed = link;
//         setPage(linkingTo);
//     }

    
    /*
    private LWComponent OLD_guessNextPage()
    {
        // todo: only bother with links that have component endpoints!
        List links = mCurrentPage.getLinks();
        LWLink toFollow = null;
        if (links.size() == 1) {
            toFollow = (LWLink) mCurrentPage.getLinks().get(0);
        } else {
            Iterator i = mCurrentPage.getLinks().iterator();
            
            while (i.hasNext()) {
                LWLink link = (LWLink) i.next();
                if (link.getFarNavPoint(currentNode()) == null)
                    continue; // if a link to nothing, ignore
                if (link != mLastFollowed) {
                    toFollow = link;
                    break;
                }
            }
        }
        LWComponent nextPage = null;
        if (toFollow != null) {
            mLastFollowed = toFollow;
            nextPage = toFollow.getFarNavPoint(currentNode());
            if (nextPage.getParent() instanceof LWGroup)
                nextPage = nextPage.getParent();
        }
        // TODO: each page keeps a mPrevPage, which we use instead of
        // the peek here to find the "default" backup.  Maybe
        // a useful "uparrow" functionality can also make use of this?
        // uparrow should also go back up to the parent of the current
        // page if it's anything other than the map itself
        if (nextPage == null && !mVisited.isEmpty() && mVisited.peek() != mCurrentPage)
            nextPage = mVisited.peek();

        return nextPage;
    }
    */

    
    /*    
    public void handleSelectionChange(LWSelection s) {
        if (s.size() == 1 &&
            VUE.multipleMapsVisible() &&
            VUE.getRightTabbedPane().getSelectedViewer().getMap() == VUE.getActiveMap())
        {
            MapViewer viewer = VUE.getRightTabbedPane().getSelectedViewer();
            ZoomTool.setZoomFitRegion(viewer, s.first().getBounds(), 16);
        }
    }
    */
    
    /** @return true */
    public boolean supportsSelection() { return true; }
    /** @return false */
    public boolean supportsResizeControls() { return false; }
    /** @return false */
    public boolean supportsDrag(MapMouseEvent e) { return false; }
    /** @return false */
    public boolean supportsDraggedSelector(MapMouseEvent e) { return false; }
    /** @return true */
    public boolean hasDecorations() { return true; }
    /** @return true */
    public boolean usesRightClick() { return true; }


    /** @return false if in native full screen */
    @Override
    public boolean permitsToolChange() {
        return !VUE.inNativeFullScreen();
    }


    
}


// Left handed original style nav-nodes:
//     private void makeNavNodes(Page page, Rectangle frame)
//     {
//         // always add the current pathway at the top
//         //if (node.inPathway(mPathway))
//         if (page.onPathway(mPathway))
//             mNavNodes.add(createNavNode(page));
//         // tho having the order switch on the user kind of sucks...

//         final LWComponent mapNode = page.getOriginalMapNode();
        
//         for (LWPathway otherPath : mapNode.getPathways()) {
//             //if (otherPath != mPathway && !otherPath.isFiltered())
//             if (!otherPath.isFiltered() && otherPath != mPathway)
//                 mNavNodes.add(createNavNode(new Page(otherPath.getFirstEntry(mapNode))));
//         }


//         float x = NavNodeX, y = frame.y;

//         if (DEBUG.NAV) {
//             // make room for diagnostics:
//             y += 200;
//         }

        
//         for (NavNode nav : mNavNodes) {
//             y += nav.getHeight() + 5;
//             nav.setLocation(x, y);
//         }
        
//         if (!mNavNodes.isEmpty())
//             y += 30;

        
//         NavNode nav;
//         for (LWLink link : mapNode.getLinks()) {
//             // LWComponent farpoint = link.getFarNavPoint(mapNode); // adjust for arrow directionality
//             LWComponent farpoint = link.getFarPoint(mapNode);
//             if (DEBUG.WORK) out(mapNode + " found farpoint " + farpoint);
//             if (farpoint != null && farpoint.isDrawn()) {
//                 if (false && link.hasLabel())
//                     nav = createNavNode(new Page(link));
//                 //nav = createNavNode(link, null); // just need to set syncSource to the farpoint
//                 else
//                     nav = createNavNode(new Page(farpoint));
//                 //nav = createNavNode(farpoint, null);
//                 mNavNodes.add(nav);

//                 y += nav.getHeight() + 5;
//                 nav.setLocation(x, y);
//             }
//         }

//         /*
//         float spacePerNode = frame.width;
//         if (mShowNavigator)
//             spacePerNode -= (frame.width / OverviewMapFraction);
//         spacePerNode /= mNavNodes.size();
//         int cnt = 0;
//         float x, y;
//         //out("frame " + frame);
//         for (LWComponent c : mNavNodes) {
//             x = cnt * spacePerNode + spacePerNode / 2;
//             x -= c.getWidth() / 2;
//             y = frame.height - c.getHeight();
//             c.setLocation(x, y);
//             //out("location set to " + x + "," + y);
//             cnt++;
//         }
//         */
//     }
//     private NavNode createNavNode(LWPathway.Entry e) {
//         //if (DEBUG.WORK) out("creating nav node for page " + page);
//         return new NavNode(new Page(e));
//     }
//     private NavNode createNavNode(Page page) {
//         //if (DEBUG.WORK) out("creating nav node for page " + page);
//         return new NavNode(page);
//     }
//     private NavNode createNavNode(Page page, boolean isLastPathwayPage) {
//         //if (DEBUG.WORK) out("creating nav node for page " + page);
//         return new NavNode(page, isLastPathwayPage);
//     }
//     private NavNode createNavNode(LWPathway path) {
//         //if (DEBUG.WORK) out("creating nav node for page " + page);
//         return new NavNode(new Page(path.asEntry()), 300);
//     }
//     private NavNode createNavNode(LWComponent src, LWPathway pathway) {
//         return new NavNode(src, pathway);
//     }
    
    


//     @Override
//     public boolean handleFocalSwitch(final MapViewer viewer,
//                                      final LWComponent oldFocal,
//                                      final LWComponent newFocal)
//     {
//         if (DEBUG.PRESENT) out("handleFocalSwitch in " + viewer
//                                + "\n\tfrom: " + oldFocal
//                                + "\n\t  to: " + newFocal);
        
//         if (!pageLoadingUnderway)
//             recordPageTransition(new Page(newFocal), true);
        
//         final boolean isSlideTransition;

//         if (mCurrentPage != null && mLastPage.entry != null && mCurrentPage.entry != null)
//             isSlideTransition = true;
//         else
//             isSlideTransition = false;

//         if (isSlideTransition && mFadeEffect) {
//             makeInvisible();
//             mFocal = mCurrentPage.getPresentationFocal();
//             viewer.loadFocal(mFocal, FIT_FOCAL, NO_ANIMATE);
//             makeVisibleLater();
//             return true;
//         }


// //         if (isSlideTransition && mFadeEffect) {

// //             // It case there was a tip visible, we need to make sure
// //             // we wait for it to finish clearing before we move on, so
// //             // we need to put the rest of this in the queue.  (if we
// //             // don't do this, the screen fades out & comes back before
// //             // the map has panned)
            
// //             VUE.invokeAfterAWT(new Runnable() {
// //                     public void run() {
// //                         makeInvisible();
// //                         mFocal = mCurrentPage.getPresentationFocal();
// //                         viewer.loadFocal(mFocal, true, false);
// //                         //zoomToFocal(page.getPresentationFocal(), false);
// //                         //zoomToFocal(page.getPresentationFocal(), !mFadeEffect);
// //                         if (mScreenBlanked)
// //                             makeVisibleLater();
// //                     }
// //                 });

// //             return true;
// //         }

//         if (startUnderway) {
//             mFocal = newFocal;
//             if (DEBUG.PRESENT) out("handleFocalSwitch: starting focal " + newFocal);
//             viewer.loadFocal(newFocal, FIT_FOCAL, NO_ANIMATE);
//             return true;
//         }
            
            
        
        
//         // Figure out if this transition is across a continuous coordinate
//         // region (so we can animate).  E.g., we're moving across the map,
//         // or within a single slide.  Slides and the map they're a part of
//         // exist in separate coordinate spaces, and we can't animate
//         // across that boundary.
// //         final LWComponent oldParent = oldFocal == null ? null : oldFocal.getParent();
// //         final LWComponent newParent = newFocal.getParent();
// //         final LWComponent lastSlideAncestor = oldFocal == null ? null : oldFocal.getAncestorOfType(LWSlide.class);
// //         final LWComponent thisSlideAncestor = newFocal.getAncestorOfType(LWSlide.class);


//         if (AnimateTransitions) {
        
//         LWComponent animatingFocal = null; // a TEMPORARY focal to animate across

// //         if (lastSlideAncestor == thisSlideAncestor && thisSlideAncestor != null) 
// //             animatingFocal = thisSlideAncestor;
// //         else

//         if (oldFocal.hasAncestor(newFocal)) {
//             animatingFocal = newFocal;
//         } else if (newFocal.hasAncestor(oldFocal)) {
//             animatingFocal = oldFocal;
//         } else if (oldFocal instanceof LWSlide || newFocal instanceof LWSlide)
//             animatingFocal = newFocal.getMap();
        
// //         if (oldFocal instanceof LWSlide || newFocal instanceof LWSlide)
// //             animatingFocal = newFocal.getMap();
// //         else if (oldFocal.hasAncestor(newFocal))
// //             animatingFocal = newFocal;
        

//         boolean loaded = false;
//         if (animatingFocal != null) {
//             // if the new focal is a parent of old focal, first
//             // load it (without auto-zooming), so we can pan across
//             // it while we animate
//             viewer.loadFocal(animatingFocal, false, false);
//             mFocal = newFocal;
//             if (animatingFocal == newFocal)
//                 loaded = true;

//             // now zoom to the current focal within the new parent focal: this should
//             // have the effect of making the parent focal visible, while the viewer is
//             // actually in the same place on the old focal within the new focal: (todo:
//             // this would actually be good default MapViewer behavior: if load the focal
//             // of any ancestor, leave is looking at the same absolute map location,
//             // which means adjusting our offset within the new focal)
            
//             ZoomTool.setZoomFitRegion(viewer,
//                                       getFocalBounds(oldFocal),
//                                       oldFocal.getFocalMargin(),
//                                       false);
            

//         }

//         if (DEBUG.PRESENT) out("  animating focal: " + animatingFocal);
//         if (DEBUG.PRESENT) out("destination focal: " + newFocal);
        
//         if (animatingFocal instanceof LWSlide && newFocal == animatingFocal)
//             animateToFocal(viewer, newFocal, true);
//         else
//             animateToFocal(viewer, newFocal, false);


//         }
            
//         //if (oldParent == newParent || lastSlideAncestor == thisSlideAncestor)

//         //if (!loaded) {
//             if (true) { // just in case...
//             GUI.invokeAfterAWT(new Runnable() { public void run() {
//                 mFocal = newFocal; // TODO: THREADSAFE?
//                 viewer.loadFocal(newFocal, true, false);
//             }});
//         }


//         return true;
        
            
// //         if (oldParent == newParent || lastSlideAncestor == thisSlideAncestor) {
                
// //             animate = true;
            
// //             //if (oldParent == newParent && newParent instanceof LWMap && !(newFocal instanceof LWPortal)) {
// //             if (false && oldParent == newParent && newParent instanceof LWMap && !(newFocal instanceof LWPortal)) {
// //                 // temporarily load the map as the focal so we can
// //                 // see the animation across the map
// //                 if (DEBUG.WORK) out("loadFocal of parent for x-zoom: " + newParent);
// //                 viewer.loadFocal(newParent);
// //             }
// //         } else
// //             animate = false;
        
// //         //GUI.invokeAfterAWT(new Runnable() { public void run() {
// //         // don't invoke later: is allowing an overview map repaint in an intermediate state
// //         animateToFocal(viewer, newFocal);
// //         //}});
// //        return false;
//     }
    
