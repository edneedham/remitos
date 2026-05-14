SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_table_access_method = heap;

--
-- Name: afip_tickets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.afip_tickets (
    service text NOT NULL,
    token text NOT NULL,
    sign text NOT NULL,
    generation_time timestamp with time zone NOT NULL,
    expiration_time timestamp with time zone NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id uuid NOT NULL,
    warehouse_id uuid,
    user_id uuid,
    device_id uuid,
    action_type character varying(100) NOT NULL,
    target_id uuid,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: billing_invoice_factura_attempts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.billing_invoice_factura_attempts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    invoice_id uuid NOT NULL,
    attempted_at timestamp with time zone DEFAULT now() NOT NULL,
    ok boolean DEFAULT false NOT NULL,
    error_code text,
    error_msg text,
    request_xml text,
    response_xml text
);


--
-- Name: billing_invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.billing_invoices (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id uuid NOT NULL,
    amount_minor bigint NOT NULL,
    currency character varying(8) DEFAULT 'ARS'::character varying NOT NULL,
    status character varying(32) DEFAULT 'paid'::character varying NOT NULL,
    description text,
    issued_at timestamp with time zone DEFAULT now() NOT NULL,
    mp_payment_id character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    receipt_email_sent_at timestamp with time zone,
    renewal_failure_notice_sent_at timestamp with time zone,
    usd_list_amount numeric(12,2),
    ars_per_usd numeric(12,6),
    fx_source text,
    fx_effective_date date,
    factura_tipo smallint,
    factura_pto_vta integer,
    factura_numero bigint,
    factura_cae character varying(20),
    factura_cae_vto date,
    factura_emitted_at timestamp with time zone,
    factura_request_id uuid,
    factura_last_error text,
    factura_attempts integer DEFAULT 0 NOT NULL,
    CONSTRAINT billing_invoices_status_check CHECK (((status)::text = ANY (ARRAY[('paid'::character varying)::text, ('pending'::character varying)::text, ('void'::character varying)::text])))
);


--
-- Name: COLUMN billing_invoices.receipt_email_sent_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.billing_invoices.receipt_email_sent_at IS 'When we emailed a payment receipt for this row (idempotent with mp_payment_id)';


--
-- Name: COLUMN billing_invoices.renewal_failure_notice_sent_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.billing_invoices.renewal_failure_notice_sent_at IS 'When we emailed about a failed automatic renewal charge for this pending invoice';


--
-- Name: COLUMN billing_invoices.usd_list_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.billing_invoices.usd_list_amount IS 'Catalog USD list amount captured at invoice issuance (major units).';


--
-- Name: COLUMN billing_invoices.ars_per_usd; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.billing_invoices.ars_per_usd IS 'Applied ARS per 1 USD used for conversion at issuance time.';


--
-- Name: COLUMN billing_invoices.fx_source; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.billing_invoices.fx_source IS 'FX quote source identifier used at issuance (for support/accounting traceability).';


--
-- Name: COLUMN billing_invoices.fx_effective_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.billing_invoices.fx_effective_date IS 'Effective date of the FX quote used to compute this invoice.';


--
-- Name: COLUMN billing_invoices.factura_tipo; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.billing_invoices.factura_tipo IS 'AFIP comprobante type (1=A, 6=B, 11=C, …).';


--
-- Name: COLUMN billing_invoices.factura_request_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.billing_invoices.factura_request_id IS 'Idempotency id for FECAESolicitar retries.';


--
-- Name: COLUMN billing_invoices.factura_attempts; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.billing_invoices.factura_attempts IS 'How many emission attempts; capped in worker.';


--
-- Name: companies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.companies (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    code character varying(50),
    cuit character varying(20),
    status text DEFAULT 'active'::text NOT NULL,
    is_verified boolean DEFAULT false,
    subscription_plan text DEFAULT 'free'::text,
    subscription_expires_at timestamp without time zone,
    archived_at timestamp without time zone,
    trial_ends_at timestamp with time zone,
    max_warehouses integer,
    max_users integer,
    mp_customer_id character varying(255),
    mp_card_id character varying(255),
    documents_monthly_limit integer,
    trial_activation_at timestamp with time zone,
    onboarding_nudge_setup_sent_at timestamp with time zone,
    onboarding_nudge_day1_sent_at timestamp with time zone,
    onboarding_nudge_day3_sent_at timestamp with time zone,
    renewal_reminder_sent_for_expires_at timestamp with time zone,
    trial_end_notice_sent_for_trial_ends_at timestamp with time zone,
    subscription_lapse_notice_sent_for_expires_at timestamp with time zone,
    pending_plan text,
    cuit_verified_at timestamp with time zone,
    razon_social text,
    condicion_iva text,
    domicilio_fiscal text,
    padron_synced_at timestamp with time zone,
    cuit_estado text
);


--
-- Name: COLUMN companies.trial_ends_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.trial_ends_at IS 'End of free trial; no charge before this when trialing';


--
-- Name: COLUMN companies.max_warehouses; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.max_warehouses IS 'Cap for trial/plan; NULL = unlimited (legacy)';


