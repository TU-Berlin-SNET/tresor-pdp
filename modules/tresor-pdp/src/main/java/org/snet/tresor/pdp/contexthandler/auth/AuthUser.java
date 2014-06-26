package org.snet.tresor.pdp.contexthandler.auth;

import javax.servlet.http.HttpServletRequest;

/**
 * Interface for authenticated users
 * @author malik
 *
 */
public interface AuthUser {

	/**
	 * @return the name e.g. username
	 */
	public String getName();
	
	/**
	 * @return domain this user belongs to
	 */
	public String getDomain();	
	
	/**
	 * @param request the httpservlet request
	 * @return whether the user is authorized to do given request
	 */
	public boolean isAuthorizedTo(HttpServletRequest request);
	
}
