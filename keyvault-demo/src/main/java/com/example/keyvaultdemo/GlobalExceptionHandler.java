package com.example.keyvaultdemo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, HttpServletRequest request) {
        // Determine status using the exception type if possible
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex instanceof HttpClientErrorException httpEx) {
            status = HttpStatus.valueOf(httpEx.getStatusCode().value());
        }

        // Determine the Apim-Trace-Id from the backend response if present
        String apimTraceId = "Not Provided";
        if (ex instanceof HttpClientErrorException httpEx) {
            HttpHeaders backendHeaders = httpEx.getResponseHeaders();
            if (backendHeaders != null && backendHeaders.containsKey("Apim-Trace-Id")) {
                apimTraceId = backendHeaders.getFirst("Apim-Trace-Id");
            }
        }

        // Set the Apim-Trace-Id header in our custom response
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Apim-Trace-Id", apimTraceId);

        // Create a custom error response body with actual error details
        String errorMsg;
        if (ex instanceof HttpClientErrorException httpEx2) {
            String respBody = httpEx2.getResponseBodyAsString();
            errorMsg = (respBody != null && !respBody.isEmpty()) ? respBody : "No error message returned";
        } else {
            String exMsg = ex.getMessage();
            errorMsg = (exMsg != null && !exMsg.isEmpty()) ? exMsg : "No error message returned";
        }
        ErrorResponse errorResponse = new ErrorResponse(errorMsg, status.value());

        return new ResponseEntity<>(errorResponse, responseHeaders, status);
    }
    
    public static class ErrorResponse {
        private String message;
        private int status;

        public ErrorResponse(String message, int status) {
            this.message = message;
            this.status = status;
        }

        public String getMessage() {
            return message;
        }
        public int getStatus() {
            return status;
        }
        public void setMessage(String message) {
            this.message = message;
        }
        public void setStatus(int status) {
            this.status = status;
        }
    }
}