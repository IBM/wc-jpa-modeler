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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import com.ibm.commerce.jpa.port.info.AccessBeanInfo.NullConstructorParameter;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ClassInfo;
import com.ibm.commerce.jpa.port.info.ColumnInfo;
import com.ibm.commerce.jpa.port.info.EjbRelationshipRoleInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.FinderInfo;
import com.ibm.commerce.jpa.port.info.ForeignKeyInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.RelatedEntityInfo;
import com.ibm.commerce.jpa.port.info.TableInfo;
import com.ibm.commerce.jpa.port.info.TargetExceptionInfo;
import com.ibm.commerce.jpa.port.info.UserMethodInfo;
import com.ibm.commerce.jpa.port.util.AccessBeanUtil;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.ImportUtil;
import com.ibm.commerce.jpa.port.util.JavaUtil;
import com.ibm.commerce.jpa.port.util.PrimaryKeyUtil;
import com.ibm.commerce.jpa.port.util.TargetExceptionUtil;

@SuppressWarnings("deprecation")
public class EntityClassGenerator {
	private static final String BLOB = "BLOB";
	private static final String CLOB = "CLOB";
	private static final String CHAR = "CHAR";
	private static final String VARCHAR = "VARCHAR";
	private static final String INTEGER = "INTEGER";
	private static final String OPT_COUNTER = "optCounter";
	private static final String STRING = "java.lang.String";
	private static final String SET_INIT_KEY = "setInitKey_";
	private static final String INSTANTIATE_ENTITY = "instantiateEntity";
	private static final String TRIM = "trim";
//	private static final String GET_LINKS_METHOD = "_getLinks";
//	private static final String INIT_LINKS_METHOD = "_initLinks";
//	private static final String REMOVE_LINKS_METHOD = "_removeLinks";
//	private static final String EJB_ACTIVATE_METHOD = "ejbActivate";
//	private static final String EJB_CREATE = "ejbCreate";
//	private static final String EJB_LOAD = "ejbLoad";
//	private static final String EJB_PASSIVATE = "ejbPassivate";
//	private static final String EJB_POST_CREATE = "ejbPostCreate";
//	private static final String EJB_REMOVE = "ejbRemove";
//	private static final String EJB_STORE = "ejbStore";
//	private static final String COPY_FROM_EJB = "_copyFromEJB";
//	private static final String COPY_TO_EJB = "_copyToEJB";
//	private static final Set<String> STANDARD_EJB_METHODS;
//	static {
//		STANDARD_EJB_METHODS = new HashSet<String>();
//		STANDARD_EJB_METHODS.add(GET_LINKS_METHOD);
//		STANDARD_EJB_METHODS.add(INIT_LINKS_METHOD);
//		STANDARD_EJB_METHODS.add(REMOVE_LINKS_METHOD);
//		STANDARD_EJB_METHODS.add(EJB_ACTIVATE_METHOD);
//		STANDARD_EJB_METHODS.add(EJB_CREATE);
//		STANDARD_EJB_METHODS.add(EJB_LOAD);
//		STANDARD_EJB_METHODS.add(EJB_PASSIVATE);
//		STANDARD_EJB_METHODS.add(EJB_POST_CREATE);
//		STANDARD_EJB_METHODS.add(EJB_REMOVE);
//		STANDARD_EJB_METHODS.add(EJB_STORE);
//		STANDARD_EJB_METHODS.add(COPY_FROM_EJB);
//		STANDARD_EJB_METHODS.add(COPY_TO_EJB);
//	}
	private static final Map<String, List<MethodGenerator>> METHOD_GENERATORS_MAP;
	static {
		METHOD_GENERATORS_MAP = new HashMap<String, List<MethodGenerator>>();
		List<MethodGenerator> methodGenerators = new ArrayList<MethodGenerator>();
		methodGenerators.add(new CatalogEntryEntityBaseGenerator.GetTypeMethodGenerator());
		METHOD_GENERATORS_MAP.put(CatalogEntryEntityBaseGenerator.TYPE_NAME, methodGenerators);
		methodGenerators = new ArrayList<MethodGenerator>();
		methodGenerators.add(new MemberEntityBaseGenerator.GetTypeMethodGenerator());
		METHOD_GENERATORS_MAP.put(MemberEntityBaseGenerator.TYPE_NAME, methodGenerators);
		methodGenerators = new ArrayList<MethodGenerator>();
		methodGenerators.add(new MemberAttributeEntityBaseGenerator.AddAttributeValueMethodGenerator());
		METHOD_GENERATORS_MAP.put(MemberAttributeEntityBaseGenerator.TYPE_NAME, methodGenerators);
		methodGenerators = new ArrayList<MethodGenerator>();
		methodGenerators.add(new StoreEntityEntityBaseGenerator.GetTypeMethodGenerator());
		METHOD_GENERATORS_MAP.put(StoreEntityEntityBaseGenerator.TYPE_NAME, methodGenerators);
		methodGenerators = new ArrayList<MethodGenerator>();
		methodGenerators.add(new ManagedFileEntityBaseGenerator.GetContextMethodGenerator());
		methodGenerators.add(new ManagedFileEntityBaseGenerator.GetFileMethodGenerator());
		methodGenerators.add(new ManagedFileEntityBaseGenerator.SetFileMethodGenerator());
		methodGenerators.add(new ManagedFileEntityBaseGenerator.GetLargeFileHomeMethodGenerator());
		methodGenerators.add(new ManagedFileEntityBaseGenerator.GetSmallFileHomeMethodGenerator());
		METHOD_GENERATORS_MAP.put(ManagedFileEntityBaseGenerator.TYPE_NAME, methodGenerators);
		methodGenerators = new ArrayList<MethodGenerator>();
		methodGenerators.add(new PAttrAttachmentValueEntityGenerator.SetPAttrValueMethodGenerator());
		METHOD_GENERATORS_MAP.put(PAttrAttachmentValueEntityGenerator.TYPE_NAME, methodGenerators);
		methodGenerators = new ArrayList<MethodGenerator>();
		methodGenerators.add(new MemberAttributeValueEntityBaseGenerator.GetMemberAttributeTypeMethodGenerator());
		METHOD_GENERATORS_MAP.put(MemberAttributeValueEntityBaseGenerator.TYPE_NAME, methodGenerators);
	}
	private static final Map<String, List<FieldGenerator>> FIELD_GENERATORS_MAP;
	static {
		FIELD_GENERATORS_MAP = new HashMap<String, List<FieldGenerator>>();
		List<FieldGenerator> fieldGenerators = new ArrayList<FieldGenerator>();
		fieldGenerators.add(new CatalogEntryEntityBaseGenerator.TypeSuffixFieldGenerator());
		FIELD_GENERATORS_MAP.put(CatalogEntryEntityBaseGenerator.TYPE_NAME, fieldGenerators);
		fieldGenerators = new ArrayList<FieldGenerator>();
		fieldGenerators.add(new MemberEntityBaseGenerator.TypeSuffixFieldGenerator());
		FIELD_GENERATORS_MAP.put(MemberEntityBaseGenerator.TYPE_NAME, fieldGenerators);
		fieldGenerators = new ArrayList<FieldGenerator>();
		fieldGenerators.add(new StoreEntityEntityBaseGenerator.TypeSuffixFieldGenerator());
		FIELD_GENERATORS_MAP.put(StoreEntityEntityBaseGenerator.TYPE_NAME, fieldGenerators);
		fieldGenerators = new ArrayList<FieldGenerator>();
		fieldGenerators.add(new MemberAttributeValueEntityBaseGenerator.TypeSuffixFieldGenerator());
		FIELD_GENERATORS_MAP.put(MemberAttributeValueEntityBaseGenerator.TYPE_NAME, fieldGenerators);
		fieldGenerators = new ArrayList<FieldGenerator>();
		fieldGenerators.add(new ManagedFileEntityBaseGenerator.ConstantLargeFileEjbNameFieldGenerator());
		fieldGenerators.add(new ManagedFileEntityBaseGenerator.ConstantSmallFileEjbNameFieldGenerator());
		fieldGenerators.add(new ManagedFileEntityBaseGenerator.ContextFieldGenerator());
		fieldGenerators.add(new ManagedFileEntityBaseGenerator.LargeFileBlobColumnNameFieldGenerator());
		fieldGenerators.add(new ManagedFileEntityBaseGenerator.LargeFileHomeFieldGenerator());
		fieldGenerators.add(new ManagedFileEntityBaseGenerator.LargeFileTableNameFieldGenerator());
		fieldGenerators.add(new ManagedFileEntityBaseGenerator.LargeFileWhereClauseFieldGenerator());
		fieldGenerators.add(new ManagedFileEntityBaseGenerator.SmallFileBlobColumnNameFieldGenerator());
		fieldGenerators.add(new ManagedFileEntityBaseGenerator.SmallFileHomeFieldGenerator());
		fieldGenerators.add(new ManagedFileEntityBaseGenerator.SmallFileTableNameFieldGenerator());
		fieldGenerators.add(new ManagedFileEntityBaseGenerator.SmallFileWhereClauseFieldGenerator());
		FIELD_GENERATORS_MAP.put(ManagedFileEntityBaseGenerator.TYPE_NAME, fieldGenerators);
		fieldGenerators = new ArrayList<FieldGenerator>();
		fieldGenerators.add(new EmailPromotionEntityBaseGenerator.UnsentIntegerFieldGenerator());
		fieldGenerators.add(new EmailPromotionEntityBaseGenerator.SentIntegerFieldGenerator());
		fieldGenerators.add(new EmailPromotionEntityBaseGenerator.DeletedIntegerFieldGenerator());
		FIELD_GENERATORS_MAP.put(EmailPromotionEntityBaseGenerator.TYPE_NAME, fieldGenerators);
		fieldGenerators = new ArrayList<FieldGenerator>();
		fieldGenerators.add(new OrderItemEntityBaseGenerator.BigDecimalFieldGenerator());
		FIELD_GENERATORS_MAP.put(OrderItemEntityBaseGenerator.TYPE_NAME, fieldGenerators);
		fieldGenerators = new ArrayList<FieldGenerator>();
		fieldGenerators.add(new EmailConfigurationEntityBaseGenerator.TypeInboundFieldGenerator());
		fieldGenerators.add(new EmailConfigurationEntityBaseGenerator.TypeOutboundFieldGenerator());
		FIELD_GENERATORS_MAP.put(EmailConfigurationEntityBaseGenerator.TYPE_NAME, fieldGenerators);
	}
	
