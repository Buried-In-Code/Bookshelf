async function validate() {
  return (
    validateText("title", "Please enter a Title.")
  )
}

async function submitCreate() {
  const caller = "create-button";
  addLoading(caller);

  if (await validate()) {
    const form = document.getElementById("create-form");
    const formData = Object.fromEntries(new FormData(form));
    const body = {
      summary: formData["summary"].trim() || null,
      title: formData["title"].trim(),
    };

    const response = await submitRequest("/api/roles", "POST", body);
    if (response !== null) {
      form.reset();
      window.location = `/roles/${response.body.id}`;
    }
  }

  removeLoading(caller);
}

async function submitUpdate(roleId) {
  const caller = "update-button";
  addLoading(caller);

  if (await validate()) {
    const form = document.getElementById("update-form");
    const formData = Object.fromEntries(new FormData(form));
    const body = {
      summary: formData["summary"].trim() || null,
      title: formData["title"].trim(),
    };

    const response = await submitRequest(`/api/roles/${roleId}`, "PUT", body);
    if (response !== null) {
      form.reset();
      window.location = `/roles/${roleId}`;
    }
  }

  removeLoading(caller);
}

async function submitDelete(roleId) {
  const caller = "delete-button";
  addLoading(caller);

  const response = await submitRequest(`/api/roles/${roleId}`, "DELETE");
  if (response !== null)
    window.location = "/roles";

  removeLoading(caller);
}
