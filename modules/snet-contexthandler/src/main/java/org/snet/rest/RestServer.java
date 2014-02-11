package org.snet.rest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.snet.contexthandler.ContextHandler;

/**
 * Very simple Jetty REST Server implementation
 * @author malik
 */
public class RestServer {

	Server server;
	ContextHandler contextHandler;
	
	public RestServer(ContextHandler contextHandler) {
		this.contextHandler = contextHandler;
	}
	
	/**
	 * Sets up and starts server bound to given port
	 * @param port, the port to bind the server to
	 * @throws Exception
	 */
	public void init(int port) throws Exception {
		// init servletholders
		ServletHolder getHolder = new ServletHolder(new GetServlet());
		ServletHolder postHolder = new ServletHolder(new PostServlet(this.contextHandler));
		
		// init servletcontexthandler with paths for REST
		ServletContextHandler servletContextHandler = new ServletContextHandler();
		servletContextHandler.addServlet(getHolder, "/rest");
		servletContextHandler.addServlet(postHolder, "/rest/pdp");
		
		// finally initialize and run the jetty server
		this.server = new Server(port);
		this.server.setHandler(servletContextHandler);
		this.server.start();
		this.server.join();
	}
	
	/**
	 * Stops the Server
	 * @throws Exception
	 */
	public void stop() throws Exception {
		if (this.server != null && this.server.isRunning())
			this.server.stop();
	}

}
