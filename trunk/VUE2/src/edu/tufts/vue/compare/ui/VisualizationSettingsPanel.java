
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

package edu.tufts.vue.compare.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

/*
 * VisualizationSettingsPanel.java
 *
 * Created on May 14, 2007, 2:45 PM
 *
 * @author dhelle01
 */
public class VisualizationSettingsPanel extends JPanel implements ActionListener {

    // visualization types
    public final static int VOTE = 0;
    public final static int WEIGHT = 1;
    
    public final static String VOTE_STRING = "Vote";
    public final static String WEIGHT_STRING = "Weight";
    public final static String visualizationSettingsChoiceMessage = "Which type of visualization would you like to use?";
    public final static String filterOnBaseMapMessageString = "Only include items found on the guide map";
    
    private JComboBox visualizationChoice;
    
    private GridBagLayout gridBag;
    private GridBagConstraints gridBagConstraints;
    
    private VoteVisualizationSettingsPanel votePanel = VoteVisualizationSettingsPanel.getSharedPanel();
    private WeightVisualizationSettingsPanel weightPanel = WeightVisualizationSettingsPanel.getSharedPanel();
   
    private JPanel bottomPanel;
    private JCheckBox filterOnBaseMap;
    
    public VisualizationSettingsPanel() 
    {
        BoxLayout boxLayout = new BoxLayout(this,BoxLayout.Y_AXIS);
        gridBag = new GridBagLayout();
        gridBagConstraints = new GridBagConstraints();
        setLayout(gridBag);
       // setLayout(boxLayout);
        
        String[] choices = {"Vote","Weight"};
        visualizationChoice = new JComboBox(choices);
        visualizationChoice.addActionListener(this);
        
        JLabel visualizationSettingsChoiceLabel = new JLabel(visualizationSettingsChoiceMessage);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBag.setConstraints(visualizationSettingsChoiceLabel,gridBagConstraints);
        add(visualizationSettingsChoiceLabel);
        
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(visualizationChoice,gridBagConstraints);
        add(visualizationChoice);
        gridBag.setConstraints(votePanel,gridBagConstraints);
        gridBag.setConstraints(weightPanel,gridBagConstraints);
        add(votePanel);
        
        bottomPanel = new JPanel();
        filterOnBaseMap = new JCheckBox();
        JLabel filterOnBaseMapMessage = new JLabel(filterOnBaseMapMessageString);
        //gridBagConstraints.gridwidth = 1;
        //gridBag.setConstraints(filterOnBaseMap,gridBagConstraints);
        bottomPanel.add(filterOnBaseMap);
        //gridBag.setConstraints(filterOnBaseMapMessage,gridBagConstraints);
        bottomPanel.add(filterOnBaseMapMessage);
        add(bottomPanel);
    }
    
    public int getVisualizationSettingsType()
    {
        if(visualizationChoice.getSelectedIndex() == 0)
        {
            return VOTE;
        }
        else
        {
            return WEIGHT;
        }
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == visualizationChoice)
        {
            if(getVisualizationSettingsType() == VOTE)
            {
                remove(weightPanel);
                remove(bottomPanel);
                gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
                gridBag.setConstraints(votePanel,gridBagConstraints);
                add(votePanel);
                add(bottomPanel);
                revalidate();
                repaint();
            }
            if(getVisualizationSettingsType() == WEIGHT)
            {
                remove(votePanel);
                remove(bottomPanel);
                gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
                gridBag.setConstraints(weightPanel,gridBagConstraints);
                add(weightPanel);
                add(bottomPanel);
                revalidate();
                repaint();
            }
        }
    }
    
    public int getNodeThresholdSliderValue()
    {
        return votePanel.getNodeThresholdSliderValue();
    }
    
    public int getLinkThresholdSliderValue()
    {
        return votePanel.getLinkThresholdSliderValue();
    }
    
    public boolean getFilterOnBaseMap()
    {
        if(filterOnBaseMap.isSelected())
            return true;
        else
            return false;
    }
    
}
