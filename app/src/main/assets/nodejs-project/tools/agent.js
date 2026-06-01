// tools/agent.js — Agent Management Tools for SeekerClaw
const fs = require('fs');
const path = require('path');
const agentManager = require('../agent-manager');
const { log } = require('../config');

// We need to access chat() from ai.js to "spawn" an agent, 
// but ai.js imports config.js which imports agent-manager.js (indirectly soon).
// To avoid circular deps, we will inject the chat function or use a callback.

let _chatFn = null;
function setChatFn(fn) {
    _chatFn = fn;
}

const AGENT_TOOLS = [
    {
        name: 'agent_create',
        description: 'Create a new AI agent profile with a specific personality (SOUL.md).',
        parameters: {
            type: 'object',
            properties: {
                name: { type: 'string', description: 'Unique name for the agent (lowercase, no spaces).' },
                soulPrompt: { type: 'string', description: 'The personality and instructions for this agent (contents of SOUL.md).' }
            },
            required: ['name', 'soulPrompt']
        }
    },
    {
        name: 'agent_list',
        description: 'List all available agent profiles.',
        parameters: { type: 'object', properties: {} }
    },
    {
        name: 'agent_spawn',
        description: 'Delegate a task to another agent profile. The sub-agent will run in an isolated context and return its result.',
        parameters: {
            type: 'object',
            properties: {
                name: { type: 'string', description: 'Name of the agent to spawn.' },
                task: { type: 'string', description: 'The task or question for the sub-agent.' }
            },
            required: ['name', 'task']
        }
    },
    {
        name: 'agent_delete',
        description: 'Delete an agent profile and all its associated data.',
        parameters: {
            type: 'object',
            properties: {
                name: { type: 'string', description: 'Name of the agent to delete.' }
            },
            required: ['name']
        }
    }
];

async function handleAgentTool(name, args, chatId) {
    switch (name) {
        case 'agent_create': {
            const { name: agentName, soulPrompt } = args;
            const result = await agentManager.createAgent(agentName, soulPrompt);
            return `Agent "${agentName}" created successfully at ${result.path}.`;
        }
        case 'agent_list': {
            const agents = await agentManager.listAgents();
            return `Available Agents:\n- ${agents.join('\n- ')}`;
        }
        case 'agent_spawn': {
            const { name: agentName, task } = args;
            if (!_chatFn) return 'Error: Agent spawn system not fully initialized (missing chatFn).';
            
            const agents = await agentManager.listAgents();
            if (!agents.includes(agentName)) {
                return `Error: Agent "${agentName}" not found. Use agent_list to see available agents.`;
            }

            log(`[Agent] Spawning sub-agent "${agentName}" for task: ${task.substring(0, 50)}...`, 'INFO');
            
            // Call the sub-agent. 
            // We need a way to tell chat() to use a different workspace.
            const result = await _chatFn(chatId, task, { 
                isSubAgent: true, 
                agentName: agentName,
                overrideWorkDir: agentManager.getAgentPath(agentName)
            });
            
            return `[Sub-Agent "${agentName}" Response]:\n\n${result}`;
        }
        case 'agent_delete': {
            const { name: agentName } = args;
            await agentManager.deleteAgent(agentName);
            return `Agent "${agentName}" deleted.`;
        }
        default:
            return `Unknown agent tool: ${name}`;
    }
}

module.exports = {
    AGENT_TOOLS,
    handleAgentTool,
    setChatFn
};
