/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.dsl.document;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dsl.document.linetracker.DefaultLineTracker;
import org.springframework.dsl.document.linetracker.LineTracker;
import org.springframework.dsl.document.linetracker.Region;
import org.springframework.dsl.domain.DidChangeTextDocumentParams;
import org.springframework.dsl.domain.Position;
import org.springframework.dsl.domain.Range;
import org.springframework.dsl.domain.TextDocumentContentChangeEvent;
import org.springframework.dsl.domain.TextDocumentIdentifier;
import org.springframework.dsl.model.LanguageId;

import javolution.text.Text;

/**
 * {@link Document} implementation having a textual content and understands
 * normal line delimiters.
 *
 * @author Kris De Volder
 * @author Janne Valkealahti
 *
 */
public class TextDocument implements Document {

	private static final Logger log = LoggerFactory.getLogger(TextDocument.class);
	private static final Pattern NEWLINE = Pattern.compile("\\r(\\n)?|\\n");

	//TODO: should try to avoid haveing any methods returning String
	//      This defeats the point of using javaolution.Text (i.e. converion into
	//      String can cause massive string copying)

	//TODO: this 'stateful' object should be
	//      - renamed to TextDocumentState
	//      - not implement IDocument but have a method obtain an IDocument
	//      - representing a read-only snapshot of the document contents.
	//      - replace line tracker with something better.

	private LineTracker lineTracker = new DefaultLineTracker();
	private final LanguageId languageId;
	private final String uri;
	private Text text = new Text("");
	private int version;

	public TextDocument(String content) {
		this(null, null, 0, content);
	}

	public TextDocument(String uri, LanguageId languageId) {
		this(uri, languageId, 0, "");
	}

	public TextDocument(String uri, LanguageId languageId, int version, String text) {
		this.uri = uri;
		this.languageId = languageId;
		this.version = version;
		setText(text);
	}

	private TextDocument(TextDocument other) {
		this.uri = other.uri;
		this.languageId = other.languageId();
		this.text = other.text;
		this.lineTracker.set(text.toString());
		this.version = other.version;
	}

	@Override
	public String uri() {
		return uri;
	}

	@Override
	public String content() {
		return getText().toString();
	}

	@Override
	public int caret(Position position) {
		return lineTracker.getLineOffset(position.getLine()) + position.getCharacter();
	}

	public synchronized Text getText() {
		return text;
	}

	public synchronized void setText(String content) {
		this.text = new Text(content);
		this.lineTracker.set(content);
	}

	public synchronized void apply(DidChangeTextDocumentParams params) {
		int newVersion = params.getTextDocument().getVersion();
		if (version < newVersion) {
			log.trace("Number of changes {}", params.getContentChanges().size());
			for (TextDocumentContentChangeEvent change : params.getContentChanges()) {
				apply(change);
			}
			this.version = newVersion;
		} else {
			log.warn("Change event with bad version ignored, current {} new {}: {}", version, newVersion, params);
		}
	}

	/**
	 * Convert a simple offset+length pair into a vscode range. This is a method on
	 * TextDocument because it requires splitting document into lines to determine
	 * line numbers from offsets.
	 *
	 * @param offset the offset
	 * @param length the length
	 * @return the range
	 * @throws BadLocationException the bad location exception
	 */
	public Range toRange(int offset, int length) {
		int end = Math.min(offset + length, length());
		Range range = new Range();
		range.setStart(toPosition(offset));
		range.setEnd(toPosition(end));
		return range;
	}

	/**
	 * Determine the line-number a given offset (i.e. what line is the offset inside of?)
	 *
	 * @param offset the offset
	 * @return the int
	 * @throws BadLocationException the bad location exception
	 */
	private int lineNumber(int offset) {
		return lineTracker.getLineNumberOfOffset(offset);
	}

	private void apply(TextDocumentContentChangeEvent change) {
		log.trace("Old content before apply is '{}'", content());
		Range rng = change.getRange();
		if (rng==null) {
			//full sync mode
			setText(change.getText());
		} else {
			int start = toOffset(rng.getStart());
			int end = toOffset(rng.getEnd());
			replace(start, end-start, change.getText());
		}
		log.trace("New content after apply is '{}'", content());
	}

//	@Override
	public Position toPosition(int offset) {
		int line = lineNumber(offset);
		int startOfLine = startOfLine(line);
		int column = offset - startOfLine;
		Position pos = new Position();
		pos.setCharacter(column);
		pos.setLine(line);
		return pos;
	}

	private int startOfLine(int line) {
		Region region = lineTracker.getLineInformation(line);
		return region.getOffset();
	}

//	@Override
//	public Region getLineInformationOfOffset(int offset) {
//		try {
//			if (offset<=getLength()) {
//				int line = lineNumber(offset);
//				return getLineInformation(line);
//			}
//		} catch (BadLocationException e) {
//			//outside document.
//		}
//		return null;
//	}

	@Override
	public int length() {
		return text.length();
	}

//	@Override
	@Override
	public String content(int start, int len) {
		try {
			return text.subtext(start, start+len).toString();
		} catch (Exception e) {
			throw new BadLocationException("Error processing subtext", e);
		}
	}

	@Override
	public int lineCount() {
		return lineTracker.getNumberOfLines();
	}

//	@Override
	public String getDefaultLineDelimiter() {
		Matcher newlineFinder = NEWLINE.matcher(text);
		if (newlineFinder.find()) {
			return text.subtext(newlineFinder.start(), newlineFinder.end()).toString();
		}
		return System.getProperty("line.separator");
	}

//	@Override
	@Override
	public char charAt(int offset) {
		if (offset >= 0 && offset < text.length()) {
			return text.charAt(offset);
		}
		throw new BadLocationException("Offset location not in bounds");
	}

//	@Override
	public int getLineOfOffset(int offset) {
		return lineTracker.getLineNumberOfOffset(offset);
	}

//	@Override
//	public Region getLineInformation(int line) {
//		try {
//			return lineTracker.getLineInformation(line);
//		} catch (BadLocationException e) {
//			//line doesn't exist
//		}
//		return null;
//	}

//	@Override
	public int getLineOffset(int line) {
		return lineTracker.getLineOffset(line);
	}

	public int toOffset(Position position) {
		Region region = lineTracker.getLineInformation(position.getLine());
		int lineStart = region.getOffset();
		return lineStart + position.getCharacter();
	}

//	@Override
	public synchronized void replace(int start, int len, String ins) {
		int end = start+len;
		text = text
			.delete(start, end)
			.insert(start, new Text(ins));
		lineTracker.replace(start, len, ins);
	}

	public synchronized TextDocument copy() {
		return new TextDocument(this);
	}

//	@Override
	public String textBetween(int start, int end) {
		return content(start, end-start);
	}

	@Override
	public String toString() {
		return "TextDocument(uri="+uri+"["+version+"],\n"+this.text+"\n)";
	}

	/**
	 * Like getChar but never throws {@link BadLocationException}. Instead it
	 * return (char)0 for offsets outside the document.
	 *
	 * @param offset the offset
	 * @return the safe char
	 */
	public char getSafeChar(int offset) {
		try {
			return charAt(offset);
		} catch (BadLocationException e) {
			return 0;
		}
	}

	@Override
	public LanguageId languageId() {
		return languageId;
	}

	@Override
	public int getVersion() {
		return version;
	}

	public TextDocumentIdentifier getId() {
		if (uri!=null) {
			return new TextDocumentIdentifier(uri);
		}
		return null;
	}
}
