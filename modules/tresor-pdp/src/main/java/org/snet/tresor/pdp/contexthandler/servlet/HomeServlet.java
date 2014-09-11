package org.snet.tresor.pdp.contexthandler.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.contexthandler.ContextHandler;

/**
 * A simple Servlet which responds to GET-requests as is specified in
 * the "REST Profile of XACML v3.0" specification
 * Also initializes Configuration
 * @author malik
 */
public class HomeServlet extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(HomeServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {		
		Helper.respondHTTP(false, HttpServletResponse.SC_OK,
				ServletConstants.CONTENTTYPE_XML, ServletConstants.HOME_DISCOVERY_XML,
				response);
	}
	
	@Override
	public void init() {
		ContextHandler ctxHandler = ContextHandler.getInstance();
		try {
			ctxHandler.initialize();
		} catch (Exception e) {
			MDC.put("tresor-component", "PDP");
			MDC.put("category", "Initialization");
			
			// log and exit
			log.error("Initialization failed, shutting down!", e);
			
			MDC.clear();
			System.exit(1);
		}
	}
	
}
