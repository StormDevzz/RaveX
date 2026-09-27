import type { Metadata } from 'next'
import CodeBrowser from './CodeBrowser'

export const metadata: Metadata = {
  title: 'RaveX - Code',
  description: 'Browse the RaveX source code live, straight from GitHub.',
}

export default function CodePage() {
  return (
    <div className="h-screen">
      <CodeBrowser />
    </div>
  )
}
