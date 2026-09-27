'use client'

import { useEffect, useMemo, useRef, useState, type MouseEvent } from 'react'
import hljs from 'highlight.js/lib/common'

const RAW_BASE = 'https://raw.githubusercontent.com/StormDevzz/RaveX/main/'
const DEFAULT_FILE = 'src/main/java/ravex/modules/player/AirPlace.java'

interface TreeItem {
  path: string
  type: string
}

interface TreeNode {
  name: string
  path: string
  type: 'blob' | 'tree'
  children?: TreeNode[]
}

interface LangConf {
  hl?: string
  label: string
  binary?: boolean
}

const EXT_LANG: Record<string, LangConf> = {
  java: { hl: 'java', label: 'Java' },
  js: { hl: 'javascript', label: 'JavaScript' },
  mjs: { hl: 'javascript', label: 'JavaScript' },
  jsx: { hl: 'javascript', label: 'JavaScript JSX' },
  ts: { hl: 'typescript', label: 'TypeScript' },
  tsx: { hl: 'typescript', label: 'TypeScript JSX' },
  json: { hl: 'json', label: 'JSON' },
  md: { hl: 'markdown', label: 'Markdown' },
  css: { hl: 'css', label: 'CSS' },
  scss: { hl: 'scss', label: 'SCSS' },
  html: { hl: 'xml', label: 'HTML' },
  htm: { hl: 'xml', label: 'HTML' },
  xml: { hl: 'xml', label: 'XML' },
  svg: { hl: 'xml', label: 'SVG' },
  yml: { hl: 'yaml', label: 'YAML' },
  yaml: { hl: 'yaml', label: 'YAML' },
  sh: { hl: 'bash', label: 'Shell' },
  bash: { hl: 'bash', label: 'Shell' },
  zsh: { hl: 'bash', label: 'Shell' },
  py: { hl: 'python', label: 'Python' },
  lua: { hl: 'lua', label: 'Lua' },
  c: { hl: 'c', label: 'C' },
  h: { hl: 'c', label: 'C Header' },
  hpp: { hl: 'cpp', label: 'C++ Header' },
  hxx: { hl: 'cpp', label: 'C++ Header' },
  cc: { hl: 'cpp', label: 'C++' },
  cxx: { hl: 'cpp', label: 'C++' },
  cpp: { hl: 'cpp', label: 'C++' },
  gradle: { hl: 'java', label: 'Gradle' },
  properties: { label: 'Properties' },
  toml: { hl: 'ini', label: 'TOML' },
  ini: { hl: 'ini', label: 'INI' },
  txt: { label: 'Plain Text' },
  bat: { label: 'Batch' },
  png: { label: 'PNG', binary: true },
  jpg: { label: 'JPEG', binary: true },
  jpeg: { label: 'JPEG', binary: true },
  gif: { label: 'GIF', binary: true },
  webp: { label: 'WebP', binary: true },
  ico: { label: 'Icon', binary: true },
  mp4: { label: 'MP4', binary: true },
  mp3: { label: 'MP3', binary: true },
  wav: { label: 'WAV', binary: true },
  ttf: { label: 'TrueType', binary: true },
  woff: { label: 'Font', binary: true },
  woff2: { label: 'Font', binary: true },
  jar: { label: 'JAR', binary: true },
  zip: { label: 'ZIP', binary: true },
  gz: { label: 'Gzip', binary: true },
  so: { label: 'Shared Object', binary: true },
  dll: { label: 'DLL', binary: true },
  class: { label: 'Class', binary: true },
  exe: { label: 'Executable', binary: true },
  pdf: { label: 'PDF', binary: true },
  lock: { label: 'Lock Text' },
}

const SPECIAL_FILES: Record<string, LangConf> = {
  Makefile: { hl: 'makefile', label: 'Makefile' },
  Dockerfile: { hl: 'bash', label: 'Dockerfile' },
  LICENSE: { label: 'Plain Text' },
  gradlew: { hl: 'bash', label: 'Shell' },
  '.gitignore': { label: 'Ignore List' },
  '.gitattributes': { label: 'Ignore List' },
}

