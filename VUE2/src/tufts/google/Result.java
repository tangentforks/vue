/*
 * Result.java
 *
 * Created on April 18, 2003, 3:19 PM
 */

package  tufts.google;

/**
 *
 * @author  akumar03
 */
public class Result {
    
    private int count;
    
    private String mimeType;
    
    private String url;
    
    private String title;
    
    private String description;
    
    /** Creates a new instance of Result */
    public Result() {
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public int getCount() {
        return this.count;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public String getMimeType() {
        return this.mimeType;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getUrl() {
        return this.url;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getTitle(){
        return this.title;
    }
    
    public void setDescription(String description){
        this.description = description;
    }
    
    public String getDescription(){
        return this.description;
    }
    
     public void addObject(Object obj) {
   
       //-- ignore
     }

}
