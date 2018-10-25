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

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.ibm.commerce.jpa.port.info.AccessBeanInfo;
import com.ibm.commerce.jpa.port.info.AccessBeanInfo.CopyHelperProperty;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanCatchClause;
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
import com.ibm.commerce.jpa.port.info.CreatorInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.InstanceVariableInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.StaticFieldInfo;
import com.ibm.commerce.jpa.port.util.JavaUtil;

public class EjbClassParser {
	//private static final String ABSTRACT_ENTITY_DATA_STORE = "AbstractEntityData.Store";
	private static final String GENERATE_KEY_AS_INT = "generateKeyAsInt+java.lang.String";
	private static final String GENERATE_KEY_AS_LONG = "generateKeyAsLong+java.lang.String";
	private static final String INTEGER_TYPE = "java.lang.Integer";
	private static final String LONG_TYPE = "java.lang.Long";
	private static final String EJB_CREATE = "ejbCreate";
	private static final String EJB_STORE = "ejbStore";
	private static final String CREATE = "create";
	private static final String STORE = "store";
	private static final String SET_VALUE = "setValue";
	private static final String COPYRIGHT_FIELD = "COPYRIGHT";
	private static final String OPT_COUNTER = "optCounter";
	private static final String CLOB_ATTRIBUTE_VALUE = "com.ibm.commerce.base.helpers.ClobAttributeValue";
	private static final String BLOB_ATTRIBUTE_VALUE = "com.ibm.commerce.base.helpers.BlobAttributeValue";
	private static final String INITIALIZE_OPT_COUNTER = "initializeOptCounter+java.lang.Object";
	private static final String INITIALIZE_OPT_COUNTER_INFO = "initializeOptCounterInfo+java.lang.Object";
	private static final String INIT_LINKS = "_initLinks";
	private static final String RESET = "reset";
	private static final String INITIALIZE_CLOBS = "initializeClobs";
	private static final String INITIALIZE_FIELDS = "initializeFields";
	private static final String INITIALIZE = "initialize";
	private static final String PROPERTY_SET = "propertySet+java.util.Hashtable";
	private static final String LOG_AUCTION = "logAuction+com.ibm.commerce.negotiation.objects.AuctionAccessBean";
	private static final String INIT_NON_NULL_FIELDS = "initNonNullFields";
	private static final String LOG_BID = "logBid+com.ibm.commerce.negotiation.objects.BidAccessBean";
	private static final String LOG_AUTO_BID = "logAutoBid+com.ibm.commerce.negotiation.objects.AutoBidAccessBean";
	private static final String CHECK_ADDRESS_BOOK = "checkAddressBook+java.lang.Long";
	private static final String SET_ORGANIZATION_ID = "setOrganizationId+java.lang.Long";
	private static final String SET_USER_ID = "setUserId+java.lang.Long";
	private static final String CLEAR_ORDER_ITEMS_CACHE = "clearOrderItemsCache";
	private static final String GET_CLASS = "getClass";
	private static final String BASE_COMPILATION_UNIT = "baseCompilationUnit";
	private static final String TRIM = "trim";
	private static final String ACCESS_BEAN_HASHTABLE = "com.ibm.ivj.ejb.runtime.AccessBeanHashtable";
	private static final String HASH_TABLE = "java.util.Hashtable";
	private static final String PUT = "put";
	private static final String CONTAINS_KEY = "containsKey";
	private static final String COPY_FROM_EJB = "_copyFromEJB";
	private static final String COPY_TO_EJB = "_copyToEJB";
	private static final Set<String> EXEMPT_ACCESS_BEAN_METHODS;
	static {
		EXEMPT_ACCESS_BEAN_METHODS = new HashSet<String>();
		EXEMPT_ACCESS_BEAN_METHODS.add(INITIALIZE_OPT_COUNTER);
		EXEMPT_ACCESS_BEAN_METHODS.add(INITIALIZE_OPT_COUNTER_INFO);
		EXEMPT_ACCESS_BEAN_METHODS.add(INIT_LINKS);
		EXEMPT_ACCESS_BEAN_METHODS.add(RESET);
		EXEMPT_ACCESS_BEAN_METHODS.add(INITIALIZE_CLOBS);
		EXEMPT_ACCESS_BEAN_METHODS.add(SET_ORGANIZATION_ID);
	}
	private static final Set<String> CREATOR_INITIALIZERS;
	static {
		CREATOR_INITIALIZERS = new HashSet<String>();
		CREATOR_INITIALIZERS.add(INITIALIZE);
		CREATOR_INITIALIZERS.add(INITIALIZE_FIELDS);
		CREATOR_INITIALIZERS.add(PROPERTY_SET);
		CREATOR_INITIALIZERS.add(LOG_AUCTION);
		CREATOR_INITIALIZERS.add(INIT_NON_NULL_FIELDS);
		CREATOR_INITIALIZERS.add(LOG_BID);
		CREATOR_INITIALIZERS.add(LOG_AUTO_BID);
		CREATOR_INITIALIZERS.add(CHECK_ADDRESS_BOOK);
		CREATOR_INITIALIZERS.add(SET_USER_ID);
		CREATOR_INITIALIZERS.add(CLEAR_ORDER_ITEMS_CACHE);
	}
	
	private ModuleInfo iModuleInfo;
	private EntityInfo iEntityInfo;
	private ASTParser iASTParser;
	private AccessBeanInfo iAccessBeanInfo;
	private List<CompilationUnit> iEjbCompilationUnits = new ArrayList<CompilationUnit>();
	private Set<String> iEjbCreateMethods = new HashSet<String>();
	private Set<String> iEjbGetterMethods = new HashSet<String>();
	private Set<String> iEjbSetterMethods = new HashSet<String>();
	private Set<String> iExpressionAccessBeanMethods = new HashSet<String>();
	private Set<String> iReferencedAccessBeanMethods = new HashSet<String>();
	private Set<String> iProcessedAccessBeanMethods = new HashSet<String>();
	private Map<String, String> iClobAttributeToSetterMap = new HashMap<String, String>();
	private Map<String, String> iClobAttributeToGetterMap = new HashMap<String, String>();
	private Map<String, CopyHelperProperty> iEjbGetterNameToCopyHelperPropertyMap = new HashMap<String, CopyHelperProperty>();
	private Map<String, CopyHelperProperty> iEjbSetterNameToCopyHelperPropertyMap = new HashMap<String, CopyHelperProperty>();
	
	public EjbClassParser(ASTParser astParser, EntityInfo entityInfo) {
		iASTParser = astParser;
		iModuleInfo = entityInfo.getModuleInfo();
		iEntityInfo = entityInfo;
		iAccessBeanInfo = entityInfo.getAccessBeanInfo();
	}

