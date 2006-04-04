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

package tufts.vue.ui;

import tufts.Util;
import tufts.vue.*;
import tufts.vue.gui.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.*;

import java.awt.datatransfer.*;


/**
 * Display a preview of the selected resource.  E.g., and image or an icon.
 *
 * @version $Revision: 1.4 $ / $Date: 2006-04-04 04:56:42 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public class PreviewPane extends JPanel
    implements Images.Listener, Runnable
{
    private final Image NoImage = VueResources.getImage("NoImage");
    
    private static boolean FirstPreview = true;

    private Resource mResource;
    private Object mPreviewData;
    private Image mImage;
    private int mImageWidth;
    private int mImageHeight;
    private boolean isLoading = false;

    private final JLabel StatusLabel = new JLabel("(status)", JLabel.CENTER);
    //private final JTextArea StatusLabel = new JTextArea("(status)");
    //private final JTextPane StatusLabel = new JTextPane();
    // how in holy hell to get a multi-line text object centered w/out using a styled document?

    //private Image LoadingImage = null;
        
    PreviewPane() {
        super(new BorderLayout());
        setMinimumSize(new Dimension(32,32));
        setPreferredSize(new Dimension(200,200));
        setOpaque(false);

        //StatusLabel.setLineWrap(true);
        //StatusLabel.setAlignmentX(0.5f);
        //StatusLabel.setAlignmentY(0.5f);
        //StatusLabel.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        //StatusLabel.setBorder(new LineBorder(Color.red));
        StatusLabel.setVisible(false);
        add(StatusLabel);

        addMouseListener(new java.awt.event.MouseAdapter() {    
                public void mouseClicked(MouseEvent me){
                    if (mResource != null && me.getClickCount() == 2)
                        mResource.displayContent();
                }
            });
        
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {    
                public void mouseDragged(MouseEvent me){
                    if (mResource != null) {
                        GUI.startSystemDrag(PreviewPane.this, me, mImage, new GUI.ResourceTransfer(mResource));
                        // start caching the non-preview versiomn of the image if it's not already there
                        // todo: make this a low-priority disk-cache only, then if it's actually
                        // dropped, we can create the in-memory image
                        //Images.getImage(mResource, PreviewPane.this);
                        // need to set preview data so that when this comes back, we don't ignore it.
                        // Tho then we need to not clear existing thumbnail, but can put "loading" over it...
                    }
                }
            });
        
    }

    private void status(String msg) {
        StatusLabel.setText(msg);
        StatusLabel.setVisible(true);
    }
    private void clearStatus() {
        StatusLabel.setVisible(false);
    }
    

    synchronized void loadResource(Resource r) {

        if (DEBUG.RESOURCE || DEBUG.IMAGE) out("loadResource: " + r);
            
        mResource = r;
        if (r != null)
            mPreviewData = r.getPreview();
        else
            mPreviewData = null;
        mImage = null;

        if (mPreviewData == null && mResource.isImage())
            mPreviewData = mResource;

        if (isShowing()) {

            loadPreview(mPreviewData);
            FirstPreview = false;

        } else {

            if (FirstPreview && mPreviewData != null) {
                FirstPreview = false;
                Widget.setExpanded(PreviewPane.this, true);
                // Exposing the panel will cause repaint, which
                // will trigger a preview load.
            } else {
                if (DEBUG.RESOURCE || DEBUG.IMAGE) out("not showing: no action");
            }
        }

    }

    private void loadPreview(Object previewData)
    {
        // todo: handle if preview is a Component, 
        // todo: handle a String as preview data.

        if (false /*&& r.getIcon() != null*/) { // these not currently valid from Osid2AssetResource (size=-1x-1)
            //displayIcon(r.getIcon());
        } else if (previewData instanceof java.awt.Component) {
            out("TODO: handle Component preview " + previewData);
            displayImage(NoImage);
        } else if (previewData != null) { // todo: check an Images.isImageableSource
            loadImage(previewData);
        } else {
            displayImage(NoImage);
        }
    }

    // TODO: if this triggered from an LWImage selection, and LWImage had
    // an image error, also notify the LWImage of good data if it comes
    // in as a result of selection.

    private synchronized void loadImage(Object imageData) {
        if (DEBUG.IMAGE) out("loadImage " + imageData);

        // test of synchronous loading:
        //out("***GOT IMAGE " + Images.getImage(imageData));
        
        if (!Images.getImage(imageData, this)) {
            // will make callback to gotImage when we have it
            isLoading = true;
            status("Loading...");
        } else {
            // gotImage has already been called
            isLoading = false;
        }
    }


    /** @see Images.Listener */
    public synchronized void gotImageSize(Object imageSrc, int width, int height) {

        if (imageSrc != mPreviewData)
            return;
            
        mImageWidth = width;
        mImageHeight = height;
    }
    
    /** @see Images.Listener */
    public synchronized void gotImage(Object imageSrc, Image image, int w, int h) {

        if (imageSrc != mPreviewData)
            return;
            
        displayImage(image);
        isLoading = false;
    }
    /** @see Images.Listener */
    public synchronized void gotImageError(Object imageSrc, String msg) {

        if (imageSrc != mPreviewData)
            return;
            
        displayImage(NoImage);
        status("Image Error:\n" + msg);
        isLoading = false;
    }

    /*
      private void displayIcon(ImageIcon icon) {
      displayImage(icon.getImage());
      }
    */

    private void displayImage(Image image) {
        if (DEBUG.RESOURCE || DEBUG.IMAGE) out("displayImage " + Util.tag(image));

        mImage = image;
        if (mImage != null) {
            mImageWidth = mImage.getWidth(null);
            mImageHeight = mImage.getHeight(null);
            if (DEBUG.IMAGE) out("displayImage " + mImageWidth + "x" + mImageHeight);
        }

        clearStatus();
        repaint();
    }

    public void run() {
        loadPreview(mPreviewData);
    }

    private void out(String s) {
        System.out.println("PreviewPane: " + s);
    }
        

    /** draw the image into the current avilable space, scaling it down if needed (never scale up tho) */
    public void paintComponent(Graphics g)
    {
        if (DEBUG.IMAGE) out("paint");

        if (mImage == null) {
            if (!isLoading && mPreviewData != null) {
                synchronized (this) {
                    if (!isLoading && mPreviewData != null)
                        VUE.invokeAfterAWT(PreviewPane.this); // load the preview
                }
            }
            return;
        }
            
        //g.setColor(Color.black);
        //g.fillRect(0,0, w,h);

        double zoomFit;
        if (mImage == NoImage) {
            zoomFit = 1;
        } else {
            java.awt.geom.Rectangle2D imageBounds
                = new java.awt.geom.Rectangle2D.Float(0, 0, mImageWidth, mImageHeight);
            zoomFit = ZoomTool.computeZoomFit(getSize(),
                                              0,
                                              imageBounds,
                                              null,
                                              false);
            //if (zoomFit > 1)
            //    zoomFit = 1;
        }
            

        final int drawW = (int) (mImageWidth * zoomFit);
        final int drawH = (int) (mImageHeight * zoomFit);
                                                     
        final int w = getWidth();
        final int h = getHeight();
            
        // center if drawable area is bigger than image
        int xoff = 0;
        int yoff = 0;
        if (drawW != w)
            xoff = (w - drawW) / 2;
        if (drawH != h)
            yoff = (h - drawH) / 2;
            
        if (DEBUG.IMAGE) out("painting " + Util.tag(mImage));
        g.drawImage(mImage, xoff, yoff, drawW, drawH, null);
    }
}
