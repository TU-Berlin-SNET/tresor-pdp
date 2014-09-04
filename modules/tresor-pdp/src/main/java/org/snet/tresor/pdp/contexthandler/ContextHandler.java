package org.snet.tresor.pdp.contexthandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.Configuration;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.TresorPDP;
import org.snet.tresor.pdp.contexthandler.authentication.Authenticator;
import org.snet.tresor.pdp.contexthandler.handler.Handler;

/**
 * Class for initializing ContextHandler system, API,  
 * handling incoming requests and providing them to the appropriate handler
 */
public class ContextHandler {
	private static final Logger log = LoggerFactory.getLogger(ContextHandler.class);
	
	private static ContextHandler INSTANCE;
	
	private Map<String, Handler> handlers;
	
	private boolean initialized;
	
	public static synchronized ContextHandler getInstance() {
		if (INSTANCE == null)
			INSTANCE = new ContextHandler();
		return INSTANCE;
	}
	
	private ContextHandler() {
		this.initialized = false;
		this.handlers = new HashMap<String, Handler>();
	}
	
	private void setHandlers(Map<String, Handler> handlers) {
		this.handlers = handlers;
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
	
	public synchronized void initialize() {
		
		if (this.initialized)
			return;
		
		try {
			log.info("begin contexthandler initialization");
			Configuration conf = Configuration.getInstance();
			
			TresorPDP tresorPDP = new TresorPDP(conf.getConfig("tresor-pdp"));
			Authenticator authenticator = Helper.createInstance(conf.getConfig("authenticator"), Authenticator.class);
			
			JSONObject ctxConf = conf.getConfig("contexthandler");						
			List<Handler> handlerList = new ArrayList<Handler>(); 
			Helper.createInstances(ctxConf.getJSONArray("handlers"), handlerList, Handler.class);
			
			Map<String, Handler> handlers = new HashMap<String, Handler>();
			for (Handler h : handlerList) {
				if (h.getResourceName().equalsIgnoreCase("pdp")) {
					h.setComponent("pdp", tresorPDP.getPDP());
				}
				
				if (h.getResourceName().equalsIgnoreCase("policy")) {
					h.setComponent("policy", tresorPDP.getPolicyStore());
					h.setComponent("authenticator", authenticator);
				}
				
				log.info("Registering new handler for resource {}", h.getResourceName());
				handlers.put(h.getResourceName(), h);
			}
						
			this.setHandlers(handlers);
			
		} catch (IOException e) {
			// TODO log
			log.info("IOException happened.", e);
		} catch (JSONException e) {			
			// TODO log
			log.info("JSONException happened.", e);
		} catch (Exception e) {
			// TODO log
			log.info("Exception happened.", e);
		}
		
		this.initialized = true;
	}
	
}
