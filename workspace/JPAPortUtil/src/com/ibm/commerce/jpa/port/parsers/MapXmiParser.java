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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.commerce.jpa.port.info.ColumnInfo;
import com.ibm.commerce.jpa.port.info.ConstraintInfo;
import com.ibm.commerce.jpa.port.info.EjbRelationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.TableInfo;
import com.ibm.commerce.jpa.port.util.XMLUtil;

public class MapXmiParser {
	private static final String NESTED = "nested";
	private static final String HELPER = "helper";
	private static final String INPUTS = "inputs";
	private static final String OUTPUTS = "outputs";
	private static final String TYPE = "xmi:type";
	private static final String HREF = "href";
	private static final String EJB_MAPPER = "ejbrdbmapping:RDBEjbMapper";
	private static final String PERSISTENT_TABLE = "SQLTables:PersistentTable";
	private static final String LUW_TABLE = "LUW:LUWTable";
	private static final String ORACLE_TABLE = "OracleModel:OracleTable";
	private static final String CONTAINER_MANAGED_ENTITY = "ejb:ContainerManagedEntity";
	private static final String PRIMARY_TABLE_STRATEGY = "ejbrdbmapping:PrimaryTableStrategy";
	private static final String EJB_FIELD_MAPPER = "ejbrdbmapping:RDBEjbFieldMapper";
	private static final String SQLTABLES_COLUMN = "SQLTables:Column";
	private static final String LUW_COLUMN = "LUW:LUWColumn";
	private static final String CMP_ATTRIBUTE = "ejb:CMPAttribute";
	private static final String EJB_CONVERTER = "ejbrdbmapping:EJBConverter";
	private static final String TARGET_CLASS = "targetClass";
	private static final String TRANSFORMER_CLASS = "transformerClass";
	private static final String INHERITED_PRIMARY_TABLE_STRATEGY = "ejbrdbmapping:InheritedPrimaryTableStrategy";
	private static final String DISCRIMINATOR_VALUES = "discriminatorValues";
	private static final String DISCRIMINATOR_MEMBERS = "discriminatorMembers";
	private static final String SECONDARY_STRATEGY = "secondaryStrategy";
	private static final String TABLE = "table";
	private static final String JOIN_KEY = "joinKey";
	private static final String TYPE_MAPPING = "typeMapping";
	private static final String EJB_RELATIONSHIP_ROLE = "ejb:EJBRelationshipRole";
	private static final String SQL_CONSTRAINTS_FOREIGN_KEY = "SQLConstraints:ForeignKey";
	
	private IFile iFile;
	private ModuleInfo iModuleInfo;
	
	public MapXmiParser(IFile file, ModuleInfo moduleInfo) {
		iFile = file;
		iModuleInfo = moduleInfo;
	}
	
	public void parse() {
		Document document = iModuleInfo.getXMLUtil().readXml(iFile);
		parseEjbRdbDocumentRootElement(document.getDocumentElement());
		iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
	}
	
