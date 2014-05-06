package org.snet.policystore;

import java.util.Map;

public interface DBPolicyStoreManager {
	
	/**
	 * Retrieves all policies belonging to the given domain as a Map
	 * @param domain
	 * @return Map with the serviceIds as keys and the policy strings as values
	 */
	public Map<String, String> getAll(String domain);
	
	/**
	 * Retrieves the corresponding policy 
	 * @param domain, customer domain of policy
	 * @param service, service the policy is valid for
	 * @return policy
	 */
	public String getPolicy(String domain, String service);
	
	/**
	 * Adds a Policy to the PolicyStore
	 * @param domain, domain the policy is applicable for
	 * @param service, service the policy applicable for
	 * @param policy, the policy as string
	 * @return id of the new policy or -1 if failed
	 */
	public String addPolicy(String domain, String service, String policy);
	
	/**
	 * Deletes Policy which corresponds to given domain/service combination
	 * @param domain
	 * @param service
	 * @return Deleted policy as string or null if failed
	 */
	public String deletePolicy(String domain, String service);
	
}
