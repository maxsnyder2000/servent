import {useCallback, useEffect, useState} from "react";
import {deleteRequest, getRequest, patchRequest, postRequest} from "./requests";

import "./App.css";

const App = () => {
	// TABLE: Table1
	// GET
	const [dataTable1, setDataTable1] = useState([]);
	const getTable1 = useCallback(() => {
		getRequest("/Table1/GET").then((response) => setDataTable1(response));
	}, []);
	useEffect(getTable1, [getTable1]);
	// DELETE
	const deleteTable1 = useCallback(() => {
		deleteRequest("/Table1/DELETE").then(getTable1);
	}, [getTable1]);
	// PATCH
	const patchTable1Column3 = useCallback((row, i) => {
		const body = {
			id: row.id,
			column3: i
		};
		patchRequest("/Table1/PATCH", body).then(getTable1);
	}, [getTable1]);
	// POST
	const [valueTable1Column2, setValueTable1Column2] = useState("");
	const onChangeTable1Column2 = useCallback((event) => {
		setValueTable1Column2(event?.target?.value);
	}, []);
	const postTable1 = useCallback(() => {
		if (!((s) => s.length !== 0)(valueTable1Column2)) {
			return;
		}
		const body = {
			column1: (() => Date.now() / 1000)(),
			column2: valueTable1Column2,
			column3: (() => 0)()
		};
		postRequest("/Table1/POST", body).then(getTable1).then(() => {
			setValueTable1Column2("");
		});
	}, [getTable1, valueTable1Column2]);
	return (
		<div className="App">
			<br />
			<input value={valueTable1Column2} placeholder="Text" onChange={onChangeTable1Column2} />
			<button onClick={postTable1}>
				Post
			</button>
			<button onClick={deleteTable1}>
				Delete All
			</button>
			<br />
			<br />
			{dataTable1.length > 0 && 
				<table>
					<thead>
						<tr>
							<th>Date</th>
							<th>Text</th>
							<th>Likes</th>
							<th>Actions</th>
						</tr>
					</thead>
					<tbody>
						{dataTable1.map((row) => (
							<tr key={row.id}>
								<td>{((l) => new Date(l * 1000).toString())(row.column1)}</td>
								<td>{row.column2}</td>
								<td>{row.column3}</td>
								<td>
									<>
										<button onClick={() => patchTable1Column3(row, 1)}>
											Like
										</button>
										<button onClick={() => patchTable1Column3(row, -1)}>
											Dislike
										</button>
									</>
								</td>
							</tr>
						))}
					</tbody>
				</table>
			}
		</div>
	);
};

export default App;
