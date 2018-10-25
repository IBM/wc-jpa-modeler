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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.util.AbstractEntityDataUtil;
import com.ibm.commerce.jpa.port.util.AccessBeanUtil;
import com.ibm.commerce.jpa.port.util.DataClassUtil;
import com.ibm.commerce.jpa.port.util.EntityUtil;
import com.ibm.commerce.jpa.port.util.FinderResultCacheUtil;
import com.ibm.commerce.jpa.port.util.HomeInterfaceUtil;
import com.ibm.commerce.jpa.port.util.JavaUtil;
import com.ibm.commerce.jpa.port.util.PrimaryKeyUtil;
import com.ibm.commerce.jpa.port.util.RefreshOnceUtil;
import com.ibm.commerce.jpa.port.util.TargetExceptionUtil;

public class PortVisitor extends ASTVisitor {
	private static final String PROP_PORTED = "ported";
	private static final String ABSTRACT_ENTITY_ACCESS_BEAN = "com.ibm.ivj.ejb.runtime.AbstractEntityAccessBean";
	private static final String COMMIT_COPY_HELPER = "commitCopyHelper";
	private static final String REFRESH_COPY_HELPER = "refreshCopyHelper";
	private static final String INSTANTIATE_ENTITY = "instantiateEntity";
	private static final String SET_INIT_KEY = "setInitKey_";
	private static final String GET_EJB_REF = "getEJBRef";
	private static final String RESET_EJB_REF = "resetEJBRef";
	private static final String DETACH = "detach";
	private static final String REMOVE = "remove";
	private static final String EJB_OBJECT = "javax.ejb.EJBObject";
	private static final String GET_KEY = "__getKey";
	private static final Map<String, PrimitiveType.Code> PRIMITIVE_TYPES;
	static {
		PRIMITIVE_TYPES = new HashMap<String, PrimitiveType.Code>();
		PRIMITIVE_TYPES.put("byte", PrimitiveType.BYTE);
		PRIMITIVE_TYPES.put("short", PrimitiveType.SHORT);
		PRIMITIVE_TYPES.put("char", PrimitiveType.CHAR);
		PRIMITIVE_TYPES.put("int", PrimitiveType.INT);
		PRIMITIVE_TYPES.put("long", PrimitiveType.LONG);
		PRIMITIVE_TYPES.put("float", PrimitiveType.FLOAT);
		PRIMITIVE_TYPES.put("double", PrimitiveType.DOUBLE);
	}
	private ApplicationInfo iApplicationInfo;
	private IJavaProject iJavaProject;
	private boolean iEntityReferencingType = false;
	
	public PortVisitor(ApplicationInfo applicationInfo, IJavaProject javaProject) {
		iApplicationInfo = applicationInfo;
		iJavaProject = javaProject;
	}
	
	public PortVisitor(ApplicationInfo applicationInfo, IJavaProject javaProject, boolean entityReferencingType) {
		iApplicationInfo = applicationInfo;
		iJavaProject = javaProject;
		iEntityReferencingType = entityReferencingType;
	}
	
	public boolean preVisit2(ASTNode astNode) {
		boolean callVisit = true;
		if (astNode instanceof Statement) {
			callVisit = false;
			if (astNode.getProperty(PROP_PORTED) == null) {
				callVisit = true;
				markAsPorted(astNode);
			}
		}
		if (callVisit) {
			callVisit = super.preVisit2(astNode);
		}
		return callVisit;
	}
	
	public void markAsPorted(ASTNode astNode) {
		astNode.setProperty(PROP_PORTED, Boolean.TRUE);
	}
	
