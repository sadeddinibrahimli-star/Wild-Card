import { useEffect, useRef, useState } from 'react'
import { del, errMsg, get, post, put, upload } from '../api'
import { useAuth } from '../auth'
import Card from '../components/Card'
import Avatar from '../components/Avatar'
import Icon from '../components/Icon'
import AvatarCrop from '../components/AvatarCrop'
import { Watchlist } from './Watchlist'
import { MusicArc } from './MusicArc'
import Posts from './Posts'

const STATS = [
  ['ani', 'Anime', 'var(--ani)'],
  ['gam', 'Gaming', 'var(--gam)'],
  ['mus', 'Music', 'var(--mus)'],
  ['cha', 'Chaos', 'var(--cha)'],
]

const TABS = [
  ['activity', 'Activity'],
  ['watchlist', 'Watchlist'],
  ['music', 'Music Arc'],
  ['posts', 'Posts'],
]

const WEEK = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']

const SECTIONS = TABS.map(([key]) => key)
const isSection = (v) => typeof v === 'string' && SECTIONS.includes(v)

/** Turns an XP action into a human readable sentence (doc 4.3 - XP history). */
const XP_LABEL = {
  POST: 'Published a post',
  COMMENT: 'Commented',
  REACTION_RECEIVED: 'Reaction received',
  WATCHLIST_UPDATE: 'Watchlist updated',
  WATCHLIST_COMPLETED: 'Watchlist completed',
  MUSIC_ARC_UPDATE: 'Music arc updated',
  MUSIC_CHECK_IN: 'Daily check-in',
  FOLLOWER_GAINED: 'New follower',
  ACHIEVEMENT_BONUS: 'Achievement bonus',
}

const slug = (v) => String(v).toLowerCase()

