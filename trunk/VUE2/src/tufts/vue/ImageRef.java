package tufts.vue;

import tufts.Util;

import java.lang.ref.*;
import java.awt.Image;
import java.awt.Graphics2D;

import static tufts.vue.ImageRep.UNAVAILABLE;

public class ImageRef
{
    public static final boolean IMMEDIATE_ICONS = false;
    
    public static final int DEFAULT_ICON_SIZE = 128;
    public static final int[] ZERO_SIZE = ImageRep.ZERO_SIZE;

    //===================================================================================================

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ImageRef.class);
    private static final int PIXEL_THRESHOLD_FOR_ICON_GENERATION = (2*DEFAULT_ICON_SIZE) * (2*DEFAULT_ICON_SIZE);
    private static final int PIXEL_THRESHOLD_FOR_ICON_DRAWING = DEFAULT_ICON_SIZE*2;

    private static final boolean ICONS_ARE_DISPOSABLE = false; // todo: true case needs testing / may not work
    
    private static final String SIZE_FULL = "FULL-SIZE";
    private static final String SIZE_ICON = "ICON-SIZE";
    private static final String SIZE_UNKNOWN = "UNKNOWN-SIZE";
    
    private volatile ImageRep _full = ImageRep.UNAVAILABLE;
    private volatile ImageRep _icon = ImageRep.UNAVAILABLE;
    //private volatile Object _desired = SIZE_UNKNOWN;
    // _desired not used at moment -- would be easy to have one global instance of an ImageRef per image w/out it,
    // and could add back in this functionality by allowing a client to implement a simple recording API for desired
    // e.g., set/getDesired -- is only needed to prevent extra repaints when a new image rep arrives.
    
    private volatile ImageSource _source;

    private volatile float _aspect = 0;

    private Listener _repainter;
    
    public static interface Listener {
        public void imageRefChanged(Object cause);
    }

    public ImageRef(Listener listener) {
        _repainter = (Listener) listener;
    }
    
    public boolean isBlank() {
        return _source == null;
    }
    
    public void setImageSource(Object is) {
        //if (_source != null) throw new Error("ImageSource re-set not permitted: " + this);
        if (_source != is) {
            //-----------------------------------------------------------------------------
            // PROBLEM: if this is a local file, the URI cache key in the _source.key
            // will be null, meaning we can't later create an icon cache key from it.
            // Yet at the moment we're only seeing this as a problem if the local
            // file is missing -- so how is this being working in the regular case?
            //-----------------------------------------------------------------------------
            _source = ImageSource.create(is);
            //if (DEBUG.IMAGE) Log.debug("created image source " + _source + " from " + is);
            initReps();
        }
    }

    private static final boolean PRE_LOAD_ICONS = false;

    private void initReps() {

        if (DEBUG.IMAGE) debug("initReps");

        if (_icon != UNAVAILABLE || _full != UNAVAILABLE)
            throw new Error("re-init of reps");
        
        // As icons only exist in the cache, we know we don't have an icon
        // created yet if there isn't at least a unloaded cache entry for the icon.

        final java.net.URI iconKey = _source.getIconKey(DEFAULT_ICON_SIZE);

        if (iconKey != null) {
            if (PRE_LOAD_ICONS) {
                //synchronized (Images.getCacheLock()) {
                    // we do this in a cache-lock to ensure the cache entry can't have
                    // been GC'd or changed from the time we check for it to the time we
                    // kick a load for it.  Could we dead-lock? -- Ideally, we want to
                    // release the lock at the point getImage returns in
                    // ImageRep.reconstitute, otherwise it's ImageRef callback,
                    // notifyRepHasArrived, will also happen in the cache-lock, which is
                    // dangerous -- it will block all other image processing threads
                    // till it returns [todo: test/verify]
                    if (Images.hasCacheEntry(iconKey)) {
                        _icon = createPreLoadedIconRep(iconKey);
                        kickLoad(_icon);
                    }
                    //}
            } else {
                if (Images.hasCacheEntry(iconKey)) {
                    _icon = createPreLoadedIconRep(iconKey);
                }
            }
        } // _icon left as ImageRep.UNAVAILABLE

        // rep won't load until it attempts to draw:
        _full = ImageRep.create(this, _source);
    }
    
