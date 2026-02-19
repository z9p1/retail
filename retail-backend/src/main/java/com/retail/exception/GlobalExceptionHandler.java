package com.retail.exception;

import com.retail.common.Result;
import com.retail.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> businessException(BusinessException e, HttpServletResponse response) {
        int code = e.getCode();
        if (code == ResultCode.UNAUTHORIZED) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else if (code == ResultCode.FORBIDDEN) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
        } else if (code >= 4000 && code < 5000) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        } else {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return Result.fail(code, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> validException(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException) {
            msg = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors().stream()
                    .map(f -> f.getField() + " " + f.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b).orElse(msg);
        } else if (e instanceof BindException) {
            msg = ((BindException) e).getBindingResult().getFieldErrors().stream()
                    .map(f -> f.getField() + " " + f.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b).orElse(msg);
        }
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> exception(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.SERVER_ERROR, "系统繁忙，请稍后重试");
    }
}
