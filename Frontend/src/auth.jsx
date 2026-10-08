import { createContext, useContext, useEffect, useState } from 'react'
import { get, post } from './api'
import { startRealtime, stopRealtime } from './realtime'

const AuthCtx = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('wc_user')
    return raw ? JSON.parse(raw) : null
  })

  // səhifə yenilənəndə də canlı bağlantı qurulur (token localStorage-dadır)
  useEffect(() => {
    if (localStorage.getItem('wc_token')) startRealtime()
    return () => stopRealtime()
  }, [])

  function save(auth) {
    localStorage.setItem('wc_token', auth.accessToken)
    localStorage.setItem('wc_user', JSON.stringify(auth.user))
    setUser(auth.user)
    startRealtime()
  }

  const value = {
    user,
    async login(email, password) {
      save(await post('/auth/login', { email, password }))
    },
    async register(username, email, password) {
      save(await post('/auth/register', { username, email, password }))
    },
    logout() {
      localStorage.removeItem('wc_token')
      localStorage.removeItem('wc_user')
      stopRealtime()
      setUser(null)
      location.hash = '#feed'
    },
    async refreshMe() {
      const fresh = await get('/users/me')
      localStorage.setItem('wc_user', JSON.stringify(fresh))
      setUser(fresh)
      return fresh
    },
  }

  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>
}

export const useAuth = () => useContext(AuthCtx)
