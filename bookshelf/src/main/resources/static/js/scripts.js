const HEADERS = {
  "Accept": "application/json; charset=UTF-8",
  "Content-Type": "application/json; charset=UTF-8",
};

function ready(callback) {
  if (document.readyState === "loading")
    document.addEventListener("DOMContentLoaded", callback);
  else
    callback();
}

function addLoading(caller) {
  const element = document.getElementById(caller);
  element.classList.add("is-loading");
}

function removeLoading(caller) {
  const element = document.getElementById(caller);
  element.classList.remove("is-loading");
}

function resetForm(page) {
  window.location = page;
}

async function submitRequest(endpoint, method, body = {}, headers = HEADERS) {
  try {
    const options = {
      method: method,
      headers: headers,
    };
    if (method !== "GET")
      options.body = JSON.stringify(body);

    const response = await fetch(endpoint, options);

    if (!response.ok)
      throw response;
    const responseBody = response.status !== 204 ? await response.json() : "";
    return { status: response.status, body: responseBody }
  } catch(error) {
    if(typeof error.text === "function")
      alert(`${error.status} ${error.statusText}: ${await error.text()}`);
    else
      alert(error);
    return null;
  }
}

async function signOut() {
    const caller = "sign-out-button";
    
    addLoading(caller);
    const response = await submitRequest("/sign-out", "DELETE");
    if (response !== null)
      window.location = "/";
    removeLoading(caller);
}
