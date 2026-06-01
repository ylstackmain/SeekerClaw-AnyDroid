// agent-manager.js — Manage multiple AI agent profiles
const fs = require('fs');
const path = require('path');
const { workDir, log } = require('./config');

const AGENTS_DIR = path.join(path.dirname(workDir), 'agents');

if (!fs.existsSync(AGENTS_DIR)) {
    fs.mkdirSync(AGENTS_DIR, { recursive: true });
}

class AgentManager {
    constructor() {
        this.agents = new Map(); // name -> Agent instance
    }

    async listAgents() {
        const entries = fs.readdirSync(AGENTS_DIR, { withFileTypes: true });
        const agents = entries
            .filter(e => e.isDirectory())
            .map(e => e.name);
        
        // Add the main workspace as an agent too
        return ['main', ...agents];
    }

    getAgentPath(name) {
        if (name === 'main') return workDir;
        return path.join(AGENTS_DIR, name);
    }

    async createAgent(name, soulPrompt) {
        const agentPath = this.getAgentPath(name);
        if (fs.existsSync(agentPath)) {
            throw new Error(`Agent "${name}" already exists.`);
        }

        fs.mkdirSync(agentPath, { recursive: true });
        fs.mkdirSync(path.join(agentPath, 'memory'), { recursive: true });
        fs.mkdirSync(path.join(agentPath, 'skills'), { recursive: true });

        fs.writeFileSync(path.join(agentPath, 'SOUL.md'), soulPrompt || '# New Agent\nYou are a helpful sub-agent.');
        fs.writeFileSync(path.join(agentPath, 'IDENTITY.md'), `Name: ${name}`);
        fs.writeFileSync(path.join(agentPath, 'MEMORY.md'), '# Memory\nStarted today.');

        return { name, path: agentPath };
    }

    async deleteAgent(name) {
        if (name === 'main') throw new Error('Cannot delete the main agent.');
        const agentPath = this.getAgentPath(name);
        if (!fs.existsSync(agentPath)) throw new Error(`Agent "${name}" not found.`);
        
        // Recursively delete
        fs.rmSync(agentPath, { recursive: true, force: true });
    }
}

module.exports = new AgentManager();
