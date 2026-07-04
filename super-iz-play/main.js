'use strict'
// Super IZ Play — shell Electron do painel de monitoramento (admin).
// App SEPARADO do cliente (pasta própria). Só lê dados reais do Painel Admin.
const { app, BrowserWindow, shell } = require('electron')

function createWindow() {
  const win = new BrowserWindow({
    width: 1320,
    height: 860,
    minWidth: 980,
    minHeight: 640,
    title: 'Super IZ Play — Monitor',
    backgroundColor: '#0a0a0a',
    autoHideMenuBar: true,
    webPreferences: {
      // ferramenta interna de admin, conteúdo local confiável -> Node no renderer
      // (permite chamar as APIs do Painel via http/https do Node, sem CORS).
      nodeIntegration: true,
      contextIsolation: false,
      // habilita <webview> para a aba "Player de teste" (embute o web player real)
      webviewTag: true
    }
  })
  win.setMenuBarVisibility(false)
  win.loadFile('index.html')
  // links externos abrem no navegador
  win.webContents.setWindowOpenHandler(({ url }) => { shell.openExternal(url); return { action: 'deny' } })
}

app.whenReady().then(createWindow)
app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) createWindow() })
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit() })
