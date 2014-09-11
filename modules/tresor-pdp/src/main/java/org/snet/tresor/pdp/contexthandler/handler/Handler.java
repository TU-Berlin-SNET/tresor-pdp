package org.snet.tresor.pdp.contexthandler.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

/**
 * Interface for handlers
 * @author malik
 */
public interface Handler {
	
	/**
	 * @return the name of the resource this handler handles
	 */
	public String getResourceName();
	
	/**
	 * Set important component
	 * @param componentName name of the component to set
	 * @param component the component itself
	 * @return true if setting was successful, false otherwise and may indicate unsupported component
	 */
	public boolean setComponent(String componentName, Object component);
	
	/**
	 * Handle given request and return response as json for servlet to return
	 * @param request the httpservlet request
	 * @param response the httpservlet response
	 * @return JSONObject containing information for responding to client or null if not processable (http 500)
	 */
	public JSONObject handle(HttpServletRequest request, HttpServletResponse response);
	
	public static final String RESOURCE_PDP = "PDP";
	
	public static final String RESOURCE_POLICYSTORE = "POLICYSTORE";
	
	/**
	 * array containing possible keys for additional possible header information in jsonobject
	 */
	public static final String[] KEYJSON_HEADER = { "WWW-Authenticate", "Location", "Allow" };
		
	/**
	 * optional key for value in jsonobject containing whether error happened
	 */
	public static final String KEYJSON_ERROR = "error";
	
	/**
	 * optional key for value in jsonobject containing http status code
	 */
	public static final String KEYJSON_STATUSCODE = "code";
	
	/**
	 * optional key for value in jsonobject containing http content type
	 */
	public static final String KEYJSON_CONTENTTYPE = "contentType";
	
	/**
	 * optional key for value in jsonobject containing the actual content for response body
	 */
	public static final String KEYJSON_CONTENT = "content";
	
	/**
	 * key for the serviceID value
	 */
	public static final String KEYJSON_SERVICE = "service";
	
	/**
	 * key for the domain value
	 */
	public static final String KEYJSON_DOMAIN = "domain";
	
	/**
	 * key for the policy value
	 */
	public static final String KEYJSON_POLICY = "policy";
	
}
