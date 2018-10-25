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

public class HomeInterfaceParser {
	private static final String CREATE = "create";
	private static final String FIND = "find";
	private static final String FIND_BY_PRIMARY_KEY = "findByPrimaryKey";
//	private final static String ENTITY_CREATION_DATA = "com.ibm.commerce.context.content.objects.EntityCreationData";
	private static final String EJS_REMOTE_CMP = "EJSRemoteCMP";
	private static final String STUB = "_Stub";
	
	private EntityInfo iEntityInfo;
	private ModuleInfo iModuleInfo;
	private ASTParser iASTParser;
	
	public HomeInterfaceParser(ASTParser astParser, EntityInfo entityInfo) {
		iASTParser = astParser;
		iEntityInfo = entityInfo;
		iModuleInfo = entityInfo.getModuleInfo();
	}

	public void parse(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("parse home interface for " + iEntityInfo.getEjbName(), IProgressMonitor.UNKNOWN);
			IType homeInterface = iEntityInfo.getHomeType();
			if (homeInterface != null) {
				parseInterface(progressMonitor, homeInterface);
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
						iModuleInfo.addDeleteIntendedType(tieClassName);
					}
					String homeBeanClassName = implementingClass.getFullyQualifiedName('.').replace("Home", "HomeBean").replace("EJSRemoteCMP", "EJSCMP");
					IType homeBeanClassType = JavaUtil.resolveType(implementingClass, homeBeanClassName);
					if (homeBeanClassType != null) {
						iModuleInfo.addDeleteIntendedType(homeBeanClassName);
					}
				}
				else if (typeName.endsWith(STUB)) {
					iModuleInfo.addDeleteIntendedType(implementingClass.getFullyQualifiedName('.'));
				}
				else {
					System.out.println("unexpected home interface implementor: " + implementingClass.getFullyQualifiedName('.'));
				}
			}
		}
		iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
		// _EJSRemoteCMPProduct_9d8e22c7_Tie - need to discover through naming convention - should be done in the remote interface parser
		// _EJSRemoteCMPProductHome_9d8e22c7_Tie - naming convention - do this here in home interface parser
		// EJSCMPProductHomeBean_9d8e22c7 - naming convention - do this here
		// EJSJDBCPersisterCMPProductBean_9d8e22c7 - this implements EJSFinderProductBean - what's that?
		// EJSRemoteCMPProduct_9d8e22c7 - covered - implements remote interface
		// EJSRemoteCMPProductHome_9d8e22c7 - covered - implements home interface
		
		// _EJSRemoteCMPContextData_e33d6509_Tie
		// _EJSRemoteCMPContextDataHome_e33d6509_Stub - this will be caught because it implements ContextDataHome
		// _EJSRemoteCMPContextDataHome_e33d6509_Tie
		// EJSCMPContextDataHomeBean_e33d6509 - no interface - need to use naming convention
		// EJSLocalCMPContextData_e33d6509 - implements local interface
		// EJSLocalCMPContextDatatHome_e33d6509 - implements local home interface
		// EJSRemoteCMPContextData_e33d6509 implements remote interface
		// EJSRemoteCMPContextDataHome_e33d6509 - implements remote home interface
		
		// EJSCMPEDPOrderHomeBean_9a2637b5 - naming convention - local home seems to have the same convention as remote home
		// EJSLocalCMPEDPOrder_9a2637b5 - implements local interface
		// EJSLocalCMPEDPOrderHome_9a2637b5 - implements local home interface
		// ConcreteEDPOrder_9a2637b5 - extends the bean (or rather implements the abstract bean)
		
		// EJSFinderProductBean - is this named after the ejbclass? should be parsed there by naming convention - it is an interface

		// there are directories under websphere_deploy that must be skipped
		// there is code under com.ibm.ws.ejbdeploy.J(project name).DB2UDBNT_V82_1
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
//			boolean entityCreationDataCreator = false;
			if (parameters.size() == 1) {
				entityDataCreator = isEntityDataType(interfaceType, parameterTypes[0]);
//				entityCreationDataCreator = isEntityCreationDataType(parameterTypes[0]);
			}
			if (!entityDataCreator /* && !entityCreationDataCreator */ && parameters.size() > 0) {
				iEntityInfo.getCreatorInfo(parameterTypes, true);
			}
		}
	}
	
	private boolean isEntityDataType(IType interfaceType, String typeName) {
		IType type = JavaUtil.resolveType(interfaceType, typeName);
		return AbstractEntityDataUtil.isAbstractEntityDataType(type);
	}
	
//	private boolean isEntityCreationDataType(String typeName) {
//		return ENTITY_CREATION_DATA.equals(typeName); 
//	}
}
