package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import tufts.vue.action.*;

/**
 * Vue application class.
 * Create an application frame and layout all the components
 * we want to see there (including menus, toolbars, etc).
 *
 */
public class VUE
    implements VueConstants
{
    public static final String CASTOR_XML_MAPPING = Vue2DMap.CASTOR_XML_MAPPING;

    public static Cursor CURSOR_ZOOM_IN;
    public static Cursor CURSOR_ZOOM_OUT;
    
    public static JFrame frame;
    //set to public so that action package can access it (Jay Briedis 6/4/03)
    public static JTabbedPane tabbedPane;
    
    static {
        /*
        String imgLocation = "toolbarButtonGraphics/navigation/Back24.gif";
        URL imageURL = getClass().getResource(imgLocation);
        if (imageURL != null)
            button = new JButton(new ImageIcon(imageURL));
        */

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image iconZoomIn;
        Image iconZoomOut;
        if (VueUtil.isMacPlatform()) {
            iconZoomIn = toolkit.getImage("images/ZoomIn16.gif");
            iconZoomOut = toolkit.getImage("images/ZoomOut16.gif");
        } else {
            iconZoomIn = toolkit.getImage("images/ZoomIn24.gif");
            iconZoomOut = toolkit.getImage("images/ZoomOut24.gif");
        }
        CURSOR_ZOOM_IN = toolkit.createCustomCursor(iconZoomIn, new Point(0,0), "ZoomIn");
        CURSOR_ZOOM_OUT = toolkit.createCustomCursor(iconZoomOut, new Point(0,0), "ZoomOut");
    }

    static class VueFrame extends JFrame
        implements MapViewerListener
    {
        final int TitleChangeMask = MapViewerEvent.DISPLAYED | MapViewerEvent.ZOOM;
        
        VueFrame()
        {
            super("VUE: Tufts Concept Map Tool");
        }
        public void mapViewerEventRaised(MapViewerEvent e)
        {
            if ((e.getID() & TitleChangeMask) != 0)
                setTitleFromViewer(e.getMapViewer());
        }

        private void setTitleFromViewer(MapViewer viewer)
        {
            String title = "VUE: " + viewer.getMap().getLabel();
            
            int displayZoom = (int) (viewer.getZoomFactor() * 10000.0);
            // round the display value down to 2 digits
            title += " [";
            if ((displayZoom / 100) * 100 == displayZoom)
                title += (displayZoom / 100) + "%";
            else
                title += (((float) displayZoom) / 100f) + "%";
            title += "]";
            setTitle(title);
        }
    }

    static class VuePanel extends JPanel
    {
        public void paint(Graphics g)
        {
            // only works when, of course, the panel is asked
            // to redraw -- but if you mess with subcomponents
            // and just they repaint, we lose this.
            // todo: There must be a way to stick this in a global
            // property somewhere.
            ((Graphics2D)g).setRenderingHint
                (RenderingHints.KEY_TEXT_ANTIALIASING,
                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paint(g);
        }
    }

    static {
        if (false && VueUtil.isMacPlatform()) {
            final String usmbProp = "apple.laf.useScreenMenuBar";
            final String appNameProp = "com.apple.mrj.application.apple.menu.about.name";
            if (System.getProperty(usmbProp) == null)
                System.setProperty(usmbProp, "true");
            // setting appNameProp here doesn't do anything anything since VM
            // has already made use of this property...
            System.setProperty(appNameProp, "VUE");
        }
    }

    public static void activateWaitCursor()
    {
        // todo: save current cursor and pop off stack when we clear
        SwingUtilities.getRootPane(VUE.frame).setCursor(CURSOR_WAIT);
    }
    public static void clearWaitCursor()
    {
        SwingUtilities.getRootPane(VUE.frame).setCursor(CURSOR_DEFAULT);
    }

    private VUE() {}
    
    public static void main(String[] args)
    {
        String laf = null;
        //laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        //laf = javax.swing.UIManager.getCrossPlatformLookAndFeelClassName();
        try {
            if (laf != null)
                javax.swing.UIManager.setLookAndFeel(laf);
        } catch (Exception e) {
            System.err.println(e);
        }
        
        /*
         * create an example map (this will become
         * map loading code after the viewer is up)
         */
        Vue2DMap map1 = new Vue2DMap("One");
        Vue2DMap map2 = new Vue2DMap("Two");
        Vue2DMap map3 = new Vue2DMap("Empty Map");

        installExampleMap(map1);
        installExampleMap(map2);
        installExampleNodes(map1);
        
        /*
         * create the map viewer
         */

        MapViewer mapViewer1 = new tufts.vue.MapViewer(map1);
        MapViewer mapViewer2 = new tufts.vue.MapViewer(map2);
        MapViewer mapViewer3 = new tufts.vue.MapViewer(map3);
        //MapViewer mapViewer4 = new tufts.vue.MapViewer(map1);

        tabbedPane = new JTabbedPane();        
        tabbedPane.addTab(map1.getLabel(), mapViewer1);
        //tabbedPane.addTab(map1.getLabel()+"[View2]", mapViewer4);
        // todo: can support seperate views, EXCEPT for selection bit
        // that exsits in the LWComponent itself...
        tabbedPane.addTab(map2.getLabel(), mapViewer2);
        tabbedPane.addTab(map3.getLabel(), mapViewer3);
        
        tabbedPane.setSelectedIndex(0);
        tabbedPane.setTabPlacement(SwingConstants.BOTTOM);
        //tabbedPane.setTabPlacement(SwingConstants.TOP);
        
        /*
         * create a an application frame and layout components
         */
        
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BorderLayout());
        toolPanel.add(new DRBrowser(), BorderLayout.CENTER);
        //toolPanel.add(new MapPanner(mapViewer1), BorderLayout.CENTER);
        toolPanel.add(new LWCInspector(), BorderLayout.SOUTH);
        //toolPanel.add(new MapItemInspector(), BorderLayout.SOUTH);


        JSplitPane splitPane = new JSplitPane();
        //JScrollPane leftScroller = new JScrollPane(toolPanel);

        splitPane.setResizeWeight(0.25); // 25% space to the left component
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setLeftComponent(toolPanel);
        //splitPane.setLeftComponent(leftScroller);
        splitPane.setRightComponent(tabbedPane);


        frame = new VueFrame();
        JPanel vuePanel = new VuePanel();
        vuePanel.setLayout(new BorderLayout());
        vuePanel.add(splitPane, BorderLayout.CENTER);
        //vuePanel.add(splitPane);

        // Create the tool windows
        ToolWindow pannerTool = new ToolWindow("Panner", frame);
        pannerTool.setSize(120,120);
        pannerTool.addTool(new MapPanner(mapViewer1));

        ToolWindow inspectorTool = new ToolWindow("Inspector", frame);
        inspectorTool.addTool(new LWCInspector());
        //inspectorTool.addTool(new MapItemInspector());

        Action[] windowActions = { pannerTool.getDisplayAction(),
                                   inspectorTool.getDisplayAction() };

        // adding the menus and toolbars
        setMenuToolbars(frame, windowActions);
        
        frame.getContentPane().add(vuePanel,BorderLayout.CENTER);
        //frame.setContentPane(vuePanel);
        //frame.setContentPane(splitPane);
        frame.setBackground(Color.white);
        frame.pack();

        Dimension d = frame.getToolkit().getScreenSize();
        int x = d.width/2 - frame.getWidth()/2;
        int y = d.height/2 - frame.getHeight()/2;
        frame.setLocation(x, y);
        
        frame.show();
        

    }

    public static MapViewer getActiveViewer()
    {
        return (MapViewer) tabbedPane.getSelectedComponent();
    }
    public static Vue2DMap getActiveMap()
    {
        return getActiveViewer().getMap();
    }

    public static void displayMap(Vue2DMap map)
    {
        // todo: figure out if we're already displaying this map
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            MapViewer mv = (MapViewer) tabbedPane.getComponentAt(i);
        }
        MapViewer mapViewer = new tufts.vue.MapViewer(map);
        tabbedPane.addTab(map.getLabel(), mapViewer);
        tabbedPane.setSelectedComponent(mapViewer);
    }
    
    // really, this wants to be ComponentAction or something
    // also, it doesn't belong in the MapViewer -- put it
    // in VUE?  Vue2DMap?  Main menu's will also need to use this.
    //private MapAction maBringToFront = new MapAction("Bring to Front");

    //REALLY, these are ConceptMapActions -- they take a selection
    // and make changes to a Vue2DMap based on what's in it.

    // set to VUE.this for the singleton VUE instance
    // We need this to access the actions statically.

    static class Actions {
        static final int META = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
        static final int CTRL = Event.CTRL_MASK;
        static final int SHIFT = Event.SHIFT_MASK;

        static final private KeyStroke keyStroke(int vk, int mod) {
            return KeyStroke.getKeyStroke(vk, mod);
        }
        
    static final MapAction Copy = new MapAction("Copy") {
            void Xact(LWComponent c) {

            }};
    static final MapAction Group =
        new MapAction("Group", keyStroke(KeyEvent.VK_G, CTRL))
        {
            // enable only when two or more objects in selection
            boolean enabled(java.util.List l) { return l.size() > 1; }
            void Xact(java.util.List selectionList)
            {
                //getMap().createGroup(selectionList);
            }
        };
    static final MapAction Ungroup =
        new MapAction("Ungroup", keyStroke(KeyEvent.VK_G, CTRL+SHIFT))
        {
            void act(LWComponent c) {
                if (c instanceof LWGroup)
                    System.out.println("found group " + c);
            }
        };
    static final MapAction Rename =
        new MapAction("Rename")
        {
            boolean enabled(java.util.List l) { return l.size() == 1; }
            void act(LWComponent c) {
                getActiveViewer().activateLabelEdit(c);
            }
        };
    static final MapAction Delete = new MapAction("Delete") {
            void act(java.util.List selectionList)
            {
                // need to copy list as selection gets modified as we delete
                Object[] comps = selectionList.toArray();
                for (int i = 0; i < comps.length; i++) {
                    LWComponent c = (LWComponent) comps[i];
                    c.getParent().removeChild(c);
                }
            }};
    static final MapAction BringToFront =
        new MapAction("Bring to Front",
                      "Raise object to the top, completely unobscured",
                      keyStroke(KeyEvent.VK_CLOSE_BRACKET, CTRL+SHIFT))
        {
            void act(java.util.List selection) {
                LWGroup.bringToFront(selection);
            }
        };
    static final MapAction SendToBack =
        new MapAction("Send to Back",
                      "Make sure this object doesn't obscure any other object",
                      keyStroke(KeyEvent.VK_OPEN_BRACKET, CTRL+SHIFT))
        {
            void act(java.util.List selection) {
                LWGroup.sendToBack(selection);
            }
        };
    static final MapAction BringForward =
        new MapAction("Bring Forward", keyStroke(KeyEvent.VK_CLOSE_BRACKET, CTRL))
        {
            void act(java.util.List selection) {
                LWGroup.bringForward(selection);
            }
        };
    static final MapAction SendBackward =
        new MapAction("Send Backward", keyStroke(KeyEvent.VK_OPEN_BRACKET, CTRL))
        {
            void act(java.util.List selection) {
                LWGroup.sendBackward(selection);
            }
        };
    }

    //componentaction? MapSelectionAction?
    static class MapAction extends AbstractAction {
        // handles an action on a selection (single or group)

        // statically listen for selection events as their's
        // only ever one selection, altho everybody needs
        // to do their own check, so I guess everyone needs
        // to listen anyway..
        private MapAction(String name, String shortDescription, KeyStroke keyStroke)
        {
            super(name);
            if (shortDescription != null)
                putValue(SHORT_DESCRIPTION, shortDescription);
            if (keyStroke != null)
                putValue(ACCELERATOR_KEY, keyStroke);
        }
        private  MapAction(String name)
        {
            this(name, null, null);
        }
        private MapAction(String name, KeyStroke keyStroke)
        {
            this(name, null, keyStroke);
        }
        public String getActionName()
        {
            return (String) getValue(Action.NAME);
        }
        public void actionPerformed(ActionEvent ae)
        {
            java.util.List selectionList = getActiveViewer().getSelectionList();
            //todo: if no active viewer, try a static MapViewer in case it's running alone
            System.out.println(ae.getActionCommand() + " " + selectionList);
            if (selectionList.size() == 0) {
                System.err.println("MapAction: nothing in selection!");
                return;
            }
            if (enabled(selectionList)) {
                act(selectionList);
                getActiveViewer().repaint();
            } else
                System.out.println("Not allowed now");
        }
        boolean enabled(java.util.List l) { return true; }
        
        void act(java.util.List selectionList)
        {
            act(selectionList.iterator());
        }
        // automatically apply the action serially to everything in the
        // selection -- override if this or isn't what the action
        // wants to do.
        void act(java.util.Iterator i)
        {
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                act(c);
            }
        }
        void act(LWComponent c)
        {
            System.out.println("unhandled MapAction: " + getActionName() + " on " + c);
        }
        void Xact(LWComponent c) {}// for commenting convenience
    }
    
    private static void  setMenuToolbars(JFrame frame, Action[] windowActions)
    {
        final int metaMask = VueUtil.isMacPlatform() ? Event.META_MASK : Event.CTRL_MASK;
        
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu viewMenu = new JMenu("View");
        JMenu formatMenu = new JMenu("Format");
        JMenu arrangeMenu = new JMenu("Arrange");
        JMenu alignMenu = new JMenu("Align");
        JMenu windowMenu = new JMenu("Window");
        JMenu optionsMenu = new JMenu("Options");
        JMenu helpMenu = new JMenu("Help");
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(formatMenu);
        menuBar.add(arrangeMenu);
        menuBar.add(windowMenu);
        menuBar.add(optionsMenu);
        menuBar.add(helpMenu);
        //adding actions
        SaveAction saveAction = new SaveAction("Save", false);
        SaveAction saveAsAction = new SaveAction("Save As...");
        OpenAction openAction = new OpenAction("Open");
        ExitAction exitAction = new ExitAction("Quit");
        fileMenu.add(new JMenuItem("New"));
        fileMenu.add(openAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, metaMask));
        fileMenu.add(saveAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, metaMask));
        fileMenu.add(saveAsAction);
        //fileMenu.add(htmlAction);
        fileMenu.add(new JMenuItem("Export ..."));
        fileMenu.addSeparator();
        fileMenu.add(exitAction);
        
        editMenu.add(new JMenuItem("Undo"));
        editMenu.add(new JMenuItem("Redo"));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem("Cut"));
        editMenu.add(new JMenuItem("Copy"));
        editMenu.add(new JMenuItem("Paste"));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem("Select All"));
        
        viewMenu.add(new JMenuItem("Zoom In"));
        viewMenu.add(new JMenuItem("Zoom Out"));
        viewMenu.add(new JMenuItem("Zoom Fit"));
        viewMenu.add(new JMenuItem("Zoom 100%"));
        viewMenu.addSeparator();
        viewMenu.add(new JMenuItem("Resources"));
        viewMenu.add(new JMenuItem("Collection"));
        viewMenu.add(new JMenuItem("Inspector"));
        viewMenu.add(new JMenuItem("Pathway"));
        viewMenu.add(new JMenuItem("Toolbar"));
        viewMenu.add(new JMenuItem("Overview"));
        
        JMenu fontMenu = new JMenu("Font");

        /*
        // this list bigger than screen & menu isn't scrolling for us!
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        System.out.println(java.util.Arrays.asList(fonts));
        for (int i = 0; i < fonts.length; i++) {
            JMenuItem fm = new JMenuItem(fonts[i]);
            fontMenu.add(fm);
        }
        */
                           
        formatMenu.add(fontMenu);
        formatMenu.add(new JMenuItem("Size"));
        formatMenu.add(new JMenuItem("Style"));
        formatMenu.add(new JMenuItem("Justify"));

        alignMenu.add(new JMenuItem("Row"));
        alignMenu.add(new JMenuItem("Column"));
        
        arrangeMenu.add(Actions.BringToFront);
        arrangeMenu.add(Actions.SendToBack);
        arrangeMenu.add(Actions.BringForward);
        arrangeMenu.add(Actions.SendBackward);
        arrangeMenu.addSeparator();
        arrangeMenu.add(Actions.Group);
        arrangeMenu.add(Actions.Ungroup);
        arrangeMenu.addSeparator();
        arrangeMenu.add(alignMenu);
        
        for (int i = 0; i < windowActions.length; i++) {
            System.out.println("adding " + windowActions[i]);
            windowMenu.add(new JCheckBoxMenuItem(windowActions[i]));
        }

        optionsMenu.add(new JMenuItem("Node Types..."));
        optionsMenu.add(new JMenuItem("Map Preference..."));
        optionsMenu.add(new JMenuItem("Preferences..."));
        
        helpMenu.add(new JMenuItem("Help"));
        

        //extra additions by the power team members
        //PDFConversion pdfAction = new PDFConversion("PDF");
        PDFTransform pdfAction = new PDFTransform("PDF");
        HTMLConversion htmlAction = new HTMLConversion("HTML");
        XMLView xmlAction = new XMLView("XML");
        ImageConversion imageAction = new ImageConversion("Jpeg");
        ImageMap imageMap = new ImageMap("IMAP");
        //SVGConversion svgAction = new SVGConversion("SVG");
        PrintAction printAction = new PrintAction("Print");
        
        JToolBar toolBar = new JToolBar();
        toolBar.add(openAction);
        toolBar.add(saveAction);
        toolBar.add(saveAsAction);
        toolBar.add(imageAction);
        toolBar.add(htmlAction);
        toolBar.add(xmlAction);
        toolBar.add(pdfAction);
        toolBar.add(imageMap);
        //toolBar.add(svgAction);
        toolBar.add(printAction);
        //toolBar.add(new JButton(new ImageIcon("tufts/vue/images/ZoomOut16.gif")));
        frame.setJMenuBar(menuBar);
        frame.getContentPane().add(toolBar,BorderLayout.NORTH);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}});
    }






    static void installExampleNodes(Vue2DMap map)
    {
        map.addLWC(new LWNode("Oval", 0)).setFillColor(Color.red);
        map.addLWC(new LWNode("Circle", 1)).setFillColor(Color.green);
        map.addLWC(new LWNode("Square", 2)).setFillColor(Color.orange);
        map.addLWC(new LWNode("Rectangle", 3)).setFillColor(Color.blue);
        map.addLWC(new LWNode("Rounded Rectangle", 4)).setFillColor(Color.yellow);
        
        map.addNode(new LWNode("One"));
        map.addNode(new LWNode("Two"));
        map.addNode(new LWNode("Three"));
        map.addNode(new LWNode("Four"));
    }
    
    static void installExampleMap(Vue2DMap map)
    {
        /*
         * create some test nodes & links
         */
        LWNode n1 = new LWNode("Google", new Resource("http://www.google.com/"));
        LWNode n2 = new LWNode("Program Files", new Resource("C:\\Program Files"));
        LWNode n3 = new LWNode("readme.txt", new Resource("readme.txt"));
        LWNode n4 = new LWNode("Slash", new Resource("file:///"));
        n1.setLocation(100, 30);
        n2.setLocation(100, 100);
        n3.setLocation(50, 180);
        n4.setLocation(200, 180);
        map.addNode(n1);
        map.addNode(n2);
        map.addNode(n3);
        map.addNode(n4);
        map.addLink(new LWLink(n1, n2));
        map.addLink(new LWLink(n2, n3));
        map.addLink(new LWLink(n2, n4));
    }


}
