package repository

import (
	"context"
	"database/sql"
	"fmt"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/models"
)

type SyncRepository struct {
	pool *pgxpool.Pool
}

func NewSyncRepository(pool *pgxpool.Pool) *SyncRepository {
	return &SyncRepository{pool: pool}
}

type inboundUpsertWork struct {
	note   models.SyncInboundNote
	cloudID string
	insert  bool
}

func (r *SyncRepository) UpsertInboundNotes(ctx context.Context, companyID string, notes []models.SyncInboundNote) ([]models.IdMapping, error) {
	if len(notes) == 0 {
		return nil, nil
	}

	resolved := make([]struct {
		note    models.SyncInboundNote
		cloudID string
	}, 0, len(notes))
	ids := make([]uuid.UUID, 0, len(notes))
	for _, note := range notes {
		cloudID := note.CloudID
		if cloudID == "" {
			cloudID = uuid.New().String()
		}
		parsed, err := uuid.Parse(cloudID)
		if err != nil {
			return nil, fmt.Errorf("inbound note cloud_id: %w", err)
		}
		resolved = append(resolved, struct {
			note    models.SyncInboundNote
			cloudID string
		}{note: note, cloudID: cloudID})
		ids = append(ids, parsed)
	}

	existingCompanyByCloud := make(map[string]string, len(resolved))
	if len(ids) > 0 {
		rows, err := r.pool.Query(ctx, `
			SELECT cloud_id::text, company_id::text
			FROM inbound_notes
			WHERE cloud_id = ANY($1::uuid[])
		`, ids)
		if err != nil {
			return nil, err
		}
		for rows.Next() {
			var cid, comp string
			if err := rows.Scan(&cid, &comp); err != nil {
				rows.Close()
				return nil, err
			}
			existingCompanyByCloud[cid] = comp
		}
		if err := rows.Err(); err != nil {
			return nil, err
		}
		rows.Close()
	}

	work := make([]inboundUpsertWork, 0, len(resolved))
	for _, item := range resolved {
		comp, exists := existingCompanyByCloud[item.cloudID]
		if exists && comp != companyID {
			continue
		}
		work = append(work, inboundUpsertWork{
			note:    item.note,
			cloudID: item.cloudID,
			insert:  !exists,
		})
	}

	if len(work) == 0 {
		return nil, nil
	}

	batch := &pgx.Batch{}
	const insertSQL = `
		INSERT INTO inbound_notes (
			cloud_id, company_id, remito_num_cliente, remito_num_interno,
			cant_bultos_total, cuit_remitente, nombre_remitente, apellido_remitente,
			nombre_destinatario, apellido_destinatario, direccion_destinatario, telefono_destinatario,
			status, created_at, updated_at
		) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, to_timestamp($14/1000), to_timestamp($15/1000))
		ON CONFLICT (cloud_id) DO UPDATE SET
			remito_num_cliente = EXCLUDED.remito_num_cliente,
			remito_num_interno = EXCLUDED.remito_num_interno,
			cant_bultos_total = EXCLUDED.cant_bultos_total,
			cuit_remitente = EXCLUDED.cuit_remitente,
			nombre_remitente = EXCLUDED.nombre_remitente,
			apellido_remitente = EXCLUDED.apellido_remitente,
			nombre_destinatario = EXCLUDED.nombre_destinatario,
			apellido_destinatario = EXCLUDED.apellido_destinatario,
			direccion_destinatario = EXCLUDED.direccion_destinatario,
			telefono_destinatario = EXCLUDED.telefono_destinatario,
			status = EXCLUDED.status,
			updated_at = EXCLUDED.updated_at`
	const updateSQL = `
		UPDATE inbound_notes SET
			remito_num_cliente = $3, remito_num_interno = $4,
			cant_bultos_total = $5, cuit_remitente = $6,
			nombre_remitente = $7, apellido_remitente = $8,
			nombre_destinatario = $9, apellido_destinatario = $10,
			direccion_destinatario = $11, telefono_destinatario = $12,
			status = $13, updated_at = to_timestamp($14/1000)
		WHERE cloud_id = $1 AND company_id = $2`
	for _, w := range work {
		n := w.note
		if w.insert {
			batch.Queue(insertSQL,
				w.cloudID, companyID, n.RemitoNumCliente, n.RemitoNumInterno,
				n.CantBultosTotal, n.CuitRemitente, n.NombreRemitente, n.ApellidoRemitente,
				n.NombreDestinatario, n.ApellidoDestinatario, n.DireccionDestinatario, n.TelefonoDestinatario,
				n.Status, n.CreatedAt, n.UpdatedAt,
			)
		} else {
			batch.Queue(updateSQL,
				w.cloudID, companyID, n.RemitoNumCliente, n.RemitoNumInterno,
				n.CantBultosTotal, n.CuitRemitente, n.NombreRemitente, n.ApellidoRemitente,
				n.NombreDestinatario, n.ApellidoDestinatario, n.DireccionDestinatario, n.TelefonoDestinatario,
				n.Status, n.UpdatedAt,
			)
		}
	}

	br := r.pool.SendBatch(ctx, batch)
	defer br.Close()
	for _, w := range work {
		if _, err := br.Exec(); err != nil {
			if w.insert {
				return nil, fmt.Errorf("failed to upsert inbound note: %w", err)
			}
			return nil, fmt.Errorf("failed to update inbound note: %w", err)
		}
	}

	mappings := make([]models.IdMapping, 0, len(work))
	for _, w := range work {
		mappings = append(mappings, models.IdMapping{
			LocalID: w.note.LocalID,
			CloudID: w.cloudID,
		})
	}
	return mappings, nil
}

