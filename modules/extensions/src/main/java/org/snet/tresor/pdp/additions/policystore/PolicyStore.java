package org.snet.tresor.pdp.additions.policystore;

import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.PolicyFinder;

/**
 * Interface for a Policystore providing a method to retrieve policies
 */
public interface PolicyStore {

	/**
	 * Retrieves a policy. Implementation may or may not use data from given Parameters
	 * Does NOT imply matching!
	 * @param ctx the evaluationCtx
	 * @param finder the PolicyFinder
	 * @return AbstractPolicy or null
	 */
	public AbstractPolicy get(EvaluationCtx ctx, PolicyFinder finder);

}
