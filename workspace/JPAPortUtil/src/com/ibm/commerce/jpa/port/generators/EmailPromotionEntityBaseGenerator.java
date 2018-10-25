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

public class EmailPromotionEntityBaseGenerator {
	public static final String TYPE_NAME = "com.ibm.commerce.emarketing.objimpl.EmailPromotionJPAEntityBase";

	public static class UnsentIntegerFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "UNSENT_INTEGER";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
			sb.append("\r\n\tpublic static final Integer UNSENT_INTEGER = com.ibm.commerce.base.helpers.Neww.integerr(0);\r\n");
		}
	}

	public static class SentIntegerFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "SENT_INTEGER";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
			sb.append("\r\n\tpublic static final Integer SENT_INTEGER = com.ibm.commerce.base.helpers.Neww.integerr(1);\r\n");
		}
	}

	public static class DeletedIntegerFieldGenerator implements FieldGenerator {
		public String getFieldName() {
			return "DELETED_INTEGER";
		}
		
		public void appendField(StringBuilder sb, EntityInfo entityInfo) {
			sb.append("\r\n\tpublic static final Integer DELETED_INTEGER = com.ibm.commerce.base.helpers.Neww.integerr(2);\r\n");
		}
	}

}
