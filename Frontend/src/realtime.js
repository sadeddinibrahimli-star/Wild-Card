import { Client } from '@stomp/stompjs'

/**
 * Doc 3: "Real-time layer: WebSocket (STOMP over SockJS) for live notifications".
 *
 * Backend: ws://<host>/ws - JWT via the Authorization header in CONNECT.
 *
 * RULE: the connection is never required.
 *   - when connected, messages arrive instantly
 *   - when not, the old polling (chat 3s, notifications 5s) keeps working
 * Everything here is inside try/catch - no error may reach the UI.
 */

let client = null
let status = 'idle' // idle | connecting | active | failed
const listeners = new Set()

function notify() {
  listeners.forEach((fn) => {
    try {
      fn(status)
    } catch {
      /* a listener error must not affect the UI */
    }
  })
}

export function realtimeStatus() {
  return status
}

/** Observe the connection status (cancelled when the component unmounts). */
export function onRealtimeStatus(fn) {
  listeners.add(fn)
  return () => listeners.delete(fn)
}

function token() {
  return localStorage.getItem('wc_token')
}

/** Open the connection. Do nothing without a token. */
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
        // after a framework error the polling still works
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
    }
  }
  if (resetStatus) {
    status = 'idle'
    notify()
  }
}

/**
 * Subscribe to a topic. Returns a no-op when not connected (does nothing).
 * Incoming messages are parsed as JSON; a parse failure drops the message.
 */
export function subscribe(destination, handler) {
  if (!client || !client.connected) return () => {}

  let subscription
  try {
    subscription = client.subscribe(destination, (msg) => {
      try {
        handler(JSON.parse(msg.body))
      } catch {
        /* incomplete message - dropped */
      }
    })
  } catch {
    return () => {}
  }

  return () => {
    try {
      subscription.unsubscribe()
    } catch {
    }
  }
}

/** Send a message to an /app/... destination. Returns false when not connected (the REST fallback stays). */
export function send(destination, payload) {
  if (!client || !client.connected) return false
  try {
    client.publish({ destination, body: JSON.stringify(payload ?? {}) })
    return true
  } catch {
    return false
  }
}
