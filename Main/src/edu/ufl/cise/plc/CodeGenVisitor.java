package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.runtime.ConsoleIO;

import java.util.List;
import java.util.Locale;

public class CodeGenVisitor implements ASTVisitor {

    private String packageName;
    // still trying to figure out how to get program to pass in here
    public CodeGenVisitor(String _packageName) throws Exception {
        packageName = _packageName;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        int index = program.getParams().size();
        CodeGenStringBuilder sb = new CodeGenStringBuilder();
        sb.append("package ");
        sb.append(packageName).semi();
        sb.append(" import runtime.*; ");
        sb.append("public class ").append(program.getName()).leftBrace().append("\t public static ").append(program.getReturnType().toString().toLowerCase());
        sb.append(" apply ").leftParen();
        // if there are parameters
        if (program.getParams().size() > 0) {
            // make list of parameter name as strings
            List<String> params = null;
            for (NameDef param : program.getParams()) {
                params.add(param.getName());
            }
            // turn param names into CSV string
            String CSVParams = String.join(",", params);
            sb.append(CSVParams);
        }
        sb.rightParen().leftBrace();

        if (program.getDecsAndStatements() != null) {
            // add list of declarations and statements
            for (ASTNode object : program.getDecsAndStatements()) {
                object.visit(this, sb);
            }
        }
        sb.rightBrace().rightBrace().newline();

        return sb.toString();
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
        CodeGenStringBuilder sb = new CodeGenStringBuilder();
        Types.Type type = nameDef.getType();
        String name = nameDef.getName();
        sb.append(type.toString()).append(name).newline();
        return sb;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        return null;
    }

    public Object visitVarDeclaration(VarDeclaration varDeclaration, Object arg) throws Exception {
        CodeGenStringBuilder sb = new CodeGenStringBuilder();
        NameDef nameDef = varDeclaration.getNameDef();
        Expr expr = varDeclaration.getExpr();
        IToken.Kind op = varDeclaration.getOp().getKind();
        nameDef.visit((ASTVisitor) this, sb);
        if (varDeclaration.getOp() == null) {
            sb.semi();
        } else if (op == IToken.Kind.ASSIGN) {
            throw new UnsupportedOperationException("Not implemented");
        } else {
            sb.append("=");
            expr.visit((ASTVisitor) this, arg);
        }
        sb.newline();
        return sb;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        return null;
    }

    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = new CodeGenStringBuilder();
        Expr condition = conditionalExpr.getCondition();
        Expr trueCase = conditionalExpr.getTrueCase();
        Expr falseCase = conditionalExpr.getFalseCase();
        sb.leftParen();
        condition.visit((ASTVisitor) this, sb);
        sb.rightParen().ternary();
        trueCase.visit((ASTVisitor) this, sb);
        sb.colon();
        falseCase.visit((ASTVisitor) this, sb);
        sb.newline();
        return sb;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        return null;
    }

    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = new CodeGenStringBuilder();
        Types.Type type = binaryExpr.getType();
        Expr left = binaryExpr.getLeft();
        Expr right = binaryExpr.getRight();
        Types.Type leftType = left.getCoerceTo() != null ? left.getCoerceTo() : left.getType();
        Types.Type rightType = right.getCoerceTo() != null ? right.getCoerceTo() : right.getType();
        IToken.Kind op = binaryExpr.getOp().getKind();
        sb.leftParen();
        left.visit((ASTVisitor) this, sb);
        sb.append(binaryExpr.getOp().getText());
        right.visit((ASTVisitor) this, sb);
        sb.rightParen();
        if (binaryExpr.getCoerceTo() != type) {
            genTypeConversion(type, binaryExpr.getCoerceTo(), sb);
        }
        return ((CodeGenStringBuilder) arg).append(sb.toString());
    }

    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.append(booleanLitExpr.getValue());
        return sb.newline();
    }

    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Types.Type coerceType = consoleExpr.getCoerceTo();
        String objectType = null;
        if (coerceType == Types.Type.INT) {
            objectType = "Integer";
        } else if (coerceType == Types.Type.STRING) {
            objectType = "String";
        } else if (coerceType == Types.Type.BOOLEAN) {
            objectType = "Boolean";
        } else if (coerceType == Types.Type.FLOAT) {
            objectType = "Float";
        }
        sb.leftParen().append(objectType);
        ConsoleIO.readValueFromConsole(coerceType.toString(), "Enter desired type");
        sb.rightParen().newline();
        return sb;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        return null;
    }

    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        float value = floatLitExpr.getValue();
        if (floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() != Types.Type.FLOAT) {
            genTypeConversion(floatLitExpr.getType(), floatLitExpr.getCoerceTo(), sb);
        }
        sb.append(value).newline();
        return sb;
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
        return null;
    }

    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        int value = intLitExpr.getValue();
        if (intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Types.Type.INT) {
            genTypeConversion(intLitExpr.getType(), intLitExpr.getCoerceTo(), sb);
        }
        sb.append(value).newline();
        return sb;
    }

    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        if (identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != identExpr.getType()) {
            genTypeConversion(identExpr.getType(), identExpr.getCoerceTo(), sb);
        }
        sb.append(identExpr.getText()).newline();
        return sb;
    }

    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.tripleQuote().append(stringLitExpr.getValue()).tripleQuote().newline();
        return sb;
    }

    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
//        Types.Type type = unaryExpr.getType();
        IToken.Kind op = unaryExpr.getOp().getKind();
        Expr expr = unaryExpr.getExpr();
        if (op == IToken.Kind.BANG || op == IToken.Kind.MINUS) {
            sb.leftParen().append(unaryExpr.getOp().getText());
            expr.visit((ASTVisitor) this, sb);
            sb.rightParen().newline();
        } else {
            throw new UnsupportedOperationException("Not implemented");
        }
        return sb;
    }


    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = readStatement.getName();
        ConsoleExpr expr = (ConsoleExpr) readStatement.getSource();
        sb.append(name).assign();
        expr.visit((ASTVisitor) this, sb);
        sb.semi().newline();
        return sb;
    }

    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Expr source = writeStatement.getSource();
        ConsoleIO.console.println(source.visit((ASTVisitor) this, sb));
        sb.semi().newline();
        return sb;
    }

    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = assignmentStatement.getName();
        Expr expr = assignmentStatement.getExpr();
        sb.append(name).assign();
        expr.visit((ASTVisitor) this, sb);
        sb.semi().newline();
        return sb;
    }


    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Expr expr = returnStatement.getExpr();
        sb.append("return");
        expr.visit((ASTVisitor) this, sb);
        sb.semi().newline();
        return sb;
    }

    private void genTypeConversion(Types.Type type, Types.Type coerceTo, CodeGenStringBuilder sb) {

    }


}
