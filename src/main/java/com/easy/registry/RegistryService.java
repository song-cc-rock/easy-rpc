package com.easy.registry;

import com.easy.config.RpcProperties;

public interface RegistryService {

	/**
	 * 初始化注册中心
	 * @param registry 注册中心配置
	 */
	void init(RpcProperties.Registry registry);

	/**
	 * 获取注册中心类型
	 * @return 注册中心类型
	 */
	String type();
}
