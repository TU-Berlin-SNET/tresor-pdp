package org.snet.contexthandler;

import java.io.InputStream;
import java.io.Reader;

import org.opensaml.xacml.profile.saml.SAMLProfileConstants;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.snet.saml.SAMLHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.PDP;
import org.wso2.balana.XACMLConstants;

/**
 * Simple Class for handling incoming requests and providing them to the appropiate handler
 */
public class ContextHandler {	
	
	ParserPool parserPool;
	PDP pdp;
	
	public ContextHandler(ParserPool parserPool, PDP pdp) {
		this.parserPool = parserPool;
		this.pdp = pdp;
	}
	
	/**
	 * Checks context of requests that come in (i.e. whether they are xacml or xacml-samlp)
	 * and subsequently provides them to the appropiate handler.
	 * @param reader, request.getReader()
	 * @return the response from the handler or null if an error happened
	 */
	public String handle(Reader reader) {
        //public String handle(InputStream reader) {
		String response = null;
		Document doc = null;
		Element elem = null;

		try { 
			doc = this.parserPool.parse(reader);
			elem = doc.getDocumentElement();
		} catch (Exception e) { e.printStackTrace(); }
		
		if (elem != null) {
			// if elem == xacml 2 OR 3
			if (elem.getNamespaceURI() == XACMLConstants.REQUEST_CONTEXT_2_0_IDENTIFIER ||
					elem.getNamespaceURI() == XACMLConstants.REQUEST_CONTEXT_3_0_IDENTIFIER) {
				response = XACMLHandler.handle(elem, pdp);
			}
			// if elem == xacml-samlp
			if (elem.getNamespaceURI() == SAMLProfileConstants.SAML20XACML20P_NS ||
					elem.getNamespaceURI() == SAMLProfileConstants.SAML20XACML30P_NS) {
				
				try {
					SAMLHandler samlHandler = new SAMLHandler();
					String request, s;
                    request = samlHandler.handleRequest(elem);
                    s = XACMLHandler.handle(request, this.pdp);
                    response = samlHandler.handleResponse(s);
				} catch (Exception e) { e.printStackTrace(); }
				
			}
		}
		return response;
	}
	
	public void setPDP(PDP pdp) {
		this.pdp = pdp;
	}

}