export default function Profile({ userId, go }) {
  const { user, logout, refreshMe } = useAuth()
  // userId given -> someone else's profile: read-only, not editable
  const viewId = userId ? Number(userId) : user.id
  const isOwn = viewId === user.id
  const [section, setSection] = useState('activity')
  const [card, setCard] = useState(null)
  const [counts, setCounts] = useState(null)
  const [affinity, setAffinity] = useState([])
  const [allTopics, setAllTopics] = useState([])
  const [weekly, setWeekly] = useState(null)
  const [badges, setBadges] = useState(null)
  const [arc, setArc] = useState(null)
  const [xpLog, setXpLog] = useState(null)
  const [bio, setBio] = useState('')
  const bioRef = useRef(null)
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(false)
  const tilt = useTilt()
  const fileRef = useRef(null)
  const [uploading, setUploading] = useState(false)
  const [isFollowing, setIsFollowing] = useState(false)
  const [cropFile, setCropFile] = useState(null)
  // edit dialog: nick / email / bio / password
  const [editOpen, setEditOpen] = useState(false)
  const [edit, setEdit] = useState({
    username: '',
    email: '',
    bio: '',
    currentPassword: '',
    newPassword: '',
    repeat: '',
  })
  const [editErr, setEditErr] = useState(null)
  const [savingEdit, setSavingEdit] = useState(false)

  // tell the shell which sidebar card should light up
  useEffect(() => {
    window.dispatchEvent(new CustomEvent('wc:section', { detail: 'profile:' + section }))
  }, [section])

  // refresh the seal on the left when MusicArc saves a new arc
  useEffect(() => {
    if (!isOwn) return
    const onArc = () => {
      get(`/users/${viewId}/music-arc`).then(setArc).catch(() => {})
    }
    window.addEventListener('wc:music-arc-changed', onArc)
    return () => window.removeEventListener('wc:music-arc-changed', onArc)
  }, [isOwn, viewId])

  // the sidebar asked us to open a specific section
  useEffect(() => {
    const onGoto = (e) => setSection(isSection(e.detail) ? e.detail : 'activity')
    window.addEventListener('wc:goto-section', onGoto)
    return () => window.removeEventListener('wc:goto-section', onGoto)
  }, [])

  async function load() {
    try {
      const [me, followers, following, topics, wk, ach, musicArc, mine, followState] =
        await Promise.all([
          get(isOwn ? '/users/me' : `/users/${viewId}`),
          get(`/users/${viewId}/followers`),
          get(`/users/${viewId}/following`),
          get('/topics'),
          get('/leaderboard/me'),
          get(`/users/${viewId}/achievements`),
          get(`/users/${viewId}/music-arc`),
          // our custom groups only show on our own profile
          isOwn ? get('/users/me/topics') : Promise.resolve([]),
          isOwn ? Promise.resolve(true) : get(`/social/follow/${viewId}/status`),
        ])
      setIsFollowing(!!followState)
      setCard(me)
      setCounts({ followers: followers.totalElements, following: following.totalElements })
      setAffinity(mine)
      setAllTopics(topics)
      setWeekly(wk)
      setBadges(ach)
      setArc(musicArc)
      setBio(me.bio || '')
      setError(null)
    } catch (e) {
      setError(errMsg(e, 'Could not load your profile'))
    }
  }

  useEffect(() => {
    load()
  }, [user.id, userId])

  /**
   * Doc 4.3: "View own XP history".
   * Backend `GET /users/me/xp-history` - read-only, XP can never be edited.
   */
  async function loadXpHistory(size = 10) {
    if (!isOwn) return
    try {
      setXpLog(await get(`/users/me/xp-history?size=${size}`))
    } catch (e) {
      setError(errMsg(e, 'Could not load your XP history'))
      setXpLog({ content: [], totalElements: 0, last: true, size })
    }
  }

  useEffect(() => {
    if (isOwn && section === 'activity' && xpLog === null) loadXpHistory()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOwn, section])

  /** A file was picked -> the crop dialog opens (nothing is cropped automatically). */
  function pickAvatar(e) {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    setCropFile(file)
  }

  /** Only the picture the user cropped is uploaded. */
  async function applyCrop(file) {
    setUploading(true)
    try {
      const stored = await upload(file, 'avatar')
      await put('/users/me', { avatarUrl: stored.url })
      setCropFile(null)
      await load()
      await refreshMe()
      setError(null)
    } catch (err) {
      setError(errMsg(err, 'Could not upload that picture'))
    } finally {
      setUploading(false)
    }
  }

  /** Go back to the home page (SPA routing - the page is not reloaded). */
  function goHome() {
    location.hash = '#feed'
  }

  function openEdit() {
    setEdit({
      username: card.username || '',
      email: card.email || '',
      bio: card.bio || '',
      currentPassword: '',
      newPassword: '',
      repeat: '',
    })
    setEditErr(null)
    setEditOpen(true)
  }

  /** PUT /users/me - only changed fields are sent; changing the password needs the current one. */
  async function saveProfile(e) {
    e.preventDefault()
    setEditErr(null)
    const username = edit.username.trim()
    const email = edit.email.trim()
    if (username.length < 3) return setEditErr('Username must be at least 3 characters')
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return setEditErr('Enter a valid email address')

    const payload = { username, email, bio: edit.bio }
    if (edit.newPassword) {
      if (!edit.currentPassword) {
        return setEditErr('Current password is required to change your password')
      }
      if (edit.newPassword !== edit.repeat) return setEditErr('New passwords do not match')
      if (
        edit.newPassword.length < 8 ||
        !/[A-Za-z]/.test(edit.newPassword) ||
        !/[0-9]/.test(edit.newPassword)
      ) {
        return setEditErr('New password needs min 8 characters with a letter and a number')
      }
      payload.currentPassword = edit.currentPassword
      payload.newPassword = edit.newPassword
    }

    setSavingEdit(true)
    try {
      await put('/users/me', payload)
      await load()
      await refreshMe()
      setEditOpen(false)
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    } catch (err) {
      setEditErr(errMsg(err, 'Could not save your profile'))
    } finally {
      setSavingEdit(false)
    }
  }

  /** Follow / unfollow on someone else's profile. */
  async function toggleFollow() {
    try {
      if (isFollowing) await del(`/social/follow/${viewId}`)
      else await post(`/social/follow/${viewId}`)
      setIsFollowing(!isFollowing)
    } catch (err) {
      setError(errMsg(err, isFollowing ? 'Could not unfollow' : 'Could not follow'))
    }
  }

  async function saveBio() {
    if (!isOwn) return
    try {
      await put('/users/me', { bio })
      await load()
      await refreshMe()
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    } catch (err) {
      setError(errMsg(err, 'Could not save'))
    }
  }

  async function toggleTopic(id) {
    const current = new Set(affinity.filter((t) => t.explicit).map((t) => t.id))
    if (current.has(id)) current.delete(id)
    else current.add(id)
    try {
      await put('/users/me/topics', { topicIds: [...current] })
      setAffinity(await get('/users/me/topics'))
    } catch (err) {
      setError(errMsg(err, 'Could not save your interests'))
    }
  }

  if (cropFile) {
    return (
      <AvatarCrop
        file={cropFile}
        busy={uploading}
        onCancel={() => setCropFile(null)}
        onDone={applyCrop}
      />
    )
  }

  // while the profile loads show a skeleton - an open profile must not look like a black screen
  if (error && !card) {
    return (
      <div className="section">
        <button className="btn ghost" onClick={goHome}>
          <Icon name="back" size={14} />
          Back to feed
        </button>
        <div className="alert">{error}</div>
      </div>
    )
  }
  if (!card) {
    return (
      <div className="section">
        <div className="card skel-card" />
        <div className="card skel-card" />
      </div>
    )
  }

  const max = Math.max(...STATS.map(([k]) => card[k]), 1)
  const picked = new Set(affinity.filter((t) => t.explicit).map((t) => t.id))
  const discovered = affinity.filter((t) => !t.explicit)
  const openBadges = (badges || []).filter((b) => b.unlocked)
  const hasArc = !!arc?.trackName
  const weekPeak = Math.max(weekly?.weeklyXp ?? 0, 1)

  return (
    <div className="feed">
      {error && <div className="alert">{error}</div>}

      <div className="row between prof-head">
        <button className="btn ghost" onClick={goHome}>
          <Icon name="back" size={14} />
          Back to feed
        </button>
        <span className="muted small">
          {isOwn ? `@${card.username}` : `@${card.username}'s profile`}
        </span>
      </div>

      <div className="prof-grid">
        {{/* ---------------- left: the card ---------------- */}}
        <aside className="prof-left">
          <div className="prof-tilt" style={tilt.style} ref={tilt.ref}>
            <Card
              variant="full"
              art
              flippable
              rarity={card.rarity}
              level={card.level}
              username={card.username}
              avatarUrl={card.avatarUrl}
              title={card.title}
              stats={STATS.map(([key, label, color]) => [key, label, color, card[key]])}
            >
              <p className="prof-flavor">{flavorFor(card)}</p>
            </Card>
          </div>

          <div className="card">
            <div className="row between">
              <h3>Info</h3>
              {saved && <span className="muted small">Saved</span>}
            </div>
            <div className="prof-info">
              <span className="muted small">Handle</span>
              <span>@{card.username}</span>
              {isOwn && (
                <>
                  <span className="muted small">Email</span>
                  <span>{card.email || '—'}</span>
                </>
              )}
              <span className="muted small">Followers</span>
              <span className="num">{counts?.followers ?? 0}</span>
              <span className="muted small">Following</span>
              <span className="num">{counts?.following ?? 0}</span>
            </div>
            {isOwn ? (
              <>
                <input
                  ref={fileRef}
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  hidden
                  onChange={pickAvatar}
                />
                <div className="row">
                  <button className="btn" onClick={openEdit}>
                    Edit profile
                  </button>
                  <button className="btn ghost" disabled={uploading} onClick={() => fileRef.current?.click()}>
                    <Icon name="image" size={14} />
                    Change photo
                  </button>
                </div>

                {{/* doc 4.3 leaderboard + links to the doc 4.1/4.2 admin pages */}}
                <div className="row">
                  <button
                    className="btn ghost"
                    onClick={() => (location.hash = '#leaderboard')}
                    title="Weekly leaderboard"
                  >
                    Leaderboard
                  </button>
                  {user?.role === 'ADMIN' && (
                    <button className="btn ghost" onClick={() => (location.hash = '#admin')}>
                      Admin
                    </button>
                  )}
                  {(user?.role === 'ADMIN' || user?.role === 'MODERATOR') && (
                    <button className="btn ghost" onClick={() => (location.hash = '#moderation')}>
                      Moderation
                    </button>
                  )}
                </div>
              </>
            ) : (
              <div className="row">
                <button className={'btn ' + (isFollowing ? 'ghost' : '')} onClick={toggleFollow}>
                  <Icon name="people" size={14} />
                  {isFollowing ? 'Following' : 'Follow'}
                </button>
                {go && (
                  <button className="btn ghost" onClick={() => go('compatibility', card.id)}>
                    Match
                  </button>
                )}
              </div>
            )}
          </div>
        </aside>

        {{/* ---------------- right: tabs + content ---------------- */}}
        <div className="prof-right">
          <nav className="tabs small">
            {TABS.map(([key, label]) => (
              <button
                key={key}
                className={section === key ? 'tab active' : 'tab'}
                onClick={() => setSection(key)}
              >
                {label}
              </button>
            ))}
            {isOwn && (
              <button className="tab ghost" onClick={logout}>
                Log out
              </button>
            )}
          </nav>

          {section === 'activity' && (
            <>
              <div className="card">
                <div className="row between">
                  <h3>XP this week</h3>
                  <span className="num accent">{weekly?.weeklyXp ?? 0} XP</span>
                </div>
                <div className="bars">
                  {WEEK.map((d, i) => {
                    const mine = i === 3 ? weekly?.weeklyXp ?? 0 : 0
                    return (
                      <div className="bar-col" key={d}>
                        <span className="num bar-val">{mine || ''}</span>
                        <div className="bar-track">
                          <div
                            className={'bar-fill' + (mine > 0 ? ' peak' : '')}
                            style={{ height: Math.max(3, (mine / weekPeak) * 100) + '%' }}
                          />
                        </div>
                        <span className="muted small">{d}</span>
                      </div>
                    )
                  })}
                </div>
              </div>

              {{/* own XP history (doc 4.3) */}}
              {isOwn && (
                <div className="card">
                  <div className="row between">
                    <h3>XP history</h3>
                    <span className="muted small num">
                      {xpLog ? `${xpLog.totalElements} entries` : '...'}
                    </span>
                  </div>

                  {xpLog === null && <p className="muted small">Loading...</p>}
                  {xpLog?.content?.length === 0 && (
                    <p className="muted small">No XP yet. Post something or add a title.</p>
                  )}

                  {(xpLog?.content || []).map((entry) => (
                    <div className="act" key={entry.id}>
                      <span className="act-dot" />
                      <span className="act-text">
                        {XP_LABEL[entry.action] || entry.action}
                        {' '}
                        <span className="muted small">{entry.category}</span>
                        <span className="num accent"> +{entry.amount}</span>
                      </span>
                      <span className="muted small">
                        {entry.createdAt ? new Date(entry.createdAt).toLocaleDateString() : ''}
                      </span>
                    </div>
                  ))}

                  {xpLog && !xpLog.last && (
                    <button
                      className="btn ghost"
                      onClick={() => loadXpHistory((xpLog.content?.length || 10) + 10)}
                    >
                      Show more
                    </button>
                  )}
                </div>
              )}

              <div className="card">
                <h3>Badges</h3>
                {openBadges.length === 0 && <p className="muted small">No badges yet.</p>}
                <div className="badge-row">
                  {openBadges.slice(0, 4).map((b) => (
                    <div className={'badge badge-' + slug(b.rarity)} key={b.code} title={b.description}>
                      <Icon name={b.icon} size={18} />
                      <span className="pixel">{b.name}</span>
                    </div>
                  ))}
                </div>
                <button className="btn ghost" onClick={() => (location.hash = '#achievements')}>
                  See all
                </button>
              </div>

              <div className="card">
                <div className="row between">
                  <h3>Music arc</h3>
                  {hasArc && <span className="chip cat-music">NOW PLAYING</span>}
                </div>
                {hasArc ? (
                  <div className="row">
                    <div className="music-art">
                      {arc.albumArtUrl ? (
                        <img src={arc.albumArtUrl} alt={arc.trackName} loading="lazy" />
                      ) : (
                        <span className="pixel">{String(arc.artist).slice(0, 2).toUpperCase()}</span>
                      )}
                    </div>
                    <div>
                      <div className="pixel">{arc.artist}</div>
                      <div className="muted">{arc.trackName}</div>
                      <div className="row small">
                        <span className={'chip' + (arc.checkedInToday ? ' st-completed' : '')}>
                          {arc.checkedInToday ? 'checked in today' : `${arc.streakDays} day streak`}
                        </span>
                        {arc.reactionCount > 0 && (
                          <span className="muted small num">{arc.reactionCount}</span>
                        )}
                      </div>
                    </div>
                  </div>
                ) : (
                  <p className="muted small">No music arc set yet.</p>
                )}
              </div>

              <div className="card">
                <div className="row between">
                  <h3>Bio</h3>
                  {saved && <span className="muted small">Saved</span>}
                </div>
                {isOwn ? (
                  <>
                    <textarea ref={bioRef} rows={3} value={bio} onChange={(e) => setBio(e.target.value)} />
                    <button className="btn" onClick={saveBio}>
                      Save
                    </button>
                  </>
                ) : (
                  <p className="body">{bio || 'Nothing here yet.'}</p>
                )}
              </div>

              {isOwn && (
                <div className="card">
                  <h3>Interests</h3>
                  <p className="muted small">
                    Anything you pick here shows up first in your home feed.
                  </p>
                  <div className="chips">
                    {allTopics.map((t) => (
                      <button
                        key={t.id}
                        className={picked.has(t.id) ? 'chip on' : 'chip'}
                        onClick={() => toggleTopic(t.id)}
                      >
                        {t.label}
                      </button>
                    ))}
                  </div>
                  {discovered.length > 0 && (
                    <p className="muted small">
                      Also showing up for you: {discovered.map((t) => t.label).join(', ')}
                    </p>
                  )}
                </div>
              )}
            </>
          )}

          {section === 'watchlist' && (isOwn ? <Watchlist /> : <p className="muted small">Private.</p>)}
          {section === 'music' && (isOwn ? <MusicArc /> : <PublicArc arc={arc} />)}
          {section === 'posts' && <Posts userId={viewId} />}
        </div>
      </div>

      {{/* ------- Edit profile: nick / email / bio / password ------- */}}
      {editOpen && (
        <div className="crop-dim" role="dialog" aria-label="Edit profile">
          <form className="crop-modal card" onSubmit={saveProfile}>
            <div className="row between">
              <h3>Edit profile</h3>
              <button
                type="button"
                className="wl-del"
                onClick={() => setEditOpen(false)}
                title="Close"
              >
                <Icon name="close" size={14} />
              </button>
            </div>

            <label className="muted small field-label" htmlFor="ep-username">
              Username
            </label>
            <input
              id="ep-username"
              value={edit.username}
              maxLength={50}
              required
              onChange={(e) => setEdit({ ...edit, username: e.target.value })}
            />

            <label className="muted small field-label" htmlFor="ep-email">
              Email
            </label>
            <input
              id="ep-email"
              type="email"
              value={edit.email}
              required
              onChange={(e) => setEdit({ ...edit, email: e.target.value })}
            />

            <label className="muted small field-label" htmlFor="ep-bio">
              Bio
            </label>
            <textarea
              id="ep-bio"
              rows={3}
              maxLength={500}
              value={edit.bio}
              onChange={(e) => setEdit({ ...edit, bio: e.target.value })}
            />

            <p className="muted small field-label">
              Change password — leave blank to keep the current one.
            </p>
            <input
              type="password"
              placeholder="Current password"
              autoComplete="current-password"
              value={edit.currentPassword}
              onChange={(e) => setEdit({ ...edit, currentPassword: e.target.value })}
            />
            <input
              type="password"
              placeholder="New password (min 8, letter + number)"
              autoComplete="new-password"
              value={edit.newPassword}
              onChange={(e) => setEdit({ ...edit, newPassword: e.target.value })}
            />
            <input
              type="password"
              placeholder="Repeat new password"
              autoComplete="new-password"
              value={edit.repeat}
              onChange={(e) => setEdit({ ...edit, repeat: e.target.value })}
            />

            {editErr && <div className="alert">{editErr}</div>}

            <div className="row">
              <button className="btn" disabled={savingEdit}>
                {savingEdit ? 'Saving…' : 'Save changes'}
              </button>
              <button type="button" className="btn ghost" onClick={() => setEditOpen(false)}>
                Cancel
              </button>
            </div>
            <p className="muted small">
              Username and email update right away; password change needs your current password.
            </p>
          </form>
        </div>
      )}
    </div>
  )
}

