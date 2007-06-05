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

import tufts.Util;
import tufts.vue.shape.RectangularPoly2D;
                       
import edu.tufts.vue.preferences.PreferencesManager;
import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.VuePrefListener;
import edu.tufts.vue.preferences.implementations.ShowIconsPreference;
import edu.tufts.vue.preferences.interfaces.VuePreference;
    
import java.util.Iterator;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.ImageIcon;

/**
 *
 * This is the core graphical object in VUE.  It maintains a {@link java.awt.geom.RectangularShape}
 * to be painted, and {@link LWIcon.Block} for rollovers.
 *
 * The layout mechanism is frighteningly convoluted.
 *
 * @version $Revision: 1.168 $ / $Date: 2007-06-05 13:02:36 $ / $Author: sfraize $
 * @author Scott Fraize
 */

// todo: node layout code could use cleanup, as well as additional layout
// features (multiple columns).
// todo: "text" nodes are currently a total hack

/*

Okay, this issue with getting rid of auto-sized is this:
1 - it simplifies alot and gets rid of a bunch of annoying code
2 - we have to give up the feature of always shrinking if was already at min size,
    tho really, we could still do this if it's just one line and already at the min
    size, so this actually isn't an issue.
3 - the BIGGEST issue is that if you switch platforms and the font isn't exactly
    right, the node size won't be right anymore, so we can expand it to bigger
    if it's bigger, and if it's smaller, we'll just have to be out of luck and
    it won't fit anymore.  ALTHOUGH, we COULD save the TEXT size, and if it's
    DIFFERENT on restore, tweak the node by exactly that much, and we should then
    still be perfect, eh?

4 - and the big benefit in all this is we get multi-line text.  The width
    dimension rules: height is always adjusted, if say, the font gets
    bigger (and do we adjust smaller if it gets smaller?)


    Okay, maybe we KEEP auto-sized: lots of stuff can change the
    size of a node (adding/removing children, icons appear/dissapear),
    unlike OmniGraffle.   Maybe we even have an autoHeight & autoWidth.

    So now we just need to detect older nodes that won't have a text size
    encoded.  So if there's no text size encoded, I guess it's an older
    node :)  In that case, we size the text-box to it's preferred
    width, as opposed to it's minimum width, and fit the node to that
    (if it's autosized).  If it's not auto-sized ... (what?)


*/

public class LWNode extends LWContainer
{
    public static final Object TYPE_TEXT = "textNode";
    
    final static boolean WrapText = false; // under development
    
    public static final Font  DEFAULT_NODE_FONT = VueResources.getFont("node.font");
    public static final Color DEFAULT_NODE_FILL = VueResources.getColor("node.fillColor");
    public static final int   DEFAULT_NODE_STROKE_WIDTH = VueResources.getInt("node.strokeWidth");
    public static final Color DEFAULT_NODE_STROKE_COLOR = VueResources.getColor("node.strokeColor");
    public static final Font  DEFAULT_TEXT_FONT = VueResources.getFont("text.font");
    
    /** how much smaller children are than their immediately enclosing parent (is cumulative) */
    static final float ChildScale = VueResources.getInt("node.child.scale", 75) / 100f;

    //------------------------------------------------------------------
    // Instance info
    //------------------------------------------------------------------
    
    protected RectangularShape drawnShape; // 0 based, not scaled
    protected RectangularShape boundsShape; // map based, scaled, used for computing hits
    protected boolean autoSized = true; // compute size from label & children

    //-----------------------------------------------------------------------------
    // consider moving all the below stuff into a layout object

    //private transient Line2D dividerUnderline = new Line2D.Float();
    //private transient Line2D dividerStub = new Line2D.Float();
    private transient float mBoxedLayoutChildY;

    private transient boolean mIsRectShape = true;
    //private transient boolean mIsTextNode = false; // todo: are we saving this in XML???

    private transient Line2D.Float mIconDivider = new Line2D.Float(); // vertical line between icon block & node label / children
    private transient Point2D.Float mLabelPos = new Point2D.Float(); // for use with irregular node shapes
    private transient Point2D.Float mChildPos = new Point2D.Float(); // for use with irregular node shapes

    private transient Size mMinSize;

    private transient LWIcon.Block mIconBlock =
        new LWIcon.Block(this,
                         IconWidth, IconHeight,
                         null,
                         LWIcon.Block.VERTICAL,
                         LWIcon.Block.COORDINATES_COMPONENT);

    

    public LWNode(String label)
    {
        this(label, 0, 0);
    }

    public LWNode(String label, RectangularShape shape)
    {
        this(label, 0, 0, shape);
    }

    /** internal convenience */
    LWNode(String label, float x, float y, RectangularShape shape)
    {
        super.label = label; // todo: this for debugging
        setFillColor(DEFAULT_NODE_FILL);
        if (shape == null)
            setShape(tufts.vue.shape.RoundRect2D.class);
          //setShape(new RoundRectangle2D.Float(0,0, 10,10, 20,20));
        else if (shape != null)
            setShapeInstance(shape);
        setStrokeWidth(DEFAULT_NODE_STROKE_WIDTH);
        setStrokeColor(DEFAULT_NODE_STROKE_COLOR);
        setLocation(x, y);
        //if (getAbsoluteWidth() < 10 || getAbsoluteHeight() < 10)
        //setSize(MIN_SIZE,MIN_SIZE);
        this.width = NEEDS_DEFAULT;
        this.height = NEEDS_DEFAULT;
        //this.font = DEFAULT_NODE_FONT;
        setFont(DEFAULT_NODE_FONT); // shouldn't need to do this, but label not getting created in setLabel?
        //getLabelBox(); // shoudn't need to do this either: first attempt at labelbox should get it! (not working either!)
        setLabel(label);       
        
    }
    
    /** internal convenience */
    LWNode(String label, float x, float y)
    {
        this(label, x, y, null);
    }

    /** internal convenience */
    LWNode(String label, Resource resource)
    {
        this(label, 0, 0);
        setResource(resource);
    }

    public void setResource(Resource r)
    {
        super.setResource(r);
        if (getChild(0) instanceof LWImage) {
            LWImage image = (LWImage) getChild(0);
            if (image.isNodeIcon())
                image.setResource(r);
        }
    }
    
    
    public static final Key KEY_Shape =
        new Key<LWNode,Class<? extends RectangularShape>>("node.shape", "shape") {
        @Override
        public boolean setValueFromCSS(LWNode c, String cssKey, String cssValue) {
            RectangularShape shape = NodeTool.getTool().getNamedShape(cssValue);
            if (shape == null) {
                return false;
            } else {
                setValue(c, shape.getClass());
                System.err.println("applied shape: " + this + "=" + getValue(c));
                return true;
            }
        }
        @Override
        public void setValue(LWNode c, Class<? extends RectangularShape> shapeClass) {
            c.setShape(shapeClass);
        }
        @Override
        public Class<? extends RectangularShape> getValue(LWNode c) {
            try {
                return c.getShape().getClass();
            } catch (NullPointerException e) {
                return null;
            }
        }
    };

    

    /*
    public static final Key KEY_Shape = new Key("node.shape", KeyType.STYLE) {
            Property getSlot(LWComponent c) { return ((LWNode)c).mShape; }
        };
    public class ShapeProperty extends Property<RectangularShape> {
        ShapeProperty(Key key) {
            super(key);
            // leave as null?
            //value = new Rectangle2D.Float();
        }
        
        final RectangularShape get() { return value; }
    }
    private final ShapeProperty mShape = new ShapeProperty(KEY_Shape); // { }
    */

        
    
    private static RectangularShape cloneShape(Object shape) {
        return (RectangularShape) ((RectangularShape)shape).clone();
    }

    /**
     * @param shapeClass -- a class object this is a subclass of RectangularShape
     */
    public void setShape(Class<? extends RectangularShape> shapeClass) {

        if (boundsShape != null && IsSameShape(boundsShape.getClass(), shapeClass))
            return;

        // todo: could skip instancing unless we actually go to draw ourselves (lazy
        // create the instance) -- it's completely useless for LWNodes serving as style
        // holders to create the instance, tho then we would need to keep a ref
        // to the class object...

        try {
            setShapeInstance(shapeClass.newInstance());
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t);
        }
    }
    
    /**
     * @param shape a new instance of a shape for us to use: should be a clone and not an original
     */
    protected void setShapeInstance(RectangularShape shape)
    {
        if (DEBUG.CASTOR) System.out.println("SETSHAPE " + shape.getClass() + " in " + this + " " + shape);
        //System.out.println("SETSHAPE bounds " + shape.getBounds());
        //if (shape instanceof RoundRectangle2D.Float) {
        //RoundRectangle2D.Float rr = (RoundRectangle2D.Float) shape;
        //    System.out.println("RR arcs " + rr.getArcWidth() +"," + rr.getArcHeight());
        //}

        if (IsSameShape(this.boundsShape, shape))
            return;

        Object old = this.boundsShape;
        this.mIsRectShape = (shape instanceof Rectangle2D || shape instanceof RoundRectangle2D);
        this.boundsShape = shape;
        this.drawnShape = cloneShape(shape);
        adjustDrawnShape();
        layout();
        notify(LWKey.Shape, new Undoable(old) { void undo() { setShapeInstance((RectangularShape)old); }} );
    }

    public void setXMLshape(RectangularShape shape) {
        setShapeInstance(shape);
    }
                                                     
    public RectangularShape getXMLshape() {
        return getShape();
    }
                                                     
    
    /** @return shape object with map coordinates -- can be used for hit testing, drawing, etc */
    public RectangularShape getShape() {
        return this.boundsShape;
    }
    
    protected Point2D.Float getCorner() {
        if (mIsRectShape)
            return super.getCorner();

        // find out where a line drawn from our local center to our
        // lower right bounding box intersects the lower right edge of
        // our local shape
        
        float[] corner =
            VueUtil.computeIntersection(getWidth() / 2, getHeight() / 2,
                                        getWidth(), getHeight(),
                                        getLocalShape(),
                                        null);

        return new Point2D.Float(corner[0], corner[1]);
    }

    
    //public Shape getMapShape() { return this.boundsShape; }
    
    @Override
    public Shape getLocalShape() {
        return this.drawnShape;
    }

    /** Duplicate this node.
     * @return the new node -- will have the same style (visible properties) of the old node */
    @Override
    public LWComponent duplicate(CopyContext cc)
    {
        LWNode newNode = (LWNode) super.duplicate(cc);
        // make sure shape get's set with old size:
        if (DEBUG.STYLE) out("re-adjusting size during duplicate to set shape size");
        newNode.setSize(super.getWidth(), super.getHeight()); 
        return newNode;
    }

    @Override
    public boolean supportsUserLabel() {
        return true;
    }
    
    @Override
    public boolean supportsUserResize() {
        if (isTextNode())
            return !isAutoSized(); // could be confusing, as once is shrunk down, can't resize again w/out undo
        else
            return true;
    }

    /** @return false if this is a text node */
    @Override
    public boolean supportsChildren() {
        return !isTextNode();
    }
    

