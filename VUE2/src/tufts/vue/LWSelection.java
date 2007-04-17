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

import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;

/**
 *
 * Maintains the VUE global list of selected LWComponent's.
 *
 * @version $Revision: 1.60 $ / $Date: 2007-04-17 23:00:10 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */

// TODO: have this listen to everyone in it, and then
// folks who want to monitor the selected object
// can only once regiester as a selectedEventListener,
// instead of always adding and removing from the individual objects.
public class LWSelection extends java.util.ArrayList<LWComponent>
{
    private List listeners = new java.util.ArrayList();
    private List controlListeners = new java.util.LinkedList();
    private Rectangle2D.Float mBounds = null;

    private boolean isClone = false;

    private int mWidth = -1, mHeight = -1;  // only used for manually created selections

    private Object source;
    private LWComponent focal; // root of selection tree

    private long mEditablePropertyKeys;

    private List<LWComponent> mSecureList = null;

    public LWSelection() {}

    public Object getSource() {
        return source;
    }

    public void setSource(Object src) {
        this.source = src;
    }
    public void setFocal(LWComponent focal) {
        this.focal = focal;
    }
    public LWComponent getFocal() {
        return focal;
    }

    // currently only used for special case manually created selections
    public int getWidth() {
        return mWidth;
    }

    // currently only used for special case manually created selections
    public int getHeight() {
        return mHeight;
    }

    public boolean isSized() {
        return mWidth > 0 && mHeight > 0;
    }

    public void setSize(int w, int h) {
        mWidth = w;
        mHeight = h;
    }

    /** @return an unmodifable list of our contents useful for iteration */
    public List<LWComponent> contents() {
        if (mSecureList == null)
            mSecureList = java.util.Collections.unmodifiableList(this);
        return mSecureList;
    }

    /** create a temporary selection that contains just the given component */
    public LWSelection(LWComponent c) {
        //if (DEBUG.Enabled) tufts.Util.printStackTrace("selection singleton for " + c);
        isClone = true;
        super.add(c);
    }

    /** create a temporary selection that contains just the given components */
    public LWSelection(java.util.List list) {
        isClone = true;
        super.addAll(list);
    }
    
    public interface Listener extends java.util.EventListener {
        void selectionChanged(LWSelection selection);
    }

    public static class Controller extends Point2D.Float
    {
        protected java.awt.Color color = VueConstants.COLOR_SELECTION_HANDLE;
        
        public Controller() {}
        public Controller(float x, float y) {
            super(x, y);
        }
        public Controller(java.awt.geom.Point2D p) {
            this((float)p.getX(), (float)p.getY());
        }

        public void setColor(java.awt.Color c) {
            this.color = c;
        }
        
        public java.awt.Color getColor() {
            return color;
        }

        /** @return override for an optional shape (besides the default square) */
        public RectangularShape getShape() { return null; }
        /** @return override for an optional rotation for the shape (in radians) */
        public double getRotation() { return 0; }

    }
    public interface ControlListener extends java.util.EventListener {
        void controlPointPressed(int index, MapMouseEvent e);
        void controlPointMoved(int index, MapMouseEvent e);
        void controlPointDropped(int index, MapMouseEvent e);
        Controller[] getControlPoints();
    }
    

    private void addControlListener(ControlListener listener)
    {
        if (DEBUG.SELECTION) System.out.println(this + " adding control listener " + listener);
        controlListeners.add(listener);
    }
    
    private void removeControlListener(ControlListener listener)
    {
        if (DEBUG.SELECTION) System.out.println(this + " removing control listener " + listener);
        if (!controlListeners.remove(listener))
            throw new IllegalStateException(this + " didn't contain control listener " + listener);
    }

    java.util.List<ControlListener> getControlListeners()
    {
        return controlListeners;
    }

    public synchronized void addListener(Listener l)
    {
        if (DEBUG.SELECTION&&DEBUG.META) System.out.println(this + " adding listener   " + l);
        listeners.add(l);
    }
    public synchronized void removeListener(Listener l)
    {
        if (DEBUG.SELECTION&&DEBUG.META) System.out.println(this + " removing listener " + l);
        listeners.remove(l);
    }

