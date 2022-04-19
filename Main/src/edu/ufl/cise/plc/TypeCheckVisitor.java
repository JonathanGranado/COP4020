package edu.ufl.cise.plc;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.ast.Types.Type;
import edu.ufl.cise.plc.runtime.*;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static edu.ufl.cise.plc.ast.Types.Type.*;
import static edu.ufl.cise.plc.runtime.FileURLIO.readImage;

public class TypeCheckVisitor implements ASTVisitor {

    SymbolTable symbolTable = new SymbolTable();
    Program root;
    //Maps forms a lookup table that maps an operator expression pair into result type.
    //This more convenient than a long chain of if-else statements.
    //Given combinations are legal; if the operator expression pair is not in the map, it is an error.
    Map<Pair<Kind, Type>, Type> unaryExprs = Map.of(
            new Pair<Kind, Type>(Kind.BANG, BOOLEAN), BOOLEAN,
            new Pair<Kind, Type>(Kind.MINUS, FLOAT), FLOAT,
            new Pair<Kind, Type>(Kind.MINUS, INT), INT,
            new Pair<Kind, Type>(Kind.COLOR_OP, INT), INT,
            new Pair<Kind, Type>(Kind.COLOR_OP, COLOR), INT,
            new Pair<Kind, Type>(Kind.COLOR_OP, IMAGE), IMAGE,
            new Pair<Kind, Type>(Kind.IMAGE_OP, IMAGE), INT
    );

    private boolean assignmentCompatible(Type targetType, Type rhsType) {
        return (targetType == rhsType
                || targetType == INT && rhsType == FLOAT
                || targetType == FLOAT && rhsType == INT
                || targetType == INT && rhsType == COLOR
                || targetType == COLOR && rhsType == INT
                || targetType == IMAGE && rhsType == INT
                || targetType == IMAGE && rhsType == FLOAT
                || targetType == IMAGE && rhsType == COLOR
                || targetType == IMAGE && rhsType == COLORFLOAT
        );
    }

    //may be useful for constructing lookup tables.

    private void check(boolean condition, ASTNode node, String message) throws TypeCheckException {
        if (!condition) {
            throw new TypeCheckException(message, node.getSourceLoc());
        }
    }

