package tufts.vue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import edu.tufts.vue.preferences.implementations.AutoZoomPreference;
import edu.tufts.vue.preferences.implementations.BooleanPreference;
import edu.tufts.vue.preferences.implementations.StringPreference;
import edu.tufts.vue.preferences.interfaces.VuePreference;

public class RecentlyOpenedFilesManager 
{
	 private static VuePreference openFilePref = StringPreference.create(
				edu.tufts.vue.preferences.PreferenceConstants.FILES_CATEGORY,
				"recentlyOpenedFiles", 
				"Recently Opened Files", 
				"Number of Recently Opened Files to maintain",
				"5",
				false);
	
	private LinkedList list = new LinkedList();
	private int maxsize; 
	private static RecentlyOpenedFilesManager _instance; 
	
	
	 // For lazy initialization
	 public static synchronized RecentlyOpenedFilesManager getInstance() {
	  if (_instance==null) {
	   _instance = new RecentlyOpenedFilesManager();
	  }
	  return _instance;
	 }	
	 
	 public VuePreference getPreference()
	 {
		 return openFilePref;
	 }
	 
	private RecentlyOpenedFilesManager()
	{
		StringTokenizer tokens = new StringTokenizer((String)openFilePref.getValue(),"*");
		
		while (tokens.hasMoreTokens())
		{
			list.add(tokens.nextToken());
		}
		maxsize = getMaxSize();
		
		return;
	}
	
	private int getMaxSize()
	{
		maxsize = Integer.valueOf((String)list.getFirst());
		return maxsize;
		
	}
	
	private void setMaxSize(int i)
	{
		list.removeFirst();
		list.set(0, (Object)(new Integer(i).toString()));
		maxsize = i;
	}
	
	public int getFileListSize()
	{
		return list.subList(1, list.size()).size();
	}
	
	public List getRecentlyOpenedFiles()
	{
		return list.subList(1, list.size());
	}
	
	public void updateRecentlyOpenedFiles(String s)
	{
		//try to add this file to the list and update the preference
		Iterator i = list.iterator();
		
		//is file already in list?
		if (list.contains(s))
		{
			int index = list.indexOf(s);
			list.remove(index);
			list.add(1, s);
			
		}
		else
		{
			//if it's not in the list add it to the top and deal with it later
			list.add(1,s);
		}
		
		//trim list if necessary...
		while (list.size() > (maxsize + 1))
			list.removeLast();
		
		//build a new pref String;
		StringBuffer sbuffer = new StringBuffer();
		
		i = list.iterator();
		
		while (i.hasNext())
		{
			sbuffer.append((String)i.next());
			sbuffer.append("*");
		}
		//update the preference
		openFilePref.setValue(sbuffer.toString());
	}
}
