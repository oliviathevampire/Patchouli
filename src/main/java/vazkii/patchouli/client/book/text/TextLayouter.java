package vazkii.patchouli.client.book.text;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.resource.language.LanguageDefinition;
import vazkii.patchouli.client.book.gui.GuiBook;

public class TextLayouter {
	private final List<Word> words = new ArrayList<>();
	private final GuiBook gui;
	private final int pageX;
	private final int lineHeight;
	private final int pageWidth;

	public TextLayouter(GuiBook gui, int pageX, int pageY, int lineHeight, int pageWidth) {
		this.gui = gui;
		this.pageX = pageX;
		this.lineHeight = lineHeight;
		this.pageWidth = pageWidth;

		y = pageY;
	}

	private int y;
	private List<Word> linkCluster = null;
	private List<Span> spanCluster = null;

	private List<SpanTail> pending = new ArrayList<>();
	private int lineStart = 0;
	private int widthSoFar = 0;
	
	private TextRenderer font;

	public void layout(TextRenderer font, List<Span> spans) {
		this.font = font;
		
		List<Span> paragraph = new ArrayList<>();
		for (Span span : spans) {
			if (span.lineBreaks > 0) {
				layoutParagraph(paragraph);

				widthSoFar = 0;
				y += span.lineBreaks * lineHeight;
				paragraph.clear();
			}

			paragraph.add(span);
		}
		if (!paragraph.isEmpty())
			layoutParagraph(paragraph);
	}

	// a paragraph is a series of spans without explicit line break
	private void layoutParagraph(List<Span> paragraph) {
		String text = toString(paragraph);
		// todo fabric is there a helper to do this?
		LanguageDefinition language = MinecraftClient.getInstance().getLanguageManager().getLanguage();
		Locale locale;
		String[] splitLangCode = language.getName().split("_", 2);
		if (splitLangCode.length == 1) {
			locale = new Locale(language.getCode());
		} else {
			locale = new Locale(splitLangCode[0], splitLangCode[1]);
		}
		BreakIterator iterator = BreakIterator.getLineInstance(locale);
		iterator.setText(text);
		lineStart = 0;

		for (Span span : paragraph)
			append(iterator, span);

		flush();
	}

	private void append(BreakIterator iterator, Span span) {
		if (spanCluster != span.linkCluster) {
			linkCluster = span.linkCluster == null ? null : new ArrayList<>();
			spanCluster = span.linkCluster;
		}

		SpanTail last = new SpanTail(span, 0, linkCluster);
		widthSoFar += last.width;
		pending.add(last);

		while (widthSoFar > pageWidth) {
			breakLine(iterator);

			widthSoFar = 0;
			for (SpanTail pending : this.pending)
				widthSoFar += pending.width;
		}
	}

	private void breakLine(BreakIterator iterator) {
		int width = 0;
		int offset = 0;
		for (SpanTail pending : this.pending) {
			width += pending.width;
			offset += pending.length;
		}

		SpanTail last = pending.get(pending.size() - 1);
		width -= last.width;
		offset -= last.length;

		char[] characters = last.span.text.toCharArray();
		for (int i = last.start; i < characters.length; i++) {
			width += font.getCharWidth(characters[i]);
			if (last.span.bold)
				width++;

			if (width > pageWidth) {
				int overflowOffset = lineStart + offset + i - last.start;
				int breakOffset = overflowOffset + 1;
				if (!Character.isWhitespace(characters[i]))
					breakOffset = iterator.preceding(breakOffset);
				if (breakOffset <= lineStart) // could not break: we have a long word
					breakOffset = overflowOffset - 1; // cut off the word

				breakLine(breakOffset);
				return;
			}
		}

		// if this point has been reached, it means getCharWidth and getStringWidth give conflicting results
		// so just break the line here; to be sure the book doesn't hang the client in an infinite loop
		// it shouldn't happen [but you never know]
		flush();
		y += lineHeight;
	}

	private String toString(List<Span> paragraph) {
		StringBuilder result = new StringBuilder();
		for (Span span : paragraph)
			result.append(span.text);
		return result.toString();
	}

	public void flush() {
		if (pending.isEmpty())
			return;

		int x = pageX;
		for (SpanTail pending : this.pending) {
			words.add(pending.position(gui, x, y, pending.length));
			x += pending.width;
		}
		pending.clear();
	}

	private void breakLine(int textOffset) {
		int index;
		int offset = lineStart;
		int x = pageX;
		for (index = 0; index < pending.size(); index++) {
			SpanTail span = pending.get(index);
			if (offset + span.length < textOffset) {
				words.add(span.position(gui, x, y, span.length));
				offset += span.length;
				x += span.width;
			} else {
				words.add(span.position(gui, x, y, textOffset - offset));
				pending.set(index, span.tail(textOffset - offset));
				break;
			}
		}
		for (int i = index - 1; i >= 0; i--)
			pending.remove(i);

		lineStart = textOffset;
		y += lineHeight;
	}

	public List<Word> getWords() {
		return words;
	}

	private class SpanTail {
		
		private final Span span;
		private final int start;
		private final int width;
		private final List<Word> cluster;
		private final int length;

		public SpanTail(Span span, int start, List<Word> cluster) {
			this.span = span;
			this.start = start;
			this.width = font.getStringWidth(span.codes + span.text.substring(start)) + span.spacingLeft + span.spacingRight;
			this.cluster = cluster;
			this.length = span.text.length() - start;
		}

		public Word position(GuiBook gui, int x, int y, int length) {
			x += span.spacingLeft;
			Word result = new Word(gui, span, span.text.substring(start, start + length), x, y, width, cluster);
			if (cluster != null)
				cluster.add(result);
			return result;
		}

		public SpanTail tail(int offset) {
			return new SpanTail(span, start + offset, cluster);
		}
	}
}
