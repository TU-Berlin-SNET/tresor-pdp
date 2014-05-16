package org.snet.tresor.pdp.policystore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PolicyStore {

	private static Log logger = LogFactory.getLog(PolicyStore.class);
	
	private static PolicyStoreManager MANAGER;
	
	public static PolicyStoreManager getManager() {
		if (MANAGER == null)
			logger.info("No PolicyStoreManager specified, returning DummyPolicyStoreManager");			
			
		return MANAGER;
	}
	
	public static void setManager(PolicyStoreManager manager) {
		MANAGER = manager;
		logger.info("PolicyStoreManager set to: " + manager.getClass());
	}
	
}
