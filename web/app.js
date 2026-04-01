const occasionFilters = [
  { name: "Trinity", start: 1, end: 35 },
  { name: "Advent", start: 36, end: 45 },
  { name: "Jisu Sarijanai", start: 46, end: 81 },
  { name: "Bwswr Gwdan", start: 82, end: 96 },
  { name: "Epiphany", start: 97, end: 103 },
  { name: "Jisu ni Mung", start: 104, end: 129 },
  { name: "Jisu ni Onnai", start: 130, end: 142 },
  { name: "Jisu ni Anjart lajanai", start: 143, end: 144 },
  { name: "Jisu ni Maonai", start: 145, end: 157 },
  { name: "Mariam Niao Gaham Khourang", start: 158, end: 160 },
  { name: "Dalai Bonai", start: 161, end: 166 },
  { name: "Jisuni Swinai", start: 167, end: 181 },
  { name: "Jisuni Thwinai", start: 182, end: 195 },
  { name: "Phwrbuni Bhos", start: 196, end: 205 },
  { name: "Jisu ni Thangkhangphinnai", start: 206, end: 220 },
  { name: "Jisu ni Swrgwao Gakhwnai", start: 221, end: 226 },
  { name: "Gwthar Jiu", start: 227, end: 237 },
  { name: "Jisu ni Phwiphinnai", start: 238, end: 248 },
  { name: "Jisu ni Phwiphinnai ar Bisar", start: 249, end: 254 },
  { name: "Siri Mwnnai", start: 255, end: 258 },
  { name: "Baibel Sasthwr", start: 259, end: 266 },
  { name: "Gaham Batra", start: 267, end: 294 },
  { name: "Gaham Batra Phwsaonai", start: 295, end: 304 },
  { name: "Srang", start: 305, end: 312 },
  { name: "Gaham Laokhar", start: 313, end: 318 },
  { name: "Kristan Jibon", start: 319, end: 339 },
  { name: "Phwthaigraphwrni Borsa", start: 340, end: 361 },
  { name: "Phwthaigraphwrni Swnarnai", start: 362, end: 371 },
  { name: "Kristanphwrni Rongjanai", start: 372, end: 377 },
  { name: "Gwtharphwr", start: 378, end: 383 },
  { name: "Kristan Phwthainai", start: 384, end: 387 },
  { name: "Kristan Daoha", start: 388, end: 395 },
  { name: "Krus Nisan", start: 396, end: 402 },
  { name: "Phwgwmnai arw Phwtharjanai", start: 403, end: 410 },
  { name: "Hangkhrainai", start: 411, end: 435 },
  { name: "Borsa gwiywi Ha'", start: 436, end: 440 },
  { name: "Phap", start: 441, end: 447 },
  { name: "Gwsw Swlainai", start: 448, end: 453 },
  { name: "Baknainai", start: 454, end: 475 },
  { name: "Binai", start: 476, end: 504 },
  { name: "Deobar San / Girja", start: 505, end: 512 },
  { name: "Mondoli / Girja", start: 513, end: 521 },
  { name: "Kristanphwrni Anjart", start: 522, end: 525 },
  { name: "Baptisma", start: 526, end: 529 },
  { name: "Gothophwr", start: 530, end: 536 },
  { name: "Sunday School", start: 537, end: 541 },
  { name: "Jhwlao-Sikhla", start: 542, end: 581 },
  { name: "Phungbili", start: 582, end: 592 },
  { name: "Mwnabili", start: 593, end: 601 },
  { name: "Jothumnai", start: 602, end: 606 },
  { name: "Haba", start: 607, end: 613 },
  { name: "Thwinai arw Phobnai", start: 614, end: 618 },
  { name: "Dan / Sanda", start: 619, end: 626 },
  { name: "Hari", start: 627, end: 635 },
  { name: "Hadort", start: 636, end: 638 },
  { name: "Bidai", start: 639, end: 644 },
  { name: "Chorus", start: 645, end: 716 },
  { name: "Others", start: 717, end: Number.MAX_SAFE_INTEGER }
];

const state = {
  songs: [],
  tab: "home",
  filterPane: "letter",
  filterAnimating: false,
  themeAnimating: false,
  query: "",
  filterChar: null,
  filterOccasion: null,
  selectedSong: null,
  fontSize: Number(localStorage.getItem("fontSize") || 18),
  keepScreenOn: localStorage.getItem("keepScreenOn") === "true",
  favoriteIds: loadIdList("favoriteSongIds"),
  recentIds: loadIdList("recentSongIds"),
  themeMode: localStorage.getItem("themeMode") || "system",
  reduceEffectsManual: localStorage.getItem("reduceEffectsManual") === "true",
  autoReducedEffects: false,
  effectsReduced: false,
  deferredInstallPrompt: null,
  wakeLock: null,
  searchRenderTimer: null
};

const NATIVE_RELEASES_URL = "https://github.com/Badmaneers/bororwjabbilai/releases";
const NATIVE_RELEASES_API_LATEST = "https://api.github.com/repos/Badmaneers/bororwjabbilai/releases/latest";

