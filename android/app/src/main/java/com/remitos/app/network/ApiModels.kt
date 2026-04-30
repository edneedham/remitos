package com.remitos.app.network

import com.google.gson.annotations.SerializedName

// Auth Request/Response Models

data class ApiErrorBody(
    val error: String? = null,
    val message: String? = null,
    val fields: Map<String, String>? = null
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val nombre: String? = null,
    val apellido: String? = null,
    val role: String = "driver"
)

data class LoginRequest(
    @SerializedName("company_code")
    val companyCode: String,
    
    val username: String,
    
    val password: String,
    
    @SerializedName("device_name")
    val deviceName: String? = null
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class AuthResponse(
    val token: String,
    
    @SerializedName("refresh_token")
    val refreshToken: String,
    
    @SerializedName("expires_in")
    val expiresIn: Int,
    
    val role: String? = null
)

data class ProfileDto(
    val id: String,
    val username: String,
    val email: String?,
    @SerializedName("company_id")
    val companyId: String,
    @SerializedName("company_name")
    val companyName: String,
    @SerializedName("company_code")
    val companyCode: String,
    val role: String,
)

data class RegisterDeviceRequest(
    @SerializedName("device_uuid")
    val deviceUuid: String,
    val platform: String,
    @SerializedName("warehouse_id")
    val warehouseId: String,
    val model: String? = null,
    @SerializedName("os_version")
    val osVersion: String? = null,
    @SerializedName("app_version")
    val appVersion: String? = null,
    @SerializedName("device_name")
    val deviceName: String? = null,
    val username: String? = null,
    val password: String? = null
)

data class DeviceRegistrationResponse(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("access_token")
    val accessToken: String? = null,
    @SerializedName("refresh_token")
    val refreshToken: String? = null,
    @SerializedName("expires_in")
    val expiresIn: Int? = null
)

data class WarehouseDto(
    val id: String,
    @SerializedName("company_id")
    val companyId: String,
    val name: String,
    val address: String? = null,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

// Admin Request/Response Models

data class OperatorDto(
    val id: String,
    val email: String?,
    val username: String?,
    val role: String,
    val status: String,
    @SerializedName("created_at")
    val createdAt: String
)

data class CreateOperatorRequest(
    val email: String? = null,
    val password: String
)

data class UpdateOperatorStatusRequest(
    val status: String
)

data class UpdateOperatorPasswordRequest(
    val password: String
)

// Inbound Note Models

data class InboundNoteDto(
    val id: Long? = null,
    
    @SerializedName("remito_num_cliente")
    val remitoNumCliente: String,
    
    @SerializedName("remito_num_interno")
    val remitoNumInterno: String? = null,
    
    @SerializedName("cant_bultos_total")
    val cantBultosTotal: Int,
    
    @SerializedName("cuit_remitente")
    val cuitRemitente: String? = null,
    
    @SerializedName("nombre_remitente")
    val nombreRemitente: String? = null,
    
    @SerializedName("apellido_remitente")
    val apellidoRemitente: String? = null,
    
    @SerializedName("nombre_destinatario")
    val nombreDestinatario: String? = null,
    
    @SerializedName("apellido_destinatario")
    val apellidoDestinatario: String? = null,
    
    @SerializedName("direccion_destinatario")
    val direccionDestinatario: String? = null,
    
    @SerializedName("telefono_destinatario")
    val telefonoDestinatario: String? = null,
    
    val status: String = "Activa",
    
    @SerializedName("created_at")
    val createdAt: String? = null,
    
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class InboundNoteListResponse(
    val data: List<InboundNoteDto>,
    val total: Int,
    val page: Int,
    @SerializedName("per_page")
    val perPage: Int
)

// Outbound List Models

data class OutboundListDto(
    val id: Long? = null,
    
    @SerializedName("list_number")
    val listNumber: Long? = null,
    
    @SerializedName("issue_date")
    val issueDate: Long,
    
    @SerializedName("driver_nombre")
    val driverNombre: String? = null,
    
    @SerializedName("driver_apellido")
    val driverApellido: String? = null,
    
    val status: String = "Abierta",
    
    val lines: List<OutboundLineDto>? = null,
    
    @SerializedName("checklist_signature_path")
    val checklistSignaturePath: String? = null,
    
    @SerializedName("checklist_signed_at")
    val checklistSignedAt: Long? = null,
    
    @SerializedName("created_at")
    val createdAt: String? = null
)

data class OutboundLineDto(
    val id: Long? = null,
    
    @SerializedName("inbound_note_id")
    val inboundNoteId: Long,
    
    @SerializedName("package_qty")
    val packageQty: Int,
    
    @SerializedName("allocated_package_ids")
    val allocatedPackageIds: String? = null,
    
    @SerializedName("delivery_number")
    val deliveryNumber: String? = null,
    
    @SerializedName("recipient_nombre")
    val recipientNombre: String? = null,
    
    @SerializedName("recipient_apellido")
    val recipientApellido: String? = null,
    
    @SerializedName("recipient_direccion")
    val recipientDireccion: String? = null,
    
    @SerializedName("recipient_telefono")
    val recipientTelefono: String? = null,
    
    val status: String = "EnDeposito",
    
    @SerializedName("delivered_qty")
    val deliveredQty: Int = 0,
    
    @SerializedName("returned_qty")
    val returnedQty: Int = 0,
    
    @SerializedName("missing_qty")
    val missingQty: Int = 0,
    
    @SerializedName("created_at")
    val createdAt: String? = null
)

// Scan/OCR Models

data class ScanRequest(
    val text: String? = null,
    val confidence: Map<String, Double>? = null
)

data class ScanResponse(
    val text: String,
    val fields: Map<String, String>,
    val confidence: Map<String, Double>,
    val source: String,  // "mlkit", "cloud_vision"
    @SerializedName("processed_at")
    val processedAt: String
)

// Sync Models

data class SyncRequest(
    @SerializedName("last_sync_timestamp")
    val lastSyncTimestamp: Long,
    
    @SerializedName("inbound_notes")
    val inboundNotes: List<SyncInboundNoteDto>,
    
    @SerializedName("outbound_lists")
    val outboundLists: List<SyncOutboundListDto>,
    
    @SerializedName("status_history")
    val statusHistory: List<SyncStatusHistoryDto>,
    
    @SerializedName("edit_history")
    val editHistory: List<SyncEditHistoryDto>
)

data class SyncInboundNoteDto(
    @SerializedName("local_id")
    val localId: Long,
    
    @SerializedName("cloud_id")
    val cloudId: String? = null,
    
    @SerializedName("remito_num_cliente")
    val remitoNumCliente: String,
    
    @SerializedName("remito_num_interno")
    val remitoNumInterno: String? = null,
    
    @SerializedName("cant_bultos_total")
    val cantBultosTotal: Int,
    
    @SerializedName("cuit_remitente")
    val cuitRemitente: String? = null,
    
    @SerializedName("nombre_remitente")
    val nombreRemitente: String? = null,
    
    @SerializedName("apellido_remitente")
    val apellidoRemitente: String? = null,
    
    @SerializedName("nombre_destinatario")
    val nombreDestinatario: String? = null,
    
    @SerializedName("apellido_destinatario")
    val apellidoDestinatario: String? = null,
    
    @SerializedName("direccion_destinatario")
    val direccionDestinatario: String? = null,
    
    @SerializedName("telefono_destinatario")
    val telefonoDestinatario: String? = null,
    
    val status: String,
    
    @SerializedName("created_at")
    val createdAt: Long,
    
    @SerializedName("updated_at")
    val updatedAt: Long
)

data class SyncOutboundListDto(
    @SerializedName("local_id")
    val localId: Long,
    
    @SerializedName("cloud_id")
    val cloudId: String? = null,
    
    @SerializedName("list_number")
    val listNumber: Long,
    
    @SerializedName("issue_date")
    val issueDate: Long,
    
    @SerializedName("driver_nombre")
    val driverNombre: String,
    
    @SerializedName("driver_apellido")
    val driverApellido: String,
    
    val status: String,
    
    val lines: List<SyncOutboundLineDto>,
    
    @SerializedName("checklist_signature_path")
    val checklistSignaturePath: String? = null,
    
    @SerializedName("checklist_signed_at")
    val checklistSignedAt: Long? = null,
    
    @SerializedName("created_at")
    val createdAt: Long? = null
)

data class SyncOutboundLineDto(
    @SerializedName("local_id")
    val localId: Long,
    
    @SerializedName("cloud_id")
    val cloudId: String? = null,
    
    @SerializedName("local_inbound_note_id")
    val localInboundNoteId: Long,
    
    @SerializedName("inbound_note_cloud_id")
    val inboundNoteCloudId: String? = null,
    
    @SerializedName("package_qty")
    val packageQty: Int,
    
    @SerializedName("allocated_package_ids")
    val allocatedPackageIds: String? = null,
    
    @SerializedName("delivery_number")
    val deliveryNumber: String,
    
    @SerializedName("recipient_nombre")
    val recipientNombre: String,
    
    @SerializedName("recipient_apellido")
    val recipientApellido: String,
    
    @SerializedName("recipient_direccion")
    val recipientDireccion: String,
    
    @SerializedName("recipient_telefono")
    val recipientTelefono: String,
    
    val status: String,
    
    @SerializedName("delivered_qty")
    val deliveredQty: Int,
    
    @SerializedName("returned_qty")
    val returnedQty: Int,
    
    @SerializedName("missing_qty")
    val missingQty: Int
)

data class SyncStatusHistoryDto(
    @SerializedName("local_id")
    val localId: Long,
    
    @SerializedName("local_outbound_line_id")
    val localOutboundLineId: Long,
    
    val status: String,
    
    @SerializedName("created_at")
    val createdAt: Long
)

data class SyncEditHistoryDto(
    @SerializedName("local_id")
    val localId: Long,
    
    @SerializedName("local_outbound_line_id")
    val localOutboundLineId: Long,
    
    @SerializedName("field_name")
    val fieldName: String,
    
    @SerializedName("old_value")
    val oldValue: String,
    
    @SerializedName("new_value")
    val newValue: String,
    
    val reason: String,
    
    @SerializedName("created_at")
    val createdAt: Long
)

data class SyncResponse(
    @SerializedName("server_timestamp")
    val serverTimestamp: Long,
    
    @SerializedName("inbound_notes")
    val inboundNotes: List<SyncInboundNoteDto>,
    
    @SerializedName("outbound_lists")
    val outboundLists: List<SyncOutboundListDto>,
    
    @SerializedName("id_mappings")
    val idMappings: SyncIdMappings,
    
    @SerializedName("conflicts")
    val conflicts: List<ConflictDto> = emptyList()
)

data class SyncIdMappings(
    @SerializedName("inbound_notes")
    val inboundNotes: List<IdMapping> = emptyList(),
    
    @SerializedName("outbound_lists")
    val outboundLists: List<IdMapping> = emptyList(),
    
    @SerializedName("outbound_lines")
    val outboundLines: List<IdMapping> = emptyList()
)

data class IdMapping(
    @SerializedName("local_id")
    val localId: Long,
    
    @SerializedName("cloud_id")
    val cloudId: String
)

data class UserStatusResponse(
    @SerializedName("user_status")
    val userStatus: String,
    
    @SerializedName("device_status")
    val deviceStatus: String,
    
    val message: String? = null
)

data class ConflictDto(
    val type: String,
    @SerializedName("local_id")
    val localId: Long,
    @SerializedName("remote_id")
    val remoteId: Long,
    @SerializedName("local_data")
    val localData: Any,
    @SerializedName("remote_data")
    val remoteData: Any,
    @SerializedName("conflict_field")
    val conflictField: String? = null
)

// Error Response

data class ErrorResponse(
    @SerializedName("error_code")
    val errorCode: String,
    
    val message: String,
    
    @SerializedName("field_errors")
    val fieldErrors: Map<String, String>? = null
)

// Pagination

data class PaginatedResponse<T>(
    val data: List<T>,
    val total: Int,
    val page: Int,
    @SerializedName("per_page")
    val perPage: Int,
    @SerializedName("total_pages")
    val totalPages: Int
)
