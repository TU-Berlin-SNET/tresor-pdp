package org.snet.tresor.pdp.policystore;

import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * PolicyStore for storing/retrieving policies from a redis database
 * @author malik
 */
public class RedisDBPolicyStore implements PolicyStore {	
	JedisPool redisPool;
	
	/**
	 * Create RedisDBPolicyStore with default config & localhost as location
	 */
	public RedisDBPolicyStore() {
		this.redisPool = new JedisPool(new JedisPoolConfig(), "localhost");
	}
	
	/**
	 * Create RedisDBPolicyStore with given parameter(s)
	 * @param params contains location
	 */
	public RedisDBPolicyStore(String... params) {		
		this.redisPool = new JedisPool(new JedisPoolConfig(), params[0]);
	}

	public Map<String, String> get(String domain) {
		// get client from pool
		Jedis redis = this.redisPool.getResource();
		
		// get values from database
		Map<String, String> result = redis.hgetAll(domain);
		
		// return client to pool
		this.redisPool.returnResource(redis);
		
		// return values
		return result;
	}

	public String get(String domain, String service) {
		Jedis redis = this.redisPool.getResource();
		String result = redis.hget(domain, service);
		this.redisPool.returnResource(redis);
		return result;
	}

	public String put(String domain, String service, String policy) {
		Jedis redis = this.redisPool.getResource();
		long result = redis.hset(domain, service, policy);
		this.redisPool.returnResource(redis);
		
		if (result == 0)
			return null;
		else
			return service;
	}

	public int delete(String domain, String service) {
		Jedis redis = this.redisPool.getResource();		
		long result = redis.hdel(domain, service);
		this.redisPool.returnResource(redis);
		
		if (result == 0)
			return 0;
		else
			return 1;
	}
	
	public void close() {
		this.redisPool.destroy();
	}
	
}
