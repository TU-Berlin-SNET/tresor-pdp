package org.snet.tresor.pdp.contexthandler.handler;

import java.io.StringWriter;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.wso2.balana.PDP;

/**
 * Simple Handler for pure XACML requests.
 */
public class XACMLHandler {

	/**
	 * Converts given XACML-Element to String an evaluates via given PDP
	 * @param elem, XACML request
	 * @param pdp
	 * @return xacml-response from pdp
	 */
	public static String handle(Element elem, PDP pdp) {
		String s = null;
		
		try {
			StringWriter buffer = new StringWriter();
			TransformerFactory.newInstance().newTransformer()
				.transform(new DOMSource(elem), new StreamResult(buffer));
			s = pdp.evaluate(buffer.toString());
		} catch (Exception e) {	e.printStackTrace(); }

		return s;
	}
	
	/**
	 * Evaluates given xacml-request with pdp
	 * @param req, xacml-request
	 * @param pdp
	 * @return xacml-response from pdp
	 */
	public static String handle(String req, PDP pdp) {
		return pdp.evaluate(req);
	}

}
