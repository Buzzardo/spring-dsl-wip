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
package org.springframework.dsl.lsp.client;

import org.springframework.dsl.jsonrpc.JsonRpcResponse;
import org.springframework.dsl.jsonrpc.codec.JsonRpcExtractor;
import org.springframework.dsl.jsonrpc.codec.JsonRpcExtractorStrategies;

import reactor.core.publisher.Mono;

public interface LspClientResponse {

	JsonRpcExtractorStrategies strategies();

	<T> Mono<T> resultToMono(Class<? extends T> elementClass);

	<T> T result(JsonRpcExtractor<T, JsonRpcResponse> extractor);

	JsonRpcResponse response();

	static Builder create() {
		return new DefaultLspClientResponseBuilder(JsonRpcExtractorStrategies.withDefaults());
	}

	static Builder create(JsonRpcExtractorStrategies strategies) {
		return new DefaultLspClientResponseBuilder(strategies);
	}

	interface Builder {

		Builder response(JsonRpcResponse response);
		LspClientResponse build();
	}
}
