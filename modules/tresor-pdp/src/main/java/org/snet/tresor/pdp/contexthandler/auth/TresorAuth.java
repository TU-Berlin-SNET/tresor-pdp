package org.snet.tresor.pdp.contexthandler.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

public interface TresorAuth {

	/**
	 * Handle authentication procedure and communication with client
	 * @param request the httpservlet request
	 * @param response the httpservlet response
	 * @return an AuthUser or null if auth failed
	 */
	public AuthUser authenticate(HttpServletRequest request, HttpServletResponse response);

	/**
	 * @return jsonobject containing information for servlet when authentification fails or is necessary
	 */
	public JSONObject getErrorResponseJSON();
	
}