	private ASTParser iASTParser;
	private BackupUtil iBackupUtil;
	private EntityInfo iEntityInfo;
	private ModuleInfo iModuleInfo;
	private ApplicationInfo iApplicationInfo;
	private ClassInfo iEntityClassInfo;
	private ClassInfo iEntityBaseClassInfo;
	private ClassInfo iEntityKeyClassInfo;
	private ClassInfo iEntityAccessHelperClassInfo;
	private List<FieldGenerator> iEntityFieldGenerators;
	private List<FieldGenerator> iEntityBaseFieldGenerators;
	private List<MethodGenerator> iEntityMethodGenerators;
	private List<MethodGenerator> iEntityBaseMethodGenerators;
//	private CompilationUnit iEntityCompilationUnit;
//	private CompilationUnit iEntityBaseCompilationUnit;
	
	public EntityClassGenerator(ASTParser astParser, BackupUtil backupUtil, EntityInfo entityInfo) {
		iASTParser = astParser;
		iBackupUtil = backupUtil;
		iEntityInfo = entityInfo;
		iModuleInfo = entityInfo.getModuleInfo();
		iApplicationInfo = iModuleInfo.getApplicationInfo();
		iEntityClassInfo = entityInfo.getEntityClassInfo();
		iEntityBaseClassInfo = entityInfo.getEntityBaseClassInfo();
		iEntityKeyClassInfo = entityInfo.getEntityKeyClassInfo();
		iEntityAccessHelperClassInfo = entityInfo.getEntityAccessHelperClassInfo();
		iEntityFieldGenerators = FIELD_GENERATORS_MAP.get(iEntityClassInfo.getQualifiedClassName());
		iEntityMethodGenerators = METHOD_GENERATORS_MAP.get(iEntityClassInfo.getQualifiedClassName());
		if (iEntityBaseClassInfo != null) {
			iEntityBaseFieldGenerators = FIELD_GENERATORS_MAP.get(iEntityBaseClassInfo.getQualifiedClassName());
			iEntityBaseMethodGenerators = METHOD_GENERATORS_MAP.get(iEntityBaseClassInfo.getQualifiedClassName());
		}
	}
	
