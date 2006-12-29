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

import tufts.vue.gui.GUI;
import tufts.vue.gui.TextRow;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;

//import java.awt.font.LineBreakMeasurer;
//import java.awt.font.TextAttribute;

/**
 * A multi-line editable text object that supports left/center/right
 * aligment for its lines of text.
 *
 * Used in two modes: (1) "normal" mode -- used to paint multi-line
 * text objects (labels, notes, etc) and (2) "edit".  In normal mode,
 * this JComponent has no parent -- it isn't added to any AWT
 * hierarchy -- it's only used to paint as part of the
 * LWMap/LWContainer paint tree (via the draw(Graphics2D)) method.  In
 * edit mode, it's temporarily added to the canvas so it can receive
 * user input.  Only one instance of these is ever added and active in
 * AWT at the same time.  We have to do some funky wrangling to deal
 * with zoom, because the JComponent can't paint and interact on a
 * zoomed (scaled) graphics context (unless we were to implement mouse
 * event retargeting, which is a future possibility).  So if there is
 * a scale active on the currently displayed map, we manually derive a
 * new font for the whole text object (the Document) and set it to
 * that temporarily while it's active in edit mode, and then re-set it
 * upon removal.  Note that users of this class (e.g., LWNode) should
 * not bother to paint it (call draw()) if it's in edit mode
 * (getParent() != null) as the AWT/Swing tree is dealing with that
 * while it's in its activated edit state.
 *
 * We use a JTextPane because it supports a StyledDocument, which is
 * what we need to be able to set left/center/right aligment for all
 * the paragraphs in the document.  This is a bit heavy weight for our
 * uses right now as we only make use of one font at a time for the whole
 * document (this is the heaviest weight text component in Swing).
 * JTextArea would have worked for us, except it only supports its
 * fixed default of left-aligned text.  However, eventually we're
 * probably going to want to suport intra-string formatting (fonts,
 * colors, etc) and so we'll be ready for that, with the exception of
 * the hack mentioned above to handle zooming (tho we could
 * theoretically iterate through the whole document, individually
 * deriving zoomed fonts for every font found in the Document.)
 *
 * Note that you can get center/variable alignment even with
 * a JLabel if it's text starts with &lt;HTML&gt;, and you
 * put in a center tag.  Unfortunately, JTextArea's don't
 * do HTML w/out setting up a StyledDocument manually.
 *
 *
 * @author Scott Fraize
 * @version $Revision: 1.40 $ / $Date: 2006-12-29 23:22:31 $ / $Author: sfraize $
 *
 */

