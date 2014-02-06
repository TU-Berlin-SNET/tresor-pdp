package org.snet.test;
import org.snet.rest.RestServer;

/**
 * Class used for on the fly testing during development, WILL be changed very frequently 
 * @author malik
 */
public class HelloJetty {
	
	public static void main(String[] args) throws Exception {
		
		RestServer server = new RestServer();
		server.init(8080);
					
	}
	
}