	public void generate(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("generate "+iEntityInfo.getEjbName(), IProgressMonitor.UNKNOWN);
			generateEntityBaseClass(progressMonitor);
			progressMonitor.worked(500);
			generateEntityClass(progressMonitor);
			progressMonitor.worked(500);
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private void generateEntityClass(IProgressMonitor progressMonitor) {
		try {
			CompilationUnit ejbCompilationUnit = iEntityInfo.getEjbCompilationUnit();
			StringBuilder sb = new StringBuilder();
			sb.append("package ");
			sb.append(iEntityClassInfo.getPackageFragment().getElementName());
			sb.append(";\r\n");
			JavaUtil.appendCopyrightComment(sb);
			sb.append("\r\nimport javax.persistence.*;\r\n");
			@SuppressWarnings("unchecked")
			List<ImportDeclaration> importDeclarations = ejbCompilationUnit.imports();
			ImportUtil.appendImports(importDeclarations, sb);
			if (iEntityClassInfo.getSuperclassName() != null) {
				if (!iEntityClassInfo.getSuperclassPackage().equals(iEntityClassInfo.getPackageFragment().getElementName())) {
					sb.append("\r\nimport ");
					sb.append(iEntityClassInfo.getQualifiedSuperclassName());
					sb.append(";\r\n");
				}
			}
			sb.append("\r\n@javax.persistence.Entity(name = \"");
			sb.append(iEntityInfo.getEjbName());
			sb.append("\")");
			TableInfo tableInfo = iEntityInfo.getSecondaryTableInfo();
			if (tableInfo == null) {
				tableInfo = iEntityInfo.getPrimaryTableInfo();
			}
			if (tableInfo != null) {
				System.out.println("table="+tableInfo.getTableName());
				sb.append("\r\n@Table(name = \"");
				sb.append(tableInfo.getTableName());
				sb.append("\")");
			}
			else {
				System.out.println("no table for "+iEntityInfo.getEjbName());
			}
			if (iEntityInfo.getSubtypes() != null) {
				sb.append("\r\n@Inheritance(strategy = ");
				boolean joined = false;
				for (EntityInfo subType : iEntityInfo.getSubtypes()) {
					if (subType.getSecondaryTableInfo() != null) {
						joined = true;
						break;
					}
				}
				if (joined) {
					sb.append("InheritanceType.JOINED");
				}
				else {
					sb.append("InheritanceType.SINGLE_TABLE");
				}
				sb.append(")");
				if (iEntityInfo.getDiscriminatorColumnInfo() != null) {
					ColumnInfo columnInfo = iEntityInfo.getDiscriminatorColumnInfo();
					sb.append("\r\n@DiscriminatorColumn(name = \"");
					sb.append(columnInfo.getColumnName());
					sb.append("\"");
					String type = columnInfo.getTypeName();
					Integer length = columnInfo.getLength();
					if (CHAR.equals(type) && (length == null || length == 1)) {
						sb.append(", discriminatorType = DiscriminatorType.CHAR");
					}
					else if (VARCHAR.equals(type) || CHAR.equals(type)) {
						sb.append(", discriminatorType = DiscriminatorType.STRING");
					}
					else if (INTEGER.equals(type)) {
						sb.append(", discriminatorType = DiscriminatorType.INTEGER");
					}
					if (columnInfo.getLength() != null) {
						sb.append(", length = ");
						sb.append(columnInfo.getLength());
					}
					sb.append(")");
				}
			}
			if (iEntityInfo.getDiscriminatorValue() != null) {
				String discriminatorValue = iEntityInfo.getDiscriminatorValue();
				ColumnInfo discriminatorColumnInfo = iEntityInfo.getDiscriminatorColumnInfo();
				if (discriminatorColumnInfo == null && iEntityInfo.getSupertype() != null) {
					discriminatorColumnInfo = iEntityInfo.getSupertype().getDiscriminatorColumnInfo();
				}
				if (discriminatorColumnInfo != null) {
					Integer length = discriminatorColumnInfo.getLength();
					if (CHAR.equals(discriminatorColumnInfo.getTypeName()) && length != null && length > 1) {
						while (discriminatorValue.length() < length) {
							discriminatorValue = discriminatorValue + " ";
						}
					}
				}
				sb.append("\r\n@DiscriminatorValue(\"");
				sb.append(discriminatorValue);
				sb.append("\")");
			}
			if (iEntityInfo.getJoinKey() != null && iEntityInfo.getSecondaryTableInfo() != null) {
				ColumnInfo joinColumn = iEntityInfo.getSecondaryTableInfo().getColumnInfo(iEntityInfo.getJoinKey().getMembers()[0]);
				ColumnInfo referencedColumn = iEntityInfo.getSupertype().getPrimaryTableInfo().getColumnInfo(iEntityInfo.getJoinKey().getReferencedMembers()[0]);
				sb.append("\r\n@PrimaryKeyJoinColumn(name = \"");
				sb.append(joinColumn.getColumnName());
				sb.append("\", referencedColumnName = \"");
				sb.append(referencedColumn.getColumnName());
				sb.append("\")");
			}
			boolean includeAllKeyFields = false;
			if (iEntityKeyClassInfo != null && hasKeyFields(iEntityInfo.getEjbType())) {
				sb.append("\r\n@IdClass(");
				sb.append(iEntityKeyClassInfo.getQualifiedClassName());
				sb.append(".class)");
				includeAllKeyFields = true;
			}
			appendNamedNativeQueries(sb);
			sb.append("\r\npublic class ");
			sb.append(iEntityClassInfo.getClassName());
			if (iEntityClassInfo.getSuperclassName() != null) {
				sb.append(" extends ");
				sb.append(iEntityClassInfo.getSuperclassName());
			}
			if (iEntityBaseClassInfo == null) {
				if (iEntityInfo.getProtectable() && (iEntityInfo.getSupertype() == null || !iEntityInfo.getSupertype().getProtectable())) {
					sb.append(" implements com.ibm.commerce.security.Protectable");
					if (iEntityInfo.getGroupable() && (iEntityInfo.getSupertype() == null || !iEntityInfo.getSupertype().getGroupable())) {
						sb.append(", com.ibm.commerce.grouping.Groupable");
					}
				}
				else if (iEntityInfo.getGroupable() && (iEntityInfo.getSupertype() == null || !iEntityInfo.getSupertype().getGroupable())) {
					sb.append(" implements com.ibm.commerce.security.Groupable");
				}
			}
			sb.append(" {\r\n");
			JavaUtil.appendCopyrightField(sb);
			sb.append("\r\n\tpublic ");
			sb.append(iEntityClassInfo.getClassName());
			sb.append("() {\r\n\t}\r\n");
			List<FieldInfo> fields = getFields((TypeDeclaration) ejbCompilationUnit.types().get(0));
			List<RelatedEntityInfo> relatedEntities = getRelatedEntities((TypeDeclaration) ejbCompilationUnit.types().get(0));
			List<EjbRelationshipRoleInfo> ejbRelationshipRoles = getEjbRelationshipRoles((TypeDeclaration) ejbCompilationUnit.types().get(0));
			if (includeAllKeyFields) {
				List<FieldInfo> keyFields = iEntityInfo.getKeyFields();
				for (FieldInfo keyFieldInfo : keyFields) {
					if (!fields.contains(keyFieldInfo)) {
						fields.add(keyFieldInfo);
					}
					RelatedEntityInfo relatedEntityInfo = keyFieldInfo.getRelatedEntityInfo();
					if (relatedEntityInfo != null && !relatedEntities.contains(relatedEntityInfo)) {
						relatedEntities.add(relatedEntityInfo);
					}
				}
				List<EjbRelationshipRoleInfo> allEjbRelationshipRoles = iEntityInfo.getEjbRelationshipRoles();
				for (EjbRelationshipRoleInfo ejbRelationshipRole : allEjbRelationshipRoles) {
					if (ejbRelationshipRole.getIsKeyField() && !ejbRelationshipRoles.contains(ejbRelationshipRole)) {
						ejbRelationshipRoles.add(ejbRelationshipRole);
					}
				}
			}
			appendFieldDeclarations(fields, relatedEntities, ejbRelationshipRoles, sb);
			appendGeneratedFields(sb, iEntityInfo, iEntityFieldGenerators);
			appendGettersAndSetters(fields, relatedEntities, ejbRelationshipRoles, sb);
			if (iEntityBaseClassInfo == null) {
				appendGetAccessHelper(sb);
			}
			appendGeneratedMethods(sb, iEntityInfo, iEntityMethodGenerators);
			sb.append("}");
			String source = sb.toString();
			List<FieldDeclaration> fieldDeclarations = getPortRequiredFieldDeclarations((TypeDeclaration) ejbCompilationUnit.types().get(0), iEntityFieldGenerators);
			List<MethodDeclaration> methodDeclarations = getPortRequiredMethodDeclarations((TypeDeclaration) ejbCompilationUnit.types().get(0), iEntityMethodGenerators);
			if (fieldDeclarations.size() > 0 || methodDeclarations.size() > 0) {
				IDocument document = new Document(source);
				iASTParser.setProject(iEntityInfo.getModuleInfo().getJavaProject());
				iASTParser.setResolveBindings(false);
				iASTParser.setSource(document.get().toCharArray());
				CompilationUnit entityCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
				entityCompilationUnit.recordModifications();
				portFieldDeclarations((TypeDeclaration) entityCompilationUnit.types().get(0), fieldDeclarations);
				portMethodDeclarations((TypeDeclaration) entityCompilationUnit.types().get(0), methodDeclarations);
				TextEdit edits = entityCompilationUnit.rewrite(document, null);
				edits.apply(document);
				source = document.get();
			}

			ICompilationUnit compilationUnit = iEntityClassInfo.getPackageFragment().createCompilationUnit(iEntityClassInfo.getClassName() + ".java", source, true, new SubProgressMonitor(progressMonitor, 100));
//			ImportUtil.resolveImports(iASTParser, compilationUnit, new SubProgressMonitor(progressMonitor, 100));
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
	
	private void generateEntityBaseClass(IProgressMonitor progressMonitor) {
		if (iEntityBaseClassInfo != null) {
			try {
				CompilationUnit ejbBaseCompilationUnit = iEntityInfo.getEjbBaseCompilationUnit();
				StringBuilder sb = new StringBuilder();
				sb.append("package ");
				sb.append(iEntityBaseClassInfo.getPackageFragment().getElementName());
				sb.append(";\r\n");
				JavaUtil.appendCopyrightComment(sb);
				sb.append("\r\nimport javax.persistence.*;\r\n");
				@SuppressWarnings("unchecked")
				List<ImportDeclaration> importDeclarations = ejbBaseCompilationUnit.imports();
				ImportUtil.appendImports(importDeclarations, sb);
				if (iEntityBaseClassInfo.getSuperclassName() != null) {
					if (!iEntityBaseClassInfo.getSuperclassPackage().equals(iEntityBaseClassInfo.getPackageFragment().getElementName())) {
						sb.append("\r\nimport ");
						sb.append(iEntityBaseClassInfo.getQualifiedSuperclassName());
						sb.append(";\r\n");
					}
				}
				sb.append("\r\n@MappedSuperclass");
				boolean excludeAllKeyFields = false;
				if (iEntityKeyClassInfo != null && hasKeyFields(iEntityInfo.getEjbBaseType())) {
					if (!hasKeyFields(iEntityInfo.getEjbType())) {
						sb.append("\r\n@IdClass(");
						sb.append(iEntityKeyClassInfo.getQualifiedClassName());
						sb.append(".class)");
					}
					else {
						excludeAllKeyFields = true;
					}
				}
				sb.append("\r\npublic class ");
				sb.append(iEntityBaseClassInfo.getClassName());
				if (iEntityBaseClassInfo.getSuperclassName() != null) {
					sb.append(" extends ");
					sb.append(iEntityBaseClassInfo.getSuperclassName());
				}
				if (iEntityInfo.getProtectable() && (iEntityInfo.getSupertype() == null || !iEntityInfo.getSupertype().getProtectable())) {
					sb.append(" implements com.ibm.commerce.security.Protectable");
					if (iEntityInfo.getGroupable() && (iEntityInfo.getSupertype() == null || !iEntityInfo.getSupertype().getGroupable())) {
						sb.append(", com.ibm.commerce.grouping.Groupable");
					}
				}
				else if (iEntityInfo.getGroupable() && (iEntityInfo.getSupertype() == null || !iEntityInfo.getSupertype().getGroupable())) {
					sb.append(" implements com.ibm.commerce.grouping.Groupable");
				}
				sb.append(" {\r\n");
				JavaUtil.appendCopyrightField(sb);
				List<FieldInfo> fields = getFields((TypeDeclaration) ejbBaseCompilationUnit.types().get(0));
				List<RelatedEntityInfo> relatedEntities = getRelatedEntities((TypeDeclaration) ejbBaseCompilationUnit.types().get(0));
				List<EjbRelationshipRoleInfo> ejbRelationshipRoles = getEjbRelationshipRoles((TypeDeclaration) ejbBaseCompilationUnit.types().get(0));
				if (excludeAllKeyFields) {
					List<FieldInfo> keyFields = iEntityInfo.getKeyFields();
					for (FieldInfo keyFieldInfo : keyFields) {
						if (fields.contains(keyFieldInfo)) {
							fields.remove(keyFieldInfo);
						}
						RelatedEntityInfo relatedEntityInfo = keyFieldInfo.getRelatedEntityInfo();
						if (relatedEntityInfo != null && relatedEntities.contains(relatedEntityInfo)) {
							relatedEntities.remove(relatedEntityInfo);
						}
					}
					List<EjbRelationshipRoleInfo> allEjbRelationshipRoles = iEntityInfo.getEjbRelationshipRoles();
					for (EjbRelationshipRoleInfo ejbRelationshipRole : allEjbRelationshipRoles) {
						if (ejbRelationshipRole.getIsKeyField() && ejbRelationshipRoles.contains(ejbRelationshipRole)) {
							ejbRelationshipRoles.remove(ejbRelationshipRole);
						}
					}
				}
				appendFieldDeclarations(fields, relatedEntities, ejbRelationshipRoles, sb);
				appendGeneratedFields(sb, iEntityInfo, iEntityBaseFieldGenerators);
				appendGettersAndSetters(fields, relatedEntities, ejbRelationshipRoles, sb);
				appendGetAccessHelper(sb);
				appendGeneratedMethods(sb, iEntityInfo, iEntityBaseMethodGenerators);
				sb.append("}");
				String source = sb.toString();
				List<FieldDeclaration> fieldDeclarations = getPortRequiredFieldDeclarations((TypeDeclaration) ejbBaseCompilationUnit.types().get(0), iEntityBaseFieldGenerators);
				List<MethodDeclaration> methodDeclarations = getPortRequiredMethodDeclarations((TypeDeclaration) ejbBaseCompilationUnit.types().get(0), iEntityBaseMethodGenerators);
				if (fieldDeclarations.size() > 0 || methodDeclarations.size() > 0) {
					IDocument document = new Document(source);
					iASTParser.setProject(iEntityInfo.getModuleInfo().getJavaProject());
					iASTParser.setResolveBindings(false);
					iASTParser.setSource(document.get().toCharArray());
					CompilationUnit entityCompilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
					entityCompilationUnit.recordModifications();
					portFieldDeclarations((TypeDeclaration) entityCompilationUnit.types().get(0), fieldDeclarations);
					portMethodDeclarations((TypeDeclaration) entityCompilationUnit.types().get(0), methodDeclarations);
					TextEdit edits = entityCompilationUnit.rewrite(document, null);
					edits.apply(document);
					source = document.get();
				}
				ICompilationUnit compilationUnit = iEntityBaseClassInfo.getPackageFragment().createCompilationUnit(iEntityBaseClassInfo.getClassName() + ".java", source, true, new SubProgressMonitor(progressMonitor, 100));
//				ImportUtil.resolveImports(iASTParser, compilationUnit, new SubProgressMonitor(progressMonitor, 100));
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
	}

	private List<FieldInfo> getFields(TypeDeclaration ejbTypeDeclaration) {
		List<FieldInfo> fields = new ArrayList<FieldInfo>();
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = ejbTypeDeclaration.bodyDeclarations();
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			switch (bodyDeclaration.getNodeType()) {
				case ASTNode.FIELD_DECLARATION: {
					FieldDeclaration fieldDeclaration = (FieldDeclaration) bodyDeclaration;
					@SuppressWarnings("unchecked")
					List<VariableDeclarationFragment> ejbVariableDeclarationFragments = fieldDeclaration.fragments();
					for (VariableDeclarationFragment ejbVariableDeclarationFragment : ejbVariableDeclarationFragments) {
						String fieldName = ejbVariableDeclarationFragment.getName().getIdentifier();
						FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
						if (fieldInfo != null && !fields.contains(fieldInfo)) {
							fields.add(fieldInfo);
						}
					}
					break;
				}
				case ASTNode.METHOD_DECLARATION: {
					MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
					if (methodDeclaration.getReturnType2() != null && methodDeclaration.parameters().size() == 0) {
						FieldInfo fieldInfo = iEntityInfo.getFieldInfoByGetterName(methodDeclaration.getName().getIdentifier());
						if (fieldInfo != null && !fields.contains(fieldInfo)) {
							fields.add(fieldInfo);
						}
					}
				}
			}
		}
		return fields;
	}
	
	private List<RelatedEntityInfo> getRelatedEntities(TypeDeclaration ejbTypeDeclaration) {
		List<RelatedEntityInfo> relatedEntities = new ArrayList<RelatedEntityInfo>();
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = ejbTypeDeclaration.bodyDeclarations();
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			switch (bodyDeclaration.getNodeType()) {
				case ASTNode.FIELD_DECLARATION: {
					FieldDeclaration fieldDeclaration = (FieldDeclaration) bodyDeclaration;
					@SuppressWarnings("unchecked")
					List<VariableDeclarationFragment> ejbVariableDeclarationFragments = fieldDeclaration.fragments();
					for (VariableDeclarationFragment ejbVariableDeclarationFragment : ejbVariableDeclarationFragments) {
						String fieldName = ejbVariableDeclarationFragment.getName().getIdentifier();
						FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
						if (fieldInfo != null && fieldInfo.getRelatedEntityInfo() != null && !relatedEntities.contains(fieldInfo.getRelatedEntityInfo())) {
							relatedEntities.add(fieldInfo.getRelatedEntityInfo());
						}
					}
					break;
				}
				case ASTNode.METHOD_DECLARATION: {
					MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
					if (methodDeclaration.getReturnType2() != null && methodDeclaration.parameters().size() == 0) {
						FieldInfo fieldInfo = iEntityInfo.getFieldInfoByGetterName(methodDeclaration.getName().getIdentifier());
						if (fieldInfo != null && fieldInfo.getRelatedEntityInfo() != null && !relatedEntities.contains(fieldInfo.getRelatedEntityInfo())) {
							relatedEntities.add(fieldInfo.getRelatedEntityInfo());
						}
					}
				}
			}
		}
		return relatedEntities;
	}
	
	private List<EjbRelationshipRoleInfo> getEjbRelationshipRoles(TypeDeclaration ejbTypeDeclaration) {
		List<EjbRelationshipRoleInfo> ejbRelationshipRoles = new ArrayList<EjbRelationshipRoleInfo>();
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = ejbTypeDeclaration.bodyDeclarations();
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			switch (bodyDeclaration.getNodeType()) {
				case ASTNode.FIELD_DECLARATION: {
					FieldDeclaration fieldDeclaration = (FieldDeclaration) bodyDeclaration;
					@SuppressWarnings("unchecked")
					List<VariableDeclarationFragment> ejbVariableDeclarationFragments = fieldDeclaration.fragments();
					for (VariableDeclarationFragment ejbVariableDeclarationFragment : ejbVariableDeclarationFragments) {
						String fieldName = ejbVariableDeclarationFragment.getName().getIdentifier();
						EjbRelationshipRoleInfo ejbRelationshipRoleInfo = iEntityInfo.getEjbRelationshipRoleInfoByName(fieldName);
						if (ejbRelationshipRoleInfo != null && !ejbRelationshipRoles.contains(ejbRelationshipRoleInfo)) {
							ejbRelationshipRoles.add(ejbRelationshipRoleInfo);
						}
					}
					break;
				}
				case ASTNode.METHOD_DECLARATION: {
					MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
					if (methodDeclaration.getReturnType2() != null && methodDeclaration.parameters().size() == 0) {
						EjbRelationshipRoleInfo ejbRelationshipRoleInfo = iEntityInfo.getEjbRelationshipRoleInfoByGetterName(methodDeclaration.getName().getIdentifier());
						if (ejbRelationshipRoleInfo != null && !ejbRelationshipRoles.contains(ejbRelationshipRoleInfo)) {
							ejbRelationshipRoles.add(ejbRelationshipRoleInfo);
						}
					}
				}
			}
		}
		return ejbRelationshipRoles;
	}
	
	private void appendFieldDeclarations(List<FieldInfo> fields, List<RelatedEntityInfo> relatedEntities, List<EjbRelationshipRoleInfo> ejbRelationshipRoles, StringBuilder sb) {
		for (FieldInfo fieldInfo : fields) {
			if (fieldInfo.getRelatedEntityInfo() == null && !fieldInfo.getFieldName().equals(OPT_COUNTER)) {
				sb.append("\r\n\tprivate ");
				sb.append(fieldInfo.getTypeName());
				sb.append(" ");
				sb.append(fieldInfo.getTargetFieldName());
//				if (fieldInfo.getColumnInfo() != null && fieldInfo.getColumnInfo().getDefaultValue() != null) {
//					String defaultValue = fieldInfo.getColumnInfo().getDefaultValue();
//					if (fieldInfo.getTypeName().equals("java.math.BigDecimal")) {
//						sb.append(" = new ");
//						sb.append(fieldInfo.getTypeName());
//						sb.append("(");
//						sb.append(defaultValue);
//						sb.append(")");
//					}
//					else if (fieldInfo.getTypeName().equals("java.lang.Boolean")) {
//						if ("1".equals(defaultValue)) {
//							sb.append(" = java.lang.Boolean.TRUE");
//						}
//						else {
//							sb.append(" = java.lang.Boolean.FALSE");
//						}
//					}
//				}
				sb.append(";\r\n");
			}
		}
		for (RelatedEntityInfo relatedEntityInfo : relatedEntities) {
			if (relatedEntityInfo.getEjbRelationshipRoleInfo() == null) {
				sb.append("\r\n\tprivate ");
				sb.append(relatedEntityInfo.getParentEntityInfo().getEntityClassInfo().getQualifiedClassName());
				sb.append(" ");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(";\r\n");
			}
		}
		for (EjbRelationshipRoleInfo ejbRelationshipRoleInfo : ejbRelationshipRoles) {
			sb.append("\r\n\tprivate ");
			if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
				sb.append("java.util.Collection<");
			}
			sb.append(ejbRelationshipRoleInfo.getRelatedEntityInfo().getEntityClassInfo().getQualifiedClassName());
			if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
				sb.append(">");
			}
			sb.append(" ");
			sb.append(ejbRelationshipRoleInfo.getFieldName());
			sb.append(";\r\n");
		}
	}
	
