import { useState } from 'react'
import { useAuth } from '../auth.jsx'
import Avatar from './Avatar'

/**
 * Left rail, two states.
 *
 * Narrow (58px): the icon column only -- the avatar plus four squares. The card
 * stack is not rendered at all, so nothing can leak out of the collapsed rail.
 *
 * Hover: the rail widens to 190px and the stack appears as overlapping card
 * strips. Exactly one card is open (266px) at a time: the one under the pointer,
 * or the active route's card when the pointer is elsewhere.
 */
const PROFILE = { key: 'profile', label: 'PROFILE', route: 'profile' }
const ITEMS = [
  PROFILE,
  { key: 'home', label: 'HOME', icon: 'home', route: 'feed' },
  { key: 'watchlist', label: 'WATCHLIST', icon: 'watchlist', route: 'watchlist' },
  { key: 'music', label: 'MUSIC', icon: 'music', route: 'musicarc' },
  { key: 'friends', label: 'FRIENDS', icon: 'friends', route: 'friends' },
]

// the four cards, top to bottom. The profile slot above them is the card back.
const NAV = ITEMS.slice(1)

/* Drawn on a 48 grid: the same glyph is reused at 20px in the rail, 16px in the
   card corner and 92px in the open card, so it has to scale cleanly. */
const ICONS = {
  home: <path d="M6 23 24 8l18 15M11 20v20h26V20M20 40V28h8v12" />,
  // one glyph: screen, play triangle and stand, never stacked on top of each other
  watchlist: (
    <>
      <rect x="6" y="9" width="36" height="26" rx="4" />
      <path d="M20 16v12l11-6zM16 42h16" />
    </>
  ),
  music: (
    <>
      <path d="M18 34V10l18-4v24" />
      <circle cx="13" cy="34" r="5" />
      <circle cx="31" cy="30" r="5" />
    </>
  ),
  friends: (
    <>
      <circle cx="20" cy="17" r="7" />
      <path d="M6 40c0-8 6-13 14-13s14 5 14 13M38 15v12M32 21h12" />
    </>
  ),
}

function Glyph({ name, className }) {
  return (
    <svg
      className={className}
      viewBox="0 0 48 48"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {ICONS[name]}
    </svg>
  )
}

export default function Sidebar({ current, section, go }) {
  const { user } = useAuth()
  const [railOpen, setRailOpen] = useState(false)
  const [hoverKey, setHoverKey] = useState(null)

  function open(item) {
    if (item.route === 'feed') return go('feed')
    if (item.route === 'friends') return go('friends')
    if (item.route === 'profile') return go('profile')
    if (item.route === 'watchlist') return go('watchlist')
    if (item.route === 'musicarc') return go('musicarc')
    go('profile', item.route.split(':')[1])
  }

  const activeKey =
    current === 'friends'
      ? 'friends'
      : current === 'watchlist'
        ? 'watchlist'
        : current === 'musicarc'
          ? 'music'
          : current === 'profile'
            ? ITEMS.find((i) => i.route === 'profile:' + section)?.key ?? 'profile'
            : 'home'

  // the accordion: pointer wins, otherwise fall back to the active route
  const openKey = hoverKey ?? activeKey

  function leave() {
    setRailOpen(false)
    setHoverKey(null)
  }

  return (
    <nav
      className={'sidebar' + (railOpen ? ' op' : '')}
      aria-label="Main"
      onMouseEnter={() => setRailOpen(true)}
      onMouseLeave={leave}
      onFocus={() => setRailOpen(true)}
      onBlur={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget)) leave()
      }}
    >
      <div className="side-panel">
      {!railOpen ? (
        /* ---- narrow: icons only, no cards ---- */
        <div className="rail">
          <button
            className={'side-item' + (activeKey === 'profile' ? ' is-active' : '')}
            onClick={() => open(PROFILE)}
            title={PROFILE.label}
            aria-label={PROFILE.label}
            aria-current={activeKey === 'profile' ? 'page' : undefined}
          >
            <span className="side-brand">
              <Avatar user={user} size={30} className="side-avatar" />
            </span>
          </button>

          {NAV.map((item) => (
            <button
              key={item.key}
              className={'side-item' + (activeKey === item.key ? ' is-active' : '')}
              onClick={() => open(item)}
              title={item.label}
              aria-label={item.label}
              aria-current={activeKey === item.key ? 'page' : undefined}
            >
              <Glyph name={item.icon} className="side-ico" />
            </button>
          ))}
        </div>
      ) : (
        /* ---- hover: the card stack ---- */
        <div className="sw">
          <button className="bk" onClick={() => open(PROFILE)} title={PROFILE.label}>
            <span className="dm" />
            <Avatar user={user} size={36} className="pf" />
          </button>

          {NAV.map((item) => (
            <button
              key={item.key}
              className={'nc' + (openKey === item.key ? ' on' : '')}
              onMouseEnter={() => setHoverKey(item.key)}
              onClick={() => open(item)}
              title={item.label}
              aria-current={activeKey === item.key ? 'page' : undefined}
            >
              <span className="tt">{item.label}</span>
              <Glyph name={item.icon} className="ix" />
              <Glyph name={item.icon} className="ix r" />
              <Glyph name={item.icon} className="ic" />
            </button>
          ))}
        </div>
      )}
      </div>

      <span className="sr-only">Signed in as {user.username}</span>
    </nav>
  )
}