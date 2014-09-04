package org.snet.tresor.pdp.contexthandler.authentication;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

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

	public boolean isAuthorizedTo(HttpServletRequest request, JSONObject reqBody) {
		return true;
	}

	
}