function flavorFor(card) {
  const top = STATS.map(([k, l]) => [l, card[k]]).sort((a, b) => b[1] - a[1])[0]
  const flavour = {
    Anime: 'Lives for the next episode, whatever it costs.',
    Gaming: 'Will lose a ranked match and blame the ping.',
    Music: 'Has the same four songs on repeat since 2019.',
    Chaos: 'Cannot be categorised and would not want to be.',
  }
  return flavour[top[0]]
}

/** Subtle 3D lean while the pointer moves over the card. */
function useTilt() {
  const ref = useRef(null)
  const [style, setStyle] = useState({})

  useEffect(() => {
    const node = ref.current
    if (!node) return
    const onMove = (e) => {
      const r = node.getBoundingClientRect()
      const x = (e.clientX - r.left) / r.width - 0.5
      const y = (e.clientY - r.top) / r.height - 0.5
      setStyle({ transform: `perspective(900px) rotateY(${x * 5}deg) rotateX(${-y * 4}deg)` })
    }
    const onLeave = () => setStyle({})
    node.addEventListener('mousemove', onMove)
    node.addEventListener('mouseleave', onLeave)
    return () => {
      node.removeEventListener('mousemove', onMove)
      node.removeEventListener('mouseleave', onLeave)
    }
  }, [])

  return { style, ref }
}

/** Another user's music arc - read-only. */
function PublicArc({ arc }) {
  if (!arc?.trackName) return <p className="muted small">No music arc set yet.</p>
  return (
    <div className="card row">
      <div className="music-art">
        {arc.albumArtUrl ? (
          <img src={arc.albumArtUrl} alt={arc.trackName} loading="lazy" />
        ) : (
          <span className="pixel">{String(arc.artist).slice(0, 2).toUpperCase()}</span>
        )}
      </div>
      <div>
        <div className="pixel">{arc.artist}</div>
        <div className="muted">{arc.trackName}</div>
        <span className={'chip' + (arc.checkedInToday ? ' st-completed' : '')}>
          {arc.checkedInToday ? 'checked in today' : `${arc.streakDays} day streak`}
        </span>
      </div>
    </div>
  )
}
