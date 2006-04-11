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
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * An icon for the preview of a given resource.
 *
 * TODO: merge common code with PreviewPane, and perhaps put in a 3rd class
 * so can have multiple icons referencing the same underlying image.
 *
 * @version $Revision: 1.5 $ / $Date: 2006-04-11 05:28:36 $ / $Author: sfraize $
 * @author Scott Fraize
 */

public class ResourceIcon
    implements javax.swing.Icon, Images.Listener, Runnable
{
    private final Image NoImage = VueResources.getImage("NoImage");
    
    private Resource mResource;
    private Object mPreviewData;
    private Image mImage;
    private int mImageWidth;
    private int mImageHeight;
    private boolean isLoading = false;
    private Component mPainter = null;

    private int mWidth = -1;
    private int mHeight = -1;

    //private final JLabel StatusLabel = new JLabel("(status)", JLabel.CENTER);


    /**
     *
     * This constructor will be needed if the icon is to be placed into a component that
     * uses cell renderers, such as JList, JTable, or JTree.  In the normal course,
     * ResourceIcon's repaint when their data is loaded, and to do so they remember the
     * component they're being painted on, and ask it to repaint when the data comes in.
     *
     * But this repaint doesn't work if what they're being asked to paint on is a renderer.
     *
     * The problem with renderer's is that "repaint" is overriden and defined as a
     * no-op, as they're only used to temporarily take on and draw their values when
     * they're asked to paint.
     *
     **/
    public ResourceIcon(Resource r, int width, int height, Component painter) {
        setSize(width, height);
        //StatusLabel.setVisible(false);
        //add(StatusLabel);
        //loadResource(r);
        mResource = r;
        mPainter = painter;
        //out("Constructed " + width + "x" + height + " " + r);
    }
        
    /** Act like a regular Icon */
    public ResourceIcon(Resource r, int width, int height) {
        this(r, width, height, null);
    }

    /** Expand's to fit the size of the component it's painted into */
    public ResourceIcon() {}

    public void setSize(int w, int h) {
        if (w != mWidth || h != mHeight) {
            mWidth = w;
            mHeight = h;
            repaint();
        }
    }

    private void repaint() {
        if (mPainter != null) {
            //if (DEBUG.IMAGE) out("repaint in " + GUI.name(mPainter));
            mPainter.repaint();
        }
        // FYI, this will not repaint if the parent is a DefaultTreeCellRenderer,
        // and the parent of the parent (a JLabel), is null, so we can't get that.
    }

    public int getIconWidth() { return mWidth; }
    public int getIconHeight() { return mHeight; }
    

    private void showLoadingStatus() {
        //StatusLabel.setText("Loading...");
        //StatusLabel.setVisible(true);
    }
    private void status(String msg) {
        //StatusLabel.setText("<HTML>"+msg);
        //StatusLabel.setVisible(true);
    }
    private void clearStatus() {
        //StatusLabel.setVisible(false);
    }
    

    synchronized void loadResource(Resource r) {

        if (DEBUG.RESOURCE || DEBUG.IMAGE) out("loadResource: " + Util.tag(r) + " " + r);
            
        mResource = r;
        if (r != null)
            mPreviewData = r.getPreview();
        else
            mPreviewData = null;
        mImage = null;

        if (mPreviewData == null && mResource.isImage())
            mPreviewData = mResource;

        loadPreview(mPreviewData);
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
            showLoadingStatus();
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
        status("Error: " + msg);
        isLoading = false;
    }

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

    public synchronized void run() {
        //loadPreview(mPreviewData);
        if (!isLoading)
            loadResource(mResource);
    }

    private static final double MaxZoom = 2.0;
    private static final boolean DrawBorder = false; // todo: doesn't handle cropped / squared off
    private static final int BorderWidth = 1; // width of border (todo: only works as 1)
    private static final int BorderGap = 1; // whitespace around drawn border
    private static final int BorderSpace = BorderWidth + BorderGap;

    private static final boolean CropToSquare = true;
    
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        final boolean expandToFit = (mWidth < 1);

        if (mPainter == null) {
            // note this means repaint updates would stop in a new parent,
            // tho assuming it's loaded by then, regular paints would work fine.
            mPainter = c;
        }
        
        if (DEBUG.IMAGE) out("paint, parent=" + GUI.name(c));

        if (DrawBorder && !expandToFit) {
            g.setColor(Color.gray);
            g.drawRect(x, y, mWidth-1, mHeight-1);
        }
            
        if (mImage == null) {

            if (!isLoading /*&& mPreviewData != null*/) {
                synchronized (this) {
                    if (!isLoading /*&& mPreviewData != null*/)
                        VUE.invokeAfterAWT(ResourceIcon.this); // load the preview
                }
            }
            g.setColor(Color.gray);
            g.drawRect(x, y, mWidth-1, mHeight-1);
            return;
        }

        int fitWidth, fitHeight;
        final Dimension maxImageSize;

        if (expandToFit) {
            // fill the given component
            fitWidth = c.getWidth();
            fitHeight = c.getHeight();
            maxImageSize = c.getSize();
        } else {
            // paint at our fixed size
            fitWidth = mWidth;
            fitHeight = mHeight;

            if (DrawBorder)
                maxImageSize = new Dimension(fitWidth - BorderSpace*2, fitHeight - BorderSpace*2);
            else
                maxImageSize = new Dimension(fitWidth, fitHeight);
            
            if (DEBUG.IMAGE) out("painting into " + GUI.name(maxImageSize));
        }

        double zoomFit;
        if (mImage == NoImage && expandToFit) {
            zoomFit = 1;
        } else {
            Rectangle2D imageBounds;
            if (CropToSquare) {
                // square off image, then fit in icon (todo: better; crop to icon)
                int smallestAxis = mImageWidth > mImageHeight ? mImageHeight : mImageWidth;
                imageBounds = new Rectangle2D.Float(0, 0, smallestAxis, smallestAxis);
            } else {
                // fit entire image in icon
                imageBounds = new Rectangle2D.Float(0, 0, mImageWidth, mImageHeight);
            }
            zoomFit = ZoomTool.computeZoomFit(maxImageSize,
                                              0,
                                              imageBounds,
                                              null,
                                              false);
            if (zoomFit > MaxZoom)
                zoomFit = MaxZoom;
        }
            

        final int drawW = (int) (mImageWidth * zoomFit + 0.5);
        final int drawH = (int) (mImageHeight * zoomFit + 0.5);
                                                     
        int xoff = x;
        int yoff = y;

        // center if drawable area is bigger than image
        if (drawW != fitWidth)
            xoff += (fitWidth - drawW) / 2;
        if (drawH != fitHeight)
            yoff += (fitHeight - drawH) / 2;

        Shape oldClip = null;
        if (CropToSquare && !expandToFit) {
            oldClip = g.getClip();            
            g.clipRect(x, y, mWidth, mHeight);
        }
            
        if (DEBUG.IMAGE) out("painting " + Util.tag(mImage) + " as " + drawW + "x" + drawH);
        g.drawImage(mImage, xoff, yoff, drawW, drawH, null);

        if (DEBUG.BOXES) {
            g.setColor(Color.green);
            ((Graphics2D)g).setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.2f));
            g.drawRect(x, y, mWidth-1, mHeight-1);
            ((Graphics2D)g).setComposite(java.awt.AlphaComposite.SrcOver);
        }
        
        if (CropToSquare && !expandToFit)
            g.setClip(oldClip);
    }

    private void out(String s) {
        System.out.println("ResourceIcon: " + s);
    }
        
    
}
