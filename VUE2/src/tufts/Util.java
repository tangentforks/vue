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

package tufts;

import tufts.macosx.MacOSX;

import java.io.*;
import java.net.*;
import java.lang.ref.*;
import java.util.*;
import java.util.jar.*;
import java.util.prefs.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Utility class.  Provides code for determining what platform we're on,
 * platform-specific code for opening URL's, as well as various convenience
 * functions.
 *
 * @version $Revision: $ / $Date: $ / $Author: sfraize $
 */

public class Util
{
    private static boolean WindowsPlatform = false;
    private static boolean MacPlatform = false;
    private static boolean MacAquaLAF = false;
    private static boolean MacAquaLAF_set = false;
    private static boolean UnixPlatform = false;
    private static final String PlatformName;
    private static float javaVersion = 1.0f;
    
    private static int MacMRJVersion = -1;

    private static boolean DEBUG = false;

    private static String PlatformEncoding;
    
    static {

        if (!DEBUG)
            DEBUG = System.getProperty("tufts.Util.debug") != null;
            
        String osName = System.getProperty("os.name");
        String javaSpec = System.getProperty("java.specification.version");

        PlatformName = osName + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch");

        try {
            javaVersion = Float.parseFloat(javaSpec);
            if (DEBUG) out("Java Version: " + javaVersion);
        } catch (Exception e) {
            errorOut("couldn't parse java.specifcaion.version: [" + javaSpec + "]");
            errorOut(e.toString());
        }

        String osn = osName.toUpperCase();
        if (osn.startsWith("MAC")) {
            MacPlatform = true;
            if (DEBUG) out("Mac JVM: " + osName);
            String mrj = System.getProperty("mrj.version");
            int i = 0;
            while (i < mrj.length()) {
                if (!Character.isDigit(mrj.charAt(i)))
                    break;
                i++;
            }

            try {
                MacMRJVersion = Integer.parseInt(mrj.substring(0, i));
            } catch (NumberFormatException e) {
                errorOut("couldn't parse mrj.version: " + e);
                errorOut(e.toString());
            }
            if (DEBUG) out("Mac mrj.version: \"" + mrj + "\" = " + MacMRJVersion);
            
        } else if (osn.indexOf("WINDOWS") >= 0) {
            WindowsPlatform = true;
            if (DEBUG) out("Windows JVM: " + osName);
        } else {
            UnixPlatform = true;
        }

        final String term = System.getenv("TERM");
        if (term == null || term.indexOf("color") < 0) {
            TERM_RED = TERM_GREEN = TERM_YELLOW = TERM_BLUE = TERM_PURPLE = TERM_CYAN = TERM_CLEAR = "";
        } else {
            TERM_RED    = "\033[1;31m";  
            TERM_GREEN  = "\033[1;32m";
            TERM_YELLOW = "\033[1;33m";
            TERM_BLUE   = "\033[1;34m";
            TERM_PURPLE = "\033[1;35m";
            TERM_CYAN   = "\033[1;36m";
            TERM_CLEAR  = "\033[m";
        }
        //printStackTrace("TERM[" + term + "]");
    }
    
    /** Common escape codes for terminal text colors.  Set to empty string unless on a color terminal */
    public static final String TERM_RED, TERM_GREEN, TERM_YELLOW, TERM_BLUE, TERM_PURPLE, TERM_CYAN, TERM_CLEAR;

    private static void out(String s) {
        System.out.println("tufts.Util: " + s);
    }

    private static void errorOut(String s) {
        System.err.println("tufts.Util: " + s);
    }

    /*
     * Be sure to compare this value to a constant *float*
     * value -- e.g. "1.4f" -- just "1.4" as double value may
     * actually appear to be less than 1.4, as that can't
     * be exactly represented by a double.
     */
    public static float getJavaVersion() {
        return javaVersion;
    }
       
    /** return mac runtime for java version.  Will return -1 if we're not running on mac platform. */
    public static int getMacMRJVersion() {
        return MacMRJVersion;
    }
    
    public static String getPlatformName() {
        return PlatformName == null ? (System.getProperty("os.name") + "?") : PlatformName;
    }
       

    /** @return true if we're running on a version Microsoft Windows */
    public static boolean isWindowsPlatform() {
        return WindowsPlatform;
    }
    /** @return true if we're running on an Apple Mac OS */
    public static boolean isMacPlatform() {
        return MacPlatform;
    }
    /** @return true if we're running on unix only platform (e.g., Linux -- Mac OS X not included) */
    public static boolean isUnixPlatform(){
        return UnixPlatform;
    }

    public static String getDefaultPlatformEncoding() {
        if (PlatformEncoding == null)
            PlatformEncoding = (new java.io.OutputStreamWriter(new java.io.ByteArrayOutputStream())).getEncoding();
        return PlatformEncoding;
    }
    

    /** @return true if the current Look & Feel is Mac Aqua (not always true just because you're on a mac)
     * Note: do NOT call this from any static initializers the result may be changed by application startup
     * code. */
    public static boolean isMacAquaLookAndFeel()
    {
        // we can't set this at static init time because the LAF can be set after that
        if (MacAquaLAF_set == false) {
            MacAquaLAF =
                isMacPlatform() &&
                javax.swing.UIManager.getLookAndFeel().getName().toLowerCase().indexOf("aqua") >= 0;
            MacAquaLAF_set = true;
        }
        return MacAquaLAF;
    }

    /** @param url -- a url assumed to be in the platform default format (e.g., it may be
     * formatted in a way only understood on the current platform).
     */
    public static void openURL(String url)
        throws java.io.IOException
    {
        boolean isLocalFile = (url.length() >= 5 && "file:".equalsIgnoreCase(url.substring(0,5))) || url.startsWith("/");
        boolean isMailTo = url.length() >= 7 && "mailto:".equalsIgnoreCase(url.substring(0,7));

        if (DEBUG) System.err.println("openURL_PLAT [" + url + "] isLocalFile=" + isLocalFile + " isMailTo=" + isMailTo);
        
        if (isMacPlatform())
            openURL_Mac(url, isLocalFile, isMailTo);
        else if (isUnixPlatform())
            openURL_Unix(url, isLocalFile, isMailTo);
        else // default is a windows platform
            openURL_Windows(url, isLocalFile, isMailTo);
    }

