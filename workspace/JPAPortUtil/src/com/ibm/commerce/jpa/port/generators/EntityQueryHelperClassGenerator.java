package com.ibm.commerce.jpa.port.generators;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ClassInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.ExceptionUtil;
import com.ibm.commerce.jpa.port.util.ImportUtil;
import com.ibm.commerce.jpa.port.util.JavaUtil;

public class EntityQueryHelperClassGenerator {
	private static final String COPYRIGHT_FIELD = "COPYRIGHT";
	private static final String SET_NULL = "setNull";
	private static final String SET_BYTES = "setBytes";
	private static final String SET_LONG = "setLong";
	private static final String SET_DOUBLE = "setDouble";
	private static final String SET_INT = "setInt";
	private static final String SET_STRING = "setString";
	private static final String SET_FLOAT = "setFloat";
	private static final String SET_OBJECT = "setObject";
	private static final String SET_SHORT = "setShort";
	private static final String SET_TIMESTAMP = "setTimestamp";
	private static final String SET_PARAMETER = "setParameter";
	private static final String SET_MAX_ROWS = "setMaxRows";
	private static final String SET_MAX_RESULTS = "setMaxResults";
	private static final String GET_MERGED_PREPARED_STATEMENT = "getMergedPreparedStatement";
	private static final String GET_MERGED_QUERY = "getMergedQuery";
	private static final String GET_PREPARED_STATEMENT = "getPreparedStatement";
	private static final String GET_QUERY = "getQuery";
	private static final String PREPARED_STATEMENT = "java.sql.PreparedStatement";
	private static final String CATALOG_SQL_HELPER = "com.ibm.commerce.catalog.objects.CatalogSqlHelper";
	private static final String SET_PARAMETERS_IN_WHERE_CLAUSE = "setParametersInWhereClause";
	private static final String SET_PARAMETERS_IN_QUERY = "setParametersInQuery";
	private static final String QUERY = "javax.persistence.Query";
	private static final String SQL_EXCEPTION = "java.sql.SQLException";
	private static final String ILLEGAL_ARGUMENT_EXCEPTION = "java.lang.IllegalArgumentException";
	private static final String UTF_SORTING_ATTRIBUTE = "com.ibm.commerce.utf.helper.SortingAttribute";
	private static final String NEGOTIATION_SORTING_ATTRIBUTE = "com.ibm.commerce.negotiation.util.SortingAttribute";
	private static final String SORTING_ATTRIBUE = "com.ibm.commerce.base.util.SortingAttribute";
	private static final String UTF_QUERY_COLUMN = "com.ibm.commerce.utf.helper.QueryColumn";
	private static final String NEGOTIATION_QUERY_COLUMN = "com.ibm.commerce.negotiation.util.QueryColumn";
	private static final String QUERY_COLUMN = "com.ibm.commerce.base.util.QueryColumn";
	private static final String FIND_WITH_PUSH_DOWN_QUERY = "findWithPushDownQuery";
	private static final Map<String, String> PREPARED_STATEMENT_TO_QUERY_METHOD_MAP;
	private static final Map<String, String> FINDER_OBJECT_TO_QUERY_HELPER_METHOD_MAP;
	private static final Map<String, String> TYPE_MAP;
	private static final Set<String> EXEMPT_METHODS;
	static {
		PREPARED_STATEMENT_TO_QUERY_METHOD_MAP = new HashMap<String, String>();
		PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.put(SET_NULL, SET_PARAMETER);
		PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.put(SET_BYTES, SET_PARAMETER);
		PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.put(SET_LONG, SET_PARAMETER);
		PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.put(SET_DOUBLE, SET_PARAMETER);
		PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.put(SET_INT, SET_PARAMETER);
		PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.put(SET_STRING, SET_PARAMETER);
		PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.put(SET_FLOAT, SET_PARAMETER);
		PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.put(SET_OBJECT, SET_PARAMETER);
		PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.put(SET_SHORT, SET_PARAMETER);
		PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.put(SET_TIMESTAMP, SET_PARAMETER);
		PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.put(SET_MAX_ROWS, SET_MAX_RESULTS);
		FINDER_OBJECT_TO_QUERY_HELPER_METHOD_MAP = new HashMap<String, String>();
		FINDER_OBJECT_TO_QUERY_HELPER_METHOD_MAP.put(GET_MERGED_PREPARED_STATEMENT, GET_MERGED_QUERY);
		FINDER_OBJECT_TO_QUERY_HELPER_METHOD_MAP.put(GET_PREPARED_STATEMENT, GET_QUERY);
		TYPE_MAP = new HashMap<String, String>();
		TYPE_MAP.put(PREPARED_STATEMENT, QUERY);
		TYPE_MAP.put(SQL_EXCEPTION, ILLEGAL_ARGUMENT_EXCEPTION);
		TYPE_MAP.put(UTF_SORTING_ATTRIBUTE, SORTING_ATTRIBUE);
		TYPE_MAP.put(UTF_QUERY_COLUMN, QUERY_COLUMN);
		TYPE_MAP.put(NEGOTIATION_SORTING_ATTRIBUTE, SORTING_ATTRIBUE);
		TYPE_MAP.put(NEGOTIATION_QUERY_COLUMN, QUERY_COLUMN);
		EXEMPT_METHODS = new HashSet<String>();
		EXEMPT_METHODS.add(FIND_WITH_PUSH_DOWN_QUERY);
	}
	private static final Map<String, List<FieldGenerator>> FIELD_GENERATORS_MAP;
	static {
		FIELD_GENERATORS_MAP = new HashMap<String, List<FieldGenerator>>();
		List<FieldGenerator> fieldGenerators = new ArrayList<FieldGenerator>();
		fieldGenerators.add(new PVCDeviceSpecJPAQueryHelperBaseGenerator.FromClauseFieldGenerator());
		FIELD_GENERATORS_MAP.put(PVCDeviceSpecJPAQueryHelperBaseGenerator.TYPE_NAME, fieldGenerators);
	}

