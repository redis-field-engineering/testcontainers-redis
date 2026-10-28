package com.redis.testcontainers;

import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

abstract class AbstractRedisContainer<C extends AbstractRedisContainer<C>> extends GenericContainer<C>
		implements RedisServer {

	public static final int REDIS_PORT = 6379;

	protected AbstractRedisContainer(String dockerImageName) {
		this(DockerImageName.parse(dockerImageName));
	}

	protected AbstractRedisContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
		addExposedPorts(REDIS_PORT);
		// The log message signals readiness inside the container.
		// The port check ensures the host-mapped port is reachable too.
		waitingFor(new WaitAllStrategy()
				.withStrategy(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
				.withStrategy(Wait.forListeningPorts(REDIS_PORT)).withStartupTimeout(Duration.ofSeconds(60)));
	}

	@SuppressWarnings("unchecked")
	public C withKeyspaceNotifications() {
		withCopyFileToContainer(MountableFile.forClasspathResource("redis-keyspace-notifications.conf"),
				"/data/redis.conf");
		withCommand("redis-server", "/data/redis.conf");
		return (C) this;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public String getRedisHost() {
		return getHost();
	}

	@Override
	public int getRedisPort() {
		return getFirstMappedPort();
	}

	@Override
	public boolean isRedisCluster() {
		return false;
	}

}
