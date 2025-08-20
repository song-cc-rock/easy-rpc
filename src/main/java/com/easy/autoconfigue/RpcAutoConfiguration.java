package com.easy.autoconfigue;

import com.easy.config.RpcProperties;
import com.easy.consumer.ReferenceProcessor;
import com.easy.consumer.RpcClient;
import com.easy.provider.RpcProvider;
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
	public RpcProvider rpcProvider(RpcProperties props) {
		return new RpcProvider(props);
	}

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "easy.rpc.enabled", havingValue = "true")
	public Registry registryService(RpcProperties props) {
		return RegistryLoader.load(props.getRegistry());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "easy.rpc.enabled", havingValue = "true")
	public RpcClient rpcClient(Registry registry, RpcProperties props) {
		return new RpcClient(registry, props.getInstance());
	}

	@Bean
	@ConditionalOnMissingBean
	public ReferenceProcessor referenceProcessor(RpcClient rpcClient) {
		return new ReferenceProcessor(rpcClient);
	}
}
