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

import java.util.LinkedList;
import java.util.Objects;
import java.util.Scanner;

import scheme.types.SBoolean;
import scheme.types.SCharacter;
import scheme.types.SNull;
import scheme.types.SNumber;
import scheme.types.SPair;
import scheme.types.SString;
import scheme.types.SSymbol;
import scheme.types.SValue;
import scheme.types.SVector;

/**
 * Parser contains the main parser for the Scheme implementation capable of
 * reading full expressions from a {@link Scanner}.
 */
public class Parser {
	
	private TokenBuffer tokenBuffer;
		
	public Parser(final Scanner scanner) {
		Objects.requireNonNull(scanner);
		this.tokenBuffer = new TokenBuffer(scanner);
	}	

	/** Reads a full Scheme expression from the scanner. **/
	public SValue readExpression() throws EvaluationException {
		return parseHelper(tokenBuffer);
	}

	/**
	 * parseHelper is the recursive part of the parser. It keeps the
	 * invariants:
	 * 
	 * * After a call, the queue will have all consumed tokens removed.
	 * * If the queue isn't blank upon return to the top-level caller, 
	 *   the expression was not well-formed.
	 * 
	 * @param tokenQueue a mutable queue of tokens
	 * @return A top-level node
	 */
	private static SValue parseHelper(final TokenBuffer tokenQueue) throws EvaluationException {
		final var firstToken = tokenQueue.poll();
		switch(firstToken.getType()) {
		case FALSE:
			return SBoolean.FALSE;
		case DOT: // top-level dot is an identifier
		case IDENTIFIER:
			return SSymbol.of(firstToken.getData());
		case INTEGER:
			return SNumber.parse(firstToken.getData(), 10);
		case LPAREN:
			var sentinel = SPair.cons( SPair.empty(), SPair.empty());
			var tail = sentinel;
			
			while(!lookaheadIs(tokenQueue, TokenType.RPAREN)) {
				var tmp = SPair.cons(parseHelper(tokenQueue), SPair.empty());
				tail.setCdr(tmp);
				tail = tmp;
				
				// Shorthand for irregular lists; MUST be of the form DOT <expr> RPAREN
				if(lookaheadIs(tokenQueue, TokenType.DOT)) {
					tokenQueue.poll(); // consume DOT
					tail.setCdr(parseHelper(tokenQueue));
					if(!lookaheadIs(tokenQueue, TokenType.RPAREN)) {
						throw new EvaluationException("malformed dotted list");
					}
				}
			}
			tokenQueue.poll(); // consume RPAREN
			return 	sentinel.getCdr();
		case LVECTOR:
			var contents = new LinkedList<SValue>();
			while(!lookaheadIs(tokenQueue, TokenType.RPAREN)) {
				contents.add(parseHelper(tokenQueue));
			}
			tokenQueue.poll(); // consume RPAREN
			return SVector.of(contents.toArray(SValue[]::new));
		case RPAREN:
			// If the token is a close bracket, we know parseHelper broke
			// an invariant or the user entered invalid code.
			throw new EvaluationException("unexpected close bracket");
		case STRING:
			// FIXME: this is simplistic and doesn't handle escapes or quotes.
			return new SString(firstToken.getData().substring(1, firstToken.getData().length() - 1));
		case TRUE:
			return SBoolean.TRUE;
		case QUOTE:
			return SPair.properListOf(SSymbol.QUOTE, parseHelper(tokenQueue));
		case QUASIQUOTE:
			return SPair.properListOf(SSymbol.QUASIQUOTE, parseHelper(tokenQueue));
		case UNQUOTE:
			return SPair.properListOf(SSymbol.UNQUOTE, parseHelper(tokenQueue));
		case UNQUOTESPLICING:
			return SPair.properListOf(SSymbol.UNQUOTE_SPLICING, parseHelper(tokenQueue));
		case CHARSPACE:
			return new SCharacter(' ');
		case CHARNEWLINE:
			return new SCharacter('\n');
		case CHARRAW:
			var data = firstToken.getData();
			return new SCharacter(data.charAt(data.length()-1));
		case EOF:
			return SNull.EOF;
		// The rest of the tokens shouldn't exist unless explicitly fetched.
		case WHITESPACE:
		case COMMENT:
			throw new EvaluationException(String.format("Unexpected token: %s", firstToken.toString()));
		default:
			throw new IllegalArgumentException(String.format("Unknown token: %s", firstToken.toString()));
		}
	}
	
	private static boolean lookaheadIs(final TokenBuffer tokenQueue, TokenType type) throws EvaluationException {
		return tokenQueue.peek().getType() == type;
	}
}
