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
import tufts.vue.beans.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;


/**
 * An an editor panel for LWLink properties.
 */
public class LinkToolPanel extends LWCToolPanel
{
    private AbstractButton mArrowStartButton;
    private AbstractButton mArrowEndButton;
 	
    protected void buildBox()
    {
        mArrowStartButton = new VueButton.Toggle("link.button.arrow.start");
        mArrowEndButton = new VueButton.Toggle("link.button.arrow.end");
        
        JLabel label = new JLabel("   Link: ");
        label.setFont(VueConstants.FONT_SMALL);
        addComponent(label);
        
        addComponent(mArrowStartButton);
        addComponent(mArrowEndButton);
        addComponent(mStrokeColorButton);
        addComponent(mStrokeButton);
        addComponent(mFontPanel);
        addComponent(mTextColorButton);

        final LWPropertyHandler arrowPropertyHandler =
            new LWPropertyHandler() {
                public Object getPropertyKey() { return LWKey.LinkArrows; }
                public Object getPropertyValue() {
                    int arrowState = LWLink.ARROW_NONE;
                    if (mArrowStartButton.isSelected())
                        arrowState |= LWLink.ARROW_EP1;
                    if (mArrowEndButton.isSelected())
                        arrowState |= LWLink.ARROW_EP2;
                    return new Integer(arrowState);
                }
                public void setPropertyValue(Object o) {
                    int arrowState = ((Integer)o).intValue();
                    mArrowStartButton.setSelected((arrowState & LWLink.ARROW_EP1) != 0);
                      mArrowEndButton.setSelected((arrowState & LWLink.ARROW_EP2) != 0);
                }
            };

        // todo arch: could create single abstract class for handling both
        // above property handler and the actionPerfomed propertyChange
        // initiating code here:
        ActionListener arrowButtonActionListener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    Object oldValue = new Integer(0); // how to get this?
                    Object newValue = arrowPropertyHandler.getPropertyValue();
                    propertyChange(new PropertyChangeEvent(ae.getSource(), LWKey.LinkArrows, oldValue, newValue));
                }
            };

        addPropertyProducer(arrowPropertyHandler);
        mArrowStartButton.addActionListener(arrowButtonActionListener);
        mArrowEndButton.addActionListener(arrowButtonActionListener);
    }
     
    protected void initDefaultState() {
        LWLink link = LWLink.setDefaults(new LWLink());
        mDefaultState = VueBeans.getState(link);
    }
 	
    public static void main(String[] args) {
        System.out.println("LinkToolPanel:main");
        VUE.initUI(true);
        VueUtil.displayComponent(new LinkToolPanel());
    }
}

             
    /*
    public void actionPerformed(ActionEvent pEvent) {
        Object source = pEvent.getSource();
 		
        if (source instanceof JToggleButton) {
            JToggleButton button = (JToggleButton) source;
            if (button == mArrowStartButton || button == mArrowEndButton) {
                Object oldValue = new Integer(0); // how to get this?
                Object newValue = mArrowPropertyHandler.getPropertyValue();
                propertyChange(new PropertyChangeEvent(button, LWKey.LinkArrows, oldValue, newValue));
            }
        }
    }
    */
 	
 	
         /*
         getBox().add(label);
         getBox().add(mArrowStartButton);
         getBox().add(mArrowEndButton);
         getBox().add(mStrokeColorButton);
         getBox().add(mStrokeButton);
         getBox().add(mFontPanel);
         getBox().add(mTextColorButton);
         */

     /*
     void loadValues(Object pValue) {
         super.loadValues(pValue);
         if (!(pValue instanceof LWLink))
             return;
 		
         setIgnorePropertyChangeEvents(true);

         // ick: we're relying on the side-effect of mState having been set in parent call
         // TODO: either force everything to use a loadValues(state), or have a loadValues(LWComponent)
         // and loadValues(VueBeanState), or perhaps get rid of the hairy-ass VueBeanState crap
         // alltogether.
         if (mState.hasProperty(LWKey.LinkArrows)) {
             int arrowState = mState.getIntValue(LWKey.LinkArrows);
             mArrowStartButton.setSelected((arrowState & LWLink.ARROW_EP1) != 0);
               mArrowEndButton.setSelected((arrowState & LWLink.ARROW_EP2) != 0);
         } else
             System.out.println(this + " missing arrow state property in state");
 		
         setIgnorePropertyChangeEvents(false);
     }
     */
         /*
         Color [] linkColors = VueResources.getColorArray( "linkColorValues");
         String [] linkColorNames = VueResources.getStringArray( "linkColorNames");
         mLinkColorButton = new ColorMenuButton( linkColors, linkColorNames, true);
         ImageIcon fillIcon = VueResources.getImageIcon("linkFillIcon");
         BlobIcon fillBlob = new BlobIcon();
         fillBlob.setOverlay( fillIcon );
         mLinkColorButton.setIcon(fillBlob);
         mLinkColorButton.setPropertyName(LWKey.StrokeColor);
         mLinkColorButton.setBorderPainted(false);
         mLinkColorButton.setMargin(ButtonInsets);
         mLinkColorButton.addPropertyChangeListener( this);
         */
