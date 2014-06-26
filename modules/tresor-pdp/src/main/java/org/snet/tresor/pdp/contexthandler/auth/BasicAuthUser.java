package org.snet.tresor.pdp.contexthandler.auth;

import javax.servlet.http.HttpServletRequest;

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

	public boolean isAuthorizedTo(HttpServletRequest request) {
		// every user is allowed to act however it wants to on resources in its own domain
		return true;
	}

}
