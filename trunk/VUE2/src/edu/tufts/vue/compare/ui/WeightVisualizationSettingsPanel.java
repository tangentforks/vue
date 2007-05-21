
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

/*
 * WeightVisualizationSettingsPanel.java
 *
 * Created on February 2, 2007, 3:47 PM
 *
 * @author dhelle01
 */

package edu.tufts.vue.compare.ui;

import edu.tufts.vue.style.*;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import tufts.vue.LWMap;
import tufts.vue.VueResources;

public class WeightVisualizationSettingsPanel extends JPanel implements ActionListener{
   
    public static final int DEFAULT_STYLE_CHOICES = 5;
    
    public static final String parameterChoiceMessageString = "Set parameters for:";
    //public static final String parameterChoiceMessageString = "Select a vizualization mode:";
    public static final String intervalChoiceMessageString = "Set number of intervals:";
    public static final String paletteChoiceMessageString = "Select a color Palette:";
    
    private JComboBox parameterChoice;
    private JComboBox intervalNumberChoice;
    private JComboBox paletteChoice;
    
    private IntervalListModel nodeModel;
    private IntervalListModel linkModel;
    private JTable intervalList;
    
    private LWMap map = null;
    
    private ArrayList<Style> nodeStyles = new ArrayList<Style>();
    private ArrayList<Style> linkStyles = new ArrayList<Style>();
    
    private JLabel parameterChoiceMessage;
    
    private static WeightVisualizationSettingsPanel panel = new WeightVisualizationSettingsPanel();
    
    public static WeightVisualizationSettingsPanel getSharedPanel()
    {
        return panel;
    }

    public WeightVisualizationSettingsPanel() 
    {
        // too soon for all settings, so just load default styles here
        loadDefaultStyles();
        
        //int b = mmc.TAB_BORDER_SIZE;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(5,5,5,5));
        
        parameterChoiceMessage = new JLabel(parameterChoiceMessageString,JLabel.RIGHT)
        {
            public java.awt.Dimension getPreferredSize()
            {
              //if(mmc!=null)
              //  return mmc.getVizLabelPreferredSize();    
              //else
                return new java.awt.Dimension(100,30);
            }
        };
     
        setUpGui();
    }
    
    public WeightVisualizationSettingsPanel(final tufts.vue.MergeMapsChooser mmc) 
    {
        // too soon for all settings, so just load default styles here
        loadDefaultStyles();
        
        int b = mmc.TAB_BORDER_SIZE;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(b,b,b,b));
        
        parameterChoiceMessage = new JLabel(parameterChoiceMessageString,JLabel.RIGHT)
        {
            public java.awt.Dimension getPreferredSize()
            {
              if(mmc!=null)
                return mmc.getVizLabelPreferredSize();    
              else
                return new java.awt.Dimension(100,30);
            }
        };
     
