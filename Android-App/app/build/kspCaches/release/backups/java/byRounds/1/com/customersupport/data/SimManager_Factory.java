package com.customersupport.data;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SimManager_Factory implements Factory<SimManager> {
  private final Provider<Context> contextProvider;

  public SimManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SimManager get() {
    return newInstance(contextProvider.get());
  }

  public static SimManager_Factory create(Provider<Context> contextProvider) {
    return new SimManager_Factory(contextProvider);
  }

  public static SimManager newInstance(Context context) {
    return new SimManager(context);
  }
}
