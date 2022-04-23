package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.runtime.ConsoleIO;

import java.util.Iterator;
import java.util.List;

public class CodeGenVisitor implements ASTVisitor {

    private String packageName;

    // still trying to figure out how to get program to pass in here
    public CodeGenVisitor(String _packageName) throws Exception {
        packageName = _packageName;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        CodeGenStringBuilder sb = new CodeGenStringBuilder();
        sb.append("package ");
        sb.append(packageName).semi().newline();
        sb.append("import java.awt.image.BufferedImage;").newline();
        sb.append("import java.awt.Color;").newline();
        sb.append("import edu.ufl.cise.plc.runtime.*; ").newline();
        sb.append("public class ").append(program.getName()).append(" ").leftBrace().newline().append("\t public static ");
        if (program.getReturnType() == Types.Type.STRING) {
            sb.append("String");
        } else if (program.getReturnType() == Types.Type.IMAGE) {
            sb.append("BufferedImage");
        } else if (program.getReturnType() == Types.Type.COLOR) {
            sb.append("ColorTuple");
        } else {
            sb.append(program.getReturnType().toString().toLowerCase());
        }
        sb.append(" apply").leftParen();

        List<NameDef> list = program.getParams();
        Iterator<NameDef> iterator = list.iterator();
        while (iterator.hasNext()) {
            NameDef name = iterator.next();
            if (name.getType().toString().equals("STRING")) {
                sb.append("String " + name.getName());
            } else if (name.getType().toString().equals("IMAGE")) {
                sb.append(("BufferedImage " + name.getName()));
            } else if (name.getType().toString().equals("COLOR")) {
                sb.append(("ColorTuple " + name.getName()));

            } else {
                sb.append(name.getType().toString().toLowerCase() + " " + name.getName());
            }
            if (iterator.hasNext()) {
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
        if (nameDef.getType() == Types.Type.STRING) {
            sb.append("String ").append(nameDef.getName());
        } else if (nameDef.getType() == Types.Type.IMAGE) {
            sb.append("BufferedImage ").append(nameDef.getName());
        } else {
            Types.Type type = nameDef.getType();
            String name = nameDef.getName();
            sb.append(type.toString().toLowerCase()).append(" ").append(name);
        }
        return sb;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = nameDefWithDim.getName();
        Dimension dim = nameDefWithDim.getDim();
        sb.append("BufferedImage ").append(name).append(" = new BufferedImage(");
        dim.visit((ASTVisitor) this, sb);
        sb.comma().append("BufferedImage.TYPE_INT_RGB").rightParen();
        return sb;
    }

    public Object visitVarDeclaration(VarDeclaration varDeclaration, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        NameDef nameDef = varDeclaration.getNameDef();
        Expr expr = varDeclaration.getExpr();
        IToken op = varDeclaration.getOp();
        if (varDeclaration.getType() == Types.Type.IMAGE) {
            if (varDeclaration.getDim() != null) {
                if (op != null && op.getKind() == IToken.Kind.LARROW) {
                    String url = varDeclaration.getExpr().getText();
                    sb.append("BufferedImage " + varDeclaration.getNameDef().getName() + " = FileURLIO.readImage(");
                    sb.append(url).comma().append(varDeclaration.getDim().getWidth().getText()).comma().append(varDeclaration.getDim().getHeight().getText()).rightParen().semi().newline();
                    sb.append("\t\tFileURLIO.closeFiles();").newline();
                } else {
                    nameDef.visit((ASTVisitor) this, sb);
                }
            } else {
                if (op != null && op.getKind() == IToken.Kind.LARROW) {
                    String url = varDeclaration.getExpr().getText();
                    sb.append("BufferedImage " + varDeclaration.getNameDef().getName() + " = FileURLIO.readImage(").append(url).rightParen().semi();
                }
                // image being assigned another image
                else if (op != null && op.getKind() == IToken.Kind.ASSIGN) {
                    nameDef.visit((ASTVisitor) this, sb);
                    if (varDeclaration.getDim() != null) {
                        expr.visit(this, sb);
                    } else if (expr.getClass() == IdentExpr.class) {
                        expr.visit(this, sb);
                    } else {
                        sb.assign();
                        expr.visit(this, sb);
                        sb.semi().newline();
                    }
                }
            }
        } else {
            // if no initializer
            if (op == null) {
                sb.semi();
            } else {
                String name = varDeclaration.getNameDef().getName();
                Types.Type type = varDeclaration.getNameDef().getType();
                if (type == Types.Type.COLOR) {
                    sb.append("ColorTuple ");
                } else {
                    sb.append(type.toString().toLowerCase() + " ");
                }
                sb.append(name + " = ");
                if (op.getKind() == IToken.Kind.ASSIGN) {
                    if (expr.getCoerceTo() != null && expr.getCoerceTo() != expr.getType()) {
                        genTypeConversion(expr.getType(), expr.getCoerceTo(), sb);
                    }
                    expr.visit(this, sb);
                } else if (op.getKind() == IToken.Kind.LARROW) {
                    sb.append(" = ");
                    if (expr.getCoerceTo() != null && expr.getCoerceTo() != expr.getType()) {
                        genTypeConversion(expr.getType(), expr.getCoerceTo(), sb);
                    }
                    expr.visit(this, sb); // should be consoleExpr
                } else {
                    throw new UnsupportedOperationException("Invalid operator");
                }
                sb.semi();
            }
        }
        sb.newline();
        return sb;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Expr expr = unaryExprPostfix.getExpr();
        String x = unaryExprPostfix.getSelector().getX().getText();
        String y = unaryExprPostfix.getSelector().getY().getText();
        sb.append("ColorTuple.unpack(");
        sb.append(expr.getText());
        sb.append(".getRGB(" + x + ", " + y + "))");

        return sb;
    }

    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Expr condition = conditionalExpr.getCondition();
        Expr trueCase = conditionalExpr.getTrueCase();
        Expr falseCase = conditionalExpr.getFalseCase();
        sb.append("\t\t").leftParen();
        condition.visit((ASTVisitor) this, sb);
        sb.rightParen().ternary().newline().append("\t\t").leftParen();
        trueCase.visit((ASTVisitor) this, sb);
        sb.rightParen().colon().newline().append("\t\t").leftParen();
        falseCase.visit((ASTVisitor) this, sb);
        sb.rightParen();
        return sb;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Expr width = dimension.getWidth();
        Expr height = dimension.getHeight();
        width.visit(this, sb);
        sb.comma();
        height.visit(this, sb);
        return sb;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String firstVar = pixelSelector.getX().getText();
        String secondVar = pixelSelector.getY().getText();
        String name = pixelSelector.getText();

        sb.append("for(int " + firstVar + " = 0;" + firstVar + "< " + name + ".getWidth();" + firstVar + "++)").newline();
        sb.append("\tfor(int " + secondVar + " = 0;" + secondVar + "< " + name + ".getWidth();" + secondVar + "++)").newline();
        return sb;
    }

    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = new CodeGenStringBuilder();
        Types.Type type = binaryExpr.getType();
        Expr left = binaryExpr.getLeft();
        Expr right = binaryExpr.getRight();
        Types.Type leftType = left.getCoerceTo() != null ? left.getCoerceTo() : left.getType();
        Types.Type rightType = right.getCoerceTo() != null ? right.getCoerceTo() : right.getType();
        IToken.Kind op = binaryExpr.getOp().getKind();
        String operator = op.toString();


        // image op image -> binaryImageImageOp
        if (leftType == Types.Type.IMAGE && rightType == Types.Type.IMAGE) {
            sb.append("ImageOpsBetter.binaryImageImageOp(");
            if (operator.equals("EQUALS") || operator.equals("NOT_EQUALS")) {
                sb.append("ImageOpsBetter.BoolOP." + operator);
            } else {
                sb.append("ImageOpsBetter.OP." + operator);
            }
            sb.comma();
            left.visit(this, sb);
            sb.comma();
            right.visit(this, sb);
            sb.rightParen();
        } else if (leftType == Types.Type.IMAGE && rightType == Types.Type.INT || (leftType == Types.Type.INT && rightType == Types.Type.IMAGE)) {
            sb.append("ImageOpsBetter.binaryImageScalar(");
            if (operator.equals("EQUALS") || operator.equals("NOT_EQUALS")) {
                sb.append("ImageOpsBetter.BoolOP." + operator);
            } else {
                sb.append("ImageOpsBetter.OP." + operator);
            }
            sb.comma();
            left.visit(this, sb);
            sb.comma();
            right.visit(this, sb);
            sb.rightParen().semi();
        }
        // color  op color
        else if (leftType == Types.Type.COLOR && rightType == Types.Type.COLOR) {
            sb.append("ImageOpsBetter.binaryTupleOp(");
            if (operator.equals("EQUALS") || operator.equals("NOT_EQUALS")) {
                sb.append("ImageOpsBetter.BoolOP." + operator);
            } else {
                sb.append("ImageOpsBetter.OP." + operator);
            }
            sb.comma();
            left.visit(this, sb);
            sb.comma();
            right.visit(this, sb);
            sb.rightParen().semi();
        }
        // image op color
        else if (leftType == Types.Type.IMAGE && rightType == Types.Type.COLOR || leftType == Types.Type.COLOR && rightType == Types.Type.IMAGE) {
            sb.append("ImageOpsBetter.setColor(");
            left.visit(this, sb);
            sb.comma();
            right.visit(this, sb);
            sb.rightParen().semi();
        } else {

            if (op == IToken.Kind.NOT_EQUALS) {
                sb.append("!");
            }
            sb.leftParen();
            left.visit((ASTVisitor) this, sb);
            if (leftType == Types.Type.STRING && rightType == Types.Type.STRING && op == IToken.Kind.EQUALS) {
                sb.append(".equals").leftParen();
                right.visit((ASTVisitor) this, sb);
                sb.rightParen();
                // != how to keep track of that
            } else if (leftType == Types.Type.STRING && rightType == Types.Type.STRING && op == IToken.Kind.NOT_EQUALS) {
                sb.append(".equals").leftParen();
                right.visit((ASTVisitor) this, sb);
                sb.rightParen();
            } else {
                sb.append(binaryExpr.getOp().getText());
                right.visit((ASTVisitor) this, sb);
                sb.rightParen();
            }
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
        if (coerceType == Types.Type.STRING) {
            sb.append("\"");
            Object value = ConsoleIO.readValueFromConsole(coerceType.toString(), "Enter string: ");
            sb.append(value);
            sb.append("\"");
        } else if (coerceType == Types.Type.COLOR) {
            Object value = ConsoleIO.readValueFromConsole(coerceType.toString(), "Enter a three color values : ");
            sb.append(value);
        } else {
            Object value = ConsoleIO.readValueFromConsole(coerceType.toString(), "Enter a " + coerceType.toString().toLowerCase() + ": ");
            sb.append(value);
        }
        return sb;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        Expr red = colorExpr.getRed();
        Expr green = colorExpr.getGreen();
        Expr blue = colorExpr.getBlue();
        sb.append("new ColorTuple(");
        red.visit(this, sb);
        sb.comma();
        green.visit(this, sb);
        sb.comma();
        blue.visit(this, sb);
        sb.rightParen();
        return sb;

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
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = colorConstExpr.getText();
        sb.append("ColorTuple.unpack(Color." + name + ".getRGB())");
        return sb;
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
        return sb.toString();
    }

    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        sb.append("\"").append(stringLitExpr.getValue()).append("\"");
        return sb;
    }

    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
//        Types.Type type = unaryExpr.getType();
        IToken.Kind opKind = unaryExpr.getOp().getKind();
        String op = unaryExpr.getOp().getText();
        Expr expr = unaryExpr.getExpr();
        if (opKind == IToken.Kind.BANG || opKind == IToken.Kind.MINUS) {
            sb.leftParen().append(unaryExpr.getOp().getText());
            expr.visit((ASTVisitor) this, sb);
            sb.rightParen().newline();
        } else if (opKind == IToken.Kind.COLOR_OP) {
            // if expr is INT it is interpreted as PACKED pixel
            if (expr.getType() == Types.Type.INT || expr.getType() == Types.Type.COLOR) {
                if (op.equals("getRed")) {
                    sb.append("(ColorTuple.getRed(");
                    expr.visit(this, sb);
                    sb.append("))");
                } else if (op.equals("getGreen")) {
                    sb.append("(ColorTuple.getGreen(");
                    expr.visit(this, sb);
                    sb.append("))");
                } else if (op.equals("getBlue")) {
                    sb.append("(ColorTuple.getBlue(");
                    expr.visit(this, sb);
                    sb.append("))");
                }
            } else if (expr.getType() == Types.Type.IMAGE) {

                if (op.equals("getRed")) {
                    sb.append("(ImageOpsBetter.extractRed(");
                    expr.visit(this, sb);
                    sb.append("))");
                } else if (op.equals("getGreen")) {
                    sb.append("(ImageOpsBetter.extractGreen(");
                    expr.visit(this, sb);
                    sb.append("))");
                } else if (op.equals("getBlue")) {
                    sb.append("(ImageOpsBetter.extractBlue(");
                    expr.visit(this, sb);
                    sb.append("))");
                }
            }
        } else {
            throw new UnsupportedOperationException("Not implemented");
        }
        return sb;
    }


    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = readStatement.getName();
        ConsoleExpr expr = (ConsoleExpr) readStatement.getSource();
        String url = readStatement.getTargetDec().getText();
        Types.Type targetType = readStatement.getTargetDec().getType();
        if (targetType == Types.Type.IMAGE) {
            if (readStatement.getSelector() != null) {
                sb.append(name + " = FileURLIO.readImage(" + readStatement.getSource().visit(this, sb)).comma().append(readStatement.getSelector().visit(this, sb) + ");").newline();
            } else {
                sb.append(name + " = FileURLIO.readImage(" + readStatement.getSource().visit(this, sb)).append(");").semi().newline();
            }
            sb.append("FileURLIO.closeFiles()");
        } else {
            sb.append("\t\t").append(name).assign();
            expr.visit((ASTVisitor) this, sb);
            sb.semi().newline();
        }
        return sb;
    }

    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        Expr source = writeStatement.getSource();
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;

        // if image -> console
        if (source.getType() == Types.Type.IMAGE && writeStatement.getDest().getType() == Types.Type.CONSOLE) {
            sb.append("ConsoleIO.displayImageOnScreen(" + source.getText() + ");");
        }
        // if writing to file
        else if (writeStatement.getDest().getType() == Types.Type.STRING) {
            // if writing from image to file
            if (source.getType() == Types.Type.IMAGE) {
                sb.append("FileURLIO.writeImage(" + source.getText() + ",  " + writeStatement.getDest().getText() + ");");
            } else {
                sb.append("FileURLIO.writeValue(" + source.getText() + ",  " + writeStatement.getDest().getText() + ");");
            }
        } else {
            sb.append("ConsoleIO.console.println(" + source.getText() + ");");
            //  ConsoleIO.console.println(source.visit(this, sb));
            sb.append("\t\t");
        }


        return sb;
    }

    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        CodeGenStringBuilder sb = (CodeGenStringBuilder) arg;
        String name = assignmentStatement.getName();
        Declaration lhs = assignmentStatement.getTargetDec();
        Expr rhs = assignmentStatement.getExpr();


        if (lhs!=null && lhs.getType() == Types.Type.IMAGE){
            if (lhs.getDim() != null){
                if (rhs.getCoerceTo() == Types.Type.COLOR){
                    lhs.getDim().visit(this,sb);

                }
            }
        }
        // if the namedef for assignment statement is an image and has dimension and expr.coerceTo is color

        /*if (lhs != null && lhs.getType() == Types.Type.IMAGE && rhs.getType() == Types.Type.IMAGE) {
            if (lhs.getDim() != null) {
                sb.append(name).assign().append("ImageOpsBetter.resize(" + rhs.visit(this, sb) + ")").semi().newline();
            } else {
//                the image <name>  takes the size of the
//                right hand side image.
                if (rhs.getClass() == IdentExpr.class) {
                    sb.append(name).assign().append("ImageOpsBetter.clone(" + rhs.getText());
                } else if (rhs.getCoerceTo() == Types.Type.COLOR) {
                    rhs.visit(this, sb);
                } else if (rhs.getCoerceTo() == Types.Type.INT) {
                    rhs.visit(this, sb);
                }
            }
        } else*/ {
            sb.append(name).assign();
            if (rhs.getCoerceTo() != null && rhs.getType() != rhs.getCoerceTo()) {
                genTypeConversion(rhs.getType(), rhs.getCoerceTo(), sb);
            }
            rhs.visit((ASTVisitor) this, sb);
            sb.semi().newline();
        }
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
// should also handle if case is String
        if (coerceTo == Types.Type.STRING) {
            sb.leftParen().append("String").rightParen();
        } else if (coerceTo == Types.Type.COLOR) {
            sb.leftParen().append("ColorTuple").rightParen();
        } else {
            sb.leftParen().append(coerceTo.toString().toLowerCase()).rightParen();
        }
    }


}
