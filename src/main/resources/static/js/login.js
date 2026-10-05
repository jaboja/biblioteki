const loginForm = document.getElementById("loginForm");
const errorEl = document.getElementById("error");
if (loginForm) {
  loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const usernameInput = document.getElementById("username");
    const passwordInput = document.getElementById("password");
    if (!usernameInput || !passwordInput) return;
    const username = usernameInput.value;
    const password = passwordInput.value;
    if (errorEl) {
      errorEl.style.display = "none";
    }
    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ username, password })
      });
      if (!response.ok) {
        throw new Error("Login failed");
      }
      const data = await response.json();
      const token = data.token;
      localStorage.setItem("jwt", token);
      window.location.href = "/";
    } catch (err) {
      if (errorEl) {
        errorEl.style.display = "block";
      }
    }
  });
}
