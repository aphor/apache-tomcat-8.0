package org.apache.tomcat.util.xml.parser;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.xml.WebXml;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public class WebXmlParser {
    private static final Log log = LogFactory.getLog( WebXmlParser.class );
    
    /**
     * The string resources for this package.
     */
    protected static final StringManager sm =
        StringManager.getManager(Constants.Package);

    /**
     * The <code>Digester</code> we will use to process web application
     * deployment descriptor files.
     */
    protected Digester webDigester = null;
    protected WebRuleSet webRuleSet = null;

    /**
     * The <code>Digester</code> we will use to process web fragment
     * deployment descriptor files.
     */
    protected Digester webFragmentDigester = null;
    protected WebRuleSet webFragmentRuleSet = null;

    public WebXmlParser(boolean namespaceAware, boolean validation) {
    	createWebXmlDigester(namespaceAware, validation);
    }
    
    /**
     * Create and return a Digester configured to process the
     * web application deployment descriptor (web.xml).
     */
    public void createWebXmlDigester(boolean namespaceAware,
            boolean validation) {

        webRuleSet = new WebRuleSet(false);
        webDigester = DigesterFactory.newDigester(validation,
                namespaceAware, webRuleSet);
        webDigester.getParser();

        webFragmentRuleSet = new WebRuleSet(true);
        webFragmentDigester = DigesterFactory.newDigester(validation,
                namespaceAware, webFragmentRuleSet);
        webFragmentDigester.getParser();
    }

    public boolean parseWebXml(InputSource source, WebXml dest,
            boolean fragment) {

        boolean ok = true;
    	
    	if (source == null) {
            return ok;
        }

        XmlErrorHandler handler = new XmlErrorHandler();

        Digester digester;
        WebRuleSet ruleSet;
        if (fragment) {
            digester = webFragmentDigester;
            ruleSet = webFragmentRuleSet;
        } else {
            digester = webDigester;
            ruleSet = webRuleSet;
        }

        digester.push(dest);
        digester.setErrorHandler(handler);

        if(log.isDebugEnabled()) {
            log.debug(sm.getString("webXmlParser.applicationStart",
                    source.getSystemId()));
        }

        try {
            digester.parse(source);

            if (handler.getWarnings().size() > 0 ||
                    handler.getErrors().size() > 0) {
                ok = false;
                handler.logFindings(log, source.getSystemId());
            }
        } catch (SAXParseException e) {
            log.error(sm.getString("webXmlParser.applicationParse",
                    source.getSystemId()), e);
            log.error(sm.getString("webXmlParser.applicationPosition",
                             "" + e.getLineNumber(),
                             "" + e.getColumnNumber()));
            ok = false;
        } catch (Exception e) {
            log.error(sm.getString("webXmlParser.applicationParse",
                    source.getSystemId()), e);
            ok = false;
        } finally {
            digester.reset();
            ruleSet.recycle();
        }
        
        return ok;
    }

}
