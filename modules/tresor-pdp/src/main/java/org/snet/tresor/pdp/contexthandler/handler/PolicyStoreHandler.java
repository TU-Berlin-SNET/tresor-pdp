package org.snet.tresor.pdp.contexthandler.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.TresorPDP;
import org.snet.tresor.pdp.contexthandler.auth.AuthUser;
import org.snet.tresor.pdp.contexthandler.auth.TresorAuth;
import org.snet.tresor.pdp.contexthandler.servlet.ServletConstants;
import org.snet.tresor.pdp.policystore.PolicyStoreManager;

/**
 * Handler-class which handles getting, adding and removing of policies.
 * 
 * @author malik
 */
public class PolicyStoreHandler implements Handler {
	private static Log log = LogFactory.getLog(PolicyStoreHandler.class);
	
	private PolicyStoreManager policystoremanager;
	private TresorAuth authenticator;		
	
	public PolicyStoreHandler() {
		// TODO remove
		log.info("loading policystorehandler");
		
		this.policystoremanager = TresorPDP.getInstance().getPolicyStoreManager();
		this.authenticator = TresorPDP.getInstance().getTresorAuth();
	}
	
	public JSONObject handle(HttpServletRequest request,
			HttpServletResponse response, String httpMethod) {
		AuthUser authUser = this.authenticator.authenticate(request, response);
		JSONObject responseJSON = null;
		
		if (authUser != null) {
			// TODO remove
			log.info("authuser not null");
			if (authUser.isAuthorizedTo(httpMethod, request.getRequestURI())) {				
				// TODO remove
				log.info("authuser authorized");
			if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_GET))
				responseJSON = this.handleGet(request, response, authUser);
			
			if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_PUT))
				responseJSON = this.handlePut(request, response, authUser);
			
			if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_POST))
				responseJSON = this.handlePost(request, response, authUser);
			
			if (httpMethod.equalsIgnoreCase(ServletConstants.HTTP_DELETE))
				responseJSON = this.handleDelete(request, response, authUser);
			
			} else {
				// TODO remove
				log.info("authuser not authorized");
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, true)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_FORBIDDEN);								
			}
			
		} else {
			// TODO remove
			log.info("authuser not authenticated");
			responseJSON = this.authenticator.getErrorResponseJSON();
		}
		
		return responseJSON;
	}
	
	private JSONObject handleGet(HttpServletRequest request,
			HttpServletResponse response, AuthUser authUser) {
		JSONObject responseJSON = null;
		
		// TODO remove
		log.info("policystorehandler, handleGet");		
		
		String domain = authUser.getDomain();
		String[] params = request.getRequestURI().split("/");
		
		// TODO remove
		log.info("params.length= " + params.length);
		
		Map<String, String> policyMap = this.getPolicies(domain, params);
		
		// TODO remove
		log.info("policyMap.isEmpty= " + policyMap.isEmpty());
		
		// TODO remove
		log.info("policyMap.isEmpty= " + policyMap.keySet().toArray());
		
		for (Entry<String, String> e : policyMap.entrySet())
			System.out.println(e.getKey() + " " + e.getValue());
		
		if (policyMap != null) {
			
			if (params.length == 3 && policyMap.isEmpty()) {
				// TODO remove
				log.info("params.length = 3 and policymap empty");
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, true)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_NOT_FOUND);
			} else {
				// TODO remove
				log.info("params.length != 3 || policymap not empty");
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, false)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_OK)
								.put(KEYJSON_CONTENTTYPE, ServletConstants.CONTENTTYPE_JSON)
								.put(KEYJSON_CONTENT, new JSONObject(policyMap).toString());
			}
			
		} else {
			// TODO remove
			log.info("policymap == null");
			responseJSON = new JSONObject()
							.put(KEYJSON_ERROR, true)
							.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_BAD_REQUEST);
		}
		
		return responseJSON;
		
	}

	public JSONObject handlePut(HttpServletRequest request,
			HttpServletResponse response, AuthUser authUser) {		
		JSONObject responseJSON = null;
		JSONObject requestJSON = Helper.getJSONFromBody(request);
		
		String domain = authUser.getDomain();
		String[] params = request.getRequestURI().split("/");
		
		if (requestJSON != null && requestJSON.has(KEYJSON_POLICY)) {
			String policy = requestJSON.getString(KEYJSON_POLICY);
			
			if (params.length == 2) {				
				if (requestJSON.has(KEYJSON_SERVICE)) {
					String service = requestJSON.getString(KEYJSON_SERVICE);
					boolean policyExists = this.policystoremanager.getPolicy(domain, service) != null;
					
					if (!policyExists) {
						this.policystoremanager.addPolicy(domain, service, policy);
						responseJSON = new JSONObject()
										.put(KEYJSON_ERROR, false)
										.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_CREATED)
										.put("Location", "/" + params[1] + "/" + service);										
					} else {
						responseJSON = new JSONObject()
										.put(KEYJSON_ERROR, true)
										.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_BAD_REQUEST)
										.put(KEYJSON_CONTENTTYPE, ServletConstants.CONTENTTYPE_TEXTPLAIN)
										.put(KEYJSON_CONTENT, "Policy already exists");
					}
					
				} else {
					responseJSON = new JSONObject()
									.put(KEYJSON_ERROR, true)
									.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_BAD_REQUEST);
				}
			}
			
			if (params.length == 3) {
				String service = params[2];
				boolean policyExists = this.policystoremanager.getPolicy(domain, service) != null;
				
				if (policyExists) {
					this.policystoremanager.addPolicy(domain, service, policy);
					responseJSON = new JSONObject()
									.put(KEYJSON_ERROR, false)
									.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_NO_CONTENT);
				} else {
					responseJSON = new JSONObject()
									.put(KEYJSON_ERROR, true)
									.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_NOT_FOUND);
				}
				
			}
			
			if (params.length < 2 || params.length > 3) {
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, true)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_BAD_REQUEST);
			}
			
		}
		
		return responseJSON;
	}
	
	public JSONObject handlePost(HttpServletRequest request,
			HttpServletResponse response, AuthUser authUser) {
		return new JSONObject()
					.put(KEYJSON_ERROR, true)
					.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_METHOD_NOT_ALLOWED)
					.put("Allow", "GET, PUT, DELETE");
	}

	public JSONObject handleDelete(HttpServletRequest request,
			HttpServletResponse response, AuthUser authUser) {
		JSONObject responseJSON = null;
		String[] params = request.getRequestURI().split("/");
		
		if (params.length == 3) {					
			String domain = authUser.getDomain();
			String service = params[2];
			boolean policyExists = this.policystoremanager.getPolicy(domain, service) == null;
			
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
				responseJSON = new JSONObject()
								.put(KEYJSON_ERROR, true)
								.put(KEYJSON_STATUSCODE, HttpServletResponse.SC_NOT_FOUND);
			}				
			
		} else {
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
