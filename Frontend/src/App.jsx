import { useEffect, useState } from 'react'
import { AuthProvider, useAuth } from './auth.jsx'
import Sidebar from './components/Sidebar'
import Avatar from './components/Avatar'
import Icon from './components/Icon'
import Login from './pages/Login'
import Register from './pages/Register'
import Welcome from './pages/Welcome'
import Feed from './pages/Feed'
import PostDetail from './pages/PostDetail'
import Profile from './pages/Profile'
import { Chat } from './pages/Chat'
import { Notifications } from './pages/Notifications'
import { Watchlist } from './pages/Watchlist'
import { Friends } from './pages/Friends'
import { ResetPassword } from './pages/ResetPassword'
import { Admin } from './pages/Admin'
import { Moderation } from './pages/Moderation'
import { Achievements } from './pages/Achievements'
import Compatibility from './pages/Compatibility'
import { MusicArc } from './pages/MusicArc'
import { Leaderboard } from './pages/Leaderboard'
import LevelUpModal from './components/LevelUpModal'
import { onRealtimeStatus, realtimeStatus, subscribe } from './realtime'

const OVERLAYS = {
  profile: Profile,
  chat: Chat,
  friends: Friends,
  post: PostDetail,
  watchlist: Watchlist,
  admin: Admin,
  moderation: Moderation,
  achievements: Achievements,
  compatibility: Compatibility,
  musicarc: MusicArc,
  leaderboard: Leaderboard,
}

function useHash() {
  const read = () => {
    const raw = location.hash.slice(1) || 'feed'
    return raw.split('?')[0]
  }
  const [hash, setHash] = useState(read)

  useEffect(() => {
    const onChange = () => setHash(read())
    window.addEventListener('hashchange', onChange)
    return () => window.removeEventListener('hashchange', onChange)
  }, [])

  return hash
}

function param(name) {
  const q = location.hash.split('?')[1] || ''
  return new URLSearchParams(q).get(name)
}

/**
 * Hər route öz "id"-sini oxuyur.
 * Əvvəl hamısı eyni param-ı oxuyurdu: '#post?id=12' açılanda Profile və
 * Compatibility də id=12 götürürdü -> `/users/12` (404) və
 * `/compatibility/12` səhv sorğuları çıxırdı.
 */
const paramOf = (route, name = 'id') => {
  const [r, q] = location.hash.slice(1).split('?')
  return r === route ? new URLSearchParams(q || '').get(name) : null
}

const postId = () => paramOf('post')
const compatId = () => paramOf('compatibility')
const profileId = () => paramOf('profile')
// #reset/<token> - token birbaşa yolda olur (query deyil)
const resetToken = () => {
  const raw = location.hash.slice(1)
  if (raw.startsWith('reset/')) return raw.slice('reset/'.length).trim()
  return param('token')
}

function Topbar({ open, go, query, setQuery, alertsOpen, setAlertsOpen }) {
  const { user } = useAuth()

  return (
    <header className="topbar">
      <button className="logo pixel" onClick={() => go('feed')}>
        <span className="logo-diamond">◆</span> <span className="logo-word">Wild<span className="logo-c">C</span>ard</span>
      </button>

      <div className="topbar-search">
        <Icon name="search" size={15} className="muted" />
        <input
          value={query}
          placeholder="Search people"
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && query.trim()) {
              window.dispatchEvent(new CustomEvent('wc:search', { detail: query.trim() }))
              go('friends')
            }
          }}
        />
      </div>

      <div className="quick">
        <button
          className={'icon-btn' + (alertsOpen ? ' on' : '')}
          onClick={() => setAlertsOpen((v) => !v)}
          title="Alerts"
        >
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" strokeWidth="1.8">
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.7 21a2 2 0 0 1-3.4 0" />
          </svg>
        </button>

        <button
          className={'avatar' + (open === 'profile' ? ' on' : '')}
          onClick={() => go('profile')}
          title="Profile"
        >
          <Avatar user={user} size={26} />
        </button>
      </div>
    </header>
  )
}

