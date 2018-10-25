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

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.ibm.commerce.jpa.port.generators.PortVisitor;
import com.ibm.commerce.jpa.port.info.AccessBeanInfo.NullConstructorParameter;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.KeyClassConstructorInfo;
import com.ibm.commerce.jpa.port.info.RelatedEntityInfo;

public class FinderResultCacheUtil {
	private static final String ABSTRACT_FINDER_RESULT_CACHE = "com.ibm.commerce.dynacache.commands.AbstractFinderResultCache";
	private static final String NEW_ATTACHMENT_RELATION_USAGE_ACCESS_BEAN = "newAttachmentRelationUsageAccessBean";
	private static final String NEW_ATTACHMENT_RELATION_USAGE_DESCRIPTION_ACCESS_BEAN = "newAttachmentRelationUsageDescriptionAccessBean";
	private static final String NEW_BUSINESS_POLICY_ACCESS_BEAN = "newBusinessPolicyAccessBean";
	private static final String NEW_CALCULATION_CODE_ACCESS_BEAN = "newCalculationCodeAccessBean";
	private static final String NEW_CALCULATION_CODE_DESCRIPTION_ACCESS_BEAN = "newCalculationCodeDescriptionAccessBean";
	private static final String NEW_CALCULATION_CODE_PROMOTION_ACCESS_BEAN = "newCalculationCodePromotionAccessBean";
	private static final String NEW_CALCULATION_USAGE_ACCESS_BEAN = "newCalculationUsageAccessBean";
	private static final String NEW_CATALOG_ACCESS_BEAN = "newCatalogAccessBean";
	private static final String NEW_CATALOG_DESCRIPTION_ACCESS_BEAN = "newCatalogDescriptionAccessBean";
	private static final String NEW_CATALOG_ENTRY_ACCESS_BEAN = "newCatalogEntryAccessBean";
	private static final String NEW_CATALOG_ENTRY_DESCRIPTION_ACCESS_BEAN = "newCatalogEntryDescriptionAccessBean";
	private static final String NEW_CATALOG_ENTRY_SHIPPING_ACCESS_BEAN = "newCatalogEntryShippingAccessBean";
	private static final String NEW_CATALOG_GROUP_ACCESS_BEAN = "newCatalogGroupAccessBean";
	private static final String NEW_CATALOG_GROUP_CATALOG_ENTRY_RELATION_ACCESS_BEAN = "newCatalogGroupCatalogEntryRelationAccessBean";
	private static final String NEW_CATALOG_GROUP_DESCRIPTION_ACCESS_BEAN = "newCatalogGroupDescriptionAccessBean";
	private static final String NEW_CATALOG_GROUP_RELATION_ACCESS_BEAN = "newCatalogGroupRelationAccessBean";
	private static final String NEW_COUNTRY_ACCESS_BEAN = "newCountryAccessBean";
	private static final String NEW_CURRENCY_ACCESS_BEAN = "newCurrencyAccessBean";
	private static final String NEW_CURRENCY_DESCRIPTION_ACCESS_BEAN = "newCurrencyDescriptionAccessBean";
	private static final String NEW_DEMOGRAPHICS_ACCESS_BEAN = "newDemographicsAccessBean";
	private static final String NEW_EXTENDED_TERM_ACCESS_BEAN = "newExtendedTermConditionAccessBean";
	private static final String NEW_FULFILLMENT_CENTER_ACCESS_BEAN = "newFulfillmentCenterAccessBean";
	private static final String NEW_ITEM_ACCESS_BEAN = "newItemAccessBean";
	private static final String NEW_LANGUAGE_ACCESS_BEAN = "newLanguageAccessBean";
	private static final String NEW_LANGUAGE_DESCRIPTION_ACCESS_BEAN = "newLanguageDescriptionAccessBean";
	private static final String NEW_LIST_PRICE_ACCESS_BEAN = "newListPriceAccessBean";
	private static final String NEW_MEMBER_ACCESS_BEAN = "newMemberAccessBean";
	private static final String NEW_MEMBER_GROUP_ACCESS_BEAN = "newMemberGroupAccessBean";
	private static final String NEW_MEMBER_GROUP_MEMBER_ACCESS_BEAN = "newMemberGroupMemberAccessBean";
	private static final String NEW_MEMBER_RELATIONSHIPS_ACCESS_BEAN = "newMemberRelationshipsAccessBean";
	private static final String NEW_MEMBER_ROLE_ACCESS_BEAN = "newMemberRoleAccessBean";
	private static final String NEW_OFFER_ACCESS_BEAN = "newOfferAccessBean";
	private static final String NEW_OFFER_DESCRIPTION_ACCESS_BEAN = "newOfferDescriptionAccessBean";
	private static final String NEW_OFFER_PRICE_ACCESS_BEAN = "newOfferPriceAccessBean";
	private static final String NEW_ORGANIZATION_ACCESS_BEAN = "newOrganizationAccessBean";
	private static final String NEW_POLICY_ACCOUNT_ACCESS_BEAN = "newPolicyAccountAccessBean";
	private static final String NEW_POLICY_ACCOUNT_LOCKOUT_ACCESS_BEAN = "newPolicyAccountLockoutAccessBean";
	private static final String NEW_POLICY_ACCOUNT_DESCRIPTION_ACCESS_BEAN = "newPolicyDescriptionAccessBean";
	private static final String NEW_POLICY_PASSWORD_ACCESS_BEAN = "newPolicyPasswordAccessBean";
	private static final String NEW_PRODUCT_ACCESS_BEAN = "newProductAccessBean";
	private static final String NEW_RELATED_CATALOG_ENTRY_ACCESS_BEAN = "newRelatedCatalogEntryAccessBean";
	private static final String NEW_ROLE_ACCESS_BEAN = "newRoleAccessBean";
	private static final String NEW_SHIPPING_MODE_ACCESS_BEAN = "newShippingModeAccessBean";
	private static final String NEW_SHIPPING_MODE_DESCRIPTION_ACCESS_BEAN = "newShippingModeDescriptionAccessBean";
	private static final String NEW_STATE_PROVINCE_ACCESS_BEAN = "newStateProvinceAccessBean";
	private static final String NEW_STORE_ENTITY_DESCRIPTION_ACCESS_BEAN = "newStoreEntityDescriptionAccessBean";
	private static final String NEW_STORE_TRANS_ACCESS_BEAN = "newStoreTransAccessBean";
	private static final String NEW_SUPPORTED_LANGUAGE_ACCESS_BEAN = "newSupportedLanguageAccessBean";
	private static final String NEW_TICKLER_ACCESS_BEAN = "newTicklerAccessBean";
	private static final String NEW_USER_ACCESS_BEAN = "newUserAccessBean";
	private static final String NEW_USER_REGISTRY_ACCESS_BEAN = "newUserRegistryAccessBean";
	//caiduan-resolve the compilation errors after "Update Entities Ref"
	private static final String NEW_CONTRACT_ACCESS_BEAN = "newContractAccessBean";
	private static final String NEW_TAX_CATEGORY_ACCESS_BEAN = "newTaxCategoryAccessBean";
	private static final String NEW_TAX_CATEGORY_DES_ACCESS_BEAN = "newTaxCategoryDescriptionAccessBean";
	private static final String NEW_TRADING_DES_ACCESS_BEAN = "newTradingDescriptionAccessBean";	
	private static final String NEW_TERM_CONDITION_ACCESS_BEAN = "newTermConditionAccessBean";
	//caiduan-For Bumblebee-mod-0309
	private static final String NEW_SHIPPINGARRANGEMENT_ACCESS_BEAN = "newShippingArrangementAccessBean";
	
	
	private static Set<String> NEW_ACCESS_BEAN_METHODS;
	static {
		NEW_ACCESS_BEAN_METHODS = new HashSet<String>();
		NEW_ACCESS_BEAN_METHODS.add(NEW_ATTACHMENT_RELATION_USAGE_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_ATTACHMENT_RELATION_USAGE_DESCRIPTION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_BUSINESS_POLICY_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CALCULATION_CODE_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CALCULATION_CODE_DESCRIPTION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CALCULATION_CODE_PROMOTION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CALCULATION_USAGE_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CATALOG_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CATALOG_DESCRIPTION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CATALOG_ENTRY_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CATALOG_ENTRY_DESCRIPTION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CATALOG_ENTRY_SHIPPING_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CATALOG_GROUP_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CATALOG_GROUP_CATALOG_ENTRY_RELATION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CATALOG_GROUP_DESCRIPTION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CATALOG_GROUP_RELATION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_COUNTRY_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CURRENCY_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_CURRENCY_DESCRIPTION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_DEMOGRAPHICS_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_EXTENDED_TERM_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_FULFILLMENT_CENTER_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_ITEM_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_LANGUAGE_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_LANGUAGE_DESCRIPTION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_LIST_PRICE_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_MEMBER_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_MEMBER_GROUP_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_MEMBER_GROUP_MEMBER_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_MEMBER_RELATIONSHIPS_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_MEMBER_ROLE_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_OFFER_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_OFFER_DESCRIPTION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_OFFER_PRICE_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_ORGANIZATION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_POLICY_ACCOUNT_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_POLICY_ACCOUNT_LOCKOUT_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_POLICY_ACCOUNT_DESCRIPTION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_POLICY_PASSWORD_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_PRODUCT_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_RELATED_CATALOG_ENTRY_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_ROLE_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_SHIPPING_MODE_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_SHIPPING_MODE_DESCRIPTION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_STATE_PROVINCE_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_STORE_ENTITY_DESCRIPTION_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_STORE_TRANS_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_SUPPORTED_LANGUAGE_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_TICKLER_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_USER_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_USER_REGISTRY_ACCESS_BEAN);
		//caiduan-resolve the compilation errors after "Update Entities Ref"
		NEW_ACCESS_BEAN_METHODS.add(NEW_CONTRACT_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_TAX_CATEGORY_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_TAX_CATEGORY_DES_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_TRADING_DES_ACCESS_BEAN);
		NEW_ACCESS_BEAN_METHODS.add(NEW_TERM_CONDITION_ACCESS_BEAN);
		//caiduan-For Bumblebee-mod-0309
		System.out.println("[Before=]" + NEW_ACCESS_BEAN_METHODS);
		NEW_ACCESS_BEAN_METHODS.add(NEW_SHIPPINGARRANGEMENT_ACCESS_BEAN);	
		System.out.println("[After=]" + NEW_ACCESS_BEAN_METHODS);
		
	}
	private static final String ATTACHMENT_RELATION_USAGE_CACHE = "com.ibm.commerce.attachment.beansrc.AttachmentRelationUsageCache";
	private static final String ATTACHMENT_RELATION_USAGE_ACCESS_BEAN = "com.ibm.commerce.attachment.objects.AttachmentRelationUsageAccessBean";
	private static final String ATTACHMENT_RELATION_USAGE_DESCRIPTION_CACHE = "com.ibm.commerce.attachment.beansrc.AttachmentRelationUsageDescriptionCache";
	private static final String ATTACHMENT_RELATION_USAGE_DESCRIPTION_ACCESS_BEAN = "com.ibm.commerce.attachment.objects.AttachmentRelationUsageDescriptionAccessBean";
	private static final String BUSINESS_POLICY_CACHE = "com.ibm.commerce.contract.beansrc.BusinessPolicyCache";
	private static final String BUSINESS_POLICY_ACCESS_BEAN = "com.ibm.commerce.contract.objects.BusinessPolicyAccessBean";
	private static final String CALCULATION_CODE_CACHE = "com.ibm.commerce.fulfillment.objsrc.CalculationCodeCache";
	private static final String CALCULATION_CODE_ACCESS_BEAN = "com.ibm.commerce.fulfillment.objects.CalculationCodeAccessBean";
	private static final String CALCULATION_CODE_DESCRIPTION_CACHE = "com.ibm.commerce.fulfillment.objsrc.CalculationCodeDescriptionCache";
	private static final String CALCULATION_CODE_DESCRIPTION_ACCESS_BEAN = "com.ibm.commerce.fulfillment.objects.CalculationCodeDescriptionAccessBean";
	private static final String CALCULATION_CODE_PROMOTION_CACHE = "com.ibm.commerce.tools.epromotion.objimpl.CalculationCodePromotionCache";
	private static final String CALCULATION_CODE_PROMOTION_ACCESS_BEAN = "com.ibm.commerce.tools.epromotion.objects.CalculationCodePromotionAccessBean";
	private static final String CALCULATION_USAGE_CACHE = "com.ibm.commerce.fulfillment.beansrc.CalculationUsageCache";
	private static final String CALCULATION_USAGE_ACCESS_BEAN = "com.ibm.commerce.fulfillment.objects.CalculationUsageAccessBean";
	private static final String CATALOG_CACHE = "com.ibm.commerce.catalog.objsrc.CatalogCache";
	private static final String CATALOG_ACCESS_BEAN = "com.ibm.commerce.catalog.objects.CatalogAccessBean";
	private static final String CATALOG_DESCRIPTION_CACHE = "com.ibm.commerce.catalog.objsrc.CatalogDescriptionCache";
	private static final String CATALOG_DESCRIPTION_ACCESS_BEAN = "com.ibm.commerce.catalog.objects.CatalogDescriptionAccessBean";
	private static final String CATALOG_ENTRY_CACHE = "com.ibm.commerce.catalog.objsrc.CatalogEntryCache";
	private static final String CATALOG_ENTRY_ACCESS_BEAN = "com.ibm.commerce.catalog.objects.CatalogEntryAccessBean";
	private static final String CATALOG_ENTRY_DESCRIPTION_CACHE = "com.ibm.commerce.catalog.objsrc.CatalogEntryDescriptionCache";
	private static final String CATALOG_ENTRY_DESCRIPTION_ACCESS_BEAN = "com.ibm.commerce.catalog.objects.CatalogEntryDescriptionAccessBean";
	private static final String CATALOG_ENTRY_SHIPPING_CACHE = "com.ibm.commerce.fulfillment.objsrc.CatalogEntryShippingCache";
	private static final String CATALOG_ENTRY_SHIPPING_ACCESS_BEAN = "com.ibm.commerce.fulfillment.objects.CatalogEntryShippingAccessBean";
	private static final String CATALOG_GROUP_CACHE = "com.ibm.commerce.catalog.objsrc.CatalogGroupCache";
	private static final String CATALOG_GROUP_ACCESS_BEAN = "com.ibm.commerce.catalog.objects.CatalogGroupAccessBean";
	private static final String CATALOG_GROUP_CATALOG_ENTRY_RELATION_CACHE = "com.ibm.commerce.catalog.beansrc.CatalogGroupCatalogEntryRelationCache";
	private static final String CATALOG_GROUP_CATALOG_ENTRY_RELATION_ACCESS_BEAN = "com.ibm.commerce.catalog.objects.CatalogGroupCatalogEntryRelationAccessBean";
	private static final String CATALOG_GROUP_DESCRIPTION_CACHE = "com.ibm.commerce.catalog.objsrc.CatalogGroupDescriptionCache";
	private static final String CATALOG_GROUP_DESCRIPTION_ACCESS_BEAN = "com.ibm.commerce.catalog.objects.CatalogGroupDescriptionAccessBean";
	private static final String CATALOG_GROUP_RELATION_CACHE = "com.ibm.commerce.catalog.beansrc.CatalogGroupRelationCache";
	private static final String CATALOG_GROUP_RELATION_ACCESS_BEAN = "com.ibm.commerce.catalog.objects.CatalogGroupRelationAccessBean";
	private static final String COUNTRY_CACHE = "com.ibm.commerce.taxation.objsrc.CountryCache";
	private static final String COUNTRY_ACCESS_BEAN = "com.ibm.commerce.taxation.objects.CountryAccessBean";
	private static final String CURRENCY_CACHE = "com.ibm.commerce.common.objsrc.CurrencyCache";
	private static final String CURRENCY_ACCESS_BEAN = "com.ibm.commerce.common.objects.CurrencyAccessBean";
	private static final String CURRENCY_DESCRIPTION_CACHE = "com.ibm.commerce.common.objsrc.CurrencyDescriptionCache";
	private static final String CURRENCY_DESCRIPTION_ACCESS_BEAN = "com.ibm.commerce.common.objects.CurrencyDescriptionAccessBean";
	private static final String DEMOGRAPHICS_CACHE = "com.ibm.commerce.user.objsrc.DemographicsCache";
	private static final String DEMOGRAPHICS_ACCESS_BEAN = "com.ibm.commerce.user.objects.DemographicsAccessBean";
	private static final String EXTENDED_TERM_CONDITION_CACHE = "com.ibm.commerce.contract.beansrc.ExtendedTermConditionCache";
	private static final String EXTENDED_TERM_CONDITION_ACCESS_BEAN = "com.ibm.commerce.contract.objects.ExtendedTermConditionAccessBean";
	private static final String FULFILLMENT_CENTER_CACHE = "com.ibm.commerce.fulfillment.objsrc.FulfillmentCenterCache";
	private static final String FULFILLMENT_CENTER_ACCESS_BEAN = "com.ibm.commerce.fulfillment.objects.FulfillmentCenterAccessBean";
	private static final String ITEM_CACHE = "com.ibm.commerce.catalog.objsrc.ItemCache";
	private static final String ITEM_ACCESS_BEAN = "com.ibm.commerce.catalog.objects.ItemAccessBean";
	private static final String LANGUAGE_CACHE = "com.ibm.commerce.common.objsrc.LanguageCache";
	private static final String LANGUAGE_ACCESS_BEAN = "com.ibm.commerce.common.objects.LanguageAccessBean";
	private static final String LANGUAGE_DESCRIPTION_CACHE = "com.ibm.commerce.common.objsrc.LanguageDescriptionCache";
	private static final String LANGUAGE_DESCRIPTION_ACCESS_BEAN = "com.ibm.commerce.common.objects.LanguageDescriptionAccessBean";
	private static final String LIST_PRICE_CACHE = "com.ibm.commerce.catalog.beansrc.ListPriceCache";
	private static final String LIST_PRICE_ACCESS_BEAN = "com.ibm.commerce.catalog.objects.ListPriceAccessBean";
	private static final String MEMBER_CACHE = "com.ibm.commerce.user.objsrc.MemberCache";
	private static final String MEMBER_ACCESS_BEAN = "com.ibm.commerce.user.objects.MemberAccessBean";
	private static final String MEMBER_GROUP_CACHE = "com.ibm.commerce.user.objsrc.MemberGroupCache";
	private static final String MEMBER_GROUP_ACCESS_BEAN = "com.ibm.commerce.user.objects.MemberGroupAccessBean";
	private static final String MEMBER_GROUP_MEMBER_CACHE = "com.ibm.commerce.user.objsrc.MemberGroupMemberCache";
	private static final String MEMBER_GROUP_MEMBER_ACCESS_BEAN = "com.ibm.commerce.user.objects.MemberGroupMemberAccessBean";
	private static final String MEMBER_RELATIONSHIPS_CACHE = "com.ibm.commerce.user.objsrc.MemberRelationshipsCache";
	private static final String MEMBER_RELATIONSHIPS_ACCESS_BEAN = "com.ibm.commerce.user.objects.MemberRelationshipsAccessBean";
	private static final String MEMBER_RELATIONSHIPS_EXTENDED_CACHE = "com.ibm.commerce.user.beansrc.MemberRelationshipsExtendedCache";
	private static final String MEMBER_RELATIONSHIPS_EXTENDED_ACCESS_BEAN = "com.ibm.commerce.user.objects.MemberRelationshipsAccessBean";
	private static final String MEMBER_ROLE_CACHE = "com.ibm.commerce.user.objsrc.MemberRoleCache";
	private static final String MEMBER_ROLE_ACCESS_BEAN = "com.ibm.commerce.user.objects.MemberRoleAccessBean";
	private static final String OFFER_CACHE = "com.ibm.commerce.order.objsrc.OfferCache";
	private static final String OFFER_EXTENDED_CACHE = "com.ibm.commerce.order.beansrc.OfferExtendedCache";
	private static final String OFFER_ACCESS_BEAN = "com.ibm.commerce.order.objects.OfferAccessBean";
	private static final String OFFER_DESCRIPTION_CACHE = "com.ibm.commerce.order.beansrc.OfferDescriptionCache";
	private static final String OFFER_DESCRIPTION_ACCESS_BEAN = "com.ibm.commerce.order.objects.OfferDescriptionAccessBean";
	private static final String OFFER_PRICE_CACHE = "com.ibm.commerce.order.objsrc.OfferPriceCache";
	private static final String OFFER_PRICE_EXTENDED_CACHE = "com.ibm.commerce.order.beansrc.OfferPriceExtendedCache";
	private static final String OFFER_PRICE_ACCESS_BEAN = "com.ibm.commerce.order.objects.OfferPriceAccessBean";
	private static final String ORGANIZATION_CACHE = "com.ibm.commerce.user.objsrc.OrganizationCache";
	private static final String ORGANIZATION_ACCESS_BEAN = "com.ibm.commerce.user.objects.OrganizationAccessBean";
	private static final String POLICY_ACCOUNT_CACHE = "com.ibm.commerce.user.beansrc.PolicyAccountCache";
	private static final String POLICY_ACCOUNT_ACCESS_BEAN = "com.ibm.commerce.user.objects.PolicyAccountAccessBean";
	private static final String POLICY_ACCOUNT_LOCKOUT_CACHE = "com.ibm.commerce.user.beansrc.PolicyAccountLockoutCache";
	private static final String POLICY_ACCOUNT_LOCKOUT_ACCESS_BEAN = "com.ibm.commerce.user.objects.PolicyAccountLockoutAccessBean";
	private static final String POLICY_DESCRIPTION_CACHE = "com.ibm.commerce.contract.beansrc.PolicyDescriptionCache";
	private static final String POLICY_DESCRIPTION_ACCESS_BEAN = "com.ibm.commerce.contract.objects.PolicyDescriptionAccessBean";
	private static final String POLICY_PASSWORD_CACHE = "com.ibm.commerce.user.beansrc.PolicyPasswordCache";
	private static final String POLICY_PASSWORD_ACCESS_BEAN = "com.ibm.commerce.user.objects.PolicyPasswordAccessBean";
	private static final String PRODUCT_CACHE = "com.ibm.commerce.catalog.objsrc.ProductCache";
	private static final String PRODUCT_ACCESS_BEAN = "com.ibm.commerce.catalog.objects.ProductAccessBean";
	private static final String RELATED_CATALOG_ENTRY_CACHE = "com.ibm.commerce.catalog.beansrc.RelatedCatalogEntryCache";
	private static final String RELATED_CATALOG_ENTRY_ACCESS_BEAN = "com.ibm.commerce.catalog.objects.RelatedCatalogEntryAccessBean";
	private static final String ROLE_CACHE = "com.ibm.commerce.user.objsrc.RoleCache";
	private static final String ROLE_ACCESS_BEAN = "com.ibm.commerce.user.objects.RoleAccessBean";
	private static final String SHIPPING_MODE_CACHE = "com.ibm.commerce.fulfillment.objsrc.ShippingModeCache";
	private static final String SHIPPING_MODE_ACCESS_BEAN = "com.ibm.commerce.fulfillment.objects.ShippingModeAccessBean";
	private static final String SHIPPING_MODE_DESCRIPTION_CACHE = "com.ibm.commerce.fulfillment.objsrc.ShippingModeDescriptionCache";
	private static final String SHIPPING_MODE_DESCRIPTION_ACCESS_BEAN = "com.ibm.commerce.fulfillment.objects.ShippingModeDescriptionAccessBean";
	private static final String STATE_PROVINCE_CACHE = "com.ibm.commerce.taxation.objsrc.StateProvinceCache";
	private static final String STATE_PROVINCE_ACCESS_BEAN = "com.ibm.commerce.taxation.objects.StateProvinceAccessBean";
	private static final String STORE_ENTITY_DESCRIPTION_CACHE = "com.ibm.commerce.common.objsrc.StoreEntityDescriptionCache";
	private static final String STORE_ENTITY_DESCRIPTION_ACCESS_BEAN = "com.ibm.commerce.common.objects.StoreEntityDescriptionAccessBean";
	private static final String STORE_TRANS_CACHE = "com.ibm.commerce.messaging.databeans.StoreTransCache";
	private static final String STORE_TRANS_ACCESS_BEAN = "com.ibm.commerce.messaging.objects.StoreTransAccessBean";
	private static final String SUPPORTED_LANGUAGE_CACHE = "com.ibm.commerce.common.objsrc.SupportedLanguageCache";
	private static final String SUPPORTED_LANGUAGE_EXTENDED_CACHE = "com.ibm.commerce.common.beansrc.SupportedLanguageExtendedCache";
	private static final String SUPPORTED_LANGUAGE_ACCESS_BEAN = "com.ibm.commerce.common.objects.SupportedLanguageAccessBean";
	private static final String TICKLER_CACHE = "com.ibm.commerce.tickler.beans.TicklerCache";
	private static final String TICKLER_ACCESS_BEAN = "com.ibm.commerce.tickler.objects.TicklerAccessBean";
	private static final String USER_CACHE = "com.ibm.commerce.user.objsrc.UserCache";
	private static final String USER_ACCESS_BEAN = "com.ibm.commerce.user.objects.UserAccessBean";
	private static final String USER_REGISTRY_CACHE = "com.ibm.commerce.user.objsrc.UserRegistryCache";
	private static final String USER_REGISTRY_ACCESS_BEAN = "com.ibm.commerce.user.objects.UserRegistryAccessBean";
	private static final String COLLECTION_SUFFIX = "Collection";
	private static final String LIST = "list";
	private static final String GET = "get";
	private static final String ACCESS_BEAN_SUFFIX = "AccessBean";
	private static final String GET_ENTITY = "getEntity";
	private static final Map<String, String> CACHE_UTIL_TO_ACCESSBEAN_MAP;
	
