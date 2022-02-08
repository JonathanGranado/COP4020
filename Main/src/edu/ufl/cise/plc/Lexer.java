package edu.ufl.cise.plc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


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
    IN_COMMENT,
    IN_NUM,
    IN_IDENT,
    IN_FLOAT,
    IN_STRING
}

public class Lexer implements ILexer {

    Map<String, IToken.Kind> reservedMap = new HashMap<>();
    public ArrayList<IToken> holdingTokens = new ArrayList<>();
    char[] chars;
    State state = State.START;
    int arrayPos = 0;
    int pos, lineNumber, startPos, lineBasePos;


    public Lexer(String input) throws LexicalException {
        String holdingToken = "";

        if (input.isEmpty()) {
            chars = new char[1];
            chars[0] = '~';
        } else {
            chars = new char[input.length() + 1];
            for (int i = 0; i < input.length(); i++) {
                chars[i] = input.charAt(i);
            }
        }
        chars[input.length()] = '~';
        reservedMap.put("int", IToken.Kind.TYPE);
        reservedMap.put("float", IToken.Kind.TYPE);
        reservedMap.put("string", IToken.Kind.TYPE);
        reservedMap.put("boolean", IToken.Kind.TYPE);
        reservedMap.put("color", IToken.Kind.TYPE);
        reservedMap.put("image", IToken.Kind.TYPE);
        reservedMap.put("void", IToken.Kind.KW_VOID);
        // <image_op>
        reservedMap.put("getWidth", IToken.Kind.IMAGE_OP);
        reservedMap.put("getHeight", IToken.Kind.IMAGE_OP);
        // <color_op>
        reservedMap.put("getRed", IToken.Kind.COLOR_OP);
        reservedMap.put("getBlue", IToken.Kind.COLOR_OP);
        reservedMap.put("getGreen", IToken.Kind.COLOR_OP);
        // <color_const>
        reservedMap.put("BLACK", IToken.Kind.COLOR_CONST);
        reservedMap.put("BLUE", IToken.Kind.COLOR_CONST);
        reservedMap.put("CYAN", IToken.Kind.COLOR_CONST);
        reservedMap.put("DARK_GRAY", IToken.Kind.COLOR_CONST);
        reservedMap.put("GRAY", IToken.Kind.COLOR_CONST);
        reservedMap.put("GREEN", IToken.Kind.COLOR_CONST);
        reservedMap.put("LIGHT_GRAY", IToken.Kind.COLOR_CONST);
        reservedMap.put("MAGENTA", IToken.Kind.COLOR_CONST);
        reservedMap.put("ORANGE", IToken.Kind.COLOR_CONST);
        reservedMap.put("PINK", IToken.Kind.COLOR_CONST);
        reservedMap.put("RED", IToken.Kind.COLOR_CONST);
        reservedMap.put("WHITE", IToken.Kind.COLOR_CONST);
        reservedMap.put("YELLOW", IToken.Kind.COLOR_CONST);
        // <boolean_lit>
        reservedMap.put("true", IToken.Kind.BOOLEAN_LIT);
        reservedMap.put("false", IToken.Kind.BOOLEAN_LIT);
        // <other_keyword>
        reservedMap.put("if", IToken.Kind.KW_IF);
        reservedMap.put("fi", IToken.Kind.KW_FI);
        reservedMap.put("else", IToken.Kind.KW_ELSE);
        reservedMap.put("write", IToken.Kind.KW_WRITE);
        reservedMap.put("console", IToken.Kind.KW_CONSOLE);


        loop:
        while (true) {
            char ch = chars[pos];
            if (state != null) {
                switch (state) {
                    case START -> {
                        switch (ch) {
                            case ' ', '\t', '\r' -> pos++;
                            case '\n' -> {
                                pos++;
                                lineNumber++;
                                startPos = pos;
                                lineBasePos = pos;
                            }
                            case '"' -> {
                                holdingToken += ch;
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                state = State.HAVE_QUOTE;
                            }
                            case '&' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.AND, "&", startPos, 1, lineNumber));
                            }
                            case ',' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.COMMA, ",", startPos, 1, lineNumber));
                            }
                            case '/' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.DIV, "/", startPos, 1, lineNumber));
                            }
                            case '(' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.LPAREN, "(", startPos, 1, lineNumber));
                            }
                            case '[' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.LSQUARE, "[", startPos, 1, lineNumber));
                            }
                            case '%' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.MOD, "%", startPos, 1, lineNumber));
                            }
                            case '|' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.OR, "|", startPos, 1, lineNumber));
                            }
                            case '+' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.PLUS, "+", startPos, 1, lineNumber));
                            }
                            case '^' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.RETURN, "^", startPos, 1, lineNumber));
                            }
                            case ')' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.RPAREN, ")", startPos, 1, lineNumber));
                            }
                            case ']' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.RSQUARE, "]", startPos, 1, lineNumber));
                            }
                            case ';' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.SEMI, ":", startPos, 1, lineNumber));
                            }
                            case '*' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.TIMES, "*", startPos, 1, lineNumber));
                            }
                            case '#' -> {
                                pos++;
                                state = State.IN_COMMENT;
                            }
                            case '=' -> {
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                holdingToken += ch;
                                state = State.HAVE_EQ;
                            }
                            case '-' -> {
                                pos++;
                                startPos = pos;
                                holdingToken += ch;
                                state = State.HAVE_MINUS;
                            }
                            case '<' -> {
                                pos++;
                                startPos = pos;
                                holdingToken += ch;
                                state = State.HAVE_LT;
                            }
                            case '>' -> {
                                pos++;
                                startPos = pos;
                                holdingToken += ch;
                                state = State.HAVE_GT;
                            }
                            case '!' -> {
                                pos++;
                                startPos = pos;
                                holdingToken += ch;
                                state = State.HAVE_BANG;
                            }
                            case '0' -> {
                                holdingToken += ch;
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                state = State.HAVE_ZERO;
                            }
                            case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                                holdingToken += ch;
                                checkPosition(lineNumber, startPos, pos);
                                pos++;
                                state = State.IN_NUM;
                            }
                            case '~' -> {
                                holdingTokens.add(new Token(IToken.Kind.EOF, "~", startPos, 1, lineNumber));
                                break loop;
                            }
                            default -> {
                                if (Character.isJavaIdentifierStart(ch)) {
                                    holdingToken += ch;
                                    checkPosition(lineNumber, startPos, pos);
                                    pos++;
                                    state = State.IN_IDENT;
                                } else {
                                    throw new LexicalException("Error! " + ch + " is not a legal character");
                                }
                            }
                        }
                    }
                    case IN_COMMENT -> {
                        switch (ch) {
                            case '\n' -> {
                                pos++;
                                lineNumber++;
                                startPos = pos;
                                state = State.START;
                            }
                            case '~' -> {
                                pos++;
                                state = State.START;
                            }
                            default -> pos++;
                        }
                    }
                    case HAVE_ZERO -> {
                        if (ch == '.') {
                            holdingToken += ch;
                            state = State.HAVE_DOT;
                            pos++;
                        } else {
                            holdingTokens.add(new Token(IToken.Kind.INT_LIT, "0", startPos, 1, lineNumber));
                            holdingToken = "";
                            state = State.START;
                        }
                    }
                    case HAVE_DOT -> {
                        switch (ch) {
                            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                                holdingToken += ch;
                                pos++;
                                state = State.IN_FLOAT;
                            }
                            default -> throw new LexicalException("Incorrect Token Sequence");
                        }
                    }
                    case HAVE_EQ -> {
                        if (ch == '=') {
                            holdingToken += ch;
                            pos++;
                            holdingTokens.add(new Token(IToken.Kind.EQUALS, holdingToken, startPos, 2, lineNumber));
                            holdingToken = "";
                            state = State.START;
                        } else {
                            holdingTokens.add(new Token(IToken.Kind.ASSIGN, holdingToken, startPos, 1, lineNumber));
                            holdingToken = "";
                            state = State.START;
                        }
                    }
                    case HAVE_QUOTE -> {
                        switch (ch) {
                            case '\\' -> {
                                holdingToken += ch;
                                pos++;
                                startPos++;
                                state = State.IN_STRING;
                            }
                            case '\"' -> {
                                holdingToken += "\" ";
                                holdingTokens.add(new Token(IToken.Kind.STRING_LIT, holdingToken, startPos, 1, lineNumber));
                                holdingToken = "";
                                pos++;
                                state = State.START;
                            }
                            default -> {
                                holdingToken += ch;
                                pos++;
                            }
                        }
                    }
                    case IN_STRING -> {
                        switch (ch) {
                            case 'b', 't', 'n', 'f', 'r', '"', '\\', '\'' -> {
                                holdingToken += ch;
                                pos++;
                                state = State.HAVE_QUOTE;
                            }
                            default -> throw new LexicalException("Incorrect Token Sequence");
                        }
                    }
                    case HAVE_MINUS -> {
                        startPos = pos - startPos;
                        switch (ch) {
                            case '>' -> {
                                holdingToken += ch;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.RARROW, holdingToken, startPos, 2, lineNumber));
                                holdingToken = "";
                                state = State.START;
                            }
                            default -> {
                                holdingTokens.add(new Token(IToken.Kind.MINUS, holdingToken, startPos, 1, lineNumber));
                                holdingToken = "";
                                state = State.START;
                            }
                        }
                    }
                    case HAVE_LT -> {
                        checkPosition(lineNumber, startPos, pos);
                        switch (ch) {
                            case '-' -> {
                                holdingToken += ch;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.LARROW, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                            case '<' -> {
                                holdingToken += ch;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.LANGLE, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                            case '=' -> {
                                holdingToken += ch;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.LE, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                            default -> {
                                holdingTokens.add(new Token(IToken.Kind.LT, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                        }

                    }
                    case HAVE_GT -> {
                        checkPosition(lineNumber, startPos, pos);
                        switch (ch) {
                            case '>' -> {
                                holdingToken += ch;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.RANGLE, holdingToken, startPos, 1, lineNumber));
                                holdingToken = "";
                                state = State.START;
                            }
                            case '=' -> {
                                holdingToken += ch;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.GE, holdingToken, startPos, 1, lineNumber));
                                holdingToken = "";
                                state = State.START;
                            }
                            default -> {
                                holdingTokens.add(new Token(IToken.Kind.GT, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                        }
                    }
                    case HAVE_BANG -> {
                        if (ch == '=') {
                            checkPosition(lineNumber, startPos, pos);
                            holdingToken += ch;
                            holdingTokens.add(new Token(IToken.Kind.NOT_EQUALS, holdingToken, startPos, 2, lineNumber));
                            pos++;
                            holdingToken = "";
                            state = State.START;
                        } else {
                            holdingTokens.add(new Token(IToken.Kind.BANG, holdingToken, startPos, 1, lineNumber));
                            holdingToken = "";
                            state = State.START;
                        }
                    }
                    case IN_NUM -> {
                        switch (ch) {
                            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                                holdingToken += ch;
                                pos++;
                            }
                            case '.' -> {
                                holdingToken += ch;
                                pos++;
                                state = State.HAVE_DOT;
                            }
                            default -> {
                                holdingTokens.add(new Token(IToken.Kind.INT_LIT, holdingToken, startPos, holdingToken.length(), lineNumber));
                                try {
                                    Integer.parseInt(holdingToken);
                                } catch (NumberFormatException e) {
                                    throw new LexicalException("Whoops, Integer is not valid");
                                }
                                holdingToken = "";
                                state = State.START;
                            }
                        }
                    }
                    case IN_IDENT -> {
                        if (Character.isJavaIdentifierPart(ch)) {
                            holdingToken += ch;
                            pos++;
                        } else {
                            holdingTokens.add(new Token(reservedMap.getOrDefault(holdingToken, IToken.Kind.IDENT), holdingToken, startPos, holdingToken.length(), lineNumber));
                            checkLinePosition(lineNumber, startPos, pos, lineBasePos);
                            holdingToken = "";
                            state = State.START;
                        }
                    }
                    case IN_FLOAT -> {
                        switch (ch) {
                            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                                holdingToken += ch;
                                pos++;
                            }
                            default -> {
                                holdingTokens.add(new Token(IToken.Kind.FLOAT_LIT, holdingToken, startPos, holdingToken.length(), lineNumber));
                                holdingToken = "";
                                state = State.START;
                            }
                        }
                    }
                    default -> throw new LexicalException("Something went wrong. Try again");
                }
            }
        }
    }

    void checkPosition(int lineNumber, int startPos, int pos) {
        if (lineNumber > 0) {
            this.startPos = pos - startPos;
        } else {
            this.startPos = pos;
        }
    }
    void checkLinePosition(int lineNumber, int startPos, int pos, int lineBasePos) {
        if (lineNumber > 0) {
            this.startPos = lineBasePos;
        } else {
            this.startPos = pos - startPos;
        }
    }

    @Override
    public IToken next() {
        IToken result = holdingTokens.get(arrayPos);
        arrayPos++;
        return result;
    }

    @Override
    public IToken peek() {
        return holdingTokens.get(arrayPos);
    }

}

