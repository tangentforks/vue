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

import java.awt.*;
import java.awt.event.*;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.border.*;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * Code for handling a tabbed pane of MapViewer's: adding, removing,
 * keeping tab labels current & custom appearance tweaks.
 *
 * @version $Revision: 1.34 $ / $Date: 2007-05-09 19:59:45 $ / $Author: sfraize $ 
 */

// todo: need to figure out how to have the active map grab
// the focus if no other map has focus: switching tabs
// changes the map you're looking it, and it's set to
// the active map, but it doesn't get focus unless you click on it!
public class MapTabbedPane extends JTabbedPane
    implements LWComponent.Listener, FocusListener, MapViewer.Listener
{
    private Color BgColor;
    private String name;
    
    MapTabbedPane(String name) {
        this.name = name;
        setName("mapTabs-" + name);
        setFocusable(false);
        BgColor = GUI.getToolbarColor();
        setTabPlacement(javax.swing.SwingConstants.TOP);
        setPreferredSize(new Dimension(300,400));

        /*//getModel().
        addChangeListener(new javax.swing.event.ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent e) {
                    if (DEBUG.Enabled) out("stateChanged: selectedIndex=" + getSelectedIndex());
                }
                });*/
    }
        
    private int mWasSelected = -1;
    protected void fireStateChanged() {
        try {
            if (true||DEBUG.FOCUS) out("fireStateChanged, selectedIndex=" +getSelectedIndex());
            super.fireStateChanged();
        } catch (ArrayIndexOutOfBoundsException e) {
            // this is happening after we close everything and then
            // open another map -- no idea why, but this successfully
            // ignores it.
            System.err.println(this + " JTabbedPane.fireStateChanged: " + e);
        }
        
        int selected = getModel().getSelectedIndex();
        
        if (!GUI.isMacAqua()) { // don't mess w/aqua colors
            
            if (mWasSelected >= 0)
                setForegroundAt(mWasSelected, Color.darkGray);
            
            if (selected >= 0) {
                setForegroundAt(selected, Color.black);
                setBackgroundAt(selected, BgColor);
            }
        }
        
        mWasSelected = selected;
        MapViewer viewer = getSelectedViewer();
        if (viewer != null) {
            if (DEBUG.FOCUS) out("REQUESTING FOCUS FOR " + viewer);
            viewer.requestFocus();
        }
    }
        
    public void reshape(int x, int y, int w, int h) {
        boolean ignore =
            getX() == x &&
            getY() == y &&
            getWidth() == w &&
            getHeight() == h;
            
        // if w or h <= 0 we can know we're being hidden
        //System.out.println(this + " reshape " + x + "," + y + " " + w + "x" + h + (ignore?" (IGNORING)":""));
        super.reshape(x,y, w,h);
    }

    public MapViewer getSelectedViewer() {
        return getViewerAt(getSelectedIndex());
    }

    public void focusGained(FocusEvent e) {
        out("focusGained (from " + e.getOppositeComponent() + ")");
    }
    public void focusLost(FocusEvent e) {
        out("focusLost (to " + e.getOppositeComponent() + ")");
    }
    public void addNotify() {
        super.addNotify();
        if (!VueUtil.isMacPlatform()) {
            setForeground(Color.darkGray);
            setBackground(BgColor);
        }
        //addFocusListener(this); // hope not to hear anything...
        // don't let us be focusable or sometimes you can select
        // & activate a new map for interaction, but we keep
        // the focus here in the tabbed instead of giving to
        // the component in the tab.
        //setFocusable(false); // in constructor
    }
        
    private String mapToTabTitle(LWMap map) {
        String title = map.getLabel();
        if (title.toLowerCase().endsWith(".vue") && title.length() > 4)
            title = title.substring(0, title.length() - 4);
        if (map.isCurrentlyFiltered())
            title += "*";
        return title;
    }

    private String viewerToTabTitle(MapViewer viewer) {

        return mapToTabTitle(viewer.getMap()) + " (" + ZoomTool.prettyZoomPercent(viewer.getZoomFactor()) + ")";
        /*
        
        String title = mapToTabTitle(viewer.getMap());

        // Present the zoom factor as a percentange
        
        title += " (";
        double zoomPct = viewer.getZoomFactor() * 100;
        if (zoomPct < 10) {
            // if < 10% zoom, show with 1 digit of decimal value if it would be non-zero
            title += VueUtil.oneDigitDecimal(zoomPct);
        } else {
            //title += (int) Math.round(zoomPct);
            title += (int) Math.floor(zoomPct + 0.49);
        }
        return title + "%)";
        */
    }

    private void updateTitleTextAt(int i) {
        if (i >= 0) {
            MapViewer viewer = getViewerAt(i);
            setTitleAt(i, viewerToTabTitle(viewer));
            LWMap map = viewer.getMap();
            String tooltip = null;
            if (map.getFile() != null)
                tooltip = map.getFile().toString();
            else
                tooltip = "<html>&nbsp;<i>Unsaved</i> &nbsp;";
            if (map.isCurrentlyFiltered())
                tooltip += " (filtered)";
            setToolTipTextAt(i, tooltip);
        }
    }
    
    static final int TitleChangeMask =
        MapViewerEvent.DISPLAYED |
        MapViewerEvent.FOCUSED |
        MapViewerEvent.ZOOM;        // title includes zoom

    private static MapTabbedPane lastFocusPane;
    private static int lastFocusIndex = -1;
    public void mapViewerEventRaised(MapViewerEvent e) {
        if ((e.getID() & TitleChangeMask) != 0) {
            int i = indexOfComponent(e.getMapViewer());
            if (i >= 0) {
                updateTitleTextAt(i);
                if (e.getID() == MapViewerEvent.FOCUSED) {
                    if (lastFocusPane != null) {
                        //lastFocusPane.setIconAt(lastFocusIndex, null);
                        lastFocusPane = null;
                    }
                    if (VUE.multipleMapsVisible()) {
                        lastFocusPane = this;
                        lastFocusIndex = i;
                        //setIconAt(i, new BlobIcon(5,5, Color.green));
                    }
                }
            }
        }
    }
    
    public void LWCChanged(LWCEvent e) {
        if (e.getSource() instanceof LWMap)
            updateTitleTextAt(findTabWithMap((LWMap)e.getSource()));
    }
        
    public void addViewer(MapViewer viewer) {
        
        Component c = new tufts.vue.gui.MapScrollPane(viewer);
        
        if (false) {
            String tabTitle = viewerToTabTitle(viewer);
            if (tabTitle == null)
                tabTitle = "unknown";
            System.out.println("Adding tab '" + tabTitle + "' component=" + c);
            addTab(tabTitle, c);
        } else {
            addTab(viewerToTabTitle(viewer), c);
        }
        
        LWMap map = viewer.getMap();
        map.addLWCListener(this, LWKey.MapFilter, LWKey.Label);
        // todo perf: we should be able to ask to listen only
        // for events from this object directly (that we don't
        // care to hear from it's children)
        updateTitleTextAt(indexOfComponent(c)); // first time just needed for tooltip
    }
        
    /*
    // put BACKINGSTORE mode on a diag switch and test performance difference -- the
    // obvious difference is vastly better performance if an inspector window is
    // obscuring any part of the canvas (or any other window for that mater), which
    // kills off a huge chunk of BLIT_SCROLL_MODE's optimization.  However, using
    // backing store completely fucks up if we start hand-panning the map, tho I'm
    // presuming that's because the hand panning isn't being done thru the viewport yet.
    //sp.getViewport().setScrollMode(javax.swing.JViewport.BACKINGSTORE_SCROLL_MODE);
    public void addTab(LWMap pMap, Component c)
    {
    //scroller.getViewport().setScrollMode(javax.swing.JViewport.BACKINGSTORE_SCROLL_MODE);
    //super.addTab(pMap.getLabel(), c instanceof JScrollPane ? c : new JScrollPane(c));
    super.addTab(pMap.getLabel(), c);
    pMap.addLWCListener(this);
    if (pMap.getFile() != null)
    setToolTipTextAt(indexOfComponent(c), pMap.getFile().toString());
    }
    */
        

    /**
     * Will find either the component index (default superclass
     * behavior), or, if the component found at any location
     * is a JScrollPane, look within it at the JViewport's
     * view, and if it matches the component sought, return that index.
     */
    public int indexOfComponent(Component component) {
        for (int i = 0; i < getTabCount(); i++) {
            Component c = getComponentAt(i);
            if ((c != null && c.equals(component)) ||
                (c == null && c == component)) {
                return i;
            }
            if (c instanceof JScrollPane) {
                if (component == ((JScrollPane)c).getViewport().getView())
                    return i;
            }
        }
        return -1;
    }
        
    public MapViewer getViewerAt(int index) {
        Object c;
        try {
            c = getComponentAt(index);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
        MapViewer viewer = null;
        if (c instanceof MapViewer)
            viewer = (MapViewer) c;
        else if (c instanceof JScrollPane)
            viewer = (MapViewer) ((JScrollPane)c).getViewport().getView();
        return viewer;
    }
        
    public LWMap getMapAt(int index) {
        MapViewer viewer = getViewerAt(index);
        LWMap map = null;
        if (viewer == null && VUE.inFullScreen()) // hack, but works for now: todo: cleaner
            return VUE.getActiveMap();
        if (viewer != null)
            map = viewer.getMap();
        //System.out.println(this + " map at index " + index + " is " + map);
        return map;
    }
    
    public Iterator<LWMap> getAllMaps() {
        int tabs = getTabCount();
        ArrayList<LWMap> list = new ArrayList();
        for(int i= 0;i< tabs;i++){
            LWMap m = getMapAt(i);
            list.add(m);
        }
        return list.iterator();
    }

    public MapViewer getViewerWithMap(LWMap map) {
        return getViewerAt(findTabWithMap(map));
    }
        
    private int findTabWithMap(LWMap map) {
        int tabs = getTabCount();
        for (int i = 0; i < tabs; i++) {
            LWMap m = getMapAt(i);
            if (m != null && m == map) {
                //System.out.println(this + " found map " + map + " at index " + i);
                return i;
            }
        }
        out("failed to find map " + map);
        return -1;
    }

    public void closeMap(LWMap map) {
        if (DEBUG.FOCUS) out("closeMap " + map);
        
        int mapTabIndex = findTabWithMap(map);
        MapViewer viewer = getViewerAt(mapTabIndex);

        if (DEBUG.FOCUS) out("closeMap"
                             + "\n\t   indexOfMap=" + mapTabIndex
                             + "\n\tviewerAtIndex=" + viewer
                             + "\n\t activeViewer=" + VUE.getActiveViewer());
        
        // Note: if we close out the last tab (while it's selected), the selected index
        // must change, and the JTabbedPane acts sanely, delivers change events, and
        // causes the MapViewer in the previously second-to-last-tab, now in the last
        // tab, to gain focus.  However, if any OTHER tab is removed, technically the
        // selected index can (and does) stay the same, which makes sense, except the
        // selected ITEM is now different, and JTabbedPane is completely ignorant of
        // this, and does nothing, and delivers focus to nothing, so we must handle that
        // manually.  This also reveals a weaknes the DefaultSingleSelectionModel, which
        // has no code to deal with the case of a selected item from change out from
        // under the selected index.  So what we need to do is make sure the right
        // MapViewer forcably grabs the focus.
        
        boolean forceFocusTransfer = false;

        if (viewer == VUE.getActiveViewer()) {

            // If this is the active viewer, we may need to manage
            // a focus transfer.
            
            // Apparently, even sometimes when it's the last tab that changes, JTabbedPane fails
            // to tansfer focus, so we do this always...
            //if (mapTabIndex != getTabCount() - 1)
                forceFocusTransfer = true;
                
            // Immediately make sure nothing can refer this this viewer.
            //VUE.setActive(MapViewer.class, this, null);

            // we might want to force notification even if selection is already empty:
            // we want all listeners, particularly the actions, to
            // update in case this is last map open
            VUE.getSelection().clear();
        }

        
        removeTabAt(mapTabIndex);

        if (forceFocusTransfer) {
            int selectedIndex = getSelectedIndex();
            if (DEBUG.FOCUS) out("FORCE FOCUS TRANSFER TO selected tab index: " + selectedIndex);
            if (selectedIndex >= 0)
                getViewerAt(selectedIndex).grabVueApplicationFocus("closeMap", null);
        }
    }
        
    public void paintComponent(Graphics g) {
        ((Graphics2D)g).setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                         java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(g);
    }
        
    public String toString() {
        return "MapTabbedPane<"+name+">";
    }

    private void out(String s) {
        System.out.println(this + ": " + s);
    }
        
}
