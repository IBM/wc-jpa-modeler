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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ClassInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.ImportUtil;
import com.ibm.commerce.jpa.port.util.JavaUtil;
import com.ibm.commerce.jpa.port.util.TargetExceptionUtil;

public class EntityAccessHelperClassGenerator {
	private static final String COPYRIGHT_FIELD = "COPYRIGHT";
	
	private ASTParser iASTParser;
	private BackupUtil iBackupUtil;
	private EntityInfo iEntityInfo;
	private ModuleInfo iModuleInfo;
	private ApplicationInfo iApplicationInfo;
	private ClassInfo iEntityAccessHelperClassInfo;
	private TypeDeclaration iEntityAccessHelperTypeDeclaration;
	
	public EntityAccessHelperClassGenerator(ASTParser astParser, BackupUtil backupUtil, EntityInfo entityInfo) {
		iASTParser = astParser;
		iBackupUtil = backupUtil;
		iEntityInfo = entityInfo;
		iModuleInfo = entityInfo.getModuleInfo();
		iApplicationInfo = entityInfo.getModuleInfo().getApplicationInfo();
		iEntityAccessHelperClassInfo = entityInfo.getEntityAccessHelperClassInfo();
	}

	public void generate(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("generate entity access helper for " + iEntityInfo.getEjbName(), 1000);
			if (iEntityAccessHelperClassInfo != null) {
				try {
					iASTParser.setSource(iEntityInfo.getEjbAccessHelperType().getCompilationUnit());
					iASTParser.setResolveBindings(true);
					CompilationUnit ejbAccessHelperCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
					StringBuilder sb = new StringBuilder();
					sb.append("package ");
					sb.append(iEntityAccessHelperClassInfo.getPackageFragment().getElementName());
					sb.append(";\r\n");
					JavaUtil.appendCopyrightComment(sb);
					@SuppressWarnings("unchecked")
					List<ImportDeclaration> importDeclarations = ejbAccessHelperCompilationUnit.imports();
					ImportUtil.appendImports(importDeclarations, sb);
					sb.append("\r\npublic class ");
					sb.append(iEntityAccessHelperClassInfo.getClassName());
					sb.append(" extends ");
					if (iEntityInfo.getSupertype() != null && iEntityInfo.getSupertype().getEntityAccessHelperClassInfo() != null) {
						sb.append(iEntityInfo.getSupertype().getEntityAccessHelperClassInfo().getQualifiedClassName());
					}
					else {
						sb.append("com.ibm.commerce.security.AccessHelper");
					}
					sb.append(" {\r\n");
					JavaUtil.appendCopyrightField(sb);
					sb.append("\r\n\tpublic ");
					sb.append(iEntityAccessHelperClassInfo.getClassName());
					sb.append("(){\r\n\t}\r\n}");
					String source = sb.toString();
					IDocument document = new Document(source);
					iASTParser.setProject(iModuleInfo.getJavaProject());
					iASTParser.setResolveBindings(false);
					iASTParser.setSource(document.get().toCharArray());
					CompilationUnit entityAccessHelperCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
					iEntityAccessHelperTypeDeclaration = (TypeDeclaration) entityAccessHelperCompilationUnit.types().get(0);
					entityAccessHelperCompilationUnit.recordModifications();
					portAccessHelperTypeDeclaration((TypeDeclaration) ejbAccessHelperCompilationUnit.types().get(0));
					TextEdit edits = entityAccessHelperCompilationUnit.rewrite(document, null);
					edits.apply(document);
					source = document.get();
					ICompilationUnit compilationUnit = iEntityAccessHelperClassInfo.getPackageFragment().createCompilationUnit(iEntityAccessHelperClassInfo.getClassName() + ".java", source, true, new SubProgressMonitor(progressMonitor, 100));
//					ImportUtil.resolveImports(iASTParser, compilationUnit, new SubProgressMonitor(progressMonitor, 100));
					iBackupUtil.addGeneratedFile((IFile) compilationUnit.getResource(), new SubProgressMonitor(progressMonitor, 100));
					iApplicationInfo.incrementGeneratedAssetCount();
				}
				catch (CoreException e) {
					e.printStackTrace();
				}
				catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
			progressMonitor.worked(500);
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private void portAccessHelperTypeDeclaration(TypeDeclaration ejbAccessHelperTypeDeclaration) {
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> ejbBodyDeclarations = ejbAccessHelperTypeDeclaration.bodyDeclarations();
		for (BodyDeclaration ejbBodyDeclaration : ejbBodyDeclarations) {
			switch (ejbBodyDeclaration.getNodeType()) {
				case ASTNode.FIELD_DECLARATION: {
					portFieldDeclaration((FieldDeclaration) ejbBodyDeclaration);
					break;
				}
				case ASTNode.METHOD_DECLARATION: {
					portMethodDeclaration((MethodDeclaration) ejbBodyDeclaration);
					break;
				}
			}
		}
	}
	
	private void portFieldDeclaration(FieldDeclaration fieldDeclaration) {
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> variableDeclarationFragments = fieldDeclaration.fragments();
		for (VariableDeclarationFragment variableDeclarationFragment : variableDeclarationFragments) {
			String fieldName = variableDeclarationFragment.getName().getIdentifier();
			if (!fieldName.equals(COPYRIGHT_FIELD)) {
				fieldDeclaration.accept(new PortAccessHelperVisitor());
				FieldDeclaration entityFieldDeclaration = (FieldDeclaration) ASTNode.copySubtree(iEntityAccessHelperTypeDeclaration.getAST(), fieldDeclaration);
				int index = 0;
				@SuppressWarnings("unchecked")
				List<BodyDeclaration> entityBodyDeclarations = iEntityAccessHelperTypeDeclaration.bodyDeclarations();
				for (BodyDeclaration bodyDeclaration : entityBodyDeclarations) {
					if (bodyDeclaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
						break;
					}
					index++;
				}
				entityBodyDeclarations.add(index, entityFieldDeclaration);
			}
		}
	}
	
	private void portMethodDeclaration(MethodDeclaration methodDeclaration) {
		if (!methodDeclaration.isConstructor()) {
			TargetExceptionUtil.getUnhandledTargetExceptions(iApplicationInfo, iModuleInfo.getJavaProject(), methodDeclaration.getBody());
			methodDeclaration.accept(new PortAccessHelperVisitor());
			MethodDeclaration entityMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(iEntityAccessHelperTypeDeclaration.getAST(), methodDeclaration);
			@SuppressWarnings("unchecked")
			List<BodyDeclaration> entityBodyDeclarations = iEntityAccessHelperTypeDeclaration.bodyDeclarations();
			entityBodyDeclarations.add(entityMethodDeclaration);
		}
	}
	
	private class PortAccessHelperVisitor extends PortVisitor {
		public PortAccessHelperVisitor() {
			super(iApplicationInfo, iModuleInfo.getJavaProject());
		}
	}
}
