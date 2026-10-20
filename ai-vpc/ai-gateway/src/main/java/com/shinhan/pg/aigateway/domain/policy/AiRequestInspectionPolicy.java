package com.shinhan.pg.aigateway.domain.policy;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/** Finite, versioned deny rules. Unknown/encoded attacks are outside the guarantee of this MVP. */
public final class AiRequestInspectionPolicy {
    public static final String VERSION = "preparation-rules-v1";
    private static final List<Pattern> DENY = List.of(
            Pattern.compile("(?:ignore|disregard|forget|override).{0,60}(?:previous|prior|above|system).{0,40}(?:instructions?|prompts?|rules?)"),
            Pattern.compile("(?:reveal|show|print|leak|exfiltrate|repeat).{0,60}(?:system\\s*(?:prompt|instructions?)|api[ _-]?key|password|secret|access[ _-]?token)"),
            Pattern.compile("(?:이전|기존|위의|시스템).{0,30}(?:지시|명령|규칙|프롬프트).{0,30}(?:무시|잊|덮어|출력|공개|보여)"),
            Pattern.compile("(?:비밀|비밀번호|인증\\s*토큰|api\\s*키).{0,30}(?:출력|공개|전송|보여|유출)"),
            Pattern.compile("(?:jailbreak|do anything now|developer mode|탈옥|개발자 모드)"),
            Pattern.compile("(?:<\\|(?:im_start|system|assistant)|\\[inst]|<\\s*/?\\s*(?:system|assistant|developer)\\s*>|(?:^|\\s)(?:system|developer)\\s*:)"));

    public boolean blocks(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
        return DENY.stream().anyMatch(rule -> rule.matcher(normalized).find());
    }
}
