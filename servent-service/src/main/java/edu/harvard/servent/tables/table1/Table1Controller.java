package edu.harvard.servent.tables.table1;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.harvard.servent.Response;

@RestController
@RequestMapping("/Table1")
public class Table1Controller {
	@Autowired
	Table1Service service;

	@DeleteMapping(value = "/DELETE")
	public ResponseEntity<Response<Void>> delete() {
		service.delete();
		return Response.ok();
	}

	@GetMapping(value = "/GET")
	public ResponseEntity<Response<List<Table1GetDTO>>> get() {
		List<Table1GetDTO> dtos = service.get();
		return Response.ok(dtos);
	}

	@PatchMapping(value = "/PATCH")
	public ResponseEntity<Response<Void>> patch(@RequestBody Table1PatchDTO dto) {
		service.patch(dto);
		return Response.ok();
	}

	@PostMapping(value = "/POST")
	public ResponseEntity<Response<Void>> post(@RequestBody Table1PostDTO dto) {
		service.post(dto);
		return Response.ok();
	}
}
