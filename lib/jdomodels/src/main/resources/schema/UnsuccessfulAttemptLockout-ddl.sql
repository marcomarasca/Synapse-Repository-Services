CREATE TABLE IF NOT EXISTS `UNSUCCESSFUL_ATTEMPT_LOCKOUT`(
  `ATTEMPT_KEY` varchar(256) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  `UNSUCCESSFUL_ATTEMPT_COUNT` bigint(20) NOT NULL,
  `LOCKOUT_EXPIRATION` TIMESTAMP(3) NOT NULL,
  PRIMARY KEY (`ATTEMPT_KEY`)
);