//     /** @return true if the given property is currently supported on this component
//      * Overriden to disable fill support if current a text node */
//     @Override
//     public boolean supportsProperty(Key key) {
//         if (isTextNode() && key == LWKey.FillColor)
//             return false;
//         else
//             return super.supportsProperty(key);
//     }
    
    
    /**
     * This is consulted during LAYOUT, which can effect the size of the node.
     * So if anything happens that changes what this returns, the node has
     * to be laid out again.  (E.g., if we turn them all of with a pref,
     * all nodes need to be re-laid out / resized
     */
    protected boolean iconShowing()
    {    	
        if (isPresentationContext() || isTextNode())
            return false;
         else
            return mIconBlock.isShowing(); // remember not current till after a layout
    }

    // was text box hit?  coordinates are component local
    private boolean textBoxHit(float cx, float cy)
    {
        // todo cleanup: this is a fudgey computation: IconPad / PadTop not always used!
        float lx = relativeLabelX() - IconPadRight;
        float ly = relativeLabelY() - PadTop;
        Size size = getTextSize();
        float h = size.height + PadTop;
        float w = size.width + IconPadRight;
        //float height = getLabelBox().getHeight() + PadTop;
        //float width = (IconPadRight + getLabelBox().getWidth()) * TextWidthFudgeFactor;

        return
            cx >= lx &&
            cy >= ly &&
            cx <= lx + w &&
            cy <= ly + h;
    }

    @Override
    public void mouseOver(MapMouseEvent e)
    {
        //if (textBoxHit(cx, cy)) System.out.println("over label");
        //if (mIconBlock.isShowing())
        if (iconShowing())
            mIconBlock.checkAndHandleMouseOver(e);
    }

    @Override
    public boolean handleDoubleClick(MapMouseEvent e)
    {
        //System.out.println("*** handleDoubleClick " + e + " " + this);

        float cx = e.getComponentX();
        float cy = e.getComponentY();

        if (textBoxHit(cx, cy)) {
            e.getViewer().activateLabelEdit(this);
        } else {
            if (!mIconBlock.handleDoubleClick(e)) {
                // by default, a double-click anywhere else in
                // node opens the resource
                if (hasResource()) {
                    getResource().displayContent();
                    // todo: some kind of animation or something to show
                    // we're "opening" this node -- maybe an indication
                    // flash -- we'll need another thread for that.
                    
                    //mme.getViewer().setIndicated(this); or
                    //mme.getComponent().paintImmediately(mapToScreenRect(getBounds()));
                    //or mme.repaint(this)
                    // now open resource, and then clear indication
                    //clearIndicated();
                    //repaint();
                }
            }
        }
        return true;
    }

    @Override
    public boolean handleSingleClick(MapMouseEvent e)
    {
        //System.out.println("*** handleSingleClick " + e + " " + this);
        // "handle", but don't actually do anything, if they single click on
        // the icon (to prevent activating a label edit if they click here)
        //return iconShowing() && genIcon.contains(e.getComponentPoint());

        // for now, never activate a label edit on just a single click.
        // --prob better to conifg somehow than to depend on MapViewer side-effects
        return true;
    }

    //public void setIcon(javax.swing.ImageIcon icon) {}
    //public javax.swing.ImageIcon getIcon() { return null; }

    @Override
    public Object getTypeToken() {
        return isTextNode() ? TYPE_TEXT : super.getTypeToken();
    }
    
    /**
     * if asText is true, make this a text node, and isTextNode should return true.
     * If asText is false, do the minimum to this node such that isTextNode will
     * no longer return true.
     */
    public void setAsTextNode(boolean asText)
    {
        if (asText) {
            setShape(java.awt.geom.Rectangle2D.Float.class); // now enforced
            //setStrokeWidth(0f); // just a default, not enforced
            disableProperty(LWKey.Shape);
            setFillColor(COLOR_TRANSPARENT);
            setFont(DEFAULT_TEXT_FONT);
        } else {
            enableProperty(LWKey.Shape);
            setFillColor(DEFAULT_NODE_FILL);
        }
        if (asText)
            setAutoSized(true);
        //setFillColor(getParent().getFillColor());
    	//mIsTextNode = pState;
    }
    
    public static boolean isTextNode(LWComponent c) {
        if (c instanceof LWNode)
            return ((LWNode)c).isTextNode();
        else
            return false;
    }
    
    public boolean isTextNode() {
        // todo: "text" node should display no note icon, but display the note if any when any part of it is rolled over.
        // Just what a text node is is a bit confusing right now, but it's useful
        // guess for now.
    	//return (mIsTextNode || (getFillColor() == null && mIsRectShape)) && !hasChildren();

        return getClass() == LWNode.class // sub-classes don't count
            && isTranslucent()
            && !hasChildren()
            && !inPathway(); // heuristic to exclude LWNode portals (not likely to just put a piece of text alone on a pathway)
    }
    
    /** If true, compute node size from label & children */
    @Override
    public boolean isAutoSized() {
        if (WrapText)
            return false; // LAYOUT-NEW
        else
            return this.autoSized;
    }

    /**
     * For explicitly restoring the autoSized bit to true.
     *
     * The autoSize bit is only *cleared* via automatic means: when the
     * node's size is explicitly set to something bigger than that
     * size it would have if it took on it's automatic size.
     *
     * Clearing the autoSize bit on a node manually would have no
     * effect, because as soon as it was next laid out, it would
     * notice it has it's minimum size, and would automatically
     * set the bit.
     */
    
    @Override
    public void setAutoSized(boolean makeAutoSized)
    {
        if (WrapText) return; // LAYOUT-NEW
        
        if (autoSized == makeAutoSized)
            return;
        if (DEBUG.LAYOUT) out("*** setAutoSized " + makeAutoSized);

        // We only need an undo event if going from not-autosized to
        // autosized: i.e.: it wasn't an automatic shift triggered via
        // set size. Because size events aren't delieverd if autosized
        // is on (which would normally notice the size change), we need
        // to remember the old size manually here if turning autosized
        // back on)

        Object old = null;
        if (makeAutoSized)
            old = new Point2D.Float(this.width, this.height);
        this.autoSized = makeAutoSized;
        if (autoSized && !inLayout)
            layout();
        if (makeAutoSized)
            notify("node.autosized", new Undoable(old) {
                    void undo() {
                        Point2D.Float p = (Point2D.Float) old;
                        setSize(p.x, p.y);
                    }});
    }
    
    /**
     * For triggering automatic shifts in the auto-size bit based on a call
     * to setSize or as a result of a layout
     */
    private void setAutomaticAutoSized(boolean tv)
    {
        if (isOrphan()) // if this is during a restore, don't do any automatic auto-size computations
            return;
        if (autoSized == tv)
            return;
        if (DEBUG.LAYOUT) out("*** setAutomaticAutoSized " + tv);
        this.autoSized = tv;
    }
    

    private static boolean IsSameShape(
                                       Class<? extends RectangularShape> c1,
                                       Class<? extends RectangularShape> c2) {
        if (c1 == null || c2 == null)
            return false;
        if (c1 == c2) {
            if (java.awt.geom.RoundRectangle2D.class.isAssignableFrom(c1))
                return false; // just in case arc's are different
            else
                return true;
        } else
            return false;
    }

    private static boolean IsSameShape(Shape s1, Shape s2) {
        if (s1 == null || s2 == null)
            return false;
        if (s1.getClass() == s2.getClass()) {
            if (s1 instanceof java.awt.geom.RoundRectangle2D) {
                RoundRectangle2D rr1 = (RoundRectangle2D) s1;
                RoundRectangle2D rr2 = (RoundRectangle2D) s2;
                return
                    rr1.getArcWidth() == rr2.getArcWidth() &&
                    rr1.getArcHeight() == rr2.getArcHeight();
            } else
                return true;
        } else
            return false;
    }

    /*
    public Rectangle2D getBounds()
    {
        Rectangle2D b = this.boundsShape.getBounds2D();
        double sw = getStrokeWidth();
        if (sw > 0) {
            double adj = sw / 2;
            b.setRect(b.getX()-adj, b.getY()-adj, b.getWidth()+sw, b.getHeight()+sw);
        }
        return b;
        //return this.boundsShape.getBounds2D();
    }
    */

    /*
      // using the default means we're only intersecting with the rectangular bounds, not the actual shape...
    protected boolean intersectsImpl(final Rectangle2D rect)
    {
        // todo: can't we generically handle in LWComponent?
        
        final Rectangle2D hitRect;
        final boolean overlaps;
        
        final float strokeWidth = getStrokeWidth();
        if (strokeWidth > 0 || isSelected()) {
            // todo opt: cache this
            final Rectangle2D.Float r = new Rectangle2D.Float();
            r.setRect(rect);
            
            // this isn't so pretty -- expanding the test rectangle to
            // compensate for the border width, but it works mostly --
            // only a little off on non-rectangular sides of shapes.
            // (todo: sharp points are problem too -- e.g, a flat diamond)
            
            float totalStroke = strokeWidth;
            if (isSelected())
                totalStroke += SelectionStrokeWidth;
            final float adj = totalStroke / 2;
            r.x -= adj;
            r.y -= adj;
            r.width += totalStroke;
            r.height += totalStroke;
            hitRect = r;
        } else
            hitRect = rect;

        overlaps = boundsShape.intersects(hitRect);
            
        if (DEBUG.PAINT && DEBUG.META) System.out.println("INTERSECTS LWNode " + hitRect + " is " + overlaps + " for " + boundsShape + " " + this);

        return overlaps;
    }
    */

    @Override
    protected boolean containsImpl(float x, float y, float zoom) {
        if (mIsRectShape) {
            // won't be perfect for round-rect at big scales, but good
            // enough, and takes into account stroke width
            return super.containsImpl(x, y, zoom);
        } else if (super.containsImpl(x, y, zoom)) {
            
            // above was a fast-reject check on the bounding box, now check the actual shape:
            
            // TODO: need to figure out a way to compenstate for stroke width on
            // arbitrary shapes.  (This is only noticable when zoomed up to massive
            // scales with large stroke widths). We could compute a connector and check
            // the distance^2 against the (strokeWidth/2)^2, and in that case we could
            // override pickDistance if we want near picking of nodes, tho I don't think
            // we need that.
            
            return boundsShape.contains(x, y);
        } else
            return false;
    }
    
    /*
    protected boolean containsImpl(float x, float y)
    {
        if (imageIcon != null) {
            return super.containsImpl(x,y);
        } else {
            if (true) {
                return boundsShape.contains(x, y);
            } else {
                // DEBUG: util irregular shapes can still give access to children
                // outside their bounds, we're checking everything in the bounding box
                // for the moment if there are any children.
                if (hasChildren())
                    return super.containsImpl(x,y);
                else if (mIsRectShape) {
                    return boundsShape.contains(x, y);
                } else {
                    float cx = x - getX();
                    float cy = y - getY();
                    // if we end up using these zillion checks, be sure to
                    // first surround with a fast-reject bounding-box check
                    return boundsShape.contains(x, y)
                        || textBoxHit(cx, cy)
                        ;
                    //|| mIconBlock.contains(cx, cy)
                }
            }
        }
    }
    */

    /*
    protected void addChildImpl(LWComponent c) {
        super.addChildImpl(c);
        if (c instanceof LWNode)
            c.setScale(getScale() * LWNode.ChildScale);
    }
    */


    @Override
    //public void addChildren(Iterator i)
    public void addChildren(Iterable i)
    {
        // todo: should be able to do this generically
        // in LWContainer and not have to override this here.
        super.addChildren(i);
        setScale(getScale()); // make sure children get shrunk
        layout();
    }

    @Override
    public void setSize(float w, float h)
    {
        if (DEBUG.LAYOUT) out("*** setSize         " + w + "x" + h);
        if (isAutoSized() && (w > this.width || h > this.height)) // does this handle scaling?
            setAutomaticAutoSized(false);
        layout(LWKey.Size,
               new Size(getWidth(), getHeight()),
               new Size(w, h));
    }

    private void setSizeNoLayout(float w, float h)
    {
        if (DEBUG.LAYOUT) out("*** setSizeNoLayout " + w + "x" + h);
        super.setSize(w, h);
        if (VUE.RELATIVE_COORDS)
            this.boundsShape.setFrame(0, 0, getWidth(), getHeight());
        else
            this.boundsShape.setFrame(getX(), getY(), getScaledWidth(), getScaledHeight());
        adjustDrawnShape();
    }

    @Override
    void setScale(double scale)
    {
        super.setScale(scale);
        if (!VUE.RELATIVE_COORDS)
            this.boundsShape.setFrame(getX(), getY(), getScaledWidth(), getScaledHeight());
    }

    @Override
    void setScaleOnChild(double parentScale, LWComponent c) {
        if (DEBUG.LAYOUT) out("setScaleOnChild " + parentScale + "*" + ChildScale + " " + c);
        if (c instanceof LWImage) {
            ; // we don't scale down images
        } else {
            if (VUE.RELATIVE_COORDS)
                c.setScale(LWNode.ChildScale);
            else
                c.setScale(parentScale * LWNode.ChildScale);
        }
    }
    public Size getMinimumSize() {
        return mMinSize;
    }
    
    private void adjustDrawnShape()
    {
        // This was to shrink the drawn shape size by border width
        // so it fits entirely inside the bounds shape, tho
        // we're not making use of that right now.
        if (DEBUG.LAYOUT) out("*** adjstDrawnShape " + getAbsoluteWidth() + "x" + getAbsoluteHeight());
        //System.out.println("boundsShape.bounds: " + boundsShape.getBounds());
        //System.out.println("drawnShape.setFrame " + x + "," + y + " " + w + "x" + h);
        this.drawnShape.setFrame(0, 0, getAbsoluteWidth(), getAbsoluteHeight());
    }

    @Override
    public void setLocation(float x, float y)
    {
        //System.out.println("setLocation " + this);
        super.setLocation(x, y);
        if (!VUE.RELATIVE_COORDS)
            this.boundsShape.setFrame(x, y, getScaledWidth(), getScaledHeight());
        //adjustDrawnShape(); // if width or height isn't changing, shouldn't need this...

        // Must lay-out children seperately from layout() -- if we
        // just call layout here we'll recurse when setting the
        // location of our children as they they try and notify us
        // back that we need to layout.
        
        layoutChildren();
    }
    
    private boolean inLayout = false;
    private boolean isCenterLayout = false;// todo: get rid of this and use mChildPos, etc for boxed layout also

    @Override
    protected void layoutImpl(Object triggerKey) {
        layout(triggerKey, new Size(getWidth(), getHeight()), null);
    }

    /**
     * @param triggerKey - the property change that triggered this layout
     * @param curSize - the current size of the node
     * @param request - the requested new size of the node
     */
    protected void layout(Object triggerKey, Size curSize, Size request)
    {
        if (inLayout) {
            if (DEBUG.Enabled) new Throwable("ALREADY IN LAYOUT " + this).printStackTrace();
            return;
        }
        inLayout = true;
        if (DEBUG.LAYOUT) {
            String msg = "*** LAYOUT, trigger="+triggerKey
                + " cur=" + curSize
                + " request=" + request
                + " isAutoSized=" + isAutoSized();
            if (true)
                Util.printClassTrace("tufts.vue.LW", msg + " " + this);
            else
                out(msg);
        }

        mIconBlock.layout(); // in order to compute the size & determine if anything showing

        if (DEBUG.LAYOUT && getLabelBox().getHeight() != getLabelBox().getPreferredSize().height) {
            // NOTE: prefHeight often a couple of pixels less than getHeight
            System.err.println("prefHeight != height in " + this);
            System.err.println("\tpref=" + getLabelBox().getPreferredSize().height);
            System.err.println("\treal=" + getLabelBox().getHeight());
        }

        // The current width & height is at this moment still a
        // "request" size -- e.g., the user may have attempted to drag
        // us to a size smaller than our minimum size.  During that
        // operation, the size of the node is momentarily set to
        // whatever the user requests, but then is immediately laid
        // out here, during which we will revert the node size to the
        // it's minimum size if bigger than the requested size.
        
        //-------------------------------------------------------
        // If we're a rectangle (rect or round rect) we use
        // layoutBoxed, if anything else, we use layoutCeneter
        //-------------------------------------------------------

        final Size min;

        if (mIsRectShape) {
            isCenterLayout = false;
            min = layoutBoxed(request, curSize, triggerKey);
            if (request == null)
                request = curSize;
        } else {
            isCenterLayout = true;
            if (request == null)
                request = curSize;
            min = layoutCentered(request);
        }

        mMinSize = new Size(min);

        if (DEBUG.LAYOUT) out("*** layout computed minimum=" + min);

        // If the size gets set to less than or equal to
        // minimize size, lock back into auto-sizing.
        if (request.height <= min.height && request.width <= min.width)
            setAutomaticAutoSized(true);
        
        final float newWidth;
        final float newHeight;

        if (isAutoSized()) {
            newWidth = min.width;
            newHeight = min.height;
        } else {
            // we always compute the minimum size, and
            // never let us get smaller than that -- so
            // only use given size if bigger than min size.
            if (request.width > min.width)
                newWidth = request.width;
            else
                newWidth = min.width;
            if (request.height > min.height)
                newHeight = request.height;
            else
                newHeight = min.height;
        }

        setSizeNoLayout(newWidth, newHeight);

        if (isCenterLayout == false) {
            // layout label last in case size is bigger than min and label is centered
            layoutBoxed_label();

            // ??? todo: cleaner move this to layoutBoxed, and have layout methods handle
            // the auto-size check (min gets set to request if request is bigger), as
            // layout_centered has to compute that now anyway.
            mIconDivider.setLine(IconMargin, MarginLinePadY, IconMargin, newHeight-MarginLinePadY);
            // mIconDivider set by layoutCentered in the other case
        }

    
        if (this.parent != null && this.parent instanceof LWMap == false) {
            // todo: should only need to do if size changed
            this.parent.layout();
        }
        
        inLayout = false;
    }

    /** @return the current size of the label object, providing a margin of error
     * on the width given sometime java bugs in computing the accurate length of a
     * a string in a variable width font. */
    
    protected Size getTextSize() {

        if (WrapText) {
            Size s = new Size(getLabelBox().getSize());
            //s.width += 3;
            return s;
        } else {

            // TODO: Check if this hack still needed in current JVM's
        
            // getSize somtimes a bit bigger thatn preferred size & more accurate
            // This is gross, but gives us best case data: we want the largest in width,
            // and smallest in height, as reported by BOTH getSize and getPreferredSize.

            Size s = new Size(getLabelBox().getPreferredSize());
            Size ps = new Size(getLabelBox().getSize());
            //if (ps.width > s.width) 
            //    s.width = s.width; // what the hell
            if (ps.height < s.height)
                s.height = ps.height;
            s.width *= TextWidthFudgeFactor;
            s.width += 3;
            return s;
        }
    }

    private int getTextWidth() {
        if (WrapText)
            return labelBox.getWidth();
        else
            return Math.round(getTextSize().width);
    }

    
    /**
     * Layout the contents of the node centered, and return the min size of the node.
     * @return the minimum rectangular size of node shape required to to contain all
     * the visual node contents
     */
    private Size layoutCentered(Size request)
    {
        NodeContent content = getLaidOutNodeContent();
        Size minSize = new Size(content);
        Size node = new Size(content);

        // Current node size is largest of current size, or
        // minimum content size.
        if (!isAutoSized()) {
            node.fit(request);
            //node.fitWidth(getWidth());
            //node.fitHeight(getHeight());
        }

        //Rectangle2D.Float content = new Rectangle2D.Float();
        //content.width = minSize.width;
        //content.height = minSize.height;

        RectangularShape nodeShape = (RectangularShape) drawnShape.clone();
        nodeShape.setFrame(0,0, content.width, content.height);
        //nodeShape.setFrame(0,0, minSize.width, minSize.height);
        
        // todo perf: allow for skipping of searching for minimum size
        // if current size already big enough for content

        // todo: we shouldn't even by trying do a layout if have no children or label...
        if ((hasLabel() || hasChildren()) && growShapeUntilContainsContent(nodeShape, content)) {
            // content x/y is now at the center location of our MINUMUM size,
            // even tho our current size may be bigger and require moving it..
            minSize.fit(nodeShape);
            node.fit(minSize);
        }

        //Size text = getTextSize();
        //mLabelPos.x = content.x + (((float)nodeShape.getWidth()) - text.width) / 2;
        //mLabelPos.x = content.x + (node.width - text.width) / 2;

        nodeShape.setFrame(0,0, node.width, node.height);
        layoutContentInShape(nodeShape, content);
        if (DEBUG.LAYOUT) out("*** content placed at " + content + " in " + nodeShape);

        content.layoutTargets();
        
        return minSize;
    }

    /**
     * Brute force increase the size of the given arbitrary shape until it's borders fully
     * contain the given rectangle when it is centered in the shape.  Algorithm starts
     * with size of content for shape (which would work it it was rectangular) then increases
     * width & height incrememntally by %10 until content is contained, then backs off 1 pixel
     * at a time to tighten the fit.
     *
     * @param shape - the shape to grow: expected be zero based (x=y=0)
     * @param content - the rectangle to ensure we can contain (x/y is ignored: it's x/y value at end will be centered)
     * @return true if the shape was grown
     */
    private boolean growShapeUntilContainsContent(RectangularShape shape, NodeContent content)
    {
        final int MaxTries = 1000; // in case of loops (e.g., a broke shape class whose contains() never succeeds)
        final float increment;
        if (content.width > content.height)
            increment = content.width * 0.1f;
        else
            increment = content.height * 0.1f;
        final float xinc = increment;
        final float yinc = increment;
        //final float xinc = content.width * 0.1f;
        //final float yinc = (content.height / content.width) * xinc;
        //System.out.println("xinc=" + xinc + " yinc=" + yinc);
        int tries = 0;
        while (!shape.contains(content) && tries < MaxTries) {
        //while (!content.fitsInside(shape) && tries < MaxTries) {
            shape.setFrame(0, 0, shape.getWidth() + xinc, shape.getHeight() + yinc);
            //System.out.println("trying size " + shape + " for content " + content);
            layoutContentInShape(shape, content);
            tries++;
        }
        if (tries > 0) {
            final float shrink = 1f;
            if (DEBUG.LAYOUT) System.out.println("Contents of " + shape + "  rought  fit  to " + content + " in " + tries + " tries");
            do {
                shape.setFrame(0, 0, shape.getWidth() - shrink, shape.getHeight() - shrink);
                //System.out.println("trying size " + shape + " for content " + content);
                layoutContentInShape(shape, content);
                tries++;
                //} while (shape.contains(content) && tries < MaxTries);
            } while (content.fitsInside(shape) && tries < MaxTries);
            shape.setFrame(0, 0, shape.getWidth() + shrink, shape.getHeight() + shrink);
            //layoutContentInShape(shape, content);

            /*
            if (getLabel().indexOf("*s") >= 0) {
            do {
                shape.setFrame(0, 0, shape.getWidth(), shape.getHeight() - shrink);
                tries++;
            } while (content.fitsInside(shape) && tries < MaxTries);
            shape.setFrame(0, 0, shape.getWidth(), shape.getHeight() + shrink);
            }

            if (getLabel().indexOf("*ml") >= 0) {
            do {
                shape.setFrame(0, 0, shape.getWidth(), shape.getHeight() - shrink);
                tries++;
            } while (content.fitsInside(shape) && tries < MaxTries);
            shape.setFrame(0, 0, shape.getWidth(), shape.getHeight() + shrink);
            }
            */
            
        }
        
        if (tries >= MaxTries) {
            System.err.println("Contents of " + shape + " failed to contain " + content + " after " + tries + " tries.");
        } else if (tries > 0) {
            if (DEBUG.LAYOUT) System.out.println("Contents of " + shape + " grown to contain " + content + " in " + tries + " tries");
        } else
            if (DEBUG.LAYOUT) System.out.println("Contents of " + shape + " already contains " + content);
        if (DEBUG.LAYOUT) out("*** content minput at " + content + " in " + shape);
        return tries > 0;
    }
    
    /**
     * Layout the given content rectangle in the given shape.  The default is to center
     * the content rectangle in the shape, however, if the shape in an instance
     * of tufts.vue.shape.RectangularPoly2D, it will call getContentGravity() to
     * determine layout gravity (CENTER, NORTH, EAST, etc).
     *
     * @param shape - the shape to layout the content in
     * @param content - the region to layout in the shape: x/y values will be set
     *
     * @see tufts.vue.shape.RectangularPoly2D, 
     */
    private void layoutContentInShape(RectangularShape shape, NodeContent content)
    {
        final float width = (float) shape.getWidth();
        final float height = (float) shape.getHeight();
        final float margin = 0.5f; // safety so 100% sure will be in-bounds
        boolean content_laid_out = false;

        if (shape instanceof RectangularPoly2D) {
            int gravity = ((RectangularPoly2D)shape).getContentGravity();
            content_laid_out = true;
            if (gravity == RectangularPoly2D.CENTER) {
                content.x = (width - content.width) / 2;
                content.y = (height - content.height) / 2;
            } else if (gravity == RectangularPoly2D.EAST) {
                content.x = margin;
                content.y = (float) (height - content.height) / 2;
            } else if (gravity == RectangularPoly2D.WEST) {
                content.x = (width - content.width) + margin;
                content.y = (float) Math.floor((height - content.height) / 2);
            } else if (gravity == RectangularPoly2D.NORTH) {
                content.x = (width - content.width) / 2;
                content.y = margin;
            } else if (gravity == RectangularPoly2D.SOUTH) {
                content.x = (width - content.width) / 2;
                content.y = (height - content.height) - margin;
            } else {
                System.err.println("Unsupported content gravity " + gravity + " on shape " + shape + "; defaulting to CENTER");
                content_laid_out = false;
            }
        }
        if (!content_laid_out) {
            // default is center layout
            content.x = (width - content.width) / 2;
            content.y = (height - content.height) / 2;
        }
    }

    /**
     * Provide a center-layout frame work for all the node content.
     * Constructing this object creates the layout.  It get's sizes
     * for all the potential regions in the node (icon block, label,
     * children) and lays out those regions relative to each other,
     * contained in a single rectangle.  Then that containging
     * rectangle can be used to quickly compute the the size of a
     * non-rectangular node shape required to enclose it completely.
     * Layout of the actual underlying targets doesn't happen until
     * layoutTargets() is called.
     */
    private class NodeContent extends Rectangle2D.Float {

        // regions for icons, label & children
        private Rectangle2D.Float rIcons;
        private Rectangle2D.Float rLabel = new Rectangle2D.Float();
        private Rectangle2D.Float rChildren;

        /**
         * Initial position is 0,0.  Regions are all normalized to offsets from 0,0.
         * Construct node content layout object: layout the regions for
         * icons, label & children.  Does NOT do the final layout (setting
         * LWNode member variables, laying out the child nodes, etc, until
         * layoutTargts() is called).
         */
        NodeContent()
        {
            if (hasLabel()) {
                Size text = getTextSize();
                rLabel.width = text.width;
                rLabel.height = text.height;
                rLabel.x = ChildPadX;
                this.width = ChildPadX + text.width;
                this.height = text.height;
            } 
            if (iconShowing()) {
                rIcons = new Rectangle2D.Float(0, 0, mIconBlock.width, mIconBlock.height);
                this.width += mIconBlock.width;
                this.width += ChildPadX; // add space at right of label to match space at left
                // move label to right to make room for icon block at left
                rLabel.x += mIconBlock.width;
            }
            if (hasChildren()) {
                Size children = layoutChildren(new Size(), 0f, true);
                //float childx = rLabel.x + ChildPadX;
                float childx = rLabel.x;
                float childy = rLabel.height + ChildPadY;
                rChildren = new Rectangle2D.Float(childx,childy, children.width, children.height);

                // can set absolute height based on label height & children height
                this.height = rLabel.height + ChildPadY + children.height;

                // make sure we're wide enough for the children in case children wider than label
                fitWidth(rLabel.x + children.width); // as we're 0 based, rLabel.x == width of gap at left of children
            }
            
            if (rIcons != null) {
                fitHeight(mIconBlock.height);

                if (mIconBlock.height < height) {
                    // vertically center icon block if less than total height
                    rIcons.y = (height - rIcons.height) / 2;
                } else if (height > rLabel.height && !hasChildren()) {
                    // vertically center the label if no children & icon block is taller than label
                    rLabel.y = (height - rLabel.height) / 2;
                }
            }
        }

        /** do the center-layout for the actual targets (LWNode state) of our regions */
        void layoutTargets() {
            if (DEBUG.LAYOUT) out("*** laying out targets");
            mLabelPos.setLocation(x + rLabel.x, y + rLabel.y);
            if (rIcons != null) {
                mIconBlock.setLocation(x + rIcons.x, y + rIcons.y);
                // Set divider line to height of the content, at right of icon block
                mIconDivider.setLine(mIconBlock.x + mIconBlock.width, this.y,
                                     mIconBlock.x + mIconBlock.width, this.y + this.height);
            }
            if (rChildren != null) {
                mChildPos.setLocation(x + rChildren.x, y + rChildren.y);
                layoutChildren();
            }
        }
        
        /** @return true if all of the individual content items, as currently positioned, fit
            inside the given shape.  Note that this may return true even while outer dimensions
            of the NodeContent do NOT fit inside the shape: it's okay to clip corners of
            the NodeContent box as long as the individual components still fit: the NodeContent
            box is used for <i>centering</i> the content in the bounding box of the shape,
            and for the initial rough estimate of an enclosing shape.
        */
        private Rectangle2D.Float checker = new Rectangle2D.Float();
        boolean fitsInside(RectangularShape shape) {
            //return shape.contains(this);
            boolean fit = true;
            copyTranslate(rLabel, checker, x, y);
            fit &= shape.contains(checker);
            //System.out.println(this + " checked " + VueUtil.out(shape) + " for label " + VueUtil.out(rLabel) + " RESULT=" + fit);
            if (rIcons != null) {
                copyTranslate(rIcons, checker, x, y);
                fit &= shape.contains(checker);
                //System.out.println("Contains    icons: " + fit);
            }
            if (rChildren != null) {
                copyTranslate(rChildren, checker, x, y);
                fit &= shape.contains(checker);
                //System.out.println("Contains children: " + fit);
            }
            return fit;
        }

        private void copyTranslate(Rectangle2D.Float src, Rectangle2D.Float dest, float xoff, float yoff) {
            dest.width = src.width;
            dest.height = src.height;
            dest.x = src.x + xoff;
            dest.y = src.y + yoff;
        }

        private void fitWidth(float w) {
            if (width < w)
                width = w;
        }
        private void fitHeight(float h) {
            if (height < h)
                height = h;
        }
        
        public String toString() {
            return "NodeContent[" + VueUtil.out(this) + "]";
        }
    }

    /** 
     * @return internal node content already laid out
     */
    
    private NodeContent _lastNodeContent;
    /** get a center-layout framework */
    private NodeContent getLaidOutNodeContent()
    {
        return _lastNodeContent = new NodeContent();
    }

    private Size layoutBoxed(Size request, Size oldSize, Object triggerKey) {
        final Size min;
        
        if (WrapText)
            min = layoutBoxed_floating_text(request, oldSize, triggerKey);
        else
            min = layoutBoxed_vanilla(request);

        return min;

    }

    
    /** @return new minimum size of node */
    private Size layoutBoxed_vanilla(final Size request)
    {
        final Size min = new Size();
        final Size text = getTextSize();

        min.width = text.width;
        min.height = EdgePadY + text.height + EdgePadY;

        // *** set icon Y position in all cases to a centered vertical
        // position, but never such that baseline is below bottom of
        // first icon -- this is tricky tho, as first icon can move
        // down a bit to be centered with the label!

        if (!iconShowing()) {
            min.width += LabelPadLeft;
        } else {
            float dividerY = EdgePadY + text.height;
            //double stubX = LabelPositionXWhenIconShowing + (text.width * TextWidthFudgeFactor);
            double stubX = LabelPositionXWhenIconShowing + text.width;
            double stubHeight = DividerStubAscent;
            
            ////dividerUnderline.setLine(0, dividerY, stubX, dividerY);
            //dividerUnderline.setLine(IconMargin, dividerY, stubX, dividerY);
            //dividerStub.setLine(stubX, dividerY, stubX, dividerY - stubHeight);

            ////height = PadTop + (float)dividerY + IconDescent; // for aligning 1st icon with label bottom
            min.width = (float)stubX + IconPadLeft; // be symmetrical with left padding
            //width += IconPadLeft;
        }

        if (hasChildren()) {
            if (DEBUG.LAYOUT) out("*** textSize b4 layoutBoxed_children: " + text);
            layoutBoxed_children(min, text);
        }
        
        if (iconShowing())
            layoutBoxed_icon(request, min, text);
        
        return min;
    }

    /** set mLabelPos */
    private void layoutBoxed_label()
    {
        Size text = getTextSize();
        
        if (hasChildren()) {
            mLabelPos.y = EdgePadY;
        } else {
            // only need this in case of small font sizes and an icon
            // is showing -- if so, center label vertically in row with the first icon
            // Actually, no: center in whole node -- gak, we really want both,
            // but only to a certian threshold -- what a hack!
            //float textHeight = getLabelBox().getPreferredSize().height;
            //mLabelPos.y = (this.height - textHeight) / 2;
            mLabelPos.y = (this.height - text.height) / 2;
        }

        if (iconShowing()) {
            //layoutBoxed_icon(request, min, newTextSize);
            // TODO:
            // need to center label between the icon block and the RHS
            // we currently need more space at the RHS.
            // does relativeLabelX even use this in this case?
            // really: do something that isn't a total freakin hack like all our current layout code.
            //mLabelPos.x = LabelPositionXWhenIconShowing;
            mLabelPos.x = -100;  // marked bad because should never see this this: is IGNORED if icon is showing
        } else {
            //-------------------------------------------------------
            // horizontally center if no icons
            //-------------------------------------------------------
            if (WrapText)
                mLabelPos.x = (this.width - text.width) / 2 + 1;
            else
                mLabelPos.x = 200; // marked bad because unused in this case
        }
        
    }

    //----------------------------------------------------------------------------------------
    // Crap.  We need the max child width first to know the min width for wrapped text,
    // but then need the text height to compute the child X location.
    //----------------------------------------------------------------------------------------

    /** will CHANGE min.width and min.height */ 
    private void layoutBoxed_children(Size min, Size labelText) {
        if (DEBUG.LAYOUT) out("*** layoutBoxed_children; min=" + min + " text=" + labelText);

        mBoxedLayoutChildY = EdgePadY + labelText.height; // must set before layoutChildren, as may be used in childOffsetY()

        float minWidth;
        if (false && isPresentationContext()) {
            minWidth = Math.max(labelText.width, getWidth()-20);
            // Prob will have to just let it compute max child width, then center
            // the whole child box in the node (this isn't letting shrink node via drag-resize properly,
            // even with a 20px margin of error...)
        } else
            minWidth = 0;
        
        final Size children = layoutChildren(new Size(), minWidth, false);
        final float childSpan = childOffsetX() + children.width + ChildPadX;

        if (min.width < childSpan)
            min.width = childSpan;
        
        /*
        if (isPresentationContext()) {
            if (min.width < text.width)
                min.width = text.width;
        }
        */
        
        min.height += children.height;
        min.height += ChildOffsetY + ChildrenPadBottom; // additional space below last child before bottom of node
    }

    // good for single column layout only.  layout code is in BAD NEED of complete re-architecting.
    protected float getMaxChildSpan()
    {
        java.util.Iterator i = getChildIterator();
        float maxWidth = 0;
        
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            float w = c.getBoundsWidth();
            if (w > maxWidth)
                maxWidth = w;
        }
        return childOffsetX() + maxWidth + ChildPadX;
    }
    

    /** will CHANGE min */
    private void layoutBoxed_icon(Size request, Size min, Size text) {

        if (DEBUG.LAYOUT) out("*** layoutBoxed_icon");
        
        float iconWidth = IconWidth;
        float iconHeight = IconHeight;
        float iconX = IconPadLeft;
        //float iconY = dividerY - IconAscent;
        //float iconY = dividerY - iconHeight; // align bottom of 1st icon with bottom of label
        //float iconY = PadTop;

        /*
          if (iconY < IconMinY) {
          // this can happen if font size is very small when
          // alignining the first icon with the bottom of the text label
          iconY = IconMinY;
          dividerY = iconY + IconAscent;
          }
        */

        float iconPillarX = iconX;
        float iconPillarY = IconPillarPadY;
        //iconPillarY = EdgePadY;

        //float totalIconHeight = icons * IconHeight;
        float totalIconHeight = (float) mIconBlock.getHeight();
        float iconPillarHeight = totalIconHeight + IconPillarPadY * 2;


        if (min.height < iconPillarHeight) {
            min.height += iconPillarHeight - min.height;
        } else if (mIsRectShape) {
            // special case prettification -- if vertically centering
            // the icon stack would only drop it down by up to a few
            // pixels, go ahead and do so because it's so much nicer
            // to look at.
            float centerY = (min.height - totalIconHeight) / 2;
            if (centerY > IconPillarPadY+IconPillarFudgeY)
                centerY = IconPillarPadY+IconPillarFudgeY;
            iconPillarY = centerY;
        }
            
        if (!mIsRectShape) {
            float height;
            if (isAutoSized())
                height = min.height;
            else
                height = Math.max(min.height, request.height);
            iconPillarY = height / 2 - totalIconHeight / 2;
        }
            
        mIconBlock.setLocation(iconPillarX, iconPillarY);

    }

    /**
     * @param curSize - if non-null, re-layout giving priority to currently requested size (getWidth/getHeight)
     * if null, give priority to keeping the existing TexBox as unchanged as possible.
     *
     * @param request - requested size -- can be null, which means adjust size because something changed
     * @param curSize - the current/old size of the node, in case it's already been resized
     *
     *
     * @return new size of node, resizing the text box as needed -- because we're laying out
     * text, this is NOT the minimum size of the node: it includes request size
     */

    // TODO: need to have a special curSize that is the uninitialized size,
    // either that or a requestSize that is a special "natural" size, and in
    // this special case, put all on one line if width isn't "too big", (e.g.,
    // at least "Node Node" for sure), or if is really big (e.g., drop of a big
    // text clipping) set to some default preferred aspect, such as 3/4, or perhaps
    // the current screen aspect).

    // TODO: PROBLEM: if children wider than label, label is NOT STABLE!  TextBox can be dragged
    // full width of node, but then snaps back to min-size on re-layout!

    // todo: may not need all three args
    private Size layoutBoxed_floating_text(Size request, Size curSize, Object triggerKey)
    {
        if (DEBUG.LAYOUT) out("*** layoutBoxed_floating_text, req="+request + " cur=" + curSize + " trigger=" + triggerKey);

        final Size min = new Size(); // the minimum size of the Node

        getLabelBox(); // make sure labelBox is set

        //------------------------------------------------------------------
        // start building up minimum width & height
        //------------------------------------------------------------------
        
        if (iconShowing())
            min.width = LabelPositionXWhenIconShowing;
        else
            min.width = LabelPadLeft;
        min.width += LabelPadRight;
        min.height = EdgePadY + EdgePadY;

        final float textPadWidth = min.width;
        final float textPadHeight = min.height;

        //------------------------------------------------------------------
        // adjust minimum width & height for text size and requested size
        //------------------------------------------------------------------
        
        final Size newTextSize;
        final boolean resizeRequest;

        // resizeRequest is true if we're requesting a new size for
        // this node, otherwise, resizeRequest is false and some
        // property is changing that may effect the size of the node

        if (request == null) {
            resizeRequest = false;
            request = curSize;
        } else
            resizeRequest = true;

        if (hasChildren())
            request.fitWidth(getMaxChildSpan());

        //if (request.width <= MIN_SIZE && request.height <= MIN_SIZE) {
        if (curSize.width == NEEDS_DEFAULT) { // NEEDS_DEFAULT meaningless now: will never be true (oh, only on restore?)
            if (DEBUG.WORK) out("SETTING DEFAULT - UNITIALIZED WIDTH");
            // usually this happens with a new node
            newTextSize = new Size(labelBox.getPreferredSize());
        } else if (textSize == null) {
            if (DEBUG.WORK) out("SETTING DEFAULT - NO TEXT SIZE");
            newTextSize = new Size(labelBox.getPreferredSize());
        } else {
            //newTextSize = new Size();
            newTextSize = new Size(textSize);

            //if (triggerKey == LWKey.Size) {
            if (resizeRequest) {
                // ADJUST TEXT TO FIT NODE

                // fit the text to the new size as best we can.
                // (we're most likely drag-resizing the node)
                
                newTextSize.width = request.width - textPadWidth;
                newTextSize.height = request.height - textPadHeight;
                newTextSize.fitWidth(labelBox.getMaxWordWidth());


            } else {
                // ADJUST NODE TO FIT TEXT

                // adjust node size around text size
                // e.g., we changed font: trust that the labelBox is already sized as it needs to
                // be and size the node around it.
                
                //if (triggerKey == LWKey.Font && isAutoSized()) {
                //if (false && triggerKey == LWKey.Font) {
                if (true) {
                    // this should work even if our current width is > maxWordWidth
                    // and not matter if we're auto-sized or not: we just want
                    // to force an increase in the width only
                    
                    // So what's the new width?

                    // When NEWLINES are in text, preferred width is width of longest LINE.
                    // So actually, preferred with is always width of longest line.

                    // So how to handle the one-line that's been wrapped case?
                    // (a single UNWRAPPED line would in fact just use preferred size for new width in, eg., a bold font)

                    // Okay, either we're going to have to eat that case,
                    // (or delve into TextUI, etc: forget that!), or we
                    // could seek out the right width by slowly increasing it
                    // until preferred height comes to match the old preferred height...

                    // Or maybe, in fact, we don't want to do anything?  Could go either
                    // way: which is more important: the current size of the node
                    // or the current breaks in the text?  Can we do this only
                    // if autoSized?  Is autoSized even possible when text is wrapped?
                    // (and if not, we're not handling that right).  AND, autoSized
                    // may effect the hard-line-breaks case we think we have handled above...

                    // note that restoring wrapped text isn't working right now either...

                    // basically, we're trying to have a new kind of autoSized, which remembers
                    // the current user size, but on ADJUSTMENT does different things.

                    // AT LEAST: if our old txt width is equal to old max word width,
                    // then keep that same relationship here.

                    boolean keepPreferredWidth = false;
                    boolean keepMaxWordWidth = false;
                    final int curWidth = labelBox.getWidth();

                    // damn! if font set, labelBox preferred and max word width is already adjusted!
                    
                    if (curWidth == labelBox.getPreferredSize().width)
                        keepPreferredWidth = true;
                    else if (curWidth == labelBox.getMaxWordWidth())
                        keepMaxWordWidth = true;
                    
                    newTextSize.width = labelBox.getMaxWordWidth();
                } else {
                    newTextSize.width = labelBox.getWidth();
                    newTextSize.fitWidth(labelBox.getMaxWordWidth());
                }
                newTextSize.height = labelBox.getHeight();

            }
        }

        
        labelBox.setSizeFlexHeight(newTextSize);
        newTextSize.height = labelBox.getHeight();
        this.textSize = newTextSize.dim();
        
        min.height += newTextSize.height;
        min.width += newTextSize.width;

        //-------------------------------------------------------
        // Now that we have our minimum width and height, layout
        // the label and any icons.
        //-------------------------------------------------------

        if (hasChildren()) {
            layoutBoxed_children(min, newTextSize);
            /*
            if (mChildScale != ChildScale || request.height > min.height) {
                // if there's extra space, zoom all children to occupy it
                mChildScale = request.height / min.height;
                if (DEBUG.LAYOUT) out("*** expanded childScale to " + mChildScale);
            } else {
                mChildScale = ChildScale;
            }
            */
        }

        if (iconShowing())
            layoutBoxed_icon(request, min, newTextSize);
            
        return min;
    }

    /** override's superclass impl of {@link XMLUnmarshalListener} -- fix's up text size */
    /*
    public void XML_completed() {
        if (textSize != null)
            getLabelBox().setSize(textSize);
        super.XML_completed();
    }
    */
    
    //private float mChildScale = ChildScale;

    /**
     * Need to be able to do this seperately from layout -- this
     * get's called everytime a node's location is changed so
     * that's it's children will follow along with it.
     *
     * Children are laid out relative to the parent, but given
     * absolute map coordinates.  Note that because if this, anytime
     * we're computing a location for a child, we have to factor in
     * the current scale factor of the parent.
     */
    
    void layoutChildren() {
        layoutChildren(null, 0f, false);
    }
    
    // for computing size only
    private Size layoutChildren(Size result) {
        return layoutChildren(0f, 0f, 0f, result);
    }

    //private Rectangle2D child_box = new Rectangle2D.Float(); // for debug
    private Size layoutChildren(Size result, float minWidth, boolean sizeOnly)
    {
        if (DEBUG.LAYOUT) out("*** layoutChildren; sizeOnly=" + sizeOnly);
        
        if (!hasChildren())
            return Size.None;

        float baseX = 0;
        float baseY = 0;

        if (!sizeOnly) {
            baseX = childOffsetX();
            baseY = childOffsetY();
//             if (VUE.RELATIVE_COORDS) {
//                 baseX = childOffsetX();
//                 baseY = childOffsetY();
//             } else {
//                 baseX = getX() + childOffsetX() * getScaleF();
//                 baseY = getY() + childOffsetY() * getScaleF();
//             }
        }

        return layoutChildren(baseX, baseY, minWidth, result);
    }
        
    private void layoutChildren(float baseX, float baseY) {
        layoutChildren(baseX, baseY, 0f, null);
    }
    
    private Size layoutChildren(float baseX, float baseY, float minWidth, Size result)
    {
        if (DEBUG.LAYOUT) out("*** layoutChildren at " + baseX + "," + baseY);
        if (DEBUG.LAYOUT && DEBUG.META) Util.printClassTrace("tufts.vue.LW", "*** layoutChildren");
        //if (baseX > 0) new Throwable("LAYOUT-CHILDREN").printStackTrace();
//         if (isPresentationContext())
//             layoutChildrenGrid(baseX, baseY, result, 1, minWidth);
//         else
            layoutChildrenSingleColumn(baseX, baseY, result);

        if (result != null) {
            if (!VUE.RELATIVE_COORDS) {
                result.width /= getScale();
                result.height /= getScale();
            }
            //if (DEBUG.BOXES)
            //child_box.setRect(baseX, baseY, result.width, result.height);
        }
        return result;
    }

        
    protected void layoutChildrenSingleColumn(float baseX, float baseY, Size result)
    {
        float y = baseY;
        float maxWidth = 0;
        boolean first = true;

        for (LWComponent c : getChildList()) {
            if (c instanceof LWLink) // todo: don't allow adding of links into a manged layout node!
                continue;
            if (first)
                first = false;
            else
                y += ChildVerticalGap * getScale();
            if (c.hasAbsoluteMapLocation()) {
                // this is a hack only for old maps that might have groups inside of nodes --
                // try and do something reasonable...  this isn't all there tho.
                c.setLocation(baseX + getX(),
                              y + getY());
            } else
                c.setLocation(baseX, y);
            y += c.getScaledHeight();

            if (result != null) {
                // track max width
                float w = c.getScaledBoundsWidth();
                if (w > maxWidth)
                    maxWidth = w;
            }
        }

        if (result != null) {
            result.width = maxWidth;
            result.height = (y - baseY);
        }
    }

    class Column extends java.util.ArrayList<LWComponent>
    {
        float width;
        float height;

        Column(float minWidth) {
            width = minWidth;
        }

        void layout(float baseX, float baseY, boolean center)
        {
            float y = baseY;
            Iterator i = iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (center)
                    c.setLocation(baseX + (width - c.getBoundsWidth())/2, y);
                else
                    c.setLocation(baseX, y);
                y += c.getHeight();
                y += ChildVerticalGap * getScale();
                // track size
                //float w = c.getBoundsWidth();
                //if (w > width)
                //  width = w;
            }
            height = y - baseY;
        }

        void addChild(LWComponent c)
        {
            super.add(c);
            float w = c.getBoundsWidth();
            if (w > width)
                width = w;
        }
    }

    // If nColumn == 1, it does center layout.  minWidth only meant for single column
    protected void layoutChildrenGrid(float baseX, float baseY, Size result, int nColumn, float minWidth)
    {
        float y = baseY;
        float totalWidth = 0;
        float maxHeight = 0;
        
        Column[] cols = new Column[nColumn];
        java.util.Iterator i = getChildIterator();
        int curCol = 0;
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (cols[curCol] == null)
                cols[curCol] = new Column(minWidth);
            cols[curCol].addChild(c);
            if (++curCol >= nColumn)
                curCol = 0;
        }

        float colX = baseX;
        float colY = baseY;
        for (int x = 0; x < cols.length; x++) {
            Column col = cols[x];
            if (col == null)
                break;
            col.layout(colX, colY, nColumn == 1);
            colX += col.width + ChildHorizontalGap;
            totalWidth += col.width + ChildHorizontalGap;
            if (col.height > maxHeight)
                maxHeight = col.height;
        }
        // peel back the last gap as no more columns to right
        totalWidth -= ChildHorizontalGap;

        if (result != null) {
            result.width = totalWidth;
            result.height = maxHeight;
        }
    }

    @Override
    public float getLabelX()
    {
        return getMapX() + relativeLabelX() * getMapScaleF();
    }
    
    @Override
    public float getLabelY()
    {
        return getMapY() + relativeLabelY() * getMapScaleF();
        /*
        if (this.labelBox == null)
            return getY() + relativeLabelY();
        else
            return (getY() + relativeLabelY()) - this.labelBox.getHeight();
        */
    }

    private static final AlphaComposite ZoomTransparency = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);

    @Override
    public Color getRenderFillColor(DrawContext dc)
    {
        if (DEBUG.LAYOUT) if (!isAutoSized()) return Color.green; // LAYOUT-NEW

        if (dc == null || dc.focal == this) // TextBox or presentation focal
            return super.getRenderFillColor(dc);
        
        // TODO: cleanup using new ColorProperty methods & super.getRenderFillColor with drawContext
        // Also add/move Util.darkColor to ColorProperty
        Color fillColor = super.getFillColor();
        if (getParent() instanceof LWNode) {
            if (fillColor != null) {
                Color parentFill = getParent().getRenderFillColor(dc);
                if (parentFill != null && !parentFill.equals(Color.black) && parentFill.getAlpha() != 0 && fillColor.equals(parentFill)) {
                    // If our fill is the same as our parents, we darken it, unless our parent is already entirely black,
                    // or entirely transparent.
                    fillColor = VueUtil.darkerColor(fillColor);
                }
            }
        }
        return fillColor;
    }
    
    @Override
    protected void drawImpl(DrawContext dc)
    {
        if (!isFiltered()) {
            // Desired functionality is that if this node is filtered, we don't draw it, of course.
            // But also, even if this node is filtered, we still draw any children who are
            // NOT filtered -- we just drop out the parent background.
            drawNode(dc);
        }

        //-------------------------------------------------------
        // Draw any children
        //-------------------------------------------------------

        if (hasChildren()) {
            if (isZoomedFocus())
                dc.g.setComposite(ZoomTransparency);
            super.drawChildren(dc);
        }
    }
        
    protected void drawNode(DrawContext dc)
    {
        //-------------------------------------------------------
        // Fill the shape (if it's not transparent)
        //-------------------------------------------------------
        
        if (isSelected() && dc.isInteractive() && dc.focal != this) {
            LWPathway p = VUE.getActivePathway();
            if (p != null && p.isVisible() && p.getCurrentNode() == this) {
                // SPECIAL CASE:
                // as the current element on the current pathway draws a huge
                // semi-transparent stroke around it, skip drawing our fat 
                // transparent selection stroke on this node.  So we just
                // do nothing here.
            } else {
                dc.g.setColor(COLOR_HIGHLIGHT);
                dc.g.setStroke(new BasicStroke(getStrokeWidth() + SelectionStrokeWidth));
                //g.setStroke(new BasicStroke(stroke.getLineWidth() + SelectionStrokeWidth));
                dc.g.draw(drawnShape);
            }
        }
        
        if (imageIcon != null) {
            // experimental
            //imageIcon.paintIcon(null, g, (int)getX(), (int)getY());
            imageIcon.paintIcon(null, dc.g, 0, 0);
        } else if (false && (dc.isPresenting() || isPresentationContext())) { // old-style "turn off the wrappers"
            ; // do nothing: no fill
        } else {
            Color fillColor = getRenderFillColor(dc);
            if (fillColor != null && fillColor.getAlpha() != 0) { // transparent if null
                dc.g.setColor(fillColor);
                if (isZoomedFocus())
                    dc.g.setComposite(ZoomTransparency);
                dc.g.fill(drawnShape);
                if (isZoomedFocus())
                    dc.g.setComposite(AlphaComposite.Src);
            }
        }

        /*
        if (!isAutoSized()) { // debug
            g.setColor(Color.green);
            g.setStroke(STROKE_ONE);
            g.draw(drawnShape);
        }
        else if (false&&isRollover()) { // debug
            // temporary debug
            //g.setColor(new Color(0,0,128));
            g.setColor(Color.blue);
            g.draw(drawnShape);
        }
        else*/
        
        if (getStrokeWidth() > 0 /*&& !isPresentationContext() && !dc.isPresenting()*/) { // old style "turn off the wrappers"
            //if (LWSelection.DEBUG_SELECTION && isSelected())
            //if (isSelected())
            //g.setColor(COLOR_SELECTION);
            //else
                dc.g.setColor(getStrokeColor());
            dc.g.setStroke(this.stroke);
            dc.g.draw(drawnShape);
        }


        if (DEBUG.BOXES) {
            dc.setAbsoluteStroke(0.5);
            //if (hasChildren()) dc.g.draw(child_box);
            if (false && _lastNodeContent != null && !mIsRectShape) {
                dc.g.setColor(Color.darkGray);
                dc.g.draw(_lastNodeContent);
            } else {
                dc.g.setColor(Color.blue);
                dc.g.draw(this.drawnShape);
            }
        }
            
        //-------------------------------------------------------
        // Draw the generated icon
        //-------------------------------------------------------

        drawNodeDecorations(dc);

        // todo: create drawLabel, drawBorder & drawBody
        // LWComponent methods so can automatically turn
        // this off in MapViewer, adjust stroke color for
        // selection, etc.
        
        // TODO BUG: label sometimes getting "set" w/out sending layout event --
        // has to do with case where we pre-fill a textbox with "label", and
        // if they type nothing we don't set a label, but that's not working
        // entirely -- it manages to not trigger an update event, but somehow
        // this.label is still getting set -- maybe we have to null it out
        // manually (and maybe labelBox also)
        
        if (hasLabel() && this.labelBox != null && this.labelBox.getParent() == null) {
            
            // if parent is not null, this box is an active edit on the map
            // and we don't want to paint it here as AWT/Swing is handling
            // that at the moment (and at a possibly slightly different offset)

            drawLabel(dc);
        }

    }

    protected void drawLabel(DrawContext dc)
    {
        float lx = relativeLabelX();
        float ly = relativeLabelY();
        dc.g.translate(lx, ly);
        //if (DEBUG.CONTAINMENT) System.out.println("*** " + this + " drawing label at " + lx + "," + ly);
        this.labelBox.draw(dc);
        dc.g.translate(-lx, -ly);
        
        // todo: this (and in LWLink) is a hack -- can't we
        // do this relative to the node?
        //this.labelBox.setMapLocation(getX() + lx, getY() + ly);
    }

    /*
    public void XX_drawChild(LWComponent child, DrawContext dc)
    {
        // can use this if children could ever do anything to the scale
        // (thus we'd need to protect each child from changes made
        // by others)
        //child.draw(dc.createScaled(ChildScale));
    }
    
    public void X_drawChild(LWComponent child, DrawContext dc)
    {
        //Graphics2D g = dc.g;
        //g.translate(childBaseX * ChildScale, childBaseY * ChildScale);
        // we double the translation because the translation done by
        // the child will happen in a shrunk context -- but that only works if ChildScale == 0.5!
        //g.translate((double)child.getX() * ChildScale, (double)child.getY() * ChildScale);
        dc.g.translate(child.getX(), child.getY());
        dc.g.scale(ChildScale, ChildScale);
        child.draw(dc);
        //g.translate(-childBaseX, -childBaseY);
    }

    public LWComponent relative_findLWComponentAt(float mapX, float mapY)
    {
        if (DEBUG_CONTAINMENT) System.out.println("LWCNode.findLWComponentAt[" + getLabel() + "]");
        // hit detection must traverse list in reverse as top-most
        // components are at end
        java.util.ListIterator i = children.listIterator(children.size());

        mapX -= getX() + childBaseX;
        mapY -= getY() + childBaseY;
        mapX /= ChildScale;
        mapY /= ChildScale;
        while (i.hasPrevious()) {
            LWComponent c = (LWComponent) i.previous();
            if (c.contains(mapX, mapY)) {
                if (c.hasChildren())
                    return ((LWContainer)c).findLWComponentAt(mapX, mapY);
                else
                    return c;
            }
        }
        return this;
    }
    */
    

    private void drawNodeDecorations(DrawContext dc)
    {
        final Graphics2D g = dc.g;

        /*
        if (DEBUG.BOXES && mIsRectShape) {
            //-------------------------------------------------------
            // paint a divider line
            //-------------------------------------------------------
            g.setColor(Color.gray);
            dc.setAbsoluteStroke(0.5);
            g.draw(dividerUnderline);
            g.draw(dividerStub);
        }
        */
            
        //-------------------------------------------------------
        // paint the node icons
        //-------------------------------------------------------

        if (/*!dc.isPresenting() &&*/ iconShowing()) {
            mIconBlock.draw(dc);
            // draw divider if there's a label
            if (hasLabel()) {
                final Color renderFill = getRenderFillColor(dc);
                final Color marginColor;
                if (renderFill != null) {
                    if (renderFill.equals(Color.black))
                        marginColor = Color.darkGray;
                    else
                        marginColor = renderFill.darker();
                } else {
                    // transparent fill: base on stroke color
                    marginColor = getStrokeColor().brighter();
                }
                g.setColor(marginColor);
                g.setStroke(STROKE_ONE);
                g.draw(mIconDivider);
            }
        }
    }

    //-----------------------------------------------------------------------------
    // I think these are done dynamically instead of always using
    // mLabelPos.x and mLabelPos.y because we haven't always done a
    // layout when we need this?  Is that true?  Does this have
    // anything to do with activating an edit box on a newly created
    // node?
    //-----------------------------------------------------------------------------
    
    protected float relativeLabelX()
    {
        //return mLabelPos.x;
        if (isCenterLayout) { // non-rectangular shapes
            return mLabelPos.x;
//         } else if (isTextNode() && mStrokeWidth.get() == 0) {
//             return 1;
//             //return 1 + (strokeWidth == 0 ? 0 : strokeWidth / 2);
        } else if (iconShowing()) {
            //offset = (float) (PadX*1.5 + genIcon.getWidth());
            //offset = (float) genIcon.getWidth() + 7;
            //offset = IconMargin + LabelPadLeft;
            return LabelPositionXWhenIconShowing;
        } else {
            // horizontally center if no icons

            if (WrapText)
                return mLabelPos.x;
            else {
                // Doing this risks slighly moving the damn TextBox just as you edit it.
                final float offset = (this.width - getTextSize().width) / 2;
                return offset + 1;
            }
        }
    }
    
    protected float relativeLabelY()
    {
        //return mLabelPos.y;
        if (isCenterLayout) {
            return mLabelPos.y;
        } else if (hasChildren()) {
            return EdgePadY;
        } else {
            // only need this in case of small font sizes and an icon
            // is showing -- if so, center label vertically in row with the first icon
            // Actually, no: center in whole node -- gak, we really want both,
            // but only to a certian threshold -- what a hack!
            //float textHeight = getLabelBox().getPreferredSize().height;
            
            if (false && WrapText)
                return mLabelPos.y;
            else {
                // Doing this risks slighly moving the damn TextBox just as you edit it.
                // Tho querying the underlying TextBox for it's size every time
                // we repaint this object is pretty gross also (e.g., every drag)
                return (this.height - getTextSize().height) / 2;
            }
            
        }
        
        /*
          // for single resource icon style layout
        if (iconShowing() || hasChildren()) {
            if (iconShowing())
                return (float) dividerUnderline.getY1() - getLabelBox().getPreferredSize().height;
            else
                return PadTop;
        }
        else // center vertically
            return (this.height - getLabelBox().getPreferredSize().height) / 2;
        */
    }

    private float childOffsetX() {
        if (isCenterLayout) {
            //System.out.println("\tchildPos.x=" + mChildPos.x);
            return mChildPos.x;
        }
        return iconShowing() ? ChildOffsetX : ChildPadX;
    }
    private float childOffsetY() {
        if (isCenterLayout) {
            //System.out.println("\tchildPos.y=" + mChildPos.y);
            return mChildPos.y;
        }
        float baseY;
        if (iconShowing()) {
            //baseY = (float) (mIconResource.getY() + IconHeight + ChildOffsetY);
            //baseY = (float) dividerUnderline.getY1();
            baseY = mBoxedLayoutChildY;
            if (DEBUG.LAYOUT) out("*** childOffsetY starting with precomputed " + baseY + " to produce " + (baseY + ChildOffsetY));
        } else {
            baseY = relativeLabelY() + getLabelBox().getHeight();
        }
        baseY += ChildOffsetY;
        return baseY;
    }
    

    // experimental
    private transient ImageIcon imageIcon = null;
    // experimental
    void setImage(Image image)
    {
        imageIcon = new ImageIcon(image, "Image Description");
        setAutoSized(false);
        setShape(Rectangle2D.Float.class);
        setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
    }
    

    //------------------------------------------------------------------
    // Constants for layout of the visible objects in a node.
    // This is some scary stuff.
    // (label, icons & children, etc)
    //------------------------------------------------------------------

    private static final int EdgePadY = 4; // Was 3 in VUE 1.5
    private static final int PadTop = EdgePadY;

    private static final int IconGutterWidth = 26;

    private static final int IconPadLeft = 2;
    private static final int IconPadRight = 0;
    private static final int IconWidth = IconGutterWidth - IconPadLeft; // 22 is min width that will fit "www" in our icon font
    private static final int IconHeight = 12;
    
    //private static final int IconPadRight = 4;
    private static final int IconMargin = IconPadLeft + IconWidth + IconPadRight;
    /** this is the descent of the closed icon down below the divider line */
    private static final float IconDescent = IconHeight / 3f;
    /** this is the rise of the closed icon above the divider line */
    private static final float IconAscent = IconHeight - IconDescent;
    private static final int IconPadBottom = (int) IconAscent;
    private static final int IconMinY = IconPadLeft;

    private static final int LabelPadLeft = 8; // Was 6 in VUE 1.5; fixed distance to right of iconMargin dividerLine
    private static final int LabelPadRight = 8; // Was 6 in VUE 1.5; minimum gap to right of text before right edge of node
    private static final int LabelPadX = LabelPadLeft;
    private static final int LabelPadY = EdgePadY;
    private static final int LabelPositionXWhenIconShowing = IconMargin + LabelPadLeft;

    // TODO: need to multiply all these by ChildScale (huh?)
    
    private static final int ChildOffsetX = IconMargin + LabelPadLeft; // X offset of children when icon showing
    private static final int ChildOffsetY = 4; // how far children down from bottom of label divider line
    private static final int ChildPadY = ChildOffsetY;
    private static final int ChildPadX = 5; // min space at left/right of children
    private static final int ChildVerticalGap = 3; // vertical space between children
    private static final int ChildHorizontalGap = 3; // horizontal space between children
    private static final int ChildrenPadBottom = ChildPadX - ChildVerticalGap; // make same as space at right
    //    private static final int ChildrenPadBottom = 3; // space at bottom after all children
    
    
    private static final float DividerStubAscent = IconDescent;
    
    // at some zooms (some of the more "irregular" ones), we get huge
    // understatement errors from java in computing the width of some
    // font strings, so this pad needs to be big enough to compensate
    // for the error in the worst case, which we're guessing at here
    // based on a small set of random test cases.
    //private static final float TextWidthFudgeFactor = 1 + 0.1f; // 10% fudge
    //private static final float TextWidthFudgeFactor = 1 + 0.05f; // 5% fudge
    private static final float TextWidthFudgeFactor = 1; // off for debugging (Almost uneeded in new Mac JVM's)
    // put back to constant??  Also TODO: Text nodes left-aligned, not centered, and for real disallow BG color.
    //private static final float TextWidthFudgeFactor = 1;
    //private static final int DividerStubPadX = TextWidthFudgeAmount;

    private static final int MarginLinePadY = 5;
    private static final int IconPillarPadY = MarginLinePadY;
    private static final int IconPillarFudgeY = 4; // attempt to get top icon to align with top of 1st caps char in label text box

    /** for castor restore, internal default's and duplicate use only */
    public LWNode()
    {
        this.mIsRectShape = true;
        // I think we may only need this default shape setting for backward compat with old save files.
        this.boundsShape = new java.awt.geom.Rectangle2D.Float();
        this.drawnShape = cloneShape(boundsShape);
        this.autoSized = false;
        adjustDrawnShape();


        // Force the creation of the TextBox (this.labelBox).
        // We need this for now to make sure wrapped text nodes don't unwrap
        // to one line on restore. I think the TextBox needs to pick up our size
        // before setLabel for it to work.
        //getLabelBox(); LAYOUT-NEW
    }
    
    
}