	private void appendGettersAndSetters(List<FieldInfo> fields, List<RelatedEntityInfo> relatedEntities, List<EjbRelationshipRoleInfo> ejbRelationshipRoles, StringBuilder sb) {
		for (FieldInfo fieldInfo : fields) {
			if (fieldInfo.getRelatedEntityInfo() == null && !fieldInfo.getFieldName().equals(OPT_COUNTER)) {
				sb.append("\r\n\tpublic void ");
				sb.append(fieldInfo.getTargetSetterName());
				sb.append("(");
				sb.append(fieldInfo.getTypeName());
				sb.append(" ");
				sb.append(fieldInfo.getTargetFieldName());
				sb.append(") {\r\n\t\tthis.");
				sb.append(fieldInfo.getTargetFieldName());
				sb.append(" = ");
				sb.append(fieldInfo.getTargetFieldName());
				sb.append(";\r\n\t}\r\n\r\n\t");
				ColumnInfo columnInfo = fieldInfo.getColumnInfo();
				if (columnInfo != null) {
					if (CLOB.equals(columnInfo.getTypeName()) || BLOB.equals(columnInfo.getTypeName())) {
						sb.append("@Lob\r\n\t");
					}
					sb.append("@Column(name = \"");
					sb.append(columnInfo.getColumnName());
					sb.append("\"");
					if (!columnInfo.getNullable()) {
						sb.append(", nullable = false");
					}
					if (columnInfo.getLength() != null) {
						sb.append(", length = ");
						sb.append(columnInfo.getLength().toString());
					}
					sb.append(")\r\n\t");
					if (!columnInfo.getNullable()) {
						sb.append("@Basic(optional = false)\r\n\t");
					}
				}
				if (fieldInfo.getIsKeyField()) {
					sb.append("@Id\r\n\t");
				}
				sb.append("public ");
				sb.append(fieldInfo.getTypeName());
				sb.append(" ");
				sb.append(fieldInfo.getTargetGetterName());
				sb.append("() {\r\n\t\treturn ");
				sb.append(fieldInfo.getTargetFieldName());
				sb.append(";\r\n\t}\r\n");
			}
		}
		for (RelatedEntityInfo relatedEntityInfo : relatedEntities) {
			if (relatedEntityInfo.getEjbRelationshipRoleInfo() == null) {
				sb.append("\r\n\tpublic void ");
				sb.append(relatedEntityInfo.getSetterName());
				sb.append("(");
				sb.append(relatedEntityInfo.getParentEntityInfo().getEntityClassInfo().getQualifiedClassName());
				sb.append(" ");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(") {\r\n\t\tthis.");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(" = ");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(";\r\n\t}\r\n\r\n\t@OneToOne");
				if (!relatedEntityInfo.getOptional()) {
					sb.append("(optional = false)");
				}
				sb.append("\r\n\t");
				List<FieldInfo> memberFields = relatedEntityInfo.getMemberFields();
				if (memberFields.size() == 1) {
					ColumnInfo columnInfo = memberFields.get(0).getColumnInfo();
					ColumnInfo referencedColumn = columnInfo.getReferencedColumn();
					sb.append("@JoinColumn(name = \"");
					sb.append(columnInfo.getColumnName());
					sb.append("\"");
					if (!columnInfo.getNullable()) {
						sb.append(", nullable = false");
					}
					sb.append(", referencedColumnName = \"");
					sb.append(referencedColumn.getColumnName());
					sb.append("\")\r\n\t");
				}
				else if (memberFields.size() > 1) {
					sb.append("@JoinColumns({\r\n\t\t");
					boolean firstColumn = true;
					for (FieldInfo fieldInfo : memberFields) {
						ColumnInfo columnInfo = fieldInfo.getColumnInfo();
						ColumnInfo referencedColumn = columnInfo.getReferencedColumn();
						if (firstColumn) {
							firstColumn = false;
						}
						else {
							sb.append(",\r\n\t\t");
						}
						sb.append("@JoinColumn(name = \"");
						sb.append(columnInfo.getColumnName());
						sb.append("\"");
						if (!columnInfo.getNullable()) {
							sb.append(", nullable = false");
						}
						sb.append(", referencedColumnName = \"");
						sb.append(referencedColumn.getColumnName());
						sb.append("\")");
					}
					sb.append("\r\n\t})\r\n\t");
				}
				if (relatedEntityInfo.getIsKeyField()) {
					sb.append("@Id\r\n\t");
				}
				sb.append("public ");
				sb.append(relatedEntityInfo.getParentEntityInfo().getEntityClassInfo().getQualifiedClassName());
				sb.append(" ");
				sb.append(relatedEntityInfo.getGetterName());
				sb.append("() {\r\n\t\treturn ");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(";\r\n\t}\r\n");
			}
		}
		for (EjbRelationshipRoleInfo ejbRelationshipRoleInfo : ejbRelationshipRoles) {
			sb.append("\r\n\tpublic void ");
			sb.append(ejbRelationshipRoleInfo.getSetterName());
			sb.append("(");
			if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
				sb.append("java.util.Collection<");
			}
			sb.append(ejbRelationshipRoleInfo.getRelatedEntityInfo().getEntityClassInfo().getQualifiedClassName());
			if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
				sb.append(">");
			}
			sb.append(" ");
			sb.append(ejbRelationshipRoleInfo.getFieldName());
			sb.append(") {\r\n\t\tthis.");
			sb.append(ejbRelationshipRoleInfo.getFieldName());
			sb.append(" = ");
			sb.append(ejbRelationshipRoleInfo.getFieldName());
			sb.append(";\r\n\t}\r\n\r\n\t@");
			sb.append(ejbRelationshipRoleInfo.getMultiplicity());
			sb.append("To");
			sb.append(ejbRelationshipRoleInfo.getRelatedMultiplicity());
			ForeignKeyInfo foreignKeyInfo = ejbRelationshipRoleInfo.getEjbRelationInfo().getConstraintInfo().getForeignKeyInfo();
			TableInfo tableInfo = ejbRelationshipRoleInfo.getEntityInfo().getTableInfo();
			if (foreignKeyInfo.getTableInfo() == tableInfo) {
				ColumnInfo columnInfo = foreignKeyInfo.getMemberColumns().get(0);
				if (!columnInfo.getNullable()) {
					sb.append("(optional = false)");
				}
				sb.append("\r\n\t");
				ColumnInfo referencedColumn = columnInfo.getReferencedColumn();
				sb.append("@JoinColumn(name = \"");
				sb.append(columnInfo.getColumnName());
				sb.append("\"");
				if (!columnInfo.getNullable()) {
					sb.append(", nullable = false");
				}
				sb.append(", referencedColumnName = \"");
				sb.append(referencedColumn.getColumnName());
				sb.append("\")\r\n\t");
				if (ejbRelationshipRoleInfo.getIsKeyField()) {
					sb.append("@Id\r\n\t");
				}
			}
			else {
				sb.append("(mappedBy = \"");
				sb.append(ejbRelationshipRoleInfo.getRelatedFieldName());
				sb.append("\"");
				if (ejbRelationshipRoleInfo.getCascadeDelete()) {
					sb.append(", cascade = REMOVE");
				}
				sb.append(")\r\n\t");
			}
			sb.append("public ");
			if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
				sb.append("java.util.Collection<");
			}
			sb.append(ejbRelationshipRoleInfo.getRelatedEntityInfo().getEntityClassInfo().getQualifiedClassName());
			if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
				sb.append(">");
			}
			sb.append(" ");
			sb.append(ejbRelationshipRoleInfo.getGetterName());
			sb.append("() {\r\n\t\treturn ");
			sb.append(ejbRelationshipRoleInfo.getFieldName());
			sb.append(";\r\n\t}\r\n");
		}
	}
	
	private void appendGetAccessHelper(StringBuilder sb) {
		if (iEntityAccessHelperClassInfo != null) {
			sb.append("\r\n\t@Transient\r\n\tprotected com.ibm.commerce.security.AccessHelper getAccessHelper() {\r\n\t\tif (accessHelper == null) {\r\n\t\t\taccessHelper = new ");
			sb.append(iEntityAccessHelperClassInfo.getQualifiedClassName());
			sb.append("();\r\n\t\t}\r\n\t\treturn accessHelper;\r\n\t}\r\n");
		}
	}
	
	private void appendGeneratedFields(StringBuilder sb, EntityInfo entityInfo, List<FieldGenerator> fieldGenerators) {
		if (fieldGenerators != null) {
			for (FieldGenerator fieldGenerator : fieldGenerators) {
				fieldGenerator.appendField(sb, entityInfo);
			}
		}
	}
	
	private void appendGeneratedMethods(StringBuilder sb, EntityInfo entityInfo, List<MethodGenerator> methodGenerators) {
		if (methodGenerators != null) {
			for (MethodGenerator methodGenerator : methodGenerators) {
				methodGenerator.appendMethod(sb, entityInfo);
			}
		}
	}
	
