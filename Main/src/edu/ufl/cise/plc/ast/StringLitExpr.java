package edu.ufl.cise.plc.ast;

import edu.ufl.cise.plc.IToken;
import edu.ufl.cise.plc.LexicalException;

public class StringLitExpr extends Expr{

	public StringLitExpr(IToken firstToken) {
		super(firstToken);
	}

	@Override
	public Object visit(ASTVisitor v, Object arg) throws Exception {
		return v.visitStringLitExpr(this,arg);
	}
	
	public String getValue() throws LexicalException {
		return firstToken.getStringValue();
	}

	@Override
	public String toString() {
		try {
			return "StringLitExpr [getValue()=" + getValue() + "]";
		} catch (LexicalException e) {
			e.printStackTrace();
		}
		return "Error with getting String";
	}

}
