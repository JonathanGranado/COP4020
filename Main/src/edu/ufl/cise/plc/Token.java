package edu.ufl.cise.plc;

import java.util.HashMap;
import java.util.Map;

public class Token implements IToken {


    final Kind kind;
    final String input;
    final int pos;
    final int length;
    Map<String, Kind> reservedMap = new HashMap<>();

    public Token(Kind _kind, String _input, int _pos, int _length) {
        this.kind = _kind;
        this.input = _input;
        this.pos = _pos;
        this.length = _length;
    }


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

    public boolean isReserved(String str) {
        return reservedMap.containsKey(str);
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getText() {
        return input.substring(pos, pos + length);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return null;
    }

    @Override
    public int getIntValue() {
        if (kind == Kind.INT_LIT) {
            return Integer.parseInt(getText());
        } else {
            System.out.println("ERROR. Token is not INT_LIT");
        }
    return -1;
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
