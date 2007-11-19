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

import tufts.Util;
import static tufts.Util.*;

import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import java.util.*;
import java.net.*;
//import tufts.vue.beans.UserMapType; // remove: old SB stuff we never used
import tufts.vue.filter.*;

import edu.tufts.vue.metadata.MetadataList;

import edu.tufts.vue.style.Style;

import edu.tufts.vue.preferences.implementations.BooleanPreference;
import edu.tufts.vue.preferences.interfaces.VuePreference;    

/**
 * VUE base class for all components to be rendered and edited in the MapViewer.
 *
 * @version $Revision: 1.387 $ / $Date: 2007-11-19 06:20:27 $ / $Author: sfraize $
 * @author Scott Fraize
 * @license Mozilla
 */

// todo: on init, we need to force the constraint of size being set before
// label (applies to XML restore & duplicate) to support backward compat before
// line-wrapped text.  Otherwise, in LWNode's, setting label before size set will cause
// the size to be set.

public class LWComponent
    implements VueConstants, XMLUnmarshalListener
{
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWComponent.class);
    
    public enum ChildKind {
        /** the default, conceptually significant chilren */
        PROPER,
            
        /** VISIBLE is PROPER + Currently visible */
        VISIBLE,

        /** Above, plus include ANY children, such as slides and their children -- the only
         * way to make sure you hit every active LWComponent in the runtime related
         * to a particular LWMap (not including the Undo queue)
         */
        ANY

       // VIRTUAL -- would be *just* what ANY currently adds, and exclude PROPER -- currently unsupported
    }

    /** order of result set for getAllDescendents -- not applicable if collection passed in isn't ordered */
    public enum Order {
            /** default traversal order: parents before children */
            TREE,
            /** order for layout operations; children before parents */
            DEPTH
    };

    

    /*
    // need an IntegerPreference and/or an IntegerRangePreference (that ImagePreference could also use)
    private static final VuePreference SlideIconPref =
        IntegerPreference.create(edu.tufts.vue.preferences.PreferenceConstants.MAPDISPLAY_CATEGORY,
			"slideIconSize", 
			"Slide Icon Size", 
			"Size of Slide icons displayed on the map",
			true);

    */
    

    public enum HideCause {
//         /** special case bit for deleted objects (which always remain in the undo queue) */
//         DELETED (),
            
            /** each subclass of LWComponent can use this for it's own purposes */
            DEFAULT (),
            /* we've been hidden by a filter */
            //FILTER (), 
            /** we've been hidden by link pruning */
            PRUNE (),
            /** we're a member of a pathway that hides when the pathway hides, and all pathways we're on are hidden */
            HIDES_WITH_PATHWAY (true),
            /** we've been hidden by a pathway that is in the process of revealing */
            PATH_UNREVEALED (true),
            /** we've been hidden because the current pathway is all we we want to see, and we're not on it */
            NOT_ON_CURRENT_PATH (true); 

            
        final int bit = 1 << ordinal();
        final boolean isPathwayCause;

        HideCause(boolean isPathCause) { isPathwayCause = isPathCause; }
        HideCause() { isPathwayCause = false; }
    }

    public enum Flag {
        DELETED,
            
            IS_STYLE,
            /** cannot move, delete, link to or edit label */
            LOCKED,
            /** can't be moved */
            FIXED_LOCATION,
//             NO_DELETE,
//             NO_LINKS,
            SLIDE_STYLE;

        // TODO: general LOCKED which means fixed,no-delete,no-duplicate?,no-reorder(forward/back),no-link
            
        final int bit = 1 << ordinal();
    }
    
    

    //Static { for (Hide reason : Hide.values()) { System.out.println(reason + " bit=" + reason.bit); } }

    public static final java.awt.datatransfer.DataFlavor DataFlavor =
        tufts.vue.gui.GUI.makeDataFlavor(LWComponent.class);
    
    public static final int MIN_SIZE = 10;
    public static final Size MinSize = new Size(MIN_SIZE, MIN_SIZE);
    public static final float NEEDS_DEFAULT = Float.MIN_VALUE;
    
    public interface Listener extends java.util.EventListener {
        public void LWCChanged(LWCEvent e);
    }

    /*
     * Meta-data persistant information
     */
    protected String label = null; // protected for debugging purposes
    private String notes = null;
    private Resource resource = null;
    
    /*
     * Persistent information
     */

    private String ID = null;

    // todo: next major re-architecting: instead of x/y width/height,
    // keep a Point2D.Float bounds up to date (and can skip creating
    // a rectangles constantly).  (Might also keep a mapBounds?)

    private float x;
    private float y;
    // TODO: if we want to support some kind of keep-relative alignment for an object
    // (in it's parent), we couldn't just use a special object on a generic x/y value
    // ptr -- we still need ACTUAL x/y values to render, but we could have an
    // xAnchor/yAnchor, which could even be a list of actions to perform every time the
    // object is laid out, or it's parent resizes.
    
    private boolean isFiltered = false; // replace with hidebits
    
    private MetadataList metadataList = new MetadataList();
    private static final NodeFilter NEEDS_NODE_FILTER = new NodeFilter();
    private NodeFilter nodeFilter = NEEDS_NODE_FILTER;
    private URI uri;
    protected float width = NEEDS_DEFAULT;
    protected float height = NEEDS_DEFAULT;

    /** cached affine transform for use by getZeroTransform() */
    private final AffineTransform _zeroTransform = new AffineTransform();


    /*
     * Runtime only information
     */
    protected transient TextBox labelBox = null;
    protected transient BasicStroke stroke = STROKE_ZERO;
    protected transient boolean selected = false;
    //protected transient boolean rollover = false;
    protected transient boolean isZoomedFocus = false;
    protected int mHideBits = 0x0; // any bit set means we're hidden
    protected int mFlags = 0x0;

    protected transient LWContainer parent;
    protected transient LWComponent mParentStyle;
    protected transient LWComponent mSyncSource; // "semantic source" for nodes on slide to refer back to the concept map
    protected transient Collection<LWComponent> mSyncClients; // set of sync sources that point back to us

    /** list of links that contain us as an endpoint */
    private transient List<LWLink> mLinks;
    /** list of pathways that we are a member of */
    private transient List<LWPathway> mPathways;
    /** list of all pathway entries that refer to us (one for each time we appear on an individual pathway) */
    protected transient List<LWPathway.Entry> mEntries;
    
    // todo memory perf: mEntries should subclass ArrayList and implement this iter
    // so they can be allocated together, instead of leaving this slot here unused
    // for ever node w/out pathway entries.
    private SlideIconIter mVisibleSlideIconIterator;
    
    private transient long mSupportedPropertyKeys;

    private transient double scale = 1.0;

    protected transient final LWChangeSupport mChangeSupport = new LWChangeSupport(this);

    protected transient boolean mXMLRestoreUnderway = false; // are we in the middle of a restore?
    
    protected transient BufferedImage mCachedImage;

    public static final Comparator XSorter = new Comparator<LWComponent>() {
            public int compare(LWComponent c1, LWComponent c2) {
                // we multiply up the result so as not to loose differential precision in the integer result
                return (int) (128f * (c1.x - c2.x));
            }
        };
    public static final Comparator YSorter = new Comparator<LWComponent>() {
            public int compare(LWComponent c1, LWComponent c2) {
                return (int) (128f * (c1.y - c2.y));
            }
        };

    public static final Comparator GridSorter = new Comparator<LWComponent>() {
            public int compare(LWComponent c1, LWComponent c2) {
                if (c1.y == c2.y)
                    return XSorter.compare(c1, c2);
                else
                    return YSorter.compare(c1, c2);
                
            }
        };

    /** constructor */
    public LWComponent()
    {
        if (DEBUG.PARENTING) Log.debug("construct of " + Util.tag(this));
        mSupportedPropertyKeys = Key.PropertyMaskForClass(getClass());
        if (mSupportedPropertyKeys == 0) {
            // this can happen during init before circular dependencies are resolved
            if (DEBUG.INIT || DEBUG.STYLE) Util.printStackTrace("ZERO PROPERTY BITS IN " + Util.tag(this));
        } else {
            // not on by default:
            disableProperty(KEY_Alignment);
        }

    }
    
