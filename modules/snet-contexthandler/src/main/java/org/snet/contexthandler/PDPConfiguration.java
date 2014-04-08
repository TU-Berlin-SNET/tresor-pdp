package org.snet.contexthandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import org.snet.finder.impl.LocationAttributeFinderModule;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.ResourceFinder;
import org.wso2.balana.finder.ResourceFinderModule;
import org.wso2.balana.finder.impl.CurrentEnvModule;
import org.wso2.balana.finder.impl.SelectorModule;

/**
 * Replacement for config.xml
 * @author malik
 */
public class PDPConfiguration {
	private static PDPConfig PDPCONFIG = null;
	
	private static AttributeFinderModule[] ATTRIBUTEFINDERMODULES = { new CurrentEnvModule(), 
															  		  new SelectorModule(),
															  		  getLocationAttributeFinderModule()
																	};
		
	private static PolicyFinderModule[] POLICYFINDERMODULES = { };
	
	private static ResourceFinderModule[] RESOURCEFINDERMODULES = {	};
	
	private static LocationAttributeFinderModule locModule;
	private static LocationAttributeFinderModule getLocationAttributeFinderModule() {
		if (locModule == null) {
			locModule = new LocationAttributeFinderModule();
			
			locModule.addPIP("wifiSSID", "http://localhost:8080/wifiPIP");
			locModule.addPIP("position", "http://localhost:8080/wifiPIP");
			locModule.addPIP("timestamp-wifi", "http://localhost:8080/wifiPIP");
			locModule.addPIP("timestamp-geo", "http://localhost:8080/wifiPIP");			
		}
		
		return locModule;
	}
	
	/**
	 * @return a PDPConfig as specified in PDPConfiguration
	 */
	public static PDPConfig getPDPConfig() {
		if (PDPCONFIG == null)
			initConfig();
		return PDPCONFIG;
	}
	
	/**
	 * Initializes and sets the PDPConfig
	 */
	private static void initConfig() {
		
		// prepare attributefinder
		AttributeFinder attributeFinder = new AttributeFinder();
		attributeFinder.setModules(new ArrayList<AttributeFinderModule>(
				Arrays.asList(ATTRIBUTEFINDERMODULES)));
		
		// prepare policyfinder
		PolicyFinder policyFinder = new PolicyFinder();
		policyFinder.setModules(new HashSet<PolicyFinderModule>(
				Arrays.asList(POLICYFINDERMODULES)));		
		
		// prepare resourcefinder
		ResourceFinder resourceFinder = new ResourceFinder();
		resourceFinder.setModules(new ArrayList<ResourceFinderModule>(
				Arrays.asList(RESOURCEFINDERMODULES)));
		
		// create pdpConfig
		PDPCONFIG = new PDPConfig(attributeFinder, policyFinder, resourceFinder);		
	}
	
}
