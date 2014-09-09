package org.snet.tresor.pdp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.contexthandler.ContextHandler;
import org.snet.tresor.pdp.contexthandler.authentication.Authenticator;
import org.snet.tresor.pdp.contexthandler.handler.Handler;
import org.snet.tresor.pdp.contexthandler.saml.SAMLConfig;
import org.snet.tresor.pdp.policystore.PolicyStore;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;
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
	private static final Logger log = LoggerFactory.getLogger(Configuration.class);
	// System Property Key of the config File (if applicable)
	private static final String TRESOR_PDP_CONFIG_PROPERTY = "org.snet.tresor.pdp.config";
	private static final String TRESOR_PDP_DEFAULT_CONFIG_FILE = "config";
	
	private static Configuration INSTANCE;	
	public static synchronized Configuration getInstance() throws JSONException, IOException {
		if (INSTANCE == null)
			INSTANCE = new Configuration();
		return INSTANCE;
	}
	
	private ClassLoader loader;
	private JSONObject config;
	
	private Configuration() throws JSONException, IOException {
		this.loader = this.getClass().getClassLoader();
		this.loadConfigJSON();
	}
	
	/**
	 * @return the complete configuration json
	 */
	public JSONObject getFullConfig() {
		return this.config;
	}
	
	/**
	 * @param component name of the component whose config you want (as written in json as key)
	 * @return the configuration for given component name
	 */
	public JSONObject getConfig(String component) {
		return this.config.optJSONObject(component);
	}
	
	private void loadConfigJSON() throws IOException, JSONException{
		InputStream configStream = null;

		// open stream from config file, either provided by system property or default location		
		String configPath = System.getProperty(TRESOR_PDP_CONFIG_PROPERTY);
		if (configPath != null) {
			configStream = new FileInputStream(new File(configPath));
		}
		else {
			configStream = this.loader.getResourceAsStream(TRESOR_PDP_DEFAULT_CONFIG_FILE);
		}

		if (configStream != null) {
			JSONTokener tok = new JSONTokener(configStream);
			this.config = new JSONObject(tok);
		}
	}
	
	
	
	
	
	
	
	
	
	
