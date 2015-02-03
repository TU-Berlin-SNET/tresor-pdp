package org.snet.tresor.pdp.additions.finder.impl;

import org.wso2.balana.XACMLConstants;

import java.net.URI;

/**
 * Constants for some (Policy-/Attribute-)Finders
 */
public class FinderConstants {

	/**
	 * XACML Attribute Ids
	 */

	public static final URI ATTRIBUTE_ID_SUBJECT;
	public static final URI ATTRIBUTE_ID_DEVICE;
	public static final URI ATTRIBUTE_ID_CLIENT;
	public static final URI ATTRIBUTE_ID_SERVICE;

	public static final String ATTRIBUTE_ID_GEOLOCATION = "org:snet:tresor:attribute:current-geolocation";
	public static final String ATTRIBUTE_ID_GEOLOCATION_TIMESTAMP = "org:snet:tresor:attribute:timestamp-geolocation";

	public static final String ATTRIBUTE_ID_WIFILOCATION = "org:snet:tresor:attribute:current-wifi";
	public static final String ATTRIBUTE_ID_WIFILOCATION_TIMESTAMP = "org:snet:tresor:attribute:timestamp-wifi";

	public static final String ATTRIBUTE_ID_WIFISSID = "org:snet:tresor:attribute:current-wifi-ssid";
	public static final String ATTRIBUTE_ID_WIFISSID_TIMESTAMP = "org:snet:tresor:attribute:timestamp-wifi-ssid";

	public static final String ATTRIBUTE_ID_BTLOCATION = "org:snet:tresor:attribute:current-bluetooth-location";
	public static final String ATTRIBUTE_ID_BTLOCATION_TIMESTAMP = "org:snet:tresor:attribute:timestamp-bluetooth-location";

	public static final String ATTRIBUTE_ID_CELLID = "org:snet:tresor:attribute:current-cell-id";
	public static final String ATTRIBUTE_ID_CELLID_TIMESTAMP = "org:snet:tresor:attribute:timestamp-cell-id";

	public static final String ATTRIBUTE_ID_DOCTOR_STATION = "org:snet:tresor:attribute:doctor-station";
	public static final String ATTRIBUTE_ID_DOCTOR_ROLE = "org:snet:tresor:attribute:doctor-role";

	public static final String ATTRIBUTE_ID_PATIENT_STATION = "org:snet:tresor:attribute:patient-station";
	public static final String ATTRIBUTE_ID_PATIENT_ROLE = "org:snet:tresor:attribute:patient-role";


	/**
	 * XACML Datatypes
	 */

	public static final URI DATATYPE_STRING;
	public static final URI DATATYPE_INTEGER;
	public static final URI DATATYPE_GEO;


	/**
	 * XACML Categories
	 */

	public static final URI CATEGORY_SUBJECT;
	public static final URI CATEGORY_RESOURCE;
	public static final URI CATEGORY_ENVIRONMENT;


	static {
		try {
			ATTRIBUTE_ID_SUBJECT = new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id");
			ATTRIBUTE_ID_DEVICE = new URI("org:snet:tresor:attribute:device-id");
			ATTRIBUTE_ID_CLIENT = new URI("http://schemas.cloud-tresor.com/request/2014/09/tresor-organization-uuid");
			ATTRIBUTE_ID_SERVICE = new URI("http://schemas.cloud-tresor.com/request/2014/04/service-uuid");

			DATATYPE_STRING = new URI("http://www.w3.org/2001/XMLSchema#string");
			DATATYPE_INTEGER = new URI("http://www.w3.org/2001/XMLSchema#integer");
			DATATYPE_GEO = new URI("urn:ogc:def:dataType:geoxacml:1.0:geometry");

			CATEGORY_SUBJECT = new URI(XACMLConstants.SUBJECT_CATEGORY);
			CATEGORY_RESOURCE = new URI(XACMLConstants.RESOURCE_CATEGORY);
			CATEGORY_ENVIRONMENT = new URI(XACMLConstants.ENT_CATEGORY);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
