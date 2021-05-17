-- --------------------------------------------------------
-- Host:                         10.0.0.62
-- Server version:               10.3.24-MariaDB - Source distribution
-- Server OS:                    Linux
-- HeidiSQL Version:             11.2.0.6213
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- Dumping database structure for afcl
CREATE DATABASE IF NOT EXISTS `afcl` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `afcl`;

-- Dumping structure for table afcl.fcdeployment
CREATE TABLE IF NOT EXISTS `fcdeployment` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `FCteam` int(11) NOT NULL DEFAULT 0,
  `fd` int(11) NOT NULL,
  `description` varchar(50) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `FKfcteam` (`FCteam`),
  CONSTRAINT `FKfcteam` FOREIGN KEY (`FCteam`) REFERENCES `fcteam` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=113 DEFAULT CHARSET=latin1 COMMENT='8 deployments for each FC, based on function deployments';

-- Dumping data for table afcl.fcdeployment: ~112 rows (approximately)
/*!40000 ALTER TABLE `fcdeployment` DISABLE KEYS */;
REPLACE INTO `fcdeployment` (`id`, `FCteam`, `fd`, `description`) VALUES
	(1, 1, 1, 'IBM Frankfurt 128 MB'),
	(2, 1, 2, 'IBM Frankfurt 256 MB'),
	(3, 1, 3, 'IBM Tokyo 128 MB'),
	(4, 1, 4, 'IBM Tokyo 256 MB'),
	(5, 1, 5, 'IBM Washington 128 MB'),
	(6, 1, 6, 'IBM Washington 256 MB'),
	(7, 1, 7, 'IBM London 128 MB'),
	(8, 1, 8, 'IBM London 256 MB'),
	(9, 2, 1, 'IBM Frankfurt 128 MB'),
	(10, 2, 2, 'IBM Frankfurt 256 MB'),
	(11, 2, 3, 'IBM Tokyo 128 MB'),
	(12, 2, 4, 'IBM Tokyo 256 MB'),
	(13, 2, 5, 'IBM Washington 128 MB'),
	(14, 2, 6, 'IBM Washington 256 MB'),
	(15, 2, 7, 'AWS N. Virginia 128 MB'),
	(16, 2, 8, 'AWS N. Virginia 256 MB'),
	(17, 3, 1, 'Google Frankfurt 128 MB'),
	(18, 3, 2, 'Google Frankfurt 256 MB'),
	(19, 3, 3, 'Google Tokyo 128 MB'),
	(20, 3, 4, 'Google Tokyo 256 MB'),
	(21, 3, 5, 'Google South Carolina 128 MB'),
	(22, 3, 6, 'Google South Carolina 256 MB'),
	(23, 3, 7, 'AWS N. Virginia 128 MB'),
	(24, 3, 8, 'AWS N. Virginia 256 MB'),
	(25, 4, 1, 'IBM Frankfurt 128 MB Python'),
	(26, 4, 2, 'IBM Frankfurt 128 MB node.js'),
	(27, 4, 3, 'IBM Tokyo 128 MB Python'),
	(28, 4, 4, 'IBM Tokyo 128 MB node.js'),
	(29, 4, 5, 'IBM Washington 128 MB Python'),
	(30, 4, 6, 'IBM Washington 128 MB node.js'),
	(31, 4, 7, 'AWS N. Virginia 128 MB Python'),
	(32, 4, 8, 'AWS N. Virginia 128 MB node.js'),
	(33, 5, 1, 'IBM Frankfurt 128 MB'),
	(34, 5, 2, 'IBM Frankfurt 256 MB'),
	(35, 5, 3, 'IBM Tokyo 128 MB'),
	(36, 5, 4, 'IBM Tokyo 256 MB'),
	(37, 5, 5, 'IBM Washington 128 MB'),
	(38, 5, 6, 'IBM Washington 256 MB'),
	(39, 5, 7, 'AWS N. Virginia 128 MB'),
	(40, 5, 8, 'AWS N. Virginia 256 MB'),
	(41, 6, 1, 'IBM Frankfurt 128 MB'),
	(42, 6, 2, 'IBM Frankfurt 256 MB'),
	(43, 6, 3, 'IBM Tokyo 128 MB'),
	(44, 6, 4, 'IBM Tokyo 256 MB'),
	(45, 6, 5, 'IBM Washington 128 MB'),
	(46, 6, 6, 'IBM Washington 256 MB'),
	(47, 6, 7, 'AWS N. Virginia 128 MB'),
	(48, 6, 8, 'AWS N. Virginia 256 MB'),
	(49, 7, 1, 'IBM Frankfurt 128 MB Python'),
	(50, 7, 2, 'IBM Frankfurt 128 MB Java'),
	(51, 7, 3, 'IBM Tokyo 128 MB Python'),
	(52, 7, 4, 'IBM Tokyo 128 MB Java'),
	(53, 7, 5, 'IBM Washington 128 MB Python'),
	(54, 7, 6, 'IBM Washington 128 MB Java'),
	(55, 7, 7, 'AWS N. Virginia 128 MB Python'),
	(56, 7, 8, 'AWS N. Virginia 128 MB Java'),
	(57, 8, 1, 'IBM Frankfurt 128 MB'),
	(58, 8, 2, 'IBM Frankfurt 256 MB'),
	(59, 8, 3, 'IBM Tokyo 128 MB'),
	(60, 8, 4, 'IBM Tokyo 256 MB'),
	(61, 8, 5, 'IBM Washington 128 MB'),
	(62, 8, 6, 'IBM Washington 256 MB'),
	(63, 8, 7, 'AWS N. Virginia 128 MB'),
	(64, 8, 8, 'AWS N. Virginia 256 MB'),
	(65, 9, 1, 'IBM Frankfurt 128 MB Python'),
	(66, 9, 2, 'IBM Frankfurt 128 MB node.js'),
	(67, 9, 3, 'IBM Tokyo 128 MB Python'),
	(68, 9, 4, 'IBM Tokyo 128 MB node.js'),
	(69, 9, 5, 'IBM Washington 128 MB Python'),
	(70, 9, 6, 'IBM Washington 128 MB node.js'),
	(71, 9, 7, 'AWS N. Virginia 128 MB Python'),
	(72, 9, 8, 'AWS N. Virginia 128 MB node.js'),
	(73, 10, 1, 'IBM Frankfurt 128 MB'),
	(74, 10, 2, 'IBM Frankfurt 256 MB'),
	(75, 10, 3, 'IBM Tokyo 128 MB'),
	(76, 10, 4, 'IBM Tokyo 256 MB'),
	(77, 10, 5, 'IBM Washington 128 MB'),
	(78, 10, 6, 'IBM Washington 256 MB'),
	(79, 10, 7, 'IBM London 128 MB'),
	(80, 10, 8, 'IBM London 256 MB'),
	(81, 11, 1, 'IBM Frankfurt 128 MB'),
	(82, 11, 2, 'IBM Frankfurt 256 MB'),
	(83, 11, 3, 'IBM Tokyo 128 MB'),
	(84, 11, 4, 'IBM Tokyo 256 MB'),
	(85, 11, 5, 'IBM Washington 128 MB'),
	(86, 11, 6, 'IBM Washington 256 MB'),
	(87, 11, 7, 'AWS N. Virginia 128 MB'),
	(88, 11, 8, 'AWS N. Virginia 256 MB'),
	(89, 12, 1, 'IBM Frankfurt 128 MB Python'),
	(90, 12, 2, 'IBM Frankfurt 128 MB Java'),
	(91, 12, 3, 'IBM Tokyo 128 MB Python'),
	(92, 12, 4, 'IBM Tokyo 128 MB Java'),
	(93, 12, 5, 'IBM Washington 128 MB Python'),
	(94, 12, 6, 'IBM Washington 128 MB Java'),
	(95, 12, 7, 'AWS N. Virginia 128 MB Python'),
	(96, 12, 8, 'AWS N. Virginia 128 MB Java'),
	(97, 13, 1, 'AWS N. Virginia 128 MB'),
	(98, 13, 2, 'AWS N. Virginia 256 MB'),
	(99, 13, 3, 'AWS N. Virginia 512 MB'),
	(100, 13, 4, 'AWS N. Virginia 1 GB'),
	(101, 13, 5, 'AWS N. Virginia 2 GB'),
	(102, 13, 6, 'AWS N. Virginia 3 GB'),
	(103, 13, 7, 'AWS N. Virginia 4 GB'),
	(104, 13, 8, 'AWS N. Virginia 5 GB'),
	(105, 14, 1, 'IBM Frankfurt 128 MB'),
	(106, 14, 2, 'IBM Frankfurt 256 MB'),
	(107, 14, 3, 'IBM Tokyo 128 MB'),
	(108, 14, 4, 'IBM Tokyo 256 MB'),
	(109, 14, 5, 'IBM Washington 128 MB'),
	(110, 14, 6, 'IBM Washington 256 MB'),
	(111, 14, 7, 'AWS N. Virginia 128 MB'),
	(112, 14, 8, 'AWS N. Virginia 256 MB');
