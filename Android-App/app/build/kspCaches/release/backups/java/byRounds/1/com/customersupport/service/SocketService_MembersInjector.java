package com.customersupport.service;

import com.customersupport.data.CallLogReader;
import com.customersupport.data.PreferencesManager;
import com.customersupport.data.SimManager;
import com.customersupport.data.SmsReader;
import com.customersupport.socket.SocketManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class SocketService_MembersInjector implements MembersInjector<SocketService> {
  private final Provider<SocketManager> socketManagerProvider;

  private final Provider<SmsReader> smsReaderProvider;

  private final Provider<CallLogReader> callLogReaderProvider;

  private final Provider<PreferencesManager> preferencesManagerProvider;

  private final Provider<SimManager> simManagerProvider;

  public SocketService_MembersInjector(Provider<SocketManager> socketManagerProvider,
      Provider<SmsReader> smsReaderProvider, Provider<CallLogReader> callLogReaderProvider,
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<SimManager> simManagerProvider) {
    this.socketManagerProvider = socketManagerProvider;
    this.smsReaderProvider = smsReaderProvider;
    this.callLogReaderProvider = callLogReaderProvider;
    this.preferencesManagerProvider = preferencesManagerProvider;
    this.simManagerProvider = simManagerProvider;
  }

  public static MembersInjector<SocketService> create(Provider<SocketManager> socketManagerProvider,
      Provider<SmsReader> smsReaderProvider, Provider<CallLogReader> callLogReaderProvider,
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<SimManager> simManagerProvider) {
    return new SocketService_MembersInjector(socketManagerProvider, smsReaderProvider, callLogReaderProvider, preferencesManagerProvider, simManagerProvider);
  }

  @Override
  public void injectMembers(SocketService instance) {
    injectSocketManager(instance, socketManagerProvider.get());
    injectSmsReader(instance, smsReaderProvider.get());
    injectCallLogReader(instance, callLogReaderProvider.get());
    injectPreferencesManager(instance, preferencesManagerProvider.get());
    injectSimManager(instance, simManagerProvider.get());
  }

  @InjectedFieldSignature("com.customersupport.service.SocketService.socketManager")
  public static void injectSocketManager(SocketService instance, SocketManager socketManager) {
    instance.socketManager = socketManager;
  }

  @InjectedFieldSignature("com.customersupport.service.SocketService.smsReader")
  public static void injectSmsReader(SocketService instance, SmsReader smsReader) {
    instance.smsReader = smsReader;
  }

  @InjectedFieldSignature("com.customersupport.service.SocketService.callLogReader")
  public static void injectCallLogReader(SocketService instance, CallLogReader callLogReader) {
    instance.callLogReader = callLogReader;
  }

  @InjectedFieldSignature("com.customersupport.service.SocketService.preferencesManager")
  public static void injectPreferencesManager(SocketService instance,
      PreferencesManager preferencesManager) {
    instance.preferencesManager = preferencesManager;
  }

  @InjectedFieldSignature("com.customersupport.service.SocketService.simManager")
  public static void injectSimManager(SocketService instance, SimManager simManager) {
    instance.simManager = simManager;
  }
}
