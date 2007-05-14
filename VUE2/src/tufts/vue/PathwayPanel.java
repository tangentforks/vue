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

import tufts.vue.gui.*;
import tufts.vue.gui.formattingpalette.ButtonlessComboBoxUI;


import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.TreeUI;
import javax.swing.table.*;
import javax.swing.border.*;

import edu.tufts.vue.preferences.ui.tree.VueTreeUI;

/**
 * Provides a panel that displays the PathwayTable with note panel
 * editable view of currently selected pathway or item on pathway, as
 * well as controls for navigating through the pathway, and
 * adding/removing new pathways, adding/removing items to the
 * pathways.
 *
 * @see PathwayTable
 * @see PathwayTableModel
 * @see LWPathwayList
 * @see LWPathway
 *
 * @author  Daisuke Fujiwara
 * @author  Scott Fraize
 * @version $Revision: 1.81 $ / $Date: 2007-05-14 03:31:45 $ / $Author: sfraize $
 */

public class PathwayPanel extends JPanel
    implements ActionListener, ActiveListener<LWPathway.Entry>
{    
    private Frame mParentFrame;
    
    private VueButton btnAddSlide = new VueButton("presentationDialog.button.makeSlides",this);
    private VueButton btnMergeInto = new VueButton("presentationDialog.button.mergeInto",this);
    private VueButton btnLiveMap = new VueButton("presentationDialog.button.liveMap", this);
    
    //edit
    private VueButton btnPreview = new VueButton("presentationDialog.button.preview", this);
    private VueButton btnPreviewFull = new VueButton("presentationDialog.button.previewFull", this);
       
    //master slide
    private VueButton btnMasterSlide = new VueButton("presentationDialog.button.masterSlide",this);
    
    //new    
    private VueButton btnPresentationCreate = new VueButton("presentationDialog.button.add",this);        
    private VueButton btnPresentationDelete = new VueButton("presentationDialog.button.delete",this);
    

    //filter
    private JToggleButton btnPathwayOnly = new VueButton.Toggle("presentationDialog.button.viewAll",this);
    
    
    //map view
//     private ImageDropDown btnShowSlides = new ImageDropDown(VueResources.getImageIcon("presentationDialog.button.showSlides.raw"),
//                                                             VueResources.getImageIcon("presentationDialog.button.showNodes.raw"),
//                                                             VueResources.getImageIcon("presentationDialog.button.showSlides.disabled"));
    // hack for now as single button just to get this working:
    private final JToggleButton btnShowSlides = new VueButton.Toggle("presentationDialog.button.showSlides");
    
    
    //playback mode
    private ImageDropDown btnPlayMaps = new ImageDropDown(VueResources.getImageIcon("presentationDialog.button.playMap.raw"),
                                                          VueResources.getImageIcon("presentationDialog.button.playSlides.raw"),
                                                          VueResources.getImageIcon("presentationDialog.button.playSlides.disabled"));
    
    private VueButton btnPlay = new VueButton("presentationDialog.button.play",this);    
                                            
    //Section Labels for the top
    private JLabel lblCreateSlides = new JLabel(VueResources.getString("presentationDialog.createslides.label"));
    private JLabel lblEditSlides = new JLabel(VueResources.getString("presentationDialog.editslides.label"));
    private JLabel lblMasterSlide = new JLabel(VueResources.getString("presentationDialog.masterslide.label"));
    private JLabel lblNew = new JLabel(VueResources.getString("presentationDialog.new.label"));    
    private JLabel lblFilter = new JLabel(VueResources.getString("presentationDialog.filter.label"));
    //private JLabel lblMapView = new JLabel(VueResources.getString("presentationDialog.mapview.label"));
    private JLabel lblMapView = new JLabel("Slide Icons");
    private JLabel lblPlayback = new JLabel(VueResources.getString("presentationDialog.playback.label"));

    public PathwayTable mPathwayTable;
    private PathwayTableModel mTableModel;
      
    
    private JLabel pathLabel;           // updated for current PathwayTable selection
    private JLabel pathElementLabel;    // updated for current PathwayTable selection
    private JTextArea notesArea;        // updated for current PathwayTable selection
    
    private LWPathway.Entry mSelectedEntry;
    private boolean mNoteKeyWasPressed = false;

    private final Color BGColor = new Color(241, 243, 246);
 
    //MK - Despite these not being used on the presentation window anymore they are still
    //referenced by the pathway tool so they're sticking around for now.
    private static final Action path_rewind = new PlayerAction("pathway.control.rewind");
    private static final Action path_backward = new PlayerAction("pathway.control.backward");
    private static final Action path_forward = new PlayerAction("pathway.control.forward");
    private static final Action path_last = new PlayerAction("pathway.control.last");

    private final JTabbedPane tabbedPane = new JTabbedPane();
    
    public PathwayPanel(Frame parent) 
    {   
    	//DISABLE THE NOTES BUTTONS FOR NOW UNTIL WE FIGURE OUT WHAT THEY DO -MK
    	Icon i =VueResources.getIcon("presentationDialog.button.viewAll.raw");
    	addToolTips();
    //	btnAnnotateSlide.setEnabled(false);
    //	btnAnnotatePresentation.setEnabled(false);
    	btnMergeInto.setEnabled(false);
    	btnPlayMaps.setEnabled(false);
    	//btnLiveMap.setEnabled(false);
    	btnPreviewFull.setEnabled(false);
    	btnShowSlides.setEnabled(true);
    	btnShowSlides.setSelected(true);
    	btnPlayMaps.setEnabled(false);
    //	btnPlaySlides.setEnabled(false);
//    	btnDisplayAsMap.setEnabled(false);
  //  	btnDisplayAsText.setEnabled(false);
    	//END    	

        btnShowSlides.addActionListener(this);
        LWPathway.setShowSlides(btnShowSlides.isSelected());    
    	
        //Font defaultFont = new Font("Helvetica", Font.PLAIN, 12);
        //Font highlightFont = new Font("Helvetica", Font.BOLD, 12);
        final Font defaultFont = getFont();
        final Font boldFont = defaultFont.deriveFont(Font.BOLD);
        final Font smallFont = defaultFont.deriveFont((float) boldFont.getSize()-1);
        final Font smallBoldFont = smallFont.deriveFont(Font.BOLD);
    
        mParentFrame = parent;
        setBorder(new EmptyBorder(4, 4, 7, 4));

        //-------------------------------------------------------
        // Set up the PathwayTableModel, PathwayTable & Listeners
        //-------------------------------------------------------

        mTableModel = new PathwayTableModel();
        mPathwayTable = new PathwayTable(mTableModel);
        
        mPathwayTable.setBackground(BGColor);
        
   
        notesArea = new JTextArea("");
        notesArea.setColumns(5);
        notesArea.setWrapStyleWord(true);
        notesArea.setAutoscrolls(true);
        notesArea.setLineWrap(true);
        notesArea.setBackground(Color.white);
        notesArea.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) { mNoteKeyWasPressed = true; }
                public void keyReleased(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) ensureNotesSaved(); }
            });
        notesArea.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    if (DEBUG.PATHWAY) System.out.println("PathwayPanel.notesArea     focusLost to " + e.getOppositeComponent());
                    ensureNotesSaved();
                }
                public void focusGained(FocusEvent e) {
                    if (DEBUG.PATHWAY) System.out.println("PathwayPanel.notesArea focusGained from " + e.getOppositeComponent());
                }
            });


        JPanel noteLabelPanel = new VueUtil.JPanelAA();
        JLabel notesLabel = new JLabel(" Notes: ");
        //notesLabel.setFont(smallFont);
        noteLabelPanel.setLayout(new BoxLayout(noteLabelPanel, BoxLayout.X_AXIS));
        noteLabelPanel.add(notesLabel);
        noteLabelPanel.add(pathLabel = new JLabel(""));
        noteLabelPanel.add(pathElementLabel = new JLabel(""));
        pathLabel.setFont(smallBoldFont);
        pathElementLabel.setFont(smallFont);
        pathElementLabel.setForeground(Color.red.darker());

        JPanel notesPanel = new JPanel(new BorderLayout(0,0));
        notesPanel.add(noteLabelPanel, BorderLayout.NORTH);
        notesPanel.setBorder(new EmptyBorder(7,0,0,0));
        notesPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);


        
        //-------------------------------------------------------
        // Layout for the table components 
        //-------------------------------------------------------
        
        GridBagLayout bag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        setLayout(bag);
        c.gridwidth = GridBagConstraints.REMAINDER; // put everything in one column
        c.weightx = 1.0; // make sure everything can fill to width
        
        //-------------------------------------------------------
        // add pathway create/delete/lock control panel
        //-------------------------------------------------------

        c.fill = GridBagConstraints.HORIZONTAL;
        //bag.setConstraints(pathwayMasterPanel, c);
        //add(pathwayMasterPanel);        
        JPanel playbackPanel = new JPanel();
        JPanel slidePanel = new JPanel();
        
        buildPlaybackPanel(playbackPanel);
        buildSlidePanel(slidePanel);
        tabbedPane.add(VueResources.getString("presentationDialog.slideTab.title"), slidePanel);
        tabbedPane.add(VueResources.getString("presentationDialog.playbackTab.title"), playbackPanel);
  
    //    setLayout(new BorderLayout());
        add(tabbedPane,c);
        //-------------------------------------------------------
        // add the PathwayTable
        //-------------------------------------------------------

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 2.5;
        JScrollPane tablePane = new JScrollPane(mPathwayTable);
        tablePane.setPreferredSize(new Dimension(getWidth(), 180));
        bag.setConstraints(tablePane, c);
        add(tablePane,c);
        
        //-------------------------------------------------------
        // Add the notes panel
        //-------------------------------------------------------
        
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.insets = new Insets(0,0,0,0);
        notesPanel.setPreferredSize(new Dimension(getWidth(), 80));
        bag.setConstraints(notesPanel, c);
        add(notesPanel);

        //-------------------------------------------------------
        // Disable all that need to be
        //-------------------------------------------------------
        
        updateEnabledStates();

        //-------------------------------------------------------
        // Set up the listeners
        //-------------------------------------------------------
        
        
        
        VUE.addActiveListener(LWPathway.Entry.class, this);
        
        VUE.getSelection().addListener(new LWSelection.Listener() {
                public void selectionChanged(LWSelection s) {
                    final LWPathway curPath = getSelectedPathway();
                    if (s.size() == 1 && s.first().inPathway(curPath)) {
                        curPath.setIndex(curPath.firstIndexOf(s.first()));
                    } else
                        updateEnabledStates();
                }
            }     
        );          
    }
    
 /*   public void paint(Graphics g)
    {
    	super.paint(g);
    	System.out.println("master panel :" + masterPanel.getSize().toString());
    	System.out.println("new panel :" + newPanel.getSize().toString());
    }*/
    
    private void addToolTips()
    {
    	String baseProp = "presentationDialog.button.";    	

    	btnAddSlide.setToolTipText(VueResources.getString(baseProp+"makeSlides.tooltip"));
        btnMergeInto.setToolTipText(VueResources.getString(baseProp+"mergeInto.tooltip"));
        btnLiveMap.setToolTipText(VueResources.getString(baseProp+"liveMap.tooltip"));
        
        //edit
        btnPreview.setToolTipText(VueResources.getString(baseProp+"preview.tooltip"));
        btnPreviewFull.setToolTipText(VueResources.getString(baseProp+"previewFull.tooltip"));
           
        //master slide
        btnMasterSlide.setToolTipText(VueResources.getString(baseProp+"masterSlide.tooltip"));
        
        //new    
        btnPresentationCreate.setToolTipText(VueResources.getString(baseProp+"add.tooltip"));        
        btnPresentationDelete.setToolTipText(VueResources.getString(baseProp+"delete.tooltip"));
        

        //filter
        btnPathwayOnly.setToolTipText(VueResources.getString(baseProp+"viewAll.tooltip"));
        
        // hack for now as single button just to get this working:
        btnShowSlides.setToolTipText(VueResources.getString(baseProp+"showNodes.tooltip"));
        
        
        //playback mode
        btnPlayMaps.setToolTipText(VueResources.getString(baseProp+"playSlides.tooltip"));
        btnPlay.setToolTipText(VueResources.getString(baseProp+"play.tooltip"));               	
    }
    
    private void buildSlidePanel(JPanel slidePanel)
    {
    	slidePanel.setLayout(new BoxLayout(slidePanel,BoxLayout.X_AXIS));
    	JPanel createSlidePanel = new JPanel();
    	JPanel editPanel = new JPanel();
    	JPanel masterPanel = new JPanel();
    	JPanel newPanel = new JPanel();	
    	JPanel deletePanel = new JPanel();
    	
    	DividerPanel p1 = new DividerPanel(25);
        DividerPanel p2 = new DividerPanel(25);
        DividerPanel p3 = new DividerPanel(25);
        DividerPanel p4 = new DividerPanel(25);
        
        
        java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
        
        createSlidePanel.setLayout(new GridBagLayout());
        editPanel.setLayout(new GridBagLayout());
        masterPanel.setLayout(new GridBagLayout());
        newPanel.setLayout(new GridBagLayout());
        deletePanel.setLayout(new GridBagLayout());
        
        lblCreateSlides.setFont(VueResources.getFont("node.icon.font"));
        lblEditSlides.setFont(VueResources.getFont("node.icon.font"));
        lblMasterSlide.setFont(VueResources.getFont("node.icon.font"));
        lblNew.setFont(VueResources.getFont("node.icon.font"));
        
        
        //START CREATE SLIDE PANEL
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        createSlidePanel.add(lblCreateSlides,gbConstraints);
        
        gbConstraints.gridx=0;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.gridy=1;
        gbConstraints.anchor=GridBagConstraints.WEST;
        gbConstraints.fill=GridBagConstraints.NONE;
        createSlidePanel.add(btnAddSlide,gbConstraints);
    
        gbConstraints.gridx=1;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.anchor=GridBagConstraints.WEST;
        createSlidePanel.add(btnMergeInto,gbConstraints);
        
        gbConstraints.gridx=2;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.anchor=GridBagConstraints.WEST;
        createSlidePanel.add(btnLiveMap,gbConstraints);
        //END CREATE SLIDE PANEL
        
        //START EDIT PANEL
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        gbConstraints.fill=GridBagConstraints.NONE;        
        editPanel.add(lblEditSlides,gbConstraints);
        
        gbConstraints.gridx=0;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.WEST;
        editPanel.add(btnPreviewFull,gbConstraints);
    
        gbConstraints.gridx=1;
        gbConstraints.gridy=1;       
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.anchor=GridBagConstraints.WEST;
        editPanel.add(btnPreview,gbConstraints);
        //END EDIT PANEL
        
        //START MASTER PANEL        
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        masterPanel.add(lblMasterSlide,gbConstraints);
        
        gbConstraints.gridx=0;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.WEST;
        masterPanel.add(btnMasterSlide,gbConstraints);
        //END MASTER PANEL
        
        //START NEW PANEL
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.insets = new Insets(5,0,0,0);
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        newPanel.add(lblNew,gbConstraints);
        
        gbConstraints.gridx=0;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.insets = new Insets(3,0,6,0);
        gbConstraints.anchor=GridBagConstraints.WEST;
        newPanel.add(btnPresentationCreate,gbConstraints);
        //END NEW PANEL
        
        //START DELETE PANEL
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.insets = new Insets(0,0,0,0);
        gbConstraints.fill=GridBagConstraints.NONE;
        deletePanel.add(new JPanel(),gbConstraints);
        
        
        gbConstraints.gridx=0;
        gbConstraints.gridy=1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.insets = new Insets(2,0,0,0);
        deletePanel.add(btnPresentationDelete,gbConstraints);
        //END DELETE PANEL        
        
        slidePanel.add(Box.createHorizontalStrut(1));
        slidePanel.add(createSlidePanel);
        slidePanel.add(Box.createHorizontalStrut(1));
        slidePanel.add(p1);
        slidePanel.add(Box.createHorizontalStrut(1));
        slidePanel.add(editPanel);                
        slidePanel.add(Box.createHorizontalStrut(1));
        slidePanel.add(p3);
        slidePanel.add(Box.createHorizontalStrut(5));
        slidePanel.add(masterPanel);        
        slidePanel.add(p4);
        slidePanel.add(Box.createHorizontalStrut(5));                
        slidePanel.add(newPanel);        
        slidePanel.add(p2);
        slidePanel.add(Box.createHorizontalStrut(1));
        slidePanel.add(deletePanel);
        slidePanel.add(Box.createHorizontalStrut(1));        
       
       
        return;
    }
    
       
    private void buildPlaybackPanel(JPanel presentationPanel)
    {
    	presentationPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    	JPanel filterPanel = new JPanel();
    	JPanel mapViewPanel = new JPanel();
    	JPanel playBackPanel = new JPanel();
    	    	
    	DividerPanel p1 = new DividerPanel(25);
        DividerPanel p2 = new DividerPanel(25);
                
        java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
        
        filterPanel.setLayout(new GridBagLayout());
        mapViewPanel.setLayout(new GridBagLayout());
        playBackPanel.setLayout(new GridBagLayout());
        
        lblFilter.setFont(VueResources.getFont("node.icon.font"));
        lblMapView.setFont(VueResources.getFont("node.icon.font"));
        lblPlayback.setFont(VueResources.getFont("node.icon.font"));        
        
        
        //START FILTER PANEL
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        gbConstraints.weightx=0;
        gbConstraints.weighty=0;
        filterPanel.add(lblFilter,gbConstraints);
        
        gbConstraints.gridx=0;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.gridy=1;
        gbConstraints.fill=GridBagConstraints.BOTH;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        filterPanel.add(btnPathwayOnly,gbConstraints);            
        //END FILTER PANEL
        
        //START MAP VIEW PANEL
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        gbConstraints.fill=GridBagConstraints.NONE;        
        gbConstraints.weightx=0;
        gbConstraints.weighty=0;
        //gbConstraints.insets = new Insets(4,15,4,15);
        mapViewPanel.add(lblMapView,gbConstraints);
        
        gbConstraints.gridx=0;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        mapViewPanel.add(btnShowSlides,gbConstraints);    
        //END MAP VIEW PANEL
        
        //START PLAYBACK PANEL        
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        gbConstraints.weightx=0;
        gbConstraints.weighty=0;
        playBackPanel.add(lblPlayback,gbConstraints);
        
        gbConstraints.gridx=0;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        playBackPanel.add(btnPlayMaps,gbConstraints);
        
        gbConstraints.gridx=1;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.insets = new Insets(0,6,0,0);
        gbConstraints.fill=GridBagConstraints.NONE;
        playBackPanel.add(btnPlay,gbConstraints);
        //END PLAYBACK PANEL
        
       
        presentationPanel.add(Box.createHorizontalStrut(1));
        presentationPanel.add(filterPanel);
        presentationPanel.add(Box.createHorizontalStrut(1));
        presentationPanel.add(p1);
        presentationPanel.add(Box.createHorizontalStrut(1));
        presentationPanel.add(mapViewPanel);                
        presentationPanel.add(Box.createHorizontalStrut(1));
        presentationPanel.add(p2);
        presentationPanel.add(Box.createHorizontalStrut(5));
        presentationPanel.add(playBackPanel);        
        presentationPanel.add(Box.createHorizontalStrut(5));                
                                                                         	
        return;
    }
    

    class ImageDropDown extends JPanel {
        
    	ImageIcon[] images;        
        private JComboBox comboList;
        ImageIcon disabledIcon = null;
        public ImageDropDown(ImageIcon icon1, ImageIcon icon2,ImageIcon icon3) {
            super(new BorderLayout());
            
            disabledIcon = icon3;
            
            //Load the pet images and create an array of indexes.
            images = new ImageIcon[2];
            Integer[] intArray = new Integer[2];
            
            intArray[0] = new Integer(0);
            intArray[1] = new Integer(1);
                
            images[0] = icon1;
            images[1] = icon2;                                                      

            //Create the combo box.
            
            comboList = new JComboBox(intArray);
            ComboBoxRenderer renderer= new ComboBoxRenderer();
            renderer.setPreferredSize(new Dimension(20, 20));
            comboList.setRenderer(renderer);
            comboList.setMaximumRowCount(3);
            comboList.setUI(new ButtonlessComboBoxUI());
            comboList.setOpaque(false);
            comboList.setBorder(BorderFactory.createEmptyBorder());
            comboList.setBackground(this.getBackground());
            //Lay out the demo.
            setOpaque(true);
            add(comboList, BorderLayout.PAGE_START);
          //  this.setPreferredSize(new Dimension(comboList.getWidth()-10,comboList.getHeight()));            
        }

        public void setEnabled(boolean enabled)
        {
        	comboList.setEnabled(enabled);
        }
        public JComboBox getComboBox()
        {
        	return comboList;
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

                /*if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }
                */
                
                //Set the icon and text.  If icon was null, say so.
                ImageIcon icon = images[selectedIndex];
                setIcon(icon);
                if (!comboList.isEditable())
                	setIcon(disabledIcon);
      
                return this;
            }
        }