--
-- Name: COLUMN companies.max_users; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.max_users IS 'Cap for trial/plan; NULL = unlimited (legacy)';


--
-- Name: COLUMN companies.mp_customer_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.mp_customer_id IS 'Mercado Pago customer id';


--
-- Name: COLUMN companies.mp_card_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.mp_card_id IS 'Mercado Pago saved card id for charging after trial';


--
-- Name: COLUMN companies.documents_monthly_limit; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.documents_monthly_limit IS 'Max inbound documents per calendar month (UTC); NULL = unlimited';


--
-- Name: COLUMN companies.trial_activation_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.trial_activation_at IS 'Set when the customer chooses PyME/Empresa plan after signup; used for onboarding reminder emails';


--
-- Name: COLUMN companies.onboarding_nudge_setup_sent_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.onboarding_nudge_setup_sent_at IS 'First reminder (+~10m): download Android app';


--
-- Name: COLUMN companies.onboarding_nudge_day1_sent_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.onboarding_nudge_day1_sent_at IS 'Second reminder (+~24h)';


--
-- Name: COLUMN companies.onboarding_nudge_day3_sent_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.onboarding_nudge_day3_sent_at IS 'Third reminder (+~72h)';


--
-- Name: COLUMN companies.renewal_reminder_sent_for_expires_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.renewal_reminder_sent_for_expires_at IS 'Last subscription_expires_at we emailed an upcoming renewal notice for; resend when expiry advances';


--
-- Name: COLUMN companies.trial_end_notice_sent_for_trial_ends_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.trial_end_notice_sent_for_trial_ends_at IS 'trial_ends_at value we already emailed a trial-ending reminder for';


--
-- Name: COLUMN companies.subscription_lapse_notice_sent_for_expires_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.subscription_lapse_notice_sent_for_expires_at IS 'subscription_expires_at we emailed a lapse notice for';


--
-- Name: COLUMN companies.pending_plan; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.pending_plan IS 'When set, the company is scheduled to switch to this plan at the next subscription renewal (downgrade only).';


--
-- Name: COLUMN companies.condicion_iva; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.condicion_iva IS 'Canonical: RESPONSABLE_INSCRIPTO, MONOTRIBUTO, EXENTO, CONSUMIDOR_FINAL, NO_CATEGORIZADO, DESCONOCIDO.';


--
-- Name: COLUMN companies.cuit_estado; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.companies.cuit_estado IS 'AFIP padron estado clave (e.g. ACTIVO, INACTIVO).';


--
-- Name: device_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    device_id uuid NOT NULL,
    company_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    event_type text NOT NULL,
    performed_by uuid,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: devices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.devices (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    device_uuid text NOT NULL,
    platform text NOT NULL,
    model text,
    os_version text,
    app_version text,
    status text DEFAULT 'pending'::text NOT NULL,
    approved_by uuid,
    approved_at timestamp without time zone,
    registered_at timestamp without time zone DEFAULT now() NOT NULL,
    last_seen_at timestamp without time zone,
    name character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    fingerprint character varying(255)
);


--
-- Name: document_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.document_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    document_id uuid NOT NULL,
    description text NOT NULL,
    expected_quantity integer DEFAULT 0 NOT NULL,
    received_quantity integer DEFAULT 0 NOT NULL,
    unit character varying(50)
);


--
-- Name: documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.documents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    type character varying(50) NOT NULL,
    supplier_name character varying(255),
    document_number character varying(100),
    ocr_confidence double precision,
    ocr_engine_used character varying(20),
    status character varying(20) DEFAULT 'draft'::character varying NOT NULL,
    created_by uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    verified_at timestamp without time zone,
    extra_fields_json jsonb DEFAULT '{}'::jsonb,
    cloud_id uuid DEFAULT gen_random_uuid(),
    CONSTRAINT documents_ocr_engine_used_check CHECK (((ocr_engine_used)::text = ANY (ARRAY[('local'::character varying)::text, ('cloud'::character varying)::text]))),
    CONSTRAINT documents_status_check CHECK (((status)::text = ANY (ARRAY[('draft'::character varying)::text, ('verifying'::character varying)::text, ('verified'::character varying)::text, ('finalized'::character varying)::text]))),
    CONSTRAINT documents_type_check CHECK (((type)::text = ANY (ARRAY[('incoming_remito'::character varying)::text, ('outgoing_remito'::character varying)::text])))
);


