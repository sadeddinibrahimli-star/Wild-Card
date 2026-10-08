import { useEffect, useRef, useState } from 'react'
import { del, errMsg, get, post, put, upload } from '../api'
import { useAuth } from '../auth'
import Card from '../components/Card'
import PostCard, { ReportProvider } from '../components/PostCard'
import Avatar from '../components/Avatar'
import RightPanel from '../components/RightPanel'
import Icon from '../components/Icon'

const CATEGORIES = ['ANIME', 'MUSIC', 'FILM', 'GAMING', 'DND']

const REACTIONS = [
  ['FIRE', '🔥'],
  ['HEART', '❤️'],
  ['LAUGH', '😂'],
  ['CRY', '😢'],
  ['WOW', '😮'],
  ['GG', 'GG'],
]

const slug = (v) => String(v).toLowerCase()

export default function Feed({ go }) {
  const { user, refreshMe } = useAuth()
  const [tab, setTab] = useState('home')
  const [sort, setSort] = useState('latest')
  const [posts, setPosts] = useState(null)
  const [error, setError] = useState(null)
  const [topics, setTopics] = useState([])
  const [openPost, setOpenPost] = useState(null)
  const openReport = (p) => window.dispatchEvent(new CustomEvent('wc:report-post', { detail: p }))

  const [draft, setDraft] = useState({ category: 'ANIME', title: '', body: '', topicIds: [], imageUrl: null })
  const [busy, setBusy] = useState(false)

  async function load() {
    try {
      const page = await get(
        tab === 'home'
          ? `/home/feed?sort=${sort}`
          : '/feed?onlyFollowing=true',
      )
      setPosts(page.content)
      setError(null)
    } catch (e) {
      setError(errMsg(e, 'Could not load the feed'))
    }
  }

  useEffect(() => {
    load()
  }, [tab, sort])

  useEffect(() => {
    get('/topics').then(setTopics)
  }, [])

  async function create(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await post('/posts', { ...draft, imageUrl: draft.imageUrl || null })
      setDraft({ category: 'ANIME', title: '', body: '', topicIds: [], imageUrl: null })
      setOpenPost(null)
      await load()
      await refreshMe()
    } catch (err) {
      setError(errMsg(err, 'Could not publish the post'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <ReportProvider>
      <div className="feed-grid">
        <div className="feed">
        <Composer
          draft={draft}
          setDraft={setDraft}
          topics={topics}
          busy={busy}
          error={error}
          open={openPost}
          setOpen={setOpenPost}
          onSubmit={create}
          user={user}
        />

        <div className="row between feed-tabs">
          <div className="tabs small">
            <button className={tab === 'home' ? 'tab active' : 'tab'} onClick={() => setTab('home')}>
              For you
            </button>
            <button className={tab === 'recency' ? 'tab active' : 'tab'} onClick={() => setTab('recency')}>
              Following
            </button>
          </div>

          {tab === 'home' && (
            <select
              className="sort-select"
              value={sort}
              onChange={(e) => setSort(e.target.value)}
              title="Sort order"
            >
              <option value="latest">Newest first</option>
              <option value="relevance">By relevance</option>
            </select>
          )}
        </div>

        {error && <div className="alert">{error}</div>}

        {posts === null && (
          <div className="feed">
            <div className="card skel-card" />
            <div className="card skel-card" />
          </div>
        )}
        {posts?.length === 0 && (
          <div className="card empty">
            <p className="muted">No posts yet. Follow someone or make the first post.</p>
            <div className="row">
              <button className="btn" onClick={() => setOpenPost('new')}>
                Write the first post
              </button>
              <button className="btn ghost" onClick={() => go('friends')}>
                Find people
              </button>
            </div>
          </div>
        )}

        {posts?.map((p) => (
          <PostCard
            key={p.id}
            post={p}
            me={user}
            go={go}
            onChange={load}
            onReport={openReport}
          />
        ))}
      </div>

        <RightPanel />
      </div>
    </ReportProvider>
  )
}

/* ------------------------------------------------------------------ */

function Composer({ draft, setDraft, topics, busy, error, open, setOpen, onSubmit, user }) {
  const expanded = open === 'new' || draft.title.trim().length > 0
  const fileRef = useRef(null)
  const [upError, setUpError] = useState(null)

  async function onPick(e) {
    const file = e.target.files?.[0]
    if (!file) return
    setUpError(null)
    try {
      const stored = await upload(file, 'post')
      setDraft((d) => ({ ...d, imageUrl: stored.url }))
    } catch (err) {
      setUpError(errMsg(err, 'Could not upload that image'))
    }
    e.target.value = ''
  }

  return (
    <form className="card composer" onSubmit={onSubmit}>
      <div className="row composer-head">
        <Avatar user={user} size={34} />

        {!expanded ? (
          <button type="button" className="composer-peek" onClick={() => setOpen('new')}>
            Make a post come on :)
          </button>
        ) : (
          <input
            placeholder="Title"
            value={draft.title}
            onChange={(e) => setDraft({ ...draft, title: e.target.value })}
            required
            autoFocus
          />
        )}
      </div>

      {expanded && (
        <>
          <textarea
            className="composer-body"
            rows={3}
            placeholder="What is happening in your fandom?"
            value={draft.body}
            onChange={(e) => setDraft({ ...draft, body: e.target.value })}
            required
          />

          <div className="chips">
            {CATEGORIES.map((c) => (
              <button
                type="button"
                key={c}
                className={'chip cat-' + slug(c) + (draft.category === c ? ' on' : '')}
                onClick={() => setDraft({ ...draft, category: c })}
              >
                {c}
              </button>
            ))}
          </div>

          <div className="chips">
            {topics.slice(0, 12).map((t) => (
              <button
                type="button"
                key={t.id}
                className={draft.topicIds.includes(t.id) ? 'chip on' : 'chip'}
                onClick={() =>
                  setDraft({
                    ...draft,
                    topicIds: draft.topicIds.includes(t.id)
                      ? draft.topicIds.filter((x) => x !== t.id)
                      : [...draft.topicIds, t.id],
                  })
                }
              >
                {t.label}
              </button>
            ))}
          </div>

          <input
            ref={fileRef}
            type="file"
            accept="image/png,image/jpeg,image/webp"
            hidden
            onChange={onPick}
          />

          <div className="row">
            <button type="button" className="btn ghost" onClick={() => fileRef.current?.click()}>
              <Icon name="image" size={15} /> Image
            </button>
            {draft.imageUrl && (
              <span className="row">
                <img className="thumb" src={draft.imageUrl} alt="" />
                <button
                  type="button"
                  className="link"
                  onClick={() => setDraft({ ...draft, imageUrl: null })}
                >
                  remove
                </button>
              </span>
            )}
          </div>

          {error && <div className="alert">{error}</div>}

          <div className="row between">
            <span className="muted small num">+20 XP for posting</span>
            <div className="row">
              {open === 'new' && (
                <button type="button" className="btn ghost" onClick={() => setOpen(null)}>
                  Cancel
                </button>
              )}
              <button className="btn" disabled={busy}>
                {busy ? 'Posting...' : 'Post'}
              </button>
            </div>
          </div>
        </>
      )}
    </form>
  )
}

/* ------------------------------------------------------------------ */
