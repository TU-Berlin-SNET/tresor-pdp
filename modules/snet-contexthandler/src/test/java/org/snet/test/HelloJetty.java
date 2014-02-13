package org.snet.test;
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.geotools.xacml.geoxacml.attr.proxy.GeometryAttributeProxy;
import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.opensaml.xml.parse.BasicParserPool;
import org.snet.contexthandler.ContextHandler;
import org.snet.rest.RestServer;
import org.snet.saml.SAMLConfig;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;

/**
 * Class used for on the fly testing during development, WILL be changed very frequently 
 * @author malik
 */
public class HelloJetty {

	public static void main(String[] args) throws Exception {
		Balana balana = Balana.getInstance();
		GeoXACML.initialize();
		SAMLConfig.InitSAML();
		
		balana.getAttributeFactory().addDatatype(GeometryAttribute.identifier, new GeometryAttributeProxy());		
		
		PDP pdp = new PDP(balana.getPdpConfig());
		BasicParserPool pp = new BasicParserPool();
		
		ContextHandler cx = new ContextHandler(pp, pdp);
		
		RestServer server = new RestServer(cx);
		server.init(8080);
	}

}