--
-- Name: image_metadata; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.image_metadata (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    scan_event_id uuid,
    file_name character varying(255),
    file_size integer,
    width integer,
    height integer,
    format character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: images; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.images (
    id uuid NOT NULL,
    gcs_path character varying(500) NOT NULL,
    content_type character varying(100) DEFAULT 'image/jpeg'::character varying NOT NULL,
    file_size bigint NOT NULL,
    entity_type character varying(50) NOT NULL,
    entity_id bigint NOT NULL,
    warehouse_id uuid,
    uploaded_by uuid,
    uploaded_at timestamp without time zone DEFAULT now() NOT NULL,
    storage_class character varying(20) DEFAULT 'STANDARD'::character varying,
    CONSTRAINT images_entity_type_check CHECK (((entity_type)::text = ANY (ARRAY[('inbound_note'::character varying)::text, ('outbound_list'::character varying)::text, ('scan'::character varying)::text])))
);


--
-- Name: inbound_notes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.inbound_notes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cloud_id uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id uuid NOT NULL,
    warehouse_id uuid,
    remito_num_cliente character varying(100) NOT NULL,
    remito_num_interno character varying(100),
    cant_bultos_total integer DEFAULT 0 NOT NULL,
    cuit_remitente character varying(50),
    nombre_remitente character varying(255),
    apellido_remitente character varying(255),
    nombre_destinatario character varying(255),
    apellido_destinatario character varying(255),
    direccion_destinatario character varying(500),
    telefono_destinatario character varying(50),
    status character varying(50) DEFAULT 'Activa'::character varying NOT NULL,
    image_gcs_path character varying(500),
    image_url character varying(1000),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: ocr_results; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ocr_results (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    document_id uuid NOT NULL,
    source character varying(50) NOT NULL,
    text_extracted text,
    confidence_score double precision,
    raw_response jsonb,
    processed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    company_id uuid NOT NULL,
    CONSTRAINT ocr_results_source_check CHECK (((source)::text = ANY (ARRAY[('mlkit'::character varying)::text, ('cloud_vision'::character varying)::text])))
);


--
-- Name: outbound_line_edit_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.outbound_line_edit_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cloud_id uuid DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL,
    outbound_line_id uuid NOT NULL,
    field_name character varying(100) NOT NULL,
    old_value text,
    new_value text,
    reason text,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: outbound_line_status_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.outbound_line_status_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cloud_id uuid DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL,
    outbound_line_id uuid NOT NULL,
    status character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: outbound_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.outbound_lines (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cloud_id uuid DEFAULT gen_random_uuid() NOT NULL,
    outbound_list_id uuid NOT NULL,
    inbound_note_id uuid,
    delivery_number character varying(100),
    recipient_nombre character varying(255),
    recipient_apellido character varying(255),
    recipient_direccion character varying(500),
    recipient_telefono character varying(50),
    package_qty integer DEFAULT 0 NOT NULL,
    allocated_package_ids text,
    status character varying(50) DEFAULT 'EnDeposito'::character varying NOT NULL,
    delivered_qty integer DEFAULT 0 NOT NULL,
    returned_qty integer DEFAULT 0 NOT NULL,
    missing_qty integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: outbound_lists; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.outbound_lists (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cloud_id uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id uuid NOT NULL,
    warehouse_id uuid,
    list_number bigint NOT NULL,
    issue_date timestamp without time zone DEFAULT now() NOT NULL,
    driver_nombre character varying(255),
    driver_apellido character varying(255),
    checklist_signature_path character varying(500),
    checklist_signed_at timestamp without time zone,
    status character varying(50) DEFAULT 'Abierta'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token_hash text NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(255) NOT NULL,
    device_name character varying(255),
    expires_at timestamp without time zone NOT NULL,
    revoked_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(50) NOT NULL
);


--
-- Name: scan_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scan_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    warehouse_id uuid NOT NULL,
    user_id uuid,
    device_id uuid,
    source character varying(50) NOT NULL,
    text_extracted text,
    confidence_score double precision,
    processed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT scan_events_source_check CHECK (((source)::text = ANY (ARRAY[('mlkit'::character varying)::text, ('cloud_vision'::character varying)::text])))
);


--
-- Name: scanned_codes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scanned_codes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    document_id uuid NOT NULL,
    document_item_id uuid,
    raw_value text NOT NULL,
    parsed_gtin character varying(50),
    parsed_sscc character varying(50),
    parsed_batch character varying(50),
    parsed_expiry date,
    matched boolean DEFAULT false NOT NULL,
    scanned_by uuid,
    device_id uuid,
    scanned_at_local timestamp without time zone,
    scanned_at_server timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: subscriptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.subscriptions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    device_id uuid,
    status character varying(50) DEFAULT 'trialing'::character varying NOT NULL,
    device_connected boolean DEFAULT false NOT NULL,
    features jsonb DEFAULT '{"offlineMode": true, "connectedMode": true, "premiumFeatures": true}'::jsonb NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: sync_metadata; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sync_metadata (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    device_id uuid NOT NULL,
    company_id uuid NOT NULL,
    last_sync_at timestamp without time zone DEFAULT now() NOT NULL,
    last_sync_timestamp bigint DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: user_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    company_id uuid NOT NULL,
    event_type text NOT NULL,
    performed_by uuid,
    metadata jsonb,
    ip_address character varying(45),
    user_agent text,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: user_notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    company_id uuid NOT NULL,
    kind text NOT NULL,
    title text NOT NULL,
    body text,
    action_url text,
    read_at timestamp with time zone,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: user_warehouses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_warehouses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    warehouse_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    company_id uuid NOT NULL,
    role character varying(50) DEFAULT 'operator'::character varying NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id uuid NOT NULL,
    email character varying(255),
    password_hash character varying(255) NOT NULL,
    role character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    username character varying(100),
    warehouse_id uuid,
    status text DEFAULT 'active'::text NOT NULL,
    is_verified boolean DEFAULT false,
    role_id uuid,
    external_id character varying(50)
);


--
-- Name: waitlist_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.waitlist_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email_normalized text NOT NULL,
    full_name text,
    company_name text,
    source text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    delivery_notes_per_day_band text,
    processing_mode text,
    digital_application text,
    warehouse_count integer,
    logistics_pain_points text
);


