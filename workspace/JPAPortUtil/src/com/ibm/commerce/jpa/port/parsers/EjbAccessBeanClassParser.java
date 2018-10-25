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
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.commerce.jpa.port.info.AccessBeanInfo;
import com.ibm.commerce.jpa.port.info.AccessBeanInfo.CopyHelperProperty;
import com.ibm.commerce.jpa.port.info.AccessBeanInfo.NullConstructorParameter;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.util.JavaUtil;

public class EjbAccessBeanClassParser {
	private static final String GET = "get";
	private static final String SET = "set";
	private static final String IN_EJB_TYPE = "InEJBType";
	private static final String STRING_CONVERTER = "com.ibm.ivj.ejb.runtime.StringConverter";
	private static final String GET_CACHE = "__getCache";
	private static final String SET_CACHE = "__setCache";
	private static final String SET_INIT_KEY = "setInitKey_";
	private EntityInfo iEntityInfo;
	private ModuleInfo iModuleInfo;
	private AccessBeanInfo iAccessBeanInfo;
	private ASTParser iASTParser;
	
	public EjbAccessBeanClassParser(ASTParser astParser, EntityInfo entityInfo) {
		iASTParser = astParser;
		iEntityInfo = entityInfo;
		iModuleInfo = entityInfo.getModuleInfo();
		iAccessBeanInfo = entityInfo.getAccessBeanInfo();
	}
	
	public void parse(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("parse access bean class for " + iEntityInfo.getEjbName(), IProgressMonitor.UNKNOWN);
			IType accessBeanType = iEntityInfo.getEjbAccessBeanType();
			if (accessBeanType != null) {
				parseAccessBeanClass(progressMonitor, accessBeanType);
			} else if ( iEntityInfo.getAccessBeanInfo().getAccessBeanPackage() != null){
				//AB should have a fully qualified name
				iEntityInfo.getAccessBeanInfo().addError("ibm-ejb-access-bean.xmi: AccessBean class " + iEntityInfo.getAccessBeanInfo().getQualifiedAccessBeanName() +  " does not exist");
			} else {
				iEntityInfo.getAccessBeanInfo().addError("ibm-ejb-access-bean.xmi: Could not locate access bean for entity " + iEntityInfo.getEjbClass());
			}
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private void parseAccessBeanClass(IProgressMonitor progressMonitor, IType accessBeanType) throws JavaModelException {
		iModuleInfo.addDeleteIntendedType(accessBeanType.getFullyQualifiedName('.'));
		String[] superInterfaceNames = accessBeanType.getSuperInterfaceNames();
		for (String superInterfaceName : superInterfaceNames) {
			IType superInterface = JavaUtil.resolveType(accessBeanType, superInterfaceName);
			if (superInterface != null && superInterface.getResource() != null && superInterface.getResource().getProject() == accessBeanType.getResource().getProject()) {
				iAccessBeanInfo.addAccessBeanInterface(superInterface.getFullyQualifiedName('.'));
				iModuleInfo.addDeleteIntendedType(superInterface.getFullyQualifiedName('.'));
			}
		}
		iASTParser.setResolveBindings(true);
		iASTParser.setSource(accessBeanType.getCompilationUnit());
		CompilationUnit compilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
		TypeDeclaration typeDeclaration = (TypeDeclaration) compilationUnit.types().get(0);
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			if (bodyDeclaration.getNodeType() == ASTNode.METHOD_DECLARATION) {
				parseMethodDeclaration(accessBeanType, (MethodDeclaration) bodyDeclaration);
				progressMonitor.worked(100);
			}
		}
		iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
	}
	