	private ASTParser iASTParser;
	private BackupUtil iBackupUtil;
	private EntityInfo iEntityInfo;
	private ModuleInfo iModuleInfo;
	private ApplicationInfo iApplicationInfo;
	private ClassInfo iEntityQueryHelperClassInfo;
	private ClassInfo iEntityQueryHelperBaseClassInfo;
	private List<FieldGenerator> iEntityQueryHelperFieldGenerators;
	private List<FieldGenerator> iEntityQueryHelperBaseFieldGenerators;
	
	public EntityQueryHelperClassGenerator(ASTParser astParser, BackupUtil backupUtil, EntityInfo entityInfo) {
		iASTParser = astParser;
		iBackupUtil = backupUtil;
		iEntityInfo = entityInfo;
		iModuleInfo = entityInfo.getModuleInfo();
		iApplicationInfo = iModuleInfo.getApplicationInfo();
		iEntityQueryHelperClassInfo = entityInfo.getEntityQueryHelperClassInfo();
		iEntityQueryHelperBaseClassInfo = entityInfo.getEntityQueryHelperBaseClassInfo();
		if (iEntityQueryHelperClassInfo != null) {
			iEntityQueryHelperFieldGenerators = FIELD_GENERATORS_MAP.get(iEntityQueryHelperClassInfo.getQualifiedClassName());
			if (iEntityQueryHelperBaseClassInfo != null) {
				iEntityQueryHelperBaseFieldGenerators = FIELD_GENERATORS_MAP.get(iEntityQueryHelperBaseClassInfo.getQualifiedClassName());
			}
		}
	}
	