const EXT_COLORS: Record<string, string> = {
  java: '#f89820',
  js: '#f7df1e',
  mjs: '#f7df1e',
  jsx: '#f7df1e',
  ts: '#3178c6',
  tsx: '#3178c6',
  json: '#cbcb41',
  md: '#519aba',
  css: '#519aba',
  scss: '#cd6799',
  html: '#e44d26',
  htm: '#e44d26',
  xml: '#e37933',
  svg: '#a074c4',
  yml: '#cb171e',
  yaml: '#cb171e',
  sh: '#89e051',
  bash: '#89e051',
  zsh: '#89e051',
  py: '#3572a5',
  lua: '#569cd6',
  c: '#f34b7d',
  h: '#f34b7d',
  hpp: '#f34b7d',
  hxx: '#f34b7d',
  cc: '#f34b7d',
  cxx: '#f34b7d',
  cpp: '#f34b7d',
  gradle: '#02303a',
  properties: '#c5c5c5',
  toml: '#c5c5c5',
  ini: '#c5c5c5',
  txt: '#c5c5c5',
  lock: '#c5c5c5',
  png: '#a074c4',
  jpg: '#a074c4',
  mp4: '#a074c4',
  ttf: '#a074c4',
}

function langFor(path: string): LangConf {
  const name = path.split('/').pop() || path
  if (SPECIAL_FILES[name]) return SPECIAL_FILES[name]
  const dot = name.lastIndexOf('.')
  const ext = dot > 0 ? name.slice(dot + 1).toLowerCase() : ''
  if (EXT_LANG[ext]) return EXT_LANG[ext]
  if (dot <= 0) return { label: 'Plain Text' }
  return { label: ext.toUpperCase() }
}

function extFor(path: string): string {
  const name = path.split('/').pop() || path
  const dot = name.lastIndexOf('.')
  return dot > 0 ? name.slice(dot + 1).toLowerCase() : ''
}

function buildTree(items: TreeItem[]): TreeNode[] {
  const root: TreeNode[] = []
  const nodes = new Map<string, TreeNode>()
  const ensure = (path: string, type: 'tree' | 'blob'): TreeNode => {
    let node = nodes.get(path)
    if (!node) {
      node = { name: path.split('/').pop() || path, path, type, children: type === 'tree' ? [] : undefined }
      nodes.set(path, node)
      const slash = path.lastIndexOf('/')
      const siblings = slash === -1 ? root : ensure(path.slice(0, slash), 'tree').children!
      siblings.push(node)
    }
    return node
  }
  for (const item of items) ensure(item.path, item.type === 'tree' ? 'tree' : 'blob')
  const sortRec = (list: TreeNode[]) => {
    list.sort((a, b) => (a.type === b.type ? a.name.localeCompare(b.name) : a.type === 'tree' ? -1 : 1))
    for (const node of list) if (node.children) sortRec(node.children)
  }
  sortRec(root)
  return root
}

function ancestorDirs(path: string): string[] {
  const parts = path.split('/')
  const dirs: string[] = []
  let acc = ''
  for (let i = 0; i < parts.length - 1; i++) {
    acc = acc ? acc + '/' + parts[i] : parts[i]
    dirs.push(acc)
  }
  return dirs
}

function escapeHtml(text: string) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

const BINARY_RE = /[\x00-\x08\x0E-\x1F]/

function IconChevron({ open }: { open: boolean }) {
  return (
    <svg viewBox="0 0 16 16" className={'w-3 h-3 shrink-0 transition-transform duration-100 ' + (open ? 'rotate-90' : '')} fill="currentColor" aria-hidden="true">
      <path d="M6 4l4 4-4 4z" />
    </svg>
  )
}

function IconFolder() {
  return (
    <svg viewBox="0 0 16 16" className="w-3.5 h-3.5 shrink-0 text-[#dcb67a]" fill="currentColor" aria-hidden="true">
      <path d="M1.5 4A1.5 1.5 0 0 1 3 2.5h3L7.5 4H13A1.5 1.5 0 0 1 14.5 5.5V12A1.5 1.5 0 0 1 13 13.5H3A1.5 1.5 0 0 1 1.5 12z" />
    </svg>
  )
}

function IconFile({ path }: { path: string }) {
  const color = EXT_COLORS[extFor(path)] || '#c5c5c5'
  return (
    <svg viewBox="0 0 16 16" className="w-3.5 h-3.5 shrink-0" fill="none" stroke={color} strokeWidth={1.2} aria-hidden="true">
      <path d="M4 1.5h5.5L12 4v10.5H4z" />
      <path d="M9.5 1.5V4H12" />
    </svg>
  )
}

