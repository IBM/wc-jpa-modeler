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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import com.ibm.commerce.jpa.port.info.AccessBeanSubclassInfo;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.TargetExceptionInfo;
import com.ibm.commerce.jpa.port.util.AccessBeanUtil;
import com.ibm.commerce.jpa.port.util.JavaUtil;
import com.ibm.commerce.jpa.port.util.TargetExceptionUtil;

public class EntityReferencingTypeGenerator {
	private static final String COMMIT_COPY_HELPER = "commitCopyHelper";
	private static final String REFRESH_COPY_HELPER = "refreshCopyHelper";
	private static final String INSTANTIATE_ENTITY = "instantiateEntity";
	private static final String GET_EJB_REF = "getEJBRef";
	private static final String EJB_OBJECT = "javax.ejb.EJBObject";
	private static final String NARROW = "narrow";	
	private static final Map<String, Map<String, String>> TYPE_MAPPINGS;
	static {
		TYPE_MAPPINGS = new HashMap<String, Map<String, String>>();
//		Map<String, String> typeMappings = new HashMap<String, String>();
//		typeMappings.put("com.ibm.ivj.ejb.runtime.AbstractAccessBean", "com.ibm.commerce.persistence.AbstractJPAEntityAccessBean");
//		TYPE_MAPPINGS.put("com.ibm.commerce.order.utils.OrderRecycler", typeMappings);
	}
	private ASTParser iASTParser;
	private ApplicationInfo iApplicationInfo;
	private IType iType;
	private String iSimpleTypeName;
	private String iSimpleJpaName;
	private Map<String, String> iTypeMappings;
	private static final String COPYRIGHT_FIELD_NAME = "COPYRIGHT";
	
	public EntityReferencingTypeGenerator(ASTParser astParser, ApplicationInfo applicationInfo, IType type) {
		iASTParser = astParser;
		iApplicationInfo = applicationInfo;
		iType = type;
		iSimpleTypeName = iType.getTypeQualifiedName();
		iSimpleJpaName = "JPA" + iSimpleTypeName;
		if (applicationInfo.isStubType(type.getFullyQualifiedName())) {
			iSimpleJpaName = "$" + iSimpleJpaName;
		}
		iTypeMappings = TYPE_MAPPINGS.get(type.getFullyQualifiedName('.'));
	}
	