	public void generate(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("generate query helper for " + iEntityInfo.getEjbName(), IProgressMonitor.UNKNOWN);
			generateEntityQueryHelperBaseClass(progressMonitor);
			progressMonitor.worked(1000);
			generateEntityQueryHelperClass(progressMonitor);
			progressMonitor.worked(1000);
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private void generateEntityQueryHelperClass(IProgressMonitor progressMonitor) {
		if (iEntityInfo.getEjbFinderObjectType() != null) {
			try {
				iASTParser.setSource(iEntityInfo.getEjbFinderObjectType().getCompilationUnit());
				iASTParser.setResolveBindings(true);
				CompilationUnit ejbFinderObjectCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
				StringBuilder sb = new StringBuilder();
				sb.append("package ");
				sb.append(iEntityQueryHelperClassInfo.getPackageFragment().getElementName());
				sb.append(";\r\n");
				JavaUtil.appendCopyrightComment(sb);
				sb.append("\r\nimport javax.persistence.*;\r\n");
				@SuppressWarnings("unchecked")
				List<ImportDeclaration> importDeclarations = ejbFinderObjectCompilationUnit.imports();
				ImportUtil.appendImports(importDeclarations, sb);
				if (iEntityQueryHelperClassInfo.getSuperclassName() != null) {
					if (!iEntityQueryHelperClassInfo.getSuperclassPackage().equals(iEntityQueryHelperClassInfo.getPackageFragment().getElementName())) {
						sb.append("\r\nimport ");
						sb.append(iEntityQueryHelperClassInfo.getQualifiedSuperclassName());
						sb.append(";\r\n");
					}
				}
				sb.append("\r\npublic class ");
				sb.append(iEntityQueryHelperClassInfo.getClassName());
				if (iEntityQueryHelperClassInfo.getSuperclassName() != null) {
					sb.append(" extends ");
					sb.append(iEntityQueryHelperClassInfo.getSuperclassName());
				}
				sb.append(" {\r\n");
				JavaUtil.appendCopyrightField(sb);
				appendGeneratedFields(sb, iEntityInfo, iEntityQueryHelperFieldGenerators);
				sb.append("\r\n\tpublic ");
				sb.append(iEntityQueryHelperClassInfo.getClassName());
				sb.append("() {\r\n\t}\r\n}");
				IDocument document = new Document(sb.toString());
				iASTParser.setProject(iEntityInfo.getModuleInfo().getJavaProject());
				iASTParser.setResolveBindings(true);
				iASTParser.setSource(document.get().toCharArray());
				CompilationUnit astCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
				astCompilationUnit.recordModifications();
				portFinderObjectTypeDeclaration((TypeDeclaration) ejbFinderObjectCompilationUnit.types().get(0), astCompilationUnit, iEntityQueryHelperFieldGenerators);
				TextEdit edits = astCompilationUnit.rewrite(document, null);
				edits.apply(document);
				ICompilationUnit compilationUnit = iEntityQueryHelperClassInfo.getPackageFragment().createCompilationUnit(iEntityQueryHelperClassInfo.getClassName() + ".java", document.get(), true, new SubProgressMonitor(progressMonitor, 100));
				ExceptionUtil.suppressExceptions(iASTParser, compilationUnit, new SubProgressMonitor(progressMonitor, 100));
				iBackupUtil.addGeneratedFile((IFile) compilationUnit.getResource(), new SubProgressMonitor(progressMonitor, 100));
				iApplicationInfo.incrementGeneratedAssetCount();
			}
			catch (CoreException e) {
				e.printStackTrace();
			}
			catch (BadLocationException e) {
				e.printStackTrace();
			}
//			catch (Throwable e) {
//				System.out.print("EntityQueryHelperClassGenerator " + iEntityInfo.getEjbFinderObjectType().getCompilationUnit().getElementName());
//				e.printStackTrace();
//			}
		}
	}
	
	private void generateEntityQueryHelperBaseClass(IProgressMonitor progressMonitor) {
		if (iEntityQueryHelperBaseClassInfo != null) {
			try {
				iASTParser.setSource(iEntityInfo.getEjbFinderObjectBaseType().getCompilationUnit());
				iASTParser.setResolveBindings(true);
				CompilationUnit ejbFinderObjectBaseCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
				StringBuilder sb = new StringBuilder();
				sb.append("package ");
				sb.append(iEntityQueryHelperBaseClassInfo.getPackageFragment().getElementName());
				sb.append(";\r\n");
				JavaUtil.appendCopyrightComment(sb);
				sb.append("\r\nimport javax.persistence.*;\r\n");
				@SuppressWarnings("unchecked")
				List<ImportDeclaration> importDeclarations = ejbFinderObjectBaseCompilationUnit.imports();
				ImportUtil.appendImports(importDeclarations, sb);
				if (iEntityQueryHelperBaseClassInfo.getSuperclassName() != null) {
					if (!iEntityQueryHelperBaseClassInfo.getSuperclassPackage().equals(iEntityQueryHelperBaseClassInfo.getPackageFragment().getElementName())) {
						sb.append("\r\nimport ");
						sb.append(iEntityQueryHelperBaseClassInfo.getQualifiedSuperclassName());
						sb.append(";\r\n");
					}
				}
				sb.append("\r\npublic class ");
				sb.append(iEntityQueryHelperBaseClassInfo.getClassName());
				if (iEntityQueryHelperBaseClassInfo.getSuperclassName() != null) {
					sb.append(" extends ");
					sb.append(iEntityQueryHelperBaseClassInfo.getSuperclassName());
				}
				sb.append(" {\r\n");
				JavaUtil.appendCopyrightField(sb);
				appendGeneratedFields(sb, iEntityInfo, iEntityQueryHelperBaseFieldGenerators);
				sb.append("\r\n\tpublic ");
				sb.append(iEntityQueryHelperBaseClassInfo.getClassName());
				sb.append("() {\r\n\t}\r\n}");
				IDocument document = new Document(sb.toString());
				iASTParser.setProject(iEntityInfo.getModuleInfo().getJavaProject());
				iASTParser.setResolveBindings(true);
				iASTParser.setSource(document.get().toCharArray());
				CompilationUnit astCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
				astCompilationUnit.recordModifications();
				portFinderObjectTypeDeclaration((TypeDeclaration) ejbFinderObjectBaseCompilationUnit.types().get(0), astCompilationUnit, iEntityQueryHelperBaseFieldGenerators);
				TextEdit edits = astCompilationUnit.rewrite(document, null);
				edits.apply(document);
				ICompilationUnit compilationUnit = iEntityQueryHelperBaseClassInfo.getPackageFragment().createCompilationUnit(iEntityQueryHelperBaseClassInfo.getClassName() + ".java", document.get(), true, new SubProgressMonitor(progressMonitor, 100));
				ExceptionUtil.suppressExceptions(iASTParser, compilationUnit, new SubProgressMonitor(progressMonitor, 100));
//				ImportUtil.resolveImports(iASTParser, compilationUnit, new SubProgressMonitor(progressMonitor, 100));
				iBackupUtil.addGeneratedFile((IFile) compilationUnit.getResource(), new SubProgressMonitor(progressMonitor, 100));
				iApplicationInfo.incrementGeneratedAssetCount();
			}
			catch (CoreException e) {
				e.printStackTrace();
			}
			catch (BadLocationException e) {
				e.printStackTrace();
			}
//			catch (Throwable e) {
//				System.out.print("EntityQueryHelperClassGenerator " + iEntityInfo.getEjbFinderObjectType().getCompilationUnit().getElementName());
//				e.printStackTrace();
//			}
		}
	}
	
	private void appendGeneratedFields(StringBuilder sb, EntityInfo entityInfo, List<FieldGenerator> fieldGenerators) {
		if (fieldGenerators != null) {
			for (FieldGenerator fieldGenerator : fieldGenerators) {
				fieldGenerator.appendField(sb, entityInfo);
			}
		}
	}
	
	private void portFinderObjectTypeDeclaration(TypeDeclaration finderObjectTypeDeclaration, CompilationUnit entityQueryHelperCompilationUnit, List<FieldGenerator> fieldGenerators) {
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = finderObjectTypeDeclaration.bodyDeclarations();
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			switch (bodyDeclaration.getNodeType()) {
				case ASTNode.FIELD_DECLARATION: {
					FieldDeclaration fieldDeclaration = (FieldDeclaration) bodyDeclaration;
					portFieldDeclaration(fieldDeclaration, entityQueryHelperCompilationUnit, fieldGenerators);
					break;
				}
				case ASTNode.INITIALIZER: {
					portInitializer((Initializer) bodyDeclaration, entityQueryHelperCompilationUnit);
					break;
				}
				case ASTNode.METHOD_DECLARATION: {
					portMethodDeclaration((MethodDeclaration) bodyDeclaration, entityQueryHelperCompilationUnit);
					break;
				}
			}
		}
	}
	
