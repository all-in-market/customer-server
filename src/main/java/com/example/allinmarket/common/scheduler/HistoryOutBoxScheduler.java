package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.common.outbox.consts.HistoryOutBoxConsts;
import com.example.allinmarket.common.outbox.entity.HistoryOutbox;
import com.example.allinmarket.common.outbox.repository.HistoryOutBoxRepository;
import com.example.allinmarket.common.outbox.service.HistoryOutBoxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryOutBoxScheduler {

    private final HistoryOutBoxRepository historyOutBoxRepository;
    private final HistoryOutBoxService historyOutBoxService;

    @Scheduled(fixedDelay = 10_000)
    public void processOutboxEvents() {
        Pageable pageable = PageRequest.of(0, 200);
        List<HistoryOutbox> outBoxes = historyOutBoxRepository.findUnprocessed(HistoryOutBoxConsts.MAX_RETRY_COUNT, pageable);

        if (outBoxes.isEmpty()) return;

        log.info("미처리 OutboxEvent 수: {}", outBoxes.size());

        outBoxes.forEach(outBox -> historyOutBoxService.process(outBox.getId()));
    }
}