type outboundListWork struct {
	list       models.SyncOutboundList
	cloudID    string
	existingID uuid.UUID
}

func (r *SyncRepository) UpsertOutboundLists(ctx context.Context, companyID string, lists []models.SyncOutboundList) ([]models.IdMapping, []models.IdMapping, error) {
	if len(lists) == 0 {
		return nil, nil, nil
	}

	prepared := make([]outboundListWork, 0, len(lists))
	listUUIDs := make([]uuid.UUID, 0, len(lists))
	for _, list := range lists {
		cloudID := list.CloudID
		if cloudID == "" {
			cloudID = uuid.New().String()
		}
		parsed, err := uuid.Parse(cloudID)
		if err != nil {
			return nil, nil, fmt.Errorf("outbound list cloud_id: %w", err)
		}
		prepared = append(prepared, outboundListWork{list: list, cloudID: cloudID})
		listUUIDs = append(listUUIDs, parsed)
	}

	existingListID := make(map[string]uuid.UUID, len(prepared))
	if len(listUUIDs) > 0 {
		rows, err := r.pool.Query(ctx, `
			SELECT cloud_id::text, id FROM outbound_lists WHERE cloud_id = ANY($1::uuid[])
		`, listUUIDs)
		if err != nil {
			return nil, nil, err
		}
		for rows.Next() {
			var cid string
			var id uuid.UUID
			if err := rows.Scan(&cid, &id); err != nil {
				rows.Close()
				return nil, nil, err
			}
			existingListID[cid] = id
		}
		if err := rows.Err(); err != nil {
			return nil, nil, err
		}
		rows.Close()
	}

	for i := range prepared {
		if id, ok := existingListID[prepared[i].cloudID]; ok {
			prepared[i].existingID = id
		}
	}

	const updateListSQL = `
		UPDATE outbound_lists SET
			list_number = $3, issue_date = to_timestamp($4/1000),
			driver_nombre = $5, driver_apellido = $6,
			status = $7, checklist_signature_path = $8,
			checklist_signed_at = to_timestamp($9/1000)
		WHERE cloud_id = $1 AND company_id = $2`
	const insertListSQL = `
		INSERT INTO outbound_lists (
			cloud_id, company_id, list_number, issue_date,
			driver_nombre, driver_apellido, status,
			checklist_signature_path, checklist_signed_at, created_at
		) VALUES ($1, $2, $3, to_timestamp($4/1000), $5, $6, $7, $8, to_timestamp($9/1000), NOW())
		RETURNING id`
	const upsertLineSQL = `
		INSERT INTO outbound_lines (
			cloud_id, outbound_list_id, delivery_number,
			recipient_nombre, recipient_apellido, recipient_direccion, recipient_telefono,
			package_qty, allocated_package_ids, status,
			delivered_qty, returned_qty, missing_qty
		) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
		ON CONFLICT (cloud_id) DO UPDATE SET
			delivery_number = EXCLUDED.delivery_number,
			recipient_nombre = EXCLUDED.recipient_nombre,
			recipient_apellido = EXCLUDED.recipient_apellido,
			recipient_direccion = EXCLUDED.recipient_direccion,
			recipient_telefono = EXCLUDED.recipient_telefono,
			package_qty = EXCLUDED.package_qty,
			allocated_package_ids = EXCLUDED.allocated_package_ids,
			status = EXCLUDED.status,
			delivered_qty = EXCLUDED.delivered_qty,
			returned_qty = EXCLUDED.returned_qty,
			missing_qty = EXCLUDED.missing_qty`

	ub := &pgx.Batch{}
	for _, p := range prepared {
		if p.existingID != uuid.Nil {
			l := p.list
			ub.Queue(updateListSQL,
				p.cloudID, companyID, l.ListNumber, l.IssueDate,
				l.DriverNombre, l.DriverApellido, l.Status,
				l.ChecklistSignaturePath, l.ChecklistSignedAt,
			)
		}
	}
	if ub.Len() > 0 {
		br := r.pool.SendBatch(ctx, ub)
		for _, p := range prepared {
			if p.existingID != uuid.Nil {
				if _, err := br.Exec(); err != nil {
					br.Close()
					return nil, nil, fmt.Errorf("failed to update outbound list: %w", err)
				}
			}
		}
		br.Close()
	}

	insertedIDs := make(map[string]uuid.UUID, len(prepared))
	ib := &pgx.Batch{}
	for _, p := range prepared {
		if p.existingID == uuid.Nil {
			l := p.list
			ib.Queue(insertListSQL,
				p.cloudID, companyID, l.ListNumber, l.IssueDate,
				l.DriverNombre, l.DriverApellido, l.Status,
				l.ChecklistSignaturePath, l.ChecklistSignedAt,
			)
		}
	}
	if ib.Len() > 0 {
		br := r.pool.SendBatch(ctx, ib)
		for _, p := range prepared {
			if p.existingID == uuid.Nil {
				var newID uuid.UUID
				if err := br.QueryRow().Scan(&newID); err != nil {
					br.Close()
					return nil, nil, fmt.Errorf("failed to insert outbound list: %w", err)
				}
				insertedIDs[p.cloudID] = newID
			}
		}
		br.Close()
	}

	listMappings := make([]models.IdMapping, 0, len(prepared))
	lineMappings := make([]models.IdMapping, 0)
	lb := &pgx.Batch{}
	lineBatchCount := 0
	for _, p := range prepared {
		listID := p.existingID
		if listID == uuid.Nil {
			listID = insertedIDs[p.cloudID]
		}
		listMappings = append(listMappings, models.IdMapping{
			LocalID: p.list.LocalID,
			CloudID: p.cloudID,
		})
		for _, line := range p.list.Lines {
			lineCloudID := line.CloudID
			if lineCloudID == "" {
				lineCloudID = uuid.New().String()
			}
			lineMappings = append(lineMappings, models.IdMapping{
				LocalID: line.LocalID,
				CloudID: lineCloudID,
			})
			lb.Queue(upsertLineSQL,
				lineCloudID, listID, line.DeliveryNumber,
				line.RecipientNombre, line.RecipientApellido, line.RecipientDireccion, line.RecipientTelefono,
				line.PackageQty, line.AllocatedPackageIDs, line.Status,
				line.DeliveredQty, line.ReturnedQty, line.MissingQty,
			)
			lineBatchCount++
		}
	}
	if lb.Len() > 0 {
		br := r.pool.SendBatch(ctx, lb)
		for i := 0; i < lineBatchCount; i++ {
			if _, err := br.Exec(); err != nil {
				br.Close()
				return nil, nil, fmt.Errorf("failed to upsert outbound line: %w", err)
			}
		}
		br.Close()
	}

	return listMappings, lineMappings, nil
}