const elements = {
  mainView: document.getElementById("mainView"),
  detailView: document.getElementById("detailView"),
  detailTitle: document.getElementById("detailTitle"),
  lyricsContainer: document.getElementById("lyricsContainer"),
  searchInput: document.getElementById("searchInput"),
  clearSearchBtn: document.getElementById("clearSearchBtn"),
  themeBtn: document.getElementById("themeBtn"),
  infoBtn: document.getElementById("infoBtn"),
  installBtn: document.getElementById("installBtn"),
  themeTransition: document.getElementById("themeTransition"),
  filterBtn: document.getElementById("filterBtn"),
  iosInstallTip: document.getElementById("iosInstallTip"),
  filterModal: document.getElementById("filterModal"),
  letterPane: document.getElementById("letterPane"),
  categoryPane: document.getElementById("categoryPane"),
  clearFilterBtn: document.getElementById("clearFilterBtn"),
  closeFilterBtn: document.getElementById("closeFilterBtn"),
  infoModal: document.getElementById("infoModal"),
  closeInfoBtn: document.getElementById("closeInfoBtn"),
  effectsToggle: document.getElementById("effectsToggle"),
  effectsStatus: document.getElementById("effectsStatus"),
  nativePromptModal: document.getElementById("nativePromptModal"),
  downloadNativeBtn: document.getElementById("downloadNativeBtn"),
  continueWebBtn: document.getElementById("continueWebBtn"),
  backBtn: document.getElementById("backBtn"),
  fontDownBtn: document.getElementById("fontDownBtn"),
  fontUpBtn: document.getElementById("fontUpBtn"),
  wakeBtn: document.getElementById("wakeBtn")
};

init();

async function init() {
  applyEffectsMode();
  applyTheme();
  applyFontSize();
  buildFilterControls();
  wireEvents();
  setupGestureSupport();
  setupAutoEffectsWatchers();
  syncSearchActions();
  showIosInstallTipIfNeeded();
  await loadSongs();
  scheduleSongSearchWarmup();
  render();
  maybeShowAndroidNativePrompt();
  registerServiceWorker();
}

function loadIdList(key) {
  try {
    const value = localStorage.getItem(key);
    if (!value) return [];
    const parsed = JSON.parse(value);
    if (!Array.isArray(parsed)) return [];
    return parsed.map(Number).filter(Number.isFinite);
  } catch {
    return [];
  }
}

function saveIdList(key, ids) {
  localStorage.setItem(key, JSON.stringify(ids));
}

async function loadSongs() {
  const response = await fetch("./data/song.json");
  if (!response.ok) throw new Error("Failed to load songs");
  const songs = await response.json();
  state.songs = songs.map((song) => ({
    ...song,
    _titleLower: (song.title || "").toLowerCase(),
    _categoryLower: (song.category || "").toLowerCase(),
    _lyricsText: null,
    _lyricsLower: null,
    _searchIndexed: false
  }));
}

function ensureSongSearchCache(song) {
  if (song._searchIndexed) return song;
  const lyricsText = flattenLyrics(song);
  song._lyricsText = lyricsText;
  song._lyricsLower = lyricsText.toLowerCase();
  song._searchIndexed = true;
  return song;
}

function scheduleSongSearchWarmup() {
  if (!state.songs.length) return;

  let index = 0;
  const chunkSize = 16;

  const processChunk = (deadline) => {
    while (index < state.songs.length) {
      if (deadline && typeof deadline.timeRemaining === "function" && deadline.timeRemaining() < 4) break;

      const end = Math.min(index + chunkSize, state.songs.length);
      for (; index < end; index += 1) {
        ensureSongSearchCache(state.songs[index]);
      }
    }

    if (index < state.songs.length) {
      queueWarmup(processChunk);
    }
  };

  queueWarmup(processChunk);
}

function queueWarmup(callback) {
  if ("requestIdleCallback" in window) {
    window.requestIdleCallback(callback, { timeout: 280 });
    return;
  }
  window.setTimeout(() => callback(null), 32);
}

