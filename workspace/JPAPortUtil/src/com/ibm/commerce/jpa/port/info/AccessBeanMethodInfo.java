package com.ibm.commerce.jpa.port.info;

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

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;

import com.ibm.commerce.jpa.port.generators.PortVisitor;
import com.ibm.commerce.jpa.port.util.JavaUtil;
import com.ibm.commerce.jpa.port.util.TargetExceptionUtil;

public class AccessBeanMethodInfo {
	private static final String EJB_CREATE = "ejbCreate+";
	private static final String COMMIT_COPY_HELPER = "commitCopyHelper";
	private static final String CLOB_ATTRIBUTE_VALUE = "com.ibm.commerce.base.helpers.ClobAttributeValue";
	private static final String BLOB_ATTRIBUTE_VALUE = "com.ibm.commerce.base.helpers.BlobAttributeValue";
	private static final String GET_VALUE = "getValue";
	private static final String SET_VALUE = "setValue";
	private static final String ENTITY_CREATION_DATA = "com.ibm.commerce.context.content.objects.EntityCreationData";
	private static final String COPY_FIELDS = "copyFields";
	private static final String CHAR = "CHAR";
	private static final String TRIM = "trim";
	private EntityInfo iEntityInfo;
	private String iMethodKey;
	private String iMethodName;
	private String iTargetMethodName;
	private String iReturnType;
	private String[] iParameterTypes;
	private String[] iParameterNames;
	private String[] iTargetParameterNames;
	private List<AccessBeanStatement> iStatements = new ArrayList<AccessBeanStatement>();
	private List<Set<FieldInfo>> iFieldsInitializedByParameter;
	private Set<FieldInfo> iInitializedFields = new HashSet<FieldInfo>();
	private Collection<String> iReferencedAccessBeanMethods = new HashSet<String>();
	private Collection<String> iReferencedStaticFieldNames = new HashSet<String>();
	private Collection<String> iReferencedInstanceVariableNames = new HashSet<String>();
	private AccessBeanMethodInfo iSuperAccessBeanMethodInfo;
	private boolean iCallsSuperAccessBeanMethod;
	private boolean iInvalid;
	private VariableRenameVisitor iVariableRenameVisitor;
	private String iModifiedInstanceVariableName;
	
	public AccessBeanMethodInfo(EntityInfo entityInfo, String methodKey, String methodName, String[] parameterTypes, String returnType) {
		iEntityInfo = entityInfo;
		iMethodKey = methodKey;
		iMethodName = methodName;
		iTargetMethodName = methodName;
		iParameterTypes = parameterTypes;
		iParameterNames = new String[parameterTypes.length];
		iTargetParameterNames = new String[parameterTypes.length];
		iReturnType = returnType;
	}
	
	public EntityInfo getEntityInfo() {
		return iEntityInfo;
	}
	
	public String getMethodKey() {
		return iMethodKey;
	}
	
	public String getMethodName() {
		return iMethodName;
	}
	
	public void setTargetMethodName(String targetMethodName) {
		iTargetMethodName = targetMethodName;
	}
	
	public String getTargetMethodName() {
		return iTargetMethodName;
	}
	
	public String[] getParameterTypes() {
		return iParameterTypes;
	}
	
	public String getReturnType() {
		return iReturnType;
	}
	
	public List<AccessBeanStatement> getStatements() {
		return iStatements;
	}
	
	public void setParameterName(int parameterIndex, String parameterName) {
		iParameterNames[parameterIndex] = parameterName;
	}
	
	public String[] getParameterNames() {
		return iParameterNames;
	}
	
	public void setTargetParameterName(int parameterIndex, String targetParameterName) {
		iTargetParameterNames[parameterIndex] = targetParameterName;
	}
	
	public String getTargetParameterName(int parameterIndex) {
		String targetParameterName = iTargetParameterNames[parameterIndex];
		if (iCallsSuperAccessBeanMethod && iSuperAccessBeanMethodInfo != null) {
			targetParameterName = iSuperAccessBeanMethodInfo.getTargetParameterName(parameterIndex);
		}
		if (targetParameterName == null) {
			targetParameterName = iParameterNames[parameterIndex];
		}
		return targetParameterName;
	}
	
	public void addFieldInitializedByParameter(int parameterIndex, FieldInfo fieldInfo) {
		if (iFieldsInitializedByParameter == null) {
			iFieldsInitializedByParameter = new ArrayList<Set<FieldInfo>>(iParameterTypes.length);
			for (int i = 0; i < iParameterTypes.length; i++) {
				iFieldsInitializedByParameter.add(null);
			}
		}
		if (iFieldsInitializedByParameter.get(parameterIndex) == null) {
			iFieldsInitializedByParameter.set(parameterIndex, new HashSet<FieldInfo>());
		}
		iFieldsInitializedByParameter.get(parameterIndex).add(fieldInfo);
	}
	
	public Set<FieldInfo> getFieldsInitializedByParameter(int parameterIndex) {
		Set<FieldInfo> fieldsInitializedByParameter = null;
		if (iFieldsInitializedByParameter != null) {
			fieldsInitializedByParameter = iFieldsInitializedByParameter.get(parameterIndex);
		}
		return fieldsInitializedByParameter;
	}
	
	public void addInitializedField(FieldInfo fieldInfo) {
		iInitializedFields.add(fieldInfo);
	}
	
	public Set<FieldInfo> getInitializedFields() {
		if (iCallsSuperAccessBeanMethod && iSuperAccessBeanMethodInfo != null) {
			iInitializedFields.addAll(iSuperAccessBeanMethodInfo.getInitializedFields());
		}
		return iInitializedFields;
	}
	
	public void addReferencedAccessBeanMethod(String methodKey) {
		iReferencedAccessBeanMethods.add(methodKey);
	}
	
	public Collection<String> getReferencedAccessBeanMethods() {
		if (iCallsSuperAccessBeanMethod && iSuperAccessBeanMethodInfo != null) {
			iReferencedAccessBeanMethods.addAll(iSuperAccessBeanMethodInfo.getReferencedAccessBeanMethods());
		}
		return iReferencedAccessBeanMethods;
	}
	
	public TargetExceptionInfo getUnhandledExceptions() {
		TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
		for (AccessBeanStatement statement : iStatements) {
			unhandledExceptions.addAll(statement.getUnhandledExceptions(this));
		}
		return unhandledExceptions;
	}
	
	public Collection<String> getReferencedStaticFieldNames() {
		if (iCallsSuperAccessBeanMethod && iSuperAccessBeanMethodInfo != null) {
			iReferencedStaticFieldNames.addAll(iSuperAccessBeanMethodInfo.getReferencedStaticFieldNames());
		}
		return iReferencedStaticFieldNames;
	}
	
	public Collection<String> getReferencedInstanceVariableNames() {
		if (iCallsSuperAccessBeanMethod && iSuperAccessBeanMethodInfo != null) {
			iReferencedInstanceVariableNames.addAll(iSuperAccessBeanMethodInfo.getReferencedInstanceVariableNames());
		}
		return iReferencedInstanceVariableNames;
	}
	
	public void addSuperAccessBeanMethodInfo(AccessBeanMethodInfo superAccessBeanMethodInfo) {
		if (iSuperAccessBeanMethodInfo == null) {
			iSuperAccessBeanMethodInfo = superAccessBeanMethodInfo;
		}
		else {
			iSuperAccessBeanMethodInfo.addSuperAccessBeanMethodInfo(superAccessBeanMethodInfo);
		}
	}
	
	public AccessBeanMethodInfo getSuperAccessBeanMethodInfo() {
		return iSuperAccessBeanMethodInfo;
	}
	
