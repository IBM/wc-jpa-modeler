package com.ibm.commerce.jpa.port.util;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.ibm.commerce.jpa.port.generators.PortVisitor;
import com.ibm.commerce.jpa.port.info.AccessBeanSubclassInfo;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.FinderInfo;
import com.ibm.commerce.jpa.port.info.TargetExceptionInfo;
import com.ibm.commerce.jpa.port.info.UserMethodInfo;

public class TargetExceptionUtil {
	public static final String PERSISTENCE_EXCEPTION = "javax.persistence.PersistenceException";
	
	private static final String PROP_HANDLED_TARGET_EXCEPTIONS = "handledTargetExceptions";
	private static final String PROP_HANDLED_SOURCE_EXCEPTIONS = "handledSourceExceptions";
	private static final String PROP_PROMOTED_VARIABLE_DECLARATIONS = "promotedVariableDeclarations";
	private static final String PROP_TARGET_EXCEPTION = "targetException";
	private static final String PROP_SESSION_BEAN = "sessionBean";
	private static final String PROP_DISCARDED_CATCH_CLAUSE = "discardedCatchClause";
//	private static final String JAVA_LANG = "java.lang.";
	private static final String EJB_OBJECT = "javax.ejb.EJBObject";
	private static final String EJB_LOCAL_OBJECT = "javax.ejb.EJBLocalObject";
	private static final String ABSTRACT_ACCESS_BEAN = "com.ibm.ivj.ejb.runtime.AbstractAccessBean";
	private static final String ABSTRACT_ENTITY_ACCESS_BEAN = "com.ibm.ivj.ejb.runtime.AbstractEntityAccessBean";
	private static final String EC_ENTITY_BEAN = "com.ibm.commerce.base.objects.ECEntityBean";
	private static final String REFRESH_ONCE_ACCESS_BEAN_HELPER = "com.ibm.commerce.datatype.RefreshOnceAccessBeanHelper";
	private static final String PRE_REFRESH_COPY_HELPER = "preRefreshCopyHelper";
//	private static final String NAMING_CONTEXT = "javax.naming.Context";
//	private static final String PORTABLE_REMOTE_OBJECT = "javax.rmi.PortableRemoteObject";
	private static final String FIND = "find";
	private static final String FIND_BY_PRIMARY_KEY = "findByPrimaryKey";
	private static final String USING_JDBC = "UsingJDBC";
	private static final String SET = "set";
	private static final String GET = "get";
	private static final String GET_KEY = "__getKey";
	private static final String SET_INIT_KEY = "setInitKey_";
	private static final String IN_EJB_TYPE = "InEJBType";
	private static final String REFRESH_COPY_HELPER = "refreshCopyHelper";
	private static final String COMMIT_COPY_HELPER = "commitCopyHelper";
	private static final String GET_EJB_REF = "getEJBRef";
	private static final String REMOVE = "remove";
	private static final String GET_FALLBACK_DESCRIPTION = "getFallbackDescription";
	private static final String FULFILLS = "fulfills";
	private static final String FULFILLS_KEY = "fulfills+java.lang.Long+java.lang.String";
	private static final String GET_OWNER = "getOwner";
	private static final String GET_GROUPING_ATTRIBUTE_VALUE = "getGroupingAttributeValue";
	private static final String GET_GROUPING_ATTRIBUTE_VALUE_KEY = "getGroupingAttributeValue+java.lang.String+com.ibm.commerce.grouping.GroupingContext";
	private static final String REMOTE_EXCEPTION = "java.rmi.RemoteException";
	private static final String REMOTE_EXCEPTION_SIMPLE_NAME = "RemoteException";
	private static final String EJB_EXCEPTION = "javax.ejb.EJBException";
	private static final String EJB_EXCEPTION_SIMPLE_NAME = "EJBException";
	private static final String FINDER_EXCEPTION = "javax.ejb.FinderException";
	private static final String FINDER_EXCEPTION_SIMPLE_NAME = "FinderException";
	private static final String OBJECT_NOT_FOUND_EXCEPTION = "javax.ejb.ObjectNotFoundException";
	private static final String OBJECT_NOT_FOUND_EXCEPTION_SIMPLE_NAME = "ObjectNotFoundException";
	private static final String CREATE_EXCEPTION = "javax.ejb.CreateException";
	private static final String CREATE_EXCEPTION_SIMPLE_NAME = "CreateException";
	private static final String NAMING_EXCEPTION = "javax.naming.NamingException";
	private static final String DUPLICATE_KEY_EXCEPTION = "javax.ejb.DuplicateKeyException";
	private static final String DUPLICATE_KEY_EXCEPTION_SIMPLE_NAME = "DuplicateKeyException";
	private static final String THROWABLE = "java.lang.Throwable";
	private static final String EXCEPTION = "java.lang.Exception";
	private static final String RUNTIME_EXCEPTION = "java.lang.RuntimeException";
	private static final String ILLEGAL_ARGUMENT_EXCEPTION = "java.lang.IllegalArgumentException";
	private static final String NUMBER_FORMAT_EXCEPTION = "java.lang.NumberFormatException";
	private static final String SECURITY_EXCEPTION = "java.lang.SecurityException";
	private static final String ENTITY_EXISTS_EXCEPTION = "javax.persistence.EntityExistsException";
	private static final String ENTITY_NOT_FOUND_EXCEPTION = "javax.persistence.EntityNotFoundException";
	private static final String DOM_EXCEPTION = "org.w3c.dom.DOMException";
	private static final String TERM_COND_CREATE_EXCEPTION = "com.ibm.commerce.contract.helper.TermCondCreateException";
	private static final String NO_RESULT_EXCEPTION = "javax.persistence.NoResultException";
	private static final String IO_EXCEPTION = "java.io.IOException";
	private static final String SAX_EXCEPTION = "org.xml.sax.SAXException";
	private static final String SQL_EXCEPTION = "java.sql.SQLException";
	private static final String CLASS_CAST_EXCEPTION = "java.lang.ClassCastException";
	private static final String PARSE_CONFIGURATION_EXCEPTION = "javax.xml.parsers.ParserConfigurationException";
	private static final Map<String, String> EXCEPTION_SUPERTYPE_MAP;
	static {
		EXCEPTION_SUPERTYPE_MAP = new ConcurrentHashMap<String, String>();
		EXCEPTION_SUPERTYPE_MAP.put(ENTITY_NOT_FOUND_EXCEPTION, PERSISTENCE_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(ENTITY_EXISTS_EXCEPTION, PERSISTENCE_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(NO_RESULT_EXCEPTION, PERSISTENCE_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(PERSISTENCE_EXCEPTION, RUNTIME_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(EJB_EXCEPTION, RUNTIME_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(ILLEGAL_ARGUMENT_EXCEPTION, RUNTIME_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(RUNTIME_EXCEPTION, EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(DUPLICATE_KEY_EXCEPTION, CREATE_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(CREATE_EXCEPTION, EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(OBJECT_NOT_FOUND_EXCEPTION, FINDER_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(FINDER_EXCEPTION, EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(EXCEPTION, THROWABLE);
		EXCEPTION_SUPERTYPE_MAP.put(NUMBER_FORMAT_EXCEPTION, ILLEGAL_ARGUMENT_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(SECURITY_EXCEPTION, RUNTIME_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(TERM_COND_CREATE_EXCEPTION, CREATE_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(DOM_EXCEPTION, RUNTIME_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(IO_EXCEPTION, EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(SAX_EXCEPTION, EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(SQL_EXCEPTION, EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(CLASS_CAST_EXCEPTION, RUNTIME_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(NAMING_EXCEPTION, EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(REMOTE_EXCEPTION, IO_EXCEPTION);
		EXCEPTION_SUPERTYPE_MAP.put(PARSE_CONFIGURATION_EXCEPTION, EXCEPTION);
	}
	private static final Map<String, String> SIMPLE_NAME_EXCEPTION_MAP;
	static {
		SIMPLE_NAME_EXCEPTION_MAP = new HashMap<String, String>();
		SIMPLE_NAME_EXCEPTION_MAP.put(OBJECT_NOT_FOUND_EXCEPTION_SIMPLE_NAME, OBJECT_NOT_FOUND_EXCEPTION);
		SIMPLE_NAME_EXCEPTION_MAP.put(DUPLICATE_KEY_EXCEPTION_SIMPLE_NAME, DUPLICATE_KEY_EXCEPTION);
		SIMPLE_NAME_EXCEPTION_MAP.put(EJB_EXCEPTION_SIMPLE_NAME, EJB_EXCEPTION);
		SIMPLE_NAME_EXCEPTION_MAP.put(CREATE_EXCEPTION_SIMPLE_NAME, CREATE_EXCEPTION);
		SIMPLE_NAME_EXCEPTION_MAP.put(FINDER_EXCEPTION_SIMPLE_NAME, FINDER_EXCEPTION);
		SIMPLE_NAME_EXCEPTION_MAP.put(REMOTE_EXCEPTION_SIMPLE_NAME, REMOTE_EXCEPTION);
	}
	private static final Map<String, Map<String, TargetExceptionInfo>> UNHANDLED_TARGET_EXCEPTIONS_MAP;
	static {
		UNHANDLED_TARGET_EXCEPTIONS_MAP = new HashMap<String, Map<String, TargetExceptionInfo>>();
		TargetExceptionInfo unhandledTargetExceptions = new TargetExceptionInfo();
		unhandledTargetExceptions.addTargetException(ENTITY_NOT_FOUND_EXCEPTION);
		unhandledTargetExceptions.addSourceException(OBJECT_NOT_FOUND_EXCEPTION);
		Map<String, TargetExceptionInfo> unhandledTargetExceptionsMethodMap = new HashMap<String, TargetExceptionInfo>();
		unhandledTargetExceptionsMethodMap.put("getFile+java.lang.String", unhandledTargetExceptions);
		unhandledTargetExceptionsMethodMap.put("setFile+byte[]+java.lang.String", new TargetExceptionInfo());
		UNHANDLED_TARGET_EXCEPTIONS_MAP.put("ManagedFile", unhandledTargetExceptionsMethodMap);
//		unhandledTargetExceptions = new TargetExceptionInfo();
//		unhandledTargetExceptions.addTargetException(NAMING_EXCEPTION);
//		unhandledTargetExceptions.addSourceException(NAMING_EXCEPTION);
//		unhandledTargetExceptions.addSourceException(FINDER_EXCEPTION);
//		unhandledTargetExceptionsMethodMap = new HashMap<String, TargetExceptionInfo>();
//		unhandledTargetExceptionsMethodMap.put("getMasterCatalogStatic+java.lang.Integer", unhandledTargetExceptions);
//		UNHANDLED_TARGET_EXCEPTIONS_MAP.put("Store", unhandledTargetExceptionsMethodMap);
	}
	
	public static Collection<String> getFilteredExceptions(IJavaProject javaProject, Collection<String> exceptions) {
		Collection<String> filteredExceptions = new HashSet<String>();
		for (String exception : exceptions) {
			boolean add = false;
			if (!isRuntimeException(javaProject, exception)) {
				add = true;
				for (String filteredException : filteredExceptions) {
					if (catchHandlesException(javaProject, exception, filteredException)) {
						filteredExceptions.remove(filteredException);
						filteredExceptions.add(exception);
						add = false;
						break;
					}
					else if (catchHandlesException(javaProject, filteredException, exception)) {
						add = false;
						break;
					}
				}
			}
			if (add) {
				filteredExceptions.add(exception);
			}
		}
		return filteredExceptions;
	}
	
//	public static Collection<String> getFilteredUnhandledExceptions(IJavaProject javaProject, Collection<String> unhandledExceptions, Collection<String> declaredExceptions) {
//		Collection<String> filteredUnhandledExceptions = new HashSet<String>();
//		for (String exception : unhandledExceptions) {
//			boolean add = false;
//			if (!isRuntimeException(javaProject, exception) && !exceptionMatchesDeclaredExceptions(javaProject, exception, declaredExceptions)) {
//				add = true;
//				for (String filteredUnhandledException : filteredUnhandledExceptions) {
//					if (catchHandlesException(javaProject, exception, filteredUnhandledException)) {
//						filteredUnhandledExceptions.remove(filteredUnhandledException);
//						filteredUnhandledExceptions.add(exception);
//						add = false;
//						break;
//					}
//					else if (catchHandlesException(javaProject, filteredUnhandledException, exception)) {
//						add = false;
//						break;
//					}
//				}
//			}
//			if (add) {
//				filteredUnhandledExceptions.add(exception);
//			}
//		}
//		return filteredUnhandledExceptions;
//	}
	
//	private static boolean exceptionMatchesDeclaredExceptions(IJavaProject javaProject, String exception, Collection<String> declaredExceptions) {
//		boolean match = false;
//		for (String declaredException : declaredExceptions) {
//			if (catchHandlesException(javaProject, declaredException, exception)) {
//				match = true;
//				break;
//			}
//		}
//		return match;
//	}
	
	public static TargetExceptionInfo getUnhandledTargetExceptions(ApplicationInfo applicationInfo, IJavaProject javaProject, ASTNode astNode) {
		ExceptionVisitor exceptionVisitor = new ExceptionVisitor(applicationInfo, javaProject);
		if (astNode != null) {
			astNode.accept(exceptionVisitor);
		}
		return exceptionVisitor.getUnhandledTargetExceptions();
	}
	
	public static TargetExceptionInfo getEntityReferencingTypeUnhandledTargetExceptions(ApplicationInfo applicationInfo, IJavaProject javaProject, ASTNode astNode) {
		ExceptionVisitor exceptionVisitor = new ExceptionVisitor(applicationInfo, javaProject, true);
		if (astNode != null) {
			astNode.accept(exceptionVisitor);
		}
		return exceptionVisitor.getUnhandledTargetExceptions();
	}
	
	public static TargetExceptionInfo getEjbMethodUnhandledTargetExceptions(EntityInfo entityInfo, String methodKey) {
		TargetExceptionInfo unhandledTargetExceptions = entityInfo.getEjbMethodUnhandledTargetExceptions(methodKey);
		if (unhandledTargetExceptions == null) {
			if (UNHANDLED_TARGET_EXCEPTIONS_MAP.get(entityInfo.getEjbName()) != null && UNHANDLED_TARGET_EXCEPTIONS_MAP.get(entityInfo.getEjbName()).containsKey(methodKey)) {
				unhandledTargetExceptions = UNHANDLED_TARGET_EXCEPTIONS_MAP.get(entityInfo.getEjbName()).get(methodKey);
			}
			else if (entityInfo.getSupertype() != null && entityInfo.getSupertype().getEjbMethodDeclarations(methodKey) != null) {
				unhandledTargetExceptions = getEjbMethodUnhandledTargetExceptions(entityInfo.getSupertype(), methodKey);
			}
			else if (entityInfo.getSubtypes() != null) {
				unhandledTargetExceptions = new TargetExceptionInfo();
				Set<EntityInfo> subtypes = entityInfo.getSubtypes();
				for (EntityInfo subtype : subtypes) {
					List<MethodDeclaration> methodDeclarations = subtype.getEjbMethodDeclarations(methodKey);
					if (methodDeclarations != null) {
						for (MethodDeclaration methodDeclaration : methodDeclarations) {
							unhandledTargetExceptions.addAll(getUnhandledTargetExceptions(subtype.getModuleInfo().getApplicationInfo(), subtype.getModuleInfo().getJavaProject(), methodDeclaration.getBody()));
						}
					}
					subtype.setEjbMethodUnhandledTargetExceptions(methodKey, unhandledTargetExceptions);
				}
			}
			else {
				unhandledTargetExceptions = new TargetExceptionInfo();
				List<MethodDeclaration> methodDeclarations = entityInfo.getEjbMethodDeclarations(methodKey);
				if (methodDeclarations != null) {
					for (MethodDeclaration methodDeclaration : methodDeclarations) {
						unhandledTargetExceptions.addAll(getUnhandledTargetExceptions(entityInfo.getModuleInfo().getApplicationInfo(), entityInfo.getModuleInfo().getJavaProject(), methodDeclaration.getBody()));
					}
				}
			}
			entityInfo.setEjbMethodUnhandledTargetExceptions(methodKey, unhandledTargetExceptions);
		}
		return unhandledTargetExceptions;
	}
	
	public static TargetExceptionInfo getAccessBeanMethodUnhandledTargetExceptions(EntityInfo entityInfo, String methodKey) {
		TargetExceptionInfo unhandledTargetExceptions = new TargetExceptionInfo();
		String methodName = JavaUtil.getMethodName(methodKey);
		String[] parameterTypes = JavaUtil.getParameterTypes(methodKey);
		UserMethodInfo userMethodInfo = entityInfo.getUserMethodInfo(methodKey);
		if (userMethodInfo != null) {
			unhandledTargetExceptions.addAll(TargetExceptionUtil.getEjbMethodUnhandledTargetExceptions(entityInfo, methodKey));
		}
		else if (JavaUtil.isConstructor(methodKey)) {
			if (parameterTypes.length > 1 || (parameterTypes.length == 1 && !EJB_OBJECT.equals(parameterTypes[0]))) {
				unhandledTargetExceptions.addTargetException(ENTITY_EXISTS_EXCEPTION);
			}
		}
		else if (methodName.startsWith(FIND)) {
			FinderInfo finderInfo = entityInfo.getFinderInfo(methodKey);
			if (finderInfo != null) {
				unhandledTargetExceptions.addAll(finderInfo.getTargetExceptions());
			}
		}
		else if (entityInfo.getProtectable() && (GET_OWNER.equals(methodKey) || FULFILLS_KEY.equals(methodKey))) {
			unhandledTargetExceptions.addTargetException(EXCEPTION);
		}
		else if (entityInfo.getGroupable() && GET_GROUPING_ATTRIBUTE_VALUE_KEY.equals(methodKey)) {
			unhandledTargetExceptions.addTargetException(EXCEPTION);
		}
		else if ((methodName.startsWith(GET) && parameterTypes.length == 0 && entityInfo.getFieldInfoByGetterName(methodName) != null) ||
				methodName.endsWith(IN_EJB_TYPE)) {
			FieldInfo fieldInfo = entityInfo.getFieldInfoByGetterName(methodName);
			if (fieldInfo == null && methodName.endsWith(IN_EJB_TYPE)) {
				fieldInfo = entityInfo.getFieldInfoByGetterName(methodName.substring(0, methodName.lastIndexOf(IN_EJB_TYPE)));
			}
			if (fieldInfo != null && !fieldInfo.getIsKeyField()) {
				unhandledTargetExceptions.addTargetException(ENTITY_NOT_FOUND_EXCEPTION);
			}
		}
		else if (methodName.startsWith(SET) && parameterTypes.length == 1 && entityInfo.getFieldInfoBySetterName(methodName) != null) {
			// setters throw no exceptions
		}
		else if (methodName.startsWith(SET_INIT_KEY)) {
			// set init key throws no exceptions
		}
		else if (REFRESH_COPY_HELPER.equals(methodName)) {
			unhandledTargetExceptions.addTargetException(ENTITY_NOT_FOUND_EXCEPTION);
		}
		else if (COMMIT_COPY_HELPER.equals(methodName)) {
			// no exceptions
		}
		else if (GET_KEY.equals(methodName)) {
			// no exceptions
		}
		else {
			System.out.println("unknown access bean method: "+methodKey);
		}
		return unhandledTargetExceptions;
	}
	
	public static TargetExceptionInfo getAccessBeanSubclassMethodUnhandledTargetExceptions(AccessBeanSubclassInfo accessBeanSubclassInfo, String methodKey) {
		if ("com.ibm.commerce.catalog.beans.CatalogEntryDataBean".equals(accessBeanSubclassInfo.getName()) && methodKey.equals("populate")) {
			System.out.println("com.ibm.commerce.catalog.beans.CatalogEntryDataBean");
		}
		if ("com.ibm.commerce.giftregistry.beans.GiftRegistryCatalogEntryDataBean".equals(accessBeanSubclassInfo.getName()) && methodKey.equals("populate")) {
			System.out.println("com.ibm.commerce.giftregistry.beans.GiftRegistryCatalogEntryDataBean");
		}
		TargetExceptionInfo unhandledTargetExceptions = accessBeanSubclassInfo.getMethodUnhandledTargetExceptions(methodKey);
		if (unhandledTargetExceptions == null && accessBeanSubclassInfo.hasMethod(methodKey)) {
			EntityInfo entityInfo = accessBeanSubclassInfo.getEntityInfo();
			boolean isAccessBeanMethod = entityInfo.isEjbAccessBeanMethod(methodKey);
			AccessBeanSubclassInfo baseAccessBeanSubclassInfo = accessBeanSubclassInfo;
			for (AccessBeanSubclassInfo current = accessBeanSubclassInfo.getSuperclass(); current != null; current = current.getSuperclass()) {
				if (current.hasMethod(methodKey)) {
					baseAccessBeanSubclassInfo = current;
				}
			}
			Set<AccessBeanSubclassInfo> accessBeanSubclasses = new HashSet<AccessBeanSubclassInfo>();
			accessBeanSubclasses.add(baseAccessBeanSubclassInfo);
			while (!accessBeanSubclasses.isEmpty()) {
				Set<AccessBeanSubclassInfo> newAccessBeanSubclasses = new HashSet<AccessBeanSubclassInfo>();
				for (AccessBeanSubclassInfo current : accessBeanSubclasses) {
					if (current.hasMethod(methodKey)) {
						TargetExceptionInfo currentUnhandledTargetExceptions = new TargetExceptionInfo();
						current.setMethodUnhandledTargetExceptions(methodKey, currentUnhandledTargetExceptions);
						MethodDeclaration methodDeclaration = current.getMethodDeclaration(methodKey);
						if (methodDeclaration != null) {
							if (isAccessBeanMethod) {
								currentUnhandledTargetExceptions.addAll(getAccessBeanMethodUnhandledTargetExceptions(entityInfo, methodKey));
							}
							else {
								currentUnhandledTargetExceptions.addAll(getUnhandledTargetExceptions(current.getProjectInfo().getApplicationInfo(), current.getProjectInfo().getJavaProject(), methodDeclaration));
								if (!JavaUtil.isConstructor(methodKey)) {
									for (AccessBeanSubclassInfo base = current.getSuperclass(); base != null; base = base.getSuperclass()) {
										if (base.hasMethod(methodKey)) {
											base.getMethodUnhandledTargetExceptions(methodKey).addAll(currentUnhandledTargetExceptions);
										}
									}
								}
							}
//							current.releaseMethodDeclaration(methodKey);
						}
					}
					newAccessBeanSubclasses.addAll(current.getSubclasses());
				}
				accessBeanSubclasses = newAccessBeanSubclasses;
			}
			// do another pass in case new exceptions were added to the base method and some of the subclasses call the super method
			accessBeanSubclasses.clear();
			accessBeanSubclasses.add(baseAccessBeanSubclassInfo);
			while (!accessBeanSubclasses.isEmpty()) {
				Set<AccessBeanSubclassInfo> newAccessBeanSubclasses = new HashSet<AccessBeanSubclassInfo>();
				for (AccessBeanSubclassInfo current : accessBeanSubclasses) {
					if (current.hasMethod(methodKey)) {
						TargetExceptionInfo currentUnhandledTargetExceptions = current.getMethodUnhandledTargetExceptions(methodKey);
						MethodDeclaration methodDeclaration = current.getMethodDeclaration(methodKey);
						if (methodDeclaration != null) {
							if (!isAccessBeanMethod) {
								currentUnhandledTargetExceptions.addAll(getUnhandledTargetExceptions(current.getProjectInfo().getApplicationInfo(), current.getProjectInfo().getJavaProject(), methodDeclaration));
							}
							current.releaseMethodDeclaration(methodKey);
						}
					}
					newAccessBeanSubclasses.addAll(current.getSubclasses());
				}
				accessBeanSubclasses = newAccessBeanSubclasses;
			}
			unhandledTargetExceptions = accessBeanSubclassInfo.getMethodUnhandledTargetExceptions(methodKey);
		}
		return unhandledTargetExceptions;
	}
	
	public static void portTryStatement(IJavaProject javaProject, TryStatement tryStatement, PortVisitor portVisitor, boolean entityReferencingType) {
		AST ast = tryStatement.getAST();
		boolean dropsThrough = false;
		Block tryStatementBlock = tryStatement.getBody();
		if (isDropThroughStatement(tryStatementBlock)) {
			dropsThrough = true;
		}
		@SuppressWarnings("unchecked")
		List<CatchClause> catchClauses = tryStatement.catchClauses();
		if (catchClauses.size() > 0) {
			for (int i = 0; i < catchClauses.size(); i++) {
				CatchClause catchClause = catchClauses.get(i);
				SingleVariableDeclaration exceptionDeclaration = catchClause.getException();
				ITypeBinding exceptionTypeBinding = exceptionDeclaration.getType().resolveBinding();
//				if (exceptionTypeBinding == null) {
//					System.out.println("null binding? "+exceptionDeclaration);
//				}
				String exceptionType = exceptionTypeBinding != null ? exceptionTypeBinding.getQualifiedName() : exceptionDeclaration.getType().toString();
				@SuppressWarnings("unchecked")
				Collection<String> handledTargetExceptions = (Collection<String>) catchClause.getProperty(PROP_HANDLED_TARGET_EXCEPTIONS);
				@SuppressWarnings("unchecked")
				Collection<String> handledSourceExceptions = (Collection<String>) catchClause.getProperty(PROP_HANDLED_SOURCE_EXCEPTIONS);
				if (handledTargetExceptions == null || handledTargetExceptions.isEmpty()) {
					if (!entityReferencingType || (!isRuntimeException(javaProject, exceptionType) && !((EXCEPTION.equals(exceptionType) || THROWABLE.equals(exceptionType)) && (handledSourceExceptions == null || handledSourceExceptions.isEmpty())))) {
						catchClauses.remove(i);
						i--;
					}
				}
				else if (handledTargetExceptions.size() == 1) {
					for (String handledException : handledTargetExceptions) {
						if (portVisitor.getTypeMapping(handledException) != null) {
							handledException = portVisitor.getTypeMapping(handledException);
						}
						if (!catchHandlesException(javaProject, exceptionType, handledException)) {
							SingleVariableDeclaration newExceptionDeclaration = ast.newSingleVariableDeclaration();
							newExceptionDeclaration.setType(ast.newSimpleType(ast.newName(handledException)));
							newExceptionDeclaration.setName(ast.newSimpleName(exceptionDeclaration.getName().getIdentifier()));
							portVisitor.replaceASTNode(exceptionDeclaration, newExceptionDeclaration);
							catchClause.getBody().accept(new CatchClauseVisitor(handledTargetExceptions));
						}
					}
				}
				else {
					boolean catchUsed = false;
					Collection<String> unhandledExceptions = new HashSet<String>();
					for (String handledException : handledTargetExceptions) {
						if (catchHandlesException(javaProject, exceptionType, handledException)) {
							catchUsed = true;
						}
						else {
							unhandledExceptions.add(handledException);
						}
					}
					if (unhandledExceptions.size() > 0) {
						String targetExceptionType = null;
						String secondTargetExceptionType = null;
						if (FINDER_EXCEPTION.equals(exceptionType) || OBJECT_NOT_FOUND_EXCEPTION.equals(exceptionType)) {
							if (unhandledExceptions.size() == 2 && unhandledExceptions.contains(NO_RESULT_EXCEPTION) && unhandledExceptions.contains(ENTITY_NOT_FOUND_EXCEPTION)) {
								targetExceptionType = NO_RESULT_EXCEPTION;
								secondTargetExceptionType = ENTITY_NOT_FOUND_EXCEPTION;
							}
							else if (unhandledExceptions.size() == 1) {
								for (String unhandledException : unhandledExceptions) {
									targetExceptionType = unhandledException;
								}
							}
						}
						else if (CREATE_EXCEPTION.equals(exceptionType) || DUPLICATE_KEY_EXCEPTION.equals(exceptionType)) {
							targetExceptionType = ENTITY_EXISTS_EXCEPTION;
						}
						else if (REMOTE_EXCEPTION.equals(exceptionType) || EJB_EXCEPTION.equals(exceptionType)) {
							targetExceptionType = PERSISTENCE_EXCEPTION;
						}
						if (targetExceptionType == null) {
							System.out.println("unhandledExceptions but could not determine target exception :");
							for (String unhandledException : unhandledExceptions) {
								System.out.println("\t" + unhandledException);
							}
						}
						else {
							if (!catchUsed) {
								exceptionDeclaration.setType(ast.newSimpleType(ast.newName(targetExceptionType)));
							}
							else {
								CatchClause newCatchClause = ast.newCatchClause(); //(CatchClause) ASTNode.copySubtree(ast, catchClause);
								SingleVariableDeclaration newExceptionDeclaration = ast.newSingleVariableDeclaration(); // (SingleVariableDeclaration) ASTNode.copySubtree(ast, newCatchClause.getException());
								newExceptionDeclaration.setType(ast.newSimpleType(ast.newName(targetExceptionType)));
								newExceptionDeclaration.setName(ast.newSimpleName(exceptionDeclaration.getName().getIdentifier()));
								newCatchClause.setException(newExceptionDeclaration);
								Block newCatchBody = (Block) ASTNode.copySubtree(ast, catchClause.getBody());
								newCatchClause.setBody(newCatchBody);
								i++;
								catchClauses.add(i, newCatchClause);
								newCatchClause.getBody().accept(new CatchClauseVisitor(unhandledExceptions));
							}
							if (secondTargetExceptionType != null) {
								CatchClause newCatchClause = ast.newCatchClause(); //(CatchClause) ASTNode.copySubtree(ast, catchClause);
								SingleVariableDeclaration newExceptionDeclaration = ast.newSingleVariableDeclaration(); // (SingleVariableDeclaration) ASTNode.copySubtree(ast, newCatchClause.getException());
								newExceptionDeclaration.setType(ast.newSimpleType(ast.newName(secondTargetExceptionType)));
								newExceptionDeclaration.setName(ast.newSimpleName(exceptionDeclaration.getName().getIdentifier()));
								newCatchClause.setException(newExceptionDeclaration);
								Block newCatchBody = (Block) ASTNode.copySubtree(ast, catchClause.getBody());
								newCatchClause.setBody(newCatchBody);
								i++;
								catchClauses.add(i, newCatchClause);
								newCatchClause.getBody().accept(new CatchClauseVisitor(unhandledExceptions));
							}
						}
					}
				}
			}
			// sort the catch clauses
			int persistenceExceptionPosition = -1;
			for (int i = 0; i < catchClauses.size(); i++) {
				CatchClause catchClause = catchClauses.get(i);
				SingleVariableDeclaration exceptionDeclaration = catchClause.getException();
				String exceptionType = exceptionDeclaration.getType().toString();
				if (PERSISTENCE_EXCEPTION.equals(exceptionType)) {
					persistenceExceptionPosition = i;
				}
				else if (persistenceExceptionPosition != -1 && (ENTITY_EXISTS_EXCEPTION.equals(exceptionType) || NO_RESULT_EXCEPTION.equals(exceptionType) || ENTITY_NOT_FOUND_EXCEPTION.equals(exceptionType))) {
					catchClause.delete();
					CatchClause persistenceExceptionCatchClause = catchClauses.get(persistenceExceptionPosition);
					persistenceExceptionCatchClause.delete();
					catchClauses.add(persistenceExceptionPosition, catchClause);
					catchClauses.add(i, persistenceExceptionCatchClause);
				}
			}
			
			if (catchClauses.size() == 0 && tryStatement.getFinally() == null) {
				@SuppressWarnings("unchecked")
				List<Statement> statements = tryStatementBlock.statements();
				if (tryStatement.getParent().getNodeType() == ASTNode.BLOCK) {
					for (int i = 0; i < statements.size(); i++) {
						Statement statement = statements.get(i);
						if (statement.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
							VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
							@SuppressWarnings("unchecked")
							List<VariableDeclarationFragment> variableDeclarationFragments = variableDeclarationStatement.fragments();
							for (int j = 0; j < variableDeclarationFragments.size(); j++) {
								VariableDeclarationFragment variableDeclarationFragment = variableDeclarationFragments.get(j);
								String variableName = variableDeclarationFragment.getName().getIdentifier();
								if (isPromotedVariable(tryStatement.getParent(), variableName)) {
									variableDeclarationFragments.remove(j);
									j--;
									if (variableDeclarationFragment.getInitializer() != null) {
										Expression initializer = variableDeclarationFragment.getInitializer();
										initializer.delete();
										Assignment assignment = ast.newAssignment();
										assignment.setLeftHandSide(ast.newName(variableName));
										assignment.setOperator(Assignment.Operator.ASSIGN);
										assignment.setRightHandSide(initializer);
										ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
										statements.add(i, expressionStatement);
										i++;
									}
								}
								else {
									@SuppressWarnings("unchecked")
									Collection<String> promotedVariableDeclarationStatements = (Collection<String>) tryStatement.getParent().getProperty(PROP_PROMOTED_VARIABLE_DECLARATIONS);
									if (promotedVariableDeclarationStatements == null) {
										promotedVariableDeclarationStatements = new HashSet<String>();
										tryStatement.getParent().setProperty(PROP_PROMOTED_VARIABLE_DECLARATIONS, promotedVariableDeclarationStatements);
									}
									promotedVariableDeclarationStatements.add(variableName);
								}
							}
							if (variableDeclarationFragments.size() == 0) {
								statements.remove(i);
								i--;
							}
						}	
					}
				}
				if (tryStatementBlock.getProperty(PROP_PROMOTED_VARIABLE_DECLARATIONS) != null) {
					@SuppressWarnings("unchecked")
					Collection<String> promotedVariableDeclarationStatements = (Collection<String>) tryStatementBlock.getProperty(PROP_PROMOTED_VARIABLE_DECLARATIONS);
					@SuppressWarnings("unchecked")
					Collection<String> parentPromotedVariableDeclarationStatements = (Collection<String>) tryStatement.getParent().getProperty(PROP_PROMOTED_VARIABLE_DECLARATIONS);
					if (parentPromotedVariableDeclarationStatements == null) {
						tryStatement.getParent().setProperty(PROP_PROMOTED_VARIABLE_DECLARATIONS, promotedVariableDeclarationStatements);
					}
					else {
						parentPromotedVariableDeclarationStatements.addAll(promotedVariableDeclarationStatements);
					}
				}
				Statement lastStatement = statements.size() > 0 ? statements.get(statements.size() - 1) : null;
				portVisitor.replaceStatement(tryStatement, statements);
				if (lastStatement != null && isReturnStatement(lastStatement)) {
					if (lastStatement.getParent().getNodeType() == ASTNode.BLOCK) {
						Block parentBlock = (Block) lastStatement.getParent();
						@SuppressWarnings("unchecked")
						List<Statement> parentBlockStatements = parentBlock.statements();
						while (parentBlockStatements.size() > 0 && parentBlockStatements.get(parentBlockStatements.size() - 1) != lastStatement) {
							parentBlockStatements.remove(parentBlockStatements.size() - 1);
						}
					}
				}
			}
			else {
				for (CatchClause catchClause : catchClauses) {
					if (!dropsThrough && isDropThroughStatement(catchClause.getBody())) {
						dropsThrough = true;
					}
				}
				if (!dropsThrough) {
					if (tryStatement.getParent().getNodeType() == ASTNode.BLOCK) {
						Block parentBlock = (Block) tryStatement.getParent();
						@SuppressWarnings("unchecked")
						List<Statement> parentBlockStatements = parentBlock.statements();
						while (parentBlockStatements.size() > 1 && parentBlockStatements.get(parentBlockStatements.size() - 1) != tryStatement) {
							parentBlockStatements.remove(parentBlockStatements.size() - 1);
						}
					}
				}
			}
		}
	}
	
	private static boolean isReturnStatement(Statement statement) {
		boolean result = false;
		switch (statement.getNodeType()) {
			case ASTNode.RETURN_STATEMENT: {
				result = true;
				break;
			}
			case ASTNode.BLOCK: {
				Block block = (Block) statement;
				@SuppressWarnings("unchecked")
				List<Statement> statements = block.statements();
				if (statements.size() > 0) {
					result = isReturnStatement(statements.get(statements.size() - 1));
				}
				break;
			}
			case ASTNode.IF_STATEMENT: {
				IfStatement ifStatement = (IfStatement) statement;
				if (ifStatement.getElseStatement() != null) {
					result = isReturnStatement(ifStatement.getThenStatement()) && isReturnStatement(ifStatement.getElseStatement());
				}
				break;
			}
		}
		return result;
	}
	
	private static boolean isPromotedVariable(ASTNode astNode, String variableName) {
		boolean result = false;
		while (astNode != null && astNode.getNodeType() != ASTNode.METHOD_DECLARATION && astNode.getNodeType() != ASTNode.INITIALIZER) {
			@SuppressWarnings("unchecked")
			Collection<String> promotedVariableDeclarations = (Collection<String>) astNode.getProperty(PROP_PROMOTED_VARIABLE_DECLARATIONS);
			if (promotedVariableDeclarations != null && promotedVariableDeclarations.contains(variableName)) {
				result = true;
				break;
		
			}
			astNode = astNode.getParent();
		}
		return result;
	}
	
	public static boolean portVariableDeclarationStatement(VariableDeclarationStatement variableDeclarationStatement, PortVisitor childVisitor) {
		boolean visitChildren = true;
		AST ast = variableDeclarationStatement.getAST();
		List<Statement> newStatements = null;
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> variableDeclarationFragments = variableDeclarationStatement.fragments();
		for (int j = 0; j < variableDeclarationFragments.size(); j++) {
			VariableDeclarationFragment variableDeclarationFragment = variableDeclarationFragments.get(j);
			String variableName = variableDeclarationFragment.getName().getIdentifier();
			if (isPromotedVariable(variableDeclarationStatement.getParent(), variableName)) {
				variableDeclarationFragments.remove(j);
				j--;
				if (variableDeclarationFragment.getInitializer() != null) {
					Expression initializer = variableDeclarationFragment.getInitializer();
					initializer.accept(childVisitor);
					initializer.delete();
					Assignment assignment = ast.newAssignment();
					assignment.setLeftHandSide(ast.newName(variableName));
					assignment.setOperator(Assignment.Operator.ASSIGN);
					assignment.setRightHandSide(initializer);
					ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
					if (newStatements == null) {
						newStatements = new ArrayList<Statement>();
					}
					newStatements.add(expressionStatement);
				}
			}
		}
		if (newStatements != null) {
			visitChildren = false;
			if (variableDeclarationFragments.size() > 0) {
				variableDeclarationStatement.accept(childVisitor);
				newStatements.add((Statement) ASTNode.copySubtree(ast, variableDeclarationStatement));
			}
			childVisitor.replaceStatement(variableDeclarationStatement, newStatements);
		}
		return visitChildren;
	}
	
	public static void portThrowStatement(ThrowStatement throwStatement, boolean entityReferencingType) {
		Expression throwExpression = throwStatement.getExpression();
		ITypeBinding typeBinding = throwExpression.resolveTypeBinding();
		if (typeBinding != null) {
			String exception = typeBinding.getQualifiedName();
			String targetException = null;
			if (OBJECT_NOT_FOUND_EXCEPTION.equals(exception) || FINDER_EXCEPTION.equals(exception)) {
				targetException = ENTITY_NOT_FOUND_EXCEPTION;
			}
			else if (DUPLICATE_KEY_EXCEPTION.equals(exception) || CREATE_EXCEPTION.equals(exception)) {
				targetException = ENTITY_EXISTS_EXCEPTION;
			}
			else if (EJB_EXCEPTION.equals(exception) || REMOTE_EXCEPTION.equals(exception)) {
				targetException = PERSISTENCE_EXCEPTION;
			}
			if (targetException != null && (!entityReferencingType || targetException.equals(throwStatement.getProperty(PROP_TARGET_EXCEPTION)))) {
				if (throwExpression.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) throwExpression;
					classInstanceCreation.setType(classInstanceCreation.getAST().newSimpleType(classInstanceCreation.getAST().newName(targetException)));
				}
			}
		}
	}
	
	public static boolean catchHandlesException(IJavaProject javaProject, String catchException, String exception) {
		boolean handlesException = false;
		while (exception != null) {
			if (exception.equals(catchException)) {
				handlesException = true;
				break;
			}
			exception = getExceptionSupertype(javaProject, exception);
		}
		return handlesException;
	}
	
	public static boolean catchHandlesTargetException(IJavaProject javaProject, String catchException, String targetException) {
		boolean handlesException = catchHandlesException(javaProject, catchException, targetException);
		if (!handlesException) {
			if ((targetException.equals(NO_RESULT_EXCEPTION) || targetException.equals(ENTITY_NOT_FOUND_EXCEPTION)) &&
				(catchException.equals(FINDER_EXCEPTION) || catchException.equals(OBJECT_NOT_FOUND_EXCEPTION))) {
				handlesException = true;
			}
			else if (targetException.equals(ENTITY_EXISTS_EXCEPTION) &&
					(catchException.equals(CREATE_EXCEPTION) || catchException.equals(DUPLICATE_KEY_EXCEPTION))) {
				handlesException = true;
			}
			else if (targetException.equals(PERSISTENCE_EXCEPTION) &&
					(catchException.equals(REMOTE_EXCEPTION) || catchException.equals(EJB_EXCEPTION))) {
				handlesException = true;
			}
		}
		return handlesException;
	}
	
	public static boolean isRuntimeException(IJavaProject javaProject, String exception) {
		boolean runtimeException = false;
		while (exception != null) {
			if (RUNTIME_EXCEPTION.equals(exception)) {
				runtimeException = true;
				break;
			}
			exception = getExceptionSupertype(javaProject, exception);
		}
		return runtimeException;
	}
	
	public static boolean isDropThroughStatement(Statement statement) {
		boolean dropsThrough = true;
		if (statement != null) {
			switch (statement.getNodeType()) {
				case ASTNode.IF_STATEMENT: {
					IfStatement ifStatement = (IfStatement) statement;
					if (!isDropThroughStatement(ifStatement.getThenStatement()) && !isDropThroughStatement(ifStatement.getElseStatement())) {
						dropsThrough = false;
					}
					break;
				}
				case ASTNode.RETURN_STATEMENT: {
					dropsThrough = false;
					break;
				}
				case ASTNode.THROW_STATEMENT: {
					dropsThrough = false;
					break;
				}
				case ASTNode.BLOCK: {
					Block block = (Block) statement;
					@SuppressWarnings("unchecked")
					List<Statement> statements = block.statements();
					if (statements.size() > 0) {
						Statement lastStatement = statements.get(statements.size() - 1);
						if (!isDropThroughStatement(lastStatement)) {
							dropsThrough = false;
						}
					}
					break;
				}
			}
		}
		return dropsThrough;
	}
	
	private static String getExceptionSupertype(IJavaProject javaProject, String exception) {
		String exceptionSupertype = null;
		if (!THROWABLE.equals(exception)) {
			if (!EXCEPTION_SUPERTYPE_MAP.containsKey(exception)) {
				try {
					IType exceptionType = javaProject.findType(exception);
					if (exceptionType == null) {
						System.out.println("unable to find exception type "+exception);
					}
					else {
						while (exceptionType.getSuperclassName() != null) {
							if (EXCEPTION_SUPERTYPE_MAP.containsKey(exceptionType.getFullyQualifiedName())) {
								break;
							}
							IType superClassType = exceptionType.isBinary() ? exceptionType.getJavaProject().findType(exceptionType.getSuperclassName()) : JavaUtil.resolveType(exceptionType, exceptionType.getSuperclassName());
							EXCEPTION_SUPERTYPE_MAP.put(exceptionType.getFullyQualifiedName(), superClassType.getFullyQualifiedName());
							exceptionType = superClassType;
						}
					}
				}
				catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
			exceptionSupertype = EXCEPTION_SUPERTYPE_MAP.get(exception);
		}
		return exceptionSupertype;
	}
	
	private static class ExceptionVisitor extends ASTVisitor {
		private ApplicationInfo iApplicationInfo;
		private IJavaProject iJavaProject;
		private boolean iEntityReferencingType = false;
//		private String iLocation;
		private TargetExceptionInfo iTargetExceptionInfo = new TargetExceptionInfo();
		private Deque<TryStatement> iTryStatementStack = new ArrayDeque<TryStatement>();
		private Deque<CatchClause> iCatchClauseStack = new ArrayDeque<CatchClause>();
		
		public ExceptionVisitor(ApplicationInfo applicationInfo, IJavaProject javaProject) {
			iApplicationInfo = applicationInfo;
			iJavaProject = javaProject;
		}
		
		public ExceptionVisitor(ApplicationInfo applicationInfo, IJavaProject javaProject, boolean entityReferencingType) {
			iApplicationInfo = applicationInfo;
			iJavaProject = javaProject;
			iEntityReferencingType = true;
		}

		public boolean visit(Block block) {
			if (block.getParent().getNodeType() == ASTNode.TRY_STATEMENT) {
				TryStatement tryStatement = (TryStatement) block.getParent();
				if (tryStatement.getBody() == block) {
					iTryStatementStack.push(tryStatement);
				}
			}
			else if (block.getParent().getNodeType() == ASTNode.CATCH_CLAUSE) {
				CatchClause catchClause = (CatchClause) block.getParent();
				iCatchClauseStack.push(catchClause);
			}
			return true;
		}
		
		public void endVisit(Block node) {
			if (iTryStatementStack.size() > 0) {
				if (iTryStatementStack.peek().getBody() == node) {
					iTryStatementStack.pop();
				}
			}
			if (iCatchClauseStack.size() > 0) {
				if (iCatchClauseStack.peek().getBody() == node) {
					iCatchClauseStack.pop();
				}
			}
		}
		
		public boolean visit(ClassInstanceCreation classInstanceCreation) {
			IMethodBinding methodBinding = classInstanceCreation.resolveConstructorBinding();
			if (methodBinding != null) {
				processUnhandledExceptions(methodBinding, null, null);
			}
			return true;
		}
		
		public boolean visit(MethodInvocation methodInvocation) {
			IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
			if (methodBinding != null) {
				Expression methodExpression = methodInvocation.getExpression();
				if (methodExpression != null) {
					if (methodExpression.getNodeType() == ASTNode.METHOD_INVOCATION) {
						MethodInvocation methodExpressionMethodInvocation = (MethodInvocation) methodExpression;
						if (methodExpressionMethodInvocation.getExpression() != null && GET_EJB_REF.equals(methodExpressionMethodInvocation.getName().getIdentifier())) {
							if (!AccessBeanUtil.isAccessBeanType(iApplicationInfo, methodExpressionMethodInvocation.getExpression())) {
								methodExpression.setProperty(PROP_SESSION_BEAN, Boolean.TRUE);
								methodExpressionMethodInvocation.getExpression().setProperty(PROP_SESSION_BEAN, Boolean.TRUE);
							}
						}
					}
				}
				processUnhandledExceptions(methodBinding, methodInvocation, methodExpression);
			}
			return true;
		}
		
		public boolean visit(SuperMethodInvocation superMethodInvocation) {
			IMethodBinding methodBinding = superMethodInvocation.resolveMethodBinding();
			if (methodBinding != null) {
				String declaringClassType = methodBinding.getDeclaringClass().getQualifiedName();
				if (!iApplicationInfo.isEntityType(declaringClassType)) {
					processUnhandledExceptions(methodBinding, null, null);
				}
			}
			return true;
		}

		public boolean visit(CatchClause catchClause) {
			boolean visitChildren = true;
			Block block = catchClause.getBody();
			if (iEntityReferencingType && block.statements().size() == 0 && catchClause.getProperty(PROP_HANDLED_TARGET_EXCEPTIONS) == null) {
				SingleVariableDeclaration exceptionDeclaration = catchClause.getException();
				ITypeBinding exceptionTypeBinding = exceptionDeclaration.getType().resolveBinding();
				String exceptionType = exceptionTypeBinding != null ? exceptionTypeBinding.getQualifiedName() : exceptionDeclaration.getType().toString();
				if (OBJECT_NOT_FOUND_EXCEPTION.equals(exceptionType) || FINDER_EXCEPTION.equals(exceptionType)) {
					addHandledTargetException(catchClause, ENTITY_NOT_FOUND_EXCEPTION);
				}
			}
			@SuppressWarnings("unchecked")
			Collection<String> handledTargetExceptions = (Collection<String>) catchClause.getProperty(PROP_HANDLED_TARGET_EXCEPTIONS);
			@SuppressWarnings("unchecked")
			Collection<String> handledSourceExceptions = (Collection<String>) catchClause.getProperty(PROP_HANDLED_SOURCE_EXCEPTIONS);
			SingleVariableDeclaration exceptionDeclaration = catchClause.getException();
			ITypeBinding exceptionTypeBinding = exceptionDeclaration.getType().resolveBinding();
			String exceptionType = exceptionTypeBinding != null ? exceptionTypeBinding.getQualifiedName() : exceptionDeclaration.getType().toString();
			if (handledTargetExceptions == null || handledTargetExceptions.isEmpty()) {
//				@SuppressWarnings("unchecked")
//				List<CatchClause> catchClauses = iTryStatementStack.peek().catchClauses();
//				boolean isOnlyCatchClause = catchClauses.size() == 1;
//				if (!isOnlyCatchClause && catchClauses.get(catchClauses.size() - 1) == catchClause) {
//					isOnlyCatchClause = true;
//					for (CatchClause currentCatchClause : catchClauses) {
//						if (currentCatchClause == catchClause) {
//							break;
//						}
//						if (catchClause.getProperty(PROP_DISCARDED_CATCH_CLAUSE) == null) {
//							isOnlyCatchClause = false;
//							break;
//						}
//					}
//				}
				if (!iEntityReferencingType || (!isRuntimeException(iJavaProject, exceptionType) && !((EXCEPTION.equals(exceptionType) || THROWABLE.equals(exceptionType)) && (handledSourceExceptions == null || handledSourceExceptions.isEmpty())))) {
					catchClause.setProperty(PROP_DISCARDED_CATCH_CLAUSE, Boolean.TRUE);
					visitChildren = false;
				}
			}
			return visitChildren;
		}
		
		public boolean visit(ThrowStatement throwStatement) {
			Expression throwExpression = throwStatement.getExpression();
			ITypeBinding exceptionTypeBinding = throwExpression.resolveTypeBinding();
			if (exceptionTypeBinding != null) {
				String sourceExceptionType = exceptionTypeBinding.getQualifiedName();
				String targetExceptionType = sourceExceptionType;
				if (throwExpression.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION && !iEntityReferencingType) {
					if (OBJECT_NOT_FOUND_EXCEPTION.equals(sourceExceptionType) || FINDER_EXCEPTION.equals(sourceExceptionType)) {
						targetExceptionType = ENTITY_NOT_FOUND_EXCEPTION;
					}
					else if (DUPLICATE_KEY_EXCEPTION.equals(sourceExceptionType) || CREATE_EXCEPTION.equals(sourceExceptionType)) {
						targetExceptionType = ENTITY_EXISTS_EXCEPTION;
					}
					else if (EJB_EXCEPTION.equals(sourceExceptionType) || REMOTE_EXCEPTION.equals(sourceExceptionType)) {
						targetExceptionType = PERSISTENCE_EXCEPTION;
					}
				}
				else if (iCatchClauseStack.size() > 0) {
					CatchClause catchClause = iCatchClauseStack.peek();
					@SuppressWarnings("unchecked")
					Collection<String> handledTargetExceptions = (Collection<String>) catchClause.getProperty(PROP_HANDLED_TARGET_EXCEPTIONS);
					if (OBJECT_NOT_FOUND_EXCEPTION.equals(sourceExceptionType) || FINDER_EXCEPTION.equals(sourceExceptionType)) {
						if (handledTargetExceptions.contains(ENTITY_NOT_FOUND_EXCEPTION)) {
							targetExceptionType = ENTITY_NOT_FOUND_EXCEPTION;
						}
						else if (handledTargetExceptions.contains(NO_RESULT_EXCEPTION)) {
							targetExceptionType = NO_RESULT_EXCEPTION;
						}
					}
					else if (DUPLICATE_KEY_EXCEPTION.equals(sourceExceptionType) || CREATE_EXCEPTION.equals(sourceExceptionType)) {
						if (handledTargetExceptions.contains(ENTITY_EXISTS_EXCEPTION)) {
							targetExceptionType = ENTITY_EXISTS_EXCEPTION;
						}
					}
				}
				if (isUnhandledTargetException(targetExceptionType)) {
					iTargetExceptionInfo.addTargetException(targetExceptionType);
				}
				else {
					throwStatement.setProperty(PROP_TARGET_EXCEPTION, targetExceptionType);
				}
				if (isUnhandledSourceException(sourceExceptionType)) {
					iTargetExceptionInfo.addSourceException(sourceExceptionType);					
				}
			}
			return true;
		}
		
		private void processUnhandledExceptions(IMethodBinding methodBinding, MethodInvocation methodInvocation, Expression methodExpression) {
			if (methodBinding != null) {
				ITypeBinding[] sourceExceptionTypes = methodBinding.getExceptionTypes();
				if (sourceExceptionTypes != null) {
					for (ITypeBinding sourceExceptionType : sourceExceptionTypes) {
						if (isUnhandledSourceException(sourceExceptionType)) {
							iTargetExceptionInfo.addSourceException(sourceExceptionType.getQualifiedName());
						}
					}
				}
				String methodName = methodBinding.getName();
				String methodKey = JavaUtil.getMethodKey(methodBinding);
				String declaringClassType = methodBinding.getDeclaringClass().getQualifiedName();
				EntityInfo entityInfo = iApplicationInfo.getEntityInfoForType(declaringClassType);
				if (iApplicationInfo.isAccessBeanType(declaringClassType)) {
					TargetExceptionInfo targetExceptionInfo = getAccessBeanMethodUnhandledTargetExceptions(entityInfo, methodKey);
					Collection<String> targetExceptions = targetExceptionInfo.getTargetExceptions();
					for (String targetException : targetExceptions) {
						if (isUnhandledTargetException(targetException)) {
							iTargetExceptionInfo.addTargetException(targetException);
						}
					}
					Collection<String> sourceExceptions = targetExceptionInfo.getSourceExceptions();
					for (String sourceException : sourceExceptions) {
						if (isUnhandledSourceException(sourceException)) {
							iTargetExceptionInfo.addSourceException(sourceException);
						}
					}
				}
				else if (iApplicationInfo.isEntityType(declaringClassType)) {
					TargetExceptionInfo targetExceptionInfo = TargetExceptionUtil.getEjbMethodUnhandledTargetExceptions(entityInfo, methodKey);
					Collection<String> targetExceptions = targetExceptionInfo.getTargetExceptions();
					for (String targetException : targetExceptions) {
						if (isUnhandledTargetException(targetException)) {
							iTargetExceptionInfo.addTargetException(targetException);
						}
					}
					Collection<String> sourceExceptions = targetExceptionInfo.getSourceExceptions();
					for (String sourceException : sourceExceptions) {
						if (isUnhandledSourceException(sourceException)) {
							iTargetExceptionInfo.addSourceException(sourceException);
						}
					}
				}
				else if (iApplicationInfo.isHomeInterfaceType(declaringClassType)) {
					// ignore for now - should probably add ENTITY_NOT_FOUND
				}
				else if (FinderResultCacheUtil.isFinderResultCacheUtil(methodBinding.getDeclaringClass())) {
//						&& methodInvocation != null && methodExpression != null &&
//							(FinderResultCacheUtil.isNewAccessBeanMethod(methodName) ||
//								FinderResultCacheUtil.isFindAsCollectionMethod(methodName) || 
//								FinderResultCacheUtil.isGetAsCollectionMethod(methodName) ||
//								FinderResultCacheUtil.isFinderMethod(methodName) ||
//								FinderResultCacheUtil.isUserMethod(iApplicationInfo, methodInvocation) ||
//								FinderResultCacheUtil.isAccessBeanConversionMethodInvocation(iApplicationInfo, methodInvocation) ||
//								FinderResultCacheUtil.isGetCachedAccessBeanMethodInvocation(iApplicationInfo, methodInvocation))) {
					if (FinderResultCacheUtil.isNewAccessBeanMethod(methodName)) {
						// no exceptions
					}
					else if (methodName.startsWith(FIND) && methodName.endsWith(USING_JDBC)) {
						if (sourceExceptionTypes != null) {
							for (ITypeBinding exceptionType : sourceExceptionTypes) {
								if (isUnhandledTargetException(exceptionType)) {
									iTargetExceptionInfo.addTargetException(exceptionType.getQualifiedName());
								}
							}
						}
					}
					else if (methodName.equals(FIND_BY_PRIMARY_KEY)) {
						ITypeBinding returnTypeBinding = methodBinding.getReturnType();
						if (returnTypeBinding != null && iApplicationInfo.isAccessBeanType(returnTypeBinding.getQualifiedName())) {
							if (isUnhandledTargetException(ENTITY_NOT_FOUND_EXCEPTION)) {
								iTargetExceptionInfo.addTargetException(ENTITY_NOT_FOUND_EXCEPTION);
							}
						}
					}
					else if (methodName.startsWith(FIND)) {
						ITypeBinding returnTypeBinding = methodBinding.getReturnType();
						if (returnTypeBinding != null && iApplicationInfo.isAccessBeanType(returnTypeBinding.getQualifiedName())) {
							if (isUnhandledTargetException(NO_RESULT_EXCEPTION)) {
								iTargetExceptionInfo.addTargetException(NO_RESULT_EXCEPTION);
							}
						}
					}
				}
				else if (EJB_OBJECT.equals(declaringClassType) && (methodExpression == null || methodExpression.getProperty(PROP_SESSION_BEAN) == null)) {
					if (methodName.equals(REMOVE)) {
						// no exceptions
					}
				}
				else if (EJB_LOCAL_OBJECT.equals(declaringClassType)) {
					if (methodName.equals(REMOVE)) {
						// no exceptions
					}
				}
				else if (ABSTRACT_ACCESS_BEAN.equals(declaringClassType) && (methodExpression == null || methodExpression.getProperty(PROP_SESSION_BEAN) == null)) {
					if (methodName.equals(GET_EJB_REF)) {
						// no exceptions
					}
				}
				else if (ABSTRACT_ENTITY_ACCESS_BEAN.equals(declaringClassType)) {
					if (methodName.equals(GET_KEY)) {
						// no exceptions
					}
				}
				else if (EC_ENTITY_BEAN.equals(declaringClassType)) {
					if (methodName.equals(GET_FALLBACK_DESCRIPTION)) {
						if (isUnhandledTargetException(ENTITY_NOT_FOUND_EXCEPTION)) {
							iTargetExceptionInfo.addTargetException(ENTITY_NOT_FOUND_EXCEPTION);
						}
					}
					else if (methodName.equals(FULFILLS) || methodName.equals(GET_OWNER) || methodName.equals(GET_GROUPING_ATTRIBUTE_VALUE)) {
						if (isUnhandledTargetException(EXCEPTION)) {
							iTargetExceptionInfo.addTargetException(EXCEPTION);
						}
					}
				}
//					else if (NAMING_CONTEXT.equals(declaringClassType)) {
//						// ignore
//					}
//					else if (PORTABLE_REMOTE_OBJECT.equals(declaringClassType)) {
//						// ignore
//					}
				else if (iApplicationInfo.isAccessBeanSubclass(declaringClassType)) {
					TargetExceptionInfo targetExceptionInfo = TargetExceptionUtil.getAccessBeanSubclassMethodUnhandledTargetExceptions(iApplicationInfo.getAccessBeanSubclassInfoForType(declaringClassType), methodKey);
					if (targetExceptionInfo != null) {
						Collection<String> targetExceptions = targetExceptionInfo.getTargetExceptions();
						for (String targetException : targetExceptions) {
							if (isUnhandledTargetException(targetException)) {
								iTargetExceptionInfo.addTargetException(targetException);
							}
						}
						Collection<String> sourceExceptions = targetExceptionInfo.getSourceExceptions();
						for (String sourceException : sourceExceptions) {
							if (isUnhandledSourceException(sourceException)) {
								iTargetExceptionInfo.addSourceException(sourceException);
							}
						}
					}
				}
//				else if (iApplicationInfo.isEntityInterfaceType(declaringClassType) && methodName.startsWith(GET) && methodBinding.getParameterTypes().length == 0 && entityInfo.getFieldInfoByGetterName(methodName) != null) {
//					if (isUnhandledTargetException(ENTITY_NOT_FOUND_EXCEPTION)) {
//						iTargetExceptionInfo.addTargetException(ENTITY_NOT_FOUND_EXCEPTION);
//					}
//				}
				else if (REFRESH_ONCE_ACCESS_BEAN_HELPER.equals(declaringClassType)) {
					if (methodName.equals(PRE_REFRESH_COPY_HELPER)) {
						if (isUnhandledTargetException(ENTITY_NOT_FOUND_EXCEPTION)) {
							iTargetExceptionInfo.addTargetException(ENTITY_NOT_FOUND_EXCEPTION);
						}
					}
				}
				else {
					if (sourceExceptionTypes != null) {
						for (ITypeBinding exceptionType : sourceExceptionTypes) {
							if (isUnhandledTargetException(exceptionType)) {
//									if (!declaringClassType.startsWith(JAVA_LANG) && !PORT_EXEMPT_EXCEPTION_SOURCES.contains(declaringClassType)) { 
//										System.out.println("adding unhandled exception "+exceptionType.getQualifiedName()+" from "+methodInvocation+ " in " + iLocation+" declaringClassType="+declaringClassType);
//									}
								iTargetExceptionInfo.addTargetException(exceptionType.getQualifiedName());
							}
						}
					}
				}
			}
		}
		
		private boolean isUnhandledSourceException(ITypeBinding exceptionTypeBinding) {
			boolean handled = false;
			if (exceptionTypeBinding != null) {
				for (TryStatement tryStatement : iTryStatementStack) {
					@SuppressWarnings("unchecked")
					List<CatchClause> catchClauses = tryStatement.catchClauses();
					for (CatchClause catchClause : catchClauses) {
						SingleVariableDeclaration exceptionDeclaration = catchClause.getException();
						ITypeBinding handledException = exceptionDeclaration.getType().resolveBinding();
						if (handledException == null) {
							System.out.println("handledException is null");
						}
						if (exceptionTypeBinding.equals(handledException) || exceptionTypeBinding.isSubTypeCompatible(handledException)) {
							addHandledSourceException(catchClause, exceptionTypeBinding.getQualifiedName());
							handled = true;
							break;
						}
						else if (handledException.isSubTypeCompatible(exceptionTypeBinding)) {
							addHandledSourceException(catchClause, handledException.getQualifiedName());
						}
					}
					if (handled) {
						break;
					}
				}
			}
			return !handled;
		}
		
		private boolean isUnhandledSourceException(String exception) {
			boolean handled = false;
			for (TryStatement tryStatement : iTryStatementStack) {
				@SuppressWarnings("unchecked")
				List<CatchClause> catchClauses = tryStatement.catchClauses();
				for (CatchClause catchClause : catchClauses) {
					SingleVariableDeclaration exceptionDeclaration = catchClause.getException();
					String handledException = exceptionDeclaration.getType().resolveBinding().getQualifiedName();
					if (TargetExceptionUtil.catchHandlesException(iJavaProject, handledException, exception)) {
						// I changed this from handledException
						addHandledSourceException(catchClause, exception);
						handled = true;
						break;
					}
					else if (TargetExceptionUtil.catchHandlesException(iJavaProject, exception, handledException)) {
						addHandledSourceException(catchClause, handledException);
					}
				}
				if (handled) {
					break;
				}
			}
			return !handled;
		}
		
		private void addHandledSourceException(CatchClause catchClause, String exception) {
			@SuppressWarnings("unchecked")
			Collection<String> handledExceptions = (Collection<String>) catchClause.getProperty(PROP_HANDLED_SOURCE_EXCEPTIONS);
			if (handledExceptions == null) {
				handledExceptions = new HashSet<String>();
				catchClause.setProperty(PROP_HANDLED_SOURCE_EXCEPTIONS, handledExceptions);
			}
			handledExceptions.add(exception);
		}
		
		
		private boolean isUnhandledTargetException(ITypeBinding exceptionTypeBinding) {
			boolean handled = false;
			if (exceptionTypeBinding != null) {
				for (TryStatement tryStatement : iTryStatementStack) {
					@SuppressWarnings("unchecked")
					List<CatchClause> catchClauses = tryStatement.catchClauses();
					for (CatchClause catchClause : catchClauses) {
						SingleVariableDeclaration exceptionDeclaration = catchClause.getException();
						ITypeBinding handledException = exceptionDeclaration.getType().resolveBinding();
						if (handledException == null) {
							System.out.println("handledException is null");
						}
						if (exceptionTypeBinding.equals(handledException) || exceptionTypeBinding.isSubTypeCompatible(handledException)) {
							addHandledTargetException(catchClause, exceptionTypeBinding.getQualifiedName());
							handled = true;
							break;
						}
						else if (handledException.isSubTypeCompatible(exceptionTypeBinding)) {
							addHandledTargetException(catchClause, handledException.getQualifiedName());
						}
					}
					if (handled) {
						break;
					}
				}
			}
			return !handled;
		}
		
		private boolean isUnhandledTargetException(String exception) {
			boolean handled = false;
			for (TryStatement tryStatement : iTryStatementStack) {
				@SuppressWarnings("unchecked")
				List<CatchClause> catchClauses = tryStatement.catchClauses();
				for (CatchClause catchClause : catchClauses) {
					SingleVariableDeclaration exceptionDeclaration = catchClause.getException();
					String handledException = exceptionDeclaration.getType().resolveBinding().getQualifiedName();
					if (TargetExceptionUtil.catchHandlesTargetException(iJavaProject, handledException, exception)) {
						addHandledTargetException(catchClause, exception);
						handled = true;
						break;
					}
					else if (TargetExceptionUtil.catchHandlesTargetException(iJavaProject, exception, handledException)) {
						addHandledTargetException(catchClause, handledException);
					}
				}
				if (handled) {
					break;
				}
			}
			return !handled;
		}
		
		private void addHandledTargetException(CatchClause catchClause, String exception) {
			@SuppressWarnings("unchecked")
			Collection<String> handledExceptions = (Collection<String>) catchClause.getProperty(PROP_HANDLED_TARGET_EXCEPTIONS);
			if (handledExceptions == null) {
				handledExceptions = new HashSet<String>();
				catchClause.setProperty(PROP_HANDLED_TARGET_EXCEPTIONS, handledExceptions);
			}
			handledExceptions.add(exception);
		}
		
		public TargetExceptionInfo getUnhandledTargetExceptions() {
			return iTargetExceptionInfo;
		}
	}
	
	private static class CatchClauseVisitor extends ASTVisitor {
		private Collection<String> iHandledTargetExceptions;
		
		public CatchClauseVisitor(Collection<String> handledTargetExceptions) {
			iHandledTargetExceptions = handledTargetExceptions;
		}
		
		public boolean visit(SimpleName simpleName) {
			String exceptionType = SIMPLE_NAME_EXCEPTION_MAP.get(simpleName.getIdentifier());
			if (exceptionType != null) {
				String targetExceptionType = getTargetExceptionType(exceptionType);
				if (targetExceptionType != null) {
					JavaUtil.replaceASTNode(simpleName, simpleName.getAST().newName(targetExceptionType));
				}
			}
			return false;
		}
		
		public boolean visit(QualifiedName qualifiedName) {
			String targetExceptionType = getTargetExceptionType(qualifiedName.getFullyQualifiedName());
			if (targetExceptionType != null) {
				JavaUtil.replaceASTNode(qualifiedName, qualifiedName.getAST().newName(targetExceptionType));
			}
			
			return false;
		}
		
		private String getTargetExceptionType(String exceptionType) {
			String targetExceptionType = null;
			if (FINDER_EXCEPTION.equals(exceptionType) || OBJECT_NOT_FOUND_EXCEPTION.equals(exceptionType)) {
				if (iHandledTargetExceptions.contains(NO_RESULT_EXCEPTION) && iHandledTargetExceptions.contains(ENTITY_NOT_FOUND_EXCEPTION)) {
					targetExceptionType = PERSISTENCE_EXCEPTION;
				}
				else if (iHandledTargetExceptions.contains(NO_RESULT_EXCEPTION)) {
					targetExceptionType = NO_RESULT_EXCEPTION;
				}
				else if (iHandledTargetExceptions.contains(ENTITY_NOT_FOUND_EXCEPTION)) {
					targetExceptionType = ENTITY_NOT_FOUND_EXCEPTION;
				}
			}
			else if (CREATE_EXCEPTION.equals(exceptionType) || DUPLICATE_KEY_EXCEPTION.equals(exceptionType)) {
				targetExceptionType = ENTITY_EXISTS_EXCEPTION;
			}					
			return targetExceptionType;
		}
	}
}
