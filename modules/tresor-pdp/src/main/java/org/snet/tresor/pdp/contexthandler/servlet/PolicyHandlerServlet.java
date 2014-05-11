package org.snet.tresor.pdp.contexthandler.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.snet.tresor.pdp.contexthandler.handler.PolicyHandler;

/**
 * A simple Servlet which provides a restful interface for getting, adding and removing of policies.
 * @author malik
 */
public class PolicyHandlerServlet extends HttpServlet {

	PolicyHandler policyHandler;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		try {			
			String res = this.policyHandler.handleGet(request.getReader());
			response.setStatus(200);
			response.setContentType("text/plain");
			response.setContentLength(res.length());
			response.getWriter().write(res);
			response.getWriter().flush();
		} catch (Exception e) {
			response.setStatus(400);
			e.printStackTrace();
		}
	}
	
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) {
		try {			
			this.policyHandler.handlePut(request.getInputStream());
			response.setStatus(200);
		} catch (Exception e) {
			response.setStatus(400);
			e.printStackTrace();
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
		try {
			this.policyHandler.handleDelete(request.getReader(), request.getContentLength());			
			response.setStatus(200);
		} catch (Exception e) {
			response.setStatus(400);
			e.printStackTrace();
		}
	}
	
	public void init() throws ServletException {
		this.policyHandler = PolicyHandler.getInstance();
		super.init();
	}

}
