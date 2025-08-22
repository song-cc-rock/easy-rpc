package com.easy.consumer;

import com.easy.annotation.Reference;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReferenceProcessor implements BeanPostProcessor, ApplicationContextAware {

	private ApplicationContext applicationContext;
	private final Map<Class<?>, Object> referenceProxyCache = new ConcurrentHashMap<>();

	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		for (Field field : bean.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Reference.class)) {
				field.setAccessible(true);
				// 代理对象复用
				referenceProxyCache.computeIfAbsent(field.getType(), k -> Proxy.newProxyInstance(k.getClassLoader(), new Class[]{k},
						(proxy, method, args) -> {
							// 使用RPC客户端调用远程服务
							RpcClient rpcClient = applicationContext.getBean(RpcClient.class);
							return rpcClient.callSync(k.getName(), method, args, method.getReturnType());
						}));
				try {
					field.set(bean, referenceProxyCache.get(field.getType()));
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return bean;
	}
}
