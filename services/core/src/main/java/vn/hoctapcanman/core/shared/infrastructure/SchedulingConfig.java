package vn.hoctapcanman.core.shared.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Bật việc theo lịch (dọn refresh token hết hạn…). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {}
