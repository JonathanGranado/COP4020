package edu.ufl.cise.plc;
import java.util.*;
public class Token implements IToken {


    Map<String, Kind> reservedMap = new HashMap<>();

    // Inserting reserved words into map

    public Map<String, Kind> getReservedMap() {
        // <type>
        reservedMap.put("int", Kind.TYPE);
        reservedMap.put("float", Kind.TYPE);
        reservedMap.put("string", Kind.TYPE);
        reservedMap.put("boolean", Kind.TYPE);
        reservedMap.put("color", Kind.TYPE);
        reservedMap.put("image", Kind.TYPE);
        reservedMap.put("void", Kind.KW_VOID);
        // <image_op>
        reservedMap.put("getWidth", Kind.IMAGE_OP);
        reservedMap.put("getHeight", Kind.IMAGE_OP);
        // <color_op>
        reservedMap.put("BLACK", Kind.COLOR_CONST);
        reservedMap.put("BLUE", Kind.COLOR_CONST);
        reservedMap.put("CYAN", Kind.COLOR_CONST);
        reservedMap.put("DARK_GRAY", Kind.COLOR_CONST);
        reservedMap.put("GRAY", Kind.COLOR_CONST);
        reservedMap.put("GREEN", Kind.COLOR_CONST);
        reservedMap.put("LIGHT_GRAY", Kind.COLOR_CONST);
        reservedMap.put("MAGENTA", Kind.COLOR_CONST);
        reservedMap.put("ORANGE", Kind.COLOR_CONST);
        reservedMap.put("PINK", Kind.COLOR_CONST);
        reservedMap.put("RED", Kind.COLOR_CONST);
        reservedMap.put("WHITE", Kind.COLOR_CONST);
        reservedMap.put("YELLOW", Kind.COLOR_CONST);
        // <boolean_lit>
        reservedMap.put("true", Kind.BOOLEAN_LIT);
        reservedMap.put("false", Kind.BOOLEAN_LIT);
        // <other_keyword>
        reservedMap.put("if", Kind.KW_IF);
        reservedMap.put("fi", Kind.KW_FI);
        reservedMap.put("else", Kind.KW_ELSE);
        reservedMap.put("write", Kind.KW_WRITE);
        reservedMap.put("console", Kind.KW_WRITE);

        return reservedMap;
    }

    public boolean isReserved(String str){
        return reservedMap.containsKey(str);
    }
    @Override
    public Kind getKind() {
        return null;
    }

    @Override
    public String getText() {
        return null;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return null;
    }

    @Override
    public int getIntValue() {
        return 0;
    }

    @Override
    public float getFloatValue() {
        return 0;
    }

    @Override
    public boolean getBooleanValue() {
        return false;
    }

    @Override
    public String getStringValue() {
        return null;
    }
}
