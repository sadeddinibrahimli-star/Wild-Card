import { useEffect, useState } from 'react'
import { del, errMsg, get, post, put } from '../api'
import Icon from '../components/Icon'

const STATUS = ['WATCHING', 'COMPLETED', 'PLAN_TO_WATCH', 'DROPPED']
const TABS = [
  ['ALL', 'All'],
  ['WATCHING', 'Watching'],
  ['COMPLETED', 'Completed'],
  ['PLAN_TO_WATCH', 'Plan to watch'],
  ['DROPPED', 'Dropped'],
]
const KINDS = ['ANIME', 'FILM']
const slug = (v) => String(v).toLowerCase()

export function Watchlist() {
  const [items, setItems] = useState(null)
  const [error, setError] = useState(null)
  const [tab, setTab] = useState('ALL')
  const [adding, setAdding] = useState(false)
  const [form, setForm] = useState({ title: '', kind: 'ANIME', status: 'WATCHING', rating: '', posterUrl: '', externalId: '' })
  const [results, setResults] = useState(null)
  const [stats, setStats] = useState(null)
  const [query, setQuery] = useState('')
  const [filmEnabled, setFilmEnabled] = useState(true)

  async function load() {
    try {
      const page = await get('/watchlist?size=100')
      setItems(page.content)
      get('/watchlist/stats').then(setStats).catch(() => setStats(null))
      setError(null)
    } catch (e) {
      setError(errMsg(e, 'Could not load your list'))
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function add(e) {
    e.preventDefault()
    try {
      await post('/watchlist', { ...form, rating: form.rating ? Number(form.rating) : null })
      setForm({ title: '', kind: 'ANIME', status: 'WATCHING', rating: '', posterUrl: '', externalId: '' })
      setAdding(false)
      load()
    } catch (err) {
      setError(errMsg(err, 'Could not add that title'))
    }
  }

  /** The backend calls AniList/TMDB - the frontend never calls an external API. */
  useEffect(() => {
    get('/search/titles/status')
      .then((s) => setFilmEnabled(!!s.filmSearchEnabled))
      .catch(() => setFilmEnabled(false))
  }, [])

  async function searchTitles(e) {
    e.preventDefault()
    try {
      setResults(await get(`/search/titles?q=${encodeURIComponent(query)}&type=${form.kind}`))
    } catch (err) {
      setError(errMsg(err, 'Search failed'))
    }
  }

  function pick(hit) {
    setForm({
      ...form,
      title: hit.title,
      posterUrl: hit.posterUrl || '',
      externalId: hit.externalId || '',
    })
    setResults(null)
    setQuery('')
  }

  async function change(item, status) {
    // never fail silently - the user must see that the button worked
    try {
      await put(`/watchlist/${item.id}`, { status })
      await load()
    } catch (err) {
      setError(errMsg(err, 'Could not change the status'))
      await load()
    }
  }

  /** Change the rating after adding (PUT /watchlist/{id} partial update). */
  async function rate(item, value) {
    if (value === '') return
    try {
      await put(`/watchlist/${item.id}`, { rating: Number(value) })
      await load()
    } catch (err) {
      setError(errMsg(err, 'Could not change the rating'))
      await load()
    }
  }

  async function remove(id) {
    try {
      await del(`/watchlist/${id}`)
      await load()
    } catch (err) {
      setError(errMsg(err, 'Could not remove that title'))
    }
  }

  const shown = (items || []).filter((i) => tab === 'ALL' || i.status === tab)

  return (
    <div className="section">
      <nav className="tabs small">
        {TABS.map(([key, label]) => (
          <button key={key} className={tab === key ? 'tab active' : 'tab'} onClick={() => setTab(key)}>
            {label}
          </button>
        ))}
        <button className="btn right" onClick={() => setAdding((a) => !a)}>
          {adding ? 'Close' : '+ Add title'}
        </button>
      </nav>

      {error && <div className="alert">{error}</div>}

      {stats && stats.total > 0 && (
        <p className="muted small num">
          {stats.total} titles · {stats.watching} watching · {stats.completed} completed
          {stats.planToWatch ? ` · ${stats.planToWatch} planned` : ''}
          {stats.dropped ? ` · ${stats.dropped} dropped` : ''}
          {stats.averageRating ? ` · ★ ${stats.averageRating} avg` : ''}
        </p>
      )}

      {adding && (
        <form className="card add-form" onSubmit={add}>
          <div className="row">
            <input
              placeholder={form.kind === 'FILM' ? 'Search films (TMDB)...' : 'Search anime (AniList)...'}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            <button className="btn ghost" type="button" onClick={searchTitles}>
              <Icon name="search" size={14} />
              Search
            </button>
          </div>

          {results?.length === 0 && (
            <p className="muted small">
              {form.kind === 'FILM' && !filmEnabled
                ? 'Film search needs TMDB_API_KEY. You can still add titles manually.'
                : 'No matches. Add it manually below.'}
            </p>
          )}

          {results?.length > 0 && (
            <div className="row wrap">
              {results.map((hit, i) => (
                <button key={hit.externalId || i} className="chip" type="button" onClick={() => pick(hit)}>
                  {hit.title}
                  {hit.year ? ` (${hit.year})` : ''}
                </button>
              ))}
            </div>
          )}

          <input
            placeholder="Title"
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            required
          />
          <select value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value })}>
            {KINDS.map((k) => (
              <option key={k}>{k}</option>
            ))}
          </select>
          <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
            {STATUS.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
          <input
            type="number"
            min="1"
            max="10"
            placeholder="Rating"
            style={{ width: 88 }}
            value={form.rating}
            onChange={(e) => setForm({ ...form, rating: e.target.value })}
          />
          <button className="btn">Add</button>
        </form>
      )}

      {items === null && <p className="muted">Loading...</p>}
      {items?.length === 0 && (
        <div className="card">
          <p className="muted">Your list is empty.</p>
        </div>
      )}
      {items?.length > 0 && shown.length === 0 && (
        <div className="card">
          <p className="muted">Nothing in this tab.</p>
        </div>
      )}

      <div className="wl-grid">
        {shown.map((item) => (
          <article className="card wl-card" key={item.id}>
            <div className={'wl-poster wl-' + slug(item.kind)}>
              {item.posterUrl ? (
                <img src={item.posterUrl} alt={item.title} loading="lazy" />
              ) : (
                <span className="pixel">{String(item.title).slice(0, 2).toUpperCase()}</span>
              )}
              <span className="wl-poster-tag">{item.kind}</span>
            </div>

            <div className="wl-body">
              <strong className="wl-title">{item.title}</strong>

              <div className="wl-meta">
                <span className={'chip st-' + slug(item.status)}>
                  {item.status.replace('_', ' ')}
                </span>
                <span className="wl-rate num">{item.rating != null ? `★ ${item.rating}` : '—'}</span>
              </div>

              <div className="wl-actions">
                <select
                  className="wl-rating"
                  value={item.rating ?? ''}
                  onChange={(e) => rate(item, e.target.value)}
                  aria-label={'Rating for ' + item.title}
                >
                  {item.rating == null && <option value="">★ —</option>}
                  {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((n) => (
                    <option key={n} value={n}>
                      ★ {n}
                    </option>
                  ))}
                </select>
                <select
                  value={item.status}
                  onChange={(e) => change(item, e.target.value)}
                  aria-label={'Status for ' + item.title}
                >
                  {STATUS.map((s) => (
                    <option key={s}>{s}</option>
                  ))}
                </select>
                <button className="wl-del" onClick={() => remove(item.id)} title="Remove">
                  <Icon name="close" size={14} />
                </button>
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  )
}
