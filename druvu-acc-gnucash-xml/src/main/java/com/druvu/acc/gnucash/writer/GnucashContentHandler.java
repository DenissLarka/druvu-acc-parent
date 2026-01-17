package com.druvu.acc.gnucash.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;

/**
 * SAX ContentHandler that writes GnuCash-compatible XML output.
 * <p>
 * Handles proper indentation, GUID lowercase formatting, and XML entity encoding
 * to produce output that is compatible with what GnuCash itself writes.
 *
 * @author Deniss Larka
 * <br/>on 13 Jan 2026
 */
@Slf4j
public class GnucashContentHandler implements ContentHandler {

	private static final String XML_DATA_TYPE_GUID = "guid";

	private static final String[] ENCODE_FROM = {"&", ">", "<"};
	private static final String[] ENCODE_TO = {"&amp;", "&gt;", "&lt;"};

	private static final int LAST_WAS_OPEN_ELEMENT = 1;
	private static final int LAST_WAS_CLOSE_ELEMENT = 2;
	private static final int LAST_WAS_CHARACTER_DATA = 3;

	private final Writer writer;

	private int depth = 0;
	private int lastWas = 0;
	private char[] spaces;

	private boolean isGUID = false;
	private boolean isSlotValueTypeString = false;
	private boolean isTrnDescription = false;
	private boolean insideGncTemplateTransactions = false;

	public GnucashContentHandler(Writer writer) {
		this.writer = writer;
	}

	@Override
	public void startDocument() throws SAXException {
		try {
			writer.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
		}
		catch (IOException e) {
			log.error("Error writing start document", e);
			throw new SAXException(e);
		}
	}

	@Override
	public void endDocument() throws SAXException {
		try {
			writer.write("\n\n<!-- Local variables: -->\n<!-- mode: xml        -->\n<!-- End:             -->\n");
		}
		catch (IOException e) {
			log.error("Error writing end document", e);
			throw new SAXException(e);
		}
	}

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
			throws SAXException {
		try {
			if (lastWas == LAST_WAS_OPEN_ELEMENT) {
				writer.write(">\n");
				writeSpaces();
			}

			if (lastWas == LAST_WAS_CLOSE_ELEMENT) {
				writer.write("\n");
				writeSpaces();
			}

			writer.write("<" + qName);

			if (qName.equals("gnc_template-transactions")) {
				insideGncTemplateTransactions = true;
			}

			isTrnDescription = qName.equals("trn_description");
			isGUID = false;
			isSlotValueTypeString = false;

			for (int i = 0; i < atts.getLength(); i++) {
				writer.write(" " + atts.getQName(i) + "=\"" + atts.getValue(i) + "\"");

				if (atts.getQName(i).equals("type") && atts.getValue(i).equals(XML_DATA_TYPE_GUID)) {
					isGUID = true;
				}

				if (qName.equals("slot_value") && atts.getQName(i).equals("type") && atts.getValue(i).equals("string")) {
					isSlotValueTypeString = true;
				}
			}

			depth += 2;
			lastWas = LAST_WAS_OPEN_ELEMENT;
		}
		catch (IOException e) {
			log.error("Error writing start element: {}", qName, e);
			throw new SAXException(e);
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		try {
			// Create <slot:value type="string"></slot:value> instead of <slot:value type="string"/>
			if ((isTrnDescription || isSlotValueTypeString) && lastWas != LAST_WAS_CHARACTER_DATA) {
				characters(new char[0], 0, 0);
			}

			if (qName.equals("gnc_template-transactions")) {
				insideGncTemplateTransactions = false;
			}

			depth -= 2;

			if (lastWas == LAST_WAS_CLOSE_ELEMENT) {
				writer.write("\n");
				writeSpaces();
				writer.write("</" + qName + ">");
			}

			if (lastWas == LAST_WAS_OPEN_ELEMENT) {
				writer.write("/>");
			}

			if (lastWas == LAST_WAS_CHARACTER_DATA) {
				writer.write("</" + qName + ">");
			}

			lastWas = LAST_WAS_CLOSE_ELEMENT;
		}
		catch (IOException e) {
			log.error("Error writing end element: {}", qName, e);
			throw new SAXException(e);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		try {
			if (lastWas == LAST_WAS_OPEN_ELEMENT) {
				writer.write(">");
			}

			if (lastWas == LAST_WAS_CLOSE_ELEMENT) {
				return;
			}

			// GUIDs should be written in lowercase
			if (isGUID) {
				String s = new String(ch, start, length);
				writer.write(s.toLowerCase());
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append(ch, start, length);

				// Encode XML entities
				for (int j = 0; j < ENCODE_FROM.length; j++) {
					int index = 0;
					while ((index = sb.indexOf(ENCODE_FROM[j], index)) != -1) {
						sb.replace(index, index + ENCODE_FROM[j].length(), ENCODE_TO[j]);
						index += ENCODE_TO[j].length() - ENCODE_FROM[j].length() + 1;
					}
				}

				writer.write(sb.toString());
			}

			lastWas = LAST_WAS_CHARACTER_DATA;
		}
		catch (IOException e) {
			log.error("Error writing characters", e);
			throw new SAXException(e);
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) {
		// Ignored
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		try {
			writer.write("<?" + target);
			if (data != null) {
				writer.write(data);
			}
			writer.write("?>\n");
		}
		catch (IOException e) {
			log.error("Error writing processing instruction", e);
			throw new SAXException(e);
		}
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		// Not used
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) {
		// Not used
	}

	@Override
	public void endPrefixMapping(String prefix) {
		// Not used
	}

	@Override
	public void skippedEntity(String name) {
		// Not used
	}

	private void writeSpaces() throws IOException {
		int effectiveDepth;
		if (insideGncTemplateTransactions) {
			if (depth < 6) {
				return;
			}
			effectiveDepth = depth - 6;
		} else {
			if (depth < 4) {
				return;
			}
			effectiveDepth = depth - 4;
		}

		if (spaces == null || spaces.length < effectiveDepth) {
			spaces = new char[effectiveDepth];
			Arrays.fill(spaces, ' ');
		}

		writer.write(spaces, 0, effectiveDepth);
	}
}