/*!40000 ALTER TABLE `fcdeployment` ENABLE KEYS */;

-- Dumping structure for table afcl.fcteam
CREATE TABLE IF NOT EXISTS `fcteam` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fctype` mediumint(9) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fctype` (`fctype`),
  CONSTRAINT `fctype` FOREIGN KEY (`fctype`) REFERENCES `fctype` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=latin1 COMMENT='concrete CFCL for some AFCL (FCType tabe)';

-- Dumping data for table afcl.fcteam: ~14 rows (approximately)
/*!40000 ALTER TABLE `fcteam` DISABLE KEYS */;
REPLACE INTO `fcteam` (`id`, `fctype`, `name`) VALUES
	(1, 2, 'Plangger Samuel Benjamin'),
	(2, 1, 'Aspir Ahmet, Kaufmann Stefan'),
	(3, 1, 'Kupek Tobias Wolfgang, Pfister Zoe Michaela Dietmar'),
	(4, 1, 'Gaßner Matthias, Gaspar Jakov, Pockstaller Tobias'),
	(5, 3, 'Buchauer Manuel, De Sclavis Davide'),
	(6, 4, 'Engelhardt Chris Günter, Mittelberger Jakob'),
	(7, 3, 'Hollaus Christian, Kleinfeld Simon, Maier Florian'),
	(8, 4, 'Pedratscher Stefan, Plattner Michael'),
	(9, 2, 'Bless Christof, Drude Jonathan, Reitinger Sandra'),
	(10, 3, 'Kaltschmid Michael, Reiter Markus'),
	(11, 4, 'Gruber Raphael, Lanzinger Patrick'),
	(12, 2, 'Klösch Christoph Alexander, Moosbrugger Markus Lorenz, Nöckl Jakob'),
	(13, 3, 'Kommer Phillip Joseph'),
	(14, 3, 'Wurzer Thomas, Gatta David');
