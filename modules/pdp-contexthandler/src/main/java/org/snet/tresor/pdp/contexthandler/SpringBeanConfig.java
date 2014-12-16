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

import org.snet.tresor.pdp.additions.finder.impl.LocationAttributeFinderModule;
import org.snet.tresor.pdp.additions.finder.impl.PolicyStorePolicyFinderModule;
import org.snet.tresor.pdp.additions.finder.impl.StationAttributeFinderModule;
import org.snet.tresor.pdp.additions.finder.impl.WeekdayAttributeFinderModule;
import org.snet.tresor.pdp.additions.policystore.AbstractClientIdServiceIdPolicyStore;
import org.snet.tresor.pdp.additions.policystore.FileBasedClientIdServiceIdPolicyStore;
import org.snet.tresor.pdp.additions.policystore.PolicyStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
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

	@Bean
	AbstractClientIdServiceIdPolicyStore getClientIdServiceIdPolicyStore() throws IOException {
		return new FileBasedClientIdServiceIdPolicyStore(new File(".").getCanonicalPath() + "/policies/",
				new ReentrantReadWriteLock());
	}

	@Bean
	ThreadLocal<Map<String, String>> getCache() {
		return new ThreadLocal<Map<String,String>>() {
			protected synchronized Map<String, String> initialValue() {
				return new HashMap<String, String>();
			}
		};
	}

	@Bean
	@Inject
	AttributeFinder getAttributeFinder(ObjectMapper objectMapper, ThreadLocal<Map<String, String>> cache) {
		List<AttributeFinderModule> attributeFinderModules = new ArrayList<AttributeFinderModule>();
		attributeFinderModules.add(new CurrentEnvModule());
		attributeFinderModules.add(new SelectorModule());
		attributeFinderModules.add(new WeekdayAttributeFinderModule());
		attributeFinderModules.add(new StationAttributeFinderModule("http://localhost:3300", objectMapper));
		attributeFinderModules.add(new LocationAttributeFinderModule(
				"http://ls.snet.tu-berlin.de:8080/pe/api/v2/pdp",
				"Basic cGVfdXNlcjo5NTViMDYzMzY0ZDkxNTdjMDgzOTI1M2U4NDcwMjI2ODliNWVlMWRm",
				objectMapper, cache));

		AttributeFinder attributeFinder = new AttributeFinder();
		attributeFinder.setModules(attributeFinderModules);

		return attributeFinder;
	}

	@Bean
	@Inject
	PolicyFinder getPolicyFinder(PolicyStore policyStore) {
		Set<PolicyFinderModule> policyFinderModules = new HashSet<PolicyFinderModule>();
		policyFinderModules.add(new PolicyStorePolicyFinderModule(policyStore));

		PolicyFinder policyFinder = new PolicyFinder();
		policyFinder.setModules(policyFinderModules);

		return policyFinder;
	}

	@Bean
	ResourceFinder getResourceFinder() {
		List<ResourceFinderModule> resourceFinderModules = new ArrayList<ResourceFinderModule>();

		ResourceFinder resourceFinder = new ResourceFinder();
		resourceFinder.setModules(resourceFinderModules);

		return resourceFinder;
	}

	@Bean
	@Inject
	PDPConfig getPDPConfig(AttributeFinder attributeFinder, PolicyFinder policyFinder,
			ResourceFinder resourceFinder) {
		return new PDPConfig(attributeFinder, policyFinder, resourceFinder);
	}

	@Bean
	@Inject
	PDP getPDP(PDPConfig pdpConfig) {
		return new PDP(pdpConfig);
	}


	@Bean
	ObjectMapper getObjectmapper() {
		return new ObjectMapper();
	}

	@Bean
	RequestCtxFactory getReqCtxFactory() {
		return RequestCtxFactory.getFactory();
	}

	@Bean
	EvaluationCtxFactory getEvalCtxFactory() {
		return EvaluationCtxFactory.getFactory();
	}

}