//     /** for internal proxy instances only */
//     private LWComponent(String label) {
//         setLabel(label);
//     }

    public long getSupportedPropertyBits() {
        return mSupportedPropertyKeys;
    }

    /** Convenience: If key not a real Key (a String), always return true */
    public boolean supportsProperty(Object key) {
        if (key instanceof Key)
            return supportsProperty((Key)key);
        else
            return false;
    }
    
    /** @return true if the given property is currently supported on this component */
    public boolean supportsProperty(Key key) {
        return (mSupportedPropertyKeys & key.bit) != 0;
    }

    protected void disableProperty(Key key) {
        disablePropertyBits(key.bit);
    }
    
    protected void enableProperty(Key key) {
        enablePropertyBits(key.bit);
    }

    protected void disablePropertyBits(long bits) {
        mSupportedPropertyKeys &= ~bits;
    }
    
    protected void enablePropertyBits(long bits) {
        mSupportedPropertyKeys |= bits;
    }
    
    protected void disablePropertyTypes(KeyType type) {
        for (Key key : Key.AllKeys)
            if (key.type == type || (type == KeyType.STYLE && key.type == KeyType.SUB_STYLE))
                disableProperty(key);
    }
    
    
    /** Apply all style properties from styleSource to this component */
    public void copyStyle(LWComponent styleSource) {
        copyStyle(styleSource, ~0L);
    }
    
    public void copyStyle(LWComponent styleSource, long permittedPropertyBits) {
        if (DEBUG.STYLE) System.out.println("COPY STYLE of " + styleSource + " ==>> " + this + " permitBits=" + Long.bitCount(permittedPropertyBits));
        for (Key key : Key.AllKeys)
            if (key.isStyleProperty && styleSource.supportsProperty(key) && (permittedPropertyBits & key.bit) != 0)
                key.copyValue(styleSource, this);
    }

    public void copyProperties(LWComponent source, long propertyBits) {
        if (DEBUG.STYLE) System.out.println("COPY PROPS of " + source + " ==>> " + this + " bits=" + Long.bitCount(propertyBits));
        for (Key key : Key.AllKeys)
            if ((propertyBits & key.bit) != 0 && source.supportsProperty(key))
                key.copyValue(source, this);
    }
    

    public void applyCSS(edu.tufts.vue.style.Style cssStyle)
    {
        System.out.println("Applying CSS style " + cssStyle.getName() + ":");
        for (Map.Entry<String,String> se : cssStyle.getAttributes().entrySet()) {
            
            final String cssName = se.getKey().trim().toLowerCase(); // todo: shouldn't have to trim this
            final String cssValue = se.getValue().trim();
            boolean applied = false;

            System.err.format("%-35s CSS key %-17s value %-15s",
                              toString(),
                              '\'' + cssName + '\'',
                              '\"' + cssValue + '\"'
                              );
            
            for (Key key : Key.AllKeys) {
                if (key.cssName == null)
                    continue;
                //out("Checking key [" + cssName + "] against [" + key.cssName + "]");
                                
                if(key.cssName.indexOf(";") > 0)
                {
                    String[] names = key.cssName.split(";");
                    for(int i=0;i<names.length;i++)
                    {
                        if(supportsProperty(key) && names[i].equals(cssName))
                        {    
                          applied = key.setValueFromCSS(this,names[i],cssValue);
                        }
                    }
                }
                else
                if (supportsProperty(key) && cssName.equals(key.cssName)) {
                    //out("Matched supported property key " + key.cssName);

                    applied = key.setValueFromCSS(this, cssName, cssValue);

                    /*
                    final Property slot = key.getSlot(this);
                    if (slot == Key.NO_SLOT_PROVIDED) {
                        out("Can't apply CSS Style property to non-slotted key: " + cssName + " -> " + key);
                    } else {
                        try {
                            slot.setFromCSS(cssName, cssValue);
                            System.err.println("applied value: " + slot);
                            applied = true;
                            break;
                        } catch (Throwable t) {
                            System.err.println();
                            tufts.Util.printStackTrace(new Throwable(t), "failed to apply CSS key/value " + cssName + "=" + cssValue);
                        }
                    }
                    */
                }
            }
            setFont(cssStyle.getFont());
            if (!applied)
                System.err.println("UNHANDLED");

        }
    }

    /**
     * Describes a property on a VUE LWComponent, and provides an info string for creating Undo names,
     * and for diagnostic output.  Implies the ability to set/get the value on an LWComponent by some means.
     */
    // todo: consdier moving all the Key/Property code to some kind of superclass to LWComponent -- LWStyle? Vnode? LWKey? LWState?
    // We'd move it elsewhere, but we'd have to export all sorts of stuff to make all thats needed available,
    // as they get everything currently being inner classes.

    // The generic type TSubclass allows the inner-class impl's of getValue & setValue, in subclasses
    // of LWComponent, to use their own type in the first argument to set/getValue, omitting
    // the need for casts in the method.

    public enum KeyType { Default, STYLE, SUB_STYLE, DATA };
            
    // todo: TValue may be overkill -- may want to revert to using just Object
    public static class Key<TSubclass extends LWComponent,TValue> {
        /** A name for this key (used for undo labels & debugging) */
        public final String name;
        /** A name for a CSS property that can be used to initialize the value for this key */
        public final String cssName;
        /** The unique bit for this property key.
            (Implies a max of 64 keys that can uniquely known as active to our tools -- use a BitSet if need more) */
        public final long bit;
        /** True if this key for a style property -- a property that moves from style holders to LWCopmonents
         * pointing to it via mParentStyle */
        public final boolean isStyleProperty;

        public final KeyType type;

        /* True this property is a sub-part of some other property */
        //public final boolean isSubProperty;

        public static final java.util.List<Key> AllKeys = new java.util.ArrayList<Key>();

        private static int InstanceCount; // increment for each key instance, to establish the appropriate bit
        private static final java.util.Map<Class,Long> ClassProperties = new java.util.HashMap<Class,Long>();
        
        /** Get the supported property bit mask for the given class in the LWComponent inheritance tree
         * This will only return accurate results after all Key's in the codebase have been initialized. */
        static long PropertyMaskForClass(Class<? extends LWComponent> clazz) {
            final Long bitsForClass = ClassProperties.get(clazz); // property bits for this class
            if (bitsForClass == null) {
                // If we found nothing, this must be the first instance of a new object
                // for some subclass of LWComponent that doesn't declare any of it's
                // own keys.  Merge the bits for all superclasses and put it in the
                // map for future reference.
                long propMaskForClass = 0L;
                for (Class c = clazz; c != null; c = c.getSuperclass())
                    propMaskForClass |= PartialPropertyMaskForClass(c);

                if (DEBUG.INIT) Log.debug(String.format("CACHED PROPERTY BITS for %s: %d", clazz, Long.bitCount(propMaskForClass)));
                ClassProperties.put(clazz, propMaskForClass);

                return propMaskForClass;
            } else
                return bitsForClass;
        }

        /** @return the currently stored property mask for the given class: only used during initialization
         * Will return 0L (no bit set) if the given class is not in the map (e.g., java.lang.Object)
         * This is used to disambiguate between properties that apply only to a particular
         * LWComponent subclass while we produce the ultimate merged results for all classes in
         * the hierarchy.
         */
        private static long PartialPropertyMaskForClass(Class clazz) {
            final Long bitsForClass = ClassProperties.get(clazz); // property bits for this class
            if (bitsForClass == null)
                return 0L;
            else
                return bitsForClass;
        }
        
        public Key(String name) {
            this(name, KeyType.Default);
        }
        public Key(String name, KeyType keyType) {
            this(name, null, keyType);
        }
        public Key(String name, String cssName) {
            this(name, cssName, KeyType.STYLE);
        }

        //protected Key(String name, String cssName, boolean partOfStyle, boolean isSubProperty) {
        protected Key(String name, String cssName, KeyType keyType) {
            this.name = name;
            this.cssName = cssName;
            this.type = keyType;
            this.isStyleProperty = (keyType == KeyType.STYLE);
            //this.isStyleProperty = partOfStyle;
            //this.isSubProperty = isSubProperty;
            if (InstanceCount >= Long.SIZE) {
                this.bit = 0;
                tufts.Util.printStackTrace(Key.class + ": " + InstanceCount + "th key created -- need to re-implement (try BitSet)");
            } else
                this.bit = 1 << InstanceCount;
            AllKeys.add(this);

            // Note: this only works if the key is in fact declared in the enclosing class to
            // which it applies.  If we want to declare keys elsewhere, we'll need to add
            // a Class argument to the constructor.
            final Class clazz = getClass().getEnclosingClass(); // the class that own's the Key
            long propMaskForClass = (PartialPropertyMaskForClass(clazz) | bit); // add the new bit

            // Now be sure to mix in all properties found in all super-classes:
            for (Class c = clazz; c != null; c = c.getSuperclass())
                propMaskForClass |= PartialPropertyMaskForClass(c);
            
            ClassProperties.put(clazz, propMaskForClass);

            if (DEBUG.INIT || DEBUG.STYLE)
                Log.debug(String.format("KEY %-20s %-11s %-22s bit#%2d; %25s now has %2d properties", 
                                        name,
                                        //isStyleProperty ? "STYLE;" : "",
                                        keyType,
                                        cssName == null ? "" : cssName,
                                        InstanceCount,
                                        clazz.getName(),
                                        Long.bitCount(propMaskForClass)
                                        ));
            InstanceCount++;

            // Just referencing a class object won't load it's statics: must do a new instance.
            // This will be easy enough to ensure at startup.
            //new LWImage();
            //System.out.println("BITS FOR " + LWImage.class + " " + PropertyMaskForClass(LWImage.class));
            
            // Could build list of all key (and thus slot) values here for each subclass,
            // but where would we attach it?  Would need to pass in the class variable
            // in the constructor, and hash it to a list for the class.  Then the
            // problem would be that each list would only contain the subclass items,
            // not the super -- tho could we just iterate up through the supers getting
            // their lists to build the full list for each class?  (e.g., for duplicate,
            // persistance, or runtime diagnostic property editors)

            // OH: we also need to build the bitfield for the enclosing class:
            // the runtime-constant bit-mask representing all the properties
            // handled by this class / subclass of LWComponent

        }

        private static final LWComponent EmptyStyle = new LWComponent();
        static final Property NO_SLOT_PROVIDED = EmptyStyle.mFillColor; // any slot will do
        //private static final Property BAD_SLOT = EmptyStyle.mStrokeColor; // any (different) slot will do
        /** If this isn't overriden to return non-null, getValue & setValue must be overriden to provide the setter/getter impl  */
        Property getSlot(TSubclass c) { return NO_SLOT_PROVIDED; }

        boolean isSlotted(TSubclass c) { return getSlot(c) != NO_SLOT_PROVIDED; }

        // If we wanted to get rid of the slot decl's in the key's (for those that use
        // slots), we could, in our defult slot-using set/getValue, search all property
        // objects in the LWComponent, and if any of them match our key, we know that's
        // that slot, and if none of them do, then we have in internal error: coder
        // should have impl'd set/getValue themselves.

        /** non slot-based property keys can override this */
        TValue getValue(TSubclass c) {
            final Property propertySlot = getSlotSafely(c);
            try {
                if (propertySlot == NO_SLOT_PROVIDED) {
                    tufts.Util.printStackTrace(this + ": no slot, and getValue not overriden");
                    return null;
                } else
                    return (TValue) propertySlot.get();
            } catch (Throwable t) {
                if (DEBUG.META)
                    tufts.Util.printStackTrace(new Throwable(t), this + ": property slot get() failed " + propertySlot);
                else
                    Log.warn(this + ": property slot get() failed " + propertySlot + " " + t);
                return DEBUG.Enabled ? (TValue) "<unsupported for this object>" : null;
                //return null;
            }
        }

        void setValueInternal(TSubclass c, TValue value) {
            setValue(c, value);
        }
        
        /** non slot-based property keys can override this */
        void setValue(TSubclass c, TValue value) {
            final Property slot = getSlotSafely(c);
            if (slot == null || slot == NO_SLOT_PROVIDED)
                return;
            if (value instanceof String) {
                // If a String value comes in, this allows us to auto-parse it
                slot.setFromString((String)value);
            } else {
                slot.set(value);
            }
                /*
            try {
                if (value instanceof String) {
                    // If a String value comes in, this allows us to auto-parse it
u                    getSlot(c).setFromString((String)value);
                } else {
                    getSlot(c).set(value);
                }
            } catch (ClassCastException e) {
                tufts.Util.printStackTrace(e, "Bad setValue type for " + getSlot(c) + ": " + (value == null ? "null" : value.getClass()));
            }
                */
        }
        
        private Property getSlotSafely(TSubclass c) {
            Property slot = null;
            try {
                slot = getSlot(c);
            } catch (ClassCastException e) {
                String msg = "Property not supported: " + this + " on\t" + c + " (getSlot failed; returned null)";
                //tufts.Util.printStackTrace(e, msg);
                Log.warn(msg + "; " + e);
                return null;
            } catch (Throwable t) {
                tufts.Util.printStackTrace(new Throwable(t), this + ": bad slot? unimplemented get/setValue?");
                return null;
            }
            //if (slot == NO_SLOT_PROVIDED) tufts.Util.printStackTrace(this + ": no slot provided");
            return slot;
        }
        

        /** non slot-based property keys can override this */
        String getStringValue(TSubclass c) {
            final Property slot = getSlotSafely(c);
            if (slot == NO_SLOT_PROVIDED || slot == null) {
                // If there is no slot provided, we must get the value from the overridden
                // getter, getValue.
                Object typedValue = null;
                try {
                    // Call the overriden getValue:
                    typedValue = getValue(c);
                } catch (ClassCastException e) {
                    final String msg = "Property not supported(getStringValue): " + this + " on\t" + c;
                    if (DEBUG.META)
                        tufts.Util.printStackTrace(e, msg);
                    else
                        Log.warn(msg + "; " + e);
                    return DEBUG.Enabled ? "<unsupported for this object>" : null;
                }
                return typedValue == null ? null : typedValue.toString(); // produce something
//              } else if (slot == null) {
//                 // If a slot was provided, but it failed, no sense in trying
//                 // the default getValue, which presumably wasn't overriden if
//                 // a slot was provided.
//                 //tufts.Util.printStackTrace(this + ": bad slot");
//                 return DEBUG.Enabled ? "<unsupported for this object>" : null;
            } else
                return slot.asString();
        }
        
        void setStringValue(TSubclass c, String stringValue) {
            Property slot = getSlotSafely(c);
            if (slot != NO_SLOT_PROVIDED) {
                slot.setFromString(stringValue);
            } else {
                TValue curValue = getValue(c);
                // handle a few special cases for standard java types, even if there's no slot (Property object) to parse the string
                // FYI, this won't work if getValue returns null, as we'll have no class object to check for type information.
                     if (curValue instanceof String)    setValue(c, (TValue) stringValue);
                else if (curValue instanceof Integer)   setValue(c, (TValue) Integer.valueOf(stringValue));
                else if (curValue instanceof Long)      setValue(c, (TValue) Long.valueOf(stringValue));
                else if (curValue instanceof Float)     setValue(c, (TValue) Float.valueOf(stringValue));
                else if (curValue instanceof Double)    setValue(c, (TValue) Double.valueOf(stringValue));
                else
                    tufts.Util.printStackTrace(this + ":setValue(" + stringValue + "); no slot provided for parsing string value");
            }
        }

        /** @return true if was successful */
        boolean setValueFromCSS(TSubclass c, String cssKey, String cssValue) {
            final Property slot = getSlot(c);
            if (slot == Key.NO_SLOT_PROVIDED) {
                c.out("Can't auto-apply CSS Style property to non-slotted key: " + cssName + " -> " + this);
                return false;
            }
            try {
                slot.setFromCSS(cssName, cssValue);
                System.err.println("applied value: " + slot);
                return true;
            } catch (Throwable t) {
                System.err.println();
                tufts.Util.printStackTrace(new Throwable(t), "failed to apply CSS key/value " + cssName + "=" + cssValue);
            }
            return false;
        }
        

        /** @return true if the value for this Key in LWComponent is equivalent to otherValue
         * Override to provide non-standard equivalence (Object.equals) */
        boolean valueEquals(TSubclass c, TValue otherValue) 
        {
            final TValue value = getValue(c);
            return value == otherValue || (otherValue != null && otherValue.equals(value));
        }

        void copyValue(TSubclass source, TSubclass target)
        {
            if (!source.supportsProperty(this)) {
                if (DEBUG.STYLE && DEBUG.META) System.err.println(" COPY-VALUE: " + this + "; source doesn't support this property; " + source);
            } else if (!target.supportsProperty(this)) {
                if (DEBUG.STYLE && DEBUG.META) System.err.println(" COPY-VALUE: " + this + "; target doesn't support this property; " + target);
            } else {
                final TValue newValue = getValue(source);
                final TValue oldValue = getValue(target);

                if (newValue != oldValue && (newValue == null || !newValue.equals(oldValue))) {
                    if (DEBUG.STYLE) System.out.format("  COPY-VALUE: %s %-15s %-40s -> %s over (%s)\n",
                                                       source,
                                                       name,
                                                       "(" + newValue + ")",
                                                       target,
                                                       oldValue);
                    setValue(target, newValue);
                }


                //if (DEBUG.STYLE) System.err.print(" COPY-VALUE: " + this + "(");
                //if (DEBUG.STYLE) System.err.println(copyValue + ") -> " + target);
            }
        }

        public String toString() { return name; } // must == name for now until tool panels handle new key objects (is this true yet?)
        //public String toString() { return type + "{" + name + "}"; }
    }

    /**
     * This class allows us to define an arbitrary property for a LWComponent, and
     * define a default set of setters and getters that automatically handle stuff like
     * undo and positing change notifications.  It is also essential in allowin us to
     * easily attach meta-data to the property itself: e.g., it's locked, it's
     * overriding a parent style value, it's caching some related computed value, etc.
     */
    public abstract class Property<T> {
        
        final Key key;
        protected T value;

        boolean locked; // could handle instead as above bitfield
        
        Property(Key key) {
            this.key = key;
            //mSupportedPropertyKeys |= key.bit;
            //LWComponent.this.allProps.add(this);
        }

        T get() { return value; }

        void set(T newValue) {
            //final Object old = get(); // if "get" actually does anything tho, this is a BAD idea; if needbe, create a "curValue"
            if (this.value == newValue || (newValue != null && newValue.equals(this.value)))
                return;
            final Object oldValue = this.value;
            take(newValue);
            onChange();
            LWComponent.this.notify(this.key, oldValue);
        }

        /** This JUST changes the stored value: no notifications of any kind will be triggered, no undo recorded. */
        void take(T o) {
            this.value = o;
            if (DEBUG.TOOL) System.out.printf("     TAKING: %-30s -> %s\n", vtag(key, o, this), LWComponent.this);
        }

        /** impl's can override this to do something after the value has changed (after take() has been called),
         * and before listeners have been notified */
        void onChange() {}

        void setFromString(String s) {
            try {
                setBy(s);
            } catch (Throwable t) {
                Log.error("bad value for " + this + ": [" + s + "] " + t);
            }
        }

        void setFromCSS(String cssKey, String value) {
            throw new UnsupportedOperationException(this + " unimplemented setFromCSS " + cssKey + " = " + value);
            //VUE.Log.error("unimplemented setFromCSS " + cssKey + " = " + value);
        }

        void setBy(String fromValue) {
            // Could get rid all of the setBy's (and then mayve even all the StyleProp subclasses!!)
            // If we just had mapper class that took a type, a value, and returned a string (e.g., Font.class, Object value)
            Log.error("unimplememnted: " + this + " setBy " + fromValue.getClass() + " " + fromValue);
        }

        /** override to provide an impl other than value.toString() */
        String asString() {
            return value == null ? null : value.toString();
        }

        /*
        void setByUser(Object newValue) { // for tools.  Actually, tools using generic setProperty right now...
            out("SetByUser: " + key + " " + newValue);
            set(newValue);
        }
        */

        /** used for debugging */
        public String toString() {
            return key + "[" + value + "]";
        }
        
    }

    public class EnumProperty<T extends Enum> extends Property<T> {
        EnumProperty(Key key, T defaultValue) {
            super(key);
            value = defaultValue;
            //System.out.println("enum values: " + Arrays.asList(defaultValue.getClass().getEnumConstants()));
            //System.out.println("enum test: " + Enum.valueOf(defaultValue.getClass(), "DASH1"));
        }
        void setBy(String s) {
            // note: value can never be null, or we'll need to store the Enum class reference elsewhere
            // (e.g., in the Key -- better there anyway, where we could provide a generic "values"
            // to list the supported values)
            set((T) Enum.valueOf(value.getClass(), s.trim())); 
        }
    }
    
    private static final String _DefaultString = "";
    public class StringProperty extends Property<java.lang.String> {
        StringProperty(Key key) {
            super(key);
            value = _DefaultString;
        }
        void setBy(String s) { set(s); }
    }

    public class BooleanProperty extends Property<java.lang.Boolean> {
        BooleanProperty(Key key, Boolean defaultValue) {
            super(key);
            value = defaultValue;
        }
        BooleanProperty(Key key) {
           this(key, Boolean.FALSE);
        }
        
        void setBy(String s) { set(Boolean.valueOf(s)); }
    }
    
    abstract public class NumberProperty<T> extends Property<T> {
        NumberProperty(Key key) { super(key); }
            
        void setFromCSS(String cssKey, String value) {
            if (value.endsWith("pt") || value.endsWith("px"))
                setBy(value.substring(0, value.length()-2));
            else
                throw new IllegalArgumentException("unhandled CSS number conversion for [" + value + "]");
                      
        }
        
    }


    static class PropertyValueVeto extends RuntimeException {
        PropertyValueVeto(String msg) {
            super(msg);
        }
    }

    
    
    private static final Integer _DefaultInteger = new Integer(0);
    public class IntProperty extends NumberProperty<java.lang.Integer> {
        IntProperty(Key key, Integer defaultValue) {
            super(key);
            value = defaultValue;
        }
        IntProperty(Key key) {
            this(key, _DefaultInteger);
        }
        
        void setBy(String s) { set(new Integer(s)); }
    }
    
    private static final Float _DefaultFloat = new Float(0f);
    public class FloatProperty extends NumberProperty<java.lang.Float> {
        FloatProperty(Key key) {
            super(key);
            value = _DefaultFloat;
        }
        void setBy(String s) { set(new Float(s)); }
    }

    public class FontProperty extends Property<java.awt.Font> {
        FontProperty(Key key) {
            super(key);
            value = VueConstants.FONT_DEFAULT;
        }
        final void setBy(String s) { set(Font.decode(s)); }
        final String asString() {
            //if (this.font == null || this.font == getParent().getFont())
            //return null;

            final Font font = get();
            final String strStyle;
            
            if (font.isBold()) {
                strStyle = font.isItalic() ? "bolditalic" : "bold";
            } else {
                strStyle = font.isItalic() ? "italic" : "plain";
            }
            return font.getName() + "-" + strStyle + "-" + font.getSize();
        }
    }

    /**
     * Handles CSS font-style value "italic" ("normal", or anything else, has no effect as of yet)
     * Also handles CSS font-weight value of "bold" (anything else is ignored for now)
     * todo: no hook for font-weight yet, permits invalid CSS
     */
    public class CSSFontStyleProperty extends IntProperty {
        CSSFontStyleProperty(Key key) { super(key); }
        void setFromCSS(String cssKey, String value) {
            // todo: this ignoring the key, which will permit non-confomant CSS
            if ("italic".equalsIgnoreCase(value))
                set(java.awt.Font.ITALIC);
            else if ("bold".equalsIgnoreCase(value))
                set(java.awt.Font.BOLD);
            else
                set(0);
        }
    }

    /*
    public class CSSFontSizeProperty extends IntProperty {
        CSSFontSizeProperty(Key key) { super(key); }
        void setFromCSS(String cssKey, String value) {
            if (value.endsWith("pt"))
                setBy(value.substring(0, value.length()-2));
            else
                throw new IllegalArgumentException("unhandled CSS font size [" + value + "]");
                      
        }
    }
    */

    public class CSSFontFamilyProperty extends StringProperty {
        CSSFontFamilyProperty(Key key) { super(key); }
        void setFromCSS(String cssKey, String value) {
            // no translation needed for now: just use the raw name -- if it's a preference list tho, we'll need to handle it
            setBy(value);
        }
    }
    

    
    
    
    
    public class ColorProperty extends Property<java.awt.Color> {
        private static final short ALPHA_NOT_PERMITTED = Short.MIN_VALUE;
        private static final short NO_ALPHA_SET = -1;
        private short fixedAlpha = NO_ALPHA_SET;
        
        ColorProperty(Key key) { super(key); }
        ColorProperty(Key key, Color defaultValue) {
            this(key);
            this.value = defaultValue;
        }

        public boolean isTransparent() {
            return value == null || value.getAlpha() == 0;
        }
    
        public boolean isTranslucent() {
            return value == null || value.getAlpha() != 0xFF;
        }

        void setAllowAlpha(boolean allow) {
            if (allow)
                fixedAlpha = NO_ALPHA_SET;
            else
                fixedAlpha = ALPHA_NOT_PERMITTED;
        }

        /** alpha should be in the range 0-255 */
        void setFixedAlpha(int alpha) {
            if (alpha > 255)
                alpha = 255;
            else if (alpha < 0)
                alpha = 0;
            fixedAlpha = (short) alpha;
            //out("SET FIXED ALPHA " + fixedAlpha);
        }

        @Override
        void set(Color newColor) {

            if (fixedAlpha < 0) {
                super.set(newColor);
                return;
            }

            if (value == newColor)
                return;

            // enforce the fixed alpha on any incoming color:
            if (newColor != null && newColor.getAlpha() != fixedAlpha && newColor.getAlpha() != 0) {
                //out("COLOR VALUE: " + newColor + " " + ColorToString(newColor) + " alpha=" + newColor.getAlpha());
                newColor = new Color((newColor.getRGB() & 0xFFFFFF) + (fixedAlpha << 24), true);
                //out("used fixed alpha " + fixedAlpha + " producing " + newColor + " alpha=" + newColor.getAlpha()
                //+ " " + ColorToString(newColor));
            }

            super.set(newColor);
        }
        
        @Override
        void take(Color c) {
            if (fixedAlpha < NO_ALPHA_SET && (c == null || c.getAlpha() != 0xFF))
                throw new PropertyValueVeto(key + "; color with translucence: "
                                            + c
                                            + " alpha=" + c.getAlpha()
                                            + " not allowed on " + LWComponent.this);

//             if (LWComponent.this instanceof LWNode)
//                 super.take(c == null ? null : new Color(c.getRGB() + (fixedAlpha << 24), true));
//             //super.take(c == null ? null : new Color(c.getRGB() + ((128 & 0xFF) << 24), true));
//             //super.take(c == null ? null : new Color(c.getRGB() + 0x20000000, true));
//             else
            super.take(c);
        }

        @Override
        void setBy(String s) {
            set(StringToColor(s));
        }

        @Override
        void setFromCSS(String key, String value) {
            // todo: CSS Style object could include the already instanced Color object
            // we ignore key: assume that whatever it is is a color value
            setBy(value);
        }

        /** @return a value between 0.0 and 1.0 representing brightness: the saturation % of the strongest channel
         * e.g.: white returns 1, black returns 0
         */
        public float brightness() {
            return Util.brightness(value);
        }

//         dynamic version not workng
//         ///** @return the color, but with 50% alpha (half transparent) */
//         public final Color getWithAlpha(float alpha) {
//             return new Color(value.getRGB() + (((byte)(alpha*256)) << 6), true);
//             //return new Color(value.getRGB() + 0x80000000, true);
//         }

        public boolean equals(Color c) {
            return value == c || (c != null && c.equals(value));
        }


        String asString() {
            return ColorToString(get());
        }
    }
    
    public static Color StringToColor(final String s)
    {
        if (s.trim().length() < 1)
            return null;
        
        Color c = null;
        try {
            c = VueResources.parseColor(s);
        } catch (NumberFormatException e) {
            tufts.Util.printStackTrace(new Throwable(e), "LWComponent.StringToColor[" + s + "]");
        }
        return c;
    }
    public static String ColorToString(final Color c)
    {
        // if null, or no hue and no alpha, return null
        //if (c == null || ((c.getRGB() & 0xFFFFFF) == 0 && c.getAlpha() == 255))
        if (c == null)
            return null;
        
        if (c.getAlpha() == 255) // opaque: only bother to save hue info
            return String.format("#%06X", c.getRGB() & 0xFFFFFF);
        else
            return String.format("#%08X", c.getRGB());
    }

    public enum Alignment { LEFT, CENTER, RIGHT }
    
    public static final Key KEY_FillColor   = new Key("fill.color", "background")       { final Property getSlot(LWComponent c) { return c.mFillColor; } };
    public static final Key KEY_TextColor   = new Key("text.color", "font-color")       { final Property getSlot(LWComponent c) { return c.mTextColor; } };
    public static final Key KEY_StrokeColor = new Key("stroke.color", "border-color")   { final Property getSlot(LWComponent c) { return c.mStrokeColor; } };
    //public static final Key KEY_StrokeStyle = new Key("stroke.style", "border-style")   { final Property getSlot(LWComponent c) { return null; } };
    public static final Key KEY_StrokeWidth = new Key("stroke.width", "stroke-width")   { final Property getSlot(LWComponent c) { return c.mStrokeWidth; } };
    public static final Key KEY_StrokeStyle = new Key<LWComponent,StrokeStyle>
        ("stroke.style", KeyType.STYLE)   { final Property getSlot(LWComponent c) { return c.mStrokeStyle; } };
    public static final Key KEY_Alignment = new Key<LWComponent,Alignment>
        ("alignment", KeyType.STYLE)   { final Property getSlot(LWComponent c) { return c.mAlignment; } };


    /* font.size: point size for font */
    /* font.style: @See java.awt.Font 0x0=Plain, 0x1=Bold On, 0x2=Italic On */
    /* font.name: family name of the font */
    
    /** Aggregate font key, which represents the combination of it's three sub-properties */
    public static final Key KEY_Font = new Key("font", KeyType.STYLE)                   { final Property getSlot(LWComponent c) { return c.mFont; } };
    public static final Key KEY_FontSize  = new Key("font.size", KeyType.SUB_STYLE)     { final Property getSlot(LWComponent c) { return c.mFontSize; } };
    public static final Key KEY_FontStyle = new Key("font.style", KeyType.SUB_STYLE)    { final Property getSlot(LWComponent c) { return c.mFontStyle; } };
    public static final Key KEY_FontName  = new Key("font.name", KeyType.SUB_STYLE)     { final Property getSlot(LWComponent c) { return c.mFontName; } };
    
    public final ColorProperty mFillColor = new ColorProperty(KEY_FillColor);
    public final ColorProperty mTextColor = new ColorProperty(KEY_TextColor, java.awt.Color.black) {
            //{ color = java.awt.Color.black; } // default value
            void onChange() {
                if (labelBox != null)
                    labelBox.copyStyle(LWComponent.this); // todo better: handle thru style.textColor notification?
            }
        };
    public final ColorProperty mStrokeColor = new ColorProperty(KEY_StrokeColor, java.awt.Color.darkGray);
    public final FloatProperty mStrokeWidth = new FloatProperty(KEY_StrokeWidth) { void onChange() { rebuildStroke(); }};
    public final EnumProperty<Alignment> mAlignment = new EnumProperty(KEY_Alignment, Alignment.LEFT) {
            void onChange() { layout(KEY_Alignment); }
        };

    public final EnumProperty<StrokeStyle> mStrokeStyle = new EnumProperty(KEY_StrokeStyle, StrokeStyle.SOLID) { void onChange() { rebuildStroke(); }};

    public enum StrokeStyle {

        SOLID   (1,0),
            DOTTED (1,1),
            DASHED (2,2),
            DASH2 (3,2),
            DASH3 (5,3);
            
        private final float[] dashPattern = new float[2];

        StrokeStyle(float dashOn, float dashOff) {
            dashPattern[0] = dashOn; // pixels on (drawn)
            dashPattern[1] = dashOff; // pixels off (whitespace)
        }

        public BasicStroke makeStroke(double width) {
            return makeStroke((float) width);
        }
        
        public BasicStroke makeStroke(float width) {
            if (this == SOLID)
                return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
                //return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
            else
                return new BasicStroke(width
                                       , BasicStroke.CAP_BUTT // anything else will mess with the dash pattern
                                       , BasicStroke.JOIN_BEVEL
                                       , 10f // miter-limit
                                       //, 0f // miter-limit
                                       , dashPattern
                                       , 0f); // dash-phase (offset to start of pattern -- apparently pixels, not index)
        }
        // todo opt: better: could cache the strokes here for each dash pattern/size
        
    }

    private void rebuildStroke() {
        final float width = mStrokeWidth.get();
        if (width > 0)
            this.stroke = mStrokeStyle.get().makeStroke(width);
        else
            this.stroke = STROKE_ZERO;
        /*/ below code was broken in previous code.  Node child layout does NOT
        // appear to be taking into account total bounds with at the moment anyway...
        // (Or was that just for Groups?  No, those appear to be handling the full bounds change.)
        // Also, want to make generic with a flag in Key if layout needed when
        // the given property changes.
        if (getParent() != null) {
            // because stroke affects bounds-width, may need to re-layout parent
            getParent().layout();
        }
        layout();*/
    }


    public final IntProperty mFontStyle = new CSSFontStyleProperty(KEY_FontStyle)       { void onChange() { rebuildFont(); } };
    public final IntProperty mFontSize = new IntProperty(KEY_FontSize)                  { void onChange() { rebuildFont(); } };
    public final StringProperty mFontName = new CSSFontFamilyProperty(KEY_FontName)     { void onChange() { rebuildFont(); } };

    private boolean fontIsRebuilding; // hack till we cleanup the old font code in gui tools (it's only all-at-once)
    private void rebuildFont() {
        // This so at least for now we have backward compat with the old font property (esp. for tools & persistance)
        fontIsRebuilding = true;
        try  {
            mFont.set(new Font(mFontName.get(), mFontStyle.get(), mFontSize.get()));
        } finally {
            fontIsRebuilding = false;
        }
    }
    
    public final FontProperty mFont = new FontProperty(KEY_Font) {
            void onChange() {
                if (!fontIsRebuilding) {
                    final Font f = get();
                    mFontStyle.take(f.getStyle());
                    mFontSize.take(f.getSize());
                    mFontName.take(f.getName());
                }

                if (labelBox != null)
                    labelBox.copyStyle(LWComponent.this);
                layout(this.key); // could make this generic: add a key bit that says "layout needed on-change";
            }
        };


    public static final Key KEY_Label = new Key<LWComponent,String>("label", KeyType.DATA) {
            public void setValue(LWComponent c, String val) { c.setLabel(val); }
            public String getValue(LWComponent c) { return c.getLabel(); }
        };
    public static final Key KEY_Notes = new Key<LWComponent,String>("notes", KeyType.DATA) {
            public void setValue(LWComponent c, String val) { c.setNotes(val); }
            public String getValue(LWComponent c) { return c.getNotes(); }
        };


    //===================================================================================================
    //
    // End of Key's and Properties
    //
    //===================================================================================================

    // for debug
    private static String vtag(Object key, Object val, Property p) 
    {
        if (val == null) {
            return key + "(null)";
        } else if (val.getClass() == String.class) {
            return key + "(\"" + val + "\")";
        }
        
        String typeName = val.getClass().getName();
        String valType = typeName.substring(typeName.lastIndexOf('.') + 1);
        String valRep = (p == null ? val.toString() : p.asString());

        String extra = "";
        
        //if (p != null) extra = val.toString();
        //valType += "@" + Integer.toHexString(val.hashCode());
        
        return key + " " + valType + "(" + valRep + ")" + extra + "";
    }
    
    /**
     * Get the named property value from this component.
     * @param key property key (see LWKey)
     * @return object representing appropriate value, or null if none found (note: properties may be null also -- todo: fix)
     */

    public Object getPropertyValue(final Object key)
    {
        if (key instanceof Key) {
            // If getValue on the key was overriden, we may still need to trap an exception here
            try {
                return ((Key)key).getValue(this);
            } catch (ClassCastException e) {
                String msg = "Property not supported(getPropertyValue): " + key + " on " + this + " (returned null)";
                if (DEBUG.META)
                    tufts.Util.printStackTrace(e, msg);
                else
                    Log.warn(msg + "; " + e);
                return null;
            }
        }

        // Old property keys that don't make use of the Key class yet:
        if (key == LWKey.Resource)      return getResource();
        if (key == LWKey.Location)      return getLocation();
        if (key == LWKey.Size)          return new Size(this.width, this.height);
        if (key == LWKey.Hidden)        return isHidden() ? Boolean.TRUE : Boolean.FALSE;
             
        Log.warn(this + " getPropertyValue; unsupported property [" + key + "] (returning null)");
        if (key == null) Util.printStackTrace("key was null");
        //throw new RuntimeException("Unknown property key[" + key + "]");
        return null;
    }

    public void setProperty(final Object key, Object val)
    {
        if (DEBUG.TOOL||DEBUG.UNDO) System.out.println("setProperty: " + vtag(key, val, null) + " on " + LWComponent.this);

        if (key instanceof Key) {
            final Key k = (Key) key;
            k.setValue(this, val);
            // Experiment it auto-copying over data elements to siblings
            // TODO: label's a special case due to TextBox use of non-key'd setLabel0
            //if (k.keyType == KeyType.DATA && mSibling != null)
            //    k.setValue(mSibling, val);
        }
        // Old property keys that don't make use of the Key class yet:
        //else if (key == LWKey.Hidden)        setHidden( ((Boolean)val).booleanValue());
        else if (key == LWKey.Scale)         setScale((Double) val);
        else if (key == LWKey.Resource)      setResource( (Resource) val);
        else if (key == LWKey.Location) {

            // This is a bit of a hack, in that we're relying on the fact that the only
            // thing to call setProperty with a Location key right now is the
            // UndoManager.  In any case, on undo, we do NOT want to additionally make
            // mapLocationChanged calls on all descendents (for absolute map location
            // objects; e.g. LWLink's).  Location changes as a result of these calls
            // were already recorded as events and will be undone on their own.
            
            final Point2D.Float loc = (Point2D.Float) val;
            setLocation(loc.x, loc.y, this, false);
            //setLocation( (Point2D) val);
        }
        else if (key == LWKey.Size) {
            Size s = (Size) val;
            setSize(s.width, s.height);
        } else if (key == LWKey.Frame) {
            Rectangle2D.Float r = (Rectangle2D.Float) val;
            setFrame(r.x, r.y, r.width, r.height);
        } else {
            //out("setProperty: unknown key [" + key + "] with value [" + val + "]");
            tufts.Util.printStackTrace("FYI: Unhandled Property key: " + key.getClass() + "[" + key + "] with value [" + val + "]");
        }
    }


    /**
     * This is used during duplication of group's of LWComponent's
     * (e.g., a random selection, or a set of children, or an entire map),
     * to reconnect links within the group after duplication, and
     * passing flags into the dupe context.
     */
    public static class LinkPatcher {
        private java.util.Map<LWComponent,LWComponent> mCopies = new java.util.HashMap();
        private java.util.Map<LWComponent,LWComponent> mOriginals = new java.util.HashMap();

        public LinkPatcher() {
            if (DEBUG.DND) Log.debug("LinkPatcher: created");
        }

        public void reset() {
            mCopies.clear();
            mOriginals.clear();
        }

        public void track(LWComponent original, LWComponent copy)
        {
            if (DEBUG.DND && DEBUG.META) Log.debug("LinkPatcher: tracking " + copy);
            mCopies.put(original, copy);
            mOriginals.put(copy, original);
        }

        //public Collection getCopies() { return mCopies.values(); }
        
        public void reconnectLinks() {
            
            // Find all LWLink instances in the set of copied
            // objects, and fix their endpoint pointers to
            // point to the right object within the copied set.
            
            for (LWComponent c : mCopies.values()) {
                if (!(c instanceof LWLink))
                    continue;

                final LWLink linkCopy = (LWLink) c;
                final LWLink linkOriginal = (LWLink) mOriginals.get(linkCopy);
                
                final LWComponent headCopy = mCopies.get(linkOriginal.getHead());
                final LWComponent tailCopy = mCopies.get(linkOriginal.getTail());
                
                if (DEBUG.DND)
                    Log.debug("LinkPatcher: reconnecting " + linkCopy + " endpoints:"
                              + "\n\t" + headCopy
                              + "\n\t" + tailCopy
                              );
                
                linkCopy.setHead(headCopy);
                linkCopy.setTail(tailCopy);
            }
        }
    }

    public static class CopyContext {
        final boolean dupeChildren;
        LinkPatcher patcher;
        CopyContext() { this(true); }
        CopyContext(boolean dupeChildren) {
            this.dupeChildren = dupeChildren;
        }
        CopyContext(LinkPatcher lp, boolean dupeChildren) {
            this.patcher = lp;
            this.dupeChildren = dupeChildren;
        }

        void reset() {
            if (patcher != null)
                patcher.reset();
        }

        void complete() {
            if (patcher != null)
                patcher.reconnectLinks();
        }
    }

    protected void copySupportedProperties(LWComponent c) {
        mSupportedPropertyKeys = c.mSupportedPropertyKeys;
    }
    
    
    public boolean canDuplicate() {
        return true;
    }
    
    /**
     * Create a component with duplicate content & style.  Does not
     * duplicate any links to this component, and leaves it an
     * unparented orphan.
     *
     * @param linkPatcher may be null.  If not, it's used when
     * duplicating group's of objects containing links that need to be
     * reconnected at the end of the duplicate.
     */

    public LWComponent duplicate(CopyContext cc)
    {
        final LWComponent c;

        try {
            c = getClass().newInstance();
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, "duplicate " + getClass());
            return null;
        }

        c.copySupportedProperties(this);
        
        c.x = this.x;
        c.y = this.y;
        c.width = this.width;
        c.height = this.height;
        c.scale = this.scale;
        c.stroke = this.stroke; // cached info only

        
        // duplicate meta-data list
        List<edu.tufts.vue.metadata.VueMetadataElement> md = getMetadataList().getMetadata();
        List<edu.tufts.vue.metadata.VueMetadataElement> mdc = c.getMetadataList().getMetadata();
        Iterator<edu.tufts.vue.metadata.VueMetadataElement> i = md.iterator();
        while(i.hasNext())
        {
            mdc.add(i.next());
        }

        c.copyStyle(this);

        c.setAutoSized(isAutoSized());
        //c.setFillColor(getFillColor());
        //c.setTextColor(getTextColor());
        //c.setStrokeColor(getStrokeColor());
        c.setLabel(this.label); // use setLabel so new TextBox will be created [!no longer an effect]
        c.getLabelBox().setSize(getLabelBox().getSize());

        
        if (hasResource())
            c.setResource(getResource());
        if (hasNotes())
            c.setNotes(getNotes());

        if (cc.patcher != null)
            cc.patcher.track(this, c);
                
        return c;
    }

    public LWComponent duplicate() {
        return duplicate(new CopyContext());
    }

    protected boolean isPresentationContext() {
        if (true) return false;// turned off for now
        if (parent == null)
            return false; // this means presentation nodes will report wrong sizes during restores...
        else
            return parent.isPresentationContext();
    }


    /**
     * Make sure this LWComponent has an ID -- will have an effect on
     * on any brand new LWComponent exactly once per VM instance.
     */
    protected void ensureID(LWComponent c)
    {
        if (c.getID() == null) {
            String id = getNextUniqueID();
            // no ID may be available if we're an orphan: it will be
            // patched up when we eventually get added to to a map
            if (id != null)
                c.setID(id);
        }

        for (LWComponent child : c.getChildList())
            ensureID(child);
    }

    protected String getNextUniqueID()
    {
        if (getParent() == null) {
            //throw new IllegalStateException("LWComponent has null parent; needs a parent instance subclassed from LWContainer that implements getNextUniqueID: " + this);
            //if (DEBUG.PARENTING) tufts.Util.printStackTrace("getNextUniqueID: returning null for current orphan " + this);
            if (DEBUG.PARENTING) out("getNextUniqueID: returning null for current orphan");
            return null;
        } else
            return getParent().getNextUniqueID();
    }

    //private static int MapDepth;
    public LWMap getMap() {
        if (parent == null) {
            return null;
        } else {
//             if (++MapDepth >= 64) { // DEBUG
//                 Util.printStackTrace("PARENT LOOP at depth " + MapDepth);
//                 System.err.println("LWC: " + this);
//                 return null;
//             }
//             final LWMap m = parent.getMap();
//             MapDepth--;
//             return m;
            return parent.getMap();
            
        }
    }

    public UndoManager getUndoManager() {
        final LWMap map = getMap();
        if (map == null)
            return null;
        else
            return map.getUndoManager();
    }

    protected void addCleanupTask(Runnable task) {
        addCleanupTask(task, this);
        //addCleanupTask(task, this, null);
    }
    
    //    protected void addCleanupTask(Runnable task, Object taskKey, Object srcMsg) {
    protected void addCleanupTask(Runnable task, Object taskKey) {
        final UndoManager um = getUndoManager();

        if (um != null) {
            if (um.isUndoing()) {
                if (DEBUG.WORK || DEBUG.UNDO) System.out.println("Ignoring cleanup task during undo: " + task + " for " + this);
            } else if (um.hasCleanupTask(taskKey)) {
                if (DEBUG.WORK || DEBUG.UNDO) System.out.println("Ignoring duplicate cleanup task: " + task + " for " + this);
            } else {

                boolean debug = DEBUG.WORK || DEBUG.UNDO;
                if (isDeleted()) {
                    Util.printStackTrace("warning: adding cleanup task when deleted");
                    debug = true;
                }
                
//                 if (debug) {
//                     System.out.println(TERM_RED + "ADDING CLEANUP TASK: " + task 
//                                        + (srcMsg==null?"":("on " + srcMsg))
//                                        + (task == this ? "" : (" for " + this))
//                                        + TERM_CLEAR);
//                 }
                
                um.addCleanupTask(this, task);
            }
        }
    }
    
    
    
    public UserMapType getUserMapType() { throw new UnsupportedOperationException("deprecated"); }
    public boolean hasMetaData() {
        return ( (metadataList != null) && (getMetaDataAsHTML().length() > 0) );
    }
    public String getMetaDataAsHTML() {
        if (metadataList != null && metadataList.getMetadata().size() > 0) {
            String txt = "";
            //List allMetadata = metadataList;
            for (int i=0;i<metadataList.getCategoryListSize();i++) {
                String value = metadataList.getMetadata().get(i).getValue();//((edu.tufts.vue.metadata.VueMetadataElement)o).getValue();
                if(value.length() > 0)
                {    
                  txt += "<br>" + value;
                }
            }
            
            if(txt.length() > 0)
            {
                txt = "Keywords: " + txt;
            }
            
            return txt;
        } else {
            return "";
        }
    }
   
    
    /**
     * This sets the flag for the component so that it is either
     * hidden or visible based on a match to the active LWCFilter
     **/
    public void setFiltered(boolean filtered) {
    	isFiltered = filtered;
        //setHidden(HideCause.FILTER, filtered);
    }
    
    /**
     * @return true if should be hidden due to a currently applied filter, false if not
     **/
    public boolean isFiltered() {
    	return isFiltered;
        //return (mHideBits & HideCause.FILTER.bit) != 0;
    }

    /**
     * Called during restore from presistance, or when newly added to a container.
     * Must be called at some point before any attempt to persist, with a unique
     * identifier within the entire LWMap.  This is how components are referenced
     * in the persisted data.
     */
    public void setID(String ID)
    {
        if (this.ID != null)
            throw new IllegalStateException("Can't set ID to [" + ID + "], already set on " + this);
        //System.out.println("setID [" + ID + "] on " + this);
        this.ID = ID;

        // special case: if undo of add of any component that was brand new, this is
        // a new component creation, and to undo it is actually a delete.
        // UndoManager handles the hierarchy end of this, but we need this here
        // to differentiate hierarchy events that are just reparentings from
        // new creation events.

        notify(LWKey.Created, new Undoable() {
                void undo() {
                    // parent may already have deleted it for us, so only delete if need be
                    if (!isDeleted())
                        removeFromModel();
                }} );
    }

