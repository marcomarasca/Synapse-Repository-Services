CREATE TABLE IF NOT EXISTS `FILES_SCANNER_STATUS` (
  `ID` BIGINT NOT NULL,
  `STARTED_ON` TIMESTAMP(3) NOT NULL,
  `UPDATED_ON` TIMESTAMP(3) NOT NULL,
  `JOBS_STARTED_COUNT` BIGINT NOT NULL,
  `JOBS_COMPLETED_COUNT` BIGINT NOT NULL,
  `SCANNED_ASSOCIATIONS_COUNT` BIGINT NOT NULL,
  `RELINKED_FILES_COUNT` BIGINT NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `STARTED_ON_KEY` (`STARTED_ON`),
  KEY `UPDATED_ON_KEY` (`UPDATED_ON`)
)
