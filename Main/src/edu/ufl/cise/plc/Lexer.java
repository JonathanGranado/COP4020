package edu.ufl.cise.plc;

import com.sun.jdi.IntegerValue;
import org.hamcrest.internal.ArrayIterator;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
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
    IN_COMMENT,
    IN_NUM,
    IN_IDENT,
    IN_FLOAT,
    IN_STRING
}

public class Lexer implements ILexer {

    public ArrayList<IToken> holdingTokens = new ArrayList<>();
    char[] chars;
    State state = State.START;
    int arrayPos = 0;


    public Lexer(String input) throws LexicalException {
        String holdingToken = "";

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
        int lineBasePos = 0;
        loop:
        while (true) {
            char ch = chars[pos];
            if (state != null) {
                System.out.println("Pos: " + pos);
                switch (state) {
                    case START -> {
                        switch (ch) {
                            // implement comment
                            case ' ', '\t', '\r' -> {
                                pos++;
                            }
                            case '\n' -> {
                                pos++;
                                lineNumber++;
                                startPos = pos;
                                lineBasePos = pos;
                                System.out.println("startPos recalculated to " + startPos + " by assigning due to newline");
                            }
                            case '"' -> {
                                state = State.HAVE_QUOTE;
                                startPos = pos;
                                pos++;
                            }
                            case '&' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.AND, "&", startPos, 1, lineNumber));
                                pos++;
                            }
                            case ',' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.COMMA, ",", startPos, 1, lineNumber));
                                pos++;
                            }
                            case '/' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.DIV, "/", startPos, 1, lineNumber));
                                pos++;
                            }
                            case '(' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.LPAREN, "(", startPos, 1, lineNumber));
                                pos++;
                            }
                            case '[' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.LSQUARE, "[", startPos, 1, lineNumber));
                                pos++;
                            }
                            case '%' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.MOD, "%", startPos, 1, lineNumber));
                                pos++;
                            }
                            case '|' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.OR, "|", startPos, 1, lineNumber));
                                pos++;
                            }
                            case '+' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.PLUS, "+", startPos, 1, lineNumber));
                                pos++;
                              //  startPos++;
                            }
                            case '^' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.RETURN, "^", startPos, 1, lineNumber));
                                pos++;
                            }
                            case ')' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.RPAREN, ")", startPos, 1, lineNumber));
                                pos++;
                            }
                            case ']' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.RSQUARE, "]", startPos, 1, lineNumber));
                                pos++;
                            }
                            case ':' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.SEMI, ":", startPos, 1, lineNumber));
                                pos++;
                            }
                            case '*' -> {
                                startPos = pos - startPos;
                                holdingTokens.add(new Token(IToken.Kind.TIMES, "*", startPos, 1, lineNumber));
                                pos++;
                            }
                            case '#' -> {
                                state = State.IN_COMMENT;
                                pos++;
                            }
                            case '=' -> {
                                state = State.HAVE_EQ;
                                startPos = pos;
                                pos++;
                                holdingToken += ch;
                            }
                            case '-' -> {
                                state = State.HAVE_MINUS;
                                pos++;
                                startPos = pos;
                                holdingToken += ch;
                            }
                            case '<' -> {
                                state = State.HAVE_LT;
                                pos++;
                                startPos = pos;
                                holdingToken += ch;
                            }
                            case '>' -> {
                                state = State.HAVE_GT;
                                pos++;
                                startPos = pos;
                                holdingToken += ch;
                            }
                            case '!' -> {
                                state = State.HAVE_BANG;
                                pos++;
                                startPos = pos;
                                holdingToken += ch;
                            }
                            case '0' -> {
                                holdingToken += ch;
                                startPos = pos - startPos;
                                pos++;
                                state = State.HAVE_ZERO;
                                // add test case
                            }
                            case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                                holdingToken += ch;
                                startPos = pos;
                                // TODO: add if statement like in default
                                System.out.println("startPos recalculated to " + startPos + " by assigning in num start");
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
                                    if(lineNumber > 0) {
                                        startPos = pos - startPos;
                                        System.out.println("startPos recalculated to " + startPos + " by subtracting in iden start");
                                    }else {
                                        startPos = pos; // passes IDenINT
                                        System.out.println("startPos recalculated to " + startPos + " by assigning in iden start");
                                    }
                                    pos++;
                                    state = State.IN_IDENT;
                                } else {
                                    throw new LexicalException("Error! " + ch + " is not a legal character");
                                }
                            }
                        }
                    }
                    case IN_COMMENT -> {
                        switch (ch){
                            case '\r' -> {
                                pos++;
                            }
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
                    // leads to either a int of just 0 or float if it has . then another digit
                    case HAVE_ZERO -> {
                        switch (ch){
                        case '.' -> {
                            holdingToken += ch;
                            state = State.HAVE_DOT;
                            pos++;
                            }
                            default -> {
                                holdingTokens.add(new Token(IToken.Kind.INT_LIT, "0", startPos, 1, lineNumber));
                                holdingToken = "";
                                state = State.START;
                            }
                        }
                    }

                    // 0
                    // check if it has case of digit, if not throw lexicalException
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
                    // if the next character is not another = it is an error
                    case HAVE_EQ -> {
                        switch (ch) {
                            case '=' -> {
                                holdingToken += ch;
                                holdingTokens.add(new Token(IToken.Kind.EQUALS, holdingToken, startPos, 2, lineNumber));
                                holdingToken = "";
                                pos++;
                                state = State.START;
                            }
                            default -> {
                                holdingTokens.add(new Token(IToken.Kind.ASSIGN, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                        }
                    }
                    // start of string lit
                    case HAVE_QUOTE -> {
                        switch (ch){
                            case '\'' -> {
                                holdingToken += ch;
                                state = State.IN_STRING;
                            pos++;
                            startPos++;
                            }
                            case '\"' ->  {
                                holdingToken += " \" ";
                                holdingTokens.add(new Token(IToken.Kind.STRING_LIT, holdingToken, startPos, 1, lineNumber));
                                holdingToken = "";
                                state = State.START;
                                pos++;
                            }
                            default -> {
                                holdingToken += ch;
                                pos++;
                            }
                        }
                    }

                    case IN_STRING -> {
                        switch (ch){
                            case 'b', 't', 'n', 'f', 'r', '"', '\\' , '\'' -> {
                                holdingToken += ch;
                                pos++;
                                state = State.HAVE_QUOTE;
                            }
                            default -> throw new LexicalException("Incorrect Token Sequence");
                        }
                    }

                    // can have ->, or just -
                    case HAVE_MINUS -> {
                        switch (ch){
                            case '>' -> {
                                holdingToken += ch;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.RARROW, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                            default -> {
                                startPos = pos - startPos;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.MINUS, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                        }
                    }
                    // can have <, or <-, or <=
                    case HAVE_LT -> {
                        switch (ch){
                            case '-' -> {
                                holdingToken += ch;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.LARROW, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                            case '=' -> {
                                holdingToken += ch;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.LE, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                            default -> {
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.LT, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                        }

                    }
                    // can have >, or >>, or >=
                    case HAVE_GT -> {
                        switch (ch){
                            case '>' -> {
                                holdingToken += ch;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.RANGLE, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                            case '=' -> {
                                holdingToken += ch;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.GE, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                            default -> {
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.GT, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                        }
                    }
                    // can just have !, or !=
                    case HAVE_BANG -> {
                        switch (ch){
                            case '=' -> {
                                holdingToken += ch;
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.NOT_EQUALS, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
                            default -> {
                                pos++;
                                holdingTokens.add(new Token(IToken.Kind.BANG, holdingToken, startPos, 1, lineNumber));
                                state = State.START;
                            }
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
                                System.out.println("Creating new  token\n"+holdingToken+" Start = "+ startPos + " Pos = " + pos);

                                holdingTokens.add(new Token(IToken.Kind.INT_LIT, holdingToken, startPos, holdingToken.length(), lineNumber));
                                try {
                                    Integer.parseInt(holdingToken);
                                }catch(NumberFormatException e){
                                    throw new LexicalException("Whoops, Integer is not valid");
                                }
                                holdingToken = "";
                                state = State.START;
                            }
                        }
                    }
                    // if it has letter, $, or _, then stays (by increasing pos++) if it also has 0..9
                    case IN_IDENT -> {
                            if(Character.isJavaIdentifierPart(ch)){
                                holdingToken += ch;
                                pos++;
                            }else{
                                holdingTokens.add(new Token(IToken.Kind.IDENT, holdingToken, startPos, holdingToken.length(), lineNumber));
                                System.out.println("Creating new  token\n"+holdingToken+" Start = "+ startPos + " Pos = " + pos);
                                if(lineNumber >0){
                                    startPos = lineBasePos;
                                } else{
                                    startPos = pos - startPos;
                                    System.out.println("startPos recalculated to " + startPos + " by substracting");
                                }

                                holdingToken = "";
                                state = State.START;
                            }
                    }

                    // in float after int lit -> have dot and then has a digit
                    case IN_FLOAT -> {
                        switch (ch){
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

