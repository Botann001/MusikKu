const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('elektronAPI', {
  // Window controls
  minimize: () => ipcRenderer.send('window-minimize'),
  maximize: () => ipcRenderer.send('window-maximize'),
  close: () => ipcRenderer.send('window-close'),

  // Music operations
  openMusicFolder: () => ipcRenderer.invoke('open-music-folder'),
  scanMusicDirectory: (dirPath) => ipcRenderer.invoke('scan-music-directory', dirPath),
  readMetadata: (filePath) => ipcRenderer.invoke('read-metadata', filePath),
  getDefaultMusicPath: () => ipcRenderer.invoke('get-default-music-path'),
  selectCoverImage: () => ipcRenderer.invoke('select-cover-image'),

  // Media key listeners
  onMediaKey: (callback) => {
    ipcRenderer.on('media-key', (_event, action) => callback(action));
  },

  // Lyrics
  getLyrics: (params) => ipcRenderer.invoke('get-lyrics', params),

  // Online Streaming & Downloader Tools
  onlineSearch: (query) => ipcRenderer.invoke('online-search', query),
  resolveStreamUrl: (youtubeId) => ipcRenderer.invoke('resolve-stream-url', youtubeId),
  downloadTrack: (track) => ipcRenderer.invoke('online-download', track),
  openDownloadsFolder: () => ipcRenderer.invoke('open-downloads-folder'),
  getDownloadsFolder: () => ipcRenderer.invoke('get-downloads-folder'),
  deleteSongFile: (filePath) => ipcRenderer.invoke('delete-song-file', filePath),
  importSpotify: (url) => ipcRenderer.invoke('import-spotify', url),
  onDownloadProgress: (callback) => {
    ipcRenderer.on('download-progress', (_event, data) => callback(data));
  }
});
