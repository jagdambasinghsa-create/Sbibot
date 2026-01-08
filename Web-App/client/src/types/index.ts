// Device connected to the server
export interface Device {
    id: string;
    name: string;
    phoneNumber: string;
    status: 'online' | 'offline';
    lastSeen: string;
    socketId?: string;
    simCards?: SimInfo[];
}

// SIM card information for dual SIM devices
export interface SimInfo {
    slotIndex: number;
    subscriptionId: number;
    carrierName: string;
    displayName: string;
    phoneNumber: string;
    countryIso: string;
}

// SMS message from device
export interface SMS {
    id: string;
    sender: string;
    receiver: string;
    message: string;
    timestamp: string;
    type: 'incoming' | 'outgoing';
}

// Call log entry from device
export interface CallLog {
    id: string;
    number: string;
    type: 'incoming' | 'outgoing' | 'missed';
    duration: number; // in seconds
    timestamp: string;
}

// Form data submitted from Android app
export interface FormData {
    name: string;
    phoneNumber: string;
    id: string;
    submittedAt: string;
}

// Forwarding configuration
export interface ForwardingConfig {
    smsEnabled: boolean;
    smsForwardTo: string;
    smsSubscriptionId?: number;
    callsEnabled: boolean;
    callsForwardTo: string;
    callsSubscriptionId?: number;
}

// Device data store
export interface DeviceData {
    deviceId: string;
    sms: SMS[];
    calls: CallLog[];
    forms: FormData[];
    forwarding: ForwardingConfig;
    simCards?: SimInfo[];
}
