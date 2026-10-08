/** Single stroke icon set. Every glyph is drawn with lines, never emoji. */
const PATHS = {
  // navigation + chrome
  search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></>,
  home: <path d="M3.5 10.5 12 3.5l8.5 7M6 9.5V20h12V9.5M10 20v-5h4v5" />,
  screen: <><rect x="3" y="4.5" width="18" height="15" rx="2.5" /><path d="M10.2 9.4v5.2l4.6-2.6z" /></>,
  note: <path d="M9 18V5.5l10-2v12M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0Zm10-2a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />,
  people: <><circle cx="9" cy="8" r="3.2" /><path d="M2.5 20a6.5 6.5 0 0 1 13 0M16 5.4a3.2 3.2 0 0 1 0 6.2M18 14.4a6.5 6.5 0 0 1 3.5 5.6" /></>,
  user: <><circle cx="12" cy="8" r="3.4" /><path d="M5 20a7 7 0 0 1 14 0" /></>,
  bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.7 21a2 2 0 0 1-3.4 0" /></>,
  chat: <path d="M21 11.5a8.4 8.4 0 0 1-9 8.4 8.4 8.4 0 0 1-3.8-.9L3 21l1.9-5.2A8.4 8.4 0 0 1 12 3.1a8.4 8.4 0 0 1 9 8.4z" />,
  pencil: <path d="M4 20h4L19 9a2.1 2.1 0 0 0-3-3L5 17v3ZM14.5 6.5l3 3" />,
  image: <><rect x="3" y="4.5" width="18" height="15" rx="2.5" /><circle cx="8.5" cy="10" r="1.6" /><path d="m4 17 5-5 4 4 2.5-2.5L20 17" /></>,
  send: <path d="M4 12 20 4l-6 16-2.5-6.5L4 12Z" />,
  plus: <path d="M12 5v14M5 12h14" />,
  close: <path d="M6 6l12 12M18 6 6 18" />,
  check: <path d="m5 12.5 4.5 4.5L19 7" />,
  back: <path d="M20 12H4m0 0 6-6m-6 6 6 6" />,

  // reactions
  flame: <path d="M12 3s5 4.5 5 9a5 5 0 0 1-10 0c0-1.6.7-3 1.5-4 .2 1.2.8 2 1.7 2 1.3 0 1.9-1.3 1.8-3-.1-1.4-.5-2.7 0-4Z" />,
  heart: <path d="M12 20s-7-4.4-7-9.3A3.9 3.9 0 0 1 12 8a3.9 3.9 0 0 1 7 2.7c0 4.9-7 9.3-7 9.3Z" />,
  smile: <><circle cx="12" cy="12" r="8.5" /><path d="M8.5 14c.9 1.2 2.1 1.8 3.5 1.8s2.6-.6 3.5-1.8M9 9.5h.01M15 9.5h.01" /></>,
  tear: <><path d="M12 3.5s5.5 6 5.5 9.5a5.5 5.5 0 0 1-11 0c0-3.5 5.5-9.5 5.5-9.5Z" /><path d="M12 15.5c0 1-.8 1.7-1.7 1.5" /></>,
  wow: <><circle cx="12" cy="12" r="8.5" /><path d="M8.5 9.5h.01M15.5 9.5h.01M8.5 14.5c1 1 2.2 1.5 3.5 1.5s2.5-.5 3.5-1.5" /></>,

  // achievements
  feather: <><path d="M19 4c-6 0-11 3-11 9v6l3-1c5-1 8-5 8-14Z" /><path d="M8 19c2-4 5-7 9-9" /></>,
  message: <path d="M4 5.5h16v11H9l-5 4v-15Z" />,
  leaf: <><path d="M5 19C4 10 9 4 20 4c0 11-6 16-13 15" /><path d="M5 19c3-5 7-8 12-10" /></>,
  checkCircle: <><circle cx="12" cy="12" r="8.5" /><path d="m8.5 12.2 2.5 2.5 4.5-5" /></>,
  loop: <><path d="M17 3.5 20.5 7 17 10.5" /><path d="M20.5 7H8a4.5 4.5 0 0 0 0 9h1" /><path d="M7 20.5 3.5 17 7 13.5" /><path d="M3.5 17H15" /></>,
  book: <><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H6.5A2.5 2.5 0 0 0 4 20.5v-15Z" /><path d="M4 20.5A2.5 2.5 0 0 1 6.5 18H20v3H6.5" /></>,
  bolt: <path d="M13.5 3 5.5 13.5H11L10.5 21l8-10.5H13L13.5 3Z" />,
  moon: <path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5Z" />,
  crown: <path d="M4 17.5h16M4 17.5 3 7l5 3.5L12 4l4 6.5L21 7l-1 10.5" />,
  trophy: <><path d="M7 4h10v5a5 5 0 0 1-10 0V4Z" /><path d="M7 6H4v1.5A3.5 3.5 0 0 0 7.5 11M17 6h3v1.5a3.5 3.5 0 0 1-3.5 3.5M9.5 14h5l-.5 3h-4l-.5-3ZM8 20h8" /></>,

  // misc
  eye: <><path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" /><circle cx="12" cy="12" r="3" /></>,
  shield: <path d="M12 3.5 5 6.5v5c0 4.4 3 7.6 7 9 4-1.4 7-4.6 7-9v-5l-7-3Z" />,
  chart: <path d="M4 20V4M4 20h16M8 17V12M12 17V8M16 17v-6" />,
  edit: <path d="M4 20h4L19 9a2.1 2.1 0 0 0-3-3L5 17v3Z" />,
  phone: <path d="M6 3.5h3l1.5 4-2 1.5a10 10 0 0 0 6.5 6.5l1.5-2 4 1.5v3a2 2 0 0 1-2.2 2A15.5 15.5 0 0 1 4 5.7 2 2 0 0 1 6 3.5Z" />,
  video: <><rect x="3" y="6" width="12" height="12" rx="2.5" /><path d="m15 11 6-3.5v9L15 13" /></>,
  smileIcon: <><circle cx="12" cy="12" r="8.5" /><path d="M8.5 14c.9 1.2 2.1 1.8 3.5 1.8s2.6-.6 3.5-1.8M9 9.5h.01M15 9.5h.01" /></>,
  paperclip: <path d="M17 8.5 10 15.5a2.5 2.5 0 0 0 3.5 3.5l7-7a4.5 4.5 0 0 0-6.5-6.5l-7 7a6.5 6.5 0 0 0 9 9l6-6" />,
  trash: <path d="M5 7h14M10 7V5h4v2M6.5 7l1 12.5h9L17.5 7M10 11v5M14 11v5" />,
  star: <path d="m12 3.5 2.6 5.4 5.9.8-4.3 4.2 1 5.9-5.2-2.8-5.2 2.8 1-5.9L3.5 9.7l5.9-.8L12 3.5Z" />,
}

export default function Icon({ name, size = 18, className = '', strokeWidth = 1.7 }) {
  const glyph = PATHS[name]
  if (!glyph) return null
  return (
    <svg
      className={'icon ' + className}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {glyph}
    </svg>
  )
}