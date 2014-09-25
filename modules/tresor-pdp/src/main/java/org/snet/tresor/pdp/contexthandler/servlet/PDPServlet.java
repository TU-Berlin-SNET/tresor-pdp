package org.snet.tresor.pdp.contexthandler.servlet;

import java.io.IOException;
import java.io.Reader;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.xml.parse.XMLParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.contexthandler.ContextHandler;
import org.snet.tresor.pdp.contexthandler.handler.PDPHandler;
import org.wso2.balana.ParsingException;

/**
 * A simple Servlet which responds to POST-requests as is specified in
 * the "REST Profile of XACML v3.0" specification
 * @author malik
 */
public class PDPServlet extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(PDPServlet.class);
	private String acceptContentTypes = "Accepted Content-Types: " + ServletConstants.CONTENTTYPE_XACML
			+ ", " + ServletConstants.CONTENTTYPE_XACMLSAML;

	private PDPHandler pdpHandler;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		MDC.clear();
		MDC.put("tresor-component", "PDP");
		MDC.put("category", "Request Validation");

		// check if contenttype is invalid
		String contenttype = request.getContentType().toLowerCase();
		if (contenttype.contains(ServletConstants.CONTENTTYPE_XACML))
			MDC.put("category", "XACML decision request");
		else if (contenttype.contains(ServletConstants.CONTENTTYPE_XACMLSAML))
			MDC.put("category", "XACML-SAML decision request");
		else {
			log.info("Rejected request because of unsupported content of type {}", contenttype);
			// respond with http 415 and return
			Helper.respondHTTP(true, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, ServletConstants.CONTENTTYPE_TEXTPLAIN,
					this.acceptContentTypes, response);
			return;
		}

		log.debug("Accepted valid POST request with contenttype {}", contenttype);

		// go on with processing
		String decision = null;
		Reader reader = null;
		try {
			reader = Helper.getRequestInputStreamReader(request);
			decision = this.pdpHandler.retrieveDecisionFor(contenttype, reader);
			// no errors? respond with decision
			Helper.respondHTTP(false, HttpServletResponse.SC_OK, contenttype, decision, response);
		} catch (IOException e) {
			// TODO return http error 400
			log.warn("Rejected request due to failure while retrieving input", e);
			Helper.respondHTTP(true, HttpServletResponse.SC_BAD_REQUEST, response);
		} catch (XMLParserException e) {
			// TODO return http error 400
			log.info("Rejected request due to malformed XML", e);
			Helper.respondHTTP(true, HttpServletResponse.SC_BAD_REQUEST, response);
		} catch (ParsingException e) {
			// TODO return http error 422 Unprocessable entity, indicates semantic errors in xacml
			log.info("Rejected request due to malformed XACML", e);
			Helper.respondHTTP(true, ServletConstants.SC_UNPROCESSABLE_ENTITY, response);
		} catch (Exception e) {
			// TODO return http error 500
			log.error("Rejected request due to an unexpected error while processing", e);
			Helper.respondHTTP(true, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
		} finally {
			try { reader.close(); }
			catch (IOException e) {}
			Helper.clearPDPCaches();
		}

	}

	@Override
	public void init() {
		this.pdpHandler = ContextHandler.getInstance().getPDPHandler();
		log.info("PDPServlet initialized");
	}

}
