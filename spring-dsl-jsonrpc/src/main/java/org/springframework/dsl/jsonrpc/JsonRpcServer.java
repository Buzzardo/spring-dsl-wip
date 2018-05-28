package org.springframework.dsl.jsonrpc;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.function.BiFunction;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.LineBasedFrameDecoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.WorkQueueProcessor;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.NettyInbound;
import reactor.ipc.netty.NettyOutbound;
import reactor.ipc.netty.NettyPipeline;
import reactor.ipc.netty.tcp.TcpServer;

public class JsonRpcServer {

	private final Subscriber<JsonRpcRequest> requests;
	private final Publisher<JsonRpcResponse> responses;
	private WorkQueueProcessor<ByteBuf> workProcessor;

	public JsonRpcServer(Subscriber<JsonRpcRequest> requests, Publisher<JsonRpcResponse> responses) {
		this.requests = requests;
		this.responses = responses;
	}

	public void start() {
		workProcessor = WorkQueueProcessor.create("jsonrpc-worker", 8192);
		createProtocolListener();
	}

	public NettyContext createProtocolListener() {
//		Flux<ByteBuf> stream = Flux.from(responses).map(r -> {
//			return Unpooled.copiedBuffer(r.getId().getBytes());
//		}).subscribeWith(workProcessor);

		TcpServer server = TcpServer.create(9999);



		NettyContext connectedServer = server.newHandler((in, out) -> {
			in.context().addHandlerLast(new JsonRpcDecoder());

			in.receive()
				.map(bb -> {
					JsonRpcRequest request = new JsonRpcRequest();
					String content = bb.retain().duplicate().toString(Charset.defaultCharset());
//					request.setId(content);
					return request;
				})
				.subscribe(request -> {
					requests.onNext(request);
				});

			return out.options(NettyPipeline.SendOptions::flushOnEach)
//					.send(stream)
					.neverComplete();
		}).block();
		return connectedServer;
	}
}