//     public void drawInto(Graphics2D g, float width, float height)
//     {
//         // need to always try, as this is how we'll know if the 
//         // the full is ever desired (when an icon has been pre-loaded)
//         drawBestAvailable(g, width, height);
//     }
    
    public void drawInto(DrawContext dc, float width, float height)
    {
        try {
            if (dc.isAnimating() || dc.isDraftQuality()) {
                drawAvailable(dc.g, width, height);
            } else {
                drawBestAvailable(dc.g, width, height);
            }
        } catch (Throwable t) {
            Log.error("exception painting " + this, t);
        }
    }

    private void drawAvailable(Graphics2D g, float width, float height) 
    {
        if (_icon.available())
            _icon.renderRep(g, width, height);
        else if (_full.available())
            _full.renderRep(g, width, height);
        else 
            ; //UNAVAILABLE.drawRep(g, width, height);
    }
    
    private static final java.awt.Color DebugRed = new java.awt.Color(255,0,0,128);
    private static final java.awt.Color DebugGreen = new java.awt.Color(0,255,0,128);
    private static final java.awt.Color DebugBlue = new java.awt.Color(0,0,255,128);
    private static final java.awt.Color DebugYellow = new java.awt.Color(255,255,0,128);

    private ImageRep pickRepToDraw(final ImageRep desired, final ImageRep backup) {
        if (desired.available()) // the most common case at runtime
            return desired; 
        else if (backup.available())
            return backup;
        else if (desired == backup) {
            // this is the common case at init
            if (desired != ImageRep.UNAVAILABLE) {
                // should never happen
                Log.error("desired == backup != UNAVAILABLE: " + desired, new Throwable("HERE"));
                reload();
            }
            return ImageRep.UNAVAILABLE;
        }
        else if (desired.loading())
            return desired;
        else if (backup.loading())
            return backup;
        else if (desired.hasError())
            return desired;
        else if (backup.hasError())
            return backup;
        else
            return ImageRep.UNAVAILABLE;
        
    }
    
    private void drawBestAvailable(Graphics2D g, float width, float height)
    {
        // We have two main tasks to accomplish here:
        // (1) pick the best representation available to draw
        // (2) start loading a better (or any) representation if we can
        
        final ImageRep backupRep;
        final ImageRep desiredRep;

        final double scale = g.getTransform().getScaleX();

        final int onScreenMaxDim;

        if (aspect() > 1f) {
            //debug("aspect="+aspect() + " picking width " + width);
            onScreenMaxDim = (int) (scale * width);
        } else {
            //debug("aspect="+aspect() + " picking height " + height);
            onScreenMaxDim = (int) (scale * height);
        }

        // We don't worry about coherency sync issues with _full & _icon here -- we should handle
        // whatever's thrown at us on a best-available basis. The only thing we rely on is they
        // should never be null.

        if (onScreenMaxDim <= PIXEL_THRESHOLD_FOR_ICON_DRAWING) {
            // todo: would be better to check actual iconRep size v.s. our constant,
            // tho this lets us not worry if it's been loaded or not
            desiredRep = _icon;
            backupRep = _full;
            //_desired = SIZE_ICON;
            //debug("onScreenMaxDim below thresh " + PIXEL_THRESHOLD_FOR_ICON_DRAWING + " at " + onScreenMaxDim);
        } else {
            //debug("onScreenMaxDim ABOVE thresh " + PIXEL_THRESHOLD_FOR_ICON_DRAWING + " at " + onScreenMaxDim);
            //_desired = SIZE_FULL;
            desiredRep = _full;
            backupRep = _icon;
        }

        final ImageRep drawRep = pickRepToDraw(desiredRep, backupRep);

        if (DEBUG.IMAGE && DEBUG.BOXES) {
            debug("  desired " + desiredRep);
            debug("   backup " + backupRep);
            debug("   toDraw " + drawRep);
        }
        
        if (!desiredRep.available() && desiredRep != UNAVAILABLE) {
            kickLoad(desiredRep);
        } else if (!drawRep.available()) {
            if (drawRep == UNAVAILABLE) { // if icon load failed, must create a new one (low memory) [NOT ENOUGH!]
                if (DEBUG.Enabled) debug("forcing full load");
                kickLoad(_full);
            } else if (drawRep == _icon && drawRep.hasError()) {
                //****************************************************************************************
                // if icon load failed, must create a new one (low memory) [TODO: NOT ENOUGH]
                //****************************************************************************************
                if (DEBUG.Enabled) debug("forcing full load on bad icon");
                kickLoad(_full);
            } else
                kickLoad(drawRep);
        }

        drawRep.renderRep(g, width, height); // before/after kickloads doesn't matter as long as reps don't auto-constitute

        if (DEBUG.BOXES) {
            final float hw = width / 2f;
            final float hh =  height / 2f;
            final java.awt.geom.Rectangle2D.Float r = new java.awt.geom.Rectangle2D.Float();
            if (drawRep == _icon) {
                // we're looking at the icon rep
                g.setColor(DebugYellow);
                r.setRect(0, 0, hw, hh);
                g.fill(r);
            }
            if (drawRep != desiredRep) {
                // we're waiting for a better rep
                g.setColor(DebugRed);
                r.setRect(0, hh, hw, hh);
                g.fill(r);
            }
            if (_full.available()) {
//                 // we've got the full rep loaded
//                 if (_full.isFading()) // was to check to Reference enequing
//                     g.setColor(DebugYellow);
//                 else
                    g.setColor(DebugBlue);
                r.setRect(hw, 0, hw, height);
                g.fill(r);
            }
            
            
        }
    }

    private void repaint() {
        _repainter.imageRefChanged("repaint");
    }

    public void notifyRepHasProgress(final ImageRep rep, final float pct) {
        repaint();
    }
    
    /** the ImageRep is done loading -- it has all the renderable image data */
    public void notifyRepHasArrived(final ImageRep freshRep, final Image hardImageRef)
    {
        if (_aspect == 0 || freshRep == _full)
            _aspect = freshRep.aspect(); // the one place aspect is loaded

        //if (_desired == freshRep || _desired == SIZE_UNKNOWN)  // may be easiest/safest just to always repaint
            repaint();

        if (freshRep == _full && _icon == ImageRep.UNAVAILABLE) {
            // no icon was previously generated -- look to see if
            // one has been generated elsewhere in this runtime,
            // or if not, and we need one, create it now.
            if (_full.area() > PIXEL_THRESHOLD_FOR_ICON_GENERATION) {
                _icon = createRuntimeScaledIconRep(freshRep, hardImageRef);
            } // else _icon left as ImageRep.UNAVAILABLE
             
        }
    }

    private ImageRep createPreLoadedIconRep(java.net.URI cacheKey)
    {
        return ImageRep.create(this,
                               ImageSource.create(cacheKey),
                               ICONS_ARE_DISPOSABLE);
    }
    

    private ImageRep createRuntimeScaledIconRep(final ImageRep full, final Image hardFullImageRef) {
        // could pass in something like Scaler with just a produceIcon method into the image source
        // for creating the icon, or a general FutureTask.

        // NOTE: if the icon is NOT created immediately, and we're in the middle of loading lots of
        // images, that hard image reference is going to stay around, held by the ImageSource in an
        // Images IconTask, in the the thread-pool task queue, unable to be GC'd, which will lead
        // to contention that's very difficult to recover from should we start running out of
        // memory.

        // Although an incompletely impl, Images.DELAYED_ICONS uses the full ImageRep into the
        // icon-source ImageSource instead of the hard image reference, and could attempt to
        // reconstitute it if it's been GC'd once the IconTask get's around to running.  That
        // implies a bunch more complexity to the code.  We're going with simpler and more reliable
        // for now.

        final ImageRep icon = ImageRep.create(this,
                                              ImageSource.createIconSource(_source, full, hardFullImageRef, DEFAULT_ICON_SIZE),
                                              ICONS_ARE_DISPOSABLE);

        // For ideal memory usage, we'd create the icon immediately in this thread, but we don't
        // actually want to do this: if there are multiple Ref's to the same content, they'll all
        // be in the listener-relay chain, but the FIRST one to get this callback is going to hang
        // up the rest of the thread while generating the icon if we request an immediate load, and
        // the down-relay ImageRef's, which could at least draw the full-rep while waiting, will be
        // waiting until the icon generation is done.  Furthermore: this will trigger callbacks
        // with icon data to nodes, THEN the backed up relay's will fire, making it look like the
        // full rep has arrived after the icon, which explains why when we tried this some of the
        // images in our repeats test are displaying the full image AFTER it's been generated, tho
        // that gets fixed on the first repaint after the updates.

        // The issue of generating icons sooner rather than later is how handled
        // in Images by giving higher priority to icon generating tasks than image loading tasks,
        // and by keeping a hard-ref to the image in the ImageSource.

        kickLoad(icon);

        return icon;
        
     }
    
    private void kickLoad(ImageRep rep) {
        if (DEBUG.IMAGE) debug(" kickLoad " + rep);
        //if (DEBUG.IMAGE && rep == _full) Log.debug("FULL REP LOAD " + rep, new Throwable("HERE"));
        rep.reconstitute();
    }
    private void requestImmediateLoad(ImageRep rep) {
        if (DEBUG.IMAGE) debug("IMMEDIATE " + rep);
        rep.reconstituteNow();
    }

    void preLoadFullSize() {
        kickLoad(_full);
    }

    private void debug(String s) {
        Log.debug(String.format("%08x[%s] %s", System.identityHashCode(this), debugSRC(_source), s));
    }
    static String debugSRC(ImageSource s)
    {
        if (s == null)
            return "[null ImageSource]";
        else
            return s.debugName();
    }

    public boolean available() {
        return _icon.available() || _full.available();
    }

    public boolean hasError() {
        return _full.hasError();
    }

    public float aspect() {
        return _aspect;
    }

    public int[] fullPixelSize() {
        return _full.size();
    }

    public void reload() {
        _full = ImageRep.UNAVAILABLE;
        _icon = ImageRep.UNAVAILABLE;
        repaint();
    }
    
    @Override public String toString() {
        //return "ImageRef[full=" + fullRep() + "; icon=" + iconRep() + "; src=" + _source + "]";
        
        return String.format("ImageRef[full=%s icon=%s]", _full, _icon.handle());
        //return String.format("ImageRef[full: %s\n\ticon: %s\n\tsrc: %s]", _full, _icon, _source);
    }

}

