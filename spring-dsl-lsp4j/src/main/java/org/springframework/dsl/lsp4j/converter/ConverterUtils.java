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
package org.springframework.dsl.lsp4j.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dsl.lsp.domain.CompletionItem;
import org.springframework.dsl.lsp.domain.CompletionOptions;
import org.springframework.dsl.lsp.domain.Diagnostic;
import org.springframework.dsl.lsp.domain.DiagnosticSeverity;
import org.springframework.dsl.lsp.domain.DidChangeTextDocumentParams;
import org.springframework.dsl.lsp.domain.DidCloseTextDocumentParams;
import org.springframework.dsl.lsp.domain.DidOpenTextDocumentParams;
import org.springframework.dsl.lsp.domain.DidSaveTextDocumentParams;
import org.springframework.dsl.lsp.domain.Hover;
import org.springframework.dsl.lsp.domain.InitializeParams;
import org.springframework.dsl.lsp.domain.InitializeResult;
import org.springframework.dsl.lsp.domain.Position;
import org.springframework.dsl.lsp.domain.PublishDiagnosticsParams;
import org.springframework.dsl.lsp.domain.Range;
import org.springframework.dsl.lsp.domain.ServerCapabilities;
import org.springframework.dsl.lsp.domain.TextDocumentIdentifier;
import org.springframework.dsl.lsp.domain.TextDocumentItem;
import org.springframework.dsl.lsp.domain.TextDocumentPositionParams;
import org.springframework.dsl.lsp.domain.TextDocumentSyncKind;
import org.springframework.dsl.lsp.domain.TextDocumentSyncOptions;

/**
 * Utilities to convert {@code POJO}s between {@code LSP4J} and
 * {@code Spring DSL} {@code LSP} domain objects.
 *
 * @author Janne Valkealahti
 *
 */
public final class ConverterUtils {

	public static InitializeParams toInitializeParams(org.eclipse.lsp4j.InitializeParams from) {
		InitializeParams to = new InitializeParams();
		return to;
	}

	public static org.eclipse.lsp4j.InitializeParams toInitializeParams(InitializeParams from) {
		org.eclipse.lsp4j.InitializeParams to = new org.eclipse.lsp4j.InitializeParams();
		return to;
	}

	public static InitializeResult toInitializeResult(org.eclipse.lsp4j.InitializeResult from) {
		InitializeResult to = new InitializeResult();
		if (from.getCapabilities() != null) {
			to.setCapabilities(toServerCapabilities(from.getCapabilities()));
		}
		return to;
	}

	public static org.eclipse.lsp4j.InitializeResult toInitializeResult(InitializeResult from) {
		org.eclipse.lsp4j.InitializeResult to = new org.eclipse.lsp4j.InitializeResult();
		if (from.getCapabilities() != null) {
			to.setCapabilities(toServerCapabilities(from.getCapabilities()));
		}
		return to;
	}

	public static ServerCapabilities toServerCapabilities(org.eclipse.lsp4j.ServerCapabilities from) {
		ServerCapabilities to = new ServerCapabilities();
		to.setHoverProvider(from.getHoverProvider());
		to.setCompletionProvider(toCompletionOptions(from.getCompletionProvider()));
		return to;
	}

	public static org.eclipse.lsp4j.ServerCapabilities toServerCapabilities(ServerCapabilities from) {
		org.eclipse.lsp4j.ServerCapabilities to = new org.eclipse.lsp4j.ServerCapabilities();
		to.setHoverProvider(from.getHoverProvider());
		to.setCompletionProvider(toCompletionOptions(from.getCompletionProvider()));
		if (from.getTextDocumentSyncOptions() != null) {
			to.setTextDocumentSync(toTextDocumentSyncOptions(from.getTextDocumentSyncOptions()));
		} else if (from.getTextDocumentSyncKind() != null) {
			to.setTextDocumentSync(toTextDocumentSyncKind(from.getTextDocumentSyncKind()));
		}
		return to;
	}

	public static TextDocumentSyncOptions toTextDocumentSyncOptions(org.eclipse.lsp4j.TextDocumentSyncOptions from) {
		TextDocumentSyncOptions to = new TextDocumentSyncOptions();
		return to;
	}

	public static org.eclipse.lsp4j.TextDocumentSyncOptions toTextDocumentSyncOptions(TextDocumentSyncOptions from) {
		org.eclipse.lsp4j.TextDocumentSyncOptions to = new org.eclipse.lsp4j.TextDocumentSyncOptions();
		return to;
	}

