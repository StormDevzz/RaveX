const fs = require('fs');
const path = 'src/main/java/ravex/mixin/render/MixinLevelRenderer.java';
let content = fs.readFileSync(path, 'utf8');

// Pattern: "SomeModule var = ModuleManager.getComponent(SomeModule.class);"
// followed by "if (!var.getEnabled())" or "if (var.getEnabled())"
// Replace with: keep getComponent for field access, use ModuleManager.get() for .getEnabled()

const replacements = [
    { from: 'AirPlace ap', module: 'AirPlace' },
    { from: 'Scaffold sc', module: 'Scaffold' },
    { from: 'Trap trap', module: 'Trap' },
    { from: 'SelfTrap selfTrap', module: 'SelfTrap' },
    { from: 'BasePlace basePlace', module: 'BasePlace' },
    { from: 'AnchorAura anchorAura', module: 'AnchorAura' },
    { from: 'AutoCrystal ac', module: 'AutoCrystal' },
    { from: 'TreeCutter tc', module: 'TreeCutter' },
    { from: 'WebSelf ws', module: 'WebSelf' },
    { from: 'Breaker br', module: 'Breaker' },
    { from: 'AutoTunnel at', module: 'AutoTunnel' },
    { from: 'Nuker nk', module: 'Nuker' },
    { from: 'PVEUtils sm', module: 'PVEUtils' },
    { from: 'PVEUtils bw', module: 'PVEUtils' },
    { from: 'ECFarmer ec', module: 'ECFarmer' },
    { from: 'AutoPortal pb', module: 'AutoPortal' },
    { from: 'HoleFill hf', module: 'HoleFill' },
    { from: 'Search search', module: 'Search' },
];

for (const r of replacements) {
    // Find: "Type var = ModuleManager.getComponent(Module.class);" then "if (!var.getEnabled())"
    // Replace "if (!var.getEnabled())" with "if (!ModuleManager.get(Module.class).getEnabled())"
    // Also "if (var.getEnabled())" with "if (ModuleManager.get(Module.class).getEnabled())"
    
    let varName = r.from.split(' ')[1];
    let modClass = r.module;
    
    // Replace "if (!varName.getEnabled())" with "if (!ModuleManager.get(modClass.class).getEnabled())"
    const negPattern = new RegExp(`if \\(!${varName}\\.getEnabled\\(\\)\\)`, 'g');
    content = content.replace(negPattern, `if (!ModuleManager.get(${modClass}.class).getEnabled())`);
    
    // Replace "if (varName.getEnabled()" with "if (ModuleManager.get(modClass.class).getEnabled()"
    // (positive check, without parentheses for the rest of the condition)
    const posPattern = new RegExp(`if \\(${varName}\\.getEnabled\\(\\) &&`, 'g');
    content = content.replace(posPattern, `if (ModuleManager.get(${modClass}.class).getEnabled() &&`);
}

fs.writeFileSync(path, content, 'utf8');
console.log('Done');
