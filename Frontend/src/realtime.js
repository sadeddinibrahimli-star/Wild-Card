import { Client } from '@stomp/stompjs'

/**
 * Doc 3: "Real-time layer: WebSocket (STOMP over SockJS) for live notifications".
 *
 * Backend: ws://<host>/ws  · JWT CONNECT çərçivəsində Authorization başlığı ilə.
 *
 * QAYDA: bağlantı heç vaxt məcburi deyil.
 *   - qoşulduqsa mesajlar ani gəlir
 *   - qoşulmasa köhnə polling (chat 3s, bildiriş 5s) olduğu kimi işləyir
 * Buna görə buradakı hər şey try/catch içindədir - heç bir xəta UI-a çıxmamalıdır.
 */

let client = null
let status = 'idle' // idle | connecting | active | failed
const listeners = new Set()

function notify() {
  listeners.forEach((fn) => {
    try {
      fn(status)
    } catch {
      /* dinləyici xətası UI-a təsir etməməlidir */
    }
  })
}

export function realtimeStatus() {
  return status
}

/** Bağlantı vəziyyətinə qulaq as (komponent unmount edəndə ləğv olunur). */
export function onRealtimeStatus(fn) {
  listeners.add(fn)
  return () => listeners.delete(fn)
}

function token() {
  return localStorage.getItem('wc_token')
}

/** Bağlantını aç. Token yoxdursa heç nə etmə. */
export function startRealtime() {
  const t = token()
  if (!t) return
  if (client && (client.active || status === 'connecting' || status === 'active')) return

  stopRealtime(false)
  status = 'connecting'
  notify()

  try {
    const scheme = location.protocol === 'https:' ? 'wss' : 'ws'
    client = new Client({
      brokerURL: `${scheme}://${location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${t}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        status = 'active'
        notify()
      },
      onDisconnect: () => {
        status = 'idle'
        notify()
      },
      onStompError: () => {
        // səhv framework-dən sonra da polling işləyir
        status = 'failed'
        notify()
      },
      onWebSocketClose: () => {
        if (status === 'active') {
          status = 'connecting'
          notify()
        }
      },
    })
    client.activate()
  } catch {
    status = 'failed'
    notify()
  }
}

export function stopRealtime(resetStatus = true) {
  const c = client
  client = null
  if (c) {
    try {
      c.deactivate()
    } catch {
      /* önemsiz */
    }
  }
  if (resetStatus) {
    status = 'idle'
    notify()
  }
}

/**
 * Mövzuya abunə ol. Qoşulmayıbsa boş funksiya qaytarır (heç nəmir).
 * Gələn mesaj JSON kimi parse olunur, parse uğursuzsa cavab işlənmir.
 */
export function subscribe(destination, handler) {
  if (!client || !client.connected) return () => {}

  let subscription
  try {
    subscription = client.subscribe(destination, (msg) => {
      try {
        handler(JSON.parse(msg.body))
      } catch {
        /* natamam mesaj - atılır */
      }
    })
  } catch {
    return () => {}
  }

  return () => {
    try {
      subscription.unsubscribe()
    } catch {
      /* önemsiz */
    }
  }
}

/** /app/... yola mesaj göndər. Qoşulmayıbsa false (REST fallback qalır). */
export function send(destination, payload) {
  if (!client || !client.connected) return false
  try {
    client.publish({ destination, body: JSON.stringify(payload ?? {}) })
    return true
  } catch {
    return false
  }
}
