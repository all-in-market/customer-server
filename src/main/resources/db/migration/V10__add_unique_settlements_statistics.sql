-- 테이블 중복 정리 및 유니크 제약 조건 추가
-- 동일한 조합 중 ID가 가증 큰 것만(최신) 남기고 삭제

DELETE FROM settlements s1
    USING settlements s2
WHERE s1.seller_id = s2.seller_id
  AND s1.period_start = s2.period_start
  AND s1.period_end = s2.period_end
  AND s1.id < s2.id;

ALTER TABLE settlements
    ADD CONSTRAINT uk_settlement_period UNIQUE (seller_id, period_start, period_end);

DELETE FROM seller_daily_statistics s1
    USING seller_daily_statistics s2
WHERE s1.seller_id = s2.seller_id
  AND s1.stat_date = s2.stat_date
  AND s1.id < s2.id;

ALTER TABLE seller_daily_statistics
    ADD CONSTRAINT uk_seller_statistics_stat_date UNIQUE (seller_id, stat_date);