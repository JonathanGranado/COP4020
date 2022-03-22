package edu.ufl.cise.plc;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.ast.Types.Type;

import java.util.List;
import java.util.Map;

import static edu.ufl.cise.plc.ast.Types.Type.*;

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
        check(dec.isInitialized(), identExpr, "using uninitialized variable");
        identExpr.setDec(dec);
        Type type = dec.getType();
        identExpr.setType(type);
        return type;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        Type condition = (Type)conditionalExpr.getCondition().visit(this, arg);
        Type trueCase = (Type)conditionalExpr.getTrueCase().visit(this, arg);
        Type falseCase = (Type)conditionalExpr.getFalseCase().visit(this, arg);

        check(condition == BOOLEAN, conditionalExpr, "type of condition must be boolean");
        check(trueCase == falseCase, conditionalExpr, "type of true case must be equal to false case");
        conditionalExpr.setType(trueCase);
        return trueCase;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        Type left = (Type)dimension.getHeight().visit(this, arg);
        Type right = (Type)dimension.getWidth().visit(this, arg);
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
        Type rhsType = (Type) assignmentStatement.getExpr().visit(this, arg);
        Type lhsType = lhsDeclaration.getType();
        Declaration rhsDeclaration = (Declaration) assignmentStatement.getExpr().visit(this, arg);
        check(rhsDeclaration.isInitialized(), assignmentStatement, "rhs expression is not initialized");
        lhsDeclaration.setInitialized(true);
        //TODO: FINISH
        boolean hasSelector = assignmentStatement.getSelector() != null;
        if (lhsType != IMAGE) {
            check(assignmentCompatible(lhsType, rhsType), assignmentStatement, "incompatible types in assignment");
            assignmentStatement.getExpr().setCoerceTo(lhsType);

        } else if (!hasSelector) {
            check(assignmentCompatible(lhsType, rhsType), assignmentStatement, "incompatible types in assignment");
            if (rhsType == INT) assignmentStatement.getExpr().setCoerceTo(COLOR);
            if (rhsType == FLOAT) assignmentStatement.getExpr().setCoerceTo(COLORFLOAT);

        } else {

            PixelSelector selector = assignmentStatement.getSelector();
            Expr x =  selector.getX();
            Expr y =  selector.getY();
            check(x.getClass() == IdentExpr.class, assignmentStatement, "x is not type identExpr");
            check(y.getClass() == IdentExpr.class, assignmentStatement, "y is not type identExpr");
            check(symbolTable.lookup(x.getText()) == null, assignmentStatement, "x is already declared");
            check(symbolTable.lookup(y.getText()) == null, assignmentStatement, "y is already declared");
            check((x.getType() == INT && y.getType() == INT), assignmentStatement, "x,y must be INT");
            // declare and initialize local variables
            symbolTable.insert(x.getText(), ((IdentExpr) x).getDec());
            symbolTable.insert(y.getText(), ((IdentExpr) y).getDec());
            symbolTable.lookup(x.getText()).setInitialized(true);
            symbolTable.lookup(y.getText()).setInitialized(true);

            check(assignmentCompatible(lhsType, rhsType), assignmentStatement, "incompatible rhs type in assignment");
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
        Type sourceType = (Type) writeStatement.getSource().visit(this, arg);
        Type destType = (Type) writeStatement.getDest().visit(this, arg);
        check(destType == Type.STRING || destType == Type.CONSOLE, writeStatement,
                "illegal destination type for write");
        check(sourceType != Type.CONSOLE, writeStatement, "illegal source type for write");
        return null;
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {

        String name = readStatement.getName();
        Declaration dec = symbolTable.lookup(name);
        check(dec.isInitialized(), readStatement, "uninitialized variable " + name);
        Type type = (Type) readStatement.getSource().visit(this, arg);
        check(readStatement.getSelector() == null, readStatement, "read statement cannot have PixelSelector");
        check(type == CONSOLE || type == STRING, readStatement, "read rhs type must be CONTROL or STRING");
        dec.setInitialized(true);
        return null;
    }

    @Override
    public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
        //TODO:  implement this method
        throw new UnsupportedOperationException("Unimplemented visit method.");
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        //TODO:  this method is incomplete, finish it.

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
        // TODO: check that dimensions are int
        symbolTable.insert(nameDefWithDim.getName(), nameDefWithDim);
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
        Type returnType = root.getReturnType();  //This is why we save program in visitProgram.
        Type expressionType = (Type) returnStatement.getExpr().visit(this, arg);
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
