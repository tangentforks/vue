package tufts.vue.gui;

import tufts.Util;
import tufts.vue.VUE;
import tufts.vue.LWMap;
import tufts.vue.DEBUG;
import tufts.vue.MapViewer;
import tufts.vue.VueTool;
import tufts.vue.VueToolbarController;

import tufts.macosx.MacOSX;

import java.awt.*;
import javax.swing.JFrame;
import javax.swing.JWindow;
import org.apache.log4j.NDC;

/**
 * Code for providing, entering and exiting VUE full screen modes.
 *
 * @version $Revision: 1.19 $ / $Date: 2007-11-16 22:16:46 $ / $Author: sfraize $
 *
 */

// This code is pretty messy as it's full of old commented out experimental workarounds
// for java limitations relating to window parentage, visibility & z-order.  The current
// code seems to be the only thing that actually works, but lots of old code is left in
// all over the place in case we have problems with this again.  I've submitted an RFC
// for Java with Sun Micro on this, which was accepted, but the desired clean
// functionality won't be in there till Java 6 at the earliest, maybe not till Java 7.
// -- SMF 2007-05-22

public class FullScreen
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(FullScreen.class);
    
    private static boolean fullScreenWorking = false;
    private static boolean fullScreenNative = false; // using native full-screen mode, that hides even mac menu bar?
    private static boolean nativeModeHidAllDockWindows;
    private static FSWindow FullScreenWindow;
    private static MapViewer FullScreenViewer;
    private static MapViewer FullScreenLastActiveViewer;

    private static final String FULLSCREEN_NAME = "*FULLSCREEN*";

    public static final String VIEWER_NAME = "FULL";

    //private static final boolean ExtraDockWindowHiding = !Util.isMacPlatform(); // must be done on WinXP
    private static final boolean ExtraDockWindowHiding = true;
    

    /**
     * The special full-screen window for VUE -- overrides setVisible for special handling
     */
    public static class FSWindow extends javax.swing.JWindow
    {
        private VueMenuBar mainMenuBar;
        private boolean isHidden = false;
        private boolean screenBlacked = false;
        
        FSWindow() {
            super(VUE.getApplicationFrame());
            if (GUI.isMacBrushedMetal())
                setName(GUI.OVERRIDE_REDIRECT); // so no background flashing of the texture
            else
                setName(FULLSCREEN_NAME);

            GUI.setRootPaneNames(this, FULLSCREEN_NAME);
            GUI.setOffScreen(this);
            
            if (Util.isMacPlatform()) {
                // On the Mac, this must be shown before any DockWindows display,
                // or they won't stay on top of their parents (this is a mac bug;
                // as of 2006 anyway -- don't know if we still need this,
                // but best to leave it in -- SMF 2007-05-22).
                setVisible(true);
                setVisible(false);
            }
            setBackground(Color.black);
            VUE.addActiveListener(tufts.vue.LWMap.class, FSWindow.this);
        }

        public void activeChanged(tufts.vue.ActiveEvent e, tufts.vue.LWMap map) {
            if (fullScreenWorking)
                FullScreenViewer.loadFocal(map);
        }

        @Override
        public void paint(Graphics g) {

            boolean offscreen = getX() < 0 || getY() < 0;
            
            if (DEBUG.Enabled) Log.debug("paint " + this + ";  hidden=" + isHidden + "; offscreen=" + offscreen);

            // don't paint if we're offscreen.  This is important for the presentation tool,
            // as it actually configures some of it's special button locations when it
            // paints, so only one viewer at a time should be painting, otherwise the
            // buttons could get the wrong location for the visible viewer.
            if (offscreen)
                return;
            
            super.paint(g);

            if (isHidden) {
                // wait for paint to finish, then fade us up
                fadeUp();
            }

            if (screenBlacked) {
                fadeFromBlack();
                screenBlacked = false;
            }
        }

        void screenToBlack() {
            screenBlacked = true;
            goBlack();
        }


        private void fadeUp()
        {
            isHidden = false;
            if (Util.isMacPlatform()) {
                if (DEBUG.Enabled) Log.debug("requesting fadeup");
                GUI.invokeAfterAWT(new Runnable() { public void run() {
                    doFadeUp();
                }});
            }
        }
        
        private void doFadeUp()
        {
            if (DEBUG.Enabled) Log.debug("fadeup invoked");
            if (Util.isMacPlatform()) {
                try {
                    //if (MacOSX.isMainInvisible())
                    MacOSX.fadeUpMainWindow();
                } catch (Throwable t) {
                    Log.error(t);
                }
            }
            isHidden = false;
        }

        void makeInvisible() {
            if (!isHidden && Util.isMacPlatform()) {
                isHidden = true;
                MacOSX.setWindowAlpha(this, 0);
            }
        }

//         void makeVisible() {
//             if (isHidden) {
//                 isHidden = false;
//                 //MacOSX.cycleAlpha(this, 0f, 1f);
//                 MacOSX.setAlpha(this, 1f);
//             }
//         }

//         void makeVisibleLater() {
//             if (isHidden) {
//                 GUI.invokeAfterAWT(new Runnable() { public void run() {
//                     makeVisible();
//                 }});
//             }
//         }
        
        

        private javax.swing.JMenuBar getMainMenuBar() {
            if (mainMenuBar == null) {
                // fyi, if this is ever called before all the DockWindow's have
                // been constructed, the Windows menu in the VueMenuBar
                // will be awfully sparse...
                mainMenuBar = new VueMenuBar();
            }
            return mainMenuBar;
        }

        /**
           
         * Allow a menu bar at the top for full-screen working mode on non-mac
         * platforms.  (Mac always has a menu bar at the top in any working mode, as
         * opposed to full-screen native mode).  For Windows full-screen "native" mode,
         * we need to make sure this is NOT enabled, or it will show up at the top of
         * the full-screen window during a presentation.
         
         */
        
        private void setMenuBarEnabled(boolean enabled) {
            if (GUI.isMacAqua()) {
                // we never need to do this in Mac Aqua, as the application
                // always has the Mac menu bar at the top of the screen
                return;
            }
            
            if (enabled)
                getRootPane().setJMenuBar(getMainMenuBar());
            else
                getRootPane().setJMenuBar(null);
        }
        
        /**
           
         * When set to hide, this instead sets us off-screen and forces us to be
         * non-focusable (we don't want an "invisible" window accidentally having the
         * focus).
         
         * We need to do this because all the DockWindows are children of this window,
         * (so they always stay on top of it in full-screen working mode, which is
         * currently the only way to ensure this), and as children, if we actually hid
         * this window, they'd all hide with it.

         * And when shown, we prophylacticly raise all the DockWindows, in case one of
         * the various window hierarchy display bugs in the various java platform
         * implementations left them behind something.
         
         */
        
        @Override
        public void setVisible(boolean show)
        {
            setFocusableWindowState(show);
            
            // if set we allow it to actually go invisible
            // all children will hide (hiding all the DockWindow's)
            
            if (show)
                super.setVisible(true);
            else 
                GUI.setOffScreen(this);

            if (show && !inNativeFullScreen()) 
                DockWindow.raiseAll(); // just in case
        }
        
    }

    private static void goBlack() {
        if (Util.isMacPlatform()) {
            try {
                MacOSX.goBlack();
            } catch (Error e) {
                Log.error(e);
            }
        }
    }

    private static void goClear() {
        if (Util.isMacPlatform()) {
            try {
                MacOSX.hideFSW();
            } catch (Error e) {
                Log.error(e);
            }
        }
    }
    

    private static void fadeFromBlack() {
        if (Util.isMacPlatform()) {
            try {
                MacOSX.fadeFromBlack();
            } catch (Error e) {
                Log.error(e);
            }
        }
    }
    
