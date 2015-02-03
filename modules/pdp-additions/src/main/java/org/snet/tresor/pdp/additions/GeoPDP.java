package org.snet.tresor.pdp.additions;

import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;

/**
 * Convenience class for geoXACML initialization on PDP creation
 */
public class GeoPDP {

    public static PDP getGeoExtendedPDP(PDPConfig pdpConfig) {
        GeoXACML.initialize();
        return new PDP(pdpConfig);
    }

}
