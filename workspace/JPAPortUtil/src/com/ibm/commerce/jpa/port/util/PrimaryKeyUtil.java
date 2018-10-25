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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;

import com.ibm.commerce.jpa.port.generators.PortVisitor;
import com.ibm.commerce.jpa.port.info.AccessBeanInfo.NullConstructorParameter;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.KeyClassConstructorInfo;

public class PrimaryKeyUtil {
	private static final String STRING = "java.lang.String";
	private static final String TO_STRING = "toString";
	private static final String IN_ENTITY_TYPE = "InEntityType";
	private static final String GET_PRIMARY_KEY = "getPrimaryKey";
	
	public static boolean isPrimaryKeyFieldAccess(ApplicationInfo applicationInfo, FieldAccess fieldAccess) {
		boolean match = false;
		if (fieldAccess.getExpression() != null) {
			ITypeBinding typeBinding = fieldAccess.getExpression().resolveTypeBinding();
			if (typeBinding != null) {
				String qualifiedName = typeBinding.getQualifiedName();
				if (applicationInfo.isEntityKeyType(qualifiedName)) {
					EntityInfo entityInfo = applicationInfo.getEntityInfoForType(qualifiedName);
					match = entityInfo.getFieldInfoByName(fieldAccess.getName().getIdentifier()) != null;
				}
			}
		}
		return match;
	}
	
