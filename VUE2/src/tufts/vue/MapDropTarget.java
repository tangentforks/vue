/*
 * Copyright 2003-2007 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import tufts.Util;
import tufts.vue.gui.GUI;
import tufts.vue.NodeTool.NodeModeTool;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.Image;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.*;

import java.io.File;
import java.io.FileInputStream;

import static java.awt.dnd.DnDConstants.*;

import java.net.*;

/**
 * Handle the dropping of drags mediated by host operating system onto the map.
 *
 * We currently handling the dropping of File lists, LWComponent lists,
 * Resource lists, and text (a String).
 *
 * @version $Revision: 1.92 $ / $Date: 2008-04-28 06:56:29 $ / $Author: sfraize $  
 */
class MapDropTarget
    implements java.awt.dnd.DropTargetListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MapDropTarget.class);

    private static final boolean DropImagesAsNodes = true;

    private static final int DROP_FILE_LIST = 1;
    private static final int DROP_NODE_LIST = 2;
    private static final int DROP_RESOURCE_LIST = 3;
    private static final int DROP_TEXT = 4;
    private static final int DROP_ONTOLOGY_TYPE = 5;

    
    public static final int ALL_DROP_TYPES =
        DnDConstants.ACTION_COPY        // 0x1
        | DnDConstants.ACTION_MOVE      // 0x2
        | DnDConstants.ACTION_LINK      // 0x40000000
        ;
        
    public static final int ACCEPTABLE_DROP_TYPES =
        DnDConstants.ACTION_COPY        // 0x1
        //| DnDConstants.ACTION_MOVE      // 0x2
        | DnDConstants.ACTION_LINK      // 0x40000000
        ;

    //public static final DataFlavor URLDataFlavor = GUI.makeDataFlavor(java.net.URL.class);
    //public static final DataFlavor URLDataFlavor = GUI.makeDataFlavor("application/x-java-url");
    
    // Calls to acceptDrag / rejectDrag do absolutely nothing as far as I can tell
    // (at last on MacOSX).  The cursor certianly doesn't change, which is what we
    // want (to make the "copy" cursor show).  Apparently the drag & drop code gets
    // a major overhaul in Java 1.6 (Mustang), and I can see why.
        
    // [ OLD COMMENT ] Do NOT include MOVE, or dragging a URL from the IE address bar
    // becomes a denied drag option!  Even dragged text from IE becomes disabled.  FYI,
    // also, "move" doesn't appear to actually ever mean delete original source.
    // Putting this in makes certial special windows files "available" for drag (e.g., a
    // desktop "hard" reference link), yet there are never any data-flavors available to
    // process it, so we might as well indicate that we can't accept it.

    private boolean CenterNodesOnDrop = true;
    
    private final MapViewer mViewer;

    /** for Windows, track the last dropAction we got during dragOver, as it can somehow be reverted by
     * the time we get the drop event */
    private int dropActionOverride = 0;

    public MapDropTarget(MapViewer viewer) {
       mViewer = viewer;
    }

    private void trackDrag(DropTargetDragEvent e) {

        // There's no point in doings this unless we want to request
        // a change for the default operation
        
//         if (DEBUG.DND && DEBUG.META) out("acceptDrag " + e);
//         if (Util.isMacPlatform())
//             e.acceptDrag(ALL_DROP_TYPES);
//         else
//             e.acceptDrag(ACCEPTABLE_DROP_TYPES);
        // This may be helping, er, sometimes...
        //mViewer.setTopCursor(DragSource.DefaultCopyDrop);
    }

    /** DropTargetListener */
    public void dragEnter(DropTargetDragEvent e)
    {
        if (DEBUG.DND) out("dragEnter " + GUI.dragName(e));
        trackDrag(e);
    }
    /** DropTargetListener */
    public void dragExit(DropTargetEvent e) {
        if (DEBUG.DND) out("dragExit " + e);
    }
    /** DropTargetListener */
    public void dropActionChanged(DropTargetDragEvent e) {
        if (DEBUG.DND) out("dropActionChanged: " + GUI.dragName(e));
        dragOver(e); // in case action type has changed and we want to change indication color
        //trackDrag(e);
    }
    /** DropTargetListener */
    public void dragOver(DropTargetDragEvent e)
    {
        if (DEBUG.DND && DEBUG.META) out("dragOver " + GUI.dragName(e));

        final boolean isSetResourceAction = (e.getDropAction() == DnDConstants.ACTION_LINK);

        final LWComponent over = mViewer.pickDropTarget(dropToMapLocation(e.getLocation()), null, isSetResourceAction);

        if (over != null)
            mViewer.setIndicated(over, isSetResourceAction);
        else
            mViewer.clearIndicated();

        if (Util.isWindowsPlatform()) {
            // this is being "forgot" on WinXP sometimes between now
            // and when they let go of the mouse!
            dropActionOverride = e.getDropAction();
        } else {
            dropActionOverride = 0;
        }

        /*
          // HANDLE IN TRAVERSAL!
          
        if (over instanceof LWNode || over instanceof LWLink) {
            // todo: if over resource icon and we can set THAT indicated, do
            // so and also use that to indicate we'd like to set the resource
            // instead of adding a new child
            mViewer.setIndicated(over);
        } else
            mViewer.clearIndicated();

        */

        trackDrag(e);
    }
    
    /** DropTargetListener */
    public void drop(DropTargetDropEvent e)
    {
        if (DEBUG.DND) out(Util.TERM_GREEN + "\nDROP: " + e 
                           + "\n\t        dropAction: " + dropName(e.getDropAction())
                           + "\n\tdropActionOverride: " + dropName(dropActionOverride)
                           + "\n\t     sourceActions: " + dropName(e.getSourceActions())
                           + "\n\t          location: " + e.getLocation()
                           + Util.TERM_CLEAR
                           );

        /* UnsupportedOperation (tring to discover key's being held down ourselves) try {
            System.out.println("caps state="+mViewer.getToolkit()
                               .getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK));
                               } catch (Exception ex) { System.err.println(ex); }*/

        
        if (e.getDropAction() == ACTION_NONE) {
            final int sa = e.getSourceActions();
            int accept;
            if ((sa & ACTION_COPY_OR_MOVE) != 0)
                accept = ACTION_COPY;
            else if ((sa & ACTION_LINK) != 0)
                accept = ACTION_LINK;
            else
                accept = ACTION_COPY;
            if (DEBUG.Enabled) out("DROP ACTION NONE: ACCEPTING AS: " + dropName(accept));
            e.acceptDrop(accept);
        } else
            e.acceptDrop(e.getDropAction());
        
        // Scan thru the data-flavors, looking for a useful mime-type
        boolean success =
            processTransferable(e.getTransferable(), e);

        e.dropComplete(success);
        
        mViewer.clearIndicated();        
    }

    // TODO: to cleanup the dropping onto hit/hitParent v.s. dropping into the focal in
    // all the below code, have a single drop.hit that is allowed to take on the value
    // of the focal (e.g., the LWMap, or a master slide, or any slide in the slide
    // viewer).  Always assume you may need to set the coords on new children, and do
    // that, and if whatever the new children are added to wants to re-lay them out,
    // fine.  Then all you need to do is be able to distinguish between something you're
    // allowed to set a resource on...  I supposed a boolean LWComponent.takesResource()
    // could tell us this (off for LWSlide, LWMap, LWGroup, LWText?), tho before doing
    // that, if we manage a fully dynamic property system, that might handle it for us.
    
