package com.ibm.commerce.jpa.port.search;

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

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ProjectInfo;
import com.ibm.commerce.jpa.port.util.FinderResultCacheUtil;

public class EntitySearchUtil2 {
	private ApplicationInfo iApplicationInfo;
	private IWorkspace iWorkspace;
	private ProjectInfo iProjectInfo;
	private IJavaProject iJavaProject;
	private ASTParser iASTParser = ASTParser.newParser(AST.JLS3);
	private static Collection<String> EXEMPT_REFERENCES;
	static {
		EXEMPT_REFERENCES = new HashSet<String>();
		EXEMPT_REFERENCES.add("com.ibm.ivj.ejb.runtime.AbstractAccessBean");
		EXEMPT_REFERENCES.add("com.ibm.commerce.contract.objects.ContractJDBCHelperAccessBean");
		EXEMPT_REFERENCES.add("com.ibm.commerce.registry.StoreRegistry");
	}
	
	public EntitySearchUtil2(ApplicationInfo applicationInfo, IWorkspace workspace, ProjectInfo projectInfo) {
		iApplicationInfo = applicationInfo;
		iWorkspace = workspace;
		iProjectInfo = projectInfo;
		iJavaProject = projectInfo.getJavaProject();
	}
	
	public IStatus search(IProgressMonitor progressMonitor) {
		IStatus status = Status.OK_STATUS;
		try {
			progressMonitor.beginTask("search for entity references in " + iProjectInfo.getJavaProject().getElementName(), IProgressMonitor.UNKNOWN);
			long start = new java.util.Date().getTime();
			System.out.println("searching for entity references in "+ iProjectInfo.getJavaProject().getElementName());
			if(isJavaProject(iJavaProject)) {
				IClasspathEntry[] classpathEntries = iJavaProject.getResolvedClasspath(true);
				for (IClasspathEntry entry : classpathEntries) {
					if (progressMonitor.isCanceled()) {
						status = Status.CANCEL_STATUS;
						break;
					}
					if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						if(entry.getPath().segmentCount() > 1) {
							IFolder sourceFolder = iWorkspace.getRoot().getFolder(entry.getPath());
							if(sourceFolder.exists()) {
								status = searchFolder(sourceFolder, new SubProgressMonitor(progressMonitor, 100));
							}
						}
						if (status == Status.CANCEL_STATUS) {
							break;
						}
					}
				}
			}
			System.out.println("end search in " + iProjectInfo.getJavaProject().getElementName() + " " + ((new java.util.Date().getTime() - start)/1000)+" seconds");
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
		return status;
	}
		
	private boolean isJavaProject(IJavaProject iJavaProject2) throws CoreException {
		boolean isJP = false;
		String[] natures = iJavaProject2.getProject().getDescription().getNatureIds();
		for(String nature: natures) {
			if(nature != null && nature.equals("org.eclipse.jdt.core.javanature")) {
				isJP = true;
			}
		}
		return isJP;
	}

