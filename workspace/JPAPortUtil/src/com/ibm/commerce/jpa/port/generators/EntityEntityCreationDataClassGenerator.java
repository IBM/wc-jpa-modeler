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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ClassInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.JavaUtil;
import com.ibm.commerce.jpa.port.util.TargetExceptionUtil;

public class EntityEntityCreationDataClassGenerator {
	private static final String COPY_FIELDS = "copyFields";
	
	private ApplicationInfo iApplicationInfo;
	private BackupUtil iBackupUtil;
	private EntityInfo iEntityInfo;
	private ModuleInfo iModuleInfo;
	private IType iType;
	private ClassInfo iEntityEntityCreationDataClassInfo;
	private String iOldName;
	private String iNewName;
	
	public EntityEntityCreationDataClassGenerator(BackupUtil backupUtil, EntityInfo entityInfo) {
		iBackupUtil = backupUtil;
		iEntityInfo = entityInfo;
		iModuleInfo = entityInfo.getModuleInfo();
		iApplicationInfo = entityInfo.getModuleInfo().getApplicationInfo();
		iType = entityInfo.getEjbEntityCreationDataType();
		iEntityEntityCreationDataClassInfo = entityInfo.getEntityEntityCreationDataClassInfo();
	}
	
	public void generate(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("generate entity creation data class " + iEntityInfo.getEjbName(), IProgressMonitor.UNKNOWN);
			if (iType != null) {
				generateEntityEntityCreationDataType(progressMonitor);
				progressMonitor.worked(1000);
			}
		}
		finally {
			progressMonitor.done();
		}
	}
	
	public void generateEntityEntityCreationDataType(IProgressMonitor progressMonitor) {
		try {
			iOldName = iType.getTypeQualifiedName();
			iNewName = iEntityEntityCreationDataClassInfo.getClassName(); 
			CompilationUnit astCompilationUnit = iEntityInfo.getEjbEntityCreationDataCompilationUnit();
			astCompilationUnit.recordModifications();
			astCompilationUnit.accept(new EntityCreationDataPortVisitor());
			IDocument document = JavaUtil.getDocument(iType);
			TextEdit edits = astCompilationUnit.rewrite(document, null);
			edits.apply(document);
			ICompilationUnit compilationUnit = iType.getPackageFragment().createCompilationUnit(iEntityEntityCreationDataClassInfo.getClassName() + ".java", document.get(), true, new SubProgressMonitor(progressMonitor, 100));
			iBackupUtil.addGeneratedFile((IFile) compilationUnit.getResource(), new SubProgressMonitor(progressMonitor, 100));
			iApplicationInfo.incrementGeneratedAssetCount();
		}
		catch (BadLocationException e) {
			e.printStackTrace();
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private class EntityCreationDataPortVisitor extends PortVisitor {
		public EntityCreationDataPortVisitor() {
			super(iApplicationInfo, iModuleInfo.getJavaProject());
		}

		public boolean visit(SimpleName simpleName) {
			if (simpleName.getIdentifier().equals(iOldName)) {
				replaceASTNode(simpleName, simpleName.getAST().newName(iNewName));
			}
			else {
				super.visit(simpleName);
			}
			return false;
		}
		
		public boolean visit(MethodDeclaration methodDeclaration) {
			boolean visitChildren = false;
			if (methodDeclaration.getName().getIdentifier().equals(COPY_FIELDS)) {
				methodDeclaration.delete();
			}
			else {
				TargetExceptionUtil.getUnhandledTargetExceptions(iApplicationInfo, iModuleInfo.getJavaProject(), methodDeclaration.getBody());
				visitChildren = super.visit(methodDeclaration);
			}
			return visitChildren;
		}
		
		public boolean visit(Initializer initializer) {
			TargetExceptionUtil.getUnhandledTargetExceptions(iApplicationInfo, iModuleInfo.getJavaProject(), initializer.getBody());
			return super.visit(initializer);
		}
	}
}
