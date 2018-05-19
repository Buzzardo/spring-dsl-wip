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
package org.springframework.dsl.lsp.server.result.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dsl.lsp.LspMethod;
import org.springframework.dsl.lsp.server.ServerLspExchange;
import org.springframework.lang.Nullable;

// TODO: Auto-generated Javadoc
/**
 * The Class LspRequestMethodsRequestCondition.
 */
public class LspRequestMethodsRequestCondition extends AbstractRequestCondition<LspRequestMethodsRequestCondition> {

	/** The methods. */
	private final Set<LspMethod> methods;

	/**
	 * Create a new instance with the given request methods.
	 * @param requestMethods 0 or more HTTP request methods;
	 * if, 0 the condition will match to every request
	 */
	public LspRequestMethodsRequestCondition(LspMethod... requestMethods) {
		this(asList(requestMethods));
	}

	/**
	 * Instantiates a new lsp request methods request condition.
	 *
	 * @param requestMethods the request methods
	 */
	private LspRequestMethodsRequestCondition(Collection<LspMethod> requestMethods) {
		this.methods = Collections.unmodifiableSet(new LinkedHashSet<>(requestMethods));
	}

	/**
	 * As list.
	 *
	 * @param requestMethods the request methods
	 * @return the list
	 */
	private static List<LspMethod> asList(LspMethod... requestMethods) {
		return (requestMethods != null ? Arrays.asList(requestMethods) : Collections.emptyList());
	}

	/**
	 * Returns all {@link LspMethod}s contained in this condition.
	 *
	 * @return the methods
	 */
	public Set<LspMethod> getMethods() {
		return this.methods;
	}

	/* (non-Javadoc)
	 * @see org.springframework.dsl.lsp.server.result.condition.AbstractRequestCondition#getContent()
	 */
	@Override
	protected Collection<LspMethod> getContent() {
		return this.methods;
	}

	/* (non-Javadoc)
	 * @see org.springframework.dsl.lsp.server.result.condition.AbstractRequestCondition#getToStringInfix()
	 */
	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * Returns a new instance with a union of the HTTP request methods
	 * from "this" and the "other" instance.
	 *
	 * @param other the other
	 * @return the lsp request methods request condition
	 */
	@Override
	public LspRequestMethodsRequestCondition combine(LspRequestMethodsRequestCondition other) {
		Set<LspMethod> set = new LinkedHashSet<>(this.methods);
		set.addAll(other.methods);
		return new LspRequestMethodsRequestCondition(set);
	}

	/**
	 * Check if any of the HTTP request methods match the given request and
	 * return an instance that contains the matching HTTP request method only.
	 * @param exchange the current exchange
	 * @return the same instance if the condition is empty (unless the request
	 * method is HTTP OPTIONS), a new condition with the matched request method,
	 * or {@code null} if there is no match or the condition is empty and the
	 * request method is OPTIONS.
	 */
	@Override
	@Nullable
	public LspRequestMethodsRequestCondition getMatchingCondition(ServerLspExchange exchange) {
		if (getMethods().isEmpty()) {
			return this;
		}
		return matchRequestMethod(exchange.getRequest().getMethod());
	}

	/**
	 * Match request method.
	 *
	 * @param coapMethod the coap method
	 * @return the lsp request methods request condition
	 */
	@Nullable
	private LspRequestMethodsRequestCondition matchRequestMethod(@Nullable LspMethod coapMethod) {
		if (coapMethod != null) {
			for (LspMethod method : getMethods()) {
				if (coapMethod.matches(method.name())) {
					return new LspRequestMethodsRequestCondition(method);
				}
			}
		}
		return null;
	}

	/**
	 * Returns:
	 * <ul>
	 * <li>0 if the two conditions contain the same number of HTTP request methods
	 * <li>Less than 0 if "this" instance has an COAP request method but "other" doesn't
	 * <li>Greater than 0 "other" has an COAP request method but "this" doesn't
	 * </ul>
	 * <p>It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(ServerLspExchange)} and therefore each instance
	 * contains the matching HTTP request method only or is otherwise empty.
	 *
	 * @param other the other
	 * @param exchange the exchange
	 * @return the int
	 */
	@Override
	public int compareTo(LspRequestMethodsRequestCondition other, ServerLspExchange exchange) {
		return (other.methods.size() - this.methods.size());
	}

}