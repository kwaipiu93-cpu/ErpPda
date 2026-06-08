package com.erp.pda.data.model

import com.google.gson.annotations.SerializedName

// ─── Generic Response Wrapper ───

data class ApiResponse<T>(
    val ok: Boolean = true,
    val data: T? = null,
    val error: ApiError? = null
)

data class ApiError(
    val code: String = "",
    val message: String = ""
)

// ─── Auth ───

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String = "",
    @SerializedName("refresh_token") val refreshToken: String = "",
    @SerializedName("user") val user: UserInfo? = null
)

data class UserInfo(
    val id: Int = 0,
    val email: String = "",
    @SerializedName("display_name") val displayName: String = "",
    val role: String = ""
)

data class TokenRefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class TokenRefreshResponse(
    @SerializedName("access_token") val accessToken: String = "",
    @SerializedName("refresh_token") val refreshToken: String = ""
)

// ─── Warehouse ───

data class Warehouse(
    val id: Int = 0,
    @SerializedName("name_en") val nameEn: String = "",
    @SerializedName("name_zh") val nameZh: String = ""
)

// ─── Purchase Order ───

data class PurchaseOrder(
    val id: Int = 0,
    @SerializedName("po_number") val poNumber: String = "",
    @SerializedName("supplier_id") val supplierId: Int = 0,
    @SerializedName("supplier_name") val supplierName: String = "",
    @SerializedName("warehouse_id") val warehouseId: Int = 0,
    @SerializedName("fsm_status") val fsmStatus: String = "",
    @SerializedName("currency_code") val currencyCode: String = "",
    @SerializedName("total_amount_hkd") val totalAmountHkd: Double = 0.0,
    @SerializedName("ordered_at") val orderedAt: String = ""
)

data class PurchaseOrderDetail(
    val id: Int = 0,
    @SerializedName("po_number") val poNumber: String = "",
    @SerializedName("supplier_name") val supplierName: String = "",
    @SerializedName("fsm_status") val fsmStatus: String = "",
    @SerializedName("items") val items: List<PurchaseOrderItem> = emptyList()
)

data class PurchaseOrderItem(
    val id: Int = 0,
    @SerializedName("product_id") val productId: Int = 0,
    @SerializedName("sku_code") val skuCode: String = "",
    @SerializedName("product_name") val productName: String = "",
    @SerializedName("qty_ordered") val qtyOrdered: Int = 0,
    @SerializedName("qty_received") val qtyReceived: Int = 0,
    @SerializedName("is_serial_tracked") val isSerialTracked: Boolean = false,
    @SerializedName("warranty_months") val warrantyMonths: Int = 12
)

data class ReceiveItem(
    @SerializedName("po_item_id") val poItemId: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("qty_received") val qtyReceived: Int,
    @SerializedName("unit_price_hkd") val unitPriceHkd: Double = 0.0,
    @SerializedName("serial_numbers") val serialNumbers: List<String> = emptyList()
)

data class ReceiveRequest(
    @SerializedName("warehouse_id") val warehouseId: Int,
    @SerializedName("items") val items: List<ReceiveItem>,
    @SerializedName("auto_dispatch") val autoDispatch: Boolean = false
)

// ─── Delivery Note ───

data class DeliveryNote(
    val id: Int = 0,
    @SerializedName("dn_number") val dnNumber: String = "",
    @SerializedName("invoice_id") val invoiceId: Int = 0,
    @SerializedName("invoice_number") val invoiceNumber: String = "",
    @SerializedName("customer_name") val customerName: String = "",
    @SerializedName("warehouse_id") val warehouseId: Int = 0,
    @SerializedName("fsm_status") val fsmStatus: String = ""
)

data class DeliveryNoteDetail(
    val id: Int = 0,
    @SerializedName("dn_number") val dnNumber: String = "",
    @SerializedName("customer_name") val customerName: String = "",
    @SerializedName("fsm_status") val fsmStatus: String = "",
    @SerializedName("items") val items: List<DeliveryNoteItem> = emptyList()
)

