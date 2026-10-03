/* Talks to the REST API. Stores the JWT in localStorage and adds it to every request. */
const Api = (() => {
  const TOKEN_KEY = "labsupport.token";
  const USER_KEY = "labsupport.user";

  class ApiError extends Error {
    constructor(status, message, fieldErrors) {
      super(message);
      this.status = status;
      this.fieldErrors = fieldErrors || [];
    }
  }

  function getToken() { return localStorage.getItem(TOKEN_KEY); }

  function getUser() {
    try { return JSON.parse(localStorage.getItem(USER_KEY)); } catch (e) { return null; }
  }

  function setSession(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  /** options.auth = false sends no token (login/register). options.redirect401 = false stops the auto redirect. */
  async function request(method, path, body, options) {
    const opts = Object.assign({ auth: true, redirect401: true }, options || {});
    const headers = {};
    if (body !== undefined && body !== null) headers["Content-Type"] = "application/json";
    const token = getToken();
    if (opts.auth && token) headers["Authorization"] = "Bearer " + token;

    let res;
    try {
      res = await fetch(path, {
        method,
        headers,
        body: body !== undefined && body !== null ? JSON.stringify(body) : undefined
      });
    } catch (e) {
      throw new ApiError(0, "Cannot reach the server. Is the application running?");
    }

    if (res.status === 204) return null;

    let data = null;
    const text = await res.text();
    if (text) {
      try { data = JSON.parse(text); } catch (e) { data = null; }
    }

    if (!res.ok) {
      if (res.status === 401 && opts.auth && opts.redirect401 && token) {
        clearSession();
        window.location.href = "/login.html?expired=1";
      }
      throw new ApiError(res.status,
        (data && data.message) || ("Request failed (" + res.status + ")"),
        data && data.fieldErrors);
    }
    return data;
  }

  return {
    ApiError, getToken, getUser, setSession, clearSession, request,
    get: (p, o) => request("GET", p, null, o),
    post: (p, b, o) => request("POST", p, b === undefined ? null : b, o),
    put: (p, b, o) => request("PUT", p, b === undefined ? null : b, o),
    del: (p, o) => request("DELETE", p, null, o),
    postPublic: (p, b) => request("POST", p, b, { auth: false })
  };
})();
