package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.runtime.ConsoleIO;
import edu.ufl.cise.plc.runtime.ImageOps;

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
        if (program.getReturnType() == Types.Type.IMAGE) {
            sb.append("import java.awt.image.BufferedImage;").newline();
        }
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
        sb.comma().append("BufferedImage.TYPE_INT_RGB").rightParen().semi();
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
                        //TODO: Change to visit once PixelSelector is implemented
                        sb.assign().append("ImageOps.resize(" + expr.getText() + ", " + varDeclaration.getDim().getHeight().getText() + ", " + varDeclaration.getDim().getHeight().getText()).rightParen().semi().newline();
                    } else if (expr.getClass() == IdentExpr.class) {
                        sb.assign().append("ImageOps.clone(" + expr.getText()).rightParen().semi().newline();
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
//        Expr x = unaryExprPostfix.getSelector().getX();
//        Expr y = unaryExprPostfix.getSelector().getY();
//
//
//
//        ColorTuple.unpack(.getRGB(0, 1));
        return null;
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
        /*
        color op color - binaryTupleOp
        image op image - binaryImageImageOp
        image op int - binaryImageScalarOp
        create a new colorTuple(int)


        color op image
        image op color
         */
        String operator = op.toString();

        if (leftType == Types.Type.COLOR && rightType == Types.Type.COLOR) {
            //TODO: apply the enum thing to all
            sb.append("ImageOps.binaryTupleOp(");
            sb.append("ImageOps.OP." + operator);
            sb.comma().append(left.getText()).comma().append(right.getText()).rightParen();
        } else if (leftType == Types.Type.IMAGE && rightType == Types.Type.IMAGE) {
            sb.append("ImageOps.binaryImageImageOp(").append(ImageOps.OP.valueOf(operator)).comma().append(left.getText()).comma().append(right.getText()).rightParen().semi();
        } else if(leftType == Types.Type.IMAGE && rightType == Types.Type.INT /*|| leftType == Types.Type.INT && rightType == Types.Type.IMAGE*/) {
            sb.append("ImageOps.binaryImageScalarOp(");
            sb.append("ImageOps.OP." + operator);
            sb.comma().append(left.getText()).comma().append(right.getText()).rightParen();
        }else {


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
        } else if (op == IToken.Kind.COLOR_OP) {
            // if expr is INT it is interpreted as PACKED pixel
            if (expr.getType() == Types.Type.INT || expr.getType() == Types.Type.COLOR) {
                if (op.toString().equals("getRed")) {
                    sb.append("(getRed(" + expr.getText() + "))");
                } else if (op.toString().equals("getGreen")) {
                    sb.append("(getGreen(" + expr.getText() + "))");

                } else if (op.toString().equals("getBlue")) {
                    sb.append("(getBlue(" + expr.getText() + "))");
                }
            } else if (expr.getType() == Types.Type.IMAGE) {
                if (op.toString().equals("getRed")) {
                    sb.append("(extractRed(" + expr.getText() + "))");
                } else if (op.toString().equals("getGreen")) {
                    sb.append("(extractGreen(" + expr.getText() + "))");
                } else if (op.toString().equals("getBlue")) {
                    sb.append("(extractBlue(" + expr.getText() + "))");
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
        Expr rhs = assignmentStatement.getExpr(); // rhs
        Declaration lhs = assignmentStatement.getTargetDec();
        if(lhs != null && lhs.getType() == Types.Type.IMAGE && rhs.getType() == Types.Type.IMAGE){
            if(lhs.getDim() != null){
//                The assignment is implemented by evaluating the
//                right hand size and calling ImageOps.resize.
                sb.append(name).assign().append("ImageOps.resize(" + assignmentStatement.getExpr().getText() + ", " +
                                                assignmentStatement.getSelector().getX().getText() + ", " +
                                                assignmentStatement.getSelector().getY().getText()).rightParen().semi().newline();
            }else{
//                the image <name>  takes the size of the
//                right hand side image.
            }
        }else if (rhs.getClass() == IdentExpr.class){
                sb.append(name).assign().append("ImageOps.clone(" + assignmentStatement.getExpr().getText()).rightParen().semi().newline();
        }else if(rhs.getCoerceTo() == Types.Type.COLOR){
            sb.append("for(int x = 0; x < " + name + ".getWidth(); x++)").newline();
            sb.append("\tfor(int y = 0; y < " + name + ".getHeight(); y++)").newline();
            sb.append("\t\tImageOps.setColor(" + name).comma().append("x,y,").newline();
            rhs.visit(this, sb);
            sb.rightParen().semi().newline();
        }else if(rhs.getCoerceTo() == Types.Type.INT){
//            the int is used as a single color component in a
//            ColorTuple where all three color components have
//            the value of the int.
//            (The value is truncated,
//            so values outside of [0, 256) will
//            be either white or black.)
            sb.append("in get coerce to int");
        }

//        if (assignmentStatement.getTargetDec() != null &&
//                assignmentStatement.getTargetDec().getType() == Types.Type.IMAGE &&
//                rhs.getType() == Types.Type.IMAGE) {
//            // if it has dimension
//            if (assignmentStatement.getSelector() != null) {
//                sb.append(name).assign().append("ImageOps.resize(" + assignmentStatement.getExpr().getText() + ", " + assignmentStatement.getSelector().getX().getText() + ", " + assignmentStatement.getSelector().getY().getText()).rightParen().semi().newline();
//            } else if (rhs.getClass() == IdentExpr.class) {
//                sb.append(name).assign().append("ImageOps.clone(" + assignmentStatement.getExpr().getText()).rightParen().semi().newline();
//            } else {
//                sb.append(name).assign().append(rhs.getText()).semi().newline();
//            }

/*
        if(assignmentStatement.getTargetDec().getType() == Types.Type.IMAGE && expr.getType() == Types.Type.IMAGE){
            if(assignmentStatement.getTargetDec().getDim() != null){
                sb.append("ImageOps.resize(").append(name, )
            }
        }
                sb.append("for(int x = 0; x < " + name + ".getWidth(); x++").newline();
                sb.append("\tfor(int y = 0; x < " + name + ".getHeight(); y++").newline();
                sb.append("\t\tImage")
*/
        else {
        sb.append(name).assign();
        if (rhs.getCoerceTo() != null && rhs.getType() != rhs.getCoerceTo()) {
            genTypeConversion(rhs.getType(), rhs.getCoerceTo(), sb);
        }
        rhs.visit((ASTVisitor) this, sb);
        sb.semi().newline();}
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
        } else {
            sb.leftParen().append(coerceTo.toString().toLowerCase()).rightParen();
        }
    }


}
