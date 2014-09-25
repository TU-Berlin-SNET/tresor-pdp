package org.snet.tresor.pdp.contexthandler.authentication;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.NotImplementedException;

/**
 * Representation of a user containing required information for further processing after auth procedure
 * @author malik
 */
public class BasicAuthenticatedUser implements AuthenticatedUser{

	private String name;
	private String clientID;

	public BasicAuthenticatedUser(String name, String clientID) {
		this.name = name;
		this.clientID = clientID;
	}

	public String getName() {
		return this.name;
	}

	public String getClientID() {
		return this.clientID;
	}

	public boolean isAuthorizedTo(HttpServletRequest request) {
		throw new NotImplementedException();
	}

	public boolean isAuthorizedTo(String action, String clientID) {
		//return (this.clientID.equals(clientID) || this.name.equals("broker"));
		return (this.clientID.equalsIgnoreCase(clientID) || this.name.equalsIgnoreCase("broker"));
	}

	public boolean isAuthorizedTo(String action, String clientID, String serviceID) {
		return this.isAuthorizedTo(action, clientID);
	}

}
