package com.ibm.commerce.jpa.port.util;

/*
 *-----------------------------------------------------------------
 * Copyright 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *-----------------------------------------------------------------
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

public class JavaUtil {
	private static String COMMENT_START = "/*";
	private static final String COMMENT_END = "*/";
	private static final String COMMERCE_COPYRIGHT_COMMENT = "\r\n" + COMMENT_START +
		"\r\n *-----------------------------------------------------------------\r\n" +
		" * IBM Confidential\r\n" +
		" *\r\n" +
		" * OCO Source Materials\r\n" +
		" *\r\n" +
		" * WebSphere Commerce\r\n" +
		" *\r\n" +
		" * (C) Copyright IBM Corp. 2015\r\n" +
		" *\r\n" +
		" * The source code for this program is not published or otherwise\r\n" +
		" * divested of its trade secrets, irrespective of what has\r\n" +
		" * been deposited with the U.S. Copyright Office.\r\n" +
		" *-----------------------------------------------------------------\r\n" +
		" " + COMMENT_END + "\r\n";
	private static String COPYRIGHT_FIELD = "\t/**\r\n" +
		"\t * Copyright.\r\n" +
		"\t */\r\n" +
		"\tpublic static final String COPYRIGHT = com.ibm.commerce.copyright.IBMCopyright.SHORT_COPYRIGHT;\r\n";
	private static String CONSTRUCTOR = "_constructor_";
	private static final Collection<String> PRIMITIVE_TYPES;
	static {
		PRIMITIVE_TYPES = new HashSet<String>();
		PRIMITIVE_TYPES.add("boolean");
		PRIMITIVE_TYPES.add("byte");
		PRIMITIVE_TYPES.add("short");
		PRIMITIVE_TYPES.add("char");
		PRIMITIVE_TYPES.add("int");
		PRIMITIVE_TYPES.add("long");
		PRIMITIVE_TYPES.add("float");
		PRIMITIVE_TYPES.add("double");
	}
	
	public static void appendCopyrightComment(StringBuilder sb) {
		//sb.append(COMMERCE_COPYRIGHT_COMMENT);
	}
	
	public static void appendCopyrightField(StringBuilder sb) {
		//sb.append(COPYRIGHT_FIELD);
	}
	
	public static void setFieldPrivate(FieldDeclaration fieldDeclaration) {
		boolean privateSet = false;
		@SuppressWarnings("unchecked")
		List<IExtendedModifier> fieldModifiers = fieldDeclaration.modifiers();
		for (IExtendedModifier fieldModifier : fieldModifiers) {
			if (fieldModifier.isModifier()) {
				Modifier modifier = (Modifier) fieldModifier;
				if (modifier.isPublic()) {
					modifier.setKeyword(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
					privateSet = true;
				}
				else if (modifier.isProtected()) {
					modifier.setKeyword(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
					privateSet = true;
				}
				else if (modifier.isPrivate()) {
					privateSet = true;
				}
			}
		}
		if (!privateSet) {
			fieldModifiers.add(fieldDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
		}
	}
	
	public static void setFieldProtected(FieldDeclaration fieldDeclaration) {
		boolean protectedSet = false;
		@SuppressWarnings("unchecked")
		List<IExtendedModifier> fieldModifiers = fieldDeclaration.modifiers();
		for (IExtendedModifier fieldModifier : fieldModifiers) {
			if (fieldModifier.isModifier()) {
				Modifier modifier = (Modifier) fieldModifier;
				if (modifier.isPublic()) {
					modifier.setKeyword(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
					protectedSet = true;
				}
				else if (modifier.isProtected()) {
					protectedSet = true;
				}
				else if (modifier.isPrivate()) {
					modifier.setKeyword(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
					protectedSet = true;
				}
			}
		}
		if (!protectedSet) {
			fieldModifiers.add(fieldDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD));
		}
	}
	
	public static void setMethodPublic(MethodDeclaration methodDeclaration) {
		boolean publicSet = false;
		@SuppressWarnings("unchecked")
		List<IExtendedModifier> methodModifiers = methodDeclaration.modifiers();
		for (IExtendedModifier methodModifier : methodModifiers) {
			if (methodModifier.isModifier()) {
				Modifier modifier = (Modifier) methodModifier;
				if (modifier.isPublic()) {
					publicSet = true;
				}
				else if (modifier.isProtected()) {
					modifier.setKeyword(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
					publicSet = true;
				}
				else if (modifier.isPrivate()) {
					modifier.setKeyword(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
					publicSet = true;
				}
			}
		}
		if (!publicSet) {
			methodModifiers.add(methodDeclaration.getAST().newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
		}
	}
	
	public static boolean isStaticField(FieldDeclaration fieldDeclaration) {
		boolean isStatic = false;
		@SuppressWarnings("unchecked")
		List<IExtendedModifier> fieldModifiers = fieldDeclaration.modifiers();
		for (IExtendedModifier fieldModifier : fieldModifiers) {
			if (fieldModifier.isModifier()) {
				Modifier modifier = (Modifier) fieldModifier;
				if (modifier.isStatic()) {
					isStatic = true;
					break;
				}
			}
		}
		return isStatic;
	}
	
	public static IType resolveTypeSignature(IType sourceType, String signature) {
		IType resolvedType = null;
		if (signature != null) {
			String typeName = Signature.toString(signature);
			int kind = Signature.getTypeSignatureKind(signature);
			if (kind == Signature.CLASS_TYPE_SIGNATURE) {
				try {
					String[][] resolvedTypes = sourceType.resolveType(typeName);
					if (resolvedTypes != null && resolvedTypes.length > 0) {
						resolvedType = sourceType.getJavaProject().findType(resolvedTypes[0][0] + "." + resolvedTypes[0][1]);
					}
				}
				catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
		return resolvedType;
	}
	
	public static boolean isConstructor(String methodKey) {
		return methodKey.startsWith(CONSTRUCTOR);
	}
	
	public static boolean isPrimitiveType(String type) {
		return PRIMITIVE_TYPES.contains(type);
	}
	
	public static String[] getParameterTypes(String methodKey) {
		String[] parameterTypes = new String[0];
		int index = methodKey.indexOf('+');
		if (index > -1) {
			parameterTypes = methodKey.substring(index + 1).split("\\+");
		}
		return parameterTypes;
	}
	
	public static String getMethodName(String methodKey) {
		String methodName = methodKey;
		int index = methodKey.indexOf('+');
		if (index > -1) {
			methodName = methodKey.substring(0, index);
		}
		return methodName;
	}
	
	public static String getMethodKey(MethodDeclaration methodDeclaration) {
		String name = methodDeclaration.isConstructor() ? CONSTRUCTOR : methodDeclaration.getName().getIdentifier();
		StringBuilder sb = new StringBuilder(name);
		@SuppressWarnings("unchecked")
		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		for (SingleVariableDeclaration parameter : parameters) {
			sb.append("+");
			Type type = parameter.getType();
			sb.append(type.resolveBinding().getQualifiedName());
			for (int i = 0; i < parameter.getExtraDimensions(); i++) {
				sb.append("[]");
			}
		}
		return sb.toString();
	}
	
	public static String getMethodKey(IMethodBinding methodBinding) {
		String name = methodBinding.isConstructor() ? CONSTRUCTOR : methodBinding.getName();
		StringBuilder sb = new StringBuilder(name);
		ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
		for (ITypeBinding parameterType : parameterTypes) {
			sb.append("+");
			sb.append(parameterType.getQualifiedName());
		}
		return sb.toString();
	}
	
	public static String getMethodKey(IType type, IMethod method) {
		String methodKey = null;
		try {
			if (Flags.isPublic(method.getFlags()) || Flags.isProtected(method.getFlags())) {
				String name = method.getElementName();
				if (method.isConstructor()) {
					name = CONSTRUCTOR;
				}
				StringBuilder sb = new StringBuilder(name);
				String[] parameterTypes = method.getParameterTypes();
				for (String parameterType : parameterTypes) {
					String typeName = Signature.toString(parameterType);
					int kind = Signature.getTypeSignatureKind(parameterType);
					if (kind == Signature.CLASS_TYPE_SIGNATURE) {
						String[][] resolvedTypes = type.resolveType(typeName);
						if (resolvedTypes != null && resolvedTypes.length > 0) {
							IType resolvedType = type.getJavaProject().findType(resolvedTypes[0][0] + "." + resolvedTypes[0][1]);
							typeName = resolvedType.getFullyQualifiedName();
						}
					}
					sb.append("+");
					sb.append(typeName);
				}
				methodKey = sb.toString();
			}
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		return methodKey;
	}
	
	public static void replaceASTNode(ASTNode astNode, ASTNode newNode) {
		StructuralPropertyDescriptor location = astNode.getLocationInParent();
		if (location.isChildListProperty()) {
			@SuppressWarnings("unchecked")
			List<ASTNode> childList = (List<ASTNode>) astNode.getParent().getStructuralProperty(location);
			int index = childList.indexOf(astNode);
			childList.remove(astNode);
			childList.add(index, newNode);
		}
		else {
			astNode.getParent().setStructuralProperty(location, newNode);
		}
	}
	
	public static void replaceStatement(Statement statement, List<Statement> newStatements) {
		StructuralPropertyDescriptor location = statement.getLocationInParent();
		if (location.isChildListProperty()) {
			@SuppressWarnings("unchecked")
			List<ASTNode> childList = (List<ASTNode>) statement.getParent().getStructuralProperty(location);
			int index = childList.indexOf(statement);
			childList.remove(statement);
			while (newStatements.size() > 0) {
				Statement newStatement = newStatements.remove(0);
				childList.add(index, newStatement);
				index++;
			}
		}
		else {
			Block newBlock = statement.getAST().newBlock();
			@SuppressWarnings("unchecked")
			List<Statement> statements = newBlock.statements();
			while (newStatements.size() > 0) {
				Statement newStatement = newStatements.remove(0);
				statements.add(newStatement);
			}
			statement.getParent().setStructuralProperty(location, newBlock);
		}
	}
	
	public static IType resolveType(IType sourceType, String typeName) {
		IType resolvedType = null;
		try {
			String[][] resolvedBaseName = sourceType.resolveType(typeName);
			resolvedType = (resolvedBaseName != null && resolvedBaseName.length > 0) ? sourceType.getJavaProject().findType(resolvedBaseName[0][0] + "." + resolvedBaseName[0][1]) : null;
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		return resolvedType;
	}
	
	public static IDocument getDocument(IType type) {
		return getDocument(type.getCompilationUnit());
	}
	
	public static IDocument getDocument(ICompilationUnit compilationUnit) {
		IDocument document = null;
		InputStream inputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		IFile file = (IFile) compilationUnit.getResource();
		try {
			inputStream = file.getContents();
			inputStreamReader = new InputStreamReader(inputStream, file.getCharset());
			bufferedReader = new BufferedReader(inputStreamReader);
			StringBuilder sb = new StringBuilder();
			char[] buffer = new char[1024];
			int count = bufferedReader.read(buffer);
			while (count > -1) {
				sb.append(buffer, 0, count);
				count = bufferedReader.read(buffer);
			}
			document = new Document(sb.toString());
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (inputStreamReader != null) {
				try {
					inputStreamReader.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return document;
	}
	
	public static void renameJavaClass(BackupUtil backupUtil, ASTParser astParser, ICompilationUnit compilationUnit, String targetName, Map<String, String> nameMap, IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("rename " + compilationUnit.getElementName(), 500);
			IFile sourceFile = (IFile) compilationUnit.getResource();
			backupUtil.backupFile3(sourceFile, new SubProgressMonitor(progressMonitor, 100));
			astParser.setProject(compilationUnit.getJavaProject());
			astParser.setSource(compilationUnit);
			CompilationUnit astCompilationUnit = (CompilationUnit) astParser.createAST(new SubProgressMonitor(progressMonitor, 100));
			astCompilationUnit.recordModifications();
			astCompilationUnit.accept(new RenameJavaClassVisitor(nameMap));
			IDocument document = JavaUtil.getDocument(compilationUnit);
			TextEdit edits = astCompilationUnit.rewrite(document, null);
			edits.apply(document);
			ICompilationUnit newCompilationUnit = ((IPackageFragment) compilationUnit.getParent()).createCompilationUnit(targetName + ".java", document.get(), true, new SubProgressMonitor(progressMonitor, 100));
			backupUtil.addGeneratedFile3((IFile) newCompilationUnit.getResource(), new SubProgressMonitor(progressMonitor, 100));
			compilationUnit.getResource().delete(true, new SubProgressMonitor(progressMonitor, 100));
		}
		catch (BadLocationException e) {
			e.printStackTrace();
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private static class RenameJavaClassVisitor extends ASTVisitor {
		private Map<String, String> iNameMap;
		private Collection<String> iImportedNames = new HashSet<String>();
		private Map<String, String> iImportedNameMap = new HashMap<String, String>();
		
		public RenameJavaClassVisitor(Map<String, String> nameMap) {
			iNameMap = nameMap;
		}
		
//		public boolean preVisit2(ASTNode astNode) {
//			if (astNode.toString().startsWith("ApprovalStatusJPAAccessHelper")) {
//				System.out.println("ApprovalStatusJPAAccessHelper "+iNameMap.get("ApprovalStatusJPAAccessHelper"));
//			}
//			return true;
//		}
		
		public boolean visit(ImportDeclaration importDeclaration) {
			boolean visitChildren = true;
			Name name = importDeclaration.getName();
			if (name.getNodeType() == ASTNode.QUALIFIED_NAME) {
				QualifiedName qualifiedName = (QualifiedName) name;
				String sourceName = qualifiedName.getName().getIdentifier();
				String targetName = iNameMap.get(sourceName);
				if (targetName == null) {
					targetName = sourceName;
				}
				if (iImportedNames.contains(targetName)) {
					iImportedNameMap.put(sourceName, qualifiedName.getQualifier().getFullyQualifiedName() + "." + targetName);
					importDeclaration.delete();
					visitChildren = false;
				}
				else {
					iImportedNames.add(targetName);
				}
			}
			return visitChildren;
		}
		
		public boolean visit(SimpleName simpleName) {
			if (iImportedNameMap.containsKey(simpleName.getIdentifier())) {
				if (iImportedNameMap.get(simpleName.getIdentifier()).contains(".") && simpleName.getParent() != null && simpleName.getParent().getNodeType() == ASTNode.QUALIFIED_NAME) {
					System.out.println("mapping "+simpleName.getIdentifier()+ " to " + iImportedNameMap.get(simpleName.getIdentifier()));
				}
				JavaUtil.replaceASTNode(simpleName, simpleName.getAST().newName(iImportedNameMap.get(simpleName.getIdentifier())));
			}
			else if (iNameMap.containsKey(simpleName.getIdentifier())) {
				simpleName.setIdentifier(iNameMap.get(simpleName.getIdentifier()));
			}
			return true;
		}
	} 
}
