const fs = require('fs');
const path = require('path');

const root = 'src/main/java/ravex/modules';

function getFiles(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    const files = [];
    for (const e of entries) {
        if (e.name === 'annotations' || e.name === 'Module.java' || e.name === 'Category.java') continue;
        const full = path.join(dir, e.name);
        if (e.isDirectory()) files.push(...getFiles(full));
        else if (e.isFile() && e.name.endsWith('.java')) files.push(full);
    }
    return files;
}

function addImport(c, imp) {
    if (c.includes(imp)) return c;
    return c.replace(/^(package ravex(?:\.\w+)+;|package ravex\.modules;)/m, `$1\n${imp}`);
}

let changed = 0;
for (const file of getFiles(root)) {
    let c = fs.readFileSync(file, 'utf8');
    const orig = c;
    if (/extends\s+ravex\.modules\.Module\b/.test(c)) {
        c = addImport(c, 'import ravex.modules.Module;');
        c = c.replace(/extends\s+ravex\.modules\.Module\b/g, 'extends Module');
    }
    if (/Category\.(?:COMBAT|RENDER|PLAYER|MOVEMENT|MISC|WORLD|CLIENT|HUD)/.test(c) && !c.includes('import ravex.modules.Category;')) {
        c = addImport(c, 'import ravex.modules.Category;');
    }
    c = c.replace(/\n{3,}/g, '\n\n');
    if (c !== orig) {
        fs.writeFileSync(file, c, 'utf8');
        changed++;
    }
}
console.log(`Done. ${changed} files changed.`);
