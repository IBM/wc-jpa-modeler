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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ProjectInfo;

public class EntityReferenceUtil {
	private static final Map<String, Collection<String>> PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES;
	static {
		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES = new HashMap<String, Collection<String>>();
//		Collection<String> entityReferencingTypes = new HashSet<String>();
////		entityReferencingTypes.add("com.ibm.commerce.server.WcsApp");
//		entityReferencingTypes.add("com.ibm.commerce.beans.SmartDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.beans.event.DataBeanAuditProvider");
////		entityReferencingTypes.add("com.ibm.commerce.command.ECCommand");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.ErrorCmdExecUnit");
//		entityReferencingTypes.add("com.ibm.commerce.security.event.GuestUserMigrationEvent");
//		entityReferencingTypes.add("com.ibm.commerce.beans.SmartDataBeanImpl");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.RequestHandle");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.ExecutableUnit");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.JspExecUnit");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.ErrorViewExecUnit");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.UrlCmdExecUnit");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.ViewCmdExecUnit");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.JspCmdExecUnitFactory");
//		entityReferencingTypes.add("com.ibm.commerce.content.preview.util.PreviewHelper");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.AbstractErrorViewExecUnit");
//		entityReferencingTypes.add("com.ibm.commerce.accesscontrol.policymanager.PolicyManagerProvider");
//		entityReferencingTypes.add("com.ibm.commerce.accesscontrol.policymanager.PolicyManager");
//		entityReferencingTypes.add("com.ibm.commerce.event.accesslogging.AccessLogging");
//		entityReferencingTypes.add("com.ibm.commerce.accesscontrol.util.PolicyManagerHelper");
//		entityReferencingTypes.add("com.ibm.commerce.event.accesslogging.AccessLoggingBodyData");
//		entityReferencingTypes.add("com.ibm.commerce.event.accesslogging.AccessLoggingHeaderData");
//		entityReferencingTypes.add("com.ibm.commerce.browseradapter.PasswordRerequestMediator");
//		entityReferencingTypes.add("com.ibm.commerce.browseradapter.persistentsession.PersistentSessionHelper");
//		entityReferencingTypes.add("com.ibm.commerce.scheduler.SchedulerJobAsync");
//		entityReferencingTypes.add("com.ibm.commerce.scheduler.SchedulerThreadGroup");
//		entityReferencingTypes.add("com.ibm.commerce.datatype.WCDeadlockException");
//		entityReferencingTypes.add("com.ibm.commerce.datatype.WCLockException");
//		entityReferencingTypes.add("com.ibm.commerce.tools.devtools.store.commands.RegistryUtil");
//		entityReferencingTypes.add("com.ibm.commerce.adapter.DeviceFormatAdapter");
//		entityReferencingTypes.add("com.ibm.commerce.adapter.AbstractHttpAdapter");
//		entityReferencingTypes.add("com.ibm.commerce.adapter.HttpAdapter");
//		entityReferencingTypes.add("com.ibm.commerce.util.SecurityHelper");
//		entityReferencingTypes.add("com.ibm.commerce.performance.monitor.PerfMonitor");
//		entityReferencingTypes.add("com.ibm.commerce.command.event.CommandExecutionEventUtil");
//		entityReferencingTypes.add("com.ibm.commerce.command.event.CommandExecutionEventFactory");
//		entityReferencingTypes.add("com.ibm.commerce.dynacache.commands.DynaCacheInvalidationImpl");
//		entityReferencingTypes.add("com.ibm.commerce.tools.devtools.publish.datadeploy.DataDeployController");
//		entityReferencingTypes.add("com.ibm.commerce.security.event.EventBaseData");
//		entityReferencingTypes.add("com.ibm.commerce.event.impl.AbstractECEventListener");
//		entityReferencingTypes.add("com.ibm.commerce.command.CommandFactory");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.HttpControllerRequestObject");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.ControllerRequestObject");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.ControllerReqObj");
//		entityReferencingTypes.add("com.ibm.commerce.event.businessaudit.BusinessAuditCommandExecutionEventFactory");
//		entityReferencingTypes.add("com.ibm.commerce.browseradapter.CookieDetector");
//		entityReferencingTypes.add("com.ibm.commerce.scheduler.SchedulerThreadGroups");
//		entityReferencingTypes.add("com.ibm.commerce.adapter.DeviceFormatManager");
//		entityReferencingTypes.add("com.ibm.commerce.event.usertraffic.Traffic");
//		entityReferencingTypes.add("com.ibm.commerce.server.DeviceAdapterHelper");
//		entityReferencingTypes.add("com.ibm.commerce.accesscontrol.policymanager.CommandLevelAuthorizationCache");
//		entityReferencingTypes.add("com.ibm.commerce.browseradapter.WhiteListProtectionHelper");
//		entityReferencingTypes.add("com.ibm.commerce.browseradapter.CrossSiteScriptingHelper");
//		entityReferencingTypes.add("com.ibm.commerce.foundation.services.invocation.spi.JCAHeaderInvocationMediator");
//		entityReferencingTypes.add("com.ibm.commerce.webcontroller.SOAWebController");
//		entityReferencingTypes.add("com.ibm.commerce.beans.DataBeanManager");
////		entityReferencingTypes.add("com.ibm.commerce.command.AbstractECTargetableCommand");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Enablement-BaseComponentsLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.ubf.registry.BusinessFlowStateRelation");
//		entityReferencingTypes.add("com.ibm.commerce.ubf.event.BusinessFlowEventData");
////		entityReferencingTypes.add("com.ibm.commerce.common.beansrc.CachedExtendedSupportedLanguageAccessBean");
//		entityReferencingTypes.add("com.ibm.commerce.utf.helper.PAttributeSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.utf.helper.PAttrValueSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.utf.helper.RFQSortingAttribute");
//		//caiduan
//		entityReferencingTypes.add("com.ibm.commerce.price.utils.AbstractGetPriceCmdInput");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Enablement-RelationshipManagementLogic", entityReferencingTypes);
////		entityReferencingTypes = new HashSet<String>();
////		entityReferencingTypes.add("com.ibm.commerce.price.utils.AbstractGetPriceCmdInput");
////		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Enablement-RelationshipManagementLogic-FEP", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.store.helpers.StoreSearchBean");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Enablement-RelationshipManagementData", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.me.datatype.CIDataImpl");
//		entityReferencingTypes.add("com.ibm.commerce.me.datatype.CIQuote");
//		entityReferencingTypes.add("com.ibm.commerce.messaging.composer.JSPInvoker");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Enablement-IntegrationLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.content.facade.server.commands.AbstractFetchAttachmentCmdImpl");
//		entityReferencingTypes.add("com.ibm.commerce.content.facade.server.utils.ContentComponentHelper");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Content-Server", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.seo.beans.util.AbstractCatalogNodeBeanObservable");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("SEO-BaseComponentLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.pa.admin.QnAStatusVisitor");
//		entityReferencingTypes.add("com.ibm.commerce.pa.admin.QnANodeVisitor");
//		entityReferencingTypes.add("com.ibm.commerce.pa.admin.QnATree");
//		entityReferencingTypes.add("com.ibm.commerce.pa.admin.QnAWalker");
//		entityReferencingTypes.add("com.ibm.commerce.pa.admin.MessageBox");
//		entityReferencingTypes.add("com.ibm.commerce.pa.admin.InformDialog");
//		entityReferencingTypes.add("com.ibm.commerce.pa.admin.XMLinkMetaphorDefault");
//		entityReferencingTypes.add("com.ibm.commerce.pa.admin.XMLinkMetaphor");
//		entityReferencingTypes.add("com.ibm.commerce.pa.admin.builder.ValueList");
//		entityReferencingTypes.add("com.ibm.commerce.pa.metaphor.SearchEngine");
//		entityReferencingTypes.add("com.ibm.commerce.pa.beans.DynamicDataBeanImpl");
//		entityReferencingTypes.add("com.ibm.commerce.pa.admin.builder.ProductFamilyBuilder");
//		entityReferencingTypes.add("com.ibm.commerce.catalog.content.util.SalesCatalogSyncHelper");
//		entityReferencingTypes.add("com.ibm.commerce.pa.admin.builder.SyncManager");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Catalog-ProductManagementLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.rest.classic.model.ControllerCommandModel");
//		entityReferencingTypes.add("com.ibm.commerce.catalog.util.RangePricingData");
//		entityReferencingTypes.add("com.ibm.commerce.foundation.rest.resourcehandler.AbstractResourceHandler");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Rest-Core", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.bi.taglib.BaseTag");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Analytics-BusinessIntelligenceLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.emarketing.emailtemplate.tag.TagUtil");
//		entityReferencingTypes.add("com.ibm.commerce.emarketing.emailtemplate.tag.Tag");
//		entityReferencingTypes.add("com.ibm.commerce.emarketing.engine.SMTPDistributor");
//		entityReferencingTypes.add("com.ibm.commerce.emarketing.emailtemplate.tag.TagEngine");
//		entityReferencingTypes.add("com.ibm.commerce.emarketing.emailtemplate.tag.TagEngineImpl");
//		entityReferencingTypes.add("com.ibm.commerce.tools.campaigns.search.beans.CollateralSearchListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.emarketing.utils.EmailDeliveryHelper");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Marketing-CampaignsAndScenarioMarketingLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.infrastructure.facade.server.util.InfrastructureServerUtil");
//		//caiduan
//		entityReferencingTypes.add("com.ibm.commerce.infrastructure.facade.server.helpers.InfrastructureComponentHelper");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Infrastructure-Server", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.context.baseimpl.ContextHelper");
//		entityReferencingTypes.add("com.ibm.commerce.component.WebAdapterComponent");
//		entityReferencingTypes.add("com.ibm.commerce.component.AbstractManagedComponentImpl");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Enablement-BusinessContextEngineLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.telesales.messaging.bodreply.ITelesalesResponseBuilder");
//		entityReferencingTypes.add("com.ibm.commerce.telesales.messaging.bodreply.TelesalesResponseBuilderImpl");
//		entityReferencingTypes.add("com.ibm.commerce.telesales.messaging.bodreply.TelesalesResponseDirector");
//		entityReferencingTypes.add("com.ibm.commerce.telesales.messaging.bodreply.TelesalesResponseBuilderFactory");
//		entityReferencingTypes.add("com.ibm.commerce.telesales.messaging.bodreply.AcknowledgeLogonCacheCmdImpl");
//		entityReferencingTypes.add("com.ibm.commerce.telesales.messaging.bodreply.ShowCountryCacheCmdImpl");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Telesales-EnablementIntegrationLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.order.utils.OrderItemDataPair");
//		entityReferencingTypes.add("com.ibm.commerce.payment.rules.EDPServices");
//		entityReferencingTypes.add("com.ibm.commerce.edp.tickler.IEDPTickler");
//		entityReferencingTypes.add("com.ibm.commerce.tools.optools.order.beans.OrderSearchDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.edp.config.ConfigurationService");
//		entityReferencingTypes.add("com.ibm.commerce.edp.tickler.EDPTicklerFactory");
//		entityReferencingTypes.add("com.ibm.commerce.order.beans.UsableInstructsPerOrderItemBlockListInputDataBean");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Order-OrderCaptureLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.contentmanagement.events.WorkspaceEventFactory");
//		entityReferencingTypes.add("com.ibm.commerce.contentmanagement.events.WorkspaceEventDataImpl");
//		entityReferencingTypes.add("com.ibm.commerce.contentmanagement.events.WorkspaceEventData");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("ContentManagement-WorkspaceFlowLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.foundation.server.command.soi.MessageMappingResponseCmd");
//		//caiduan
//		entityReferencingTypes.add("com.ibm.commerce.foundation.rest.util.SessionInformation");
//		entityReferencingTypes.add("com.ibm.commerce.foundation.rest.util.SessionInformationImpl");
//		entityReferencingTypes.add("com.ibm.commerce.foundation.rest.util.CookieHelper");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Foundation-Extension", entityReferencingTypes);
////		entityReferencingTypes = new HashSet<String>();
////		entityReferencingTypes.add("com.ibm.commerce.foundation.rest.util.SessionInformation");
////		entityReferencingTypes.add("com.ibm.commerce.foundation.rest.util.SessionInformationImpl");
////		entityReferencingTypes.add("com.ibm.commerce.foundation.rest.util.CookieHelper");
////		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Foundation-Extension-FEP", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.tools.optools.user.helpers.UserSearchAccessBean");
//		entityReferencingTypes.add("com.ibm.commerce.base.helpers.InitParameters");
//		entityReferencingTypes.add("com.ibm.commerce.base.helpers.BaseJDBCHelper");
//		entityReferencingTypes.add("com.ibm.commerce.tools.optools.user.helpers.UserSearch");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Enablement-BaseComponentsData", entityReferencingTypes);
////		entityReferencingTypes = new HashSet<String>();
////		entityReferencingTypes.add("com.ibm.commerce.marketing.segment.util.CustomerSegmentUtils");
////		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Marketing-Server-FEP", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.marketing.cache.GetMarketingSpotBehaviorCmdImpl");
//		entityReferencingTypes.add("com.ibm.commerce.marketing.dialog.util.MarketingUtil");
//		entityReferencingTypes.add("com.ibm.commerce.marketing.dialog.trigger.SensorEventListener");
//		//caiduan
//		entityReferencingTypes.add("com.ibm.commerce.marketing.segment.util.CustomerSegmentUtils");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Marketing-Server", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.tools.optools.order.helpers.OrderSearchAccessBean");
//		entityReferencingTypes.add("com.ibm.commerce.tools.optools.order.helpers.OrderSearch");
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.objects.OrderManagementJDBCHelperAccessBean");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Order-OrderCaptureData", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.edp.model.OMFAccessor");
//		entityReferencingTypes.add("com.ibm.commerce.edp.activitylog.ActivityLoggerAccessor");
//		entityReferencingTypes.add("com.ibm.commerce.edp.refunds.RefundFacadeAccessor");
//		entityReferencingTypes.add("com.ibm.commerce.payments.plugincontroller.ObjectModuleAccessor");
//		entityReferencingTypes.add("com.ibm.commerce.payments.plugincontroller.PaymentsAccessor");
//		entityReferencingTypes.add("com.ibm.commerce.payments.plugincontroller.beans.ObjectModuleFacadeLocalHome");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Payments-EDP-Data", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.price.facade.server.commands.AbstractFetchPriceListCmdImpl");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Price-Server", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.util.OrderBlockManagerInterface");
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.beans.DispositionHeaderInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.beans.MerchantReturnReasonsInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.beans.PreviousDispositionsInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.beans.ReturnDispositionInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.beans.ReturnedProductsInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.beans.ReturnReasonsInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.beans.ReturnRecordComponentByRmaAndLanguageInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.beans.ReturnRecordComponentInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.beans.ReturnRecordsForOperationManagerInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.beans.ReturnRecordsForReceiverInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.ordermanagement.beans.ReturnRecordsForReturnAdministratorInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.fulfillment.beans.FfmCenterByFfmCenterIdAndLanguageIdInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.fulfillment.beans.FFMOrderItemsInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.fulfillment.beans.FulfillmentCenterByLanguageAndStoreInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.fulfillment.beans.FulfillmentCenterByLanguageInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.fulfillment.beans.FulfillmentCenterInformationByLanguageInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.inventory.beans.PackageInformationByReleaseNumberInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.inventory.beans.PackageInformationInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.inventory.beans.VendorInformationInputListDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.tools.optools.returns.beans.ReturnSearchDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.inventory.beans.VendorPOReceiptListInputDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.inventory.beans.ExpectedInventoryRecordReceiptListInputDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.inventory.beans.ProductAvailabilityDataBean");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Order-OrderManagementLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.marketing.promotion.runtime.AdvancedPromotionEngine");
//		entityReferencingTypes.add("com.ibm.commerce.tools.epromotion.RLPromotionFactory");
//		entityReferencingTypes.add("com.ibm.commerce.marketing.promotion.integration.dependency.WCSOrderItem");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Merchandising-PromotionsAndDiscountsLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.component.giftregistry.objects.GiftRegistrySearchAccessBean");
//		entityReferencingTypes.add("com.ibm.commerce.component.giftregistry.objimpl.GiftRegistrySearchBase");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("GiftRegistry-BaseComponentData", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.utf.beans.UTFListSmartDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.utf.beans.UTFListInputDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.beans.OpenCryBidControlRuleInputDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.beans.OpenCryBidControlRuleSmartDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.beans.NegotiationListInputDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.beans.NegotiationListSmartDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.beans.ControlRuleDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.beans.NumericRangeDataBean");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.operation.NumericRangeDataBeanHelper");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Trading-AuctionsAndRFQsLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.rfq.helpers.RFQJdbcHelperAccessBean");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.helpers.AuctionJDBCHelperAccessBean");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Trading-AuctionsAndRFQsData", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.member.facade.server.commands.ComposeMemberGroupFromDataBeanCmd");
//		entityReferencingTypes.add("com.ibm.commerce.member.facade.server.commands.ComposeOrganizationFromDataBeanCmd");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.AuctionDataLightSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.AuctionDescriptionSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.AuctionInfoSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.AuctionItemSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.AuctionSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.AuctionStyleSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.AutoBidSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.BidDataLightSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.BidSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.ControlRuleSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.ForumMessageSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.ForumSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.MemberAuctionRelationSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.MessageInfoSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.negotiation.util.MessageSortingAttribute");
//		entityReferencingTypes.add("com.ibm.commerce.rfq.utils.RFQSortingAttribute");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Member-Server", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.experimentation.runtime.ExperimentService");
//		entityReferencingTypes.add("com.ibm.commerce.experimentation.util.ExperimentRuntimeUtil");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Marketing-ExperimentationManagementLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.rules.likeminds.WCSRecommendationServer");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Marketing-CustomerProfilingAndSegmentationLogic", entityReferencingTypes);
////		entityReferencingTypes = new HashSet<String>();
////		entityReferencingTypes.add("com.ibm.commerce.infrastructure.facade.server.helpers.InfrastructureComponentHelper");
////		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Infrastructure-Server-FEP", entityReferencingTypes);
////		entityReferencingTypes = new HashSet<String>();
////		entityReferencingTypes.add("com.ibm.commerce.order.event.CreateOrderOnSuccessEventData");
////		entityReferencingTypes.add("com.ibm.commerce.order.event.ChangeExternalOrdersEventData");
////		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Order-Server-FEP", entityReferencingTypes);
//		//caiduan
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.order.event.CreateOrderOnSuccessEventData");
//		entityReferencingTypes.add("com.ibm.commerce.order.event.ChangeExternalOrdersEventData");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Order-Server", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.user.beans.UserInfoDataBean");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Member-MemberManagementLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.payments.plugincontroller.plugin.PluginFactory");
//		entityReferencingTypes.add("com.ibm.commerce.payments.plugincontroller.beans.ObjectModuleFacadeFactory");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Payments-Plugin-Controller", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.component.objects.WebAdapterServiceAccessBean");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("Enablement-BusinessContextEngineInterface", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.giftregistry.util.GiftRegistryUtils");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("GiftRegistry-InfrastructureLogic", entityReferencingTypes);
//		entityReferencingTypes = new HashSet<String>();
//		entityReferencingTypes.add("com.ibm.commerce.giftregistry.registry.GiftRegistryRules");
//		PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.put("GiftRegistry-BaseComponentLogic", entityReferencingTypes);
	}
	private static final Collection<String> PORT_EXEMPT_ENTITY_REFERENCING_TYPES;
	static {
		PORT_EXEMPT_ENTITY_REFERENCING_TYPES = new HashSet<String>();
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.datatype.AbstractEntityAccessBeanFinderResult");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.edp.model.AtomicPaymentFactory");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.edp.model.EDPOrderFactory");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.edp.model.PaymentInstructionFactory");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.edp.model.ReleaseFactory");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.edp.refunds.RefundAtomicFactory");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.edp.refunds.RefundFactory");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.edp.refunds.RefundInstructionFactory");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.order.calculation.RegistryCalculationRangeAccessBean");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.order.calculation.RegistryCalculationRuleAccessBean");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.registry.ExtendedTermConditionCopy");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.order.beans.ReluctantOrderAccessBean");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.order.beans.ReluctantOrderItemAccessBean");
////		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.registry.StoreCopy");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.entityadmin.EntityAdminTaskCmdImpl");
////		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.price.utils.RefreshOnceCatalogEntryShippingAccessBean");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.user.objimpl.UserAdminAccessBean");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.user.beansrc.CachedExtendedMemberRelationshipsAccessBean");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.order.beansrc.CachedExtendedOfferAccessBean");
//		PORT_EXEMPT_ENTITY_REFERENCING_TYPES.add("com.ibm.commerce.common.beansrc.CachedExtendedSupportedLanguageAccessBean");
	}
	
	public static void loadPredefinedEntityReferences(ApplicationInfo applicationInfo, IWorkspaceRoot workspaceRoot) {
		Collection<String> predefinedProjects = PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.keySet();
		for (String predefinedProject : predefinedProjects) {
			ProjectInfo projectInfo = applicationInfo.getProjectInfo(workspaceRoot.getProject(predefinedProject));
			Collection<String> predefinedEntityReferencingTypes = PREDEFINED_INDIRECT_ENTITY_REFERENCING_TYPES.get(predefinedProject);
			for (String predefinedEntityReferencingType : predefinedEntityReferencingTypes) {
				try {
					IType type = projectInfo.getJavaProject().findType(predefinedEntityReferencingType);
					if (type == null) {
						System.out.println("bad predefined type: "+predefinedEntityReferencingType);
					}
					ApplicationInfoUtil.addJpaStubTypeMapping(applicationInfo, type);
					projectInfo.addIndirectEntityReferencingType(predefinedEntityReferencingType);
				}
				catch (JavaModelException e) {
					System.out.println("[PD]bad predefined type: "+predefinedEntityReferencingType);
					System.out.println("[PD]bad predefined project: "+predefinedProject);
					e.printStackTrace();
				}
			}
		}
	}
	
	public static boolean isPortExemptEntityReferencingType(IType type) {
		return FinderResultCacheUtil.isFinderResultCacheUtil(type) || PORT_EXEMPT_ENTITY_REFERENCING_TYPES.contains(type.getFullyQualifiedName('.'));
	}
}
