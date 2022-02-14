package edu.ufl.cise.plc.test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import edu.ufl.cise.plc.*;

import java.util.Arrays;


public class LexerTests {

    ILexer getLexer(String input) throws LexicalException {
        return CompilerComponentFactory.getLexer(input);
    }

    //makes it easy to turn output on and off (and less typing than System.out.println)
    static final boolean VERBOSE = true;
    void show(Object obj) {
        if(VERBOSE) {
            System.out.println(obj);
        }
    }

    //check that this token has the expected kind
    void checkToken(IToken t, IToken.Kind expectedKind) {
        assertEquals(expectedKind, t.getKind());
    }

    //check that the token has the expected kind and position
    void checkToken(IToken t, IToken.Kind expectedKind, int expectedLine, int expectedColumn){
        assertEquals(expectedKind, t.getKind());
        assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
    }
    //check that the token has the expected kind and position and text
    void checkToken(IToken t, IToken.Kind expectedKind, int expectedLine, int expectedColumn, String expectedText){
        assertEquals(expectedKind, t.getKind());
        assertEquals(expectedText, t.getText());
        assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
    }

    //check that this token is an IDENT and has the expected name
    void checkIdent(IToken t, String expectedName){
        assertEquals(IToken.Kind.IDENT, t.getKind());
        assertEquals(expectedName, t.getText());
    }

    //check that this token is an IDENT, has the expected name, and has the expected position
    void checkIdent(IToken t, String expectedName, int expectedLine, int expectedColumn){
        checkIdent(t,expectedName);
        assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
    }

    //check that this token is an INT_LIT with expected int value
    void checkInt(IToken t, int expectedValue) {
        assertEquals(IToken.Kind.INT_LIT, t.getKind());
        assertEquals(expectedValue, t.getIntValue());
    }

    //check that this token  is an INT_LIT with expected int value and position
    void checkInt(IToken t, int expectedValue, int expectedLine, int expectedColumn) {
        checkInt(t,expectedValue);
        assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
    }

    void checkFloat(IToken t, float expectedValue){
        assertEquals(IToken.Kind.FLOAT_LIT, t.getKind());
        assertEquals(expectedValue, t.getFloatValue());
    }

    //check that this token is the EOF token
    void checkEOF(IToken t) {
        checkToken(t, IToken.Kind.EOF);
    }
    //The lexer should add an EOF token to the end.

    @Test
    public void testingErrors() throws LexicalException{
        String input = "a1244";
        show(input);
        ILexer lexer = getLexer(input);
        checkIdent(lexer.next(), "a");
        checkFloat(lexer.next(), (float) 124.4);
    }
    @Test
    void testEmpty() throws LexicalException {
        String input = "";
        show(input);
        ILexer lexer = getLexer(input);
        checkEOF(lexer.next());
    }

    //A couple of single character tokens
    @Test
    void testSingleChar0() throws LexicalException {
        String input = """
				+ 
				- 	 
				""";

        show(input);
        ILexer lexer = getLexer(input);
        checkToken(lexer.next(), IToken.Kind.PLUS, 0,0);
        checkToken(lexer.next(), IToken.Kind.MINUS, 1,0);
        checkEOF(lexer.next());
    }

    //comments should be skipped
    @Test
    void testComment0() throws LexicalException {
        //Note that the quotes around "This is a string" are passed to the lexer.
        String input = """
				"This is a string"
				#this is a comment
				*
				""";
        show(input);
        ILexer lexer = getLexer(input);
        checkToken(lexer.next(), IToken.Kind.STRING_LIT, 0,0);
        checkToken(lexer.next(), IToken.Kind.TIMES, 2,0);
        checkEOF(lexer.next());
    }

    //Example for testing input with an illegal character
    @Test
    void testError0() throws LexicalException {
        String input = "abc\\g xyz";
        show(input);
        ILexer lexer = getLexer(input);
        //this check should succeed
        checkIdent(lexer.next(), "abc");
        assertThrows(LexicalException.class, () -> {
            @SuppressWarnings("unused")
            IToken token = lexer.next();
        });
    }



