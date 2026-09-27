'use client'

import { useEffect, useRef, useState, type MouseEvent } from 'react'
import CodeBrowser from './code/CodeBrowser'

const telegramUrl = 'https://t.me/ravex_free'
const discordUrl = 'https://discord.gg/n9HPbgN7S'
const githubUrl = 'https://github.com/StormDevzz/RaveX'
const jarVersion = process.env.RAVEX_JAR_VERSION || 'unknown'
const jarUrl = 'https://github.com/StormDevzz/RaveX/releases/latest/download/ravex-' + jarVersion + '.jar'

const version = process.env.RAVEX_VERSION || 'unknown'

type Lang = 'ru' | 'en'

const screenshots = [
  { src: '/screenshots/targetesp.png', en: 'New target ESP for 1.4.9', ru: 'Новый target ESP в 1.4.9' },
  { src: '/screenshots/loading.png', en: 'Loading', ru: 'Загрузка' },
  { src: '/screenshots/gui.png', en: 'ClickGUI', ru: 'ClickGUI' },
  { src: '/screenshots/physics.png', en: 'Physics', ru: 'Физика' },
  { src: '/screenshots/newgui.png', en: 'New GUI', ru: 'Новый GUI' },
  { src: '/screenshots/russian.png', en: 'Russian', ru: 'Русский' },
]

const community = [
  { key: 'discord', href: discordUrl, color: '#5865f2', icon: <IconDiscord /> },
  { key: 'telegram', href: telegramUrl, color: '#229ed9', icon: <IconTelegram /> },
  { key: 'github', href: githubUrl, color: '#c8c8d0', icon: <IconGithub /> },
] as const

function IconGithub() {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4" aria-hidden="true">
      <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12" />
    </svg>
  )
}

function IconDiscord() {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4" aria-hidden="true">
      <path d="M20.317 4.3698a19.7913 19.7913 0 00-4.8851-1.5152.0741.0741 0 00-.0785.0371c-.211.3753-.4447.8648-.6083 1.2495-1.8447-.2762-3.68-.2762-5.4868 0-.1636-.3933-.4058-.8742-.6177-1.2495a.077.077 0 00-.0785-.037 19.7363 19.7363 0 00-4.8852 1.515.0699.0699 0 00-.0321.0277C.5334 9.0458-.319 13.5799.0992 18.0578a.0824.0824 0 00.0312.0561c2.0528 1.5076 4.0413 2.4228 5.9929 3.0294a.0777.0777 0 00.0842-.0276c.4616-.6304.8731-1.2952 1.226-1.9942a.076.076 0 00-.0416-.1057c-.6528-.2476-1.2743-.5495-1.8722-.8923a.077.077 0 01-.0076-.1277c.1258-.0943.2517-.1923.3718-.2914a.0743.0743 0 01.0776-.0105c3.9278 1.7933 8.18 1.7933 12.0614 0a.0739.0739 0 01.0785.0095c.1202.099.246.1981.3728.2924a.077.077 0 01-.0066.1276 12.2986 12.2986 0 01-1.873.8914.0766.0766 0 00-.0407.1067c.3604.698.7719 1.3628 1.225 1.9932a.076.076 0 00.0842.0286c1.961-.6067 3.9495-1.5219 6.0023-3.0294a.077.077 0 00.0313-.0552c.5004-5.177-.8382-9.6739-3.5485-13.6604a.061.061 0 00-.0312-.0286zM8.02 15.3312c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9555-2.4189 2.157-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.9555 2.4189-2.1569 2.4189zm7.9748 0c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9554-2.4189 2.1569-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.946 2.4189-2.1568 2.4189Z" />
    </svg>
  )
}

function IconTelegram() {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4" aria-hidden="true">
      <path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z" />
    </svg>
  )
}

