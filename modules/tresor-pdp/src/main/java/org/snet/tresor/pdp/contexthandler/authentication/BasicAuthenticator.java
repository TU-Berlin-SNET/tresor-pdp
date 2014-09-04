package org.snet.tresor.pdp.contexthandler.authentication;

import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.contexthandler.handler.Handler;
import org.snet.tresor.pdp.contexthandler.servlet.ServletConstants;

/**
 * Class providing HTTP basic authentication
 * @author malik
 *
 */
public class BasicAuthenticator implements Authenticator {
	private static Logger log = LoggerFactory.getLogger(BasicAuthenticator.class);
	
	/**
	 * Hashtable, Keys: "user:password", values: "domain"
	 */
	private Hashtable<String, String> users;
	
	public BasicAuthenticator() {		
		this.users = new Hashtable<String, String>();
	}
	
	/**
	 * Create new HTTPBasicAuth with given username:password -> domain params
	 * @param params array containing user information in the 
	 * following form ["username:password", "domain", ... ]
	 */
	public BasicAuthenticator(String... params) {
		this();
		
		for (int i = 0; i < params.length - 1; i+=2)
			this.users.put(params[i], params[i+1]);		
	}
	
	public AuthenticatedUser authenticate(HttpServletRequest request, HttpServletResponse response) {		
		String auth = request.getHeader(ServletConstants.HEADER_AUTHORIZATION);
		AuthenticatedUser authUser = null;
		
		// check if it is HTTP Basic Auth
		if (auth != null && auth.toUpperCase().startsWith("BASIC ")) {
			String userPass = new String(Base64.decodeBase64(auth.substring(6)));
			
			// check if user:password is in known users
			if (users.containsKey(userPass)) {
				// create authUser
				String username = userPass.split(":")[0];
				String userdomain = users.get(userPass);

				authUser = new BasicAuthenticatedUser(username, userdomain);
			}
		}
		
		// if auth failed
		if (authUser == null) {
			if (auth == null)
				log.error("No Auth header available");
			else if (auth != null && !auth.toUpperCase().startsWith("BASIC "))
				log.error("Wrong Auth header type");
			else
				log.error("Auth failed");
		}
		
		return authUser;
	}
	
	public JSONObject getErrorResponseJSON() {	
		return new JSONObject()
					.put(Handler.KEYJSON_ERROR, true)
					.put(Handler.KEYJSON_STATUSCODE, HttpServletResponse.SC_UNAUTHORIZED)
					.put("WWW-Authenticate", "BASIC realm=\"policystore\"")
					.put(Handler.KEYJSON_CONTENTTYPE, ServletConstants.CONTENTTYPE_TEXTHTML);
	}

}
