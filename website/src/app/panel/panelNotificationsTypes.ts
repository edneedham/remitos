export type PanelNotification = {
  id: string;
  kind: string;
  title: string;
  body?: string | null;
  action_url?: string | null;
  read_at?: string | null;
  created_at: string;
};

export type PanelNotificationsResponse = {
  notifications: PanelNotification[];
  unread_count: number;
};
