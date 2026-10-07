import http from './http'

export const authApi = {
  login: (payload) => http.post('/auth/login', payload),
  register: (payload) => http.post('/auth/register', payload),
  me: () => http.get('/auth/me'),
  updateProfile: (payload) => http.put('/auth/me', payload),
  changePassword: (payload) => http.put('/auth/password', payload),
}

export const roomApi = {
  list: (params) => http.get('/rooms', { params }),
  detail: (id, params) => http.get(`/rooms/${id}`, { params }),
  availability: (id, date) =>
    http.get(`/rooms/${id}/availability`, { params: { date } }),
  buildings: () => http.get('/rooms/options/buildings'),
  equipment: () => http.get('/rooms/options/equipment'),
  recommendations: (params) => http.get('/rooms/recommendations', { params }),
  favorite: (id) => http.post(`/rooms/${id}/favorite`),
  unfavorite: (id) => http.delete(`/rooms/${id}/favorite`),
}

export const bookingApi = {
  create: (payload) => http.post('/bookings', payload),
  mine: (params) => http.get('/bookings/mine', { params }),
  cancel: (id, payload) => http.post(`/bookings/${id}/cancel`, payload),
}

export const notificationApi = {
  list: () => http.get('/notifications'),
  unreadCount: () => http.get('/notifications/unread-count'),
  markRead: (id) => http.post(`/notifications/${id}/read`),
  markAllRead: () => http.post('/notifications/read-all'),
}

export const adminApi = {
  bookings: (params) => http.get('/admin/bookings', { params }),
  approve: (id, payload) => http.post(`/admin/bookings/${id}/approval`, payload),
  stats: () => http.get('/admin/stats'),
  rooms: () => http.get('/admin/rooms'),
  createRoom: (payload) => http.post('/admin/rooms', payload),
  updateRoom: (id, payload) => http.put(`/admin/rooms/${id}`, payload),
  disableRoom: (id) => http.delete(`/admin/rooms/${id}`),
  updateRoomStatus: (id, status) =>
    http.put(`/admin/rooms/${id}/status`, { status }),
  unavailableRooms: () => http.get('/admin/rooms/unavailable'),
  createUnavailable: (payload) => http.post('/admin/rooms/unavailable', payload),
  users: () => http.get('/admin/users'),
  revokeApprover: (id) => http.delete(`/admin/users/${id}/approver`),
  grantApprover: (id) => http.post(`/admin/users/${id}/approver`),
  deleteUser: (id) => http.delete(`/admin/users/${id}`),
  updateBookingPermission: (id, payload) =>
    http.put(`/admin/users/${id}/booking-permission`, payload),
  auditLogs: () => http.get('/admin/audit-logs'),
}

