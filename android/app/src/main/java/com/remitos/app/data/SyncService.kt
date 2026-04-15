package com.remitos.app.data

import android.content.Context
import android.util.Log
import com.remitos.app.data.db.AppDatabase
import com.remitos.app.data.db.entity.InboundNoteEntity
import com.remitos.app.data.db.entity.OutboundLineEntity
import com.remitos.app.data.db.entity.OutboundLineEditHistoryEntity
import com.remitos.app.data.db.entity.OutboundLineStatusHistoryEntity
import com.remitos.app.data.db.entity.OutboundListEntity
import com.remitos.app.network.ApiClient
import com.remitos.app.network.RemitosApiService
import com.remitos.app.network.SyncEditHistoryDto
import com.remitos.app.network.SyncInboundNoteDto
import com.remitos.app.network.SyncOutboundLineDto
import com.remitos.app.network.SyncOutboundListDto
import com.remitos.app.network.SyncRequest
import com.remitos.app.network.SyncResponse
import com.remitos.app.network.SyncStatusHistoryDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncService(
    private val context: Context,
    private val authManager: AuthManager,
    private val db: AppDatabase
) {
    companion object {
        private const val TAG = "SyncService"
    }

    suspend fun performFullSync(lastSyncTimestamp: Long): SyncResult = withContext(Dispatchers.IO) {
        try {
            val apiService = getApiService() ?: return@withContext SyncResult.Error("Not authenticated")

            val inboundDao = db.inboundDao()
            val outboundDao = db.outboundDao()

            val unsyncedNotes = inboundDao.getUnsynced()
            val unsyncedLists = outboundDao.getUnsyncedLists()
            val unsyncedStatusHistory = outboundDao.getUnsyncedStatusHistory()
            val unsyncedEditHistory = outboundDao.getUnsyncedEditHistory()

            val syncInboundNotes = unsyncedNotes.map { it.toSyncDto() }
            val syncOutboundLists = mutableListOf<SyncOutboundListDto>()

            for (list in unsyncedLists) {
                val lines = outboundDao.getLinesForListSync(list.id)
                val syncLines = lines.map { line ->
                    val noteCloudId = line.cloudId
                        ?: inboundDao.getInboundNote(line.inboundNoteId)?.cloudId
                    SyncOutboundLineDto(
                        localId = line.id,
                        cloudId = line.cloudId,
                        localInboundNoteId = line.inboundNoteId,
                        inboundNoteCloudId = noteCloudId,
                        packageQty = line.packageQty,
                        allocatedPackageIds = line.allocatedPackageIds,
                        deliveryNumber = line.deliveryNumber,
                        recipientNombre = line.recipientNombre,
                        recipientApellido = line.recipientApellido,
                        recipientDireccion = line.recipientDireccion,
                        recipientTelefono = line.recipientTelefono,
                        status = line.status,
                        deliveredQty = line.deliveredQty,
                        returnedQty = line.returnedQty,
                        missingQty = line.missingQty
                    )
                }
                syncOutboundLists.add(list.toSyncDto(syncLines))
            }

            val syncStatusHistory = unsyncedStatusHistory.map { it.toSyncDto() }
            val syncEditHistory = unsyncedEditHistory.map { it.toSyncDto() }

            val request = SyncRequest(
                lastSyncTimestamp = lastSyncTimestamp,
                inboundNotes = syncInboundNotes,
                outboundLists = syncOutboundLists,
                statusHistory = syncStatusHistory,
                editHistory = syncEditHistory
            )

            val response = apiService.sync(request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Sync failed: HTTP ${response.code()} - $errorBody")
                return@withContext SyncResult.Error("HTTP ${response.code()}: $errorBody")
            }

            val syncResponse = response.body() ?: return@withContext SyncResult.Error("Empty response body")

            applyServerChanges(syncResponse)
            markLocalAsSynced(syncResponse)

            SyncResult.Success(syncResponse.serverTimestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            SyncResult.Error(e.message ?: "Unknown sync error")
        }
    }

    private suspend fun applyServerChanges(response: SyncResponse) {
        val inboundDao = db.inboundDao()
        val outboundDao = db.outboundDao()
        val timestamp = response.serverTimestamp

        for (serverNote in response.inboundNotes) {
            val existing = serverNote.cloudId?.let { inboundDao.getByCloudId(it) }
            if (existing != null) {
                val updated = existing.copy(
                    senderCuit = serverNote.cuitRemitente ?: existing.senderCuit,
                    senderNombre = serverNote.nombreRemitente ?: existing.senderNombre,
                    senderApellido = serverNote.apellidoRemitente ?: existing.senderApellido,
                    destNombre = serverNote.nombreDestinatario ?: existing.destNombre,
                    destApellido = serverNote.apellidoDestinatario ?: existing.destApellido,
                    destDireccion = serverNote.direccionDestinatario ?: existing.destDireccion,
                    destTelefono = serverNote.telefonoDestinatario ?: existing.destTelefono,
                    cantBultosTotal = serverNote.cantBultosTotal,
                    remitoNumCliente = serverNote.remitoNumCliente,
                    remitoNumInterno = serverNote.remitoNumInterno ?: existing.remitoNumInterno,
                    status = serverNote.status,
                    updatedAt = timestamp,
                    needsSync = false,
                    lastSyncedAt = timestamp
                )
                inboundDao.updateInbound(updated)
            } else {
                val newNote = InboundNoteEntity(
                    cloudId = serverNote.cloudId,
                    senderCuit = serverNote.cuitRemitente ?: "",
                    senderNombre = serverNote.nombreRemitente ?: "",
                    senderApellido = serverNote.apellidoRemitente ?: "",
                    destNombre = serverNote.nombreDestinatario ?: "",
                    destApellido = serverNote.apellidoDestinatario ?: "",
                    destDireccion = serverNote.direccionDestinatario ?: "",
                    destTelefono = serverNote.telefonoDestinatario ?: "",
                    cantBultosTotal = serverNote.cantBultosTotal,
                    remitoNumCliente = serverNote.remitoNumCliente,
                    remitoNumInterno = serverNote.remitoNumInterno ?: "",
                    status = serverNote.status,
                    scanImagePath = null,
                    ocrTextBlob = null,
                    ocrConfidenceJson = null,
                    createdAt = serverNote.createdAt.takeIf { it > 0 } ?: timestamp,
                    updatedAt = timestamp,
                    needsSync = false,
                    lastSyncedAt = timestamp
                )
                inboundDao.insertInbound(newNote)
            }
        }

        for (serverList in response.outboundLists) {
            val existing = serverList.cloudId?.let { outboundDao.getListByCloudId(it) }
            if (existing != null) {
                val updated = existing.copy(
                    listNumber = serverList.listNumber ?: existing.listNumber,
                    issueDate = serverList.issueDate,
                    driverNombre = serverList.driverNombre ?: existing.driverNombre,
                    driverApellido = serverList.driverApellido ?: existing.driverApellido,
                    checklistSignaturePath = serverList.checklistSignaturePath ?: existing.checklistSignaturePath,
                    checklistSignedAt = serverList.checklistSignedAt ?: existing.checklistSignedAt,
                    status = serverList.status,
                    needsSync = false,
                    lastSyncedAt = timestamp
                )
                outboundDao.updateOutboundList(updated)
            } else {
                val newList = OutboundListEntity(
                    cloudId = serverList.cloudId,
                    listNumber = serverList.listNumber ?: 0,
                    issueDate = serverList.issueDate,
                    driverNombre = serverList.driverNombre ?: "",
                    driverApellido = serverList.driverApellido ?: "",
                    checklistSignaturePath = serverList.checklistSignaturePath,
                    checklistSignedAt = serverList.checklistSignedAt,
                    status = serverList.status,
                    needsSync = false,
                    lastSyncedAt = timestamp
                )
                outboundDao.upsertList(newList)
            }

            for (serverLine in serverList.lines ?: emptyList()) {
                val existingLine = serverLine.cloudId?.let { outboundDao.getLineByCloudId(it) }
                if (existingLine != null) {
                    val updated = existingLine.copy(
                        deliveryNumber = serverLine.deliveryNumber,
                        recipientNombre = serverLine.recipientNombre,
                        recipientApellido = serverLine.recipientApellido,
                        recipientDireccion = serverLine.recipientDireccion,
                        recipientTelefono = serverLine.recipientTelefono,
                        packageQty = serverLine.packageQty,
                        status = serverLine.status,
                        deliveredQty = serverLine.deliveredQty,
                        returnedQty = serverLine.returnedQty,
                        missingQty = serverLine.missingQty,
                        needsSync = false,
                        lastSyncedAt = timestamp
                    )
                    outboundDao.updateOutboundLine(updated)
                }
            }
        }
    }

    private suspend fun markLocalAsSynced(response: SyncResponse) {
        val inboundDao = db.inboundDao()
        val outboundDao = db.outboundDao()
        val timestamp = response.serverTimestamp

        for (mapping in response.idMappings.inboundNotes) {
            inboundDao.markSynced(mapping.localId, mapping.cloudId, timestamp)
        }

        for (mapping in response.idMappings.outboundLists) {
            outboundDao.markListSynced(mapping.localId, mapping.cloudId, timestamp)
        }

        for (mapping in response.idMappings.outboundLines) {
            outboundDao.markLineSynced(mapping.localId, mapping.cloudId, timestamp)
        }

        val syncedStatusIds = outboundDao.getUnsyncedStatusHistory().map { it.id }
        if (syncedStatusIds.isNotEmpty()) {
            outboundDao.markStatusHistorySynced(syncedStatusIds)
        }

        val syncedEditIds = outboundDao.getUnsyncedEditHistory().map { it.id }
        if (syncedEditIds.isNotEmpty()) {
            outboundDao.markEditHistorySynced(syncedEditIds)
        }
    }

    private suspend fun getApiService(): RemitosApiService? {
        return try {
            ApiClient.getApiService(authManager)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create API service", e)
            null
        }
    }

    sealed class SyncResult {
        data class Success(val serverTimestamp: Long) : SyncResult()
        data class Error(val message: String) : SyncResult()
    }
}

private fun InboundNoteEntity.toSyncDto() = SyncInboundNoteDto(
    localId = id,
    cloudId = cloudId,
    remitoNumCliente = remitoNumCliente,
    remitoNumInterno = remitoNumInterno,
    cantBultosTotal = cantBultosTotal,
    cuitRemitente = senderCuit,
    nombreRemitente = senderNombre,
    apellidoRemitente = senderApellido,
    nombreDestinatario = destNombre,
    apellidoDestinatario = destApellido,
    direccionDestinatario = destDireccion,
    telefonoDestinatario = destTelefono,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun OutboundListEntity.toSyncDto(lines: List<SyncOutboundLineDto>) = SyncOutboundListDto(
    localId = id,
    cloudId = cloudId,
    listNumber = listNumber,
    issueDate = issueDate,
    driverNombre = driverNombre,
    driverApellido = driverApellido,
    status = status,
    lines = lines,
    checklistSignaturePath = checklistSignaturePath,
    checklistSignedAt = checklistSignedAt
)

private fun OutboundLineStatusHistoryEntity.toSyncDto() = SyncStatusHistoryDto(
    localId = id,
    localOutboundLineId = outboundLineId,
    status = status,
    createdAt = createdAt
)

private fun OutboundLineEditHistoryEntity.toSyncDto() = SyncEditHistoryDto(
    localId = id,
    localOutboundLineId = outboundLineId,
    fieldName = fieldName,
    oldValue = oldValue,
    newValue = newValue,
    reason = reason,
    createdAt = createdAt
)