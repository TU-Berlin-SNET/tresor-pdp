package org.snet.tresor.pdp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.snet.tresor.pdp.contexthandler.saml.SAMLConfig;
import org.snet.tresor.pdp.policystore.PolicyStore;
import org.snet.tresor.pdp.policystore.PolicyStoreManager;
import org.wso2.balana.Balana;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.ResourceFinder;
import org.wso2.balana.finder.ResourceFinderModule;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;
import org.wso2.balana.finder.impl.InMemoryPolicyFinderModule;

/**
 * Loads, parses and prepares configuration
 * @author malik
 */
public class Configuration {
	// System Property Key of the config File (if applicable)
	private static final String TRESOR_PDP_CONFIG_PROPERTY = "org.snet.tresor.pdp.config";
	
	private PDPConfig PDP_CONFIG;	
	
    // the classloader we'll use for loading classes
    private ClassLoader loader;

    // the logger we'll use for all messages
    private static Log logger = LogFactory.getLog(Configuration.class);
	
	public Configuration() {
		this.loader = getClass().getClassLoader();
		this.initConfiguration();
	}
	
	/**
	 * Loads and parses the configuration file if applicable, loads default configuration otherwise
	 * Initializes GeoXACML and SAML support
	 */
	private void initConfiguration() {
		String configPath = System.getProperty(TRESOR_PDP_CONFIG_PROPERTY);
		
		GeoXACML.initialize();
		logger.info("GeoXACML: success");
		
		try {
			SAMLConfig.InitSAML();
			logger.info("SAML: success");
		} catch (Exception e) {
			logger.error("SAML: fail", e);
		}
		
		// if there is a configuration file..
		if (configPath != null) {
			try {
				// ..parse it..
				JSONTokener tokener = new JSONTokener(new FileInputStream(new File(configPath)));
				JSONObject configJSON = new JSONObject(tokener);
				
				// ..read pdp configuration..
				this.PDP_CONFIG = parsePDPConfig(configJSON);
				
				// ..read further configuration if available..
				if (configJSON.has("policystoremanager"))
					parsePolicyStoreManager(configJSON.getJSONObject("policystoremanager"));

				logger.info("Configuration read successfully");

			} catch (FileNotFoundException e) {
				logger.error("Could not find or open configuration file", e);
			} catch (JSONException e) {
				logger.error("Error parsing config file or getting a value from parsed JSONObject", e);
			} catch (ReflectiveOperationException e) {
				logger.error("Error doing reflective operation", e);
			}
		}
		
		// if there is no configuration file or parsing did not go as planned, go to default
		if (this.PDP_CONFIG == null) {
			logger.info("Using default configuration");
			this.PDP_CONFIG = Balana.getInstance().getPdpConfig();
		}
		
		Balana.getInstance().setPdpConfig(this.PDP_CONFIG);
	}
	
	/**
	 * Reads and creates a PDPConfig from given JSONObject
	 * @param configJSON, JSONObject containing configuration details
	 * @return created PDPConfig
	 * @throws ReflectiveOperationException 
	 * @throws JSONException 
	 */
	private PDPConfig parsePDPConfig(JSONObject configJSON) throws JSONException, ReflectiveOperationException {		
		List<AttributeFinderModule> attributeFinderModules = new ArrayList<AttributeFinderModule>();
		Set<PolicyFinderModule> policyFinderModules = new HashSet<PolicyFinderModule>();
		List<ResourceFinderModule> resourceFinderModules = new ArrayList<ResourceFinderModule>();		
		
		createInstances(configJSON.getJSONArray("attributefindermodules"), 
						attributeFinderModules, 
						AttributeFinderModule.class);
		
		createInstances(configJSON.getJSONArray("policyfindermodules"), 
						policyFinderModules, 
						PolicyFinderModule.class);
		
		createInstances(configJSON.getJSONArray("resourcefindermodules"), 
						resourceFinderModules, 
						ResourceFinderModule.class);
		
		AttributeFinder attributeFinder = new AttributeFinder();
		attributeFinder.setModules(attributeFinderModules);
		
		PolicyFinder policyFinder = new PolicyFinder();
		policyFinder.setModules(policyFinderModules);
		
		ResourceFinder resourceFinder = new ResourceFinder();
		resourceFinder.setModules(resourceFinderModules);
		
		return new PDPConfig(attributeFinder, policyFinder, resourceFinder);		
	}

	/**
	 * Parses PolicyStoreManager Configuration details and sets the manager for the PolicyStore
	 * @param managerConfig, JSONObject containing classname and optional parameters for the policystoremanager
	 * @throws ReflectiveOperationException
	 */
	private void parsePolicyStoreManager(JSONObject managerConfig) throws ReflectiveOperationException {
		PolicyStoreManager manager;
		
		if (managerConfig.has("parameters"))
			manager = createInstanceWithParameters(managerConfig, PolicyStoreManager.class);
		else
			manager = createInstance(managerConfig, PolicyStoreManager.class);
		
		PolicyStore.setManager(manager);
	}
	
	/**
	 * Creates instances of given class with given configuration, optionally with parameters,
	 * and puts them into the output collection
	 * @param classes, JSONArray containing JSONObjects with classnames (and optionally parameters)
	 * @param output, the collection to output to
	 * @param cls, the type of class to create
	 * @throws ReflectiveOperationException
	 */
	private <T> void createInstances(JSONArray classes, Collection<T> output, Class<T> cls) throws ReflectiveOperationException {
		JSONObject j;
		T instance;
		
		for (int i = 0; i < classes.length(); i++) {
			instance = null;
			j = classes.getJSONObject(i);
			
			if (j.has("parameters"))
				instance = createInstanceWithParameters(j, cls);
			else
				instance = createInstance(j, cls);
			
			if (instance != null)
				output.add(instance);			
		}
	}
	
	/**
	 * Creates an instance of the class with classname inside given JSONObject 
	 * and type of given class via the default constructor 
	 * @param j, JSONObject containing classname and no parameters
	 * @param cls, type of class to create
	 * @return created instance
	 * @throws ReflectiveOperationException
	 */
	private <T> T createInstance(JSONObject j, Class<T> cls) throws ReflectiveOperationException {
		Class<?> c = this.loader.loadClass(j.getString("classname"));		
		return cls.cast(c.newInstance());
	}
	
	/**
	 * Creates an instance of the class with classname inside given JSONObject
	 * and type of given class via a constructor which takes varargs
	 * @param j, JSONObject containing classname and additional parameters JSONArray
	 * @param cls, type of class to create
	 * @return created instance
	 * @throws ReflectiveOperationException
	 */
	private <T> T createInstanceWithParameters(JSONObject j, Class<T> cls) throws ReflectiveOperationException {
		Class<?> c = this.loader.loadClass(j.getString("classname"));
		Object instance;		
		
		JSONArray paramsJSON = j.getJSONArray("parameters");
		String[] params = new String[paramsJSON.length()];				
		for (int k = 0; k < paramsJSON.length(); k++)
			params[k] = paramsJSON.getString(k);				
		
		// necessary workaround, May 13 2014
		Object[] wrappedParams = { params };
		
		// first two for compatibility with balana, third one for our own modules
		if (c == FileBasedPolicyFinderModule.class)
			instance = c.getConstructor(Set.class).newInstance(new HashSet<String>(Arrays.asList(params)));
		else if (c == InMemoryPolicyFinderModule.class)
			instance = c.getConstructor(List.class).newInstance(Arrays.asList(params));
		else
			instance = c.getConstructor(String[].class).newInstance(wrappedParams);					
		
		return cls.cast(instance);
	}
	
}