	private IStatus searchFolder(IFolder folder, IProgressMonitor progressMonitor) {
		IStatus status = Status.OK_STATUS;
		try {
			IResource[] members = folder.members();
			for (IResource member : members) {
				if (progressMonitor.isCanceled()) {
					status = Status.CANCEL_STATUS;
					break;
				}
				if (member.getType() == IResource.FILE) {
					searchFile((IFile) member, progressMonitor);
				}
				else if (member.getType() == IResource.FOLDER) {
					status = searchFolder((IFolder) member, progressMonitor);
					if (status == Status.CANCEL_STATUS) {
						break;
					}
				}
			}
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		return status;
	}

	private void searchFile(IFile file, IProgressMonitor progressMonitor) {
		if (file.getName().endsWith(".java")) {
			ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(file);
			try {
				if (compilationUnit.getTypes().length > 0) {
					IType type = compilationUnit.getTypes()[0];
					String qualifiedTypeName = type.getFullyQualifiedName('.');
					if (!iApplicationInfo.isDeleteIntendedType(qualifiedTypeName) && !iApplicationInfo.isAccessBeanSubclass(qualifiedTypeName) && !qualifiedTypeName.contains("JPA")) {
						iASTParser.setResolveBindings(true);
						iASTParser.setSource(compilationUnit);
						CompilationUnit astCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
						SearchVisitor searchVisitor = new SearchVisitor();
						astCompilationUnit.accept(searchVisitor);
						if (searchVisitor.getContainsEntityReference()) {
							iProjectInfo.addEntityReferencingType(compilationUnit.getTypes()[0].getFullyQualifiedName('.'));
							iApplicationInfo.incrementSearchResultCount();
						}
					}
				}
			}
			catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class SearchVisitor extends ASTVisitor {
		private boolean iContainsEntityReference;
		
		public boolean getContainsEntityReference() {
			return iContainsEntityReference;
		}
		
		public boolean visit(SimpleType simpleType) {
			ITypeBinding typeBinding = simpleType.resolveBinding();
			if (typeBinding != null) {
				String qualifiedTypeName = typeBinding.getQualifiedName();
				checkQualifiedTypeName(qualifiedTypeName);
			}
			return false;
		}
		
		public boolean visit(ParameterizedType parameterizedType) {
			ITypeBinding typeBinding = parameterizedType.resolveBinding();
			if (typeBinding != null) {
				String qualifiedTypeName = typeBinding.getTypeDeclaration().getQualifiedName();
				checkQualifiedTypeName(qualifiedTypeName);
			}
			return !iContainsEntityReference;
		}
		
		public boolean visit(SimpleName simpleName) {
			if (!simpleName.isDeclaration()) {
				IBinding binding = simpleName.resolveBinding();
				if (binding != null) {
					if (binding.getKind() == ITypeBinding.TYPE) {
						ITypeBinding typeBinding = (ITypeBinding) binding;
						String qualifiedTypeName = typeBinding.getQualifiedName();
						checkQualifiedTypeName(qualifiedTypeName);
					}
				}
			}
			return false;
		}
		
		public boolean visit(QualifiedName qualifiedName) {
			IBinding binding = qualifiedName.resolveBinding();
			if (binding != null) {
				if (binding.getKind() == ITypeBinding.TYPE) {
					ITypeBinding typeBinding = (ITypeBinding) binding;
					String qualifiedTypeName = typeBinding.getQualifiedName();
					checkQualifiedTypeName(qualifiedTypeName);
				}
			}
			if (!iContainsEntityReference) {
				ITypeBinding typeBinding = qualifiedName.getQualifier().resolveTypeBinding();
				if (typeBinding != null) {
					String qualifiedTypeName = typeBinding.getQualifiedName();
					checkQualifiedTypeName(qualifiedTypeName);
				}
			}
			if (!iContainsEntityReference) {
				qualifiedName.getQualifier().accept(this);
			}
			return !iContainsEntityReference;
		}
		
		public boolean visit(MethodInvocation methodInvocation) {
			IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
			if (methodBinding != null && methodBinding.getDeclaringClass() != null) {
				ITypeBinding typeBinding = methodBinding.getDeclaringClass();
				if (typeBinding != null) {
					if (FinderResultCacheUtil.isFinderResultCacheUtil(typeBinding)) {
						iContainsEntityReference = true;
					}
					else {
						String qualifiedTypeName = typeBinding.getQualifiedName();
						checkQualifiedTypeName(qualifiedTypeName);
					}
				}
			}
			return !iContainsEntityReference;
		}
		
		public boolean visit(FieldAccess fieldAccess) {
			if (fieldAccess.getExpression() != null) {
				ITypeBinding typeBinding = fieldAccess.getExpression().resolveTypeBinding();
				if (typeBinding != null) {
					String qualifiedTypeName = typeBinding.getQualifiedName();
					checkQualifiedTypeName(qualifiedTypeName);
				}
			}
			return !iContainsEntityReference;
		}
		
		private void checkQualifiedTypeName(String qualifiedTypeName) {
			if ((iApplicationInfo.getTypeMapping(qualifiedTypeName) != null && !EXEMPT_REFERENCES.contains(qualifiedTypeName)) ||
					iApplicationInfo.isDeleteIntendedType(qualifiedTypeName) ||
					iApplicationInfo.isAccessBeanSubclass(qualifiedTypeName)) {
//				System.out.println(qualifiedTypeName + " mapping: "+iApplicationInfo.getTypeMapping(qualifiedTypeName) + " " + iApplicationInfo.isDeleteIntendedType(qualifiedTypeName)+ " "+iApplicationInfo.isAccessBeanSubclass(qualifiedTypeName));
				iContainsEntityReference = true;
			}
		}
	}
}
