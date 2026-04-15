package models

type SyncInboundNote struct {
	LocalID               int64  `json:"local_id"`
	CloudID               string `json:"cloud_id"`
	RemitoNumCliente      string `json:"remito_num_cliente"`
	RemitoNumInterno      string `json:"remito_num_interno"`
	CantBultosTotal       int    `json:"cant_bultos_total"`
	CuitRemitente         string `json:"cuit_remitente"`
	NombreRemitente       string `json:"nombre_remitente"`
	ApellidoRemitente     string `json:"apellido_remitente"`
	NombreDestinatario    string `json:"nombre_destinatario"`
	ApellidoDestinatario  string `json:"apellido_destinatario"`
	DireccionDestinatario string `json:"direccion_destinatario"`
	TelefonoDestinatario  string `json:"telefono_destinatario"`
	Status                string `json:"status"`
	CreatedAt             int64  `json:"created_at"`
	UpdatedAt             int64  `json:"updated_at"`
}

type SyncOutboundLine struct {
	LocalID             int64  `json:"local_id"`
	CloudID             string `json:"cloud_id"`
	LocalInboundNoteID  int64  `json:"local_inbound_note_id"`
	InboundNoteCloudID  string `json:"inbound_note_cloud_id"`
	PackageQty          int    `json:"package_qty"`
	AllocatedPackageIDs string `json:"allocated_package_ids"`
	DeliveryNumber      string `json:"delivery_number"`
	RecipientNombre     string `json:"recipient_nombre"`
	RecipientApellido   string `json:"recipient_apellido"`
	RecipientDireccion  string `json:"recipient_direccion"`
	RecipientTelefono   string `json:"recipient_telefono"`
	Status              string `json:"status"`
	DeliveredQty        int    `json:"delivered_qty"`
	ReturnedQty         int    `json:"returned_qty"`
	MissingQty          int    `json:"missing_qty"`
}

type SyncOutboundList struct {
	LocalID                int64              `json:"local_id"`
	CloudID                string             `json:"cloud_id"`
	ListNumber             int64              `json:"list_number"`
	IssueDate              int64              `json:"issue_date"`
	DriverNombre           string             `json:"driver_nombre"`
	DriverApellido         string             `json:"driver_apellido"`
	Status                 string             `json:"status"`
	Lines                  []SyncOutboundLine `json:"lines"`
	ChecklistSignaturePath string             `json:"checklist_signature_path"`
	ChecklistSignedAt      *int64             `json:"checklist_signed_at"`
}

type SyncStatusHistory struct {
	LocalID             int64  `json:"local_id"`
	LocalOutboundLineID int64  `json:"local_outbound_line_id"`
	Status              string `json:"status"`
	CreatedAt           int64  `json:"created_at"`
}

type SyncEditHistory struct {
	LocalID             int64  `json:"local_id"`
	LocalOutboundLineID int64  `json:"local_outbound_line_id"`
	FieldName           string `json:"field_name"`
	OldValue            string `json:"old_value"`
	NewValue            string `json:"new_value"`
	Reason              string `json:"reason"`
	CreatedAt           int64  `json:"created_at"`
}

type SyncRequest struct {
	LastSyncTimestamp int64               `json:"last_sync_timestamp"`
	InboundNotes      []SyncInboundNote   `json:"inbound_notes"`
	OutboundLists     []SyncOutboundList  `json:"outbound_lists"`
	StatusHistory     []SyncStatusHistory `json:"status_history"`
	EditHistory       []SyncEditHistory   `json:"edit_history"`
}

type IdMapping struct {
	LocalID int64  `json:"local_id"`
	CloudID string `json:"cloud_id"`
}

type SyncIdMappings struct {
	InboundNotes  []IdMapping `json:"inbound_notes"`
	OutboundLists []IdMapping `json:"outbound_lists"`
	OutboundLines []IdMapping `json:"outbound_lines"`
}

type SyncResponse struct {
	ServerTimestamp int64              `json:"server_timestamp"`
	InboundNotes    []SyncInboundNote  `json:"inbound_notes"`
	OutboundLists   []SyncOutboundList `json:"outbound_lists"`
	IdMappings      SyncIdMappings     `json:"id_mappings"`
	Conflicts       []interface{}      `json:"conflicts"`
}