    //The type of a BooleanLitExpr is always BOOLEAN.
    //Set the type in AST Node for later passes (code generation)
    //Return the type for convenience in this visitor.
    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
        booleanLitExpr.setType(Type.BOOLEAN);
        return Type.BOOLEAN;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
        stringLitExpr.setType(Type.STRING);
        return Type.STRING;
    }

    @Override
    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
        intLitExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
        floatLitExpr.setType(Type.FLOAT);
        return Type.FLOAT;
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
        colorConstExpr.setType(Type.COLOR);
        return Type.COLOR;
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        consoleExpr.setType(Type.CONSOLE);
        return Type.CONSOLE;
    }

    //Visits the child expressions to get their type (and ensure they are correctly typed)
    //then checks the given conditions.
    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        Type redType = (Type) colorExpr.getRed().visit(this, arg);
        Type greenType = (Type) colorExpr.getGreen().visit(this, arg);
        Type blueType = (Type) colorExpr.getBlue().visit(this, arg);
        check(redType == greenType && redType == blueType, colorExpr, "color components must have same type");
        check(redType == Type.INT || redType == Type.FLOAT, colorExpr, "color component type must be int or float");
        Type exprType = (redType == Type.INT) ? Type.COLOR : Type.COLORFLOAT;
        colorExpr.setType(exprType);
        return exprType;
    }

    //Visits the child expression to get the type, then uses the above table to determine the result type
    //and check that this node represents a legal combination of operator and expression type.
    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
        // !, -, getRed, getGreen, getBlue
        Kind op = unaryExpr.getOp().getKind();
        Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);
        //Use the lookup table above to both check for a legal combination of operator and expression, and to get result type.
        Type resultType = unaryExprs.get(new Pair<Kind, Type>(op, exprType));
        check(resultType != null, unaryExpr, "incompatible types for unaryExpr");
        //Save the type of the unary expression in the AST node for use in code generation later.
        unaryExpr.setType(resultType);
        //return the type for convenience in this visitor.
        return resultType;
    }

    //This method has several cases. Work incrementally and test as you go.
    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        Kind op = binaryExpr.getOp().getKind();
        Type leftType = (Type) binaryExpr.getLeft().visit(this, arg);
        Type rightType = (Type) binaryExpr.getRight().visit(this, arg);
        Type resultType = null;

        switch (op) {
            case OR, AND -> {
                check(leftType == BOOLEAN && rightType == BOOLEAN, binaryExpr, "both sides must be BOOLEAN");
                resultType = BOOLEAN;
            }
            case EQUALS, NOT_EQUALS -> {
                check(leftType == rightType, binaryExpr, "incompatible types");
                resultType = BOOLEAN;
            }
            case PLUS, MINUS -> {
                if (leftType == INT && rightType == INT) resultType = INT;
                else if ((leftType == INT || leftType == FLOAT) && (rightType == FLOAT || rightType == INT)) {
                    if (leftType == INT) binaryExpr.getLeft().setCoerceTo(FLOAT);
                    if (rightType == INT) binaryExpr.getRight().setCoerceTo(FLOAT);
                    resultType = FLOAT;
                } else if (leftType == COLOR && rightType == COLOR) resultType = COLOR;
                else if ((leftType == COLORFLOAT || leftType == COLOR) && (rightType == COLORFLOAT || rightType == COLOR)) {
                    if (leftType == COLOR) binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
                    if (rightType == COLOR) binaryExpr.getRight().setCoerceTo(COLORFLOAT);
                    resultType = COLORFLOAT;
                } else if (leftType == IMAGE && rightType == IMAGE) resultType = IMAGE;
                else check(false, binaryExpr, "incompatible types for operator");
            }
            case TIMES, DIV, MOD -> {
                if (leftType == INT && rightType == INT) resultType = INT;
                else if ((leftType == INT || leftType == FLOAT) && (rightType == FLOAT || rightType == INT)) {
                    if (leftType == INT) binaryExpr.getLeft().setCoerceTo(FLOAT);
                    if (rightType == INT) binaryExpr.getRight().setCoerceTo(FLOAT);
                    resultType = FLOAT;
                } else if (leftType == COLOR && rightType == COLOR) resultType = COLOR;
                else if ((leftType == COLORFLOAT || leftType == COLOR) && (rightType == COLORFLOAT || rightType == COLOR)) {
                    if (leftType == COLOR) binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
                    if (rightType == COLOR) binaryExpr.getRight().setCoerceTo(COLORFLOAT);
                    resultType = COLORFLOAT;
                } else if (leftType == IMAGE && rightType == IMAGE) resultType = IMAGE;
                else if (leftType == IMAGE && rightType == INT) resultType = IMAGE;
                else if ((leftType == INT || leftType == COLOR) && (rightType == INT || rightType == COLOR)) {
                    if (leftType == INT) binaryExpr.getLeft().setCoerceTo(COLOR);
                    if (rightType == INT) binaryExpr.getRight().setCoerceTo(COLOR);
                    resultType = COLOR;
                } else if ((leftType == FLOAT || leftType == COLOR) && (rightType == FLOAT || rightType == COLOR)) {
                    binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
                    binaryExpr.getRight().setCoerceTo(COLORFLOAT);
                    resultType = COLORFLOAT;
                } else check(false, binaryExpr, "incompatible types for operator");
            }
            case GE, LT, GT, LE -> {
                if (leftType == INT && rightType == INT) resultType = BOOLEAN;
                else if ((leftType == INT || leftType == FLOAT) && (rightType == FLOAT || rightType == INT))
                    resultType = BOOLEAN;
                else check(false, binaryExpr, "incompatible types for operator");
            }
            default -> throw new Exception("compiler error in visitBinaryExpr");
        }
        binaryExpr.setType(resultType);
        return resultType;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        //TODO:Review
        String name = identExpr.getText();
        Declaration dec = symbolTable.lookup(name);
        check(dec != null, identExpr, "undefined identifier " + name);
        identExpr.setDec(dec);
        Type type = dec.getType();
        identExpr.setType(type);
        return type;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        Type condition = (Type) conditionalExpr.getCondition().visit(this, arg);
        Type trueCase = (Type) conditionalExpr.getTrueCase().visit(this, arg);
        Type falseCase = (Type) conditionalExpr.getFalseCase().visit(this, arg);

        check(condition == BOOLEAN, conditionalExpr, "type of condition must be boolean");
        check(trueCase == falseCase, conditionalExpr, "type of true case must be equal to false case");
        conditionalExpr.setType(trueCase);
        return trueCase;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        Type left = (Type) dimension.getHeight().visit(this, arg);
        Type right = (Type) dimension.getWidth().visit(this, arg);
        check(left == Type.INT && right == Type.INT, dimension, "both expressions must have type INT");
        return left;
    }

    @Override
    //This method can only be used to check PixelSelector objects on the right hand side of an assignment.
    //Either modify to pass in context info and add code to handle both cases, or when on left side
    //of assignment, check fields from parent assignment statement.
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        Type xType = (Type) pixelSelector.getX().visit(this, arg);
        check(xType == Type.INT, pixelSelector.getX(), "only ints as pixel selector components");
        Type yType = (Type) pixelSelector.getY().visit(this, arg);
        check(yType == Type.INT, pixelSelector.getY(), "only ints as pixel selector components");
        return null;
    }

    @Override
    //This method several cases--you don't have to implement them all at once.
    //Work incrementally and systematically, testing as you go.
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        // look up name in symbol table, check that variable is declared
        String name = assignmentStatement.getName();
        Declaration lhsDeclaration = symbolTable.lookup(name);
        check(lhsDeclaration != null, assignmentStatement, "undeclared variable " + name);
        // get types

        Type lhsType = lhsDeclaration.getType();

        lhsDeclaration.setInitialized(true);


        boolean hasSelector = assignmentStatement.getSelector() != null;
        if (lhsType != IMAGE) {
            check(!hasSelector, assignmentStatement, "Cannot have pixel selector if is not type IMAGE");
            check(assignmentCompatible(lhsType, (Type) assignmentStatement.getExpr().visit(this, arg)), assignmentStatement, "incompatible types in assignment");
            assignmentStatement.getExpr().setCoerceTo(lhsType);

        } else if (!hasSelector) {
            check(assignmentCompatible(lhsType, (Type) assignmentStatement.getExpr().visit(this, arg)), assignmentStatement, "incompatible types in assignment");
            if ((Type) assignmentStatement.getExpr().visit(this, arg) == INT)
                assignmentStatement.getExpr().setCoerceTo(COLOR);
            if ((Type) assignmentStatement.getExpr().visit(this, arg) == FLOAT)
                assignmentStatement.getExpr().setCoerceTo(COLORFLOAT);

        } else {

            PixelSelector selector = assignmentStatement.getSelector();
            Expr x = selector.getX();
            Expr y = selector.getY();
            check(x.getClass() == IdentExpr.class, assignmentStatement, "x is not type identExpr");
            check(y.getClass() == IdentExpr.class, assignmentStatement, "y is not type identExpr");
            check(symbolTable.lookup(x.getText()) == null, assignmentStatement, "x is already declared");
            check(symbolTable.lookup(y.getText()) == null, assignmentStatement, "y is already declared");
//            x.setType();
            // declare and initialize local variables
            NameDef xDec = new NameDef(x.getFirstToken(), "int", x.getText());
            NameDef yDec = new NameDef(y.getFirstToken(), "int", y.getText());
            symbolTable.insert(x.getText(), xDec);
            symbolTable.insert(y.getText(), yDec);
            visitIdentExpr((IdentExpr) x, arg);
            visitIdentExpr((IdentExpr) y, arg);
            //symbolTable.insert(x.getText(), ((IdentExpr) x).getDec());
            //symbolTable.insert(y.getText(), ((IdentExpr) y).getDec());
            symbolTable.lookup(x.getText()).setInitialized(true);
            symbolTable.lookup(y.getText()).setInitialized(true);

            check(assignmentCompatible(lhsType, (Type) assignmentStatement.getExpr().visit(this, arg)), assignmentStatement, "incompatible rhs type in assignment");
            assignmentStatement.getExpr().setCoerceTo(COLOR);
            // remove local variables
            symbolTable.lookup(x.getText()).setInitialized(false);
            symbolTable.lookup(y.getText()).setInitialized(false);
            symbolTable.table.remove(x.getText());
            symbolTable.table.remove(y.getText());
        }
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
       // left is source right is dest
        Type sourceType = (Type) writeStatement.getSource().visit(this, arg);
        Type destType = (Type) writeStatement.getDest().visit(this, arg);
