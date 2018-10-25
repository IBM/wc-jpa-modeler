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

import com.ibm.commerce.jpa.port.info.EntityInfo;

public class AttributeValueEntityAccessBeanGenerator {
	private static final String ATTRIBUTE_FLOAT_VALUE = "AttributeFloatValue";
	private static final String ATTRIBUTE_INTEGER_VALUE = "AttributeIntegerValue";
	private static final String ATTRIBUTE_STRING_VALUE = "AttributeIntegerValue";
	public final static String ATTRIBUTE_VALUE_TYPE_NAME = "com.ibm.commerce.catalog.objects.AttributeValueJPAAccessBean";
	public final static String ATTRIBUTE_FLOAT_VALUE_TYPE_NAME = "com.ibm.commerce.catalog.objects.AttributeFloatValueJPAAccessBean";
	public final static String ATTRIBUTE_INTEGER_VALUE_TYPE_NAME = "com.ibm.commerce.catalog.objects.AttributeIntegerValueJPAAccessBean";
	public final static String ATTRIBUTE_STRING_VALUE_TYPE_NAME = "com.ibm.commerce.catalog.objects.AttributeStringValueJPAAccessBean";
	
	public static final void appendEntityCreationDataConstructor(StringBuilder sb, EntityInfo entityInfo) {
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
		
		String ejbName = entityInfo.getEjbName();
		
		sb.append("\r\n\tpublic ");
		sb.append(ejbName);
		sb.append("JPAAccessBean(com.ibm.commerce.context.content.resources.EntityBeanCreationData entityCreationData) throws EntityExistsException, PersistenceException {\r\n");
		sb.append("\t\tsetEntity(new ");
		sb.append(ejbName);
		sb.append("JPAEntity());\r\n");
		sb.append("\t\tcom.ibm.commerce.catalog.objects.AttributeJPAKey attributeKey = new com.ibm.commerce.catalog.objects.AttributeJPAKey();\r\n");
		sb.append("\t\tcom.ibm.commerce.catalog.objects.JPAAttributeValueEntityCreationData attributeValueEntityCreationData = (com.ibm.commerce.catalog.objects.JPAAttributeValueEntityCreationData) entityCreationData;\r\n");
		sb.append("\t\tiTypedEntity.setAttributeValueReferenceNumber(attributeValueEntityCreationData.getAttributeValueReferenceNumber());\r\n");
		sb.append("\t\tiTypedEntity.setSequenceNumber(attributeValueEntityCreationData.getSequenceNumber());\r\n");
		sb.append("\t\tiTypedEntity.setCatalogEntry(findRelatedCatalogEntryEntity(attributeValueEntityCreationData.getCatalogEntryReferenceNumber()));\r\n");
		sb.append("\t\tiTypedEntity.setField1(attributeValueEntityCreationData.getField1());\r\n");
		sb.append("\t\tattributeKey.setLanguage_id(attributeValueEntityCreationData.getLanguage_id());\r\n");
		sb.append("\t\tattributeKey.setAttributeReferenceNumber(attributeValueEntityCreationData.getAttributeReferenceNumber());\r\n");
		if (ATTRIBUTE_FLOAT_VALUE.equals(entityInfo)) {
			sb.append("\t\tiTypedEntity.setAttributeValue(attributeValueEntityCreationData.getFloatvalue());\r\n");
		}
		else if (ATTRIBUTE_INTEGER_VALUE.equals(entityInfo)) {
			sb.append("\t\tiTypedEntity.setAttributeValue(attributeValueEntityCreationData.getIntegervalue());\r\n");
		}
		else if (ATTRIBUTE_STRING_VALUE.equals(entityInfo)) {
			sb.append("\t\tiTypedEntity.setAttributeValue(attributeValueEntityCreationData.getStringvalue());\r\n");
		}
		sb.append("\t\tiTypedEntity.setName(attributeValueEntityCreationData.getName());\r\n");
		sb.append("\t\tiTypedEntity.setImage1(attributeValueEntityCreationData.getImage1());\r\n");
		sb.append("\t\tiTypedEntity.setImage2(attributeValueEntityCreationData.getImage2());\r\n");
		sb.append("\t\tiTypedEntity.setField2(attributeValueEntityCreationData.getField2());\r\n");
		sb.append("\t\tiTypedEntity.setField3(attributeValueEntityCreationData.getField3());\r\n");
		sb.append("\t\tiTypedEntity.setOid(attributeValueEntityCreationData.getOid());\r\n");
		sb.append("\t\tif (iTypedEntity.getSequenceNumber() == null) {\r\n");
		sb.append("\t\t\tiTypedEntity.setSequenceNumber(new Double(0));\r\n");
		sb.append("\t\t}\r\n");
		sb.append("\t\tif (iTypedEntity.getAttributeValueReferenceNumber() == null) {\r\n");
		sb.append("\t\t\tiTypedEntity.setAttributeValueReferenceNumber(generatePrimaryKey());\r\n");
		sb.append("\t\t}\r\n");
		sb.append("\t\tiTypedEntity.setAttribute(findRelatedAttributeEntity(attributeKey));\r\n");
		sb.append("\t\tgetEntityManager().persist(iEntity);\r\n");
		sb.append("\t}\r\n");
	}

	public static class AttributeValueEntityCreationDataConstructorGenerator implements MethodGenerator {
		public String getMethodKey() {
			return "ejbCreate+com.ibm.commerce.context.content.objects.EntityCreationData";
		}

		public void appendMethod(StringBuilder sb, EntityInfo entityInfo) {
			appendEntityCreationDataConstructor(sb, entityInfo);
		}
	}
	
	public static class AttributeFloatValueEntityCreationDataConstructorGenerator implements MethodGenerator {
		public String getMethodKey() {
			return "ejbCreate+com.ibm.commerce.context.content.objects.EntityCreationData";
		}

		public void appendMethod(StringBuilder sb, EntityInfo entityInfo) {
			appendEntityCreationDataConstructor(sb, entityInfo);
		}
	}
	
	public static class AttributeIntegerValueEntityCreationDataConstructorGenerator implements MethodGenerator {
		public String getMethodKey() {
			return "ejbCreate+com.ibm.commerce.context.content.objects.EntityCreationData";
		}

		public void appendMethod(StringBuilder sb, EntityInfo entityInfo) {
			appendEntityCreationDataConstructor(sb, entityInfo);
		}
	}
	
	public static class AttributeStringValueEntityCreationDataConstructorGenerator implements MethodGenerator {
		public String getMethodKey() {
			return "ejbCreate+com.ibm.commerce.context.content.objects.EntityCreationData";
		}

		public void appendMethod(StringBuilder sb, EntityInfo entityInfo) {
			appendEntityCreationDataConstructor(sb, entityInfo);
		}
	}
}