	public static TextDocumentSyncKind toTextDocumentSyncKind(org.eclipse.lsp4j.TextDocumentSyncKind from) {
		if (from == null) {
			return null;
		}
		return TextDocumentSyncKind.valueOf(from.name());
	}

	public static org.eclipse.lsp4j.TextDocumentSyncKind toTextDocumentSyncKind(TextDocumentSyncKind from) {
		if (from == null) {
			return null;
		}
		return org.eclipse.lsp4j.TextDocumentSyncKind.valueOf(from.name());
	}

	public static CompletionOptions toCompletionOptions(org.eclipse.lsp4j.CompletionOptions from) {
		if (from == null) {
			return null;
		}
		return new CompletionOptions();
	}

	public static org.eclipse.lsp4j.CompletionOptions toCompletionOptions(CompletionOptions from) {
		if (from == null) {
			return null;
		}
		return new org.eclipse.lsp4j.CompletionOptions();
	}

	public static DidChangeTextDocumentParams toDidChangeTextDocumentParams(org.eclipse.lsp4j.DidChangeTextDocumentParams from) {
		DidChangeTextDocumentParams to = new DidChangeTextDocumentParams();
		return to;
	}

	public static org.eclipse.lsp4j.DidChangeTextDocumentParams toDidChangeTextDocumentParams(DidChangeTextDocumentParams from) {
		org.eclipse.lsp4j.DidChangeTextDocumentParams to = new org.eclipse.lsp4j.DidChangeTextDocumentParams();
		return to;
	}

	public static DidCloseTextDocumentParams toDidCloseTextDocumentParams(org.eclipse.lsp4j.DidCloseTextDocumentParams from) {
		DidCloseTextDocumentParams to = new DidCloseTextDocumentParams();
		return to;
	}

	public static org.eclipse.lsp4j.DidCloseTextDocumentParams toDidCloseTextDocumentParams(DidCloseTextDocumentParams from) {
		org.eclipse.lsp4j.DidCloseTextDocumentParams to = new org.eclipse.lsp4j.DidCloseTextDocumentParams();
		return to;
	}

	/**
	 * Convert {@code Spring DSL} {@link DidOpenTextDocumentParams} to {@code LSP4J}
	 * {@link org.eclipse.lsp4j.DidOpenTextDocumentParams}.
	 *
	 * @param from the {@code Spring DSL DidOpenTextDocumentParams}
	 * @return {@code LSP4J DidOpenTextDocumentParams}
	 */
	public static org.eclipse.lsp4j.DidOpenTextDocumentParams toDidOpenTextDocumentParams(
			DidOpenTextDocumentParams from) {
		org.eclipse.lsp4j.DidOpenTextDocumentParams to = new org.eclipse.lsp4j.DidOpenTextDocumentParams();
		if (from != null) {
			to.setTextDocument(toTextDocumentItem(from.getTextDocument()));
		}
		return to;
	}

	/**
	 * Convert {@code LSP4J} {@link org.eclipse.lsp4j.DidOpenTextDocumentParams} to {@code Spring DSL}
	 * {@link DidOpenTextDocumentParams}.
	 *
	 * @param from the {@code LSP4J DidOpenTextDocumentParams}
	 * @return {@code Spring DSL DidOpenTextDocumentParams}
	 */
	public static DidOpenTextDocumentParams toDidOpenTextDocumentParams(
			org.eclipse.lsp4j.DidOpenTextDocumentParams from) {
		DidOpenTextDocumentParams to = new DidOpenTextDocumentParams();
		if (from != null) {
			to.setTextDocument(toTextDocumentItem(from.getTextDocument()));
		}
		return to;
	}

	/**
	 * Convert {@code Spring DSL} {@link DidSaveTextDocumentParams} to {@code LSP4J}
	 * {@link org.eclipse.lsp4j.DidSaveTextDocumentParams}.
	 *
	 * @param from the {@code Spring DSL DidSaveTextDocumentParams}
	 * @return {@code LSP4J DidSaveTextDocumentParams}
	 */
	public static org.eclipse.lsp4j.DidSaveTextDocumentParams toDidSaveTextDocumentParams(
			DidSaveTextDocumentParams from) {
		if (from == null) {
			return null;
		}
		org.eclipse.lsp4j.DidSaveTextDocumentParams to = new org.eclipse.lsp4j.DidSaveTextDocumentParams();
		return to;
	}

