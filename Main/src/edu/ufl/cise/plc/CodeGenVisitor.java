package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

public class CodeGenVisitor {

    public CodeGenVisitor(String packageName){
        visitProgram(packageName);
    }

    private void visitProgram(String packageName) {
        CodeGenStringBuilder sb = new CodeGenStringBuilder();
        sb.append(packageName);
        sb.append("import runtime.*");

    }

    // return arg.. are just placeholders
    public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
        return arg;
    }

    public Object visitVarDeclaration(VarDeclaration varDeclaration, Object arg) throws Exception {
        return arg;

    }

    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        return arg;

    }

    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = new CodeGenStringBuilder();
        Types.Type type = binaryExpr.getType();
        Expr left = binaryExpr.getLeft();
        Expr right = binaryExpr.getRight();
        Types.Type leftType = left.getCoerceTo() != null ? left.getCoerceTo() : left.getType();
        Types.Type rightType = right.getCoerceTo() != null ? right.getCoerceTo() : right.getType();
        IToken.Kind op = binaryExpr.getOp().getKind();
//        if(not handled in assignment 5){
//            throw new UnsupportedOperationException("Not implemented");
//        }else{
//            sb.leftParen();
//            left.visit((ASTVisitor) this, sb);
//            sb.append(binaryExpr.getOp().getText());
//            right.visit((ASTVisitor) this, sb);
//            sb.rightParen();
//        }
        if(binaryExpr.getCoerceTo() != type){
            genTypeConversion(type, binaryExpr.getCoerceTo(), sb);
        }
        return ((CodeGenStringBuilder)arg).append(sb.toString());
    }

    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
        return arg;

    }

    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        return arg;

    }

    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
        return arg;

    }

    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
        return arg;

    }

    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        return arg;

    }

    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
        return arg;

    }

    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
        return arg;

    }


    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        return arg;

    }

    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        return arg;
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
