package com.shinhan.pg.aigateway.domain.policy;

import com.shinhan.pg.aigateway.domain.error.AiPreparationException;
import com.shinhan.pg.aigateway.domain.model.PiiType;
import com.shinhan.pg.aigateway.domain.model.PreparationReason;
import com.shinhan.pg.aigateway.domain.model.SensitiveValue;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Bounded Korean financial-text rules, not universal PII detection.
 * Figma 1:15 / 19:621: Gateway masks input; names/ambiguous identifiers also need caller hints.
 * All token dictionaries are method-local. Neither the result nor persistence receives their values.
 */
public final class PiiMaskingPolicy {
    public static final int MAX_PROMPT_BYTES = 16_384;
    public static final int MAX_HINTS = 64;
    public static final int MAX_HINT_LENGTH = 256;
    private static final Pattern RESERVED = Pattern.compile("(?i)\\[PII_[^]\\r\\n]*]");
    private static final Pattern RRN = Pattern.compile("(?<![0-9])[0-9]{6}[- ]?[1-8][0-9]{6}(?![0-9])");
    private static final Pattern PHONE = Pattern.compile("(?<![0-9])(?:0(?:1[016789]|2|[3-6][1-5]|70)|\\+82[- ]?(?:1[016789]|2|[3-6][1-5]|70))[- .]?[0-9]{3,4}[- .]?[0-9]{4}(?![0-9])");
    private static final Pattern EMAIL = Pattern.compile("[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}-]+(?:\\.[\\p{L}\\p{N}-]+)+");
    private static final Pattern CARD = Pattern.compile("(?<![0-9])(?:[0-9]{4}[- ]){3}[0-9]{4}(?![0-9])|(?<![0-9])[0-9]{16,19}(?![0-9])");
    private static final Pattern ACCOUNT = Pattern.compile("(?<![0-9])[0-9]{10,15}(?![0-9])|(?<![0-9])(?:[0-9]{2,6}-){1,3}[0-9]{2,8}(?![0-9])");
    private static final Pattern LABELED_NAME = Pattern.compile("(?:이름|성명|고객명|예금주)\\s*[:=]\\s*([가-힣]{2,5})(?![가-힣])");
    // Deliberately conservative: do not certify unsupported long digit runs as clear.
    private static final Pattern UNCLASSIFIED_NUMBER = Pattern.compile("[0-9](?:[- .]?[0-9]){9,}");

    private record Span(int start, int end, PiiType type, int priority) {}
    public record MaskedInput(String text, Map<PiiType, Integer> counts) {
        public MaskedInput { counts = Map.copyOf(counts); }
        @Override public String toString() { return "MaskedInput[REDACTED]"; }
    }

    public String normalize(String text) { return Normalizer.normalize(text, Normalizer.Form.NFKC); }

    public void validate(String prompt, List<SensitiveValue> hints) {
        if (prompt == null || prompt.isBlank() || hints == null || hints.size() > MAX_HINTS)
            throw new AiPreparationException(PreparationReason.INVALID_INPUT);
        if (prompt.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_PROMPT_BYTES)
            throw new AiPreparationException(PreparationReason.INPUT_TOO_LARGE);
        String normalized = normalize(prompt);
        if (normalized.isBlank()) throw new AiPreparationException(PreparationReason.INVALID_INPUT);
        if (unsupported(normalized)) throw new AiPreparationException(PreparationReason.UNSUPPORTED_TEXT);
        if (RESERVED.matcher(normalized).find()) throw new AiPreparationException(PreparationReason.RESERVED_TOKEN);
        var types = new HashMap<String, PiiType>();
        for (SensitiveValue hint : hints) {
            if (hint == null || hint.type() == null || hint.value() == null)
                throw new AiPreparationException(PreparationReason.INVALID_INPUT);
            String v = normalize(hint.value());
            if (v.isBlank() || v.length() < 2 || v.length() > MAX_HINT_LENGTH || unsupported(v)
                    || v.contains("[PII_") || !normalized.contains(v))
                throw new AiPreparationException(PreparationReason.INVALID_INPUT);
            var old = types.putIfAbsent(v, hint.type());
            if (old != null && old != hint.type()) throw new AiPreparationException(PreparationReason.INVALID_INPUT);
        }
    }

