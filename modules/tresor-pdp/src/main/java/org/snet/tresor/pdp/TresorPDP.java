package org.snet.tresor.pdp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.contexthandler.saml.SAMLConfig;
import org.snet.tresor.pdp.finder.impl.PolicyStorePolicyFinderModule;
import org.snet.tresor.pdp.policystore.DummyPolicyStore;
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

/**
 * Class for initializing balana PDP with extra components (GeoXACML, SAML) and configuration
 * @author malik
 */
public class TresorPDP {
	private static final Logger log = LoggerFactory.getLogger(TresorPDP.class);

	private PDP pdp;
	private PDPConfig pdpConfig;
	private PolicyStore policyStore;

	/**
	 * Creates a new TresorPDP with default and dummy configuration
	 * @throws Exception
	 */
	public TresorPDP() throws Exception {
		GeoXACML.initialize();
		SAMLConfig.InitSAML();
		this.pdp = new PDP(Balana.getInstance().getPdpConfig());
		this.policyStore = new DummyPolicyStore();
		log.info("TresorPDP initialized without configuration");
	}

	/**
	 * Creates a new TresorPDP with given configuration
	 * @param pdpConfig
	 * @throws Exception
	 */
	public TresorPDP(JSONObject pdpConfig) throws Exception {
		GeoXACML.initialize();
		SAMLConfig.InitSAML();
		this.configure(pdpConfig);
		log.info("TresorPDP initialized with configuration");
	}

	/**
	 * @return actual PDP instance
	 */
	public PDP getPDP() {
		return this.pdp;
	}

	/**
	 * @return actual configuration of PDP instance
	 */
	public PDPConfig getPDPConfig() {
		return this.pdpConfig;
	}

	/**
	 * @return the policyStore used by the PDP
	 */
	public PolicyStore getPolicyStore() {
		return this.policyStore;
	}

	/**
	 * Configures TresorPDP with given config
	 * @param configJSON
	 * @throws Exception
	 */
	public void configure(JSONObject configJSON) throws Exception {
		log.debug("Configuring TresorPDP");
		this.policyStore = this.parsePolicyStore(configJSON);
		this.pdp = this.parsePDP(configJSON);
	}

	/**
	 * Parses the configuration for the PolicyStore and creates the instance
	 * @param configJSON
	 * @return created PolicyStore
	 * @throws Exception
	 */
	private PolicyStore parsePolicyStore(JSONObject configJSON) throws Exception {
		JSONObject policyStoreConfig = configJSON.getJSONObject("policystore");
		PolicyStore policyStore = Helper.createInstance(policyStoreConfig, PolicyStore.class);

		return policyStore;
	}

	/**
	 * Parses the configuration for the PDP and creates instances of PDP and PDPConfig
	 * @param configJSON
	 * @return created PDP
	 * @throws Exception
	 */
	private PDP parsePDP(JSONObject configJSON) throws Exception {
		List<AttributeFinderModule> attributeFinderModules = new ArrayList<AttributeFinderModule>();
		Set<PolicyFinderModule> policyFinderModules = new HashSet<PolicyFinderModule>();
		List<ResourceFinderModule> resourceFinderModules = new ArrayList<ResourceFinderModule>();

		Helper.createInstances(configJSON.getJSONArray("attributefindermodules"), attributeFinderModules, AttributeFinderModule.class);
		AttributeFinder attributeFinder = new AttributeFinder();
		attributeFinder.setModules(attributeFinderModules);

		policyFinderModules.add(new PolicyStorePolicyFinderModule(this.policyStore));
		PolicyFinder policyFinder = new PolicyFinder();
		policyFinder.setModules(policyFinderModules);

		Helper.createInstances(configJSON.getJSONArray("resourcefindermodules"), resourceFinderModules, ResourceFinderModule.class);
		ResourceFinder resourceFinder = new ResourceFinder();
		resourceFinder.setModules(resourceFinderModules);

		this.pdpConfig = new PDPConfig(attributeFinder, policyFinder, resourceFinder);
		return new PDP(this.pdpConfig);
	}

}