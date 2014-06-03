package org.snet.tresor.pdp.contexthandler.auth;

/**
 * Representation of a user containing required information for further processing after auth procedure 
 * @author malik
 */
public class BasicAuthUser implements AuthUser{

	private String name;
	private String domain;

	public BasicAuthUser(String name, String domain) {
		this.name = name.toLowerCase();
		this.domain = domain.toLowerCase();
	}

	public String getName() {
		return this.name;
	}
	
	public String getDomain() {
		return this.domain;
	}

	public boolean isAuthorizedTo(String action, String resource) {
		// every user is allowed to act however it wants to on resources in its own domain
		return true;
	}

}
