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
/*
 * SaveNode.java
 *
 * Created on October 14, 2003, 1:01 PM
 */

/**
 *
 * @author  rsaigal
 */

import java.util.Vector;
import javax.swing.tree.*;
import javax.swing.*;
import java.io.*;
import java.util.Enumeration;

public class SaveNode{
   
    private Resource resource;
    private Vector children;
    
    public SaveNode(){
        
    }
    
    public SaveNode(ResourceNode resourceNode){
      
            
        
        Enumeration e = resourceNode.children();
        this.setResource(resourceNode.getResource());
        System.out.println("Resource Node" + resourceNode.getResource());
        Vector v = new Vector();
       
        while (e.hasMoreElements())
        {    
              
               ResourceNode newResNode =(ResourceNode)e.nextElement();
            //  if (newResNode.getResource() instanceof CabinetResource)
            //{ 
             //  SaveNode child = new SaveNode(new ResourceNode));
              // v.add(child);
            //}
           
          
            //else{
            
            SaveNode child = new SaveNode(newResNode);
            v.add(child);
          
            //}
           
        }
          
        this.setChildren(v);
     //  System.out.println("I am resource" + this.getResource()+this.getResource().getType()); 
        
    }
    
    
    public void setResource(Resource resource){
        
        this.resource = resource;
    }
    
    public Resource getResource(){
        return (this.resource);
        
    }
    
    
    public void setChildren(Vector children){
        
        this.children= children;
        
        
    }
    
    public Vector getChildren(){
         return (this.children);
    }
    
}  