--
-- Name: warehouses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.warehouses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    address character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    location character varying(500),
    archived_at timestamp without time zone
);


--
-- Name: web_session_transfers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.web_session_transfers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    token_hash character varying(128) NOT NULL,
    user_id uuid NOT NULL,
    desktop_refresh_token_id uuid NOT NULL,
    phone_refresh_token_id uuid,
    expires_at timestamp without time zone NOT NULL,
    used_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: afip_tickets afip_tickets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.afip_tickets
    ADD CONSTRAINT afip_tickets_pkey PRIMARY KEY (service);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: billing_invoice_factura_attempts billing_invoice_factura_attempts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billing_invoice_factura_attempts
    ADD CONSTRAINT billing_invoice_factura_attempts_pkey PRIMARY KEY (id);


--
-- Name: billing_invoices billing_invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billing_invoices
    ADD CONSTRAINT billing_invoices_pkey PRIMARY KEY (id);


--
-- Name: companies companies_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_code_key UNIQUE (code);


--
-- Name: companies companies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_pkey PRIMARY KEY (id);


--
-- Name: device_events device_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_events
    ADD CONSTRAINT device_events_pkey PRIMARY KEY (id);


--
-- Name: devices devices_company_id_device_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.devices
    ADD CONSTRAINT devices_company_id_device_uuid_key UNIQUE (company_id, device_uuid);


--
-- Name: devices devices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.devices
    ADD CONSTRAINT devices_pkey PRIMARY KEY (id);


--
-- Name: document_items document_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.document_items
    ADD CONSTRAINT document_items_pkey PRIMARY KEY (id);


--
-- Name: documents documents_cloud_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_cloud_id_key UNIQUE (cloud_id);


--
-- Name: documents documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_pkey PRIMARY KEY (id);


--
-- Name: image_metadata image_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.image_metadata
    ADD CONSTRAINT image_metadata_pkey PRIMARY KEY (id);


--
-- Name: images images_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.images
    ADD CONSTRAINT images_pkey PRIMARY KEY (id);


--
-- Name: inbound_notes inbound_notes_cloud_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inbound_notes
    ADD CONSTRAINT inbound_notes_cloud_id_key UNIQUE (cloud_id);


--
-- Name: inbound_notes inbound_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inbound_notes
    ADD CONSTRAINT inbound_notes_pkey PRIMARY KEY (id);


--
-- Name: ocr_results ocr_results_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ocr_results
    ADD CONSTRAINT ocr_results_pkey PRIMARY KEY (id);


--
-- Name: outbound_line_edit_history outbound_line_edit_history_cloud_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_line_edit_history
    ADD CONSTRAINT outbound_line_edit_history_cloud_id_key UNIQUE (cloud_id);


--
-- Name: outbound_line_edit_history outbound_line_edit_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_line_edit_history
    ADD CONSTRAINT outbound_line_edit_history_pkey PRIMARY KEY (id);


--
-- Name: outbound_line_status_history outbound_line_status_history_cloud_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_line_status_history
    ADD CONSTRAINT outbound_line_status_history_cloud_id_key UNIQUE (cloud_id);


--
-- Name: outbound_line_status_history outbound_line_status_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_line_status_history
    ADD CONSTRAINT outbound_line_status_history_pkey PRIMARY KEY (id);


--
-- Name: outbound_lines outbound_lines_cloud_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_lines
    ADD CONSTRAINT outbound_lines_cloud_id_key UNIQUE (cloud_id);


--
-- Name: outbound_lines outbound_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_lines
    ADD CONSTRAINT outbound_lines_pkey PRIMARY KEY (id);


--
-- Name: outbound_lists outbound_lists_cloud_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_lists
    ADD CONSTRAINT outbound_lists_cloud_id_key UNIQUE (cloud_id);


--
-- Name: outbound_lists outbound_lists_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_lists
    ADD CONSTRAINT outbound_lists_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: scan_events scan_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scan_events
    ADD CONSTRAINT scan_events_pkey PRIMARY KEY (id);


--
-- Name: scanned_codes scanned_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scanned_codes
    ADD CONSTRAINT scanned_codes_pkey PRIMARY KEY (id);


--
-- Name: subscriptions subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT subscriptions_pkey PRIMARY KEY (id);


--
-- Name: sync_metadata sync_metadata_device_id_company_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sync_metadata
    ADD CONSTRAINT sync_metadata_device_id_company_id_key UNIQUE (device_id, company_id);


