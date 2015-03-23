package org.snet.tresor.pdp.additions.policystore;

import java.util.Map;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Two-key-value policy store implementation using the redis database
 */
public class RedisClientIdServiceIdPolicyStore extends AbstractClientIdServiceIdPolicyStore {
	JedisPool redisPool;

    public RedisClientIdServiceIdPolicyStore(String host) {
        this(host, 6379, 2000, null);
    }

    public RedisClientIdServiceIdPolicyStore(String host, int port) {
        this(host, port, 2000, null);
    }

    public RedisClientIdServiceIdPolicyStore(String host, int port, int timeout) {
        this(host, port, timeout, null);
    }

    public RedisClientIdServiceIdPolicyStore(String host, int port, int timeout, String password) {
        this.redisPool = new JedisPool(new GenericObjectPoolConfig(), host, port, timeout, password);

        try {
            Jedis j = this.redisPool.getResource();
            this.redisPool.returnResource(j);
        } catch (JedisConnectionException e) {
            throw new RuntimeException("Failed to establish connection with redisDB", e);
        }
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