//        writeStatement.getDest().setCoerceTo(sourceType);
        check(destType == Type.STRING || destType == Type.CONSOLE, writeStatement,
                "illegal destination type for write");
        check(sourceType != Type.CONSOLE, writeStatement, "illegal source type for write");
        return null;
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        // left is dest right is source
        String name = readStatement.getName();
        Declaration dec = symbolTable.lookup(name);
        Type type = (Type) readStatement.getSource().visit(this, arg);
        if(type == CONSOLE){
            readStatement.getSource().setCoerceTo(dec.getType());
        }
        check(readStatement.getSelector() == null, readStatement, "read statement cannot have PixelSelector");
        check(type == CONSOLE || type == STRING, readStatement, "read rhs type must be CONTROL or STRING");
        readStatement.setTargetDec(dec);
        dec.setInitialized(true);
        return null;
    }

    private Kind getOp(VarDeclaration declaration) {
        Token op = (Token) declaration.getOp();
        return op.getKind();
    }

    @Override
    public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
        // insert into symbol table
        boolean hasDim = declaration.getNameDef().getDim() != null;
        if (hasDim) {
            if (declaration.getDim().getWidth().getClass() == IdentExpr.class) {
                visitIdentExpr((IdentExpr) declaration.getDim().getWidth(), arg);
            }
            if (declaration.getDim().getHeight().getClass() == IdentExpr.class) {
                visitIdentExpr((IdentExpr) declaration.getDim().getHeight(), arg);
            }
            if(declaration.getDim().getHeight().getClass() == IntLitExpr.class){
                declaration.getDim().getHeight().setType(Type.INT);
            }
            if(declaration.getDim().getWidth().getClass() == IntLitExpr.class){
                declaration.getDim().getWidth().setType(Type.INT);
            }
            visitNameDefWithDim((NameDefWithDim) declaration.getNameDef(), arg);
        } else {
            visitNameDef(declaration.getNameDef(), arg);
        }

        Expr rhs = declaration.getExpr();
        //If type of variable is Image, it must either have an initializer expression of type IMAGE, or a Dimension
        if (declaration.getType() == IMAGE) {
            if(declaration.getDim() != null){
                int height = Integer.parseInt(declaration.getDim().getHeight().getText());
                int width = Integer.parseInt(declaration.getDim().getWidth().getText());
                if(getOp(declaration) == Kind.ASSIGN){
                    readImage(rhs.getText(), width, height);
                }else if(rhs == null){
                    BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                }
            }else{
                if(getOp(declaration) == Kind.ASSIGN){
                    readImage(declaration.getName());
                }
            }

//            if (rhs != null) {
//                check(rhs.visit(this, arg) == IMAGE, declaration, "Initializer is not type IMAGE");
//            } else if(!hasDim){
//                throw new TypeCheckException("no image initializer or dimension for type IMAGE");
//            }

        }
