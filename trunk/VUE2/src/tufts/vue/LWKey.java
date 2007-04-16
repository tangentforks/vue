// 	$Id$	
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


// todo: a Key class, and some are marked as model-changing (default),
// and some not (e.g., repaints, user-action-completed)
// final flags: isChange & isSignal (one or other), isBoundsChange
// by default always safe with isChange=true, isSignal=false, isBoundsChange=true
// can provide isSignal=true & isBoundsChange=false for optimization.

/**
 * Some pre-defined property types.  Any string may be used as an
 * property or event identifier, but you must be sure to use the constant
 * object here for any of these events or they may not be
 * recognized .
 *
 * @version $Revision:  $ / $Date: 2006/01/20 17:17:29 $ / $Author: sfraize $
 */
public interface LWKey {

    public String UserActionCompleted = "user.action.completed";
    
    public LWComponent.Key FillColor = LWComponent.KEY_FillColor; 
    public LWComponent.Key TextColor = LWComponent.KEY_TextColor; 
    public LWComponent.Key StrokeColor = LWComponent.KEY_StrokeColor; 
    public LWComponent.Key StrokeWidth = LWComponent.KEY_StrokeWidth;
    public LWComponent.Key StrokeStyle = LWComponent.KEY_StrokeStyle;
    public LWComponent.Key Font =  LWComponent.KEY_Font;
    public LWComponent.Key FontSize =  LWComponent.KEY_FontSize;
    public LWComponent.Key FontName =  LWComponent.KEY_FontName;
    public LWComponent.Key FontStyle =  LWComponent.KEY_FontStyle;
    public LWComponent.Key Shape = LWNode.KEY_Shape;

    public LWComponent.Key Label = LWComponent.KEY_Label;
    public LWComponent.Key Notes = LWComponent.KEY_Notes;;
    
    //public String FillColor = "fill.color"; 
    //public String TextColor = "text.color"; 
    //public String StrokeColor = "stroke.color"; 
    //public String StrokeWidth = "stroke.width"; 
    //public String Font = "font";
    //public String Shape = "node.shape"; 
    
    // a handy hack: if we want a "key" type more specific than object, but
    // can also refer to a String (which is a final class), String implements
    // CharSequence, so we could use that as a narrower generic for both
    // our LWComonent.Key object, and Strings.
    
    public String Location = "location"; 
    public String Size = "size";
    public String Frame = "frame"; // location & size
    
    //public String Label = "label"; 
    //public String Notes = "notes"; 
    public String Scale = "scale"; 
    public String Resource = "resource"; 
    public String Hidden = "hidden";


    public String Created = "new.component"; // any LWComponets creation event
    //public String Added = "added"; // a child components add-notify
    //public String ChildAdded = "childAdded";// the parent component's add-notify
    public String ChildrenAdded = "hier.childrenAdded";// the parent component's group add-notify
    //public String ChildRemoved = "hier.childRemoved";// the parent component's remove-notify
    public String ChildrenRemoved = "hier.childrenRemoved";// the parent component's group remove-notify
    public String HierarchyChanging = "hier.changing"; // pre-change event for any hierarchy change
    public String HierarchyChanged = "hier.changed"; // post-change event for hierarchy changes during undo operations

    public String Deleting = ":deleting"; // the component's just-before-delete notify
    //public String Deleted = "deleted"; // the component's after-delete notify

    public String LinkAdded = "lwc.link.added"; // a link has been added to this component
    public String LinkRemoved = "lwc.link.removed"; // a link has been removed from this component
    
    public String Repaint = "repaint"; // general: visual change but no permanent data change
    public String RepaintComponent = "repaint.component"; // IMMEDIATELY repaint (don't wait for AWT), but just the component
    public String RepaintAsync = "repaint.async"; // a repaint from an auxillary thread: all visual listeners need immediate repaint

    /** link arrow state: 0=none, 1=start arrow, 2=end arrow, 3=both arrows */
    //public String LinkArrows = "link.arrows";
    public LWComponent.Key LinkArrows = LWLink.KEY_LinkArrows;
    /** link curve state: 0=straight, 1=1 control point (Quadric), 2=2 control points (Cubic) */
    public String LinkCurves = "link.curves";

    /* the map filter has changed somehow */
    public String MapFilter = "map.filter"; 
    
    
}
