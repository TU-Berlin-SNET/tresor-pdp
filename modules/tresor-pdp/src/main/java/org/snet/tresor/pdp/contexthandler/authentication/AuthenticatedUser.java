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
	 * @return true if authorized, false otherwise
	 */
	public boolean isAuthorizedTo(HttpServletRequest request);
	
	/**
	 * @param action the action to be taken
	 * @param clientID the resource's clientID
	 * @return true if authorized, false otherwise
	 */
	public boolean isAuthorizedTo(String action, String clientID);
	
	/**
	 * @param action the action to be taken
	 * @param clientID the resource's clientID
	 * @param serviceID the resource's serviceID
	 * @return true if authorized, false otherwise
	 */
	public boolean isAuthorizedTo(String action, String clientID, String serviceID);
	
}
