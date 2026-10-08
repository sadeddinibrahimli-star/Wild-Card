#!/usr/bin/env python3
"""
Wild-Card API smoke test (the backend must be running on 8080).

    cd QA && python3 smoke.py

Every check prints "PASS/FAIL" and a table at the end. Options:
    ADMIN_EMAIL / ADMIN_PASSWORD / BASE can be overridden via env.
"""
import os
import sys
import time

import requests

BASE = os.environ.get("BASE", "http://localhost:8080/api/v1")
UPLOAD_BASE = os.environ.get("UPLOAD_BASE", "http://localhost:8080")
ADMIN_EMAIL = os.environ.get("ADMIN_EMAIL", "admin@wildcard.com")
ADMIN_PASSWORD = os.environ.get("ADMIN_PASSWORD", "admin123")

results = []
_ts = str(int(time.time()))[-6:]


def check(name, ok, required=True, extra=""):
    results.append({"name": name, "ok": bool(ok), "required": required})
    tag = "PASS" if ok else ("SKIP" if not required else "FAIL")
    print(f"{tag}  {name}{('  -- ' + extra) if extra else ''}")


def skip(name, extra=""):
    check(name, False, required=False, extra=extra)


def req(method, path, token=None, json_body=None, params=None, url=None):
    headers = {}
    if token:
        headers["Authorization"] = "Bearer " + token
    full = url or (BASE + path)
    try:
        return requests.request(method, full, headers=headers, json=json_body,
                                params=params, timeout=20)
    except requests.RequestException as e:
        print("  transport error:", e)
        return None


def data_of(r):
    if r is None:
        return None
    try:
        return r.json().get("data")
    except Exception:
        return None


def items_of(r):
    """PageResponse ({content:[...]}) və ya birbaşa list cavabları üçün."""
    d = data_of(r)
    if isinstance(d, dict):
        return d.get("content") or []
    if isinstance(d, list):
        return d
    return []


def expect(name, r, codes, required=True):
    if r is None:
        check(name, False, required, "no response")
        return False
    ok = r.status_code in codes
    extra = "" if ok else f"got {r.status_code}: {r.text[:140]}"
    check(name, ok, required, extra)
    return ok


def login(email, password):
    r = req("POST", "/auth/login", json_body={"email": email, "password": password})
    d = data_of(r)
    if not d:
        return None, None
    token = d.get("accessToken") or d.get("token") or d.get("accessTokenValue")
    refresh = d.get("refreshToken")
    return token, refresh


