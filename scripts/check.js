'use strict'

const fs = require('fs')
const path = require('path')
const vm = require('vm')
const { spawnSync } = require('child_process')

const root = path.resolve(__dirname, '..')
const failures = []

function checkNode(file) {
  const result = spawnSync(process.execPath, ['--check', file], { encoding: 'utf8' })
  if (result.status !== 0) failures.push(`${file}: ${result.stderr.trim()}`)
}

function checkHtml(file) {
  const html = fs.readFileSync(file, 'utf8')
  const scripts = [...html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/gi)]
    .map(match => match[1])
    .filter(script => script.trim())
  scripts.forEach((script, index) => {
    try { new vm.Script(script, { filename: `${file}#script-${index}` }) }
    catch (error) { failures.push(`${file}: ${error.message}`) }
  })
}

checkNode(path.join(root, 'app', 'main.js'))
checkNode(path.join(root, 'gateway', 'server.js'))
checkNode(path.join(root, 'panel', 'server.js'))
checkHtml(path.join(root, 'app', 'index.html'))
checkHtml(path.join(root, 'web-player', 'index.html'))
checkHtml(path.join(root, 'panel', 'index.html'))

const required = [
  'app/index.html',
  'app/main.js',
  'android/gradlew.bat',
  'android/app/src/main/AndroidManifest.xml',
  'web-player/index.html',
  'web-player/manifest.json',
  'web-player/service-worker.js',
  'web-player/vendor/swarmcloud-hls.min.js',
  'deploy/nginx-izplay-v2.1.conf',
  'deploy/izplay-gateway.service',
  'deploy/izplay-panel.service',
  'releases/android/iz-play-android-v2.1-debug.apk'
]
for (const relative of required) {
  if (!fs.existsSync(path.join(root, relative))) failures.push(`Arquivo ausente: ${relative}`)
}

if (failures.length) {
  console.error(failures.join('\n'))
  process.exit(1)
}
console.log('IZ Play v2.1: validacao estrutural concluida.')
