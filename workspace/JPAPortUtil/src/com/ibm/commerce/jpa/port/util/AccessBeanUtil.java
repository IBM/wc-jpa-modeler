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
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.ibm.commerce.jpa.port.generators.PortVisitor;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.UserMethodInfo;

public class AccessBeanUtil {
	private static final String REFRESH_COPY_HELPER = "refreshCopyHelper";
	private static final String INSTANTIATE_ENTITY = "instantiateEntity";
	private static final String IN_EJB_TYPE = "InEJBType";
	private static final String IN_ENTITY_TYPE = "InEntityType";
	private static final String SET_INIT_KEY = "setInitKey_";
	private static final String SET_INIT_PRIMARY_KEY = "setInit_primaryKey";
	private static final String SET_INIT = "setInit_";
	private static final String SET = "set";
	private static final String GET = "get";
	private static final String GET_EJB_REF = "getEJBRef";
	private static final String SET_EJB_REF = "setEJBRef";
	private static final String GET_ENTITY = "getEntity";
	private static final String SET_ENTITY = "setEntity";
	private static final String CACHED_ENTITY_ACCESS_BEAN = "com.ibm.commerce.datatype.AbstractEntityAccessBeanFinderResult.CachedEntityAccessBean";
	private static final String COMPACT_ENTITY_ACCESS_BEAN = "com.ibm.commerce.datatype.AbstractEntityAccessBeanFinderResult.CompactEntityAccessBean";
	private static final String OBJECT = "java.lang.Object";
	private static final String INITIALIZE = "initialize";
	private static final String EJB_OBJECT = "javax.ejb.EJBObject";
	private static final String REMOVE = "remove";
	private static final String ABSTRACT_ACCESS_BEAN = "com.ibm.ivj.ejb.runtime.AbstractAccessBean";
	private static final String ABSTRACT_ENTITY_ACCESS_BEAN = "com.ibm.ivj.ejb.runtime.AbstractEntityAccessBean";
	private static final String NARROW = "narrow";
	private static final String GET_REFRESH_ONCE_ACCESS_BEAN_HELPER = "getRefreshOnceAccessBeanHelper";
	private static final String GET_INSTANTIATE_ONCE_ACCESS_BEAN_HELPER = "getInstantiateOnceAccessBeanHelper";
	
	public static boolean isAccessBeanType(ApplicationInfo applicationInfo, String typeName) {
		boolean result = false;
		if (applicationInfo.isAccessBeanType(typeName) || ABSTRACT_ACCESS_BEAN.equals(typeName) || ABSTRACT_ENTITY_ACCESS_BEAN.equals(typeName)) {
			result = true;
		}
		return result;
	}
	
	public static boolean isAccessBeanType(ApplicationInfo applicationInfo, Expression expression) {
		boolean result = false;
		ITypeBinding typeBinding = expression.resolveTypeBinding();
		if (typeBinding != null) {
			String qualifiedTypeName = typeBinding.getQualifiedName();
			if (isAccessBeanType(applicationInfo, qualifiedTypeName)) {
				result = true;
			}
		}
		return result;
	}
	
	public static boolean isAccessBeanSubclass(ApplicationInfo applicationInfo, Expression expression) {
		boolean result = false;
		ITypeBinding typeBinding = expression.resolveTypeBinding();
		if (typeBinding != null) {
			String qualifiedTypeName = typeBinding.getQualifiedName();
			if (applicationInfo.isAccessBeanSubclass(qualifiedTypeName)) {
				result = true;
			}
		}
		return result;
	}
	
