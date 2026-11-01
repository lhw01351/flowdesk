/**
 * 프로필 사진 원형 크롭 팝업 (S-Gate 인사관리 내정보 프로필사진변경 UX 참고)
 * window.openProfileCrop({ onConfirm(blob, dataUrl), onCancel? })
 */
(function () {
  var OUT_SIZE = 400;
  var VIEW_W = 640;
  var VIEW_H = 420;
  var CIRCLE_R = 130;

  var state = {
    open: false,
    img: null,
    objectUrl: null,
    scale: 1,
    minScale: 1,
    ox: 0,
    oy: 0,
    dragging: false,
    lastX: 0,
    lastY: 0,
    onConfirm: null,
    onCancel: null
  };

  function ensureDom() {
    if (document.getElementById('fdProfileCropBackdrop')) return;
    var el = document.createElement('div');
    el.id = 'fdProfileCropBackdrop';
    el.className = 'fd-crop-backdrop';
    el.innerHTML =
      '<div class="fd-crop-dialog" role="dialog" aria-modal="true" aria-labelledby="fdCropTitle">' +
        '<div class="fd-crop-head">' +
          '<div class="fd-crop-title" id="fdCropTitle">프로필 사진 변경</div>' +
          '<button type="button" class="fd-crop-x" id="fdCropCloseX" aria-label="닫기">×</button>' +
        '</div>' +
        '<div class="fd-crop-body">' +
          '<div class="fd-crop-stage" id="fdCropStage">' +
            '<canvas id="fdCropCanvas" width="' + VIEW_W + '" height="' + VIEW_H + '"></canvas>' +
            '<div class="fd-crop-hint" id="fdCropHint">사진을 선택해주세요</div>' +
          '</div>' +
          '<div class="fd-crop-zoom-row">' +
            '<span>축소</span>' +
            '<input type="range" id="fdCropZoom" min="100" max="300" value="100" disabled>' +
            '<span>확대</span>' +
          '</div>' +
          '<div class="fd-crop-guide">원 안에 얼굴이 오도록 드래그·확대해 주세요.</div>' +
        '</div>' +
        '<div class="fd-crop-actions">' +
          '<input type="file" id="fdCropFile" accept="image/png,image/jpeg,image/gif,image/webp" hidden>' +
          '<button type="button" class="fd-btn fd-btn-primary" id="fdCropPick">사진변경</button>' +
          '<button type="button" class="fd-btn fd-btn-primary" id="fdCropOk">확인</button>' +
          '<button type="button" class="fd-btn" id="fdCropClose">닫기</button>' +
        '</div>' +
      '</div>';
    document.body.appendChild(el);

    var canvas = document.getElementById('fdCropCanvas');
    var zoom = document.getElementById('fdCropZoom');
    var file = document.getElementById('fdCropFile');

    document.getElementById('fdCropPick').addEventListener('click', function () {
      file.value = '';
      file.click();
    });
    file.addEventListener('change', function () {
      if (file.files && file.files[0]) loadFile(file.files[0]);
    });
    document.getElementById('fdCropOk').addEventListener('click', confirmCrop);
    document.getElementById('fdCropClose').addEventListener('click', closeCrop);
    document.getElementById('fdCropCloseX').addEventListener('click', closeCrop);
    el.addEventListener('click', function (e) {
      if (e.target === el) closeCrop();
    });
    zoom.addEventListener('input', function () {
      if (!state.img) return;
      var next = (Number(zoom.value) / 100) * state.minScale;
      setScale(next);
      paint();
    });

    canvas.addEventListener('mousedown', onPointerDown);
    window.addEventListener('mousemove', onPointerMove);
    window.addEventListener('mouseup', onPointerUp);
    canvas.addEventListener('touchstart', onTouchStart, { passive: false });
    canvas.addEventListener('touchmove', onTouchMove, { passive: false });
    canvas.addEventListener('touchend', onPointerUp);
    canvas.addEventListener('wheel', onWheel, { passive: false });

    document.addEventListener('keydown', function (e) {
      if (!state.open) return;
      if (e.key === 'Escape') closeCrop();
    });
  }

  function canvasScale() {
    var canvas = document.getElementById('fdCropCanvas');
    var rect = canvas.getBoundingClientRect();
    return rect.width > 0 ? canvas.width / rect.width : 1;
  }

  function onPointerDown(e) {
    if (!state.img) return;
    state.dragging = true;
    state.lastX = e.clientX;
    state.lastY = e.clientY;
  }
  function onPointerMove(e) {
    if (!state.dragging || !state.img) return;
    var k = canvasScale();
    state.ox += (e.clientX - state.lastX) * k;
    state.oy += (e.clientY - state.lastY) * k;
    state.lastX = e.clientX;
    state.lastY = e.clientY;
    clampOffset();
    paint();
  }
  function onPointerUp() {
    state.dragging = false;
  }
  function onTouchStart(e) {
    if (!state.img || !e.touches.length) return;
    e.preventDefault();
    state.dragging = true;
    state.lastX = e.touches[0].clientX;
    state.lastY = e.touches[0].clientY;
  }
  function onTouchMove(e) {
    if (!state.dragging || !state.img || !e.touches.length) return;
    e.preventDefault();
    var k = canvasScale();
    state.ox += (e.touches[0].clientX - state.lastX) * k;
    state.oy += (e.touches[0].clientY - state.lastY) * k;
    state.lastX = e.touches[0].clientX;
    state.lastY = e.touches[0].clientY;
    clampOffset();
    paint();
  }
  function onWheel(e) {
    if (!state.img) return;
    e.preventDefault();
    var factor = e.deltaY < 0 ? 1.06 : 0.94;
    setScale(state.scale * factor);
    var zoom = document.getElementById('fdCropZoom');
    zoom.value = String(Math.round((state.scale / state.minScale) * 100));
    paint();
  }

  function setScale(next) {
    var cx = VIEW_W / 2;
    var cy = VIEW_H / 2;
    var imgCx = (cx - state.ox) / state.scale;
    var imgCy = (cy - state.oy) / state.scale;
    state.scale = Math.max(state.minScale, Math.min(state.minScale * 3, next));
    state.ox = cx - imgCx * state.scale;
    state.oy = cy - imgCy * state.scale;
    clampOffset();
  }

  function clampOffset() {
    if (!state.img) return;
    var r = CIRCLE_R;
    var cx = VIEW_W / 2;
    var cy = VIEW_H / 2;
    var w = state.img.naturalWidth * state.scale;
    var h = state.img.naturalHeight * state.scale;
    // 원이 이미지 밖으로 나가지 않도록
    state.ox = Math.min(cx - r, Math.max(cx + r - w, state.ox));
    state.oy = Math.min(cy - r, Math.max(cy + r - h, state.oy));
  }

  function paint() {
    var canvas = document.getElementById('fdCropCanvas');
    var ctx = canvas.getContext('2d');
    var hint = document.getElementById('fdCropHint');
    ctx.clearRect(0, 0, VIEW_W, VIEW_H);
    ctx.fillStyle = '#1a1c28';
    ctx.fillRect(0, 0, VIEW_W, VIEW_H);

    if (!state.img) {
      hint.style.display = '';
      return;
    }
    hint.style.display = 'none';

    ctx.drawImage(
      state.img,
      state.ox,
      state.oy,
      state.img.naturalWidth * state.scale,
      state.img.naturalHeight * state.scale
    );

    // 원 밖 딤
    ctx.save();
    ctx.fillStyle = 'rgba(10,12,20,0.58)';
    ctx.beginPath();
    ctx.rect(0, 0, VIEW_W, VIEW_H);
    ctx.arc(VIEW_W / 2, VIEW_H / 2, CIRCLE_R, 0, Math.PI * 2, true);
    ctx.fill('evenodd');
    ctx.restore();

    // 원 테두리
    ctx.beginPath();
    ctx.arc(VIEW_W / 2, VIEW_H / 2, CIRCLE_R, 0, Math.PI * 2);
    ctx.strokeStyle = 'rgba(255,255,255,0.92)';
    ctx.lineWidth = 2;
    ctx.stroke();
  }

  function loadFile(file) {
    if (!file || !file.type || file.type.indexOf('image/') !== 0) {
      if (window.cxToast) cxToast('이미지 파일만 선택할 수 있습니다.');
      return;
    }
    if (state.objectUrl) {
      try { URL.revokeObjectURL(state.objectUrl); } catch (e) {}
    }
    state.objectUrl = URL.createObjectURL(file);
    var img = new Image();
    img.onload = function () {
      state.img = img;
      fitImage();
      document.getElementById('fdCropZoom').disabled = false;
      document.getElementById('fdCropZoom').value = '100';
      paint();
    };
    img.onerror = function () {
      if (window.cxToast) cxToast('이미지를 불러오지 못했습니다.');
    };
    img.src = state.objectUrl;
  }

  function fitImage() {
    var img = state.img;
    var diameter = CIRCLE_R * 2;
    // 원이 이미지로 가득 차도록 최소 배율
    state.minScale = Math.max(diameter / img.naturalWidth, diameter / img.naturalHeight);
    state.scale = state.minScale;
    state.ox = (VIEW_W - img.naturalWidth * state.scale) / 2;
    state.oy = (VIEW_H - img.naturalHeight * state.scale) / 2;
    clampOffset();
  }

  function confirmCrop() {
    if (!state.img) {
      if (window.cxToast) cxToast('사진을 먼저 선택해주세요.');
      return;
    }
    var sx = (VIEW_W / 2 - CIRCLE_R - state.ox) / state.scale;
    var sy = (VIEW_H / 2 - CIRCLE_R - state.oy) / state.scale;
    var sw = (CIRCLE_R * 2) / state.scale;
    var sh = sw;

    var out = document.createElement('canvas');
    out.width = OUT_SIZE;
    out.height = OUT_SIZE;
    var ctx = out.getContext('2d');
    ctx.imageSmoothingEnabled = true;
    ctx.imageSmoothingQuality = 'high';
    ctx.drawImage(state.img, sx, sy, sw, sh, 0, 0, OUT_SIZE, OUT_SIZE);

    var dataUrl = out.toDataURL('image/jpeg', 0.92);
    out.toBlob(function (blob) {
      if (!blob) {
        if (window.cxToast) cxToast('크롭 결과 생성에 실패했습니다.');
        return;
      }
      var cb = state.onConfirm;
      closeCrop(true);
      if (cb) cb(blob, dataUrl);
    }, 'image/jpeg', 0.92);
  }

  function closeCrop(fromConfirm) {
    var backdrop = document.getElementById('fdProfileCropBackdrop');
    if (backdrop) backdrop.classList.remove('show');
    state.open = false;
    state.dragging = false;
    state.img = null;
    if (state.objectUrl) {
      try { URL.revokeObjectURL(state.objectUrl); } catch (e) {}
      state.objectUrl = null;
    }
    var zoom = document.getElementById('fdCropZoom');
    if (zoom) {
      zoom.value = '100';
      zoom.disabled = true;
    }
    var file = document.getElementById('fdCropFile');
    if (file) file.value = '';
    paint();
    if (!fromConfirm && state.onCancel) state.onCancel();
    state.onConfirm = null;
    state.onCancel = null;
  }

  function openCrop(opts) {
    opts = opts || {};
    ensureDom();
    state.onConfirm = opts.onConfirm || null;
    state.onCancel = opts.onCancel || null;
    state.open = true;
    var backdrop = document.getElementById('fdProfileCropBackdrop');
    backdrop.classList.add('show');
    paint();
    // S-Gate처럼 열자마자 파일 선택
    setTimeout(function () {
      var file = document.getElementById('fdCropFile');
      if (file) {
        file.value = '';
        file.click();
      }
    }, 40);
  }

  window.openProfileCrop = openCrop;
})();
