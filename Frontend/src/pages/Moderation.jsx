import { useEffect, useState } from 'react'
import { del, errMsg, get, patch } from '../api'
import Icon from '../components/Icon'

const TABS = [
  ['PENDING', 'Pending'],
  ['REVIEWED', 'Reviewed'],
  ['RESOLVED', 'Resolved'],
]
const NEXT = { PENDING: 'REVIEWED', REVIEWED: 'RESOLVED', RESOLVED: null }

const slug = (v) => String(v).toLowerCase()

export function Moderation() {
  const [tab, setTab] = useState('PENDING')
  const [rows, setRows] = useState(null)
  const [error, setError] = useState(null)

  async function load() {
    try {
      const page = await get(`/moderation/reports?status=${tab}`)
      setRows(page.content)
      setError(null)
    } catch (e) {
      setError(errMsg(e, 'Could not load reports'))
      setRows([])
    }
  }

  useEffect(() => {
    load()
  }, [tab])

  const [note, setNote] = useState({})
  const [busy, setBusy] = useState(false)

  /** Reportu növbədən çıxarır (PENDING -> REVIEWED -> RESOLVED). */
  async function advance(row) {
    const to = NEXT[row.status]
    if (!to) return
    setBusy(true)
    try {
      await patch(`/moderation/reports/${row.id}`, { status: to })
      await load()
    } catch (err) {
      setError(errMsg(err, 'Could not update the report'))
    } finally {
      setBusy(false)
    }
  }

  /**
   * Doc 4.2 + istifadəçi tələbi: moderator qərar verir -
   *   "heç nə etmə" | "gizlət (bərpa olunur)" | "sil (soft delete)" | "hesabı blokla"
   * Report heç vaxt özü məzmunu toxunmur.
   */
  async function decide(row, action) {
    setBusy(true)
    setError(null)
    try {
      const resolutionNote = (note[row.id] || '').trim() || `Moderator: ${action}`

      if (action === 'block') {
        await patch(
          `/admin/users/${row.reportedPostAuthorId || row.reportedCommentAuthorId}/status?status=SUSPENDED`,
        )
        await patch(`/moderation/reports/${row.id}`, {
          status: 'RESOLVED',
          resolutionNote,
        })
        await load()
        return
      }

      if (action === 'hide' && row.postId) {
        await patch(`/moderation/posts/${row.postId}/hide?hide=true`)
      }
      if (action === 'remove' && row.postId) {
        await del(`/moderation/posts/${row.postId}`)
      }

      await patch(`/moderation/reports/${row.id}`, {
        status: 'RESOLVED',
        resolutionNote,
        removeContent: action === 'remove',
      })
      await load()
    } catch (err) {
      setError(errMsg(err, 'Could not apply that decision'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="section">
      <h2>Moderation</h2>

      <nav className="tabs small">
        {TABS.map(([key, label]) => (
          <button key={key} className={tab === key ? 'tab active' : 'tab'} onClick={() => setTab(key)}>
            {label}
          </button>
        ))}
      </nav>

      {error && <div className="alert">{error}</div>}
      {rows === null && <div className="card skel-card" />}
      {rows?.length === 0 && (
        <div className="card">
          <p className="muted">No reports. All quiet.</p>
        </div>
      )}

      {rows?.length > 0 && (
        <div className="card table">
          <div className="tr th">
            <span>#</span>
            <span>Type</span>
            <span>Reported content</span>
            <span>Reason</span>
            <span>Status</span>
            <span />
          </div>
          {rows.map((r) => (
            <div className="tr" key={r.id}>
              <span className="num muted">{r.id}</span>
              <span>{r.targetType || (r.postId ? 'Post' : r.commentId ? 'Comment' : '—')}</span>
              <Reported r={r} />
              <span className="reason">{r.reason}</span>
              <span>
                <span className={'chip st-' + slug(r.status)}>{r.status}</span>
              </span>
              <span>
                {NEXT[r.status] && (
                  <button className="btn ghost" disabled={busy} onClick={() => advance(r)}>
                    Mark {NEXT[r.status].toLowerCase()}
                  </button>
                )}
              </span>
            </div>
          ))}

          {/* qərarlar: heç nə / gizlət / sil / hesabı blokla */}
          {rows.map((r) => (
            <div className="card mod-decide" key={'d' + r.id}>
              <div className="row between">
                <strong className="pixel small">#{r.id} decision</strong>
                <span className={'chip st-' + slug(r.status)}>{r.status}</span>
              </div>

              <input
                placeholder="Note (optional) - kept on the report"
                value={note[r.id] || ''}
                onChange={(e) => setNote({ ...note, [r.id]: e.target.value })}
              />

              <div className="row wrap">
                {r.status !== 'RESOLVED' && (
                  <button
                    className="btn ghost"
                    disabled={busy}
                    onClick={() => advance(r)}
                  >
                    Keep · {NEXT[r.status]?.toLowerCase()}
                  </button>
                )}

                {r.postId && !r.reportedPostHidden && r.status !== 'RESOLVED' && (
                  <button
                    className="btn ghost"
                    disabled={busy}
                    onClick={() => decide(r, 'hide')}
                    title="Hide from public feeds - can be restored"
                  >
                    <Icon name="eye" size={14} />
                    Hide
                  </button>
                )}

                {r.postId && !r.reportedPostHidden && r.status !== 'RESOLVED' && (
                  <button
                    className="btn ghost danger"
                    disabled={busy}
                    onClick={() => decide(r, 'remove')}
                    title="Remove the post (soft delete)"
                  >
                    <Icon name="close" size={14} />
                    Remove post
                  </button>
                )}

                {(r.reportedPostAuthorId || r.reportedCommentAuthorId) &&
                  r.status !== 'RESOLVED' && (
                    <button
                      className="btn ghost danger"
                      disabled={busy}
                      onClick={() => decide(r, 'block')}
                      title="Suspend the account - the post stays"
                    >
                      <Icon name="shield" size={14} />
                      Block account
                    </button>
                  )}

                {r.status === 'RESOLVED' && (
                  <span className="muted small">
                    resolved{r.resolutionNote ? ' · ' + r.resolutionNote : ''}
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

/**
 * Doc 4.2: "View and triage reported posts and comments".
 * Moderator baxmadan evvel SIZI GORMELIDIR - sadece id deyil, məzmunun özü.
 */
function Reported({ r }) {
  const [open, setOpen] = useState(false)

  if (r.reportedPostTitle || r.reportedPostBody || r.reportedCommentBody) {
    return (
      <div className="reported">
        <button className="link" onClick={() => setOpen((o) => !o)}>
          {open ? 'hide' : 'view'}
        </button>

        {open ? (
          <div className="reported-box">
            {r.reportedPostTitle && (
              <strong>{r.reportedPostTitle}</strong>
            )}
            <p className="muted small">
              {(r.reportedPostBody || r.reportedCommentBody || '').slice(0, 220)}
            </p>
            {r.reportedPostImageUrl && (
              <img src={r.reportedPostImageUrl} alt="" loading="lazy" />
            )}
            <span className="muted small">
              by @{r.reportedPostAuthorUsername || r.reportedCommentAuthorUsername}
              {r.reportedPostHidden && ' · already hidden'}
            </span>
          </div>
        ) : (
          <span className="muted small">by @{r.reportedPostAuthorUsername || r.reportedCommentAuthorUsername || '—'}</span>
        )}
      </div>
    )
  }

  return <span className="muted small">content gone</span>
}
