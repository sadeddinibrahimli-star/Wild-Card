import { useState } from 'react'
import { errMsg, post } from '../api'
import Icon from '../components/Icon'

/**
 * Parol sıfırlama səhifəsi — hash routing ilə açılır:
 *
 *   {FRONTEND_URL}/#reset/{token}
 *
 * Backend forgot-password cavabında bu formatda link göndərir.
 */

export function ResetPassword({ token }) {
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [done, setDone] = useState(false)

  const missing = !token

  function validate() {
    if (password.length < 8) return 'Password must be at least 8 characters'
    if (!/[A-Za-z]/.test(password) || !/[0-9]/.test(password)) {
      return 'Password must contain a letter and a number'
    }
    if (password !== confirm) return 'Passwords do not match'
    return null
  }

  async function submit(e) {
    e.preventDefault()
    const problem = validate()
    if (problem) {
      setError(problem)
      return
    }
    setBusy(true)
    setError(null)
    try {
      await post('/auth/reset-password', { token, newPassword: password })
      setDone(true)
    } catch (err) {
      setError(errMsg(err, 'Could not reset the password'))
    } finally {
      setBusy(false)
    }
  }

  if (done) {
    return (
      <div className="login-art-side">
        <div className="login-box">
          <h1 className="logo pixel">
            <span className="logo-diamond">◆</span>{' '}
            <span className="logo-word">
              Wild<span className="logo-c">C</span>ard
            </span>
          </h1>
          <p className="muted">Password updated.</p>
          <button className="btn" onClick={() => (location.hash = '#feed')}>
            Sign in
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="login-art-side">
      <form className="login-box" onSubmit={submit}>
        <h1 className="logo pixel">
          <span className="logo-diamond">◆</span>{' '}
          <span className="logo-word">
            Wild<span className="logo-c">C</span>ard
          </span>
        </h1>

        {missing ? (
          <>
            <p className="muted">This reset link is incomplete.</p>
            <button className="btn" type="button" onClick={() => (location.hash = '#feed')}>
              Back
            </button>
          </>
        ) : (
          <>
            <p className="muted small">Choose a new password.</p>

            <input
              type="password"
              placeholder="New password (min 8, letter + number)"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoFocus
              required
            />
            <input
              type="password"
              placeholder="Repeat password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              required
            />

            {error && <div className="alert">{error}</div>}

            <button className="btn" disabled={busy}>
              <Icon name="check" size={14} />
              {busy ? 'Saving...' : 'Save password'}
            </button>
          </>
        )}
      </form>
    </div>
  )
}