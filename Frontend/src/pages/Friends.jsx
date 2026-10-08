import { useEffect, useState } from 'react'
import { del, errMsg, get, post } from '../api'
import { useAuth } from '../auth'
import Avatar from '../components/Avatar'
import Icon from '../components/Icon'
import { compatibility } from './Compatibility'

const TABS = [
  ['discover', 'Discover'],
  ['followers', 'Followers'],
  ['following', 'Following'],
]

export function Friends({ go }) {
  const { user } = useAuth()
  const [tab, setTab] = useState('discover')
  const [rows, setRows] = useState(null)
  const [error, setError] = useState(null)
  const [search, setSearch] = useState('')

  async function load() {
    setRows(null)
    try {
      if (tab === 'discover') {
        const page = search
          ? await get('/users?size=24&search=' + encodeURIComponent(search))
          : await get('/users/discover?size=24')
        setRows(page.content)
      } else {
        const path = tab === 'followers' ? 'followers' : 'following'
        const page = await get(`/users/${user.id}/${path}`)
        setRows(page.content)
      }
      setError(null)
    } catch (e) {
      setError(errMsg(e, 'Could not load this list'))
      setRows([])
    }
  }

  useEffect(() => {
    load()
  }, [tab, search])

  // the topbar search box sends us here with a term
  useEffect(() => {
    const onSearch = (e) => {
      setTab('discover')
      setSearch(e.detail)
    }
    window.addEventListener('wc:search', onSearch)
    return () => window.removeEventListener('wc:search', onSearch)
  }, [])

  async function toggleFollow(target) {
    const was = !!target.isFollowing
    try {
      if (was) await del(`/social/follow/${target.id}`)
      else await post(`/social/follow/${target.id}`)
      setRows((list) => list.map((r) => (r.id === target.id ? { ...r, isFollowing: !was } : r)))
      setError(null)
    } catch (err) {
      setError(errMsg(err, was ? 'Could not unfollow' : 'Could not follow'))
    }
  }

  return (
    <div className="section">
      <nav className="tabs small">
        {TABS.map(([key, label]) => (
          <button key={key} className={tab === key ? 'tab active' : 'tab'} onClick={() => setTab(key)}>
            {label}
          </button>
        ))}
        {tab === 'discover' && (
          <input
            className="tab-search"
            placeholder="Search people"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        )}
      </nav>

      {error && <div className="alert">{error}</div>}
      {rows === null && <p className="muted">Loading...</p>}
      {rows?.length === 0 && (
        <div className="card">
          <p className="muted">Nobody here yet.</p>
        </div>
      )}

      <div className="grid-cards">
        {rows?.map((u) => (
          <div className="card friend" key={u.id}>
            <button
              className="row link-author"
              onClick={() => go && go('user', u.id)}
              title={'View @' + u.username}
            >
              <Avatar username={u.username} avatarUrl={u.avatarUrl} size={44} />
              <div className="friend-id">
                <strong>{u.username}</strong>
                <div className="muted small">{u.title}</div>
              </div>
            </button>

            <Match me={user} other={u} />

            {u.id !== user.id && (
              <button
                className={'btn ' + (u.isFollowing ? 'ghost' : '')}
                onClick={() => toggleFollow(u)}
              >
                <Icon name="people" size={14} />
                {u.isFollowing ? 'Following' : 'Follow'}
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}


/** Compatibility from 4 stats, same formula: 100 - |difference|. */
function Match({ me, other }) {
  const hasStats = ['ani', 'gam', 'mus', 'cha'].some((k) => other[k] != null)
  if (!hasStats) return null

  const { score } = compatibility(me, other)
  if (score === null) return null

  return (
    <div className="friend-match">
      <div className="track">
        <div className="fill" style={{ width: score + '%' }} />
      </div>
      <span className="muted small num">{score}% compatible</span>
    </div>
  )
}
