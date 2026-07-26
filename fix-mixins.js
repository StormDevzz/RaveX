const fs = require('fs');
const path = require('path');

function getJavaFiles(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    const files = [];
    for (const e of entries) {
        const full = path.join(dir, e.name);
        if (e.isDirectory()) files.push(...getJavaFiles(full));
        else if (e.isFile() && e.name.endsWith('.java')) files.push(full);
    }
    return files;
}

const folders = ['src/main/java/ravex/mixin', 'src/main/java/ravex/integrations'];
const files = folders.flatMap(f => fs.existsSync(f) ? getJavaFiles(f) : []);

// Known module classes - all under ravex.modules
const modulePkgs = ['combat', 'render', 'player', 'movement', 'misc', 'world', 'client', 'hud'];

let replacementCount = 0;
let changed = 0;

function isModuleClass(name) {
    return modulePkgs.some(pkg => {
        try {
            const fullPath = `src/main/java/ravex/modules/${pkg}/${name}.java`;
            return fs.existsSync(fullPath);
        } catch { return false; }
    });
}

// Check subpackages too
const subpackages = {
    'AutoReGear': 'player/autoregear',
    'InvClean': 'player/invclean',
    'Nuker': 'world/nuker'
};

function isModule(name) {
    if (subpackages[name]) {
        return fs.existsSync(`src/main/java/ravex/modules/${subpackages[name]}/${name}.java`);
    }
    return isModuleClass(name);
}

for (const file of files) {
    let c = fs.readFileSync(file, 'utf8');
    const orig = c;

    // Replace ClassName.itz() -> ModuleManager.getComponent(ClassName.class)
    c = c.replace(/([A-Z]\w+)\.itz\s*\(\s*\)/g, (match, className) => {
        if (isModule(className)) {
            replacementCount++;
            return `ModuleManager.getComponent(${className}.class)`;
        }
        return match;
    });

    // Replace ClassName.maybeEnabled() -> ModuleManager.get(ClassName.class).getEnabled()
    c = c.replace(/([A-Z]\w+)\.maybeEnabled\s*\(\s*\)/g, (match, className) => {
        if (isModule(className)) {
            replacementCount++;
            return `ModuleManager.get(${className}.class).getEnabled()`;
        }
        return match;
    });

    // Add ModuleManager import if not present and replacements were made
    if (c !== orig) {
        if (!c.includes('import ravex.manager.ModuleManager;')) {
            c = c.replace(/^(package ravex(?:\.\w+)*;)/m, '$1\nimport ravex.manager.ModuleManager;');
        }
        fs.writeFileSync(file, c, 'utf8');
        changed++;
    }
}

console.log(`Done. ${changed} mixin files changed, ${replacementCount} replacements made.`);
