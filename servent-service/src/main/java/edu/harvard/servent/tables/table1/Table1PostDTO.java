package edu.harvard.servent.tables.table1;

import java.util.UUID;

public class Table1PostDTO {
	public Long column1;
	public String column2;
	public Integer column3;

	public Table1Row row() {
		Table1Row row = new Table1Row();
		row.idPublic = UUID.randomUUID();
		row.column1 = column1;
		row.column2 = column2;
		row.column3 = column3;
		return row;
	}
}
