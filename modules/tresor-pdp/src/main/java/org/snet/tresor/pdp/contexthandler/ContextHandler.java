package org.snet.tresor.pdp.contexthandler;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.contexthandler.handler.Handler;

/**
 * Simple Class for handling incoming requests and providing them to the appropriate handler
 */
public class ContextHandler {
	private static Log log = LogFactory.getLog(ContextHandler.class);
	private static ContextHandler INSTANCE;
	
	private Map<String, Handler> handlers;	
	
	public static ContextHandler getInstance() {
		if (INSTANCE == null)
			INSTANCE = new ContextHandler();
		return INSTANCE;
	}
	
	private ContextHandler() {
		this.handlers = new HashMap<String, Handler>();
	}

	/**
	 * Pass request to corresponding handler
	 * @param request the httpservlet request
	 * @param response the httpservlet response
	 */
	public JSONObject handle(HttpServletRequest request, HttpServletResponse response) {
		String[] params = request.getRequestURI().split("/");
		JSONObject responseJSON = null;
		
		// minimum two parameters (first is usually blank, second is resource)
		if (params.length > 1) {
			String resource = params[1].toLowerCase();
			Handler handler = this.handlers.get(resource);
			
			if (handler != null) {
				responseJSON = handler.handle(request, response);
			} else {
				log.info("No handler found for resource: " + resource);
				responseJSON = Helper.createResponseJSON(true, HttpServletResponse.SC_NOT_FOUND);
			}
		}

		return responseJSON;
	}
	
	/**
	 * Associates the given resource with the given handler, replaces previous associations if applicable
	 * This method is NOT THREAD-SAFE and should only be used on startup to add handlers
	 * @param resource name of the resource
	 * @param handler handler handling request on that resource
	 */
	public void putHandler(String resource, Handler handler) {
		this.handlers.put(resource, handler);
	}
	
}
