package org.snet.tresor.pdp.additions.policystore;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.XACMLHelper;
import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.PolicyFinder;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Two-key-value policy store implementation using the redis database
 */
public class RedisClientIdServiceIdPolicyStore implements TwoKeyValuePolicyStore {
	private static final Logger log = LoggerFactory.getLogger(RedisClientIdServiceIdPolicyStore.class);
	JedisPool redisPool;

	/**
	 * Create RedisClientIdServiceIdPolicyStore with given parameter(s)
	 * @param jedisPool a JedisPool connected to the redisdb
	 */
	public RedisClientIdServiceIdPolicyStore(JedisPool jedisPool) {
		this.redisPool = jedisPool;

		if (this.redisPool == null)
			throw new RuntimeException("Connection to RedisDB may not be null");
	}


	public boolean hasPolicy(String clientId, String serviceId) {
		Jedis redis = this.redisPool.getResource();
		boolean exists = redis.hexists(clientId, serviceId);
		this.redisPool.returnResource(redis);

		return exists;
	}


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

	public Map<String, String> get(String clientId) {
		// get client from pool
		Jedis redis = this.redisPool.getResource();

		// get values from database
		Map<String, String> result = redis.hgetAll(clientId);

		// return client to pool
		this.redisPool.returnResource(redis);

		// return values
		return result;
	}

	public String get(String clientId, String serviceId) {
		Jedis redis = this.redisPool.getResource();
		String result = redis.hget(clientId, serviceId);
		this.redisPool.returnResource(redis);

		return result;
	}


	public void put(String clientId, String serviceId, String policy) {
		Jedis redis = this.redisPool.getResource();
		redis.hset(clientId, serviceId, policy);
		this.redisPool.returnResource(redis);
	}

	public boolean delete(String clientId, String serviceId) {
		Jedis redis = this.redisPool.getResource();
		long result = redis.hdel(clientId, serviceId);
		this.redisPool.returnResource(redis);

		return (result != 0);
	}


	public void close() {
		this.redisPool.destroy();
	}

}
