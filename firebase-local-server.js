/**
 * Mess Management System - Local Firebase Realtime Database Server
 * 
 * Runs a standalone local HTTP server on http://localhost:9000 that mimics
 * the Google Firebase Realtime Database REST API.
 * Uses native Node.js (zero external npm dependencies, modern WHATWG URL API).
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 9000;
const DATA_FILE = path.join(__dirname, 'resources', 'firebase-data.json');
const INITIAL_SEED_FILE = path.join(__dirname, 'firebase', 'initial-data.json');

// In-memory data store for sub-millisecond responses and concurrency safety
let inMemoryDb = loadDatabase();

function loadDatabase() {
    try {
        if (fs.existsSync(DATA_FILE)) {
            const raw = fs.readFileSync(DATA_FILE, 'utf8');
            const parsed = JSON.parse(raw);
            if (parsed && typeof parsed === 'object' && Object.keys(parsed).length > 0) {
                return parsed;
            }
        }
    } catch (err) {
        console.error('[Database Load Warning]:', err.message);
    }

    // Fallback to initial seed data
    try {
        if (fs.existsSync(INITIAL_SEED_FILE)) {
            const raw = fs.readFileSync(INITIAL_SEED_FILE, 'utf8');
            return JSON.parse(raw);
        }
    } catch (err) {
        console.error('[Seed Load Warning]:', err.message);
    }

    return {};
}

// Atomic & debounced disk persistence
let saveTimeout = null;
function persistDatabase() {
    if (saveTimeout) clearTimeout(saveTimeout);
    saveTimeout = setTimeout(() => {
        try {
            const dir = path.dirname(DATA_FILE);
            if (!fs.existsSync(dir)) {
                fs.mkdirSync(dir, { recursive: true });
            }
            const tmpFile = DATA_FILE + '.tmp';
            fs.writeFileSync(tmpFile, JSON.stringify(inMemoryDb, null, 2), 'utf8');
            fs.renameSync(tmpFile, DATA_FILE);
        } catch (err) {
            console.error('[Disk Write Warning]:', err.message);
        }
    }, 100);
}

// Traverse object path (e.g. "students/STU101")
function getPath(obj, segments) {
    let curr = obj;
    for (const seg of segments) {
        if (!curr || typeof curr !== 'object') return undefined;
        curr = curr[seg];
    }
    return curr;
}

// Set object path
function setPath(obj, segments, value) {
    let curr = obj;
    for (let i = 0; i < segments.length - 1; i++) {
        const seg = segments[i];
        if (!curr[seg] || typeof curr[seg] !== 'object') {
            curr[seg] = {};
        }
        curr = curr[seg];
    }
    const last = segments[segments.length - 1];
    curr[last] = value;
}

// Delete object path
function deletePath(obj, segments) {
    let curr = obj;
    for (let i = 0; i < segments.length - 1; i++) {
        const seg = segments[i];
        if (!curr[seg] || typeof curr[seg] !== 'object') return false;
        curr = curr[seg];
    }
    const last = segments[segments.length - 1];
    if (curr && typeof curr === 'object' && last in curr) {
        delete curr[last];
        return true;
    }
    return false;
}

const server = http.createServer((req, res) => {
    // Enable CORS for any client / browser
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, PUT, PATCH, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Accept');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    // Modern WHATWG URL parsing (no deprecation warning)
    const parsedUrl = new URL(req.url, `http://localhost:${PORT}`);
    let pathname = decodeURIComponent(parsedUrl.pathname);

    // Root shallow / health-check query
    if (pathname === '/' || pathname === '/.json') {
        if (req.method === 'GET') {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(inMemoryDb));
            return;
        }
    }

    // Ignore browser favicon requests
    if (pathname.includes('favicon.ico')) {
        res.writeHead(204);
        res.end();
        return;
    }

    // Strip trailing .json
    if (pathname.endsWith('.json')) {
        pathname = pathname.substring(0, pathname.length - 5);
    }

    // Clean leading/trailing slashes
    pathname = pathname.replace(/^\/+|\/+$/g, '');
    const segments = pathname.length > 0 ? pathname.split('/') : [];

    let body = '';
    req.on('data', chunk => {
        body += chunk;
    });

    req.on('end', () => {
        const timestamp = new Date().toLocaleTimeString();

        try {
            if (req.method === 'GET') {
                if (segments.length === 0) {
                    res.writeHead(200, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify(inMemoryDb));
                } else {
                    const result = getPath(inMemoryDb, segments);
                    res.writeHead(200, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify(result !== undefined ? result : null));
                }
                console.log(`[${timestamp}] GET /${pathname}.json -> 200 OK`);
            } else if (req.method === 'PUT') {
                const parsedBody = body.trim() ? JSON.parse(body) : null;
                if (segments.length === 0) {
                    inMemoryDb = parsedBody || {};
                } else {
                    setPath(inMemoryDb, segments, parsedBody);
                }
                persistDatabase();
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify(parsedBody));
                console.log(`[${timestamp}] PUT /${pathname}.json -> 200 OK`);
            } else if (req.method === 'PATCH') {
                const parsedBody = body.trim() ? JSON.parse(body) : {};
                const current = getPath(inMemoryDb, segments);
                const merged = Object.assign({}, typeof current === 'object' ? current : {}, parsedBody);
                setPath(inMemoryDb, segments, merged);
                persistDatabase();
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify(merged));
                console.log(`[${timestamp}] PATCH /${pathname}.json -> 200 OK`);
            } else if (req.method === 'DELETE') {
                if (segments.length === 0) {
                    inMemoryDb = {};
                } else {
                    deletePath(inMemoryDb, segments);
                }
                persistDatabase();
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify(null));
                console.log(`[${timestamp}] DELETE /${pathname}.json -> 200 OK`);
            } else {
                res.writeHead(405, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Method not allowed' }));
            }
        } catch (err) {
            console.error(`[${timestamp}] Error processing /${pathname}:`, err.message);
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: err.message }));
        }
    });
});

server.listen(PORT, '127.0.0.1', () => {
    console.log('========================================================');
    console.log(`  🔥 Localhost Firebase Realtime Database Server Running!`);
    console.log(`  Endpoint URL: http://localhost:${PORT}/`);
    console.log(`  Database Storage: ${DATA_FILE}`);
    console.log('========================================================');
    console.log('  Press Ctrl+C in this terminal to stop the server.\n');
});
