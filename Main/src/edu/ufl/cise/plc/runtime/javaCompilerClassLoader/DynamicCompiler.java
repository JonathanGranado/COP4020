package edu.ufl.cise.plc.runtime.javaCompilerClassLoader;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.util.ArrayList;
import java.util.List;

public class DynamicCompiler {
	
	
	/** Compiles java source code provided in the form a  String and returns the class file in the form of a byte array. */
	public static byte[] compile(String fullyQualifiedName, String sourceCode) throws Exception {
		
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		InMemoryClassFileManager fileManager = new InMemoryClassFileManager(compiler.getStandardFileManager(null, null, null));
		
		List<JavaFileObject> sourceFiles = new ArrayList<>();
		sourceFiles.add(new StringJavaFileObject(fullyQualifiedName, sourceCode));

		List<String> options = new ArrayList();
		options.add("--release");
		options.add("15");
		options.add("--enable-preview");

		boolean success = compiler.getTask(null, fileManager, null, options, null, sourceFiles).call();
		if (success) {
			return fileManager.byteCodeObject.getBytes();
		}
		else throw new Exception("error compiling generated code");
		}
	}
	

