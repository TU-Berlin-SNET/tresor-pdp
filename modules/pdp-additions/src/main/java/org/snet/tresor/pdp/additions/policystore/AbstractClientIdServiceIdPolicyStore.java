package org.snet.tresor.pdp.additions.policystore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.XACMLHelper;
import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.PolicyFinder;

public abstract class AbstractClientIdServiceIdPolicyStore implements TwoKeyValuePolicyStore {
	private static final Logger log = LoggerFactory.getLogger(AbstractClientIdServiceIdPolicyStore.class);

	public AbstractPolicy get(EvaluationCtx ctx, PolicyFinder finder) {
		String clientId = XACMLHelper.getClientID(ctx);
		String serviceId = XACMLHelper.getServiceID(ctx);
		String policy = this.get(clientId, serviceId);
		try {
			if (policy != null)
				return XACMLHelper.loadPolicyOrPolicySet(policy, finder);
		} catch (Exception e) {
			log.error("Found corresponding policy for client {} and service {} but failed to load it", clientId, serviceId);
		}
		return null;
	}

}
