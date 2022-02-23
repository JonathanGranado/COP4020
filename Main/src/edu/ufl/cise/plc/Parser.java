package edu.ufl.cise.plc;
import edu.ufl.cise.plc.ast.*;

import java.util.ArrayList;
import java.util.List;

public class Parser implements IParser {
    Lexer lex;
    IToken t;

    public Parser(String input) throws PLCException {
        lex = new Lexer(input);
        t = lex.next();
    }

    @Override
    public ASTNode parse() throws PLCException {
        return Program();
    }

    public Program Program() throws PLCException {
        IToken firstToken = t;
        Types.Type returnType = null;
        String name = null;
        List<NameDef> params = new ArrayList<NameDef>();
        List<ASTNode> decsAndStatements = new ArrayList<ASTNode>();
        NameDef left;
        NameDef right;
        Declaration decl;
        Statement state = null;

        if (t.getKind() == IToken.Kind.TYPE || t.getKind() == IToken.Kind.KW_VOID) {
            if (match(IToken.Kind.TYPE)) {
                returnType = Types.Type.toType(t.getText());
            }
            if (match(IToken.Kind.KW_VOID)) {
                returnType = Types.Type.toType("void");
            }
            consume();

            if (match(IToken.Kind.IDENT)) {
                name = t.getText();
                consume();
                if (match(IToken.Kind.LPAREN)) {
                    consume();
                    if (!match(IToken.Kind.RPAREN)) {
                        left = NameDef();
                        params.add(left);
                        consume();
                        while (t.getKind() == IToken.Kind.COMMA) {
                            consume();
                            right = NameDef();
                            params.add(right);
                            consume();
                        }
                        if(match(IToken.Kind.RPAREN)){
                            consume();
                            if(t.getText().equals("~")){
                                return new Program(firstToken, returnType, name, params, decsAndStatements);
                            }else{
                                decl = Declaration();
                                state = Statement();
                                while (decl != null || state != null) {
                                    decl = Declaration();
                                    state = Statement();
                                    decsAndStatements.add(decl);
                                    decsAndStatements.add(state);
                                    if (!match(IToken.Kind.SEMI)) {
                                        error("Error inside of Declaration or Statement *");
                                    }
                                }
                                return new Program(firstToken, returnType, name, params, decsAndStatements);
                            }
                        }
                    } else {
                        consume();
                        if (!t.getText().equals("~")) {
                            decl = Declaration();
                            state = Statement();
                            while (decl != null || state != null) {
                                decl = Declaration();
                                state = Statement();
                                decsAndStatements.add(decl);
                                decsAndStatements.add(state);
                                if (!match(IToken.Kind.SEMI)) {
                                    error("Error inside of Declaration or Statement *");
                                }
                            }
                            return new Program(firstToken, returnType, name, params, decsAndStatements);
                        }
                        return new Program(firstToken, returnType, name, params, decsAndStatements);
                    }
                }
            }
        }
        throw new SyntaxException("Something wrong");
    }

    public NameDef NameDef() throws PLCException {
        IToken firstToken = t;
        Dimension left;

        if(match(IToken.Kind.TYPE)){
            IToken kind = firstToken;
            consume();
            if(match(IToken.Kind.IDENT)){
                return new NameDef(firstToken, kind.getText(), t.getText());
            }else{
                left = Dimension();
                consume();
                if(match(IToken.Kind.IDENT)){
                    return new NameDefWithDim(firstToken, kind.getText(), t.getText(), left);
                }
            }
        }
        throw new SyntaxException("Error in NameDef");
    }

    public Declaration Declaration() throws PLCException {
        IToken firstToken = t;
        Expr right = null;
        NameDef left = null;

        left = NameDef();
        if(firstToken.getKind() == IToken.Kind.ASSIGN || firstToken.getKind() == IToken.Kind.LARROW){
            IToken op = firstToken;
            consume();
            right = expr();
            return new VarDeclaration(firstToken, left, op, right);
        }
        return new VarDeclaration(firstToken, left, null, null);
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
        Expr red = null;
        Expr green = null;
        Expr blue = null;
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
        } else if (firstToken.getKind() == IToken.Kind.COLOR_CONST) {
            e = new ColorConstExpr(firstToken);
        } else if (firstToken.getKind() == IToken.Kind.KW_CONSOLE) {
            e = new ConsoleExpr(firstToken);
        } else if (firstToken.getKind() == IToken.Kind.LPAREN) {
                consume();
                e = expr();
                if (!match(IToken.Kind.RPAREN)) error("Missing right parentheses");
        } else if (firstToken.getKind() == IToken.Kind.LANGLE) {
            consume();
            red = expr();
            if (match(IToken.Kind.COMMA)) consume();
            green = expr();
            if(match(IToken.Kind.COMMA)) consume();
            blue = expr();
            if(match(IToken.Kind.RANGLE)){
                return new ColorExpr(firstToken, red, green, blue);
            }else{
                error("Not a complete color expression");
            }
        } else {
            error("Incorrect Syntax");
        }
        return e;
    }


    public Dimension Dimension() throws PLCException {
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
        return new Dimension(firstToken, x, y);
    }

    public Statement Statement() throws PLCException {
        IToken firstToken = t;
        Expr x = null;
        Expr y = null;

        if (firstToken.getKind() != IToken.Kind.IDENT) {
            if (match(IToken.Kind.KW_WRITE)) {
                consume();
                x = expr();
                if (match(IToken.Kind.RARROW)) {
                    consume();
                    y = expr();
                }
                return new WriteStatement(firstToken, x, y);
            }
            if(match(IToken.Kind.RETURN)){
                consume();
                x = expr();
                return new ReturnStatement(firstToken, x);
            }else{
                throw new SyntaxException("This is not a valid statement");
            }
        }else{
            String name = firstToken.getText();
            consume();
            PixelSelector temp = this.PixelSelector();
            if(temp == null){
                if(match(IToken.Kind.EQUALS)){
                    consume();
                    x = expr();
                    return new AssignmentStatement(firstToken, name, null, x);
                }else if(match(IToken.Kind.LARROW)){
                    consume();
                    x = expr();
                    return new ReadStatement(firstToken, name, null, x);
                }
            }else{
                if(match(IToken.Kind.EQUALS)){
                    consume();
                    x = expr();
                    return new AssignmentStatement(firstToken, name, temp, x);
                }else if(match(IToken.Kind.LARROW)){
                    consume();
                    x = expr();
                    return new ReadStatement(firstToken, name, temp, x);
                }
            }
        }
        throw new SyntaxException("Incorrect statement");
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