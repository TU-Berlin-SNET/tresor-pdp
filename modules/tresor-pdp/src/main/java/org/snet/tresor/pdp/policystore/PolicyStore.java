package org.snet.tresor.pdp.policystore;

import java.util.Map;

public interface PolicyStore {
	
	/**
	 * Retrieves all policies belonging to the given clientID as a Map
	 * @param clientID client identifier
	 * @return Map with the serviceIds as keys and the policy strings as values
	 */
	public Map<String, String> get(String clientID);
	
	/**
	 * Retrieves the corresponding policy 
	 * @param clientID client identifier
	 * @param serviceID service identifier
	 * @return policy or null if there is none
	 */
	public String get(String clientID, String serviceID);
	
	/**
	 * Puts a Policy to the PolicyStore
	 * @param clientID client identifier
	 * @param serviceID service identifier
	 * @param policy the policy as string
	 * @return id of the new policy or null if failed
	 */
	public String put(String clientID, String serviceID, String policy);
	
	/**
	 * Deletes Policy which corresponds to given clientID/serviceID combination
	 * @param clientID client identifier
	 * @param serviceID service identifier
	 * @return 1 if successful, 0 if failed
	 */
	public int delete(String clientID, String serviceID);
	
	/**
	 * Disconnect from PolicyStore and/or close PolicyStore
	 * Free resources
	 */
	public void close();
	
}
