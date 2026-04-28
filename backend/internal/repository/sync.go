package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/models"
)

type SyncRepository struct {
	pool *pgxpool.Pool
}

func NewSyncRepository(pool *pgxpool.Pool) *SyncRepository {
	return &SyncRepository{pool: pool}
}

func (r *SyncRepository) UpsertInboundNotes(ctx context.Context, companyID string, notes []models.SyncInboundNote) ([]models.IdMapping, error) {
	mappings := make([]models.IdMapping, 0, len(notes))

	for _, note := range notes {
		cloudID := note.CloudID
		if cloudID == "" {
			cloudID = uuid.New().String()
		}

		var id uuid.UUID
		var existingCompanyID *string

		err := r.pool.QueryRow(ctx, `
			SELECT id, company_id FROM inbound_notes WHERE cloud_id = $1
		`, cloudID).Scan(&id, &existingCompanyID)

		if err == nil && existingCompanyID != nil && *existingCompanyID != companyID {
			continue
		}

		if err != nil {
			_, err := r.pool.Exec(ctx, `
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
					updated_at = EXCLUDED.updated_at
			`, cloudID, companyID, note.RemitoNumCliente, note.RemitoNumInterno,
				note.CantBultosTotal, note.CuitRemitente, note.NombreRemitente, note.ApellidoRemitente,
				note.NombreDestinatario, note.ApellidoDestinatario, note.DireccionDestinatario, note.TelefonoDestinatario,
				note.Status, note.CreatedAt, note.UpdatedAt,
			)
			if err != nil {
				return nil, fmt.Errorf("failed to upsert inbound note: %w", err)
			}
		} else {
			_, err := r.pool.Exec(ctx, `
				UPDATE inbound_notes SET
					remito_num_cliente = $3, remito_num_interno = $4,
					cant_bultos_total = $5, cuit_remitente = $6,
					nombre_remitente = $7, apellido_remitente = $8,
					nombre_destinatario = $9, apellido_destinatario = $10,
					direccion_destinatario = $11, telefono_destinatario = $12,
					status = $13, updated_at = to_timestamp($14/1000)
				WHERE cloud_id = $1 AND company_id = $2
			`, cloudID, companyID, note.RemitoNumCliente, note.RemitoNumInterno,
				note.CantBultosTotal, note.CuitRemitente, note.NombreRemitente, note.ApellidoRemitente,
				note.NombreDestinatario, note.ApellidoDestinatario, note.DireccionDestinatario, note.TelefonoDestinatario,
				note.Status, note.UpdatedAt,
			)
			if err != nil {
				return nil, fmt.Errorf("failed to update inbound note: %w", err)
			}
		}

		mappings = append(mappings, models.IdMapping{
			LocalID: note.LocalID,
			CloudID: cloudID,
		})
	}

	return mappings, nil
}

func (r *SyncRepository) UpsertOutboundLists(ctx context.Context, companyID string, lists []models.SyncOutboundList) ([]models.IdMapping, []models.IdMapping, error) {
	listMappings := make([]models.IdMapping, 0, len(lists))
	lineMappings := make([]models.IdMapping, 0)

	for _, list := range lists {
		cloudID := list.CloudID
		if cloudID == "" {
			cloudID = uuid.New().String()
		}

		var listID uuid.UUID
		err := r.pool.QueryRow(ctx, `
			SELECT id FROM outbound_lists WHERE cloud_id = $1
		`, cloudID).Scan(&listID)

		if err != nil {
			err = r.pool.QueryRow(ctx, `
				INSERT INTO outbound_lists (
					cloud_id, company_id, list_number, issue_date,
					driver_nombre, driver_apellido, status,
					checklist_signature_path, checklist_signed_at, created_at
				) VALUES ($1, $2, $3, to_timestamp($4/1000), $5, $6, $7, $8, to_timestamp($9/1000), NOW())
				RETURNING id
			`, cloudID, companyID, list.ListNumber, list.IssueDate,
				list.DriverNombre, list.DriverApellido, list.Status,
				list.ChecklistSignaturePath, list.ChecklistSignedAt,
			).Scan(&listID)
			if err != nil {
				return nil, nil, fmt.Errorf("failed to insert outbound list: %w", err)
			}
		} else {
			_, err = r.pool.Exec(ctx, `
				UPDATE outbound_lists SET
					list_number = $3, issue_date = to_timestamp($4/1000),
					driver_nombre = $5, driver_apellido = $6,
					status = $7, checklist_signature_path = $8,
					checklist_signed_at = to_timestamp($9/1000)
				WHERE cloud_id = $1 AND company_id = $2
			`, cloudID, companyID, list.ListNumber, list.IssueDate,
				list.DriverNombre, list.DriverApellido, list.Status,
				list.ChecklistSignaturePath, list.ChecklistSignedAt,
			)
			if err != nil {
				return nil, nil, fmt.Errorf("failed to update outbound list: %w", err)
			}
		}

		listMappings = append(listMappings, models.IdMapping{
			LocalID: list.LocalID,
			CloudID: cloudID,
		})

		for _, line := range list.Lines {
			lineCloudID := line.CloudID
			if lineCloudID == "" {
				lineCloudID = uuid.New().String()
			}

			_, err := r.pool.Exec(ctx, `
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
					missing_qty = EXCLUDED.missing_qty
			`, lineCloudID, listID, line.DeliveryNumber,
				line.RecipientNombre, line.RecipientApellido, line.RecipientDireccion, line.RecipientTelefono,
				line.PackageQty, line.AllocatedPackageIDs, line.Status,
				line.DeliveredQty, line.ReturnedQty, line.MissingQty,
			)
			if err != nil {
				return nil, nil, fmt.Errorf("failed to upsert outbound line: %w", err)
			}

			lineMappings = append(lineMappings, models.IdMapping{
				LocalID: line.LocalID,
				CloudID: lineCloudID,
			})
		}
	}

	return listMappings, lineMappings, nil
}

