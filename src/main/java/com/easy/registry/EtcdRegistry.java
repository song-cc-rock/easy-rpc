package com.easy.registry;

import com.easy.config.RpcProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EtcdRegistry implements Registry{

	private RpcProperties.Registry registry;
	private Client client;
	private KV kvClient;
	private Lease leaseClient;
	/**
	 * 存储key和对应的租约ID
	 * 用于取消注册时使用
	 */
	private final Map<String, Long> leaseMap = new ConcurrentHashMap<>();

	private ScheduledExecutorService scheduler;

	@Override
	public void init(RpcProperties.Registry registry) {
		this.registry = registry;
		this.client = Client.builder().endpoints(registry.getEndpoints().toArray(new String[0])).build();
		this.kvClient = client.getKVClient();
		this.leaseClient = client.getLeaseClient();
		// 创建一个守护线程池，用于定时续租
		this.scheduler = new ScheduledThreadPoolExecutor(2, new ThreadFactory() {
			final AtomicInteger threadCount = new AtomicInteger(1);
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "etcd-lease-" + threadCount.getAndIncrement());
				thread.setDaemon(true);
				return thread;
			}
		});
	}

	@Override
	public void register(RpcProperties.ServiceInstance instance, String interfaceName) {
		try {
			// get lease
			long leaseId = leaseClient.grant(registry.getTtl()).get().getID();
			/*
			 * /ns/interface/version/host:port → instance metadata
			 */
			String key = getKey(instance, interfaceName);
			String value = JsonMapper.builder().build().writeValueAsString(instance);
			kvClient.put(ByteSequence.from(key, StandardCharsets.UTF_8), ByteSequence.from(value, StandardCharsets.UTF_8),
					PutOption.builder().withLeaseId(leaseId).build()).get();
			// keep alive
			scheduler.scheduleAtFixedRate(() -> leaseClient.keepAliveOnce(leaseId), 0, registry.getTtl() / 5, TimeUnit.SECONDS);
			leaseMap.put(key, leaseId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void deregister(String key) {
		try {
			kvClient.delete(ByteSequence.from(key, StandardCharsets.UTF_8)).get();
			leaseClient.revoke(leaseMap.get(key)).get();
			scheduler.shutdown();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<RpcProperties.ServiceInstance> discover(RpcProperties.ServiceInstance instance, String interfaceName) {
		// 获取服务实例
		List<RpcProperties.ServiceInstance> instances;
		try {
			KV kvClient = client.getKVClient();
			instances = kvClient.get(ByteSequence.from(getKeyWithOutInstance(instance, interfaceName), StandardCharsets.UTF_8), GetOption.builder().isPrefix(true).build())
					.thenApply(response -> response.getKvs().stream()
							.map(kv -> {
								try {
									return JsonMapper.builder().build().readValue(kv.getValue().toString(StandardCharsets.UTF_8), RpcProperties.ServiceInstance.class);
								} catch (JsonProcessingException e) {
									throw new RuntimeException(e);
								}
							})
							.toList()).join();
			return instances;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public void close() {
		client.close();
		scheduler.shutdown();
	}

	@Override
	public String type() {
		return "etcd";
	}

	/**
	 * 获取服务实例的唯一key {etc.namespace}/{interfaceName}/{version}/{host}:{port}
	 * @param instance 服务实例
	 * @param interfaceName 接口名称
	 * @return 服务实例Key
	 */
	@Override
	public String getServiceKey(RpcProperties.ServiceInstance instance, String interfaceName) {
		return getKey(instance, interfaceName);
	}

	/**
	 * 生成服务实例的唯一key {etc.namespace}/{interfaceName}/{version}/{host}:{port}
	 * @param instance 服务实例
	 * @param interfaceName 接口名称
	 * @return 唯一key
	 */
	public String getKey(RpcProperties.ServiceInstance instance, String interfaceName) {
		return "/" + registry.getNamespace() + "/" + interfaceName + "/" + instance.getVersion() + "/" + instance.getHost() + ":" + instance.getPort();
	}

	/**
	 * 生成服务实例的唯一key {etc.namespace}/{interfaceName}/{version}
	 * @param instance 服务实例
	 * @param interfaceName 接口名称
	 * @return 唯一key
	 */
	public String getKeyWithOutInstance(RpcProperties.ServiceInstance instance, String interfaceName) {
		return "/" + registry.getNamespace() + "/" + interfaceName + "/" + instance.getVersion();
	}
}