	public void parse(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("parse " + iEntityInfo.getEjbType().getFullyQualifiedName('.'), IProgressMonitor.UNKNOWN);
//			parseInterfaces();
			iModuleInfo.addDeleteIntendedType(iEntityInfo.getEjbType().getFullyQualifiedName('.'));
			String ejsFinderInterfaceName = iEntityInfo.getEjbType().getPackageFragment().getElementName() + ".EJSFinder" + iEntityInfo.getEjbType().getTypeQualifiedName();
			IType ejsFinderInterfaceType = JavaUtil.resolveType(iEntityInfo.getEjbType(), ejsFinderInterfaceName);
			if (ejsFinderInterfaceType != null) {
				iModuleInfo.addDeleteIntendedType(ejsFinderInterfaceName);
				ITypeHierarchy typeHierarchy = ejsFinderInterfaceType.newTypeHierarchy(iModuleInfo.getJavaProject(), new SubProgressMonitor(progressMonitor, 100));
				IType[] implementingClasses = typeHierarchy.getImplementingClasses(ejsFinderInterfaceType);
				if (implementingClasses != null) {
					for (IType implementingClass : implementingClasses) {
						String typeName = implementingClass.getFullyQualifiedName('.');
						iModuleInfo.addDeleteIntendedType(typeName);
					}
				}
			}
			if (Flags.isAbstract(iEntityInfo.getEjbType().getFlags())) {
				ITypeHierarchy typeHierarchy = iEntityInfo.getEjbType().newTypeHierarchy(iModuleInfo.getJavaProject(), new SubProgressMonitor(progressMonitor, 100));
				IType[] subClasses = typeHierarchy.getSubclasses(iEntityInfo.getEjbType());
				for (IType subClass : subClasses) {
					iModuleInfo.addDeleteIntendedType(subClass.getFullyQualifiedName('.'));
				}
			}
			iEntityInfo.setEjbCompilationUnit(parseEJBType(progressMonitor, iEntityInfo.getEjbType()));
			IType currentType = iEntityInfo.getEjbType();
			iEntityInfo.addClassToEjbHierarchy(currentType.getFullyQualifiedName('.'));
			if (iEntityInfo.getEjbBaseType() != null) {
				iModuleInfo.addDeleteIntendedType(iEntityInfo.getEjbBaseType().getFullyQualifiedName('.'));
				iEntityInfo.setEjbBaseCompilationUnit(parseEJBType(progressMonitor, iEntityInfo.getEjbBaseType()));
				currentType = iEntityInfo.getEjbBaseType();
				iEntityInfo.addClassToEjbHierarchy(currentType.getFullyQualifiedName('.'));
			}
			if (iEntityInfo.getSupertype() != null) {
				iEjbCompilationUnits.add(iEntityInfo.getSupertype().getEjbCompilationUnit());
				currentType = iEntityInfo.getSupertype().getEjbType();
				iEntityInfo.addClassToEjbHierarchy(currentType.getFullyQualifiedName('.'));
				if (iEntityInfo.getSupertype().getEjbBaseType() != null) {
					iEjbCompilationUnits.add(iEntityInfo.getSupertype().getEjbBaseCompilationUnit());
					currentType = iEntityInfo.getSupertype().getEjbBaseType();
					iEntityInfo.addClassToEjbHierarchy(currentType.getFullyQualifiedName('.'));
				}
			}
			progressMonitor.worked(1000);
			while (currentType != null) {
				String baseName = currentType.getSuperclassName();
				if (baseName != null) {
					currentType = JavaUtil.resolveType(currentType, baseName);
					if (currentType.getCompilationUnit() != null) {
						CompilationUnit compilationUnit = iModuleInfo.getCompilationUnit(currentType.getFullyQualifiedName('.'));
						if (compilationUnit != null) {
							iEjbCompilationUnits.add(compilationUnit);
						}
						else {
							compilationUnit = parseEJBType(progressMonitor, currentType);
							compilationUnit.setProperty(BASE_COMPILATION_UNIT, Boolean.TRUE);
							iModuleInfo.setCompilationUnit(currentType.getFullyQualifiedName('.'), compilationUnit);
						}
						iEntityInfo.addClassToEjbHierarchy(currentType.getFullyQualifiedName('.'));
					}
				}
				else {
					currentType = null;
				}
			}
			progressMonitor.worked(1000);
			parseCompilationUnits();
			progressMonitor.worked(1000);
			parseCreators();
			progressMonitor.worked(1000);
			parseGetters();
			progressMonitor.worked(1000);
			parseSetters();
			progressMonitor.worked(1000);
			parseReferencedAccessBeanMethods();
			progressMonitor.worked(1000);
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
	}
	
//	private void parseInterfaces() {
//		try {
//			IType ejbType = iEntityInfo.getEjbType();
//			String[] interfaceNames = ejbType.getSuperInterfaceNames();
//			for (String interfaceName : interfaceNames) {
//				IType interfaceType = JavaUtil.resolveType(ejbType, interfaceName);
//				if (interfaceType != null) {
//					String[] superInterfaceNames = interfaceType.getSuperInterfaceNames();
//					for (String superInterfaceName : superInterfaceNames) {
//						String[][] resolvedInterfaceName = interfaceType.resolveType(superInterfaceName);
//						if (resolvedInterfaceName.length > 0 && ABSTRACT_ENTITY_DATA_STORE.equals(resolvedInterfaceName[0][1])) {
//							iEntityInfo.setEntityDataType(interfaceType.getDeclaringType());
//							iModuleInfo.addDeleteIntendedType(interfaceType.getDeclaringType().getFullyQualifiedName());
//						}
//					}
//				}
//			}
//		}
//		catch (CoreException e) {
//			e.printStackTrace();
//		}
//	}
	
	private CompilationUnit parseEJBType(IProgressMonitor progressMonitor, IType ejbType) {
		CompilationUnit compilationUnit = null;
		if (ejbType != null) {
			iASTParser.setResolveBindings(true);
			iASTParser.setSource(ejbType.getCompilationUnit());
			compilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
			iEjbCompilationUnits.add(compilationUnit);
		}
		return compilationUnit;
	}
	

