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

import java.util.Collection;
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
import org.eclipse.jdt.core.dom.Expression;

import com.ibm.commerce.jpa.port.info.AccessBeanInfo;
import com.ibm.commerce.jpa.port.info.AccessBeanInfo.CopyHelperProperty;
import com.ibm.commerce.jpa.port.info.AccessBeanInfo.NullConstructorParameter;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanCatchClause;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanEntityCreationDataInitializedFieldStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanExpressionInitializedFieldStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanIfStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanInstanceVariableAssignmentStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanKeyParameterInitializedFieldStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanMethodInvocationStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanParameterInitializedFieldStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanReturnStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanSuperMethodInvocationStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanTryStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanUserMethodInvocationStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanVariableAssignmentStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanVariableDeclarationStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanVariableMethodInvocationStatement;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ClassInfo;
import com.ibm.commerce.jpa.port.info.ColumnInfo;
import com.ibm.commerce.jpa.port.info.CreatorInfo;
import com.ibm.commerce.jpa.port.info.EjbRelationshipRoleInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.FinderInfo;
import com.ibm.commerce.jpa.port.info.InstanceVariableInfo;
import com.ibm.commerce.jpa.port.info.KeyClassConstructorInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.RelatedEntityInfo;
import com.ibm.commerce.jpa.port.info.StaticFieldInfo;
import com.ibm.commerce.jpa.port.info.TableInfo;
import com.ibm.commerce.jpa.port.info.TargetExceptionInfo;
import com.ibm.commerce.jpa.port.info.UserMethodInfo;
import com.ibm.commerce.jpa.port.util.AccessBeanUtil;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.ImportUtil;
import com.ibm.commerce.jpa.port.util.JavaUtil;
import com.ibm.commerce.jpa.port.util.TargetExceptionUtil;

public class EntityAccessBeanClassGenerator {
	private static final String UTF_SORTING_ATTRIBUTE = "com.ibm.commerce.utf.helper.SortingAttribute";
	private static final String NEGOTIATION_SORTING_ATTRIBUTE = "com.ibm.commerce.negotiation.util.SortingAttribute";
	private static final String SORTING_ATTRIBUE = "com.ibm.commerce.base.util.SortingAttribute";
	private static final String CHAR = "CHAR";
	private static final Map<String, String> TYPE_MAP;
	private static Map<String, String> STRING_CONVERTER_TO_METHODS;
	private static Map<String, String> STRING_CONVERTER_FROM_METHODS;
	static {
		TYPE_MAP = new HashMap<String, String>();
		TYPE_MAP.put(UTF_SORTING_ATTRIBUTE, SORTING_ATTRIBUE);
		TYPE_MAP.put(NEGOTIATION_SORTING_ATTRIBUTE, SORTING_ATTRIBUE);
		STRING_CONVERTER_TO_METHODS = new HashMap<String, String>();
		STRING_CONVERTER_TO_METHODS.put("java.math.BigDecimal", "BigDecimalToString");
		STRING_CONVERTER_TO_METHODS.put("java.lang.Boolean", "BooleanToString");
		STRING_CONVERTER_TO_METHODS.put("boolean", "booleanToString");
		STRING_CONVERTER_TO_METHODS.put("byte[]", "byteArrayToString");
		STRING_CONVERTER_TO_METHODS.put("byte", "byteToString");
		STRING_CONVERTER_TO_METHODS.put("java.lang.Byte", "ByteToString");
		STRING_CONVERTER_TO_METHODS.put("com.ibm.icu.util.Calendar", "CalendarToString");
		STRING_CONVERTER_TO_METHODS.put("java.lang.Character", "CharacterToString");
		STRING_CONVERTER_TO_METHODS.put("char", "charToString");
		STRING_CONVERTER_TO_METHODS.put("double", "doubleToString");
		STRING_CONVERTER_TO_METHODS.put("java.lang.Double", "DoubleToString");
		STRING_CONVERTER_TO_METHODS.put("float", "floatToString");
		STRING_CONVERTER_TO_METHODS.put("java.lang.Float", "FloatToString");
		STRING_CONVERTER_TO_METHODS.put("java.lang.Integer", "IntegerToString");
		STRING_CONVERTER_TO_METHODS.put("int", "intToString");
		STRING_CONVERTER_TO_METHODS.put("long", "longToString");
		STRING_CONVERTER_TO_METHODS.put("java.lang.Long", "LongToString");
		STRING_CONVERTER_TO_METHODS.put("java.lang.Short", "ShortToString");
		STRING_CONVERTER_TO_METHODS.put("short", "shortToString");
		STRING_CONVERTER_TO_METHODS.put("java.sql.Timestamp", "TimestampToString");
		STRING_CONVERTER_TO_METHODS.put("java.sql.Date", "DateToString");
		STRING_CONVERTER_FROM_METHODS = new HashMap<String, String>();
		STRING_CONVERTER_FROM_METHODS.put("java.math.BigDecimal", "StringToBigDecimal");
		STRING_CONVERTER_FROM_METHODS.put("java.lang.Boolean", "StringToBoolean");
		STRING_CONVERTER_FROM_METHODS.put("boolean", "StringToboolean");
		STRING_CONVERTER_FROM_METHODS.put("byte", "StringTobyte");
		STRING_CONVERTER_FROM_METHODS.put("java.lang.Byte", "StringToByte");
		STRING_CONVERTER_FROM_METHODS.put("byte[]", "StringTobyteArray");
		STRING_CONVERTER_FROM_METHODS.put("com.ibm.icu.util.Calendar", "StringToCalendar");
		STRING_CONVERTER_FROM_METHODS.put("char", "StringTochar");
		STRING_CONVERTER_FROM_METHODS.put("java.lang.Character", "StringToCharacter");
		STRING_CONVERTER_FROM_METHODS.put("double", "StringTodouble");
		STRING_CONVERTER_FROM_METHODS.put("java.lang.Double", "StringToDouble");
		STRING_CONVERTER_FROM_METHODS.put("float", "StringTofloat");
		STRING_CONVERTER_FROM_METHODS.put("java.lang.Float", "StringToFloat");
		STRING_CONVERTER_FROM_METHODS.put("int", "StringToint");
		STRING_CONVERTER_FROM_METHODS.put("java.lang.Integer", "StringToInteger");
		STRING_CONVERTER_FROM_METHODS.put("long", "StringTolong");
		STRING_CONVERTER_FROM_METHODS.put("java.lang.Long", "StringToLong");
		STRING_CONVERTER_FROM_METHODS.put("java.lang.Short", "StringToShort");
		STRING_CONVERTER_FROM_METHODS.put("short", "StringToshort");
		STRING_CONVERTER_FROM_METHODS.put("java.sql.Timestamp", "StringToTimestamp");
		STRING_CONVERTER_FROM_METHODS.put("java.sql.Date", "StringToDate");
	}
	private static final Map<String, Map<String, MethodGenerator>> METHOD_GENERATORS_MAP;
	static {
		METHOD_GENERATORS_MAP = new HashMap<String, Map<String, MethodGenerator>>();
		Map<String, MethodGenerator> methodGenerators = new HashMap<String, MethodGenerator>();
		MethodGenerator methodGenerator = new AttributeValueEntityAccessBeanGenerator.AttributeValueEntityCreationDataConstructorGenerator(); 
		methodGenerators.put(methodGenerator.getMethodKey(), methodGenerator);
		METHOD_GENERATORS_MAP.put(AttributeValueEntityAccessBeanGenerator.ATTRIBUTE_VALUE_TYPE_NAME, methodGenerators);
		methodGenerators = new HashMap<String, MethodGenerator>();
		methodGenerator = new AttributeValueEntityAccessBeanGenerator.AttributeFloatValueEntityCreationDataConstructorGenerator();
		methodGenerators.put(methodGenerator.getMethodKey(), methodGenerator);
		METHOD_GENERATORS_MAP.put(AttributeValueEntityAccessBeanGenerator.ATTRIBUTE_FLOAT_VALUE_TYPE_NAME, methodGenerators);
		methodGenerators = new HashMap<String, MethodGenerator>();
		methodGenerator = new AttributeValueEntityAccessBeanGenerator.AttributeIntegerValueEntityCreationDataConstructorGenerator();
		methodGenerators.put(methodGenerator.getMethodKey(), methodGenerator);
		METHOD_GENERATORS_MAP.put(AttributeValueEntityAccessBeanGenerator.ATTRIBUTE_INTEGER_VALUE_TYPE_NAME, methodGenerators);
		methodGenerators = new HashMap<String, MethodGenerator>();
		methodGenerator = new AttributeValueEntityAccessBeanGenerator.AttributeStringValueEntityCreationDataConstructorGenerator();
		methodGenerators.put(methodGenerator.getMethodKey(), methodGenerator);
		METHOD_GENERATORS_MAP.put(AttributeValueEntityAccessBeanGenerator.ATTRIBUTE_STRING_VALUE_TYPE_NAME, methodGenerators);
	}
	
	private BackupUtil iBackupUtil;
	private EntityInfo iEntityInfo;
	private ModuleInfo iModuleInfo;
	private ApplicationInfo iApplicationInfo;
	private ClassInfo iEntityClassInfo;
	private ClassInfo iEntityKeyClassInfo;
	private ClassInfo iEntityAccessBeanClassInfo;
	private ClassInfo iEntityQueryHelperClassInfo;
	private AccessBeanInfo iAccessBeanInfo;
	private Collection<String> iProcessedStaticFieldNames = new HashSet<String>();
	private Map<String, MethodGenerator> iMethodGenerators;
	
	public EntityAccessBeanClassGenerator(BackupUtil backupUtil, EntityInfo entityInfo) {
		iBackupUtil = backupUtil;
		iEntityInfo = entityInfo;
		iModuleInfo = entityInfo.getModuleInfo();
		iApplicationInfo = iModuleInfo.getApplicationInfo();
		iEntityClassInfo = entityInfo.getEntityClassInfo();
		iEntityKeyClassInfo = entityInfo.getEntityKeyClassInfo();
		iEntityAccessBeanClassInfo = entityInfo.getEntityAccessBeanClassInfo();
		iEntityQueryHelperClassInfo = entityInfo.getEntityQueryHelperClassInfo();
		iAccessBeanInfo = entityInfo.getAccessBeanInfo();
		iMethodGenerators = METHOD_GENERATORS_MAP.get(iEntityAccessBeanClassInfo.getQualifiedClassName());
	}