	private void portFieldDeclaration(FieldDeclaration fieldDeclaration, CompilationUnit entityQueryHelperCompilationUnit, List<FieldGenerator> fieldGenerators) {
		fieldDeclaration.accept(new QueryHelperPortVisitor());
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = ((TypeDeclaration) entityQueryHelperCompilationUnit.types().get(0)).bodyDeclarations();
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> ejbVariableDeclarationFragments = fieldDeclaration.fragments();
		for (VariableDeclarationFragment ejbVariableDeclarationFragment : ejbVariableDeclarationFragments) {
			String fieldName = ejbVariableDeclarationFragment.getName().getIdentifier();
			if (!COPYRIGHT_FIELD.equals(fieldName)) {
				boolean generatedField = false;
				if (fieldGenerators != null) {
					for (FieldGenerator fieldGenerator : fieldGenerators) {
						if (fieldGenerator.getFieldName().equals(fieldName)) {
							generatedField = true;
							break;
						}
					}
				}
				if (!generatedField) {
					FieldDeclaration entityQueryHelperFieldDeclaration = (FieldDeclaration) ASTNode.copySubtree(entityQueryHelperCompilationUnit.getAST(), fieldDeclaration);
					bodyDeclarations.add(entityQueryHelperFieldDeclaration);
				}
			}
		}
	}
	