function wireEvents() {
  document.querySelectorAll(".tab-btn").forEach((button) => {
    button.addEventListener("click", () => {
      switchTab(button.dataset.tab, true);
    });
  });

  elements.searchInput.addEventListener("input", (event) => {
    state.query = event.target.value;
    syncSearchActions();
    if (state.searchRenderTimer) {
      window.clearTimeout(state.searchRenderTimer);
    }
    state.searchRenderTimer = window.setTimeout(() => {
      render();
    }, 90);
  });

  elements.clearSearchBtn.addEventListener("click", () => {
    state.query = "";
    elements.searchInput.value = "";
    syncSearchActions();
    render();
  });

  elements.filterBtn.addEventListener("click", () => {
    openFilterModal("button");
  });

  elements.closeFilterBtn.addEventListener("click", () => {
    closeFilterModal();
  });

  elements.filterModal.addEventListener("click", (event) => {
    if (event.target === elements.filterModal) {
      closeFilterModal();
    }
  });

  elements.clearFilterBtn.addEventListener("click", () => {
    state.filterChar = null;
    state.filterOccasion = null;
    refreshFilterActiveStates();
    render();
  });

  document.querySelectorAll(".modal-tab[data-filter-tab]").forEach((tab) => {
    tab.addEventListener("click", () => {
      const pane = tab.dataset.filterTab;
      const direction = pane === "category" ? "left" : "right";
      setFilterPane(pane, { animate: true, direction });
    });
  });

  elements.infoBtn.addEventListener("click", () => elements.infoModal.classList.remove("hidden"));
  elements.closeInfoBtn.addEventListener("click", () => elements.infoModal.classList.add("hidden"));
  elements.effectsToggle?.addEventListener("change", (event) => {
    state.reduceEffectsManual = Boolean(event.target.checked);
    localStorage.setItem("reduceEffectsManual", String(state.reduceEffectsManual));
    applyEffectsMode();
  });
  elements.infoModal.addEventListener("click", (event) => {
    if (event.target === elements.infoModal) {
      elements.infoModal.classList.add("hidden");
    }
  });

  elements.downloadNativeBtn?.addEventListener("click", async () => {
    const downloadBtn = elements.downloadNativeBtn;
    if (!downloadBtn) return;

    const originalText = downloadBtn.textContent;
    downloadBtn.disabled = true;
    downloadBtn.textContent = "Checking latest APK...";

    try {
      const apkUrl = await getLatestReleasedApkUrl();
      window.open(apkUrl, "_blank", "noopener,noreferrer");
      dismissAndroidNativePrompt();
    } catch {
      window.open(NATIVE_RELEASES_URL, "_blank", "noopener,noreferrer");
      dismissAndroidNativePrompt();
    } finally {
      downloadBtn.disabled = false;
      downloadBtn.textContent = originalText;
    }
  });

  elements.continueWebBtn?.addEventListener("click", () => {
    dismissAndroidNativePrompt();
  });

  elements.nativePromptModal?.addEventListener("click", (event) => {
    if (event.target === elements.nativePromptModal) {
      dismissAndroidNativePrompt();
    }
  });

  elements.themeBtn.addEventListener("click", () => {
    const next = resolveDarkModeFor(state.themeMode) ? "light" : "dark";
    animateThemeModeChange(next);
  });

  elements.backBtn.addEventListener("click", closeDetail);

  elements.fontDownBtn.addEventListener("click", () => {
    state.fontSize = Math.max(12, state.fontSize - 2);
    localStorage.setItem("fontSize", String(state.fontSize));
    applyFontSize();
  });

  elements.fontUpBtn.addEventListener("click", () => {
    state.fontSize = Math.min(40, state.fontSize + 2);
    localStorage.setItem("fontSize", String(state.fontSize));
    applyFontSize();
  });

  elements.wakeBtn.addEventListener("click", toggleWakeLock);

  window.addEventListener("beforeinstallprompt", (event) => {
    event.preventDefault();
    state.deferredInstallPrompt = event;
    elements.installBtn.classList.remove("hidden");
  });

  elements.installBtn.addEventListener("click", async () => {
    if (!state.deferredInstallPrompt) return;
    state.deferredInstallPrompt.prompt();
    await state.deferredInstallPrompt.userChoice;
    state.deferredInstallPrompt = null;
    elements.installBtn.classList.add("hidden");
  });

  window.addEventListener("popstate", () => {
    if (state.selectedSong) closeDetail();
  });
}