function IconClose({ className = 'w-3.5 h-3.5' }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" className={className} fill="none" stroke="currentColor" strokeWidth={1.4} strokeLinecap="round" aria-hidden="true">
      <path d="M4 4l8 8M12 4l-8 8" />
    </svg>
  )
}

function IconExplorer() {
  return (
    <svg viewBox="0 0 24 24" className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth={1.4} aria-hidden="true">
      <path d="M7 4h6l4 4v9H7z" />
      <path d="M13 4v4h4" />
      <path d="M5 7v11a1 1 0 0 0 1 1h9" />
    </svg>
  )
}

function IconSearch() {
  return (
    <svg viewBox="0 0 24 24" className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth={1.6} aria-hidden="true">
      <circle cx="10.5" cy="10.5" r="6" />
      <path d="M15 15l5 5" strokeLinecap="round" />
    </svg>
  )
}

function IconSourceControl() {
  return (
    <svg viewBox="0 0 24 24" className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth={1.6} aria-hidden="true">
      <circle cx="7" cy="6" r="2.2" />
      <circle cx="7" cy="18" r="2.2" />
      <circle cx="17" cy="8" r="2.2" />
      <path d="M7 8.2v7.6" />
      <path d="M17 10.2c0 3.5-4 3.5-7 5" strokeLinecap="round" />
    </svg>
  )
}

function IconDebug() {
  return (
    <svg viewBox="0 0 24 24" className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth={1.6} aria-hidden="true">
      <path d="M8 5.5l10 6.5-10 6.5z" strokeLinejoin="round" />
    </svg>
  )
}

function IconExtensions() {
  return (
    <svg viewBox="0 0 24 24" className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth={1.5} aria-hidden="true">
      <rect x="4" y="4" width="7" height="7" />
      <rect x="13" y="4" width="7" height="7" />
      <rect x="4" y="13" width="7" height="7" />
      <path d="M13 16.5h7M16.5 13v7" strokeLinecap="round" />
    </svg>
  )
}

function IconBranch() {
  return (
    <svg viewBox="0 0 16 16" className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth={1.3} aria-hidden="true">
      <circle cx="5" cy="4" r="1.6" />
      <circle cx="5" cy="12" r="1.6" />
      <circle cx="11" cy="5.5" r="1.6" />
      <path d="M5 5.6v4.8" />
      <path d="M11 7.1c0 2.6-3 2.6-5.2 3.6" strokeLinecap="round" />
    </svg>
  )
}

function TreeRow({
  node,
  depth,
  expanded,
  activePath,
  onToggle,
  onOpen,
}: {
  node: TreeNode
  depth: number
  expanded: Record<string, boolean>
  activePath: string
  onToggle: (path: string) => void
  onOpen: (path: string) => void
}) {
  const isOpen = node.type === 'tree' && !!expanded[node.path]
  const isActive = node.type === 'blob' && node.path === activePath

  if (node.type === 'tree') {
    return (
      <div>
        <button
          onClick={() => onToggle(node.path)}
          className="w-full flex items-center gap-1.5 h-[22px] pr-2 text-[13px] text-[#cccccc] hover:bg-[#2a2d2e] transition-colors"
          style={{ paddingLeft: 8 + depth * 10 }}
        >
          <IconChevron open={isOpen} />
          <IconFolder />
          <span className="truncate">{node.name}</span>
        </button>
        {isOpen && node.children && (
          <div>
            {node.children.map((child) => (
              <TreeRow
                key={child.path}
                node={child}
                depth={depth + 1}
                expanded={expanded}
                activePath={activePath}
                onToggle={onToggle}
                onOpen={onOpen}
              />
            ))}
          </div>
        )}
      </div>
    )
  }

  return (
    <button
      onClick={() => onOpen(node.path)}
      className={'w-full flex items-center gap-1.5 h-[22px] pr-2 text-[13px] transition-colors ' + (isActive ? 'bg-[#37373d] text-white' : 'text-[#cccccc] hover:bg-[#2a2d2e]')}
      style={{ paddingLeft: 8 + depth * 10 }}
    >
      <span className="w-3 shrink-0" />
      <IconFile path={node.path} />
      <span className="truncate">{node.name}</span>
    </button>
  )
}

