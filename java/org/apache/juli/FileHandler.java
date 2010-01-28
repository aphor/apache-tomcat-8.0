/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.juli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Implementation of <b>Handler</b> that appends log messages to a file
 * named {prefix}.{date}.{suffix} in a configured directory, with an
 * optional preceding timestamp.
 *
 * @version $Revision$ $Date$
 */

public class FileHandler
    extends Handler {


    // ------------------------------------------------------------ Constructor

    
    public FileHandler() {
        this(null, null, null);
    }
    
    
    public FileHandler(String directory, String prefix, String suffix) {
        this.directory = directory;
        this.prefix = prefix;
        this.suffix = suffix;
        configure();
        openWriter();
    }
    

    // ----------------------------------------------------- Instance Variables


    /**
     * The as-of date for the currently open log file, or a zero-length
     * string if there is no open log file.
     */
    private volatile String date = "";


    /**
     * The directory in which log files are created.
     */
    private String directory = null;


    /**
     * The prefix that is added to log file filenames.
     */
    private String prefix = null;


    /**
     * The suffix that is added to log file filenames.
     */
    private String suffix = null;


    /**
     * The PrintWriter to which we are currently logging, if any.
     */
    private volatile PrintWriter writer = null;
    
    /**
     * Log buffer size
     */
    private int bufferSize = -1;


    // --------------------------------------------------------- Public Methods


    /**
     * Format and publish a <tt>LogRecord</tt>.
     *
     * @param  record  description of the log event
     */
    @Override
    public void publish(LogRecord record) {

        if (!isLoggable(record)) {
            return;
        }

        // Construct the timestamp we will use, if requested
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        String tsString = ts.toString().substring(0, 19);
        String tsDate = tsString.substring(0, 10);

        // If the date has changed, switch log files
        if (!date.equals(tsDate)) {
            synchronized (this) {
                if (!date.equals(tsDate)) {
                    closeWriter();
                    date = tsDate;
                    openWriter();
                }
            }
        }

        String result = null;
        try {
            result = getFormatter().format(record);
        } catch (Exception e) {
            reportError(null, e, ErrorManager.FORMAT_FAILURE);
            return;
        }
        
        try {
            PrintWriter writer = this.writer;
            if (writer!=null) {
                writer.write(result);
                if (bufferSize < 0) {
                    writer.flush();
                }
            } else {
                reportError("FileHandler is closed or not yet initialized, unable to log ["+result+"]", null, ErrorManager.WRITE_FAILURE);
            }
        } catch (Exception e) {
            reportError(null, e, ErrorManager.WRITE_FAILURE);
            return;
        }
        
    }
    
    
    // -------------------------------------------------------- Private Methods


    /**
     * Close the currently open log file (if any).
     */
    @Override
    public void close() {
        closeWriter();
    }

    protected void closeWriter() {
        
        try {
            PrintWriter writer = this.writer;
            this.writer = null;
            if (writer == null)
                return;
            writer.write(getFormatter().getTail(this));
            writer.flush();
            writer.close();
            writer = null;
            date = "";
        } catch (Exception e) {
            reportError(null, e, ErrorManager.CLOSE_FAILURE);
        }
        
    }


    /**
     * Flush the writer.
     */
    @Override
    public void flush() {

        try {
            PrintWriter writer = this.writer;
            if (writer==null)
                return;
            writer.flush();
        } catch (Exception e) {
            reportError(null, e, ErrorManager.FLUSH_FAILURE);
        }
        
    }
    
    
    /**
     * Configure from <code>LogManager</code> properties.
     */
    private void configure() {

        Timestamp ts = new Timestamp(System.currentTimeMillis());
        String tsString = ts.toString().substring(0, 19);
        date = tsString.substring(0, 10);

        String className = this.getClass().getName(); //allow classes to override
        
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        
        // Retrieve configuration of logging file name
        if (directory == null)
            directory = getProperty(className + ".directory", "logs");
        if (prefix == null)
            prefix = getProperty(className + ".prefix", "juli.");
        if (suffix == null)
            suffix = getProperty(className + ".suffix", ".log");
        String sBufferSize = getProperty(className + ".bufferSize", String.valueOf(bufferSize));
        try {
            bufferSize = Integer.parseInt(sBufferSize);
        } catch (NumberFormatException ignore) {
            //no op
        }
        // Get encoding for the logging file
        String encoding = getProperty(className + ".encoding", null);
        if (encoding != null && encoding.length() > 0) {
            try {
                setEncoding(encoding);
            } catch (UnsupportedEncodingException ex) {
                // Ignore
            }
        }

        // Get logging level for the handler
        setLevel(Level.parse(getProperty(className + ".level", "" + Level.ALL)));

        // Get filter configuration
        String filterName = getProperty(className + ".filter", null);
        if (filterName != null) {
            try {
                setFilter((Filter) cl.loadClass(filterName).newInstance());
            } catch (Exception e) {
                // Ignore
            }
        }

        // Set formatter
        String formatterName = getProperty(className + ".formatter", null);
        if (formatterName != null) {
            try {
                setFormatter((Formatter) cl.loadClass(formatterName).newInstance());
            } catch (Exception e) {
                // Ignore
            }
        } else {
            setFormatter(new SimpleFormatter());
        }
        
        // Set error manager
        setErrorManager(new ErrorManager());
        
    }

    
    private String getProperty(String name, String defaultValue) {
        String value = LogManager.getLogManager().getProperty(name);
        if (value == null) {
            value = defaultValue;
        } else {
            value = value.trim();
        }
        return value;
    }
    
    
    /**
     * Open the new log file for the date specified by <code>date</code>.
     */
    protected void open() {
        openWriter();
    }
    
    protected void openWriter() {

        // Create the directory if necessary
        File dir = new File(directory);
        dir.mkdirs();

        // Open the current log file
        try {
            String pathname = dir.getAbsolutePath() + File.separator +
                prefix + date + suffix;
            String encoding = getEncoding();
            FileOutputStream fos = new FileOutputStream(pathname, true);
            OutputStream os = bufferSize>0?new BufferedOutputStream(fos,bufferSize):fos;
            writer = new PrintWriter(
                    (encoding != null) ? new OutputStreamWriter(os, encoding)
                                       : new OutputStreamWriter(os), false);
            writer.write(getFormatter().getHead(this));
        } catch (Exception e) {
            reportError(null, e, ErrorManager.OPEN_FAILURE);
            writer = null;
        }

    }


}
