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
package org.springframework.dsl.lsp.server.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dsl.lsp.server.jsonrpc.NettyTcpServer;
import org.springframework.dsl.lsp.server.jsonrpc.ReactorJsonRpcHandlerAdapter;
import org.springframework.dsl.lsp.server.support.StdioSocketBridge;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.tcp.TcpClient;
import reactor.ipc.netty.tcp.TcpServer;

/**
 *
 *
 * @author Janne Valkealahti
 *
 */
@Configuration
@Import(GenericLspConfiguration.class)
public class LspServerStdioConfiguration {

	@Bean(initMethod = "start", destroyMethod = "stop")
	public NettyTcpServer nettyTcpServer(ReactorJsonRpcHandlerAdapter handlerAdapter) {
		TcpServer tcpServer = TcpServer.create();
		NettyTcpServer nettyTcpServer = new NettyTcpServer(tcpServer, handlerAdapter, null);
		return nettyTcpServer;
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public StdioSocketBridge stdioSocketBridge(NettyTcpServer nettyTcpServer) {
		return new StdioSocketBridge(nettyTcpServer);
	}

}