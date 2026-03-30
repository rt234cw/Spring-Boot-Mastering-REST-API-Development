package com.codewithmosh.store.exceptions;


import com.codewithmosh.store.dtos.ErrorDto;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
   public ResponseEntity<ErrorDto> handleUnreadableMessage(){
        return  ResponseEntity.badRequest().body(
                new ErrorDto("Invalid request body"));
    }


}