	public boolean visit(SimpleType simpleType) {
		ITypeBinding typeBinding = simpleType.resolveBinding();
		if (typeBinding != null) {
			String qualifiedTypeName = typeBinding.getQualifiedName();
			String newTypeName = getTypeMapping(qualifiedTypeName);
			if (newTypeName != null) {
				if (PRIMITIVE_TYPES.get(newTypeName) != null) {
					PrimitiveType newPrimitiveType = simpleType.getAST().newPrimitiveType(PRIMITIVE_TYPES.get(newTypeName));
					replaceASTNode(simpleType, newPrimitiveType);
				}
				else {
					SimpleType newSimpleType = simpleType.getAST().newSimpleType(simpleType.getAST().newName(newTypeName));
					replaceASTNode(simpleType, newSimpleType);
				}
			}
		}
		return false;
	}
	
	public boolean visit(ParameterizedType parameterizedType) {
		ITypeBinding typeBinding = parameterizedType.resolveBinding();
		if (typeBinding != null) {
			String qualifiedTypeName = typeBinding.getTypeDeclaration().getQualifiedName();
			String newTypeName = getTypeMapping(qualifiedTypeName);
			if (newTypeName != null) {
				AST ast = parameterizedType.getAST();
				parameterizedType.setType(ast.newSimpleType(ast.newName(newTypeName)));
			}
		}
		return true;
	}
	
	public boolean visit(SimpleName simpleName) {
		if (!simpleName.isDeclaration()) {
			IBinding binding = simpleName.resolveBinding();
			if (binding != null) {
				if (binding.getKind() == ITypeBinding.TYPE) {
					ITypeBinding typeBinding = (ITypeBinding) binding;
					String qualifiedTypeName = typeBinding.getQualifiedName();
					String newTypeName = getTypeMapping(qualifiedTypeName);
					if (newTypeName != null) {
						replaceASTNode(simpleName, simpleName.getAST().newName(newTypeName));
					}
				}
			}
		}
		return false;
	}
	
	public boolean visit(QualifiedName qualifiedName) {
		boolean visitChildren = true;
		IBinding binding = qualifiedName.resolveBinding();
		if (binding != null) {
			if (binding.getKind() == ITypeBinding.TYPE) {
				ITypeBinding typeBinding = (ITypeBinding) binding;
				String qualifiedTypeName = typeBinding.getQualifiedName();
				String newTypeName = getTypeMapping(qualifiedTypeName);
				if (newTypeName != null) {
					replaceASTNode(qualifiedName, qualifiedName.getAST().newName(newTypeName));
					visitChildren = false;
				}
			}
		}
		if (visitChildren) {
			if (PrimaryKeyUtil.isPrimaryKeyQualifiedName(iApplicationInfo, qualifiedName)) {
				visitChildren = PrimaryKeyUtil.portPrimaryQualifiedName(iApplicationInfo, qualifiedName, this);
			}
		}
		if (visitChildren) {
			qualifiedName.getQualifier().accept(this);
		}
		return false;
	}
	
	public boolean visit(FieldAccess fieldAccess) {
		boolean visitChildren = true;
		if (PrimaryKeyUtil.isPrimaryKeyFieldAccess(iApplicationInfo, fieldAccess)) {
			visitChildren = PrimaryKeyUtil.portPrimaryKeyFieldAccess(iApplicationInfo, fieldAccess, this);
		}
		return visitChildren;
	}
	
