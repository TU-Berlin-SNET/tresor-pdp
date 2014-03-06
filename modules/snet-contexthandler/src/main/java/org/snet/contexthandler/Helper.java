package org.snet.contexthandler;

import java.util.HashSet;
import java.util.LinkedList;
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

	static boolean initialized = false;
	
	public static void initEngine() {
		try {
			GeoXACML.initialize();
			SAMLConfig.InitSAML();
			initialized = true;
		} catch (Exception e) {
			initialized = false;
		}
	}
	
	public static PDP getPDP() {
		if (!initialized) {
			initEngine();
		}
		return Helper.getPDP(new LinkedList<Document>());
//		return new PDP(Balana.getInstance().getPdpConfig());		
	}
	
    /**
     * Returns a new PDP instance with new XACML policies
     * 
     * @param policies, a list of policies
     * @return a PDP instance
     */
    public static PDP getPDP(List<Document> policies) {

    	if (!initialized) {
    		initEngine();
    	}
    	
        PolicyFinder finder= new PolicyFinder();        
        
        InMemoryPolicyFinderModule testPolicyFinderModule = new InMemoryPolicyFinderModule(policies);
        Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();
        policyModules.add(testPolicyFinderModule);
        finder.setModules(policyModules);
        
        Balana balana = Balana.getInstance();
        PDPConfig pdpConfig = balana.getPdpConfig();
        pdpConfig = new PDPConfig(pdpConfig.getAttributeFinder(), finder,
                                                            pdpConfig.getResourceFinder(), true);
        return new PDP(pdpConfig);
    }
	
	

}
