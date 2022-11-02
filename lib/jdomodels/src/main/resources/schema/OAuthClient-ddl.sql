CREATE TABLE IF NOT EXISTS `OAUTH_CLIENT` (
  `ID` BIGINT NOT NULL,
  `NAME` varchar(256) NOT NULL UNIQUE,
  `SECRET_HASH` char(74),
  `OAUTH_SECTOR_IDENTIFIER_URI` varchar(256) NOT NULL,
  `IS_VERIFIED` boolean NOT NULL,
  `PROPERTIES` mediumblob,
  `CREATED_BY` BIGINT NOT NULL,
  `CREATED_ON` BIGINT NOT NULL,
  `MODIFIED_ON` BIGINT NOT NULL,
  `ETAG` char(36) NOT NULL,
  PRIMARY KEY (`ID`),
  CONSTRAINT `OAUTH_CLIENT_SECTOR_IDENTIFIER_FK` FOREIGN KEY (`OAUTH_SECTOR_IDENTIFIER_URI`) REFERENCES `OAUTH_SECTOR_IDENTIFIER` (`URI`),
  CONSTRAINT `OAUTH_CLIENT_CREATED_BY_FK` FOREIGN KEY (`CREATED_BY`) REFERENCES `USER_GROUP` (`ID`)
)


	
	