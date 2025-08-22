package com.easy.consumer;

import com.easy.config.RpcProperties;
import com.easy.registry.Registry;
import com.easy.registry.RegistryLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import lombok.extern.slf4j.Slf4j;
import rpc.Rpc;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RpcClient implements AutoCloseable{

	private final Registry registry;
	private final Vertx vertx = Vertx.vertx();
	private final RpcProperties rpcProperties;
	private final Random random = new Random();
	public static final int SUCCESS_CODE = 200;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final WorkerExecutor worker = vertx.createSharedWorkerExecutor("rpc-worker-pool");

	public RpcClient(RpcProperties props) {
		this.rpcProperties = props;
		this.registry = RegistryLoader.load(rpcProperties.getRegistry());
	}

	/**
	 * 同步调用RPC服务，线程安全，可在Vert.x事件循环线程调用
	 * @param interfaceName 接口名称
	 * @param method 方法对象
	 * @param args 参数数组
	 * @param returnType 返回类型Class
	 * @param <T> 泛型类型
	 * @return 调用结果
	 */
	public <T> T callSync(String interfaceName, Method method, Object[] args, Class<T> returnType) {
		if (Vertx.currentContext() != null && Vertx.currentContext().isEventLoopContext()) {
			CompletableFuture<T> future = new CompletableFuture<>();
			worker.executeBlocking(() -> callSyncInternal(interfaceName, method, args, returnType), res -> {
				if (res.succeeded()) {
					future.complete(res.result());
				} else {
					future.completeExceptionally(res.cause());
				}
			});

			try {
				return future.get();
			} catch (Exception e) {
				throw new RuntimeException("EasyRpc call failed", e);
			}
		} else {
			return callSyncInternal(interfaceName, method, args, returnType);
		}
	}

	/**
	 * 内部同步调用逻辑，不涉及事件循环线程判断
	 */
	private <T> T callSyncInternal(String interfaceName, Method method, Object[] args, Class<T> returnType) {
		try {
			CompletableFuture<Object> future = callAsync(interfaceName, method, args);
			Object result = future.get();
			if (returnType == Void.class || result == null) {
				return null;
			}
			return returnType.cast(result);
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
	public <T> CompletableFuture<T> callAsync(String interfaceName, Method method, Object[] args) {
		CompletableFuture<T> future = new CompletableFuture<>();
		// 可用的服务实例
		List<RpcProperties.ServiceInstance> instances = registry.discover(rpcProperties.getInstance(), interfaceName);
		if (instances == null || instances.isEmpty()) {
			future.completeExceptionally(new RuntimeException("No available service instance for " + interfaceName));
			return future;
		}
		// 简化处理，实际应用中可使用负载均衡策略
		RpcProperties.ServiceInstance serviceInstance = instances.get(random.nextInt(instances.size()));
		// 封装请求参数
		try {
			Rpc.RpcRequest.Builder request = Rpc.RpcRequest.newBuilder()
					.setInterfaceName(interfaceName)
					.setMethodName(method.getName());
			if (args != null) {
				for (Object a : args) {
					byte[] bs = objectMapper.writeValueAsBytes(a);
					request.addParams(ByteString.copyFrom(bs));
				}
			}
			// 创建NetClient连接到服务实例, 获取异步结果
			Class<?> returnType = method.getReturnType();
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
							if (returnType.equals(Void.class)) {
								future.complete(null);
							} else {
								T result = objectMapper.readValue(resp.getData().toByteArray(), (Class<T>) returnType);
								future.complete(result);
							}
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
		} catch (Exception e) {
			future.completeExceptionally(e);
		}

		return future;
	}


	@Override
	public void close() {
		registry.close();
	}
}
