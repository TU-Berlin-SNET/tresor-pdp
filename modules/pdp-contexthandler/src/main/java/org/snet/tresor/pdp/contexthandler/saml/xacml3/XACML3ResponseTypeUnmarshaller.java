package org.snet.tresor.pdp.contexthandler.saml.xacml3;

import org.opensaml.xacml.XACMLConstants;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.w3c.dom.Element;

/**
 * Simple Unmarshaller for XACML3 Responses
 */
public class XACML3ResponseTypeUnmarshaller implements Unmarshaller {

	public XACML3ResponseTypeUnmarshaller() {
		super();
	}

	/**
	 * Basically wraps the given Element into a XACML3ResponseType object
	 */
	public XMLObject unmarshall(Element element) throws UnmarshallingException {
		return new XACML3ResponseType(element.getNamespaceURI(),
									  element.getLocalName(),
									  XACMLConstants.XACMLCONTEXT_PREFIX,
									  element);
	}

}