	//caiduan-Resolve Errors of "Generate Entities Reference"
	private static final String TRADING_DESCRIPTION_CACHE = "com.ibm.commerce.contract.beansrc.TradingDescriptionCache";
	private static final String TRADING_DESCRIPTION_ACCESS_BEAN = "com.ibm.commerce.contract.objects.TradingDescriptionAccessBean";
	
	private static final String TAX_CATDES_CACHE = "com.ibm.commerce.taxation.beansrc.TaxCategoryDescriptionCache";
	private static final String TAX_CATDES_ACCESS_BEAN = "com.ibm.commerce.taxation.objects.TaxCategoryDescriptionAccessBean";
	
	//caiduan-Resolve compilation errors after "Update Entity Reference" of "myConvertFromCompactForm" method of JPAIndirectAttachmentCache
	private static final String CALCODE_EXTENDED_CACHE = "com.ibm.commerce.fulfillment.beans.CalculationCodeExtendedCache";
	private static final String CALCODE_ACCESS_BEAN = "com.ibm.commerce.fulfillment.objects.CalculationCodeAccessBean";
	
	
	static {
		CACHE_UTIL_TO_ACCESSBEAN_MAP = new HashMap<String, String>();
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(ATTACHMENT_RELATION_USAGE_CACHE, ATTACHMENT_RELATION_USAGE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(ATTACHMENT_RELATION_USAGE_DESCRIPTION_CACHE, ATTACHMENT_RELATION_USAGE_DESCRIPTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(BUSINESS_POLICY_CACHE, BUSINESS_POLICY_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CALCULATION_CODE_CACHE, CALCULATION_CODE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CALCULATION_CODE_DESCRIPTION_CACHE, CALCULATION_CODE_DESCRIPTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CALCULATION_CODE_PROMOTION_CACHE, CALCULATION_CODE_PROMOTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CALCULATION_USAGE_CACHE, CALCULATION_USAGE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CATALOG_CACHE, CATALOG_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CATALOG_DESCRIPTION_CACHE, CATALOG_DESCRIPTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CATALOG_ENTRY_CACHE, CATALOG_ENTRY_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CATALOG_ENTRY_DESCRIPTION_CACHE, CATALOG_ENTRY_DESCRIPTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CATALOG_ENTRY_SHIPPING_CACHE, CATALOG_ENTRY_SHIPPING_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CATALOG_GROUP_CACHE, CATALOG_GROUP_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CATALOG_GROUP_CATALOG_ENTRY_RELATION_CACHE, CATALOG_GROUP_CATALOG_ENTRY_RELATION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CATALOG_GROUP_DESCRIPTION_CACHE, CATALOG_GROUP_DESCRIPTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CATALOG_GROUP_RELATION_CACHE, CATALOG_GROUP_RELATION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(COUNTRY_CACHE, COUNTRY_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CURRENCY_CACHE, CURRENCY_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CURRENCY_DESCRIPTION_CACHE, CURRENCY_DESCRIPTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(DEMOGRAPHICS_CACHE, DEMOGRAPHICS_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(EXTENDED_TERM_CONDITION_CACHE, EXTENDED_TERM_CONDITION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(FULFILLMENT_CENTER_CACHE, FULFILLMENT_CENTER_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(ITEM_CACHE, ITEM_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(LANGUAGE_CACHE, LANGUAGE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(LANGUAGE_DESCRIPTION_CACHE, LANGUAGE_DESCRIPTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(LIST_PRICE_CACHE, LIST_PRICE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(MEMBER_CACHE, MEMBER_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(MEMBER_GROUP_CACHE, MEMBER_GROUP_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(MEMBER_GROUP_MEMBER_CACHE, MEMBER_GROUP_MEMBER_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(MEMBER_RELATIONSHIPS_CACHE, MEMBER_RELATIONSHIPS_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(MEMBER_RELATIONSHIPS_EXTENDED_CACHE, MEMBER_RELATIONSHIPS_EXTENDED_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(MEMBER_ROLE_CACHE, MEMBER_ROLE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(OFFER_CACHE, OFFER_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(OFFER_EXTENDED_CACHE, OFFER_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(OFFER_DESCRIPTION_CACHE, OFFER_DESCRIPTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(OFFER_PRICE_CACHE, OFFER_PRICE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(OFFER_PRICE_EXTENDED_CACHE, OFFER_PRICE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(ORGANIZATION_CACHE, ORGANIZATION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(POLICY_ACCOUNT_CACHE, POLICY_ACCOUNT_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(POLICY_ACCOUNT_LOCKOUT_CACHE, POLICY_ACCOUNT_LOCKOUT_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(POLICY_DESCRIPTION_CACHE, POLICY_DESCRIPTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(POLICY_PASSWORD_CACHE, POLICY_PASSWORD_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(PRODUCT_CACHE, PRODUCT_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(RELATED_CATALOG_ENTRY_CACHE, RELATED_CATALOG_ENTRY_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(ROLE_CACHE, ROLE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(SHIPPING_MODE_CACHE, SHIPPING_MODE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(SHIPPING_MODE_DESCRIPTION_CACHE, SHIPPING_MODE_DESCRIPTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(STATE_PROVINCE_CACHE, STATE_PROVINCE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(STORE_ENTITY_DESCRIPTION_CACHE, STORE_ENTITY_DESCRIPTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(STORE_TRANS_CACHE, STORE_TRANS_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(SUPPORTED_LANGUAGE_CACHE, SUPPORTED_LANGUAGE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(SUPPORTED_LANGUAGE_EXTENDED_CACHE, SUPPORTED_LANGUAGE_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(TICKLER_CACHE, TICKLER_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(USER_CACHE, USER_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(USER_REGISTRY_CACHE, USER_REGISTRY_ACCESS_BEAN);
		//caiduan-Resolve Errors of "Generate Entities Reference"
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(TRADING_DESCRIPTION_CACHE, TRADING_DESCRIPTION_ACCESS_BEAN);
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(TAX_CATDES_CACHE, TAX_CATDES_ACCESS_BEAN);		
		//caiduan-Resolve compilation errors after "Update Entity Reference" of "myConvertFromCompactForm" method of JPAIndirectAttachmentCache
		CACHE_UTIL_TO_ACCESSBEAN_MAP.put(CALCODE_EXTENDED_CACHE, CALCODE_ACCESS_BEAN);
	}
	private static final String COLLECTIONS = "java.util.Collections";
	private static final String FIND = "find";
	private static final String FIND_BY_PRIMARY_KEY = "findByPrimaryKey";
	private static final String SET_INIT_KEY = "setInitKey_";
	private static final String STRING = "java.lang.String";
	private static final String[] OFFER_PRICE_KEY_FIELD_NAMES = {"currency", "offerId"};
	private static final Map<String, String[]> CACHE_UTIL_TO_FIND_BY_PRIMARY_KEY_FIELDS_MAP;
	static {
		CACHE_UTIL_TO_FIND_BY_PRIMARY_KEY_FIELDS_MAP = new HashMap<String, String[]>();
		CACHE_UTIL_TO_FIND_BY_PRIMARY_KEY_FIELDS_MAP.put(OFFER_PRICE_CACHE, OFFER_PRICE_KEY_FIELD_NAMES);
	}
	private static final String INSTANTIATE_ENTITY = "instantiateEntity";
	private static final String SERVER_JDBC_HELPER_CACHE = "com.ibm.commerce.base.objects.ServerJDBCHelperCache";
	private static final String ABSTRACT_DISTRIBUTED_MAP_CACHE_CACHE = "com.ibm.commerce.dynacache.commands.AbstractDistributedMapCache.Cache";
	private static final String RETRIEVE_PRICES_CMD_IMPL_CACHE = "com.ibm.commerce.price.commands.RetrievePricesCmdImpl.Cache";
	private static final String LIST_MEMBER_GROUPS_FOR_USER_CMD_IMPL_CACHE = "com.ibm.commerce.membergroup.commands.ListMemberGroupsForUserCmdImpl.Cache";
	private static final Collection<String> EXEMPT_FINDER_RESULT_CACHE_CLASSES;
	static {
		EXEMPT_FINDER_RESULT_CACHE_CLASSES = new HashSet<String>();
		EXEMPT_FINDER_RESULT_CACHE_CLASSES.add(SERVER_JDBC_HELPER_CACHE);
		EXEMPT_FINDER_RESULT_CACHE_CLASSES.add(ABSTRACT_DISTRIBUTED_MAP_CACHE_CACHE);
		EXEMPT_FINDER_RESULT_CACHE_CLASSES.add(RETRIEVE_PRICES_CMD_IMPL_CACHE);
		EXEMPT_FINDER_RESULT_CACHE_CLASSES.add(LIST_MEMBER_GROUPS_FOR_USER_CMD_IMPL_CACHE);
	}
	private static final String USING_JDBC = "UsingJDBC";
	private static final Map<String, String> FIND_USING_JDBC_ACCESS_BEANS;
	static {
		FIND_USING_JDBC_ACCESS_BEANS = new HashMap<String, String>();
		FIND_USING_JDBC_ACCESS_BEANS.put("findByOffersAndCurrencyUsingJDBC", "com.ibm.commerce.order.helpers.OrderJDBCHelperAccessBean");
	}
	private static final String GET_NAME = "getName";
	
	public static boolean isFinderResultCacheUtil(ITypeBinding typeBinding) {
		boolean result = false;
		if (typeBinding != null && typeBinding.getSuperclass() != null) {
			if (ABSTRACT_FINDER_RESULT_CACHE.equals(typeBinding.getSuperclass().getQualifiedName()) && !EXEMPT_FINDER_RESULT_CACHE_CLASSES.contains(typeBinding.getQualifiedName())) {
				result = true;
			}
		}
		return result;
	}
	
	public static boolean isFinderResultCacheUtil(IType type) {
		boolean result = false;
		if (!EXEMPT_FINDER_RESULT_CACHE_CLASSES.contains(type.getFullyQualifiedName('.'))) {
			try {
				if (type.getSuperclassName() != null) {
					IType superClassType = JavaUtil.resolveType(type, type.getSuperclassName());
					if (superClassType == null) {
						System.out.println("unable to resolve type "+superClassType);
					}
					if (ABSTRACT_FINDER_RESULT_CACHE.equals(superClassType.getFullyQualifiedName('.'))) {
						result = true;
					}
				}
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	public static boolean isNewAccessBeanMethod(String methodName) {
		return NEW_ACCESS_BEAN_METHODS.contains(methodName);
	}
	
	public static boolean isFindAsCollectionMethod(String methodName) {
		return methodName.startsWith(FIND) && methodName.endsWith(COLLECTION_SUFFIX);
	}
	
	public static boolean replaceFindAsCollectionMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = false;
		//RelatedCatalogEntryCache.findByCatalogEntryAccessoryAndStoreCollection(anCatEntryId, anStoreId)
		String cacheUtil = methodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
		String accessBean = CACHE_UTIL_TO_ACCESSBEAN_MAP.get(cacheUtil);
		String jpaAccessBean = applicationInfo.getTypeMapping(accessBean);
		String finderMethodName = methodInvocation.getName().getIdentifier();
		if (finderMethodName.endsWith(COLLECTION_SUFFIX)) {
			finderMethodName = finderMethodName.substring(0, finderMethodName.lastIndexOf(COLLECTION_SUFFIX));
		}
//		else if (finderMethodName.endsWith(USING_JDBC)) {
//			finderMethodName = finderMethodName.substring(0, finderMethodName.length() - USING_JDBC.length());
//		}
		AST ast = methodInvocation.getAST();
		ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
		classInstanceCreation.setType(ast.newSimpleType(ast.newName(jpaAccessBean)));
		MethodInvocation finderMethodInvocation = methodInvocation.getAST().newMethodInvocation();
		finderMethodInvocation.setExpression(classInstanceCreation);
		finderMethodInvocation.setName(ast.newSimpleName(finderMethodName));
		@SuppressWarnings("unchecked")
		List<Expression> arguments = methodInvocation.arguments();
		@SuppressWarnings("unchecked")
		List<Expression> finderMethodArguments = finderMethodInvocation.arguments();
		while(arguments.size() > 0) {
			Expression argument = arguments.get(0);
			argument.accept(portVisitor);
			argument = arguments.get(0);
			argument.delete();
			finderMethodArguments.add(argument);
		}
		MethodInvocation collectionsListMethodInvocation = ast.newMethodInvocation();
		collectionsListMethodInvocation.setExpression(ast.newName(COLLECTIONS));
		collectionsListMethodInvocation.setName(ast.newSimpleName(LIST));
		@SuppressWarnings("unchecked")
		List<Expression> collectionsListArguments = collectionsListMethodInvocation.arguments();
		collectionsListArguments.add(finderMethodInvocation);
		portVisitor.replaceASTNode(methodInvocation, collectionsListMethodInvocation);
		return visitChildren;
	}
	
	public static boolean isFindUsingJDBCMethod(String methodName) {
		return FIND_USING_JDBC_ACCESS_BEANS.get(methodName) != null;
	}
	
	public static boolean replaceFindUsingJDBCMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = false;
		String finderMethodName = methodInvocation.getName().getIdentifier();
		String accessBean = FIND_USING_JDBC_ACCESS_BEANS.get(finderMethodName);
		String jpaAccessBean = applicationInfo.getTypeMapping(accessBean);
		if (jpaAccessBean == null) {
			jpaAccessBean = accessBean;
		}
		if (finderMethodName.endsWith(USING_JDBC)) {
			finderMethodName = finderMethodName.substring(0, finderMethodName.length() - USING_JDBC.length());
		}
		AST ast = methodInvocation.getAST();
		ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
		classInstanceCreation.setType(ast.newSimpleType(ast.newName(jpaAccessBean)));
		MethodInvocation finderMethodInvocation = methodInvocation.getAST().newMethodInvocation();
		finderMethodInvocation.setExpression(classInstanceCreation);
		finderMethodInvocation.setName(ast.newSimpleName(finderMethodName));
		@SuppressWarnings("unchecked")
		List<Expression> arguments = methodInvocation.arguments();
		@SuppressWarnings("unchecked")
		List<Expression> finderMethodArguments = finderMethodInvocation.arguments();
		while(arguments.size() > 0) {
			Expression argument = arguments.get(0);
			argument.accept(portVisitor);
			argument = arguments.get(0);
			argument.delete();
			finderMethodArguments.add(argument);
		}
		finderMethodArguments.add(ast.newNumberLiteral("100"));
		portVisitor.replaceASTNode(methodInvocation, finderMethodInvocation);
		return visitChildren;
	}
	
	public static boolean isGetAsCollectionMethod(String methodName) {
		return methodName.startsWith(GET) && methodName.endsWith(COLLECTION_SUFFIX);
	}
	
	public static boolean replaceGetAsCollectionMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = false;
		// UserCache.getMemberGroupsCollection(user)
		String getterMethodName = methodInvocation.getName().getIdentifier();
		getterMethodName = getterMethodName.substring(0, getterMethodName.lastIndexOf(COLLECTION_SUFFIX));
		AST ast = methodInvocation.getAST();
		@SuppressWarnings("unchecked")
		List<Expression> arguments = methodInvocation.arguments();
		Expression accessBeanExpression = arguments.get(0);
		accessBeanExpression.delete();
		MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
		getterMethodInvocation.setName(ast.newSimpleName(getterMethodName));
		getterMethodInvocation.setExpression(accessBeanExpression);
		MethodInvocation collectionsListMethodInvocation = ast.newMethodInvocation();
		collectionsListMethodInvocation.setExpression(ast.newName(COLLECTIONS));
		collectionsListMethodInvocation.setName(ast.newSimpleName(LIST));
		@SuppressWarnings("unchecked")
		List<Expression> collectionsListArguments = collectionsListMethodInvocation.arguments();
		collectionsListArguments.add(getterMethodInvocation);
		portVisitor.replaceASTNode(methodInvocation, collectionsListMethodInvocation);
		return visitChildren;
	}
	
	public static boolean isFinderMethod(String methodName) {
		return methodName.startsWith(FIND) && !methodName.equals(FIND_BY_PRIMARY_KEY);
	}
	
	public static boolean replaceFinderMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = false;
		String cacheUtil = methodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
		String accessBean = CACHE_UTIL_TO_ACCESSBEAN_MAP.get(cacheUtil);
		String jpaAccessBean = applicationInfo.getTypeMapping(accessBean);
		if (jpaAccessBean == null) {
			System.out.println("no mapping for "+accessBean+" cachUtil="+cacheUtil);
		}
		String finderMethodName = methodInvocation.getName().getIdentifier();
//		if (finderMethodName.endsWith(USING_JDBC)) {
//			finderMethodName = finderMethodName.substring(0, finderMethodName.length() - USING_JDBC.length());
//		}
		//caiduan-resolve the compilation errors after "Update Entities Ref" in the JPARelationshipEvaluator.java
		Expression finderCallderName = methodInvocation.getExpression();
		if (finderMethodName.equals("findDescendantOrganizationIds")) {
		finderMethodName = "findDescendantOrganizations";
		}
		AST ast = methodInvocation.getAST();
		Expression newMethodExpression = null;
		@SuppressWarnings("unchecked")
		List<Expression> arguments = methodInvocation.arguments();
		if (arguments.size() > 0 && arguments.get(0).resolveTypeBinding() != null) {
			ITypeBinding typeBinding = arguments.get(0).resolveTypeBinding();
			if (typeBinding.getQualifiedName().equals(accessBean) || applicationInfo.isAccessBeanSubclass(typeBinding.getQualifiedName())) {
				arguments.get(0).accept(portVisitor);
				newMethodExpression = arguments.get(0);
				newMethodExpression.delete();
			}
		}
		if (newMethodExpression == null && jpaAccessBean != null) {
			ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
			classInstanceCreation.setType(ast.newSimpleType(ast.newName(jpaAccessBean)));
			newMethodExpression = classInstanceCreation;
		} else {
			System.out.println("TEST: " + accessBean);
		}

		MethodInvocation finderMethodInvocation = methodInvocation.getAST().newMethodInvocation();
		finderMethodInvocation.setExpression(newMethodExpression);
		finderMethodInvocation.setName(ast.newSimpleName(finderMethodName));
		@SuppressWarnings("unchecked")
		List<Expression> finderMethodArguments = finderMethodInvocation.arguments();
		while(arguments.size() > 0) {
			Expression argument = arguments.get(0);
			argument.delete();
			finderMethodArguments.add(argument);
			argument.accept(portVisitor);
		}
		portVisitor.replaceASTNode(methodInvocation, finderMethodInvocation);
		return visitChildren;
	}
	
	public static boolean isFindByPrimaryKeyStatement(ApplicationInfo applicationInfo, VariableDeclarationStatement variableDeclarationStatement) {
		boolean findByPrimaryKeyStatement = false;
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> variableDeclarationFragments = variableDeclarationStatement.fragments();
		if (variableDeclarationFragments.size() == 1) {
			VariableDeclarationFragment variableDeclarationFragment = variableDeclarationFragments.get(0);
			findByPrimaryKeyStatement = isFindByPrimaryKeyExpression(applicationInfo, variableDeclarationFragment.getInitializer());
		}
		return findByPrimaryKeyStatement;
	}
	
	public static boolean isFindByPrimaryKeyStatement(ApplicationInfo applicationInfo, ExpressionStatement expressionStatement) {
		boolean findByPrimaryKeyStatement = false;
		Expression statementExpression = expressionStatement.getExpression();
		if (statementExpression.getNodeType() == ASTNode.ASSIGNMENT) {
			Assignment assignment = (Assignment) statementExpression;
			findByPrimaryKeyStatement = assignment.getLeftHandSide().getNodeType() == ASTNode.SIMPLE_NAME && isFindByPrimaryKeyExpression(applicationInfo, assignment.getRightHandSide());
		}
		else if (isFindByPrimaryKeyExpression(applicationInfo, statementExpression)) {
			findByPrimaryKeyStatement = true;
		}
		else if (statementExpression.getNodeType() == ASTNode.METHOD_INVOCATION) {
			MethodInvocation methodInvocation = (MethodInvocation) statementExpression;
			@SuppressWarnings("unchecked")
			List<Expression> arguments = methodInvocation.arguments();
			for (Expression argument : arguments) {
				if (isFindByPrimaryKeyExpression(applicationInfo, argument)) {
					findByPrimaryKeyStatement = true;
				}
			}
		}
		return findByPrimaryKeyStatement;
	}
	
	public static boolean isConditionalFindByPrimaryKeyStatement(ApplicationInfo applicationInfo, ExpressionStatement expressionStatement) {
		boolean conditionalFindByPrimaryKeyStatement = false;
		Expression statementExpression = expressionStatement.getExpression();
		if (statementExpression.getNodeType() == ASTNode.ASSIGNMENT) {
			Assignment assignment = (Assignment) statementExpression;
			if (assignment.getLeftHandSide().getNodeType() == ASTNode.SIMPLE_NAME) {
				Expression expression = assignment.getRightHandSide();
				if (expression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
					expression = ((ParenthesizedExpression) expression).getExpression();
				}
				if (expression.getNodeType() == ASTNode.CONDITIONAL_EXPRESSION) {
					ConditionalExpression conditionalExpression = (ConditionalExpression) expression;
					if (isFindByPrimaryKeyExpression(applicationInfo, conditionalExpression.getThenExpression())) {
						conditionalFindByPrimaryKeyStatement = true;
					}
					else if (isFindByPrimaryKeyExpression(applicationInfo, conditionalExpression.getElseExpression())) {
						conditionalFindByPrimaryKeyStatement = true;
					}
				}
			}
		}
		return conditionalFindByPrimaryKeyStatement;
	}
	
	public static boolean isFindByPrimaryKeyExpression(ApplicationInfo applicationInfo, Expression expression) {
		boolean findByPrimaryKeyExpression = false;
		if (expression != null) {
			if (expression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
				ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) expression;
				expression = parenthesizedExpression.getExpression();
			}
			ITypeBinding typeBinding = expression.resolveTypeBinding();
			if (typeBinding != null && applicationInfo.isAccessBeanType(typeBinding.getQualifiedName())) {
				if (expression.getNodeType() == ASTNode.METHOD_INVOCATION) {
					MethodInvocation methodInvocation = (MethodInvocation) expression;
					String methodName = methodInvocation.getName().getIdentifier();
					if (FIND_BY_PRIMARY_KEY.equals(methodName) && methodInvocation.getExpression() != null) {
						ITypeBinding methodExpressionTypeBinding = methodInvocation.getExpression().resolveTypeBinding();
						if (isFinderResultCacheUtil(methodExpressionTypeBinding)) {
							findByPrimaryKeyExpression = true;
						}
						else {
							System.out.println("not a cache util findByPrimaryKey " + expression);
						}
					}
				}
			}
		}
		return findByPrimaryKeyExpression;
	}
	
	private static MethodInvocation getFindByPrimaryKeyMethodInvocationFromExpression(Expression expression) {
		if (expression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
			expression = ((ParenthesizedExpression) expression).getExpression();
		}
		return (MethodInvocation) expression;
	}
	
	public static boolean portFindByPrimaryKeyStatement(ApplicationInfo applicationInfo, VariableDeclarationStatement variableDeclarationStatement, PortVisitor portVisitor) {
		//CatalogEntryAccessBean a = CatalogEntryCache.findByPrimaryKey(new CatalogEntryKey(this.toCatalogEntryReferenceNumber));
		boolean visitChildren = false;
		AST ast = variableDeclarationStatement.getAST();
		variableDeclarationStatement.getType().accept(portVisitor);
		VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) variableDeclarationStatement.fragments().get(0);
		MethodInvocation findByPrimaryKeyMethodInvocation = getFindByPrimaryKeyMethodInvocationFromExpression(variableDeclarationFragment.getInitializer());
		String cacheUtil = findByPrimaryKeyMethodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
		String accessBean = CACHE_UTIL_TO_ACCESSBEAN_MAP.get(cacheUtil);
		if(accessBean == null){
			System.out.println("The accessBean is null for: " + cacheUtil);
			return false;			
		}
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(accessBean);
		String jpaAccessBean = applicationInfo.getTypeMapping(accessBean);
		ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
		classInstanceCreation.setType(ast.newSimpleType(ast.newName(jpaAccessBean)));
		findByPrimaryKeyMethodInvocation.delete();
		variableDeclarationFragment.setInitializer(classInstanceCreation);
		List<Statement> newStatementList = new ArrayList<Statement>();
		newStatementList.add(variableDeclarationStatement);
		portFindByPrimaryKeyArguments(entityInfo, newStatementList, findByPrimaryKeyMethodInvocation, variableDeclarationFragment.getName().getIdentifier(), portVisitor);
		portVisitor.replaceStatement(variableDeclarationStatement, newStatementList);
		TargetExceptionUtil.portVariableDeclarationStatement(variableDeclarationStatement, portVisitor);
		return visitChildren;
	}
	
	public static boolean portFindByPrimaryKeyStatement(ApplicationInfo applicationInfo, ExpressionStatement expressionStatement, PortVisitor portVisitor) {
		boolean visitChildren = true;
		AST ast = expressionStatement.getAST();
		Expression statementExpression = expressionStatement.getExpression();
		if (statementExpression.getNodeType() == ASTNode.ASSIGNMENT) {
			Assignment assignment = (Assignment) statementExpression;
			SimpleName leftHandSide = (SimpleName) assignment.getLeftHandSide();
			MethodInvocation findByPrimaryKeyMethodInvocation = getFindByPrimaryKeyMethodInvocationFromExpression(assignment.getRightHandSide());
			String cacheUtil = findByPrimaryKeyMethodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
			String accessBean = CACHE_UTIL_TO_ACCESSBEAN_MAP.get(cacheUtil);
			if(accessBean == null){
				System.out.println("2The accessBean is null for: " + cacheUtil);
				return false;			
			}
			EntityInfo entityInfo = applicationInfo.getEntityInfoForType(accessBean);
			String jpaAccessBean = applicationInfo.getTypeMapping(accessBean);
			ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
			classInstanceCreation.setType(ast.newSimpleType(ast.newName(jpaAccessBean)));
			assignment.setRightHandSide(classInstanceCreation);
			List<Statement> newStatementList = new ArrayList<Statement>();
			newStatementList.add(expressionStatement);
			portFindByPrimaryKeyArguments(entityInfo, newStatementList, findByPrimaryKeyMethodInvocation, leftHandSide.getIdentifier(), portVisitor);
			portVisitor.replaceStatement(expressionStatement, newStatementList);
			visitChildren = false;
		}
		else if (statementExpression.getNodeType() == ASTNode.METHOD_INVOCATION) {
			List<Statement> newStatementList = new ArrayList<Statement>();
			MethodInvocation methodInvocation = (MethodInvocation) statementExpression;
			if (FIND_BY_PRIMARY_KEY.equals(methodInvocation.getName().getIdentifier())) {
				portFindByPrimaryKeyMethodInvocation(applicationInfo, methodInvocation, newStatementList, portVisitor);
			}
			else {
				@SuppressWarnings("unchecked")
				List<Expression> arguments = methodInvocation.arguments();
				for (Expression argument : arguments) {
					if (isFindByPrimaryKeyExpression(applicationInfo, argument)) {
						String accessBeanVariableName = portFindByPrimaryKeyMethodInvocation(applicationInfo, getFindByPrimaryKeyMethodInvocationFromExpression(argument), newStatementList, portVisitor);
						JavaUtil.replaceASTNode(argument, ast.newSimpleName(accessBeanVariableName));
					}
					else {
						argument.accept(portVisitor);
					}
				}
				if (methodInvocation.getExpression() != null) {
					methodInvocation.getExpression().accept(portVisitor);
				}
				newStatementList.add(expressionStatement);
			}
			portVisitor.replaceStatement(expressionStatement, newStatementList);
			visitChildren = false;
		}
		return visitChildren;
	}
	
	public static boolean portConditionalFindByPrimaryKeyStatement(ApplicationInfo applicationInfo, ExpressionStatement expressionStatement, PortVisitor portVisitor) {
		boolean visitChildren = true;
		AST ast = expressionStatement.getAST();
		Expression statementExpression = expressionStatement.getExpression();
		if (statementExpression.getNodeType() == ASTNode.ASSIGNMENT) {
			Assignment assignment = (Assignment) statementExpression;
			SimpleName leftHandSide = (SimpleName) assignment.getLeftHandSide();
			Expression expression = assignment.getRightHandSide();
			if (expression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
				expression = ((ParenthesizedExpression) expression).getExpression();
			}
			ConditionalExpression conditionalExpression = (ConditionalExpression) expression;
			conditionalExpression.getExpression().accept(portVisitor);
			Expression ifExpression = conditionalExpression.getExpression();
			if (ifExpression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
				ifExpression = ((ParenthesizedExpression) ifExpression).getExpression();
			}
			IfStatement ifStatement = ast.newIfStatement();
			ifStatement.setExpression((Expression) ASTNode.copySubtree(ast, ifExpression)); 
			Block thenBlock = ast.newBlock();
			ifStatement.setThenStatement(thenBlock);
			if (isFindByPrimaryKeyExpression(applicationInfo, conditionalExpression.getThenExpression())) {
				MethodInvocation findByPrimaryKeyMethodInvocation = getFindByPrimaryKeyMethodInvocationFromExpression(conditionalExpression.getThenExpression());
				String cacheUtil = findByPrimaryKeyMethodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
				String accessBean = CACHE_UTIL_TO_ACCESSBEAN_MAP.get(cacheUtil);
				EntityInfo entityInfo = applicationInfo.getEntityInfoForType(accessBean);
				String jpaAccessBean = applicationInfo.getTypeMapping(accessBean);
				ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
				classInstanceCreation.setType(ast.newSimpleType(ast.newName(jpaAccessBean)));
				Assignment newAssignment = ast.newAssignment();
				newAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, leftHandSide));
				newAssignment.setRightHandSide(classInstanceCreation);
				ExpressionStatement newExpressionStatement = ast.newExpressionStatement(newAssignment);
				@SuppressWarnings("unchecked")
				List<Statement> newStatementList = thenBlock.statements();
				newStatementList.add(newExpressionStatement);
				portFindByPrimaryKeyArguments(entityInfo, newStatementList, findByPrimaryKeyMethodInvocation, leftHandSide.getIdentifier(), portVisitor);
			}
			else {
				conditionalExpression.getThenExpression().accept(portVisitor);
				Assignment newAssignment = ast.newAssignment();
				newAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, leftHandSide));
				newAssignment.setRightHandSide((Expression) ASTNode.copySubtree(ast, conditionalExpression.getThenExpression()));
				ExpressionStatement newExpressionStatement = ast.newExpressionStatement(newAssignment);
				@SuppressWarnings("unchecked")
				List<Statement> newStatementList = thenBlock.statements();
				newStatementList.add(newExpressionStatement);
			}
			Block elseBlock = ast.newBlock();
			ifStatement.setElseStatement(elseBlock);
			if (isFindByPrimaryKeyExpression(applicationInfo, conditionalExpression.getElseExpression())) {
				MethodInvocation findByPrimaryKeyMethodInvocation = getFindByPrimaryKeyMethodInvocationFromExpression(conditionalExpression.getElseExpression());
				String cacheUtil = findByPrimaryKeyMethodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
				String accessBean = CACHE_UTIL_TO_ACCESSBEAN_MAP.get(cacheUtil);
				EntityInfo entityInfo = applicationInfo.getEntityInfoForType(accessBean);
				String jpaAccessBean = applicationInfo.getTypeMapping(accessBean);
				ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
				classInstanceCreation.setType(ast.newSimpleType(ast.newName(jpaAccessBean)));
				Assignment newAssignment = ast.newAssignment();
				newAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, leftHandSide));
				newAssignment.setRightHandSide(classInstanceCreation);
				ExpressionStatement newExpressionStatement = ast.newExpressionStatement(newAssignment);
				@SuppressWarnings("unchecked")
				List<Statement> newStatementList = elseBlock.statements();
				newStatementList.add(newExpressionStatement);
				portFindByPrimaryKeyArguments(entityInfo, newStatementList, findByPrimaryKeyMethodInvocation, leftHandSide.getIdentifier(), portVisitor);
			}
			else {
				conditionalExpression.getElseExpression().accept(portVisitor);
				Assignment newAssignment = ast.newAssignment();
				newAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, leftHandSide));
				newAssignment.setRightHandSide((Expression) ASTNode.copySubtree(ast, conditionalExpression.getElseExpression()));
				ExpressionStatement newExpressionStatement = ast.newExpressionStatement(newAssignment);
				@SuppressWarnings("unchecked")
				List<Statement> newStatementList = elseBlock.statements();
				newStatementList.add(newExpressionStatement);
			}
			portVisitor.replaceASTNode(expressionStatement, ifStatement);
			visitChildren = false;
		}
		return visitChildren;
	}
	
	private static String portFindByPrimaryKeyMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, List<Statement> newStatementList, PortVisitor portVisitor) {
		AST ast = methodInvocation.getAST();
		String cacheUtil = methodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
		String accessBean = CACHE_UTIL_TO_ACCESSBEAN_MAP.get(cacheUtil);
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(accessBean);
		String jpaAccessBean = applicationInfo.getTypeMapping(accessBean);
		String accessBeanVariableName = accessBean;
		int index = accessBeanVariableName.lastIndexOf('.');
		if (index > -1) {
			accessBeanVariableName = accessBeanVariableName.substring(index + 1);
		}
		accessBeanVariableName = Introspector.decapitalize(accessBeanVariableName);
		ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
		classInstanceCreation.setType(ast.newSimpleType(ast.newName(jpaAccessBean)));
		VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
		variableDeclarationFragment.setName(ast.newSimpleName(accessBeanVariableName));
		variableDeclarationFragment.setInitializer(classInstanceCreation);
		VariableDeclarationStatement variableDeclarationStatement = ast.newVariableDeclarationStatement(variableDeclarationFragment);
		variableDeclarationStatement.setType(ast.newSimpleType(ast.newName(jpaAccessBean)));
		newStatementList.add(variableDeclarationStatement);
		portFindByPrimaryKeyArguments(entityInfo, newStatementList, methodInvocation, accessBeanVariableName, portVisitor);
		return accessBeanVariableName;
	}
	
	public static boolean portFindByPrimaryKeyArguments(EntityInfo entityInfo, List<Statement> statementList, MethodInvocation findByPrimaryKeyMethodInvocation, String accessBeanVariableName, PortVisitor portVisitor) {
		boolean visitChildren = false;
		AST ast = findByPrimaryKeyMethodInvocation.getAST();
		String cacheUtil = findByPrimaryKeyMethodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
		List<FieldInfo> keyFields = entityInfo.getKeyFields();
		@SuppressWarnings("unchecked")
		List<Expression> findByPrimaryKeyArguments = findByPrimaryKeyMethodInvocation.arguments();
		List<Expression> initKeyExpressions = null;
		if (findByPrimaryKeyArguments.size() == 1 && findByPrimaryKeyArguments.get(0).getNodeType() == ASTNode.CLASS_INSTANCE_CREATION &&
				PrimaryKeyUtil.isPrimaryKeyClassInstanceCreation(entityInfo.getModuleInfo().getApplicationInfo(), (ClassInstanceCreation) findByPrimaryKeyArguments.get(0))) {
			ClassInstanceCreation keyClassInstanceCreation = (ClassInstanceCreation) findByPrimaryKeyArguments.get(0);
			keyClassInstanceCreation.delete();
			initKeyExpressions = PrimaryKeyUtil.getPortedInitKeyArguments(entityInfo, keyClassInstanceCreation, portVisitor);
		}
		else if (findByPrimaryKeyArguments.size() == 1) {
			initKeyExpressions = new ArrayList<Expression>();
			ITypeBinding keyArgumentTypeBinding = findByPrimaryKeyArguments.get(0).resolveTypeBinding();
			findByPrimaryKeyArguments.get(0).accept(portVisitor);
			Expression initKeyArgument = findByPrimaryKeyArguments.get(0);
			initKeyArgument.delete();
			if (entityInfo.getModuleInfo().getApplicationInfo().isEntityKeyType(keyArgumentTypeBinding.getQualifiedName()) && keyFields.size() > 1) {
				for (FieldInfo fieldInfo : keyFields) {
					Expression keyFieldReference = null;
					RelatedEntityInfo relatedEntityInfo = fieldInfo.getRelatedEntityInfo();
					MethodInvocation getterMethodInvocation = ast.newMethodInvocation();
					getterMethodInvocation.setName(ast.newSimpleName(relatedEntityInfo == null ? fieldInfo.getTargetGetterName() : relatedEntityInfo.getGetterName()));
					getterMethodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, initKeyArgument));
					keyFieldReference = getterMethodInvocation;
//					if (relatedEntityInfo == null) {
//						keyFieldReference = getterMethodInvocation;
//					}
//					else {
//						MethodInvocation relatedFieldMethodInvocation = ast.newMethodInvocation();
//						relatedFieldMethodInvocation.setExpression(getterMethodInvocation);
//						if (fieldInfo.getReferencedFieldInfo() == null || fieldInfo.getReferencedFieldInfo().getReferencedFieldInfo() == null) {
//							System.out.println(fieldInfo.getFieldName()+" "+fieldInfo.getColumnInfo().getTableInfo().getTableName() + "." + fieldInfo.getColumnInfo().getColumnName());
//							relatedFieldMethodInvocation.setName(ast.newSimpleName(fieldInfo.getReferencedFieldInfo().getTargetGetterName()));
//						}
//						else {
//							System.out.println(fieldInfo.getFieldName()+" "+fieldInfo.getColumnInfo().getTableInfo().getTableName() + "." + fieldInfo.getColumnInfo().getColumnName());
//							relatedFieldMethodInvocation.setName(ast.newSimpleName(fieldInfo.getReferencedFieldInfo().getReferencedFieldInfo().getTargetGetterName()));
//						}
//						//relatedFieldMethodInvocation.setName(ast.newSimpleName(fieldInfo.getReferencedFieldInfo().getReferencedFieldInfo().getTargetGetterName()));
//						keyFieldReference = relatedFieldMethodInvocation;
//					}
					if (!STRING.equals(fieldInfo.getTypeName())) {
						NullConstructorParameter nullConstructorParameter = entityInfo.getAccessBeanInfo().getNullConstructorParameterByName(fieldInfo.getFieldName());
						if (nullConstructorParameter != null && nullConstructorParameter.getConverterClassName() != null) {
							keyFieldReference = PrimaryKeyUtil.convertExpressionToString(keyFieldReference);
						}
					}
					initKeyExpressions.add(keyFieldReference);
				}
			}
			else {
				if (keyArgumentTypeBinding != null && !STRING.equals(keyArgumentTypeBinding.getQualifiedName())) {
					FieldInfo fieldInfo = keyFields.get(0);
					NullConstructorParameter nullConstructorParameter = entityInfo.getAccessBeanInfo().getNullConstructorParameterByName(fieldInfo.getFieldName());
					if (nullConstructorParameter != null && nullConstructorParameter.getConverterClassName() != null) {
						initKeyArgument = PrimaryKeyUtil.convertExpressionToString(initKeyArgument);
					}
				}
				initKeyExpressions.add(initKeyArgument);
			}
		}
		else if (CACHE_UTIL_TO_FIND_BY_PRIMARY_KEY_FIELDS_MAP.get(cacheUtil) != null) {
			initKeyExpressions = new ArrayList<Expression>();
			for (int i = 0; i < keyFields.size(); i++) {
				initKeyExpressions.add(null);
			}
			String[] fieldNames = CACHE_UTIL_TO_FIND_BY_PRIMARY_KEY_FIELDS_MAP.get(cacheUtil);
			for (String fieldName : fieldNames) {
				ITypeBinding keyArgumentTypeBinding = findByPrimaryKeyArguments.get(0).resolveTypeBinding();
				findByPrimaryKeyArguments.get(0).accept(portVisitor);
				Expression initKeyArgument = findByPrimaryKeyArguments.get(0);
				initKeyArgument.delete();
				FieldInfo fieldInfo = entityInfo.getFieldInfoByName(fieldName);
				if (keyArgumentTypeBinding != null && !STRING.equals(keyArgumentTypeBinding.getQualifiedName())) {
					NullConstructorParameter nullConstructorParameter = entityInfo.getAccessBeanInfo().getNullConstructorParameterByName(fieldInfo.getFieldName());
					if (nullConstructorParameter != null && nullConstructorParameter.getConverterClassName() != null) {
						initKeyArgument = PrimaryKeyUtil.convertExpressionToString(initKeyArgument);
					}
				}
				int fieldIndex = keyFields.indexOf(fieldInfo);
				initKeyExpressions.set(fieldIndex, initKeyArgument);
			}
		}
		else {
			initKeyExpressions = new ArrayList<Expression>();
			for (int i = 0; i < keyFields.size(); i++) {
				initKeyExpressions.add(null);
			}
			KeyClassConstructorInfo keyClassConstructorInfo = PrimaryKeyUtil.getMatchingKeyClassConstructorInfo(entityInfo, findByPrimaryKeyArguments);
			List<FieldInfo> constructorFields = keyClassConstructorInfo.getFields();
			for (FieldInfo fieldInfo : constructorFields) {
				ITypeBinding keyArgumentTypeBinding = findByPrimaryKeyArguments.get(0).resolveTypeBinding();
				findByPrimaryKeyArguments.get(0).accept(portVisitor);
				Expression initKeyArgument = findByPrimaryKeyArguments.get(0);
				initKeyArgument.delete();
				if (keyArgumentTypeBinding != null && !STRING.equals(keyArgumentTypeBinding.getQualifiedName())) {
					NullConstructorParameter nullConstructorParameter = entityInfo.getAccessBeanInfo().getNullConstructorParameterByName(fieldInfo.getFieldName());
					if (nullConstructorParameter != null && nullConstructorParameter.getConverterClassName() != null) {
						initKeyArgument = PrimaryKeyUtil.convertExpressionToString(initKeyArgument);
					}
				}
				int fieldIndex = keyFields.indexOf(fieldInfo);
				initKeyExpressions.set(fieldIndex, initKeyArgument);
			}
		}
		for (FieldInfo keyField : keyFields) {
			MethodInvocation setInitKeyMethodInvocation = ast.newMethodInvocation();
			setInitKeyMethodInvocation.setName(ast.newSimpleName(SET_INIT_KEY + keyField.getTargetFieldName()));
			setInitKeyMethodInvocation.setExpression(ast.newSimpleName(accessBeanVariableName));
			@SuppressWarnings("unchecked")
			List<Expression> setInitKeyMethodInvocationArguments = setInitKeyMethodInvocation.arguments();
			Expression argument = initKeyExpressions.remove(0);
			try {
				setInitKeyMethodInvocationArguments.add(argument);
			}
			catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			ExpressionStatement expressionStatement = ast.newExpressionStatement(setInitKeyMethodInvocation);
			statementList.add(expressionStatement);
		}
		MethodInvocation instantiateEntityMethodInvocation = ast.newMethodInvocation();
		instantiateEntityMethodInvocation.setName(ast.newSimpleName(INSTANTIATE_ENTITY));
		instantiateEntityMethodInvocation.setExpression(ast.newSimpleName(accessBeanVariableName));
		ExpressionStatement expressionStatement = ast.newExpressionStatement(instantiateEntityMethodInvocation);
		statementList.add(expressionStatement);
		return visitChildren;
	}
	
	public static boolean isUserMethod(ApplicationInfo applicationInfo, MethodInvocation methodInvocation) {
		//CatalogEntryCache.getTemplateFileName(abCatalogEntry, getUserId(), getUser(),	getStoreId(),commandContext.getDeviceType().toString(), commandContext.getLanguageId());
		boolean result = false;
		String cacheUtil = methodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
		String accessBeanType = CACHE_UTIL_TO_ACCESSBEAN_MAP.get(cacheUtil);
		if (accessBeanType != null) {
			EntityInfo entityInfo = applicationInfo.getEntityInfoForType(accessBeanType);
			if(entityInfo != null) {
				IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
				if (methodBinding != null) {
					ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
					if (parameterTypes.length > 0) {
						StringBuilder sb = new StringBuilder(methodBinding.getName());
						for (int i = 1; i < parameterTypes.length; i++) {
							sb.append("+");
							sb.append(parameterTypes[i].getQualifiedName());
						}
						String methodKey = sb.toString();
						if (entityInfo.getUserMethodInfo(methodKey) != null) {
							result = true;
						}
					}
				}
			}
		}
		return result;
	}
	
	public static boolean replaceUserMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		AST ast = methodInvocation.getAST();
		@SuppressWarnings("unchecked")
		List<Expression> arguments = methodInvocation.arguments();
		MethodInvocation newMethodInvocation = ast.newMethodInvocation();
		newMethodInvocation.setName(ast.newSimpleName(methodInvocation.getName().getIdentifier()));
		Expression argument = arguments.get(0);
		ITypeBinding typeBinding = argument.resolveTypeBinding();
		if (typeBinding != null) {
			if (applicationInfo.isAccessBeanType(typeBinding.getQualifiedName())) {
				argument.accept(portVisitor);
				argument = arguments.get(0);
				argument.delete();
				newMethodInvocation.setExpression(argument);
			}
			else if (argument.getNodeType() == ASTNode.METHOD_INVOCATION && ((MethodInvocation) argument).getExpression() != null) {
				MethodInvocation argumentMethodInvocation = (MethodInvocation) argument;
				argumentMethodInvocation.getExpression().accept(portVisitor);
				Expression methodInvocationExpression = argumentMethodInvocation.getExpression();
				argumentMethodInvocation.setExpression(null);
				newMethodInvocation.setExpression(methodInvocationExpression);
				argument.delete();
			}
			else {
				// need to instantiate the access bean instance from the primary key
				argument.delete();
			}
		}
		@SuppressWarnings("unchecked")
		List<Expression> newArguments = newMethodInvocation.arguments();
		while (arguments.size() > 0) {
			arguments.get(0).accept(portVisitor);
			argument = arguments.get(0);
			argument.delete();
			newArguments.add(argument);
		}
		portVisitor.replaceASTNode(methodInvocation, newMethodInvocation);
		return false;
	}
	
	public static boolean isAccessBeanConversionMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation) {
		//CatalogEntryCache.getCatalogEntryAccessBean((ItemJPAAccessBean)iabResource);
		//ProductCache.getProductAccessBean(catEntryAB)
		boolean result = false;
		IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
		if (methodBinding != null) {
			String methodName = methodInvocation.resolveMethodBinding().getName();
			ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
			ITypeBinding returnTypeBinding = methodBinding.getReturnType();
			if (returnTypeBinding != null && parameterTypes.length == 1 && methodName.startsWith(GET) && methodName.endsWith(ACCESS_BEAN_SUFFIX)) {
				String cacheUtil = methodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
				String accessBeanType = CACHE_UTIL_TO_ACCESSBEAN_MAP.get(cacheUtil);
				String sourceAccessBeanType = parameterTypes[0].getQualifiedName();
				if (returnTypeBinding.getQualifiedName().equals(accessBeanType) && !accessBeanType.equals(sourceAccessBeanType) && applicationInfo.isAccessBeanType(sourceAccessBeanType)) {
					result = true;
				}
			}
		}
		return result;
	}
	
	public static boolean portAccessBeanConversionMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = false;
		AST ast = methodInvocation.getAST();
		String cacheUtil = methodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
		String accessBeanType = CACHE_UTIL_TO_ACCESSBEAN_MAP.get(cacheUtil);
		String jpaAccessBeanType = applicationInfo.getTypeMapping(accessBeanType);
		EntityInfo entityInfo = applicationInfo.getEntityInfoForType(accessBeanType);
		@SuppressWarnings("unchecked")
		List<Expression> arguments = methodInvocation.arguments();
		String sourceAccessBeanType = arguments.get(0).resolveTypeBinding().getQualifiedName();
		EntityInfo sourceEntityInfo = applicationInfo.getEntityInfoForType(sourceAccessBeanType);
		MethodInvocation newMethodInvocation = ast.newMethodInvocation();
		newMethodInvocation.setName(ast.newSimpleName(GET_ENTITY));
		arguments.get(0).accept(portVisitor);
		Expression argument = arguments.get(0);
		argument.delete();
		if (argument.getNodeType() == ASTNode.CAST_EXPRESSION) {
			ParenthesizedExpression parenthesizedExpression = ast.newParenthesizedExpression();
			parenthesizedExpression.setExpression(argument);
			newMethodInvocation.setExpression(parenthesizedExpression);
		}
		else {
			newMethodInvocation.setExpression(argument);
		}
		ClassInstanceCreation classInstanceCreation = ast.newClassInstanceCreation();
		classInstanceCreation.setType(ast.newSimpleType(ast.newName(jpaAccessBeanType)));
		@SuppressWarnings("unchecked")
		List<Expression> classInstanceCreationArguments = classInstanceCreation.arguments();
		if (sourceEntityInfo != null && sourceEntityInfo.getSubtypes() != null) {
			CastExpression newCastExpression = ast.newCastExpression();
			newCastExpression.setType(ast.newSimpleType(ast.newName(entityInfo.getEntityClassInfo().getQualifiedClassName())));
			newCastExpression.setExpression(newMethodInvocation);
			classInstanceCreationArguments.add(newCastExpression);
		}
		else {
			classInstanceCreationArguments.add(newMethodInvocation);
		}
		portVisitor.replaceASTNode(methodInvocation, classInstanceCreation);
		return visitChildren;
	}
	
	public static boolean isGetCachedAccessBeanMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation) {
		//UserCache.getUserAccessBean(abUser)
		boolean result = false;
		IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
		if (methodBinding != null) {
			String methodName = methodInvocation.resolveMethodBinding().getName();
			ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
			ITypeBinding returnTypeBinding = methodBinding.getReturnType();
			if (returnTypeBinding != null && parameterTypes.length == 1 && methodName.startsWith(GET) && methodName.endsWith(ACCESS_BEAN_SUFFIX)) {
				String cacheUtil = methodInvocation.getExpression().resolveTypeBinding().getQualifiedName();
				String accessBeanType = CACHE_UTIL_TO_ACCESSBEAN_MAP.get(cacheUtil);
				String sourceAccessBeanType = parameterTypes[0].getQualifiedName();
				if (accessBeanType != null && accessBeanType.equals(sourceAccessBeanType)) {
					result = true;
				}
			}
		}
		return result;
	}
	
	public static boolean portGetCachedAccessBeanMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = false;
		@SuppressWarnings("unchecked")
		List<Expression> arguments = methodInvocation.arguments();
		arguments.get(0).accept(portVisitor);
		Expression accessBeanExpression = arguments.get(0);
		accessBeanExpression.delete();
		portVisitor.replaceASTNode(methodInvocation, accessBeanExpression);
		return visitChildren;
	}
	
	public static boolean isGetCachedAccessBeanAssignment(ApplicationInfo applicationInfo, Assignment assignment) {
		boolean result = false;
		if (assignment.getLeftHandSide().getNodeType() == ASTNode.SIMPLE_NAME && assignment.getRightHandSide().getNodeType() == ASTNode.METHOD_INVOCATION) {
			MethodInvocation methodInvocation = (MethodInvocation) assignment.getRightHandSide();
			if (isGetCachedAccessBeanMethodInvocation(applicationInfo, methodInvocation)) {
				@SuppressWarnings("unchecked")
				List<Expression> arguments = methodInvocation.arguments();
				Expression argument = arguments.get(0);
				if (argument.getNodeType() == ASTNode.SIMPLE_NAME) {
					SimpleName source = (SimpleName) argument;
					SimpleName target = (SimpleName) assignment.getLeftHandSide();
					if (source.getIdentifier().equals(target.getIdentifier())) {
						result = true;
					}
				}
			}
		}
		return result;
	}
	
	public static boolean isGetClassNameMethodInvocation(MethodInvocation methodInvocation) {
		boolean result = false;
		if (methodInvocation.getExpression() != null && methodInvocation.getExpression().getNodeType() == ASTNode.TYPE_LITERAL && GET_NAME.equals(methodInvocation.getName().getIdentifier())) {
			TypeLiteral typeLiteral = (TypeLiteral) methodInvocation.getExpression();
			Type type = typeLiteral.getType();
			ITypeBinding typeBinding = type.resolveBinding();
			if (typeBinding != null && FinderResultCacheUtil.isFinderResultCacheUtil(typeBinding)) {
				result = true;
			}
		}
		return result;
	}
	
	public static boolean portGetClassNameMethodInvocation(MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = true;
		AST ast = methodInvocation.getAST();
		String qualifiedTypeName = ((TypeLiteral) methodInvocation.getExpression()).getType().resolveBinding().getQualifiedName();
		StringLiteral stringLiteral = ast.newStringLiteral();
		stringLiteral.setLiteralValue(qualifiedTypeName);
		portVisitor.replaceASTNode(methodInvocation, stringLiteral);
		return visitChildren;
	}
}
