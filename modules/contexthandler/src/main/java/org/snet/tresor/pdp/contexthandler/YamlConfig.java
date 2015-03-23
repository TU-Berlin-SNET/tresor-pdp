package org.snet.tresor.pdp.contexthandler;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix="pdp")
public class YamlConfig {

    private List<Map<String, String>> locationpips = new ArrayList<>();
    public List<Map<String, String>> getLocationpips() {
        return this.locationpips;
    }

    private List<Map<String, String>> stationpips = new ArrayList<>();
    public List<Map<String, String>> getStationpips() {
        return this.stationpips;
    }

    private Map<String, String> policystore = new HashMap<>();
    public Map<String, String> getPolicystore() {
        return this.policystore;
    }


}
