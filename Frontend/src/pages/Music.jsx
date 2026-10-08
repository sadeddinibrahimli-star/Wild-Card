import { useEffect, useState } from 'react'
import { errMsg, get, post, put } from '../api'

/** What the user is listening to right now, plus their listening history. */
export function Music() {
  const [current, setCurrent] = useState(null)
  const [arcs, setArcs] = useState(null)
  const [error, setError] = useState(null)
  const [form, setForm] = useState({ artist: '', trackName: '', note: '' })

  async function load() {
    try {
      setCurrent(await get('/music/current'))
      setArcs((await get('/music/arcs')).content)
    } catch (e) {
      setError(errMsg(e, 'Could not load your music'))
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function listen(e) {
    e.preventDefault()
    try {
      await post('/music/current', form)
      setForm({ artist: '', trackName: '', note: '' })
      await load()
    } catch (err) {
      setError(errMsg(err, 'Could not save'))
    }
  }

  async function stop() {
    await put('/music/current', {})
    await load()
  }

  return (
    <div className="feed">
      {error && <div className="alert">{error}</div>}

      {current ? (
        <div className="card">
          <div className="muted">Listening now</div>
          <h3>{current.trackName}</h3>
          <div>{current.artist}</div>
          {current.note && <p className="body">{current.note}</p>}
          <button className="btn ghost" onClick={stop}>
            Stop
          </button>
        </div>
      ) : (
        <div className="card">
          <p className="muted">Nothing playing right now.</p>
        </div>
      )}

      <form className="card" onSubmit={listen}>
        <div className="row">
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
        <input
          placeholder="Note (optional)"
          value={form.note}
          onChange={(e) => setForm({ ...form, note: e.target.value })}
        />
        <button className="btn">Save</button>
      </form>

      <h3>History</h3>
      {arcs?.length === 0 && <p className="muted">No tracks yet.</p>}
      {arcs?.map((a) => (
        <div className="card row between" key={a.id}>
          <div>
            <strong>{a.trackName}</strong>
            <div className="muted">{a.artist}</div>
          </div>
          <span className="muted small">
            {a.endedAt ? new Date(a.endedAt).toLocaleString() : 'Playing'}
          </span>
        </div>
      ))}
    </div>
  )
}
