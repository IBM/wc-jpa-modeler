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

public class RemoteInterfaceParser {
	private static final String PROTECTABLE = "com.ibm.commerce.security.Protectable";
	private static final String GROUPABLE = "com.ibm.commerce.grouping.Groupable";
	private static final String COPY_HELPER = "com.ibm.ivj.ejb.runtime.CopyHelper";
	private static final String EJB_OBJECT = "javax.ejb.EJBObject";
	private static final String EJB_CREATE = "ejbCreate";
	private static final String COMMIT_COPY_HELPER = "commitCopyHelper";
	private static final String REFRESH_COPY_HELPER = "refreshCopyHelper";
	private static final Set<String> STANDARD_INTERFACES;
	private static final Set<String> STANDARD_METHODS;
	static {
		STANDARD_INTERFACES = new HashSet<String>();
		STANDARD_INTERFACES.add(PROTECTABLE);
		STANDARD_INTERFACES.add(GROUPABLE);
		STANDARD_INTERFACES.add(COPY_HELPER);
		STANDARD_INTERFACES.add(EJB_OBJECT);
		STANDARD_METHODS = new HashSet<String>();
		STANDARD_METHODS.add(EJB_CREATE);
		STANDARD_METHODS.add(COMMIT_COPY_HELPER);
		STANDARD_METHODS.add(REFRESH_COPY_HELPER);
	}
	private static final String EJS_REMOTE_CMP = "EJSRemoteCMP";
	private static final String STUB = "_Stub";
	
	private EntityInfo iEntityInfo;
	private ModuleInfo iModuleInfo;
	private ASTParser iASTParser;
	
	public RemoteInterfaceParser(ASTParser astParser, EntityInfo entityInfo) {
		iASTParser = astParser;
		iEntityInfo = entityInfo;
		iModuleInfo = entityInfo.getModuleInfo();
	}

	public void parse(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("parse remote interface for " + iEntityInfo.getEjbName(), IProgressMonitor.UNKNOWN);
			IType remoteInterface = iEntityInfo.getRemoteType();
			if (remoteInterface != null) {
				parseInterface(progressMonitor, remoteInterface);
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
		iModuleInfo.addDeleteIntendedType(interfaceType.getFullyQualifiedName('.'));
		ITypeHierarchy typeHierarchy = interfaceType.newTypeHierarchy(iModuleInfo.getJavaProject(), new SubProgressMonitor(progressMonitor, 1000));
		IType[] implementingClasses = typeHierarchy.getImplementingClasses(interfaceType);
		if (implementingClasses != null) {
			for (IType implementingClass : implementingClasses) {
				String typeName = implementingClass.getTypeQualifiedName();
				if (typeName.startsWith(EJS_REMOTE_CMP)) {
					iModuleInfo.addDeleteIntendedType(implementingClass.getFullyQualifiedName('.'));
					String tieClassName = implementingClass.getPackageFragment().getElementName() + "._" + implementingClass.getTypeQualifiedName() + "_Tie";
					IType tieClassType = JavaUtil.resolveType(implementingClass, tieClassName);
					if (tieClassType != null) {
						iModuleInfo.addDeleteIntendedType(tieClassType.getFullyQualifiedName('.'));
					}
				}
				else if (typeName.endsWith(STUB)) {
					iModuleInfo.addDeleteIntendedType(implementingClass.getFullyQualifiedName('.'));
				}
				else {
					System.out.println("unexpected remote interface implementor: "+implementingClass.getFullyQualifiedName('.'));
				}
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
		try {
			return AbstractEntityDataUtil.isAbstractEntityDataType(type);
		}
		catch (Exception e) {
			System.out.println("exception "+e+" while checking "+interfaceType+" typeName="+typeName);
			return false;
		}
	}
}