	public static boolean portPrimaryKeyFieldAccess(ApplicationInfo applicationInfo, FieldAccess fieldAccess, PortVisitor portVisitor) {
		// ((TermConditionKey) a.__getKey()).referenceNumber
		boolean visitChildren = true;
		Expression keyObjectExpression = fieldAccess.getExpression();
		AccessBeanExpressionVisitor accessBeanExpressionVisitor = new AccessBeanExpressionVisitor(applicationInfo);
		keyObjectExpression.accept(accessBeanExpressionVisitor);
		String fieldName = fieldAccess.getName().getIdentifier();
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(keyObjectExpression.resolveTypeBinding().getQualifiedName());
		FieldInfo fieldInfo = entityInfo.getFieldInfoByName(fieldName);
		AST ast = fieldAccess.getAST();
		String getterName = fieldInfo.getTargetGetterName();
		if (accessBeanExpressionVisitor.getAccessBeanExpression() != null) {
			MethodInvocation newMethodInvocation = ast.newMethodInvocation();
			if (fieldInfo.getHasStringConversionAccessMethod()) {
				getterName += IN_ENTITY_TYPE;
			}
			newMethodInvocation.setName(ast.newSimpleName(getterName));
			Expression accessBeanExpression = accessBeanExpressionVisitor.getAccessBeanExpression();
			accessBeanExpression.accept(portVisitor);
			accessBeanExpression.delete();
			newMethodInvocation.setExpression(accessBeanExpression);
			portVisitor.replaceASTNode(fieldAccess, newMethodInvocation);
			visitChildren = false;
		}
		else if (entityInfo.getEntityKeyClassInfo() != null) {
			MethodInvocation newMethodInvocation = ast.newMethodInvocation();
			newMethodInvocation.setName(ast.newSimpleName(getterName));
			keyObjectExpression.accept(portVisitor);
			//keyObjectExpression.delete();
			newMethodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, keyObjectExpression));
			portVisitor.replaceASTNode(fieldAccess, newMethodInvocation);
			visitChildren = false;
		}
		else if (entityInfo.getKeyFields().size() == 1) {
			portVisitor.replaceASTNode(fieldAccess, ASTNode.copySubtree(ast, keyObjectExpression));
			visitChildren = false;
		}
		return visitChildren;
	}
	
	public static boolean isPrimaryKeyMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean match = false;
		if (methodInvocation.getExpression() != null) {
			ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
			if (typeBinding != null) {
				String qualifiedName = typeBinding.getQualifiedName();
				if (applicationInfo.isEntityKeyType(qualifiedName)) {
					EntityInfo entityInfo = applicationInfo.getEntityInfoForType(qualifiedName);
					match = entityInfo.getFieldInfoByGetterName(methodInvocation.getName().getIdentifier()) != null;
				}
			}
		}
		return match;
	}
	
	public static boolean portPrimaryKeyMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = true;
		Expression keyObjectExpression = methodInvocation.getExpression();
		AccessBeanExpressionVisitor accessBeanExpressionVisitor = new AccessBeanExpressionVisitor(applicationInfo);
		keyObjectExpression.accept(accessBeanExpressionVisitor);
		String methodName = methodInvocation.getName().getIdentifier();
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(keyObjectExpression.resolveTypeBinding().getQualifiedName());
		FieldInfo fieldInfo = entityInfo.getFieldInfoByGetterName(methodName);
		AST ast = methodInvocation.getAST();
		String getterName = fieldInfo.getTargetGetterName();
		if (accessBeanExpressionVisitor.getAccessBeanExpression() != null) {
			MethodInvocation newMethodInvocation = ast.newMethodInvocation();
			if (fieldInfo.getHasStringConversionAccessMethod()) {
				getterName += IN_ENTITY_TYPE;
			}
			newMethodInvocation.setName(ast.newSimpleName(getterName));
			Expression accessBeanExpression = accessBeanExpressionVisitor.getAccessBeanExpression();
			accessBeanExpression.accept(portVisitor);
			accessBeanExpression.delete();
			newMethodInvocation.setExpression(accessBeanExpression);
			portVisitor.replaceASTNode(methodInvocation, newMethodInvocation);
			visitChildren = false;
		}
		else if (entityInfo.getEntityKeyClassInfo() != null) {
			MethodInvocation newMethodInvocation = ast.newMethodInvocation();
			if (fieldInfo.getRelatedEntityInfo() != null) {
				getterName = fieldInfo.getRelatedEntityInfo().getGetterName();
			}
			newMethodInvocation.setName(ast.newSimpleName(getterName));
			keyObjectExpression.accept(portVisitor);
			newMethodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, keyObjectExpression));
			portVisitor.replaceASTNode(methodInvocation, newMethodInvocation);
			visitChildren = false;
		}
		else if (entityInfo.getKeyFields().size() == 1) {
			portVisitor.replaceASTNode(methodInvocation, ASTNode.copySubtree(ast, keyObjectExpression));
			visitChildren = false;
		}
		return visitChildren;
	}
	
	public static boolean isPrimaryKeyQualifiedName(ApplicationInfo applicationInfo, QualifiedName qualifiedName) {
		boolean match = false;
		ITypeBinding typeBinding = qualifiedName.getQualifier().resolveTypeBinding();
		if (typeBinding != null) {
			String typeName = typeBinding.getQualifiedName();
			if (applicationInfo.isEntityKeyType(typeName)) {
				EntityInfo entityInfo = applicationInfo.getEntityInfoForType(typeName);
				match = entityInfo.getFieldInfoByName(qualifiedName.getName().getIdentifier()) != null;
			}
		}
		return match;
	}
	
	public static boolean portPrimaryQualifiedName(ApplicationInfo applicationInfo, QualifiedName qualifiedName, PortVisitor portVisitor) {
		boolean visitChildren = true;
		Name keyObjectName = qualifiedName.getQualifier();
		String fieldName = qualifiedName.getName().getIdentifier();
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(keyObjectName.resolveTypeBinding().getQualifiedName());
		FieldInfo fieldInfo = entityInfo.getFieldInfoByName(fieldName);
		AST ast = qualifiedName.getAST();
		String getterName = fieldInfo.getRelatedEntityInfo() == null ? fieldInfo.getTargetGetterName() : fieldInfo.getRelatedEntityInfo().getGetterName();
		if (entityInfo.getEntityKeyClassInfo() != null) {
			MethodInvocation newMethodInvocation = ast.newMethodInvocation();
			newMethodInvocation.setName(ast.newSimpleName(getterName));
			keyObjectName.accept(portVisitor);
			newMethodInvocation.setExpression((Name) ASTNode.copySubtree(ast, keyObjectName));
			portVisitor.replaceASTNode(qualifiedName, newMethodInvocation);
			visitChildren = false;
		}
		else if (entityInfo.getKeyFields().size() == 1) {
			portVisitor.replaceASTNode(qualifiedName, ASTNode.copySubtree(ast, keyObjectName));
			visitChildren = false;
		}
		return visitChildren;
	}
	
	public static boolean isPrimaryKeyClassInstanceCreation(ApplicationInfo applicationInfo, ClassInstanceCreation classInstanceCreation) {
		boolean match = false;
		ITypeBinding typeBinding = classInstanceCreation.resolveTypeBinding();
		if (typeBinding != null && applicationInfo.isEntityKeyType(typeBinding.getQualifiedName())) {
			match = true;
		}
		return match;
	}
	
	public static boolean portPrimaryKeyClassInstanceCreation(ApplicationInfo applicationInfo, ClassInstanceCreation classInstanceCreation, PortVisitor portVisitor) {
		boolean visitChildren = true;
		@SuppressWarnings("unchecked")
		List<Expression> arguments = classInstanceCreation.arguments();
		if (arguments.size() == 1) {
			arguments.get(0).accept(portVisitor);
			Expression key = arguments.get(0);
			key.delete();
			portVisitor.replaceASTNode(classInstanceCreation, key);
			visitChildren = false;
		}
		return visitChildren;
	}
	
	public static List<Expression> getPortedInitKeyArguments(EntityInfo entityInfo, ClassInstanceCreation classInstanceCreation, PortVisitor portVisitor) {
		List<Expression> portedInitKeyArguments = new ArrayList<Expression>();
		List<FieldInfo> entityKeyFields = entityInfo.getKeyFields();
		for (int i = 0; i < entityKeyFields.size(); i++) {
			portedInitKeyArguments.add(null);
		}
		@SuppressWarnings("unchecked")
		List<Expression> arguments = classInstanceCreation.arguments();
		KeyClassConstructorInfo constructorInfo = getMatchingKeyClassConstructorInfo(entityInfo, arguments);
		if (constructorInfo != null) {
			List<FieldInfo> constructorFields = constructorInfo.getFields();
			for (FieldInfo fieldInfo : constructorFields) {
				int fieldIndex = entityKeyFields.indexOf(fieldInfo);
				ITypeBinding argumentTypeBinding = arguments.get(0).resolveTypeBinding();
				arguments.get(0).accept(portVisitor);
				Expression argument = arguments.get(0);
				argument.delete();
				if (argumentTypeBinding != null) {
					String argumentType = argumentTypeBinding.getQualifiedName();
					if (!STRING.equals(argumentType)) {
						NullConstructorParameter nullConstructorParameter = entityInfo.getAccessBeanInfo().getNullConstructorParameterByName(fieldInfo.getFieldName());
						if (nullConstructorParameter != null && nullConstructorParameter.getConverterClassName() != null) {
							argument = convertExpressionToString(argument);
						}
					}
				}
				portedInitKeyArguments.set(fieldIndex, argument);
			}
		}
		return portedInitKeyArguments;
	}
	
	public static KeyClassConstructorInfo getMatchingKeyClassConstructorInfo(EntityInfo entityInfo, List<Expression> arguments) {
		KeyClassConstructorInfo matchingConstructorInfo = null;
		List<KeyClassConstructorInfo> keyClassConstructors = entityInfo.getKeyClassConstructors();
		if (keyClassConstructors != null) {
			for (KeyClassConstructorInfo current : keyClassConstructors) {
				if (arguments.size() == current.getFields().size()) {
					matchingConstructorInfo = current;
					break;
				}
			}
		}
		return matchingConstructorInfo;
	}
	
	public static Expression convertExpressionToString(Expression expression) {
		AST ast = expression.getAST();
		if (expression.getNodeType() == ASTNode.CONDITIONAL_EXPRESSION) {
			ConditionalExpression conditionalExpression = (ConditionalExpression) expression;
			if (conditionalExpression.getThenExpression().getNodeType() != ASTNode.NULL_LITERAL) {
				Expression thenExpression = conditionalExpression.getThenExpression();
				MethodInvocation argumentToStringMethodInvocation = ast.newMethodInvocation();
				argumentToStringMethodInvocation.setName(ast.newSimpleName(TO_STRING));
				conditionalExpression.setThenExpression(argumentToStringMethodInvocation);
				argumentToStringMethodInvocation.setExpression(thenExpression);
			}
			if (conditionalExpression.getElseExpression().getNodeType() != ASTNode.NULL_LITERAL) {
				Expression elseExpression = conditionalExpression.getElseExpression();
				MethodInvocation argumentToStringMethodInvocation = ast.newMethodInvocation();
				argumentToStringMethodInvocation.setName(ast.newSimpleName(TO_STRING));
				conditionalExpression.setElseExpression(argumentToStringMethodInvocation);
				argumentToStringMethodInvocation.setExpression(elseExpression);
			}
		}
		else {
			MethodInvocation argumentToStringMethodInvocation = ast.newMethodInvocation();
			argumentToStringMethodInvocation.setName(ast.newSimpleName(TO_STRING));
			argumentToStringMethodInvocation.setExpression(expression);
			expression = argumentToStringMethodInvocation;
		}
		return expression;
		
	}
	
	public static boolean isPrimaryKeyAssignment(ApplicationInfo applicationInfo, Assignment assignment) {
		boolean primaryKeyAssignment = false;
		Expression leftHandSide = assignment.getLeftHandSide();
		if (leftHandSide.getNodeType() == ASTNode.FIELD_ACCESS) {
			FieldAccess fieldAccess = (FieldAccess) leftHandSide;
			ITypeBinding typeBinding = fieldAccess.getExpression().resolveTypeBinding();
			if (typeBinding != null && applicationInfo.isEntityKeyType(typeBinding.getQualifiedName())) {
				primaryKeyAssignment = true;
			}
		}
		else if (leftHandSide.getNodeType() == ASTNode.QUALIFIED_NAME) {
			QualifiedName qualifiedName = (QualifiedName) leftHandSide;
			ITypeBinding typeBinding = qualifiedName.getQualifier().resolveTypeBinding();
			if (typeBinding != null && applicationInfo.isEntityKeyType(typeBinding.getQualifiedName())) {
				primaryKeyAssignment = true;
			}
		}
		return primaryKeyAssignment;
	}
	
	public static boolean portPrimaryKeyAssignment(ApplicationInfo applicationInfo, Assignment assignment, PortVisitor portVisitor) {
		boolean visitChildren = true;
		Expression leftHandSide = assignment.getLeftHandSide();
		Expression keyObjectExpression = null;
		String fieldName = null;
		if (leftHandSide.getNodeType() == ASTNode.FIELD_ACCESS) {
			FieldAccess fieldAccess = (FieldAccess) leftHandSide;
			fieldAccess.getExpression().accept(portVisitor);
			keyObjectExpression = fieldAccess.getExpression();
			fieldName = fieldAccess.getName().getIdentifier();
		}
		else if (leftHandSide.getNodeType() == ASTNode.QUALIFIED_NAME) {
			QualifiedName qualifiedName = (QualifiedName) leftHandSide;
			keyObjectExpression = qualifiedName.getQualifier();
			fieldName = qualifiedName.getName().getIdentifier();
		}
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(keyObjectExpression.resolveTypeBinding().getQualifiedName());
		FieldInfo fieldInfo = entityInfo.getFieldInfoByName(fieldName);
		AST ast = assignment.getAST();
		String setterName = fieldInfo.getTargetSetterName();
		if (entityInfo.getEntityKeyClassInfo() != null) {
			MethodInvocation newMethodInvocation = ast.newMethodInvocation();
			newMethodInvocation.setName(ast.newSimpleName(setterName));
			newMethodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, keyObjectExpression));
			assignment.getRightHandSide().accept(portVisitor);
			@SuppressWarnings("unchecked")
			List<Expression> arguments = newMethodInvocation.arguments();
			arguments.add((Expression) ASTNode.copySubtree(ast, assignment.getRightHandSide()));
			portVisitor.replaceASTNode(assignment, newMethodInvocation);
			visitChildren = false;
		}
		return visitChildren;
	}
	
	public static boolean isGetPrimaryKeyCastExpression(ApplicationInfo applicationInfo, CastExpression castExpression) {
		//(com.ibm.commerce.context.objects.ContextManagementKey) argContextmanagement.getPrimaryKey()
		boolean result = false;
		ITypeBinding typeBinding = castExpression.getType().resolveBinding();
		Expression expression = castExpression.getExpression();
		if (typeBinding != null && applicationInfo.isEntityKeyType(typeBinding.getQualifiedName()) && expression.getNodeType() == ASTNode.METHOD_INVOCATION) {
			MethodInvocation methodInvocation = (MethodInvocation) expression;
			result = methodInvocation.getName().getIdentifier().equals(GET_PRIMARY_KEY) && methodInvocation.getExpression() != null && methodInvocation.getExpression().resolveTypeBinding() != null && applicationInfo.isEntityInterfaceType(methodInvocation.getExpression().resolveTypeBinding().getQualifiedName());
		}
		return result;
	}
	
	public static boolean portGetPrimaryKeyCastExpression(ApplicationInfo applicationInfo, CastExpression castExpression, PortVisitor portVisitor) {
		boolean visitChildren = true;
		MethodInvocation methodInvocation = (MethodInvocation) castExpression.getExpression();
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(methodInvocation.getExpression().resolveTypeBinding().getQualifiedName());
		List<FieldInfo> keyFields = entityInfo.getKeyFields();
		if (keyFields.size() == 1) {
			AST ast = methodInvocation.getAST();
			MethodInvocation newMethodInvocation = ast.newMethodInvocation();
			newMethodInvocation.setName(ast.newSimpleName(keyFields.get(0).getTargetGetterName()));
			Expression expression = methodInvocation.getExpression();
			expression.delete();
			newMethodInvocation.setExpression(expression);
			expression.accept(portVisitor);
			portVisitor.replaceASTNode(castExpression, newMethodInvocation);
			visitChildren = false;
		}
		return visitChildren;
	}
	
	public static class AccessBeanExpressionVisitor extends ASTVisitor {
		private ApplicationInfo iApplicationInfo;
		private Expression iAccessBeanExpression;
		
		public AccessBeanExpressionVisitor(ApplicationInfo applicationInfo) {
			iApplicationInfo = applicationInfo;
		}
		
		public void preVisit(ASTNode node) {
			if (node instanceof Expression) {
				ITypeBinding typeBinding = ((Expression) node).resolveTypeBinding();
				if (typeBinding != null) {
					String qualifiedName = typeBinding.getQualifiedName();
					if (iApplicationInfo.isAccessBeanType(qualifiedName) || iApplicationInfo.isDataClassType(qualifiedName)) {
						iAccessBeanExpression = (Expression) node;
					}
				}
			}
		}
		
		public Expression getAccessBeanExpression() {
			return iAccessBeanExpression;
		}
	}
}
