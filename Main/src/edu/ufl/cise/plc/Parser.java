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

    public Expr MultiplicativeExpr() throws SyntaxException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;

        left = UnaryExpr();
        IToken.Kind tempKind = firstToken.getKind();

        while (tempKind == IToken.Kind.MOD || tempKind == IToken.Kind.TIMES || tempKind == IToken.Kind.DIV){
            IToken op = t;
            consume();
            right = UnaryExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    public Expr UnaryExpr() throws SyntaxException {
        IToken firstToken = t;
        Expr x = null;
        IToken.Kind tempKind = firstToken.getKind();
        if(tempKind == IToken.Kind.COLOR_OP || tempKind == IToken.Kind.IMAGE_OP || tempKind == IToken.Kind.BANG || tempKind == IToken.Kind.MINUS){
            consume();
            x = UnaryExpr();
        }else{
            x = UnaryExprPostFix();
            return x;
        }
        return x;
    }

    public Expr UnaryExprPostFix() throws SyntaxException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;
        left = this.PrimaryExpr();
        consume();
        right = PixelSelector();
        return left;
    }

    public Expr PixelSelector() {
        IToken firstToken = t;
        Expr x = null;
        Expr y = null;

        if (firstToken.getKind() != IToken.Kind.LSQUARE) {
                return null;
        }else {
            consume();
            x = expr();
        }
        if (firstToken.getKind() == IToken.Kind.COMMA) {
                consume();
                y = expr();
                if (firstToken.getKind() == IToken.Kind.RSQUARE) {
                    consume();
                }
            }
        return x;
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













