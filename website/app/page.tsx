'use client'

import { useRef, type MouseEvent } from 'react'

const telegramUrl = 'https://t.me/ravex_free'
const discordUrl = 'https://discord.gg/n9HPbgN7S'
const githubUrl = 'https://github.com/StormDevzz/RaveX'
const officialSite = 'https://ravex.serveousercontent.com/'
const releasesUrl = 'https://github.com/StormDevzz/RaveX/releases'

const version = process.env.RAVEX_VERSION || 'unknown'

const screenshots = [
  { src: '/screenshots/loading.png', label: 'loading' },
  { src: '/screenshots/gui.png', label: 'clickgui' },
  { src: '/screenshots/physics.png', label: 'physics' },
  { src: '/screenshots/newgui.png', label: 'new gui' },
  { src: '/screenshots/russian.png', label: 'russian lang' },
]

function HoverCard({ children, className = '' }: { children: React.ReactNode; className?: string }) {
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
      className={'hover-card ' + className}
    >
      {children}
    </div>
  )
}

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="sticky top-0 z-50 border-b border-[#12121e] bg-[#06060e]/90 backdrop-blur-sm">
        <div className="max-w-5xl mx-auto px-5 h-12 flex items-center justify-between">
          <span className="text-sm font-semibold tracking-widest uppercase text-[#38bdf8]">
            ravex
          </span>
          <nav className="flex items-center gap-5 text-xs">
            <a href="#screenshots" className="text-[#7878a0] hover:text-[#c8c8d0] transition-colors">
              screenshots
            </a>
            <a href="#about" className="text-[#7878a0] hover:text-[#c8c8d0] transition-colors">
              about
            </a>
            <a
              href={releasesUrl}
              target="_blank"
              className="px-4 py-1.5 bg-[#38bdf8] text-[#06060e] text-xs font-semibold hover:bg-[#7dd3fc] transition-colors"
            >
              download
            </a>
          </nav>
        </div>
      </header>

      <main className="flex-1">
        <section className="pt-28 pb-20 md:pt-36 md:pb-28">
          <div className="max-w-5xl mx-auto px-5">
            <div className="inline-flex items-center gap-2 px-2.5 py-1 bg-[#0d0d1a] border border-[#1a1a2e] text-[#38bdf8] text-xs tracking-wider mb-6">
              v{version}
            </div>
            <h1 className="text-[clamp(2.5rem,10vw,5rem)] font-bold leading-[1.05] tracking-tight text-white mb-4">
              ravex
            </h1>
            <p className="text-[#7878a0] text-sm md:text-base max-w-lg leading-relaxed mb-8">
              open-source minecraft utility client for fabric 1.21.x.
              built by stormdevzz. community site.
            </p>
            <p className="text-xs text-[#484870] mb-10">
              unofficial community site —{' '}
              <a href={officialSite} target="_blank" className="text-[#38bdf8] underline underline-offset-2 hover:text-[#7dd3fc] transition-colors">
                official site
              </a>
            </p>
            <div className="flex flex-wrap gap-3">
              <a
                href={releasesUrl}
                target="_blank"
                className="px-6 py-2.5 bg-[#38bdf8] text-[#06060e] text-sm font-semibold hover:bg-[#7dd3fc] transition-colors"
              >
                download latest
              </a>
              <a
                href={githubUrl}
                target="_blank"
                className="px-6 py-2.5 border border-[#1a1a2e] text-[#c8c8d0] text-sm font-semibold hover:bg-[#0d0d1a] transition-colors"
              >
                source
              </a>
            </div>
          </div>
        </section>

        <section id="screenshots" className="py-16 md:py-24 border-t border-[#12121e]">
          <div className="max-w-5xl mx-auto px-5">
            <div className="mb-10">
              <h2 className="text-white text-sm font-semibold tracking-widest uppercase mb-2">
                screenshots
              </h2>
              <p className="text-[#484870] text-xs">
                {screenshots.length} images
              </p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {screenshots.map((s) => (
                <HoverCard key={s.label} className="border border-[#12121e] bg-[#0a0a14]">
                  <img
                    src={s.src}
                    alt={s.label}
                    className="w-full h-44 object-cover object-top"
                  />
                  <div className="relative z-10 px-3 py-2.5 border-t border-[#12121e]">
                    <span className="text-xs text-[#484870]">{s.label}</span>
                  </div>
                </HoverCard>
              ))}
            </div>
          </div>
        </section>

        <section id="about" className="py-16 md:py-24 border-t border-[#12121e]">
          <div className="max-w-5xl mx-auto px-5">
            <div className="grid md:grid-cols-2 gap-12">
              <div>
                <h2 className="text-white text-sm font-semibold tracking-widest uppercase mb-4">
                  about
                </h2>
                <p className="text-[#7878a0] text-sm leading-relaxed mb-4">
                  ravex is an open-source minecraft client built on fabric loader.
                  written in java, c++, and lua. supports custom addons for all
                  three languages.
                </p>
                <p className="text-[#7878a0] text-sm leading-relaxed mb-6">
                  fully verifiable — build from source and compare sha-256 against
                  the official release.
                </p>
                <div className="flex flex-wrap gap-2">
                  <span className="px-2 py-0.5 text-xs bg-[#0d0d1a] text-[#38bdf8] border border-[#1a1a2e]">
                    java
                  </span>
                  <span className="px-2 py-0.5 text-xs bg-[#0d0d1a] text-[#38bdf8] border border-[#1a1a2e]">
                    c++
                  </span>
                  <span className="px-2 py-0.5 text-xs bg-[#0d0d1a] text-[#38bdf8] border border-[#1a1a2e]">
                    lua
                  </span>
                  <span className="px-2 py-0.5 text-xs bg-[#0d0d1a] text-[#484870] border border-[#1a1a2e]">
                    fabric 1.21.x
                  </span>
                </div>
              </div>
              <div className="flex flex-col gap-3">
                {[
                  { href: discordUrl, label: 'discord', desc: 'community chat', color: '#5865f2' },
                  { href: telegramUrl, label: 'telegram', desc: 'news channel', color: '#229ed9' },
                  { href: githubUrl, label: 'github', desc: 'source code', color: '#c8c8d0' },
                ].map((link) => (
                  <a
                    key={link.label}
                    href={link.href}
                    target="_blank"
                    className="flex items-center gap-3 px-4 py-3 border border-[#12121e] bg-[#0a0a14] hover:bg-[#0d0d1a] transition-colors group"
                  >
                    <span
                      className="w-2 h-2 shrink-0"
                      style={{ backgroundColor: link.color }}
                    />
                    <div>
                      <div className="text-sm text-[#c8c8d0] group-hover:text-white transition-colors">
                        {link.label}
                      </div>
                      <div className="text-xs text-[#484870]">{link.desc}</div>
                    </div>
                  </a>
                ))}
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-[#12121e] py-6">
        <div className="max-w-5xl mx-auto px-5 flex flex-col md:flex-row items-center justify-between gap-3">
          <p className="text-xs text-[#484870]">
            community site for ravex. not official.
          </p>
          <p className="text-xs text-[#484870]">
            made by sh2-u34r
          </p>
        </div>
      </footer>
    </div>
  )
}
