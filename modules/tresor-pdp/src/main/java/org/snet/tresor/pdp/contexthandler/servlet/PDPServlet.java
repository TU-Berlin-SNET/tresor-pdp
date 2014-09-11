package org.snet.tresor.pdp.contexthandler.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.contexthandler.ContextHandler;
import org.snet.tresor.pdp.contexthandler.handler.Handler;
import org.snet.tresor.pdp.contexthandler.handler.PDPHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
		MDC.put("category", "PDP");
		
		// check if contenttype is invalid
		String contenttype = request.getContentType();		
		if (!(contenttype.contains(ServletConstants.CONTENTTYPE_XACML) || contenttype.contains(ServletConstants.CONTENTTYPE_XACMLSAML))) {
			// respond with http 415 and return
			Helper.respondHTTP(true, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, ServletConstants.CONTENTTYPE_TEXTPLAIN, 
					this.acceptContentTypes, response);			
			return;
		}
		
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
			Helper.respondHTTP(true, HttpServletResponse.SC_BAD_REQUEST, response);
		} catch (XMLParserException e) {
			// TODO return http error 400
			Helper.respondHTTP(true, HttpServletResponse.SC_BAD_REQUEST, response);
		} catch (ParsingException e) {
			// TODO return http error 422 Unprocessable entity, indicates semantic errors in xacml
			Helper.respondHTTP(true, ServletConstants.SC_UNPROCESSABLE_ENTITY, response);
		} catch (Exception e) {
			// TODO return http error 500
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
	}
	
}
