CREATE TABLE IF NOT EXISTS `FAVORITE` (
  `FAVORITE_ID` BIGINT NOT NULL,
  `PRINCIPAL_ID` BIGINT NOT NULL,
  `NODE_ID` BIGINT NOT NULL,
  `CREATED_ON` BIGINT NOT NULL,
  PRIMARY KEY (FAVORITE_ID),
  UNIQUE KEY `UNIQUE_PRINC_NODE` (`PRINCIPAL_ID`,`NODE_ID`),
  CONSTRAINT `FAVORITE_NODE_ID_FK` FOREIGN KEY (`NODE_ID`) REFERENCES `JDONODE` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `FAVORITE_CREATED_BY_FK` FOREIGN KEY (`PRINCIPAL_ID`) REFERENCES `USER_GROUP` (`ID`) ON DELETE CASCADE
)
