package com.easy.provider;

import com.easy.annotation.Service;
import lombok.Getter;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;

public class ServiceProcessor implements BeanPostProcessor {

	@Getter
	private final Map<String, Object> serviceMap = new HashMap<>();

	@Override
	public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		Service easyService = targetClass.getAnnotation(Service.class);
		if (easyService != null) {
			Class<?>[] interfaces = targetClass.getInterfaces();
			for (Class<?> is : interfaces) {
				serviceMap.put(is.getName(), bean);
			}
		}
		return bean;
	}
}
