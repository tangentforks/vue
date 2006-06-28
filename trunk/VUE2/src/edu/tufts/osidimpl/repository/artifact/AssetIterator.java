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

package edu.tufts.osidimpl.repository.artifact;

import tufts.vue.*;
import java.net.*;
import java.io.*;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;
import org.xml.sax.InputSource;

public class AssetIterator
implements org.osid.repository.AssetIterator
{
    public  Unmarshaller unmarshaller = null;    
    private java.util.Vector vector = new java.util.Vector();
	private int arrayCount = 0;
    public static final String CASTOR_MAPPING = "artifact.xml";
	private org.osid.shared.Id recordStructureId = RecordStructure.getInstance().getId();

    protected AssetIterator(java.util.Vector vector)
    throws org.osid.repository.RepositoryException
    {
        this.vector = vector;
    }

    public boolean hasNextAsset()
    throws org.osid.repository.RepositoryException
    {
		return arrayCount < vector.size();
    }

    public org.osid.repository.Asset nextAsset()
    throws org.osid.repository.RepositoryException
    {
        if (arrayCount >= vector.size())
        {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NO_MORE_ITERATOR_ELEMENTS);
        }
        return (org.osid.repository.Asset)vector.elementAt(arrayCount++);
    }

	protected AssetIterator(String query, org.osid.shared.Id repositoryId)
	throws org.osid.repository.RepositoryException
	{
		ArtifactResult artifactResult = null;
		java.util.Vector hitList = null;
		try 
		{
			artifactResult = loadArtifactResult(query);
		} catch (Exception ex) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
		try {
			hitList = artifactResult.getHitList();
		} catch (Exception ex) {
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.PERMISSION_DENIED);
		}
		try {
			if (hitList == null) {
				// no hits
				return;
			}
			for (int i=0, size = hitList.size(); i < size; i++) {
				Hit hit = (Hit)hitList.elementAt(i);
				org.osid.repository.Asset asset = new Asset(hit.getTitle(),
															"",
															Utilities.getIdManager().createId(),
															repositoryId);
				org.osid.repository.Record record = asset.createRecord(this.recordStructureId);
				
				record.createPart(ArtifactPartStructure.getInstance().getId(),hit.getArtifact());
				record.createPart(ThumbnailPartStructure.getInstance().getId(),hit.getThumb());
				record.createPart(LargeImagePartStructure.getInstance().getId(),hit.getFullImage());
				record.createPart(ArtistPartStructure.getInstance().getId(),hit.artist);
				record.createPart(CulturePartStructure.getInstance().getId(),hit.culture);
				record.createPart(CurrentLocationPartStructure.getInstance().getId(),hit.currentLocation);
				record.createPart(MaterialPartStructure.getInstance().getId(),hit.material);
				record.createPart(OriginPartStructure.getInstance().getId(),hit.origin);
				record.createPart(PeriodPartStructure.getInstance().getId(),hit.period);
				record.createPart(SubjectPartStructure.getInstance().getId(),hit.subject);
				record.createPart(ViewPartStructure.getInstance().getId(),hit.view);
				record.createPart(URLPartStructure.getInstance().getId(),hit.artifact);
				asset.updateContent(hit.artifact);
				this.vector.addElement(asset);
			}
		}
		catch (Throwable t)
		{
			Utilities.log(t);
			throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
		}
	}
    
	public  ArtifactResult loadArtifactResult(String query)
	{
		try {
			Unmarshaller unmarshaller = getUnmarshaller();
			unmarshaller.setValidation(false);
			URL url = new URL(query);
			InputStream stream = url.openStream();
			ArtifactResult artifactResult = (ArtifactResult) unmarshaller.unmarshal(new InputSource(stream));
			return artifactResult;			
		} catch (Exception ex) {
			Utilities.log(ex);
		}
		return null;
    }
    
	public  Unmarshaller getUnmarshaller()
	{
		try {
			if (unmarshaller == null) {
				unmarshaller = new Unmarshaller();
				Mapping mapping = new Mapping();
				
				// use Provider to find the mapping file
				String url = Utilities.getResourcePath(CASTOR_MAPPING);
				
				mapping.loadMapping(new URL("file://" + url));
				unmarshaller.setMapping(mapping);
			}
			return unmarshaller;
		} catch (Exception ex) {
			Utilities.log(ex);
		}
		return null;
	}
}
