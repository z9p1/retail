package com.retail.schedule;

import com.retail.service.ScheduleService;
import com.retail.service.SimulatePurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每小时整点执行：若开关打开，则模拟 customer1 购买一件在售商品
 */
@Component
public class SimulatePurchaseJob {

    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private SimulatePurchaseService simulatePurchaseService;

    @Scheduled(cron = "0 0 * * * ?")
    public void run() {
        if (!scheduleService.isSimulatePurchaseEnabled()) {
            return;
        }
        simulatePurchaseService.runOnce();
    }
}
