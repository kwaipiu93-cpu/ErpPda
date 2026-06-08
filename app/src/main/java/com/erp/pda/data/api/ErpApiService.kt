package com.erp.pda.data.api

import com.erp.pda.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ErpApiService {

    // ─── Auth ───
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: TokenRefreshRequest): Response<ApiResponse<TokenRefreshResponse>>

    // ─── Warehouses ───
    @GET("warehouses")
    suspend fun getWarehouses(): Response<ApiResponse<List<Warehouse>>>

    // ─── Purchase Orders ───
    @GET("purchase-orders")
    suspend fun getPurchaseOrders(
        @Query("status") status: String = "Ordered,Partially_Received"
    ): Response<ApiResponse<List<PurchaseOrder>>>

    @GET("purchase-orders/{id}")
    suspend fun getPurchaseOrder(@Path("id") id: Int): Response<ApiResponse<PurchaseOrderDetail>>

    @POST("purchase-orders/{id}/receive")
    suspend fun receivePurchaseOrder(
        @Path("id") id: Int,
        @Body request: ReceiveRequest
    ): Response<ApiResponse<Any>>

    // ─── Delivery Notes ───
    @GET("delivery-notes")
    suspend fun getDeliveryNotes(
        @Query("status") status: String = "Pending"
    ): Response<ApiResponse<List<DeliveryNote>>>

    @GET("delivery-notes/{id}")
    suspend fun getDeliveryNote(@Path("id") id: Int): Response<ApiResponse<DeliveryNoteDetail>>

    @POST("delivery-notes/{id}/dispatch")
    suspend fun dispatchDeliveryNote(
        @Path("id") id: Int,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ApiResponse<Any>>

    // ─── Serial Lookup ───
    @GET("serials/lookup")
    suspend fun lookupSerial(
        @Query("serial") serialNumber: String
    ): Response<ApiResponse<SerialLookupResult>>

    // ─── Products ───
    @GET("products/search")
    suspend fun searchProducts(
        @Query("q") keyword: String,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<List<Product>>>

    @GET("products/{id}/stock")
    suspend fun getProductStock(@Path("id") productId: Int): Response<ApiResponse<List<StockInfo>>>

    // ─── Stocktake ───
    @POST("stocktake-tasks")
    suspend fun createStocktake(@Body request: StocktakeRequest): Response<ApiResponse<StocktakeTask>>

    @POST("stocktake-tasks/{id}/scan")
    suspend fun scanStocktake(
        @Path("id") taskId: Int,
        @Body item: StocktakeScanItem
    ): Response<ApiResponse<Any>>

    @GET("stocktake-tasks/{id}/diff")
    suspend fun getStocktakeDiff(@Path("id") taskId: Int): Response<ApiResponse<Any>>

    @POST("stocktake-tasks/{id}/complete")
    suspend fun completeStocktake(@Path("id") taskId: Int): Response<ApiResponse<Any>>

    // ─── Credit Notes ───
    @GET("credit-notes")
    suspend fun getCreditNotes(
        @Query("status") status: String = "Draft"
    ): Response<ApiResponse<List<CreditNote>>>

    @GET("credit-notes/{id}")
    suspend fun getCreditNoteDetail(@Path("id") id: Int): Response<ApiResponse<CreditNote>>

    @POST("credit-notes/{id}/confirm")
    suspend fun confirmCreditNote(
        @Path("id") id: Int,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ApiResponse<Any>>

    // ─── Stock Transfers ───
    @GET("stock-transfers")
    suspend fun getStockTransfers(
        @Query("status") status: String = "Transiting"
    ): Response<ApiResponse<List<StockTransfer>>>

    @GET("stock-transfers/{id}")
    suspend fun getStockTransferDetail(@Path("id") id: Int): Response<ApiResponse<StockTransfer>>

    @POST("stock-transfers/{id}/receive")
    suspend fun receiveStockTransfer(
        @Path("id") id: Int,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ApiResponse<Any>>

    // ─── Sales ───
    @GET("invoices")
    suspend fun getInvoices(
        @Query("invoice_number") invoiceNumber: String? = null,
        @Query("status") status: String? = null,
        @Query("document_type") docType: String? = null
    ): Response<ApiResponse<List<InvoiceSummary>>>

    @GET("invoices/{id}")
    suspend fun getInvoiceDetail(@Path("id") id: Int): Response<ApiResponse<InvoiceDetail>>

    @POST("checkout/b2c-fast")
    suspend fun b2cFastCheckout(@Body request: B2cCheckoutRequest): Response<ApiResponse<B2cCheckoutResponse>>

    @GET("customers/search")
    suspend fun searchCustomers(@Query("q") query: String): Response<ApiResponse<List<CustomerSummary>>>

    @GET("customers")
    suspend fun getAllCustomers(): Response<ApiResponse<List<CustomerSummary>>>

    @GET("customers/{id}")
    suspend fun getCustomerDetail(@Path("id") id: Int): Response<ApiResponse<CustomerDetail>>

    @POST("customers")
    suspend fun createCustomer(@Body request: CreateCustomerRequest): Response<ApiResponse<CustomerSummary>>

    // ─── Quotations ───
    @GET("quotations")
    suspend fun getQuotations(
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<InvoiceSummary>>>

    @GET("quotations/{id}")
    suspend fun getQuotationDetail(@Path("id") id: Int): Response<ApiResponse<InvoiceDetail>>

    @POST("quotations")
    suspend fun createQuotation(@Body request: CreateQuotationRequest): Response<ApiResponse<QuotationResponse>>

    @PUT("quotations/{id}")
    suspend fun updateQuotation(
        @Path("id") id: Int,
        @Body request: UpdateQuotationRequest
    ): Response<ApiResponse<Any>>

    // ─── Invoices (update) ───

    @PUT("invoices/{id}")
    suspend fun updateInvoice(
        @Path("id") id: Int,
        @Body request: UpdateInvoiceRequest
    ): Response<ApiResponse<Any>>

    // ─── Payments ───
    @POST("payments")
    suspend fun recordPayment(@Body request: RecordPaymentRequest): Response<ApiResponse<Any>>

    // ─── Suppliers ───
    @GET("suppliers")
    suspend fun getSuppliers(): Response<ApiResponse<List<SupplierSummary>>>

    // ─── Purchase Orders ───
    @POST("purchase-orders")
    suspend fun createPurchaseOrder(@Body request: CreatePoRequest): Response<ApiResponse<Any>>
}