//        else if(declaration.getType() == COLOR){
//            ColorTuple tuple = new ColorTuple();
//        }
            else {
            if (rhs != null) {
                Declaration rhsDec = symbolTable.lookup(rhs.getText());
                if (rhs.getClass() == IdentExpr.class) {
                    check(rhsDec != null, declaration, "rhs is not declared");
                    check(rhsDec.isInitialized(), declaration, "rhs is not initialized");
                }
                Type rhsType = (Type) rhs.visit(this, arg);
                if (getOp(declaration) == Kind.ASSIGN) {
                    check(assignmentCompatible(declaration.getType(), rhsType), declaration, "type of expression and declared type do not match");
                    declaration.getNameDef().setInitialized(true);
                    declaration.getExpr().setCoerceTo(declaration.getType());
                } else if (getOp(declaration) == Kind.LARROW) {
                    if(rhsType == CONSOLE){
                        declaration.getExpr().setCoerceTo(declaration.getType());
                    }
                    check(rhsType == CONSOLE || rhsType == STRING, declaration, "type of expression and declared type do not match");
                    declaration.getNameDef().setInitialized(true);
                } else {
                    throw new TypeCheckException("something here idk");
                }
            }
        }
        return declaration;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {

        List<NameDef> params = program.getParams();
        for (NameDef param : params) {
            check(symbolTable.lookup(param.getName()) == null, param, "Parameter name already declared");
            param.visit(this, arg);
            param.setInitialized(true);
        }
        //Save root of AST so return type can be accessed in return statements
        root = program;

        //Check declarations and statements
        List<ASTNode> decsAndStatements = program.getDecsAndStatements();
        for (ASTNode node : decsAndStatements) {
            node.visit(this, arg);
        }
        return program;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
        symbolTable.insert(nameDef.getName(), nameDef);
        return null;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        check(nameDefWithDim.getDim().getHeight().getType() == INT, nameDefWithDim, "height is not of type INT");
        check(nameDefWithDim.getDim().getWidth().getType() == INT, nameDefWithDim, "width is not of type INT");
        symbolTable.insert(nameDefWithDim.getName(), nameDefWithDim);
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
        Type returnType = root.getReturnType();  //This is why we save program in visitProgram.
        Type expressionType = (Type) returnStatement.getExpr().visit(this, arg);
        if (returnStatement.getExpr().getClass() == IdentExpr.class){
            Declaration dec = symbolTable.lookup(returnStatement.getExpr().getText());
            check(dec.isInitialized(), returnStatement, "return rhs has not been initialized");
        }
        check(returnType == expressionType, returnStatement, "return statement with invalid type");
        return null;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        Type expType = (Type) unaryExprPostfix.getExpr().visit(this, arg);
        check(expType == Type.IMAGE, unaryExprPostfix, "pixel selector can only be applied to image");
        unaryExprPostfix.getSelector().visit(this, arg);
        unaryExprPostfix.setType(Type.INT);
        unaryExprPostfix.setCoerceTo(COLOR);
        return Type.COLOR;
    }

    record Pair<T0, T1>(T0 t0, T1 t1) {
    }

}
