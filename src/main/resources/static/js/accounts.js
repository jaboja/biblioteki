let libraries = [];
let currentEditId = null;
async function loadAccounts() {
  try {
    await loadLibraries();
    const response = await fetch("/api/accounts");
    if (!response.ok) {
      throw new Error(`Błąd HTTP! status: ${response.status}`);
    }
    const data = await response.json();
    displayAccounts(data);
  } catch (error) {
    showError("Nie udało się załadować kont: " + error.message);
  }
}
async function loadLibraries() {
  try {
    const response = await fetch("/api/accounts/libraries");
    if (!response.ok) {
      throw new Error(`Błąd HTTP! status: ${response.status}`);
    }
    libraries = await response.json();
    updateLibrarySelect();
  } catch (error) {
    showError("Nie udało się załadować listy bibliotek: " + error.message);
  }
}
function updateLibrarySelect() {
  const select = document.getElementById("librarySelect");
  if (!select) return;
  select.innerHTML = '<option value="">Wybierz bibliotekę...</option>';
  libraries.forEach((lib) => {
    const option = document.createElement("option");
    option.value = lib.id;
    option.textContent = lib.name || lib.id;
    select.appendChild(option);
  });
}
function displayAccounts(accounts) {
  const refreshTime = (/* @__PURE__ */ new Date()).toLocaleString("pl-PL");
  const refreshTimeEl = document.getElementById("refreshTime");
  if (refreshTimeEl) {
    refreshTimeEl.textContent = `Ostatnia aktualizacja: ${refreshTime}`;
  }
  hideError();
  localStorage.setItem("currentAccounts", JSON.stringify(accounts));
  const tbody = document.getElementById("accountsTableBody");
  if (!tbody) return;
  tbody.innerHTML = "";
  if (accounts.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" style="text-align: center;">Brak kont</td></tr>';
    return;
  }
  accounts.forEach((account) => {
    const row = document.createElement("tr");
    let statusClass = "renewable";
    let statusText = "Aktywne";
    if (!account.enabled) {
      statusClass = "not-renewable";
      statusText = "Nieaktywne";
    }
    row.innerHTML = `
            <td>${account.id || "Brak"}</td>
            <td>${account.libraryDisplayName || account.library || "Brak"}</td>
            <td>${account.username || "Brak"}</td>
            <td class="${statusClass}">${statusText}</td>
            <td class="actions">
                <button onclick="editAccount(${account.id})" style="background-color: #3498db" title="Edytuj">Edytuj</button>
                <button onclick="deleteAccount(${account.id})" style="background-color: #e74c3c" title="Usuń">Usuń</button>
            </td>
        `;
    tbody.appendChild(row);
  });
}
function showAddForm() {
  currentEditId = null;
  const formTitle = document.getElementById("formTitle");
  if (formTitle) formTitle.textContent = "Dodaj Nowe Konto";
  setFormValues("", "", "", true);
  showForm();
}
function showEditForm(account) {
  currentEditId = account.id;
  const formTitle = document.getElementById("formTitle");
  if (formTitle) formTitle.textContent = "Edytuj Konto";
  setFormValues(account.library, account.username, "", account.enabled);
  showForm();
}
function setFormValues(library, username, password, enabled) {
  const librarySelect = document.getElementById("librarySelect");
  const usernameInput = document.getElementById("usernameInput");
  const passwordInput = document.getElementById("passwordInput");
  const enabledInput = document.getElementById("enabledInput");
  if (librarySelect) librarySelect.value = library;
  if (usernameInput) usernameInput.value = username;
  if (passwordInput) passwordInput.value = password;
  if (enabledInput) enabledInput.checked = enabled;
}
function showForm() {
  const form = document.getElementById("accountForm");
  if (form) form.style.display = "block";
}
function hideForm() {
  const form = document.getElementById("accountForm");
  if (form) form.style.display = "none";
  currentEditId = null;
}
async function saveAccount() {
  const librarySelect = document.getElementById("librarySelect");
  const usernameInput = document.getElementById("usernameInput");
  const passwordInput = document.getElementById("passwordInput");
  const enabledInput = document.getElementById("enabledInput");
  if (!librarySelect || !usernameInput) {
    showError("Proszę uzupełnić wszystkie wymagane pola");
    return;
  }
  const library = librarySelect.value;
  const username = usernameInput.value;
  const password = (passwordInput == null ? void 0 : passwordInput.value) || "";
  const enabled = (enabledInput == null ? void 0 : enabledInput.checked) || false;
  if (!library || !username) {
    showError("Proszę uzupełnić wszystkie wymagane pola");
    return;
  }
  try {
    const requestBody = { library, username, password, enabled };
    let response;
    if (currentEditId) {
      response = await fetch(`/api/accounts/${currentEditId}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestBody)
      });
    } else {
      response = await fetch("/api/accounts", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestBody)
      });
    }
    if (!response.ok) {
      throw new Error(`Błąd HTTP! status: ${response.status}`);
    }
    hideForm();
    await loadAccounts();
  } catch (error) {
    showError("Nie udało się zapisać konta: " + error.message);
  }
}
async function deleteAccount(id) {
  if (!confirm("Czy na pewno chcesz usunąć to konto?")) {
    return;
  }
  try {
    const response = await fetch(`/api/accounts/${id}`, {
      method: "DELETE"
    });
    if (!response.ok) {
      throw new Error(`Błąd HTTP! status: ${response.status}`);
    }
    await loadAccounts();
  } catch (error) {
    showError("Nie udało się usunąć konta: " + error.message);
  }
}
function editAccount(id) {
  const accountsJson = localStorage.getItem("currentAccounts");
  if (!accountsJson) {
    showError("Nie znaleziono kont do edycji");
    return;
  }
  const accounts = JSON.parse(accountsJson);
  const account = accounts.find((a) => a.id === id);
  if (account) {
    showEditForm({
      id: account.id,
      library: account.library,
      username: account.username,
      enabled: account.enabled
    });
  } else {
    showError("Nie znaleziono konta do edycji");
  }
}
function showError(message) {
  const errorDiv = document.getElementById("errors");
  if (errorDiv) {
    errorDiv.textContent = message;
    errorDiv.style.display = "block";
  }
}
function hideError() {
  const errorDiv = document.getElementById("errors");
  if (errorDiv) {
    errorDiv.style.display = "none";
  }
}
window.loadAccounts = loadAccounts;
window.loadLibraries = loadLibraries;
window.displayAccounts = displayAccounts;
window.showAddForm = showAddForm;
window.showEditForm = showEditForm;
window.setFormValues = setFormValues;
window.showForm = showForm;
window.hideForm = hideForm;
window.saveAccount = saveAccount;
window.deleteAccount = deleteAccount;
window.editAccount = editAccount;
window.updateLibrarySelect = updateLibrarySelect;
window.showError = showError;
window.hideError = hideError;
if (window.location.pathname.includes("accounts")) {
  window.addEventListener("DOMContentLoaded", loadAccounts);
}
