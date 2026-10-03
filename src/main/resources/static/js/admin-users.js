/* One script for the Students and Technicians pages: list, search, add, edit, reset password, delete. */
const AdminUsers = (() => {
  function init(cfg) {
    const $ = id => document.getElementById(id);
    const byId = {};
    let page = 0, lastPage = true, mode = "create", editingId = null, modal = null, search = "";

    $("page").innerHTML =
      '<div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2"><h3 class="page-title mb-0">' + cfg.plural + "</h3>" +
      '<div class="d-flex gap-2"><input id="search" class="form-control" placeholder="Search name or email">' +
      '<button class="btn btn-primary text-nowrap" id="addBtn">+ Add ' + cfg.singular + "</button></div></div>" +
      '<div id="alert"></div>' +
      '<div class="card"><div class="table-responsive"><table class="table align-middle mb-0"><thead><tr><th>Name</th><th>Email</th><th>Status</th><th>Created</th><th></th></tr></thead>' +
      '<tbody id="rows"></tbody></table></div><div class="card-footer bg-white d-flex justify-content-between align-items-center">' +
      '<span class="small text-muted" id="count"></span><div class="btn-group"><button class="btn btn-sm btn-outline-secondary" id="prev">Previous</button>' +
      '<button class="btn btn-sm btn-outline-secondary" id="next">Next</button></div></div></div>' +
      '<div class="modal fade" id="userModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">' +
      '<div class="modal-header"><h5 class="modal-title" id="mTitle"></h5><button class="btn-close" data-bs-dismiss="modal"></button></div>' +
      '<div class="modal-body"><div id="mAlert"></div>' +
      '<div id="gName" class="mb-3"><label class="form-label">Full name</label><input id="fullName" class="form-control" maxlength="100"></div>' +
      '<div id="gEmail" class="mb-3"><label class="form-label">Email</label><input id="email" type="email" class="form-control" maxlength="150"></div>' +
      '<div id="gPass" class="mb-3"><label class="form-label" id="passLabel">Password</label><input id="password" type="password" class="form-control" maxlength="64" autocomplete="new-password">' +
      '<div class="form-text">8-64 characters with at least one letter and one digit.</div></div>' +
      '<div id="gActive" class="form-check form-switch"><input class="form-check-input" type="checkbox" id="active"><label class="form-check-label" for="active">Account is active (can log in)</label></div>' +
      '</div><div class="modal-footer"><button class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button><button class="btn btn-primary" id="mSave">Save</button></div></div></div></div>';

    modal = new bootstrap.Modal($("userModal"));

    async function load() {
      const q = "page=" + page + "&size=10" + (search ? "&search=" + encodeURIComponent(search) : "");
      try {
        const p = await Api.get(cfg.base + "?" + q);
        lastPage = p.last;
        p.content.forEach(u => byId[u.id] = u);
        $("rows").innerHTML = p.content.length ? p.content.map(u =>
          "<tr><td>" + UI.esc(u.fullName) + "</td><td>" + UI.esc(u.email) + "</td><td>" +
          (u.active ? '<span class="badge bg-success">Active</span>' : '<span class="badge bg-secondary">Inactive</span>') +
          "</td><td>" + UI.dateTime(u.createdAt) + '</td><td class="text-end text-nowrap">' +
          '<button class="btn btn-sm btn-outline-primary me-1" data-act="edit" data-id="' + u.id + '">Edit</button>' +
          '<button class="btn btn-sm btn-outline-secondary me-1" data-act="pwd" data-id="' + u.id + '">Password</button>' +
          '<button class="btn btn-sm btn-outline-danger" data-act="del" data-id="' + u.id + '">Delete</button></td></tr>').join("")
          : '<tr><td colspan="5" class="text-muted p-3">No ' + cfg.plural.toLowerCase() + " found.</td></tr>";
        $("count").textContent = p.totalElements + " " + cfg.singular + "(s) - page " + (p.page + 1) + " of " + Math.max(p.totalPages, 1);
        $("prev").disabled = page === 0; $("next").disabled = lastPage;
      } catch (e) { UI.showError("alert", e); }
    }

    function openModal(m, user) {
      mode = m; editingId = user ? user.id : null;
      UI.alertBox("mAlert", "", "");
      $("fullName").value = user ? user.fullName : ""; $("email").value = user ? user.email : "";
      $("password").value = ""; $("active").checked = user ? user.active : true;
      $("gName").classList.toggle("d-none", m === "pwd"); $("gEmail").classList.toggle("d-none", m === "pwd");
      $("gPass").classList.toggle("d-none", m === "edit"); $("gActive").classList.toggle("d-none", m !== "edit");
      $("passLabel").textContent = m === "pwd" ? "New password" : "Password";
      $("mTitle").textContent = m === "create" ? "Add " + cfg.singular : m === "edit" ? "Edit " + cfg.singular : "Reset password for " + user.fullName;
      modal.show();
    }

    $("mSave").onclick = async () => {
      UI.alertBox("mAlert", "", "");
      try {
        if (mode === "create") await Api.post(cfg.base, { fullName: $("fullName").value.trim(), email: $("email").value.trim(), password: $("password").value });
        else if (mode === "edit") await Api.put(cfg.base + "/" + editingId, { fullName: $("fullName").value.trim(), email: $("email").value.trim(), active: $("active").checked });
        else await Api.put(cfg.base + "/" + editingId + "/password", { newPassword: $("password").value });
        modal.hide();
        UI.alertBox("alert", "success", mode === "pwd" ? "Password changed." : "Saved.");
        load();
      } catch (e) { UI.showError("mAlert", e); }
    };

    $("rows").onclick = async ev => {
      const b = ev.target.closest("button[data-act]"); if (!b) return;
      const u = byId[b.dataset.id];
      if (b.dataset.act === "edit") openModal("edit", u);
      else if (b.dataset.act === "pwd") openModal("pwd", u);
      else if (confirm("Delete " + u.fullName + "? This cannot be undone.")) {
        try { await Api.del(cfg.base + "/" + u.id); UI.alertBox("alert", "success", "Deleted."); load(); }
        catch (e) { UI.showError("alert", e); }
      }
    };
    $("addBtn").onclick = () => openModal("create", null);
    let timer; $("search").oninput = e => { clearTimeout(timer); timer = setTimeout(() => { search = e.target.value.trim(); page = 0; load(); }, 300); };
    $("prev").onclick = () => { page--; load(); }; $("next").onclick = () => { page++; load(); };
    load();
  }
  return { init };
})();