	private void parseCompilationUnits() {
		for (CompilationUnit ejbCompilationUnit : iEjbCompilationUnits) {
			@SuppressWarnings("unchecked")
			List<TypeDeclaration> typeDeclarations = ejbCompilationUnit.types();
			TypeDeclaration typeDeclaration = typeDeclarations.get(0);
			@SuppressWarnings("unchecked")
			List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
			for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
				if (bodyDeclaration.getNodeType() == BodyDeclaration.METHOD_DECLARATION) {
					MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
					String methodKey = JavaUtil.getMethodKey(methodDeclaration);
					if (ejbCompilationUnit.getProperty(BASE_COMPILATION_UNIT) == null) {
						iEntityInfo.addEjbMethodDeclaration(methodDeclaration);
					}
					parseMethodDeclaration(methodKey, methodDeclaration);
				}
				else if (bodyDeclaration.getNodeType() == BodyDeclaration.FIELD_DECLARATION) {
					parseFieldDeclaration((FieldDeclaration) bodyDeclaration);
				}
			}
			iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
		}
		parseGettersAndSetters();
	}
	
	private void parseMethodDeclaration(String methodKey, MethodDeclaration methodDeclaration) {
		String methodName = methodDeclaration.getName().getIdentifier();
		methodDeclaration.accept(new MethodRequirementsVisitor(methodKey));
//		UserMethodInfo userMethodInfo = iEntityInfo.getUserMethodInfo(methodKey);
//		if (userMethodInfo != null) {
//			@SuppressWarnings("unchecked")
//			List<Name> thrownExceptions = methodDeclaration.thrownExceptions();
//			for (Name thrownException : thrownExceptions) {
//				userMethodInfo.addException(((ITypeBinding) thrownException.resolveBinding()).getQualifiedName());
//			}
//		}
		if (methodName.equals(EJB_CREATE)) {
			iEntityInfo.addPortExemptMethod(methodKey);
			@SuppressWarnings("unchecked")
			List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
			if (parameters.size() > 0) {
				iEjbCreateMethods.add(methodKey);
			}
		}
		else if (methodName.equals(EJB_STORE)) {
			methodDeclaration.getBody().accept(new EjbStoreVisitor());
		}
		else if (methodName.equals(COPY_TO_EJB)) {
			methodDeclaration.getBody().accept(new CopyToEJBVisitor());
		}
		else if (methodName.equals(COPY_FROM_EJB)) {
			methodDeclaration.getBody().accept(new CopyFromEJBVisitor());
		}
		else if (methodDeclaration.resolveBinding() != null) {
			int modifiers = methodDeclaration.resolveBinding().getModifiers();
			if (Flags.isPublic(modifiers) && Flags.isStatic(modifiers)) {
				iEntityInfo.addStaticMethod(methodKey);
			}
		}
	}
	
	private void parseGettersAndSetters() {
		for (CompilationUnit ejbCompilationUnit : iEjbCompilationUnits) {
			@SuppressWarnings("unchecked")
			List<TypeDeclaration> typeDeclarations = ejbCompilationUnit.types();
			TypeDeclaration typeDeclaration = typeDeclarations.get(0);
			@SuppressWarnings("unchecked")
			List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
			for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
				if (bodyDeclaration.getNodeType() == BodyDeclaration.METHOD_DECLARATION) {
					MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
					String methodName = methodDeclaration.getName().getIdentifier();
					String methodKey = JavaUtil.getMethodKey(methodDeclaration);
					if (methodDeclaration.parameters().size() == 0 && methodDeclaration.getReturnType2() != null) {
						String returnType = methodDeclaration.getReturnType2().resolveBinding().getQualifiedName();
						if (methodDeclaration.getBody() != null) {
							if (!methodName.startsWith(EJB_CREATE)) {
								methodDeclaration.getBody().accept(new GeneratePrimaryKeyMethodVisitor(methodKey, returnType));
							}
							if (iEjbGetterNameToCopyHelperPropertyMap.containsKey(methodName) || (iEjbGetterNameToCopyHelperPropertyMap.size() == 0 && (methodName.startsWith("get") || methodName.startsWith("is"))) || (iAccessBeanInfo != null && iAccessBeanInfo.isExcludedPropertyMethod(methodKey))) {
								GetterMethodVisitor getterMethodVisitor = new GetterMethodVisitor(methodKey, methodName);
								methodDeclaration.getBody().accept(getterMethodVisitor);
							}
						}
						else if (methodName.startsWith("get")) {
							String fieldName = methodName.substring(3);
							FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
							if (fieldInfo == null) {
								fieldInfo = iEntityInfo.getFieldInfoByName("i" + fieldName);
							}
							if (fieldInfo == null && fieldName.length() > 1) {
								fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
								fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
							}
							if (fieldInfo != null) {
								fieldInfo.setTypeName(returnType);
								fieldInfo.setGetterName(methodName);
								iEntityInfo.addPortExemptMethod(methodKey);
							}
						}
					}
					else if (methodDeclaration.parameters().size() == 1 && methodName.startsWith("set")) {
						if (methodDeclaration.getBody() != null) {
							if (iEjbSetterNameToCopyHelperPropertyMap.containsKey(methodName) || iEjbSetterNameToCopyHelperPropertyMap.size() == 0) {
								SetterMethodVisitor setterMethodVisitor = new SetterMethodVisitor(methodName);
								methodDeclaration.getBody().accept(setterMethodVisitor);
								FieldInfo fieldInfo = setterMethodVisitor.getFieldInfo();
								if (fieldInfo != null) {
									fieldInfo.setSetterName(methodName);
									iEntityInfo.setFieldSetterMethodKey(fieldInfo, methodKey);
									iEntityInfo.addPortExemptMethod(methodKey);
									iEjbSetterMethods.add(methodKey);
									CopyHelperProperty copyHelperProperty = iEjbSetterNameToCopyHelperPropertyMap.get(methodName);
									if (copyHelperProperty != null && copyHelperProperty.getFieldInfo() == null) {
										copyHelperProperty.setFieldInfo(fieldInfo);
										fieldInfo.setCopyHelperProperty(copyHelperProperty);
									}
								}
							}
						}
						else {
							String fieldName = methodName.substring(3);
							FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
							if (fieldInfo == null) {
								fieldInfo = iEntityInfo.getFieldInfoByName("i" + fieldName);
							}
							if (fieldInfo == null && fieldName.length() > 1) {
								fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
								fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
							}
							if (fieldInfo != null) {
								fieldInfo.setSetterName(methodName);
								iEntityInfo.setFieldSetterMethodKey(fieldInfo, methodKey);
								iEntityInfo.addPortExemptMethod(methodKey);
							}
						}
					}
				}
			}
		}
	}
	
	private void parseAccessBeanMethod(AccessBeanMethodInfo accessBeanMethodInfo, MethodDeclaration methodDeclaration) {
		iProcessedAccessBeanMethods.add(accessBeanMethodInfo.getMethodKey());
		@SuppressWarnings("unchecked")
		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		methodDeclaration.getBody().accept(new AccessBeanMethodVisitor(parameters, accessBeanMethodInfo, accessBeanMethodInfo.getStatements()));
		String parameterNames[] = accessBeanMethodInfo.getParameterNames();
		for (int i = 0; i < parameterNames.length; i++) {
			String parameterName = parameterNames[i];
			Set<FieldInfo> fieldsInitializedByParameter = accessBeanMethodInfo.getFieldsInitializedByParameter(i);
			if (fieldsInitializedByParameter != null && fieldsInitializedByParameter.size() == 1) {
				for (FieldInfo fieldInfo : fieldsInitializedByParameter) {
					String newParameterName = fieldInfo.getTargetFieldName();
					for (int j = 0; j < parameterNames.length; j++) {
						if (j != i && newParameterName.equals(parameterNames[j])) {
							newParameterName = parameterName;
							break;
						}
					}
					if (!newParameterName.equals(parameterName)) {
						accessBeanMethodInfo.setTargetParameterName(i, newParameterName);
					}
				}
			}
		}
	}
	
	private void parseFieldDeclaration(FieldDeclaration fieldDeclaration) {
		Type fieldType = fieldDeclaration.getType();
		ITypeBinding typeBinding = fieldType.resolveBinding();
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> ejbVariableDeclarationFragments = fieldDeclaration.fragments();
		for (VariableDeclarationFragment ejbVariableDeclarationFragment : ejbVariableDeclarationFragments) {
			String fieldName = ejbVariableDeclarationFragment.getName().getIdentifier();
			FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
			if (fieldInfo == null) {
				if ((iEntityInfo.getSupertype() == null || iEntityInfo.getSupertype().getFieldInfoByName(fieldName) == null) && !COPYRIGHT_FIELD.equals(fieldName)) {
					if ((fieldDeclaration.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) == (Modifier.STATIC | Modifier.FINAL)) {
						StaticFieldInfo staticFieldInfo = new StaticFieldInfo(fieldName, typeBinding.getQualifiedName(), ejbVariableDeclarationFragment.getInitializer());
						iEntityInfo.addEjbStaticFieldInfo(staticFieldInfo);
					}
					else {
						InstanceVariableInfo instanceVariableInfo = new InstanceVariableInfo(fieldName, typeBinding.getQualifiedName(), ejbVariableDeclarationFragment.getInitializer());
						iEntityInfo.addEjbInstanceVariableInfo(instanceVariableInfo);
					}
				}
				if (ejbVariableDeclarationFragment.getInitializer() != null) {
					ejbVariableDeclarationFragment.getInitializer().accept(new FieldRequirementsVisitor(fieldName));
				}
			}
			else {
				fieldInfo.setTypeName(typeBinding.getQualifiedName());
				iEntityInfo.addPortExemptField(fieldName);
			}
		}
	}
	
	private void parseCreators() {
		for (String ejbCreateMethodKey : iEjbCreateMethods) {
			List<MethodDeclaration> methodDeclarations = iEntityInfo.getEjbMethodDeclarations(ejbCreateMethodKey);
			for (MethodDeclaration methodDeclaration : methodDeclarations) {
				@SuppressWarnings("unchecked")
				List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
				String[] parameterTypes = new String[parameters.size()];
				String[] parameterNames = new String[parameters.size()];
				int i = 0;
				for (SingleVariableDeclaration parameter : parameters) {
					parameterNames[i] = parameter.getName().getIdentifier();
					Type type = parameter.getType();
					ITypeBinding typeBinding = type.resolveBinding();
					if (typeBinding != null) {
						parameterTypes[i] = typeBinding.getQualifiedName();
						if (iEntityInfo.getPrimaryKeyField() == null && parameterTypes[i].equals(iEntityInfo.getPrimaryKeyClass())) {
							parameterNames[i] = "key";
						}
					}
					i++;
				}
				CreatorInfo creatorInfo = iEntityInfo.getCreatorInfo(parameterTypes);
				if (creatorInfo != null) {
					AccessBeanMethodInfo accessBeanMethodInfo = new AccessBeanMethodInfo(iEntityInfo, ejbCreateMethodKey, methodDeclaration.getName().getIdentifier(), parameterTypes, null);
					for (i = 0; i < parameterNames.length; i++) {
						accessBeanMethodInfo.setParameterName(i, parameterNames[i]);
					}
					parseAccessBeanMethod(accessBeanMethodInfo, methodDeclaration);
					creatorInfo.addAccessBeanMethodInfo(accessBeanMethodInfo);
				}
			}
		}
	}
	
	private void parseGetters() {
		for (String ejbGetterMethodKey : iEjbGetterMethods) {
			List<MethodDeclaration> methodDeclarations = iEntityInfo.getEjbMethodDeclarations(ejbGetterMethodKey);
			for (MethodDeclaration methodDeclaration : methodDeclarations) {
				String returnType = null;
				if (methodDeclaration.getReturnType2() != null) {
					returnType = methodDeclaration.getReturnType2().resolveBinding().getQualifiedName();
				}
				AccessBeanMethodInfo accessBeanMethodInfo = new AccessBeanMethodInfo(iEntityInfo, ejbGetterMethodKey, methodDeclaration.getName().getIdentifier(), new String[0], returnType);
				parseAccessBeanMethod(accessBeanMethodInfo, methodDeclaration);
				if (iEntityInfo.getAccessBeanMethodInfo(ejbGetterMethodKey) == null) {
					iEntityInfo.setAccessBeanMethodInfo(ejbGetterMethodKey, accessBeanMethodInfo);
				}
				else {
					iEntityInfo.getAccessBeanMethodInfo(ejbGetterMethodKey).addSuperAccessBeanMethodInfo(accessBeanMethodInfo);
				}
			}
		}
	}
	
	private void parseSetters() {
		for (String ejbSetterMethodKey : iEjbSetterMethods) {
			List<MethodDeclaration> methodDeclarations = iEntityInfo.getEjbMethodDeclarations(ejbSetterMethodKey);
			for (MethodDeclaration methodDeclaration : methodDeclarations) {
				@SuppressWarnings("unchecked")
				List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
				String[] parameterTypes = new String[parameters.size()];
				String[] parameterNames = new String[parameters.size()];
				int i = 0;
				for (SingleVariableDeclaration parameter : parameters) {
					parameterNames[i] = parameter.getName().getIdentifier();
					Type type = parameter.getType();
					ITypeBinding typeBinding = type.resolveBinding();
					if (typeBinding != null) {
						parameterTypes[i] = typeBinding.getQualifiedName();
					}
					i++;
				}
				AccessBeanMethodInfo accessBeanMethodInfo = new AccessBeanMethodInfo(iEntityInfo, ejbSetterMethodKey, methodDeclaration.getName().getIdentifier(), parameterTypes, null);
				for (i = 0; i < parameterNames.length; i++) {
					accessBeanMethodInfo.setParameterName(i, parameterNames[i]);
				}
				parseAccessBeanMethod(accessBeanMethodInfo, methodDeclaration);
				FieldInfo fieldInfo = iEntityInfo.getFieldInfoBySetterMethodKey(ejbSetterMethodKey);
				if (fieldInfo != null) {
					accessBeanMethodInfo.setTargetParameterName(0, fieldInfo.getTargetFieldName());
				}
				if (iEntityInfo.getAccessBeanMethodInfo(ejbSetterMethodKey) == null) {
					iEntityInfo.setAccessBeanMethodInfo(ejbSetterMethodKey, accessBeanMethodInfo);
				}
				else {
					iEntityInfo.getAccessBeanMethodInfo(ejbSetterMethodKey).addSuperAccessBeanMethodInfo(accessBeanMethodInfo);
				}
			}
		}
	}
	
	private void parseReferencedAccessBeanMethods() {
		while (!iReferencedAccessBeanMethods.isEmpty()) {
			Set<String> accessBeanMethods = iReferencedAccessBeanMethods;
			iReferencedAccessBeanMethods = new HashSet<String>();
			for (String methodKey : accessBeanMethods) {
				List<MethodDeclaration> methodDeclarations = iEntityInfo.getEjbMethodDeclarations(methodKey);
				if (methodDeclarations != null) {
					for (MethodDeclaration methodDeclaration : methodDeclarations) {
						String returnType = null;
						if (iExpressionAccessBeanMethods.contains(methodKey) && methodDeclaration.getReturnType2() != null) {
							returnType = methodDeclaration.getReturnType2().resolveBinding().getQualifiedName();
						}
						@SuppressWarnings("unchecked")
						List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
						String[] parameterTypes = new String[parameters.size()];
						String[] parameterNames = new String[parameters.size()];
						int i = 0;
						for (SingleVariableDeclaration parameter : parameters) {
							parameterNames[i] = parameter.getName().getIdentifier();
							Type type = parameter.getType();
							ITypeBinding typeBinding = type.resolveBinding();
							if (typeBinding != null) {
								parameterTypes[i] = typeBinding.getQualifiedName();
							}
							i++;
						}
						AccessBeanMethodInfo accessBeanMethodInfo = new AccessBeanMethodInfo(iEntityInfo, methodKey, methodDeclaration.getName().getIdentifier(), parameterTypes, returnType);
						for (i = 0; i < parameterNames.length; i++) {
							accessBeanMethodInfo.setParameterName(i, parameterNames[i]);
						}
						parseAccessBeanMethod(accessBeanMethodInfo, methodDeclaration);
						if (iEntityInfo.getAccessBeanMethodInfo(methodKey) == null) {
							iEntityInfo.setAccessBeanMethodInfo(methodKey, accessBeanMethodInfo);
						}
						else {
							iEntityInfo.getAccessBeanMethodInfo(methodKey).addSuperAccessBeanMethodInfo(accessBeanMethodInfo);
						}
					}
				}
			}
		}
	}
	
	private class AccessBeanMethodVisitor extends ASTVisitor {
		private List<SingleVariableDeclaration> iParameters;
		private AccessBeanMethodInfo iAccessBeanMethodInfo;
		private List<AccessBeanStatement> iAccessBeanStatementList;
		private AccessBeanExpressionVisitor iAccessBeanExpressionVisitor;
		
		public AccessBeanMethodVisitor(List<SingleVariableDeclaration> parameters, AccessBeanMethodInfo accessBeanMethodInfo, List<AccessBeanStatement> accessBeanStatementList) {
			iParameters = parameters;
			iAccessBeanMethodInfo = accessBeanMethodInfo;
			iAccessBeanStatementList = accessBeanStatementList;
			iAccessBeanExpressionVisitor = new AccessBeanExpressionVisitor();
		}

		public boolean visit(VariableDeclarationStatement variableDeclarationStatement) {
			AccessBeanVariableDeclarationStatement accessBeanVariableDeclarationStatement = new AccessBeanVariableDeclarationStatement();
			accessBeanVariableDeclarationStatement.setType(variableDeclarationStatement.getType().resolveBinding().getQualifiedName());
			@SuppressWarnings("unchecked")
			List<VariableDeclarationFragment> variableDeclarationFragments = variableDeclarationStatement.fragments();
			for (VariableDeclarationFragment variableDeclarationFragment : variableDeclarationFragments) {
				accessBeanVariableDeclarationStatement.setVariableName(variableDeclarationFragment.getName().getIdentifier());
				Expression initializationExpression = variableDeclarationFragment.getInitializer();
				accessBeanVariableDeclarationStatement.setInitializationExpression(initializationExpression);
				initializationExpression.accept(iAccessBeanExpressionVisitor);
				for (int i = 0; i < variableDeclarationFragment.getExtraDimensions(); i++) {
					accessBeanVariableDeclarationStatement.setType(accessBeanVariableDeclarationStatement.getType() + "[]");
				}
				break;
			}
			iAccessBeanStatementList.add(accessBeanVariableDeclarationStatement);
			return true;
		}
		
		public boolean visit(IfStatement ifStatement) {
			Expression ifExpression = ifStatement.getExpression();
			AccessBeanIfStatement accessBeanIfStatement = new AccessBeanIfStatement();
			ifExpression.accept(iAccessBeanExpressionVisitor);
			accessBeanIfStatement.setIfExpression(ifExpression);
			ifStatement.getThenStatement().accept(new AccessBeanMethodVisitor(iParameters, iAccessBeanMethodInfo, accessBeanIfStatement.getThenStatements()));
			if (ifStatement.getElseStatement() != null) {
				ifStatement.getElseStatement().accept(new AccessBeanMethodVisitor(iParameters, iAccessBeanMethodInfo, accessBeanIfStatement.getElseStatements()));	
			}
			iAccessBeanStatementList.add(accessBeanIfStatement);
			return false;
		}
		
		public boolean visit(TryStatement tryStatement) {
			AccessBeanTryStatement accessBeanTryStatement = new AccessBeanTryStatement();
			tryStatement.getBody().accept(new AccessBeanMethodVisitor(iParameters, iAccessBeanMethodInfo, accessBeanTryStatement.getTryStatements()));
			@SuppressWarnings("unchecked")
			List<CatchClause> catchClauses = tryStatement.catchClauses();
			for (CatchClause catchClause : catchClauses) {
				AccessBeanCatchClause accessBeanCatchClause = new AccessBeanCatchClause(iAccessBeanMethodInfo);
				accessBeanTryStatement.getCatchClauses().add(accessBeanCatchClause);
				SingleVariableDeclaration catchException = catchClause.getException();
				accessBeanCatchClause.setExceptionType(catchException.getType().resolveBinding().getQualifiedName());
				accessBeanCatchClause.setExceptionVariableName(catchException.getName().getIdentifier());
				catchClause.getBody().accept(new AccessBeanMethodVisitor(iParameters, iAccessBeanMethodInfo, accessBeanCatchClause.getCatchStatements()));
			}
			if (tryStatement.getFinally() != null) {
				tryStatement.getFinally().accept(new AccessBeanMethodVisitor(iParameters, iAccessBeanMethodInfo, accessBeanTryStatement.getFinallyStatements()));
			}
			iAccessBeanStatementList.add(accessBeanTryStatement);
			return false;
		}
		
		public boolean visit(Assignment assignment) {
			FieldInfo fieldInfo = null;
			String variableName = null;
			String instanceVariableName = null;
			Expression leftHandSide = assignment.getLeftHandSide();
			switch (leftHandSide.getNodeType()) {
				case Expression.FIELD_ACCESS: {
					FieldAccess fieldAccess = (FieldAccess) leftHandSide;
					if (fieldAccess.getExpression().getNodeType() == Expression.THIS_EXPRESSION) {
						instanceVariableName = fieldAccess.getName().getIdentifier();
						fieldInfo = iEntityInfo.getFieldInfoByName(instanceVariableName);
					}
					break;
				}
				case Expression.SIMPLE_NAME: {
					SimpleName simpleName = (SimpleName) leftHandSide;
					IBinding binding = simpleName.resolveBinding();
					if (binding instanceof IVariableBinding) {
						IVariableBinding variableBinding = (IVariableBinding) binding;
						if (variableBinding.isField()) {
							instanceVariableName = simpleName.getIdentifier();
							fieldInfo = iEntityInfo.getFieldInfoByName(instanceVariableName);
						}
						else {
							variableName = simpleName.getIdentifier();
						}
					}
					break;
				}
				case Expression.SUPER_FIELD_ACCESS: {
					SuperFieldAccess superFieldAccess = (SuperFieldAccess) leftHandSide;
					instanceVariableName = superFieldAccess.getName().getIdentifier();
					fieldInfo = iEntityInfo.getFieldInfoByName(instanceVariableName);
					break;
				}
			}
			if (fieldInfo != null && !OPT_COUNTER.equals(fieldInfo.getFieldName())) {
				Expression rightHandSide = assignment.getRightHandSide();
				Expression initializationExpression = rightHandSide;
				if (rightHandSide.getNodeType() == Expression.CAST_EXPRESSION) {
					CastExpression castExpression = (CastExpression) rightHandSide;
					rightHandSide = castExpression.getExpression();
				}
				if (rightHandSide.getNodeType() == Expression.SIMPLE_NAME) {
					SimpleName simpleName = (SimpleName) rightHandSide;
					IBinding binding = simpleName.resolveBinding();
					if (binding instanceof IVariableBinding) {
						IVariableBinding variableBinding = (IVariableBinding) binding;
						if (variableBinding.isParameter()) {
							int i = 0;
							for (SingleVariableDeclaration parameter : iParameters) {
								if (parameter.getName().getIdentifier().equals(simpleName.getIdentifier())) {
									AccessBeanParameterInitializedFieldStatement accessBeanParameterInitializedFieldStatement = new AccessBeanParameterInitializedFieldStatement(fieldInfo, i);
									iAccessBeanStatementList.add(accessBeanParameterInitializedFieldStatement);
									iAccessBeanMethodInfo.addFieldInitializedByParameter(i, fieldInfo);
									iAccessBeanMethodInfo.addInitializedField(fieldInfo);
									initializationExpression = null;
									break;
								}
								i++;
							}
						}
					}
				}
				else if (rightHandSide.getNodeType() == Expression.QUALIFIED_NAME) {
					QualifiedName qualifiedName = (QualifiedName) rightHandSide;
					if (qualifiedName.getQualifier().getNodeType() == Expression.SIMPLE_NAME) {
						SimpleName simpleName = (SimpleName) qualifiedName.getQualifier();
						if (simpleName.resolveTypeBinding().getQualifiedName().equals(iEntityInfo.getPrimaryKeyClass())) {
							AccessBeanKeyParameterInitializedFieldStatement accessBeanKeyParameterInitializedFieldStatement = new AccessBeanKeyParameterInitializedFieldStatement();
							accessBeanKeyParameterInitializedFieldStatement.setFieldInfo(fieldInfo);
							iAccessBeanStatementList.add(accessBeanKeyParameterInitializedFieldStatement);
							initializationExpression = null;
						}
					}
				}
				else if (rightHandSide.getNodeType() == Expression.METHOD_INVOCATION) {
					MethodInvocation methodInvocation = (MethodInvocation) rightHandSide;
					String methodName = methodInvocation.getName().getIdentifier();
					if (methodInvocation.getExpression() != null && (methodName.equals(CREATE) || methodName.equals(SET_VALUE)) && methodInvocation.arguments().size() == 1) {
						String expressionType = methodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
						if (CLOB_ATTRIBUTE_VALUE.equals(expressionType) || BLOB_ATTRIBUTE_VALUE.equals(expressionType)) {
							Expression argument = (Expression) methodInvocation.arguments().get(0);
							initializationExpression = argument;
							if (argument.getNodeType() == Expression.SIMPLE_NAME) {
								SimpleName simpleName = (SimpleName) argument;
								IBinding binding = simpleName.resolveBinding();
								if (binding instanceof IVariableBinding) {
									IVariableBinding variableBinding = (IVariableBinding) binding;
									if (variableBinding.isParameter()) {
										int i = 0;
										for (SingleVariableDeclaration parameter : iParameters) {
											if (parameter.getName().getIdentifier().equals(simpleName.getIdentifier())) {
												AccessBeanParameterInitializedFieldStatement accessBeanParameterInitializedFieldStatement = new AccessBeanParameterInitializedFieldStatement(fieldInfo, i);
												iAccessBeanStatementList.add(accessBeanParameterInitializedFieldStatement);
												iAccessBeanMethodInfo.addFieldInitializedByParameter(i, fieldInfo);
												iAccessBeanMethodInfo.addInitializedField(fieldInfo);
												initializationExpression = null;
												break;
											}
											i++;
										}
									}
								}
							}
						}
					}
				}
				if (initializationExpression != null && initializationExpression.getNodeType() != ASTNode.NULL_LITERAL) {
					AccessBeanExpressionInitializedFieldStatement accessBeanExpressionInitializedFieldStatement = new AccessBeanExpressionInitializedFieldStatement();
					accessBeanExpressionInitializedFieldStatement.setFieldInfo(fieldInfo);
					accessBeanExpressionInitializedFieldStatement.setExpression(initializationExpression);
					initializationExpression.accept(iAccessBeanExpressionVisitor);
					iAccessBeanStatementList.add(accessBeanExpressionInitializedFieldStatement);
					iAccessBeanMethodInfo.addInitializedField(fieldInfo);
				}
			}
			else if (variableName != null) {
				AccessBeanVariableAssignmentStatement accessBeanVariableAssignmentStatement = new AccessBeanVariableAssignmentStatement();
				accessBeanVariableAssignmentStatement.setVariableName(variableName);
				Expression assignmentExpression = assignment.getRightHandSide();
				accessBeanVariableAssignmentStatement.setAssignmentExpression(assignmentExpression);
				assignmentExpression.accept(iAccessBeanExpressionVisitor);
				iAccessBeanStatementList.add(accessBeanVariableAssignmentStatement);
			}
			else if (fieldInfo == null && instanceVariableName != null && !OPT_COUNTER.equals(instanceVariableName) && assignment.getRightHandSide().getNodeType() != ASTNode.NULL_LITERAL) {
				AccessBeanInstanceVariableAssignmentStatement accessBeanInstanceVariableAssignmentStatement = new AccessBeanInstanceVariableAssignmentStatement();
				accessBeanInstanceVariableAssignmentStatement.setInstanceVariableName(instanceVariableName);
				Expression assignmentExpression = assignment.getRightHandSide();
				accessBeanInstanceVariableAssignmentStatement.setAssignmentExpression(assignmentExpression);
				assignmentExpression.accept(iAccessBeanExpressionVisitor);
				iAccessBeanStatementList.add(accessBeanInstanceVariableAssignmentStatement);
			}
			return false;
		}
		
		public boolean visit(ReturnStatement returnStatement) {
			boolean visitChildren = true;
			Expression returnExpression = returnStatement.getExpression();
			if ((iExpressionAccessBeanMethods.contains(iAccessBeanMethodInfo.getMethodKey()) || iEjbGetterMethods.contains(iAccessBeanMethodInfo.getMethodKey())) &&
				(returnExpression == null || returnExpression.getNodeType() != ASTNode.SUPER_METHOD_INVOCATION)) {
				AccessBeanReturnStatement accessBeanReturnStatement = new AccessBeanReturnStatement();
				accessBeanReturnStatement.setReturnExpression(returnExpression);
				returnExpression.accept(iAccessBeanExpressionVisitor);
				iAccessBeanStatementList.add(accessBeanReturnStatement);
			}
			else if (returnExpression != null) {
				processStatementExpression(returnExpression);
			}
			return visitChildren;
		}
		
		public boolean visit(ExpressionStatement expressionStatement) {
			return processStatementExpression(expressionStatement.getExpression());
		}
		
		private boolean processStatementExpression(Expression expression) {
			boolean visitChildren = true;
			if (expression.getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation methodInvocation = (MethodInvocation) expression;
				@SuppressWarnings("unchecked")
				List<Expression> arguments = methodInvocation.arguments();
				if (methodInvocation.getExpression() == null || methodInvocation.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
					visitChildren = processStatementMethodInvocation(methodInvocation.resolveMethodBinding(), arguments);
				}
				else if (methodInvocation.getExpression().getNodeType() == ASTNode.SIMPLE_NAME) {
					SimpleName simpleName = (SimpleName) methodInvocation.getExpression();
					IBinding binding = simpleName.resolveBinding();
					if (binding != null && binding.getKind() == IBinding.VARIABLE) {
						IVariableBinding variableBinding = (IVariableBinding) binding;
						if (!variableBinding.isField()) {
							AccessBeanVariableMethodInvocationStatement accessBeanVariableMethodInvocationStatement = new AccessBeanVariableMethodInvocationStatement();
							accessBeanVariableMethodInvocationStatement.setVariableName(simpleName.getIdentifier());
							accessBeanVariableMethodInvocationStatement.setVariableType(variableBinding.getType().getQualifiedName());
							accessBeanVariableMethodInvocationStatement.setMethodName(methodInvocation.getName().getIdentifier());
							accessBeanVariableMethodInvocationStatement.setMethodKey(JavaUtil.getMethodKey(methodInvocation.resolveMethodBinding()));
							for (Expression argument : arguments) {
								accessBeanVariableMethodInvocationStatement.addArgument(argument);
								argument.accept(iAccessBeanExpressionVisitor);
							}
							iAccessBeanStatementList.add(accessBeanVariableMethodInvocationStatement);
						}
					}
				}
			}
			else if (expression.getNodeType() == ASTNode.SUPER_METHOD_INVOCATION) {
				SuperMethodInvocation superMethodInvocation = (SuperMethodInvocation) expression;
				String methodKey = JavaUtil.getMethodKey(superMethodInvocation.resolveMethodBinding());
				if (iAccessBeanMethodInfo.getMethodKey().equals(methodKey)) {
					AccessBeanSuperMethodInvocationStatement accessBeanSuperMethodInvocationStatement = new AccessBeanSuperMethodInvocationStatement();
					iAccessBeanStatementList.add(accessBeanSuperMethodInvocationStatement);
					@SuppressWarnings("unchecked")
					List<Expression> arguments = superMethodInvocation.arguments();
					for (Expression argument : arguments) {
						accessBeanSuperMethodInvocationStatement.addArgument(argument);
						argument.accept(iAccessBeanExpressionVisitor);
					}
					iAccessBeanMethodInfo.setCallsSuperAccessBeanMethod(true);
				}
				else {
					@SuppressWarnings("unchecked")
					List<Expression> arguments = superMethodInvocation.arguments();
					visitChildren = processStatementMethodInvocation(superMethodInvocation.resolveMethodBinding(), arguments);
				}
			}
			return visitChildren;
		}
		
		private boolean processStatementMethodInvocation(IMethodBinding methodBinding, List<Expression> arguments) {
			boolean visitChildren = true;
			String methodName = methodBinding.getName();
			FieldInfo fieldInfo = null;
			if (methodName.startsWith("set") && arguments.size() == 1) {
				String parameterType = methodBinding.getParameterTypes()[0].getQualifiedName();
				fieldInfo = iEntityInfo.getFieldInfoBySetterName(methodName);
				if (fieldInfo == null) {
					String fieldName = methodName.substring(3);
					fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
					if (fieldInfo == null) {
						fieldInfo = iEntityInfo.getFieldInfoByName("i" + fieldName);
					}
					if (fieldInfo == null) {
						fieldInfo = iEntityInfo.getFieldInfoByName(Introspector.decapitalize(fieldName));
					}
				}
				if (fieldInfo != null && !parameterType.equals(fieldInfo.getTypeName())) {
					fieldInfo = null;
				}
				if (fieldInfo != null) {
					Expression setterExpression = arguments.get(0);
					if (setterExpression.getNodeType() == ASTNode.SIMPLE_NAME) {
						SimpleName simpleName = (SimpleName) setterExpression;
						IBinding binding = simpleName.resolveBinding();
						if (binding instanceof IVariableBinding) {
							IVariableBinding variableBinding = (IVariableBinding) binding;
							if (variableBinding.isParameter()) {
								int i = 0;
								for (SingleVariableDeclaration parameter : iParameters) {
									if (parameter.getName().getIdentifier().equals(simpleName.getIdentifier())) {
										AccessBeanParameterInitializedFieldStatement accessBeanParameterInitializedFieldStatement = new AccessBeanParameterInitializedFieldStatement(fieldInfo, i);
										iAccessBeanStatementList.add(accessBeanParameterInitializedFieldStatement);
										iAccessBeanMethodInfo.addFieldInitializedByParameter(i, fieldInfo);
										iAccessBeanMethodInfo.addInitializedField(fieldInfo);
//										iCreatorInfo.setParameterName(i, fieldInfo.getTargetFieldName());
//										if (!simpleName.getIdentifier().equals(fieldInfo.getTargetFieldName())) {
//											iCreatorInfo.addVariableNameMapping(simpleName.getIdentifier(), fieldInfo.getTargetFieldName());
//										}
										setterExpression = null;
										break;
									}
									i++;
								}
							}
						}
					}
					if (setterExpression != null && setterExpression.getNodeType() != ASTNode.NULL_LITERAL) {
						AccessBeanExpressionInitializedFieldStatement accessBeanExpressionInitializedFieldStatement = new AccessBeanExpressionInitializedFieldStatement();
						accessBeanExpressionInitializedFieldStatement.setFieldInfo(fieldInfo);
						accessBeanExpressionInitializedFieldStatement.setExpression(setterExpression);
						setterExpression.accept(iAccessBeanExpressionVisitor);
						iAccessBeanStatementList.add(accessBeanExpressionInitializedFieldStatement);
						iAccessBeanMethodInfo.addInitializedField(fieldInfo);
					}
					visitChildren = false;
				}
			}
			if (fieldInfo == null) {
				String methodKey = JavaUtil.getMethodKey(methodBinding);
				if (iEntityInfo.getUserMethodInfo(methodKey) != null) {
					AccessBeanUserMethodInvocationStatement accessBeanUserMethodInvocationStatement = new AccessBeanUserMethodInvocationStatement(methodKey);
					for (Expression argument : arguments) {
						accessBeanUserMethodInvocationStatement.addArgument(argument);
						argument.accept(iAccessBeanExpressionVisitor);
					}
					iAccessBeanStatementList.add(accessBeanUserMethodInvocationStatement);
				}
				else if (!EXEMPT_ACCESS_BEAN_METHODS.contains(methodKey)) {
					if (CREATOR_INITIALIZERS.contains(methodKey) || methodKey.startsWith(EJB_CREATE)) {
						if (methodKey.equals(iAccessBeanMethodInfo.getMethodKey())) {
							System.out.println("Marking method invalid: "+iEntityInfo.getEjbName()+" "+iAccessBeanMethodInfo.getMethodKey());
							iAccessBeanMethodInfo.markInvalid();
						}
						else {
							AccessBeanMethodInvocationStatement accessBeanMethodInvocationStatement = new AccessBeanMethodInvocationStatement(methodKey);
							for (Expression argument : arguments) {
								accessBeanMethodInvocationStatement.addArgument(argument);
								argument.accept(iAccessBeanExpressionVisitor);
							}
							iAccessBeanStatementList.add(accessBeanMethodInvocationStatement);
							if (!methodKey.startsWith(EJB_CREATE) && !iProcessedAccessBeanMethods.contains(methodKey)) {
								iReferencedAccessBeanMethods.add(methodKey);
							}
						}
					}
					else {
						System.out.println("access bean method invocation statement: "+methodKey+" in ejbCreate "+iEntityInfo.getEjbClass());
					}
				}
			}
			return visitChildren;
		}
	}
	
	private class GeneratePrimaryKeyMethodVisitor extends ASTVisitor {
		private String iMethodKey;
		private String iReturnType;
		
		public GeneratePrimaryKeyMethodVisitor(String methodKey, String returnType) {
			iMethodKey = methodKey;
			iReturnType = returnType;
		}
		
		public boolean visit(SimpleName node) {
			if ("ECKeyManager".equals(node.getIdentifier())) {
				iEntityInfo.setGeneratePrimaryKeyMethodKey(iMethodKey);
				iEntityInfo.setGeneratedPrimaryKeyType(iReturnType);
				iEntityInfo.addPortExemptMethod(iMethodKey);
			}
			return false;
		}
		
		public boolean visit(MethodInvocation methodInvocation) {
			if ((methodInvocation.getExpression() == null || methodInvocation.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) &&
					methodInvocation.arguments().size() == 1) {
				String methodKey = JavaUtil.getMethodKey(methodInvocation.resolveMethodBinding());
				if (methodKey.equals(GENERATE_KEY_AS_INT) || methodKey.equals(GENERATE_KEY_AS_LONG)) {
					iEntityInfo.setGeneratePrimaryKeyMethodKey(iMethodKey);
					iEntityInfo.setGeneratedPrimaryKeyType(iReturnType);
					iEntityInfo.addPortExemptMethod(iMethodKey);
				}
			}
			return true;
		}
	}
	
	private class GetterMethodVisitor extends ASTVisitor {
		private String iMethodKey;
		private String iMethodName;
		private FieldInfo iFieldInfo;
		private String iFieldVariableName;
		
		public GetterMethodVisitor(String methodKey, String methodName) {
			iMethodKey = methodKey;
			iMethodName = methodName;
		}
		
		public boolean visit(ReturnStatement returnStatement) {
			//public java.lang.String getXMLDefinition() {
			//	return iClobValue.getValue();
			//}
			Expression expression = returnStatement.getExpression();
			if (expression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
				expression = ((ParenthesizedExpression) expression).getExpression();
			}
			if (expression.getNodeType() == ASTNode.INFIX_EXPRESSION) {
				InfixExpression infixExpression = (InfixExpression) expression;
				checkExpression(infixExpression.getLeftOperand());
				checkExpression(infixExpression.getRightOperand());
			}
			else {
				checkExpression(expression);
			}
			return false;
		}
		
		public boolean visit(VariableDeclarationStatement variableDeclarationStatement) {
			@SuppressWarnings("unchecked")
			List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();
			for (VariableDeclarationFragment fragment : fragments) {
				if (fragment.getInitializer() != null) {
					String fieldName = null;
					Expression expression = fragment.getInitializer();
					if (expression.getNodeType() == ASTNode.METHOD_INVOCATION) {
						MethodInvocation methodInvocation = (MethodInvocation) expression;
						if (methodInvocation.getExpression() != null) {
							expression = methodInvocation.getExpression();
						}
					}
					if (expression.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
						ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) expression;
						if (classInstanceCreation.arguments().size() == 1) {
							@SuppressWarnings("unchecked")
							List<Expression> arguments = classInstanceCreation.arguments();
							expression = arguments.get(0);
						}
					}
					switch (expression.getNodeType()) {
						case ASTNode.SIMPLE_NAME: {
							SimpleName simpleName = (SimpleName) expression;
							fieldName = simpleName.getIdentifier();
							break;
						}
						case ASTNode.FIELD_ACCESS: {
							FieldAccess fieldAccess = (FieldAccess) expression;
							if (fieldAccess.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
								fieldName = fieldAccess.getName().getIdentifier();
							}
							break;
						}
						case ASTNode.SUPER_FIELD_ACCESS: {
							SuperFieldAccess superFieldAccess = (SuperFieldAccess) expression;
							fieldName = superFieldAccess.getName().getIdentifier();
							break;
						}
					}
					if (fieldName != null) {
						FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
						if (fieldInfo != null) {
							iFieldInfo = fieldInfo;
							iFieldVariableName = fragment.getName().getIdentifier();
						}
					}
				}
			}
			return true;
		}
		
		private void checkExpression(Expression expression) {
			String fieldName = null;
			if (expression.getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation methodInvocation = (MethodInvocation) expression;
				if (methodInvocation.getExpression() != null) {
					expression = methodInvocation.getExpression();
				}
			}
			switch (expression.getNodeType()) {
				case ASTNode.SIMPLE_NAME: {
					SimpleName simpleName = (SimpleName) expression;
					fieldName = simpleName.getIdentifier();
					break;
				}
				case ASTNode.FIELD_ACCESS: {
					FieldAccess fieldAccess = (FieldAccess) expression;
					if (fieldAccess.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
						fieldName = fieldAccess.getName().getIdentifier();
					}
					break;
				}
				case ASTNode.SUPER_FIELD_ACCESS: {
					SuperFieldAccess superFieldAccess = (SuperFieldAccess) expression;
					fieldName = superFieldAccess.getName().getIdentifier();
					break;
				}
			}
			if (fieldName != null) {
				if (fieldName.equals(iFieldVariableName)) {
					fieldName = iFieldInfo.getFieldName();
				}
				CopyHelperProperty copyHelperProperty = iEjbGetterNameToCopyHelperPropertyMap.get(iMethodName);
				
				CopyHelperProperty fieldCopyHelperProperty = null; 
				if(iAccessBeanInfo != null) {
					fieldCopyHelperProperty = iAccessBeanInfo.getCopyHelperProperty(fieldName, false);
				}
				if (fieldCopyHelperProperty == null && iAccessBeanInfo != null) {
					fieldCopyHelperProperty = iAccessBeanInfo.getCopyHelperProperty(Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1), false);
				}
				if (copyHelperProperty == null || fieldCopyHelperProperty == null || fieldCopyHelperProperty == copyHelperProperty) {
					FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
					if (fieldInfo == null && iEntityInfo.getClobAttributeFieldInfo(fieldName) != null) {
						fieldInfo = iEntityInfo.getClobAttributeFieldInfo(fieldName);
					}
					if (fieldInfo != null) {
						fieldInfo.setGetterName(iMethodName);
						iEntityInfo.addPortExemptMethod(iMethodKey);
						iEjbGetterMethods.add(iMethodKey);
						if (copyHelperProperty != null && copyHelperProperty.getFieldInfo() == null) {
							copyHelperProperty.setFieldInfo(fieldInfo);
							fieldInfo.setCopyHelperProperty(copyHelperProperty);
						}
					}
					else {
						iClobAttributeToGetterMap.put(fieldName, iMethodName);
					}
				}
			}
		}
	}
	
	private class SetterMethodVisitor extends ASTVisitor {
		private String iMethodName;
		private FieldInfo iFieldInfo;
		
		public SetterMethodVisitor(String methodName) {
			iMethodName = methodName;
		}
		
		public FieldInfo getFieldInfo() {
			return iFieldInfo;
		}
		
		public boolean visit(Assignment assignment) {
			//public void setXMLDefinition(java.lang.String aXMLDefinition) {
			//	xmlDefinition = iClobValue.setValue(aXMLDefinition);
			//}
			String fieldName = null;
			Expression rightHandSide = assignment.getRightHandSide();
			Expression leftHandSide = assignment.getLeftHandSide();
			if (rightHandSide.getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation methodInvocation = (MethodInvocation) rightHandSide;
				String methodName = methodInvocation.getName().getIdentifier();
				if (methodInvocation.getExpression() != null) {
					if (TRIM.equals(methodName)) {
						rightHandSide = methodInvocation.getExpression();
					}
					else if (SET_VALUE.equals(methodName) && methodInvocation.arguments().size() == 1) {
						rightHandSide = (Expression) methodInvocation.arguments().get(0);
					}
				}
			}
			else if (rightHandSide.getNodeType() == ASTNode.CONDITIONAL_EXPRESSION) {
				ConditionalExpression conditionalExpression = (ConditionalExpression) rightHandSide;
				rightHandSide = conditionalExpression.getExpression();
			}
			else if (rightHandSide.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
				ClassInstanceCreation classinstanceCreation = (ClassInstanceCreation) rightHandSide;
				@SuppressWarnings("unchecked")
				List<Expression> arguments = classinstanceCreation.arguments();
				if (arguments.size() == 1) {
					rightHandSide = arguments.get(0);
				}
			}
			if (rightHandSide.getNodeType() == ASTNode.SIMPLE_NAME) {
				SimpleName simpleName = (SimpleName) rightHandSide;
				if (!simpleName.isDeclaration()) {
					IBinding binding = simpleName.resolveBinding();
					if (binding instanceof IVariableBinding && ((IVariableBinding) binding).isParameter()) {
						switch (leftHandSide.getNodeType()) {
							case ASTNode.SIMPLE_NAME: {
								simpleName = (SimpleName) leftHandSide;
								fieldName = simpleName.getIdentifier();
								break;
							}
							case ASTNode.FIELD_ACCESS: {
								FieldAccess fieldAccess = (FieldAccess) leftHandSide;
								if (fieldAccess.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
									fieldName = fieldAccess.getName().getIdentifier();
								}
								break;
							}
							case ASTNode.SUPER_FIELD_ACCESS: {
								SuperFieldAccess superFieldAccess = (SuperFieldAccess) leftHandSide;
								fieldName = superFieldAccess.getName().getIdentifier();
								break;
							}
						}
					}
				}
			}
			if (fieldName != null) {
				FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
				if (fieldInfo == null && iEntityInfo.getClobAttributeFieldInfo(fieldName) != null) {
					fieldInfo = iEntityInfo.getClobAttributeFieldInfo(fieldName);
				}
				if (fieldInfo != null) {
					iFieldInfo = fieldInfo;
				}
				else {
					iClobAttributeToSetterMap.put(fieldName, iMethodName);
				}
			}
			return false;
		}
	}
	
	private class MethodRequirementsVisitor extends ASTVisitor {
		private String iMethodKey;
		
		public MethodRequirementsVisitor(String methodKey) {
			iMethodKey = methodKey;
		}

		public boolean visit(MethodInvocation methodInvocation) {
			if (isThisClass(methodInvocation.getExpression())) {
				IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
				String requiredMethodKey = JavaUtil.getMethodKey(methodBinding);
				iEntityInfo.addRequiredMethod(iMethodKey, requiredMethodKey);
			}
			return true;
		}
		
		public boolean visit(SuperMethodInvocation superMethodInvocation) {
			IMethodBinding methodBinding = superMethodInvocation.resolveMethodBinding();
			String requiredMethodKey = JavaUtil.getMethodKey(methodBinding);
			iEntityInfo.addRequiredMethod(iMethodKey, requiredMethodKey);
			return true;
		}
		
		public boolean visit(SimpleName simpleName) {
			if (!simpleName.isDeclaration()) {
				IBinding binding = simpleName.resolveBinding();
				if (binding instanceof IVariableBinding) {
					IVariableBinding variableBinding = (IVariableBinding) binding;
					if (variableBinding.getDeclaringClass() != null) {
						iEntityInfo.addMethodRequiredField(iMethodKey, simpleName.getIdentifier());
					}
				}
			}
			return true;
		}
		
		public boolean visit(QualifiedName qualifiedName) {
			return false;
		}
		
		public boolean visit(FieldAccess fieldAccess) {
			if (fieldAccess.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
				iEntityInfo.addMethodRequiredField(iMethodKey, fieldAccess.getName().getIdentifier());
			}
			return false;
		}
		
		public boolean visit(SuperFieldAccess superFieldAccess) {
			if (superFieldAccess.getQualifier() == null) {
				iEntityInfo.addMethodRequiredField(iMethodKey, superFieldAccess.getName().getIdentifier());
			}
			return false;
		}
		
		private boolean isThisClass(Expression expression) {
			boolean thisClass = true;
			if (expression != null && expression.getNodeType() != ASTNode.THIS_EXPRESSION){
				thisClass = false;
				if (expression.getNodeType() == ASTNode.SIMPLE_NAME) {
					SimpleName simpleName = (SimpleName) expression;
					IBinding binding = simpleName.resolveBinding();
					if (binding != null) {
						if (binding.getKind() == ITypeBinding.TYPE) {
							ITypeBinding typeBinding = (ITypeBinding) binding;
							String qualifiedTypeName = typeBinding.getQualifiedName();
							thisClass = qualifiedTypeName.equals(iEntityInfo.getEjbType().getFullyQualifiedName('.')) ||
									qualifiedTypeName.equals(iEntityInfo.getEjbBaseType() == null ? null : iEntityInfo.getEjbBaseType().getFullyQualifiedName('.'));
						}
					}
				}
			}
			return thisClass;
		}
	}
	
	private class FieldRequirementsVisitor extends ASTVisitor {
		private String iFieldName;
		
		public FieldRequirementsVisitor(String fieldName) {
			iFieldName = fieldName;
		}

		public boolean visit(SimpleName simpleName) {
			if (!simpleName.isDeclaration()) {
				IBinding binding = simpleName.resolveBinding();
				if (binding instanceof IVariableBinding) {
					IVariableBinding variableBinding = (IVariableBinding) binding;
					if (variableBinding.getDeclaringClass() != null) {
						iEntityInfo.addFieldRequiredField(iFieldName, simpleName.getIdentifier());
					}
				}
			}
			return true;
		}
		
		public boolean visit(QualifiedName qualifiedName) {
			return false;
		}
		
		public boolean visit(FieldAccess fieldAccess) {
			if (fieldAccess.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
				iEntityInfo.addFieldRequiredField(iFieldName, fieldAccess.getName().getIdentifier());
			}
			return false;
		}
		
		public boolean visit(SuperFieldAccess superFieldAccess) {
			if (superFieldAccess.getQualifier() == null) {
				iEntityInfo.addFieldRequiredField(iFieldName, superFieldAccess.getName().getIdentifier());
			}
			return false;
		}
	}
	
	private class AccessBeanExpressionVisitor extends ASTVisitor {
		public AccessBeanExpressionVisitor() {
		}
		
		public boolean visit(MethodInvocation methodInvocation) {
			if (methodInvocation.getExpression() == null || methodInvocation.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION) {
				IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
				if (methodBinding != null) {
					String methodKey = JavaUtil.getMethodKey(methodBinding);
					if (GENERATE_KEY_AS_INT.equals(methodKey)) {
						iEntityInfo.setGeneratePrimaryKeyMethodKey(methodKey);
						iEntityInfo.setGeneratedPrimaryKeyType(INTEGER_TYPE);
					}
					else if (GENERATE_KEY_AS_LONG.equals(methodKey)) {
						iEntityInfo.setGeneratePrimaryKeyMethodKey(methodKey);
						iEntityInfo.setGeneratedPrimaryKeyType(LONG_TYPE);
					}
					if (!methodKey.equals(iEntityInfo.getGeneratePrimaryKeyMethodKey()) && iEntityInfo.getFieldInfoByGetterName(methodKey) == null && !methodKey.equals(GET_CLASS) && !iProcessedAccessBeanMethods.contains(methodKey)) {
						iReferencedAccessBeanMethods.add(methodKey);
						iExpressionAccessBeanMethods.add(methodKey);
					}
				}
			}
			return true;
		}
	}
	
	private class EjbStoreVisitor extends ASTVisitor {
		public boolean visit(Assignment assignment) {
			//xmlDefinition = iClobValue.store();
			//public java.lang.String getXMLDefinition() {
			//	return iClobValue.getValue();
			//}
			//public void setXMLDefinition(java.lang.String aXMLDefinition) {
			//	xmlDefinition = iClobValue.setValue(aXMLDefinition);
			//}
			FieldInfo fieldInfo = null;
			String clobAttributeFieldName = null;
			Expression rightHandSide = assignment.getRightHandSide();
			if (rightHandSide.getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation methodInvocation = (MethodInvocation) rightHandSide;
				if (methodInvocation.getExpression() != null && methodInvocation.getName().getIdentifier().equals(STORE)) {
					rightHandSide = methodInvocation.getExpression();
					if (rightHandSide.getNodeType() == ASTNode.SIMPLE_NAME) {
						SimpleName simpleName = (SimpleName) rightHandSide;
						IBinding binding = simpleName.resolveBinding();
						if (binding instanceof IVariableBinding && ((IVariableBinding) binding).isField()) {
							clobAttributeFieldName = simpleName.getIdentifier();
							Expression leftHandSide = assignment.getLeftHandSide();
							switch (leftHandSide.getNodeType()) {
								case ASTNode.SIMPLE_NAME: {
									simpleName = (SimpleName) leftHandSide;
									fieldInfo = iEntityInfo.getFieldInfoByName(simpleName.getIdentifier());
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
						}
					}
				}
			}
			if (fieldInfo != null && clobAttributeFieldName != null) {
				iEntityInfo.setClobAttributeFieldInfo(clobAttributeFieldName, fieldInfo);
				if (iClobAttributeToGetterMap.get(clobAttributeFieldName) != null) {
					String getterMethodName = iClobAttributeToGetterMap.get(clobAttributeFieldName);
					fieldInfo.setGetterName(getterMethodName);
					iEntityInfo.addPortExemptMethod(getterMethodName);
				}
				if (iClobAttributeToSetterMap.get(clobAttributeFieldName) != null) {
					String setterMethodName = iClobAttributeToSetterMap.get(clobAttributeFieldName);
					fieldInfo.setSetterName(setterMethodName);
					iEntityInfo.addPortExemptMethod(setterMethodName + "+" + fieldInfo.getTypeName());
				}
			}
			return false;
		}
	}
	
	private class CopyFromEJBVisitor extends ASTVisitor {
		//h.put("quantity", getQuantity());
		public boolean visit(MethodInvocation methodInvocation) {
			if (methodInvocation.getExpression() != null && PUT.equals(methodInvocation.getName().getIdentifier()) && methodInvocation.arguments().size() == 2) {
				ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
				if (typeBinding != null) {
					String typeName = typeBinding.getQualifiedName();
					if (ACCESS_BEAN_HASHTABLE.equals(typeName) || HASH_TABLE.equals(typeName)) {
						Expression firstArgument = (Expression) methodInvocation.arguments().get(0);
						Expression secondArgument = (Expression) methodInvocation.arguments().get(1);
						String copyHelperPropertyName = null;
						String getterMethodName = null;
						String propertyType = null;
						if (firstArgument.getNodeType() == ASTNode.STRING_LITERAL) {
							copyHelperPropertyName = ((StringLiteral) firstArgument).getLiteralValue();
						}
						if (secondArgument.getNodeType() == ASTNode.METHOD_INVOCATION) {
							MethodInvocation getterMethodInvocation = (MethodInvocation) secondArgument;
							if (getterMethodInvocation.getExpression() == null || getterMethodInvocation.getNodeType() == ASTNode.THIS_EXPRESSION) {
								getterMethodName = getterMethodInvocation.getName().getIdentifier();
								ITypeBinding methodInvocationTypeBinding = getterMethodInvocation.resolveTypeBinding();
								if (methodInvocationTypeBinding != null) {
									propertyType = methodInvocationTypeBinding.getQualifiedName();
								}
							}
						}
						else if (secondArgument.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
							ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) secondArgument;
							@SuppressWarnings("unchecked")
							List<Expression> arguments = classInstanceCreation.arguments();
							Expression argument = arguments.get(0);
							if (argument.getNodeType() == ASTNode.METHOD_INVOCATION) {
								MethodInvocation getterMethodInvocation = (MethodInvocation) argument;
								if (getterMethodInvocation.getExpression() == null || getterMethodInvocation.getNodeType() == ASTNode.THIS_EXPRESSION) {
									getterMethodName = getterMethodInvocation.getName().getIdentifier();
									ITypeBinding classInstanceCreationTypeBinding = classInstanceCreation.resolveTypeBinding();
									if (classInstanceCreationTypeBinding != null) {
										propertyType = classInstanceCreationTypeBinding.getQualifiedName();
									}
								}
							}
						}
						if (copyHelperPropertyName != null && getterMethodName != null) {
							CopyHelperProperty copyHelperProperty = iAccessBeanInfo.getCopyHelperProperty(copyHelperPropertyName, true);
							iEjbGetterNameToCopyHelperPropertyMap.put(getterMethodName, copyHelperProperty);
							if (copyHelperProperty.getType() == null) {
								copyHelperProperty.setType(propertyType);
							}
						}
					}
				}
			}
			return false;
		}
	}
	
	private class CopyToEJBVisitor extends ASTVisitor {
		//if (h.containsKey("quantity"))
		//setQuantity((localQuantity));
		public boolean visit(IfStatement ifStatement) {
			Expression expression = ifStatement.getExpression();
			if (expression.getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation methodInvocation = (MethodInvocation) expression;
				if (methodInvocation.getExpression() != null && CONTAINS_KEY.equals(methodInvocation.getName().getIdentifier())) {
					Expression argument = (Expression) methodInvocation.arguments().get(0);
					ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
					if (typeBinding != null && argument.getNodeType() == ASTNode.STRING_LITERAL) {
						String copyHelperPropertyName = ((StringLiteral) argument).getLiteralValue();
						String typeName = typeBinding.getQualifiedName();
						if (HASH_TABLE.equals(typeName)) {
							Statement thenStatement = ifStatement.getThenStatement();
							if (thenStatement.getNodeType() == ASTNode.BLOCK) {
								Block block = (Block) thenStatement;
								if (block.statements().size() == 1) {
									thenStatement = (Statement) block.statements().get(0);
								}
							}
							if (thenStatement.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
								ExpressionStatement expressionStatement = (ExpressionStatement) thenStatement;
								if (expressionStatement.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION) {
									CopyHelperProperty copyHelperProperty = iAccessBeanInfo.getCopyHelperProperty(copyHelperPropertyName, true);
									iEjbSetterNameToCopyHelperPropertyMap.put(((MethodInvocation) expressionStatement.getExpression()).getName().getIdentifier(), copyHelperProperty);
								}
							}
						}
					}
				}
			}
			return false;
		}
	}
}
