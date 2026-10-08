import { useEffect, useState } from 'react'
import Card, { rarityOf } from './Card'

const KEY = 'wc_last_level'

/**
 * Level ups are detected by comparing the level before and after a refresh.
 * The last level we already celebrated is kept in localStorage so the modal
 * never shows twice for the same level.
 */
export default function LevelUpModal({ user }) {
  const [shown, setShown] = useState(null)

  useEffect(() => {
    if (!user) return
    const level = Number(user.level || 0)
    const stored = Number(localStorage.getItem(KEY))
    if (!stored) {
      localStorage.setItem(KEY, String(level))
      return
    }
    if (level > stored) {
      setShown({ from: stored, to: level })
    }
    localStorage.setItem(KEY, String(level))
  }, [user])

  useEffect(() => {
    if (!shown) return
    const onKey = (e) => e.key === 'Escape' && setShown(null)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [shown])

  if (!shown) return null

  return (
    <div className="levelup" onClick={() => setShown(null)}>
      <div className="levelup-card" onClick={(e) => e.stopPropagation()}>
        <span className="muted small">LEVEL UP!</span>

        <div className="pixel levelup-nums">
          Lv.{shown.from} <span className="levelup-arrow">→</span> Lv.{shown.to}
        </div>

        <Card
          variant="full"
          rarity={rarityOf(shown.to)}
          level={shown.to}
          username={user.username}
          avatarUrl={user.avatarUrl}
          title={user.title}
          stats={[
            ['ani', 'Anime', 'var(--ani)', user.ani],
            ['gam', 'Gaming', 'var(--gam)', user.gam],
            ['mus', 'Music', 'var(--mus)', user.mus],
            ['cha', 'Chaos', 'var(--cha)', user.cha],
          ]}
        />

        {user.title && <div className="muted">New title: {user.title}</div>}
        <div className="levelup-xp num">+500 XP</div>

        <button className="btn primary" onClick={() => setShown(null)}>
          Continue
        </button>
      </div>
    </div>
  )
}