function setupGestureSupport() {
  const interactiveSelector = "button, a, input, textarea, select, label";

  let mainTouchStartX = 0;
  let mainTouchStartY = 0;
  let mainTouchStartTime = 0;

  elements.mainView.addEventListener("touchstart", (event) => {
    if (event.touches.length !== 1) return;
    if (event.target.closest(interactiveSelector)) return;
    const touch = event.touches[0];
    mainTouchStartX = touch.clientX;
    mainTouchStartY = touch.clientY;
    mainTouchStartTime = Date.now();
  }, { passive: true });

  elements.mainView.addEventListener("touchend", (event) => {
    if (state.selectedSong || !mainTouchStartTime) return;
    const touch = event.changedTouches[0];
    const deltaX = touch.clientX - mainTouchStartX;
    const deltaY = touch.clientY - mainTouchStartY;
    const elapsed = Date.now() - mainTouchStartTime;
    mainTouchStartTime = 0;

    if (elapsed > 500) return;
    if (Math.abs(deltaX) < 60) return;
    if (Math.abs(deltaX) < Math.abs(deltaY) * 1.2) return;

    if (deltaX < 0) {
      switchToRelativeTab(1);
    } else {
      switchToRelativeTab(-1);
    }
  }, { passive: true });

  let detailTouchStartX = 0;
  let detailTouchStartY = 0;
  let detailTouchStartTime = 0;
  let detailSwipeDeltaX = 0;
  let detailSwipeDragging = false;

  elements.detailView.addEventListener("touchstart", (event) => {
    if (event.touches.length !== 1) return;
    const touch = event.touches[0];
    detailTouchStartX = touch.clientX;
    detailTouchStartY = touch.clientY;
    detailTouchStartTime = Date.now();
    detailSwipeDeltaX = 0;
    detailSwipeDragging = false;
    elements.detailView.classList.remove("detail-swipe-complete");
  }, { passive: true });

  elements.detailView.addEventListener("touchmove", (event) => {
    if (!state.selectedSong || !detailTouchStartTime) return;
    if (event.touches.length !== 1) return;

    const touch = event.touches[0];
    const deltaX = touch.clientX - detailTouchStartX;
    const deltaY = touch.clientY - detailTouchStartY;

    if (!detailSwipeDragging) {
      if (deltaX <= 0) return;
      if (Math.abs(deltaX) < 12) return;
      if (Math.abs(deltaX) < Math.abs(deltaY) * 1.15) return;
      detailSwipeDragging = true;
      elements.detailView.classList.add("detail-swipe-active");
    }

    detailSwipeDeltaX = Math.max(0, deltaX);
    elements.detailView.style.setProperty("--detail-swipe-x", `${detailSwipeDeltaX}px`);
    event.preventDefault();
  }, { passive: false });

  elements.detailView.addEventListener("touchend", (event) => {
    if (!state.selectedSong || !detailTouchStartTime) return;
    const touch = event.changedTouches[0];
    const deltaX = touch.clientX - detailTouchStartX;
    const deltaY = touch.clientY - detailTouchStartY;
    const elapsed = Date.now() - detailTouchStartTime;
    detailTouchStartTime = 0;

    if (detailSwipeDragging) {
      const velocityX = elapsed > 0 ? detailSwipeDeltaX / elapsed : 0;
      const shouldClose = detailSwipeDeltaX > window.innerWidth * 0.28 || velocityX > 0.55;
      if (shouldClose) {
        closeDetail({ animateSlide: true });
      } else {
        resetDetailSwipeState();
      }
      detailSwipeDragging = false;
      detailSwipeDeltaX = 0;
      return;
    }

    if (elapsed > 600) return;
    if (deltaX < 80) return;
    if (Math.abs(deltaX) < Math.abs(deltaY) * 1.3) return;

    closeDetail({ animateSlide: true });
  }, { passive: true });

  const homeTabButton = document.querySelector('.tab-btn[data-tab="home"]');
  if (!homeTabButton) return;

  let homeSwipeStartX = 0;
  let homeSwipeStartY = 0;
  let homeSwipeStartTime = 0;

  homeTabButton.addEventListener("touchstart", (event) => {
    if (event.touches.length !== 1) return;
    const touch = event.touches[0];
    homeSwipeStartX = touch.clientX;
    homeSwipeStartY = touch.clientY;
    homeSwipeStartTime = Date.now();
  }, { passive: true });

  homeTabButton.addEventListener("touchend", (event) => {
    if (state.tab !== "home" || state.selectedSong || !homeSwipeStartTime) return;
    const touch = event.changedTouches[0];
    const deltaX = touch.clientX - homeSwipeStartX;
    const deltaY = touch.clientY - homeSwipeStartY;
    const elapsed = Date.now() - homeSwipeStartTime;
    homeSwipeStartTime = 0;

    if (elapsed > 500) return;
    if (deltaY > -55) return;
    if (Math.abs(deltaY) < Math.abs(deltaX) * 1.1) return;

    openFilterModal("swipe");
  }, { passive: true });

  let filterTouchStartX = 0;
  let filterTouchStartY = 0;
  let filterTouchStartTime = 0;

  elements.filterModal.addEventListener("touchstart", (event) => {
    if (elements.filterModal.classList.contains("hidden")) return;
    if (event.touches.length !== 1) return;
    if (!event.target.closest("#letterPane, #categoryPane")) return;
    const touch = event.touches[0];
    filterTouchStartX = touch.clientX;
    filterTouchStartY = touch.clientY;
    filterTouchStartTime = Date.now();
  }, { passive: true });

  elements.filterModal.addEventListener("touchend", (event) => {
    if (!filterTouchStartTime || elements.filterModal.classList.contains("hidden")) return;
    const touch = event.changedTouches[0];
    const deltaX = touch.clientX - filterTouchStartX;
    const deltaY = touch.clientY - filterTouchStartY;
    const elapsed = Date.now() - filterTouchStartTime;
    filterTouchStartTime = 0;

    if (elapsed > 520) return;
    if (Math.abs(deltaX) < 50) return;
    if (Math.abs(deltaX) < Math.abs(deltaY) * 1.2) return;

    if (deltaX < 0 && state.filterPane === "letter") {
      setFilterPane("category", { animate: true, direction: "left" });
    } else if (deltaX > 0 && state.filterPane === "category") {
      setFilterPane("letter", { animate: true, direction: "right" });
    }
  }, { passive: true });
}

