package com.retail.common;

/**
 * 业务错误码
 */
public final class ResultCode {

    public static final int SUCCESS = 0;

    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int BAD_REQUEST = 400;

    /** 库存不足 */
    public static final int STOCK_INSUFFICIENT = 4001;
    /** 商品已下架 */
    public static final int PRODUCT_OFF_SHELF = 4002;
    /** 订单状态不允许操作 */
    public static final int ORDER_STATUS_INVALID = 4003;
    /** 重复操作（幂等已处理） */
    public static final int DUPLICATE_OPERATION = 4004;

    public static final int SERVER_ERROR = 500;
}