--
-- Name: sync_metadata sync_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sync_metadata
    ADD CONSTRAINT sync_metadata_pkey PRIMARY KEY (id);


--
-- Name: users unique_company_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT unique_company_email UNIQUE (company_id, email);


--
-- Name: users unique_company_username; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT unique_company_username UNIQUE (company_id, username);


--
-- Name: images unique_entity_image; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.images
    ADD CONSTRAINT unique_entity_image UNIQUE (entity_type, entity_id);


--
-- Name: user_events user_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_events
    ADD CONSTRAINT user_events_pkey PRIMARY KEY (id);


--
-- Name: user_notifications user_notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT user_notifications_pkey PRIMARY KEY (id);


--
-- Name: user_warehouses user_warehouses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_warehouses
    ADD CONSTRAINT user_warehouses_pkey PRIMARY KEY (id);


--
-- Name: user_warehouses user_warehouses_user_id_warehouse_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_warehouses
    ADD CONSTRAINT user_warehouses_user_id_warehouse_id_key UNIQUE (user_id, warehouse_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_external_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_external_id_key UNIQUE (external_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: waitlist_entries waitlist_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.waitlist_entries
    ADD CONSTRAINT waitlist_entries_pkey PRIMARY KEY (id);


--
-- Name: warehouses warehouses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouses
    ADD CONSTRAINT warehouses_pkey PRIMARY KEY (id);


--
-- Name: web_session_transfers web_session_transfers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.web_session_transfers
    ADD CONSTRAINT web_session_transfers_pkey PRIMARY KEY (id);


--
-- Name: web_session_transfers web_session_transfers_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.web_session_transfers
    ADD CONSTRAINT web_session_transfers_token_hash_key UNIQUE (token_hash);


--
-- Name: idx_afip_tickets_expires; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_afip_tickets_expires ON public.afip_tickets USING btree (expiration_time);


--
-- Name: idx_audit_logs_action_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_action_type ON public.audit_logs USING btree (action_type);


--
-- Name: idx_audit_logs_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_company_id ON public.audit_logs USING btree (company_id);


--
-- Name: idx_audit_logs_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_created_at ON public.audit_logs USING btree (created_at);


--
-- Name: idx_audit_logs_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_user_id ON public.audit_logs USING btree (user_id);


--
-- Name: idx_audit_logs_warehouse_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_warehouse_id ON public.audit_logs USING btree (warehouse_id);


--
-- Name: idx_billing_factura_attempts_invoice_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_billing_factura_attempts_invoice_id ON public.billing_invoice_factura_attempts USING btree (invoice_id, attempted_at DESC);


--
-- Name: idx_billing_invoices_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_billing_invoices_company_id ON public.billing_invoices USING btree (company_id);


--
-- Name: idx_billing_invoices_issued_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_billing_invoices_issued_at ON public.billing_invoices USING btree (issued_at DESC);


--
-- Name: idx_billing_invoices_mp_payment_id_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_billing_invoices_mp_payment_id_unique ON public.billing_invoices USING btree (mp_payment_id) WHERE ((mp_payment_id IS NOT NULL) AND ((mp_payment_id)::text <> ''::text));


--
-- Name: idx_companies_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_code ON public.companies USING btree (code);


--
-- Name: idx_companies_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_companies_status ON public.companies USING btree (status);


--
-- Name: idx_device_events_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_device_events_company_id ON public.device_events USING btree (company_id);


--
-- Name: idx_device_events_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_device_events_created_at ON public.device_events USING btree (created_at);


--
-- Name: idx_device_events_device_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_device_events_device_id ON public.device_events USING btree (device_id);


--
-- Name: idx_device_events_event_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_device_events_event_type ON public.device_events USING btree (event_type);


--
-- Name: idx_devices_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_devices_company_id ON public.devices USING btree (company_id);


--
-- Name: idx_devices_device_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_devices_device_uuid ON public.devices USING btree (device_uuid);


--
-- Name: idx_devices_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_devices_status ON public.devices USING btree (status);


--
-- Name: idx_devices_warehouse_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_devices_warehouse_id ON public.devices USING btree (warehouse_id);


--
-- Name: idx_document_items_document_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_document_items_document_id ON public.document_items USING btree (document_id);


--
-- Name: idx_documents_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_documents_company_id ON public.documents USING btree (company_id);


--
-- Name: idx_documents_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_documents_status ON public.documents USING btree (status);


--
-- Name: idx_documents_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_documents_type ON public.documents USING btree (type);


--
-- Name: idx_documents_warehouse_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_documents_warehouse_id ON public.documents USING btree (warehouse_id);


--
-- Name: idx_image_metadata_scan_event_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_image_metadata_scan_event_id ON public.image_metadata USING btree (scan_event_id);


--
-- Name: idx_images_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_images_entity ON public.images USING btree (entity_type, entity_id);


--
-- Name: idx_images_uploaded_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_images_uploaded_at ON public.images USING btree (uploaded_at);


--
-- Name: idx_images_uploaded_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_images_uploaded_by ON public.images USING btree (uploaded_by);


--
-- Name: idx_images_warehouse; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_images_warehouse ON public.images USING btree (warehouse_id);


--
-- Name: idx_inbound_notes_cloud_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inbound_notes_cloud_id ON public.inbound_notes USING btree (cloud_id);


--
-- Name: idx_inbound_notes_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inbound_notes_company_id ON public.inbound_notes USING btree (company_id);


--
-- Name: idx_inbound_notes_company_updated_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inbound_notes_company_updated_at ON public.inbound_notes USING btree (company_id, updated_at);


--
-- Name: idx_inbound_notes_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inbound_notes_status ON public.inbound_notes USING btree (status);


--
-- Name: idx_inbound_notes_updated_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_inbound_notes_updated_at ON public.inbound_notes USING btree (updated_at);


--
-- Name: idx_ocr_results_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ocr_results_company_id ON public.ocr_results USING btree (company_id);


--
-- Name: idx_ocr_results_document_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ocr_results_document_id ON public.ocr_results USING btree (document_id);


--
-- Name: idx_ocr_results_processed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ocr_results_processed_at ON public.ocr_results USING btree (processed_at);


--
-- Name: idx_outbound_line_edit_history_cloud_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbound_line_edit_history_cloud_id ON public.outbound_line_edit_history USING btree (cloud_id);


--
-- Name: idx_outbound_line_edit_history_line_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbound_line_edit_history_line_id ON public.outbound_line_edit_history USING btree (outbound_line_id);


--
-- Name: idx_outbound_line_status_history_cloud_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbound_line_status_history_cloud_id ON public.outbound_line_status_history USING btree (cloud_id);


--
-- Name: idx_outbound_line_status_history_line_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbound_line_status_history_line_id ON public.outbound_line_status_history USING btree (outbound_line_id);


--
-- Name: idx_outbound_lines_cloud_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbound_lines_cloud_id ON public.outbound_lines USING btree (cloud_id);


--
-- Name: idx_outbound_lines_outbound_list_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbound_lines_outbound_list_id ON public.outbound_lines USING btree (outbound_list_id);


--
-- Name: idx_outbound_lines_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbound_lines_status ON public.outbound_lines USING btree (status);


--
-- Name: idx_outbound_lists_cloud_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbound_lists_cloud_id ON public.outbound_lists USING btree (cloud_id);


--
-- Name: idx_outbound_lists_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbound_lists_company_id ON public.outbound_lists USING btree (company_id);


--
-- Name: idx_outbound_lists_company_updated_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbound_lists_company_updated_at ON public.outbound_lists USING btree (company_id, updated_at);


--
-- Name: idx_outbound_lists_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbound_lists_status ON public.outbound_lists USING btree (status);


--
-- Name: idx_outbound_lists_updated_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_outbound_lists_updated_at ON public.outbound_lists USING btree (updated_at);


--
-- Name: idx_password_reset_tokens_token_hash; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_password_reset_tokens_token_hash ON public.password_reset_tokens USING btree (token_hash) WHERE (used_at IS NULL);


--
-- Name: idx_password_reset_tokens_user_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_password_reset_tokens_user_active ON public.password_reset_tokens USING btree (user_id) WHERE (used_at IS NULL);


--
-- Name: idx_refresh_tokens_expires_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_expires_at ON public.refresh_tokens USING btree (expires_at);


--
-- Name: idx_refresh_tokens_token_hash; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_token_hash ON public.refresh_tokens USING btree (token_hash);


--
-- Name: idx_refresh_tokens_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_user_id ON public.refresh_tokens USING btree (user_id);


--
-- Name: idx_scan_events_processed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scan_events_processed_at ON public.scan_events USING btree (processed_at);


--
-- Name: idx_scan_events_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scan_events_user_id ON public.scan_events USING btree (user_id);


--
-- Name: idx_scan_events_warehouse_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scan_events_warehouse_id ON public.scan_events USING btree (warehouse_id);


--
-- Name: idx_scanned_codes_document_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scanned_codes_document_id ON public.scanned_codes USING btree (document_id);


--
-- Name: idx_scanned_codes_document_item_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scanned_codes_document_item_id ON public.scanned_codes USING btree (document_item_id);


--
-- Name: idx_scanned_codes_raw_value; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scanned_codes_raw_value ON public.scanned_codes USING btree (raw_value);


--
-- Name: idx_subscriptions_device_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_subscriptions_device_id ON public.subscriptions USING btree (device_id);


--
-- Name: idx_subscriptions_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_subscriptions_status ON public.subscriptions USING btree (status);


--
-- Name: idx_subscriptions_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_subscriptions_user_id ON public.subscriptions USING btree (user_id);


--
-- Name: idx_sync_metadata_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_metadata_company_id ON public.sync_metadata USING btree (company_id);


--
-- Name: idx_sync_metadata_device_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_metadata_device_id ON public.sync_metadata USING btree (device_id);


--
-- Name: idx_user_events_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_events_company_id ON public.user_events USING btree (company_id);


--
-- Name: idx_user_events_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_events_created_at ON public.user_events USING btree (created_at);


--
-- Name: idx_user_events_event_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_events_event_type ON public.user_events USING btree (event_type);


--
-- Name: idx_user_events_performed_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_events_performed_by ON public.user_events USING btree (performed_by);


--
-- Name: idx_user_events_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_events_user_id ON public.user_events USING btree (user_id);


--
-- Name: idx_user_notifications_company_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_notifications_company_created ON public.user_notifications USING btree (company_id, created_at DESC);


--
-- Name: idx_user_notifications_unread; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_notifications_unread ON public.user_notifications USING btree (user_id) WHERE (read_at IS NULL);


--
-- Name: idx_user_notifications_user_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_notifications_user_created ON public.user_notifications USING btree (user_id, created_at DESC);


--
-- Name: idx_user_warehouses_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_warehouses_company_id ON public.user_warehouses USING btree (company_id);


--
-- Name: idx_user_warehouses_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_warehouses_role ON public.user_warehouses USING btree (role);


--
-- Name: idx_user_warehouses_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_warehouses_user_id ON public.user_warehouses USING btree (user_id);


--
-- Name: idx_user_warehouses_warehouse_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_warehouses_warehouse_id ON public.user_warehouses USING btree (warehouse_id);


--
-- Name: idx_users_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_company_id ON public.users USING btree (company_id);


--
-- Name: idx_users_role_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_role_id ON public.users USING btree (role_id);


--
-- Name: idx_users_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_status ON public.users USING btree (status);


--
-- Name: idx_warehouses_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouses_company_id ON public.warehouses USING btree (company_id);


--
-- Name: idx_warehouses_company_id_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_warehouses_company_id_active ON public.warehouses USING btree (company_id) WHERE (archived_at IS NULL);


--
-- Name: idx_web_session_transfers_expires_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_web_session_transfers_expires_at ON public.web_session_transfers USING btree (expires_at);


--
-- Name: idx_web_session_transfers_used_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_web_session_transfers_used_at ON public.web_session_transfers USING btree (used_at);


--
-- Name: idx_web_session_transfers_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_web_session_transfers_user_id ON public.web_session_transfers USING btree (user_id);


--
-- Name: waitlist_entries_email_normalized_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX waitlist_entries_email_normalized_key ON public.waitlist_entries USING btree (email_normalized);


--
-- Name: audit_logs audit_logs_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: audit_logs audit_logs_device_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_device_id_fkey FOREIGN KEY (device_id) REFERENCES public.devices(id) ON DELETE SET NULL;


--
-- Name: audit_logs audit_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: audit_logs audit_logs_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id) ON DELETE SET NULL;


--
-- Name: billing_invoice_factura_attempts billing_invoice_factura_attempts_invoice_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billing_invoice_factura_attempts
    ADD CONSTRAINT billing_invoice_factura_attempts_invoice_id_fkey FOREIGN KEY (invoice_id) REFERENCES public.billing_invoices(id) ON DELETE CASCADE;


--
-- Name: billing_invoices billing_invoices_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billing_invoices
    ADD CONSTRAINT billing_invoices_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: device_events device_events_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_events
    ADD CONSTRAINT device_events_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: device_events device_events_device_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_events
    ADD CONSTRAINT device_events_device_id_fkey FOREIGN KEY (device_id) REFERENCES public.devices(id) ON DELETE CASCADE;


--
-- Name: device_events device_events_performed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_events
    ADD CONSTRAINT device_events_performed_by_fkey FOREIGN KEY (performed_by) REFERENCES public.users(id);


--
-- Name: device_events device_events_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_events
    ADD CONSTRAINT device_events_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: devices devices_approved_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.devices
    ADD CONSTRAINT devices_approved_by_fkey FOREIGN KEY (approved_by) REFERENCES public.users(id);


--
-- Name: devices devices_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.devices
    ADD CONSTRAINT devices_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: devices devices_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.devices
    ADD CONSTRAINT devices_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id) ON DELETE RESTRICT;


