package com.shinhan.pg.aigateway.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** AI Gateway only; finance service wire formats remain unchanged. */
@Configuration(proxyBeanMethods = false)
public class AiPreparationJsonConfiguration {
    @Bean public Jackson2ObjectMapperBuilderCustomizer preparationJson() {
        return builder -> {
            builder.featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_TRAILING_TOKENS, JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            builder.postConfigurer(mapper -> {
                mapper.getFactory().setStreamReadConstraints(StreamReadConstraints.builder()
                        .maxNestingDepth(16).maxStringLength(65_536).maxNumberLength(128).build());
                mapper.coercionConfigFor(LogicalType.Textual)
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
            });
        };
    }
}
