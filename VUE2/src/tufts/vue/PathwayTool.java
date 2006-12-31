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

import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.*;
import java.util.Iterator;

/**
 * MapViewer tool functionality plus pathway contextual toolbar tool.
 * The contextual toolbar maintains a JComboBox that stays synchronized
 * with whatever is in the LWPathwayList of the current map, as well
 * as the currently selected/active pathway in the list.
 *
 * @see LWPathwayList
 * @see LWPathway
 *
 * @version $Revision: 1.26 $ / $Date: 2006-12-31 22:42:40 $ / $Author: sfraize $
 * @author  Scott Fraize
 */
public class PathwayTool extends VueSimpleTool
{
    private static JPanel sControlPanel;

    public PathwayTool() {
        super();
    }
    
    public boolean handleKeyPressed(java.awt.event.KeyEvent e)  {
        return false;
    }
    
    public boolean handleMousePressed(MapMouseEvent e)
    {
        if (!e.isAltDown())
            return false;
        
        final LWComponent hit = e.getPicked();
        final LWPathway activePathway = VUE.getActivePathway();

        out(this + " handleMousePressed " + e + " hit on " + hit + " activePathway=" + activePathway);

        if (hit == null || activePathway == null)
            return false;

        if (hit.inPathway(activePathway))
            activePathway.remove(hit);
        else
            activePathway.add(hit);
        
        return true;
    }

    
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (DEBUG.TOOL) System.out.println("PATHWAYTOOL " + e);
        GUI.makeVisibleOnScreen(this, PathwayPanel.class);
        //if (VUE.MapInspector != null)
        //    VUE.MapInspector.showTab("Pathway");
    }


    public boolean supportsSelection() { return true; }

    // todo: need selection, but no drag, and click-to-deselect still working
    public boolean supportsDraggedSelector(java.awt.event.MouseEvent e) { return true; }

    public JPanel getContextualPanel() {
        if (sControlPanel == null)
            sControlPanel = new PathwayToolPanel();
        return sControlPanel;
    }

    private static class PathwayComboBoxModel extends DefaultComboBoxModel
        implements VUE.ActiveMapListener, LWComponent.Listener
    {
        LWPathwayList mPathwayList;

        PathwayComboBoxModel() {
            VUE.addActiveMapListener(this);
            //activeMapChanged(VUE.getActiveMap());
            setPathwayList(VUE.getActiveMap());
            LWPathway current = VUE.getActivePathway();
            if (DEBUG.TOOL) System.out.println(this + ": CURRENT PATHWAY AT INIT: " + current);
            if (current != null)
                setSelectedItem(current.getDisplayLabel());
        }

        // TODO FIX: Active map not changing on tab select if the active mapviewer
        // doesn't have focus, cause then it's not knowing it's LOSING focus --
        // the tab switch is going to have to change the active map itself.
        public void activeMapChanged(LWMap map) {
            if (DEBUG.PATHWAY) System.out.println(this + ": map changed to " + map);
            setPathwayList(map);
        }
        
        public void LWCChanged(LWCEvent e) {
            if (DEBUG.PATHWAY) System.out.println(this + ": " + e);
            if (e.getComponent() instanceof LWPathway) {
                final String what = e.getWhat();
                if (e.key == LWKey.Label
                    || "pathway.deleted".equals(what)
                    || "pathway.created".equals(what)) {
                    rebuildModel();
                } else if ("pathway.list.active".equals(what)) {
                    setSelectedItem(e.getComponent().getDisplayLabel());
                }
            }
        }

        private void setPathwayList(LWMap map) {
            LWPathwayList pathwayList;
            if (map == null)
                pathwayList = null;
            else
                pathwayList = map.getPathwayList();
                
            if (mPathwayList != pathwayList) {
                if (mPathwayList != null)
                    mPathwayList.removeListener(this);
                mPathwayList = pathwayList;
                if (mPathwayList != null)
                    mPathwayList.addListener(this);
            }
            rebuildModel();
        }

        private boolean rebuilding = false;
        
        /** override */
        public void setSelectedItem(Object o) {
            if (rebuilding) {
                if (DEBUG.PATHWAY) System.out.println(this + " setSelectedItem IGNORED " + o);
                return;
            } else {
                if (DEBUG.PATHWAY) System.out.println(this + " setSelectedItem " + o);
            }
            super.setSelectedItem(o);
            if (mPathwayList != null)
                mPathwayList.setCurrentIndex(getIndexOf(o));
        }

        private void rebuildModel() {
            removeAllElements();
            rebuilding = true;
            try {
                if (mPathwayList != null) {
                    Iterator i = mPathwayList.iterator();
                    while (i.hasNext()) {
                        addElement(((LWPathway)i.next()).getDisplayLabel());
                    }
                }
            } finally {
                rebuilding = false;
            }
        }

        
    }
    private static class  PathwayToolPanel extends JPanel
    {
        public PathwayToolPanel() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setOpaque(false);
            if (false) {
                JLabel label = new JLabel("Pathway playback:  ");
                label.setBorder(new EmptyBorder(3,0,0,0));
                add(label);
            }
            JComboBox combo = new JComboBox(new PathwayComboBoxModel());
            Font f = combo.getFont();
            Font menuFont = new Font(f.getFontName(), f.getStyle(), f.getSize() - 2);
            combo.setFont(menuFont);
            GUI.applyToolbarColor(combo);
            combo.setFocusable(false);

            // A total hack so the visible height of the combo-box is squeezed down a bit
            // Setting the size only appears to work for the width, not the height.
            if (GUI.isMacAqua())
                combo.setBorder(new EmptyBorder(2,0,2,0));
            else
                combo.setBorder(new MatteBorder(2,0,2,0, GUI.getToolbarColor()));
            //combo.setBorder(new EmptyBorder(2,0,2,0)); // so height get's squeezed
            //combo.setPreferredSize(new Dimension(150, 18));
            //combo.setSize(new Dimension(150, 18));
            
            add(combo);
            add(Box.createHorizontalStrut(5));
            JPanel controls = new PathwayPanel.PlaybackToolPanel();
            //controls.setBackground(Color.red);
            controls.setOpaque(false); // so we use parents background fill color
            add(controls);
            //add(Box.createHorizontalGlue());
            add(Box.createHorizontalStrut(22));
        }
    };

    
    
}
