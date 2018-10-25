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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;

import com.ibm.commerce.jpa.port.info.ClassInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.KeyClassConstructorInfo;
import com.ibm.commerce.jpa.port.info.RelatedEntityInfo;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.JavaUtil;

public class EntityKeyClassGenerator {
	private static Map<String, String> PRIMITIVE_TYPE_MAP;
	private final static String INT_PRIMITIVE = "int";
	private final static String INTEGER_WRAPPER = "java.lang.Integer";
	private final static String LONG_PRIMITIVE = "long";
	private final static String LONG_WRAPPER = "java.lang.Long";
	static {
		PRIMITIVE_TYPE_MAP = new HashMap<String, String>();
		PRIMITIVE_TYPE_MAP.put(INT_PRIMITIVE, INTEGER_WRAPPER);
		PRIMITIVE_TYPE_MAP.put(LONG_PRIMITIVE, LONG_WRAPPER);
	}
	
	private BackupUtil iBackupUtil;
	private EntityInfo iEntityInfo;
	private ClassInfo iEntityKeyClassInfo;
//	private CompilationUnit iEntityKeyCompilationUnit;
	
	public EntityKeyClassGenerator(BackupUtil backupUtil, EntityInfo entityInfo) {
		iBackupUtil = backupUtil;
		iEntityInfo = entityInfo;
		iEntityKeyClassInfo = entityInfo.getEntityKeyClassInfo();
	}

	public void generate(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("generate key class for " + iEntityInfo.getEjbName(), IProgressMonitor.UNKNOWN);
			if (iEntityKeyClassInfo != null && (iEntityInfo.getSupertype() == null || !iEntityInfo.getPrimaryKeyClass().equals(iEntityInfo.getSupertype().getPrimaryKeyClass()))) {
				try {
	//				iASTParser.setSource(iEntityInfo.getPrimaryKeyType().getCompilationUnit());
	//				iASTParser.setResolveBindings(true);
	//				CompilationUnit primaryKeyCompilationUnit = (CompilationUnit) iASTParser.createAST(null);
					StringBuilder sb = new StringBuilder();
					sb.append("package ");
					sb.append(iEntityKeyClassInfo.getPackageFragment().getElementName());
					sb.append(";\r\n");
					JavaUtil.appendCopyrightComment(sb);
					sb.append("\r\nimport java.io.Serializable;\r\n");
	//				@SuppressWarnings("unchecked")
	//				List<ImportDeclaration> importDeclarations = primaryKeyCompilationUnit.imports();
	//				ImportUtil.appendImports(importDeclarations, sb);
					sb.append("\r\npublic class ");
					sb.append(iEntityKeyClassInfo.getClassName());
					sb.append(" implements Serializable {\r\n");
					JavaUtil.appendCopyrightField(sb);
					appendFieldDeclarations(sb);
					appendConstructors(sb);
//					sb.append("\r\n\tpublic ");
//					sb.append(iEntityKeyClassInfo.getClassName());
//					sb.append("(){\r\n\t}\r\n");
	//				IDocument document = new Document(sb.toString());
	//				iASTParser.setProject(iEntityInfo.getModuleInfo().getJavaProject());
	//				iASTParser.setResolveBindings(false);
	//				iASTParser.setSource(document.get().toCharArray());
	//				iEntityKeyCompilationUnit = (CompilationUnit) iASTParser.createAST(null);
	//				iEntityKeyCompilationUnit.recordModifications();
	//				portPrimaryKeyTypeDeclaration((TypeDeclaration) primaryKeyCompilationUnit.types().get(0));
	//				TextEdit edits = iEntityKeyCompilationUnit.rewrite(document, null);
	//				edits.apply(document);
	//				String documentString = document.get();
	//				sb = new StringBuilder(documentString.subSequence(0, documentString.lastIndexOf("}")));
					progressMonitor.worked(100);
					appendGettersAndSetters(sb);
					progressMonitor.worked(100);
					appendEqualsMethod(sb);
					progressMonitor.worked(100);
					appendHashCodeMethod(sb);
					progressMonitor.worked(100);
					sb.append("}");
					ICompilationUnit compilationUnit = iEntityKeyClassInfo.getPackageFragment().createCompilationUnit(iEntityKeyClassInfo.getClassName() + ".java", sb.toString(), true, new SubProgressMonitor(progressMonitor, 100));
//					ImportUtil.resolveImports(iASTParser, compilationUnit, new SubProgressMonitor(progressMonitor, 100));
					iBackupUtil.addGeneratedFile((IFile) compilationUnit.getResource(), new SubProgressMonitor(progressMonitor, 100));
					iEntityInfo.getModuleInfo().getApplicationInfo().incrementGeneratedAssetCount();
				}
				catch (CoreException e) {
					e.printStackTrace();
				}
	//			catch (BadLocationException e) {
	//				e.printStackTrace();
	//			}
			}
		}
		finally {
			progressMonitor.done();
		}
	}
	