//     /** set the ID string, no questions asked */
//     protected void takeID(String ID) {
//         this.ID = ID;
//     }
    
    public void setLabel(String label)
    {
        setLabel0(label, true);
    }


    /**
     * Called directly by TextBox after document edit with setDocument=false,
     * so we don't attempt to re-update the TextBox, which has just been
     * updated.
     */
    void setLabel0(String newLabel, boolean setDocument)
    {
        Object old = this.label;
        if (this.label == newLabel)
            return;
        if (this.label != null && this.label.equals(newLabel))
            return;
        if (newLabel == null || newLabel.length() == 0) {
            this.label = null;
            if (labelBox != null)
                labelBox.setText("");
        } else {
            this.label = newLabel;
            // todo opt: only need to do this if node or link (LWImage?)
            // Handle this more completely -- shouldn't need to create
            // label box at all -- why can't do entirely lazily?
            if (this.labelBox == null) {
                // figure out how to skip this:
                //getLabelBox();
            } else if (setDocument) {
                getLabelBox().setText(newLabel);
            }
        }
        layout();
        notify(LWKey.Label, old);
    }

    protected TextBox getLabelBox()
    {
        if (this.labelBox == null) {
            synchronized (this) {
                if (this.labelBox == null)
                    this.labelBox = new TextBox(this, this.label);
            }
        }

        return this.labelBox;
    }

    public void setNotes(String pNotes)
    {
        Object old = this.notes;
        if (pNotes == null) {
            this.notes = null;
        } else {
            String trimmed = pNotes.trim();
            if (trimmed.length() > 0)
                this.notes = pNotes;
            else
                this.notes = null;
        }
        layout();
        notify(LWKey.Notes, old);
    }

    /*
    public void setMetaData(String metaData)
    {
        this.metaData = metaData;
        layout();
        notify("meta-data");
    }
    // todo: setCategory still relevant?
    public void setCategory(String category)
    {
        this.category = category;
        layout();
        notify("category");
    }
    */
    /*
    public String getCategory()
    {
        return this.category;
    }
    */
    
    public void setResource(Resource resource)
    {
        if (DEBUG.CASTOR) out("SETTING RESOURCE TO " + (resource==null?"":resource.getClass()) + " [" + resource + "]");
        Object old = this.resource;
        this.resource = resource;
        layout();
        if (DEBUG.CASTOR) out("NOTIFYING");
        notify(LWKey.Resource, old);
        
        /*
        try {
            layout();u
        } catch (Exception e) {u
            e.printStackTrace();
            if (DEBUG.CASTOR) System.exit(-1);
        }
        */
    }
    
    public Resource getResource() {
        return this.resource;
    }

    public Resource.Factory getResourceFactory() {
        final LWMap map = getMap();
        if (map == null)
            return Resource.getFactory();
        else
            return map.getResourceFactory();
    }
 
    /** convenience delegate to resource factory */
    public void setResource(String spec) {
        setResource(getResourceFactory().get(spec));
    }
    /** convenience delegate to resource factory */
    public void setResource(java.net.URL url) {
        setResource(getResourceFactory().get(url));
    }
    /** convenience delegate to resource factory */
    public void setResource(java.net.URI uri) {
        setResource(getResourceFactory().get(uri));
    }
    /** convenience delegate to resource factory */
    public void setResource(java.io.File file) {
        setResource(getResourceFactory().get(file));
    }
        
//     public void setResource(String urn)
//     {
//         if (urn == null || urn.length() == 0)
//             setResource((Resource)null);
//         else
//             setResource(new MapResource(urn));
//     }

    
    public String getID() {
        return this.ID;
    }

    public int getNumericID() {
        return idStringToInt(getID());
    }

    /** for use during restore */
    protected final int idStringToInt(String idStr)
    {
//         if (idStr != null && idStr.charAt(0) == '<') {
//             // special case for internal use objects, marked with '<' as initial character
//             return -1;
//         }
        
        int id = -1;
        try {
            id = Integer.parseInt(idStr);
        } catch (Exception e) {
            System.err.println(e + " invalid ID: '" + idStr + "'");
            e.printStackTrace();
        }
        return id;
    }
    
    
  /*  public String getStyledLabel()
    {
    	return this.label;
    	
    }*/
    public String getLabel() {
    	return this.label;
    	/*
    	if (this.label == null)
    		return null;
    	
    	String noHTMLString = this.label.replaceAll("\\<.*?\\>","");
    	noHTMLString = noHTMLString.replaceAll("\\&.*?\\;","");
    	noHTMLString = noHTMLString.replaceAll("\n","");
    	noHTMLString = noHTMLString.replaceAll("\\<!--.*?--\\>","");
    	noHTMLString = noHTMLString.replaceAll(" {2,}", " ").trim();
        
    	return noHTMLString;*/
    }

    
    /**
     * @return a label suitable for displaying in a list: if this component
     * has no label set, generate a unique name for it, and if the label has any newlines
     * in it, replace them with spaces.
     */
    public String getDisplayLabel() {
        if (getLabel() == null) {
            return getUniqueComponentTypeLabel();
        } else
            return getLabel().replace('\n', ' ');
    }
    
    String getDiagnosticLabel() {
        if (getLabel() == null) {
            return getUniqueComponentTypeLabel();
        } else
            return getUniqueComponentTypeLabel() + ": " + getLabel().replace('\n', ' ');
    }

    /** return a guaranteed unique name for this LWComponent */
    public String getUniqueComponentTypeLabel() {
        return getComponentTypeLabel() + " #" + getID();
    }
    
    /** return a type name for this LWComponent */
    public String getComponentTypeLabel() {
        String name = getClass().getName();
        if (name.startsWith("tufts.vue.LW"))
            name = name.substring(12);
        else if (name.startsWith("tufts.vue."))
            name = name.substring(10);
        return name;
    }

    String toName() {
        if (getLabel() == null)
            return getDisplayLabel();
        else
            return getComponentTypeLabel() + "[" + getLabel() + "]";
    }
    
    
    /**
     *
     * Metadata List for use with RDF Index
     * It is sufficient for the minimal RDF functionality
     * to be able to retrieve this list from the LWComponent
     * using this method and add elements directly to the list as needed.
     * LWComponent may choose to create notifications/modifcations
     * for any data added directly through LWComponent itself
     * in future.
     *
     **/
     public MetadataList getMetadataList()
     {
         return metadataList;
     }
     
     public void setMetadataList(MetadataList list)
     {
         metadataList = list;
     }
    
    /**
     * left in for (possible future) backward file compatibility
     * do nothing with this data anymore for now.
     **/
    public synchronized NodeFilter getNodeFilter() {
        // if the double-checked locking idiom was reliable in java, we'd use it here, but
        // since it's not, we synchronize this whole method.
        if (nodeFilter == NEEDS_NODE_FILTER) {
            //Util.printStackTrace("lazy create of node filter for " + this);
            nodeFilter = new NodeFilter();
        }
        return nodeFilter;
    }

    /** for persistance */
    public void setXMLnodeFilter(NodeFilter nodeFilter) {
        this.nodeFilter = nodeFilter;
    }
    
    /** return null if the node filter is empty, so we don't bother with entry in the save file */
    public NodeFilter getXMLnodeFilter() {
        if (mXMLRestoreUnderway) {
            // in case validation is on:
            return nodeFilter;
        } else if (nodeFilter == NEEDS_NODE_FILTER || (nodeFilter != null && nodeFilter.size() < 1)) {
            return null;
        } else
            return nodeFilter;
    }

    /** does this support a user editable label? */
    // TODO: resolve this with supportsProperty(LWKey.Label) (perhaps lose this method)
    public boolean supportsUserLabel() {
        return supportsProperty(LWKey.Label);
    }
    /** does this support user resizing? */
    // TODO: change these "supports" calls to an arbitrary property list
    // that could have arbitrary properties added to it by plugged-in non-standard tools
    public boolean supportsUserResize() {
        return false;
    }
    
    /** @return false: subclasses (e.g. containers), override to return true if allows children dragged in and out
     * by a user.
     */
    public boolean supportsChildren() {
        return false;
    }

    /** @Return true: subclasses (e.g. containers), override to return false if you never want this component
        reparented by users */
    public boolean supportsReparenting() {
        return parent instanceof LWGroup == false; // todo: handle via API that LWGroup can declare
    }

    /** @return true: by default, all objects can be selected with other objects at the same time */
    public boolean supportsMultiSelection() {
        return true;
    }

    /** @return false by default -- only containers can have slides */
    public boolean supportsSlide() {
        return false;
    }

    /** @return false by default -- override to initiate dupe and system drag */
    public boolean supportsCopyOnDrag() {
        return false;
    }
    

    /** @return true if we allow a link to the target, and the target allows a link to us.
     * Eventually we can use this to check ontology information.
     * @param target -- the target to check.  If null, tells is if this component allows
     * link to nothing / allows links at all.
     */
    public boolean canLinkTo(LWComponent target) {
        return canLinkToImpl(target) && (target == null || target.canLinkToImpl(this));
    }
    
    
    /** @return true -- subclass impl's can override */
    protected boolean canLinkToImpl(LWComponent target) {
        return hasFlag(Flag.LOCKED) == false;
    }
    
    public boolean hasLabel() {
        return this.label != null && this.label.length() > 0;
    }
    public String getNotes() {
        return this.notes;
    }
    public boolean hasNotes() {
        return this.notes != null && this.notes.length() > 0;
    }
    public boolean hasResource() {
        return this.resource != null;
    }
    public boolean hasLinks() {
        return mLinks != null && mLinks.size() > 0;
    }
    /*
    public String getMetaData()
    {
        return this.metaData;
    }
    public boolean hasMetaData()
    {
        return this.metaData != null;gajendracircle
    }
    */
    public boolean inPathway()
    {
        return mPathways != null && mPathways.size() > 0;
    }

    public boolean inVisiblePathway()
    {
        if (inPathway())
            for (LWPathway p : mPathways)
                if (p.isDrawn())
                    return true;
        return false;
    }
    


    /** Is component in the given pathway? */
    // rename onPathway?
    public boolean inPathway(LWPathway path)
    {
        if (mPathways == null || path == null)
            return false;

        for (LWPathway p : mPathways)
            if (p == path)
                return true;
        
        return false;
    }

    /** @return null if we're in more than one visible pathway, or the LWPathway we're on if it's the only visible one */
    public LWPathway getExclusiveVisiblePathway()
    {
        if (mPathways == null)
            return null;

        boolean foundOne = false;
        LWPathway singleVisible = null;
        for (LWPathway p : mPathways) {
            if (p.isDrawn()) {
                if (foundOne)
                    return null;
                foundOne = true;
                singleVisible = p;
            }
        }
        
        return singleVisible;
    }
    

    public List<LWPathway> getPathways() {
        return mPathways == null ? java.util.Collections.EMPTY_LIST : mPathways;
    }
    
    /**
     * @return true if this component is in a pathway that is
     * drawn with decorations (e.g., not a reveal-way)
     */
    public boolean inDrawnPathway()
    {
        if (mPathways == null)
            return false;

        for (LWPathway p : mPathways)
            if (p.isVisible() && !p.isRevealer())
                return true;

        return false;
    }

    public boolean hasEntries() {
        return mEntries != null && mEntries.size() > 0;
    }

    public int numEntries() {
        return mEntries == null ? 0 : mEntries.size();
    }

    public List<LWPathway.Entry> getEntries() {
        return mEntries;
    }
    

    protected void addEntryRef(LWPathway.Entry e) {
        if (mEntries == null) {
            mEntries = new ArrayList();
            mVisibleSlideIconIterator = new SlideIconIter();
        }
        if (!mEntries.contains(e))
            mEntries.add(e);
        addPathwayRef(e.pathway);
    }

    protected void removeEntryRef(LWPathway.Entry e) {
        if (mEntries == null) {
            Util.printStackTrace(this + "; no entries! can't remove: " + e);
            return;
        }
        if (!mEntries.remove(e))
            Util.printStackTrace(this + "; Warning: didn't contain entry " + e);
        removePathwayRef(e.pathway);
    }
    
    
    private void addPathwayRef(LWPathway p)
    {
        if (mPathways == null)
            mPathways = new ArrayList();
        if (!mPathways.contains(p)) {
            mPathways.add(p);
            layout();
        }
        //notify("pathway.add");
    }
    private void removePathwayRef(LWPathway p)
    {
        if (mPathways == null) {
            if (DEBUG.META) tufts.Util.printStackTrace("attempt to remove non-existent pathwayRef to " + p + " in " + this);
            return;
        }
        mPathways.remove(p);
        // clear any hidden bits that may be set as a result
        // of the membership in the pathway.
        for (HideCause cause : HideCause.values())
            if (cause.isPathwayCause)
                clearHidden(cause);
        layout();
        //notify("pathway.remove");
    }

    

    /** @deprecated - not really deprecated, but intended for persistance only */
    public java.awt.Dimension getXMLtextBox() {
        return null;
        // NOT CURRENTLY USED
        /*
        if (this.labelBox == null)
            return null;
        else
            return this.labelBox.getSize();
        */
    }
    
    /** @deprecated - not really deprecated, intended for persistance only */
    public void setXMLtextBox(java.awt.Dimension d) {
        //this.textSize = d;
    }

    /** for persistance */
    // todo: move all this XML handling stuff to a special castor property mapper,
    // presumably in conjunction with re-architecting the whole mapping style &
    // save mechanism.
    public String getXMLlabel()
    {
        return this.label;
        //return tufts.Util.encodeUTF(this.label);
    }

    /** for persistance */
    public void setXMLlabel(String text)
    {
        setLabel(unEscapeNewlines(text));
        //this.label = unEscapeNewlines(text);
        //getLabelBox().setText(this.label);
        // we want to make sure layout() is not called, 
        // and currently there's no need to do notify's during init.
    }

    /** for persistance */
    public String getXMLnotes()
    {
        //return this.notes;
        // TODO: can escape newlines new with &#xa; and tab with &#x9;
        return escapeWhitespace(this.notes);
    }

    /** for persistance -- gets called by castor after it reads in XML */
    public void setXMLnotes(String text)
    {
        setNotes(decodeCastorMultiLineText(text));
    }

    protected static String decodeCastorMultiLineText(String text)
    {

        // If castor xml indent was on when save was done
        // (org.exolab.castor.indent=true in castor.properties
        // somewhere in the classpath, to make the XML more human
        // readable) it will break up elements like: <note>many chars
        // of text...</note> with newlines and whitespaces to indent
        // the new lines in the XML -- however, on reading them back
        // in, it puts this white space into the string you saved!  So
        // when we save we're sure to manually encode newlines and
        // runs of white space, so when we get here, if see any actual
        // newlines followed by runs of white space, we know to trash
        // them because it was castor formatting fluff.  (btw, this
        // isn't a problem for labels because they're XML attributes,
        // not elements, which are quoted).

        // Update: As of castor 0.9.7, this no longer appears true
        // (it doesn't indent new text lines with white space
        // even after wrapping them), but we still need this
        // here to deal with old save files.
        
        text = text.replaceAll("\n[ \t]*%nl;", "%nl;");
        text = text.replaceAll("\n[ \t]*", " ");
        return unEscapeWhitespace(text);
    }

    // FYI, this is no longer needed for castor XML attributes, as
    // of version 0.9.7 it automatically encodes & preserves them.
    // Note that this is still NOT true for XML elements.
    private static String escapeNewlines(String text)
    {
        if (text == null)
            return null;
        else {
            return text.replaceAll("[\n\r]", "%nl;");
        }
    }
    private static String unEscapeNewlines(String text)
    {
        if (text == null)
            return null;
        else { 
            return text.replaceAll("%nl;", "\n");
        }

    }
    private static String escapeWhitespace(String text)
    {
        if (text == null)
            return null;
        else {
            text = text.replaceAll("%", "%pct;");
            // replace all instances of two spaces with space+%sp;
            // to break them up (and thus we wont lose space runs)
            text = text.replaceAll("  ", " %sp;");
            text = text.replaceAll("\t", "%tab;");
            return escapeNewlines(text);
        }
    }
    private static String unEscapeWhitespace(String text)
    {
        if (text == null)
            return null;
        else { 
            text = unEscapeNewlines(text);
            text = text.replaceAll("%tab;", "\t");
            text = text.replaceAll("%sp;", " ");
            return text.replaceAll("%pct;", "%");
        }
    }
    
    /**
     * If this component supports special layout for it's children,
     * or resizes based on font, label, etc, do it here.
     */
    public final void layout() {
        if (mXMLRestoreUnderway == false)
            layout("default");
    }
    
    final void layout(Object triggerKey) {
        if (mXMLRestoreUnderway == false) {
            layoutImpl(triggerKey);

            if (triggerKey == LWMap.NODE_INIT_LAYOUT)
                layoutSlideIcons(null);
            // need a reshape/reshapeImpl for this (size/location changes)
            //if (mSlideIconBounds != null)
            //    mSlideIconBounds.x = Float.NaN; // invalidate
        }
    }

    protected void layoutImpl(Object triggerKey) {}
    
    /** @return true: default is always autoSized */
    //public boolean isAutoSized() { return true; }
    public boolean isAutoSized() { return false; } // LAYOUT-NEW
    /** do nothing: default is always autoSized */
    public void setAutoSized(boolean t) {}
    
    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
    
    public boolean isTransparent() {
        return mFillColor.isTransparent();
    }
    
    public boolean isTranslucent() {
        return mFillColor.isTranslucent();
    }
    
    /**
     * Color to use at draw time. LWNode overrides to provide darkening of children.
     * We also use this for the background color in active on-map text edits.
     */
    public Color getRenderFillColor(DrawContext dc) {
        if (mFillColor.isTransparent()) {
            if (dc != null && dc.focal == this) {
                //System.out.println("     DC FILL: " + dc.getFill() + " " + this);
                return dc.getBackgroundFill();
            } else if (parent != null) {
                //System.out.println(" PARENT FILL: " + parent.getRenderFillColor(dc) + " " + this);
                return parent.getRenderFillColor(dc);
            }
        }
        //System.out.println("DEFAULT FILL: " + mFillColor.get() + " " + this);
        return mFillColor.get();
    }

    public Color getFinalFillColor(DrawContext dc) {
        if (mFillColor.isTransparent()) {
            Color c = null;
            if (getParent() != null)
                return getParent().getFinalFillColor(dc);
            else if (dc != null)
                return dc.getBackgroundFill();
            else
                return null;            
        } else
            return getFillColor();
    }

    public static Color getContrastColor(Color c) {
        if (c != null) {
            if (c.equals(Color.black))
                return Color.darkGray;
            else
                return c.darker();
        } else {
            return DEBUG.BOXES ? Color.red : Color.gray;
        }
    }

    public Color getContrastStrokeColor(DrawContext dc) {
        final Color renderFill = getRenderFillColor(dc);
        if (renderFill != null && !isTransparent()) {
            return getContrastColor(renderFill);
        } else {
            // transparent fill: just use stroke color
            return getStrokeColor();
            // transparent fill: base on stroke color
            //return getStrokeColor().brighter();
        }
    }

    

    //private LWPathway lastPriorit;

    public Color getPriorityPathwayColor(DrawContext dc) {
        final LWPathway exclusive = getExclusiveVisiblePathway();
        if (exclusive != null)
            return exclusive.getColor();
        else if (inPathway(VUE.getActivePathway()) && VUE.getActivePathway().isDrawn())
            return VUE.getActivePathway().getColor();
        else
            return null;
        //return getRenderFillColor(dc);
    }

    
    
    void takeFillColor(Color color) {
        mFillColor.take(color);
    }

    // We still need these standard style setters & getters for backward compat
    // with all sorts of old code, and espcially for persistance (the castor
    // mapping, which refers to these methods)

    public float        getStrokeWidth()                { return mStrokeWidth.get(); }
    public void         setStrokeWidth(float w)         { mStrokeWidth.set(w); }
    
    /** @return null for SOLID (ordinal 0, the default, as for old save files), or otherwise, the ordinal of the style enum
     * Castor will not bother to generate the attribute/element when it's value is null. */
    public Integer getXMLstrokeStyle() {
        int code = mStrokeStyle.get().ordinal();
        return code == 0 ? null : code;
    }
    public void setXMLstrokeStyle(Integer ordinal)  {
        // todo: have the Key class process enum's generically, caching the results of Class<? extends Enum>.getEnumConstants()
        for (StrokeStyle ss : StrokeStyle.values()) {
            if (ss.ordinal() == ordinal) {
                mStrokeStyle.set(ss);
                break;
            }
        }
    }
    
    public Color        getFillColor()                  { return mFillColor.get(); }
    public void         setFillColor(Color c)           { mFillColor.set(c); }
    public String       getXMLfillColor()               { return mFillColor.asString(); }
    public void         setXMLfillColor(String xml)     { mFillColor.setFromString(xml); }
    
    public Color        getTextColor()                  { return mTextColor.get(); }
    public void         setTextColor(Color c)           { mTextColor.set(c); }
    public String       getXMLtextColor()               { return mTextColor.asString(); }
    public void         setXMLtextColor(String xml)     { mTextColor.setFromString(xml); }
    
    public Color        getStrokeColor()                { return mStrokeColor.get(); }
    public void         setStrokeColor(Color c)         { mStrokeColor.set(c); }
    public String       getXMLstrokeColor()             { return mStrokeColor.asString(); }
    public void         setXMLstrokeColor(String xml)   { mStrokeColor.setFromString(xml); }
        
    public Font         getFont()               { return mFont.get(); }
    public void         setFont(Font font)      { mFont.set(font); }
    public String       getXMLfont()            { return mFont.asString(); }
    public void         setXMLfont(String xml)  { mFont.setFromString(xml); }


    
    /** 
     * The first time a TextBox is created for edit, it may not have been laid out
     * by it's parent, which is where it normally gets it's location.  This 
     * initializes the location of the TextBox for first usage.  The default
     * impl here centers the TextBox in the LWComponent.
     */
    public void initTextBoxLocation(TextBox textBox) {
        textBox.setBoxCenter(getWidth() / 2,
                             getHeight() / 2);
    }

    public LWContainer getParent() {
        return this.parent;
    }


    protected void setParent(LWContainer newParent) {

        if (DEBUG.UNDO) System.err.println("*** SET-PARENT: " + newParent + " for " + this);

        
        //final boolean linkNotify = (!mXMLRestoreUnderway && parent != null);
        if (parent == newParent) {
            // This is normal.
            // (e.g., one case: during undo of reparenting operations)
            //if (DEBUG.Enabled) Util.printStackTrace("redundant set-parent in " + this + "; parent=" + newParent);
            return;
        }

        if (newParent.hasAncestor(this)) {
            Util.printStackTrace("ATTEMPTED PARENT LOOP " + this + " can't make a child our parent: " + newParent);
            return;
        }
        
        parent = newParent;
//         if (linkNotify && mLinks.size() > 0)
//             for (LWLink link : mLinks)
//                 link.notifyEndpointReparented(this);
    }
    
    //protected void reparentNotify(LWContainer parent) {}

    public void setSyncSource(LWComponent source) {

        if (true) return; // all dynamic data syncing disabled for now as per Melanie -- SMF 2007-11-14
        
        if (mSyncClients != null) {
            out("blowing away sync clients on syncSource set");
            // just in case
            mSyncClients.clear();
            mSyncClients = null;
        }
        mSyncSource = source;
        mSyncSource.addSyncClient(this);
    }

    public LWComponent getSyncSource() {
        return mSyncSource;
    }

    protected void addSyncClient(LWComponent c) {
        if (mSyncClients == null)
            mSyncClients = new HashSet();
        mSyncClients.add(c);
    }

    public void setStyle(LWComponent parentStyle)
    {
        mParentStyle = parentStyle;
        //parentStyle.isStyle = true;
        parentStyle.setFlag(Flag.IS_STYLE);
        if (!mXMLRestoreUnderway)       // we can skip the copy during restore
            copyStyle(parentStyle);
    }

    /** for castor persist */
    public LWComponent getStyle() {
        return mParentStyle;
    }

    public boolean isStyle() {
        return hasFlag(Flag.IS_STYLE);
        //return isStyle;
    }
    
    /** @return Boolean.TRUE if this component is serving as an active style for other objects, null otherwise */
    public Boolean getPersistIsStyle() {
        return isStyle() ? Boolean.TRUE : null;
    }
    
    public void setPersistIsStyle(Boolean b) {
        setFlag(Flag.IS_STYLE, b.booleanValue());

    }

    /** @return Boolean.TRUE if this component has marked as having the special "slide" style, null otherwise */
    public Boolean getPersistIsSlideStyled() {
        return hasFlag(Flag.SLIDE_STYLE) ? Boolean.TRUE : null;
    }
    
    public void setPersistIsSlideStyled(Boolean b) {
        setFlag(Flag.SLIDE_STYLE, b.booleanValue());

    }
    

    /** @deprecated: tmp back compat only */ public void setParentStyle(LWComponent c) { setStyle(c); }
    /** @deprecated: tmp back compat only */ public Boolean getPersistIsStyleParent() { return null; }
    /** @deprecated: tmp back compat only */ public void setPersistIsStyleParent(Boolean b) { setPersistIsStyle(b); }
    /** @deprecated: tmp back compat only */ public LWComponent getParentStyle() { return null; }


    // TODO: implement layers -- this a stop-gap for hiding LWSlides
    public int getLayer() {
        if (this.parent == null) {
            //out("parent null, layer 0");
            return 0;
        } else {
            return this.parent.getLayer();
            //int l = this.parent.getLayer();
            //out("parent " + parent + " layer is " + l);
            //return l;
        }
    }

    public int getDepth() {
        if (parent == null)
            return 0;
        else
            return parent.getDepth() + 1;
    }

    /**
     * @return 0 by default
     * the pick depth (in PickContext) must be >= what this returns for descdents of this component
     * be picked (selected, etc).  Mostly meaningful when an LWContainer subclass implements
     * and returns something > 0, tho a single component could use this to become a "background" item.
     * You can think of this as establishing a "wall" in the depth hierarchy, past which pick
     * traversals will not descend unless given a high enough pickDepth to jump the wall.
     */
    public int getPickLevel() {
        return 0;
    }


    //private static LWComponent ProxySlideComponent = new LWComponent("<global-slide-proxy>");

    /** return the component to be picked if we're picked: e.g., may return null if you only want children picked, and not the parent */
    protected LWComponent defaultPick(PickContext pc) {
        // If we're dropping something, never allow us to be picked
        // if we're a descendent of what's being dropped! (would be a parent/child loop)
        if (pc.dropping != null && pc.dropping instanceof LWContainer && hasAncestor((LWComponent)pc.dropping))
            return null;
//         else if (isDrawingSlideIcon() && getMapSlideIconBounds().contains(pc.x, pc.y)) {
//             return getEntryToDisplay().getSlide();
//         }
        else
            return defaultPickImpl(pc);
    }
    
    protected LWComponent defaultPickImpl(PickContext pc) {
        return this;
    }

    /** If PickContext.dropping is a LWComponent, return parent (as we can't take children),
     * otherwise return self
     */
    protected LWComponent defaultDropTarget(PickContext pc) {
        // TODO: if this is a system drag, dropping is null,
        // and we don't know if this is a localDrop of a node,
        // or a drop of a resource, so, for example, links
        // will incorrectly get targeted for local node system drops.
        // (tho when dropped, it'll still just get added to the parent).
        if (pc.dropping instanceof LWComponent)
            return getParent();
        else
            return this;
    }
    
    public boolean isOrphan() {
        return this.parent == null;
    }

    public boolean hasChildren() {
        return false;
    }

    /** @return false; overriding impl's should return true if this
     * component has children, and those children are always fully contained
     * within the bounds of the parent */
    public boolean fullyContainsChildren() {
        return false;
    }
    
    public boolean hasChild(LWComponent c) {
        return false;
    }

    public boolean isManagedLocation() {
        return (parent != null && parent.isManagingChildLocations()) || (isSelected() && isAncestorSelected());
    }

    public boolean isManagingChildLocations() {
        return false;
    }

    /** @return true - A single component always "has content" -- subclasses override to provide varying semantics */
    public boolean hasContent() {
        return true;
    }

