const fs = require('fs');
const path = require('path');

const gradleProps = fs.readFileSync(path.join(__dirname, '..', 'gradle.properties'), 'utf-8');
const match = gradleProps.match(/mod_version\s*=\s*(.+)/);
const baseVersion = match ? match[1].trim() : 'unknown';

/** @type {import('next').NextConfig} */
const nextConfig = {
  env: {
    RAVEX_VERSION: baseVersion + ' Recode',
    RAVEX_JAR_VERSION: baseVersion,
  },
}

module.exports = nextConfig
