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

import java.io.File;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ColumnInfo;
import com.ibm.commerce.jpa.port.info.ForeignKeyInfo;
import com.ibm.commerce.jpa.port.info.TableInfo;

/**
 * Parses foreign key relationships for OOB tables out of the xml files in the <PLUGIN_ROOT>/configuration directory.
 * It stores these relationships in the foreignKeys element within the TableInfo objects that are stored within the ApplicationInfo object.
 * 
 *
 */
public class PortConfigurationParser {
	private static final String TABLE = "Table";
	private static final String TABLE_NAME = "tableName";
	private static final String COLUMN = "Column";
	private static final String TYPE = "type";
	private static final String LENGTH = "length";
	private static final String FOREIGN_KEY = "ForeignKey";
	private static final String PARENT_TABLE_NAME = "parentTableName";
	private static final String MEMBER = "Member";
	private static final String COLUMN_NAME = "columnName";
	private static final String REFERENCED_COLUMN_NAME = "referencedColumnName";
	private static final String NULLABLE = "nullable";
	
	private ApplicationInfo iApplicationInfo;
	private File iPortConfigurationFile;
	
	public PortConfigurationParser(ApplicationInfo applicationInfo, File portConfigurationFile) {
		iApplicationInfo = applicationInfo;
		iPortConfigurationFile = portConfigurationFile;
	}
	
	public void parse() {
		Document document = iApplicationInfo.getXMLUtil().readXml(iPortConfigurationFile);
		if (document == null || document.getDocumentElement() == null) {
			System.out.println("null document "+iPortConfigurationFile);
			iPortConfigurationFile.delete();
		}
		else {
			parsePortConfigurationElement(document.getDocumentElement());
			iApplicationInfo.incrementParsedAssetCount();
		}
	}
	
	private void parsePortConfigurationElement(Element portConfigurationElement) {
		NodeList childNodes = portConfigurationElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (TABLE.equals(nodeName)) {
					parseTableElement(element);
				}
//				else if ("table".equals(nodeName)) {
//					parseXXXTableElement(element);
//				}
			}
		}
	}
	
	private void parseTableElement(Element tableElement) {
		String tableName = tableElement.getAttribute(TABLE_NAME);
		TableInfo tableInfo = iApplicationInfo.getTableInfo(tableName);
		if (tableInfo != null) {
			NodeList childNodes = tableElement.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node node = childNodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) node;
					String nodeName = element.getNodeName();
					if (FOREIGN_KEY.equals(nodeName)) {
						parseForeignKeyElement(tableInfo, element);
					}
					else if (COLUMN.equals(nodeName)) {
						parseColumnElement(tableInfo, element);
					}
				}	
			}
		}
		else {
			System.out.println("Error parsing configuration file " + iPortConfigurationFile + ". Invalid table name: " + tableName);
		}
	}
	
	private void parseForeignKeyElement(TableInfo tableInfo, Element foreignKeyElement) {
		String parentTableName = foreignKeyElement.getAttribute(PARENT_TABLE_NAME);
		TableInfo parentTableInfo = iApplicationInfo.getTableInfo(parentTableName);
		if (parentTableInfo != null) {
			ForeignKeyInfo foreignKeyInfo = new ForeignKeyInfo(tableInfo);
			NodeList childNodes = foreignKeyElement.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node node = childNodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) node;
					String nodeName = element.getNodeName();
					if (MEMBER.equals(nodeName)) {
						String columnName = element.getAttribute(COLUMN_NAME);
						ColumnInfo memberColumn = tableInfo.getColumnInfoByName(columnName);
						if (memberColumn == null) {
							System.out.println("Error parsing configuration file " + iPortConfigurationFile + ". Invalid column name: " + columnName);
						}
						String referencedColumnName = element.getAttribute(REFERENCED_COLUMN_NAME);
						ColumnInfo referencedColumn = parentTableInfo.getColumnInfoByName(referencedColumnName);
						if (referencedColumn == null) {
							System.out.println("Error parsing configuration file " + iPortConfigurationFile + ". Invalid referenced column name: " + referencedColumnName);
						}
						if (memberColumn != null && referencedColumn != null) {
							boolean found = false;
							Set<ForeignKeyInfo> foreignKeys = tableInfo.getForeignKeys();
							for (ForeignKeyInfo current : foreignKeys) {
								List<ColumnInfo> members = current.getMemberColumns();
								for (ColumnInfo member : members) {
									if (member == memberColumn) {
										System.out.println("foreign key exists "+tableInfo.getTableName()+"."+columnName);
										found = true;
									}
								}
							}
							if (!found) {
								foreignKeyInfo.addMember(memberColumn, referencedColumn);
							}
							else {
								foreignKeyInfo = null;
								break;
							}
						}
					}
				}
			}
			if (foreignKeyInfo != null) {
				tableInfo.addForeignKey(foreignKeyInfo);
				foreignKeyInfo.setParentTableInfo(parentTableInfo);
			}
		}
		else {
			System.out.println("Error parsing configuration file " + iPortConfigurationFile + ". Invalid parent table name: " + parentTableName);
		}
	}
	
	private void parseColumnElement(TableInfo tableInfo, Element columnElement) {
		String columnName = columnElement.getAttribute(COLUMN_NAME);
		ColumnInfo columnInfo = tableInfo.getColumnInfoByName(columnName);
		if (columnInfo != null) {
			if (columnElement.hasAttribute(TYPE)) {
				columnInfo.setTypeName(columnElement.getAttribute(TYPE));
			}
			if (columnElement.hasAttribute(LENGTH)) {
				columnInfo.setLength(new Integer(columnElement.getAttribute(LENGTH)));
			}
			if (columnElement.hasAttribute(NULLABLE)) {
				columnInfo.setNullable(Boolean.parseBoolean(columnElement.getAttribute(NULLABLE)));
			}
		}
		else {
			System.out.println("Error parsing configuration file " + iPortConfigurationFile + ". Invalid column name: " + columnName + " in table " + tableInfo.getTableName());
		}
	}
	