//     /** @return false by default */
//     public boolean isImageNode() {
//         return false;
//     }

    /** @return false by default */
    public boolean isTextNode() {
        return false;
    }
    

    /**
     * Although unsupported on LWComponents (must be an LWContainer subclass to support children),
     * this method appears here for typing convenience and debug.  If a non LWContainer subclass
     * calls this, it's a no-op, and a diagnostic stack trace is dumped to the console.
     */
    public void addChild(LWComponent c) {
        Util.printStackTrace(this + ": can't take children; ignored new child: " + c);
    }

    /** default action: redirect to pasteChildren */
    public void pasteChild(LWComponent c) {
        pasteChildren(new Util.SingleIterator(c));
    }

    /** default action: redirects to dropChildren */
    public void dropChild(LWComponent c) {
        dropChildren(new Util.SingleIterator(c));
    }
        
//     /** default action: pasteChild */
//     public void dropChild(LWComponent c) {
//         pasteChild(c);
//     }


    /**
     * Although unsupported on LWComponents (must be an LWContainer subclass to support children),
     * this method appears here for typing convenience and debug.  If a non LWContainer subclass
     * calls this, it's a no-op, and a diagnostic stack trace is dumped to the console.
     */
    public void addChildren(Iterable<LWComponent> iterable) {
        Util.printStackTrace(this + ": can't take children; ignored iterable: " + iterable);
    }

    /** default action: addChildren */
    public void pasteChildren(Iterable<LWComponent> iterable) {
        addChildren(iterable);
    }

    /** default action: pasteChildren */
    public void dropChildren(Iterable<LWComponent> iterable) {
        pasteChildren(iterable);
    }

    /** return true if this component is only a "virutal" member of the map:
     * It may report that it's parent is in the map, but that parent doesn't
     * list the component as a child (so it will never be drawn or traversed
     * when handling the entire map).
     */
    public boolean isMapVirtual() {
        return getParent() == null || !getParent().hasChild(this);
    }

    /** @return 0 -- override to support children */
    public int numChildren() {
        return 0;
    }
    
    public java.util.List<LWComponent> getChildList()
    {
        return java.util.Collections.EMPTY_LIST;
    }

    public Collection<LWComponent> getChildren()
    {
        return java.util.Collections.EMPTY_LIST;
    }


    /** @return: always null */
    public LWComponent getChild(int index) {
        return null;
    }
    
    public boolean hasPicks() {
        return hasChildren() || hasEntries();
    }

    /** ordered for drawing and picking */
    private final class SlideIconIter implements Iterator<LWSlide>, Iterable<LWSlide>
    {
        int nextIndex;
        LWSlide nextSlide;
        LWSlide onTop;
        DrawContext dc;
        LWPathway activePathway;
        LWPathway.Entry activeEntry;
        
        private SlideIconIter() {
            //System.out.println("\nSlideIter, entries=" + mEntries.size());
            advance();
        }

        private void advance() {
            //out("advance; nextIndex =" + nextIndex);
            nextSlide = null;
            int i = nextIndex;
            for (; i < mEntries.size(); i++) {
                final LWPathway.Entry e = mEntries.get(i);
                //out("inspecting index " + i + " " + e);
                if (e.hasVisibleSlide()) {
                    final LWSlide slide = e.getSlide();
                    if (activePathway == null && slide.isSelected()) {
                        onTop = slide;
                        //} else if (slide.getEntry().pathway == activePathway) {
                    } else if (slide.getPathwayEntry() == activeEntry) {
                        onTop = slide;
                    } else {
                        nextSlide = slide;
                        break;
                    }
                }
            }
            
            nextIndex = i + 1;

            // if we're at the end, provide the selected (if there was one)
            if (nextSlide == null) {
                nextSlide = onTop;
                onTop = null;
            }
        }
        
        public boolean hasNext() {
            final boolean t = (nextSlide != null);
            //out("hasNext " + t);
            if (nextIndex > 100) {
                Util.printStackTrace("loop");
                return false;
            }
            return t;
            //return nextSlide != null;
        }
        
        public LWSlide next() {
            if (nextSlide == null) {
                if (DEBUG.Enabled) Util.printStackTrace(this + " next at end of SlideIter; entries= + mEntries");
                return null;
            }
            final LWSlide s = nextSlide;
            advance();
            //out("return " + s);
            return s;
        }
        
        public void remove() { throw new UnsupportedOperationException(); }

        public Iterator<LWSlide> iterator() {
            // reset when re-used
            nextIndex = 0;
            nextSlide = null;
            onTop = null;
            if (dc != null && dc.isPresenting()) {
                activePathway = VUE.getActivePathway();
                //activeEntry = VUE.getActiveEntry();
            } else {
                activePathway = null;
                //activeEntry = null;
            }
            activeEntry = VUE.getActiveEntry();
            advance();
            return this;
        }
    }

    /** @return the slides for drawing as slide icons in the current picking and drawing order */
    private final Iterable<LWSlide> seenSlideIcons(DrawContext dc) {
//         if (mEntries == null || mEntries.size() == 0) {
//             // this is sort of overkill, as we shouldn't even be calling this if hasEntries is false
//             return Util.EmptyIterable;
//         } else
        if (mEntries.size() == 1) {
            final LWPathway.Entry e = mEntries.get(0);
            if (e.hasVisibleSlide())
                return new Util.SingleIterator(e.getSlide());
            else
                return Util.EmptyIterable;
        } else {
            mVisibleSlideIconIterator.dc = dc;
            return mVisibleSlideIconIterator;
        }
        //return new SlideIter();
    }


    /**
     * @return a list, to be traversed in reverse order.  If a new list needs to be constructed,
     * it will dumped into stored, which will be returned.  Otherwise, an internal list may be returned.
     */
    public List<LWComponent> getPickList(PickContext pc, List<LWComponent> stored)
    {
        Iterable<LWSlide> seenSlides = null;
        if (pc.root != this && hasEntries() && (seenSlides = seenSlideIcons(pc.dc)) != Util.EmptyIterable) {
            // todo performance: see LWTraversal for comments: change impl to return a ReverseListIterator, etc.a
            stored.clear();
            stored.addAll(getChildren());
            for (LWSlide s : seenSlides)
                stored.add(s);
            return stored;
        } else
            return (List) getChildren();
    }
    
    
    
    public java.util.Iterator<LWComponent> getChildIterator() {
        return tufts.Util.EmptyIterator;
    }

    /** The default is to get all ChildKind.PROPER children (backward compatability)
     * This impl always returns an empty list.  Subclasses that can have proper
     * children provide the impl for that
     */
    public Collection<LWComponent> getAllDescendents() {
        // Default is only CHILD_PROPER, and by definition,
        // LWComponents have no proper children.
        // return getAllDescendents(CHILD_PROPER);
        return java.util.Collections.EMPTY_LIST;
    }    

    public Collection<LWComponent> getAllDescendents(final ChildKind kind) {
        if (kind == ChildKind.PROPER)
            return java.util.Collections.EMPTY_LIST;
        else
            return getAllDescendents(kind, new java.util.ArrayList(), Order.TREE);
    }
    
    public Collection<LWComponent> getAllDescendents(final ChildKind kind, final Collection<LWComponent> bag) {
        return getAllDescendents(kind, bag, Order.TREE);
    }
    
    public Collection<LWComponent> getAllDescendents(final ChildKind kind, final Collection<LWComponent> bag, Order order) {
        return bag;
    }
    

    /** for tracking who's linked to us */
    void addLinkRef(LWLink link)
    {
        if (DEBUG.UNDO) out(this + " adding link ref to " + link);
        if (mLinks == null)
            mLinks = new ArrayList(4);
        if (mLinks.contains(link)) {
            //tufts.Util.printStackTrace("addLinkRef: " + this + " already contains " + link);
            if (DEBUG.Enabled) Log.warn("addLinkRef: " + this + " already contains " + link);
        } else {
            mLinks.add(link);
            notify(LWKey.LinkAdded, link); // informational only event
        }
    }
    /** for tracking who's linked to us */
    void removeLinkRef(LWLink link)
    {
        if (DEBUG.EVENTS||DEBUG.UNDO) out(this + " removing link ref to " + link);
        if (mLinks == null || !mLinks.remove(link))
            throw new IllegalStateException("removeLinkRef: " + this + " didn't contain " + link);
        clearHidden(HideCause.PRUNE);
        notify(LWKey.LinkRemoved, link); // informational only event
    }
    
    /** @return us all the links who have us as one of their endpoints */
    public List<LWLink> getLinks(){
        return mLinks == null ? Collections.EMPTY_LIST : mLinks;
    }
    
    /** @return all LWComponents directly connected to this one: for most components, this
     * is just all the LWLink's that connect to us.  For LWLinks, it's mainly it's endpoints,
     * plus also any LWLink that may be directly connected to the link itself
     */
    public Collection<? extends LWComponent> getLinked() {
        // returning mLinks is an optimization, but requireds
        // subclasses to override this method also if want to change
        // the impl.
        return getLinks();
        
        //return getLinked(new ArrayList(mLinks.size()));
        //return Collections.unmodifiableList(mLinks);
    }
    
    public Collection<LWComponent> getLinked(Collection bag) {
        bag.addAll(getLinks());
        return bag;
    }

    /** @return a list of every component connected to this one via links, including the links themselves */
    public Collection<LWComponent> getLinkChain() {
        return getLinkChain(new HashSet());
    }
    
    /**
     * @return a list of every component connected to this one via links, including the links themselves
     * @param bag - the collection to store the results in.  Any component already in the bag will not
     * have it's outbound links followed -- this provides inherent loop protection.
     * Note that this collection isn't a Set of some kind, components will appear in the bag more than once.
     * (Once for every time they were visited).
     */
    public Collection<LWComponent> getLinkChain(Collection bag)
    {
        if (!bag.add(this)) {
            // already added to the set with all connections -- don't process again            
            return bag;
        }
        
        for (LWComponent c : getLinked())
            c.getLinkChain(bag);

        return bag;
    }

    public Rectangle2D.Float getFanBounds() {
        return getFanBounds(null);

    }

    /** @return the union of the bounds of the current component, all connected links, and all far endpoints
     * of those links.
     */
    public Rectangle2D.Float getFanBounds(Rectangle2D.Float rect)
    {
        if (rect == null)
            rect = getMapBounds();
        else
            rect.setRect(getMapBounds());
            
        for (LWLink link : getLinks()) {
            final LWComponent head = link.getHead();
            final LWComponent tail = link.getTail();

            rect.add(link.getPaintBounds());
            
            if (head != this) {
                if (head != null)
                    rect.add(head.getPaintBounds());
            } else if (tail != this) {
                if (tail != null)
                    rect.add(tail.getPaintBounds());
            } 
        }
        return rect;
    }

    public Rectangle2D.Float getCenteredFanBounds() {
        return expandToCenteredBounds(getFanBounds());
    }
    

    /** get bounds that are centered on this node that fully include the given bounds */
    public Rectangle2D.Float expandToCenteredBounds(Rectangle2D.Float r) {
        // expand the given rectangle in all directions such that the distance
        // from our center point of this component to each edge is the same.

        final float cx = getMapCenterX();
        final float cy = getMapCenterY();

        final float topDiff = cy - r.y;
        final float botDiff = (r.y + r.height) - cy;
        final float leftDiff = cx - r.x;
        final float rightDiff = (r.x + r.width) - cx;

        if (topDiff > botDiff) {
            // expand below us
            r.height = topDiff * 2;
        } else if (botDiff > topDiff) {
            // expand above us
            r.y = cy - botDiff;
            r.height = botDiff * 2;
        }
        if (leftDiff > rightDiff) {
            // expand to the right
            r.width = leftDiff * 2;
        } else if (rightDiff > leftDiff) {
            // expand to the left
            r.x = cx - rightDiff;
            r.width = rightDiff * 2;
        }

        return r;
    }

    
    /** @return a list of all LWComponents at the far end of any links that are connected to us */
    public Collection<LWComponent> getLinkEndPoints() {
        // default uses a set, in case there are multiple links to the same endpoint
        return getLinkEndPoints(new HashSet(getLinks().size()));
    }

    public Collection<LWComponent> getLinkEndPoints(Collection bag)
    {
        for (LWLink link : getLinks()) {
            final LWComponent head = link.getHead();
            final LWComponent tail = link.getTail();
            if (head != this) {
                if (head != null)
                    bag.add(head);
            } else if (tail != this) {
                if (tail != null)
                    bag.add(tail);
            }
        }
        return bag;
    }

    /*
     * Return an iterator over all link endpoints,
     * which will all be instances of LWComponent.
     * If this is a LWLink, it should include it's
     * own endpoints in the list.

    public java.util.Iterator<LWComponent> getLinkEndpointsIterator()
    {
        return
            new java.util.Iterator<LWComponent>() {
                java.util.Iterator i = getLinkRefs().iterator();
                public boolean hasNext() {return i.hasNext();}
		public LWComponent next()
                {
                    LWLink l = (LWLink) i.next();
                    LWComponent head = l.getHead();
                    LWComponent tail = l.getTail();
                    
                    // Every link, as it's connected to us, should have us as one of
                    // it's endpoints -- so return the opposite endpoint.  TODO: now
                    // that links can have null endpoints, this iterator can return null
                    // -- hasNext will have to get awfully fancy to handle this.
                    
                    if (head == LWComponent.this)
                        return tail;
                    else
                        return head;
                }
		public void remove() {
		    throw new UnsupportedOperationException();
                }
            };
    }
     */    
    /*
     * Return all LWComponents connected via LWLinks to this object.
     * Included everything except LWLink objects themselves (unless
     * it's an endpoint -- a link to a link)
     *
     * todo opt: this is repaint optimization -- when links
     * eventually know their own bounds (they know real connection
     * endpoints) we can re-do this as getAllConnections(), which
     * will can return just the linkRefs and none of the endpoints)
     */
    /*
    public java.util.List getAllConnectedNodes()
    {
        java.util.List list = new java.util.ArrayList(mLinks.size());
        java.util.Iterator i = mLinks.iterator();
        while (i.hasNext()) {
            LWLink l = (LWLink) i.next();
            if (l.getComponent1() != this)
                list.add(l.getComponent1());
            else if (l.getComponent2() != this) // todo opt: remove extra check eventually
                list.add(l.getComponent2());
            else
                // todo: actually, I think we want to support these
                throw new IllegalStateException("link to self on " + this);
            
        }
        return list;
    }
    */
    
    /* include all links and far endpoints of links connected to this component 
    public java.util.List getAllConnectedComponents()
    {
        List list = new java.util.ArrayList(mLinks.size());
        for (LWLink l : mLinks) {
            list.add(l);
            if (l.getHead() != this)
                list.add(l.getHead());
            else if (l.getTail() != this) // todo opt: remove extra check eventually
                list.add(l.getTail());
            else
                // todo: actually, I think we want to support these
                throw new IllegalStateException("link to self on " + this);
            
        }
        return list;
    }
*/
    
    /** get all links to us + to any descendents */
    // TODO: return immutable versions
    public List getAllLinks() {
        return getLinks();
    }

    public int countLinksTo(LWComponent c)
    {
        if (c == null || mLinks == null)
            return 0;
        
        int count = 0;
        for (LWLink link : mLinks)
            if (link.hasEndpoint(c))
                count++;
        return count;
    }

    /** @return true if there are any links between us and the given component */
    public boolean hasLinkTo(LWComponent c)
    {
        if (c == null || mLinks == null)
            return false;
        
        for (LWLink link : mLinks)
            if (link.hasEndpoint(c))
                return true;
        return false;
    }

    /** @return true of this component has any connections (links) to the given component.
     *  LWLink overrides to include it's endpoints in the definition of "connected" to.
     */
    public boolean isConnectedTo(LWComponent c) {
        return hasLinkTo(c);
    }
    
        
    public int countCurvedLinksTo(LWComponent c)
    {
        int count = 0;
        for (LWLink link : getLinks())
            if (link.hasEndpoint(c) && link.isCurved())
                count++;
        return count;
    }
    
    /** supports ensure link paint order code */
    protected  LWComponent getParentWithParent(LWContainer parent)
    {
        if (getParent() == parent)
            return this;
        if (getParent() == null)
            return null;
        return getParent().getParentWithParent(parent);
    }

    /** @return a collection of our ancestors.  default impl returns a list with nearest ancestor first */
    public List<LWComponent> getAncestors() {
        return (List) getAncestors(new ArrayList(8));
    }

    protected Collection<LWComponent> getAncestors(Collection bag) {
        if (parent != null) {
            bag.add(parent);
            return parent.getAncestors(bag);
        } else
            return bag;
    }

    public boolean hasAncestor(LWComponent c) {
        LWComponent parent = getParent();
        if (parent == null)
            return false;
        else if (c == parent)
            return true;
        else
            return parent.hasAncestor(c);
    }

    public boolean hasAncestorOfType(Class clazz) {
        return getParentOfType(clazz) != null;
    }
    

    /** @return the first ancestor, EXCLUDING this component (starting with the parent), that is of the given type, or null if none found */
    public LWComponent getParentOfType(Class clazz) {
        return getParentOfType(clazz, null);
    }
    
    /** never ascend above root */
    public LWComponent getParentOfType(Class clazz, LWComponent root) {
        LWComponent parent = getParent();
        if (parent == null)
            return null;
        else
            return parent.getAncestorOfType(clazz, root);
    }
    
    /** @return the first ancestor, INCLUDING this component, that is of the given type, or null if none found */
    // TODO: including this component is confusing...
    public LWComponent getAncestorOfType(Class clazz) {
        return getAncestorOfType(clazz, null);
    }
    
    /** never ascend above root */
    public LWComponent getAncestorOfType(Class clazz, LWComponent root) {
        if (clazz.isInstance(this))
            return this;
        else if (this == root)
            return null;
        else
            return getParentOfType(clazz, root);
    }

    public LWComponent getTopMostAncestorOfType(Class clazz) {
        return getTopMostAncestorOfType(clazz, null);
    }
    
    /** never ascend above root */
    public LWComponent getTopMostAncestorOfType(Class clazz, LWComponent root) {
        LWComponent topAncestor = getAncestorOfType(clazz, root);
        LWComponent nextAncestor = topAncestor;

        if (nextAncestor != null) {
            for (;;) {
                nextAncestor = nextAncestor.getParentOfType(clazz, root);
                if (nextAncestor != null)
                    topAncestor = nextAncestor;
                else
                    break;
                //if (DEBUG.PICK) out("nextAncestor of type " + clazz + ": " + topAncestor);
            }
        }
        
        return topAncestor;
    }
    

    /** @return by default, return the class object as returned by getClass().  Subclasses can override to provide differentiation between runtime sub-types.
     * E.g., a node class could return getClass() by default, but the constant string "textNode" for runtime instances that we
     * want the tool code to treat is coming from a different class.  Also note that supported property bits for
     * all instances with a given type token should be the same.
     */
    public Object getTypeToken() {
        // todo: should really return null if we detect this is an instance of an anonymous class
        // -- we don't want to be duplicating and using a style holder an instance of an anon
        // glass that might be overriding god knows what and affecting property setting/getting
        // Not that this will probably hurt anything: it'll never be referenced by a VueTool,
        // so we'll never see it even if it winds up in the typed style cache.
        return getClass();
    }

    /** @return the viewer margin in pixels when we're the focal -- default is 30 */
    public int getFocalMargin() {
        return 30;
    }
    
    protected void takeScale(double newScale) {
        if (DEBUG.LAYOUT) out("takeScale " + newScale);
        this.scale = newScale;
    }
    
    protected void setScale(double newScale)
    {
        if (this.scale == newScale)
            return;
        final double oldScale = this.scale;
        //if (DEBUG.LAYOUT) out("setScale " + newScale);
        //if (DEBUG.LAYOUT) tufts.Util.printClassTrace("tufts.vue", "setScale " + scale);
        takeScale(newScale);
        
        // can only do this via debug inspector right now, and is causing lots of
        // suprious events during init:
        //if (LWLink.LOCAL_LINKS && !mXMLRestoreUnderway)
        if (!mXMLRestoreUnderway)
            notify(LWKey.Scale, oldScale); // todo: make scale a real property
        
        updateConnectedLinks(null);
        //System.out.println("Scale set to " + scale + " in " + this);
    }
    
    /**
     * @return the scale value relative to it's parent.  So for a 50% scale in it's parent,
     * it just returns 0.5.  E.g., this would mean if the parent was also scaled at 50%,
     * the net on-map visible scaled size of the component would be 25%.
     */
    public double getScale()
    {
        return this.scale;
    }
    
    /** @return the on-map scale at 100% map scale (the concatentation of our scale plus all parent scales) */
    public double getMapScale()
    {
        if (getParent() == null)
            return getScale();
        else
            return getParent().getMapScale() * getScale();
    }

    /** Convenience for returning float */ public final float getScaleF() { return (float) getScale(); }
    /** Convenience for returning float */ public final float getMapScaleF() { return (float) getMapScale(); }
    
    

    public Size getMinimumSize() {
        return MinSize;
    }
    
    public void setFrame(Rectangle2D r)
    {
        setFrame((float)r.getX(), (float)r.getY(),
                 (float)r.getWidth(), (float)r.getHeight());
    }


    /**
     * Default impl just call's setSize, then setLocation.  You
     * may want to override if want to constrain in some way,
     * such as to underlying content (e.g., an image).
     */
    public void setFrame(float x, float y, float w, float h)
    {
        if (DEBUG.LAYOUT) out("*** setFrame " + x+","+y + " " + w+"x"+h);

        setSize(w, h);
        setLocation(x, y);

        /*
        Object old = new Rectangle2D.Float(this.x, this.y, getWidth(), getHeight());
        takeLocation(x, y);
        takeSize(w, h);
        updateConnectedLinks();
        notify(LWKey.Frame, old);
        */
    }

    /** default calls setFrame -- override to provide constraints */
    public void userSetFrame(float x, float y, float w, float h) {
        setFrame(x, y, w, h);
    }
    
    protected void userSetFrame(float x, float y, float w, float h, MapMouseEvent e) {
        userSetFrame(x, y, w, h);
    }

    // todo: handle via disabling a location property?
    public void setMoveable(boolean moveable) {
        setFlag(Flag.FIXED_LOCATION, !moveable);
    }
        
    public boolean isMoveable() {
        return hasFlag(Flag.FIXED_LOCATION) == false;
    }

    /** @return true if this component is "owned" by the pathway -- e.g., a slide that only appears as an icon */
    public boolean isPathwayOwned() {
        return false;
    }
    
        

    //private boolean linkNotificationDisabled = false;
    protected void takeLocation(float x, float y) {
        if (DEBUG.LAYOUT) {
            out("takeLocation " + x + "," + y);
            //if (DEBUG.META) tufts.Util.printStackTrace("takeLocation");
        }
        this.x = x;
        this.y = y;
    }
    