    private static final String PC_OPENURL_CMD = "rundll32 url.dll,FileProtocolHandler";
    private static void openURL_Windows(String url, boolean isLocalFile, boolean isMailTo)
        throws java.io.IOException
    {
        System.err.println("openURL_Win  [" + url + "]");

        // On at least Windows 2000, %20's don't work when given to url.dll FileProtocolHandler
        // (Should be using some kind of URLProtocolHanlder ?)
        
        // Also at least Win2K: file:///C:/foo/bar.html works, but start that with file:/ or file://
        // and it DOES NOT work -- you MUST have the three slashes, OR, you can have NO SLASHES,
        // and it will work... (file:C:/foo/bar.html)
        // ALSO, file:// or // file:/// works BY ITSELF (file:/ by itself still doesn't work).
        
        //url = url.replaceAll("%20", " ");

        if (!isMailTo) {
            url = decodeURL(url);
            url = decodeURL(url); // run twice in case any %2520 double encoded spaces
        }

        if (isLocalFile) {
            // below works, but we're doing a full conversion to windows path names now
            //url = url.replaceFirst("^file:/+", "file:");
            url = url.replaceFirst("^file:/+", "");
            url = url.replace('/', '\\');
            char c1 = 0;
            try { c1 = url.charAt(1); } catch (StringIndexOutOfBoundsException e) {}
            if (c1 == ':' || c1 == '|') {
                // Windows drive letter specification, e.g., "C:".
                // Also, I've seen D|/dir/file.txt in a shortcut file, so we allow that too.
                // In any case, we do noting: this string should be workable to url.dll
            } else {
                // if this does NOT start with a drive specification, assume
                // the first component is a host for a windows network
                // drive, and thus we have to pre-pend \\ to it, to get \\host\file
                url = "\\\\" + url; 
            }
            // at this point, "url" is really just a local windows file
            // in full windows syntax (backslashes, drive specs, etc)
            System.err.println("openURL_WinLF[" + url + "]");
        }

        String cmd = PC_OPENURL_CMD + " " + url;
        final int sizeURL = url.length();
        final int sizeCommand = cmd.length();
        final int sizeWinArgs = sizeCommand - 17; // subtract "rundll32 url.dll,"
        boolean debug = DEBUG;
        if (sizeURL > 2027 || sizeCommand > 2064 || sizeWinArgs >= 2048) {
            System.err.println("\nWarning: WinXP buffer lengths likely exceeded: command not likely to run (arglen=" + sizeWinArgs + ")");
            debug = true;
        } else {
            System.err.println();
        }
        if (debug) {
            System.err.println("exec       url length: " + sizeURL);
            System.err.println("exec   command length: " + sizeCommand);
            System.err.println("exec url.dll args len: " + sizeWinArgs);
        }

        if (sizeCommand > 2064) {
            System.err.println("exec: truncating command and hoping for the best...");
            cmd = cmd.substring(0,2063);
        }

        System.err.println("exec[" + cmd + "]");

        Runtime.getRuntime().exec(cmd);

//         final String mailto = "mailto:"
//             + "foo@bar.com?"
//             + "subject=TestSubject"
//             + "&attachment="
//             //+ "&Attach="
//             + "\""
//             + "c:\\\\foo.txt"
//             + "\""
//             ;
//         System.err.println("MAILTO: ["+mailto+"]");
//         Runtime.getRuntime().exec(new String[] {"rundll32", "url.dll,FileProtocolHandler", mailto
//                                                 "mailto:"
//                                                 + "&subject=TestSubject"
//                                                 + "&attachment=" + "\"" + "c:\\\\foo.txt" + "\""
//             },
//             null);

        
//         if (false) {
//             Process p = Runtime.getRuntime().exec(cmd);
//             try {
//                 System.err.println("waiting...");
//                 p.waitFor();
//             } catch (Exception ex) {
//                 System.err.println(ex);
//             }
//             System.err.println("exit value=" + p.exitValue());
//         }
    }

    /**
     * Call a named static method with the given args.
     * The method signature is inferred from the argument types.  We map the primitive
     * type wrapper class to their primitive types (an auto un-boxing), so this
     * won't work for method calls that take args of Boolean, Integer, etc,
     * only boolean, int, etc.
     */
    public static Object execute(Object object, String className, String methodName, Object[] args)
        throws ClassNotFoundException,
               NoSuchMethodException,
               IllegalArgumentException,
               IllegalAccessException,
               java.lang.reflect.InvocationTargetException
    {
        if (DEBUG) {
            // use multiple prints in remote case of a class-load or some such error along the way
            System.err.println("Util.execute:");
            System.err.println("\t className: [" + className + "]");
            System.err.println("\tmethodName: [" + methodName + "]");

            String desc;
            if (args == null)
                desc = "null";
            else if (args.length == 0)
                desc = "(none)";
            else if (args.length == 1)
                desc = objectTag(args[0]) + " [" + args[0] + "]";
            else
                desc = Arrays.asList(args).toString();
                
            System.err.println("\t      args: " + desc);
            System.err.println("\t    object: " + objectTag(object) + " [" + object + "]");
                               
        }
        
        final Class clazz = Class.forName(className);
        final Class[] argTypes;
        
        if (args == null) {
            argTypes = null;
        } else {
            argTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                final Class argClass = args[i].getClass();
                if (argClass == Boolean.class) argTypes[i] = Boolean.TYPE;
                else if (argClass == Integer.class) argTypes[i] = Integer.TYPE;
                else if (argClass == Long.class)    argTypes[i] = Long.TYPE;
                else if (argClass == Short.class)   argTypes[i] = Short.TYPE;
                else if (argClass == Float.class)   argTypes[i] = Float.TYPE;
                else if (argClass == Double.class)  argTypes[i] = Double.TYPE;
                else if (argClass == Byte.class)    argTypes[i] = Byte.TYPE;
                else if (argClass == Character.class) argTypes[i] = Character.TYPE;
                else
                    argTypes[i] = args[i].getClass();
            }
        }
                
        java.lang.reflect.Method method = clazz.getMethod(methodName, argTypes);

        if (DEBUG) System.err.print("\t  invoking: " + method + " ... ");
        
        Object result = method.invoke(object, args);

        if (DEBUG) System.err.println("returned.");
        
        if (DEBUG && method.getReturnType() != void.class) {
            // use multiple prints in remote case of a class-load or some such error along the way
            //System.err.println("Util.execute:");
           System.err.println("\t    return: " + objectTag(result) + " [" + result + "]");
        }
        //System.out.println("Sucessfully invoked " + className + "." + methodName + ", result=" + result);
        
