package com.retail.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.common.ResultCode;
import com.retail.entity.UserAddress;
import com.retail.exception.BusinessException;
import com.retail.mapper.UserAddressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户收货地址 CRUD（可为空，仅本人操作）
 */
@Service
public class UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    public List<UserAddress> listByUserId(Long userId) {
        return userAddressMapper.selectList(
                new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId).orderByDesc(UserAddress::getUpdateTime));
    }

    public UserAddress getByIdAndUserId(Long id, Long userId) {
        UserAddress a = userAddressMapper.selectById(id);
        if (a == null || !a.getUserId().equals(userId)) return null;
        return a;
    }

    public UserAddress add(Long userId, String receiver, String phone, String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "地址不能为空");
        }
        UserAddress a = new UserAddress();
        a.setUserId(userId);
        a.setReceiver(receiver != null ? receiver.trim() : null);
        a.setPhone(phone != null ? phone.trim() : null);
        a.setAddress(address.trim());
        a.setCreateTime(LocalDateTime.now());
        a.setUpdateTime(LocalDateTime.now());
        userAddressMapper.insert(a);
        return a;
    }

    public void update(Long userId, Long id, String receiver, String phone, String address) {
        UserAddress a = getByIdAndUserId(id, userId);
        if (a == null) throw new BusinessException(ResultCode.BAD_REQUEST, "地址不存在或无权限");
        if (address != null && !address.trim().isEmpty()) a.setAddress(address.trim());
        if (receiver != null) a.setReceiver(receiver.trim().isEmpty() ? null : receiver.trim());
        if (phone != null) a.setPhone(phone.trim().isEmpty() ? null : phone.trim());
        a.setUpdateTime(LocalDateTime.now());
        userAddressMapper.updateById(a);
    }

    public void delete(Long userId, Long id) {
        UserAddress a = getByIdAndUserId(id, userId);
        if (a == null) throw new BusinessException(ResultCode.BAD_REQUEST, "地址不存在或无权限");
        userAddressMapper.deleteById(id);
    }
}
