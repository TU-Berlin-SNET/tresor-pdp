package org.snet.tresor.pdp.contexthandler.handler;

import java.io.Reader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.TresorPDP;
import org.snet.tresor.pdp.contexthandler.servlet.ServletConstants;
import org.w3c.dom.Document;
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
		this.pdp = TresorPDP.getInstance().getPDP();
		this.parser = new BasicParserPool();		
	}
	
	public JSONObject handle(HttpServletRequest request,
			HttpServletResponse response, String httpMethod) {
		JSONObject responseJSON = null;
		
		if (httpMethod == ServletConstants.HTTP_POST) {
			String contenttype = request.getContentType().toLowerCase();
			String decision = null;
			
			if (contenttype.startsWith(ServletConstants.CONTENTTYPE_XACMLSAML))
				decision = this.handleSAML(request);
							
			if (contenttype.startsWith(ServletConstants.CONTENTTYPE_XACMLXML))
				decision = this.handleXACML(request);
			
			if (decision != null) {
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, false)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_OK)
								.put(KEYJSON_CONTENTTYPE, contenttype)
								.put(KEYJSON_CONTENT, decision);				
			} else {
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, true)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_BAD_REQUEST);
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
		// TODO authentification, authorization, verification
		// TODO unpack xacml
		// TODO get decision via pdp
		// TODO pack xacml into xacml-saml
		// TODO convert to string and return
		log.fatal("SAML handling not implemented");
		return null;
	}

}
