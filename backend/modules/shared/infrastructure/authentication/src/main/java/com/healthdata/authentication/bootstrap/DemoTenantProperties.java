package com.healthdata.authentication.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hdim.demo-tenant")
public class DemoTenantProperties {

    private boolean enabled = true;
    private String id = "demo";
    private String name = "HDIM Demo";
    private String adminUsername = "demo_admin";
    private String adminEmail = "demo_admin@hdim.local";
    private String adminPassword = "changeme123";
    private String analystUsername = "demo_analyst";
    private String analystEmail = "demo_analyst@hdim.local";
    private String viewerUsername = "demo_viewer";
    private String viewerEmail = "demo_viewer@hdim.local";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
    public String getAnalystUsername() { return analystUsername; }
    public void setAnalystUsername(String analystUsername) { this.analystUsername = analystUsername; }
    public String getAnalystEmail() { return analystEmail; }
    public void setAnalystEmail(String analystEmail) { this.analystEmail = analystEmail; }
    public String getViewerUsername() { return viewerUsername; }
    public void setViewerUsername(String viewerUsername) { this.viewerUsername = viewerUsername; }
    public String getViewerEmail() { return viewerEmail; }
    public void setViewerEmail(String viewerEmail) { this.viewerEmail = viewerEmail; }
}
