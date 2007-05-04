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

public class SakaiExport
{
//	private org.osid.shared.Type _collectionAssetType = new edu.tufts.vue.util.Type("sakaiproject.org","asset","siteCollection");
	private org.osid.shared.Type _collectionAssetType = new edu.tufts.vue.util.Type("com.harvestroad","asset","category");
	private org.osid.shared.Type _sakaiRepositoryType = new edu.tufts.vue.util.Type("sakaiproject.org","repository","contentHosting");
	private edu.tufts.vue.dsm.DataSourceManager _dsm = null;

	public SakaiExport(edu.tufts.vue.dsm.DataSourceManager dsm)
	{
		_dsm = dsm;
	}
	
	public edu.tufts.vue.dsm.DataSource[] getSakaiDataSources()
		throws org.osid.repository.RepositoryException
	{
		java.util.Vector dataSourceVector = new java.util.Vector();
		edu.tufts.vue.dsm.DataSource result[] = new edu.tufts.vue.dsm.DataSource[0];
		
		edu.tufts.vue.dsm.DataSource dataSources[] = _dsm.getDataSources();
		for (int i=0; i < dataSources.length; i++) {
			System.out.println("Examining Repository " + dataSources[i].getRepository().getDisplayName());
			if (dataSources[i].supportsUpdate()) {
				System.out.println("Supports Update, Now Checking Type");
				org.osid.repository.Repository repository = dataSources[i].getRepository();
				if (repository.getType().isEqual(_sakaiRepositoryType)) {
					dataSourceVector.addElement(dataSources[i]);
				}
			}
		}
		// convert to array for return
		int size = dataSourceVector.size();
		result = new edu.tufts.vue.dsm.DataSource[size];
		for (int i=0; i < size; i++) {
			result[i] = (edu.tufts.vue.dsm.DataSource)dataSourceVector.elementAt(i);
		}
		return result;
	}

	// if array is empty, configuration was incomplete, server was not responding, permission was denied
	public SakaiCollection[] getCollections(edu.tufts.vue.dsm.DataSource dataSource)
		throws org.osid.repository.RepositoryException
	{
		java.util.Vector collectionVector = new java.util.Vector();
		SakaiCollection collection[] = new SakaiCollection[0];
		
		// get all the collections
		if (dataSource.supportsUpdate()) {
			org.osid.repository.Repository repository = dataSource.getRepository();
			org.osid.repository.AssetIterator assetIterator = repository.getAssetsByType(_collectionAssetType);
			while (assetIterator.hasNextAsset()) {
				collectionVector.addElement(new SakaiCollection(assetIterator.nextAsset()));
			}
			// convert to array for return
			int size = collectionVector.size();
			collection = new SakaiCollection[size];
			for (int i=0; i < size; i++) {
				collection[i] = (SakaiCollection)collectionVector.elementAt(i);
			}
		}
		return collection;
	}
}
