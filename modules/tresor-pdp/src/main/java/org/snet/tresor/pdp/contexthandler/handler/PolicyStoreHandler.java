package org.snet.tresor.pdp.contexthandler.handler;

import java.io.StringReader;
import java.util.HashMap;
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
	
	public JSONObject handle(HttpServletRequest request,
			HttpServletResponse response, String httpMethod) {
		AuthUser authUser = this.authenticator.authenticate(request, response);
		JSONObject responseJSON = null;
		
		// if user is authenticated..
		if (authUser != null) {
			// ..and authorized
			if (authUser.isAuthorizedTo(httpMethod, request.getRequestURI())) {				

				if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_GET))
					responseJSON = this.handleGet(request, response, authUser);

				if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_PUT))
					responseJSON = this.handlePut(request, response, authUser);

				if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_DELETE))
					responseJSON = this.handleDelete(request, response,	authUser);
				
			} else {
				// user is authenticated but not authorized
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, true)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_FORBIDDEN);
			}
			
		} else {
			// user is not authenticated
			responseJSON = this.authenticator.getErrorResponseJSON();
		}
		
		return responseJSON;
	}
	
	private JSONObject handleGet(HttpServletRequest request,
			HttpServletResponse response, AuthUser authUser) {
		JSONObject responseJSON = null;		
		
		String domain = authUser.getDomain();
		String[] params = request.getRequestURI().split("/");
		
		Map<String, String> policyMap = this.getPolicies(domain, params);
		
		if (policyMap != null) {
			
			// if asked for specific policy and policyMap returned empty
			if (params.length == 3 && policyMap.isEmpty()) {
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, true)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_NOT_FOUND);
			} else {
				// asked for all policies so return what we got, does not matter whether empty or not
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, false)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_OK)
								.put(KEYJSON_CONTENTTYPE, ServletConstants.CONTENTTYPE_JSON)
								.put(KEYJSON_CONTENT, new JSONObject(policyMap).toString());
			}
			
		} else {
			responseJSON = new JSONObject()
							.put(KEYJSON_ERROR, true)
							.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_BAD_REQUEST);
		}
		
		return responseJSON;
		
	}

	private JSONObject handlePut(HttpServletRequest request,
			HttpServletResponse response, AuthUser authUser) {		
		JSONObject responseJSON = null;
		JSONObject requestJSON = Helper.getJSONFromBody(request);
		
		String domain = authUser.getDomain();
		String[] params = request.getRequestURI().split("/");
		
		if (requestJSON != null && requestJSON.has(KEYJSON_POLICY)) {
			String policy = requestJSON.getString(KEYJSON_POLICY);
		
			// check whether XML is valid, i.e. whether we can parse it
			boolean validXML = false;
			try {
				this.parser.parse(new StringReader(policy));
				validXML = true;
			} catch (Exception e) {				
				log.info("Error parsing policy", e);
				validXML = false;
				responseJSON = new JSONObject()
									.put(KEYJSON_ERROR, true)
									.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_BAD_REQUEST);
			}
		
			// if XML is valid and request is to create new policy (indicated by only two params)
			if (validXML && params.length == 2) {
				if (requestJSON.has(KEYJSON_SERVICE)) {
					// get the service the policy belongs to
					String service = requestJSON.getString(KEYJSON_SERVICE);
					boolean policyExists = this.policystoremanager.getPolicy(domain, service) != null;
					
					// if no policy exists for that service add it
					if (!policyExists) {
						String result = this.policystoremanager.addPolicy(domain, service, policy);
						if (result != null) {
							responseJSON = new JSONObject()
											.put(KEYJSON_ERROR, false)
											.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_CREATED)
											.put("Location", "/" + params[1] + "/" + result);
						} else {
							responseJSON = new JSONObject()
											.put(KEYJSON_ERROR, true)
											.put(KEYJSON_STATUSCODE, 500);
						}
					} else {
						// if a policy for that service already exists, throw error
						responseJSON = new JSONObject()
										.put(KEYJSON_ERROR, true)
										.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_FORBIDDEN)
										.put(KEYJSON_CONTENTTYPE, ServletConstants.CONTENTTYPE_TEXTPLAIN)
										.put(KEYJSON_CONTENT, "Policy already exists");
					}
					
				} else {
					responseJSON = new JSONObject()
									.put(KEYJSON_ERROR, true)
									.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_BAD_REQUEST);
				}
			}
			
			// if XML is valid and request is to update existing policy (indicated by three params, last one for serviceID)
			// see REST Api
			if (validXML && params.length == 3) {
				String service = params[2];
				boolean policyExists = this.policystoremanager.getPolicy(domain, service) != null;
				
				// if the policy exists, replace with new one
				if (policyExists) {
					String result = this.policystoremanager.addPolicy(domain, service, policy);
					if (result != null) {
						responseJSON = new JSONObject()
										.put(KEYJSON_ERROR, false)
										.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_NO_CONTENT);						
					} else {
						responseJSON = new JSONObject()
										.put(KEYJSON_ERROR, true)
										.put(KEYJSON_STATUSCODE, 500);
					}
				} else {
					// if the policy does not exist throw error
					responseJSON = new JSONObject()
									.put(KEYJSON_ERROR, true)
									.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_NOT_FOUND);
				}
				
			}
			
			// if xml is valid but parameter count is off, throw error
			if (validXML && (params.length < 2 || params.length > 3)) {
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, true)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_BAD_REQUEST);
			}
			
		} else {
			responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, true)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_BAD_REQUEST);
		}
		
		return responseJSON;
	}

	private JSONObject handleDelete(HttpServletRequest request,
			HttpServletResponse response, AuthUser authUser) {
		JSONObject responseJSON = null;
		String[] params = request.getRequestURI().split("/");
		
		// delete only possible if param count is 3, indicating direct access to policy, e.g. /policy/serviceID
		if (params.length == 3) {
			String domain = authUser.getDomain();
			String service = params[2];
			
			boolean policyExists = this.policystoremanager.getPolicy(domain, service) != null;
			
			// if the policy exists, delete it
			if (policyExists) {
				int result = this.policystoremanager.deletePolicy(domain, service);
				
				if (result == 0) {
					responseJSON = new JSONObject()
									.put(KEYJSON_ERROR, true)
									.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
				
				if (result == 1) {
					responseJSON = new JSONObject()
									.put(KEYJSON_ERROR, false)
									.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_NO_CONTENT);
				}
				
			} else {
				// if policy does not exist
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, true)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_NOT_FOUND);
			}				
			
		} else {
			// if parameter count is not three
			responseJSON = new JSONObject()
							.put(KEYJSON_ERROR, true)
							.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_BAD_REQUEST);
		}
		
		return responseJSON;			
	}
	
	/**
	 * Gets either one or all policies which belong to given domain or domain/service pair
	 * Which one depends on params length, params[2] taken as service
	 * @param domain the domain the user belongs to
	 * @param params params given through request uri
	 * @return map containing policies as key:service value:policy pairs or null if params.length is not as expected
	 */
	private Map<String, String> getPolicies(String domain, String[] params) {
		Map<String, String> map = null;
		
		if (params.length == 2)
			map = this.policystoremanager.getAll(domain);
		
		if (params.length == 3) {
			map = new HashMap<String, String>();
			String policy = this.policystoremanager.getPolicy(domain, params[2]);
			
			if (policy != null)
				map.put(params[2], policy);
		}
					
		return map;
	}

}
