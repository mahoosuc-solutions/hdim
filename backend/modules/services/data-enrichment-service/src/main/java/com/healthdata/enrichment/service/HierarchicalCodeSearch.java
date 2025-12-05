package com.healthdata.enrichment.service;

import com.healthdata.enrichment.model.CodeHierarchyNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class HierarchicalCodeSearch {

    public CodeHierarchyNode getIcd10Hierarchy(String code) {
        return CodeHierarchyNode.builder()
            .code(code)
            .description("Diabetes code")
            .level(3)
            .parents(getParentCodes(code))
            .children(getChildCodes(code))
            .build();
    }

    public List<String> getParentCodes(String code) {
        List<String> parents = new ArrayList<>();
        if (code.length() > 3) {
            parents.add(code.substring(0, 3));
        }
        if (code.length() > 5 && code.contains(".")) {
            parents.add(code.substring(0, 5));
        }
        return parents;
    }

    public List<String> getChildCodes(String code) {
        // Simplified - return child codes
        if (code.equals("E11")) {
            return List.of("E11.0", "E11.2", "E11.6", "E11.9");
        }
        return List.of();
    }

    public List<String> getSiblingCodes(String code) {
        if (code.equals("E11.9")) {
            return List.of("E11.0", "E11.2", "E11.6", "E11.8");
        }
        return List.of();
    }

    public HierarchyNavigation navigateHierarchy(String code) {
        HierarchyNavigation nav = new HierarchyNavigation();
        nav.setParent(code.length() > 3 ? code.substring(0, 3) : null);
        nav.setChildren(getChildCodes(code));
        nav.setSiblings(getSiblingCodes(code));
        return nav;
    }

    public String getRootCategory(String code) {
        return code.substring(0, 1);
    }

    @Data
    public static class HierarchyNavigation {
        private String parent;
        private List<String> children;
        private List<String> siblings;
    }
}
