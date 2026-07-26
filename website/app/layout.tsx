import type { Metadata } from "next"
import "./globals.css"

export const metadata: Metadata = {
  title: "ravex",
  description:
    "ravex - open-source minecraft utility client for fabric 1.21.x",
  icons: undefined,
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className="bg-[#06060e] text-[#c8c8d0] uppercase">{children}</body>
    </html>
  )
}
