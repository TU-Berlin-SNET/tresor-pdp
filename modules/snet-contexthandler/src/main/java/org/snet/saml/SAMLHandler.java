package org.snet.saml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import org.opensaml.xml.ConfigurationException;
import org.w3c.dom.Element;

/**
 *
 * @author zequeira
 */
public class SAMLHandler {
    
    SAMLUtility samlUtility = null;
    
        public SAMLHandler() throws ConfigurationException, NoSuchAlgorithmException, NoSuchProviderException, IOException, FileNotFoundException, InvalidKeySpecException {
                samlUtility = new SAMLUtility();
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
