import GuiPreview from '../gui/GuiPreview'

export default function GuiPage() {
  return (
    <div className="min-h-screen flex flex-col bg-[#06060e]">
      <header className="sticky top-0 z-50 border-b border-[#12121e] bg-[#06060e]/90 backdrop-blur-sm">
        <div className="max-w-5xl mx-auto px-5 h-12 flex items-center justify-between">
          <a href="/" className="flex items-center shrink-0" aria-label="RaveX home">
            <img src="/ravexv2.png" alt="RaveX" className="h-8 w-auto" />
          </a>
          <nav className="flex items-center gap-3 md:gap-5 text-xs">
            <a href="/" className="text-[#7878a0] hover:text-white transition-colors">Home</a>
            <a href="/code" className="text-[#7878a0] hover:text-white transition-colors">Code</a>
          </nav>
        </div>
      </header>
      <main className="flex-1 max-w-5xl mx-auto px-5 py-16 w-full">
        <h1 className="text-white text-2xl font-bold mb-2">GUI Preview</h1>
        <p className="text-[#a0a0c8] text-sm mb-8">
          Interactive preview of the RaveX ClickGUI. Toggle modules, switch categories.
        </p>
        <GuiPreview />
      </main>
      <footer className="border-t border-[#12121e] py-8">
        <div className="max-w-5xl mx-auto px-5 text-center text-xs text-[#a0a0c8]">
          Community site for RaveX. Not official.
        </div>
      </footer>
    </div>
  )
}