//	private void portEjbTypeDeclaration(TypeDeclaration ejbTypeDeclaration, CompilationUnit entityCompilationUnit) {
//		@SuppressWarnings("unchecked")
//		List<BodyDeclaration> bodyDeclarations = ejbTypeDeclaration.bodyDeclarations();
//		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
//			switch (bodyDeclaration.getNodeType()) {
//				case ASTNode.FIELD_DECLARATION: {
//					FieldDeclaration fieldDeclaration = (FieldDeclaration) bodyDeclaration;
//					@SuppressWarnings("unchecked")
//					List<VariableDeclarationFragment> ejbVariableDeclarationFragments = fieldDeclaration.fragments();
//					for (VariableDeclarationFragment ejbVariableDeclarationFragment : ejbVariableDeclarationFragments) {
//						String fieldName = ejbVariableDeclarationFragment.getName().getIdentifier();
//						FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
//						if (fieldInfo != null) {
//							FieldDeclaration entityFieldDeclaration = (FieldDeclaration) ASTNode.copySubtree(entityCompilationUnit.getAST(), fieldDeclaration);
//							JavaUtil.setFieldPrivate(entityFieldDeclaration);
//							bodyDeclarations.add(entityFieldDeclaration);
//						}
//					}
//					break;
//				}
//				case ASTNode.METHOD_DECLARATION: {
//					portMethodDeclaration((MethodDeclaration) bodyDeclaration, entityCompilationUnit);
//					break;
//				}
//				default: {
//					System.out.println("bodyDeclaration: "+bodyDeclaration);
//					break;
//				}
//			}
//		}
//	}
//
//	private FieldInfo portFieldDeclaration(FieldDeclaration fieldDeclaration, CompilationUnit entityCompilationUnit) {
//		FieldInfo fieldInfo = null;
//		@SuppressWarnings("unchecked")
//		List<BodyDeclaration> bodyDeclarations = ((TypeDeclaration) entityCompilationUnit.types().get(0)).bodyDeclarations();
//		@SuppressWarnings("unchecked")
//		List<VariableDeclarationFragment> ejbVariableDeclarationFragments = fieldDeclaration.fragments();
//		for (VariableDeclarationFragment ejbVariableDeclarationFragment : ejbVariableDeclarationFragments) {
//			String fieldName = ejbVariableDeclarationFragment.getName().getIdentifier();
//			fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
//			if (fieldInfo != null) {
//				FieldDeclaration entityFieldDeclaration = (FieldDeclaration) ASTNode.copySubtree(entityCompilationUnit.getAST(), fieldDeclaration);
//				JavaUtil.setFieldPrivate(entityFieldDeclaration);
//				bodyDeclarations.add(entityFieldDeclaration);
//			}
//		}
//		return fieldInfo;
//	}
	
	private void appendNamedNativeQueries(StringBuilder sb) {
		List<FinderInfo> finders = iEntityInfo.getQueryFinders();
		Map<String, String> nativeQueries = new HashMap<String, String>();
		Map<String, String> queries = new HashMap<String, String>();
		for (FinderInfo finderInfo : finders) {
			if (finderInfo.getFinderSelectStatement() != null) {
				nativeQueries.put(finderInfo.getQueryName(), finderInfo.getFinderSelectStatement());
			}
			else if (finderInfo.getFinderQuery() != null) {
				queries.put(finderInfo.getQueryName(), resolveQuery(finderInfo.getFinderQuery()));
			}
			else {
				nativeQueries.put(finderInfo.getQueryName(), iEntityInfo.getSelectClause() + finderInfo.getFinderWhereClause());
				if (finderInfo.getOracleFinderWhereClause() != null) {
					nativeQueries.put(finderInfo.getQueryName() + "_ORACLE", iEntityInfo.getSelectClause() + finderInfo.getOracleFinderWhereClause());	
				}
			}
		}
		if (nativeQueries.size() == 1) {
			Set<String> names = nativeQueries.keySet();
			for (String name : names) {
				sb.append("\r\n@NamedNativeQuery(name = \"");
				sb.append(name);
				sb.append("\", query = \"");
				sb.append(nativeQueries.get(name));
				sb.append("\", resultClass=");
				sb.append(iEntityClassInfo.getQualifiedClassName());
				sb.append(".class)");
			}
		}
		else {
			Set<String> names = nativeQueries.keySet();
			boolean firstQuery = true;
			for (String name : names) {
				if (firstQuery) {
					firstQuery = false;
					sb.append("\r\n@NamedNativeQueries({");
				}
				else {
					sb.append(",");
				}
				sb.append("\r\n\t@NamedNativeQuery(name = \"");
				sb.append(name);
				sb.append("\", query = \"");
				sb.append(nativeQueries.get(name));
				sb.append("\", resultClass=");
				sb.append(iEntityClassInfo.getQualifiedClassName());
				sb.append(".class)");
			}
			if (!firstQuery) {
				sb.append("\r\n})");
			}
		}
		if (queries.size() == 1) {
			Set<String> names = queries.keySet();
			for (String name : names) {
				sb.append("\r\n@NamedQuery(name = \"");
				sb.append(name);
				sb.append("\", query = \"");
				sb.append(queries.get(name));
				sb.append("\")");
			}
		}
		else {
			Set<String> names = queries.keySet();
			boolean firstQuery = true;
			for (String name : names) {
				if (firstQuery) {
					firstQuery = false;
					sb.append("\r\n@NamedQueries({");
				}
				else {
					sb.append(",");
				}
				sb.append("\r\n\t@NamedQuery(name = \"");
				sb.append(name);
				sb.append("\", query = \"");
				sb.append(queries.get(name));
				sb.append("\")");
			}
			if (!firstQuery) {
				sb.append("\r\n})");
			}
		}
	}
	
	private String resolveQuery(String sourceQuery) {
		String targetQuery = sourceQuery.replace(" from " + iEntityInfo.getEjbName() + "Bean", " from " + iEntityInfo.getEjbName());
		Collection<FieldInfo> fields = iEntityInfo.getFields();
		for (FieldInfo fieldInfo : fields) {
			if (fieldInfo.getRelatedEntityInfo() != null) {
				targetQuery = targetQuery.replaceAll("." + fieldInfo.getFieldName(), "." + fieldInfo.getRelatedEntityInfo().getFieldName() + "." + fieldInfo.getReferencedFieldInfo().getTargetFieldName());
			}
		}

		// 2018-04-27 - bsteinba@us.ibm.com - there may still be "Bean" object references in the EJB QL
		// Issue #2 - replace object "Bean" references in sub-selects with just the object name
		targetQuery = targetQuery.replaceAll(" (.*?)Bean ", " $1 ");
		
		return targetQuery;
	}
	
//	private void portMethodDeclaration(MethodDeclaration methodDeclaration, CompilationUnit entityCompilationUnit) {
//		String methodName = methodDeclaration.getName().getIdentifier();
//		if (methodName.startsWith("get")) {
//			portGetterMethod(methodDeclaration, entityCompilationUnit);
//		}
//		else if (methodName.startsWith("set")) {
//			portSetterMethod(methodDeclaration, entityCompilationUnit);
//		}
//		else if (STANDARD_EJB_METHODS.contains(methodName)) {
//			portStandardMethod(methodDeclaration, entityCompilationUnit);
//		}
//		else {
//			portUserMethod(methodDeclaration, entityCompilationUnit);
//		}
//	}
	
