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
package edu.tufts.osidimpl.repository.sakai;

public class RepositoryManager
implements org.osid.repository.RepositoryManager
{
    private org.osid.OsidContext context = null;
    private java.util.Properties configuration = null;

    private org.osid.authentication.AuthenticationManager authenticationManager = null;
	private org.osid.repository.Repository repository = null;
	private org.osid.shared.Type repositoryType = null;
	private org.osid.shared.Id repositoryId = null;
    private java.util.Vector repositoryVector = new java.util.Vector();
    private java.util.Vector searchTypeVector = new java.util.Vector();
	private String displayName = null;

    public void osidVersion_2_0()
		throws org.osid.repository.RepositoryException
    {
    }

    public org.osid.OsidContext getOsidContext()
		throws org.osid.repository.RepositoryException
    {
        return context;
    }
	
    public void assignOsidContext(org.osid.OsidContext context)
    throws org.osid.repository.RepositoryException
    {
        this.context = context;
    }
	
    public void assignConfiguration(java.util.Properties configuration)
    throws org.osid.repository.RepositoryException
    {
		this.configuration = configuration;
		try {
			// setup logging and id manager, once
			if (Utilities.getIdManager() == null) {
				org.osid.logging.LoggingManager loggingManager = (org.osid.logging.LoggingManager)org.osid.OsidLoader.getManager("org.osid.logging.LoggingManager",
																																 "comet.osidimpl.logging.plain",
																																 this.context,
																																 new java.util.Properties());
				
				org.osid.logging.WritableLog log = null;
				try {
					log = loggingManager.getLogForWriting("SakaiRepositoryOSID");
				} catch (org.osid.logging.LoggingException lex) {
					log = loggingManager.createLog("SakaiRepositoryOSID");
				}
				log.assignFormatType(new Type("mit.edu","logging","plain"));
				log.assignPriorityType(new Type("mit.edu","logging","info"));
				Utilities.setLog(log);			
				
				org.osid.id.IdManager idManager = (org.osid.id.IdManager)org.osid.OsidLoader.getManager("org.osid.id.IdManager",
																										"comet.osidimpl.id.no_persist",
																										this.context,
																										new java.util.Properties());
				Utilities.setIdManager(idManager);
			}
			
			/*
			 The OBA for this authentication type states that the following will be present in the properties:
			 
			 sakaiUsername
			 sakaiPassword
			 sakaiHost
			 sakaiPort
			 
			 a session id is placed in the context if all goes well
			 */
			if (configuration.getProperty("sakaiHost") != null) {
				
				org.osid.shared.Type authenticationType = new Type("sakaiproject.org","authentication","sakai");
				this.authenticationManager = (org.osid.authentication.AuthenticationManager)org.osid.OsidLoader.getManager("org.osid.authentication.AuthenticationManager",
																														   "edu.tufts.osidimpl.authentication.sakai",
																														   this.context,
																														   configuration);			
				Utilities.setAuthenticationManager(this.authenticationManager);
				this.authenticationManager.authenticateUser(authenticationType);
				if (!this.authenticationManager.isUserAuthenticated(authenticationType)) {
					throw new org.osid.repository.RepositoryException(org.osid.OsidException.PERMISSION_DENIED);
				}
				
				String key = (String)context.getContext("org.sakaiproject.instanceKey");
				//System.out.println("assigned key is " + key);
				String sessionId = (String)context.getContext("org.sakaiproject.sessionId." + key);
				if (sessionId == null) {
					throw new org.osid.repository.RepositoryException(org.osid.OsidException.CONFIGURATION_ERROR);
				}
				Utilities.setSessionId(sessionId,key);
				
				/*
				 Make one repository
				 */
				this.repositoryType = new Type("sakaiproject.org","repository","contentHosting");
				
				String h = configuration.getProperty("sakaiHost");
				if (h.startsWith("http://")) {
					h = h.substring(7);
				}
				
				String displayName = configuration.getProperty("sakaiDisplayName");
				
				//Utilities.setRepositoryId(h + ".Virtual-Root-Identifier");
				this.repositoryVector.removeAllElements();
				this.repositoryVector.addElement(new Repository(displayName,key));
				
				String address = h + ":" + configuration.getProperty("sakaiPort") + "/";
				Utilities.setEndpoint(address + "sakai-axis/ContentHosting.jws");		
				Utilities.setAddress(address);
			}
		} catch (Throwable t) {
			Utilities.log(t);
			if (t instanceof org.osid.repository.RepositoryException) {
				throw new org.osid.repository.RepositoryException(t.getMessage());
			} else {                
				throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
			}
		}
	}

	/**
		Unimplemented Method
	 */
    public org.osid.repository.Repository createRepository(String displayName
                                                         , String description
                                                         , org.osid.shared.Type repositoryType)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

	/**
		Unimplemented Method
	 */
    public void deleteRepository(org.osid.shared.Id repositoryId)
    throws org.osid.repository.RepositoryException
    {
        if (repositoryId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

	/**
		Return the sole Repository that points to the Sakai instance.
	 */
    public org.osid.repository.RepositoryIterator getRepositories()
    throws org.osid.repository.RepositoryException
    {
		return new RepositoryIterator(this.repositoryVector);
	}

	/**
		Return the sole Repository that points to the Sakai instance.
	 */
    public org.osid.repository.RepositoryIterator getRepositoriesByType(org.osid.shared.Type repositoryType)
    throws org.osid.repository.RepositoryException
    {
        if (repositoryType == null) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		if (!repositoryType.isEqual(this.repositoryType)) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_TYPE);
		}			
		return getRepositories();
    }

	/**
		Return the sole Repository that points to the Sakai instance.
	 */
    public org.osid.repository.Repository getRepository(org.osid.shared.Id repositoryId)
    throws org.osid.repository.RepositoryException
    {
        if (repositoryId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }

		String repositoryIdString = null;
		try {
			repositoryIdString = repositoryId.getIdString();
        } catch (Throwable t) {
            Utilities.log(t.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
		if (repositoryIdString.length() == 0) {
			throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
		}
		
		try {
			org.osid.repository.RepositoryIterator repositoryIterator = getRepositories();
			while (repositoryIterator.hasNextRepository()) {
				org.osid.repository.Repository repository = repositoryIterator.nextRepository();
				if (repository.getId().getIdString().equals(repositoryIdString)) {
					return repository;
				}
			}
        } catch (Throwable t) {
            Utilities.log(t.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
		throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.UNKNOWN_ID);
    }

	/**
		Delgate to Repositories to perform work.
	 */
    public org.osid.repository.Asset getAsset(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
        if (assetId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        try {
			org.osid.repository.RepositoryIterator repositoryIterator = getRepositories();
			while (repositoryIterator.hasNextRepository()) {
			org.osid.repository.Repository nextRepository = repositoryIterator.nextRepository();
			try {
				org.osid.repository.Asset asset = nextRepository.getAsset(assetId);
				return asset;
			} catch (Throwable t) {}
			}
		} catch (Throwable t) {
            Utilities.log(t.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
		}
        throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.UNKNOWN_ID);
    }

	/**
		Unimplemented Method -- No version support
	 */
    public org.osid.repository.Asset getAssetByDate(org.osid.shared.Id assetId
                                                  , long date)
    throws org.osid.repository.RepositoryException
    {
        if (assetId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

	/**
		Unimplemented Method -- No version support
	 */
    public org.osid.shared.LongValueIterator getAssetDates(org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
        if (assetId == null) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
		throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

	/**
		Delgate to Repositories to perform work.
	 */
    public org.osid.repository.AssetIterator getAssetsBySearch(org.osid.repository.Repository[] repositories
                                                             , java.io.Serializable searchCriteria
                                                             , org.osid.shared.Type searchType
                                                             , org.osid.shared.Properties searchProperties)
    throws org.osid.repository.RepositoryException
    {
        if (repositories == null) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        try {
            java.util.Vector results = new java.util.Vector();
            for (int j=0; j < repositories.length; j++) {
                org.osid.repository.Repository nextRepository = repositories[j];
                //optionally add a separate thread here
                try {
                    org.osid.repository.AssetIterator assetIterator =
                        nextRepository.getAssetsBySearch(searchCriteria,searchType,searchProperties);
                    while (assetIterator.hasNextAsset())
                    {
                        results.addElement(assetIterator.nextAsset());
                    }
                } catch (Throwable t) {
                    // log exceptions but don't stop searching
                    Utilities.log(t.getMessage());
                }
            }
            return new AssetIterator(results);
        } catch (Throwable t) {
            Utilities.log(t.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
    }

	/**
		Unimplemented Method
	 */
    public org.osid.shared.Id copyAsset(org.osid.repository.Repository repository
                                      , org.osid.shared.Id assetId)
    throws org.osid.repository.RepositoryException
    {
        if ((repository == null) || (assetId == null)) {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NULL_ARGUMENT);
        }
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

	/**
		Return the one type Sakai defines
	 */
    public org.osid.shared.TypeIterator getRepositoryTypes()
    throws org.osid.repository.RepositoryException
    {
        java.util.Vector results = new java.util.Vector();
        try {
            results.addElement(this.repositoryType);
            return new TypeIterator(results);
        } catch (Throwable t) {
            Utilities.log(t.getMessage());
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
    }
}
