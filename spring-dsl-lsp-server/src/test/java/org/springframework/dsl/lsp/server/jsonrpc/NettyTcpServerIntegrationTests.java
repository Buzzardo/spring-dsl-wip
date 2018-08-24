/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.dsl.lsp.server.jsonrpc;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.dsl.domain.InitializeParams;
import org.springframework.dsl.jsonrpc.JsonRpcResponse;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcController;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcNotification;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcRequestMapping;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcResponseBody;
import org.springframework.dsl.jsonrpc.config.EnableJsonRcp;
import org.springframework.dsl.jsonrpc.session.JsonRpcSession;
import org.springframework.dsl.jsonrpc.support.DispatcherJsonRpcHandler;
import org.springframework.dsl.lsp.client.ClientReactorJsonRpcHandlerAdapter;
import org.springframework.dsl.lsp.client.LspClient;
import org.springframework.dsl.lsp.client.NettyTcpClientLspClient;
import org.springframework.dsl.lsp.server.config.LspDomainJacksonConfiguration;

import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.tcp.TcpClient;
import reactor.ipc.netty.tcp.TcpServer;

public class NettyTcpServerIntegrationTests {

	private static final byte[] CONTENT2 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 2, \"method\": \"hi\"}");
	private static final byte[] CONTENT3 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 3, \"method\": \"bye\"}");
	private static final byte[] CONTENT4 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 3, \"method\": \"shouldnotexist\"}");
	private static final byte[] CONTENT5 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 4, \"method\": \"pojo1\"}");
	private static final byte[] CONTENT6 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 4, \"method\": \"methodparams\", \"params\":\"hi\"}");
	private static final byte[] CONTENT7 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 4, \"method\": \"monovoid\", \"params\":null}");
	private static final byte[] CONTENT8 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 4, \"method\": \"void\", \"params\":null}");
	private static final byte[] CONTENT9 = createContent("{\"jsonrpc\": \"2.0\", \"method\": \"notificationsingleresponse\"}");
	private static final byte[] CONTENT10 = createContent("{\"jsonrpc\": \"2.0\", \"method\": \"notificationmultiresponse\"}");

	private static final String initializeParams = "{" +
			"\"processId\":1," +
			"\"rootUri\":\"rootUri\"," +
			"\"initializationOptions\":\"initializationOptions\"," +
			"\"trace\":\"trace\"," +
			"\"capabilities\":{\"experimental\":\"experimental\"}" +
			"}";

	private static final byte[] CONTENT11 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 4, \"method\": \"initializeparams\", \"params\":" + initializeParams + "}");
	private static final byte[] CONTENT12 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 4, \"method\": \"monoobjectempty\", \"params\":null}");
	private static final byte[] CONTENT13 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 2, \"method\": \"session1\"}");
	private static final byte[] CONTENT14 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 2, \"method\": \"session2\"}");
	private static final byte[] CONTENT15 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 4, \"method\": \"delay\", \"params\":\"1000\"}");
	private static final byte[] CONTENT16 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 4, \"method\": \"$/cancelRequest\", \"params\":{\"id\": 4}}");
	private static final byte[] CONTENT17 = createContent("{\"jsonrpc\": \"2.0\",\"id\": 2, \"method\": \"counter\"}");

	private static byte[] createContent(String... lines) {
		StringBuilder buf = new StringBuilder();
		for (String line : lines) {
			buf.append(line);
			buf.append("\r\n");
		}
		String message = "Content-Length: " + buf.length() + "\r\n\r\n" + buf.toString();
		return message.getBytes();
	}

	private AnnotationConfigApplicationContext context;
	private AnnotationConfigApplicationContext clientContext;

	@Before
	public void init() {
		context = null;
		clientContext = null;
	}

	@After
	public void clean() {
		if (context != null) {
			context.close();
		}
		context = null;
		if (clientContext != null) {
			clientContext.close();
		}
		clientContext = null;
	}

	@Test
	public void testCancellation() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(1);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in
					.receive()
					.subscribe(c -> {
						responses.add(c.retain().duplicate().toString(Charset.defaultCharset()));
						dataLatch.countDown();
					});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT15), Unpooled.copiedBuffer(CONTENT16)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(2, TimeUnit.SECONDS)).isTrue();

		String response = "Content-Length: 71\r\n\r\n{\"jsonrpc\":\"2.0\", \"id\":4, \"error\":{\"code\":-32800, \"message\": \"cancel\"}}";

		assertThat(responses).containsExactlyInAnyOrder(response);
	}

	@Test
	public void testSmoke() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(10);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in.context().addHandlerLast(new LspJsonRpcDecoder());
					in.receiveObject()
						.ofType(String.class)
						.subscribe(c -> {
							responses.add(c);
							dataLatch.countDown();
						});

					return out
							.send(Flux.range(0, 10).map(r -> {
								return Unpooled.copiedBuffer(CONTENT2);
							}))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(1, TimeUnit.SECONDS)).isTrue();

		String response = "{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":\"hi\"}";
		assertThat(responses).hasSize(10);
		assertThat(responses).allMatch(c -> c.equals(response));
	}

	@Test
	public void testOk1() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(2);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in.context().addHandlerLast(new LspJsonRpcDecoder());
					in.receiveObject()
						.ofType(String.class)
						.subscribe(c -> {
							responses.add(c);
							dataLatch.countDown();
						});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT2), Unpooled.copiedBuffer(CONTENT3)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(1, TimeUnit.SECONDS)).isTrue();
		String response1 = "{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":\"hi\"}";
		String response2 = "{\"jsonrpc\":\"2.0\",\"id\":3,\"result\":\"bye\"}";

		assertThat(responses).containsExactlyInAnyOrder(response1, response2);
	}

	@Test
	public void testOk2() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(1);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in
					.receive()
					.subscribe(c -> {
						responses.add(c.retain().duplicate().toString(Charset.defaultCharset()));
						dataLatch.countDown();
					});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT5)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(1, TimeUnit.SECONDS)).isTrue();

		String response = "Content-Length: 50\r\n\r\n{\"jsonrpc\":\"2.0\",\"id\":4,\"result\":{\"message\":\"hi\"}}";

		assertThat(responses).containsExactlyInAnyOrder(response);
	}

	@Test
	public void testOk4() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(1);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in
					.receive()
					.subscribe(c -> {
						responses.add(c.retain().duplicate().toString(Charset.defaultCharset()));
						dataLatch.countDown();
					});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT7)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(1, TimeUnit.SECONDS)).isFalse();
	}

	@Test
	public void testOk5() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(1);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in
					.receive()
					.subscribe(c -> {
						responses.add(c.retain().duplicate().toString(Charset.defaultCharset()));
						dataLatch.countDown();
					});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT8)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(1, TimeUnit.SECONDS)).isFalse();
	}

	@Test
	public void testOk3() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(1);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in
					.receive()
					.subscribe(c -> {
						responses.add(c.retain().duplicate().toString(Charset.defaultCharset()));
						dataLatch.countDown();
					});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT6)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(1, TimeUnit.SECONDS)).isTrue();

		String response = "Content-Length: 38\r\n\r\n{\"jsonrpc\":\"2.0\",\"id\":4,\"result\":\"hi\"}";

		assertThat(responses).containsExactlyInAnyOrder(response);
	}

	@Test
	public void testOk6() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(1);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in
					.receive()
					.subscribe(c -> {
						responses.add(c.retain().duplicate().toString(Charset.defaultCharset()));
						dataLatch.countDown();
					});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT11)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(1, TimeUnit.SECONDS)).isTrue();

		String response = "Content-Length: 180\r\n\r\n{\"jsonrpc\":\"2.0\",\"id\":4,\"result\":{\"processId\":1,\"rootUri\":\"rootUri\",\"initializationOptions\":\"initializationOptions\",\"capabilities\":{\"experimental\":\"experimental\"},\"trace\":\"trace\"}}";

		assertThat(responses).containsExactlyInAnyOrder(response);
	}

	@Test
	public void testSingleNotification() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(1);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in
					.receive()
					.subscribe(c -> {
						responses.add(c.retain().duplicate().toString(Charset.defaultCharset()));
						dataLatch.countDown();
					});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT9)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(1, TimeUnit.SECONDS)).isTrue();

		String response = "Content-Length: 57\r\n\r\n{\"jsonrpc\":\"2.0\",\"method\":\"singleresponse\",\"params\":\"hi\"}";

		assertThat(responses).containsExactlyInAnyOrder(response);
	}

	@Test
	public void testMultiNotification() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(2);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in.context().addHandlerLast(new LspJsonRpcDecoder());
					in.receiveObject()
						.ofType(String.class)
						.subscribe(c -> {
							responses.add(c);
							dataLatch.countDown();
						});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT10)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(1, TimeUnit.SECONDS)).isTrue();

		String response1 = "{\"jsonrpc\":\"2.0\",\"method\":\"notificationmultiresponse\",\"params\":\"hi\"}";
		String response2 = "{\"jsonrpc\":\"2.0\",\"method\":\"notificationmultiresponse\",\"params\":\"bye\"}";

		assertThat(responses).containsExactlyInAnyOrder(response1, response2);
	}

	@Test
	public void testMethodNotMatchedError() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(1);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in
					.receive()
					.log()
					.subscribe(c -> {
						responses.add(c.retain().duplicate().toString(Charset.defaultCharset()));
						dataLatch.countDown();
					});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT4)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		String response = "Content-Length: 86\r\n\r\n{\"jsonrpc\":\"2.0\", \"id\":3, \"error\":{\"code\":-32603, \"message\": \"internal server error\"}}";

		assertThat(dataLatch.await(2, TimeUnit.SECONDS)).isTrue();

		assertThat(responses).contains(response);
	}


	@Test
	public void testEmptyWithObjectReturnsNull() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(1);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in
					.receive()
					.subscribe(c -> {
						responses.add(c.retain().duplicate().toString(Charset.defaultCharset()));
						dataLatch.countDown();
					});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT12)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(1, TimeUnit.SECONDS)).isTrue();

		String response = "Content-Length: 38\r\n\r\n{\"jsonrpc\":\"2.0\",\"id\":4,\"result\":null}";

		assertThat(responses).containsExactlyInAnyOrder(response);
	}

	@Test
	public void testSession() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(2);
		final List<String> responses = new ArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in
					.receive()
					.subscribe(c -> {
						responses.add(c.retain().duplicate().toString(Charset.defaultCharset()));
						dataLatch.countDown();
					});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT13), Unpooled.copiedBuffer(CONTENT14)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(1, TimeUnit.SECONDS)).isTrue();

		assertThat(responses).hasSize(2);
		assertThat(responses.get(0)).doesNotContain("error");
		assertThat(responses.get(1)).doesNotContain("error");
		assertThat(responses.get(0)).isEqualTo(responses.get(1));
	}

	@Test
	public void testClient() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();

		clientContext = new AnnotationConfigApplicationContext();
		clientContext.register(JsonRpcConfig.class, JsonRpcClientConfig.class, TestClientJsonRpcController.class);
		clientContext.refresh();

		ClientReactorJsonRpcHandlerAdapter xxx = clientContext.getBean(ClientReactorJsonRpcHandlerAdapter.class);

		NettyTcpServer server = context.getBean(NettyTcpServer.class);
		CountDownLatch dataLatch = new CountDownLatch(1);
		final List<JsonRpcResponse> responses = new ArrayList<>();

		LspClient lspClient = LspClient.builder().host("0.0.0.0").port(server.getPort()).build();
		((NettyTcpClientLspClient)lspClient).adapter = xxx;
		((NettyTcpClientLspClient)lspClient).init();

		Mono<JsonRpcResponse> lspClientResponseMono = lspClient.request().id(1).method("methodparams").params("hi").exchange();
		lspClientResponseMono.doOnNext(r -> {
			responses.add(r);
			dataLatch.countDown();
		}).subscribe();

		assertThat(dataLatch.await(2, TimeUnit.SECONDS)).isTrue();
		assertThat(responses.get(0).getId()).isEqualTo(1);
		assertThat(responses.get(0).getResult()).isEqualTo("hi");
	}

	@Test
	public void testClientServerToClientRequest() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();

		clientContext = new AnnotationConfigApplicationContext();
		clientContext.register(JsonRpcConfig.class, JsonRpcClientConfig.class, TestClientJsonRpcController.class);
		clientContext.refresh();

		ClientReactorJsonRpcHandlerAdapter xxx = clientContext.getBean(ClientReactorJsonRpcHandlerAdapter.class);

		NettyTcpServer server = context.getBean(NettyTcpServer.class);
		CountDownLatch dataLatch = new CountDownLatch(1);
		final List<JsonRpcResponse> responses = new ArrayList<>();

		LspClient lspClient = LspClient.builder().host("0.0.0.0").port(server.getPort()).build();
		((NettyTcpClientLspClient)lspClient).adapter = xxx;
		((NettyTcpClientLspClient)lspClient).init();
		Mono<JsonRpcResponse> lspClientResponseMono = lspClient.request().id(1).method("serverhi").exchange();
		lspClientResponseMono.doOnNext(r -> {
			responses.add(r);
			dataLatch.countDown();
		}).subscribe();

		assertThat(dataLatch.await(2, TimeUnit.SECONDS)).isTrue();
		assertThat(responses.get(0).getId()).isEqualTo(1);
		assertThat(responses.get(0).getError()).isNull();
		assertThat(responses.get(0).getResult()).isEqualTo("clienthi");
	}

	@Test
	public void testMultipleClients() throws InterruptedException {
		context = new AnnotationConfigApplicationContext();
		context.register(JsonRpcConfig.class, JsonRpcServerConfig.class, TestServerJsonRpcController.class);
		context.refresh();
		NettyTcpServer server = context.getBean(NettyTcpServer.class);

		CountDownLatch dataLatch = new CountDownLatch(2);
		final List<String> responses = new CopyOnWriteArrayList<>();

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in
					.receive()
					.subscribe(c -> {
						responses.add(c.toString(Charset.defaultCharset()));
						dataLatch.countDown();
					});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT17)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		TcpClient.create(server.getPort())
				.newHandler((in, out) -> {
					in
					.receive()
					.subscribe(c -> {
						responses.add(c.toString(Charset.defaultCharset()));
						dataLatch.countDown();
					});

					return out
							.send(Flux.just(Unpooled.copiedBuffer(CONTENT17)))
							.neverComplete();
				})
				.block(Duration.ofSeconds(30));

		assertThat(dataLatch.await(1, TimeUnit.SECONDS)).isTrue();

		String response1 = "Content-Length: 37\r\n\r\n{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":\"0\"}";
		String response2 = "Content-Length: 37\r\n\r\n{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":\"1\"}";

		assertThat(responses).containsExactlyInAnyOrder(response1, response2);
	}

	@EnableJsonRcp
	@Import(LspDomainJacksonConfiguration.class)
	static class JsonRpcConfig {

		@Bean
		public LspDomainArgumentResolver lspDomainArgumentResolver() {
			return new LspDomainArgumentResolver();
		}

		@Bean
		public LspClientArgumentResolver lspClientArgumentResolver() {
			return new LspClientArgumentResolver();
		}

		@Bean
		public ReactiveAdapterRegistry jsonRpcAdapterRegistry() {
			return new ReactiveAdapterRegistry();
		}

		@Bean
		public RpcJsonRpcHandlerAdapter rpcJsonRpcHandlerAdapter(DispatcherJsonRpcHandler dispatcherJsonRpcHandler) {
			return new RpcJsonRpcHandlerAdapter(dispatcherJsonRpcHandler);
		}

		@Bean
		public ReactorJsonRpcHandlerAdapter reactorJsonRpcHandlerAdapter(RpcJsonRpcHandlerAdapter rpcJsonRpcHandlerAdapter) {
			return new ReactorJsonRpcHandlerAdapter(rpcJsonRpcHandlerAdapter);
		}
	}

	static class JsonRpcServerConfig {

		@Bean(initMethod = "start")
		public NettyTcpServer nettyTcpServer(ReactorJsonRpcHandlerAdapter handlerAdapter) {
			TcpServer tcpServer = TcpServer.create();
			NettyTcpServer nettyTcpServer = new NettyTcpServer(tcpServer, handlerAdapter, null);
			return nettyTcpServer;
		}
	}

	static class JsonRpcClientConfig {

		@Bean
		public ClientReactorJsonRpcHandlerAdapter clientReactorJsonRpcHandlerAdapter(RpcHandler rpcHandler) {
			return new ClientReactorJsonRpcHandlerAdapter(rpcHandler);
		}
	}

	@JsonRpcController
	private static class TestClientJsonRpcController {

		@JsonRpcRequestMapping(method = "clienthi")
		@JsonRpcResponseBody
		public String clienthi() {
			return "clienthi";
		}
	}

	@JsonRpcController
	private static class TestServerJsonRpcController {

		private AtomicInteger counter = new AtomicInteger();

		@JsonRpcRequestMapping(method = "serverhi")
		@JsonRpcResponseBody
		public Mono<String> serverhi(LspClient lspClient) {
			return lspClient
					.request().id(10).method("clienthi").exchange()
					.map(r -> r.getResult());
		}

		@JsonRpcRequestMapping(method = "counter")
		@JsonRpcResponseBody
		public String counter() {
			return Integer.toString(counter.getAndIncrement());
		}

		@JsonRpcRequestMapping(method = "hi")
		@JsonRpcResponseBody
		public String hi() {
			return "hi";
		}

		@JsonRpcRequestMapping(method = "bye")
		@JsonRpcResponseBody
		public String bye() {
			return "bye";
		}

		@JsonRpcRequestMapping(method = "pojo1")
		@JsonRpcResponseBody
		public Pojo1 pojo1() {
			return new Pojo1();
		}

		@JsonRpcRequestMapping(method = "methodparams")
		@JsonRpcResponseBody
		public String methodparams(String params) {
			return params;
		}

		@JsonRpcRequestMapping(method = "monovoid")
		@JsonRpcResponseBody
		public Mono<Void> monovoid() {
			return Mono.empty();
		}

		@JsonRpcRequestMapping(method = "monoobjectempty")
		@JsonRpcResponseBody
		public Mono<Object> monoobjectempty() {
			return Mono.empty();
		}

		@JsonRpcRequestMapping(method = "void")
		@JsonRpcResponseBody
		public void justvoid() {
		}

		@JsonRpcRequestMapping(method = "notificationsingleresponse")
		@JsonRpcNotification("singleresponse")
		public String notificationsingleresponse() {
			return "hi";
		}

		@JsonRpcRequestMapping(method = "notificationmultiresponse")
		@JsonRpcNotification
		public Flux<String> notificationmultiresponse() {
			return Flux.just("hi", "bye");
		}

		@JsonRpcRequestMapping(method = "initializeparams")
		@JsonRpcResponseBody
		public InitializeParams initializeparams(InitializeParams params) {
			return params;
		}

		@JsonRpcRequestMapping(method = "session1")
		@JsonRpcResponseBody
		public Mono<String> session1(JsonRpcSession session) {
			session.getAttributes().put("foo", "bar");
			return Mono.just(session.getId() + session.getAttributes().get("foo"));
		}

		@JsonRpcRequestMapping(method = "session2")
		@JsonRpcResponseBody
		public Mono<String> session2(JsonRpcSession session) {
			return Mono.just(session.getId() + session.getAttributes().get("foo"));
		}

		@JsonRpcRequestMapping(method = "delay")
		@JsonRpcResponseBody
		public Mono<String> delay(String params) {
			long delay = Long.parseLong(params);
			return Mono
					.delay(Duration.ofMillis(delay))
					.thenReturn("delay");
		}
	}

	private static class Pojo1 {
		private String message = "hi";

		@SuppressWarnings("unused")
		public String getMessage() {
			return message;
		}
	}
}
