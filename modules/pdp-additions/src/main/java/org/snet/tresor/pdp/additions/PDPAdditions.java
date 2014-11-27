package org.snet.tresor.pdp.additions;

import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.ResourceFinder;

public class PDPAdditions {

	/**
	 * Initializes PDP additions (i.e. initializes/registers GeoXACML components)
	 */
	public static void initalizeAdditions() {
		GeoXACML.initialize();
	}

	/**
	 * Convenience method to initialize additions and retrieve a PDP
	 * @param conf
	 * @return a PDP
	 */
	public static PDP getExtendedPDP(PDPConfig conf) {
		PDPAdditions.initalizeAdditions();
		return new PDP(conf);
	}

	/**
	 * Convenience method to initialize additions and retrieve a PDP
	 * @param attributeFinder
	 * @param policyFinder
	 * @param resourceFinder
	 * @return a PDP
	 */
	public static PDP getExtendedPDP(AttributeFinder attributeFinder, PolicyFinder policyFinder,
			ResourceFinder resourceFinder) {
		return PDPAdditions.getExtendedPDP(new PDPConfig(attributeFinder, policyFinder, resourceFinder));
	}

}
