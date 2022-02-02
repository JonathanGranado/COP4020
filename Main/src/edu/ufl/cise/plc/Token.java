package edu.ufl.cise.plc;
import java.lang.reflect.Array;
import java.util.*;
public class Token implements IToken {

    final Kind kind;
    final String input;
    final int pos;
    final int lineNumber;
    final int length;
    SourceLocation srcLocation;

    public Token(Kind _kind,String _input, int _pos, int _length, int _lineNumber){
        this.kind = _kind;
        this.input = _input;
        this.pos = _pos;
        this.length = _length;
        this.lineNumber = _lineNumber;
        this.srcLocation = new SourceLocation(_lineNumber, _pos);

    }



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
        reservedMap.put("getRed", Kind.COLOR_OP);
        reservedMap.put("getBlue", Kind.COLOR_OP);
        reservedMap.put("getGreen", Kind.COLOR_OP);
        // <color_const>
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
        return kind;
    }

    @Override
    public String getText() {
        System.out.println("pos is " + pos + " length is " + length + " length of input " + input.length());
        String text = input.substring(0, length);
        System.out.println(text);
        return text;
    }

    @Override
    public SourceLocation getSourceLocation() {

        return this.srcLocation;
    }

    @Override
    public int getIntValue() {
        if(kind == Kind.INT_LIT){
            return Integer.parseInt(getText());
        }else{
            System.out.println("ERROR, Token is not INT_LIT");
        }
        return -1;
    }

    @Override
    public float getFloatValue() {
        if (kind == Kind.FLOAT_LIT) {
            return Float.parseFloat(getText());
        } else {
            System.out.println("ERROR. Token is not INT_LIT");
        }
        return -1;
    }

    @Override
    public boolean getBooleanValue() {
        String substring = input.substring(pos, pos + length);
        if (kind == Kind.BOOLEAN_LIT) {
            if (substring.equals("true")) {
                return true;
            } else if (substring.equals("false")) {
                return false;
            }
        } else {
            System.out.println("ERROR. Token is not BOOLEAN_LIT");
        }
        return false;
    }

    @Override
    //returns the String represented by the characters of this token if kind is STRING_LIT
    //The delimiters should be removed and escape sequences replaced by the characters they represent.
    //TODO: set it up so the escape sequences are represented correctly
    public String getStringValue() {
        StringBuilder subString = new StringBuilder(input.substring(pos, pos + length));
        String outPut = "";
        if(kind == Kind.STRING_LIT){
            for(int i = 0; i < input.length(); i++){
                if(input.charAt(i) == ' '){
                    i++;
                }
                if(input.charAt(i) == '\n'){
                    subString.append("\n");
                }
                if(input.charAt(i) == '\t'){
                    subString.append("\t");
                }
                if(input.charAt(i) == '\b'){
                    subString.append("\b");
                }
                if(input.charAt(i) == '\f'){
                    subString.append("\f");
                }
                if(input.charAt(i) == '\r'){
                    subString.append("\r");
                }
            }
        }
        return null;
    }

}

