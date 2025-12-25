package com.healthdata.sales.entity;

/**
 * Type of healthcare organization
 */
public enum OrganizationType {
    ACO,           // Accountable Care Organization
    HEALTH_SYSTEM, // Integrated Health System
    PAYER,         // Health Insurance / Medicare Advantage
    HIE,           // Health Information Exchange
    FQHC,          // Federally Qualified Health Center
    CLINIC,        // Ambulatory Clinic/Practice
    HOSPITAL,      // Standalone Hospital
    OTHER
}