//     public static void fadeToBlack() {
//         // None of the MacOSX utils that use a separate NSWindow drawn
//         // black on top of everything else work in full-screen native
//         // mode -- the full screen window itself always stays on-top,
//         // even if we order the special NSWindow to the front.
        
// //         if (Util.isMacPlatform()) {
// //             try {
// //                 MacOSX.fadeToBlack();
// //             } catch (Error e) {
// //                 Log.error(e);
// //             }
// //         }
//     }

    

    
    
    public static boolean inFullScreen() {
        return fullScreenWorking || fullScreenNative;
    }

    public static boolean inWorkingFullScreen() {
        return fullScreenWorking;
    }
    public static boolean inNativeFullScreen() {
        return fullScreenNative;
        //return fullScreenNative || (DEBUG.PRESENT && inWorkingFullScreen());
    }

    public static void toggleFullScreen() {
        toggleFullScreen(false);
    }

//     public static void setFullScreenWorkingMode() {
//         dropFromNativeToWorking();
//     }
    
    public static void dropFromNativeToWorking() {

        // TODO: merge this code with exitFullScreen

        FullScreenWindow.screenToBlack(); // will auto-fade-up next time it completes a paint

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        
        if (device.getFullScreenWindow() != null) {
            // this will take us out of true full screen mode
            Log.debug("clearing native full screen window:"
                          + "\n\t  controlling device: " + device
                          + "\n\tcur device FS window: " + device.getFullScreenWindow());
            device.setFullScreenWindow(null);
        }
        FullScreenWindow.setMenuBarEnabled(true);
        VUE.getActiveTool().handleFullScreen(true, false);
        GUI.setFullScreenVisible(FullScreenWindow);
        fullScreenWorking = true;
        fullScreenNative = false;

        //FullScreenViewer.grabVueApplicationFocus("FullScreen.nativeDrop", null);

//         GUI.invokeAfterAWT(new Runnable() { public void run() {
//             VUE.getActiveTool().handleFullScreen(true, false);
//         }});
        
        
    }
    
    public static synchronized void toggleFullScreen(boolean goNative)
    {
        //agoNative=false;
        // TODO: getMapAt in MapTabbedPane fails returning null when, of course, MapViewer is parented out!
                
        // On the mac, the order in which the tool windows are shown (go from hidden to visible) is the
        // z-order, with the last being on top -- this INCLUDES the full-screen window, so when it get's
        // shown, all the tool windows will always go below it, (including the main VUE frame) so we have
        // have to hide/show all the tool windows each time so they come back to the front.

        // If the tool window was open when fs popped, you can get it back by hitting it's shortcuut
        // twice, hiding it then bringing it back, tho it appeared on mac that this didn't always work.

        // More Mac Problems: We need the FSW (full screen window) to be a frame so we can set a
        // jmenu-bar for the top (MRJAdapter non-active jmenu bar won't help: it's for only for when
        // there's NO window active).  But then as a sibling frame, to VUE.frame instead ofa child to it,
        // VUE.frame can appear on top of you Option-~.  Trying to move VUE.frame off screen doesn't
        // appear to be working -- maybe we could set it to zero size?  Furthermore, all the tool
        // Windows, which are children to VUE.frame, won't stay on top of the FSW after it takes focus.
        // We need to see what happens if they're frames, as they're going to need to be anyway.  (There
        // is the nice ability to Option-~ them all front/back at once, as children of the VUE.frame, if
        // they're windows tho...)

        // What about using a JDialog instead of a JFrame?  JDialog's can have
        // a parent frame AND a JMenuBar...

//         final boolean doBlack = (goNative || inNativeFullScreen());
//         if (doBlack)
//             goBlack();

        if (fullScreenWorking) {
            if (goNative && !inNativeFullScreen())
                enterFullScreenMode(true);
            else           
                exitFullScreenMode();
        } else {
            enterFullScreenMode(goNative);
        }

        if (!inWorkingFullScreen())
        {
            tufts.vue.VueToolbarController.getController()
                .setSelectedTool(tufts.vue.VueToolbarController.getController().getSelectedTool());

        }
        else if (!goNative)
        {
            tufts.vue.VueToolbarController.getController()
                .setSelectedTool(VUE.getFloatingZoomPanel().getSelectedTool());
            // Is causing event loop:
            //VUE.setActive(VueTool.class, FullScreen.class, VUE.getFloatingZoomPanel().getSelectedTool());
            
        	
        }
        
        //VUE.getActiveViewer().requestFocus();
        ////ToolWindow.adjustMacWindows();
        VueMenuBar.toggleFullScreenTools();

//         if (doBlack)
//             VUE.invokeAfterAWT(new Runnable() {
//                     public void run() { tufts.Util.screenFadeFromBlack();}
//                 });
    }

    private synchronized static void enterFullScreenMode(final boolean goNative)
    {
        NDC.push("[FS->]");

        boolean wentBlack = false;
        if (goNative && inWorkingFullScreen()) {
            // as the working full-screen window is the same window as the native
            // full-screen window, we need to show the black window when transitioning
            // from working full-screen to native full-screen, otherwise we see the
            // working full-screen tear-down and the underlying VUE app/desktop before
            // seeing the black screen of the initial native window at 0 alpha.
            // Okay to leave this up while in native, as it'll be torn down automatically
            // on exit, tho it safer to tear it down just in case.
            goBlack();
            wentBlack = true;
        }

        final LWMap activeMap = VUE.getActiveMap();

        FullScreenLastActiveViewer = VUE.getActiveViewer();
        FullScreenLastActiveViewer.setFocusable(false);
        
        if (goNative) {
            // Can't use heavy weights, as they're windows that can't be seen,
            // and actually the screen goes blank on Mac OS X trying to handle this.
            javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(true);
            // also: appear to need to do this before anything else
            // to have it property take effect?
        }

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        final VueTool activeTool = VueToolbarController.getActiveTool();
        
        //out("Native full screen support available: " + device.isFullScreenSupported());
        // todo: crap: if the screen resolution changes, we'll need to resize the full-screen window


        Log.debug("Entering full screen mode; goNative=" + goNative);
        if (FullScreenWindow == null) {
            FullScreenWindow = (FSWindow) GUI.getFullScreenWindow();
            FullScreenWindow.getContentPane().add(FullScreenViewer = new MapViewer(null, VIEWER_NAME));
            //fullScreenWindow.pack();
        }
                
        FullScreenWindow.setMenuBarEnabled(!goNative);
        // FullScreenViewer.loadFocal(VUE.getActiveMap()); // can't do till we're sure it has a size!
        fullScreenWorking = true; // we're in the mode as soon as the add completes (no going back then)
        fullScreenNative = goNative;

        if (!Util.isWindowsPlatform() && goNative) {

            // Try and prevent us from flashing a big white screen while we load.
            // Tho immediately loading the focal below will quickly override this...
            FullScreenWindow.setBackground(Color.black);
            FullScreenViewer.setBackground(Color.black);
            
            // On Mac, must use native full-screen to get the window over
            // the mac menu bar.
                    
            FullScreenWindow.makeInvisible();
            device.setFullScreenWindow(FullScreenWindow);

            // We run into a serious problem using the special java full-screen mode on the mac: if
            // you right-click, it attemps to pop-up a menu over the full screen window, which is not
            // allowed in mac full-screen, and it apparently auto-switches context somehow for you,
            // but just leaves you at a fully blank screen that you can sometimes never recover from
            // without powering off!  This true as of java version "1.4.2_05-141.3", Mac OS X 10.3.5/6.

            if (ExtraDockWindowHiding && !DockWindow.AllWindowsHidden()) {
                nativeModeHidAllDockWindows = true;
                DockWindow.HideAllWindows();
            }

        } else {
            if (goNative) {
                if (ExtraDockWindowHiding && !DockWindow.AllWindowsHidden()) {
                    nativeModeHidAllDockWindows = true;
                    DockWindow.HideAllWindows();
                }
            }
            GUI.setFullScreenVisible(FullScreenWindow);
        }
                
        FullScreenViewer.loadFocal(FullScreenLastActiveViewer.getFocal());
        FullScreenViewer.grabVueApplicationFocus("FullScreen.enter-1", null);
        
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            if (DEBUG.PRESENT) Log.debug("AWT thread activeTool.handleFullScreen for " + activeTool);
            activeTool.handleFullScreen(true, goNative);
        }});

        if (wentBlack) {
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                // shouldn't ever be able to see this happen, as the native full-screen should be
                // on top of us, but just in case we make sure to clear it out behind us...
                goClear();
            }});
        }

        GUI.invokeAfterAWT(new Runnable() { public void run() {

            // The initial request for focus sometimes happens while the
            // FullScreenViewer already has focus, and immediately after that it
            // completely loses focus (to null), so we request again here to be
            // absolutely sure we have keyboard focus.

            FullScreenViewer.grabVueApplicationFocus("FullScreen.enter-2", null);
            NDC.pop();
        }});
        
    }

    
    
    private synchronized static void exitFullScreenMode()
    {
        NDC.push("[<-FS]");
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        final boolean wasNative = inNativeFullScreen();

        Log.debug("Exiting full screen mode; inNative=" + wasNative);

        final boolean fadeBack = wasNative && Util.isMacPlatform();

        if (fadeBack) {
            // For Mac: this won't be visible till full-screen is torn down (nothing
            // can go over the FSW), but it will be there all full and black once
            // the java FSW *is* torn down, then we can fade that out to reveal
            // the desktop.
            goBlack();
        }

        //javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(true);
        
        if (device.getFullScreenWindow() != null) {
            // this will take us out of true full screen mode
            Log.debug("clearing native full screen window:"
                          + "\n\t  controlling device: " + device
                          + "\n\tcur device FS window: " + device.getFullScreenWindow());
            device.setFullScreenWindow(null);
            // note that when coming out of full screen, the java impl
            // first restores the given  window to it's state before
            // we made it the FSW, which is annoying on the mac cause
            // it flashes a small window briefly in the upper left.
        }
        FullScreenWindow.setVisible(false);
        FullScreenViewer.loadFocal(null);
        fullScreenWorking = false;
        fullScreenNative = false;

        if (ExtraDockWindowHiding && nativeModeHidAllDockWindows) {
            nativeModeHidAllDockWindows = false;
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                DockWindow.ShowPreviouslyHiddenWindows();
                if (Util.isMacPlatform()) {
                    // Only need to do this on mac, as we can't
                    // hide what's happening on WinXP by fading
                    // to/from black anyway.
                    DockWindow.ImmediatelyRepaintAllWindows();
                }
            }});
        }
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            FullScreenLastActiveViewer.grabVueApplicationFocus("FullScreen.exit", null);
        }});
        
        GUI.invokeAfterAWT(new Runnable() { public void run() {
            VueToolbarController.getActiveTool().handleFullScreen(false, wasNative);
            //Log.debug("activeTool.handleFullScreen " + VueToolbarController.getActiveTool());
            //VUE.getActiveViewer().popToMapFocal(); // old active viewer shouldn't have changed...
        }});


        if (fadeBack) {

            // note: if the DockWindows were hidden, and requested to be shown above,
            // this fadeFromBlack still gets into the AWT EDT queue before those paints
            // complete (paints have special low priority?) -- even if we try cascading
            // the queue requests as much as four times!
            
            if (DEBUG.Enabled) Log.debug("requesting fadeFromBlack");
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                fadeFromBlack();
            }});
        }


        GUI.invokeAfterAWT(new Runnable() { public void run() {
            NDC.pop();
        }});
        
    }





    /*
    private static void exitFullScreenMode()
    {
        NDC.push("[<-FS]");
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        final boolean wasNative = inNativeFullScreen();
        
        Log.debug("Exiting full screen mode, inNative=" + wasNative);

        //javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        //javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(true);
        
        if (device.getFullScreenWindow() != null) {
            // this will take us out of true full screen mode
            Log.debug("clearing native full screen window:"
                          + "\n\t  controlling device: " + device
                          + "\n\tcur device FS window: " + device.getFullScreenWindow());
            device.setFullScreenWindow(null);
            // note that when coming out of full screen, the java impl
            // first restores the given  window to it's state before
            // we made it the FSW, which is annoying on the mac cause
            // it flashes a small window briefly in the upper left.
        }
        fullScreenWindow.setVisible(false);
        fullScreenWorking = false;
        fullScreenNative = false;
        if (fullScreenWindow != VUE.getMainWindow()) {
            Log.debug("re-attaching prior extracted viewer content " + fullScreenContent);
            fullScreenOldParent.add(fullScreenContent);
            //fullScreenOldParent.add(VUE.getActiveViewer());
        }
        if (VUE.getMainWindow() != null) {
//             if (fullScreenOldVUELocation != null)
//                 VUE.getMainWindow().setLocation(fullScreenOldVUELocation); // mac window manger not allowing
//             if (fullScreenOldVUESize != null)
//                 VUE.getMainWindow().setSize(fullScreenOldVUESize); // mac window manager won't go to 0
            //VUE.getMainWindow.setExtendedState(Frame.NORMAL); // iconifies but only until an Option-TAB switch-back
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                Log.debug("showing main window " + VUE.getMainWindow());
                VUE.getMainWindow().setVisible(true);
            }});
        }

        if (nativeModeHidAllDockWindows) {
            nativeModeHidAllDockWindows = false;
            GUI.invokeAfterAWT(new Runnable() { public void run() {
                DockWindow.ShowPreviouslyHiddenWindows();
            }});
        }

        
        GUI.invokeAfterAWT(new Runnable() {
                public void run() {
                    //Log.debug("activeTool.handleFullScreen " + VueToolbarController.getActiveTool());
                    VueToolbarController.getActiveTool().handleFullScreen(false, wasNative);
                    NDC.pop();
                }});

        //NDC.pop();
        
    }
    */      


    /*
    private static void enterFullScreenMode(boolean goNative)
    {
        NDC.push("[FS->]");

        //goNative = false; // TODO: TEMP DEBUG

        if (goNative) {
            // Can't use heavy weights, as they're windows that can't be seen,
            // and actually the screen goes blank on Mac OS X trying to handle this.
            javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(true);
            // also: appear to need to do this before anything else
            // to have it property take effect?
        }

        
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        final MapViewer viewer = VUE.getActiveViewer();
        final VueTool activeTool = VueToolbarController.getActiveTool();
        
        //out("Native full screen support available: " + device.isFullScreenSupported());
        //  setFullScreenWindow(SwingUtilities.getWindowAncestor(this));
        //VUE.frame.setVisible(false); // this also hids all children of the frame, including the new fs window.
        // mac bug: if don't create window every time, subsequent full-screen
        // modes won't extend to the the bottom of the screen (probably related to mac dock being there)
        // Very odd even if not running as a native window...  The window reports proper location, but it
        // show's up placed at a negative y.

        // todo: crap: if the screen resolution changes, we'll need to resize the full-screen window


        Log.debug("enterFullScreenMode: goingNative=" + goNative);
//         if (false&&goNative) {
//             if (VueUtil.isMacPlatform() || cachedFSWnative == null) {
//                 // have to create full screen native win on mac every time or it comes
//                 // back trying to avoid the dock??
//                 if (cachedFSWnative != null)
//                     cachedFSWnative.setTitle("_old-mac-full-native"); // '_' important for macosx hacks
//                 cachedFSWnative = GUI.createFrame("VUE-FULL-NATIVE");
//                 cachedFSWnative.setUndecorated(true);
//                 cachedFSWnative.setLocation(0,0);
//             }
//             fullScreenWindow = cachedFSWnative;
//         } else {
        if (cachedFSW == null) {
            cachedFSW = GUI.getFullScreenWindow();
            //cachedFSW = GUI.createFrame("VUE-FULL-WORKING");
            //cachedFSW.setUndecorated(true);
        }
        fullScreenWindow = cachedFSW;
            //}
            
//         if (false) {
//             fullScreenWindow = VUE.getMainWindow();
//         } else if (VueUtil.isMacPlatform() || fullScreenWindow == null) {
//             //} else if (fullScreenWindow == null) {
//             // Terribly wasteful to have to re-create this on the mac all the time..
//             if (false) {
//                 fullScreenWindow = VUE.createWindow(); // if VUE.frame is parent, it will stay on top of it
//             } else {
//                 if (fullScreenWindow != null)
//                     ((Frame)fullScreenWindow).setTitle("OLD-FULL-FRAME");
//                 fullScreenWindow = VUE.createFrame("VUE-FULL-FRAME");
//                 // but we need a Frame in order to have the menu-bar on the mac!
//                 ((Frame)fullScreenWindow).setUndecorated(true);
//             }
//             //fullScreenWindow.setName("VUE-FULL-SCREEN");
//             //fullScreenWindow.setBackground(Color.RED);
//         }

                
        if (fullScreenWindow != VUE.getMainWindow() && VUE.getMainWindow() != null) {
            //javax.swing.JComponent fullScreenContent = viewer;
            fullScreenContent = viewer;
            //fullScreenContent = new JLabel("TEST");
            fullScreenOldParent = viewer.getParent();

            Log.debug("adding content to FSW:"
                          + "\n\tCONTENT: "+ fullScreenContent
                          + "\n\t    FSW: "+ fullScreenWindow
                          );
            
            if (fullScreenWindow instanceof DockWindow) {
                ((DockWindow)fullScreenWindow).add(fullScreenContent);
            } else if (fullScreenWindow instanceof JFrame) {
                //((JFrame)fullScreenWindow).setContentPane(fullScreenContent);
                ((JFrame)fullScreenWindow).getContentPane().add(fullScreenContent);
            } else if (fullScreenWindow instanceof JWindow) {
                //((JWindow)fullScreenWindow).setContentPane(fullScreenContent);
                // adding to the contentPane instead of setting as allows
                // a JMenuBar to be added to the top and the viewer then
                // appears under it (instead of the menu overlapping it at the top)
                ((JWindow)fullScreenWindow).getContentPane().add(fullScreenContent);
            } else // is Window
                fullScreenWindow.add(fullScreenContent);

            fullScreenWindow.pack();

            //getMap().setFillColor(Color.BLACK);
            //fullScreenWindow.getContentPane().add(MapViewer.this.getParent().getParent()); // add with scroll bars
        }
                
        fullScreenWorking = true; // we're in the mode as soon as the add completes (no going back then)
        fullScreenNative = goNative;
        
//         if (VUE.getMainWindow() != null) {
//             //fullScreenOldVUELocation = VUE.getMainWindow().getLocation();
//             //fullScreenOldVUESize = VUE.getMainWindow().getSize();
//             //if (fullScreenWindow != VUE.getMainWindow())
//             //VUE.getMainWindow().setVisible(false);
//         }

        if (fullScreenWindow instanceof FSWindow) {
            ((FSWindow)fullScreenWindow).setMenuBarEnabled(!goNative);
        }

        if (goNative) {

            // On Mac, must use native full-screen to get the window over
            // the mac menu bar.
                    
            //tufts.macosx.Screen.goBlack();

            device.setFullScreenWindow(fullScreenWindow);
                
//             if (VueUtil.isMacPlatform()) {
//             try {
//             tufts.macosx.Screen.makeMainInvisible();
//             } catch (Exception e) {
//             System.err.println(e);
//             }
//             }

            //if (DEBUG.Enabled) out("fsw=" + fullScreenWindow.getPeer().getClass());
                    
            //fullScreenWindow.addKeyListener(inputHandler);
            //w.enableInputMethods(true);
            //enableInputMethods(true);
                            
            // We run into a serious problem using the special java full-screen mode on the mac: if
            // you right-click, it attemps to pop-up a menu over the full screen window, which is not
            // allowed in mac full-screen, and it apparently auto-switches context somehow for you,
            // but just leaves you at a fully blank screen that you can sometimes never recover from
            // without powering off!  This true as of java version "1.4.2_05-141.3", Mac OS X 10.3.5/6.

            if (!DockWindow.AllWindowsHidden()) {
                nativeModeHidAllDockWindows = true;
                DockWindow.HideAllWindows();
            }
            
                            
        } else {
            GUI.setFullScreenVisible(fullScreenWindow);
        }
                
        activeTool.handleFullScreen(true, goNative);
                    
//         if (false && fullScreenWindow != VUE.getMainWindow() && VUE.getMainWindow() != null) {
//             VUE.getMainWindow().setVisible(false);

//             //VUE.getMainWindow().setSize(0,0);
//             //tufts.Util.setOffScreen(VUE.getMainWindow());
                
//             //VUE.getMainWindow().setLocation(0,0);
//             //VUE.getMainWindow().setLocation(3072,2048);
//             //VUE.getMainWindow().setExtendedState(Frame.ICONIFIED);
//         }

        NDC.pop();
    }
*/      

    private static void out(String s) {
        System.out.println("VUE FullScreen: " + s);
    }
    
}