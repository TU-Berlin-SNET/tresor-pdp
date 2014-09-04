package org.snet.tresor.pdp.contexthandler.authentication;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

/**
 * Interface for authenticated users
 * @author malik
 *
 */
public interface AuthenticatedUser {

	/**
	 * @return the name e.g. username
	 */
	public String getName();
	
	/**
	 * @return client-id this user belongs to
	 */
	public String getClientID();	
	
	/**
	 * @param request the httpservlet request
	 * @return whether the user is authorized to do given request
	 */
	public boolean isAuthorizedTo(HttpServletRequest request);
	
	/**
	 * @param request the httpservlet request
	 * @param reqBody the body inside the request, parsed
	 * @return whether the user is authorized to do given request
	 */
	public boolean isAuthorizedTo(HttpServletRequest request, JSONObject reqBody);
}
