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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ClassInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.RelatedEntityInfo;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.DataClassUtil;
import com.ibm.commerce.jpa.port.util.ImportUtil;
import com.ibm.commerce.jpa.port.util.JavaUtil;
import com.ibm.commerce.jpa.port.util.TargetExceptionUtil;

public class EntityDataClassGenerator {
	private static final String COPYRIGHT_FIELD = "COPYRIGHT";
	private static final String IS = "is";
	private static final String DIRTY = "Dirty";
	private static final String GET_IS = "getIs";
	private static final String GET = "get";
	private static final String COPY_TO = "copyTo";
	private static final String INITIALIZE = "initialize";
	private static final String GET_PRIMARY_KEY = "getPrimaryKey";
	
	private ASTParser iASTParser;
	private BackupUtil iBackupUtil;
	private EntityInfo iEntityInfo;
	private ModuleInfo iModuleInfo;
	private ApplicationInfo iApplicationInfo;
	private ClassInfo iEntityAccessBeanClassInfo;
	private TypeDeclaration iEntityDataClassTypeDeclaration;
	
	public EntityDataClassGenerator(ASTParser astParser, BackupUtil backupUtil, EntityInfo entityInfo) {
		iASTParser = astParser;
		iBackupUtil = backupUtil;
		iEntityInfo = entityInfo;
		iModuleInfo = entityInfo.getModuleInfo();
		iApplicationInfo = entityInfo.getModuleInfo().getApplicationInfo();
		iEntityAccessBeanClassInfo = entityInfo.getEntityAccessBeanClassInfo();
	}

