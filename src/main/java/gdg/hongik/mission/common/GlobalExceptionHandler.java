package gdg.hongik.mission.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String SERVER_ERROR = "서버에서 요청을 처리하는 중 오류가 발생했습니다.";
    private static final String INVALID_REQUEST = "요청 값과 형식을 확인해주세요.";

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected request failure", exception);
        return ResponseEntity.internalServerError().body(new ErrorResponse(SERVER_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request
    ) {
        String message = INVALID_REQUEST;
        if (status.is5xxServerError()) {
            log.error("Unexpected MVC failure", exception);
            message = SERVER_ERROR;
        } else if (exception instanceof MethodArgumentNotValidException validation) {
            message = validation.getBindingResult().getAllErrors().stream()
                    .findFirst().map(MessageSourceResolvable::getDefaultMessage).orElse(INVALID_REQUEST);
        } else if (exception instanceof HandlerMethodValidationException validation) {
            message = validation.getParameterValidationResults().stream()
                    .flatMap(result -> result.getResolvableErrors().stream())
                    .findFirst().map(MessageSourceResolvable::getDefaultMessage).orElse(INVALID_REQUEST);
        } else if (status.value() == HttpStatus.NOT_FOUND.value()) {
            message = "요청한 리소스를 찾을 수 없습니다.";
        }
        return super.handleExceptionInternal(exception, new ErrorResponse(message), headers, status, request);
    }
}