	public void setCallsSuperAccessBeanMethod(boolean callsSuperAccessBeanMethod) {
		iCallsSuperAccessBeanMethod = callsSuperAccessBeanMethod;
	}
	
	public boolean getCallsSuperAccessBeanMethod() {
		return iCallsSuperAccessBeanMethod;
	}
	
	public boolean isEmptyMethod() {
		boolean emptyMethod = false;
		if (iStatements.size() == 0) {
			emptyMethod = true;
		}
		else if (iStatements.size() == 1 && iCallsSuperAccessBeanMethod) {
			emptyMethod = iSuperAccessBeanMethodInfo == null || iSuperAccessBeanMethodInfo.isEmptyMethod();
		}
		return emptyMethod;
	}
	
	public void markInvalid() {
		iInvalid = true;
	}
	
	public boolean isInvalid() {
		if (!iInvalid && iCallsSuperAccessBeanMethod && iSuperAccessBeanMethodInfo != null) {
			iInvalid = iSuperAccessBeanMethodInfo.isInvalid();
		}
		return iInvalid;
	}
	
	public void renameVariables(ASTNode node) {
		if (iVariableRenameVisitor == null) {
			Map<String, String> variableNameMap = new HashMap<String, String>();
			for (int i = 0; i < iParameterNames.length; i++) {
				String parameterName = iParameterNames[i];
				String targetParameterName = getTargetParameterName(i);
				if (!parameterName.equals(targetParameterName)) {
					variableNameMap.put(parameterName, targetParameterName);
				}
			}
			iVariableRenameVisitor = new VariableRenameVisitor(variableNameMap);
		}
		node.accept(iVariableRenameVisitor);
	}
	
	public void resolve() {
		if (iSuperAccessBeanMethodInfo != null) {
			iSuperAccessBeanMethodInfo.resolve();
		}
		Deque<List<AccessBeanStatement>> statementListStack = new ArrayDeque<List<AccessBeanStatement>>();
		resolveStatements(statementListStack, iStatements);
	}
	
	public void resolveStatements(Deque<List<AccessBeanStatement>> statementListStack, List<AccessBeanStatement> statements) {
		statementListStack.push(statements);
		int i = statements.size() - 1;
		while (i >= 0) {
			statements.get(i).resolve(this, statementListStack);
			i--;
		}
		statementListStack.pop();
	}
	
	public void setModifiedInstanceVariableName(String modifiedInstanceVariableName) {
		iModifiedInstanceVariableName = modifiedInstanceVariableName;
	}
	
	public String getModifiedInstanceVariableName() {
		return iModifiedInstanceVariableName;
	}
	
	public abstract static class AccessBeanStatement {
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			return new TargetExceptionInfo();
		}
		
		public void resolve(AccessBeanMethodInfo accessBeanMethodInfo, Deque<List<AccessBeanStatement>> statementListStack) {
			
		}
		
