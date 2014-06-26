package org.snet.tresor.pdp.contexthandler.auth;

import javax.servlet.http.HttpServletRequest;

/**
 * Dummy Auth User implementation
 * @author malik
 */
public class DummyAuthUser implements AuthUser {

	public String getName() {
		return "Dummy";
	}

	public String getDomain() {
		return "DummyDomain";
	}

	public boolean isAuthorizedTo(HttpServletRequest request) {
		return true;
	}

	
}
