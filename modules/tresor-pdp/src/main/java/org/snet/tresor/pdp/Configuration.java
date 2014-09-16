package org.snet.tresor.pdp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		log.info("Configuration initialized");
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
			log.debug("Loading config given via system property");
			configStream = new FileInputStream(new File(configPath));
		}
		else {
			log.debug("Loading default config in classpath");
			configStream = this.loader.getResourceAsStream(TRESOR_PDP_DEFAULT_CONFIG_FILE);
		}

		if (configStream != null) {
			JSONTokener tok = new JSONTokener(configStream);
			this.config = new JSONObject(tok);
			log.debug("Loaded and parsed configuration file successfully");
		}
	}

}
