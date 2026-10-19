package com.shinhan.pg.transfer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * [MVP1-F20] UNKNOWN 복구 배치의 스케줄링 활성화. 업무 bean과 마찬가지로 skeleton 프로필에서만 동작한다.
 * TransferSkeletonConfiguration과 분리해 둔다 — 단위 테스트가 해당 configuration을 직접 기동할 때
 * 스케줄러가 mock DataSource 위에서 발화하지 않도록 하기 위함이다(런타임은 component scan으로 로드).
 * 주기 기본값은 RecoverTransferBatchSkeleton의 pg.transfer.recover.interval(보수적 60s) 주석 참조.
 */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
@EnableScheduling
public class TransferSchedulingConfiguration {}
