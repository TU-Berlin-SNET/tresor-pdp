package org.snet.tresor.pdp.contexthandler.handler;

import java.io.Reader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.snet.tresor.pdp.Configuration;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.contexthandler.servlet.ServletConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.PDP;
import org.wso2.balana.ctx.AbstractRequestCtx;
import org.wso2.balana.ctx.RequestCtxFactory;

/**
 * Handler for requests to the pdp
 * @author malik
 */
public class PDPHandler implements Handler {
	private static Log log = LogFactory.getLog(PDPHandler.class);
	private ParserPool parser;
	private PDP pdp;
	
	public PDPHandler() {
		this.pdp = Configuration.getInstance().getPDP();
		this.parser = new BasicParserPool();
	}
	
	public JSONObject handle(HttpServletRequest request, HttpServletResponse response) {		
		String contenttype = request.getContentType();		
		
		// check whether the content type is supported
		if (!this.isSupported(contenttype)) {
			return Helper.createResponseJSON(true, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
							ServletConstants.CONTENTTYPE_TEXTPLAIN,
							"Accepted Content-types: application/xacml+xml, application/samlassertion+xml");
		}
		
		String decision = null;
		if (contenttype.startsWith(ServletConstants.CONTENTTYPE_XACMLSAML))
			decision = this.handleSAML(request);
		
		if (contenttype.startsWith(ServletConstants.CONTENTTYPE_XACMLXML))
			decision = this.handleXACML(request);
		
		JSONObject responseJSON = null;
		
		if (decision != null)
			responseJSON = Helper.createResponseJSON(false, HttpServletResponse.SC_OK, contenttype, decision);
		else
			responseJSON = Helper.createResponseJSON(true, HttpServletResponse.SC_BAD_REQUEST);
		
		return responseJSON;
	}

	/**
	 * Try to parse assumed XACML request in request body, get and return decision
	 * @param request the httpservlet request
	 * @return the decision as a string or null
	 */
	private String handleXACML(HttpServletRequest request) {
		String decision = null;
		AbstractRequestCtx xacmlRequest = null;

		try {
			Reader body = Helper.getRequestInputStreamReader(request);
			Document xacmlDoc = parser.parse(body);
			
			xacmlRequest = RequestCtxFactory.getFactory().getRequestCtx(xacmlDoc.getDocumentElement());
		} catch (Exception e) { log.info("Error creating request context"); }
		
		if (xacmlRequest != null) {
			decision = this.pdp.evaluate(xacmlRequest).encode();
		}
		
		return decision;
	}

	private String handleSAML(HttpServletRequest request) {
                String decision = null;
                AbstractRequestCtx xacmlRequest = null;
                SAMLHandler samlHandler = null;
                
                try {
                        samlHandler = new SAMLHandler();
			Reader body = Helper.getRequestInputStreamReader(request);
			Document samlXACMLDoc = parser.parse(body);
                        Element xacmlReq = samlHandler.handleRequest(samlXACMLDoc.getDocumentElement());
                                
			xacmlRequest = RequestCtxFactory.getFactory().getRequestCtx(xacmlReq);
		} catch (Exception e) { log.error("Error creating request context from SAML request", e); }
                
                if (xacmlRequest != null) {
                    try {
                        decision = samlHandler.handleResponse(this.pdp.evaluate(xacmlRequest).encode());
                    } catch (Exception ex) {
                        log.error("Error getting a SAML response", ex);
                    }
		}
                
                return decision;
	}
	
	private boolean isSupported(String contenttype) {		
		return contenttype != null && (
				contenttype.toLowerCase().startsWith(ServletConstants.CONTENTTYPE_XACMLSAML) ||
				contenttype.toLowerCase().startsWith(ServletConstants.CONTENTTYPE_XACMLXML)  ||
                                contenttype.toLowerCase().startsWith(ServletConstants.CONTENTTYPE_XML)
			);		
	}

}
