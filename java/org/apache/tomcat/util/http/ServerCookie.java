/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.tomcat.util.http;

import java.io.Serializable;
import java.text.FieldPosition;
import java.util.Date;

import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.DateTool;
import org.apache.tomcat.util.buf.MessageBytes;


/**
 *  Server-side cookie representation.
 *  Allows recycling and uses MessageBytes as low-level
 *  representation ( and thus the byte-> char conversion can be delayed
 *  until we know the charset ).
 *
 *  Tomcat.core uses this recyclable object to represent cookies,
 *  and the facade will convert it to the external representation.
 */
public class ServerCookie implements Serializable {
    
    
    private static org.apache.juli.logging.Log log =
        org.apache.juli.logging.LogFactory.getLog(ServerCookie.class);

    // Version 0 (Netscape) attributes
    private MessageBytes name=MessageBytes.newInstance();
    private MessageBytes value=MessageBytes.newInstance();
    // Expires - Not stored explicitly. Generated from Max-Age (see V1)
    private MessageBytes path=MessageBytes.newInstance();
    private MessageBytes domain=MessageBytes.newInstance();
    private boolean secure;
    
    // Version 1 (RFC2109) attributes
    private MessageBytes comment=MessageBytes.newInstance();
    private int maxAge = -1;
    private int version = 0;

    // Note: Servlet Spec =< 2.5 only refers to Netscape and RFC2109,
    // not RFC2965

    // Version 1 (RFC2965) attributes
    // TODO Add support for CommentURL
    // Discard - implied by maxAge <0
    // TODO Add support for Port

    public ServerCookie() {
    }

    public void recycle() {
        path.recycle();
        name.recycle();
        value.recycle();
        comment.recycle();
        maxAge=-1;
        path.recycle();
        domain.recycle();
        version=0;
        secure=false;
    }

    public MessageBytes getComment() {
        return comment;
    }

    public MessageBytes getDomain() {
        return domain;
    }