/*!40000 ALTER TABLE `fcteam` ENABLE KEYS */;

-- Dumping structure for table afcl.fctype
CREATE TABLE IF NOT EXISTS `fctype` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL DEFAULT '0',
  `AFCL` blob DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=latin1 COMMENT='A table that contain all FCs that can be executed for logging';

-- Dumping data for table afcl.fctype: ~4 rows (approximately)
/*!40000 ALTER TABLE `fctype` DISABLE KEYS */;
REPLACE INTO `fctype` (`id`, `name`, `AFCL`) VALUES
	(1, 'p1ObjectRecognition', NULL),
	(2, 'p2BWA', NULL),
	(3, 'P3PredictionStockPrice', NULL),
	(4, 'P4GateChangeAlert', NULL);
/*!40000 ALTER TABLE `fctype` ENABLE KEYS */;

-- Dumping structure for table afcl.functiondeployment
CREATE TABLE IF NOT EXISTS `functiondeployment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `functionImplementation_id` bigint(20) DEFAULT NULL,
  `KMS_Arn` varchar(255) DEFAULT NULL,
  `regionID` int(11) DEFAULT NULL,
  `description` varchar(255) NOT NULL,
  `handlerName` varchar(255) NOT NULL,
  `input` varchar(255) DEFAULT NULL,
  `isDeployed` bit(1) NOT NULL,
  `memorySize` int(11) NOT NULL,
  `timeout` int(11) NOT NULL,
  `avgRTT` double DEFAULT NULL,
  `avgRuntime` double DEFAULT NULL,
  `avgCost` double DEFAULT NULL,
  `successRate` double DEFAULT NULL,
  `computationalSpeed` double DEFAULT NULL,
  `memorySpeed` double DEFAULT NULL,
  `ioSpeed` double DEFAULT NULL,
  `invocations` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_si8kq5j31b1h3ixxiqjoein15` (`name`),
  KEY `FKrjwhvvrdf0kji44tg2kis0v3e` (`functionImplementation_id`),
  KEY `FKs9d8fj3fc9383n38sfs3csdco` (`regionID`) USING BTREE,
  CONSTRAINT `FKrjwhvvrdf0kji44tg2kis0v3e` FOREIGN KEY (`functionImplementation_id`) REFERENCES `functionimplementation` (`id`),
  CONSTRAINT `FKs9d8fj3fc9383n38sfs3csdco` FOREIGN KEY (`regionID`) REFERENCES `region` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.functiondeployment: ~0 rows (approximately)