		public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
			if (astNode.getParent() != null) {
				JavaUtil.replaceASTNode(astNode, newNode);
			}
		}
		
		public boolean usesVariable(String variableName) {
			return false;
		}
		
		public boolean containsStatement(AccessBeanStatement statement) {
			return statement == this;
		}
	}
	
	public static class AccessBeanIfStatement extends AccessBeanStatement {
		private Expression iIfExpression;
		private TargetExceptionInfo iIfExpressionUnhandledExceptions;
		private List<AccessBeanStatement> iThenStatements = new ArrayList<AccessBeanStatement>();
		private List<AccessBeanStatement> iElseStatements = new ArrayList<AccessBeanStatement>();
		
		public AccessBeanIfStatement() {
		}
		
		public void setIfExpression(Expression ifExpression) {
			iIfExpression = ifExpression;
		}
		
		public Expression getIfExpression() {
			return iIfExpression;
		}
		
		public List<AccessBeanStatement> getThenStatements() {
			return iThenStatements;
		}
		
		public List<AccessBeanStatement> getElseStatements() {
			return iElseStatements;
		}
		
		public void resolve(AccessBeanMethodInfo accessBeanMethodInfo, Deque<List<AccessBeanStatement>> statementListStack) {
			List<AccessBeanStatement> statementList = statementListStack.peek();
			accessBeanMethodInfo.resolveStatements(statementListStack, iThenStatements);
			accessBeanMethodInfo.resolveStatements(statementListStack, iElseStatements);
			if (iThenStatements.size() == 0 && iElseStatements.size() == 0) {
				statementList.remove(this);
			}
			else {
				ModuleInfo moduleInfo = accessBeanMethodInfo.getEntityInfo().getModuleInfo();
				iIfExpressionUnhandledExceptions = TargetExceptionUtil.getUnhandledTargetExceptions(moduleInfo.getApplicationInfo(), moduleInfo.getJavaProject(), iIfExpression);
				accessBeanMethodInfo.renameVariables(iIfExpression);
				iIfExpression.accept(new AccessBeanExpressionVisitor(accessBeanMethodInfo, this));
			}
		}
		
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
			unhandledExceptions.addAll(iIfExpressionUnhandledExceptions);
			for (AccessBeanStatement statement : iThenStatements) {
				unhandledExceptions.addAll(statement.getUnhandledExceptions(accessBeanMethodInfo));
			}
			for (AccessBeanStatement statement : iElseStatements) {
				unhandledExceptions.addAll(statement.getUnhandledExceptions(accessBeanMethodInfo));
			}
			return unhandledExceptions;
		}
		
		public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
			super.replaceASTNode(astNode, newNode);
			if (astNode == iIfExpression) {
				iIfExpression = (Expression) newNode;
			}
		}
		
		public boolean usesVariable(String variableName) {
			boolean variableUsed = false;
			VariableCheckVisitor variableCheckVisitor = new VariableCheckVisitor(variableName);
			iIfExpression.accept(variableCheckVisitor);
			if (variableCheckVisitor.getVariableFound()) {
				variableUsed = true;
			}
			else {
				for (AccessBeanStatement statement : iThenStatements) {
					if (statement.usesVariable(variableName)) {
						variableUsed = true;
						break;
					}
				}
				if (!variableUsed) {
					for (AccessBeanStatement statement : iElseStatements) {
						if (statement.usesVariable(variableName)) {
							variableUsed = true;
							break;
						}
					}
				}
			}
			return variableUsed;
		}
		
		public boolean containsStatement(AccessBeanStatement statement) {
			boolean match = statement == this;
			if (!match) {
				for (AccessBeanStatement currentStatement : iThenStatements) {
					if (currentStatement.containsStatement(statement)) {
						match = true;
						break;
					}
				}
				if (!match) {
					for (AccessBeanStatement currentStatement : iElseStatements) {
						if (currentStatement.containsStatement(statement)) {
							match = true;
							break;
						}
					}
				}
			}
			return match;
		}
	}
	
	public static class AccessBeanParameterInitializedFieldStatement extends AccessBeanStatement {
		private FieldInfo iFieldInfo;
		private int iParameterIndex;
		
		public AccessBeanParameterInitializedFieldStatement(FieldInfo fieldInfo, int parameterIndex) {
			iFieldInfo = fieldInfo;
			iParameterIndex = parameterIndex;
		}
		
		public FieldInfo getFieldInfo() {
			return iFieldInfo;
		}
		
		public int getParameterIndex() {
			return iParameterIndex;
		}
	}
	
	public static class AccessBeanExpressionInitializedFieldStatement extends AccessBeanStatement {
		private FieldInfo iFieldInfo;
		private Expression iExpression;
		private TargetExceptionInfo iExpressionUnhandledExceptions;
		
		public AccessBeanExpressionInitializedFieldStatement() {
		}
		
		public void setFieldInfo(FieldInfo fieldInfo) {
			iFieldInfo = fieldInfo;
		}
		
		public FieldInfo getFieldInfo() {
			return iFieldInfo;
		}
		
		public void setExpression(Expression expression) {
			iExpression = expression;
		}
		
		public Expression getExpression() {
			return iExpression;
		}
		
		public void resolve(AccessBeanMethodInfo accessBeanMethodInfo, Deque<List<AccessBeanStatement>> statementListStack) {
			ModuleInfo moduleInfo = accessBeanMethodInfo.getEntityInfo().getModuleInfo();
			iExpressionUnhandledExceptions = TargetExceptionUtil.getUnhandledTargetExceptions(moduleInfo.getApplicationInfo(), moduleInfo.getJavaProject(), iExpression);
			accessBeanMethodInfo.renameVariables(iExpression);
			iExpression.accept(new AccessBeanExpressionVisitor(accessBeanMethodInfo, this));
		}
		
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
			unhandledExceptions.addAll(iExpressionUnhandledExceptions);
			return unhandledExceptions;
		}
		
		public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
			super.replaceASTNode(astNode, newNode);
			if (astNode == iExpression) {
				iExpression = (Expression) newNode;
			}
		}
		
		public boolean usesVariable(String variableName) {
			boolean variableUsed = false;
			VariableCheckVisitor variableCheckVisitor = new VariableCheckVisitor(variableName);
			iExpression.accept(variableCheckVisitor);
			if (variableCheckVisitor.getVariableFound()) {
				variableUsed = true;
			}
			return variableUsed;
		}
	}
	
	public static class AccessBeanKeyParameterInitializedFieldStatement extends AccessBeanStatement {
		private FieldInfo iFieldInfo;
		
		public AccessBeanKeyParameterInitializedFieldStatement() {
		}
		
		public void setFieldInfo(FieldInfo fieldInfo) {
			iFieldInfo = fieldInfo;
		}
		
		public FieldInfo getFieldInfo() {
			return iFieldInfo;
		}
	}
	
	public static class AccessBeanEntityCreationDataInitializedFieldStatement extends AccessBeanStatement {
		private FieldInfo iFieldInfo;
		
		public AccessBeanEntityCreationDataInitializedFieldStatement() {
		}
		
		public void setFieldInfo(FieldInfo fieldInfo) {
			iFieldInfo = fieldInfo;
		}
		
		public FieldInfo getFieldInfo() {
			return iFieldInfo;
		}
	}
	
	public static class AccessBeanVariableDeclarationStatement extends AccessBeanStatement {
		private String iVariableName;
		private String iType;
		private Expression iInitializationExpression;
		private TargetExceptionInfo iInitializationExpressionUnhandledExceptions;
		
		public AccessBeanVariableDeclarationStatement() {
		}
		
		public void setVariableName(String variableName) {
			iVariableName = variableName;
		}
		
		public String getVariableName() {
			return iVariableName;
		}
		
		public void setType(String type) {
			iType = type;
		}
		
		public String getType() {
			return iType;
		}
		
		public void setInitializationExpression(Expression initializationExpression) {
			iInitializationExpression = initializationExpression;
		}
		
		public Expression getInitializationExpression() {
			return iInitializationExpression;
		}
		
		public void resolve(AccessBeanMethodInfo accessBeanMethodInfo, Deque<List<AccessBeanStatement>> statementListStack) {
			boolean variableUsed = false;
			for (List<AccessBeanStatement> statementList : statementListStack) {
				boolean beforeCurrentStatement = true;
				for (AccessBeanStatement statement : statementList) {
					if (beforeCurrentStatement) {
						if (statement.containsStatement(this)) {
							beforeCurrentStatement = false;
						}
					}
					else if (statement.usesVariable(iVariableName)) {
						variableUsed = true;
						break;
					}
				}
			}
			if (!variableUsed) {
				statementListStack.peek().remove(this);
			}
			else if (iInitializationExpression != null) {
//				if (iInitializationExpression.toString().endsWith("ContractJDBCHelperAccessBean().getLargestTCSequenceByTrading(iTypedEntity.getTradingId()) + 1")) {
//					System.out.println("new JPAContractJDBCHelperAccessBean().getLargestTCSequenceByTrading(iTypedEntity.getTradingId()) + 1 hello");
//				}
				ModuleInfo moduleInfo = accessBeanMethodInfo.getEntityInfo().getModuleInfo();
				iInitializationExpressionUnhandledExceptions = TargetExceptionUtil.getUnhandledTargetExceptions(moduleInfo.getApplicationInfo(), moduleInfo.getJavaProject(), iInitializationExpression);
				accessBeanMethodInfo.renameVariables(iInitializationExpression);
				iInitializationExpression.accept(new AccessBeanExpressionVisitor(accessBeanMethodInfo, this));
			}
		}
		
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
			if (iInitializationExpressionUnhandledExceptions != null) {
				unhandledExceptions.addAll(iInitializationExpressionUnhandledExceptions);
			}
			return unhandledExceptions;
		}
		
		public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
			super.replaceASTNode(astNode, newNode);
			if (astNode == iInitializationExpression) {
				iInitializationExpression = (Expression) newNode;
			}
		}
		
		public boolean usesVariable(String variableName) {
			boolean variableUsed = false;
			if (iInitializationExpression != null) {
				VariableCheckVisitor variableCheckVisitor = new VariableCheckVisitor(variableName);
				iInitializationExpression.accept(variableCheckVisitor);
				if (variableCheckVisitor.getVariableFound()) {
					variableUsed = true;
				}
			}
			return variableUsed;
		}
	}
	
	public static class AccessBeanVariableAssignmentStatement extends AccessBeanStatement {
		private String iVariableName;
		private Expression iAssignmentExpression;
		private TargetExceptionInfo iAssignmentExpressionUnhandledExceptions;
		
		public AccessBeanVariableAssignmentStatement() {
		}
		
		public void setVariableName(String variableName) {
			iVariableName = variableName;
		}
		
		public String getVariableName() {
			return iVariableName;
		}
		
		public void setAssignmentExpression(Expression assignmentExpression) {
			iAssignmentExpression = assignmentExpression;
		}
		
		public Expression getAssignmentExpression() {
			return iAssignmentExpression;
		}
		
		public void resolve(AccessBeanMethodInfo accessBeanMethodInfo, Deque<List<AccessBeanStatement>> statementListStack) {
			boolean variableUsed = false;
			for (List<AccessBeanStatement> statementList : statementListStack) {
				boolean beforeCurrentStatement = true;
				for (AccessBeanStatement statement : statementList) {
					if (beforeCurrentStatement && statement.containsStatement(this)) {
						beforeCurrentStatement = false;
					}
					else if (statement.usesVariable(iVariableName)) {
						variableUsed = true;
						break;
					}
				}
			}
			if (!variableUsed) {
				statementListStack.peek().remove(this);
			}
			else {
				ModuleInfo moduleInfo = accessBeanMethodInfo.getEntityInfo().getModuleInfo();
				iAssignmentExpressionUnhandledExceptions = TargetExceptionUtil.getUnhandledTargetExceptions(moduleInfo.getApplicationInfo(), moduleInfo.getJavaProject(), iAssignmentExpression);
				accessBeanMethodInfo.renameVariables(iAssignmentExpression);
				iAssignmentExpression.accept(new AccessBeanExpressionVisitor(accessBeanMethodInfo, this));
			}
		}
		
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
			unhandledExceptions.addAll(iAssignmentExpressionUnhandledExceptions);
			return unhandledExceptions;
		}
		
		public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
			super.replaceASTNode(astNode, newNode);
			if (astNode == iAssignmentExpression) {
				iAssignmentExpression = (Expression) newNode;
			}
		}
		
		public boolean usesVariable(String variableName) {
			boolean variableUsed = false;
			VariableCheckVisitor variableCheckVisitor = new VariableCheckVisitor(variableName);
			iAssignmentExpression.accept(variableCheckVisitor);
			if (variableCheckVisitor.getVariableFound()) {
				variableUsed = true;
			}
			return variableUsed;
		}
	}
	
	public static class AccessBeanVariableMethodInvocationStatement extends AccessBeanStatement {
		private String iVariableName;
		private String iVariableType;
		private String iMethodName;
		private String iMethodKey;
		private List<Expression> iArguments = new ArrayList<Expression>();
		private TargetExceptionInfo iArgumentsUnhandledExceptions = new TargetExceptionInfo();
		
		public AccessBeanVariableMethodInvocationStatement() {
		}
		
		public void setVariableName(String variableName) {
			iVariableName = variableName;
		}
		
		public String getVariableName() {
			return iVariableName;
		}
		
		public void setVariableType(String variableType) {
			iVariableType = variableType;
		}
		
		public String getVariableType() {
			return iVariableType;
		}
		
		public void setMethodName(String methodName) {
			iMethodName = methodName;
		}
		
		public String getMethodName() {
			return iMethodName;
		}
		
		public void setMethodKey(String methodKey) {
			iMethodKey = methodKey;
		}
		
		public String getMethodKey() {
			return iMethodKey;
		}
		
		public void addArgument(Expression argument) {
			iArguments.add(argument);
		}
		
		public List<Expression> getArguments() {
			return iArguments;
		}
		
		public void resolve(AccessBeanMethodInfo accessBeanMethodInfo, Deque<List<AccessBeanStatement>> statementListStack) {
			boolean variableUsed = false;
			ApplicationInfo applicationInfo = accessBeanMethodInfo.getEntityInfo().getModuleInfo().getApplicationInfo();
			boolean isAccessBeanType = applicationInfo.isAccessBeanType(iVariableType);
			if (isAccessBeanType) {
				EntityInfo entityInfo = applicationInfo.getEntityInfoForType(iVariableType);
				if (entityInfo.getUserMethodInfo(iMethodKey) != null) {
					variableUsed = true;
				}
			}
			
			boolean isEntityCreationDataCopyFields = false;
			if (ENTITY_CREATION_DATA.equals(iVariableType) && COPY_FIELDS.equals(iMethodName)) {
				variableUsed = true;
				isEntityCreationDataCopyFields = true;
			}
			if (!variableUsed) {
				for (List<AccessBeanStatement> statementList : statementListStack) {
					boolean beforeCurrentStatement = true;
					for (AccessBeanStatement statement : statementList) {
						if (beforeCurrentStatement && statement.containsStatement(this)) {
							beforeCurrentStatement = false;
						}
						else if (statement.usesVariable(iVariableName)) {
							variableUsed = true;
							break;
						}
					}
				}
			}
			if (isEntityCreationDataCopyFields) {
				List<AccessBeanStatement> statementList = statementListStack.peek();
				int location = statementList.indexOf(this);
				Set<FieldInfo> fields = accessBeanMethodInfo.getEntityInfo().getEntityCreationDataFields();
				for (FieldInfo fieldInfo : fields) {
					AccessBeanEntityCreationDataInitializedFieldStatement statement = new AccessBeanEntityCreationDataInitializedFieldStatement();
					statement.setFieldInfo(fieldInfo);
					statementList.add(location, statement);
				}
				statementList.remove(this);
			}
			else if (!variableUsed || (isAccessBeanType && COMMIT_COPY_HELPER.equals(iMethodName))) {
				statementListStack.peek().remove(this);
			}
			else {
				AccessBeanExpressionVisitor accessBeanExpressionVisitor = new AccessBeanExpressionVisitor(accessBeanMethodInfo, this);
				ModuleInfo moduleInfo = accessBeanMethodInfo.getEntityInfo().getModuleInfo();
				for (Expression argument : iArguments) {
					iArgumentsUnhandledExceptions.addAll(TargetExceptionUtil.getUnhandledTargetExceptions(moduleInfo.getApplicationInfo(), moduleInfo.getJavaProject(), argument));
					accessBeanMethodInfo.renameVariables(argument);
					argument.accept(accessBeanExpressionVisitor);
				}
			}
		}
		
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
			unhandledExceptions.addAll(iArgumentsUnhandledExceptions);
			return unhandledExceptions;
		}
		
		public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
			super.replaceASTNode(astNode, newNode);
			for (int i = 0; i < iArguments.size(); i++) {
				if (iArguments.get(i) == astNode) {
					iArguments.set(i, (Expression) newNode);
					break;
				}
			}
		}
		
		public boolean usesVariable(String variableName) {
			boolean variableUsed = variableName.equals(iVariableName);
			if (!variableUsed) {
				VariableCheckVisitor variableCheckVisitor = new VariableCheckVisitor(variableName);
				for (Expression argument : iArguments) {
					argument.accept(variableCheckVisitor);
					if (variableCheckVisitor.getVariableFound()) {
						variableUsed = true;
						break;
					}
				}
			}
			return variableUsed;
		}
	}
	
	public static class AccessBeanInstanceVariableAssignmentStatement extends AccessBeanStatement {
		private String iInstanceVariableName;
		private Expression iAssignmentExpression;
		private TargetExceptionInfo iAssignmentExpressionUnhandledExceptions;
		
		public AccessBeanInstanceVariableAssignmentStatement() {
		}
		
		public void setInstanceVariableName(String instanceVariableName) {
			iInstanceVariableName = instanceVariableName;
		}
		
		public String getInstanceVariableName() {
			return iInstanceVariableName;
		}
		
		public void setAssignmentExpression(Expression assignmentExpression) {
			iAssignmentExpression = assignmentExpression;
		}
		
		public Expression getAssignmentExpression() {
			return iAssignmentExpression;
		}
		
		public void resolve(AccessBeanMethodInfo accessBeanMethodInfo, Deque<List<AccessBeanStatement>> statementListStack) {
			ModuleInfo moduleInfo = accessBeanMethodInfo.getEntityInfo().getModuleInfo();
			iAssignmentExpressionUnhandledExceptions = TargetExceptionUtil.getUnhandledTargetExceptions(moduleInfo.getApplicationInfo(), moduleInfo.getJavaProject(), iAssignmentExpression);
			accessBeanMethodInfo.renameVariables(iAssignmentExpression);
			iAssignmentExpression.accept(new AccessBeanExpressionVisitor(accessBeanMethodInfo, this));
			accessBeanMethodInfo.setModifiedInstanceVariableName(iInstanceVariableName);
		}
		
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
			unhandledExceptions.addAll(iAssignmentExpressionUnhandledExceptions);
			return unhandledExceptions;
		}
		
		public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
			super.replaceASTNode(astNode, newNode);
			if (astNode == iAssignmentExpression) {
				iAssignmentExpression = (Expression) newNode;
			}
		}
		
		public boolean usesVariable(String variableName) {
			boolean variableUsed = false;
			VariableCheckVisitor variableCheckVisitor = new VariableCheckVisitor(variableName);
			iAssignmentExpression.accept(variableCheckVisitor);
			if (variableCheckVisitor.getVariableFound()) {
				variableUsed = true;
			}
			return variableUsed;
		}
	}
	
	public static class AccessBeanMethodInvocationStatement extends AccessBeanStatement {
		private String iMethodKey;
		private List<Expression> iArguments = new ArrayList<Expression>();
		private TargetExceptionInfo iArgumentsUnhandledExceptions = new TargetExceptionInfo();
		
		public AccessBeanMethodInvocationStatement(String methodKey) {
			iMethodKey = methodKey;
		}
		
		public String getMethodKey() {
			return iMethodKey;
		}
		
		public void addArgument(Expression argument) {
			iArguments.add(argument);
		}
		
		public List<Expression> getArguments() {
			return iArguments;
		}
		
		public void resolve(AccessBeanMethodInfo accessBeanMethodInfo, Deque<List<AccessBeanStatement>> statementListStack) {
			AccessBeanExpressionVisitor accesBeanExpressionVisitor = new AccessBeanExpressionVisitor(accessBeanMethodInfo, this);
			ModuleInfo moduleInfo = accessBeanMethodInfo.getEntityInfo().getModuleInfo();
			for (Expression argument : iArguments) {
				iArgumentsUnhandledExceptions.addAll(TargetExceptionUtil.getUnhandledTargetExceptions(moduleInfo.getApplicationInfo(), moduleInfo.getJavaProject(), argument));
				accessBeanMethodInfo.renameVariables(argument);
				argument.accept(accesBeanExpressionVisitor);
			}
			if (iMethodKey.startsWith(EJB_CREATE)) {
				String[] parameterTypes = iMethodKey.substring(EJB_CREATE.length()).split("\\+");
				CreatorInfo creatorInfo = accessBeanMethodInfo.getEntityInfo().getCreatorInfo(parameterTypes);
				accessBeanMethodInfo.getEntityInfo().addCreatorCalledByCreator(creatorInfo);
			}
			accessBeanMethodInfo.addReferencedAccessBeanMethod(iMethodKey);
		}
		
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
			unhandledExceptions.addAll(iArgumentsUnhandledExceptions);
			return unhandledExceptions;
		}
		
		public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
			super.replaceASTNode(astNode, newNode);
			for (int i = 0; i < iArguments.size(); i++) {
				if (iArguments.get(i) == astNode) {
					iArguments.set(i, (Expression) newNode);
					break;
				}
			}
		}
		
		public boolean usesVariable(String variableName) {
			boolean variableUsed = false;
			VariableCheckVisitor variableCheckVisitor = new VariableCheckVisitor(variableName);
			for (Expression argument : iArguments) {
				argument.accept(variableCheckVisitor);
				if (variableCheckVisitor.getVariableFound()) {
					variableUsed = true;
					break;
				}
			}
			return variableUsed;
		}
	}
	
	public static class AccessBeanSuperMethodInvocationStatement extends AccessBeanStatement {
		private List<Expression> iArguments = new ArrayList<Expression>();
		
		public void addArgument(Expression argument) {
			iArguments.add(argument);
		}
		
		public List<Expression> getArguments() {
			return iArguments;
		}
		
		public void resolve(AccessBeanMethodInfo accessBeanMethodInfo, Deque<List<AccessBeanStatement>> statementListStack) {
			List<AccessBeanStatement> statementList = statementListStack.peek();
			if (accessBeanMethodInfo.getSuperAccessBeanMethodInfo() == null) {
				statementList.remove(this);
				accessBeanMethodInfo.setCallsSuperAccessBeanMethod(false);
			}
		}
		
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
			if (accessBeanMethodInfo.getSuperAccessBeanMethodInfo() != null) {
				unhandledExceptions.addAll(accessBeanMethodInfo.getSuperAccessBeanMethodInfo().getUnhandledExceptions());
			}
			return unhandledExceptions;
		}
		
		public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
			super.replaceASTNode(astNode, newNode);
			for (int i = 0; i < iArguments.size(); i++) {
				if (iArguments.get(i) == astNode) {
					iArguments.set(i, (Expression) newNode);
					break;
				}
			}
		}
		
		public boolean usesVariable(String variableName) {
			boolean variableUsed = false;
			VariableCheckVisitor variableCheckVisitor = new VariableCheckVisitor(variableName);
			for (Expression argument : iArguments) {
				argument.accept(variableCheckVisitor);
				if (variableCheckVisitor.getVariableFound()) {
					variableUsed = true;
					break;
				}
			}
			return variableUsed;
		}
	}
	
	public static class AccessBeanUserMethodInvocationStatement extends AccessBeanStatement {
		private String iMethodKey;
		private List<Expression> iArguments = new ArrayList<Expression>();
		private TargetExceptionInfo iArgumentsUnhandledExceptions = new TargetExceptionInfo();
		
		public AccessBeanUserMethodInvocationStatement(String methodKey) {
			iMethodKey = methodKey;
		}
		
		public String getMethodKey() {
			return iMethodKey;
		}
		
		public void addArgument(Expression argument) {
			iArguments.add(argument);
		}
		
		public List<Expression> getArguments() {
			return iArguments;
		}
		
		public void resolve(AccessBeanMethodInfo accessBeanMethodInfo, Deque<List<AccessBeanStatement>> statementListStack) {
			AccessBeanExpressionVisitor accessBeanExpressionVisitor = new AccessBeanExpressionVisitor(accessBeanMethodInfo, this);
			ModuleInfo moduleInfo = accessBeanMethodInfo.getEntityInfo().getModuleInfo();
			for (Expression argument : iArguments) {
				iArgumentsUnhandledExceptions.addAll(TargetExceptionUtil.getUnhandledTargetExceptions(moduleInfo.getApplicationInfo(), moduleInfo.getJavaProject(), argument));
				accessBeanMethodInfo.renameVariables(argument);
				argument.accept(accessBeanExpressionVisitor);
			}
		}
		
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
			unhandledExceptions.addAll(iArgumentsUnhandledExceptions);
			return unhandledExceptions;
		}
		
		public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
			super.replaceASTNode(astNode, newNode);
			for (int i = 0; i < iArguments.size(); i++) {
				if (iArguments.get(i) == astNode) {
					iArguments.set(i, (Expression) newNode);
					break;
				}
			}
		}
		
		public boolean usesVariable(String variableName) {
			boolean variableUsed = false;
			VariableCheckVisitor variableCheckVisitor = new VariableCheckVisitor(variableName);
			for (Expression argument : iArguments) {
				argument.accept(variableCheckVisitor);
				if (variableCheckVisitor.getVariableFound()) {
					variableUsed = true;
					break;
				}
			}
			return variableUsed;
		}
	}
	
	public static class AccessBeanReturnStatement extends AccessBeanStatement {
		private Expression iReturnExpression;
		private TargetExceptionInfo iReturnExpressionUnhandledExceptions;
		
		public AccessBeanReturnStatement() {
		}
		
		public void setReturnExpression(Expression returnExpression) {
			iReturnExpression = returnExpression;
		}
		
		public Expression getReturnExpression() {
			return iReturnExpression;
		}
		
		public void resolve(AccessBeanMethodInfo accessBeanMethodInfo, Deque<List<AccessBeanStatement>> statementListStack) {
			if (iReturnExpression != null) {
				ModuleInfo moduleInfo = accessBeanMethodInfo.getEntityInfo().getModuleInfo();
				iReturnExpressionUnhandledExceptions = TargetExceptionUtil.getUnhandledTargetExceptions(moduleInfo.getApplicationInfo(), moduleInfo.getJavaProject(), iReturnExpression);
				accessBeanMethodInfo.renameVariables(iReturnExpression);
				iReturnExpression.accept(new AccessBeanExpressionVisitor(accessBeanMethodInfo, this));
			}
		}
		
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
			if (iReturnExpressionUnhandledExceptions != null) {
				unhandledExceptions.addAll(iReturnExpressionUnhandledExceptions);
			}
			return unhandledExceptions;
		}
		
		public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
			super.replaceASTNode(astNode, newNode);
			if (iReturnExpression == astNode) {
				iReturnExpression = (Expression) newNode;
			}
		}
		
		public boolean usesVariable(String variableName) {
			boolean variableUsed = false;
			if (iReturnExpression != null) {
				VariableCheckVisitor variableCheckVisitor = new VariableCheckVisitor(variableName);
				iReturnExpression.accept(variableCheckVisitor);
				if (variableCheckVisitor.getVariableFound()) {
					variableUsed = true;
				}
			}
			return variableUsed;
		}
	}
	
	public static class AccessBeanTryStatement extends AccessBeanStatement {
		private List<AccessBeanStatement> iTryStatements = new ArrayList<AccessBeanStatement>();
		private List<AccessBeanCatchClause> iCatchClauses = new ArrayList<AccessBeanCatchClause>();
		private List<AccessBeanStatement> iFinallyStatements = new ArrayList<AccessBeanStatement>();
		
		public AccessBeanTryStatement() {
		}
		
		public List<AccessBeanStatement> getTryStatements() {
			return iTryStatements;
		}
		
		public List<AccessBeanCatchClause> getCatchClauses() {
			return iCatchClauses;
		}
		
		public List<AccessBeanStatement> getFinallyStatements() {
			return iFinallyStatements;
		}
		
		public void resolve(AccessBeanMethodInfo accessBeanMethodInfo, Deque<List<AccessBeanStatement>> statementListStack) {
			List<AccessBeanStatement> statementList = statementListStack.peek();
			accessBeanMethodInfo.resolveStatements(statementListStack, iTryStatements);
			accessBeanMethodInfo.resolveStatements(statementListStack, iFinallyStatements);
			if (iTryStatements.size() == 0 && iFinallyStatements.size() == 0) {
				statementList.remove(this);
			}
			else if (iTryStatements.size() == 0) {
				int index = statementList.indexOf(this);
				statementList.remove(this);
				while (iFinallyStatements.size() > 0) {
					statementList.add(index, iFinallyStatements.remove(iFinallyStatements.size() - 1));
				}
			}
			else {
				TargetExceptionInfo tryBlockExceptionInfo = new TargetExceptionInfo();
				for (AccessBeanStatement statement : iTryStatements) {
					tryBlockExceptionInfo.addAll(statement.getUnhandledExceptions(accessBeanMethodInfo));
				}
				IJavaProject javaProject = accessBeanMethodInfo.getEntityInfo().getModuleInfo().getJavaProject();
				Collection<String> tryBlockExceptions = tryBlockExceptionInfo.getTargetExceptions();
				for (String exception : tryBlockExceptions) {
					for (AccessBeanCatchClause catchClause : iCatchClauses) {
						if (TargetExceptionUtil.catchHandlesException(javaProject, catchClause.getExceptionType(), exception)) {
							catchClause.getHandledExceptions().add(exception);
							break;
						}
						else if (TargetExceptionUtil.catchHandlesTargetException(javaProject, catchClause.getExceptionType(), exception)) {
							catchClause.getHandledExceptions().add(exception);
							break;
						}
					}
				}
				int i = iCatchClauses.size() - 1;
				while (i >= 0) {
					AccessBeanCatchClause catchClause = iCatchClauses.get(i);
					if (catchClause.getHandledExceptions().size() > 0) {
						accessBeanMethodInfo.resolveStatements(statementListStack, catchClause.getCatchStatements());
						if (catchClause.getCatchStatements().size() == 0) {
							iCatchClauses.remove(i);
						}
					}
					else {
						iCatchClauses.remove(i);
					}
					i--;
				}
				if (iCatchClauses.size() == 0 && iFinallyStatements.size() == 0) {
					int index = statementList.indexOf(this);
					statementList.remove(this);
					while (iTryStatements.size() > 0) {
						statementList.add(index, iTryStatements.remove(iTryStatements.size() - 1));
					}
				}
			}
		}
		
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			TargetExceptionInfo tryBlockTargetExceptionInfo = new TargetExceptionInfo();
			for (AccessBeanStatement statement : iTryStatements) {
				tryBlockTargetExceptionInfo.addAll(statement.getUnhandledExceptions(accessBeanMethodInfo));
			}
			IJavaProject javaProject = accessBeanMethodInfo.getEntityInfo().getModuleInfo().getJavaProject();
			TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
			Collection<String> tryBlockExceptions = tryBlockTargetExceptionInfo.getTargetExceptions();
			for (String exception : tryBlockExceptions) {
				boolean handled = false;
				for (AccessBeanCatchClause catchClause : iCatchClauses) {
					if (TargetExceptionUtil.catchHandlesException(javaProject, catchClause.getExceptionType(), exception)) {
						handled = true;
						break;
					}
					else if (TargetExceptionUtil.catchHandlesTargetException(javaProject, catchClause.getExceptionType(), exception)) {
						handled = true;
						break;
					}
				}
				if (!handled) {
					unhandledExceptions.addTargetException(exception);
				}
			}
			for (AccessBeanCatchClause catchClause : iCatchClauses) {
				unhandledExceptions.addAll(catchClause.getUnhandledExceptions(accessBeanMethodInfo));
			}
			for (AccessBeanStatement statement : iFinallyStatements) {
				unhandledExceptions.addAll(statement.getUnhandledExceptions(accessBeanMethodInfo));
			}
			return unhandledExceptions;
		}
		
		public boolean usesVariable(String variableName) {
			boolean variableUsed = false;
			for (AccessBeanStatement statement : iTryStatements) {
				if (statement.usesVariable(variableName)) {
					variableUsed = true;
					break;
				}
			}
			if (!variableUsed) {
				for (AccessBeanStatement statement : iFinallyStatements) {
					if (statement.usesVariable(variableName)) {
						variableUsed = true;
						break;
					}
				}
			}
			if (!variableUsed) {
				for (AccessBeanCatchClause catchClause : iCatchClauses) {
					if (catchClause.usesVariable(variableName)) {
						variableUsed = true;
						break;
					}
				}
			}
			return variableUsed;
		}
		
		public boolean containsStatement(AccessBeanStatement statement) {
			boolean match = statement == this;
			if (!match) {
				for (AccessBeanStatement currentStatement : iTryStatements) {
					if (currentStatement.containsStatement(statement)) {
						match = true;
						break;
					}
				}
				if (!match) {
					for (AccessBeanStatement currentStatement : iFinallyStatements) {
						if (currentStatement.containsStatement(statement)) {
							match = true;
							break;
						}
					}
				}
				if (!match) {
					for (AccessBeanCatchClause catchClause : iCatchClauses) {
						if (catchClause.containsStatement(statement)) {
							match = true;
							break;
						}
					}
				}
			}
			return match;
		}
	}
	
	public static class AccessBeanCatchClause {
		private AccessBeanMethodInfo iAccessBeanMethodInfo;
		private String iExceptionType;
		private String iExceptionVariableName;
		private List<AccessBeanStatement> iCatchStatements = new ArrayList<AccessBeanStatement>();
		private Collection<String> iHandledExceptions = new HashSet<String>();
		
		public AccessBeanCatchClause(AccessBeanMethodInfo accessBeanMethodInfo) {
			iAccessBeanMethodInfo = accessBeanMethodInfo;
		}
		
		public void setExceptionType(String exceptionType) {
			iExceptionType = exceptionType;
		}
		
		public String getExceptionType() {
			return iExceptionType;
		}
		
		public Collection<String> getTargetExceptionTypes() {
			Collection<String> targetExceptionTypes = new HashSet<String>();
			if (iHandledExceptions.size() == 1) {
				targetExceptionTypes.addAll(iHandledExceptions);
			}
			else {
				IJavaProject javaProject = iAccessBeanMethodInfo.getEntityInfo().getModuleInfo().getJavaProject();
				Collection<String> unhandledExceptions = new HashSet<String>();
				for (String handledException : iHandledExceptions) {
					if (TargetExceptionUtil.catchHandlesException(javaProject, iExceptionType, handledException)) {
						targetExceptionTypes.add(iExceptionType);
					}
					else {
						unhandledExceptions.add(handledException);
					}
				}
				if (unhandledExceptions.size() == 1) {
					targetExceptionTypes.addAll(unhandledExceptions);
				}
				else if (unhandledExceptions.size() > 1) {
					String targetException = null;
					for (String exception : unhandledExceptions) {
						if (targetException == null) {
							targetException = exception;
						}
						else if (TargetExceptionUtil.catchHandlesException(javaProject, exception, targetException)) {
							targetException = exception;
						}
					}
					targetExceptionTypes.add(targetException);
				}
			}
			return targetExceptionTypes;
		}
		
		public void setExceptionVariableName(String exceptionVariableName) {
			iExceptionVariableName = exceptionVariableName;
		}
		
		public String getExceptionVariableName() {
			return iExceptionVariableName;
		}
		
		public List<AccessBeanStatement> getCatchStatements() {
			return iCatchStatements;
		}
		
		public Collection<String> getHandledExceptions() {
			return iHandledExceptions;
		}
		
		public TargetExceptionInfo getUnhandledExceptions(AccessBeanMethodInfo accessBeanMethodInfo) {
			TargetExceptionInfo unhandledExceptions = new TargetExceptionInfo();
			for (AccessBeanStatement statement : iCatchStatements) {
				unhandledExceptions.addAll(statement.getUnhandledExceptions(accessBeanMethodInfo));
			}
			return unhandledExceptions;
		}
		
		public boolean usesVariable(String variableName) {
			boolean variableUsed = false;
			for (AccessBeanStatement statement : iCatchStatements) {
				if (statement.usesVariable(variableName)) {
					variableUsed = true;
					break;
				}
			}
			return variableUsed;
		}
		
		public boolean containsStatement(AccessBeanStatement statement) {
			boolean match = false;
			for (AccessBeanStatement currentStatement : iCatchStatements) {
				if (currentStatement.containsStatement(statement)) {
					match = true;
					break;
				}
			}
			return match;
		}
	}
	
	private static class VariableCheckVisitor extends ASTVisitor {
		private String iVariableName;
		private boolean iVariableFound = false;
		public VariableCheckVisitor(String variableName) {
			iVariableName = variableName;
		}
		
		public boolean visit(SimpleName simpleName) {
			if (iVariableName.equals(simpleName.getIdentifier())) {
				IBinding binding = simpleName.resolveBinding();
				if (binding == null) {
					iVariableFound = true;
				}
				else if (binding instanceof IVariableBinding) {
					IVariableBinding variableBinding = (IVariableBinding) binding;
					if (variableBinding.getDeclaringMethod() != null) {
						iVariableFound = true;
					}
				}
			}
			return false;
		}
		
		public boolean getVariableFound() {
			return iVariableFound;
		}
	}
	
	private static class VariableRenameVisitor extends ASTVisitor {
		private Map<String, String> iVariableNameMap;
		
		public VariableRenameVisitor(Map<String, String> variableMap) {
			iVariableNameMap = variableMap;
		}
		
		public boolean visit(SimpleName simpleName) {
			if (iVariableNameMap.containsKey(simpleName.getIdentifier())) {
				IBinding binding = simpleName.resolveBinding();
				if (binding instanceof IVariableBinding) {
					IVariableBinding variableBinding = (IVariableBinding) binding;
					if (variableBinding.getDeclaringMethod() != null) {
						simpleName.setIdentifier(iVariableNameMap.get(simpleName.getIdentifier()));
					}
				}
			}
			return false;
		}
	}
	
	private static class AccessBeanExpressionVisitor extends PortVisitor {
		private EntityInfo iEntityInfo;
		private AccessBeanMethodInfo iAccessBeanMethodInfo;
		private AccessBeanStatement iAccessBeanStatement;
		
		public AccessBeanExpressionVisitor(AccessBeanMethodInfo accessBeanMethodInfo, AccessBeanStatement accessBeanStatement) {
			super(accessBeanMethodInfo.getEntityInfo().getModuleInfo().getApplicationInfo(), accessBeanMethodInfo.getEntityInfo().getModuleInfo().getJavaProject());
			iEntityInfo = accessBeanMethodInfo.getEntityInfo();
			iAccessBeanMethodInfo = accessBeanMethodInfo;
			iAccessBeanStatement = accessBeanStatement;
		}

		public boolean visit(SimpleName simpleName) {
			IBinding binding = simpleName.resolveBinding();
			if (binding != null && binding.getKind() == ITypeBinding.VARIABLE && !simpleName.isDeclaration()) {
				IVariableBinding variableBinding = (IVariableBinding) binding;
				if (!simpleName.isDeclaration() && variableBinding.getDeclaringClass() != null && variableBinding.isField()) {
					boolean staticField = (variableBinding.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) == (Modifier.STATIC | Modifier.FINAL);
					String fieldName = simpleName.getIdentifier();
					String declaringClass = variableBinding.getDeclaringClass().getQualifiedName();
					if (!iEntityInfo.isClassInEjbHierarchy(declaringClass)) {
						if (staticField) {
							Name newName = simpleName.getAST().newName(declaringClass + "." + fieldName);
							replaceASTNode(simpleName, newName);
						}
					}
					else {
						FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(simpleName.getIdentifier());
						if (fieldInfo == null) {
							if (staticField) {
								iAccessBeanMethodInfo.getReferencedStaticFieldNames().add(fieldName);
							}
							else {
								iAccessBeanMethodInfo.getReferencedInstanceVariableNames().add(fieldName);
							}
						}
						else {
							replaceFieldReference(simpleName, fieldInfo);
						}
					}
				}
			}
			else {
				super.visit(simpleName);
			}
			return false;
		}
		
		public boolean visit(FieldAccess fieldAccess) {
			boolean visitChildren = true;
			if (fieldAccess.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
				IVariableBinding variableBinding = fieldAccess.resolveFieldBinding();
				if (variableBinding != null && variableBinding.getDeclaringClass() != null && variableBinding.isField()) {
					boolean staticField = (variableBinding.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) == (Modifier.STATIC | Modifier.FINAL);
					String fieldName = fieldAccess.getName().getIdentifier();
					String declaringClass = variableBinding.getDeclaringClass().getQualifiedName();
					if (!iEntityInfo.isClassInEjbHierarchy(declaringClass)) {
						if (staticField) {
							Name newName = fieldAccess.getAST().newName(declaringClass + "." + fieldName);
							replaceASTNode(fieldAccess, newName);
							visitChildren = false;
						}
					}
					else {
						FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
						if (fieldInfo == null) {
							if (staticField) {
								iAccessBeanMethodInfo.getReferencedStaticFieldNames().add(fieldName);
							}
							else {
								iAccessBeanMethodInfo.getReferencedInstanceVariableNames().add(fieldName);
							}
							Name newName = fieldAccess.getAST().newName(fieldName);
							replaceASTNode(fieldAccess, newName);
							visitChildren = false;
						}
						else {
							replaceFieldReference(fieldAccess, fieldInfo);
							visitChildren = false;
						}
					}
				}
			}
			if (visitChildren) {
				visitChildren = super.visit(fieldAccess);
			}
			return visitChildren;
		}
		
		public boolean visit(SuperFieldAccess superFieldAccess) {
			if (superFieldAccess.getQualifier() == null) {
				FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(superFieldAccess.getName().getIdentifier());
				if (fieldInfo != null) {
					replaceFieldReference(superFieldAccess, fieldInfo);
				}
			}
			return false;
		}
		
		public boolean visit(MethodInvocation methodInvocation) {
			boolean visitChildren = true;
			if (methodInvocation.getExpression() == null || methodInvocation.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
				IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
				if (methodBinding != null) {
					String methodKey = JavaUtil.getMethodKey(methodBinding);
					if (methodKey.equals(iEntityInfo.getGeneratePrimaryKeyMethodKey())) {
						MethodInvocation newMethodInvocation = methodInvocation.getAST().newMethodInvocation();
						SimpleName newName = methodInvocation.getAST().newSimpleName("generatePrimaryKey");
						newMethodInvocation.setName(newName);
						replaceASTNode(methodInvocation, newMethodInvocation);
						visitChildren = false;
					}
					else {
						FieldInfo fieldInfo = iEntityInfo.getFieldInfoByGetterName(methodKey);
						if (fieldInfo != null) {
							replaceFieldReference(methodInvocation, fieldInfo);
							visitChildren = false;
						}
						else {
							if (!methodKey.equals(iEntityInfo.getGeneratePrimaryKeyMethodKey()) && iEntityInfo.getFieldInfoByGetterName(methodKey) == null) {
								iAccessBeanMethodInfo.addReferencedAccessBeanMethod(methodKey);
							}
						}
					}
				}
				else {
					System.out.println("EntityResolver no method binding for " + iEntityInfo.getEjbName() + " " +methodInvocation);
				}
			}
			if (methodInvocation.getExpression() != null) {
				ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
				if (typeBinding != null) {
					String typeName = typeBinding.getQualifiedName();
					if (CLOB_ATTRIBUTE_VALUE.equals(typeName) || BLOB_ATTRIBUTE_VALUE.equals(typeName)) {
						String methodName = methodInvocation.getName().getIdentifier();
						if (methodName.equals(SET_VALUE)) {
							Expression argument = (Expression) methodInvocation.arguments().get(0);
							argument.delete();
							replaceASTNode(methodInvocation, argument);
							argument.accept(this);
							visitChildren = false;
						}
						else if (methodName.equals(GET_VALUE)) {
							String fieldName = null;
							Expression clobExpression = methodInvocation.getExpression();
							switch(clobExpression.getNodeType()) {
								case ASTNode.SIMPLE_NAME: {
									SimpleName simpleName = (SimpleName) clobExpression;
									fieldName = simpleName.getIdentifier();
									break;
								}
								case ASTNode.FIELD_ACCESS: {
									FieldAccess fieldAccess = (FieldAccess) clobExpression;
									fieldName = fieldAccess.getName().getIdentifier();
									break;
								}
								case ASTNode.SUPER_FIELD_ACCESS: {
									SuperFieldAccess superFieldAccess = (SuperFieldAccess) clobExpression;
									fieldName = superFieldAccess.getName().getIdentifier();
									break;
								}
							}
							if (fieldName != null && iEntityInfo.getClobAttributeFieldInfo(fieldName) != null) {
								FieldInfo fieldInfo = iEntityInfo.getClobAttributeFieldInfo(fieldName);
								replaceFieldReference(methodInvocation, fieldInfo);
								visitChildren = false;
							}
						}
					}
				}
			}
			if (visitChildren) {
				visitChildren = super.visit(methodInvocation);
			}
			return visitChildren;
		}
		
		private void replaceFieldReference(ASTNode fieldReference, FieldInfo fieldInfo) {
			AST ast = fieldReference.getAST();
			if (fieldInfo.getRelatedEntityInfo() == null) {
				MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
				getterMethodInvocation.setExpression(ast.newSimpleName("iTypedEntity"));
				getterMethodInvocation.setName(ast.newSimpleName(fieldInfo.getTargetGetterName()));
				if (fieldInfo.getColumnInfo() != null && CHAR.equals(fieldInfo.getColumnInfo().getTypeName()) && fieldInfo.getColumnInfo().getLength() != null && fieldInfo.getColumnInfo().getLength() > 1) {
					MethodInvocation trimMethodInvocation = ast.newMethodInvocation();
					trimMethodInvocation.setName(ast.newSimpleName(TRIM));
					trimMethodInvocation.setExpression(getterMethodInvocation);
					if (fieldInfo.getColumnInfo().getNullable()) {
						InfixExpression infixExpression = ast.newInfixExpression();
						infixExpression.setLeftOperand((Expression) ASTNode.copySubtree(ast, getterMethodInvocation));
						infixExpression.setOperator(Operator.EQUALS);
						infixExpression.setRightOperand(ast.newNullLiteral());
						ConditionalExpression conditionalExpression = ast.newConditionalExpression();
						conditionalExpression.setExpression(infixExpression);
						conditionalExpression.setThenExpression(ast.newNullLiteral());
						conditionalExpression.setElseExpression(trimMethodInvocation);
						replaceASTNode(fieldReference, conditionalExpression);
					}
					else {
						replaceASTNode(fieldReference, trimMethodInvocation);
					}
				}
				else {
					replaceASTNode(fieldReference, getterMethodInvocation);
				}
			}
			else {
				RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
				String relatedEntityGetterName = relatedEntityInfo.getGetterName();
				if (relatedEntityGetterName == null) {
					relatedEntityGetterName = "get" + Character.toUpperCase(relatedEntityInfo.getFieldName().charAt(0)) + relatedEntityInfo.getFieldName().substring(1);
				}
				String referencedFieldGetterName = fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null ? fieldInfo.getReferencedFieldInfo().getTargetGetterName() : fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo().getGetterName();
				ConditionalExpression conditionalExpression = ast.newConditionalExpression();
				MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
				getterMethodInvocation.setExpression(ast.newSimpleName("iTypedEntity"));
				getterMethodInvocation.setName(ast.newSimpleName(relatedEntityGetterName));
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
					ColumnInfo referencedColumnInfo = fieldInfo.getReferencedFieldInfo().getColumnInfo();
					if (referencedColumnInfo != null && CHAR.equals(referencedColumnInfo.getTypeName()) && referencedColumnInfo.getLength() != null && referencedColumnInfo.getLength() > 1) {
						MethodInvocation trimMethodInvocation = ast.newMethodInvocation();
						trimMethodInvocation.setName(ast.newSimpleName(TRIM));
						trimMethodInvocation.setExpression(fieldMethodInvocation);
						conditionalExpression.setElseExpression(trimMethodInvocation);
					}
					else {
						conditionalExpression.setElseExpression(fieldMethodInvocation);			
					}
				}
				else {
					MethodInvocation relatedFieldMethodInvocation = ast.newMethodInvocation();
					relatedFieldMethodInvocation.setExpression(fieldMethodInvocation);
					relatedFieldMethodInvocation.setName(ast.newSimpleName(fieldInfo.getReferencedFieldInfo().getReferencedFieldInfo().getTargetGetterName()));
					conditionalExpression.setElseExpression(relatedFieldMethodInvocation);
				}
				ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
				parenthesizedExpression.setExpression(conditionalExpression);
				replaceASTNode(fieldReference, parenthesizedExpression);
			}
		}
		
		public void replaceASTNode(ASTNode astNode, ASTNode newNode) {
			iAccessBeanStatement.replaceASTNode(astNode, newNode);
		}
	}
}