	private void portInitializer(Initializer initializer, CompilationUnit entityQueryHelperCompilationUnit) {
		initializer.accept(new QueryHelperPortVisitor());
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = ((TypeDeclaration) entityQueryHelperCompilationUnit.types().get(0)).bodyDeclarations();
		Initializer entityQueryHelperInitializer = (Initializer) ASTNode.copySubtree(entityQueryHelperCompilationUnit.getAST(), initializer);
		bodyDeclarations.add(entityQueryHelperInitializer);
	}
	
	private void portMethodDeclaration(MethodDeclaration methodDeclaration, CompilationUnit entityQueryHelperCompilationUnit) {
		if (!methodDeclaration.isConstructor() && !EXEMPT_METHODS.contains(methodDeclaration.getName().getIdentifier())) {
			methodDeclaration.accept(new QueryHelperPortVisitor());
			MethodDeclaration entityQueryHelperMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(entityQueryHelperCompilationUnit.getAST(), methodDeclaration);
			entityQueryHelperMethodDeclaration.setJavadoc(null);
			@SuppressWarnings("unchecked")
			List<Name> thrownExceptions = entityQueryHelperMethodDeclaration.thrownExceptions();
			thrownExceptions.clear();
			@SuppressWarnings("unchecked")
			List<BodyDeclaration> bodyDeclarations = ((TypeDeclaration) entityQueryHelperCompilationUnit.types().get(0)).bodyDeclarations();
			bodyDeclarations.add(entityQueryHelperMethodDeclaration);
		}
	}
	
