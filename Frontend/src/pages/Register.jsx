import { useState } from 'react'
import { useAuth } from '../auth.jsx'
import { errMsg } from '../api'
import Avatar from '../components/Avatar'
import Card from '../components/Card'
import Icon from '../components/Icon'

const STARTER_STATS = [
  ['ani', 'Anime', 'var(--ani)'],
  ['gam', 'Gaming', 'var(--gam)'],
  ['mus', 'Music', 'var(--mus)'],
  ['cha', 'Chaos', 'var(--cha)'],
]

export default function Register() {
  const { register } = useAuth()
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const [stepUpload, setStepUpload] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await register(form.username, form.email, form.password)
      location.hash = '#feed'
    } catch (err) {
      setError(errMsg(err, 'Something went wrong'))
    } finally {
      setBusy(false)
    }
  }

  const name = form.username.trim()

  return (
    <div className="login">
      <div className="login-split">
        <form className="login-pane" onSubmit={submit}>
          <div className="login-mark pixel">
            <span className="login-diamond">◆</span> WildCard
          </div>
          <p className="login-slogan muted">Every card tells a story. Yours starts here.</p>

          <label className="field">
            <span>Username</span>
            <input
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              autoComplete="username"
              placeholder="pick something loud"
              required
            />
          </label>

          <label className="field">
            <span>Email</span>
            <input
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              autoComplete="email"
              required
            />
          </label>

          <label className="field">
            <span>Password</span>
            <input
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              autoComplete="new-password"
              required
            />
          </label>

          {error && <div className="alert">{error}</div>}

          <button className="btn primary full" disabled={busy}>
            {busy ? 'Creating...' : 'Start your adventure'}
          </button>

          <button type="button" className="btn ghost full" onClick={() => (location.hash = '#login')}>
            I already have an account
          </button>
        </form>

        <aside className="login-art">
          <p className="art-label muted small">YOUR STARTING CARD</p>

          <Card
            variant="full"
            rarity="COMMON"
            level={1}
            username={name || 'your name'}
            title="Wild Card Rookie"
            stats={STARTER_STATS.map(([k, l, c]) => [k, l, c, 10])}
          />

          <div className="reg-preview-user">
            <Avatar username={name} size={44} />
            <span className="muted small">
              {name ? 'This avatar is generated from your username' : 'Type a username above'}
            </span>
          </div>

          <button type="button" className="btn ghost" onClick={() => setStepUpload(true)}>
            <Icon name="image" size={15} /> Upload photo instead
          </button>

          {stepUpload && (
            <div className="card reg-upload">
              <p className="muted small">After you create the account you can add a picture here.</p>
              <button type="button" className="btn ghost" onClick={() => setStepUpload(false)}>
                Keep the generated one
              </button>
            </div>
          )}
        </aside>
      </div>
    </div>
  )
}