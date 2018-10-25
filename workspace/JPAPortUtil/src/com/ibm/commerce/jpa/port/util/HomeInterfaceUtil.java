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

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.ibm.commerce.jpa.port.generators.PortVisitor;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;

public class HomeInterfaceUtil {
	private static final String JAVA_COMP_ENV = "java:comp/env/";
	private static final String CREATE = "create";
	
	public static boolean isGetHomeInterfaceMethodDeclaration(ApplicationInfo applicationInfo, MethodDeclaration methodDeclaration) {
		boolean result = false;
		IMethodBinding methodBinding = methodDeclaration.resolveBinding();
		if (methodBinding != null) {
			ITypeBinding returnTypeBinding = methodBinding.getReturnType();
			if (returnTypeBinding != null) {
				if (applicationInfo.isHomeInterfaceType(returnTypeBinding.getQualifiedName())) {
					result = true;
				}
			}
		}
		return result;
	}
	
	public static boolean isGetHomeInterfaceMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation) {
		boolean result = false;
		IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
		if (methodBinding != null) {
			ITypeBinding returnTypeBinding = methodBinding.getReturnType();
			if (returnTypeBinding != null) {
				if (applicationInfo.isHomeInterfaceType(returnTypeBinding.getQualifiedName())) {
					result = true;
				}
			}
		}
		return result;
	}
	
	public static boolean portGetHomeInterfaceMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = false;
		IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(methodBinding.getReturnType().getQualifiedName());
		if (entityInfo.getAccessBeanInfo().getDataClassType()) {
			methodInvocation.delete();
		}
		else {
			if(entityInfo.getAccessBeanInfo().getAccessBeanName() != null) {
				String accessBeanName = entityInfo.getAccessBeanInfo().getQualifiedAccessBeanName();
				AST ast = methodInvocation.getAST();
				ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
				classInstanceCreation.setType(ast.newSimpleType(ast.newName(accessBeanName)));
				portVisitor.replaceASTNode(methodInvocation, classInstanceCreation);
			}
		}
		return visitChildren;
	}
	
	public static boolean isHomeInterfaceVariableDeclarationStatement(ApplicationInfo applicationInfo, VariableDeclarationStatement variableDeclarationStatement) {
		boolean result = false;
		Type type = variableDeclarationStatement.getType();
		if (type != null) {
			ITypeBinding typeBinding = type.resolveBinding();
			if (typeBinding != null && applicationInfo.isHomeInterfaceType(typeBinding.getQualifiedName())) {
				result = true;
			}
		}
		return result;
	}
	
	public static boolean portHomeInterfaceVariableDeclarationStatement(ApplicationInfo applicationInfo, VariableDeclarationStatement variableDeclarationStatement, PortVisitor portVisitor) {
		boolean visitChildren = false;
		Type type = variableDeclarationStatement.getType();
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(type.resolveBinding().getQualifiedName());
		if (entityInfo.getAccessBeanInfo().getDataClassType()) {
			variableDeclarationStatement.delete();
		}
		else {
			HomeVariableUsedVisitor homeVariableUsedVisitor = new HomeVariableUsedVisitor(applicationInfo, variableDeclarationStatement);
			if (homeVariableUsedVisitor.isHomeVariableUsed()) {
				AST ast = variableDeclarationStatement.getAST();
				variableDeclarationStatement.setType(ast.newSimpleType(ast.newName(entityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName())));
				@SuppressWarnings("unchecked")
				List<VariableDeclarationFragment> variableDeclarationFragments = variableDeclarationStatement.fragments();
				for (VariableDeclarationFragment variableDeclarationFragment : variableDeclarationFragments) {
					if (variableDeclarationFragment.getInitializer() != null && variableDeclarationFragment.getInitializer().getNodeType() != ASTNode.NULL_LITERAL) {
						ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
						classInstanceCreation.setType(ast.newSimpleType(ast.newName(entityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName())));
						variableDeclarationFragment.setInitializer(classInstanceCreation);
					}
				}
				TargetExceptionUtil.portVariableDeclarationStatement(variableDeclarationStatement, portVisitor);
			}
			else {
				variableDeclarationStatement.delete();
			}
		}
		return visitChildren;
	}
	
	public static boolean isHomeInterfaceJndiNameField(ApplicationInfo applicationInfo, FieldDeclaration fieldDeclaration) {
		boolean result = false;
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
		if (fragments.size() == 1) {
			VariableDeclarationFragment variableDeclarationFragment = fragments.get(0);
			if (variableDeclarationFragment.getInitializer() != null) {
				Expression initializerExpression = variableDeclarationFragment.getInitializer();
				if (initializerExpression.getNodeType() == ASTNode.STRING_LITERAL) {
					StringLiteral stringLiteral = (StringLiteral) initializerExpression;
					String literalValue = stringLiteral.getLiteralValue();
					if (applicationInfo.isEntityJndiName(literalValue)) {
						result = true;
					}
					else if (literalValue.startsWith(JAVA_COMP_ENV) && applicationInfo.isEntityJndiName(literalValue.substring(JAVA_COMP_ENV.length()))) {
						result = true;
					}
				}
			}
		}
		return result;
	}
	
	public static boolean isHomeInterfaceCreateMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation) {
		boolean result = false;
		if (CREATE.equals(methodInvocation.getName().getIdentifier())) {
			IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
			if (methodBinding != null) {
				if (applicationInfo.isHomeInterfaceType(methodBinding.getDeclaringClass().getQualifiedName())) {
					result = true;
				}
			}
		}
		return result;
	}
	
	public static boolean portHomeInterfaceCreateMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = false;
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(methodInvocation.resolveMethodBinding().getDeclaringClass().getQualifiedName());
		AST ast = methodInvocation.getAST();
		ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
		classInstanceCreation.setType(ast.newSimpleType(ast.newName(entityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName())));
		@SuppressWarnings("unchecked")
		List<Expression> classInstanceCreationArguments = classInstanceCreation.arguments();
		@SuppressWarnings("unchecked")
		List<Expression> methodInvocationArguments = methodInvocation.arguments();
		while (methodInvocationArguments.size() > 0) {
			methodInvocationArguments.get(0).accept(portVisitor);
			Expression argument = methodInvocationArguments.get(0);
			argument.delete();
			classInstanceCreationArguments.add(argument);
		}
		portVisitor.replaceASTNode(methodInvocation, classInstanceCreation);
		return visitChildren;
	}
	
	private static class HomeVariableUsedVisitor extends ASTVisitor {
		private ApplicationInfo iApplicationInfo;
		private boolean iHomeVariableUsed = false;
		private VariableDeclarationStatement iVariableDeclarationStatement;
		private String iVariableName;
		
		public HomeVariableUsedVisitor(ApplicationInfo applicationInfo, VariableDeclarationStatement variableDeclarationStatement) {
			iApplicationInfo = applicationInfo;
			iVariableDeclarationStatement = variableDeclarationStatement;
			@SuppressWarnings("unchecked")
			List<VariableDeclarationFragment> fragments = iVariableDeclarationStatement.fragments();
			for (VariableDeclarationFragment fragment : fragments) {
				iVariableName = fragment.getName().getIdentifier();
			}
		}
		
		public boolean isHomeVariableUsed() {
			iVariableDeclarationStatement.getParent().accept(this);
			return iHomeVariableUsed;
		}
		
		public boolean visit(VariableDeclarationStatement variableDeclarationStatement) {
			boolean visitChildren = true;
			if (variableDeclarationStatement == iVariableDeclarationStatement) {
				visitChildren = false;
			}
			return visitChildren;
		}
		
		public boolean visit(SimpleName simpleName) {
			if (simpleName.getIdentifier().equals(iVariableName)) {
				IBinding binding = simpleName.resolveBinding();
				if (binding != null && binding.getKind() == ITypeBinding.VARIABLE && !simpleName.isDeclaration()) {
					iHomeVariableUsed = true;
				}
			}
			return false;
		}
		
		public boolean visit(QualifiedName qualifiedName) {
			return false;
		}
		
		public boolean visit(MethodInvocation methodInvocation) {
			boolean visitChildren = true;
			if (HomeInterfaceUtil.isHomeInterfaceCreateMethodInvocation(iApplicationInfo, methodInvocation)) {
				visitChildren = false;
			}
			return visitChildren;
		}
	}
}