	/**
	 * Convert {@code LSP4J} {@link org.eclipse.lsp4j.DidSaveTextDocumentParams} to {@code Spring DSL}
	 * {@link DidSaveTextDocumentParams}.
	 *
	 * @param from the {@code LSP4J DidSaveTextDocumentParams}
	 * @return {@code Spring DSL DidSaveTextDocumentParams}
	 */
	public static DidSaveTextDocumentParams toDidSaveTextDocumentParams(
			org.eclipse.lsp4j.DidSaveTextDocumentParams from) {
		if (from == null) {
			return null;
		}
		DidSaveTextDocumentParams to = new DidSaveTextDocumentParams();
		return to;
	}

	/**
	 * Convert {@code Spring DSL} {@link TextDocumentItem} to {@code LSP4J}
	 * {@link org.eclipse.lsp4j.TextDocumentItem}.
	 *
	 * @param from the {@code Spring DSL TextDocumentItem}
	 * @return {@code LSP4J TextDocumentItem}
	 */
	public static org.eclipse.lsp4j.TextDocumentItem toTextDocumentItem(TextDocumentItem from) {
		if (from == null) {
			return null;
		}
		org.eclipse.lsp4j.TextDocumentItem to = new org.eclipse.lsp4j.TextDocumentItem();
		to.setUri(from.getUri());
		to.setLanguageId(from.getLanguageId());
		to.setVersion(from.getVersion());
		to.setText(from.getText());
		return to;
	}

	/**
	 * Convert {@code LSP4J} {@link org.eclipse.lsp4j.TextDocumentItem} to {@code Spring DSL}
	 * {@link TextDocumentItem}.
	 *
	 * @param from the {@code LSP4J TextDocumentItem}
	 * @return {@code Spring DSL TextDocumentItem}
	 */
	public static TextDocumentItem toTextDocumentItem(org.eclipse.lsp4j.TextDocumentItem from) {
		if (from == null) {
			return null;
		}
		TextDocumentItem to = new TextDocumentItem();
		to.setUri(from.getUri());
		to.setLanguageId(from.getLanguageId());
		to.setVersion(from.getVersion());
		to.setText(from.getText());
		return to;
	}

	/**
	 * Convert {@code Spring DSL} {@link PublishDiagnosticsParams} to {@code LSP4J}
	 * {@link org.eclipse.lsp4j.PublishDiagnosticsParams}.
	 *
	 * @param from the {@code Spring DSL PublishDiagnosticsParams}
	 * @return {@code LSP4J PublishDiagnosticsParams}
	 */
	public static org.eclipse.lsp4j.PublishDiagnosticsParams toPublishDiagnosticsParams(PublishDiagnosticsParams from) {
		if (from == null) {
			return null;
		}
		org.eclipse.lsp4j.PublishDiagnosticsParams to = new org.eclipse.lsp4j.PublishDiagnosticsParams();
		to.setUri(from.getUri());
		List<org.eclipse.lsp4j.Diagnostic> diagnostics = new ArrayList<org.eclipse.lsp4j.Diagnostic>();
		for (Diagnostic d : from.getDiagnostics()) {
			diagnostics.add(toDiagnostic(d));
		}
		to.setDiagnostics(diagnostics);
		return to;
	}

	/**
	 * Convert {@code LSP4J} {@link org.eclipse.lsp4j.PublishDiagnosticsParams} to
	 * {@code Spring DSL} {@link PublishDiagnosticsParams}.
	 *
	 * @param from the {@code LSP4J PublishDiagnosticsParams}
	 * @return {@code Spring DSL PublishDiagnosticsParams}
	 */
	public static PublishDiagnosticsParams toPublishDiagnosticsParams(org.eclipse.lsp4j.PublishDiagnosticsParams from) {
		if (from == null) {
			return null;
		}
		PublishDiagnosticsParams to = new PublishDiagnosticsParams();
		to.setUri(from.getUri());
		List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
		for (org.eclipse.lsp4j.Diagnostic d : from.getDiagnostics()) {
			diagnostics.add(toDiagnostic(d));
		}
		to.setDiagnostics(diagnostics);
		return to;
	}

