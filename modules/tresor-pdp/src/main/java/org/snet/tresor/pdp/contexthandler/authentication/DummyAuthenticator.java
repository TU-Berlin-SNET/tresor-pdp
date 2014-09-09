package org.snet.tresor.pdp.contexthandler.authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.snet.tresor.pdp.contexthandler.handler.Handler;

/**
 * Dummy auth implementation
 * @author malik
 */
public class DummyAuthenticator implements Authenticator {

	public AuthenticatedUser authenticate(HttpServletRequest request,
			HttpServletResponse response) {
		return new DummyAuthenticatedUser();
	}

	public JSONObject getErrorResponseJSON() {
		return new JSONObject().put(Handler.KEYJSON_ERROR, true)
							   .put(Handler.KEYJSON_STATUSCODE, 500);
	}

}
