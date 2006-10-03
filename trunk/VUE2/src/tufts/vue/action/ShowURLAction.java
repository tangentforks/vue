package tufts.vue.action;

import tufts.vue.VueUtil;
import tufts.vue.DEBUG;
import tufts.vue.VUE;
import java.net.URL;
import java.io.File;

/**
 * Display a given URL in an external browser.
 */
public class ShowURLAction extends tufts.vue.VueAction
{
    private String urlString = null;
    private URL url = null;
    private String localName = null; // only used if we need to unpack from a jar
    
    public ShowURLAction(String urlString) {
        this(urlString, urlString);
    }
    public ShowURLAction(String label, String urlString) {
        super(label);
        this.urlString = urlString;
    }

    /**
     * If the given URL is a jar:file URL (which a browser can't acccess),
     * unpack it into the users VUE config dir (e.g., ~/.vue), and
     * display it from there.  However, if the file was already
     * unpacked and created there, don't bother to unpack it again. 
     */
    public ShowURLAction(String label, URL url, String fileName) {
        super(label);
        this.url = url;
        this.localName = fileName;
    }
    
    public void act()
    {
        String browserURL = null;
        
        if (url == null) {
            browserURL = urlString;
        } else {
            
            if (DEBUG.IO) VUE.Log.info("ShowURLAction: raw url: " + url);
            if (DEBUG.IO) out("PROTOCOL: " + url.getProtocol());

            if ("jar".equals(url.getProtocol())) {

                if (DEBUG.IO) out("Dealing with [" + localName + "]");
                File dir = VueUtil.getDefaultUserFolder();
                File localFile = new File(dir, this.localName);
                
                try {                
                    if (DEBUG.IO) out("Looking for " + localFile);
                    if (localFile.exists() && localFile.length() > 0) {
                        if (DEBUG.IO) out("EXISTS: " + localFile);
                    } else {
                        if (DEBUG.IO) out("COPYING FILE to " + localFile);
                        if (!localFile.exists() && !localFile.createNewFile())
                            throw new Error("failed to create " + localFile);
                        VueUtil.copyURL(url, localFile);
                    }

                    browserURL = localFile.toURL().toString();
                    if (DEBUG.IO) out("LOCAL URL: " + browserURL);
                    
                } catch (Throwable t) {
                    VUE.Log.error("Failed to display content for " + url + " in " + localFile);
                    t.printStackTrace();
                    return;
                }
            } else {
                browserURL = url.toString();
            }
        }
        
        try {
            tufts.vue.VueUtil.openURL(browserURL);
        } catch (Exception ex) {
            //System.out.println("ShowURLAction " + this + " failed to display [" + url + "] on " + ae);
            throw new RuntimeException("ShowURLAction " + this + " failed to display [" + url + "]", ex);
            
        }
    }
}