--
-- Name: document_items document_items_document_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.document_items
    ADD CONSTRAINT document_items_document_id_fkey FOREIGN KEY (document_id) REFERENCES public.documents(id) ON DELETE CASCADE;


--
-- Name: documents documents_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: documents documents_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: documents documents_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id) ON DELETE RESTRICT;


--
-- Name: image_metadata image_metadata_scan_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.image_metadata
    ADD CONSTRAINT image_metadata_scan_event_id_fkey FOREIGN KEY (scan_event_id) REFERENCES public.scan_events(id);


--
-- Name: images images_uploaded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.images
    ADD CONSTRAINT images_uploaded_by_fkey FOREIGN KEY (uploaded_by) REFERENCES public.users(id);


--
-- Name: images images_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.images
    ADD CONSTRAINT images_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: inbound_notes inbound_notes_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inbound_notes
    ADD CONSTRAINT inbound_notes_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: inbound_notes inbound_notes_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.inbound_notes
    ADD CONSTRAINT inbound_notes_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id) ON DELETE SET NULL;


--
-- Name: ocr_results ocr_results_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ocr_results
    ADD CONSTRAINT ocr_results_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: outbound_line_edit_history outbound_line_edit_history_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_line_edit_history
    ADD CONSTRAINT outbound_line_edit_history_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: outbound_line_edit_history outbound_line_edit_history_outbound_line_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_line_edit_history
    ADD CONSTRAINT outbound_line_edit_history_outbound_line_id_fkey FOREIGN KEY (outbound_line_id) REFERENCES public.outbound_lines(id) ON DELETE CASCADE;


