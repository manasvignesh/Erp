let currentUser = null;
let currentEvent = null;
let allMembers = [];
let selectedMembers = new Set();

const loginView = document.getElementById("loginView");
const eventsView = document.getElementById("eventsView");
const detailView = document.getElementById("detailView");
const userArea = document.getElementById("userArea");

function show(view) {
  loginView.classList.add("hidden");
  eventsView.classList.add("hidden");
  detailView.classList.add("hidden");
  view.classList.remove("hidden");
}

async function api(url, options = {}) {
  const response = await fetch(url, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });

  if (!response.ok) {
    let message = "Something went wrong";
    try {
      const body = await response.json();
      message = body.message || body.detail || message;
    } catch (_) {}
    throw new Error(message);
  }

  return response.status === 204 ? null : response.json();
}

function setUser(user) {
  currentUser = user;

  if (!user) {
    sessionStorage.removeItem("nlscUser");
    userArea.classList.add("hidden");
    show(loginView);
    return;
  }

  sessionStorage.setItem("nlscUser", JSON.stringify(user));
  document.getElementById("userName").textContent = user.name + " (" + user.role + ")";
  userArea.classList.remove("hidden");
  show(eventsView);
  loadEvents();
}

document.getElementById("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const error = document.getElementById("loginError");
  error.textContent = "";

  try {
    const user = await api("/api/login", {
      method: "POST",
      body: JSON.stringify({
        email: document.getElementById("email").value,
        password: document.getElementById("password").value
      })
    });
    setUser(user);
  } catch (err) {
    error.textContent = err.message;
  }
});

document.getElementById("logoutBtn").addEventListener("click", () => setUser(null));

async function loadEvents() {
  const search = document.getElementById("eventSearch").value.trim();
  const events = await api("/api/events?search=" + encodeURIComponent(search));
  const list = document.getElementById("eventList");

  if (!events.length) {
    list.innerHTML = '<div class="card">No events found.</div>';
    return;
  }

  list.innerHTML = events.map(event => `
    <div class="event-card">
      <span class="status">${event.status}</span>
      <h3>${escapeHtml(event.eventName)}</h3>
      <p class="muted">${escapeHtml(event.description)}</p>
      <div class="event-meta">
        <span>📅 ${event.eventDate}</span>
        <span>📍 ${escapeHtml(event.location)}</span>
      </div>
      <button class="primary-btn" onclick="openEvent(${event.id})">View Details</button>
    </div>
  `).join("");
}

let searchTimer;
document.getElementById("eventSearch").addEventListener("input", () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(loadEvents, 250);
});

window.openEvent = async function(id) {
  currentEvent = await api("/api/events/" + id);

  document.getElementById("detailName").textContent = currentEvent.eventName;
  document.getElementById("detailStatus").textContent = currentEvent.status;
  document.getElementById("detailDescription").textContent = currentEvent.description;
  document.getElementById("detailDate").textContent = currentEvent.eventDate;
  document.getElementById("detailLocation").textContent = currentEvent.location;
  document.getElementById("detailOrganizer").textContent = currentEvent.organizerEmail;
  document.getElementById("detailSupport").textContent = currentEvent.supportMobile;

  const teamArea = document.getElementById("studentTeamArea");
  const qrAdmin = document.getElementById("adminQrArea");

  if (currentUser.role === "STUDENT") {
    teamArea.classList.remove("hidden");
    await loadTeam();
    await loadMembers("");
  } else {
    teamArea.classList.add("hidden");
  }

  if (currentUser.role === "ADMIN" || currentUser.role === "COLLEGE") {
    qrAdmin.classList.remove("hidden");
  } else {
    qrAdmin.classList.add("hidden");
  }

  document.getElementById("attendanceMessage").textContent = "";
  document.getElementById("qrResult").innerHTML = "";
  show(detailView);
};

document.getElementById("backBtn").addEventListener("click", () => {
  show(eventsView);
});

async function loadTeam() {
  const result = await api(
    "/api/events/" + currentEvent.id + "/team?captainId=" + currentUser.id
  );

  const registered = document.getElementById("teamRegistered");
  const form = document.getElementById("teamForm");

  if (!result.registered) {
    registered.classList.add("hidden");
    form.classList.remove("hidden");
    return;
  }

  form.classList.add("hidden");
  registered.classList.remove("hidden");

  const names = result.members.map(m => escapeHtml(m.name)).join(", ");
  registered.innerHTML = `
    <div class="demo-box">
      <strong>${escapeHtml(result.team.teamName)}</strong><br>
      Members: ${names}
    </div>
  `;
}

async function loadMembers(search) {
  allMembers = await api(
    "/api/users?exclude=" + currentUser.id +
    "&search=" + encodeURIComponent(search)
  );
  renderMembers();
}

function renderMembers() {
  const list = document.getElementById("memberList");

  list.innerHTML = allMembers.map(member => `
    <label class="member-option">
      <input type="checkbox"
        value="${member.id}"
        ${selectedMembers.has(member.id) ? "checked" : ""}>
      <span>${escapeHtml(member.name)}<br>
        <small>${escapeHtml(member.email)}</small>
      </span>
    </label>
  `).join("");

  list.querySelectorAll("input[type=checkbox]").forEach(box => {
    box.addEventListener("change", () => {
      const id = Number(box.value);

      if (box.checked) {
        if (selectedMembers.size >= 2) {
          box.checked = false;
          alert("Only 2 members can be added with the captain.");
          return;
        }
        selectedMembers.add(id);
      } else {
        selectedMembers.delete(id);
      }
    });
  });
}

let memberTimer;
document.getElementById("memberSearch").addEventListener("input", e => {
  clearTimeout(memberTimer);
  memberTimer = setTimeout(() => loadMembers(e.target.value), 250);
});

document.getElementById("teamForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const message = document.getElementById("teamMessage");
  message.className = "";
  message.textContent = "";

  try {
    await api("/api/teams", {
      method: "POST",
      body: JSON.stringify({
        teamName: document.getElementById("teamName").value,
        eventId: currentEvent.id,
        captainId: currentUser.id,
        memberIds: Array.from(selectedMembers)
      })
    });

    message.className = "success";
    message.textContent = "Team registered successfully.";
    selectedMembers.clear();
    await loadTeam();
  } catch (err) {
    message.className = "error";
    message.textContent = err.message;
  }
});

document.getElementById("generateQrBtn").addEventListener("click", async () => {
  const resultBox = document.getElementById("qrResult");
  resultBox.innerHTML = "";

  try {
    const result = await api("/api/events/" + currentEvent.id + "/qr");
    resultBox.innerHTML = `
      <p><strong>Code:</strong> ${escapeHtml(result.code)}</p>
      <img src="${result.qr}" alt="Event QR code">
    `;
  } catch (err) {
    resultBox.innerHTML = '<p class="error">' + escapeHtml(err.message) + '</p>';
  }
});

document.getElementById("markAttendanceBtn").addEventListener("click", async () => {
  const message = document.getElementById("attendanceMessage");
  message.className = "";
  message.textContent = "";

  try {
    await api("/api/attendance", {
      method: "POST",
      body: JSON.stringify({
        eventId: currentEvent.id,
        userId: currentUser.id,
        code: document.getElementById("attendanceCode").value.trim()
      })
    });

    message.className = "success";
    message.textContent = "Attendance marked.";
  } catch (err) {
    message.className = "error";
    message.textContent = err.message;
  }
});

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

const saved = sessionStorage.getItem("nlscUser");
if (saved) {
  try {
    setUser(JSON.parse(saved));
  } catch (_) {
    setUser(null);
  }
} else {
  setUser(null);
}
