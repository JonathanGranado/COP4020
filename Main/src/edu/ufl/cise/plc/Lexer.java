package edu.ufl.cise.plc;

public class Lexer implements ILexer  {

    char[] chars;
    public static enum State{
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
    }

    @Override
    public IToken next() throws LexicalException {
        return null;
    }

    @Override
    public IToken peek() throws LexicalException {
        return null;
    }
}
