import { readdirSync } from 'node:fs'
import { join } from 'node:path'
import { spawnSync } from 'node:child_process'

function testFiles(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name)
    return entry.isDirectory() ? testFiles(path) : /\.test\.tsx?$/.test(entry.name) ? [path] : []
  })
}

let failed = false
for (const file of testFiles('src').sort()) {
  process.stdout.write(`\n${file}\n`)
  const result = spawnSync(process.execPath, ['--import', 'tsx', file], { stdio: 'inherit' })
  if (result.error) throw result.error
  if (result.status !== 0) failed = true
}
process.exit(failed ? 1 : 0)
