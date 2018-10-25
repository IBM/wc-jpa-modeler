package com.ibm.commerce.jpa.port.parsers;

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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.KeyClassConstructorInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;

public class EjbKeyClassParser {
	private EntityInfo iEntityInfo;
	private ModuleInfo iModuleInfo;
	private ASTParser iASTParser;
	
	public EjbKeyClassParser(ASTParser astParser, EntityInfo entityInfo) {
		iASTParser = astParser;
		iEntityInfo = entityInfo;
		iModuleInfo = entityInfo.getModuleInfo();
	}
	
	public void parse(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("parse key class for " + iEntityInfo.getEjbName(), IProgressMonitor.UNKNOWN);
			IType primaryKeyType = iEntityInfo.getPrimaryKeyType();
			if (primaryKeyType != null && !primaryKeyType.isBinary()) {
				iModuleInfo.addDeleteIntendedType(primaryKeyType.getFullyQualifiedName('.'));
				parseKeyClass(progressMonitor, primaryKeyType);
			}
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private void parseKeyClass(IProgressMonitor progressMonitor, IType primaryKeyType) {
		CompilationUnit compilationUnit = null;
		if (iEntityInfo.getSupertype() != null && iEntityInfo.getPrimaryKeyClass().equals(iEntityInfo.getSupertype().getPrimaryKeyClass())) {
			compilationUnit = iEntityInfo.getSupertype().getEjbKeyClassCompilationUnit();
		}
		else {
			iASTParser.setResolveBindings(true);
			iASTParser.setSource(primaryKeyType.getCompilationUnit());
			compilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
			if (iEntityInfo.getSubtypes() != null) {
				iEntityInfo.setEjbKeyClassCompilationUnit(compilationUnit);
			}
		}
		TypeDeclaration typeDeclaration = (TypeDeclaration) compilationUnit.types().get(0);
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			if (bodyDeclaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
				parseMethodDeclaration(primaryKeyType, (MethodDeclaration) bodyDeclaration);
				progressMonitor.worked(100);
			}
		}
		iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
	}
	
	private void parseMethodDeclaration(IType primaryKeyType, MethodDeclaration methodDeclaration) {
		if (methodDeclaration.isConstructor()) {
			KeyClassConstructorInfo constructorInfo = new KeyClassConstructorInfo();
			ConstructorVisitor constructorVisitor = new ConstructorVisitor();
			List<FieldInfo> constructorFields = constructorInfo.getFields();
			methodDeclaration.getBody().accept(constructorVisitor);
			@SuppressWarnings("unchecked")
			List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
			for (SingleVariableDeclaration singleVariableDeclaration : parameters) {
				String parameterName = singleVariableDeclaration.getName().getIdentifier();
				FieldInfo fieldInfo = constructorVisitor.getFieldInfo(parameterName);
				constructorFields.add(fieldInfo);
			}
			int index = 0;
			for (FieldInfo constructorField : constructorFields) {
				if (constructorField == null) {
					List<FieldInfo> keyFields = iEntityInfo.getKeyFields();
					for (FieldInfo keyField : keyFields) {
						if (!constructorFields.contains(keyField)) {
							constructorFields.set(index, keyField);
							break;
						}
					}
				}
				index++;
			}
			iEntityInfo.addKeyClassConstructor(constructorInfo);
		}
	}
	
	private class ConstructorVisitor extends ASTVisitor {
		private Map<String, FieldInfo> iParameterToFieldMap = new HashMap<String, FieldInfo>();
		
		public ConstructorVisitor() {
		}
		
		public FieldInfo getFieldInfo(String parameterName) {
			return iParameterToFieldMap.get(parameterName);
		}
		
		public boolean visit(Assignment assignment) {
			String parameterName = null;
			FieldInfo fieldInfo = null;
			Expression rightHandSide = assignment.getRightHandSide();
			if (rightHandSide.getNodeType() == ASTNode.SIMPLE_NAME) {
				SimpleName simpleName = (SimpleName) rightHandSide;
				if (!simpleName.isDeclaration()) {
					IBinding binding = simpleName.resolveBinding();
					if (binding instanceof IVariableBinding && ((IVariableBinding) binding).isParameter()) {
						parameterName = simpleName.getIdentifier();
						Expression leftHandSide = assignment.getLeftHandSide();
						switch (leftHandSide.getNodeType()) {
							case ASTNode.SIMPLE_NAME: {
								simpleName = (SimpleName) leftHandSide;
								fieldInfo = iEntityInfo.getFieldInfoByName(simpleName.getIdentifier());
								break;
							}
							case ASTNode.FIELD_ACCESS: {
								FieldAccess fieldAccess = (FieldAccess) leftHandSide;
								if (fieldAccess.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
									fieldInfo = iEntityInfo.getFieldInfoByName(fieldAccess.getName().getIdentifier());
								}
								break;
							}
							case ASTNode.SUPER_FIELD_ACCESS: {
								SuperFieldAccess superFieldAccess = (SuperFieldAccess) leftHandSide;
								fieldInfo = iEntityInfo.getFieldInfoByName(superFieldAccess.getName().getIdentifier());
								break;
							}
						}
					}
				}
			}
			if (parameterName != null && fieldInfo != null) {
				iParameterToFieldMap.put(parameterName, fieldInfo); 
			}
			return false;
		}
	}
	
}
