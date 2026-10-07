import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('meeting_room_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload?.code !== 0) {
      const message = payload?.message || '请求失败'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return payload.data
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络请求失败'
    if (status === 401) {
      localStorage.removeItem('meeting_room_token')
      localStorage.removeItem('meeting_room_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  },
)

export default http