	public static void updateAccessBeanMethodName(ApplicationInfo applicationInfo, PortVisitor portVisitor, MethodInvocation methodInvocation) {
		ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
		if (typeBinding != null) {
			String newMethodName = null;
			String methodName = methodInvocation.getName().getIdentifier();
			if (methodInvocation.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation expressionMethodInvocation = (MethodInvocation) methodInvocation.getExpression();
				if (EntityUtil.isEntityMethodInvocation(applicationInfo, expressionMethodInvocation)) {
					String entityType = expressionMethodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
					EntityInfo entityInfo = applicationInfo.getEntityInfoForType(entityType);
					IMethodBinding methodBinding = expressionMethodInvocation.resolveMethodBinding();
					if (methodBinding != null) {
						String methodKey = JavaUtil.getMethodKey(methodBinding);
						UserMethodInfo userMethodInfo = entityInfo.getUserMethodInfo(methodKey);
						if (userMethodInfo != null && userMethodInfo.getRelatedEntityInfo() != null) {
							if (methodName.endsWith(IN_EJB_TYPE)) {
								newMethodName = methodName.substring(0, methodName.length() - IN_EJB_TYPE.length());
							}
						}
					}
				}
			}
			if (newMethodName == null) {
				String accessBeanType = typeBinding.getQualifiedName();
				newMethodName = getNewAccessBeanMethodName(applicationInfo, accessBeanType, methodName);
			}
			if (newMethodName != null) {
				SimpleName newName = methodInvocation.getAST().newSimpleName(newMethodName);
				portVisitor.replaceASTNode(methodInvocation.getName(), newName);
			}
		}
	}
	
	public static String getNewAccessBeanMethodName(ApplicationInfo applicationInfo, String accessBeanType, String methodName) {
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(accessBeanType);
		return getNewAccessBeanMethodName(entityInfo, methodName);
	}
	
	public static String getNewAccessBeanMethodName(EntityInfo entityInfo, String methodName) {
		String newMethodName = null;
		if (entityInfo != null) {
			if (methodName.endsWith(IN_EJB_TYPE)) {
				newMethodName = methodName.substring(0, methodName.lastIndexOf(IN_EJB_TYPE));
				FieldInfo fieldInfo = entityInfo.getFieldInfoByGetterName(newMethodName);
				if (fieldInfo == null || fieldInfo.getHasStringConversionAccessMethod()) {
					newMethodName += IN_ENTITY_TYPE;
				}
			}
			else if (SET_INIT_PRIMARY_KEY.equals(methodName) && entityInfo.getKeyFields().size() == 1) {
				FieldInfo fieldInfo = entityInfo.getKeyFields().get(0);
				newMethodName = SET_INIT_KEY + fieldInfo.getTargetFieldName();
			}
			else if (methodName.startsWith(SET_INIT_KEY)) {
				String fieldName = methodName.substring(SET_INIT_KEY.length());
				FieldInfo fieldInfo = entityInfo.getFieldInfoByName(fieldName);
				if (fieldInfo == null && entityInfo.getKeyFields().size() == 1) {
					fieldInfo = entityInfo.getKeyFields().get(0);
				}
				if (fieldInfo == null && "LanguageId".equals(fieldName)) {
					fieldInfo = entityInfo.getFieldInfoByName("Language_id");
				}
				if (fieldInfo != null && !fieldName.equals(fieldInfo.getTargetFieldName())) {
					newMethodName = SET_INIT_KEY + fieldInfo.getTargetFieldName();
				}
			}
			else if (methodName.startsWith(SET_INIT)) {
				String fieldName = methodName.substring(SET_INIT.length());
				FieldInfo fieldInfo = entityInfo.getFieldInfoByName(fieldName);
				if (fieldInfo == null && "LanguageId".equals(fieldName)) {
					fieldInfo = entityInfo.getFieldInfoByName("Language_id");
				}
				if (fieldInfo != null && !fieldName.equals(fieldInfo.getTargetFieldName())) {
					newMethodName = SET_INIT_KEY + fieldInfo.getTargetFieldName();
				}
			}
			else if (methodName.equals(REFRESH_COPY_HELPER)) {
				newMethodName = INSTANTIATE_ENTITY;
			}
			else if (methodName.equals(GET_EJB_REF)) {
				newMethodName = GET_ENTITY;
			}
			else if (methodName.equals(SET_EJB_REF)) {
				newMethodName = SET_ENTITY;
			}
			else if (methodName.equals(GET_REFRESH_ONCE_ACCESS_BEAN_HELPER)) {
				newMethodName = GET_INSTANTIATE_ONCE_ACCESS_BEAN_HELPER;
			}
			else if (methodName.startsWith(SET)) {
				
			}
			else if (methodName.startsWith(GET)) {
				
			}
		}
		else if (methodName.equals(GET_EJB_REF)) {
			newMethodName = GET_ENTITY;
		}
		return newMethodName;
	}
	
