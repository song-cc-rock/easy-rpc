package com.easy.registry;

import com.easy.config.RpcProperties;

import java.util.List;
import java.util.Map;

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
	 * @param key 服务Key
	 */
	void deregister(String key);

	/**
	 * 发现服务实例
	 * @param instance 服务实例
	 * @param interfaceName 接口名称
	 * @return 服务实例
	 */
	List<RpcProperties.ServiceInstance> discover(RpcProperties.ServiceInstance instance, String interfaceName);

	/**
	 * 销毁注册中心
	 */
	void close();

	/**
	 * 获取注册中心类型
	 * @return 注册中心类型
	 */
	String type();

	/**
	 * 获取服务Key
	 * @param instance 服务实例
	 * @param interfaceName 接口名称
	 * @return Key
	 */
	String getServiceKey(RpcProperties.ServiceInstance instance, String interfaceName);
}
