import { useState } from 'react'
import { useAuth } from '../auth.jsx'
import { errMsg, get, post } from '../api'

const SLOGAN = 'Your personality is probably a side quest.'

export default function Login() {
  const { login } = useAuth()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  // "Forgot password" axını: email istə → backend link yaradır →
  // lokalda SMTP olmadığı üçün linki dev-mailbox-dan oxuyub göstəririk.
  const [mode, setMode] = useState('signin') // signin | forgot
  const [resetEmail, setResetEmail] = useState('')
  const [resetBusy, setResetBusy] = useState(false)
  const [resetMsg, setResetMsg] = useState(null)
  const [resetLink, setResetLink] = useState(null)

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(form.email, form.password)
      location.hash = '#feed'
    } catch (err) {
      setError(errMsg(err, 'Wrong email or password'))
    } finally {
      setBusy(false)
    }
  }

  async function sendReset(e) {
    e.preventDefault()
    setResetBusy(true)
    setResetMsg(null)
    setResetLink(null)
    try {
      const res = await post('/auth/forgot-password', { email: resetEmail.trim() })
      setResetMsg(res?.message || 'If that email exists we sent a reset link')

      // Lokal quruluşda e-poçt göndərilmir - link dev-poçt qutusundadır.
      // Bu endpoint autentifikasiya tələb edir (yalnız dev profilində var),
      // ona görə yalnız daxil olmuş istifadəçiyə göstəririk.
      if (localStorage.getItem('wc_token')) {
        try {
          const box = await get('/dev/mailbox')
          const hit = (box || []).find(
            (m) => (m.to || '').toLowerCase() === resetEmail.trim().toLowerCase(),
          )
          if (hit?.body) setResetLink(hit.body)
        } catch {
          /* dev-mailbox yoxdursa önemsizdir */
        }
      }
    } catch (err) {
      setError(errMsg(err, 'Could not send the reset link'))
    } finally {
      setResetBusy(false)
    }
  }

  if (mode === 'forgot') {
    return (
      <div className="login">
        <div className="login-split">
          <form className="login-pane" onSubmit={sendReset}>
            <div className="login-mark pixel">
              <span className="login-diamond">◆</span>{' '}
              <span className="logo-word">
                Wild<span className="logo-c">C</span>ard
              </span>
            </div>
            <p className="login-slogan muted">Reset your password</p>

            <label className="field">
              <span>Email</span>
              <input
                type="email"
                value={resetEmail}
                onChange={(e) => setResetEmail(e.target.value)}
                autoComplete="email"
                required
                autoFocus
              />
            </label>

            {error && <em className="field-err">{error}</em>}
            {resetMsg && <em className="field-err" style={{ color: 'var(--accent)' }}>{resetMsg}</em>}

            {resetLink && (
              <div className="row">
                <a className="btn" href={resetLink}>
                  Open reset link
                </a>
              </div>
            )}

            <button className="btn primary full" disabled={resetBusy}>
              {resetBusy ? 'Sending…' : 'Send reset link'}
            </button>

            <button
              type="button"
              className="btn ghost full"
              onClick={() => {
                setMode('signin')
                setError(null)
                setResetMsg(null)
                setResetLink(null)
              }}
            >
              Back to sign in
            </button>

            <div className="login-hint muted small">
              Link is valid for 30 minutes and can be used once.
              {!resetLink && ' Local setup: the link is printed in the backend console.'}
            </div>
          </form>

          <aside className="login-art" aria-hidden="true">
            <div className="art-card">
              <div className="art-grid" />
              <div className="art-spin">
                <span className="pixel">WC</span>
              </div>
            </div>
            <p className="art-cap muted small">Every card tells a story. Yours starts here.</p>
          </aside>
        </div>
      </div>
    )
  }

  return (
    <div className="login">
      <div className="login-split">
        <form className="login-pane" onSubmit={submit}>
          <div className="login-mark pixel">
            <span className="login-diamond">◆</span>{' '}
            <span className="logo-word">
              Wild<span className="logo-c">C</span>ard
            </span>
          </div>
          <p className="login-slogan muted">{SLOGAN}</p>

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
              autoComplete="current-password"
              required
            />
          </label>

          {error && <em className="field-err">{error}</em>}

          <div className="login-meta">
            <label className="check">
              <input type="checkbox" /> Remember me
            </label>
            <button
              type="button"
              className="link"
              onClick={() => {
                setMode('forgot')
                setError(null)
              }}
            >
              Forgot password
            </button>
          </div>

          <button className="btn primary full" disabled={busy}>
            {busy ? 'Signing in…' : 'Sign in'}
          </button>

          <button
            type="button"
            className="btn ghost full"
            onClick={() => (location.hash = '#register')}
          >
            Create account
          </button>

          <div className="login-hint muted small">admin@wildcard.com / admin123</div>
        </form>

        <aside className="login-art" aria-hidden="true">
          <div className="art-card">
            <div className="art-grid" />
            <div className="art-spin">
              <span className="pixel">WC</span>
            </div>
          </div>
          <p className="art-cap muted small">Every card tells a story. Yours starts here.</p>
        </aside>
      </div>
    </div>
  )
}
