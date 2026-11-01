#!/bin/bash
# FlowDesk 스모크 테스트 (WSL Tomcat /flowdesk 가정)
set -e
BASE="${FLOWDESK_BASE:-http://127.0.0.1:8080/flowdesk}"
PASS=0
FAIL=0

ok() { echo "  OK  $*"; PASS=$((PASS+1)); }
bad() { echo "  FAIL $*"; FAIL=$((FAIL+1)); }

echo "=== static ==="
for p in /login.html /css/theme.css /js/app-shell.js; do
  code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE$p" || true)
  if [ "$code" = "200" ]; then ok "$p $code"; else bad "$p $code"; fi
done

# 보호 HTML: 미로그인 시 로그인으로 리다이렉트
for p in /home.html /users.html /menus.html; do
  code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE$p" || true)
  if [ "$code" = "302" ]; then ok "$p(unauth) $code"; else bad "$p(unauth) $code"; fi
done

echo "=== auth / menus ==="
python3 - <<'PY'
import json, os, sys, urllib.request, urllib.error, http.cookiejar

BASE = os.environ.get("FLOWDESK_BASE", "http://127.0.0.1:8080/flowdesk")
fail = 0

def login(user, pw):
    cj = http.cookiejar.CookieJar()
    op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
    req = urllib.request.Request(
        BASE + "/api/auth/login",
        data=json.dumps({"username": user, "password": pw}).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        body = json.loads(op.open(req).read().decode())
        return op, body, None
    except urllib.error.HTTPError as e:
        return None, None, (e.code, e.read().decode()[:120])

def status(op, path, follow=False):
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, req, fp, code, msg, headers, newurl):
            return None
    if not follow:
        # rebuild without redirect for HTML checks
        pass
    try:
        r = op.open(BASE + path)
        return r.status
    except urllib.error.HTTPError as e:
        return e.code

# Passwords only from env (no secrets in repo). Guest defaults to public demo.
# Prefer FLOWDESK_PASS_* ; FD_PASS_* kept as alias for local scripts.
def pass_of(name, default=""):
    return (
        os.environ.get("FLOWDESK_PASS_" + name)
        or os.environ.get("FD_PASS_" + name)
        or default
    ).strip()

cases = [
    ("admin", pass_of("ADMIN"), "01", True, True, True),
    ("hwlee", pass_of("HWLEE"), "01", True, True, True),
    ("hani", pass_of("HANI"), "01", True, False, False),
    ("guest", pass_of("GUEST", "guest"), "02", False, False, False),
]

print("login matrix")
for user, pw, auth, expect_inq, expect_users, expect_menus in cases:
    if not pw:
        print(f"  SKIP {user} (set FLOWDESK_PASS_{user.upper()} or FD_PASS_*)")
        continue
    op, body, err = login(user, pw)
    if err:
        print(f"  FAIL {user} login {err}")
        fail += 1
        continue
    if body.get("authCd") != auth:
        print(f"  FAIL {user} authCd want {auth} got {body.get('authCd')}")
        fail += 1
    else:
        print(f"  OK  {user} auth={auth} display={body.get('displayName')} menus={[m.get('name') for m in body.get('menuList') or []]}")

    for path, expect_ok in [("/api/inquiries", expect_inq), ("/api/users", expect_users), ("/api/menus", expect_menus)]:
        st = status(op, path)
        want = 200 if expect_ok else 403
        if st == want:
            print(f"  OK  {user} {path} {st}")
        else:
            print(f"  FAIL {user} {path} got {st} want {want}")
            fail += 1

# old accounts must fail (wrong id — any password)
for user in ["cxadmin", "guest_user", "guest_folio"]:
    op, body, err = login(user, "invalid")
    if err and err[0] == 401:
        print(f"  OK  {user} rejected 401")
    else:
        print(f"  FAIL {user} should be 401")
        fail += 1

# manager can list users / update menu name roundtrip
admin_pw = pass_of("ADMIN")
op, body, err = (None, None, "no admin pw") if not admin_pw else login("admin", admin_pw)
if op:
    users = json.loads(op.open(BASE + "/api/users").read().decode())
    names = sorted(u["username"] for u in users)
    if names == ["admin", "guest", "hani", "hwlee"]:
        print(f"  OK  users list {names}")
    else:
        print(f"  FAIL users list {names}")
        fail += 1
    menus = json.loads(op.open(BASE + "/api/menus").read().decode())
    home = next((m for m in menus if m.get("menuId") == "HOME"), None)
    if home and home.get("menuNm"):
        print(f"  OK  menus HOME='{home.get('menuNm')}' auth={home.get('authCds')}")
    else:
        print("  FAIL menus HOME missing")
        fail += 1
    try:
        auths = json.loads(op.open(BASE + "/api/auths").read().decode())
        cds = sorted(a["authCd"] for a in auths)
        if cds == ["01", "02"]:
            print(f"  OK  auths {[(a['authCd'], a['authNm']) for a in auths]}")
        else:
            print(f"  FAIL auths {cds}")
            fail += 1
    except urllib.error.HTTPError as e:
        print(f"  FAIL /api/auths {e.code}")
        fail += 1
else:
    print("  SKIP admin manager checks (set FLOWDESK_PASS_ADMIN)")

sys.exit(1 if fail else 0)
PY
rc=$?
if [ $rc -ne 0 ]; then FAIL=$((FAIL+1)); else PASS=$((PASS+1)); fi

echo "=== headers (optional) ==="
hdr=$(curl -sI "$BASE/login.html" | tr -d '\r')
echo "$hdr" | grep -qi "Content-Security-Policy" && ok "CSP" || bad "CSP missing"
echo "$hdr" | grep -qi "X-Frame-Options" && ok "X-Frame-Options" || bad "X-Frame-Options missing"

echo "=== summary pass=$PASS fail=$FAIL ==="
[ "$FAIL" -eq 0 ]
