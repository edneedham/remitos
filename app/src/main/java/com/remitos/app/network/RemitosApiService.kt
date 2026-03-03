package com.remitos.app.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface for Remitos backend.
 * All endpoints return Response<T> to allow checking HTTP status codes.
 */
interface RemitosApiService {

    // ==================== AUTH ====================

    /**
     * Register a new user account.
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    /**
     * Login with email and password.
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    /**
     * Refresh access token using refresh token.
     */
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    /**
     * Logout current user (invalidates token on backend).
     */
    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    /**
     * Get current user profile.
     */
    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserDto>

    /**
     * Register this device to a warehouse.
     */
    @POST("auth/device")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): Response<DeviceRegistrationResponse>

    /**
     * Get warehouses for the current user's company.
     * @param companyCode Optional company code to get warehouses without auth
     */
    @GET("warehouses")
    suspend fun getWarehouses(@Query("company_code") companyCode: String? = null): Response<List<WarehouseDto>>

    // ==================== INBOUND NOTES ====================

    /**
     * Get all inbound notes with optional filtering.
     * @param since Only return notes updated after this timestamp
     * @param limit Maximum number of results
     * @param offset Pagination offset
     */
    @GET("admin/inbound-notes")
    suspend fun getInboundNotes(
        @Query("since") since: Long? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<PaginatedResponse<InboundNoteDto>>

    /**
     * Get a specific inbound note by ID.
     */
    @GET("admin/inbound-notes/{id}")
    suspend fun getInboundNote(@Path("id") id: Long): Response<InboundNoteDto>

    /**
     * Create a new inbound note.
     */
    @POST("admin/inbound-notes")
    suspend fun createInboundNote(@Body note: InboundNoteDto): Response<InboundNoteDto>

    /**
     * Update an existing inbound note.
     */
    @PUT("admin/inbound-notes/{id}")
    suspend fun updateInboundNote(
        @Path("id") id: Long,
        @Body note: InboundNoteDto
    ): Response<InboundNoteDto>

    /**
     * Delete (void) an inbound note.
     */
    @DELETE("admin/inbound-notes/{id}")
    suspend fun deleteInboundNote(@Path("id") id: Long): Response<Unit>

    // ==================== OUTBOUND LISTS ====================

    /**
     * Get all outbound lists with optional filtering.
     */
    @GET("admin/outbound-lists")
    suspend fun getOutboundLists(
        @Query("since") since: Long? = null,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<PaginatedResponse<OutboundListDto>>

    /**
     * Get a specific outbound list by ID.
     */
    @GET("admin/outbound-lists/{id}")
    suspend fun getOutboundList(@Path("id") id: Long): Response<OutboundListDto>

    /**
     * Create a new outbound list.
     */
    @POST("admin/outbound-lists")
    suspend fun createOutboundList(@Body list: OutboundListDto): Response<OutboundListDto>

    /**
     * Update an existing outbound list.
     */
    @PUT("admin/outbound-lists/{id}")
    suspend fun updateOutboundList(
        @Path("id") id: Long,
        @Body list: OutboundListDto
    ): Response<OutboundListDto>

    /**
     * Close an outbound list.
     */
    @POST("admin/outbound-lists/{id}/close")
    suspend fun closeOutboundList(@Path("id") id: Long): Response<OutboundListDto>

    // ==================== SCAN/OCR ====================

    /**
     * Scan/process an image using backend OCR (Cloud Vision).
     * Use this when on-device OCR confidence is low.
     */
    @Multipart
    @POST("scan")
    suspend fun scanImage(
        @Part image: MultipartBody.Part,
        @Part("text") text: String? = null,
        @Part("confidence") confidenceJson: String? = null
    ): Response<ScanResponse>

    /**
     * Scan with client OCR data.
     * Send client OCR results; backend validates and enhances if needed.
     */
    @POST("scan")
    suspend fun scanWithClientOcr(@Body request: ScanRequest): Response<ScanResponse>

    // ==================== SYNC ====================

    /**
     * Get current user status (active/suspended).
     */
    @GET("user/status")
    suspend fun getUserStatus(): Response<UserStatusResponse>

    /**
     * Perform bidirectional sync with backend.
     * Sends local changes and receives remote changes.
     */
    @POST("sync")
    suspend fun sync(@Body request: SyncRequest): Response<SyncResponse>

    /**
     * Get sync status (for checking pending changes).
     */
    @GET("sync/status")
    suspend fun getSyncStatus(
        @Query("last_sync") lastSyncTimestamp: Long
    ): Response<Map<String, Any>>

    // ==================== IMAGES ====================

    /**
     * Upload an image for backup/audit.
     */
    @Multipart
    @POST("images/upload")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("entity_type") entityType: String,
        @Part("entity_id") entityId: Long
    ): Response<Map<String, String>>

    /**
     * Get image URL for downloading.
     */
    @GET("images/{id}/url")
    suspend fun getImageUrl(@Path("id") imageId: String): Response<Map<String, String>>
}