	private static class BlockInfo {
		public BlockInfo(Block block) {
			iBlock = block;
		}
		public Block iBlock;
		public Collection<Expression> iDeadIfExpressions;
		public Collection<Expression> iDeadElseExpressions;
	}
	
	private class QueryHelperPortVisitor extends PortVisitor {
		private Deque<BlockInfo> iBlockInfoStack = new ArrayDeque<BlockInfo>();
		private Deque<IfStatement> iIfStatementStack = new ArrayDeque<IfStatement>();
		
		public QueryHelperPortVisitor() {
			super(iApplicationInfo, iModuleInfo.getJavaProject());
		}
		
		public boolean visit(Block block) {
			iBlockInfoStack.push(new BlockInfo(block));
			return true;
		}
		
		public void endVisit(Block block) {
			if (iBlockInfoStack.peek().iBlock == block) {
				iBlockInfoStack.pop();
			}
		}
		
		public boolean visit(IfStatement ifStatement) {
			boolean visitChildren = true;
			Expression ifExpression = ifStatement.getExpression();
			for (BlockInfo blockInfo : iBlockInfoStack) {
				if (blockInfo.iDeadIfExpressions != null) {
					for (Expression deadIfExpression : blockInfo.iDeadIfExpressions) {
						if (deadIfExpression.toString().equals(ifExpression.toString())) {
							if (ifStatement.getElseStatement() != null) {
								ifStatement.getElseStatement().accept(this);
								if (ifStatement.getElseStatement().getNodeType() == ASTNode.BLOCK) {
									@SuppressWarnings("unchecked")
									List<Statement> statements = ((Block) ifStatement.getElseStatement()).statements();
									replaceStatement(ifStatement, statements);
								}
								else {
									replaceASTNode(ifStatement, ifStatement.getElseStatement());
								}
							}
							else {
								ifStatement.delete();
							}
							visitChildren = false;
							break;
						}
					}
					if (!visitChildren) {
						break;
					}
				}
				if (blockInfo.iDeadElseExpressions != null) {
					for (Expression deadElseExpression : blockInfo.iDeadElseExpressions) {
						if (deadElseExpression.toString().equals(ifExpression.toString())) {
							ifStatement.getThenStatement().accept(this);
							if (ifStatement.getThenStatement().getNodeType() == ASTNode.BLOCK) {
								@SuppressWarnings("unchecked")
								List<Statement> statements = ((Block) ifStatement.getThenStatement()).statements();
								replaceStatement(ifStatement, statements);
							}
							else {
								replaceASTNode(ifStatement, ifStatement.getThenStatement());
							}
							visitChildren = false;
							break;
						}
					}
					if (!visitChildren) {
						break;
					}
				}
			}
			if (visitChildren) {
				iIfStatementStack.push(ifStatement);
			}
			return visitChildren;
		}
		
		public void endVisit(IfStatement ifStatement) {
			if (iIfStatementStack.peek() == ifStatement) {
				iIfStatementStack.pop();
			}
		}
		
