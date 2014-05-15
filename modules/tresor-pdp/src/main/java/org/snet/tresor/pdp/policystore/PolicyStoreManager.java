package org.snet.tresor.pdp.policystore;

import java.util.Map;

public interface PolicyStoreManager {
	
	/**
	 * Retrieves all policies belonging to the given domain as a Map
	 * @param domain, customer domain
	 * @return Map with the serviceIds as keys and the policy strings as values
	 */
	public Map<String, String> getAll(String domain);
	
	/**
	 * Retrieves the corresponding policy 
	 * @param domain, customer domain of policy
	 * @param service, service the policy is valid for (also in our case: id)
	 * @return policy or null if there is none
	 */
	public String getPolicy(String domain, String service);
	
	/**
	 * Adds a Policy to the PolicyStore
	 * @param domain, domain the policy is applicable for
	 * @param service, service the policy is applicable for (also in our case: id)
	 * @param policy, the policy as string
	 * @return id of the new policy or null if failed
	 */
	public String addPolicy(String domain, String service, String policy);
	
	/**
	 * Deletes Policy which corresponds to given domain/service combination
	 * @param domain, domain the policy is applicable for
	 * @param service, service the policy is applicable for (also in our case: id)
	 * @return 1 if successful, 0 if failed
	 */
	public int deletePolicy(String domain, String service);
	
	/**
	 * Disconnect from PolicyStore and/or close PolicyStoreManager
	 */
	public void close();
	
}
