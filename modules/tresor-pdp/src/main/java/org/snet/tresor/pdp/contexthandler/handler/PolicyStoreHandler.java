package org.snet.tresor.pdp.contexthandler.handler;

import java.io.StringReader;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.contexthandler.authentication.AuthenticatedUser;
import org.snet.tresor.pdp.contexthandler.authentication.Authenticator;
import org.snet.tresor.pdp.contexthandler.servlet.ServletConstants;
import org.snet.tresor.pdp.policystore.PolicyStore;
import org.w3c.dom.Element;
import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.DOMHelper;
import org.wso2.balana.Policy;

/**
 * Handler for interfacing with the policyStoreManager
 * @author malik
 */
public class PolicyStoreHandler implements Handler {	
	private static final Logger log = LoggerFactory.getLogger(PolicyStoreHandler.class);
	
	private PolicyStore policystore;
	private Authenticator authenticator;

	private ParserPool parser;
	private String resource;
	
	public PolicyStoreHandler() {
		this.resource = "policy";
		this.parser = new BasicParserPool();
	}
	
	public String getResourceName() {
		return this.resource;
	}
	
	public boolean setComponent(String componentName, Object component) {
		if (componentName.equalsIgnoreCase(this.resource) && component instanceof PolicyStore) {
			this.policystore = (PolicyStore) component;
			return true;
		}
		
		if (componentName.equalsIgnoreCase("authenticator") && component instanceof Authenticator) {
			this.authenticator = (Authenticator) component;
			return true;
		}
		
		// TODO log unsupported component
		return false;
	}
	
	public JSONObject handle(HttpServletRequest request, HttpServletResponse response) {
		AuthenticatedUser authUser = this.authenticator.authenticate(request, response);

		if (authUser == null)
			return this.authenticator.getErrorResponseJSON();

		String httpMethod = request.getMethod();

		JSONObject responseJSON = null;
		
		if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_GET))
			responseJSON = this.handleGet(request, authUser);

		if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_PUT))
			responseJSON = this.handlePut(request, authUser);

		if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_DELETE))
			responseJSON = this.handleDelete(request, authUser);
		
		return responseJSON;
	}
	
	private JSONObject handleGet(HttpServletRequest request, AuthenticatedUser user) {		
		if (!user.isAuthorizedTo(request))
			return Helper.createResponseJSON(true, HttpServletResponse.SC_FORBIDDEN);
		
		JSONObject responseJSON = null;
		String[] params = request.getRequestURI().split("/");
		
		// bad request
		if (params.length != 2 && params.length != 3)
			responseJSON = Helper.createResponseJSON(true, HttpServletResponse.SC_BAD_REQUEST);
		
		String clientID = user.getClientID();
		
		// call only one level deep -> get all policies belonging to domain
		if (params.length == 2) {
			Map<String, String> policies = this.policystore.getAll(clientID);
			responseJSON = Helper.createResponseJSON(false,	HttpServletResponse.SC_OK,
					ServletConstants.CONTENTTYPE_JSON, new JSONObject(policies).toString());
		}
			
		// call two levels deep -> get specific policy
		if (params.length == 3) {
			String service = params[2];
			String policy = this.policystore.getPolicy(clientID, service);
			
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
	
	private JSONObject handlePut(HttpServletRequest request, AuthenticatedUser user) {
		
		if (!this.isSupported(request.getContentType()))
			return Helper.createResponseJSON(true, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
					ServletConstants.CONTENTTYPE_TEXTPLAIN, "Accepted Content-Types: application/json");
		
		JSONObject reqBody = Helper.getJSONFromBody(request);
		String[] params = request.getRequestURI().split("/");
		
		// check validity of input parameters and request path
		if ((params.length != 2 && params.length != 3) || !this.isValidPutBody(reqBody))
			return Helper.createResponseJSON(true, HttpServletResponse.SC_BAD_REQUEST);
		
		// check if user has authorization to do this request
		if (!user.isAuthorizedTo(request, reqBody))
			return Helper.createResponseJSON(true, HttpServletResponse.SC_FORBIDDEN);			
		
		String targetdomain = user.getClientID();
		
		// TODO remove
		if (reqBody.has("client-id") || reqBody.has("domain")) {
			targetdomain = (reqBody.has("client-id")) ? reqBody.getString("client-id") : reqBody.getString("domain");
		}
		
		JSONObject responseJSON = null;
		
		if (params.length == 2)
			responseJSON = this.putNew(targetdomain, params, reqBody);
		
		if (params.length == 3)
			responseJSON = this.putReplace(targetdomain, params[2], reqBody);
			
		return responseJSON;
	}
	
	private JSONObject putNew(String domain, String[] params, JSONObject reqBody) {		
		String service = reqBody.optString(KEYJSON_SERVICE);
		
		// check for service variable
		if (service == null || service.isEmpty())
			return Helper.createResponseJSON(true, HttpServletResponse.SC_BAD_REQUEST);
		
		// check if policy already exists
		boolean policyExists = this.policystore.getPolicy(domain, service) != null;
		if (policyExists)
			return Helper.createResponseJSON(true, HttpServletResponse.SC_FORBIDDEN, 
					ServletConstants.CONTENTTYPE_TEXTPLAIN, "Policy already exists");
		
		// try adding to policystore
		JSONObject responseJSON = null;
		if (!policyExists) {
			String policy = reqBody.getString(KEYJSON_POLICY);
			String result = this.policystore.addPolicy(domain, service, policy);
			
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
		boolean policyExists = this.policystore.getPolicy(domain, service) != null;		
		if (!policyExists)
			return Helper.createResponseJSON(true, HttpServletResponse.SC_NOT_FOUND);
		
		// try adding to policystore
		JSONObject responseJSON = null;
		if (policyExists) {
			String policy = reqBody.getString(KEYJSON_POLICY);
			String result = this.policystore.addPolicy(domain, service, policy);
			
			if (result == null)
				responseJSON = Helper.createResponseJSON(true, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			else
				responseJSON = Helper.createResponseJSON(false, HttpServletResponse.SC_NO_CONTENT);
		}
		return responseJSON;
	}

	private JSONObject handleDelete(HttpServletRequest request, AuthenticatedUser user) {
		if (!user.isAuthorizedTo(request))
			return Helper.createResponseJSON(true, HttpServletResponse.SC_FORBIDDEN);
		
		String[] params = request.getRequestURI().split("/");
		
		if (params.length != 3)
			return Helper.createResponseJSON(true, HttpServletResponse.SC_BAD_REQUEST);
				
		String service = params[2];
		String clientID = user.getClientID();
		boolean policyExists = this.policystore.getPolicy(clientID, service) != null;
		
		if (!policyExists)
			return Helper.createResponseJSON(true, HttpServletResponse.SC_NOT_FOUND);
		
		JSONObject responseJSON = null;
		
		if (policyExists) {
			int result = this.policystore.deletePolicy(clientID, service);
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
				String policyString = requestBody.getString(KEYJSON_POLICY);
				Element root = this.parser.parse(new StringReader(policyString)).getDocumentElement();
								
				String name = DOMHelper.getLocalName(root);
				AbstractPolicy policy = null;
				
                if (name.equals("Policy"))
                	policy = Policy.getInstance(root);                
				
				result = policy != null;
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