	public void generate(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("generate entity referencing class " + iType.getFullyQualifiedName('.'), IProgressMonitor.UNKNOWN);
			portEntityReferencingType(progressMonitor);
		}
		finally {
			progressMonitor.done();
		}
	}
	
	public void portEntityReferencingType(IProgressMonitor progressMonitor) {
		try {
			iASTParser.setProject(iType.getJavaProject());
			iASTParser.setSource(iType.getCompilationUnit());
			iASTParser.setResolveBindings(true);
			CompilationUnit astCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
			astCompilationUnit.recordModifications();
			astCompilationUnit.accept(new EntityReferencingTypePortVisitor());
			IDocument document = JavaUtil.getDocument(iType);
			TextEdit edits = astCompilationUnit.rewrite(document, null);
			edits.apply(document);
			ICompilationUnit compilationUnit = iType.getPackageFragment().createCompilationUnit(iSimpleJpaName + ".java", document.get(), true, new SubProgressMonitor(progressMonitor, 100));
			iApplicationInfo.getBackupUtil(iType.getJavaProject().getProject()).addGeneratedFile2((IFile) compilationUnit.getResource(), new SubProgressMonitor(progressMonitor, 100));
			iApplicationInfo.incrementGeneratedAssetCount();
		}
		catch (BadLocationException e) {
			e.printStackTrace();
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private class EntityReferencingTypePortVisitor extends PortVisitor {
		public EntityReferencingTypePortVisitor() {
			super(iApplicationInfo, iType.getJavaProject(), true);
		}

		public boolean visit(MethodDeclaration methodDeclaration) {
			TargetExceptionUtil.getEntityReferencingTypeUnhandledTargetExceptions(iApplicationInfo, iType.getJavaProject(), methodDeclaration.getBody());
			return super.visit(methodDeclaration);
		}
		
		public boolean visit(Initializer initializer) {
			TargetExceptionUtil.getEntityReferencingTypeUnhandledTargetExceptions(iApplicationInfo, iType.getJavaProject(), initializer.getBody());
			return super.visit(initializer);
		}
		
		public boolean visit(SimpleName simpleName) {
			if (simpleName.getIdentifier().equals(iSimpleTypeName)) {
				super.replaceASTNode(simpleName, simpleName.getAST().newName(iSimpleJpaName));
			}
			else {
				super.visit(simpleName);
			}
			return false;
		}
		
		public boolean visit(TypeDeclaration typeDeclaration) {
			boolean visitChildren = true;
			@SuppressWarnings("unchecked")
			List<Type> superInterfaceTypes = typeDeclaration.superInterfaceTypes();
			for (int i = 0; i < superInterfaceTypes.size(); i++) {
				Type superInterfaceType = superInterfaceTypes.get(i);
				ITypeBinding superInterfaceTypeBinding = superInterfaceType.resolveBinding();
				if (superInterfaceTypeBinding != null && iApplicationInfo.isAccessBeanInterfaceType(superInterfaceTypeBinding.getQualifiedName())) {
					superInterfaceType.delete();
					i--;
				}
			}
			ITypeBinding typeBinding = typeDeclaration.resolveBinding();
			if (typeBinding != null) {
				String qualifiedName = typeBinding.getQualifiedName();
				AccessBeanSubclassInfo accessBeanSubclassInfo = iApplicationInfo.getAccessBeanSubclassInfoForType(qualifiedName);
				if (accessBeanSubclassInfo != null) {
					typeDeclaration.accept(new AccessBeanSubclassPortVisitor(accessBeanSubclassInfo, this));
					visitChildren = false;
				}
				else if (iApplicationInfo.isStubType(qualifiedName)) {
					typeDeclaration.accept(new StubPortVisitor());
					visitChildren = false;
				}
			}
			return visitChildren;
		}
		
		public String getTypeMapping(String typeName) {
			String typeMapping = null;
			if (iTypeMappings != null && iTypeMappings.containsKey(typeName)) {
				typeMapping = iTypeMappings.get(typeName);
			}
			else {
				typeMapping = super.getTypeMapping(typeName);
			}
			return typeMapping;
		}
	}
	
	private class AccessBeanSubclassPortVisitor extends PortVisitor {
		private EntityReferencingTypePortVisitor iEntityReferencingTypePortVisitor;
		private AccessBeanSubclassInfo iAccessBeanSubclassInfo;
		private EntityInfo iEntityInfo;
		
		public AccessBeanSubclassPortVisitor(AccessBeanSubclassInfo accessBeanSubclassInfo, EntityReferencingTypePortVisitor entityReferencingTypePortVisitor) {
			super(iApplicationInfo, iType.getJavaProject());
			iAccessBeanSubclassInfo = accessBeanSubclassInfo;
			iEntityInfo = accessBeanSubclassInfo.getEntityInfo();
			iEntityReferencingTypePortVisitor = entityReferencingTypePortVisitor;
		}

		public boolean visit(TypeDeclaration typeDeclaration) {
			boolean visitChildren = true;
			ITypeBinding typeBinding = typeDeclaration.resolveBinding();
			if (typeBinding == null || !typeBinding.getQualifiedName().equals(iAccessBeanSubclassInfo.getName())) {
				typeDeclaration.accept(iEntityReferencingTypePortVisitor);
				visitChildren = false;
			}
			return visitChildren;
		}
		
		public boolean visit(MethodDeclaration methodDeclaration) {
			boolean visitChildren = true;
			String methodKey = JavaUtil.getMethodKey(methodDeclaration);
			if (COMMIT_COPY_HELPER.equals(methodKey)) {
				methodDeclaration.delete();
				visitChildren = false;
			}
			//caiduan-Resolve the compilation error after "Update Entity Reference" for the JPATermConditionCopy.java, should never generate "narrow" method in new JPA files
			if (NARROW.equals(methodDeclaration.getName().getIdentifier())) {
				methodDeclaration.delete();
				visitChildren = false;
			}
			else {
				TargetExceptionUtil.getUnhandledTargetExceptions(iApplicationInfo, iType.getJavaProject(), methodDeclaration.getBody());
				AST ast = methodDeclaration.getAST();
				if (!methodDeclaration.isConstructor()) {
					String methodName = AccessBeanUtil.getNewAccessBeanMethodName(iEntityInfo, methodDeclaration.getName().getIdentifier());
					if (methodName != null) {
						methodDeclaration.setName(ast.newSimpleName(methodName));
					}
				}
				else {
					@SuppressWarnings("unchecked")
					List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
					if (parameters != null && parameters.size() == 1) {
						ITypeBinding typeBinding = parameters.get(0).getType().resolveBinding();
						if (typeBinding != null && EJB_OBJECT.equals(typeBinding.getQualifiedName())) {
							parameters.get(0).setType(ast.newSimpleType(ast.newName(iEntityInfo.getEntityClassInfo().getQualifiedClassName())));
						}
					}
				}
				IJavaProject javaProject = iType.getJavaProject();
				TargetExceptionInfo targetExceptionInfo = TargetExceptionUtil.getAccessBeanSubclassMethodUnhandledTargetExceptions(iAccessBeanSubclassInfo, methodKey);
				if (targetExceptionInfo == null) {
					System.out.println("no target exceptions for "+methodKey+" in "+iAccessBeanSubclassInfo.getName());
				}
				else {
					//caiduan-Resolve the compilation error after "Update Entity Reference" for the JPATermConditionCopy-start
					if((methodDeclaration.getParent()) instanceof TypeDeclaration
						&& ((TypeDeclaration)methodDeclaration.getParent()).getName().getIdentifier().equals("JPATermConditionCopy")
							&& (methodDeclaration.getName().getIdentifier().equals("getProductSetAdjustments")
									|| methodDeclaration.getName().getIdentifier().equals("findProductSetIdsByTCIdAndType")
									|| methodDeclaration.getName().getIdentifier().equals("getExclusionProductSets")
									|| methodDeclaration.getName().getIdentifier().equals("getInclusionProductSets"))){
								//do nothing for the exception throw declaration
						List<Name> thrownExceptions = methodDeclaration.thrownExceptions();
						List<Name> newExceptions = new java.util.ArrayList<Name>();
						for (int i = 0; i < thrownExceptions.size(); i++) {	
							Name thrownException = thrownExceptions.get(i); 							
//							System.out.println("exception:" + thrownException.getFullyQualifiedName());
							if (!thrownException.getFullyQualifiedName().equals("javax.ejb.FinderException")
									&&!thrownException.getFullyQualifiedName().equals("java.rmi.RemoteException")){
								newExceptions.add(thrownException);
							}													
						}
						thrownExceptions.clear();
						thrownExceptions.addAll(newExceptions);	
					}else{//caiduan-end
					
					Collection<String> targetExceptions = TargetExceptionUtil.getFilteredExceptions(javaProject, targetExceptionInfo.getTargetExceptions());
					@SuppressWarnings("unchecked")
					List<Name> thrownExceptions = methodDeclaration.thrownExceptions();
		
					for (int i = 0; i < thrownExceptions.size(); i++) {
						Name thrownException = thrownExceptions.get(i);
						ITypeBinding exceptionTypeBinding = thrownException.resolveTypeBinding();
						if (exceptionTypeBinding != null) {
							String declaredException = exceptionTypeBinding.getQualifiedName();
							boolean exceptionUsed = false;
							for (String targetException : targetExceptions) {
								if (TargetExceptionUtil.catchHandlesException(javaProject, declaredException, targetException)) {
									exceptionUsed = true;
									break;
								}
							}
							if (!exceptionUsed) {
								//System.out.println("removing declared exception "+declaredException);
								thrownExceptions.remove(i);
								i--;
							}
						}
					}
					//caiduan-start
					}//caiduan-end
				}
				visitChildren = super.visit(methodDeclaration);
			}
			return visitChildren;
		}
		
		public boolean visit(MethodInvocation methodInvocation) {
			boolean visitChildren = true;
			if (methodInvocation.getExpression() == null || methodInvocation.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
				String methodName = AccessBeanUtil.getNewAccessBeanMethodName(iEntityInfo, methodInvocation.getName().getIdentifier());
				if (methodName != null) {
					methodInvocation.setName(methodInvocation.getAST().newSimpleName(methodName));
				}
			}
			if (AccessBeanUtil.isCachedAccessBeanInitializeMethodInvocation(iApplicationInfo, methodInvocation)) {
				visitChildren = AccessBeanUtil.portCachedAccessBeanInitializeMethodInvocation(iApplicationInfo, methodInvocation, this);
			}
			else {
				visitChildren = super.visit(methodInvocation);
			}
			return visitChildren;
		}
		
		public boolean visit(SuperMethodInvocation superMethodInvocation) {
			boolean visitChildren = true;
			String methodName = AccessBeanUtil.getNewAccessBeanMethodName(iEntityInfo, superMethodInvocation.getName().getIdentifier());
			if (methodName != null) {
				superMethodInvocation.setName(superMethodInvocation.getAST().newSimpleName(methodName));
			}
			if (AccessBeanUtil.isCachedAccessBeanInitializeSuperMethodInvocation(iApplicationInfo, superMethodInvocation)) {
				visitChildren = AccessBeanUtil.portCachedAccessBeanInitializeSuperMethodInvocation(iApplicationInfo, superMethodInvocation, this);
			}
			else {
				visitChildren = super.visit(superMethodInvocation);
			}
			return visitChildren;
		}
		
		public boolean visit(ExpressionStatement expressionStatement) {
			boolean visitChildren = true;
			Expression statementExpression = expressionStatement.getExpression();
			if (statementExpression.getNodeType() == ASTNode.SUPER_METHOD_INVOCATION) {
				SuperMethodInvocation superMethodInvocation = (SuperMethodInvocation) statementExpression;
				String methodName = superMethodInvocation.getName().getIdentifier();
				int argumentCount = superMethodInvocation.arguments().size();
				if (COMMIT_COPY_HELPER.equals(methodName) || (REFRESH_COPY_HELPER.equals(methodName) && argumentCount == 1)) {
					expressionStatement.delete();
					visitChildren = false;
				}
				else if (REFRESH_COPY_HELPER.equals(methodName) || GET_EJB_REF.equals(methodName)) {
					superMethodInvocation.setName(superMethodInvocation.getAST().newSimpleName(INSTANTIATE_ENTITY));
					visitChildren = false;
				}
			}
			else if (statementExpression.getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation methodInvocation = (MethodInvocation) statementExpression;
				if (methodInvocation.getExpression() == null || methodInvocation.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
					String methodName = methodInvocation.getName().getIdentifier();
					int argumentCount = methodInvocation.arguments().size();
					if (COMMIT_COPY_HELPER.equals(methodName) || (REFRESH_COPY_HELPER.equals(methodName) && argumentCount == 1)) {
						expressionStatement.delete();
						visitChildren = false;
					}
					else if (REFRESH_COPY_HELPER.equals(methodName) || GET_EJB_REF.equals(methodName)) {
						methodInvocation.setName(methodInvocation.getAST().newSimpleName(INSTANTIATE_ENTITY));
						visitChildren = false;
					}
				}
			}
			if (visitChildren) {
				visitChildren = super.visit(expressionStatement);
			}
			return visitChildren;
		}
		
		public boolean visit(Initializer initializer) {
			TargetExceptionUtil.getUnhandledTargetExceptions(iApplicationInfo, iType.getJavaProject(), initializer.getBody());
			return super.visit(initializer);
		}
		
		public boolean visit(SimpleName simpleName) {
			if (simpleName.getIdentifier().equals(iSimpleTypeName)) {
				super.replaceASTNode(simpleName, simpleName.getAST().newName(iSimpleJpaName));
			}
			else {
				super.visit(simpleName);
			}
			return false;
		}
	}
	
	private class StubPortVisitor extends PortVisitor {
		public StubPortVisitor() {
			super(iApplicationInfo, iType.getJavaProject());
		}

		public boolean visit(Initializer initializer) {
			initializer.delete();
			return false;
		}
		
		public boolean visit(FieldDeclaration fieldDeclaration) {
			boolean visitChildren = true;
			@SuppressWarnings("unchecked")
			List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
			if (Modifier.isPrivate(fieldDeclaration.getModifiers())) {
				fieldDeclaration.delete();
				visitChildren = false;
			}
			else {
				Type type = fieldDeclaration.getType();
				for (VariableDeclarationFragment fragment : fragments) {
					String fieldName = fragment.getName().getIdentifier();
					if (!COPYRIGHT_FIELD_NAME.equals(fieldName)) {
						fragment.setInitializer(getTypedStubExpression(type));
					}
				}
				@SuppressWarnings("unchecked")
				List<IExtendedModifier> modifiers = fieldDeclaration.modifiers();
				for (int i = 0; i < modifiers.size(); i++) {
					if (modifiers.get(i).isAnnotation()) {
						modifiers.remove(i);
						i--;
					}
				}
			}
			if (visitChildren) {
				visitChildren = super.visit(fieldDeclaration);
			}
			return visitChildren;
		}
		
		public boolean visit(TypeDeclaration typeDeclaration) {
			
			
			boolean visitChildren = true;
			if (Modifier.isPrivate(typeDeclaration.getModifiers())) {
				//caiduan-Resolve the compilation error after "Update Entity Reference" for the $JPAUpdateStaticEARContentUsingFTPCmdImpl
				if(typeDeclaration.getName().getFullyQualifiedName().equals("ServerInfo") 
						&& ((TypeDeclaration)(typeDeclaration.getParent())).getName().getFullyQualifiedName().equals("$JPAUpdateStaticEARContentUsingFTPCmdImpl")){
					//Clear the ServerInfo body thoroughly.
					List<ASTNode> list = typeDeclaration.bodyDeclarations();
					list.clear();
					
				}else{
					typeDeclaration.delete();
					visitChildren = false;
				}
			}
			if (visitChildren) {
				visitChildren = super.visit(typeDeclaration);
			}
			return visitChildren;
		}
		
		public boolean visit(MethodDeclaration methodDeclaration) {
			boolean visitChildren = true;
			if (Modifier.isPrivate(methodDeclaration.getModifiers()) && !methodDeclaration.isConstructor()) {
				visitChildren = false;
				methodDeclaration.delete();
			}
			else {
				@SuppressWarnings("unchecked")
				List<IExtendedModifier> modifiers = methodDeclaration.modifiers();
				for (int i = 0; i < modifiers.size(); i++) {
					if (modifiers.get(i).isAnnotation()) {
						modifiers.remove(i);
						i--;
					}
				}
				@SuppressWarnings("unchecked")
				List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
				for (SingleVariableDeclaration parameter : parameters) {
					@SuppressWarnings("unchecked")
					List<IExtendedModifier> parameterModifiers = parameter.modifiers();
					for (int i = 0; i < parameterModifiers.size(); i++) {
						if (parameterModifiers.get(i).isAnnotation()) {
							parameterModifiers.remove(i);
							i--;
						}
					}
				}
				if (methodDeclaration.getBody() != null) {
					AST ast = methodDeclaration.getAST();
					if (methodDeclaration.isConstructor()) {
						@SuppressWarnings("unchecked")
						List<Statement> statements = methodDeclaration.getBody().statements();
						for (int i = 0; i < statements.size(); i++) {
							Statement statement = statements.get(i);
							if (statement.getNodeType() != ASTNode.SUPER_CONSTRUCTOR_INVOCATION && statement.getNodeType() != ASTNode.CONSTRUCTOR_INVOCATION) {
								statement.delete();
								i--;
							}
						}
					}
					else {
						Block newBody = ast.newBlock();
						@SuppressWarnings("unchecked")
						List<Statement> newStatements = newBody.statements();
						ReturnStatement newReturnStatement = ast.newReturnStatement();
						Type returnType = methodDeclaration.getReturnType2();
						newStatements.add(newReturnStatement);
						if (returnType != null) {
							newReturnStatement.setExpression(getTypedStubExpression(returnType));
						}
						replaceASTNode(methodDeclaration.getBody(), newBody);
					}
				}
			}
			if (visitChildren) {
				visitChildren = super.visit(methodDeclaration);
			}
			return visitChildren;
		}
		
		public boolean visit(SimpleName simpleName) {
			if (simpleName.getIdentifier().equals(iSimpleTypeName)) {
				super.replaceASTNode(simpleName, simpleName.getAST().newName(iSimpleJpaName));
			}
			else {
				super.visit(simpleName);
			}
			return false;
		}
		
		private Expression getTypedStubExpression(Type type) {
			Expression expression = null;
			if (type.isPrimitiveType()) {
				PrimitiveType primitiveType = (PrimitiveType) type;
				if (primitiveType.getPrimitiveTypeCode() != PrimitiveType.VOID) {
					if (primitiveType.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
						expression = type.getAST().newBooleanLiteral(false);
					}
					else {
						expression = type.getAST().newNumberLiteral("0");
					}
				}
			}
			else {
				expression = type.getAST().newNullLiteral();
			}
			return expression;
		}
	}
}