function IconJava() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" className="w-3.5 h-3.5" aria-hidden="true">
      <path d="M5 9h11v5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5V9z" />
      <path d="M16 10h1.5a2.5 2.5 0 0 1 0 5H16" />
      <path d="M8.5 3.5c.7.9.7 1.9 0 2.8" />
      <path d="M12 3.5c.7.9.7 1.9 0 2.8" />
    </svg>
  )
}

function IconCpp() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" className="w-3.5 h-3.5" aria-hidden="true">
      <path d="M9 6 4 12l5 6" />
      <path d="M15 6l5 6-5 6" />
    </svg>
  )
}

function IconBlock() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" className="w-3.5 h-3.5" aria-hidden="true">
      <path d="M12 3.5 19.5 8v8L12 20.5 4.5 16V8L12 3.5z" />
      <path d="M4.5 8 12 12.5 19.5 8" />
      <path d="M12 12.5v8" />
    </svg>
  )
}

function HoverCard({ children, className = '', onClick }: { children: React.ReactNode; className?: string; onClick?: () => void }) {
  const ref = useRef<HTMLDivElement>(null)

  function handleMouse(e: MouseEvent<HTMLDivElement>) {
    const el = ref.current
    if (!el) return
    const rect = el.getBoundingClientRect()
    el.style.setProperty('--mx', String(e.clientX - rect.left))
    el.style.setProperty('--my', String(e.clientY - rect.top))
  }

  return (
    <div
      ref={ref}
      onMouseMove={handleMouse}
      onClick={onClick}
      className={'hover-card ' + className}
    >
      {children}
    </div>
  )
}

function pluralRu(n: number, one: string, few: string, many: string) {
  const mod10 = n % 10
  const mod100 = n % 100
  if (mod10 === 1 && mod100 !== 11) return one
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few
  return many
}

const EN = {
  nav: { screenshots: 'Screenshots', about: 'About', code: 'Code', download: 'Download' },
  hero: {
    tagline: 'A free and open-source utility client for Minecraft on Fabric.',
    sub: 'Written in Java and C++. Pick your language, set up your modules and just play. Everything is on GitHub if you want to check it yourself.',
    download: 'Download latest',
    source: 'View source',
  },
  shots: {
    title: 'Screenshots',
    sub: (n: number) => n + ' images, click to open fullscreen',
  },
  code: {
    title: 'Source code',
    sub: 'Straight from GitHub, always up to date, read only. The file tree loads through the GitHub API - 60 requests per hour per IP, so if it ever stops loading, just hit Retry later',
    fullscreen: 'Open full screen',
  },
  about: {
    title: 'About',
    p1: 'RaveX is a Minecraft utility client built on Fabric. Combat, movement, render, HUD, macros, the usual stuff, plus a ClickGUI you can rearrange however you like.',
    p2: 'You can write your own addons in Java or C++. And if you ever wonder what is inside the jar, build it from source and compare the sha-256 with the official release.',
    links: {
      discord: { label: 'Discord', desc: 'Come chat with us' },
      telegram: { label: 'Telegram', desc: 'News and updates' },
      github: { label: 'GitHub', desc: 'Source code and releases' },
    },
    stats: { stars: 'Stars', forks: 'Forks', issues: 'Open issues', lastCommit: 'Last commit' },
  },
  install: {
    title: 'How I install this client?',
    sub: 'Four steps, about a minute. Fabric API is not required',
    steps: [
      {
        title: 'Install Fabric Loader',
        body: 'Grab Fabric Loader 0.16.10 or newer for Minecraft 1.21.11 from fabricmc.net. The vanilla launcher and its bundled Java 21 are enough',
      },
      {
        title: 'Download the RaveX jar',
        body: 'The button below pulls the latest release straight from GitHub.',
        link: { href: jarUrl, label: 'Download RaveX jar' },
      },
      {
        title: 'Drop the jar into the mods folder',
        body: 'Close the launcher first, then move the jar into your mods folder: %appdata%\\.minecraft\\mods on Windows, ~/.minecraft/mods on Linux and macOS. No other mods are required',
      },
      {
        title: 'Start the Fabric profile',
        body: 'Launch the Fabric profile, join a world or a server and open the ClickGUI. Bind any module with a middle click',
      },
    ],
  },
  footer: {
    note: 'Community site for RaveX. Not official.',
    author: 'Made by sh2-u34r',
    projectSince: (y: number) => 'RaveX - a project since ' + y,
    repoCreated: 'Repository created',
    today: 'Today',
    age: 'Age:',
  },
}

