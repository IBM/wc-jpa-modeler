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

import org.eclipse.core.resources.IFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.commerce.jpa.port.info.ColumnInfo;
import com.ibm.commerce.jpa.port.info.ConstraintInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.TableInfo;

public class SchemaDbmParser {
	private static final String LUW_TABLE = "LUW:LUWTable";
	private static final String SQL_TABLE = "SQLTables:PersistentTable"; // ARVERA
	private static final String ORACLE_TABLE = "OracleModel:OracleTable"; // ARVERA
	private static final String ID = "xmi:id";
	private static final String NAME = "name";
	private static final String COLUMNS = "columns";
	private static final String TYPE = "xsi:type";
	private static final String NULLABLE = "nullable";
	private static final String DEFAULT_VALUE = "defaultValue";
	private static final String PRIMITIVE_TYPE = "primitiveType";
	private static final String CONTAINED_TYPE = "containedType";
	private static final String LENGTH = "length";
	private static final String CONSTRAINTS = "constraints";
	private static final String MEMBERS = "members";
	private static final String FOREIGN_KEY = "ForeignKey";
	private static final String UNIQUE_CONSTRAINT = "uniqueConstraint";
	private static final String REFERENCED_MEMBERS = "referencedMembers";
	private static final String REFERENCED_TABLE = "referencedTable";
	
	private IFile iFile;
	private ModuleInfo iModuleInfo;
	
	public SchemaDbmParser(IFile file, ModuleInfo moduleInfo) {
		iFile = file;
		iModuleInfo = moduleInfo;
	}
	
	public void parse() {
		Document document = iModuleInfo.getXMLUtil().readXml(iFile);
		parseXmiElement(document.getDocumentElement());
		iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
	}
	
	private void parseXmiElement(Element xmiElement) {
		NodeList childNodes = xmiElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (LUW_TABLE.equals(nodeName)) {
					parseLUWTableElement(element);
				}
				if(SQL_TABLE.equals(nodeName) || ORACLE_TABLE.equals(nodeName)) { // ARVERA
					System.out.println("ARVERA: Found a NONLUW_TABLE..parsing...");
					parseNONLUWTableElement(element);
				}
			}
		}
	}
	
	private void parseNONLUWTableElement(Element luwTableElement) {
		TableInfo tableInfo = iModuleInfo.getTableInfo(luwTableElement.getAttribute(ID), true);
		iModuleInfo.setTableName(tableInfo, luwTableElement.getAttribute(NAME));
		NodeList childNodes = luwTableElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (COLUMNS.equals(nodeName)) {
					parseColumnElement(element, tableInfo);
				}
				else if (CONSTRAINTS.equals(nodeName)) {
					parseConstraintsElement(element, tableInfo);
				}
			}
		}
	}
	
	
	private void parseLUWTableElement(Element luwTableElement) {
		TableInfo tableInfo = iModuleInfo.getTableInfo(luwTableElement.getAttribute(ID), true);
		iModuleInfo.setTableName(tableInfo, luwTableElement.getAttribute(NAME));
		NodeList childNodes = luwTableElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (COLUMNS.equals(nodeName)) {
					parseColumnElement(element, tableInfo);
				}
				else if (CONSTRAINTS.equals(nodeName)) {
					parseConstraintsElement(element, tableInfo);
				}
			}
		}
	}
	
	private void parseColumnElement(Element columnElement, TableInfo tableInfo) {
		ColumnInfo columnInfo = tableInfo.getColumnInfo(columnElement.getAttribute(ID), true);
		iModuleInfo.addColumnInfo(columnInfo);
		tableInfo.setColumnName(columnInfo, columnElement.getAttribute(NAME).trim());
		if (columnElement.hasAttribute(NULLABLE)) {
			columnInfo.setNullable(!"false".equals(columnElement.getAttribute(NULLABLE)));
		}
		if (columnElement.hasAttribute(DEFAULT_VALUE)) {
			columnInfo.setDefaultValue(columnElement.getAttribute(DEFAULT_VALUE));
		}
		NodeList childNodes = columnElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (CONTAINED_TYPE.equals(nodeName)) {
					parseContainedTypeElement(element, columnInfo);
				}
			}
		}
		if(columnInfo.getColumnName() != null && columnInfo.getColumnName().equals("OPTCOUNTER") &&
				!columnInfo.getPrimitiveType().equals("INTEGER") &&
				!columnInfo.getPrimitiveType().equals("SMALLINT") 
		) {
			tableInfo.addError("DB SCHEMA DBM: " + tableInfo.getTableName() + " has an OPTCOUNTER column that is not a valid type: " + columnInfo.getPrimitiveType());
		}
	}
	
	private void parseContainedTypeElement(Element containedTypeElement, ColumnInfo columnInfo) {
		if (containedTypeElement.hasAttribute(TYPE)) {
			columnInfo.setType(containedTypeElement.getAttribute(TYPE));
		}
		if (containedTypeElement.hasAttribute(NAME)) {
			columnInfo.setTypeName(containedTypeElement.getAttribute(NAME));
		}
		if (containedTypeElement.hasAttribute(PRIMITIVE_TYPE)) {
			columnInfo.setPrimitiveType(containedTypeElement.getAttribute(PRIMITIVE_TYPE));
		}
		if (containedTypeElement.hasAttribute(LENGTH)) {
			columnInfo.setLength(new Integer(containedTypeElement.getAttribute(LENGTH)));
		}
	}
	
	private void parseConstraintsElement(Element constraintsElement, TableInfo tableInfo) {
		ConstraintInfo constraintInfo = tableInfo.getConstraintInfo(constraintsElement.getAttribute(ID), true);
		iModuleInfo.addConstraintInfo(constraintInfo);
		constraintInfo.setConstraintName(constraintsElement.getAttribute(NAME));
		constraintInfo.setType(constraintsElement.getAttribute(TYPE));
		constraintInfo.setMembers(constraintsElement.getAttribute(MEMBERS));
		if (constraintsElement.hasAttribute(UNIQUE_CONSTRAINT)) {
			constraintInfo.setUniqueConstraint(constraintsElement.getAttribute(UNIQUE_CONSTRAINT));
		}
		if (constraintsElement.hasAttribute(FOREIGN_KEY)) {
			constraintInfo.setForeignKey(constraintsElement.getAttribute(FOREIGN_KEY));
		}
		if (constraintsElement.hasAttribute(REFERENCED_MEMBERS)) {
			constraintInfo.setReferencedMembers(constraintsElement.getAttribute(REFERENCED_MEMBERS));
		}
		if (constraintsElement.hasAttribute(REFERENCED_TABLE)) {
			constraintInfo.setReferencedTable(constraintsElement.getAttribute(REFERENCED_TABLE));
		}
		if (ConstraintInfo.PRIMARY_KEY_CONSTRAINT.equals(constraintInfo.getType())) {
			String[] members = constraintInfo.getMembers();
			for (String member : members) {
				if (!tableInfo.getTableName().equals("TFALGOPOL") || !member.equals("_sENdNL0yEdu0kO8I8T7YKw")) {
					tableInfo.addPrimaryKeyColumnInfo(tableInfo.getColumnInfo(member));
				}
			}
		}
	}
}
