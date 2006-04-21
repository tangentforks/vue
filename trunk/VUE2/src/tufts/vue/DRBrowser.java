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
import tufts.vue.gui.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Digital Repository Browser
 *
 * @version $Revision: 1.44 $ / $Date: 2006-04-21 03:39:03 $ / $Author: sfraize $ 
 */
public class DRBrowser extends JPanel
{
    public static final Object SEARCH_EDITOR = "search_editor_layout_constraint";
    public static final Object SEARCH_RESULT = "search_result_layout_constraint";
    
    private static final boolean SingleDockWindowImpl = true; // these two must be exclusive
    private static final boolean DoubleDockWindowImpl = false;
    
    final JComponent searchPane = new Widget("Search") {
            private Component editor, result;
            private GridBagConstraints bc = new GridBagConstraints();
            {
                //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                //setLayout(new GridLayout(2, 1));
                //setLayout(new GridBagLayout());
                setOpaque(false);
                if (DoubleDockWindowImpl)
                    setWantsScroller(true);
                bc.weightx = 1;
                bc.gridx = 0;
                bc.gridwidth = GridBagConstraints.REMAINDER; // last in row
            }
            
            protected void addImpl(Component c, Object constraints, int idx) {
                if (DEBUG.DR) out("SEARCH-WIDGET addImpl: " + GUI.name(c) + " " + constraints + " idx=" + idx);
                JComponent jc = null;
                if (c instanceof JComponent)
                    jc = (JComponent) c;
                if (constraints == SEARCH_EDITOR) {
                    if (editor != null)
                        remove(editor);
                    editor = c;
                    bc.gridy = 0;
                    bc.weighty = 0;
                    bc.gridheight = GridBagConstraints.RELATIVE;
                    bc.fill = GridBagConstraints.HORIZONTAL;
                    constraints = BorderLayout.NORTH;
                    if (jc != null)
                        jc.setBorder(GUI.WidgetInsetBorder);
                    if (DEBUG.BOXES && jc != null) jc.setBorder(new LineBorder(Color.magenta, 4));
                } else if (constraints == SEARCH_RESULT) {

                    if (SingleDockWindowImpl) {
                        // this method of setting this is a crazy hack for now, but
                        // it's perfect for allowing us to try different layouts
                        resultsPane.removeAll();
                        resultsPane.add(jc);
                        resultsPane.setHidden(false);
                        resultsPane.validate();
                        return;
                    }
                    
                    if (result != null)
                        remove(result);
                    result = c;
                    bc.gridy = 1;
                    bc.weighty = 1;
                    bc.gridheight = GridBagConstraints.REMAINDER;
                    bc.fill = GridBagConstraints.BOTH;
                    constraints = BorderLayout.CENTER;
                    if (DEBUG.BOXES && jc != null && jc.getBorder() == null) jc.setBorder(new LineBorder(Color.pink, 4));
                } else {
                    tufts.Util.printStackTrace("illegal search pane constraints: " + constraints);
                    bc.gridy = 2; // shouldn't see this
                }
                
                //constraints = bc;
                super.addImpl(c, constraints, idx);
                revalidate();
            }

            //public void doLayout() { GUI.dumpSizes(this, "doLayout"); super.doLayout(); }
            //public void setPreferredSize(Dimension d) { Util.printStackTrace("setPreferredSize " + d);}
            //public void setLayout(LayoutManager m) {Util.printStackTrace("setLayout; " + m);super.setLayout(m);}
            

        };
    
    final JPanel librariesPanel;
    final Widget browsePane = new Widget("Browse") {
            public void setHidden(boolean hidden) {
                if (!hidden) Widget.setHiddenImpl(resultsPane, true);
                super.setHidden(hidden);
                if (hidden) Widget.setHiddenImpl(resultsPane, false);
            }
        };
    
    final Widget resultsPane = new Widget("Search Results") {
            public void setHidden(boolean hidden) {
                if (!hidden)
                    Widget.setHiddenImpl(browsePane, true);
                super.setHidden(hidden);
            }
            
            /*
            protected void addImpl(Component c, Object lc, int idx) {
                if (DEBUG.DR) out("RESULTS-WIDGET addImpl " + GUI.name(c));
                if (c instanceof WidgetStack)
                    ((JComponent)c).setBorder(new MatteBorder(1,0,0,0, Color.darkGray));
                super.addImpl(c, lc, idx);
            }
            */
            
            /*
            final JScrollPane scroller = new JScrollPane(null,
                                                         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            { scroller.setViewportBorder(new EmptyBorder(4,3,0,0)); }
            protected void addImpl(Component c, Object lc, int idx) {
                if (DEBUG.DR) out("RESULTS-WIDGET addImpl " + GUI.name(c));
                if (c instanceof JLabel) {
                    super.addImpl(c, lc, idx);
                    revalidate();
                } else {
                    scroller.setViewportView(c);
                    super.addImpl(scroller, lc, idx);
                    scroller.revalidate();
                    scroller.repaint();
                }
            }
            */
        };

    
    //final Widget previewPane = new Widget("Preview");
    //final Widget savedResourcesPane = new Widget("Saved Resources");

