import { apiClient } from "../apiClient";
import type { NotificationResponse } from "../types/notification";

export async function listNotificationsByUser(userId: number): Promise<NotificationResponse[]> {
  return apiClient<NotificationResponse[]>(`/api/notifications?userId=${userId}`);
}
