package com.easy.autoconfigue;

import com.easy.config.RpcProperties;
import com.easy.registry.Registry;
import com.easy.registry.RegistryLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RpcProperties.class)
public class RpcAutoConfiguration {

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "easy.rpc.enabled", havingValue = "true")
	public Registry registryService(RpcProperties props) {
		return RegistryLoader.load(props.getRegistry());
	}

}
