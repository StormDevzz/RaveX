const fs = require('fs');
const path = require('path');

const srcDir = 'src/main/java/ravex';

// Directories to process
const dirs = ['gui', 'cmd', 'manager', 'mixin'];

// Modules that need getComponent() for field access
const fieldAccessModules = [
    'ClickGui', 'Commands', 'Hud', 'Settings', 'Waypoint', 'Xray',
    'AutoCrystal', 'KillAura', 'Scaffold', 'Flight', 'NoSlow',
    'GuiMove', 'NoInteract', 'ChatHelper', 'PacketMine',
    'FreeCam', 'FreeLook', 'NoRender', 'ESP', 'BoatFly',
    'ElytraFly', 'Velocity', 'Speed', 'Spider', 'Timer',
    'NoRotate', 'HighJump', 'TridentBoost', 'ChorusExploit',
    'AutoReconnect', 'InvClean', 'GuiParticles', 'TabHelper',
    'FastBreak', 'GhostHand', 'StashFinder', 'FastItem',
    'ViewLock', 'Avoid', 'PortalGui', 'Hitboxes', 'NoPush',
    'NoWeb', 'LiquidControl', 'RideExploit', 'AutoSign',
    'SafeWalk', 'Breaker', 'Trap', 'SelfTrap', 'BasePlace',
    'AnchorAura', 'AntiAim', 'BowAim', 'Quiver', 'ShieldFucker',
    'AirPlace', 'TreeCutter', 'WebSelf', 'AutoTunnel', 'Nuker',
    'PVEUtils', 'ECFarmer', 'AutoPortal', 'HoleFill', 'Search',
    'ViewModel', 'NameProtect', 'RichPresence'
];

function processFile(filePath) {
    let content = fs.readFileSync(filePath, 'utf8');
    let original = content;

    // Replace ModuleManager.get(SomeModule.class) with ModuleManager.getComponent(SomeModule.class)
    // when followed by a field/method access (not getEnabled/setEnabled)
    for (const mod of fieldAccessModules) {
        // Pattern: ModuleManager.get(Mod.class).something where something != getEnabled/setEnabled
        const regex = new RegExp(`ModuleManager\\.get\\(${mod}\\.class\\)\\.(?!getEnabled\\b|setEnabled\\b)(\\w+)`, 'g');
        content = content.replace(regex, `ModuleManager.getComponent(${mod}.class).$1`);
    }

    if (content !== original) {
        fs.writeFileSync(filePath, content, 'utf8');
        console.log(`Fixed: ${filePath}`);
    }
}

function walkDir(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            walkDir(fullPath);
        } else if (entry.isFile() && entry.name.endsWith('.java')) {
            processFile(fullPath);
        }
    }
}

for (const d of dirs) {
    const fullDir = path.join(srcDir, d);
    if (fs.existsSync(fullDir)) {
        walkDir(fullDir);
    }
}
