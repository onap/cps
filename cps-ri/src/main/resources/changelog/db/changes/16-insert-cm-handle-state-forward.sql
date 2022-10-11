INSERT INTO
	fragment(
		xpath,
		attributes,
		anchor_id,
		parent_id,
		dataspace_id,
		schema_node_id
	)
SELECT
	concat(cmHandles.xpath, '/state') AS xpath,
	to_jsonb(
		concat(
			'{"cm-handle-state": "READY", "last-update-time": "',
			to_char(
				now(),
				'YYYY-MM-DD"T"HH24:MI:SS.MSTZHTZM'
			),
			'", "data-sync-enabled": false}'
		) :: json
	) AS attributes,
	cmHandles.anchor_id,
	cmHandles.id,
	cmHandles.dataspace_id,
	cmHandles.schema_node_id
FROM
	(
		SELECT
			id,
			xpath,
			anchor_id,
			dataspace_id,
			schema_node_id
		FROM
			fragment
		WHERE
			xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]$'
			AND xpath NOT IN (
				SELECT
					SUBSTRING(
						xpath
						FROM
							'^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]'
					)
				FROM
					fragment
				WHERE
					xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state$'
			)
	) AS cmHandles;
INSERT INTO
	fragment(
		xpath,
		attributes,
		anchor_id,
		parent_id,
		dataspace_id,
		schema_node_id
	)
SELECT
	concat(cmHandlesStates.xpath, '/datastores'),
	to_jsonb('{}' :: json),
	cmHandlesStates.anchor_id,
	cmHandlesStates.id,
	cmHandlesStates.dataspace_id,
	cmHandlesStates.schema_node_id
FROM
	(
		SELECT
			id,
			xpath,
			anchor_id,
			dataspace_id,
			schema_node_id
		FROM
			fragment
		WHERE
			xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state$'
			AND xpath NOT IN (
				SELECT
					SUBSTRING(
						xpath
						FROM
							'^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state'
					)
				FROM
					fragment
				WHERE
					xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state/datastores$'
			)
	) AS cmHandlesStates;
INSERT INTO
	fragment(
		xpath,
		attributes,
		anchor_id,
		parent_id,
		dataspace_id,
		schema_node_id
	)
SELECT
	concat(
		cmHandlesDatastores.xpath,
		'/operational'
	),
	to_jsonb(
		concat('{"sync-state": "NONE_REQUESTED"}') :: json
	),
	cmHandlesDatastores.anchor_id,
	cmHandlesDatastores.id,
	cmHandlesDatastores.dataspace_id,
	cmHandlesDatastores.schema_node_id
FROM
	(
		SELECT
			id,
			xpath,
			anchor_id,
			dataspace_id,
			schema_node_id
		FROM
			fragment
		WHERE
			xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state/datastores$'
			AND xpath NOT IN (
				SELECT
					SUBSTRING(
						xpath
						FROM
							'^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state/datastores'
					)
				FROM
					fragment
				WHERE
					xpath ~* '^/dmi-registry/cm-handles\[@id=''[\w\-]+''\]/state/datastores/operational$'
			)
	) AS cmHandlesDatastores;