class TextBox extends JTextPane
    implements VueConstants
               , FocusListener
               , KeyListener
               , DocumentListener
{
// todo: duplicate not working[? for wrap only? ]

    private static final boolean WrapText = LWNode.WrapText;
    private static final Color SelectionColor = VueResources.getColor("mapViewer.textBox.selection.color");
    
    private static boolean TestDebug = false;
    private static boolean TestHarness = false;
    
    private LWComponent lwc;
    private float mapX;
    private float mapY;
    private float mapWidth;
    private float mapHeight;
    private boolean wasOpaque; /** were we opaque before we started an edit? */
    private MutableAttributeSet mAttributeSet;
    private float mMaxCharWidth;
    private float mMaxWordWidth;
    
    private boolean mKeepHeight = false;
    
    TextBox(LWComponent lwc)
    {
        this(lwc, null);
    }

    TextBox(LWComponent lwc, String text)
    {
        if (DEBUG.TEXT && DEBUG.LAYOUT) tufts.Util.printClassTrace("tufts.vue.", "NEW TextBox, txt=" + text);
        if (TestDebug||DEBUG.TEXT) out("NEW [" + text + "] " + lwc);
        
        this.lwc = lwc;
        //setBorder(javax.swing.border.LineBorder.createGrayLineBorder());
        // don't set border -- adds small margin that screws us up, especially
        // at high scales
        setDragEnabled(false);
        setBorder(null);
        if (text != null)
            setText(text);
        setMargin(null);
        setOpaque(false); // don't bother to paint background
        setVisible(true);
        //setFont(SmallFont);
        // PC text pane will pick this font up up as style for
        // document, but mac ignores.
            
        //setAlignmentX(1f);//nobody's paying attention to this

        //setContentType("text/rtf"); for attributes + unicode, but will need lots of work

        addKeyListener(this);
        addFocusListener(this);
        getDocument().addDocumentListener(this);
        setSize(getPreferredSize());
        if (VueUtil.isWindowsPlatform() && SelectionColor != null)
            setSelectionColor(SelectionColor);
        
        if (TestDebug||DEBUG.TEXT) out("constructed " + getSize());
    }

    /*
    public String getText() {
        //java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        Document doc = getDocument();
        String text = null;
        try {
            // better to use doc.getText(0, doc.getLength())
            //getEditorKit().write(buf, doc, 0, doc.getLength());
            text = doc.getText(0, doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (text == null || text.length() == 0)
            return ":"+super.getText();
        return text;
        //return buf.toString();
    }
    */

    public float getMaxCharWidth() {
        return mMaxCharWidth;
    }

    public float getMaxWordWidth() {
        return mMaxWordWidth;
    }

    LWComponent getLWC() {
        return this.lwc;
    }


    /*
     * When activated for editing, draw an appropriate background color
     * for the node -- the we need to do because if it's a small on-screen
     * font at the moment (depending on zoom level), we make the
     * text box always appear at the 100% zoomed font (because we're
     * not managing scaled repaint of the added object or retargeting
     * scaled mouse events, etc).  Also, when it's transparent, the
     * whole map has to be repainted each cursor blink just in case
     * there is some LWComponent under the transparent text item.
     * (Tho with a very small clip region).
     */
    private Font preZoomFont = null;
    private String mUnchangedText;
    private Dimension mUnchangedSize;
    private boolean keyWasPressed = false;
    private static final int MinEditSize = 11; // todo: prefs
    // todo bug: on PC, font edits at size < 11 are failing produce the
    // right selection or cursor coordinates, and what you see
    // is NOT what you get anymore.  ACTUALLY, this may be due
    // to special charactes in the string -- it was a piece of
    // pasted HTML text with "1/2" chars and \226 dashes..
    // Okay, no -- even vanilla text at 10 point does it
    // (SansSerif-plain-10) -- okay, this is crap -- a "regular"
    // node v.s. a text node is working fine down at nine-point,
    // tho it does have only 3 lines -- ugh, this is going
    // to require alot of fiddling and testing.


    /** called by MapViewer before we go active on the map */
    void saveCurrentState()
    {
        mUnchangedText = getText();
        mUnchangedSize = getSize();
    }
    
    /** deprecated */
    void saveCurrentText() {
        saveCurrentState();
    }
    
    public void addNotify()
    {
        if (TestDebug||DEBUG.TEXT) out("*** ADDNOTIFY ***");
        if (getText().length() < 1)
            setText("<label>");
        keyWasPressed = false;
        Dimension size = getSize();
        super.addNotify();
        // note: we get a a flash/move if we add the border before the super.addNotify()
        if (false && TestDebug)
            ; //setBorder(javax.swing.BorderFactory.createLineBorder(Color.green));
        else {
            // ADDING THIS BORDER INCREASES THE PREFERRED SIZE
            // Width goes up by 4 pix and height goes up by a line because
            // actual getSize width no longer fits.  Very convoluted.
            //setBorder(javax.swing.border.LineBorder.createGrayLineBorder());
        }
        java.awt.Container parent = getParent();
        if (parent instanceof MapViewer) { // todo: could be a scroller!
            double zoom = ((MapViewer)parent).getZoomFactor();
            // todo: also account for getScale of children!
            zoom *= lwc.getScale();
            if (zoom != 1.0) {
                Font f = lwc.getFont();
                float zoomedPointSize = (float) (f.getSize() * zoom);
                if (zoomedPointSize < MinEditSize)
                    zoomedPointSize = MinEditSize;
                preZoomFont = f;
                setDocumentFont(f.deriveFont(f.getStyle(), zoomedPointSize));
                if (WrapText) {
                    double boxZoom = zoomedPointSize / preZoomFont.getSize();
                    size.width *= boxZoom;
                    size.height *= boxZoom;
                } else {
                    setSize(getPreferredSize());
                }
            } else {
                // this forces the AWT to redraw this component
                // (just calling repaint doesn't do it).
                // When zoomed we must do this (see above), so
                // in that case it's already handled.
                setDocumentFont(lwc.getFont());
            }
        }
            
        wasOpaque = isOpaque();
        Color c = lwc.getRenderFillColor();
        //if (c == null && lwc.getParent() != null && lwc.getParent() instanceof LWNode)
        final LWContainer nodeParent = lwc.getParent();
        if (c == null && nodeParent != null)
            c = nodeParent.getRenderFillColor(); // todo: only handles 1 level transparent embed!
        // todo: could also consider using map background if the node itself
        // is transpatent (has no fill color)

        // This is a complete hack:
        if (c == null && nodeParent instanceof LWSlide) {
            try {
                c = VUE.getActivePathway().getMasterSlide().getFillColor();
            } catch (Throwable t) { if (DEBUG.Enabled) t.printStackTrace(); }
        }

        // TODO: this workaround until we can recursively find a real fill color
        // node that for SLIDES, we'd have to get awfully fancy and
        // usually pull the color of the master slide (unless the slide
        // happened to have it's own special color).  Only super clean
        // way to do this would be to have established some kind of
        // rendering pipeline record... (yeah, right)
        if (c == null)
            c = Color.gray;
        
        if (c != null) {
            // note that if we set opaque to false, interaction speed is
            // noticably slowed down in edit mode because it has to consider
            // repainting the entire map each cursor blink as the object
            // is transparent, and thus it's background is the displayed
            // map.  So if we can guess at a reasonable fill color in edit mode,
            // we temporarily set us to opaque.
            setOpaque(true);
            setBackground(c);
        }
        setSize(size);

        Dimension preferred = getPreferredSize();
        int w = getWidth() + 1;
        if (w < preferred.width)
            mKeepTextWidth = true;
        else
            mKeepTextWidth = false;
        if (TestDebug||DEBUG.TEXT) out("width+1=" + w + " < preferred.width=" + preferred.width + " fixedWidth=" + mKeepTextWidth);
        
        if (TestDebug||DEBUG.TEXT) out("addNotify end: insets="+getInsets());
        mFirstAfterAddNotify = true;
    }
    
    /*
     * Return to the regular transparent state.
     */
    public void removeNotify()
    {
        if (TestDebug||DEBUG.TEXT) out("*** REMOVENOTIFY ***");
        
        //------------------------------------------------------------------
        // We need to clear any text selection here as a workaround
        // for an obscure bug where sometimes if the focus change is
        // to a pop-up menu, the edit properly goes inactive, but the
        // selection within it is still drawn with it's highlighted
        // background.
        clearSelection();
        //------------------------------------------------------------------
        
        super.removeNotify();

        if (mFirstAfterAddNotify == false) {
            // if cleared, it was used
            out("restoring expanded width");
            setSize(new Dimension(getWidth()-1, getHeight()));
            //out("SKPPING restoring expanded width");
        } else
            mFirstAfterAddNotify = false;
        
        setBorder(null);
        if (preZoomFont != null) {
            setDocumentFont(preZoomFont);
            preZoomFont = null;
            if (WrapText)
                adjustSizeDynamically();
            else
                setSize(getPreferredSize());
        }
        
        if (wasOpaque != isOpaque())
            setOpaque(wasOpaque);
        if (TestDebug||DEBUG.TEXT) out("*** REMOVENOTIFY end: insets="+getInsets());
    }

    public void clearSelection() {
        // this set's the "mark to the point" -- sets them to the same
        // location, thus clearing the selection.
        setCaretPosition(getCaretPosition());
    }

    public void setText(String text)
    {
        if (getDocument() == null) {
            out("creating new document");
            setStyledDocument(new DefaultStyledDocument());
        }
        /*try {
            doc.insertString(0, text, null);
        } catch (Exception e) {
            System.err.println(e);
            }*/ 
        if (TestDebug||DEBUG.TEXT) out("setText[" + text + "]");
        super.setText(text);
        copyStyle(this.lwc);
        if (WrapText) {
            ;
        } else {
            setSize(getPreferredSize());
        }
    }

    public boolean keepHeight() {
        if (mKeepHeight) {
            mKeepHeight = false;
            return true;
        } else
            return false;
    }


    private void setDocumentFont(Font f)
    {
        if (DEBUG.TEXT) out("setDocumentFont " + f);
        SimpleAttributeSet a = new SimpleAttributeSet();
        setFontAttributes(a, f);
        StyledDocument doc = getStyledDocument();
        doc.setParagraphAttributes(0, doc.getEndPosition().getOffset(), a, false);
        computeMinimumWidth(f, lwc.getLabel());
        mKeepHeight = true;
    }

    private void setDocumentColor(Color c)
    {
        StyleConstants.setForeground(mAttributeSet, c);
        StyledDocument doc = getStyledDocument();
        doc.setParagraphAttributes(0, doc.getEndPosition().getOffset(), mAttributeSet, true);
    }


    private static void setFontAttributes(MutableAttributeSet a, Font f)
    {
        StyleConstants.setFontFamily(a, f.getFamily());
        StyleConstants.setFontSize(a, f.getSize());
        StyleConstants.setItalic(a, f.isItalic());
        StyleConstants.setBold(a, f.isBold());
    }

    
    // this called every time setText is called to ensure we get
    // the font style encoded in our owning LWComponent
    void copyStyle(LWComponent c)
    {
        if (DEBUG.TEXT) out("copyStyle " + c);
        SimpleAttributeSet a = new SimpleAttributeSet();
        if (TestHarness || c instanceof LWNode && ((LWNode)c).isTextNode())
            StyleConstants.setAlignment(a, StyleConstants.ALIGN_LEFT);
        else
            StyleConstants.setAlignment(a, StyleConstants.ALIGN_CENTER);
        StyleConstants.setForeground(a, c.getTextColor());
        final Font font = c.getFont();
        setFontAttributes(a, font);

        StyledDocument doc = getStyledDocument();
        if (DEBUG.TEXT) getPreferredSize();
        doc.setParagraphAttributes(0, doc.getEndPosition().getOffset(), a, false);
        if (DEBUG.TEXT) getPreferredSize();
        computeMinimumWidth(font, lwc.getLabel());
        mKeepHeight = true;
        mAttributeSet = a;

        if (WrapText) {
            // adjust WIDTH ONLY (or: attempt to keep aspect)
            //adjustSize(false);
        } else {
            setSize(getPreferredSize());
            setSize(getPreferredSize());
        }
    }


    /** compute mMaxWordWidth and mMaxCharWidth */
    private void computeMinimumWidth(Font font, String text) {
        mMaxCharWidth = (float) font.getMaxCharBounds(DefaultFontContext).getWidth();
        try {
            mMaxWordWidth = maxWordWidth(font, text);
        } catch (Exception e) {
            mMaxWordWidth = mMaxCharWidth;
        }
    }

    private static final boolean DebugWord = false;
    private static final int BigWordLen = 9;
    private float maxWordWidth(Font font, String text) {

        if (text == null || text.length() == 0)
            return mMaxCharWidth;

        if (text.length() > 512) // provide a rough figure if string is long
            return mMaxCharWidth * BigWordLen;

        if (text.indexOf(' ') < 0 && text.indexOf('\n') < 0) // if no spaces, specal case no wrapping
            return (float) font.getStringBounds(text, DefaultFontContext).getWidth();

        int maxRunIdx = 0;
        int maxRunLen = 0;
        int curRunIdx = 0;
        int curRunLen = 0;
        boolean lastWasBreak = false;
        float maxWidth = 0;
        final int len = text.length();

        for (int i = 0; i <= len; i++) {
            char c;
            if (i < len) {
                c = text.charAt(i);
                curRunLen++;
            } else
                c = 0;

            if (DebugWord) out("char[" + c + "] ci="+curRunIdx + " cl=" + curRunLen);

            // add '/' as word break character if no whitespace?
            if (c == 0 || Character.isWhitespace(c) || c == '.' || c == ',')
                ; // treat as a word break
            else
                continue;
                
            if (c != 0) { // if we're not at the end
                try {
                    char whiteChar;
                    do {
                        curRunLen++;
                        whiteChar = text.charAt(++i);
                        if (DebugWord) out("char{" + whiteChar + "} ci="+curRunIdx + " cl=" + curRunLen);
                    } while (Character.isWhitespace(whiteChar));
                } catch (StringIndexOutOfBoundsException e) {
                    if (DebugWord) out("charEOS ci="+curRunIdx + " cl=" + curRunLen);
                }
                curRunLen--;
                i--;
            }
            float wordWidth = (float)
                font.getStringBounds(text,
                                     curRunIdx,
                                     curRunIdx + curRunLen,
                                     DefaultFontContext).getWidth();
            
            if (DebugWord) out("word[" + text.substring(curRunIdx, curRunIdx + curRunLen) + "] w=" + wordWidth);
                
            if (wordWidth > maxWidth) {
                if (c == 0 && curRunIdx == 0) {
                    // If no whitespace in the whole thing, allow some breaking (should never happen currently)
                    return wordWidth < mMaxCharWidth * BigWordLen ? wordWidth : mMaxCharWidth * BigWordLen;
                } else {
                    maxWidth = wordWidth;
                    maxRunIdx = curRunIdx;
                    maxRunLen = curRunLen;
                    if (DebugWord) out("MI="+curRunIdx + " ML=" + curRunLen + " w=" + wordWidth);
                }
            }
            curRunIdx = i + 1;
            curRunLen = 0;
        }
        
        if (DebugWord || DEBUG.TEXT) out("maxWord[" + text.substring(maxRunIdx, maxRunIdx + maxRunLen) + "] w=" + maxWidth);

        return maxWidth;
    }

    
    /** override Container.doLayout: only called when we've been added to a map for interactive editing.
     * Is called during interactive edit's after each modification.
     */
    public void doLayout()
    {
        if (getParent() instanceof MapViewer) {
            // Automatic layout (e.g. FlowLayout)
            // produces two layout passes -- perhaps
            // this is why we need to call this TWICE
            // here so that the box size doesn't
            // temporarily flash bigger on every update.
            if (TestDebug || DEBUG.LAYOUT) out("doLayout w/adjustSizeDynamically");
            if (false)
                out("skipping size fix-up");
            else {
                if (WrapText)
                    adjustSizeDynamically();
                else {
                    setSize(getPreferredSize());
                    setSize(getPreferredSize());
                }
            }
        } else {
            if (!TestHarness)
                new Throwable(this + " UNPARENTED doLayout").printStackTrace();
        }
        //super.layout();
    }

    private boolean mFirstAfterAddNotify = false;
    private boolean mKeepTextWidth = false;
    /** adjust size given new text that has been typed into this component while it's being display */
    private void adjustSizeDynamically() {
        adjustSize(mKeepTextWidth);
    }

    private void adjustSize(boolean keepTextWidth) {
        if (TestDebug||DEBUG.TEXT) out("adjustSize, keepWidth=" + keepTextWidth);

        Dimension preferred = getPreferredSize();
        Dimension newSize;

        if (keepTextWidth) {
            newSize = new Dimension();
            newSize.width = getWidth();
            if (mFirstAfterAddNotify) {
                if (TestDebug||DEBUG.TEXT) out("adding 1 to width");
                // ensure there is room to draw cursor at end of line.
                // Actually, it appears that once the addNotify is done, getPreferredSize
                // returns a width increased by one: it must notice it's now editable
                // and want to include room for the cursor.
                newSize.width++;
                mFirstAfterAddNotify = false;
            }
            newSize.height = preferred.height;
        } else {
            // First call to preferred size may be based somewhat on current size
            // E.g., if current size doesn't fit all on line line, will give a preferred
            // that includes two lines.  However, if we then set our size to the new preferred
            // size, it turns out it all DOES fit on one line, and then the height becomes
            // reported as only one line's worth in the second getPreferredSize().
            setSize(preferred);
            newSize = getPreferredSize();
            newSize.width++; // ensure enough room to draw cursor at end of line
        }
        if (TestDebug||DEBUG.TEXT) out("adjustTo", newSize, keepTextWidth ? "(keep width)" : "(expand width)");
        setSize(newSize);
        //new Throwable("adjustSizeDynamically").printStackTrace();
    }
    
    public void keyReleased(KeyEvent e) { e.consume(); }
    public void keyTyped(KeyEvent e) {
        // todo: would be nice if centered labels stayed center as you typed them
        //setLocation((int)lwc.getLabelX(), (int)lwc.getLabelY());
        // needs something else, plus can't work at zoom because
        // width isn't updated till the end (because width at + zoom
        // is overstated based on temporarily scaled font)
        // Man, it would be REALLY nice if we could paint the
        // real component in a scaled GC w/out the font tweaking --
        // problems like this would go away.
    }
    private static boolean isFinishEditKeyPress(KeyEvent e) {
        // if we hit return key either on numpad ("enter" key), or
        // with any modifier down except a shift alone (in case of
        // caps lock) complete the edit.
        return e.getKeyCode() == KeyEvent.VK_ENTER &&
            (
             e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD ||
             (e.getModifiersEx() != 0 && !e.isShiftDown())
             )
            == true;
        //== false; // reversed logic of below description
    }

    private Container removeAsEdit() {
        Container parent = getParent();
        if (parent != null)
            parent.remove(this);
        else
            out("FAILED TO FIND PARENT ATTEMPTING TO REMOVE SELF");
        return parent;
    }
    
    public void keyPressed(KeyEvent e)
    {
        if (DEBUG.KEYS) out(e.toString());
        //System.out.println("TextBox: " + e);

        //if (VueUtil.isAbortKey(e)) // check for ESCAPE for CTRL-Z or OPTION-Z if on mac
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            e.consume();
            getParent().remove(this); // will trigger a save (via focusLost)
            super.setText(mUnchangedText); 
            setSize(mUnchangedSize); // todo: won't be good enough if we ever resize the actual node as we type
        } else if (isFinishEditKeyPress(e)) {
            keyWasPressed = true;
            e.consume();
            getParent().remove(this); // will trigger a save (via focusLost)
        } else if (e.getKeyCode() == KeyEvent.VK_U && e.isMetaDown()) {
            e.consume();
            String t = getText();
            if (e.isShiftDown())
                setText(t.toUpperCase()); // upper whole string
            else
                setText(Character.toTitleCase(t.charAt(0)) + t.substring(1)); // upper first char
        } else
            keyWasPressed = true;

        // action keys will be ignored if we consume this here!
        // (e.g., "enter" does nothing)
        //e.consume();
    }

    /**
     * This is what triggers the final save of the new text value to the LWComponent,
     * and notify's the UndoManager that a user action was completed.
     */
    public void focusLost(FocusEvent e)
    {
        if (TestDebug||DEBUG.FOCUS) outc("focusLost to " + e.getOppositeComponent());
        if (TestHarness == false && getParent() != null)
            getParent().remove(this);
        if (keyWasPressed) { // TODO: as per VueTextField, need to handle drag & drop detect
            // only do this if they typed something (so we don't wind up with "label"
            // for the label on an accidental edit activation)
            if (TestDebug||DEBUG.FOCUS) out("key was pressed; setting label to: [" + getText() + "]");
            final String text = getText();
            computeMinimumWidth(lwc.getFont(), text); // do before setLabel
            lwc.setLabel0(text, false);
            VUE.getUndoManager().mark();
        }
        lwc.notify(this, LWKey.Repaint);
    }

    public void focusGained(FocusEvent e)
    {
        if (TestDebug||DEBUG.FOCUS) outc("focusGained from " + e.getOppositeComponent());
    }

    /** do not use, or won't be able to get out actual text height */
    public void setPreferredSize(Dimension preferredSize) {
        if (true||TestDebug) out("setPreferred " + preferredSize);
        super.setPreferredSize(preferredSize);
    }
    public Dimension getPreferredSize() {
        Dimension s = super.getPreferredSize();
        //getMinimumSize();//debug
        //s.width = (int) lwc.getWidth();
        //System.out.println("MTP lwcWidth " + lwc.getWidth());
        if (getParent() != null)
            s.width += 1; // leave room for cursor, which at least on mac gets clipped if at EOL
        //if (getParent() == null)
        //    s.width += 10;//fudge factor for understated string widths (don't do this here -- need best accuracy here)
        if (TestDebug||DEBUG.TEXT) out("getPrefer", s);
        //if (TestDebug && DEBUG.META) new Throwable("getPreferredSize").printStackTrace();
        return s;
    }

    public void setSize(Size s) {
        setSize(s.dim());
    }

    public void setSize(Dimension s) {
        if (TestDebug||DEBUG.TEXT) out("setSize", s);
        super.setSize(s);
        if (preZoomFont == null) {
            // preZoomFont only set if we had to zoom the font
            this.mapWidth = s.width;
            this.mapHeight = s.height;
        }
    }
    /*
    public void setHeight(int h) {
        // todo: may need to all the above setSize for font code
        super.setSize(getWidth(), h);
    }
    */

    /**
     * Set the size to the given size, increasing or decreasing height as
     * needed to provide a fit around our text
     */
    public void setSizeFlexHeight(Size newSize) {

        //------------------------------------------------------------------
        // Tell the JTextPane to take on the size requested.  It will
        // then set the preferred height to the minimum height able to
        // contain the given text at that width.
        //------------------------------------------------------------------

        setSize(newSize);
        
        //------------------------------------------------------------------
        // Now adjust our height to the new preferred height, which should
        // just contain our text.
        //------------------------------------------------------------------
        
        final Dimension s = getPreferredSize();
        s.width = getWidth();
        if (TestDebug||DEBUG.TEXT) out("flexHeigt", s);
        super.setSize(s.width, s.height);
    }


    
    public void setSize(float w, float h) {
        setSize(new Dimension((int)w, (int)h));
    }
    public void setPreferredSize(float w, float h) {
        setPreferredSize(new Dimension((int)w, (int)h));
    }
    
    public Dimension getSize() {
        Dimension s = super.getSize();
        //s.width = (int) lwc.getWidth();
        if (TestDebug||DEBUG.TEXT) out("getSize", s);
        //if (DEBUG.TEXT&&DEBUG.META) new Throwable("getSize").printStackTrace();
        return s;
    }
    public void setMaximumSize(Dimension s) {
        if (true||TestDebug) out("setMaximumSize", s);
        super.setMaximumSize(s);
    }
    public Dimension getMaximumSize() {
        Dimension s = super.getMaximumSize();
        if (TestDebug||DEBUG.TEXT) out("getMaximumSize", s);
        return s;
    }
    public void setMinimumSize(Dimension s) {
        if (true||TestDebug) out("setMinimumSize", s);
        super.setMinimumSize(s);
    }
    public Dimension getMinimumSize() {
        Dimension s = super.getMinimumSize();
        if (TestDebug||DEBUG.TEXT) out("getMinimumSize", s);
        return s;
    }

    public void reshape(int x, int y, int w, int h) {
        if (TestDebug||DEBUG.TEXT) {
            boolean change = getX() != x || getY() != y || getWidth() != w || getHeight() != h;
            if (change) {
                out(" oldshape " + tufts.Util.out(getBounds()));
                out("  reshape " + w + "x" + h + " " + x + "," + y);
            }
            //out("  reshape " + w + "x" + h + " " + x + "," + y + (change ? "" : " (no change)"));
            if (DEBUG.META && change)
                new Throwable("reshape").printStackTrace();
        }
        super.reshape(x, y, w, h);
        if (TestDebug||DEBUG.TEXT) {
            Rectangle b = getBounds();
            if (b.x != x || b.y != y || b.width != w || b.height != h)
                out("BADBOUNDS " + tufts.Util.out(b));
        }
    }

    public float getMapX() { return this.mapX; }
    public float getMapY() { return this.mapY; }
    public float getMapWidth() { return mapWidth; }
    public float getMapHeight() { return mapHeight; }

    public void setMapLocation(float x, float y)
    {
        this.mapX = x;
        this.mapY = y;
    }
        

    public boolean intersectsMapRect(Rectangle2D rect)
    {
        return rect.intersects(mapX, mapY, mapWidth, mapHeight);
    }

    // todo: this doesn't account for scaling if laid
    // out in a child...
    public boolean containsMapLocation(float x, float y)
    {
        return
            x >= mapX && y >= mapY &&
            x <= mapX + mapWidth &&
            y <= mapY + mapHeight;
    }

    /*
    void resizeToWidth(float w)
    {
        int width = (int) (w + 0.5f);
        setSize(new Dimension(width, 999));  // can set height to 1 as we're ignore the set-size
        // now the preferred height will be set to the real
        // total text height at that width -- pull it back out and set actual size to same
        Dimension ps = getPreferredSize();
        setSize(new Dimension(width, (int)ps.getHeight()));
    }
    */


    public void Xpaint(Graphics g) {
        super.paint(g);
        g.setColor(Color.gray);
        g.setClip(null);
        g.drawRect(0,0, getWidth(), getHeight());
    }
    
    public void paintComponent(Graphics g)
    {
        if (TestDebug||DEBUG.TEXT) out("paintComponent");

        MapViewer viewer = (MapViewer) javax.swing.SwingUtilities.getAncestorOfClass(MapViewer.class, this);
        if (viewer != null)
            ((Graphics2D)g).setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, viewer.AA_ON);
        // turn on anti-aliasing -- the cursor repaint loop doesn't
        // set anti-aliasing, so text goes jiggy around cursor/in selection if we don't do this
        super.paintComponent(g);
        if (true) {
            // draw a border (we don't want to add one because that changes the preferred size at a bad time)
            g.setColor(Color.gray);
            g.setClip(null);
            final int xpad = 1;
            final int ypad = 1;
            g.drawRect(-xpad,-ypad, getWidth()+xpad*2-1, getHeight()+ypad*2-1);
        }
    }

    private static final BasicStroke MinStroke = new BasicStroke(1/8f);
    private static final BasicStroke MinStroke2 = new BasicStroke(1/24f);

    /** @return true if hue value of Color is black, ignoring any alpha */
    private boolean isBlack(Color c) {
        return (c.getRGB() & 0xFFFFFF) == 0;
    }
    
    public void draw(DrawContext dc)
    {
        if (TestDebug||DEBUG.TEXT) out("draw");

        if (getParent() != null)
            System.err.println("Warning: 2nd draw of an AWT drawn component!");

        //todo: could try saving current translation or coordinates here,
        // so have EXACT last position painted at.  Tho we really should
        // be able to compute it... problem is may not be at integer
        // boundry at current translation, but have to be when we add it
        // to the map -- tho hey, LWNode could force integer boundry
        // when setting the translation before painting us.
        
        if (DEBUG.BOXES && DEBUG.META) {
            if (lwc.getLabel().indexOf('\n') < 0) {
                TextRow r = new TextRow(lwc.getLabel(), lwc.getFont(), dc.g.getFontRenderContext());
                dc.g.setColor(Color.lightGray);
                r.draw(dc.g, 0, 0);
            }
        }

        boolean inverted;
        if (dc.isBlackWhiteReversed() &&
            (dc.isPresenting() || lwc.isTransparent() /*|| isBlack(lwc.getFillColor())*/) &&
            isBlack(lwc.getTextColor())) {
            //System.out.println("reversing color to white for " + this);
            setDocumentColor(Color.white);
            inverted = true;
        } else
            inverted = false;
        
        //super.paintBorder(g);
        super.paintComponent(dc.g);
        //super.paint(g);

        if (inverted)
            setDocumentColor(Color.black);

        // draw a border for links -- why?
        // and even if, better to handle in LWLink
        /*
        if (lwc instanceof LWLink) {
            Dimension s = getSize();
            if (lwc.isSelected())
                g.setColor(COLOR_SELECTION);
            else
                g.setColor(Color.gray);
            g.setStroke(MinStroke);
            g.drawRect(0,0, s.width-1, s.height-2);
        }
        */

        Graphics2D g = dc.g;
        if (DEBUG.BOXES) {
            Dimension s = getPreferredSize();
            g.setColor(Color.red);
            dc.setAbsoluteStroke(0.5);
            //g.setStroke(MinStroke);
            g.drawRect(0,0, s.width, s.height);
            //g.drawRect(0,0, s.width-1, s.height);
        }
            
        //s = getMinimumSize();
        //g.setColor(Color.red);
        //g.setStroke(new BasicStroke(1/8f));
        //g.drawRect(0,0, s.width, s.height);

        if (DEBUG.BOXES || getParent() != null) {
            Dimension s = getSize();
            g.setColor(Color.blue);
            dc.setAbsoluteStroke(0.5);
            //g.setStroke(MinStroke);
            g.drawRect(0,0, s.width, s.height);
            //g.drawRect(0,0, s.width-1, s.height);
        }

    }

    private void handleChange() {
        // appears to be happening too late for dynamic size adjust -- current character isnt include
    }
    public void removeUpdate(DocumentEvent de) {
        if (TestDebug||DEBUG.TEXT) out("removeUpdate " + de);
        handleChange();
    }
    public void changedUpdate(DocumentEvent de) {
        if (TestDebug||DEBUG.TEXT) out("changeUpdate " + de.getType() + " len=" + de.getLength());
        handleChange();
    }
    public void insertUpdate(DocumentEvent de) {
        if (TestDebug||DEBUG.TEXT) out("insertUpdate " + de);
        handleChange();
    }

    public String toString()
    {
        return "TextBox[" + lwc + "]";
    }

    private static class TestPanel extends javax.swing.JPanel {
        final TextBox box;
        TestPanel(TextBox box) {
            this.box = box;
            setBorder(new javax.swing.border.EmptyBorder(100,100,100,100));
            setLayout(new java.awt.BorderLayout());
            box.setBorder(javax.swing.BorderFactory.createLineBorder(Color.blue));
            //add(box, java.awt.BorderLayout.CENTER);
            add(box, java.awt.BorderLayout.NORTH);
            //box.setEditable(false);
        }

        public void paint(Graphics g) {
            super.paint(g);
            Dimension d = box.getPreferredSize();
            g.setColor(Color.red);
            g.drawRect(box.getX(), box.getY(), d.width-1, d.height-1);
            g.drawString(d.width + "x" + d.height + " preferred size", box.getX(), box.getY()-2);
            g.setColor(Color.blue);
            g.drawString(box.getWidth() + "x" + box.getHeight() + " size", box.getX(), box.getY() + box.getHeight() + 11);
        }
    }

    private void out(String s) {
        System.out.println("TextBox@" + Integer.toHexString(hashCode()) + " " + s);
    }
    private void out(String s, Dimension d) {
        out(VueUtil.pad(' ', 9, s, true) + " " + tufts.Util.out(d));
    }
    
    private void out(String s, Dimension d, String s2) {
        out(VueUtil.pad(' ', 9, s, true) + " " + tufts.Util.out(d) + " " + s2);
    }
    
    private void outc(String s) {
        System.out.println(this + " " + Integer.toHexString(hashCode()) + " " + s);
    }
    
    

    public static void main(String args[]) {
        VUE.parseArgs(args);
        VUE.initUI();
        TestDebug = true;
        TestHarness = true;
        DEBUG.BOXES = true;
        LWComponent node = new LWNode("Foo");
        TextBox box = new TextBox(node, "One Two Three Four Five Six Seven");
        java.awt.Window w = GUI.createDockWindow("TextBox Resize Test", new TestPanel(box));
        w.setVisible(true);
        //tufts.Util.displayComponent(panel);
    }
    

}
