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
 * SearchPanel.java
 *
 * Created on May 2, 2003, 9:03 PM
 */




import tufts.google.*;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.Vector;
import java.util.Iterator;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;

import org.xml.sax.InputSource;
/**
 *
 * @author  rsaigal
 */
public class SearchPanel extends JPanel{
  
    private static  String searchURL;
    private static java.util.prefs.Preferences prefs;
    private static String  XML_MAPPING;
    private static String query;
    
    private static int NResults = 10;
    
    private static String result ="";
    
    private static URL url;
    
    public SearchPanel() {

        searchURL = VueResources.getString("url.google");
        XML_MAPPING = VueResources.getString("mapping.google");
 
        this.setLayout(new BorderLayout());
       //Create the query panel and result panel//      
        JPanel queryPanel =  new JPanel();        
        final JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new BorderLayout());
        queryPanel.setLayout(new BorderLayout()); 
        final JTextField queryBox = new JTextField();
        queryPanel.add(queryBox,BorderLayout.NORTH);
        JButton searchButton = new JButton("Search");     
        searchButton.setPreferredSize(new Dimension(100,30));      
        queryPanel.add(searchButton,BorderLayout.SOUTH);
        this.add(queryPanel,BorderLayout.NORTH);
        this.add(resultPanel,BorderLayout.CENTER);

        //Add action to the submint button
   
        
        searchButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                
              int index = 0;  
              
                JScrollPane jsp = new JScrollPane();
                
                String searchString = queryBox.getText();
                        
                
               if (!searchString.equals("")){
                
                try {
           url = new URL(searchURL+searchString);
           InputStream input = url.openStream();
           int c;
           while((c=input.read())!= -1) {
               result = result + (char) c;
           }
           FileWriter fileWriter = new FileWriter("google_result.xml");
           fileWriter.write(result);
           fileWriter.close();
           result = "";
         
          GSP gsp = loadGSP("google_result.xml");
           Iterator i = gsp.getRES().getResultList().iterator();
            Vector resultVector = new Vector();
            
           while(i.hasNext()) {
               Result r = (Result)i.next();
               resultVector.add(r.getUrl());
               System.out.println(r.getTitle()+" "+r.getUrl());
           } 
          
                VueDragTree tree = new VueDragTree(resultVector.iterator(),"GoogleSearchResults");
                tree.setEditable(true);
                tree.setRootVisible(false);
              
                JTextField ja = new JTextField("Google Search Results");
                jsp = new JScrollPane(tree);
         
               
                resultPanel.add(ja,BorderLayout.NORTH);
                resultPanel.add(jsp,BorderLayout.CENTER,index);
                       index = index++;
                    
                resultPanel.revalidate();  
        } catch (Exception ex) {}
 
                }
                
            
              
            }
            
            });
                
        
    }
    
   private static GSP loadGSP(String filename) {
  
        try {
           Unmarshaller unmarshaller = getUnmarshaller();
           unmarshaller.setValidation(false);
           GSP gsp = (GSP) unmarshaller.unmarshal(new InputSource(new FileReader(filename)));
            return gsp;
        } catch (Exception e) {
            System.out.println("loadGSP[" + filename + "]: " + e);
            e.printStackTrace();
            return null;
        }
    }
    
   private static GSP loadGSP(URL url)
    {
       try {
         InputStream input = url.openStream();
         int c;
         while((c=input.read())!= -1) {
               result = result + (char) c;
         }
       
           Unmarshaller unmarshaller = getUnmarshaller();
           unmarshaller.setValidation(false);
           GSP gsp = (GSP) unmarshaller.unmarshal(new InputSource());
            return gsp;
        } catch (Exception e) {
            System.out.println("loadGSP " + e);
            e.printStackTrace();
            return null;
        }
    }
    

    private static Unmarshaller unmarshaller = null;
    private static Unmarshaller getUnmarshaller()
    {
        if (unmarshaller == null) {
            unmarshaller = new Unmarshaller();
            Mapping mapping = new Mapping();
            try {
                mapping.loadMapping(XML_MAPPING);
                unmarshaller.setMapping(mapping);
            } catch (Exception e) {
                System.out.println("getUnmarshaller: " + XML_MAPPING+e);
           }
        }
        return unmarshaller;
    }
}
