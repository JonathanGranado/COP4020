package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

public class Parser implements IParser {
    Lexer lex;
    IToken t;

    public Parser(String input) throws PLCException {
        lex = new Lexer(input);
        t = lex.next();
    }


    @Override
    public ASTNode parse() throws PLCException {
        return expr();
    }

    public Expr expr() throws PLCException {
        return ConditionalExpr();
    }

    public Expr ConditionalExpr() throws PLCException {
        IToken firstToken = t;
        Expr condition;
        Expr trueCase;
        Expr falseCase;
        Expr result = null;
        if (match(IToken.Kind.KW_IF)) {
            consume();
            if (match(IToken.Kind.LPAREN)) {
                consume();
                condition = expr();
                if (match(IToken.Kind.RPAREN)) {
                    consume();
                    trueCase = expr();
                    if (match(IToken.Kind.KW_ELSE)) {
                        consume();
                        falseCase = expr();
                        if (match(IToken.Kind.KW_FI)) {
                            consume();
                            result = new ConditionalExpr(firstToken, condition, trueCase, falseCase);
                        } else {
                            error("Was expecting a FI");
                        }
                    } else {
                        error("Was expecting an Else");
                    }
                } else {
                    error("Was expecting a Right Paren");
                }
            } else {
                error("Was expecting a Left Paren");
            }
        } else {
            // TODO: error handling
            return LogicalOrExpr();
        }
        return result;
    }


    public Expr LogicalOrExpr() throws PLCException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;

        left = LogicalAndExpr();
        while (t.getKind() == IToken.Kind.OR) {
            IToken op = t;
            consume();
            right = LogicalAndExpr();

            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    public Expr LogicalAndExpr() throws PLCException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;

        left = ComparisonExpr();
        while (t.getKind() == IToken.Kind.AND) {
            IToken op = t;
            consume();
            right = ComparisonExpr();

            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    public Expr ComparisonExpr() throws PLCException {
        IToken firstToken = t;
        Expr left;
        Expr right;

        left = AdditiveExpr();
        while (t.getKind() == IToken.Kind.LT || t.getKind() == IToken.Kind.GT || t.getKind() == IToken.Kind.EQUALS ||
                t.getKind() == IToken.Kind.NOT_EQUALS || t.getKind() == IToken.Kind.LE || t.getKind() == IToken.Kind.GE) {
            IToken op = t;
            consume();
            right = AdditiveExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    public Expr AdditiveExpr() throws PLCException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;

        left = MultiplicativeExpr();
        while (t.getKind() == IToken.Kind.PLUS || t.getKind() == IToken.Kind.MINUS) {
            IToken op = t;
            consume();
            right = MultiplicativeExpr();

            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    public Expr MultiplicativeExpr() throws PLCException {
        IToken firstToken = t;
        Expr left = null;
        Expr right = null;

        left = UnaryExpr();

        while (t.getKind() == IToken.Kind.MOD || t.getKind() == IToken.Kind.TIMES || t.getKind() == IToken.Kind.DIV) {
            IToken op = t;
            consume();
            right = UnaryExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    public Expr UnaryExpr() throws PLCException {
        IToken firstToken = t;
        Expr x = null;
        IToken.Kind tempKind = firstToken.getKind();
        if (tempKind == IToken.Kind.COLOR_OP || tempKind == IToken.Kind.IMAGE_OP || tempKind == IToken.Kind.BANG || tempKind == IToken.Kind.MINUS) {
            consume();
            Expr e = UnaryExpr();
            x = new UnaryExpr(firstToken, firstToken, e);
        } else {
            x = UnaryExprPostFix();
            return x;
        }
        return x;
    }

    public Expr UnaryExprPostFix() throws PLCException {
        IToken firstToken = t;
        Expr left = null;
        PixelSelector right = null;
        left = this.PrimaryExpr();
        consume();
        right = this.PixelSelector();
        if (right == null) {
            return left;
        }
        return new UnaryExprPostfix(firstToken, left, right);
    }

    public PixelSelector PixelSelector() throws PLCException {
        IToken firstToken = t;
        Expr x = null;
        Expr y = null;

        if (firstToken.getKind() != IToken.Kind.LSQUARE) {
            return null;
        } else {
            consume();
            x = AdditiveExpr();
        }
        if (t.getKind() == IToken.Kind.COMMA) {
            consume();
            y = AdditiveExpr();
            if (t.getKind() == IToken.Kind.RSQUARE) {
                consume();
            }
        }
        return new PixelSelector(firstToken, x, y);
    }

    public Expr PrimaryExpr() throws PLCException {
        IToken firstToken = t;
        Expr e = null;
        if (firstToken.getKind() == IToken.Kind.STRING_LIT) {
            e = new StringLitExpr(firstToken);
        } else if (firstToken.getKind() == IToken.Kind.BOOLEAN_LIT) {
            e = new BooleanLitExpr(firstToken);
        } else if (firstToken.getKind() == IToken.Kind.INT_LIT) {
            e = new IntLitExpr(firstToken);
        } else if (firstToken.getKind() == IToken.Kind.FLOAT_LIT) {
            e = new FloatLitExpr(firstToken);
        } else if (firstToken.getKind() == IToken.Kind.IDENT) {
            e = new IdentExpr(firstToken);
        } else if (firstToken.getKind() == IToken.Kind.LPAREN) {
            consume();
            e = expr();
            if (!match(IToken.Kind.RPAREN)) error("Missing right parentheses");
        } else {
            error("Incorrect Syntax, missing the rest of if else statement");
        }
        return e;
    }


    void consume() {
        t = lex.next();
    }

    private boolean match(IToken.Kind kind) {
        return t.getKind() == kind;
    }

    private void error(String m) throws PLCException {
        throw new SyntaxException(m);
    }
}













