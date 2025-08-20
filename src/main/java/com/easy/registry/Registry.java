package com.easy.registry;

import com.easy.config.RpcProperties;

public interface Registry {

	/**
	 * 初始化注册中心
	 * @param registry 注册中心配置
	 */
	void init(RpcProperties.Registry registry);

	/**
	 * 注册服务实例
	 * @param instance 服务实例
	 * @param interfaceName 接口名称
	 */
	void register(RpcProperties.ServiceInstance instance, String interfaceName);

	/**
	 * 注销服务实例
	 * @param instance 服务实例
	 * @param interfaceName 接口名称
	 */
	void deregister(RpcProperties.ServiceInstance instance, String interfaceName);

	/**
	 * 销毁注册中心
	 */
	void close();

	/**
	 * 获取注册中心类型
	 * @return 注册中心类型
	 */
	String type();

}
