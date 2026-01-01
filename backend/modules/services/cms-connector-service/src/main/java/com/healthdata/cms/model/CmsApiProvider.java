package com.healthdata.cms.model;

import lombok.Getter;

/**
 * CMS API Provider Enumeration
 * 
 * Represents the different CMS API endpoints that can be integrated.
 */
@Getter
public enum CmsApiProvider {
    BCDA("bcda", "Beneficiary Claims Data API", 
         "https://api.bcda.cms.gov", 
         "https://sandbox.bcda.cms.gov",
         "Weekly bulk Medicare Part A, B, D claims export"),
    
    DPC("dpc", "Data at Point of Care",
        "https://api.dpc.cms.gov",
        "https://sandbox.dpc.cms.gov",
        "Real-time point-of-care claims queries"),
    
    BLUE_BUTTON_2_0("blue-button-2-0", "Blue Button 2.0",
                   "https://api.bluebutton.cms.gov",
                   "https://sandbox.bluebutton.cms.gov",
                   "Beneficiary-initiated data sharing"),
    
    AB2D("ab2d", "Medicare Part D Claims API",
         "https://api.ab2d.cms.gov",
         "https://sandbox.ab2d.cms.gov",
         "Bulk Medicare Part D claims for PDP sponsors"),
    
    QPP_SUBMISSIONS("qpp-submissions", "Quality Payment Program Submissions",
                   "https://api.qpp.cms.gov",
                   "https://sandbox.qpp.cms.gov",
                   "Real-time quality measure submissions");

    private final String id;
    private final String displayName;
    private final String productionUrl;
    private final String sandboxUrl;
    private final String description;

    CmsApiProvider(String id, String displayName, String productionUrl, 
                   String sandboxUrl, String description) {
        this.id = id;
        this.displayName = displayName;
        this.productionUrl = productionUrl;
        this.sandboxUrl = sandboxUrl;
        this.description = description;
    }

    public String getUrl(boolean isSandbox) {
        return isSandbox ? sandboxUrl : productionUrl;
    }

    public static CmsApiProvider fromId(String id) {
        for (CmsApiProvider provider : values()) {
            if (provider.id.equalsIgnoreCase(id)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown CMS API provider: " + id);
    }
}
