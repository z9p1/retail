package com.retail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.entity.Order;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrderMapper extends BaseMapper<Order> {

    long countPaidOrdersByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countDistinctUserByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    BigDecimal sumPaidAmountByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 按日期汇总已支付金额，返回 date(字符串), amount */
    List<Map<String, Object>> sumPaidAmountGroupByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 按日期+商品汇总已支付金额，返回 date, productId, productName, amount */
    List<Map<String, Object>> sumPaidAmountGroupByDateAndProduct(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
