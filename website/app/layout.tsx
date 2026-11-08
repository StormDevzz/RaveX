import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'RaveX',
  description: 'A free and open-source utility client for Minecraft on Fabric',
  manifest: '/manifest.json',
  themeColor: '#38bdf8',
  appleWebApp: {
    capable: true,
    statusBarStyle: 'black-translucent',
    title: 'RaveX',
  },
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <head>
        <link rel="apple-touch-icon" href="/favicon.png" />
      </head>
      <body>
        {children}
        <script
          dangerouslySetInnerHTML={{
            __html: `if('serviceWorker' in navigator){window.addEventListener('load',()=>{navigator.serviceWorker.register('/sw.js')})}`,
          }}
        />
      </body>
    </html>
  )
}