	public void generate(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("generate entity data class for " + iEntityInfo.getEjbName(), 1000);
			try {
				iASTParser.setSource(iEntityInfo.getEjbAccessBeanType().getCompilationUnit());
				iASTParser.setResolveBindings(true);
				CompilationUnit ejbDataClassCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
				StringBuilder sb = new StringBuilder();
				sb.append("package ");
				sb.append(iEntityAccessBeanClassInfo.getPackageFragment().getElementName());
				sb.append(";\r\n");
				JavaUtil.appendCopyrightComment(sb);
				@SuppressWarnings("unchecked")
				List<ImportDeclaration> importDeclarations = ejbDataClassCompilationUnit.imports();
				ImportUtil.appendImports(importDeclarations, sb);
				sb.append("\r\npublic class ");
				sb.append(iEntityAccessBeanClassInfo.getClassName());
				String[] superInterfaceNames = iEntityInfo.getEjbAccessBeanType().getSuperInterfaceNames();
				boolean firstInterface = true;
				for (String superInterfaceName : superInterfaceNames) {
					if (firstInterface) {
						firstInterface = false;
						sb.append(" implements ");
					}
					else {
						sb.append(", ");
					}
					sb.append(superInterfaceName);
				}
				sb.append(" {\r\n");
				JavaUtil.appendCopyrightField(sb);
				appendConstructors(sb);
				sb.append("}");
				String source = sb.toString();
				IDocument document = new Document(source);
				iASTParser.setProject(iModuleInfo.getJavaProject());
				iASTParser.setResolveBindings(false);
				iASTParser.setSource(document.get().toCharArray());
				CompilationUnit entityDataClassCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
				iEntityDataClassTypeDeclaration = (TypeDeclaration) entityDataClassCompilationUnit.types().get(0);
				entityDataClassCompilationUnit.recordModifications();
				portDataClassTypeDeclaration((TypeDeclaration) ejbDataClassCompilationUnit.types().get(0));
				TextEdit edits = entityDataClassCompilationUnit.rewrite(document, null);
				edits.apply(document);
				source = document.get();
				ICompilationUnit compilationUnit = iEntityAccessBeanClassInfo.getPackageFragment().createCompilationUnit(iEntityAccessBeanClassInfo.getClassName() + ".java", source, true, new SubProgressMonitor(progressMonitor, 100));
				iBackupUtil.addGeneratedFile((IFile) compilationUnit.getResource(), new SubProgressMonitor(progressMonitor, 100));
				iApplicationInfo.incrementGeneratedAssetCount();
			}
			catch (CoreException e) {
				e.printStackTrace();
			}
			catch (BadLocationException e) {
				e.printStackTrace();
			}
			progressMonitor.worked(500);
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private void appendConstructors(StringBuilder sb) {
		sb.append("\r\n\tpublic ");
		sb.append(iEntityAccessBeanClassInfo.getClassName());
		sb.append("(){\r\n\t}\r\n");
	}
	
	private void portDataClassTypeDeclaration(TypeDeclaration ejbDataClassTypeDeclaration) {
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> ejbBodyDeclarations = ejbDataClassTypeDeclaration.bodyDeclarations();
		for (BodyDeclaration ejbBodyDeclaration : ejbBodyDeclarations) {
			switch (ejbBodyDeclaration.getNodeType()) {
				case ASTNode.FIELD_DECLARATION: {
					portFieldDeclaration((FieldDeclaration) ejbBodyDeclaration);
					break;
				}
				case ASTNode.METHOD_DECLARATION: {
					portMethodDeclaration((MethodDeclaration) ejbBodyDeclaration);
					break;
				}
			}
		}
	}
	
	private void portFieldDeclaration(FieldDeclaration fieldDeclaration) {
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> variableDeclarationFragments = fieldDeclaration.fragments();
		for (VariableDeclarationFragment variableDeclarationFragment : variableDeclarationFragments) {
			String fieldName = variableDeclarationFragment.getName().getIdentifier();
			if (!fieldName.equals(COPYRIGHT_FIELD) && !(fieldName.startsWith(IS) && fieldName.endsWith(DIRTY))) {
				fieldDeclaration.accept(new PortDataClassVisitor());
				FieldDeclaration entityFieldDeclaration = (FieldDeclaration) ASTNode.copySubtree(iEntityDataClassTypeDeclaration.getAST(), fieldDeclaration);
				int index = 0;
				@SuppressWarnings("unchecked")
				List<BodyDeclaration> entityBodyDeclarations = iEntityDataClassTypeDeclaration.bodyDeclarations();
				for (BodyDeclaration bodyDeclaration : entityBodyDeclarations) {
					if (bodyDeclaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
						break;
					}
					index++;
				}
				entityBodyDeclarations.add(index, entityFieldDeclaration);
			}
		}
	}
	
	private void portMethodDeclaration(MethodDeclaration methodDeclaration) {
		if (!methodDeclaration.isConstructor()) {
			String methodName = methodDeclaration.getName().getIdentifier();
			if (methodName.equals(INITIALIZE)) {
				portInitializeMethodDeclaration(methodDeclaration);
			}
			else if (!(methodName.startsWith(GET_IS) && methodName.endsWith(DIRTY)) && !COPY_TO.equals(methodName)) {
				TargetExceptionUtil.getUnhandledTargetExceptions(iApplicationInfo, iModuleInfo.getJavaProject(), methodDeclaration.getBody());
				methodDeclaration.accept(new PortDataClassVisitor());
				MethodDeclaration entityMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(iEntityDataClassTypeDeclaration.getAST(), methodDeclaration);
				@SuppressWarnings("unchecked")
				List<BodyDeclaration> entityBodyDeclarations = iEntityDataClassTypeDeclaration.bodyDeclarations();
				entityBodyDeclarations.add(entityMethodDeclaration);
			}
		}
	}
	
	private void portInitializeMethodDeclaration(MethodDeclaration initializeMethodDeclaration) {
		AST ast = initializeMethodDeclaration.getAST();
		JavaUtil.setMethodPublic(initializeMethodDeclaration);
		@SuppressWarnings("unchecked")
		List<SingleVariableDeclaration> parameters = initializeMethodDeclaration.parameters();
		SingleVariableDeclaration singleVariableDeclaration = parameters.get(0);
		singleVariableDeclaration.setType(ast.newSimpleType(ast.newName(iEntityInfo.getEntityClassInfo().getQualifiedClassName())));
		initializeMethodDeclaration.getBody().accept(new InitializeMethodVisitor());
		MethodDeclaration entityMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(iEntityDataClassTypeDeclaration.getAST(), initializeMethodDeclaration);
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> entityBodyDeclarations = iEntityDataClassTypeDeclaration.bodyDeclarations();
		entityBodyDeclarations.add(entityMethodDeclaration);
	}
	
	private class InitializeMethodVisitor extends ASTVisitor {
		public boolean visit(MethodInvocation methodInvocation) {
			boolean visitChildren = true;
			String methodName = methodInvocation.getName().getIdentifier();
			if (methodName.startsWith(GET)) {
				FieldInfo fieldInfo = iEntityInfo.getFieldInfoByGetterName(methodName);
				if (fieldInfo != null) {
					RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
					if (relatedEntityInfo != null) {
						AST ast = methodInvocation.getAST();
						String relatedEntityGetterName = relatedEntityInfo.getGetterName();
						if (relatedEntityGetterName == null) {
							relatedEntityGetterName = "get" + Character.toUpperCase(relatedEntityInfo.getFieldName().charAt(0)) + relatedEntityInfo.getFieldName().substring(1);
						}
						String referencedFieldGetterName = fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null ? fieldInfo.getReferencedFieldInfo().getTargetGetterName() : fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo().getGetterName();
						ConditionalExpression conditionalExpression = ast.newConditionalExpression();
						MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
						getterMethodInvocation.setName(ast.newSimpleName(relatedEntityGetterName));
						getterMethodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, methodInvocation.getExpression()));
						InfixExpression infixExpression = ast.newInfixExpression();
						infixExpression.setLeftOperand(getterMethodInvocation);
						infixExpression.setOperator(Operator.EQUALS);
						infixExpression.setRightOperand(ast.newNullLiteral());
						conditionalExpression.setExpression(infixExpression);
						conditionalExpression.setThenExpression(ast.newNullLiteral());
						getterMethodInvocation = (MethodInvocation) ASTNode.copySubtree(ast, getterMethodInvocation);
						MethodInvocation fieldMethodInvocation = ast.newMethodInvocation();
						fieldMethodInvocation.setExpression(getterMethodInvocation);
						fieldMethodInvocation.setName(ast.newSimpleName(referencedFieldGetterName));
						if (fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null) {
							conditionalExpression.setElseExpression(fieldMethodInvocation);
						}
						else {
							MethodInvocation relatedFieldMethodInvocation = ast.newMethodInvocation();
							relatedFieldMethodInvocation.setExpression(fieldMethodInvocation);
							relatedFieldMethodInvocation.setName(ast.newSimpleName(fieldInfo.getReferencedFieldInfo().getReferencedFieldInfo().getTargetGetterName()));
							conditionalExpression.setElseExpression(relatedFieldMethodInvocation);
						}
						JavaUtil.replaceASTNode(methodInvocation, conditionalExpression);
						visitChildren = false;
					}
				}
			}
			return visitChildren;
		}
	}
	
	private class PortDataClassVisitor extends PortVisitor {
		public PortDataClassVisitor() {
			super(iApplicationInfo, iModuleInfo.getJavaProject());
		}
		
		public boolean visit(ExpressionStatement expressionStatement) {
			boolean visitChildren = true;
			Expression statementExpression = expressionStatement.getExpression();
			if (statementExpression.getNodeType() == ASTNode.ASSIGNMENT) {
				Assignment assignment = (Assignment) statementExpression;
				Expression leftHandSide = assignment.getLeftHandSide();
				switch (leftHandSide.getNodeType()) {
					case ASTNode.FIELD_ACCESS: {
						FieldAccess fieldAccess = (FieldAccess) leftHandSide;
						if (fieldAccess.getExpression().getNodeType() == Expression.THIS_EXPRESSION) {
							String fieldName = fieldAccess.getName().getIdentifier();
							if (fieldName.startsWith(IS) && fieldName.endsWith(DIRTY)) {
								expressionStatement.delete();
								visitChildren = false;
							}
						}
						break;
					}
					case ASTNode.SIMPLE_NAME: {
						SimpleName simpleName = (SimpleName) leftHandSide;
						IBinding binding = simpleName.resolveBinding();
						if (binding instanceof IVariableBinding) {
							IVariableBinding variableBinding = (IVariableBinding) binding;
							if (variableBinding.isField()) {
								String fieldName = simpleName.getIdentifier();
								if (fieldName.startsWith(IS) && fieldName.endsWith(DIRTY)) {
									expressionStatement.delete();
									visitChildren = false;
								}
							}
						}
						break;
					}
				}
			}
			if (visitChildren) {
				visitChildren = super.visit(expressionStatement);
			}
			return visitChildren;
		}
		
		public boolean visit(MethodInvocation methodInvocation) {
			boolean visitChildren = true;
			if (methodInvocation.getExpression() == null || methodInvocation.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
				String methodName = methodInvocation.getName().getIdentifier();
				if (GET_PRIMARY_KEY.equals(methodName)) {
					visitChildren = DataClassUtil.portGetPrimaryKeyMethodInvocation(iEntityInfo, methodInvocation, this);
				}
			}
			if (visitChildren) {
				visitChildren = super.visit(methodInvocation);
			}
			return visitChildren;
		}
		
		public boolean visit(CastExpression castExpression) {
			boolean visitChildren = true;
			if (castExpression.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation methodInvocation = (MethodInvocation) castExpression.getExpression();
				String methodName = methodInvocation.getName().getIdentifier();
				if (GET_PRIMARY_KEY.equals(methodName)) {
					visitChildren = DataClassUtil.portGetPrimaryKeyMethodInvocation(iEntityInfo, methodInvocation, castExpression, this);
				}
			}
			if (visitChildren) {
				visitChildren = super.visit(castExpression);
			}
			return visitChildren;
		}
	}
}
