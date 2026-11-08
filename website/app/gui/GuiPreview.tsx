'use client'

import { useState } from 'react'

const categories = [
  { name: 'Combat', color: '#ff4444', modules: ['AimAssist', 'AnchorAura', 'AntiBot', 'AntiPearl', 'AntiReGear', 'AutoApple', 'AutoBow', 'AutoCart', 'AutoClicker', 'AutoCrystal', 'AutoDrop', 'AutoSoup', 'AutoTotem', 'BasePlace', 'BedBomb', 'BowAim', 'Breaker', 'Burrow', 'Criticals', 'Hitboxes', 'HoleFill', 'KeyPearl', 'KillAura', 'MaceAura', 'MaceSwap', 'PearlTarget', 'Quiver', 'Reach', 'SelfTrap', 'ShieldFucker', 'Surround', 'TntAura', 'Trap', 'Trigger', 'WebAura', 'WebSelf', 'WindAura'] },
  { name: 'Render', color: '#4444ff', modules: ['Ambient', 'Animations', 'AspectRatio', 'BlockOutline', 'Borders', 'BreadCrumbs', 'ChinaHat', 'CityESP', 'Crosshair', 'DeathText', 'ESP', 'FreeCam', 'FreeLook', 'Fullbright', 'Glint', 'HoleESP', 'ItemPhysics', 'JumpCircles', 'KillEffects', 'LogoutSpots', 'NameTags', 'NoBob', 'NoRender', 'Particles', 'Search', 'Shaders', 'ShiftInterp', 'Skeleton', 'StorageESP', 'SwingAnimation', 'ToolTips', 'Tracers', 'Trails', 'Trajectories', 'TunnelESP', 'ViewClip', 'ViewModel', 'VoidESP', 'Waypoint', 'Weather', 'WorldColor', 'Zoom'] },
  { name: 'Player', color: '#ffff44', modules: ['AirPlace', 'AntiAim', 'AntiHunger', 'AutoArmor', 'AutoMend', 'AutoReGear', 'AutoRespawn', 'AutoTool', 'ChestHelper', 'ChorusExploit', 'ElytraHelper', 'FastBreak', 'GhostHand', 'Handshake', 'InvClean', 'ItemSaver', 'MiddleClick', 'MineAnimation', 'MobOwner', 'MultiTask', 'NoDelay', 'NoInteract', 'NoSwing', 'PacketMine', 'Replenish', 'SourceFiller', 'TabHelper', 'ViewLock'] },
  { name: 'Movement', color: '#44ff44', modules: ['AntiVoid', 'AutoSprint', 'AutoWalk', 'Avoid', 'Blink', 'BoatFly', 'ClickFly', 'ClickTP', 'ElytraFly', 'FastStairs', 'Flight', 'GuiMove', 'HighJump', 'LiquidControl', 'LongJump', 'NoFall', 'NoPush', 'NoSlow', 'NoWeb', 'PacketFly', 'Phase', 'ReverseStep', 'RidingHelper', 'SafeWalk', 'Sleepy', 'Speed', 'Spider', 'Step', 'TickShift', 'Timer', 'TridentBoost', 'Velocity'] },
  { name: 'Misc', color: '#44ffff', modules: ['AntiAfk', 'AntiAttack', 'AntiQuit', 'AutoAuth', 'AutoEat', 'AutoLog', 'AutoReconnect', 'AutoSoup', 'BlockMixer', 'BookHelper', 'ChatHelper', 'Disabler', 'FakePearl', 'FastItem', 'IllegalDisconnect', 'LagNotify', 'MessageAura', 'MoreChatHistory', 'NameProtect', 'NewChunks', 'NoPacketKick', 'PacketHelper', 'PauseBaritone', 'PingSpoof', 'PortalGod', 'PortalGui', 'RideExploit', 'StashFinder', 'WaxAura', 'Xray'] },
  { name: 'World', color: '#ff44ff', modules: ['AutoFarm', 'AutoFish', 'AutoMount', 'AutoNameTag', 'AutoPortal', 'AutoShear', 'AutoSign', 'AutoTrade', 'AutoTunnel', 'AutoWither', 'ChestAura', 'ECFarmer', 'FakePlayer', 'GhostBlocks', 'Igniter', 'Nuker', 'PVEUtils'] },
  { name: 'Client', color: '#aa44ff', modules: ['Baritone', 'Calculator', 'ClickGui', 'Commands', 'DesktopGui', 'FastLatency', 'Fonts', 'GuiParticles', 'Hud', 'MainMenu', 'Notifications', 'RichPresence', 'Settings', 'Sounds'] },
  { name: 'Custom', color: '#888888', modules: [] },
]

