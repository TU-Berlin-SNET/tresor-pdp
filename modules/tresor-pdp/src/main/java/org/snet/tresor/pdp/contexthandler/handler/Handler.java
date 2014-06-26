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
	 * Handle given request and return response as json for servlet to return
	 * @param request the httpservlet request
	 * @param response the httpservlet response
	 * @return JSONObject containing information for responding to client or null if not processable (http 500)
	 */
	public JSONObject handle(HttpServletRequest request, HttpServletResponse response);
	
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