	private void parseMethodDeclaration(IType primaryKeyType, MethodDeclaration methodDeclaration) {
		if (!methodDeclaration.isConstructor()) {
			String methodName = methodDeclaration.getName().getIdentifier();
			if (methodName.startsWith(GET) && methodDeclaration.parameters().size() == 0 && !methodName.endsWith(IN_EJB_TYPE)) {
				GetterMethodVisitor getterMethodVisitor = new GetterMethodVisitor();
				methodDeclaration.getBody().accept(getterMethodVisitor);
				if (getterMethodVisitor.getFieldName() != null) {
					CopyHelperProperty copyHelperProperty = iAccessBeanInfo.getCopyHelperProperty(getterMethodVisitor.getFieldName(), false);
					if (copyHelperProperty == null) {
						copyHelperProperty = iAccessBeanInfo.getCopyHelperProperty(getterMethodVisitor.getFieldName(), true);
						copyHelperProperty.setType(getterMethodVisitor.getFieldType());
					}
					if (iAccessBeanInfo.isExcludedPropertyName(getterMethodVisitor.getFieldName())) {
						iAccessBeanInfo.removeExcludedPropertyName(getterMethodVisitor.getFieldName());
					}
					copyHelperProperty.setGetterName(methodName);
					copyHelperProperty.setConverterClassName(getterMethodVisitor.getConverterClassName());
				}
				else {
					FieldInfo fieldInfo = iEntityInfo.getFieldInfoByGetterName(methodName);
					if (fieldInfo != null) {
						if (iAccessBeanInfo.isExcludedPropertyName(fieldInfo.getFieldName())) {
							iAccessBeanInfo.removeExcludedPropertyName(fieldInfo.getFieldName());
						}
						CopyHelperProperty copyHelperProperty = iAccessBeanInfo.getCopyHelperProperty(fieldInfo.getFieldName(), false);
						if (copyHelperProperty == null) {
							copyHelperProperty = iAccessBeanInfo.getCopyHelperProperty(fieldInfo.getFieldName(), true);
							copyHelperProperty.setType(fieldInfo.getTypeName());
						}
						copyHelperProperty.setGetterName(methodName);
					}
				}
			}
			else if (methodName.startsWith(SET_INIT_KEY) && methodDeclaration.parameters().size() == 1) {
				String fieldName = methodName.substring(SET_INIT_KEY.length());
				FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
				if (fieldInfo != null) {
					SetInitKeyMethodVisitor setInitKeyMethodVisitor = new SetInitKeyMethodVisitor();
					methodDeclaration.getBody().accept(setInitKeyMethodVisitor);
					NullConstructorParameter nullConstructorParameter = iAccessBeanInfo.getNullConstructorParameterByName(fieldName);
					if (nullConstructorParameter == null) {
						nullConstructorParameter = iAccessBeanInfo.getNullConstructorParameter(fieldName, true);
						nullConstructorParameter.setName(fieldName);
						nullConstructorParameter.setType(setInitKeyMethodVisitor.getFieldType());
					}
					if (setInitKeyMethodVisitor.getConverterClassName() != null) {
						nullConstructorParameter.setConverterClassName(setInitKeyMethodVisitor.getConverterClassName());
					}
				}
			}
			else if (methodName.startsWith(SET) && methodDeclaration.parameters().size() == 1) {
				SetterMethodVisitor setterMethodVisitor = new SetterMethodVisitor();
				methodDeclaration.getBody().accept(setterMethodVisitor);
				if (setterMethodVisitor.getFieldName() != null) {
					CopyHelperProperty copyHelperProperty = iEntityInfo.getAccessBeanInfo().getCopyHelperProperty(setterMethodVisitor.getFieldName(), false);
					if (copyHelperProperty == null) {
						copyHelperProperty = iAccessBeanInfo.getCopyHelperProperty(setterMethodVisitor.getFieldName(), true);
						copyHelperProperty.setType(setterMethodVisitor.getFieldType());
					}
					if (iAccessBeanInfo.isExcludedPropertyName(setterMethodVisitor.getFieldName())) {
						iAccessBeanInfo.removeExcludedPropertyName(setterMethodVisitor.getFieldName());
					}
					copyHelperProperty.setSetterName(methodName);
					if (setterMethodVisitor.getConverterClassName() != null) {
						copyHelperProperty.setConverterClassName(setterMethodVisitor.getConverterClassName());
					}
				}
			}
		}
	}
	
	private class SetInitKeyMethodVisitor extends ASTVisitor {
		private String iConverterClassName;
		private String iFieldType;
		
		public SetInitKeyMethodVisitor() {
		}
	
