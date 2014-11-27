package org.snet.tresor.pdp.contexthandler;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.snet.tresor.pdp.additions.PDPAdditions;
import org.snet.tresor.pdp.additions.finder.impl.PolicyStorePolicyFinderModule;
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

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@ComponentScan
@EnableWebMvc
public class SpringConfig {

	@Bean
	ObjectMapper getObjectmapper() {
		return new ObjectMapper();
	}

	@Bean
	@Inject
	PDP getPDP(PDPConfig conf) {
		return PDPAdditions.getExtendedPDP(conf);
	}

	@Bean
	@Inject
	PDPConfig getPDPConfig(PolicyStore store) {
		AttributeFinder a = new AttributeFinder();
		a.setModules(new LinkedList<AttributeFinderModule>());

		PolicyFinder p = new PolicyFinder();
		Set<PolicyFinderModule> policyfindermodules = new HashSet<PolicyFinderModule>();
		policyfindermodules.add(new PolicyStorePolicyFinderModule(store));
		p.setModules(policyfindermodules);

		ResourceFinder r = new ResourceFinder();
		r.setModules(new LinkedList<ResourceFinderModule>());

		return new PDPConfig(a, p, r);
	}

	@Bean
	RequestCtxFactory getReqCtxFactory() {
		return RequestCtxFactory.getFactory();
	}

	@Bean
	EvaluationCtxFactory getEvalCtxFactory() {
		return EvaluationCtxFactory.getFactory();
	}

	@Bean PolicyStore getPolicyStore() {
		return new FileBasedClientIdServiceIdPolicyStore("/home/malik/policies/", new ReentrantReadWriteLock());
	}

}
