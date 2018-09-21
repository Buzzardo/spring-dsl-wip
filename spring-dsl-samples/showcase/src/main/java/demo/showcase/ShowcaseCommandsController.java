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
package demo.showcase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dsl.domain.LogMessageParams;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcController;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcNotification;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcRequestMapping;
import org.springframework.dsl.lsp.client.LspClient;

import reactor.core.publisher.Mono;

@JsonRpcController
@JsonRpcRequestMapping(method = "showcase/")
public class ShowcaseCommandsController {

	private static final Logger log = LoggerFactory.getLogger(ShowcaseCommandsController.class);

	@JsonRpcRequestMapping(method = "ping")
	@JsonRpcNotification
	public void ping() {
		log.info("ping");
	}

	@JsonRpcRequestMapping(method = "log")
	@JsonRpcNotification
	public Mono<Void> sendLogNotification(LspClient lspClient) {
		return lspClient.notification()
			.method("window/logMessage")
			.params(LogMessageParams.from("hi"))
			.exchange()
			.then();
	}
}
