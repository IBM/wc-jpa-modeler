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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.ibm.commerce.jpa.port.generators.PortVisitor;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;

public class AbstractEntityDataUtil {
	private final static String ABSTRACT_ENTITY_DATA = "com.ibm.etools.ejb.client.runtime.AbstractEntityData";
	
	public static boolean isAbstractEntityDataType(IType type) {
		boolean result = false;
		try {
			while (!result && type != null && type.getSuperclassName() != null) {
				type = JavaUtil.resolveType(type, type.getSuperclassName());
				if (type != null) {
					result = ABSTRACT_ENTITY_DATA.equals(type.getFullyQualifiedName('.'));
				}
			}
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static boolean isAbstractEntityDataType(ITypeBinding typeBinding) {
		boolean result = false;
		while (!result && typeBinding != null && typeBinding.getSuperclass() != null) {
			if (ABSTRACT_ENTITY_DATA.equals(typeBinding.getSuperclass().getQualifiedName())) {
				result = true;
				break;
			}
			typeBinding = typeBinding.getSuperclass();
		}
		return result;
	}
	
	public static boolean isAbstractEntityDataConversionMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation) {
		//private Collection convertRecordsIntoValueObjects(final Collection ejbs) {
		// in com.ibm.commerce.edp.activitylog.ActivityLoggerBean
		boolean result = false;
		return result;
	}
	
	public static boolean isGetAbstractEntityDataExpressionStatement(ApplicationInfo applicationInfo, ExpressionStatement expressionStatement) {
		boolean result = false;
		if (expressionStatement.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION && isGetAbstractEntityDataMethodInvocation(applicationInfo, (MethodInvocation) expressionStatement.getExpression())) {
			result = true;
		}
		return result;
	}
	
	public static boolean isGetAbstractEntityDataMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation) {
		boolean result = false;
		if (methodInvocation.getExpression() != null && methodInvocation.resolveMethodBinding() != null && methodInvocation.resolveMethodBinding().getReturnType() != null) {
			Expression methodExpression = methodInvocation.getExpression();
			ITypeBinding typeBinding = methodExpression.resolveTypeBinding();
			if (typeBinding != null) {
				String qualifiedTypeName = typeBinding.getQualifiedName();
				if (applicationInfo.isEntityInterfaceType(qualifiedTypeName)) {
					result = isAbstractEntityDataType(methodInvocation.resolveMethodBinding().getReturnType());
				}
			}
		}
		return result;
	}
	
	public static boolean portGetAbstractEntityDataMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = false;
		methodInvocation.getExpression().accept(portVisitor);
		Expression expression = methodInvocation.getExpression();
		methodInvocation.setExpression(null);
		portVisitor.replaceASTNode(methodInvocation, expression);
		return visitChildren;
	}
	
	public static boolean isSetAbstractEntityDataExpressionStatement(ApplicationInfo applicationInfo, ExpressionStatement expressionStatement) {
		boolean result = false;
		if (expressionStatement.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION && isSetAbstractEntityDataMethodInvocation(applicationInfo, (MethodInvocation) expressionStatement.getExpression())) {
			result = true;
		}
		return result;
	}
	
	public static boolean isSetAbstractEntityDataMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation) {
		boolean result = false;
		if (methodInvocation.getExpression() != null && methodInvocation.resolveMethodBinding() != null && methodInvocation.resolveMethodBinding().getParameterTypes().length == 1) {
			Expression methodExpression = methodInvocation.getExpression();
			ITypeBinding typeBinding = methodExpression.resolveTypeBinding();
			if (typeBinding != null) {
				String qualifiedTypeName = typeBinding.getQualifiedName();
				if (applicationInfo.isEntityInterfaceType(qualifiedTypeName)) {
					result = isAbstractEntityDataType(methodInvocation.resolveMethodBinding().getParameterTypes()[0]);
				}
			}
		}
		return result;
	}
	
	public static boolean portSetAbstractEntityDataMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = false;
		methodInvocation.getExpression().accept(portVisitor);
		Expression expression = methodInvocation.getExpression();
		methodInvocation.setExpression(null);
		portVisitor.replaceASTNode(methodInvocation, expression);
		return visitChildren;
	}
}
