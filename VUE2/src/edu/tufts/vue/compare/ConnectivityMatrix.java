/*
 * ConnectivityMatrix.java
 *
 * Created on September 13, 2006, 11:13 AM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2006
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 * The class creates a connectivity Matrix using a VUe Map.  Further information
 * on connectivity matrix can be found at
 * http://w3.antd.nist.gov/wctg/netanal/netanal_netmodels.html
 * The matrix can be used to assess the connetiving among give set of nodes.
 * A value of connetion is 1 if there is a connection between nodes
 * connection(a,b) = 1 implies there is a link from a to b
 * connection(b,a) = 1 implies there is a link from b to a
 * connection(b,a) may not be equal to connection(a,b)
 * connection(a,b) = connection(b,a) implies the link between a and b is not
 * directed.
 */

package edu.tufts.vue.compare;

import java.util.*;
import java.io.*;
import tufts.vue.*;


public class ConnectivityMatrix {
    public static final int SIZE = 1000;
    protected List labels = new  ArrayList(); // these labels need not be node labels
    protected int c[][] = new int[SIZE][SIZE];
    protected int size;
    private LWMap map; 
    /** Creates a new instance of ConnectivityMatrix */
    public ConnectivityMatrix() {
    }
    
    public ConnectivityMatrix(LWMap map) {
        size = 0;
        this.map = map;
        addLabels();
        generateMatrix();
    }
    
    private void addLabels(){
        Iterator i = map.getNodeIterator();
        while(i.hasNext()){
            LWNode node = (LWNode)i.next();
            labels.add(getMergeProperty(node));
            size++;
        }
    }
    private void generateMatrix() {
        Iterator links = map.getLinkIterator();
        while(links.hasNext()) {        
            LWLink link = (LWLink)links.next();
            //LWComponent n1 = link.getComponent1(); // deprecated
            //LWComponent n2 = link.getComponent2(); // deprecated
            LWComponent n1 = link.getHead();
            LWComponent n2 = link.getTail();
            int arrowState = link.getArrowState();
            if(n1  instanceof LWNode && n2 instanceof LWNode) {
               try
               {
                 if(arrowState == LWLink.ARROW_BOTH || arrowState == LWLink.ARROW_NONE) {
                      c[labels.indexOf(getMergeProperty(n2))][labels.indexOf(getMergeProperty(n1))] = 1;
                      c[labels.indexOf(getMergeProperty(n1))][labels.indexOf(getMergeProperty(n2))] =1;
                 } else if(arrowState == LWLink.ARROW_HEAD) { // EP1 and EP2 were deprecated.
                      c[labels.indexOf(getMergeProperty(n2))][labels.indexOf(getMergeProperty(n1))] = 1;
                 } else    if(arrowState == LWLink.ARROW_TAIL) { // EP1 and EP2 were deprecated.
                      c[labels.indexOf(getMergeProperty(n1))][labels.indexOf(getMergeProperty(n2))] =1;
                 }
               }
               catch(ArrayIndexOutOfBoundsException ae)
               {
                   System.out.println("Connectivity Matrix Exception - skipping link: " + link);
                   System.out.println("Exception was: " + ae);
               }
            }

        }
    }
    public List getLabels() {
        return labels;
    }
    
    public void setLabels(List labels){
        this.labels = labels;
    }
    
    public int getConnection(int i, int j) {
        return c[i][j];
    }
    
    public int getConnection(String label1,String label2) {
        int index1 = labels.indexOf(label1);
        int index2 = labels.indexOf(label2);
        if(index1 >= 0 && index2 >=0 ){
            return c[index1][index2];
        } else {
            return 0;
        }
    }
    
    public void setConnection(String label1, String label2, int value) {
        int index1 = labels.indexOf(label1);
        int index2 = labels.indexOf(label2);
        if(index1 >= 0 && index2 >=0 ){
            c[index1][index2] = value;
        }
    }
    public int getSize(){
        return size;
    }
    public int[][] getMatrix() {
        return c;
    }
    public void setMatrix(int[][] c) {
        this.c = c;
    }
    
    public LWMap getMap() {
        return this.map;
    }
    public void store(OutputStream out) {
        try {
            out.write(this.toString().getBytes());
        }catch(IOException ex) {
            System.out.println("ConnectivityMatrix.store:"+ex);
        }
    }
    
    
    public boolean compare(ConnectivityMatrix c2) {
        if(c2.getSize() != size) {
            return false;
        }
        for(int i=0;i<size;i++) {
            for(int j=0;j<size;j++) {
                if(c[i][j] != c2.getMatrix()[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public String toString() {
        String output = new String();
 // removed the first label from the output. to add it uncomment the commented lines in this function
 //       output = "\t";   //leave the first cell empty;
        Iterator iterator = labels.iterator();
        while(iterator.hasNext()){
            output += (String)iterator.next()+"\t";
        }
        output +="\n";
        for(int i=0;i<size;i++){
//            output += labels.get(i)+"\t";
            for(int j=0;j<size;j++) {
                output  += c[i][j]+"\t";
            }
            output +="\n";
        }
        return output;
    }
    
    private String getMergeProperty(LWComponent node) {
        return  Util.getMergeProperty(node);
    }
}
