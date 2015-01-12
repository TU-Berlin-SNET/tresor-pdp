package org.snet.tresor.pdp.additions.finder.impl;

import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Constants for some (Policy-/Attribute-)Finders
 */
public class FinderConstants {

	/**
	 * MapType for a Map<String, String> when using the ObjectMapper
	 */
	public static final MapType MAPTYPE_STRING = TypeFactory.defaultInstance().constructMapType(Map.class, String.class, String.class);

	public static final String GEOLOCATION_ATTRIBUTEID = "org:snet:tresor:attribute:current-geolocation";
	public static final String GEOLOCATION_TIMESTAMP_ATTRIBUTEID = "org:snet:tresor:attribute:timestamp-geolocation";

	public static final String WIFILOCATION_ATTRIBUTEID = "org:snet:tresor:attribute:current-wifi";
	public static final String WIFILOCATION_TIMESTAMP_ATTRIBUTEID = "org:snet:tresor:attribute:timestamp-wifi";

	public static final String WIFISSID_ATTRIBUTEID = "org:snet:tresor:attribute:current-wifi-ssid";
	public static final String WIFISSID_TIMESTAMP_ATTRIBUTEID = "org:snet:tresor:attribute:timestamp-wifi-ssid";

	public static final String BTLOCATION_ATTRIBUTEID = "org:snet:tresor:attribute:current-bluetooth-location";
	public static final String BTLOCATION_TIMESTAMP_ATTRIBUTEID = "org:snet:tresor:attribute:timestamp-bluetooth-location";

	public static final String CELLID_ATTRIBUTEID = "org:snet:tresor:attribute:current-cell-id";
	public static final String CELLID_TIMESTAMP_ATTRIBUTEID = "org:snet:tresor:attribute:timestamp-cell-id";

	public static final String DOCTOR_STATION_ATTRIBUTEID = "org:snet:tresor:attribute:doctor-station";
	public static final String DOCTOR_ROLE_ATTRIBUTEID = "org:snet:tresor:attribute:doctor-role";

	public static final String PATIENT_STATION_ATTRIBUTEID = "org:snet:tresor:attribute:patient-station";
	public static final String PATIENT_ROLE_ATTRIBUTEID = "org:snet:tresor:attribute:patient-role";

	/**
	 * XACML URI denoting subject category
	 */
	public static final URI SUBJECT_CATEGORY_URI;

	/**
	 * XACML URI denoting resource category
	 */
	public static final URI RESOURCE_CATEGORY_URI;

	/**
	 * XACML URI denoting string datatype
	 */
	public static final URI STRING_DATATYPE_URI;

	/**
	 * XACML URI denoting subject id
	 */
	public static final URI SUBJECT_ID_URI;

	/**
	 * XACML URI denoting device id
	 */
	public static final URI DEVICE_ID_URI;

	/**
	 * XACML URI denoting domain id
	 */
	public static final URI CLIENT_ID_URI;

	/**
	 * XACML URI denoting service id
	 */
	public static final URI SERVICE_ID_URI;

	static {
		try {
			SUBJECT_CATEGORY_URI = new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
			RESOURCE_CATEGORY_URI = new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource");
			STRING_DATATYPE_URI = new URI("http://www.w3.org/2001/XMLSchema#string");
			SUBJECT_ID_URI = new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id");
			DEVICE_ID_URI = new URI("org:snet:tresor:attribute:device-id");
			CLIENT_ID_URI = new URI("http://schemas.cloud-tresor.com/request/2014/09/tresor-organization-uuid");
			SERVICE_ID_URI = new URI("http://schemas.cloud-tresor.com/request/2014/04/service-uuid");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
