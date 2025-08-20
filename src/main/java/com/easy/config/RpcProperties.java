package com.easy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "easy.rpc")
public class RpcProperties {

	/**
	 * 是否启用rpc自动配置
	 */
	private boolean enabled;

	/**
	 * 注册中心配置
	 */
	private Registry registry;

	/**
	 * 服务实例配置
	 */
	private ServiceInstance instance;

	@Data
	public static class Registry{

		/**
		 * 节点地址
		 */
		private List<String> endpoints;
		/**
		 * 注册中心类型
		 * 可选值：etcd, zookeeper, consul等
		 */
		private String type;
		/**
		 * 命名空间
		 */
		private String namespace;
		/**
		 * 租约 (可选)
		 */
		private long ttl;
	}

	@Data
	public static class ServiceInstance {

		/**
		 * 端口
		 */
		private int port;
		/**
		 * 版本
		 */
		private String version;
	}
}