	public boolean visit(MethodInvocation methodInvocation) {
		boolean visitChildren = true;
		if (HomeInterfaceUtil.isGetHomeInterfaceMethodInvocation(iApplicationInfo, methodInvocation)) {
			visitChildren = HomeInterfaceUtil.portGetHomeInterfaceMethodInvocation(iApplicationInfo, methodInvocation, this);
		}
		if (PrimaryKeyUtil.isPrimaryKeyMethodInvocation(iApplicationInfo, methodInvocation, this)) {
			PrimaryKeyUtil.portPrimaryKeyMethodInvocation(iApplicationInfo, methodInvocation, this);
		}
		if (DataClassUtil.isGetPrimaryKeyMethodInvocation(iApplicationInfo, methodInvocation)) {
			DataClassUtil.portGetPrimaryKeyMethodInvocation(iApplicationInfo, methodInvocation, this);
		}
		if (visitChildren) {
			IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
			String methodName = methodInvocation.getName().getIdentifier();
			if (methodInvocation.getExpression() != null && methodInvocation.getExpression().getNodeType() != ASTNode.THIS_EXPRESSION) {
				if (AccessBeanUtil.isAccessBeanType(iApplicationInfo, methodInvocation.getExpression()) || AccessBeanUtil.isAccessBeanSubclass(iApplicationInfo, methodInvocation.getExpression())) {
					AccessBeanUtil.updateAccessBeanMethodName(iApplicationInfo, this, methodInvocation);
				}
				if (FinderResultCacheUtil.isFinderResultCacheUtil(methodBinding != null ? methodBinding.getDeclaringClass() : null)) {
					if (FinderResultCacheUtil.isNewAccessBeanMethod(methodName)) {
						ITypeBinding methodInvocationTypeBinding = methodInvocation.resolveTypeBinding();
						String accessBeanType = methodInvocationTypeBinding.getQualifiedName();
						if (iApplicationInfo.isAccessBeanType(accessBeanType)) {
							ClassInstanceCreation classInstanceCreation = methodInvocation.getAST().newClassInstanceCreation();
							classInstanceCreation.setType(methodInvocation.getAST().newSimpleType(methodInvocation.getAST().newName(getTypeMapping(accessBeanType))));
							replaceASTNode(methodInvocation, classInstanceCreation);
							visitChildren = false;
						}
					}
					else if (FinderResultCacheUtil.isFindAsCollectionMethod(methodName)) {
						visitChildren = FinderResultCacheUtil.replaceFindAsCollectionMethodInvocation(iApplicationInfo, methodInvocation, this);
					}
					else if (FinderResultCacheUtil.isFindUsingJDBCMethod(methodName)) {
						visitChildren = FinderResultCacheUtil.replaceFindUsingJDBCMethodInvocation(iApplicationInfo, methodInvocation, this);
					}
					else if (FinderResultCacheUtil.isGetAsCollectionMethod(methodName)) {
						visitChildren = FinderResultCacheUtil.replaceGetAsCollectionMethodInvocation(iApplicationInfo, methodInvocation, this);
					}
					else if (FinderResultCacheUtil.isFinderMethod(methodName)) {
						visitChildren = FinderResultCacheUtil.replaceFinderMethodInvocation(iApplicationInfo, methodInvocation, this);
					}
					else if (FinderResultCacheUtil.isUserMethod(iApplicationInfo, methodInvocation)) {
						visitChildren = FinderResultCacheUtil.replaceUserMethodInvocation(iApplicationInfo, methodInvocation, this);
					}
					else if (FinderResultCacheUtil.isAccessBeanConversionMethodInvocation(iApplicationInfo, methodInvocation)) {
						visitChildren = FinderResultCacheUtil.portAccessBeanConversionMethodInvocation(iApplicationInfo, methodInvocation, this);
					}
					else if (FinderResultCacheUtil.isGetCachedAccessBeanMethodInvocation(iApplicationInfo, methodInvocation)) {
						visitChildren = FinderResultCacheUtil.portGetCachedAccessBeanMethodInvocation(iApplicationInfo, methodInvocation, this);
					}
				}
				if (EntityUtil.isEntityMethodInvocation(iApplicationInfo, methodInvocation)) {
					visitChildren = EntityUtil.portEntityMethodInvocation(iApplicationInfo, methodInvocation, this);
				}
				else if (HomeInterfaceUtil.isHomeInterfaceCreateMethodInvocation(iApplicationInfo, methodInvocation)) {
					visitChildren = HomeInterfaceUtil.portHomeInterfaceCreateMethodInvocation(iApplicationInfo, methodInvocation, this);
				}
				else if (AbstractEntityDataUtil.isGetAbstractEntityDataMethodInvocation(iApplicationInfo, methodInvocation)) {
					visitChildren = AbstractEntityDataUtil.portGetAbstractEntityDataMethodInvocation(iApplicationInfo, methodInvocation, this);
				}
				else if (AbstractEntityDataUtil.isSetAbstractEntityDataMethodInvocation(iApplicationInfo, methodInvocation)) {
					visitChildren = AbstractEntityDataUtil.portSetAbstractEntityDataMethodInvocation(iApplicationInfo, methodInvocation, this);
				}
				else if (FinderResultCacheUtil.isGetClassNameMethodInvocation(methodInvocation)) {
					visitChildren = FinderResultCacheUtil.portGetClassNameMethodInvocation(methodInvocation, this);
				}
				else if (RefreshOnceUtil.isRefreshOnceAccessBeanHelper(methodBinding != null ? methodBinding.getDeclaringClass() : null)) {
					visitChildren = RefreshOnceUtil.portRefreshOnceAccessBeanHelperMethodInvocation(iApplicationInfo, methodInvocation, this);
				}
			}
		}
		return visitChildren;
	}
	