	/**
	 * Convert {@code Spring DSL} {@link Diagnostic} to {@code LSP4J}
	 * {@link org.eclipse.lsp4j.Diagnostic}.
	 *
	 * @param from the {@code Spring DSL Diagnostic}
	 * @return {@code LSP4J Diagnostic}
	 */
	public static org.eclipse.lsp4j.Diagnostic toDiagnostic(Diagnostic from) {
		if (from == null) {
			return null;
		}
		org.eclipse.lsp4j.Diagnostic to = new org.eclipse.lsp4j.Diagnostic();
		to.setRange(toRange(from.getRange()));
		to.setSeverity(toDiagnosticSeverity(from.getSeverity()));
		to.setCode(from.getCode());
		to.setSource(from.getSource());
		to.setMessage(from.getMessage());
		return to;
	}

	/**
	 * Convert {@code LSP4J} {@link org.eclipse.lsp4j.Diagnostic} to {@code Spring DSL}
	 * {@link Diagnostic}.
	 *
	 * @param from the {@code LSP4J Diagnostic}
	 * @return {@code Spring DSL Diagnostic}
	 */
	public static Diagnostic toDiagnostic(org.eclipse.lsp4j.Diagnostic from) {
		if (from == null) {
			return null;
		}
		Diagnostic to = new Diagnostic();
		to.setRange(toRange(from.getRange()));
		to.setSeverity(toDiagnosticSeverity(from.getSeverity()));
		to.setCode(from.getCode());
		to.setSource(from.getSource());
		to.setMessage(from.getMessage());
		return to;
	}

	/**
	 * Convert {@code Spring DSL} {@link Range} to {@code LSP4J}
	 * {@link org.eclipse.lsp4j.Range}.
	 *
	 * @param from the {@code Spring DSL Range}
	 * @return {@code LSP4J Range}
	 */
	public static org.eclipse.lsp4j.Range toRange(Range from) {
		if (from == null) {
			return null;
		}
		org.eclipse.lsp4j.Range to = new org.eclipse.lsp4j.Range();
		to.setStart(toPosition(from.getStart()));
		to.setEnd(toPosition(from.getEnd()));
		return to;
	}

	/**
	 * Convert {@code LSP4J} {@link org.eclipse.lsp4j.Range} to {@code Spring DSL}
	 * {@link Range}.
	 *
	 * @param from the {@code LSP4J Range}
	 * @return {@code Spring DSL Range}
	 */
	public static Range toRange(org.eclipse.lsp4j.Range from) {
		if (from == null) {
			return null;
		}
		Range to = new Range();
		to.setStart(toPosition(from.getStart()));
		to.setEnd(toPosition(from.getEnd()));
		return to;
	}

	/**
	 * Convert {@code Spring DSL} {@link Position} to {@code LSP4J}
	 * {@link org.eclipse.lsp4j.Position}.
	 *
	 * @param from the {@code Spring DSL Position}
	 * @return {@code LSP4J Position}
	 */
	public static org.eclipse.lsp4j.Position toPosition(Position from) {
		org.eclipse.lsp4j.Position to = new org.eclipse.lsp4j.Position();
		to.setLine(from.getLine());
		to.setCharacter(from.getCharacter());
		return to;
	}

	/**
	 * Convert {@code LSP4J} {@link org.eclipse.lsp4j.Position} to {@code Spring DSL}
	 * {@link Position}.
	 *
	 * @param from the {@code LSP4J Position}
	 * @return {@code Spring DSL Position}
	 */
	public static Position toPosition(org.eclipse.lsp4j.Position from) {
		Position to = new Position();
		to.setLine(from.getLine());
		to.setCharacter(from.getCharacter());
		return to;
	}

	/**
	 * Convert {@code Spring DSL} {@link DiagnosticSeverity} to {@code LSP4J}
	 * {@link org.eclipse.lsp4j.DiagnosticSeverity}.
	 *
	 * @param from the {@code Spring DSL DiagnosticSeverity}
	 * @return {@code LSP4J DiagnosticSeverity}
	 */
	public static org.eclipse.lsp4j.DiagnosticSeverity toDiagnosticSeverity(DiagnosticSeverity from) {
		if (from == null) {
			return null;
		}
		return org.eclipse.lsp4j.DiagnosticSeverity.valueOf(from.name());
	}

	/**
	 * Convert {@code LSP4J} {@link org.eclipse.lsp4j.DiagnosticSeverity} to {@code Spring DSL}
	 * {@link DiagnosticSeverity}.
	 *
	 * @param from the {@code LSP4J DiagnosticSeverity}
	 * @return {@code Spring DSL DiagnosticSeverity}
	 */
	public static DiagnosticSeverity toDiagnosticSeverity(org.eclipse.lsp4j.DiagnosticSeverity from) {
		if (from == null) {
			return null;
		}
		return DiagnosticSeverity.valueOf(from.name());
	}