    private Listener[] listener_buf = new Listener[128];
    private boolean inNotify = false;
    private synchronized void notifyListeners()
    {
        if (isClone) throw new IllegalStateException(this + " clone's can't notify listeners! " + this);

        if (notifyUnderway())
            return;
        
        try {
            inNotify = true;
        
            if (DEBUG.SELECTION) {
                System.out.println("-----------------------------------------------------------------------------");
                System.out.println(this + " NOTIFYING " + listeners.size() + " LISTENERS from " + Thread.currentThread());
            }
            Listener[] listener_iter = (Listener[]) listeners.toArray(listener_buf);
            int nlistener = listeners.size();
            long start = 0;
            for (int i = 0; i < nlistener; i++) {
                if (DEBUG.SELECTION && DEBUG.META) System.out.print(this + " notifying: #" + (i+1) + " " + (i<9?" ":""));
                Listener l = listener_iter[i];
                try {
                    if (DEBUG.SELECTION) {
                        System.out.print(l + "...");
                        start = System.currentTimeMillis();
                    }
                    l.selectionChanged(this);
                    if (DEBUG.SELECTION) {
                        long delta = System.currentTimeMillis() - start;
                        System.out.println(delta + "ms");
                    }
                } catch (Exception ex) {
                    System.err.println(this + " notifyListeners: exception during selection change notification:"
                                       + "\n\tselection: " + this
                                       + "\n\tfailing listener: " + l);
                    ex.printStackTrace();
                    //java.awt.Toolkit.getDefaultToolkit().beep();
                }
            }
        } finally {
            inNotify = false;
        }
    }

    private boolean notifyUnderway() {
        if (inNotify) {
            new Throwable(this + " attempt to change selection during selection change notification: denied.").printStackTrace();
            java.awt.Toolkit.getDefaultToolkit().beep();            
            return true;
        } else {
            return false;
        }
    }

    /** for Actions.java */
    java.util.List getListeners()
    {
        return this.listeners;
    }

    public synchronized void setTo(LWComponent c)
    {
        if (size() == 1 && first() == c)
            return;
        if (notifyUnderway())
            return;
        clear0();
        add(c);
        if (VUE.getResourceSelection().get() != c.getResource())
            VUE.getResourceSelection().setTo(null, this);
    }
    
    public void setTo(Collection bag)
    {
        setTo(bag.iterator());
    }
    
    public synchronized void setTo(Iterator i)
    {
        if (notifyUnderway())
            return;
        clear0();
        add(i);
    }
    
    synchronized public boolean add(LWComponent c)
    {
        if (notifyUnderway())
            return false;
        if (!c.isSelected()) {
            add0(c);
            if (!isClone) notifyListeners();
        } else {
            if (DEBUG.SELECTION) System.out.println(this + " addToSelection(already): " + c);
            return false;
        }
        return true;
        
    }
    
    //public boolean add(Object o) {
    //throw new RuntimeException(this + " can't add " + o.getClass() + ": " + o);
    //}
    
    /** Make sure all in Collection are in selection & do a single change notify at the end */
    public void add(Collection c) {
        add(c.iterator());
    }
        
    /** Make sure all in iterator are in selection & do a single change notify at the end */
    synchronized void add(Iterator i)
    {
        if (notifyUnderway())
            return;
        
        LWComponent c;
        boolean changed = false;
        while (i.hasNext()) {
            c = (LWComponent) i.next();
            if (!c.isSelected() && c.isDrawn()) {
                add0(c);
                changed = true;
            }
        }
        if (changed)
            notifyListeners();
    }
    
    /** Change the selection status of all LWComponents in iterator */
    synchronized void toggle(Iterator i)
    {
        if (notifyUnderway())
            return;
        
        LWComponent c;
        boolean changed = i.hasNext();
        while (i.hasNext()) {
            c = (LWComponent) i.next();
            if (c.isSelected())
                remove0(c);
            else
                add0(c);
        }
        if (changed)
            notifyListeners();
    }
    
    private synchronized void add0(LWComponent c)
    {
        if (DEBUG.SELECTION) System.out.println(this + " adding " + c);

        if (notifyUnderway())
            return;
        
        if (!c.isSelected()) {
            if (!isClone) c.setSelected(true);
            mBounds = null;
            mEditablePropertyKeys = 0;  // set to recompute
            super.add(c);
            if (!isClone && c instanceof ControlListener)
                addControlListener((ControlListener)c);
        } else
            throw new RuntimeException(this + " attempt to add already selected component " + c);
    }
    
    public synchronized void remove(LWComponent c)
    {
        remove0(c);
        notifyListeners();
    }

    private synchronized void remove0(LWComponent c)
    {
        if (DEBUG.SELECTION) System.out.println(this + " removing " + c);
        if (notifyUnderway())
            return;
        if (!isClone) c.setSelected(false);
        if (!isClone && c instanceof ControlListener)
            removeControlListener((ControlListener)c);
        mBounds = null;
        mEditablePropertyKeys = 0; // set to recompute
        if (!super.remove(c))
            throw new RuntimeException(this + " remove: list doesn't contain " + c);
    }
    
    /**
     * clearAndNotify
     * This emthod clears teh selection and always notifies
     * listeners of a change.
     *
     **/
    public synchronized void clearAndNotify() {
    	clear0();
        if (DEBUG.SELECTION) System.out.println(this + " clearAndNotify: forced notification after clear");
    	notifyListeners();
    }
    
