package org.snet.tresor.pdp.contexthandler.controller;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.snet.tresor.pdp.additions.XACMLHelper;
import org.snet.tresor.pdp.additions.policystore.AbstractClientIdServiceIdPolicyStore;
import org.snet.tresor.pdp.contexthandler.LogHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Controller class for the PolicyStore API
 */
@RestController
@RequestMapping("/policy")
@PreAuthorize("isAuthenticated() and hasAnyRole('ROLE_'.concat(#clientId), 'ROLE_BROKER', 'ROLE_PAP')")
public class ClientIdServiceIdPolicyStoreController {
	private static final Logger log = LoggerFactory.getLogger(ClientIdServiceIdPolicyStoreController.class);

	private AbstractClientIdServiceIdPolicyStore store;
	private ObjectMapper mapper;

	@Inject
	public ClientIdServiceIdPolicyStoreController(AbstractClientIdServiceIdPolicyStore store, ObjectMapper mapper) {
		this.store = store;
		this.mapper = mapper;
	}

	private String getSubjectId(UserDetails user) {
		return user.getUsername()+"\\\\"+user.getAuthorities().iterator().next().getAuthority().substring(5);
	}

	@RequestMapping(value="/{clientId}", method=RequestMethod.GET, produces="application/json")
	public ResponseEntity<String> getPolicies(@PathVariable String clientId,
			@AuthenticationPrincipal UserDetails user) throws JsonProcessingException {

		LogHelper.putMDCs("Policy Retrieval", clientId, this.getSubjectId(user));
		log.info("Retrieving all policies of {}", clientId);

		Map<String, String> values = this.store.get(clientId);
		ResponseEntity<String> result = null;
		if (values == null) {
			log.info("No policies found for client {}", clientId);
			result = new ResponseEntity<String>(HttpStatus.NOT_FOUND);
		} else {
			log.info("Retrieved {} policies of client {}", values.size(), clientId);
			String valuesJson = this.mapper.writeValueAsString(values);
			result = new ResponseEntity<String>(valuesJson, HttpStatus.OK);
		}

		MDC.clear();
		return result;
	}

	@RequestMapping(value="/{clientId}/{serviceId}", method=RequestMethod.GET, produces="application/xacml+xml")
	public ResponseEntity<String> getPolicy(@PathVariable String clientId, @PathVariable String serviceId,
			@AuthenticationPrincipal UserDetails user) {

		LogHelper.putMDCs("Policy Retrieval", clientId, this.getSubjectId(user));
		log.info("Retrieving policy of client {} for service {}", clientId, serviceId);

		String policy = this.store.get(clientId, serviceId);
		ResponseEntity<String> result = null;
		if (policy == null) {
			log.info("No policy found for client {} and service {}", clientId, serviceId);
			result = new ResponseEntity<String>(HttpStatus.NOT_FOUND);
		} else {
			result = new ResponseEntity<String>(policy, HttpStatus.OK);
		}

		MDC.clear();
		return result;
	}


	@RequestMapping(value="/{clientId}/{serviceId}", method=RequestMethod.PUT, consumes="application/xacml+xml")
	public ResponseEntity<String> putPolicy(@PathVariable String clientId, @PathVariable String serviceId,
			@RequestBody String policy, @AuthenticationPrincipal UserDetails user) {

		LogHelper.putMDCs("Policy Insertion", clientId, this.getSubjectId(user));
		log.info("Inserting policy for client {} and service {}", clientId, serviceId);

		try {
			// check validity of policy
			XACMLHelper.loadPolicy(policy);
			boolean isExistingPolicy = this.store.hasPolicy(clientId, serviceId);
			this.store.put(clientId, serviceId, policy.toString());

			HttpStatus httpStatus = (isExistingPolicy) ? HttpStatus.NO_CONTENT : HttpStatus.CREATED;
			return new ResponseEntity<String>(httpStatus);

		} catch (Exception e) {
			log.info("Failed to insert policy for client {} and service {}", clientId, serviceId, e);
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
		} finally {
			MDC.clear();
		}
	}


	@RequestMapping(value="/{clientId}/{serviceId}", method=RequestMethod.DELETE)
	public ResponseEntity<String> deletePolicy(@PathVariable String clientId, @PathVariable String serviceId,
			@AuthenticationPrincipal UserDetails user) {

		LogHelper.putMDCs("Policy Deletion", clientId, this.getSubjectId(user));
		log.info("Deleting policy of client {} for service {}", clientId, serviceId);

		ResponseEntity<String> result = null;
		if (!this.store.hasPolicy(clientId, serviceId)) {
			log.info("No Policy found for client {} and service {} to delete", clientId, serviceId);
			result = new ResponseEntity<String>(HttpStatus.NOT_FOUND);

		} else {
			if (this.store.delete(clientId, serviceId)) {
				log.info("Deleted policy of client {} for service {}", clientId, serviceId);
				result = new ResponseEntity<String>(HttpStatus.NO_CONTENT);
			} else {
				log.error("Failed to delete policy of client {} for service {}", clientId, serviceId);
				result = new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		MDC.clear();
		return result;
	}

}
