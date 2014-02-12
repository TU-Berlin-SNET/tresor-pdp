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
	
	public String handle(Reader reader) throws Exception {
		String response = null;
                String request = null;
		Document doc = null;
		Element elem = null;

		try { doc = this.parserPool.parse(reader);	}
		catch (XMLParserException e) { }
		
		elem = doc.getDocumentElement();
		
		if (elem != null) {
			// if elem == xacml
			if (elem.getNamespaceURI() == XACMLConstants.XACML_3_0_IDENTIFIER) {
				// response = this.pdp.evaluate(elem converted to String)
			}
			// if elem == xacml-saml 2 OR 3 (xacmlauthzdecisionquery)
			if (elem.getNamespaceURI() == SAMLProfileConstants.SAML20XACML20P_NS ||
					elem.getNamespaceURI() == SAMLProfileConstants.SAML20XACML30P_NS) {
                            
                            SAMLHandler samlHandler = new SAMLHandler();
                            
                            request = samlHandler.handleRequest(elem);
                            // response = this.pdp.evaluate(elem converted to String)
                            
                            response = samlHandler.handleResponse(response);
			}
		}
		
		return response;
	}

}
