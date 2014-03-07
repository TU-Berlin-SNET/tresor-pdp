package org.snet.rest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple Servlet which responds to GET-requests as is specified in
 * the "REST Profile of XACML v3.0" specification
 * @author malik
 */
public class HomeServlet extends HttpServlet {

	private String resourcesXML = "<resources xmlns='http://ietf.org/ns/home-documents'	xmlns:atom='http://www.w3.org/2005/Atom'>"
			+ "<resource rel='http://docs.oasis-open.org/ns/xacml/relation/pdp'>"
			+ "<atom:link href='/rest/pdp' />"
			+ "</resource></resources>";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setStatus(200);
		response.setContentType("application/xml");
		response.setContentLength(resourcesXML.length());
		response.getWriter().println(resourcesXML);
		response.getWriter().flush();
	}
	
	public HomeServlet() throws ServletException {
		this.init();
	}	
	
}
