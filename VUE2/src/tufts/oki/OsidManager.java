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
 * OsidManager.java
 *
 * Created on October 23, 2003, 3:07 PM
 */

package tufts.oki;
import osid.OsidOwner;

/**
 *
 * @author  Mark Norton
 */
public class OsidManager implements osid.OsidManager{
     /* Use this constant for the organizational authority string.  */
    public static final String AUTHORITY = "tufts.edu";
   
    private osid.OsidOwner mgr_owner;
    
    /**
     *  Creates a new instance of OsidManager.  Since no owner is provided, a 
     *  default empty owner is created.
     *
     *  @author Mark Norton
     */
    public OsidManager() {
        mgr_owner = new osid.OsidOwner();
    }
    
    /**
     *  Create a new instance of OsidManager using owner provide. 
     *
     *  @author Mark Norton
     */
    public OsidManager(osid.OsidOwner owner) {
     //   assert (owner != null) :  "Owner is null";
        mgr_owner = owner;
    }
    
    /**
     *  Get the owner of this manager.
     *
     *  @author Mark Norton
     *
     *  @return The owner of this manager.
     */
    public osid.OsidOwner getOwner() {
        return mgr_owner;
    }
    
    /**
     *  The OsidLoader uses this method to determine which version of the OSID is being used.
     */
    public void osidVersion_1_0() {
    }
    
    /**
     *  Update the the configuration map.
     *  <p>
     *  Currently unimplemented.
     *
     *  @author Mark Norton
     */
    public void updateConfiguration(java.util.Map configuration) throws osid.OsidException {
        //throw new osid.OsidException (osid.OsidException.UNIMPLEMENTED);
    }
    
    /**
     *  Change the owner of this manager to the one provided.
     *
     *  @author Mark Norton
     */
    public void updateOwner(osid.OsidOwner owner) throws osid.OsidException {
       // assert (owner != null) :  "Owner is null";
        mgr_owner = owner;
    }
    
}