data class DeliveryNoteItem(
    val id: Int = 0,
    @SerializedName("product_id") val productId: Int = 0,
    @SerializedName("sku_code") val skuCode: String = "",
    @SerializedName("product_name") val productName: String = "",
    @SerializedName("qty_to_deliver") val qtyToDeliver: Int = 0,
    @SerializedName("qty_picked") val qtyPicked: Int = 0,
    @SerializedName("qty_delivered") val qtyDelivered: Int = 0,
    @SerializedName("is_serial_tracked") val isSerialTracked: Boolean = false,
    @SerializedName("warranty_months") val warrantyMonths: Int = 12
)

// ─── Serial Lookup ───

data class SerialLookupResult(
    @SerializedName("serial_number") val serialNumber: String = "",
    val status: String = "",
    @SerializedName("product_name") val productName: String = "",
    @SerializedName("sku_code") val skuCode: String = "",
    val brand: String = "",
    @SerializedName("supplier_warranty_expiry") val supplierWarrantyExpiry: String? = null,
    @SerializedName("customer_warranty_expiry") val customerWarrantyExpiry: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    @SerializedName("sale_date") val saleDate: String? = null,
    @SerializedName("supplier_name") val supplierName: String? = null,
    @SerializedName("po_number") val poNumber: String? = null,
    @SerializedName("po_date") val poDate: String? = null
)

// ─── Product / Stock ───

data class Product(
    val id: Int = 0,
    @SerializedName("sku_code") val skuCode: String = "",
    @SerializedName("name_zh") val nameZh: String = "",
    @SerializedName("name_en") val nameEn: String = "",
    @SerializedName("is_serial_tracked") val isSerialTracked: Boolean = false
)

data class StockInfo(
    @SerializedName("product_id") val productId: Int = 0,
    @SerializedName("warehouse_id") val warehouseId: Int = 0,
    @SerializedName("warehouse_name") val warehouseName: String = "",
    @SerializedName("physical_stock") val physicalStock: Int = 0,
    @SerializedName("allocated_stock") val allocatedStock: Int = 0,
    @SerializedName("committed_stock") val committedStock: Int = 0,
    @SerializedName("available") val available: Int = 0
)

// ─── Stocktake ───

data class StocktakeRequest(
    @SerializedName("warehouse_id") val warehouseId: Int,
    val remarks: String = ""
)

data class StocktakeTask(
    val id: Int = 0,
    @SerializedName("stk_number") val stkNumber: String = "",
    @SerializedName("warehouse_id") val warehouseId: Int = 0,
    @SerializedName("fsm_status") val fsmStatus: String = ""
)

data class StocktakeScanItem(
    @SerializedName("serial_number") val serialNumber: String,
    @SerializedName("product_id") val productId: Int = 0
)

// ─── Credit Note ───

data class CreditNote(
    val id: Int = 0,
    @SerializedName("cn_number") val cnNumber: String = "",
    @SerializedName("customer_name") val customerName: String = "",
    @SerializedName("fsm_status") val fsmStatus: String = "",
    @SerializedName("credit_type") val creditType: String = "",
    @SerializedName("total_amount_hkd") val totalAmountHkd: Double = 0.0,
    @SerializedName("has_goods_return") val hasGoodsReturn: Boolean = false,
    @SerializedName("items") val items: List<CreditNoteItem> = emptyList()
)

data class CreditNoteItem(
    val id: Int = 0,
    @SerializedName("product_id") val productId: Int = 0,
    @SerializedName("sku_code") val skuCode: String = "",
    @SerializedName("product_name") val productName: String = "",
    val qty: Int = 0,
    @SerializedName("unit_price") val unitPrice: Double = 0.0,
    @SerializedName("is_serial_tracked") val isSerialTracked: Boolean = false,
    @SerializedName("is_defective") val isDefective: Boolean = false
)

