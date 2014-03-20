package org.snet.rest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.snet.contexthandler.ContextHandler;

/**
 * A simple Servlet which responds to POST-requests as is specified in
 * the "REST Profile of XACML v3.0" specification
 * @author malik
 */
public class PDPServlet extends HttpServlet {
	
	ContextHandler contextHandler;
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String s = null;
		
		s = this.contextHandler.handle(request.getReader());
		
		if (s != null && !s.isEmpty()) {
			response.setStatus(200);
			response.setContentType("application/xml");
			response.setContentLength(s.length());
			response.getWriter().print(s);
			response.getWriter().flush();
		} else {
			response.setStatus(400);
		}		
	}
	
	public void init() throws ServletException {
		this.contextHandler = ContextHandler.getInstance();
		super.init();		
	}
	
}
