package org.snet.tresor.pdp.contexthandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.snet.tresor.pdp.additions.GeoPDP;
import org.snet.tresor.pdp.additions.finder.impl.*;
import org.snet.tresor.pdp.additions.pip.PIP;
import org.snet.tresor.pdp.additions.pip.impl.LocationPIP;
import org.snet.tresor.pdp.additions.pip.impl.StationPIP;
import org.snet.tresor.pdp.additions.pip.impl.WeekdayPIP;
import org.snet.tresor.pdp.additions.policystore.AbstractClientIdServiceIdPolicyStore;
import org.snet.tresor.pdp.additions.policystore.FileBasedClientIdServiceIdPolicyStore;
import org.snet.tresor.pdp.additions.policystore.PolicyStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.ctx.EvaluationCtxFactory;
import org.wso2.balana.ctx.RequestCtxFactory;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.ResourceFinder;
import org.wso2.balana.finder.ResourceFinderModule;
import org.wso2.balana.finder.impl.CurrentEnvModule;
import org.wso2.balana.finder.impl.SelectorModule;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@ComponentScan
@EnableWebMvc
public class SpringBeanConfig {
    @Inject Environment env;

	@Bean
	AbstractClientIdServiceIdPolicyStore policyStore() throws IOException {
        String path = (env.containsProperty("policystore.path")) ? env.getProperty("policystore.path") :
                new File(".").getCanonicalPath() + "/policies/";
        return new FileBasedClientIdServiceIdPolicyStore(path, new ReentrantReadWriteLock());
	}

	@Bean
	PIPAttributeFinderModule pipAttributeFinderModule() {
		List<PIP> pips = new ArrayList<>();
		pips.add(new WeekdayPIP());

        if (env.containsProperty("stationpip.url"))
		    pips.add(new StationPIP(env.getProperty("stationpip.url"), objectMapper()));

        if (env.containsProperty("locationpip.url"))
            pips.add(new LocationPIP(env.getProperty("locationpip.url"), env.getProperty("locationpip.authentication"),
                    objectMapper()));

		return new PIPAttributeFinderModule(pips);
	}

	@Bean
	AttributeFinder attributeFinder() {
		List<AttributeFinderModule> attributeFinderModules = new ArrayList<AttributeFinderModule>();
		attributeFinderModules.add(new CurrentEnvModule());
		attributeFinderModules.add(new SelectorModule());
		attributeFinderModules.add(pipAttributeFinderModule());

		AttributeFinder attributeFinder = new AttributeFinder();
		attributeFinder.setModules(attributeFinderModules);

		return attributeFinder;
	}

	@Bean
	PolicyFinder policyFinder() throws IOException {
		Set<PolicyFinderModule> policyFinderModules = new HashSet<PolicyFinderModule>();
		policyFinderModules.add(new PolicyStorePolicyFinderModule(policyStore()));

		PolicyFinder policyFinder = new PolicyFinder();
		policyFinder.setModules(policyFinderModules);

		return policyFinder;
	}

	@Bean
	ResourceFinder resourceFinder() {
		List<ResourceFinderModule> resourceFinderModules = new ArrayList<ResourceFinderModule>();

		ResourceFinder resourceFinder = new ResourceFinder();
		resourceFinder.setModules(resourceFinderModules);

		return resourceFinder;
	}

	@Bean
	PDPConfig pdpConfig() throws IOException {
		return new PDPConfig(attributeFinder(), policyFinder(), resourceFinder());
	}

	@Bean
	PDP pdp() throws IOException {
		return GeoPDP.getGeoExtendedPDP(pdpConfig());
	}

	@Bean
	ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	RequestCtxFactory reqCtxFactory() {
		return RequestCtxFactory.getFactory();
	}

	@Bean
	EvaluationCtxFactory evalCtxFactory() {
		return EvaluationCtxFactory.getFactory();
	}

}
