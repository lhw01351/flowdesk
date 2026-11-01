/**
 * FlowDesk shell + shared helpers
 * data-page: home | cx | health | remind | access | chatfiles | users | menus
 */
(function () {
  var path = window.location.pathname.replace(/\/[^/]*\.html$/i, '').replace(/\/$/, '');
  var BASE = path || '';

  function href(page) {
    return BASE + '/' + page;
  }

  window.CX_APP_BASE = BASE;
  window.FD_APP_BASE = BASE;
  window.CX_AUTH_BASE = BASE + '/api/auth';
  window.CX_INBODY_BASE = BASE + '/api/inbody';
  window.CX_INQUIRY_BASE = BASE + '/api/inquiries';
  window.CX_ACCESS_BASE = BASE + '/api/access-logs';
  window.CX_CHAT_BASE = BASE + '/api/chat';
  window.FD_USERS_BASE = BASE + '/api/users';
  window.FD_AUTH_BASE = BASE + '/api/auths';
  window.FD_MENUS_BASE = BASE + '/api/menus';
  window.FD_REMINDERS_BASE = BASE + '/api/reminders';
  window.FD_USER = null;

  /** 타인 데이터 대상 선택 가능 — 전체관리자(01). 포폴용 guest 제외 */
  window.fdCanPickOwner = function () {
    var me = window.FD_USER;
    if (!me || !me.username) return false;
    if (String(me.username).toLowerCase() === 'guest') return false;
    return me.authCd === '01';
  };

  /** API URL에 ?user= 대상 붙이기. auth 01만 타인 지정 (guest 제외) */
  window.fdWithOwner = function (baseUrl, path, ownerUser) {
    var url = (baseUrl || '') + (path || '');
    var me = window.FD_USER;
    var selfName = me && me.username;
    if (ownerUser && selfName && ownerUser !== selfName && fdCanPickOwner()) {
      url += (url.indexOf('?') >= 0 ? '&' : '?') + 'user=' + encodeURIComponent(ownerUser);
    }
    return url;
  };

  /** 인바디 API URL (?user= 대상). auth 01만 타인 지정 */
  window.fdInbodyUrl = function (path, ownerUser) {
    return fdWithOwner(CX_INBODY_BASE, path, ownerUser);
  };

  /** 문의 트래커 API URL (?user= 대상) */
  window.fdInquiryUrl = function (path, ownerUser) {
    return fdWithOwner(CX_INQUIRY_BASE, path, ownerUser);
  };

  /**
   * 01 권한용 사용자 콤보 HTML + 변경 콜백.
   * @param usersUrl 선택 — 기본 /api/inbody/users
   * @returns 현재 선택 username
   */
  window.fdMountUserPicker = async function (containerId, storageKey, onChange, usersUrl) {
    var me = window.FD_USER;
    var box = document.getElementById(containerId);
    if (!box) return me && me.username;
    if (!fdCanPickOwner()) {
      box.style.display = 'none';
      box.innerHTML = '';
      try { localStorage.removeItem(storageKey); } catch (e) {}
      return me && me.username;
    }
    box.style.display = '';
    var saved = '';
    try { saved = localStorage.getItem(storageKey) || ''; } catch (e) {}
    var res = await cxApiFetch(usersUrl || (CX_INBODY_BASE + '/users'));
    if (!res.ok) {
      box.innerHTML = '';
      return me.username;
    }
    var users = await res.json();
    var cur = saved;
    if (!users.some(function (u) { return u.username === cur; })) {
      cur = me.username;
    }
    var opts = users.map(function (u) {
      var label = (u.displayName ? u.displayName + ' (' + u.username + ')' : u.username);
      return '<option value="' + cxEscapeHtml(u.username) + '"' +
        (u.username === cur ? ' selected' : '') + '>' + cxEscapeHtml(label) + '</option>';
    }).join('');
    box.innerHTML =
      '<span class="fd-control-label">대상 사용자</span>' +
      '<select id="' + containerId + 'Select" class="fd-user-select">' + opts + '</select>';
    var sel = document.getElementById(containerId + 'Select');
    if (sel) {
      sel.addEventListener('change', function () {
        try { localStorage.setItem(storageKey, sel.value); } catch (e) {}
        if (onChange) onChange(sel.value);
      });
    }
    return cur;
  };

  window.cxEscapeHtml = function (s) {
    if (s == null) return '';
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  };

  /**
   * 긴 텍스트 말줄임 HTML (기본 10자 + …).
   * 데스크톱: hover/focus 시 전체 툴팁, 모바일: 탭으로 툴팁 토글.
   */
  window.cxClipHtml = function (s, maxLen) {
    var full = (s == null || s === '') ? '-' : String(s);
    var n = (maxLen == null || maxLen < 1) ? 10 : maxLen;
    if (full === '-' || full.length <= n) {
      return '<span class="fd-clip fd-clip-plain">' + cxEscapeHtml(full) + '</span>';
    }
    var short = full.slice(0, n) + '…';
    return '<span class="fd-clip" tabindex="0" role="button" aria-label="전체 텍스트 보기" title="' +
      cxEscapeHtml(full) + '">' +
      '<span class="fd-clip-short">' + cxEscapeHtml(short) + '</span>' +
      '<span class="fd-clip-tip" role="tooltip">' + cxEscapeHtml(full) + '</span>' +
      '</span>';
  };

  function fdCloseAllClips(except) {
    document.querySelectorAll('.fd-clip.is-open').forEach(function (el) {
      if (el !== except) el.classList.remove('is-open');
    });
  }

  document.addEventListener('click', function (e) {
    var clip = e.target && e.target.closest ? e.target.closest('.fd-clip:not(.fd-clip-plain)') : null;
    if (clip) {
      var coarse = window.matchMedia && window.matchMedia('(hover: none), (pointer: coarse)').matches;
      if (coarse) {
        e.preventDefault();
        var open = !clip.classList.contains('is-open');
        fdCloseAllClips(clip);
        clip.classList.toggle('is-open', open);
      }
      return;
    }
    fdCloseAllClips(null);
  });

  document.addEventListener('keydown', function (e) {
    if (e.key !== 'Escape') return;
    fdCloseAllClips(null);
  });

  window.cxToast = function (msg) {
    var el = document.getElementById('fdToast');
    if (!el) {
      el = document.createElement('div');
      el.id = 'fdToast';
      el.className = 'fd-toast';
      document.body.appendChild(el);
    }
    el.textContent = msg || '';
    el.classList.add('show');
    clearTimeout(el._t);
    el._t = setTimeout(function () { el.classList.remove('show'); }, 2200);
  };

  /**
   * 커스텀 확인 다이얼로그 (window.confirm 대체)
   * @returns {Promise<boolean>}
   */
  window.cxConfirm = function (opts) {
    opts = opts || {};
    var title = opts.title || '확인';
    var message = opts.message || '정말로 삭제하시겠습니까?';
    var okText = opts.okText || '확인';
    var cancelText = opts.cancelText || '취소';
    var danger = opts.danger !== false;

    return new Promise(function (resolve) {
      var existing = document.getElementById('fdConfirmBackdrop');
      if (existing) existing.remove();

      var backdrop = document.createElement('div');
      backdrop.id = 'fdConfirmBackdrop';
      backdrop.className = 'fd-confirm-backdrop';
      backdrop.setAttribute('role', 'dialog');
      backdrop.setAttribute('aria-modal', 'true');
      backdrop.innerHTML =
        '<div class="fd-confirm">' +
          '<div class="fd-confirm-head">' +
            '<div class="fd-confirm-title">' + cxEscapeHtml(title) + '</div>' +
          '</div>' +
          '<div class="fd-confirm-body">' + cxEscapeHtml(message).replace(/\n/g, '<br>') + '</div>' +
          '<div class="fd-confirm-actions">' +
            '<button type="button" class="fd-btn fd-btn-ghost fd-btn-sm" data-act="cancel">' +
              cxEscapeHtml(cancelText) + '</button>' +
            '<button type="button" class="fd-btn fd-btn-sm ' +
              (danger ? 'fd-btn-danger' : 'fd-btn-primary') + '" data-act="ok">' +
              cxEscapeHtml(okText) + '</button>' +
          '</div>' +
        '</div>';
      document.body.appendChild(backdrop);
      requestAnimationFrame(function () { backdrop.classList.add('show'); });

      function close(result) {
        backdrop.classList.remove('show');
        setTimeout(function () {
          if (backdrop.parentNode) backdrop.parentNode.removeChild(backdrop);
        }, 160);
        document.removeEventListener('keydown', onKey);
        resolve(!!result);
      }
      function onKey(e) {
        if (e.key === 'Escape') close(false);
        if (e.key === 'Enter') close(true);
      }
      backdrop.addEventListener('click', function (e) {
        if (e.target === backdrop) close(false);
      });
      backdrop.querySelector('[data-act="cancel"]').addEventListener('click', function () { close(false); });
      backdrop.querySelector('[data-act="ok"]').addEventListener('click', function () { close(true); });
      document.addEventListener('keydown', onKey);
      setTimeout(function () {
        var okBtn = backdrop.querySelector('[data-act="ok"]');
        if (okBtn) okBtn.focus();
      }, 30);
    });
  };

  window.cxApiFetch = function (url, options) {
    options = options || {};
    options.credentials = 'same-origin';
    return fetch(url, options).then(function (res) {
      if (res.status === 401) {
        location.replace(BASE + '/login.html');
        throw new Error('로그인이 필요합니다.');
      }
      if (res.status === 403) {
        location.replace(BASE + '/home.html');
        throw new Error('권한이 없습니다.');
      }
      return res;
    });
  };

  window.cxLogout = async function () {
    try {
      await window.cxApiFetch(window.CX_AUTH_BASE + '/logout', { method: 'POST' });
    } catch (e) {}
    location.replace(BASE + '/login.html');
  };

  window.cxRequireAuth = async function () {
    var res = await window.cxApiFetch(window.CX_AUTH_BASE + '/me');
    if (!res.ok) return null;
    var me = await res.json();
    window.FD_USER = me;
    return me;
  };

  window.cxToday = function () {
    return cxPadDate(new Date());
  };

  window.cxPadDate = function (d) {
    var y = d.getFullYear();
    var m = String(d.getMonth() + 1).padStart(2, '0');
    var day = String(d.getDate()).padStart(2, '0');
    return y + '-' + m + '-' + day;
  };

  window.cxLastNDays = function (n) {
    var end = new Date();
    var start = new Date();
    start.setDate(end.getDate() - (n - 1));
    return { from: cxPadDate(start), to: cxPadDate(end) };
  };

  window.cxInRange = function (dateStr, from, to) {
    if (!dateStr) return false;
    var d = String(dateStr).substring(0, 10);
    return d >= from && d <= to;
  };

  /** 클라이언트 페이징 슬라이스 */
  window.fdPageSlice = function (list, page, size) {
    var arr = list || [];
    var sz = size > 0 ? size : 15;
    var total = arr.length;
    var pages = Math.max(1, Math.ceil(total / sz) || 1);
    var p = Math.min(Math.max(1, page || 1), pages);
    var start = (p - 1) * sz;
    return {
      page: p,
      pages: pages,
      total: total,
      size: sz,
      start: start,
      items: arr.slice(start, start + sz)
    };
  };

  /**
   * 페이저 HTML 렌더 + 클릭 바인딩
   * @param containerElOrId DOM 또는 id
   * @param page 현재 페이지
   * @param pages 총 페이지
   * @param onGo function(page)
   * @param opts { total, size } 선택 — 하단에 "n–m / 전체" 표시
   */
  window.fdRenderPager = function (containerElOrId, page, pages, onGo, opts) {
    var el = typeof containerElOrId === 'string'
      ? document.getElementById(containerElOrId)
      : containerElOrId;
    if (!el) return;
    opts = opts || {};
    if (!pages || pages <= 1) {
      if (opts.total != null && opts.total > 0) {
        el.innerHTML = '<div class="fd-page-info">전체 ' + opts.total + '건</div>';
      } else {
        el.innerHTML = '';
      }
      return;
    }
    var cur = Math.min(Math.max(1, page || 1), pages);
    var nums = [];
    if (pages <= 7) {
      for (var i = 1; i <= pages; i++) nums.push(i);
    } else {
      nums.push(1);
      var left = Math.max(2, cur - 1);
      var right = Math.min(pages - 1, cur + 1);
      if (left > 2) nums.push('...');
      for (var j = left; j <= right; j++) nums.push(j);
      if (right < pages - 1) nums.push('...');
      nums.push(pages);
    }
    var html = '<div class="fd-pagination">';
    html += '<button type="button" class="fd-page-btn" data-page="1"' + (cur === 1 ? ' disabled' : '') + '>«</button>';
    html += '<button type="button" class="fd-page-btn" data-page="' + (cur - 1) + '"' + (cur === 1 ? ' disabled' : '') + '>‹</button>';
    nums.forEach(function (n) {
      if (n === '...') {
        html += '<span class="fd-page-ellipsis">…</span>';
      } else {
        html += '<button type="button" class="fd-page-btn' + (n === cur ? ' active' : '') +
          '" data-page="' + n + '">' + n + '</button>';
      }
    });
    html += '<button type="button" class="fd-page-btn" data-page="' + (cur + 1) + '"' + (cur === pages ? ' disabled' : '') + '>›</button>';
    html += '<button type="button" class="fd-page-btn" data-page="' + pages + '"' + (cur === pages ? ' disabled' : '') + '>»</button>';
    html += '</div>';
    if (opts.total != null && opts.size) {
      var fromN = (cur - 1) * opts.size + 1;
      var toN = Math.min(cur * opts.size, opts.total);
      html += '<div class="fd-page-info">' + fromN + '–' + toN + ' / 전체 ' + opts.total + '건</div>';
    }
    el.innerHTML = html;
    el.querySelectorAll('[data-page]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        if (btn.disabled) return;
        var next = parseInt(btn.getAttribute('data-page'), 10);
        if (!next || next === cur) return;
        if (onGo) onGo(next);
      });
    });
  };

  window.cxGetGeo = function () {
    return new Promise(function (resolve) {
      if (!navigator.geolocation) {
        resolve({ gpsStatus: '미지원' });
        return;
      }
      var done = false;
      var timer = setTimeout(function () {
        if (done) return;
        done = true;
        resolve({ gpsStatus: '타임아웃' });
      }, 4000);
      navigator.geolocation.getCurrentPosition(
        function (pos) {
          if (done) return;
          done = true;
          clearTimeout(timer);
          resolve({
            latitude: pos.coords.latitude,
            longitude: pos.coords.longitude,
            accuracy: pos.coords.accuracy,
            gpsStatus: 'OK'
          });
        },
        function (err) {
          if (done) return;
          done = true;
          clearTimeout(timer);
          var status = '거부/실패';
          if (err && err.code === 1) status = '권한거부';
          if (err && err.code === 2) status = '위치불가';
          if (err && err.code === 3) status = '타임아웃';
          resolve({ gpsStatus: status });
        },
        { enableHighAccuracy: false, timeout: 3500, maximumAge: 60000 }
      );
    });
  };

  window.cxTrackAccess = async function (pagePath, menuNm) {
    try {
      var key = 'fd-access-tracked:' + pagePath;
      if (sessionStorage.getItem(key)) return;
      var geo = await cxGetGeo();
      await cxApiFetch(CX_ACCESS_BASE, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          eventType: 'PAGE',
          pagePath: pagePath || location.pathname,
          menuNm: menuNm || undefined,
          latitude: geo.latitude,
          longitude: geo.longitude,
          accuracy: geo.accuracy,
          gpsStatus: geo.gpsStatus
        })
      });
      sessionStorage.setItem(key, '1');
    } catch (e) {}
  };

  var NAV_OPEN_KEY = 'fd-nav-open';

  function loadOpenGroups() {
    try { return JSON.parse(localStorage.getItem(NAV_OPEN_KEY) || '{}') || {}; }
    catch (e) { return {}; }
  }
  function saveOpenGroups(map) {
    try { localStorage.setItem(NAV_OPEN_KEY, JSON.stringify(map)); } catch (e) {}
  }

  function renderNavNode(node, openMap, activeKey) {
    var children = node.children || [];
    var isLeaf = node.isLeaf !== false && children.length === 0;
    if (node.isLeaf === false) isLeaf = false;

    if (isLeaf) {
      var page = node.page || ((node.key || '') + '.html');
      var ico = node.icon || node.key || '';
      return '<a href="' + href(page) + '" data-nav="' + cxEscapeHtml(node.key) + '" class="app-nav-link app-nav-leaf">' +
        '<span class="app-nav-ico">' + cxEscapeHtml(ico) + '</span>' +
        '<span class="app-nav-text">' + cxEscapeHtml(node.name || node.key) + '</span></a>';
    }

    var id = node.menuId || node.key;
    var open = !!openMap[id];
    // 활성 페이지의 조상은 자동 펼침
    if (activeKey && containsKey(node, activeKey)) open = true;
    var html = '<div class="app-nav-group' + (open ? ' open' : '') + '" data-group="' + cxEscapeHtml(id) + '">' +
      '<button type="button" class="app-nav-toggle" aria-expanded="' + (open ? 'true' : 'false') + '">' +
        '<span class="app-nav-ico">' + cxEscapeHtml(node.icon || '▾') + '</span>' +
        '<span class="app-nav-text">' + cxEscapeHtml(node.name || node.key) + '</span>' +
        '<span class="app-nav-chevron">▾</span>' +
      '</button>' +
      '<div class="app-nav-children">';
    children.forEach(function (ch) {
      html += renderNavNode(ch, openMap, activeKey);
    });
    html += '</div></div>';
    return html;
  }

  function containsKey(node, key) {
    if (node.key === key) return true;
    var ch = node.children || [];
    for (var i = 0; i < ch.length; i++) {
      if (containsKey(ch[i], key)) return true;
    }
    return false;
  }

  function buildNavHtml(menuTree, menuList, menus, activeKey) {
    var html =
      '<div class="app-brand">' +
        '<div class="app-brand-label">MY WORKSPACE</div>' +
        '<div class="app-brand-title">FlowDesk</div>' +
        '<div class="app-brand-sub">업무와 루틴을 한곳에서</div>' +
      '</div>' +
      '<nav class="app-nav">' +
        '<div class="app-nav-section">Menu</div>';

    var openMap = loadOpenGroups();
    if (menuTree && menuTree.length) {
      menuTree.forEach(function (node) {
        html += renderNavNode(node, openMap, activeKey);
      });
    } else if (menuList && menuList.length) {
      menuList.forEach(function (item) {
        html += renderNavNode(
          { key: item.key, name: item.name, page: item.page, icon: item.icon, isLeaf: true, children: [] },
          openMap, activeKey
        );
      });
    } else {
      menus = menus || {};
      if (menus.home !== false) {
        html += '<a href="' + href('home.html') + '" data-nav="home" class="app-nav-link"><span class="app-nav-ico">홈</span>홈</a>';
      }
    }

    html += '</nav>';
    return html;
  }

  function formatSessionRemain(sec) {
    var s = Math.max(0, Math.floor(sec || 0));
    if (s <= 0) return '세션 만료';
    var h = Math.floor(s / 3600);
    var m = Math.floor((s % 3600) / 60);
    if (h > 0) return '남은 ' + h + '시간 ' + m + '분';
    if (m > 0) return '남은 ' + m + '분';
    return '남은 1분 미만';
  }

  function formatSessionMax(sec) {
    var s = Math.max(0, Math.floor(sec || 0));
    var h = Math.floor(s / 3600);
    var m = Math.floor((s % 3600) / 60);
    if (h > 0 && m > 0) return '유지 ' + h + '시간 ' + m + '분';
    if (h > 0) return '유지 ' + h + '시간';
    if (m > 0) return '유지 ' + m + '분';
    return '';
  }

  function initialFromName(me) {
    var raw = (me && (me.displayName || me.username)) || '?';
    return String(raw).trim().charAt(0).toUpperCase() || '?';
  }

  function avatarSrc(me) {
    if (!me || !me.avatarUrl) return '';
    var u = String(me.avatarUrl);
    if (u.indexOf('http') === 0 || u.indexOf(BASE) === 0) return u;
    return BASE + u;
  }

  function buildUserAreaHtml(me) {
    var name = (me && me.displayName) ? me.displayName : ((me && me.username) || '');
    var sub = (me && me.username) ? me.username : '';
    if (me && me.authCd) sub += (sub ? ' · ' : '') + me.authCd;
    var remain = formatSessionRemain(me && me.sessionRemainingSec);
    var src = avatarSrc(me);
    var avatarInner = src
      ? '<img class="app-user-avatar-img" src="' + cxEscapeHtml(src) + '" alt="">'
      : '<span class="app-user-avatar-letter">' + cxEscapeHtml(initialFromName(me)) + '</span>';
    return (
      '<div class="app-user-area" id="appUserArea">' +
        '<button type="button" class="app-user-btn" id="appUserBtn" aria-haspopup="true" aria-expanded="false">' +
          '<span class="app-user-avatar" id="appUserAvatar">' + avatarInner + '</span>' +
          '<span class="app-user-meta">' +
            '<span class="app-user-name" id="appUserName">' + cxEscapeHtml(name) + '</span>' +
            '<span class="app-user-session" id="appUserSession">' + cxEscapeHtml(remain) + '</span>' +
          '</span>' +
          '<span class="app-user-caret" aria-hidden="true">▾</span>' +
        '</button>' +
        '<div class="app-user-drop" id="appUserDrop" hidden>' +
          '<div class="app-user-drop-head">' +
            '<div class="app-user-drop-name" id="appUserDropName">' + cxEscapeHtml(name) + '</div>' +
            '<div class="app-user-drop-sub" id="appUserDropSub">' + cxEscapeHtml(sub) + '</div>' +
            '<div class="app-user-drop-session" id="appUserDropSession">' +
              cxEscapeHtml(remain) +
              (me && me.sessionMaxInactiveSec
                ? ' · ' + cxEscapeHtml(formatSessionMax(me.sessionMaxInactiveSec))
                : '') +
            '</div>' +
          '</div>' +
          '<button type="button" class="app-user-logout" id="shellLogoutBtn">로그아웃</button>' +
        '</div>' +
      '</div>'
    );
  }

  function bindUserArea(me) {
    var area = document.getElementById('appUserArea');
    var btn = document.getElementById('appUserBtn');
    var drop = document.getElementById('appUserDrop');
    var logoutBtn = document.getElementById('shellLogoutBtn');
    if (!area || !btn || !drop) return;

    var sessionEndsAt = Date.now() + Math.max(0, (me && me.sessionRemainingSec) || 0) * 1000;
    var maxSec = (me && me.sessionMaxInactiveSec) || 0;

    function renderSession() {
      var remain = Math.max(0, Math.floor((sessionEndsAt - Date.now()) / 1000));
      var label = formatSessionRemain(remain);
      var el = document.getElementById('appUserSession');
      var dropEl = document.getElementById('appUserDropSession');
      if (el) el.textContent = label;
      if (dropEl) {
        dropEl.textContent = label + (maxSec ? ' · ' + formatSessionMax(maxSec) : '');
      }
      if (remain <= 0) {
        location.replace(BASE + '/login.html');
      }
    }

    function setOpen(open) {
      drop.hidden = !open;
      btn.setAttribute('aria-expanded', open ? 'true' : 'false');
      area.classList.toggle('open', !!open);
    }

    btn.addEventListener('click', function (e) {
      e.stopPropagation();
      setOpen(drop.hidden);
    });
    document.addEventListener('click', function (e) {
      if (!area.contains(e.target)) setOpen(false);
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') setOpen(false);
    });
    if (logoutBtn) logoutBtn.addEventListener('click', window.cxLogout);

    renderSession();
    setInterval(renderSession, 30000);

    // 주기적으로 세션·아바타 동기화 (활동 시 남은 시간 갱신)
    setInterval(async function () {
      try {
        var res = await fetch(window.CX_AUTH_BASE + '/me', { credentials: 'same-origin' });
        if (res.status === 401) {
          location.replace(BASE + '/login.html');
          return;
        }
        if (!res.ok) return;
        var next = await res.json();
        if (!next || !next.ok) return;
        window.FD_USER = next;
        sessionEndsAt = Date.now() + Math.max(0, next.sessionRemainingSec || 0) * 1000;
        maxSec = next.sessionMaxInactiveSec || maxSec;
        var nameEl = document.getElementById('appUserName');
        var dropName = document.getElementById('appUserDropName');
        var dropSub = document.getElementById('appUserDropSub');
        var avatar = document.getElementById('appUserAvatar');
        var nm = next.displayName || next.username || '';
        var sub = next.username || '';
        if (next.authCd) sub += (sub ? ' · ' : '') + next.authCd;
        if (nameEl) nameEl.textContent = nm;
        if (dropName) dropName.textContent = nm;
        if (dropSub) dropSub.textContent = sub;
        if (avatar) {
          var src = avatarSrc(next);
          avatar.innerHTML = src
            ? '<img class="app-user-avatar-img" src="' + cxEscapeHtml(src) + '?t=' + Date.now() + '" alt="">'
            : '<span class="app-user-avatar-letter">' + cxEscapeHtml(initialFromName(next)) + '</span>';
        }
        renderSession();
      } catch (err) {}
    }, 60000);
  }

  function bindNavToggles(nav) {
    nav.querySelectorAll('.app-nav-toggle').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var group = btn.closest('.app-nav-group');
        if (!group) return;
        group.classList.toggle('open');
        var open = group.classList.contains('open');
        btn.setAttribute('aria-expanded', open ? 'true' : 'false');
        var id = group.getAttribute('data-group');
        var map = loadOpenGroups();
        if (open) map[id] = true; else delete map[id];
        saveOpenGroups(map);
      });
    });
  }

  function hasMenu(menus, menuList, key) {
    if (menuList && menuList.length) {
      return menuList.some(function (m) { return m.key === key; });
    }
    return !!(menus && menus[key]);
  }

  async function mountShell() {
    if (document.querySelector('.app-shell')) return;

    var page = document.body.getAttribute('data-page') || 'home';
    var me = null;
    try {
      me = await cxRequireAuth();
    } catch (e) {
      return;
    }
    var menus = (me && me.menus) || {};
    var menuList = (me && me.menuList) || [];
    var menuTree = (me && me.menuTree) || [];

    if (page !== 'home' && !hasMenu(menus, menuList, page)) {
      location.replace(href('home.html'));
      return;
    }

    var nav = document.createElement('aside');
    nav.className = 'app-sidebar';
    nav.id = 'appSidebar';
    nav.innerHTML = buildNavHtml(menuTree, menuList, menus, page);

    var main = document.createElement('div');
    main.className = 'app-main';

    var topbar = document.createElement('div');
    topbar.className = 'app-topbar';
    topbar.innerHTML =
      '<button type="button" class="app-menu-btn" id="appMenuBtn" aria-label="메뉴 열기">☰</button>' +
      '<div class="app-topbar-title">FlowDesk</div>' +
      '<div class="app-topbar-spacer"></div>' +
      buildUserAreaHtml(me);

    var backdrop = document.createElement('div');
    backdrop.className = 'app-nav-backdrop';
    backdrop.id = 'appNavBackdrop';

    var shell = document.createElement('div');
    shell.className = 'app-shell';

    Array.prototype.slice.call(document.body.childNodes).forEach(function (n) {
      main.appendChild(n);
    });

    main.insertBefore(topbar, main.firstChild);
    shell.appendChild(nav);
    shell.appendChild(main);
    shell.appendChild(backdrop);
    document.body.appendChild(shell);

    function setNavOpen(open) {
      shell.classList.toggle('nav-open', !!open);
      document.body.classList.toggle('nav-lock', !!open);
    }
    document.getElementById('appMenuBtn').addEventListener('click', function () {
      setNavOpen(!shell.classList.contains('nav-open'));
    });
    backdrop.addEventListener('click', function () { setNavOpen(false); });
    nav.querySelectorAll('a.app-nav-link, a.app-nav-leaf, a[data-nav]').forEach(function (a) {
      a.addEventListener('click', function () { setNavOpen(false); });
    });

    bindNavToggles(nav);
    bindUserArea(me);

    var active = nav.querySelector('[data-nav="' + page + '"]');
    if (active) {
      active.classList.add('active');
      var group = active.closest('.app-nav-group');
      if (group) {
        group.classList.add('open');
        var map = loadOpenGroups();
        map[group.getAttribute('data-group')] = true;
        saveOpenGroups(map);
      }
    }

    var pageMap = {
      home: { path: '/home.html', name: '홈' },
      cx: { path: '/index.html', name: '문의 트래커' },
      health: { path: '/health.html', name: '인바디 케어' },
      remind: { path: '/remind.html', name: '리마인드' },
      access: { path: '/access.html', name: '로그인 로그' },
      chatfiles: { path: '/chat-files.html', name: '채팅 첨부' },
      users: { path: '/users.html', name: '회원 관리' },
      menus: { path: '/menus.html', name: '메뉴 관리' }
    };
    var track = pageMap[page] || { path: location.pathname, name: '' };
    if (menuList && menuList.length) {
      for (var i = 0; i < menuList.length; i++) {
        if (menuList[i] && menuList[i].key === page && menuList[i].name) {
          track.name = menuList[i].name;
          break;
        }
      }
    }
    cxTrackAccess(track.path, track.name);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', mountShell);
  } else {
    mountShell();
  }
})();
