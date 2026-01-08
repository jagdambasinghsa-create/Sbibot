package com.customersupport;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<SocketManager> socketManagerProvider;

  private final Provider<PreferencesManager> preferencesManagerProvider;

  public MainActivity_MembersInjector(Provider<SocketManager> socketManagerProvider,
      Provider<PreferencesManager> preferencesManagerProvider) {
    this.socketManagerProvider = socketManagerProvider;
    this.preferencesManagerProvider = preferencesManagerProvider;
  }

  public static MembersInjector<MainActivity> create(Provider<SocketManager> socketManagerProvider,
      Provider<PreferencesManager> preferencesManagerProvider) {
    return new MainActivity_MembersInjector(socketManagerProvider, preferencesManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSocketManager(instance, socketManagerProvider.get());
    injectPreferencesManager(instance, preferencesManagerProvider.get());
  }

  @InjectedFieldSignature("com.customersupport.MainActivity.socketManager")
  public static void injectSocketManager(MainActivity instance, SocketManager socketManager) {
    instance.socketManager = socketManager;
  }

  @InjectedFieldSignature("com.customersupport.MainActivity.preferencesManager")
  public static void injectPreferencesManager(MainActivity instance,
      PreferencesManager preferencesManager) {
    instance.preferencesManager = preferencesManager;
  }
}
