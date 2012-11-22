package org.apache.tomcat.util.xml.parser;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.xml.WebXml;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Provides web.xml parse capabilities.
 */
public class WebXmlParser {

    private static final Log log = LogFactory.getLog(WebXmlParser.class);

    /**
     * The string resources for this package.
     */
    protected static final StringManager sm = StringManager
            .getManager(Constants.Package);

    /**
     * The <code>Digester</code> we will use to process web application
     * deployment descriptor files.
     */
    protected Digester webDigester = null;

    /**
     * <p>
     * <strong>RuleSet</strong> for processing the contents of a web application
     * deployment descriptor (<code>/WEB-INF/web.xml</code>) resource.
     * </p>
     */
    protected WebRuleSet webRuleSet = null;

    /**
     * The <code>Digester</code> we will use to process web fragment deployment
     * descriptor files.
     */
    protected Digester webFragmentDigester = null;

    /**
     * <p>
     * <strong>RuleSet</strong> for processing the contents of a web application
     * deployment descriptor (<code>/WEB-INF/web.xml</code>) resource.
     * </p>
     */
    protected WebRuleSet webFragmentRuleSet = null;

    /**
     * Constructs <code>WebXMLParser</code> and initializes the
     * <code>Digester</code> and the <code>RuleSet</code> that will be used
     * during processing the content of the web application deployment
     * descriptors.
     * 
     * @param namespaceAware XML namespace validation
     * @param validation XML validation
     */
    public WebXmlParser(boolean namespaceAware, boolean validation) {
        createWebXmlDigester(namespaceAware, validation);
    }

    /**
     * Create and return a Digester configured to process the web application
     * deployment descriptor (web.xml).
     * 
     * @param namespaceAware XML namespace validation
     * @param validation XML validation
     */
    protected void createWebXmlDigester(boolean namespaceAware, boolean validation) {

        this.webRuleSet = new WebRuleSet(false);
        this.webDigester = DigesterFactory.newDigester(validation, namespaceAware,
                this.webRuleSet);
        this.webDigester.getParser();

        this.webFragmentRuleSet = new WebRuleSet(true);
        this.webFragmentDigester = DigesterFactory.newDigester(validation,
                namespaceAware, this.webFragmentRuleSet);
        this.webFragmentDigester.getParser();
    }

    /**
     * Parses the given source and stores the parsed data in the given web.xml
     * representation. Status of the parse operation is provided as a result.
     * Note the status will be successful of the source is <code>NULL</code>
     * because a web application without web deployment descriptors is
     * acceptable.
     * 
     * @param source The source that is going to be parsed
     * @param dest Representation of web.xml where the parsed data will be stored
     * @param fragment Flag that indicates whether the source is the main web.xml or
     *        web-fragment.xml
     * @return Returns the result of the parsing operation - whether it is
     *         successful or not. Note the result will be successful of the
     *         source is <code>NULL</code> because web application without web
     *         deployment descriptors is acceptable.
     */
    public boolean parseWebXml(InputSource source, WebXml dest, boolean fragment) {

        boolean ok = true;

        if (source == null) {
            return ok;
        }

        XmlErrorHandler handler = new XmlErrorHandler();

        Digester digester;
        WebRuleSet ruleSet;
        if (fragment) {
            digester = this.webFragmentDigester;
            ruleSet = this.webFragmentRuleSet;
        } else {
            digester = this.webDigester;
            ruleSet = this.webRuleSet;
        }

        digester.push(dest);
        digester.setErrorHandler(handler);

        if (log.isDebugEnabled()) {
            log.debug(sm.getString("webXmlParser.applicationStart",
                    source.getSystemId()));
        }

        try {
            digester.parse(source);

            if (handler.getWarnings().size() > 0
                    || handler.getErrors().size() > 0) {
                ok = false;
                handler.logFindings(log, source.getSystemId());
            }
        } catch (SAXParseException e) {
            log.error(
                    sm.getString("webXmlParser.applicationParse",
                            source.getSystemId()), e);
            log.error(sm.getString("webXmlParser.applicationPosition",
                    "" + e.getLineNumber(), "" + e.getColumnNumber()));
            ok = false;
        } catch (Exception e) {
            log.error(
                    sm.getString("webXmlParser.applicationParse",
                            source.getSystemId()), e);
            ok = false;
        } finally {
            digester.reset();
            ruleSet.recycle();
        }

        return ok;
    }

}
