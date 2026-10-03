/* Small helpers shared by all pages: escaping, formatting, alerts, navbar with notification bell. */
const UI = (() => {
  function esc(value) {
    if (value === null || value === undefined) return "";
    return String(value)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
  }

  function dateTime(value) {
    if (!value) return "-";
    const d = new Date(value);
    return isNaN(d) ? esc(value) : d.toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
  }

  function minutes(value) {
    if (value === null || value === undefined) return "-";
    const m = Math.round(Number(value));
    if (m < 60) return m + " min";
    const h = Math.floor(m / 60);
    const rest = m % 60;
    if (h < 24) return h + "h " + rest + "m";
    return Math.floor(h / 24) + "d " + (h % 24) + "h";
  }

  function titleCase(v) {
    return String(v).replace(/_/g, " ").toLowerCase().replace(/(^|\s)\S/g, c => c.toUpperCase());
  }

  function statusBadge(status) {
    if (!status) return "";
    return '<span class="badge status-' + esc(status) + '">' + esc(titleCase(status)) + "</span>";
  }

  function priorityBadge(name) {
    if (!name) return "-";
    const up = String(name).toUpperCase();
    const key = ["LOW", "MEDIUM", "HIGH", "CRITICAL"].includes(up) ? up : "OTHER";
    return '<span class="badge priority-' + key + '">' + esc(titleCase(name)) + "</span>";
  }

  function slaBadge(breached) {
    return breached ? ' <span class="badge sla-breached">SLA breached</span>' : "";
  }

  /** Shows a Bootstrap alert inside the element with the given id. Pass message = "" to clear it. */
  function alertBox(elementId, type, message) {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.innerHTML = message
      ? '<div class="alert alert-' + type + ' alert-dismissible fade show" role="alert">' + message +
        '<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>'
      : "";
  }

  /** Shows an ApiError (with field errors if any) in an alert box. */
  function showError(elementId, err) {
    let html = esc(err && err.message ? err.message : "Something went wrong");
    if (err && err.fieldErrors && err.fieldErrors.length) {
      html += "<ul class='mb-0 mt-1'>" +
        err.fieldErrors.map(f => "<li>" + esc(f.message) + "</li>").join("") + "</ul>";
    }
    alertBox(elementId, "danger", html);
  }

  function statIcon(label, accent) {
    const l = String(label).toLowerCase();
    if (l.includes("sla")) return "bi-alarm";
    if (l.includes("avg") || l.includes("average")) return "bi-hourglass-split";
    if (l.includes("laborator")) return "bi-building";
    if (l.includes("computer") || l.includes("working") || l.includes("service") || l.includes("maintenance")) return "bi-pc-display";
    return { red: "bi-clock-history", blue: "bi-person-check", orange: "bi-gear-fill", green: "bi-check-circle-fill", gray: "bi-archive" }[accent] || "bi-ticket-detailed";
  }

  function statCard(label, value, accent) {
    return '<div class="col-6 col-md-4 col-xl-3"><div class="card stat-card accent-' + (accent || "dark") +
      ' h-100"><div class="card-body"><div class="stat-ico"><i class="bi ' + statIcon(label, accent) + '"></i></div><div>' +
      '<div class="stat-value">' + esc(value) + '</div><div class="stat-label">' + esc(label) + "</div></div></div></div></div>";
  }

  /* ---------------- sidebar + top bar ---------------- */
  // [label, icon, href]  | href "bell" = open notifications | href "logout" | no href = page not built yet
  const MENU = {
    STUDENT: [
      ["Dashboard", "bi-speedometer2", "/student/dashboard.html"],
      ["New Ticket", "bi-plus-square", "/student/create-ticket.html"],
      ["My Tickets", "bi-ticket-detailed", "/student/tickets.html"],
      ["Maintenance History", "bi-tools"],
      ["Feedback", "bi-chat-left-text"],
      ["Notifications", "bi-bell", "bell"],
      ["Profile", "bi-person"],
      ["Logout", "bi-box-arrow-right", "logout"]
    ],
    TECHNICIAN: [
      ["Dashboard", "bi-speedometer2", "/technician/dashboard.html"],
      ["Ticket Pool", "bi-inbox", "/technician/tickets.html"],
      ["Assigned Tickets", "bi-list-check", "/technician/tickets.html"],
      ["My Work", "bi-briefcase"],
      ["Maintenance History", "bi-tools"],
      ["Notifications", "bi-bell", "bell"],
      ["Profile", "bi-person"],
      ["Logout", "bi-box-arrow-right", "logout"]
    ],
    ADMIN: [
      ["Dashboard", "bi-speedometer2", "/admin/dashboard.html"],
      ["Tickets", "bi-ticket-detailed", "/admin/tickets.html"],
      ["Students", "bi-people", "/admin/students.html"],
      ["Technicians", "bi-person-gear", "/admin/technicians.html"],
      ["Laboratories", "bi-building", "/admin/laboratories.html"],
      ["Computers", "bi-pc-display", "/admin/computers.html"],
      ["Categories", "bi-tags", "/admin/categories.html"],
      ["Priorities", "bi-flag", "/admin/priorities.html"],
      ["Reports & Analytics", "bi-graph-up"],
      ["SLA Monitoring", "bi-stopwatch"],
      ["Notifications", "bi-bell", "bell"],
      ["Settings", "bi-gear"],
      ["Logout", "bi-box-arrow-right", "logout"]
    ]
  };

  function ticketLink(role, ticketId) {
    if (role === "STUDENT") return "/student/ticket-details.html?id=" + ticketId;
    if (role === "TECHNICIAN") return "/technician/ticket-details.html?id=" + ticketId;
    return "/admin/tickets.html";
  }

  function renderNavbar(user) {
    const host = document.getElementById("navbar");
    if (!host || document.querySelector(".app-shell")) return;
    const page = host.nextElementSibling; // the page's own container is moved into the new layout
    const path = location.pathname;
    const detailsOf = path.endsWith("ticket-details.html") ? path.replace("ticket-details.html", "tickets.html") : null;
    let activeDone = false;

    const links = (MENU[user.role] || []).map(([label, icon, href]) => {
      const ic = '<i class="bi ' + icon + '"></i>' + label;
      if (!href) return '<span class="side-link soon" title="Coming soon">' + ic + '<span class="tag">Soon</span></span>';
      if (href === "bell" || href === "logout") return '<button type="button" class="side-link" data-act="' + href + '">' + ic + "</button>";
      const on = !activeDone && (href === path || href === detailsOf);
      if (on) activeDone = true;
      return '<a class="side-link' + (on ? " active" : "") + '" href="' + href + '">' + ic + "</a>";
    }).join("");

    const shell = document.createElement("div");
    shell.className = "app-shell";
    shell.innerHTML =
      '<aside class="sidebar" id="sidebar"><a class="brand" href="' + Auth.homeFor(user.role) + '"><i class="bi bi-pc-display-horizontal"></i>' +
      '<span><b>Smart Lab</b><small>IT Support System</small></span></a><nav class="side-nav">' + links + "</nav></aside>" +
      '<div class="sidebar-backdrop" id="backdrop"></div>' +
      '<div class="app-main"><header class="topbar">' +
      '<button class="icon-btn d-lg-none" id="menuBtn" aria-label="Open menu"><i class="bi bi-list"></i></button><div class="flex-grow-1"></div>' +
      '<div class="dropdown"><button class="icon-btn position-relative" id="bellBtn" data-bs-toggle="dropdown" data-bs-auto-close="outside" aria-label="Notifications" aria-expanded="false">' +
      '<i class="bi bi-bell"></i><span class="badge bg-danger rounded-pill d-none bell-count" id="bellCount">0</span></button>' +
      '<div class="dropdown-menu dropdown-menu-end p-0 notif-menu"><div class="d-flex justify-content-between align-items-center px-3 py-2 border-bottom">' +
      '<strong>Notifications</strong><button class="btn btn-link btn-sm p-0" id="markAllBtn">Mark all read</button></div>' +
      '<div id="notifList" style="max-height:340px;overflow-y:auto"><div class="p-3 text-muted small">Loading...</div></div></div></div>' +
      '<div class="dropdown"><button class="btn d-flex align-items-center gap-2 p-1" data-bs-toggle="dropdown" aria-expanded="false">' +
      '<span class="avatar">' + esc((user.fullName || "?").charAt(0).toUpperCase()) + '</span>' +
      '<span class="d-none d-sm-block text-start lh-sm"><span class="d-block fw-semibold small">' + esc(user.fullName) +
      '</span><span class="text-muted" style="font-size:.7rem">' + esc(titleCase(user.role)) + '</span></span><i class="bi bi-chevron-down small"></i></button>' +
      '<ul class="dropdown-menu dropdown-menu-end"><li><button class="dropdown-item" id="logoutBtn"><i class="bi bi-box-arrow-right me-2"></i>Logout</button></li></ul></div>' +
      '</header><main class="app-content" id="appContent"></main></div>';

    host.parentNode.insertBefore(shell, host);
    host.remove();
    if (page) document.getElementById("appContent").appendChild(page);

    const sidebar = document.getElementById("sidebar"), backdrop = document.getElementById("backdrop");
    const toggle = open => { sidebar.classList.toggle("open", open); backdrop.classList.toggle("show", open); };
    document.getElementById("menuBtn").addEventListener("click", () => toggle(!sidebar.classList.contains("open")));
    backdrop.addEventListener("click", () => toggle(false));
    shell.querySelectorAll("[data-act]").forEach(b => b.addEventListener("click", () => {
      if (b.dataset.act === "logout") Auth.logout();
      else { toggle(false); document.getElementById("bellBtn").click(); }
    }));

    document.getElementById("logoutBtn").addEventListener("click", Auth.logout);
    document.getElementById("markAllBtn").addEventListener("click", async () => {
      try { await Api.put("/api/notifications/read-all"); await refreshBell(user); } catch (e) { /* ignore */ }
    });
    document.getElementById("bellBtn").addEventListener("click", () => loadNotifications(user));
    refreshBell(user);
    setInterval(() => refreshBell(user), 30000);
  }

  async function refreshBell(user) {
    try {
      const r = await Api.get("/api/notifications/unread-count", { redirect401: false });
      const badge = document.getElementById("bellCount");
      if (!badge) return;
      badge.textContent = r.unread;
      badge.classList.toggle("d-none", !r.unread);
    } catch (e) { /* the bell is optional, ignore errors */ }
  }

  async function loadNotifications(user) {
    const list = document.getElementById("notifList");
    try {
      const page = await Api.get("/api/notifications?size=10");
      if (!page.content.length) {
        list.innerHTML = '<div class="p-3 text-muted small">No notifications yet.</div>';
        return;
      }
      list.innerHTML = page.content.map(n =>
        '<a href="#" class="dropdown-item notif-item border-bottom py-2' + (n.read ? "" : " unread") +
        '" data-id="' + n.id + '" data-ticket="' + (n.ticketId || "") + '">' +
        '<div class="small">' + esc(n.message) + '</div><div class="text-muted" style="font-size:.7rem">' +
        dateTime(n.createdAt) + "</div></a>").join("");
      list.querySelectorAll("a.notif-item").forEach(a => a.addEventListener("click", async ev => {
        ev.preventDefault();
        try { await Api.put("/api/notifications/" + a.dataset.id + "/read"); } catch (e) { /* ignore */ }
        if (a.dataset.ticket) window.location.href = ticketLink(user.role, a.dataset.ticket);
        else { a.classList.remove("unread"); refreshBell(user); }
      }));
    } catch (e) {
      list.innerHTML = '<div class="p-3 text-danger small">' + esc(e.message) + "</div>";
    }
  }

  return { esc, dateTime, minutes, statusBadge, priorityBadge, slaBadge, alertBox, showError, statCard, renderNavbar };
})();
