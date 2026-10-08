import { useEffect, useMemo, useRef, useState } from 'react'
import { errMsg, get, post } from '../api'
import { useAuth } from '../auth'
import { onRealtimeStatus, realtimeStatus, send as rtSend, subscribe } from '../realtime'
import Avatar from '../components/Avatar'
import Icon from '../components/Icon'

const TABS = [
  ['primary', 'Primary'],
  ['general', 'General'],
  ['requests', 'Requests'],
]

/** For a direct chat the other member is the one who is not me. */
function peerOf(conversation, me) {
  const members = conversation?.members || []
  return members.find((m) => m.id !== me.id) || members[0] || null
}

const dayLabel = (d) => {
  const date = new Date(d)
  const today = new Date()
  const yest = new Date(Date.now() - 86400000)
  if (date.toDateString() === today.toDateString()) return 'TODAY'
  if (date.toDateString() === yest.toDateString()) return 'YESTERDAY'
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }).toUpperCase()
}

export function Chat() {
  const { user } = useAuth()
  const [list, setList] = useState(null)
  const [active, setActive] = useState(null)
  const [messages, setMessages] = useState([])
  const [body, setBody] = useState('')
  const [error, setError] = useState(null)
  const [search, setSearch] = useState('')
  const [tab, setTab] = useState('primary')
  const [newTitle, setNewTitle] = useState(null)
  const [partner, setPartner] = useState('')
  const scrollRef = useRef(null)
  // doc 6: "Yazır…" + "Seen · vaxt" - STOMP (qoşulmayıbsa polling qalır)
  const [rt, setRt] = useState(realtimeStatus())
  const [typingPeer, setTypingPeer] = useState(null)
  const typingTimer = useRef(null)

  useEffect(() => onRealtimeStatus(setRt), [])

  async function loadConversations() {
    try {
      setList(await get('/chat/conversations'))
    } catch (e) {
      setError(errMsg(e, 'Could not load chats'))
      setList([])
    }
  }

  async function loadMessages(id) {
    const page = await get(`/chat/${id}/messages`)
    setMessages(page.content)
  }

  useEffect(() => {
    loadConversations()
  }, [])

  // 3 saniyelik polling, real WebSocket yoxdur
  useEffect(() => {
    const id = setInterval(async () => {
      try {
        setList(await get('/chat/conversations'))
        if (active) await loadMessages(active.id)
      } catch {
        /* sessizce */
      }
    }, 3000)
    return () => clearInterval(id)
  }, [active])

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight
  }, [messages])

  async function open(c) {
    setActive(c)
    try {
      await loadMessages(c.id)
    } catch (e) {
      setError(errMsg(e, 'Could not load that conversation'))
    }
  }

  /*
   * Canlı axın (doc 6 / BACKEND.md §5):
   *   /topic/chat/{id}          - yeni mesajlar anında
   *   /topic/chat/{id}/typing   - "Yazır…"
   *   /topic/chat/{id}/seen     - oxundu işarəsi
   * Bağlantı yoxdursa heç nə olmur - 3 saniyəlik polling işləyir.
   */
  useEffect(() => {
    if (!active || rt !== 'active') return undefined
    const id = active.id

    const unsubMessages = subscribe(`/topic/chat/${id}`, () => {
      loadMessages(id).catch(() => {})
      loadConversations().catch(() => {})
    })
    const unsubTyping = subscribe(`/topic/chat/${id}/typing`, (m) => {
      if (!m || m.userId === user.id) return
      if (m.typing) {
        setTypingPeer(m.userId)
        clearTimeout(typingTimer.current)
        typingTimer.current = setTimeout(() => setTypingPeer(null), 4000)
      } else {
        setTypingPeer(null)
      }
    })
    const unsubSeen = subscribe(`/topic/chat/${id}/seen`, () => {
      loadMessages(id).catch(() => {})
    })

    // söhbəti açan tərəf mesajları oxunmuş işarələyir
    rtSend(`/app/chat/${id}/seen`, {})

    return () => {
      unsubMessages()
      unsubTyping()
      unsubSeen()
      clearTimeout(typingTimer.current)
      setTypingPeer(null)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active?.id, rt])

  /** Yazanda "Yazır…" göndərir, 2.5 saniyə fasilədən sonra söndürür. */
  function onBodyChange(e) {
    setBody(e.target.value)
    if (!active || rt !== 'active') return
    rtSend(`/app/chat/${active.id}/typing`, {})
    clearTimeout(typingTimer.current)
    typingTimer.current = setTimeout(() => {
      rtSend(`/app/chat/${active.id}/stop-typing`, {})
    }, 2500)
  }

  async function send(e) {
    e.preventDefault()
    if (!body.trim() || !active) return
    try {
      await post(`/chat/${active.id}/messages`, { body: body.trim() })
      setBody('')
      await loadMessages(active.id)
      await loadConversations()
    } catch (err) {
      setError(errMsg(err, 'Could not send'))
    }
  }

  async function startDirect() {
    if (!partner) return
    try {
      const c = await post(`/chat/direct/${partner}`, {})
      setNewTitle(null)
      setPartner('')
      await loadConversations()
      setActive(c)
    } catch (err) {
      setError(errMsg(err, 'Could not open that chat'))
    }
  }

  async function startGroup() {
    if (!newTitle) return
    try {
      await post('/chat/group', { title: newTitle, memberIds: [] })
      setNewTitle(null)
      await loadConversations()
    } catch (err) {
      setError(errMsg(err, 'Could not create the group'))
    }
  }

  const grouped = useMemo(() => {
    const items = (list || []).filter((c) => {
      if (search) {
        const peer = peerOf(c, user)
        const hay = (c.title || peer?.username || '').toLowerCase()
        if (!hay.includes(search.toLowerCase())) return false
      }
      if (tab === 'general') return c.type === 'GROUP'
      if (tab === 'requests') return false
      return c.type === 'DIRECT'
    })
    return items
  }, [list, search, tab, user])

  const byDay = useMemo(() => {
    const out = []
    for (const m of messages) {
      const label = dayLabel(m.createdAt)
      const last = out[out.length - 1]
      if (last && last.label === label) last.items.push(m)
      else out.push({ label, items: [m] })
    }
    return out
  }, [messages])

  const peer = active ? peerOf(active, user) : null

  return (
    <div className="section">
      {error && <div className="alert">{error}</div>}

      <div className="chat">
        <aside className="card chat-side">
          <div className="row between">
            <h3>Messages</h3>
            <button className="icon-btn" title="New conversation" onClick={() => setNewTitle('')}>
              <Icon name="pencil" size={15} />
            </button>
          </div>

          <input
            className="chat-search"
            placeholder="Search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          <nav className="tabs small">
            {TABS.map(([key, label]) => (
              <button key={key} className={tab === key ? 'tab active' : 'tab'} onClick={() => setTab(key)}>
                {label}
                {key === 'requests' && <span className="muted small"> 0</span>}
              </button>
            ))}
          </nav>

          {newTitle !== null && (
            <div className="card chat-new">
              <input
                placeholder="Group name"
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
              />
              <div className="row">
                <button className="btn" onClick={startGroup}>
                  Create group
                </button>
              </div>
              <input
                placeholder="Direct: user id"
                value={partner}
                onChange={(e) => setPartner(e.target.value)}
              />
              <button className="btn ghost" onClick={startDirect}>
                Open direct chat
              </button>
            </div>
          )}

          <div className="chat-list">
            {list === null && <p className="muted small">Loading...</p>}
            {list?.length === 0 && <p className="muted small">Nothing yet.</p>}
            {tab === 'requests' && list?.length > 0 && (
              <p className="muted small">No pending requests.</p>
            )}
            {grouped.map((c) => {
              const p = peerOf(c, user)
              return (
                <button
                  key={c.id}
                  className={active?.id === c.id ? 'conv active' : 'conv'}
                  onClick={() => open(c)}
                >
                  <span className="conv-av">
                    <Avatar username={p?.username} avatarUrl={p?.avatarUrl} size={30} />
                    <span className="online-dot" />
                  </span>
                  <span className="conv-id">
                    <strong>{c.title || p?.username || 'Direct message'}</strong>
                    <span className="muted small">{(c.lastMessage || 'empty').slice(0, 30)}</span>
                  </span>
                  <span className="conv-tail">
                    <span className="muted small num">
                      {c.lastMessageAt ? new Date(c.lastMessageAt).toLocaleTimeString().slice(0, 5) : ''}
                    </span>
                    {c.unreadCount > 0 && <span className="unread-badge num">{c.unreadCount}</span>}
                  </span>
                </button>
              )
            })}
          </div>
        </aside>

        <div className="card chat-main">
          {!active && <p className="muted">Pick a conversation on the left.</p>}
          {active && (
            <>
              <div className="row between chat-head">
                <span className="row">
                  <span className="conv-av">
                    <Avatar username={peer?.username} avatarUrl={peer?.avatarUrl} size={30} />
                    <span className="online-dot" />
                  </span>
                  <span>
                    <h3>{active.title || peer?.username || 'Direct message'}</h3>
                    <span className="muted small">Active {ago(active.lastMessageAt)}</span>
                  </span>
                </span>
                <span className="row">
                  <Icon name="phone" size={17} className="muted" />
                  <Icon name="video" size={17} className="muted" />
                </span>
              </div>

              <div className="msgs" ref={scrollRef}>
                {byDay.map((group) => (
                  <div key={group.label}>
                    <div className="day-sep">
                      <span className="muted small">{group.label}</span>
                    </div>
                    {group.items.map((m) => (
                      <div key={m.id} className={m.senderId === user.id ? 'msg mine' : 'msg'}>
                        {m.senderId !== user.id && (
                          <div className="row">
                            <Avatar username={m.senderUsername} size={20} />
                            <span className="muted small">{m.senderUsername}</span>
                          </div>
                        )}
                        <div>{m.body}</div>
                        <div className="msg-time muted small">
                          {new Date(m.createdAt).toLocaleTimeString().slice(0, 5)}
                        </div>
                      </div>
                    ))}
                  </div>
                ))}
              </div>

              <form className="row chat-entry" onSubmit={send}>
                <Icon name="smileIcon" size={16} className="muted" />
                <Icon name="paperclip" size={16} className="muted" />
                <input
                  value={body}
                  placeholder="Write a message"
                  onChange={onBodyChange}
                />
                <button className="btn">
                  <Icon name="send" size={15} />
                </button>
              </form>
              <span className="muted small" style={{ minHeight: 16, display: 'block' }}>
                {typingPeer ? `${peer?.username || 'Someone'} is typing…` : ''}
              </span>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

function ago(iso) {
  if (!iso) return 'recently'
  const mins = Math.max(1, Math.round((Date.now() - new Date(iso)) / 60000))
  if (mins < 60) return `${mins} min ago`
  const hours = Math.round(mins / 60)
  if (hours < 24) return `${hours} h ago`
  return `${Math.round(hours / 24)} d ago`
}