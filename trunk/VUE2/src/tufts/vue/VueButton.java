package tufts.vue;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.*;

/**
 *
 * This class is a wrapper around JButton to get the look and feel for VUE.
 * VueButtons are currently used in Pathway Panel and Advanced Search.  The button sets the disabled, Up and Down icons.
 * All the icons must be present in VueResources in format buttonName.Up, buttonName.down, buttonName.disabled, or .raw
 * for generated buttons.
 *
 * @author  akumar03
 * @author  Scott Fraize
 * @version March 2004
 */

public class VueButton extends JButton
{
    private static final String UP = ".up";
    private static final String DOWN = ".down";
    private static final String DISABLED = ".disabled";
    private static final String RAW = ".raw";

    protected String key;

    public VueButton(String name, ActionListener l)
    {
        init(name);
        if (l != null)
            addActionListener(l);
    }
    
    public VueButton(String name) {
        init(name);
    }

    public VueButton(Action a) {
        setAction(a);
        init((String) a.getValue(Action.ACTION_COMMAND_KEY));
    }


    private void init(String key)
    {
        this.key = key;
        if (DEBUG.Enabled) System.out.println("initializing " + this);
        
        Icon i;
        
        if ((i = VueResources.getImageIcon(key + RAW)) != null) {
            VueButtonIcon.installGenerated(this, i);
        } else {
            if ((i = VueResources.getImageIcon(key + UP)) != null)       setIcon(i);
            if ((i = VueResources.getImageIcon(key + DOWN)) != null)     setPressedIcon(i);
            if ((i = VueResources.getImageIcon(key + DISABLED)) != null) setDisabledIcon(i);
        }

        if (true) {
            setBorder(null);
            setBorderPainted(false);
            setFocusable(false);
            setOpaque(false);
        }

        if (getIcon() != null) {
            Dimension imageSize = new Dimension(getIcon().getIconWidth(), getIcon().getIconHeight());
            System.out.println(this + " icon size is " + VueUtil.out(imageSize) + " on " + key);
            setPreferredSize(imageSize);
        }

        //setBackground(Color.white);
        //setBackground(Color.red);
        if (DEBUG.SELECTION&&DEBUG.META) new Throwable().printStackTrace();
        
    }

    public String toString() {
        return "VueButton[" + key + "]";
    }

    public static class Toggle extends VueButton {
        public Toggle(String name, ActionListener l) {
            super(name, l);
            // apparently need more than this to get to work as a toggle
            setModel(new JToggleButton.ToggleButtonModel());            
        }
        public String toString() {
            return "VueButton.Toggle[" + key + "]";
        }
    }
    
   
}
