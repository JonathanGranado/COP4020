package edu.ufl.cise.plc;

public class Lexer implements ILexer  {

    char[] chars;
    public enum State{
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



    public Lexer(String input){
        chars = new char[input.length()];
        for(int i = 0; i < input.length(); i++){
            chars[i] = input.charAt(i);
        }
    }

    @Override
    public IToken next() throws LexicalException {
        return null;
    }

    @Override
    public IToken peek() throws LexicalException {
        return null;
    }

    public void readingChars(State state) throws LexicalException {
        int pos = 0;
        while(true){
            char ch = chars[pos];
            switch(state){
                case START -> {
                    // single characters, and white space characters, just missing the commment
                    int startPos = pos;
                    switch (ch){
                        case ' ', '\t','\n', '\r' -> {pos++;}
                        case '&' -> {   new Token(IToken.Kind.AND, "&", startPos, 1);       pos++;}
                        case ',' -> {   new Token(IToken.Kind.COMMA, ",", startPos, 1);     pos++;}
                        case '/' -> {   new Token(IToken.Kind.DIV, "/", startPos, 1);       pos++;}
                        case '(' -> {   new Token(IToken.Kind.LPAREN, "(", startPos, 1);    pos++;}
                        case '[' -> {   new Token(IToken.Kind.LSQUARE, "[", startPos, 1);   pos++;}
                        case '%' -> {   new Token(IToken.Kind.MOD, "%", startPos, 1);       pos++;}
                        case '|' -> {   new Token(IToken.Kind.OR, "|", startPos, 1);        pos++;}
                        case '+' -> {   new Token(IToken.Kind.PLUS, "+", startPos, 1);      pos++;}
                        case '^' -> {   new Token(IToken.Kind.RETURN, "^", startPos, 1);    pos++;}
                        case ')' -> {   new Token(IToken.Kind.RPAREN, ")", startPos, 1);    pos++;}
                        case ']' -> {   new Token(IToken.Kind.RSQUARE, "]", startPos, 1);   pos++;}
                        case ':' -> {   new Token(IToken.Kind.SEMI, ":", startPos, 1);      pos++;}
                        case '*' -> {   new Token(IToken.Kind.TIMES, "*", startPos, 1);     pos++;}
                        case '=' -> { state = State.HAVE_EQ; pos++;}
                        case '-' -> { state = State.HAVE_MINUS; pos++;}
                        case '<' -> { state = State.HAVE_LT; pos++;}
                        case '>' -> { state = State.HAVE_GT; pos++;}
                        case '!' -> { state = State.HAVE_BANG; pos++;}
                        case 0 -> {new Token(IToken.Kind.EOF, " ", startPos, 1);}
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
                    int startPos = pos;
                    switch(ch){
                        case '=' -> { new Token(IToken.Kind.EQUALS, "=", startPos, 2); pos++;}
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
                    int startPos = 0;
                    switch(ch) {
                        case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {pos++;}
                        default -> {
                            new Token(IToken.Kind.INT_LIT, String.valueOf(ch), startPos, pos - startPos);
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
