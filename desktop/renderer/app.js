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
    autoScrollLyrics: true,

    // Preferensi & Pengaturan
    theme: localStorage.getItem('musikku_desktop_theme') || 'light',
    language: localStorage.getItem('musikku_desktop_language') || 'id',
    autoPlayNext: localStorage.getItem('musikku_autoplay') !== 'false',
    audioQuality: localStorage.getItem('musikku_audio_quality') || 'standard',
    sleepTimerMinutes: 0,
    sleepTimerRemaining: 0,
    sleepTimerInterval: null
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
    } else if (tabName === 'settings') {
      renderSettingsView();
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
    } else if (state.autoPlayNext !== false) {
      playNext();
    } else {
      audio.pause();
      state.isPlaying = false;
      updatePlayerUI();
      renderCurrentScreenTracks();
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
    slider.style.background = `linear-gradient(to right, var(--accent) 0%, var(--accent) ${pct}%, var(--line-strong) ${pct}%, var(--line-strong) 100%)`;
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
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA' || e.target.tagName === 'SELECT') return;
    if (e.code === 'Escape' && state.isNowPlayingOpen) {
      closeNowPlayingScreen();
      return;
    }
    if (e.code === 'Space') {
      e.preventDefault();
      togglePlayPause();
    } else if (e.ctrlKey && e.code === 'ArrowRight') {
      e.preventDefault();
      playNext();
    } else if (e.ctrlKey && e.code === 'ArrowLeft') {
      e.preventDefault();
      playPrevious();
    } else if (e.ctrlKey && e.code === 'ArrowUp') {
      e.preventDefault();
      setVolume(Math.min(1, state.volume + 0.05));
    } else if (e.ctrlKey && e.code === 'ArrowDown') {
      e.preventDefault();
      setVolume(Math.max(0, state.volume - 0.05));
    } else if (e.code === 'KeyM') {
      e.preventDefault();
      if (audio.muted || state.volume === 0) {
        audio.muted = false;
        setVolume(state.lastVolume || 0.8);
      } else {
        state.lastVolume = state.volume;
        setVolume(0);
      }
    } else if (e.code === 'KeyF') {
      if (state.currentSong) {
        e.preventDefault();
        toggleFavorite(state.currentSong);
      }
    } else if (e.code === 'KeyL') {
      e.preventDefault();
      state.repeatMode = (state.repeatMode + 1) % 3;
      updatePlayerUI();
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

  // ─── Layar Pengaturan (Settings Logic) ────────────────────────────────────
  function applyTheme(themeName) {
    state.theme = themeName || 'light';
    localStorage.setItem('musikku_desktop_theme', state.theme);
    if (state.theme === 'light') {
      document.body.removeAttribute('data-theme');
    } else {
      document.body.setAttribute('data-theme', state.theme);
    }
    updateThemeCardsUI();
  }

  function updateThemeCardsUI() {
    const cards = document.querySelectorAll('.theme-option-card');
    cards.forEach(card => {
      const t = card.dataset.theme;
      card.classList.toggle('active', t === state.theme);
    });
  }

  // ─── Kamus Terjemahan Multibahasa (i18n) ──────────────────────────────────
  const TRANSLATIONS = {
    id: {
      nav_space: 'RUANG MUSIKMU',
      nav_library: 'Perpustakaan',
      nav_online: 'Jelajah online',
      nav_favorites: 'Favorit',
      nav_downloads: 'Unduhan',
      nav_playlists: 'Playlist',
      nav_settings: 'Pengaturan',
      btn_add_folder: 'Tambah folder',
      btn_dl_url: 'Download dari URL',
      tagline: 'Musikmu. Ruangmu.',
      lib_eyebrow: 'MUSIKKU · PERPUSTAKAAN',
      lib_search_placeholder: 'Cari lagu, artis, album...',
      recent_searches: 'Riwayat Pencarian',
      clear_all: 'Hapus Semua',
      popular_suggestions: 'Saran Populer',
      now_playing_eyebrow: 'SEDANG DIPUTAR',
      now_playing_empty: 'Pilih lagu dari perpustakaan atau jelajah',
      collection_title: 'Koleksi',
      play_all: 'Putar semua',
      quick_add_folder_title: 'Tambah folder',
      quick_add_folder_sub: 'Pindai musik dari komputermu',
      quick_dl_title: 'Unduh dari URL',
      quick_dl_sub: 'Tempel tautan, simpan offline',
      empty_lib_title: 'Perpustakaan Kosong',
      empty_lib_desc: 'Tambahkan folder musik dari komputermu untuk mulai mendengarkan.',
      explore_eyebrow: 'STREAMING JUTAAN MUSIK ONLINE',
      explore_heading: 'Jelajah',
      explore_search_placeholder: 'Cari musik online (SZA, Daniel Caesar, pop, dll)...',
      source_youtube: 'YouTube Music',
      source_jamendo: 'Jamendo (Bebas Royalti)',
      tuner_eyebrow: 'PUTAR PENALA',
      active_channel: 'Saluran aktif:',
      empty_search_title: 'Mulai Mencari Musik',
      empty_search_desc: 'Ketik judul lagu, nama artis, atau album di bilah pencarian atas, atau putar penala genre untuk menemukan lagu menarik.',
      load_more: 'Muat Lebih Banyak Lagu',
      fav_eyebrow: 'FAVORITMU',
      fav_title: 'Favorit',
      fav_search_placeholder: 'Cari dalam favorit...',
      empty_fav_eyebrow: 'PIRINGAN KOSONG',
      empty_fav_title: 'Belum ada lagu<br />favorit',
      empty_fav_desc: 'Klik ikon hati pada lagu apa saja untuk menambahkannya ke favorit.',
      btn_fav_explore: 'Jelajahi musik online',
      btn_fav_lib: 'Buka perpustakaan',
      dl_breadcrumb: 'Perpustakaan / Unduhan',
      dl_search_placeholder: 'Cari di koleksi unduhan...',
      dl_tag: 'TERSEDIA OFFLINE',
      dl_hero_heading: 'Musik favorit.<br>Tanpa batas sinyal.',
      dl_hero_sub: 'Semua lagu unduhanmu, dalam satu ruang. Siap menemani kapan pun kamu mau.',
      dl_card_title: 'Lagu unduhan',
      dl_open_folder: 'Buka folder',
      sort_recent: 'Terbaru',
      sort_title: 'Judul (A–Z)',
      sort_duration: 'Durasi',
      sort_name: 'Nama A–Z',
      dl_th_title: 'JUDUL LAGU',
      dl_th_source: 'SUMBER',
      dl_th_dur: 'DURASI',
      dl_empty_title: 'Belum ada lagu unduhan',
      dl_empty_desc: 'Unduh lagu dari tab "Jelajah" atau gunakan tombol tautan di bilah kiri untuk menyimpan lagu offline.',
      dl_footer_note: 'Tersimpan di perangkatmu. Nikmati musik tanpa koneksi internet.',
      pl_eyebrow: 'DAFTAR PUTAR',
      pl_title: 'Playlist',
      pl_search_placeholder: 'Cari playlist...',
      btn_new_pl: 'Playlist baru',
      btn_back_pl: 'Semua playlist',
      pl_detail_songs: 'Isi playlist',
      btn_add_to_pl_picker: 'Tambah lagu dari perpustakaan',
      settings_eyebrow: 'MUSIKKU · PENGATURAN',
      settings_title: 'Pengaturan',
      settings_badge: 'Preferensi & Sistem',
      settings_theme_title: 'Tema & Tampilan',
      settings_theme_desc: 'Pilih gaya visual yang paling nyaman untuk matamu',
      settings_lang_title: 'Bahasa Aplikasi',
      settings_lang_desc: 'Pilih bahasa tampilan antarmuka untuk MusikKu',
      settings_audio_title: 'Pemutaran & Audio',
      settings_audio_desc: 'Kontrol pengatur waktu tidur dan alur pemutaran musik',
      settings_timer_title: 'Sleep Timer (Pengatur Waktu Tidur)',
      settings_timer_sub: 'Otomatis menjeda musik saat waktu habis · Tidak aktif',
      settings_timer_off: 'Mati',
      settings_autoplay_title: 'Putar Otomatis Lagu Berikutnya',
      settings_autoplay_sub: 'Lanjutkan lagu berikutnya dalam antrean setelah lagu saat ini berakhir',
      settings_quality_title: 'Kualitas Streaming Online',
      settings_quality_sub: 'Pilih kompromi antara kualitas audio terbaik dan efisiensi kuota data',
      quality_high: 'Tinggi (256 - 320 kbps)',
      quality_std: 'Standar (160 kbps)',
      quality_low: 'Hemat Kuota (96 kbps)',
      settings_folders_title: 'Folder & Penyimpanan Musik',
      settings_folders_desc: 'Lokasi folder di komputermu yang dipindai untuk koleksi lagu lokal',
      btn_settings_add_folder: 'Tambah Folder',
      btn_settings_rescan: 'Pindai Ulang Koleksi',
      btn_settings_open_dl: 'Buka Folder Unduhan',
      settings_shortcuts_title: 'Pintasan Keyboard',
      settings_shortcuts_desc: 'Kendalikan musik lebih cepat tanpa menyentuh mouse',
      shortcut_play_pause: 'Putar / Jeda musik',
      shortcut_next: 'Lagu berikutnya',
      shortcut_prev: 'Lagu sebelumnya',
      shortcut_vol: 'Volume Naik / Turun',
      shortcut_mute: 'Bisukan suara (Mute / Unmute)',
      shortcut_fav: 'Tandai Favorit lagu saat ini',
      shortcut_repeat: 'Beralih mode repeat (Ulang)',
      settings_data_title: 'Data & Pembersihan',
      settings_data_desc: 'Kelola cache lokal dan riwayat pencarian aplikasi',
      settings_clear_searches_title: 'Riwayat Pencarian',
      settings_clear_searches_sub: 'Hapus semua riwayat kata kunci pencarian perpustakaan dan online',
      btn_settings_clear_history: 'Hapus Riwayat',
      settings_reset_title: 'Reset Pengaturan',
      settings_reset_sub: 'Kembalikan tema tampilan dan preferensi pemutar ke setelan awal pabrik',
      btn_settings_reset: 'Reset Setelan',
      about_app_desc: 'Pemutar musik modern berkonsep frosted glassmorphism & vinyl dial player. Mendukung pemutaran lagu lokal offline, streaming online, lirik lagu, dan pengunduhan langsung.',
      np_close: 'Tutup pemutar',
      change_photo: 'Ganti Foto',
      np_playing_eyebrow: 'SEDANG DIPUTAR',
      np_tab_queue: 'Antrean',
      np_tab_lyrics: 'Lirik',
      np_lrc_loading: 'Memuat lirik dari LRCLIB...',
      np_lrc_empty: 'Lirik belum tersedia untuk lagu ini',
      np_lrc_hint: 'Klik baris untuk melompat ke bagian itu.',
      np_lrc_follow: 'Ikuti lagu',
      np_queue_now: 'SEDANG DIPUTAR',
      np_save_queue_pl: 'Simpan antrean jadi playlist',
      np_clear_queue: 'Kosongkan antrean',
      dl_modal_title: 'Tools Download Musik',
      dl_modal_sub: 'Unduh file audio langsung dari URL',
      dl_modal_url_label: 'URL File Audio (MP3 / Direct Stream) *',
      dl_modal_title_label: 'Judul Lagu',
      dl_modal_artist_label: 'Nama Artis',
      btn_cancel: 'Batal',
      btn_start_dl: 'Mulai Unduh',
      pl_modal_title: 'Playlist Baru',
      pl_modal_sub: 'Kelompokkan lagu-lagu favoritmu',
      pl_modal_name_label: 'Nama Playlist',
      pl_modal_cover_label: 'Warna Sampul',
      btn_save_pl: 'Simpan Playlist',
      toast_lang_changed: 'Bahasa berhasil diubah ke Bahasa Indonesia'
    },
    en: {
      nav_space: 'YOUR MUSIC SPACE',
      nav_library: 'Library',
      nav_online: 'Explore online',
      nav_favorites: 'Favorites',
      nav_downloads: 'Downloads',
      nav_playlists: 'Playlists',
      nav_settings: 'Settings',
      btn_add_folder: 'Add folder',
      btn_dl_url: 'Download from URL',
      tagline: 'Your music. Your space.',
      lib_eyebrow: 'MUSIKKU · LIBRARY',
      lib_search_placeholder: 'Search songs, artists, albums...',
      recent_searches: 'Recent Searches',
      clear_all: 'Clear All',
      popular_suggestions: 'Popular Suggestions',
      now_playing_eyebrow: 'NOW PLAYING',
      now_playing_empty: 'Choose a song from library or explore',
      collection_title: 'Collection',
      play_all: 'Play all',
      quick_add_folder_title: 'Add folder',
      quick_add_folder_sub: 'Scan music from your computer',
      quick_dl_title: 'Download from URL',
      quick_dl_sub: 'Paste link, save offline',
      empty_lib_title: 'Empty Library',
      empty_lib_desc: 'Add music folders from your computer to start listening.',
      explore_eyebrow: 'STREAM MILLIONS OF SONGS ONLINE',
      explore_heading: 'Explore',
      explore_search_placeholder: 'Search music online (SZA, Daniel Caesar, pop, etc)...',
      source_youtube: 'YouTube Music',
      source_jamendo: 'Jamendo (Royalty Free)',
      tuner_eyebrow: 'TURN TUNER',
      active_channel: 'Active channel:',
      empty_search_title: 'Start Searching Music',
      empty_search_desc: 'Type song title, artist, or album above, or turn the genre tuner.',
      load_more: 'Load More Songs',
      fav_eyebrow: 'YOUR FAVORITES',
      fav_title: 'Favorites',
      fav_search_placeholder: 'Search in favorites...',
      empty_fav_eyebrow: 'EMPTY VINYL',
      empty_fav_title: 'No favorite<br />songs yet',
      empty_fav_desc: 'Click the heart icon on any song to add it to favorites.',
      btn_fav_explore: 'Explore online music',
      btn_fav_lib: 'Open library',
      dl_breadcrumb: 'Library / Downloads',
      dl_search_placeholder: 'Search in downloaded collection...',
      dl_tag: 'AVAILABLE OFFLINE',
      dl_hero_heading: 'Favorite music.<br>Without limits.',
      dl_hero_sub: 'All your downloaded songs in one place. Ready to listen anytime.',
      dl_card_title: 'Downloaded Songs',
      dl_open_folder: 'Open folder',
      sort_recent: 'Recent',
      sort_title: 'Title (A–Z)',
      sort_duration: 'Duration',
      sort_name: 'Name A–Z',
      dl_th_title: 'SONG TITLE',
      dl_th_source: 'SOURCE',
      dl_th_dur: 'DURATION',
      dl_empty_title: 'No downloaded songs yet',
      dl_empty_desc: 'Download songs from "Explore" tab or use the link button on sidebar to save offline.',
      dl_footer_note: 'Stored on your device. Enjoy music without internet connection.',
      pl_eyebrow: 'PLAYLISTS',
      pl_title: 'Playlists',
      pl_search_placeholder: 'Search playlists...',
      btn_new_pl: 'New playlist',
      btn_back_pl: 'All playlists',
      pl_detail_songs: 'Playlist contents',
      btn_add_to_pl_picker: 'Add song from library',
      settings_eyebrow: 'MUSIKKU · SETTINGS',
      settings_title: 'Settings',
      settings_badge: 'Preferences & System',
      settings_theme_title: 'Theme & Appearance',
      settings_theme_desc: 'Choose visual style most comfortable for your eyes',
      settings_lang_title: 'App Language',
      settings_lang_desc: 'Choose interface display language for MusikKu',
      settings_audio_title: 'Playback & Audio',
      settings_audio_desc: 'Manage sleep timer and music playback behavior',
      settings_timer_title: 'Sleep Timer',
      settings_timer_sub: 'Automatically pauses playback when time is up · Inactive',
      settings_timer_off: 'Off',
      settings_autoplay_title: 'Auto-Play Next Song',
      settings_autoplay_sub: 'Play the next song in queue when current song ends',
      settings_quality_title: 'Online Streaming Quality',
      settings_quality_sub: 'Choose compromise between highest audio quality and data efficiency',
      quality_high: 'High (256 - 320 kbps)',
      quality_std: 'Standard (160 kbps)',
      quality_low: 'Data Saver (96 kbps)',
      settings_folders_title: 'Music Folders & Storage',
      settings_folders_desc: 'Folders on your computer scanned for local music files',
      btn_settings_add_folder: 'Add Folder',
      btn_settings_rescan: 'Rescan Collection',
      btn_settings_open_dl: 'Open Downloads Folder',
      settings_shortcuts_title: 'Keyboard Shortcuts',
      settings_shortcuts_desc: 'Control music faster without touching the mouse',
      shortcut_play_pause: 'Play / Pause music',
      shortcut_next: 'Next song',
      shortcut_prev: 'Previous song',
      shortcut_vol: 'Volume Up / Down',
      shortcut_mute: 'Mute / Unmute sound',
      shortcut_fav: 'Toggle Favorite on current song',
      shortcut_repeat: 'Toggle repeat mode',
      settings_data_title: 'Data & Cleanup',
      settings_data_desc: 'Manage local cache and search history',
      settings_clear_searches_title: 'Search History',
      settings_clear_searches_sub: 'Clear all search query history for library and online',
      btn_settings_clear_history: 'Clear History',
      settings_reset_title: 'Reset Settings',
      settings_reset_sub: 'Restore appearance themes and player settings to defaults',
      btn_settings_reset: 'Reset Settings',
      about_app_desc: 'Modern music player featuring frosted glassmorphism & vinyl dial player. Supports offline playback, online streaming, synchronized lyrics, and direct downloading.',
      np_close: 'Close player',
      change_photo: 'Change Photo',
      np_playing_eyebrow: 'NOW PLAYING',
      np_tab_queue: 'Queue',
      np_tab_lyrics: 'Lyrics',
      np_lrc_loading: 'Loading lyrics from LRCLIB...',
      np_lrc_empty: 'Lyrics not available for this song',
      np_lrc_hint: 'Click a line to jump to that timestamp.',
      np_lrc_follow: 'Auto-scroll',
      np_queue_now: 'NOW PLAYING',
      np_save_queue_pl: 'Save queue as playlist',
      np_clear_queue: 'Clear queue',
      dl_modal_title: 'Music Download Tool',
      dl_modal_sub: 'Download audio file directly from URL',
      dl_modal_url_label: 'Audio File URL (MP3 / Direct Stream) *',
      dl_modal_title_label: 'Song Title',
      dl_modal_artist_label: 'Artist Name',
      btn_cancel: 'Cancel',
      btn_start_dl: 'Start Download',
      pl_modal_title: 'New Playlist',
      pl_modal_sub: 'Group your favorite tracks',
      pl_modal_name_label: 'Playlist Name',
      pl_modal_cover_label: 'Cover Color',
      btn_save_pl: 'Save Playlist',
      toast_lang_changed: 'Language changed to English'
    },
    zh: {
      nav_space: '你的音乐空间',
      nav_library: '音乐库',
      nav_online: '在线探索',
      nav_favorites: '收藏夹',
      nav_downloads: '下载',
      nav_playlists: '歌单',
      nav_settings: '设置',
      btn_add_folder: '添加文件夹',
      btn_dl_url: '从链接下载',
      tagline: '你的音乐，你的专属空间。',
      lib_eyebrow: 'MUSIKKU · 音乐库',
      lib_search_placeholder: '搜索歌曲、歌手、专辑...',
      recent_searches: '搜索历史',
      clear_all: '清除全部',
      popular_suggestions: '热门推荐',
      now_playing_eyebrow: '正在播放',
      now_playing_empty: '从音乐库或探索中选择歌曲',
      collection_title: '音乐收藏',
      play_all: '播放全部',
      quick_add_folder_title: '添加文件夹',
      quick_add_folder_sub: '扫描计算机中的本地音乐',
      quick_dl_title: '从链接下载',
      quick_dl_sub: '粘贴链接，离线保存',
      empty_lib_title: '音乐库为空',
      empty_lib_desc: '从计算机添加音乐文件夹以开始聆听。',
      explore_eyebrow: '在线畅听海量音乐',
      explore_heading: '探索',
      explore_search_placeholder: '在线搜索音乐 (SZA, 周杰伦, 流行等)...',
      source_youtube: 'YouTube Music',
      source_jamendo: 'Jamendo (免版税)',
      tuner_eyebrow: '流派旋钮',
      active_channel: '当前频道：',
      empty_search_title: '开始搜索音乐',
      empty_search_desc: '在上方输入歌曲名称或转动流派旋钮发现精彩歌曲。',
      load_more: '加载更多歌曲',
      fav_eyebrow: '你的收藏',
      fav_title: '收藏夹',
      fav_search_placeholder: '在收藏夹中搜索...',
      empty_fav_eyebrow: '唱片空空如也',
      empty_fav_title: '暂无收藏歌曲',
      empty_fav_desc: '点击任何歌曲上的心形图标即可加入收藏。',
      btn_fav_explore: '探索在线音乐',
      btn_fav_lib: '打开音乐库',
      dl_breadcrumb: '音乐库 / 下载',
      dl_search_placeholder: '在已下载歌曲中搜索...',
      dl_tag: '离线可用',
      dl_hero_heading: '心动音乐。<br>无网亦动听。',
      dl_hero_sub: '所有已下载歌曲尽在此处，随时随地静心聆听。',
      dl_card_title: '已下载歌曲',
      dl_open_folder: '打开文件夹',
      sort_recent: '最新',
      sort_title: '标题 (A–Z)',
      sort_duration: '时长',
      sort_name: '名称 A–Z',
      dl_th_title: '歌曲标题',
      dl_th_source: '来源',
      dl_th_dur: '时长',
      dl_empty_title: '暂无下载歌曲',
      dl_empty_desc: '从“探索”中下载歌曲或使用左侧栏链接保存离线音频。',
      dl_footer_note: '已保存在您的设备中，无需网络连接畅享音乐。',
      pl_eyebrow: '播放列表',
      pl_title: '歌单',
      pl_search_placeholder: '搜索歌单...',
      btn_new_pl: '新建歌单',
      btn_back_pl: '所有歌单',
      pl_detail_songs: '歌单内容',
      btn_add_to_pl_picker: '从音乐库添加歌曲',
      settings_eyebrow: 'MUSIKKU · 设置',
      settings_title: '设置',
      settings_badge: '偏好与系统',
      settings_theme_title: '主题与外观',
      settings_theme_desc: '选择最适合您双眼的视觉风格',
      settings_lang_title: '应用语言',
      settings_lang_desc: '选择 MusikKu 的界面显示语言',
      settings_audio_title: '播放与音频',
      settings_audio_desc: '控制睡眠定时器与音乐播放行为',
      settings_timer_title: '睡眠定时器',
      settings_timer_sub: '时间结束时自动暂停音乐 · 未启用',
      settings_timer_off: '关闭',
      settings_autoplay_title: '自动播放下一首',
      settings_autoplay_sub: '当前歌曲播放完毕后自动继续播放队列',
      settings_quality_title: '在线流媒体音质',
      settings_quality_sub: '在最佳音质与流量节省之间取得平衡',
      quality_high: '高音质 (256 - 320 kbps)',
      quality_std: '标准 (160 kbps)',
      quality_low: '省流量 (96 kbps)',
      settings_folders_title: '音乐文件夹与存储',
      settings_folders_desc: '用于扫描本地音乐文件的电脑文件夹路径',
      btn_settings_add_folder: '添加文件夹',
      btn_settings_rescan: '重新扫描曲库',
      btn_settings_open_dl: '打开下载文件夹',
      settings_shortcuts_title: '键盘快捷键',
      settings_shortcuts_desc: '无需鼠标快速操控音乐播放',
      shortcut_play_pause: '播放 / 暂停音乐',
      shortcut_next: '下一首歌曲',
      shortcut_prev: '上一首歌曲',
      shortcut_vol: '音量加 / 减',
      shortcut_mute: '静音 / 取消静音',
      shortcut_fav: '收藏当前歌曲',
      shortcut_repeat: '切换循环模式',
      settings_data_title: '数据与清理',
      settings_data_desc: '管理本地缓存和应用搜索历史',
      settings_clear_searches_title: '搜索历史',
      settings_clear_searches_sub: '清除本地与在线的所有搜索历史记录',
      btn_settings_clear_history: '清除历史',
      settings_reset_title: '重置设置',
      settings_reset_sub: '将外观主题和播放偏好恢复为出厂默认设置',
      btn_settings_reset: '重置设置',
      about_app_desc: '融合磨砂玻璃态与黑胶旋转表盘的现代音乐播放器。支持离线播放、在线流媒体、实时歌词与直接下载。',
      np_close: '关闭播放器',
      change_photo: '更换封面',
      np_playing_eyebrow: '正在播放',
      np_tab_queue: '播放队列',
      np_tab_lyrics: '歌词',
      np_lrc_loading: '正在从 LRCLIB 加载歌词...',
      np_lrc_empty: '此歌曲暂无歌词',
      np_lrc_hint: '点击歌词行可跳转至对应时间点。',
      np_lrc_follow: '随歌滚动',
      np_queue_now: '正在播放',
      np_save_queue_pl: '将队列保存为歌单',
      np_clear_queue: '清空播放队列',
      dl_modal_title: '音乐下载工具',
      dl_modal_sub: '直接从 URL 下载音频文件',
      dl_modal_url_label: '音频文件链接 (MP3 / 直链流) *',
      dl_modal_title_label: '歌曲标题',
      dl_modal_artist_label: '歌手名称',
      btn_cancel: '取消',
      btn_start_dl: '开始下载',
      pl_modal_title: '新建歌单',
      pl_modal_sub: '整理您喜爱的歌曲',
      pl_modal_name_label: '歌单名称',
      pl_modal_cover_label: '封面渐变色',
      btn_save_pl: '保存歌单',
      toast_lang_changed: '已切换语言至 中文'
    },
    pt: {
      nav_space: 'SEU ESPAÇO DE MÚSICA',
      nav_library: 'Biblioteca',
      nav_online: 'Explorar online',
      nav_favorites: 'Favoritos',
      nav_downloads: 'Downloads',
      nav_playlists: 'Playlists',
      nav_settings: 'Configurações',
      btn_add_folder: 'Adicionar pasta',
      btn_dl_url: 'Baixar por URL',
      tagline: 'Sua música. Seu espaço.',
      lib_eyebrow: 'MUSIKKU · BIBLIOTECA',
      lib_search_placeholder: 'Buscar músicas, artistas, álbuns...',
      recent_searches: 'Buscas Recentes',
      clear_all: 'Limpar Tudo',
      popular_suggestions: 'Sugestões Populares',
      now_playing_eyebrow: 'TOCANDO AGORA',
      now_playing_empty: 'Escolha uma música da biblioteca ou explore',
      collection_title: 'Coleção',
      play_all: 'Tocar tudo',
      quick_add_folder_title: 'Adicionar pasta',
      quick_add_folder_sub: 'Escanear músicas do seu computador',
      quick_dl_title: 'Baixar por URL',
      quick_dl_sub: 'Cole o link, salve offline',
      empty_lib_title: 'Biblioteca Vazia',
      empty_lib_desc: 'Adicione pastas de música do computador para começar a ouvir.',
      explore_eyebrow: 'TRANSMISSÃO DE MILHÕES DE MÚSICAS ONLINE',
      explore_heading: 'Explorar',
      explore_search_placeholder: 'Buscar músicas online (SZA, Daniel Caesar, pop, etc)...',
      source_youtube: 'YouTube Music',
      source_jamendo: 'Jamendo (Sem Royalties)',
      tuner_eyebrow: 'SELETOR DE GÊNEROS',
      active_channel: 'Canal ativo:',
      empty_search_title: 'Comece a Buscar Músicas',
      empty_search_desc: 'Digite título, artista ou gire o seletor para encontrar músicas incríveis.',
      load_more: 'Carregar Mais Músicas',
      fav_eyebrow: 'SEUS FAVORITOS',
      fav_title: 'Favoritos',
      fav_search_placeholder: 'Buscar nos favoritos...',
      empty_fav_eyebrow: 'DISCO VAZIO',
      empty_fav_title: 'Nenhuma música<br />favorita ainda',
      empty_fav_desc: 'Clique no ícone de coração em qualquer música para adicioná-la aos favoritos.',
      btn_fav_explore: 'Explorar música online',
      btn_fav_lib: 'Abrir biblioteca',
      dl_breadcrumb: 'Biblioteca / Downloads',
      dl_search_placeholder: 'Buscar nos downloads...',
      dl_tag: 'DISPONÍVEL OFFLINE',
      dl_hero_heading: 'Música favorita.<br>Sem limites de sinal.',
      dl_hero_sub: 'Todas as suas músicas baixadas em um só lugar. Prontas para ouvir a qualquer hora.',
      dl_card_title: 'Músicas baixadas',
      dl_open_folder: 'Abrir pasta',
      sort_recent: 'Mais recentes',
      sort_title: 'Título (A–Z)',
      sort_duration: 'Duração',
      sort_name: 'Nome A–Z',
      dl_th_title: 'TÍTULO DA MÚSICA',
      dl_th_source: 'FONTE',
      dl_th_dur: 'DURAÇÃO',
      dl_empty_title: 'Nenhuma música baixada',
      dl_empty_desc: 'Baixe músicas na aba "Explorar" ou use o botão de link para salvar offline.',
      dl_footer_note: 'Salvo no seu dispositivo. Curta música sem conexão com a internet.',
      pl_eyebrow: 'LISTAS DE REPRODUÇÃO',
      pl_title: 'Playlists',
      pl_search_placeholder: 'Buscar playlists...',
      btn_new_pl: 'Nova playlist',
      btn_back_pl: 'Todas as playlists',
      pl_detail_songs: 'Conteúdo da playlist',
      btn_add_to_pl_picker: 'Adicionar música da biblioteca',
      settings_eyebrow: 'MUSIKKU · CONFIGURAÇÕES',
      settings_title: 'Configurações',
      settings_badge: 'Preferências & Sistema',
      settings_theme_title: 'Tema & Visual',
      settings_theme_desc: 'Escolha o estilo visual mais confortável para seus olhos',
      settings_lang_title: 'Idioma do Aplicativo',
      settings_lang_desc: 'Escolha o idioma de exibição do MusikKu',
      settings_audio_title: 'Reprodução & Áudio',
      settings_audio_desc: 'Controle o timer de suspensão e fluxo de reprodução',
      settings_timer_title: 'Timer de Suspensão (Sleep Timer)',
      settings_timer_sub: 'Pausa a música automaticamente quando o tempo acaba · Inativo',
      settings_timer_off: 'Desativado',
      settings_autoplay_title: 'Tocar Próxima Automaticamente',
      settings_autoplay_sub: 'Continuar com a próxima música da fila quando a atual terminar',
      settings_quality_title: 'Qualidade de Streaming Online',
      settings_quality_sub: 'Escolha entre melhor fidelidade sonora e economia de dados',
      quality_high: 'Alta (256 - 320 kbps)',
      quality_std: 'Padrão (160 kbps)',
      quality_low: 'Econômica (96 kbps)',
      settings_folders_title: 'Pastas & Armazenamento',
      settings_folders_desc: 'Pastas no seu computador verificadas para arquivos de áudio locais',
      btn_settings_add_folder: 'Adicionar Pasta',
      btn_settings_rescan: 'Reescanear Coleção',
      btn_settings_open_dl: 'Abrir Pasta de Downloads',
      settings_shortcuts_title: 'Atalhos de Teclado',
      settings_shortcuts_desc: 'Controle a música rapidamente sem usar o mouse',
      shortcut_play_pause: 'Reproduzir / Pausar',
      shortcut_next: 'Próxima música',
      shortcut_prev: 'Música anterior',
      shortcut_vol: 'Aumentar / Diminuir Volume',
      shortcut_mute: 'Silenciar som (Mudo)',
      shortcut_fav: 'Favoritar música atual',
      shortcut_repeat: 'Alternar modo de repetição',
      settings_data_title: 'Dados & Limpeza',
      settings_data_desc: 'Gerencie o cache local e histórico de busca',
      settings_clear_searches_title: 'Histórico de Busca',
      settings_clear_searches_sub: 'Limpar histórico de buscas locais e online',
      btn_settings_clear_history: 'Limpar Histórico',
      settings_reset_title: 'Redefinir Configurações',
      settings_reset_sub: 'Restaurar temas e preferências aos padrões de fábrica',
      btn_settings_reset: 'Redefinir Ajustes',
      about_app_desc: 'Player de música moderno com estética frosted glassmorphism & dial de vinil. Suporta reprodução offline, streaming online, letras sincronizadas e download direto.',
      np_close: 'Fechar player',
      change_photo: 'Trocar Foto',
      np_playing_eyebrow: 'TOCANDO AGORA',
      np_tab_queue: 'Fila',
      np_tab_lyrics: 'Letra',
      np_lrc_loading: 'Carregando letra do LRCLIB...',
      np_lrc_empty: 'Letra indisponível para esta música',
      np_lrc_hint: 'Clique em um verso para avançar a reprodução.',
      np_lrc_follow: 'Rolar automático',
      np_queue_now: 'TOCANDO AGORA',
      np_save_queue_pl: 'Salvar fila como playlist',
      np_clear_queue: 'Esvaziar fila',
      dl_modal_title: 'Ferramenta de Download',
      dl_modal_sub: 'Baixar arquivo de áudio direto de um link',
      dl_modal_url_label: 'URL do Áudio (MP3 / Stream Direto) *',
      dl_modal_title_label: 'Título da Música',
      dl_modal_artist_label: 'Nome do Artista',
      btn_cancel: 'Cancelar',
      btn_start_dl: 'Iniciar Download',
      pl_modal_title: 'Nova Playlist',
      pl_modal_sub: 'Agrupe suas músicas favoritas',
      pl_modal_name_label: 'Nome da Playlist',
      pl_modal_cover_label: 'Cor da Capa',
      btn_save_pl: 'Salvar Playlist',
      toast_lang_changed: 'Idioma alterado para Português'
    },
    ru: {
      nav_space: 'ВАШЕ МУЗЫКАЛЬНОЕ ПРОСТРАНСТВО',
      nav_library: 'Библиотека',
      nav_online: 'Онлайн-поиск',
      nav_favorites: 'Избранное',
      nav_downloads: 'Загрузки',
      nav_playlists: 'Плейлисты',
      nav_settings: 'Настройки',
      btn_add_folder: 'Добавить папку',
      btn_dl_url: 'Скачать по ссылке',
      tagline: 'Твоя музыка. Твое пространство.',
      lib_eyebrow: 'MUSIKKU · БИБЛИОТЕКА',
      lib_search_placeholder: 'Поиск треков, артистов, альбомов...',
      recent_searches: 'История поиска',
      clear_all: 'Очистить всё',
      popular_suggestions: 'Популярные запросы',
      now_playing_eyebrow: 'СЕЙЧАС ИГРАЕТ',
      now_playing_empty: 'Выберите трек из библиотеки или поиска',
      collection_title: 'Коллекция',
      play_all: 'Воспроизвести всё',
      quick_add_folder_title: 'Добавить папку',
      quick_add_folder_sub: 'Сканировать музыку с вашего компьютера',
      quick_dl_title: 'Скачать по ссылке',
      quick_dl_sub: 'Вставьте ссылку и сохраните офлайн',
      empty_lib_title: 'Библиотека пуста',
      empty_lib_desc: 'Добавьте папки с музыкой на компьютере, чтобы начать прослушивание.',
      explore_eyebrow: 'МИЛЛИОНЫ ТРЕКОВ ОНЛАЙН',
      explore_heading: 'Обзор',
      explore_search_placeholder: 'Поиск музыки онлайн (SZA, Daniel Caesar, рок, поп)...',
      source_youtube: 'YouTube Music',
      source_jamendo: 'Jamendo (Без лицензий)',
      tuner_eyebrow: 'ПЕРЕКЛЮЧАТЕЛЬ ЖАНРОВ',
      active_channel: 'Активный канал:',
      empty_search_title: 'Начните поиск музыки',
      empty_search_desc: 'Введите название трека или артиста сверху, либо поверните переключатель жанров.',
      load_more: 'Загрузить больше треков',
      fav_eyebrow: 'ВАШЕ ИЗБРАННОЕ',
      fav_title: 'Избранное',
      fav_search_placeholder: 'Поиск в избранном...',
      empty_fav_eyebrow: 'ПУСТОЙ ВИНИЛ',
      empty_fav_title: 'В избранном пока<br />нет треков',
      empty_fav_desc: 'Нажмите на иконку сердечка у любого трека, чтобы добавить его в избранное.',
      btn_fav_explore: 'Искать музыку онлайн',
      btn_fav_lib: 'Открыть библиотеку',
      dl_breadcrumb: 'Библиотека / Загрузки',
      dl_search_placeholder: 'Поиск среди загруженных треков...',
      dl_tag: 'ДОСТУПНО ОФЛАЙН',
      dl_hero_heading: 'Любимая музыка.<br>Вне зависимости от сети.',
      dl_hero_sub: 'Все скачанные треки в одном месте. Слушайте где и когда угодно.',
      dl_card_title: 'Загруженные треки',
      dl_open_folder: 'Открыть папку',
      sort_recent: 'Сначала новые',
      sort_title: 'Название (А–Я)',
      sort_duration: 'Длительность',
      sort_name: 'Название А–Я',
      dl_th_title: 'НАЗВАНИЕ ТРЕКА',
      dl_th_source: 'ИСТОЧНИК',
      dl_th_dur: 'ДЛИТЕЛЬНОСТЬ',
      dl_empty_title: 'Нет загруженных треков',
      dl_empty_desc: 'Скачивайте треки из вкладки «Обзор» или используйте ссылку в боковом меню.',
      dl_footer_note: 'Сохранено на вашем устройстве. Наслаждайтесь музыкой без интернета.',
      pl_eyebrow: 'СПИСКИ ВОСПРОИЗВЕДЕНИЯ',
      pl_title: 'Плейлисты',
      pl_search_placeholder: 'Поиск плейлистов...',
      btn_new_pl: 'Новый плейлист',
      btn_back_pl: 'Все плейлисты',
      pl_detail_songs: 'Треки в плейлисте',
      btn_add_to_pl_picker: 'Добавить трек из библиотеки',
      settings_eyebrow: 'MUSIKKU · НАСТРОЙКИ',
      settings_title: 'Настройки',
      settings_badge: 'Предпочтения и система',
      settings_theme_title: 'Тема и оформление',
      settings_theme_desc: 'Выберите комфортный визуальный стиль для ваших глаз',
      settings_lang_title: 'Язык приложения',
      settings_lang_desc: 'Выберите язык интерфейса MusikKu',
      settings_audio_title: 'Воспроизведение и звук',
      settings_audio_desc: 'Таймер сна и параметры непрерывного воспроизведения',
      settings_timer_title: 'Таймер сна (Sleep Timer)',
      settings_timer_sub: 'Автоматически ставит музыку на паузу по таймеру · Отключен',
      settings_timer_off: 'Выкл',
      settings_autoplay_title: 'Автовоспроизведение следующего трека',
      settings_autoplay_sub: 'Продолжать воспроизведение очереди после завершения текущего трека',
      settings_quality_title: 'Качество онлайн-потока',
      settings_quality_sub: 'Баланс между наилучшим звучанием и экономией трафика',
      quality_high: 'Высокое (256 - 320 кбит/с)',
      quality_std: 'Стандартное (160 кбит/с)',
      quality_low: 'Экономия трафика (96 кбит/с)',
      settings_folders_title: 'Папки и хранилище музыки',
      settings_folders_desc: 'Папки на компьютере, сканируемые для поиска локальной музыки',
      btn_settings_add_folder: 'Добавить папку',
      btn_settings_rescan: 'Пересканировать коллекцию',
      btn_settings_open_dl: 'Открыть папку загрузок',
      settings_shortcuts_title: 'Горячие клавиши',
      settings_shortcuts_desc: 'Управляйте музыкой мгновенно без мыши',
      shortcut_play_pause: 'Воспроизведение / Пауза',
      shortcut_next: 'Следующий трек',
      shortcut_prev: 'Предыдущий трек',
      shortcut_vol: 'Громкость Выше / Ниже',
      shortcut_mute: 'Отключить / Включить звук',
      shortcut_fav: 'Добавить/Убрать из избранного',
      shortcut_repeat: 'Переключить режим повтора',
      settings_data_title: 'Данные и очистка',
      settings_data_desc: 'Управление локальным кэшем и историей поиска',
      settings_clear_searches_title: 'История поиска',
      settings_clear_searches_sub: 'Очистить историю запросов в библиотеке и онлайн',
      btn_settings_clear_history: 'Очистить историю',
      settings_reset_title: 'Сброс настроек',
      settings_reset_sub: 'Сбросить тему оформления и параметры плеера до заводских',
      btn_settings_reset: 'Сбросить настройки',
      about_app_desc: 'Современный плеер в стиле frosted glassmorphism с вращающимся виниловым диском. Поддерживает офлайн треки, онлайн стриминг, синхронизированные тексты и скачивание.',
      np_close: 'Закрыть плеер',
      change_photo: 'Сменить фото',
      np_playing_eyebrow: 'СЕЙЧАС ИГРАЕТ',
      np_tab_queue: 'Очередь',
      np_tab_lyrics: 'Текст песни',
      np_lrc_loading: 'Загрузка текста из LRCLIB...',
      np_lrc_empty: 'Для этого трека текст пока недоступен',
      np_lrc_hint: 'Нажмите на строку текста, чтобы перейти к этому моменту.',
      np_lrc_follow: 'Следить за треком',
      np_queue_now: 'СЕЙЧАС ИГРАЕТ',
      np_save_queue_pl: 'Сохранить очередь в плейлист',
      np_clear_queue: 'Очистить очередь',
      dl_modal_title: 'Инструмент загрузки',
      dl_modal_sub: 'Скачать аудиофайл напрямую по ссылке',
      dl_modal_url_label: 'Ссылка на аудио (MP3 / Direct Stream) *',
      dl_modal_title_label: 'Название трека',
      dl_modal_artist_label: 'Имя исполнителя',
      btn_cancel: 'Отмена',
      btn_start_dl: 'Начать загрузку',
      pl_modal_title: 'Новый плейлист',
      pl_modal_sub: 'Сгруппируйте любимые композиции',
      pl_modal_name_label: 'Название плейлиста',
      pl_modal_cover_label: 'Цвет обложки',
      btn_save_pl: 'Сохранить плейлист',
      toast_lang_changed: 'Язык переключен на Русский'
    }
  };

  function applyLanguage(lang) {
    state.language = lang || 'id';
    localStorage.setItem('musikku_desktop_language', state.language);
    const dict = TRANSLATIONS[state.language] || TRANSLATIONS.id;

    // Update all text elements with data-i18n
    document.querySelectorAll('[data-i18n]').forEach(el => {
      const key = el.dataset.i18n;
      if (dict[key] !== undefined) {
        if (dict[key].includes('<')) {
          el.innerHTML = dict[key];
        } else {
          el.textContent = dict[key];
        }
      }
    });

    // Update all placeholder elements with data-i18n-placeholder
    document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
      const key = el.dataset.i18nPlaceholder;
      if (dict[key] !== undefined) {
        el.placeholder = dict[key];
      }
    });

    // Update all title elements with data-i18n-title
    document.querySelectorAll('[data-i18n-title]').forEach(el => {
      const key = el.dataset.i18nTitle;
      if (dict[key] !== undefined) {
        el.title = dict[key];
      }
    });

    updateLanguageCardsUI();
  }

  function updateLanguageCardsUI() {
    const cards = document.querySelectorAll('.lang-option-card');
    cards.forEach(card => {
      const l = card.dataset.lang;
      card.classList.toggle('active', l === state.language);
    });
  }

  function startSleepTimer(minutes) {
    if (state.sleepTimerInterval) {
      clearInterval(state.sleepTimerInterval);
      state.sleepTimerInterval = null;
    }

    state.sleepTimerMinutes = minutes;
    if (minutes <= 0) {
      state.sleepTimerRemaining = 0;
      updateSleepTimerUI();
      showDownloadToast('Sleep Timer dinonaktifkan', 0);
      hideDownloadToast(1500);
      return;
    }

    state.sleepTimerRemaining = minutes * 60;
    updateSleepTimerUI();
    showDownloadToast(`Sleep Timer aktif: ${minutes} menit`, 100);
    hideDownloadToast(2000);

    state.sleepTimerInterval = setInterval(() => {
      state.sleepTimerRemaining -= 1;
      updateSleepTimerUI();

      if (state.sleepTimerRemaining <= 0) {
        clearInterval(state.sleepTimerInterval);
        state.sleepTimerInterval = null;
        state.sleepTimerMinutes = 0;
        if (state.isPlaying) {
          audio.pause();
          state.isPlaying = false;
          updatePlayerUI();
          renderCurrentScreenTracks();
        }
        showDownloadToast('Waktu tidur habis: Pemutaran musik dijeda', 0);
        hideDownloadToast(3500);
        updateSleepTimerUI();
      }
    }, 1000);
  }

  function updateSleepTimerUI() {
    const pills = document.querySelectorAll('.btn-timer-pill');
    pills.forEach(p => {
      const m = parseInt(p.dataset.mins || '0', 10);
      p.classList.toggle('active', m === state.sleepTimerMinutes);
    });

    const statusEl = document.getElementById('sleep-timer-status-text');
    if (statusEl) {
      if (state.sleepTimerRemaining > 0) {
        const mins = Math.floor(state.sleepTimerRemaining / 60);
        const secs = state.sleepTimerRemaining % 60;
        const formatted = `${mins}:${secs < 10 ? '0' : ''}${secs}`;
        statusEl.textContent = `Musik akan dijeda otomatis dalam ${formatted}`;
        statusEl.style.color = 'var(--accent)';
      } else {
        statusEl.textContent = 'Otomatis menjeda musik saat waktu habis · Tidak aktif';
        statusEl.style.color = '';
      }
    }
  }

  function renderSettingsFolders() {
    const container = document.getElementById('settings-folder-list');
    if (!container) return;
    container.innerHTML = '';

    if (!state.scannedFolders || state.scannedFolders.length === 0) {
      container.innerHTML = `
        <div class="settings-folder-empty">
          <span class="material-icons-round">folder_off</span>
          <span>Belum ada folder kustom yang ditambahkan</span>
        </div>
      `;
      return;
    }

    state.scannedFolders.forEach((folderPath, index) => {
      const row = document.createElement('div');
      row.className = 'settings-folder-row';
      row.innerHTML = `
        <div class="settings-folder-info">
          <span class="material-icons-round folder-icon">folder</span>
          <span class="folder-path" title="${folderPath}">${folderPath}</span>
        </div>
        <button type="button" class="btn-remove-folder" data-index="${index}" title="Hapus folder dari daftar perpustakaan">
          <span class="material-icons-round">delete_outline</span>
        </button>
      `;

      row.querySelector('.btn-remove-folder')?.addEventListener('click', async () => {
        const confirmed = await showConfirmModal({
          title: 'Hapus Folder?',
          subtitle: 'Folder akan dikeluarkan dari daftar perpustakaan',
          message: `Folder:<br><code style="font-size:12px;word-break:break-all;">${folderPath}</code><br><br>File musik asli di komputermu <strong>tetap aman dan tidak akan terhapus</strong>.`,
          confirmText: 'Hapus Folder',
          cancelText: 'Batal',
          icon: 'folder_delete',
          isDanger: true
        });

        if (confirmed) {
          state.scannedFolders.splice(index, 1);
          saveFolders();
          renderSettingsFolders();
          // Rescan sisa folder
          state.allSongs = [];
          for (const f of state.scannedFolders) {
            const songs = await scanFolder(f);
            mergeSongs(songs);
          }
          applyFilterAndRenderLibrary();
          showDownloadToast('Folder berhasil dihapus dari perpustakaan', 0);
          hideDownloadToast(2000);
        }
      });

      container.appendChild(row);
    });
  }

  async function rescanAllFolders() {
    showDownloadToast('Memindai ulang seluruh folder musik...', 40);
    state.allSongs = [];
    if (state.scannedFolders.length > 0) {
      for (const folder of state.scannedFolders) {
        const songs = await scanFolder(folder);
        mergeSongs(songs);
      }
    }
    try {
      const dlFolder = await window.elektronAPI?.getDownloadsFolder();
      if (dlFolder) {
        const dlFiles = await scanFolder(dlFolder);
        if (dlFiles.length > 0) {
          state.downloadedSongs = dlFiles.map(f => ({ ...f, isDownloaded: true }));
          saveDownloads();
          mergeSongs(dlFiles);
        }
      }
    } catch (e) {
      console.warn('Gagal memindai folder unduhan:', e);
    }

    applyFilterAndRenderLibrary();
    renderSettingsFolders();
    showDownloadToast(`Pemindaian selesai: ${state.allSongs.length} lagu ditemukan`, 100);
    hideDownloadToast(2500);
  }

  function renderSettingsView() {
    updateThemeCardsUI();
    updateLanguageCardsUI();
    updateSleepTimerUI();
    renderSettingsFolders();

    const autoplayToggle = document.getElementById('setting-autoplay');
    if (autoplayToggle) {
      autoplayToggle.checked = state.autoPlayNext !== false;
    }

    const qualitySelect = document.getElementById('setting-audio-quality');
    if (qualitySelect) {
      qualitySelect.value = state.audioQuality || 'standard';
    }
  }

  function setupSettingsEventListeners() {
    // Theme cards
    const themeCards = document.querySelectorAll('.theme-option-card');
    themeCards.forEach(card => {
      card.addEventListener('click', () => {
        const theme = card.dataset.theme;
        if (theme) {
          applyTheme(theme);
          const themeNames = {
            light: 'Light Sky Blue',
            emerald: 'Dark Emerald',
            dark: 'Midnight Obsidian'
          };
          showDownloadToast(`Tema diubah ke ${themeNames[theme] || theme}`, 100);
          hideDownloadToast(1800);
        }
      });
    });

    // Language cards
    const langCards = document.querySelectorAll('.lang-option-card');
    langCards.forEach(card => {
      card.addEventListener('click', () => {
        const lang = card.dataset.lang;
        if (lang) {
          applyLanguage(lang);
          const langNames = {
            id: 'Bahasa Indonesia',
            en: 'English',
            zh: 'Mandarin (中文)',
            pt: 'Português',
            ru: 'Русский'
          };
          const msg = TRANSLATIONS[lang]?.toast_lang_changed || `Bahasa diubah ke ${langNames[lang] || lang}`;
          showDownloadToast(msg, 100);
          hideDownloadToast(1800);
        }
      });
    });

    // Sleep timer pills
    const timerPills = document.querySelectorAll('.btn-timer-pill');
    timerPills.forEach(pill => {
      pill.addEventListener('click', () => {
        const mins = parseInt(pill.dataset.mins || '0', 10);
        startSleepTimer(mins);
      });
    });

    // Autoplay toggle
    const autoplayToggle = document.getElementById('setting-autoplay');
    autoplayToggle?.addEventListener('change', (e) => {
      state.autoPlayNext = e.target.checked;
      localStorage.setItem('musikku_autoplay', e.target.checked ? 'true' : 'false');
      showDownloadToast(state.autoPlayNext ? 'Putar otomatis berikutnya aktif' : 'Putar otomatis dinonaktifkan', 100);
      hideDownloadToast(1800);
    });

    // Audio quality select
    const qualitySelect = document.getElementById('setting-audio-quality');
    qualitySelect?.addEventListener('change', (e) => {
      state.audioQuality = e.target.value;
      localStorage.setItem('musikku_audio_quality', e.target.value);
      showDownloadToast(`Kualitas streaming disetel: ${e.target.options[e.target.selectedIndex].text}`, 100);
      hideDownloadToast(2000);
    });

    // Add folder button in settings
    document.getElementById('btn-settings-add-folder')?.addEventListener('click', async () => {
      await addFolder();
      renderSettingsFolders();
    });

    // Rescan collection
    document.getElementById('btn-settings-rescan')?.addEventListener('click', rescanAllFolders);

    // Open downloads folder
    document.getElementById('btn-settings-open-downloads')?.addEventListener('click', () => {
      window.elektronAPI?.openDownloadsFolder();
    });

    // Clear search history
    document.getElementById('btn-settings-clear-searches')?.addEventListener('click', async () => {
      const confirmed = await showConfirmModal({
        title: 'Hapus Riwayat Pencarian?',
        subtitle: 'Seluruh saran pencarian akan dibersihkan',
        message: 'Apakah kamu yakin ingin menghapus seluruh riwayat kata kunci pencarian di perpustakaan dan online?',
        confirmText: 'Hapus Riwayat',
        cancelText: 'Batal',
        icon: 'delete_sweep',
        isDanger: true
      });

      if (confirmed) {
        clearRecentSearches('lib');
        clearRecentSearches('online');
        renderRecentSearchPopover('lib');
        renderRecentSearchPopover('online');
        showDownloadToast('Riwayat pencarian berhasil dibersihkan', 100);
        hideDownloadToast(2000);
      }
    });

    // Reset settings
    document.getElementById('btn-settings-reset-all')?.addEventListener('click', async () => {
      const confirmed = await showConfirmModal({
        title: 'Reset Semua Pengaturan?',
        subtitle: 'Kembalikan preferensi ke setelan pabrik',
        message: 'Tema tampilan, sleep timer, putar otomatis, dan kualitas streaming akan dikembalikan ke setelan awal. Lagu dan playlist Anda tetap aman.',
        confirmText: 'Reset Pengaturan',
        cancelText: 'Batal',
        icon: 'restart_alt',
        isDanger: true
      });

      if (confirmed) {
        applyTheme('light');
        applyLanguage('id');
        state.autoPlayNext = true;
        localStorage.setItem('musikku_autoplay', 'true');
        state.audioQuality = 'standard';
        localStorage.setItem('musikku_audio_quality', 'standard');
        startSleepTimer(0);
        renderSettingsView();
        showDownloadToast('Pengaturan telah direset ke setelan awal', 100);
        hideDownloadToast(2500);
      }
    });
  }

  // ─── App Initialization ────────────────────────────────────────────────────
  async function init() {
    applyTheme(state.theme);
    applyLanguage(state.language);
    setupSettingsEventListeners();
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
