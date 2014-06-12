package org.snet.tresor.pdp.contexthandler.auth;

import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.snet.tresor.pdp.contexthandler.handler.Handler;
import org.snet.tresor.pdp.contexthandler.servlet.ServletConstants;

/**
 * Class providing HTTP basic authentication
 * @author malik
 *
 */
public class HTTPBasicAuth implements TresorAuth {
	private static Log log = LogFactory.getLog(HTTPBasicAuth.class);
	
	/**
	 * Hashtable, Keys: "user:password", values: "domain"
	 */
	private Hashtable<String, String> users;
	
	public HTTPBasicAuth() {		
		this.users = new Hashtable<String, String>();
	}
	
	/**
	 * Create new HTTPBasicAuth with given username:password -> domain params
	 * @param params array containing user information in the 
	 * following form ["username:password", "domain", ... ]
	 */
	public HTTPBasicAuth(String... params) {
		this();
		
		for (int i = 0; i < params.length - 1; i+=2)
			this.users.put(params[i], params[i+1]);		
	}
	
	public AuthUser authenticate(HttpServletRequest request, HttpServletResponse response) {		
		String auth = request.getHeader(ServletConstants.HEADER_AUTHORIZATION);
		AuthUser authUser = null;
		
		// check if it is HTTP Basic Auth
		if (auth != null && auth.toUpperCase().startsWith("BASIC ")) {
			String userPass = new String(Base64.decodeBase64(auth.substring(6)));
			
			// check if user:password is in known users
			if (users.containsKey(userPass)) {
				// create authUser
				String username = userPass.split(":")[0];
				String userdomain = users.get(userPass);

				authUser = new BasicAuthUser(username, userdomain);
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
					.put("WWW-Authenticate", "BASIC realm=\"policystore\"")
					.put(Handler.KEYJSON_CONTENTTYPE, ServletConstants.CONTENTTYPE_TEXTHTML)
					.put(Handler.KEYJSON_STATUSCODE, HttpServletResponse.SC_UNAUTHORIZED);
	}

}
