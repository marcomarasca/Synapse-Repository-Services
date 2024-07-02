CREATE TABLE IF NOT EXISTS `OAUTH_ACCESS_TOKEN` (
  `ID` BIGINT NOT NULL,
  `TOKEN_ID` CHAR(36) NOT NULL,
  `REFRESH_TOKEN_ID` BIGINT DEFAULT NULL,
  `PRINCIPAL_ID` BIGINT NOT NULL,
  `CLIENT_ID` BIGINT NOT NULL,
  `CREATED_ON` TIMESTAMP(3) NOT NULL,
  `EXPIRES_ON` TIMESTAMP(3) DEFAULT NULL,
  `SESSION_ID` CHAR(36) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `OAUTH_ACCESS_TOKEN_TOKEN_ID_KEY` (`TOKEN_ID`),
  KEY `OAUTH_ACCESS_TOKEN_EXPIRES_ON_KEY` (`EXPIRES_ON`),
  CONSTRAINT `OAUTH_ACCESS_TOKEN_REFRESH_TOKEN_ID_FK` FOREIGN KEY (`REFRESH_TOKEN_ID`) REFERENCES `OAUTH_REFRESH_TOKEN` (`ID`) ON DELETE CASCADE
)