package com.healthdata.gateway.admin.configversion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "config.promotions")
public class ConfigPromotionProperties {

    private String demoTenantId = "demo-clinic";
    private boolean requireDemo = true;
    private boolean requireTwoPersonApproval = true;

    public String getDemoTenantId() {
        return demoTenantId;
    }

    public void setDemoTenantId(String demoTenantId) {
        this.demoTenantId = demoTenantId;
    }

    public boolean isRequireDemo() {
        return requireDemo;
    }

    public void setRequireDemo(boolean requireDemo) {
        this.requireDemo = requireDemo;
    }

    public boolean isRequireTwoPersonApproval() {
        return requireTwoPersonApproval;
    }

    public void setRequireTwoPersonApproval(boolean requireTwoPersonApproval) {
        this.requireTwoPersonApproval = requireTwoPersonApproval;
    }
}
