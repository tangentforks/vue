/*
 * WeightAggregate.java
 *
 * Created on October 16, 2006, 4:40 PM
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
 */
package edu.tufts.vue.compare;

import java.util.*;
import tufts.vue.*;

public class WeightAggregate extends ConnectivityMatrix {
    public static final int COUNT_ERROR = -1;
    private List<ConnectivityMatrix> matrices = new ArrayList();
    private int mergeNodeCount[] = new int[SIZE];
    private int count;
    public WeightAggregate(List<ConnectivityMatrix> matrices ){
        this.matrices  = matrices;
        count= matrices.size();
        mergeMatrices();
    }
    
    /*
     * merges connectivity matrices in the list 'matrices'
     *
     */
    private void mergeMatrices() {
        Iterator<ConnectivityMatrix> i = matrices.iterator();
        while(i.hasNext()){
            ConnectivityMatrix matrix = i.next();
            updateLabels(matrix.getLabels());
            int matrixSize = matrix.getSize();
            if(matrixSize > size) {
                size = matrixSize;
            }
            for(int index1 = 0;index1 < getLabels().size();index1++) {
                for(int index2= index1+1; index2<getLabels().size();index2++) {
                    c[index1][index2] += matrix.getConnection((String)getLabels().get(index1),(String)getLabels().get(index2));
                }
            }
        }
    }
    
    public ConnectivityMatrix getAggregate(){
        return this;
    }
    // need to implement this one.
    public LWMap getMap() {
        return null;
    }
    
    private void updateLabels(List<String> mLabels) {
        Iterator<String> i = mLabels.iterator();
        while(i.hasNext()) {
            String mLabel = i.next();
            if(!labels.contains(mLabel)){
                labels.add(mLabel);
                mergeNodeCount[labels.indexOf(mLabel)] = 1;
            }else{
                mergeNodeCount[labels.indexOf(mLabel)]++;
            }
        }
    }
    
    /**
     * gets the number of nodes with the label in the aggregate
     * @param label label of the node
     * @return node count
    */        
    public int getNodeCount(String label) {
        if(labels.indexOf(label)>=0)
            return mergeNodeCount[labels.indexOf(label)];
        else return COUNT_ERROR;
    }
    public int getCount() {
        return count;
    }
}