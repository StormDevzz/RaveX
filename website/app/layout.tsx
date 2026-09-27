import type { Metadata } from "next"
import "./globals.css"
import "highlight.js/styles/vs2015.css"

export const metadata: Metadata = {
  title: "RaveX - open-source Minecraft utility client",
  description:
    "RaveX is a free and open-source Minecraft utility client for Fabric 1.21.x, written in Java and C++.",
  icons: "/favicon.png",
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="ru">
      <body className="bg-[#06060e] text-[#c8c8d0]">{children}</body>
    </html>
  )
}
