package com.customersupport.receiver;

import com.customersupport.data.PreferencesManager;
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
public final class SmsReceiver_MembersInjector implements MembersInjector<SmsReceiver> {
  private final Provider<PreferencesManager> preferencesManagerProvider;

  private final Provider<SocketManager> socketManagerProvider;

  public SmsReceiver_MembersInjector(Provider<PreferencesManager> preferencesManagerProvider,
      Provider<SocketManager> socketManagerProvider) {
    this.preferencesManagerProvider = preferencesManagerProvider;
    this.socketManagerProvider = socketManagerProvider;
  }

  public static MembersInjector<SmsReceiver> create(
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<SocketManager> socketManagerProvider) {
    return new SmsReceiver_MembersInjector(preferencesManagerProvider, socketManagerProvider);
  }

  @Override
  public void injectMembers(SmsReceiver instance) {
    injectPreferencesManager(instance, preferencesManagerProvider.get());
    injectSocketManager(instance, socketManagerProvider.get());
  }

  @InjectedFieldSignature("com.customersupport.receiver.SmsReceiver.preferencesManager")
  public static void injectPreferencesManager(SmsReceiver instance,
      PreferencesManager preferencesManager) {
    instance.preferencesManager = preferencesManager;
  }

  @InjectedFieldSignature("com.customersupport.receiver.SmsReceiver.socketManager")
  public static void injectSocketManager(SmsReceiver instance, SocketManager socketManager) {
    instance.socketManager = socketManager;
  }
}