	private void parseEjbRdbDocumentRootElement(Element ejbRdbDocumentRootElement) {
		NodeList childNodes = ejbRdbDocumentRootElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				String type = element.getAttribute(TYPE);
				if (NESTED.equals(nodeName) && EJB_MAPPER.equals(type)) {
					parseEjbMapperElement(element);
				}
			}
		}
	}
	
	private void parseEjbMapperElement(Element ejbMapperElement) {
		EntityInfo entityInfo = null;
		List<TableInfo> tables = new ArrayList<TableInfo>();
		NodeList childNodes = ejbMapperElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				String type = element.getAttribute(TYPE);
				// 2018-04-27 - bsteinba@us.ibm.com - added condition to catch Oracle tables
				if ((INPUTS.equals(nodeName) || OUTPUTS.equals(nodeName)) && (PERSISTENT_TABLE.equals(type) || LUW_TABLE.equals(type) || ORACLE_TABLE.equals(type))) {
					tables.add(iModuleInfo.getTableInfo(getIdFromHref(element)));
				}
				else if ((INPUTS.equals(nodeName) || OUTPUTS.equals(nodeName)) && CONTAINER_MANAGED_ENTITY.equals(type)) {
					entityInfo = iModuleInfo.getEntityInfo(getIdFromHref(element));
				}
			}			
		}
		if(entityInfo != null) {
			for (TableInfo tableInfo : tables) {
				entityInfo.addTable(tableInfo);
			}
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node node = childNodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) node;
					String nodeName = element.getNodeName();
					String type = element.getAttribute(TYPE);
					if (HELPER.equals(nodeName) && PRIMARY_TABLE_STRATEGY.equals(type)) {
						parsePrimaryTableStrategyElement(element, entityInfo);
					}
					else if (HELPER.equals(nodeName) && INHERITED_PRIMARY_TABLE_STRATEGY.equals(type)) {
						parseInheritedPrimaryTableStrategyElement(element, entityInfo);
					}
					else if (NESTED.equals(nodeName) && EJB_FIELD_MAPPER.equals(type)) {
						parseEjbFieldMapperElement(element, entityInfo);
					}
				}
			}
		}
	}
	
	private void parsePrimaryTableStrategyElement(Element primaryTableStrategyElement, EntityInfo entityInfo) {
		String discriminatorMemberId = null;
		NodeList childNodes = primaryTableStrategyElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (DISCRIMINATOR_VALUES.equals(nodeName)) {
					entityInfo.setDiscriminatorValue(XMLUtil.getElementText(element));
				}
				else if (DISCRIMINATOR_MEMBERS.equals(nodeName)) {
					discriminatorMemberId = getIdFromHref(element);
				}
				else if (TABLE.equals(nodeName)) {
					System.out.println("---- ARVERA:["+this.getClass().getCanonicalName()+"]: file: "+ iFile.getName()); 
					System.out.println("---- ARVERA:["+this.getClass().getCanonicalName()+"]:"+getIdFromHref(element));
					entityInfo.setPrimaryTableInfo(iModuleInfo.getTableInfo(getIdFromHref(element))); //ARVERA
				}
			}
		}
		if (discriminatorMemberId != null) {
			entityInfo.setDiscriminatorColumnInfo(iModuleInfo.getColumnInfo(discriminatorMemberId));
		}
	}

	private void parseInheritedPrimaryTableStrategyElement(Element inheritedPrimaryTableStrategyElement, EntityInfo entityInfo) {
		NodeList childNodes = inheritedPrimaryTableStrategyElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (DISCRIMINATOR_VALUES.equals(nodeName)) {
					entityInfo.setDiscriminatorValue(XMLUtil.getElementText(element));
				}
				else if (SECONDARY_STRATEGY.equals(nodeName)) {
					parseSecondaryStrategyElement(element, entityInfo);
				}
			}
		}
	}
	
	private void parseSecondaryStrategyElement(Element secondaryStrategyElement, EntityInfo entityInfo) {
		TableInfo tableInfo = null;
		String joinKeyId = null;
		NodeList childNodes = secondaryStrategyElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (TABLE.equals(nodeName)) {
					tableInfo = iModuleInfo.getTableInfo(getIdFromHref(element));
					entityInfo.setSecondaryTableInfo(tableInfo);
				}
				else if (JOIN_KEY.equals(nodeName)) {
					joinKeyId = getIdFromHref(element);
				}
			}
		}
		entityInfo.setJoinKey(tableInfo.getConstraintInfo(joinKeyId));
	}
	
	private void parseEjbFieldMapperElement(Element ejbFieldMapperElement, EntityInfo entityInfo) {
		ColumnInfo columnInfo = null;
		FieldInfo fieldInfo = null;
		EjbRelationInfo ejbRelationInfo = null;
		ConstraintInfo constraintInfo = null;
		NodeList childNodes = ejbFieldMapperElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				String type = element.getAttribute(TYPE);
				if ((INPUTS.equals(nodeName) || OUTPUTS.equals(nodeName)) && (SQLTABLES_COLUMN.equals(type) || LUW_COLUMN.equals(type))) {
					columnInfo = iModuleInfo.getColumnInfo(getIdFromHref(element));
				}
				else if ((OUTPUTS.equals(nodeName) || INPUTS.equals(nodeName)) && CMP_ATTRIBUTE.equals(type)) {
					fieldInfo = entityInfo.getFieldInfo(getIdFromHref(element));
				}
				else if (INPUTS.equals(nodeName) && EJB_RELATIONSHIP_ROLE.equals(type)) {
					ejbRelationInfo = iModuleInfo.getEjbRelationshipRoleInfo(getIdFromHref(element)).getEjbRelationInfo();
				}
				else if (OUTPUTS.equals(nodeName) && SQL_CONSTRAINTS_FOREIGN_KEY.equals(type)) {
					constraintInfo = iModuleInfo.getConstraintInfo(getIdFromHref(element));
				}
			}
		}
		if (fieldInfo != null) {
			fieldInfo.setColumnInfo(columnInfo);
			Set<EntityInfo> subEntities = entityInfo.getSubtypes();
			if (subEntities != null) {
				for (EntityInfo subEntity : subEntities) {
					FieldInfo subFieldInfo = subEntity.getFieldInfoByName(fieldInfo.getFieldName());
					if (subFieldInfo != null) {
						subFieldInfo.setColumnInfo(columnInfo);
						parseEjbFieldMapperElement(ejbFieldMapperElement, subFieldInfo);
					}
				}
			}
		}
		else if (ejbRelationInfo != null) {
			ejbRelationInfo.setConstraintInfo(constraintInfo);
		}
		else {
			System.out.println("no field or ejb relation info "+iModuleInfo.getJavaProject().getElementName()+" "+ejbFieldMapperElement.getAttribute("xmi:id"));
		}
	}
	
	private void parseEjbFieldMapperElement(Element ejbFieldMapperElement, FieldInfo fieldInfo) {
		NodeList childNodes = ejbFieldMapperElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				String type = element.getAttribute(TYPE);
				if (HELPER.equals(nodeName) && EJB_CONVERTER.equals(type)) {
					parseEjbConverterElement(element, fieldInfo);
				}
				else if (TYPE_MAPPING.equals(nodeName)) {
					fieldInfo.setTypeMapping(getIdFromHref(element));
				}
			}
		}
	}
	
	private void parseEjbConverterElement(Element ejbConverterElement, FieldInfo fieldInfo) {
		NodeList childNodes = ejbConverterElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (TARGET_CLASS.equals(nodeName)) {
					fieldInfo.setTargetClass(element.getAttribute(HREF));
				}
				else if (TRANSFORMER_CLASS.equals(nodeName)) {
					fieldInfo.setTransformerClass(element.getAttribute(HREF));
				}
			}
		}
	}
	
	private String getIdFromHref(Element element) {
		String id = null;
		if (element.hasAttribute(HREF)) {
			String href = element.getAttribute(HREF);
			int index = href.indexOf('#');
			if (index > -1) {
				id = href.substring(index + 1);
			}
		}
		return id;
	}
}