func (r *SyncRepository) UpsertStatusHistory(ctx context.Context, companyID string, history []models.SyncStatusHistory) error {
	if len(history) == 0 {
		return nil
	}
	const q = `
		INSERT INTO outbound_line_status_history (
			cloud_id, company_id, status, created_at
		) VALUES ($1, $2, $3, to_timestamp($4/1000))
		ON CONFLICT (cloud_id) DO NOTHING`
	b := &pgx.Batch{}
	for _, h := range history {
		b.Queue(q, uuid.New().String(), companyID, h.Status, h.CreatedAt)
	}
	br := r.pool.SendBatch(ctx, b)
	defer br.Close()
	for range history {
		if _, err := br.Exec(); err != nil {
			return fmt.Errorf("failed to insert status history: %w", err)
		}
	}
	return nil
}

func (r *SyncRepository) UpsertEditHistory(ctx context.Context, companyID string, history []models.SyncEditHistory) error {
	if len(history) == 0 {
		return nil
	}
	const q = `
		INSERT INTO outbound_line_edit_history (
			cloud_id, company_id, field_name, old_value, new_value, reason, created_at
		) VALUES ($1, $2, $3, $4, $5, $6, $7)
		ON CONFLICT (cloud_id) DO NOTHING`
	b := &pgx.Batch{}
	for _, h := range history {
		b.Queue(q, uuid.New().String(), companyID, h.FieldName, h.OldValue, h.NewValue, h.Reason, fmt.Sprintf("%d", h.CreatedAt))
	}
	br := r.pool.SendBatch(ctx, b)
	defer br.Close()
	for range history {
		if _, err := br.Exec(); err != nil {
			return fmt.Errorf("failed to insert edit history: %w", err)
		}
	}
	return nil
}

