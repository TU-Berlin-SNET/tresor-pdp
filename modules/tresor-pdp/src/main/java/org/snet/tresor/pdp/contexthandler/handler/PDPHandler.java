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
		} catch (Exception e) { log.error("Error creating request context", e); }
		
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

}
