package edu.ufl.cise.plc.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import edu.ufl.cise.plc.runtime.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import edu.ufl.cise.plc.TypeCheckException;
import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.Program;
import edu.ufl.cise.plc.runtime.javaCompilerClassLoader.PLCLangExec;

class Assignment6StarterTest {

	// Default package name for generated code
	String packageName = "cop4020sp22Package";
//	String packageName = "";

	boolean VERBOSE = true;

	private Object show(Object obj) throws IOException {
		if (VERBOSE) {
			if (obj instanceof BufferedImage) {
				show((BufferedImage) obj);
				pauseImageDisplay();
			} else {
				System.out.println(obj);
			}
		}
		return obj;
	}

	private BufferedImage show(BufferedImage image) {
		if (VERBOSE) {
			ConsoleIO.displayImageOnScreen(image);
			try {
				pauseImageDisplay();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return image;
	}

	/**
	 * This is the same as show(BufferedImage) except that the image is placed in a different location: 
	 * [0,0] is in center of screen rather than the upper left corner.
	 */

	private BufferedImage showRef(BufferedImage image) {
		if (VERBOSE) {
			ConsoleIO.displayReferenceImageOnScreen(image);
			try {
				pauseImageDisplay();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return image;
	}

	boolean PAUSE = true;
	int MILLIS = 2000;

	private void pauseImageDisplay() throws IOException {
		if (PAUSE) {
			/*
			 * If you would like Junit test to pause for input, uncomment The next two lines
			 * and comment the try-catch block.
			 */
//			System.out.print("Enter any character to terminate: ");
//			System.in.read();
			try {
				Thread.sleep(MILLIS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	static final float DELTA = .001f;

	String getName(ASTNode ast) throws Exception {
		if (ast instanceof Program) {
			return ((Program) ast).getName();
		} else
			throw new Exception("bug--expected ast was program");
	}

	String getReturnType(ASTNode ast) throws Exception {
		if (ast instanceof Program) {
			return ((Program) ast).getReturnType().toString();
		} else
			throw new Exception("bug--expected ast was program");
	}

	Object check(String input, Object expectedResult) throws Exception {
		return check(input, null, expectedResult);
	}

	Object check(String input, Object[] params, Object expected) throws Exception {
		Object actual = exec(input, params);
		if (expected instanceof BufferedImage) {
			BufferedImage actualImage = (BufferedImage) actual;
			BufferedImage expectedImage = (BufferedImage) expected;
			int[] actualPixels = ImageOps.getRGBPixels(actualImage);
			int[] expectedPixels = ImageOps.getRGBPixels(expectedImage);
			assertArrayEquals(expectedPixels, actualPixels);
		} else {
			assertEquals(expected, actual);
		}
		return actual;
	}

	Object exec(String input, Object[] params) throws Exception {
		return (new PLCLangExec(packageName, VERBOSE)).exec(input, params);
	}

	Object exec(String input) throws Exception {
		return (new PLCLangExec(packageName, VERBOSE)).exec(input, null);
	}

	// need separate method to check floats due to DELTA. Note in simple cases, just
	// use normal check.
	void checkFloatResult(String input, float expectedResult) throws Exception {
		checkFloatResult(input, null, expectedResult);
	}

	void checkFloatResult(String input, Object[] params, float expectedResult) throws Exception {
		Object result = exec(input, params);
		show("result = " + result);
		assertEquals(expectedResult, (float) result, DELTA);
	}

	@AfterEach
	public void closeFiles() {
		FileURLIO.closeFiles();
	}

	@Test
	void lectureExample3() throws Exception {
		String input = """
				image a(string url, int width, int height)
				      image[width,height] b <- url;
				      ^b;
				      """;
		String url = "https://upload.wikimedia.org/wikipedia/commons/9/92/Albert_and_Alberta.jpg";
		Object[] params = { url, 300, 200 };
		BufferedImage refImage = showRef(FileURLIO.readImage(url, 300, 200));
		show(
				check(input, params, refImage));
	}

	@Test
	void lectureExample4() throws Exception {
		String input = """
				image f(int widthAndHeight) 
							image[widthAndHeight,widthAndHeight] a;
				            a[x,y] = <<x-y, 0, y-x>>;
				            ^ a;
				""";
		int size = 500;
		// create reference image
		BufferedImage refImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				refImage.setRGB(x, y, (new ColorTuple(x - y, 0, y - x)).pack());
			}
		}
		// run PLCLang program
		Object[] params = { size };
		show(check(input, params, refImage));
	    //If you don't want to compare with ref image use show(exec(input,params));
	}
	
	
	@Test
	void lectureExample5() throws Exception {
		String input = """
				image flag(int size)
				   image[size,size] f;
				   int stripSize = size/2;
				   f[x,y] = if (y > stripSize) YELLOW else BLUE fi;
				   ^f;
				   """;
		int size = 1024;		
		BufferedImage refImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				refImage.setRGB(x, y, (y>size/2)? Color.YELLOW.getRGB() : Color.BLUE.getRGB());
			}
		}
		Object[] params = { size };
		show(check(input, params, refImage));
	}
	
	@Test
	void lectureExample6() throws Exception {
		String input = """
				image f(string url)
                image a <- url;
                write a -> console;
                image b = a/3;
                ^b;

				""";
		String url = "https://upload.wikimedia.org/wikipedia/commons/9/92/Albert_and_Alberta.jpg";
		BufferedImage inputImage = FileURLIO.readImage(url);
		int w = inputImage.getWidth();
		int h = inputImage.getHeight();
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++) {
				ColorTuple pixel = ColorTuple.unpack(inputImage.getRGB(x, y));
				int newPackedPixel = (new ColorTuple(pixel.red/3,pixel.green/3,pixel.blue/3)).pack();
				refImage.setRGB(x, y, newPackedPixel);
			}
		Object[] params = {url};
		ConsoleIO.displayReferenceImageOnScreen(inputImage);
		//this image should be the same size, but darker than inputImage
		show(check(input, params, refImage));
	}
	
	


	@Test
	void colorArithmetic0() throws Exception {
		String input = """
				color f()
				color a = <<50,60,70>>;
				color b = <<13,14,15>>;
				^ a + b;
				""";
		check(input, new edu.ufl.cise.plc.runtime.ColorTuple(50 + 13, 60 + 14, 70 + 15));
	}

	@Test
	void imageArithemtic0() throws Exception {
		String input = """
				image testImageArithemtic0()
				image[500,500] blue;
				blue[x,y] = BLUE;
				image[500,500] green;
				green[a,b] = GREEN;
				image[500,500] teal;
				teal[x,y] = blue[x,y] + green[x,y];
				^teal;
				""";
		int w = 500;
		int h = 500;
		int size = w*h;
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		int teal = Color.GREEN.getRGB() | Color.BLUE.getRGB(); //bitwise or, teal = ff00ffff
		System.out.println("teal=" + Integer.toHexString(teal));
		int[] rgbArray = new int[size];
		Arrays.fill(rgbArray, teal);
		refImage.setRGB(0, 0, w,h, rgbArray, 0, w);
		show(check(input, refImage));
	}

	@Test
	void colorBool() throws Exception {
		String input = """
				boolean f()
				color a = <<50,60,70>>;
				color b = <<13,14,15>>;
				^ a == b;
				""";
		check(input, false);
		String input2 = """
				boolean f()
				color a = <<50,60,70>>;
				color b = <<13,14,15>>;
				^ a != b;
				""";
		check(input2, true);
	}


	//Possibly fixed, verify first
	@Test
	void testAssigningOneValueToImage() throws Exception{
		String input = """
        image f()
              image[500, 500] b;
              b = 100;
              ^b;
        """;
		int w = 500;
		int h = 500;
		int size = w*h;
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		int[] rgbArray = new int[size];
		Color hundred = new Color(100, 100, 100);
		Arrays.fill(rgbArray, hundred.getRGB());
		refImage.setRGB(0, 0, w,h, rgbArray, 0, w);
		show(check(input, refImage));
	}
	@Test
	void testAssigningOneColorToImage() throws Exception{
		String input = """
        image f()
              image[500, 500] b;
              b = BLUE;
              ^b;
        """;
		int w = 500;
		int h = 500;
		int size = w*h;
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		int blue = Color.BLUE.getRGB();
		int[] rgbArray = new int[size];
		Arrays.fill(rgbArray, blue);
		refImage.setRGB(0, 0, w,h, rgbArray, 0, w);
		ConsoleIO.displayReferenceImageOnScreen(refImage);
		show(check(input, refImage));
	}


	@Test
	void testAssignImageToImageWithDimension() throws Exception{
		String input = """
        image f()
              image[500, 500] b;
              b = BLUE;
              image[200, 200] c;
              c = b;
              ^c;
        """;
		int w = 200;
		int h = 200;
		int size = w*h;
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
//		int blue = Color.BLUE.getRGB();
		int blue = ColorTuple.toColorTuple(Color.BLUE).pack(); //if above one doesn't work
		int[] rgbArray = new int[size];
		Arrays.fill(rgbArray, blue);
		refImage.setRGB(0, 0, w,h, rgbArray, 0, w);
		//ConsoleIO.displayReferenceImageOnScreen(refImage);
		show(check(input, refImage));
	}



	/**
	 * If not declared with a size, the image <name>  takes the size of the right hand side image.
	 * If <expr> is an identExpr, the rhs image is cloned using ImageOps.clone (FROM DOCUMENT)
	 */
	@Test
	void testAssignImageToImageWithoutDimension() throws Exception{
		String input = """
        image f(string url)
              image b <- url;
              image[300, 300] c = RED;
              b = c;
              ^b;
        """;
		int w = 300;
		int h = 300;
		int size = w*h;
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		int blue = Color.RED.getRGB();
		//int blue = ColorTuple.toColorTuple(Color.BLUE).pack(); //if above one doesn't work
		int[] rgbArray = new int[size];
		Arrays.fill(rgbArray, blue);
		refImage.setRGB(0, 0, w,h, rgbArray, 0, w);
		//ConsoleIO.displayReferenceImageOnScreen(refImage);
		String url = "https://upload.wikimedia.org/wikipedia/commons/9/92/Albert_and_Alberta.jpg";
		Object[] params = {url};
		//this image should be the same size, but darker than inputImage
		show(check(input, params, refImage));
	}





	// Enter 100 200 100
	@Test
	void readColorFromConsole() throws Exception{
		String input = """
        image f()
              image[500, 500] b;
              color x;
              x <- console;
              b = x;
              ^b;
        """;
		int w = 500;
		int h = 500;
		int size = w*h;
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		int blue = Color.BLUE.getRGB();
		//int blue = ColorTuple.toColorTuple(Color.BLUE).pack(); //if above one doesn't work
		int[] rgbArray = new int[size];
		Color hundred = new Color(100, 200, 100);
		Arrays.fill(rgbArray, hundred.getRGB());
		refImage.setRGB(0, 0, w,h, rgbArray, 0, w);
		//ConsoleIO.displayReferenceImageOnScreen(refImage);
		show(check(input, refImage));
	}
	@Test
	void testDeclaringOneValueToImage() throws Exception{
		String input = """
      image f()
            image[500, 500] b = 100;
            ^b;
      """;
		int w = 500;
		int h = 500;
		int size = w*h;
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		int[] rgbArray = new int[size];
		Color hundred = new Color(100, 100, 100);
		Arrays.fill(rgbArray, hundred.getRGB());
		refImage.setRGB(0, 0, w,h, rgbArray, 0, w);
		show(check(input, refImage));
	}



	@Test
	void testBinaryImageImageOp() throws Exception {
		String input = """
				image f()
				    image[300, 300] a = RED;
				        	image[300, 300] b = BLUE;
				        	image c = a+b;
				        	^c;
				""";

		int w = 300;
		int h = 300;
		int size = w * h;
		BufferedImage image1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		BufferedImage image2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

		int red = Color.RED.getRGB();
		int blue = Color.BLUE.getRGB();
		int[] rgbRedArray = new int[size];
		int[] rgbBlueArray = new int[size];
		Arrays.fill(rgbRedArray, red);
		Arrays.fill(rgbBlueArray, blue);
		image1.setRGB(0, 0, w, h, rgbRedArray, 0, w);
		image2.setRGB(0, 0, w, h, rgbBlueArray, 0, w);

		BufferedImage refImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++) {
				ColorTuple pixel1 = ColorTuple.unpack(image1.getRGB(x, y));
				ColorTuple pixel2 = ColorTuple.unpack(image2.getRGB(x, y));
				int newPackedPixel = (new ColorTuple(pixel1.red + pixel2.red, pixel1.green + pixel2.green,
						pixel1.blue + pixel2.blue)).pack();
				refImage.setRGB(x, y, newPackedPixel);
			}
		ConsoleIO.displayReferenceImageOnScreen(image1);
		ConsoleIO.displayReferenceImageOnScreen(image2);

		show(check(input, null, refImage));
	}
	@Test
	void testImageEqualsTrue() throws Exception {
		String input = """
				boolean f()
				    image[300, 300] a = RED;
				      	image[300, 300] b = RED;
				      	^ a == b;
				""";

		int w = 300;
		int h = 300;
		int size = w * h;
		BufferedImage image1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		BufferedImage image2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

		int red = Color.RED.getRGB();
		int[] rgbRedArray = new int[size];
		Arrays.fill(rgbRedArray, red);
		image1.setRGB(0, 0, w, h, rgbRedArray, 0, w);
		image2.setRGB(0, 0, w, h, rgbRedArray, 0, w);

		ConsoleIO.displayReferenceImageOnScreen(image1);
		ConsoleIO.displayReferenceImageOnScreen(image2);

		check(input, true);
	}

	@Test
	void testImageEqualsFalse() throws Exception {
		String input = """
				boolean f()
				    image[300, 300] a = RED;
				      	image[300, 300] b = BLUE;


				      	^ a == b;
				""";

		int w = 300;
		int h = 300;
		int size = w * h;
		BufferedImage image1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		BufferedImage image2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

		int red = Color.RED.getRGB();
		int blue = Color.BLUE.getRGB();
		int[] rgbRedArray = new int[size];
		int[] rgbBlueArray = new int[size];
		Arrays.fill(rgbRedArray, red);
		Arrays.fill(rgbBlueArray, blue);
		image1.setRGB(0, 0, w, h, rgbRedArray, 0, w);
		image2.setRGB(0, 0, w, h, rgbBlueArray, 0, w);

		ConsoleIO.displayReferenceImageOnScreen(image1);
		ConsoleIO.displayReferenceImageOnScreen(image2);

		check(input, false);

	}





/*
Import Tests
Series of tests that checks for imports; you must manually check by looking at your generated code.
*/

	@Test
		// must import java.awt.image.BufferedImage from parameter
	void importTest1() throws Exception {
		String input = """
				void a(image b)
				      int c = 0;
				      """;
		String url = "https://upload.wikimedia.org/wikipedia/commons/9/92/Albert_and_Alberta.jpg";
		BufferedImage inputImage = FileURLIO.readImage(url);
		Object[] params = {inputImage};
		exec(input, params);
	}



	@Test
		// must import edu.ufl.cise.plc.runtime.ColorTuple from parameter
	void importTest2() throws Exception {
		String input = """
				void a(color b)
				      int c = 0;
				      """;
		ColorTuple inputColor = new ColorTuple(0);
		Object[] params = {inputColor};
		exec(input, params);
	}


	@Test
		// must import edu.ufl.cise.plc.runtime.ColorTuple from first declaration
		// must import java.awt.Color from first declaration
		// must import edu.ufl.cise.plc.runtime.ImageOps if using the binaryImageScalarOp function

	void importTest3() throws Exception {
		String input = """
				void a()
				      color b = BLUE;
				      b = b * 3;
				      """;
		exec(input);
	}


	@Test
		// must import java.awt.image.BufferedImage for image declaration
		// must import edu.ufl.cise.plc.runtime.ImageOps if using the setColor function when initializing image
		// must import edu.ufl.cise.plc.runtime.ColorTuple when converting colorfloat to color
		// must import edu.ufl.cise.plc.runtime.ColorTupleFloat for the expression assigned to image
		// must import edu.ufl.cise.plc.runtime.ConsoleIO for write assignment
	void importTest4() throws Exception {
		String input = """
				void a()
					image[200,200] b = <<0.0, 255.0, 0.0>>;
					write b -> console;
				""";
		exec(input);
		//pauseImageDisplay();
	}



	@Test
		// first program:
		// must import edu.ufl.cise.plc.runtime.ColorTuple for first declaration
		// must import edu.ufl.cise.plc.runtime.FileURLIO for write assignment

		// second program:
		// must import edu.ufl.cise.plc.runtime.ColorTuple for first declaration
		// must import import edu.ufl.cise.plc.runtime.FileURLIO for read assignment
		// must import edu.ufl.cise.plc.runtime.ConsoleIO for write assignment
	void importTest5() throws Exception {
		String input1 = """
				void a()
					color b = <<100,100,100>>;
					write b -> "colorFile";
				""";
		exec(input1);
		File file = new File("colorFile");
		assertEquals(true, file.exists());

		String input2 = """
				void c()
					color d <- "colorFile";
					int e = getRed d;
					write e -> console;
				""";
		exec(input2);
	}






	@Test
		// hint: input https://upload.wikimedia.org/wikipedia/commons/9/92/Albert_and_Alberta.jpg in console for code to work
		// must import java.awt.image.BufferedImage for first declaration
		// must import edu.ufl.cise.plc.runtime.ConsoleIO for read assignment
		// must import edu.ufl.cise.plc.runtime.FileURLIO for read assignment


	void importTest6() throws Exception {
		String input = """
				void a()
					image b <- console;
				""";
		exec(input);
	}





/*
Unsuccessful Tests
List of tests that must give exceptions
*/

	@Test
		// must give a syntaxException
	void unsuccessful1() throws Exception {
		String input = """
				colorfloat a()
					^ <<100.0, 250.0, 0.0>>;
				""";
		Exception e = assertThrows(Exception.class, () -> {
			exec(input);
		});

	}




	@Test
		// must give a TypeCheckException
	void unsuccessful2() throws Exception {
		String input = """
				color a()
					^ <<100.0, 250.0, 0.0>>;
				""";
		Exception e = assertThrows(Exception.class, () -> {
			exec(input);
		});

	}




	@Test
		// first program should work
		// second program must give a ClassCastException or a TypeCheckException (depending on how your TypeCheckVisitor works)
	void unsuccessful3() throws Exception{
		String input1 = """
				void a()
					color b = <<100,100,100>>;
					write (b / 1.0) -> "colorFloatFile";
				""";
		exec(input1);
		File file = new File("colorFloatFile");
		assertEquals(true, file.exists());
		String input2 = """
				void c()
					color d <- "colorFloatFile";
				""";
		Exception e = assertThrows(Exception.class, () -> {
			exec(input2);
		});
	}







			//tests that got deleted

			// Have to create setColor method with ColorTupleFloat
	@Test
	void testColorExpressions() throws Exception {
		String input = """
       image a(int width, int height)
             image[width, height] f;
             float x = width;
             float y = height;
             f[g,h] = <<(g/x*255), 0.0, (h/y*255)>>;
             ^f;
             """;
		int width = 640;
		int height = 480;
		Object[] params = {width, height};
		BufferedImage refImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				refImage.setRGB(x, y, (new ColorTupleFloat(x/(float)width*255, 0.0f, y/(float)height*255)).pack());
			}
		}
		showRef(refImage);
		show(check(input, params, refImage));
	}




