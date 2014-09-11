package org.snet.tresor.pdp.contexthandler.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.contexthandler.ContextHandler;

/**
 * A simple Servlet which provides a restful interface for getting, adding and removing of policies.
 * @author malik
 */
public class PolicyHandlerServlet extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(PolicyHandlerServlet.class);
	private ContextHandler contextHandler;
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		this.putMDCs(request);
		MDC.put("category", "Getting Policies");				
		
		log.info("GET request to PolicyStore received");
		
		JSONObject responseJSON = this.contextHandler.handle(request, response);
		Helper.respondHTTP(responseJSON, response);
		
		MDC.clear();
	}
	
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) {
		this.putMDCs(request);
		MDC.put("category", "Adding/Replacing Policies");
		
		log.info("PUT request to PolicyStore received");
		
		JSONObject responseJSON = this.contextHandler.handle(request, response);
		Helper.respondHTTP(responseJSON, response);
		
		MDC.clear();
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
		this.putMDCs(request);
		MDC.put("category", "Deleting policies");
		
		log.info("DELETE request to PolicyStore received");
		
		JSONObject responseJSON = this.contextHandler.handle(request, response);
		Helper.respondHTTP(responseJSON, response);
		
		MDC.clear();
	}
	
	private void putMDCs(HttpServletRequest request) {
		MDC.put("tresor-component", "PolicyStore");
		MDC.put("client-id", request.getHeader("TRESOR-Organization"));
	}
	
	@Override
	public void init() {		
		this.contextHandler = ContextHandler.getInstance();		
	}
	
}
