const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const moduleRoot = 'src/main/java/ravex/modules';

function getFiles(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    const files = [];
    for (const entry of entries) {
        if (entry.name === 'annotations' || entry.name === 'Module.java' || entry.name === 'Category.java') continue;
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) files.push(...getFiles(full));
        else if (entry.isFile() && entry.name.endsWith('.java')) files.push(full);
    }
    return files;
}

function removeMethod(content, methodPattern) {
    const regex = new RegExp(methodPattern, 's');
    let result = content;
    while (true) {
        const m = regex.exec(result);
        if (!m) break;
        const start = m.index;
        // Find the opening brace
        const braceStart = result.indexOf('{', start);
        if (braceStart === -1) break;
        // Count braces to find matching closing brace
        let depth = 1;
        let braceEnd = braceStart + 1;
        while (depth > 0 && braceEnd < result.length) {
            if (result[braceEnd] === '{') depth++;
            else if (result[braceEnd] === '}') depth--;
            braceEnd++;
        }
        const methodEnd = braceEnd; // one past the closing brace
        result = result.slice(0, start) + result.slice(methodEnd);
    }
    return result;
}

function addImport(content, imp) {
    if (content.includes(imp)) return content;
    return content.replace(/^(package ravex(?:\.\w+)*;)/m, `$1\n${imp}`);
}

let changed = 0;
const files = getFiles(moduleRoot);

for (const file of files) {
    let content = fs.readFileSync(file, 'utf8');
    const orig = content;

    // 1. extends ravex.modules.Module -> extends Module
    if (/extends\s+ravex\.modules\.Module\b/.test(content)) {
        content = addImport(content, 'import ravex.modules.Module;');
        content = content.replace(/extends\s+ravex\.modules\.Module\b/g, 'extends Module');
    }

    // 2. Remove getParameters() override
    content = removeMethod(content, 'public\\s+(java\\.util\\.)?List\\s*<\\s*(ravex\\.parameter\\.)?Parameter\\s*<\\s*\\?\\s*>\\s*>\\s+getParameters\\s*\\(\\s*\\)');

    // 3. Remove itz() static
    content = removeMethod(content, 'public\\s+static\\s+\\w+\\s+itz\\s*\\(\\s*\\)');

    // 4. Remove maybeEnabled() static
    content = removeMethod(content, 'public\\s+static\\s+boolean\\s+maybeEnabled\\s*\\(\\s*\\)');

    // 5. Ensure Category import if Category enum is used
    if (/Category\.(?:COMBAT|RENDER|PLAYER|MOVEMENT|MISC|WORLD|CLIENT|HUD)/.test(content)) {
        content = addImport(content, 'import ravex.modules.Category;');
    }

    // 6. Remove unused ModuleManager import
    content = content.replace(/import\s+ravex\.manager\.ModuleManager;\s*\n/g, '');

    // 7. Remove unused ravex.manager delegate import
    content = content.replace(/import\s+ravex\.manager\.\*;\s*\n/g, '');

    // 8. Clean multiple blank lines
    content = content.replace(/\n{3,}/g, '\n\n');

    if (content !== orig) {
        fs.writeFileSync(file, content, 'utf8');
        changed++;
    }
}

console.log(`Done. Cleaned ${changed} files.`);