        return result;
    }

    public static Object execute(String className, String methodName, Object[] args)
        throws ClassNotFoundException,
               NoSuchMethodException,
               IllegalArgumentException,
               IllegalAccessException,
               java.lang.reflect.InvocationTargetException
    {
        return execute(null, className, methodName, args);
    }
    
    /** Attempt a method call.  If it fails, quietly fail, printing a stack trace and returning null.
     * @param object - cannot be null (implying a static method), as is also used to get the class name
     * If object is a Class object, make this a static invocation in that class.
     */
    public static Object invoke(Object object, String methodName, Object[] args) 
    {
        String className;
        if (object instanceof Class) {
            className = ((Class)object).getName();
            object = null;
        } else
            className = object.getClass().getName();
        
        try {
            return execute(object, className, methodName, args);
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }

    /**
     * Attempt a method call.  If it fails, quietly fail, printing a stack trace and returning null.
     * This is a convenience version that allows the passing in of a single argument directly.
     **/
    public static Object invoke(Object object, String methodName, Object arg0) {
        return invoke(object, methodName, new Object[] { arg0 });
    }
    public static Object invoke(Object object, String methodName) {
        return invoke(object, methodName, null);
    }
    
    /**
     * Attempt to invoke the given method with the given args.  If 
     * any exception occurs, (e.g., ClassNotFoundException because a
     * particular library isn't present) we quietly catch it and
     * pass it back as the return value;
     */
    
    public static Object executeIfFound(String className, String methodName, Object[] args) {
        try {
            return execute(className, methodName, args);
        } catch (Exception e) {
            return e;
        }
    }

    /** Encode a query pair URL such that it will work with the current platform openURL code.
     * E.g., construct a mailto: whose subject/body args are encoded such that they successfully
     * pass through to the local mail client.
     */
    public static String makeQueryURL(String urlStart, String ... nameValuePairs) {

        StringBuffer url = new StringBuffer(urlStart);

        boolean isKey = true;
        boolean isFirstKey = true;
        String curKey = "<no-key!>";
        for (String s : nameValuePairs) {
            //System.out.format("TOKEN[%s]\n", s);
            if (isKey) {
                if (isFirstKey) {
                    url.append('?');
                    isFirstKey = false;
                } else
                    url.append('&');
                curKey = s;
                System.out.format("KEY[%s]\n", s);
                url.append(s);
                url.append('=');
            } else if ("attachment".equals(curKey)) {
                // append windows pathname raw:
                // An "attachment" arg has been rumoured to work on Windows,
                // but as far as I can tell, these rumours are total BS.
                // Have yet to see this work in any configuration.
                url.append('"' + s + '"');
            } else {
                // value field:
                final String encodedValue = encodeURLData(s);
                if (DEBUG) {
                    System.out.format("    %-17s [%s]\n", curKey + "(RAW):", s);
                    System.out.format("    %-17s [%s]\n", curKey + "(ENCODED):", encodedValue);
                }
                url.append(encodedValue);
            }
            isKey = !isKey;
            
        }
        //if (DEBUG) System.out.println("QUERY URL: " + url);
        return url.toString();
    }
    
    public static String encodeURLData(String url) {
        try {
            url = URLEncoder.encode(url, "UTF-8");
            url = url.replaceAll("\\+", "%20"); // Mail clients don't understand '+' as space
        } catch (Throwable t) {
            // should never happen:
            printStackTrace(t);
        }
        return url;
    }



    // GAK!  Move this code to URLResource, as it's going to have to be smart
    // about local file drops for mac URL's, to be sure to UTF-8 DECODE them
    // first, so we get RID of special characters or spaces, which come
    // when the local file was dragged Safari, as opposed to the Finder
    // (e.g., Desktop), which already decodes them for us.

    // Also, we need to generically handle the decoding of ":" into "/"
    // for these URL's. (God forbid there is HTML in the name: e.g. <i>foo</i>
    // will send us a ':' in place of the '/', which we also don't
    // want to mistake for a protocol below.  Actually, do fix the ':'
    // in the display name (title), but not raw meta-data: could be confusing.


    private static java.lang.reflect.Method macOpenURL_Method = null;
    private static void openURL_Mac(String url, boolean isLocalFile, boolean isMailTo)
    {
        if (DEBUG) System.err.println("openURL_Mac0 [" + url + "]");

        if (isLocalFile) {
            boolean changed = true;
            if (url.startsWith("file:////")) {
                // don't think we have to do this, but just in case
                // (was getting a complaint and couldn't tell if this was why or not,
                // so now we won't see it)
                url = "file:///" + url.substring(9);
            } else if (url.startsWith("/")) {
                // must have file: at head to be sure it works on mac
                url = "file:" + url;
            } else
                changed = false;
            if (DEBUG && changed) System.err.println("openURL_Mac1 [" + url + "]");
        }

        if (isMailTo) {
//             final String flatURL = url.toLowerCase();
//             final int queryIndex = flatURL.indexOf('?') + 1;
//             if (queryIndex > 1) {
//                 final String query = flatURL.substring(queryIndex);
//                 if (DEBUG) System.out.println("QUERY: [" + query + "]");
//                 final int subjectIndex = query.indexOf("subject=");
//             }
            
            //url = url.replaceAll(" ", "%20");
//             try {
//                 url = URLEncoder.encode(url, "UTF-8");
//             } catch (Throwable t) {
//                 printStackTrace(t);
//             }
        } else {
            try {
                url = encodeSpecialChars_Mac(url, isMailTo);
            } catch (Throwable t) {
                printStackTrace(t);
            }
        }

        // In Mac OS X, local files MUST have %20 and NO spaces in the URL's -- reverse of Windows.
        // But enforcing this for regular HTTP URL's breaks them: turns existing %20's into %2520's

        // If no protocol in the URL string, assume it's a pathname.  Normally, there
        // would be nothing to do as openURL handles that fine on MacOSX, unless it's
        // not absolute, it which case we make it absolute by assuming the users home
        // directory.
        
        if (!isMailTo && url.indexOf(':') < 0 && !url.startsWith("/")) {
            // Hack to make relative references relative to user home directory.  OSX
            // won't default to use current directory for a relative references, so 
            // we're prepending the home directory manually as a bail out try-for-it.
            url = "file://" + System.getProperty("user.home") + "/" + url;
            if (DEBUG) System.err.println("    OUM HOME [" + url + "]");
        }

        execMacOpenURL(url);
    }

    // WHAT WE KNOW WORKS FOR MAILTO: '@' encoded, but ? / & decoded, except WITHIN
    // the value of &field=value, which of course messes it up.
    // Also in the working state: All spaces as %20, newlines %0A
    // If we take out the colons tho, we're screwed.

    private static String encodeSpecialChars_Mac(String url, boolean isMailTo)
        throws java.io.IOException
    {
        // In case there are any special characters (e.g., Unicode chars) in the
        // file name, we must first encode them for MacOSX (local files only?)
        // FYI, MacOSX openURL uses UTF-8, NOT the native MacRoman encoding.
        // URLEncoder encodes EVERYTHING other than alphas tho, so we need
        // to put it back.

        // But first we DECODE it, in case there are already any encodings,
        // we don't want to double-encode.
        url = java.net.URLDecoder.decode(url, "UTF-8");
        if (DEBUG) System.err.println("  DECODE UTF [" + url + "]");

        url = java.net.URLEncoder.encode(url, "UTF-8"); // URLEncoder is way overzealous...
        if (DEBUG) System.err.println("  ENCODE UTF [" + url + "]");

            // now decode the over-coded stuff so it looks sane (has colon & slashes, etc)
        url = url.replaceAll("%3A", ":"); // be sure to do ALL of these...
        url = url.replaceAll("%2F", "/");
        url = url.replaceAll("%3F", "?");
        url = url.replaceAll("%3D", "=");
        url = url.replaceAll("%26", "&");
        url = url.replaceAll("\\+", "%20"); // Mac doesn't undestand '+' I think

        if (DEBUG) System.err.println("     CLEANUP [" + url + "]");

        return url;
    }

    private static void execMacOpenURL(String url)
    {
        if (getJavaVersion() >= 1.4f) {
            // Can't call this directly because wont compile on the PC
            //com.apple.eio.FileManager.openURL(url);
            
            if (macOpenURL_Method == null) {
                try {
                    Class macFileManager = Class.forName("com.apple.eio.FileManager");
                    //Class macFileManager = ClassLoader.getSystemClassLoader().loadClass("com.apple.eio.FileManager");
                    macOpenURL_Method = macFileManager.getMethod("openURL", new Class[] { String.class });
                } catch (Exception e) {
                    System.err.println("Failed to find Mac FileManager or openURL method: will not be able to display URL content.");
                    e.printStackTrace();
                    throw new UnsupportedOperationException("com.apple.eio.FileManager:openURL " + e);
                }
            }
            
            if (macOpenURL_Method != null) {
                try {
                    macOpenURL_Method.invoke(null, new Object[] { url });
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new UnsupportedOperationException("com.apple.eio.FileManager.openURL " + e);
                }
            } else
                throw new UnsupportedOperationException("openURL_Mac");


        } else {
            throw new UnsupportedOperationException("mac java <= 1.3 openURL unimplemented");
            // put this back if want to suppor mac java 1.3
            // this has been deprecated in mac java 1.4, so
            // just ignore the warning if using a 1.4 or beyond
            // compiler
            //    com.apple.mrj.MRJFileUtils.openURL(url);
        }

        if (DEBUG) System.err.println("execMacOpenURL returns for (" + url + ")");
        
    }

    

    private static void openURL_Unix(String url, boolean isLocalFile, boolean isMailTo)
        throws java.io.IOException
    {
        // For now we just assume Netscape is installed.

        // todo: run down list of netscape, mozilla, konqueror (KDE
        // browser), gnome version?  KDE/Gnome may have their own
        // services for getting a default browser.
        
    	/*    	 
    	The problem I had with my 2 linux installations was that even though netscape wasn't 
    	installed the distribution provided a symlink named netscape which pointed to firefox
    	however firefox couldn't interpet the netscape parameters.  So below I've modified it
    	to try to load firefox first and then if that fails load netscape, i suppose konqueror
    	should be in the mix too maybe but this is better than it was and catches alot of cases.
    	*/
    	Process process = null;
    	try
    	{
    		process = Runtime.getRuntime().exec(new String[] { "firefox", url});
    	}
    	catch (java.io.IOException ioe)
    	{
    		//firefox not available try netscape instead.
    		process = Runtime.getRuntime().exec(new String[] { "netscape",
                    "-remove",
                    "'openURL('" + url + "')" });
    	}
        out("process: " + process);
        
// Can't wait for process to complete!  Browser is left running...
//         try {
//             int exitCode = process.waitFor();
//             if (exitCode != 0)	// if Netscape was not open
//                 Runtime.getRuntime().exec(new String[] { "netscape", url });
//         } catch (InterruptedException e) {
//             java.io.IOException ioe =  new java.io.IOException();
//             ioe.initCause(e);
//             throw ioe;
//         }
    }


    /**
     * Fast impl of replacing %xx hexidecimal codes with actual characters (e.g., %20 is a space, %2F is '/').
     * Leaves untouched any bad hex digits, or %xx strings that represent control characters (less than space/0x20
     * or greater than tilde/0x7E).
     *
     * @return String with replaced %xx codes.  If there are no '%' characters in the input string,
     * the original String object is returned.
     */
    public static String decodeURL(final String s)
    {
        //System.out.println("DECODING " + s);
        int i = s.indexOf('%');
        if (i < 0)
            return s;
        final int len = s.length();
        final StringBuffer buf = new StringBuffer(len);
        // copy in everything we've skipped in the original string up to now
        buf.append(s.substring(0, i));
        //System.out.println("DECODE START " + buf);
        for ( ; i < len; i++) {
            final char c = s.charAt(i);
            if (c == '%' && i+2 < len) {
                final int hex1 = Character.digit(s.charAt(++i), 16);
                final int hex2 = Character.digit(s.charAt(++i), 16);
                final char charValue = (char) (hex1 * 16 + hex2);
                
                //System.out.println("Got char value " + charValue + " from " + c1 + c2);
                
                if (hex1 < 1 || hex2 < 0 || charValue < 0x20 || charValue > 0x7E) {
                    // Pass through untouched if not two good hex characters (and first can't be 0),
                    // or if result is a control character (anything less than space, or greater than '~')
                    buf.append('%');
                    buf.append(s.charAt(i-1));
                    buf.append(s.charAt(i));
                } else {
                    buf.append(charValue);
                }
            } else {
                buf.append(c);
            }
        }
        if (DEBUG) {
            System.out.println("DECODED      [" + s + "]");
            System.out.println("     TO      [" + buf + "]");
        }
        return buf.toString();
    }

    
    public static final java.util.Iterator EmptyIterator = new java.util.Iterator() {
            public boolean hasNext() { return false; }
            public Object next() { throw new NoSuchElementException(); }
            public void remove() { throw new UnsupportedOperationException(); }
            public String toString() { return "EmptyIterator"; }
        };

    public static final Iterable EmptyIterable = new Iterable() {
            public Iterator iterator() { return EmptyIterator; }
            public String toString() { return "EmptyIterable"; }
        };
    

    /** Convenience class: provides a single element iterator */
    public static final class SingleIterator<T> implements java.util.Iterator<T>, Iterable<T> {
        private T object;
        public SingleIterator(T o) {
            object = o;
        }
        public boolean hasNext() { return object != null; }
        public T next() { if (object == null) throw new NoSuchElementException(); T o = object; object = null; return o; }
        public void remove() { throw new UnsupportedOperationException(); }
        public Iterator<T> iterator() { return this; }
        public String toString() { return "SingleIterator[" + object + "]"; }
        
    };
    
    /** Convenience class: provides an array iterator */
    public static final class ArrayIterator implements java.util.Iterator, Iterable {
        private Object[] array;
        private int index;
        public ArrayIterator(Object[] a) {
            array = a;
            index = 0;
        }
        public boolean hasNext() { return index < array.length; }
        public Object next() { return array[index++]; }
        public void remove() { throw new UnsupportedOperationException(); }
        public Iterator iterator() { return this; }
    };

    /** GroupIterator allows you to construct a new iterator that
     * will aggregate an underlying set of Iterators and/or Collections */
    public static final class GroupIterator extends java.util.ArrayList
        implements java.util.Iterator, Iterable
    {
        int iterIndex = 0;
        Iterator curIter;
        
        public GroupIterator() {}
        
        /** All parameters must be either instances of Iterator or Collection */
        public GroupIterator(Object i1, Object i2)
        {
            this(i1, i2, null);
        }
        /** All parameters must be either instances of Iterator or Collection */
        public GroupIterator(Object i1, Object i2, Object i3)
        {
            super(3);
            if (i1 == null || i2 == null)
                throw new IllegalArgumentException("null Collection or Iterator");
            add(i1);
            add(i2);
            if (i3 != null)
                add(i3);
        }

        /**
         * Add a new Iterator or Collection to this GroupIterator.  This can only be
         * done before iteration has started for this GroupIterator.
         * @param o an Iterator or Collection
         * @return result of super.add (ArrayList.add)
         */
        public boolean add(Object o) {
            if (!(o instanceof Iterable) &&
                !(o instanceof Iterator))
                throw new IllegalArgumentException("Can only add Iterable or Iterator: " + o);
            return super.add(o);
        }

        public boolean hasNext()
        {
            if (curIter == null) {
                if (iterIndex >= size())
                    return false;
                Object next = get(iterIndex);
                if (next instanceof Iterator)
                    curIter = (Iterator) next;
                else
                    curIter = ((Iterable)next).iterator();
                iterIndex++;
                if (curIter == null)
                    return false;
            }
            // make the call on the underlying iterator
            if (curIter.hasNext()) {
                return true;
            } else {
                curIter = null;
                return hasNext();
            }
        }

        public Object next()
        {
            if (curIter == null)
                return null;
            else
                return curIter.next();
        }

        public void remove()
        {
            if (curIter != null)
                curIter.remove();
            else
                throw new IllegalStateException(this + ": no underlying iterator");
        }

        public Iterator iterator() {
            return this;
        }
    }


    /**
     * A soft-reference map for maps containing items can be garbage
     * collected if we run low on memory (e.g., images).  If that
     * happens, items will seem to have disspeared from the cache.
     * 
     * Not all HashMap methods covered: only safe to use
     * the onces explicity implemented here.
     */
    public static class SoftMap extends HashMap {

        public synchronized Object get(Object key) {
            //out("SoftMap; get: " + key);
            Object val = super.get(key);
            if (val == null)
                return null;
            Reference ref = (Reference) val;
            val = ref.get();
            if (val == null) {
                //out("SoftMap; image was garbage collected: " + key);
                super.remove(key);
                return null;
            } else
                return val;
        }
        
        public synchronized Object put(Object key, Object value) {
            //out("SoftMap; put: " + key);
            return super.put(key, new SoftReference(value));
        }

        public synchronized boolean containsKey(Object key) {
            return get(key) != null;
        }

        public synchronized void clear() {
            Iterator i = values().iterator();
            while (i.hasNext())
                ((Reference)i.next()).clear();
            super.clear();
        }
        
    }
    

    public static void dumpCollection(Collection c) {
        System.out.println("Collection of size: " + c.size() + " (" + c.getClass().getName() + ")");
        dumpIterator(c.iterator());
    }
    public static void dumpIterator(Iterator i) {
        int x = 0;
        while (i.hasNext()) {
            System.out.println((x<10?" ":"") + x + ": " + i.next());
            x++;
        }
    }

    public static void dumpURL(URL u) {
        out("URL dump: " + u
            + "\n\t  protocol " + u.getProtocol()
            + "\n\t  userInfo " + u.getUserInfo()
            + "\n\t authority [" + u.getAuthority() + "]"
            + "\n\t      host [" + u.getHost() + "]"
            + "\n\t      port " + u.getPort()
            + "\n\t      path " + u.getPath()
            + "\n\t      file " + u.getFile()
            + "\n\t     query " + u.getQuery()
            + "\n\t       ref " + u.getRef()
            );
        
    }

    public static void dumpURI(URI u) {
        dumpURI(u, null);
    }
    public static void dumpURI(URI u, String msg) {

        synchronized (System.out) {
            //System.out.format("%16s URI: %s %s\n", msg, u, System.identityHashCode(u), u, msg==null?"":"("+msg+")");
            System.out.format("%16s URI: %s @%x\n",
                              msg==null?"":('"'+msg+'"'),
                              u,
                              System.identityHashCode(u));
            dumpField("hashCode",       Integer.toHexString(u.hashCode()));
            dumpField("scheme",		u.getScheme());
            dumpField("rawAuthority",   u.getRawAuthority());
            dumpField("authority",      u.getAuthority());
            dumpField("userInfo",       u.getUserInfo());
            dumpField("host",		u.getHost());
            if (u.getPort() != -1)
                dumpField("port",	u.getPort());
            dumpField("rawPath",        u.getRawPath());
            dumpField("path",		u.getPath());
            dumpField("rawQuery",       u.getRawQuery());
            dumpField("query",		u.getQuery());
            dumpField("rawFragment",    u.getRawFragment());
            dumpField("fragment",       u.getFragment());
            System.out.println("-------------------------------------------------------");
        }
        
//             + "\n\t     hashCode " + Integer.toHexString(u.hashCode())
//             + "\n\t       scheme " + u.getScheme()
//             + "\n\t rawAuthority " + u.getRawAuthority()
//             + "\n\t    authority " + u.getAuthority()
//             + "\n\t     userInfo " + u.getUserInfo()
//             + "\n\t         host " + u.getHost()
//             + "\n\t         port " + u.getPort()
//             + "\n\t      rawPath " + u.getRawPath()
//             + "\n\t         path " + u.getPath()
//             + "\n\t     rawQuery " + u.getRawQuery()
//             + "\n\t        query " + u.getQuery()
//             + "\n\t  rawFragment " + u.getRawFragment()
//             + "\n\t     fragment " + u.getFragment()
        
    }

    private static void dumpField(String label, Object value) {
        if (value != null)
            System.out.format("%20s: %s\n", label, value);
    }

    /** center the given window on default physical screen */
    public static void centerOnScreen(java.awt.Window window)
    {
        if (getJavaVersion() >= 1.4f) {
            java.awt.Point p = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
            p.x -= window.getWidth() / 2;
            p.y -= window.getHeight() / 2;
            window.setLocation(p);
        } else {
            java.awt.Dimension d = window.getToolkit().getScreenSize();
            int x = d.width/2 - window.getWidth()/2;
            int y = d.height/2 - window.getHeight()/2;
            window.setLocation(x, y);
        }
    }


    static final double sFactor = 0.9;
    public static Color darkerColor(Color c) {
        return factorColor(c, sFactor);
    }
    public static Color brighterColor(Color c) {
        return factorColor(c, 1/sFactor);
    }
    public static Color factorColor(Color c, double factor)
    {
	return new Color((int)(c.getRed()  *factor),
			 (int)(c.getGreen()*factor),
			 (int)(c.getBlue() *factor));
    }


    public static final float brightness(java.awt.Color c) {
            
        if (c == null)
            return 0;

        final int r = c.getRed();
        final int g = c.getGreen();
        final int b = c.getBlue();

        int max = (r > g) ? r : g;
        if (b > max) max = b;
            
        return ((float) max) / 255f;
    }
        
    /** @return a new color, which is a mix a given color with alpha, plus another NON alpha color */
    // ... wouldn't take much more to mix multiple alpha's, tho normally some color at bottom should
    // always be non-alpha
    public static final Color alphaMix(java.awt.Color ac, java.awt.Color c) {
        final float r = ac.getRed();
        final float g = ac.getGreen();
        final float b = ac.getBlue();
        final float alpha = ac.getAlpha();
        final float mix = 255 - alpha;

        return new Color((int) ( (r * alpha + mix * c.getRed()  ) / 255f + 0.5f ),
                         (int) ( (g * alpha + mix * c.getGreen()) / 255f + 0.5f ),
                         (int) ( (b * alpha + mix * c.getBlue() ) / 255f + 0.5f ));
        }

    


    /** a JPanel that anti-aliases text */
    public static class JPanelAA extends javax.swing.JPanel {
        public JPanelAA(java.awt.LayoutManager layout) {
            super(layout, true);
        }
        public JPanelAA() {}

        public void paint(java.awt.Graphics g) {
            // only works when, of course, the panel is asked
            // to redraw -- but if you mess with subcomponents
            // and just they repaint, we lose this.
            // todo: There must be a way to stick this in a global
            // property somewhere.

            // anti-alias is default on mac, so don't do it there.
            if (!isMacPlatform())
                ((java.awt.Graphics2D)g).setRenderingHint
                    (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                     java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paint(g);
        }
    }

    public static class JLabelAA extends javax.swing.JLabel {
        public JLabelAA(String text) {
            super(text);
        }
        public JLabelAA() {}

        public void paintComponent(java.awt.Graphics g) {
            // anti-alias is default on mac, so don't do it there.
            if (!isMacPlatform())
                ((java.awt.Graphics2D)g).setRenderingHint
                    (java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                     java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paintComponent(g);
        }
    }
    
    
    /** This is for testing individual components. It will display the given component in frame
     * of the given size. */
    public static JFrame displayComponent(java.awt.Component comp, int width, int height)
    {
        javax.swing.JFrame frame = new javax.swing.JFrame(objectTag(comp));
        comp.setSize(comp.getPreferredSize());
        if (comp instanceof JComponent)
            frame.setContentPane((JComponent)comp);
        else
            frame.getContentPane().add(comp);
        if (width != 0 && height != 0)
            frame.setSize(width, height);
        else
            frame.pack();
        frame.validate();
        centerOnScreen(frame);
        frame.setVisible(true);
        return frame;
    }
    
    /** This is for testing individual components. It will display the given component in frame. */
    public static JFrame displayComponent(java.awt.Component comp) {
        return displayComponent(comp, 0, 0);
    }

    private static JColorChooser colorChooser;
    private static Dialog colorChooserDialog;
    private static boolean colorChosen;
    /** Convience method for running a JColorChooser and collecting the result */
    public static Color runColorChooser(String title, Color c, Component chooserParent)
    {
        if (colorChooserDialog == null) {
            colorChooser = new JColorChooser();
            //colorChooser.setDragEnabled(true);
            //colorChooser.setPreviewPanel(new JLabel("FOO")); // makes it dissapear entirely, W2K/1.4.2/Metal
            if (false) {
                final JPanel np = new JPanel();
                np.add(new JLabel("Text"));
                np.setSize(new Dimension(300,100)); // will be invisible otherwise
                np.setBackground(Color.red);
                //np.setBorder(new EmptyBorder(10,10,10,10));
                //np.setBorder(new EtchedBorder());
                np.setBorder(new LineBorder(Color.black));
                np.setOpaque(true);
                np.addPropertyChangeListener(new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent e) {
                            System.out.println("CC " + e.getPropertyName() + "=" + e.getNewValue());
                            if (e.getPropertyName().equals("foreground"))
                                np.setBackground((Color)e.getNewValue());
                        }});
                colorChooser.setPreviewPanel(np); // also makes dissapear entirely
            }
            /*
            JComponent pp = colorChooser.getPreviewPanel();
            System.out.println("CC Preview Panel: " + pp);
            for (int i = 0; i < pp.getComponentCount(); i++)
                System.out.println("#" + i + " " + pp.getComponent(i));
            colorChooser.getPreviewPanel().add(new JLabel("FOO"));
            */
            colorChooserDialog =
                JColorChooser.createDialog(chooserParent,
                                           "Color Chooser",
                                           true,  
                                           colorChooser,
                                           new ActionListener() { public void actionPerformed(ActionEvent e)
                                               { colorChosen = true; } },
                                           null);
        }
        if (c != null)
            colorChooser.setColor(c);
        if (title != null)
            colorChooserDialog.setTitle(title);

        colorChosen = false;
        // show() blocks until a color chosen or cancled, then automatically hides the dialog:
        colorChooserDialog.setVisible(true);

        JComponent pp = colorChooser.getPreviewPanel();
        System.out.println("CC Preview Panel: " + pp + " children=" + Arrays.asList(pp.getComponents()));
        for (int i = 0; i < pp.getComponentCount(); i++)
            System.out.println("#" + i + " " + pp.getComponent(i));
        
        return colorChosen ? colorChooser.getColor() : null;
    }

    public static void screenToBlack() {
        if (!isMacPlatform())
            return;
        try {
            MacOSX.goBlack();    
        } catch (LinkageError e) {
            eout(e);
        }
    }
    
    public static void screenFadeFromBlack() {
        if (isMacPlatform()) {
            try {
                MacOSX.fadeFromBlack();
            } catch (LinkageError e) {
                eout(e);
            }
        }
    }

    /** For mac platform: to keep tool windows that are frames on top
     * of the main app window, you need to go to "native" java/cocoa code.
     * @param inActionTitle - the title of a window just shown / or currently
     * undergoing a show, to make sure we fix up, even if it doesn't claim
     * to be visible yet.
     */
    public static void adjustMacWindows(String mainWindowTitleStart,
                                        String ensureShown,
                                        String ensureHidden,
                                        boolean inFullScreenMode)
    {
        if (isMacPlatform()) {
            try {
                MacOSX.adjustMacWindows(mainWindowTitleStart, ensureShown, ensureHidden, inFullScreenMode);
            } catch (LinkageError e) {
                eout(e);
            } catch (Exception e) {
                System.err.println("failed to handle mac window adjustment");
                e.printStackTrace();
            }
        }
    }

    public static String upperCaseWords(String s)
    {
        //if (DEBUG) out("UCW in["+s+"]");

        StringBuffer result = new StringBuffer(s.length());
        String[] words = s.split(" ");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            //if (DEBUG) out("word" + i + "["+word+"]");
            if (word.length() < 1)
                continue;
            if (Character.isLowerCase(word.charAt(0))) {
                result.append(Character.toUpperCase(word.charAt(0)));
                word = word.substring(1);
            }
            result.append(word);
            if (i + 1 < words.length)
                result.append(' ');
        }
        //if (DEBUG) out("UCWout["+result+"]");
        return result.toString();
    }

    private static void eout(LinkageError e) {
        if (e instanceof NoSuchMethodError)
            eout((NoSuchMethodError)e);
        else if (e instanceof NoClassDefFoundError)
            eout((NoClassDefFoundError)e);
        else {
            System.err.println(e + ": problem with Mac Java/Cocoa Code");
        }
    }
    
    private static void eout(NoSuchMethodError e) {
        // If tufts.macosx.Screen get's out of date, or
        // it's library is not included in the build, we'll
        // get a NoSuchMethodError
        System.err.println(e + ": tufts.macosx.Screen needs rebuild or VUE-MacOSX.jar library not in classpath");
    }
    private static void eout(NoClassDefFoundError e) {
        // We'll get this if /System/Library/Java isn't in the classpath
        System.err.println(e + ": Not Mac OS X Platform, or VUE-MacOSX.jar and/or /System/Library/Java not in classpath");
    }


    public static String fmt(final Shape shape) {

        final Rectangle2D r;
        final String name;
        if (shape instanceof Rectangle2D) {
            r = (Rectangle2D) shape;
            name = "Rect";
        } else {
            if (shape == null) {
                name = "Shape";
                r = null;
            } else {
                name = shape.getClass().getName();
                r = shape.getBounds2D();
            }
        }
        
        return shape == null
            ? "<null-" + name + ">"
            : String.format("%s@%x[%7.1f,%-7.1f %5.1fx%-5.1f]",
                            name,
                            shape == null ? 0 : System.identityHashCode(shape),
                            r.getX(), r.getY(), r.getWidth(), r.getHeight());
        
    }

    public static String fmt(final java.awt.Color c) {
        if (c == null)
            return "<null-Color>";
        
        if (c.getAlpha() != 0xFF)
            return String.format("Color(%d,%d,%d,%d)", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        else
            return String.format("Color(%d,%d,%d)", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static String fmt(java.awt.geom.Point2D p) {
        if (p == null)
            return "<null-Point2D>";
        else
            return String.format("%.1f,%.1f", p.getX(), p.getY());
    }

    public static String fmt(java.awt.geom.Line2D l) {
        if (l == null)
            return "<null-Line2D>";
        else
            return String.format("%.1f,%.1f -> %.1f,%.1f", l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }
    

    public static void out(Object o) {
        System.out.println((o==null?"null":o.toString()));
    }

    /*
    public static void out(String s) {
        System.out.println(s==null?"null":s);
    }
    */
    public static String out(Object[] o) {
        if (o == null)
            return "<null Object[]>";
        else
            return Arrays.asList(o).toString();
    }
    
    public static String out(java.awt.geom.Point2D p) {
        return fmt(p);
        //return oneDigitDecimal(p.getX()) + "," + oneDigitDecimal(p.getY());
        //return (float)p.getX() + "," + (float)p.getY();
    }
    
    public static String out(java.awt.Dimension d) {
        return d == null ? "null" : (d.width + "x" + d.height);
    }

    public static String out(java.awt.geom.Rectangle2D r) {
        if (r == null)
            return "<null Rectangle2D>";
        else
            return String.format("[%7.1f,%-7.1f %5.1fx%-5.1f]", r.getX(), r.getY(), r.getWidth(), r.getHeight());
//         return ""
//             + (float)r.getX() + "," + (float)r.getY()
//             + " "
//             + (float)r.getWidth() + "x" + (float)r.getHeight()
//             ;
    }

    public static String out(java.awt.Rectangle r) {
        if (r == null)
            return "<null Rectangle>";
        else
            return String.format("[%4d,%4d %-5dx%4d]", r.x, r.y, r.width, r.height);
//         return ""
//             + r.width + "x" + r.height
//             + " "
//             + r.x + "," + r.y
//             ;
    }
    
    public static String out(java.awt.geom.RectangularShape r) {
        
        if (r == null)
            return "<null RectangularShape>";
        
        String name = r.getClass().getName();
        name = name.substring(name.lastIndexOf('.') + 1);
        return name + "["
            + (float)r.getX() + "," + (float)r.getY()
            + " "
            + (float)r.getWidth() + "x" + (float)r.getHeight()
            + "]"
            ;
    }
    
    public static String out(java.awt.geom.Line2D l) {
        if (l == null)
            return "<null Line2D>";
        else
            return ""
                + (float)l.getX1() + "," + (float)l.getY1()
                + " -> "
                + (float)l.getX2() + "," + (float)l.getY2()
                ;
    }
    
    public static String oneDigitDecimal(double x) {
        int tenX = (int) Math.round(x * 10);
        if ((tenX / 10) * 10 == tenX)
            return new Integer(tenX / 10).toString();
        else
            return new Float( ((float)tenX) / 10f ).toString();
    }

    /** @return a friendly looking string to represent the given number of bytes: e.g.: 120k, or 3.8M.
     * for values less than zero, returns ""
     */
    
    public static String abbrevBytes(long bytes) {
        if (bytes > 1024*1024)
            return oneDigitDecimal(bytes/(1024.0*1024)) + "M";
        else if (bytes > 1024)
            return bytes/1024 + "k";
        else if (bytes >= 0)
            return "" + bytes;
        else
            return "";
    }
    
    public static void test_OpenURL()
    {
        try {
            openURL("http://hosea.lib.tufts.edu:8080/fedora/get//tufts:7/demo:60/getThumbnail/");
            //openURL("file:///tmp/two words.txt");       // does not work on OSX 10.2 (does now with %20 space replacement)
            //openURL("\"file:///tmp/two words.txt\"");   // does not work on OSX 10.2
            //openURL("\'file:///tmp/two words.txt\'");   // does not work on OSX 10.2
            //openURL("file:///tmp/two%20words.txt");     // works on OSX 10.2, but not Windows 2000
            //openURL("file:///tmp/foo.txt");
            //openURL("file:///tmp/index.html");
            //openURL("file:///tmp/does_not_exist");
            //openURL("file:///zip/About_Developer_Tools.pdf");
        } catch (Exception e) {
            System.err.println(e);
        }
    }


    /** encode the given String in some charset into easily persistable UTF-8 8-bit format */
    public static String encodeUTF(String s) {
        if (s == null) return null;
        try {
            String encoded = new String(s.getBytes("UTF-8"));
            //System.out.println("ENCODED [" + encoded + "]");
            return encoded;
        } catch (java.io.UnsupportedEncodingException e) {
            // should never happen
            e.printStackTrace();
            return s;
        }
    }

    public static String encodeASCII(String s) {
        if (s == null) return null;
        try {
            // unfortunately, this doesn't encode non-ascii chars as % codes, only '?'
            String encoded = new String(s.getBytes("ASCII"));
            return encoded;
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
            return s;
        }
    }
    
    /** decode the given String in 8-bit UTF-8 to the default java 16-bit unicode format
        (the platform default, which varies) for display */
    public static String decodeUTF(String s) {
        if (s == null) return null;
        try {
            String decoded = new String(s.getBytes(), "UTF-8");
            //System.out.println("DECODED [" + decoded + "]");
            return decoded;
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
            return s;
        }
    }

    private final static String NO_CLASS_FILTER = "";


    private static final StringWriter ExceptionLog = new StringWriter(1024);
    private static final PrintWriter Log = new PrintWriter(ExceptionLog);

    /**
     * @eturn the contents of the exception log.  This is not a copy, it's
     * the actual log, so don't modify the contents unless you really mean
     * to.
     */
    public static StringBuffer getExceptionLog() {
        return ExceptionLog.getBuffer();
    }

    /** @return the log writer for anyone else who might want to write to it */
    public static Writer getLogWriter() {
        return Log;
    }
    


    /** print stack trace items only from fully qualified class names that match the given prefix */
    public static void printClassTrace(Throwable t, String prefix, String message, java.io.PrintStream pst) {

        java.awt.Toolkit.getDefaultToolkit().beep();
        
        synchronized (System.out) {
        synchronized (System.err) {
        synchronized (pst) {

            pst.print(TERM_RED);
            Log.println();

            if (message != null) {
                pst.println(message);
                Log.println(message);
            }
            
            final String head;
            if (t.getClass().getName().equals("java.lang.Throwable"))
                head = t.getMessage();
            else
                head = t.toString();
            if (prefix == null || prefix == NO_CLASS_FILTER) {
                pst.println(head + ";");
                Log.println(head + ";");
            } else {
                pst.println(head + " (stack element prefix \"" + prefix + "\") ");
                Log.println(head + " (stack element prefix \"" + prefix + "\") ");
            }

            final long now = System.currentTimeMillis();
            final String stamp = "\tin " + Thread.currentThread() + " at " + now + " " + new java.util.Date(now);

            pst.print(stamp);
            Log.print(stamp);

            pst.print(TERM_CLEAR);
            
            if (prefix == null || prefix == NO_CLASS_FILTER)
                prefix = "!tufts.Util.print";

            StackTraceElement[] trace = t.getStackTrace();
            int skipped = 0;
            for (int i = 0; i < trace.length; i++) {
                if (includeInTrace(trace[i], prefix)) {
                    pst.print("\n\tat " + trace[i] + " ");
                    Log.print("\n\tat " + trace[i] + " ");
                } else {
                    pst.print(".");
                    Log.print(".");
                }
            }
            pst.println();
            Log.println();

            Throwable cause = t.getCause();
            if (cause != null) {
                //ourCause.printStackTraceAsCause(s, trace);
                pst.print(TERM_RED);
                pst.print("    CAUSE: ");
                Log.print("    CAUSE: ");
                pst.print(TERM_CLEAR);
                cause.printStackTrace(pst);
                cause.printStackTrace(Log);
            }
            pst.println("END " + t + "\n");
            Log.println("END " + t + "\n");
            
        }
        }}

        //System.exit(-1); // e.g.: enable for debugging severe stack overflows
    }

    private static boolean includeInTrace(StackTraceElement trace, String prefix) {
        
        boolean matchIsIncluded = true;
        if (prefix.charAt(0) == '!') {
            prefix = prefix.substring(1);
            matchIsIncluded = false;
        }

        String where = trace.getClassName() + "." + trace.getMethodName();

        if (where.startsWith(prefix))
            return matchIsIncluded;
        else
            return !matchIsIncluded;
    }

    public static void printClassTrace(Throwable t, String prefix) {
        printClassTrace(t, prefix, null, System.err);
    }
    
    public static void printClassTrace(String prefix, String message) {
        printClassTrace(new Throwable(message), prefix, null, System.err);
    }
    
    public static void printClassTrace(String prefix) {
        printClassTrace(prefix, "*** STACK TRACE ***");
    }
    public static void printClassTrace(Class clazz, String message) {
        printClassTrace(clazz.getName(), message);
    }

    public static void printStackTrace() {
        printClassTrace(NO_CLASS_FILTER);
    }
    
    public static void printStackTrace(Throwable t) {
        printClassTrace(t, NO_CLASS_FILTER, null, System.err);
    }
    
    public static void printStackTrace(Throwable t, String message) {
        printClassTrace(t, NO_CLASS_FILTER, message, System.err);
    }
        
    public static void printStackTrace(String message) {
        printClassTrace(new Throwable(message), NO_CLASS_FILTER);
    }
    

    public static String tag(Object o) {
        if (o == null)
            return "null";
        else
            return String.format("%s@%07x", o.getClass().getName(), System.identityHashCode(o));
    }
    
    /**
     * Produce a "package.class@idenityHashCode[toString]" debug tag to uniquely identify
     * arbitrary objects.  Allows for toString failure, and shortens the output if the
     * toString result already includes the type (class) @ identityHashCode information.
     */
    public static String tags(Object o) {
        if (o == null)
            return "null";

        if (o instanceof java.lang.String) {
            // special case for strings: we dont care about hashCode / type -- just return quoted
            return '"' + o.toString() + '"';
        }
            
        final String type = o.getClass().getName();
        String txt = null;
        try {
            txt = o.toString();
        } catch (Throwable t) {
            txt = t.toString();
        }

        final int ident = System.identityHashCode(o);
        final String pureTag = String.format("%s@%x", type, ident); // default java object toString

        if (txt.startsWith(pureTag)) {
            // remove redunant type info from toString
            txt = txt.substring(pureTag.length());
        } else {
            final String shortTag = String.format("%s@%x", o.getClass().getSimpleName(), ident);
            // remove redunant (short) type info from toString
            if (txt.startsWith(shortTag))
                txt = txt.substring(shortTag.length());
        }
        
        final String s;
        if (txt.length() > 0)
            s = String.format("%s@%07x[%s]", type, ident, txt);
        else
            s = String.format("%s@%07x", type, ident);
            
        return s;
    }
    
    public static String objectTag(Object o) {
        return tag(o);
    }

    
    public static String pad(char c, int wide, String s, boolean alignRight) {
        if (s.length() >= wide)
            return s;
        int pad = wide - s.length();
        StringBuffer buf = new StringBuffer(wide);
        if (alignRight == false)
            buf.append(s);
        while (pad-- > 0)
            buf.append(c);
        if (alignRight)
            buf.append(s);
        return buf.toString();
    }

    public static String pad(char c, int wide, String s) {
        return pad(c, wide, s, false);
    }
    public static String pad(int wide, String s) {
        return pad(' ', wide, s, false);
    }

    /**
     * For now, this just determines if DNS is available, and assumes that if it is,
     * we have external network access.  Ultimately, pinging a known "permanent",
     * high-availability host (e.g., www.google.com), would be somewhat more accurate,
     * (tho also slignly risky, as whatever you pick may in fact someday not respoond)
     * but InetAddress.isReachable is a java 1.5 API call, and VUE isn't built
     * with that yet.
     *
     * TODO: This currently not actually helpful to us: only will tell you if DNS
     * is available the first time this is run in the Java VM (VUE was started),
     * after that, the result is cached.
     */
    public static boolean isInternetReachable() {
        try {
            InetAddress result = InetAddress.getByName("www.google.com");
            return result != null;
        } catch (Throwable t) {
            return false;
        }
    }

    

    public static void main(String args[])
        throws Exception
    {

        if (false) {
            openURL(makeQueryURL("MAILTO:foo@foobar.com",
                                 
                                 "subject", "VUE Log Report",
                                 
                                 //"attachment", "c:\\\\foo.txt"
                                 //,
                                 
                                 "body",
                                 "I am the body.  Spic & Span?"
                                 + "\nfoo@bar.com"
                                 + "\nfile://local/file/"
                                 + "\n\\\\.psf\\foobie\\"
                                 + "\ncolons:are:no:problem"


+ "\nVUE 2007-06-25 18:32:43,187 [main] INFO   Startup; build: June 25 2007 at 1523 by sfraize on Mac OS X 10.4.10 i386 JVM 1.5.0_07-164"
+ "\nVUE 2007-06-25 18:32:43,197 [main] INFO   Running in Java VM: 1.5.0_11-b03; MaxMemory(-Xmx)=381.1M, CurMemory(-Xms)=1.9M"
+ "\nVUE 2007-06-25 18:32:43,197 [main] INFO   VUE version: 2.0 alpha-x"
+ "\nVUE 2007-06-25 18:32:43,197 [main] INFO   Current Working Directory: \\\\.psf\\vue"
+ "\nVUE 2007-06-25 18:32:43,197 [main] INFO   User/host: Scott Fraize@null"
+ "\nVUE 2007-06-25 18:32:43,197 [main] DEBUG GUI init"
+ "\nVUE 2007-06-25 18:32:43,257 [main] DEBUG GUI LAF  name: Windows"
+ "\nVUE 2007-06-25 18:32:43,257 [main] DEBUG GUI LAF descr: The Microsoft Windows Look and Feel"
+ "\nVUE 2007-06-25 18:32:43,257 [main] DEBUG GUI LAF class: class com.sun.java.swing.plaf.windows.WindowsLookAndFeel"
+ "\nVUE 2007-06-25 18:32:47,523 [main] DEBUG  loading disk cache..."
+ "\nVUE 2007-06-25 18:32:47,784 [main] DEBUG  Got cache directory: C:\\Documents and Settings\\Scott Fraize\\vue_2\\cache"
+ "\nVUE 2007-06-25 18:32:47,784 [main] DEBUG  listing disk cache..."
+ "\nVUE 2007-06-25 18:32:47,784 [main] DEBUG  listing disk cache: done; entries=21"
+ "\nVUE 2007-06-25 18:32:47,934 [main] DEBUG  loading disk cache: done"
+ "\nVUE 2007-06-25 18:32:48,064 [main] DEBUG  loading fonts."
+ "XXXXXXX"
+ "XXXXXXX"
+ "XXXXXXX"
+ "0"
+ "1" // 2064
+ "2" // 2065

                                 + "\n\nEnd.\n"
                                 ));
            System.exit(0);
        }
                         

    
        if (args.length > 0 && "network".equals(args[0])) {
            int tries = 1;
            for (int i = 0; i < tries; i++) {
                if (i > 0)
                    Thread.sleep(2000);
                System.err.print("Internet is reachable (DNS available): ");
                System.err.println("" + isInternetReachable());
            }
        
            Enumeration nie = NetworkInterface.getNetworkInterfaces();
            while (nie.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nie.nextElement();
                System.err.println("\nNetwork Interface[" + ni + "]");
                //Enumeration ie = ni.get
            
            }
            System.exit(0);
        }

        //System.err.println("ServerSocket: " + new ServerSocket(0)); // not sensitive to network availability
      
       if (false && args.length > 0 && args[0].startsWith("-")) {
            String host = args[0].substring(1);
            InetAddress[] ips = InetAddress.getAllByName(host);
            System.out.println(host + " IP's: " + Arrays.asList(ips));
            System.exit(0);
        }
        
        
        //System.out.println("cursor16 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(16,16));
        //System.out.println("cursor24 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(24,24));
        //System.out.println("cursor32 " + java.awt.Toolkit.getDefaultToolkit().getBestCursorSize(32,32));
                                       //.list(System.out);

        DEBUG = true;
        // TESTING CODE
        System.out.println("Default JVM character encoding for this platform: " +
                           (new java.io.OutputStreamWriter(new java.io.ByteArrayOutputStream())).getEncoding());
            
        if (args.length > 0 && args[0].equals("-charsets")) {
            Map cs = java.nio.charset.Charset.availableCharsets();
            //System.out.println("Charsets: " + cs.values());
            Iterator i = cs.values().iterator();
            while (i.hasNext()) {
                java.nio.charset.Charset o = (java.nio.charset.Charset) i.next();
                System.out.println(o
                                   + "\t" + o.aliases()
                                   //+ " " + o.getClass()
                                   );
            }
            System.exit(0);
        }

        String test = "file:///Users/sfraize/Desktop/Cup�Chevron.png";
        
        if (args.length == 1) {
            if ("test".equals(args[0])) {
                execMacOpenURL(test);
                openURL(test);
            } else
                openURL(args[0]);
            //else
            //test_OpenURL();
        } else if (args.length == 2) {
            try {
                // Even tho we tried putting blackships.jar in two different places in java.library.path, it claimed it couldn't find it.
                System.loadLibrary("blackships.jar");
                java.net.URL url = new java.net.URL("blackships/large/02_010b_DutchFamily_l.jpg");
                System.out.println("URL: " + url);
                System.out.println("CONTENT: " + url.getContent());
                // cannot build a file object from a URL
                //java.net.URL url = new java.net.URL("jar:file:/VUE/src/VUE-core.jar!/tufts/vue/images/pathway_hide_on.gif");
                //java.net.URI uri = new java.net.URI(url.toString());
                //java.io.File f = new java.io.File(uri);
                //System.out.println("File " + f + " has length " + f.length() + " exists=" + f.exists());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (args.length == 3) {
            JarFile jar = new JarFile(args[0]);
            System.out.println("Got jar " + jar);
            Enumeration e = jar.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = (JarEntry) e.nextElement();
                long size = entry.getSize();
                System.out.println("Got entry " + entry + "  size=" + size);
                if (entry.getComment() != null)
                    System.out.println("\tcomment[" + entry.getComment() + "]");
                    byte[] extra = entry.getExtra();
                    if (extra != null) {
                        System.out.println("\textra len=" + extra.length + " [" + extra + "]");
                    }
                if (entry.getName().endsWith("MANIFEST.MF")) {
                    java.io.InputStream in = jar.getInputStream(entry);
                    byte[] data = new byte[(int)size];
                    in.read(data);
                    System.out.println("Contents[" + new String(data) + "]");
                }
            }
                
        } else {
            Hashtable props = System.getProperties();
            Enumeration e = props.keys();
            while (e.hasMoreElements()) {
                Object key = e.nextElement();
                //System.out.println("[1;36m" + key + "[m=" + props.get(key));
                System.out.println(key + "=" + props.get(key));
            }
        }
        
        System.exit(0);
    }

    
    
}
