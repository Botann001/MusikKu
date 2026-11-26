const { app, BrowserWindow, ipcMain, dialog, globalShortcut, nativeTheme, shell } = require('electron');
const path = require('path');
const fs = require('fs');
const https = require('https');
const http = require('http');
const { execFile, spawn } = require('child_process');

let mainWindow;

// ─── Resolve yt-dlp & Python PATH on Windows ────────────────────────────────
const localAppData = process.env.LOCALAPPDATA || '';
const exeName = process.platform === 'win32' ? 'yt-dlp.exe' : 'yt-dlp';

const searchDirs = [
  // 1. Packaged Electron app (unpacked ASAR resources)
  process.resourcesPath ? path.join(process.resourcesPath, 'app.asar.unpacked', 'bin') : null,
  process.resourcesPath ? path.join(process.resourcesPath, 'bin') : null,
  path.join(__dirname.replace(/app\.asar$/, 'app.asar.unpacked'), 'bin'),
  // 2. Development or relative bin folders
  path.join(__dirname, 'bin'),
  path.join(__dirname, '..', 'bin'),
  path.join(process.cwd(), 'bin'),
  path.join(process.cwd(), 'desktop', 'bin'),
  // 3. User Python Scripts paths
  path.join(localAppData, 'Programs', 'Python', 'Python314', 'Scripts'),
  path.join(localAppData, 'Programs', 'Python', 'Python314'),
  path.join(localAppData, 'Programs', 'Python', 'Python313', 'Scripts'),
  path.join(localAppData, 'Programs', 'Python', 'Python313'),
  path.join(localAppData, 'Programs', 'Python', 'Python312', 'Scripts'),
  path.join(localAppData, 'Programs', 'Python', 'Python312'),
  path.join(process.env.APPDATA || '', 'Python', 'Scripts')
].filter(Boolean);

for (const dir of searchDirs) {
  if (fs.existsSync(dir) && !(process.env.PATH || '').includes(dir)) {
    process.env.PATH = `${dir};${process.env.PATH || ''}`;
  }
}

function getYtDlpCommand() {
  for (const dir of searchDirs) {
    const candidate = path.join(dir, exeName);
    if (fs.existsSync(candidate)) return candidate;
  }
  return 'yt-dlp';
}

function getYtDlpFormatSelector(quality = 'standard', preferM4a = false) {
  if (quality === 'high') {
    return preferM4a ? 'ba[ext=m4a]/ba/b' : 'ba[abr>=192]/ba/b';
  }
  if (quality === 'saver') {
    return preferM4a ? 'ba[abr<=96][ext=m4a]/ba[abr<=96]/wa/ba/b' : 'ba[abr<=96]/wa/ba/b';
  }
  return preferM4a ? 'ba[abr<=160][ext=m4a]/ba[ext=m4a]/ba/b' : 'ba[abr<=160]/ba/b';
}

function resolveDirectAudioUrl(url, audioQuality = 'standard') {
  return new Promise((resolve) => {
    const ytdlpBin = getYtDlpCommand();
    const args = [
      '--extractor-args', 'youtube:player_client=web,mweb,android',
      '-f', getYtDlpFormatSelector(audioQuality, false),
      '-g',
      url
    ];
    execFile(ytdlpBin, args, { timeout: 10000 }, (err, stdout) => {
      if (err || !stdout) {
        return resolve(null);
      }
      const lines = stdout.trim().split(/\r?\n/).map(l => l.trim()).filter(l => l.startsWith('http'));
      if (lines.length > 0) {
        return resolve(lines[0]);
      }
      resolve(null);
    });
  });
}

// ─── Local Audio Streaming Proxy Server ─────────────────────────────────────
let streamServer;
let streamServerPort = 0;

