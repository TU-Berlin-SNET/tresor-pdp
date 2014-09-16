package org.snet.tresor.pdp.contexthandler.servlet;

import java.io.IOException;
import java.io.Reader;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

		// get parameters from request path
		String[] params = this.getPathParams(request);

		// check request for validity, authentication and authorization
		if (!this.isRequestValid(request, response, params))
			return;

		MDC.put("category", "Policy Retrieval");

		String contenttype = null;
		String content = null;

		// e.g. request in this form: /policy/:clientID
		if (params.length == 1) {
			log.info("Accepted valid GET request for client {}", params[0]);
			contenttype = ServletConstants.CONTENTTYPE_JSON;
			content = this.policyStoreHandler.retrieve(params[0]);
		}

		// e.g. request in this form: /policy/:clientID/:serviceID
		if (params.length == 2) {
			log.info("Accepted valid GET request for client {} and service {}", params[0], params[1]);
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

		// get parameters from request path
		String[] params = this.getPathParams(request);

		// check request for validity, authentication and authorization
		if (!this.isRequestValid(request, response, params))
			return;

		MDC.put("category", "Policy Insertion");
		log.info("Accepted valid PUT request for client {} and service {}", params[0], params[1]);

		Reader reader = null;
		try {
			reader = Helper.getRequestInputStreamReader(request);
			boolean success = this.policyStoreHandler.put(params[0], params[1], reader);

			if (success) {
				Helper.respondHTTP(false, HttpServletResponse.SC_OK, response);
			} else {
				Helper.respondHTTP(true, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
			}

		} catch (IOException e) {
			// TODO http 400
			log.warn("Failed to retrieve Reader for request", e);
			Helper.respondHTTP(true, HttpServletResponse.SC_BAD_REQUEST, response);
		} catch (XMLParserException e) {
			// TODO http 400
			log.debug("Malformed XML, parsing failed", e);
			Helper.respondHTTP(true, HttpServletResponse.SC_BAD_REQUEST, response);
		} catch (ParsingException e) {
			// TODO http 422
			log.debug("Malformed XACML, processing failed", e);
			Helper.respondHTTP(true, ServletConstants.SC_UNPROCESSABLE_ENTITY, response);
		}

	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
		MDC.clear();
		MDC.put("tresor-component", "PolicyStore");

		// get parameters from request path
		String[] params = this.getPathParams(request);

		// check request for validity, authentication and authorization
		if (!this.isRequestValid(request, response, params))
			return;

		MDC.put("category", "Policy Deletion");
		log.info("Accepted valid DELETE request for client {} and service {}", params[0], params[1]);

		// check whether policy exists
		String policy = this.policyStoreHandler.retrieve(params[0], params[1]);
		if (policy == null) {
			Helper.respondHTTP(true, HttpServletResponse.SC_NOT_FOUND, response);
		}

		// process deletion
		boolean success = this.policyStoreHandler.delete(params[0], params[1]);
		if (success) {
			Helper.respondHTTP(false, HttpServletResponse.SC_NO_CONTENT, response);
		} else {
			Helper.respondHTTP(true, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response);
		}

	}

	private boolean isRequestValid(HttpServletRequest request, HttpServletResponse response, String[] params) {

		// check authentication
		AuthenticatedUser user = this.authenticator.authenticate(request, response);
		if (user == null) {
			MDC.put("category", "Authentication/Authorization");
			log.info("Authentication failed");
			// 401 already returned from authenticator so just return
			return false;
		}

		log.debug("Authentication successful");

		// add information for logging purposes
		MDC.put("client-id", user.getClientID());
		MDC.put("subject-id", user.getName());
		log.debug("Client-ID and Subject-ID now available");

		// check if request is Get, important for url params validation
		String method = request.getMethod();
		boolean isGet = method.equalsIgnoreCase(ServletConstants.HTTP_GET);

		// check validity of url params
		boolean isParamsValid = (isGet) ? (params.length == 1 || params.length == 2) : (params.length == 2);
		if (!isParamsValid) {
			Helper.respondHTTP(true, HttpServletResponse.SC_BAD_REQUEST, response);
			return false;
		}

		log.debug("Url Parameters are valid");

		// check authorization
		boolean isAuthorized = (params.length == 1) ? user.isAuthorizedTo(method, params[0]) : user.isAuthorizedTo(method, params[0], params[1]);
		if (!isAuthorized) {
			// insufficient authorization, log and error response
			MDC.put("category", "Authentication/Authorization");
			log.info("Insufficient authorization for {} request to resource of client {}", method, params[0]);
			Helper.respondHTTP(true, HttpServletResponse.SC_FORBIDDEN, response);
			return false;
		}

		log.debug("Authorization successful");

		// if put request, check validity of contenttype
		boolean isPut = method.equalsIgnoreCase(ServletConstants.HTTP_PUT);
		if (isPut) {
			log.debug("Request is put");
			String contenttype = request.getContentType();
			if (contenttype == null || !contenttype.contains(ServletConstants.CONTENTTYPE_XACML)) {
				log.debug("Contenttype is invalid");
				Helper.respondHTTP(true, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,	ServletConstants.CONTENTTYPE_TEXTPLAIN,
						this.acceptContentTypes, response);
				return false;
			}
		}

		log.debug("Request is valid");

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
		log.info("PolicyStoreServlet initialized");
	}

}
