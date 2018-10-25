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

public class ManagedFileEntityBaseGenerator {
	public static final String TYPE_NAME = "com.ibm.commerce.context.content.resources.file.objimpl.ManagedFileJPAEntityBase";
	
	public static class ContextFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "iContext";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class ConstantSmallFileEjbNameFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "CONSTANT_SMALL_FILE_EJB_NAME";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class ConstantLargeFileEjbNameFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "CONSTANT_LARGE_FILE_EJB_NAME";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class SmallFileHomeFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "iSmallFileHome";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class LargeFileHomeFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "iLargeFileHome";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class SmallFileTableNameFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "SMALL_FILE_TABLE_NAME";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class LargeFileTableNameFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "LARGE_FILE_TABLE_NAME";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class SmallFileBlobColumnNameFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "SMALL_FILE_BLOB_COLUMN_NAME";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class LargeFileBlobColumnNameFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "LARGE_FILE_BLOB_COLUMN_NAME";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class SmallFileWhereClauseFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "SMALL_FILE_WHERE_CLAUSE";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class LargeFileWhereClauseFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "LARGE_FILE_WHERE_CLAUSE";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class GetContextMethodGenerator implements MethodGenerator {
		public String getMethodKey() {
			return "getContext";
		}

		public void appendMethod(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class GetSmallFileHomeMethodGenerator implements MethodGenerator {
		public String getMethodKey() {
			return "getSmallFileHome";
		}
		
		public void appendMethod(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class GetLargeFileHomeMethodGenerator implements MethodGenerator {
		public String getMethodKey() {
			return "getLargeFileHome";
		}
		
		public void appendMethod(StringBuilder sb, EntityInfo entityInfo) {
		}
	}
	
	public static class GetFileMethodGenerator implements MethodGenerator {
		public String getMethodKey() {
			return "getFile+java.lang.String";
		}
		
		public void appendMethod(StringBuilder sb, EntityInfo entityInfo) {
			sb.append("\r\n\t/**\r\n");
			sb.append("\t * Returns the file contents associated with the managed file for the specified workspace.\r\n");
			sb.append("\t * @param contentWorkspace  The workspace to look for.\r\n");
			sb.append("\t * @return  The file contents.\r\n");
			sb.append("\t */\r\n");
			sb.append("\tpublic byte[] getFile(String contentWorkspace) throws javax.persistence.EntityNotFoundException {\r\n");
			sb.append("\t\tfinal String METHODNAME = \"getFile(String contentWorkspace)\";\r\n");
			sb.append("\t\tif (WcContentTraceLogger.isLoggableEntryExit()) {\r\n");
			sb.append("\t\t\tObject[] obj = { contentWorkspace };\r\n");
			sb.append("\t\t\tWcContentTraceLogger.entry(CLASSNAME, METHODNAME, obj);\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\tif (getStorageType().intValue() == -1) {\r\n");
			sb.append("\t\t\tif (WcContentTraceLogger.isLoggableEntryExit()) {\r\n");
			sb.append("\t\t\t\tWcContentTraceLogger.exit(CLASSNAME, METHODNAME, null);\r\n");
			sb.append("\t\t\t}\r\n");
			sb.append("\t\t\treturn null;\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\tif (contentWorkspace == null || contentWorkspace.equals(\"\")) {\r\n");
			sb.append("\t\t\tcontentWorkspace = ECManagedFileConstants.EC_MANAGED_FILE_DEFAULT_BASE;\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\tbyte[] file = null;\r\n");
			sb.append("\t\tif (getStorageType().intValue() == 1) {\r\n");
			sb.append("\t\t\tcom.ibm.commerce.context.content.resources.file.objects.ManagedLargeFileJPAAccessBean fileAB = new com.ibm.commerce.context.content.resources.file.objects.ManagedLargeFileJPAAccessBean();\r\n");
			sb.append("\t\t\tfileAB.setInitKey_fileId(getFileId());\r\n");
			sb.append("\t\t\tfileAB.setInitKey_contentWorkspace(contentWorkspace);\r\n");
			sb.append("\t\t\tfile = fileAB.getFile();\r\n");
			sb.append("\t\t}\r\n\t\telse {\r\n");
			sb.append("\t\t\tcom.ibm.commerce.context.content.resources.file.objects.ManagedSmallFileJPAAccessBean fileAB = new com.ibm.commerce.context.content.resources.file.objects.ManagedSmallFileJPAAccessBean();\r\n");
			sb.append("\t\t\tfileAB.setInitKey_fileId(getFileId());\r\n");
			sb.append("\t\t\tfileAB.setInitKey_contentWorkspace(contentWorkspace);\r\n");
			sb.append("\t\t\tfile = fileAB.getFile();\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\tif (WcContentTraceLogger.isLoggableEntryExit()) {\r\n");
			sb.append("\t\t\tif (file == null) {\r\n");
			sb.append("\t\t\t\tWcContentTraceLogger.exit(CLASSNAME, METHODNAME, null);\r\n");
			sb.append("\t\t\t}\r\n");
			sb.append("\t\t\telse {\r\n");
			sb.append("\t\t\t\tWcContentTraceLogger.exit(CLASSNAME, METHODNAME);\r\n");
			sb.append("\t\t\t}\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\treturn file;\r\n");
			sb.append("\t}\r\n");
		}
	}
	
	public static class SetFileMethodGenerator implements MethodGenerator {
		public String getMethodKey() {
			return "setFile+byte[]+java.lang.String";
		}
		
		public void appendMethod(StringBuilder sb, EntityInfo entityInfo) {
			sb.append("\r\n\t/**\r\n");
			sb.append("\t * Sets the file contents for the specified workspace.\r\n");
			sb.append("\t * @param file  The file contents.\r\n");
			sb.append("\t * @param contentWorkspace  The content workspace name.\r\n");
			sb.append("\t */\r\n");
			sb.append("\tpublic void setFile(byte[] file, String contentWorkspace) {\r\n");
			sb.append("\t\tfinal String METHODNAME = \"setFile(byte[] file, String contentWorkspace)\";\r\n");
			sb.append("\t\tif (WcContentTraceLogger.isLoggableEntryExit()) {\r\n");
			sb.append("\t\t\tObject[] obj = { \"<<byte[]>>\", contentWorkspace };\r\n");
			sb.append("\t\t\tWcContentTraceLogger.entry(CLASSNAME, METHODNAME, obj);\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\tresetMetaDataForChangedFileContents();\r\n");
			sb.append("\t\tif (contentWorkspace == null || contentWorkspace.equals(\"\")) {\r\n");
			sb.append("\t\t\tcontentWorkspace = ECManagedFileConstants.EC_MANAGED_FILE_DEFAULT_BASE;\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\tif (file == null) {\r\n");
			sb.append("\t\t\tif (getStorageType() != null) {\r\n");
			sb.append("\t\t\t\tif (getStorageType().intValue() == 1) {\r\n");
			sb.append("\t\t\t\t\tcom.ibm.commerce.context.content.resources.file.objects.ManagedLargeFileJPAAccessBean fileAB = new com.ibm.commerce.context.content.resources.file.objects.ManagedLargeFileJPAAccessBean();\r\n");
			sb.append("\t\t\t\t\tfileAB.setInitKey_fileId(getFileId());\r\n");
			sb.append("\t\t\t\t\tfileAB.setInitKey_contentWorkspace(contentWorkspace);\r\n");
			sb.append("\t\t\t\t\tfileAB.remove();\r\n");
			sb.append("\t\t\t\t}\r\n");
			sb.append("\t\t\t\telse if (getStorageType().intValue() == 0) {\r\n");
			sb.append("\t\t\t\t\tcom.ibm.commerce.context.content.resources.file.objects.ManagedSmallFileJPAAccessBean fileAB = new com.ibm.commerce.context.content.resources.file.objects.ManagedSmallFileJPAAccessBean();\r\n");
			sb.append("\t\t\t\t\tfileAB.setInitKey_fileId(getFileId());\r\n");
			sb.append("\t\t\t\t\tfileAB.setInitKey_contentWorkspace(contentWorkspace);\r\n");
			sb.append("\t\t\t\t\tfileAB.remove();\r\n");
			sb.append("\t\t\t\t}\r\n");
			sb.append("\t\t\t}\r\n");
			sb.append("\t\t\tsetStorageType(DEFAULT_STORAGE_TYPE);\r\n");
			sb.append("\t\t\tif (WcContentTraceLogger.isLoggableEntryExit()) {\r\n");
			sb.append("\t\t\t\tWcContentTraceLogger.exit(CLASSNAME, METHODNAME, null);\r\n");
			sb.append("\t\t\t}\r\n");
			sb.append("\t\t\tsetFileSize(LONG_ZERO);\r\n");
			sb.append("\t\t\treturn;\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\tboolean create = false;\r\n");
			sb.append("\t\tif (getStorageType() == null) {\r\n");
			sb.append("\t\t\tcreate = true;\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\telse if (getStorageType().intValue() == 0) {\r\n");
			sb.append("\t\t\tcom.ibm.commerce.context.content.resources.file.objects.ManagedSmallFileJPAAccessBean fileAB = new com.ibm.commerce.context.content.resources.file.objects.ManagedSmallFileJPAAccessBean();\r\n");
			sb.append("\t\t\tfileAB.setInitKey_fileId(getFileId());\r\n");
			sb.append("\t\t\tfileAB.setInitKey_contentWorkspace(contentWorkspace);\r\n");
			sb.append("\t\t\tif (file.length > MAX_SMALL_FILE_SIZE) {\r\n");
			sb.append("\t\t\t\tcreate = true;\r\n");
			sb.append("\t\t\t\tfileAB.remove();\r\n");
			sb.append("\t\t\t}\r\n");
			sb.append("\t\t\telse {\r\n");
			sb.append("\t\t\t\tfileAB.setFile(file);\r\n");
			sb.append("\t\t\t\tsetFileSize(new Long(file.length));\r\n");
			sb.append("\t\t\t}\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\telse if (getStorageType().intValue() == 1) {\r\n");
			sb.append("\t\t\tcom.ibm.commerce.context.content.resources.file.objects.ManagedLargeFileJPAAccessBean fileAB = new com.ibm.commerce.context.content.resources.file.objects.ManagedLargeFileJPAAccessBean();\r\n");
			sb.append("\t\t\tfileAB.setInitKey_fileId(getFileId());\r\n");
			sb.append("\t\t\tfileAB.setInitKey_contentWorkspace(contentWorkspace);\r\n");
			sb.append("\t\t\tif (file.length <= MAX_SMALL_FILE_SIZE) {\r\n");
			sb.append("\t\t\t\tcreate = true;\r\n");
			sb.append("\t\t\t\tfileAB.remove();\r\n");
			sb.append("\t\t\t}\r\n");
			sb.append("\t\t\telse {\r\n");
			sb.append("\t\t\t\tfileAB.setFile(file);\r\n");
			sb.append("\t\t\t\tsetFileSize(new Long(file.length));\r\n");
			sb.append("\t\t\t}\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\tif (create) {\r\n");
			sb.append("\t\t\tif (file.length > MAX_SMALL_FILE_SIZE) {\r\n");
			sb.append("\t\t\t\tsetStorageType(SHORT_ONE);\r\n");
			sb.append("\t\t\t\tsetFileSize(new Long(file.length));\r\n");
			sb.append("\t\t\t\tcom.ibm.commerce.context.content.resources.file.objects.ManagedLargeFileJPAAccessBean fileAB = new com.ibm.commerce.context.content.resources.file.objects.ManagedLargeFileJPAAccessBean(getFileId(), contentWorkspace, null, file);\r\n");
			sb.append("\t\t\t}\r\n");
			sb.append("\t\t\telse {\r\n");
			sb.append("\t\t\t\tsetStorageType(SHORT_ONE);\r\n");
			sb.append("\t\t\t\tsetFileSize(new Long(file.length));\r\n");
			sb.append("\t\t\t\tcom.ibm.commerce.context.content.resources.file.objects.ManagedSmallFileJPAAccessBean fileAB = new com.ibm.commerce.context.content.resources.file.objects.ManagedSmallFileJPAAccessBean(getFileId(), contentWorkspace, null, file);\r\n");
			sb.append("\t\t\t}\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t\tif (WcContentTraceLogger.isLoggableEntryExit()) {\r\n");
			sb.append("\t\t\tWcContentTraceLogger.exit(CLASSNAME, METHODNAME, new Boolean(create));\r\n");
			sb.append("\t\t}\r\n");
			sb.append("\t}\r\n");
		}
	}
}
