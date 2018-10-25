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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.UserMethodInfo;
import com.ibm.commerce.jpa.port.util.AbstractEntityDataUtil;
import com.ibm.commerce.jpa.port.util.JavaUtil;

public class LocalInterfaceParser {
	private static final String PROTECTABLE = "com.ibm.commerce.security.Protectable";
	private static final String GROUPABLE = "com.ibm.commerce.grouping.Groupable";
	private static final String EJB_LOCAL_OBJECT = "javax.ejb.EJBLocalObject";
	private static final Set<String> STANDARD_INTERFACES;
	private static final Set<String> STANDARD_METHODS;
	static {
		STANDARD_INTERFACES = new HashSet<String>();
		STANDARD_INTERFACES.add(PROTECTABLE);
		STANDARD_INTERFACES.add(GROUPABLE);
		STANDARD_INTERFACES.add(EJB_LOCAL_OBJECT);
		STANDARD_METHODS = new HashSet<String>();
	}
	private static final String EJS_LOCAL_CMP = "EJSLocalCMP";
	
	private ASTParser iASTParser;
	private EntityInfo iEntityInfo;
	private ModuleInfo iModuleInfo;
	
	public LocalInterfaceParser(ASTParser astParser, EntityInfo entityInfo) {
		iASTParser = astParser;
		iEntityInfo = entityInfo;
		iModuleInfo = entityInfo.getModuleInfo();
	}

	public void parse(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("parse local interface for " + iEntityInfo.getEjbName(), IProgressMonitor.UNKNOWN);
			IType localInterface = iEntityInfo.getLocalType();
			if (localInterface != null) {
				parseInterface(progressMonitor, localInterface);
				ITypeHierarchy typeHierarchy = localInterface.newTypeHierarchy(iModuleInfo.getJavaProject(), new SubProgressMonitor(progressMonitor, 1000));
				IType[] implementingClasses = typeHierarchy.getImplementingClasses(localInterface);
				if (implementingClasses != null) {
					for (IType implementingClass : implementingClasses) {
						String typeName = implementingClass.getTypeQualifiedName();
						if (typeName.startsWith(EJS_LOCAL_CMP)) {
							iModuleInfo.addDeleteIntendedType(implementingClass.getFullyQualifiedName('.'));
						}
						else {
							System.out.println("unexpected remote interface implementor: "+implementingClass.getFullyQualifiedName('.'));
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
			if (superInterface != null) {
				superInterfaceName = superInterface.getFullyQualifiedName('.');
				if (PROTECTABLE.equals(superInterfaceName)) {
					iEntityInfo.setProtectable(true);
				}
				else if (GROUPABLE.equals(superInterfaceName)) {
					iEntityInfo.setGroupable(true);
				}
				if (!STANDARD_INTERFACES.contains(superInterfaceName)) {
					if (superInterface.getResource() != null && superInterface.getResource().getProject() == interfaceType.getResource().getProject()) {
						parseInterface(progressMonitor, superInterface);
					}
				}
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
		if (!STANDARD_METHODS.contains(methodName)) {
			UserMethodInfo userMethodInfo = new UserMethodInfo(methodName);
			@SuppressWarnings("unchecked")
			List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
			for (SingleVariableDeclaration parameter : parameters) {
				String parameterName = parameter.getName().getIdentifier();
				Type parameterType = parameter.getType();
				String parameterTypeName = parameterType.resolveBinding().getQualifiedName();
				for (int i = 0; i < parameter.getExtraDimensions(); i++) {
					parameterTypeName += "[]";
				}
				userMethodInfo.addParameter(parameterName, parameterTypeName);
			}
//			@SuppressWarnings("unchecked")
//			List<Name> thrownExceptions = methodDeclaration.thrownExceptions();
//			for (Name thrownException : thrownExceptions) {
//				userMethodInfo.addException(((ITypeBinding) thrownException.resolveBinding()).getQualifiedName());
//			}
			Type returnType = methodDeclaration.getReturnType2();
			if (returnType != null && (!returnType.isPrimitiveType() || ((PrimitiveType) returnType).getPrimitiveTypeCode() != PrimitiveType.VOID)) {
				userMethodInfo.setReturnType(returnType.resolveBinding().getQualifiedName());
			}
			boolean entityDataUserMethod = false;
			if (userMethodInfo.getParameterTypes().size() == 1) {
				entityDataUserMethod = isEntityDataType(interfaceType, userMethodInfo.getParameterTypes().get(0));
			}
			else if (userMethodInfo.getReturnType() != null) {
				entityDataUserMethod = isEntityDataType(interfaceType, userMethodInfo.getReturnType());
			}
			if (!entityDataUserMethod) {
				iEntityInfo.addUserMethod(userMethodInfo);
			}
		}
	}
	
	private boolean isEntityDataType(IType interfaceType, String typeName) {
		IType type = JavaUtil.resolveType(interfaceType, typeName);
		return AbstractEntityDataUtil.isAbstractEntityDataType(type);
	}
}
