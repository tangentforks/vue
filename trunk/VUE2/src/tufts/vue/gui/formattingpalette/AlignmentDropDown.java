package tufts.vue.gui.formattingpalette;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import tufts.vue.VueResources;

public class AlignmentDropDown extends JPanel {
    ImageIcon[] images;
    

    private JComboBox alignList;
    
    public AlignmentDropDown() {
        super(new BorderLayout());

        //Load the pet images and create an array of indexes.
        images = new ImageIcon[3];
        Integer[] intArray = new Integer[3];
        
        intArray[0] = new Integer(0);
        intArray[1] = new Integer(1);
        intArray[2] = new Integer(2);
            
        images[0] = (ImageIcon) VueResources.getIcon("list.button.leftalignment.raw");
        images[1] = (ImageIcon) VueResources.getIcon("list.button.centeralignment.raw");
        images[2] = (ImageIcon) VueResources.getIcon("list.button.rightalignment.raw");                                           

        //Create the combo box.
        alignList = new JComboBox(intArray);
        ComboBoxRenderer renderer= new ComboBoxRenderer();
        renderer.setPreferredSize(new Dimension(15, 15));
        alignList.setRenderer(renderer);
        alignList.setMaximumRowCount(3);
        alignList.setUI(new ButtonlessComboBoxUI());
        alignList.setOpaque(false);
        alignList.setBorder(BorderFactory.createEmptyBorder());
        alignList.setBackground(this.getBackground());
        //Lay out the demo.
        setOpaque(false);
        add(alignList, BorderLayout.PAGE_START);
        this.setPreferredSize(new Dimension(alignList.getWidth()-10,alignList.getHeight()));
        
    }

    public JComboBox getComboBox()
    {
    	return alignList;
    }
    
    class ComboBoxRenderer extends JLabel
                           implements ListCellRenderer {
        private Font uhOhFont;

        public ComboBoxRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
            
        }

        /*
         * This method finds the image and text corresponding
         * to the selected value and returns the label, set up
         * to display the text and image.
         */
        public Component getListCellRendererComponent(
                                           JList list,
                                           Object value,
                                           int index,
                                           boolean isSelected,
                                           boolean cellHasFocus) {
            //Get the selected index. (The index param isn't
            //always valid, so just use the value.)
            int selectedIndex = ((Integer)value).intValue();

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            //Set the icon and text.  If icon was null, say so.
            ImageIcon icon = images[selectedIndex];
            setIcon(icon);
  
            return this;
        }
    }
//  default offsets for drawing popup arrow via code
    public int mArrowSize = 3;
    public int mArrowHOffset  = -13;
    public int mArrowVOffset = -7;
    public void paint(Graphics g)
    {
    	super.paint(g);
    	
    	        // draw popup arrow
                Color saveColor = g.getColor();
                g.setColor( Color.black);
    			
                int w = getWidth();
                int h = getHeight();
    			
                int x1 = w + mArrowHOffset;
                int y = h + mArrowVOffset;
                int x2 = x1 + (mArrowSize * 2) -1;
    			
                //((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                //RenderingHints.VALUE_ANTIALIAS_ON);
                
                for(int i=0; i< mArrowSize; i++) { 
                    g.drawLine(x1,y,x2,y);
                    x1++;
                    x2--;
                    y++;
                }
                g.setColor( saveColor);
          }
    
}


           
       