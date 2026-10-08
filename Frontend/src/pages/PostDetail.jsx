import { useEffect, useState } from 'react'
import { del, errMsg, get, post as sendPost, put } from '../api'
import { useAuth } from '../auth'
import Card from '../components/Card'
import { ReportProvider } from '../components/PostCard'
import RightPanel from '../components/RightPanel'
import Avatar from '../components/Avatar'
import Icon from '../components/Icon'

const REACTIONS = [
  ['FIRE', '🔥'],
  ['HEART', '❤️'],
  ['LAUGH', '😂'],
  ['CRY', '😢'],
  ['WOW', '😮'],
  ['GG', 'GG'],
]
const slug = (v) => String(v).toLowerCase()

export default function PostDetail({ postId, go }) {
  const { user, refreshMe } = useAuth()
  const [post, setPost] = useState(null)
  const [comments, setComments] = useState(null)
  const [body, setBody] = useState('')
  const [error, setError] = useState(null)

  async function load() {
    try {
      const [p, page] = await Promise.all([
        get(`/posts/${postId}`),
        get(`/posts/${postId}/comments`),
      ])
      setPost(p)
      setComments(page.content)
      setError(null)
    } catch (e) {
      setError(errMsg(e, 'Could not load this post'))
    }
  }

  useEffect(() => {
    // postId yoxdursa (overlay hələ açılmayıb) sorğu göndərmirik -
    // əvvəl hər səhifədə `/posts/null` (400) çıxırdı.
    if (!postId) {
      setPost(null)
      setComments(null)
      return
    }
    load()
  }, [postId])

  async function react(type) {
    await put(`/posts/${postId}/reaction`, { type })
    await load()
    await refreshMe()
  }

  async function send(e) {
    e.preventDefault()
    if (!body.trim()) return
    try {
      await sendPost(`/posts/${postId}/comments`, { body: body.trim() })
      setBody('')
      await load()
      await refreshMe()
    } catch (err) {
      setError(errMsg(err, 'Could not post the comment'))
    }
  }

  async function removeComment(id) {
    await del(`/comments/${id}`)
    load()
  }

  if (!postId) return null

  if (!post) {
    return (
      <div className="feed-grid">
        <div className="section">
          {error && <div className="alert">{error}</div>}
          <div className="card skel-card" />
        </div>
        <RightPanel />
      </div>
    )
  }

  const counts = post.reactionCounts || {}

  return (
    <ReportProvider>
      <div className="feed-grid">
        <div className="section">
        <button className="back-btn" onClick={() => go('feed')}>
          ← Back
        </button>
        {error && <div className="alert">{error}</div>}

        <article className="card post">
          <div className="row between post-head">
            <button
              className="post-author"
              onClick={() => go('user', post.authorId)}
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
            <span className={'chip cat-' + slug(post.category)}>{post.category}</span>
          </div>

          <div className="row between post-meta">
            <span className="muted small">
              {post.commentCount || 0} comments
            </span>
            <button
              className="btn ghost report-btn"
              onClick={() =>
                window.dispatchEvent(
                  new CustomEvent('wc:report-post', { detail: post }),
                )
              }
              title="Report this post"
            >
              <Icon name="shield" size={14} />
              Report
            </button>
          </div>

          <button className="post-title" onClick={() => go('feed')}>
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
            {REACTIONS.map(([type, emoji]) => (
              <button
                key={type}
                className={'react react-' + slug(type)}
                disabled={post.authorId === user.id}
                onClick={() => react(type)}
              >
                <span className="react-emoji">{emoji}</span>
                {counts[type] > 0 && <span className="num">{counts[type]}</span>}
              </button>
            ))}
          </div>
        </article>

        <div className="card">
          <h3>Comments · {comments?.length ?? 0}</h3>

          {comments?.length === 0 && <p className="muted">No comments yet. Say something.</p>}

          {(comments || []).map((c) => (
            <div className="comment" key={c.id}>
              <div className="row between">
                <button
                  className="row link-author"
                  onClick={() => go('user', c.authorId)}
                  title={'View @' + c.authorUsername}
                >
                  <Avatar username={c.authorUsername} avatarUrl={c.authorAvatarUrl} size={24} />
                  <strong>{c.authorUsername}</strong>
                </button>
                <span className="muted small">{new Date(c.createdAt).toLocaleString()}</span>
              </div>
              <div>{c.body}</div>
              {c.reactionCount > 0 && <div className="muted small num">🔥 {c.reactionCount}</div>}
              {(c.authorId === user.id || post.authorId === user.id) && (
                <button className="btn ghost sm" onClick={() => removeComment(c.id)}>
                  Delete
                </button>
              )}
            </div>
          ))}

          <form className="row" onSubmit={send}>
            <input
              value={body}
              placeholder="Write your comment…"
              onChange={(e) => setBody(e.target.value)}
            />
            <button className="btn">Send</button>
          </form>
        </div>
      </div>

        <RightPanel />
      </div>
    </ReportProvider>
  )
}
