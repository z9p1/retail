package com.retail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.entity.AccessLog;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface AccessLogMapper extends BaseMapper<AccessLog> {

    long countPv(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
