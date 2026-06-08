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
    @SerializedName("credit_type") val creditType: String = ""
)

// ─── Stock Transfer ───

data class StockTransfer(
    val id: Int = 0,
    @SerializedName("st_number") val stNumber: String = "",
    @SerializedName("from_warehouse_name") val fromWarehouseName: String = "",
    @SerializedName("to_warehouse_name") val toWarehouseName: String = "",
    @SerializedName("fsm_status") val fsmStatus: String = ""
)
