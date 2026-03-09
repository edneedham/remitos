# Data Structures

This document lists the core data structures used by the app and what they represent. Each structure includes a JSON-shaped example for reference.

## InboundNoteEntity
Represents a scanned inbound delivery note (remito) with sender/destination details and OCR metadata.

```json
{
  "id": 0,
  "senderCuit": "20-12345678-9",
  "senderNombre": "Juan",
  "senderApellido": "Perez",
  "destNombre": "Maria",
  "destApellido": "Gomez",
  "destDireccion": "Calle 123",
  "destTelefono": "111111111",
  "cantBultosTotal": 3,
  "remitoNumCliente": "R-123",
  "remitoNumInterno": "RI-000123",
  "status": "activa",
  "scanImagePath": "file:///data/user/0/com.remitos.app/files/remitos/remito_20250101_120000.jpg",
  "ocrTextBlob": "...",
  "ocrConfidenceJson": "{...}",
  "createdAt": 1735747200000,
  "updatedAt": 1735747200000
}
```

## InboundPackageEntity
Represents a package/bulto tied to an inbound note, used to track availability and status.

```json
{
  "id": 0,
  "inboundNoteId": 10,
  "packageIndex": 1,
  "status": "disponible"
}
```

## InboundNoteWithAvailable
Computed view that pairs an inbound note with the number of available packages.

```json
{
  "note": { "id": 0, "senderCuit": "20-12345678-9", "status": "activa" },
  "availableCount": 2
}
```

## OutboundListEntity
Represents a delivery checklist (reparto) header with driver and status data.

```json
{
  "id": 0,
  "listNumber": 12,
  "issueDate": 1735747200000,
  "driverNombre": "Ana",
  "driverApellido": "Lopez",
  "checklistSignaturePath": null,
  "checklistSignedAt": null,
  "status": "abierta"
}
```

## OutboundLineEntity
Represents a single delivery note (remito) line within a checklist.

```json
{
  "id": 0,
  "outboundListId": 12,
  "inboundNoteId": 10,
  "deliveryNumber": "E-100",
  "recipientNombre": "Sofia",
  "recipientApellido": "Ibarra",
  "recipientDireccion": "Av. Siempre Viva 742",
  "recipientTelefono": "111111111",
  "packageQty": 3,
  "allocatedPackageIds": "1,2,3",
  "status": "en_deposito",
  "deliveredQty": 0,
  "returnedQty": 0,
  "missingQty": 0
}
```

## OutboundLineWithRemito
Read model that joins an outbound line with inbound remito identifiers.

```json
{
  "id": 0,
  "outboundListId": 12,
  "inboundNoteId": 10,
  "deliveryNumber": "E-100",
  "recipientNombre": "Sofia",
  "recipientApellido": "Ibarra",
  "recipientDireccion": "Av. Siempre Viva 742",
  "recipientTelefono": "111111111",
  "packageQty": 3,
  "allocatedPackageIds": "1,2,3",
  "status": "en_deposito",
  "deliveredQty": 0,
  "returnedQty": 0,
  "missingQty": 0,
  "remitoNumCliente": "R-123",
  "remitoNumInterno": "RI-000123"
}
```

## OutboundLineStatusHistoryEntity
Append-only audit log of checklist line status changes.

```json
{
  "id": 0,
  "outboundLineId": 55,
  "status": "entregado",
  "createdAt": 1735747200000
}
```

## OutboundLineEditHistoryEntity
Append-only audit log of field edits for a checklist line.

```json
{
  "id": 0,
  "outboundLineId": 55,
  "fieldName": "recipient_direccion",
  "oldValue": "Calle 123",
  "newValue": "Calle 456",
  "reason": "Corrección de datos",
  "createdAt": 1735747200000
}
```

## SequenceEntity
Named sequence tracker used for generating incremental numbers (e.g., list numbers).

```json
{
  "name": "outbound_list_number",
  "nextValue": 123
}
```

## DebugLogEntity
Stores OCR/debug telemetry for troubleshooting and audits.

```json
{
  "id": 0,
  "createdAt": 1735747200000,
  "scanId": 10,
  "ocrConfidenceJson": "{...}",
  "preprocessTimeMs": 450,
  "failureReason": null,
  "imageWidth": 1600,
  "imageHeight": 1200,
  "deviceModel": "Pixel 6",
  "parsingErrorSummary": null
}
```

## OutboundSearchFilters
In-memory filters used to search outbound checklists by tokens and status filters.

```json
{
  "query": "lopez 123",
  "listStatuses": ["abierta"],
  "lineStatuses": ["en_deposito", "entregado"]
}
```

## OcrResult
OCR output with extracted fields and confidence values.

```json
{
  "text": "...",
  "fields": { "remito_num_cliente": "R-123" },
  "confidence": { "remito_num_cliente": 0.92 },
  "preprocessTimeMs": 420,
  "imageWidth": 1600,
  "imageHeight": 1200,
  "parsingErrorSummary": null
}
```

## OcrDebugInfo
Lightweight debug info used when OCR processing fails.

```json
{
  "preprocessTimeMs": 420,
  "imageWidth": 1600,
  "imageHeight": 1200
}
```
