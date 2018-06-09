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
package org.springframework.dsl.lsp.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dsl.document.Document;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcController;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcRequestMapping;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcResponseBody;
import org.springframework.dsl.lsp.LspClientContext;
import org.springframework.dsl.lsp.domain.CompletionItem;
import org.springframework.dsl.lsp.domain.CompletionParams;
import org.springframework.dsl.lsp.domain.DidChangeTextDocumentParams;
import org.springframework.dsl.lsp.domain.DidCloseTextDocumentParams;
import org.springframework.dsl.lsp.domain.DidOpenTextDocumentParams;
import org.springframework.dsl.lsp.domain.DidSaveTextDocumentParams;
import org.springframework.dsl.lsp.domain.Hover;
import org.springframework.dsl.lsp.domain.PublishDiagnosticsParams;
import org.springframework.dsl.lsp.domain.TextDocumentPositionParams;
import org.springframework.dsl.lsp.domain.TextEdit;
import org.springframework.dsl.lsp.domain.WillSaveTextDocumentParams;
import org.springframework.dsl.lsp.service.Completioner;
import org.springframework.dsl.lsp.service.DocumentStateTracker;
import org.springframework.dsl.lsp.service.Hoverer;
import org.springframework.dsl.lsp.service.Reconciler;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A {@code JsonRpcController} implementation {@code textDocument} features
 * what a {@code Language Server} should provide.
 *
 * @author Janne Valkealahti
 *
 */
@JsonRpcController
@JsonRpcRequestMapping(method = "textDocument/")
public class TextDocumentLanguageServerController {

	private static final Logger log = LoggerFactory.getLogger(TextDocumentLanguageServerController.class);
	private DocumentStateTracker documentStateTracker;
	private Reconciler reconciler;
	private Completioner completioner;
	private Hoverer hoverer;

	/**
	 * Method handling {@code LSP client didOpen} request and dispatching
	 * information about opened {@code document} and its metadata to document
	 * tracker. Behaviour of further {@code LSP client} requests for
	 * {@code didChanged}, {@code didSave} and {@code didClose} will be based on
	 * information stored and dispatched in this method.
	 *
	 * @param params the {@link DidOpenTextDocumentParams}
	 * @param context the lsp client context
	 */
	@JsonRpcRequestMapping(method = "didOpen")
	public void clientDocumentOpened(DidOpenTextDocumentParams params, LspClientContext context) {
		handle(this.documentStateTracker.didOpen(params), reconciler, context, params.getTextDocument().getUri());
	}

	/**
	 * Method handling {@code LSP client didChange} request and dispatching into
	 * {@link Reconciler} if available.
	 *
	 * @param params the {@link DidChangeTextDocumentParams}
	 * @param context the lsp client context
	 */
	@JsonRpcRequestMapping(method = "didChange")
	public void clientDocumentChanged(DidChangeTextDocumentParams params, LspClientContext context) {
		handle(this.documentStateTracker.didChange(params), reconciler, context, params.getTextDocument().getUri());
	}

	/**
	 * Method handling {@code LSP client didClose} request and dispatching into
	 * {@link DocumentStateTracker}.
	 *
	 * @param params the {@link DidCloseTextDocumentParams}
	 * @param context the lsp client context
	 */
	@JsonRpcRequestMapping(method = "didClose")
	public void clientDocumentClosed(DidCloseTextDocumentParams params, LspClientContext context) {
		handle(this.documentStateTracker.didClose(params), reconciler, context, params.getTextDocument().getUri());
	}

	/**
	 * Method handling {@code LSP client didSave} request and dispatching into
	 * {@link DocumentStateTracker}.
	 *
	 * @param params the {@link DidSaveTextDocumentParams}
	 * @param context the lsp client context
	 */
	@JsonRpcRequestMapping(method = "didSave")
	public void clientDocumentSaved(DidSaveTextDocumentParams params, LspClientContext context) {
		handle(this.documentStateTracker.didSave(params), reconciler, context, params.getTextDocument().getUri());
	}

	/**
	 * Method handling {@code LSP client willSave} request and dispatching into
	 * {@link DocumentStateTracker}.
	 *
	 * @param params the {@link WillSaveTextDocumentParams}
	 * @param context the lsp client context
	 */
	@JsonRpcRequestMapping(method = "willSave")
	public void clientDocumentWillSave(WillSaveTextDocumentParams params, LspClientContext context) {
		handle(this.documentStateTracker.willSave(params), reconciler, context, params.getTextDocument().getUri());
	}

	/**
	 * Method handling {@code LSP client willSaveWaitUntil} request and dispatching into
	 * {@link DocumentStateTracker}.
	 *
	 * @param params the {@link WillSaveTextDocumentParams}
	 * @param context the lsp client context
	 * @return a flux of textedit's
	 */
	@JsonRpcRequestMapping(method = "willSaveWaitUntil")
	@JsonRpcResponseBody
	public Flux<TextEdit> clientDocumentWillSaveWaitUntil(WillSaveTextDocumentParams params, LspClientContext context) {
		return Flux.empty();
	}

	/**
	 * Method handling {@code LSP client hover} request and dispatching into
	 * {@link Hoverer}.
	 *
	 * @param params the {@link TextDocumentPositionParams}
	 * @return a mono of hover
	 */
	@JsonRpcRequestMapping(method = "hover")
	@JsonRpcResponseBody
	public Mono<Hover> hover(TextDocumentPositionParams params) {
		if (hoverer != null) {
			return hoverer.hover(documentStateTracker.getDocument(params.getTextDocument().getUri()),
					params.getPosition());
		}
		return Mono.empty();
	}

	/**
	 * Method handling {@code LSP client completion} request and dispatching into
	 * {@link Completioner}.
	 *
	 * @param params the {@link TextDocumentPositionParams}
	 * @return a flux of completion items
	 */
	@JsonRpcRequestMapping(method = "completion")
	@JsonRpcResponseBody
	public Flux<CompletionItem> completion(CompletionParams params) {
		if (completioner != null && params != null) {
			return completioner.complete(documentStateTracker.getDocument(params.getTextDocument().getUri()),
					params.getPosition());
		}
		return Flux.empty();
	}

	private static void handle(Mono<Document> document, Reconciler reconciler, LspClientContext context,
			String uri) {
		log.trace("Handling document {}, reconciler {}, context {}, uri {}", document, reconciler, context, uri);
		document.doOnNext(doc -> {
			reconciler.reconcile(doc)
				.switchIfEmpty(Mono.just(new PublishDiagnosticsParams(uri)))
				.doOnNext(diag -> {
					context.getClient().send(diag);
				})
				.subscribe();
		})
		.subscribe();
	}

}