func (r *SyncRepository) UpsertStatusHistory(ctx context.Context, companyID string, history []models.SyncStatusHistory) error {
	for _, h := range history {
		_, err := r.pool.Exec(ctx, `
			INSERT INTO outbound_line_status_history (
				cloud_id, company_id, status, created_at
			) VALUES ($1, $2, $3, to_timestamp($4/1000))
			ON CONFLICT (cloud_id) DO NOTHING
		`, uuid.New().String(), companyID, h.Status, h.CreatedAt)
		if err != nil {
			return fmt.Errorf("failed to insert status history: %w", err)
		}
	}
	return nil
}

func (r *SyncRepository) UpsertEditHistory(ctx context.Context, companyID string, history []models.SyncEditHistory) error {
	for _, h := range history {
		_, err := r.pool.Exec(ctx, `
			INSERT INTO outbound_line_edit_history (
				cloud_id, company_id, field_name, old_value, new_value, reason, created_at
			) VALUES ($1, $2, $3, $4, $5, $6, $7)
			ON CONFLICT (cloud_id) DO NOTHING
		`, uuid.New().String(), companyID, h.FieldName, h.OldValue, h.NewValue, h.Reason, fmt.Sprintf("%d", h.CreatedAt))
		if err != nil {
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
		SELECT ol.cloud_id, ol.list_number, EXTRACT(EPOCH FROM ol.issue_date)::bigint * 1000,
			ol.driver_nombre, ol.driver_apellido, ol.status,
			ol.checklist_signature_path, ol.checklist_signed_at
		FROM outbound_lists ol
		WHERE ol.company_id = $1 AND ol.updated_at > $2
		ORDER BY ol.updated_at ASC
	`, companyID, since)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	lists := make([]models.SyncOutboundList, 0)
	for rows.Next() {
		var l models.SyncOutboundList
		var signedAt *int64
		err := rows.Scan(&l.CloudID, &l.ListNumber, &l.IssueDate,
			&l.DriverNombre, &l.DriverApellido, &l.Status,
			&l.ChecklistSignaturePath, &signedAt)
		if err != nil {
			return nil, err
		}
		l.ChecklistSignedAt = signedAt

		lines, err := r.getLinesForList(ctx, l.CloudID)
		if err != nil {
			return nil, err
		}
		l.Lines = lines

		lists = append(lists, l)
	}

	return lists, rows.Err()
}

func (r *SyncRepository) getLinesForList(ctx context.Context, listCloudID string) ([]models.SyncOutboundLine, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT ol.cloud_id, ol.delivery_number,
			ol.recipient_nombre, ol.recipient_apellido, ol.recipient_direccion, ol.recipient_telefono,
			ol.package_qty, ol.allocated_package_ids, ol.status,
			ol.delivered_qty, ol.returned_qty, ol.missing_qty,
			COALESCE(in_cloud_id.cloud_id, '')
		FROM outbound_lines ol
		JOIN outbound_lists obl ON ol.outbound_list_id = obl.id
		LEFT JOIN inbound_notes in_cloud_id ON ol.inbound_note_id = in_cloud_id.id
		WHERE obl.cloud_id = $1
		ORDER BY ol.id ASC
	`, listCloudID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	lines := make([]models.SyncOutboundLine, 0)
	for rows.Next() {
		var l models.SyncOutboundLine
		var inboundNoteCloudID string
		err := rows.Scan(&l.CloudID, &l.DeliveryNumber,
			&l.RecipientNombre, &l.RecipientApellido, &l.RecipientDireccion, &l.RecipientTelefono,
			&l.PackageQty, &l.AllocatedPackageIDs, &l.Status,
			&l.DeliveredQty, &l.ReturnedQty, &l.MissingQty,
			&inboundNoteCloudID)
		if err != nil {
			return nil, err
		}
		l.InboundNoteCloudID = inboundNoteCloudID
		lines = append(lines, l)
	}

	return lines, rows.Err()
}

func (r *SyncRepository) GetInboundNotesCountSince(ctx context.Context, companyID string, since time.Time) (int, error) {
	var count int
	err := r.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM inbound_notes
		WHERE company_id = $1 AND updated_at > $2
	`, companyID, since).Scan(&count)
	return count, err
}

// CountInboundNotesCreatedInLast30Days returns how many inbound remitos were first recorded
// (created_at) within the trailing 30-day window. Uses the database clock (UTC).
func (r *SyncRepository) CountInboundNotesCreatedInLast30Days(ctx context.Context, companyID uuid.UUID) (int64, error) {
	var count int64
	err := r.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM inbound_notes
		WHERE company_id = $1
		  AND created_at >= NOW() - INTERVAL '30 days'
	`, companyID).Scan(&count)
	return count, err
}

// DocumentUsageSeriesPoint is one calendar day in the month-to-date cumulative series (UTC).
type DocumentUsageSeriesPoint struct {
	Date       string `json:"date"`
	Cumulative int64  `json:"cumulative"`
}

// InboundNotesMTDCumulativeSeries returns cumulative inbound document counts from the first day of the
// current UTC calendar month through today, one point per day (missing days carry the previous cumulative).
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
		cum += counts[key]
		points = append(points, DocumentUsageSeriesPoint{Date: key, Cumulative: cum})
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