	/**
	 * Convert {@code Spring DSL} {@link Hover} to {@code LSP4J}
	 * {@link org.eclipse.lsp4j.Hover}.
	 *
	 * @param from the {@code Spring DSL Hover}
	 * @return {@code LSP4J Hover}
	 */
	public static org.eclipse.lsp4j.Hover toHover(Hover from) {
		if (from == null) {
			return null;
		}
		return new org.eclipse.lsp4j.Hover();
	}

	/**
	 * Convert {@code LSP4J} {@link org.eclipse.lsp4j.Hover} to {@code Spring DSL}
	 * {@link Hover}.
	 *
	 * @param from the {@code LSP4J Hover}
	 * @return {@code Spring DSL Hover}
	 */
	public static Hover toHover(org.eclipse.lsp4j.Hover from) {
		if (from == null) {
			return null;
		}
		return new Hover();
	}

	/**
	 * Convert {@code Spring DSL} {@link TextDocumentPositionParams} to
	 * {@code LSP4J} {@link org.eclipse.lsp4j.TextDocumentPositionParams}.
	 *
	 * @param from the {@code Spring DSL TextDocumentPositionParams}
	 * @return {@code LSP4J TextDocumentPositionParams}
	 */
	public static org.eclipse.lsp4j.TextDocumentPositionParams toTextDocumentPositionParams(
			TextDocumentPositionParams from) {
		if (from == null) {
			return null;
		}
		return new org.eclipse.lsp4j.TextDocumentPositionParams();
	}

	/**
	 * Convert {@code LSP4J} {@link org.eclipse.lsp4j.TextDocumentPositionParams} to
	 * {@code Spring DSL} {@link TextDocumentPositionParams}.
	 *
	 * @param from the {@code LSP4J TextDocumentPositionParams}
	 * @return {@code Spring DSL TextDocumentPositionParams}
	 */
	public static TextDocumentPositionParams toTextDocumentPositionParams(
			org.eclipse.lsp4j.TextDocumentPositionParams from) {
		if (from == null) {
			return null;
		}
		return new TextDocumentPositionParams();
	}

	/**
	 * Convert {@code Spring DSL} {@link CompletionItem} to {@code LSP4J}
	 * {@link org.eclipse.lsp4j.CompletionItem}.
	 *
	 * @param from the {@code Spring DSL CompletionItem}
	 * @return {@code LSP4J CompletionItem}
	 */
	public static org.eclipse.lsp4j.CompletionItem toCompletionItem(CompletionItem from) {
		if (from == null) {
			return null;
		}
		return new org.eclipse.lsp4j.CompletionItem();
	}


	/**
	 * Convert {@code LSP4J} {@link org.eclipse.lsp4j.CompletionItem} to
	 * {@code Spring DSL} {@link CompletionItem}.
	 *
	 * @param from the {@code LSP4J CompletionItem}
	 * @return {@code Spring DSL CompletionItem}
	 */
	public static CompletionItem toCompletionItem(org.eclipse.lsp4j.CompletionItem from) {
		if (from == null) {
			return null;
		}
		return new CompletionItem();
	}

	/**
	 * Convert {@code Spring DSL} {@link TextDocumentIdentifier} to {@code LSP4J}
	 * {@link org.eclipse.lsp4j.TextDocumentIdentifier}.
	 *
	 * @param from the {@code Spring DSL TextDocumentIdentifier}
	 * @return {@code LSP4J TextDocumentIdentifier}
	 */
	public static org.eclipse.lsp4j.TextDocumentIdentifier toTextDocumentIdentifier(TextDocumentIdentifier from) {
		if (from == null) {
			return null;
		}
		return new org.eclipse.lsp4j.TextDocumentIdentifier();
	}


	/**
	 * Convert {@code LSP4J} {@link org.eclipse.lsp4j.TextDocumentIdentifier} to
	 * {@code Spring DSL} {@link TextDocumentIdentifier}.
	 *
	 * @param from the {@code LSP4J TextDocumentIdentifier}
	 * @return {@code Spring DSL TextDocumentIdentifier}
	 */
	public static TextDocumentIdentifier toTextDocumentIdentifier(org.eclipse.lsp4j.TextDocumentIdentifier from) {
		if (from == null) {
			return null;
		}
		return new TextDocumentIdentifier();
	}
}