import { useEffect, useState } from 'react'
import { get, put } from '../api'
import { useAuth } from '../auth'

/**
 * Onboarding: after registering we ask what they actually like.
 * Those picks become explicit topic affinities, so the home feed knows
 * what to show before the user has reacted to anything yet.
 */
export default function Welcome() {
  const { user, logout, refreshMe } = useAuth()
  const [topics, setTopics] = useState([])
  const [picked, setPicked] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    get('/topics')
      .then(setTopics)
      .catch(() => setError('Could not load topics'))
  }, [])

  function toggle(id) {
    setPicked((current) =>
      current.includes(id) ? current.filter((x) => x !== id) : [...current, id],
    )
  }

  async function finish() {
    setBusy(true)
    setError(null)
    try {
      await put('/users/me/topics', { topicIds: picked })
      await refreshMe()
      location.hash = '#feed'
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not save your picks')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login">
      <div className="welcome-box">
        <h1>
          Welcome, <span>{user.username}</span>
        </h1>
        <p className="muted">
          Pick the things you are into. Your home feed will start showing these first.
          You can change this any time in your profile.
        </p>

        {error && <div className="alert">{error}</div>}

        <div className="chips">
          {topics.map((t) => (
            <button
              key={t.id}
              className={picked.includes(t.id) ? 'chip on' : 'chip'}
              onClick={() => toggle(t.id)}
            >
              {t.label}
            </button>
          ))}
        </div>

        <div className="row">
          <button className="btn primary" onClick={finish} disabled={busy}>
            {busy ? 'Saving...' : 'Start exploring'}
          </button>
          <button className="btn ghost" onClick={finish} disabled={busy}>
            Skip for now
          </button>
          <button className="btn ghost" onClick={logout}>
            Log out
          </button>
        </div>

        <p className="muted small">Selected: {picked.length}</p>
      </div>
    </div>
  )
}
