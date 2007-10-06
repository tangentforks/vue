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

/**
 * The currently selected resource.  Currently only supports a single selected
 * resource at a time.
 *
 * @version $Revision: 1.10 $ / $Date: 2007-10-06 03:49:26 $ / $Author: sfraize $
 */
//public class ResourceSelection {}
public class ResourceSelection
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ResourceSelection.class);
    
    private Resource selected = null;
    
    private java.util.List listeners = new java.util.ArrayList();

    private int notifyCount = 0;
    private boolean inNotify = false;

    public static class Event {
        final public Resource selected;
        final public Object source;
        public Event(Resource r, Object src) {
            selected = r;
            source = src;
        }
    }

    public interface Listener extends java.util.EventListener {
        void resourceSelectionChanged(Event e);
    }
    
    public synchronized void addListener(Listener l)
    {
        if (DEBUG.SELECTION) out("adding listener: " + GUI.namex(l));
        listeners.add(l);
    }
    public synchronized void removeListener(Listener l)
    {
        if (DEBUG.SELECTION) out("removing listener: " + GUI.namex(l));
        listeners.remove(l);
    }

    private synchronized void notifyListeners(Object source)
    {
        if (inNotify)
            tufts.Util.printStackTrace("looped resource notification from " + GUI.namex(source));
        inNotify = true;
        try {
            notifyCount++;
            notifyListenersImpl(source);
        } finally {
            inNotify = false;
        }
    }
    
    private synchronized void notifyListenersImpl(Object source)
    {
        java.util.Iterator i = listeners.iterator();
        Event e = new Event(this.selected, source);
        if (DEBUG.SELECTION) out("notification from source " + GUI.namex(source) + " of " + selected);
        while (i.hasNext()) {
            Listener l = (Listener) i.next();
            if (l == source) {
                if (DEBUG.SELECTION) out("skipping source: " + GUI.namex(l));
                continue;
            }
            if (DEBUG.SELECTION) out("notifying: " + GUI.namex(l));
            l.resourceSelectionChanged(e);
        }
        if (DEBUG.SELECTION) out("notifications complete from source " + GUI.namex(source) + " of " + selected);
        
    }
       
    public synchronized Resource get() {
        return selected;
    }
    
    /*
    public synchronized void setTo(Resource r) {
        setTo(r, null);
    }
    */
    
    public synchronized void setTo(Resource r, Object src) {
        if (selected != r) {
            if (DEBUG.SELECTION) out("set to " + tufts.Util.tag(r) + " " + r);
            selected = r;
            notifyListeners(src);
        }
    }
    
    public synchronized void clearAndNotify() {
    	clear0();
    	notifyListeners(null);
    }
    
    public synchronized void clear()
    {
        if (clear0())
            notifyListeners(null);
    }

    private synchronized boolean clear0()
    {
        if (selected == null)
            return false;
        
        if (DEBUG.SELECTION) out("clear " + selected);
        selected = null;
        return true;
    }

    private void out(Object o) {
        String s = "ResourceSelection#" + notifyCount + " " + (""+System.currentTimeMillis()).substring(8);
        s += " [" + Thread.currentThread().getName() + "]";
        if (false)
            Log.debug(s + " " + (o==null?"null":o.toString()));
        else
            System.err.println(s + " " + (o==null?"null":o.toString()));
    }
    
    
}