		public boolean visit(ExpressionStatement expressionStatement) {
			boolean visitChildren = true;
			if (expressionStatement.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation methodInvocation = (MethodInvocation) expressionStatement.getExpression();
				if (methodInvocation.getExpression() != null && methodInvocation.getExpression().getNodeType() != ASTNode.THIS_EXPRESSION) {
					ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
					if (typeBinding != null) {
						String methodName = methodInvocation.getName().getIdentifier();
						if (PREPARED_STATEMENT.equals(typeBinding.getQualifiedName()) && SET_NULL.equals(methodName)) {
							AST ast = methodInvocation.getAST();
							ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
							classInstanceCreation.setType(ast.newSimpleType(ast.newName("javax.persistence.NoResultException")));
							ThrowStatement throwStatement = ast.newThrowStatement();
							throwStatement.setExpression(classInstanceCreation);
							Block currentBlock = iBlockInfoStack.peek().iBlock;
							@SuppressWarnings("unchecked")
							List<Statement> statements = currentBlock.statements();
							boolean beforeCurrentStatement = true;
							for (int i = 0; i < statements.size(); i++) {
								Statement statement = statements.get(i);
								if (beforeCurrentStatement) {
									if (statement == expressionStatement) {
										beforeCurrentStatement = false;
									}
								}
								else {
									statement.delete();
									i--;
								}
							}
							IfStatement currentIfStatement = iIfStatementStack.peek();
							if (currentIfStatement.getThenStatement() == expressionStatement || currentIfStatement.getThenStatement() == currentBlock) {
								if (currentIfStatement.getParent().getNodeType() == ASTNode.BLOCK) {
									for (BlockInfo blockInfo : iBlockInfoStack) {
										if (blockInfo.iBlock == currentIfStatement.getParent()) {
											if (blockInfo.iDeadIfExpressions == null) {
												blockInfo.iDeadIfExpressions = new HashSet<Expression>();
											}
											blockInfo.iDeadIfExpressions.add(currentIfStatement.getExpression());
											break;
										}
									}
								}
							}
							else if (currentIfStatement.getElseStatement() == expressionStatement || currentIfStatement.getElseStatement() == currentBlock) {
								if (currentIfStatement.getParent().getNodeType() == ASTNode.BLOCK) {
									for (BlockInfo blockInfo : iBlockInfoStack) {
										if (blockInfo.iBlock == currentIfStatement.getParent()) {
											if (blockInfo.iDeadElseExpressions == null) {
												blockInfo.iDeadElseExpressions = new HashSet<Expression>();
											}
											blockInfo.iDeadElseExpressions.add(currentIfStatement.getExpression());
											break;
										}
									}
								}
							}
							replaceASTNode(expressionStatement, throwStatement);
							visitChildren = false;
						}
					}
				}
			}
			return visitChildren;
		}
		
		public void endVisit(MethodInvocation methodInvocation) {
			String methodName = methodInvocation.getName().getIdentifier();
			if (methodInvocation.getExpression() != null && methodInvocation.getExpression().getNodeType() != ASTNode.THIS_EXPRESSION) {
				ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
				if (typeBinding != null) {
					if (PREPARED_STATEMENT.equals(typeBinding.getQualifiedName())) {
						if (PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.containsKey(methodName)) {
							methodInvocation.getName().setIdentifier(PREPARED_STATEMENT_TO_QUERY_METHOD_MAP.get(methodName));
						}
						else {
							System.out.println("unmapped prepared statement method: "+methodName);
						}
						if (SET_NULL.equals(methodName)) {
							@SuppressWarnings("unchecked")
							List<Expression> arguments = methodInvocation.arguments();
							while (arguments.size() > 1) {
								arguments.remove(arguments.size() - 1);
							}
							NullLiteral nullLiteral = methodInvocation.getAST().newNullLiteral();
							arguments.add(nullLiteral);
						}
						else if (SET_OBJECT.equals(methodName)) {
							@SuppressWarnings("unchecked")
							List<Expression> arguments = methodInvocation.arguments();
							while (arguments.size() > 2) {
								arguments.remove(arguments.size() - 1);
							}
						}
					}
					else if (CATALOG_SQL_HELPER.equals(typeBinding.getQualifiedName()) && methodName.equals(SET_PARAMETERS_IN_WHERE_CLAUSE)) {
						methodInvocation.setExpression(null);
						methodInvocation.getName().setIdentifier(SET_PARAMETERS_IN_QUERY);
					}
				}
			}
			else {
				if (FINDER_OBJECT_TO_QUERY_HELPER_METHOD_MAP.containsKey(methodName)) {
					methodInvocation.getName().setIdentifier(FINDER_OBJECT_TO_QUERY_HELPER_METHOD_MAP.get(methodName));
				}
			}
		}
		
