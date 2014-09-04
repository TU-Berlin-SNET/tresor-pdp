package org.snet.tresor.pdp.contexthandler.servlet;

import java.util.Enumeration;

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
 * A simple Servlet which responds to POST-requests as is specified in
 * the "REST Profile of XACML v3.0" specification
 * @author malik
 */
public class PDPServlet extends HttpServlet {	
	private static final Logger log = LoggerFactory.getLogger(PDPServlet.class);
	private ContextHandler contextHandler;
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		MDC.put("tresor-component", "PDP");
		MDC.put("category", "PDP");
		MDC.put("client-id", request.getHeader("TRESOR-Organization"));
		log.info("POST request to PDP received");
		
		// TODO remove
//		Enumeration<String> headers = request.getHeaderNames();		
//		while (headers.hasMoreElements()) {
//			String name = headers.nextElement();
//			log.info("Available Header: {}:{}", name, request.getHeader(name));
//		}			
		
		JSONObject responseJSON = this.contextHandler.handle(request, response);
		Helper.respondHTTP(responseJSON, response);
		Helper.clearCaches();
		
		MDC.clear();
	}
	
	@Override
	public void init() {
		this.contextHandler = ContextHandler.getInstance();
	}
	
}