    //Several identifiers to test positions
    @Test
    public void testIdent0() throws LexicalException {
        String input = """
				abc
				  def
				     ghi

				""";
        show(input);
        ILexer lexer = getLexer(input);
        checkIdent(lexer.next(), "abc", 0,0);
        checkIdent(lexer.next(), "def", 1,2);
        checkIdent(lexer.next(), "ghi", 2,5);
        checkEOF(lexer.next());
    }


    @Test
    public void testEquals0() throws LexicalException {
        String input = """
				= == ===
				""";
        show(input);
        ILexer lexer = getLexer(input);
        checkToken(lexer.next(),IToken.Kind.ASSIGN,0,0);
        checkToken(lexer.next(),IToken.Kind.EQUALS,0,2);
        checkToken(lexer.next(),IToken.Kind.EQUALS,0,5);
        checkToken(lexer.next(),IToken.Kind.ASSIGN,0,7);
        checkEOF(lexer.next());
    }

    @Test
    public void testLetters0() throws LexicalException {
        String input = """
				a bc 45d
				""";
        show(input);
        ILexer lexer = getLexer(input);
        checkIdent(lexer.next(),"a", 0,0);
        checkIdent(lexer.next(),"bc",0,2);
        checkInt(lexer.next(),45,0,5);
        checkIdent(lexer.next(),"d",0,7);
        checkEOF(lexer.next());
    }

    @Test
    public void testIdenInt() throws LexicalException {
        String input = """
				a123 456b
				""";
        show(input);
        ILexer lexer = getLexer(input);
        checkIdent(lexer.next(), "a123", 0,0);
        checkInt(lexer.next(), 456, 0,5);
        checkIdent(lexer.next(), "b",0,8);
        checkEOF(lexer.next());
    }


    //example showing how to handle number that are too big.
    @Test
    public void testIntTooBig() throws LexicalException {
        String input = """
				42
				99999999999999999999999999999999999999999999999999999999999999999999999
				""";
        ILexer lexer = getLexer(input);
        checkInt(lexer.next(),42);
        Exception e = assertThrows(LexicalException.class, () -> {
            lexer.next();
        });
    }

    @Test
    public void testFloatLit() throws LexicalException {
        String input = """
	3.4500000000000000
				""";
        ILexer lexer = getLexer(input);
        checkFloat(lexer.next(), 3.45f);
        }

    @Test
    public void testSingleZero() throws LexicalException {
        String input = """
    0
				""";
        ILexer lexer = getLexer(input);
        checkInt(lexer.next(), 0);
    }

    @Test
    public void testIdenIntWthMultipleLines() throws LexicalException {
        String input = """
				a123 456
				  a123 def
				""";
        show(input);
        ILexer lexer = getLexer(input);
        checkIdent(lexer.next(), "a123", 0,0);
        checkInt(lexer.next(), 456, 0,5);
        checkIdent(lexer.next(), "a123", 1,2);
        checkIdent(lexer.next(), "def", 1, 7);
        checkEOF(lexer.next());
    }

