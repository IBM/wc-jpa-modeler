package com.ibm.commerce.jpa.updaters;

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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.util.JavaUtil;

public class GeneratedTypeUpdater {
	private ASTParser iASTParser;
	private ApplicationInfo iApplicationInfo;
	private IType iType;
	private boolean iModified;
	
	public GeneratedTypeUpdater(ASTParser astParser, ApplicationInfo applicationInfo, IType type) {
		iASTParser = astParser;
		iApplicationInfo = applicationInfo;
		iType = type;
	}
	
	public void update(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("generated type update " + iType.getFullyQualifiedName('.'), IProgressMonitor.UNKNOWN);
			updateGeneratedType(progressMonitor);
		}
		finally {
			progressMonitor.done();
		}
	}
	
	public void updateGeneratedType(IProgressMonitor progressMonitor) {
		try {
			iASTParser.setProject(iType.getJavaProject());
			iASTParser.setSource(iType.getCompilationUnit());
			iASTParser.setResolveBindings(true);
			CompilationUnit astCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
			astCompilationUnit.recordModifications();
			astCompilationUnit.accept(new GeneratedTypeUpdateVisitor());
			if (iModified) {
				IFile file = (IFile) iType.getCompilationUnit().getResource();
				iApplicationInfo.getBackupUtil(iType.getJavaProject().getProject()).backupFile2(file, new SubProgressMonitor(progressMonitor, 100));
				IDocument document = JavaUtil.getDocument(iType);
				TextEdit edits = astCompilationUnit.rewrite(document, null);
				edits.apply(document);
				iType.getPackageFragment().createCompilationUnit(iType.getTypeQualifiedName() + ".java", document.get(), true, new SubProgressMonitor(progressMonitor, 100));
//				iApplicationInfo.getBackupUtil(iType.getJavaProject().getProject()).addGeneratedFile2((IFile) compilationUnit.getResource(), new SubProgressMonitor(progressMonitor, 100));
				iApplicationInfo.incrementGeneratedAssetCount();
			}
		}
		catch (BadLocationException e) {
			e.printStackTrace();
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private class GeneratedTypeUpdateVisitor extends ASTVisitor {
		public boolean visit(SimpleName simpleName) {
			if (!simpleName.isDeclaration()) {
				IBinding binding = simpleName.resolveBinding();
				if (binding != null) {
					if (binding.getKind() == ITypeBinding.TYPE) {
						ITypeBinding typeBinding = (ITypeBinding) binding;
						String qualifiedTypeName = typeBinding.getQualifiedName();
						String newTypeName = iApplicationInfo.getTypeMapping(qualifiedTypeName);
						if (newTypeName != null) {
							int index = newTypeName.lastIndexOf(".JPA");
							if (index == -1) {
								index = newTypeName.lastIndexOf(".$JPA");
							}
							if (index != -1) {
								newTypeName = newTypeName.substring(index + 1);
								if (newTypeName.indexOf('.') == -1) {
									iModified = true;
									JavaUtil.replaceASTNode(simpleName, simpleName.getAST().newName(newTypeName));
								}
							}
						}
					}
				}
			}
			return true;
		}
		
		public boolean visit(QualifiedName qualifiedName) {
			IBinding binding = qualifiedName.resolveBinding();
			if (binding != null) {
				if (binding.getKind() == ITypeBinding.TYPE) {
					ITypeBinding typeBinding = (ITypeBinding) binding;
					String qualifiedTypeName = typeBinding.getQualifiedName();
					String newTypeName = iApplicationInfo.getTypeMapping(qualifiedTypeName);
					if (newTypeName != null) {
						iModified = true;
						JavaUtil.replaceASTNode(qualifiedName, qualifiedName.getAST().newName(newTypeName));
					}
				}
			}
			return false;
		}
	}
}
