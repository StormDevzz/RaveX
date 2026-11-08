'use client'

import { useEffect, useState } from 'react'

type Review = {
  id: number
  nickname: string
  text: string
  avatar: string
  date: string
}

export default function ReviewsPage() {
  const [reviews, setReviews] = useState<Review[]>([])
  const [nickname, setNickname] = useState('')
  const [text, setText] = useState('')
  const [avatar, setAvatar] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    try {
      const saved = localStorage.getItem('ravex-reviews')
      if (saved) {
        const parsed = JSON.parse(saved)
        if (Array.isArray(parsed)) setReviews(parsed)
      }
    } catch {}
  }, [])

  function submitReview() {
    setError('')
    if (!nickname.trim()) {
      setError('Please enter a nickname')
      return
    }
    if (!text.trim()) {
      setError('Please enter a review')
      return
    }
    if (text.length > 1000) {
      setError('Review must be 1000 characters or less')
      return
    }
    const newReview: Review = {
      id: Date.now(),
      nickname: nickname.trim(),
      text: text.trim(),
      avatar: avatar.trim(),
      date: new Date().toISOString(),
    }
    const updated = [newReview, ...reviews]
    setReviews(updated)
    try { localStorage.setItem('ravex-reviews', JSON.stringify(updated)) } catch {}
    setNickname('')
    setText('')
    setAvatar('')
  }

  function handleAvatarFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.size > 2 * 1024 * 1024) {
      setError('Avatar must be under 2MB')
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      setAvatar(reader.result as string)
    }
    reader.readAsDataURL(file)
  }

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

      <main className="flex-1 max-w-3xl mx-auto px-5 py-16 w-full">
        <h1 className="text-white text-2xl font-bold mb-2">Reviews</h1>
        <p className="text-[#a0a0c8] text-sm mb-8">
          Share your experience with RaveX. Reviews are stored locally in your browser.
        </p>

        {/* Review form */}
        <div className="border border-[#12121e] bg-[#0a0a14] rounded-lg p-5 mb-8">
          <h2 className="text-white text-sm font-semibold mb-4">Write a review</h2>
          <div className="flex flex-col gap-3">
            <input
              type="text"
              placeholder="Your nickname"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              maxLength={32}
              className="px-3 py-2 rounded-lg bg-[#0d0d1a] border border-[#1a1a2e] text-[#c8c8d0] text-sm placeholder-[#484870] focus:outline-none focus:border-[#38bdf8]"
            />
            <div className="flex items-center gap-3">
              {avatar ? (
                <img src={avatar} alt="avatar" className="w-10 h-10 rounded-full object-cover shrink-0" width={40} height={40} />
              ) : (
                <div className="w-10 h-10 rounded-full bg-[#1a1a2e] flex items-center justify-center text-[#38bdf8] text-sm font-bold shrink-0">
                  {nickname ? nickname.charAt(0).toUpperCase() : '?'}
                </div>
              )}
              <div className="flex-1 flex flex-col gap-2">
                <input
                  type="text"
                  placeholder="Avatar URL (optional)"
                  value={avatar.startsWith('data:') ? '' : avatar}
                  onChange={(e) => setAvatar(e.target.value)}
                  className="px-3 py-2 rounded-lg bg-[#0d0d1a] border border-[#1a1a2e] text-[#c8c8d0] text-sm placeholder-[#484870] focus:outline-none focus:border-[#38bdf8]"
                />
                <label className="text-xs text-[#a0a0c8] cursor-pointer hover:text-white transition-colors">
                  or upload image
                  <input type="file" accept="image/*" onChange={handleAvatarFile} className="hidden" />
                </label>
              </div>
              {avatar && (
                <button
                  onClick={() => setAvatar('')}
                  className="text-xs text-[#f48771] hover:underline shrink-0"
                >
                  Remove
                </button>
              )}
            </div>
            <textarea
              placeholder="Your review (up to 1000 characters)"
              value={text}
              onChange={(e) => setText(e.target.value)}
              maxLength={1000}
              rows={4}
              className="px-3 py-2 rounded-lg bg-[#0d0d1a] border border-[#1a1a2e] text-[#c8c8d0] text-sm placeholder-[#484870] focus:outline-none focus:border-[#38bdf8] resize-none"
            />
            <div className="flex items-center justify-between">
              <span className="text-xs text-[#484870]">{text.length}/1000</span>
              <button
                onClick={submitReview}
                className="px-6 py-2 rounded-lg bg-[#38bdf8] text-[#06060e] text-sm font-semibold hover:bg-[#7dd3fc] active:scale-95 transition-all"
              >
                Submit
              </button>
            </div>
            {error && <p className="text-[#f48771] text-xs">{error}</p>}
          </div>
        </div>

        {/* Reviews list */}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-white text-sm font-semibold">Reviews ({reviews.length})</h2>
        </div>
        <div className="flex flex-col gap-4">
          {reviews.length === 0 && (
            <p className="text-[#a0a0c8] text-sm">No reviews yet. Be the first!</p>
          )}
          {reviews.map((r) => (
            <div key={r.id} className="border border-[#12121e] bg-[#0a0a14] rounded-lg px-4 py-4">
              <div className="flex items-center gap-3 mb-2">
                {r.avatar ? (
                  <img src={r.avatar} alt={r.nickname} className="w-8 h-8 rounded-full object-cover shrink-0" width={32} height={32} />
                ) : (
                  <div className="w-8 h-8 rounded-full bg-[#1a1a2e] flex items-center justify-center text-[#38bdf8] text-sm font-bold shrink-0">
                    {r.nickname.charAt(0).toUpperCase()}
                  </div>
                )}
                <div>
                  <div className="text-sm text-[#c8c8d0] font-semibold">{r.nickname}</div>
                  <div className="text-xs text-[#a0a0c8]">{new Date(r.date).toLocaleDateString()}</div>
                </div>
              </div>
              <p className="text-xs text-[#a0a0c8] leading-relaxed whitespace-pre-wrap">{r.text}</p>
            </div>
          ))}
        </div>
      </main>

      <footer className="border-t border-[#12121e] py-8">
        <div className="max-w-5xl mx-auto px-5 text-center text-xs text-[#a0a0c8]">
          Community site for RaveX. Not official.
        </div>
      </footer>
    </div>
  )
}