const RU: typeof EN = {
  nav: { screenshots: 'Скриншоты', about: 'О проекте', code: 'Код', download: 'Скачать' },
  hero: {
    tagline: 'Бесплатный клиент-утилита для Minecraft на Fabric с открытым кодом.',
    sub: 'Написан на Java и C++. Выбирай язык, настраивай модули и просто играй. Всё лежит на GitHub, если хочешь проверить сам.',
    download: 'Скачать последнюю',
    source: 'Смотреть исходники',
  },
  shots: {
    title: 'Скриншоты',
    sub: (n: number) => n + ' ' + pluralRu(n, 'изображение', 'изображения', 'изображений') + ', кликни для полноэкранного просмотра',
  },
  code: {
    title: 'Исходный код',
    sub: 'Прямо с GitHub, всегда актуально, только чтение. Дерево файлов грузится через GitHub API - 60 запросов в час на IP, если перестало грузиться, просто нажми Retry позже',
    fullscreen: 'Открыть на весь экран',
  },
  about: {
    title: 'О проекте',
    p1: 'RaveX - это Minecraft клиент-утилита на Fabric. Боёвка, движение, рендер, HUD, макросы - всё как обычно, плюс ClickGUI, который можно перестроить как угодно.',
    p2: 'Можно писать свои аддоны на Java или C++. А если хочется знать, что внутри jar - собери из исходников и сравни sha-256 с официальным релизом.',
    links: {
      discord: { label: 'Discord', desc: 'Приходи общаться' },
      telegram: { label: 'Telegram', desc: 'Новости и обновления' },
      github: { label: 'GitHub', desc: 'Исходники и релизы' },
    },
    stats: { stars: 'Звёзды', forks: 'Форки', issues: 'Открытые issues', lastCommit: 'Последний коммит' },
  },
  install: {
    title: 'Как установить клиент?',
    sub: 'Четыре шага, около минуты. Fabric API не нужен',
    steps: [
      {
        title: 'Установи Fabric Loader',
        body: 'Скачай Fabric Loader 0.16.10 или новее для Minecraft 1.21.11 с fabricmc.net. Ванильного лаунчера и встроенного Java 21 достаточно',
      },
      {
        title: 'Скачай jar RaveX',
        body: 'Кнопка ниже скачивает последний релиз прямо с GitHub.',
        link: { href: jarUrl, label: 'Скачать RaveX jar' },
      },
      {
        title: 'Положи jar в папку mods',
        body: 'Сначала закрой лаунчер, потом перенеси jar в папку модов: %appdata%\\.minecraft\\mods на Windows, ~/.minecraft/mods на Linux и macOS. Другие моды не нужны',
      },
      {
        title: 'Запусти Fabric-профиль',
        body: 'Запусти Fabric-профиль, зайди в мир или на сервер и открой ClickGUI. Любой модуль привязывается средней кнопкой мыши',
      },
    ],
  },
  footer: {
    note: 'Фанатский сайт о RaveX. Не официальный.',
    author: 'Сделал sh2-u34r',
    projectSince: (y: number) => 'RaveX - проект с ' + y + ' года',
    repoCreated: 'Репозиторий создан',
    today: 'Сегодня',
    age: 'Возраст:',
  },
}

const STRINGS: Record<Lang, typeof EN> = { en: EN, ru: RU }

const TYPED_WORD = 'RaveX'

