package org.snet.test.server;

import java.security.ProtectionDomain;
import java.util.LinkedList;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.geotools.xacml.geoxacml.attr.proxy.GeometryAttributeProxy;
import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.opensaml.xml.parse.BasicParserPool;
import org.snet.contexthandler.ContextHandler;
import org.snet.rest.GetServlet;
import org.snet.rest.PostServlet;
import org.snet.saml.SAMLConfig;
import org.snet.test.TestUtils;
import org.w3c.dom.Document;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.attr.AttributeFactory;
import org.wso2.balana.attr.BaseAttributeFactory;
import org.wso2.balana.attr.StandardAttributeFactory;

public class TestServer {

	public static void main(String[] args) throws Exception {
		Balana balana = Balana.getInstance();
		GeoXACML.initialize();
		SAMLConfig.InitSAML();
		
//		balana.getAttributeFactory().addDatatype(GeometryAttribute.identifier, new GeometryAttributeProxy());
		
		AttributeFactory sattr = StandardAttributeFactory.getNewFactory();
		sattr.addDatatype(GeometryAttribute.identifier, new GeometryAttributeProxy());
		balana.setAttributeFactory(sattr);
		
		PDP pdp = TestUtils.getPDPNewInstance(new LinkedList<Document>());
		BasicParserPool pp = new BasicParserPool();		
		ContextHandler cx = new ContextHandler(pp, pdp);
		PolicyHandler ph = new PolicyHandler(cx);
		
		// init servletholders
		ServletHolder getHolder = new ServletHolder(new GetServlet());
		ServletHolder postHolder = new ServletHolder(new PostServlet(cx));
		ServletHolder policyServletHolder = new ServletHolder(new PolicyHandlerServlet(ph));
		
		// init servletcontexthandler with paths for REST
		ServletContextHandler servletContextHandler = new ServletContextHandler();
		servletContextHandler.addServlet(getHolder, "/rest");
		servletContextHandler.addServlet(postHolder, "/rest/pdp");
		servletContextHandler.addServlet(policyServletHolder, "/rest/policyhandler");
		
		Server server = new Server(9090);
		server.setHandler(servletContextHandler);
		server.start(); 
		server.join();		
	}

}
