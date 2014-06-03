package org.snet.tresor.pdp.contexthandler.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.snet.tresor.pdp.TresorPDP;
import org.snet.tresor.pdp.contexthandler.servlet.ServletConstants;
import org.w3c.dom.Document;
import org.wso2.balana.PDP;
import org.wso2.balana.ctx.AbstractRequestCtx;
import org.wso2.balana.ctx.RequestCtxFactory;

/**
 * Handler for requests to the pdp
 * @author malik
 *
 */
public class PDPHandler implements Handler {
	private static Log log = LogFactory.getLog(PDPHandler.class);	
	private ParserPool parser;
	private PDP pdp;
	
	public PDPHandler() {
		// TODO remove
		log.info("pdp handler loader");
		this.pdp = TresorPDP.getInstance().getPDP();
		this.parser = new BasicParserPool();		
	}
	
	public JSONObject handle(HttpServletRequest request,
			HttpServletResponse response, String httpMethod) {
		JSONObject responseJSON = null;
		
		if (httpMethod == ServletConstants.HTTP_POST) {
			// TODO remove
			log.info("handling pdp post event");
			String contenttype = request.getHeader(ServletConstants.HEADER_CONTENTTYPE);
			boolean evaluated = false;
			String decision = null;
						
			if (contenttype.equals(ServletConstants.CONTENTTYPE_XACMLSAML)) {
				// TODO remove
				log.info("handling saml");
				decision = this.handleSAML(request);
				evaluated = true;
			}								
							
			if (contenttype.equals(ServletConstants.CONTENTTYPE_XACMLXML)) {
				// TODO remove
				log.info("handling xacml");
				decision = this.handleXACML(request);
				evaluated = true;
			}
							
			if (decision != null) {
				// TODO remove
				log.info("decision reached");
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, false)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_OK)
								.put(KEYJSON_CONTENTTYPE, contenttype)
								.put(KEYJSON_CONTENT, decision);				
			} else {
				// TODO remove
				log.info("no decision reached");
				if (evaluated) {
					responseJSON = new JSONObject()
					.put(KEYJSON_ERROR, true)
					.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} else {
					responseJSON = new JSONObject()
									.put(KEYJSON_ERROR, true)
									.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_BAD_REQUEST);
				}
			}
			
		} else {
			responseJSON = new JSONObject()
							.put(KEYJSON_ERROR, true)
							.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		}
		
		return responseJSON;
	}

	private String handleXACML(HttpServletRequest request) {
		String decision = null;		
		AbstractRequestCtx xacmlRequest = null;

		try {
			Document xacmlDoc = parser.parse(request.getInputStream());
			xacmlRequest = RequestCtxFactory.getFactory().getRequestCtx(xacmlDoc.getDocumentElement());
		} catch (Exception e) { log.error("Error creating request context"); }
		
		if (xacmlRequest != null) {
			decision = this.pdp.evaluate(xacmlRequest).encode();
		}
		
		return decision;
	}

	private String handleSAML(HttpServletRequest request) {
		// TODO authentification, authorization, verification
		// TODO unpack xacml
		// TODO get decision via pdp
		// TODO pack xacml into xacml-saml
		// TODO convert to string and return
		log.fatal("SAML handling not implemented");
		return null;
	}

}
