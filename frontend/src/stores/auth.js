import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '../api'

const TOKEN_KEY = 'meeting_room_token'
const USER_KEY = 'meeting_room_user'

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(readStoredUser())

  const isAuthenticated = computed(() => Boolean(token.value))
  const roles = computed(() => user.value?.roles || [])
  const isAdmin = computed(() => roles.value.includes('ADMIN'))
  const isApprover = computed(
    () => roles.value.includes('APPROVER') || roles.value.includes('ADMIN'),
  )

  function persist(nextToken, nextUser) {
    token.value = nextToken
    user.value = nextUser
    localStorage.setItem(TOKEN_KEY, nextToken)
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
  }

  async function login(payload) {
    const result = await authApi.login(payload)
    persist(result.token, result.user)
    return result
  }

  async function register(payload) {
    const result = await authApi.register(payload)
    persist(result.token, result.user)
    return result
  }

  async function fetchMe() {
    const profile = await authApi.me()
    user.value = profile
    localStorage.setItem(USER_KEY, JSON.stringify(profile))
    return profile
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    user,
    roles,
    isAuthenticated,
    isAdmin,
    isApprover,
    login,
    register,
    fetchMe,
    logout,
  }
})

