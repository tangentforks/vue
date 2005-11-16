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
import java.awt.Point;
import java.awt.Color;
import java.awt.geom.Point2D;
import javax.swing.Action;

/**
 * UndoManager
 *
 * Records all changes that take place in a LWMap (as seen from
 * LWCEvent delivery off the LWMap) and provides for arbitrarily
 * marking named points of rollback.
 *
 * For robustness, if the application fails to mark any changes,
 * they'll either have been rolled undo another undo action, or
 * stuffed into an un-named Undo action if they attempt an undo while
 * there are unmarked changes.
 *
 * @author Scott Fraize
 * @version July 2004
 */

public class UndoManager
    implements LWComponent.Listener, VUE.ActiveMapListener
{
    private static boolean sUndoUnderway = false;
    private static boolean sRedoUnderway = false;

    /** The list of undo actions (named groups of property changes) */
    private UndoActionList UndoList = new UndoActionList("Undo"); 
    /** The list of redo actions (named groups of property changes generated from Undo's) */
    private UndoActionList RedoList = new UndoActionList("Redo"); 
    
    /** The map who's modifications we're tracking. */
    protected LWMap mMap; 
    
    /** All recorded changes since last mark, mapped by component (for detecting & ignoring repeats) */
    private Map mComponentChanges = new HashMap();
    /** All recorded changes since last mark, marked for sequential processing */
    //private List mUndoSequence = new ArrayList(); 
    /** The last LWCEvent we didn't ignore since last mark -- used for guessing at good Undo action title names */
    private LWCEvent mLastEvent;
    /** The total number of recorded or compressed changes since last mark (will be >= mUndoSequence.size()) */
    //private int mChangeCount;

    /** The current collector of changes, to be permanently recorded and named when a user "mark" is established by a GUI */
    private UndoAction mCurrentUndo;

    /** map of threads currently attched to a particular undo mark */
    private Map mThreadsWithMark = Collections.synchronizedMap(new HashMap());


    public UndoManager(LWMap map)
    {
        mMap = map;
        mCurrentUndo = new UndoAction();
        map.addLWCListener(this);
        VUE.addActiveMapListener(this);
        activeMapChanged(map); // make sure actions disabled at start
    }

    
    /**
     * This aggregates a sequence of property changes under a single
     * named user action.
     *
     * A named sequence of triples: LWComponent, property key, and old value.
     * LWCEvents are undone in the reverse order that they happened: the changes
     * are peeled back.
     */
    private static class UndoAction {
        private String name = null;
        private List undoSequence; // list of UndoItem's -- will be sorted by sequence index before first use
        /** The total number of recorded or compressed changes that happened on our watch (will be >= undoSequence.size()) */
        private int eventCount = 0;
        private boolean sorted = false;
        private List attachedThreads;

        UndoAction() {
            undoSequence = new ArrayList();
        }
        /*
        UndoAction(String name, List undoSequence) {
            this.name = name;
            this.undoSequence = undoSequence;
        }
        */

        synchronized void addAttachedThread(Thread t) {
            if (attachedThreads == null)
                attachedThreads = new ArrayList();
            attachedThreads.add(t);
        }

        int changeCount() {
            return undoSequence.size();
        }

        int size() { return undoSequence.size(); }

        void mark(String name) {
            this.name = name;
            if (DEBUG.UNDO) System.out.println(this + " MARKED");
        }

        boolean isIncomplete() {
            return size() > 0 && name != null;
        }
        boolean isMarked() {
            return name != null;
        }
        
        synchronized void undo() {
            try {
                sUndoUnderway = true;
                run_undo();
            } finally {
                sUndoUnderway = false;
            }
        }

        // todo: if there are any UndoableThread's attached to us, they should
        // ideally be interrupted.
        private synchronized void run_undo() {
            if (DEBUG.UNDO) System.out.println(this + " undoing sequence of size " + changeCount());

            if (attachedThreads != null) {
                // First: interrupt any running threads that may yet deliver events
                // to this UndoAction. 
                Iterator i = attachedThreads.iterator();
                while (i.hasNext()) {
                    Thread t = (Thread) i.next();
                    if (DEBUG.UNDO) System.out.println(this + " interrupting " + t);
                    if (t.isAlive())
                        t.interrupt();
                    // only interrupt the first time
                    i.remove();
                }
            }

            if (!sorted) {
                Collections.sort(undoSequence);
                sorted = true;
                if (DEBUG.UNDO){
                    System.out.println("=======================================================");
                    VueUtil.dumpCollection(undoSequence);
                    System.out.println("-------------------------------------------------------");
                }
            }

            boolean hierarchyChanged = false;
            
            //-------------------------------------------------------
            // First, process all hierarchy events
            //-------------------------------------------------------
            
            ListIterator i = undoSequence.listIterator(undoSequence.size());
            while (i.hasPrevious()) {
                UndoItem undoItem = (UndoItem) i.previous();
                if (undoItem.propKey == LWKey.HierarchyChanging) {
                    undoItem.undo();
                    hierarchyChanged = true;
                }
            }

            //-------------------------------------------------------
            // Second, process all property change events
            //-------------------------------------------------------
            
            i = undoSequence.listIterator(undoSequence.size());
            while (i.hasPrevious()) {
                UndoItem undoItem = (UndoItem) i.previous();
                if (undoItem.propKey != LWKey.HierarchyChanging)
                    undoItem.undo();
            }
            
            if (hierarchyChanged)
                VUE.getSelection().clearDeleted();
        }

        String getName() {
            return name;
        }
        
        /**
         *  massage the name of the property to produce a more human
         *  presentable name for the undo action.
         */
        String getDisplayName() {
            if (DEBUG.UNDO) return this.name + " {" + changeCount() + "}";
            
            String display = "";
            String uName = this.name;
            if (uName == LWKey.HierarchyChanging)
                uName = "Change";
            else if (uName.startsWith("hier."))
                uName = uName.substring(5);
            // Replace all '.' with ' ' and capitalize first letter of each word
            uName = uName.replace('-', '.');
            String[] word = uName.split("\\.");
            for (int i = 0; i < word.length; i++) {
                if (Character.isLowerCase(word[i].charAt(0)))
                    word[i] = Character.toUpperCase(word[i].charAt(0)) + word[i].substring(1);
                if (i > 0)
                    display += " ";
                display += word[i];
            }
            return display;
        }
    
        public String toString() {
            int s = size();
            return "UndoAction@" + Integer.toHexString(hashCode()) + "["
                + (name==null?"":name)
                + (s<10?" ":"") + s + " changes"
                + " from " + eventCount + " events"
                + "]";
        }
    }

    /**
     * A single property change on a single component.
     */
    private static class UndoItem implements Comparable
    {
        LWComponent component;
        Object propKey;
        Object oldValue;
        int order; // for sorting; highest values are most recent changes

        UndoItem(LWComponent c, Object propertyKey, Object oldValue, int order) {
            this.component = c;
            this.propKey = propertyKey;
            this.oldValue = oldValue;
            this.order = order;
        }

        void undo() {
            if (DEBUG.UNDO) System.out.println("UNDOING: " + this);
            if (propKey == LWKey.HierarchyChanging) {
                undoHierarchyChange((LWContainer) component, oldValue);
            } else if (oldValue instanceof Undoable) {
                ((Undoable)oldValue).undo();
            } else {
                if (DEBUG.Enabled && DEBUG.META) {
                    try {
                        Object curValue = component.getPropertyValue(propKey);
                        if (curValue != null)
                            undoAnimated();
                    } catch (Exception e) {
                        System.err.println("Exception during animated undo of [" + propKey + "] on " + component);
                        if (oldValue != null)
                            System.err.println("\toldValue is " + oldValue.getClass() + " " + oldValue);
                        e.printStackTrace();
                    }
                }
                component.setProperty(propKey, oldValue);
            }
        }

        private void undoAnimated() {
            // redo not working if we suspend events here...
            // please don't tell me redo was happening by capturing
            // the zillions of animated events...
            
            // Also going to be tricky: animating through changes
            // in a bunch of nodes at the same time -- right now
            // a group drag animates each one back into place
            // one at a time in sequence...

            // Also: SEE COMMENT in LWLink.getPropertyValue
                    
            // experimental for animated presentation
            //component.getChangeSupport().setEventsSuspended();
            
            if (oldValue instanceof Point)
                animatedChange((Point)oldValue);
            else if (oldValue instanceof Point2D)
                animatedChange((Point2D)oldValue);
            else if (oldValue instanceof Color)
                animatedChange((Color)oldValue);
            else if (oldValue instanceof Size)
                animatedChange((Size)oldValue);
            else if (oldValue instanceof Integer)
                animatedChange((Integer)oldValue);
            else if (oldValue instanceof Float)
                animatedChange((Float)oldValue);
            else if (oldValue instanceof Double)
                animatedChange((Double)oldValue);
            //component.getChangeSupport().setEventsResumed();
        }

        private static final int segments = 5;
        
        private static void repaint() {
            //VUE.getActiveMap().notify("repaint");
            VUE.getActiveViewer().paintImmediately();
            //try { Thread.sleep(100); } catch (Exception e) {}
        }

        private void animatedChange(Size endValue) {
            Size curValue = (Size) component.getPropertyValue(propKey);
            final float winc = (endValue.width - curValue.width) / segments;
            final float hinc = (endValue.height - curValue.height) / segments;
            Size value = new Size(curValue);
            
            for (int i = 0; i < segments; i++) {
                value.width += winc;
                value.height += hinc;
                component.setProperty(propKey, value);
                repaint();
            }
        }
        private void animatedChange(Float endValue) {
            Float curValue = (Float) component.getPropertyValue(propKey);
            final float inc = (endValue.floatValue() - curValue.floatValue()) / segments;
            Float value;
            
            for (int i = 1; i < segments+1; i++) {
                value = new Float(curValue.intValue() + inc * i);
                component.setProperty(propKey, value);
                repaint();
            }
        }
        private void animatedChange(Double endValue) {
            Double curValue = (Double) component.getPropertyValue(propKey);
            final double inc = (endValue.doubleValue() - curValue.doubleValue()) / segments;
            Double value;
            
            for (int i = 1; i < segments+1; i++) {
                value = new Double(curValue.intValue() + inc * i);
                component.setProperty(propKey, value);
                repaint();
            }
        }
        private void animatedChange(Integer endValue) {
            Integer curValue = (Integer) component.getPropertyValue(propKey);
            final float inc = (endValue.intValue() - curValue.intValue()) / segments;
            Integer value;
            
            for (int i = 1; i < segments+1; i++) {
                value = new Integer((int) (curValue.intValue() + inc * i));
                component.setProperty(propKey, value);
                repaint();
            }
        }
        
        private void animatedChange(Color endValue) {
            Color curValue = (Color) component.getPropertyValue(propKey);
            final int rinc = (endValue.getRed() - curValue.getRed()) / segments;
            final int ginc = (endValue.getGreen() - curValue.getGreen()) / segments;
            final int binc = (endValue.getBlue() - curValue.getBlue()) / segments;
            Color value;
            
            for (int i = 1; i < segments+1; i++) {
                value = new Color(curValue.getRed() + rinc * i,
                                  curValue.getGreen() + ginc * i,
                                  curValue.getBlue() + binc * i);
                component.setProperty(propKey, value);
                repaint();
            }
        }

        private void animatedChange(Point2D endValue) {
            Point2D curValue = (Point2D) component.getPropertyValue(propKey);
            final double xinc = (endValue.getX() - curValue.getX()) / segments;
            final double yinc = (endValue.getY() - curValue.getY()) / segments;
            Point2D.Double value = new Point2D.Double(curValue.getX(), curValue.getY());
            
            for (int i = 0; i < segments; i++) {
                value.x += xinc;
                value.y += yinc;
                component.setProperty(propKey, value);
                repaint();
            }
        }
        
        private void animatedChange(Point endValue) {
            Point curValue = (Point) component.getPropertyValue(propKey);
            final double xinc = (endValue.getX() - curValue.getX()) / segments;
            final double yinc = (endValue.getY() - curValue.getY()) / segments;
            Point value = new Point(curValue);
            
            for (int i = 0; i < segments; i++) {
                value.x += xinc;
                value.y += yinc;
                component.setProperty(propKey, value);
                repaint();
            }
        }
            
        private void undoHierarchyChange(LWContainer parent, Object oldValue)
        {
            if (DEBUG.UNDO) System.out.println("\trestoring children of " + parent + " to " + oldValue);

            parent.notify(LWKey.HierarchyChanging); // this event important for REDO

            // Create data for synthesized ChildrenAdded & ChildrenRemoved events
            List newChildList = (List) oldValue;
            List oldChildList = parent.children;
            ArrayList childrenAdded = new ArrayList(newChildList);
            childrenAdded.removeAll(oldChildList);
            ArrayList childrenRemoved = new ArrayList(oldChildList);
            childrenRemoved.removeAll(newChildList);

            // Do the swap in of the old list of children:
            parent.children = (List) oldValue;
            // now make sure all the children are properly parented,
            // and none of them are marked as deleted.
            Iterator ci = parent.children.iterator();
            while (ci.hasNext()) {
                LWComponent child = (LWComponent) ci.next();
                if (parent instanceof LWPathway) {
                    // Special case for pathways. todo: something cleaner (pathways don't "own" their children)
                    ((LWPathway)parent).addChildRefs(child);
                } else {
                    if (child.isDeleted())
                        child.restoreToModel();
                    child.setParent(parent);
                }
            }
            parent.setScale(parent.getScale());
            parent.layout();
            // issue synthesized ChildrenAddded and/or ChildrenRemoved events
            if (childrenAdded.size() > 0) {
                if (DEBUG.UNDO) out("Synthetic event " + LWKey.ChildrenAdded + " " + childrenAdded);
                parent.notify(LWKey.ChildrenAdded, childrenAdded);
            }
            if (childrenRemoved.size() > 0) {
                if (DEBUG.UNDO) out("Synthetic event " + LWKey.ChildrenRemoved + " " + childrenRemoved);
                parent.notify(LWKey.ChildrenRemoved, childrenRemoved);
            }
            // issue the general hierarchy change event
            parent.notify(LWKey.HierarchyChanged);
        }

        public int compareTo(Object o) {
            return order - ((UndoItem)o).order;
        }
        
        public String toString() {
            Object old = oldValue;
            if (oldValue instanceof Collection) {
                Collection c = (Collection) oldValue;
                if (c.size() > 1)
                    old = c.getClass().getName() + "{" + c.size() + "}";
            }
            return "UndoItem["
                + order + (order<10?" ":"")
                + " " + propKey
                + " " + component
                + " old=" + old
                + "]";
        }

    }

    private static class UndoActionList extends ArrayList
    {
        private String name;
        private int current = -1;

        UndoActionList(String name) {
            this.name = name;
        }
        
        public boolean add(Object o) {
            // when adding, flush anything after the top
            if (current < size() - 1) {
                int s = current + 1;
                int e = size();
                if (DEBUG.UNDO) out("flushing " + s + " to " + e + " in " + this);
                removeRange(s, e);
            }
            if (DEBUG.UNDO) out("adding: " + o);
            super.add(o);
            current = size() - 1;
            return true;
        }
        UndoAction pop() {
            if (current < 0)
                return null;
            return (UndoAction) get(current--);
        }
        UndoAction peek() {
            if (current < 0)
                return null;
            return (UndoAction) get(current);
        }

        void advance() {
            current++;
            if (current >= size())
                throw new IllegalStateException(this + " top >= size()");
        }

        public void clear() {
            super.clear();
            current = -1;
        }

        int top() {
            return current;
        }

        private void out(String s) {
            System.out.println("\tUAL[" + name + "] " + s);
        }
        
        public String toString() {
            return "UndoActionList[" + name + " top=" + top() + " size=" + size() + "]";
        }
        
        public void add(int index, Object element) { throw new UnsupportedOperationException(); }
        public Object remove(int index) { throw new UnsupportedOperationException(); }
    }

    public void activeMapChanged(LWMap map)
    {
        if (map == mMap)
            updateActionLabels();
    }

    private void updateActionLabels() {
        setActionLabel(Actions.Undo, UndoList);
        setActionLabel(Actions.Redo, RedoList);
    }

    void flush() {
        UndoList.clear();
        RedoList.clear();
        mComponentChanges.clear();
        if (VUE.getActiveMap() == mMap)
            updateActionLabels();
    }

    /* If we are asked to do an undo (or redo), and find modifications
     * on the undo list that have not been collected into a user mark,
     * this is a problem -- all changes should be collected into a
     * user umark (otherwise, for instance, creating a new node would
     * show up as separate undo actions for every property that was
     * set on the node during it's contstruction).  If we find
     * unmarked modifications, we report it to the console, and
     * create a synthetic mark for all the unmarked changes, and
     * name the last property on the list (which we could make
     * look "normal" if there was only one unmarked change, but
     * we want to know if this is happening.)
     */
    private boolean checkAndHandleUnmarkedChanges() {
        if (mCurrentUndo.isIncomplete()) {
            new Throwable(this + " UNMARKED CHANGES! " + mComponentChanges).printStackTrace();
            java.awt.Toolkit.getDefaultToolkit().beep();
            boolean olddb = DEBUG.UNDO;
            DEBUG.UNDO = true;
            markChangesAsUndo("Unnamed Actions [last=" + mLastEvent.getKeyName() + "]"); // collect whatever's there
            DEBUG.UNDO = olddb;
            return true;
        }
        return false;
    }

    private boolean mRedoCaptured = false; // debug
    public synchronized void redo()
    {
        checkAndHandleUnmarkedChanges();
        mRedoCaptured = false;
        UndoAction redoAction = RedoList.pop();
        if (DEBUG.UNDO) System.out.println(this + " redoing " + redoAction);
        if (redoAction != null) {
            try {
                sRedoUnderway = true;
                redoAction.undo();
            } finally {
                sRedoUnderway = false;
            }
            UndoList.advance();
            mMap.notify(this, LWKey.UserActionCompleted);
        }
        updateActionLabels();
    }
    
    public synchronized void undo()
    {
        checkAndHandleUnmarkedChanges();
        
        UndoAction undoAction = UndoList.pop();
        if (DEBUG.UNDO) System.out.println("\n" + this + " undoing " + undoAction);
        if (undoAction != null) {
            mRedoCaptured = false;
            undoAction.undo();
            RedoList.add(collectChangesAsUndoAction(undoAction.name));
            mMap.notify(this, LWKey.UserActionCompleted);
        }
        updateActionLabels();
        // We've undo everything: we can mark the map as having no modifications
        if (UndoList.peek() == null)
            mMap.markAsSaved();
    }

    private void setActionLabel(Action a, UndoActionList undoList) {
        String label = undoList.name;
        if (DEBUG.UNDO) label += "#" + undoList.top() + "["+undoList.size()+"]";
        if (undoList.top() >= 0) {
            label += " " + undoList.peek().getDisplayName();
            if (DEBUG.UNDO) System.out.println(this + " now available: '" + label + "'");
            a.setEnabled(true);
        } else
            a.setEnabled(false);
        a.putValue(Action.NAME, label);
    }

    /** figure the name of the undo action from the last LWCEvent we stored
     * an old property value for */
    public void mark() {
        markChangesAsUndo(null);
    }

    /**
     * If only one property changed, use the name of that property,
     * otherwise use the @param aggregateName for the group of property
     * changes that took place.
     */
    
    public void mark(String aggregateName) {
        String name = null;
        if (mCurrentUndo.size() == 1 && mLastEvent != null) // going to need to put last event into UndoAction..
            name = mLastEvent.getKeyName();
        else
            name = aggregateName;
        markChangesAsUndo(name);
    }

    public synchronized void markChangesAsUndo(String name)
    {
        if (mCurrentUndo.size() == 0) // if nothing changed, don't bother adding an UndoAction
            return;
        if (name == null) {
            if (mLastEvent == null)
                return;
            name = mLastEvent.getKeyName();
        }
        UndoList.add(collectChangesAsUndoAction(name));
        RedoList.clear();
        mMap.notify(this, LWKey.UserActionCompleted);
        updateActionLabels();
    }

    private synchronized UndoAction collectChangesAsUndoAction(String name)
    {
        //UndoAction newUndoAction = new UndoAction(name, mUndoSequence);

        final UndoAction markedUndo = mCurrentUndo;
        markedUndo.mark(name);
        //mUndoSequence = new ArrayList();
        mCurrentUndo = new UndoAction();
        mComponentChanges.clear();
        mLastEvent = null;
        //mChangeCount = 0;
        return markedUndo;
    }

    
    /**
     * Store a key in the given UndoableThread that tells the UndoManager what UndoAction
     * is affected by LWCEvents coming from that thread.  This must be called BEFORE any
     * events in the given thread have been marked.  To ensure this, make sure this is
     * called before the thread has been started.
     */
    
    void attachThreadToUpcomingMark(UndoableThread t) {
        if (t.isAlive())
            throw new Error(t + ": not safe to attach an UndoAction to an already started thread");
        t.setMarker(mCurrentUndo);
    }

    private static class UndoMark {
        final UndoManager manager;
        final UndoAction action;
        UndoMark(UndoManager m) {
            manager = m;
            action = manager.mCurrentUndo;
        }
        public String toString() {
            String s = "UndoMark[";
            if (action != manager.mCurrentUndo) 
                s += action + " / ";
            return s + manager + "]";
        }
    }
    
    /**
     * @return a key that marks the current location in the undo queue,
     * that can be used to attach a subsequent thread to.  That is,
     * by taking the returned key and later calling attachThreadToMark
     * in another thread, all further events received by the UndoManager from
     * that thread will be attched in the undo queue at the location
     * of the given mark.  This may return null, which means there
     * is no current UndoManager listening for events.
     */
    public static Object getKeyForUpcomingMark(LWComponent c) {
        LWMap map = c.getMap();
        if (map == null)
            return null;
            //throw new Error("Component not yet in map: can't search for undo manager " + c);
        UndoManager undoManager = map.getUndoManager();
        if (undoManager == null)
            return null;
        else
            return undoManager.getKeyForUpcomingMark();
        //return currentManager.getStringKeyForUpcomingMark();
    }

    public Object getKeyForUpcomingMark() {
        UndoMark mark = new UndoMark(this);
        if (DEBUG.UNDO || DEBUG.THREAD) System.out.println("GENERATED MARK " + mark);
        return mark;
    }

    
    /**
     * Attach the current thread to the location in the undo queue marked by the given key.
     *
     * @param undoActionKey key obtained from getKeyForUpcomingMark, which may be null,
     * in which case this method does nothing.
     */

    static void attachCurrentThreadToMark(Object undoActionKey) {
        if (undoActionKey != null) {
            Thread thread = Thread.currentThread();
            if (!thread.getName().startsWith("Image Fetcher"))
                new Throwable("Warning: attaching mark to non-Image Fetch thread: " + thread).printStackTrace();

            // extract the mark, because it contains the manager we need to insert the thread:mark mapping
            UndoMark mark = (UndoMark) undoActionKey;
            // store the mark in the appropriate UndoManager, and notify of error if thread was already marked
            UndoMark existingMark = (UndoMark) mark.manager.mThreadsWithMark.put(thread, mark);

            mark.action.addAttachedThread(thread);
            
            if (existingMark != null)
                new Throwable("Error: " + thread + " was tied to mark " + existingMark + ", superceeded by " + mark).printStackTrace();
            if (DEBUG.UNDO || DEBUG.THREAD) System.out.println("ATTACHED " + mark + " to " + thread);
        }
    }
    
    static void detachCurrentThread(Object undoActionKey) {
        if (undoActionKey != null) {
            UndoMark mark = (UndoMark) undoActionKey;
            mark.manager.mThreadsWithMark.remove(Thread.currentThread());

            // Tell everyone listing it's time to repaint.
            // todo: only need to do this if a property actually changed during this thread

            mark.manager.mMap.notify(mark.manager, LWKey.RepaintAsync);
            
            // todo: messy to require two events here..
            // This event will tell the ACTIVE viewer to repaint:
            //mark.manager.mMap.notify(mark.manager, LWKey.Repaint);
            // This event is for all NON-ACTIVE viewers, or Panners, other listeners, etc:
            //mark.manager.mMap.notify(mark.manager, LWKey.UserActionCompleted);
        }
        /*
        final Thread thread = Thread.currentThread();
        final String tn = thread.getName();
        if (tn.startsWith(UNDO_ACTION_TAG)) {
            thread.setName(tn.substring(tn.indexOf(')') + 2));
            System.out.println("Released thread " + thread);
        }
        */
    }

    /*
    private Map taggedUndoActions = new HashMap();
    private static final String UNDO_ACTION_TAG = "+VUA@(";
    static void attachCurrentThreadToStringMark(String undoActionKey) {
        if (undoActionKey != null) {
            Thread t = Thread.currentThread();
            if (!t.getName().startsWith("Image Fetcher")) {
                new Throwable("Warning: attaching mark to non-Image Fetch thread: " + t).printStackTrace();
            }
            // todo: cleaner if we kept a map of Thread:UndoAction's, but a tad slower
            String newName = undoActionKey + " " + t.getName();
            t.setName(newName);
            if (DEBUG.UNDO || DEBUG.THREAD) System.out.println("Applied key " + undoActionKey + " to " + t);
        }
    }
    private String _getStringKeyForUpcomingMark() {
        final String currentUndoKey = Integer.toHexString(mCurrentUndo.hashCode());
        synchronized (this) {
            taggedUndoActions.put(currentUndoKey, mCurrentUndo);
        }
        return UNDO_ACTION_TAG + currentUndoKey + ")";
    }
    */

    /**
     * Every event anywhere in the map we're listening to will
     * get delivered to us here.  If the event has an old
     * value in it, we save it for later undo.  If it's
     * a hierarchy event (add/remove/delete/forward/back, etc)
     * we handle it specially.
     */

    public void LWCChanged(LWCEvent e) {

        if (sRedoUnderway) // ignore everything during redo
            return;

        if (sUndoUnderway) {
            if (!mRedoCaptured && mCurrentUndo.size() > 0) 
                throw new Error("Undo Error: have changes at start of redo record: " + mCurrentUndo.size() + " " + mComponentChanges + " " + e);
            mRedoCaptured = true;
            if (DEBUG.UNDO) System.out.print("\tredo: " + e);
        } else {
            if (DEBUG.UNDO) System.out.print(this + " " + e);
        }
        processEvent(e);
    }

    private void processEvent(LWCEvent e)
    {
        if (e.getKey() == LWKey.HierarchyChanging || e.getKeyName().startsWith("hier.")) {
            recordEvent(e, true);
        } else if (e.hasOldValue()) {
            recordEvent(e, false);
        } else {
            if (DEBUG.UNDO) {
                System.out.println(" (ignored: no old value)");
                if (DEBUG.META) new Throwable().printStackTrace();
            }
        }
    }

    private void recordEvent(LWCEvent e, boolean hierarchyEvent)
    {
        final UndoAction relevantUndoAction;
        final Map perComponentChanges;
        final Thread thread = Thread.currentThread();

        if (thread instanceof UndoableThread) {
            relevantUndoAction = (UndoAction) ((UndoableThread)thread).getMarker();
            if (relevantUndoAction == null) {
                // This can happen if there was no UndoManager at the time
                // the UndoableThread was started, such as when loading
                // a map (we don't assign an undo manager to a map
                // until it's fully loaded).  In this case, there's
                // nothing to do: these property changes weren't supposed
                // to be undoable in the first place.
                return;
            }
            perComponentChanges = null; // we can live w/out "compression" for changes on UndoableThread's
            if (DEBUG.UNDO || DEBUG.THREAD) System.out.println("\n" + thread + " initiating change in " + relevantUndoAction);

        } else if (mThreadsWithMark.size() > 0 && mThreadsWithMark.containsKey(thread)) {
            final UndoMark mark = (UndoMark) mThreadsWithMark.get(thread);
            if (DEBUG.UNDO || DEBUG.THREAD)
                System.out.println("\nFOUND MARK FOR CURRENT THREAD " + thread
                                   + "\n\t mark: " + mark
                                   + "\n\tevent: " + e);
            relevantUndoAction = mark.action;
            perComponentChanges = null;

/*      This code allowed us to tag a thread by tweaking it's name.  Not a very safe method.
        
        } else if (thread.getName().startsWith(UNDO_ACTION_TAG)) {
            String key = thread.getName().substring(UNDO_ACTION_TAG.length());
            key = key.substring(0, key.indexOf(')'));
            final UndoAction taggedUndo;
            synchronized (this) {
                taggedUndo = (UndoAction) taggedUndoActions.get(key);
            }
            System.out.println("Got from key [" + key + "] " + taggedUndo + " for " + e);
            relevantUndoAction = taggedUndo;
            perComponentChanges = null;
*/
        } else {
            //------------------------------------------------------------------
            // This is almost always where we wind up: all the above code
            // in this method is just in case we got an event from
            // a spawned thread, such as an ImageFetcher.
            //------------------------------------------------------------------
            relevantUndoAction = mCurrentUndo;
            perComponentChanges = mComponentChanges;
        }

        if (hierarchyEvent)
            recordUndoableChangeEvent(relevantUndoAction,
                                      perComponentChanges,
                                      LWKey.HierarchyChanging,
                                      (LWContainer) e.getSource(), // parent
                                      HIERARCHY_CHANGE_TAG);
        else
            recordUndoableChangeEvent(relevantUndoAction,
                                      perComponentChanges,
                                      e.getKey(),
                                      e.getComponent(),
                                      e.getOldValue());

        mLastEvent = e;

        //recordHierarchyChangingEvent(e, relevantUndoAction, perComponentChanges);
        //recordPropertyChangeEvent(e, relevantUndoAction, perComponentChanges);
    }

    private static final Object HIERARCHY_CHANGE_TAG = "hierarchy.change";
    private void XrecordHierarchyChangingEvent(LWCEvent e, UndoAction undoAction, Map perComponentChanges)
    {
        LWContainer parent = (LWContainer) e.getSource();
        //recordUndoableChangeEvent(mHierarchyChanges, LWKey.HierarchyChanging, parent, HIERARCHY_CHANGE);
        //recordUndoableChangeEvent(um.mUndoSequence, um.mComponentChanges, LWKey.HierarchyChanging, parent, HIERARCHY_CHANGE);
        recordUndoableChangeEvent(undoAction, perComponentChanges, LWKey.HierarchyChanging, parent, HIERARCHY_CHANGE_TAG);
        mLastEvent = e;
    }
    
    private void XrecordPropertyChangeEvent(LWCEvent e, UndoAction undoAction, Map perComponentChanges)
    {
        // e.getComponent can really be list... todo: warn us if list (should only be for hier events)
        //recordUndoableChangeEvent(mPropertyChanges, e.getKey(), e.getComponent(), e.getOldValue());
        recordUndoableChangeEvent(undoAction, perComponentChanges, e.getKey(), e.getComponent(), e.getOldValue());
        mLastEvent = e;
    }

    /**
     * Record a property change to a given component with the given property key.  Our
     * objective is to store one old value (the oldest) for each changed
     * component:propertyKey pair.  As such, we can "compress" repeat events for the same
     * component:propertyKey pair.  E.g., a single component is dragged across a map, and
     * we get continuous events with propertyKey "location" for that component.  We only
     * need to store the old value from the FIRST of these events, and we can toss away
     * the old value from all subsequent "location" changes to that same component, as
     * these are just intermediate values over the course of one single user action.
     *
     * Note: if we did store all the intermediate values, along with the time each one
     * happened (UndoItem.order would become a long, set to System.currentTimeMillis() --
     * inefficient but easy), we'd actually be recording all user activity on the map, and
     * be able to play back how they used the tool, or use it to construct demo's or such.
     * Although our "animated undo" would be a much more efficient way of getting
     * essentially the same behavior (interpolate the intermediate values instead of
     * recording them all).
     *
     *
     */

    //private static void recordUndoableChangeEvent(List undoSequence,Map componentChanges,Object propertyKey,LWComponent component,Object oldValue)
    private static void recordUndoableChangeEvent(UndoAction undoAction,
                                                  Map perComponentChanges,
                                                  Object propertyKey,
                                                  LWComponent component,
                                                  Object oldValue)
    {
        boolean compressed = false; // already had one of these props: can ignore all subsequent
        Map allChangesToComponent = null;
        TaggedPropertyValue alreadyStoredValue = null; // a value already stored for this (component,propertyKey)
        
        if (perComponentChanges != null) {
            // If we have a map of existing components, we can do compression (don't have it for UndoableThread's)
            // We look for find existing changes to this particular component, if any.
            allChangesToComponent = (Map) perComponentChanges.get(component);

            if (allChangesToComponent == null) {
                // No prior changes to this component: create a new map for this component for remembering changes in
                allChangesToComponent = new HashMap();
                perComponentChanges.put(component, allChangesToComponent);
            } else {
                // If we already have a change to the same component with the same propertyKey, we can
                //if (DEBUG.UNDO) System.out.println("\tfound existing component " + c);
                //Object value = allChangesToComponent.get(propertyKey);
                alreadyStoredValue = (TaggedPropertyValue) allChangesToComponent.get(propertyKey);
                if (alreadyStoredValue != null) {
                    if (DEBUG.UNDO) System.out.println(" (compressed)");
                    compressed = true;
                }
            }
        }
        
        if (compressed) {
            // If compressed, still make sure the current property change UndoItem is
            // marked as being at the current end of the undo sequence.
            if (undoAction.size() > 1) {
                //UndoItem undoItem = (UndoItem) undoSequence.get(alreadyStoredValue.index);
                UndoItem undoItem = (UndoItem) undoAction.undoSequence.get(alreadyStoredValue.index);
                if (DEBUG.UNDO&&DEBUG.META) System.out.println("Moving index "
                                                               +alreadyStoredValue.index+" to end index "+undoAction.eventCount
                                                               + " " + undoItem);
                undoItem.order = undoAction.eventCount++;
            }
        } else {
            if (oldValue == HIERARCHY_CHANGE_TAG)
                oldValue = ((ArrayList)((LWContainer)component).children).clone();
            if (allChangesToComponent != null)
                allChangesToComponent.put(propertyKey, new TaggedPropertyValue(undoAction.size(), oldValue));
            undoAction.undoSequence.add(new UndoItem(component, propertyKey, oldValue, undoAction.eventCount));
            undoAction.eventCount++;
            if (DEBUG.UNDO) {
                System.out.println(" (stored: " + oldValue + ")");
                //if (DEBUG.META) 
                //else System.out.println(" (stored)");
            }
        }
    }

    private static class TaggedPropertyValue {
        int index; // index into undoSequence of this property value
        Object value;

        TaggedPropertyValue(int index, Object value) {
            this.index = index;
            this.value = value;
        }

        public String toString() {
            return value + "~" + index;
        }
    }
    
    private static void out(String s) {
        System.out.println("UndoManger: " + s);
    }

    public String toString()
    {
        return "UndoManager[" + mMap.getLabel() + " "
            + mCurrentUndo
            + "]"
            //+ hashCode()
            ;
    }
    
}

