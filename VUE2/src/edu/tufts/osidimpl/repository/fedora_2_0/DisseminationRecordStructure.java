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

package  edu.tufts.osidimpl.repository.fedora_2_0;

public class DisseminationRecordStructure
implements org.osid.repository.RecordStructure
{
    private java.util.Vector partsVector = new java.util.Vector();
    private String displayName = "Dissemination";
    private String description = "Provides information to get the Dissemination";
    private org.osid.shared.Id id = null;
    private String schema = null;
    private String format = "Plain Text";
    private org.osid.shared.Type type = new Type("tufts.edu","recordStructure","dissemination");
    private org.osid.repository.PartStructure BDEFPartStructure = null;
    private org.osid.repository.PartStructure disseminationURLPartStructure = null;
    private org.osid.repository.PartStructure parameterPartStructure = null;

    protected DisseminationRecordStructure(Repository repository)
    throws org.osid.repository.RepositoryException
    {
        try
        {
            this.id = new PID(FedoraUtils.getFedoraProperty(repository, "DisseminationInfoStructureId"));
        }
        catch (org.osid.shared.SharedException sex)
        {
        }
        this.BDEFPartStructure = new BDEFPartStructure(this,repository);
        this.disseminationURLPartStructure = new DisseminationURLPartStructure(this,repository);
        this.parameterPartStructure = new ParameterPartStructure(this,repository);
        this.partsVector.add(this.BDEFPartStructure);        
        this.partsVector.add(this.disseminationURLPartStructure);        
        this.partsVector.add(this.parameterPartStructure);        
    }

    public String getDisplayName()
    throws org.osid.repository.RepositoryException
    {
        return this.displayName;
    }

    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public String getDescription()
    throws org.osid.repository.RepositoryException
    {
        return this.description;
    }

    public String getFormat()
    throws org.osid.repository.RepositoryException
    {
        return this.format;
    }

    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException
    {
        return this.id;
    }

    public org.osid.repository.PartStructureIterator getPartStructures()
    throws org.osid.repository.RepositoryException
    {
        return new PartStructureIterator(this.partsVector);
    }

    public String getSchema()
    throws org.osid.repository.RepositoryException
    {
        return this.schema;
    }

    public org.osid.shared.Type getType()
    throws org.osid.repository.RepositoryException
    {
        return this.type;
    }

    public boolean isRepeatable()
    throws org.osid.repository.RepositoryException
    {
        return false;
    }

    public boolean validateRecord(org.osid.repository.Record record)
    throws org.osid.repository.RepositoryException
    {
        return true;
    }

    public org.osid.repository.PartStructure getBDEFPartStructure()
    throws org.osid.repository.RepositoryException
    {
        if (this.BDEFPartStructure == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.BDEFPartStructure;
    }

    public org.osid.repository.PartStructure getDisseminationURLPartStructure()
    throws org.osid.repository.RepositoryException
    {
        if (this.disseminationURLPartStructure == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.disseminationURLPartStructure;
    }

    public org.osid.repository.PartStructure getParameterPartStructure()
    throws org.osid.repository.RepositoryException
    {
        if (this.parameterPartStructure == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.parameterPartStructure;
    }

}
