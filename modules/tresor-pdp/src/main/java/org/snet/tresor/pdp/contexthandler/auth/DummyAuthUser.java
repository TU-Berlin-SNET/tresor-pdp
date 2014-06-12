package org.snet.tresor.pdp.contexthandler.auth;

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

	public boolean isAuthorizedTo(String action, String resource) {
		return true;
	}

	
}