    private boolean unsupported(String value) {
        for (int i = 0; i < value.length();) {
            int cp = value.codePointAt(i);
            if (Character.getType(cp) == Character.FORMAT || Character.getType(cp) == Character.SURROGATE
                    || (Character.isISOControl(cp) && cp != '\n' && cp != '\r' && cp != '\t')) return true;
            i += Character.charCount(cp);
        }
        return false;
    }

    public MaskedInput mask(String prompt, List<SensitiveValue> hints) {
        String text = normalize(prompt);
        var spans = detect(text, hints);
        spans.sort(Comparator.comparingInt(Span::start)
                .thenComparing(Comparator.comparingInt(Span::end).reversed())
                .thenComparingInt(Span::priority));
        var counts = new EnumMap<PiiType, Integer>(PiiType.class);
        var tokenIds = new EnumMap<PiiType, Integer>(PiiType.class);
        var tokens = new HashMap<String, String>();
        var result = new StringBuilder();
        int cursor = 0;
        for (int i = 0; i < spans.size();) {
            Span chosen = spans.get(i++);
            if (chosen.start() < cursor) continue;
            int end = chosen.end();
            // Union partially overlapping matches: no unmasked tail can survive an overlap.
            while (i < spans.size() && spans.get(i).start() < end) {
                Span next = spans.get(i++);
                if (next.end() <= end && next.priority() < chosen.priority()
                        && next.start() == chosen.start() && next.end() == end) chosen = next;
                end = Math.max(end, next.end());
            }
            String raw = text.substring(chosen.start(), end);
            String key = chosen.type() + ":" + canonical(chosen.type(), raw);
            PiiType type = chosen.type();
            String token = tokens.computeIfAbsent(key, ignored ->
                    "[PII_" + type + "_" + tokenIds.merge(type, 1, Integer::sum) + "]");
            result.append(text, cursor, chosen.start()).append(token);
            counts.merge(type, 1, Integer::sum);
            cursor = end;
        }
        result.append(text, cursor, text.length());
        return new MaskedInput(result.toString(), counts);
    }

    private String canonical(PiiType type, String value) {
        return switch (type) {
            case RRN, ACCOUNT, PHONE, CARD -> value.replaceAll("[^0-9]", "");
            case EMAIL -> value.toLowerCase(Locale.ROOT);
            case NAME -> value;
        };
    }

    private ArrayList<Span> detect(String text, List<SensitiveValue> hints) {
        var spans = new ArrayList<Span>();
        // Hints have precedence for equal spans; exact literals need no regex escaping.
        for (SensitiveValue hint : hints) {
            String v = normalize(hint.value());
            int position = text.indexOf(v);
            while (position >= 0) {
                spans.add(new Span(position, position + v.length(), hint.type(), 0));
                position = text.indexOf(v, position + 1);
            }
        }
        add(spans, text, RRN, PiiType.RRN, 1);
        add(spans, text, PHONE, PiiType.PHONE, 2);
        add(spans, text, EMAIL, PiiType.EMAIL, 3);
        add(spans, text, CARD, PiiType.CARD, 4);
        var accounts = ACCOUNT.matcher(text);
        while (accounts.find()) {
            String digits = accounts.group().replace("-", "");
            if (digits.length() >= 10 && digits.length() <= 16)
                spans.add(new Span(accounts.start(), accounts.end(), PiiType.ACCOUNT, 5));
        }
        var names = LABELED_NAME.matcher(text);
        while (names.find()) spans.add(new Span(names.start(1), names.end(1), PiiType.NAME, 6));
        return spans;
    }

    private void add(List<Span> spans, String text, Pattern pattern, PiiType type, int priority) {
        var matcher = pattern.matcher(text);
        while (matcher.find()) spans.add(new Span(matcher.start(), matcher.end(), type, priority));
    }

    public boolean residualFree(String masked, List<SensitiveValue> hints) {
        // Remove only well-formed tokens produced by this implementation before inspecting content.
        String content = masked.replaceAll("\\[PII_(?:NAME|RRN|ACCOUNT|PHONE|EMAIL|CARD)_[1-9][0-9]*]", " ");
        return detect(content, hints).isEmpty() && !UNCLASSIFIED_NUMBER.matcher(content).find();
    }
}
