package org.snet.contexthandler;

import java.io.StringWriter;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.wso2.balana.PDP;

public class XACMLHandler {

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
	
	public static String handle(String req, PDP pdp) {
		return pdp.evaluate(req);
	}

}
