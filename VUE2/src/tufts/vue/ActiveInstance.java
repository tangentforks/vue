package tufts.vue;

import java.lang.reflect.Method;
import java.util.*;
import tufts.Util;
import static tufts.Util.*;

/**
 * This provides for tracking the single selection of a given typed
 * object, and providing notifications for interested listeners when this selection
 * changes.  The one drawback to using a generics approach here is that once
 * type-erasure is complete, all the listener signatures are the same, so that a
 * single object can only ever listen for activeChanged calls for a single type
 * if it is declared as implementing an ActiveListener that includes type information.  In
 * practice, it's easy to deal with this limitation using anonymous classes, or
 * by using the same handler method, and checking the class type in the passed
 * ActiveEvent (we can't rely on checking the type of the what's currently active,
 * as it may be null).
 *
 * Instances of this can be created on the fly by calling the static
 * method getHandler for a given class type, which will automatically
 * create a new handler for the given type if one doesn't exist.
 * If special handlers have been created that have side-effects (e.g., in onChange),
 * make sure they're instantiated before anyone asks for a handler
 * for the type that they handle.


 * @author Scott Fraize 2007-05-05
 * @version $Revision: 1.12 $ / $Date: 2007-09-01 21:46:07 $ / $Author: sfraize $
 */

// ResourceSelection could be re-implemented using this, as long
// as we stay with only a singly selected resource object at a time.
public class ActiveInstance<T>
{
    private static final Map<Class,ActiveInstance> AllActiveHandlers = new HashMap();
    private static final List<ActiveListener> ListenersForAllActiveEvents = new ArrayList();
    private static int depth = -1; // event delivery depth
    
    private final Collection<ActiveListener> mListeners = new ArrayList();
    private Set<T> allInstances;
    
    protected final Class itemType;
    protected final String itemTypeName; // for debug
    protected final boolean itemIsMarkable;
    protected final boolean itemsAreTracked;

    private volatile T nowActive;
    private ActiveEvent lastEvent;
    private boolean inNotify;

    /**
     * If the active item itself wants to be told when it's been set to active or has lost it's active status,
     * it can implement this interface, and it will be told as it goes active / inactive.
     */
    public interface Markable {
        public void markActive(boolean active);
    }

    /** Add's a lister for ALL active item events */
    public static void addAllActiveListener(ActiveListener l) {
        ListenersForAllActiveEvents.add(l);
    }


    public static void addListener(Class clazz, ActiveListener listener) {
        getHandler(clazz).addListener(listener);
    }
    public static void addListener(Class clazz, Object reflectedListener) {
        getHandler(clazz).addListener(reflectedListener);
    }
    public static void removeListener(Class clazz, ActiveListener listener) {
        getHandler(clazz).removeListener(listener);
    }
    public static void removeListener(Class clazz, Object reflectedListener) {
        getHandler(clazz).removeListener(reflectedListener);
    }

    public static void set(Class clazz, Object source, Object item) {
        getHandler(clazz).setActive(source, item);
    }

    public static void get(Class clazz) {
        getHandler(clazz).getActive();
    }


    protected ActiveInstance(Class clazz) {
        this(clazz, false);
    }
    
    protected ActiveInstance(Class clazz, boolean trackInstances) {
        itemType = clazz;
        itemTypeName = "<" + itemType.getName() + ">";
        itemIsMarkable = clazz.isInstance(Markable.class);
        itemsAreTracked = trackInstances;
        lock(null, "INIT");
        synchronized (AllActiveHandlers) {
            if (AllActiveHandlers.containsKey(itemType)) {
                // tho this is not ideal, the safest thing to do is blow away the old one,
                // as it's likely this accidentally happened by a request for a generic
                // listener before a specialized side-effecting type-handler was initiated.
                // We copy over the listeners from the old handler if there were any.
                tufts.Util.printStackTrace("ignoring prior active change handler for " + getClass() + " and taking over listeners");
                mListeners.addAll(getHandler(itemType).mListeners);
            }
            AllActiveHandlers.put(itemType, this);
        }
        unlock(null, "INIT");
        if (DEBUG.INIT || DEBUG.EVENTS) System.out.println("Created " + this);
    }

