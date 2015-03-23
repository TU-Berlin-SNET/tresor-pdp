package org.snet.tresor.pdp.contexthandler.saml.xacml3;

import java.util.List;
import javax.xml.namespace.QName;

import org.opensaml.xacml.XACMLConstants;
import org.opensaml.xacml.ctx.ResponseType;
import org.opensaml.xacml.ctx.ResultType;
import org.opensaml.xacml.impl.AbstractXACMLObject;
import org.opensaml.xml.XMLObject;
import org.w3c.dom.Element;

/**
 * Basic representation of a XACML3 response
 */
public class XACML3ResponseType extends AbstractXACMLObject implements ResponseType {

	public static final String DEFAULT_ELEMENT_LOCAL_NAME = "Response";

	public static final QName DEFAULT_ELEMENT_NAME = new QName(
			"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17",
			DEFAULT_ELEMENT_LOCAL_NAME,
			XACMLConstants.XACMLCONTEXT_PREFIX);

	public static final String TYPE_LOCAL_NAME = "ResponseType";

	public static final QName TYPE_NAME = new QName(
			"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17",
			XACMLConstants.XACMLCONTEXT_PREFIX);

    protected XACML3ResponseType(String namespaceURI, String elementLocalName,
			String namespacePrefix) {
		super(namespaceURI, elementLocalName, namespacePrefix);
	}

    public XACML3ResponseType(String namespaceURI, String elementLocalName,
			String namespacePrefix, Element element) {
		super(namespaceURI, elementLocalName, namespacePrefix);
		this.setDOM(element);
	}

	public List<ResultType> getResults() {
		return null;
	}

	public List<XMLObject> getOrderedChildren() {
		return null;
	}

	public ResultType getResult() {
		return null;
	}

	public void setResult(ResultType newResult) { }

}