function startStreamServer() {
  if (streamServer) return;
  streamServer = http.createServer((req, res) => {
    try {
      const parsedUrl = new URL(req.url, `http://127.0.0.1:${streamServerPort}`);
      if (parsedUrl.pathname === '/stream') {
        const audioUrl = parsedUrl.searchParams.get('url');
        const fallbackQuery = parsedUrl.searchParams.get('q') || '';
        if (!audioUrl) {
          res.writeHead(400);
          return res.end('Missing url');
        }

        let headersSent = false;
        let dataSent = false;
        let activeChild = null;
        let isEnded = false;
        let fallbackStarted = false;

        function endResponse(status) {
          if (isEnded) return;
          isEnded = true;
          if (!headersSent && status) {
            try { res.writeHead(status); } catch (e) {}
          }
          try { res.end(); } catch (e) {}
        }

        async function streamViaHttpProxy() {
          if (isEnded) return;
          let vId = parsedUrl.searchParams.get('id');
          if (!vId && audioUrl) {
            const match = audioUrl.match(/(?:youtu\.be\/|v\/|watch\?v=|\&v=)([^#\&\?]{11})/i);
            if (match) vId = match[1];
          }

          if (vId) {
            console.log(`[StreamServer] Mencoba stream direct via Invidious Web API untuk video: ${vId}`);
            const directUrl = await resolveInvidiousAudioUrl(vId);
            if (directUrl && !isEnded) {
              try {
                const u = new URL(directUrl);
                const client = u.protocol === 'http:' ? http : https;
                const proxyReq = client.get(directUrl, {
                  headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
                    'Accept': '*/*'
                  }
                }, (proxyRes) => {
                  if (proxyRes.statusCode >= 300 && proxyRes.statusCode < 400 && proxyRes.headers.location) {
                    const redUrl = new URL(proxyRes.headers.location, directUrl).href;
                    const redU = new URL(redUrl);
                    const redClient = redU.protocol === 'http:' ? http : https;
                    redClient.get(redUrl, (rRes) => {
                      if (!headersSent && !isEnded) {
                        res.writeHead(200, {
                          'Content-Type': rRes.headers['content-type'] || 'audio/mp4',
                          'Accept-Ranges': 'bytes',
                          'Access-Control-Allow-Origin': '*'
                        });
                        headersSent = true;
                      }
                      rRes.pipe(res);
                    }).on('error', () => endResponse(500));
                    return;
                  }
                  if (!headersSent && !isEnded) {
                    res.writeHead(200, {
                      'Content-Type': proxyRes.headers['content-type'] || 'audio/mp4',
                      'Accept-Ranges': 'bytes',
                      'Access-Control-Allow-Origin': '*'
                    });
                    headersSent = true;
                  }
                  proxyRes.pipe(res);
                });
                proxyReq.on('error', () => endResponse(500));
                return true;
              } catch (_) {
                endResponse(500);
                return false;
              }
            }
          }
          endResponse(500);
          return false;
        }

        function run(target, isFallback) {
          if (isEnded) return;
          const ytdlpBin = getYtDlpCommand();
          const args = [
            '--extractor-args', 'youtube:player_client=web,mweb,android',
            '-f', 'ba/b',
            '-o', '-',
            target
          ];

          let child = null;
          try {
            child = spawn(ytdlpBin, args);
            activeChild = child;
          } catch (_) {
            return streamViaHttpProxy();
          }

          function tryFallback() {
            if (fallbackStarted || dataSent || isFallback || isEnded) return false;
            const q = fallbackQuery || (target.startsWith('http') ? 'Trending Musik Indonesia' : target);
            fallbackStarted = true;
            try { child?.kill(); } catch (e) {}
            console.log(`[StreamServer] YouTube failed. Auto-fallback to: scsearch1:${q}`);
            run(`scsearch1:${q}`, true);
            return true;
          }

          // Safety timeout: if YouTube stalls or hangs without sending data, auto fallback after 12 seconds
          const fallbackTimeout = setTimeout(() => {
            if (!dataSent && !isEnded && !isFallback) {
              console.log('[StreamServer] Handshake timeout (12s), auto switching to fallback stream...');
              if (!tryFallback()) streamViaHttpProxy();
            }
          }, 12000);

          child.stdout.on('data', (chunk) => {
            if (isEnded) return;
            clearTimeout(fallbackTimeout);
            if (!headersSent) {
              res.writeHead(200, {
                'Content-Type': 'audio/mpeg',
                'Transfer-Encoding': 'chunked',
                'Accept-Ranges': 'bytes',
                'Access-Control-Allow-Origin': '*'
              });
              headersSent = true;
            }
            dataSent = true;
            res.write(chunk);
          });

          child.stderr.on('data', (d) => {
            // Keep child alive; yt-dlp emits many non-fatal warnings
            console.log('[StreamServer yt-dlp stderr]', d.toString().trim());
          });

          child.on('error', (err) => {
            clearTimeout(fallbackTimeout);
            console.warn('[StreamServer] Child process error:', err.message);
            if (!tryFallback()) streamViaHttpProxy();
          });

          child.on('close', (code) => {
            clearTimeout(fallbackTimeout);
            if (fallbackStarted && !isFallback) return;
            if (!dataSent) {
              console.warn(`[StreamServer] yt-dlp exited with code ${code} without sending data`);
              if (!tryFallback()) streamViaHttpProxy();
            } else {
              endResponse();
            }
          });
        }

        req.on('close', () => {
          isEnded = true;
          if (activeChild) {
            try { activeChild.kill(); } catch (e) {}
          }
        });

        run(audioUrl, false);
      } else {
        res.writeHead(404);
        res.end();
      }
    } catch (e) {
      if (!res.headersSent) res.writeHead(500);
      res.end();
    }
  });

  streamServer.listen(0, '127.0.0.1', () => {
    streamServerPort = streamServer.address().port;
    console.log(`[StreamServer] Proxy server listening on http://127.0.0.1:${streamServerPort}`);
  });
}

startStreamServer();

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 900,
    minHeight: 600,
    frame: false,
    transparent: false,
    backgroundColor: '#00000000',
    backgroundMaterial: 'acrylic',
    icon: path.join(__dirname, 'assets', 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      webSecurity: false
    },
    show: false,
    titleBarStyle: 'hidden',
    titleBarOverlay: false
  });

  mainWindow.loadFile(path.join(__dirname, 'renderer', 'index.html'));

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  // Register media key shortcuts
  globalShortcut.register('MediaPlayPause', () => {
    mainWindow?.webContents.send('media-key', 'play-pause');
  });
  globalShortcut.register('MediaNextTrack', () => {
    mainWindow?.webContents.send('media-key', 'next');
  });
  globalShortcut.register('MediaPreviousTrack', () => {
    mainWindow?.webContents.send('media-key', 'previous');
  });
}

// ─── IPC Handlers ────────────────────────────────────────────────────────────

// Window controls
ipcMain.on('window-minimize', () => mainWindow?.minimize());
ipcMain.on('window-maximize', () => {
  if (mainWindow?.isMaximized()) {
    mainWindow.unmaximize();
  } else {
    mainWindow?.maximize();
  }
});
ipcMain.on('window-close', () => mainWindow?.close());

// Open folder dialog to scan music
ipcMain.handle('open-music-folder', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    properties: ['openDirectory'],
    title: 'Pilih Folder Musik'
  });
  if (result.canceled) return null;
  return result.filePaths[0];
});

// Select image file for album cover
ipcMain.handle('select-cover-image', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    properties: ['openFile'],
    title: 'Pilih Foto Cover Album',
    filters: [
      { name: 'Gambar (Cover Album)', extensions: ['jpg', 'jpeg', 'png', 'webp', 'gif', 'bmp'] }
    ]
  });
  if (result.canceled || !result.filePaths || result.filePaths.length === 0) {
    return null;
  }
  const filePath = result.filePaths[0];
  try {
    const ext = path.extname(filePath).toLowerCase().replace('.', '');
    const mimeType = ext === 'jpg' ? 'image/jpeg' : (ext === 'svg' ? 'image/svg+xml' : `image/${ext}`);
    const fileBuf = fs.readFileSync(filePath);
    const base64 = fileBuf.toString('base64');
    return `data:${mimeType};base64,${base64}`;
  } catch (err) {
    console.error('Gagal membaca file gambar cover:', err);
    return null;
  }
});

// Scan a directory for audio files
ipcMain.handle('scan-music-directory', async (_event, dirPath) => {
  const audioExtensions = ['.mp3', '.flac', '.wav', '.ogg', '.m4a', '.aac', '.wma', '.opus'];
  const songs = [];

  function scanDir(dir) {
    try {
      const entries = fs.readdirSync(dir, { withFileTypes: true });
      for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
          scanDir(fullPath);
        } else if (entry.isFile()) {
          const ext = path.extname(entry.name).toLowerCase();
          if (audioExtensions.includes(ext)) {
            const stats = fs.statSync(fullPath);
            songs.push({
              id: fullPath,
              title: path.basename(entry.name, ext),
              artist: 'Unknown Artist',
              album: path.basename(dir),
              path: fullPath,
              duration: 0,
              size: stats.size,
              dateAdded: stats.mtimeMs
            });
          }
        }
      }
    } catch (e) {
      // Skip inaccessible directories
    }
  }

  scanDir(dirPath);
  return songs;
});

