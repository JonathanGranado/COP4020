package edu.ufl.cise.plc;

public class CodeGenStringBuilder {

    StringBuilder delegate;

    public CodeGenStringBuilder append(String s){
        delegate.append(s);
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

    public CodeGenStringBuilder leftSemi() {
        delegate.append("(");
        return this;
    }

    public CodeGenStringBuilder rightSemi() {
        delegate.append(")");
        return this;
    }


    public CodeGenStringBuilder assign() {
        delegate.append("=");
        return this;
    }
}
