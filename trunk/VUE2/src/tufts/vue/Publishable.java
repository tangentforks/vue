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


/*
 * Publishable.java
 *
 * Created on June 27, 2004, 12:27 PM
 *
 *The Interface defines methods to publish to a data source and the types of publish modes.
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 */
import java.io.IOException;
public interface Publishable {
      
    public static final int PUBLISH_NO_MODES = 0;;
    public static final int PUBLISH_MAP = 1; // just the map
    public static final int PUBLISH_CMAP = 2; // the map with selected resources in IMSCP format
    public static final int PUBLISH_ALL = 3; // all resources published to fedora and map published with pointers to resources.
    public static final int PUBLISH_ALL_MODES = 10; // this means that datasource can publish to any mode.
 
    /*
     */
    public int[] getPublishableModes();
    
    public void publish(int mode,LWMap map) throws IOException;
        
    
}
