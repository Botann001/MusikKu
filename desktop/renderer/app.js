/**
 * ════════════════════════════════════════════════════════════════════════════
 * MusikKu Desktop — Application Controller ("Dial" Design)
 * Dark Emerald Glassmorphism Music Player
 * ════════════════════════════════════════════════════════════════════════════
 */

(function () {
  'use strict';

  // ─── 8 Preset Gradasi Sampul Playlist ──────────────────────────────────────
  const GRADIENT_PRESETS = [
    'linear-gradient(135deg, #38BDF8, #168CDE 60%, #0F5A96)', // Ocean Blue
    'linear-gradient(135deg, #7DD3FC, #0284C7 60%, #0369A1)', // Sky Ice
    'linear-gradient(135deg, #818CF8, #4F46E5 60%, #312E81)', // Indigo Wave
    'linear-gradient(135deg, #2DD4BF, #0D9488 60%, #115E59)', // Teal Aqua
    'linear-gradient(135deg, #FB7185, #E11D48 60%, #9F1239)', // Sunset Coral
    'linear-gradient(135deg, #FBBF24, #D97706 60%, #78350F)', // Amber Gold
    'linear-gradient(135deg, #C084FC, #9333EA 60%, #581C87)', // Violet Mist
    'linear-gradient(135deg, #34D399, #059669 60%, #064E3B)'  // Emerald Mint
  ];

  // ─── State ─────────────────────────────────────────────────────────────────
  const loadedFavItems = (() => {
    try {
      const raw = localStorage.getItem('musikku_fav_items');
      return raw ? new Map(JSON.parse(raw)) : new Map();
    } catch (e) {
      return new Map();
    }
  })();

  const state = {
    allSongs: [],
    onlineSongs: [],
    onlineCurrentQuery: '',
    onlineCurrentPage: 1,
    hasMoreOnline: true,
    isLoadingMoreOnline: false,
    downloadedSongs: JSON.parse(localStorage.getItem('musikku_downloads') || '[]'),
    playlists: JSON.parse(localStorage.getItem('musikku_playlists_v2') || '[]'),
    activePlaylistId: null,
    playlistSort: 'recent', // 'recent' | 'name'

    currentList: [],
    currentSongIndex: -1,
    currentSong: null,
    isPlaying: false,
    isShuffle: false,
    repeatMode: 0, // 0: off, 1: repeat-all, 2: repeat-one
    activeTab: 'library', // 'library' | 'online' | 'favorites' | 'downloads' | 'playlists'
    onlineSource: 'youtube', // 'youtube' | 'jamendo' (sama seperti aplikasi Android MusikKu)
    activeGenre: 'trending',
    searchQuery: '',

    favorites: new Set(JSON.parse(localStorage.getItem('musikku_favs') || '[]')),
    favoriteSongs: loadedFavItems,
    scannedFolders: JSON.parse(localStorage.getItem('musikku_folders') || '[]'),
    metadataCache: new Map(),
    volume: parseFloat(localStorage.getItem('musikku_vol') || '0.8'),
    isLoadingOnline: false,
    customCovers: JSON.parse(localStorage.getItem('musikku_custom_covers') || '{}'),

    isNowPlayingOpen: false,
    npActiveTab: 'lyrics', // 'lyrics' | 'queue'
    lyricsFontSize: 16,
    autoScrollLyrics: true
  };

  const audio = new Audio();
  audio.volume = state.volume;

  // ─── Migrasi Playlist Lama ke v2 ───────────────────────────────────────────
  // ─── Migrasi & Normalisasi Playlist v2 ─────────────────────────────────────
  function migratePlaylistsIfNeeded() {
    // Normalisasi struktur semua playlist di state agar selalu memiliki trackIds valid
    if (Array.isArray(state.playlists)) {
      state.playlists = state.playlists.map(pl => {
        const trackIds = Array.isArray(pl.trackIds) ? pl.trackIds : (Array.isArray(pl.songKeys) ? pl.songKeys : []);
        return {
          id: pl.id || ('pl_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4)),
          name: pl.name || 'Playlist',
          coverGradient: pl.coverGradient || GRADIENT_PRESETS[0],
          trackIds: trackIds,
          createdAt: pl.createdAt || Date.now(),
          updatedAt: pl.updatedAt || pl.createdAt || Date.now()
        };
      });
    } else {
      state.playlists = [];
    }

    const legacyPl = localStorage.getItem('musikku_playlists');
    const migrated = localStorage.getItem('musikku_pl_migrated');
    if (!migrated && legacyPl) {
      try {
        const oldSongs = JSON.parse(legacyPl);
        if (Array.isArray(oldSongs) && oldSongs.length > 0) {
          const trackIds = oldSongs.map(s => s.path || s.streamUrl).filter(Boolean);
          const initialPl = {
            id: 'pl_' + Date.now(),
            name: 'Playlist Saya',
            coverGradient: GRADIENT_PRESETS[0],
            trackIds: trackIds,
            createdAt: Date.now(),
            updatedAt: Date.now()
          };
          state.playlists = [initialPl];
          savePlaylists();
        }
      } catch (e) {
        console.warn('Playlist migration warning:', e);
      }
      localStorage.setItem('musikku_pl_migrated', 'true');
    }

    // Berikan contoh playlist awal jika masih kosong
    if (state.playlists.length === 0) {
      state.playlists = [
        {
          id: 'pl_default_1',
          name: 'Santai Malam',
          coverGradient: GRADIENT_PRESETS[0],
          trackIds: [],
          createdAt: Date.now() - 3000,
          updatedAt: Date.now() - 3000
        },
        {
          id: 'pl_default_2',
          name: 'Perjalanan',
          coverGradient: GRADIENT_PRESETS[1],
          trackIds: [],
          createdAt: Date.now() - 2000,
          updatedAt: Date.now() - 2000
        },
        {
          id: 'pl_default_3',
          name: 'Fokus',
          coverGradient: GRADIENT_PRESETS[2],
          trackIds: [],
          createdAt: Date.now() - 1000,
          updatedAt: Date.now() - 1000
        }
      ];
      savePlaylists();
    }
  }

  function savePlaylists() {
    if (Array.isArray(state.playlists)) {
      state.playlists.forEach(pl => {
        if (!Array.isArray(pl.trackIds)) {
          pl.trackIds = Array.isArray(pl.songKeys) ? pl.songKeys : [];
        }
      });
    }
    localStorage.setItem('musikku_playlists_v2', JSON.stringify(state.playlists));
  }

  function saveFavorites() {
    localStorage.setItem('musikku_favs', JSON.stringify(Array.from(state.favorites)));
    localStorage.setItem('musikku_fav_items', JSON.stringify(Array.from(state.favoriteSongs.entries())));
  }

  function saveDownloads() {
    localStorage.setItem('musikku_downloads', JSON.stringify(state.downloadedSongs));
  }

  function saveFolders() {
    localStorage.setItem('musikku_folders', JSON.stringify(state.scannedFolders));
  }

  // ─── DOM Elements Cache ────────────────────────────────────────────────────
  const elements = {
    // Window controls
    btnMinimize: document.getElementById('btn-minimize'),
    btnMaximize: document.getElementById('btn-maximize'),
    btnClose: document.getElementById('btn-close'),

    // Nav & Sidebar Resizing
    sidebarRail: document.getElementById('sidebar-rail'),
    sidebarResizer: document.getElementById('sidebar-resizer'),
    btnToggleSidebar: document.getElementById('btn-toggle-sidebar'),
    sidebarLogoBox: document.getElementById('sidebar-logo-box'),
    navButtons: document.querySelectorAll('.rail .nav'),
    tabViews: document.querySelectorAll('.tab-view'),
    btnAddFolder: document.getElementById('btn-add-folder'),
    btnOpenDlModal: document.getElementById('btn-open-dl-modal'),

    // Layar Perpustakaan
    viewLibrary: document.getElementById('view-library'),
    libSearchWrap: document.getElementById('lib-search-wrap'),
    librarySearch: document.getElementById('library-search'),
    libRecentPopover: document.getElementById('lib-recent-popover'),
    recentListLib: document.getElementById('recent-list-lib'),
    btnClearRecentLib: document.getElementById('btn-clear-recent-lib'),
    heroDial: document.getElementById('hero-dial'),
    dialArc: document.getElementById('dial-arc'),
    dialKnob: document.getElementById('dial-knob'),
    dialDisc: document.getElementById('dial-disc'),
    dialCoverImg: document.getElementById('dial-cover-img'),
    dialCenterIcon: document.getElementById('dial-center-icon'),
    heroTitle: document.getElementById('hero-title'),
    heroSubtitle: document.getElementById('hero-subtitle'),
    heroCurTime: document.getElementById('hero-cur-time'),
    heroDurTime: document.getElementById('hero-dur-time'),
    heroBtnShuffle: document.getElementById('hero-btn-shuffle'),
    heroBtnPrev: document.getElementById('hero-btn-prev'),
    heroBtnPlay: document.getElementById('hero-btn-play'),
    heroPlayIcon: document.getElementById('hero-play-icon'),
    heroBtnNext: document.getElementById('hero-btn-next'),
    heroBtnRepeat: document.getElementById('hero-btn-repeat'),
    heroRepeatIcon: document.getElementById('hero-repeat-icon'),
    heroBtnFav: document.getElementById('hero-btn-fav'),
    heroFavIcon: document.getElementById('hero-fav-icon'),
    heroVolume: document.getElementById('hero-volume'),
    heroVolVal: document.getElementById('hero-vol-val'),

    libCount: document.getElementById('lib-count'),
    libPlayAll: document.getElementById('lib-play-all'),
    libShuffle: document.getElementById('lib-shuffle'),
    koleksiNowCard: document.getElementById('koleksi-now-card'),
    kcTitle: document.getElementById('kc-title'),
    kcSub: document.getElementById('kc-sub'),
    cardBtnAddFolder: document.getElementById('card-btn-add-folder'),
    cardBtnUrlDl: document.getElementById('card-btn-url-dl'),
    libraryTrackContainer: document.getElementById('library-track-container'),
    libraryEmpty: document.getElementById('library-empty'),
    libraryTrackList: document.getElementById('library-track-list'),

    // Layar Jelajah Online
    viewOnline: document.getElementById('view-online'),
    onlineEyebrow: document.getElementById('online-eyebrow'),
    srcYt: document.getElementById('src-yt'),
    srcJamendo: document.getElementById('src-jamendo'),
    onlineSearchWrap: document.getElementById('online-search-wrap'),
    onlineSearchInput: document.getElementById('online-search-input'),
    onlineRecentPopover: document.getElementById('online-recent-popover'),
    recentListOnline: document.getElementById('recent-list-online'),
    btnClearRecentOnline: document.getElementById('btn-clear-recent-online'),
    activeTunerLabel: document.getElementById('active-tuner-label'),
    tunerStops: document.getElementById('tuner-stops'),
    onlinePlayAll: document.getElementById('online-play-all'),
    onlineShuffle: document.getElementById('online-shuffle'),
    onlineCount: document.getElementById('online-count'),
    onlineLoading: document.getElementById('online-loading'),
    onlineLoadingText: document.getElementById('online-loading-text'),
    onlineEmpty: document.getElementById('online-empty'),
    onlineEmptyText: document.getElementById('online-empty-text'),
    onlineTrackList: document.getElementById('online-track-list'),
    onlineLoadMoreWrap: document.getElementById('online-load-more-wrap'),
    btnOnlineLoadMore: document.getElementById('btn-online-load-more'),
    btnLoadMoreText: document.getElementById('btn-load-more-text'),
    btnLoadMoreSpinner: document.getElementById('btn-load-more-spinner'),

    // Layar Favorit
    viewFavorites: document.getElementById('view-favorites'),
    favSearchInput: document.getElementById('fav-search-input'),
    favPlayAll: document.getElementById('fav-play-all'),
    favShuffle: document.getElementById('fav-shuffle'),
    favCount: document.getElementById('fav-count'),
    favEmpty: document.getElementById('fav-empty'),
    favTrackList: document.getElementById('fav-track-list'),
    favGoOnline: document.getElementById('fav-go-online'),
    favGoLib: document.getElementById('fav-go-lib'),

    // Layar Unduhan
    viewDownloads: document.getElementById('view-downloads'),
    dlSearchInput: document.getElementById('dl-search-input'),
    dlPlayAll: document.getElementById('dl-play-all'),
    dlShuffle: document.getElementById('dl-shuffle'),
    btnOpenDlFolder: document.getElementById('btn-open-dl-folder'),
    dlCount: document.getElementById('dl-count'),
    dlStatsCount: document.getElementById('dl-stats-count'),
    dlStatsDur: document.getElementById('dl-stats-dur'),
    dlSortSelect: document.getElementById('dl-sort-select'),
    dlEmpty: document.getElementById('dl-empty'),
    dlTrackList: document.getElementById('dl-track-list'),
    railDlBadge: document.getElementById('rail-dl-badge'),

    // Layar Playlist
    viewPlaylists: document.getElementById('view-playlists'),
    playlistOverview: document.getElementById('playlist-overview'),
    plSearchInput: document.getElementById('pl-search-input'),
    btnNewPlaylist: document.getElementById('btn-new-playlist'),
    plSortRecent: document.getElementById('pl-sort-recent'),
    plSortName: document.getElementById('pl-sort-name'),
    plTotalCount: document.getElementById('pl-total-count'),
    playlistGrid: document.getElementById('playlist-grid'),

    playlistDetail: document.getElementById('playlist-detail'),
    btnBackPlaylists: document.getElementById('btn-back-playlists'),
    plDetailDisc: document.getElementById('pl-detail-disc'),
    plDetailSleeve: document.getElementById('pl-detail-sleeve'),
    plDetailTitle: document.getElementById('pl-detail-title'),
    plDetailMeta: document.getElementById('pl-detail-meta'),
    plDetailPlayAll: document.getElementById('pl-detail-play-all'),
    plDetailShuffle: document.getElementById('pl-detail-shuffle'),
    plDetailRename: document.getElementById('pl-detail-rename'),
    plDetailDelete: document.getElementById('pl-detail-delete'),
    plDetailTrackCount: document.getElementById('pl-detail-track-count'),
    plDetailTrackList: document.getElementById('pl-detail-track-list'),
    btnAddToPlPicker: document.getElementById('btn-add-to-pl-picker'),

    // Mini Player
    nowPlayingBar: document.getElementById('now-playing-bar'),
    miniDisc: document.getElementById('mini-disc'),
    miniCoverImg: document.getElementById('mini-cover-img'),
    miniCenterIcon: document.getElementById('mini-center-icon'),
    miniInfoWrap: document.getElementById('mini-info-wrap'),
    miniTitle: document.getElementById('mini-title'),
    miniArtist: document.getElementById('mini-artist'),
    miniFav: document.getElementById('mini-fav'),
    miniFavIcon: document.getElementById('mini-fav-icon'),
    miniShuffle: document.getElementById('mini-shuffle'),
    miniPrev: document.getElementById('mini-prev'),
    miniPlay: document.getElementById('mini-play'),
    miniPlayIcon: document.getElementById('mini-play-icon'),
    miniNext: document.getElementById('mini-next'),
    miniCurTime: document.getElementById('mini-cur-time'),
    miniDurTime: document.getElementById('mini-dur-time'),
    miniRulerBar: document.getElementById('mini-ruler-bar'),
    miniNeedle: document.getElementById('mini-needle'),
    miniRepeat: document.getElementById('mini-repeat'),
    miniRepeatIcon: document.getElementById('mini-repeat-icon'),
    miniVolSlider: document.getElementById('mini-vol-slider'),
    miniExpand: document.getElementById('mini-expand'),

    // Layar Sedang Diputar & Lirik Overlay ("Dial" Design)
    viewNowPlaying: document.getElementById('view-now-playing'),
    npBtnClose: document.getElementById('np-btn-close'),
    npSourceBadge: document.getElementById('np-source-badge'),
    npSourceText: document.getElementById('np-source-text'),
    npBtnToggleView: document.getElementById('np-btn-toggle-view'),
    npViewToggleIcon: document.getElementById('np-view-toggle-icon'),
    npBtnMore: document.getElementById('np-btn-more'),
    npDial: document.getElementById('np-dial'),
    npDialArc: document.getElementById('np-dial-arc'),
    npDialKnob: document.getElementById('np-dial-knob'),
    npDisc: document.getElementById('np-disc'),
    npCoverImg: document.getElementById('np-cover-img'),
    npDiscIcon: document.getElementById('np-disc-icon'),
    npBadgeType: document.getElementById('np-badge-type'),
    npTitle: document.getElementById('np-title'),
    npSubtitle: document.getElementById('np-subtitle'),
    npWaveformRuler: document.getElementById('np-waveform-ruler'),
    npWaveTicks: document.getElementById('np-wave-ticks'),
    npWaveNeedle: document.getElementById('np-wave-needle'),
    npCurTime: document.getElementById('np-cur-time'),
    npDurTime: document.getElementById('np-dur-time'),
    npBtnShuffle: document.getElementById('np-btn-shuffle'),
    npBtnPrev: document.getElementById('np-btn-prev'),
    npBtnPlay: document.getElementById('np-btn-play'),
    npPlayIcon: document.getElementById('np-play-icon'),
    npBtnNext: document.getElementById('np-btn-next'),
    npBtnRepeat: document.getElementById('np-btn-repeat'),
    npRepeatIcon: document.getElementById('np-repeat-icon'),
    npBtnFav: document.getElementById('np-btn-fav'),
    npFavIcon: document.getElementById('np-fav-icon'),
    npBtnDownload: document.getElementById('np-btn-download'),
    npBtnAddPl: document.getElementById('np-btn-add-pl'),
    npBtnChangeCover: document.getElementById('np-btn-change-cover'),
    npVolSlider: document.getElementById('np-vol-slider'),
    npTabQueue: document.getElementById('np-tab-queue'),
    npTabLyrics: document.getElementById('np-tab-lyrics'),
    npPanelLyrics: document.getElementById('np-panel-lyrics'),
    npLyricsContainer: document.getElementById('np-lyrics-container'),
    npLyricsLoading: document.getElementById('np-lyrics-loading'),
    npLyricsEmpty: document.getElementById('np-lyrics-empty'),
    npLyricsEmptyText: document.getElementById('np-lyrics-empty-text'),
    npLyricsList: document.getElementById('np-lyrics-list'),
    lrcBtnDec: document.getElementById('lrc-btn-dec'),
    lrcBtnInc: document.getElementById('lrc-btn-inc'),
    lrcBtnFollow: document.getElementById('lrc-btn-follow'),
    npPanelQueue: document.getElementById('np-panel-queue'),
    npQueueCurrentCard: document.getElementById('np-queue-current-card'),
    npQcTitle: document.getElementById('np-qc-title'),
    npQcArtist: document.getElementById('np-qc-artist'),
    npQueueCountLabel: document.getElementById('np-queue-count-label'),
    npQueueList: document.getElementById('np-queue-list'),
    npBtnSaveQueuePl: document.getElementById('np-btn-save-queue-pl'),
    npBtnClearQueue: document.getElementById('np-btn-clear-queue'),

    // Modal Tools Download URL
    downloadModal: document.getElementById('download-modal'),
    btnCloseModal: document.getElementById('btn-close-modal'),
    btnCancelModal: document.getElementById('btn-cancel-modal'),
    btnStartUrlDownload: document.getElementById('btn-start-url-download'),
    inputDlUrl: document.getElementById('input-dl-url'),
    inputDlTitle: document.getElementById('input-dl-title'),
    inputDlArtist: document.getElementById('input-dl-artist'),
    modalDlProgress: document.getElementById('modal-dl-progress'),
    modalProgressFill: document.getElementById('modal-progress-fill'),
    modalProgressText: document.getElementById('modal-progress-text'),

    // Modal Buat/Ubah Playlist
    playlistModal: document.getElementById('playlist-modal'),
    plModalTitle: document.getElementById('pl-modal-title'),
    inputPlName: document.getElementById('input-pl-name'),
    plGradientPicker: document.getElementById('pl-gradient-picker'),
    btnCancelPlModal: document.getElementById('btn-cancel-pl-modal'),
    btnClosePlModal: document.getElementById('btn-close-pl-modal'),
    btnSavePlaylist: document.getElementById('btn-save-playlist'),

    // Modal Tambah Lagu ke Playlist
    addToPlModal: document.getElementById('add-to-pl-modal'),
    btnClosePickerModal: document.getElementById('btn-close-picker-modal'),
    btnCancelPicker: document.getElementById('btn-cancel-picker'),
    pickerSearchInput: document.getElementById('picker-search-input'),
    pickerTrackList: document.getElementById('picker-track-list'),

    // Modal Konfirmasi Dialog
    confirmModal: document.getElementById('confirm-modal'),
    confirmModalBadge: document.getElementById('confirm-modal-badge'),
    confirmModalIcon: document.getElementById('confirm-modal-icon'),
    confirmModalTitle: document.getElementById('confirm-modal-title'),
    confirmModalSub: document.getElementById('confirm-modal-sub'),
    confirmModalMessage: document.getElementById('confirm-modal-message'),
    confirmCloseBtn: document.getElementById('btn-close-confirm-modal'),
    confirmBtnCancel: document.getElementById('btn-cancel-confirm'),
    confirmBtnAction: document.getElementById('btn-action-confirm'),
    confirmActionIcon: document.getElementById('confirm-action-icon'),
    confirmActionLabel: document.getElementById('confirm-action-label'),

    // Modal Tambah Lagu ke Playlist (Pilih Playlist)
    songToPlModal: document.getElementById('song-to-playlist-modal'),
    songPlModalTitle: document.getElementById('song-pl-modal-title'),
    songPlModalSub: document.getElementById('song-pl-modal-sub'),
    btnCloseSongPlModal: document.getElementById('btn-close-song-pl-modal'),
    btnSongPlNew: document.getElementById('btn-song-pl-new'),
    songPlList: document.getElementById('song-pl-list'),
    btnDoneSongPl: document.getElementById('btn-done-song-pl'),

    // Toast
    downloadToast: document.getElementById('download-toast'),
    toastTitle: document.getElementById('toast-title'),
    toastBarFill: document.getElementById('toast-bar-fill'),

    // Context Menu
    contextMenu: document.getElementById('track-context-menu'),
    ctxPlay: document.getElementById('ctx-play'),
    ctxFav: document.getElementById('ctx-fav'),
    ctxChangeCover: document.getElementById('ctx-change-cover'),
    ctxResetCover: document.getElementById('ctx-reset-cover'),
    ctxPlList: document.getElementById('ctx-pl-list'),
    ctxNewPl: document.getElementById('ctx-new-pl'),
    coverFileInput: document.getElementById('cover-file-input')
  };

  // ─── Helpers ───────────────────────────────────────────────────────────────
  function formatTime(seconds) {
    if (!seconds || isNaN(seconds) || !isFinite(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  }

  function formatTimeLong(seconds) {
    if (!seconds || isNaN(seconds) || !isFinite(seconds) || seconds <= 0) return '0 menit 0 detik';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    if (mins === 0) return `${secs} detik`;
    return `${mins} menit ${secs} detik`;
  }

  function getEffectiveDuration() {
    const songDur = Number(state.currentSong?.duration) || 0;
    const audioDur = (audio.duration && !isNaN(audio.duration) && isFinite(audio.duration) && audio.duration > 0) ? audio.duration : 0;

    // Untuk lagu online (YouTube/SoundCloud chunked stream), audio.duration hanya buffer sementara
    // Jika songDur sudah diketahui dari API metadata (misal 204 detik untuk 3:24), SELALU prioritaskan songDur!
    if (state.currentSong?.isOnline) {
      if (songDur > 0) return songDur;
      if (audioDur > 0) return audioDur;
      return 0;
    }

    // Untuk file lokal di komputer:
    if (audioDur > 0) return audioDur;
    if (songDur > 0) return songDur;
    return 0;
  }

  function getGradientStyle(str) {
    let hash = 0;
    const s = str || 'musikku';
    for (let i = 0; i < s.length; i++) {
      hash = (hash << 5) - hash + s.charCodeAt(i);
      hash |= 0;
    }
    const idx = Math.abs(hash) % GRADIENT_PRESETS.length;
    return GRADIENT_PRESETS[idx];
  }

  function formatAudioSrc(filePath) {
    if (!filePath) return '';
    if (filePath.startsWith('http://') || filePath.startsWith('https://') || filePath.startsWith('blob:') || filePath.startsWith('data:') || filePath.startsWith('file://')) {
      return filePath;
    }
    const normalized = filePath.replace(/\\/g, '/');
    const parts = normalized.split('/');
    const encoded = parts.map((part, idx) => {
      if (idx === 0 && /^[a-zA-Z]:$/.test(part)) return part;
      return encodeURIComponent(part);
    });
    return 'file:///' + encoded.filter(Boolean).join('/');
  }

  function getSongKey(song) {
    if (!song) return '';
    return song.id || song.sourceUrl || song.path || song.streamUrl || `${song.title || ''}_${song.artist || ''}`;
  }

  function getSongCover(song) {
    if (!song) return null;
    const key = getSongKey(song);
    if (state.customCovers && state.customCovers[key]) {
      return state.customCovers[key];
    }
    if (song.id && state.customCovers && state.customCovers[song.id]) {
      return state.customCovers[song.id];
    }
    if (song.path && state.customCovers && state.customCovers[song.path]) {
      return state.customCovers[song.path];
    }
    return song.cover || null;
  }

  function setCustomCover(song, dataUrl) {
    if (!song) return;
    const key = getSongKey(song);
    if (!state.customCovers) state.customCovers = {};
    if (dataUrl) {
      state.customCovers[key] = dataUrl;
      if (song.id) state.customCovers[song.id] = dataUrl;
      if (song.path) state.customCovers[song.path] = dataUrl;
      song.cover = dataUrl;
    } else {
      delete state.customCovers[key];
      if (song.id) delete state.customCovers[song.id];
      if (song.path) delete state.customCovers[song.path];
      if (song.originalCover !== undefined) {
        song.cover = song.originalCover;
      }
    }
    try {
      localStorage.setItem('musikku_custom_covers', JSON.stringify(state.customCovers));
    } catch (e) {
      console.warn('Gagal menyimpan custom cover ke localStorage:', e);
    }
    if (isSongFavorite(song)) {
      const fav = state.favoriteSongs.get(key) || (song.id && state.favoriteSongs.get(song.id));
      if (fav) {
        fav.cover = getSongCover(song);
        try {
          localStorage.setItem('musikku_fav_items', JSON.stringify(Array.from(state.favoriteSongs.entries())));
        } catch (err) {}
      }
    }
    updateDiscCoverUI();
    updateThumbnailsForSong(song);
  }

  function updateThumbnailsForSong(song) {
    if (!song) return;
    const songKey = getSongKey(song);
    const coverUrl = getSongCover(song);
    const escapedKey = (typeof CSS !== 'undefined' && CSS.escape) ? CSS.escape(songKey) : songKey.replace(/"/g, '\\"');
    const rows = document.querySelectorAll(`.track[data-key="${escapedKey}"]`);
    rows.forEach(row => {
      const thumb = row.querySelector('.disc-thumb');
      if (thumb) {
        if (coverUrl) {
          thumb.innerHTML = `<img src="${coverUrl}" alt="" />`;
        } else {
          thumb.innerHTML = `<div class="disc-fallback" style="background:${getGradientStyle(song.title || song.path)}"><span class="material-icons-round" style="font-size:22px;">music_note</span></div>`;
        }
      }
    });
  }

  function updateDiscCoverUI() {
    const currentSong = state.currentSong;
    const coverUrl = currentSong ? getSongCover(currentSong) : null;

    // 1. Hero Dial Disc
    if (elements.dialDisc && elements.dialCoverImg && elements.dialCenterIcon) {
      if (coverUrl) {
        elements.dialCoverImg.src = coverUrl;
        elements.dialCoverImg.classList.remove('hidden');
        elements.dialCenterIcon.classList.add('hidden');
        elements.dialDisc.classList.add('has-cover');
      } else {
        elements.dialCoverImg.src = '';
        elements.dialCoverImg.classList.add('hidden');
        elements.dialCenterIcon.classList.remove('hidden');
        elements.dialDisc.classList.remove('has-cover');
      }
    }

    // 2. Now Playing Big Disc
    if (elements.npDisc && elements.npCoverImg && elements.npDiscIcon) {
      if (coverUrl) {
        elements.npCoverImg.src = coverUrl;
        elements.npCoverImg.classList.remove('hidden');
        elements.npDiscIcon.classList.add('hidden');
        elements.npDisc.classList.add('has-cover');
      } else {
        elements.npCoverImg.src = '';
        elements.npCoverImg.classList.add('hidden');
        elements.npDiscIcon.classList.remove('hidden');
        elements.npDisc.classList.remove('has-cover');
      }
    }

    // 3. Mini Player Disc
    if (elements.miniDisc && elements.miniCoverImg && elements.miniCenterIcon) {
      if (coverUrl) {
        elements.miniCoverImg.src = coverUrl;
        elements.miniCoverImg.classList.remove('hidden');
        elements.miniCenterIcon.classList.add('hidden');
        elements.miniDisc.classList.add('has-cover');
      } else {
        elements.miniCoverImg.src = '';
        elements.miniCoverImg.classList.add('hidden');
        elements.miniCenterIcon.classList.remove('hidden');
        elements.miniDisc.classList.remove('has-cover');
      }
    }

    // 4. Koleksi active card (kc-disc)
    const kcDisc = document.querySelector('.kc-disc');
    if (kcDisc) {
      if (coverUrl) {
        kcDisc.innerHTML = `<img src="${coverUrl}" alt="" style="width:100%;height:100%;object-fit:cover;border-radius:50%;" />`;
      } else {
        kcDisc.innerHTML = `<span class="material-icons-round">music_note</span>`;
      }
    }

    // 5. Now Playing queue active card (np-qc-disc)
    const npQcDisc = document.querySelector('.np-qc-disc');
    if (npQcDisc) {
      if (coverUrl) {
        npQcDisc.innerHTML = `<img src="${coverUrl}" alt="" style="width:100%;height:100%;object-fit:cover;border-radius:50%;" />`;
      } else {
        npQcDisc.innerHTML = `<span class="material-icons-round">music_note</span>`;
      }
    }
  }

  async function pickAndChangeCover(targetSong) {
    const song = targetSong || state.currentSong;
    if (!song) {
      showDownloadToast('Pilih atau putar lagu terlebih dahulu untuk mengganti foto album', 100);
      hideDownloadToast(2500);
      return;
    }

    try {
      let dataUrl = null;
      if (window.elektronAPI && typeof window.elektronAPI.selectCoverImage === 'function') {
        dataUrl = await window.elektronAPI.selectCoverImage();
      } else {
        dataUrl = await new Promise((resolve) => {
          const input = elements.coverFileInput || document.getElementById('cover-file-input');
          if (!input) return resolve(null);
          input.value = '';
          input.onchange = () => {
            const file = input.files && input.files[0];
            if (!file) return resolve(null);
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result);
            reader.onerror = () => resolve(null);
            reader.readAsDataURL(file);
          };
          input.click();
        });
      }

      if (dataUrl) {
        if (song.originalCover === undefined) {
          song.originalCover = song.cover || null;
        }
        setCustomCover(song, dataUrl);
        showDownloadToast(`Foto album berhasil diperbarui untuk "${song.title || 'Lagu'}"`, 100);
        hideDownloadToast(2500);
      }
    } catch (err) {
      console.error('Error saat mengganti foto cover:', err);
      showDownloadToast('Gagal memuat foto album', 100);
      hideDownloadToast(2500);
    }
  }

  function setupDiscDragAndDrop(discEl, getTargetSongFn) {
    if (!discEl) return;

    discEl.addEventListener('dragenter', (e) => {
      e.preventDefault();
      e.stopPropagation();
      discEl.classList.add('drag-over');
    });

    discEl.addEventListener('dragover', (e) => {
      e.preventDefault();
      e.stopPropagation();
      discEl.classList.add('drag-over');
    });

    discEl.addEventListener('dragleave', (e) => {
      e.preventDefault();
      e.stopPropagation();
      discEl.classList.remove('drag-over');
    });

    discEl.addEventListener('drop', (e) => {
      e.preventDefault();
      e.stopPropagation();
      discEl.classList.remove('drag-over');

      const song = getTargetSongFn ? getTargetSongFn() : state.currentSong;
      if (!song) {
        showDownloadToast('Pilih lagu terlebih dahulu sebelum menambahkan foto', 100);
        hideDownloadToast(2500);
        return;
      }

      const files = e.dataTransfer && e.dataTransfer.files;
      if (files && files.length > 0) {
        const file = files[0];
        if (file.type && file.type.startsWith('image/')) {
          const reader = new FileReader();
          reader.onload = () => {
            if (song.originalCover === undefined) {
              song.originalCover = song.cover || null;
            }
            setCustomCover(song, reader.result);
            showDownloadToast(`Foto album berhasil diperbarui: "${song.title || 'Lagu'}"`, 100);
            hideDownloadToast(2500);
          };
          reader.readAsDataURL(file);
        } else {
          showDownloadToast('Harap gunakan file gambar (JPG, PNG, WebP)', 100);
          hideDownloadToast(2500);
        }
      }
    });
  }

  function isSongDownloaded(song) {
    if (!song) return false;
    const key = getSongKey(song);
    return state.downloadedSongs.some(d => getSongKey(d) === key || (song.path && d.path === song.path) || (song.id && d.id === song.id));
  }

  function isSongFavorite(song) {
    if (!song) return false;
    const key = getSongKey(song);
    return state.favoriteSongs.has(key) ||
           state.favorites.has(key) ||
           (song.path && (state.favoriteSongs.has(song.path) || state.favorites.has(song.path))) ||
           (song.streamUrl && (state.favoriteSongs.has(song.streamUrl) || state.favorites.has(song.streamUrl))) ||
           (song.id && (state.favoriteSongs.has(song.id) || state.favorites.has(song.id)));
  }

  function getAllKnownSongs() {
    const map = new Map();
    state.allSongs.forEach(s => map.set(getSongKey(s), s));
    state.downloadedSongs.forEach(s => map.set(getSongKey(s), s));
    state.favoriteSongs.forEach(s => map.set(getSongKey(s), s));
    state.onlineSongs.forEach(s => map.set(getSongKey(s), s));
    return Array.from(map.values());
  }

  function findSongByKey(key) {
    if (!key) return null;
    return getAllKnownSongs().find(s => getSongKey(s) === key || s.path === key || s.streamUrl === key || s.id === key) || null;
  }

  function syncLegacyFavorites() {
    if (state.favorites.size > 0 && state.allSongs.length > 0) {
      let changed = false;
      for (const song of state.allSongs) {
        const key = getSongKey(song);
        if (state.favorites.has(key) || (song.path && state.favorites.has(song.path))) {
          if (!state.favoriteSongs.has(key)) {
            state.favoriteSongs.set(key, song);
            changed = true;
          }
        }
      }
      if (changed) saveFavorites();
    }
  }

  // ─── Window Controls ───────────────────────────────────────────────────────
  elements.btnMinimize?.addEventListener('click', () => window.elektronAPI?.minimize());
  elements.btnMaximize?.addEventListener('click', () => window.elektronAPI?.maximize());
  elements.btnClose?.addEventListener('click', () => window.elektronAPI?.close());

  // ─── Tab Navigation ────────────────────────────────────────────────────────
  function switchTab(tabName) {
    state.activeTab = tabName;
    state.searchQuery = '';

    elements.navButtons.forEach(btn => {
      const isCurrent = btn.dataset.tab === tabName;
      btn.classList.toggle('active', isCurrent);
      if (isCurrent) {
        btn.setAttribute('aria-current', 'page');
      } else {
        btn.removeAttribute('aria-current');
      }
    });

    elements.tabViews.forEach(view => {
      const match = view.id === `view-${tabName}`;
      view.classList.toggle('hidden', !match);
    });

    // Update visibility of bottom mini player
    // Note: on Perpustakaan, hero dial is already prominent; on other tabs, mini player shows if song is active
    updateMiniPlayerVisibility();

    // Render tab content
    if (tabName === 'library') {
      applyFilterAndRenderLibrary();
    } else if (tabName === 'online') {
      if (elements.tunerStops && elements.tunerStops.children.length === 0) {
        renderTunerStops();
      }
      if (state.onlineSongs.length === 0) {
        const defaultGenres = ONLINE_GENRES[state.onlineSource] || ONLINE_GENRES.youtube;
        const activeItem = defaultGenres.find(g => g.id === state.activeGenre) || defaultGenres[0];
        fetchOnlineTracks(activeItem.query);
      } else {
        renderOnlineTracks();
      }
    } else if (tabName === 'favorites') {
      renderFavorites();
    } else if (tabName === 'downloads') {
      renderDownloads();
    } else if (tabName === 'playlists') {
      showPlaylistOverview();
    }
  }

  elements.navButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const tab = btn.dataset.tab;
      if (tab) switchTab(tab);
    });
  });

  // ─── Sidebar Resizer & Compact Mode Logic ─────────────────────────────────
  const SIDEBAR_MIN_WIDTH = 180;
  const SIDEBAR_MAX_WIDTH = 460;
  const SIDEBAR_DEFAULT_WIDTH = 240;
  const SIDEBAR_COMPACT_WIDTH = 72;
  const SIDEBAR_SNAP_THRESHOLD = 135;
  const STORAGE_KEY_SIDEBAR_WIDTH = 'musikku_sidebar_width';

  function applySidebarWidth(width, save = true) {
    if (!elements.sidebarRail) return;
    const isCompact = width < SIDEBAR_SNAP_THRESHOLD;
    let finalWidth;
    if (isCompact) {
      finalWidth = SIDEBAR_COMPACT_WIDTH;
      elements.sidebarRail.classList.add('compact');
    } else {
      finalWidth = Math.min(SIDEBAR_MAX_WIDTH, Math.max(SIDEBAR_MIN_WIDTH, width));
      elements.sidebarRail.classList.remove('compact');
    }

    elements.sidebarRail.style.width = `${finalWidth}px`;
    document.documentElement.style.setProperty('--sidebar-width', `${finalWidth}px`);

    if (elements.btnToggleSidebar) {
      elements.btnToggleSidebar.title = isCompact ? 'Perbesar sidebar' : 'Perkecil sidebar';
      elements.btnToggleSidebar.setAttribute('aria-label', isCompact ? 'Perbesar sidebar' : 'Perkecil sidebar');
      const icon = elements.btnToggleSidebar.querySelector('.material-icons-round');
      if (icon) {
        icon.textContent = isCompact ? 'menu' : 'menu_open';
      }
    }

    if (save) {
      try {
        localStorage.setItem(STORAGE_KEY_SIDEBAR_WIDTH, String(finalWidth));
      } catch (_) {}
    }
  }

  function toggleSidebarCompact() {
    if (!elements.sidebarRail) return;
    const isCurrentlyCompact = elements.sidebarRail.classList.contains('compact') ||
      elements.sidebarRail.offsetWidth <= SIDEBAR_SNAP_THRESHOLD;

    if (isCurrentlyCompact) {
      let saved = parseInt(localStorage.getItem(STORAGE_KEY_SIDEBAR_WIDTH) || '0', 10);
      if (!saved || saved < SIDEBAR_SNAP_THRESHOLD) {
        saved = SIDEBAR_DEFAULT_WIDTH;
      }
      applySidebarWidth(saved, true);
    } else {
      applySidebarWidth(SIDEBAR_COMPACT_WIDTH, true);
    }
  }

  function setupSidebarResizer() {
    if (!elements.sidebarRail || !elements.sidebarResizer) return;

    // Restore saved width from localStorage
    try {
      const savedWidth = parseInt(localStorage.getItem(STORAGE_KEY_SIDEBAR_WIDTH) || '', 10);
      if (savedWidth && !isNaN(savedWidth)) {
        applySidebarWidth(savedWidth, false);
      }
    } catch (_) {}

    let isDragging = false;
    let startX = 0;
    let startWidth = 0;

    elements.sidebarResizer.addEventListener('mousedown', (e) => {
      if (e.button !== 0) return;
      isDragging = true;
      startX = e.clientX;
      startWidth = elements.sidebarRail.getBoundingClientRect().width;

      document.body.classList.add('resizing-sidebar');
      elements.sidebarResizer.classList.add('active');

      const onMouseMove = (moveEvent) => {
        if (!isDragging) return;
        const currentX = moveEvent.clientX;
        const newWidth = startWidth + (currentX - startX);
        applySidebarWidth(newWidth, false);
      };

      const onMouseUp = () => {
        if (!isDragging) return;
        isDragging = false;
        document.body.classList.remove('resizing-sidebar');
        elements.sidebarResizer.classList.remove('active');
        window.removeEventListener('mousemove', onMouseMove);
        window.removeEventListener('mouseup', onMouseUp);

        const finalWidth = elements.sidebarRail.getBoundingClientRect().width;
        applySidebarWidth(finalWidth, true);
      };

      window.addEventListener('mousemove', onMouseMove);
      window.addEventListener('mouseup', onMouseUp);
      e.preventDefault();
    });

    // Double click pada garis resizer untuk reset ke default atau toggle
    elements.sidebarResizer.addEventListener('dblclick', () => {
      const currentWidth = elements.sidebarRail.getBoundingClientRect().width;
      if (currentWidth <= SIDEBAR_SNAP_THRESHOLD) {
        applySidebarWidth(SIDEBAR_DEFAULT_WIDTH, true);
      } else {
        applySidebarWidth(SIDEBAR_COMPACT_WIDTH, true);
      }
    });

    // Tombol toggle icon di samping brand MusikKu
    elements.btnToggleSidebar?.addEventListener('click', (e) => {
      e.stopPropagation();
      toggleSidebarCompact();
    });

    // Klik kotak logo musik ketika sidebar compact untuk membuka kembali
    elements.sidebarLogoBox?.addEventListener('click', () => {
      if (elements.sidebarRail.classList.contains('compact')) {
        toggleSidebarCompact();
      }
    });
  }

  setupSidebarResizer();

  function updateMiniPlayerVisibility() {
    if (!state.currentSong) {
      elements.nowPlayingBar?.classList.add('hidden');
      return;
    }
    // Show mini player everywhere when a song is playing / loaded
    elements.nowPlayingBar?.classList.remove('hidden');
  }

  // ─── Dial Renderer & Interaction (Perpustakaan) ───────────────────────────
  const DIAL_R = 190;
  const DIAL_C = 2 * Math.PI * DIAL_R; // ~1193.8

  function renderDialProgress(p) {
    if (!elements.dialArc || !elements.dialKnob) return;
    const progress = Math.max(0, Math.min(1, p));
    elements.dialArc.setAttribute('stroke-dasharray', `${progress * DIAL_C} ${DIAL_C}`);
    const angle = progress * 2 * Math.PI; // sudut searah jarum jam dari jam 12
    const cx = 220 + DIAL_R * Math.sin(angle);
    const cy = 220 - DIAL_R * Math.cos(angle);
    elements.dialKnob.setAttribute('cx', cx.toFixed(2));
    elements.dialKnob.setAttribute('cy', cy.toFixed(2));
  }

  // Dial Click to Seek
  let isDialDragging = false;
  function handleDialSeek(e) {
    const dur = getEffectiveDuration();
    if (!dur || !elements.heroDial) return;
    const rect = elements.heroDial.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    const dx = e.clientX - centerX;
    const dy = e.clientY - centerY;

    let rad = Math.atan2(dx, -dy); // 0 at top (jam 12), searah jarum jam
    if (rad < 0) rad += 2 * Math.PI;
    const frac = rad / (2 * Math.PI);
    audio.currentTime = frac * dur;
    renderDialProgress(frac);
  }

  elements.heroDial?.addEventListener('mousedown', (e) => {
    isDialDragging = true;
    handleDialSeek(e);
  });
  window.addEventListener('mousemove', (e) => {
    if (isDialDragging) handleDialSeek(e);
  });
  window.addEventListener('mouseup', () => {
    isDialDragging = false;
  });

  // ─── Mini Ruler Progress Interaction ──────────────────────────────────────
  let isRulerDragging = false;
  function handleRulerSeek(e) {
    const dur = getEffectiveDuration();
    if (!dur || !elements.miniRulerBar) return;
    const rect = elements.miniRulerBar.getBoundingClientRect();
    const frac = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    audio.currentTime = frac * dur;
    elements.miniNeedle.style.left = `${frac * 100}%`;
  }

  elements.miniRulerBar?.addEventListener('mousedown', (e) => {
    isRulerDragging = true;
    handleRulerSeek(e);
  });
  window.addEventListener('mousemove', (e) => {
    if (isRulerDragging) handleRulerSeek(e);
  });
  window.addEventListener('mouseup', () => {
    isRulerDragging = false;
  });

  // ─── Now Playing Screen Dial Renderer & Interaction ───────────────────────
  const NP_DIAL_R = 162;
  const NP_DIAL_C = 2 * Math.PI * NP_DIAL_R; // ~1017.88

  function renderNpDialProgress(p) {
    if (!elements.npDialArc || !elements.npDialKnob) return;
    const progress = Math.max(0, Math.min(1, p));
    elements.npDialArc.setAttribute('stroke-dasharray', `${progress * NP_DIAL_C} ${NP_DIAL_C}`);
    const angle = progress * 2 * Math.PI;
    const cx = 190 + NP_DIAL_R * Math.sin(angle);
    const cy = 190 - NP_DIAL_R * Math.cos(angle);
    elements.npDialKnob.setAttribute('cx', cx.toFixed(2));
    elements.npDialKnob.setAttribute('cy', cy.toFixed(2));
  }

  let isNpDialDragging = false;
  function handleNpDialSeek(e) {
    const dur = getEffectiveDuration();
    if (!dur || !elements.npDial) return;
    const rect = elements.npDial.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    const dx = e.clientX - centerX;
    const dy = e.clientY - centerY;

    let rad = Math.atan2(dx, -dy);
    if (rad < 0) rad += 2 * Math.PI;
    const frac = rad / (2 * Math.PI);
    audio.currentTime = frac * dur;
    renderNpDialProgress(frac);
    updateWaveformProgress(frac);
  }

  elements.npDial?.addEventListener('mousedown', (e) => {
    isNpDialDragging = true;
    handleNpDialSeek(e);
  });
  window.addEventListener('mousemove', (e) => {
    if (isNpDialDragging) handleNpDialSeek(e);
  });
  window.addEventListener('mouseup', () => {
    isNpDialDragging = false;
  });

  // ─── Waveform Soundwave Ruler (Now Playing Screen) ─────────────────────────
  const WAVE_HEIGHTS = [
    16, 22, 18, 28, 38, 44, 36, 48, 52, 44, 38, 48, 56, 50, 42, 36, 48, 52, 46, 38,
    44, 50, 48, 38, 42, 48, 52, 46, 38, 44, 52, 56, 48, 42, 36, 48, 50, 44, 38, 44,
    48, 42, 36, 28, 22, 26, 20, 16, 24, 18
  ];

  function initWaveformTicks() {
    if (!elements.npWaveTicks || elements.npWaveTicks.children.length > 0) return;
    elements.npWaveTicks.innerHTML = '';
    WAVE_HEIGHTS.forEach((h) => {
      const bar = document.createElement('div');
      bar.className = 'np-tick-bar';
      bar.style.height = `${Math.round((h / 56) * 36) + 8}px`;
      elements.npWaveTicks.appendChild(bar);
    });
  }

  function updateWaveformProgress(frac) {
    if (!elements.npWaveTicks || !elements.npWaveNeedle) return;
    const clamped = Math.max(0, Math.min(1, frac));
    elements.npWaveNeedle.style.left = `${clamped * 100}%`;
    const totalBars = elements.npWaveTicks.children.length;
    const activeCount = Math.floor(clamped * totalBars);
    for (let i = 0; i < totalBars; i++) {
      elements.npWaveTicks.children[i].classList.toggle('active', i <= activeCount);
    }
  }

  let isWaveDragging = false;
  function handleWaveSeek(e) {
    const dur = getEffectiveDuration();
    if (!dur || !elements.npWaveformRuler) return;
    const rect = elements.npWaveformRuler.getBoundingClientRect();
    const frac = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    audio.currentTime = frac * dur;
    updateWaveformProgress(frac);
    renderNpDialProgress(frac);
  }

  elements.npWaveformRuler?.addEventListener('mousedown', (e) => {
    isWaveDragging = true;
    handleWaveSeek(e);
  });
  window.addEventListener('mousemove', (e) => {
    if (isWaveDragging) handleWaveSeek(e);
  });
  window.addEventListener('mouseup', () => {
    isWaveDragging = false;
  });

  // ─── Lyrics Management & LRCLIB Integration ────────────────────────────────
  let currentLyrics = {
    songKey: '',
    lines: [],
    plain: '',
    source: ''
  };
  let activeLyricIdx = -1;

  async function loadLyricsForSong(song) {
    if (!song) return;
    const songKey = getSongKey(song);
    if (currentLyrics.songKey === songKey && currentLyrics.lines.length > 0) {
      renderLyricsList();
      return;
    }

    currentLyrics = { songKey, lines: [], plain: '', source: '' };
    activeLyricIdx = -1;
    if (elements.npLyricsList) elements.npLyricsList.innerHTML = '';
    elements.npLyricsLoading?.classList.remove('hidden');
    elements.npLyricsEmpty?.classList.add('hidden');

    try {
      const res = await window.elektronAPI?.getLyrics({
        title: song.title || '',
        artist: song.artist || '',
        duration: song.duration || (audio.duration || 0)
      });

      elements.npLyricsLoading?.classList.add('hidden');

      if (!res || (!res.syncedLyrics && !res.plainLyrics)) {
        elements.npLyricsEmpty?.classList.remove('hidden');
        if (elements.npLyricsEmptyText) {
          elements.npLyricsEmptyText.textContent = `Lirik belum tersedia untuk "${song.title || 'lagu ini'}"`;
        }
        return;
      }

      const parsedLines = [];
      if (res.syncedLyrics) {
        const rawLines = res.syncedLyrics.split('\n');
        const reg = /^\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]\s*(.*)$/;
        for (const line of rawLines) {
          const m = line.match(reg);
          if (m) {
            const timeSec = parseInt(m[1], 10) * 60 + parseInt(m[2], 10) + (m[3] ? parseFloat('0.' + m[3]) : 0);
            const text = (m[4] || '').trim();
            if (text) parsedLines.push({ timeSec, text });
          }
        }
        parsedLines.sort((a, b) => a.timeSec - b.timeSec);
      }

      if (parsedLines.length === 0 && res.plainLyrics) {
        const plainArr = res.plainLyrics.split('\n').map(l => l.trim()).filter(Boolean);
        const approxStep = (song.duration || audio.duration || 180) / Math.max(1, plainArr.length);
        plainArr.forEach((text, i) => {
          parsedLines.push({ timeSec: i * approxStep, text });
        });
      }

      currentLyrics = {
        songKey,
        lines: parsedLines,
        plain: res.plainLyrics || '',
        source: res.source || 'LRCLIB'
      };

      if (parsedLines.length === 0) {
        elements.npLyricsEmpty?.classList.remove('hidden');
      } else {
        renderLyricsList();
        syncLyrics(audio.currentTime);
      }
    } catch (e) {
      elements.npLyricsLoading?.classList.add('hidden');
      elements.npLyricsEmpty?.classList.remove('hidden');
    }
  }

  function renderLyricsList() {
    if (!elements.npLyricsList) return;
    elements.npLyricsList.innerHTML = '';
    currentLyrics.lines.forEach((line, idx) => {
      const row = document.createElement('div');
      row.className = 'lrc-row future';
      row.dataset.idx = idx;

      const textEl = document.createElement('span');
      textEl.className = 'lrc-text';
      textEl.textContent = line.text;

      row.appendChild(textEl);

      row.addEventListener('click', () => {
        audio.currentTime = line.timeSec;
        syncLyrics(line.timeSec);
      });

      elements.npLyricsList.appendChild(row);
    });
  }

  function syncLyrics(currentTime) {
    if (!elements.npLyricsList || currentLyrics.lines.length === 0) return;
    let newIdx = -1;
    for (let i = 0; i < currentLyrics.lines.length; i++) {
      if (currentTime >= currentLyrics.lines[i].timeSec) {
        newIdx = i;
      } else {
        break;
      }
    }

    if (newIdx === activeLyricIdx) return;
    activeLyricIdx = newIdx;

    const rows = elements.npLyricsList.children;
    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];
      if (i === activeLyricIdx) {
        row.className = 'lrc-row active';
        if (state.autoScrollLyrics) {
          row.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      } else if (i < activeLyricIdx) {
        row.className = 'lrc-row past';
      } else {
        row.className = 'lrc-row future';
      }
    }
  }

  // ─── Queue Management (Now Playing Screen) ─────────────────────────────────
  function renderQueue() {
    if (!elements.npQueueCurrentCard || !elements.npQueueList) return;

    if (state.currentSong) {
      elements.npQcTitle.textContent = state.currentSong.title || '—';
      elements.npQcArtist.textContent = state.currentSong.artist || '—';
    }

    const upcoming = state.currentList.slice(state.currentSongIndex + 1);
    if (elements.npQueueCountLabel) {
      elements.npQueueCountLabel.textContent = `BERIKUTNYA · ${upcoming.length} LAGU`;
    }

    elements.npQueueList.innerHTML = '';
    if (upcoming.length === 0) {
      const empty = document.createElement('div');
      empty.className = 'empty-queue-msg';
      empty.style.cssText = 'padding: 24px 12px; text-align: center; color: var(--muted); font-size: 13px;';
      empty.textContent = 'Tidak ada lagu berikutnya dalam antrean';
      elements.npQueueList.appendChild(empty);
      return;
    }

    upcoming.forEach((song, idx) => {
      const realIndex = state.currentSongIndex + 1 + idx;
      const row = document.createElement('div');
      row.className = 'np-qi-row';

      const numEl = document.createElement('span');
      numEl.className = 'np-qi-idx';
      numEl.textContent = (idx + 1) < 10 ? `0${idx + 1}` : `${idx + 1}`;

      const discEl = document.createElement('div');
      discEl.className = 'np-qi-disc';
      discEl.innerHTML = '<span class="material-icons-round">music_note</span>';

      const infoEl = document.createElement('div');
      infoEl.className = 'np-qi-info';
      const tEl = document.createElement('span');
      tEl.className = 'np-qi-title';
      tEl.textContent = song.title || 'Lagu';
      const sEl = document.createElement('span');
      sEl.className = 'np-qi-sub';
      sEl.textContent = `${song.artist || 'Artis'} · ${formatTime(song.duration)}`;
      infoEl.appendChild(tEl);
      infoEl.appendChild(sEl);

      const actionsEl = document.createElement('div');
      actionsEl.className = 'np-qi-actions';

      const isFav = isSongFavorite(song);
      const favBtn = document.createElement('button');
      favBtn.className = `btn-mini-icon ${isFav ? 'active' : ''}`;
      favBtn.title = isFav ? 'Hapus dari favorit' : 'Tambah ke favorit';
      favBtn.innerHTML = `<span class="material-icons-round">${isFav ? 'favorite' : 'favorite_border'}</span>`;
      favBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleFavorite(song);
        renderQueue();
      });

      const delBtn = document.createElement('button');
      delBtn.className = 'btn-mini-icon';
      delBtn.title = 'Hapus dari antrean';
      delBtn.innerHTML = '<span class="material-icons-round">close</span>';
      delBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        state.currentList.splice(realIndex, 1);
        renderQueue();
      });

      actionsEl.appendChild(favBtn);
      actionsEl.appendChild(delBtn);

      row.appendChild(numEl);
      row.appendChild(discEl);
      row.appendChild(infoEl);
      row.appendChild(actionsEl);

      row.addEventListener('click', () => {
        playSong(song, state.currentList, realIndex);
      });

      elements.npQueueList.appendChild(row);
    });
  }

  // ─── Now Playing Screen Control Functions ──────────────────────────────────
  function setNowPlayingTab(tab) {
    state.npActiveTab = tab;
    if (tab === 'lyrics') {
      elements.npTabLyrics?.classList.add('active');
      elements.npTabQueue?.classList.remove('active');
      elements.npPanelLyrics?.classList.remove('hidden');
      elements.npPanelQueue?.classList.add('hidden');
      if (elements.npViewToggleIcon) elements.npViewToggleIcon.textContent = 'notes';
    } else {
      elements.npTabQueue?.classList.add('active');
      elements.npTabLyrics?.classList.remove('active');
      elements.npPanelQueue?.classList.remove('hidden');
      elements.npPanelLyrics?.classList.add('hidden');
      if (elements.npViewToggleIcon) elements.npViewToggleIcon.textContent = 'queue_music';
      renderQueue();
    }
  }

  function openNowPlayingScreen(tab = null) {
    if (tab) setNowPlayingTab(tab);
    else setNowPlayingTab(state.npActiveTab || 'lyrics');
    elements.viewNowPlaying?.classList.remove('hidden');
    state.isNowPlayingOpen = true;
    initWaveformTicks();
    updateNowPlayingUI();
    const cur = audio.currentTime || 0;
    const dur = getEffectiveDuration();
    const frac = dur > 0 ? Math.max(0, Math.min(1, cur / dur)) : 0;
    renderNpDialProgress(frac);
    updateWaveformProgress(frac);
    if (elements.npCurTime) elements.npCurTime.textContent = formatTime(cur);
    if (elements.npDurTime) elements.npDurTime.textContent = formatTime(dur);
    if (state.currentSong) {
      loadLyricsForSong(state.currentSong);
      renderQueue();
    }
  }

  function closeNowPlayingScreen() {
    elements.viewNowPlaying?.classList.add('hidden');
    state.isNowPlayingOpen = false;
  }

  function updateNowPlayingUI() {
    if (!elements.viewNowPlaying) return;

    const isPlaying = state.isPlaying;
    if (elements.npPlayIcon) elements.npPlayIcon.textContent = isPlaying ? 'pause' : 'play_arrow';
    elements.npDisc?.classList.toggle('spinning', isPlaying);

    if (!state.currentSong) {
      if (elements.npTitle) elements.npTitle.textContent = '—';
      if (elements.npSubtitle) elements.npSubtitle.textContent = 'Pilih lagu untuk memutar';
      if (elements.npSourceText) elements.npSourceText.textContent = 'TIDAK ADA LAGU';
      if (elements.npBadgeType) elements.npBadgeType.textContent = 'OFFLINE';
      if (elements.npFavIcon) elements.npFavIcon.textContent = 'favorite_border';
      elements.npBtnFav?.classList.remove('active');
      return;
    }

    const title = state.currentSong.title || 'Unknown Title';
    const artist = state.currentSong.artist || 'Unknown Artist';
    const src = state.currentSong.source || (state.currentSong.isOnline ? 'Online Stream' : 'Perpustakaan');

    if (elements.npTitle) elements.npTitle.textContent = title;
    if (elements.npSubtitle) elements.npSubtitle.textContent = `${artist} · ${src}`;
    if (elements.npSourceText) elements.npSourceText.textContent = `MEMUTAR DARI · ${src.toUpperCase()}`;
    if (elements.npBadgeType) elements.npBadgeType.textContent = state.currentSong.isOnline ? 'ONLINE' : 'LOKAL';

    const isFav = isSongFavorite(state.currentSong);
    if (elements.npFavIcon) elements.npFavIcon.textContent = isFav ? 'favorite' : 'favorite_border';
    elements.npBtnFav?.classList.toggle('active', isFav);

    elements.npBtnShuffle?.classList.toggle('active', state.isShuffle);
    elements.npBtnRepeat?.classList.toggle('active', state.repeatMode > 0);
    const repeatIcons = ['repeat', 'repeat', 'repeat_one'];
    if (elements.npRepeatIcon) elements.npRepeatIcon.textContent = repeatIcons[state.repeatMode];

    if (elements.npQcTitle) elements.npQcTitle.textContent = title;
    if (elements.npQcArtist) elements.npQcArtist.textContent = `${artist} · ${src}`;
    updateDiscCoverUI();
  }

  // ─── Audio Playback Controller ─────────────────────────────────────────────
  async function playSong(song, list = null, index = -1) {
    if (!song) return;

    // Hentikan pemutaran sebelumnya seketika agar lagu lokal/lama tidak bocor!
    audio.pause();
    state.isPlaying = false;

    if (list) {
      state.currentList = list;
      state.currentSongIndex = index >= 0 ? index : list.findIndex(s => 
        (s.id && s.id === song.id) || 
        (s.path && s.path === song.path) || 
        (s.streamUrl && s.streamUrl === song.streamUrl)
      );
    }

    state.currentSong = song;
    updatePlayerUI();
    updateMiniPlayerVisibility();
    renderCurrentScreenTracks();
    loadLyricsForSong(song);
    renderQueue();

    // Inisialisasi tampilan garis waktu & durasi lagu secara akurat
    const initDur = getEffectiveDuration();
    const durText = formatTime(initDur);
    if (elements.heroCurTime) elements.heroCurTime.textContent = '0:00';
    if (elements.heroDurTime) elements.heroDurTime.textContent = durText;
    if (elements.miniCurTime) elements.miniCurTime.textContent = '0:00';
    if (elements.miniDurTime) elements.miniDurTime.textContent = durText;
    if (elements.npCurTime) elements.npCurTime.textContent = '0:00';
    if (elements.npDurTime) elements.npDurTime.textContent = durText;
    renderDialProgress(0);
    renderNpDialProgress(0);
    updateWaveformProgress(0);
    if (elements.miniNeedle) elements.miniNeedle.style.left = '0%';

    let targetUrl = '';
    // Prioritaskan file lokal / unduhan fisik yang tersimpan di disk
    if (song.path && (!song.isOnline || song.isDownloaded)) {
      targetUrl = formatAudioSrc(song.path);
    } else if (song.source === 'Jamendo' || (song.streamUrl && song.streamUrl.includes('jamendo.com'))) {
      targetUrl = song.streamUrl;
    } else if (song.isOnline && window.elektronAPI?.resolveStreamUrl) {
      try {
        targetUrl = await window.elektronAPI.resolveStreamUrl(song);
        song.streamUrl = targetUrl;
      } catch (err) {
        targetUrl = song.streamUrl || (song.path ? formatAudioSrc(song.path) : '');
      }
    } else if (song.path) {
      targetUrl = formatAudioSrc(song.path);
    } else {
      targetUrl = song.streamUrl || '';
    }

    if (!targetUrl) {
      showDownloadToast('URL audio tidak ditemukan', 0);
      hideDownloadToast(3000);
      return;
    }

    audio.src = targetUrl;
    audio.play().then(() => {
      state.isPlaying = true;
      updatePlayerUI();
      renderCurrentScreenTracks();
    }).catch(err => {
      console.warn('Audio playback error:', err);
      state.isPlaying = false;
      updatePlayerUI();
      const msg = song.isOnline ? 'Gagal memutar lagu online' : 'Gagal memutar file audio';
      showDownloadToast(msg, 0);
      hideDownloadToast(3500);
    });

    if ('mediaSession' in navigator) {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: song.title || 'Unknown Title',
        artist: song.artist || 'Unknown Artist',
        album: song.album || 'MusikKu',
        artwork: song.cover ? [{ src: song.cover, sizes: '512x512', type: 'image/jpeg' }] : []
      });
    }

    if (!song.isOnline) {
      fetchMetadataAsync(song);
    }
  }

  function togglePlayPause() {
    if (!state.currentSong) {
      if (state.currentList.length > 0) {
        playSong(state.currentList[0], state.currentList, 0);
      } else if (state.allSongs.length > 0) {
        playSong(state.allSongs[0], state.allSongs, 0);
      }
      return;
    }

    if (state.isPlaying) {
      audio.pause();
      state.isPlaying = false;
    } else {
      audio.play().then(() => {
        state.isPlaying = true;
      }).catch(console.error);
    }
    updatePlayerUI();
    renderCurrentScreenTracks();
  }

  function playNext() {
    if (!state.currentList || state.currentList.length === 0) return;

    if (state.isShuffle) {
      const rnd = Math.floor(Math.random() * state.currentList.length);
      playSong(state.currentList[rnd], state.currentList, rnd);
      return;
    }

    let nextIdx = state.currentSongIndex + 1;
    if (nextIdx >= state.currentList.length) {
      if (state.repeatMode === 1) {
        nextIdx = 0;
      } else {
        audio.pause();
        state.isPlaying = false;
        updatePlayerUI();
        renderCurrentScreenTracks();
        return;
      }
    }
    playSong(state.currentList[nextIdx], state.currentList, nextIdx);
  }

  function playPrevious() {
    if (!state.currentList || state.currentList.length === 0) return;

    if (audio.currentTime > 3) {
      audio.currentTime = 0;
      return;
    }

    let prevIdx = state.currentSongIndex - 1;
    if (prevIdx < 0) {
      prevIdx = state.currentList.length - 1;
    }
    playSong(state.currentList[prevIdx], state.currentList, prevIdx);
  }

  // ─── Audio Events ──────────────────────────────────────────────────────────
  audio.addEventListener('timeupdate', () => {
    const cur = audio.currentTime;
    const dur = getEffectiveDuration();

    const curText = formatTime(cur);
    const durText = formatTime(dur);

    elements.heroCurTime.textContent = curText;
    elements.heroDurTime.textContent = durText;
    elements.miniCurTime.textContent = curText;
    elements.miniDurTime.textContent = durText;
    if (elements.npCurTime) elements.npCurTime.textContent = curText;
    if (elements.npDurTime) elements.npDurTime.textContent = durText;

    const progress = dur > 0 ? Math.max(0, Math.min(1, cur / dur)) : 0;
    renderDialProgress(progress);
    renderNpDialProgress(progress);
    updateWaveformProgress(progress);

    if (elements.miniNeedle) {
      elements.miniNeedle.style.left = `${progress * 100}%`;
    }

    syncLyrics(cur);
  });

  audio.addEventListener('loadedmetadata', () => {
    const dur = getEffectiveDuration();
    const durText = formatTime(dur);
    elements.heroDurTime.textContent = durText;
    elements.miniDurTime.textContent = durText;
    if (elements.npDurTime) elements.npDurTime.textContent = durText;
    if (state.currentSong && !state.currentSong.duration && audio.duration && !isNaN(audio.duration) && isFinite(audio.duration) && audio.duration > 0) {
      state.currentSong.duration = audio.duration;
    }
  });

  audio.addEventListener('ended', () => {
    if (state.repeatMode === 2) {
      audio.currentTime = 0;
      audio.play();
    } else {
      playNext();
    }
  });

  audio.addEventListener('error', () => {
    console.warn('Audio element error event:', audio.error);
    state.isPlaying = false;
    updatePlayerUI();
  });

  // ─── UI Updates for Controls & Now Playing ─────────────────────────────────
  function updatePlayerUI() {
    const isPlaying = state.isPlaying;

    elements.heroPlayIcon.textContent = isPlaying ? 'pause' : 'play_arrow';
    elements.miniPlayIcon.textContent = isPlaying ? 'pause' : 'play_arrow';

    elements.dialDisc?.classList.toggle('spinning', isPlaying);
    elements.miniDisc?.classList.toggle('spinning', isPlaying);

    if (!state.currentSong) {
      elements.heroTitle.textContent = '—';
      elements.heroSubtitle.textContent = 'Pilih lagu dari perpustakaan atau jelajah';
      elements.miniTitle.textContent = '—';
      elements.miniArtist.textContent = '—';
      elements.koleksiNowCard?.classList.add('hidden');
      updateDiscCoverUI();
      return;
    }

    const title = state.currentSong.title || 'Unknown Title';
    const artist = state.currentSong.artist || 'Unknown Artist';
    const album = state.currentSong.album || (state.currentSong.isOnline ? 'Streaming Online' : 'MusikKu');

    elements.heroTitle.textContent = title;
    elements.heroSubtitle.textContent = `${artist} · ${album}`;
    elements.miniTitle.textContent = title;
    elements.miniArtist.textContent = artist;

    // Koleksi now playing card on library
    if (elements.koleksiNowCard) {
      elements.koleksiNowCard.classList.remove('hidden');
      elements.kcTitle.textContent = title;
      elements.kcSub.textContent = `${artist} · ${formatTime(state.currentSong.duration)}`;
    }

    // Favorite heart icon state
    const isFav = isSongFavorite(state.currentSong);
    elements.heroFavIcon.textContent = isFav ? 'favorite' : 'favorite_border';
    elements.heroBtnFav?.classList.toggle('active', isFav);
    elements.miniFavIcon.textContent = isFav ? 'favorite' : 'favorite_border';
    elements.miniFav?.classList.toggle('active', isFav);

    // Shuffle & repeat states
    elements.heroBtnShuffle?.classList.toggle('active', state.isShuffle);
    elements.heroBtnRepeat?.classList.toggle('active', state.repeatMode > 0);
    elements.miniRepeat?.classList.toggle('active', state.repeatMode > 0);

    const repeatIcons = ['repeat', 'repeat', 'repeat_one'];
    elements.heroRepeatIcon.textContent = repeatIcons[state.repeatMode];
    elements.miniRepeatIcon.textContent = repeatIcons[state.repeatMode];

    updateDiscCoverUI();
    updateNowPlayingUI();
  }

  function toggleFavorite(songOrKey) {
    if (!songOrKey) return;
    let song = null;
    let key = '';

    if (typeof songOrKey === 'string') {
      key = songOrKey;
      song = findSongByKey(key) || state.favoriteSongs.get(key) || {
        id: key,
        title: 'Lagu Favorit',
        artist: 'MusikKu',
        album: 'Favorit',
        path: key.startsWith('http') || key.startsWith('sc_') || key.startsWith('yt_') || key.startsWith('jamendo_') ? '' : key,
        streamUrl: key.startsWith('http') ? key : '',
        duration: 0
      };
    } else {
      song = songOrKey;
      key = getSongKey(song);
    }

    const currentlyFav = isSongFavorite(song);

    if (currentlyFav) {
      state.favoriteSongs.delete(key);
      state.favorites.delete(key);
      if (song.id) {
        state.favoriteSongs.delete(song.id);
        state.favorites.delete(song.id);
      }
      if (song.path) {
        state.favoriteSongs.delete(song.path);
        state.favorites.delete(song.path);
      }
      if (song.streamUrl) {
        state.favoriteSongs.delete(song.streamUrl);
        state.favorites.delete(song.streamUrl);
      }
      showDownloadToast(`Dihapus dari Favorit: ${song.title || 'Lagu'}`, 100);
      hideDownloadToast(2000);
    } else {
      const savedSong = {
        id: song.id || key,
        title: song.title || 'Lagu Tanpa Judul',
        artist: song.artist || 'Artis',
        album: song.album || (song.isOnline ? 'Online Stream' : 'MusikKu'),
        duration: song.duration || 0,
        cover: song.cover || '',
        path: song.path || '',
        streamUrl: song.streamUrl || '',
        sourceUrl: song.sourceUrl || '',
        downloadUrl: song.downloadUrl || '',
        isOnline: !!song.isOnline,
        isFullTrack: !!song.isFullTrack,
        source: song.source || (song.isOnline ? 'Online' : 'Lokal')
      };
      state.favoriteSongs.set(key, savedSong);
      state.favorites.add(key);
      if (song.id) {
        state.favoriteSongs.set(song.id, savedSong);
        state.favorites.add(song.id);
      }
      showDownloadToast(`Ditambahkan ke Favorit: ${song.title || 'Lagu'}`, 100);
      hideDownloadToast(2000);
    }

    saveFavorites();
    updatePlayerUI();
    renderCurrentScreenTracks();
    if (state.npActiveTab === 'queue') renderQueue();
  }

  function renderCurrentScreenTracks() {
    if (state.activeTab === 'library') applyFilterAndRenderLibrary();
    else if (state.activeTab === 'online') renderOnlineTracks();
    else if (state.activeTab === 'favorites') renderFavorites();
    else if (state.activeTab === 'downloads') renderDownloads();
    else if (state.activeTab === 'playlists' && state.activePlaylistId) renderPlaylistDetail(state.activePlaylistId);
  }

  // ─── Volume Controls ───────────────────────────────────────────────────────
  function updateVolumeSliderFill(slider, pct) {
    if (!slider) return;
    slider.style.background = `linear-gradient(to right, #168CDE 0%, #168CDE ${pct}%, rgba(23, 53, 80, 0.14) ${pct}%, rgba(23, 53, 80, 0.14) 100%)`;
  }

  function setVolume(val) {
    const v = Math.max(0, Math.min(1, val));
    state.volume = v;
    audio.volume = v;
    localStorage.setItem('musikku_vol', v.toString());
    const pct = Math.round(v * 100);
    if (elements.heroVolume) {
      elements.heroVolume.value = pct;
      updateVolumeSliderFill(elements.heroVolume, pct);
    }
    if (elements.miniVolSlider) {
      elements.miniVolSlider.value = pct;
      updateVolumeSliderFill(elements.miniVolSlider, pct);
    }
    if (elements.npVolSlider) {
      elements.npVolSlider.value = pct;
      updateVolumeSliderFill(elements.npVolSlider, pct);
    }
    if (elements.heroVolVal) elements.heroVolVal.textContent = `${pct}%`;
  }

  elements.heroVolume?.addEventListener('input', (e) => setVolume(e.target.value / 100));
  elements.miniVolSlider?.addEventListener('input', (e) => setVolume(e.target.value / 100));
  elements.npVolSlider?.addEventListener('input', (e) => setVolume(e.target.value / 100));
  setVolume(state.volume);

  // ─── Playback Controls Listeners ───────────────────────────────────────────
  elements.heroBtnPlay?.addEventListener('click', togglePlayPause);
  elements.miniPlay?.addEventListener('click', togglePlayPause);
  elements.npBtnPlay?.addEventListener('click', togglePlayPause);

  elements.heroBtnNext?.addEventListener('click', playNext);
  elements.miniNext?.addEventListener('click', playNext);
  elements.npBtnNext?.addEventListener('click', playNext);

  elements.heroBtnPrev?.addEventListener('click', playPrevious);
  elements.miniPrev?.addEventListener('click', playPrevious);
  elements.npBtnPrev?.addEventListener('click', playPrevious);

  elements.heroBtnShuffle?.addEventListener('click', () => {
    state.isShuffle = !state.isShuffle;
    updatePlayerUI();
  });
  elements.npBtnShuffle?.addEventListener('click', () => {
    state.isShuffle = !state.isShuffle;
    updatePlayerUI();
  });

  elements.heroBtnRepeat?.addEventListener('click', () => {
    state.repeatMode = (state.repeatMode + 1) % 3;
    updatePlayerUI();
  });
  elements.miniRepeat?.addEventListener('click', () => {
    state.repeatMode = (state.repeatMode + 1) % 3;
    updatePlayerUI();
  });
  elements.npBtnRepeat?.addEventListener('click', () => {
    state.repeatMode = (state.repeatMode + 1) % 3;
    updatePlayerUI();
  });

  elements.heroBtnFav?.addEventListener('click', () => {
    if (state.currentSong) toggleFavorite(state.currentSong);
  });
  elements.miniFav?.addEventListener('click', () => {
    if (state.currentSong) toggleFavorite(state.currentSong);
  });
  elements.npBtnFav?.addEventListener('click', () => {
    if (state.currentSong) toggleFavorite(state.currentSong);
  });

  elements.npBtnDownload?.addEventListener('click', () => {
    if (state.currentSong) downloadSingleTrack(state.currentSong, elements.npBtnDownload);
  });
  elements.npBtnAddPl?.addEventListener('click', () => {
    if (state.currentSong) {
      openSongToPlaylistModal(state.currentSong);
    } else {
      showDownloadToast('Tidak ada lagu yang sedang diputar', 0);
      hideDownloadToast(2000);
    }
  });
  elements.npBtnChangeCover?.addEventListener('click', (e) => {
    e.stopPropagation();
    pickAndChangeCover();
  });

  // Disc Click & Drag/Drop for changing album cover
  elements.dialDisc?.addEventListener('mousedown', (e) => {
    e.stopPropagation();
  });
  elements.dialDisc?.addEventListener('click', (e) => {
    e.stopPropagation();
    pickAndChangeCover();
  });

  elements.npDisc?.addEventListener('mousedown', (e) => {
    e.stopPropagation();
  });
  elements.npDisc?.addEventListener('click', (e) => {
    e.stopPropagation();
    pickAndChangeCover();
  });

  // Error listeners for disc cover images fallback
  elements.dialCoverImg?.addEventListener('error', () => {
    elements.dialCoverImg.classList.add('hidden');
    elements.dialCenterIcon?.classList.remove('hidden');
    elements.dialDisc?.classList.remove('has-cover');
  });
  elements.npCoverImg?.addEventListener('error', () => {
    elements.npCoverImg.classList.add('hidden');
    elements.npDiscIcon?.classList.remove('hidden');
    elements.npDisc?.classList.remove('has-cover');
  });
  elements.miniCoverImg?.addEventListener('error', () => {
    elements.miniCoverImg.classList.add('hidden');
    elements.miniCenterIcon?.classList.remove('hidden');
    elements.miniDisc?.classList.remove('has-cover');
  });

  setupDiscDragAndDrop(elements.dialDisc, () => state.currentSong);
  setupDiscDragAndDrop(elements.npDisc, () => state.currentSong);

  // ─── Mini Player & Fullscreen Now Playing Overlay Listeners ────────────────
  elements.miniDisc?.addEventListener('click', () => openNowPlayingScreen());
  elements.miniInfoWrap?.addEventListener('click', () => openNowPlayingScreen());
  elements.miniExpand?.addEventListener('click', () => openNowPlayingScreen());
  elements.npBtnClose?.addEventListener('click', closeNowPlayingScreen);

  elements.npTabQueue?.addEventListener('click', () => setNowPlayingTab('queue'));
  elements.npTabLyrics?.addEventListener('click', () => setNowPlayingTab('lyrics'));
  elements.npBtnToggleView?.addEventListener('click', () => {
    setNowPlayingTab(state.npActiveTab === 'lyrics' ? 'queue' : 'lyrics');
  });

  elements.lrcBtnDec?.addEventListener('click', () => {
    state.lyricsFontSize = Math.max(13, (state.lyricsFontSize || 16) - 2);
    if (elements.npLyricsList) elements.npLyricsList.style.fontSize = `${state.lyricsFontSize}px`;
  });
  elements.lrcBtnInc?.addEventListener('click', () => {
    state.lyricsFontSize = Math.min(28, (state.lyricsFontSize || 16) + 2);
    if (elements.npLyricsList) elements.npLyricsList.style.fontSize = `${state.lyricsFontSize}px`;
  });
  elements.lrcBtnFollow?.addEventListener('click', () => {
    state.autoScrollLyrics = !state.autoScrollLyrics;
    elements.lrcBtnFollow?.classList.toggle('active', state.autoScrollLyrics);
  });

  elements.npBtnSaveQueuePl?.addEventListener('click', () => {
    if (state.currentList.length === 0) {
      showDownloadToast('Antrean masih kosong', 0);
      hideDownloadToast(2000);
      return;
    }
    const defaultName = `Antrean ${new Date().toLocaleDateString('id-ID')}`;
    const pl = {
      id: 'pl_' + Date.now(),
      name: defaultName,
      coverGradient: GRADIENT_PRESETS[Math.floor(Math.random() * GRADIENT_PRESETS.length)],
      trackIds: state.currentList.map(s => getSongKey(s)),
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    state.playlists.unshift(pl);
    savePlaylists();
    showDownloadToast(`Antrean berhasil disimpan ke playlist "${defaultName}"`, 100);
    hideDownloadToast(2500);
  });

  elements.npBtnClearQueue?.addEventListener('click', async () => {
    if (state.currentList.length <= 1) {
      state.currentList = state.currentSong ? [state.currentSong] : [];
      state.currentSongIndex = 0;
      renderQueue();
      return;
    }
    const confirmed = await showConfirmModal({
      title: 'Kosongkan Antrean?',
      subtitle: 'Antrean berikutnya akan dibersihkan',
      message: 'Yakin ingin mengosongkan seluruh antrean lagu berikutnya?',
      confirmText: 'Kosongkan Antrean',
      cancelText: 'Batal',
      icon: 'clear_all',
      isDanger: true
    });
    if (!confirmed) return;
    state.currentList = state.currentSong ? [state.currentSong] : [];
    state.currentSongIndex = 0;
    renderQueue();
    showDownloadToast('Antrean berhasil dikosongkan', 100);
    hideDownloadToast(2000);
  });

  // ─── Keyboard Shortcuts ────────────────────────────────────────────────────
  window.addEventListener('keydown', (e) => {
    if (e.code === 'Escape') {
      if (elements.onlineRecentPopover && !elements.onlineRecentPopover.classList.contains('hidden')) {
        elements.onlineRecentPopover.classList.add('hidden');
        return;
      }
      if (elements.libRecentPopover && !elements.libRecentPopover.classList.contains('hidden')) {
        elements.libRecentPopover.classList.add('hidden');
        return;
      }
      if (elements.songToPlModal && !elements.songToPlModal.classList.contains('hidden')) {
        closeSongToPlaylistModal();
        return;
      }
      if (elements.confirmModal && !elements.confirmModal.classList.contains('hidden')) {
        closeConfirmModal(false);
        return;
      }
      if (elements.playlistModal && !elements.playlistModal.classList.contains('hidden')) {
        closePlaylistModal();
        return;
      }
      if (elements.addToPlModal && !elements.addToPlModal.classList.contains('hidden')) {
        elements.addToPlModal.classList.add('hidden');
        return;
      }
    }
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
    if (e.code === 'Escape' && state.isNowPlayingOpen) {
      closeNowPlayingScreen();
      return;
    }
    if (e.code === 'Space') {
      e.preventDefault();
      togglePlayPause();
    }
  });

  // ─── Generic Track Row Builder (.track) ────────────────────────────────────
  function createTrackRow(song, index, list, options = {}) {
    const songKey = getSongKey(song);
    const isCurrent = state.currentSong && (
      (song.id && state.currentSong.id === song.id) ||
      (song.path && state.currentSong.path === song.path) ||
      (song.streamUrl && state.currentSong.streamUrl === song.streamUrl)
    );
    const isFav = isSongFavorite(song);
    const isDownloaded = isSongDownloaded(song);

    const row = document.createElement('div');
    row.className = `track ${isCurrent ? 'playing' : ''}`;
    row.dataset.key = songKey;

    // 1. Number
    const numEl = document.createElement('div');
    numEl.className = 'num';
    numEl.textContent = (index + 1).toString().padStart(2, '0');

    // 2. Disc dot / Thumbnail
    const discEl = document.createElement('div');
    discEl.className = 'disc-thumb';
    const songCover = getSongCover(song);
    if (songCover) {
      const img = document.createElement('img');
      img.src = songCover;
      img.alt = '';
      img.onerror = () => {
        discEl.innerHTML = `<div class="disc-fallback" style="background:${getGradientStyle(song.title)}"><span class="material-icons-round" style="font-size:22px;">music_note</span></div>`;
      };
      discEl.appendChild(img);
    } else {
      discEl.innerHTML = `<div class="disc-fallback" style="background:${getGradientStyle(song.title || song.path)}"><span class="material-icons-round" style="font-size:22px;">music_note</span></div>`;
    }

    // 3. Title & Subtitle + Badge
    const titleGroup = document.createElement('div');
    titleGroup.className = 'title-group';

    const titleRow = document.createElement('div');
    titleRow.className = 'title-row';

    const titleSpan = document.createElement('span');
    titleSpan.className = 'title';
    titleSpan.textContent = song.title || 'Unknown Title';
    titleRow.appendChild(titleSpan);

    if (song.isOnline) {
      const badge = document.createElement('span');
      badge.className = 'badge badge-online';
      badge.textContent = 'ONLINE';
      titleRow.appendChild(badge);
    } else if (isDownloaded) {
      const badge = document.createElement('span');
      badge.className = 'badge badge-offline';
      badge.textContent = 'OFFLINE';
      titleRow.appendChild(badge);
    }

    const subSpan = document.createElement('span');
    subSpan.className = 'sub';
    subSpan.textContent = `${song.artist || 'Unknown Artist'} · ${song.album || (song.isOnline ? 'Lagu Lengkap' : 'MusikKu')}`;

    titleGroup.appendChild(titleRow);
    titleGroup.appendChild(subSpan);

    // 4. Action 1 (Download or Remove from Playlist)
    const act1 = document.createElement('div');
    if (options.onRemove) {
      const removeBtn = document.createElement('button');
      removeBtn.className = 'action-btn';
      removeBtn.title = 'Hapus dari playlist';
      removeBtn.innerHTML = '<span class="material-icons-round">close</span>';
      removeBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        options.onRemove(song);
      });
      act1.appendChild(removeBtn);
    } else if (song.isOnline) {
      const dlBtn = document.createElement('button');
      dlBtn.className = `action-btn ${isDownloaded ? 'downloaded' : ''}`;
      dlBtn.title = isDownloaded ? 'Tersimpan Offline' : 'Unduh Lagu';
      dlBtn.innerHTML = `<span class="material-icons-round">${isDownloaded ? 'cloud_done' : 'download'}</span>`;
      if (!isDownloaded) {
        dlBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          downloadSingleTrack(song, dlBtn);
        });
      }
      act1.appendChild(dlBtn);
    }

    // 5. Action 2 (Favorite)
    const act2 = document.createElement('div');
    const favBtn = document.createElement('button');
    favBtn.className = `action-btn ${isFav ? 'active-fav' : ''}`;
    favBtn.title = isFav ? 'Hapus dari favorit' : 'Tambah ke favorit';
    favBtn.innerHTML = `<span class="material-icons-round">${isFav ? 'favorite' : 'favorite_border'}</span>`;
    favBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      toggleFavorite(song);
    });
    act2.appendChild(favBtn);

    // 6. Duration
    const durEl = document.createElement('div');
    durEl.className = 'dur';
    durEl.textContent = formatTime(song.duration);

    row.appendChild(numEl);
    row.appendChild(discEl);
    row.appendChild(titleGroup);
    row.appendChild(act1);
    row.appendChild(act2);
    row.appendChild(durEl);

    row.addEventListener('click', () => {
      playSong(song, list, index);
    });

    // Right-click context menu
    row.addEventListener('contextmenu', (e) => {
      e.preventDefault();
      showContextMenu(e.clientX, e.clientY, song);
    });

    return row;
  }

  // ─── Riwayat Pencarian (Recent Searches) ───────────────────────────────────
  function getRecentSearches(type = 'online') {
    const key = type === 'online' ? 'musikku_recent_searches_online' : 'musikku_recent_searches_lib';
    try {
      const stored = localStorage.getItem(key);
      const parsed = stored ? JSON.parse(stored) : [];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  function saveRecentSearch(query, type = 'online') {
    const q = (query || '').trim();
    if (!q) return;
    const key = type === 'online' ? 'musikku_recent_searches_online' : 'musikku_recent_searches_lib';
    let list = getRecentSearches(type).filter(item => item.toLowerCase() !== q.toLowerCase());
    list.unshift(q);
    if (list.length > 8) list = list.slice(0, 8);
    localStorage.setItem(key, JSON.stringify(list));
  }

  function removeRecentSearch(query, type = 'online') {
    const key = type === 'online' ? 'musikku_recent_searches_online' : 'musikku_recent_searches_lib';
    let list = getRecentSearches(type).filter(item => item.toLowerCase() !== (query || '').toLowerCase());
    localStorage.setItem(key, JSON.stringify(list));
  }

  function clearRecentSearches(type = 'online') {
    const key = type === 'online' ? 'musikku_recent_searches_online' : 'musikku_recent_searches_lib';
    localStorage.removeItem(key);
  }

  function renderRecentSearchPopover(type = 'online') {
    const isOnline = type === 'online';
    const listEl = isOnline ? elements.recentListOnline : elements.recentListLib;
    const popoverEl = isOnline ? elements.onlineRecentPopover : elements.libRecentPopover;
    const inputEl = isOnline ? elements.onlineSearchInput : elements.librarySearch;

    if (!listEl || !popoverEl || !inputEl) return;
    listEl.innerHTML = '';

    const list = getRecentSearches(type);

    if (list.length === 0) {
      listEl.innerHTML = `
        <div class="recent-empty">
          <span class="material-icons-round" style="font-size:18px;">history</span>
          <span>Belum ada riwayat pencarian</span>
        </div>
      `;
      return;
    }

    list.forEach(queryText => {
      const item = document.createElement('div');
      item.className = 'recent-item';
      item.innerHTML = `
        <span class="material-icons-round recent-item-icon">history</span>
        <span class="recent-item-text">${queryText}</span>
        <button type="button" class="recent-item-del" title="Hapus dari riwayat">
          <span class="material-icons-round">close</span>
        </button>
      `;

      item.addEventListener('click', () => {
        inputEl.value = queryText;
        saveRecentSearch(queryText, type);
        popoverEl.classList.add('hidden');
        if (isOnline) {
          clearTimeout(onlineSearchTimer);
          if (elements.activeTunerLabel) elements.activeTunerLabel.textContent = `"${queryText}"`;
          elements.tunerStops?.querySelectorAll('.stop').forEach(s => s.removeAttribute('aria-pressed'));
          fetchOnlineTracks(queryText);
        } else {
          applyFilterAndRenderLibrary();
        }
      });

      const btnDel = item.querySelector('.recent-item-del');
      btnDel?.addEventListener('click', (e) => {
        e.stopPropagation();
        removeRecentSearch(queryText, type);
        renderRecentSearchPopover(type);
      });

      listEl.appendChild(item);
    });
  }

  // ─── 1. Layar Perpustakaan ─────────────────────────────────────────────────
  function applyFilterAndRenderLibrary() {
    const query = (elements.librarySearch?.value || '').toLowerCase().trim();
    let songs = [...state.allSongs];

    if (query) {
      songs = songs.filter(s =>
        (s.title || '').toLowerCase().includes(query) ||
        (s.artist || '').toLowerCase().includes(query) ||
        (s.album || '').toLowerCase().includes(query)
      );
    }

    elements.libCount.textContent = `${songs.length} lagu`;

    if (songs.length === 0) {
      elements.libraryTrackList.innerHTML = '';
      elements.libraryEmpty.classList.remove('hidden');
    } else {
      elements.libraryEmpty.classList.add('hidden');
      const fragment = document.createDocumentFragment();
      songs.forEach((song, idx) => {
        fragment.appendChild(createTrackRow(song, idx, songs));
      });
      elements.libraryTrackList.innerHTML = '';
      elements.libraryTrackList.appendChild(fragment);
    }
  }

  elements.librarySearch?.addEventListener('focus', () => {
    renderRecentSearchPopover('lib');
    elements.libRecentPopover?.classList.remove('hidden');
  });

  elements.librarySearch?.addEventListener('click', (e) => {
    e.stopPropagation();
    renderRecentSearchPopover('lib');
    elements.libRecentPopover?.classList.remove('hidden');
  });

  elements.librarySearch?.addEventListener('input', () => {
    const q = elements.librarySearch.value.trim();
    if (q) {
      elements.libRecentPopover?.classList.add('hidden');
    }
    applyFilterAndRenderLibrary();
  });

  elements.librarySearch?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      const q = elements.librarySearch.value.trim();
      if (q) {
        saveRecentSearch(q, 'lib');
        elements.libRecentPopover?.classList.add('hidden');
      }
    } else if (e.key === 'Escape') {
      elements.libRecentPopover?.classList.add('hidden');
    }
  });

  elements.btnClearRecentLib?.addEventListener('click', (e) => {
    e.stopPropagation();
    clearRecentSearches('lib');
    renderRecentSearchPopover('lib');
  });

  elements.libPlayAll?.addEventListener('click', () => {
    if (state.allSongs.length > 0) {
      playSong(state.allSongs[0], state.allSongs, 0);
    }
  });

  elements.libShuffle?.addEventListener('click', () => {
    if (state.allSongs.length > 0) {
      state.isShuffle = true;
      const rnd = Math.floor(Math.random() * state.allSongs.length);
      playSong(state.allSongs[rnd], state.allSongs, rnd);
    }
  });

  elements.cardBtnAddFolder?.addEventListener('click', addFolder);
  elements.cardBtnUrlDl?.addEventListener('click', () => {
    elements.btnOpenDlModal?.click();
  });

  // ─── 2. Layar Jelajah Online (Sama seperti Aplikasi Android MusikKu) ─────────
  const ONLINE_GENRES = {
    youtube: [
      { id: 'trending', label: '🔥 Trending', query: 'Trending Musik Indonesia' },
      { id: 'pop_indo', label: 'Pop Indonesia', query: 'Pop Indonesia' },
      { id: 'pop_barat', label: 'Pop Barat', query: 'Pop Barat' },
      { id: 'rock', label: 'Rock', query: 'Rock' },
      { id: 'lofi', label: 'Lo-Fi Beats', query: 'Lo-Fi Beats' },
      { id: 'kpop', label: 'K-Pop', query: 'K-Pop' },
      { id: 'akustik', label: 'Akustik', query: 'Akustik' },
      { id: 'dangdut', label: 'Dangdut Koplo', query: 'Dangdut Koplo' },
      { id: 'rnb', label: 'R&B', query: 'R&B' }
    ],
    jamendo: [
      { id: 'popular', label: 'Populer', query: 'popular' },
      { id: 'pop', label: 'Pop', query: 'pop' },
      { id: 'lofi', label: 'Lo-fi', query: 'lofi' },
      { id: 'electronic', label: 'Elektronik', query: 'electronic' },
      { id: 'acoustic', label: 'Akustik', query: 'acoustic' },
      { id: 'rock', label: 'Rock', query: 'rock' },
      { id: 'jazz', label: 'Jazz', query: 'jazz' },
      { id: 'chill', label: 'Chill', query: 'chill' }
    ]
  };

  function renderTunerStops() {
    if (!elements.tunerStops) return;
    const genres = ONLINE_GENRES[state.onlineSource] || ONLINE_GENRES.youtube;
    elements.tunerStops.innerHTML = '';

    genres.forEach((g, idx) => {
      const isPressed = state.activeGenre === g.id || (idx === 0 && !state.activeGenre);
      if (isPressed) {
        state.activeGenre = g.id;
        if (elements.activeTunerLabel) elements.activeTunerLabel.textContent = g.label;
      }
      const btn = document.createElement('button');
      btn.className = 'stop';
      btn.dataset.genre = g.id;
      btn.dataset.query = g.query;
      if (isPressed) btn.setAttribute('aria-pressed', 'true');
      btn.innerHTML = `<i class="needle"></i>${g.label}`;
      btn.addEventListener('click', () => {
        elements.tunerStops.querySelectorAll('.stop').forEach(s => s.removeAttribute('aria-pressed'));
        btn.setAttribute('aria-pressed', 'true');
        state.activeGenre = g.id;
        if (elements.activeTunerLabel) elements.activeTunerLabel.textContent = g.label;
        if (elements.onlineSearchInput) elements.onlineSearchInput.value = '';
        fetchOnlineTracks(g.query);
      });
      elements.tunerStops.appendChild(btn);
    });
  }

  function setOnlineSource(source) {
    state.onlineSource = source;
    const isYt = source === 'youtube';

    elements.srcYt?.classList.toggle('active', isYt);
    elements.srcYt?.setAttribute('aria-selected', isYt ? 'true' : 'false');
    elements.srcJamendo?.classList.toggle('active', !isYt);
    elements.srcJamendo?.setAttribute('aria-selected', !isYt ? 'true' : 'false');

    if (elements.onlineEyebrow) {
      elements.onlineEyebrow.innerHTML = `<span class="dot"></span>${isYt ? 'STREAMING JUTAAN MUSIK ONLINE' : 'MUSIK LEGAL BEBAS ROYALTI'}`;
    }

    if (elements.onlineSearchInput) {
      elements.onlineSearchInput.placeholder = isYt
        ? 'Cari musik online (SZA, Daniel Caesar, pop, dll)...'
        : 'Cari musik bebas royalti di Jamendo...';
      elements.onlineSearchInput.value = '';
    }

    state.activeGenre = isYt ? 'trending' : 'popular';
    renderTunerStops();
    const initialQuery = isYt ? 'Trending Musik Indonesia' : 'popular';
    fetchOnlineTracks(initialQuery);
  }

  elements.srcYt?.addEventListener('click', () => setOnlineSource('youtube'));
  elements.srcJamendo?.addEventListener('click', () => setOnlineSource('jamendo'));

  async function fetchOnlineTracks(genreOrQuery, isLoadMore = false) {
    if (!window.elektronAPI?.onlineSearch) return;

    if (isLoadMore) {
      if (state.isLoadingOnline || state.isLoadingMoreOnline || !state.hasMoreOnline) return;
      state.isLoadingMoreOnline = true;
      if (elements.btnOnlineLoadMore) elements.btnOnlineLoadMore.disabled = true;
      if (elements.btnLoadMoreText) elements.btnLoadMoreText.textContent = 'Memuat lagu berikutnya...';
      elements.btnLoadMoreSpinner?.classList.remove('hidden');
      elements.btnOnlineLoadMore?.querySelector('.load-more-icon')?.classList.add('hidden');
    } else {
      state.onlineCurrentQuery = genreOrQuery || '';
      state.onlineCurrentPage = 1;
      state.hasMoreOnline = true;
      state.isLoadingOnline = true;
      elements.onlineEmpty.classList.add('hidden');
      elements.onlineLoading.classList.remove('hidden');
      elements.onlineLoadingText.textContent = `Mencari "${state.onlineCurrentQuery}"...`;
      elements.onlineTrackList.innerHTML = '';
      elements.onlineLoadMoreWrap?.classList.add('hidden');
    }

    const targetPage = isLoadMore ? (state.onlineCurrentPage + 1) : 1;

    try {
      const results = await window.elektronAPI.onlineSearch({
        query: state.onlineCurrentQuery,
        source: state.onlineSource,
        page: targetPage
      });

      // Hanya masukkan lagu utuh > 45 detik
      const validResults = (results || []).filter(t => (t.duration || 0) > 45);

      if (isLoadMore) {
        state.isLoadingMoreOnline = false;
        if (elements.btnOnlineLoadMore) elements.btnOnlineLoadMore.disabled = false;
        if (elements.btnLoadMoreText) elements.btnLoadMoreText.textContent = 'Muat lebih banyak lagu';
        elements.btnLoadMoreSpinner?.classList.add('hidden');
        elements.btnOnlineLoadMore?.querySelector('.load-more-icon')?.classList.remove('hidden');

        // Filter duplikat dari daftar lagu yang sudah ada
        const existingKeys = new Set(state.onlineSongs.map(s => (s.id || '') + '::' + (s.title || '').trim().toLowerCase()));
        const newTracks = [];
        for (const t of validResults) {
          const key = (t.id || '') + '::' + (t.title || '').trim().toLowerCase();
          if (!existingKeys.has(key)) {
            existingKeys.add(key);
            newTracks.push({
              ...t,
              path: t.streamUrl || t.sourceUrl || t.id,
              isOnline: true
            });
          }
        }

        if (newTracks.length === 0 || validResults.length < 8) {
          state.hasMoreOnline = false;
        }

        if (newTracks.length > 0) {
          state.onlineCurrentPage = targetPage;
          state.onlineSongs.push(...newTracks);
        }

        renderOnlineTracks();
      } else {
        state.isLoadingOnline = false;
        elements.onlineLoading.classList.add('hidden');
        state.onlineSongs = validResults.map(t => ({
          ...t,
          path: t.streamUrl || t.sourceUrl || t.id,
          isOnline: true
        }));

        if (validResults.length < 10) {
          state.hasMoreOnline = false;
        }

        renderOnlineTracks();
      }
    } catch (err) {
      console.error('Online search failed:', err);
      if (isLoadMore) {
        state.isLoadingMoreOnline = false;
        if (elements.btnOnlineLoadMore) elements.btnOnlineLoadMore.disabled = false;
        if (elements.btnLoadMoreText) elements.btnLoadMoreText.textContent = 'Coba lagi';
        elements.btnLoadMoreSpinner?.classList.add('hidden');
        elements.btnOnlineLoadMore?.querySelector('.load-more-icon')?.classList.remove('hidden');
      } else {
        state.isLoadingOnline = false;
        elements.onlineLoading.classList.add('hidden');
        state.onlineSongs = [];
        renderOnlineTracks();
      }
    }
  }

  function renderOnlineTracks() {
    const songs = state.onlineSongs;
    elements.onlineCount.textContent = `${songs.length} lagu`;

    if (songs.length === 0 && !state.isLoadingOnline) {
      elements.onlineEmpty.classList.remove('hidden');
      elements.onlineTrackList.innerHTML = '';
      elements.onlineLoadMoreWrap?.classList.add('hidden');
    } else {
      elements.onlineEmpty.classList.add('hidden');
      const fragment = document.createDocumentFragment();
      songs.forEach((song, idx) => {
        fragment.appendChild(createTrackRow(song, idx, songs));
      });
      elements.onlineTrackList.innerHTML = '';
      elements.onlineTrackList.appendChild(fragment);

      // Tampilkan atau sembunyikan tombol "Muat lebih banyak"
      if (elements.onlineLoadMoreWrap) {
        if (songs.length > 0 && state.hasMoreOnline && !state.isLoadingOnline) {
          elements.onlineLoadMoreWrap.classList.remove('hidden');
          if (elements.btnOnlineLoadMore) elements.btnOnlineLoadMore.disabled = false;
          if (elements.btnLoadMoreText) elements.btnLoadMoreText.textContent = 'Muat lebih banyak lagu';
          elements.btnLoadMoreSpinner?.classList.add('hidden');
          elements.btnOnlineLoadMore?.querySelector('.load-more-icon')?.classList.remove('hidden');
        } else {
          elements.onlineLoadMoreWrap.classList.add('hidden');
        }
      }
    }
  }

  elements.btnOnlineLoadMore?.addEventListener('click', () => {
    fetchOnlineTracks(state.onlineCurrentQuery, true);
  });

  elements.onlineSearchInput?.addEventListener('focus', () => {
    renderRecentSearchPopover('online');
    elements.onlineRecentPopover?.classList.remove('hidden');
  });

  elements.onlineSearchInput?.addEventListener('click', (e) => {
    e.stopPropagation();
    renderRecentSearchPopover('online');
    elements.onlineRecentPopover?.classList.remove('hidden');
  });

  let onlineSearchTimer = null;
  elements.onlineSearchInput?.addEventListener('input', (e) => {
    const q = e.target.value.trim();
    if (q) {
      elements.onlineRecentPopover?.classList.add('hidden');
    }
    clearTimeout(onlineSearchTimer);
    onlineSearchTimer = setTimeout(() => {
      if (q) {
        saveRecentSearch(q, 'online');
        if (elements.activeTunerLabel) elements.activeTunerLabel.textContent = `"${q}"`;
        elements.tunerStops?.querySelectorAll('.stop').forEach(s => s.removeAttribute('aria-pressed'));
        fetchOnlineTracks(q);
      } else {
        renderRecentSearchPopover('online');
        elements.onlineRecentPopover?.classList.remove('hidden');
        const defaultGenres = ONLINE_GENRES[state.onlineSource] || ONLINE_GENRES.youtube;
        const activeItem = defaultGenres.find(g => g.id === state.activeGenre) || defaultGenres[0];
        if (elements.activeTunerLabel) elements.activeTunerLabel.textContent = activeItem.label;
        fetchOnlineTracks(activeItem.query);
      }
    }, 450);
  });

  elements.onlineSearchInput?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      const q = elements.onlineSearchInput.value.trim();
      if (q) {
        clearTimeout(onlineSearchTimer);
        saveRecentSearch(q, 'online');
        elements.onlineRecentPopover?.classList.add('hidden');
        if (elements.activeTunerLabel) elements.activeTunerLabel.textContent = `"${q}"`;
        elements.tunerStops?.querySelectorAll('.stop').forEach(s => s.removeAttribute('aria-pressed'));
        fetchOnlineTracks(q);
      }
    } else if (e.key === 'Escape') {
      elements.onlineRecentPopover?.classList.add('hidden');
    }
  });

  elements.btnClearRecentOnline?.addEventListener('click', (e) => {
    e.stopPropagation();
    clearRecentSearches('online');
    renderRecentSearchPopover('online');
  });

  // Suggestion tags di bawah riwayat pencarian online
  elements.onlineSearchWrap?.querySelectorAll('.recent-tag').forEach(tag => {
    tag.addEventListener('click', (e) => {
      e.stopPropagation();
      const q = tag.dataset.query;
      if (q) {
        elements.onlineSearchInput.value = q;
        saveRecentSearch(q, 'online');
        elements.onlineRecentPopover?.classList.add('hidden');
        clearTimeout(onlineSearchTimer);
        if (elements.activeTunerLabel) elements.activeTunerLabel.textContent = `"${q}"`;
        elements.tunerStops?.querySelectorAll('.stop').forEach(s => s.removeAttribute('aria-pressed'));
        fetchOnlineTracks(q);
      }
    });
  });

  // Klik di luar bar pencarian menutup popover riwayat
  window.addEventListener('click', (e) => {
    if (!elements.onlineSearchWrap?.contains(e.target)) {
      elements.onlineRecentPopover?.classList.add('hidden');
    }
    if (!elements.libSearchWrap?.contains(e.target)) {
      elements.libRecentPopover?.classList.add('hidden');
    }
  });

  elements.onlinePlayAll?.addEventListener('click', () => {
    if (state.onlineSongs.length > 0) {
      playSong(state.onlineSongs[0], state.onlineSongs, 0);
    }
  });

  elements.onlineShuffle?.addEventListener('click', () => {
    if (state.onlineSongs.length > 0) {
      state.isShuffle = true;
      const rnd = Math.floor(Math.random() * state.onlineSongs.length);
      playSong(state.onlineSongs[rnd], state.onlineSongs, rnd);
    }
  });

  // ─── 3. Layar Favorit ──────────────────────────────────────────────────────
  function renderFavorites() {
    // 1. Recover any favorite keys that might not yet be in favoriteSongs Map
    if (state.favorites.size > 0) {
      let changed = false;
      const allKnown = getAllKnownSongs();
      for (const key of state.favorites) {
        if (!state.favoriteSongs.has(key)) {
          const found = allKnown.find(s => getSongKey(s) === key || s.id === key || s.path === key || s.streamUrl === key);
          if (found) {
            state.favoriteSongs.set(key, found);
            changed = true;
          } else {
            // Placeholder fallback agar lagu favorit yang pernah ditambahkan tetap muncul di daftar
            const cleanName = key.replace(/^(sc_|yt_|jamendo_)/, '').replace(/_/g, ' ');
            state.favoriteSongs.set(key, {
              id: key,
              title: cleanName,
              artist: 'Artis Favorit',
              album: 'Favorit',
              path: key.startsWith('http') || key.startsWith('sc_') || key.startsWith('yt_') || key.startsWith('jamendo_') ? '' : key,
              streamUrl: key.startsWith('http') ? key : '',
              sourceUrl: key.startsWith('http') ? key : (key.startsWith('yt_') ? `https://www.youtube.com/watch?v=${key.replace('yt_', '')}` : ''),
              duration: 0,
              isOnline: key.startsWith('http') || key.startsWith('sc_') || key.startsWith('yt_') || key.startsWith('jamendo_'),
              source: key.startsWith('yt_') ? 'YouTube Music' : (key.startsWith('sc_') ? 'SoundCloud' : 'Favorit')
            });
            changed = true;
          }
        }
      }
      if (changed) saveFavorites();
    }

    // 2. Deduplicate songs
    const seen = new Set();
    const favSongs = [];
    for (const song of state.favoriteSongs.values()) {
      const k = getSongKey(song);
      if (seen.has(k)) continue;
      seen.add(k);
      favSongs.push(song);
    }

    const query = (elements.favSearchInput?.value || '').toLowerCase().trim();
    const filtered = query
      ? favSongs.filter(s => (s.title || '').toLowerCase().includes(query) || (s.artist || '').toLowerCase().includes(query))
      : favSongs;

    elements.favCount.textContent = `${filtered.length} lagu`;
    const isEmpty = filtered.length === 0;

    elements.favPlayAll.disabled = isEmpty;
    elements.favShuffle.disabled = isEmpty;

    if (isEmpty) {
      elements.favEmpty.classList.remove('hidden');
      elements.favTrackList.classList.add('hidden');
      elements.favTrackList.innerHTML = '';
    } else {
      elements.favEmpty.classList.add('hidden');
      elements.favTrackList.classList.remove('hidden');
      const fragment = document.createDocumentFragment();
      filtered.forEach((song, idx) => {
        fragment.appendChild(createTrackRow(song, idx, filtered));
      });
      elements.favTrackList.innerHTML = '';
      elements.favTrackList.appendChild(fragment);
    }
  }

  elements.favSearchInput?.addEventListener('input', renderFavorites);
  elements.favGoOnline?.addEventListener('click', () => switchTab('online'));
  elements.favGoLib?.addEventListener('click', () => switchTab('library'));

  elements.favPlayAll?.addEventListener('click', () => {
    const seen = new Set();
    const favs = [];
    for (const song of state.favoriteSongs.values()) {
      const k = getSongKey(song);
      if (seen.has(k)) continue;
      seen.add(k);
      favs.push(song);
    }
    if (favs.length > 0) playSong(favs[0], favs, 0);
  });

  elements.favShuffle?.addEventListener('click', () => {
    const seen = new Set();
    const favs = [];
    for (const song of state.favoriteSongs.values()) {
      const k = getSongKey(song);
      if (seen.has(k)) continue;
      seen.add(k);
      favs.push(song);
    }
    if (favs.length > 0) {
      state.isShuffle = true;
      const rnd = Math.floor(Math.random() * favs.length);
      playSong(favs[rnd], favs, rnd);
    }
  });

  // ─── 4. Layar Unduhan (Desain Baru Modern) ──────────────────────────────────
  function updateDownloadsCountBadge() {
    const count = state.downloadedSongs.length;
    if (elements.railDlBadge) {
      elements.railDlBadge.textContent = count;
      elements.railDlBadge.style.display = count > 0 ? 'inline-flex' : 'none';
    }
  }

  function createDownloadTrackRow(song, index, list) {
    const songKey = getSongKey(song);
    const isCurrent = state.currentSong && (
      (song.id && state.currentSong.id === song.id) ||
      (song.path && state.currentSong.path === song.path) ||
      (song.streamUrl && state.currentSong.streamUrl === song.streamUrl)
    );
    const isPlayingThis = isCurrent && state.isPlaying;

    const row = document.createElement('div');
    row.className = `dl-row ${isCurrent ? 'playing' : ''}`;
    row.dataset.key = songKey;

    // Col 1: Num or animated EQ bars
    const numEl = document.createElement('div');
    numEl.className = 'dl-td-num';
    if (isPlayingThis) {
      numEl.innerHTML = '<div class="eq-bars"><span></span><span></span><span></span></div>';
    } else if (isCurrent) {
      numEl.innerHTML = '<span class="material-icons-round" style="font-size:20px;color:var(--accent);">play_arrow</span>';
    } else {
      numEl.textContent = (index + 1).toString().padStart(2, '0');
    }

    // Col 2: Cover & Title/Artist
    const titleCol = document.createElement('div');
    titleCol.className = 'dl-td-title';

    const thumb = document.createElement('div');
    thumb.className = 'dl-thumb';
    const coverUrl = getSongCover(song);
    if (coverUrl) {
      const img = document.createElement('img');
      img.src = coverUrl;
      img.alt = '';
      img.style.cssText = 'width:100%;height:100%;object-fit:cover;border-radius:12px;';
      thumb.appendChild(img);
    } else {
      thumb.innerHTML = '<span class="material-icons-round" style="font-size:22px;">music_note</span>';
    }

    const titleGroup = document.createElement('div');
    titleGroup.className = 'dl-title-group';
    const titleSpan = document.createElement('span');
    titleSpan.className = 'dl-title';
    titleSpan.textContent = song.title || 'Unknown Title';
    const artistSpan = document.createElement('span');
    artistSpan.className = 'dl-artist';
    artistSpan.textContent = song.artist || 'Unknown Artist';
    titleGroup.appendChild(titleSpan);
    titleGroup.appendChild(artistSpan);

    titleCol.appendChild(thumb);
    titleCol.appendChild(titleGroup);

    // Col 3: Sumber
    const sourceCol = document.createElement('div');
    sourceCol.className = 'dl-td-source';
    sourceCol.innerHTML = '<span class="material-icons-round" style="font-size:16px;color:var(--accent);">check</span> <span>MusikKu Downloads</span>';

    // Col 4: Durasi & More button
    const durCol = document.createElement('div');
    durCol.className = 'dl-td-dur';
    durCol.innerHTML = `<span>${formatTime(song.duration)}</span>`;
    const moreBtn = document.createElement('button');
    moreBtn.className = 'btn-dl-more';
    moreBtn.title = 'Opsi lagu';
    moreBtn.innerHTML = '<span class="material-icons-round">more_horiz</span>';
    moreBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      showContextMenu(e.clientX, e.clientY, song);
    });
    durCol.appendChild(moreBtn);

    row.appendChild(numEl);
    row.appendChild(titleCol);
    row.appendChild(sourceCol);
    row.appendChild(durCol);

    row.addEventListener('click', () => {
      if (isCurrent) {
        if (state.isPlaying) audio.pause();
        else audio.play();
      } else {
        playSong(song, list, index);
      }
    });

    row.addEventListener('contextmenu', (e) => {
      e.preventDefault();
      showContextMenu(e.clientX, e.clientY, song);
    });

    return row;
  }

  function renderDownloads() {
    const query = (elements.dlSearchInput?.value || '').toLowerCase().trim();
    let songs = [...state.downloadedSongs];

    // Sorting
    const sortMode = elements.dlSortSelect?.value || 'recent';
    if (sortMode === 'title') {
      songs.sort((a, b) => (a.title || '').localeCompare(b.title || ''));
    } else if (sortMode === 'duration') {
      songs.sort((a, b) => (b.duration || 0) - (a.duration || 0));
    } else {
      songs.sort((a, b) => (b.dateAdded || 0) - (a.dateAdded || 0));
    }

    if (query) {
      songs = songs.filter(s => (s.title || '').toLowerCase().includes(query) || (s.artist || '').toLowerCase().includes(query));
    }

    // Stats
    const totalDuration = songs.reduce((acc, s) => acc + (s.duration || 0), 0);
    const durMins = Math.floor(totalDuration / 60);
    const durSecs = Math.floor(totalDuration % 60);
    const durText = durMins > 0 ? `${durMins} menit ${durSecs} detik` : `${durSecs} detik`;

    if (elements.dlStatsCount) elements.dlStatsCount.textContent = `${songs.length} lagu tersimpan`;
    if (elements.dlStatsDur) elements.dlStatsDur.textContent = durText;
    if (elements.dlCount) elements.dlCount.textContent = `${songs.length} lagu`;
    updateDownloadsCountBadge();

    if (songs.length === 0) {
      elements.dlEmpty.classList.remove('hidden');
      elements.dlTrackList.innerHTML = '';
    } else {
      elements.dlEmpty.classList.add('hidden');
      const fragment = document.createDocumentFragment();
      songs.forEach((song, idx) => {
        fragment.appendChild(createDownloadTrackRow(song, idx, songs));
      });
      elements.dlTrackList.innerHTML = '';
      elements.dlTrackList.appendChild(fragment);
    }
  }

  elements.dlSearchInput?.addEventListener('input', renderDownloads);
  elements.dlSortSelect?.addEventListener('change', renderDownloads);
  elements.btnOpenDlFolder?.addEventListener('click', () => {
    window.elektronAPI?.openDownloadsFolder();
  });

  elements.dlPlayAll?.addEventListener('click', () => {
    if (state.downloadedSongs.length > 0) playSong(state.downloadedSongs[0], state.downloadedSongs, 0);
  });

  elements.dlShuffle?.addEventListener('click', () => {
    if (state.downloadedSongs.length > 0) {
      state.isShuffle = true;
      const rnd = Math.floor(Math.random() * state.downloadedSongs.length);
      playSong(state.downloadedSongs[rnd], state.downloadedSongs, rnd);
    }
  });

  elements.miniShuffle?.addEventListener('click', () => {
    state.isShuffle = !state.isShuffle;
    elements.miniShuffle?.classList.toggle('active', state.isShuffle);
    elements.heroBtnShuffle?.classList.toggle('active', state.isShuffle);
    elements.npBtnShuffle?.classList.toggle('active', state.isShuffle);
  });

  // ─── 5. Layar Playlist (Sub-Layar A & Sub-Layar B) ──────────────────────────
  function showPlaylistOverview() {
    state.activePlaylistId = null;
    elements.playlistOverview.classList.remove('hidden');
    elements.playlistDetail.classList.add('hidden');
    renderPlaylistGrid();
  }

  function renderPlaylistGrid() {
    const query = (elements.plSearchInput?.value || '').toLowerCase().trim();
    let playlists = [...state.playlists];

    if (state.playlistSort === 'name') {
      playlists.sort((a, b) => a.name.localeCompare(b.name));
    } else {
      playlists.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0));
    }

    if (query) {
      playlists = playlists.filter(p => p.name.toLowerCase().includes(query));
    }

    elements.plTotalCount.textContent = `${playlists.length} playlist`;
    elements.playlistGrid.innerHTML = '';

    playlists.forEach(pl => {
      const li = document.createElement('li');
      const card = document.createElement('a');
      card.className = 'pl';
      card.href = '#';

      const trackIds = Array.isArray(pl.trackIds) ? pl.trackIds : (Array.isArray(pl.songKeys) ? pl.songKeys : []);
      pl.trackIds = trackIds;
      const count = trackIds.length;
      let durationSec = 0;
      trackIds.forEach(id => {
        const s = findSongByKey(id);
        if (s && s.duration) durationSec += s.duration;
      });

      const metaText = count === 0 ? 'Kosong · 0 lagu' : `${count} lagu · ${formatTime(durationSec)}`;

      card.innerHTML = `
        <div class="pl-cover">
          <div class="dk disc"></div>
          <div class="sleeve" style="--g: ${pl.coverGradient || GRADIENT_PRESETS[0]}">
            <span class="material-icons-round sleeve-icon">music_note</span>
          </div>
        </div>
        <span class="pl-name">${pl.name}</span>
        <span class="pl-meta">${metaText}</span>
      `;

      card.addEventListener('click', (e) => {
        e.preventDefault();
        openPlaylistDetail(pl.id);
      });

      li.appendChild(card);
      elements.playlistGrid.appendChild(li);
    });

    // Card + Buat playlist baru
    const newLi = document.createElement('li');
    const newBtn = document.createElement('button');
    newBtn.className = 'pl pl-new-card';
    newBtn.innerHTML = `
      <div class="pl-new-circle">
        <span class="material-icons-round" style="font-size:32px;">add</span>
      </div>
      <span>Buat playlist baru</span>
    `;
    newBtn.addEventListener('click', (e) => {
      e.preventDefault();
      openPlaylistModal();
    });
    newLi.appendChild(newBtn);
    elements.playlistGrid.appendChild(newLi);
  }

  // Sort Chips
  elements.plSortRecent?.addEventListener('click', () => {
    state.playlistSort = 'recent';
    elements.plSortRecent.classList.add('active');
    elements.plSortName.classList.remove('active');
    renderPlaylistGrid();
  });

  elements.plSortName?.addEventListener('click', () => {
    state.playlistSort = 'name';
    elements.plSortName.classList.add('active');
    elements.plSortRecent.classList.remove('active');
    renderPlaylistGrid();
  });

  elements.plSearchInput?.addEventListener('input', renderPlaylistGrid);

  // Sub-Layar B: Detail Playlist
  function openPlaylistDetail(playlistId) {
    state.activePlaylistId = playlistId;
    if (state.activeTab !== 'playlists') {
      switchTab('playlists');
    }
    if (elements.playlistOverview) elements.playlistOverview.classList.add('hidden');
    if (elements.playlistDetail) elements.playlistDetail.classList.remove('hidden');
    renderPlaylistDetail(playlistId);
  }

  function renderPlaylistDetail(playlistId) {
    const pl = state.playlists.find(p => p.id === playlistId);
    if (!pl) {
      showPlaylistOverview();
      return;
    }

    const trackIds = Array.isArray(pl.trackIds) ? pl.trackIds : (Array.isArray(pl.songKeys) ? pl.songKeys : []);
    pl.trackIds = trackIds;

    if (elements.plDetailTitle) elements.plDetailTitle.textContent = pl.name || 'Playlist';
    if (elements.plDetailSleeve) elements.plDetailSleeve.style.setProperty('--g', pl.coverGradient || GRADIENT_PRESETS[0]);

    // Resolve tracks
    const tracks = trackIds.map(id => findSongByKey(id)).filter(Boolean);
    let totalSec = 0;
    tracks.forEach(t => { if (t.duration) totalSec += t.duration; });

    if (elements.plDetailMeta) elements.plDetailMeta.textContent = `${tracks.length} lagu · ${formatTimeLong(totalSec)}`;
    if (elements.plDetailTrackCount) elements.plDetailTrackCount.textContent = `${tracks.length} lagu`;

    if (elements.plDetailTrackList) {
      elements.plDetailTrackList.innerHTML = '';
      if (tracks.length === 0) {
        elements.plDetailTrackList.innerHTML = `
          <div style="padding: 40px 0; text-align: center; color: var(--muted); font-size: 14px;">
            Playlist ini belum memiliki lagu. Klik "Tambah lagu dari perpustakaan" di bawah.
          </div>
        `;
      } else {
        const fragment = document.createDocumentFragment();
        tracks.forEach((track, idx) => {
          fragment.appendChild(createTrackRow(track, idx, tracks, {
            onRemove: (songToRemove) => {
              const key = songToRemove.path || songToRemove.streamUrl;
              pl.trackIds = pl.trackIds.filter(id => id !== key);
              pl.updatedAt = Date.now();
              savePlaylists();
              renderPlaylistDetail(playlistId);
            }
          }));
        });
        elements.plDetailTrackList.appendChild(fragment);
      }
    }
  }

  elements.btnBackPlaylists?.addEventListener('click', showPlaylistOverview);

  elements.plDetailPlayAll?.addEventListener('click', () => {
    if (!state.activePlaylistId) return;
    const pl = state.playlists.find(p => p.id === state.activePlaylistId);
    if (!pl) return;
    const trackIds = Array.isArray(pl.trackIds) ? pl.trackIds : [];
    const tracks = trackIds.map(id => findSongByKey(id)).filter(Boolean);
    if (tracks.length > 0) playSong(tracks[0], tracks, 0);
  });

  elements.plDetailShuffle?.addEventListener('click', () => {
    if (!state.activePlaylistId) return;
    const pl = state.playlists.find(p => p.id === state.activePlaylistId);
    if (!pl) return;
    const trackIds = Array.isArray(pl.trackIds) ? pl.trackIds : [];
    const tracks = trackIds.map(id => findSongByKey(id)).filter(Boolean);
    if (tracks.length > 0) {
      state.isShuffle = true;
      const rnd = Math.floor(Math.random() * tracks.length);
      playSong(tracks[rnd], tracks, rnd);
    }
  });

  // Ubah Nama Playlist
  elements.plDetailRename?.addEventListener('click', () => {
    if (!state.activePlaylistId) return;
    const pl = state.playlists.find(p => p.id === state.activePlaylistId);
    if (pl) openPlaylistModal(pl);
  });

  // ─── Modal Konfirmasi Universal (Dialog Kustom) ───────────────────────────
  let confirmDialogPromiseResolve = null;

  function showConfirmModal({
    title = 'Konfirmasi',
    subtitle = 'Tindakan ini tidak dapat dibatalkan',
    message = 'Apakah Anda yakin ingin melanjutkan?',
    confirmText = 'Hapus',
    cancelText = 'Batal',
    icon = 'delete_outline',
    isDanger = true
  } = {}) {
    return new Promise((resolve) => {
      confirmDialogPromiseResolve = resolve;

      if (elements.confirmModalTitle) elements.confirmModalTitle.textContent = title;
      if (elements.confirmModalSub) elements.confirmModalSub.textContent = subtitle;
      if (elements.confirmModalMessage) elements.confirmModalMessage.innerHTML = message;
      if (elements.confirmActionLabel) elements.confirmActionLabel.textContent = confirmText;
      if (elements.confirmBtnCancel) elements.confirmBtnCancel.textContent = cancelText;

      if (elements.confirmModalIcon) elements.confirmModalIcon.textContent = icon;
      if (elements.confirmActionIcon) elements.confirmActionIcon.textContent = icon === 'delete_outline' ? 'delete' : icon;

      if (elements.confirmModalBadge) {
        elements.confirmModalBadge.className = `modal-badge-icon ${isDanger ? 'badge-danger' : ''}`;
      }
      if (elements.confirmBtnAction) {
        elements.confirmBtnAction.className = `btn ${isDanger ? 'btn-danger' : 'btn-primary'}`;
      }

      if (elements.confirmModal) {
        elements.confirmModal.style.display = 'flex';
        elements.confirmModal.classList.remove('hidden');
      }
    });
  }

  function closeConfirmModal(result = false) {
    if (elements.confirmModal) {
      elements.confirmModal.classList.add('hidden');
      elements.confirmModal.style.display = 'none';
    }
    if (confirmDialogPromiseResolve) {
      const res = confirmDialogPromiseResolve;
      confirmDialogPromiseResolve = null;
      res(result);
    }
  }

  elements.confirmCloseBtn?.addEventListener('click', () => closeConfirmModal(false));
  elements.confirmBtnCancel?.addEventListener('click', () => closeConfirmModal(false));
  elements.confirmBtnAction?.addEventListener('click', () => closeConfirmModal(true));
  elements.confirmModal?.addEventListener('click', (e) => {
    if (e.target === elements.confirmModal) {
      closeConfirmModal(false);
    }
  });

  // Hapus Playlist dengan Konfirmasi Kustom (Modern Modal)
  elements.plDetailDelete?.addEventListener('click', async () => {
    if (!state.activePlaylistId) return;
    const pl = state.playlists.find(p => p.id === state.activePlaylistId);
    if (!pl) return;

    const confirmed = await showConfirmModal({
      title: 'Hapus Playlist?',
      subtitle: 'Tindakan ini tidak dapat dibatalkan',
      message: `Yakin ingin menghapus playlist <strong>"${pl.name}"</strong>?<br>Lagu-lagu di dalamnya tetap tersimpan dan tidak akan dihapus dari perangkatmu.`,
      confirmText: 'Hapus Playlist',
      cancelText: 'Batal',
      icon: 'delete_outline',
      isDanger: true
    });

    if (confirmed) {
      state.playlists = state.playlists.filter(p => p.id !== pl.id);
      savePlaylists();
      showPlaylistOverview();
      showDownloadToast(`Playlist "${pl.name}" berhasil dihapus`, 100);
      hideDownloadToast(2000);
    }
  });

  // ─── Modal Buat / Ubah Playlist ────────────────────────────────────────────
  let editingPlaylist = null;
  let selectedGradient = GRADIENT_PRESETS[0];
  let pendingSongToAdd = null;

  function closePlaylistModal() {
    if (elements.playlistModal) {
      elements.playlistModal.classList.add('hidden');
      elements.playlistModal.style.display = 'none';
    }
    const modal = document.getElementById('playlist-modal');
    if (modal) {
      modal.classList.add('hidden');
      modal.style.display = 'none';
    }
    editingPlaylist = null;
    pendingSongToAdd = null;
    if (elements.inputPlName) {
      elements.inputPlName.value = '';
    }
  }

  function openPlaylistModal(pl = null, songToAdd = null) {
    editingPlaylist = pl;
    pendingSongToAdd = songToAdd;
    if (elements.playlistModal) {
      elements.playlistModal.style.display = 'flex';
      elements.playlistModal.classList.remove('hidden');
    }
    const modal = document.getElementById('playlist-modal');
    if (modal) {
      modal.style.display = 'flex';
      modal.classList.remove('hidden');
    }

    if (pl) {
      if (elements.plModalTitle) elements.plModalTitle.textContent = 'Ubah Playlist';
      if (elements.inputPlName) elements.inputPlName.value = pl.name || '';
      selectedGradient = pl.coverGradient || GRADIENT_PRESETS[0];
    } else {
      if (elements.plModalTitle) elements.plModalTitle.textContent = 'Playlist Baru';
      if (elements.inputPlName) elements.inputPlName.value = '';
      selectedGradient = GRADIENT_PRESETS[Math.floor(Math.random() * GRADIENT_PRESETS.length)];
    }

    renderGradientPicker();

    // Langsung fokus ke input teks agar pengguna bisa langsung mengetik tanpa delay / cooldown
    requestAnimationFrame(() => {
      elements.inputPlName?.focus();
      elements.inputPlName?.select();
    });
    setTimeout(() => {
      elements.inputPlName?.focus();
      elements.inputPlName?.select();
    }, 40);
  }

  function renderGradientPicker() {
    if (!elements.plGradientPicker) return;
    elements.plGradientPicker.innerHTML = '';
    GRADIENT_PRESETS.forEach(grad => {
      const choice = document.createElement('div');
      choice.className = `grad-choice ${grad === selectedGradient ? 'selected' : ''}`;
      choice.style.background = grad;
      choice.addEventListener('click', () => {
        selectedGradient = grad;
        renderGradientPicker();
      });
      elements.plGradientPicker.appendChild(choice);
    });
  }

  function handleSavePlaylist() {
    const name = (elements.inputPlName?.value || '').trim();
    if (!name) {
      elements.inputPlName?.focus();
      return;
    }

    // Segera tutup modal form agar popup langsung hilang dari layar
    const targetEditing = editingPlaylist;
    const targetSong = pendingSongToAdd;
    closePlaylistModal();

    try {
      if (targetEditing) {
        targetEditing.name = name;
        targetEditing.coverGradient = selectedGradient;
        targetEditing.updatedAt = Date.now();
        if (!Array.isArray(targetEditing.trackIds)) targetEditing.trackIds = [];
        savePlaylists();
        renderPlaylistGrid();
        openPlaylistDetail(targetEditing.id);
        showDownloadToast(`Playlist "${name}" berhasil diperbarui`, 100);
      } else {
        const initialTracks = [];
        if (targetSong) {
          const songKey = getSongKey(targetSong);
          if (songKey) initialTracks.push(songKey);
        }
        const newPl = {
          id: 'pl_' + Date.now(),
          name: name,
          coverGradient: selectedGradient,
          trackIds: initialTracks,
          createdAt: Date.now(),
          updatedAt: Date.now()
        };
        state.playlists.unshift(newPl);
        savePlaylists();
        renderPlaylistGrid();
        // Langsung buka playlist yang baru saja dibuat
        openPlaylistDetail(newPl.id);
        const toastMsg = targetSong
          ? `Playlist "${name}" dibuat & lagu ditambahkan`
          : `Playlist "${name}" berhasil dibuat`;
        showDownloadToast(toastMsg, 100);
      }
      hideDownloadToast(2000);
    } catch (err) {
      console.error('Error saat menyimpan playlist:', err);
    }
  }

  elements.btnNewPlaylist?.addEventListener('click', (e) => {
    e.preventDefault();
    openPlaylistModal();
  });
  elements.btnClosePlModal?.addEventListener('click', (e) => {
    e.preventDefault();
    closePlaylistModal();
  });
  elements.btnCancelPlModal?.addEventListener('click', (e) => {
    e.preventDefault();
    closePlaylistModal();
  });

  elements.playlistModal?.addEventListener('click', (e) => {
    if (e.target === elements.playlistModal) {
      closePlaylistModal();
    }
  });

  elements.inputPlName?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSavePlaylist();
    }
  });

  elements.btnSavePlaylist?.addEventListener('click', (e) => {
    e.preventDefault();
    handleSavePlaylist();
  });

  // ─── Modal Tambah Lagu Ini ke Playlist (Pilih Playlist) ────────────────────
  let songTargetForPlaylist = null;

  function openSongToPlaylistModal(song) {
    if (!song) {
      showDownloadToast('Tidak ada lagu yang sedang diputar', 0);
      hideDownloadToast(2000);
      return;
    }
    songTargetForPlaylist = song;

    if (elements.songPlModalTitle) elements.songPlModalTitle.textContent = 'Tambah ke Playlist';
    if (elements.songPlModalSub) elements.songPlModalSub.textContent = song.title || 'Pilih playlist untuk lagu ini';

    renderSongPlaylistList();

    if (elements.songToPlModal) {
      elements.songToPlModal.style.display = 'flex';
      elements.songToPlModal.classList.remove('hidden');
    }
  }

  function closeSongToPlaylistModal() {
    if (elements.songToPlModal) {
      elements.songToPlModal.classList.add('hidden');
      elements.songToPlModal.style.display = 'none';
    }
    songTargetForPlaylist = null;
  }

  function renderSongPlaylistList() {
    if (!elements.songPlList || !songTargetForPlaylist) return;
    elements.songPlList.innerHTML = '';
    const songKey = getSongKey(songTargetForPlaylist);

    if (state.playlists.length === 0) {
      elements.songPlList.innerHTML = `
        <div style="padding: 24px 12px; text-align: center; color: var(--muted); font-size: 13px;">
          Belum ada playlist. Buat playlist baru menggunakan tombol di atas.
        </div>
      `;
      return;
    }

    state.playlists.forEach(pl => {
      if (!Array.isArray(pl.trackIds)) {
        pl.trackIds = Array.isArray(pl.songKeys) ? pl.songKeys : [];
      }
      const isAlreadyIn = pl.trackIds.includes(songKey);
      const count = pl.trackIds.length;

      const item = document.createElement('div');
      item.className = `song-pl-item ${isAlreadyIn ? 'active' : ''}`;
      item.innerHTML = `
        <div class="song-pl-sleeve" style="background: ${pl.coverGradient || GRADIENT_PRESETS[0]}">
          <span class="material-icons-round" style="font-size:18px; color:#fff;">queue_music</span>
        </div>
        <div class="song-pl-info">
          <span class="song-pl-name">${pl.name}</span>
          <span class="song-pl-count">${count} lagu</span>
        </div>
        <div class="song-pl-check">
          <span class="material-icons-round">${isAlreadyIn ? 'check_circle' : 'add_circle_outline'}</span>
        </div>
      `;

      item.addEventListener('click', () => {
        if (isAlreadyIn) {
          pl.trackIds = pl.trackIds.filter(id => id !== songKey);
          pl.updatedAt = Date.now();
          savePlaylists();
          showDownloadToast(`Dihapus dari "${pl.name}"`, 100);
        } else {
          pl.trackIds.push(songKey);
          pl.updatedAt = Date.now();
          savePlaylists();
          showDownloadToast(`Ditambahkan ke "${pl.name}"`, 100);
        }
        hideDownloadToast(2000);

        renderSongPlaylistList();
        renderPlaylistGrid();
        if (state.activePlaylistId === pl.id) {
          renderPlaylistDetail(pl.id);
        }
      });

      elements.songPlList.appendChild(item);
    });
  }

  elements.btnCloseSongPlModal?.addEventListener('click', closeSongToPlaylistModal);
  elements.btnDoneSongPl?.addEventListener('click', closeSongToPlaylistModal);
  elements.songToPlModal?.addEventListener('click', (e) => {
    if (e.target === elements.songToPlModal) {
      closeSongToPlaylistModal();
    }
  });

  elements.btnSongPlNew?.addEventListener('click', () => {
    const song = songTargetForPlaylist;
    closeSongToPlaylistModal();
    openPlaylistModal(null, song);
  });

  // ─── Modal Tambah Lagu ke Playlist (Picker) ────────────────────────────────
  elements.btnAddToPlPicker?.addEventListener('click', () => {
    if (!state.activePlaylistId) return;
    openAddToPlaylistModal(state.activePlaylistId);
  });

  function openAddToPlaylistModal(playlistId) {
    const pl = state.playlists.find(p => p.id === playlistId);
    if (!pl) return;

    if (!Array.isArray(pl.trackIds)) {
      pl.trackIds = Array.isArray(pl.songKeys) ? pl.songKeys : [];
    }

    elements.addToPlModal.classList.remove('hidden');
    elements.pickerSearchInput.value = '';

    const currentIds = new Set(pl.trackIds);
    const allTracks = getAllKnownSongs();

    function renderPickerList() {
      const q = elements.pickerSearchInput.value.toLowerCase().trim();
      const list = q ? allTracks.filter(t => (t.title || '').toLowerCase().includes(q) || (t.artist || '').toLowerCase().includes(q)) : allTracks;

      elements.pickerTrackList.innerHTML = '';
      if (list.length === 0) {
        elements.pickerTrackList.innerHTML = '<div style="padding:20px; text-align:center; color:var(--muted);">Tidak ada lagu ditemukan.</div>';
        return;
      }

      list.forEach(t => {
        const key = t.path || t.streamUrl;
        const item = document.createElement('label');
        item.className = 'picker-item';

        const chk = document.createElement('input');
        chk.type = 'checkbox';
        chk.checked = currentIds.has(key);

        chk.addEventListener('change', () => {
          if (chk.checked) {
            if (!pl.trackIds.includes(key)) pl.trackIds.push(key);
          } else {
            pl.trackIds = pl.trackIds.filter(id => id !== key);
          }
          pl.updatedAt = Date.now();
          savePlaylists();
          renderPlaylistDetail(playlistId);
        });

        const info = document.createElement('div');
        info.className = 'picker-item-info';
        info.innerHTML = `
          <span class="picker-title">${t.title || 'Unknown Title'}</span>
          <span class="picker-artist">${t.artist || 'Unknown Artist'} · ${formatTime(t.duration)}</span>
        `;

        item.appendChild(chk);
        item.appendChild(info);
        elements.pickerTrackList.appendChild(item);
      });
    }

    elements.pickerSearchInput.oninput = renderPickerList;
    renderPickerList();
  }

  elements.btnClosePickerModal?.addEventListener('click', () => elements.addToPlModal.classList.add('hidden'));
  elements.btnCancelPicker?.addEventListener('click', () => elements.addToPlModal.classList.add('hidden'));

  // ─── Klik Kanan Context Menu (Tambah ke Playlist) ──────────────────────────
  let contextSong = null;

  function showContextMenu(x, y, song) {
    contextSong = song;
    const songKey = getSongKey(song);
    const isFav = isSongFavorite(song);

    elements.ctxFav.innerHTML = `
      <span class="material-icons-round">${isFav ? 'favorite' : 'favorite_border'}</span>
      ${isFav ? 'Hapus dari favorit' : 'Tambah ke favorit'}
    `;

    const hasCustomCover = Boolean(state.customCovers && (state.customCovers[songKey] || (song.id && state.customCovers[song.id]) || (song.path && state.customCovers[song.path])));
    if (elements.ctxResetCover) {
      elements.ctxResetCover.classList.toggle('hidden', !hasCustomCover);
    }

    // Populate playlists
    elements.ctxPlList.innerHTML = '';
    state.playlists.forEach(pl => {
      if (!Array.isArray(pl.trackIds)) {
        pl.trackIds = Array.isArray(pl.songKeys) ? pl.songKeys : [];
      }
      const alreadyIn = pl.trackIds.includes(songKey);
      const div = document.createElement('div');
      div.className = 'ctx-item';
      div.innerHTML = `
        <span class="material-icons-round">${alreadyIn ? 'check' : 'add'}</span>
        ${pl.name}
      `;
      div.addEventListener('click', () => {
        if (!alreadyIn) {
          pl.trackIds.push(songKey);
          pl.updatedAt = Date.now();
          savePlaylists();
          showDownloadToast(`Ditambahkan ke ${pl.name}`, 100);
          hideDownloadToast(2000);
        }
        hideContextMenu();
      });
      elements.ctxPlList.appendChild(div);
    });

    elements.contextMenu.style.left = `${Math.min(window.innerWidth - 240, x)}px`;
    elements.contextMenu.style.top = `${Math.min(window.innerHeight - 300, y)}px`;
    elements.contextMenu.classList.remove('hidden');
  }

  function hideContextMenu() {
    elements.contextMenu.classList.add('hidden');
    contextSong = null;
  }

  window.addEventListener('click', hideContextMenu);
  elements.ctxPlay?.addEventListener('click', () => {
    if (contextSong) playSong(contextSong, [contextSong], 0);
    hideContextMenu();
  });
  elements.ctxFav?.addEventListener('click', () => {
    if (contextSong) toggleFavorite(contextSong);
    hideContextMenu();
  });
  elements.ctxChangeCover?.addEventListener('click', () => {
    if (contextSong) pickAndChangeCover(contextSong);
    hideContextMenu();
  });
  elements.ctxResetCover?.addEventListener('click', () => {
    if (contextSong) {
      setCustomCover(contextSong, null);
      showDownloadToast(`Foto kustom dihapus untuk "${contextSong.title || 'Lagu'}"`, 100);
      hideDownloadToast(2000);
    }
    hideContextMenu();
  });
  elements.ctxNewPl?.addEventListener('click', () => {
    const s = contextSong;
    hideContextMenu();
    openPlaylistModal(null, s);
  });

  // ─── Tools Download URL Modal ──────────────────────────────────────────────
  elements.btnOpenDlModal?.addEventListener('click', () => {
    elements.downloadModal.classList.remove('hidden');
    elements.inputDlUrl.value = '';
    elements.inputDlTitle.value = '';
    elements.inputDlArtist.value = '';
    elements.modalDlProgress.classList.add('hidden');
    elements.btnStartUrlDownload.disabled = false;
  });

  elements.inputDlUrl?.addEventListener('input', async (e) => {
    const val = e.target.value.trim();
    if (!val) return;
    const ytMatch = val.match(/(?:youtu\.be\/|v\/|u\/\w\/|embed\/|watch\?v=|\&v=)([^#\&\?]{11})/i);
    if (ytMatch && (!elements.inputDlTitle.value.trim() || !elements.inputDlArtist.value.trim())) {
      try {
        const oembedUrl = `https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=${ytMatch[1]}&format=json`;
        const res = await fetch(oembedUrl);
        if (res.ok) {
          const data = await res.json();
          if (data && data.title) {
            let t = data.title;
            let a = data.author_name || '';
            if (t.includes(' - ')) {
              const parts = t.split(' - ');
              a = parts[0].trim();
              t = parts[1].replace(/\(.*?\)|\[.*?\]/g, '').trim();
            }
            if (!elements.inputDlTitle.value.trim()) elements.inputDlTitle.value = t;
            if (!elements.inputDlArtist.value.trim()) elements.inputDlArtist.value = a;
          }
        }
      } catch (err) {}
    }
  });

  elements.btnCloseModal?.addEventListener('click', () => elements.downloadModal.classList.add('hidden'));
  elements.btnCancelModal?.addEventListener('click', () => elements.downloadModal.classList.add('hidden'));

  elements.btnStartUrlDownload?.addEventListener('click', async () => {
    const url = elements.inputDlUrl.value.trim();
    if (!url) {
      elements.inputDlUrl.focus();
      return;
    }

    const title = elements.inputDlTitle.value.trim() || 'MusikKu Audio';
    const artist = elements.inputDlArtist.value.trim() || 'Unknown Artist';

    elements.btnStartUrlDownload.disabled = true;
    elements.modalDlProgress.classList.remove('hidden');
    elements.modalProgressFill.style.width = '10%';
    elements.modalProgressText.textContent = 'Menghubungkan ke server...';

    try {
      const result = await window.elektronAPI.downloadTrack({
        id: 'url_' + Date.now(),
        sourceUrl: url,
        streamUrl: url,
        downloadUrl: url,
        title,
        artist,
        album: 'URL Download'
      });

      if (result && result.success) {
        elements.modalProgressFill.style.width = '100%';
        elements.modalProgressText.textContent = 'Unduhan Berhasil!';

        const downloadedItem = {
          id: 'url_' + Date.now(),
          title: result.title || title,
          artist: result.artist || artist,
          album: 'MusikKu Downloads',
          path: result.filePath,
          duration: result.duration || 0,
          cover: result.cover || '',
          size: result.size || 0,
          isDownloaded: true
        };

        state.downloadedSongs.unshift(downloadedItem);
        saveDownloads();
        state.allSongs.unshift(downloadedItem);
        fetchMetadataAsync(downloadedItem);

        setTimeout(() => {
          elements.downloadModal.classList.add('hidden');
          if (state.activeTab === 'downloads') renderDownloads();
          else if (state.activeTab === 'library') applyFilterAndRenderLibrary();
        }, 1200);
      }
    } catch (err) {
      console.error('URL download error:', err);
      elements.modalProgressFill.style.width = '0%';
      elements.modalProgressText.textContent = 'Gagal mengunduh: ' + (err.message || 'Error');
      elements.btnStartUrlDownload.disabled = false;
    }
  });

  // ─── Downloader Logic (Single Track) ───────────────────────────────────────
  function showDownloadToast(title, progress = 0) {
    elements.downloadToast.classList.remove('hidden');
    elements.toastTitle.textContent = title;
    elements.toastBarFill.style.width = `${progress}%`;
  }

  function hideDownloadToast(delay = 2500) {
    setTimeout(() => {
      elements.downloadToast.classList.add('hidden');
    }, delay);
  }

  function handleDownloadProgress(data) {
    if (elements.modalDlProgress && !elements.modalDlProgress.classList.contains('hidden')) {
      if (data.progress) {
        elements.modalProgressFill.style.width = `${data.progress}%`;
        elements.modalProgressText.textContent = data.progress >= 100 ? 'Memproses penyimpanan file...' : `Mengunduh (${data.progress}%)...`;
      }
    }
    if (data.status === 'completed' || data.progress >= 100) {
      showDownloadToast('Unduhan selesai!', 100);
      hideDownloadToast(3000);
    } else {
      showDownloadToast(`Mengunduh (${data.progress}%)...`, data.progress);
    }
  }

  async function downloadSingleTrack(track, btnEl = null) {
    if (!window.elektronAPI?.downloadTrack) return;

    if (btnEl) {
      btnEl.innerHTML = '<span class="material-icons-round">sync</span>';
      btnEl.disabled = true;
    }

    showDownloadToast(`Mengunduh: ${track.title}...`, 5);

    try {
      const result = await window.elektronAPI.downloadTrack(track);
      if (result && result.success) {
        showDownloadToast(`Selesai diunduh: ${track.title}`, 100);
        hideDownloadToast(3000);

        const newDownloadedSong = {
          id: track.id,
          title: result.title || track.title,
          artist: result.artist || track.artist,
          album: result.album || track.album,
          path: result.filePath,
          duration: track.duration || 0,
          cover: track.cover || '',
          size: result.size || 0
        };

        state.downloadedSongs.unshift(newDownloadedSong);
        saveDownloads();
        state.allSongs.unshift(newDownloadedSong);

        if (btnEl) {
          btnEl.innerHTML = '<span class="material-icons-round">cloud_done</span>';
          btnEl.title = 'Tersimpan Offline';
        }

        if (state.activeTab === 'downloads') renderDownloads();
      }
    } catch (err) {
      console.error('Download error:', err);
      showDownloadToast('Gagal mengunduh lagu', 0);
      hideDownloadToast(4000);
      if (btnEl) {
        btnEl.innerHTML = '<span class="material-icons-round">download</span>';
        btnEl.disabled = false;
      }
    }
  }

  // ─── Async Metadata Fetcher ────────────────────────────────────────────────
  async function fetchMetadataAsync(song) {
    if (state.metadataCache.has(song.path)) {
      const meta = state.metadataCache.get(song.path);
      Object.assign(song, meta);
      updatePlayerUI();
      renderCurrentScreenTracks();
      return;
    }

    if (!window.elektronAPI?.readMetadata) return;

    try {
      const meta = await window.elektronAPI.readMetadata(song.path);
      if (meta) {
        state.metadataCache.set(song.path, meta);
        Object.assign(song, meta);
        if (state.currentSong && state.currentSong.path === song.path) {
          updatePlayerUI();
        }
        renderCurrentScreenTracks();
      }
    } catch (e) {
      console.warn('Metadata read error:', e);
    }
  }

  // ─── Folder Scanning & Library Management ──────────────────────────────────
  async function scanFolder(folderPath) {
    if (!window.elektronAPI?.scanMusicDirectory) return [];
    try {
      const songs = await window.elektronAPI.scanMusicDirectory(folderPath);
      return songs || [];
    } catch (err) {
      console.error('Scan error:', err);
      return [];
    }
  }

  async function addFolder() {
    if (!window.elektronAPI?.openMusicFolder) return;

    const folderPath = await window.elektronAPI.openMusicFolder();
    if (!folderPath) return;

    if (!state.scannedFolders.includes(folderPath)) {
      state.scannedFolders.push(folderPath);
      saveFolders();
    }

    const newSongs = await scanFolder(folderPath);
    mergeSongs(newSongs);
  }

  function mergeSongs(newSongs) {
    const existingPaths = new Set(state.allSongs.map(s => s.path));
    const toAdd = newSongs.filter(s => !existingPaths.has(s.path));

    state.allSongs = [...state.allSongs, ...toAdd];
    syncLegacyFavorites();
    applyFilterAndRenderLibrary();

    toAdd.slice(0, 30).forEach(song => fetchMetadataAsync(song));
  }

  elements.btnAddFolder?.addEventListener('click', addFolder);

  // ─── App Initialization ────────────────────────────────────────────────────
  async function init() {
    migratePlaylistsIfNeeded();
    renderTunerStops();

    if (!window.elektronAPI) {
      console.warn('Electron API tidak terdeteksi. Berjalan dalam mode web preview.');
      applyFilterAndRenderLibrary();
      return;
    }

    // Media key listener
    window.elektronAPI.onMediaKey?.((action) => {
      if (action === 'play-pause') togglePlayPause();
      else if (action === 'next') playNext();
      else if (action === 'previous') playPrevious();
    });

    // Download progress listener
    window.elektronAPI.onDownloadProgress?.((data) => {
      handleDownloadProgress(data);
    });

    // Auto-scan Downloads folder
    try {
      const dlFolder = await window.elektronAPI.getDownloadsFolder();
      if (dlFolder) {
        const dlFiles = await scanFolder(dlFolder);
        if (dlFiles.length > 0) {
          state.downloadedSongs = dlFiles.map(f => ({ ...f, isDownloaded: true }));
          saveDownloads();
          mergeSongs(dlFiles);
        }
      }
    } catch (e) {
      console.warn('Could not auto-scan downloads:', e);
    }

    // Perbarui metadata & durasi lagu-lagu unduhan yang masih 0
    state.downloadedSongs.forEach(s => {
      if ((!s.duration || s.duration === 0) && s.path) {
        fetchMetadataAsync(s);
      }
    });

    // Load saved folders
    if (state.scannedFolders.length > 0) {
      for (const folder of state.scannedFolders) {
        const songs = await scanFolder(folder);
        mergeSongs(songs);
      }
    } else {
      try {
        const defaultPath = await window.elektronAPI.getDefaultMusicPath();
        if (defaultPath) {
          state.scannedFolders.push(defaultPath);
          saveFolders();
          const songs = await scanFolder(defaultPath);
          mergeSongs(songs);
        }
      } catch (err) {
        console.warn('Could not read default music folder:', err);
      }
    }

    applyFilterAndRenderLibrary();
  }

  init();
})();
