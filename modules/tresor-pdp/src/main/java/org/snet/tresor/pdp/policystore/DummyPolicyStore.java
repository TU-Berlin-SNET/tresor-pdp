package org.snet.tresor.pdp.policystore;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Empty PolicyStore implementation to be returned when no PolicyStore was defined
 * Does practically nothing
 * @author malik
 */
public class DummyPolicyStore implements PolicyStore {
	
	// the logger we'll use for all messages    
	private static final Logger log = LoggerFactory.getLogger(DummyPolicyStore.class);
	
	public Map<String, String> getAll(String domain) {
		log.info("getAll called for domain: " + domain);
		return new HashMap<String, String>();
	}

	public String getPolicy(String domain, String service) {
		log.info("getPolicy called for domain/service combination: " + domain+"/"+service);
		return null;
	}

	public String addPolicy(String domain, String service, String policy) {
		log.info("addPolicy called for domain/service combination: " + domain+"/"+service);
		return null;
	}

	public int deletePolicy(String domain, String service) {
		log.info("deletePolicy called for domain/service combination: " + domain+"/"+service);
		return 0;
	}

	public void close() {
		log.info("close called");
	}

}
