package org.snet.contexthandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.snet.saml.SAMLConfig;
import org.w3c.dom.Document;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.impl.InMemoryPolicyFinderModule;

public class Helper {

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
        
    private static void initEngine() {
    	if (!initialized) {
        	try {
        		GeoXACML.initialize();
        		SAMLConfig.InitSAML();
        		Balana.getInstance().setPdpConfig(PDPConfiguration.getPDPConfig());
        		initialized = true;
        	} catch (Exception e) {
        		initialized = false;
        	}    		
    	}
    }
    
}
