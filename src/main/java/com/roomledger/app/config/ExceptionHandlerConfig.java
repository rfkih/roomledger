package com.roomledger.app.config;


import com.roomledger.app.dto.ResponseDto;
import com.roomledger.app.exthandler.InvalidAccountException;
import com.roomledger.app.exthandler.InvalidInputException;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.util.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionHandlerConfig {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlerConfig.class);

    /**
     * @param req HttpServletRequest
     * @param e all exception status 400
     * @return {@link HttpStatus#OK} Status code 200
     */
    @ExceptionHandler(value = {
            InvalidTransactionException.class,
    })
    public ResponseEntity<ResponseDto> invalidTransaction(HttpServletRequest req, Exception e) {
        ResponseDto errorResponse = ResponseDto.builder()
                .responseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()  + ResponseCode.INVALID_TRANSACTION.getCode())
                .responseDesc(ResponseCode.INVALID_TRANSACTION.getDescription() + " - " + e.getMessage())
                .build();
        log.error("{} : {} - Invalid Transaction service for req - {}", e.getClass().getSimpleName(), e.getLocalizedMessage(), req.getRequestURL(), e);
        return ResponseEntity.status(HttpStatus.OK).body(errorResponse);
    }


    /**
     * @param req HttpServletRequest
     * @param e all exception status 400
     * @return {@link HttpStatus#OK} Status code 200
     */
    @ExceptionHandler(value = {
            InvalidInputException.class,
    })
    public ResponseEntity<ResponseDto> invalidInput(HttpServletRequest req, Exception e) {
        ResponseDto errorResponse = ResponseDto.builder()
                .responseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()  + ResponseCode.BAD_REQUEST_INVALID_INPUT.getCode())
                .responseDesc(ResponseCode.BAD_REQUEST_INVALID_INPUT.getDescription() + " - " + e.getMessage())
                .build();
        log.error("{} : {} - Invalid Input service for req - {}", e.getClass().getSimpleName(), e.getLocalizedMessage(), req.getRequestURL(), e);
        return ResponseEntity.status(HttpStatus.OK).body(errorResponse);
    }

    /**
     * @param req HttpServletRequest
     * @param e all exception status 400
     * @return {@link HttpStatus#OK} Status code 200
     */
    @ExceptionHandler(value = {
            InvalidAccountException.class,
    })
    public ResponseEntity<ResponseDto> invalidAcount(HttpServletRequest req, Exception e) {
        ResponseDto errorResponse = ResponseDto.builder()
                .responseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()  + ResponseCode.INVALID_ACCOUNT.getCode())
                .responseDesc(ResponseCode.INVALID_ACCOUNT.getDescription() + " - " + e.getMessage())
                .build();
        log.error("{} : {} - Invalid Account service for req - {}", e.getClass().getSimpleName(), e.getLocalizedMessage(), req.getRequestURL(), e);
        return ResponseEntity.status(HttpStatus.OK).body(errorResponse);
    }


}