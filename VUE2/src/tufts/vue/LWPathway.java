/*
 * LWPathway.java
 *
 * Created on June 18, 2003, 1:37 PM
 *
 * @author  Jay Briedis
 */

package tufts.vue;

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.geom.Line2D;
import java.awt.geom.Area;
import java.util.ArrayList;

public class LWPathway
    implements Pathway, LWComponent.Listener
{
    private LinkedList elementList = null;
    private int weight = 1;
    private String comment = "";
    private boolean ordered = false;
    private Color borderColor = Color.blue;
    private LWMap map = null;
    private String label = "";
    private int currentIndex;
    private String notes = "";
    private boolean showing = true;
    private boolean open = true;
    private boolean locked = false;
    private boolean mDoingXMLRestore = false;
    private ArrayList colorArray = new ArrayList();
    
    private ArrayList elementPropertyList = null;
    
    /**default constructor used for marshalling*/
    public LWPathway() {
        //added by Daisuke
        elementList = new LinkedList();
        elementPropertyList = new ArrayList();
        
        currentIndex = -1;
        mDoingXMLRestore = true;

        /*

        colorArray.add(new Color(255, 255, 51));
        colorArray.add(new Color(255, 102, 51));
        colorArray.add(new Color(204, 51, 204));
        colorArray.add(new Color(51, 204, 204));
        colorArray.add(new Color(51, 204, 51));

        // need to save & restore this color -- in any case,
        // we can't rely on having an active map while
        // simply doing a raw data-restore -- SMF 2004-01-29 20:27.48

          LWPathwayManager manager = VUE.getActiveMap().getPathwayManager();
        if(manager != null && manager.getPathwayList() != null){
            int num = manager.getPathwayList().size();
            borderColor = (Color)colorArray.get(num % 5);
        }
        System.out.println("manager: " + manager.toString());
        System.out.println("pathway border color: " + borderColor.toString());
        
        */
    }
    
    public LWPathway(LWMap map, String label) {
        this(label);
        this.map = map;        
    }
    
    /** Creates a new instance of LWPathway with the specified label */
    public LWPathway(String label) {
        this.setLabel(label);
        elementList = new LinkedList();
        elementPropertyList = new ArrayList();
        currentIndex = -1;
        
        colorArray.add(new Color(153, 51, 51));
        colorArray.add(new Color(204, 51, 204));
        colorArray.add(new Color(51, 204, 51));
        colorArray.add(new Color(51, 204, 204));
        colorArray.add(new Color(255, 102, 51));
        colorArray.add(new Color(51, 102, 204));
        
        LWPathwayManager manager = VUE.getActiveMap().getPathwayManager();
        if(manager != null && manager.getPathwayList() != null){
            int num = manager.getPathwayList().size();
            borderColor = (Color)colorArray.get(num % 6);
        }
        
        System.out.println("manager: " + manager.toString());
        System.out.println("pathway border color: " + borderColor.toString());
    }
     
    /** adds an element to the end of the pathway */
    public void addElement(LWComponent element) {
       elementList.add(element);
       elementPropertyList.add(new LWPathwayElementProperty(element.getID()));
       element.addLWCListener(this);       
       element.addPathwayRef(this);
       if (currentIndex == -1) currentIndex = length() - 1;
       //maybe need to repaint the view?
    }
    
    public void LWCChanged(LWCEvent e)
    {
        if (e.getWhat() == LWCEvent.Deleting) {
            removeElement(elementList.indexOf(e.getComponent()), true);
        }
    }
    
    public LWMap getPathwayMap(){
        return map;
    }
    
    
    public void setShowing(boolean showing){
        this.showing = showing;
    }
    
    public void toggleShowing(){
        if(this.showing)
            this.showing = false;
        else
            this.showing = true;
    }
    
    public boolean isShowing(){
        return showing;
    }
    
    public boolean getShowing(){
        return showing;
    }
    
    public void setLocked(){
         if(this.locked)
            this.locked = false;
        else
            this.locked = true;
    }
    
    public boolean getLocked(){
        return locked;
    }
    
    public void setOpen(boolean open){
        this.open = open;
    }
    
    public void setOpen(){
        if(this.open)
            this.open = false;
        else
            this.open = true;
    }
    
    public boolean getOpen(){
        return open;
    }
    
    private static final AlphaComposite PathTranslucence = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f);
    private static final AlphaComposite PathSelectedTranslucence = PathTranslucence;
    //private static final AlphaComposite PathSelectedTranslucence = AlphaComposite.Src;
    //private static final AlphaComposite PathSelectedTranslucence = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);

    private static float dash_length = 4;
    private static float dash_phase = 0;
    public void drawPathway(DrawContext dc){
        Iterator i = this.getElementIterator();
        Graphics2D g = dc.g;

        if (dc.getIndex() % 2 == 0)
            dash_phase = 0;
        else
            dash_phase = 0.5f;
        if (DEBUG.PATHWAY) System.out.println("Drawing " + this + " index=" + dc.getIndex() + " phase=" + dash_phase);
        
        g.setColor(borderColor);
        LWComponent last = null;
        Line2D connector = new Line2D.Float();
        BasicStroke connectorStroke =
            new BasicStroke(6, BasicStroke.CAP_BUTT
                            , BasicStroke.JOIN_BEVEL
                            , 0f
                            , new float[] { dash_length, dash_length }
                            , dash_phase);

        final int BaseStroke = 3;
        
        while (i.hasNext()) {
            LWComponent c = (LWComponent)i.next();

            int strokeWidth;
            boolean selected = (((LWComponent)elementList.get(this.getCurrentIndex())) == c);
            strokeWidth = BaseStroke;

            // because we're drawing under the object, only half of
            // the amount we add to to the stroke width is visible
            // outside the edge of the object, except for links,
            // which are one-dimensional, so we use a narrower
            // stroke width for them.
            if (c instanceof LWLink)
                ;//strokeWidth++;
            else
                strokeWidth *= 2;

            if (selected)
                g.setComposite(PathSelectedTranslucence);
            else
                g.setComposite(PathTranslucence);
        
            strokeWidth += c.getStrokeWidth();
            if (selected) {
                g.setStroke(new BasicStroke(strokeWidth*2));
            } else {
                g.setStroke(new BasicStroke(strokeWidth
                                            , BasicStroke.CAP_BUTT
                                            , BasicStroke.JOIN_BEVEL
                                            , 0f
                                            , new float[] { dash_length, dash_length }
                                            , dash_phase));
            }
            g.draw(c.getShape());

            // If there was an element in the path before this one,
            // draw a connector line from that last component to this
            // one.
            if (last != null) {
                g.setComposite(PathTranslucence);
                connector.setLine(last.getCenterPoint(), c.getCenterPoint());
                g.setStroke(connectorStroke);
                g.draw(connector);
            }
            last = c;
        }
    }

    public void OLD_drawPathway(Graphics2D g){
        Iterator iter = this.getElementIterator();
        Color oldColor = g. getColor();
        BasicStroke oldStroke = (BasicStroke)g.getStroke();
        float width = oldStroke.getLineWidth();
        BasicStroke currentStroke = new BasicStroke(width*4);
        while(iter.hasNext()){
            LWComponent comp = (LWComponent)iter.next();
            g.setColor(borderColor);
            g.setStroke(oldStroke);
            if((LWComponent)elementList.get(this.getCurrentIndex()) == comp)
                g.setStroke(currentStroke);
            if(comp instanceof LWNode)
                g.draw(comp.getShape());      
            else if(comp instanceof LWLink){
                LWLink link = (LWLink)comp;
                LWComponent ep1 = link.getComponent1();
                LWComponent ep2 = link.getComponent2();
                if (!(ep1 instanceof LWLink && ep2 instanceof LWLink)
                && !(ep1.getShape() == null && ep2.getShape() == null)) {
                Area clipArea = new Area(g.getClipBounds());
                if (!(ep1 instanceof LWLink) && ep1.getShape() != null)
                    clipArea.subtract(new Area(ep1.getShape()));
                if (!(ep2 instanceof LWLink) && ep2.getShape() != null)
                    clipArea.subtract(new Area(ep2.getShape()));
                g.clip(clipArea);
            }
                g.draw(link.getShape());
            }
        }
        g.setColor(oldColor);
    }
    
    public boolean contains(LWComponent element){
        return elementList.contains(element);
    }
    
    public int length() {
        return elementList.size();
    }
    
    public LWComponent getFirst() {        
        LWComponent firstElement = null;
        
        try{
            firstElement = (LWComponent)elementList.getFirst();
            currentIndex = 0;
        }catch(NoSuchElementException ne){
            firstElement = null;
        }        
        
        return firstElement;
    }
    
    public boolean isFirst(){
        return (currentIndex == 0);
    }
    
    public LWComponent getLast() {        
        LWComponent lastElement = null;  
        
        try{
            lastElement = (LWComponent)elementList.getLast();
            currentIndex = length() - 1;
        }catch(NoSuchElementException ne){
            lastElement = null;
        }
        return lastElement;
    }
    
    public boolean isLast(){
        return (currentIndex == (length() - 1));
    }
      
    public LWComponent getPrevious(){
        if (currentIndex > 0)
            return (LWComponent)elementList.get(--currentIndex);        
        else
            return null;
    }
    
    public LWComponent getNext(){
        if (currentIndex < (length() - 1))
            return (LWComponent)elementList.get(++currentIndex);        
        else 
            return null;
    }
    
    public LWComponent getElement(int index){
        LWComponent element = null;
        
        try{
            element = (LWComponent)elementList.get(index);
        }catch (IndexOutOfBoundsException ie){
            element = null;
        }    
        
        return element;
    }
    
    public java.util.Iterator getElementIterator() {
        return elementList.iterator();
    }
    
    public void removeElement(int index) {
        removeElement(index, false);
    }

    protected void removeElement(int index, boolean deleted) {
        
        //if the current node needs to be deleted and it isn't the first node, 
        //set the current index to the one before, else keep the same index
        if (index == currentIndex && !isFirst())
            //if (!isFirst())
          currentIndex--;
        
        //if the node to be deleted is before the current node, set the current index to the one before
        else if (index < currentIndex)
          currentIndex--;
        
        LWComponent element = (LWComponent)elementList.remove(index);
        
        System.out.println(this + " removing index " + index + " c="+element);

        /**Daisuke's code */
        for(Iterator i = elementPropertyList.iterator(); i.hasNext();)
        {
            if(((LWPathwayElementProperty)i.next()).getElementID().equals(element.getID()))
            {
                i.remove();
                break;
            }
        }
        
        /**end */
        
        if (element == null) {
            System.err.println(this + " removeElement: element does not exist in pathway");
        } else {
            if (!deleted) {
                element.removePathwayRef(this);
                element.removeLWCListener(this);
            }
        }
    }
       
    public void removeElement(LWComponent element) {
      
       System.out.println("the element version of the remove is being called");
       for(int i = 0; i < elementList.size(); i++){
            LWComponent comp = (LWComponent)elementList.get(i);
            if(comp.equals(element)){
                this.removeElement(i);
                element.removePathwayRef(this);
                //break;
            }
       }
    }

    /**
     * Make sure we've completely cleaned up the pathway when it's
     * been deleted (must get rid of LWComponent references to this
     * pathway)
     */
    public void removeFromModel()
    {
        Iterator i = elementList.iterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            c.removePathwayRef(this);
       }
    }

    public void moveElement(int oldIndex, int newIndex) {
        throw new UnsupportedOperationException("LWPathway.moveElement");
        // will need to clean up add/remove element code at bottom
        // and track addPathRefs before can put this back in
        /*
        LWComponent element = getElement(oldIndex);
        removeElement(oldIndex);
        addElement(element, newIndex);
        */
    }
    
    /**accessor methods used also by xml marshalling process*/
    public Color getBorderColor(){
        return borderColor;
    }
    
    public void setBorderColor(Color color){
        this.borderColor = color;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public boolean getOrdered() {
        return ordered;
    }
    
    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }
    
    public java.util.List getElementList() {
        //System.out.println(this + " getElementList type  ="+elementList.getClass().getName()+"  size="+elementList.size());
        return elementList;
    }
    
    public void setElementList(java.util.List elementList) {
        this.elementList = (LinkedList)elementList;
        if (elementList.size() >= 1) currentIndex = 0;
        System.out.println(this + " setElementList type  ="+elementList.getClass().getName()+"  size="+elementList.size());
    }

    public java.util.List getElementPropertyList()
    {
        return elementPropertyList;
    }
    
    public void setElementPropertyList(java.util.List elementPropertyList)
    {
        this.elementPropertyList = (ArrayList)elementPropertyList;
    }
    
    private List idList = new ArrayList();
    /** for XML save/restore only */
    public List getElementIDList() {
        if (mDoingXMLRestore) {
            return idList;
        } else {
            idList.clear();
            Iterator i = getElementIterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                idList.add(c.getID());
            }
        }
        System.out.println(this + " getElementIDList: " + idList);
        return idList;
    }


    /*
    public void setElementIDList(List idList) {
        System.out.println(this + " setElementIDList: " + idList);
        this.idList = idList;
    }
    */
    
    void completeXMLRestore(LWMap map)
    {
        System.out.println(this + " completeXMLRestore, map=" + map);
        this.map = map;
        Iterator i = this.idList.iterator();
        while (i.hasNext()) {
            String id = (String) i.next();
            LWComponent c = this.map.findChildByID(id);
            System.out.println("\tpath adding " + c);
            addElement(c);
        }
        mDoingXMLRestore = false;
    }
    
    /** Interface for the linked list used by the Castor mapping file*/
    /**
    public ArrayList getElementArrayList()
    {
        System.out.println("calling get elementarraylist for " + getLabel());
         return new ArrayList(elementList);
    }
    
    public void setElementArrayList(ArrayList list)
    {
        System.out.println("calling set elementarraylist for " + getLabel());
        elementList = new LinkedList(list);
    }
    **/
    public void setElementArrayList(LWComponent component)
    {
        System.out.println("calling set elementarraylist for " + getLabel());
        elementList.add(component);
    }
    
    /** end of Castor Interface */
    
    public LWComponent getCurrent() { 
        LWComponent element = null;        
        try{
            element = (LWComponent)elementList.get(currentIndex);
        }catch (IndexOutOfBoundsException ie){
            element = null;
        }      
        return element;
    }
    
    public String getComment(){
        return comment;
    }
    
    public void setComment(String comment){
        this.comment = comment;
    }
    
    public void setCurrentIndex(int i){
        System.out.println("Current pathway node is now " + i);
        currentIndex = i;
        VUE.getActiveMap().notify(this, LWCEvent.Repaint);
    }
    
    public int getCurrentIndex(){
        return currentIndex;
    }
    
    public int getElementIndex(LWComponent comp){
        Iterator iter = this.elementList.iterator();
        int index = 0;
        while(iter.hasNext()){
            LWComponent c = (LWComponent)iter.next();
            if(c.equals(comp)){
                return index;
            }
            index++;
        }
        return -1;
    }
    
    public String getLabel() {
        return this.label;
    }
    
    public String getNotes() {
        return this.notes;
    }
      
    public String getElementNotes(LWComponent component)
    {
        String notes = "Not available";
        
        if (component == null)
        {
            System.err.println("argument to getElementNotes is null");
            return notes;
        }
        
        for(Iterator i = elementPropertyList.iterator(); i.hasNext();)
        {
            LWPathwayElementProperty element = (LWPathwayElementProperty)i.next();
            
            if (element.getElementID().equals(component.getID()))
            {
                notes = element.getElementNotes();
                break;
            }
        }    
        
        return notes;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setElementNotes(LWComponent component, String notes)
    {   
        if (notes == null || component == null)
        {
            System.err.println("argument(s) to setElementNotes is null");
            return;
        }
        
        for(Iterator i = elementPropertyList.iterator(); i.hasNext();)
        {
            LWPathwayElementProperty element = (LWPathwayElementProperty)i.next();
            
            if (element.getElementID().equals(component.getID()))
            {
                element.setElementNotes(notes);
                //can elements be in a pathway twice?
                break;
            }
        }
    }

    public String toString()
    {
        return "LWPathway[" + label
            + " n="
            + (elementList==null?-1:elementList.size())
            + " idx="+currentIndex
            + " map=" + (map==null?"null":map.getLabel())
            + "]";
    }

    
/*****************************/    
/**methods below are not used (actually: moveElement uses them -- SMF) */    
/*****************************/
    
    /** adds an element at the specified location within the pathway*/
    /*
    public void addElement(LWComponent element, int index){
        if(elementList.size() >= index){
            elementList.add(index, element);
            //if(current == null) setCurrent(element);
            if (currentIndex == -1) currentIndex = index;
        }else{
            System.out.println("LWPathway.addElement(element, index), index out of bounds");
        }
    }
    */
    /** adds an element in between two other elements, if they are adjacent*/
    /*
    public void addElement(LWComponent element, LWComponent adj1, LWComponent adj2){
        int index1 = elementList.indexOf(adj1);
        int index2 = elementList.indexOf(adj2);
        int dif = index1 - index2;
        if(elementList.size() >= index1 && elementList.size() >= index2){
            if(Math.abs(dif) == 1){
                if(dif == -1)
                    elementList.add(index2, element);
                else
                    elementList.add(index1, element);
            }
        }else{
            System.out.println("LWPathway.addElement(element,adj1,adj2), index out of bounds");
        }
    }
    
    public LWComponent getNext(LWComponent current) {
        int index = elementList.indexOf(current);
        
        if (index >= 0 && index < (length() - 1))
          return (LWComponent)elementList.get(++index);
        
        //if (currentIndex >= 0 && currentIndex < (length() - 1))
          //return (LWComponent)elementList.get(currentIndex + 1);
        else
          return null;
    }
    
    public LWComponent getPrevious(LWComponent current) {
        int index = elementList.indexOf(current);
        
        if (index > 0)
          return (LWComponent)elementList.get(--index);
        
        //if (currentIndex > 0)
          //return (LWComponent)elementList.get(currentIndex - 1);
        else
          return null;
        
    }
    */
}

