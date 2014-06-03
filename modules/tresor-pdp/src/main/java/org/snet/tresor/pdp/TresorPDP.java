package org.snet.tresor.pdp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snet.tresor.pdp.contexthandler.ContextHandler;
import org.snet.tresor.pdp.contexthandler.auth.TresorAuth;
import org.snet.tresor.pdp.policystore.PolicyStoreManager;
import org.wso2.balana.PDP;

public class TresorPDP {
	private static Log log = LogFactory.getLog(TresorPDP.class);
	
	private static TresorPDP INSTANCE;
	
	private Configuration CONFIGURATION;
	
	// Initialization
	static {
		TresorPDP.getInstance();
		Configuration.getInstance().initConfiguration();
	}
	
	public static TresorPDP getInstance() {
		if (INSTANCE == null)
			INSTANCE = new TresorPDP();
		return INSTANCE;
	}
	
	private TresorPDP() {
		ContextHandler.getInstance();
		this.CONFIGURATION = Configuration.getInstance();		
	}
	
	public PolicyStoreManager getPolicyStoreManager() {
		return this.CONFIGURATION.getPolicyStoreManager();
	}
	
	public ContextHandler getContextHandler() {
		return this.CONFIGURATION.getContextHandler();
	}
	
	public TresorAuth getTresorAuth() {
		return this.CONFIGURATION.getTresorAuth();
	}
	
	public PDP getPDP() {
		return this.CONFIGURATION.getPDP();
	}
	
}
