package com.retail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.entity.Order;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderMapper extends BaseMapper<Order> {

    long countPaidOrdersByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countDistinctUserByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    BigDecimal sumPaidAmountByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