function openFilterModal(source = "button") {
  if (!elements.filterModal.classList.contains("hidden")) return;
  setFilterPane(state.filterPane, { animate: false });
  document.body.classList.add("filter-open");
  elements.filterModal.classList.remove("hidden");
  elements.filterModal.classList.remove("filter-open-swipe", "filter-open-button");
  const openClass = source === "swipe" ? "filter-open-swipe" : "filter-open-button";
  elements.filterModal.classList.add(openClass);
  window.setTimeout(() => {
    elements.filterModal.classList.remove(openClass);
  }, 420);
}

function closeFilterModal() {
  elements.filterModal.classList.add("hidden");
  document.body.classList.remove("filter-open");
}

function setFilterPane(pane, { animate = true, direction = "left" } = {}) {
  if (!pane || state.filterAnimating) return;
  if (pane === state.filterPane && animate) return;

  const currentPane = state.filterPane === "letter" ? elements.letterPane : elements.categoryPane;
  const nextPane = pane === "letter" ? elements.letterPane : elements.categoryPane;
  document.querySelectorAll(".modal-tab[data-filter-tab]").forEach((tab) => {
    tab.classList.toggle("active", tab.dataset.filterTab === pane);
  });

  if (!animate || pane === state.filterPane) {
    elements.letterPane.classList.toggle("hidden", pane !== "letter");
    elements.categoryPane.classList.toggle("hidden", pane !== "category");
    state.filterPane = pane;
    return;
  }

  state.filterAnimating = true;

  const outClass = direction === "left" ? "pane-out-left" : "pane-out-right";
  const inClass = direction === "left" ? "pane-in-right" : "pane-in-left";

  currentPane.classList.remove("pane-out-left", "pane-out-right");
  nextPane.classList.remove("pane-in-left", "pane-in-right");

  elements.filterModal.classList.add("pane-animating");
  currentPane.classList.add(outClass);

  window.setTimeout(() => {
    elements.letterPane.classList.toggle("hidden", pane !== "letter");
    elements.categoryPane.classList.toggle("hidden", pane !== "category");

    nextPane.classList.add(inClass);
    void nextPane.offsetWidth;
    nextPane.classList.remove(inClass);

    currentPane.classList.remove("pane-out-left", "pane-out-right");
    state.filterPane = pane;
    state.filterAnimating = false;
    elements.filterModal.classList.remove("pane-animating");
  }, 130);
}

function switchToRelativeTab(direction) {
  const tabOrder = ["saved", "home", "recents"];
  const currentIndex = tabOrder.indexOf(state.tab);
  if (currentIndex === -1) return;
  const nextIndex = currentIndex + direction;
  if (nextIndex < 0 || nextIndex >= tabOrder.length) return;
  switchTab(tabOrder[nextIndex], true);
}

function switchTab(tab, animate) {
  if (!tab || state.tab === tab) return;
  state.tab = tab;
  document.querySelectorAll(".tab-btn").forEach((button) => {
    const isActive = button.dataset.tab === tab;
    button.classList.toggle("active", isActive);
    if (isActive && animate) {
      playBottomTabBounce(button);
    }
  });
  render();
}

function playBottomTabBounce(button) {
  button.classList.remove("bounce");
  void button.offsetWidth;
  button.classList.add("bounce");
  window.setTimeout(() => {
    button.classList.remove("bounce");
  }, 540);
}

function setIconUse(target, iconId) {
  const useEl = target?.querySelector("use");
  if (!useEl) return;
  useEl.setAttribute("href", `#${iconId}`);
}

function resolveDarkModeFor(themeMode) {
  const systemDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
  return themeMode === "dark" || (themeMode === "system" && systemDark);
}

function animateThemeModeChange(nextThemeMode) {
  if (state.themeAnimating) return;

  const transitionLayer = elements.themeTransition;
  const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const currentUseDark = resolveDarkModeFor(state.themeMode);
  const nextUseDark = resolveDarkModeFor(nextThemeMode);

  if (!transitionLayer || prefersReducedMotion || state.effectsReduced) {
    state.themeMode = nextThemeMode;
    localStorage.setItem("themeMode", nextThemeMode);
    applyTheme();
    return;
  }

  state.themeAnimating = true;

  const buttonRect = elements.themeBtn.getBoundingClientRect();
  const originX = buttonRect.left + buttonRect.width / 2;
  const originY = buttonRect.top + buttonRect.height / 2;
  const maxDistanceX = Math.max(originX, window.innerWidth - originX);
  const maxDistanceY = Math.max(originY, window.innerHeight - originY);
  const rippleRadius = Math.hypot(maxDistanceX, maxDistanceY);

  transitionLayer.style.setProperty("--ripple-x", `${originX}px`);
  transitionLayer.style.setProperty("--ripple-y", `${originY}px`);
  transitionLayer.style.setProperty("--ripple-radius", `${Math.ceil(rippleRadius)}px`);

  transitionLayer.classList.remove("active", "animating", "from-light", "from-dark");
  void transitionLayer.offsetWidth;
  transitionLayer.classList.add("active", currentUseDark ? "from-dark" : "from-light", "animating");

  window.setTimeout(() => {
    state.themeMode = nextThemeMode;
    localStorage.setItem("themeMode", nextThemeMode);
    applyTheme();
  }, 90);

  window.setTimeout(() => {
    transitionLayer.classList.remove("animating", "active", "from-light", "from-dark");
    state.themeAnimating = false;
  }, 1180);
}

