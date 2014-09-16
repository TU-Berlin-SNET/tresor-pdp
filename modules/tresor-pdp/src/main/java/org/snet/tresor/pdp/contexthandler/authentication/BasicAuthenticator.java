package org.snet.tresor.pdp.contexthandler.authentication;

import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.Helper;
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
		String authHeader = request.getHeader(ServletConstants.HEADER_AUTHORIZATION);

		// check for validity of authentication header
		if (authHeader == null || !authHeader.toUpperCase().startsWith("BASIC ") || authHeader.length() < 6) {
			// log missing authentication header
			log.info("Invalid or missing authentication header");
			// ask for authentication
			response.setHeader("WWW-Authenticate", "BASIC realm=\"policystore\"");
			response.setContentType(ServletConstants.CONTENTTYPE_TEXTHTML);
			Helper.respondHTTP(true, HttpServletResponse.SC_UNAUTHORIZED, response);
			return null;
		}

		String userPass = new String(Base64.decodeBase64(authHeader.substring(6)));

		if (userPass != null && users.containsKey(userPass)) {
			String username = userPass.split(":")[0];
			String clientID = users.get(userPass);

			if (clientID != null)
				return new BasicAuthenticatedUser(username, clientID);
			else
				log.warn("Invalid username:password combination");
		}

		return null;
	}

}
