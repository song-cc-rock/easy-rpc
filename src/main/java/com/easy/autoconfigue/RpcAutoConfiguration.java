package com.easy.autoconfigue;

import com.easy.config.RpcProperties;
import com.easy.consumer.ReferenceProcessor;
import com.easy.consumer.RpcClient;
import com.easy.provider.RpcProvider;
import com.easy.provider.ServiceProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RpcProperties.class)
public class RpcAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public static ServiceProcessor serviceProcessor() {
		return new ServiceProcessor();
	}

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "easy.rpc.enabled", havingValue = "true")
	public static RpcProvider rpcProvider(RpcProperties props, ServiceProcessor serviceProcessor) {
		return new RpcProvider(props, serviceProcessor);
	}

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "easy.rpc.enabled", havingValue = "true")
	public static RpcClient rpcClient(RpcProperties props) {
		return new RpcClient(props);
	}

	@Bean
	@ConditionalOnMissingBean
	public static ReferenceProcessor referenceProcessor() {
		return new ReferenceProcessor();
	}
}