    public synchronized void clear()
    {
        if (clear0())
            notifyListeners();
    }

    private synchronized boolean clear0()
    {
        if (isEmpty())
            return false;
        if (notifyUnderway())
            return false;

        if (DEBUG.SELECTION) System.out.println(this + " clear0");

        if (!isClone) {
            java.util.Iterator i = iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                c.setSelected(false);
            }
        }
        controlListeners.clear();
        mEditablePropertyKeys = 0;
        mBounds = null;
        super.clear();
        return true;
    }

    /** Remove from selection anything that's been deleted */
    synchronized void clearDeleted()
    {
        if (DEBUG.SELECTION) System.out.println(this + " clearDeleted");
        boolean removed = false;
        LWComponent[] elements = new LWComponent[size()];
        toArray(elements);
        for (int i = 0; i < elements.length; i++) {
            LWComponent c = elements[i];
            if (c.isDeleted()) {
                if (DEBUG.SELECTION) System.out.println(this + " clearDeleted: clearing " + c);
                remove0(c);
                removed = true;
            }
        }
        if (removed)
            notifyListeners();
    }

    /** return bounds of map selection in map (not screen) coordinates */
    public Rectangle2D getBounds()
    {
        if (size() == 0)
            return null;
        //todo:not really safe to cache as we don't know if anything in has has moved?
        //if (bounds == null) {
        mBounds = LWMap.getBounds(iterator());
            //System.out.println("COMPUTED SELECTION BOUNDS=" + bounds);
            //}

        if (isSized()) {
            mBounds.width = mWidth;
            mBounds.height = mHeight;
        }
                
        return mBounds;
    }

    /** return shape bounds of map selection in map (not screen) coordinates
     * Does NOT inclde any stroke widths. */
    public Rectangle2D getShapeBounds()
    {
        if (size() == 0)
            return null;
        return LWMap.getShapeBounds(iterator());
    }

    void flushBounds()
    {
        mBounds = null;
    }

    public boolean contains(float mapX, float mapY)
    {
        if (size() == 0)
            return false;
	return getBounds().contains(mapX, mapY);
    }

    public LWComponent first() {
        return size() == 0 ? null : get(0);
    }

    /** @return the single component in the selection if there is only one, otherwise, null */
    public LWComponent only() {
        return size() == 1 ? get(0) : null;
    }
    
    public LWComponent last()
    {
        return size() == 0 ? null : (LWComponent) get(size()-1);
    }
    
    public int countTypes(Class clazz)
    {
        int count = 0;
        Iterator i = iterator();
        while (i.hasNext())
            if (clazz.isInstance(i.next()))
                count++;
        return count;
    }
    
    public boolean containsType(Class clazz)
    {
        Iterator i = iterator();
        while (i.hasNext())
            if (clazz.isInstance(i.next()))
                return true;
        return false;
    }
    
    public boolean allOfType(Class clazz)
    {
        Iterator i = iterator();
        while (i.hasNext())
            if (!clazz.isInstance(i.next()))
                return false;
        return size() != 0;
    }

    public boolean allOfSameType()
    {
        Iterator i = iterator();
        Object first = i.next();
        while (i.hasNext())
            if (i.next().getClass() != first.getClass())
                return false;
        return true;
    }

    public boolean allHaveSameParent()
    {
        LWComponent oc = null;
        Iterator i = iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (oc != null && oc.getParent() != c.getParent())
                return false;
            oc = c;
        }
        return true;
    }
    
    public boolean allHaveSameParentOfType(Class clazz)
    {
        if (size() == 0)
            return false;

        return allHaveSameParent() && clazz.isInstance(first().getParent());
    }

    public LWComponent[] asArray()
    {
        LWComponent[] array = new LWComponent[size()];
        super.toArray(array);
        return array;
    }

    public long getEditablePropertyBits() {
        if (mEditablePropertyKeys == 0 && !isEmpty()) {
            // keys may be zero if something was removed from the
            // selection, requiring us to recompute from scratch
            for (LWComponent c : this)
                mEditablePropertyKeys |= c.getSupportedPropertyBits();
        }
            
        return mEditablePropertyKeys;
    }
    
    public boolean hasEditableProperty(Object key) {
        if (key instanceof LWComponent.Key)
            return ( getEditablePropertyBits() & ((LWComponent.Key)key).bit ) != 0;
        else
            return true;
    }

    public LWSelection clone()
    {
        LWSelection copy = (LWSelection) super.clone();
        copy.isClone = true;
        // if anybody tries to use these we want a NPE
        copy.listeners = null;
        copy.controlListeners = null;
        return copy;
    }

    public String toString()
    {
        String content = (size() != 1 ? "" : " (" + first().toString() + ")");
        return "LWSelection[" + size() + (isClone?" CLONE]":"]" + content);
    }
    
}
