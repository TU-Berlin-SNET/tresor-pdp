package org.snet.tresor.pdp.contexthandler.authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Authenticator {

	/**
	 * Handle authentication procedure
	 * @param request the httpservlet request
	 * @param response the httpservlet response
	 * @return an authenticatedUser or null if authentication failed or is missing
	 */
	public AuthenticatedUser authenticate(HttpServletRequest request, HttpServletResponse response);

}
