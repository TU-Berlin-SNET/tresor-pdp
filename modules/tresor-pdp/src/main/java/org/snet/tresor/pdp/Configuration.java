package org.snet.tresor.pdp;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.snet.tresor.pdp.finder.impl.DBPolicyFinderModule;
import org.snet.tresor.pdp.finder.impl.LocationAttributeFinderModule;
import org.snet.tresor.pdp.policystore.RedisDBPolicyStoreManager;
import org.wso2.balana.Balana;
import org.wso2.balana.ConfigurationStore;
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
 * Loads, parses and prepares configuration
 * @author malik
 */
public class Configuration {
	private static final String TRESOR_PDP_CONFIG_PROPERTY = "org.snet.tresor.pdp.config";
	
	protected PDPConfig PDP_CONFIG;
	
    // the classloader we'll use for loading classes
    private ClassLoader loader;

    // the logger we'll use for all messages
    private static Log logger = LogFactory.getLog(ConfigurationStore.class);
	
	public Configuration() {
		this.PDP_CONFIG = this.createConfiguration();
		
		

	}
		
	private PDPConfig createConfig(String configpath) {
		String config = System.getProperty(TRESOR_PDP_CONFIG_PROPERTY);
		
		File file = new File(null);
		
		if (config == null)
			this.PDP_CONFIG = Balana.getInstance().getPdpConfig(); 
		else
			this.PDP_CONFIG = createConfig(config);
		
		PDPConfig config = null;
		File configFile = new File(configpath);
		
		if (configFile.isFile()) {
			JSONTokener tokener = new JSONTokener(new FileInputStream(configFile));
		} else {
			config = Balana.getInstance().getPdpConfig();
		}
		
		return config;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private static PDPConfig PDPCONFIG = null;
	
	private static AttributeFinderModule[] ATTRIBUTEFINDERMODULES = { new CurrentEnvModule(), 
															  		  new SelectorModule()/*,
															  		  getLocationAttributeFinderModule()*/
																	};
		
	private static PolicyFinderModule[] POLICYFINDERMODULES = { new DBPolicyFinderModule(
																		RedisDBPolicyStoreManager.getInstance()) };
	
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
	
	public static
	
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
