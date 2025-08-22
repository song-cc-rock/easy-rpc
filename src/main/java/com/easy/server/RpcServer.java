package com.easy.server;

import com.google.protobuf.ByteString;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.buffer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.Rpc;

import java.lang.reflect.Method;
import java.util.Map;

public class RpcServer {

	private final Vertx vertx = Vertx.vertx();
	private final Map<String, Object> serviceMap;

	private static final Logger log = LoggerFactory.getLogger(RpcServer.class);

	public RpcServer(Map<String, Object> serviceMap) {
		this.serviceMap = serviceMap;
	}

	/**
	 * 启动RPC服务器
	 * @param host 主机
	 * @param port 端口
	 * @return NetServer 实例
	 */
	public NetServer start(String host, int port) {
		NetServer server = vertx.createNetServer()
				.connectHandler(socket -> socket.handler(buf -> handle(buf, socket)));
		server.listen(port, host);
		log.info("RPC Server started on port {} (tcp)", port);
		return server;
	}

	/**
	 * 停止RPC服务器
	 * @param server NetServer 实例
	 */
	public void stop(NetServer server) {
		if (server != null) {
			server.close();
		}
	}

	/**
	 * 调用处理方法
	 * @param buf 接收的数据
	 * @param socket socket连接
	 */
	private void handle(Buffer buf, NetSocket socket) {
		try {
			Rpc.RpcRequest request = Rpc.RpcRequest.parseFrom(buf.getBytes());
			String interfaceName = request.getInterfaceName();
			Object obj = serviceMap.get(interfaceName);
			Rpc.RpcResponse.Builder response = Rpc.RpcResponse.newBuilder();
			if (obj == null) {
				// service not found
				response.setCode(404).setMsg("Service not found: " + interfaceName);
			} else {
				Method method = getInvokeMethod(obj, request.getMethodName(), request.getParamsCount());
				if (method == null) {
					// method not found
					response.setCode(404).setMsg("Method not found: " + request.getMethodName());
				} else {
					Object[] args = new Object[request.getParamsCount()];
					for (int i = 0; i < request.getParamsList().size(); i++) {
						args[i] = new String(request.getParams(i).toByteArray());
					}
					Object result = method.invoke(obj, args);
					byte[] out = (result == null) ? new byte[0] : result.toString().getBytes();
					response.setCode(200).setData(ByteString.copyFrom(out));
				}
			}
			socket.write(Buffer.buffer(response.build().toByteArray()));
		} catch (Exception e) {
			Rpc.RpcResponse errResp = Rpc.RpcResponse.newBuilder().setCode(500).setMsg(e.getMessage()).build();
			socket.write(Buffer.buffer(errResp.toByteArray()));
		}
	}

	/**
	 * 获取调用方法
	 * @param obj 对象
	 * @param methodName 方法名
	 * @param paramCount 参数数量
	 * @return 方法
	 */
	private Method getInvokeMethod(Object obj, String methodName, int paramCount) {
		for (Method method : obj.getClass().getMethods()) {
			if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
				return method;
			}
		}
		return null;
	}
}