// ─── Stock Transfer ───

data class StockTransfer(
    val id: Int = 0,
    @SerializedName("st_number") val stNumber: String = "",
    @SerializedName("from_warehouse_id") val fromWarehouseId: Int = 0,
    @SerializedName("from_warehouse_name") val fromWarehouseName: String = "",
    @SerializedName("to_warehouse_id") val toWarehouseId: Int = 0,
    @SerializedName("to_warehouse_name") val toWarehouseName: String = "",
    @SerializedName("fsm_status") val fsmStatus: String = "",
    @SerializedName("items") val items: List<StockTransferItem> = emptyList()
)

data class StockTransferItem(
    val id: Int = 0,
    @SerializedName("product_id") val productId: Int = 0,
    @SerializedName("sku_code") val skuCode: String = "",
    @SerializedName("product_name") val productName: String = "",
    val qty: Int = 0,
    @SerializedName("qty_received") val qtyReceived: Int = 0,
    @SerializedName("is_serial_tracked") val isSerialTracked: Boolean = false
)

// ─── Sales / Checkout ───

data class CustomerSummary(
    val id: Int = 0,
    @SerializedName("company_name_zh") val companyNameZh: String = "",
    @SerializedName("company_name_en") val companyNameEn: String = "",
    @SerializedName("customer_type") val customerType: String = "",
    @SerializedName("contact_phone") val contactPhone: String = "",
    @SerializedName("contact_person") val contactPerson: String = "",
    @SerializedName("outstanding_hkd") val outstandingHkd: Double = 0.0,
    @SerializedName("credit_limit_hkd") val creditLimitHkd: Double = 0.0
)

data class CustomerDetail(
    val id: Int = 0,
    @SerializedName("company_name_zh") val companyNameZh: String = "",
    @SerializedName("company_name_en") val companyNameEn: String = "",
    @SerializedName("customer_type") val customerType: String = "",
    @SerializedName("br_number") val brNumber: String = "",
    @SerializedName("contact_person") val contactPerson: String = "",
    @SerializedName("contact_phone") val contactPhone: String = "",
    @SerializedName("contact_email") val contactEmail: String = "",
    @SerializedName("billing_address") val billingAddress: String = "",
    @SerializedName("shipping_address") val shippingAddress: String = "",
    @SerializedName("credit_term_days") val creditTermDays: Int = 0,
    @SerializedName("credit_limit_hkd") val creditLimitHkd: Double = 0.0,
    @SerializedName("outstanding_hkd") val outstandingHkd: Double = 0.0,
    @SerializedName("is_active") val isActive: Boolean = true
)

data class B2cCheckoutRequest(
    @SerializedName("customer_id") val customerId: Int,
    @SerializedName("warehouse_id") val warehouseId: Int,
    @SerializedName("payment_method") val paymentMethod: String = "FPS",
    @SerializedName("reference_number") val referenceNumber: String = "",
    val items: List<B2cCheckoutItem>
)

data class B2cCheckoutItem(
    @SerializedName("product_id") val productId: Int,
    val qty: Int = 1,
    @SerializedName("unit_price") val unitPrice: Double = 0.0,
    @SerializedName("serial_numbers") val serialNumbers: List<String> = emptyList()
)

data class B2cCheckoutResponse(
    @SerializedName("invoice_id") val invoiceId: Int = 0,
    @SerializedName("invoice_number") val invoiceNumber: String = "",
    @SerializedName("total_hkd") val totalHkd: Double = 0.0
)

// ─── Invoice Lookup ───