//     private static class DropData {
//         final Transferable transfer;

//         DataFlavor flavor;
//         Object data;

//         URL mainURL;
//         URL iconURL;
//         URL contextURL; // referrer page for search-engine light-tray results that provide them
//         URL searchURL; // search engine this was found at
//         List list;
//         String text;
        
//         DropData(Transferable t) {
//             transfer = t;
//         }

// //         void select(DataFlavor pickFlavor) {
// //             this.flavor = pickFlavor;
// //             this.data = extractData(transfer, pickFlavor);
// //         }
        
//     }

//     private static final Collection<DropHandler> DropHandlers = new java.util.ArrayList();

//     private abstract static class DropHandler<T> {

//         final DataFlavor flavor;

//         T data;
        
//         DropHandler(DataFlavor f) {
//             flavor = f;
//             DropHandlers.add(this); // not threadsafe
//         }

//         // override for anything more complicated
//         boolean accept(DropContext drop) {
//             return drop.transfer.isDataFlavorSupported(flavor);
//         }

//         void processDrop(DropContext drop) {
//             Object data = extractData(drop.transfer, flavor);
//             process(drop, data); // just set data in drop context?
//         }
        
//         abstract void process(DropContext drop, Object data);
//     }
    
//     static {

//         // may want to change impl to just adding anon inner class impls to a list...
//         // how handle priorities?
        
//         new DropHandler(edu.tufts.vue.ontology.ui.TypeList.DataFlavor) {
// //             boolean accept(DropContext drop) {
// //                 return drop.dropAction == DnDConstants.ACTION_LINK && super.accept(drop);
// //             }
    
//             void process(DropContext drop, Object data) {
//                 //private boolean processDroppedOntologyType(DropContext drop,Object foundData)
//                 edu.tufts.vue.metadata.VueMetadataElement ele = new edu.tufts.vue.metadata.VueMetadataElement();
//                 ele.setObject(data);
//                 drop.hit.getMetadataList().getMetadata().add(ele);
//             }
//         };


//         new DropHandler(LWComponent.DataFlavor) {
//             //private boolean processDroppedNodes(DropContext drop)
//             void process(DropContext drop, Object data) {
//                 final List<LWComponent> items = (List) data;
        
//                 // now add them to the map
                
//                 // Always to the set center, in case hitParent isn't something
//                 // that is going to auto-layout the new children
//                 setCenterAt(items, drop.location);
                
//                 if (drop.hitParent != null) {
//                     drop.hitParent.addChildren(items);
//                 } else {
//                     drop.viewer.getFocal().addChildren(items);
//                 }
//                 drop.added.addAll(items);
//             }
//         };


// //         // createNode needs to be made static / moved to this drop handler
// //         new DropHandler(Resource.DataFlavor) {
// //             //private boolean processDroppedResourceList(DropContext drop)
// //             void process(DropContext drop, Object data)
// //             {
// //                 final List<Resource> items = (List) data;

// //                 if (items.size() == 1 && drop.hit != null && drop.isLinkAction) {
            
// //                     // Only one item is in the list, and we've hit a component, and
// //                     // it's a link-action drop: replace the hit component resource
// //                     drop.hit.setResource(items.get(0));
            
// //                 } else {
            
// //                     for (Resource resource : items) {

// //                         if (drop.hitParent != null && !drop.isLinkAction) {
// //                             // create new node children of the hit node
// //                             //drop.hitParent.addChild(createNode(drop, resource, null));
// //                             drop.hitParent.addChild(createNode(drop, resource, drop.nextDropLocation()));
// //                         } else {
// //                             createNode(drop, resource, drop.nextDropLocation());
// //                         }
                        
