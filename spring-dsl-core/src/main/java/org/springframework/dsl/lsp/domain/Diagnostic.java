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
package org.springframework.dsl.lsp.domain;

import org.springframework.dsl.lsp.domain.Range.RangeBuilder;
import org.springframework.dsl.support.AbstractDomainBuilder;
import org.springframework.dsl.support.DomainBuilder;

/**
 * Represents a diagnostic, such as a compiler error or warning. Diagnostic
 * objects are only valid in the scope of a resource.
 *
 * @author Janne Valkealahti
 *
 */
public class Diagnostic {

	private Range range;
	private DiagnosticSeverity severity;
	private String code;
	private String source;
	private String message;

	public Diagnostic() {
	}

	public Diagnostic(Range range, DiagnosticSeverity severity, String code, String source, String message) {
		this.range = range;
		this.severity = severity;
		this.code = code;
		this.source = source;
		this.message = message;
	}

	public Range getRange() {
		return range;
	}

	public void setRange(Range range) {
		this.range = range;
	}

	public DiagnosticSeverity getSeverity() {
		return severity;
	}

	public void setSeverity(DiagnosticSeverity severity) {
		this.severity = severity;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((range == null) ? 0 : range.hashCode());
		result = prime * result + ((severity == null) ? 0 : severity.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Diagnostic other = (Diagnostic) obj;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (range == null) {
			if (other.range != null)
				return false;
		} else if (!range.equals(other.range))
			return false;
		if (severity != other.severity)
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}
	
	public interface DiagnosticBuilder<P> extends DomainBuilder<Diagnostic, P> {

		RangeBuilder<DiagnosticBuilder<P>> range();
		DiagnosticBuilder<P> severity(DiagnosticSeverity severity);
		DiagnosticBuilder<P> code(String code);
		DiagnosticBuilder<P> source(String source);
		DiagnosticBuilder<P> message(String message);
		
	}

	public static <P> DiagnosticBuilder<P> diagnostic() {
		return new InternalDiagnosticBuilder<>(null);
	}

	protected static <P> DiagnosticBuilder<P> diagnostic(P parent) {
		return new InternalDiagnosticBuilder<>(parent);
	}
	
	private static class InternalDiagnosticBuilder<P>
			extends AbstractDomainBuilder<Diagnostic, P> implements DiagnosticBuilder<P> {

		private RangeBuilder<DiagnosticBuilder<P>> range;
		private DiagnosticSeverity severity;
		private String code;
		private String source;
		private String message;
		
		InternalDiagnosticBuilder(P parent) {
			super(parent);
		}

		@Override
		public RangeBuilder<DiagnosticBuilder<P>> range() {
			this.range = Range.range(this);
			return range;
		}

		@Override
		public DiagnosticBuilder<P> severity(DiagnosticSeverity severity) {
			this.severity = severity;
			return this;
		}

		@Override
		public DiagnosticBuilder<P> code(String code) {
			this.code = code;
			return this;
		}

		@Override
		public DiagnosticBuilder<P> source(String source) {
			this.source = source;
			return this;
		}

		@Override
		public DiagnosticBuilder<P> message(String message) {
			this.message = message;
			return this;
		}

		@Override
		public Diagnostic build() {
			Diagnostic diagnostic = new Diagnostic();
			if (range != null) {
				diagnostic.setRange(range.build());
			}
			diagnostic.setSeverity(severity);
			diagnostic.setCode(code);
			diagnostic.setSource(source);
			diagnostic.setMessage(message);
			return diagnostic;
		}
	}
}
