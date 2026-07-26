const telegramUrl = "https://t.me/ravex_free"
const discordUrl = "https://discord.gg/n9HPbgN7S"
const githubUrl = "https://github.com/StormDevzz/RaveX"
const officialSite = "https://ravex.serveousercontent.com/"
const releasesUrl = "https://github.com/StormDevzz/RaveX/releases"

const screenshots = [
  { src: "/screenshots/loading.png", label: "Loading Screen" },
  { src: "/screenshots/gui.png", label: "ClickGUI" },
  { src: "/screenshots/physics.png", label: "Physics" },
  { src: "/screenshots/newgui.png", label: "New GUI" },
  { src: "/screenshots/russian.png", label: "Russian Language" },
]

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="sticky top-0 z-50 backdrop-blur-lg bg-ravex-dark/80 border-b border-ravex-border">
        <div className="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
          <a href="/" className="text-xl font-extrabold gradient-text tracking-tight">
            RAVEX
          </a>
          <nav className="flex items-center gap-6 text-sm">
            <a href="#screenshots" className="text-gray-400 hover:text-white transition-colors">
              Screenshots
            </a>
            <a href="#about" className="text-gray-400 hover:text-white transition-colors">
              About
            </a>
            <a
              href={releasesUrl}
              target="_blank"
              className="px-4 py-1.5 rounded-lg bg-gradient-to-r from-purple-500 to-cyan-500 text-white font-semibold text-sm hover:opacity-90 transition-opacity"
            >
              Download
            </a>
          </nav>
        </div>
      </header>

      <main className="flex-1">
        <section className="relative overflow-hidden pt-24 pb-16 md:pt-32 md:pb-24">
          <div className="absolute inset-0 bg-gradient-to-b from-purple-500/5 via-transparent to-transparent pointer-events-none" />
          <div className="max-w-6xl mx-auto px-4 text-center relative z-10">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-ravex-card border border-ravex-border text-xs text-gray-400 mb-8">
              Community Project
            </div>
            <h1 className="text-4xl md:text-7xl font-black tracking-tight mb-6">
              <span className="gradient-text">RaveX</span>
              <br />
              <span className="text-white">Minecraft Utility Client</span>
            </h1>
            <p className="text-gray-400 text-lg md:text-xl max-w-2xl mx-auto mb-8 leading-relaxed">
              An open-source Minecraft client modification for Fabric 1.21.x
              developed by{" "}
              <a
                href="https://github.com/StormDevzz"
                target="_blank"
                className="text-purple-400 hover:text-purple-300 transition-colors"
              >
                StormDevzz
              </a>
              .
            </p>
            <p className="text-gray-500 text-sm mb-10">
              This is an unofficial community-maintained website.{" "}
              <a
                href={officialSite}
                target="_blank"
                className="text-cyan-400 hover:text-cyan-300 underline underline-offset-2 transition-colors"
              >
                Official site →
              </a>
            </p>
            <div className="flex flex-wrap justify-center gap-4">
              <a
                href={releasesUrl}
                target="_blank"
                className="px-8 py-3 rounded-xl bg-gradient-to-r from-purple-500 to-cyan-500 text-white font-bold hover:opacity-90 transition-opacity shadow-lg shadow-purple-500/20"
              >
                Download Latest
              </a>
              <a
                href={githubUrl}
                target="_blank"
                className="px-8 py-3 rounded-xl bg-ravex-card border border-ravex-border text-gray-200 font-bold hover:bg-ravex-border transition-colors"
              >
                Source Code
              </a>
            </div>
          </div>
        </section>

        <section id="screenshots" className="py-16 md:py-24">
          <div className="max-w-6xl mx-auto px-4">
            <div className="text-center mb-12">
              <h2 className="text-3xl md:text-4xl font-black gradient-text mb-4">
                Screenshots
              </h2>
              <p className="text-gray-500">
                A glimpse of what RaveX looks like in action.
              </p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {screenshots.map((s) => (
                <div
                  key={s.label}
                  className="group rounded-xl overflow-hidden bg-ravex-card border border-ravex-border glow"
                >
                  <img
                    src={s.src}
                    alt={s.label}
                    className="w-full h-48 object-cover object-top group-hover:scale-105 transition-transform duration-500"
                  />
                  <div className="p-3 text-center text-sm text-gray-400">
                    {s.label}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section id="about" className="py-16 md:py-24 border-t border-ravex-border">
          <div className="max-w-6xl mx-auto px-4">
            <div className="grid md:grid-cols-2 gap-12 items-center">
              <div>
                <h2 className="text-3xl md:text-4xl font-black gradient-text mb-6">
                  Built with passion
                </h2>
                <p className="text-gray-400 leading-relaxed mb-4">
                  RaveX is an open-source Minecraft utility client built on Fabric
                  for version 1.21.x. It combines Java, C++, and Lua to deliver a
                  feature-rich experience.
                </p>
                <p className="text-gray-500 text-sm leading-relaxed mb-6">
                  The project supports custom addons in Java, C++, and Lua, and is
                  fully verifiable — build from source and compare the SHA-256 hash
                  against the official release.
                </p>
                <div className="flex flex-wrap gap-3">
                  <span className="px-3 py-1 rounded-md bg-purple-500/10 text-purple-400 text-xs font-medium border border-purple-500/20">
                    Java
                  </span>
                  <span className="px-3 py-1 rounded-md bg-cyan-500/10 text-cyan-400 text-xs font-medium border border-cyan-500/20">
                    C++
                  </span>
                  <span className="px-3 py-1 rounded-md bg-green-500/10 text-green-400 text-xs font-medium border border-green-500/20">
                    Lua
                  </span>
                  <span className="px-3 py-1 rounded-md bg-gray-500/10 text-gray-400 text-xs font-medium border border-gray-500/20">
                    Fabric 1.21.x
                  </span>
                </div>
              </div>
              <div className="flex flex-col gap-4">
                <a
                  href={discordUrl}
                  target="_blank"
                  className="flex items-center gap-4 p-4 rounded-xl bg-ravex-card border border-ravex-border hover:bg-ravex-border transition-colors group"
                >
                  <div className="w-10 h-10 rounded-lg bg-indigo-500/20 flex items-center justify-center text-indigo-400 text-xl font-bold">
                    D
                  </div>
                  <div>
                    <div className="font-semibold text-gray-200 group-hover:text-white transition-colors">
                      Discord
                    </div>
                    <div className="text-sm text-gray-500">
                      Join the community
                    </div>
                  </div>
                </a>
                <a
                  href={telegramUrl}
                  target="_blank"
                  className="flex items-center gap-4 p-4 rounded-xl bg-ravex-card border border-ravex-border hover:bg-ravex-border transition-colors group"
                >
                  <div className="w-10 h-10 rounded-lg bg-sky-500/20 flex items-center justify-center text-sky-400 text-xl font-bold">
                    T
                  </div>
                  <div>
                    <div className="font-semibold text-gray-200 group-hover:text-white transition-colors">
                      Telegram
                    </div>
                    <div className="text-sm text-gray-500">
                      News and updates
                    </div>
                  </div>
                </a>
                <a
                  href={githubUrl}
                  target="_blank"
                  className="flex items-center gap-4 p-4 rounded-xl bg-ravex-card border border-ravex-border hover:bg-ravex-border transition-colors group"
                >
                  <div className="w-10 h-10 rounded-lg bg-gray-500/20 flex items-center justify-center text-gray-400 text-xl font-bold">
                    G
                  </div>
                  <div>
                    <div className="font-semibold text-gray-200 group-hover:text-white transition-colors">
                      GitHub
                    </div>
                    <div className="text-sm text-gray-500">
                      Source code & releases
                    </div>
                  </div>
                </a>
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-ravex-border py-8">
        <div className="max-w-6xl mx-auto px-4 flex flex-col md:flex-row items-center justify-between gap-4">
          <p className="text-sm text-gray-500">
            Community website for{" "}
            <a
              href={githubUrl}
              target="_blank"
              className="text-purple-400 hover:text-purple-300 transition-colors"
            >
              RaveX
            </a>
            . Not affiliated with StormDevzz.
          </p>
          <p className="text-sm text-gray-500">
            Created by{" "}
            <span className="text-cyan-400 font-medium">sh2-u34r</span>
          </p>
        </div>
      </footer>
    </div>
  )
}
