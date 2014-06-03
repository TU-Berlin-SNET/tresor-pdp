package org.snet.tresor.pdp.contexthandler.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.TresorPDP;
import org.snet.tresor.pdp.contexthandler.ContextHandler;

/**
 * A simple Servlet which provides a restful interface for getting, adding and removing of policies.
 * @author malik
 */
public class PolicyHandlerServlet extends HttpServlet {

	private ContextHandler contextHandler;
	
	// TODO adjust to new structure
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {		
		JSONObject responseJSON = this.contextHandler.handle(request, response);
		Helper.respondHTTP(responseJSON, response);
	}
	
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) {
		JSONObject responseJSON = this.contextHandler.handle(request, response);
		Helper.respondHTTP(responseJSON, response);
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
		JSONObject responseJSON = this.contextHandler.handle(request, response);
		Helper.respondHTTP(responseJSON, response);
	}
	
	@Override
	public void init() {		
		this.contextHandler = TresorPDP.getInstance().getContextHandler();		
	}
	
}
