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
import java.util.ArrayList;

/**
 * Handle dispatching of LWCEvents, mainly for LWComponents, but any client
 * class can use this for event dispatch, selective listening, and the heavy-duty
 * diagnostic support.
 * 
 * @version $Revision: 1. $ / $Date: 2006/01/20 18:49:16 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class LWChangeSupport
{
    private static int sEventDepth = 0;
    
    private List listeners;
    private Object mClient;
    private boolean mEventsDisabled = false;
    private int mEventSuspensions = 0;
    
    LWChangeSupport(Object client) {
        mClient = client;
    }
    private static class LWCListenerProxy implements LWComponent.Listener
    {
        LWComponent.Listener listener;
        private Object eventMask;

        public LWCListenerProxy(LWComponent.Listener listener, Object eventMask) {
            this.listener = listener;
            this.eventMask = eventMask;
        }

        /** this should never actually get used, as we pluck the real listener
            out of the proxy in dispatch */
        public void LWCChanged(LWCEvent e) {
            listener.LWCChanged(e);
        }

        public boolean isListeningFor(LWCEvent e)
        {
            if (eventMask instanceof Object[]) {
                final Object[] eventKeys = (Object[]) eventMask;
                for (int i = 0; i < eventKeys.length; i++)
                    if (eventKeys[i] == e.getWhat())
                        return true;
                return false;
            } else
                return eventMask == e.getWhat();
        }
        

        public String toString() {
            String s = listener.toString();
            if (eventMask != null) {
                s += ":only<";
                if (eventMask instanceof Object[]) {
                    Object[] eventKeys = (Object[]) eventMask;
                    for (int i = 0; i < eventKeys.length; i++) {
                        if (i>0) s+= ",";
                        s += eventKeys[i];
                    }
                } else {
                    s += eventMask;
                }
                s += ">";
            }
            return s;
        }
    }

    private class LWCListenerList extends java.util.Vector {
        public synchronized int indexOf(Object elem, int index) {
            if (elem == null) {
                for (int i = index ; i < elementCount ; i++)
                    if (elementData[i]==null)
                        return i;
            } else {
                for (int i = index ; i < elementCount ; i++) {
                    Object ed = elementData[i];
                    if (elem.equals(ed))
                        return i;
                    if (ed instanceof LWCListenerProxy && ((LWCListenerProxy)ed).listener == elem)
                        return i;
                }
            }
            return -1;
        }
        public synchronized int lastIndexOf(Object elem, int index) {
            throw new UnsupportedOperationException("lastIndexOf");
        }
    }

    /**
     * Move @param listener - existing listener, to the front of the
     * notification list, so it get's notifications before anyone else.
     */

    /* Todo: create ChangeSupport style class for managing & notifying
     * listeners, with setPriority, etc, that LWSelection, the VUE
     * map & viewer listeners, etc, can all use.
     */
    public synchronized void setPriorityListener(LWComponent.Listener listener) {
        int i = listeners.indexOf(listener);
        
        if (i > 0) {
            if (i == 1) {
                // swap first two
                listeners.set(1, listeners.set(0, listener));
            } else {
                // remove & add back at the front
                listeners.remove(listener);
                listeners.add(0, listener);
            }
        } else if (i == 0) {
            ; // already priority listener
        } else
            throw new IllegalArgumentException(mClient + " " + listener + " not already a listener");
    }
    
    public synchronized void addListener(LWComponent.Listener listener) {
        addListener(listener, null);
    }

    // TODO: eventMask should be of an LWKey enumeration type (java 1.5) or an
    // array of such enums.
    public synchronized void addListener(LWComponent.Listener listener, Object eventMask)
    {
        if (listeners == null)
            listeners = new LWCListenerList();
        if (listeners.contains(listener)) {
            // do nothing (they're already listening to us)
            if (DEBUG.EVENTS) {
                System.out.println("already listening to us: " + listener + " " + mClient);
                if (DEBUG.META) new Throwable("already listening to us:" + listener + " " + mClient).printStackTrace();
            }
        } else if (listener == mClient) {
            // This is likely to produce event loops, and the exception is here as a safety measure.
            throw new IllegalArgumentException(mClient + " attempt to add self as LWCEvent listener: not allowed");
        } else {
            if (DEBUG.EVENTS && DEBUG.META)
                outln("*** LISTENER " + listener + "\t+++ADDS " + mClient + (eventMask==null?"":(" eventMask=" + eventMask)));
            if (eventMask == null)
                listeners.add(listener);
            else
                listeners.add(new LWCListenerProxy(listener, eventMask));
        }
    }
    public synchronized void removeListener(LWComponent.Listener listener)
    {
        if (listeners == null)
            return;
        if (DEBUG.EVENTS) System.out.println("*** LISTENER " + listener + "\tREMOVES " + mClient);
        listeners.remove(listener);
    }
    public synchronized void removeAllListeners()
    {
        if (listeners != null) {
            if (DEBUG.EVENTS) System.out.println(mClient + " *** CLEARING ALL LISTENERS " + listeners);
            listeners.clear();
        }
    }

    private void setEventsEnabled(boolean t) {
        if (DEBUG.EVENTS&&DEBUG.META) System.out.println(mClient + " *** EVENTS ENABLED: from " + !mEventsDisabled + " to " + t);
        mEventsDisabled = !t;
    }

    protected synchronized void setEventsSuspended() {
        mEventSuspensions++;
        setEventsEnabled(false);
    }
    protected synchronized void setEventsResumed() {
        mEventSuspensions--;
        if (mEventSuspensions < 0)
            throw new IllegalStateException("events suspend/resume unpaired");
        if (mEventSuspensions == 0)
            setEventsEnabled(true);
    }

    public boolean eventsDisabled() {
        return mEventsDisabled;
    }

    /**
     * This method for clients that are LWComponent's ONLY.  Otherwise call dispatchLWCEvent
     * directly.
     */
    synchronized void notifyListeners(LWComponent client, LWCEvent e)
    {
        if (mEventsDisabled) {
            if (DEBUG.EVENTS) System.out.println(e + " (dispatch skipped: events disabled)");
            return;
        }
        
        if (client.isDeleted()) {
            System.err.println("ZOMBIE EVENT: deleted component attempting event notification:"
                               + "\n\tdeleted=" + mClient
                               + "\n\tattempted notification=" + e);
            // this situation not so serious at this point: we may have no listeners
            if (DEBUG.Enabled) new Throwable("ZOMBIE EVENT").printStackTrace();
        }
        
        if (listeners != null && listeners.size() > 0) {
            dispatchLWCEvent(client, listeners, e);
        } else {
            if (DEBUG.EVENTS && DEBUG.META)
                eoutln(e + " -> " + "<NO LISTENERS>" + (client.isOrphan() ? " (orphan)":""));
        }

        // todo: have a seperate notifyParent? -- every parent
        // shouldn't have to be a listener

        // todo: "added" events don't need to go thru parent chain as
        // a "childAdded" event has already taken place (but
        // listeners, eg, inspectors, may need to know to see if the
        // parent changed)
        
        if (client.getParent() != null) {
            if (DEBUG.EVENTS && DEBUG.META) {
                eoutln(e + " " + client.getParent() + " ** PARENT UP-NOTIFICATION");
            }
            client.getParent().broadcastChildEvent(e);
        } else if (client.isOrphan()) {
            if (listeners != null && listeners.size() > 0) {
                System.out.println("*** ORPHAN NODE w/LISTENERS DELIVERED EVENTS:"
                                   + "\n\torphan=" + client
                                   + "\n\tevent=" + e
                                   + "\n\tlisteners=" + listeners);
                if (DEBUG.PARENTING) new Throwable().printStackTrace();
            }
            /*else if (DEBUG.META && (DEBUG.EVENTS || DEBUG.PARENTING) && !(this instanceof LWGroup))
                // dragged selection group is a null parented object, so we're
                // ignoring all groups for purposes of this diagnostic for now.
                System.out.println(e + " (FYI: orphan node event)");
            */
        }
    }

    private static void eout(String s) {
        for (int x = 0; x < sEventDepth; x++) System.out.print("    ");
        System.out.print(s);
    }
    private static void eoutln(String s) {
        eout(s + "\n");
    }
    private static void outln(String s) {
        System.out.println(s);
    }
    
    public void dispatchEvent(LWCEvent e) {
        if (listeners != null)
            dispatchLWCEvent(mClient, listeners, e);
    }

    /**
     * Deliver LWCEvent @param e to all the @param listeners
     */
    //private static Listener[] listener_buf = new Listener[16];
    static synchronized void dispatchLWCEvent(Object source, List listeners, LWCEvent e)
    {
        if (sEventDepth > 5) // guestimate max based on current architecture -- increase if you need to
            throw new IllegalStateException("eventDepth=" + sEventDepth
                                            + ", assumed looping on delivery of "
                                            + e + " in " + source + " to " + listeners);

        if (source instanceof LWComponent && ((LWComponent)source).isDeleted() || listeners == null) {
            System.err.println("ZOMBIE DISPATCH: deleted component or null listeners attempting event dispatch:"
                               + "\n\tsource=" + source
                               + "\n\tlisteners=" + listeners
                               + "\n\tattempted notification=" + e);
            new Throwable("ZOMBIE DISPATCH").printStackTrace();
            return;
        }
        
        // todo perf: take array code out and see if can fix all
        // concurrent mod exceptions (e.g., delete out from under a
        // pathway was giving us some problems, tho I think that may
        // have gone away) or: allow listener removes via nulling, tho
        // that's not really a concern anyway in that a component that
        // removes itself as a listener after having been notified of
        // an event has already had it's notification and we don't
        // need to make sure it doesn't get one further down the list.
        
        //int nlistener = listeners.size();
        //Listener[] listener_array = (Listener[]) listeners.toArray(listener_buf);
        //if (listener_array != listener_buf)
        //    out("FYI: listener count " + nlistener + " exceeded performance buffer.");
        // Of course, using a static buf of course doesn't work the second we have any event depth!
        // and we can't make it a member as this is a static method...
        // We could actually just only allocate a new array if there IS any event depth...

        LWComponent.Listener[] listener_array = new LWComponent.Listener[listeners.size()];
        listeners.toArray(listener_array);
        for (int i = 0; i < listener_array.length; i++) {
            if (DEBUG.EVENTS && DEBUG.META) {
                if (e.getSource() != source)
                    eout(e + " => " + source + " >> ");
                else
                    eout(e + " >> ");
            }
            LWComponent.Listener l = listener_array[i];
            //-------------------------------------------------------
            // If a listener proxy, extract the real listener
            //-------------------------------------------------------
            if (l instanceof LWCListenerProxy) {
                LWCListenerProxy lp = (LWCListenerProxy) l;
                if (!lp.isListeningFor(e)) {
                    if (DEBUG.EVENTS && DEBUG.META)
                        outln(l + " (filtered)");
                    continue;
                }
                l = lp.listener;
            }
            //-------------------------------------------------------
            // now we know we have the real listener
            //-------------------------------------------------------
            if (DEBUG.EVENTS) {
                if (DEBUG.META) {
                    if (e.getSource() == l)
                        outln(l + " (SKIPPED: source)");
                } else if (e.getSource() != l) {
                    if (e.getSource() != source)
                        eout(e + " => " + source + " -> ");
                    else
                        eout(e + " -> ");
                }
            }
            if (e.getSource() == l) // this prevents events from going back to their source
                continue;
            sEventDepth++;
            try {
                if (DEBUG.EVENTS)
                    outln(l + "");
                //-------------------------------------------------------
                // deliver the event
                //-------------------------------------------------------

                l.LWCChanged(e);

            } catch (Throwable t) {
                tufts.Util.printStackTrace
                    (t,
                     "LWComponent.dispatchLWCEvent: exception during LWCEvent notification:"
                     + "\n\tnotifying component: " + source
                     + "\n\tevent was: " + e
                     + "\n\tfailing listener: " + l);
            } finally {
                sEventDepth--;
            }
        }
    }
}