	public boolean visit(Assignment assignment) {
		boolean visitChildren = true;
		if (PrimaryKeyUtil.isPrimaryKeyAssignment(iApplicationInfo, assignment)) {
			visitChildren = PrimaryKeyUtil.portPrimaryKeyAssignment(iApplicationInfo, assignment, this);
		}
		return visitChildren;
	}
	
	public boolean visit(ExpressionStatement expressionStatement) {
		boolean visitChildren = true;
		Expression statementExpression = expressionStatement.getExpression();
		if (FinderResultCacheUtil.isFindByPrimaryKeyStatement(iApplicationInfo, expressionStatement)) {
			visitChildren = FinderResultCacheUtil.portFindByPrimaryKeyStatement(iApplicationInfo, expressionStatement, this);
		}
		else if (FinderResultCacheUtil.isConditionalFindByPrimaryKeyStatement(iApplicationInfo, expressionStatement)) {
			visitChildren = FinderResultCacheUtil.portConditionalFindByPrimaryKeyStatement(iApplicationInfo, expressionStatement, this);
		}
		else if (statementExpression.getNodeType() == ASTNode.ASSIGNMENT) {
			Assignment assignment = (Assignment) statementExpression;
			if (FinderResultCacheUtil.isGetCachedAccessBeanAssignment(iApplicationInfo, assignment)) {
				expressionStatement.delete();
				visitChildren = false;
			}
		}
		else if (AbstractEntityDataUtil.isGetAbstractEntityDataExpressionStatement(iApplicationInfo, expressionStatement) ||
				AbstractEntityDataUtil.isSetAbstractEntityDataExpressionStatement(iApplicationInfo, expressionStatement)) {
			expressionStatement.delete();
			visitChildren = false;
		}
		else if (statementExpression.getNodeType() == ASTNode.METHOD_INVOCATION) {
			MethodInvocation methodInvocation = (MethodInvocation) statementExpression;
			String methodName = methodInvocation.getName().getIdentifier();
			int argumentCount = methodInvocation.arguments().size();
			if (methodInvocation.getExpression() != null) {
				ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
				if (typeBinding != null) {
					String qualifiedTypeName = typeBinding.getQualifiedName();
					if (ABSTRACT_ENTITY_ACCESS_BEAN.equals(qualifiedTypeName) || iApplicationInfo.isAccessBeanType(qualifiedTypeName) || iApplicationInfo.isAccessBeanSubclass(qualifiedTypeName)) {
						if (COMMIT_COPY_HELPER.equals(methodName) || (REFRESH_COPY_HELPER.equals(methodName) && argumentCount == 1) || (qualifiedTypeName.equals("com.ibm.commerce.collaboration.cc.objects.QueueAccessBean") && methodName.equals("setInit_Store_id"))) {
							expressionStatement.delete();
							visitChildren = false;
						}
						else if (REFRESH_COPY_HELPER.equals(methodName) || GET_EJB_REF.equals(methodName)) {
							methodInvocation.setName(methodInvocation.getAST().newSimpleName(INSTANTIATE_ENTITY));
							visitChildren = false;
						}
						else if (RESET_EJB_REF.equals(methodName)) {
							methodInvocation.setName(methodInvocation.getAST().newSimpleName(DETACH));
							visitChildren = false;
						}
						else if (methodName.startsWith(SET_INIT_KEY) && argumentCount == 1) {
							Expression expression = (Expression) methodInvocation.arguments().get(0);
							GetKeyReferenceVisitor getKeyReferenceVisitor = new GetKeyReferenceVisitor(methodInvocation.getExpression());
							expression.accept(getKeyReferenceVisitor);
							if (getKeyReferenceVisitor.getReferencesGetKey()) {
								expressionStatement.delete();
								visitChildren = false;
							}
						}
					}
					else if (EJB_OBJECT.equals(qualifiedTypeName) && methodInvocation.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION && methodName.equals(REMOVE)) {
						MethodInvocation ejbObjectMethodInvocation = (MethodInvocation) methodInvocation.getExpression();
						if (ejbObjectMethodInvocation.getExpression() != null) {
							typeBinding = ejbObjectMethodInvocation.getExpression().resolveTypeBinding();
							if (typeBinding != null) {
								qualifiedTypeName = typeBinding.getQualifiedName();
								if (AccessBeanUtil.isAccessBeanType(iApplicationInfo, qualifiedTypeName)) {
									String ejbObjectMethodName = ejbObjectMethodInvocation.getName().getIdentifier();
									if (GET_EJB_REF.equals(ejbObjectMethodName)) {
										Expression accessBeanExpression = ejbObjectMethodInvocation.getExpression();
										accessBeanExpression.delete();
										accessBeanExpression.accept(this);
										methodInvocation.setExpression(accessBeanExpression);
										visitChildren = false;
									}
								}
							}
						}
					}
				}
			}
		}
		return visitChildren;
	}
	
