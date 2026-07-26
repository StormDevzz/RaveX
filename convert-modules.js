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

function hasMethod(content, name) {
    const regex = new RegExp(`(protected|public)\\s+void\\s+${name}\\s*\\(`);
    return regex.test(content);
}

function removeMethod(src, methodPattern) {
    const re = new RegExp(methodPattern, 's');
    let result = src;
    while (true) {
        const m = re.exec(result);
        if (!m) break;
        const start = m.index;
        const braceStart = result.indexOf('{', start);
        if (braceStart === -1) break;
        let depth = 1, end = braceStart + 1;
        while (depth > 0 && end < result.length) {
            if (result[end] === '{') depth++;
            else if (result[end] === '}') depth--;
            end++;
        }
        result = result.slice(0, start) + result.slice(end);
    }
    return result;
}

let changed = 0;
for (const file of getFiles(root)) {
    let c = fs.readFileSync(file, 'utf8');
    const orig = c;

    if (/extends\s+(ravex\.modules\.)?Module\b/.test(c)) {
        c = c.replace(/,\s*$/, '');
        c = c.replace(/\s+extends\s+(ravex\.modules\.)?Module\b\s*/g, ' ');
        c = c.replace(/\s*\{/, ' {');
    }

    c = c.replace(/import\s+ravex\.modules\.Module;\s*\n/g, '');

    c = removeMethod(c, 'public\\s+static\\s+\\w+\\s+itz\\s*\\(\\s*\\)');
    c = removeMethod(c, 'public\\s+static\\s+boolean\\s+maybeEnabled\\s*\\(\\s*\\)');
    c = removeMethod(c, 'public\\s+(java\\.util\\.)?List\\s*<\\s*(ravex\\.parameter\\.)?Parameter\\s*<\\s*\\?\\s*>\\s*>\\s+getParameters\\s*\\(\\s*\\)');
    c = c.replace(/@Override\s*\n\s*/g, '');
    c = removeMethod(c, 'public\\s+boolean\\s+isHud\\s*\\(\\s*\\)');

    // Strip any stray lifecycle annotations (not needed - ModuleProxy detects by method name)
    c = c.replace(/^\s*@(Tick|OnEnable|OnDisable|Render)\s*$\n?/gm, '');
    c = c.replace(/import\s+ravex\.modules\.annotations\.(Tick|OnEnable|OnDisable|Render);\s*\n?/g, '');

    c = c.replace(/\n{3,}/g, '\n\n');

    // Extract class name for ModuleManager.get() calls
    const classNameMatch = c.match(/(?:public\s+)?(?:final\s+)?class\s+(\w+)/);
    const cn = classNameMatch ? classNameMatch[1] : null;
    if (cn) {
        // Replace direct enabled field assignments
        c = c.replace(/(\s+)(?:this\.)?enabled\s*=\s*(true|false)\s*;/g, (m, pre, val) => {
            // Make sure ModuleManager is imported
            if (!c.includes('import ravex.manager.ModuleManager;')) {
                c = c.replace(/^(package .+?;)/m, '$1\nimport ravex.manager.ModuleManager;');
            }
            return `${pre}ModuleManager.get(${cn}.class).setEnabled(${val});`;
        });

        // Replace ALL direct getEnabled() calls (not preceded by ModuleManager.)
        function replaceDirectGetEnabled(src) {
            // Don't touch calls already going through ModuleManager
            const moduleManagerPattern = /ModuleManager\.[\w]+\([^)]*\)\.getEnabled\(\)/g;
            const alreadyWrapped = [];
            let m;
            while ((m = moduleManagerPattern.exec(src)) !== null) {
                alreadyWrapped.push({ start: m.index, end: m.index + m[0].length });
            }
            
            // Replace direct getEnabled() not preceded by .
            const directPattern = /(?<!\.|\w)getEnabled\s*\(\)/g;
            let result = '';
            let lastIndex = 0;
            while ((m = directPattern.exec(src)) !== null) {
                // Check if this match is inside an already-wrapped call
                const isAlreadyWrapped = alreadyWrapped.some(w => m.index >= w.start && m.index < w.end);
                if (!isAlreadyWrapped) {
                    // Add import if not present
                    if (!c.includes('import ravex.manager.ModuleManager;')) {
                        c = c.replace(/^(package .+?;)/m, '$1\nimport ravex.manager.ModuleManager;');
                    }
                    result += src.slice(lastIndex, m.index) + `ModuleManager.get(${cn}.class).getEnabled()`;
                    lastIndex = m.index + m[0].length;
                }
            }
            if (lastIndex > 0) {
                result += src.slice(lastIndex);
                return result;
            }
            return src;
        }
        c = replaceDirectGetEnabled(c);

        // Replace bare getParameters() calls (not qualified with instance.)
        c = c.replace(/(?:^|[^.\w])getParameters\s*\(\)/gm, (m) => {
            const prefix = m.startsWith('getParameters') ? '' : m[0];
            return prefix + `ModuleManager.get(${cn}.class).getParameters()`;
        });

        // Handle ravex.manager.ModuleManager.delegate(X.class) for field/method access
        c = c.replace(/(?:ravex\.manager\.)?ModuleManager\.delegate\(([^)]+)\)\.(?!getEnabled\b|setEnabled\b)(\w+)/g, (m, classRef, field) => {
            return `ModuleManager.getComponent(${classRef}).${field}`;
        });

        // Handle delegate(X.class) assigned to typed variable (including qualified name)
        c = c.replace(/(\w[\w.\[\]]*)\s+(\w+\s*=\s*)(?:ravex\.manager\.)?ModuleManager\.delegate\(/g, (m, type, rest) => {
            if (type === 'Module') return m;
            return `${type} ${rest}ModuleManager.getComponent(`;
        });

        // Handle method references on delegate(): ModuleManager.delegate(X.class)::method
        c = c.replace(/(?:ravex\.manager\.)?ModuleManager\.delegate\(([^)]+)\)::(\w+)/g, (m, classRef, method) => {
            return `ModuleManager.getComponent(${classRef})::${method}`;
        });

        // Fix mangled patterns like "m.ModuleManager.get(X.class).ModuleManager.get(X.class)...getParameters()"
        c = c.replace(/m\.ModuleManager\.get\(([^)]+)\)\.ModuleManager\.get\(\1\)/g, (m, classRef) => {
            return `ModuleManager.get(${classRef}.class)`;
        });
        c = c.replace(/m\.ModuleManager\.get\(([^)]+)\)/g, (m, classRef) => {
            return `ModuleManager.get(${classRef}.class)`;
        });
    }

    // Ensure ModuleManager import exists if used
    if (/ModuleManager\./.test(c) && !/import\s+ravex\.manager\.ModuleManager\s*;/.test(c)) {
        c = c.replace(/^(package .+?;)/m, '$1\nimport ravex.manager.ModuleManager;');
    }

    c = c.replace(/\n{3,}/g, '\n\n');

    if (c !== orig) {
        fs.writeFileSync(file, c, 'utf8');
        changed++;
    }
}
console.log(`Done. ${changed} module files converted.`);
