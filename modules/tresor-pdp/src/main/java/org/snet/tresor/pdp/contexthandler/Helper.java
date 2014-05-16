package org.snet.tresor.pdp.contexthandler;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.snet.tresor.pdp.Configuration;
import org.snet.tresor.pdp.contexthandler.saml.SAMLConfig;
import org.snet.tresor.pdp.finder.impl.LocationAttributeFinderModule;
import org.w3c.dom.Document;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.impl.InMemoryPolicyFinderModule;

/**
 * Helper-Class for interfacing with Balana and PDP
 * @author malik
 */
public class Helper {

	public static URI CATEGORY_SUBJECT_URI;
	public static URI CATEGORY_RESOURCE_URI;
	public static URI DATATYPE_STRING_URI;
	public static URI ID_DOMAIN_URI;
	public static URI ID_SERVICE_URI;
	
	static {
		try {
			CATEGORY_SUBJECT_URI = new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
			CATEGORY_RESOURCE_URI = new URI("urn:oasis:names:tc:xacml:3.0:attribute-category:resource");				
			DATATYPE_STRING_URI = new URI("http://www.w3.org/2001/XMLSchema#string");
			ID_DOMAIN_URI = new URI("domain");
			ID_SERVICE_URI = new URI("service");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static boolean initialized = false;
	
	/**
	 * @return a PDP instance with base pdp configuration (see PDPConfiguration.java)
	 */
	public static PDP getPDP() {
		if (!initialized)
			initEngine();
		
		return new PDP(Balana.getInstance().getPdpConfig());
	}
	
    /**
     * @param policyDocuments, a list of policy documents
     * @return a PDP instance with given policies
     */
    public static PDP getPDP(List<Document> policyDocuments) {

    	if (!initialized)
    		initEngine();
    	    	
    	InMemoryPolicyFinderModule policyFinderModule = new InMemoryPolicyFinderModule(policyDocuments);    	    	
        Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();
        policyModules.add(policyFinderModule);
        
        PolicyFinder policyFinder = new PolicyFinder();
        policyFinder.setModules(policyModules);        
        
        PDPConfig pdpConfig = Balana.getInstance().getPdpConfig();
        pdpConfig = new PDPConfig(pdpConfig.getAttributeFinder(), policyFinder, pdpConfig.getResourceFinder(), true);
        
        return new PDP(pdpConfig);
    }    
    
    public static void clearCaches() {
    	LocationAttributeFinderModule.clearThreadCache();
    }
    
    /**
     * Initializes the engine with additional geoxacml and saml support, also sets new pdpconfiguration
     */
    private static void initEngine() {
    	if (!initialized) {
        	try {
        		GeoXACML.initialize();
        		SAMLConfig.InitSAML();
        		Balana.getInstance().setPdpConfig(Configuration.getPDPConfig());
        		initialized = true;
        	} catch (Exception e) {
        		e.printStackTrace();
        		initialized = false;
        	}    		
    	}
    }
    
}
