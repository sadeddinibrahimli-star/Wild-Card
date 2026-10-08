import axios from 'axios'

const client = axios.create({ baseURL: '/api/v1' })

// her soruga token elave olunur
client.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('wc_token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

// 401 olsa tokeni silib login-e at
client.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401 && !err.config?.url?.includes('/auth/')) {
      localStorage.removeItem('wc_token')
      localStorage.removeItem('wc_user')
      location.hash = '#feed'
    }
    return Promise.reject(err)
  },
)

export const get = (url, config) => client.get(url, config).then((r) => r.data.data)
export const post = (url, data) => client.post(url, data).then((r) => r.data.data)
export const put = (url, data) => client.put(url, data).then((r) => r.data.data)
export const patch = (url, data) => client.patch(url, data).then((r) => r.data.data)
export const del = (url) => client.delete(url).then((r) => r.data.data)
export const upload = (file, kind = 'post') => {
  const form = new FormData()
  form.append('file', file)
  form.append('kind', kind)
  return client
    .post('/files', form, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((r) => r.data.data)
}
export const errMsg = (e, fallback = 'Xeta') =>
  e?.response?.data?.message || e?.message || fallback
