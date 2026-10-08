/**
 * Wild-Card STOMP/WebSocket probe (the backend must run on 8080).
 *
 *   cd QA && node wsprobe.js
 *
 * Checks (doc 4.3 real-time notifications):
 *   1. CONNECT with JWT -> CONNECTED (user-name)
 *   2. a lower case `authorization` header also works (header fix)
 *   3. no / wrong token -> the connection is rejected
 *   4. /user/queue/alerts subscription -> a follow notification arrives live
 */
const WebSocket = require('ws')

const API = process.env.API || 'http://localhost:8080/api/v1'
const WS_URL = process.env.WS_URL || 'ws://localhost:8080/ws'
const ADMIN_EMAIL = process.env.ADMIN_EMAIL || 'admin@wildcard.com'
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'admin123'

const results = []
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

function check(name, ok, extra = '') {
  results.push({ name, ok: !!ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${extra ? '  -- ' + extra : ''}`)
}

async function api(method, path, token, body) {
  const res = await fetch(API + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  let json = null
  try {
    json = await res.json()
  } catch {}
  return { status: res.status, data: json && json.data }
}

function stompConnectFrame(authHeader) {
  return (
    'CONNECT\naccept-version:1.2\nheart-beat:0,0\n' +
    (authHeader ? authHeader + '\n' : '') +
    '\n\0'
  )
}

/** WS + STOMP CONNECT; waits for the events with a Promise. */
function openStomp(headers, { subscribeAlerts = false } = {}) {
  return new Promise((resolve) => {
    const ws = new WebSocket(WS_URL, { headers })
    const state = { ws, connected: false, error: null, frames: [], alerts: [], done: false }
    const finish = () => {
      if (!state.done) {
        state.done = true
        resolve(state)
      }
    }
    const timer = setTimeout(() => finish(), 7000)

    ws.on('open', () => ws.send(stompConnectFrame(headers && headers.Authorization ? 'Authorization:' + headers.Authorization : headers && headers.authorization ? 'authorization:' + headers.authorization : null)))
    ws.on('message', (raw) => {
      const text = raw.toString()
      state.frames.push(text)
      if (text.startsWith('CONNECTED')) {
        state.connected = true
        if (subscribeAlerts) {
          ws.send('SUBSCRIBE\nid:sub-1\ndestination:/user/queue/alerts\n\n\0')
          resolve(state) // subscription sent -> continue
          state.done = true
          clearTimeout(timer)
        } else {
          clearTimeout(timer)
          finish()
        }
      }
      if (text.startsWith('ERROR')) {
        state.error = text.slice(0, 160)
        clearTimeout(timer)
        finish()
      }
      if (text.includes('/user/queue/alerts') && text.includes('FOLLOW')) {
        state.alerts.push(text)
        clearTimeout(timer)
        finish()
      }
    })
    ws.on('close', () => finish())
    ws.on('error', (e) => {
      state.error = state.error || String(e.message || e)
      finish()
    })
    state.close = () => {
      clearTimeout(timer)
      try {
        ws.close()
      } catch {}
    }
  })
}

async function main() {
  // ---------------------------------------------------------- login
  const adminLogin = await api('POST', '/auth/login', null, {
    email: ADMIN_EMAIL,
    password: ADMIN_PASSWORD,
  })
  const adminToken = adminLogin.data && (adminLogin.data.accessToken || adminLogin.data.token)
  check('Admin login (token alındı)', !!adminToken)
  if (!adminToken) return finish()

  const adminMe = await api('GET', '/users/me', adminToken)
  const adminId = adminMe.data && adminMe.data.id

  const uname = 'wsprobe_' + String(Date.now()).slice(-6)
  const reg = await api('POST', '/auth/register', null, {
    username: uname,
    email: uname + '@example.com',
    password: 'Passw0rd123',
  })
  const tempToken = reg.data && (reg.data.accessToken || reg.data.token)
  check('Temp istifadəçi register', !!tempToken)
  const tempMe = tempToken ? await api('GET', '/users/me', tempToken) : { data: null }
  const tempId = tempMe.data && tempMe.data.id

  // ---------------------------------------------------------- 1. JWT CONNECT
  let s = await openStomp({ Authorization: 'Bearer ' + adminToken })
  check('STOMP CONNECT (Authorization:Bearer) → CONNECTED', s.connected,
        s.connected ? (s.frames[0].match(/user-name:(.*)/) || ['', '?'])[1] && 'user-name ok' : s.error || 'timeout')
  s.close && s.close()

  // ---------------------------------------------------------- 2. lowercase header
  s = await openStomp({ authorization: 'Bearer ' + adminToken })
  check('Kiçik hərf `authorization` header → CONNECTED (halef)', s.connected,
        s.connected ? '' : s.error || 'timeout')
  s.close && s.close()

  // ---------------------------------------------------------- 3. rejects
  s = await openStomp({ Authorization: 'Bearer not-a-real-token' })
  check('Düzgün olmayan token rədd edilir', !s.connected,
        s.connected ? 'CONNECTED qayıtdı!' : (s.error ? 'ERROR/closed' : 'closed'))
  s.close && s.close()

  s = await openStomp({})
  check('Token-suz CONNECT rədd edilir', !s.connected,
        s.connected ? 'CONNECTED qayıtdı!' : (s.error ? 'ERROR/closed' : 'closed'))
  s.close && s.close()

  // ---------------------------------------------------------- 4. real-time push
  if (tempId && adminId) {
    s = await openStomp({ Authorization: 'Bearer ' + adminToken }, { subscribeAlerts: true })
    check('/user/queue/alerts SUBSCRIBE göndərildi', s.connected, s.error || '')
    if (s.connected) {
      // follow the temp admin -> the backend must push after the commit
      const r = await api('POST', `/social/follow/${adminId}`, tempToken)
      check('Temp admin-i follow etdi (push tetikləyir)', [200, 201, 204].includes(r.status),
            'status=' + r.status)
      // wait for delivery (max 6s)
      const t0 = Date.now()
      while (Date.now() - t0 < 6000 && s.alerts.length === 0) await sleep(200)
      check('Bildiriş real vaxtda çatdı (FOLLOW)', s.alerts.length > 0,
            s.alerts.length ? '' : '6s-də frame gəlmədi')
    } else {
      check('Temp admin-i follow etdi (push tetikləyir)', false, 'abunəlik alınmadı')
      check('Bildiriş real vaxtda çatdı (FOLLOW)', false, 'abunəlik yox idi')
    }
    s.close && s.close()
  } else {
    check('/user/queue/alerts SUBSCRIBE göndərildi', false, 'id-lər yoxdur')
    check('Bildiriş real vaxtda çatdı (FOLLOW)', false, 'id-lər yoxdur')
  }

  return finish()
}

function finish() {
  const passed = results.filter((r) => r.ok).length
  const total = results.length
  console.log('\n-----------------------------------------')
  console.log(`WSPROBE: ${passed}/${total} PASS`)
  process.exit(passed === total && total > 0 ? 0 : 1)
}

main().catch((e) => {
  console.error('WSPROBE crashed:', e)
  process.exit(1)
})