function Shell() {
  const { user } = useAuth()
  const hash = useHash()
  const [ready, setReady] = useState(false)
  const [section, setSection] = useState('card')
  const [pid, setPid] = useState(postId)
  const [oid, setOid] = useState(compatId)
  const [profId, setProfId] = useState(profileId)
  const [query, setQuery] = useState('')
  const [alertsOpen, setAlertsOpen] = useState(false)
  const [rt, setRt] = useState(realtimeStatus())

  // canlı bağlantı vəziyyətini izlə
  useEffect(() => onRealtimeStatus(setRt), [])

  // doc 4.3: "Receive real-time notifications" - /user/queue/alerts
  // Bağlantı yoxdursa köhnə polling (5s) işləməyə davam edir.
  useEffect(() => {
    if (rt !== 'active') return undefined
    return subscribe('/user/queue/alerts', () => {
      window.dispatchEvent(new CustomEvent('wc:alerts'))
    })
  }, [rt])

  useEffect(() => {
    const id = requestAnimationFrame(() => setReady(true))
    return () => cancelAnimationFrame(id)
  }, [])

  useEffect(() => {
    const onSection = (e) => setSection(e.detail)
    const onHash = () => {
      setPid(postId())
      setOid(compatId())
      setProfId(profileId())
    }
    window.addEventListener('wc:section', onSection)
    window.addEventListener('hashchange', onHash)
    return () => {
      window.removeEventListener('wc:section', onSection)
      window.removeEventListener('hashchange', onHash)
    }
  }, [])

  function go(route, sub) {
    if (route === 'post') location.hash = '#post?id=' + sub
    else if (route === 'compatibility') location.hash = '#compatibility?id=' + sub
    // "@istifadeci" adına basmaq profili açır (əvvəl '#user'ə gedirdi - belə route yox idi)
    else if (route === 'user') location.hash = '#profile?id=' + sub
    else location.hash = '#' + route

    if (route === 'profile') {
      window.dispatchEvent(new CustomEvent('wc:goto-section', { detail: sub || 'activity' }))
    }
  }

  // parol sıfırlama: #reset/{token} - login olmadan da açıla bilməlidir
  if (hash === 'reset' || hash.startsWith('reset/')) return <ResetPassword token={resetToken()} />

  if (!user) return hash === 'register' ? <Register /> : <Login />
  if (user.onboarded === false) return <Welcome />

  const open = OVERLAYS[hash] ? hash : null
  const Overlay = OVERLAYS[hash]

  return (
    <div className={'stage' + (open ? ' open' : '') + (ready ? ' ready' : '')}>
      <section className="pane pane-feed">
        <div className="shell-row">
          <Sidebar current={open === 'profile' ? 'profile' : hash} section={section} go={go} />

          <div className="shell-main">
            <Topbar open={open} go={go} query={query} setQuery={setQuery} alertsOpen={alertsOpen} setAlertsOpen={setAlertsOpen} />
            <main className="content">
              <Feed go={go} />
            </main>
          </div>
        </div>

        <div className="dim" />
      </section>

      {Object.entries(OVERLAYS).map(([key, Comp]) => (
        <section
          key={key}
          className={'pane pane-overlay' + (open === key ? ' overlay-on' : '')}
          aria-hidden={open !== key}
        >
          <main className="content">
            {key === 'post' ? (
              <PostDetail postId={pid} go={go} />
            ) : key === 'profile' ? (
              <Profile userId={profId} go={go} />
            ) : (
              /* qalan overlay-lar (o cümlədən achievements + compatibility)
                 standart geri düyməsi alır — əvvəl bunlar special-case-da
                 idimə və pəncərədə heç bir çıxış yox idi */
              <>
                <button className="back-btn" onClick={() => go('feed')}>
                  ← Back
                </button>
                {key === 'compatibility' ? (
                  <Compatibility otherId={oid} go={go} />
                ) : key === 'achievements' ? (
                  <Achievements userId={user.id} />
                ) : (
                  <Comp go={go} />
                )}
              </>
            )}
          </main>
        </section>
      ))}

      {alertsOpen && (
        <>
          <div className="drop-dim" onClick={() => setAlertsOpen(false)} />
          <div className="alerts-drop">
            <Notifications />
          </div>
        </>
      )}

      <LevelUpModal user={user} />

      {!open && (
        <button className="chat-fab" onClick={() => go('chat')} title="Chat">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.8">
            <path d="M21 11.5a8.4 8.4 0 0 1-9 8.4 8.4 8.4 0 0 1-3.8-.9L3 21l1.9-5.2A8.4 8.4 0 0 1 12 3.1a8.4 8.4 0 0 1 9 8.4z" />
          </svg>
        </button>
      )}
    </div>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <Shell />
    </AuthProvider>
  )
}
