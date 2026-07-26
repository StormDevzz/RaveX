import type { Metadata } from "next"
import "./globals.css"

export const metadata: Metadata = {
  title: "RaveX — Community Website",
  description:
    "RaveX is an open-source Minecraft utility client by StormDevzz. This is a community-made website.",
  icons: {
    icon: "/favicon.svg",
  },
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className="bg-ravex-dark text-gray-100 antialiased">{children}</body>
    </html>
  )
}
