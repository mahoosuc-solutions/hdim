import fs from 'fs'
import path from 'path'

const repoRoot = path.resolve(process.cwd(), '..')
const readmePath = path.join(process.cwd(), 'README.md')
const workflowPath = path.join(repoRoot, '.github', 'workflows', 'landing-page-validation.yml')

const expectedWorkflowRef = 'actions/workflows/landing-page-validation.yml'

function fail(message: string): never {
  console.error(`❌ ${message}`)
  process.exit(1)
}

console.log('🔍 Validating README workflow badge...')

if (!fs.existsSync(readmePath)) {
  fail('README.md not found in landing-page-v0')
}

if (!fs.existsSync(workflowPath)) {
  fail('Root workflow missing: .github/workflows/landing-page-validation.yml')
}

const readme = fs.readFileSync(readmePath, 'utf-8')

if (!readme.includes(expectedWorkflowRef)) {
  fail(`README badge/link does not reference ${expectedWorkflowRef}`)
}

console.log('✅ README badge references landing-page validation workflow')