//	private void parseXXXTableElement(Element tableElement) {
//		String tableName = tableElement.getAttribute("name");
//		TableInfo tableInfo = iApplicationInfo.getTableInfo(tableName);
//		if (tableInfo == null) {
////			System.out.println("could not find table info for " + tableName);
//		}
//		else if (tableInfo.getEntityInfo() != null) {
//			Collection<String> processedColumns = new HashSet<String>();
//			NodeList childNodes = tableElement.getChildNodes();
//			for (int i = 0; i < childNodes.getLength(); i++) {
//				Node node = childNodes.item(i);
//				if (node.getNodeType() == Node.ELEMENT_NODE) {
//					Element element = (Element) node;
//					String nodeName = element.getNodeName();
//					if ("normal".equals(nodeName) || "imported".equals(nodeName) || "exported".equals(nodeName)) {
//						String columnName = element.getAttribute("column");
//						if (!processedColumns.contains(columnName)) {
//							processedColumns.add(columnName);
//							String type = element.getAttribute("type");
//							if ("CHAR () FOR BIT DATA".equals(type)) {
//								type = "CHAR FOR BIT DATA";
//							}
//							Integer length = null;
//							if (element.hasAttribute("length") && !type.equals("INTEGER") && !type.equals("BIGINT") && !type.equals("SMALLINT") && !type.equals("TIMESTAMP") && !type.equals("DECIMAL") && !type.equals("DOUBLE") && !(type.equals("CHAR") && "1".equals(element.getAttribute("length")))) {
//								length = new Integer(element.getAttribute("length"));
//							}
//							boolean nullable = Boolean.parseBoolean(element.getAttribute("nullable"));
//							ColumnInfo columnInfo = tableInfo.getColumnInfoByName(columnName);
//							if (columnInfo == null) {
////								System.out.println("could not find column info for " + columnName + " in " + tableName);
//							}
//							else {
//								if (!type.equals(columnInfo.getTypeName())) {
//									System.out.println("types not equal - " + type + " " + columnInfo.getTypeName() + " " + tableName + "." + columnName);
//									System.out.println(tableInfo.getEntityInfo().getEjbName());
//									System.out.println("<PortConfiguration>");
//									System.out.println("\t<Table tableName=\"" + tableName + "\">");
//									if (length != null) {
//										System.out.println("\t\t<Column columnName=\"" + columnName + "\" type=\"" + type + "\" length=\"" + length + "\" nullable=\"" + nullable + "\"/>");
//									}
//									else {
//										System.out.println("\t\t<Column columnName=\"" + columnName + "\" type=\"" + type + "\" nullable=\"" + nullable + "\"/>");
//									}
//									System.out.println("\t</Table>");
//									System.out.println("</PortConfiguration>");
//								}
//								else if (length != null && !length.equals(columnInfo.getLength())) {
//									System.out.println("lengths not equal - " + length + " " + columnInfo.getLength() + " " + tableName + "." + columnName);
//									System.out.println(tableInfo.getEntityInfo().getEjbName());
//									System.out.println("<PortConfiguration>");
//									System.out.println("\t<Table tableName=\"" + tableName + "\">");
//									System.out.println("\t\t<Column columnName=\"" + columnName + "\" type=\"" + type + "\" length=\"" + length + "\" nullable=\"" + nullable + "\"/>");
//									System.out.println("\t</Table>");
//									System.out.println("</PortConfiguration>");
//								}
//								else if (columnInfo.getNullable() != nullable && !"OPTCOUNTER".equals(columnName)) {
//									System.out.println("nullable not equal " + nullable + " " + columnInfo.getNullable() + " " + tableName + "." + columnName);
//									System.out.println(tableInfo.getEntityInfo().getEjbName());
//									System.out.println("<PortConfiguration>");
//									System.out.println("\t<Table tableName=\"" + tableName + "\">");
//									System.out.println("\t\t<Column columnName=\"" + columnName + "\" nullable=\"" + nullable + "\"/>");
//									System.out.println("\t</Table>");
//									System.out.println("</PortConfiguration>");
//								}
//							}
//						}
//					}
//					if (nodeName.equals("imported")) {
//						String parentTableName = element.getAttribute("fromtable");
//						TableInfo parentTableInfo = iApplicationInfo.getTableInfo(parentTableName);
//						if (parentTableInfo != null && parentTableInfo.getModuleInfo() == tableInfo.getModuleInfo() && parentTableInfo.getEntityInfo() != null) {
//							String columnName = element.getAttribute("column");
//							ColumnInfo columnInfo = tableInfo.getColumnInfoByName(columnName);
//							String referencedColumnName = element.getAttribute("fromcolumn");
//							if ("LANGUAGE".equals(parentTableName) && "LANGUAGE_ID".equals(referencedColumnName) && tableInfo.getEntityInfo().getEjbName().contains("Desc")) {
//							}
//							else if ("STOREENT".equals(parentTableName) && "STOREENT_ID".equals(referencedColumnName)) {
//							}
//							else if ("STORE".equals(parentTableName) && "STORE_ID".equals(referencedColumnName)) {
//							}
//							else if ("MEMBER".equals(parentTableName) && "MEMBER_ID".equals(referencedColumnName)) {
//							}
//							else {
//								Set<ForeignKeyInfo> foreignKeys = tableInfo.getForeignKeys();
//								boolean found = false;
//								for (ForeignKeyInfo foreignKeyInfo : foreignKeys) {
//									List<ColumnInfo> memberColumns = foreignKeyInfo.getMemberColumns();
//									for (ColumnInfo memberColumn : memberColumns) {
//										if (columnInfo == memberColumn) {
//											found = true;
//											break;
//										}
//									}
//									if (found) {
//										break;
//									}
//								}
//								if (!found) {
//									if (parentTableInfo.getPrimaryKeyColumns().size() > 1) {
//										System.out.println(tableInfo.getEntityInfo().getEjbName() + ".xml");
//										System.out.println("foreign key to table with multi column primary key " + tableName + "." + columnName + " " + parentTableName + "." + referencedColumnName + " " + tableInfo.getModuleInfo().getJavaProject().getElementName());
//									}
//									else {
//										File newConfigurationFile = new File(iPortConfigurationFile.getParentFile(), tableInfo.getEntityInfo().getEjbName() + ".xml");
//										if (newConfigurationFile.exists()) {
//											//System.out.println("missing foreign key " + tableName + "." + columnName + " " + parentTableName + "." + referencedColumnName + " " + tableInfo.getModuleInfo().getJavaProject().getElementName());
//											System.out.println(tableInfo.getEntityInfo().getEjbName() + ".xml");
//											//System.out.println("<PortConfiguration>");
//											//System.out.println("\t<Table tableName=\"" + tableName + "\">");
//											System.out.println("\t\t<ForeignKey parentTableName=\"" + parentTableName + "\">");
//											System.out.println("\t\t\t<Member columnName=\"" + columnName + "\" referencedColumnName=\"" + referencedColumnName + "\"/>");
//											System.out.println("\t\t</ForeignKey>");
//											//System.out.println("\t</Table>");
//											//System.out.println("</PortConfiguration>");
//										}
//										else {
//											try {
//												FileWriter fileWriter = new FileWriter(newConfigurationFile);
//												fileWriter.write("<PortConfiguration>\r\n");
//												fileWriter.write("\t<Table tableName=\"" + tableName + "\">\r\n");
//												fileWriter.write("\t\t<ForeignKey parentTableName=\"" + parentTableName + "\">\r\n");
//												fileWriter.write("\t\t\t<Member columnName=\"" + columnName + "\" referencedColumnName=\"" + referencedColumnName + "\"/>\r\n");
//												fileWriter.write("\t\t</ForeignKey>\r\n");
//												fileWriter.write("\t</Table>\r\n");
//												fileWriter.write("</PortConfiguration>");
//												fileWriter.close();
//											}
//											catch (IOException e) {
//												e.printStackTrace();
//											}
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//	}
}
