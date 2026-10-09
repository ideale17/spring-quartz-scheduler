package com.kji.scheduler.dto;

/**
 * Quartz Trigger의 Misfire 처리 정책을 정의한다.
 */
public enum MisfirePolicy {
	SMART_POLICY,
	FIRE_AND_PROCEED,
	DO_NOTHING,
	FIRE_NOW,
	NOW_WITH_EXISTING_COUNT,
	NOW_WITH_REMAINING_COUNT,
	NEXT_WITH_EXISTING_COUNT,
	NEXT_WITH_REMAINING_COUNT
}