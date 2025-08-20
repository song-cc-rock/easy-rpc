package com.easy.provider;

import com.easy.annotation.Service;
import com.example.etcd.EtcdServiceRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.HashMap;
import java.util.Map;

public class RpcProvider implements BeanPostProcessor {

	private final Map<String, Object> serviceMap = new HashMap<>();
	private final EtcdServiceRegistry etcdServiceRegistry;
	private final String host;
	private final int port;
	private final String version;

	public RpcProvider() {

	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Service easyService = bean.getClass().getAnnotation(Service.class);
		if (easyService != null) {
			Class<?>[] interfaces = easyService.getClass().getInterfaces();
			for (Class<?> is : interfaces) {
				serviceMap.put(is.getSimpleName(), is);
			}
		}
		return bean;
	}
}