/*!40000 ALTER TABLE `functiondeployment` DISABLE KEYS */;
/*!40000 ALTER TABLE `functiondeployment` ENABLE KEYS */;

-- Dumping structure for table afcl.functiondeployment_environmentvariables
CREATE TABLE IF NOT EXISTS `functiondeployment_environmentvariables` (
  `FunctionDeployment_id` bigint(20) NOT NULL,
  `environmentVariables` varchar(255) DEFAULT NULL,
  `environmentVariables_KEY` varchar(255) NOT NULL,
  PRIMARY KEY (`FunctionDeployment_id`,`environmentVariables_KEY`),
  CONSTRAINT `FKkekgwdaqekiticds1wq2mn35j` FOREIGN KEY (`FunctionDeployment_id`) REFERENCES `functiondeployment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.functiondeployment_environmentvariables: ~0 rows (approximately)
/*!40000 ALTER TABLE `functiondeployment_environmentvariables` DISABLE KEYS */;
/*!40000 ALTER TABLE `functiondeployment_environmentvariables` ENABLE KEYS */;

-- Dumping structure for table afcl.functionimplementation
CREATE TABLE IF NOT EXISTS `functionimplementation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `functionType_id` bigint(20) DEFAULT NULL,
  `algorithm` varchar(255) DEFAULT NULL,
  `implementationFilePath` varchar(255) DEFAULT NULL,
  `provider` smallint(6) NOT NULL DEFAULT 0,
  `language_id` bigint(20) DEFAULT NULL,
  `avgRTT` double DEFAULT NULL,
  `avgCost` double DEFAULT NULL,
  `successRate` double DEFAULT NULL,
  `computationWork` double DEFAULT NULL,
  `memoryWork` double DEFAULT NULL,
  `ioWork` double DEFAULT NULL,
  `invocations` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_gy0r4omi46xytndlts8gunwln` (`name`),
  KEY `FKiclu7oaoqp57tdx8772jv5hpc` (`functionType_id`),
  KEY `FKd8t6ku556b5yr94nmge02nw8r` (`language_id`),
  KEY `FKProvider` (`provider`),
  CONSTRAINT `FKProvider` FOREIGN KEY (`provider`) REFERENCES `provider` (`id`),
  CONSTRAINT `FKd8t6ku556b5yr94nmge02nw8r` FOREIGN KEY (`language_id`) REFERENCES `language` (`id`),
  CONSTRAINT `FKiclu7oaoqp57tdx8772jv5hpc` FOREIGN KEY (`functionType_id`) REFERENCES `functiontype` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.functionimplementation: ~0 rows (approximately)
/*!40000 ALTER TABLE `functionimplementation` DISABLE KEYS */;
/*!40000 ALTER TABLE `functionimplementation` ENABLE KEYS */;

-- Dumping structure for table afcl.functionimplementation_additionalservices
CREATE TABLE IF NOT EXISTS `functionimplementation_additionalservices` (
  `PersistentFunctionImplementation_id` bigint(20) NOT NULL,
  `isDerived` bit(1) DEFAULT NULL,
  `additionalService_id` bigint(20) NOT NULL,
  PRIMARY KEY (`PersistentFunctionImplementation_id`,`additionalService_id`),
  CONSTRAINT `FKe3rrelg2btkb0ueyw9fao7ae1` FOREIGN KEY (`PersistentFunctionImplementation_id`) REFERENCES `functionimplementation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table afcl.functionimplementation_additionalservices: ~0 rows (approximately)
/*!40000 ALTER TABLE `functionimplementation_additionalservices` DISABLE KEYS */;
/*!40000 ALTER TABLE `functionimplementation_additionalservices` ENABLE KEYS */;

-- Dumping structure for table afcl.functionimplementation_additionalservicetype
CREATE TABLE IF NOT EXISTS `functionimplementation_additionalservicetype` (
  `FunctionImplementation_id` bigint(20) NOT NULL,
  `additionalServices_id` bigint(20) NOT NULL,
  PRIMARY KEY (`FunctionImplementation_id`,`additionalServices_id`),
  CONSTRAINT `FKhy7o5bstrj9h0rkarhad9u55c` FOREIGN KEY (`FunctionImplementation_id`) REFERENCES `functionimplementation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.functionimplementation_additionalservicetype: ~0 rows (approximately)
/*!40000 ALTER TABLE `functionimplementation_additionalservicetype` DISABLE KEYS */;
/*!40000 ALTER TABLE `functionimplementation_additionalservicetype` ENABLE KEYS */;

-- Dumping structure for table afcl.functiontype
CREATE TABLE IF NOT EXISTS `functiontype` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `avgRTT` double DEFAULT NULL,
  `avgCost` double DEFAULT NULL,
  `successRate` double DEFAULT NULL,
  `invocations` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_p9rvsuvhqwj84qagf3d5myr35` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.functiontype: ~23 rows (approximately)
/*!40000 ALTER TABLE `functiontype` DISABLE KEYS */;
REPLACE INTO `functiontype` (`id`, `name`, `type`, `avgRTT`, `avgCost`, `successRate`, `invocations`) VALUES
	(1, 'countVideos', 'countVideosType', NULL, NULL, NULL, 0),
	(2, 'frameExtractor', 'frameExtractorType', NULL, NULL, NULL, 0),
	(3, 'deltaFinderRekog', 'deltaFinderRekogType', NULL, NULL, NULL, 0),
	(4, 'countPerVideo', 'countPerVideoType', NULL, NULL, NULL, 0),
	(5, 'outPerVideo', 'outPerVideoType', NULL, NULL, NULL, 0),
	(6, 'split', 'splitType', NULL, NULL, NULL, 0),
	(7, 'bwaIndex', 'bwaIndexType', NULL, NULL, NULL, 0),
	(8, 'bwaAlnR1', 'bwaAlnR1Type', NULL, NULL, NULL, 0),
	(9, 'bwaAlnR2', 'bwaAlnR2Type', NULL, NULL, NULL, 0),
	(10, 'bwaSampe', 'bwaSampeType', NULL, NULL, NULL, 0),
	(11, 'merge', 'mergeType', NULL, NULL, NULL, 0),
	(12, 'sort', 'sortType', NULL, NULL, NULL, 0),
	(13, 'fetchProcess', 'fetchProcessType', NULL, NULL, NULL, 0),
	(14, 'forecast', 'forecastType', NULL, NULL, NULL, 0),
	(15, 'processResult', 'processResultType', NULL, NULL, NULL, 0),
	(16, 'createChart', 'createChartType', NULL, NULL, NULL, 0),
	(17, 'getPassengers', 'getPassengersType', NULL, NULL, NULL, 0),
	(18, 'secCheckTime', 'secCheckTimeType', NULL, NULL, NULL, 0),
	(19, 'getGateGPS', 'getGateGPSType', NULL, NULL, NULL, 0),
	(20, 'distanceGPS', 'distanceGPSType', NULL, NULL, NULL, 0),
	(21, 'readGPS', 'readGPSType', NULL, NULL, NULL, 0),
	(22, 'sumUp', 'sumUpType', NULL, NULL, NULL, 0),
	(23, 'averageTime', 'averageTimeType', NULL, NULL, NULL, 0);
/*!40000 ALTER TABLE `functiontype` ENABLE KEYS */;

-- Dumping structure for table afcl.functiontype_inputs
CREATE TABLE IF NOT EXISTS `functiontype_inputs` (
  `FunctionType_id` bigint(20) NOT NULL,
  `inputs` varchar(255) DEFAULT NULL,
  KEY `FK7qrq191xp1j948y9309x41475` (`FunctionType_id`),
  CONSTRAINT `FK7qrq191xp1j948y9309x41475` FOREIGN KEY (`FunctionType_id`) REFERENCES `functiontype` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.functiontype_inputs: ~0 rows (approximately)
/*!40000 ALTER TABLE `functiontype_inputs` DISABLE KEYS */;
/*!40000 ALTER TABLE `functiontype_inputs` ENABLE KEYS */;

-- Dumping structure for table afcl.functiontype_outputs
CREATE TABLE IF NOT EXISTS `functiontype_outputs` (
  `FunctionType_id` bigint(20) NOT NULL,
  `outputs` varchar(255) DEFAULT NULL,
  KEY `FK42twsvtuudf0y8d8auiwwf57s` (`FunctionType_id`),
  CONSTRAINT `FK42twsvtuudf0y8d8auiwwf57s` FOREIGN KEY (`FunctionType_id`) REFERENCES `functiontype` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.functiontype_outputs: ~0 rows (approximately)
/*!40000 ALTER TABLE `functiontype_outputs` DISABLE KEYS */;
/*!40000 ALTER TABLE `functiontype_outputs` ENABLE KEYS */;

-- Dumping structure for table afcl.hibernate_sequence
CREATE TABLE IF NOT EXISTS `hibernate_sequence` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.hibernate_sequence: ~4 rows (approximately)
/*!40000 ALTER TABLE `hibernate_sequence` DISABLE KEYS */;
REPLACE INTO `hibernate_sequence` (`next_val`) VALUES
	(1),
	(1),
	(1),
	(1);
/*!40000 ALTER TABLE `hibernate_sequence` ENABLE KEYS */;

-- Dumping structure for table afcl.language
CREATE TABLE IF NOT EXISTS `language` (
  `id` bigint(20) NOT NULL,
  `idAWS` varchar(255) DEFAULT NULL,
  `idAlibaba` varchar(255) DEFAULT NULL,
  `idAzure` varchar(255) DEFAULT NULL,
  `idGoogle` varchar(255) DEFAULT NULL,
  `idIBM` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_nk4c9qcgv8el6abqd6etg77yy` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.language: ~17 rows (approximately)
/*!40000 ALTER TABLE `language` DISABLE KEYS */;
REPLACE INTO `language` (`id`, `idAWS`, `idAlibaba`, `idAzure`, `idGoogle`, `idIBM`, `name`) VALUES
	(0, 'java8', NULL, NULL, NULL, 'java (JDK 8)', 'Java 8'),
	(1, 'java8.al2', NULL, NULL, NULL, NULL, 'Java 8 (Corretto)'),
	(2, 'java11', NULL, NULL, NULL, NULL, 'Java 11 (Corretto)'),
	(3, 'python2.7', NULL, NULL, NULL, NULL, 'Python 2.7'),
	(4, 'python3.6', NULL, NULL, NULL, 'python:3.6', 'Python 3.6'),
	(5, 'python3.7', NULL, NULL, NULL, 'python:3.7', 'Python 3.7'),
	(6, 'python3.8', NULL, NULL, NULL, NULL, 'Python 3.8'),
	(7, 'nodejs12.x', NULL, NULL, NULL, 'nodejs:12', 'Node.js 12.x'),
	(8, 'nodejs10.x', NULL, NULL, NULL, 'nodejs:10', 'Node.js 10.x'),
	(9, 'dotnetcore2.1', NULL, NULL, NULL, NULL, '.NET Core 2.1 (C#/PowerShell)'),
	(10, NULL, NULL, NULL, NULL, 'dotnet:2.2', '.NET Core 2.2'),
	(11, 'dotnetcore3.1', NULL, NULL, NULL, NULL, '.NET Core 3.1 (C#/PowerShell)'),
	(12, 'go1.x', NULL, NULL, NULL, 'go:1.11', 'Go 1.x'),
	(13, 'ruby2.5', NULL, NULL, NULL, 'ruby:2.5', 'Ruby 2.5'),
	(14, 'ruby2.7', NULL, NULL, NULL, NULL, 'Ruby 2.7'),
	(15, NULL, NULL, NULL, NULL, 'swift:4.2', 'Swift 4.2'),
	(16, NULL, NULL, NULL, NULL, 'php:7.3', 'PHP 7.3');
/*!40000 ALTER TABLE `language` ENABLE KEYS */;

-- Dumping structure for table afcl.logger
CREATE TABLE IF NOT EXISTS `logger` (
  `id` bigint(20) NOT NULL,
  `date` datetime(6) DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.logger: ~0 rows (approximately)
/*!40000 ALTER TABLE `logger` DISABLE KEYS */;
/*!40000 ALTER TABLE `logger` ENABLE KEYS */;

-- Dumping structure for table afcl.provider
CREATE TABLE IF NOT EXISTS `provider` (
  `id` smallint(6) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `invocationCost` double DEFAULT NULL,
  `durationGBpsCost` double DEFAULT NULL,
  `durationGHzpsCost` double DEFAULT NULL,
  `unitTimems` smallint(6) DEFAULT NULL,
  `maxConcurrency` smallint(6) DEFAULT NULL,
  `maxThroughput` smallint(6) DEFAULT NULL,
  `maxDurationSec` smallint(6) DEFAULT NULL,
  `maxDataInputMB` smallint(6) DEFAULT NULL,
  `maxDataOutputMB` smallint(6) DEFAULT NULL,
  `concurrencyOverheadms` smallint(6) DEFAULT NULL,
  `faasSystemOverheadms` smallint(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=latin1 COMMENT='Table for all providers';

-- Dumping data for table afcl.provider: ~5 rows (approximately)
/*!40000 ALTER TABLE `provider` DISABLE KEYS */;
REPLACE INTO `provider` (`id`, `name`, `invocationCost`, `durationGBpsCost`, `durationGHzpsCost`, `unitTimems`, `maxConcurrency`, `maxThroughput`, `maxDurationSec`, `maxDataInputMB`, `maxDataOutputMB`, `concurrencyOverheadms`, `faasSystemOverheadms`) VALUES
	(1, 'AWS', 0.0000002, 0.0000166667, 0, 1, 1000, NULL, 900, 6, 6, NULL, NULL),
	(2, 'IBM', 0, 0.000017, 0, 100, 100, NULL, 600, 5, 5, NULL, NULL),
	(3, 'Google', 0.0000004, 0.0000025, 0.00001, 100, 100, NULL, 540, 10, 10, NULL, NULL),
	(4, 'Microsoft', 0, 0, 0, 100, 2, NULL, NULL, NULL, NULL, NULL, NULL),
	(5, 'Alibaba', 0.0000002, 0.00001668, 0, 100, 300, NULL, 600, 6, 6, NULL, NULL);
/*!40000 ALTER TABLE `provider` ENABLE KEYS */;

-- Dumping structure for table afcl.region
CREATE TABLE IF NOT EXISTS `region` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `region` varchar(255) DEFAULT NULL,
  `availability` double DEFAULT NULL,
  `provider` varchar(255) DEFAULT NULL,
  `providerID` smallint(6) DEFAULT NULL,
  `location` varchar(100) DEFAULT NULL,
  `networkOverheadms` double DEFAULT NULL,
  `overheadLoadms` double DEFAULT NULL,
  `overheadBurstms` double DEFAULT NULL,
  `invocationDelayLoadms` double DEFAULT NULL,
  `invocationDelayBurstms` double DEFAULT NULL,
  `concurrencyOverheadms` double DEFAULT NULL,
  `faasSystemOverheadms` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK_region_Providers` (`providerID`),
  CONSTRAINT `FK_region_Providers` FOREIGN KEY (`providerID`) REFERENCES `provider` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.region: ~49 rows (approximately)
/*!40000 ALTER TABLE `region` DISABLE KEYS */;
REPLACE INTO `region` (`id`, `region`, `availability`, `provider`, `providerID`, `location`, `networkOverheadms`, `overheadLoadms`, `overheadBurstms`, `invocationDelayLoadms`, `invocationDelayBurstms`, `concurrencyOverheadms`, `faasSystemOverheadms`) VALUES
	(1, 'ap-northeast-1', 0.9995, 'AWS', 1, 'Tokyo', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(2, 'ap-northeast-2', 0.9995, 'AWS', 1, 'Seoul', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(3, 'ap-northeast-3', 0.9995, 'AWS', 1, 'Osaka-Local', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(4, 'ap-south-1', 0.9995, 'AWS', 1, 'Mumbai', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(5, 'ap-southeast-1', 0.9995, 'AWS', 1, 'Singapore', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(6, 'ap-southeast-2', 0.9995, 'AWS', 1, 'Sydney', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(7, 'ca-central-1', 0.9995, 'AWS', 1, 'Canada (Central)', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(8, 'eu-central-1', 0.9995, 'AWS', 1, 'Frankfurt', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(9, 'eu-north-1', 0.9995, 'AWS', 1, 'Stockholm', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(10, 'eu-west-1', 0.9995, 'AWS', 1, 'Ireland', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(11, 'eu-west-2', 0.9995, 'AWS', 1, 'London', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(12, 'eu-west-3', 0.9995, 'AWS', 1, 'Paris', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(13, 'me-south-1', 0.9995, 'AWS', 1, 'Bahrain', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(14, 'sa-east-1', 0.9995, 'AWS', 1, 'São Paulo', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(15, 'us-east-1', 0.9995, 'AWS', 1, 'N. Virginia', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(16, 'us-east-2', 0.9995, 'AWS', 1, 'Ohio', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(17, 'us-west-1', 0.9995, 'AWS', 1, 'N. California', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(18, 'us-west-2', 0.9995, 'AWS', 1, 'Oregon', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(19, 'af-south-1', 0.9995, 'AWS', 1, 'Cape Town', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(20, 'ap-east-1', 0.9995, 'AWS', 1, 'Hong Kong', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(21, 'cn-north-1', 0.9995, 'AWS', 1, 'Beijing', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(22, 'cn-northwest-1', 0.9995, 'AWS', 1, 'Ningxia', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(23, 'eu-south-1', 0.9995, 'AWS', 1, 'Milan', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(24, 'us-gov-east-1', 0.9995, 'AWS', 1, 'US-East', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(25, 'us-gov-west-1', 0.9995, 'AWS', 1, 'US', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(26, 'us-south', NULL, 'IBM', 2, 'Dallas', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(27, 'us-east', NULL, 'IBM', 2, 'Washington DC', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(28, 'eu-gb', NULL, 'IBM', 2, 'London', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(29, 'eu-de', NULL, 'IBM', 2, 'Frankfurt', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(30, 'jp-tok', NULL, 'IBM', 2, 'Tokyo', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(31, 'us-central1', NULL, 'Google', 3, 'Iowa', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(32, 'us-east1', NULL, 'Google', 3, 'South Carolina', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(33, 'us-east4', NULL, 'Google', 3, 'N. Virginia', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(34, 'europe-west1', NULL, 'Google', 3, 'Belgium', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(35, 'europe-west2', NULL, 'Google', 3, 'London', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(36, 'asia-east2', NULL, 'Google', 3, 'Hong Kong', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(37, 'asia-northeast1', NULL, 'Google', 3, 'Tokyo', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(38, 'asia-northeast2', NULL, 'Google', 3, 'Osaka', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(39, 'us-west2', NULL, 'Google', 3, 'Los Angeles', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(40, 'us-west3', NULL, 'Google', 3, 'Salt Lake City', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(41, 'us-west4', NULL, 'Google', 3, 'Las Vegas', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(42, 'northamerica-northeast1', NULL, 'Google', 3, 'Montreal', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(43, 'southamerica-east1', NULL, 'Google', 3, 'São Paulo', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(44, 'europe-west3', NULL, 'Google', 3, 'Frankfurt', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(45, 'europe-west6', NULL, 'Google', 3, 'Zurich', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(46, 'australia-southeast1', NULL, 'Google', 3, 'Sydney', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(47, 'asia-south1', NULL, 'Google', 3, 'Mumbai', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(48, 'asia-southeast2', NULL, 'Google', 3, 'Jakarta', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
	(49, 'asia-northeast3', NULL, 'Google', 3, 'Seoul', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
/*!40000 ALTER TABLE `region` ENABLE KEYS */;

-- Dumping structure for table afcl.storagedeployment
CREATE TABLE IF NOT EXISTS `storagedeployment` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `KMS_Arn` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `provider` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_8lgh2aqmm0ft7txkeeorbndnw` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.storagedeployment: ~0 rows (approximately)
/*!40000 ALTER TABLE `storagedeployment` DISABLE KEYS */;
/*!40000 ALTER TABLE `storagedeployment` ENABLE KEYS */;

-- Dumping structure for table afcl.vm_deployment
CREATE TABLE IF NOT EXISTS `vm_deployment` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `ami_id` varchar(255) DEFAULT NULL,
  `blockDeviceDeleteOnTermination` bit(1) NOT NULL,
  `blockDeviceName` varchar(255) DEFAULT NULL,
  `blockDeviceType` varchar(255) DEFAULT NULL,
  `blockDeviceVolumeSize` int(11) DEFAULT NULL,
  `blockDeviceVolumeType` varchar(255) DEFAULT NULL,
  `datacenter` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `domain` varchar(255) DEFAULT NULL,
  `hostname` varchar(255) DEFAULT NULL,
  `instanceType` varchar(255) DEFAULT NULL,
  `ip` varchar(255) DEFAULT NULL,
  `memory` int(11) DEFAULT NULL,
  `provider` varchar(255) NOT NULL,
  `runningStatus` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_p6h0a5srh1aav2v2axn5vyjmq` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.vm_deployment: ~0 rows (approximately)
/*!40000 ALTER TABLE `vm_deployment` DISABLE KEYS */;
/*!40000 ALTER TABLE `vm_deployment` ENABLE KEYS */;

-- Dumping structure for table afcl.vm_deployment_disks
CREATE TABLE IF NOT EXISTS `vm_deployment_disks` (
  `VM_Deployment_id` bigint(20) NOT NULL,
  `disks` int(11) DEFAULT NULL,
  KEY `FK2k0gqg7e225ne24prsgx08ot2` (`VM_Deployment_id`),
  CONSTRAINT `FK2k0gqg7e225ne24prsgx08ot2` FOREIGN KEY (`VM_Deployment_id`) REFERENCES `vm_deployment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dumping data for table afcl.vm_deployment_disks: ~0 rows (approximately)
/*!40000 ALTER TABLE `vm_deployment_disks` DISABLE KEYS */;
/*!40000 ALTER TABLE `vm_deployment_disks` ENABLE KEYS */;

/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
