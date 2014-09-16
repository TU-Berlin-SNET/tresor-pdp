package org.snet.tresor.pdp.contexthandler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.Configuration;
import org.snet.tresor.pdp.Helper;
import org.snet.tresor.pdp.TresorPDP;
import org.snet.tresor.pdp.contexthandler.authentication.Authenticator;
import org.snet.tresor.pdp.contexthandler.handler.PDPHandler;
import org.snet.tresor.pdp.contexthandler.handler.PolicyStoreHandler;

/**
 * Class for initializing ContextHandler system, including API and PDP
 */
public class ContextHandler {
	private static final Logger log = LoggerFactory.getLogger(ContextHandler.class);

	private TresorPDP tresorPDP;
	private Authenticator authenticator;

	private PDPHandler pdpHandler;
	private PolicyStoreHandler policyStoreHandler;
	private boolean initialized;

	private static ContextHandler INSTANCE;
	public static synchronized ContextHandler getInstance() {
		if (INSTANCE == null)
			INSTANCE = new ContextHandler();
		return INSTANCE;
	}

	private ContextHandler() {
		this.initialized = false;
	}

	public PDPHandler getPDPHandler() {
		return this.pdpHandler;
	}

	public PolicyStoreHandler getPolicyStoreHandler() {
		return this.policyStoreHandler;
	}

	public TresorPDP getTresorPDP() {
		return this.tresorPDP;
	}

	public Authenticator getAuthenticator() {
		return this.authenticator;
	}

	public synchronized void initialize() throws IOException, Exception {
		if (this.initialized) {
			log.debug("ContextHandler already initialized");
			return;
		}

		log.debug("Initializing ContextHandler");

		Configuration conf = Configuration.getInstance();

		// Create the PDP
		this.tresorPDP = new TresorPDP(conf.getConfig("tresor-pdp"));

		// Create the authenticator
		this.authenticator = Helper.createInstance(conf.getConfig("authenticator"), Authenticator.class);

		// Create Handlers
		this.pdpHandler = new PDPHandler(this.tresorPDP.getPDP(), this.tresorPDP.getPDPConfig());
		this.policyStoreHandler = new PolicyStoreHandler(this.tresorPDP.getPolicyStore(), this.authenticator);

		log.info("ContextHandler initialized");

		this.initialized = true;
	}

}