    @Test
    void testReservedWords() throws LexicalException {
        String input = """
			string CYAN
			int
			float
			boolean
			color
			image
			void
			getWidth
			getHeight
			getRed
			getGreen
			getBlue
			BLACK
			BLUE
			CYAN
			DARK_GRAY
			GRAY
			GREEN
			LIGHT_GRAY
			MAGENTA
			ORANGE
			PINK
			RED
			WHITE
			YELLOW
			true
			false
			if
			else
			fi
			write
			console	 
			""";
        show(input);
        ILexer lexer = getLexer(input);
        checkToken(lexer.next(), IToken.Kind.TYPE,		    0, 0, "string");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,    0, 7, "CYAN");
        checkToken(lexer.next(), IToken.Kind.TYPE,		    1, 0, "int");
        checkToken(lexer.next(), IToken.Kind.TYPE,		    2, 0, "float");
        checkToken(lexer.next(), IToken.Kind.TYPE,		    3, 0, "boolean");
        checkToken(lexer.next(), IToken.Kind.TYPE,		    4, 0, "color");
        checkToken(lexer.next(), IToken.Kind.TYPE,		    5, 0, "image");
        checkToken(lexer.next(), IToken.Kind.KW_VOID,	    6, 0, "void");
        checkToken(lexer.next(), IToken.Kind.IMAGE_OP,	    7, 0, "getWidth");
        checkToken(lexer.next(), IToken.Kind.IMAGE_OP,	    8, 0, "getHeight");
        checkToken(lexer.next(), IToken.Kind.COLOR_OP,	    9, 0, "getRed");
        checkToken(lexer.next(), IToken.Kind.COLOR_OP,	    10, 0, "getGreen");
        checkToken(lexer.next(), IToken.Kind.COLOR_OP,	    11, 0, "getBlue");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	12, 0, "BLACK");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	13, 0, "BLUE");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	14, 0, "CYAN");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	15, 0, "DARK_GRAY");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	16, 0, "GRAY");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	17, 0, "GREEN");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	18, 0, "LIGHT_GRAY");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	19, 0, "MAGENTA");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	20, 0, "ORANGE");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	21, 0, "PINK");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	22, 0, "RED");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	23, 0, "WHITE");
        checkToken(lexer.next(), IToken.Kind.COLOR_CONST,	24, 0, "YELLOW");
        checkToken(lexer.next(), IToken.Kind.BOOLEAN_LIT,	25, 0, "true");
        checkToken(lexer.next(), IToken.Kind.BOOLEAN_LIT,	26, 0, "false");
        checkToken(lexer.next(), IToken.Kind.KW_IF,        27, 0, "if");
        checkToken(lexer.next(), IToken.Kind.KW_ELSE,	    28, 0, "else");
        checkToken(lexer.next(), IToken.Kind.KW_FI,	    29, 0, "fi");
        checkToken(lexer.next(), IToken.Kind.KW_WRITE,	    30, 0, "write");
        checkToken(lexer.next(), IToken.Kind.KW_CONSOLE,	31, 0, "console");
        checkEOF(lexer.next());
    }

    @Test
    void testAllSymbolTokens() throws LexicalException {
        String input = """
			&
			|
			/
			*
			+
			(
			)
			[
			]
			!=
			==
			>=
			<=
			>>
			<<
			<-
			->
			%
			^
			,
			;
			!
			=
			-
			<
			>
			""";
        show(input);
        ILexer lexer = getLexer(input);
        checkToken(lexer.next(), IToken.Kind.AND,		0, 0);
        checkToken(lexer.next(), IToken.Kind.OR,		1, 0);
        checkToken(lexer.next(), IToken.Kind.DIV,		2, 0);
        checkToken(lexer.next(), IToken.Kind.TIMES,	3, 0);
        checkToken(lexer.next(), IToken.Kind.PLUS,		4, 0);
        checkToken(lexer.next(), IToken.Kind.LPAREN,	5, 0);
        checkToken(lexer.next(), IToken.Kind.RPAREN,	6, 0);
        checkToken(lexer.next(), IToken.Kind.LSQUARE,    7, 0);
        checkToken(lexer.next(), IToken.Kind.RSQUARE,	8, 0);
        checkToken(lexer.next(), IToken.Kind.NOT_EQUALS,	9, 0);
        checkToken(lexer.next(), IToken.Kind.EQUALS,    	10, 0);
        checkToken(lexer.next(), IToken.Kind.GE,         11, 0);
        checkToken(lexer.next(), IToken.Kind.LE,         12, 0);
        checkToken(lexer.next(), IToken.Kind.RANGLE,     13, 0);
        checkToken(lexer.next(), IToken.Kind.LANGLE,     14, 0);
        checkToken(lexer.next(), IToken.Kind.LARROW,     15, 0);
        checkToken(lexer.next(), IToken.Kind.RARROW,     16, 0);
        checkToken(lexer.next(), IToken.Kind.MOD,        17, 0);
        checkToken(lexer.next(), IToken.Kind.RETURN,     18, 0);
        checkToken(lexer.next(), IToken.Kind.COMMA,      19, 0);
        checkToken(lexer.next(), IToken.Kind.SEMI,       20, 0);
        checkToken(lexer.next(), IToken.Kind.BANG,       21, 0);
        checkToken(lexer.next(), IToken.Kind.ASSIGN,     22, 0);
        checkToken(lexer.next(), IToken.Kind.MINUS,      23, 0);
        checkToken(lexer.next(), IToken.Kind.LT,		24, 0);
        checkToken(lexer.next(), IToken.Kind.GT,		25, 0);
        checkEOF(lexer.next());
    }

    String getASCII(String s) {
        int[] ascii = new int[s.length()];
        for (int i = 0; i != s.length(); i++) {
            ascii[i] = s.charAt(i);
        }
        return Arrays.toString(ascii);
    }


    @Test
    public void testEscapeSequences0() throws LexicalException {
        String input = "\"\\b \\t \\n \\f \\r \" ";
        show(input);
        show("input chars= " + getASCII(input));
        ILexer lexer = getLexer(input);
        IToken t = lexer.next();
        String val = t.getStringValue();
        show("getStringValueChars=" + getASCII(val));
        String expectedStringValue = "\b \t \n \f \r ";
        show("expectedStringValueChars=" + getASCII(expectedStringValue));
        assertEquals(expectedStringValue, val);
        String text = t.getText();
        show("getTextChars= 	" +getASCII(text));
        String expectedText = "\"\\b \\t \\n \\f \\r \"";
        show("expectedTextChars="+getASCII(expectedText));
        assertEquals(expectedText,text);
    }

    @Test
    public void testEscapeSequences1() throws LexicalException {
        String input = "   \" ...  \\\"  \\\'  \\\\  \"";
        show(input);
        show("input chars= " + getASCII(input));
        ILexer lexer = getLexer(input);
        IToken t = lexer.next();
        String val = t.getStringValue();
        show("getStringValueChars= 	" + getASCII(val));
        String expectedStringValue = " ...  \"  \'  \\  ";
        show("expectedStringValueChars=" + getASCII(expectedStringValue));
        assertEquals(expectedStringValue, val);
        String text = t.getText();
        show("getTextChars= 	" +getASCII(text));
        String expectedText = "\" ...  \\\"  \\\'  \\\\  \""; //almost the same as input, but white space is omitted
        show("expectedTextChars="+getASCII(expectedText));
        assertEquals(expectedText,text);
    }

    @Test
    public void testBang() throws LexicalException{
        String input = """
        !=
        !!
        !=!
        !!=>>>=<-<<<
        """;
        show(input);
        ILexer lexer = getLexer(input);
        checkToken(lexer.next(), IToken.Kind.NOT_EQUALS, 0, 0);
        checkToken(lexer.next(), IToken.Kind.BANG, 1, 0);
        checkToken(lexer.next(), IToken.Kind.BANG, 1, 1);
        checkToken(lexer.next(), IToken.Kind.NOT_EQUALS, 2, 0);
        checkToken(lexer.next(), IToken.Kind.BANG, 2, 2);
        checkToken(lexer.next(), IToken.Kind.BANG, 3, 0 );
        checkToken(lexer.next(), IToken.Kind.NOT_EQUALS, 3, 1);
        checkToken(lexer.next(), IToken.Kind.RANGLE, 3, 3);
        checkToken(lexer.next(), IToken.Kind.GE, 3, 5);
        checkToken(lexer.next(), IToken.Kind.LARROW, 3,7);
        checkToken(lexer.next(), IToken.Kind.LANGLE, 3, 9);
        checkToken(lexer.next(), IToken.Kind.LT, 3, 11);
        checkEOF(lexer.next());
    }

    @Test
    public void testCodeExample() throws LexicalException{
        String input = """
  		string a = "hello\\nworld";
  		int size = 11;
  		string b = "";
  		boolean display = true;
  		
	 	for (int i = size - 1;i >= 0; i++) [
	 		b = b + a[i];
	 	]

  		if (display == true)
  		print(b);
	  	""";
        show(input);
        ILexer lexer = getLexer(input);
        checkToken(lexer.next(), IToken.Kind.TYPE, 0, 0, "string");
        checkToken(lexer.next(), IToken.Kind.IDENT, 0, 7, "a");
        checkToken(lexer.next(), IToken.Kind.ASSIGN, 0, 9, "=");
        checkToken(lexer.next(), IToken.Kind.STRING_LIT, 0, 11, "\"hello\\nworld\"");
        checkToken(lexer.next(), IToken.Kind.SEMI, 0, 25);

        checkToken(lexer.next(), IToken.Kind.TYPE, 1, 0, "int");
        checkToken(lexer.next(), IToken.Kind.IDENT, 1, 4, "size");
        checkToken(lexer.next(), IToken.Kind.ASSIGN, 1, 9, "=");
        checkInt(lexer.next(), 11, 1, 11);
        checkToken(lexer.next(), IToken.Kind.SEMI, 1, 13);

        checkToken(lexer.next(), IToken.Kind.TYPE);
        checkToken(lexer.next(), IToken.Kind.IDENT);
        checkToken(lexer.next(), IToken.Kind.ASSIGN);
        checkToken(lexer.next(), IToken.Kind.STRING_LIT);
        checkToken(lexer.next(), IToken.Kind.SEMI);

        checkToken(lexer.next(), IToken.Kind.TYPE, 3, 0, "boolean");
        checkToken(lexer.next(), IToken.Kind.IDENT, 3, 8, "display");
        checkToken(lexer.next(), IToken.Kind.ASSIGN, 3, 16, "=");
        checkToken(lexer.next(), IToken.Kind.BOOLEAN_LIT, 3, 18, "true");
        checkToken(lexer.next(), IToken.Kind.SEMI, 3, 22);

        checkToken(lexer.next(), IToken.Kind.IDENT, 5, 0, "for");
        checkToken(lexer.next(), IToken.Kind.LPAREN);
        checkToken(lexer.next(), IToken.Kind.TYPE);
        checkToken(lexer.next(), IToken.Kind.IDENT);
        checkToken(lexer.next(), IToken.Kind.ASSIGN);
        checkToken(lexer.next(), IToken.Kind.IDENT);
        checkToken(lexer.next(), IToken.Kind.MINUS);
        checkInt(lexer.next(), 1);


        checkToken(lexer.next(), IToken.Kind.SEMI);
        checkToken(lexer.next(), IToken.Kind.IDENT);
        checkToken(lexer.next(), IToken.Kind.GE);
        checkInt(lexer.next(), 0);
        checkToken(lexer.next(), IToken.Kind.SEMI);
        checkToken(lexer.next(), IToken.Kind.IDENT);
        checkToken(lexer.next(), IToken.Kind.PLUS);
        checkToken(lexer.next(), IToken.Kind.PLUS);
        checkToken(lexer.next(), IToken.Kind.RPAREN);
        checkToken(lexer.next(), IToken.Kind.LSQUARE);

        checkToken(lexer.next(), IToken.Kind.IDENT);
        checkToken(lexer.next(), IToken.Kind.ASSIGN);
        checkToken(lexer.next(), IToken.Kind.IDENT);
        checkToken(lexer.next(), IToken.Kind.PLUS);
        checkToken(lexer.next(), IToken.Kind.IDENT);
        checkToken(lexer.next(), IToken.Kind.LSQUARE);
        checkToken(lexer.next(), IToken.Kind.IDENT);
        checkToken(lexer.next(), IToken.Kind.RSQUARE);
        checkToken(lexer.next(), IToken.Kind.SEMI);

        checkToken(lexer.next(), IToken.Kind.RSQUARE);

        checkToken(lexer.next(), IToken.Kind.KW_IF);
        checkToken(lexer.next(), IToken.Kind.LPAREN);
        checkToken(lexer.next(), IToken.Kind.IDENT);
        checkToken(lexer.next(), IToken.Kind.EQUALS);
        checkToken(lexer.next(), IToken.Kind.BOOLEAN_LIT);
        checkToken(lexer.next(), IToken.Kind.RPAREN);
        checkToken(lexer.next(), IToken.Kind.IDENT);
        checkToken(lexer.next(), IToken.Kind.LPAREN);
        checkToken(lexer.next(), IToken.Kind.IDENT);
        checkToken(lexer.next(), IToken.Kind.RPAREN);
        checkToken(lexer.next(), IToken.Kind.SEMI);

        checkEOF(lexer.next());
    }

}
