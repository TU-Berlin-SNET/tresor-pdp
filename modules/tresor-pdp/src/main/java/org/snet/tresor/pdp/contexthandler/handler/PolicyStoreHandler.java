package org.snet.tresor.pdp.contexthandler.handler;

import java.io.Reader;
import java.util.Map;

import org.json.JSONObject;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.contexthandler.authentication.Authenticator;
import org.snet.tresor.pdp.policystore.PolicyStore;
import org.w3c.dom.Document;
import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.ParsingException;
import org.wso2.balana.Policy;

/**
 * Handler for interfacing with the PolicyStore
 * @author malik
 */
public class PolicyStoreHandler {
	private static final Logger log = LoggerFactory.getLogger(PolicyStoreHandler.class);

	private ParserPool parser;
	private PolicyStore policyStore;

	/**
	 * Create a new PolicyStoreHandler for interfacing with given policyStore using given authenticator for authentication
	 * @param policyStore
	 * @param authenticator
	 */
	public PolicyStoreHandler(PolicyStore policyStore, Authenticator authenticator) {
		this.policyStore = policyStore;
		this.parser = new BasicParserPool();
	}

	/**
	 * Retrieve all policies for given client
	 * @param clientID
	 * @return all policies as String in json format, i.e. "{ ":service-id" : ":policy", ... }"
	 */
	public String retrieve(String clientID) {
		log.info("Retrieving all policies for client {}", clientID);
		Map<String, String> policies = this.policyStore.get(clientID);

		log.debug("Creating JSONObject containing retrieved policies");
		JSONObject j = new JSONObject(policies);

		log.debug("Returning retrieved policies");
		return j.toString();
	}

	/**
	 * Retrieve policy corresponding to given client and service ids
	 * @param clientID
	 * @param serviceID
	 * @return Policy as a string or null if not found
	 */
	public String retrieve(String clientID, String serviceID) {
		log.info("Retrieving policy for client {} and service {}", clientID, serviceID);

		String policy = this.policyStore.get(clientID, serviceID);
		if (policy == null)
			log.info("Policy for client {} and service {} not found", clientID, serviceID);

		return policy;
	}

	/**
	 * Put a policy to policyStore with given client and service ids
	 * @param clientID
	 * @param serviceID
	 * @param reader the xacml-policy
	 * @return true if successful, false otherwise
	 * @throws XMLParserException if XML of policy is invalid
	 * @throws ParsingException if XACML of policy is invalid
	 */
	public boolean put(String clientID, String serviceID, Reader reader) throws XMLParserException, ParsingException {
		log.debug("Inserting a policy for client {} and service {}", clientID, serviceID);
		Document doc = this.parser.parse(reader);
		log.debug("XML is valid");

		// BREAKS when PolicySet is used
		AbstractPolicy policy = Policy.getInstance(doc.getDocumentElement());
		log.debug("XACML is valid");

		String id = this.policyStore.put(clientID, serviceID, policy.encode());

		boolean success = !(id == null);
		if (!success)
			log.error("Unexpected Error: Inserting a policy for client {} and service {} failed", clientID, serviceID);
		else
			log.info("Successfully inserted a policy for client {} and service {}", clientID, serviceID);

		return success;
	}

	/**
	 * Delete a policy from policyStore corresponding to given client and service ids
	 * @param clientID
	 * @param serviceID
	 * @return true if successful, false otherwise
	 */
	public boolean delete(String clientID, String serviceID) {
		log.debug("Deleting policy for client {} and service {}", clientID, serviceID);

		boolean success = this.policyStore.delete(clientID, serviceID) == 1;
		if (!success)
			log.warn("Deleting policy for client {} and service {} failed, policy does not exist or unexpected error", clientID, serviceID);
		else
			log.info("Successfully deleted a policy for client {} and service {}", clientID, serviceID);

		return success;
	}

}