    final DockWindow dockWindow;
    final DockWindow searchDock;
    final DockWindow resourceDock;
    
    private JLabel loadingLabel;
    
    public DRBrowser(boolean delayedLoading, DockWindow resourceDock, DockWindow searchDock)
    {
        super(new BorderLayout());
        setName("Resources");
        //Dimension startSize = new Dimension(300,160);
        //setPreferredSize(startSize);
        //setMinimumSize(startSize);

        //this.dockWindow = dockWindow;
        this.dockWindow = resourceDock;
        this.resourceDock = resourceDock;
        this.searchDock = searchDock;
        this.librariesPanel = this;

        //setOpaque(true);
        //setBackground(Color.white);

        buildWidgets();
        
        if (delayedLoading) {
            loadingLabel = new JLabel("Loading data sources...", SwingConstants.CENTER);
            loadingLabel.setMinimumSize(new Dimension(150, 80));
            add(loadingLabel);
        } else {
            loadDataSourceViewer();
        }

        if (DoubleDockWindowImpl)
            buildDoubleDockWindow();
        else if (SingleDockWindowImpl)
            buildSingleDockWindow();
        else
            buildMultipleDockWindows();
    }

    private void buildSingleDockWindow()
    {
        resultsPane.setTitleHidden(true);
        resultsPane.setHidden(true);

        WidgetStack stack = new WidgetStack("singleDock");

        Widget.setWantsScroller(stack, true);

        stack.addPane(searchPane, 0f);
        stack.addPane(librariesPanel, 0f);
        stack.addPane(browsePane, 1f);
        stack.addPane(resultsPane, 1f);

        JLabel startLabel = new JLabel("Search Results", JLabel.CENTER);
        startLabel.setPreferredSize(new Dimension(100, 100));
        startLabel.setBorder(new MatteBorder(1,0,0,0, Color.darkGray));
        resultsPane.add(startLabel);

        this.dockWindow.setContent(stack);
    }

    private void buildDoubleDockWindow() {
        WidgetStack stack = new WidgetStack();

        stack.addPane(librariesPanel, 0f);
        stack.addPane(browsePane, 1f);
        resourceDock.setContent(stack);

        //WidgetStack searchStack = new WidgetStack();
        //searchStack.addPane(searchPane, 0f);
        searchDock.setContent(searchPane);

    }
    
    private void buildMultipleDockWindows()
    {
        // make sure the loading label will be visible
        this.dockWindow.setContent(librariesPanel);
                              
        // now create the stack of DockWindows
        DockWindow drBrowserDock = this.dockWindow;
        
        DockWindow searchDock = GUI.createDockWindow(searchPane);
        DockWindow browseDock = GUI.createDockWindow(browsePane);
        //DockWindow previewDock = GUI.createDockWindow(previewPane);
        //DockWindow savedResourcesDock = GUI.createDockWindow(savedResourcesPane);
        
        drBrowserDock.setStackOwner(true);
        drBrowserDock.addChild(searchDock);
        drBrowserDock.addChild(browseDock);
        //drBrowserDock.addChild(previewDock);
        //previewDock.setLocation(300,300);
        //drBrowserDock.addChild(savedResourcesDock);
        
        searchDock.setContent(searchPane);
        searchDock.setRolledUp(true);
        
        browseDock.setContent(browsePane);
        browseDock.setRolledUp(true);
        //browsePane.setHidden(true);
        //browseDock.setVisible(false); // won't work till after displayed
        
        //savedResourcesDock.setContent(savedResourcesPane);
        //savedResourcesDock.setRolledUp(true);
        
        //previewDock.setContent(previewPane);
        //previewDock.setRolledUp(true);

    }
    
