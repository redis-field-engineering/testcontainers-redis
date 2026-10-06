package com.redis.testcontainers;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.lifecycle.Startable;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RedisModulesCommands;
import com.redis.lettucemod.cluster.RedisModulesClusterClient;
import com.redis.lettucemod.timeseries.CreateOptions;
import com.redis.lettucemod.timeseries.Sample;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.KeyValue;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;

@TestInstance(Lifecycle.PER_CLASS)
@SuppressWarnings("unchecked")
public abstract class AbstractRedisTestBase {

	private RedisServer redis;
	private AbstractRedisClient client;
	private StatefulRedisModulesConnection<String, String> connection;
	private RedisModulesCommands<String, String> commands;

	protected abstract RedisServer getRedisServer();

	@BeforeAll
	public void setup() {
		redis = getRedisServer();
		if (redis instanceof Startable) {
			((Startable) redis).start();
		}
		if (redis.isRedisCluster()) {
			RedisModulesClusterClient clusterClient = RedisModulesClusterClient.create(redis.getRedisURI());
			client = clusterClient;
			connection = clusterClient.connect();
		} else {
			RedisModulesClient standaloneClient = RedisModulesClient.create(redis.getRedisURI());
			client = standaloneClient;
			connection = standaloneClient.connect();
		}
		commands = connection.sync();
	}

	@AfterAll
	public void teardown() {
		commands = null;
		if (connection != null) {
			connection.close();
		}
		if (client != null) {
			client.close();
		}
		if (redis instanceof Startable) {
			((Startable) redis).stop();
		}
	}

	@BeforeEach
	void flushall() {
		commands.flushall();
	}

	@Test
	void ping() {
		Assertions.assertEquals("PONG", commands.ping());
	}

	@Test
	void search() {
		commands.ftCreate("test", List.of(TextFieldArgs.<String>builder().name("name").build(),
				TagFieldArgs.<String>builder().name("id").build()));
		int count = 10;
		for (int index = 0; index < count; index++) {
			Map<String, String> doc = new HashMap<>();
			doc.put("name", "name " + index);
			doc.put("id", String.valueOf(index + 1));
			commands.hset("hash:" + index, doc);
		}
		Awaitility.await().atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> Assertions.assertEquals(count, commands.ftSearch("test", "*").getCount()));
	}

	@Test
	void timeseries() {
		// TimeSeries tests
		commands.tsCreate("temperature:3:11", CreateOptions.<String, String>builder().retentionPeriod(6000)
				.labels(KeyValue.just("sensor_id", "2"), KeyValue.just("area_id", "32")).build());
		// TS.ADD temperature:3:11 1548149181 30
		Long add1 = commands.tsAdd("temperature:3:11", Sample.of(1548149181, 30));
		Assertions.assertEquals(1548149181, add1);
	}

	@Test
	void writeHash() {
		// Write test
		Map<String, String> map = new HashMap<>();
		map.put("field1", "value1");
		String key = "testhash";
		commands.hset(key, map);
		Assertions.assertEquals(map, commands.hgetall(key));
	}

}
