package org.snet.tresor.pdp.policystore;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Empty PolicyStoreManager implementation to be returned when no PolicyStoreManager was defined
 * Does practically nothing
 * @author malik
 */
public class DummyPolicyStoreManager implements PolicyStoreManager {
	
	// the logger we'll use for all messages
    private static Log logger = LogFactory.getLog(DummyPolicyStoreManager.class);
	
	public Map<String, String> getAll(String domain) {
		logger.info("getAll called for domain: " + domain);
		return new HashMap<String, String>();
	}

	public String getPolicy(String domain, String service) {
		logger.info("getPolicy called for domain/service combination: " + domain+"/"+service);
		return null;
	}

	public String addPolicy(String domain, String service, String policy) {
		logger.info("addPolicy called for domain/service combination: " + domain+"/"+service);
		return null;
	}

	public int deletePolicy(String domain, String service) {
		logger.info("deletePolicy called for domain/service combination: " + domain+"/"+service);
		return 0;
	}

	public void close() {
		logger.info("close called");
	}

}
