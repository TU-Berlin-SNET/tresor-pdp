package org.snet.tresor.pdp.additions.policystore;

import java.util.Map;

/**
 * Interface for a Policystore which maps two keys(Key1, Key2) to one policy
 */
public interface TwoKeyValuePolicyStore extends PolicyStore {

	/**
	 * Check whether this policy store has a corresponding policy
	 * NOTE: no guarantees for any subsequent actions
	 * @param key1
	 * @param key2
	 * @return true if it exists, false otherwise
	 */
	public boolean hasPolicy(String key1, String key2);

	/**
	 * Retrieves all policies mapping to key1
	 * @param key1 the first key
	 * @return a map containing Key2 to Policy mappings or null
	 */
	public Map<String, String> get(String key1);

	/**
	 * Retrieves the corresponding policy
	 * @param key1 the first key
	 * @param key2 the second key
	 * @return policy or null
	 */
	public String get(String key1, String key2);

	/**
	 * Puts a policy to the PolicyStore
	 * @param key1 the first key
	 * @param key2 the second key
	 * @param policy the policy
	 */
	public void put(String key1, String key2, String policy);

	/**
	 * Deletes corresponding policy
	 * @param key1 the first key
	 * @param key2 the second key
	 * @return true if successful, false otherwise
	 */
	public boolean delete(String key1, String key2);

}
