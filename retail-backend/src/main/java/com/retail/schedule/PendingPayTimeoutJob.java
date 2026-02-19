package com.retail.schedule;

import com.retail.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 待支付订单超时自动取消：每 2 分钟检查一次，将创建超过 30 分钟的待支付订单置为已取消。
 */
@Component
public class PendingPayTimeoutJob {

    private static final int TIMEOUT_MINUTES = 30;

    @Autowired
    private OrderService orderService;

    @Scheduled(cron = "0 */2 * * * ?")
    public void run() {
        orderService.cancelPendingPayOlderThanMinutes(TIMEOUT_MINUTES);
    }
}
