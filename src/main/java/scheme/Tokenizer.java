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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Tokenizer {
	
	// Holds a pattern with named capture group for each TokenType.
	// Patterns are immutable and thread safe, but the matches they
	// produce are not.
	private static final Pattern TOKEN_PATTERN;
	
	static {
		var rawPattern = Arrays.stream(TokenType.values())
				.map(tt -> String.format("(?<%s>%s)", tt.name(), tt.getPattern()))
				.collect(Collectors.joining("|", "(?i)",""));

		TOKEN_PATTERN = Pattern.compile(rawPattern);
	}
	
	private Tokenizer() {
		// Private utility class constructor.
	}

	public static Token nextToken(final Scanner s) {
		Objects.requireNonNull(s);

		while (s.hasNext()) {
			var match = s.findWithinHorizon(TOKEN_PATTERN, 0);
			var matcher = TOKEN_PATTERN.matcher(match);

			var possible = tokenFromMatch(matcher);
			if (possible.isPresent()) {
				return possible.get();
			}
		}

		return new Token(TokenType.EOF, "");
	}
	
	private static Optional<Token> tokenFromMatch(Matcher matcher) {
		if (!matcher.matches()) {
			throw new IllegalArgumentException("requires a match");
		}

		for (var tt : TokenType.values()) {
			var groupData = matcher.group(tt.name());
			if (groupData != null) { // found this token
				if (!tt.isIgnored()) { // make sure not ignored
					return Optional.of(new Token(tt, groupData));
				}

				break;
			}

			// TODO: if we reached here without finding a match, what do we do?
		}

    	return Optional.empty();
	}
}
