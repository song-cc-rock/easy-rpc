package com.easy.registry;

import com.easy.config.RpcProperties;

import java.util.ServiceLoader;

public final class RegistryLoader {

	private RegistryLoader() {
	}

	public static Registry load(RpcProperties.Registry registry) {
		ServiceLoader<Registry> loader = ServiceLoader.load(Registry.class);
		Registry first = null;
		for (Registry rs : loader) {
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