    public void setMaxAge(int expiry) {
        maxAge = expiry;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public MessageBytes getPath() {
        return path;
    }

    public void setSecure(boolean flag) {
        secure = flag;
    }

    public boolean getSecure() {
        return secure;
    }

    public MessageBytes getName() {
        return name;
    }

    public MessageBytes getValue() {
        return value;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int v) {
        version = v;
    }


    // -------------------- utils --------------------

    public static void log(String s ) {
        if (log.isDebugEnabled())
            log.debug("ServerCookie: " + s);
    }

    public String toString() {
        return "Cookie " + getName() + "=" + getValue() + " ; "
            + getVersion() + " " + getPath() + " " + getDomain();
    }
    
    private static final String tspecials = ",; ";
    private static final String tspecials2 = "()<>@,;:\\\"/[]?={} \t";

    /*
     * Tests a string and returns true if the string counts as a
     * reserved token in the Java language.
     *
     * @param value the <code>String</code> to be tested
     *
     * @return      <code>true</code> if the <code>String</code> is a reserved
     *              token; <code>false</code> if it is not
     */
    public static boolean isToken(String value) {
        if( value==null) return true;
        int len = value.length();

        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);

            if (c < 0x20 || c >= 0x7f || tspecials.indexOf(c) != -1)
                return false;
        }
        return true;
    }

    public static boolean isToken2(String value) {
        if( value==null) return true;
        int len = value.length();

        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);

            if (c < 0x20 || c >= 0x7f || tspecials2.indexOf(c) != -1)
                return false;
        }
        return true;
    }

    /**
     * @deprecated - Not used
     */
    public static boolean checkName( String name ) {
        if (!isToken(name)
                || name.equalsIgnoreCase("Comment")     // rfc2019
                || name.equalsIgnoreCase("Discard")     // rfc2965
                || name.equalsIgnoreCase("Domain")      // rfc2019
                || name.equalsIgnoreCase("Expires")     // Netscape
                || name.equalsIgnoreCase("Max-Age")     // rfc2019
                || name.equalsIgnoreCase("Path")        // rfc2019
                || name.equalsIgnoreCase("Secure")      // rfc2019
                || name.equalsIgnoreCase("Version")     // rfc2019
                // TODO remaining RFC2965 attributes
            ) {
            return false;
        }
        return true;
    }

    // -------------------- Cookie parsing tools

    
    /**
     * Return the header name to set the cookie, based on cookie version.
     */
    public String getCookieHeaderName() {
        return getCookieHeaderName(version);
    }

    /**
     * Return the header name to set the cookie, based on cookie version.
     */
    public static String getCookieHeaderName(int version) {
        // TODO Re-enable logging when RFC2965 is implemented
        // log( (version==1) ? "Set-Cookie2" : "Set-Cookie");
        if (version == 1) {
            // XXX RFC2965 not referenced in Servlet Spec
            // Set-Cookie2 is not supported by Netscape 4, 6, IE 3, 5
            // Set-Cookie2 is supported by Lynx and Opera
            // Need to check on later IE and FF releases but for now... 
            // RFC2109
            return "Set-Cookie";
            // return "Set-Cookie2";
        } else {
            // Old Netscape
            return "Set-Cookie";
        }
    }

    private static final String ancientDate =
        DateTool.formatOldCookie(new Date(10000));

    // TODO RFC2965 fields also need to be passed
    public static void appendCookieValue( StringBuffer buf,
                                          int version,
                                          String name,
                                          String value,
                                          String path,
                                          String domain,
                                          String comment,
                                          int maxAge,
                                          boolean isSecure )
    {
        // Servlet implementation checks name
        buf.append( name );
        buf.append("=");
        // Servlet implementation does not check anything else
        
        maybeQuote2(version, buf, value);

        // Add version 1 specific information
        if (version == 1) {
            // Version=1 ... required
            buf.append ("; Version=1");

            // Comment=comment
            if ( comment!=null ) {
                buf.append ("; Comment=");
                maybeQuote2(version, buf, comment);
            }
        }
        
        // Add domain information, if present
        if (domain!=null) {
            buf.append("; Domain=");
            maybeQuote2(version, buf, domain);
        }

        // Max-Age=secs ... or use old "Expires" format
        // TODO RFC2965 Discard
        if (maxAge >= 0) {
            if (version == 0) {
                // Wdy, DD-Mon-YY HH:MM:SS GMT ( Expires Netscape format )
                buf.append ("; Expires=");
                // To expire immediately we need to set the time in past
                if (maxAge == 0)
                    buf.append( ancientDate );
                else
                    DateTool.formatOldCookie
                        (new Date( System.currentTimeMillis() +
                                   maxAge *1000L), buf,
                         new FieldPosition(0));

            } else {
                buf.append ("; Max-Age=");
                buf.append (maxAge);
            }
        }

        // Path=path
        if (path!=null) {
            buf.append ("; Path=");
            maybeQuote2(version, buf, path);
        }

        // Secure
        if (isSecure) {
          buf.append ("; Secure");
        }
        
        
    }

    /**
     * @deprecated - Not used
     */
    public static void maybeQuote (int version, StringBuffer buf,
            String value) {
        // special case - a \n or \r  shouldn't happen in any case
        if (isToken(value)) {
            buf.append(value);
        } else {
            buf.append('"');
            buf.append(escapeDoubleQuotes(value));
            buf.append('"');
        }
    }
    
    /**
     * Quotes values using rules that vary depending on Cookie version.
     * @param version
     * @param buf
     * @param value
     */
    public static void maybeQuote2 (int version, StringBuffer buf,
            String value) {
        // special case - a \n or \r  shouldn't happen in any case
        if (version == 0 && isToken(value) || version == 1 && isToken2(value)) {
            buf.append(value);
        } else {
            buf.append('"');
            buf.append(escapeDoubleQuotes(value));
            buf.append('"');
        }
    }


    /**
     * Escapes any double quotes in the given string.
     *
     * @param s the input string
     *
     * @return The (possibly) escaped string
     */
    private static String escapeDoubleQuotes(String s) {

        if (s == null || s.length() == 0 || s.indexOf('"') == -1) {
            return s;
        }

        StringBuffer b = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"')
                b.append('\\').append('"');
            else
                b.append(c);
        }

        return b.toString();
    }

    /**
     * Unescapes any double quotes in the given cookie value.
     *
     * @param bc The cookie value to modify
     */
    public static void unescapeDoubleQuotes(ByteChunk bc) {

        if (bc == null || bc.getLength() == 0 || bc.indexOf('"', 0) == -1) {
            return;
        }

        int src = bc.getStart();
        int end = bc.getEnd();
        int dest = src;
        byte[] buffer = bc.getBuffer();
        
        while (src < end) {
            if (buffer[src] == '\\' && src < end && buffer[src+1]  == '"') {
                src++;
            }
            buffer[dest] = buffer[src];
            dest ++;
            src ++;
        }
        bc.setEnd(dest);
    }
}

