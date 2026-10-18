/**
 * transfer.completed 발행 의도를 업무 변경과 같은 로컬 DB 트랜잭션/연결에 append할 위치. Kafka 전송은 공통 Outbox relay가 수행한다.
 * 실제 store·테이블·직렬화는 미구현이다.
 */
package com.shinhan.pg.transfer.adapter.out.persistence.outbox;
