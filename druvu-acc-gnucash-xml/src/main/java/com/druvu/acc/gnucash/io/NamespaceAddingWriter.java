package com.druvu.acc.gnucash.io;

import java.io.IOException;
import java.io.Writer;

/**
 * A Writer that replaces '_' in XML tag names and attribute names with ':'.
 * <p>
 * This is the opposite of {@link NamespaceRemovingReader} - it converts underscored
 * element names like {@code <gnc_book>} back to namespaced format like {@code <gnc:book>}
 * that GnuCash expects.
 * <p>
 * Also, injects the required xmlns declarations after the root element.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 13
 */
public class NamespaceAddingWriter extends Writer {

	private static final String XMLNS_DECLARATIONS = """
			
			xmlns:gnc="http://www.gnucash.org/XML/gnc"
			xmlns:act="http://www.gnucash.org/XML/act"
			xmlns:book="http://www.gnucash.org/XML/book"
			xmlns:cd="http://www.gnucash.org/XML/cd"
			xmlns:cmdty="http://www.gnucash.org/XML/cmdty"
			xmlns:price="http://www.gnucash.org/XML/price"
			xmlns:slot="http://www.gnucash.org/XML/slot"
			xmlns:split="http://www.gnucash.org/XML/split"
			xmlns:sx="http://www.gnucash.org/XML/sx"
			xmlns:trn="http://www.gnucash.org/XML/trn"
			xmlns:ts="http://www.gnucash.org/XML/ts"
			xmlns:fs="http://www.gnucash.org/XML/fs"
			xmlns:bgt="http://www.gnucash.org/XML/bgt"
			xmlns:recurrence="http://www.gnucash.org/XML/recurrence"
			xmlns:lot="http://www.gnucash.org/XML/lot"
			xmlns:cust="http://www.gnucash.org/XML/cust"
			xmlns:job="http://www.gnucash.org/XML/job"
			xmlns:addr="http://www.gnucash.org/XML/addr"
			xmlns:owner="http://www.gnucash.org/XML/owner"
			xmlns:taxtable="http://www.gnucash.org/XML/taxtable"
			xmlns:tte="http://www.gnucash.org/XML/tte"
			xmlns:employee="http://www.gnucash.org/XML/employee"
			xmlns:order="http://www.gnucash.org/XML/order"
			xmlns:billterm="http://www.gnucash.org/XML/billterm"
			xmlns:bt-days="http://www.gnucash.org/XML/bt-days"
			xmlns:bt-prox="http://www.gnucash.org/XML/bt-prox"
			xmlns:invoice="http://www.gnucash.org/XML/invoice"
			xmlns:entry="http://www.gnucash.org/XML/entry"
			xmlns:vendor="http://www.gnucash.org/XML/vendor\"""";

	// Tag names that legitimately contain underscores (not namespace separators)
	private static final String[] UNDERSCORE_EXCEPTIONS = {
			"fs:ui",            // fs:ui_type
			"cmdty:get",        // cmdty:get_quotes
			"cmdty:quote",      // cmdty:quote_source
			"invoice:billing",  // invoice:billing_id
			"recurrence:period" // recurrence:period_type
	};

	private final Writer output;
	private boolean isInTag = false;
	private boolean isInQuotation = false;

	public NamespaceAddingWriter(Writer output) {
		this.output = output;
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		for (int i = off; i < off + len; i++) {
			char c = cbuf[i];

			if (isInTag && (c == '"' || c == '\'')) {
				isInQuotation = !isInQuotation;
			} else if (c == '<' && !isInQuotation) {
				isInTag = true;
			} else if (c == '>' && !isInQuotation) {
				isInTag = false;
			} else if (c == '_' && isInTag && !isInQuotation) {
				// Replace '_' with ':' unless it's a legitimate underscore
				if (!isUnderscoreException(cbuf, i)) {
					cbuf[i] = ':';
				}
			}
		}

		output.write(cbuf, off, len);

		// Inject xmlns declarations after <gnc-v2 (which becomes <gnc:v2 after replacement)
		if (len == 7 && new String(cbuf, off, len).equals("<gnc:v2")) {
			output.write(XMLNS_DECLARATIONS);
		}
	}

	private boolean isUnderscoreException(char[] cbuf, int underscorePos) {
		for (String exception : UNDERSCORE_EXCEPTIONS) {
			int exLen = exception.length();
			if (underscorePos >= exLen) {
				String preceding = new String(cbuf, underscorePos - exLen, exLen);
				if (preceding.equals(exception)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void flush() throws IOException {
		output.flush();
	}

	@Override
	public void close() throws IOException {
		output.close();
	}
}