//     public void userTranslate(float dx, float dy) {
//         translate(dx, dy);
//     }
    
    /** Translate this component within it's parent by the given amount */
    public void translate(float dx, float dy) {
        setLocation(this.x + dx,
                    this.y + dy);
    }

    /** Translate this component within it's parent by the given amount -- quietly w/out generating events */
    public void takeTranslation(float dx, float dy) {
        takeLocation(this.x + dx,
                     this.y + dy);
    }
    

    /** translate across the map in absolute map coordinates */
    public void translateOnMap(double dx, double dy)
    {
        // If this node exists in a scaled context, which means it's parent is scaled or
        // the parent itself is in a scaled context, we need to adjust the dx/dy for
        // that scale. The scale of this object being "dragged" by the call to
        // translateOnMap is irrelevant -- here we're concerned with it's location in
        // it's parent, not it's contents.  So we need to beef up the translation amount
        // by the context scale so drags across the map will actually stay with the
        // mouse.  E.g., if this object exists in a parent scaled down 50% (scale=0.5),
        // to move this object 2 pixels to the right in absolute top-level map
        // coordinates, we need to change it's internal location within it's parent by 4
        // pixels (2 / 0.5 = 4) to have that show up on the map (when itself displayed
        // at 100% scale) as a movement of 4 pixels.

        final double scale = getParent().getMapScale();
        if (scale != 1.0) {
            dx /= scale;
            dy /= scale;
        }
        
        translate((float) dx, (float) dy);
        
    }
    
    /** set the absolute map location -- meant to be overriden for special cases (e.g., the special selection group) */
    public void setMapLocation(double x, double y) {
        throw new UnsupportedOperationException("unimplemented in " + this);
//         final double scale = getMapScale();
//         out("map scale: " + scale);
//         if (scale != 1.0) {
//             final double oldMapX = getMapX();
//             final double oldMapY = getMapY();
//             final double dx = (x - oldMapX) * scale;
//             final double dy = (y - oldMapY) * scale;
//             setLocation((float) (oldMapX + dx),
//                         (float) (oldMapY + dy));
//         } else
//             setLocation((float) x, (float) y);
    }
    
    /**
     * Set the location of this object within it's parent. E.g., if the parent is a group or a slide,
     * setLocation(0,0) would move the component to the upper left corner of it's parent.  If the
     * parent is a map, (0,0) has no special meaning as the origin of Maps, while it does exist,
     * has no special meaning when they draw.
     */
    public void setLocation(float x, float y) {
        setLocation(x, y, this, true);
    }

    
    /** Special setLocation to permit event notification during coordinate system changes for objects not yet added to the map */
    protected void setLocation(float x, float y, LWComponent hearableEventSource, boolean issueMapLocationChangeCalls)
    {
        if (this.x == x && this.y == y)
            return;
        
        final Point2D.Float oldValue = new Point2D.Float(this.x, this.y);
        takeLocation(x, y);
        
        //if (!linkNotificationDisabled)
        //    updateConnectedLinks();
        
        if (hearableEventSource != this)
            hearableEventSource.notifyProxy(new LWCEvent(hearableEventSource, this, LWKey.Location, oldValue));
        else //if (hearableEventSource != null) // if null, skip event delivery
            notify(LWKey.Location, oldValue);

        //        if (issueMapLocationChangeCalls && parent != null) {
        if (issueMapLocationChangeCalls) {

            // NEED TO DEAL WITH COORDINATE SYSTEM CHANGES
            // And need to be able to capture old map location from our OLD parent
            // during reparenting....

            // reparenting may want to force a location in the new parent, at it's
            // current map location, but relative to the new parent's location,
            // even if it's about to be moved/laid-out elsewhere, so that once
            // we get here, the below code should always work.  Or, we could
            // even have establishLocalCoordinates call us here with extra info... (oldMapX/oldMapY)
            // or, we could implement the general setMapLocation and have establishLocalCoords call that...
            
            // This code only works if we're moving within a single parent: no coordinate system changes!

            // Would be better to merge this somehow with notifyHierarchChanged?
            
            final double scale;
            if (parent != null)
                scale = parent.getMapScale(); // we move within the scale of our parent
            else
                scale = 1.0;
            if (DEBUG.LAYOUT) out("notifyMapLocationChanged: using scale " + scale);
            notifyMapLocationChanged(this,
                                     (x - oldValue.x) * scale,
                                     (y - oldValue.y) * scale);
        } else {
            // this always needs to happen no matter what, even during undo
            // (e.g., the shape of curves isn't stored anywhere -- always needs to be recomputed)
            //if (!linkNotificationDisabled)
            if (updatingLinks())
                updateConnectedLinks(this);
        }
    }

    /**
     * Tell all links that have us as an endpoint that we've
     * moved or resized so the link knows to recompute it's
     * connection points.
     */
    protected void updateConnectedLinks(LWComponent movingSrc)
    {
        //if (!linkNotificationDisabled) // todo: if still end up using this feature, need to pass this bit on down to children
        if (updatingLinks())
            if (mLinks != null && mLinks.size() > 0)
                for (LWLink link : mLinks)
                    link.notifyEndpointMoved(movingSrc, this);
    }

//     boolean isFocal;
//     void setFocal(boolean isFocal) {
//         this.isFocal = isFocal;
//     }
    
    
    /** a notification to the component that it's absolute map location has changed by the given absolute map dx / dy */
    // todo: may be better named ancestorMoved or ancestorTranslated or some such
    protected void notifyMapLocationChanged(LWComponent movingSrc, double mdx, double mdy) {
        //if (!linkNotificationDisabled) // todo: if still end up using this feature, need to pass this bit on down to children
        if (updatingLinks())
            updateConnectedLinks(movingSrc);
    }

    protected void notifyMapScaleChanged(double oldParentMapScale, double newParentMapScale) {}

    /** A notification to the component that it or some ancestor is about to change parentage */
    public void notifyHierarchyChanging() {}
    
    /** A notification to the component that it or some ancestor changed parentage */
    public void notifyHierarchyChanged() {
        if (mLinks != null && mLinks.size() > 0)
            for (LWLink link : mLinks)
                link.notifyEndpointHierarchyChanged(this);
        
    }
    
    public final void setLocation(double x, double y) {
        setLocation((float) x, (float) y);
    }
    public final void setLocation(Point2D p) {
        setLocation((float) p.getX(), (float) p.getY());
    }

    /** default calls setLocation -- override to provide constraints */
    public void userSetLocation(float x, float y) {
        setLocation(x, y);
    }
    
    public void setCenterAt(Point2D p) {
        setLocation((float) p.getX() - getWidth()/2,
                    (float) p.getY() - getHeight()/2);
    }

    public Point2D getLocation()
    {
        return new Point2D.Float(getX(), getY());
    }
    
    /** set component to this many pixels in size, quietly, with no event notification */
    protected void takeSize(float w, float h)
    {
        //if (this.width == w && this.height == h)
        //return;
        if (DEBUG.LAYOUT) out("*** takeSize (LWC)  " + w + "x" + h);
        this.width = w;
        this.height = h;
    }

    protected float mAspect = 0;
    public void setAspect(float aspect) {
        mAspect = aspect;
        if (DEBUG.IMAGE) out("setAspect " + aspect);
    }
    
    /** set component to this many pixels in size */
    public void setSize(float w, float h)
    {
        if (this.width == w && this.height == h)
            return;
        if (DEBUG.LAYOUT) out("*** setSize  (LWC)  " + w + "x" + h);
        final Size old = new Size(width, height);

        if (mAspect > 0) {
            Size constrained = ConstrainToAspect(mAspect, w, h);
            w = constrained.width;
            h = constrained.height;
        }
        
        if (w < MIN_SIZE) w = MIN_SIZE;
        if (h < MIN_SIZE) h = MIN_SIZE;
        takeSize(w, h);
        if (getParent() != null && !(getParent() instanceof LWMap))
            getParent().layout();
        updateConnectedLinks(null);
        if (!isAutoSized())
            notify(LWKey.Size, old); // todo perf: can we optimize this event out?
    }

    public static Size ConstrainToAspect(double aspect, double w, double h)
    {
        // Given width & height are MINIMUM size: expand to keep aspect
            
        if (w <= 0) w = 1;
        if (h <= 0) h = 1;
        double tmpAspect = w / h; // aspect we would have if we did not constrain it

        //if (DEBUG.IMAGE) Log.debug("ConstrainToAspect " + tmpAspect);
        // a = w / h
        // w = a*h
        // h = w/a
//         if (DEBUG.PRESENT || DEBUG.IMAGE) {
//             out("keepAspect=" + mAspect);
//             out(" tmpAspect=" + tmpAspect);
//         }
        //             if (h == this.height) {
        //                 out("case0");
        //                 h = (float) (w / mAspect);
        //             } else if (w == this.width) {
        //                 out("case1");
        //                 w = (float) (h * mAspect); 
        //             } else
        if (tmpAspect > aspect) {
            //out("case2: expand height");
            h = w / aspect;
        } else if (tmpAspect < aspect) {
            //out("case3: expand width");
            w = h * aspect;
        }
        //else out("NO ASPECT CHANGE");

        return new Size(w, h);

        /*
          if (false) {
          if (h == this.height || tmpAspect < mAspect)
          h = (float) (w / mAspect);
          else if (w == this.width || tmpAspect > mAspect)
          w = (float) (h * mAspect);
          } else {
          if (tmpAspect < mAspect)
          h = (float) (w / mAspect);
          else if (tmpAspect > mAspect)
          w = (float) (h * mAspect);
          }
        */
                
    }

    
    /** default calls setSize -- override to provide constraints */
    public void userSetSize(float w, float h) {
        setSize(w, h);
    }
    protected void userSetSize(float w, float h, MapMouseEvent e) {
        userSetSize(w, h);
    }
        
    /* set on screen visible component size to this many pixels in size -- used for user set size from
     * GUI interaction -- takes into account any current scale factor
     * (do we still need this? I think this should be deprecated -- SMF)
     */

//     public void setAbsoluteSize(float w, float h)
//     {
//         if (true||DEBUG.LAYOUT) out("*** setAbsoluteSize " + w + "x" + h);
//         setSize(w / getScaleF(), h / getScaleF());
//         //setSize(w / getMapScaleF(), h / getMapScaleF());
//     }
    
    /** for XML restore only -- issues no event updates */
    public void setX(float x) { this.x = x; }
    /** for XML restore only -- issues no event updates */
    public void setY(float y) { this.y = y; }
    /** for castor restore -- will not trigger any events */
    public void setWidth(float w) { this.width = w; }
    /** for castor restore -- will not trigger any events */
    public void setHeight(float h) { this.height = h; }
    

    /*
     * getMapXXX methods are for values in absolute map positions and scales (needed for VUE.RELATIVE_COORDS == true)
     * getScaledXXX methods are for VUE.RELATIVE_COORDS == false, tho I think we can get rid of them?  -- SMF
     *
     * "Map" values are absolute on-screen values that are true for any component in a map rendered at 100% scale (the size & location)
     * (better naming scheme might be "getRenderXXX" or "getAbsoluteXX" ?)
     */
    
    public float getX()         { return this.x; }
    public float getY()         { return this.y; }
    public float getWidth()     { return this.width; }
    public float getHeight()    { return this.height; }

    /** @return the width inside the local parent (width * scale) */
    public float getLocalWidth()       { return (float) (this.width * getScale()); }
    /** @return the height inside the local parent (height * scale) */
    public float getLocalHeight()      { return (float) (this.height * getScale()); }
    
    /** @return on-map width when viewed at 100% */
    public float getMapWidth()          { return (float) (this.width * getMapScale()); }
    /** @return on-map height when viewed at 100% */
    public float getMapHeight()         { return (float) (this.height * getMapScale()); }

    /** @return local width including any border stroke ((width + stroke) * scale) */
    public float getLocalBorderWidth() { return (float) ((this.width + mStrokeWidth.get()) * getScale()); }
    /** @return local height including any border stroke ((height + stroke) * scale) */
    public float getLocalBorderHeight() { return (float) ((this.height + mStrokeWidth.get()) * getScale()); }
    


    protected double getMapXPrecise()
    {
        if (parent == null) {
            //if (DEBUG.Enabled && this instanceof LWMap == false)
            //    Util.printStackTrace("fetching mapX for unparented non-map: " + this);
            return getX();
        } else {
            return parent.getMapXPrecise() + getX() * parent.getMapScale();
        }
    }
    protected double getMapYPrecise() {
        if (parent == null) {
            return getY();
        } else {
            if (parent == this) { // DEBUG
                Util.printStackTrace("PARENT LOOP " + this);
                return getY();
            }
            return parent.getMapYPrecise() + getY() * parent.getMapScale();
        }
    }

    public float getMapX() {
        return (float) getMapXPrecise();
    }
    
    public float getMapY() {
        return (float) getMapYPrecise();
    }

    /** @return center x of the component in absolute map coordinates */
    public float getMapCenterX() {
        return getMapX() + getMapWidth() / 2;
    }
    /** @return center y of the component in absolute map coordinates */
    public float getMapCenterY() {
        return getMapY() + getMapHeight() / 2;
    }

//     // these two don't handle scale properly yet: need to adjust for parent scales...
//     protected float getCenterX(LWContainer ancestor) {
//         return (float) getAncestorX(ancestor) + getScaledWidth() / 2;
//     }
//     protected float getCenterY(LWContainer ancestor) {
//         return (float) getAncestorY(ancestor) + getScaledHeight() / 2;
//     }

//     // these two don't handle scale properly yet
//     public float getLinkConnectionX(LWContainer ancestor) {
//         //return getCenterX(ancestor);
//         return (float) getAncestorX(ancestor) + getScaledWidth() / 2;
//     }
//     public float getLinkConnectionY(LWContainer ancestor) {
//         //return getCenterY(ancestor);
//         return (float) getAncestorY(ancestor) + getScaledHeight() / 2;
//     }

    protected void getLinkConnectionCenterRelativeTo(Point2D.Float point, LWContainer relative)
    {
        //if (relative == null) Util.printStackTrace("null relative for " + this + ": " + relative);

        if (relative == this) {

            if (DEBUG.Enabled)
                Util.printStackTrace("debug: " + this + " is computing link connetion center relative to itself");
            //final float scale = getMapScaleF();
            
            point.x = getZeroCenterX();
            point.y = getZeroCenterY();
            //point.x = getZeroCenterX() * scale;
            //point.y = getZeroCenterY() * scale;
            
        } else if (relative == null) {
      //} else if (relative == null || relative == parent) {

            // if relative is null, just return available local data w/out accessing the parent.
            // This can happen normally during init.

            if (this instanceof LWLink) {
                point.x = getZeroCenterX();
                point.y = getZeroCenterY();
            } else {
                // works for connecting to something scaled for a map link to a scaled map-node:
                //point.x = getX() + getZeroCenterX() * scale;
                //point.y = getY() + getZeroCenterY() * scale;
                // works for connecting to inside a scaled context (e.g., a scaled down on-map slide)
                point.x = getX() + getZeroCenterX();
                point.y = getY() + getZeroCenterY();
            }

        } else if (true || ROTATE_TEST) {

            // can we construct a relativing x-hierarchy transformer in one pass?
            // e.g., a combination of transformDown's then I guess transformUp's (would need that),
            // on an AffineTransform, should produce a x-hierarchy transformer.

            // Anyway, this is the safest method possible: transform up to the map,
            // then back down to the other context, taking no shortcuts.

            point.x = getZeroCenterX();
            point.y = getZeroCenterY();
            if (DEBUG.LINK) out("    ZeroCenter: " + point);
            transformZeroToMapPoint(point, point);
            if (DEBUG.LINK) out("     MapCenter: " + point);
            relative.transformMapToZeroPoint(point, point);
            if (DEBUG.LINK) out("RelativeCenter: " + point + " to " + relative);
            
        } else {

            // note that this is the NET scale -- scale effective at the map level
            // -- THIS ISN'T CORRECT -- we need the scale relative to relative...
            final float scale = getMapScaleF();

            if (this instanceof LWLink) {
                // todo: consider getMapX/Y on LWLink override to return getParent().getMapX/Y (need to check all calls tho...)
                point.x = parent.getMapX() + getZeroCenterX() * scale;
                point.y = parent.getMapY() + getZeroCenterY() * scale;
            } else {
                point.x = getMapX() + getZeroCenterX() * scale;
                point.y = getMapY() + getZeroCenterY() * scale;
            }

            // point now has map coords -- now make relative to desired component
            // (the x/y needed if drawn in the component, that produces the same
            // ultimate map location).  Normally, relative should always
            // be one of our ancestors, as this is for special link code that
            // should only ever be interested in an ancestor value, tho we compute
            // it generically just in case.


            if (DEBUG.Enabled) {
                if (relative != null && !hasAncestor(relative)) {
                    // only if not the special invisible link endpoint, which has no parent (thus no ancestors)
                    if (getClass().getEnclosingClass() != LinkTool.LinkModeTool.class) {
                        //String msg = "debug: " + this + " is computing link connetion center relative to a non-ancestor: " + relative;
                        String msg = "non-ancestor: " + relative + " used as parent-relative of " + this;
                        if (DEBUG.META)
                            Util.printStackTrace(msg);
                        else
                            Log.debug(msg);
                    }
                }
            }
            

            relative.transformMapToZeroPoint(point, point);
        }
    }

    /** @return our center in our zero-based coordinate space: e.g., 1/2 our width.  Links
     * will compute differentely, as their zero-based coordinate space is their parent's space (same as local space) */
    protected float getZeroCenterX() {
        return getWidth() / 2;
    }
    protected float getZeroCenterY() {
        return getHeight() / 2;
    }
    


    //-----------------------------------------------------------------------------
    // experimental relatve-to-a-given-ancestor coord fetchers
    // TODO: NOT WORTH THE TROUBLE RIGHT NOW OF USING THE ANCESTOR OPTIMIZATION:
    // Just get the freakin mapx of the desired relative-to component --
    // someday those values may be cached in the object/transform anyway.
    // Oh tho -- I think in LWLink we need the mapX of US, plus the mapX of the target
    // (if KEEP the ancestor code, implement generically so can pass in any value: e.g, LWLink.mCurveCenterX)
    //-----------------------------------------------------------------------------
    

    protected double getAncestorX(LWContainer ancestor) {
        if (ancestor == parent) // quick check for the common case
            return getX();
        else if (parent == null) {
             Util.printStackTrace("didn't find ancestor " + ancestor + " for " + this);
             return getX();
        } else
            return parent.getAncestorX(ancestor) + getX() * parent.getMapScale();
    }
    
    protected double getAncestorY(LWContainer ancestor) {
        if (ancestor == parent) // quick check for the common case
            return getY();
        else if (parent == null) {
             Util.printStackTrace("didn't find ancestor " + ancestor + " for " + this);
             return getY();
        } else
            return parent.getAncestorY(ancestor) + getY() * parent.getMapScale();
    }
    

//     protected double ancestorY(double y, LWContainer ancestor) {
//         if (ancestor == parent) // quick check for the common case
//             return y;
//         else if (parent == null) {
//              Util.printStackTrace("didn't find ancestor " + ancestor + " for " + this);
//              return y;
//         } else
//             return parent.ancestorY(y, ancestor) + getY() * parent.getMapScale();
//     }
    

    
    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------
    

    /** @return uri
     * returns an unique uri for a component. If component already has one it is returned else an new uri is created and returned.
     * At present uris will be created through rdf index
     */
    public URI getURI() {
        //if (isStyle) return null;
        if (uri == null) {
            try {
                uri = new URI(edu.tufts.vue.rdf.RDFIndex.getUniqueId());
                edu.tufts.vue.rdf.VueIndexedObjectsMap.setID(uri, this);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t, "Failed to create an uri for  "+label);
            }
        }
        return uri;
    }
    
    public void setURI(URI uri) {
//         if (isStyle) {
//             VUE.Log.warn("attempt to set URI on a style: " + this + "; uri=" + uri);
//             return;
//         }
        this.uri = uri; 
    }
    
     /* Methods to persist url through castor
     * We don't want to save URI object
     *
    */
    public void setURIString(String URIString) {
//         if (isStyle) {
//             VUE.Log.warn("attempt to set URIString on a style: " + this + "; uriString=" + uri);
//             return;
//         }
        try {
            uri = new URI(URIString);
            edu.tufts.vue.rdf.VueIndexedObjectsMap.setID(uri,this);
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t, "Failed to set an uri for  "+label);
        }
        
    }
    
    public String getURIString() {
        return getURI().toString();
    }
    
    /*
    public void setShape(Shape shape)
    {
        throw new UnsupportedOperationException("unimplemented setShape in " + this);
    }
    */


    /** @return our shape, full transformed into map coords and ultimate scale when drawn at 100% map zoom
     * this is used for portal clipping, and will be imperfect for some scaled shapes, such as RountRect's
     * This only works for raw shapes that are RectangularShapes -- other Shape types just return the map bounds
     * (e.g., a link shape) */
    public Shape getMapShape()
    {
        // Will not work for shapes like RoundRect when scaled -- e..g, corner scaling will be off
            
        final Shape s = getZeroShape();
        //        if (getMapScale() != 1f && s instanceof RectangularShape) { // todo: do if any transform, not just scale
        if (s instanceof RectangularShape) {
            // todo: cache this: only need to updaate if location, size or scale changes
            // (Also, on the scale or location change of any parent!)
            RectangularShape rshape = (RectangularShape) s;
            rshape = (RectangularShape) rshape.clone();
            AffineTransform a = getZeroTransform();
            Point2D.Float loc = new Point2D.Float();
            a.transform(loc, loc);
            rshape.setFrame(loc.x, loc.y,
                            rshape.getWidth() * a.getScaleX(),
                            rshape.getHeight() * a.getScaleY());
            //System.out.println("TRANSFORMED SHAPE: " + rshape + " for " + this);
            return rshape;
        } else {
            return getMapBounds();
        }
    }


    
    /** @return the raw shape of this object, not including any shape (the stroke is laid on top of the raw shape).
        This is the zero based non-scaled shape (always at 0,0) */
    private Shape getShape()
    {
        return getZeroShape();
    }


    protected Rectangle2D.Float mZeroBounds; // don't pre-allocate -- won't be used by overriding impl's
    /** @return the raw, zero based, non-scaled shape; default impl returns same as getZeroBounds */
    public Shape getZeroShape() {
        if (mZeroBounds == null)
            mZeroBounds = new Rectangle2D.Float();
        mZeroBounds.width = getWidth();
        mZeroBounds.height = getHeight();
        return mZeroBounds;
    }
    
    /**
     * @return the raw, zero based, non-scaled bounds.
     *
     * Altho the x/y of the rectangle will normally be 0,0 (suggesting we could just use
     * a size object here), that's not always the case: a component who shares it's
     * coordinate space with it's parent (such as a link) will usually have a non-zero
     * x/y in the zero bounds.
     */
    protected Rectangle2D.Float getZeroBounds() {
        return new Rectangle2D.Float(0, 0, getWidth(), getHeight());
    }

//     protected Size getZeroPaintSize() {

//         final float strokeWidth = getStrokeWidth()l
        