--
-- Name: outbound_line_status_history outbound_line_status_history_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_line_status_history
    ADD CONSTRAINT outbound_line_status_history_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: outbound_line_status_history outbound_line_status_history_outbound_line_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_line_status_history
    ADD CONSTRAINT outbound_line_status_history_outbound_line_id_fkey FOREIGN KEY (outbound_line_id) REFERENCES public.outbound_lines(id) ON DELETE CASCADE;


--
-- Name: outbound_lines outbound_lines_inbound_note_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_lines
    ADD CONSTRAINT outbound_lines_inbound_note_id_fkey FOREIGN KEY (inbound_note_id) REFERENCES public.inbound_notes(id) ON DELETE SET NULL;


--
-- Name: outbound_lines outbound_lines_outbound_list_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_lines
    ADD CONSTRAINT outbound_lines_outbound_list_id_fkey FOREIGN KEY (outbound_list_id) REFERENCES public.outbound_lists(id) ON DELETE CASCADE;


--
-- Name: outbound_lists outbound_lists_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_lists
    ADD CONSTRAINT outbound_lists_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: outbound_lists outbound_lists_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbound_lists
    ADD CONSTRAINT outbound_lists_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id) ON DELETE SET NULL;


--
-- Name: password_reset_tokens password_reset_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: refresh_tokens refresh_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: scanned_codes scanned_codes_device_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scanned_codes
    ADD CONSTRAINT scanned_codes_device_id_fkey FOREIGN KEY (device_id) REFERENCES public.devices(id);