func (r *SyncRepository) GetInboundNotesSince(ctx context.Context, companyID string, since time.Time) ([]models.SyncInboundNote, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT cloud_id, remito_num_cliente, remito_num_interno,
			cant_bultos_total, cuit_remitente, nombre_remitente, apellido_remitente,
			nombre_destinatario, apellido_destinatario, direccion_destinatario, telefono_destinatario,
			status, EXTRACT(EPOCH FROM created_at)::bigint * 1000, EXTRACT(EPOCH FROM updated_at)::bigint * 1000
		FROM inbound_notes
		WHERE company_id = $1 AND updated_at > $2
		ORDER BY updated_at ASC
	`, companyID, since)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	notes := make([]models.SyncInboundNote, 0)
	for rows.Next() {
		var n models.SyncInboundNote
		var createdAt, updatedAt int64
		err := rows.Scan(&n.CloudID, &n.RemitoNumCliente, &n.RemitoNumInterno,
			&n.CantBultosTotal, &n.CuitRemitente, &n.NombreRemitente, &n.ApellidoRemitente,
			&n.NombreDestinatario, &n.ApellidoDestinatario, &n.DireccionDestinatario, &n.TelefonoDestinatario,
			&n.Status, &createdAt, &updatedAt)
		if err != nil {
			return nil, err
		}
		n.CreatedAt = createdAt
		n.UpdatedAt = updatedAt
		notes = append(notes, n)
	}

	return notes, rows.Err()
}

func (r *SyncRepository) GetOutboundListsSince(ctx context.Context, companyID string, since time.Time) ([]models.SyncOutboundList, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT
			ol.cloud_id,
			ol.list_number,
			EXTRACT(EPOCH FROM ol.issue_date)::bigint * 1000,
			ol.driver_nombre,
			ol.driver_apellido,
			ol.status,
			ol.checklist_signature_path,
			EXTRACT(EPOCH FROM ol.checklist_signed_at)::bigint * 1000,
			ol_line.cloud_id,
			ol_line.delivery_number,
			ol_line.recipient_nombre,
			ol_line.recipient_apellido,
			ol_line.recipient_direccion,
			ol_line.recipient_telefono,
			ol_line.package_qty,
			ol_line.allocated_package_ids,
			ol_line.status,
			ol_line.delivered_qty,
			ol_line.returned_qty,
			ol_line.missing_qty,
			COALESCE(in_cloud_id.cloud_id::text, '')
		FROM outbound_lists ol
		LEFT JOIN outbound_lines ol_line ON ol_line.outbound_list_id = ol.id
		LEFT JOIN inbound_notes in_cloud_id ON ol_line.inbound_note_id = in_cloud_id.id
		WHERE ol.company_id = $1 AND ol.updated_at > $2
		ORDER BY ol.updated_at ASC, ol.id ASC, ol_line.id ASC NULLS LAST
	`, companyID, since)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	lists := make([]models.SyncOutboundList, 0)
	var lastListCloud string
	for rows.Next() {
		var (
			listCloudID            string
			listNumber             int64
			issueDate              int64
			driverNombre           string
			driverApellido         string
			status                 string
			checklistSignaturePath string
			checklistSignedMs      sql.NullInt64
			lineCloudID            sql.NullString
			deliveryNumber         sql.NullString
			recipientNombre        sql.NullString
			recipientApellido      sql.NullString
			recipientDireccion     sql.NullString
			recipientTelefono      sql.NullString
			packageQty             sql.NullInt64
			allocatedPackageIDs    sql.NullString
			lineStatus             sql.NullString
			deliveredQty           sql.NullInt64
			returnedQty            sql.NullInt64
			missingQty             sql.NullInt64
			inboundNoteCloudID     string
		)
		err := rows.Scan(
			&listCloudID,
			&listNumber,
			&issueDate,
			&driverNombre,
			&driverApellido,
			&status,
			&checklistSignaturePath,
			&checklistSignedMs,
			&lineCloudID,
			&deliveryNumber,
			&recipientNombre,
			&recipientApellido,
			&recipientDireccion,
			&recipientTelefono,
			&packageQty,
			&allocatedPackageIDs,
			&lineStatus,
			&deliveredQty,
			&returnedQty,
			&missingQty,
			&inboundNoteCloudID,
		)
		if err != nil {
			return nil, err
		}

		if listCloudID != lastListCloud {
			nl := models.SyncOutboundList{
				CloudID:                listCloudID,
				ListNumber:             listNumber,
				IssueDate:              issueDate,
				DriverNombre:           driverNombre,
				DriverApellido:         driverApellido,
				Status:                 status,
				ChecklistSignaturePath: checklistSignaturePath,
				Lines:                  []models.SyncOutboundLine{},
			}
			if checklistSignedMs.Valid {
				v := checklistSignedMs.Int64
				nl.ChecklistSignedAt = &v
			}
			lists = append(lists, nl)
			lastListCloud = listCloudID
		}

		if lineCloudID.Valid {
			line := models.SyncOutboundLine{
				CloudID:             lineCloudID.String,
				DeliveryNumber:      deliveryNumber.String,
				RecipientNombre:     recipientNombre.String,
				RecipientApellido:   recipientApellido.String,
				RecipientDireccion:  recipientDireccion.String,
				RecipientTelefono:   recipientTelefono.String,
				PackageQty:          int(packageQty.Int64),
				AllocatedPackageIDs: allocatedPackageIDs.String,
				Status:              lineStatus.String,
				DeliveredQty:        int(deliveredQty.Int64),
				ReturnedQty:        int(returnedQty.Int64),
				MissingQty:         int(missingQty.Int64),
				InboundNoteCloudID: inboundNoteCloudID,
			}
			cur := &lists[len(lists)-1]
			cur.Lines = append(cur.Lines, line)
		}
	}

	return lists, rows.Err()
}