//         if (strokeWidth > 0) {
//             return new Size(getWidth() + strokeWidth, getHeight() + strokeWidth);
//         } else {
//             return new Size(getWidth(), getHeight());
//         }
//     }
    
    
    /** @return the PARENT based bounds  -- this is the local component x,y  width*scale,height*scale, where scale
     * is any local scale this component has (not the total map scale: the scale that includes the scaling of all ancestors) */
    public Rectangle2D.Float getLocalBounds() {
        return new Rectangle2D.Float(getX(), getY(), getLocalWidth(), getLocalHeight());
    }

    /** @return the layout bounds -- this is the local bounds, plus an extra "hangoff" decorations that are not considered
     * part of the formal bounds of the object.  When the object is the focal, these items are not displayed.
     */
    public Rectangle2D.Float getLayoutBounds() {
        return getLocalBounds();
    }

    /** @return the local (parent-based) border bounds */
    public Rectangle2D.Float getLocalBorderBounds() {
        return addLocalStrokeToBounds(getLocalBounds());
    }
    

    /** @return the PARENT based, non-scaled bounds including all extra-shape artifacts, such as a stroke */
    public Rectangle2D.Float getLocalPaintBounds() {
        return addStrokeToBounds(getLocalBounds(), 0f);
    }
    
    /** @return getMapBounds() -- map-coord (absolute) bounds of the stroke shape (not including any stroke width) */
    public final Rectangle2D.Float getBounds()
    {
        return getMapBounds();
    }

    /** @return map-coord (absolute) bounds of the stroke shape (not including any stroke width) */
    public Rectangle2D.Float getMapBounds()
    {
        return new Rectangle2D.Float(getMapX(), getMapY(), getMapWidth(), getMapHeight());
    }
    

    /**
     * Return absolute map bounds for hit detection & clipping.  This will vary
     * depenending on current stroke width, if in a visible pathway,
     * etc.
     */
    public Rectangle2D.Float getPaintBounds()
    {
        if (inDrawnPathway()) {
            Rectangle2D.Float b = addStrokeToBounds(getMapBounds(), LWPathway.PathBorderStrokeWidth);
            if (farthestVisibleSlideCorner != null)
                b.add(farthestVisibleSlideCorner);
            return b;
        } else
            return addStrokeToBounds(getMapBounds(), 0);
    }

    /** @return bounds to use when this is the focal */
    public Rectangle2D.Float getFocalBounds() {
        // do not include any slide icons
        return addStrokeToBounds(getMapBounds(), 0);
    }
    

    /**
     * Return absolute map bounds including any border stroke -- used by Groups.
     */
    public Rectangle2D.Float getBorderBounds()
    {
        return addStrokeToBounds(getMapBounds(), 0);
    }
    

    
    /** take the given map bounds, and add the scaled stroke width plus any extra if given */
    protected Rectangle2D.Float addStrokeToBounds(Rectangle2D.Float r, float extra)
    {
        float strokeWidth = getStrokeWidth() + extra;
        
        if (strokeWidth > 0) {
            strokeWidth *= getMapScale();
            final float exteriorStroke = strokeWidth / 2;
            r.x -= exteriorStroke;
            r.y -= exteriorStroke;
            r.width += strokeWidth;
            r.height += strokeWidth;
        }
        
        // we need this adjustment for repaint optimzation to
        // work properly -- would be a bit cleaner to compensate
        // for this in the viewer
        //if (isIndicated() && STROKE_INDICATION.getLineWidth() > strokeWidth)
        //    strokeWidth += STROKE_INDICATION.getLineWidth();

        
        return r;
    }

    protected Rectangle2D.Float addLocalStrokeToBounds(Rectangle2D.Float r)
    {
        float strokeWidth = getStrokeWidth();
        
        if (strokeWidth > 0) {
            strokeWidth *= getScale();
            final float exteriorStroke = strokeWidth / 2;
            r.x -= exteriorStroke;
            r.y -= exteriorStroke;
            r.width += strokeWidth;
            r.height += strokeWidth;
        }
        return r;
    }
    
    /** @return an AffineTransform that when applied to a graphics context, will have us drawing properly
     * relative to this component, including any applicable scaling.  So after this is applied,
     * 0,0 will draw in the upper left hand corner of the component */
    //create and recursively set a transform to get from the Map to this object's coordinate space
    // note: structure is same in the different transform methods
    // TODO OPT: can cache this transform: if track all ancestor hierarcy, location AND scale changes,
    // can skip recomputing it each time.
    public final AffineTransform getZeroTransform() {
        return loadZeroTransform(_zeroTransform);
//         final AffineTransform a;
//         if (parent == null) {
//             a = new AffineTransform();
//         } else {
//             a = parent.getZeroTransform();
//         }
//         return transformDownA(a);
    }

    protected final AffineTransform loadZeroTransform(final AffineTransform a) {
        if (parent == null) {
            a.setToIdentity();
            return transformDownA(a);
        } else {
            return transformDownA(parent.loadZeroTransform(a));
        }
    }
    

    /**
     * @return the transform that takes us from the given ancestor down to our local coordinate space/scale
     * @param ancestor -- the ancestor to get a transform relative to.  If null, this will return the
     * same result as getLocalTransform (relative to the map)
     */
    protected AffineTransform getRelativeTransform(LWContainer ancestor) {

        if (parent == ancestor || parent == null)
            return transformDownA(new AffineTransform());
        else
            return transformDownA(parent.getRelativeTransform(ancestor));
    }

    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------
    //
    // transformDownA + transformDownG are the two core routines that everything
    // ultimately uses -- e.g., placing a test rotation in these methods makes
    // it work everywhere that's using the transformation code (drawing, picking,
    // and link connections)
    //
    //-----------------------------------------------------------------------------
    //-----------------------------------------------------------------------------

    private final static boolean ROTATE_TEST = false;

    private static final int RotSteps = 180;
    private static final double RotStep = Math.PI * 2 / RotSteps;
    private static int RotCount = 0;
    
    /** set by model clients (e.g., MapViewer) for the zoomed rollover component */
    protected static double ZoomRolloverScale;

    /**
     * Transform the given AffineTransform down from our parent to us, the child.
     */
    protected AffineTransform transformDownA(final AffineTransform a)
    {
        if (ROTATE_TEST && parent instanceof LWMap) {
            
            // rotate around center (relative to map-bounds)
            
            final double hw = getWidth() / 2;
            final double hh = getHeight() / 2;
            a.translate(getX() + hw, getY() + hh);
            a.scale(scale, scale);
            a.rotate(Math.PI / 8);
            a.translate(-hw, -hh);
            
        } else {

            if (isZoomedFocus) {
                if (false && this instanceof LWSlide) {
                    final double scale = SlideIconScale * 2;
                    a.scale(scale, scale);
                } else {

                    // Zoom on-center.
                    
                    // To make this simple, we first translate to the local center (our
                    // center location in parent coords, compensating for any of our own
                    // scale), then apply the new zoomed scale, then translate back out
                    // by our raw width.  This isn't done often, so no point in over
                    // optimizing.

                    final double halfWidth = getWidth() / 2;
                    final double halfHeight = getHeight() / 2;
                    final double ourScale = getScale();

                    // Translate to local center:
                    a.translate(getX() + halfWidth * ourScale,
                                getY() + halfHeight * ourScale);

                    if (DEBUG.VIEWER) {
                        // note that due to nature of this testing uber-hack, the more
                        // children something has, the faster it rotates.
                        a.rotate(RotStep * RotCount);
                        if (++RotCount >= RotSteps)
                            RotCount = 0;
                    }
                    
                    // Set the super-zoom scale:
                    a.scale(ZoomRolloverScale, ZoomRolloverScale);
                    a.translate(-halfWidth, -halfHeight);
                }
            } else {
                
                //-------------------------------------------------------
                // This is the default, standard case:
                //-------------------------------------------------------
                
                a.translate(this.x, this.y);
                if (this.scale != 1)
                    a.scale(this.scale, this.scale);
            }
            
        }
        return a;
    }

//     // When working on transformDownA, comment this code in, and comment out transformDownG
//     /** Must include overrides of all AffineTransform methods used in transformDownA */
//     private static final class GCAffineProxy extends AffineTransform {
//         private Graphics2D g;
//         @Override
//         public final void translate(double x, double y) { g.translate(x, y); }
//         @Override
//         public final void scale(double xs, double ys) { g.scale(xs, ys); }
//         @Override
//         public final void rotate(double t) { g.rotate(t); }
//     }

//     private static final GCAffineProxy GCAP = new GCAffineProxy();

//     /** transform relative to the child after already being transformed relative to the parent */
//     protected void transformDownG(final Graphics2D g) {
//         GCAP.g = g; // not exactly thread-safe -- this temporary while we work on this code (cut/paste duplicate when done)
//         transformDownA(GCAP);
//     }
    


    /** transform relative to the child after already being transformed relative to the parent */
    protected void transformDownG(final Graphics2D a)
    {
        //-----------------------------------------------------------------------------
        // NOTE THAT THE CODE IN THIS METHOD IS A PURE DUPLICATE OF transformDownA
        // That is, it is literally a cut & paste of the body of transformDownA.
        // The only difference is that our argument is of type Graphics2D, instead
        // of AffineTransform -- we only call methods common to both classes.
        // (and we don't return the passed in argument in this method)
        //-----------------------------------------------------------------------------
        
        if (ROTATE_TEST && parent instanceof LWMap) {
            
            // rotate around center (relative to map-bounds)
            
            final double hw = getWidth() / 2;
            final double hh = getHeight() / 2;
            a.translate(getX() + hw, getY() + hh);
            a.scale(scale, scale);
            a.rotate(Math.PI / 8);
            a.translate(-hw, -hh);
            
        } else {

            if (isZoomedFocus) {
                if (false && this instanceof LWSlide) {
                    final double scale = SlideIconScale * 2;
                    a.scale(scale, scale);
                } else {

                    // Zoom on-center.
                    
                    // To make this simple, we first translate to the local center (our
                    // center location in parent coords, compensating for any of our own
                    // scale), then apply the new zoomed scale, then translate back out
                    // by our raw width.  This isn't done often, so no point in over
                    // optimizing.

                    final double halfWidth = getWidth() / 2;
                    final double halfHeight = getHeight() / 2;
                    final double ourScale = getScale();

                    // Translate to local center:
                    a.translate(getX() + halfWidth * ourScale,
                                getY() + halfHeight * ourScale);

                    if (DEBUG.VIEWER) {
                        // note that due to nature of this testing uber-hack, the more
                        // children something has, the faster it rotates.
                        a.rotate(RotStep * RotCount);
                        if (++RotCount >= RotSteps)
                            RotCount = 0;
                    }
                    
                    // Set the super-zoom scale:
                    a.scale(ZoomRolloverScale, ZoomRolloverScale);
                    a.translate(-halfWidth, -halfHeight);
                }
            } else {
                
                //-------------------------------------------------------
                // This is the default, standard case:
                //-------------------------------------------------------
                
                a.translate(this.x, this.y);
                if (this.scale != 1)
                    a.scale(this.scale, this.scale);
            }
            
        }
    }


    /** Will transform all the way from the the map down to the component, wherever nested/scaled.
     * So drawing at 0,0 will draw in the upper left of the component. */
    public void transformZero(final Graphics2D g) {
        
        // todo: need a relative to parent transform only for cascading application during drawing
        // (and ultimate picking when impl is optimized)
            
        if (parent == null) {
            ;
        } else {
            parent.transformZero(g);
        }
        
        transformDownG(g);

    }

    public Point2D.Float transformMapToZeroPoint(Point2D.Float mapPoint) {
        return (Point2D.Float) transformMapToZeroPoint(mapPoint, mapPoint);
    }
    
    /**
     * @param mapPoint, a point in map coordinates to transform to local coordinates
     * @param zeroPoint the destination Point2D to place the resulting transformed coordinate -- may be
     * the same object as mapPoint (it will be written over)
     * @return the transformed point (will be zeroPoint if transformed, mapPoint if no transformation was needed,
     * although mapPoint x/y values should stil be copied to zeroPoint)
     */
    public Point2D transformMapToZeroPoint(Point2D.Float mapPoint, Point2D.Float zeroPoint) {

// This doesn't work if we're a link!
//         if (!isZoomedFocus && scale == 1.0 && parent instanceof LWMap && !ROTATE_TEST) { // OPTIMIZATION
//             zeroPoint.x = mapPoint.x - this.x;
//             zeroPoint.y = mapPoint.y - this.y;
//             return zeroPoint;
//         }
        
        try {
            getZeroTransform().inverseTransform(mapPoint, zeroPoint);
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            Util.printStackTrace(e);
        }
        return zeroPoint;
    }


    protected Point2D transformZeroToMapPoint(Point2D.Float zeroPoint, Point2D.Float mapPoint) {

// This doesn't work if we're a link!
//         if (!isZoomedFocus && scale == 1.0 && parent instanceof LWMap && !ROTATE_TEST) { // OPTIMIZATION
//             mapPoint.x = zeroPoint.x + this.x;
//             mapPoint.y = zeroPoint.y + this.y;
//             return mapPoint;
//         }
        
        getZeroTransform().transform(zeroPoint, mapPoint);
        return mapPoint;
    }
    

    /**
     * @param mapRect -- incoming rectangle to transform to be relative to 0,0 of this component
     * @param zeroRect -- result is placed here -- will be created if is null
     * @return zeroRect
     *
     * E.g., if the incoming mapRect was from map coords 100,100->120,120, and this component was at 100,100,
     * the resulting zeroRect in this case would be 0,0->20,20 (assuming no scale or rotation).
     *
     */
    protected Rectangle2D transformMapToZeroRect(Rectangle2D mapRect, Rectangle2D zeroRect)
    {
        if (zeroRect == null)
            zeroRect = (Rectangle2D) mapRect.clone(); // simpler than newInstace, tho we won't need the data-copy in the end

        // If want to handle rotation, we'll need to transform each corner of the
        // rectangle separately, generating Polygon2D (which sun never implemented!)  or
        // a GeneralPath, in either case changing this method to return a Shape.  Better
        // would be to keep a cached rotated map Shape in each object, tho that means
        // solving the general problem of making sure we're updated any time our
        // ultimate map location/size/scale/rotation, etc, changes, which of course
        // changes if any of those values change on any ancestor.  If we did that, we'd
        // also be able to fully cache the _zeroTransform w/out having to recompute it
        // for each call just in case.  (Which would mean getting rid of this method
        // entirely and using the map shape in intersects, etc) Of course, crap, we
        // couldn't do all this for links, could we?  Tho maybe via special handing in an
        // override... tho that would only work for the transform, not the shape, as the
        // parent shape is useless to the link. FYI, currently, we only use this 
        // for doing intersections of links and non-rectangular nodes
        
//         final double[] points = new double[8];
//         final double width = zeroRect.getWidth();
//         final double height = zeroRect.getHeight();
//         // UL
//         points[0] = zeroRect.getX();
//         points[1] = zeroRect.getY();
//         // UR
//         points[2] = points[0] + width;
//         points[3] = points[1];
//         // LL
//         points[4] = points[0];
//         points[5] = points[1] + height;
//         // LR
//         points[6] = points[0] + width;
//         points[7] = points[1] + height;


        // Now that we know the below code can never handle rotation, we also might as
        // well toss out using the transform entirely and just use getMapScale /
        // getMapX/Y to mod a Rectangle2D.Float directly... Tho then our zoomed rollover
        // mod, which is in the transformDown code would stop working for rectangle
        // picking & clipping, tho we shouldn't need rect picking for zoomed rollovers,
        // (only point picking) and the zoomed rollover always draws no matter what (in
        // the MapViewer), so that may be moot, tho would need to fully test to be sure.
        // All of the this also applies to transformZeroToMapRect below.

        final AffineTransform tx = getZeroTransform();
        final double[] points = new double[4];
        points[0] = mapRect.getX();
        points[1] = mapRect.getY();
        points[2] = points[0] + mapRect.getWidth();
        points[3] = points[1] + mapRect.getHeight();
        try {
            tx.inverseTransform(points, 0, points, 0, 2);
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            Util.printStackTrace(e);
        }

        zeroRect.setRect(points[0],
                         points[1],
                         points[2] - points[0],
                         points[3] - points[1]
                         );

        return zeroRect;
        
    }

    /**
     * This will take the given rectangle in local coordinates, and transform it
     * into map coordinates.  The passed in Rectangle2D will be modified
     * and returned.
     */
    public Rectangle2D transformZeroToMapRect(Rectangle2D zeroRect) {
        return transformZeroToMapRect(zeroRect, zeroRect);
    }
    
    /**
     * This will take the given zeroRect rectangle in local coordinates, and transform it
     * into map coordinates, setting mapRect and returning it.  If mapRect is null,
     * a new rectangle will be created and returned.
     */
    public Rectangle2D transformZeroToMapRect(Rectangle2D zeroRect, Rectangle2D mapRect)
    {
        final AffineTransform tx = getZeroTransform();
        final double[] points = new double[4];
        
        points[0] = zeroRect.getX();
        points[1] = zeroRect.getY();
        points[2] = points[0] + zeroRect.getWidth();
        points[3] = points[1] + zeroRect.getHeight();
        tx.transform(points, 0, points, 0, 2);

        if (mapRect == null)
            mapRect = new Rectangle2D.Float();
        
        mapRect.setRect(points[0],
                        points[1],
                        points[2] - points[0],
                        points[3] - points[1]
                        );
        
        return mapRect;

        
        
// Non-rotating & non-transform using version:        
//         final double scale = getMapScale();
//         // would this be right? scale the x/y first?
//         if (scale != 1) {
//             rect.x *= scale;
//             rect.y *= scale;
//             rect.width *= scale;
//             rect.height *= scale;
//         }
//         if (this instanceof LWLink) {
//             // todo: eventually rewrite this routine entirely to use the transformations
//             // (will need that if ever want to handle rotation, as well as to skip this
//             // special case for links).
//             rect.x += getParent().getMapX();
//             rect.y += getParent().getMapY();
//         } else {
//             rect.x += getMapX();
//             rect.y += getMapY();
//         }
        
    }
                
    
    /**
     * Default implementation: checks bounding box
     * Subclasses should override and compute via shape.
     * INTERSECTIONS always intersect based on map bounds, as opposed to contains, which tests a local point.
     */
    public final boolean intersects(Rectangle2D mapRect)
    {
        return intersectsImpl(mapRect);
//         final boolean hit = intersectsImpl(rect);
//         //if (DEBUG.PAINT) System.out.println("INTERSECTS " + fmt(rect) + " " + (hit?"YES":"NO ")
//         //+ " for " + fmt(getPaintBounds()) + " " + this);
//         return hit;
    }
    
    /** default impl intersects the render/paint bounds, including any borders (we use this for draw clipping as well as selection) */
    protected boolean intersectsImpl(Rectangle2D mapRect) {
        //if (DEBUG.CONTAINMENT) System.out.println("INTERSECTS " + Util.fmt(rect));
        final Rectangle2D bounds = getPaintBounds();
        final boolean hit = mapRect.intersects(bounds);
        if (DEBUG.PAINT && DEBUG.PICK) System.out.println("INTERSECTS " + fmt(mapRect) + " " + (hit?"YES":"NO ") + " for " + fmt(bounds) + " of " + this);
        //Util.printClassTrace("tufts.vue.LW", "INTERSECTS " + this);
        return hit;
    }
    

    /** @return true if this component currently requires painting and intersects the master paint region */
    public boolean requiresPaint(DrawContext dc)
    {
        if (dc.skipDraw == this)
            return false;

        // always draw the focal
        if (dc.focal == this)
            return true;

        if (isZoomedFocus())
            return false;
        
        // if filtered, don't draw, unless has children, in which case
        // we need to draw just in case any of the children are NOT filtered.
        if (isHidden() || (isFiltered() && !hasChildren()))
            return false;

        // Not currently used:
        //if (getLayer() > dc.getMaxLayer())
        //    return false;

        if (dc.isClipOptimized()) {

            //-----------------------------------------------------------------------------
            // Returning true when parent.fullContainsChildren() is true will prevent a
            // ton of intersects calls (and subsequent map-bounds computations involving
            // transform fetches and their application to rectangles) when we have lots
            // objects that are going to need drawing no matter what (e.g., lots of
            // slide icons visible and we're zoomed out), tho it will cause the pixel
            // drawing code to be invoked more often that it needs to when zoomed in.
            // It's a basic trade-off.
            //
            // NOT checking this optimizes us for fast painting when zoomed way in on
            // sub-components of the map/slides (e.g., during presentations), and that's
            // the current chosen priority.
            //
            // As either method can safely be used (checking or not checking), we allow
            // the check, but only if it looks like we're reasonably zoomed-out.  Either
            // method is okay because this check is just an early way to say something
            // requires painting, and it's always okay to paint -- the worse that
            // happens is something off screen is painted, and we waste time in the
            // graphics pipeline having it clipped.  Essentailly, when run, this check
            // just lets us skip the intersects call below.
            //
            // The reason this is meaningful is we only get here if the parent
            // has already determined it needs to paint, and if that's the case,
            // and it fully contains it's children, if the parent is likely to
            // be fully on-screen, we should just go ahead and paint all the children.
            
            if (parent != null && dc.zoom <= 1.0 && parent.fullyContainsChildren())
                return true;
            
            //-----------------------------------------------------------------------------

            if (hasEntries()) {
                
                // for now, if we have ANY pathway entries, we say we have to draw, so
                // that if they're needed, any slide icons will draw (even if the parent
                // node is clipped: this is because the slide icons lie outside the
                // node).  Really, we only need to return true here if we're on any
                // pathways that are visible & showing slide icons, and we have at least
                // one actual slide.  Todo: cache that info so we can check it here
                // (such a bit would need to update when any pathway visibility changes,
                // or it's show icons bit flips, or our pathway memberships change,
                // etc....)

                // Also, once we'd determined there were slide icons to draw, we'd
                // also want to check each of their bounds to see if they're within
                // the master clip rect, tho right now they all scrunch together,
                // so that would be a bit of overkill.
                
                return true;
            }
            
            if (intersects(dc.getMasterClipRect()))
                return true;
            
//             if (isDrawingSlideIcon())
//                 return getMapSlideIconBounds().intersects(dc.getMasterClipRect());
//             else
                return false;
        } else {
            
            // Not clip optimized means don't bother to check the master clip to see if
            // we need to draw: just always draw everything no matter where it is
            // (unless it was hidden, etc).  E.g., if we're drawing to generate an
            // image, or drawing a zoomed rollover, we already know we just need to draw
            // the component no matter what.
            
            // More examples: when drawing raw, always draw everything, don't check
            // against the master "map" clip rect, as that's only for drawing map
            // elements (e.g., we may be drawing a LWComponent that's a decoration or
            // GUI element, like a navigation node, or a master slide background).
            
            return true;
        }

    }
    


//     /**
//      * We divide area around the bounding box into 8 regions -- directly
//      * above/below/left/right can compute distance to nearest edge
//      * with a single subtract.  For the other regions out at the
//      * corners, do a distance calculation to the nearest corner.
//      * Behaviour undefined if x,y are within component bounds.
//      */
//     public float distanceToEdgeSq(float x, float y)
//     {
//         float ex = this.x + getWidth();
//         float ey = this.y + getHeight();

//         if (x >= this.x && x <= ex) {
//             // we're directly above or below this component
//             return y < this.y ? this.y - y : y - ey;
//         } else if (y >= this.y && y <= ey) {
//             // we're directly to the left or right of this component
//             return x < this.x ? this.x - x : x - ex;
//         } else {
//             // This computation only makes sense following the above
//             // code -- we already know we must be closest to a corner
//             // if we're down here.
//             float nearCornerX = x > ex ? ex : this.x;
//             float nearCornerY = y > ey ? ey : this.y;
//             float dx = nearCornerX - x;
//             float dy = nearCornerY - y;
//             return dx*dx + dy*dy;
//         }
//     }

//     public Point2D nearestPoint(float x, float y)
//     {
//         float ex = this.x + getWidth();
//         float ey = this.y + getHeight();
//         Point2D.Float p = new Point2D.Float(x, y);

//         if (x >= this.x && x <= ex) {
//             // we're directly above or below this component
//             if (y < this.y)
//                 p.y = this.y;
//             else
//                 p.y = ey;
//         } else if (y >= this.y && y <= ey) {
//             // we're directly to the left or right of this component
//             if (x < this.x)
//                 p.x = this.x;
//             else
//                 p.x = ex;
//         } else {
//             // This computation only makes sense following the above
//             // code -- we already know we must be closest to a corner
//             // if we're down here.
//             float nearCornerX = x > ex ? ex : this.x;
//             float nearCornerY = y > ey ? ey : this.y;
//             p.x = nearCornerX;
//             p.y = nearCornerY;
//         }
//         return p;
//     }

//     public float distanceToEdge(float x, float y)
//     {
//         return (float) Math.sqrt(distanceToEdgeSq(x, y));
//     }

//     /**
//      * Return the square of the distance from x,y to the center of
//      * this components bounding box.
//      */
//     public float distanceToCenterSq(float x, float y)
//     {
//         float cx = getCenterX();
//         float cy = getCenterY();
//         float dx = cx - x;
//         float dy = cy - y;
//         return dx*dx + dy*dy;
//     }
    
//     public float distanceToCenter(float x, float y)
//     {
//         return (float) Math.sqrt(distanceToCenterSq(x, y));
//     }
    
//     public void drawPathwayDecorations(DrawContext dc)
//     {
//         if (mPathways == null)
//             return;
        
//         if (LWPathway.PathwayAsDots || this instanceof LWLink)
//             LWPathway.drawPathwayDot(dc.create(), this);
        
//         if (!LWPathway.PathwayAsDots && isTransparent()) {
//             for (LWPathway path : mPathways) {
//                 //if (!dc.isFocused && path.isDrawn()) {
//                 if (path.isDrawn()) {
//                     path.drawPathwayBorder(dc.create(), this);
//                 }
//             }
//         }
//     }

    /** if this component is selected and we're not printing, draw a selection indicator */
    // todo: drawing of selection should be handled by the MapViewer and/or the currently
    // active tool -- not in the component code
    protected void drawSelectionDecorations(DrawContext dc) {
        if (isSelected() && dc.isInteractive()) {
            LWPathway p = VUE.getActivePathway();
            if (p != null && p.isVisible() && p.getCurrentNode() == this) {
                // SPECIAL CASE:
                // as the current element on the current pathway draws a huge
                // semi-transparent stroke around it, skip drawing our fat 
                // transparent selection stroke on this node.  So we just
                // do nothing here.
            } else {
                dc.g.setColor(COLOR_HIGHLIGHT);
                dc.g.setStroke(new BasicStroke(getStrokeWidth() + SelectionStrokeWidth));
                transformZero(dc.g);
                dc.g.draw(getZeroShape());
            }
        }
    }
    

    /** @return true if the given x/y (already transformed to our local coordinate space), is within our shape */
    public final boolean contains(float x, float y, PickContext pc) {
        if (containsImpl(x, y, pc))
            return true;
//         else if (isDrawingSlideIcon()) {
//             if (DEBUG.PICK) out("Checking slide icon bounds " + getSlideIconBounds());
//             return getSlideIconBounds().contains(x, y);
//         }
        else
            return false;
    }

    /** @return 0 means a hit, -1 a completely miss, > 0 means distance, to be sorted out by caller  */
    protected float pickDistance(float x, float y, PickContext pc) {
        return contains(x, y, pc) ? 0 : -1;
    }

    /**
     * Default implementation: checks bounding box, including any stroke width.
     * Subclasses should override for more accurate hit detection.
     */
    protected boolean containsImpl(float x, float y, PickContext pc)
    {
        final float stroke = getStrokeWidth() / 2;
        
        return x >= -stroke
            && y >= -stroke
            && x <= getWidth() + stroke
            && y <= getHeight() + stroke;
    }

    /** For using a node in a non-map context (e.g., as an on-screen button) */
    // todo: this is bounding box only: odd shapes will have imperfect hit detection
    // also, if we ever add rotation of arbitrary LWComponents, this won't handle it --
    // will need need to dump this hack and do all in LWTraversal, or have the
    // local LWComponent contains/intersects code adjust for the local transformation
    // themselves.
    public boolean containsLocalCoord(float x, float y) {
        return x >= this.x
            && y >= this.y
            && x <= (this.x+getLocalWidth())
            && y <= (this.y+getLocalHeight());
    }
    

    public static final float SlideIconScale = 0.125f;
//     private Rectangle2D.Float mSlideIconBounds;
//     public Rectangle2D.Float getSlideIconBounds() {
//         if (mSlideIconBounds == null)
//             mSlideIconBounds = computeSlideIconBounds(new Rectangle2D.Float());
//         else if (true || mSlideIconBounds.x == Float.NaN) // need a reshape/reshapeImpl trigger on move/resize to properly re-validate (wait: NaN != NaN !)
//             computeSlideIconBounds(mSlideIconBounds);
//         return mSlideIconBounds;
//     }

//     public Rectangle2D.Float getMapSlideIconBounds() {
//         Rectangle2D.Float slideIcon = (Rectangle2D.Float) getSlideIconBounds().clone();
//         final float scale = getMapScaleF();
//         // Compress the local slide icon coords into the node's scale space:
//         slideIcon.x *= scale;
//         slideIcon.y *= scale;
//         // Now make them absolute map coordintes (no longer local):
//         slideIcon.x += getMapX();
//         slideIcon.y += getMapY();
//         // Now scale down size:
//         slideIcon.width *= scale;
//         slideIcon.height *= scale;

