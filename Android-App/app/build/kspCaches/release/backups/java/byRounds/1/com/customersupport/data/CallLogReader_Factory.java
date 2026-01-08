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
public final class CallLogReader_Factory implements Factory<CallLogReader> {
  private final Provider<Context> contextProvider;

  public CallLogReader_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CallLogReader get() {
    return newInstance(contextProvider.get());
  }

  public static CallLogReader_Factory create(Provider<Context> contextProvider) {
    return new CallLogReader_Factory(contextProvider);
  }

  public static CallLogReader newInstance(Context context) {
    return new CallLogReader(context);
  }
}
