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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.snet.tresor.pdp.contexthandler.ContextHandler;
import org.snet.tresor.pdp.contexthandler.auth.DummyAuth;
import org.snet.tresor.pdp.contexthandler.auth.TresorAuth;
import org.snet.tresor.pdp.contexthandler.handler.Handler;
import org.snet.tresor.pdp.contexthandler.handler.PDPHandler;
import org.snet.tresor.pdp.contexthandler.handler.PolicyStoreHandler;
import org.snet.tresor.pdp.contexthandler.saml.SAMLConfig;
import org.snet.tresor.pdp.finder.impl.PolicyStorePolicyFinderModule;
import org.snet.tresor.pdp.policystore.DummyPolicyStoreManager;
import org.snet.tresor.pdp.policystore.PolicyStoreManager;
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
	// System Property Key of the config File (if applicable)
	private static final String TRESOR_PDP_CONFIG_PROPERTY = "org.snet.tresor.pdp.config";
	
    // the logger we'll use for all messages
    private static Log log = LogFactory.getLog(Configuration.class);
    
    private PolicyStoreManager POLICYSTORE_MANAGER;
    
	private ContextHandler CONTEXT_HANDLER;
	
	private TresorAuth TRESOR_AUTH;
	
	private PDP TRESOR_PDP;
	
	private static Configuration INSTANCE;
	
	private ClassLoader loader;
	
	private Configuration() {
		this.loader = this.getClass().getClassLoader();
	}
	
	public static Configuration getInstance() {
		if (INSTANCE == null)
			INSTANCE = new Configuration();
		return INSTANCE;
	}
	
	public PolicyStoreManager getPolicyStoreManager() {
		return this.POLICYSTORE_MANAGER;
	}
	
	public ContextHandler getContextHandler() {
		return this.CONTEXT_HANDLER;
	}
	
	public TresorAuth getTresorAuth() {
		return this.TRESOR_AUTH;
	}
	
	public PDP getPDP() {
		return this.TRESOR_PDP;
	}
	
	/**
	 * Loads and parses configuration file, initializes GeoXACML and SAML support
	 */
	public void initConfiguration() {
		
		GeoXACML.initialize();
		log.info("GeoXACML: success");
		
		try {
			SAMLConfig.InitSAML();
			log.info("SAML: success");
		} catch (Exception e) {
			log.error("SAML: fail", e);
		}		
		
		// load & parse config file
		JSONObject config = this.getConfig();
		
		// if config file is present, try creating components with it
		if (config != null) {
			try { parsePolicyStoreManager(config); } 				
			catch (Exception e) { log.error("Error doing reflective operation", e); }
			
			try { parsePDP(config); } 				
			catch (Exception e) { log.error("Error doing reflective operation", e); }
			
			try { parseTresorAuth(config); } 				
			catch (Exception e) { log.error("Error doing reflective operation", e); }
			
			try { parseContextHandler(config); } 				
			catch (Exception e) { log.error("Error doing reflective operation", e); }
		}
		
		if (this.POLICYSTORE_MANAGER == null) {
			log.info("Fallback to default DummyPolicyStoreManager");
			this.POLICYSTORE_MANAGER = new DummyPolicyStoreManager();
		}
		
		if (this.TRESOR_PDP == null) {
			log.info("Fallback to default PDP");
			AttributeFinder attributeFinder = Balana.getInstance().getPdpConfig().getAttributeFinder();
			ResourceFinder resourceFinder = Balana.getInstance().getPdpConfig().getResourceFinder();

			Set<PolicyFinderModule> policyFinderModules = new HashSet<PolicyFinderModule>();
			policyFinderModules.add(new PolicyStorePolicyFinderModule());

			PolicyFinder policyFinder = new PolicyFinder();
			policyFinder.setModules(policyFinderModules);

			this.TRESOR_PDP = new PDP(new PDPConfig(attributeFinder, policyFinder, resourceFinder));
		}

		if (this.TRESOR_AUTH == null) {
			log.info("Fallback to default DummyAuth");
			this.TRESOR_AUTH = new DummyAuth();
		}

		if (this.CONTEXT_HANDLER == null) {
			log.info("Fallback to default ContextHandler");
			this.CONTEXT_HANDLER = ContextHandler.getInstance();
			this.CONTEXT_HANDLER.putHandler("pdp", new PDPHandler());
			this.CONTEXT_HANDLER.putHandler("policy", new PolicyStoreHandler());
		}		
		
		log.info("configuration finished");
	}

	private JSONObject getConfig() {
		InputStream stream = null;			
		String configPath = System.getProperty(TRESOR_PDP_CONFIG_PROPERTY);		
		
		// check whether config file was provided via system property
		if (configPath != null) {
			File file = new File(configPath);
			try { stream = new FileInputStream(file); }
			catch (IOException e) { log.error("Error opening provided config file"); }
		}
		
		// if stream has not been established until this point, load default config file
		stream = (stream == null) ? this.loader.getResourceAsStream("config") : stream;
		
		JSONObject config = null;
		// if we now have a stream..
		if (stream != null) {
			// ..try parsing it
			try {
				JSONTokener tokener = new JSONTokener(stream);
				config = new JSONObject(tokener);
			} catch (JSONException e) {
				log.error("Error parsing config file");				
			} finally {
				try { stream.close(); }
				catch (IOException e) { }
			}
		}
		
		return config;		
	}
	
	/**
	 * Creates PolicyStoreManager from given configuration details and sets the manager for the PolicyStore
	 * @param configJSON jsonobject containing configuration details
	 * @throws Exception
	 */
	private void parsePolicyStoreManager(JSONObject configJSON) throws Exception {		
		PolicyStoreManager manager;
		
		if (configJSON != null && configJSON.has("policystoremanager")) {
			JSONObject managerConfig = configJSON.getJSONObject("policystoremanager");
			
			if (managerConfig.has("parameters"))
				manager = createInstanceWithParameters(managerConfig, PolicyStoreManager.class);
			else
				manager = createInstance(managerConfig, PolicyStoreManager.class);
			
			this.POLICYSTORE_MANAGER = manager;			
		}
	}
	
	/**
	 * Creates PDP from given configuration details and sets it as the PDP
	 * @param configJSON jsonobject containing configuration details
	 * @throws Exception
	 * @throws JSONException 
	 */
	private void parsePDP(JSONObject configJSON) throws JSONException, Exception {		
		if (configJSON != null) {
			List<AttributeFinderModule> attributeFinderModules = new ArrayList<AttributeFinderModule>();
			Set<PolicyFinderModule> policyFinderModules = new HashSet<PolicyFinderModule>();
			List<ResourceFinderModule> resourceFinderModules = new ArrayList<ResourceFinderModule>();		
			
			if (configJSON.has("attributefindermodules")) {
				createInstances(configJSON.getJSONArray("attributefindermodules"), 
								attributeFinderModules, 
								AttributeFinderModule.class);
			}
			
			if (configJSON.has("policyfindermodules")) {
				createInstances(configJSON.getJSONArray("policyfindermodules"), 
								policyFinderModules, 
								PolicyFinderModule.class);				
			}

			if (configJSON.has("resourcefindermodules")) {
				createInstances(configJSON.getJSONArray("resourcefindermodules"), 
								resourceFinderModules, 
								ResourceFinderModule.class);
			}
			
			AttributeFinder attributeFinder = new AttributeFinder();
			attributeFinder.setModules(attributeFinderModules);
			
			PolicyFinder policyFinder = new PolicyFinder();
			policyFinder.setModules(policyFinderModules);
			
			ResourceFinder resourceFinder = new ResourceFinder();
			resourceFinder.setModules(resourceFinderModules);
			
			PDPConfig config = new PDPConfig(attributeFinder, policyFinder, resourceFinder);
			
			Balana.getInstance().setPdpConfig(config);
			this.TRESOR_PDP = new PDP(config);
		}		
	}
	
	/**
	 * Creates TresorAuth from given configuration details and sets it as the TresorAuth
	 * @param configJSON jsonobject containing configuration details
	 * @throws Exception
	 */
	private void parseTresorAuth(JSONObject configJSON) throws Exception {
		if (configJSON != null && configJSON.has("tresorauth")) {
			JSONObject authConfig = configJSON.getJSONObject("tresorauth");
			TresorAuth tresorAuth;
			
			if (authConfig.has("parameters"))
				tresorAuth = createInstanceWithParameters(authConfig, TresorAuth.class);
			else				
				tresorAuth = createInstance(authConfig, TresorAuth.class);
			
			this.TRESOR_AUTH = tresorAuth;
		}		
	}
	
	/**
	 * Creates ContextHandler from given configuration details and sets it as the ContextHandler
	 * @param configJSON jsonobject containing configuration details
	 * @throws Exception
	 */
	private void parseContextHandler(JSONObject configJSON) throws Exception {
		if (configJSON != null && configJSON.has("contexthandlermodules")) {			
			ContextHandler contextHandler = ContextHandler.getInstance();
			JSONArray handlerConfig = configJSON.getJSONArray("contexthandlermodules");
			
			JSONObject config;
			String resource;
			Handler handler;
			
			for (int i = 0; i < handlerConfig.length(); i++) {
				config = handlerConfig.getJSONObject(i);
				resource = config.getString("resource");			
				handler = createInstance(config, Handler.class);
				contextHandler.putHandler(resource, handler);
			}
			
			this.CONTEXT_HANDLER = contextHandler;			
		}
	}
	
	/**
	 * Creates instances of given class with given configuration, optionally with parameters,
	 * and puts them into the output collection
	 * @param classes, JSONArray containing JSONObjects with classnames (and optionally parameters)
	 * @param output, the collection to output to
	 * @param cls, the type of class to create
	 * @throws Exception
	 */
	private <T> void createInstances(JSONArray classes, Collection<T> output, Class<T> cls) throws Exception {
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
	 * @throws Exception
	 */
	private <T> T createInstance(JSONObject j, Class<T> cls) throws Exception {
		Class<?> c = this.loader.loadClass(j.getString("classname"));		
		return cls.cast(c.newInstance());
	}
	
	/**
	 * Creates an instance of the class with classname inside given JSONObject
	 * and type of given class via a constructor which takes varargs
	 * @param j, JSONObject containing classname and additional parameters JSONArray
	 * @param cls, type of class to create
	 * @return created instance
	 * @throws Exception
	 */
	private <T> T createInstanceWithParameters(JSONObject j, Class<T> cls) throws Exception {
		Class<?> c = this.loader.loadClass(j.getString("classname"));
		Object instance;		
		
		JSONArray paramsJSON = j.getJSONArray("parameters");
		String[] params = new String[paramsJSON.length()];				
		for (int k = 0; k < paramsJSON.length(); k++)
			params[k] = paramsJSON.getString(k);				
		
		// first two for compatibility with balana, third one for our own modules
		if (c == FileBasedPolicyFinderModule.class)
			instance = c.getConstructor(Set.class).newInstance(new HashSet<String>(Arrays.asList(params)));
		else if (c == InMemoryPolicyFinderModule.class)
			instance = c.getConstructor(List.class).newInstance(Arrays.asList(params));
		else {
			// necessary workaround, May 13 2014
			Object[] wrappedParams = { params };
			
			instance = c.getConstructor(String[].class).newInstance(wrappedParams);
		}
								
		
		return cls.cast(instance);
	}
	
}
