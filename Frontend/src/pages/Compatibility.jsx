import { useEffect, useState } from 'react'
import { errMsg, get, post } from '../api'
import { useAuth } from '../auth'
import Avatar from '../components/Avatar'
import Card from '../components/Card'

const PICK = [
  'Your friendship would probably be a terrible idea.',
  'You would finish each other’s sentences and each other’s bosses.',
  'Chaos recognises chaos. This one is going to be loud.',
  'You two would show up three hours late and still have fun.',
]

/**
 * Client side compatibility, using the same formula as
 * GET /users/{id}/compatibility/{otherId}: 100 - |difference| per stat, averaged.
 */
export function compatibility(a, b) {
  const KEYS = ['ani', 'gam', 'mus', 'cha']
  const pairs = KEYS.map((k) => ({
    key: k,
    mine: a?.[k] ?? 0,
    theirs: b?.[k] ?? 0,
    score: Math.max(0, 100 - Math.abs((a?.[k] ?? 0) - (b?.[k] ?? 0))),
  }))
  const score = Math.round(pairs.reduce((s, p) => s + p.score, 0) / KEYS.length)
  return { score, pairs }
}

export default function Compatibility({ otherId, go }) {
  const { user } = useAuth()
  const [other, setOther] = useState(null)
  const [mine, setMine] = useState(null)
  const [remote, setRemote] = useState(null)
  const [error, setError] = useState(null)
  const [following, setFollowing] = useState(false)

  useEffect(() => {
    let alive = true
    // wait for a real id, otherwise the route would ask for /users/null
    if (!otherId) return

    setOther(null)
    setRemote(null)
    setError(null)

    Promise.all([get('/users/me'), get(`/users/${otherId}`)])
      .then(([me, them]) => {
        if (!alive) return
        setMine(me)
        setOther(them)
        setFollowing(!!them.following)
        setError(null)
      })
      .catch((e) => alive && setError(errMsg(e, 'Could not load that profile')))

    // the backend copy is the source of truth; fall back to the local formula
    get(`/users/${user.id}/compatibility/${otherId}`)
      .then((r) => alive && setRemote(r))
      .catch(() => {})

    return () => {
      alive = false
    }
  }, [otherId, user.id])

  async function follow() {
    if (following) return
    try {
      await post(`/social/follow/${otherId}`)
      setFollowing(true)
    } catch (err) {
      setError(errMsg(err, 'Could not follow'))
    }
  }

  if (error) return <div className="alert">{error}</div>
  if (!mine || !other) {
    return (
      <div className="section">
        <div className="card skel-card" />
      </div>
    )
  }

  const local = compatibility(mine, other)
  const result = remote || local
  const pairs = remote ? remote.stats.map((s) => ({ ...s })) : local.pairs
  const verdict = PICK[result.score % PICK.length]

  const STAT_LABEL = { ani: 'Anime', gam: 'Gaming', mus: 'Music', cha: 'Chaos' }
  const STAT_VAR = { ani: 'var(--ani)', gam: 'var(--gam)', mus: 'var(--mus)', cha: 'var(--cha)' }

  return (
    <div className="section">
      <div className="cmp">
        <div className="cmp-side">
          <Card variant="full" rarity={mine.rarity} level={mine.level} username={mine.username} title={mine.title} avatarUrl={mine.avatarUrl} />
        </div>

        <div className="cmp-mid">
          <span className="muted small cmp-label">COMPATIBILITY</span>
          <div className="cmp-score num">{result.score}%</div>

          <div className="cmp-bars">
            {pairs.map((p) => {
              const key = slug(p.key)
              return (
                <div className="cmp-bar" key={p.key}>
                  <span className="muted small">
                    {p.label || STAT_LABEL[key]}
                  </span>
                  <div className="track">
                    <div className="fill" style={{ width: p.score + '%', background: STAT_VAR[key] }} />
                  </div>
                  <span className="muted small num">{p.score}%</span>
                </div>
              )
            })}
          </div>

          <p className="cmp-verdict">{verdict}</p>

          {other.id !== user.id &&
            (following ? (
              <span className="chip">Following</span>
            ) : (
              <button className="btn" onClick={follow}>
                Follow {other.username}
              </button>
            ))}
        </div>

        <div className="cmp-side">
          <Card variant="full" rarity={other.rarity} level={other.level} username={other.username} title={other.title} avatarUrl={other.avatarUrl} />
        </div>
      </div>
    </div>
  )
}

function slug(v) {
  return String(v).toLowerCase()
}