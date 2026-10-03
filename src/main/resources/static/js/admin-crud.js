/* Shared admin list page: loading / empty / error states, search, filters, optional paging,
   add-edit modal, delete, and optional extra row actions with their own dialog. */
const AdminCrud = (() => {
  const BADGE = { good: "status-RESOLVED", warn: "status-IN_PROGRESS", bad: "status-OPEN", info: "status-ASSIGNED", muted: "status-CLOSED" };
  const badge = (text, kind) => '<span class="badge ' + BADGE[kind || "muted"] + '">' + UI.esc(text) + "</span>";

  function init(cfg) {
    const $ = id => document.getElementById(id);
    const byId = {};
    const filterState = {};
    const pageSize = cfg.pageSize || 10;
    const ncols = cfg.columns.length + 1;
    let all = [], meta = {}, page = 0, editing = null, q = "";

    /* ---------- layout ---------- */
    const filtersHtml = (cfg.filters || []).map(f =>
      '<select id="f_' + f.id + '" class="form-select w-auto" aria-label="' + UI.esc(f.label) + '"><option value="">' + UI.esc(f.label) + "</option></select>").join("");
    $("page").innerHTML =
      '<div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2"><div><h3 class="page-title mb-0">' + cfg.title +
      '</h3><p class="text-muted mb-0">' + cfg.subtitle + "</p></div>" +
      '<div class="d-flex flex-wrap gap-2">' + filtersHtml +
      '<input id="search" class="form-control w-auto" placeholder="' + UI.esc(cfg.searchPlaceholder) + '" aria-label="Search">' +
      '<button class="btn btn-primary text-nowrap" id="addBtn"><i class="bi bi-plus-lg me-1"></i>Add ' + cfg.singular + "</button></div></div>" +
      '<div id="alert"></div>' +
      '<div class="card"><div class="table-responsive"><table class="table table-hover align-middle mb-0"><thead><tr>' +
      cfg.columns.map(c => "<th>" + c.label + "</th>").join("") + "<th></th></tr></thead><tbody id=\"rows\"></tbody></table></div>" +
      '<div class="card-footer bg-white d-flex justify-content-between align-items-center"><span class="small text-muted" id="count"></span>' +
      (cfg.paged ? '<div class="btn-group"><button class="btn btn-sm btn-outline-secondary" id="prev">Previous</button><button class="btn btn-sm btn-outline-secondary" id="next">Next</button></div>' : "") +
      "</div></div>" +
      // add / edit modal
      '<div class="modal fade" id="formModal" tabindex="-1"><div class="modal-dialog modal-dialog-scrollable' + (cfg.wide ? " modal-lg" : "") + '"><form class="modal-content" id="crudForm" novalidate>' +
      '<div class="modal-header"><h5 class="modal-title" id="mTitle"></h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>' +
      '<div class="modal-body"><div id="mAlert"></div><div class="row g-3">' + cfg.fields.map(fieldHtml).join("") + "</div></div>" +
      '<div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button><button class="btn btn-primary" id="mSave" type="submit">Save</button></div></form></div></div>' +
      // extra dialog (technicians, maintenance history)
      '<div class="modal fade" id="xModal" tabindex="-1"><div class="modal-dialog modal-lg modal-dialog-scrollable"><div class="modal-content">' +
      '<div class="modal-header"><h5 class="modal-title" id="xTitle"></h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>' +
      '<div class="modal-body"><div id="xAlert"></div><div id="xBody"></div></div>' +
      '<div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button><button class="btn btn-primary d-none" id="xSave"></button></div></div></div></div>';

    const formModal = new bootstrap.Modal($("formModal"));
    const xModal = new bootstrap.Modal($("xModal"));

    function fieldHtml(f) {
      const id = "fld_" + f.name;
      if (f.type === "checkbox") {
        return '<div class="col-12"><div class="form-check form-switch"><input class="form-check-input" type="checkbox" id="' + id +
          '"><label class="form-check-label" for="' + id + '">' + UI.esc(f.label) + "</label></div></div>";
      }
      const attrs = (f.maxlength ? ' maxlength="' + f.maxlength + '"' : "") + (f.min !== undefined ? ' min="' + f.min + '"' : "") + (f.max !== undefined ? ' max="' + f.max + '"' : "");
      const input = f.type === "textarea" ? '<textarea class="form-control" id="' + id + '" rows="2"' + attrs + "></textarea>"
        : f.type === "select" ? '<select class="form-select" id="' + id + '"></select>'
        : '<input class="form-control" id="' + id + '" type="' + (f.type || "text") + '" autocomplete="off"' + attrs + ">";
      return '<div class="col-12' + (f.half ? " col-md-6" : "") + '"><label class="form-label" for="' + id + '">' + UI.esc(f.label) +
        (f.required ? ' <span class="text-danger">*</span>' : "") + "</label>" + input +
        (f.help ? '<div class="form-text">' + UI.esc(f.help) + "</div>" : "") + "</div>";
    }

    function fillForm(item) {
      cfg.fields.forEach(f => {
        const el = $("fld_" + f.name);
        const v = item ? item[f.name] : f.default;
        if (f.type === "checkbox") el.checked = v === undefined ? false : !!v;
        else el.value = v === undefined || v === null ? "" : v;
      });
    }

    function readForm() {
      const out = {};
      cfg.fields.forEach(f => {
        const el = $("fld_" + f.name);
        if (f.type === "checkbox") { out[f.name] = el.checked; return; }
        const raw = el.value.trim();
        if (f.type === "number" || f.numeric) out[f.name] = raw === "" ? null : Number(raw);
        else if (f.type === "date" || f.type === "select") out[f.name] = raw === "" ? null : raw;
        else out[f.name] = raw;
      });
      return out;
    }

    function setOptions(select, pairs, placeholder) {
      select.innerHTML = (placeholder !== undefined ? '<option value="">' + UI.esc(placeholder) + "</option>" : "") +
        pairs.map(p => '<option value="' + UI.esc(p[0]) + '">' + UI.esc(p[1]) + "</option>").join("");
    }

    /* ---------- table ---------- */
    const messageRow = html => '<tr><td colspan="' + ncols + '" class="text-center text-muted py-4">' + html + "</td></tr>";

    function render() {
      const list = q ? all.filter(i => cfg.match(i, q.toLowerCase())) : all;
      const filtered = q || Object.values(filterState).some(Boolean);
      if (!list.length) {
        $("rows").innerHTML = messageRow('<i class="bi bi-inbox fs-3 d-block mb-1"></i>' +
          UI.esc(filtered ? "Nothing matches your search or filters." : cfg.emptyText) +
          (filtered ? "" : ' <button class="btn btn-sm btn-primary ms-2" data-add>Add ' + cfg.singular + "</button>"));
      } else {
        const extra = cfg.actions || [];
        $("rows").innerHTML = list.map(i =>
          "<tr>" + cfg.columns.map(c => "<td>" + c.render(i) + "</td>").join("") + '<td class="text-end text-nowrap">' +
          '<button class="btn btn-sm btn-outline-primary me-1" data-act="edit" data-id="' + i.id + '">Edit</button>' +
          extra.map(a => '<button class="btn btn-sm btn-outline-secondary me-1" data-act="' + a.key + '" data-id="' + i.id + '">' + a.label + "</button>").join("") +
          '<button class="btn btn-sm btn-outline-danger" data-act="del" data-id="' + i.id + '">Delete</button></td></tr>').join("");
      }
      if (cfg.paged) {
        $("count").textContent = (meta.totalElements || 0) + " " + cfg.plural + " - page " + ((meta.page || 0) + 1) + " of " + Math.max(meta.totalPages || 1, 1);
        $("prev").disabled = page === 0; $("next").disabled = !!meta.last;
      } else {
        $("count").textContent = list.length + (list.length === all.length ? "" : " of " + all.length) + " " + cfg.plural;
      }
    }

    async function load() {
      $("rows").innerHTML = messageRow('<span class="spinner-border spinner-border-sm me-2"></span>Loading...');
      try {
        meta = await cfg.fetch({ page, size: pageSize, filters: filterState });
        all = meta.items;
        all.forEach(i => { byId[i.id] = i; });
        render();
      } catch (e) {
        $("rows").innerHTML = messageRow('<span class="text-danger">Could not load ' + cfg.plural + '.</span> <button class="btn btn-sm btn-outline-primary ms-2" id="retry">Retry</button>');
        $("retry").onclick = load;
        UI.showError("alert", e);
      }
    }

    /* ---------- add / edit / delete / extra actions ---------- */
    const ctx = {
      reload: load,
      notify: (type, msg) => UI.alertBox("alert", type, msg),
      dialog(o) {
        $("xTitle").textContent = o.title;
        $("xBody").innerHTML = o.body;
        UI.alertBox("xAlert", "", "");
        const save = $("xSave");
        save.classList.toggle("d-none", !o.onSave);
        save.textContent = o.saveLabel || "Save";
        save.onclick = async () => {
          try { await o.onSave(); xModal.hide(); } catch (e) { UI.showError("xAlert", e); }
        };
        xModal.show();
      },
      error: e => UI.showError("xAlert", e)
    };

    function openForm(item) {
      editing = item ? item.id : null;
      UI.alertBox("mAlert", "", "");
      $("mTitle").textContent = (item ? "Edit " : "Add ") + cfg.singular;
      fillForm(item);
      formModal.show();
    }

    $("crudForm").addEventListener("submit", async ev => {
      ev.preventDefault();
      const btn = $("mSave");
      btn.disabled = true;
      UI.alertBox("mAlert", "", "");
      try {
        const body = readForm();
        if (editing) await Api.put(cfg.base + "/" + editing, body); else await Api.post(cfg.base, body);
        formModal.hide();
        ctx.notify("success", editing ? "Changes saved." : "Added successfully.");
        load();
      } catch (e) { UI.showError("mAlert", e); }
      btn.disabled = false;
    });

    $("page").addEventListener("click", async ev => {
      if (ev.target.closest("[data-add]") || ev.target.closest("#addBtn")) { openForm(null); return; }
      const b = ev.target.closest("button[data-act]");
      if (!b) return;
      const item = byId[b.dataset.id];
      const act = b.dataset.act;
      if (act === "edit") openForm(item);
      else if (act === "del") {
        if (!confirm("Delete " + cfg.itemName(item) + "? This cannot be undone.")) return;
        try { await Api.del(cfg.base + "/" + item.id); ctx.notify("success", "Deleted."); load(); }
        catch (e) { UI.showError("alert", e); }
      } else {
        const action = (cfg.actions || []).find(a => a.key === act);
        if (action) action.run(item, ctx);
      }
    });

    let timer;
    $("search").addEventListener("input", e => { clearTimeout(timer); timer = setTimeout(() => { q = e.target.value.trim(); render(); }, 250); });
    if (cfg.paged) { $("prev").onclick = () => { page--; load(); }; $("next").onclick = () => { page++; load(); }; }

    /* ---------- lookups (filters and select fields), then first load ---------- */
    (async () => {
      const urlParams = new URLSearchParams(location.search);
      for (const f of (cfg.filters || [])) {
        const el = $("f_" + f.id);
        try { setOptions(el, await f.options(), f.label); } catch (e) { /* filter stays empty */ }
        if (f.param && urlParams.get(f.param)) { el.value = urlParams.get(f.param); filterState[f.id] = el.value; }
        el.onchange = () => { filterState[f.id] = el.value; page = 0; load(); };
      }
      for (const f of cfg.fields.filter(x => x.type === "select")) {
        try { setOptions($("fld_" + f.name), await f.options(), "Select..."); } catch (e) { UI.showError("alert", e); }
      }
      load();
    })();
  }
  return { init, badge };
})();
