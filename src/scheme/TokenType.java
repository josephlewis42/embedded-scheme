// Copyright (c) 2021 Google LLC
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package scheme;

public enum TokenType {
	COMMENT(";.*\n", true),
	QUASIQUOTE("`"),
	UNQUOTESPLICING(",@"),
	UNQUOTE(","),
	LVECTOR("#\\("),
	LPAREN("\\("),
	RPAREN("\\)"),
	INTEGER("(-|\\+)?([0-9]+\\.?[0-9]*|[0-9]*\\.?[0-9]+)(e(-|\\+)?[0-9]+)?"),
	TRUE("#t"),
	FALSE("#f"),
	CHARSPACE("#\\\\space"),
	CHARNEWLINE("#\\\\newline"),
	CHARRAW("#\\\\."),
	QUOTE("'"),
	WHITESPACE("\\s+", true),
	STRING("\"(\\\\\\\"|[^\\\"])*\""),
	DOT("\\."),
	IDENTIFIER("[^\\d][^\\s()]*"),
	EOF("\\Z");
	
	private final String pattern;
	private final boolean ignored;

	private TokenType(String pattern) {
		this(pattern, false);
	}
	
	private TokenType(String pattern, boolean ignored) {
		this.pattern = pattern;
		this.ignored = ignored;
	}
	
	public String getPattern() {
		return pattern;
	}

	public boolean isIgnored() {
		return ignored;
	}
}