        setUpGui();
    }
        
     public void setUpGui()
     {
     
         setOpaque(false); 
         //setOpaque(true);
         //setBackground(java.awt.Color.BLUE);
         
        String[] parameterChoices = {"Nodes","Links"};
        
       tufts.vue.PolygonIcon lineIcon = new tufts.vue.PolygonIcon(new Color(153,153,153));
       /*{
           public java.awt.Dimension getPreferredSize()
           {
               return new java.awt.Dimension(getParent().getWidth(),1);
           }
       };*/
        // tufts.vue.PolygonIcon lineIcon = new tufts.vue.PolygonIcon(Color.BLACK);
        lineIcon.setIconWidth(500);
        lineIcon.setIconHeight(1);
        
        parameterChoice = new JComboBox(parameterChoices);
        JLabel intervalNumberChoiceMessage = new JLabel(intervalChoiceMessageString,JLabel.RIGHT);
        Integer[] intervalNumberChoices = {3,4,5,6,7,8,9,10};
        intervalNumberChoice = new JComboBox(intervalNumberChoices)
        {
            public java.awt.Dimension getPreferredSize()
            {
                return new java.awt.Dimension(30,30);
            }
        };
        intervalNumberChoice.setSelectedItem(5);
        JLabel paletteChoiceMessage = new JLabel(paletteChoiceMessageString,JLabel.RIGHT);
        Colors[] colors = {Colors.one,Colors.two,Colors.three,Colors.four,Colors.five,Colors.six};
        paletteChoice = new JComboBox(colors);
        paletteChoice.setRenderer(new ColorsComboBoxRenderer());
        //paletteChoice.setModel(... need way to express both style loaded and color brewer choices?
        nodeModel = new IntervalListModel();
        linkModel = new IntervalListModel();
        intervalList = new JTable();
        intervalList.setDefaultRenderer(PercentageInterval.class,new PercentageIntervalRenderer());
        intervalList.setDefaultEditor(PercentageInterval.class,new PercentageIntervalEditor());
        intervalList.setDefaultRenderer(IntervalStylePreview.class,new IntervalStylePreviewRenderer());
        intervalList.setDefaultEditor(IntervalStylePreview.class,new IntervalStylePreviewEditor());

        //$
           intervalList.setRowHeight(30);
        //$

        loadSettings();
        
        setModel();
        
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridBag);
        
        //first row
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0,0,0,5);
        gridBag.setConstraints(parameterChoiceMessage,c);
        add(parameterChoiceMessage);
        c.insets = new Insets(0,0,0,0);
        c.anchor = GridBagConstraints.WEST;
        //c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(parameterChoice,c);
        add(parameterChoice);
        //c.weightx = 1.0;
        //c.gridwidth = GridBagConstraints.REMAINDER;
        //gridBag.setConstraints(helpLabel,c);
        //add(helpLabel);
        
        
        
        JLabel lineLabel = new JLabel(lineIcon);
        c.insets = new Insets(15,0,15,0);
        gridBag.setConstraints(lineLabel,c);
        add(lineLabel);
        
        //second row
        //c.weightx = 0.0;
        //c.gridwidth = 1;
        //c.anchor = GridBagConstraints.EAST;
        //gridBag.setConstraints(intervalNumberChoiceMessage,c);
        //add(intervalNumberChoiceMessage);
        //c.gridwidth = GridBagConstraints.REMAINDER;
        //c.anchor = GridBagConstraints.WEST;
        //gridBag.setConstraints(intervalNumberChoice,c);
        //add(intervalNumberChoice);
        
        //third row
        c.weightx = 0.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0,0,0,5);
        gridBag.setConstraints(paletteChoiceMessage,c);
        add(paletteChoiceMessage);
        c.insets = new Insets(0,0,0,0);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridBag.setConstraints(paletteChoice,c);
        add(paletteChoice);
        
        //table
        //c.fill = GridBagConstraints.NONE;
        intervalList.setPreferredScrollableViewportSize(new java.awt.Dimension(200,150));
        JScrollPane scroll = new JScrollPane(intervalList);
        //c.gridx = 0;
        //c.gridy = 2;
        //c.gridwidth = 3;
        //$
          c.weightx = 1.0;
        //$
        gridBag.setConstraints(scroll,c);
        add(scroll);
        
        //set up event handling within this panel
        parameterChoice.addActionListener(this);
        paletteChoice.addActionListener(this);
        
    }
    
    //Likely will do more than just return map later, Probably will be initialized as needed in the constructor
    //and/or through a setMap() method called from the Chooser.
    public LWMap getMap()
    {
        return map;
    }
    
    public void loadDefaultStyles()
    {
        StyleReader.readStyles("compare.weight.css");
        
        // reading default number of styles from default style file
        // should default number be stored in css file or in VueResources?
        for(int si=0;si<DEFAULT_STYLE_CHOICES;si++)
        {
           //Style currNodeStyle = StyleMap.getStyle("node.w" + (si +1)); 
           nodeStyles.add(StyleMap.getStyle("node.w" + (si +1)));
           linkStyles.add(StyleMap.getStyle("link.w" + (si +1))); 
        }
    }
    
    public void loadDefaultSettings()
    {
        if(nodeModel!=null)
        {
            nodeModel.clear();
        }
        if(linkModel!=null)
        {
            linkModel.clear();
        }
        
        // reading default number of styles from default style file
        // should default number be stored in css file or in VueResources?
        for(int si=0;si<DEFAULT_STYLE_CHOICES;si++)
        {
           Style currNodeStyle = StyleMap.getStyle("node.w" + (si+1));
           if(currNodeStyle !=null)
           {
             nodeModel.addRow((int)(si*(100.0/DEFAULT_STYLE_CHOICES)),
                              (int)((si+1)*(100.0/DEFAULT_STYLE_CHOICES)),
                              Style.hexToColor(currNodeStyle.getAttribute("background")),Color.BLACK);
                              //,Style.hexToColor(currNodeStyle.getAttribute("foreground")));
           }
           
           Style currLinkStyle = StyleMap.getStyle("link.w" + (si+1));
           if(currLinkStyle !=null)
           {
             //System.out.println("wvsp: default styles: " + currLinkStyle.getAttribute("background"));
             linkModel.addRow((int)(si*(100.0/DEFAULT_STYLE_CHOICES)),
                              (int)((si+1)*(100.0/DEFAULT_STYLE_CHOICES)),
                              Style.hexToColor(currLinkStyle.getAttribute("background")),Color.BLACK);
                             //,Style.hexToColor(currLinkStyle.getAttribute("foreground")));
           }
           
        }
        
    }
    
    public void loadSettings()
    {
        // don't load default settings if have LWMergeMap, only load those from the Map file.
        if(getMap() == null)
        {
            loadDefaultSettings();
        }
    }
    
    // sets model based on parameter choice selection
    public void setModel()
    {
        if(parameterChoice.getSelectedIndex() == 0)
        {
            intervalList.setModel(nodeModel);
        }
        else
        {
            intervalList.setModel(linkModel);
        }
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == parameterChoice)
        {
            setModel();
            validate();
        }
        if(e.getSource() == paletteChoice)
        {
            String nodeOrLink = "node";
            if(parameterChoice.getSelectedIndex()==1)
            {
                nodeOrLink = "link";
            }
            int i = 1;
            Iterator<Color> colors = ((Colors)(paletteChoice.getSelectedItem())).getColors().iterator();
            while(colors.hasNext())
            {
                Style s = StyleMap.getStyle(nodeOrLink+".w" + (i++));
                s.setAttribute("background",Style.colorToHex(colors.next()).toString());
            }
           // loadDefaultStyles();
            loadDefaultSettings();
            repaint();
        }
    }
    
    public static void main(String[] args)
    {
        javax.swing.JFrame f = new javax.swing.JFrame("test");
        f.setBounds(100,100,300,400);
        f.setLayout(new java.awt.GridLayout(1,1));
        f.getContentPane().add(new WeightVisualizationSettingsPanel(null));
        f.pack();
        f.setVisible(true);
    }
    
    class PercentageInterval
    {
        private int start;
        private int end;
        
        public PercentageInterval(int start,int end)
        {
            this.start = start;
            this.end = end;
        }
        
        public int getStart()
        {
            return start;
        }
        
        public int getEnd()
        {
            return end;
        }
    }
    
    class IntervalStylePreview
    {
        private Color background;
        private Color foreground;
        
        public IntervalStylePreview(Color back,Color front)
        {
            background = back;
            foreground = front;
        }
        
        public Color getBackground()
        {
            return background;
        }
        
        public Color getForeground()
        {
            return foreground;
        }
        
        public void setBackground(Color bg)
        {
            background = bg;
        }
        
        public void setForeground(Color fg)
        {
            foreground = fg;
        }
        
    }
    
    class PercentageIntervalRenderer extends JPanel implements TableCellRenderer
    {
        private JTextField startField = new JTextField(3);
        private JTextField endField = new JTextField(3);
        
        public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row, int col)
        {
            // these intial values are not likely to be seen (unless wrong class used for value)
            // just a fail safe as this point
            int start = 0;
            int end = 100;
            
            if(value instanceof PercentageInterval)
            {
                PercentageInterval pi = (PercentageInterval)value;
                start = pi.getStart();
                end = pi.getEnd();
            }
            
            startField.setText(start+"");
            endField.setText(end+"");
            add(startField);
            add(endField);
            startField.setEnabled(false);
            endField.setEnabled(false);
            return this;
        }
    }
    
    class PercentageIntervalEditor extends javax.swing.AbstractCellEditor implements TableCellEditor
    {
        
        private JTextField startField = new JTextField(3);
        private JTextField endField = new JTextField(3);
        private JPanel panel = new JPanel();
        
        public PercentageIntervalEditor()
        {
            startField.setHorizontalAlignment(JTextField.LEFT);
            endField.setHorizontalAlignment(JTextField.LEFT);
            panel.add(startField);
            panel.add(endField);
        }
        
        public java.awt.Component getTableCellEditorComponent(JTable table,Object value,boolean isSelected,int row,int col)
        {
            if(value instanceof PercentageInterval)
            {    
               PercentageInterval pi = (PercentageInterval)value;
               startField.setText(pi.getStart()+"");
               endField.setText(pi.getEnd()+"");
            }
            else
            {
                startField.setText("0");
                endField.setText("0");
            }
            
            startField.setEnabled(false);
            endField.setEnabled(false);
            
            /*p.addMouseListener(new java.awt.event.MouseAdapter(){
               public void mouseClicked(java.awt.event.MouseEvent e)
               {
                   System.out.println("Mouse clicked on PI cell editor: " + e);
               }
            });*/
            
            return panel;
        }
        
        public Object getCellEditorValue()
        {
            return new PercentageInterval(Integer.parseInt(startField.getText()),Integer.parseInt(endField.getText()));
        }
         
    }
    
    class IntervalStylePreviewRenderer implements TableCellRenderer
    {   
        public java.awt.Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row, int col)
        {
            // these default colors are not likely to be seen (unless wrong class used for value)
            // just a fail safe as this point
            Color textColor = Color.BLACK;
            Color backColor = Color.WHITE;
            
            if(value instanceof IntervalStylePreview)
            {
                IntervalStylePreview isp = (IntervalStylePreview)value;
                textColor = isp.getForeground();
                backColor = isp.getBackground();
            }
            
            JPanel renderer = new JPanel(); 
            javax.swing.BoxLayout boxLayout = new javax.swing.BoxLayout(renderer,javax.swing.BoxLayout.X_AXIS);
            renderer.setLayout(boxLayout);
            final JLabel buttonImage = new JLabel("Label");
            buttonImage.setOpaque(true);
            buttonImage.setForeground(textColor);
            buttonImage.setBackground(backColor);
            //JLabel hotSpot = new JLabel("[edit style]");
            javax.swing.JButton hotSpot = new javax.swing.JButton("[edit style]")
            {
                public void paintComponent(java.awt.Graphics g)
                {
                    g.drawString("  [Edit Style]",0,buttonImage.getY()+ 12);
                }
            };
            renderer.add(buttonImage);
            renderer.add(hotSpot);
            return renderer;
        }
    }
   
    class IntervalStylePreviewEditor extends javax.swing.AbstractCellEditor implements TableCellEditor
    {   
        // these default colors are not likely to be seen (unless wrong class assigned to Table Cell value)
        // just a fail safe as this point
        Color textColor = Color.BLACK;
        Color backColor = Color.WHITE;

        private JLabel buttonImage;
       // private JLabel hotSpot;
        private javax.swing.JButton hotSpot;
        private JPanel panel = new JPanel();
        //private IntervalStylePreview current;
        private int currentRow = 0;
          
        public IntervalStylePreviewEditor()
        {            
            javax.swing.BoxLayout boxLayout = new javax.swing.BoxLayout(panel,javax.swing.BoxLayout.X_AXIS);
            panel.setLayout(boxLayout);
            buttonImage = new JLabel("Label");
            buttonImage.setOpaque(true);
            buttonImage.setForeground(textColor);
            buttonImage.setBackground(backColor);
            //hotSpot = new JLabel("[edit style]");
            hotSpot = new javax.swing.JButton()
            {
                public void paintComponent(java.awt.Graphics g)
                {
                    g.drawString("  [Edit Style]",0,buttonImage.getY()+ 12);
                }
            };
            panel.add(buttonImage);
            panel.add(hotSpot);
            //$
              final javax.swing.JColorChooser chooser = new javax.swing.JColorChooser();
              final java.awt.event.ActionListener okListener = new java.awt.event.ActionListener()
              {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    buttonImage.setBackground(chooser.getColor());
                    if(/*current!=null &&*/ (parameterChoice.getSelectedIndex() == 0))
                    {
                      Style currStyle = StyleMap.getStyle("node.w"+(currentRow+1));
                      if(currStyle!=null)
                      {
                        //currStyle.setBackgroundColor(chooser.getColor());
                        currStyle.setAttribute("background",Style.colorToHex(chooser.getColor()));
                      }
                    }
                    if(/*current!=null && */(parameterChoice.getSelectedIndex() == 1))
                    {
                      Style currStyle = StyleMap.getStyle("link.w"+(currentRow+1));
                      if(currStyle!=null)
                      {
                        //currStyle.setBackgroundColor(chooser.getColor());
                        currStyle.setAttribute("background",Style.colorToHex(chooser.getColor()));
                      }
                    }
                }
              };
              hotSpot.addMouseListener(new java.awt.event.MouseAdapter(){
                 public void mouseReleased(java.awt.event.MouseEvent e)
                 {
                     //System.out.println("wvsp: " + e);
                     chooser.setColor(backColor);
                     try
                     {        
                       javax.swing.JColorChooser.createDialog(panel,"Color",true,chooser,okListener,null).setVisible(true);;
                     }
                     catch(Exception ex)
                     {
                         ex.printStackTrace();
                     }
                     backColor = chooser.getColor();
                     stopCellEditing();
                 }
              });
            //$
        }
        
        public java.awt.Component getTableCellEditorComponent(JTable table,final Object value,boolean isSelected,int row,int col)
        {
            
            if(value instanceof IntervalStylePreview)
            {
                IntervalStylePreview isp = (IntervalStylePreview)value;
                //current = isp;
                textColor = isp.getForeground();
                backColor = isp.getBackground();
            }
            
            currentRow = row;
            
            buttonImage.setForeground(textColor);
            buttonImage.setBackground(backColor);
            return panel;
        }
        
        public Object getCellEditorValue()
        {
            return new IntervalStylePreview(backColor,textColor);
            //return current;
        }
       
    }
    
        
    class IntervalListModel implements TableModel
    {
        
        private List<PercentageInterval> piList = new ArrayList<PercentageInterval>();
        private List<IntervalStylePreview> ispList = new ArrayList<IntervalStylePreview>();
        
        public void addRow(int startPercentage,int endPercentage,Color backColor,Color foreColor)
        {
           piList.add(new PercentageInterval(startPercentage,endPercentage));
           ispList.add(new IntervalStylePreview(backColor,foreColor));
        }
        
        public void addTableModelListener(TableModelListener tml)
        {
            
        }
        
        public void removeTableModelListener(TableModelListener tml)
        {
            
        }
        
        public Class getColumnClass(int col)
        {
            if(col == 0)
            {
                return PercentageInterval.class;
            }
            else
            {
                return IntervalStylePreview.class;
            }
            
        }
        
        public int getColumnCount()
        {
            return 2;
        }
        
        public String getColumnName(int col)
        {
            if(col == 0)
                return "Intervals:";
            else
                return "Preview:";
        }
        
        public int getRowCount()
        {
            return piList.size();
        }
        
        public Object getValueAt(int row,int col)
        {
            if(col == 0)
            {
                return piList.get(row);
            }
            else
            {
                return ispList.get(row);
            }
        }
        
        public boolean isCellEditable(int row,int col)
        {
            return true;
        }
        
        public void setValueAt(Object value,int row,int col)
        {
           if(col == 0 && (value instanceof PercentageInterval))
           {
                piList.set(row,(PercentageInterval)value);
           }
           if(col == 1 && (value instanceof IntervalStylePreview))
           {
               ispList.set(row,(IntervalStylePreview)value);
           }
        }
        
        public int getIndex(IntervalStylePreview isp)
        {
            return ispList.indexOf(isp);
        }
        
        public void clear()
        {
            piList.clear();
            ispList.clear();
        }
        
    }
    
}
