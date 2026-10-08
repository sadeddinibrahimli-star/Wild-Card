import { useEffect, useState } from 'react'
import { get } from '../api'
import Card, { rarityOf } from '../components/Card'

/**
 * 250px right column. Everything here comes from endpoints that already exist:
 * /users/me, /leaderboard/me and /notifications.
 */
export default function RightPanel() {
  const [me, setMe] = useState(null)
  const [weekly, setWeekly] = useState(null)
  const [activity, setActivity] = useState(null)
  const [top, setTop] = useState(null)

  useEffect(() => {
    get('/users/me').then(setMe).catch(() => {})
    get('/leaderboard/me').then(setWeekly).catch(() => {})
    get('/leaderboard/top?limit=5').then(setTop).catch(() => setTop([]))
    get('/notifications?size=6')
      .then((p) => setActivity(p.content))
      .catch(() => {})

    // WebSocket-dən ani bildiriş gələndə yenilə (fallback: səhifə açılanda)
    const onAlert = () => {
      get('/notifications?size=6')
        .then((p) => setActivity(p.content))
        .catch(() => {})
      get('/leaderboard/me').then(setWeekly).catch(() => {})
    }
    window.addEventListener('wc:alerts', onAlert)
    return () => window.removeEventListener('wc:alerts', onAlert)
  }, [])

  if (!me) return <aside className="right-panel" />

  const nextLevelXp = (me.level + 1) * 500
  const pct = Math.min(100, Math.round((me.totalXp / nextLevelXp) * 100))

  return (
    <aside className="right-panel">
      <Card
        variant="small"
        rarity={me.rarity || rarityOf(me.level)}
        level={me.level}
        username={me.username}
        title={me.title}
      />

      <div className="card">
        <div className="row between">
          <span className="muted small">XP</span>
          <span className="muted small num">
            {me.totalXp.toLocaleString()} / {nextLevelXp.toLocaleString()}
          </span>
        </div>
        <div className="track xp-track">
          <div className="fill xp-fill" style={{ width: pct + '%' }} />
        </div>
        {weekly?.weeklyXp > 0 && (
          <div className="muted small">+{weekly.weeklyXp} XP this week</div>
        )}
      </div>

      <div className="card">
        <div className="row between">
          <h3>Weekly XP</h3>
          {weekly?.rank ? <span className="muted small num">You #{weekly.rank}</span> : null}
        </div>
        {top === null && <p className="muted small">Loading...</p>}
        {top?.length === 0 && <p className="muted small">Nobody on the board yet.</p>}
        {top?.map((r) => (
          <div className="act" key={r.userId}>
            <span className="rank num">{r.rank}</span>
            <span className="act-text">{r.username}</span>
            <span className="num accent">{r.weeklyXp}</span>
          </div>
        ))}
        <button className="btn ghost" onClick={() => (location.hash = '#leaderboard')}>
          See full leaderboard
        </button>
      </div>

      <div className="card">
        <h3>Recent activity</h3>
        {activity === null && <p className="muted small">Loading...</p>}
        {activity?.length === 0 && <p className="muted small">Nothing yet.</p>}
        {activity?.map((n) => (
          <div className="act" key={n.id}>
            <span className={'act-dot nt-' + String(n.type).toLowerCase()} />
            <span className="act-text">{describe(n)}</span>
          </div>
        ))}
      </div>

    </aside>
  )
}

function describe(n) {
  switch (n.type) {
    case 'COMMENT':
      return n.message || 'New comment'
    case 'REACTION':
      return n.message || 'New reaction'
    case 'FOLLOW':
      return n.message || 'New follower'
    case 'REPORT_RESOLVED':
      return 'A report was resolved'
    default:
      return n.message || 'System update'
  }
}
