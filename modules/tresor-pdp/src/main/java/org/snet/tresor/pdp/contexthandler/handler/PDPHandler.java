package org.snet.tresor.pdp.contexthandler.handler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.MDC;
import org.json.JSONObject;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.contexthandler.servlet.ServletConstants;
import org.snet.tresor.pdp.finder.impl.FinderConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.ParsingException;
import org.wso2.balana.ctx.AbstractRequestCtx;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.ctx.EvaluationCtxFactory;
import org.wso2.balana.ctx.RequestCtxFactory;
import org.wso2.balana.ctx.ResponseCtx;

/**
 * Handler for requests to the pdp
 * @author malik
 */
public class PDPHandler {	
	private static final Logger log = LoggerFactory.getLogger(PDPHandler.class);
	
	private PDP pdp;
	private PDPConfig pdpConfig;
	private ParserPool parser;
	private RequestCtxFactory reqFactory;
	private EvaluationCtxFactory evalFactory;
	
	
	public PDPHandler(PDP pdp, PDPConfig pdpConfig) {		
		this.pdp = pdp;
		this.pdpConfig = pdpConfig;
		this.parser = new BasicParserPool();
		this.reqFactory = RequestCtxFactory.getFactory();
		this.evalFactory = EvaluationCtxFactory.getFactory();
	}
	
	public String retrieveDecisionFor(String contenttype, Reader reader) throws XMLParserException, ParsingException, Exception {		
		Element elem = this.parser.parse(reader).getDocumentElement();
		
		String decision = null;
		
		if (contenttype.contains(ServletConstants.CONTENTTYPE_XACML))
			decision = this.retrieveDecisionForXACML(elem);
		
		if (contenttype.contains(ServletConstants.CONTENTTYPE_XACMLSAML))
			decision = this.retrieveDecisionForXACMLSAML(elem);
		
		return decision;
	}
	
	public String retrieveDecisionForXACML(Element elem) throws ParsingException {
		// create EvaluationContext
		EvaluationCtx evalCtx = this.evalFactory.getEvaluationCtx(this.reqFactory.getRequestCtx(elem), pdpConfig);
		
		// extract subject, client from request and add to MDC for logging purposes
		String subject = Helper.getAttributeAsString(
				FinderConstants.DATATYPE_STRING_URI,
				FinderConstants.ID_SUBJECT_URI, null,
				FinderConstants.CATEGORY_SUBJECT_URI, evalCtx);
		MDC.put("subject-id", subject);
		
		String client = Helper.getAttributeAsString(
				FinderConstants.DATATYPE_STRING_URI,
				FinderConstants.ID_DOMAIN_URI, null,
				FinderConstants.CATEGORY_RESOURCE_URI, evalCtx);
		MDC.put("client-id", client);
		
		// get decision
		ResponseCtx response = this.pdp.evaluate(evalCtx);
		
		// TODO do other stuff with response, e.g. logging
		
		return response.encode();
	}
	
	public String retrieveDecisionForXACMLSAML(Element elem) throws ParsingException, Exception {
		SAMLHandler samlHandler = new SAMLHandler();
		Element req = samlHandler.handleRequest(elem);
		String response = this.retrieveDecisionForXACML(req);
		return samlHandler.handleResponse(response);
	}
	
//	public JSONObject handle(HttpServletRequest request, HttpServletResponse response) {
//		String contenttype = request.getContentType();
//		
//		// check whether the content type is supported
//		if (!this.isSupported(contenttype)) {
//			return Helper.createResponseJSON(true, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
//							ServletConstants.CONTENTTYPE_TEXTPLAIN,
//							"Accepted Content-types: application/xacml+xml, application/samlassertion+xml");
//		}
//		
//		String decision = null;
//		if (contenttype.startsWith(ServletConstants.CONTENTTYPE_XACMLSAML))
//			decision = this.handleSAML(request);
//		
//		if (contenttype.startsWith(ServletConstants.CONTENTTYPE_XACMLXML))
//			decision = this.handleXACML(request);
//		
//		JSONObject responseJSON = null;
//		
//		if (decision != null)
//			responseJSON = Helper.createResponseJSON(false, HttpServletResponse.SC_OK, contenttype, decision);
//		else
//			responseJSON = Helper.createResponseJSON(true, HttpServletResponse.SC_BAD_REQUEST);
//		
//		return responseJSON;
//	}

//	/**
//	 * Try to parse assumed XACML request in request body, get and return decision
//	 * @param request the httpservlet request
//	 * @return the decision as a string or null
//	 */
//	private String handleXACML(HttpServletRequest request) {
//		String decision = null;
//		AbstractRequestCtx xacmlRequest = null;
//
//		try {
//			Reader body = Helper.getRequestInputStreamReader(request);
//			Document xacmlDoc = parser.parse(body);
//			
//			xacmlRequest = RequestCtxFactory.getFactory().getRequestCtx(xacmlDoc.getDocumentElement());
//		} catch (Exception e) {
//			log.error("Error creating request context");
//			log.error("", e);
//		}
//		
//		if (xacmlRequest != null) {
//			decision = this.pdp.evaluate(xacmlRequest).encode();
//		}
//		
//		return decision;
//	}
//
//	private String handleSAML(HttpServletRequest request) {
//                String decision = null;
//                AbstractRequestCtx xacmlRequest = null;
//                SAMLHandler samlHandler = null;
//                
//                try {
//                        samlHandler = new SAMLHandler();
//			Reader body = Helper.getRequestInputStreamReader(request);
//			Document samlXACMLDoc = parser.parse(body);
//                        Element xacmlReq = samlHandler.handleRequest(samlXACMLDoc.getDocumentElement());
//                                
//			xacmlRequest = RequestCtxFactory.getFactory().getRequestCtx(xacmlReq);
//		} catch (Exception e) { log.error("Error creating request context from SAML request", e); }
//                
//                if (xacmlRequest != null) {
//                    try {
//                        decision = samlHandler.handleResponse(this.pdp.evaluate(xacmlRequest).encode());
//                    } catch (Exception ex) {
//                        log.error("Error getting a SAML response", ex);
//                    }
//		}
//                
//                return decision;
//	}
//	
//	private boolean isSupported(String contenttype) {		
//		return contenttype != null && (
//				contenttype.toLowerCase().startsWith(ServletConstants.CONTENTTYPE_XACMLSAML) ||
//				contenttype.toLowerCase().startsWith(ServletConstants.CONTENTTYPE_XACMLXML)  ||
//                                contenttype.toLowerCase().startsWith(ServletConstants.CONTENTTYPE_XML)
//			);		
//	}

}