function applyTheme() {
  const useDark = resolveDarkModeFor(state.themeMode);
  document.documentElement.dataset.theme = useDark ? "dark" : "light";
  setIconUse(elements.themeBtn, useDark ? "i-moon" : "i-sun");
  const themeMeta = document.querySelector('meta[name="theme-color"]');
  if (themeMeta) {
    themeMeta.setAttribute("content", useDark ? "#0b111c" : "#edf2ff");
  }
}

function detectAutoReducedEffects() {
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    return true;
  }

  const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection;
  let score = 0;

  if (connection?.saveData) score += 2;

  const effectiveType = String(connection?.effectiveType || "").toLowerCase();
  if (effectiveType === "slow-2g" || effectiveType === "2g") score += 2;
  else if (effectiveType === "3g") score += 1;

  const deviceMemory = Number(navigator.deviceMemory || 0);
  if (deviceMemory > 0 && deviceMemory <= 2) score += 2;
  else if (deviceMemory > 0 && deviceMemory <= 4) score += 1;

  const cpuCores = Number(navigator.hardwareConcurrency || 0);
  if (cpuCores > 0 && cpuCores <= 2) score += 2;
  else if (cpuCores > 0 && cpuCores <= 4) score += 1;

  return score >= 2;
}

function applyEffectsMode() {
  state.autoReducedEffects = detectAutoReducedEffects();
  state.effectsReduced = state.reduceEffectsManual || state.autoReducedEffects;
  document.documentElement.dataset.effects = state.effectsReduced ? "off" : "on";
  syncEffectsUi();
}

function syncEffectsUi() {
  if (elements.effectsToggle) {
    elements.effectsToggle.checked = state.reduceEffectsManual;
  }

  if (!elements.effectsStatus) return;

  if (state.reduceEffectsManual) {
    elements.effectsStatus.textContent = "Manual mode: heavy effects reduced.";
    return;
  }

  elements.effectsStatus.textContent = state.autoReducedEffects
    ? "Auto mode: reduced effects enabled for this device."
    : "Auto mode: full effects enabled.";
}

function setupAutoEffectsWatchers() {
  const motionMedia = window.matchMedia("(prefers-reduced-motion: reduce)");
  const recheck = () => {
    if (state.reduceEffectsManual) {
      syncEffectsUi();
      return;
    }
    applyEffectsMode();
  };

  if (typeof motionMedia.addEventListener === "function") {
    motionMedia.addEventListener("change", recheck);
  } else if (typeof motionMedia.addListener === "function") {
    motionMedia.addListener(recheck);
  }

  const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection;
  if (connection && typeof connection.addEventListener === "function") {
    connection.addEventListener("change", recheck);
  }
}

function maybeShowAndroidNativePrompt() {
  const seenKey = "android_native_prompt_seen";
  const isAndroid = /android/i.test(navigator.userAgent || "");
  if (!isAndroid) return;
  if (!elements.nativePromptModal) return;
  if (sessionStorage.getItem(seenKey) === "1") return;
  sessionStorage.setItem(seenKey, "1");
  elements.nativePromptModal.classList.remove("hidden");
}

function dismissAndroidNativePrompt() {
  elements.nativePromptModal?.classList.add("hidden");
}

