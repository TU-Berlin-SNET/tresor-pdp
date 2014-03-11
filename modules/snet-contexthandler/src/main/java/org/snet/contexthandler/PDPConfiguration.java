package org.snet.contexthandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.snet.tresor.finder.impl.GeoAttributeFinderModule;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.ResourceFinder;
import org.wso2.balana.finder.ResourceFinderModule;
import org.wso2.balana.finder.impl.CurrentEnvModule;
import org.wso2.balana.finder.impl.SelectorModule;

public class PDPConfiguration {
	
	private static PDPConfig PDPCONFIG = null;	
	
	private static AttributeFinderModule[] attributeFinderModules = { new CurrentEnvModule(), 
															  		  new SelectorModule(),
															  		  new GeoAttributeFinderModule()
	};
	
	private static PolicyFinderModule[] policyFinderModules = { };
	
	private static ResourceFinderModule[] resourceFinderModules = {	};	
	
	public static PDPConfig getPDPConfig() {
		if (PDPCONFIG == null)
			initConfig();
		return PDPCONFIG;
	}
	
	private static void initConfig() {
		AttributeFinder attributeFinder = new AttributeFinder();
		attributeFinder.setModules(Arrays.asList(attributeFinderModules));
		
		PolicyFinder policyFinder = new PolicyFinder();
		Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>(
				Arrays.asList(policyFinderModules));
		policyFinder.setModules(policyModules);
		
		ResourceFinder resourceFinder = new ResourceFinder();
		resourceFinder.setModules(Arrays.asList(resourceFinderModules));
		
		PDPCONFIG = new PDPConfig(attributeFinder, policyFinder, resourceFinder, true);		
	}
	
}