	public void generate(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("generate entity access bean for " + iEntityInfo.getEjbName(), 1600);
			StringBuilder sb = new StringBuilder();
			sb.append("package ");
			sb.append(iEntityAccessBeanClassInfo.getPackageFragment().getElementName());
			sb.append(";\r\n");
			JavaUtil.appendCopyrightComment(sb);
			appendImports(sb);
			sb.append("\r\npublic class ");
			sb.append(iEntityAccessBeanClassInfo.getClassName());
			sb.append(" extends AbstractJpaEntityAccessBean {\r\n");
			JavaUtil.appendCopyrightField(sb);
			appendStaticFields(sb);
			sb.append("\r\n\tprivate ");
			sb.append(iEntityClassInfo.getClassName());
			sb.append(" iTypedEntity;\r\n");
			progressMonitor.worked(100);
			appendInitKeyDeclarations(sb);
			progressMonitor.worked(100);
			appendFindKeyDeclarations(sb);
			progressMonitor.worked(100);
			appendConstructors(sb);
			progressMonitor.worked(100);
			appendAccessBeanMethods(sb);
			progressMonitor.worked(100);
			appendInitKeySetters(sb);
			progressMonitor.worked(100);
			appendFinderMethods(sb);
			progressMonitor.worked(100);
			appendGettersAndSetters(sb);
			progressMonitor.worked(100);
			appendUserMethods(sb);
			progressMonitor.worked(100);
			appendAccessHelperMethods(sb);
			progressMonitor.worked(100);
			appendInstantiateEntityMethod(sb);
			progressMonitor.worked(100);
			appendStandardMethods(sb);
			progressMonitor.worked(100);
			appendFindRelatedEntityMethods(sb);
			progressMonitor.worked(100);
			sb.append("}");
			try {
				ICompilationUnit compilationUnit = iEntityAccessBeanClassInfo.getPackageFragment().createCompilationUnit(iEntityAccessBeanClassInfo.getClassName() + ".java", sb.toString(), true, new SubProgressMonitor(progressMonitor, 100));
//				ImportUtil.resolveImports(iASTParser, compilationUnit, new SubProgressMonitor(progressMonitor, 100));
				iBackupUtil.addGeneratedFile((IFile) compilationUnit.getResource(), new SubProgressMonitor(progressMonitor, 100));
				iApplicationInfo.incrementGeneratedAssetCount();
			}
			catch (CoreException e) {
				e.printStackTrace();
			}
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private void appendImports(StringBuilder sb) {
		sb.append("\r\nimport javax.persistence.*;\r\n");
		sb.append("import com.ibm.commerce.persistence.AbstractJpaEntityAccessBean;\r\n");
		Set<AccessBeanMethodInfo> methods = new HashSet<AccessBeanMethodInfo>();
		Collection<String> processedAccessBeanMethods = new HashSet<String>();
		Collection<CreatorInfo> creators = iEntityInfo.getCreators();
		for (CreatorInfo creator : creators) {
			if (!creator.isInvalid()) {
				appendAccessBeanMethodImports(sb, creator.getAccessBeanMethodInfo());
				methods.add(creator.getAccessBeanMethodInfo());
			}
		}
		Collection<FieldInfo> fields = iEntityInfo.getFields();
		for (FieldInfo fieldInfo : fields) {
			if (fieldInfo.getGetterAccessBeanMethodInfo() != null) {
				appendAccessBeanMethodImports(sb, fieldInfo.getGetterAccessBeanMethodInfo());
				methods.add(fieldInfo.getGetterAccessBeanMethodInfo());
			}
			if (fieldInfo.getSetterAccessBeanMethodInfo() != null) {
				appendAccessBeanMethodImports(sb, fieldInfo.getSetterAccessBeanMethodInfo());
				methods.add(fieldInfo.getSetterAccessBeanMethodInfo());
			}
		}
		List<UserMethodInfo> userMethods = iEntityInfo.getUserMethods();
		for (UserMethodInfo userMethodInfo : userMethods) {
			if (userMethodInfo.getAccessBeanMethodInfo() != null) {
				appendAccessBeanMethodImports(sb, userMethodInfo.getAccessBeanMethodInfo());
				methods.add(userMethodInfo.getAccessBeanMethodInfo());
			}
		}
		while (!methods.isEmpty()) {
			Set<AccessBeanMethodInfo> newMethods = new HashSet<AccessBeanMethodInfo>();
			for (AccessBeanMethodInfo referencingMethod : methods) {
				Collection<String> referencedMethods = referencingMethod.getReferencedAccessBeanMethods();
				for (String methodKey : referencedMethods) {
					if (!processedAccessBeanMethods.contains(methodKey)) {
						processedAccessBeanMethods.add(methodKey);
						AccessBeanMethodInfo methodInfo = iEntityInfo.getAccessBeanMethodInfo(methodKey);
						if (methodInfo != null && !methodInfo.isInvalid() && !methodInfo.isEmptyMethod()) {
							newMethods.add(methodInfo);
							appendAccessBeanMethodImports(sb, methodInfo);
						}
					}
				}
			}
			methods = newMethods;
		}
	}
	
	private void appendAccessBeanMethodImports(StringBuilder sb, AccessBeanMethodInfo methodInfo) {
		if (methodInfo.getCallsSuperAccessBeanMethod() && methodInfo.getSuperAccessBeanMethodInfo() != null) {
			appendAccessBeanMethodImports(sb, methodInfo.getSuperAccessBeanMethodInfo());
		}
		Collection<String> staticFieldNames = methodInfo.getReferencedStaticFieldNames();
		for (String fieldName : staticFieldNames) {
			StaticFieldInfo staticFieldInfo = iEntityInfo.getEjbStaticFieldInfo(fieldName);
			ImportUtil.appendImports(staticFieldInfo.getInitializationExpression(), sb);
		}
		Collection<String> instanceVariableNames = methodInfo.getReferencedInstanceVariableNames();
		for (String variableName : instanceVariableNames) {
			InstanceVariableInfo instanceVariableInfo = iEntityInfo.getEjbInstanceVariableInfo(variableName);
			if (instanceVariableInfo != null) {
				if (instanceVariableInfo.getInitializationExpression() != null) {
					ImportUtil.appendImports(instanceVariableInfo.getInitializationExpression(), sb);
				}
			}
			else {
				System.out.println("missing instance variable info for "+variableName);
			}
		}
		appendAccessBeanStatementImports(sb, methodInfo.getStatements());
	}
	
	private void appendAccessBeanStatementImports(StringBuilder sb, List<AccessBeanStatement> statements) {
		for (AccessBeanStatement statement : statements) {
			if (statement instanceof AccessBeanExpressionInitializedFieldStatement) {
				ImportUtil.appendImports(((AccessBeanExpressionInitializedFieldStatement) statement).getExpression(), sb);
			}
			else if (statement instanceof AccessBeanVariableDeclarationStatement) {
				ImportUtil.appendImports(((AccessBeanVariableDeclarationStatement) statement).getInitializationExpression(), sb);
			}
			else if (statement instanceof AccessBeanVariableAssignmentStatement) {
				ImportUtil.appendImports(((AccessBeanVariableAssignmentStatement) statement).getAssignmentExpression(), sb);
			}
			else if (statement instanceof AccessBeanVariableMethodInvocationStatement) {
				List<Expression> arguments = ((AccessBeanVariableMethodInvocationStatement) statement).getArguments();
				for (Expression argument : arguments) {
					ImportUtil.appendImports(argument, sb);
				}
			}
			else if (statement instanceof AccessBeanInstanceVariableAssignmentStatement) {
				ImportUtil.appendImports(((AccessBeanInstanceVariableAssignmentStatement) statement).getAssignmentExpression(), sb);
			}
			else if (statement instanceof AccessBeanIfStatement) {
				AccessBeanIfStatement ifStatement = (AccessBeanIfStatement) statement;
				ImportUtil.appendImports(ifStatement.getIfExpression(), sb);
				appendAccessBeanStatementImports(sb, ifStatement.getThenStatements());
				appendAccessBeanStatementImports(sb, ifStatement.getElseStatements());
			}
			else if (statement instanceof AccessBeanMethodInvocationStatement) {
				List<Expression> arguments = ((AccessBeanMethodInvocationStatement) statement).getArguments();
				for (Expression argument : arguments) {
					ImportUtil.appendImports(argument, sb);
				}
			}
			else if (statement instanceof AccessBeanUserMethodInvocationStatement) {
				List<Expression> arguments = ((AccessBeanUserMethodInvocationStatement) statement).getArguments();
				for (Expression argument : arguments) {
					ImportUtil.appendImports(argument, sb);
				}
			}
			else if (statement instanceof AccessBeanReturnStatement) {
				ImportUtil.appendImports(((AccessBeanReturnStatement) statement).getReturnExpression(), sb);
			}
			else if (statement instanceof AccessBeanTryStatement) {
				AccessBeanTryStatement tryStatement = (AccessBeanTryStatement) statement;
				appendAccessBeanStatementImports(sb, tryStatement.getTryStatements());
				List<AccessBeanCatchClause> catchClauses = tryStatement.getCatchClauses();
				for (AccessBeanCatchClause catchClause : catchClauses) {
					appendAccessBeanStatementImports(sb, catchClause.getCatchStatements());
				}
				appendAccessBeanStatementImports(sb, tryStatement.getFinallyStatements());
			}
		}
	}
	
	private void appendStaticFields(StringBuilder sb) {
		Set<AccessBeanMethodInfo> methods = new HashSet<AccessBeanMethodInfo>();
		Collection<String> processedMethods = new HashSet<String>();
		Collection<CreatorInfo> creators = iEntityInfo.getCreators();
		for (CreatorInfo creator : creators) {
			if (!creator.isInvalid()) {
				appendAccessBeanMethodStaticFields(sb, creator.getAccessBeanMethodInfo());
				methods.add(creator.getAccessBeanMethodInfo());
			}
		}
		Collection<FieldInfo> fields = iEntityInfo.getFields();
		for (FieldInfo fieldInfo : fields) {
			if (fieldInfo.getGetterAccessBeanMethodInfo() != null) {
				appendAccessBeanMethodStaticFields(sb, fieldInfo.getGetterAccessBeanMethodInfo());
				methods.add(fieldInfo.getGetterAccessBeanMethodInfo());
			}
			if (fieldInfo.getSetterAccessBeanMethodInfo() != null) {
				appendAccessBeanMethodStaticFields(sb, fieldInfo.getSetterAccessBeanMethodInfo());
				methods.add(fieldInfo.getSetterAccessBeanMethodInfo());
			}
		}
		List<UserMethodInfo> userMethods = iEntityInfo.getUserMethods();
		for (UserMethodInfo userMethodInfo : userMethods) {
			if (userMethodInfo.getAccessBeanMethodInfo() != null) {
				appendAccessBeanMethodStaticFields(sb, userMethodInfo.getAccessBeanMethodInfo());
				methods.add(userMethodInfo.getAccessBeanMethodInfo());
			}
		}
		while (!methods.isEmpty()) {
			Set<AccessBeanMethodInfo> newMethods = new HashSet<AccessBeanMethodInfo>();
			for (AccessBeanMethodInfo referencingMethodInfo : methods) {
				Collection<String> referencedMethods = referencingMethodInfo.getReferencedAccessBeanMethods();
				for (String methodKey : referencedMethods) {
					if (!processedMethods.contains(methodKey)) {
						processedMethods.add(methodKey);
						AccessBeanMethodInfo methodInfo = iEntityInfo.getAccessBeanMethodInfo(methodKey);
						if (methodInfo != null && !methodInfo.isInvalid() && !methodInfo.isEmptyMethod()) {
							newMethods.add(methodInfo);
							appendAccessBeanMethodStaticFields(sb, methodInfo);
						}
					}
				}
			}
			methods = newMethods;
		}
	}
		
	private void appendAccessBeanMethodStaticFields(StringBuilder sb, AccessBeanMethodInfo methodInfo) {
		Collection<String> staticFieldNames = methodInfo.getReferencedStaticFieldNames();
		for (String fieldName : staticFieldNames) {
			if (!iProcessedStaticFieldNames.contains(fieldName)) {
				iProcessedStaticFieldNames.add(fieldName);
				StaticFieldInfo staticFieldInfo = iEntityInfo.getEjbStaticFieldInfo(fieldName);
				sb.append("\r\n\tprivate static final ");
				sb.append(staticFieldInfo.getType());
				sb.append(" ");
				sb.append(staticFieldInfo.getVariableName());
				sb.append(" = ");
				sb.append(staticFieldInfo.getInitializationExpression());
				sb.append(";");
			}
		}
	}
	
	private void appendInitKeyDeclarations(StringBuilder sb) {
		List<FieldInfo> keyFields = iEntityInfo.getKeyFields();
		for (FieldInfo keyFieldInfo : keyFields) {
			if (keyFieldInfo.getRelatedEntityInfo() == null) {
				sb.append("\r\n\tprivate ");
				sb.append(keyFieldInfo.getTypeName());
				sb.append(" iInitKey_");
				sb.append(keyFieldInfo.getTargetFieldName());
				sb.append(";\r\n");
			}
		}
		List<RelatedEntityInfo> keyRelatedEntities = iEntityInfo.getKeyRelatedEntities();
		for (RelatedEntityInfo relatedEntityInfo : keyRelatedEntities) {
			sb.append("\r\n\tprivate ");
			sb.append(relatedEntityInfo.getKeyFieldType());
			sb.append(" iInitKey_");
			sb.append(relatedEntityInfo.getFieldName());
			sb.append(";\r\n");
		}
	}

	private void appendFindKeyDeclarations(StringBuilder sb) {
		List<RelatedEntityInfo> relatedEntities = iEntityInfo.getRelatedEntities();
		for (RelatedEntityInfo relatedEntityInfo : relatedEntities) {
			if (relatedEntityInfo.getMemberFields().size() > 1) {
				sb.append("\r\n\tprivate ");
				sb.append(relatedEntityInfo.getParentEntityInfo().getEntityKeyClassInfo().getQualifiedClassName());
				sb.append(" iFindKey_");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(";\r\n");
			}
		}
	}
	
	private void appendConstructors(StringBuilder sb) {
		sb.append("\r\n\tpublic ");
		sb.append(iEntityAccessBeanClassInfo.getClassName());
		sb.append("() {\r\n\t}\r\n");
		sb.append("\r\n\tpublic ");
		sb.append(iEntityAccessBeanClassInfo.getClassName());
		sb.append("(");
		sb.append(iEntityClassInfo.getQualifiedClassName());
		sb.append(" entity) {\r\n\t\tsetEntity(entity);\r\n\t}\r\n");
		Collection<CreatorInfo> creators = iEntityInfo.getCreators();
		for (CreatorInfo creator : creators) {
			if (!creator.isInvalid()) {
				AccessBeanMethodInfo methodInfo = creator.getAccessBeanMethodInfo();
				MethodGenerator methodGenerator = iMethodGenerators != null ? iMethodGenerators.get(methodInfo.getMethodKey()) : null;
				if (methodGenerator != null) {
					methodGenerator.appendMethod(sb, iEntityInfo);
				}
				else {
					TargetExceptionInfo targetExceptionInfo = methodInfo.getUnhandledExceptions();
					Collection<String> unhandledExceptions = TargetExceptionUtil.getFilteredExceptions(iModuleInfo.getJavaProject(), targetExceptionInfo.getTargetExceptions());
					String[] parameterTypes = methodInfo.getParameterTypes();
					sb.append("\r\n\tpublic ");
					sb.append(iEntityAccessBeanClassInfo.getClassName());
					sb.append("(");
					int i = 0;
					for (String parameterType : parameterTypes) {
						if (i != 0) {
							sb.append(", ");
						}
						if (parameterType.equals(iEntityInfo.getPrimaryKeyClass()) && iEntityKeyClassInfo != null) {
							sb.append(iEntityKeyClassInfo.getQualifiedClassName());
						}
						else if (parameterType.equals(iEntityInfo.getPrimaryKeyClass()) && iEntityInfo.getKeyFields().size() == 1) {
							sb.append(iEntityInfo.getKeyFields().get(0).getTypeName());
						}
						else {
							sb.append(getTargetType(parameterType));						
						}
						sb.append(" ");
						sb.append(methodInfo.getTargetParameterName(i));
						i++;
					}
					sb.append(") {\r\n\t\t");
					sb.append("setEntity(new ");
					sb.append(iEntityClassInfo.getClassName());
					sb.append("());\r\n");
					appendInstanceVariableDeclarations(sb, methodInfo);
					appendRelatedEntityKeyDeclarations(sb, methodInfo);
					if (unhandledExceptions.size() == 0) {
						appendAccessBeanStatements(sb, methodInfo, methodInfo.getStatements(), new HashSet<RelatedEntityInfo>(), "\t\t");
					}
					else {
						sb.append("\t\ttry {\r\n");
						appendAccessBeanStatements(sb, methodInfo, methodInfo.getStatements(), new HashSet<RelatedEntityInfo>(), "\t\t\t");
						sb.append("\t\t}\r\n");
						for (String exception : unhandledExceptions) {
							sb.append("\t\tcatch (");
							sb.append(exception);
							sb.append(" e) {\r\n\t\t\tthrow new PersistenceException(e);\r\n\t\t}\r\n");
						}
					}
					appendRelatedEntityFieldInitializations(sb, methodInfo);
					sb.append("\t\tgetEntityManager().persist(iEntity);\r\n");
					sb.append("\t}\r\n");
				}
			}
		}
	}
	
	private void appendAccessBeanMethods(StringBuilder sb) {
		Set<AccessBeanMethodInfo> methods = new HashSet<AccessBeanMethodInfo>();
		Collection<String> processedMethods = new HashSet<String>();
		Collection<CreatorInfo> creators = iEntityInfo.getCreators();
		for (CreatorInfo creator : creators) {
			if (!creator.isInvalid()) {
				methods.add(creator.getAccessBeanMethodInfo());
			}
		}
		Collection<FieldInfo> fields = iEntityInfo.getFields();
		for (FieldInfo fieldInfo : fields) {
			if (fieldInfo.getGetterAccessBeanMethodInfo() != null) {
				methods.add(fieldInfo.getGetterAccessBeanMethodInfo());
			}
			if (fieldInfo.getSetterAccessBeanMethodInfo() != null) {
				methods.add(fieldInfo.getSetterAccessBeanMethodInfo());
			}
		}
		List<UserMethodInfo> userMethods = iEntityInfo.getUserMethods();
		for (UserMethodInfo userMethodInfo : userMethods) {
			if (userMethodInfo.getAccessBeanMethodInfo() != null) {
				methods.add(userMethodInfo.getAccessBeanMethodInfo());
			}
		}
		while (!methods.isEmpty()) {
			Set<AccessBeanMethodInfo> newMethods = new HashSet<AccessBeanMethodInfo>();
			for (AccessBeanMethodInfo referencingMethodInfo : methods) {
				Collection<String> referencedMethods = referencingMethodInfo.getReferencedAccessBeanMethods();
				for (String methodKey : referencedMethods) {
					if (!processedMethods.contains(methodKey)) {
						processedMethods.add(methodKey);
						AccessBeanMethodInfo methodInfo = iEntityInfo.getAccessBeanMethodInfo(methodKey);
						if (methodInfo != null && !methodInfo.isInvalid() && !methodInfo.isEmptyMethod()) {
							newMethods.add(methodInfo);
							TargetExceptionInfo targetExceptionInfo = methodInfo.getUnhandledExceptions();
							Collection<String> unhandledExceptions = TargetExceptionUtil.getFilteredExceptions(iModuleInfo.getJavaProject(), targetExceptionInfo.getTargetExceptions());
							String returnType = methodInfo.getReturnType();
							String[] parameterTypes = methodInfo.getParameterTypes();
							InstanceVariableInfo instanceVariableInfo = null;
							String modifiedInstanceVariableName = methodInfo.getModifiedInstanceVariableName();
							if (modifiedInstanceVariableName != null) {
								instanceVariableInfo = iEntityInfo.getEjbInstanceVariableInfo(modifiedInstanceVariableName);
								returnType = instanceVariableInfo.getType();
							}
							sb.append("\r\n\tprivate ");
							if (returnType == null) {
								sb.append("void");
							}
							else {
								sb.append(getTargetType(returnType));
							}
							sb.append(" ");
							sb.append(methodInfo.getTargetMethodName());
							sb.append("(");
							int i = 0; 
							for (String parameterType : parameterTypes) {
								if (i != 0) {
									sb.append(", ");
								}
								sb.append(getTargetType(parameterType));						
								sb.append(" ");
								sb.append(methodInfo.getTargetParameterName(i));
								i++;
							}
							sb.append(") {\r\n");
							if (modifiedInstanceVariableName != null) {
								sb.append("\t\t");
								sb.append(getTargetType(returnType));
								sb.append(" ");
								sb.append(modifiedInstanceVariableName);
								if (instanceVariableInfo.getInitializationExpression() != null) {
									sb.append(" = ");
									sb.append(instanceVariableInfo.getInitializationExpression());
								}
								sb.append(";\r\n");
							}
							appendRelatedEntityKeyDeclarations(sb, methodInfo);
							if (unhandledExceptions.size() == 0) {
								appendAccessBeanStatements(sb, methodInfo, methodInfo.getStatements(), new HashSet<RelatedEntityInfo>(), "\t\t");
							}
							else {
								sb.append("\t\ttry {\r\n");
								appendAccessBeanStatements(sb, methodInfo, methodInfo.getStatements(), new HashSet<RelatedEntityInfo>(), "\t\t\t");
								sb.append("\t\t}\r\n");
								for (String exception : unhandledExceptions) {
									sb.append("\t\tcatch (");
									sb.append(exception);
									sb.append(" e) {\r\n\t\t\tthrow new PersistenceException(e);\r\n\t\t}\r\n");
								}
							}
							appendRelatedEntityFieldInitializations(sb, methodInfo);
							if (modifiedInstanceVariableName != null) {
								sb.append("\t\treturn ");
								sb.append(modifiedInstanceVariableName);
								sb.append(";\r\n");
							}
							sb.append("\t}\r\n");
						}
					}
				}
			}
			methods = newMethods;
		}
	}
	
	private void appendInstanceVariableDeclarations(StringBuilder sb, AccessBeanMethodInfo methodInfo) {
		Collection<String> instanceVariableNames = methodInfo.getReferencedInstanceVariableNames();
		for (String variableName : instanceVariableNames) {
			InstanceVariableInfo instanceVariableInfo = iEntityInfo.getEjbInstanceVariableInfo(variableName);
			sb.append("\t\t");
			sb.append(getTargetType(instanceVariableInfo.getType()));
			sb.append(" ");
			sb.append(instanceVariableInfo.getVariableName());
			if (instanceVariableInfo.getInitializationExpression() != null) {
				sb.append(" = ");
				sb.append(instanceVariableInfo.getInitializationExpression());
			}
			sb.append(";\r\n");
		}
	}
	
	private void appendRelatedEntityKeyDeclarations(StringBuilder sb, AccessBeanMethodInfo methodInfo) {
		Set<RelatedEntityInfo> processedRelatedEntities = new HashSet<RelatedEntityInfo>();
		Set<FieldInfo> initializedFields = methodInfo.getInitializedFields();
		for (FieldInfo initializedFieldInfo : initializedFields) {
			RelatedEntityInfo relatedEntityInfo = initializedFieldInfo.getRelatedEntityInfo();
			if (relatedEntityInfo != null && relatedEntityInfo.getMemberFields().size() > 1) {
				if (!processedRelatedEntities.contains(relatedEntityInfo)) {
					processedRelatedEntities.add(relatedEntityInfo);
					sb.append("\t\t");
					sb.append(relatedEntityInfo.getParentEntityInfo().getEntityKeyClassInfo().getQualifiedClassName());
					sb.append(" ");
					sb.append(relatedEntityInfo.getFieldName());
					sb.append("Key = null;\r\n");
				}
			}
		}
	}
	
	private void appendRelatedEntityFieldInitializations(StringBuilder sb, AccessBeanMethodInfo methodInfo) {
		Set<RelatedEntityInfo> processedRelatedEntities = new HashSet<RelatedEntityInfo>();
		Set<FieldInfo> initializedFields = methodInfo.getInitializedFields();
		for (FieldInfo initializedFieldInfo : initializedFields) {
			RelatedEntityInfo relatedEntityInfo = initializedFieldInfo.getRelatedEntityInfo();
			if (relatedEntityInfo != null && relatedEntityInfo.getMemberFields().size() > 1) {
				if (!processedRelatedEntities.contains(relatedEntityInfo)) {
					processedRelatedEntities.add(relatedEntityInfo);
					sb.append("\t\tif (");
					sb.append(relatedEntityInfo.getFieldName());
					sb.append("Key != null) {\r\n\t\t\t");
					sb.append("iTypedEntity.");
					sb.append(relatedEntityInfo.getSetterName());
					sb.append("(findRelated");
					sb.append(relatedEntityInfo.getParentEntityInfo().getEjbName());
					sb.append("Entity(");
					sb.append(relatedEntityInfo.getFieldName());
					sb.append("Key");
					sb.append("));\r\n\t\t}\r\n");
				}
			}
		}
	}
	
	private void appendAccessBeanStatements(StringBuilder sb, AccessBeanMethodInfo methodInfo, List<AccessBeanStatement> statements, Set<RelatedEntityInfo> initializedRelatedEntityKeys, String indent) {
		Set<RelatedEntityInfo> keyParameterInitializedRelatedEntities = new HashSet<RelatedEntityInfo>();
		for (AccessBeanStatement statement : statements) {
			if (statement instanceof AccessBeanKeyParameterInitializedFieldStatement) {
				appendKeyParameterInitializedFieldStatement(sb, (AccessBeanKeyParameterInitializedFieldStatement) statement, keyParameterInitializedRelatedEntities, indent);
			}
			else if (statement instanceof AccessBeanEntityCreationDataInitializedFieldStatement) {
				appendEntityCreationDataInitializedFieldStatement(sb, methodInfo, (AccessBeanEntityCreationDataInitializedFieldStatement) statement, initializedRelatedEntityKeys, indent);
			}
			else if (statement instanceof AccessBeanParameterInitializedFieldStatement) {
				appendParameterInitializedFieldStatement(sb, methodInfo, (AccessBeanParameterInitializedFieldStatement) statement, initializedRelatedEntityKeys, indent);
			}
			else if (statement instanceof AccessBeanExpressionInitializedFieldStatement) {
				appendExpressionInitializedFieldStatement(sb, (AccessBeanExpressionInitializedFieldStatement) statement, initializedRelatedEntityKeys, indent);
			}
			else if (statement instanceof AccessBeanVariableDeclarationStatement) {
				appendVariableDeclarationStatement(sb, (AccessBeanVariableDeclarationStatement) statement, indent);
			}
			else if (statement instanceof AccessBeanVariableAssignmentStatement) {
				appendVariableAssignmentStatement(sb, (AccessBeanVariableAssignmentStatement) statement, indent);
			}
			else if (statement instanceof AccessBeanVariableMethodInvocationStatement) {
				appendVariableMethodInvocationStatement(sb, (AccessBeanVariableMethodInvocationStatement) statement, indent);
			}
			else if (statement instanceof AccessBeanInstanceVariableAssignmentStatement) {
				appendInstanceVariableAssignmentStatement(sb, (AccessBeanInstanceVariableAssignmentStatement) statement, indent);
			}
			else if (statement instanceof AccessBeanIfStatement) {
				appendIfStatement(sb, methodInfo, (AccessBeanIfStatement) statement, initializedRelatedEntityKeys, indent);
			}
			else if (statement instanceof AccessBeanMethodInvocationStatement) {
				appendMethodInvocationStatement(sb, (AccessBeanMethodInvocationStatement) statement, indent);
			}
			else if (statement instanceof AccessBeanSuperMethodInvocationStatement) {
				appendAccessBeanStatements(sb, methodInfo.getSuperAccessBeanMethodInfo(), methodInfo.getSuperAccessBeanMethodInfo().getStatements(), initializedRelatedEntityKeys, indent);
			}
			else if (statement instanceof AccessBeanUserMethodInvocationStatement) {
				appendUserMethodInvocationStatement(sb, (AccessBeanUserMethodInvocationStatement) statement, indent);
			}
			else if (statement instanceof AccessBeanReturnStatement) {
				appendReturnStatement(sb, (AccessBeanReturnStatement) statement, indent);
			}
			else if (statement instanceof AccessBeanTryStatement) {
				appendTryStatement(sb, methodInfo, (AccessBeanTryStatement) statement, initializedRelatedEntityKeys, indent);
			}
		}
	}
	
	private void appendKeyParameterInitializedFieldStatement(StringBuilder sb, AccessBeanKeyParameterInitializedFieldStatement statement, Set<RelatedEntityInfo> keyParameterInitializedRelatedEntities, String indent) {
		FieldInfo fieldInfo = statement.getFieldInfo();
		RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
		if (relatedEntityInfo == null) {
			sb.append(indent);
			sb.append("iTypedEntity.");
			sb.append(fieldInfo.getTargetSetterName());
			if (iEntityInfo.getKeyFields().size() == 1) {
				sb.append("(key);\r\n");
			}
			else {
				sb.append("(key.");
				sb.append(fieldInfo.getTargetGetterName());
				sb.append("());\r\n");
			}
		}
		else if (!keyParameterInitializedRelatedEntities.contains(relatedEntityInfo)) {
			keyParameterInitializedRelatedEntities.add(relatedEntityInfo);
			sb.append(indent);
			sb.append("iTypedEntity.");
			sb.append(relatedEntityInfo.getSetterName());
			sb.append("(findRelated");
			sb.append(relatedEntityInfo.getParentEntityInfo().getEjbName());
			sb.append("Entity(key.");
			sb.append(relatedEntityInfo.getGetterName());
			sb.append("()));\r\n");
		}
	}
	
	private void appendEntityCreationDataInitializedFieldStatement(StringBuilder sb, AccessBeanMethodInfo methodInfo, AccessBeanEntityCreationDataInitializedFieldStatement statement, Set<RelatedEntityInfo> initializedRelatedEntityKeys, String indent) {
		String parameterName = methodInfo.getTargetParameterName(0);
		String targetEntityCreationDataType = iEntityInfo.getEntityEntityCreationDataClassInfo() == null ? null : iEntityInfo.getEntityEntityCreationDataClassInfo().getQualifiedClassName(); 
		if (targetEntityCreationDataType == null && iEntityInfo.getSupertype() != null) {
			targetEntityCreationDataType = iEntityInfo.getSupertype().getEntityEntityCreationDataClassInfo().getQualifiedClassName();
		}
		FieldInfo fieldInfo = statement.getFieldInfo();
		RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
		if (relatedEntityInfo == null) {
			sb.append(indent);
			sb.append("iTypedEntity.");
			sb.append(fieldInfo.getTargetSetterName());
			sb.append("(((");
			sb.append(targetEntityCreationDataType);
			sb.append(") ");
			sb.append(parameterName);
			sb.append(").");
			sb.append(fieldInfo.getEntityCreationDataGetterName());
			sb.append("());\r\n");
		}
		else if (relatedEntityInfo.getMemberFields().size() == 1) {
			sb.append(indent);
			sb.append("iTypedEntity.");
			sb.append(relatedEntityInfo.getSetterName());
			sb.append("(findRelated");
			sb.append(relatedEntityInfo.getParentEntityInfo().getEjbName());
			sb.append("Entity(((");
			sb.append(targetEntityCreationDataType);
			sb.append(") ");
			sb.append(parameterName);
			sb.append(").");
			sb.append(fieldInfo.getEntityCreationDataGetterName());
			sb.append("()));\r\n");
		}
		else if (relatedEntityInfo.getMemberFields().size() > 1) {
			if (!initializedRelatedEntityKeys.contains(relatedEntityInfo)) {
				initializedRelatedEntityKeys.add(relatedEntityInfo);
				sb.append(indent);
				sb.append("if (");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append("Key == null) {\r\n");
				sb.append(indent);
				sb.append("\t");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append("Key = new ");
				sb.append(relatedEntityInfo.getParentEntityInfo().getEntityKeyClassInfo().getQualifiedClassName());
				sb.append("();\r\n");
				sb.append(indent);
				sb.append("}\r\n");
			}
			sb.append(indent);
			sb.append(relatedEntityInfo.getFieldName());
			sb.append("Key.");
			if (fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null) {
				sb.append(fieldInfo.getReferencedFieldInfo().getTargetSetterName());
			}
			else {
				sb.append(fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo().getSetterName());
			}
			sb.append("(((");
			sb.append(targetEntityCreationDataType);
			sb.append(") ");
			sb.append(parameterName);
			sb.append(").");
			sb.append(fieldInfo.getEntityCreationDataGetterName());
			sb.append("());\r\n");
		}
	}
	
	private void appendParameterInitializedFieldStatement(StringBuilder sb, AccessBeanMethodInfo methodInfo, AccessBeanParameterInitializedFieldStatement statement, Set<RelatedEntityInfo> initializedRelatedEntityKeys, String indent) {
		String parameterName = methodInfo.getTargetParameterName(statement.getParameterIndex());
		FieldInfo fieldInfo = statement.getFieldInfo();
		RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
		if (relatedEntityInfo == null) {
			sb.append(indent);
			sb.append("iTypedEntity.");
			sb.append(fieldInfo.getTargetSetterName());
			sb.append("(");
			sb.append(parameterName);
			sb.append(");\r\n");
		}
		else if (relatedEntityInfo.getMemberFields().size() == 1) {
			sb.append(indent);
			sb.append("iTypedEntity.");
			sb.append(relatedEntityInfo.getSetterName());
			sb.append("(findRelated");
			sb.append(relatedEntityInfo.getParentEntityInfo().getEjbName());
			sb.append("Entity(");
			sb.append(parameterName);
			sb.append("));\r\n");
		}
		else if (relatedEntityInfo.getMemberFields().size() > 1) {
			if (!initializedRelatedEntityKeys.contains(relatedEntityInfo)) {
				initializedRelatedEntityKeys.add(relatedEntityInfo);
				sb.append(indent);
				sb.append("if (");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append("Key == null) {\r\n");
				sb.append(indent);
				sb.append("\t");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append("Key = new ");
				sb.append(relatedEntityInfo.getParentEntityInfo().getEntityKeyClassInfo().getQualifiedClassName());
				sb.append("();\r\n");
				sb.append(indent);
				sb.append("}\r\n");
			}
			sb.append(indent);
			sb.append(relatedEntityInfo.getFieldName());
			sb.append("Key.");
			if (fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null) {
				sb.append(fieldInfo.getReferencedFieldInfo().getTargetSetterName());
			}
			else {
				sb.append(fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo().getSetterName());
			}
			sb.append("(");
			sb.append(parameterName);
			sb.append(");\r\n");
		}
	}
	
	private void appendExpressionInitializedFieldStatement(StringBuilder sb, AccessBeanExpressionInitializedFieldStatement statement, Set<RelatedEntityInfo> initializedRelatedEntityKeys, String indent) {
		FieldInfo fieldInfo = statement.getFieldInfo();
		Expression expression = statement.getExpression();
		RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
		if (relatedEntityInfo == null) {
			sb.append(indent);
			sb.append("iTypedEntity.");
			sb.append(fieldInfo.getTargetSetterName());
			sb.append("(");
			if (fieldInfo.getIsKeyField() && iEntityInfo.getGeneratePrimaryKeyMethodKey() != null) {
				sb.append("generatePrimaryKey()");
			}
			else {
				sb.append(expression);
			}
			sb.append(");\r\n");
		}
		else if (relatedEntityInfo.getMemberFields().size() == 1) {
			sb.append(indent);
			sb.append("iTypedEntity.");
			sb.append(relatedEntityInfo.getSetterName());
			sb.append("(findRelated");
			sb.append(relatedEntityInfo.getParentEntityInfo().getEjbName());
			sb.append("Entity(");
			sb.append(expression);
			sb.append("));\r\n");
		}
		else if (relatedEntityInfo.getMemberFields().size() > 1) {
			if (!initializedRelatedEntityKeys.contains(relatedEntityInfo)) {
				initializedRelatedEntityKeys.add(relatedEntityInfo);
				sb.append(indent);
				sb.append("if (");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append("Key == null) {\r\n");
				sb.append(indent);
				sb.append("\t");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append("Key = new ");
				sb.append(relatedEntityInfo.getParentEntityInfo().getEntityKeyClassInfo().getQualifiedClassName());
				sb.append("();\r\n");
				sb.append(indent);
				sb.append("}\r\n");
			}
			sb.append(indent);
			sb.append(relatedEntityInfo.getFieldName());
			sb.append("Key.");
			if (fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null) {
				sb.append(fieldInfo.getReferencedFieldInfo().getTargetSetterName());
			}
			else {
				sb.append(fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo().getSetterName());
			}
			sb.append("(");
			sb.append(expression);
			sb.append(");\r\n");
		}
	}
	
	private void appendVariableDeclarationStatement(StringBuilder sb, AccessBeanVariableDeclarationStatement statement, String indent) {
		sb.append(indent);
		sb.append(getTargetType(statement.getType()));
		sb.append(" ");
		sb.append(statement.getVariableName());
		if (statement.getInitializationExpression() != null) {
			sb.append(" = ");
			sb.append(statement.getInitializationExpression());
		}
		sb.append(";\r\n");
	}
	
	private void appendVariableAssignmentStatement(StringBuilder sb, AccessBeanVariableAssignmentStatement statement, String indent) {
		sb.append(indent);
		sb.append(statement.getVariableName());
		sb.append(" = ");
		sb.append(statement.getAssignmentExpression());
		sb.append(";\r\n");
	}
	
	private void appendVariableMethodInvocationStatement(StringBuilder sb, AccessBeanVariableMethodInvocationStatement statement, String indent) {
		sb.append(indent);
		sb.append(statement.getVariableName());
		sb.append(".");
		String methodName = statement.getMethodName();
		if (iApplicationInfo.isAccessBeanType(statement.getVariableType())) {
			String newMethodName = AccessBeanUtil.getNewAccessBeanMethodName(iApplicationInfo, statement.getVariableType(), methodName);
			if (newMethodName != null) {
				methodName = newMethodName;
			}
		}
		sb.append(methodName);
		sb.append("(");
		boolean firstArgument = true;
		for (Expression expression : statement.getArguments()) {
			if (firstArgument) {
				firstArgument = false;
			}
			else {
				sb.append(", ");
			}
			sb.append(expression);
		}
		sb.append(");\r\n");
	}
	
	private void appendInstanceVariableAssignmentStatement(StringBuilder sb, AccessBeanInstanceVariableAssignmentStatement statement, String indent) {
		sb.append(indent);
		sb.append(statement.getInstanceVariableName());
		sb.append(" = ");
		sb.append(statement.getAssignmentExpression());
		sb.append(";\r\n");
	}
	
	private void appendIfStatement(StringBuilder sb, AccessBeanMethodInfo methodInfo, AccessBeanIfStatement statement, Set<RelatedEntityInfo> initializedRelatedEntityKeys, String indent) {
		sb.append(indent);
		sb.append("if (");
		sb.append(statement.getIfExpression());
		sb.append(") {\r\n");
		Set<RelatedEntityInfo> localInitializedRelatedEntityKeys = new HashSet<RelatedEntityInfo>();
		localInitializedRelatedEntityKeys.addAll(initializedRelatedEntityKeys);
		appendAccessBeanStatements(sb, methodInfo, statement.getThenStatements(), localInitializedRelatedEntityKeys, indent + "\t");
		sb.append(indent);
		sb.append("}\r\n");
		if (statement.getElseStatements().size() > 0) {
			sb.append(indent);
			sb.append("else {\r\n");
			localInitializedRelatedEntityKeys = new HashSet<RelatedEntityInfo>();
			localInitializedRelatedEntityKeys.addAll(initializedRelatedEntityKeys);
			appendAccessBeanStatements(sb, methodInfo, statement.getElseStatements(), localInitializedRelatedEntityKeys, indent + "\t");
			sb.append(indent);
			sb.append("}\r\n");
		}
	}
	
	private void appendMethodInvocationStatement(StringBuilder sb, AccessBeanMethodInvocationStatement statement, String indent) {
		AccessBeanMethodInfo methodInfo = iEntityInfo.getAccessBeanMethodInfo(statement.getMethodKey());
		if (methodInfo != null && !methodInfo.isEmptyMethod() && !methodInfo.isInvalid()) {
			sb.append(indent);
			String modifiedInstanceVariableName = methodInfo.getModifiedInstanceVariableName();
			if (modifiedInstanceVariableName != null) {
				sb.append(modifiedInstanceVariableName);
				sb.append(" = ");
			}
			sb.append(methodInfo.getTargetMethodName());
			sb.append("(");
			boolean firstArgument = true;
			for (Expression expression : statement.getArguments()) {
				if (firstArgument) {
					firstArgument = false;
				}
				else {
					sb.append(", ");
				}
				sb.append(expression);
			}
			sb.append(");\r\n");
		}
	}

	private void appendUserMethodInvocationStatement(StringBuilder sb, AccessBeanUserMethodInvocationStatement statement, String indent) {
		UserMethodInfo userMethodInfo = iEntityInfo.getUserMethodInfo(statement.getMethodKey());
		if (userMethodInfo != null) {
			sb.append(indent);
			sb.append("iTypedEntity.");
			sb.append(userMethodInfo.getMethodName());
			sb.append("(");
			boolean firstArgument = true;
			for (Expression expression : statement.getArguments()) {
				if (firstArgument) {
					firstArgument = false;
				}
				else {
					sb.append(", ");
				}
				sb.append(expression);
			}
			sb.append(");\r\n");
		}
	}
	
	private void appendReturnStatement(StringBuilder sb, AccessBeanReturnStatement statement, String indent) {
		sb.append(indent);
		sb.append("return ");
		sb.append(statement.getReturnExpression());
		sb.append(";\r\n");
	}

	private void appendTryStatement(StringBuilder sb, AccessBeanMethodInfo methodInfo, AccessBeanTryStatement statement, Set<RelatedEntityInfo> initializedRelatedEntityKeys, String indent) {
		sb.append(indent);
		sb.append("try {\r\n");
		Set<RelatedEntityInfo> localInitializedRelatedEntityKeys = new HashSet<RelatedEntityInfo>();
		localInitializedRelatedEntityKeys.addAll(initializedRelatedEntityKeys);
		appendAccessBeanStatements(sb, methodInfo, statement.getTryStatements(), localInitializedRelatedEntityKeys, indent + "\t");
		sb.append(indent);
		sb.append("}\r\n");
		List<AccessBeanCatchClause> catchClauses = statement.getCatchClauses();
		for (AccessBeanCatchClause catchClause : catchClauses) {
			Collection<String> targetExceptionTypes = catchClause.getTargetExceptionTypes();
			for (String targetExceptionType : targetExceptionTypes) {
				sb.append(indent);
				sb.append("catch (");
				sb.append(targetExceptionType);
				sb.append(" ");
				sb.append(catchClause.getExceptionVariableName());
				sb.append(") {\r\n");
				localInitializedRelatedEntityKeys = new HashSet<RelatedEntityInfo>();
				localInitializedRelatedEntityKeys.addAll(initializedRelatedEntityKeys);
				appendAccessBeanStatements(sb, methodInfo, catchClause.getCatchStatements(), localInitializedRelatedEntityKeys, indent + "\t");
				sb.append(indent);
				sb.append("}\r\n");
			}
		}
		if (statement.getFinallyStatements().size() > 0) {
			sb.append(indent);
			sb.append("finally {\r\n");
			localInitializedRelatedEntityKeys = new HashSet<RelatedEntityInfo>();
			localInitializedRelatedEntityKeys.addAll(initializedRelatedEntityKeys);
			appendAccessBeanStatements(sb, methodInfo, statement.getFinallyStatements(), localInitializedRelatedEntityKeys, indent + "\t");
			sb.append(indent);
			sb.append("}\r\n");
		}
		
	}
	
	private void appendInitKeySetters(StringBuilder sb) {
		List<FieldInfo> keyFields = iEntityInfo.getKeyFields();
		for (FieldInfo keyFieldInfo : keyFields) {
			NullConstructorParameter nullConstructorParameter = (iAccessBeanInfo != null)?iAccessBeanInfo.getNullConstructorParameterByName(keyFieldInfo.getFieldName()):null;
			if (nullConstructorParameter != null && nullConstructorParameter.getConverterClassName() != null && STRING_CONVERTER_FROM_METHODS.get(keyFieldInfo.getTypeName()) != null) {
				sb.append("\r\n\tpublic void setInitKey_");
				sb.append(keyFieldInfo.getTargetFieldName());
				sb.append("(String ");
				sb.append(keyFieldInfo.getTargetFieldName());
				sb.append(") {\r\n\t\t");
				RelatedEntityInfo relatedEntityInfo = keyFieldInfo.getRelatedEntityInfo();
				if (relatedEntityInfo == null || relatedEntityInfo.getMemberFields().size() == 1) {
					sb.append("iInitKey_");
					if (relatedEntityInfo != null) {
						sb.append(relatedEntityInfo.getFieldName());
					}
					else {
						sb.append(keyFieldInfo.getTargetFieldName());	
					}
					sb.append(" = ");
					sb.append(nullConstructorParameter.getConverterClassName());
					sb.append(".");
					sb.append(STRING_CONVERTER_FROM_METHODS.get(keyFieldInfo.getTypeName()));
					sb.append("(");
					sb.append(keyFieldInfo.getTargetFieldName());
					sb.append(")");
				}
				else if (relatedEntityInfo.getMemberFields().size() > 1) {
					sb.append("if (iInitKey_");
					sb.append(relatedEntityInfo.getFieldName());
					sb.append(" == null) {\r\n\t\t\tiInitKey_");
					sb.append(relatedEntityInfo.getFieldName());
					sb.append(" = new ");
					sb.append(relatedEntityInfo.getParentEntityInfo().getEntityKeyClassInfo().getQualifiedClassName());
					sb.append("();\r\n\t\t}\r\n\t\tiInitKey_");
					sb.append(relatedEntityInfo.getFieldName());
					sb.append(".");
					if (keyFieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null) {
						sb.append(keyFieldInfo.getReferencedFieldInfo().getTargetSetterName());
					}
					else {
						sb.append(keyFieldInfo.getReferencedFieldInfo().getRelatedEntityInfo().getSetterName());
					}
					sb.append("(");
					sb.append(nullConstructorParameter.getConverterClassName());
					sb.append(".");
					sb.append(STRING_CONVERTER_FROM_METHODS.get(keyFieldInfo.getTypeName()));
					sb.append("(");
					sb.append(keyFieldInfo.getTargetFieldName());
					sb.append("))");
				}
				sb.append(";\r\n\t}\r\n");
			}
			sb.append("\r\n\tpublic void setInitKey_");
			sb.append(keyFieldInfo.getTargetFieldName());
			sb.append("(");
			sb.append(keyFieldInfo.getTypeName());
			sb.append(" ");
			sb.append(keyFieldInfo.getTargetFieldName());
			sb.append(") {\r\n\t\t");
			RelatedEntityInfo relatedEntityInfo = keyFieldInfo.getRelatedEntityInfo();
			if (relatedEntityInfo == null || relatedEntityInfo.getMemberFields().size() == 1) {
				
				sb.append("iInitKey_");
				if (relatedEntityInfo != null) {
					sb.append(relatedEntityInfo.getFieldName());
				}
				else {
					sb.append(keyFieldInfo.getTargetFieldName());	
				}
				sb.append(" = ");
				String referencedFieldType = keyFieldInfo.getReferencedFieldInfo() == null ? null : keyFieldInfo.getReferencedFieldInfo().getTypeName(); 
				if (referencedFieldType != null && "java.lang.String".equals(keyFieldInfo.getTypeName()) && !"java.lang.String".equals(referencedFieldType)) {
					sb.append("com.ibm.commerce.base.objects.WCSStringConverter");
					sb.append(".");
					sb.append(STRING_CONVERTER_FROM_METHODS.get(referencedFieldType));
					sb.append("(");
				}
				sb.append(keyFieldInfo.getTargetFieldName());
				if (referencedFieldType != null && "java.lang.String".equals(keyFieldInfo.getTypeName()) && !"java.lang.String".equals(referencedFieldType)) {
					sb.append(")");
				}
			}
			else if (relatedEntityInfo.getMemberFields().size() > 1) {
				sb.append("if (iInitKey_");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(" == null) {\r\n\t\t\tiInitKey_");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(" = new ");
				sb.append(relatedEntityInfo.getParentEntityInfo().getEntityKeyClassInfo().getQualifiedClassName());
				sb.append("();\r\n\t\t}\r\n\t\tiInitKey_");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(".");
				if (keyFieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null) {
					sb.append(keyFieldInfo.getReferencedFieldInfo().getTargetSetterName());
				}
				else {
					sb.append(keyFieldInfo.getReferencedFieldInfo().getRelatedEntityInfo().getSetterName());
				}
				sb.append("(");
				sb.append(keyFieldInfo.getTargetFieldName());
				sb.append(")");
			}
			sb.append(";\r\n\t}\r\n");
		}		
	}
	
	private void appendFinderMethods(StringBuilder sb) {
		List<FinderInfo> queryFinders = iEntityInfo.getQueryFinders();
		for (FinderInfo finderInfo : queryFinders) {
			if (finderInfo.getInHomeInterface()) {
				sb.append("\r\n\tpublic ");
				if ("java.util.Enumeration".equals(finderInfo.getFinderMethodReturnType())) {
					sb.append("java.util.Enumeration");
				}
				else if ("java.util.Collection".equals(finderInfo.getFinderMethodReturnType())) {
					sb.append("java.util.Collection");
				}
				else if ("java.util.Iterator".equals(finderInfo.getFinderMethodReturnType())) {
					sb.append("java.util.Iterator");
				}
				else {
					sb.append(iEntityAccessBeanClassInfo.getClassName());
				}
				sb.append(" ");
				sb.append(finderInfo.getFinderMethodName());
				sb.append("(");
				String[] parameterTypes = finderInfo.getFinderMethodParameterTypes();
				int i = 0;
				for (String parameterType : parameterTypes) {
					if (i != 0) {
						sb.append(", ");
					}
					sb.append(getTargetType(parameterType));
					sb.append(" ");
					String parameterName = finderInfo.getFinderMethodParameterName(i);
					if (parameterName != null) {
						sb.append(parameterName);
					}
					else {
						sb.append("arg");
						sb.append(i + 1);
					}
					i++;
				}
				sb.append(") {\r\n");
				String indent = "\t\t";
				List<String> nullableParameters = finderInfo.getNullableParmeters();
				if (nullableParameters.size() > 0) {
					indent = "\t\t\t";
					sb.append("\t\tif (");
					boolean firstParameter = true;
					for (String nullableParameter : nullableParameters) {
						if (firstParameter) {
							firstParameter = false;
						}
						else {
							sb.append(" || ");
						}
						sb.append(nullableParameter);
						sb.append(" == null");
					}
					sb.append(") {\r\n");
					sb.append(indent);
					if ("java.util.Enumeration".equals(finderInfo.getFinderMethodReturnType())) {
						sb.append("return createAccessBeanEnumeration(new java.util.ArrayList(0));\r\n");
					}
					else if ("java.util.Collection".equals(finderInfo.getFinderMethodReturnType())) {
						sb.append("return createAccessBeanCollection(new java.util.ArrayList(0));\r\n");
					}
					else if ("java.util.Iterator".equals(finderInfo.getFinderMethodReturnType())) {
						sb.append("return createAccessBeanIterator(new java.util.ArrayList(0));\r\n");
					}
					else {
						sb.append("throw new javax.persistence.NoResultException();\r\n");
					}
					sb.append("\t\t}\r\n\t\telse {\r\n");
				}
				sb.append(indent);
				sb.append("String queryName = \"");
				sb.append(finderInfo.getQueryName());
				sb.append("\";\r\n");
				if (finderInfo.getOracleFinderWhereClause() != null) {
					sb.append(indent);
					sb.append("if (com.ibm.commerce.base.helpers.BaseJDBCHelper.useOracle()) {\r\n");
					sb.append(indent);
					sb.append("\tqueryName = \"");
					sb.append(finderInfo.getQueryName());
					sb.append("_ORACLE\";\r\n");
					sb.append(indent);
					sb.append("}\r\n");
				}
				sb.append(indent);
				sb.append("Query query = getEntityManager().createNamedQuery(queryName);\r\n");
				for (i = 0; i < parameterTypes.length; i++) {
					sb.append(indent);
					sb.append("query.setParameter(");
					sb.append(i + 1);
					sb.append(", ");
					String parameterName = finderInfo.getFinderMethodParameterName(i);
					if (parameterName != null) {
						sb.append(parameterName);
					}
					else {
						sb.append("arg");
						sb.append(i + 1);
					}
					sb.append(");\r\n");
				}
				sb.append(indent);
				if ("java.util.Enumeration".equals(finderInfo.getFinderMethodReturnType())) {
					sb.append("return createAccessBeanEnumeration(query.getResultList());\r\n");
				}
				else if ("java.util.Collection".equals(finderInfo.getFinderMethodReturnType())) {
					sb.append("return createAccessBeanCollection(query.getResultList());\r\n");
				}
				else if ("java.util.Iterator".equals(finderInfo.getFinderMethodReturnType())) {
					sb.append("return createAccessBeanIterator(query.getResultList());\r\n");
				}
				else {
					sb.append("return (");
					sb.append(iEntityAccessBeanClassInfo.getClassName());
					sb.append(") createAccessBean(query.getSingleResult());\r\n");
				}
				if (nullableParameters.size() > 0) {
					sb.append("\t\t}\r\n");
				}
				sb.append("\t}\r\n");
			}
		}
		List<FinderInfo> userFinders = iEntityInfo.getUserFinders();
		for (FinderInfo finderInfo : userFinders) {
			if (finderInfo.getInHomeInterface()) {
				sb.append("\r\n\tpublic ");
				boolean returnEmptyListOnNoResultException = true;
				if ("java.util.Enumeration".equals(finderInfo.getFinderMethodReturnType())) {
					sb.append("java.util.Enumeration");
				}
				else if ("java.util.Collection".equals(finderInfo.getFinderMethodReturnType())) {
					sb.append("java.util.Collection");
				}
				else if ("java.util.Iterator".equals(finderInfo.getFinderMethodReturnType())) {
					sb.append("java.util.Iterator");
				}
				else {
					sb.append(iEntityAccessBeanClassInfo.getClassName());
					returnEmptyListOnNoResultException = false;
				}
				sb.append(" ");
				sb.append(finderInfo.getFinderMethodName());
				sb.append("(");
				String[] parameterTypes = finderInfo.getFinderMethodParameterTypes();
				int i = 0;
				for (String parameterType : parameterTypes) {
					if (i != 0) {
						sb.append(", ");
					}
					if (TYPE_MAP.containsKey(parameterType)) {
						parameterType = TYPE_MAP.get(parameterType);
					}
					sb.append(getTargetType(parameterType));
					sb.append(" ");
					String parameterName = finderInfo.getFinderMethodParameterName(i);
					if (parameterName != null) {
						sb.append(parameterName);
					}
					else {
						sb.append("arg");
						sb.append(i + 1);
					}
					i++;
				}
				sb.append(") {\r\n");
				String indent = "\t\t";
				if (returnEmptyListOnNoResultException) {
					indent = "\t\t\t";
					sb.append("\t\ttry {\r\n");
				}
				sb.append(indent);
				sb.append(iEntityQueryHelperClassInfo.getQualifiedClassName());
				sb.append(" queryHelper = getQueryHelper();\r\n");
				sb.append(indent);
				sb.append("Query q = queryHelper.");
				sb.append(finderInfo.getFinderMethodName());
				sb.append("(");
				for (i = 0; i < parameterTypes.length; i++) {
					if (i > 0) {
						sb.append(", ");
					}
					String parameterName = finderInfo.getFinderMethodParameterName(i);
					if (parameterName != null) {
						sb.append(parameterName);
					}
					else {
						sb.append("arg");
						sb.append(i + 1);
					}
				}
				sb.append(");\r\n");
				sb.append(indent);
				if ("java.util.Enumeration".equals(finderInfo.getFinderMethodReturnType())) {
					sb.append("return createAccessBeanEnumeration(q.getResultList());\r\n");
				}
				else if ("java.util.Collection".equals(finderInfo.getFinderMethodReturnType())) {
					sb.append("return createAccessBeanCollection(q.getResultList());\r\n");
				}
				else if ("java.util.Iterator".equals(finderInfo.getFinderMethodReturnType())) {
					sb.append("return createAccessBeanIterator(q.getResultList());\r\n");
				}
				else {
					sb.append("return (");
					sb.append(iEntityAccessBeanClassInfo.getClassName());
					sb.append(") createAccessBean(q.getSingleResult());\r\n");
				}
				if (returnEmptyListOnNoResultException) {
					sb.append("\t\t}\r\n\t\tcatch (javax.persistence.NoResultException e) {\r\n");
					sb.append(indent);
					if ("java.util.Enumeration".equals(finderInfo.getFinderMethodReturnType())) {
						sb.append("return createAccessBeanEnumeration(new java.util.ArrayList(0));\r\n");
					}
					else if ("java.util.Collection".equals(finderInfo.getFinderMethodReturnType())) {
						sb.append("return createAccessBeanCollection(new java.util.ArrayList(0));\r\n");
					}
					else if ("java.util.Iterator".equals(finderInfo.getFinderMethodReturnType())) {
						sb.append("return createAccessBeanIterator(new java.util.ArrayList(0));\r\n");
					}
					sb.append("\t\t}\r\n");
				}
				sb.append("\t}\r\n");
			}
		}
	}
	
	private void appendGettersAndSetters(StringBuilder sb) {
		if(iAccessBeanInfo != null) {
			Collection<CopyHelperProperty> copyHelperProperties = iAccessBeanInfo.getCopyHelperProperties();
			for (CopyHelperProperty copyHelperProperty : copyHelperProperties) {
				if (!iAccessBeanInfo.isExcludedPropertyName(copyHelperProperty.getName())) {
					FieldInfo fieldInfo = copyHelperProperty.getFieldInfo();
					if (fieldInfo == null) {
						String setterName = copyHelperProperty.getSetterName();
						if (setterName != null) {
							sb.append("\r\n\tpublic void ");
							sb.append(setterName);
							sb.append("(");
							sb.append(getTargetType(copyHelperProperty.getType()));
							sb.append(" ");
							sb.append(copyHelperProperty.getName());
							sb.append(") {\r\n\t\tgetEntity().");
							sb.append(setterName);
							sb.append("(");
							sb.append(copyHelperProperty.getName());
							sb.append(");\r\n\t}\r\n");
						}
						String getterName = copyHelperProperty.getGetterName();
						if (getterName != null) {
							sb.append("\r\n\tpublic ");
							if (copyHelperProperty.getType() == null) {
								System.out.println("null copyhelperproperty type");
							}
							sb.append(getTargetType(copyHelperProperty.getType()));
							sb.append(" ");
							sb.append(getterName);
							if (!"java.lang.String".equals(copyHelperProperty.getType()) && copyHelperProperty.getConverterClassName() != null) {
								sb.append("InEntityType");
							}
							sb.append("() {\r\n\t\treturn getEntity().");
							sb.append(getterName);
							sb.append("();\r\n\t}\r\n");
						}
						if (!"java.lang.String".equals(copyHelperProperty.getType()) && copyHelperProperty.getConverterClassName() != null) {
							if (setterName != null) {
								sb.append("\r\n\tpublic void ");
								sb.append(setterName);
								sb.append("(String ");
								sb.append(copyHelperProperty.getName());
								sb.append(") {\r\n\t\t");
								sb.append("getEntity().");
								sb.append(setterName);
								sb.append("(");
								sb.append(copyHelperProperty.getConverterClassName());
								sb.append(".");
								sb.append(STRING_CONVERTER_FROM_METHODS.get(copyHelperProperty.getType()));
								sb.append("(");
								sb.append(copyHelperProperty.getName());
								sb.append("));\r\n\t}\r\n");
							}
							if (getterName != null) {
								sb.append("\r\n\tpublic String ");
								sb.append(getterName);
								sb.append("() {\r\n\t\treturn ");
								sb.append(copyHelperProperty.getConverterClassName());
								sb.append(".");
								sb.append(STRING_CONVERTER_TO_METHODS.get(copyHelperProperty.getType()));
								sb.append("(getEntity().");
								sb.append(getterName);
								sb.append("());\r\n\t}\r\n");
							}
						}
					}
					else {
						AccessBeanMethodInfo getterAccessBeanMethodInfo = fieldInfo.getGetterAccessBeanMethodInfo();
						AccessBeanMethodInfo setterAccessBeanMethodInfo = fieldInfo.getSetterAccessBeanMethodInfo();
						RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
						FieldInfo referencedFieldInfo = fieldInfo.getReferencedFieldInfo();
						ColumnInfo columnInfo = fieldInfo.getColumnInfo();
						String setterName = copyHelperProperty.getSetterName();
						String setterType = fieldInfo.getTypeName();
						if (setterAccessBeanMethodInfo != null) {
							setterType = setterAccessBeanMethodInfo.getParameterTypes()[0];
						}
						if (setterName != null) {
							sb.append("\r\n\tpublic void ");
							sb.append(setterName);
							sb.append("(");
							sb.append(setterType);
							sb.append(" ");
							sb.append(fieldInfo.getTargetFieldName());
							sb.append(") {\r\n\t\t");
							if (setterAccessBeanMethodInfo != null) {
								Collection<String> unhandledExceptions = TargetExceptionUtil.getFilteredExceptions(iModuleInfo.getJavaProject(), setterAccessBeanMethodInfo.getUnhandledExceptions().getTargetExceptions());
								sb.append("instantiateEntity();\r\n");
								appendInstanceVariableDeclarations(sb, setterAccessBeanMethodInfo);
								if (unhandledExceptions.size() == 0) {
									appendAccessBeanStatements(sb, setterAccessBeanMethodInfo, setterAccessBeanMethodInfo.getStatements(), new HashSet<RelatedEntityInfo>(), "\t\t");
								}
								else {
									sb.append("\t\ttry {\r\n");
									appendAccessBeanStatements(sb, setterAccessBeanMethodInfo, setterAccessBeanMethodInfo.getStatements(), new HashSet<RelatedEntityInfo>(), "\t\t\t");
									sb.append("\t\t}\r\n");
									for (String exception : unhandledExceptions) {
										sb.append("\t\tcatch (");
										sb.append(exception);
										sb.append(" e) {\r\n\t\t\tthrow new PersistenceException(e);\r\n\t\t}\r\n");
									}
								}
								appendRelatedEntityFieldInitializations(sb, setterAccessBeanMethodInfo);
								sb.append("\t}\r\n");
							}
							else if (relatedEntityInfo != null && relatedEntityInfo.getMemberFields().size() > 1) {
								sb.append("if (iFindKey_");
								sb.append(relatedEntityInfo.getFieldName());
								sb.append(" == null) {\r\n\t\t\tiFindKey_");
								sb.append(relatedEntityInfo.getFieldName());
								sb.append(" = new ");
								sb.append(relatedEntityInfo.getParentEntityInfo().getEntityKeyClassInfo().getQualifiedClassName());
								sb.append("();\r\n\t\t}\r\n\t\tiFindKey_");
								sb.append(relatedEntityInfo.getFieldName());
								sb.append(".");
								if (fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null) {
									sb.append(fieldInfo.getReferencedFieldInfo().getTargetSetterName());
								}
								else {
									sb.append(fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo().getSetterName());
								}
								sb.append("(");
								sb.append(fieldInfo.getTargetFieldName());
								sb.append(");\r\n\t\tgetEntity().");
								sb.append(relatedEntityInfo.getSetterName());
								sb.append("(findRelated");
								sb.append(relatedEntityInfo.getParentEntityInfo().getEjbName());
								sb.append("Entity(iFindKey_");
								sb.append(relatedEntityInfo.getFieldName());
								sb.append("));\r\n\t}\r\n");
							}
							else {
								sb.append("getEntity().");
								if (relatedEntityInfo == null) {
									sb.append(fieldInfo.getTargetSetterName());
								}
								else if (relatedEntityInfo.getMemberFields().size() == 1) {
									sb.append(relatedEntityInfo.getSetterName());
								}
								sb.append("(");
								if (relatedEntityInfo == null) {
									sb.append(fieldInfo.getTargetFieldName());
								}
								else if (relatedEntityInfo.getMemberFields().size() == 1) {
									String referencedFieldType = referencedFieldInfo.getTypeName();
									sb.append("findRelated");
									sb.append(relatedEntityInfo.getParentEntityInfo().getEjbName());
									sb.append("Entity(");
									if ("java.lang.String".equals(fieldInfo.getTypeName()) && !"java.lang.String".equals(referencedFieldType)) {
										sb.append("com.ibm.commerce.base.objects.WCSStringConverter");
										sb.append(".");
										sb.append(STRING_CONVERTER_FROM_METHODS.get(referencedFieldType));
										sb.append("(");
										sb.append(fieldInfo.getTargetFieldName());
										sb.append(")");
									}
									else {
										sb.append(fieldInfo.getTargetFieldName());
									}
									sb.append(")");
								}
								sb.append(");\r\n\t}\r\n");
							}
						}
						String getterName = copyHelperProperty.getGetterName();
						String getterType = fieldInfo.getTypeName();
						if (getterAccessBeanMethodInfo != null) {
							getterType = getterAccessBeanMethodInfo.getReturnType();
						}
						if (getterName != null) {
							sb.append("\r\n\tpublic ");
							sb.append(getterType);
							sb.append(" ");
							sb.append(getterName);
							if (!"java.lang.String".equals(fieldInfo.getTypeName()) && copyHelperProperty.getConverterClassName() != null) {
								sb.append("InEntityType");
							}
							sb.append("() {\r\n\t\t");
							if (getterAccessBeanMethodInfo != null) {
								Collection<String> unhandledExceptions = TargetExceptionUtil.getFilteredExceptions(iModuleInfo.getJavaProject(), getterAccessBeanMethodInfo.getUnhandledExceptions().getTargetExceptions());
								sb.append("instantiateEntity();\r\n");
								appendInstanceVariableDeclarations(sb, getterAccessBeanMethodInfo);
								if (unhandledExceptions.size() == 0) {
									appendAccessBeanStatements(sb, getterAccessBeanMethodInfo, getterAccessBeanMethodInfo.getStatements(), new HashSet<RelatedEntityInfo>(), "\t\t");
								}
								else {
									sb.append("\t\ttry {\r\n");
									appendAccessBeanStatements(sb, getterAccessBeanMethodInfo, getterAccessBeanMethodInfo.getStatements(), new HashSet<RelatedEntityInfo>(), "\t\t\t");
									sb.append("\t\t}\r\n");
									for (String exception : unhandledExceptions) {
										sb.append("\t\tcatch (");
										sb.append(exception);
										sb.append(" e) {\r\n\t\t\tthrow new PersistenceException(e);\r\n\t\t}\r\n");
									}
								}
								appendRelatedEntityFieldInitializations(sb, getterAccessBeanMethodInfo);
								sb.append("\t}\r\n");
							}
							else {
								sb.append("return ");
								if (relatedEntityInfo == null) {
									if (columnInfo != null && CHAR.equals(columnInfo.getTypeName()) && columnInfo.getLength() != null && columnInfo.getLength() > 1) {
										if (fieldInfo.getColumnInfo().getNullable()) {
											sb.append("getEntity().");
											sb.append(fieldInfo.getTargetGetterName());
											sb.append("() == null ? null : getEntity().");
											sb.append(fieldInfo.getTargetGetterName());
											sb.append("().trim()");
										}
										else {
											sb.append("getEntity().");
											sb.append(fieldInfo.getTargetGetterName());
											sb.append("().trim()");
										}
									}
									else {
										sb.append("getEntity().");
										sb.append(fieldInfo.getTargetGetterName());
										sb.append("()");
									}
								}
								else {
									String referencedFieldType = referencedFieldInfo.getTypeName();
									if ("java.lang.String".equals(fieldInfo.getTypeName()) && !"java.lang.String".equals(referencedFieldType)) {
										sb.append("com.ibm.commerce.base.objects.WCSStringConverter");
										sb.append(".");
										sb.append(STRING_CONVERTER_TO_METHODS.get(referencedFieldType));
										sb.append("(");
									}
									if (columnInfo != null && columnInfo.getNullable()) {
										sb.append("getEntity().");
										sb.append(relatedEntityInfo.getGetterName());
										sb.append("() == null ? null : ");
									}
									sb.append("getEntity().");
									sb.append(relatedEntityInfo.getGetterName());
									sb.append("().");
									if (referencedFieldInfo.getRelatedEntityInfo() == null) {
										if (referencedFieldInfo.getColumnInfo() != null && CHAR.equals(referencedFieldInfo.getColumnInfo().getTypeName()) && referencedFieldInfo.getColumnInfo().getLength() != null && referencedFieldInfo.getColumnInfo().getLength() > 1) {
											sb.append(referencedFieldInfo.getTargetGetterName());
											sb.append("().trim");
										}
										else {
											sb.append(referencedFieldInfo.getTargetGetterName());
										}
									}
									else {
										sb.append(referencedFieldInfo.getRelatedEntityInfo().getGetterName());
										sb.append("().");
										sb.append(referencedFieldInfo.getReferencedFieldInfo().getTargetGetterName());
									}
									sb.append("()");
									if ("java.lang.String".equals(fieldInfo.getTypeName()) && !"java.lang.String".equals(referencedFieldType)) {
										sb.append(")");
									}
								}
								sb.append(";\r\n\t}\r\n");
							}
						}
						if (!"java.lang.String".equals(fieldInfo.getTypeName()) && copyHelperProperty.getConverterClassName() != null) {
							if (setterName != null) {
								sb.append("\r\n\tpublic void ");
								sb.append(setterName);
								sb.append("(String ");
								sb.append(fieldInfo.getTargetFieldName());
								sb.append(") {\r\n\t\t");
								if (setterAccessBeanMethodInfo != null) {
									sb.append(setterName);
									sb.append("(");
									sb.append(copyHelperProperty.getConverterClassName());
									sb.append(".");
									sb.append(STRING_CONVERTER_FROM_METHODS.get(setterType));
									sb.append("(");
									sb.append(fieldInfo.getTargetFieldName());
									sb.append("));\r\n\t}\r\n");
								}
								else if (relatedEntityInfo != null && relatedEntityInfo.getMemberFields().size() > 1) {
									sb.append("if (iFindKey_");
									sb.append(relatedEntityInfo.getFieldName());
									sb.append(" == null) {\r\n\t\t\tiFindKey_");
									sb.append(relatedEntityInfo.getFieldName());
									sb.append(" = new ");
									sb.append(relatedEntityInfo.getParentEntityInfo().getEntityKeyClassInfo().getQualifiedClassName());
									sb.append("();\r\n\t\t}\r\n\t\tiFindKey_");
									sb.append(relatedEntityInfo.getFieldName());
									sb.append(".");
									if (fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo() == null) {
										sb.append(fieldInfo.getReferencedFieldInfo().getTargetSetterName());
									}
									else {
										sb.append(fieldInfo.getReferencedFieldInfo().getRelatedEntityInfo().getSetterName());
									}
									sb.append("(");
									sb.append(copyHelperProperty.getConverterClassName());
									sb.append(".");
									sb.append(STRING_CONVERTER_FROM_METHODS.get(setterType));
									sb.append("(");
									sb.append(fieldInfo.getTargetFieldName());
									sb.append("));\r\n\t\tgetEntity().");
									sb.append(relatedEntityInfo.getSetterName());
									sb.append("(findRelated");
									sb.append(relatedEntityInfo.getParentEntityInfo().getEjbName());
									sb.append("Entity(iFindKey_");
									sb.append(relatedEntityInfo.getFieldName());
									sb.append("));\r\n\t}\r\n");
								}
								else {
									sb.append("getEntity().");
									if (relatedEntityInfo == null) {
										sb.append(fieldInfo.getTargetSetterName());
									}
									else if (relatedEntityInfo.getMemberFields().size() == 1) {
										sb.append(relatedEntityInfo.getSetterName());
									}
									sb.append("(");
									if (relatedEntityInfo != null && relatedEntityInfo.getMemberFields().size() == 1) {
										sb.append("findRelated");
										sb.append(relatedEntityInfo.getParentEntityInfo().getEjbName());
										sb.append("Entity(");
									}
									sb.append(copyHelperProperty.getConverterClassName());
									sb.append(".");
									sb.append(STRING_CONVERTER_FROM_METHODS.get(setterType));
									sb.append("(");
									sb.append(fieldInfo.getTargetFieldName());
									if (relatedEntityInfo != null && relatedEntityInfo.getMemberFields().size() == 1) {
										sb.append(")");
									}
									sb.append("));\r\n\t}\r\n");
								}
							}
							if (getterName != null) {
								sb.append("\r\n\tpublic String ");
								sb.append(getterName);
								sb.append("() {\r\n\t\treturn ");
								sb.append(copyHelperProperty.getConverterClassName());
								sb.append(".");
								sb.append(STRING_CONVERTER_TO_METHODS.get(getterType));
								if (getterAccessBeanMethodInfo != null) {
									sb.append("(");
									sb.append(getterName);
									sb.append("InEntityType");
								}
								else {
									sb.append("(getEntity().");
									if (relatedEntityInfo == null) {
										sb.append(fieldInfo.getTargetGetterName());
									}
									else {
										if (columnInfo != null && columnInfo.getNullable()) {
											sb.append(relatedEntityInfo.getGetterName());
											sb.append("() == null ? null : getEntity().");
										}
										sb.append(relatedEntityInfo.getGetterName());
										sb.append("().");
										if (referencedFieldInfo.getRelatedEntityInfo() == null) {
											sb.append(referencedFieldInfo.getTargetGetterName());
										}
										else {
											sb.append(referencedFieldInfo.getRelatedEntityInfo().getGetterName());
											sb.append("().");
											sb.append(referencedFieldInfo.getReferencedFieldInfo().getTargetGetterName());
										}
									}
								}
								sb.append("());\r\n\t}\r\n");
							}
						}
					}
				}
			}
		}
		List<EjbRelationshipRoleInfo> ejbRelationshipRoles = iEntityInfo.getEjbRelationshipRoles();
		for (EjbRelationshipRoleInfo ejbRelationshipRoleInfo : ejbRelationshipRoles) {
			String setterName = ejbRelationshipRoleInfo.getSetterName();
			if (setterName != null) {
				sb.append("\r\n\tpublic void ");
				sb.append(setterName);
				sb.append("(");
//				if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
//					sb.append("java.util.Collection<");
//				}
//				sb.append(ejbRelationshipRoleInfo.getRelatedEntityInfo().getEntityClassInfo().getQualifiedClassName());
//				if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
//					sb.append(">");
//				}
				if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
					sb.append("java.util.Collection");
				}
				else {
					sb.append(ejbRelationshipRoleInfo.getRelatedEntityInfo().getEntityAccessBeanClassInfo().getQualifiedClassName());
				}
				sb.append(" ");
				sb.append(ejbRelationshipRoleInfo.getFieldName());
				sb.append(") {\r\n\t\tgetEntity().");
				sb.append(setterName);
				sb.append("(");
//				sb.append(ejbRelationshipRoleInfo.getFieldName());
				if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
//					sb.append("((java.util.Collection<");
//					sb.append(ejbRelationshipRoleInfo.getRelatedEntityInfo().getEntityClassInfo().getQualifiedClassName());
//					sb.append(">) ");
					sb.append("createEntityCollection(");
					sb.append(ejbRelationshipRoleInfo.getFieldName());
					sb.append(")");
//					sb.append("))");
				}
				else {
					sb.append("((");
					sb.append(ejbRelationshipRoleInfo.getRelatedEntityInfo().getEntityClassInfo().getQualifiedClassName());
					sb.append(") ");
					sb.append(ejbRelationshipRoleInfo.getFieldName());
					sb.append(".getEntity())");
				}
				sb.append(");\r\n\t}\r\n");
			}
			String getterName = ejbRelationshipRoleInfo.getGetterName();
			if (getterName != null) {
				sb.append("\r\n\tpublic ");
//				if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
//					sb.append("java.util.Collection<");
//				}
//				sb.append(ejbRelationshipRoleInfo.getRelatedEntityInfo().getEntityClassInfo().getQualifiedClassName());
//				if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
//					sb.append(">");
//				}
				if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
					sb.append("java.util.Collection");
				}
				else {
					sb.append(iEntityAccessBeanClassInfo.getQualifiedClassName());
				}
				sb.append(" ");
				sb.append(getterName);
				sb.append("() {\r\n\t\treturn ");
//				sb.append("getEntity().");
//				sb.append(getterName);
				if ("java.util.Collection".equals(ejbRelationshipRoleInfo.getFieldType())) {
					sb.append("createAccessBeanCollection(getEntity().");
					sb.append(getterName);
					sb.append("(), ");
					sb.append(ejbRelationshipRoleInfo.getRelatedEntityInfo().getEntityAccessBeanClassInfo().getQualifiedClassName());
					sb.append(".class)");
				}
				else {
					sb.append("(");
					sb.append(iEntityAccessBeanClassInfo.getQualifiedClassName());
					sb.append(") ");
					sb.append("createAccessBean(getEntity().");
					sb.append(getterName);
					sb.append("(), ");
					sb.append(ejbRelationshipRoleInfo.getRelatedEntityInfo().getEntityAccessBeanClassInfo().getQualifiedClassName());
					sb.append(".class)");
				}
//				sb.append("()");
				sb.append(";\r\n\t}\r\n");
			}
		}
	}
	
	private void appendInstantiateEntityMethod(StringBuilder sb) {
		sb.append("\r\n\tpublic void instantiateEntity() {\r\n");
		sb.append("\t\tif (iEntity == null) {\r\n");
		List<FieldInfo> keyFields = iEntityInfo.getKeyFields();
		List<RelatedEntityInfo> keyRelatedEntities = iEntityInfo.getKeyRelatedEntities();
		if (iEntityKeyClassInfo != null) {
			sb.append("\t\t\t");
			sb.append(iEntityKeyClassInfo.getQualifiedClassName());
			sb.append(" key = new ");
			sb.append(iEntityKeyClassInfo.getQualifiedClassName());
			sb.append("();\r\n");
			for (FieldInfo keyFieldInfo : keyFields) {
				if (keyFieldInfo.getRelatedEntityInfo() == null) {
					sb.append("\t\t\tkey.");
					sb.append(keyFieldInfo.getTargetSetterName());
					sb.append("(iInitKey_");
					sb.append(keyFieldInfo.getTargetFieldName());
					sb.append(");\r\n");
				}
			}
			for (RelatedEntityInfo relatedEntityInfo : keyRelatedEntities) {
				sb.append("\t\t\tkey.");
				sb.append(relatedEntityInfo.getSetterName());
				sb.append("(iInitKey_");
				sb.append(relatedEntityInfo.getFieldName());
				sb.append(");\r\n");
			}
		}
		sb.append("\t\t\tsetEntity(getEntityManager().");
		boolean useFind = iEntityInfo.getGeneratePrimaryKeyMethodKey() == null || iEntityInfo.getEntityKeyClassInfo() != null || (keyFields.size() > 0 && keyFields.get(0).getRelatedEntityInfo() != null);
		if (useFind) {
			sb.append("find(");
		}
		else {
			sb.append("getReference(");
		}
		sb.append(iEntityClassInfo.getQualifiedClassName());
		sb.append(".class, ");
		if (iEntityInfo.getEntityKeyClassInfo() != null) {
			sb.append("key");
		}
		else if (keyFields.size() > 0) {
			sb.append("iInitKey_");
			FieldInfo keyField = keyFields.get(0);
			if (keyField.getRelatedEntityInfo() == null) {
				sb.append(keyField.getTargetFieldName());
			}
			else {
				sb.append(keyField.getRelatedEntityInfo().getFieldName());
			}
		}
		else {
			System.out.println("no keyFields "+iEntityInfo.getEjbName());
		}
		sb.append("));");
		if (useFind) {
			sb.append("\r\n\t\t\tif (iEntity == null) {\r\n\t\t\t\tthrow new EntityNotFoundException();\r\n\t\t\t}");
		}
		sb.append("\r\n\t\t}\r\n\t}\r\n");
	}
	
	private void appendStandardMethods(StringBuilder sb) {
		sb.append("\r\n\tprotected void setEntity(Object entity) {\r\n\t\tiTypedEntity = (");
		sb.append(iEntityClassInfo.getQualifiedClassName());
		sb.append(") entity;\r\n\t\tsuper.setEntity(entity);\r\n\t}\r\n");
		if (iEntityInfo.getGeneratePrimaryKeyMethodKey() != null) {
			TableInfo tableInfo = iEntityInfo.getTableInfo();
			sb.append("\r\n\tprotected ");
			sb.append(iEntityInfo.getGeneratedPrimaryKeyType());
			sb.append(" generatePrimaryKey() {\r\n");
			sb.append("\t\ttry {\r\n");
			sb.append("\t\t\treturn com.ibm.commerce.key.ECKeyManager.singleton().");
			if (iEntityInfo.getGeneratedPrimaryKeyType().equals("int") || iEntityInfo.getGeneratedPrimaryKeyType().equals("java.lang.Integer")) {
				sb.append("getNextKeyAsInt");
			}
			else {
				sb.append("getNextKey");
			}
			sb.append("(\"");
			sb.append(tableInfo.getTableName().toLowerCase());
			sb.append("\");\r\n\t\t}\r\n\t\tcatch (Exception e) {\r\n\t\t\tthrow new PersistenceException(e);\r\n\t\t}\r\n\t}\r\n");
		}
		sb.append("\r\n\tpublic ");
		sb.append(iEntityClassInfo.getQualifiedClassName());
		sb.append(" getEntity() {\r\n");
		sb.append("\t\tif (iTypedEntity == null) {\r\n");
		sb.append("\t\t\tinstantiateEntity();\r\n");
		sb.append("\t\t}\r\n");
		sb.append("\t\treturn iTypedEntity;\r\n");
		sb.append("\t}\r\n");
		if (iEntityQueryHelperClassInfo != null) {
			sb.append("\r\n\tprotected ");
			sb.append(iEntityQueryHelperClassInfo.getQualifiedClassName());
			sb.append(" getQueryHelper() {\r\n\t\t");
			sb.append(iEntityQueryHelperClassInfo.getQualifiedClassName());
			sb.append(" queryHelper = new ");
			sb.append(iEntityQueryHelperClassInfo.getQualifiedClassName());
			sb.append("();\r\n");
			sb.append("\t\tqueryHelper.setEntityManager(getEntityManager());\r\n");
			sb.append("\t\tqueryHelper.setSelectClause(\"");
			sb.append(iEntityInfo.getSelectClause());
			sb.append("\");\r\n");
			sb.append("\t\tqueryHelper.setResultClass(");
			sb.append(iEntityClassInfo.getQualifiedClassName());
			sb.append(".class);\r\n");
			sb.append("\t\treturn queryHelper;\r\n");
			sb.append("\t}\r\n");
		}
		if (iEntityKeyClassInfo != null) {
			List<KeyClassConstructorInfo> keyClassConstructors = iEntityInfo.getKeyClassConstructors();
			KeyClassConstructorInfo keyClassConstructorInfo = null;
			for (KeyClassConstructorInfo current : keyClassConstructors) {
				if (current.getFields().size() == iEntityInfo.getFields().size()) {
					keyClassConstructorInfo = current;
					break;
				}
			}
			if (keyClassConstructorInfo != null) {
				sb.append("\r\n\tpublic ");
				sb.append(iEntityKeyClassInfo.getQualifiedClassName());
				sb.append(" getPrimaryKey() {\r\n\t\treturn new ");
				sb.append(iEntityKeyClassInfo.getQualifiedClassName());
				sb.append("(");
				List<FieldInfo> fields = keyClassConstructorInfo.getFields();
				boolean firstField = true;
				for (FieldInfo fieldInfo : fields) {
					if (firstField) {
						firstField = false;
					}
					else {
						sb.append(", ");
					}
					sb.append(fieldInfo.getCopyHelperProperty().getGetterName());
					sb.append("()");
				}
				sb.append(");\r\n\t}\r\n");
			}
		}
	}
	
	private void appendFindRelatedEntityMethods(StringBuilder sb) {
		Set<EntityInfo> parentEntities = new HashSet<EntityInfo>();
		List<RelatedEntityInfo> relatedEntities = iEntityInfo.getRelatedEntities();
		for (RelatedEntityInfo relatedEntityInfo : relatedEntities) {
			EntityInfo parentEntity = relatedEntityInfo.getParentEntityInfo();
			if (!parentEntities.contains(parentEntity)) {
				parentEntities.add(parentEntity);
				List<FieldInfo> referencedFields = relatedEntityInfo.getReferencedFields();
				if (referencedFields.size() == 1) {
					FieldInfo referencedFieldInfo = referencedFields.get(0);
					sb.append("\r\n\tprivate ");
					sb.append(parentEntity.getEntityClassInfo().getQualifiedClassName());
					sb.append(" findRelated");
					sb.append(parentEntity.getEjbName());
					sb.append("Entity(");
//					boolean firstField = true;
//					for (FieldInfo referencedFieldInfo : referencedFields) {
//						if (firstField) {
//							firstField = false;
//						}
//						else {
//							sb.append(", ");
//						}
//						sb.append(referencedFieldInfo.getTypeName());
//						sb.append(" ");
//						sb.append(referencedFieldInfo.getTargetFieldName());
//					}
					sb.append(referencedFieldInfo.getTypeName());
					sb.append(" ");
					sb.append(referencedFieldInfo.getTargetFieldName());
					sb.append(") {\r\n\t\t");
//					if (referencedFields.size() > 1) {
//						sb.append(parentEntity.getEntityKeyClassInfo().getQualifiedClassName());
//						sb.append(" ");
//						sb.append("key = new ");
//						sb.append(parentEntity.getEntityKeyClassInfo().getQualifiedClassName());
//						sb.append("();\r\n\t\t");
//						for (FieldInfo referencedFieldInfo : referencedFields) {
//							sb.append("key.");
//							if (referencedFieldInfo.getRelatedEntityInfo() == null) {
//								sb.append(referencedFieldInfo.getTargetSetterName());
//							}
//							else {
//								sb.append(referencedFieldInfo.getRelatedEntityInfo().getSetterName());
//							}
//							sb.append("(");
//							sb.append(referencedFieldInfo.getTargetFieldName());
//							sb.append(");\r\n\t\t");
//						}
//					}
					sb.append("return (");
					sb.append(parentEntity.getEntityClassInfo().getQualifiedClassName());
					sb.append(") getEntityManager().find(");
					sb.append(parentEntity.getEntityClassInfo().getQualifiedClassName());
					sb.append(".class, ");
//					if (referencedFields.size() > 1) {
//						sb.append("key");
//					}
//					else if (referencedFields.size() == 1) {
//						sb.append(referencedFields.get(0).getTargetFieldName());
//					}
					sb.append(referencedFieldInfo.getTargetFieldName());
					sb.append(");\r\n\t}\r\n");
				}
				if (referencedFields.size() > 1) {
					sb.append("\r\n\tprivate ");
					sb.append(parentEntity.getEntityClassInfo().getQualifiedClassName());
					sb.append(" findRelated");
					sb.append(parentEntity.getEjbName());
					sb.append("Entity(");
					sb.append(parentEntity.getEntityKeyClassInfo().getQualifiedClassName());
					sb.append(" key) {\r\n\t\treturn (");
					sb.append(parentEntity.getEntityClassInfo().getQualifiedClassName());
					sb.append(") getEntityManager().find(");
					sb.append(parentEntity.getEntityClassInfo().getQualifiedClassName());
					sb.append(".class, key);\r\n\t}\r\n");
				}
			}
		}
	}
	
	private void appendUserMethods(StringBuilder sb) {
		List<UserMethodInfo> userMethods = iEntityInfo.getUserMethods();
		for (UserMethodInfo userMethodInfo : userMethods) {
			if (userMethodInfo.getAccessBeanMethodInfo() != null) {
				AccessBeanMethodInfo accessBeanMethodInfo = userMethodInfo.getAccessBeanMethodInfo();
				Collection<String> unhandledExceptions = TargetExceptionUtil.getFilteredExceptions(iModuleInfo.getJavaProject(), accessBeanMethodInfo.getUnhandledExceptions().getTargetExceptions());
				String[] parameterTypes = accessBeanMethodInfo.getParameterTypes();
				sb.append("\r\n\tpublic ");
				if (accessBeanMethodInfo.getReturnType() != null) {
					sb.append(accessBeanMethodInfo.getReturnType());
				}
				else {
					sb.append("void");
				}
				sb.append(" ");
				sb.append(userMethodInfo.getMethodName());
				sb.append("(");
				int i = 0;
				for (String parameterType : parameterTypes) {
					if (i != 0) {
						sb.append(", ");
					}
					sb.append(getTargetType(parameterType));						
					sb.append(" ");
					sb.append(accessBeanMethodInfo.getTargetParameterName(i));
					i++;
				}
				sb.append(") {\r\n\t\tinstantiateEntity();\r\n");
				appendInstanceVariableDeclarations(sb, accessBeanMethodInfo);
				if (unhandledExceptions.size() == 0) {
					appendAccessBeanStatements(sb, accessBeanMethodInfo, accessBeanMethodInfo.getStatements(), new HashSet<RelatedEntityInfo>(), "\t\t");
				}
				else {
					sb.append("\t\ttry {\r\n");
					appendAccessBeanStatements(sb, accessBeanMethodInfo, accessBeanMethodInfo.getStatements(), new HashSet<RelatedEntityInfo>(), "\t\t\t");
					sb.append("\t\t}\r\n");
					for (String exception : unhandledExceptions) {
						sb.append("\t\tcatch (");
						sb.append(exception);
						sb.append(" e) {\r\n\t\t\tthrow new PersistenceException(e);\r\n\t\t}\r\n");
					}
				}
				appendRelatedEntityFieldInitializations(sb, accessBeanMethodInfo);
				sb.append("\t}\r\n");
			}
			else if ((userMethodInfo.getFieldInfo() == null || userMethodInfo.getFieldInfo().getCopyHelperProperty() == null) && userMethodInfo.getEjbRelationshipRoleInfo() == null) {
				if (userMethodInfo.getRelatedEntityInfo() != null) {
					RelatedEntityInfo relatedEntityInfo = userMethodInfo.getRelatedEntityInfo();
					EntityInfo parentEntityInfo = relatedEntityInfo.getParentEntityInfo();
					sb.append("\r\n\tpublic ");
					sb.append(parentEntityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName());
					sb.append(" ");
					sb.append(userMethodInfo.getMethodName());
					sb.append("() {\r\n\t\treturn new ");
					sb.append(parentEntityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName());
					sb.append("(getEntity().");
					sb.append(relatedEntityInfo.getGetterName());
					sb.append("());\r\n\t}\r\n");
				}
				else {
					EntityInfo returnTypeEntityInfo = null;
					if (userMethodInfo.getReturnType() != null && iApplicationInfo.isEntityType(userMethodInfo.getReturnType())) {
						returnTypeEntityInfo = iApplicationInfo.getEntityInfoForType(userMethodInfo.getReturnType());
					}
					String methodName = userMethodInfo.getMethodName();
					List<String> parameterTypes = userMethodInfo.getParameterTypes();
					String methodKey = methodName;
					for (String parameterType : parameterTypes) {
						methodKey += "+" + parameterType;
					}
					if (!iAccessBeanInfo.isCopyHelperMethod(methodKey)) {
						sb.append("\r\n\tpublic ");
						if (returnTypeEntityInfo != null) {
							sb.append(returnTypeEntityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName());
						}
						else if (userMethodInfo.getReturnType() != null) {
							sb.append(getTargetType(userMethodInfo.getReturnType()));
						}
						else {
							sb.append("void");
						}
						sb.append(" ");
						sb.append(methodName);
						sb.append("(");
						List<String> parameterNames = userMethodInfo.getParameterNames();
						for (int i = 0; i < parameterTypes.size(); i++) {
							if (i > 0) {
								sb.append(", ");
							}
							sb.append(getTargetType(parameterTypes.get(i)));
							sb.append(" ");
							sb.append(parameterNames.get(i));
						}
						sb.append(")");
						TargetExceptionInfo targetExceptionInfo = TargetExceptionUtil.getEjbMethodUnhandledTargetExceptions(iEntityInfo, userMethodInfo.getKey());
						Collection<String> exceptions = TargetExceptionUtil.getFilteredExceptions(iModuleInfo.getJavaProject(), targetExceptionInfo.getTargetExceptions());
						if (exceptions.size() > 0) {
							boolean firstException = true;
							for (String exception : exceptions) {
								if (firstException) {
									sb.append(" throws ");
									firstException = false;
								}
								else {
									sb.append(", ");
								}
								sb.append(exception);
							}
						}
						sb.append(" {\r\n\t\t");
						if (returnTypeEntityInfo != null) {
							sb.append(returnTypeEntityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName());
							sb.append(" accessBean = null;\r\n\t\t");
							sb.append(userMethodInfo.getReturnType());
							sb.append(" entity = ");
						}
						else {
							if (userMethodInfo.getReturnType() != null) {
								sb.append("return ");
							}
						}
						sb.append("getEntity().");
						sb.append(userMethodInfo.getMethodName());
						FieldInfo fieldInfo = userMethodInfo.getFieldInfo();
						if (userMethodInfo.getReturnType() != null && fieldInfo != null && fieldInfo.getColumnInfo() != null && CHAR.equals(fieldInfo.getColumnInfo().getTypeName()) && fieldInfo.getColumnInfo().getLength() != null && fieldInfo.getColumnInfo().getLength() > 1) {
							if (fieldInfo.getColumnInfo().getNullable()) {
								sb.append("() == null ? null : getEntity().");
								sb.append(userMethodInfo.getMethodName());
							}
							sb.append("().trim();");
						}
						else {
							sb.append("(");
							boolean firstParameter = true;
							for (String parameterName : parameterNames) {
								if (firstParameter) {
									firstParameter = false;
								}
								else {
									sb.append(", ");
								}
								sb.append(parameterName);
							}
							sb.append(");");
						}
						if (returnTypeEntityInfo != null) {
							sb.append("\r\n\t\tif (entity != null) {\r\n\t\t\taccessBean = new ");
							sb.append(returnTypeEntityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName());
							sb.append("(entity);\r\n\t\t}\r\n\t\treturn accessBean;");
						}
						sb.append("\r\n\t}\r\n");
					}
				}
			}
		}
	}
	
	private void appendAccessHelperMethods(StringBuilder sb) {
		if (iEntityInfo.getProtectable()) {
			sb.append("\r\n\tpublic boolean fulfills(Long member, String relationship) throws Exception {");
			sb.append("\r\n\t\treturn getEntity().fulfills(member, relationship);");
			sb.append("\r\n\t}\r\n");
			sb.append("\r\n\tpublic Long getOwner() throws Exception {");
			sb.append("\r\n\t\treturn getEntity().getOwner();");
			sb.append("\r\n\t}\r\n");
		}
		if (iEntityInfo.getGroupable()) {
			sb.append("\r\n\tpublic Object getGroupingAttributeValue(String attributeName, com.ibm.commerce.grouping.GroupingContext groupingContext) throws Exception {");
			sb.append("\r\n\t\treturn getEntity().getGroupingAttributeValue(attributeName, groupingContext);");
			sb.append("\r\n\t}\r\n");
		}
	}
	
	private String getTargetType(String type) {
		String targetType = type;
		String dimensions = "";
		int index = type.indexOf('[');
		if (index > -1) {
			dimensions = targetType.substring(index);
			targetType = targetType.substring(0, index);
		}
		String newType = null;
		if (iApplicationInfo.isEntityInterfaceType(targetType)) {
			EntityInfo entityInfo = iApplicationInfo.getEntityInfoForType(targetType);
			if (entityInfo.getRemote().equals(targetType)) {
				newType = entityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName();
			}
		}
		if (newType == null) {
			newType = iApplicationInfo.getTypeMapping(targetType);
		}
		if (newType != null) {
			targetType = newType;
		}
		return targetType + dimensions;
	}
}