async function getLatestReleasedApkUrl() {
  const response = await fetch(NATIVE_RELEASES_API_LATEST, {
    method: "GET",
    headers: {
      Accept: "application/vnd.github+json"
    },
    cache: "no-store"
  });

  if (!response.ok) {
    throw new Error("Failed to fetch latest release metadata");
  }

  const release = await response.json();
  const assets = Array.isArray(release?.assets) ? release.assets : [];

  const apkAsset = assets
    .filter((asset) => typeof asset?.browser_download_url === "string")
    .find((asset) => /\.apk($|[?#])/i.test(asset.browser_download_url));

  if (!apkAsset?.browser_download_url) {
    throw new Error("No APK asset found in latest release");
  }

  return apkAsset.browser_download_url;
}

function syncSearchActions() {
  elements.clearSearchBtn.classList.toggle("hidden", !state.query.trim());
}

function applyFontSize() {
  document.documentElement.style.setProperty("--font-size", `${state.fontSize}px`);
}

function buildFilterControls() {
  const letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ@".split("");
  elements.letterPane.innerHTML = "";
  letters.forEach((value) => {
    const button = document.createElement("button");
    button.className = "letter-btn";
    button.textContent = value;
    button.dataset.value = value;
    button.addEventListener("click", () => {
      state.filterOccasion = null;
      state.filterChar = state.filterChar === value ? null : value;
      refreshFilterActiveStates();
      closeFilterModal();
      render();
    });
    elements.letterPane.appendChild(button);
  });

  elements.categoryPane.innerHTML = "";
  occasionFilters.forEach((occasion) => {
    const button = document.createElement("button");
    button.className = "category-btn";
    button.textContent = occasion.name;
    button.addEventListener("click", () => {
      state.filterChar = null;
      state.filterOccasion = state.filterOccasion?.name === occasion.name ? null : occasion;
      refreshFilterActiveStates();
      closeFilterModal();
      render();
    });
    elements.categoryPane.appendChild(button);
  });

  refreshFilterActiveStates();
}

function refreshFilterActiveStates() {
  document.querySelectorAll(".letter-btn").forEach((button) => {
    button.classList.toggle("active", state.filterChar === button.dataset.value);
  });
  document.querySelectorAll(".category-btn").forEach((button) => {
    button.classList.toggle("active", state.filterOccasion?.name === button.textContent);
  });
}

function getVisibleSongs() {
  const favorites = new Set(state.favoriteIds);
  const recentsOrder = new Map(state.recentIds.map((id, index) => [id, index]));

  let source = state.songs;
  if (state.tab === "saved") {
    source = state.songs.filter((song) => favorites.has(song.id));
  } else if (state.tab === "recents") {
    source = state.songs
      .filter((song) => recentsOrder.has(song.id))
      .sort((a, b) => recentsOrder.get(a.id) - recentsOrder.get(b.id));
  }

  if (state.filterOccasion) {
    source = source.filter((song) => song.id >= state.filterOccasion.start && song.id <= state.filterOccasion.end);
  } else if (state.filterChar) {
    if (state.filterChar === "@") {
      source = source.filter((song) => !/^[A-Za-z]/.test(song.category || ""));
    } else {
      const filterCharLower = state.filterChar.toLowerCase();
      source = source.filter((song) => song._categoryLower === filterCharLower);
    }
  }

  const rawQuery = state.query.trim();
  if (!rawQuery) return source;
  const queryLower = rawQuery.toLowerCase();

  return source
    .filter((song) => {
      if (
        song._titleLower.includes(queryLower) ||
        String(song.id).includes(rawQuery) ||
        song._categoryLower === queryLower
      ) {
        return true;
      }

      const indexedSong = ensureSongSearchCache(song);
      return (
        indexedSong._lyricsLower.includes(queryLower)
      );
    })
    .sort((a, b) => scoreSong(b, queryLower, rawQuery) - scoreSong(a, queryLower, rawQuery));
}

function scoreSong(song, queryLower, rawQuery) {
  const indexedSong = ensureSongSearchCache(song);
  const title = song._titleLower;
  const category = song._categoryLower;
  const lyrics = indexedSong._lyricsLower;
  let score = 0;
  if (title.startsWith(queryLower)) score += 40;
  if (title.includes(queryLower)) score += 20;
  if (category === queryLower) score += 15;
  if (lyrics.includes(queryLower)) score += 10;
  if (String(song.id).includes(rawQuery)) score += 8;
  return score;
}

function flattenLyrics(song) {
  return (song.lyrics || []).flatMap((section) => section.lines || []).join(" ");
}

function render() {
  const songs = getVisibleSongs();
  elements.mainView.innerHTML = "";
  const favoriteSet = new Set(state.favoriteIds);

  document.querySelectorAll(".tab-btn").forEach((button) => {
    const icon = button.querySelector(".tab-icon");
    if (!icon) return;
    const tab = button.dataset.tab;
    const active = button.classList.contains("active");
    if (tab === "saved") {
      setIconUse(icon, active ? "i-heart-fill" : "i-heart");
    }
  });

  if (!songs.length) {
    if (state.tab === "saved") {
      elements.mainView.innerHTML = `
        <section class="empty-state" aria-live="polite">
          <div class="empty-icon-wrap" aria-hidden="true">
            <svg class="empty-icon"><use href="#i-heart"></use></svg>
          </div>
          <h3 class="empty-title">No Favorites Yet</h3>
          <p class="empty-copy">Songs you save will appear here.</p>
        </section>
      `;
      return;
    }

    if (state.tab === "recents") {
      elements.mainView.innerHTML = `
        <section class="empty-state" aria-live="polite">
          <div class="empty-icon-wrap" aria-hidden="true">
            <svg class="empty-icon"><use href="#i-history"></use></svg>
          </div>
          <h3 class="empty-title">No Recents Yet</h3>
          <p class="empty-copy">Opened songs will show up here.</p>
        </section>
      `;
      return;
    }

    elements.mainView.innerHTML = '<p class="song-meta">No songs found.</p>';
    return;
  }

  const fragment = document.createDocumentFragment();

  songs.forEach((song) => {
    const card = document.createElement("article");
    card.className = "song-card";

    const favoriteActive = favoriteSet.has(song.id);
    const snippet = state.query ? getSnippet(song, state.query) : "";

    card.innerHTML = `
      <div class="song-top">
        <div class="song-main">
          <span class="song-index">${song.id}.</span>
          <h3 class="song-title">${escapeHtml(song.title || "Untitled")}</h3>
        </div>
        <button class="icon-btn favorite-btn ${favoriteActive ? "active" : ""}" data-fav="${song.id}" aria-label="Favorite"><svg class="icon-svg"><use href="#i-bookmark"></use></svg></button>
      </div>
      ${snippet ? `<p class="song-snippet">${escapeHtml(snippet)}</p>` : ""}
    `;

    card.addEventListener("click", (event) => {
      if (event.target.closest(".favorite-btn")) return;
      openDetail(song.id);
    });

    card.querySelector(".favorite-btn").addEventListener("click", (event) => {
      event.stopPropagation();
      toggleFavorite(song.id);
    });

    fragment.appendChild(card);
  });

  elements.mainView.appendChild(fragment);
}

function getSnippet(song, query) {
  if (!query || query.length < 2) return "";
  const text = ensureSongSearchCache(song)._lyricsText;
  const index = text.toLowerCase().indexOf(query.toLowerCase());
  if (index < 0) return "";
  const start = Math.max(0, index - 20);
  const end = Math.min(text.length, index + query.length + 30);
  return `${start > 0 ? "..." : ""}${text.slice(start, end)}${end < text.length ? "..." : ""}`;
}

function toggleFavorite(songId) {
  const exists = state.favoriteIds.includes(songId);
  state.favoriteIds = exists ? state.favoriteIds.filter((id) => id !== songId) : [...state.favoriteIds, songId];
  saveIdList("favoriteSongIds", state.favoriteIds);
  render();
}

function addRecent(songId) {
  const recents = state.recentIds.filter((id) => id !== songId);
  recents.unshift(songId);
  state.recentIds = recents.slice(0, 20);
  saveIdList("recentSongIds", state.recentIds);
}

function openDetail(songId) {
  const song = state.songs.find((entry) => entry.id === songId);
  if (!song) return;

  state.selectedSong = song;
  addRecent(songId);

  elements.detailTitle.textContent = song.title;
  elements.lyricsContainer.innerHTML = "";

  (song.lyrics || []).forEach((section) => {
    const row = document.createElement("section");
    row.className = "lyric-row";

    const number = document.createElement("div");
    number.className = "lyric-num";
    number.textContent = section.type !== "chorus" && section.number ? `${section.number}.` : "";

    const lines = document.createElement("div");
    if (section.type === "chorus") lines.classList.add("lyric-chorus");

    (section.lines || []).forEach((line) => {
      const p = document.createElement("p");
      p.textContent = line;
      p.style.margin = "0 0 6px";
      lines.appendChild(p);
    });

    row.append(number, lines);
    elements.lyricsContainer.appendChild(row);
  });

  updateWakeButton();
  resetDetailSwipeState();
  history.pushState({ detail: songId }, "", `#song-${songId}`);
  elements.detailView.classList.remove("hidden");
  elements.detailView.setAttribute("aria-hidden", "false");
}

function resetDetailSwipeState() {
  elements.detailView.classList.remove("detail-swipe-active", "detail-swipe-complete");
  elements.detailView.style.setProperty("--detail-swipe-x", "0px");
}

function closeDetail({ animateSlide = false } = {}) {
  if (animateSlide) {
    const width = window.innerWidth || 390;
    elements.detailView.classList.remove("detail-swipe-active");
    elements.detailView.classList.add("detail-swipe-complete");
    elements.detailView.style.setProperty("--detail-swipe-x", `${width + 24}px`);
    window.setTimeout(() => {
      finalizeCloseDetail();
    }, 190);
    return;
  }

  finalizeCloseDetail();
}

function finalizeCloseDetail() {
  state.selectedSong = null;
  resetDetailSwipeState();
  elements.detailView.classList.add("hidden");
  elements.detailView.setAttribute("aria-hidden", "true");
}

async function toggleWakeLock() {
  if (state.wakeLock) {
    await state.wakeLock.release();
    state.wakeLock = null;
    state.keepScreenOn = false;
    localStorage.setItem("keepScreenOn", "false");
    updateWakeButton();
    return;
  }

  if (!("wakeLock" in navigator)) {
    return;
  }

  try {
    state.wakeLock = await navigator.wakeLock.request("screen");
    state.keepScreenOn = true;
    localStorage.setItem("keepScreenOn", "true");
    state.wakeLock.addEventListener("release", () => {
      state.keepScreenOn = false;
      updateWakeButton();
    });
    updateWakeButton();
  } catch {
  }
}

function updateWakeButton() {
  setIconUse(elements.wakeBtn, state.keepScreenOn ? "i-lock" : "i-lock-open");
}

function showIosInstallTipIfNeeded() {
  const isIos = /iphone|ipad|ipod/i.test(navigator.userAgent);
  const isStandalone = window.matchMedia("(display-mode: standalone)").matches || window.navigator.standalone;
  if (isIos && !isStandalone) {
    elements.iosInstallTip.classList.remove("hidden");
    elements.iosInstallTip.textContent = "iOS install: Safari → Share → Add to Home Screen";
  }
}

async function registerServiceWorker() {
  if (!("serviceWorker" in navigator)) return;
  try {
    await navigator.serviceWorker.register("./service-worker.js");
  } catch {
  }
}

function escapeHtml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
