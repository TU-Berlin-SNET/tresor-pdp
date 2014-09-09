package org.snet.tresor.pdp.finder.impl;

import java.net.URI;

/**
 * Class containing constants for Finders
 * @author malik
 */
public class FinderConstants {

	/**
	 * XACML URI denoting subject category
	 */
	public static URI CATEGORY_SUBJECT_URI;
	
	/**
	 * XACML URI denoting resource category
	 */
	public static URI CATEGORY_RESOURCE_URI;
	
	/**
	 * XACML URI denoting string datatype
	 */
	public static URI DATATYPE_STRING_URI;
	
	/**
	 * XACML URI denoting subject id
	 */
	public static URI ID_SUBJECT_URI;
	
	/**
	 * XACML URI denoting device id
	 */
	public static URI ID_DEVICE_URI;
	
	/**
	 * XACML URI denoting domain id
	 */
	public static URI ID_DOMAIN_URI;
	
	/**
	 * XACML URI denoting service id
	 */
	public static URI ID_SERVICE_URI;
	
	static {
		try {
			CATEGORY_SUBJECT_URI = new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
			CATEGORY_RESOURCE_URI = new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource");
			DATATYPE_STRING_URI = new URI("http://www.w3.org/2001/XMLSchema#string");
			ID_SUBJECT_URI = new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id");
			ID_DEVICE_URI = new URI("org:snet:tresor:attribute:device-id");
//			ID_DOMAIN_URI = new URI("org:snet:tresor:attribute:domain-id");
//			ID_SERVICE_URI = new URI("org:snet:tresor:attribute:service-id");
			ID_DOMAIN_URI = new URI("http://schemas.tresor.com/claims/2014/04/organization");
			ID_SERVICE_URI = new URI("http://schemas.tresor.com/request/2014/04/service-uuid");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