	public boolean visit(VariableDeclarationStatement variableDeclarationStatement) {
		boolean visitChildren = true;
		if (FinderResultCacheUtil.isFindByPrimaryKeyStatement(iApplicationInfo, variableDeclarationStatement)) {
			visitChildren = FinderResultCacheUtil.portFindByPrimaryKeyStatement(iApplicationInfo, variableDeclarationStatement, this);
		}
		else if (EntityUtil.isEjbObjectVariableDeclarationStatement(variableDeclarationStatement)) {
			visitChildren = EntityUtil.portEjbObjectVariableDeclarationStatement(variableDeclarationStatement, this);
		}
		else if (HomeInterfaceUtil.isHomeInterfaceVariableDeclarationStatement(iApplicationInfo, variableDeclarationStatement)) {
			visitChildren = HomeInterfaceUtil.portHomeInterfaceVariableDeclarationStatement(iApplicationInfo, variableDeclarationStatement, this);
		}
		else {
			visitChildren = TargetExceptionUtil.portVariableDeclarationStatement(variableDeclarationStatement, this);
		}
		return visitChildren;
	}
	
	public boolean visit(ThrowStatement throwStatement) {
		TargetExceptionUtil.portThrowStatement(throwStatement, iEntityReferencingType);
		return true;
	}
	
	public boolean visit(ClassInstanceCreation classInstanceCreation) {
		boolean visitChildren = true;
		if (PrimaryKeyUtil.isPrimaryKeyClassInstanceCreation(iApplicationInfo, classInstanceCreation)) {
			visitChildren = PrimaryKeyUtil.portPrimaryKeyClassInstanceCreation(iApplicationInfo, classInstanceCreation, this);
		}
		else if (AccessBeanUtil.isAccessBeanEjbObjectClassInstanceCreation(iApplicationInfo, classInstanceCreation)) {
			visitChildren = AccessBeanUtil.portAccessBeanEjbObjectClassInstanceCreation(iApplicationInfo, classInstanceCreation, this);
		}
		if (visitChildren && classInstanceCreation.getExpression() != null) {
			visitChildren = false;
			classInstanceCreation.getExpression().accept(this);
			@SuppressWarnings("unchecked")
			List<Expression> arguments = classInstanceCreation.arguments();
			if (arguments != null) {
				for (Expression argument : arguments) {
					argument.accept(this);
				}
			}
		}
		return visitChildren;
	}
	
