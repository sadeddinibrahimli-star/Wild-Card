import { useEffect, useState } from 'react'
import { del, errMsg, get, patch, post as sendPost, put } from '../api'
import Card from './Card'
import Icon from './Icon'

/**
 * Feed və profil arasında paylaşılan post kartı.
 *
 * Doc 4.3: "Create, edit and delete personal posts" +
 * Doc 4.2: istifadəçi şikayət edə bilər (moderator baxır).
 */

const slug = (v) => String(v).toLowerCase()

const REACTIONS = [
  ['FIRE', '🔥'],
  ['HEART', '❤️'],
  ['LAUGH', '😂'],
  ['CRY', '😢'],
  ['WOW', '😮'],
  ['GG', 'GG'],
]

/** Şikayət səbəbləri - backend heç bir məcburiyyət qoymur, sadece səbəb saxlayır. */
const REASONS = [
  'Spam or scam',
  'Harassment or hate',
  'Nudity or sexual content',
  'Violence or self-harm',
  'False information',
  'Duplicate content',
  'Other',
]

export default function PostCard({
  post,
  me,
  go,
  onChange,
  showAuthor = true,
  showFollow = false,   // yalniz profilde true
  onReport,
}) {
  const counts = post.reactionCounts || {}
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const isMine = me && post.authorId === me.id
  const isFollowing = post.isAuthorFollowed ?? false

  async function run(fn) {
    setBusy(true)
    setError(null)
    try {
      await fn()
      if (onChange) await onChange()
    } catch (err) {
      setError(errMsg(err, 'That did not work'))
    } finally {
      setBusy(false)
    }
  }

  const react = (type) =>
    run(async () => {
      await put(`/posts/${post.id}/reaction`, { type })
    })

  const remove = () =>
    run(async () => {
      await del(`/posts/${post.id}`)
      if (go) go('feed')
    })

  const follow = () =>
    run(async () => {
      if (isFollowing) await del(`/social/follow/${post.authorId}`)
      else await sendPost(`/social/follow/${post.authorId}`)
    })

  return (
    <article className="card post">
      {error && <div className="alert">{error}</div>}

      <div className="row between post-head">
        {showAuthor ? (
          <button
            className="post-author"
            onClick={() => go && go('user', post.authorId)}
            title={'View @' + post.authorUsername}
          >
            <Card
              variant="mini"
              rarity={post.authorRarity || 'COMMON'}
              level={post.authorLevel}
              username={post.authorUsername}
              avatarUrl={post.authorAvatarUrl}
              title={new Date(post.createdAt).toLocaleString()}
            />
          </button>
        ) : (
          <span className="muted small">
            {new Date(post.createdAt).toLocaleString()}
          </span>
        )}
        <span className={'chip cat-' + slug(post.category)}>{post.category}</span>
      </div>

      <button className="post-title" onClick={() => go && go('post', post.id)}>
        {post.title}
      </button>
      <p className="body">{post.body}</p>

      {post.imageUrl && <img className="post-img" src={post.imageUrl} alt="" />}

      {post.topics?.length > 0 && (
        <div className="chips">
          {post.topics.map((t) => (
            <span className="chip" key={t.id}>
              {t.label}
            </span>
          ))}
        </div>
      )}

      <div className="row reactions">
        {!isMine &&
          REACTIONS.map(([type, emoji]) => (
            <button
              key={type}
              className={
                'react react-' + slug(type) +
                (post.myReaction === type ? ' react-mine' : '')
              }
              disabled={busy}
              title={post.myReaction === type ? type + ' (your reaction)' : type}
              onClick={() => react(type)}
            >
              <span className="react-emoji">{emoji}</span>
              {counts[type] > 0 && <span className="num">{counts[type]}</span>}
            </button>
          ))}

        {post.commentCount > 0 && (
          <button className="btn ghost" onClick={() => go && go('post', post.id)}>
            {post.commentCount} {post.commentCount === 1 ? 'comment' : 'comments'}
          </button>
        )}

        {!isMine && showAuthor && showFollow && (
          <button
            className={'btn ' + (isFollowing ? 'ghost' : '')}
            disabled={busy}
            onClick={follow}
          >
            <Icon name="people" size={14} />
            {isFollowing ? 'Following' : 'Follow'}
          </button>
        )}

        {isMine ? (
          <button className="btn ghost" disabled={busy} onClick={remove}>
            <Icon name="close" size={14} />
            Delete
          </button>
        ) : (
          onReport && (
            <button
              className="btn ghost report-btn"
              disabled={busy}
              onClick={() => onReport(post)}
              title="Report this post"
            >
              <Icon name="shield" size={14} />
              Report
            </button>
          )
        )}
      </div>
    </article>
  )
}

/* ------------------------------------------------------------------ */

/**
 * Doc 4.2 axını: şikayət toplanır → moderator baxır → silir və ya saxlayır.
 * Report heç bir məzmunu dəyişmir, sadəcə növbəyə düşür.
 */
export function ReportDialog({ post, onClose }) {
  const [reason, setReason] = useState(REASONS[0])
  const [detail, setDetail] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [done, setDone] = useState(false)

  async function submit() {
    setBusy(true)
    setError(null)
    try {
      const text = detail.trim() ? `${reason}: ${detail.trim()}` : reason
      await sendPost('/reports', { postId: post.id, reason: text })
      setDone(true)
      setTimeout(onClose, 1400)
    } catch (err) {
      setError(errMsg(err, 'Could not send the report'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="crop-dim" role="dialog" aria-label="Report post">
      <div className="crop-modal card">
        <div className="row between">
          <h3>Report post</h3>
          <button className="wl-del" onClick={onClose} title="Close">
            <Icon name="close" size={14} />
          </button>
        </div>

        {done ? (
          <p className="muted">Thanks - a moderator will take a look.</p>
        ) : (
          <>
            <p className="muted small">
              “{post.title}” · by @{post.authorUsername}
            </p>

            <div className="chips">
              {REASONS.map((r) => (
                <button
                  key={r}
                  className={reason === r ? 'chip on' : 'chip'}
                  onClick={() => setReason(r)}
                >
                  {r}
                </button>
              ))}
            </div>

            <input
              placeholder="Extra detail (optional)"
              value={detail}
              onChange={(e) => setDetail(e.target.value)}
            />

            {error && <div className="alert">{error}</div>}

            <div className="row">
              <button className="btn" onClick={submit} disabled={busy}>
                {busy ? 'Sending...' : 'Send report'}
              </button>
              <button className="btn ghost" onClick={onClose}>
                Cancel
              </button>
            </div>

            <p className="muted small">
              Reporting never deletes anything. A moderator decides what happens.
            </p>
          </>
        )}
      </div>
    </div>
  )
}

/**
 * Report düyməsi + dialoq (Feed, profil, post detail üçün).
 * Provider kimi işləyir: children içində render olunur, "Report" düyməsi
 * avtomatik açılır.
 *
 *   <ReportProvider>{...}</ReportProvider>
 */
export function ReportProvider({ children }) {
  const [target, setTarget] = useState(null)

  // səhifədən asılı olmayaraq açılır: her səhifə öz Report düyməsini
  // "wc:report-post" hadisəsi ilə açır, provider isə yalnız dialoqu göstərir.
  useEffect(() => {
    const onReport = (e) => setTarget(e.detail)
    window.addEventListener('wc:report-post', onReport)
    return () => window.removeEventListener('wc:report-post', onReport)
  }, [])

  return (
    <>
      {children}
      {target && <ReportDialog post={target} onClose={() => setTarget(null)} />}
    </>
  )
}