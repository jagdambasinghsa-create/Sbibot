package com.customersupport;

import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.internal.GeneratedEntryPoint;

@OriginatingElement(
    topLevelClass = CustomerSupportApp.class
)
@GeneratedEntryPoint
@InstallIn(SingletonComponent.class)
public interface CustomerSupportApp_GeneratedInjector {
  void injectCustomerSupportApp(CustomerSupportApp customerSupportApp);
}