		public String getConverterClassName() {
			return iConverterClassName;
		}
		
		public String getFieldType() {
			return iFieldType;
		}

		public boolean visit(MethodInvocation methodInvocation) {
			if (methodInvocation.getExpression() != null && methodInvocation.arguments().size() == 1) {
				ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
				if (typeBinding != null) {
					ITypeBinding[] interfaces = typeBinding.getInterfaces();
					if (interfaces != null) {
						for (ITypeBinding interfaceTypeBinding : interfaces) {
							if (STRING_CONVERTER.equals(interfaceTypeBinding.getQualifiedName())) {
								iConverterClassName = typeBinding.getQualifiedName();
								iFieldType = methodInvocation.resolveTypeBinding().getQualifiedName();
								break;
							}
						}
					}
				}
			}
			return true;
		}
	}
	
	private class GetterMethodVisitor extends ASTVisitor {
		private String iFieldName;
		private String iConverterClassName;
		private String iFieldType;
		
		public GetterMethodVisitor() {
		}

		public String getFieldName() {
			return iFieldName;
		}
		
		public String getConverterClassName() {
			return iConverterClassName;
		}
		
		public String getFieldType() {
			return iFieldType;
		}
		
		public boolean visit(MethodInvocation methodInvocation) {
			String methodName = methodInvocation.getName().getIdentifier();
			if (GET_CACHE.equals(methodName) && methodInvocation.arguments().size() == 1) {
				Expression argument = (Expression) methodInvocation.arguments().get(0);
				if (argument.getNodeType() == ASTNode.STRING_LITERAL) {
					StringLiteral stringLiteral = (StringLiteral) argument;
					iFieldName = stringLiteral.getLiteralValue();
				}
			}
			else if (methodInvocation.getExpression() != null && methodInvocation.arguments().size() == 1) {
				ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
				if (typeBinding != null) {
					ITypeBinding[] interfaces = typeBinding.getInterfaces();
					if (interfaces != null) {
						for (ITypeBinding interfaceTypeBinding : interfaces) {
							if (STRING_CONVERTER.equals(interfaceTypeBinding.getQualifiedName())) {
								iConverterClassName = typeBinding.getQualifiedName();
								iFieldType = ((Expression) methodInvocation.arguments().get(0)).resolveTypeBinding().getQualifiedName();
								break;
							}
						}
					}
				}
			}
			return true;
		}
		
		public void endVisit(ReturnStatement returnStatement) {
			if (iFieldType == null && returnStatement.getExpression() != null) {
				iFieldType = returnStatement.getExpression().resolveTypeBinding().getQualifiedName();
			}
		}
	}
	
	private class SetterMethodVisitor extends ASTVisitor {
		private String iFieldName;
		private String iConverterClassName;
		private String iFieldType;
		
		public SetterMethodVisitor() {
		}

		public String getFieldName() {
			return iFieldName;
		}
		
		public String getFieldType() {
			return iFieldType;
		}
		
		public String getConverterClassName() {
			return iConverterClassName;
		}
		
		public boolean visit(MethodInvocation methodInvocation) {
			String methodName = methodInvocation.getName().getIdentifier();
			if (SET_CACHE.equals(methodName) && methodInvocation.arguments().size() == 2) {
				Expression argument = (Expression) methodInvocation.arguments().get(0);
				if (argument.getNodeType() == ASTNode.STRING_LITERAL) {
					StringLiteral stringLiteral = (StringLiteral) argument;
					iFieldName = stringLiteral.getLiteralValue();
					iFieldType = ((Expression) methodInvocation.arguments().get(1)).resolveTypeBinding().getQualifiedName();
				}
			}
			else if (methodInvocation.getExpression() != null) {
				ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
				if (typeBinding != null) {
					ITypeBinding[] interfaces = typeBinding.getInterfaces();
					if (interfaces != null) {
						for (ITypeBinding interfaceTypeBinding : interfaces) {
							if (STRING_CONVERTER.equals(interfaceTypeBinding.getQualifiedName())) {
								iConverterClassName = typeBinding.getQualifiedName();
								break;
							}
						}
					}
				}
			}
			return true;
		}
	}
}
