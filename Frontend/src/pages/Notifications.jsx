import { useEffect, useMemo, useRef, useState } from 'react'
import { errMsg, get, put } from '../api'
import Icon from '../components/Icon'

const TABS = [
  ['all', 'All'],
  ['follows', 'Follows'],
  ['likes', 'Likes'],
]

const ICON_FOR = {
  FOLLOW: 'people',
  REACTION: 'heart',
  COMMENT: 'message',
  REPORT_RESOLVED: 'shield',
  SYSTEM: 'trophy',
}

const COLOR_FOR = {
  FOLLOW: 'var(--rarity-rare)',
  REACTION: 'var(--cat-anime)',
  COMMENT: 'var(--cat-music)',
  REPORT_RESOLVED: 'var(--accent)',
  SYSTEM: 'var(--accent)',
}

export function Notifications() {
  const [items, setItems] = useState(null)
  const [error, setError] = useState(null)
  const [tab, setTab] = useState('all')
  const [toast, setToast] = useState(null)
  const seen = useRef(new Set())

  async function load(silent) {
    try {
      const page = await get(`/notifications?size=40&filter=${tab}`)
      const fresh = page.content

      // a SYSTEM notification is treated as an achievement popup
      if (silent) {
        const winner = fresh.find((n) => n.type === 'SYSTEM' && !seen.current.has(n.id))
        if (winner) {
          seen.current.add(winner.id)
          setToast(winner)
          setTimeout(() => setToast(null), 5200)
        }
      } else {
        fresh.forEach((n) => seen.current.add(n.id))
      }

      setItems(fresh)
      setError(null)
    } catch (e) {
      setError(errMsg(e, 'Could not load notifications'))
      setItems([])
    }
  }

  // 5s polling - still refreshed without a WebSocket (fallback)
  useEffect(() => {
    load(false)
    const id = setInterval(() => load(true), 5000)
    return () => clearInterval(id)
  }, [tab])

  // refresh immediately when news arrives over the WebSocket (doc 4.3)
  useEffect(() => {
    const onAlert = () => load(true)
    window.addEventListener('wc:alerts', onAlert)
    return () => window.removeEventListener('wc:alerts', onAlert)
  }, [tab])

  async function readAll() {
    try {
      await put('/notifications/read-all', {})
      await load(false)
    } catch (err) {
      setError(errMsg(err, 'Could not mark as read'))
    }
  }

  const shown = (items || []).filter((n) => {
    if (tab === 'follows') return n.type === 'FOLLOW'
    if (tab === 'likes') return n.type === 'REACTION' || n.type === 'COMMENT'
    return true
  })

  const grouped = useMemo(() => {
    const today = new Date().toDateString()
    const out = { TODAY: [], EARLIER: [] }
    for (const n of shown) {
      const isToday = new Date(n.createdAt).toDateString() === today
      ;(isToday ? out.TODAY : out.EARLIER).push(n)
    }
    return out
  }, [shown])

  return (
    <div className="section alerts-page">
      {error && <div className="alert">{error}</div>}

      <div className="card">
        <div className="row between">
          <h3>Notifications</h3>
          <button className="link" onClick={readAll}>
            Mark all read
          </button>
        </div>

        <nav className="tabs small">
          {TABS.map(([key, label]) => (
            <button key={key} className={tab === key ? 'tab active' : 'tab'} onClick={() => setTab(key)}>
              {label}
            </button>
          ))}
        </nav>

        {items === null && <p className="muted small">Loading...</p>}
        {items?.length === 0 && <p className="muted">You're all caught up.</p>}

        <div className="alerts-scroll">
          {grouped.TODAY.length > 0 && (
            <>
              <div className="alerts-head muted small">TODAY</div>
              {grouped.TODAY.map((n) => (
                <AlertRow key={n.id} n={n} />
              ))}
            </>
          )}
          {grouped.EARLIER.length > 0 && (
            <>
              <div className="alerts-head muted small">EARLIER</div>
              {grouped.EARLIER.map((n) => (
                <AlertRow key={n.id} n={n} />
              ))}
            </>
          )}
          {shown.length === 0 && items?.length > 0 && (
            <p className="muted small">Nothing in this tab.</p>
          )}
        </div>
      </div>

      {toast && (
        <div className="ach-toast">
          <Icon name="trophy" size={20} />
          <div>
            <div className="muted small">ACHIEVEMENT UNLOCKED</div>
            <div className="pixel">{toast.message}</div>
          </div>
          <span className="num accent">+XP</span>
        </div>
      )}
    </div>
  )
}

function AlertRow({ n }) {
  return (
    <div className={n.read ? 'alert-row' : 'alert-row unread'}>
      <span className="alert-ico" style={{ color: COLOR_FOR[n.type] || 'var(--muted)' }}>
        <Icon name={ICON_FOR[n.type] || 'bell'} size={15} />
      </span>
      <span className="alert-text">{n.message}</span>
      <span className="muted small">{new Date(n.createdAt).toLocaleTimeString().slice(0, 5)}</span>
    </div>
  )
}