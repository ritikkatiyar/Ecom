export interface NotificationResponse {
  id: number;
  userId: number;
  type: string;
  channel: string;
  recipient: string;
  subject: string;
  body: string;
  status: string;
  errorReason?: string;
  sourceTopic?: string;
  sourceEventId?: string;
  payload?: string;
  createdAt: string;
  updatedAt: string;
}
