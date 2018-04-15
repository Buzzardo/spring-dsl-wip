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
package org.springframework.dsl.lsp4j.result.method.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.dsl.lsp.LspMethod;
import org.springframework.dsl.lsp.domain.DidChangeTextDocumentParams;
import org.springframework.dsl.lsp.domain.DidCloseTextDocumentParams;
import org.springframework.dsl.lsp.domain.DidOpenTextDocumentParams;
import org.springframework.dsl.lsp.domain.DidSaveTextDocumentParams;
import org.springframework.dsl.lsp.domain.InitializeParams;
import org.springframework.dsl.lsp.server.ServerLspExchange;
import org.springframework.dsl.lsp.server.support.DefaultServerCoapExchange;
import org.springframework.dsl.lsp.server.support.GenericServerLspRequest;
import org.springframework.dsl.lsp.server.support.GenericServerLspResponse;
import org.springframework.dsl.lsp4j.converter.GenericLsp4jObjectConverter;

/**
 * Tests for {@link Lsp4jDomainArgumentResolver}.
 *
 * @author Janne Valkealahti
 *
 */
public class Lsp4jDomainArgumentResolverTests {

	private Lsp4jDomainArgumentResolver resolver;
	private MethodParameter paramInitializeParams;
	private MethodParameter paramDidChangeTextDocumentParams;
	private MethodParameter paramDidCloseTextDocumentParams;
	private MethodParameter paramDidOpenTextDocumentParams;
	private MethodParameter paramDidSaveTextDocumentParams;

	@Before
	public void setup() throws Exception {
		ConversionServiceFactoryBean conversionServiceFactoryBean = lspConversionService();
		conversionServiceFactoryBean.afterPropertiesSet();
		this.resolver = new Lsp4jDomainArgumentResolver(conversionServiceFactoryBean.getObject());
		Method payloadMethod = Lsp4jDomainArgumentResolverTests.class.getDeclaredMethod("testMethod",
				InitializeParams.class, DidChangeTextDocumentParams.class, DidCloseTextDocumentParams.class,
				DidOpenTextDocumentParams.class, DidSaveTextDocumentParams.class);
		this.paramInitializeParams = new SynthesizingMethodParameter(payloadMethod, 0);
		this.paramDidChangeTextDocumentParams = new SynthesizingMethodParameter(payloadMethod, 1);
		this.paramDidCloseTextDocumentParams = new SynthesizingMethodParameter(payloadMethod, 2);
		this.paramDidOpenTextDocumentParams = new SynthesizingMethodParameter(payloadMethod, 3);
		this.paramDidSaveTextDocumentParams = new SynthesizingMethodParameter(payloadMethod, 4);
	}

	@Test
	public void testSupports() {
		assertThat(resolver.supportsParameter(paramInitializeParams)).isTrue();
		assertThat(resolver.supportsParameter(paramDidChangeTextDocumentParams)).isTrue();
		assertThat(resolver.supportsParameter(paramDidCloseTextDocumentParams)).isTrue();
		assertThat(resolver.supportsParameter(paramDidOpenTextDocumentParams)).isTrue();
		assertThat(resolver.supportsParameter(paramDidSaveTextDocumentParams)).isTrue();
	}

	@Test
	public void testConversions() {
		ServerLspExchange exchange = createExchange(LspMethod.DIDSAVE,
				new org.eclipse.lsp4j.DidSaveTextDocumentParams());
		Object object = this.resolver.resolveArgument(paramDidSaveTextDocumentParams, exchange).block();
		assertThat(object).isInstanceOf(DidSaveTextDocumentParams.class);
	}

	@SuppressWarnings("unused")
	private void testMethod(InitializeParams p1, DidChangeTextDocumentParams p2, DidCloseTextDocumentParams p3,
			DidOpenTextDocumentParams p4, DidSaveTextDocumentParams p5) {
	};

	private ConversionServiceFactoryBean lspConversionService() {
		ConversionServiceFactoryBean factoryBean = new ConversionServiceFactoryBean();
		Set<Object> converters = new HashSet<>();
		converters.add(new GenericLsp4jObjectConverter());
		factoryBean.setConverters(converters);
		return factoryBean;
	}

	private static ServerLspExchange createExchange(LspMethod lspMethod, Object body) {
		GenericServerLspRequest request = new GenericServerLspRequest(body);
		request.setMethod(lspMethod);
		GenericServerLspResponse response = new GenericServerLspResponse();
		return new DefaultServerCoapExchange(request, response);
	}

}