	public boolean visit(CastExpression castExpression) {
		boolean visitChildren = true;
//		if (AccessBeanUtil.isGetEntityCastExpression(iApplicationInfo, castExpression)) {
//			visitChildren = AccessBeanUtil.portGetEntityCastExpression(iApplicationInfo, castExpression, this);
//		}
//		else
		if (AccessBeanUtil.isAccessBeanNarrowCastExpression(iApplicationInfo, castExpression)) {
			visitChildren = AccessBeanUtil.portAccessBeanNarrowCastExpression(iApplicationInfo, castExpression, this);
		}
		else if (DataClassUtil.isGetPrimaryKeyCastExpression(iApplicationInfo, castExpression)) {
			visitChildren = DataClassUtil.portGetPrimaryKeyCastExpression(iApplicationInfo, castExpression, this);
		}
		else if (PrimaryKeyUtil.isGetPrimaryKeyCastExpression(iApplicationInfo, castExpression)) {
			visitChildren = PrimaryKeyUtil.portGetPrimaryKeyCastExpression(iApplicationInfo, castExpression, this);
		}
		return visitChildren;
	}
	
	public boolean visit(Block block) {
		boolean visitChildren = true;
		if (AccessBeanUtil.isEjbObjectRemoveBlock(block)) {
			visitChildren = AccessBeanUtil.portEjbObjectRemoveBlock(block, this);
		}
		return visitChildren;
	}
	
	public boolean visit(MethodDeclaration methodDeclaration) {
		boolean visitChildren = true;
		if (HomeInterfaceUtil.isGetHomeInterfaceMethodDeclaration(iApplicationInfo, methodDeclaration)) {
			methodDeclaration.delete();
			visitChildren = false;
		}
		return visitChildren;
	}
	
	public boolean visit(FieldDeclaration fieldDeclaration) {
		boolean visitChildren = true;
		if (HomeInterfaceUtil.isHomeInterfaceJndiNameField(iApplicationInfo, fieldDeclaration)) {
			fieldDeclaration.delete();
			visitChildren = false;
		}
		return visitChildren;
	}
	
	public void endVisit(TryStatement tryStatement) {
		TargetExceptionUtil.portTryStatement(iJavaProject, tryStatement, this, iEntityReferencingType);
	}
	
	public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
		JavaUtil.replaceASTNode(astNode, newNode);
	}
	
	public void replaceStatement(Statement statement, List<Statement> newStatementList) {
		JavaUtil.replaceStatement(statement, newStatementList);
	}
	
	public String getTypeMapping(String typeName) {
		return iApplicationInfo.getTypeMapping(typeName);
	}
	
	private static class GetKeyReferenceVisitor extends ASTVisitor {
		private Expression iAccessBeanExpression;
		private boolean iReferencesGetKey = false;
		
		public GetKeyReferenceVisitor(Expression accessBeanExpression) {
			iAccessBeanExpression = accessBeanExpression;
		}

		public boolean visit(MethodInvocation methodInvocation) {
			if (methodInvocation.getExpression() != null && GET_KEY.equals(methodInvocation.getName().getIdentifier())) {
				if (methodInvocation.getExpression().toString().equals(iAccessBeanExpression.toString())) {
					iReferencesGetKey = true;
				}
			}
			return true;
		}
		
		public boolean getReferencesGetKey() {
			return iReferencesGetKey;
		}
	}
}
