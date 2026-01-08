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
public final class SmsReader_Factory implements Factory<SmsReader> {
  private final Provider<Context> contextProvider;

  public SmsReader_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SmsReader get() {
    return newInstance(contextProvider.get());
  }

  public static SmsReader_Factory create(Provider<Context> contextProvider) {
    return new SmsReader_Factory(contextProvider);
  }

  public static SmsReader newInstance(Context context) {
    return new SmsReader(context);
  }
}
