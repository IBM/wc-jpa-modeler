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
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.ibm.commerce.jpa.port.generators.PortVisitor;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.KeyClassConstructorInfo;

public class DataClassUtil {
	private static final String GET_PRIMARY_KEY = "getPrimaryKey";

	public static boolean isDataClassType(ApplicationInfo applicationInfo, Expression expression) {
		boolean result = false;
		ITypeBinding typeBinding = expression.resolveTypeBinding();
		if (typeBinding != null) {
			String qualifiedTypeName = typeBinding.getQualifiedName();
			if (applicationInfo.isDataClassType(qualifiedTypeName)) {
				result = true;
			}
		}
		return result;
	}
	
	public static boolean isGetPrimaryKeyMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation) {
		boolean result = false;
		Expression methodExpression = methodInvocation.getExpression();
		if (methodExpression != null && isDataClassType(applicationInfo, methodExpression) && GET_PRIMARY_KEY.equals(methodInvocation.getName().getIdentifier())) {
			result = true;
		}
		return result;
	}
	
	public static boolean portGetPrimaryKeyMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		String dataClassType = methodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(dataClassType);
		return portGetPrimaryKeyMethodInvocation(entityInfo, methodInvocation, portVisitor);
	}
	
	public static boolean portGetPrimaryKeyMethodInvocation(EntityInfo entityInfo, MethodInvocation methodInvocation, ASTNode replacementNode, PortVisitor portVisitor) {
		boolean visitChildren = false;
		AST ast = methodInvocation.getAST();
		if (entityInfo.getEntityKeyClassInfo() != null) {
			Expression dataClassExpression = null;
			if (methodInvocation.getExpression() != null) {
				methodInvocation.getExpression().accept(portVisitor);
				dataClassExpression = methodInvocation.getExpression();
				methodInvocation.setExpression(null);
			}
			ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
			classInstanceCreation.setType(ast.newSimpleType(ast.newName(entityInfo.getEntityKeyClassInfo().getQualifiedClassName())));
			@SuppressWarnings("unchecked")
			List<Expression> arguments = classInstanceCreation.arguments();
			List<KeyClassConstructorInfo> keyClassConstructors = entityInfo.getKeyClassConstructors();
			for (KeyClassConstructorInfo keyClassConstructorInfo : keyClassConstructors) {
				if (keyClassConstructorInfo.getFields().size() > 0) {
					List<FieldInfo> fields = keyClassConstructorInfo.getFields();
					for (FieldInfo fieldInfo : fields) {
						MethodInvocation fieldGetterMethodInvocation = ast.newMethodInvocation();
						fieldGetterMethodInvocation.setName(ast.newSimpleName(fieldInfo.getTargetGetterName()));
						if (dataClassExpression != null) {
							fieldGetterMethodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, dataClassExpression));
						}
						arguments.add(fieldGetterMethodInvocation);
					}
					break;
				}
			}
			portVisitor.replaceASTNode(replacementNode, classInstanceCreation);
			visitChildren = false;
		}
		else if (entityInfo.getKeyFields().size() == 1) {
			FieldInfo fieldInfo = entityInfo.getKeyFields().get(0);
			MethodInvocation newMethodInvocation = ast.newMethodInvocation();
			newMethodInvocation.setName(ast.newSimpleName(fieldInfo.getTargetGetterName()));
			if (methodInvocation.getExpression() != null) {
				methodInvocation.getExpression().accept(portVisitor);
				Expression dataClassExpression = methodInvocation.getExpression();
				methodInvocation.setExpression(null);
				newMethodInvocation.setExpression(dataClassExpression);
			}
			portVisitor.replaceASTNode(replacementNode, newMethodInvocation);
			visitChildren = false;
		}
		return visitChildren;
	}
	
	public static boolean portGetPrimaryKeyMethodInvocation(EntityInfo entityInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		return portGetPrimaryKeyMethodInvocation(entityInfo, methodInvocation, methodInvocation, portVisitor);
	}
	
	public static boolean isGetPrimaryKeyCastExpression(ApplicationInfo applicationInfo, CastExpression castExpression) {
		boolean result = false;
		if (castExpression.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION) {
			MethodInvocation methodInvocation = (MethodInvocation) castExpression.getExpression();
			result = isGetPrimaryKeyMethodInvocation(applicationInfo, methodInvocation);
		}
		return result;
	}
	
	public static boolean portGetPrimaryKeyCastExpression(EntityInfo entityInfo, CastExpression castExpression, PortVisitor portVisitor) {
		return portGetPrimaryKeyMethodInvocation(entityInfo, (MethodInvocation) castExpression.getExpression(), castExpression, portVisitor);
	}
	
	public static boolean portGetPrimaryKeyCastExpression(ApplicationInfo applicationInfo, CastExpression castExpression, PortVisitor portVisitor) {
		MethodInvocation methodInvocation = (MethodInvocation) castExpression.getExpression();
		String dataClassType = methodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(dataClassType);
		return portGetPrimaryKeyCastExpression(entityInfo, castExpression, portVisitor);
	}
}
