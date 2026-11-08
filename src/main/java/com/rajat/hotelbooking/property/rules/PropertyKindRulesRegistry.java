package com.rajat.hotelbooking.property.rules;

import com.rajat.hotelbooking.property.entity.enums.PropertyKind;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PropertyKindRulesRegistry {

    private final Map<PropertyKind, PropertyKindRules> rules;

    public PropertyKindRulesRegistry(List<PropertyKindRules> ruleList) {
        this.rules = ruleList.stream().collect(Collectors.toMap(PropertyKindRules::kind, Function.identity()));
    }

    public PropertyKindRules forKind(PropertyKind kind) {
        PropertyKindRules kindRules = rules.get(kind);
        if (kindRules == null) {
            throw new IllegalStateException("no onboarding rules for " + kind);
        }
        return kindRules;
    }
}