--
-- Name: scanned_codes scanned_codes_document_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scanned_codes
    ADD CONSTRAINT scanned_codes_document_id_fkey FOREIGN KEY (document_id) REFERENCES public.documents(id) ON DELETE CASCADE;


--
-- Name: scanned_codes scanned_codes_document_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scanned_codes
    ADD CONSTRAINT scanned_codes_document_item_id_fkey FOREIGN KEY (document_item_id) REFERENCES public.document_items(id) ON DELETE SET NULL;


--
-- Name: scanned_codes scanned_codes_scanned_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scanned_codes
    ADD CONSTRAINT scanned_codes_scanned_by_fkey FOREIGN KEY (scanned_by) REFERENCES public.users(id);


--
-- Name: subscriptions subscriptions_device_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT subscriptions_device_id_fkey FOREIGN KEY (device_id) REFERENCES public.devices(id) ON DELETE SET NULL;


--
-- Name: subscriptions subscriptions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT subscriptions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: sync_metadata sync_metadata_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sync_metadata
    ADD CONSTRAINT sync_metadata_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: sync_metadata sync_metadata_device_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sync_metadata
    ADD CONSTRAINT sync_metadata_device_id_fkey FOREIGN KEY (device_id) REFERENCES public.devices(id) ON DELETE CASCADE;


--
-- Name: user_events user_events_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_events
    ADD CONSTRAINT user_events_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: user_events user_events_performed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_events
    ADD CONSTRAINT user_events_performed_by_fkey FOREIGN KEY (performed_by) REFERENCES public.users(id);


--
-- Name: user_events user_events_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_events
    ADD CONSTRAINT user_events_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_notifications user_notifications_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT user_notifications_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: user_notifications user_notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT user_notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_warehouses user_warehouses_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_warehouses
    ADD CONSTRAINT user_warehouses_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: user_warehouses user_warehouses_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_warehouses
    ADD CONSTRAINT user_warehouses_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: user_warehouses user_warehouses_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_warehouses
    ADD CONSTRAINT user_warehouses_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: users users_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: users users_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- Name: users users_warehouse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.warehouses(id);


--
-- Name: warehouses warehouses_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouses
    ADD CONSTRAINT warehouses_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: web_session_transfers web_session_transfers_desktop_refresh_token_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.web_session_transfers
    ADD CONSTRAINT web_session_transfers_desktop_refresh_token_id_fkey FOREIGN KEY (desktop_refresh_token_id) REFERENCES public.refresh_tokens(id);


--
-- Name: web_session_transfers web_session_transfers_phone_refresh_token_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.web_session_transfers
    ADD CONSTRAINT web_session_transfers_phone_refresh_token_id_fkey FOREIGN KEY (phone_refresh_token_id) REFERENCES public.refresh_tokens(id);


--
-- Name: web_session_transfers web_session_transfers_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.web_session_transfers
    ADD CONSTRAINT web_session_transfers_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);

SET search_path TO public;
