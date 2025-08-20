package com.easy.consumer;

import com.easy.config.RpcProperties;
import com.easy.registry.Registry;
import com.google.protobuf.ByteString;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import rpc.Rpc;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class RpcClient {

	private final Registry registry;
	private final Vertx vertx = Vertx.vertx();
	private final RpcProperties.ServiceInstance instance;
	private final Random random = new Random();
	public static final int SUCCESS_CODE = 200;

	public RpcClient(Registry registry, RpcProperties.ServiceInstance instance) {
		this.registry = registry;
		this.instance = instance;
	}

	/**
	 * 同步调用RPC服务
	 * @param interfaceName 接口名称
	 * @param method 方法
	 * @param args 参数
	 * @return 调用结果
	 */
	public Object callSync(String interfaceName, String method, Object[] args) {
		CompletableFuture<Object> future = callAsync(interfaceName, method, args);
		try {
			return future.get();
		} catch (Exception e) {
			throw new RuntimeException("EasyRpc call failed", e);
		}
	}

	/**
	 * 异步调用RPC服务
	 * @param interfaceName 接口名称
	 * @param method 方法名称
	 * @param args 参数
	 * @return CompletableFuture对象，包含调用结果或异常
	 */
	public CompletableFuture<Object> callAsync(String interfaceName, String method, Object[] args) {
		CompletableFuture<Object> future = new CompletableFuture<>();
		// 可用的服务实例
		List<RpcProperties.ServiceInstance> instances = registry.discover(instance, interfaceName);
		if (instances == null || instances.isEmpty()) {
			future.completeExceptionally(new RuntimeException("No available service instance for " + interfaceName));
			return future;
		}
		// 简化处理，实际应用中可使用负载均衡策略
		RpcProperties.ServiceInstance serviceInstance = instances.get(random.nextInt(instances.size()));
		// 封装请求参数
		Rpc.RpcRequest.Builder request = Rpc.RpcRequest.newBuilder()
				.setInterfaceName(interfaceName)
				.setMethodName(method);
		if (args != null) {
			for (Object a : args) {
				byte[] bs = (a == null ? new byte[0] : a.toString().getBytes());
				request.addParams(ByteString.copyFrom(bs));
			}
		}
		// 创建NetClient连接到服务实例, 获取异步结果
		NetClient client = vertx.createNetClient();
		client.connect(serviceInstance.getPort(), serviceInstance.getHost(), asyncResult -> {
			if (!asyncResult.succeeded()) {
				future.completeExceptionally(new RuntimeException("Connect failed: " + asyncResult.cause()));
				return;
			}
			asyncResult.result().handler(buffer -> {
				try {
					Rpc.RpcResponse resp = Rpc.RpcResponse.parseFrom(buffer.getBytes());
					if (resp.getCode() == SUCCESS_CODE) {
						future.complete(new String(resp.getData().toByteArray()));
					} else {
						future.completeExceptionally(new RuntimeException(resp.getMsg()));
					}
				} catch (Exception e) {
					future.completeExceptionally(e);
				} finally {
					asyncResult.result().close();
				}
			}).write(Buffer.buffer(request.build().toByteArray()));
		});
		return future;
	}


}
