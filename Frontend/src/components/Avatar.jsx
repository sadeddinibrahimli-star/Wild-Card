import { useEffect, useState } from 'react'

/**
 * Deterministic pixel avatar, GitHub-identicon style.
 *
 * The username is hashed, the hash picks a colour and which of the 15 left-hand
 * cells are filled, and the three data columns are mirrored to the right. Nothing
 * is stored anywhere, so the same username always renders the same picture.
 */

// colours come from the styles.css :root tokens - no hex values in the JSX
const PALETTE = [
  'var(--card-frame)',
  'var(--cat-anime)',
  'var(--gam)',
  'var(--accent)',
  'var(--rarity-rare)',
  'var(--cat-dnd)',
]
const BACKDROP = 'var(--avatar-backdrop)'

const GRID = 5
const DATA_COLS = 3
const CELL = 4
const STEP = CELL + 1
const SPAN = (GRID - 1) * STEP + CELL // 24: four 1-unit gaps between five 4-unit cells

/** FNV-1a, 32 bit. Stable across browsers and runs. */
function hash32(input) {
  let h = 0x811c9dc5
  for (let i = 0; i < input.length; i++) {
    h ^= input.charCodeAt(i)
    h = Math.imul(h, 0x01000193)
  }
  return h >>> 0
}

/** Exported so tests and other code can reuse the exact same picture. */
export function identicon(username) {
  const seed = (username || '').trim().toLowerCase()
  const h = hash32(seed)

  const color = PALETTE[h % PALETTE.length]
  const cells = []

  for (let row = 0; row < GRID; row++) {
    for (let col = 0; col < GRID; col++) {
      // mirror the three data columns onto the right half
      const src = col < DATA_COLS ? col : GRID - 1 - col
      const bit = row * DATA_COLS + src
      if ((h >>> bit) & 1) cells.push([col, row])
    }
  }

  return { color, cells, seed }
}

export default function Avatar({ user, size = 32, username, avatarUrl, className = '' }) {
  const name = username ?? user?.username ?? ''
  const url = avatarUrl ?? user?.avatarUrl ?? null
  // an upload that no longer exists falls back to the generated picture
  const [broken, setBroken] = useState(false)

  useEffect(() => setBroken(false), [url])

  // --av-size lets CSS take over the size in one place (the sidebar hover state)
  const style = { width: 'var(--av-size, ' + size + 'px)', height: 'var(--av-size, ' + size + 'px)' }

  if (url && !broken) {
    return (
      <span className={'avatar-box ' + className} style={style}>
        <img
          src={url}
          alt=""
          className="avatar-img"
          style={{ width: size, height: size }}
          onError={() => setBroken(true)}
        />
      </span>
    )
  }

  const { color, cells } = identicon(name)

  return (
    <span
      className={'avatar-box avatar-pixel ' + className}
      style={{ ...style, background: BACKDROP }}
      title={name}
    >
      <svg
        viewBox={`0 0 ${SPAN} ${SPAN}`}
        width={size}
        height={size}
        shapeRendering="crispEdges"
        role="img"
        aria-label={name ? name + ' avatar' : 'avatar'}
      >
        {cells.map(([col, row]) => (
          <rect
            key={col + ':' + row}
            x={col * STEP}
            y={row * STEP}
            width={CELL}
            height={CELL}
            fill={color}
          />
        ))}
      </svg>
    </span>
  )
}