//	private void portGetterMethod(MethodDeclaration methodDeclaration, CompilationUnit entityCompilationUnit) {
//		String methodName = methodDeclaration.getName().getIdentifier();
//		String fieldName = methodName.substring(3);
//		FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
//		if (fieldInfo == null) {
//			fieldInfo = iEntityInfo.getFieldInfoByName(Introspector.decapitalize(fieldName));
//		}
//		if (fieldInfo == null) {
//			System.out.println("getter does not match field name: "+methodName);
//		}
//		else {
//			AST ast = entityCompilationUnit.getAST();
//			MethodDeclaration entityMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(ast, methodDeclaration);
//			entityMethodDeclaration.setJavadoc(null);
//			@SuppressWarnings("unchecked")
//			List<IExtendedModifier> modifiers = entityMethodDeclaration.modifiers();
//			ColumnInfo columnInfo = fieldInfo.getColumnInfo();
//			if (columnInfo != null) {
//				NormalAnnotation columnAnnotation = ast.newNormalAnnotation();
//				columnAnnotation.setTypeName(ast.newSimpleName("Column"));
//				@SuppressWarnings("unchecked")
//				List<MemberValuePair> values = columnAnnotation.values();
//				MemberValuePair columnName = ast.newMemberValuePair();
//				columnName.setName(ast.newSimpleName("name"));
//				StringLiteral stringLiteral = ast.newStringLiteral();
//				stringLiteral.setLiteralValue(columnInfo.getColumnName());
//				columnName.setValue(stringLiteral);
//				values.add(columnName);
//				if (!columnInfo.getNullable()) {
//					MemberValuePair nullable = ast.newMemberValuePair();
//					nullable.setName(ast.newSimpleName("nullable"));
//					nullable.setValue(ast.newBooleanLiteral(false));
//					values.add(nullable);
//				}
//				if (columnInfo.getLength() != null) {
//					MemberValuePair length = ast.newMemberValuePair();
//					length.setName(ast.newSimpleName("length"));
//					length.setValue(ast.newNumberLiteral(columnInfo.getLength().toString()));
//					values.add(length);
//				}
//				modifiers.add(0, columnAnnotation);
//			}
//			if (fieldInfo.getIsKeyField()) {
//				MarkerAnnotation idAnnotation = ast.newMarkerAnnotation();
//				idAnnotation.setTypeName(ast.newSimpleName("Id"));
//				modifiers.add(0, idAnnotation);
//			}
//			@SuppressWarnings("unchecked")
//			List<BodyDeclaration> bodyDeclarations = ((TypeDeclaration) entityCompilationUnit.types().get(0)).bodyDeclarations();
//			bodyDeclarations.add(entityMethodDeclaration);
//		}
//	}
//	
//	private void portSetterMethod(MethodDeclaration methodDeclaration, CompilationUnit entityCompilationUnit) {
//		String methodName = methodDeclaration.getName().getIdentifier();
//		String fieldName = methodName.substring(3);
//		FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
//		if (fieldInfo == null) {
//			fieldInfo = iEntityInfo.getFieldInfoByName(Introspector.decapitalize(fieldName));
//		}
//		if (fieldInfo == null) {
//			System.out.println("setter does not match field name: "+methodName);
//		}
//		else {
//			MethodDeclaration entityMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(entityCompilationUnit.getAST(), methodDeclaration);
//			entityMethodDeclaration.setJavadoc(null);
//			@SuppressWarnings("unchecked")
//			List<BodyDeclaration> bodyDeclarations = ((TypeDeclaration) entityCompilationUnit.types().get(0)).bodyDeclarations();
//			bodyDeclarations.add(entityMethodDeclaration);
//		}
//	}
	
