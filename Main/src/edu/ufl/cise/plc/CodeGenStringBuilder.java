package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.NameDef;

public class CodeGenStringBuilder {

    StringBuilder delegate;

    public CodeGenStringBuilder append(String s){
        delegate.append(s);
        return this;
    }

    public CodeGenStringBuilder append(NameDef nameDef){
        delegate.append(nameDef);
        return this;
    }

    public CodeGenStringBuilder append(boolean b){
        delegate.append(b);
        return this;
    }

    public CodeGenStringBuilder append(int i){
        delegate.append(i);
        return this;
    }

    public CodeGenStringBuilder append(float f){
        delegate.append(f).append("f");
        return this;
    }

    public CodeGenStringBuilder comma(){
        delegate.append(",");
        return this;
    }

    public CodeGenStringBuilder semi() {
        delegate.append(";");
        return this;
    }

    public CodeGenStringBuilder newline(){
        delegate.append("\n");
        return this;
    }

    public CodeGenStringBuilder leftParen() {
        delegate.append("(");
        return this;
    }

    public CodeGenStringBuilder rightParen() {
        delegate.append(")");
        return this;
    }


    public CodeGenStringBuilder assign() {
        delegate.append("=");
        return this;
    }

    public CodeGenStringBuilder tripleQuote() {
        delegate.append("\"\"\"");
        return this;
    }

    public CodeGenStringBuilder colon() {
        delegate.append(":");
        return this;
    }

    public CodeGenStringBuilder ternary() {
        delegate.append("?");
        return this;
    }

    public CodeGenStringBuilder leftBrace() {
        delegate.append("{");
        return this;
    }
    public CodeGenStringBuilder rightBrace() {
        delegate.append("}");
        return this;
    }
}
