package edu.harvard.servent.tables.table1;

import java.util.UUID;

public class Table1PatchDTO {
	public UUID id;
	public Long column1;
	public String column2;
	public Integer column3;

	public Table1Row patch(Table1Row row) {
		if (column1 != null) row.column1 += column1;
		if (column2 != null) row.column2 += column2;
		if (column3 != null) row.column3 += column3;
		return row;
	}
}