//         return slideIcon;
//     }

    /** @return the local lower right hand corner of the component: for rectangular shapes, this is just [width,height]
     * Non-rectangular shapes can override to do something fancier. */
    protected Point2D getZeroSouthEastCorner() {
        return new Point2D.Float(getWidth(), getHeight());
    }
    private static final Point2D ZeroNorthWestCorner = new Point2D.Float();
    protected Point2D getZeroNorthWestCorner() {
        return ZeroNorthWestCorner;
    }

//     protected Rectangle2D.Float computeSlideIconBounds(Rectangle2D.Float rect)
//     {
//         // TODO: below should take into account actual slide size...
//         final float width = LWSlide.SlideWidth * SlideIconScale;
//         final float height = LWSlide.SlideHeight * SlideIconScale;

//         Point2D.Float corner = getZeroCorner();
        
//         float xoff = corner.x - 60;
//         float yoff = corner.y - 60;

//         // If shape is small, try and keep it from overlapping too much (esp the label)
//         if (xoff < getWidth() / 2f)
//             xoff = getWidth() / 2f;
//         if (yoff < getHeight() * 0.75f)
//             yoff = getHeight() * 0.75f;

//         // This can happen for wierd shapes (e.g., shield)
//         if (xoff > corner.x)
//             xoff = corner.x;
//         if (yoff > corner.y)
//             yoff = corner.y;

//         rect.setRect(xoff,
//                      yoff,
//                      width,
//                      height);
        
//         return rect;
//     }

    private Point2D.Float getSlideIconStackLocation()
    {
        final Point2D corner = getZeroSouthEastCorner();
        
        float xoff = (float) corner.getX() - 60;
        float yoff = (float) corner.getY() - 60;

        // If shape is small, try and keep it from overlapping too much (esp the label)
        if (xoff < getWidth() / 2f)
            xoff = getWidth() / 2f;
        if (yoff < getHeight() * 0.75f)
            yoff = getHeight() * 0.75f;

        // todo: can reuse getZeroCorner point2D instead of creating anew...
        return new Point2D.Float(xoff, yoff);

    }
    
    //protected final Rectangle2D debugZeroRect = new Rectangle2D.Double();

    
    /**
     * Intended for use in an LWContainer where the parent has already
     * been drawn, and the DrawContext is currently transformed to the
     * parent.  This performs the final transform for this child and
     * transforms it.
     */
    public void drawLocal(DrawContext dc)
    {
        // this will cascade to all children when they draw, combining with their calls to transformDown
        transformDownG(dc.g);

        final AffineTransform zeroTransform = DEBUG.BOXES ? dc.g.getTransform() : null;

        //if (dc.focal == this || dc.isFocused()) // prevents slide icons from appearing in portals
        if (dc.focal == this)
            drawZero(dc);
        else
            drawZeroDecorated(dc, true);

        if (DEBUG.BOXES)
            drawDebugInfo(dc, zeroTransform);
    }

    private void drawDebugInfo(DrawContext dc, AffineTransform zeroTransform) {

        if (this instanceof LWLink)
            return;
        
        dc.g.setTransform(zeroTransform);
                
        dc.setAbsoluteStroke(1);
                
        //dc.g.setColor(Color.blue);
        //dc.g.draw(debugZeroRect);
                
        // scaling testing -- draw an exactly 8x8 pixel (rendered) box
        dc.g.setColor(Color.green);
        dc.g.drawRect(0,0,7,7);

        // show the center-point to corner intersect line (debug slide icon placement):
        dc.g.setColor(Color.red);
        //dc.setAbsoluteStroke(1);
        dc.g.setStroke(STROKE_ONE);
        dc.g.draw(new Line2D.Float(new Point2D.Float(getWidth()/2, getHeight()/2), getZeroSouthEastCorner()));

        if (DEBUG.LINK && isSelected() && getLinks().size() > 0) {
            final Rectangle2D.Float pureFan = getFanBounds();
            final Rectangle2D.Float fan = getCenteredFanBounds();
            final float cx = getMapCenterX();
            final float cy = getMapCenterY();
            final Line2D xaxis = new Line2D.Float(fan.x, cy, fan.x + fan.width, cy);
            final Line2D yaxis = new Line2D.Float(cx, fan.y, cx, fan.y + fan.height);
            dc.setMapDrawing();
            dc.setAbsoluteStroke(4);
            //dc.g.setColor(getRenderFillColor(dc));
            dc.g.setColor(Color.blue);
            dc.g.draw(pureFan);

            dc.setAbsoluteStroke(2);
            dc.g.setColor(Color.red);
            dc.g.draw(fan);
            dc.g.draw(xaxis);
            dc.g.draw(yaxis);
        }
    }
    

    /**
     *
     * This is NOT the method used to draw a component during routine drawing of the
     * entire map (unless this is the map itself).  This is for directly forcing the
     * drawing or redrawing a single component at it's proper map location.  The passed
     * in DrawContext gc is expected to be transformed for drawing the top-level map
     * (minimally transformed).  If you are going to use the passed in DrawContext after
     * this call for other map drawing operations, be sure to pass in dc.create() from
     * the caller, as this call will leaves it in a generally undefined state (probably
     * rooted at the node).
     *
     */
    public void draw(DrawContext dc) {
        dc.setClipOptimized(false); // ensure all children draw even if not inside clip
        transformZero(dc.g);
        if (dc.focal == this) {
            drawZero(dc);
        } else {
            drawZeroDecorated(dc, false);
//             if (isZoomedFocus()) {
//                 // include any slide icons
//                 drawDecorated(dc);
//             } else {
//                 if (dc.drawPathways())
//                     drawPathwayDecorations(dc);
//                 drawRaw(dc);
//             }
        }
    }
    
    public void drawZero(DrawContext dc)
    {
        final AffineTransform zeroTransform = DEBUG.PDF ? dc.g.getTransform() : null;
        
        dc.checkComposite(this);
        try {
            drawImpl(dc);
        } catch (RuntimeException e) {
            Log.error("drawImpl failed: " + e);
            try {
                dc.setAlpha(0.5);
                dc.g.setColor(Color.red);
                dc.g.fill(getZeroShape());
            } catch (Throwable t) {
                Util.printStackTrace(t);
            } finally {
                throw e;
            }
        }

        if (isDeleted()) {
            // debug
            dc.setAlpha(0.5);
            dc.g.setColor(Color.yellow);
            dc.g.fill(getZeroShape());
        }
        
        if (DEBUG.PDF && DEBUG.META && this instanceof LWLink == false) {
            dc = dc.create();
            dc.g.setTransform(zeroTransform);
            dc.g.setColor(Color.blue);
            dc.g.setFont(VueConstants.FixedSmallFont);
            dc.setAbsoluteScale(1);
            final Color c1 = getFillColor();
            final Color c2 = getRenderFillColor(dc);
            dc.g.drawString(fmt(c1), 0, 10);
            if (c1 == null || !c1.equals(c2))
                dc.g.drawString(fmt(c2), 0, 20);
        }
        
    }

    /** fit and center us into the total clip bounds of the given dc -- border gap pixels will multiplied by final scale value */
    public void drawFit(DrawContext dc, int borderGap) {
        drawFit(dc, dc.getMasterClipRect(), borderGap);
    }

    /** fit and center us into the given frame */
    public void drawFit(DrawContext dc, Rectangle2D frame, int borderGap)
    {
        final Point2D.Float offset = new Point2D.Float();
        final float totalBorder = getStrokeWidth()/2 + borderGap;
        final Size size = new Size(frame);
        final double zoom = ZoomTool.computeZoomFit(size, -totalBorder, getZeroBounds(), offset);
        if (DEBUG.PDF) out("drawFit into " + fmt(frame) + " totalBorder " + totalBorder + " zoom " + zoom);
        dc.g.translate(-offset.x + frame.getX(),
                       -offset.y + frame.getY());
        dc.g.scale(zoom, zoom);
        dc.setClipOptimized(false);
        drawZero(dc);
    }
    
    
    //private static final double PathwayOnTopZoomThreshold = 1.5;
    public static final double PathwayOnTopZoomThreshold = 3;
    
    /**
     * Draw any needed pathway decorations and related slide icons,
     * before/after calling drawZero, depending on desired impl.
     */
    protected final void drawZeroDecorated(DrawContext dc, boolean drawSlides)
    {
        if (dc.drawPathways() && mPathways != null) {
            LWPathway.decorateUnder(this, dc);
            if (dc.zoom > PathwayOnTopZoomThreshold) {
                // force the over decorations to be under
                LWPathway.decorateOver(this, dc);
                drawZero(dc);
            } else {
                drawZero(dc);
                LWPathway.decorateOver(this, dc);
            }
        } else {
            drawZero(dc);
        }
        
        if (drawSlides && mEntries != null)
            drawSlideIconStack(dc);
        //else
        //farthestVisibleSlideCorner = null;
    }

//     /** If there's a pathway entry we want to be showing, return it, otherwise, null */
//     LWPathway.Entry getEntryToDisplay()
//     {
//         LWPathway path = VUE.getActivePathway();

//         if (!inPathway(path)) {
//             if (mPathways != null && mPathways.size() > 0)
//                 path = mPathways.get(0); // show the first pathway it's in if it's not in the active pathway
//             else
//                 path = null;
//         }
            
//         if (path != null && path.isShowingSlides()) {
//             final LWPathway.Entry entry = path.getCurrentEntry();
//             // This is just in case the node is in the pathway more than once: if it is,
//             // and the current entry is for this node, use that, otherwise, just
//             // use the first entry for the the node.
//             if (entry != null && entry.node == this)
//                 return entry;
//             else
//                 return path.getEntry(path.firstIndexOf(this));
//         }
//         return null;
//     }

//     public boolean isDrawingSlideIcon() {
//         final LWPathway.Entry entry = getEntryToDisplay();
//         return entry != null && !entry.isMapView;
//     }


    private Point2D.Float farthestVisibleSlideCorner;
    
    
    /** @return a slide to be drawn last, or null if none in particular */
    
    // TODO: need to do this as part of layout: need to trigger layout if any pathway
    // visibility or membership changes.  Currently, the paint bounds falls behind as
    // farthestVisibleCorner doesn't update till draw time... We special case a call to
    // this during the init (restore) layout, so at least auto-fit at startup works, but
    // if we ever hava a viewer that's implementing a constant auto-fit feature, it will
    // fall behind until we handle this in proper model/view split fashion.
    
    private final void layoutSlideIcons(DrawContext dc) {
        if (mEntries == null)
            return;

        final Point2D.Float corner = getSlideIconStackLocation();

        float xoff = corner.x;
        float yoff = corner.y;

//         if (false && dc != null && dc.isPresenting()) {
//             // if presenting, let the position the active pathway slide as the last slide in the stack            
//             for (LWSlide slide : seenSlideIcons(dc)) {
//                 slide.takeLocation(xoff, yoff);
//                 yoff += slide.getLocalHeight() / 6;
//                 xoff += slide.getLocalWidth() / 6;
//             }
//         } else {

            // if NOT presenting, leave the slides arranged in the order
            // of the pathway list (TODO: entries order isn't synced with this...)

            LWSlide lastSlide = null;
        
            for (LWPathway.Entry e : mEntries) {
                if (e.hasVisibleSlide()) {
                    final LWSlide slide = e.getSlide();
                    lastSlide = slide;
                    
                    // TODO BUG: during a presentation, if you use 'p' to change to exclusive
                    // pathway display mode and back, it actually results in the movement of the
                    // slide icons -- so unless the slide you're on happens to be the first slide
                    // icon, it will move when you do this, and the viewer isn't catching this, and
                    // the slide moves up and to the left in the middle of the presentation.  The
                    // real fix for this involves the complete reworking of the MapViewer to just
                    // be a Focal/Tree viewer, which can truly throw out all map coordinates, and
                    // just deal with wherever it's rooted in the hierachy.  We're probably 2/3 of
                    // the way there now.  Hopefully we'll get there someday... SMF 2007-11-05

                    // so if is the focal somewhere, it can know to zoom fit, tho ideally
                    // the viewer would just ignore the location on focals...
                    // slide.setLocation(xoff, yoff); // No good: if slide's parent moves, the slide moves..
                    slide.takeLocation(xoff, yoff);
                
                    final float scaledSlideWidth = slide.getLocalWidth();
                    final float scaledSlideHeight = slide.getLocalHeight();
                    yoff += scaledSlideWidth / 6;
                    xoff += scaledSlideHeight / 6;
                }
            }

            if (lastSlide != null) {
                corner.x = lastSlide.getMapX() + lastSlide.getLocalWidth();
                corner.y = lastSlide.getMapY() + lastSlide.getLocalHeight();
                farthestVisibleSlideCorner = corner;
                //out("far corner: " + Util.fmt(corner));
            } else
                farthestVisibleSlideCorner = null;


            // Now just in case, layout all the non-visible ones after the visible, in case
            // they get manually selected via the pathway panel and temporarily shown
            // (only one can be shown at a time, so they can all occupy the last slot)

            for (LWPathway.Entry e : mEntries) {
                if (!e.pathway.isShowingSlides() && e.canProvideSlide()) {
                    e.getSlide().takeLocation(xoff, yoff);
                }
            }
            //}
    }
    
    private void drawSlideIconStack(final DrawContext dc)
    {
        layoutSlideIcons(dc);

        dc.setBackgroundFill(null); // always make sure the slide icons fill
        for (LWSlide slide : seenSlideIcons(dc)) {
            drawSlideIcon(dc.push(), slide);
            dc.pop();
        }

    }


    protected static final BasicStroke SlideIconPathwayStroke =
        new BasicStroke((float) (LWPathway.PathBorderStrokeWidth / SlideIconScale),
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND);
    

    private void drawSlideIcon(final DrawContext dc, final LWSlide slide)
    {
        slide.transformDownG(dc.g);

        //        final boolean drewBorder;

//         //if (dc.isPresenting() || slide.isSelected()) {
//         if (dc.isPresenting() || slide.getPathwayEntry() == VUE.getActiveEntry()) {
//             // every slide icon should be a slide with an entry...
//             dc.g.setColor(slide.getPathwayEntry().pathway.getColor());
//             //dc.g.setColor(Color.red);
//             dc.g.setStroke(SlideIconPathwayStroke);
//             dc.g.draw(slide.getZeroShape());
//             drewBorder = true;
//         } else {
//             drewBorder = false;
//         }
            
//         final AffineTransform zeroTransform = dc.g.getTransform();
//         final Shape curClip = dc.g.getClip();
//         dc.g.clip(slide.getZeroShape());
        slide.drawZero(dc);

//         if (!drewBorder && !dc.isAnimating()) {
//             dc.g.setClip(curClip); // TODO: this is clearing the underlying clip and allowing the border to draw over the scroll bars, etc!
//             // Generic non-presentation unselected slide icon: draw a gray border
//             //dc.g.setColor(slide.getRenderFillColor(dc).brighter());
//             dc.g.setTransform(zeroTransform);
//             dc.g.setColor(Color.darkGray);
//             dc.g.setStroke(STROKE_FIVE);
//             dc.g.draw(slide.getZeroShape());
//         }
        
        
    }



    

    /*
    protected final void drawDecorated(DrawContext dc)
    {
        final LWPathway.Entry entry = getEntryToDisplay();
        //final boolean drawSlide = (entry != null);
        final boolean drawSlide = (entry != null && !entry.isMapView);

        if (dc.drawPathways() && dc.focal != this)
            drawPathwayDecorations(dc);

        if (drawSlide) {

            drawZero(dc);

            final LWSlide slide = entry.getSlide();
            
            //double slideX = getCenterX() - (slide.getWidth()*slideScale) / 2;
            //double slideY = getCenterY() - (slide.getHeight()*slideScale) / 2;
            //dc.g.translate(slideX, slideY);

            Rectangle2D.Float slideFrame = getSlideIconBounds();

            //slide.setLocation(slideFrame.x, slideFrame.y);

            dc.setClipOptimized(false);
            dc.g.translate(slideFrame.x, slideFrame.y);
            dc.g.scale(SlideIconScale, SlideIconScale);

            // A hack so that when LWLinks (hasAbsoluteMapLocation) pop to map drawing, they
            // don't pop up beyond this point.
            //dc.mapTransform = dc.g.getTransform();
            
            //dc.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
            //entry.pathway.getMasterSlide().drawImpl(dc);
            slide.drawImpl(dc);

            //Rectangle2D border = slideFrame;
            // todo: move to LWSlide.drawImpl:
            Rectangle2D border = slide.getBounds();
            final Color slideFill = slide.getRenderFillColor(dc);
            final Color iconBorder;
            // todo: create a contrastColor, which node icon border's can also use
            if (brightness(slideFill) == 0)
                iconBorder = Color.gray;
            else if (brightness(slideFill) > 0.5)
                iconBorder = slideFill.darker();
            else
                iconBorder = slideFill.brighter();
            //out("slideFillr: " + slideFill);
            //out("iconBorder: " + iconBorder);
            dc.g.setColor(iconBorder);
            dc.g.setStroke(VueConstants.STROKE_SEVEN);
            dc.g.draw(border);

        } else {

            //if (entry != null && !dc.isFocused) {
            if (entry != null) {
                // if we had an entry, but it was a map-view slide, do something to make it look slide-like
                dc.g.setColor(entry.pathway.getMasterSlide().getFillColor());
                if (entry.node instanceof LWGroup) {
                    if (!dc.isPresenting())
                        dc.g.fill(entry.node.getZeroBounds());
                } else if (dc.focal != this && entry.node.isTranslucent()) {
                    Area toFill = new Area(entry.node.getZeroBounds());
                    toFill.subtract(new Area(entry.node.getZeroShape()));
                    dc.g.fill(toFill);
                }
            }
            
            drawZero(dc);
        }
    }
    */      

    /** default impl: does nothing -- meant to be overriden */
    protected void drawImpl(DrawContext dc) {}

    protected LWChangeSupport getChangeSupport() {
        return mChangeSupport;
    }
    public synchronized void addLWCListener(Listener listener) {
        mChangeSupport.addListener(listener, null);
    }

    public synchronized void addLWCListener(Listener listener, LWComponent.Key singleEventKey) {
        mChangeSupport.addListener(listener, singleEventKey);
    }
    /** @param eventMask is a string constant (from LWKey) or an array of such. If one
     of these non-null values, only events matching those keys will be delievered */
    public synchronized void addLWCListener(Listener listener, Object... eventsDesired) {
        mChangeSupport.addListener(listener, eventsDesired);
    }
    public synchronized void removeLWCListener(Listener listener) {
        mChangeSupport.removeListener(listener);
    }

    /** convenince method for remove a (possible) old listener, and attaching a (possible) new listener */
    public static void swapLWCListener(Listener listener, LWComponent oldSource, LWComponent newSource) {
        if (oldSource != null)
            oldSource.removeLWCListener(listener);
        if (newSource != null)
            newSource.addLWCListener(listener);
    }
    
    public synchronized void removeAllLWCListeners() {
        mChangeSupport.removeAllListeners();
    }

    protected synchronized void notifyLWCListeners(LWCEvent e)
    {
        //if (e.key.isSignal || e.key == LWKey.Location && e.source == this) {
        if (e.key == LWKey.UserActionCompleted || e.key == LWKey.Location && e.source == this) {
            // only keep if the location event is on us:
            // if this is our child that moved, obviously
            // clear the cache (we look different)
            //out("*** KEEPING IMAGE CACHE ***");
            ; // keep the cached image
        } else {
            //out("*** CLEARING IMAGE CACHE");
            //mCachedImage = null;
        }

        if (isStyle() && getParent() == null)
            ; // ignore events from non-embedded style objects (e.g., EditorManager constructs)
        else
            mChangeSupport.notifyListeners(this, e);

        if (getParent() != null && e.component == this && e.key instanceof Key) {
            // if parent is null, we're still initializing
            final Key key = (Key) e.key;

            if (isStyle() && key.isStyleProperty)
                updateStyleWatchers(key, e);
            
            if (key.type == KeyType.DATA)
                syncUpdate(key);
        }

        // labels need own call to this due to TextBox use of setLabel0
    }

    
    /** Copy the value for the given key either back to our sync source, or to our sync clients */
    private boolean syncUnderway = false;
    private void syncUpdate(Key key) {

        if (syncUnderway)
            return;
        
        syncUnderway = true;
        try {
            doSyncUpdate(key);
        } finally {
            syncUnderway = false;
        }
    }
    
    protected void doSyncUpdate(Key key) {
        // currently we only allow one or the other: you can be a source, or a client
        // this is all we need for now (a node can be synced to nodes on multiple
        // slides on different pathways, but a node in a slide can only refer
        // back to one source)
        if (mSyncSource != null) {
            Log.debug("[" + key + "] UPDATING SYNC SOURCE: " +  this + " -> " + mSyncSource);
            if (!mSyncSource.isDeleted())
                key.copyValue(this, mSyncSource);

        } else if (mSyncClients != null && !mSyncClients.isEmpty()) {
            
            for (LWComponent c : mSyncClients) {
                Log.debug("[" + key + "] UPDATING SYNC CLIENT: " + this + " -> " + c);
                //Util.printStackTrace("SYNCTRACE " + this);
                if (!c.isDeleted())
                    key.copyValue(this, c);
            }
        }
    }

    /** If the event is a change for a style property, apply the change to all
        LWComponents that refer to us as their style parent */
    protected void updateStyleWatchers(Key key, LWCEvent e)
    {
        if (!key.isStyleProperty || mXMLRestoreUnderway) {
            // nothing to do if this isn't a style property that's changing
            return;
        }

        // Now we know a styled property is changing.  Since they Key itself
        // knows how to get/set/copy values, we can now just find all the
        // components "listening" to this style (pointing to it), and copy over
        // the value that just changed on the style object.
        
        if (DEBUG.Enabled) out("STYLE OBJECT UPDATING STYLED CHILDREN with " + key);
        //final LWPathway path = ((MasterSlide)getParent()).mOwner;
        
        // We can traverse all objects in the system, looking for folks who
        // point to us.  But once slides are owned by the pathway, we'll have a
        // list of all slides here from the pathway, and we can just traverse
        // those and check for updates amongst the children, as we happen
        // to know that this style object only applies to slides
        // (as opposed to ontology style objects)
        
        // todo: this not a fast way to traverse & find what we need to change...
        for (LWComponent dest : getMap().getAllDescendents(ChildKind.ANY)) {
            // we should never be point back to ourself, but we check just in case
            if (dest.mParentStyle == this && dest.supportsProperty(key) && dest != this) {
                // Only copy over the style value if was previously set to our existing style value
                try {
                    if (key.valueEquals(dest, e.getOldValue()))
                        key.copyValue(this, dest);
                } catch (Throwable t) {
                    tufts.Util.printStackTrace(t, "Failed to copy value from " + e + " old=" + e.getOldValue());
                }
            }
        }
    }
    
    
    /**
     * A third party can ask this object to raise an event
     * on behalf of the source.
     */
    void notify(Object source, String what)
    {
        notifyLWCListeners(new LWCEvent(source, this, what));
    }

    void notifyProxy(LWCEvent e) {
        notifyLWCListeners(e);
    }

    protected void notify(String what, LWComponent contents)
    {
        notifyLWCListeners(new LWCEvent(this, contents, what));
    }

    protected void notify(String what, Object oldValue)
    {
        notifyLWCListeners(new LWCEvent(this, this, what, oldValue));
    }

    protected void notify(Key key, Object oldValue)
    {
        notifyLWCListeners(new LWCEvent(this, this, key, oldValue));
    }

    protected void notify(String what)
    {
        // todo: we still need both src & component? (this,this)
        notifyLWCListeners(new LWCEvent(this, this, what, LWCEvent.NO_OLD_VALUE));
    }
    
    /**a notify with an array of components
       added by Daisuke Fujiwara
     */
    protected void notify(String what, List<LWComponent> componentList)
    {
        notifyLWCListeners(new LWCEvent(this, componentList, what));
    }

    /**
     * Do final cleanup needed now that this LWComponent has
     * been removed from the model.  Calling this on an already
     * deleted LWComponent has no effect.
     */
    protected void removeFromModel()
    {
        if (isDeleted()) {
            if (DEBUG.PARENTING||DEBUG.EVENTS) out(this + " removeFromModel(lwc): ignoring (already removed)");
            return;
        }
        if (DEBUG.PARENTING||DEBUG.EVENTS) out(this + " removeFromModel(lwc)");
        //throw new IllegalStateException(this + ": attempt to delete already deleted");
        notify(LWKey.Deleting);
        prepareToRemoveFromModel();
        removeAllLWCListeners();
        disconnectFromLinks(); // if any of the links themseleves are being deleted, we don't actually need to disconnect
        setDeleted(true);
    }

    /**
     * For subclasses to override that need to do cleanup
     * activity before the the default LWComponent removeFromModel
     * cleanup runs.
     */
    protected void prepareToRemoveFromModel() { }

    /** undelete */
    protected void restoreToModel()
    {
        if (DEBUG.PARENTING||DEBUG.EVENTS) out(this + " restoreToModel");
        if (!isDeleted()) {
            //throw new IllegalStateException("Attempt to restore already restored: " + this);
            if (DEBUG.Enabled) out("FYI: already restored");
            //return;
        }
        // There is no reconnectToLinks: link endpoint connect events handle this.
        // We couldn't do it here anyway as we wouldn't know which of the two endpoint to connect us to.
        setDeleted(false);
    }

    public boolean isDeleted() {
        //return isHidden(HideCause.DELETED);
        return hasFlag(Flag.DELETED);
    }
    
    private void setDeleted(boolean deleted) {
        if (deleted) {
            //mHideBits |= HideCause.DELETED.bit; // direct set: don't trigger notify
            mFlags |= Flag.DELETED.bit;
            if (DEBUG.PARENTING||DEBUG.UNDO||DEBUG.EVENTS)
                if (parent != null) out("parent not yet null in setDeleted true (ok for undo of creates)");
            this.parent = null;
        } else
            mFlags &= ~Flag.DELETED.bit; // direct set: don't trigger notify
        //mHideBits &= ~HideCause.DELETED.bit; // direct set: don't trigger notify
    }

    private void disconnectFromLinks()
    {
        // iterate through copy of the list, as it may be modified concurrently during removals
        if (mLinks != null) {
            for (LWLink link : mLinks.toArray(new LWLink[mLinks.size()]))
                link.disconnectFrom(this);
        }
        clearHidden(HideCause.PRUNE);
     }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public final boolean isSelected() {
        return this.selected;
    }

    protected final boolean selectedOrParent() {
        return parent == null ? isSelected() : (parent.selectedOrParent() | isSelected());
    }
    
    public final boolean isAncestorSelected() {
        return parent == null ? false : parent.selectedOrParent();
    }

    protected void setFlag(Flag flag) {
        mFlags |= flag.bit;
    }

    protected void setFlag(Flag flag, boolean set) {
        if (set)
            setFlag(flag);
        else
            clearFlag(flag);
    }
    

    protected void clearFlag(Flag flag) {
        mFlags &= ~flag.bit;
    }

    protected boolean hasFlag(Flag flag) {
        return (mFlags & flag.bit) != 0;
    }
    
    
    private void setHideBits(int bits) {
        final boolean wasHidden = isHidden();
        mHideBits = bits;
        if (wasHidden != isHidden())
            notify(LWKey.Hidden);
    }

