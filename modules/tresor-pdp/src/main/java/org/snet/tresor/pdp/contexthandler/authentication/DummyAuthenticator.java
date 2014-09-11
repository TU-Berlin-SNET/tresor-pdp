package org.snet.tresor.pdp.contexthandler.authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Dummy auth implementation
 * @author malik
 */
public class DummyAuthenticator implements Authenticator {

	private DummyAuthenticatedUser user = new DummyAuthenticatedUser();

	public AuthenticatedUser authenticate(HttpServletRequest request,
			HttpServletResponse response) {
		return user;
	}
	
}