// Read metadata from audio file
ipcMain.handle('read-metadata', async (_event, filePath) => {
  try {
    const mm = require('music-metadata');
    const metadata = await mm.parseFile(filePath);
    let coverDataUrl = null;
    if (metadata.common.picture && metadata.common.picture.length > 0) {
      const pic = metadata.common.picture[0];
      const base64 = Buffer.from(pic.data).toString('base64');
      coverDataUrl = `data:${pic.format};base64,${base64}`;
    }
    return {
      title: metadata.common.title || path.basename(filePath, path.extname(filePath)),
      artist: metadata.common.artist || 'Unknown Artist',
      album: metadata.common.album || 'Unknown Album',
      duration: metadata.format.duration || 0,
      cover: coverDataUrl
    };
  } catch (e) {
    return null;
  }
});

// Get user's default music folder
ipcMain.handle('get-default-music-path', () => {
  return path.join(app.getPath('music'));
});

// ─── Online Streaming & Downloader Tools ─────────────────────────────────────
const JAMENDO_CLIENT_ID = 'dde1e7c5';

function fetchText(url, headers = {}, redirectCount = 0) {
  return new Promise((resolve, reject) => {
    if (redirectCount > 5) return reject(new Error('Too many redirects'));
    try {
      const parsed = new URL(url);
      const client = parsed.protocol === 'http:' ? http : https;
      const options = {
        hostname: parsed.hostname,
        port: parsed.port || (parsed.protocol === 'http:' ? 80 : 443),
        path: parsed.pathname + parsed.search,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
          'Accept-Language': 'id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7',
          ...headers
        },
        timeout: 12000
      };
      const req = client.get(options, (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          const nextUrl = new URL(res.headers.location, url).href;
          return fetchText(nextUrl, headers, redirectCount + 1).then(resolve).catch(reject);
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          return reject(new Error(`HTTP ${res.statusCode}`));
        }
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => resolve(data));
      });
      req.on('error', reject);
      req.on('timeout', () => { req.destroy(); reject(new Error('timeout')); });
    } catch (err) {
      reject(err);
    }
  });
}

function parseSpotifyLink(input) {
  const match = String(input || '').trim().match(
    /(?:https?:\/\/(?:open|play)\.spotify\.com\/(?:intl-[a-zA-Z-]+\/)?(?:user\/[^/]+\/)?|spotify:)(track|playlist|album)[:/]([a-zA-Z0-9]+)/i
  );
  if (!match) return null;
  return { type: match[1].toLowerCase(), id: match[2] };
}

function extractSpotifyCoverArt(entity) {
  const images = entity?.visualIdentity?.image;
  if (!Array.isArray(images)) return null;
  let bestUrl = null;
  let maxDim = 0;
  for (const img of images) {
    if (!img || !img.url) continue;
    const dim = Math.max(img.maxWidth || 0, img.maxHeight || 0);
    if (dim >= maxDim) {
      maxDim = dim;
      bestUrl = img.url;
    }
  }
  return bestUrl;
}

function extractSpotifyTrackList(entity, defaultCover) {
  const trackList = entity?.trackList;
  if (!Array.isArray(trackList)) return [];
  return trackList.map((item, idx) => {
    const uri = item?.uri || '';
    const trackId = uri.startsWith('spotify:track:') ? uri.slice('spotify:track:'.length) : `${idx}`;
    return {
      id: trackId,
      title: item?.title || `Lagu ${idx + 1}`,
      artist: item?.subtitle || 'Artis Tidak Diketahui',
      durationMs: Number(item?.duration) || 0,
      coverArtUri: defaultCover || null,
      spotifyUri: uri
    };
  });
}

function fetchJson(url, redirectCount = 0) {
  return new Promise((resolve, reject) => {
    if (redirectCount > 4) return reject(new Error('Too many redirects'));
    try {
      const parsed = new URL(url);
      const client = parsed.protocol === 'http:' ? http : https;
      const options = {
        hostname: parsed.hostname,
        port: parsed.port || (parsed.protocol === 'http:' ? 80 : 443),
        path: parsed.pathname + parsed.search,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 MusikKu-Desktop/1.2.0',
          'Accept': 'application/json'
        },
        timeout: 8000
      };
      const req = client.get(options, (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          const nextUrl = new URL(res.headers.location, url).href;
          return fetchJson(nextUrl, redirectCount + 1).then(resolve).catch(reject);
        }
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          try {
            resolve(JSON.parse(data));
          } catch (e) {
            reject(e);
          }
        });
      });
      req.on('error', reject);
      req.on('timeout', () => { req.destroy(); reject(new Error('timeout')); });
    } catch (err) {
      reject(err);
    }
  });
}

// ─── Invidious / Piped Public Instances (Sama seperti Android InvidiousInstanceManager.kt) ────
const INVIDIOUS_INSTANCES = [
  'https://invidious.nerdvpn.de',
  'https://inv.nadeko.net',
  'https://invidious.f5.si',
  'https://invidious.privacyredirect.com',
  'https://iv.datura.network',
  'https://invidious.materialio.us',
  'https://invidious.perennialte.ch'
];

