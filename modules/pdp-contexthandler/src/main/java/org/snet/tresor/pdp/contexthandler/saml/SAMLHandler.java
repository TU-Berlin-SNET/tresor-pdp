package org.snet.tresor.pdp.contexthandler.saml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import org.opensaml.xml.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/** Class to order the conversion from XACMLAuthzDecisionQuery to a normal XACML Request
 *  and to order the conformation of the XACMl Response into the SAML format.
 *
 * @author zequeira
 */
public class SAMLHandler {    
	private static final Logger log = LoggerFactory.getLogger(SAMLHandler.class);
	
    SAMLUtility samlUtility = null;
    
        public SAMLHandler() throws ConfigurationException, NoSuchAlgorithmException, NoSuchProviderException, IOException, FileNotFoundException, InvalidKeySpecException {
                samlUtility = new SAMLUtility();
        }

        /**
	 * Method to order the conversion from XACMLAuthzDecisionQuery(SAML) to a plane XACML Request
	 * @param element, wich represent the request
	 * @return request, in String format to be evaluated for the PDP
         * @throws org.opensaml.xml.ConfigurationException if any error happen
	 */
        public Element handleRequest(Element element) throws ConfigurationException, Exception {
            
            Element request = samlUtility.XACMLAuthzDecisionQuery2XACMLRequest(element);
            return request;
        }
        
        /**
	 * Method to order the conformation of a proper SAML response
	 * @param response, the xacml response to be converted
	 * @return samlResponse, in String format
         * @throws org.opensaml.xml.ConfigurationException if any error happen
	 */
        public String handleResponse(String response) throws ConfigurationException, Exception {            
            
            String samlResponse = samlUtility.makeSAMLxacmlResponse(response);
            return samlResponse;
        }    
    
}
