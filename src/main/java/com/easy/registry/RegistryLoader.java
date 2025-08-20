package com.easy.registry;

import com.easy.config.RpcProperties;

import java.util.ServiceLoader;

public final class RegistryLoader {

	private RegistryLoader() {
	}

	public RegistryService load(RpcProperties.Registry registry) {
		ServiceLoader<RegistryService> loader = ServiceLoader.load(RegistryService.class);
		RegistryService first = null;
		for (RegistryService rs : loader) {
			if (first == null) first = rs;
			if (registry.getType().equals(rs.type())) {
				rs.init(registry);
				return rs;
			}
		}
		if (first != null) {
			first.init(registry);
			return first;
		}
		throw new IllegalStateException("No RegistryService found for type: " + registry.getType());
	}
}
