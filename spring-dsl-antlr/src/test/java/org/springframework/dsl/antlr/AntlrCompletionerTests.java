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
package org.springframework.dsl.antlr;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.dsl.document.LanguageId;
import org.springframework.dsl.document.TextDocument;
import org.springframework.dsl.lsp.domain.CompletionItem;
import org.springframework.dsl.lsp.domain.Position;

import reactor.core.publisher.Flux;

/**
 * These tests are pretty much to verify functionality in
 * {@link AbstractAntlrCompletioner} using existing test {@code ANTLR} test
 * grammars.
 *
 * @author Janne Valkealahti
 *
 */
public class AntlrCompletionerTests {

	@Test
	public void testTest2Empty() {
		String input = "";
		TextDocument document = new TextDocument("", LanguageId.PLAINTEXT, 0, input);
		Test2AntlrCompletioner completioner = new Test2AntlrCompletioner();
		Flux<CompletionItem> completions = completioner.complete(document, new Position(0, 0));
		List<CompletionItem> items = completions.toStream().collect(Collectors.toList());
		List<String> labels = items.stream().map(item -> item.getLabel()).collect(Collectors.toList());
		assertThat(labels, containsInAnyOrder("STATEMACHINE", "STATE", "TRANSITION"));
	}

	@Test
	public void testTest2GenericThings() {
	}

	@Test
	public void testTest2DistincIdResolving() {
		String input = "state S1 {} state S2 {} transition { source ";
		TextDocument document = new TextDocument("", LanguageId.PLAINTEXT, 0, input);
		Test2AntlrCompletioner completioner = new Test2AntlrCompletioner();
		Flux<CompletionItem> completions = completioner.complete(document, new Position(0, 0));
		List<CompletionItem> items = completions.toStream().collect(Collectors.toList());
		List<String> labels = items.stream().map(item -> item.getLabel()).collect(Collectors.toList());
//		assertThat(labels, containsInAnyOrder("S1", "S2", "'}'", "SOURCE", "TARGET", "ID", "';'"));
		assertThat(labels, containsInAnyOrder("'}'", "SOURCE", "TARGET", "ID", "';'"));
	}
}