type FileStatus = 'loading' | 'ok' | 'binary' | 'error'

export default function CodeBrowser() {
  const [tree, setTree] = useState<TreeNode[] | null>(null)
  const [treeError, setTreeError] = useState(false)
  const [expanded, setExpanded] = useState<Record<string, boolean>>(() => {
    const init: Record<string, boolean> = {}
    for (const dir of ancestorDirs(DEFAULT_FILE)) init[dir] = true
    return init
  })
  const [tabs, setTabs] = useState<string[]>([DEFAULT_FILE])
  const [activeTab, setActiveTab] = useState(DEFAULT_FILE)
  const [files, setFiles] = useState<Record<string, FileStatus>>({})
  const [contents, setContents] = useState<Record<string, string>>({})
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [copied, setCopied] = useState(false)
  const [showWindowTitle, setShowWindowTitle] = useState(false)
  const titleBarRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = titleBarRef.current
    if (!el) return
    const update = () => setShowWindowTitle(el.clientWidth >= 1440)
    update()
    const observer = new ResizeObserver(update)
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  useEffect(() => {
    let cancelled = false
    async function loadTree() {
      setTreeError(false)
      try {
        const res = await fetch('https://api.github.com/repos/StormDevzz/RaveX/git/trees/main?recursive=1')
        if (!res.ok) throw new Error(String(res.status))
        const data = await res.json()
        if (!cancelled) setTree(buildTree(data.tree))
      } catch {
        if (!cancelled) setTreeError(true)
      }
    }
    void loadTree()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!activeTab) return
    if (files[activeTab]) return
    setFiles((prev) => ({ ...prev, [activeTab]: 'loading' }))
    fetch(RAW_BASE + activeTab)
      .then(async (res) => {
        if (!res.ok) throw new Error(String(res.status))
        const text = await res.text()
        if (langFor(activeTab).binary || BINARY_RE.test(text.slice(0, 4096))) {
          setFiles((prev) => ({ ...prev, [activeTab]: 'binary' }))
        } else {
          setContents((prev) => ({ ...prev, [activeTab]: text }))
          setFiles((prev) => ({ ...prev, [activeTab]: 'ok' }))
        }
      })
      .catch(() => {
        setFiles((prev) => ({ ...prev, [activeTab]: 'error' }))
      })
  }, [activeTab, files])

  function openFile(path: string) {
    setTabs((prev) => (prev.includes(path) ? prev : [...prev, path]))
    setActiveTab(path)
  }

  function closeTab(path: string, e: MouseEvent) {
    e.stopPropagation()
    setTabs((prev) => {
      const next = prev.filter((t) => t !== path)
      if (activeTab === path) {
        const idx = prev.indexOf(path)
        setActiveTab(next[Math.min(idx, next.length - 1)] || '')
      }
      return next
    })
  }

  function toggleDir(path: string) {
    setExpanded((prev) => ({ ...prev, [path]: !prev[path] }))
  }

  async function copyCode() {
    if (!activeTab || files[activeTab] !== 'ok') return
    try {
      await navigator.clipboard.writeText(contents[activeTab])
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1500)
    } catch {
      setCopied(false)
    }
  }

  const status = activeTab ? files[activeTab] : undefined
  const content = activeTab ? contents[activeTab] : undefined
  const langConf = activeTab ? langFor(activeTab) : null

  const { html, lineCount } = useMemo(() => {
    if (!content || status !== 'ok') return { html: '', lineCount: 0 }
    const lines = content.split('\n')
    if (!langConf || !langConf.hl) return { html: escapeHtml(content), lineCount: lines.length }
    try {
      const highlighted = hljs.highlight(content, { language: langConf.hl, ignoreIllegals: true })
      return { html: highlighted.value, lineCount: lines.length }
    } catch {
      return { html: escapeHtml(content), lineCount: lines.length }
    }
  }, [content, status, langConf])

  const crumbs = activeTab ? activeTab.split('/') : []

  return (
    <div className="h-full flex flex-col overflow-hidden text-[#cccccc]" style={{ fontFamily: "'Segoe UI', -apple-system, BlinkMacSystemFont, sans-serif" }}>
      <div ref={titleBarRef} className="h-9 shrink-0 flex items-center bg-[#3c3c3c] border-b border-[#2b2b2b] select-none relative overflow-hidden">
        <a href="/" className="flex items-center px-3 shrink-0" aria-label="RaveX home">
          <img src="/ravexv2.png" alt="RaveX" className="h-7 w-auto" />
        </a>
        <nav className="hidden md:flex items-center gap-4 px-2 text-[13px] text-[#cccccc] shrink-0" aria-hidden="true">
          <span>File</span>
          <span>Edit</span>
          <span>Selection</span>
          <span>View</span>
          <span>Go</span>
          <span>Run</span>
          <span>Terminal</span>
          <span>Help</span>
        </nav>
        {showWindowTitle && (
          <div className="absolute left-1/2 -translate-x-1/2 max-w-[26%] truncate text-[13px] text-[#cccccc] pointer-events-none">
            {(activeTab ? activeTab.split('/').pop() + ' - ' : '') + 'RaveX - Visual Studio Code'}
          </div>
        )}
      </div>

      <div className="flex flex-1 min-h-0">
        <div className="w-12 shrink-0 bg-[#333333] border-r border-[#2b2b2b] flex flex-col items-center pt-1">
          <button
            aria-label="Explorer"
            onClick={() => setSidebarOpen((v) => !v)}
            className={'relative w-12 h-12 grid place-items-center transition-colors ' + (sidebarOpen ? 'text-white' : 'text-[#858585] hover:text-white')}
          >
            {sidebarOpen && <span className="absolute left-0 top-0 h-full w-0.5 bg-white" />}
            <IconExplorer />
          </button>
          <span className="w-12 h-12 grid place-items-center text-[#858585]" aria-hidden="true"><IconSearch /></span>
          <span className="w-12 h-12 grid place-items-center text-[#858585]" aria-hidden="true"><IconSourceControl /></span>
          <span className="w-12 h-12 grid place-items-center text-[#858585]" aria-hidden="true"><IconDebug /></span>
          <span className="w-12 h-12 grid place-items-center text-[#858585]" aria-hidden="true"><IconExtensions /></span>
        </div>

        {sidebarOpen && (
          <aside className="w-60 shrink-0 bg-[#252526] border-r border-[#2b2b2b] flex flex-col min-h-0">
            <div className="h-9 shrink-0 flex items-center px-4 text-[11px] font-semibold tracking-wider text-[#bbbbbb]">
              EXPLORER
            </div>
            <div className="h-6 shrink-0 flex items-center gap-1 px-1 text-[11px] font-semibold uppercase text-[#cccccc]">
              <IconChevron open />
              RaveX
            </div>
            <div className="flex-1 overflow-y-auto overflow-x-hidden vs-scroll pb-4">
              {tree === null && !treeError && (
                <div className="px-4 py-2 text-[13px] text-[#858585]">Loading repository...</div>
              )}
              {treeError && (
                <div className="px-4 py-2 text-[13px]">
                  <p className="text-[#f48771] mb-2">Failed to load the file tree from GitHub.</p>
                  <p className="text-[#858585] mb-2">GitHub allows 60 API requests per hour per IP, give it a bit of time and try again.</p>
                  <button
                    onClick={() => window.location.reload()}
                    className="px-2 py-1 text-[12px] border border-[#3c3c3c] text-[#cccccc] hover:bg-[#2a2d2e] transition-colors"
                  >
                    Retry
                  </button>
                </div>
              )}
              {tree &&
                tree.map((node) => (
                  <TreeRow
                    key={node.path}
                    node={node}
                    depth={0}
                    expanded={expanded}
                    activePath={activeTab}
                    onToggle={toggleDir}
                    onOpen={openFile}
                  />
                ))}
            </div>
          </aside>
        )}

        <div className="flex-1 min-w-0 flex flex-col bg-[#1e1e1e]">
          <div className="h-9 shrink-0 flex items-stretch bg-[#252526] overflow-x-auto vs-scroll">
            {tabs.map((path) => {
              const name = path.split('/').pop() || path
              const isActive = path === activeTab
              return (
                <button
                  key={path}
                  onClick={() => setActiveTab(path)}
                  className={'group flex items-center gap-2 h-9 pl-3 pr-2 text-[13px] shrink-0 border-r border-[#252526] transition-colors ' + (isActive ? 'bg-[#1e1e1e] text-white' : 'bg-[#2d2d2e] text-[#969696] hover:text-[#cccccc]')}
                >
                  <IconFile path={path} />
                  <span>{name}</span>
                  <span
                    onClick={(e) => closeTab(path, e)}
                    className={'p-0.5 hover:bg-[#3c3c3c] transition-colors ' + (isActive ? 'opacity-100' : 'opacity-0 group-hover:opacity-100')}
                  >
                    <IconClose />
                  </span>
                </button>
              )
            })}
          </div>

          <div className="h-7 shrink-0 flex items-center justify-between px-4 text-[12px] text-[#969696]">
            <div className="flex items-center gap-1.5 truncate">
              {crumbs.map((part, i) => (
                <span key={i} className="flex items-center gap-1.5">
                  {i > 0 && <span className="text-[#6a6a6a]">&rsaquo;</span>}
                  <span className="hover:text-[#cccccc] transition-colors">{part}</span>
                </span>
              ))}
            </div>
            {status === 'ok' && (
              <button
                onClick={copyCode}
                className="ml-4 shrink-0 px-2 py-0.5 text-[11px] border border-[#3c3c3c] text-[#cccccc] hover:bg-[#2a2d2e] transition-colors"
              >
                {copied ? 'Copied!' : 'Copy'}
              </button>
            )}
          </div>

          <div className="flex-1 min-h-0 overflow-auto vs-scroll code-pane">
            {!activeTab && (
              <div className="h-full flex flex-col items-center justify-center gap-2 text-[#6a6a6a]">
                <span className="text-4xl font-light text-[#3f3f46]">RaveX</span>
                <span className="text-[13px]">Open a file from the explorer to start reading code.</span>
              </div>
            )}
            {activeTab && status === undefined && (
              <div className="px-6 py-4 text-[13px] text-[#6a6a6a]">Loading file...</div>
            )}
            {activeTab && status === 'loading' && (
              <div className="px-6 py-4 text-[13px] text-[#6a6a6a]">Loading {activeTab.split('/').pop()} from GitHub...</div>
            )}
            {activeTab && status === 'error' && (
              <div className="px-6 py-4 text-[13px] text-[#f48771]">Failed to load this file from GitHub.</div>
            )}
            {activeTab && status === 'binary' && (
              <div className="px-6 py-4 text-[13px] text-[#6a6a6a]">Binary file, preview is not available.</div>
            )}
            {activeTab && status === 'ok' && (
              <div className="flex min-h-full w-max">
                <div className="sticky left-0 z-10 select-none text-right bg-[#1e1e1e] text-[#858585] pl-4 pr-5 py-3 text-[13px]" style={{ lineHeight: '22px', fontFamily: "Consolas, 'Courier New', monospace" }}>
                  {Array.from({ length: lineCount }, (_, i) => (
                    <div key={i}>{i + 1}</div>
                  ))}
                </div>
                <pre className="m-0 py-3 pr-8 text-[13px] bg-transparent" style={{ lineHeight: '22px', tabSize: 4, fontFamily: "Consolas, 'Courier New', monospace" }}>
                  <code className="hljs" dangerouslySetInnerHTML={{ __html: html }} />
                </pre>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="h-6 shrink-0 flex items-center justify-between px-3 text-[12px] text-white bg-[#007acc] select-none">
        <div className="flex items-center gap-4">
          <span className="flex items-center gap-1.5">
            <IconBranch />
            main
          </span>
          <span className="hidden sm:flex items-center gap-1.5">
            <IconClose className="w-3 h-3" />
            0
            <span className="ml-1 inline-block w-0 h-0 border-l-[5px] border-l-transparent border-r-[5px] border-r-transparent border-b-[6px] border-b-white align-middle" />
            0
          </span>
        </div>
        <div className="flex items-center gap-4">
          {langConf && <span>{langConf.label}</span>}
          <span className="hidden sm:inline">UTF-8</span>
          <span className="hidden sm:inline">LF</span>
          <span className="hidden md:inline">Spaces: 4</span>
        </div>
      </div>
    </div>
  )
}