func (r *SyncRepository) GetInboundNotesCountSince(ctx context.Context, companyID string, since time.Time) (int, error) {
	var count int
	err := r.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM inbound_notes
		WHERE company_id = $1 AND updated_at > $2
	`, companyID, since).Scan(&count)
	return count, err
}

func (r *SyncRepository) CountInboundNotesByCloudIDs(
	ctx context.Context,
	companyID uuid.UUID,
	cloudIDs []string,
) (int64, error) {
	if len(cloudIDs) == 0 {
		return 0, nil
	}
	filtered := make([]string, 0, len(cloudIDs))
	for _, id := range cloudIDs {
		v := strings.TrimSpace(id)
		if v == "" {
			continue
		}
		filtered = append(filtered, v)
	}
	if len(filtered) == 0 {
		return 0, nil
	}

	var n int64
	err := r.pool.QueryRow(ctx, `
		SELECT COUNT(*)::bigint
		FROM inbound_notes
		WHERE company_id = $1
		  AND cloud_id = ANY($2::text[])
	`, companyID, filtered).Scan(&n)
	return n, err
}

// InboundNoteEntitlementMetrics returns trailing-30d inbound count, lifetime count, and the oldest
// note’s created_at (first synced scan / activation signal). Used by dashboard entitlement.
func (r *SyncRepository) InboundNoteEntitlementMetrics(ctx context.Context, companyID uuid.UUID) (
	last30Days int64,
	lifetime int64,
	firstCreatedAt *time.Time,
	err error,
) {
	var minAt sql.NullTime
	err = r.pool.QueryRow(ctx, `
		SELECT
			COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '30 days')::bigint,
			COUNT(*)::bigint,
			MIN(created_at)
		FROM inbound_notes
		WHERE company_id = $1
	`, companyID).Scan(&last30Days, &lifetime, &minAt)
	if err != nil {
		return 0, 0, nil, err
	}
	if minAt.Valid {
		t := minAt.Time.UTC()
		firstCreatedAt = &t
	}
	return last30Days, lifetime, firstCreatedAt, nil
}

// DocumentUsageSeriesPoint is one calendar day in the month-to-date cumulative series (UTC).
type DocumentUsageSeriesPoint struct {
	Date       string `json:"date"`
	Cumulative int64  `json:"cumulative"`
}

// InboundNotesMTDCumulativeSeries returns cumulative inbound document totals at the start of each UTC
// calendar day from the first day of the month through today. Day 1 is always 0; day 2 includes all
// counts from day 1, and so on. The returned mtdTotal is still the full month-to-date sum through today
// (same as the cumulative value after processing today's counts).
func (r *SyncRepository) InboundNotesMTDCumulativeSeries(ctx context.Context, companyID uuid.UUID) (mtdTotal int64, points []DocumentUsageSeriesPoint, err error) {
	now := time.Now().UTC()
	monthStart := time.Date(now.Year(), now.Month(), 1, 0, 0, 0, 0, time.UTC)
	today := time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, time.UTC)
	nextMonthStart := monthStart.AddDate(0, 1, 0)

	rows, err := r.pool.Query(ctx, `
		SELECT (created_at AT TIME ZONE 'UTC')::date::text AS day, COUNT(*)::bigint
		FROM inbound_notes
		WHERE company_id = $1
		  AND created_at >= $2
		  AND created_at < $3
		GROUP BY 1
		ORDER BY 1
	`, companyID, monthStart, nextMonthStart)
	if err != nil {
		return 0, nil, err
	}
	defer rows.Close()

	counts := make(map[string]int64)
	for rows.Next() {
		var day string
		var n int64
		if err := rows.Scan(&day, &n); err != nil {
			return 0, nil, err
		}
		counts[day] = n
	}
	if err := rows.Err(); err != nil {
		return 0, nil, err
	}

	points = make([]DocumentUsageSeriesPoint, 0)
	var cum int64
	for d := monthStart; !d.After(today); d = d.AddDate(0, 0, 1) {
		key := d.Format("2006-01-02")
		points = append(points, DocumentUsageSeriesPoint{Date: key, Cumulative: cum})
		cum += counts[key]
	}
	return cum, points, nil
}

// WarehouseInboundUsageRow is inbound remito counts per warehouse for the trailing 30-day window.
type WarehouseInboundUsageRow struct {
	WarehouseID uuid.UUID `json:"warehouse_id"`
	Name        string    `json:"name"`
	Count       int64     `json:"count"`
}

// ListInboundNotesByWarehouseLast30Days returns each warehouse's share of inbound remitos
// created in the last 30 days. Rows with zero count are included. When some remitos have no
// warehouse_id, they are summed under uuid.Nil with name "Sin depósito asignado".
func (r *SyncRepository) ListInboundNotesByWarehouseLast30Days(ctx context.Context, companyID uuid.UUID) ([]WarehouseInboundUsageRow, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT w.id, w.name, COUNT(inb.id)::bigint
		FROM warehouses w
		LEFT JOIN inbound_notes inb
			ON inb.warehouse_id = w.id
			AND inb.created_at >= NOW() - INTERVAL '30 days'
		WHERE w.company_id = $1
		GROUP BY w.id, w.name
		ORDER BY w.name ASC
	`, companyID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	out := make([]WarehouseInboundUsageRow, 0)
	for rows.Next() {
		var row WarehouseInboundUsageRow
		if err := rows.Scan(&row.WarehouseID, &row.Name, &row.Count); err != nil {
			return nil, err
		}
		out = append(out, row)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}

	var orphan int64
	err = r.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM inbound_notes
		WHERE company_id = $1
		  AND warehouse_id IS NULL
		  AND created_at >= NOW() - INTERVAL '30 days'
	`, companyID).Scan(&orphan)
	if err != nil {
		return nil, err
	}
	if orphan > 0 {
		out = append(out, WarehouseInboundUsageRow{
			WarehouseID: uuid.Nil,
			Name:        "Sin depósito asignado",
			Count:       orphan,
		})
	}

	return out, nil
}

// ListInboundNotesByWarehouseMTD returns inbound note counts per warehouse for the current UTC calendar month
// (same window as InboundNotesMTDCumulativeSeries). Rows with zero count are included for configured warehouses.
func (r *SyncRepository) ListInboundNotesByWarehouseMTD(ctx context.Context, companyID uuid.UUID) ([]WarehouseInboundUsageRow, error) {
	now := time.Now().UTC()
	monthStart := time.Date(now.Year(), now.Month(), 1, 0, 0, 0, 0, time.UTC)
	nextMonthStart := monthStart.AddDate(0, 1, 0)

	rows, err := r.pool.Query(ctx, `
		SELECT w.id, w.name, COUNT(inb.id)::bigint
		FROM warehouses w
		LEFT JOIN inbound_notes inb
			ON inb.warehouse_id = w.id
			AND inb.company_id = $1
			AND inb.created_at >= $2
			AND inb.created_at < $3
		WHERE w.company_id = $1
		GROUP BY w.id, w.name
		ORDER BY w.name ASC
	`, companyID, monthStart, nextMonthStart)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	out := make([]WarehouseInboundUsageRow, 0)
	for rows.Next() {
		var row WarehouseInboundUsageRow
		if err := rows.Scan(&row.WarehouseID, &row.Name, &row.Count); err != nil {
			return nil, err
		}
		out = append(out, row)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}

	var orphan int64
	err = r.pool.QueryRow(ctx, `
		SELECT COUNT(*)::bigint FROM inbound_notes
		WHERE company_id = $1
		  AND warehouse_id IS NULL
		  AND created_at >= $2
		  AND created_at < $3
	`, companyID, monthStart, nextMonthStart).Scan(&orphan)
	if err != nil {
		return nil, err
	}
	if orphan > 0 {
		out = append(out, WarehouseInboundUsageRow{
			WarehouseID: uuid.Nil,
			Name:        "Sin depósito asignado",
			Count:       orphan,
		})
	}

	return out, nil
}

func (r *SyncRepository) GetOutboundListsCountSince(ctx context.Context, companyID string, since time.Time) (int, error) {
	var count int
	err := r.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM outbound_lists
		WHERE company_id = $1 AND updated_at > $2
	`, companyID, since).Scan(&count)
	return count, err
}