    public static ActiveInstance getHandler(Class type) {
        ActiveInstance handler = null;
        lock(null, "getHandler");
        synchronized (AllActiveHandlers) {
            handler = AllActiveHandlers.get(type);
            if (handler == null)
                handler = new ActiveInstance(type);
        }
        unlock(null, "getHandler");
        return handler;
    }

    public synchronized void refreshListeners() {
        notifyListeners(lastEvent);
    }

    public int instanceCount() {
        return allInstances == null ? -1 : allInstances.size();
    }
    
    public Set<T> getAllInstances() {
        if (allInstances == null)
            return null; // Collections.EMPTY_SET; // actually, an NPE would be better to know they're not tracked
        else
            return Collections.unmodifiableSet(allInstances);
    }

    /** @return true if the item was being tracked */
    public boolean stopTracking(T instance) {
        return allInstances.remove(instance);
    }

    private static String sourceName(Object s) {
        if (s == null)
            return "null";
        else if (s instanceof ActiveEvent || s instanceof ActiveInstance)
            return s.toString();
        else
            return s.getClass().getName() + ":" + s;
    }

    public void setActive(final Object source, final T newActive)
    {
        if (newActive != null && !itemType.isInstance(newActive)) {
            Util.printStackTrace(this + ": setActive(" + newActive + ") by " + source + "; not an instance of " + itemType);
            return;
        }

        // nowActive is volatile: all threads guaranteed to see it's current value w/out a synchronization
        if (nowActive == newActive)
            return;

        lock(this, "setActive");
        synchronized (this) {
            // double-checked locking of volatile:
            if (nowActive == newActive)
                return;
            final T oldActive = nowActive;
            this.nowActive = newActive;
            setActiveNotify(source, oldActive);
            unlock(this, "setActive");
        }
    }


