import Avatar from './Avatar'
import { useState } from 'react'

const RARITY_LABEL = {
  COMMON: 'Common',
  RARE: 'Rare',
  EPIC: 'Epic',
  LEGENDARY: 'Legendary',
}

export function rarityOf(level) {
  if (level >= 50) return 'LEGENDARY'
  if (level >= 25) return 'EPIC'
  if (level >= 10) return 'RARE'
  return 'COMMON'
}

/** The patterned back you see before a card is flipped. */
export function CardBack({ size = 'full' }) {
  return (
    <div className={'card-back card-back-' + size}>
      <div className="card-back-grid" />
      <div className="card-back-mark pixel">WC</div>
    </div>
  )
}

/**
 * Player card. `variant` controls the size, `rarity` the frame colour and glow.
 * With `flippable` the whole card turns around on click to show the back.
 */
export default function Card({
  variant = 'full',
  rarity = 'COMMON',
  level,
  username,
  title,
  stats,
  avatarUrl,
  flippable = false,
  art = false,
  children,
  className = '',
}) {
  const [flipped, setFlipped] = useState(false)
  const key = String(rarity || 'COMMON').toLowerCase()
  const isFancy = rarity === 'RARE' || rarity === 'EPIC' || rarity === 'LEGENDARY'

  const card = (
    <>
      <div className="card-face-inner">
        {art && variant === 'full' && (
          <div className="pcard-art">
            <div className="pcard-art-grid" />
            <div className="pcard-art-name pixel">{username}</div>
          </div>
        )}
        {variant === 'compact' ? (
          <>
            <span className="card-place">{title}</span>
            <span className="card-name">{username}</span>
            <span className={'chip rarity-' + key}>{RARITY_LABEL[rarity] || rarity}</span>
            {level != null && <span className="num card-xp">Lv {level}</span>}
          </>
        ) : (
          <>
            {variant === 'mini' && (
              <Avatar username={username} avatarUrl={avatarUrl} size={26} />
            )}
            {variant === 'full' && (
              <Avatar username={username} avatarUrl={avatarUrl} size={46} className="card-face-avatar" />
            )}
            {level != null && <div className="num card-level">{level}</div>}
            <div className="card-id">
              {username && <div className="card-name">{username}</div>}
              {title && <div className="muted small">{title}</div>}
            </div>
            {variant === 'full' && stats && (
              <div className="card-stats">
                {stats.map(([k, label, color, value]) => (
                  <div className="card-stat" key={k}>
                    <span className="num" style={{ color }}>
                      {value ?? 0}
                    </span>
                    <span className="muted small">{label}</span>
                  </div>
                ))}
              </div>
            )}
            {children}
          </>
        )}
      </div>
    </>
  )

  return (
    <div
      className={[
        'pcard',
        `pcard-${variant}`,
        `rarity-${key}`,
        isFancy ? 'pcard-glow' : '',
        rarity === 'LEGENDARY' ? 'pcard-shimmer' : '',
        flippable ? 'pcard-flip' : '',
        flipped ? 'is-flipped' : '',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
      onClick={flippable ? () => setFlipped((f) => !f) : undefined}
      role={flippable ? 'button' : undefined}
      tabIndex={flippable ? 0 : undefined}
      onKeyDown={
        flippable
          ? (e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                setFlipped((f) => !f)
              }
            }
          : undefined
      }
    >
      <div className="pcard-inner">
        <div className="pcard-front">{card}</div>
        {flippable && (
          <div className="pcard-backwrap">
            <CardBack size={variant} />
          </div>
        )}
      </div>
    </div>
  )
}
