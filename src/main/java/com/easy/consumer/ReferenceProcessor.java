package com.easy.consumer;

import com.easy.annotation.Reference;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

public class ReferenceProcessor implements BeanPostProcessor {

	private final RpcClient rpcClient;

	public ReferenceProcessor(RpcClient rpcClient) {
		this.rpcClient = rpcClient;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		for (Field field : bean.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Reference.class)) {
				field.setAccessible(true);
				Object proxyInstance = Proxy.newProxyInstance(field.getType().getClassLoader(), new Class[]{field.getType()},
						(proxy, method, args) -> {
							// 使用RPC客户端调用远程服务
							return rpcClient.callSync(field.getType().getSimpleName(), method.getName(), args);
						});
				try {
					field.set(bean, proxyInstance);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return bean;
	}
}
