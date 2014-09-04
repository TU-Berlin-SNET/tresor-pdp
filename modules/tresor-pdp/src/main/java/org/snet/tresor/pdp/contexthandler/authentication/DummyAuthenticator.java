package org.snet.tresor.pdp.contexthandler.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.snet.tresor.pdp.contexthandler.handler.Handler;

/**
 * Dummy auth implementation
 * @author malik
 */
public class DummyAuth implements TresorAuth {

	public AuthUser authenticate(HttpServletRequest request,
			HttpServletResponse response) {
		return new DummyAuthUser();
	}

	public JSONObject getErrorResponseJSON() {
		return new JSONObject().put(Handler.KEYJSON_ERROR, true)
							   .put(Handler.KEYJSON_STATUSCODE, 500);
	}

}
