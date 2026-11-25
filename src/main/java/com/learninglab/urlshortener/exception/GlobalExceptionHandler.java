package com.learninglab.urlshortener.exception;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
				.sorted(Comparator.comparing(e -> e.getField()))
				.map(e -> Map.of("field", e.getField(),
						"message", e.getDefaultMessage() == null ? "invalid value" : e.getDefaultMessage()))
				.toList();
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed", request);
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Malformed JSON request body", request);
		return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler(ShortUrlNotFoundException.class)
	public ResponseEntity<Object> handleNotFound(ShortUrlNotFoundException ex, WebRequest request) {
		ProblemDetail problem = problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
		return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
		log.error("Unhandled exception", ex);
		ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
		return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		Object effective = body;
		if (effective == null && ex instanceof ErrorResponse er) {
			effective = er.getBody();
		}
		if (effective instanceof ProblemDetail pd && pd.getInstance() == null
				&& request instanceof ServletWebRequest swr) {
			pd.setInstance(URI.create(swr.getRequest().getRequestURI()));
		}
		return super.handleExceptionInternal(ex, body, headers, statusCode, request);
	}

	private ProblemDetail problem(HttpStatus status, String detail, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(status.getReasonPhrase());
		if (request instanceof ServletWebRequest swr) {
			problem.setInstance(URI.create(swr.getRequest().getRequestURI()));
		}
		return problem;
	}
}
