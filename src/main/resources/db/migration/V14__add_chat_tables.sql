CREATE TABLE realtime_chat_rooms (
                                     id BIGSERIAL PRIMARY KEY,
                                     buyer_id BIGINT NOT NULL,
                                     seller_id BIGINT NOT NULL,
                                     room_name VARCHAR(255) NOT NULL,
                                     last_message TEXT,
                                     last_message_time TIMESTAMP,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 유니크 제약 조건 : 동일한 구매자와 판매자 사이에는 하나의 방만 존재
                                     CONSTRAINT uk_realtime_chat_room_buyer_seller UNIQUE (buyer_id, seller_id),
                                         -- 구매자와 판매자가 동일인물인 경우 차단
                                     CONSTRAINT chk_realtime_chat_room_buyer_ne_seller CHECK (buyer_id <> seller_id)
);

-- 인덱스 : 채팅방 목록 조회 시 마지막 메시지 시간순 정렬 최적화
CREATE INDEX idx_chat_room_last_msg_time ON realtime_chat_rooms (last_message_time DESC NULLS LAST);

-- 인덱스 : 특정 구매자가 속한 채팅방을 빠르게 조회하기 위함
-- (채팅방 목록 조회 시 buyer_id 조건 필터링 최적화)
CREATE INDEX idx_chat_room_buyer_id ON realtime_chat_rooms (buyer_id);

-- 인덱스 : 특정 판매자가 속한 채팅방을 빠르게 조회하기 위함
-- (채팅방 목록 조회 시 seller_id 조건 필터링 최적화)
CREATE INDEX idx_chat_room_seller_id ON realtime_chat_rooms (seller_id);


CREATE TABLE realtime_chat_participants (
                                            id BIGSERIAL PRIMARY KEY,
                                            user_id BIGINT NOT NULL,
                                            realtime_chat_room_id BIGINT NOT NULL,

                                            CONSTRAINT fk_participant_room_id FOREIGN KEY (realtime_chat_room_id) REFERENCES realtime_chat_rooms(id) ON DELETE CASCADE,
    -- 유니크 제약 조건 : 한 방에 동일 사용자가 중복 참여 불가
                                            CONSTRAINT uk_realtime_chat_participant_room_user UNIQUE (realtime_chat_room_id, user_id)
);

-- 인덱스 : 특정 유저가 속한 방 목록을 빠르게 찾기 위함
CREATE INDEX idx_chat_participant_user_id ON realtime_chat_participants (user_id);

-- 인덱스 : 특정 채팅방의 참여자 목록 조회 성능 향상
-- (room_id 기준으로 참여자 조회 시 Full Scan 방지)
CREATE INDEX idx_chat_participant_room_id ON realtime_chat_participants (realtime_chat_room_id);

CREATE TABLE realtime_chat_messages (
                                        id BIGSERIAL PRIMARY KEY,
                                        room_id BIGINT NOT NULL,
                                        sender_id BIGINT NOT NULL,
                                        message VARCHAR(3000) NOT NULL,
                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        -- 방 삭제 시 해당 메시지들도 모두 삭제 (데이터 무결성)
                                        CONSTRAINT fk_chat_message_room_id FOREIGN KEY (room_id) REFERENCES realtime_chat_rooms(id) ON DELETE CASCADE,
                                        -- 채팅방 참여자가 아닌 사용자 검증
                                        CONSTRAINT fk_chat_message_sender_participant FOREIGN KEY (room_id, sender_id) REFERENCES realtime_chat_participants(realtime_chat_room_id, user_id),
                                        --
                                        CONSTRAINT uk_chat_message_room_id_id UNIQUE (room_id, id)
);

-- 인덱스 : 특정 방의 메시지 이력을 최신순/커서 기반으로 조회할 때 필수
CREATE INDEX idx_chat_message_room_id_id ON realtime_chat_messages (room_id, id DESC);

-- 인덱스 : 채팅 메시지 이력을 최신순으로 조회할 때 성능 최적화
-- (room_id + created_at DESC 조합으로 커서 기반 페이징 및 최신 메시지 조회 최적화)
CREATE INDEX idx_chat_message_room_created_at ON realtime_chat_messages (room_id, created_at DESC);

CREATE TABLE realtime_chat_read_status (
                                           id BIGSERIAL PRIMARY KEY,
                                           room_id BIGINT NOT NULL,
                                           user_id BIGINT NOT NULL,
                                           last_read_message_id BIGINT NOT NULL,
    -- 방 삭제 시 읽음 상태 기록 삭제
                                           CONSTRAINT fk_chat_read_status_room_id FOREIGN KEY (room_id) REFERENCES realtime_chat_rooms(id) ON DELETE CASCADE,
    -- 메시지 참조 (단, 메시지 삭제 시 처리는 비즈니스 로직에 따라 다를 수 있음)
                                           CONSTRAINT fk_chat_read_status_last_read_message_id FOREIGN KEY (room_id, last_read_message_id) REFERENCES realtime_chat_messages(room_id, id),
    -- 유니크 제약 조건 : 방 + 유저당 하나의 읽음 상태만 존재
                                           CONSTRAINT uk_realtime_chat_read_status_room_user UNIQUE (room_id, user_id),
    -- 채팅방 참여자가 아닌 사용자 검증
                                           CONSTRAINT fk_chat_read_status_participant FOREIGN KEY (room_id, user_id) REFERENCES realtime_chat_participants(realtime_chat_room_id, user_id)
);

-- 인덱스 : 특정 유저의 읽음 상태 조회 성능 향상
CREATE INDEX idx_chat_read_status_user_room ON realtime_chat_read_status (user_id, room_id);

-- 인덱스 : 특정 채팅방의 읽음 상태 조회 성능 향상
-- (room_id 기준으로 read_status 조회 시 빠른 접근)
CREATE INDEX idx_chat_read_status_room_id ON realtime_chat_read_status (room_id);