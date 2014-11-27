package org.snet.tresor.pdp.contexthandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.snet.tresor.pdp.additions.PDPAdditions;
import org.snet.tresor.pdp.additions.finder.impl.PolicyStorePolicyFinderModule;
import org.snet.tresor.pdp.additions.finder.impl.StationAttributeFinderModule;
import org.snet.tresor.pdp.additions.policystore.FileBasedClientIdServiceIdPolicyStore;
import org.snet.tresor.pdp.additions.policystore.PolicyStore;
import org.snet.tresor.pdp.additions.policystore.TwoKeyValuePolicyStore;
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
	TwoKeyValuePolicyStore getTwoKeyValuePolicyStore() {
		return new FileBasedClientIdServiceIdPolicyStore("/home/student/policies/", new ReentrantReadWriteLock());
	}

	@Bean
	@Inject
	AttributeFinder getAttributeFinder(ObjectMapper objectMapper) {
		List<AttributeFinderModule> attributeFinderModules = new ArrayList<AttributeFinderModule>();
		attributeFinderModules.add(new CurrentEnvModule());
		attributeFinderModules.add(new SelectorModule());
		attributeFinderModules.add(new StationAttributeFinderModule("http://localhost:3300", objectMapper));

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
		return PDPAdditions.getExtendedPDP(pdpConfig);
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
