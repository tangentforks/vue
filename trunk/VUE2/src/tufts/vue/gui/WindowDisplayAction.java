package tufts.vue.gui;

import tufts.Util;
import tufts.vue.DEBUG;

import java.awt.Window;

import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;

import javax.swing.Action;
import javax.swing.AbstractButton;

/**
 * An action for displaying a Window and tracking it's displayed state,
 * keeping in synchronized with a somebody's button (such a checkbox in a menu).
 *
 * @version $Revision: 1.1 $ / $Date: 2006-01-20 17:05:33 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class WindowDisplayAction extends javax.swing.AbstractAction
{
    private AbstractButton mLinkedButton;
    private Window mWindow;
    private String mTitle;
    private boolean firstDisplay = true;
    private static final boolean showActionLabel = false;
        
    public WindowDisplayAction(Object o) {
        /*
          if (o instanceof ToolWindow) {
          ToolWindow tw = (ToolWindow) o;
          init(tw.getTitle(), tw.getWindow());
          } else {
        */
        Window w = (Window) o;
        init(extractTitle(w), w);
        //}
                
    }
        
    /*
      public WindowDisplayAction(Window w) {
      super("window: " + w.getName());
      }
        
      public WindowDisplayAction(ToolWindow tw) {
      super("window: " + tw.getWindow().getName());
      init(tw.getTitle(), tw.getWindow());
      }
    */

    private void init(String title, Window w) {
        mTitle = title;
        putValue(Action.NAME, "windowDisplayAction:" + w.getName());
        updateActionTitle(true);
        mWindow = w;
        mWindow.addComponentListener(new ComponentAdapter() {
                public void componentShown(ComponentEvent e) { handleShown(); }
                public void componentHidden(ComponentEvent e) { handleHidden(); }
            });
    }

    /*
      private String getTitle() {
      return mTitle;
      //return extractTitle(mWindow);
      }
    */

    private static String extractTitle(Window w) {
        if (w instanceof java.awt.Frame)
            return ((java.awt.Frame)w).getTitle();
        else if (w instanceof java.awt.Dialog)
            return ((java.awt.Dialog)w).getTitle();
        else
            return ((java.awt.Window)w).getName();
    }

    private void handleShown() {
        if (DEBUG.DOCK) out("handleShown");
        firstDisplay = false;
        setButtonState(isConsideredShown());
        updateActionTitle(false);
    }
    private void handleHidden() {
        if (DEBUG.DOCK) out("handleHidden");
        setButtonState(false);
        updateActionTitle(false);
    }
        
    private void updateActionTitle(boolean firstTime) {
        if (!firstTime && !showActionLabel)
            return;
        if (showActionLabel) {
            String action = "Show ";
            if (mLinkedButton != null && mLinkedButton.isSelected())
                action = "Hide ";
            putValue(Action.NAME, action + mTitle);
        } else {
            putValue(Action.NAME, mTitle);
        }
    }
    void setLinkedButton(AbstractButton b) {
        mLinkedButton = b;
        setButtonState(isConsideredShown());
    }
    private void setButtonState(boolean checked) {
        if (DEBUG.DOCK) out("setButtonState " + checked);
        if (mLinkedButton != null)
            mLinkedButton.setSelected(checked);
        else
            if (DEBUG.DOCK) out("setButtonState: NO LINKED BUTTON");
    }

    private boolean isConsideredShown() {

        if (mWindow instanceof DockWindow) {
            DockWindow dockWindow = (DockWindow) mWindow;

            if (dockWindow.isDocked())
                return !dockWindow.isRolledUp();
            else
                return dockWindow.isVisible();
        } else
            return mWindow.isVisible();
    }

    // TODO: if DockWindow, re-attach to last parent if it had one?
    // Only roll/unroll instead of hide/show?

    private static int placedWindow = 1;
            
    public void actionPerformed(ActionEvent e) {
        if (DEBUG.DOCK) out("actionPerformed: " + e);
            
        if (mLinkedButton == null)
            mLinkedButton = (AbstractButton) e.getSource();

        if (firstDisplay) {
            java.awt.Insets screen = GUI.getScreenInsets();
            if (DEBUG.DOCK) out("firstDisplay, screenInsets=" + screen + " win " + mWindow.getBounds());
            // assign a default location to newly displayed windows
            // todo: cascade them or somethting
            //if (mWindow.getX() == screen.left && mWindow.getY() == screen.top)
            if (mWindow.getX() < 0)
                mWindow.setLocation(screen.left,
                                    screen.top + DockWindow.getCollapsedHeight() * placedWindow++);
        }
        firstDisplay = false;

        final boolean doShowWindow = mLinkedButton.isSelected();
        final boolean doHideWindow = !doShowWindow;


        if (mWindow instanceof DockWindow) {
            DockWindow dockWindow = (DockWindow) mWindow;
            if (doHideWindow && dockWindow.isDocked() && !dockWindow.isRolledUp()) {
                // "Hiding" a docked DockWindow that is unrolled really means roll it back up
                // instead of hiding it.
                dockWindow.setRolledUp(true);
                return;
            }
        }
            
        if (doShowWindow) {
            boolean isMac = Util.isMacPlatform();
                
            // if (isMac) tufts.Util.invoke(mWindow.getPeer(), "setAlpha", new Float(0.5));

            // Why do we see the window contents flash twice on showing?
            // Old VUE does this too -- is Swing/AWT just pathetic?
            // BTW, this doesn't happen the *first* time a window shows.
            // Not doing the toFront here doesn't help.

            // Okay, now Map Inspector is doing it, but not Object Inspector.
            // Pre-validating may help, but may slow it down?  I think this
            // has to do with validation in some way.
                
            //mWindow.validate();
            mWindow.setVisible(true);
            mWindow.toFront();

            // could always set off screen and not move back until AWT cleared

            /*
              if (isMac)
              VUE.invokeAfterAWT(new Runnable() {
              public void run() {
              tufts.Util.invoke(mWindow.getPeer(), "setAlpha", new Float(1.0));
              }});
            */

            //VUE.ensureToolWindowVisibility(mTitle);
        } else {
            mWindow.setVisible(false);
            //VUE.ensureToolWindowVisibility(null);
        }
    }

    private void out(String s) {
        System.out.println("WindowDisplayAction[" + GUI.name(mWindow) + "] " + s);
    }


    
}