	public static boolean isGetEntityCastExpression(ApplicationInfo applicationInfo, CastExpression castExpression) {
		boolean result = false;
		ITypeBinding typeBinding = castExpression.getType().resolveBinding();
		if (typeBinding != null && applicationInfo.isEntityInterfaceType(typeBinding.getQualifiedName()) && castExpression.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION) {
			MethodInvocation methodInvocation = (MethodInvocation) castExpression.getExpression();
			result = methodInvocation.getName().getIdentifier().equals(GET_EJB_REF) && methodInvocation.getExpression() != null && methodInvocation.getExpression().resolveTypeBinding() != null && applicationInfo.isAccessBeanType(methodInvocation.getExpression().resolveTypeBinding().getQualifiedName());
		}
		return result;
	}
	
	public static boolean portGetEntityCastExpression(ApplicationInfo applicationInfo, CastExpression castExpression, PortVisitor portVisitor) {
		boolean visitChildren = false;
		MethodInvocation methodInvocation = (MethodInvocation) castExpression.getExpression();
		Expression accessBeanExpression = methodInvocation.getExpression();
		accessBeanExpression.accept(portVisitor);
		accessBeanExpression.delete();
		portVisitor.replaceASTNode(castExpression, accessBeanExpression);
		return visitChildren;
	}
	
	public static boolean isPortExemptAccessBeanSubclass(IType type) {
		return isCachedEntityAccessBean(type) || isCompactEntityAccessBean(type) || EntityReferenceUtil.isPortExemptEntityReferencingType(type);
	}
	
