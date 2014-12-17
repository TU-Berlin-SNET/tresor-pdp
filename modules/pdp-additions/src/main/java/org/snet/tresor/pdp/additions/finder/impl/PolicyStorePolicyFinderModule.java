package org.snet.tresor.pdp.additions.finder.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.policystore.PolicyStore;
import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.MatchResult;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.PolicyFinderResult;

/**
 * PolicyFinderModule which uses the PolicyStore interface to retrieve
 * policies from the Policy Store
 */
public class PolicyStorePolicyFinderModule extends PolicyFinderModule{
	private static final Logger log = LoggerFactory.getLogger(PolicyStorePolicyFinderModule.class);

	private PolicyFinder finder;
	private PolicyStore policyStore;

	public PolicyStorePolicyFinderModule(PolicyStore policyStore) {
		this.policyStore = policyStore;
	}

	@Override
	public void init(PolicyFinder finder) {
		this.finder = finder;
	}

    @Override
    public boolean isRequestSupported() {
        return true;
    }

	@Override
	public PolicyFinderResult findPolicy(EvaluationCtx context) {

		AbstractPolicy policy = this.policyStore.get(context, finder);

		// if policy was successfully loaded, evaluate
		if (policy != null) {
			MatchResult match = policy.match(context);

			if (match.getResult() == MatchResult.INDETERMINATE)
				return new PolicyFinderResult(match.getStatus());

			if (match.getResult() == MatchResult.MATCH)
				return new PolicyFinderResult(policy);
		}

		// no matching policy found
		log.debug("No matching policy found");
		return new PolicyFinderResult();
	}

}
