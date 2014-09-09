package org.snet.tresor.pdp.contexthandler.authentication;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representation of a user containing required information for further processing after auth procedure 
 * @author malik
 */
public class BasicAuthenticatedUser implements AuthenticatedUser{
	private static final Logger log = LoggerFactory.getLogger(BasicAuthenticatedUser.class);
	
	private String name;
	private String clientID;

	public BasicAuthenticatedUser(String name, String clientID) {
		// TODO investigate why
//		this.name = name.toLowerCase();
//		this.clientID = clientID.toLowerCase();
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
		// every user is allowed to act however it wants to on resources in its own domain
		return true;
	}

	public boolean isAuthorizedTo(HttpServletRequest request, JSONObject reqBody) {
		// only broker is allowed to act on behalf of other domains
		// TODO can still be made better
		if (reqBody.has("client-id") || reqBody.has("domain"))
			return (this.name.equalsIgnoreCase("broker") || this.clientID.equalsIgnoreCase(reqBody.optString("client-id"))
					 || this.clientID.equalsIgnoreCase(reqBody.optString("domain")));
		
		return true;
	}

}
