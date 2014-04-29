package org.snet.policystore;

import java.util.List;
import java.util.Map;

public interface DBPolicyStoreManager {

	/**
	 * Retrieves Policy which corresponds to given id
	 * @param id
	 * @return xacml policy as string
	 */
	public String getPolicy(String id);
	
	/**
	 * Retrieves a List of all policies which correspond to given domain/service combination 
	 * @param domain
	 * @param service
	 * @return List of policies
	 */
	public List<String> getPolicy(String domain, String service);
	
	/**
	 * Adds a Policy to the PolicyStore
	 * @param domain, domain the policy is applicable for
	 * @param service, service the policy applicable for
	 * @param policy, the policy as string
	 * @return id of the new policy or -1 if failed
	 */
	public String addPolicy(String domain, String service, String policy);
	
	/**
	 * Delete Policy with given id
	 * @param policyId
	 * @return Deleted policy as string or null if failed
	 */
	public String deletePolicy(String policyId);
	
	/**
	 * Retrieves all policies belonging to the given domain as a Map
	 * @param domain
	 * @return Map with the policyIds as keys and the policy strings as values
	 */
	public Map<String, String> getAll(String domain);
	
}
