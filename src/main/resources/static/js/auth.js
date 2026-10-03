/* Login, registration, logout and role-based page protection (the server enforces the real rules). */
const Auth = (() => {
  const HOME = {
    STUDENT: "/student/dashboard.html",
    TECHNICIAN: "/technician/dashboard.html",
    ADMIN: "/admin/dashboard.html"
  };

  function homeFor(role) { return HOME[role] || "/login.html"; }

  async function login(email, password) {
    const r = await Api.postPublic("/api/auth/login", { email, password });
    Api.setSession(r.token, r.user);
    return r.user;
  }

  async function register(fullName, email, password) {
    const r = await Api.postPublic("/api/auth/register", { fullName, email, password });
    Api.setSession(r.token, r.user);
    return r.user;
  }

  function logout() {
    Api.clearSession();
    window.location.href = "/login.html";
  }

  const never = () => new Promise(() => {});

  /** Call at the top of every protected page: Auth.require("STUDENT").then(user => ...) */
  async function requireRole(role) {
    if (!Api.getToken()) {
      window.location.replace("/login.html");
      return never();
    }
    let me;
    try {
      me = await Api.get("/api/auth/me");
    } catch (e) {
      Api.clearSession();
      window.location.replace("/login.html?expired=1");
      return never();
    }
    Api.setSession(Api.getToken(), me);
    if (role && me.role !== role) {
      window.location.replace(homeFor(me.role));
      return never();
    }
    return me;
  }

  return { homeFor, login, register, logout, require: requireRole };
})();
