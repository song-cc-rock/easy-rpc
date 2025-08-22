package com.easy.provider;

import com.easy.annotation.Service;
import com.easy.config.RpcProperties;
import com.easy.registry.Registry;
import com.easy.registry.RegistryLoader;
import com.easy.server.RpcServer;
import io.vertx.core.net.NetServer;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class RpcProvider implements BeanPostProcessor, SmartInitializingSingleton, AutoCloseable {

	private final RpcProperties rpcProperties;
	private final Registry registry;
	private final Map<String, Object> serviceMap = new HashMap<>();
	private final List<NetServer> netServers = new CopyOnWriteArrayList<>();
	private final List<String> registryKeys = new CopyOnWriteArrayList<>();

	public RpcProvider(RpcProperties rpcProperties) {
		this.rpcProperties = rpcProperties;
		this.registry = RegistryLoader.load(rpcProperties.getRegistry());
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		Service easyService = targetClass.getAnnotation(Service.class);
		if (easyService != null) {
			Class<?>[] interfaces = targetClass.getInterfaces();
			for (Class<?> is : interfaces) {
				serviceMap.put(is.getName(), is);
			}
		}
		return bean;
	}

	@Override
	public void afterSingletonsInstantiated() {
		if (serviceMap.isEmpty()) {
			return;
		}
		RpcServer rpcServer = new RpcServer(serviceMap);
		RpcProperties.ServiceInstance instance = rpcProperties.getInstance();
		NetServer netServer = rpcServer.start(instance.getHost(), instance.getPort());
		netServers.add(netServer);
		for (String interfaceName : serviceMap.keySet()) {
			registry.register(instance, interfaceName);
			registryKeys.add(registry.getServiceKey(instance, interfaceName));
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try { close(); } catch (Exception ignored) {}
		}, "easy-rpc-shutdown-hook"));
	}

	@Override
	public void close() {
		for (String key : registryKeys) {
			try {
				registry.deregister(key);
			} catch (Exception ignored) {

			}
		}
		registryKeys.clear();
		for (NetServer s : netServers) {
			try {
				s.close();
			} catch (Exception ignored) {

			}
		}
		netServers.clear();
	}
}
