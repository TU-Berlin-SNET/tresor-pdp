package org.snet.tresor.pdp.contexthandler.servlet;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.opensaml.xml.parse.XMLParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.contexthandler.ContextHandler;
import org.snet.tresor.pdp.contexthandler.authentication.AuthenticatedUser;
import org.snet.tresor.pdp.contexthandler.authentication.Authenticator;
import org.snet.tresor.pdp.contexthandler.handler.PolicyStoreHandler;
import org.wso2.balana.ParsingException;

/**
 * A simple Servlet which provides a restful interface for getting, adding and removing of policies.
 * @author malik
 */
public class PolicyStoreServlet extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(PolicyStoreServlet.class);	
	
	private String acceptContentTypes = "Accepted Content-Types: " + ServletConstants.CONTENTTYPE_XACML;
	
	private PolicyStoreHandler policyStoreHandler;
	private Authenticator authenticator;
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		MDC.clear();
		MDC.put("tresor-component", "PolicyStore");
		
		// get cleaned path params
		String[] params = this.getPathParams(request);
		
		// check request for validity
		if (!this.isRequestValid(request, response, params))
			return;
		
		String contenttype = null;
		String content = null;		
		
		if (params.length == 1) {			
			contenttype = ServletConstants.CONTENTTYPE_JSON;
			content = this.policyStoreHandler.retrieve(params[0]);
		}

		if (params.length == 2) {			
			contenttype = ServletConstants.CONTENTTYPE_XACML;
			content = this.policyStoreHandler.retrieve(params[0], params[1]);
		}
		
		if (content == null) {
			Helper.respondHTTP(true, HttpServletResponse.SC_NOT_FOUND, response);
		} else {
			Helper.respondHTTP(false, HttpServletResponse.SC_OK, contenttype, content, response);
		}
		
	}
	
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) {
		MDC.clear();
		MDC.put("tresor-component", "PolicyStore");
		
		// get cleaned path params
		String[] params = this.getPathParams(request);
		
		// check request for validity
		if (!this.isRequestValid(request, response, params))
			return;
		
		Reader reader = null;
		try {
			reader = Helper.getRequestInputStreamReader(request);
			boolean success = this.policyStoreHandler.put(params[0], params[1], reader);
			
			if (success) {
				// TODO 200
				Helper.respondHTTP(false, HttpServletResponse.SC_OK, response);
			} else {
				// TODO 500
				Helper.respondHTTP(true, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
			}				
			
		} catch (IOException e) {
			// TODO
			Helper.respondHTTP(true, HttpServletResponse.SC_BAD_REQUEST, response);
		} catch (XMLParserException e) {
			// TODO http 400
			Helper.respondHTTP(true, HttpServletResponse.SC_BAD_REQUEST, response);
		} catch (ParsingException e) {
			// TODO http 422
			Helper.respondHTTP(true, ServletConstants.SC_UNPROCESSABLE_ENTITY, response);
		}
		
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
		MDC.clear();
		MDC.put("tresor-component", "PolicyStore");
		
		// get cleaned path params
		String[] params = this.getPathParams(request);
		
		// check request for validity
		if (!this.isRequestValid(request, response, params))
			return;
		
		// check whether policy exists
		String policy = this.policyStoreHandler.retrieve(params[0], params[1]);		
		if (policy == null)
			Helper.respondHTTP(true, HttpServletResponse.SC_NOT_FOUND, response);
		
		// process deletion
		boolean success = this.policyStoreHandler.delete(params[0], params[1]);		
		if (success) {
			// TODO http 204
			Helper.respondHTTP(false, HttpServletResponse.SC_NO_CONTENT, response);
		} else {
			// TODO http 500
			Helper.respondHTTP(true, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
		}
		
	}
	
	private boolean isRequestValid(HttpServletRequest request, HttpServletResponse response, String[] params) {
		// check if get
		boolean isGet = request.getMethod().equalsIgnoreCase(ServletConstants.HTTP_GET);
		
		// check url params
		boolean isParamsValid = (isGet) ? (params.length == 1 || params.length == 2) : (params.length == 2);
		if (!isParamsValid) {
			// TODO http 400
			Helper.respondHTTP(true, HttpServletResponse.SC_BAD_REQUEST, response);
			return false;
		}
		
		// check authentication
		AuthenticatedUser user = this.authenticator.authenticate(request, response);
		if (user == null) {
			// 401 already returned from authenticator
			return false;
		}
		
		// check authorization
		boolean isAuthorized = false;
		if (params.length == 1)
			isAuthorized = user.isAuthorizedTo(request.getMethod(), params[0]);
		else
			isAuthorized = user.isAuthorizedTo(request.getMethod(), params[0], params[1]);
		
		if (!isAuthorized) {
			// TODO http 403
			Helper.respondHTTP(true, HttpServletResponse.SC_FORBIDDEN, response);
			return false;
		}
		
		// if put check contenttype
		boolean isValidContentType = true;
		if (request.getMethod().equalsIgnoreCase(ServletConstants.HTTP_PUT))
			isValidContentType = request.getContentType() != null && request.getContentType().contains(ServletConstants.CONTENTTYPE_XACML);
		
		if (!isValidContentType) {
			// TODO http 415
			Helper.respondHTTP(true, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,	ServletConstants.CONTENTTYPE_TEXTPLAIN,
					this.acceptContentTypes, response);
			return false;
		}
		
		// if no errors return true
		return true;
	}
	
	/**
	 * Checks validity of used path and returns array containing params, i.e. [0] => clientID (optional: [1] => serviceID)
	 * @param request the httpservletrequest
	 * @return array containing params or with length 0
	 */
	private String[] getPathParams(HttpServletRequest request) {
		String path = request.getPathInfo();
		String[] params = null;
		
		int arrayLength = 0;
		if (path != null && (params = path.split("/")) != null && params.length > 0)
			arrayLength = params.length - 1;
		
		// clean path so that array includes [0] => clientID (optionally: [1] => serviceID)
		String[] cleanParams = new String[arrayLength];
		for (int i = 0; i < cleanParams.length; i++)
			cleanParams[i] = params[i+1];
		
		return cleanParams;
	}
	
	@Override
	public void init() {		
		this.policyStoreHandler = ContextHandler.getInstance().getPolicyStoreHandler();
		this.authenticator = ContextHandler.getInstance().getAuthenticator();
	}
	
}
