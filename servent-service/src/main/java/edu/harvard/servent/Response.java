package edu.harvard.servent;

import org.springframework.http.ResponseEntity;

public class Response<T> {
    public T response;

    public static ResponseEntity<Response<Void>> ok() {
        Response<Void> r = new Response<Void>();
        r.response = null;
        return ResponseEntity.ok(r);
    }

    public static <T> ResponseEntity<Response<T>> ok(T response) {
        Response<T> r = new Response<T>();
        r.response = response;
        return ResponseEntity.ok(r);
    }
}
