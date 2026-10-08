import { useEffect, useMemo, useState } from 'react'
import { errMsg, get } from '../api'
import Icon from '../components/Icon'

const TABS = [
  ['all', 'All'],
  ['unlocked', 'Unlocked'],
  ['progress', 'In progress'],
  ['common', 'Common'],
  ['rare', 'Rare'],
  ['epic', 'Epic'],
  ['legendary', 'Legendary'],
]

const slug = (v) => String(v).toLowerCase()

export function Achievements({ userId }) {
  const [rows, setRows] = useState(null)
  const [error, setError] = useState(null)
  const [tab, setTab] = useState('all')

  useEffect(() => {
    if (!userId) return
    setRows(null)
    setError(null)
    get(`/users/${userId}/achievements`)
      .then(setRows)
      .catch((e) => {
        setError(errMsg(e, 'Could not load achievements'))
        setRows([])
      })
  }, [userId])

  const stats = useMemo(() => {
    if (!rows) return null
    const total = rows.length
    const unlocked = rows.filter((r) => r.unlocked).length
    const real = rows.filter((r) => !r.comingSoon)
    const progressed = real.filter((r) => !r.unlocked)
    const avg = real.length
      ? Math.round(
          real.reduce((sum, r) => sum + Math.min(1, (r.current || 0) / (r.target || 1)), 0) /
            real.length *
            100,
        )
      : 0
    return { total, unlocked, avg }
  }, [rows])

  const shown = (rows || []).filter((r) => {
    if (tab === 'all') return true
    if (tab === 'unlocked') return r.unlocked
    if (tab === 'progress') return !r.comingSoon && !r.unlocked
    return slug(r.rarity) === tab
  })

  return (
    <div className="section">
      <div className="ach-head">
        <h2>Achievements</h2>
        {stats && <span className="muted small num">{stats.unlocked} / {stats.total} unlocked</span>}
      </div>

      {stats && (
        <div className="track xp-track">
          <div className="fill xp-fill" style={{ width: stats.avg + '%' }} />
        </div>
      )}

      <nav className="tabs small">
        {TABS.map(([key, label]) => (
          <button key={key} className={tab === key ? 'tab active' : 'tab'} onClick={() => setTab(key)}>
            {label}
          </button>
        ))}
      </nav>

      {error && <div className="alert">{error}</div>}
      {rows === null && (
        <div className="grid-cards six">
          <div className="card skel-card" />
          <div className="card skel-card" />
          <div className="card skel-card" />
        </div>
      )}

      <div className="grid-cards six">
        {shown.map((a) => (
          <div
            key={a.code}
            className={
              'card ach' +
              (a.comingSoon ? ' ach-soon' : '') +
              (a.unlocked ? ' ach-open' : '')
            }
          >
            <div className="ach-medal">
              <span className="ach-ring" />
              <Icon name={a.icon} size={22} />
            </div>

            <div className="pixel ach-name">{a.name}</div>
            <div className="muted ach-desc">{a.description}</div>

            {a.comingSoon ? (
              <span className="muted small ach-flag">Coming soon</span>
            ) : a.unlocked ? (
              <span className={'chip rarity-' + slug(a.rarity)}>{a.rarity}</span>
            ) : (
              <>
                <div className="track ach-track">
                  <div
                    className="fill"
                    style={{
                      width: Math.min(100, ((a.current || 0) / (a.target || 1)) * 100) + '%',
                    }}
                  />
                </div>
                <span className="muted small num">
                  {a.current} / {a.target}
                </span>
              </>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}