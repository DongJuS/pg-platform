package @@WEB_PACKAGE@@;
import com.shinhan.pg.common.commonerror.ApplicationException;
import com.shinhan.pg.common.commonerror.ErrorResponse;
import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class PlatformExceptionHandler {
    @ExceptionHandler(FeatureNotImplementedException.class)
    public ResponseEntity<ErrorResponse> unimplemented(FeatureNotImplementedException error) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ErrorResponse("COMMON-NOT-IMPLEMENTED","Feature is not implemented",MDC.get("traceId")));
    }
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> application(ApplicationException error) {
        var code = error.errorCode();
        return ResponseEntity.status(status(code.type())).body(new ErrorResponse(code.code(),code.message(),MDC.get("traceId")));
    }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
                       org.springframework.http.converter.HttpMessageNotReadableException.class,
                       org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> invalid(Exception error) {
        return ResponseEntity.badRequest().body(new ErrorResponse("COMMON-INVALID","Invalid request",MDC.get("traceId")));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> internal(Exception error) {
        if (error instanceof org.springframework.web.ErrorResponse frameworkError) {
            var status=frameworkError.getStatusCode();
            var reason=HttpStatus.resolve(status.value());
            return ResponseEntity.status(status).body(new ErrorResponse("COMMON-HTTP-"+status.value(),
                    reason==null ? "Request failed" : reason.getReasonPhrase(),MDC.get("traceId")));
        }
        LoggerFactory.getLogger(getClass()).error("Unhandled request error",error);
        return ResponseEntity.internalServerError().body(new ErrorResponse("COMMON-INTERNAL","Internal server error",MDC.get("traceId")));
    }
    static HttpStatus status(ErrorType type) {
        return switch (type) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case EXTERNAL_FAILURE -> HttpStatus.BAD_GATEWAY;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