//      default offsets for drawing popup arrow via code
        public int mArrowSize = 3;
        public int mArrowHOffset  = -9;
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
    
    
    public void activeChanged(ActiveEvent<LWPathway.Entry> e) {
    	updateTextAreas(e.active);
        updateEnabledStates();
    }
    

    private void ensureNotesSaved() {
        if (mNoteKeyWasPressed && mSelectedEntry != null) {
            mNoteKeyWasPressed = false; // do this first or callback in updateTextAreas will cause 2 different setter calls
            mSelectedEntry.setNotes(notesArea.getText());
            VUE.getUndoManager().mark();
        }
    }

    /** Returns the currently selected pathway.  As currently
     * selected must always be same as VUE globally selected,
     * we just return that. */
    private LWPathway getSelectedPathway() {
        return VUE.getActivePathway();
    }

    public static class PlaybackToolPanel extends JPanel
    {
        public PlaybackToolPanel() {
            super(new GridLayout(1, 4, 1, 0));

            add(new VueButton(path_rewind));
            add(new VueButton(path_backward));
            add(new VueButton(path_forward));
            add(new VueButton(path_last));
        }
    }

    private static class PlayerAction extends AbstractAction
    {
        PlayerAction(String name) {
            // as we're to be used by a VueButton, store the key
            // as the action command, not the name, as we don't
            // want it to show up as the button label
            putValue(Action.ACTION_COMMAND_KEY, name);
        }
        
        public void actionPerformed(ActionEvent e)
        {
            LWPathway pathway = VUE.getActivePathway();
            if (pathway == null)
                return;
            String cmd = e.getActionCommand();
                 if (cmd.endsWith("rewind"))    pathway.setFirst();
            else if (cmd.endsWith("backward"))  pathway.setPrevious();
            else if (cmd.endsWith("forward"))   pathway.setNext();
            else if (cmd.endsWith("last"))      pathway.setLast();
            else
                throw new IllegalArgumentException(this + " " + e);
                 
                 //VUE.getUndoManager().mark(); // the above stuff not worth having undoable
        }

        private static void setAllEnabled(boolean t) {
            path_rewind.setEnabled(t);
            path_backward.setEnabled(t);
            path_forward.setEnabled(t);
            path_last.setEnabled(t);
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        Object btn = e.getSource();
        LWPathway pathway = getSelectedPathway();

        if (pathway == null && btn != btnPresentationCreate)
            return;
        if (btn == btnPreview)
        {
        	VUE.getSlideDock().setVisible(true);
        	VUE.getSlideViewer().showSlideMode();
        }    
        else if (btn == btnPlay)
        {
        	 PresentationTool tool= PresentationTool.getTool();//VueToolbarController.getController().getTool("viewTool");
        	 VueToolbarController.getController().setSelectedTool(tool);
        	((PresentationTool)tool).startPresentation();
        	VUE.toggleFullScreen(true);
        }
        else if (btn == btnMasterSlide)
        {
        	VUE.getSlideDock().setVisible(true);
        	VUE.getSlideViewer().showMasterSlideMode();
        }
        else if (btn == btnAddSlide)  { pathway.add(VUE.getSelection().iterator()); }
        else if (btn == btnMergeInto)  { pathway.addMergedSlide(VUE.getSelection()); }
        else if (btn == btnLiveMap)  {
            LWPortal portal = LWPortal.create();
            pathway.getMap().add(portal);
            pathway.add(portal);
            pathway.getUndoManager().mark("New Pathway Portal"); 
        }
      //  else if (btn == btnElementUp)   { pathway.moveCurrentUp(); }
      //  else if (btn == btnElementDown) { pathway.moveCurrentDown(); }

        else if (btn == btnPresentationDelete)   
        {
        	System.out.println("Current " + pathway.getCurrentEntry());
        	if (pathway.getCurrentEntry() == null)
        	{
        		//delete the pathway
        		deletePathway(pathway);
        	}
        	else
        	{
        		//delete the current entry
//        		 This is a heuristic to try and best guess what the user might want to
                // actually remove.  If nothing in selection, and we have a current pathway
                // index/element, remove that current pathway element.  If one item in
                // selection, also remove whatever the current element is (which ideally is
                // usually also the selection, but if it's different, we want to prioritize
                // the current element hilighted in the PathwayTable).  If there's MORE than
                // one item in selection, do a removeAll of everything in the selection.
                // This removes ALL instances of everything in selection, so that, for
                // instance, a SelectAll followed by pathway delete is guaranteed to empty
                // the pathway entirely.

                if (pathway.getCurrentIndex() >= 0 && VUE.ModelSelection.size() < 2) {
                    pathway.remove(pathway.getCurrentIndex());
                } else {
                    pathway.remove(VUE.getSelection().iterator());
                }
        	}
        //	deletePathway(pathway); 
        }
        else if (btn == btnPresentationCreate)   { new PathwayDialog(mParentFrame, mTableModel, getLocationOnScreen()).setVisible(true); }
     //   else if (btn == btnLockPresentation)     { pathway.setLocked(!pathway.isLocked()); }
        else if (btn == btnPathwayOnly) {
            toggleHideEverythingButCurrentPathway(!btnPathwayOnly.isSelected());
        } else if (btn == btnShowSlides) {
            LWPathway.setShowSlides(btnShowSlides.isSelected());
            pathway.notify("pathway.showSlides");
        } else {
            System.out.println(this + ": Unhandled action: " + e);
        }

        VUE.getUndoManager().mark();
    }

    private LWPathway exclusiveDisplay;
    private synchronized void toggleHideEverythingButCurrentPathway(boolean clearFilter)
    {
        final LWPathway pathway = VUE.getActivePathway();
        final LWMap map = pathway.getMap();
        
        if (pathway == null || map == null)
            return;

        // We have to use the FILTER flag, in case a pathway member
        // is the child of another node (that isn't on the pathway),
        // so that when the parent "hides" because it's not on the
        // pathway, it doesn't also hide the child.

        // This code is a bit complicated, as it both sets a pathway
        // to be exclusively shown, or toggles it if it already is.

        // As we only support one global "filter" at a time,
        // we first we de-filter (show) everything on the map.

        for (LWComponent c : map.getAllDescendents())
            c.setFiltered(false);
        
        if (exclusiveDisplay == pathway || clearFilter) {
            // We're toggling: just leave everything visible (de-filtered) in the map
            exclusiveDisplay = null;
            clearFilter = true;
        } else {

            // We're exclusively showing the current pathway: hide (filter) everything
            // that isn't in it.  Currently, any child of an LWComponent that is on a
            // pathway, is also considered on that pathway for display purposes.

            filterAllOutsidePathway(map.getChildList(), pathway);
            exclusiveDisplay = pathway;
        }

        // Now we make sure the Pathway objects themselves
        // have their filter flag properly set.

        for (LWPathway path : map.getPathwayList()) {
            if (clearFilter)
                path.setFiltered(false);
            else
                path.setFiltered(path != pathway);
        }
        
        pathway.notify(this, "pathway.exclusive.display");
    }

    private void filterAllOutsidePathway(Iterable<LWComponent> iterable, LWPathway pathway) {
        for (LWComponent c : iterable) {
            if (c.inPathway(pathway))
                continue;
            c.setFiltered(true);
            if (c.hasChildren())
                filterAllOutsidePathway(c.getChildList(), pathway);
        }
    }
    
   
    private void updateAddRemoveActions()
    {
        if (DEBUG.PATHWAY&&DEBUG.META) System.out.println(this + " updateAddRemoveActions");
        
        LWPathway path = getSelectedPathway();
        
        if (path == null || path.isLocked()) {
            btnAddSlide.setEnabled(false);
            btnLiveMap.setEnabled(false);
           // btnDeleteSlide.setEnabled(false);
            btnPresentationDelete.setEnabled(false);
            notesArea.setEnabled(false);
            return;
        }

        btnLiveMap.setEnabled(true);
        notesArea.setEnabled(true);
        btnPresentationDelete.setEnabled(true);
        
        boolean removeDone = false;
        LWSelection selection = VUE.ModelSelection;
        
        // if any viable index, AND path is open so you can see
        // it selected, enable the remove button.
        if (path.getCurrentIndex() >= 0 && path.isOpen()) {
           //btnDeleteSlide.setEnabled(true);
            removeDone = true;
        }
            
        if (selection.size() > 0) {
            btnAddSlide.setEnabled(true);
            if (!removeDone) {
                // if at least one element in selection is on current path,
                // enable remove.  Theoretically should only get here if
                // pathway is closed.
                boolean enabled = false;
                Iterator i = selection.iterator();
                while (i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    if (c.inPathway(path)) {
                        if (DEBUG.PATHWAY) System.out.println(this + " in selection enables remove: " + c + " on " + path);
                        enabled = true;
                        break;
                    }
                }
               // btnDeleteSlide.setEnabled(enabled);
            }
        } else {
            btnAddSlide.setEnabled(false);
            //if (!removeDone)
                //btnDeleteSlide.setEnabled(false);
        }
        btnMergeInto.setEnabled(selection.size() > 1);
    }

    public void updateEnabledStates()
    {        if (DEBUG.PATHWAY&&DEBUG.META) System.out.println(this + " updateEnabledStates");
        
        updateAddRemoveActions();
        
        LWPathway pathway = VUE.getActivePathway();
       
        if (pathway != null && pathway.length() > 1) {
            boolean atFirst = pathway.atFirst();
            boolean atLast = pathway.atLast();
             path_rewind.setEnabled(!atFirst);
             path_backward.setEnabled(!atFirst);
             path_forward.setEnabled(!atLast);
             path_last.setEnabled(!atLast);
//            if (pathway.isLocked()) {
  //              btnElementUp.setEnabled(false);
    //            btnElementDown.setEnabled(false);
      //      } else {
    //            btnElementUp.setEnabled(!atFirst);
    //            btnElementDown.setEnabled(!atLast);
           // }
        } else {
            PlayerAction.setAllEnabled(false);
    //        btnElementUp.setEnabled(false);
    //        btnElementDown.setEnabled(false);
        }
        btnPathwayOnly.setEnabled(pathway != null);
        
      //  btnLockPresentation.setEnabled(pathway != null);
    }
    
    /** Delete's a pathway and all it's contents */
    private void deletePathway(LWPathway p) {
        // We only ever delete the current pathway, and if's
        // exclusively displayed, make sure to undo the filter.
        // TODO: handle for undo: is critical for undo of the pathway create!
        if (exclusiveDisplay != null)
            toggleHideEverythingButCurrentPathway(true);
        VUE.getActiveMap().getPathwayList().remove(p);
    }

    private void setSelectedEntry(LWPathway.Entry entry) {
        mSelectedEntry = entry;
    }

    
    private boolean mTrailingNoteSave;
    private void updateTextAreas(LWPathway.Entry entry)
    {
        if (DEBUG.PATHWAY||DEBUG.META)
            System.out.println(this + " updateTextAreas: " + entry + ", skipping="+mTrailingNoteSave);
        
        if (mTrailingNoteSave)
            return;
        
        try {

            // Save any unsaved changes before re-setting the labels.  This is backup
            // lazy-save as workaround for java focusLost limitation.

            // We also wrap this in a loop spoiler because if notes do get saved at this
            // point, we'll come back here with an update event, and we want to ignore
            // it as we're switching to a new note anyway.  Ideally, focusLost on the
            // notesArea would have already handled this, but unfortunately java
            // delivers that event LAST, after the new focus component has gotten and
            // handled all it's events, and if it was the PathwayTable selecting another
            // curent node to display, this code is needed to be sure the note gets
            // saved.
            
            mTrailingNoteSave = true;
            ensureNotesSaved(); 
        } finally {
            mTrailingNoteSave = false;
        }
    
        if (entry == null) {
            pathLabel.setText("");
            pathElementLabel.setText("");
            notesArea.setText("");
            setSelectedEntry(null);
            return;
        }

        String pathText = entry.pathway.getLabel();
        String entryText;

        if (entry.isPathway()) {
            entryText = "";
        } else {
            pathText += ": ";
            if (DEBUG.PATHWAY) pathText += "(" + (entry.index()+1) + ") ";
            entryText = entry.getLabel();
        }

        pathLabel.setText(pathText);
        pathElementLabel.setText(entryText);
        notesArea.setText(entry.getNotes());
        
        mNoteKeyWasPressed = false;
        setSelectedEntry(entry);
    }

    public static void main(String[] args) {
        System.out.println("PathwayPanel:main");
        DEBUG.Enabled = DEBUG.INIT = true;
        VUE.init(args);
        //VueUtil.displayComponent(new PlaybackToolPanel());
    }
    

    public String toString() {
        return "PathwayPanel[" + VUE.getActivePathway() + "]";
    }
    
}
