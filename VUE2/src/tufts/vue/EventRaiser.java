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
import tufts.vue.gui.GUI;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.Frame;

/**
 *
 * Generic AWT containment event raiser.  Will deliver an event to ANY
 * Component in the entire hierarchy for which
 * getTargetClass().isInstance(Component) returns true.  Event
 * delivery is done by an override of the abstrict dispatch(Object
 * listener).
 *
 * Alternatively, you may override visit(Component c), and perform
 * whatever tests you'd like for dispatching, or just use it for AWT
 * tree traversal.
 *
 * May be started at arbitrary points in the AWT tree
 * by using raiseStartingAt(Contaner) instead if raise()
 *
 * After the event has been raised, traversal (and subsequent event dispatch) may be stopped
 * by calling stop() (e.g., from within dispatch() or visit()).
 *
 * Although this runs surprisingly fast (order of 0.3ms to 3ms), 
 * it's probably wise to use this with care.
 * 
 * Note that if targetClass is null, visit(Component) will throw
 * a NullPointerException, so only do this if you are overriding visit(Component).
 *
 * Does not currently traverse into children of popup menus.
 *
 * @version $Revision: 1.8 $ / $Date: 2006-01-20 18:29:05 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public abstract class EventRaiser
{
    private int depth = 0;

    public final Class targetClass;
    public final Object source;
    
    public EventRaiser(Object source, Class clazz)
    {
        this.source = source;
        this.targetClass = clazz;
    }

    public EventRaiser(Object source)
    {
        this.source = source;
        this.targetClass = getTargetClass();
    }

    public Object getSource() {
        return source;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public void raise() {
        traverse(null);
    }
    
    public void raiseStartingAt(Container start) {
        traverse(start);
    }

    public abstract void dispatch(Object target);

    
    /**
     * Stop traversal: nothing further can be dispatched.
     *
     * This works by throwing a special completion exception, so
     * it must be called from within the traversal (e.g., dispatch(), or visit()).
     *
     */
    protected static void stop() {
        throw new Completion();
    }
    
    private static class Completion extends RuntimeException {}
    
    private void traverse(Container start)
    {
        long startTime = 0;

        // startTime = System.nanoTime(); // java 1.5
            
        if (DEBUG.EVENTS) {
            startTime = System.currentTimeMillis();
            out("raising " + this + " "
                + (start == null ? "everywhere" : ("starting at: " + GUI.name(start))));
        }
        
        try {
            if (start == null) {
                traverseFrames();
            } else {
                traverseContainer(start);
            }
        } catch (Completion e) {
            if (DEBUG.EVENTS) out("traversal stopped; done dispatching events");
        }

        if (DEBUG.EVENTS) {
            long delta = System.currentTimeMillis() - startTime;
            //double delta = (System.nanoTime() - startTime) / 1000000.0; // java 1.5
            out(this + " " + delta + "ms delivery");
        }
        
    }
    
    private void traverseFrames()
    {
        Frame frames[] = Frame.getFrames();

        if (DEBUG.EVENTS) out("FRAMES: " + java.util.Arrays.asList(frames));

        traverseChildren(frames);
    }

    protected void dispatchSafely(Component target)
    {
        if (DEBUG.EVENTS) out("DISPATCHING " + this + " to " + target);
        try {
            dispatch(target);
        } catch (Completion e) {
            throw e;
        } catch (Throwable e) {
            Util.printStackTrace(e, EventRaiser.class.getName() + " exception during dispatch: "
                                 + "\n\t  event source: " + source + " (" + Util.objectTag(source) + ")"
                                 + "\n\t  event raised: " + this
                                 + "\n\t   targetClass: " + targetClass
                                 + "\n\tdispatching to: " + GUI.name(target) + " (" + Util.objectTag(target) + ")"
                                 + "\n\t also known as: " + target
                                 );
        }
    }

    protected void visit(Component c) {
        if (targetClass.isInstance(c)) {
            dispatchSafely(c);
        }
    }

    protected void traverseContainer(Container parent)
    {
        visit(parent);

        if (parent.getComponentCount() > 0)
            traverseChildren(parent.getComponents());

        if (parent instanceof Window) {

            Window[] owned = ((Window)parent).getOwnedWindows();

            if (owned.length > 0) {
                if (DEBUG.EVENTS)
                    eoutln("    OWNED by " + GUI.name(parent) + ": " + java.util.Arrays.asList(owned));
                traverseChildren(owned);
            }
        }
    }

    protected void traverseChildren(Component[] children)
    {
        depth++;
        
        for (int i = 0; i < children.length; i++) {
            if (DEBUG.EVENTS && DEBUG.META) eoutln(getType(children[i]) + i + " " + GUI.name(children[i]));
            if (children[i] instanceof Container)
                traverseContainer((Container)children[i]);
            else
                visit(children[i]);
        }

        depth--;
    }

    private static String getType(Component c) {
        if (c instanceof Frame)
            return "FRAME";
        if (c instanceof Window)
            return "owned";
        return "child";
    }
                                                  

    private void out(String s) {
        System.out.println("EventRaiser: " + s);
    }

    protected void eout(String s) {
        for (int x = 0; x < depth; x++) System.out.print("    ");
        System.out.print(s);
    }
    protected void eoutln(String s) {
        eout(s + "\n");
    }
    
    
}

