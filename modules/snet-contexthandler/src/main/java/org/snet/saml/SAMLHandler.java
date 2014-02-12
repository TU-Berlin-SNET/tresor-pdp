package org.snet.saml;

import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.parse.ParserPool;
import org.w3c.dom.Element;

/**
 *
 * @author zequeira
 */
public class SAMLHandler {
    
    SAMLUtility samlUtility = null;
    
        public SAMLHandler() throws ConfigurationException {
                //SAMLConfig.InitSAML();
                this.samlUtility = new SAMLUtility();
        }

        public String handleRequest(Element element) throws ConfigurationException, Exception {
            
            String Request = samlUtility.XACMLAuthzDecisionQuery2XACMLRequest(element);
            return Request;
        }

        public String handleResponse(String response) throws ConfigurationException, Exception {            
            
            String Response = samlUtility.makeSAMLxacmlResponse(response);
            return Response;
        }    
    
}