//     /** debug -- names of set HideBits */
//     String getDescriptionOfSetBits() {
//         StringBuffer buf = new StringBuffer();
//         for (HideCause reason : HideCause.values()) {
//             if (isHidden(reason)) {
//                 if (buf.length() > 0)
//                     buf.append(',');
//                 buf.append(reason);
//             }
//         }
//         return buf.toString();
//     }
    
    String getDescriptionOfSetBits() {
        String s = "";
        if (mHideBits != 0)
            s += getDescriptionOfSetBits(HideCause.class, mHideBits);
        if (mFlags != 0) {
            if (s.length() > 0)
                s += "; ";
            s += getDescriptionOfSetBits(Flag.class, mFlags);
        }
        return s;
    }
    
    String getDescriptionOfSetBits(Class enumType, long bits) {
        final StringBuffer buf = new StringBuffer();
        buf.append(enumType.getSimpleName() + "(");
        boolean first = true;
        for (Object eValue : enumType.getEnumConstants()) {
            final Enum e = (Enum) eValue;
            if ((bits & (1<<e.ordinal())) != 0) {
                if (!first)
                    buf.append(',');
                buf.append(eValue);
                //buf.append(':');buf.append(e.ordinal());
                first = false;
            }
        }
        buf.append(')');
        return buf.toString();
    }
    
    
    public void setVisible(boolean visible) {
        setHidden(HideCause.DEFAULT, !visible);
    }
    
    public void setHidden(HideCause cause, boolean hide) {
        if (hide)
            setHidden(cause);
        else
            clearHidden(cause);
    }
    
    public void setHidden(HideCause cause) {
        setHideBits(mHideBits | cause.bit);
    }
    
    public void clearHidden(HideCause cause) {
        setHideBits(mHideBits & ~cause.bit);
    }

    /**
     * @return true if this component has been hidden.  Note that this
     * is different from isFiltered.  All children of a hidden component
     * are also hidden, but not all children of a filtered component
     * are hidden.
     */
    public final boolean isHidden() {
        return !isVisible();
    }

    public boolean isHidden(HideCause cause) {
        return (mHideBits & cause.bit) != 0;
    }
    
    public boolean isVisible() {
        return mHideBits == 0;
    }
    
    
    /** @return always null (false): subclasses can override to persist the DEFAULT
     * hidden bit if they wish.
     */
    public Boolean getXMLhidden() {
        //return hidden ? Boolean.TRUE : null;
        return null;
    }
    public void setXMLhidden(Boolean b) {
        setVisible(!b.booleanValue());
    }

    
    public boolean isDrawn() {
        return isVisible() && !isFiltered();
    }
    
//     public void setRollover(boolean tv)
//     {
//         if (this.rollover != tv) {
//             this.rollover = tv;
//         }
//     }
//     public boolean isRollover() {
//         return this.rollover;
//     }


    public void setZoomedFocus(boolean zoomedFocus) {
        isZoomedFocus = zoomedFocus;
//        linkNotificationDisabled = zoomedFocus;
    }

    public final boolean isZoomedFocus() {
        return isZoomedFocus;
    }

    protected boolean updatingLinks() {
        return !isZoomedFocus || DEBUG.VIEWER;
    }
    
    
    public void mouseEntered(MapMouseEvent e)
    {
        if (DEBUG.ROLLOVER) System.out.println("MouseEntered:     " + this);
        //e.getViewer().setIndicated(this);
        mouseOver(e);
    }
    public void mouseMoved(MapMouseEvent e)
    {
        //System.out.println("MouseMoved " + this);
        mouseOver(e);
    }
    public void mouseOver(MapMouseEvent e)
    {
        //System.out.println("MouseOver " + this);
    }
    public void mouseExited(MapMouseEvent e)
    {
        if (DEBUG.ROLLOVER) System.out.println(" MouseExited:     " + this);
        //e.getViewer().clearIndicated();
    }

    /** pre-digested single-click
     * @return true if you do anything with it, otherwise
     * the viewer can/will provide default action.
     */
    public boolean handleSingleClick(MapMouseEvent e)
    {
        return false;
    }
    
    /** pre-digested double-click
     * @return true if you do anything with it, otherwise
     * the viewer can/will provide default action.
     * Default action: if we have a resource, launch
     * it in a browser, otherwise, do nothing.
     */
    public boolean handleDoubleClick(MapMouseEvent e)
    {
        if (hasResource()) {
            out("Displaying content for: " + getResource());
            getResource().displayContent();
            return true;
        } else if (this instanceof LWSlide || this instanceof LWGroup) {
            //} else if (this instanceof LWSlide || this instanceof LWGroup || this instanceof LWPortal)
            // MapViewer "null remote focal" code would need fixing to enable selection if a portal is the focal
            // (the selected objects are not children of the focal, so they don't look like we should be seeing them)
            return doZoomingDoubleClick(e);
        } else
            return false;
    }

    public static final boolean SwapFocalOnSlideZoom = true;
    private static final boolean AnimateOnZoom = true;

    protected boolean doZoomingDoubleClick(MapMouseEvent e)
    {
        final MapViewer viewer = e.getViewer();

        if (viewer.getFocal() == this) {
            viewer.popFocal(MapViewer.POP_TO_TOP, MapViewer.ANIMATE);
            return true;
            //return false;
        }

        final Rectangle2D viewerBounds = viewer.getVisibleMapBounds();
        final Rectangle2D mapBounds = getMapBounds();
        final Rectangle2D overlap = viewerBounds.createIntersection(mapBounds);
        final double overlapArea = overlap.getWidth() * overlap.getHeight();
        //final double viewerArea = viewerBounds.getWidth() * viewerBounds.getHeight();
        final double nodeArea = mapBounds.getWidth() * mapBounds.getHeight();
        final boolean clipped = overlapArea < nodeArea;
        
        final double overlapWidth = mapBounds.getWidth() / viewerBounds.getWidth();
        final double overlapHeight = mapBounds.getHeight() / viewerBounds.getHeight();

        final boolean focusNode; // otherwise, re-focus map

        // Note: this code is way more complicated than we're making use of right now --
        // we always fully load objects (slides) as the focal when we zoom to them.
        // This code permitted double-clicking through a slide-icon stack, where we'd
        // zoom to the slide icon, but retain the map focal.  The overlap herustics here
        // determined how much of the current view was occupied by the current clicked
        // on zoom-to object.  If mostly in view, assume we want to "de-focus" (zoom
        // back out to the map from our "virtual focal" zoomed-to node), but if mostly
        // not in view, re-center on this object.  When last tested, this was smart
        // enough to allow you to simply cycle through a stack of slide-icons with
        // double clicking on the exposed edge of the nearby slide icons (of course,
        // this code was on LWSlide back then...)

        if (DEBUG.Enabled) {
            outf(" overlapWidth %4.1f%%", overlapWidth * 100);
            outf("overlapHeight %4.1f%%", overlapHeight * 100);
            outf("clipped=" + clipped);
        }
        
        if (clipped) {
            focusNode = true;
        } else if (overlapWidth > 0.8 || overlapHeight > 0.8) {
            focusNode = false;
        } else
            focusNode = true;

        if (focusNode) {
            viewer.clearRollover();
            
            if (SwapFocalOnSlideZoom) {
                // loadfocal animate only currently works when popping (to a parent focal)
                //viewer.loadFocal(this, true, AnimateOnZoom);
                ZoomTool.setZoomFitRegion(viewer,
                                          mapBounds,
                                          0,
                                          AnimateOnZoom);
                viewer.loadFocal(this);
            } else {
                ZoomTool.setZoomFitRegion(viewer,
                                          mapBounds,
                                          -LWPathway.PathBorderStrokeWidth / 2,
                                          AnimateOnZoom);
            }
        } else {
            // just re-fit to the map
            viewer.fitToFocal(AnimateOnZoom);
        }
        
        return true;
    }
    

    /** pesistance default */
    public void addObject(Object obj)
    {
        System.err.println("Unhandled XML obj: " + obj);
    }


    /** subclasses override this to add info to toString()
     (return super.paramString() + new info) */
    public String paramString()
    {
        return String.format(" %+.0f,%+.0f %.0fx%.0f", getX(), getY(), width, height);
    }

    protected void out(String s) {
        if (DEBUG.Enabled) Log.debug(s + "; " + this);
//         if (DEBUG.THREAD) {
//             String thread = Thread.currentThread().toString().substring(6);
//             System.err.format("%-32s%s %s\n", thread, this, s);
//         } else {
//             System.err.println(this + " " + s);
//         }
    }

    protected void outf(String format, Object ... args) {
        Util.outf(Log, format, args);
    }
    
    /*
    static protected void out(Object o) {
        System.out.println((o==null?"null":o.toString()));
    }
    */
/*
    static protected void out(String s) {
        System.out.println(s);
    }
*/

    /** interface {@link XMLUnmarshalListener} -- does nothing here */
    public void XML_initialized() {
        mXMLRestoreUnderway = true;
    }
    
    public void XML_fieldAdded(String name, Object child) {
        if (DEBUG.XML) out("XML_fieldAdded <" + name + "> = " + child);
    }

    /** interface {@link XMLUnmarshalListener} */
    public void XML_addNotify(String name, Object parent) {
        if (DEBUG.XML) tufts.Util.printClassTrace("tufts.vue", "XML_addNotify; name=" + name
                                                  + "\n\tparent: " + parent
                                                  + "\n\t child: " + this
                                                  + "\n");

        // TODO: moving this layout from old position at end of LWMap.completeXMLRestore
        // to here may have unpredictable results... watch of bad states after restores.
        // The advantage of doing it here is that virtual children are handled,
        // and "off map" children, such as slide children are properly handled.
        //layout("XML_addNotify"); 
    }

    /** interface {@link XMLUnmarshalListener} -- call's layout */
    public void XML_completed() {
        // 2007-06-12 SMF -- do NOT turn this off yet -- let the LWMap
        // turn it off when EVERYONE is done.
        //mXMLRestoreUnderway = false;

        /*
        // TODO: TEMPORARY DEBUG: never restore slides as format changes at moment
        //mSlides.clear();
        for (LWSlide slide : mSlides.values()) {
            // slides are virtual children of the node: we're their
            // parent, tho they're not formal children of ours.
            slide.setParent((LWContainer)this);
            // TODO: currently, this means non-container objects, such as LWImages,
            // can't have slides -- prob good to remove that restriction.
            // What would break if the parent ref were just a LWComponent?
        }
        */
        
        if (DEBUG.XML) System.out.println("XML_completed " + this);
        //layout(); need to wait till scale values are all set: so the LWMap needs to trigger this
    }

    protected static final double OPAQUE = 1.0;


    /**
     * @param alpha -- an alpha value for the whole image
     * @param maxSize -- if non-null, the max width/height of the produced image (may be smaller)
     * @param zoom -- a zoom for the map size in producing the image (currently ignored if maxSize is provided)
     */
    protected BufferedImage getAsImage(double alpha, Dimension maxSize, double zoom) {
        return createImage(alpha, maxSize, (Color) null, zoom);
    }
    
    public BufferedImage getAsImage(double alpha, Dimension maxSize) {
        return getAsImage(alpha, maxSize, 1.0);
    }
    public BufferedImage getAsImage(double zoom) {
        return getAsImage(OPAQUE, null, zoom);
    }
    public BufferedImage getAsImage() {
        return getAsImage(OPAQUE, null, 1.0);
    }

    public BufferedImage createImage(double alpha, Dimension maxSize) {
        return createImage(alpha, maxSize, null, 1.0);
    }

    protected final static Rectangle2D.Float grow(Rectangle2D.Float r, int size) {
        r.x -= size;
        r.y -= size;
        r.width += size * 2;
        r.height += size * 2;
        return r;
    }
    
    protected final static Rectangle2D.Float grow(Rectangle2D.Float r, float size) {
        r.x -= size;
        r.y -= size;
        r.width += size * 2;
        r.height += size * 2;
        return r;
    }

    /** @return the map bounds to use for rendering when generating an image of this LWComponent */
    protected Rectangle2D.Float getImageBounds() {
        final Rectangle2D.Float bounds = (Rectangle2D.Float) getPaintBounds().clone();

        int growth = 1; // just in case / rounding errors
        
        if (this instanceof LWMap)
            growth += 15;
        
        if (growth > 0)
            grow(bounds, growth);

        return bounds;
    }

    private static double computeZoomAndSize(Rectangle2D.Float bounds, Dimension maxSize, double zoomRequest, Size sizeResult)
    {
        double fitZoom = 1.0;
        
        if (maxSize != null) {
            if (bounds.width > maxSize.width || bounds.height > maxSize.height) {
                fitZoom = ZoomTool.computeZoomFit(maxSize, 0, bounds, null);
                sizeResult.width = (float) Math.ceil(bounds.width * fitZoom);
                sizeResult.height = (float) Math.ceil(bounds.height * fitZoom);
            }
        } else if (zoomRequest != 1.0) {
            sizeResult.width *= zoomRequest;
            sizeResult.height *= zoomRequest;
            fitZoom = zoomRequest;
        }

        return fitZoom;
    }
    

    
    
    /**
     * Create a new buffered image, of max dimension maxSize, and render the LWComponent
     * (and all it's children), to it using the given alpha.
     * @param alpha 0.0 (invisible) to 1.0 (no alpha)
     * @param maxSize max dimensions for image. May be null.  Image may be smaller than maxSize.
     * @param fillColor -- if non-null, will be rendered as background for image.  If alpha is
     * @param zoomRequest -- desired zoom; ignored if maxSize is non-null
     * also set, background fill will have transparency of alpha^3 to enhance contrast.
     */

    // Note: as of Mac OS X 10.4.10 (Intel), when a java drag source declares it can
    // generate an image (as we do when we Apple-drag something), if you drop it on the
    // desktop, it will create a special mac "picture clipping", which is some kind of
    // raw format, probabaly TIFF, tho you CANNOT open these in Preview.  Apparently
    // there's some kind of bug in the special .pictClipping, where sometimes when
    // opening it up it shows entirely as a blank space (I think if the image starts to
    // get "very large"), tho the data is actually there -- if you drag the picture
    // clipping into an Apple Mail message, it shows up again (and if you dragged from
    // VUE to Apple Mail in the first place, it also works fine).  Note that AFTER
    // dragging into Apple Mail, you can THEN double-click the attachment, and it will
    // open it up in Preview as a .tiff file (Apple Mail appears to be converting the
    // .pictClipping to tiff).  Note that uncompressed TIFF isn't exactly a friendly
    // mail attachment format as it's huge.  But once you open the image in Preview, you
    // have the option of saving it / exporting it as a jpeg, and you can even adjust
    // the quality to your liking.

    
    public BufferedImage createImage(double alpha, Dimension maxSize, Color fillColor, double zoomRequest)
    {
        final Rectangle2D.Float bounds = getImageBounds();

        if (DEBUG.IMAGE)  {
            System.out.println();
            System.out.println(TERM_CYAN +
                "createImage: " + this
                + "\n\t zoomRequst: " + zoomRequest
                + "\n\t    maxSize: " + maxSize
                + "\n\t  mapBounds: " + fmt(bounds)
                + "\n\t  fillColor: " + fillColor
                + "\n\t      alpha: " + alpha
                + TERM_CLEAR
                );
        }
        
        final Size imageSize = new Size(bounds);
        final double usedZoom = computeZoomAndSize(bounds, maxSize, zoomRequest, imageSize);

        // Image type ARGB is needed if at any point in the generated image,
        // there is a not 100% opaque pixel all the way through the background.
        // So TYPE_INT_RGB will handle transparency with a map fine --
        // but we need TYPE_INT_ARGB if, say, we're generating drag
        // image that we want to be a borderless node (fully transparent
        // image border), or if the whole drag image itself is semi-transparent.

        final int imageType;
        if (alpha == OPAQUE && fillColor != null && fillColor.getAlpha() == 255)
            imageType = BufferedImage.TYPE_INT_RGB;
        else
            imageType = BufferedImage.TYPE_INT_ARGB;

       final int width = imageSize.pixelWidth();
       final int height = imageSize.pixelHeight();
        
        if (DEBUG.IMAGE) out(TERM_CYAN
                             + "createImage:"
                             //+ "\n\tfinal size: " + width + "x" + height
                             + "\n\t neededSize: " + imageSize
                             + "\n\t   usedZoom: " + usedZoom
                             + "\n\t       type: " + (imageType == BufferedImage.TYPE_INT_RGB ? "OPAQUE" : "TRANSPARENT")
                             + TERM_CLEAR);

        if (mCachedImage != null &&
            mCachedImage.getWidth() == width &&
            mCachedImage.getHeight() == height &&
            mCachedImage.getType() == imageType)
        {
            // todo: could also re-use if cached image is > our needed size as long it's
            // an ARGB and we fill it with full alpha first, tho we really shouldn't
            // have each component caching it's own image: some kind of small
            // recently used image buffers cache would make more sense.
            if (DEBUG.DND || DEBUG.IMAGE) out(TERM_BLUE + "\ngot cached image: " + mCachedImage + TERM_CLEAR);
        } else {
            mCachedImage = new BufferedImage(width, height, imageType);
            if (DEBUG.DND || DEBUG.IMAGE) out(TERM_RED + "created image: " + mCachedImage + TERM_CLEAR);
        }

        drawImage((Graphics2D) mCachedImage.getGraphics(),
                  alpha,
                  maxSize,
                  fillColor,
                  zoomRequest
                  );

        return mCachedImage;
    }

    /**
     * Useful for drawing drag images into an existing graphics buffer, or drawing exportable images.
     *
     * @param alpha 0.0 (invisible) to 1.0 (no alpha -- completely opaque)
     * @param maxSize max dimensions for image. May be null.  Image may be smaller than maxSize.
     * @param fillColor -- if non-null, will be rendered as background for image.  If alpha is
     * @param zoomRequest -- desired zoom; ignored if maxSize is non-null
     * also set, background fill will have transparency of alpha^3 to enhance contrast.
     */

    public void drawImage(Graphics2D g, double alpha, Dimension maxSize, Color fillColor, double zoomRequest)
    {
        //if (DEBUG.IMAGE) out("drawImage; size " + maxSize);

        final boolean drawBorder = this instanceof LWMap; // hack for dragged images of LWMaps

        final Rectangle2D.Float bounds = getImageBounds();
        final Rectangle clip = g.getClipBounds();
        final Size fillSize = new Size(bounds);
        final double zoom = computeZoomAndSize(bounds, maxSize, zoomRequest, fillSize);
        
        if (DEBUG.IMAGE) out(TERM_GREEN
                             + "drawImage:"
                             + "\n\t   mapBounds: " + fmt(bounds)
                             + "\n\t        fill: " + fillColor
                             + "\n\t     maxSize: " + maxSize
                             + "\n\t zoomRequest: " + zoomRequest
                             + "\n\t     fitZoom: " + zoom
                             + "\n\t    fillSize: " + fillSize
                             + "\n\t          gc: " + g
                             + "\n\t        clip: " + fmt(clip)
                             + "\n\t       alpha: " + alpha
                             + TERM_CLEAR
                             );


        final int width = fillSize.pixelWidth();
        final int height = fillSize.pixelHeight();
        
        final DrawContext dc = new DrawContext(g, this);
        dc.setInteractive(false);
        dc.setBackgroundFill(getRenderFillColor(null)); // sure we want null here?
        dc.setClipOptimized(false); // always draw all children -- don't bother to check bounds
        if (DEBUG.IMAGE) out(TERM_GREEN + "drawImage: " + dc + TERM_CLEAR);

        if (fillColor != null) {
            if (false && alpha != OPAQUE) {
                Color c = fillColor;
                // if we have an alpha and a fill, amplify the alpha on the background fill
                // by changing the fill to one that has alpha*alpha, for a total of
                // alpha*alpha*alpha given our GC already has an alpha set.
                fillColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha*alpha*255+0.5));
            }
            if (alpha != OPAQUE) 
                dc.setAlpha(alpha, AlphaComposite.SRC); // erase any underlying in cache
            if (DEBUG.IMAGE) out("drawImage: fill=" + fillColor);
            g.setColor(fillColor);
            g.fillRect(0, 0, width, height);
        } else if (alpha != OPAQUE) {
            // we didn't have a fill, but we have an alpha: make sure any cached data is cleared
            dc.g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, width, height);
        }
        
        if (alpha != OPAQUE)
            dc.setAlpha(alpha, AlphaComposite.SRC);

        if (DEBUG.IMAGE && DEBUG.META) {
            // Fill the entire imageable area
            g.setColor(Color.green);
            g.fillRect(0,0, Short.MAX_VALUE, Short.MAX_VALUE);
        }

        dc.setAntiAlias(true);

        final AffineTransform rawTransform = g.getTransform();
            
        if (zoom != 1.0)
            dc.g.scale(zoom, zoom);
                
        // translate so that the upper left corner of the map region
        // we're drawing is at 0,0 on the underlying image

        g.translate(-bounds.getX(),
                    -bounds.getY());

        // GC *must* have a bounds set or we get NPE's in JComponent (textBox) rendering
        dc.setMasterClip(bounds);

        if (DEBUG.IMAGE && DEBUG.META) {
            // fill the clipped area so we can check our clip bounds
            dc.g.setColor(Color.red);
            dc.g.fillRect(-Short.MAX_VALUE/2,-Short.MAX_VALUE/2, // larger values than this can blow out internal GC code and we get nothing
                           Short.MAX_VALUE, Short.MAX_VALUE);
        }

        if (this instanceof LWImage) {
            // for some reason, raw images don't seem to want to draw unless we fill first
            dc.g.setColor(Color.white);
            dc.g.fill(bounds);
        }
        

        // render to the image through the DrawContext/GC pointing to it
        draw(dc);

        if (drawBorder) {
            g.setTransform(rawTransform);
            //g.setColor(Color.red);
            //g.fillRect(0,0, Short.MAX_VALUE, Short.MAX_VALUE);
            if (DEBUG.IMAGE) {
                g.setColor(Color.black);
                dc.setAntiAlias(false);
            } else
                g.setColor(Color.darkGray);
            g.drawRect(0, 0, width-1, height-1);
        }

        if (DEBUG.IMAGE) out(TERM_GREEN + "drawImage: completed\n" + TERM_CLEAR);
        
        
    }
    

    
    public String toString()
    {
        String cname = getClass().getName();
        String typeName = cname.substring(cname.lastIndexOf('.')+1);
        String label = "";
        String s;
        if (getLabel() != null) {
            if (true||isAutoSized())
                label = "\"" + getDisplayLabel() + "\" ";
            else
                label = "(" + getDisplayLabel() + ") ";
        }

        if (getID() == null) {
            s = String.format("%-17s[",
                              typeName + "." + Integer.toHexString(System.identityHashCode(this))
                              );
            //s += tufts.Util.pad(9, Integer.toHexString(hashCode()));
        } else {
            s = String.format("%-17s", typeName + "[" + getID());
            //s += tufts.Util.pad(4, getID());
        }
        s += label;
        //if (this.scale != 1f) s += "z" + this.scale + " ";
        if (getScale() != 1f) s += String.format("z%.2f ", getScale());
        s += paramString();
        if (mHideBits != 0) s += " " + getDescriptionOfSetBits(HideCause.class, mHideBits);
        if (mFlags != 0) s += " " + getDescriptionOfSetBits(Flag.class, mFlags);
        if (getResource() != null)
            s += " " + getResource().getSpec();
        //s += " <" + getResource() + ">";
        s += "]";
        return s;
    }



    public static void main(String args[]) throws Exception
    {
        VUE.init(args);

        /*
        for (java.lang.reflect.Field f : LWComponent.class.getDeclaredFields()) {
            Class type = f.getType();
            if (type == Key.class)
                System.out.println("KEY: " + f);
            else
                System.out.println("Field: " + f + " (" + type + ")");
        }
        */

        // for debug: ensure basic LW types created first
        
        new LWNode();
        new LWLink();
        new LWImage();

        //NodeTool.getTool();

        VueToolbarController.getController(); // make sure the tools are initialized
        
        edu.tufts.vue.style.StyleReader.readStyles("compare.weight.css");

        java.util.Set<String> sortedKeys = new java.util.TreeSet<String>(edu.tufts.vue.style.StyleMap.keySet());

        for (String key : sortedKeys) {
            final Object style = edu.tufts.vue.style.StyleMap.getStyle(key);
            System.out.println("Found CSS style key; " + key + ": " + style);
            //System.out.println("Style key: " + se.getKey() + ": " + se.getValue());
        }

        new LWNode().applyCSS(edu.tufts.vue.style.StyleMap.getStyle("node.w1"));
        new LWLink().applyCSS(edu.tufts.vue.style.StyleMap.getStyle("link.w1"));
        
    }
    

    
}



        /*
        private final java.lang.reflect.Field field;
        public Key(String name, String fieldName) {
            this(name);

            // this successfully auto-generates the slot reference, tho not really worth
            // it, as requiring the extra code snippet for grabbing the slot (Property)
            // object at least eliminates any typo's.  If we were to bother with this,
            // we'd want to generate a Field ref to an actual member field that had the
            // real value, and wasn't a slot.  Then the renderers, etc, could get
            // directly at the real value without using the slot -- a tad faster.  Then
            // stuff like the auto-notify code would all need to happen in the key, tho
            // then all our "traditional" setters (for hand-coding convenience, and at
            // least for save file backward compat) would need to use the Key to do the
            // setting for the appropriate triggers (except for "take" usage)
            
            java.lang.reflect.Field f = null;
            if (fieldName != null) {
                try {
                    f = LWComponent.class.getField(fieldName);
                    System.out.println("Found field: " + f);
                } catch (Throwable t) {
                    tufts.Util.printStackTrace(t);
                }
            }
            field = f;
        }
        Property getSlot(LWComponent c) {
            try {
                return (Property) field.get(c);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t);
            }
            return null;
        }
        */
        /*
        Object getValue(LWComponent c) {
            if (field == null)
                return getSlot(c).get();

            try {
                return field.get(c);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t);
            }
            return null;
        }
        void setValue(LWComponent c, Object value) {
            if (field == null)
                getSlot(c).set(value);
            
            try {
                field.set(c, value);
            } catch (Throwable t) {
                tufts.Util.printStackTrace(t);
            }
        }
        */
        



