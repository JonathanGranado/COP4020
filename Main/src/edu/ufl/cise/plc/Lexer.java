package edu.ufl.cise.plc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

enum State {
    START,
    HAVE_ZERO,
    HAVE_DOT,
    HAVE_EQ,
    HAVE_QUOTE,
    HAVE_MINUS,
    HAVE_LT,
    HAVE_GT,
    HAVE_BANG, // "!"
    IN_NUM,
    IN_IDENT,
    IN_FLOAT,

}

public class Lexer implements ILexer {

    public ArrayList<IToken> holdingTokens = new ArrayList<>();
    char[] chars;
    State state = State.START;
    int arrayPos = 0;
    ListIterator<IToken> iter = holdingTokens.listIterator();

    public Lexer(String input) throws LexicalException {

        Iterator<IToken> iter = holdingTokens.iterator();

        if (input.isEmpty()) {
            chars = new char[1];
            chars[0] = '~';
            System.out.println(chars.length + " checking index" + chars[0]);
        } else {
            chars = new char[input.length() + 1];
            for (int i = 0; i < input.length(); i++) {
                chars[i] = input.charAt(i);
            }
        }
        chars[input.length()] = '~';


        int pos = 0;
        int lineNumber = 0;
        int startPos = 0;
        loop:
        while (true) {
            char ch = chars[pos];
            if (state != null) {
                switch (state) {
                    case START -> {
                        // single characters, and white space characters, just missing the commment

                        switch (ch) {
                            case ' ', '\t', '\r' -> {
                                pos++;
                                startPos++;
                            }
                            case '\n' -> {
                                pos++;
                                lineNumber++;
                                startPos = 0;
                            }
                            case '"' -> {
                                holdingTokens.add(new Token(IToken.Kind.STRING_LIT, "\"", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case '&' -> {
                                holdingTokens.add(new Token(IToken.Kind.AND, "&", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case ',' -> {
                                holdingTokens.add(new Token(IToken.Kind.COMMA, ",", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case '/' -> {
                                holdingTokens.add(new Token(IToken.Kind.DIV, "/", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case '(' -> {
                                holdingTokens.add(new Token(IToken.Kind.LPAREN, "(", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case '[' -> {
                                holdingTokens.add(new Token(IToken.Kind.LSQUARE, "[", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case '%' -> {
                                holdingTokens.add(new Token(IToken.Kind.MOD, "%", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case '|' -> {
                                holdingTokens.add(new Token(IToken.Kind.OR, "|", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case '+' -> {
                                holdingTokens.add(new Token(IToken.Kind.PLUS, "+", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case '^' -> {
                                holdingTokens.add(new Token(IToken.Kind.RETURN, "^", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case ')' -> {
                                holdingTokens.add(new Token(IToken.Kind.RPAREN, ")", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case ']' -> {
                                holdingTokens.add(new Token(IToken.Kind.RSQUARE, "]", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case ':' -> {
                                holdingTokens.add(new Token(IToken.Kind.SEMI, ":", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case '*' -> {
                                holdingTokens.add(new Token(IToken.Kind.TIMES, "*", startPos, 1, lineNumber));
                                pos++;
                                startPos++;
                            }
                            case '=' -> {
                                state = State.HAVE_EQ;
                                pos++;
                                startPos++;
                            }
                            case '-' -> {
                                state = State.HAVE_MINUS;
                                pos++;
                                startPos++;
                            }
                            case '<' -> {
                                state = State.HAVE_LT;
                                pos++;
                                startPos++;
                            }
                            case '>' -> {
                                state = State.HAVE_GT;
                                pos++;
                                startPos++;
                            }
                            case '!' -> {
                                state = State.HAVE_BANG;
                                pos++;
                                startPos++;
                            }
                            case '~' -> {
                                holdingTokens.add(new Token(IToken.Kind.EOF, "~", startPos, 1, lineNumber));
                                break loop;
                            }
                        }
                    }
                    // leads to either a int of just 0 or float if it has . then another digit
                    case HAVE_ZERO -> {

                    }
                    // check if it has case of digit, if not throw lexicalException
                    case HAVE_DOT -> {

                    }
                    // if the next character is not another = it is an error
                    case HAVE_EQ -> {
                        switch (ch) {
                            case '=' -> {
                                holdingTokens.add(new Token(IToken.Kind.EQUALS, "=", startPos, 2, lineNumber));
                                pos++;
                                startPos++;
                            }
                            default -> throw new LexicalException("Incorrect Token sequence ");
                        }
                    }
                    // start of string lit
                    case HAVE_QUOTE -> {

                    }
                    // can have ->, or just -
                    case HAVE_MINUS -> {

                    }
                    // can have <, or <-, or <=
                    case HAVE_LT -> {

                    }
                    // can have >, or >>, or >=
                    case HAVE_GT -> {

                    }
                    // can just have !, or !=
                    case HAVE_BANG -> {

                    }
                    case IN_NUM -> {
                        switch (ch) {
                            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                                pos++;
                            }
                            default -> {
                                holdingTokens.add(new Token(IToken.Kind.INT_LIT, String.valueOf(ch), startPos, pos - startPos, lineNumber));
                                state = State.START;
                            }
                        }
                    }
                    // if it has letter, $, or _, then stays (by increasing pos++) if it also has 0..9
                    case IN_IDENT -> {

                    }
                    // in float after int lit -> have dot and then has a digit
                    case IN_FLOAT -> {

                    }
                }
            }
        }
    }

    @Override
    public IToken next() throws LexicalException {

        IToken result = holdingTokens.get(arrayPos);
        arrayPos++;
        return result;
    }

    @Override
    public IToken peek() throws LexicalException {
        IToken result = holdingTokens.get(arrayPos);
        return result;
    }

}