data class InvoiceSummary(
    val id: Int = 0,
    @SerializedName("invoice_number") val invoiceNumber: String = "",
    @SerializedName("document_type") val documentType: String = "",
    @SerializedName("customer_id") val customerId: Int = 0,
    @SerializedName("customer_name") val customerName: String = "",
    @SerializedName("grand_total_hkd") val grandTotalHkd: Double = 0.0,
    @SerializedName("payment_status") val paymentStatus: String = "",
    @SerializedName("lifecycle_status") val lifecycleStatus: String = "",
    @SerializedName("shipping_status") val shippingStatus: String = "",
    @SerializedName("issue_date") val issueDate: String = ""
)

data class InvoiceDetail(
    val id: Int = 0,
    @SerializedName("invoice_number") val invoiceNumber: String = "",
    @SerializedName("document_type") val documentType: String = "",
    @SerializedName("customer_name") val customerName: String = "",
    @SerializedName("grand_total_hkd") val grandTotalHkd: Double = 0.0,
    @SerializedName("paid_amount_hkd") val paidAmountHkd: Double = 0.0,
    @SerializedName("discount_amount") val discountAmount: Double = 0.0,
    @SerializedName("delivery_charge") val deliveryCharge: Double = 0.0,
    @SerializedName("payment_status") val paymentStatus: String = "",
    @SerializedName("lifecycle_status") val lifecycleStatus: String = "",
    @SerializedName("shipping_status") val shippingStatus: String = "",
    @SerializedName("issue_date") val issueDate: String = "",
    @SerializedName("due_date") val dueDate: String = "",
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("items") val items: List<InvoiceDetailItem> = emptyList()
)

data class InvoiceDetailItem(
    val id: Int = 0,
    @SerializedName("product_id") val productId: Int = 0,
    @SerializedName("sku_code") val skuCode: String = "",
    @SerializedName("product_name") val productName: String = "",
    val qty: Int = 0,
    @SerializedName("qty_shipped") val qtyShipped: Int = 0,
    @SerializedName("unit_price") val unitPrice: Double = 0.0,
    @SerializedName("line_total_hkd") val lineTotalHkd: Double = 0.0
)

// ─── Quotation Creation ───

data class CreateQuotationRequest(
    @SerializedName("customer_id") val customerId: Int,
    @SerializedName("warehouse_id") val warehouseId: Int,
    @SerializedName("project_id") val projectId: Int? = null,
    val items: List<QuoteItemRequest>,
    val notes: String? = null
)

data class QuoteItemRequest(
    @SerializedName("product_id") val productId: Int,
    val qty: Int = 1,
    @SerializedName("unit_price") val unitPrice: Double = 0.0
)

data class QuotationResponse(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("invoice_number") val invoiceNumber: String = "",
    @SerializedName("grand_total_hkd") val grandTotalHkd: Double = 0.0
)

// ─── Payment Recording ───

data class RecordPaymentRequest(
    @SerializedName("customer_id") val customerId: Int,
    @SerializedName("amount_hkd") val amountHkd: Double,
    @SerializedName("payment_method") val paymentMethod: String = "FPS",
    @SerializedName("reference_number") val referenceNumber: String = "",
    @SerializedName("invoice_id") val invoiceId: Int? = null,
    @SerializedName("received_at") val receivedAt: String = ""
)

// ─── Supplier ───

data class SupplierSummary(
    val id: Int = 0,
    @SerializedName("company_name_en") val companyNameEn: String = "",
    @SerializedName("company_name_zh") val companyNameZh: String = "",
    @SerializedName("supplier_type") val supplierType: String = "",
    @SerializedName("currency_code") val currencyCode: String = "HKD",
    @SerializedName("contact_person") val contactPerson: String = "",
    @SerializedName("is_active") val isActive: Boolean = true
)

// ─── PO Creation ───

data class CreatePoRequest(
    @SerializedName("supplier_id") val supplierId: Int,
    @SerializedName("warehouse_id") val warehouseId: Int,
    val items: List<PoItemRequest>,
    val notes: String? = null
)

data class PoItemRequest(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("qty_ordered") val qtyOrdered: Int = 1,
    @SerializedName("unit_price_foreign") val unitPriceForeign: Double = 0.0
)
