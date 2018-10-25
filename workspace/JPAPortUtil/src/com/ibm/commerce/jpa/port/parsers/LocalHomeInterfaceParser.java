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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FinderInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.util.AbstractEntityDataUtil;
import com.ibm.commerce.jpa.port.util.JavaUtil;

public class LocalHomeInterfaceParser {
	private static final String CREATE = "create";
	private static final String FIND = "find";
	private static final String FIND_BY_PRIMARY_KEY = "findByPrimaryKey";
	private static final String EJS_LOCAL_CMP = "EJSLocalCMP";
	
	private EntityInfo iEntityInfo;
	private ModuleInfo iModuleInfo;
	private ASTParser iASTParser;
	
	public LocalHomeInterfaceParser(ASTParser astParser, EntityInfo entityInfo) {
		iASTParser = astParser;
		iEntityInfo = entityInfo;
		iModuleInfo = entityInfo.getModuleInfo();
	}

	public void parse(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("parse local home interface for " + iEntityInfo.getEjbName(), IProgressMonitor.UNKNOWN);
			IType localHomeInterface = iEntityInfo.getLocalHomeType();
			if (localHomeInterface != null) {
				parseInterface(progressMonitor, localHomeInterface);
				ITypeHierarchy typeHierarchy = localHomeInterface.newTypeHierarchy(iModuleInfo.getJavaProject(), new SubProgressMonitor(progressMonitor, 1000));
				IType[] implementingClasses = typeHierarchy.getImplementingClasses(localHomeInterface);
				if (implementingClasses != null) {
					for (IType implementingClass : implementingClasses) {
						String typeName = implementingClass.getTypeQualifiedName();
						if (typeName.startsWith(EJS_LOCAL_CMP)) {
							iModuleInfo.addDeleteIntendedType(implementingClass.getFullyQualifiedName('.'));
							String homeBeanClassName = implementingClass.getFullyQualifiedName('.').replace("Home", "HomeBean").replace("EJSLocalCMP", "EJSCMP");
							IType homeBeanClassType = JavaUtil.resolveType(implementingClass, homeBeanClassName);
							if (homeBeanClassType != null) {
								iModuleInfo.addDeleteIntendedType(homeBeanClassName);
							}
						}
						else {
							System.out.println("unexpected local home interface implementor: "+implementingClass.getFullyQualifiedName('.'));
						}
					}
				}
			}
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private void parseInterface(IProgressMonitor progressMonitor, IType interfaceType) throws JavaModelException {
		iModuleInfo.addDeleteIntendedType(interfaceType.getFullyQualifiedName('.'));
		String[] superInterfaceNames = interfaceType.getSuperInterfaceNames();
		for (String superInterfaceName : superInterfaceNames) {
			IType superInterface = JavaUtil.resolveType(interfaceType, superInterfaceName);
			if (superInterface != null && superInterface.getResource() != null && superInterface.getResource().getProject() == interfaceType.getResource().getProject()) {
				parseInterface(progressMonitor, superInterface);
			}
		}
		iASTParser.setResolveBindings(true);
		iASTParser.setSource(interfaceType.getCompilationUnit());
		CompilationUnit compilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
		TypeDeclaration typeDeclaration = (TypeDeclaration) compilationUnit.types().get(0);
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			if (bodyDeclaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
				parseMethodDeclaration(interfaceType, (MethodDeclaration) bodyDeclaration);
				progressMonitor.worked(100);
			}
		}
		iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
	}
	
	private void parseMethodDeclaration(IType interfaceType, MethodDeclaration methodDeclaration) {
		String methodName = methodDeclaration.getName().getIdentifier();
		@SuppressWarnings("unchecked")
		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		String[] parameterTypes = new String[parameters.size()];
		int i = 0;
		for (SingleVariableDeclaration parameter : parameters) {
			Type parameterType = parameter.getType();
			parameterTypes[i] = parameterType.resolveBinding().getQualifiedName();
			for (int j = 0; j < parameter.getExtraDimensions(); j++) {
				parameterTypes[i] += "[]";
			}
			i++;
		}
		if (methodName.startsWith(FIND) && !methodName.equals(FIND_BY_PRIMARY_KEY)) {
			FinderInfo finderInfo = iEntityInfo.getFinderInfo(methodName, parameterTypes, true);
			finderInfo.setInHomeInterface(true);
			int parameterIndex = 0;
			for (SingleVariableDeclaration parameter : parameters) {
				finderInfo.setFinderMethodParameterName(parameterIndex, parameter.getName().getIdentifier());
				parameterIndex++;
			}
			finderInfo.setFinderMethodReturnType(methodDeclaration.getReturnType2().resolveBinding().getQualifiedName());
		}
		else if (CREATE.equals(methodName)) {
			boolean entityDataCreator = false;
			if (parameters.size() == 1) {
				entityDataCreator = isEntityDataType(interfaceType, parameterTypes[0]);
			}
			if (!entityDataCreator && parameters.size() > 0) {
				iEntityInfo.getCreatorInfo(parameterTypes, true);
			}
		}
	}
	
	private boolean isEntityDataType(IType interfaceType, String typeName) {
		IType type = JavaUtil.resolveType(interfaceType, typeName);
		return AbstractEntityDataUtil.isAbstractEntityDataType(type);
	}
}