//	
//	
//	/**
//	 * Loads and parses configuration file, initializes GeoXACML and SAML support
//	 */
//	public void initConfiguration() {
//		GeoXACML.initialize();
//		log.debug("GeoXACML successfully initialized");
//		
//		try {
//			SAMLConfig.InitSAML();
//			log.debug("SAML successfully initialized");
//		} catch (Exception e) {
//			log.error("Initializing SAML failed", e);
//		}
//				
//		// load & parse config file
//		JSONObject config = this.getConfig();
//		
//		if (config == null) {
//			log.info("Shutting down");
//			System.exit(1);
//		}
//		
//		try { parsePolicyStore(config); }
//		catch (Exception e) { log.error("Error creating PolicyStore from config", e); }
//
//		try { parsePDP(config); }
//		catch (Exception e) { log.error("Error creating PDP from config", e); }
//
//		try { parseAuthenticator(config); }
//		catch (Exception e) { log.error("Error creating Authenticator from config", e); }
//
//		try { parseContextHandler(config); }
//		catch (Exception e) { log.error("Error creating ContextHandler from config", e); }
//		
//		log.info("Configuration finished");
//	}
//
//	private JSONObject getConfig() {
//		log.info("Loading Configuration");
//		InputStream stream = null;
//		String configPath = System.getProperty(TRESOR_PDP_CONFIG_PROPERTY);
//		
//		// if no path to config file is provided, try loading default resource
//		if (configPath == null) {
//			log.info("No config path provided, searching for default file {} in classpath", TRESOR_PDP_DEFAULT_CONFIG_FILE);
//			stream = this.loader.getResourceAsStream(TRESOR_PDP_DEFAULT_CONFIG_FILE);
//		} else {
//			log.info("Path to config file provided, trying to load");
//			// else try loading file in provided path
//			File file = new File(configPath);
//			try {
//				stream = new FileInputStream(file);
//			} catch (IOException e) {
//				log.error("Loading provided config file at {} failed", configPath, e);
//			}
//		}
//		
//		JSONObject config = null;
//		if (stream == null) {
//			log.info("No config file could be loaded");
//		} else {
//			log.info("Opened stream to configuration file");
//			// ..try parsing it
//			try {
//				JSONTokener tokener = new JSONTokener(stream);
//				config = new JSONObject(tokener);
//				log.info("Parsed configuration file successfully");
//			} catch (JSONException e) {
//				log.error("Parsing configuration file failed");
//			} finally {
//				try { stream.close(); }
//				catch (IOException e) { log.error("Closing stream to configuration file caused Exception", e); }
//			}
//		}
//		
//		return config;
//	}
//	
//	/**
//	 * Creates PolicyStore from given configuration details and sets the PolicyStore
//	 * @param configJSON jsonobject containing configuration details
//	 * @throws Exception
//	 */
//	private void parsePolicyStore(JSONObject configJSON) throws Exception {		
//		PolicyStore policyStore;
//		
//		if (configJSON != null && configJSON.has("policystore")) {
//			JSONObject policyStoreConfig = configJSON.getJSONObject("policystore");
//			
//			if (policyStoreConfig.has("parameters"))
//				policyStore = createInstanceWithParameters(policyStoreConfig, PolicyStore.class);
//			else
//				policyStore = createInstance(policyStoreConfig, PolicyStore.class);
//			
//			this.POLICYSTORE = policyStore;
//		}
//	}
//	
//	/**
//	 * Creates PDP from given configuration details and sets it as the PDP
//	 * @param configJSON jsonobject containing configuration details
//	 * @throws Exception
//	 * @throws JSONException 
//	 */
//	private void parsePDP(JSONObject configJSON) throws JSONException, Exception {		
//		if (configJSON != null) {
//			List<AttributeFinderModule> attributeFinderModules = new ArrayList<AttributeFinderModule>();
//			Set<PolicyFinderModule> policyFinderModules = new HashSet<PolicyFinderModule>();
//			List<ResourceFinderModule> resourceFinderModules = new ArrayList<ResourceFinderModule>();		
//			
//			if (configJSON.has("attributefindermodules")) {
//				createInstances(configJSON.getJSONArray("attributefindermodules"), 
//								attributeFinderModules, 
//								AttributeFinderModule.class);
//			}
//			
//			if (configJSON.has("policyfindermodules")) {
//				createInstances(configJSON.getJSONArray("policyfindermodules"), 
//								policyFinderModules, 
//								PolicyFinderModule.class);
//			}
//
//			if (configJSON.has("resourcefindermodules")) {
//				createInstances(configJSON.getJSONArray("resourcefindermodules"), 
//								resourceFinderModules, 
//								ResourceFinderModule.class);
//			}
//			
//			AttributeFinder attributeFinder = new AttributeFinder();
//			attributeFinder.setModules(attributeFinderModules);
//			
//			PolicyFinder policyFinder = new PolicyFinder();
//			policyFinder.setModules(policyFinderModules);
//			
//			ResourceFinder resourceFinder = new ResourceFinder();
//			resourceFinder.setModules(resourceFinderModules);
//			
//			PDPConfig config = new PDPConfig(attributeFinder, policyFinder, resourceFinder);
//			
//			Balana.getInstance().setPdpConfig(config);
//			this.TRESOR_PDP = new PDP(config);
//		}		
//	}
//	
//	/**
//	 * Creates Authenticator from given configuration details and sets it as the Authenticator
//	 * @param configJSON jsonobject containing configuration details
//	 * @throws Exception
//	 */
//	private void parseAuthenticator(JSONObject configJSON) throws Exception {
//		if (configJSON != null && configJSON.has("authenticator")) {
//			JSONObject authConfig = configJSON.getJSONObject("authenticator");
//			Authenticator authenticator;
//			
//			if (authConfig.has("parameters"))
//				authenticator = createInstanceWithParameters(authConfig, Authenticator.class);
//			else
//				authenticator = createInstance(authConfig, Authenticator.class);
//			
//			this.authenticator = authenticator;
//		}		
//	}
//	
//	/**
//	 * Creates ContextHandler from given configuration details and sets it as the ContextHandler
//	 * @param configJSON jsonobject containing configuration details
//	 * @throws Exception
//	 */
//	private void parseContextHandler(JSONObject configJSON) throws Exception {
//		if (configJSON != null && configJSON.has("contexthandlermodules")) {
//			ContextHandler contextHandler = ContextHandler.getInstance();
//			JSONArray handlerConfig = configJSON.getJSONArray("contexthandlermodules");
//			
//			JSONObject config;
//			String resource;
//			Handler handler;
//			
//			for (int i = 0; i < handlerConfig.length(); i++) {
//				config = handlerConfig.getJSONObject(i);
//				resource = config.getString("resource");			
//				handler = createInstance(config, Handler.class);
//				contextHandler.putHandler(resource, handler);
//			}
//			
//			this.CONTEXT_HANDLER = contextHandler;			
//		}
//	}
//	
//	/**
//	 * Creates instances of given class with given configuration, optionally with parameters,
//	 * and puts them into the output collection
//	 * @param classes, JSONArray containing JSONObjects with classnames (and optionally parameters)
//	 * @param output, the collection to output to
//	 * @param cls, the type of class to create
//	 * @throws Exception
//	 */
//	private <T> void createInstances(JSONArray classes, Collection<T> output, Class<T> cls) throws Exception {
//		JSONObject j;
//		T instance;
//		
//		for (int i = 0; i < classes.length(); i++) {
//			instance = null;
//			j = classes.getJSONObject(i);
//			
//			if (j.has("parameters"))
//				instance = createInstanceWithParameters(j, cls);
//			else
//				instance = createInstance(j, cls);
//			
//			if (instance != null)
//				output.add(instance);			
//		}
//	}
//	
//	/**
//	 * Creates an instance of the class with classname inside given JSONObject 
//	 * and type of given class via the default constructor 
//	 * @param j, JSONObject containing classname and no parameters
//	 * @param cls, type of class to create
//	 * @return created instance
//	 * @throws Exception
//	 */
//	private <T> T createInstance(JSONObject j, Class<T> cls) throws Exception {
//		Class<?> c = this.loader.loadClass(j.getString("classname"));		
//		return cls.cast(c.newInstance());
//	}
//	
//	/**
//	 * Creates an instance of the class with classname inside given JSONObject
//	 * and type of given class via a constructor which takes varargs
//	 * @param j, JSONObject containing classname and additional parameters JSONArray
//	 * @param cls, type of class to create
//	 * @return created instance
//	 * @throws Exception
//	 */
//	private <T> T createInstanceWithParameters(JSONObject j, Class<T> cls) throws Exception {
//		Class<?> c = this.loader.loadClass(j.getString("classname"));
//		Object instance;		
//		
//		JSONArray paramsJSON = j.getJSONArray("parameters");
//		String[] params = new String[paramsJSON.length()];				
//		for (int k = 0; k < paramsJSON.length(); k++)
//			params[k] = paramsJSON.getString(k);				
//		
//		// first two for compatibility with balana, third one for our own modules
//		if (c == FileBasedPolicyFinderModule.class)
//			instance = c.getConstructor(Set.class).newInstance(new HashSet<String>(Arrays.asList(params)));
//		else if (c == InMemoryPolicyFinderModule.class)
//			instance = c.getConstructor(List.class).newInstance(Arrays.asList(params));
//		else {
//			// necessary workaround, May 13 2014
//			Object[] wrappedParams = { params };
//			
//			instance = c.getConstructor(String[].class).newInstance(wrappedParams);
//		}
//								
//		
//		return cls.cast(instance);
//	}
	
}
