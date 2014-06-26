package org.snet.tresor.pdp.contexthandler.handler;

import java.io.StringReader;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.snet.tresor.pdp.Configuration;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.contexthandler.auth.AuthUser;
import org.snet.tresor.pdp.contexthandler.auth.TresorAuth;
import org.snet.tresor.pdp.contexthandler.servlet.ServletConstants;
import org.snet.tresor.pdp.policystore.PolicyStoreManager;

/**
 * Handler for interfacing with the policyStoreManager
 * @author malik
 */
public class PolicyStoreHandler implements Handler {
	private static Log log = LogFactory.getLog(PolicyStoreHandler.class);
	
	private PolicyStoreManager policystoremanager;
	private TresorAuth authenticator;
	private ParserPool parser;
	
	public PolicyStoreHandler() {		
		this.policystoremanager = Configuration.getInstance().getPolicyStoreManager();
		this.authenticator = Configuration.getInstance().getTresorAuth();
		this.parser = new BasicParserPool();
	}
	
	public JSONObject handle(HttpServletRequest request, HttpServletResponse response) {
		AuthUser authUser = this.authenticator.authenticate(request, response);

		// check for authentication failure
		if (authUser == null)
			return this.authenticator.getErrorResponseJSON();

		String httpMethod = request.getMethod();
		boolean authorized = authUser.isAuthorizedTo(request);
		
		// check for insufficient authorization
		if (!authorized)
			return Helper.createResponseJSON(true, HttpServletResponse.SC_FORBIDDEN);

		JSONObject responseJSON = null;
		
		// if user is authorized..
		if (authorized) {
			String userdomain = authUser.getDomain();
			// handle the request
			if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_GET))
				responseJSON = this.handleGet(request, userdomain);

			if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_PUT))
				responseJSON = this.handlePut(request, userdomain, authUser);

			if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_DELETE))
				responseJSON = this.handleDelete(request, userdomain);
		}
		
		return responseJSON;
	}
	
	private JSONObject handleGet(HttpServletRequest request, String domain) {
		JSONObject responseJSON = null;
		String[] params = request.getRequestURI().split("/");
		
		// bad request
		if (params.length != 2 && params.length != 3)
			responseJSON = Helper.createResponseJSON(true, HttpServletResponse.SC_BAD_REQUEST);
		
		// call only one level deep -> get all policies belonging to domain
		if (params.length == 2) {
			Map<String, String> policies = this.policystoremanager.getAll(domain);
			responseJSON = Helper.createResponseJSON(false,	HttpServletResponse.SC_OK,
					ServletConstants.CONTENTTYPE_JSON, new JSONObject(policies).toString());
		}
			
		// call two levels deep -> get specific policy
		if (params.length == 3) {
			String service = params[2];
			String policy = this.policystoremanager.getPolicy(domain, service);
			
			if (policy == null)
				responseJSON = Helper.createResponseJSON(true, HttpServletResponse.SC_NOT_FOUND);
			else {
				JSONObject policyJSON = new JSONObject().put(service, policy);
				responseJSON = Helper.createResponseJSON(false, HttpServletResponse.SC_OK,
						ServletConstants.CONTENTTYPE_JSON, policyJSON.toString());
			}			
		}
		
		return responseJSON;
		
	}
	
	private JSONObject handlePut(HttpServletRequest request, String userdomain, AuthUser authUser) {
		
		if (!this.isSupported(request.getContentType()))
			return Helper.createResponseJSON(true, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
					ServletConstants.CONTENTTYPE_TEXTPLAIN, "Accepted Content-Types: application/json");
		
		JSONObject reqBody = Helper.getJSONFromBody(request);		
		String[] params = request.getRequestURI().split("/");
		
		if ((params.length != 2 && params.length != 3) || !this.isValidPutBody(reqBody))
			return Helper.createResponseJSON(true, HttpServletResponse.SC_BAD_REQUEST);
		
		// TODO this is a hacky solution, make it better
		if (reqBody.has("domain") && authUser.getName().equals("broker"))
			userdomain = reqBody.getString("domain");
		
		JSONObject responseJSON = null;
		
		if (params.length == 2)
			responseJSON = this.putNew(userdomain, params, reqBody);
		
		if (params.length == 3)
			responseJSON = this.putReplace(userdomain, params[2], reqBody);
			
		return responseJSON;
	}
	
	private JSONObject putNew(String domain, String[] params, JSONObject reqBody) {		
		String service = reqBody.optString(KEYJSON_SERVICE);
		
		// check for service variable
		if (service == null || service.isEmpty())
			return Helper.createResponseJSON(true, HttpServletResponse.SC_BAD_REQUEST);
		
		// check if policy already exists
		boolean policyExists = this.policystoremanager.getPolicy(domain, service) != null;
		if (policyExists)
			return Helper.createResponseJSON(true, HttpServletResponse.SC_FORBIDDEN, 
					ServletConstants.CONTENTTYPE_TEXTPLAIN, "Policy already exists");
		
		// try adding to policystore
		JSONObject responseJSON = null;
		if (!policyExists) {
			String policy = reqBody.getString(KEYJSON_POLICY);
			String result = this.policystoremanager.addPolicy(domain, service, policy);
			
			if (result == null)
				responseJSON = Helper.createResponseJSON(true, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			else
				responseJSON = Helper.createResponseJSON(false, HttpServletResponse.SC_CREATED)
					.put("Location", "/" + params[1] + "/" + result);
		}
		return responseJSON;
	}
		
	private JSONObject putReplace(String domain, String service, JSONObject reqBody) {
		// check if policy already exists
		boolean policyExists = this.policystoremanager.getPolicy(domain, service) != null;		
		if (!policyExists)
			return Helper.createResponseJSON(true, HttpServletResponse.SC_NOT_FOUND);
		
		// try adding to policystore
		JSONObject responseJSON = null;
		if (policyExists) {
			String policy = reqBody.getString(KEYJSON_POLICY);
			String result = this.policystoremanager.addPolicy(domain, service, policy);
			
			if (result == null)
				responseJSON = Helper.createResponseJSON(true, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			else
				responseJSON = Helper.createResponseJSON(false, HttpServletResponse.SC_NO_CONTENT);
		}
		return responseJSON;
	}

	private JSONObject handleDelete(HttpServletRequest request, String domain) {		
		String[] params = request.getRequestURI().split("/");
		
		if (params.length != 3)
			return Helper.createResponseJSON(true, HttpServletResponse.SC_BAD_REQUEST);
		
		String service = params[2];
		boolean policyExists = this.policystoremanager.getPolicy(domain, service) != null;
		
		if (!policyExists)
			return Helper.createResponseJSON(true, HttpServletResponse.SC_NOT_FOUND);
		
		JSONObject responseJSON = null;
		
		if (policyExists) {
			int result = this.policystoremanager.deletePolicy(domain, service);
			if (result == 1)
				responseJSON = Helper.createResponseJSON(false, HttpServletResponse.SC_NO_CONTENT);
			else
				responseJSON = Helper.createResponseJSON(true, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		
		return responseJSON;			
	}

	private boolean isValidPutBody(JSONObject requestBody) {
		boolean result = false;

		if (requestBody != null) {
			// try parsing it
			try {
				String policy = requestBody.getString(KEYJSON_POLICY);
				this.parser.parse(new StringReader(policy));
				result = true;
			} catch (Exception e) {
				log.info("Error parsing policy");
			}
		}

		return result;
	}
	
	private boolean isSupported(String contenttype) {		
		return contenttype != null && contenttype.toLowerCase().startsWith(ServletConstants.CONTENTTYPE_JSON);
	}
	
}
