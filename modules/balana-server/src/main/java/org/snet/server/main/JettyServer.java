package org.snet.server.main;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.snet.server.servlet.GetServlet;
import org.snet.server.servlet.PostServlet;

public class JettyServer {

	public static void main(String[] args) throws Exception {
		// init servletholders
		ServletHolder getHolder = new ServletHolder(new GetServlet());
		ServletHolder postHolder = new ServletHolder(new PostServlet());
		
		// init servletcontexthandler with paths for REST
		ServletContextHandler servletContextHandler = new ServletContextHandler();
		servletContextHandler.addServlet(getHolder, "/rest");
		servletContextHandler.addServlet(postHolder, "/rest/pdp");
		
		// finally initialize and run the jetty server
		Server server = new Server(8080);
		server.setHandler(servletContextHandler);
		server.start();
		server.join();
	}

}
