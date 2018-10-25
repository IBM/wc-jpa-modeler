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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;

public class EjbEntityCreationDataClassParser {
	private static final String COPY_FIELDS = "copyFields";
	private static final String GET = "get";
	
	private ModuleInfo iModuleInfo;
	private EntityInfo iEntityInfo;
	private ASTParser iASTParser;
	private boolean iCopyFields;
	private Set<FieldInfo> iFields = new HashSet<FieldInfo>();
	
	public EjbEntityCreationDataClassParser(ASTParser astParser, EntityInfo entityInfo) {
		iASTParser = astParser;
		iModuleInfo = entityInfo.getModuleInfo();
		iEntityInfo = entityInfo;
	}

	public void parse(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("parse EntityCreationData type for " + iEntityInfo.getEjbName(), 2100);
			IType entityCreationDataType = iEntityInfo.getEjbEntityCreationDataType();
			if (entityCreationDataType != null) {
				iModuleInfo.addDeleteIntendedType(iEntityInfo.getEjbEntityCreationDataType().getFullyQualifiedName('.'));
				iASTParser.setResolveBindings(true);
				iASTParser.setSource(entityCreationDataType.getCompilationUnit());
				CompilationUnit compilationUnit = (CompilationUnit) iASTParser.createAST(new SubProgressMonitor(progressMonitor, 100));
				iEntityInfo.setEjbEntityCreationDataCompilationUnit(compilationUnit);
				progressMonitor.worked(1000);
				parseCompilationUnit(compilationUnit);
				if (iCopyFields) {
					iEntityInfo.getEntityCreationDataFields().addAll(iFields);
				}
				progressMonitor.worked(1000);
			}
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private void parseCompilationUnit(CompilationUnit compilationUnit) {
		@SuppressWarnings("unchecked")
		List<TypeDeclaration> typeDeclarations = compilationUnit.types();
		TypeDeclaration typeDeclaration = typeDeclarations.get(0);
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			if (bodyDeclaration.getNodeType() == BodyDeclaration.METHOD_DECLARATION) {
				parseMethodDeclaration((MethodDeclaration) bodyDeclaration);
			}
		}
		iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
	}
	
	private void parseMethodDeclaration(MethodDeclaration methodDeclaration) {
		String methodName = methodDeclaration.getName().getIdentifier();
		@SuppressWarnings("unchecked")
		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		if (COPY_FIELDS.equals(methodName) && parameters.size() == 1) {
			methodDeclaration.accept(new CopyFieldsMethodVisitor());
		}
		else if (methodName.startsWith(GET) && parameters.size() == 0) {
			String fieldName = methodName.substring(3);
			FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(fieldName);
			if (fieldInfo == null) {
				fieldInfo = iEntityInfo.getFieldInfoByName(Introspector.decapitalize(fieldName));
			}
			if (fieldInfo != null) {
				fieldInfo.setEntityCreationDataGetterName(methodName);
				iFields.add(fieldInfo);
			}
			else {
				System.out.println("unable to determine field for getter: "+methodName+" in "+iEntityInfo.getEjbEntityCreationDataType().getFullyQualifiedName('.'));
			}
		}
	}
	
	private class CopyFieldsMethodVisitor extends ASTVisitor {

		// So, either we find this code or we don't. If we find the code then it means we need to initialize all of the fields
		
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
		
		
		//AttributeValueEntityCreationData:
//		AttributeValueBeanBase beanBase = (AttributeValueBeanBase)entityBeanBase;
//
//		beanBase.setAttributeValueReferenceNumber(attributeValueReferenceNumber);
//		beanBase.setSequenceNumber(sequenceNumber);
//		beanBase.setCatalogEntryReferenceNumber(catalogEntryReferenceNumber);
//		beanBase.setField1(field1);
//		beanBase.setLanguage_id(language_id);
//		beanBase.setAttributeReferenceNumber(attributeReferenceNumber);
//		beanBase.floatvalue = floatvalue;
//		beanBase.integervalue = integervalue;
//		beanBase.stringvalue = stringvalue;
//		beanBase.setName(name);
//		beanBase.setImage1(image1);
//		beanBase.setImage2(image2);
//		beanBase.setField2(field2);
//		beanBase.setField3(field3);
//		beanBase.setOid(oid);
//		beanBase.setOperatorId(operatorId);
		
		// BaseItemDescriptionEntityCreationData does not have any code in copyFields - it's used like I want to use it:
//		public com.ibm.commerce.inventory.objects.BaseItemDescriptionKey ejbCreate(EntityCreationData baseItemDescriptionEntityCreationData) throws javax.ejb.CreateException, javax.ejb.FinderException, javax.naming.NamingException {
//			
//			initializeFields();
//			BaseItemDescriptionEntityCreationData ecd =(BaseItemDescriptionEntityCreationData)baseItemDescriptionEntityCreationData;
//			setBaseItemId(ecd.getBaseItemId());
//			setLanguageId(ecd.getLanguageId());
//			setLastUpdate(ecd.getLastUpdate());
//			setLongDescription(ecd.getLongDescription());
//			setShortdescription(ecd.getShortdescription());
//
//		return null;
//	}
		// BaseItemEntityCreationData - also no cod in copyFields
		
		// BaseItemResourceManager.getManagedResourceKey(EntityCreationData) - signature of this method will need to change!

		// CatalogEntryEntityCreationData - code is a little different - skips a property - specifically it skips "catalogEntryTypeId" !: Might be ok - shouldn't be a field
		//	CatalogEntryBeanBase beanBase = (CatalogEntryBeanBase)entityBeanBase;
//		Class beanBaseClass = beanBase.getClass();
//		Field[] flds = this.getClass().getFields();
//		for (int i = 0; i < flds.length; i++) {
//			int modifiers = flds[i].getModifiers();
//			if ( flds[i].getName().equals("catalogEntryTypeId") )
//				continue;
//			
//			if ((!Modifier.isFinal(modifiers)) && (!Modifier.isStatic(modifiers)))
//				beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
//		}
		
		public CopyFieldsMethodVisitor() {
		}

		
		public boolean visit(MethodInvocation methodInvocation) {
			//beanBaseClass.getField(flds[i].getName()).set(beanBase, flds[i].get(this));
			boolean visitChildren = true;
			if (methodInvocation.getExpression() != null) {
				ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
				if (typeBinding != null && typeBinding.getQualifiedName().equals("java.lang.reflect.Field") && methodInvocation.getName().getIdentifier().equals("set")) {
					iCopyFields = true;
				}
			}
			return visitChildren;
		}
	}
}
