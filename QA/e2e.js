/**
 * Wild-Card browser E2E (puppeteer-core + system chromium).
 *
 * İstifadə (backend 8080 + frontend 5173 işlək olmalıdır):
 *   cd QA && node e2e.js
 *
 * Yoxlayır: login (wrong+right), feed tabları, post yaratma, post detail,
 * şərh, report dialog, bütün əsas route-lar, WS (/ws), alerts dropdown, logout.
 */
const puppeteer = require('puppeteer-core')

const BASE = process.env.BASE || 'http://localhost:5173'
const CHROME = process.env.CHROME || '/usr/bin/chromium'
const ADMIN_EMAIL = process.env.ADMIN_EMAIL || 'admin@wildcard.com'
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'admin123'

const results = []
const pageErrors = []
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

function check(name, ok, extra = '') {
  results.push({ name, ok: !!ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${extra ? '  -- ' + extra : ''}`)
}

async function waitSel(page, sel, timeout = 8000) {
  try {
    await page.waitForSelector(sel, { timeout })
    return true
  } catch {
    return false
  }
}

async function textExists(page, sel, text) {
  return page.evaluate((s, t) => {
    const els = [...document.querySelectorAll(s)]
    return els.some((e) => (e.textContent || '').includes(t))
  }, sel, text)
}

async function waitText(page, sel, text, timeout = 8000) {
  const t0 = Date.now()
  while (Date.now() - t0 < timeout) {
    if (await textExists(page, sel, text)) return true
    await sleep(250)
  }
  return false
}

/** React-controlled inputun bütün mətnini seçib yenisi ilə əvəz et. */
async function replaceInput(page, sel, value) {
  await page.focus(sel)
  await page.keyboard.down('Control')
  await page.keyboard.press('KeyA')
  await page.keyboard.up('Control')
  await page.keyboard.press('Backspace')
  if (value) await page.type(sel, value)
}

/** Profil başlığındakı @nick dəqiq olana qədər gözlə (substring deyil). */
async function waitHead(page, name, timeout = 5000) {
  const t0 = Date.now()
  while (Date.now() - t0 < timeout) {
    const ok = await page.evaluate((n) => {
      const span = document.querySelector('.prof-head span.muted')
      return !!span && span.textContent.trim() === '@' + n
    }, name)
    if (ok) return true
    await sleep(250)
  }
  return false
}

/** Selector ekranıdan yoxa çıxana qədər gözlə. */
async function waitGone(page, sel, timeout = 8000) {
  const t0 = Date.now()
  while (Date.now() - t0 < timeout) {
    if (!(await page.$(sel))) return true
    await sleep(250)
  }
  return false
}

async function gotoHash(page, hash, settle = 800) {
  await page.evaluate((h) => {
    location.hash = h
  }, hash)
  await sleep(settle)
}

/** Overlay içində selector varmı + görünən xəta yoxdur. */
async function overlayOk(page, probe) {
  const state = await page.evaluate((p) => {
    const ov = document.querySelector('.pane-overlay.overlay-on')
    if (!ov) return { open: false }
    const probeEl = p.kind === 'text' ? ov : ov.querySelector(p.sel)
    const has = p.kind === 'text' ? (ov.textContent || '').includes(p.val) : !!probeEl
    const err = ov.querySelector('.alert')
    return { open: true, has, err: err ? (err.textContent || '').slice(0, 120) : null }
  }, probe)
  return state.open && state.has && !state.err
}

;(async () => {
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: true,
    args: ['--no-sandbox', '--disable-dev-shm-usage', '--window-size=1400,950'],
  })
  const page = await browser.newPage()
  await page.setViewport({ width: 1400, height: 950 })
  page.on('pageerror', (e) => pageErrors.push(String(e).slice(0, 200)))

  // WebSocket spy + wc:alerts counter
  await page.evaluateOnNewDocument(() => {
    window.__wsUrls = []
    const Orig = window.WebSocket
    const Wrapped = function (url, ...rest) {
      window.__wsUrls.push(String(url))
      return new Orig(url, ...rest)
    }
    Wrapped.prototype = Orig.prototype
    window.WebSocket = Wrapped
    window.__alerts = 0
    window.addEventListener('wc:alerts', () => {
      window.__alerts += 1
    })
  })

  /* ------------------------------------------------ 1. login page */
  await page.goto(`${BASE}/#login`, { waitUntil: 'networkidle2' })
  check(
    'Login səhifəsi açılır (email+password forması)',
    !!(await page.$('input[type=email]')) && !!(await page.$('input[type=password]')),
  )

  /* ------------------------------------------------ 2. wrong password */
  await page.type('input[type=email]', ADMIN_EMAIL)
  await page.type('input[type=password]', 'definitely-wrong-pw-1')
  await page.click('form .btn.primary')
  check('Yanlış parol → xəta göstərilir', await waitSel(page, 'em.field-err', 6000))

  /* ------------------------------------------------ 3. correct login */
  await replaceInput(page, 'input[type=email]', ADMIN_EMAIL)
  await replaceInput(page, 'input[type=password]', ADMIN_PASSWORD)
  await page.click('form .btn.primary')
  check('Login → #feed keçid', await waitSel(page, '.composer', 10000))
  check('Hash #feed oldu', (await page.evaluate(() => location.hash)) === '#feed')

  /* ------------------------------------------------ 4. websocket */
  await sleep(2500)
  const wsUrls = await page.evaluate(() => window.__wsUrls)
  check('Browser /ws WebSocket açdı', wsUrls.some((u) => u.includes('/ws')), wsUrls.join(','))

  /* ------------------------------------------------ 5. feed tabs */
  const tabCount = await page.$$eval('.pane-feed .tab', (els) => els.length)
  check('Feed tabları var (ən azı 2)', tabCount >= 2, `tabs=${tabCount}`)
  const postCount = await page.$$eval('.pane-feed .post-title', (els) => els.length)
  check('Feed-də postlar görünür', postCount > 0, `posts=${postCount}`)
  await page.evaluate(() => {
    const tabs = [...document.querySelectorAll('.pane-feed .tab')]
    const recency = tabs.find((t) => /recency|latest|recent/i.test(t.textContent))
    if (recency) recency.click()
  })
  await sleep(900)
  const afterTab = await page.$$eval('.pane-feed .post-title', (els) => els.length)
  check('Tab dəyişdirildi → feed yenilənir', afterTab > 0, `posts=${afterTab}`)

  /* ------------------------------------------------ 6. create post */
  const peek = await page.$('.composer-peek')
  if (peek) await peek.click()
  const titleOk = await waitSel(page, 'input[placeholder=Title]', 4000)
  const title = `E2E test post ${Date.now()}`
  if (titleOk) {
    await page.type('input[placeholder=Title]', title)
    await page.type('textarea.composer-body', 'Automated E2E run: post body for wiring check.')
    await page.evaluate(() => {
      const form = document.querySelector('.composer')
      const btn = [...form.querySelectorAll('button')].find((b) => b.textContent.trim() === 'Post')
      btn.click()
    })
  }
  let created = await waitText(page, '.pane-feed .post-title', title, 10000)
  if (!created) {
    // bəzi sort-larda bizim post yuxarı düşməyə bilər → recency tabına keç
    await page.evaluate(() => {
      const tabs = [...document.querySelectorAll('.pane-feed .tab')]
      const recency = tabs.find((t) => /recency|latest|recent/i.test(t.textContent))
      if (recency) recency.click()
    })
    created = await waitText(page, '.pane-feed .post-title', title, 6000)
  }
  check('Post yaradılır və feed-də görünür', created, title)

  /* ------------------------------------------------ 7. post detail */
  await page.evaluate((t) => {
    const b = [...document.querySelectorAll('.pane-feed .post-title')].find((x) =>
      (x.textContent || '').includes(t),
    )
    if (b) b.click()
  }, title)
  await sleep(900)
  const detailHash = await page.evaluate(() => location.hash)
  check('Post detail açılır (#post?id=)', detailHash.startsWith('#post?id='), detailHash)

  /* ------------------------------------------------ 8. comment */
  const cOk = await waitSel(page, 'input[placeholder="Write your comment…"]', 6000)
  if (cOk) {
    await page.type('input[placeholder="Write your comment…"]', 'E2E comment: works!')
    await page.evaluate(() => {
      const input = [...document.querySelectorAll('input')].find(
        (i) => i.placeholder === 'Write your comment…',
      )
      const form = input.closest('form')
      form.querySelector('button').click()
    })
  }
  check('Şərh əlavə olunur', await waitText(page, '.comment', 'E2E comment: works!', 8000))

  /* ------------------------------------------------ 9. report dialog */
  await gotoHash(page, '#feed')
  let reported = false
  let reportNote = ''
  for (let attempt = 0; attempt < 6 && !reported; attempt++) {
    const btns = await page.$$('.pane-feed .report-btn')
    if (!btns.length) break
    const btn = btns[Math.min(attempt, btns.length - 1)]
    try {
      await btn.click()
      if (!(await waitSel(page, '.crop-modal', 3000))) continue
      const chip = await page.$('.crop-modal .chip')
      if (chip) await chip.click()
      await page.evaluate(() => {
        const b = [...document.querySelectorAll('.crop-modal button')].find((x) =>
          x.textContent.includes('Send report'),
        )
        if (b) b.click()
      })
      const t0 = Date.now()
      while (Date.now() - t0 < 5000) {
        if (await textExists(page, '.crop-modal', 'Thanks')) {
          reported = true
          break
        }
        if (await page.$('.crop-modal .alert')) {
          const err = await page.$eval('.crop-modal .alert', (e) => e.textContent)
          reportNote = 'backend rədd etdi: ' + err.slice(0, 80)
          break
        }
        await sleep(250)
      }
      if (!reported) {
        await page.evaluate(() => {
          const b = [...document.querySelectorAll('.crop-modal button')].find(
            (x) => x.textContent.trim() === 'Cancel',
          )
          if (b) b.click()
        })
        await sleep(400)
      }
    } catch (e) {
      reportNote = String(e).slice(0, 80)
      await sleep(300)
    }
  }
  check('Report dialog → Send report işləyir', reported, reportNote)

  /* ------------------------------------------------ 10. main routes */
  const routes = [
    ['#profile', { kind: 'sel', sel: '.prof-right' }, 'Profile (öz səhifəsi + XP history)'],
    ['#profile', { kind: 'text', val: 'XP history' }, 'Profile → XP history kartı'],
    ['#profile', { kind: 'text', val: 'Log out' }, 'Profile → Log out düyməsi'],
    ['#leaderboard', { kind: 'text', val: 'Weekly leaderboard' }, 'Leaderboard açılır'],
    ['#chat', { kind: 'text', val: 'Messages' }, 'Chat açılır'],
    ['#achievements', { kind: 'text', val: 'Achievements' }, 'Achievements açılır'],
    ['#musicarc', { kind: 'text', val: 'Daily check-in' }, 'Music arc açılır'],
    ['#friends', { kind: 'sel', sel: 'input[placeholder="Search people"]' }, 'Friends açılır'],
    ['#admin', { kind: 'text', val: 'Admin' }, 'Admin panel açılır'],
    ['#moderation', { kind: 'text', val: 'Moderation' }, 'Moderation açılır'],
  ]
  for (const [hash, probe, label] of routes) {
    await gotoHash(page, hash)
    const ok = await overlayOk(page, probe)
    check(label, ok)
  }

  /* ---------------------- 10a. hər overlay-də geri düyməsi olmalıdır */
  for (const [hash, label] of [
    ['#achievements', 'Achievements'],
    ['#compatibility?id=2', 'Compatibility'],
    ['#leaderboard', 'Leaderboard'],
  ]) {
    await gotoHash(page, hash)
    const hasBack = !!(await page.$('.pane-overlay.overlay-on .back-btn'))
    check(`${label}: ← Back düyməsi var`, hasBack)
    if (hasBack) {
      await page.click('.pane-overlay.overlay-on .back-btn')
      await sleep(700)
      const h = await page.evaluate(() => location.hash)
      check(`${label}: Back feed-ə qaytarır`, h === '#feed', h)
    }
  }

  /* ------------------------------------------- 10b. watchlist add form */
  await gotoHash(page, '#watchlist')
  const wlTabs = await page.$$eval('.pane-overlay.overlay-on .tab', (els) => els.length)
  check('Watchlist açılır (status tabları)', wlTabs >= 2, `tabs=${wlTabs}`)
  await page.evaluate(() => {
    const b = [...document.querySelectorAll('.pane-overlay.overlay-on button')].find((x) =>
      x.textContent.includes('+ Add title'),
    )
    if (b) b.click()
  })
  const wlForm = await waitSel(page, '.pane-overlay.overlay-on input[placeholder=Rating]', 4000)
  check('Watchlist → "+ Add title" forması açılır', wlForm)
  if (wlForm) {
    const wlTitle = `E2E Watch ${Date.now()}`
    await page.type('.pane-overlay.overlay-on input[placeholder=Title]', wlTitle)
    await page.evaluate(() => {
      const form = document.querySelector('.pane-overlay.overlay-on form.add-form')
      const btn = [...form.querySelectorAll('button')].find((b) => b.textContent.trim() === 'Add')
      btn.click()
    })
    check('Watchlist-ə title əlavə olunur', await waitText(page, '.pane-overlay.overlay-on', wlTitle, 8000))

    // əlavədən sonra reytinqi dəyişmək (★ seçicisi)
    const rated = await page.evaluate((t) => {
      const card = [...document.querySelectorAll('.wl-card')].find((c) =>
        (c.textContent || '').includes(t),
      )
      if (!card) return false
      const sel = card.querySelector('select.wl-rating')
      if (!sel) return false
      const setter = Object.getOwnPropertyDescriptor(window.HTMLSelectElement.prototype, 'value').set
      setter.call(sel, '9')
      sel.dispatchEvent(new Event('change', { bubbles: true }))
      return true
    }, wlTitle)
    let ratingOk = false
    const t0 = Date.now()
    while (Date.now() - t0 < 6000) {
      ratingOk = await page.evaluate(
        (t) => {
          const card = [...document.querySelectorAll('.wl-card')].find((c) =>
            (c.textContent || '').includes(t),
          )
          return !!card && (card.textContent || '').includes('★ 9')
        },
        wlTitle,
      )
      if (ratingOk) break
      await sleep(300)
    }
    check('Watchlist-də reytinq sonradan dəyişilir (★ 9)', rated && ratingOk)
  }

  /* ------------------------------------------------ 10c. edit profile */
  const DLG = '[role="dialog"][aria-label="Edit profile"] .crop-modal'
  async function openEditDialog() {
    await gotoHash(page, '#profile')
    await page.evaluate(() => {
      const b = [...document.querySelectorAll('.prof-left button')].find(
        (x) => x.textContent.trim() === 'Edit profile',
      )
      if (b) b.click()
    })
    return waitSel(page, DLG, 4000)
  }
  async function clickSave() {
    await page.evaluate(() => {
      const b = [...document.querySelectorAll('.crop-modal button')].find((x) =>
        x.textContent.includes('Save changes'),
      )
      if (b) b.click()
    })
  }

  const dlgOpened = await openEditDialog()
  check('Edit profile dialoquu açılır', dlgOpened)
  if (dlgOpened) {
    const prefill = await page.evaluate(() => ({
      u: document.querySelector('#ep-username')?.value || '',
      e: document.querySelector('#ep-email')?.value || '',
    }))
    check(
      'Sahələr prefill olunub (nick + email)',
      !!prefill.u && prefill.e === ADMIN_EMAIL,
      `${prefill.u}/${prefill.e}`,
    )
    const origName = prefill.u

    // --- səhv cari parol → xəta, dialoq bağlanmır
    await replaceInput(page, 'input[placeholder="Current password"]', 'wrong-pw-xyz')
    await replaceInput(
      page,
      'input[placeholder="New password (min 8, letter + number)"]',
      'Changed1234',
    )
    await replaceInput(page, 'input[placeholder="Repeat new password"]', 'Changed1234')
    await clickSave()
    const wrongPwErr = await waitText(page, '.crop-modal .alert', 'Current password is incorrect', 6000)
    const stillOpen = !!(await page.$(DLG))
    check('Səhv cari parol → xəta, dəyişiklik olmur', wrongPwErr && stillOpen)
    // şifrə sahələrini təmizlə (sonrakı save-lərə qarışmasın)
    await replaceInput(page, 'input[placeholder="Current password"]', '')
    await replaceInput(page, 'input[placeholder="New password (min 8, letter + number)"]', '')
    await replaceInput(page, 'input[placeholder="Repeat new password"]', '')

    // --- bio dəyiş → serverdən geri gəlir
    const newBio = `E2E edit-profile bio ${Date.now()}`
    await replaceInput(page, '#ep-bio', newBio)
    await clickSave()
    const bioSaved = await waitGone(page, DLG)
    check('Edit profile → bio yadda saxlanılır', bioSaved)

    // --- username dəyiş + geri qaytar (növbəti run-lar üçün bərpa edilir)
    const newName = origName + 'e2e' + String(Date.now()).slice(-4)
    let nameFlow = await openEditDialog()
    if (nameFlow) {
      await replaceInput(page, '#ep-username', newName)
      await clickSave()
      nameFlow = (await waitGone(page, DLG)) && (await waitHead(page, newName))
    }
    check('Edit profile → username dəyişir', !!nameFlow, newName)

    let reverted = await openEditDialog()
    if (reverted) {
      await replaceInput(page, '#ep-username', origName)
      await clickSave()
      reverted = (await waitGone(page, DLG)) && (await waitHead(page, origName))
    }
    check('Edit profile → username geri qaytarılır', !!reverted, origName)
  }

  /* ------------------------------------------------ 11. alerts dropdown */
  await gotoHash(page, '#feed')
  await page.click('.quick .icon-btn[title=Alerts]')
  await sleep(700)
  const alertsOk = await page.evaluate(() => {
    const drop = document.querySelector('.alerts-drop')
    return !!drop && (drop.textContent || '').includes('Notifications')
  })
  check('Alerts (bildirişlər) dropdown açılır', alertsOk)
  await page.evaluate(() => {
    const dim = document.querySelector('.drop-dim')
    if (dim) dim.click()
  })
  await sleep(400)

  /* ------------------------------------------------ 12. logout */
  await gotoHash(page, '#profile')
  const logoutOk = await page.evaluate(() => {
    const b = [...document.querySelectorAll('.prof-right .tab')].find(
      (x) => x.textContent.trim() === 'Log out',
    )
    if (b) {
      b.click()
      return true
    }
    return false
  })
  await sleep(1200)
  const backToLogin = !!(await page.$('input[type=password]'))
  check('Logout işləyir → login səhifəsinə qayıdır', logoutOk && backToLogin)

  await gotoHash(page, '#feed')
  const guarded = !!(await page.$('input[type=password]'))
  check('Logout sonrası #feed qorunur (login tələb olunur)', guarded)

  /* ------------------------------------------------ summary */
  await browser.close()

  const passed = results.filter((r) => r.ok).length
  const total = results.length
  console.log('\n-----------------------------------------')
  console.log(`E2E: ${passed}/${total} PASS`)
  if (pageErrors.length) {
    console.log(`Səhifsə (uncaught) JS xətaları: ${pageErrors.length}`)
    pageErrors.slice(0, 5).forEach((e) => console.log('  - ' + e))
  } else {
    console.log('Uncaught JS error yoxdur (crash yoxdur).')
  }
  process.exit(passed === total && pageErrors.length === 0 ? 0 : 1)
})().catch((e) => {
  console.error('E2E crashed:', e)
  process.exit(1)
})
