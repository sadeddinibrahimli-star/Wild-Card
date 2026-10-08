import { useEffect, useState } from 'react'
import { errMsg, get } from '../api'

export function Leaderboard() {
  const [rows, setRows] = useState(null)
  const [mine, setMine] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    Promise.all([get('/leaderboard/week?size=50'), get('/leaderboard/me')])
      .then(([page, position]) => {
        setRows(page.content)
        setMine(position)
        setError(null)
      })
      .catch((e) => {
        setError(errMsg(e, 'Could not load the leaderboard'))
        setRows([])
      })
  }, [])

  return (
    <div className="feed">
      <h2>Weekly leaderboard</h2>
      <p className="muted">XP earned in the last 7 days, recalculated by a scheduled job.</p>

      {error && <div className="alert">{error}</div>}

      <div className="card">
        {mine === null && <p className="muted small">Loading your position...</p>}
        {mine && !mine.ranked && (
          <p className="muted small">You have not earned XP this week yet. Get posting.</p>
        )}
        {mine?.ranked && (
          <p className="muted small">
            You are <strong className="num accent">#{mine.rank}</strong> with{' '}
            <strong className="num">{mine.weeklyXp} XP</strong> this week
            <span className="muted"> · {mine.totalXp} XP overall</span>
          </p>
        )}
      </div>

      <div className="card">
        {rows === null && <p className="muted small">Loading...</p>}
        {rows?.length === 0 && <p className="muted">Nobody on the board yet.</p>}
        {rows?.map((r) => (
          <div className="row between item" key={r.userId}>
            <span className="rank num">{r.rank}</span>
            <span>{r.username}</span>
            <span className="muted small">{r.title || ''}</span>
            <strong className="num">{r.weeklyXp} XP</strong>
          </div>
        ))}
      </div>
    </div>
  )
}
