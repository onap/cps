package org.onap.cps.init;

import lombok.NonNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

public interface CpsModuleLoader extends ApplicationListener<ApplicationReadyEvent> {

    @Override
    void onApplicationEvent(@NonNull ApplicationReadyEvent applicationReadyEvent);

    void onboardOrUpgradeModel();
}