//	private void portStandardMethod(MethodDeclaration methodDeclaration, CompilationUnit entityCompilationUnit) {
//		
//	}
//	
//	private void portUserMethod(MethodDeclaration methodDeclaration, CompilationUnit entityCompilationUnit) {
//		
//	}
	
	private boolean hasKeyFields(IType testClass) {
		boolean foundKeyField = false;
		try {
			IField[] testFields = testClass.getFields();
			for (IField testField : testFields) {
				FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(testField.getElementName());
				if (fieldInfo != null && fieldInfo.getIsKeyField()) {
					foundKeyField = true;
					break;
				}
			}
			if (!foundKeyField) {
				IMethod[] testMethods = testClass.getMethods();
				for (IMethod testMethod : testMethods) {
					FieldInfo fieldInfo = iEntityInfo.getFieldInfoByGetterName(testMethod.getElementName());
					if (fieldInfo != null && fieldInfo.getIsKeyField()) {
						foundKeyField = true;
						break;
					}
				}
			}
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		return foundKeyField;
	}
	
	private List<FieldDeclaration> getPortRequiredFieldDeclarations(TypeDeclaration ejbTypeDeclaration, List<FieldGenerator> fieldGenerators) {
		List<FieldDeclaration> fieldDeclarations = new ArrayList<FieldDeclaration>();
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = ejbTypeDeclaration.bodyDeclarations();
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			if (bodyDeclaration.getNodeType() == ASTNode.FIELD_DECLARATION) {
				FieldDeclaration fieldDeclaration = (FieldDeclaration) bodyDeclaration;
				@SuppressWarnings("unchecked")
				List<VariableDeclarationFragment> ejbVariableDeclarationFragments = fieldDeclaration.fragments();
				for (VariableDeclarationFragment ejbVariableDeclarationFragment : ejbVariableDeclarationFragments) {
					String fieldName = ejbVariableDeclarationFragment.getName().getIdentifier();
					boolean generatedField = false;
					if (fieldGenerators != null) {
						for (FieldGenerator fieldGenerator : fieldGenerators) {
							if (fieldName.equals(fieldGenerator.getFieldName())) {
								generatedField = true;
								break;
							}
						}
					}
					if (!generatedField && iEntityInfo.isRequiredField(fieldName)) {
						fieldDeclarations.add(fieldDeclaration);
						break;
					}
				}
			}
		}
		return fieldDeclarations;
	}
	
	private List<MethodDeclaration> getPortRequiredMethodDeclarations(TypeDeclaration ejbTypeDeclaration, List<MethodGenerator> methodGenerators) {
		List<MethodDeclaration> methodDeclarations = new ArrayList<MethodDeclaration>();
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = ejbTypeDeclaration.bodyDeclarations();
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			if (bodyDeclaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
				MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
				String methodKey = JavaUtil.getMethodKey(methodDeclaration);
				boolean generatedMethod = false;
				if (methodGenerators != null) {
					for (MethodGenerator methodGenerator : methodGenerators) {
						if (methodKey.equals(methodGenerator.getMethodKey())) {
							generatedMethod = true;
							break;
						}
					}
				}
				if (!generatedMethod) {
					UserMethodInfo userMethodInfo = iEntityInfo.getUserMethodInfo(methodKey);
					if ((userMethodInfo != null && userMethodInfo.getRelatedEntityInfo() == null && userMethodInfo.getFieldInfo() == null && userMethodInfo.getEjbRelationshipRoleInfo() == null) || iEntityInfo.isRequiredMethod(methodKey)) {
						methodDeclarations.add(methodDeclaration);
					}
				}
			}
		}
		return methodDeclarations;
	}
	
	private void portFieldDeclarations(TypeDeclaration entityTypeDeclaration, List<FieldDeclaration> fieldDeclarations) {
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = entityTypeDeclaration.bodyDeclarations();
		int index = 0;
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			if (bodyDeclaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
				break;
			}
			index++;
		}
		AST ast = entityTypeDeclaration.getAST();
		for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
			fieldDeclaration.accept(new EntityPortVisitor());
			FieldDeclaration entityFieldDeclaration = (FieldDeclaration) ASTNode.copySubtree(ast, fieldDeclaration);
			JavaUtil.setFieldProtected(entityFieldDeclaration);
			bodyDeclarations.add(index, entityFieldDeclaration);
			index++;
		}
	}
	
	private void portMethodDeclarations(TypeDeclaration entityTypeDeclaration, List<MethodDeclaration> methodDeclarations) {
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = entityTypeDeclaration.bodyDeclarations();
		int index = bodyDeclarations.size();
		AST ast = entityTypeDeclaration.getAST();
		for (MethodDeclaration methodDeclaration : methodDeclarations) {
			String methodName = methodDeclaration.getName().getIdentifier();
//			if (methodName.equals("getDefaultContractId")) {
//				System.out.println("getDefaultContractId");
//			}
			String methodKey = JavaUtil.getMethodKey(methodDeclaration);
			TargetExceptionInfo targetExceptionInfo = TargetExceptionUtil.getEjbMethodUnhandledTargetExceptions(iEntityInfo, methodKey);
			methodDeclaration.accept(new EntityPortVisitor());
//			methodDeclaration.getBody().accept(new UserMethodFieldGetterVisitor());
			MethodDeclaration entityMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(entityTypeDeclaration.getAST(), methodDeclaration);
			@SuppressWarnings("unchecked")
			List<Name> thrownExceptions = entityMethodDeclaration.thrownExceptions();
			thrownExceptions.clear();
			Collection<String> unhandledExceptions = TargetExceptionUtil.getFilteredExceptions(iModuleInfo.getJavaProject(), targetExceptionInfo.getTargetExceptions());
			for (String unhandledException : unhandledExceptions) {
				thrownExceptions.add(entityMethodDeclaration.getAST().newName(unhandledException));
			}
			@SuppressWarnings("unchecked")
			List<IExtendedModifier> modifiers = entityMethodDeclaration.modifiers();
			if ((methodName.startsWith("get") || methodName.startsWith("is")) && entityMethodDeclaration.getReturnType2() != null && entityMethodDeclaration.parameters().size() == 0) {
				MarkerAnnotation idAnnotation = ast.newMarkerAnnotation();
				idAnnotation.setTypeName(ast.newSimpleName("Transient"));
				modifiers.add(0, idAnnotation);
			}
//			entityMethodDeclaration.getBody().accept(new UserMethodFieldSetterVisitor());
//			entityMethodDeclaration.getBody().accept(new UserMethodFieldGetterVisitor());
			bodyDeclarations.add(index, entityMethodDeclaration);
			index++;
		}
	}
	
	private class EntityPortVisitor extends PortVisitor {
		public EntityPortVisitor() {
			super(iApplicationInfo, iModuleInfo.getJavaProject());
		}
		
		public String getTypeMapping(String typeName) {
			String typeMapping = null;
			if (iApplicationInfo.isEntityInterfaceType(typeName)) {
				EntityInfo entityInfo = iApplicationInfo.getEntityInfoForType(typeName);
				if (entityInfo.getRemote().equals(typeName)) {
					typeMapping = entityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName();
				}
			}
			return typeMapping != null ? typeMapping : super.getTypeMapping(typeName);
		}
		
		public boolean visit(SimpleName simpleName) {
			if (!simpleName.isDeclaration()) {
				FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(simpleName.getIdentifier());
				if (fieldInfo != null) {
					IBinding binding = simpleName.resolveBinding();
					if (binding instanceof IVariableBinding) {
						IVariableBinding variableBinding = (IVariableBinding) binding;
						if (variableBinding.getDeclaringClass() != null && iEntityInfo.isClassInEjbHierarchy(variableBinding.getDeclaringClass().getQualifiedName())) {
							replaceFieldReference(simpleName, fieldInfo);
						}
					}
				}
				else {
					super.visit(simpleName);
				}
			}
			return false;
		}
		
		public boolean visit(FieldAccess fieldAccess) {
			boolean visitChildren = true;
			if (fieldAccess.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
				FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldAccess.getName().getIdentifier());
				if (fieldInfo != null) {
					replaceFieldReference(fieldAccess, fieldInfo);
					visitChildren = false;
				}
			}
			else {
				visitChildren = super.visit(fieldAccess);
			}
			return visitChildren;
		}
		
		public boolean visit(SuperFieldAccess superFieldAccess) {
			boolean visitChildren = false;
			if (superFieldAccess.getQualifier() == null) {
				FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(superFieldAccess.getName().getIdentifier());
				if (fieldInfo != null) {
					replaceFieldReference(superFieldAccess, fieldInfo);
					visitChildren = false;
				}
			}
			return visitChildren;
		}
		
		public boolean visit(MethodInvocation methodInvocation) {
			boolean visitChildren = true;
			IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
			if (methodInvocation.getExpression() == null || methodInvocation.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
				if (methodBinding != null) {
					String methodKey = JavaUtil.getMethodKey(methodBinding);
					FieldInfo fieldInfo = iEntityInfo.getFieldInfoByGetterName(methodKey);
					if (fieldInfo != null) {
						replaceFieldReference(methodInvocation, fieldInfo);
						visitChildren = false;
					}
				}
			}
			else {
				visitChildren = super.visit(methodInvocation);
			}
			return visitChildren;
		}
		
		public boolean visit(Assignment assignment) {
			boolean visitChildren = true;
			FieldInfo fieldInfo = null;
			Expression leftHandSide = assignment.getLeftHandSide();
			switch (leftHandSide.getNodeType()) {
				case ASTNode.SIMPLE_NAME: {
					SimpleName simpleName = (SimpleName) leftHandSide;
					IBinding binding = simpleName.resolveBinding();
					if (binding instanceof IVariableBinding && ((IVariableBinding)binding).isField()) {
						fieldInfo = iEntityInfo.getFieldInfoByName(simpleName.getIdentifier());
					}
					break;
				}
				case ASTNode.FIELD_ACCESS: {
					FieldAccess fieldAccess = (FieldAccess) leftHandSide;
					if (fieldAccess.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
						fieldInfo = iEntityInfo.getFieldInfoByName(fieldAccess.getName().getIdentifier());
					}
					break;
				}
				case ASTNode.SUPER_FIELD_ACCESS: {
					SuperFieldAccess superFieldAccess = (SuperFieldAccess) leftHandSide;
					fieldInfo = iEntityInfo.getFieldInfoByName(superFieldAccess.getName().getIdentifier());
					break;
				}
			}
			if (fieldInfo != null) {
				if (fieldInfo.getRelatedEntityInfo() == null) {
					MethodInvocation setterMethodInvocation = assignment.getAST().newMethodInvocation();
					setterMethodInvocation.setName(assignment.getAST().newSimpleName(fieldInfo.getTargetSetterName()));
					@SuppressWarnings("unchecked")
					List<Expression> arguments = setterMethodInvocation.arguments();
					Expression rightHandSideExpression = assignment.getRightHandSide();
					rightHandSideExpression.accept(this);
					arguments.add((Expression) ASTNode.copySubtree(rightHandSideExpression.getAST(), rightHandSideExpression));
					replaceASTNode(assignment, setterMethodInvocation);
					visitChildren = false;
					assignment.getRightHandSide().accept(this);
				}
				else {
					System.out.println("user method calls related entity field setter");
				}
			}
			else {
				visitChildren = super.visit(assignment);
			}
			return visitChildren;
		}
		
		public boolean visit(ExpressionStatement expressionStatement) {
			boolean visitChildren = true;
			Expression statementExpression = expressionStatement.getExpression();
			if (statementExpression.getNodeType() == ASTNode.ASSIGNMENT) {
				Assignment assignment = (Assignment) statementExpression;
				FieldInfo fieldInfo = getAssignedField(assignment);
				if (fieldInfo != null && fieldInfo.getRelatedEntityInfo() != null && fieldInfo.getRelatedEntityInfo().getParentEntityInfo().getKeyFields().size() == 1) {
					//	orderId = (aProp.get(OrderFulfillmentStatusConstants.ORDER_ID) != null) ? new Long ((String) aProp.get(OrderFulfillmentStatusConstants.ORDER_ID)) : null;
					AST ast = expressionStatement.getAST();
					RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
					assignment.getRightHandSide().accept(this);
					Expression primaryKeyExpression = (Expression) ASTNode.copySubtree(ast, assignment.getRightHandSide());
					if (primaryKeyExpression.getNodeType() == ASTNode.CONDITIONAL_EXPRESSION) {
						ConditionalExpression conditionalExpression = (ConditionalExpression) primaryKeyExpression;
						if (conditionalExpression.getThenExpression().getNodeType() == ASTNode.NULL_LITERAL) {
							primaryKeyExpression = conditionalExpression.getElseExpression();
						}
						else if (conditionalExpression.getElseExpression().getNodeType() == ASTNode.NULL_LITERAL) {
							primaryKeyExpression = conditionalExpression.getThenExpression();
						}
					}
					InfixExpression infixExpression = ast.newInfixExpression();
					infixExpression.setOperator(Operator.NOT_EQUALS);
					infixExpression.setLeftOperand((Expression) ASTNode.copySubtree(ast, primaryKeyExpression));
					infixExpression.setRightOperand(ast.newNullLiteral());
					IfStatement ifStatement = ast.newIfStatement();
					ifStatement.setExpression(infixExpression);
					VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
					String accessBeanVariableName = relatedEntityInfo.getFieldName() + "AccessBean";
					variableDeclarationFragment.setName(ast.newSimpleName(accessBeanVariableName));
					ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
					classInstanceCreation.setType(ast.newSimpleType(ast.newName(relatedEntityInfo.getParentEntityInfo().getEntityAccessBeanClassInfo().getQualifiedClassName())));
					variableDeclarationFragment.setInitializer(classInstanceCreation);
					VariableDeclarationStatement variableDeclarationStatement = ast.newVariableDeclarationStatement(variableDeclarationFragment);
					variableDeclarationStatement.setType(ast.newSimpleType(ast.newName(relatedEntityInfo.getParentEntityInfo().getEntityAccessBeanClassInfo().getQualifiedClassName())));
					Block block = ast.newBlock();
					@SuppressWarnings("unchecked")
					List<Statement> newStatementList = block.statements();
					newStatementList.add(variableDeclarationStatement);
					if (!STRING.equals(fieldInfo.getTypeName())) {
						NullConstructorParameter nullConstructorParameter = relatedEntityInfo.getParentEntityInfo().getAccessBeanInfo().getNullConstructorParameterByName(fieldInfo.getFieldName());
						if (nullConstructorParameter != null && nullConstructorParameter.getConverterClassName() != null) {
							primaryKeyExpression = PrimaryKeyUtil.convertExpressionToString((Expression) ASTNode.copySubtree(ast, primaryKeyExpression));
						}
					}
					FieldInfo keyField = relatedEntityInfo.getParentEntityInfo().getKeyFields().get(0);
					MethodInvocation setInitKeyMethodInvocation = ast.newMethodInvocation();
					setInitKeyMethodInvocation.setName(ast.newSimpleName(SET_INIT_KEY + keyField.getTargetFieldName()));
					setInitKeyMethodInvocation.setExpression(ast.newSimpleName(accessBeanVariableName));
					@SuppressWarnings("unchecked")
					List<Expression> setInitKeyMethodInvocationArguments = setInitKeyMethodInvocation.arguments();
					setInitKeyMethodInvocationArguments.add(primaryKeyExpression);
					ExpressionStatement initKeyExpressionStatement = ast.newExpressionStatement(setInitKeyMethodInvocation);
					newStatementList.add(initKeyExpressionStatement);
					MethodInvocation instantiateEntityMethodInvocation = ast.newMethodInvocation();
					instantiateEntityMethodInvocation.setName(ast.newSimpleName(INSTANTIATE_ENTITY));
					instantiateEntityMethodInvocation.setExpression(ast.newSimpleName(accessBeanVariableName));
					ExpressionStatement instantiateEntityExpressionStatement = ast.newExpressionStatement(instantiateEntityMethodInvocation);
					newStatementList.add(instantiateEntityExpressionStatement);
					MethodInvocation getEntityMethodInvocation = ast.newMethodInvocation();
					getEntityMethodInvocation.setName(ast.newSimpleName("getEntity"));
					getEntityMethodInvocation.setExpression(ast.newSimpleName(accessBeanVariableName));
					MethodInvocation setRelatedEntityMethodInvocation = ast.newMethodInvocation();
					setRelatedEntityMethodInvocation.setName(ast.newSimpleName(relatedEntityInfo.getSetterName()));
					@SuppressWarnings("unchecked")
					List<Expression> arguments = setRelatedEntityMethodInvocation.arguments(); 
					arguments.add(getEntityMethodInvocation);
					newStatementList.add(ast.newExpressionStatement(setRelatedEntityMethodInvocation));
					ifStatement.setThenStatement(block);
					setRelatedEntityMethodInvocation = ast.newMethodInvocation();
					setRelatedEntityMethodInvocation.setName(ast.newSimpleName(relatedEntityInfo.getSetterName()));
					@SuppressWarnings("unchecked")
					List<Expression> nullArguments = setRelatedEntityMethodInvocation.arguments(); 
					nullArguments.add(ast.newNullLiteral());
					block = ast.newBlock();
					@SuppressWarnings("unchecked")
					List<Statement> elseStatements = block.statements();
					elseStatements.add(ast.newExpressionStatement(setRelatedEntityMethodInvocation));
					ifStatement.setElseStatement(block);
					replaceASTNode(expressionStatement, ifStatement);
					visitChildren = false;
				}
			}
			if (visitChildren) {
				visitChildren = super.visit(expressionStatement);
			}
			return visitChildren;
		}
		
		private FieldInfo getAssignedField(Assignment assignment) {
			FieldInfo fieldInfo = null;
			Expression leftHandSide = assignment.getLeftHandSide();
			switch (leftHandSide.getNodeType()) {
				case ASTNode.SIMPLE_NAME: {
					SimpleName simpleName = (SimpleName) leftHandSide;
					IBinding binding = simpleName.resolveBinding();
					if (binding instanceof IVariableBinding && ((IVariableBinding)binding).isField()) {
						fieldInfo = iEntityInfo.getFieldInfoByName(simpleName.getIdentifier());
					}
					break;
				}
				case ASTNode.FIELD_ACCESS: {
					FieldAccess fieldAccess = (FieldAccess) leftHandSide;
					if (fieldAccess.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
						fieldInfo = iEntityInfo.getFieldInfoByName(fieldAccess.getName().getIdentifier());
					}
					break;
				}
				case ASTNode.SUPER_FIELD_ACCESS: {
					SuperFieldAccess superFieldAccess = (SuperFieldAccess) leftHandSide;
					fieldInfo = iEntityInfo.getFieldInfoByName(superFieldAccess.getName().getIdentifier());
					break;
				}
			}
			return fieldInfo;
		}
		
		private void replaceFieldReference(ASTNode fieldReference, FieldInfo fieldInfo) {
			AST ast = fieldReference.getAST();
			if (fieldInfo.getRelatedEntityInfo() == null) {
				MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
				getterMethodInvocation.setName(ast.newSimpleName(fieldInfo.getTargetGetterName()));
				if (fieldInfo.getColumnInfo() != null && CHAR.equals(fieldInfo.getColumnInfo().getTypeName()) && fieldInfo.getColumnInfo().getLength() != null && fieldInfo.getColumnInfo().getLength() > 1) {
					MethodInvocation trimMethodInvocation = ast.newMethodInvocation();
					trimMethodInvocation.setName(ast.newSimpleName(TRIM));
					trimMethodInvocation.setExpression(getterMethodInvocation);
					if (fieldInfo.getColumnInfo().getNullable()) {
						InfixExpression infixExpression = ast.newInfixExpression();
						infixExpression.setLeftOperand((Expression) ASTNode.copySubtree(ast, getterMethodInvocation));
						infixExpression.setOperator(Operator.EQUALS);
						infixExpression.setRightOperand(ast.newNullLiteral());
						ConditionalExpression conditionalExpression = ast.newConditionalExpression();
						conditionalExpression.setExpression(infixExpression);
						conditionalExpression.setThenExpression(ast.newNullLiteral());
						conditionalExpression.setElseExpression(trimMethodInvocation);
						replaceASTNode(fieldReference, conditionalExpression);
					}
					else {
						replaceASTNode(fieldReference, trimMethodInvocation);
					}
				}
				else {
					replaceASTNode(fieldReference, getterMethodInvocation);
				}
			}
			else {
				RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
				String relatedEntityGetterName = relatedEntityInfo.getGetterName();
				if (relatedEntityGetterName == null) {
					relatedEntityGetterName = "get" + Character.toUpperCase(relatedEntityInfo.getFieldName().charAt(0)) + relatedEntityInfo.getFieldName().substring(1);
				}
				String referencedFieldGetterName = fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null ? fieldInfo.getReferencedFieldInfo().getTargetGetterName() : fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo().getGetterName();
				ConditionalExpression conditionalExpression = ast.newConditionalExpression();
				MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
				getterMethodInvocation.setName(ast.newSimpleName(relatedEntityGetterName));
				InfixExpression infixExpression = ast.newInfixExpression();
				infixExpression.setLeftOperand(getterMethodInvocation);
				infixExpression.setOperator(Operator.EQUALS);
				infixExpression.setRightOperand(ast.newNullLiteral());
				conditionalExpression.setExpression(infixExpression);
				conditionalExpression.setThenExpression(ast.newNullLiteral());
				getterMethodInvocation = (MethodInvocation) ASTNode.copySubtree(ast, getterMethodInvocation);
				MethodInvocation fieldMethodInvocation = ast.newMethodInvocation();
				fieldMethodInvocation.setExpression(getterMethodInvocation);
				fieldMethodInvocation.setName(ast.newSimpleName(referencedFieldGetterName));
				if (fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null) {
					ColumnInfo referencedColumnInfo = fieldInfo.getReferencedFieldInfo().getColumnInfo();
					if (referencedColumnInfo != null && CHAR.equals(referencedColumnInfo.getTypeName()) && referencedColumnInfo.getLength() != null && referencedColumnInfo.getLength() > 1) {
						MethodInvocation trimMethodInvocation = ast.newMethodInvocation();
						trimMethodInvocation.setName(ast.newSimpleName(TRIM));
						trimMethodInvocation.setExpression(fieldMethodInvocation);
						conditionalExpression.setElseExpression(trimMethodInvocation);
					}
					else {
						conditionalExpression.setElseExpression(fieldMethodInvocation);
					}
				}
				else {
					MethodInvocation relatedFieldMethodInvocation = ast.newMethodInvocation();
					relatedFieldMethodInvocation.setExpression(fieldMethodInvocation);
					relatedFieldMethodInvocation.setName(ast.newSimpleName(fieldInfo.getReferencedFieldInfo().getReferencedFieldInfo().getTargetGetterName()));
					conditionalExpression.setElseExpression(relatedFieldMethodInvocation);
				}
				ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
				parenthesizedExpression.setExpression(conditionalExpression);
				replaceASTNode(fieldReference, parenthesizedExpression);
			}
		}
		
		public boolean visit(CastExpression castExpression) {
			boolean visitChildren = true;
			if (AccessBeanUtil.isGetEntityCastExpression(iApplicationInfo, castExpression)) {
				visitChildren = AccessBeanUtil.portGetEntityCastExpression(iApplicationInfo, castExpression, this);
			}
			else {
				visitChildren = super.visit(castExpression);
			}
			return visitChildren;
		}
	}
	
