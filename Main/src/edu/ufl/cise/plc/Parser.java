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
        Expr e = expr();
        return e;
    }

    public Expr expr() throws SyntaxException {
        Expr left = ConditionalExpr();
        Expr right = LogicalOrExpr();
        return right;
    }

    public Expr ConditionalExpr() throws SyntaxException {
        // this is confusing :(
       return null;
    }


    public Expr LogicalOrExpr() throws SyntaxException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;

        left = LogicalAndExpr();
        while (t.getKind() == IToken.Kind.OR){
            IToken op = t;
            consume();
            right = LogicalAndExpr();

            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    public Expr LogicalAndExpr() throws SyntaxException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;

        left = ComparisonExpr();
        while (t.getKind() == IToken.Kind.AND){
            IToken op = t;
            consume();
            right = ComparisonExpr();

            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    public Expr ComparisonExpr() throws SyntaxException {
        IToken firstToken = t;
        Expr left;
        Expr right;

        left = AdditiveExpr();
        while( t.getKind() == IToken.Kind.LT || t.getKind() == IToken.Kind.GT || t.getKind() == IToken.Kind.EQUALS ||
                t.getKind() == IToken.Kind.NOT_EQUALS || t.getKind() == IToken.Kind.LE || t.getKind() == IToken.Kind.GE ){
            IToken op = t;
            consume();
            right = AdditiveExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    public Expr AdditiveExpr() throws SyntaxException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;

        left = MultiplicativeExpr();
        while (t.getKind() == IToken.Kind.PLUS || t.getKind() == IToken.Kind.MINUS){
            IToken op = t;
            consume();
            right = MultiplicativeExpr();

            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    public Expr MultiplicativeExpr() throws SyntaxException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;

        left = UnaryExpr();

        while (t.getKind() == IToken.Kind.MOD || t.getKind() == IToken.Kind.TIMES || t.getKind() == IToken.Kind.DIV){
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
            Expr e =  UnaryExpr();
            x = new UnaryExpr(firstToken, firstToken, e);
        }else{
            x = UnaryExprPostFix();
            return x;
        }
        return x;
    }

    public Expr UnaryExprPostFix() throws SyntaxException {
        IToken firstToken = t;
        Expr left = null;
        PixelSelector right = null;
        left = this.PrimaryExpr();
        consume();
        right = this.PixelSelector();
        // if it has no pixel selector and right is null, we just return left
        if (right == null){
            return left;
        }
        // if it has a Pixel Selector we create an object so we can return
        // both PrimaryExpr and PixelSelector
        return new UnaryExprPostfix(firstToken, left, right);
    }

    public PixelSelector PixelSelector() throws SyntaxException {
        IToken firstToken = t;
        Expr x = null;
        Expr y = null;

        if (firstToken.getKind() != IToken.Kind.LSQUARE) {
                return null;
        }else {
            consume();
            x = AdditiveExpr();
        }
        //TODO: I changed the firstToken here to t so that the change from consume actually
        // does something, maybe we should use match
        if (t.getKind() == IToken.Kind.COMMA) {
                consume();
                y = AdditiveExpr();
                if (t.getKind() == IToken.Kind.RSQUARE) {
                    consume();
                }
            }
        return new PixelSelector(firstToken, x, y);
    }
    // first grammar

    public Expr PrimaryExpr() throws SyntaxException {
        IToken firstToken = t;
        Expr e = null;
        if(firstToken.getKind() == IToken.Kind.STRING_LIT){
            e = new StringLitExpr(firstToken);
            // consume();
            //TODO: Breaks here, test 6
        }else if(firstToken.getKind() == IToken.Kind.BOOLEAN_LIT){
            e = new BooleanLitExpr(firstToken);
            //consume();
        }else if(firstToken.getKind() == IToken.Kind.INT_LIT){
            e = new IntLitExpr(firstToken);
           // consume();
        }else if(firstToken.getKind() == IToken.Kind.FLOAT_LIT){
            e = new FloatLitExpr(firstToken);
           // consume();
        }else if(firstToken.getKind() == IToken.Kind.IDENT){
            e = new IdentExpr(firstToken);
          //  consume();
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













