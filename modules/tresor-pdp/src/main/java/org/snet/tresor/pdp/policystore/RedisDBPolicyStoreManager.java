package org.snet.tresor.pdp.policystore;

import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisDBPolicyStoreManager implements DBPolicyStoreManager {
	static RedisDBPolicyStoreManager instance;
	
	public static RedisDBPolicyStoreManager getInstance() {
		if (instance == null)
			instance = new RedisDBPolicyStoreManager();
		
		return instance;
	}
	
	JedisPool redisdbpool;
	Jedis redisdb;
	
	private RedisDBPolicyStoreManager() {
		this.redisdbpool = new JedisPool(new JedisPoolConfig(), "localhost");
		this.redisdb = redisdbpool.getResource();
	}

	public Map<String, String> getAll(String domain) {
		return this.redisdb.hgetAll(domain);
	}

	public String getPolicy(String domain, String service) {
		return this.redisdb.hget(domain, service);
	}

	public String addPolicy(String domain, String service, String policy) {		
		long result = this.redisdb.hsetnx(domain, service, policy);		
		if (result == 0)
			return null;
		else
			return service;
	}

	public int deletePolicy(String domain, String service) {		
		long result = this.redisdb.hdel(domain, service);
		if (result == 0)
			return 0;
		else
			return 1;
	}
	
}