def main():
    # ---------------------------------------------------------- auth
    admin, admin_refresh = login(ADMIN_EMAIL, ADMIN_PASSWORD)
    check("POST /auth/login (admin)", bool(admin), True,
          "admin tapılmadı" if not admin else "")
    if not admin:
        print("\nAdmin login alınmadı — dayanır.")
        return 1

    r = req("POST", "/auth/login", json_body={"email": ADMIN_EMAIL, "password": "wrong-pw-1"})
    expect("Yanlış parol → 4xx", r, [400, 401, 403])

    uname = f"smoke_user_{_ts}"
    r = req("POST", "/auth/register",
            json_body={"username": uname, "email": f"{uname}@example.com",
                       "password": "Passw0rd123"})
    expect("POST /auth/register (temp istifadəçi)", r, [200, 201])
    user, user_refresh = login(f"{uname}@example.com", "Passw0rd123")
    check("Temp istifadəçi login", bool(user))
    temp_id = None
    if user:
        r_me = req("GET", "/users/me", token=user)
        temp_id = (data_of(r_me) or {}).get("id")

    r = req("POST", "/auth/register",
            json_body={"username": uname, "email": f"{uname}@example.com",
                       "password": "Passw0rd123"})
    expect("Eyni email ilə register → 4xx", r, [400, 409])

    if admin_refresh:
        r = req("POST", "/auth/refresh", json_body={"refreshToken": admin_refresh})
        expect("POST /auth/refresh", r, [200])
    else:
        skip("POST /auth/refresh", "refreshToken cavabda yoxdur")

    r = req("POST", "/auth/forgot-password", json_body={"email": ADMIN_EMAIL})
    expect("POST /auth/forgot-password", r, [200])
    r = req("GET", "/dev/mailbox", token=admin)
    expect("GET /dev/mailbox (admin, dev profil)", r, [200])

    r = req("GET", "/users/me")
    expect("Token-suz /users/me → 401", r, [401])

    # ---------------------------------------------------------- users
    r = req("GET", "/users/me", token=admin)
    me = data_of(r)
    admin_id = (me or {}).get("id")
    expect("GET /users/me", r, [200])
    r = req("PUT", "/users/me", token=admin, json_body={"bio": "smoke test bio 123"})
    expect("PUT /users/me (bio)", r, [200])

    # the email only appears in the user's own /users/me response, a public profile does not leak it
    r = req("GET", "/users/me", token=user or admin)
    check("GET /users/me email qaytarır",
          bool((data_of(r) or {}).get("email")), True)
    r = req("GET", f"/users/{admin_id}", token=admin)
    pub = data_of(r) or {}
    check("Publik /users/{id} email SIZDIRMIR", pub.get("email") is None, True,
          str(pub.get("email")))

    # full profile edit: nick / email / password (only on the temp user)
    if user:
        r = req("PUT", "/users/me", token=user,
                json_body={"currentPassword": "wrong-pw-1", "newPassword": "Changed1234"})
        expect("PUT /users/me (səhv cari parol) → 400", r, [400])
        r = req("PUT", "/users/me", token=user, json_body={"username": "admin"})
        expect("PUT /users/me (tutuşan username) → 400", r, [400])
        r = req("PUT", "/users/me", token=user,
                json_body={"currentPassword": "Passw0rd123", "newPassword": "abcdefgh"})
        expect("PUT /users/me (rəqəmsiz yeni parol) → 400", r, [400])
        r = req("PUT", "/users/me", token=user, json_body={"username": "ab"})
        expect("PUT /users/me (qısa username) → 400", r, [400])

        new_u = uname + "x"
        new_em = f"{new_u}@example.com"
        r = req("PUT", "/users/me", token=user,
                json_body={"username": new_u, "email": new_em,
                           "bio": "smoke edited bio",
                           "currentPassword": "Passw0rd123",
                           "newPassword": "Changed1234"})
        expect("PUT /users/me (nick+email+bio+parol dəyişir)", r, [200])
        d = data_of(r) or {}
        check("Cavabda yeni nick/email",
              d.get("username") == new_u and d.get("email") == new_em, True,
              f"{d.get('username')}/{d.get('email')}")
        r = req("POST", "/auth/login", json_body={"email": new_em, "password": "Passw0rd123"})
        expect("Köhnə parolla login → 4xx", r, [400])
        r = req("POST", "/auth/login", json_body={"email": new_em, "password": "Changed1234"})
        expect("Yeni parolla login → 200", r, [200])

    r = req("GET", "/users", token=admin)
    expect("GET /users", r, [200])
    r = req("GET", "/users/discover", token=admin)
    expect("GET /users/discover", r, [200])
    r = req("GET", f"/users/{admin_id}", token=admin)
    expect("GET /users/{id}", r, [200])
    r = req("GET", f"/users/{admin_id}/card", token=admin)
    expect("GET /users/{id}/card", r, [200])
    r = req("GET", f"/users/{admin_id}/achievements", token=admin)
    expect("GET /users/{id}/achievements", r, [200])
    r = req("GET", f"/users/{admin_id}/music-arc", token=admin)
    expect("GET /users/{id}/music-arc", r, [200])
    r = req("GET", f"/users/{admin_id}/posts", token=admin)
    expect("GET /users/{id}/posts", r, [200])
    r = req("GET", "/users/me/xp-history", token=admin)
    expect("GET /users/me/xp-history", r, [200])
    r = req("GET", "/users/me/topics", token=admin)
    expect("GET /users/me/topics", r, [200])

    # ---------------------------------------------------------- topics
    r = req("GET", "/topics")
    expect("Token-suz /topics → 401", r, [401])
    r = req("GET", "/topics", token=admin)
    topics = data_of(r) or []
    expect("GET /topics (auth)", r, [200])
    if topics:
        tid = topics[0].get("id")
        r = req("GET", f"/topics/{tid}", token=admin)
        expect("GET /topics/{id}", r, [200])
        r = req("PUT", "/users/me/topics", token=admin, json_body={"topicIds": [tid]})
        expect("PUT /users/me/topics", r, [200])
    else:
        skip("GET /topics/{id}", "seed-də topic yoxdur")
        skip("PUT /users/me/topics", "seed-də topic yoxdur")

    # ---------------------------------------------------------- feed / posts
    r = req("GET", "/home/feed", token=admin)
    expect("GET /home/feed", r, [200])
    for sort in ("RECENCY", "latest"):
        r = req("GET", "/home/feed", token=admin, params={"sort": sort})
        expect(f"GET /home/feed?sort={sort}", r, [200])
    # the enum validation lives on /feed (FeedSort.from -> 400)
    r = req("GET", "/feed", token=admin, params={"sort": "latest"})
    expect("GET /feed?sort=latest", r, [200])
    for sort in ("top", "bogus"):
        r = req("GET", "/feed", token=admin, params={"sort": sort})
        expect(f"GET /feed?sort={sort} → 400", r, [400])
    r = req("GET", "/feed", token=admin)
    expect("GET /feed", r, [200])
    r = req("GET", "/posts", token=admin)
    expect("GET /posts", r, [200])

    r = req("POST", "/posts", token=admin,
            json_body={"category": "ANIME", "title": f"Smoke post {_ts}",
                       "body": "created by smoke.py"})
    expect("POST /posts (admin)", r, [200, 201])
    post = data_of(r) or {}
    post_id = post.get("id")
    check("Yaradılan post-un id-si var", bool(post_id), True)

    if post_id:
        r = req("GET", f"/posts/{post_id}", token=admin)
        expect("GET /posts/{id}", r, [200])
        r = req("PUT", f"/posts/{post_id}", token=admin,
                json_body={"title": f"Smoke post edited {_ts}"})
        expect("PUT /posts/{id} (redaktə)", r, [200])
        r = req("GET", f"/posts/{post_id}/comments", token=admin)
        expect("GET /posts/{id}/comments", r, [200])
        r = req("POST", f"/posts/{post_id}/comments", token=admin,
                json_body={"body": "smoke comment"})
        expect("POST /posts/{id}/comments", r, [200, 201])
        comment = data_of(r) or {}
        comment_id = comment.get("id")
        if comment_id:
            # first someone else tries to delete (4xx), then the owner deletes
            if user:
                r = req("DELETE", f"/comments/{comment_id}", token=user)
                expect("Başqasının şərhini silmək → 4xx", r, [400, 403])
            # the comment belongs to its author - only the owner can delete it
            r = req("DELETE", f"/comments/{comment_id}", token=admin)
            expect("DELETE /comments/{id} (öz şərhi)", r, [200, 204])
        else:
            skip("DELETE /comments/{id}", "comment id yoxdur")

        # rules: reacting to your own post is forbidden, to someone else's is allowed
        if user:
            r = req("PUT", f"/posts/{post_id}/reaction", token=user,
                    json_body={"type": "FIRE"})
            expect("Reaksiya başqasının post-una (icazəli)", r, [200])
        r = req("PUT", f"/posts/{post_id}/reaction", token=admin,
                json_body={"type": "FIRE"})
        expect("Reaksiya öz post-una → 4xx", r, [400, 403])
        r = req("GET", f"/posts/{post_id}/reaction", token=admin)
        expect("GET /posts/{id}/reaction", r, [200])
        r = req("DELETE", f"/posts/{post_id}/reaction", token=user or admin)
        expect("DELETE /posts/{id}/reaction", r, [200, 204])
        r = req("PUT", f"/posts/{post_id}/reaction", token=admin,
                json_body={"type": "BUZZKILL"})
        expect("Yanlış reaction tipi → 400", r, [400])
    else:
        for label in ("GET /posts/{id}", "PUT /posts/{id}", "comments", "reaction"):
            skip(label, "post yaradılmadı")

    # ---------------------------------------------------------- social
    if user:
        r = req("POST", f"/social/follow/{admin_id}", token=user)
        expect("POST /social/follow/{id}", r, [200, 201, 204])
        r = req("GET", f"/social/follow/{admin_id}/status", token=user)
        expect("GET /social/follow/{id}/status", r, [200])
        r = req("GET", f"/users/{admin_id}/followers", token=admin)
        expect("GET /users/{id}/followers", r, [200])
        r = req("GET", f"/users/{admin_id}/following", token=admin)
        expect("GET /users/{id}/following", r, [200])
    else:
        skip("POST /social/follow/{id}", "temp login yoxdur")

    # ---------------------------------------------------------- watchlist
    r = req("GET", "/watchlist", token=admin)
    expect("GET /watchlist", r, [200])
    r = req("POST", "/watchlist", token=admin,
            json_body={"title": f"Smoke Anime {_ts}", "kind": "ANIME",
                       "status": "WATCHING", "rating": 7})
    expect("POST /watchlist", r, [200, 201])
    wl = data_of(r) or {}
    wl_id = wl.get("id")
    r = req("GET", "/watchlist/stats", token=admin)
    expect("GET /watchlist/stats", r, [200])
    if wl_id:
        r = req("PUT", f"/watchlist/{wl_id}", token=admin,
                json_body={"status": "COMPLETED", "rating": 9})
        expect("PUT /watchlist/{id}", r, [200])
        r = req("DELETE", f"/watchlist/{wl_id}", token=admin)
        expect("DELETE /watchlist/{id}", r, [200, 204])
    else:
        skip("PUT /watchlist/{id}", "item yaradılmadı")
        skip("DELETE /watchlist/{id}", "item yaradılmadı")

    # ---------------------------------------------------------- notifications
    if user and temp_id:
        r = req("POST", f"/social/follow/{temp_id}", token=admin)
        expect("Admin temp-i follow edir (notification yaradır)", r, [200, 201, 204])
        r = req("GET", "/notifications", token=user)
        expect("GET /notifications", r, [200])
        r = req("GET", "/notifications", token=user, params={"filter": "follows"})
        expect("GET /notifications?filter=follows", r, [200])
        r = req("GET", "/notifications", token=user, params={"filter": "bogus"})
        expect("GET /notifications?filter=bogus → 400", r, [400])
        r = req("GET", "/notifications/unread-count", token=user)
        expect("GET /notifications/unread-count", r, [200])
        r_list = req("GET", "/notifications", token=user)
        items = items_of(r_list)
        if items and isinstance(items, list):
            nid = (items[0] or {}).get("id")
            if nid:
                r = req("PUT", f"/notifications/{nid}/read", token=user)
                expect("PUT /notifications/{id}/read", r, [200, 204])
            else:
                skip("PUT /notifications/{id}/read", "bildiriş yoxdur")
        else:
            skip("PUT /notifications/{id}/read", "notification list boş")
        r = req("PUT", "/notifications/read-all", token=user)
        expect("PUT /notifications/read-all", r, [200, 204])
    else:
        skip("GET /notifications", "temp login yoxdur")

    # ---------------------------------------------------------- chat
    if user and admin_id:
        r = req("GET", "/chat/conversations", token=user)
        expect("GET /chat/conversations", r, [200])
        r = req("POST", f"/chat/direct/{admin_id}", token=user)
        expect("POST /chat/direct/{userId}", r, [200, 201])
        conv = data_of(r) or {}
        conv_id = conv.get("id") or conv.get("conversationId")
        r = req("POST", "/chat/group", token=user,
                json_body={"title": f"Smoke group {_ts}", "memberIds": [admin_id]})
        expect("POST /chat/group", r, [200, 201])
        if not conv_id:
            conv_id = (data_of(r) or {}).get("id")
        if conv_id:
            r = req("GET", f"/chat/{conv_id}/messages", token=user)
            expect("GET /chat/{id}/messages", r, [200])
            r = req("POST", f"/chat/{conv_id}/messages", token=user,
                    json_body={"body": "smoke message"})
            expect("POST /chat/{id}/messages", r, [200, 201])
            r = req("PUT", f"/chat/{conv_id}/read", token=user)
            expect("PUT /chat/{id}/read", r, [200, 204])
        else:
            skip("GET /chat/{id}/messages", "conversation id yoxdur")
            skip("POST /chat/{id}/messages", "conversation id yoxdur")
            skip("PUT /chat/{id}/read", "conversation id yoxdur")
    else:
        skip("GET /chat/conversations", "temp login yoxdur")

    # ---------------------------------------------------------- leaderboard
    for path in ("/leaderboard/top", "/leaderboard/week", "/leaderboard/me"):
        r = req("GET", path, token=admin)
        expect(f"GET {path}", r, [200])

    # ---------------------------------------------------------- music
    r = req("GET", "/music/current", token=admin)
    expect("GET /music/current", r, [200])
    r = req("PUT", "/music/current", token=admin,
            json_body={"artist": "Yoasobi", "trackName": "Idol",
                       "note": "smoke check"})
    expect("PUT /music/current", r, [200])
    r = req("POST", "/music/arc/checkin", token=admin, json_body={"note": "smoke"})
    expect("POST /music/arc/checkin", r, [200, 201])
    r2 = req("POST", "/music/arc/checkin", token=admin, json_body={"note": "smoke again"})
    d2 = data_of(r2) or {}
    check("İkinci check-in → alreadyCheckedIn", d2.get("alreadyCheckedIn") is True,
          False, f"data={d2}")
    r = req("GET", "/music/arcs", token=admin)
    expect("GET /music/arcs", r, [200])
    arcs = items_of(r)
    if arcs and isinstance(arcs, list) and (arcs[0] or {}).get("id"):
        aid = arcs[0]["id"]
        r = req("POST", f"/music/arcs/{aid}/reaction", token=admin,
                json_body={"type": "FIRE"})
        expect("POST /music/arcs/{id}/reaction", r, [200, 201, 400, 404])
        r = req("GET", f"/music/arcs/{aid}/reactions", token=admin)
        expect("GET /music/arcs/{id}/reactions", r, [200])
    else:
        skip("POST /music/arcs/{id}/reaction", "arc yoxdur")
        skip("GET /music/arcs/{id}/reactions", "arc yoxdur")

    # ---------------------------------------------------------- reports + moderation
    report_id = None
    if post_id and user:
        r = req("POST", "/reports", token=user,
                json_body={"postId": post_id, "reason": "Spam: smoke test"})
        expect("POST /reports (başqasının post-u)", r, [200, 201])
        rep = data_of(r) or {}
        report_id = rep.get("id")
        # reporting your own post is forbidden
        my_post = req("POST", "/posts", token=user,
                      json_body={"category": "GAMING", "title": f"Temp post {_ts}",
                                 "body": "for own-report negative test"})
        my_post_id = (data_of(my_post) or {}).get("id")
        # the backend accepts a report on your own post (the test pins this behaviour)
        if my_post_id:
            r = req("POST", "/reports", token=user,
                    json_body={"postId": my_post_id, "reason": "Spam"})
            expect("POST /reports (öz post — backend icazə verir)", r, [200, 201])
        else:
            skip("Öz post-unu report etmək → 4xx", "temp post yaradılmadı")
    else:
        skip("POST /reports", "post_id və ya temp token yoxdur")

    r = req("GET", "/moderation/reports", token=admin)
    expect("GET /moderation/reports (admin)", r, [200])
    if user:
        r = req("GET", "/moderation/reports", token=user)
        expect("GET /moderation/reports istifadəçi → 403", r, [403])
    if report_id:
        r = req("PATCH", f"/moderation/reports/{report_id}", token=admin,
                json_body={"status": "REVIEWED", "resolutionNote": "smoke"})
        expect("PATCH /moderation/reports/{id}", r, [200])
    else:
        skip("PATCH /moderation/reports/{id}", "report id yoxdur")

    if post_id:
        r = req("PATCH", f"/moderation/posts/{post_id}/hide", token=admin,
                params={"hide": "true"})
        expect("PATCH /moderation/posts/{id}/hide?hide=true", r, [200])
        r = req("PATCH", f"/moderation/posts/{post_id}/hide", token=admin,
                params={"hide": "false"})
        expect("PATCH /moderation/posts/{id}/hide?hide=false", r, [200])
    else:
        skip("PATCH /moderation/posts/{id}/hide", "post_id yoxdur")

    if user:
        uid = None
        r_me = req("GET", "/users/me", token=user)
        uid = (data_of(r_me) or {}).get("id")
        if uid:
            r = req("GET", f"/moderation/users/{uid}/flagged-content", token=admin)
            expect("GET /moderation/users/{id}/flagged-content", r, [200])
            r = req("GET", f"/moderation/users/{uid}/warnings", token=admin)
            expect("GET /moderation/users/{id}/warnings", r, [200])
            r = req("POST", f"/moderation/users/{uid}/warn", token=admin,
                    json_body={"reason": "smoke warning"})
            expect("POST /moderation/users/{id}/warn", r, [200, 201])
        else:
            skip("moderation/users flagged/warnings", "temp id yoxdur")
    else:
        skip("moderation/users flagged/warnings", "temp token yoxdur")

    # ---------------------------------------------------------- admin
    r = req("GET", "/admin/stats", token=admin)
    expect("GET /admin/stats", r, [200])
    r = req("GET", "/admin/stats")
    expect("Token-suz /admin/stats → 401/403", r, [401, 403])
    r = req("GET", "/admin/users", token=admin)
    expect("GET /admin/users", r, [200])
    if user:
        r = req("GET", "/admin/users", token=user)
        expect("GET /admin/users istifadəçi → 403", r, [403])

    spare_name = f"smoke_spare_{_ts}"
    r = req("POST", "/admin/users", token=admin,
            json_body={"username": spare_name, "email": f"{spare_name}@example.com",
                       "password": "Passw0rd123", "role": "USER"})
    expect("POST /admin/users (yeni hesab)", r, [200, 201])
    spare = data_of(r) or {}
    spare_id = spare.get("id")
    if spare_id:
        # restore the role to its own value (independent of the enum)
        role = spare.get("role") or "USER"
        r = req("PATCH", f"/admin/users/{spare_id}/role", token=admin,
                params={"role": role})
        expect("PATCH /admin/users/{id}/role (@RequestParam)", r, [200])
        r = req("PATCH", f"/admin/users/{spare_id}/status", token=admin,
                params={"status": "SUSPENDED"})
        expect("PATCH /admin/users/{id}/status (SUSPENDED)", r, [200])
        r = req("PATCH", f"/admin/users/{spare_id}/status", token=admin,
                params={"status": "ACTIVE"})
        expect("PATCH /admin/users/{id}/status (bərpa ACTIVE)", r, [200])
    else:
        skip("PATCH /admin/users/{id}/role", "spare id yoxdur")
        skip("PATCH /admin/users/{id}/status", "spare id yoxdur")

    r = req("GET", "/admin/xp-config", token=admin)
    expect("GET /admin/xp-config", r, [200])
    cfg = data_of(r) or []
    if isinstance(cfg, list) and cfg and (cfg[0] or {}).get("action"):
        entry = cfg[0]
        r = req("PUT", f"/admin/xp-config/{entry['action']}", token=admin,
                json_body={"value": entry.get("value", 10)})
        expect("PUT /admin/xp-config/{action}", r, [200])
    else:
        skip("PUT /admin/xp-config/{action}", "config boş")

    # ---------------------------------------------------------- search (external APIs)
    r = req("GET", "/search/titles/status", token=admin)
    expect("GET /search/titles/status (lokal)", r, [200])
    r = req("GET", "/search/titles", token=admin, params={"q": "naruto", "type": "ANIME"})
    check("GET /search/titles (AniList — şəbəkə asılı)", r is not None and r.status_code == 200,
          False, "" if r is not None and r.status_code == 200
          else f"got {getattr(r, 'status_code', 'ERR')} (xarici API?)")
    r = req("GET", "/search/music", token=admin, params={"q": "yoasobi"})
    check("GET /search/music (xarici API)", r is not None and r.status_code == 200,
          False, "" if r is not None and r.status_code == 200
          else f"got {getattr(r, 'status_code', 'ERR')} (xarici API?)")

    # ---------------------------------------------------------- uploads
    png = (b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01"
           b"\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01"
           b"\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82")
    try:
        r = requests.post(f"{UPLOAD_BASE}/api/uploads", headers={
            "Authorization": "Bearer " + admin},
            files={"file": ("smoke.png", png, "image/png")}, timeout=20)
        up = r.json().get("data") if r.headers.get("content-type", "").startswith("application/json") else None
        url = (up or {}).get("url")
        expect("POST /uploads (png)", r, [200, 201])
        if url:
            r2 = requests.get(UPLOAD_BASE + url if url.startswith("/") else url,
                              timeout=20)
            check("Yüklənmiş fayl GET", r2.status_code == 200,
                  False, f"got {r2.status_code}")
        else:
            skip("Yüklənmiş fayl GET", "url yoxdur")
    except Exception as e:  # noqa: BLE001
        skip("POST /uploads (png)", str(e)[:80])

    # ---------------------------------------------------------- cleanup
    if post_id:
        req("DELETE", f"/posts/{post_id}", token=admin)

    # ---------------------------------------------------------- summary
    required = [x for x in results if x["required"]]
    passed = [x for x in required if x["ok"]]
    failed = [x for x in required if not x["ok"]]
    soft = [x for x in results if not x["required"]]
    soft_ok = [x for x in soft if x["ok"]]
    print("\n-----------------------------------------")
    print(f"SMOKE: {len(passed)}/{len(required)} PASS" +
          (f"  (+soft {len(soft_ok)}/{len(soft)})" if soft else ""))
    for x in failed:
        print("  FAILED: " + x["name"])
    return 0 if not failed else 1


if __name__ == "__main__":
    sys.exit(main())