		public void endVisit(SuperMethodInvocation node) {
			String methodName = node.getName().getIdentifier();
			if (FINDER_OBJECT_TO_QUERY_HELPER_METHOD_MAP.containsKey(methodName)) {
				node.getName().setIdentifier(FINDER_OBJECT_TO_QUERY_HELPER_METHOD_MAP.get(methodName));
			}
		}
		
		public boolean visit(SimpleType node) {
			boolean visitChildren = true;
			ITypeBinding typeBinding = node.resolveBinding();
			if (typeBinding != null) {
				String qualifiedTypeName = typeBinding.getQualifiedName();
				if (TYPE_MAP.containsKey(qualifiedTypeName)) {
					Type newType = node.getAST().newSimpleType(node.getAST().newName(TYPE_MAP.get(qualifiedTypeName)));
					replaceASTNode(node, newType);
					visitChildren = false;
				}
			}
			if (visitChildren) {
				super.visit(node);
			}
			return visitChildren;
		}
		
		public boolean visit(SimpleName node) {
			boolean visitChildren = true;
			IBinding binding = node.resolveBinding();
			if (binding != null) {
				if (binding.getKind() == ITypeBinding.VARIABLE) {
					IVariableBinding variableBinding = (IVariableBinding) binding;
					if (!node.isDeclaration() && variableBinding.getDeclaringClass() != null && variableBinding.isField() && (variableBinding.getModifiers() & (Modifier.STATIC | Modifier.FINAL | Modifier.PUBLIC)) == (Modifier.STATIC | Modifier.FINAL | Modifier.PUBLIC)) {
						String declaringClass = variableBinding.getDeclaringClass().getQualifiedName();
						if (!isClassInHierarchy(declaringClass)) {
							Name newName = node.getAST().newName(declaringClass + "." + node.getIdentifier());
							replaceASTNode(node, newName);
							visitChildren = false;
						}
					}
				}
			}
			if (visitChildren) {
				visitChildren = super.visit(node);
			}
			return visitChildren;
		}
		
		public boolean visit(FieldAccess node) {
			boolean visitChildren = true;
			if (node.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
				IVariableBinding variableBinding = node.resolveFieldBinding();
				if (variableBinding != null && variableBinding.getDeclaringClass() != null && variableBinding.isField() && (variableBinding.getModifiers() & (Modifier.STATIC | Modifier.FINAL | Modifier.PUBLIC)) == (Modifier.STATIC | Modifier.FINAL | Modifier.PUBLIC)) {
					String declaringClass = variableBinding.getDeclaringClass().getQualifiedName();
					if (!isClassInHierarchy(declaringClass)) {
						Name newName = node.getAST().newName(declaringClass + "." + node.getName().getIdentifier());
						replaceASTNode(node, newName);
						visitChildren = false;
					}
				}
			}
			if (visitChildren) {
				visitChildren = super.visit(node);
			}
			return visitChildren;
		}
		
		private boolean isClassInHierarchy(String className) {
			return (iEntityInfo.getEjbFinderObjectType() != null && iEntityInfo.getEjbFinderObjectType().getFullyQualifiedName('.').equals(className)) ||
				(iEntityInfo.getEjbFinderObjectBaseType() != null && iEntityInfo.getEjbFinderObjectBaseType().getFullyQualifiedName('.').equals(className)) ||
				(iEntityInfo.getSupertype() != null && iEntityInfo.getSupertype().getEjbFinderObjectType() != null && iEntityInfo.getSupertype().getEjbFinderObjectType().equals(className)) ||
				(iEntityInfo.getSupertype() != null && iEntityInfo.getSupertype().getEjbFinderObjectBaseType() != null && iEntityInfo.getSupertype().getEjbFinderObjectBaseType().equals(className));
		}
	}
}
