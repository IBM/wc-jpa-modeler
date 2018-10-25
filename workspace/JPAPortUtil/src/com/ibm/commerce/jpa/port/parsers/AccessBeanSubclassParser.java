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
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.commerce.jpa.port.info.AccessBeanSubclassInfo;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ProjectInfo;
import com.ibm.commerce.jpa.port.util.AccessBeanUtil;
import com.ibm.commerce.jpa.port.util.JavaUtil;

public class AccessBeanSubclassParser {
	private AccessBeanSubclassInfo iAccessBeanSubclassInfo;
	private IType iType;
	private EntityInfo iEntityInfo;
	private ASTParser iASTParser;
	private ApplicationInfo iApplicationInfo;
	
	public AccessBeanSubclassParser(ASTParser astParser, AccessBeanSubclassInfo accessBeanSubclassInfo) {
		iASTParser = astParser;
		iAccessBeanSubclassInfo = accessBeanSubclassInfo;
		iType = accessBeanSubclassInfo.getType();
		iEntityInfo = accessBeanSubclassInfo.getEntityInfo();
		iApplicationInfo = accessBeanSubclassInfo.getProjectInfo().getApplicationInfo();
	}
	
	public void parse(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("parse access bean subclass " + iAccessBeanSubclassInfo.getName(), IProgressMonitor.UNKNOWN);
			parseAccessBeanSubclass(progressMonitor);
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private void parseAccessBeanSubclass(IProgressMonitor progressMonitor) throws JavaModelException {
		IType superclassType = JavaUtil.resolveType(iType, iType.getSuperclassName());
		if (!iEntityInfo.getAccessBeanInfo().getQualifiedAccessBeanName().equals(superclassType.getFullyQualifiedName('.'))) {
			ProjectInfo superclassProjectInfo = iApplicationInfo.getProjectInfo(superclassType.getJavaProject().getProject());
			AccessBeanSubclassInfo superclass = superclassProjectInfo.getAccessBeanSubclassInfo(superclassType.getFullyQualifiedName('.'), false);
			iAccessBeanSubclassInfo.setSuperclass(superclass);
			superclass.addSubclass(iAccessBeanSubclassInfo);
		}
		if (!AccessBeanUtil.isCachedEntityAccessBean(iType) && !AccessBeanUtil.isCompactEntityAccessBean(iType)) {
			iASTParser.setResolveBindings(true);
			iASTParser.setSource(iType.getCompilationUnit());
			CompilationUnit compilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
			compilationUnit.accept(new AccessBeanSubclassVisitor());
			progressMonitor.worked(100);
		}
		iApplicationInfo.incrementParsedAssetCount();
	}
	
	private class AccessBeanSubclassVisitor extends ASTVisitor {
		public boolean  visit(TypeDeclaration typeDeclaration) {
			ITypeBinding typeBinding = typeDeclaration.resolveBinding();
			if (typeBinding != null && typeBinding.getQualifiedName().equals(iAccessBeanSubclassInfo.getName())) {
				@SuppressWarnings("unchecked")
				List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
				for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
					if (bodyDeclaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
						parseMethodDeclaration((MethodDeclaration) bodyDeclaration);
					}
				}
			}
			return true;
		}
	}
	
	private void parseMethodDeclaration(MethodDeclaration methodDeclaration) {
		String methodKey = JavaUtil.getMethodKey(methodDeclaration);
		iAccessBeanSubclassInfo.setMethodDeclaration(methodKey, methodDeclaration);
	}
}
