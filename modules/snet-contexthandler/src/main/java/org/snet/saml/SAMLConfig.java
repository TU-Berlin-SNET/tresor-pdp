package org.snet.saml;

import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.snet.saml.xacml3.XACML3GenericMarshaller;
import org.snet.saml.xacml3.XACML3RequestType;
import org.snet.saml.xacml3.XACML3RequestTypeUnmarshaller;
import org.snet.saml.xacml3.XACML3ResponseType;
import org.snet.saml.xacml3.XACML3ResponseTypeUnmarshaller;

/**
 * Utility Class for SAML-related methods
 * @author malik
 */
public class SAMLConfig {

	/**
	 * Initializes SAML support
	 * @throws ConfigurationException 
	 */
	public static void InitSAML() throws ConfigurationException {
		// Bootstrap OpenSAML
		DefaultBootstrap.bootstrap();
		
		// Add XACML3 Support
		Configuration.getMarshallerFactory().registerMarshaller(XACML3RequestType.DEFAULT_ELEMENT_NAME,
				new XACML3GenericMarshaller());
		Configuration.getMarshallerFactory().registerMarshaller(XACML3ResponseType.DEFAULT_ELEMENT_NAME, 
				new XACML3GenericMarshaller());
		Configuration.getUnmarshallerFactory().registerUnmarshaller(XACML3RequestType.DEFAULT_ELEMENT_NAME,
				new XACML3RequestTypeUnmarshaller());
		Configuration.getUnmarshallerFactory().registerUnmarshaller(XACML3ResponseType.DEFAULT_ELEMENT_NAME,
				new XACML3ResponseTypeUnmarshaller());
		
	}

}
