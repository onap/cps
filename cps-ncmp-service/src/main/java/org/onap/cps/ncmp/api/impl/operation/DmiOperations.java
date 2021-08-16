package org.onap.cps.ncmp.api.impl.operation;

import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public class DmiOperations {

    private DmiRestClient dmiRestClient;
    private static final String GET_RESOURCE_DATA_FOR_PASSTHROUGH_OPERATIONAL = "/v1/ch/{cmHandle}/data/ds/ncmp-datastore:passthrough-operational/{resourceIdentifier}";
    private int indexCmHandleForGetResource;
    private int indexResourceIdForGetResource;

    /**
     * Constructor for {@code DmiOperations}. This method also manipulates url properties.
     *
     * @param dmiRestClient {@code DmiRestClient}
     */
    public DmiOperations(final DmiRestClient dmiRestClient) {
        this.dmiRestClient = dmiRestClient;
        indexCmHandleForGetResource = GET_RESOURCE_DATA_FOR_PASSTHROUGH_OPERATIONAL.indexOf("{cmHandle}");
        indexResourceIdForGetResource = GET_RESOURCE_DATA_FOR_PASSTHROUGH_OPERATIONAL.indexOf("{resourceIdentifier}");
    }

    /**
     * This method fetches the resource data for given cm handle identifier on given resource
     * using dmi client.
     *
     * @param dmiBasePath dmi base path
     * @param cmHandle network resource identifier
     * @param resourceId resource identifier
     * @param fieldsQuery fields query
     * @param depthQuery depth query
     * @param acceptParam accept parameter
     * @param jsonBody json body for put operation
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getResouceDataFromNode(final String dmiBasePath,
                                                         final String cmHandle,
                                                         final String resourceId,
                                                         final String fieldsQuery,
                                                         final Integer depthQuery,
                                                         final String acceptParam,
                                                         final String jsonBody) {
        final StringBuilder builder = new StringBuilder(dmiBasePath);
        builder.append(GET_RESOURCE_DATA_FOR_PASSTHROUGH_OPERATIONAL);
        replaceCmHandleAndResource(cmHandle, resourceId, builder);
        appendFieldsAndDepth(fieldsQuery, depthQuery, builder);
        final HttpHeaders httpHeaders = prepareHeader(acceptParam);
        return dmiRestClient.putOperationWithJsonData(builder.toString(), jsonBody, httpHeaders);
    }

    private void replaceCmHandleAndResource(final String cmHandle, final String resourceId, final StringBuilder builder) {
        builder.replace(indexCmHandleForGetResource, indexCmHandleForGetResource  + "{cmHandle}".length(), cmHandle);
        builder.replace(indexResourceIdForGetResource, indexResourceIdForGetResource + "{resourceIdentifier}".length(), resourceId);
    }

    private void appendFieldsAndDepth(final String fieldsQuery, final Integer depthQuery, final StringBuilder builder) {
        boolean isFieldExists = fieldsQuery != null && !fieldsQuery.isEmpty();
        if(isFieldExists) {
            builder.append("?").append("fields=").append(fieldsQuery);

        }
        if(depthQuery != null) {
            if(!isFieldExists) {
                builder.append("?");
            }else {
                builder.append("&");
            }
            builder.append("depth=").append(depthQuery);
        }
    }

    private HttpHeaders prepareHeader(final String acceptParam) {
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.ACCEPT, acceptParam);
        return httpHeaders;
    }

}
