import { useEffect, useRef, useState } from 'react'
import { errMsg, get, post, put, upload } from '../api'
import Icon from '../components/Icon'

/** Backend ReactionType enum-u ilə eynidir. */
const RATINGS = [
  { type: 'FIRE', icon: 'flame' },
  { type: 'HEART', icon: 'heart' },
  { type: 'WOW', icon: 'wow' },
  { type: 'GG', icon: 'bolt' },
]

export function MusicArc() {
  const [current, setCurrent] = useState(null)
  const [arcs, setArcs] = useState(null)
  const [error, setError] = useState(null)
  const [changing, setChanging] = useState(false)

  function openForm() {
    setChanging(true)
    setMusicSearch(null)
    setQuery('')
    setError(null)
  }
  const [form, setForm] = useState({ artist: '', trackName: '', note: '', albumArtUrl: '' })
  const [reactions, setReactions] = useState(null)
  const [checkIn, setCheckIn] = useState(null)
  const [musicSearch, setMusicSearch] = useState(null)
  const [query, setQuery] = useState('')
  const [searching, setSearching] = useState(false)
  const [busy, setBusy] = useState(false)
  const artRef = useRef(null)

  async function load() {
    try {
      const arc = await get('/music/current')
      setCurrent(arc)
      setArcs((await get('/music/arcs')).content)
      if (arc?.id) {
        setReactions(await get(`/music/arcs/${arc.id}/reactions`))
      } else {
        setReactions(null)
      }
      setError(null)
    } catch (e) {
      setError(errMsg(e, 'Could not load your music'))
    }
  }

  /** Gündəlik check-in - backend streak-i hesablayır. */
  async function doCheckIn() {
    try {
      setCheckIn(await post('/music/arc/checkin', {}))
    } catch (err) {
      setError(errMsg(err, 'Check-in failed'))
    }
  }

  /** Eyni reaksiyanı təkrar basmaq reaksiyanı silir (backend toggle edir). */
  async function react(type) {
    try {
      setReactions(await post(`/music/arcs/${current.id}/reaction`, { type }))
    } catch (err) {
      setError(errMsg(err, 'Reaction failed'))
    }
  }

  /**
   * Artist adini axtarir. iTunes-a backend gedir - frontend xarici API-ya getmir.
   * Axtarilan sadece artistdir; neticeler butun mahnilardir.
   */
  async function doSearchArtist() {
    const term = query.trim()
    if (!term) return
    setSearching(true)
    try {
      setMusicSearch(await get(`/search/music?q=${encodeURIComponent(term)}`))
      setError(null)
    } catch (err) {
      setError(errMsg(err, 'Search failed'))
    } finally {
      setSearching(false)
    }
  }

  /** Neticede bir mahni sec - forma ve qabiq avtomatik dolur. */
  function pickTrack(track) {
    setForm({
      artist: track.artist || form.artist,
      trackName: track.track || track.trackName,
      note: form.note,
      albumArtUrl: track.albumArtUrl || '',
    })
    setMusicSearch(null)
    setQuery('')
  }

  /** Axtaris uygun gelmediyse istifadeci ozu qabiq yikleyir. */
  async function onPickCover(e) {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    try {
      const stored = await upload(file, 'music')
      setForm((f) => ({ ...f, albumArtUrl: stored.url }))
    } catch (err) {
      setError(errMsg(err, 'Could not upload that cover'))
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function save(e) {
    e.preventDefault()
    setBusy(true)
    try {
      await put('/music/current', form)
      setForm({ artist: '', trackName: '', note: '', albumArtUrl: '' })
      setChanging(false)
      await load()
      // Profile-un sol tərəfdəki möhürü də yenilənsin
      window.dispatchEvent(new CustomEvent('wc:music-arc-changed'))
    } catch (err) {
      setError(errMsg(err, 'Could not save that arc'))
    } finally {
      setBusy(false)
    }
  }

  // consecutive arcs ending today, counted from the history we already have
  const streak = countStreak(arcs || [])

  return (
    <div className="section">
      {error && <div className="alert">{error}</div>}

      {arcs === null && <div className="card skel-card" />}

      {arcs?.length === 0 && !changing && (
        <div className="card">
          <p className="muted">No arc yet. Pick what you're listening to.</p>
          <button className="btn" onClick={openForm}>
            Change arc
          </button>
        </div>
      )}

      {current && (
        <div className="card music-now">
          <div className="music-art">
            {current.albumArtUrl ? (
              <img src={current.albumArtUrl} alt={current.trackName} />
            ) : (
              <span className="pixel">{String(current.artist).slice(0, 2).toUpperCase()}</span>
            )}
          </div>

          <div className="music-info">
            <span className="chip cat-music">CURRENT ARC</span>
            <div className="pixel music-artist">{current.artist}</div>
            <div className="muted">Now playing: {current.trackName}</div>
            {current.note && <p className="body">{current.note}</p>}

            <div className="row between">
              <div className="music-streak">
                <Icon name="loop" size={14} />
                <span className="num">{checkIn?.streakDays ?? streak}</span>
                <span className="muted small">
                  {checkIn?.checkedInToday && checkIn?.streakDays
                    ? 'day listening streak · checked in today'
                    : 'day listening streak'}
                </span>
              </div>
              <button className="btn ghost" onClick={openForm}>
                Change arc
              </button>
            </div>
          </div>
        </div>
      )}

      {current && (
        <>
          <div className="card">
            <div className="row between">
              <h3>Rate my current arc</h3>
              <span className="muted small">{reactions?.total ?? 0} ratings</span>
            </div>
            <div className="row">
              {RATINGS.map((r) => (
                <button
                  key={r.type}
                  className={
                    'react' + (reactions?.myReaction === r.type ? ' react-mine' : '')
                  }
                  title={r.type}
                  onClick={() => react(r.type)}
                >
                  <Icon name={r.icon} size={15} />
                  {reactions?.counts?.[r.type] ? (
                    <span className="num small">{reactions.counts[r.type]}</span>
                  ) : null}
                </button>
              ))}
            </div>
          </div>

          <div className="card">
            <div className="row between">
              <h3>Daily check-in</h3>
              {checkIn && (
                <span className="chip">{checkIn.streakDays} day streak</span>
              )}
            </div>
            <p className="muted small">
              {checkIn?.alreadyCheckedIn
                ? `Already checked in today · ${checkIn.totalCheckIns} total`
                : 'Listening today? Mark it and keep your streak.'}
            </p>
            <button className="btn" onClick={doCheckIn}>
              <Icon name="check" size={14} />
              {checkIn?.alreadyCheckedIn ? 'Checked in' : 'Check in'}
            </button>
          </div>
        </>
      )}

      {changing && (
        <form className="card add-form music-form" onSubmit={save}>
          {/* 1-ci addim: artist adini yaz, axtar (iTunes - backend vasitesi ile) */}
          <div className="field">
            <label className="muted small">Search by artist</label>
            <div className="row">
              <input
                placeholder="Artist name (e.g. Yoasobi, Radiohead)"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
              <button
                className="btn ghost"
                type="button"
                disabled={searching}
                onClick={() => doSearchArtist()}
              >
                <Icon name="search" size={14} />
                {searching ? 'Searching...' : 'Search'}
              </button>
            </div>
            <p className="muted small">
              {musicSearch === null
                ? 'Pick a song from the results, or just type the name below.'
                : musicSearch.length === 0
                  ? 'Nothing found. Type the artist and song yourself below.'
                  : 'Tap a song to fill the form.'}
            </p>
          </div>

          {/* neticeler - musiqi qabigi ile */}
          {musicSearch?.length > 0 && (
            <div className="track-list">
              {musicSearch.map((track, i) => (
                <button
                  key={`${track.track}-${i}`}
                  className="track"
                  type="button"
                  onClick={() => pickTrack(track)}
                >
                  <span className="track-art">
                    {track.albumArtUrl ? (
                      <img src={track.albumArtUrl} alt="" loading="lazy" />
                    ) : (
                      <Icon name="note" size={14} />
                    )}
                  </span>
                  <span className="track-id">
                    <strong>{track.track}</strong>
                    <span className="muted small">{track.artist || 'Unknown artist'}</span>
                  </span>
                  <Icon name="check" size={14} />
                </button>
              ))}
            </div>
          )}

          {/* 2-ci addim: ya secilib, ya da birbaqa el ile yazilir */}
          <div className="field">
            <label className="muted small">
              {musicSearch?.length > 0 ? 'Or fill it in yourself' : 'Artist & song'}
            </label>
            <input
              placeholder="Artist"
              value={form.artist}
              onChange={(e) => setForm({ ...form, artist: e.target.value })}
              required
            />
            <input
              placeholder="Track"
              value={form.trackName}
              onChange={(e) => setForm({ ...form, trackName: e.target.value })}
              required
            />
          </div>

          {/* qabiq: axtarisdan gelir, yoxsa istifadeci yukleyir */}
          <div className="field">
            <label className="muted small">Cover</label>
            {form.albumArtUrl ? (
              <div className="row">
                <span className="track-art">
                  <img src={form.albumArtUrl} alt="" />
                </span>
                <button
                  type="button"
                  className="link"
                  onClick={() => setForm({ ...form, albumArtUrl: '' })}
                >
                  remove cover
                </button>
              </div>
            ) : (
              <div className="row">
                <button type="button" className="btn ghost" onClick={() => artRef.current?.click()}>
                  <Icon name="image" size={14} /> Upload cover
                </button>
                <span className="muted small">Without a cover we show your initials.</span>
              </div>
            )}
            <input
              ref={artRef}
              type="file"
              accept="image/png,image/jpeg,image/webp"
              hidden
              onChange={onPickCover}
            />
          </div>

          <input
            placeholder="Note (optional)"
            value={form.note}
            onChange={(e) => setForm({ ...form, note: e.target.value })}
          />

          <div className="row">
            <button className="btn" disabled={busy}>
              {busy ? 'Saving...' : 'Save arc'}
            </button>
            <button type="button" className="btn ghost" onClick={() => setChanging(false)}>
              Cancel
            </button>
          </div>
        </form>
      )}

      <h3>Past arcs</h3>
      {arcs?.length > 0 && (
        <div className="card">
          {arcs.map((a) => (
            <div className="act" key={a.id}>
              <span className="act-text">
                <strong>{a.artist}</strong>{' '}
                <span className="muted small">{a.trackName}</span>
              </span>
              <span className="muted small">
                {a.startedAt ? new Date(a.startedAt).toLocaleDateString() : ''}
                {a.endedAt ? ' → ' + new Date(a.endedAt).toLocaleDateString() : ' → now'}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

/** How many arcs in a row were started on consecutive days. */
function countStreak(arcs) {
  if (!arcs.length) return 0
  const days = arcs
    .map((a) => (a.startedAt ? new Date(a.startedAt) : null))
    .filter(Boolean)
    .map((d) => d.toDateString())
  const unique = [...new Set(days)].sort((a, b) => new Date(b) - new Date(a))
  if (!unique.length) return 0

  let streak = 1
  for (let i = 1; i < unique.length; i++) {
    const prev = new Date(unique[i - 1])
    const cur = new Date(unique[i])
    const diff = Math.round((prev - cur) / 86400000)
    if (diff === 1) streak++
    else break
  }
  return streak
}