function evalExpr(expr: string): string {
  try {
    let e = expr.replace(/pi/gi, 'Math.PI')
    e = e.replace(/fact\(/g, 'factorial(')
    const factorial = (n: number): number => {
      if (n < 0) return NaN
      if (n === 0 || n === 1) return 1
      let r = 1
      for (let i = 2; i <= n; i++) r *= i
      return r
    }
    const fn = new Function('factorial', 'return ' + e)
    const result = fn(factorial)
    if (typeof result === 'number' && !isNaN(result) && isFinite(result)) {
      if (Number.isInteger(result)) return String(result)
      return result.toFixed(10).replace(/\.?0+$/, '')
    }
    return 'Error: invalid'
  } catch {
    return 'Error: invalid'
  }
}

export default function GuiPreview() {
  const [enabled, setEnabled] = useState<Record<string, boolean>>({})
  const [activeCat, setActiveCat] = useState('Combat')
  const [search, setSearch] = useState('')
  const [showCalc, setShowCalc] = useState(false)
  const [calcExpr, setCalcExpr] = useState('')
  const [calcResult, setCalcResult] = useState('')
  const [calcHistory, setCalcHistory] = useState<string[]>([])
  const [selectedModule, setSelectedModule] = useState<string | null>(null)

  function toggle(mod: string) {
    setEnabled((prev) => ({ ...prev, [mod]: !prev[mod] }))
    if (mod === 'Calculator' && !enabled[mod]) {
      setShowCalc(true)
    }
  }

  const cat = categories.find((c) => c.name === activeCat) || categories[0]
  const filteredModules = cat.modules.filter((m) => m.toLowerCase().includes(search.toLowerCase()))

  function calcBtn(label: string) {
    if (label === '=') {
      if (!calcExpr) return
      const res = evalExpr(calcExpr)
      setCalcResult(res.startsWith('Error') ? res : '')
      if (!res.startsWith('Error')) {
        setCalcHistory((prev) => [calcExpr + ' = ' + res, ...prev].slice(0, 20))
        setCalcExpr(res)
      }
      return
    }
    if (label === 'C') {
      setCalcExpr('')
      setCalcResult('')
      return
    }
    if (label === '⌫') {
      setCalcExpr((prev) => prev.slice(0, -1))
      setCalcResult('')
      return
    }
    if (label === '±') {
      if (calcExpr.startsWith('-')) setCalcExpr(calcExpr.slice(1))
      else setCalcExpr('-' + calcExpr)
      setCalcResult('')
      return
    }
    if (label === 'π') {
      setCalcExpr((prev) => prev + 'pi')
      setCalcResult('')
      return
    }
    setCalcExpr((prev) => prev + label)
    setCalcResult('')
  }

  const btnClass = (label: string) => {
    if (label === '=') return 'bg-[#7a1aaa] hover:bg-[#9a2acc] text-white font-bold'
    if (label === 'C') return 'bg-[#3a0a1a] hover:bg-[#5a1a2a] text-[#ff8888]'
    if (['÷', '×', '−', '+', '%', '^'].includes(label)) return 'bg-[#1e0e38] hover:bg-[#2e1a50] text-[#da70d6]'
    if (['sin(', 'cos(', 'tan(', 'sqrt(', 'log(', 'asin(', 'acos(', 'atan(', 'fact(', '(', ')'].includes(label)) return 'bg-[#0e0e26] hover:bg-[#1a1840] text-[#9090d0] text-xs'
    return 'bg-[#1a1040] hover:bg-[#2a2060] text-[#eeeeee]'
  }

  return (
    <div className="flex gap-4">
      <div className="flex-1 border border-[#ffffff]/15 bg-[#ffffff]/8 backdrop-blur-xl rounded-lg overflow-hidden shadow-[0_0_60px_rgba(255,255,255,0.04)]">
        <div className="flex h-[420px]">
        <div className="w-44 shrink-0 bg-[#ffffff]/8 border-r border-[#ffffff]/10 p-2 flex flex-col gap-1 overflow-y-auto">
          {categories.map((c) => (
            <button
              key={c.name}
              onClick={() => setActiveCat(c.name)}
              className={
                'text-left px-3 py-1.5 rounded text-xs font-semibold transition-colors flex items-center gap-2 ' +
                (activeCat === c.name ? 'bg-[#ffffff]/15 text-white' : 'text-[#c0c0d8] hover:text-white hover:bg-[#ffffff]/8')
              }
            >
                <img src={'/icons/' + c.name.toLowerCase() + '.png'} alt={c.name} className="w-4 h-4 shrink-0" width={16} height={16} />
                {c.name}
              </button>
            ))}
          </div>
          <div className="flex-1 p-3 overflow-y-auto flex flex-col gap-2" onClick={() => setSelectedModule(null)}>
            <input
              type="text"
              placeholder="Search modules..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full px-2 py-1 text-[12px] bg-[#ffffff]/10 text-[#e8e8f0] border border-[#ffffff]/10 rounded placeholder-[#8888aa] focus:outline-none focus:border-[#38bdf8]"
            />
            <div className="text-xs text-[#c0c0d8] font-semibold">{cat.name} Modules ({filteredModules.length})</div>
            <div className="flex flex-col gap-1.5">
              {filteredModules.map((mod) => (
                <div
                  key={mod}
                  onClick={() => toggle(mod)}
                  onContextMenu={(e) => { e.preventDefault(); setSelectedModule(mod === selectedModule ? null : mod) }}
                  className="flex items-center justify-between px-3 py-2 rounded bg-[#ffffff]/8 border border-[#ffffff]/15 hover:border-[#38bdf8]/40 transition-colors cursor-pointer select-none"
                >
                  <span className="text-sm text-[#f0f0f8]">{mod}</span>
                  <button
                    onClick={(e) => { e.stopPropagation(); toggle(mod) }}
                    className={
                      'w-8 h-4 rounded-full transition-colors relative ' +
                      (enabled[mod] ? 'bg-[#38bdf8]' : 'bg-[#5a5a7e]')
                    }
                  >
                    <span
                      className={
                        'absolute top-0.5 w-3 h-3 rounded-full bg-white transition-all ' +
                        (enabled[mod] ? 'left-4' : 'left-0.5')
                      }
                    />
                  </button>
                </div>
              ))}
            </div>
          </div>
          {selectedModule && (
            <div className="w-48 shrink-0 bg-[#ffffff]/8 border-l border-[#ffffff]/10 p-3 flex flex-col gap-2">
              <div className="text-xs text-[#c0c0d8] font-semibold">{selectedModule} Settings</div>
              <div className="text-xs text-[#8888aa]">Range</div>
              <div className="h-1.5 bg-[#ffffff]/10 rounded-full">
                <div className="h-full w-2/3 bg-[#38bdf8] rounded-full" />
              </div>
              <div className="text-xs text-[#8888aa] mt-2">Mode</div>
              <div className="text-xs text-[#d8d8e8] bg-[#ffffff]/10 px-2 py-1 rounded">Legit</div>
              <div className="text-xs text-[#8888aa] mt-2">Enabled</div>
              <div className="w-8 h-4 rounded-full bg-[#38bdf8] relative">
                <span className="absolute top-0.5 left-4 w-3 h-3 rounded-full bg-white" />
              </div>
              <button
                onClick={() => setSelectedModule(null)}
                className="mt-2 text-xs text-[#8888aa] hover:text-white transition-colors"
              >
                Close
              </button>
            </div>
          )}
        </div>
        <div className="px-3 py-2 border-t border-[#ffffff]/10 text-xs text-[#b0b0c8]">
          Interactive preview - click module row to toggle, right-click for settings, search modules
        </div>
      </div>

      {showCalc && (
        <div className="w-80 shrink-0 border border-[#ffffff]/10 bg-[#0a0a14]/80 backdrop-blur-md rounded-lg overflow-hidden flex flex-col">
          <div className="px-3 py-2 border-b border-[#ffffff]/10 flex items-center justify-between">
            <span className="text-sm font-bold text-[#da70d6]">RaveX Calculator</span>
            <button onClick={() => setShowCalc(false)} className="text-[#858585] hover:text-white text-xs">✕</button>
          </div>
          <div className="p-3 flex flex-col gap-2 flex-1">
            <div className="bg-[#080614]/60 rounded p-2 min-h-[40px] text-right">
              <div className="text-[#eeeeee] font-mono text-sm break-all">{calcExpr || ''}</div>
              <div className="text-[#da70d6] font-mono text-xs">{calcResult}</div>
            </div>
            {calcHistory.length > 0 && (
              <div className="bg-[#080614]/60 rounded p-2 max-h-[60px] overflow-y-auto text-right">
                {calcHistory.map((h, i) => (
                  <div key={i} className="text-[#7a6a9c] font-mono text-[10px]">{h}</div>
                ))}
              </div>
            )}
            <div className="grid grid-cols-5 gap-1">
              {['sin(', 'cos(', 'tan(', 'sqrt(', 'log('].map((l) => (
                <button key={l} onClick={() => calcBtn(l)} className={'py-1.5 rounded text-[10px] ' + btnClass(l)}>{l}</button>
              ))}
              {['asin(', 'acos(', 'atan(', '^', 'fact('].map((l) => (
                <button key={l} onClick={() => calcBtn(l)} className={'py-1.5 rounded text-[10px] ' + btnClass(l)}>{l}</button>
              ))}
            </div>
            <div className="grid grid-cols-4 gap-1">
              {['C', '±', '%', '⌫'].map((l) => (
                <button key={l} onClick={() => calcBtn(l)} className={'py-2 rounded text-sm ' + btnClass(l)}>{l}</button>
              ))}
              {['7', '8', '9', '÷'].map((l) => (
                <button key={l} onClick={() => calcBtn(l === '÷' ? '/' : l)} className={'py-2 rounded text-sm ' + btnClass(l === '÷' ? '/' : l)}>{l}</button>
              ))}
              {['4', '5', '6', '×'].map((l) => (
                <button key={l} onClick={() => calcBtn(l === '×' ? '*' : l)} className={'py-2 rounded text-sm ' + btnClass(l === '×' ? '*' : l)}>{l}</button>
              ))}
              {['1', '2', '3', '−'].map((l) => (
                <button key={l} onClick={() => calcBtn(l === '−' ? '-' : l)} className={'py-2 rounded text-sm ' + btnClass(l === '−' ? '-' : l)}>{l}</button>
              ))}
              <button onClick={() => calcBtn('0')} className="py-2 rounded text-sm bg-[#ffffff]/10 hover:bg-[#ffffff]/20 text-[#eeeeee] col-span-2">0</button>
              <button onClick={() => calcBtn('.')} className={'py-2 rounded text-sm ' + btnClass('.')}>.</button>
              <button onClick={() => calcBtn('+')} className={'py-2 rounded text-sm ' + btnClass('+')}>+</button>
              <button onClick={() => calcBtn('(')} className={'py-2 rounded text-sm ' + btnClass('(')}>(</button>
              <button onClick={() => calcBtn(')')} className={'py-2 rounded text-sm ' + btnClass(')')}>)</button>
              <button onClick={() => calcBtn('π')} className={'py-2 rounded text-sm ' + btnClass('π')}>π</button>
              <button onClick={() => calcBtn('=')} className={'py-2 rounded text-sm ' + btnClass('=')}>=</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}