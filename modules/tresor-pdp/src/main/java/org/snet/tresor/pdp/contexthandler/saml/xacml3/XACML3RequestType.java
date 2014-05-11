
package org.snet.tresor.pdp.contexthandler.saml.xacml3;

import java.util.List;
import javax.xml.namespace.QName;

import org.opensaml.xacml.XACMLConstants;
import org.opensaml.xacml.ctx.ActionType;
import org.opensaml.xacml.ctx.EnvironmentType;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResourceType;
import org.opensaml.xacml.ctx.SubjectType;
import org.opensaml.xacml.impl.AbstractXACMLObject;
import org.opensaml.xml.XMLObject;
import org.w3c.dom.Element;

/**
 * Basic representation of a XACML3 request
 * @author malik
 *
 */
public class XACML3RequestType extends AbstractXACMLObject implements RequestType {
	
	public static final String DEFAULT_ELEMENT_LOCAL_NAME = "Request";
	
	public static final QName DEFAULT_ELEMENT_NAME = new QName(
			"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17",
			DEFAULT_ELEMENT_LOCAL_NAME,
			XACMLConstants.XACMLCONTEXT_PREFIX);
	
	public static final String TYPE_LOCAL_NAME = "RequestType";
	
	public static final QName TYPE_NAME = new QName(
			"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17",
			XACMLConstants.XACMLCONTEXT_PREFIX);

    protected XACML3RequestType(String namespaceURI, String elementLocalName,
			String namespacePrefix) {
		super(namespaceURI, elementLocalName, namespacePrefix);
	}
    
    public XACML3RequestType(String namespaceURI, String elementLocalName,
			String namespacePrefix, Element element) {
		super(namespaceURI, elementLocalName, namespacePrefix);
		this.setDOM(element);
	}

	public List<XMLObject> getOrderedChildren() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<SubjectType> getSubjects() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ResourceType> getResources() {
		// TODO Auto-generated method stub
		return null;
	}

	public ActionType getAction() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setAction(ActionType newAction) {
		// TODO Auto-generated method stub
		
	}

	public EnvironmentType getEnvironment() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setEnvironment(EnvironmentType environment) {
		// TODO Auto-generated method stub
		
	}
	
}
