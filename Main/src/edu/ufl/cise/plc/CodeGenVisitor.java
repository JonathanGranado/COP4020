package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.runtime.ConsoleIO;

import javax.swing.event.ListDataEvent;
import java.util.Iterator;
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
        sb.append(packageName).semi().newline();
        sb.append("import edu.ufl.cise.plc.runtime.*; ").newline();
        sb.append("public class ").append(program.getName()).append(" ").leftBrace().newline().append("\t public static ");
        if (program.getReturnType() == Types.Type.STRING) {
            sb.append("String");
        } else {
            sb.append(program.getReturnType().toString().toLowerCase());
        }
        sb.append(" apply").leftParen();

        List<NameDef> list = program.getParams();
        Iterator<NameDef> iterator = list.iterator();
        while (iterator.hasNext()){
            NameDef name = iterator.next();
            if (name.getType().toString().equals("STRING")) {
                sb.append("String " + name.getName());
            } else {
                sb.append(name.getType().toString().toLowerCase() + " " + name.getName());
            }
            if(iterator.hasNext()){
                sb.comma();
            }
        }

        sb.rightParen().leftBrace().newline().append("\t\t");

        if (program.getDecsAndStatements() != null) {
            // add list of declarations and statements
            for (ASTNode object : program.getDecsAndStatements()) {
                object.visit(this, sb);
            }
        }
        sb.append("\t").rightBrace().newline().rightBrace().newline();
        String result = sb.delegate.toString();
        return result;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        if (nameDef.getType() == Types.Type.STRING){
            sb.append("String ").append(nameDef.getName());
        } else{
            Types.Type type = nameDef.getType();
            String name = nameDef.getName();
            sb.append(type.toString().toLowerCase()).append(" ").append(name);
        }
        return sb;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        return null;
    }

    public Object visitVarDeclaration(VarDeclaration varDeclaration, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        NameDef nameDef = varDeclaration.getNameDef();
        Expr expr = varDeclaration.getExpr();
        IToken op = varDeclaration.getOp();
        nameDef.visit((ASTVisitor) this, sb);
        // if no initializer
        if (op == null) {
            sb.semi();
        } else {
            if (op.getKind() == IToken.Kind.ASSIGN){
                sb.append("=");
            } else if (op.getKind() == IToken.Kind.LARROW){
                sb.append("<-");
            } else {
                throw new UnsupportedOperationException("Invalid operator");
            }
            expr.visit(this, sb);
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
        return ((CodeGenStringBuilder) arg).append(sb.delegate.toString());
    }

    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.append(booleanLitExpr.getValue());
        return sb;
    }

    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Types.Type coerceType = consoleExpr.getCoerceTo();
        String objectType = null;
        if (coerceType == Types.Type.INT) {
            objectType = "int";
        } else if (coerceType == Types.Type.STRING) {
            objectType = "String";
        } else if (coerceType == Types.Type.BOOLEAN) {
            objectType = "boolean";
        } else if (coerceType == Types.Type.FLOAT) {
            objectType = "float";
        }
        sb.leftParen().append(objectType);
        sb.rightParen();
        Object value = ConsoleIO.readValueFromConsole(coerceType.toString(), "Enter value");
        sb.append(value);
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
        sb.append(value);
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
        sb.append(value);
        return sb;
    }

    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        if (identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != identExpr.getType()) {
            genTypeConversion(identExpr.getType(), identExpr.getCoerceTo(), sb);
        }
        sb.append(identExpr.getText());
        return sb;
    }

    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.append("\"").append(stringLitExpr.getValue()).append("\"");
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
        sb.append("\t\t").append(name).assign();
        expr.visit((ASTVisitor) this, sb);
        sb.semi().newline();
        return sb;
    }

    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Expr source = writeStatement.getSource();
        ConsoleIO.console.println(source.visit((ASTVisitor) this, sb));
        sb.semi();
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
        sb.append("\t\treturn ");
        expr.visit((ASTVisitor) this, sb);
        sb.semi().newline();
        return sb;
    }

    private void genTypeConversion(Types.Type type, Types.Type coerceTo, CodeGenStringBuilder sb) {

    }


}
