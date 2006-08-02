package edu.tufts.osidimpl.repository.fedora_2_0;

public class Utilities
{
    private static org.osid.logging.WritableLog _log = null;
	private static org.osid.id.IdManager _idManager = null;
	private static final String LOG_FILENAME = "Fedora_2_0";
	private static final String ID_IMPLEMENTATION = "comet.osidimpl.id.no_persist";
	private static final String LOGGING_IMPLEMENTATION = "comet.osidimpl.logging.plain";
	private static final String LOGGING_TYPE_AUTHORITY = "mit.edu";
	private static final String LOGGING_TYPE_DOMAIN = "logging";
	private static final String LOGGING_TYPE_FORMAT = "plain";
	private static final String LOGGING_TYPE_PRIORITY = "info";
	
	public static String formatObjectUrl(String objectId,String methodId,Repository repository) throws org.osid.repository.RepositoryException {
        String  url = "";
        try {
            url = repository.getFedoraProperties().getProperty("url.fedora.get")+objectId+"/"+methodId;
        }catch (Throwable t) {
            t.printStackTrace();
        }
        return url;
    }

	private static void setupLogging()
	{
		if (_log == null) {
			try {
				org.osid.logging.LoggingManager loggingManager = (org.osid.logging.LoggingManager)org.osid.OsidLoader.getManager("org.osid.logging.LoggingManager",
																																 LOGGING_IMPLEMENTATION,
																																 new org.osid.OsidContext(),
																																 new java.util.Properties());
				try {
					_log = loggingManager.getLogForWriting(LOG_FILENAME);
				} catch (org.osid.logging.LoggingException lex) {
					_log = loggingManager.createLog(LOG_FILENAME);
				}
				_log.assignFormatType(new Type(LOGGING_TYPE_AUTHORITY,LOGGING_TYPE_DOMAIN,LOGGING_TYPE_FORMAT));
				_log.assignPriorityType(new Type(LOGGING_TYPE_AUTHORITY,LOGGING_TYPE_DOMAIN,LOGGING_TYPE_PRIORITY));
			} catch (Throwable t) {
			}
		}
	}
	
	private static void setupId()
	{
		if (_idManager == null) {
			try {
				_idManager = (org.osid.id.IdManager)org.osid.OsidLoader.getManager("org.osid.id.IdManager",
																				   ID_IMPLEMENTATION,
																				   new org.osid.OsidContext(),
																				   new java.util.Properties());
			} catch (Throwable t) {
			}
		}
	}
	
	public static void log(String entry)
	{
		setupLogging();
		try {
			_log.appendLog(entry);
		} catch (org.osid.logging.LoggingException lex) {
			// swallow exception since logging is a best attempt to log an exception anyway
		}   
	}
	
	public static void log(Throwable t)
	{
		setupLogging();
		try {
			t.printStackTrace();
			_log.appendLog(t.getMessage());
		} catch (org.osid.logging.LoggingException lex) {
			// swallow exception since logging is a best attempt to log an exception anyway
		}   
	}	
	
	public static org.osid.id.IdManager getIdManager()
	{
		setupId();
		return _idManager;
	}
	
	public static org.osid.shared.Type stringToType(String typeString) {
		String authority = "_";
		String domain = "_";
		String keyword = "_";
		try {
			if (typeString != null) {
				int indexSlash = typeString.indexOf("/");
				if (indexSlash != -1) {
					domain = typeString.substring(0,indexSlash);
					int indexAt = typeString.indexOf("@");
					if (indexAt != -1) {
						keyword = typeString.substring(indexSlash+1,indexAt);
						authority = typeString.substring(indexAt+1);
					}
				}
			}
		} catch (Throwable t) {
			// ignore formatting error
		}
		return new Type(authority,domain,keyword);
	}
}