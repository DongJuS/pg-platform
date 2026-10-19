package com.shinhan.pg.common.eventcontracts;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;

/**
 * 이벤트 payload의 Jackson JSON 직렬화 유틸. {@code OutboxRecord.payload}(byte[])에 맞춰 byte[]를 입출력한다.
 *
 * <p>모든 서비스가 동일 인스턴스 설정으로 직렬화해야 생산/소비 양쪽의 wire 형식이 일치한다:
 * ISO-8601 시각 문자열(타임스탬프 숫자 아님), 알 수 없는 필드는 무시(additive 버전업 허용).</p>
 */
public final class EventCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private EventCodec() {}

    /** 이벤트를 JSON byte[]로 직렬화한다. */
    public static byte[] toBytes(Object event) {
        try {
            return MAPPER.writeValueAsBytes(event);
        } catch (IOException e) {
            throw new EventSerializationException("event serialization failed: " + event.getClass().getName(), e);
        }
    }

    /** JSON byte[]를 이벤트로 역직렬화한다. 필수 필드 누락 시 record 생성자 검증으로 실패한다. */
    public static <T> T fromBytes(byte[] payload, Class<T> eventType) {
        try {
            return MAPPER.readValue(payload, eventType);
        } catch (IOException | RuntimeException e) {
            throw new EventSerializationException("event deserialization failed: " + eventType.getName(), e);
        }
    }

    /** 직렬화/역직렬화 실패를 감싸는 unchecked 예외. */
    public static final class EventSerializationException extends RuntimeException {
        public EventSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
