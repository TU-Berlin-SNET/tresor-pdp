package org.snet.tresor.pdp.finder.impl;

import java.net.URI;

public class FinderConstants {

	public static URI CATEGORY_SUBJECT_URI;
	public static URI CATEGORY_RESOURCE_URI;
	public static URI DATATYPE_STRING_URI;
	public static URI ID_SUBJECT_URI;
	public static URI ID_DEVICE_URI;
	public static URI ID_DOMAIN_URI;
	public static URI ID_SERVICE_URI;
	
	static {
		try {
			CATEGORY_SUBJECT_URI = new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
			CATEGORY_RESOURCE_URI = new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource");
			DATATYPE_STRING_URI = new URI("http://www.w3.org/2001/XMLSchema#string");
			ID_DOMAIN_URI = new URI("domain");
			ID_SERVICE_URI = new URI("service");
			ID_SUBJECT_URI = new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id");
			ID_DEVICE_URI = new URI("device-id");		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
