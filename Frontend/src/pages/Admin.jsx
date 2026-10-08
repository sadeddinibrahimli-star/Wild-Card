import { useEffect, useState } from 'react'
import { del, errMsg, get, patch, post, put } from '../api'
import Avatar from '../components/Avatar'
import PostCard, { ReportProvider } from '../components/PostCard'
import Icon from '../components/Icon'

/**
 * Doc 4.1 Administration panel.
 *
 *  - platform stats (active users, posts/day, XP distributed, reports pending)
 *  - create / edit / suspend / reactivate accounts
 *  - role assignment (USER, MODERATOR, ADMIN)
 *  - XP value configuration
 */

const slug = (v) => String(v).toLowerCase()
const ROLES = ['USER', 'MODERATOR', 'ADMIN']
const STATUSES = ['ACTIVE', 'RESTRICTED', 'SUSPENDED']

const TABS = [
  ['overview', 'Overview'],
  ['accounts', 'Accounts'],
  ['content', 'Content'],
  ['xp', 'XP values'],
]

export function Admin({ go }) {
  const [tab, setTab] = useState('overview')
  const [stats, setStats] = useState(null)
  const [xp, setXp] = useState(null)
  const [users, setUsers] = useState(null)
  const [allPosts, setAllPosts] = useState([])
  const [error, setError] = useState(null)
  const [note, setNote] = useState(null)
  const [busyPost, setBusyPost] = useState(null)

  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [roleFilter, setRoleFilter] = useState('')

  const [creating, setCreating] = useState(false)
  const [draft, setDraft] = useState({ username: '', email: '', password: '', role: 'USER' })
  const [editing, setEditing] = useState(null)
  const [openPosts, setOpenPosts] = useState(null)
  const [userPosts, setUserPosts] = useState(null)

  async function load() {
    try {
      const q = new URLSearchParams({ size: '50' })
      if (query.trim()) q.set('search', query.trim())
      if (statusFilter) q.set('status', statusFilter)
      if (roleFilter) q.set('role', roleFilter)

      const [s, x, u, posts] = await Promise.all([
        get('/admin/stats'),
        get('/admin/xp-config'),
        get('/admin/users?' + q.toString()).then((p) => p.content),
        get('/posts?size=50').then((p) => p.content),
      ])
      setAllPosts(posts)
      setStats(s)
      setXp(x)
      setUsers(u)
      setError(null)
    } catch (e) {
      setError(errMsg(e, 'Could not load the dashboard'))
    }
  }

  useEffect(() => {
    load()
  }, [])

  function reload() {
    load()
    setEditing(null)
  }

  function flash(msg) {
    setNote(msg)
    setTimeout(() => setNote(null), 2600)
  }

  // ---------------- XP config ----------------

  async function saveXp(action, value) {
    try {
      await put(`/admin/xp-config/${action}`, { value: Number(value) })
      setXp(await get('/admin/xp-config'))
      flash(`Saved ${action}`)
      setError(null)
    } catch (err) {
      setError(errMsg(err, 'Could not save'))
    }
  }

  // ---------------- accounts ----------------

  async function createAccount(e) {
    e.preventDefault()
    try {
      await post('/admin/users', draft)
      setCreating(false)
      setDraft({ username: '', email: '', password: '', role: 'USER' })
      flash('Account created')
      await load()
    } catch (err) {
      setError(errMsg(err, 'Could not create that account'))
    }
  }

  async function saveEdit(e) {
    e.preventDefault()
    if (!editing) return
    try {
      await patch(`/admin/users/${editing.id}`, {
        username: editing.username,
        email: editing.email,
        bio: editing.bio,
        role: editing.role,
        accountStatus: editing.accountStatus,
        password: editing.password || undefined,
      })
      flash('Account updated')
      setEditing(null)
      await load()
    } catch (err) {
      setError(errMsg(err, 'Could not update that account'))
    }
  }

  /** Opens every post of that account - "posts from my other account" are visible here. */
  async function showPosts(u) {
    setOpenPosts(u.id)
    setUserPosts(null)
    try {
      const page = await get(`/users/${u.id}/posts?size=50`)
      setUserPosts(page.content)
    } catch (err) {
      setError(errMsg(err, 'Could not load that users posts'))
      setUserPosts([])
    }
  }

  async function removePost(postId) {
    setBusyPost(postId)
    try {
      await del(`/moderation/posts/${postId}`)
      setUserPosts((list) => list.map((p) => (p.id === postId ? { ...p, deleted: true } : p)))
      flash('Post removed')
    } catch (err) {
      setError(errMsg(err, 'Could not remove that post'))
    } finally {
      setBusyPost(null)
    }
  }

  async function hidePost(postId, hide) {
    try {
      await patch(`/moderation/posts/${postId}/hide?hide=${hide}`)
      flash(hide ? 'Post hidden' : 'Post restored')
    } catch (err) {
      setError(errMsg(err, 'Could not change the post'))
    }
  }

  /** doc 4.1: suspend / reactivate. RESTRICTED only blocks creating posts. */
  async function setStatus(user, status) {
    try {
      await patch(`/admin/users/${user.id}/status?status=${status}`)
      flash(`${user.username} → ${status}`)
      setError(null)
      await load()
    } catch (err) {
      setError(errMsg(err, 'Could not change the status'))
    }
  }

  async function setRole(user, role) {
    try {
      await patch(`/admin/users/${user.id}/role?role=${role}`)
      flash(`${user.username} is now ${role}`)
      setError(null)
      await load()
    } catch (err) {
      setError(errMsg(err, 'Could not change the role'))
    }
  }

  // ---------------- render ----------------

  if (!stats) {
    return (
      <div className="section">
        {error && <div className="alert">{error}</div>}
        <div className="grid-cards">
          <div className="card skel-card" />
          <div className="card skel-card" />
          <div className="card skel-card" />
        </div>
      </div>
    )
  }

  const CARDS = [
    ['Users', stats.totalUsers],
    ['Active (24h)', stats.activeUsers],
    ['Suspended', stats.suspendedUsers],
    ['Restricted', stats.restrictedUsers],
    ['Total posts', stats.totalPosts],
    ['XP distributed', stats.totalXpGranted],
    ['XP this week', stats.xpGrantedThisWeek],
    ['Pending reports', stats.pendingReports],
  ]

  const days = stats.postsPerDay || []
  const peak = Math.max(...days.map((d) => d.count), 1)

  return (
    <div className="section">
      <h2>Admin</h2>
      {error && <div className="alert">{error}</div>}
      {note && <div className="notice">{note}</div>}

      <nav className="tabs small">
        {TABS.map(([key, label]) => (
          <button
            key={key}
            className={tab === key ? 'tab active' : 'tab'}
            onClick={() => setTab(key)}
          >
            {label}
          </button>
        ))}
      </nav>

      {tab === 'overview' && (
        <>
          <p className="muted small">
            Active = son 24 saatda girmiş istifadəçi (hesab statusu deyil).{' '}
            {stats.cachedAt && <>Son hesablama: {new Date(stats.cachedAt).toLocaleTimeString()}</>}
          </p>

          <div className="grid-cards five">
            {CARDS.map(([label, value]) => (
              <div className="card stat-box" key={label}>
                <span className="muted small">{label}</span>
                <strong className="num accent">{Number(value ?? 0).toLocaleString()}</strong>
              </div>
            ))}
          </div>

          <div className="card">
            <h3>Posts per day</h3>
            <div className="bars">
              {days.map((d) => (
                <div className="bar-col" key={d.date}>
                  <span className="num bar-val">{d.count}</span>
                  <div className="bar-track">
                    <div
                      className={'bar-fill' + (d.count === peak && d.count > 0 ? ' peak' : '')}
                      style={{ height: Math.max(3, (d.count / peak) * 100) + '%' }}
                    />
                  </div>
                  <span className="muted small">{d.label}</span>
                </div>
              ))}
            </div>
            <div className="muted small">{stats.postsToday} posts today</div>
          </div>
        </>
      )}

      {tab === 'content' && (
        <ReportProvider>
          {openPosts === null ? (
            <div className="card">
              <h3>Recent posts</h3>
              <p className="muted small">
                Doc 4.1: bütün platformadakı son paylaşımlar. Bir hesabın postlarını
                görmək üçün Accounts tabında "View posts" düyməsinə bas.
              </p>
              {allPosts?.length === 0 && <p className="muted">No posts yet.</p>}
              {allPosts?.map((p) => (
                <div className="row between item" key={p.id}>
                  <span className="row">
                    <Avatar username={p.authorUsername} avatarUrl={p.authorAvatarUrl} size={28} />
                    <span className="friend-id">
                      <strong>@{p.authorUsername}</strong>
                      <span className="muted small">
                        {new Date(p.createdAt).toLocaleString()} · {p.category}
                      </span>
                    </span>
                  </span>
                  <span className={'chip cat-' + slug(p.category)}>{p.title.slice(0, 28)}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="card">
              <div className="row between">
                <h3>Posts</h3>
                <button className="btn ghost" onClick={() => setOpenPosts(null)}>
                  Back to all posts
                </button>
              </div>
              <p className="muted small">
                Doc 4.1: "Archive or remove posts" - aşağıdakı düymələr moderator üçündür.
              </p>

              {userPosts === null && <div className="skel-card" />}
              {userPosts?.length === 0 && <p className="muted">This account has no posts.</p>}

              <div className="admin-posts">
                {userPosts?.map((p) => (
                  <article className={'card post' + (p.deleted ? ' post-removed' : '')} key={p.id}>
                    <div className="row between post-head">
                      <span className="row">
                        <Avatar username={p.authorUsername} avatarUrl={p.authorAvatarUrl} size={30} />
                        <span className="muted small">
                          @{p.authorUsername} · {new Date(p.createdAt).toLocaleString()}
                        </span>
                      </span>
                      <span className={'chip cat-' + slug(p.category)}>{p.category}</span>
                    </div>
                    <strong>{p.title}</strong>
                    <p className="body">{(p.body || '').slice(0, 180)}</p>
                    {p.imageUrl && <img className="post-img" src={p.imageUrl} alt="" />}
                    <div className="row wrap">
                      <button className="btn ghost" onClick={() => hidePost(p.id, true)}>
                        <Icon name="eye" size={14} /> Hide
                      </button>
                      <button className="btn ghost" onClick={() => hidePost(p.id, false)}>
                        <Icon name="check" size={14} /> Restore
                      </button>
                      <button
                        className="btn ghost danger"
                        disabled={busyPost === p.id}
                        onClick={() => removePost(p.id)}
                      >
                        <Icon name="close" size={14} /> Remove
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            </div>
          )}
        </ReportProvider>
      )}

      {tab === 'xp' && xp && (
        <div className="card">
          <h3>XP values</h3>
          <p className="muted small">
            Doc 4.1: "Manage XP-value configuration for each action type".
            Dəyəri dəyişib Enter bas — dərhal qeydə alınır.
          </p>
          {Object.entries(xp).map(([action, value]) => (
            <label className="field xp-row" key={action}>
              <span>{action}</span>
              <input
                type="number"
                defaultValue={value}
                onKeyDown={(e) => e.key === 'Enter' && saveXp(action, e.currentTarget.value)}
              />
            </label>
          ))}
        </div>
      )}

      {tab === 'accounts' && (
        <>
          <div className="card">
            <div className="row between">
              <h3>Accounts</h3>
              <button className="btn" onClick={() => setCreating((c) => !c)}>
                <Icon name="plus" size={14} />
                {creating ? 'Cancel' : 'New account'}
              </button>
            </div>

            {creating && (
              <form className="add-form" onSubmit={createAccount}>
                <input
                  placeholder="Username"
                  value={draft.username}
                  onChange={(e) => setDraft({ ...draft, username: e.target.value })}
                  required
                />
                <input
                  type="email"
                  placeholder="Email"
                  value={draft.email}
                  onChange={(e) => setDraft({ ...draft, email: e.target.value })}
                  required
                />
                <input
                  type="password"
                  placeholder="Password (min 8)"
                  value={draft.password}
                  onChange={(e) => setDraft({ ...draft, password: e.target.value })}
                  required
                />
                <select
                  value={draft.role}
                  onChange={(e) => setDraft({ ...draft, role: e.target.value })}
                >
                  {ROLES.map((r) => (
                    <option key={r}>{r}</option>
                  ))}
                </select>
                <button className="btn">Create</button>
              </form>
            )}

            <div className="row wrap">
              <input
                placeholder="Search username or email"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
              <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                <option value="">Any status</option>
                {STATUSES.map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </select>
              <select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
                <option value="">Any role</option>
                {ROLES.map((r) => (
                  <option key={r}>{r}</option>
                ))}
              </select>
              <button className="btn ghost" onClick={load}>
                Filter
              </button>
            </div>
          </div>

          {users?.length === 0 && (
            <div className="card">
              <p className="muted">Nobody matches that filter.</p>
            </div>
          )}

          {users?.map((u) => (
            <div className="card admin-user" key={u.id}>
              <div className="row between">
                <span className="row">
                  <Avatar username={u.username} avatarUrl={u.avatarUrl} size={36} />
                  <span className="friend-id">
                    <strong>{u.username}</strong>
                    <span className="muted small">
                      #{u.id} · Lv{u.level} · {u.rarity}
                    </span>
                  </span>
                </span>

                <span className="row">
                  <span className={'chip role-' + slug(u.role)}>{u.role}</span>
                  <span className={'chip st-' + slug(u.accountStatus)}>{u.accountStatus}</span>
                </span>
              </div>

              {u.bio && <p className="muted small">{u.bio}</p>}

              {editing?.id === u.id ? (
                <form className="add-form" onSubmit={saveEdit}>
                  <input
                    placeholder="Username"
                    value={editing.username || ''}
                    onChange={(e) => setEditing({ ...editing, username: e.target.value })}
                  />
                  <input
                    type="email"
                    placeholder="Email"
                    value={editing.email || ''}
                    onChange={(e) => setEditing({ ...editing, email: e.target.value })}
                  />
                  <input
                    placeholder="Bio"
                    value={editing.bio || ''}
                    onChange={(e) => setEditing({ ...editing, bio: e.target.value })}
                  />
                  <input
                    type="password"
                    placeholder="New password (optional)"
                    value={editing.password || ''}
                    onChange={(e) => setEditing({ ...editing, password: e.target.value })}
                  />
                  <select
                    value={editing.role}
                    onChange={(e) => setEditing({ ...editing, role: e.target.value })}
                  >
                    {ROLES.map((r) => (
                      <option key={r}>{r}</option>
                    ))}
                  </select>
                  <button className="btn">Save</button>
                  <button type="button" className="btn ghost" onClick={() => setEditing(null)}>
                    Cancel
                  </button>
                </form>
              ) : (
                <div className="row wrap admin-actions">
                  <select
                    value={u.role}
                    onChange={(e) => setRole(u, e.target.value)}
                    title="Change role"
                  >
                    {ROLES.map((r) => (
                      <option key={r}>{r}</option>
                    ))}
                  </select>

                  {u.accountStatus !== 'ACTIVE' && (
                    <button className="btn ghost" onClick={() => setStatus(u, 'ACTIVE')}>
                      Reactivate
                    </button>
                  )}
                  {u.accountStatus !== 'RESTRICTED' && (
                    <button className="btn ghost" onClick={() => setStatus(u, 'RESTRICTED')}>
                      Restrict posts
                    </button>
                  )}
                  {u.accountStatus !== 'SUSPENDED' && (
                    <button className="btn ghost danger" onClick={() => setStatus(u, 'SUSPENDED')}>
                      Suspend
                    </button>
                  )}

                  <button
                    className="btn ghost"
                    onClick={() =>
                      setEditing({
                        id: u.id,
                        username: u.username,
                        email: u.email,
                        bio: u.bio || '',
                        role: u.role,
                        accountStatus: u.accountStatus,
                        password: '',
                      })
                    }
                  >
                    <Icon name="pencil" size={14} />
                    Edit
                  </button>

                  <button className="btn ghost" onClick={() => showPosts(u)}>
                    <Icon name="image" size={14} />
                    View posts
                  </button>

                  {go && (
                    <button className="btn ghost" onClick={() => go('user', u.id)}>
                      View profile
                    </button>
                  )}
                </div>
              )}
            </div>
          ))}
        </>
      )}
    </div>
  )
}