// ALTERNATIVE DESIGN: Images.getImageRef returns an ImageRef, so that multiple
// LWImage's, preview panes, etc, are all pointing to the same ImageRef.  Which means
// the cache would always contain instances of ImageRef?  Tho that conflicts with the
// idea of keeping the cache code very clean & flat: e.g., only knows about 1 key, 1
// file, and the the icon cache file generation would get handled in ImageRef.  Tho
// actually that really doesn't need to be -- Images handles fetching and caching image
// data -- if it creates icons behind the scenes, that's an impl detail.

// If we instance the ImageRef ourselves, that is more amenable to subclassing and just
// overriding the update methods we'd like -- that could be very handy.  That would work
// here and in ResourceIcon, both which always work with a single image.  But
// PreviewPane works w/multiple images, so that means it needs the callbacks, tho the
// only once it really needs are gotImage and gotImageError.

// The big advantage to having the ImageRef doing the low-level listening to the image
// loader is that it can be one place that could handle a default common form of display
// update: e.g., while image is loading, draw as a transparent box w/loading %, display
// common unavail info, etc.  It would also probably want to take a java.awt.Component
// handle for being able to issue optional repaint() calls.  It would be one standard
// place to have all the info about raw size, loading status, etc.  It could also handle
// going between a Resource (if it has one) and the image data/image icon.

//----------------------------------------------------------------------------------------
// Note: subclassing java.awt.Image is not actually supported; in AWT Graphics drawing:
// SurfaceManager can't get surface.  see
// http://forums.sun.com/thread.jspa?threadID=5208043 -- basically, this is a 10+ year
// old bug that's never been fixed -- either java.awt.Image needs to be abstract or the
// impls need changing, and it looks like there's just no hope on this one at all.
// Subclassing BufferedImage MAY be possible tho...  this would lock us in 100% to never
// using ToolkitImage's, tho we can probably live with that as that impl gives us memory
// heartburn, at least on the Mac.  The bigger issue is we'd like to be able to rely on
// using createCompatibleImage for creating our BufferedImages, and not have to create
// our own delegating instances.

