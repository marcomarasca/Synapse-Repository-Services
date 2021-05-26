SELECT 
	T.ACTION_TYPE,
	T.ACTION_ID, COUNT(*) AS COUNT
	 FROM %s T
	  JOIN DOWNLOAD_LIST_ITEM_V2 D ON (T.FILE_ID = D.ENTITY_ID)
	   WHERE D.PRINCIPAL_ID = :principalId
	    GROUP BY T.ACTION_TYPE, T.ACTION_ID ORDER BY COUNT DESC LIMIT :limit OFFSET :offset