const fs = require('fs');
let content = fs.readFileSync('src/main/java/ravex/mixin/render/MixinLevelRenderer.java', 'utf8');

const fixes = [
  { var: 'ap', cls: 'AirPlace' },
  { var: 'sc', cls: 'Scaffold' },
  { var: 'trap', cls: 'Trap' },
  { var: 'selfTrap', cls: 'SelfTrap' },
  { var: 'basePlace', cls: 'BasePlace' },
  { var: 'anchorAura', cls: 'AnchorAura' },
  { var: 'ac', cls: 'AutoCrystal' },
  { var: 'tc', cls: 'TreeCutter' },
  { var: 'ws', cls: 'WebSelf' },
  { var: 'br', cls: 'Breaker' },
  { var: 'at', cls: 'AutoTunnel' },
  { var: 'nk', cls: 'Nuker' },
  { var: 'sm', cls: 'PVEUtils' },
  { var: 'bw', cls: 'PVEUtils' },
  { var: 'ec', cls: 'ECFarmer' },
  { var: 'pb', cls: 'AutoPortal' },
  { var: 'hf', cls: 'HoleFill' },
  { var: 'search', cls: 'Search' },
];

// Pattern: "if (var.getEnabled() &&" -> "if (ModuleManager.get(Cls.class).getEnabled() &&"
for (const f of fixes) {
  const from = `if (${f.var}.getEnabled() &&`;
  const to = `if (ModuleManager.get(${f.cls}.class).getEnabled() &&`;
  // Use replace to avoid replaceAll if not needed
  while (content.includes(from)) {
    content = content.replace(from, to);
  }
}

fs.writeFileSync('src/main/java/ravex/mixin/render/MixinLevelRenderer.java', content, 'utf8');
console.log('Done');
