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
package org.springframework.dsl.symboltable;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ClassSymbolTests {

	@Test
	public void testBasicStuff() {
		ClassSymbol sym1 = new ClassSymbol("sym1");
		assertThat(sym1.getName()).isEqualTo("sym1");
		assertThat(sym1.getNestedScopedSymbols()).hasSize(0);

		ClassSymbol sym2 = new ClassSymbol("sym2");
		sym1.define(sym2);
		assertThat(sym1.getNestedScopedSymbols()).hasSize(1);
		assertThat(sym1.getSymbols()).hasSize(1);
		assertThat(sym1.getMembers()).hasSize(1);
	}

	@Test
	public void testResolveClassFieldClassReference() {
		PredefinedScope scope = new PredefinedScope();

		ClassSymbol classA = new ClassSymbol("classA");
		scope.define(classA);

		ClassSymbol classB = new ClassSymbol("classB");
		scope.define(classB);

		FieldSymbol field1 = new FieldSymbol("classA");
		classB.define(field1);

		assertThat(classB.resolveField("classA")).isEqualTo(classA);
	}
}
