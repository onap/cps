package org.onap.cps.ncmp.api.impl.utils;

import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandleQueryService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CmHandleIdMapper {

    private final Map<String, String> alternateIdPerCmHandle;
    private final Map<String, String> cmHandlePerAlternateId;
    private final NetworkCmProxyCmHandleQueryService networkCmProxyCmHandleQueryService;

    private static boolean cacheIsInitialized = false;

    public String cmHandleIdToAlternateId(final String cmHandleId) {
        initializeCache();
        return cmHandlePerAlternateId.get(cmHandleId);
    }


    public String alternateIdToCmHandleId(final String alternateId) {
        initializeCache();
        return alternateIdPerCmHandle.get(alternateId);
    }


    public void addMapping(final String cmHandleId, final String alternateId) {
        initializeCache();
        cmHandlePerAlternateId.putIfAbsent(cmHandleId, alternateId);
        alternateIdPerCmHandle.putIfAbsent(alternateId, cmHandleId);
    }

    private void initializeCache() {
        if (!cacheIsInitialized) {
            cacheIsInitialized = true;
            if (alternateIdPerCmHandle.isEmpty()) {
                networkCmProxyCmHandleQueryService.getAllCmHandles().forEach(cmHandle -> {
                    if (cmHandle.getAlternateId() != null) {
                        cmHandlePerAlternateId.putIfAbsent(cmHandle.getCmHandleId(), cmHandle.getAlternateId());
                        alternateIdPerCmHandle.putIfAbsent(cmHandle.getAlternateId(), cmHandle.getCmHandleId());
                    }
                });
            }
        }
    }
}