// //                     }
// //                 }
// //             } // end process
// //         };

        
//     }
        
        
    private static class DropContext {
        final Transferable transfer;
        final Point2D.Float location;   // map location of the drop
        final MapViewer viewer;          // we dropped into this component
        final LWComponent hit;          // we dropped into this component
        final LWContainer hitParent;    // we dropped into this component, and it can take children
        final boolean isLinkAction;     // user kbd modifiers down produced LINK drop action

        // Data fields -- may not all be populated.
        //final Collection items;         // bag of Objects in the drop
        final List items;   // convience reference to items if it is a List
        final String text;              // only one of items+list or text

        //Object data;
        

        private float nextX;
        private float nextY;

        List added = new java.util.ArrayList(); // to track LWComponents added as a result of the drop
        
        DropContext(Transferable t,
                    Point2D.Float mapLocation,
                    MapViewer viewer,
                    List items,
                    String text,
                    LWComponent hit,
                    boolean isLinkAction)
        {
            this.transfer = t;
            this.location = mapLocation;
            this.viewer = viewer;
            this.items = items;
            this.text = text;
            this.hit = hit;

//             if (items instanceof java.util.List)
//                 list = (List) items;
//             else
//                 list = null;
            
            if (hit != null && hit.supportsChildren())
                hitParent = (LWContainer) hit;
            else
                hitParent = null;

            this.isLinkAction = isLinkAction;

            nextX = mapLocation.x;
            nextY = mapLocation.y;

            if (DEBUG.DND) System.out.println(  "DropContext: loc: " + Util.fmt(mapLocation)
                                              + "\n             hit: " + hit
                                              + "\n       hitParent: " + hitParent
                                              );
        }

        Point2D nextDropLocation() {
            Point2D p = new Point.Float(nextX, nextY);
            // todo: either track height of last item created on drop,
            // so can adjust for actual height, or just "make-column"
            // on the whole drop after it's done.
            nextX += 15;
            nextY += 15;
            return p;
        }

        /**
         * Track top-level nodes created and added to map as we processed the drop.
         * Note that nodes are created and added to the map as the drop is processed in
         * case we fail we can get some partial results.  This lets us set the selection
         * to everything that was dropped at the end.
         */
        void add(LWComponent c) {
            added.add(c);
        }
    }

    private static final Object DATA_FAILURE = new Object();

    /** Extract data and auto-cast to the desired type.  If a type-mistmatch, reporting a warning and return null. */
    // Altho tho the class information is normally stored in the flavor via it's represenation,
    // we can't make use of generics to auto-cast and auto-report any errors w/out the 3rd explicit class object (type) argument.
    private static <A> A extractData(Transferable transfer, DataFlavor flavor, Class<A> clazz) {
        final Object data = extractData(transfer, flavor);
        if (clazz.isInstance(data)) {
           return clazz.cast(data);
        } else {
            Log.warn("Transfer data expecting type " + clazz + "; found: " + Util.tags(data));
            return null;
        }
    }
        
    private static Object extractData(Transferable transfer, DataFlavor flavor)
    {
        Object data = DATA_FAILURE;
        try {

            data = transfer.getTransferData(flavor);

        } catch (UnsupportedFlavorException ex) {
            
            Util.printStackTrace(ex, "TRANSFER: Transfer lied about supporting flavor "
                                 + "\"" + flavor.getHumanPresentableName() + "\" "
                                 + flavor
                                 );
            
        } catch (java.io.IOException ex) {
            
            Util.printStackTrace(ex, "TRANSFER: data no longer available");
            
        }
        
        return data;
    }


    private static final Pattern HTML_Fragment
        = Pattern.compile(".*<!--StartFragment-->(.*)<!--EndFragment-->",
                          Pattern.MULTILINE|Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
    private static final Pattern IMG_Tag
        = Pattern.compile(".*<img\\s+.*\\bsrc=\"([^\"]*)",
                          Pattern.MULTILINE|Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    /** @return the string matched by the first group in the given Pattern, or null if no match */
    private static final String extractText(Pattern pattern, String text) {
        final Matcher m = pattern.matcher(text);
        String s = null;
        if (m.lookingAt())
            return m.group(1);
        else
            return null;
    }
    

    /**
     * Process any transferrable: @param e can be null if don't have a drop event
     * (e.g., could use to process clipboard contents as well as drop events)
     * A sucessful result will be newly created items on the map.
     * @return true if succeeded
     */
    public boolean processTransferable(Transferable transfer, DropTargetDropEvent e)
    {
        Point dropLocation = null;
        Point2D.Float mapLocation = null;
        //Point2D.Float focalLocation = null;
        int dropAction = DnDConstants.ACTION_COPY; // default action, in case no DropTargetDropEvent

        // On current JVM's on Mac and PC, default action for dragging a desktop item is
        // MOVE, and holding CTRL down (both platforms) changes action to COPY.
        // However, when dragging a link from Internet Explorer on Win2k, or Safari on
        // OS X 10.4.2, CTRL doesn't change drop action at all, SHIFT changes drop
        // action to NONE, and only CTRL-SHIFT changes action to LINK, which fortunately
        // is at least the same on the mac: CTRL-SHIFT gets you LINK.
        //
        // Note: In both Safari & IE6/W2K, dragging an image from within a web page will
        // NOT allow ACTION_LINK, so we can't change a resource that way on the mac.
        // Also note that Safari will give you the real URL of the image, where as at
        // least as of IE 6 on Win2k, it will only give you the image file from your
        // cache.  (IE does also give you HTML snippets as data transfer options, with
        // the IMG tag, but it gives you no base URL to add to the relative locations
        // usually named in IMG tags!)

        // Also: Dragging a URL from Safari address bar CLAIMS to support COPY & LINK
        // source actions, but drop action is fixed at COPY can never ba changed to LINK
        // no matter what modifier keys you hold down (MacOSX 10.4, JVM 1.5.0_06-93)
        
        if (e != null) {
            dropLocation = e.getLocation();
            if (dropActionOverride != 0)
                dropAction = dropActionOverride;
            else
                dropAction = e.getDropAction();
            mapLocation = dropToMapLocation(dropLocation);
            //focalLocation = dropToFocalLocation(dropLocation);
            //if (DEBUG.DND) out(Util.TERM_GREEN + "processTransferable: " + GUI.dropName(e) + Util.TERM_CLEAR
            if (DEBUG.DND) out("processTransferable: " + GUI.dropName(e)
                               + "\n\t        dropAction: " + dropName(e.getDropAction())                               
                               + "\n\tdropActionOverride: " + dropName(dropActionOverride)
                               + "\n\t     dropScreenLoc: " + Util.fmt(dropLocation)
                               + "\n\t        dropMapLoc: " + Util.fmt(mapLocation)
                               //+ "\n\t      dropFocalLoc: " + Util.fmt(focalLocation)
                               );
        } else {
            if (DEBUG.DND) out("processTransferable: (no drop event) transfer=" + transfer);
        }

        final boolean isLinkAction = (dropAction == DnDConstants.ACTION_LINK);
        
        LWComponent dropTarget = null;
        Point2D.Float hitLocation = null;


        if (dropLocation != null) {
            dropTarget = mViewer.pickDropTarget(mapLocation, null, isLinkAction);
            if (DEBUG.DND) out("dropTarget=" + dropTarget + " in " + mViewer);
            if (dropTarget != null) {
                if (!dropTarget.supportsChildren() && !isLinkAction) {
                    
                    // this SHOULD be preventing drops onto MapSlides, but dropTarget is
                    // coming back null because pickDropTarget is checking
                    // supportsChildren itself (and returning null) -- we might want to
                    // have a special DROP_DENIED return value from pickDropTarget, or
                    // change semantics to return NULL only when denied, and return the
                    // actual map/focal we want to hit/added to, but that value thread
                    // down through the DropContext to tons of code below that we need
                    // to check.  In any case, code down below denying based on the
                    // focal being non-map when there's no target found handles this for
                    // now.
                    
                    if (DEBUG.DND) out("dropTarget: doesn't support children: " + dropTarget);
                    return false;
                }
                hitLocation = mapToLocalLocation(mapLocation, dropTarget);
                if (DEBUG.DND) out("dropTarget hit location: " + Util.fmt(hitLocation));
            } else {
                // drop target is null
                if (mViewer.getFocal() instanceof LWMap == false) {
                    // this prevents drops on MapSlides, and off-slide when real slides are the focal
                    if (DEBUG.DND) out("null dropTarget: default drop denied to non-map focal");
                    return false;
                }
            }
            /*
              // handle via traversal picking code:
            if (dropTarget instanceof LWImage) { // todo: does LWComponent accept drop events...
                if (DEBUG.DND) out("dropHit=" + dropTarget + " (ignored)");
                dropTarget = null;
            } else 
                if (DEBUG.DND) out("dropHit=" + dropTarget);
            */
        } else {
            // if no drop location (e.g., we did a "Paste") then assume where
            // they last clicked.
            dropLocation = mViewer.getLastMousePressPoint();
            mapLocation = dropToFocalLocation(dropLocation);
        }

        if (hitLocation == null)
            hitLocation = mapLocation;
            
            
        DataFlavor foundFlavor = null;
        Object foundData = null;
        String dropText = null;
        List dropItems = null;
        
        int dropType = 0;

        if (DEBUG.DND && DEBUG.META) dumpFlavors(transfer);

        // BTW, we could wait till after we check for all the local flavors which always take precedence
        // before we bother to scan for these.

        final DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
        final DataFlavor URLFlavor = findFlavor(dataFlavors, "application/x-java-url", java.net.URL.class);
        final DataFlavor HTMLTextFlavor = findFlavor(dataFlavors, "text/html", java.lang.String.class);

//         DataFlavor URLDataFlavor = null;
//         try {
//             URLDataFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
//             if (transfer.isDataFlavorSupported(URLDataFlavor))
//                 Log.info("GENERIC URL DATA FLAVOR SUPPORTED");
//         } catch (Throwable t) {
//             Util.printStackTrace(t);
//         }
        
        
        URL found_HTTP_URL = null;

        // The fanciest we can ultimately do: search for text/html type that has
        // <!--StartFragment-->..., and pull <img src=RealImageSource> out, which is
        // especially handy for Wikipedia, and we'd stop trying to process
        // wiki/Image:Ship.jpg crap, which is actually an HTML page.  Can also pull out
        // title="foo" from <href> tag or alt="foo" from <img> tag.

        // AND, we can always scan the unicode string for a second line of text for a
        // title (firefox puts title info here).
 
        // And actually, if on Windows, prioritize text/html over local file list (espec
        // if size==1), == generically scanning for <!--StartFragment--> will pull out
        // an IMG tag also, allowing us to get the real URL, as opposed to a damn local
        // cache file...


        // TODO: ALWAYS PRE-EXTRACT ACTUAL URL OBJECTS from URLFlavor, as well as any
        // <img src=...> found in any fragment, as well as the unicode string flavor,
        // including any second line with title info, so we can compare/contrast and
        // make use of as needed below, as the logic is going to get pretty ad-hoc
        // hairy...

        // Also: split out the native types we can just check first w/out any
        // of the ad-hoc mess.  Split out into methods that take and populate
        // a drop-context, returning true if suceeded.

        
        if (HTMLTextFlavor != null) {

            // The MAIN reason we want to attempt the fragment is in case the stock
            // incoming URL is in fact a file reference to a local browser cache file,
            // which we're really not interested in. Consider only using the fragment if
            // the stock URL in fact points to a local file, when the fragment points to
            // an HTTP url, as sometimes the fragment is actually less useful data than
            // what's in the stock URL (e.g., google news images, tho there's another
            // special decoding opportunity there -- they appear to embed the original
            // source image as "imgurl=" in the query, tho w/out "http" at the front).
            // A notable reverse case is Wikipedia, where often the stock URL actually
            // points to an HTML page even tho it looks like an image link, but the
            // fragment points to the real uploaded image.
            
            final String htmlText = extractData(transfer, HTMLTextFlavor, String.class);
            if (htmlText != null) {
                //Log.debug("FOUND HTML TEXT [" + htmlText + "]");
                final String fragment = extractText(HTML_Fragment, htmlText);
                if (fragment != null) {
                    Log.debug("FOUND HTML FRAGMENT [" + fragment + "]");
                    final String imgSrc = extractText(IMG_Tag, fragment);
                    if (imgSrc != null) {
                        Log.debug("FOUND IMG SRC=[" + imgSrc + "]");
                        if (imgSrc != null && imgSrc.toLowerCase().startsWith("http")) {
                            URL url = null;
                            try {
                                url = new java.net.URL(imgSrc);
                            } catch (Throwable t) {
                                Log.debug("invalid URL: " + imgSrc + "; " + t);
                            }
                            found_HTTP_URL = url;
                        }
                    }
                }
            }
        }
       
        
        try {

            if (URLFlavor != null && found_HTTP_URL == null) {
                final URL url = extractData(transfer, URLFlavor, URL.class);
                if (url != null) {
                    if ("http".equals(url.getProtocol())) {
                        // we especially don't want file: URL's, as then we might
                        // try and process what is actually an entire list of locally
                        // dropped files as a single URL drop.
                        found_HTTP_URL = url;
                        Log.debug("FOUND HTTP URL FLAVOR/DATA: " + URLFlavor + "; URL=" + url);
                    }
                }
            }

            // We want to repeatedly do the casts below for each case
            // to make sure the data type we got is what we expected.
            // (Can be a problem if somebody creates a bad Transferable)
            
            if (transfer.isDataFlavorSupported(edu.tufts.vue.ontology.ui.TypeList.DataFlavor)
                && (dropAction == DnDConstants.ACTION_LINK))
            {
                dropType = DROP_ONTOLOGY_TYPE;
                foundData = extractData(transfer, edu.tufts.vue.ontology.ui.TypeList.DataFlavor);
                
            } else if (transfer.isDataFlavorSupported(LWComponent.DataFlavor)) {
                
                foundFlavor = LWComponent.DataFlavor;
                foundData = extractData(transfer, foundFlavor);
                dropType = DROP_NODE_LIST;
                dropItems = (List) foundData;
                
            } else if (transfer.isDataFlavorSupported(Resource.DataFlavor)) {
                
                foundFlavor = Resource.DataFlavor;
                foundData = extractData(transfer, foundFlavor);
                dropType = DROP_RESOURCE_LIST;
                dropItems = (List) foundData;
            
            } else if (found_HTTP_URL != null && !found_HTTP_URL.getHost().equals("images.google.com")) {
                // don't use fragment URL if standard URL was from google image light-tray, as
                // the fragment <img src=...> in this case is a reference to the internal google
                // image icon stored at google, and we want the original image source...

                dropType = DROP_TEXT;

                final String http_url = found_HTTP_URL.toString();

                if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    final String txt = extractData(transfer, DataFlavor.stringFlavor, String.class);
                    // If the found URL is the same as unicode string, but unicode string
                    // is longer but same at head, it may contain a newline with title info
                    // that can be parsed on processDroppedText (better: generically extract
                    // a "title" during this process and set/pass on in the drop context)
                    if (txt.length() > http_url.length() && txt.startsWith(http_url)) {
                        foundData = txt;
                        dropText = txt;
                        foundFlavor = DataFlavor.stringFlavor;
                    }
                }
                if (dropText == null) {
                    foundFlavor = URLFlavor;
                    foundData = found_HTTP_URL;
                    dropText = http_url;
                }

            } else if (transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {

                foundFlavor = DataFlavor.javaFileListFlavor;
                foundData = extractData(transfer, foundFlavor);
                dropType = DROP_FILE_LIST;
                dropItems = (List) foundData;

            } else if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                
                foundFlavor = DataFlavor.stringFlavor;
                foundData = extractData(transfer, foundFlavor);
                dropType = DROP_TEXT;
                dropText = (String) foundData;
            
            } else {
                if (DEBUG.Enabled) {
                    System.out.println("TRANSFER: found no supported dataFlavors");
                    dumpFlavors(transfer);
                }
                return false;
            }
        } catch (ClassCastException ex) {
            Util.printStackTrace(ex, "TRANSFER: Transfer data did not match expected type:"
                                 + "\n\tflavor=" + foundFlavor
                                 + "\n\t  type=" + foundData.getClass());
            return false;

        } catch (Throwable t) {
            Util.printStackTrace(t, "TRANSFER: data extraction failure");
            return false;
        }

        if (foundData == DATA_FAILURE)
            return false;

        if (DEBUG.Enabled) {
            String size = "";
            Object firstInBag = null;
            //String bagType0 = "";
            String bagEntry0 = "";
            if (foundData instanceof Collection) {
                Collection bag = (Collection) foundData;
                size = " (Collection size " + bag.size() + ")";
                if (bag.size() > 0) {
                    final Object o = bag.iterator().next();
                    //bagType0 =   "\n\ttype[0]: " + o.getClass();
                    firstInBag = o;
                    bagEntry0 =  "\n\tdata[0]: " + Util.tags(firstInBag);
                }
            }
            //Log.debug("TRANSFER: Found supported flavor \"" + foundFlavor.getHumanPresentableName() + "\""
            Log.debug(Util.TERM_CYAN + "\nTRANSFER: Found a supported DataFlavor; " //+ foundFlavor
                      + "\n\t flavor: " + foundFlavor
                      + "\n\tdataTag: " + Util.tag(foundData) + size
                      //+ bagType0
                      + "\n\tdataRaw: [" + foundData + "]"
                      + bagEntry0
                      + Util.TERM_CLEAR
                      );
        }

        DropContext drop =
            new DropContext(transfer,
                            //dropLocation,
                            //mapLocation,
                            //focalLocation,
                            hitLocation,
                            mViewer,
                            dropItems,
                            dropText,
                            dropTarget,
                            isLinkAction);

        boolean success = false;

        if (dropItems != null && dropItems.size() > 1)
            CenterNodesOnDrop = false;
        else
            CenterNodesOnDrop = true;

        try {
            switch (dropType) {

            case DROP_FILE_LIST:
                success = processDroppedFileList(drop);
                break;
            case DROP_NODE_LIST:
                success = processDroppedNodes(drop);
                break;
            case DROP_RESOURCE_LIST:
                success = processDroppedResourceList(drop);
                break;
            case DROP_TEXT:
                success = processDroppedText(drop);
                break;
            case DROP_ONTOLOGY_TYPE:
                success = processDroppedOntologyType(drop,foundData);
                break;

            default:
                // should never happen
                throw new Error("unknown drop type " + dropType);
            }

            if (drop.added.size() > 0) {

                // Must make sure the selection is owned
                // by this map before we try and change it.
                // TODO: SlideViewer currently not handling this properly...
                mViewer.grabVueApplicationFocus("drop", null);
                
                // todo: would be cleaner to have viewer.getSelection(),
                // that could grab the vue app focus?
                VUE.getSelection().setTo(drop.added);

            }
            
        } catch (Throwable t) {
            Util.printStackTrace(t, "drop processing failed");
        }

        // Even if we had an exception during processing,
        // mark the drop in case there were partial results for Undo.
        
        mViewer.getMap().getUndoManager().mark("Drop");

        return success;
    }


    private boolean processDroppedText(DropContext drop)
    {
        if (DEBUG.DND) out("processDroppedText");
        
        // Attempt to make a URL of any string dropped -- if fails, just treat as
        // regular pasted text.  todo: if newlines in middle of string, don't do this,
        // or possibly attempt to split into list of multiple URL's (tho only if *every*
        // line succeeds as a URL -- prob too hairy to bother)

        String[] rows = drop.text.split("\n");
        URL foundURL = null;
        Map properties = new HashMap();
        
        if (rows.length < 3) {
            foundURL = makeURL(rows[0]);
            if (rows.length > 1) {
                // Current version of Mozilla (at least on Windows XP, as of 2004-02-22)
                // includes the HTML <title> as second row of text.
                // TODO: pass this on to the Resource factory or actually handle
                // this directly in the resource factory.
                properties.put("title", rows[1]);
            }
        }

        if (foundURL != null && foundURL.getQuery() != null && !drop.isLinkAction) {
            // if this URL is from a common search engine, we can find
            // the original source for the image instead of the search
            // engine's context page for the image.
            foundURL = decodeSearchEngineLightBoxURL(foundURL, properties);
        }
                
        if (foundURL != null) {

            boolean processed = true;
            boolean overwriteResource = drop.isLinkAction;
            
            if (drop.hit != null) {
                if (overwriteResource) {
                    // TODO: master slides in slide-viewer are "hit", thus we can set a resource on them this way!
                    drop.hit.setResource(foundURL.toString());
                    // TODO: clean this up:  resource should load meta-data on CREATION.
                    ((URLResource)drop.hit.getResource()).scanForMetaDataAsync(drop.hit);
                } else if (drop.hitParent != null) {
                    drop.hitParent.dropChild(createNodeAndResource(drop, null, foundURL.toString(), properties, drop.location));
                } else {
                    processed = false;
                }
            }
            
            if (drop.hit == null || !processed)
                createNodeAndResource(drop, null, foundURL.toString(), properties, drop.location);

        } else {
            // create a text node
            drop.add(createTextNode(drop.text, drop.location));
        }
        
        return true;
    }

    
    private boolean processDroppedNodes(DropContext drop)
    {
        if (DEBUG.DND) out("processDroppedNodes");
        
        // now add them to the map

        // We'd like to always to the set center, in case hitParent isn't something
        // that is going to auto-layout the new children
        if (CenterNodesOnDrop)
            setCenterAt(drop.items, drop.location);
        else
            setLocation(drop.items, drop.location);

        if (drop.hitParent != null) {
            drop.hitParent.dropChildren(drop.items);
        } else {
            mViewer.getFocal().dropChildren(drop.items);
        }
        drop.added.addAll(drop.items);
            
        return true;
    }

    private boolean processDroppedResourceList(DropContext drop)
    {
        if (DEBUG.DND) out("processDroppedResourceList");
        
        if (drop.items.size() == 1 && drop.hit != null && drop.isLinkAction) {
            
            // Only one item is in the list, and we've hit a component, and
            // it's a link-action drop: replace the hit component resource
            drop.hit.setResource((Resource)drop.items.get(0));
            
        } else {
            
            Iterator i = drop.items.iterator();
            while (i.hasNext()) {
                Resource resource = (Resource) i.next();
  //              System.out.println("Following resource has been dropped"+ resource);
                if (drop.hitParent != null && !drop.isLinkAction) {

                    // create new node children of the hit node
                    //drop.hitParent.addChild(createNode(drop, resource, null));
                    drop.hitParent.dropChild(createNode(drop, resource, drop.nextDropLocation()));
                
                } else {
                    
                    createNode(drop, resource, drop.nextDropLocation());
                }
            }
        }
        return true;
    }
    
    private boolean processDroppedOntologyType(DropContext drop,Object foundData)
    {
        if (DEBUG.DND) out("processDroppedType");
        
        edu.tufts.vue.metadata.VueMetadataElement ele = new edu.tufts.vue.metadata.VueMetadataElement();
        ele.setObject(foundData);
        drop.hit.getMetadataList().getMetadata().add(ele);

        edu.tufts.vue.metadata.ui.OntologicalMembershipPane.getGlobal().refresh();
        
        return true;
    }

    private boolean processDroppedFileList(DropContext drop)
    {
        if (DEBUG.DND) out("processDroppedFileList");
        
        Iterator i = drop.items.iterator();
        while (i.hasNext()) {
            
            processDroppedFile((File) i.next(), drop);
            
        }
        
        return true;
    }

    private void processDroppedFile(File file, DropContext drop)
    {
        String resourceSpec = file.getPath();
        String path = file.getPath();

        Map props = new HashMap();

        if (path.toLowerCase().endsWith(".url")) {
            // Search a windows .url file (an internet shortcut)
            // for the actual web reference.
            String url = convertWindowsURLShortCutToURL(file);
            if (url != null) {
                resourceSpec = url;

                // We compute the resource name here as it's coming from a short-cut
                // that we extract a URL from, in which case we want to use the name of
                // the original shortcut, and not compute the resource title from it's
                // source URL.
        
                String resourceName;

                if (file.getName().length() > 4)
                    resourceName = file.getName().substring(0, file.getName().length() - 4);
                else
                    resourceName = file.getName();

                props.put("title", resourceName);
                
            }
        } else if (path.endsWith(".textClipping")  || path.endsWith(".webloc") || path.endsWith(".fileloc")) {

            // TODO: we can handle Mac .fileloc's if we check multiple data-flavors: the initial LIST
            // flavor gives us the .fileloc, which we could even pull a name from if we want, and in
            // any case, a later STRING data-flavor actually gives us the source of the link!
            // SAME APPLIES TO .webloc files... AND .textClipping files
            // Of course, if they drop multple ones of these, we're screwed, as only the last
            // one gets translated for us in the later string data-flavor.  Oh well -- at least
            // we can handle the single case if we want.

            if (drop.transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {

                // Unforunately, this only works if there's ONE ITEM IN THE LIST,
                // or more accurately, the last item in the list, or perhaps even
                // more accurately, the item that also shows up as the application/x-java-url.
                // Which is why ultimately we'll want to put all this type of processing
                // in a full-and-fancy filing OSID to end all filing OSID's, that'll
                // handle windows .url shortcuts, the above mac cases plus mac aliases, etc, etc.

                String unicodeString;
                try {
                    unicodeString = (String) drop.transfer.getTransferData(DataFlavor.stringFlavor);
                    if (DEBUG.Enabled) out("*** GOT MAC REDIRECT DATA [" + unicodeString + "]");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // for textClipping, the string data is raw dropped text, for webloc,
                // it's URL be we already have text->URL in dropped text code below,
                // and same for .fileloc...  REFACTOR THIS MESS.

                //resourceSpec = unicodeString; 
                //resourceName = 
                // NOW WE NEED TO JUMP DOWN TO HANDLING DROPPED TEXT...
            }
        }
                
        //if (debug) System.out.println("\t" + file.getClass().getName() + " " + file);
        //if (hitComponent != null && fileList.size() == 1) {
                
        if (drop.hit != null) {
            // TODO: CONSOLODATE THE SET-RESOURCE CODE FROM ALL THE PROCESSING SUB-ROUTINES
            if (drop.isLinkAction || drop.hit instanceof LWLink) { // hack for now: if a link, just always set resource...
                drop.hit.setResource(resourceSpec);
            } else if (drop.hitParent != null) {
                drop.hitParent.dropChild(createNodeAndResource(drop, file, resourceSpec, props, drop.nextDropLocation()));
                // Why were we leaving out the location here?  Oh: when hitParent could only be a node (auto-layout), that made sense
                //drop.hitNode.addChild(createNodeAndResource(drop, resourceSpec, props, null));
            }
        } else {
            createNodeAndResource(drop, file, resourceSpec, props, drop.nextDropLocation());
        }
    }


    private LWComponent createNodeAndResource(DropContext drop, File file, String resourceSpec, Map properties, Point2D where)
    {
        //URLResource resource = new URLResource(resourceSpec);
        final Resource resource;
        if (file != null)
            resource = mViewer.getMap().getResourceFactory().get(file);
        else
            resource = mViewer.getMap().getResourceFactory().get(resourceSpec);

        if (DEBUG.DND) out("createNodeAndResource " + resourceSpec + " " + properties + " where=" + where);

        LWComponent c = createNode(drop, resource, properties, where, true);

        // TODO: get this so that one call is triggering the async stuff for both
        // meta-data and image loading.  Maybe the resource can will load the image...,
        // yeah, probably.  Tho we also need activate separate animation threads that
        // are going to wait on the data loading threads...

        // Establish an undo for the node creation, and then one for the title update,
        // so you can just undo to get the http title back if you want.  Will need to
        // make undo for at least this case or maybe all cases to treat the stopping of
        // thread as an undo action itself also so you can stop it in the middle?

        // How to handle group drops tho?  There will be a ton of threads.
        // I guess it would have to be all or nothing, and somehow group
        // ALL the loading threads under a single undo thread mark?
        
        // TODO: if an image, let async image loader do this so we don't
        // have two threads both pulling data from the same URL!

        // Could wait to start this till end of all drop processing
        // and pull it from drop.added

        //resource.scanForMetaDataAsync(c, true);

        return c;
    }

    private LWComponent createNode(DropContext drop, Resource resource, Point2D where) {
        return createNode(drop, resource, Collections.EMPTY_MAP, where, false);
    }
    
    private static int MaxNodeTitleLen = VueResources.getInt("node.title.maxDefaultChars", 50);
    
    private LWComponent createNode(DropContext drop,
                                          Resource resource,
                                          Map properties,
                                          Point2D where,
                                          boolean newResource)
    {
        if (DEBUG.DND) Log.debug(drop + "; createNode " + resource + " " + properties + " where=" + where);

        if (properties == null)
            properties = Collections.EMPTY_MAP;

        // can't currently generate link action when dragging an image from
        // a web browser.
        boolean dropImagesAsNodes =
            DropImagesAsNodes
            && !drop.isLinkAction
            && !(drop.hitParent instanceof LWSlide);

        LWComponent node;
        LWImage lwImage = null;
        String displayName = (String) properties.get("title");

        if (displayName == null)
            displayName = makeNodeTitle(resource);

        String shortName = displayName;

        if (shortName.length() > MaxNodeTitleLen)
            shortName = shortName.substring(0,MaxNodeTitleLen) + "...";

        /*
        MapResource mapResource = null;
        if (resource instanceof MapResource) { // todo: fix Resource so no more of this kind of hacking
            mapResource = (MapResource) resource;
        }
        */
        
        /*
         * To accomodate for the MapDisplay->Image Size preference, I needed to do this so that
         * when Image Size is set to Off Images aren't added to the node. MK
         */
        if (resource.isImage() && LWImage.getMaxRenderSize() > 0) {
            if (DEBUG.DND || DEBUG.IMAGE) Log.debug(drop + "; IMAGE DROP " + resource + " " + properties);
            //node = new LWImage(resource, viewer.getMap().getUndoManager());
            lwImage = new LWImage();
            String ws = (String) properties.get("width");
            String hs = (String) properties.get("height");
            if (ws != null && hs != null) {
                int w = Integer.parseInt(ws);
                int h = Integer.parseInt(hs);
                lwImage.setImageSize(w, h);
                resource.setProperty("image.width", ws);
                resource.setProperty("image.height", hs);
                /*
                if (mapResource != null) {
                    mapResource.setProperty("image.width", ws);
                    mapResource.setProperty("image.height", hs);
                }
                */
            }
            //lwImage.setLabel(displayName);
        }
        
        if (lwImage == null || dropImagesAsNodes) {
            if (false && where == null && lwImage != null) {
                // don't wrap image if we're about to drop it into something else
                node = lwImage;
            } else {
                node = NodeModeTool.createNode(shortName);
                if (lwImage != null)
                    ((LWNode)node).addChild(lwImage);
                node.setResource(resource);
            }
        } else {
            // we're dropping the image raw (either on map or into something else)
            node = lwImage;
        }

        // if "where" is null, the caller is adding this to another
        // existing node, so we don't add it to the map here
        // TODO: this is a confusing side-effect!
        // TODO: merge all hitParent v.s. not swtiches above into a unified hanlder case
        // FYI, this is now overdone!  We always provide the location now,
        // so on the slideviewer for master slide, where the slide is "hittable",
        // and looks like the parent, it's added to map first needlessly, then
        // reparented to where it needs to go.

        if (where != null)
            addNodeToFocal(node, where);

        if (lwImage != null) {
            // this will cause the LWImage to start loading the image
            lwImage.setResourceAndLoad(resource, mViewer.getMap().getUndoManager());
        } else if (newResource) {
            // if image, it will do this at end of loading
            ((URLResource)resource).scanForMetaDataAsync(node, true);
        }

        drop.add(node);

        return node;
    }

    private LWComponent createTextNode(String text, Point2D where)
    {
        return addNodeToFocal(NodeModeTool.createTextNode(text), where);
    }

    private LWComponent addNodeToFocal(LWComponent node, Point2D where)
    {
        if (CenterNodesOnDrop)
            node.setCenterAt(where);
        else
            node.setLocation(where);
        mViewer.getFocal().dropChild(node);
        return node;
    }



    //-----------------------------------------------------------------------------
    // support & debug code below
    //-----------------------------------------------------------------------------
    
    

    private static String dropName(int dropAction) {
        return GUI.dropName(dropAction);
    }
    
    // debug
    private void dumpFlavors(Transferable transfer) {
//         dumpFlavors(transfer.getTransferDataFlavors());
//     }
//     private void dumpFlavors(DataFlavor[] dataFlavors)
//     {
        final DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
        
        Log.debug("TRANSFERABLE: " + transfer + " has " + dataFlavors.length + " dataFlavors:");
        for (int i = 0; i < dataFlavors.length; i++) {
            DataFlavor flavor = dataFlavors[i];
            String name = flavor.getHumanPresentableName();
            if (flavor.getMimeType().toString().startsWith(name + ";"))
                name = "";
            else
                name = "\"" + name + "\"";
            System.out.format("flavor %2d %-16s %s", i, name, flavor.getMimeType());
            //System.out.println("\tflavor:" + flavor);
            try {
                Object data = transfer.getTransferData(flavor);
                System.out.println(" [" + data + "]");
                if (DEBUG.META) {
                    if (flavor.getHumanPresentableName().equals("text/uri-list"))
                        readTextFlavor(flavor, transfer);
                }
            } catch (Exception ex) {
                System.out.println("\tEXCEPTION: getTransferData: " + ex);
            }
        }
    }

    private DataFlavor findFlavor(DataFlavor[] dataFlavors, String mimeType, Class repClass)
    {
        for (DataFlavor flavor : dataFlavors) {
            //System.out.println("MT " + Util.tags(flavor.getMimeType()) + " REPCLASS " + Util.tags(flavor.getRepresentationClass()));
            if (flavor.isMimeTypeEqual(mimeType) && flavor.getRepresentationClass() == repClass)
                return flavor;
        }
        return null;
    }

    

    /** attempt to make a URL from a string: return null if malformed */
    private static URL makeURL(String s) {
        try {
            return new URL(s);
        } catch (MalformedURLException ex) {
            return null;
        }
    }


    /**
     * URL's dragged from the image search page of most search engines include query
     * fields that allow us to locate the original source of the image, as well as
     * width and height
     *
     * @param url a URL that at least know has a query
     * @param properties a map to put found properties into (e.g., width, height)
     */
    private static URL decodeSearchEngineLightBoxURL(final URL url, Map properties)
    {
        final String query = url.getQuery();
        // special case for google image search:
        if (DEBUG.IMAGE || DEBUG.IO || DEBUG.DND) Log.debug("DECODE QUERY: host " + url.getHost() + " query " + url.getQuery());

        Map data = VueUtil.getQueryData(query);
        //if (DEBUG.DND && DEBUG.META) {
        if (DEBUG.DND) {
            String[] pairs = query.split("&");
            for (int i = 0; i < pairs.length; i++) {
                System.out.println("\tquery pair " + pairs[i]);
            }
            //System.out.println("data " + data);
        }

        final String host = url.getHost();
        final String s = url.toString();
        final String urlWithoutQuery;

        final int questionMarkIndex = s.indexOf('?');

        if (questionMarkIndex > 0)
            urlWithoutQuery = s.substring(0, questionMarkIndex);
        else
            urlWithoutQuery = s;

                
        String imageURL = (String) data.get("imgurl"); // google & yahoo
        if (imageURL == null)
            imageURL = (String) data.get("image_url"); // Lycos & Mamma(who are they?)
        // note: as of Aug 2005, excite gives us no option
        if (imageURL == null && host.endsWith(".msn.com") || host.endsWith(".live.com"))
            imageURL = (String) data.get("iu"); // MSN search / Live Search
        if (imageURL == null && host.endsWith(".netscape.com"))
            imageURL = (String) data.get("img"); // Netscape search
        if (imageURL == null && host.endsWith(".ask.com"))
            imageURL = (String) data.get("u"); // ask jeeves, but only from their context page

        URL redirectURL = null;

        if (imageURL == null && host.endsWith(".flickr.com")) {
            // TODO: can try this trick with any URL: strip off the query and see if what's
            // left is an image url (e.g., file has an image extension, or could even
            // just try grabbing content to see if it has an image type: that would
            // be most reliable and generic version)
            try {
                return new URL(urlWithoutQuery);
            } catch (Throwable t) {
                return url;
            }
        }


        if (imageURL == null)
            imageURL = (String) data.get("url");

        // TODO: ask.com now has multiple levels of indirection of query pair sets
        // to get through...
        
        // Attempt a default
            
        if (imageURL != null
            && ("images.google.com".equals(host)
             || "search.live.com".equals(host) // microsoft
             || "images.search.yahoo.com".equals(host)
             || "rds.yahoo.com".equals(host) // old
             || "search.lycos.com".equals(host)
             || "tm.ask.com".equals(host)
             || "search.msn.com".equals(host)
             || "search.netscape.com".equals(host)
             || host.endsWith("mamma.com")
             )
            ) {


            imageURL = VueUtil.decodeURL(imageURL);

            //if (imageURL.indexOf('%') >= 0)
            //VueUtil.decodeURL(imageURL); // double-encoded (Ask Jeeves) -- need get query data AGAIN and get "imgsrc"
            //-------------------------------------------------------
            // %25 is % (percent), %2520 is an apparently often over-encoded
            // %20, that we can (and need) to bring back down to %20
            //imageURL = imageURL.replaceAll("%2520", "%20");
            //-------------------------------------------------------
            //imageURL = imageURL.replaceFirst("%3A", ":");
            //imageURL = imageURL.replaceAll("%2F", "/");
            //-------------------------------------------------------
                    
            if (DEBUG.IMAGE || DEBUG.IO || DEBUG.DND) Log.debug("redirect to image search url " + imageURL);
            if (imageURL.indexOf(':') < 0)
                imageURL = "http://" + imageURL;
            redirectURL = makeURL(imageURL);
            if (redirectURL == null && !imageURL.startsWith("http://"))
                redirectURL = makeURL("http://" + imageURL);
            if (DEBUG.IMAGE || DEBUG.IO || DEBUG.DND) Log.debug("redirect got URL " + redirectURL);
                    
            if (url != null) {
                String w = (String) data.get("w");              // Google & Yahoo
                String h = (String) data.get("h");
                if (w == null || h == null) {
                    w = (String) data.get("wd");                // MSN search
                    h = (String) data.get("ht");
                    if (w == null || h == null) {
                        w = (String) data.get("image_width");   // Lycos
                        h = (String) data.get("image_height");
                        if (w == null || h == null) {
                            w = (String) data.get("width");     // Mamma
                            h = (String) data.get("height");
                        }
                    }
                }
                if (w != null && h != null && properties != null) {
                    properties.put("width", w);
                    properties.put("height", h);
                }
            }
        }

        if (redirectURL == null)
            return url;
        else
            return redirectURL;
        
    }

    
    private static final Pattern URL_Line = Pattern.compile(".*^URL=([^\r\n]+).*", Pattern.MULTILINE|Pattern.DOTALL);

    private static String convertWindowsURLShortCutToURL(File file)
    {
        String url = null;
        try {
            if (DEBUG.DND) System.out.println("*** Searching for URL in: " + file);
            FileInputStream is = new FileInputStream(file);
            byte[] buf = new byte[2048]; // if not in first 2048, don't bother
            int len = is.read(buf);
            is.close();
            String str = new String(buf, 0, len);
            if (DEBUG.DND) System.out.println("*** size="+str.length() +"["+str+"]");
            Matcher m = URL_Line.matcher(str);
            if (m.lookingAt()) {
                url = m.group(1);
                if (url != null)
                    url = url.trim();
                if (DEBUG.DND) System.out.println("*** FOUND URL ["+url+"]");
                int i = url.indexOf("|/");
                if (i > -1) {
                    // odd: have found "file:///D|/dir/file.html" example
                    // where '|' is where ':' should be -- still works
                    // for Windows 2000 as a shortcut, but NOT using
                    // Windows 2000 url DLL, so VUE can't open it.
                    url = url.substring(0,i) + ":" + url.substring(i+1);
                    System.out.println("**PATCHED URL ["+url+"]");
                }
                // if this is a file:/// url to a local html page,
                // AND we can determine that we're on another computer
                // accessing this file via the network (can we?)
                // then we should not covert this shortcut.
                // Okay, this is good enough for now, tho it also
                // won't end up converting a bad shortcut, and
                // ideally that wouldn't be our decision.
                // [this is not worth it]
                /*
                URL u = new URL(url);
                if (u.getProtocol().equals("file")) {
                    File f = new File(u.getFile());
                    if (!f.exists()) {
                       url = null;
                        System.out.println("***  BAD FILE ["+f+"]");
                    }
                }
                */
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return url;
    }

    private Point2D.Float dropToFocalLocation(Point p)
    {
        return dropToFocalLocation(p.x, p.y);
    }

    private Point2D.Float mapToLocalLocation(Point2D.Float mapLocation, LWComponent local)
    {
        return (Point2D.Float) local.transformMapToZeroPoint(mapLocation, new Point2D.Float());
    }
    
    private Point2D.Float dropToFocalLocation(int x, int y)
    {
        final Point2D.Float mapLoc = (Point2D.Float) mViewer.screenToFocalPoint(x, y);
        //if (DEBUG.DND) out("dropToMapLocation " + x + "," + y + " = " + mapLoc);
        return mapLoc;
    }

    private Point2D.Float dropToMapLocation(Point p)
    {
        final Point2D.Float mapLoc =  mViewer.screenToMapPoint(p.x, p.y);
        //if (DEBUG.DND) out("dropToMapLocation " + x + "," + y + " = " + mapLoc);
        return mapLoc;
    }
    

    
    // TODO: this should be here: move to URLResource.java
    static String makeNodeTitle(Resource resource)
    {
        if (resource.getTitle() != null)
            return resource.getTitle();

        String title = resource.getProperty("title");
        if (title != null)
            return title;

        String spec = resource.getSpec();
        String name = Util.decodeURL(spec); // in case any %xx notations

        int slashIdx = name.lastIndexOf('/');  //TODO: fileSeparator? test on PC

        if (slashIdx == name.length() - 1) {
            // last char is '/'
            return name;
        } else {
            if (slashIdx > 0) {
                name = name.substring(slashIdx+1);

                // trim off extension if there is one
                int dotIdx = name.lastIndexOf('.');
                if (dotIdx > 0)
                    name = name.substring(0, dotIdx);

                name = name.replace('_', ' ');
                name = name.replace('.', ' ');
                name = name.replace('-', ' ');

                name = Util.upperCaseWords(name);
            }
        }

        //if (DEBUG.DND) out("MADE TITLE[" + name + "]");

        return name;
    }


    /**
     * Given a collection of LWComponent's, center them as a group at the given map location.
     */
    public static void setCenterAt(Collection<LWComponent> nodes, Point2D.Float mapLocation)
    {
        //if (DEBUG.DND) Log.debug("setCenterAt " + mapLocation + "; " + nodes);
        java.awt.geom.Rectangle2D.Float bounds = LWMap.getBounds(nodes.iterator());
        //java.awt.geom.Rectangle2D.Float bounds = LWMap.getLocalBounds(nodes);

        float dx = mapLocation.x - (bounds.x + bounds.width/2);
        float dy = mapLocation.y - (bounds.y + bounds.height/2);

        translate(nodes, dx, dy);
    }

    /**
     * Given a collection of LWComponent's, place the upper left hand corner of the group at the given location.
     */
    public static void setLocation(List<LWComponent> nodes, Point2D.Float mapLocation)
    {
        if (nodes.size() == 1) {
            if (nodes.get(0).getParent() == null)
                nodes.get(0).setLocation(mapLocation);
        } else {
        
            java.awt.geom.Rectangle2D.Float bounds = LWMap.getBounds(nodes.iterator());
            
            float dx = mapLocation.x - bounds.x;
            float dy = mapLocation.y - bounds.y;
            
            translate(nodes, dx, dy);
        }
    }

    private static void translate(Collection<LWComponent>nodes, float dx, float dy)
    {
        for (LWComponent c : nodes) {
            // If parent and some child both in selection and you drag (normally
            // only the parent get's selected), the child will have it's
            // location updated by the parent, so only set the location
            // on the orphans.
            if (c.getParent() == null)
                c.translate(dx, dy);
        }

    }

    private void out(String s) {
        Log.debug(s);
//         final String name;
//         if (mViewer.getFocal() != null)
//             name = mViewer.getFocal().getLabel();
//         else
//             name = mViewer.toString();
//         Log.debug(String.format("(%s): %s", name, s));
        //System.out.println("MapDropTarget(" + name + ") " + s);
    }
    
    

    private String readTextFlavor(DataFlavor flavor, Transferable transfer)
    {
        java.io.Reader reader = null;
        String value = null;
        try {
            reader = flavor.getReaderForText(transfer);
            if (DEBUG.DND && DEBUG.META) System.out.println("\treader=" + reader);
            char buf[] = new char[512];
            int got = reader.read(buf);
            value = new String(buf, 0, got);
            if (DEBUG.DND && DEBUG.META) System.out.println("\t[" + value + "]");
            if (reader.read() != -1)
                System.out.println("there was more data in the reader");
        } catch (Exception e) {
            System.err.println("readTextFlavor: " + e);
        }
        return value;
    }

    /*
        
    private void XcreateNewNode(java.awt.Image image, Point2D where)
    {
        // todo: query the NodeTool for current node shape, etc.
        LWNode node = NodeTool.createNode();
        node.setImage(image);
        node.setNotes(image.toString());

        addNodeToMap(node, where);
        
        /*
        String label = "[image]";
        if (image instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) image;
            label = "[image "
                + bi.getWidth() + "x"
                + bi.getHeight()
                + " type " + bi.getType()
                + "]";
            //System.out.println("BufferedImage: " + bi.getColorModel());
            // is null System.out.println("BufferedImage props: " + java.util.Arrays.asList(bi.getPropertyNames()));
            }*
    }

    private void createNewNode(Asset asset, java.awt.Point p) {
        String resourceTitle = "Fedora Node";
        Resource resource =new Resource(resourceTitle);
        try {
            resourceTitle = asset.getDisplayName();
             resource.setAsset(asset);
        } catch(Exception e) { System.out.println("MapDropTarget.createNewNode " +e ) ; }
      
       
        LWNode node = NodeTool.createNode(resourceTitle);
        node.setCenterAt(dropToMapLocation(p));
        node.setResource(resource);
        viewer.getMap().addNode(node);
    }

    */

    


    private static final String MIME_TYPE_MAC_URLN = "application/x-mac-ostype-75726c6e";
    // 75726c6e="URLN" -- mac uses this type for a flavor containing the title of a web document
    // this existed in 1.3, but apparently went away in 1.4.

    
    
}