async function searchYouTubeViaHttp(query, page = 1) {
  const q = (query || '').trim();
  const targetQuery = (!q || q === 'popular' || q === 'trending') ? 'Trending Musik Indonesia' : q;
  const p = Math.max(1, parseInt(page, 10) || 1);

  for (const instance of INVIDIOUS_INSTANCES) {
    try {
      const url = `${instance}/api/v1/search?q=${encodeURIComponent(targetQuery)}&type=video&page=${p}`;
      const items = await fetchJson(url);
      if (Array.isArray(items) && items.length > 0) {
        const tracks = [];
        const seen = new Set();
        for (const item of items) {
          const vId = item.videoId || item.id;
          if (!vId || item.type === 'channel' || item.type === 'playlist') continue;
          const duration = Math.round(item.lengthSeconds || item.duration || 0);
          if (duration <= 45 || duration > 900) continue;
          if (seen.has(vId)) continue;
          seen.add(vId);

          const ytUrl = `https://www.youtube.com/watch?v=${vId}`;
          const fallbackQ = `${item.title || ''} ${item.author || item.uploader || ''}`.replace(/[/\\?%*:|"<>]/g, ' ').replace(/\s+/g, ' ').trim();
          const cover = item.videoThumbnails?.find(t => t.quality === 'high' || t.quality === 'medium')?.url 
            || `https://i.ytimg.com/vi/${vId}/hqdefault.jpg`;

          tracks.push({
            id: 'yt_' + vId,
            youtubeId: vId,
            title: item.title,
            artist: item.author || item.uploader || 'Artis',
            album: 'YouTube Music',
            duration: duration,
            cover: cover,
            streamUrl: `http://127.0.0.1:${streamServerPort}/stream?url=${encodeURIComponent(ytUrl)}&q=${encodeURIComponent(fallbackQ)}&id=${vId}`,
            sourceUrl: ytUrl,
            downloadUrl: ytUrl,
            isOnline: true,
            isFullTrack: true,
            source: 'YouTube Music'
          });
        }
        if (tracks.length > 0) return tracks;
      }
    } catch (_) {}
  }
  return [];
}

async function resolveInvidiousAudioUrl(videoId) {
  if (!videoId) return null;
  for (const instance of INVIDIOUS_INSTANCES) {
    try {
      const url = `${instance}/api/v1/videos/${videoId}`;
      const data = await fetchJson(url);
      if (data) {
        const adaptive = data.adaptiveFormats || [];
        const audioFormat = adaptive.find(f => (f.type || '').includes('audio/mp4') || (f.type || '').includes('audio/webm'))
          || adaptive.find(f => (f.type || '').includes('audio'))
          || (data.formatStreams || [])[0];
        if (audioFormat && audioFormat.url) {
          return audioFormat.url;
        }
      }
    } catch (_) {}
  }
  return null;
}

// Helper: Search SoundCloud for full commercial & indie music (No bot blocks)
function searchSoundCloud(query, page = 1) {
  return new Promise((resolve) => {
    const ytdlpBin = getYtDlpCommand();
    const p = Math.max(1, parseInt(page, 10) || 1);
    const pageSize = 15;
    const endIdx = p * pageSize;
    const startIdx = endIdx - pageSize + 1;

    execFile(ytdlpBin, [
      '-j', '--flat-playlist',
      '--playlist-items', `${startIdx}:${endIdx}`,
      `scsearch${endIdx}:${query}`
    ], { maxBuffer: 10 * 1024 * 1024 }, (err, stdout) => {
      if (err) {
        console.warn('SoundCloud search error:', err.message);
        return resolve([]);
      }
      try {
        const lines = stdout.trim().split('\n').filter(Boolean);
        const seen = new Set();
        const tracks = [];
        for (const line of lines) {
          try {
            const item = JSON.parse(line);
            const rawDuration = Math.round(item.duration || 0);

            // Strictly filter out 30-second Go+ preview snippets (hanya lagu utuh > 45 detik)
            if (rawDuration <= 45 || rawDuration >= 900) continue;
            if (seen.has(item.id)) continue;
            seen.add(item.id);

            const cover = item.thumbnails?.find(t => t.id === 't300x300' || t.id === 'large')?.url 
              || item.thumbnails?.[item.thumbnails.length - 1]?.url 
              || item.thumbnail 
              || '';
            const trackUrl = item.url || item.webpage_url || ('https://api.soundcloud.com/tracks/' + item.id);
            tracks.push({
              id: 'sc_' + item.id,
              title: item.title,
              artist: item.uploader || item.channel || 'Artis',
              album: 'Online Stream',
              duration: rawDuration,
              cover: cover,
              streamUrl: `http://127.0.0.1:${streamServerPort}/stream?url=${encodeURIComponent(trackUrl)}`,
              sourceUrl: trackUrl,
              downloadUrl: trackUrl,
              isOnline: true,
              isFullTrack: true,
              source: 'SoundCloud'
            });
          } catch (_) {}
        }
        resolve(tracks);
      } catch (e) {
        resolve([]);
      }
    });
  });
}

// Helper: Search YouTube for full-length songs (uses Android client to bypass bot check + Invidious Web fallback)
function searchYouTube(query, page = 1) {
  return new Promise((resolve) => {
    const ytdlpBin = getYtDlpCommand();
    const q = (query || '').trim();
    const targetQuery = (!q || q === 'popular' || q === 'trending') ? 'Trending Musik Indonesia' : q;
    const p = Math.max(1, parseInt(page, 10) || 1);
    const pageSize = 25;
    const endIdx = p * pageSize;
    const startIdx = endIdx - pageSize + 1;

    try {
      execFile(ytdlpBin, [
        '--extractor-args', 'youtube:player_client=web,mweb,android',
        '-j', '--flat-playlist',
        '--playlist-items', `${startIdx}:${endIdx}`,
        `ytsearch${endIdx}:${targetQuery}`
      ], { maxBuffer: 15 * 1024 * 1024 }, async (err, stdout) => {
        if (err || !stdout || stdout.trim().length === 0) {
          console.log('[YouTubeSearch] yt-dlp tidak tersedia/gagal. Beralih ke Invidious Web API...');
          const httpResults = await searchYouTubeViaHttp(query, page);
          return resolve(httpResults);
        }
        try {
          const lines = stdout.trim().split('\n').filter(Boolean);
          const tracks = [];
          const seen = new Set();
          for (const line of lines) {
            try {
              const item = JSON.parse(line);
              const rawDuration = Math.round(item.duration || 0);
              // Hanya lagu utuh (45s s.d. 15 menit), abaikan mixtape kompilasi 1-2 jam
              if (rawDuration <= 45 || rawDuration > 900) continue;
              if (seen.has(item.id)) continue;
              seen.add(item.id);

              const ytUrl = `https://www.youtube.com/watch?v=${item.id}`;
              const fallbackQ = `${item.title || ''} ${item.uploader || item.channel || ''}`.replace(/[/\\?%*:|"<>]/g, ' ').replace(/\s+/g, ' ').trim();
              tracks.push({
                id: 'yt_' + item.id,
                youtubeId: item.id,
                title: item.title,
                artist: item.uploader || item.channel || 'Artis',
                album: 'YouTube Music',
                duration: rawDuration,
                cover: item.thumbnails?.[item.thumbnails.length - 1]?.url || `https://i.ytimg.com/vi/${item.id}/hqdefault.jpg`,
                streamUrl: `http://127.0.0.1:${streamServerPort}/stream?url=${encodeURIComponent(ytUrl)}&q=${encodeURIComponent(fallbackQ)}&id=${item.id}`,
                sourceUrl: ytUrl,
                downloadUrl: ytUrl,
                isOnline: true,
                isFullTrack: true,
                source: 'YouTube Music'
              });
            } catch (_) {}
          }
          if (tracks.length === 0) {
            const httpResults = await searchYouTubeViaHttp(query, page);
            return resolve(httpResults);
          }
          resolve(tracks);
        } catch (e) {
          const httpResults = await searchYouTubeViaHttp(query, page);
          resolve(httpResults);
        }
      });
    } catch (_) {
      searchYouTubeViaHttp(query, page).then(resolve);
    }
  });
}

// Helper: Search Jamendo API for indie & CC royalty-free music (same as Android app)
async function searchJamendo(query, page = 1) {
  const q = (query || '').trim();
  const isPopular = !q || q === 'popular' || q === 'populer';
  const p = Math.max(1, parseInt(page, 10) || 1);
  const limit = 30;
  const offset = (p - 1) * limit;

  if (isPopular) {
    try {
      const url = `https://api.jamendo.com/v3.0/tracks/?client_id=${JAMENDO_CLIENT_ID}&format=json&limit=${limit}&offset=${offset}&order=popularity_total&audioformat=mp32&audiodlformat=mp32`;
      const data = await fetchJson(url);
      if (data && data.results && data.results.length > 0) {
        return data.results.map(t => ({
          id: 'jamendo_' + t.id,
          title: t.name,
          artist: t.artist_name || 'Jamendo Artist',
          album: t.album_name || 'Jamendo Music',
          duration: t.duration || 0,
          cover: t.album_image || t.image || '',
          streamUrl: t.audio,
          downloadUrl: t.audiodownload || t.audio,
          isOnline: true,
          isFullTrack: true,
          source: 'Jamendo'
        })).filter(t => t.duration > 45);
      }
    } catch (e) {
      console.warn('Jamendo popular error:', e.message);
    }
  }

  // Search by name first, then fallback to fuzzytags (same as JamendoRepository.kt)
  const strategies = ['namesearch', 'fuzzytags', 'search'];
  for (const strategy of strategies) {
    try {
      const url = `https://api.jamendo.com/v3.0/tracks/?client_id=${JAMENDO_CLIENT_ID}&format=json&limit=${limit}&offset=${offset}&${strategy}=${encodeURIComponent(q)}&order=popularity_month&audioformat=mp32&audiodlformat=mp32`;
      const data = await fetchJson(url);
      if (data && data.results && data.results.length > 0) {
        return data.results.map(t => ({
          id: 'jamendo_' + t.id,
          title: t.name,
          artist: t.artist_name || 'Jamendo Artist',
          album: t.album_name || 'Jamendo Music',
          duration: t.duration || 0,
          cover: t.album_image || t.image || '',
          streamUrl: t.audio,
          downloadUrl: t.audiodownload || t.audio,
          isOnline: true,
          isFullTrack: true,
          source: 'Jamendo'
        })).filter(t => t.duration > 45);
      }
    } catch (e) {
      // try next strategy
    }
  }
  return [];
}

// 1. Online Search Handler (Supports source: 'youtube' | 'jamendo' | 'all')
ipcMain.handle('online-search', async (_event, queryOrOptions = '') => {
  let query = '';
  let source = 'all';
  let page = 1;

  if (typeof queryOrOptions === 'object' && queryOrOptions !== null) {
    query = (queryOrOptions.query || '').trim();
    source = queryOrOptions.source || 'all';
    page = Math.max(1, parseInt(queryOrOptions.page, 10) || 1);
  } else {
    query = (queryOrOptions || '').trim();
  }

  try {
    if (source === 'jamendo') {
      return await searchJamendo(query, page);
    }

    if (source === 'youtube') {
      const [ytTracks, scTracks] = await Promise.all([
        searchYouTube(query, page),
        query ? searchSoundCloud(query, page) : Promise.resolve([])
      ]);
      const seen = new Set();
      const combined = [];
      for (const track of [...ytTracks, ...scTracks]) {
        const normKey = (track.title + '_' + track.artist).toLowerCase().replace(/[^a-z0-9]/g, '');
        if (seen.has(normKey) || seen.has(track.id)) continue;
        seen.add(normKey);
        seen.add(track.id);
        combined.push(track);
      }
      return combined;
    }

    // Default 'all': search YouTube + Jamendo in parallel
    const [ytRes, jamendoRes, scRes] = await Promise.allSettled([
      searchYouTube(query, page),
      searchJamendo(query, page),
      query ? searchSoundCloud(query, page) : Promise.resolve([])
    ]);

    const ytTracks = ytRes.status === 'fulfilled' ? ytRes.value : [];
    const jamendoTracks = jamendoRes.status === 'fulfilled' ? jamendoRes.value : [];
    const scTracks = scRes.status === 'fulfilled' ? scRes.value : [];

    const seen = new Set();
    const combined = [];
    for (const track of [...ytTracks, ...jamendoTracks, ...scTracks]) {
      if (track.duration <= 45) continue;
      const normKey = (track.title + '_' + track.artist).toLowerCase().replace(/[^a-z0-9]/g, '');
      if (seen.has(normKey) || seen.has(track.id)) continue;
      seen.add(normKey);
      seen.add(track.id);
      combined.push(track);
    }
    return combined;
  } catch (err) {
    console.warn('Online search error:', err);
    return [];
  }
});

// 2. On-demand Audio Stream URL Resolution
ipcMain.handle('resolve-stream-url', async (_event, targetOrSong) => {
  if (!targetOrSong) return '';
  let targetIdOrUrl = '';
  let fallbackQuery = '';

  if (typeof targetOrSong === 'object' && targetOrSong !== null) {
    targetIdOrUrl = targetOrSong.sourceUrl || targetOrSong.youtubeId || targetOrSong.streamUrl || '';
    fallbackQuery = `${targetOrSong.artist || ''} - ${targetOrSong.title || ''}`.replace(/[/\\?%*:|"<>]/g, ' ').replace(/\s+/g, ' ').trim();
  } else {
    targetIdOrUrl = String(targetOrSong);
  }

  // Jangan gunakan URI spotify: atau link halaman open.spotify.com secara langsung sebagai URL audio
  if (targetIdOrUrl.startsWith('spotify:') || targetIdOrUrl.includes('open.spotify.com')) {
    targetIdOrUrl = '';
  }

  // Jika URL belum tersedia (misal lagu hasil impor Spotify), cari kecocokan di YouTube / SoundCloud
  if (!targetIdOrUrl && fallbackQuery) {
    try {
      const ytMatches = await searchYouTube(fallbackQuery, 1);
      if (Array.isArray(ytMatches) && ytMatches.length > 0) {
        targetIdOrUrl = ytMatches[0].sourceUrl || ytMatches[0].youtubeId || '';
      }
      if (!targetIdOrUrl) {
        const scMatches = await searchSoundCloud(fallbackQuery, 1);
        if (Array.isArray(scMatches) && scMatches.length > 0) {
          targetIdOrUrl = scMatches[0].sourceUrl || '';
        }
      }
    } catch (e) {
      console.warn('[resolve-stream-url] Pencarian lagu fallback gagal:', e.message);
    }
  }

  if (!targetIdOrUrl) return '';

  // Jika sudah URL proxy aktif lengkap dengan query, kembalikan langsung
  if (targetIdOrUrl.startsWith('http://127.0.0.1') && targetIdOrUrl.includes('/stream?url=')) {
    return targetIdOrUrl;
  }

  // Direct MP3 URL (seperti Jamendo) tidak perlu proxy
  if (targetIdOrUrl.includes('jamendo.com') || (targetIdOrUrl.startsWith('http') && targetIdOrUrl.endsWith('.mp3'))) {
    return targetIdOrUrl;
  }
  // Jika sudah URL streaming googlevideo langsung
  if (targetIdOrUrl.includes('googlevideo.com') || targetIdOrUrl.includes('sndcdn.com')) {
    return targetIdOrUrl;
  }
  const url = targetIdOrUrl.startsWith('http') ? targetIdOrUrl : `https://www.youtube.com/watch?v=${targetIdOrUrl}`;
  let vId = '';
  const match = url.match(/(?:youtu\.be\/|v\/|watch\?v=|\&v=)([^#\&\?]{11})/i);
  if (match) vId = match[1];

  // 1. Prioritas: Coba dapatkan direct CDN URL (googlevideo / sndcdn) via yt-dlp -g
  // Direct CDN URL langsung di-stream oleh HTML5 audio tanpa beban proxy lokal (bisa scrubbing & fast buffer)
  const audioQuality = (typeof targetOrSong === 'object' && targetOrSong?.audioQuality) || 'standard';
  try {
    const directUrl = await resolveDirectAudioUrl(url, audioQuality);
    if (directUrl && directUrl.startsWith('http')) {
      console.log('[resolve-stream-url] Direct CDN stream URL berhasil didapatkan');
      return directUrl;
    }
  } catch (err) {
    console.warn('[resolve-stream-url] Direct resolution gagal, fallback ke proxy lokal:', err.message);
  }

  // 2. Fallback: Proxy streaming lokal
  return `http://127.0.0.1:${streamServerPort}/stream?url=${encodeURIComponent(url)}&q=${encodeURIComponent(fallbackQuery)}${vId ? `&id=${vId}` : ''}`;
});

// 2b. Lyrics Retrieval via LRCLIB (Exact match + fuzzy fallback + title sanitizer)
const lyricsCache = new Map();

function cleanTitleForLyrics(raw) {
  let s = (raw || '').replace(/_/g, ' ');
  s = s.replace(/^\d{1,3}[\s.\-_]+/, '');
  s = s.replace(/\.(mp3|m4a|opus|flac|wav|ogg)$/i, '');
  s = s.replace(/\((?:from|feat\.?|official|lyrical|video|audio|remix|mv|hd|4k|karaoke|full song)[^)]*\)|\[[^\]]*\]/gi, ' ');
  s = s.replace(/\b(?:official (?:video|audio|music video)|lyrical|full song|4k video)\b/gi, ' ');
  s = s.split(' | ')[0];
  return s.replace(/\s+/g, ' ').trim() || raw;
}

function cleanArtistForLyrics(raw) {
  let s = (raw || '').replace(/_/g, ' ');
  s = s.replace(/\b(VEVO|Topic|Official|Channel)\b/gi, ' ');
  s = s.replace(/\s+/g, ' ').trim();
  if (s.toLowerCase() === 'unknown' || s.toLowerCase() === 'unknown artist') return '';
  return s;
}

ipcMain.handle('get-lyrics', async (_event, params = {}) => {
  let { title = '', artist = '', duration = 0 } = params;
  let cleanTitle = cleanTitleForLyrics(title);
  let cleanArtist = cleanArtistForLyrics(artist);

  // Jika title mengandung format "Artis - Judul"
  if (cleanTitle.includes(' - ')) {
    const parts = cleanTitle.split(' - ');
    if (parts.length === 2 && parts[0].trim() && parts[1].trim()) {
      cleanArtist = cleanArtist || parts[0].trim();
      cleanTitle = parts[1].trim();
    }
  }

  const cacheKey = `${cleanTitle.toLowerCase()}|${cleanArtist.toLowerCase()}`;
  if (lyricsCache.has(cacheKey)) {
    return lyricsCache.get(cacheKey);
  }

  const durationSec = duration > 0 ? Math.round(duration) : null;

  try {
    // 1. Coba exact match /api/get
    if (cleanTitle && cleanArtist) {
      let url = `https://lrclib.net/api/get?track_name=${encodeURIComponent(cleanTitle)}&artist_name=${encodeURIComponent(cleanArtist)}`;
      if (durationSec) url += `&duration=${durationSec}`;
      const data = await fetchJson(url);
      if (data && (data.syncedLyrics || data.plainLyrics)) {
        const res = {
          syncedLyrics: data.syncedLyrics || null,
          plainLyrics: data.plainLyrics || null,
          trackName: data.trackName || cleanTitle,
          artistName: data.artistName || cleanArtist,
          source: 'LRCLIB'
        };
        lyricsCache.set(cacheKey, res);
        return res;
      }
    }

    // 2. Coba fuzzy search /api/search
    const searchQuery = cleanArtist ? `${cleanArtist} ${cleanTitle}` : cleanTitle;
    const searchUrl = `https://lrclib.net/api/search?q=${encodeURIComponent(searchQuery)}`;
    const list = await fetchJson(searchUrl);
    if (Array.isArray(list) && list.length > 0) {
      const withSynced = list.filter(x => x.syncedLyrics);
      let best = null;
      if (withSynced.length > 0) {
        if (durationSec) {
          best = withSynced.sort((a, b) => Math.abs((a.duration || 0) - durationSec) - Math.abs((b.duration || 0) - durationSec))[0];
        } else {
          best = withSynced[0];
        }
      } else {
        best = list.find(x => x.plainLyrics) || list[0];
      }

      if (best) {
        const res = {
          syncedLyrics: best.syncedLyrics || null,
          plainLyrics: best.plainLyrics || null,
          trackName: best.trackName || cleanTitle,
          artistName: best.artistName || cleanArtist,
          source: 'LRCLIB'
        };
        lyricsCache.set(cacheKey, res);
        return res;
      }
    }
  } catch (err) {
    console.warn('Lyrics fetch error:', err.message);
  }

  return null;
});

// 3. Download Track Handler (Full-length audio download to disk with progress)
ipcMain.handle('online-download', async (_event, track) => {
  const downloadsDir = path.join(app.getPath('music'), 'MusikKu Downloads');
  if (!fs.existsSync(downloadsDir)) {
    fs.mkdirSync(downloadsDir, { recursive: true });
  }

  // Retrieve raw target URL from any available field
  let rawUrl = (track.sourceUrl || track.downloadUrl || track.streamUrl || track.path || (track.youtubeId ? `https://www.youtube.com/watch?v=${track.youtubeId}` : '')).trim();
  if (rawUrl.startsWith('spotify:') || rawUrl.includes('open.spotify.com')) {
    rawUrl = '';
  }

  let title = (track.title || '').trim();
  let artist = (track.artist || '').trim();

  if (!rawUrl && title) {
    const searchQ = `${artist && artist !== 'Unknown Artist' ? artist + ' ' : ''}${title}`.trim();
    const ytMatches = await searchYouTube(searchQ, 1);
    if (Array.isArray(ytMatches) && ytMatches.length > 0 && ytMatches[0].sourceUrl) {
      rawUrl = ytMatches[0].sourceUrl;
    } else {
      rawUrl = `ytsearch1:${searchQ}`;
    }
  }

  if (!rawUrl) throw new Error('URL unduhan tidak ditemukan');

  // If it's a YouTube URL, extract videoId and fetch metadata if title/artist are missing or generic
  const ytMatch = rawUrl.match(/(?:youtu\.be\/|v\/|u\/\w\/|embed\/|watch\?v=|\&v=)([^#\&\?]{11})/i);
  if (ytMatch && (!title || title === 'MusikKu Audio' || !artist || artist === 'Unknown Artist')) {
    try {
      const oembed = await fetchJson(`https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=${ytMatch[1]}&format=json`);
      if (oembed && oembed.title) {
        if (oembed.title.includes(' - ')) {
          const parts = oembed.title.split(' - ');
          if (!artist || artist === 'Unknown Artist') artist = parts[0].trim();
          if (!title || title === 'MusikKu Audio') title = parts[1].replace(/\(.*?\)|\[.*?\]/g, '').trim();
        } else {
          if (!title || title === 'MusikKu Audio') title = oembed.title;
          if (!artist || artist === 'Unknown Artist') artist = oembed.author_name || 'Artis';
        }
      }
    } catch (e) {}
  }

  const safeTitle = (title || 'Track').replace(/[/\\?%*:|"<>#]/g, '_');
  const safeArtist = (artist || 'Artist').replace(/[/\\?%*:|"<>#]/g, '_');

  // Check if it's a direct static MP3 / audio file from a server (like Jamendo direct CDN)
  const isDirectAudio = rawUrl.startsWith('http') && 
    (rawUrl.includes('jamendo.com') || rawUrl.match(/\.(mp3|wav|flac|ogg|aac)(\?.*)?$/i)) &&
    !rawUrl.includes('youtu') && !rawUrl.includes('soundcloud');

  if (!isDirectAudio) {
    const isSearchSource = rawUrl.startsWith('ytsearch') || rawUrl.startsWith('scsearch');
    const targetSource = (rawUrl.startsWith('http') || isSearchSource) ? rawUrl : `https://www.youtube.com/watch?v=${rawUrl}`;
    const fileName = `${safeArtist} - ${safeTitle}.m4a`;
    const targetPath = path.join(downloadsDir, fileName);
    const dlFormat = getYtDlpFormatSelector(track?.audioQuality || 'standard', true);

    return new Promise((resolve, reject) => {
      mainWindow?.webContents.send('download-progress', { id: track.id, progress: 20, status: 'downloading' });
      const ytdlpBin = getYtDlpCommand();
      const fallbackQ = `${artist} ${title}`.replace(/[/\\?%*:|"<>]/g, ' ').replace(/\s+/g, ' ').trim();

      function execDownload(source, isFallback = false) {
        const dlArgs = (isFallback || isSearchSource)
          ? [
              '--match-filter', 'duration > 45 & duration < 900',
              '-f', dlFormat,
              '--max-downloads', '1',
              '-o', targetPath,
              source
            ]
          : [
              '--extractor-args', 'youtube:player_client=web,mweb,android',
              '-f', dlFormat,
              '-o', targetPath,
              source
            ];

        execFile(ytdlpBin, dlArgs, (err) => {
          let fileDownloaded = false;
          try {
            fileDownloaded = fs.existsSync(targetPath) && fs.statSync(targetPath).size > 10000;
          } catch (e) {}

          if (!fileDownloaded) {
            if (!isFallback && fallbackQ) {
              console.log(`[Downloader] YouTube failed. Auto-fallback to SoundCloud search: scsearch5:${fallbackQ}`);
              mainWindow?.webContents.send('download-progress', { id: track.id, progress: 45, status: 'downloading' });
              return execDownload(`scsearch5:${fallbackQ}`, true);
            }
            return reject(err || new Error('Gagal mengunduh file audio'));
          }

          try {
            const stats = fs.statSync(targetPath);
            let realDuration = track.duration || 0;
            let realCover = track.cover || '';
            try {
              const mm = require('music-metadata');
              const meta = fs.existsSync(targetPath) ? fs.readFileSync(targetPath) : null;
              if (meta) {
                // Parse synchronously from buffer or parseFile
                mm.parseFile(targetPath).then(parsed => {
                  if (parsed && parsed.format && parsed.format.duration) {
                    realDuration = Math.round(parsed.format.duration);
                  }
                }).catch(() => {});
              }
            } catch (e) {}

            mainWindow?.webContents.send('download-progress', { id: track.id, progress: 100, status: 'completed' });
            resolve({
              success: true,
              filePath: targetPath,
              fileName,
              title: title || track.title,
              artist: artist || track.artist,
              album: 'MusikKu Downloads',
              cover: realCover || track.cover || '',
              duration: realDuration,
              size: stats.size
            });
          } catch (statErr) {
            reject(statErr);
          }
        });
      }

      execDownload(targetSource, false);
    });
  }

  // Direct HTTP(S) download fallback (untuk file MP3 langsung / Jamendo)
  const isM4a = rawUrl.includes('.m4a') || rawUrl.includes('.aac');
  const ext = isM4a ? '.m4a' : '.mp3';
  const fileName = `${safeArtist} - ${safeTitle}${ext}`;
  const targetPath = path.join(downloadsDir, fileName);

  return new Promise((resolve, reject) => {
    function downloadFromUrl(currentUrl, redirectCount = 0) {
      if (redirectCount > 5) {
        return reject(new Error('Terlalu banyak redirect HTTP'));
      }

      const client = currentUrl.startsWith('https') ? https : http;
      const req = client.get(currentUrl, (res) => {
        if (res.statusCode === 301 || res.statusCode === 302 || res.statusCode === 303 || res.statusCode === 307 || res.statusCode === 308) {
          const redirectLocation = res.headers.location;
          if (redirectLocation) {
            return downloadFromUrl(redirectLocation, redirectCount + 1);
          }
        }

        if (res.statusCode !== 200) {
          return reject(new Error(`Gagal mengunduh file: HTTP status ${res.statusCode}`));
        }

        const totalBytes = parseInt(res.headers['content-length'] || '0', 10);
        let receivedBytes = 0;
        const fileStream = fs.createWriteStream(targetPath);

        res.on('data', (chunk) => {
          receivedBytes += chunk.length;
          if (totalBytes > 0) {
            const progress = Math.round((receivedBytes / totalBytes) * 100);
            mainWindow?.webContents.send('download-progress', {
              id: track.id,
              progress,
              receivedBytes,
              totalBytes,
              status: 'downloading'
            });
          }
        });

        res.pipe(fileStream);

        fileStream.on('finish', () => {
          fileStream.close();
          mainWindow?.webContents.send('download-progress', {
            id: track.id,
            progress: 100,
            status: 'completed',
            filePath: targetPath
          });
          resolve({
            success: true,
            filePath: targetPath,
            fileName,
            title: track.title,
            artist: track.artist,
            album: track.album,
            cover: track.cover,
            duration: track.duration,
            size: receivedBytes
          });
        });

        fileStream.on('error', (err) => {
          fs.unlink(targetPath, () => {});
          reject(err);
        });
      });

      req.on('error', (err) => {
        fs.unlink(targetPath, () => {});
        reject(err);
      });
    }

    downloadFromUrl(downloadUrl);
  });
});

// 3. Open Downloads Folder in Windows Explorer
ipcMain.handle('open-downloads-folder', async () => {
  const downloadsDir = path.join(app.getPath('music'), 'MusikKu Downloads');
  if (!fs.existsSync(downloadsDir)) {
    fs.mkdirSync(downloadsDir, { recursive: true });
  }
  await shell.openPath(downloadsDir);
  return downloadsDir;
});

// 4. Get Downloads Folder Path
ipcMain.handle('get-downloads-folder', () => {
  const downloadsDir = path.join(app.getPath('music'), 'MusikKu Downloads');
  if (!fs.existsSync(downloadsDir)) {
    fs.mkdirSync(downloadsDir, { recursive: true });
  }
  return downloadsDir;
});

// 5. Permanently delete local or downloaded audio file from disk
ipcMain.handle('delete-song-file', async (_event, filePath) => {
  if (!filePath || typeof filePath !== 'string') {
    return { success: false, error: 'Path file tidak valid.' };
  }
  try {
    if (!fs.existsSync(filePath)) {
      return { success: true, alreadyMissing: true };
    }
    await fs.promises.unlink(filePath);
    return { success: true };
  } catch (err) {
    console.error('Gagal menghapus file audio:', err);
    return { success: false, error: err?.message || 'Gagal menghapus file.' };
  }
});

// 6. Import Spotify Public Track / Album / Playlist Metadata via Embed Endpoint
ipcMain.handle('import-spotify', async (_event, urlOrUri) => {
  const parsed = parseSpotifyLink(urlOrUri);
  if (!parsed) {
    return { success: false, error: 'Tautan Spotify tidak dikenali atau tidak valid. Gunakan link open.spotify.com/playlist/..., album/..., atau track/...' };
  }

  const { type, id } = parsed;
  const embedUrl = `https://open.spotify.com/embed/${type}/${id}`;

  try {
    const html = await fetchText(embedUrl);
    const nextMatch = html.match(/<script id="__NEXT_DATA__" type="application\/json">([\s\S]*?)<\/script>/);
    if (!nextMatch || !nextMatch[1]) {
      return { success: false, error: 'Data konten Spotify tidak ditemukan pada halaman embed.' };
    }

    const rootJson = JSON.parse(nextMatch[1]);
    const entity = rootJson?.props?.pageProps?.state?.data?.entity;

    if (!entity) {
      if (type === 'track') {
        try {
          const oembed = await fetchJson(`https://open.spotify.com/oembed?url=https://open.spotify.com/track/${id}`);
          if (oembed && oembed.title) {
            const parts = oembed.title.split(' by ');
            const title = parts[0].trim() || 'Lagu Spotify';
            const artist = (parts[1] || '').trim() || 'Artis Spotify';
            const cover = oembed.thumbnail_url || null;
            return {
              success: true,
              type: 'track',
              id,
              title,
              subtitle: artist,
              coverArtUri: cover,
              originalUrl: urlOrUri,
              tracks: [{
                id,
                title,
                artist,
                durationMs: 0,
                coverArtUri: cover,
                spotifyUri: `spotify:track:${id}`
              }]
            };
          }
        } catch (_) {}
      }
      return { success: false, error: 'Konten Spotify tidak ditemukan atau link bersifat privat.' };
    }

    const coverArtUri = extractSpotifyCoverArt(entity);

    if (type === 'track') {
      const title = entity.title || entity.name || 'Tanpa Judul';
      const artists = Array.isArray(entity.artists)
        ? entity.artists.map(a => a?.name).filter(Boolean).join(', ')
        : '';
      const artist = artists || entity.subtitle || 'Artis Tidak Diketahui';
      const durationMs = Number(entity.duration) || 0;
      const spotifyUri = entity.uri || `spotify:track:${id}`;
      return {
        success: true,
        type: 'track',
        id,
        title,
        subtitle: artist,
        coverArtUri,
        originalUrl: urlOrUri,
        tracks: [{
          id,
          title,
          artist,
          durationMs,
          coverArtUri,
          spotifyUri
        }]
      };
    }

    const tracks = extractSpotifyTrackList(entity, coverArtUri);
    const title = (type === 'playlist' ? (entity.name || entity.title) : (entity.title || entity.name)) || (type === 'playlist' ? 'Playlist Spotify' : 'Album Spotify');
    const subPrefix = entity.subtitle || (type === 'playlist' ? 'Spotify Playlist' : 'Artis Tidak Diketahui');
    return {
      success: true,
      type,
      id,
      title,
      subtitle: `${subPrefix} • ${tracks.length} lagu`,
      coverArtUri,
      originalUrl: urlOrUri,
      tracks
    };
  } catch (err) {
    console.error('[import-spotify] Error:', err);
    return { success: false, error: `Gagal mengambil data dari Spotify (${err?.message || 'Error'}). Pastikan link publik & koneksi internet aktif.` };
  }
});

// App lifecycle
app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  globalShortcut.unregisterAll();
  app.quit();
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
