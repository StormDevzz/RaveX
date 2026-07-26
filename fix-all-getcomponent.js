const fs = require('fs');
const path = require('path');

const srcDir = 'src/main/java/ravex';
const dirs = ['gui', 'cmd', 'manager', 'mixin', 'utility', 'render'];
const skipDirs = ['modules'];  // module files use get() correctly

// Replace ModuleManager.get(SimpleName.class).field where field is not getEnabled/setEnabled
// Also handle ModuleManager.get(qualified.name.SimpleName.class).field
function replaceGetWithGetComponent(content) {
    // Skip all Module public methods — these should stay on .get()
    const moduleMethods = [
        'getGearAngle', 'getGearLastTick', 'setGearAngle',
        'isHud', 'setHud', 'getName', 'getCategory', 'getDescription',
        'getEnabled', 'setEnableCondition', 'isVisible', 'setVisibleCondition',
        'setEnabled', 'isToggleLocked', 'toggle',
        'getKeyBind', 'setKeyBind', 'getParameters',
        'onTick', 'render', 'saveExtra', 'loadExtra',
        'updateAnimation', 'getDisplayX', 'setDisplayX', 'getDisplayY', 'setDisplayY',
        'isAnimInitialized', 'setAnimInitialized',
        'getX', 'getY', 'getTargetX', 'getTargetY', 'setX', 'setY',
        'getWidth', 'setWidth', 'getHeight', 'setHeight',
        'getClass', 'hashCode', 'equals', 'toString', 'notify', 'notifyAll', 'wait'
    ];
    const excluded = moduleMethods.join('|');
    const regex = new RegExp(`ModuleManager\\.get\\(([a-zA-Z_][\\w.]*\\.class)\\)\\.(?!${excluded}\\b)([a-zA-Z_]\\w*)`, 'g');
    return content.replace(regex, (match, classRef, field) => {
        return `ModuleManager.getComponent(${classRef}).${field}`;
    });
}

function processFile(filePath) {
    if (skipDirs.some(d => filePath.replace(/\\/g, '/').includes('/' + d + '/'))) return;
    let content = fs.readFileSync(filePath, 'utf8');
    let original = content;
    content = replaceGetWithGetComponent(content);
    if (content !== original) {
        fs.writeFileSync(filePath, content, 'utf8');
        console.log(`Fixed: ${path.relative(srcDir, filePath)}`);
    }
}

function walkDir(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            if (!skipDirs.includes(entry.name)) {
                walkDir(fullPath);
            }
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