	@Test
	void colorArithmetic3() throws Exception {
		String input = """
       color f()
       image[100,100] a;
       a[x,y] = 10;
       ^ a[0, 0];
       """;
		check(input, new edu.ufl.cise.plc.runtime.ColorTuple(10, 10, 10));
	}



	@Test
	void testExtractRed() throws Exception {
		String input = """
    image f(string url)
             image a <- url;
             image b = getRed(a);
             ^b;

       """;
		String url = "https://upload.wikimedia.org/wikipedia/commons/9/92/Albert_and_Alberta.jpg";
		BufferedImage inputImage = FileURLIO.readImage(url);
		int w = inputImage.getWidth();
		int h = inputImage.getHeight();
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++) {
				ColorTuple pixel = ColorTuple.unpack(inputImage.getRGB(x, y));
				int newPackedPixel = (new ColorTuple(pixel.red,0,0)).pack();
				refImage.setRGB(x, y, newPackedPixel);
			}
		Object[] params = {url};
		show(check(input, params, refImage));
	}









	@Test
	void testGetRGBFromColor() throws Exception {
		String input = """
    int f()
             color a = <<10, 20, 30>>;
             int r = getRed(a);
             int g = getBlue(a);
             int b = getGreen(a);
             ^r + g + b;

       """;

		show(check(input, 60));
	}


	@Test
	void testWriteToFile() throws Exception {
		String input = """
       void f(string url)
          image b <- url;
          image c = getBlue(b);
          write c -> "blueImage";
          """;

		String url = "https://upload.wikimedia.org/wikipedia/commons/9/92/Albert_and_Alberta.jpg";
		BufferedImage inputImage = FileURLIO.readImage(url);
		int w = inputImage.getWidth();
		int h = inputImage.getHeight();
		BufferedImage refImage = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++) {
				ColorTuple pixel = ColorTuple.unpack(inputImage.getRGB(x, y));
				int newPackedPixel = (new ColorTuple(0,0,pixel.blue)).pack();
				refImage.setRGB(x, y, newPackedPixel);
			}

		Object[] params = {url};
		exec(input, params);
		ConsoleIO.displayReferenceImageOnScreen(refImage);
		File file = new File("blueImage.jpeg");
		assertEquals(true, file.exists());
	}

	@Test
	void testWriteTextToFile() throws Exception {
		String input = """
       void f()
          string message = "Hello World!";
          write message -> "textFile";
          """;



		exec(input);
//writeValue writes to file with no extension
		File file = new File("textFile");
		assertEquals(true, file.exists());

//readValueFromFile returns a string
		Object File = FileURLIO.readValueFromFile("textFile");
		assertEquals("Hello World!", File);

	}






}
