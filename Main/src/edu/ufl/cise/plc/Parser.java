package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

public class Parser implements IParser {
    Lexer lex;
    IToken t;

    public Parser(String input) throws LexicalException {
            lex = new Lexer(input);
            t = lex.next();
    }




    @Override
    public ASTNode parse() throws PLCException {
        PrimaryExpr();
        return null;
    }

    public Expr expr(){
        return null;
    }

    // first grammar
    public Expr PrimaryExpr() throws SyntaxException {
        IToken firstToken = t;
        Expr e = null;
        if(firstToken.getKind() == IToken.Kind.BOOLEAN_LIT){
            e = new BooleanLitExpr(firstToken);
            consume();
        }else if(firstToken.getKind() == IToken.Kind.STRING_LIT){
            e = new StringLitExpr(firstToken);
            consume();
        }else if(firstToken.getKind() == IToken.Kind.INT_LIT){
            e = new IntLitExpr(firstToken);
            consume();
        }else if(firstToken.getKind() == IToken.Kind.FLOAT_LIT){
            e = new FloatLitExpr(firstToken);
            consume();
        }else if(firstToken.getKind() == IToken.Kind.IDENT){
            e = new IdentExpr(firstToken);
            consume();
        }else if(firstToken.getKind() == IToken.Kind.LPAREN){
            consume();
            e = expr();
            if(match(IToken.Kind.RPAREN)){
                consume();
            }else{
                error("Expected RPAREN");
            }
        }else{
            error("Expected");
        }
        return e;
    }

        void consume(){
                t = lex.next();
        }

        private boolean match(IToken.Kind kind){
            return t.getKind() == kind;
        }

        private void error(String m) throws SyntaxException {
            throw new SyntaxException(m);
        }
}













