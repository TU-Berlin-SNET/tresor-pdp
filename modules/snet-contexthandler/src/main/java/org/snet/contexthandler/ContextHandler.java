package org.snet.contexthandler;

import java.io.Reader;

import org.opensaml.xacml.profile.saml.SAMLProfileConstants;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.snet.saml.SAMLHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.PDP;
import org.wso2.balana.XACMLConstants;

public class ContextHandler {	
	
	ParserPool parserPool;
	PDP pdp;
	
	public ContextHandler(ParserPool parserPool, PDP pdp) {
		this.parserPool = parserPool;
		this.pdp = pdp;
	}
	
	public String handle(Reader reader) {
		String response = null;
		Document doc = null;
		Element elem = null;

		try { 
			doc = this.parserPool.parse(reader);
			elem = doc.getDocumentElement();
		}
		catch (Exception e) { }
		
		if (elem != null) {
			// if elem == xacml 2 OR 3
			if (elem.getNamespaceURI() == XACMLConstants.XACML_2_0_IDENTIFIER ||
					elem.getNamespaceURI() == XACMLConstants.XACML_3_0_IDENTIFIER) {
				response = XACMLHandler.handle(elem, pdp);
			}
			// if elem == xacml-saml 2 OR 3 (xacmlauthzdecisionquery)
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

}