//	private class UserMethodFieldGetterVisitor extends ASTVisitor {
//		public boolean visit(SimpleName simpleName) {
//			if (!simpleName.isDeclaration()) {
//				FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(simpleName.getIdentifier());
//				if (fieldInfo != null) {
//					IBinding binding = simpleName.resolveBinding();
//					if (binding instanceof IVariableBinding) {
//						IVariableBinding variableBinding = (IVariableBinding) binding;
//						if (variableBinding.getDeclaringClass() != null && iEntityInfo.isClassInEjbHierarchy(variableBinding.getDeclaringClass().getQualifiedName())) {
//							replaceFieldReference(simpleName, fieldInfo);
//						}
//					}
//				}
//				else {
//					IBinding binding = simpleName.resolveBinding();
//					if (binding != null) {
//						if (binding.getKind() == ITypeBinding.TYPE) {
//							ITypeBinding typeBinding = (ITypeBinding) binding;
//							String qualifiedTypeName = typeBinding.getQualifiedName();
//							if (qualifiedTypeName.equals(iEntityInfo.getEjbType().getFullyQualifiedName())) {
//								JavaUtil.replaceASTNode(simpleName, simpleName.getAST().newName(iEntityClassInfo.getClassName()));
//							}
//							else if (qualifiedTypeName.equals(iEntityInfo.getEjbBaseType() == null ? null : iEntityInfo.getEjbBaseType().getFullyQualifiedName())) {
//								JavaUtil.replaceASTNode(simpleName, simpleName.getAST().newName(iEntityBaseClassInfo.getClassName()));
//							}
//						}
//					}
//				}
//			}
//			return false;
//		}
//		
//		public boolean visit(QualifiedName qualifiedName) {
//			return false;
//		}
//		
//		public boolean visit(FieldAccess fieldAccess) {
//			if (fieldAccess.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
//				FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldAccess.getName().getIdentifier());
//				if (fieldInfo != null) {
//					replaceFieldReference(fieldAccess, fieldInfo);
//				}
//			}
//			return false;
//		}
//		
//		public boolean visit(SuperFieldAccess superFieldAccess) {
//			if (superFieldAccess.getQualifier() == null) {
//				FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(superFieldAccess.getName().getIdentifier());
//				if (fieldInfo != null) {
//					replaceFieldReference(superFieldAccess, fieldInfo);
//				}
//			}
//			return false;
//		}
//		
//		public boolean visit(MethodInvocation methodInvocation) {
//			boolean visitChildren = true;
//			if (methodInvocation.getExpression() == null || methodInvocation.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
//				IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
//				if (methodBinding != null) {
//					String methodKey = JavaUtil.getMethodKey(methodBinding);
//					FieldInfo fieldInfo = iEntityInfo.getFieldInfoByGetterName(methodKey);
//					if (fieldInfo != null) {
//						replaceFieldReference(methodInvocation, fieldInfo);
//						visitChildren = false;
//					}
//				}
//			}
//			return visitChildren;
//		}
//		
//		private void replaceFieldReference(ASTNode fieldReference, FieldInfo fieldInfo) {
//			AST ast = fieldReference.getAST();
//			if (fieldInfo.getRelatedEntityInfo() == null) {
//				MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
//				getterMethodInvocation.setName(ast.newSimpleName(fieldInfo.getTargetGetterName()));
//				JavaUtil.replaceASTNode(fieldReference, getterMethodInvocation);
//			}
//			else {
//				RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
//				String relatedEntityGetterName = relatedEntityInfo.getGetterName();
//				if (relatedEntityGetterName == null) {
//					relatedEntityGetterName = "get" + Character.toUpperCase(relatedEntityInfo.getFieldName().charAt(0)) + relatedEntityInfo.getFieldName().substring(1);
//				}
//				String referencedFieldGetterName = fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null ? fieldInfo.getReferencedFieldInfo().getTargetGetterName() : fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo().getGetterName();
//				ConditionalExpression conditionalExpression = ast.newConditionalExpression();
//				MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
//				getterMethodInvocation.setName(ast.newSimpleName(relatedEntityGetterName));
//				InfixExpression infixExpression = ast.newInfixExpression();
//				infixExpression.setLeftOperand(getterMethodInvocation);
//				infixExpression.setOperator(Operator.EQUALS);
//				infixExpression.setRightOperand(ast.newNullLiteral());
//				conditionalExpression.setExpression(infixExpression);
//				conditionalExpression.setThenExpression(ast.newNullLiteral());
//				getterMethodInvocation = (MethodInvocation) ASTNode.copySubtree(ast, getterMethodInvocation);
//				MethodInvocation fieldMethodInvocation = ast.newMethodInvocation();
//				fieldMethodInvocation.setExpression(getterMethodInvocation);
//				fieldMethodInvocation.setName(ast.newSimpleName(referencedFieldGetterName));
//				if (fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null) {
//					conditionalExpression.setElseExpression(fieldMethodInvocation);
//				}
//				else {
//					MethodInvocation relatedFieldMethodInvocation = ast.newMethodInvocation();
//					relatedFieldMethodInvocation.setExpression(fieldMethodInvocation);
//					relatedFieldMethodInvocation.setName(ast.newSimpleName(fieldInfo.getReferencedFieldInfo().getReferencedFieldInfo().getTargetGetterName()));
//					conditionalExpression.setElseExpression(relatedFieldMethodInvocation);
//				}
//				JavaUtil.replaceASTNode(fieldReference, conditionalExpression);
//			}
//		}
//	}
}