    private synchronized void setActiveNotify(final Object source, final T oldActive)
    {
        if (DEBUG.EVENTS) {
            System.out.println(TERM_GREEN
                               + this
                               + "\n\toldActive: " + oldActive
                               + "\n\tnewActive: " + nowActive
                               + "\n\t   source: " + sourceName(source)
                               + "\n\tlisteners: " + mListeners.size() + " in " + Thread.currentThread().getName()
                               + TERM_CLEAR
                               );
        }

        if (itemsAreTracked) {
            if (allInstances == null)
                allInstances = new HashSet();
            allInstances.add(nowActive);
        }
            
        if (itemIsMarkable) {
            markActive( (Markable) oldActive, false);
            markActive( (Markable) nowActive, true);
        }
        final ActiveEvent e = new ActiveEvent(itemType, source, oldActive, nowActive);
        notifyListeners(e);
        try {
            onChange(e);
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, this + " onChange failed in implementation subclass: " + getClass());
        }
        this.lastEvent = e;
    }


    private void markActive(Markable markableItem, boolean active) {
        try {
            markableItem.markActive(active);
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, this + " marking active state to " + active + " on " + markableItem);
        }
    }

    protected void onChange(ActiveEvent<T> e) {}

    protected void notifyListeners(ActiveEvent<T> e) {
        if (inNotify) {
            tufts.Util.printStackTrace(this + " event loop! aborting delivery of: " + e);
            return;
        }
        
        inNotify = true;
        try {
            depth++;
            if (mListeners.size() > 0)
                notifyListenerList(this, e, mListeners);
            if (ListenersForAllActiveEvents.size() > 0)
                notifyListenerList(this, e, ListenersForAllActiveEvents);
        } finally {
            depth--;
            inNotify = false;
        }
        
    }
            
    protected static void notifyListenerList(ActiveInstance handler, ActiveEvent e, Collection<ActiveListener> listenerList)
    {
        final ActiveListener[] listeners;

        int count = 0;
        if (DEBUG.Enabled) lock(handler, "NOTIFY " + listenerList.size());
        synchronized (listenerList) {
            // Allow concurrent modifiation w/out synchronization:
            // (todo performance: keep an array in the handler to write this into instead
            // of having to construct if every time).
            listeners = listenerList.toArray(new ActiveListener[listenerList.size()]);
        }
        if (DEBUG.Enabled) unlock(handler, "NOTIFY " + listenerList.size());

        Object target;
        Method method;
        for (ActiveListener listener : listeners) {
            if (listener instanceof MethodProxy) {
                final MethodProxy proxy = (MethodProxy) listener;
                target = proxy.target;
                method = proxy.method;
            } else {
                target = listener;
                method = null;
            }
            count++;

            // checking the deep source might prevent us from uncovering a loop where
            // the active item is cycling -- actually, we could only get here again if
            // the active item IS cycling, because setActive of something already active
            // does nothing...
            //if (e.hasSource(target))
            
            if (e.source == target) {
                if (DEBUG.EVENTS) outf("    %2dskipSrc %s -- %s\n", count, handler.itemTypeName, target);
                continue;
            } else {
                if (DEBUG.EVENTS) outf("    %2d notify %s -> %s\n", count, handler.itemTypeName, target);
            }
                
            try {
                if (method != null)
                    method.invoke(target, e, e.active);
                else
                    listener.activeChanged(e);
            } catch (java.lang.reflect.InvocationTargetException ex) {
                tufts.Util.printStackTrace(ex.getCause(), handler + " exception notifying " + target + " with " + e);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, handler + " exception notifying " + target + " with " + e);
            }
        }
    }

    public T getActive() {
        return nowActive;
    }

    public void addListener(ActiveListener listener) {
        lock(this, "addListener");
        synchronized (mListeners) {
            if (mListeners.contains(listener)) {
                VUE.Log.warn(this + "; add: is already listening: " + listener);
            } else {
                mListeners.add(listener);
                if (DEBUG.EVENTS) outf(TERM_YELLOW + "%-50s added listener %s\n" + TERM_CLEAR, this, listener);
            }
        }
        unlock(this, "addListener");
    }

    public void addListener(Object reflectedListener) {
        Method method = null;
        try {
            // We could cache the method for the class of the given listener
            // so future instance's of the class don't have to do the method lookup,
            // but this type of listener is not frequently added.
            method = reflectedListener.getClass().getMethod("activeChanged", ActiveEvent.class, itemType);
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, this + ": "
                                       + reflectedListener.getClass()
                                       + " must implement activeChanged(ActiveEvent, " + itemType + ")"
                                       + " to be a listener for the active instance of " + itemType);
            return;
        }
        addListener(new MethodProxy(reflectedListener, method));
    }
    

    public void removeListener(ActiveListener listener) {
        lock(this, "removeListener");
        synchronized (mListeners) {
            if (!mListeners.remove(listener)) {
                VUE.Log.warn(this + "; remove: didn't contain listener " + listener);
            } else if (DEBUG.EVENTS) {
                outf(TERM_YELLOW + "%-50s removed listener %s\n" + TERM_CLEAR, this, listener);
            }
        }
        unlock(this, "removeListener");
    }

    public void removeListener(Object reflectedListener) {
        removeListener(new MethodProxy(reflectedListener, null));
    }
    

    private static void lock(ActiveInstance o, String msg) {
        if (DEBUG.THREAD) System.err.println((o == null ? "ActiveInstance" : o) + " " + msg + " LOCK");
    }
    private static void unlock(ActiveInstance o, String msg) {
        if (DEBUG.THREAD) System.err.println((o == null ? "ActiveInstance" : o) + " " + msg + " UNLOCK");
    }
    
    private static void outf(String fmt, Object... args) {
        for (int x = 0; x < depth; x++) System.err.print("    ");
        //for (int x = 0; x < depth; x++) System.err.print("----");
        System.out.format(fmt, args);
    }
    

    public String toString() {
        return "ActiveInstance" + itemTypeName;
        
    }



    private static final class MethodProxy implements ActiveListener {
        final Object target;
        final Method method;
        MethodProxy(Object t, Method m) {
            target = t;
            method = m;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(target);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MethodProxy)
                return target == ((MethodProxy)o).target;
            else
                return false;
        }

        @Override
        public String toString() {
            return "MethodProxy[" + target + "]";
        }


        public void activeChanged(ActiveEvent e) {
            /*
            try {
                method.invoke(target, e, e.active);
            } catch (java.lang.IllegalAccessException ex) {
                throw new Error(ex);
            } catch (java.lang.reflect.InvocationTargetException ex) {
                throw new Error(ex.getCause());
            }
            */
        }
        
        
            
    }
    
        
        
}
