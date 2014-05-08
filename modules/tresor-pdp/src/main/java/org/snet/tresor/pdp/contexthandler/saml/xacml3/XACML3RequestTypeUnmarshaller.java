package org.snet.saml.xacml3;

import org.opensaml.xacml.XACMLConstants;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.w3c.dom.Element;

/**
 * Simple unmarshaller for XACML3 requests
 * @author malik
 */
public class XACML3RequestTypeUnmarshaller implements Unmarshaller {
	
	public XACML3RequestTypeUnmarshaller() {
		super();
	}

	/**
	 * Basically wraps the given element into a XACML3RequestType object
	 */
	public XMLObject unmarshall(Element element) throws UnmarshallingException {
		return new XACML3RequestType(element.getNamespaceURI(), 
									  element.getLocalName(), 
									  XACMLConstants.XACMLCONTEXT_PREFIX, 
									  element);
	}
	
}
