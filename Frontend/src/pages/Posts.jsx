import { useEffect, useState } from 'react'
import { errMsg, get } from '../api'
import PostCard, { ReportProvider } from '../components/PostCard'
import { useAuth } from '../auth'

/**
 * Bir istifadəçinin postları - profilin "Posts" tab-ında.
 * Feed ilə eyni kart komponentini paylaşır (bax, sil, report).
 */
export default function Posts({ userId, go }) {
  const open = (p) => window.dispatchEvent(new CustomEvent('wc:report-post', { detail: p }))

  const { user } = useAuth()
  const [rows, setRows] = useState(null)
  const [error, setError] = useState(null)

  async function load() {
    try {
      const page = await get(`/users/${userId}/posts`)
      setRows(page.content)
      setError(null)
    } catch (e) {
      setError(errMsg(e, 'Could not load posts'))
      setRows([])
    }
  }

  useEffect(() => {
    load()
  }, [userId])

  if (error && rows === null) return <div className="alert">{error}</div>
  if (rows === null) return <div className="card skel-card" />

  if (rows.length === 0) {
    return (
      <ReportProvider>
        <div className="card">
          <p className="muted">No posts yet.</p>
          {userId === user.id && (
            <button className="btn" onClick={() => go && go('feed')}>
              Write your first post
            </button>
          )}
        </div>
      </ReportProvider>
    )
  }

  return (
    <ReportProvider>
      <div className="section">
        {rows.map((p) => (
          <PostCard key={p.id} post={p} me={user} go={go} onChange={load} showFollow onReport={open} />
        ))}
      </div>
    </ReportProvider>
  )
}