//	private void portPrimaryKeyTypeDeclaration(TypeDeclaration typeDeclaration) {
//		@SuppressWarnings("unchecked")
//		List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
//		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
//			switch (bodyDeclaration.getNodeType()) {
//			case ASTNode.FIELD_DECLARATION:
//				portPrimaryKeyFieldDeclaration((FieldDeclaration) bodyDeclaration);
//				break;
//			default:
//				//System.out.println("primary key bodyDeclaration: "+bodyDeclaration);
//				break;	
//			}
//		}
//	}
	
//	private void portPrimaryKeyFieldDeclaration(FieldDeclaration fieldDeclaration) {
//		if (!JavaUtil.isStaticField(fieldDeclaration)) {
//			@SuppressWarnings("unchecked")
//			List<BodyDeclaration> bodyDeclarations = ((TypeDeclaration) iEntityKeyCompilationUnit.types().get(0)).bodyDeclarations();
//			@SuppressWarnings("unchecked")
//			List<VariableDeclarationFragment> primaryKeyVariableDeclarationFragments = fieldDeclaration.fragments();
//			for (VariableDeclarationFragment primaryKeyVariableDeclarationFragment : primaryKeyVariableDeclarationFragments) {
//				String fieldName = primaryKeyVariableDeclarationFragment.getName().getIdentifier();
//				FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
//				if (fieldInfo != null) {
//					FieldDeclaration entityFieldDeclaration = (FieldDeclaration) ASTNode.copySubtree(iEntityKeyCompilationUnit.getAST(), fieldDeclaration);
//					JavaUtil.setFieldPrivate(entityFieldDeclaration);
//					entityFieldDeclaration.setJavadoc(null);
//					bodyDeclarations.add(entityFieldDeclaration);
//				}
//			}
//		}
//	}
	
	private void appendConstructors(StringBuilder sb) {
		sb.append("\r\n\tpublic ");
		sb.append(iEntityKeyClassInfo.getClassName());
		sb.append("(){\r\n\t}\r\n");
		List<KeyClassConstructorInfo> constructors = iEntityInfo.getKeyClassConstructors();
		for (KeyClassConstructorInfo constructorInfo : constructors) {
			List<FieldInfo> fields = constructorInfo.getFields();
			if (fields.size() > 0) {
				sb.append("\r\n\tpublic ");
				sb.append(iEntityKeyClassInfo.getClassName());
				sb.append("(");
				Set<RelatedEntityInfo> processedRelatedEntities = new HashSet<RelatedEntityInfo>();
				boolean first = true;
				for (FieldInfo fieldInfo : fields) {
					RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
					if (relatedEntityInfo == null) {
						if (first) {
							first = false;
						}
						else {
							sb.append(", ");
						}
						sb.append(fieldInfo.getTypeName());
						sb.append(" ");
						sb.append(fieldInfo.getTargetFieldName());
					}
					else if (!processedRelatedEntities.contains(relatedEntityInfo)) {
						processedRelatedEntities.add(relatedEntityInfo);
						if (first) {
							first = false;
						}
						else {
							sb.append(", ");
						}
						sb.append(relatedEntityInfo.getKeyFieldType());
						sb.append(" ");
						sb.append(relatedEntityInfo.getFieldName());
					}
				}
				sb.append(") {");
				processedRelatedEntities.clear();
				for (FieldInfo fieldInfo : fields) {
					RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
					if (relatedEntityInfo == null) {
						sb.append("\r\n\t\tthis.");
						sb.append(fieldInfo.getTargetFieldName());
						sb.append(" = ");
						sb.append(fieldInfo.getTargetFieldName());
						sb.append(";");
					}
					else if (!processedRelatedEntities.contains(relatedEntityInfo)) {
						processedRelatedEntities.add(relatedEntityInfo);
						sb.append("\r\n\t\tthis.");
						sb.append(relatedEntityInfo.getFieldName());
						sb.append(" = ");
						sb.append(relatedEntityInfo.getFieldName());
						sb.append(";");
					}
				}
				sb.append("\r\n\t}\r\n");
			}
		}
	}
	
	private void appendFieldDeclarations(StringBuilder sb) {
		List<FieldInfo> keyFields = iEntityInfo.getKeyFields();
		for (FieldInfo keyField : keyFields) {
			if (keyField.getRelatedEntityInfo() == null) {
				sb.append("\r\n\tprivate ");
				sb.append(keyField.getTypeName());
				sb.append(" ");
				sb.append(keyField.getTargetFieldName());
				sb.append(";\r\n");
			}
		}
		List<RelatedEntityInfo> keyRelatedEntities = iEntityInfo.getKeyRelatedEntities();
		for (RelatedEntityInfo relatedEntityInfo : keyRelatedEntities) {
			sb.append("\r\n\tprivate ");
			sb.append(relatedEntityInfo.getKeyFieldType());
			sb.append(" ");
			sb.append(relatedEntityInfo.getFieldName());
			sb.append(";\r\n");
		}
	}
	
	private void appendGettersAndSetters(StringBuilder sb) {
		List<FieldInfo> keyFields = iEntityInfo.getKeyFields();
		for (FieldInfo keyField : keyFields) {
			if (keyField.getRelatedEntityInfo() == null) {
				sb.append("\r\n\tpublic ");
				sb.append(keyField.getTypeName());
				sb.append(" ");
				sb.append(keyField.getTargetGetterName());
				sb.append("() {\r\n\t\treturn ");
				sb.append(keyField.getTargetFieldName());
				sb.append(";\r\n\t}\r\n");
				sb.append("\r\n\tpublic void ");
				sb.append(keyField.getTargetSetterName());
				sb.append("(");
				sb.append(keyField.getTypeName());
				sb.append(" ");
				sb.append(keyField.getTargetFieldName());
				sb.append(") {\r\n\t\tthis.");
				sb.append(keyField.getTargetFieldName());
				sb.append(" = ");
				sb.append(keyField.getTargetFieldName());
				sb.append(";\r\n\t}\r\n");
			}
		}
		List<RelatedEntityInfo> keyRelatedEntities = iEntityInfo.getKeyRelatedEntities();
		for (RelatedEntityInfo relatedEntityInfo : keyRelatedEntities) {
			sb.append("\r\n\tpublic ");
			sb.append(relatedEntityInfo.getKeyFieldType());
			sb.append(" ");
			sb.append(relatedEntityInfo.getGetterName());
			sb.append("() {\r\n\t\treturn ");
			sb.append(relatedEntityInfo.getFieldName());
			sb.append(";\r\n\t}\r\n");
			
			sb.append("\r\n\tpublic void ");
			sb.append(relatedEntityInfo.getSetterName());
			sb.append("(");
			sb.append(relatedEntityInfo.getKeyFieldType());
			sb.append(" ");
			sb.append(relatedEntityInfo.getFieldName());
			sb.append(") {\r\n\t\tthis.");
			sb.append(relatedEntityInfo.getFieldName());
			sb.append(" = ");
			sb.append(relatedEntityInfo.getFieldName());
			sb.append(";\r\n\t}\r\n");
		}
	}
	
	private void appendEqualsMethod(StringBuilder sb) {
		sb.append("\r\n\tpublic boolean equals(Object o) {\r\n");
		sb.append("\t\tboolean result = this == o;\r\n");
		sb.append("\t\tif (!result && o instanceof ");
		sb.append(iEntityKeyClassInfo.getClassName());
		sb.append(") {\r\n");
		sb.append("\t\t\t");
		sb.append(iEntityKeyClassInfo.getClassName());
		sb.append(" otherKey = (");
		sb.append(iEntityKeyClassInfo.getClassName());
		sb.append(") o;\r\n");
		sb.append("\t\t\tresult = ");
		boolean firstField = true;
		List<FieldInfo> keyFields = iEntityInfo.getKeyFields();
		for (FieldInfo keyField : keyFields) {
			if (keyField.getRelatedEntityInfo() == null) {
				if (firstField) {
					firstField = false;
				}
				else {
					sb.append(" &&\r\n\t\t\t\t");
				}
				sb.append("(");
				sb.append(keyField.getTargetFieldName());
				sb.append(" == otherKey.");
				sb.append(keyField.getTargetFieldName());
				if (!PRIMITIVE_TYPE_MAP.containsKey(keyField.getTypeName())) {
					sb.append(" || ");
					sb.append(keyField.getTargetFieldName());
					sb.append(" != null && ");
					sb.append(keyField.getTargetFieldName());
					sb.append(".equals(otherKey.");
					sb.append(keyField.getTargetFieldName());
					sb.append(")");
				}
				sb.append(")");
			}
		}
		List<RelatedEntityInfo> keyRelatedEntities = iEntityInfo.getKeyRelatedEntities();
		for (RelatedEntityInfo relatedEntityInfo : keyRelatedEntities) {
			if (firstField) {
				firstField = false;
			}
			else {
				sb.append(" &&\r\n\t\t\t\t");
			}
			sb.append("(");
			sb.append(relatedEntityInfo.getFieldName());
			sb.append(" == otherKey.");
			sb.append(relatedEntityInfo.getFieldName());
			if (!PRIMITIVE_TYPE_MAP.containsKey(relatedEntityInfo.getKeyFieldType())) {
				sb.append(" || ");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(" != null && ");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(".equals(otherKey.");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(")");
			}
			sb.append(")");
		}
		sb.append(";\r\n\t\t}\r\n");
		sb.append("\t\treturn result;\r\n\t}\r\n");
	}
	
	private void appendHashCodeMethod(StringBuilder sb) {
		sb.append("\r\n\tpublic int hashCode() {\r\n");
		boolean firstField = true;
		List<FieldInfo> keyFields = iEntityInfo.getKeyFields();
		for (FieldInfo keyField : keyFields) {
			if (keyField.getRelatedEntityInfo() == null) {
				if (firstField) {
					sb.append("\t\tint hashCode = (");
					firstField = false;
				}
				else {
					sb.append("\t\thashCode = hashCode * 31 + (");
				}
				if (PRIMITIVE_TYPE_MAP.containsKey(keyField.getTypeName())) {
					sb.append("new ");
					sb.append(PRIMITIVE_TYPE_MAP.get(keyField.getTypeName()));
					sb.append("(");
					sb.append(keyField.getTargetFieldName());
					sb.append(")");
				}
				else {
					sb.append(keyField.getTargetFieldName());
					sb.append(" == null ? 0 : ");
					sb.append(keyField.getTargetFieldName());
				}
				sb.append(".hashCode());\r\n");
			}
		}
		List<RelatedEntityInfo> keyRelatedEntities = iEntityInfo.getKeyRelatedEntities();
		for (RelatedEntityInfo relatedEntityInfo : keyRelatedEntities) {
			if (firstField) {
				sb.append("\t\tint hashCode = (");
				firstField = false;
			}
			else {
				sb.append("\t\thashCode = hashCode * 31 + (");
			}
			if (PRIMITIVE_TYPE_MAP.containsKey(relatedEntityInfo.getKeyFieldType())) {
				sb.append("new ");
				sb.append(PRIMITIVE_TYPE_MAP.get(relatedEntityInfo.getKeyFieldType()));
				sb.append("(");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(")");
			}
			else {
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(" == null ? 0 : ");
				sb.append(relatedEntityInfo.getFieldName());
			}
			sb.append(".hashCode());\r\n");
		}

		sb.append("\t\treturn hashCode;\r\n\t}\r\n");
	}
}
