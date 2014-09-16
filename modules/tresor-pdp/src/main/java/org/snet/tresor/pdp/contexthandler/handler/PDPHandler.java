package org.snet.tresor.pdp.contexthandler.handler;

import java.io.Reader;

import org.apache.log4j.MDC;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.contexthandler.servlet.ServletConstants;
import org.snet.tresor.pdp.finder.impl.FinderConstants;
import org.w3c.dom.Element;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.ParsingException;
import org.wso2.balana.ctx.AbstractResult;
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

	/**
	 * Create a new PDPHandler for interfacing with given pdp
	 * @param pdp the pdp
	 * @param pdpConfig the configuration of the pdp
	 */
	public PDPHandler(PDP pdp, PDPConfig pdpConfig) {
		this.pdp = pdp;
		this.pdpConfig = pdpConfig;
		this.parser = new BasicParserPool();
		this.reqFactory = RequestCtxFactory.getFactory();
		this.evalFactory = EvaluationCtxFactory.getFactory();
	}

	/**
	 * Retrieve a decision for given request in reader
	 * @param contenttype the contenttype of the request, either application/xacml+xml or application/samlassertion+xml
	 * @param reader reader object containing the request body
	 * @return the decision as a string
	 * @throws XMLParserException if XML of request is invalid
	 * @throws ParsingException if XACML of request is invalid
	 * @throws Exception
	 */
	public String retrieveDecisionFor(String contenttype, Reader reader) throws XMLParserException, ParsingException, Exception {
		Element elem = this.parser.parse(reader).getDocumentElement();

		String decision = null;

		if (contenttype.contains(ServletConstants.CONTENTTYPE_XACML))
			decision = this.retrieveDecisionForXACML(elem);

		if (contenttype.contains(ServletConstants.CONTENTTYPE_XACMLSAML))
			decision = this.retrieveDecisionForXACMLSAML(elem);

		return decision;
	}

	/**
	 * Retrieve a decision for given XACML request
	 * @param elem root element of XACML request
	 * @return the decision as a string
	 * @throws ParsingException if XACML of request is invalid
	 */
	public String retrieveDecisionForXACML(Element elem) throws ParsingException {
		// create EvaluationContext
		EvaluationCtx evalCtx = this.evalFactory.getEvaluationCtx(this.reqFactory.getRequestCtx(elem), pdpConfig);
		log.debug("Retrieved EvaluationContext successfully");

		// extract subject-id, client-id from request and add to MDC for logging purposes
		String subject = Helper.getAttributeAsString(
				FinderConstants.DATATYPE_STRING_URI,
				FinderConstants.ID_SUBJECT_URI, null,
				FinderConstants.CATEGORY_SUBJECT_URI, evalCtx);

		String client = Helper.getAttributeAsString(
				FinderConstants.DATATYPE_STRING_URI,
				FinderConstants.ID_DOMAIN_URI, null,
				FinderConstants.CATEGORY_RESOURCE_URI, evalCtx);

		String service = Helper.getAttributeAsString(
				FinderConstants.DATATYPE_STRING_URI,
				FinderConstants.ID_SERVICE_URI, null,
				FinderConstants.CATEGORY_RESOURCE_URI, evalCtx);

		MDC.put("subject-id", subject);
		MDC.put("client-id", client);
		log.info("Subject {} is requesting access to service {} of client {}", subject, service, client);

		// get decision
		ResponseCtx response = this.pdp.evaluate(evalCtx);

		// TODO do other stuff with response, e.g. logging
		for (AbstractResult result : response.getResults())
			log.info("Decision for access to service {} is {}", service, AbstractResult.DECISIONS[result.getDecision()]);

		return response.encode();
	}

	/**
	 * Retrieve a decision for given XACML-SAML request
	 * @param elem root element of XACML-SAML request
	 * @return the decision as a string
	 * @throws ParsingException if XACML of request is invalid
	 * @throws Exception
	 */
	public String retrieveDecisionForXACMLSAML(Element elem) throws ParsingException, Exception {
		SAMLHandler samlHandler = new SAMLHandler();
		Element req = samlHandler.handleRequest(elem);
		String response = this.retrieveDecisionForXACML(req);
		return samlHandler.handleResponse(response);
	}

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

}
