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

/**
 * Global VUE application debug flags.
 * 
 * @version $Revision: 1.1 $ / $Date: 2005/12/02 15:50:23 $ / $Author: sfraize $ 
 */
public class DEBUG
{
    public static boolean Enabled = false; // can user turn debug switches on

    public static boolean INIT = false; // startup / initializations
    
    // Can leave these as static for runtime tweaking, or
    // make final static to have compiler strip them out entirely.
    public static boolean CONTAINMENT = false;
    public static boolean PARENTING = false;
    public static boolean LAYOUT = false;
    public static boolean BOXES = false;
    public static boolean SELECTION = false;
    public static boolean UNDO = false;
    public static boolean PATHWAY = false;
    public static boolean DND = false; // drag & drop
    public static boolean MOUSE = false;
    public static boolean VIEWER = false; // MapViewer
    public static boolean PAINT = false; // painting
    public static boolean ROLLOVER = false; // MapViewer auto-zoom rollover
    public static boolean SCROLL = false; // MapViewer scroll-bars / scrolling
    public static boolean FOCUS = false; // AWT focus events, VUE MapViewer application focus
    public static boolean EVENTS = false; // VUE LWCEvents & Action Events (not tool events)
    public static boolean MARGINS = false; // turn off bounds margin adjustments for testing
    public static boolean DYNAMIC_UPDATE = true; // components process all LWCEvent's immediately
    public static boolean KEYS = false; // keyboard input
    public static boolean TOOL = false; // toolbars & tool events
    public static boolean EDGE = false; // window edges & sticking
    public static boolean IMAGE = false; // images
    public static boolean CASTOR = false; // castor persist (save/restore)
    public static boolean XML = false; // castor persist (save/restore)
    public static boolean THREAD = false; // threading
    public static boolean TEXT = false; // text objects
    public static boolean IO = false; // file and network i/o
    public static boolean DOCK = false; // DockWindow's
    public static boolean WIDGET = false; // Widget's
    public static boolean DATA = false; // data production / meta-data
    public static boolean RESOURCE = false; // Resources
    public static boolean PRESENT = false;
    public static boolean PICK = false;
    public static boolean LISTS = false; //for debugging UL and OL in HTML textbox code
    //If you set LISTS to true you'll get the HTML code for the node in the Info Label
    //instead of the rendered HTML this should be useful for debugging, at least I hope so.
    //see my note in InspectorPane for more info. -MK
    public static boolean WORK = false; // work-in-progress

    public static boolean DR = false; // digital repository & data sources
    
    public static boolean META = false; // generic toggle to use in combination with other flags

    public static  void setAllEnabled(boolean t) {
        Enabled=CONTAINMENT=PARENTING=LAYOUT=BOXES=ROLLOVER=EVENTS=
            SCROLL=SELECTION=FOCUS=UNDO=PATHWAY=DND=MOUSE=VIEWER=
            PAINT=MARGINS=INIT=DYNAMIC_UPDATE=KEYS=TOOL=DR=IMAGE=
            CASTOR=XML=THREAD=TEXT=EDGE=IO=DOCK=WIDGET=DATA=PRESENT=PICK=t;

        // only turn META & WORK off, not on
        if (t == false)
            META = WORK = false;
    }

    public static void parseArg(String arg) {
        if (arg == null || !arg.toLowerCase().startsWith("-debug"))
            return;

        if (arg.length() < 7)
            return;

        arg = arg.substring(6);

        //System.out.println("parsing arg group[" + arg + "]");

        String[] args;

        if (arg.charAt(0) == ':')
            args = arg.substring(1).split(",");
        else if (arg.charAt(0) == '_')
            args = new String[] { arg.substring(1) };
        else
            return;

        for (int i = 0; i < args.length; i++) {
            String a = args[i].toLowerCase();

            //System.out.println("parsing arg [" + a + "]");
            
                 if (a.equals("meta"))       DEBUG.META = true;
            else if (a.equals("work"))       DEBUG.WORK = !DEBUG.WORK;
            else if (a.equals("init"))       DEBUG.INIT = true;
            else if (a.equals("focus"))      DEBUG.FOCUS = true;
            else if (a.equals("dr"))         DEBUG.DR = true;
            else if (a.equals("tool"))       DEBUG.TOOL = true;
            else if (a.equals("drop"))       DEBUG.DND = true;
            else if (a.equals("undo"))       DEBUG.UNDO = true;
            else if (a.equals("castor"))     DEBUG.CASTOR = true;
            else if (a.equals("xml"))        DEBUG.XML = true;
            else if (a.equals("paint"))      DEBUG.PAINT = true;
            else if (a.equals("mouse"))      DEBUG.MOUSE = true;
            else if (a.equals("keys"))       DEBUG.KEYS = true;
            else if (a.equals("layout"))     DEBUG.LAYOUT = true;
            else if (a.equals("text"))       DEBUG.TEXT = true;
            else if (a.equals("io"))         DEBUG.IO = true;
            else if (a.equals("data"))       DEBUG.DATA = true;
            else if (a.equals("selection"))  DEBUG.SELECTION = true;
            else if (a.equals("resource"))   DEBUG.RESOURCE = true;
            else if (a.equals("scroll"))     DEBUG.SCROLL = true;
            else if (a.equals("pick"))       DEBUG.PICK = true;
            else if (a.startsWith("parent")) DEBUG.PARENTING = true;
            else if (a.startsWith("path"))   DEBUG.PATHWAY = true;
            else if (a.startsWith("edge"))   DEBUG.EDGE = true;
            else if (a.startsWith("event"))  DEBUG.EVENTS = true;
            else if (a.startsWith("thread")) DEBUG.THREAD = true;
            else if (a.startsWith("image"))  DEBUG.IMAGE = true;
            else if (a.startsWith("box"))    DEBUG.BOXES = true;
            else if (a.startsWith("dock"))   DEBUG.DOCK = true;
            else if (a.startsWith("widget")) DEBUG.WIDGET = true;
            else if (a.startsWith("pres"))   DEBUG.PRESENT = true;
        }
    }

    //Mapper pSELECTION = new Mapper("selection") { void set(boolean v) { selection=v; } boolean get() { return selection; } }

    /*
    abstract class Mapper {
        String mName;
        Mapper(String name) { mName = name; }
        abstract void set(boolean v);
        abstract boolean get();
    }
    Mapper[] props = {
        new Mapper("selection") { void set(boolean v) { SELECTION=v; } boolean get() { return SELECTION; } },
        new Mapper("scroll") { void set(boolean v) { SCROLL=v; } boolean get() { return SCROLL; } }
    };
    // use introspection instead
    */
}
