package tufts.vue;

import java.lang.reflect.Method;

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
 * @version $Revision: 1.7 $ / $Date: 2007-05-16 22:21:58 $ / $Author: sfraize $
 */

// ResourceSelection could be re-implemented using this, as long
// as we stay with only a singly selected resource object at a time.
public class ActiveChangeSupport<T>
{
    private static final java.util.Map<Class,ActiveChangeSupport> AllActiveHandlers = new java.util.HashMap();
        
    private final java.util.List<ActiveListener<T>> listenerList = new java.util.ArrayList();
    protected final Class type;
    protected final String typeName; // for debug

    private T currentlyActive;

    private boolean inNotify;


    public ActiveChangeSupport(Class clazz) {
        type = clazz;
        typeName = "<" + type.getName() + ">";
        lock(null, "INIT");
        synchronized (AllActiveHandlers) {
            if (AllActiveHandlers.containsKey(type)) {
                // tho this is an error, the safest thing to do is blow away the old one,
                // as it's likely this accidentally happened by a request for a generic
                // listener before a specialized side-effecting type-handler was initiated.
                // Listeners registered to the old handler will never get updates.
                // todo: could just copy over listener list from old handler
                tufts.Util.printStackTrace("blowing away prior active change handler for " + getClass());
            }
            AllActiveHandlers.put(type, this);
        }
        unlock(null, "INIT");
        if (DEBUG.INIT || DEBUG.EVENTS) System.out.println("Created ActiveChangeSupport"  + typeName);
    }

    public static ActiveChangeSupport getHandler(Class type) {
        ActiveChangeSupport handler = null;
        lock(null, "getHandler");
        synchronized (AllActiveHandlers) {
            handler = AllActiveHandlers.get(type);
            if (handler == null)
                handler = new ActiveChangeSupport(type);
        }
        unlock(null, "getHandler");
        return handler;
    }

    public void setActive(Object source, T newActive) {
        lock(this, "setActive");
        synchronized (this) {
            if (currentlyActive == newActive)
                return;
            if (DEBUG.EVENTS) System.out.format("ActiveInstance%s nowActive: %s  (source is %s)\n", typeName, newActive, source);
            final T oldActive = currentlyActive;
            currentlyActive = newActive;
            final ActiveEvent e = new ActiveEvent(type, source, oldActive, newActive);
            notifyListeners(e);
            onChange(e);
        }
        unlock(this, "setActive");
    }

    protected void onChange(ActiveEvent<T> e) {}

    protected void notifyListeners(ActiveEvent<T> e) {
        if (inNotify) {
            tufts.Util.printStackTrace(this + ": event loop! aborting delivery of: " + e);
            return;
        }
        
        final ActiveListener[] listeners;

        lock(this, "NOTIFY " + listenerList.size());
        synchronized (listenerList) {
            listeners = listenerList.toArray(new ActiveListener[listenerList.size()]);
        }
        unlock(this, "NOTIFY " + listenerList.size());

        inNotify = true;
        try {
            Object target;
            Method method;
            for (ActiveListener listener : listeners) {
                if (listener instanceof MethodProxy) {
                    target = ((MethodProxy)listener).target;
                    method = ((MethodProxy)listener).method;
                } else {
                    target = listener;
                    method = null;
                }

                if (target == e.source)
                    continue;
                
                if (DEBUG.EVENTS) System.out.println("\tnotify" + typeName + " -> " + target);
                try {
                    if (method != null)
                        method.invoke(target, e, e.active);
                    else
                        listener.activeChanged(e);
                } catch (java.lang.reflect.InvocationTargetException ex) {
                    tufts.Util.printStackTrace(ex.getCause(), this + " exception notifying " + target + " with " + e);
                } catch (Throwable t) {
                    tufts.Util.printStackTrace(t, this + " exception notifying " + target + " with " + e);
                }
            }
        } finally {
            inNotify = false;
        }
    }

    public T getActive() {
        return currentlyActive;
    }

    public void addListener(ActiveListener listener) {
        lock(this, "addListener");
        synchronized (listenerList) {
            listenerList.add(listener);
        }
        unlock(this, "addListener");
    }

    public void addListener(Object listener) {
        Method method = null;
        try {
            // We could cache the method for the class of the given listener
            // so future instance's of the class don't have to do the method lookup,
            // but this type of listener is not frequently added.
            method = listener.getClass().getMethod("activeChanged", ActiveEvent.class, this.type);
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, this + ": "
                                       + listener.getClass()
                                       + " must implement activeChanged(ActiveEvent, " + type + ")"
                                       + " to be a listener for the active instance of " + type);
            return;
        }
        addListener(new MethodProxy(listener, method));
    }
    

    public void removeListener(ActiveListener listener) {
        lock(this, "removeListener");
        synchronized (listenerList) {
            listenerList.remove(listener);
        }
        unlock(this, "removeListener");
    }

    private static void lock(ActiveChangeSupport o, String msg) {
        if (DEBUG.THREAD) System.err.println((o == null ? "ACS" : o) + " " + msg + " LOCK");
    }
    private static void unlock(ActiveChangeSupport o, String msg) {
        if (DEBUG.THREAD) System.err.println((o == null ? "ACS" : o) + " " + msg + " UNLOCK");
    }
    
    public void removeListener(Object listener) {
        throw new UnsupportedOperationException("implement MethodProxy removal");
    }

    public String toString() {
        return "ActiveChangeSupport" + typeName;
        
    }



    private static class MethodProxy implements ActiveListener {
        final Object target;
        final Method method;
        MethodProxy(Object t, Method m) {
            target = t;
            method = m;
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
