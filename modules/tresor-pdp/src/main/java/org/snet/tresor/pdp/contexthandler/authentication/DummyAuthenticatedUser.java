package org.snet.tresor.pdp.contexthandler.authentication;

import javax.servlet.http.HttpServletRequest;

/**
 * Dummy Auth User implementation
 * @author malik
 */
public class DummyAuthenticatedUser implements AuthenticatedUser {

	public String getName() {
		return "Dummy";
	}

	public String getClientID() {
		return "DummyClientID";
	}

	public boolean isAuthorizedTo(HttpServletRequest request) {
		return true;
	}

	public boolean isAuthorizedTo(String action, String clientID) {
		return true;
	}

	public boolean isAuthorizedTo(String action, String clientID, String serviceID) {
		return true;
	}

}
