/* Client-side helpers. Server-side validation always runs too; this only gives faster feedback. */
(function () {
  'use strict';
  function $all(sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); }

  /* ---- Field validation driven by data-* attributes ----
     data-pattern   : regex source (taken from the server's Patterns class)
     data-msg       : message when the pattern fails
     data-required  : message when empty
     data-match     : id of another field that must have the same value
  */
  function errBox(field) {
    var n = field.nextElementSibling;
    while (n && !(n.classList && n.classList.contains('err'))) {
      if (n.tagName === 'INPUT' || n.tagName === 'SELECT' || n.tagName === 'TEXTAREA') break;
      n = n.nextElementSibling;
    }
    if (n && n.classList && n.classList.contains('err')) return n;
    var d = document.createElement('div');
    d.className = 'err js-err';
    field.parentNode.insertBefore(d, field.nextSibling);
    return d;
  }
  function check(field) {
    var v = field.value;
    var msg = '';
    if (field.dataset.required && v.trim() === '') msg = field.dataset.required;
    else if (v !== '' && field.dataset.pattern && !(new RegExp(field.dataset.pattern)).test(v)) msg = field.dataset.msg || 'Invalid value.';
    else if (field.dataset.match) {
      var other = document.getElementById(field.dataset.match);
      if (other && v !== other.value) msg = field.dataset.msg || 'Values do not match.';
    }
    var box = errBox(field);
    box.textContent = msg;
    field.classList.toggle('invalid', !!msg);
    return msg === '';
  }
  function validateForm(form, quiet) {
    var ok = true;
    $all('[data-pattern],[data-required],[data-match]', form).forEach(function (f) {
      if (f.offsetParent === null || f.disabled) return;      // hidden sections are skipped
      var good = quiet ? quietCheck(f) : check(f);
      if (!good) ok = false;
    });
    return ok;
  }
  function quietCheck(field) {
    var v = field.value;
    if (field.dataset.required && v.trim() === '') return false;
    if (v !== '' && field.dataset.pattern && !(new RegExp(field.dataset.pattern)).test(v)) return false;
    if (field.dataset.match) { var o = document.getElementById(field.dataset.match); if (o && v !== o.value) return false; }
    return true;
  }

  $all('form[data-validate]').forEach(function (form) {
    var submit = form.querySelector('[data-enable-when-valid]');
    function refresh() { if (submit) submit.disabled = !validateForm(form, true); }
    $all('input,select,textarea', form).forEach(function (f) {
      f.addEventListener('blur', function () { if (f.dataset.pattern || f.dataset.required || f.dataset.match) check(f); });
      f.addEventListener('input', function () {
        if (f.classList.contains('invalid')) check(f);
        if (f.dataset.pattern || f.dataset.required || f.dataset.match) refresh();
      });
      f.addEventListener('change', refresh);
    });
    form.addEventListener('submit', function (e) {
      if (!validateForm(form, false)) { e.preventDefault(); var bad = form.querySelector('.invalid'); if (bad) bad.focus(); }
    });
    form.addEventListener('reset', function () {
      setTimeout(function () {
        $all('.invalid', form).forEach(function (f) { f.classList.remove('invalid'); });
        $all('.err', form).forEach(function (e) { e.textContent = ''; });
        refresh();
      }, 0);
    });
    refresh();
  });

  /* ---- Reset buttons that must also clear server-rendered values ---- */
  $all('[data-clear-form]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var form = btn.closest('form');
      $all('input[type=text],input[type=password],input[type=email],input[type=number],input[type=date],input[type=datetime-local],textarea', form)
        .forEach(function (f) { f.value = ''; f.classList.remove('invalid'); });
      $all('select', form).forEach(function (s) { s.selectedIndex = 0; });
      $all('input[type=checkbox]', form).forEach(function (c) { c.checked = false; });
      $all('.err', form).forEach(function (e) { e.textContent = ''; });
      var first = form.querySelector('input,select,textarea'); if (first) first.focus();
      form.dispatchEvent(new Event('change', { bubbles: true }));
    });
  });

  /* ---- Payment method sections ---- */
  var pm = $all('input[name=method]');
  if (pm.length) {
    var show = function () {
      var cur = (pm.filter(function (r) { return r.checked; })[0] || {}).value;
      $all('[data-method]').forEach(function (s) {
        var on = s.dataset.method.split(' ').indexOf(cur) >= 0;
        s.classList.toggle('hidden', !on);
        $all('input,select', s).forEach(function (i) { i.disabled = !on; });
      });
    };
    pm.forEach(function (r) { r.addEventListener('change', show); });
    show();
  }

  /* ---- Dynamic bill item rows ---- */
  var tbody = document.getElementById('bill-items');
  if (tbody) {
    var reindex = function () {
      $all('tr.item-row', tbody).forEach(function (tr, i) {
        $all('[data-field]', tr).forEach(function (el) {
          el.name = 'items[' + i + '].' + el.dataset.field;
          el.id = 'items' + i + '.' + el.dataset.field;
        });
      });
      recalc();
    };
    var recalc = function () {
      var total = 0;
      $all('tr.item-row', tbody).forEach(function (tr) {
        var g = function (n) { var e = tr.querySelector('[data-field=' + n + ']'); return parseFloat(e && e.value) || 0; };
        var line = g('quantity') * g('unitPrice');
        line = line + line * g('taxPercent') / 100 - g('discount');
        var cell = tr.querySelector('.line-total'); if (cell) cell.textContent = line.toFixed(2);
        total += line;
      });
      var t = document.getElementById('bill-total'); if (t) t.textContent = total.toFixed(2);
    };
    var tpl = document.getElementById('bill-row-template');
    document.getElementById('add-item').addEventListener('click', function () {
      tbody.appendChild(tpl.content.firstElementChild.cloneNode(true)); reindex();
    });
    tbody.addEventListener('click', function (e) {
      if (e.target.classList.contains('remove-item')) {
        if ($all('tr.item-row', tbody).length > 1) { e.target.closest('tr').remove(); reindex(); }
      }
    });
    tbody.addEventListener('input', recalc);
    reindex();
  }

  /* ---- Confirm prompts (data-confirm="message") ---- */
  $all('form[data-confirm]').forEach(function (f) {
    f.addEventListener('submit', function (e) { if (!window.confirm(f.dataset.confirm)) e.preventDefault(); });
  });

  /* ---- Invoice download toast ---- */
  $all('a[data-download]').forEach(function (a) {
    a.addEventListener('click', function () {
      var t = document.createElement('div');
      t.className = 'toast'; t.textContent = 'Your invoice has been successfully downloaded.';
      document.body.appendChild(t); setTimeout(function () { t.remove(); }, 4000);
    });
  });

  /* ---- Live availability check on the modify-booking page ---- */
  var av = document.getElementById('availability-check');
  if (av) {
    var form = av.closest('form');
    var run = function () {
      var qs = new URLSearchParams(new FormData(form));
      qs.delete('_csrf');
      fetch(av.dataset.url + '?' + qs.toString(), { credentials: 'same-origin' })
        .then(function (r) { return r.json(); })
        .then(function (j) { av.className = 'alert ' + (j.ok ? 'success' : 'error'); av.textContent = j.message; })
        .catch(function () { av.className = 'hidden'; });
    };
    var timer;
    $all('input,select', form).forEach(function (f) {
      f.addEventListener('change', function () { clearTimeout(timer); timer = setTimeout(run, 250); });
    });
  }

  /* ---- Date inputs: check-out must be after check-in ---- */
  var ci = document.querySelector('input[name=checkIn]'), co = document.querySelector('input[name=checkOut]');
  if (ci && co) {
    ci.addEventListener('change', function () {
      if (ci.value) {
        var d = new Date(ci.value); d.setDate(d.getDate() + 1);
        co.min = d.toISOString().slice(0, 10);
        if (co.value && co.value <= ci.value) co.value = '';
      }
    });
  }
})();
