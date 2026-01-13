package com.druvu.acc.gnucash.reader;

import java.io.IOException;
import java.io.Reader;

/**
 * A Reader that replaces ':' in XML tag names and attribute names with '_'.
 * <p>
 * This is necessary because GnuCash XML files use namespaced elements like {@code <gnc:book>}
 * but the XSD schema expects underscores like {@code <gnc_book>}.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
public class NamespaceRemovingReader extends Reader {

	private final Reader input;
	private boolean isInTag = false;
	private boolean isInQuotation = false;

	public NamespaceRemovingReader(Reader input) {
		this.input = input;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int read = input.read(cbuf, off, len);

		for (int i = off; i < off + read; i++) {
			char c = cbuf[i];

			if (isInTag && (c == '"' || c == '\'')) {
				isInQuotation = !isInQuotation;
			} else if (c == '<' && !isInQuotation) {
				isInTag = true;
			} else if (c == '>' && !isInQuotation) {
				isInTag = false;
			} else if (c == ':' && isInTag && !isInQuotation) {
				cbuf[i] = '_';
			}
		}

		return read;
	}

	@Override
	public void close() throws IOException {
		input.close();
	}
}
