package org.snet.tresor.pdp.contexthandler.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.snet.tresor.pdp.Helper;

/**
 * A simple Servlet which responds to GET-requests as is specified in
 * the "REST Profile of XACML v3.0" specification
 * @author malik
 */
public class HomeServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {		
		Helper.respondHTTP(false, HttpServletResponse.SC_OK, 
				ServletConstants.CONTENTTYPE_XML, ServletConstants.HOME_DISCOVERY_XML,
				response);
	}
	
}