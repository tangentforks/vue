package tufts.vue;

import java.awt.*;
import javax.swing.JFrame;
import javax.swing.JWindow;

/**
 * Code for entering and exiting VUE full screen modes.
 */
public class FullScreen {
    // Full-screen handling code
    
    private static boolean fullScreenMode = false;
    private static boolean fullScreenNative = false; // using native full-screen mode, that hides even mac menu bar?
    private static Window fullScreenWindow = null;
    private static Container fullScreenOldParent = null;
    private static Point fullScreenOldVUELocation;
    private static Dimension fullScreenOldVUESize;

    private static Frame cachedFSW = null;
    private static Frame cachedFSWnative = null;

    public static boolean inFullScreen() {
        return fullScreenMode;
    }
    public static boolean inNativeFullScreen() {
        return fullScreenNative;
    }

    public static void toggleFullScreen() {
        toggleFullScreen(false);
    }
    
    static void toggleFullScreen(final boolean goNative)
    {
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

        boolean doBlack = (goNative || inNativeFullScreen());
        if (doBlack)
            tufts.Util.screenToBlack();

        if (fullScreenMode) {
            exitFullScreenMode();
        } else {
            enterFullScreenMode(goNative);
        }

        VUE.getActiveViewer().requestFocus();
        VUE.ensureToolWindowVisibility();

        if (doBlack)
            VUE.invokeAfterAWT(new Runnable() {
                    public void run() { tufts.Util.screenFadeFromBlack();}
                });
    }

    private static void enterFullScreenMode(boolean goNative)
    {
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

        out("enterFullScreenMode: goNative=" + goNative);
        if (goNative) {
            if (VueUtil.isMacPlatform() || cachedFSWnative == null) {
                // have to create full screen native win on mac every time or it comes
                // back trying to avoid the dock??
                if (cachedFSWnative != null)
                    cachedFSWnative.setTitle("_old-mac-full-native"); // '_' important for macosx hacks
                cachedFSWnative = VUE.createFrame("VUE-FULL-NATIVE");
                cachedFSWnative.setUndecorated(true);
                cachedFSWnative.setLocation(0,0);
            }
            fullScreenWindow = cachedFSWnative;
        } else {
            if (cachedFSW == null) {
                cachedFSW = VUE.createFrame("VUE-FULL-WORKING");
                cachedFSW.setUndecorated(true);
            }
            fullScreenWindow = cachedFSW;
        }
            
        /*
          if (false) {
          fullScreenWindow = VUE.getMainWindow();
          } else if (VueUtil.isMacPlatform() || fullScreenWindow == null) {
          //} else if (fullScreenWindow == null) {
          // Terribly wasteful to have to re-create this on the mac all the time..
          if (false) {
          fullScreenWindow = VUE.createWindow(); // if VUE.frame is parent, it will stay on top of it
          } else {
          if (fullScreenWindow != null)
          ((Frame)fullScreenWindow).setTitle("OLD-FULL-FRAME");
          fullScreenWindow = VUE.createFrame("VUE-FULL-FRAME");
          // but we need a Frame in order to have the menu-bar on the mac!
          ((Frame)fullScreenWindow).setUndecorated(true);
          }
          //fullScreenWindow.setName("VUE-FULL-SCREEN");
          //fullScreenWindow.setBackground(Color.RED);
          }
        */

                
        if (fullScreenWindow != VUE.getMainWindow() && VUE.getMainWindow() != null) {
            Component fullScreenContent = viewer;
            //fullScreenContent = new JLabel("TEST");
            fullScreenOldParent = viewer.getParent();
            if (fullScreenWindow instanceof JFrame)
                ((JFrame)fullScreenWindow).getContentPane().add(fullScreenContent);
            else
                ((JWindow)fullScreenWindow).getContentPane().add(fullScreenContent);
            //getMap().setFillColor(Color.BLACK);
            //fullScreenWindow.getContentPane().add(MapViewer.this.getParent().getParent()); // add with scroll bars
        }
                
        fullScreenMode = true; // we're in the mode as soon as the add completes (no going back then)
        fullScreenNative = goNative;
        if (VUE.getMainWindow() != null) {
            //fullScreenOldVUELocation = VUE.getMainWindow().getLocation();
            //fullScreenOldVUESize = VUE.getMainWindow().getSize();
            //if (fullScreenWindow != VUE.getMainWindow())
            //VUE.getMainWindow().setVisible(false);
        }

        if (goNative) {

            // On Mac, must use native full-screen to get the window over
            // the mac menu bar.
                    
            //tufts.macosx.Screen.goBlack();
            device.setFullScreenWindow(fullScreenWindow);
            /*
                
            if (VueUtil.isMacPlatform()) {
            try {
            tufts.macosx.Screen.makeMainInvisible();
            } catch (Exception e) {
            System.err.println(e);
            }
            }
            */
            //if (DEBUG.Enabled) out("fsw=" + fullScreenWindow.getPeer().getClass());
                    
            //fullScreenWindow.addKeyListener(inputHandler);
            //w.enableInputMethods(true);
            //enableInputMethods(true);
                            
            // We run into a serious problem using the special java full-screen mode on the mac: if
            // you right-click, it attemps to pop-up a menu over the full screen window, which is not
            // allowed in mac full-screen, and it apparently auto-switches context somehow for you,
            // but just leaves you at a fully blank screen that you can sometimes never recover from
            // without powering off!  This true as of java version "1.4.2_05-141.3", Mac OS X 10.3.5/6.
                            
        } else {
            tufts.Util.setFullScreen(fullScreenWindow);
            fullScreenWindow.setVisible(true);
        }
                
        activeTool.handleFullScreen(true);
                    
        if (fullScreenWindow != VUE.getMainWindow() && VUE.getMainWindow() != null) {
            VUE.getMainWindow().setVisible(false);

            //VUE.getMainWindow().setSize(0,0);
            //tufts.Util.setOffScreen(VUE.getMainWindow());
                
            //VUE.getMainWindow().setLocation(0,0);
            //VUE.getMainWindow().setLocation(3072,2048);
            //VUE.getMainWindow().setExtendedState(Frame.ICONIFIED);
        }
    }
    
    private static void exitFullScreenMode()
    {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = ge.getDefaultScreenDevice();
        
        out("Exiting full screen mode, inNative=" + VUE.inNativeFullScreen());
        
        if (device.getFullScreenWindow() != null) {
            // this will take us out of true full screen mode
            out("clearning native full screen window " + device.getFullScreenWindow());
            device.setFullScreenWindow(null);
            // note that when coming out of full screen, the java impl
            // first restores the given  window to it's state before
            // we made it the FSW, which is annoying on the mac cause
            // it flashes a small window briefly in the upper left.
        }
        fullScreenWindow.setVisible(false);
        fullScreenMode = false;
        fullScreenNative = false;
        if (fullScreenWindow != VUE.getMainWindow()) {
            fullScreenOldParent.add(VUE.getActiveViewer());
        }
        if (VUE.getMainWindow() != null) {
            if (fullScreenOldVUELocation != null)
                VUE.getMainWindow().setLocation(fullScreenOldVUELocation); // mac window manger not allowing
            if (fullScreenOldVUESize != null)
                VUE.getMainWindow().setSize(fullScreenOldVUESize); // mac window manager won't go to 0
            //VUE.getMainWindow.setExtendedState(Frame.NORMAL); // iconifies but only until an Option-TAB switch-back
            VUE.getMainWindow().setVisible(true);
        }
        VueToolbarController.getActiveTool().handleFullScreen(false);
    }

    private static void out(String s) {
        System.out.println("VUE FullScreen: " + s);
    }
    
}