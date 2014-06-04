package org.snet.tresor.pdp.contexthandler.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.TresorPDP;
import org.snet.tresor.pdp.contexthandler.ContextHandler;

/**
 * A simple Servlet which responds to POST-requests as is specified in
 * the "REST Profile of XACML v3.0" specification
 * @author malik
 */
public class PDPServlet extends HttpServlet {
	
	private ContextHandler contextHandler;
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		JSONObject responseJSON = this.contextHandler.handle(request, response);
		Helper.respondHTTP(responseJSON, response);
	}
	
	@Override
	public void init() {		
		this.contextHandler = TresorPDP.getInstance().getContextHandler();		
	}
	
}
