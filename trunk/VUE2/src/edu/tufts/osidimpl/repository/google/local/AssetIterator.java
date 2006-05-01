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

package edu.tufts.osidimpl.repository.google.local;

import com.google.soap.search.*;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;
import java.io.*;
import java.net.*;
import tufts.vue.*;
import org.xml.sax.InputSource;

public class AssetIterator
implements org.osid.repository.AssetIterator
{
    private java.util.Iterator iterator = new java.util.Vector().iterator();
	private int currentIndex = 0;
	private String searchURL = null;
	private String criteria = null;
	private static Unmarshaller unmarshaller = null;
	private static URL XML_MAPPING = VueResources.getURL("mapping.google");
	private static URL url;
	private boolean initializedByVector = false;
	private static String result = "";

    protected AssetIterator(String searchURL,
							String criteria)
    throws org.osid.repository.RepositoryException
    {
		initializedByVector = false;
		this.searchURL = searchURL;
		this.criteria = criteria;
		search();    
	}

    protected AssetIterator(java.util.Vector v)
		throws org.osid.repository.RepositoryException
    {
		initializedByVector = true;
		this.iterator = v.iterator();
	}
	
    public boolean hasNextAsset()
    throws org.osid.repository.RepositoryException
    {
		if (this.iterator.hasNext()) {
			return true;
		} else if (!initializedByVector) {
			search();
			return (this.iterator.hasNext());
		} else {
			return false;
		}
    }

    public org.osid.repository.Asset nextAsset()
    throws org.osid.repository.RepositoryException
    {
		try {
			if (this.iterator.hasNext()) {
				return (org.osid.repository.Asset)iterator.next();
			}
		} catch (Throwable t) {
		}
		throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NO_MORE_ITERATOR_ELEMENTS);
    }
	
	private void search()
	{
//		System.out.println("start search thread");
//		Thread t = new Thread() {
//			public void run() {
				performSearch(currentIndex);
//			}
//		};
	}
	
	private void performSearch(int searchStartIndex)
	{
		try {
			url = new URL(this.searchURL+"&num=10&start="+searchStartIndex+"&q="+this.criteria);
			System.out.println("Google search = " + url);
			InputStream input = url.openStream();
			int c;
			while((c=input.read())!= -1) {
				result = result + (char) c;
			}
			String googleResultsFile = VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar+VueResources.getString("save.google.results");
			FileWriter fileWriter = new FileWriter(googleResultsFile);
			fileWriter.write(result);
			fileWriter.close();
			result = "";
			
			GSP gsp = loadGSP(googleResultsFile);
			java.util.Iterator i = gsp.getRES().getResultList().iterator();
			java.util.Vector resultVector = new java.util.Vector();
			
			while(i.hasNext()) {
				Result r = (Result)i.next();
				URLResource urlResource = new URLResource(r.getUrl());
				if (r.getTitle() != null) urlResource.setTitle(r.getTitle().replaceAll("</*[a-zA-Z]>",""));
				 else urlResource.setTitle(r.getUrl().toString());
				 resultVector.add(new Asset(r.getTitle(),"",r.getUrl()));
				 System.out.println(r.getTitle()+" "+r.getUrl());
				 
			}
				 this.currentIndex += resultVector.size();
		} catch (Throwable t) {
			Utilities.log("cannot connect google");
		}
	}
				 
	// Functions to support marshalling and unmarshalling of the reults generated through search using castor.
	private static GSP loadGSP(String filename) 
	{
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
				 
	private static Unmarshaller getUnmarshaller() 
	{
		if (unmarshaller == null) {
			unmarshaller = new Unmarshaller();
			Mapping mapping = new Mapping();
			System.out.println("XML_MAPPING =" +XML_MAPPING);
			try {
				mapping.loadMapping(XML_MAPPING);
				unmarshaller.setMapping(mapping);
			} catch (Exception e) {
				System.out.println("TuftsGoogle.getUnmarshaller: " + XML_MAPPING+e);
			}
		}
		return unmarshaller;
	}
}