type RepoInfo = {
  created_at: string
  stargazers_count: number
  forks_count: number
  open_issues_count: number
  pushed_at: string
}

const REPO_FALLBACK: RepoInfo = {
  created_at: '2026-05-24T10:56:28Z',
  stargazers_count: 20,
  forks_count: 4,
  open_issues_count: 13,
  pushed_at: '2026-09-27T12:07:36Z',
}

function TypedTitle() {
  const [text, setText] = useState(TYPED_WORD)

  useEffect(() => {
    let cancelled = false
    let timer: ReturnType<typeof setTimeout>
    let len = TYPED_WORD.length
    const delays = { hold: 2300, del: 75, gap: 650, type: 145 } as const
    let mode: keyof typeof delays = 'hold'

    function run() {
      if (cancelled) return
      const current = mode
      if (current === 'hold') {
        mode = 'del'
      } else if (current === 'del') {
        len = Math.max(0, len - 1)
        setText(TYPED_WORD.slice(0, len))
        mode = len === 0 ? 'gap' : 'del'
      } else if (current === 'gap') {
        mode = 'type'
      } else {
        len = Math.min(TYPED_WORD.length, len + 1)
        setText(TYPED_WORD.slice(0, len))
        mode = len === TYPED_WORD.length ? 'hold' : 'type'
      }
      timer = setTimeout(run, delays[mode])
    }

    timer = setTimeout(run, delays.hold)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [])

  return (
    <span>
      {text.split('').map((ch, i) => (
        <span key={i} className={'typed-letter' + (i === 4 ? ' text-[#38bdf8]' : '')}>
          {ch}
        </span>
      ))}
      <span className="typed-caret" aria-hidden="true" />
    </span>
  )
}

function LangSwitch({ lang, onChange }: { lang: Lang; onChange: (l: Lang) => void }) {
  const options: { id: Lang; flag: string; alt: string; label: string }[] = [
    { id: 'en', flag: '/flags/us.png', alt: 'English', label: 'EN' },
    { id: 'ru', flag: '/flags/ru.png', alt: 'Русский', label: 'RU' },
  ]
  return (
    <div className="flex items-center border border-[#1a1a2e] rounded-lg overflow-hidden shrink-0">
      {options.map((o) => (
        <button
          key={o.id}
          type="button"
          onClick={() => onChange(o.id)}
          aria-label={o.alt}
          aria-pressed={lang === o.id}
          className={
            'flex items-center gap-1.5 px-2 py-1.5 transition-colors ' +
            (lang === o.id ? 'bg-[#38bdf8]' : 'hover:bg-[#0d0d1a]')
          }
        >
          <img src={o.flag} alt="" width={16} height={12} className="w-4 h-3 object-contain rounded-[2px]" />
          <span className={'text-[11px] font-semibold ' + (lang === o.id ? 'text-[#06060e]' : 'text-[#a0a0c8]')}>
            {o.label}
          </span>
        </button>
      ))}
    </div>
  )
}

function formatDate(d: Date, lang: Lang) {
  return d.toLocaleDateString(lang === 'ru' ? 'ru-RU' : 'en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

function elapsedText(from: Date, to: Date, lang: Lang) {
  let years = to.getFullYear() - from.getFullYear()
  let months = to.getMonth() - from.getMonth()
  let days = to.getDate() - from.getDate()
  if (days < 0) {
    months -= 1
    days += new Date(to.getFullYear(), to.getMonth(), 0).getDate()
  }
  if (months < 0) {
    years -= 1
    months += 12
  }
  const parts: string[] = []
  if (lang === 'ru') {
    if (years > 0) parts.push(years + ' ' + pluralRu(years, 'год', 'года', 'лет'))
    if (months > 0) parts.push(months + ' ' + pluralRu(months, 'месяц', 'месяца', 'месяцев'))
    parts.push(days + ' ' + pluralRu(days, 'день', 'дня', 'дней'))
  } else {
    if (years > 0) parts.push(years + (years === 1 ? ' year' : ' years'))
    if (months > 0) parts.push(months + (months === 1 ? ' month' : ' months'))
    parts.push(days + (days === 1 ? ' day' : ' days'))
  }
  return parts.join(', ')
}

function ProjectStats({ repo, lang }: { repo: RepoInfo; lang: Lang }) {
  const t = STRINGS[lang]
  const from = new Date(repo.created_at)
  const now = new Date()

  return (
    <div className="flex flex-wrap items-center justify-center gap-x-3 gap-y-1 text-center text-xs">
      <span className="text-[#38bdf8] font-semibold">{t.footer.projectSince(from.getFullYear())}</span>
      <span className="text-[#484870]">|</span>
      <span className="text-[#a0a0c8]">{t.footer.repoCreated} {formatDate(from, lang)}</span>
      <span className="text-[#484870]">|</span>
      <span className="text-[#a0a0c8]">{t.footer.today} {formatDate(now, lang)}</span>
      <span className="text-[#484870]">|</span>
      <span className="text-[#a0a0c8]">{t.footer.age} {elapsedText(from, now, lang)}</span>
    </div>
  )
}

export default function Home() {
  const [active, setActive] = useState<number | null>(null)
  const [repo, setRepo] = useState<RepoInfo | null>(null)
  const [lang, setLang] = useState<Lang>('ru')
  const t = STRINGS[lang]

  useEffect(() => {
    const saved = localStorage.getItem('ravex-lang')
    if (saved === 'en' || saved === 'ru') setLang(saved)
  }, [])

  function switchLang(next: Lang) {
    setLang(next)
    localStorage.setItem('ravex-lang', next)
  }

  useEffect(() => {
    setRepo(REPO_FALLBACK)
    fetch('https://api.github.com/repos/StormDevzz/RaveX')
      .then((r) => (r.ok ? r.json() : Promise.reject()))
      .then((d) => {
        const live: RepoInfo = { ...REPO_FALLBACK }
        if (typeof d.created_at === 'string' && !isNaN(new Date(d.created_at).getTime())) live.created_at = d.created_at
        if (typeof d.pushed_at === 'string' && !isNaN(new Date(d.pushed_at).getTime())) live.pushed_at = d.pushed_at
        if (typeof d.stargazers_count === 'number') live.stargazers_count = d.stargazers_count
        if (typeof d.forks_count === 'number') live.forks_count = d.forks_count
        if (typeof d.open_issues_count === 'number') live.open_issues_count = d.open_issues_count
        setRepo(live)
      })
      .catch(() => {})
  }, [])

  useEffect(() => {
    if (active === null) return
    document.body.style.overflow = 'hidden'
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setActive(null)
      else if (e.key === 'ArrowRight') setActive((i) => (i === null ? i : (i + 1) % screenshots.length))
      else if (e.key === 'ArrowLeft') setActive((i) => (i === null ? i : (i - 1 + screenshots.length) % screenshots.length))
    }
    window.addEventListener('keydown', onKey)
    return () => {
      document.body.style.overflow = ''
      window.removeEventListener('keydown', onKey)
    }
  }, [active])

  return (
    <div className="min-h-screen flex flex-col overflow-x-hidden">
      <header className="sticky top-0 z-50 border-b border-[#12121e] bg-[#06060e]/90 backdrop-blur-sm">
        <div className="max-w-5xl mx-auto px-5 h-12 flex items-center justify-between">
          <a href="/" className="flex items-center shrink-0" aria-label="RaveX home">
            <img src="/ravexv2.png" alt="RaveX" className="h-8 w-auto" />
          </a>
          <nav className="flex items-center gap-3 md:gap-5 text-xs">
            <a href="#screenshots" className="hidden sm:inline-block text-[#7878a0] hover:text-white transition-colors">
              {t.nav.screenshots}
            </a>
            <a href="#about" className="hidden sm:inline-block text-[#7878a0] hover:text-white transition-colors">
              {t.nav.about}
            </a>
            <a href="#code" className="hidden sm:inline-block text-[#7878a0] hover:text-white transition-colors">
              {t.nav.code}
            </a>
            <LangSwitch lang={lang} onChange={switchLang} />
            <a
              href={jarUrl}
              className="px-4 py-1.5 rounded-lg bg-[#38bdf8] text-[#06060e] text-xs font-semibold hover:bg-[#7dd3fc] active:scale-95 transition-all"
            >
              {t.nav.download}
            </a>
          </nav>
        </div>
      </header>

      <main className="flex-1">
        <section className="relative pt-24 pb-20 md:pt-32 md:pb-28">
          <div className="absolute -top-40 left-1/2 -translate-x-1/2 w-[640px] h-[640px] rounded-full bg-[#38bdf8] opacity-[0.06] blur-[130px] pointer-events-none" />
          <div className="max-w-5xl mx-auto px-5 relative">
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-md bg-[#0d0d1a] border border-[#1a1a2e] text-[#38bdf8] text-xs tracking-wider mb-6">
              V{version}
            </div>
            <div className="flex items-center gap-4 md:gap-6 mb-5">
              <img
                src="/ravexv2.png"
                alt="RaveX logo"
                width={1220}
                height={1396}
                className="h-14 md:h-20 w-auto shrink-0"
              />
              <h1 className="text-[clamp(2.5rem,10vw,5rem)] font-bold leading-[1.05] tracking-tight text-white">
                <TypedTitle />
              </h1>
            </div>
            <p className="text-[#9a9ab8] text-base md:text-lg max-w-xl leading-relaxed mb-3">
              {t.hero.tagline}
            </p>
            <p className="text-[#7878a0] text-sm md:text-base max-w-xl leading-relaxed mb-9">
              {t.hero.sub}
            </p>
            <div className="flex flex-wrap gap-3">
              <a
                href={jarUrl}
                className="px-6 py-2.5 rounded-lg bg-[#38bdf8] text-[#06060e] text-sm font-semibold hover:bg-[#7dd3fc] active:scale-95 transition-all"
              >
                {t.hero.download}
              </a>
              <a
                href={githubUrl}
                target="_blank"
                rel="noreferrer"
                className="px-6 py-2.5 rounded-lg border border-[#1a1a2e] text-[#c8c8d0] text-sm font-semibold hover:bg-[#0d0d1a] hover:text-white active:scale-95 transition-all"
              >
                {t.hero.source}
              </a>
            </div>
          </div>
        </section>

        <section id="screenshots" className="py-16 md:py-24 border-t border-[#12121e]">
          <div className="max-w-5xl mx-auto px-5">
            <div className="mb-10">
              <h2 className="text-white text-sm font-semibold tracking-widest mb-2">
                {t.shots.title}
              </h2>
              <p className="text-[#a0a0c8] text-xs">
                {t.shots.sub(screenshots.length)}
              </p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {screenshots.map((s, i) => (
                <HoverCard
                  key={s.src}
                  className="border border-[#12121e] bg-[#0a0a14] rounded-lg cursor-zoom-in"
                  onClick={() => setActive(i)}
                >
                  <img
                    src={s.src}
                    alt={s.en}
                    width={1600}
                    height={900}
                    className="w-full h-44 object-cover object-top"
                  />
                  <div className="relative z-10 px-3 py-2.5 border-t border-[#12121e]">
                    <span className="text-xs text-[#a0a0c8]">{s[lang]}</span>
                  </div>
                </HoverCard>
              ))}
            </div>
          </div>
        </section>

        <section id="code" className="py-16 md:py-24 border-t border-[#12121e]">
          <div className="max-w-5xl mx-auto px-5">
            <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
              <div>
                <h2 className="text-white text-sm font-semibold tracking-widest mb-2">
                  {t.code.title}
                </h2>
                <p className="text-[#a0a0c8] text-xs">
                  {t.code.sub}
                </p>
              </div>
              <a
                href="/code"
                className="px-4 py-1.5 rounded-lg border border-[#1a1a2e] text-[#c8c8d0] text-xs font-semibold hover:bg-[#0d0d1a] hover:text-white active:scale-95 transition-all"
              >
                {t.code.fullscreen}
              </a>
            </div>
            <div className="h-[70vh] min-h-[520px] border border-[#12121e] overflow-hidden rounded-lg">
              <CodeBrowser />
            </div>
          </div>
        </section>

        <section id="about" className="py-16 md:py-24 border-t border-[#12121e]">
          <div className="max-w-5xl mx-auto px-5">
            <div className="grid md:grid-cols-2 gap-12">
              <div>
                <h2 className="text-white text-sm font-semibold tracking-widest mb-4">
                  {t.about.title}
                </h2>
                <p className="text-[#9a9ab8] text-sm leading-relaxed mb-4">
                  {t.about.p1}
                </p>
                <p className="text-[#9a9ab8] text-sm leading-relaxed mb-6">
                  {t.about.p2}
                </p>
                <div className="flex flex-wrap gap-2">
                  {[
                    { icon: <IconJava />, label: 'Java' },
                    { icon: <IconCpp />, label: 'C++' },
                    { icon: <IconBlock />, label: 'Fabric 1.21.x' },
                  ].map((chip) => (
                    <span
                      key={chip.label}
                      className="inline-flex items-center gap-1.5 px-2 py-1 text-xs rounded-md bg-[#0d0d1a] text-[#38bdf8] border border-[#1a1a2e]"
                    >
                      {chip.icon}
                      {chip.label}
                    </span>
                  ))}
                </div>
              </div>
              <div className="flex flex-col gap-3">
                {community.map((link) => (
                  <a
                    key={link.key}
                    href={link.href}
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center gap-3 px-4 py-3 rounded-lg border border-[#12121e] bg-[#0a0a14] hover:bg-[#0d0d1a] hover:border-[#1a1a2e] active:scale-[0.98] transition-all group"
                  >
                    <span
                      className="w-8 h-8 shrink-0 flex items-center justify-center rounded-md border border-[#1a1a2e] bg-[#0d0d1a]"
                      style={{ color: link.color }}
                    >
                      {link.icon}
                    </span>
                    <div>
                      <div className="text-sm text-[#c8c8d0] group-hover:text-white transition-colors">
                        {t.about.links[link.key].label}
                      </div>
                      <div className="text-xs text-[#a0a0c8]">{t.about.links[link.key].desc}</div>
                    </div>
                  </a>
                ))}
              </div>
            </div>
            {repo && (
              <div className="mt-12 grid grid-cols-2 md:grid-cols-4 gap-4">
                {[
                  { label: t.about.stats.stars, value: String(repo.stargazers_count) },
                  { label: t.about.stats.forks, value: String(repo.forks_count) },
                  { label: t.about.stats.issues, value: String(repo.open_issues_count) },
                  { label: t.about.stats.lastCommit, value: formatDate(new Date(repo.pushed_at), lang) },
                ].map((s) => (
                  <div key={s.label} className="border border-[#12121e] bg-[#0a0a14] rounded-lg px-4 py-4">
                    <div className="text-white text-xl font-semibold mb-1">{s.value}</div>
                    <div className="text-xs text-[#a0a0c8]">{s.label}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>

        <section id="install" className="py-16 md:py-24 border-t border-[#12121e]">
          <div className="max-w-5xl mx-auto px-5">
            <div className="mb-10">
              <h2 className="text-white text-sm font-semibold tracking-widest mb-2">
                {t.install.title}
              </h2>
              <p className="text-[#a0a0c8] text-xs">
                {t.install.sub}
              </p>
            </div>
            <ol className="flex flex-col gap-4">
              {t.install.steps.map((step, i) => (
                <li
                  key={i}
                  className="flex gap-4 px-4 py-4 rounded-lg border border-[#12121e] bg-[#0a0a14]"
                >
                  <span className="shrink-0 w-8 h-8 flex items-center justify-center rounded-md border border-[#1a1a2e] bg-[#0d0d1a] text-[#38bdf8] text-sm font-semibold">
                    {i + 1}
                  </span>
                  <div>
                    <div className="text-sm text-white font-semibold mb-1">{step.title}</div>
                    <p className="text-xs text-[#a0a0c8] leading-relaxed">{step.body}</p>
                    {step.link && (
                      <a
                        href={step.link.href}
                        className="inline-block mt-3 px-4 py-1.5 rounded-lg bg-[#38bdf8] text-[#06060e] text-xs font-semibold hover:bg-[#7dd3fc] active:scale-95 transition-all"
                      >
                        {step.link.label}
                      </a>
                    )}
                  </div>
                </li>
              ))}
            </ol>
          </div>
        </section>
      </main>

      <footer className="border-t border-[#12121e] py-8">
        <div className="max-w-5xl mx-auto px-5 flex flex-col gap-4">
          <div className="flex flex-col md:flex-row items-center justify-between gap-3">
            <p className="text-xs text-[#a0a0c8]">
              {t.footer.note}
            </p>
            <p className="text-xs text-[#a0a0c8]">
              {t.footer.author}
            </p>
          </div>
          {repo && <ProjectStats repo={repo} lang={lang} />}
        </div>
      </footer>

      {active !== null && (
        <div
          className="fixed inset-0 z-[100] bg-black/90 backdrop-blur-sm flex items-center justify-center p-4"
          onClick={() => setActive(null)}
        >
          <button
            aria-label="Close"
            className="absolute top-4 right-4 w-10 h-10 flex items-center justify-center rounded-lg border border-[#1a1a2e] bg-[#0d0d1a]/80 text-[#c8c8d0] hover:text-white hover:bg-[#12121e] active:scale-95 transition-all"
            onClick={() => setActive(null)}
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" className="w-4 h-4" aria-hidden="true">
              <path d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>
          <button
            aria-label="Previous screenshot"
            className="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 flex items-center justify-center rounded-lg border border-[#1a1a2e] bg-[#0d0d1a]/80 text-[#c8c8d0] hover:text-white hover:bg-[#12121e] active:scale-95 transition-all"
            onClick={(e) => {
              e.stopPropagation()
              setActive((active - 1 + screenshots.length) % screenshots.length)
            }}
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" className="w-4 h-4" aria-hidden="true">
              <path d="M15 18l-6-6 6-6" />
            </svg>
          </button>
          <button
            aria-label="Next screenshot"
            className="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 flex items-center justify-center rounded-lg border border-[#1a1a2e] bg-[#0d0d1a]/80 text-[#c8c8d0] hover:text-white hover:bg-[#12121e] active:scale-95 transition-all"
            onClick={(e) => {
              e.stopPropagation()
              setActive((active + 1) % screenshots.length)
            }}
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" className="w-4 h-4" aria-hidden="true">
              <path d="M9 6l6 6-6 6" />
            </svg>
          </button>
          <figure
            className="max-w-[92vw] flex flex-col items-center gap-3"
            onClick={(e) => e.stopPropagation()}
          >
            <img
              src={screenshots[active].src}
              alt={screenshots[active].en}
              className="max-w-full max-h-[78vh] object-contain rounded-lg border border-[#1a1a2e]"
            />
            <figcaption className="text-xs text-[#9a9ab8]">
              {screenshots[active][lang]} · {active + 1} / {screenshots.length}
            </figcaption>
          </figure>
        </div>
      )}
    </div>
  )
}
