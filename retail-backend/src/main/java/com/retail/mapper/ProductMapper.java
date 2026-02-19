package com.retail.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.entity.Product;
import org.apache.ibatis.annotations.Param;

/**
 * 乐观锁扣减：UPDATE ... WHERE id=? AND version=? AND stock>=?
 */
public interface ProductMapper extends BaseMapper<Product> {

    int deductStock(@Param("id") Long id, @Param("version") Integer version, @Param("quantity") Integer quantity);

    int restoreStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
