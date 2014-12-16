package org.snet.tresor.pdp.additions.policystore;

import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Two-key-value policy store implementation using the redis database
 */
public class RedisClientIdServiceIdPolicyStore extends AbstractClientIdServiceIdPolicyStore {
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