	public static boolean isCachedEntityAccessBean(IType type) {
		boolean result = false;
		try {
			String[] interfaceNames = type.getSuperInterfaceNames();
			if (interfaceNames != null) {
				for (String interfaceName : interfaceNames) {
					IType interfaceType = JavaUtil.resolveType(type, interfaceName);
					if (interfaceType != null && interfaceType.getFullyQualifiedName('.').equals(CACHED_ENTITY_ACCESS_BEAN)) {
						result = true;
						break;
					}
				}
			}
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static boolean isCachedEntityAccessBean(ITypeBinding typeBinding) {
		boolean result = false;
		ITypeBinding[] interfaces = typeBinding.getInterfaces();
		if (interfaces != null) {
			for (ITypeBinding interfaceTypeBinding : interfaces) {
				if (interfaceTypeBinding.getQualifiedName().equals(CACHED_ENTITY_ACCESS_BEAN)) {
					result = true;
					break;
				}
			}
		}
		return result;
	}
	
	public static boolean isCompactEntityAccessBean(IType type) {
		boolean result = false;
		try {
			String[] interfaceNames = type.getSuperInterfaceNames();
			if (interfaceNames != null) {
				for (String interfaceName : interfaceNames) {
					IType interfaceType = JavaUtil.resolveType(type, interfaceName);
					if (interfaceType != null && interfaceType.getFullyQualifiedName('.').equals(COMPACT_ENTITY_ACCESS_BEAN)) {
						result = true;
						break;
					}
				}
			}
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static boolean isCompactEntityAccessBean(ITypeBinding typeBinding) {
		boolean result = false;
		ITypeBinding[] interfaces = typeBinding.getInterfaces();
		if (interfaces != null) {
			for (ITypeBinding interfaceTypeBinding : interfaces) {
				if (interfaceTypeBinding.getQualifiedName().equals(COMPACT_ENTITY_ACCESS_BEAN)) {
					result = true;
					break;
				}
			}
		}
		return result;
	}
	
	public static boolean isAccessBeanSubclass(ApplicationInfo applicationInfo, IType type) {
		boolean result = false;
		try {
			IType superclassType = JavaUtil.resolveType(type, type.getSuperclassName());
			while (superclassType != null && !superclassType.getFullyQualifiedName('.').equals(OBJECT)) {
				if (applicationInfo.isAccessBeanType(superclassType.getFullyQualifiedName('.'))) {
					result = true;
					break;
				}
				superclassType = JavaUtil.resolveType(type, type.getSuperclassName());
			}
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static boolean isAccessBeanSubclass(ApplicationInfo applicationInfo, ITypeBinding typeBinding) {
		boolean result = false;
		ITypeBinding superclassTypeBinding = typeBinding.getSuperclass();
		while (superclassTypeBinding != null && !superclassTypeBinding.getQualifiedName().equals(OBJECT)) {
			if (applicationInfo.isAccessBeanType(superclassTypeBinding.getQualifiedName())) {
				result = true;
				break;
			}
			superclassTypeBinding = superclassTypeBinding.getSuperclass();
		}
		return result;
	}
	
	public static boolean isCachedAccessBeanInitializeMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation) {
		boolean result = false;
		if ((methodInvocation.getExpression() == null || methodInvocation.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) && methodInvocation.arguments().size() == 1 && INITIALIZE.equals(methodInvocation.getName().getIdentifier())) {
			IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
			if (methodBinding != null) {
				ITypeBinding declaringClassTypeBinding = methodBinding.getDeclaringClass();
				if (AccessBeanUtil.isCompactEntityAccessBean(declaringClassTypeBinding) || AccessBeanUtil.isCachedEntityAccessBean(declaringClassTypeBinding)) {
					ITypeBinding parameterType = methodBinding.getParameterTypes()[0];
					if (applicationInfo.isAccessBeanType(parameterType.getQualifiedName())) {
						result = true;
					}
				}
			}
		}
		return result;
	}

	public static boolean portCachedAccessBeanInitializeMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = true;
		Expression argument = (Expression) methodInvocation.arguments().get(0);
		argument.delete();
		argument.accept(portVisitor);
		AST ast = methodInvocation.getAST();
		MethodInvocation getEntityMethodInvocation = ast.newMethodInvocation();
		getEntityMethodInvocation.setName(ast.newSimpleName(GET_ENTITY));
		getEntityMethodInvocation.setExpression(argument);
		MethodInvocation setEntityMethodInvocation = ast.newMethodInvocation();
		setEntityMethodInvocation.setName(ast.newSimpleName(SET_ENTITY));
		@SuppressWarnings("unchecked")
		List<Expression> arguments = setEntityMethodInvocation.arguments();
		arguments.add(getEntityMethodInvocation);
		portVisitor.replaceASTNode(methodInvocation, setEntityMethodInvocation);
		return visitChildren;
	}
	
	public static boolean isCachedAccessBeanInitializeSuperMethodInvocation(ApplicationInfo applicationInfo, SuperMethodInvocation superMethodInvocation) {
		boolean result = false;
		if (superMethodInvocation.arguments().size() == 1 && INITIALIZE.equals(superMethodInvocation.getName().getIdentifier())) {
			IMethodBinding methodBinding = superMethodInvocation.resolveMethodBinding();
			if (methodBinding != null) {
				ITypeBinding declaringClassTypeBinding = methodBinding.getDeclaringClass();
				if (AccessBeanUtil.isCompactEntityAccessBean(declaringClassTypeBinding) || AccessBeanUtil.isCachedEntityAccessBean(declaringClassTypeBinding)) {
					ITypeBinding parameterType = methodBinding.getParameterTypes()[0];
					if (applicationInfo.isAccessBeanType(parameterType.getQualifiedName())) {
						result = true;
					}
				}
			}
		}
		return result;
	}
	
	public static boolean portCachedAccessBeanInitializeSuperMethodInvocation(ApplicationInfo applicationInfo, SuperMethodInvocation superMethodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = true;
		Expression argument = (Expression) superMethodInvocation.arguments().get(0);
		argument.delete();
		argument.accept(portVisitor);
		AST ast = superMethodInvocation.getAST();
		MethodInvocation getEntityMethodInvocation = ast.newMethodInvocation();
		getEntityMethodInvocation.setName(ast.newSimpleName(GET_ENTITY));
		getEntityMethodInvocation.setExpression(argument);
		MethodInvocation setEntityMethodInvocation = ast.newMethodInvocation();
		setEntityMethodInvocation.setName(ast.newSimpleName(SET_ENTITY));
		@SuppressWarnings("unchecked")
		List<Expression> arguments = setEntityMethodInvocation.arguments();
		arguments.add(getEntityMethodInvocation);
		portVisitor.replaceASTNode(superMethodInvocation, setEntityMethodInvocation);
		return visitChildren;
	}
	
	public static boolean isEjbObjectRemoveBlock(Block block) {
		//	{
		//		EJBObject ejbObj = (aAbstractAccessBean).getEJBRef();
		//		if (ejbObj != null ) {
		//			ejbObj.remove();
		//		}
		//	}
		boolean result = false;
		@SuppressWarnings("unchecked")
		List<Statement> statements = block.statements();
		if (statements.size() == 2 && statements.get(0).getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT && statements.get(1).getNodeType() == ASTNode.IF_STATEMENT) {
			IfStatement ifStatement = (IfStatement) statements.get(1);
			if (ifStatement.getThenStatement().getNodeType() == ASTNode.BLOCK) {
				@SuppressWarnings("unchecked")
				List<Statement> thenStatements = ((Block) ifStatement.getThenStatement()).statements();
				if (thenStatements.size() == 1 && thenStatements.get(0).getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
					ExpressionStatement thenStatement = (ExpressionStatement) thenStatements.get(0);
					if (thenStatement.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION) {
						IMethodBinding methodBinding = ((MethodInvocation) thenStatement.getExpression()).resolveMethodBinding();
						if (methodBinding != null) {
							if (REMOVE.equals(methodBinding.getName()) && EJB_OBJECT.equals(methodBinding.getDeclaringClass().getQualifiedName())) {
								result = true;
							}
						}
					}
				}
			}
		}
		if (!result && statements.size() > 2) {
//			EJBObject ejbObject = ecrab.getEJBRef();
//			ejbObject.remove();
			for (int i = 0; i < statements.size() - 1; i++) {
				Statement statement = statements.get(i);
				Statement nextStatement = statements.get(i + 1);
				if (statement.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT && nextStatement.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
					ITypeBinding typeBinding = variableDeclarationStatement.getType().resolveBinding();
					if (typeBinding != null && EJB_OBJECT.equals(typeBinding.getQualifiedName())) {
						ExpressionStatement expressionStatement = (ExpressionStatement) nextStatement;
						if (expressionStatement.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION) {
							IMethodBinding methodBinding = ((MethodInvocation) expressionStatement.getExpression()).resolveMethodBinding();
							if (methodBinding != null) {
								if (REMOVE.equals(methodBinding.getName()) && EJB_OBJECT.equals(methodBinding.getDeclaringClass().getQualifiedName())) {
									result = true;
									break;
								}
							}
						}
					}
				}
			}
		}
		return result;
	}
	
	public static boolean portEjbObjectRemoveBlock(Block block, PortVisitor portVisitor) {
		boolean visitChildren = true;
		@SuppressWarnings("unchecked")
		List<Statement> statements = block.statements();
		if (statements.size() == 2 && statements.get(0).getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT && statements.get(1).getNodeType() == ASTNode.IF_STATEMENT) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statements.get(0);
			@SuppressWarnings("unchecked")
			List<VariableDeclarationFragment> variableDeclarationFragments = variableDeclarationStatement.fragments();
			if (variableDeclarationFragments.size() == 1) {
				VariableDeclarationFragment variableDeclarationFragment = variableDeclarationFragments.get(0);
				if (variableDeclarationFragment.getInitializer() != null && variableDeclarationFragment.getNodeType() == ASTNode.METHOD_INVOCATION) {
					MethodInvocation methodInvocation = (MethodInvocation) variableDeclarationFragment.getInitializer();
					if (methodInvocation.getExpression() != null && GET_EJB_REF.equals(methodInvocation.getName().getIdentifier())) {
						AST ast = block.getAST();
						MethodInvocation newMethodInvocation = ast.newMethodInvocation();
						newMethodInvocation.setName(ast.newSimpleName(REMOVE));
						Expression methodExpression = methodInvocation.getExpression();
						methodExpression.accept(portVisitor);
						methodExpression.delete();
						newMethodInvocation.setExpression(methodExpression);
						ExpressionStatement newExpressionStatement = ast.newExpressionStatement(newMethodInvocation);
						statements.clear();
						statements.add(newExpressionStatement);
						visitChildren = false;
					}
				}
			}
		}
		if (visitChildren) {
			for (int i = 0; i < statements.size() - 1; i++) {
				Statement statement = statements.get(i);
				Statement nextStatement = statements.get(i + 1);
				if (statement.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT && nextStatement.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
					ExpressionStatement expressionStatement = (ExpressionStatement) nextStatement;
					ITypeBinding typeBinding = variableDeclarationStatement.getType().resolveBinding();
					if (EJB_OBJECT.equals(typeBinding.getQualifiedName()) && expressionStatement.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION) {
						IMethodBinding methodBinding = ((MethodInvocation) expressionStatement.getExpression()).resolveMethodBinding();
						@SuppressWarnings("unchecked")
						List<VariableDeclarationFragment> variableDeclarationFragments = variableDeclarationStatement.fragments();
						if (variableDeclarationFragments.size() == 1 && methodBinding != null && REMOVE.equals(methodBinding.getName()) && EJB_OBJECT.equals(methodBinding.getDeclaringClass().getQualifiedName())) {
							VariableDeclarationFragment variableDeclarationFragment = variableDeclarationFragments.get(0);
							if (variableDeclarationFragment.getInitializer() != null && variableDeclarationFragment.getInitializer().getNodeType() == ASTNode.METHOD_INVOCATION) {
								MethodInvocation methodInvocation = (MethodInvocation) variableDeclarationFragment.getInitializer();
								if (methodInvocation.getExpression() != null && GET_EJB_REF.equals(methodInvocation.getName().getIdentifier())) {
									AST ast = block.getAST();
									MethodInvocation newMethodInvocation = ast.newMethodInvocation();
									newMethodInvocation.setName(ast.newSimpleName(REMOVE));
									Expression methodExpression = methodInvocation.getExpression();
									methodExpression.accept(portVisitor);
									methodExpression.delete();
									newMethodInvocation.setExpression(methodExpression);
									ExpressionStatement newExpressionStatement = ast.newExpressionStatement(newMethodInvocation);
									portVisitor.replaceASTNode(variableDeclarationStatement, newExpressionStatement);
									portVisitor.markAsPorted(newExpressionStatement);
									nextStatement.delete();
								}
							}
						}
					}
				}
			}
		}
		return visitChildren;
	}
	
	public static boolean isAccessBeanNarrowCastExpression(ApplicationInfo applicationInfo, CastExpression castExpression) {
		boolean result = false;
		if (castExpression.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION) {
			MethodInvocation methodInvocation = (MethodInvocation) castExpression.getExpression();
			if (NARROW.equals(methodInvocation.getName().getIdentifier())) {
				ITypeBinding typeBinding = (ITypeBinding) castExpression.resolveTypeBinding();
				if (typeBinding != null) {
					String qualifiedTypeName = typeBinding.getQualifiedName();
					if (applicationInfo.isAccessBeanType(qualifiedTypeName)) {
						result = true;
					}
				}
			}
		}
		return result;
	}
	
	public static boolean portAccessBeanNarrowCastExpression(ApplicationInfo applicationInfo, CastExpression castExpression, PortVisitor portVisitor) {
		boolean visitChildren = false;
		AST ast = castExpression.getAST();
		ITypeBinding typeBinding = (ITypeBinding) castExpression.resolveTypeBinding();
		String qualifiedTypeName = typeBinding.getQualifiedName();
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(qualifiedTypeName);
		MethodInvocation methodInvocation = (MethodInvocation) castExpression.getExpression();
		MethodInvocation newMethodInvocation = ast.newMethodInvocation();
		newMethodInvocation.setName(ast.newSimpleName(GET_ENTITY));
		Expression accessBeanExpression = methodInvocation.getExpression();
		accessBeanExpression.accept(portVisitor);
		accessBeanExpression.delete();
		newMethodInvocation.setExpression(accessBeanExpression);
		CastExpression newCastExpression = ast.newCastExpression();
		newCastExpression.setType(ast.newSimpleType(ast.newName(entityInfo.getEntityClassInfo().getQualifiedClassName())));
		newCastExpression.setExpression(newMethodInvocation);
		ClassInstanceCreation newClassInstanceCreation = ast.newClassInstanceCreation();
		newClassInstanceCreation.setType(ast.newSimpleType(ast.newName(entityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName())));
		@SuppressWarnings("unchecked")
		List<Expression> arguments = newClassInstanceCreation.arguments();
		arguments.add(newCastExpression);
		portVisitor.replaceASTNode(castExpression, newClassInstanceCreation);
		return visitChildren;
	}
	
	public static boolean isAccessBeanEjbObjectClassInstanceCreation(ApplicationInfo applicationInfo, ClassInstanceCreation classInstanceCreation) {
		boolean result = false;
		ITypeBinding typeBinding = classInstanceCreation.resolveTypeBinding();
		if (typeBinding != null && applicationInfo.isAccessBeanType(typeBinding.getQualifiedName())) {
			EntityInfo entityInfo = applicationInfo.getEntityInfoForType(typeBinding.getQualifiedName());
			if (entityInfo.getSupertype() != null) {
				@SuppressWarnings("unchecked")
				List<Expression> arguments = classInstanceCreation.arguments();
				if (arguments.size() == 1) {
					typeBinding = arguments.get(0).resolveTypeBinding();
					if (typeBinding != null && EJB_OBJECT.equals(typeBinding.getQualifiedName())) {
						result = true;
					}
				}
			}
		}
		return result;
	}
	
	public static boolean portAccessBeanEjbObjectClassInstanceCreation(ApplicationInfo applicationInfo, ClassInstanceCreation classInstanceCreation, PortVisitor portVisitor) {
		boolean visitChildren = false;
		AST ast = classInstanceCreation.getAST();
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(classInstanceCreation.resolveTypeBinding().getQualifiedName());
		@SuppressWarnings("unchecked")
		List<Expression> arguments = classInstanceCreation.arguments();
		Expression ejbObjectExpression = arguments.get(0);
		ejbObjectExpression.accept(portVisitor);
		ejbObjectExpression.delete();
		CastExpression newCastExpression = ast.newCastExpression();
		newCastExpression.setType(ast.newSimpleType(ast.newName(entityInfo.getEntityClassInfo().getQualifiedClassName())));
		newCastExpression.setExpression(ejbObjectExpression);
		arguments.add(newCastExpression);
		classInstanceCreation.getType().accept(portVisitor);
		return visitChildren;
	}
}
