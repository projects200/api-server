package com.project200.undabang.timer.simple.service.impl;

import com.project200.undabang.timer.simple.service.SimpleTimerQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class SimpleTimerQueryServiceImpl implements SimpleTimerQueryService {
}
