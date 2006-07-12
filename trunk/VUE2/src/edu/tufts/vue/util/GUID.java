/*
 * GUID.java
 *
 * Created on July 12, 2006, 3:17 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 * 
 */

/**
 *
 * @author AKumar03
 */
package edu.tufts.vue.util;
// obtained from: http://forum.java.sun.com/thread.jspa?threadID=453923&start=15&tstart=0
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Arrays;
 
/** a globally unique 32-byte number (allegedly) */
public class GUID implements Serializable
{
    private static SecureRandom s_secureRandom=new SecureRandom();
    private static int s_hash=System.identityHashCode(s_secureRandom);
    private static byte[] s_addr;
 
    /** get the ip adress. if fails, generate a random one */
    static
    {
        try
        {
            s_addr=InetAddress.getLocalHost().getAddress();
        }
        catch(Exception ex)
        {
            s_addr=new byte[4];
            s_secureRandom.nextBytes(s_addr);
        }
    }
 
    /** get int as 8-char string */
    private static String hexFormat(int data)
    {
        StringBuffer stringBuffer=new StringBuffer();
        stringBuffer.append("00000000");
        stringBuffer.append(Integer.toHexString(data));
        return stringBuffer.substring(stringBuffer.length()-8);
    }
 
    /** get byte as 2 char string */
    private static String hexFormat(byte data)
    {
        StringBuffer stringBuffer=new StringBuffer();
        stringBuffer.append("00");
        stringBuffer.append(Integer.toHexString(data));
        return stringBuffer.substring(stringBuffer.length()-2);
    }
 
    /** generate guid from time,addr,hash and rand */
    public static String generate()
    {
        StringBuffer stringBuffer=new StringBuffer();
        int time=(int)System.currentTimeMillis();
        int rand=s_secureRandom.nextInt();
        stringBuffer.append(hexFormat(time));
        stringBuffer.append(hexFormat(s_addr[0]));
        stringBuffer.append(hexFormat(s_addr[1]));
        stringBuffer.append(hexFormat(s_addr[2]));
        stringBuffer.append(hexFormat(s_addr[3]));
        stringBuffer.append(hexFormat(s_hash));
        stringBuffer.append(hexFormat(rand));
        return stringBuffer.toString();
    }
}
