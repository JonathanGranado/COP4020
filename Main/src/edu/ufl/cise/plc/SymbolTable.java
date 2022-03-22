package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.Declaration;

import java.util.HashMap;

public class SymbolTable {

    //TODO:  Implement a symbol table class that is appropriate for this language.
    HashMap<String, Declaration> table = new HashMap<>();


    public boolean insert(String name, Declaration declaration) {
        return (table.putIfAbsent(name, declaration) == null);
    }

    public Declaration lookup(String name) {
        return table.get(name);
    }


}


