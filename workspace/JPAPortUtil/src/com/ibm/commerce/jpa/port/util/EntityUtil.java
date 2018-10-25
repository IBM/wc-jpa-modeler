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

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.ibm.commerce.jpa.port.generators.PortVisitor;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.RelatedEntityInfo;
import com.ibm.commerce.jpa.port.info.UserMethodInfo;

public class EntityUtil {
	private static final String GET = "get";
	private static final String EJB_OBJECT = "javax.ejb.EJBObject";
	private static final String OBJECT = "java.lang.Object";
	
	public static boolean isEntityType(ApplicationInfo applicationInfo, Expression expression) {
		boolean result = false;
		ITypeBinding typeBinding = expression.resolveTypeBinding();
		if (typeBinding != null) {
			String qualifiedTypeName = typeBinding.getQualifiedName();
			if (applicationInfo.isEntityType(qualifiedTypeName)) {
				result = true;
			}
		}
		return result;
	}
	
	public static boolean isEntityMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation) {
		boolean result = false;
		Expression methodExpression = methodInvocation.getExpression();
		if (methodExpression != null && isEntityType(applicationInfo, methodExpression)) {
			result = true;
		}
		return result;
	}
	
	public static boolean portEntityMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = true;
		AST ast = methodInvocation.getAST();
		Expression methodExpression = methodInvocation.getExpression();
		String entityType = methodExpression.resolveTypeBinding().getQualifiedName();
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(entityType);
		IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
		if (methodBinding != null) {
			FieldInfo fieldInfo = null;
			String methodKey = JavaUtil.getMethodKey(methodBinding);
			UserMethodInfo userMethodInfo = entityInfo.getUserMethodInfo(methodKey);
			if (userMethodInfo != null && userMethodInfo.getFieldInfo() != null) {
				fieldInfo = userMethodInfo.getFieldInfo();
			}
			else if (methodKey.startsWith(GET)) {
				fieldInfo = entityInfo.getFieldInfoByGetterName(methodKey);
			}
			if (fieldInfo != null) {
				RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
				if (fieldInfo.getRelatedEntityInfo() != null && methodKey.startsWith(GET)) {
					String relatedEntityGetterName = relatedEntityInfo.getGetterName();
					if (relatedEntityGetterName == null) {
						relatedEntityGetterName = "get" + Character.toUpperCase(relatedEntityInfo.getFieldName().charAt(0)) + relatedEntityInfo.getFieldName().substring(1);
					}
					String referencedFieldGetterName = fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null ? fieldInfo.getReferencedFieldInfo().getTargetGetterName() : fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo().getGetterName();
					ConditionalExpression conditionalExpression = ast.newConditionalExpression();
					MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
					getterMethodInvocation.setName(ast.newSimpleName(relatedEntityGetterName));
					methodExpression.accept(portVisitor);
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
					portVisitor.replaceASTNode(methodInvocation, conditionalExpression);
					visitChildren = false;
				}
			}
		}
		return visitChildren;
	}
	
	public static String getJpaName(IType type) {
		String typeName = type.getTypeQualifiedName('.');
		String packageName = type.getPackageFragment().getElementName();
		return packageName + ".JPA" + typeName;
	}
	
	public static String getJpaStubName(IType type) {
		String typeName = type.getTypeQualifiedName('.');
		String packageName = type.getPackageFragment().getElementName();
		return packageName + ".$JPA" + typeName;
	}
	
	public static boolean isEjbObjectVariableDeclarationStatement(VariableDeclarationStatement variableDeclarationStatement) {
		boolean result = false;
		Type type = variableDeclarationStatement.getType();
		if (type != null) {
			ITypeBinding typeBinding = type.resolveBinding();
			if (typeBinding != null && typeBinding.getQualifiedName().equals(EJB_OBJECT)) {
				result = true;
			}
		}
		return result;
	}
	
	public static boolean portEjbObjectVariableDeclarationStatement(VariableDeclarationStatement variableDeclarationStatement, PortVisitor portVisitor) {
		boolean visitChildren = false;
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
		for (VariableDeclarationFragment fragment : fragments) {
			fragment.accept(portVisitor);
		}
		AST ast = variableDeclarationStatement.getAST();
		variableDeclarationStatement.setType(ast.newSimpleType(ast.newName(OBJECT)));
		TargetExceptionUtil.portVariableDeclarationStatement(variableDeclarationStatement, portVisitor);
		return visitChildren;
	}
	
}
