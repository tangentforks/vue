package tufts.vue;

import java.awt.Cursor;
import java.awt.Color;
import java.awt.Font;

public interface VueConstants
{
    // todo: move most of this stuff to prefs
    
    static Font DefaultFont = new Font("SansSerif", Font.PLAIN, 18);
    static Font SmallFont = new Font("SansSerif", Font.PLAIN, 10);
    
    // todo: create our own cursors for most of these
    // named cursor types
    static Cursor CURSOR_HAND     = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    static Cursor CURSOR_MOVE     = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    static Cursor CURSOR_WAIT     = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    static Cursor CURSOR_CROSSHAIR= Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    static Cursor CURSOR_DEFAULT  = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    
    // tool cursor types
    static Cursor CURSOR_ZOOM_IN  = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
    static Cursor CURSOR_ZOOM_OUT = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
    static Cursor CURSOR_PAN      = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    static Cursor CURSOR_ARROW    = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    static Cursor CURSOR_SUBSELECT= Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR); // white arrow

    static java.awt.Stroke STROKE_ONE = new java.awt.BasicStroke(1f);
    static java.awt.Stroke STROKE_TWO = new java.awt.BasicStroke(2f);
    static java.awt.Stroke STROKE_DEFAULT = STROKE_ONE;
    static java.awt.Stroke STROKE_SELECTION = new java.awt.BasicStroke(1f);
    static java.awt.Stroke STROKE_SELECTION_DYNAMIC = new java.awt.BasicStroke(1f);

    static java.awt.Color COLOR_SELECTION = java.awt.Color.blue;
    static java.awt.Color COLOR_SELECTION_DRAG = java.awt.Color.gray;
    static java.awt.Color COLOR_INDICATION = java.awt.Color.red;
    static java.awt.Color COLOR_DEFAULT = java.awt.Color.black;
    static java.awt.Color COLOR_FAINT = java.awt.Color.lightGray;

    static java.awt.Color DEFAULT_NODE_COLOR = new Color(200, 200, 255);


    
}