    private void buildWidgets()
    {
        Dimension startSize = new Dimension(tufts.vue.gui.GUI.isSmallScreen() ? 250 : 400,
                                            100);        

        //-----------------------------------------------------------------------------
        // Search
        //-----------------------------------------------------------------------------
        
        searchPane.setBackground(Color.white);
        searchPane.add(new JLabel("Please Select A Library"), SEARCH_EDITOR);
		
        //-----------------------------------------------------------------------------
        // Local File Data Source
        //-----------------------------------------------------------------------------
        
        try {
            LocalFileDataSource localFileDataSource = new LocalFileDataSource("My Computer","");
            browsePane.setBackground(Color.white);
            browsePane.setLayout(new BorderLayout());
            //startSize = new Dimension(tufts.vue.gui.GUI.isSmallScreen() ? 250 : 400, 300);
            //startSize.height = GUI.GScreenHeight / 5;
            //browsePanel.setPreferredSize(startSize);
            JComponent comp = localFileDataSource.getResourceViewer();
            comp.setVisible(true);
            browsePane.add(comp);
        } catch (Exception ex) {
            if (DEBUG.DR) System.out.println("Problem loading local file library");
        }
		
        //-----------------------------------------------------------------------------
        // Saved Resources
        //-----------------------------------------------------------------------------
		
        //savedResourcesPane.setBackground(Color.white);
        //savedResourcesPane.setPreferredSize(startSize);
        //savedResourcesPane.add(new JLabel("saved resources"));
		
    }
    
    public void loadDataSourceViewer()
    {
        if (DEBUG.DR || DEBUG.Enabled) System.out.println("DRBrowser: loading the DataSourceViewer...");
            
        try {
            DataSourceViewer dsv = new DataSourceViewer(this);
            dsv.setName("Data Source Viewer");
            /*
            if (dsViewer == null) {
                // set the statics to the first initialized DRBrowser only
                dsViewer = dsv;
                //tufts.vue.VUE.dataSourceViewer = dsv;
            }
            */
            if (loadingLabel != null)
                librariesPanel.remove(loadingLabel);
            //setMinimumSize(null); some data-sources smaller: don't allow shrinkage
            //librariesPanel.setPreferredSize(null);

            //dsv.setBorder(GUI.WidgetInsetBorder);
            dsv.setBorder(new MatteBorder(0, GUI.WidgetInsets.left,
                                          0, GUI.WidgetInsets.right,
                                          Color.white));
            librariesPanel.add(dsv);

//                 System.out.println("dsv == " + dsv);
//                 System.out.println("dsv sz " + dsv.getSize());
//                 System.out.println("dsv ps " + dsv.getPreferredSize());
//                 System.out.println("DRB.SZ " + getSize());
//                 System.out.println("DRB.PS " + getPreferredSize());
//                 validate();
//                 System.out.println("validate");
//                 System.out.println("dsv sz " + dsv.getSize());
//                 System.out.println("dsv ps " + dsv.getPreferredSize());
//                 System.out.println("DRB.SZ " + getSize());
//                 System.out.println("DRB.PS " + getPreferredSize());
//                 //setMinimumSize(dsv.getPreferredSize());


            revalidate();
            // must do this to get re-laid out: apparently, the hierarchy
            // events from the add don't automatically do this!
            
            // TODO; As the DSV top-level is normally a scroll-pane, it's
            // preferred/min size is useless (always will go down
            // to one line), so either we'll need a manual size
            // set anytime there's a scroll pane, or maybe the WidgetStack
            // can detect the scroll pane, and look at the pref size of it's
            // contents.
            //setPreferredSize(dsv.getPreferredSize());
            
        } catch (Throwable e) {
            VUE.Log.error(e);
            e.printStackTrace();
            loadingLabel.setText(e.toString());
        }
        
        if (DEBUG.DR || DEBUG.Enabled) System.out.println("DRBrowser: done loading DataSourceViewer.");
    }

    private static void out(String s) {
        System.out.println("DRBrowser: " + s);
    }

    

    public static void main(String args[])
    {
        VUE.init(args);
        
        DEBUG.DR = true;

        new Frame("A Frame").setVisible(true);
        
        DockWindow dw = GUI.createDockWindow("Test Resources");
        DRBrowser drb = new DRBrowser(true, dw, GUI.createDockWindow("Search"));

        dw.setVisible(true);

        drb.loadDataSourceViewer();

        if (args.length > 1)
            tufts.vue.ui.InspectorPane.displayTestPane(null);
        
        /*
        tufts.Util.displayComponent(drb);
        try {
            java.util.prefs.Preferences p = tufts.oki.dr.fedora.FedoraUtils.getDefaultPreferences(null);
            p.exportSubtree(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
        
    }

       
    
}
