/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.xacml.transport;

import org.wso2.balana.PDP;

/**
 * Default factory creating transport objects for a local PDP
 * 
 * @author Mueller Christian
 * 
 */
public class XACMLLocalTransportFactory implements XACMLTransportFactory {

    XACMLTransport transport;

    public XACMLLocalTransportFactory(PDP pdp, boolean multithreaded) {

        transport = new XACMLLocalTransport(pdp, multithreaded);
    }

    public XACMLTransport getXACMLTransport() {
        return transport;
    }

}
