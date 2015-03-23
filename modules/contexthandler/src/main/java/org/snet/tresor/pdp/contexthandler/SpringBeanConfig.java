package org.snet.tresor.pdp.contexthandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.snet.tresor.pdp.additions.GeoPDP;
import org.snet.tresor.pdp.additions.finder.impl.PIPAttributeFinderModule;
import org.snet.tresor.pdp.additions.finder.impl.PolicyStorePolicyFinderModule;
import org.snet.tresor.pdp.additions.pip.PIP;
import org.snet.tresor.pdp.additions.pip.impl.LocationPIP;
import org.snet.tresor.pdp.additions.pip.impl.StationPIP;
import org.snet.tresor.pdp.additions.pip.impl.WeekdayPIP;
import org.snet.tresor.pdp.additions.policystore.AbstractClientIdServiceIdPolicyStore;
import org.snet.tresor.pdp.additions.policystore.FileBasedClientIdServiceIdPolicyStore;
import org.snet.tresor.pdp.additions.policystore.RedisClientIdServiceIdPolicyStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.ctx.EvaluationCtxFactory;
import org.wso2.balana.ctx.RequestCtxFactory;
import org.wso2.balana.finder.*;
import org.wso2.balana.finder.impl.CurrentEnvModule;
import org.wso2.balana.finder.impl.SelectorModule;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Configuration
@ComponentScan
@EnableWebMvc
public class SpringBeanConfig {
    @Inject Environment env;
    @Inject YamlConfig yamlConfig;

    @Bean
    PDP pdp() throws IOException {
        return GeoPDP.getGeoExtendedPDP(pdpConfig());
    }

    @Bean
    PDPConfig pdpConfig() throws IOException {
        return new PDPConfig(attributeFinder(), policyFinder(), resourceFinder());
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
    PIPAttributeFinderModule pipAttributeFinderModule() {
        List<PIP> pips = new ArrayList<>();
        pips.add(new WeekdayPIP());

        ObjectMapper objectmapper = objectMapper();

        for (Map<String, String> locationpip : yamlConfig.getLocationpips())
            pips.add(new LocationPIP(locationpip.get("url"), locationpip.get("authentication"), objectmapper));

        for (Map<String, String> stationpip : yamlConfig.getStationpips())
            pips.add(new StationPIP(stationpip.get("url"), objectmapper));

        return new PIPAttributeFinderModule(pips);
    }

	@Bean
	AbstractClientIdServiceIdPolicyStore policyStore() throws IOException {
        AbstractClientIdServiceIdPolicyStore policyStore = null;
        Map<String, String> config = yamlConfig.getPolicystore();

        String type = (config.containsKey("type")) ? config.get("type") : "file";

        if (type.equals("file")) {
            String path = (env.containsProperty("policystore.path")) ? env.getProperty("policystore.path") :
                    (config.containsKey("path")) ? config.get("path") :
                            new File(".").getCanonicalPath() + "/policies/";

            policyStore = new FileBasedClientIdServiceIdPolicyStore(path, new ReentrantReadWriteLock());
        }

        if (type.equals("redis")) {
            String host = (config.containsKey("host")) ? config.get("host") : "localhost";
            int port = (config.containsKey("port")) ? Integer.parseInt(config.get("port")) : 6379;
            int timeout = (config.containsKey("timeout")) ? Integer.parseInt(config.get("timeout")) : 2000;
            String password = config.get("password");

            policyStore = new RedisClientIdServiceIdPolicyStore(host, port, timeout, password);
        }

        return policyStore;
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
