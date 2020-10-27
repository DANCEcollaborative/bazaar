/*CREATE DATABASE `nodechat` !40100 DEFAULT CHARACTER SET utf8; */
CREATE TABLE `message` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `roomid` int(11) NOT NULL,
  `parentid` int(11) DEFAULT NULL COMMENT 'may refer back to another message.',
  `timestamp` datetime NOT NULL,
  `type` varchar(50) NOT NULL COMMENT '"text", "join", "leave", "image" are expected. Other types are possible as extensions.',
  `content` varchar(10000) NOT NULL,
  `username` varchar(100) NOT NULL,
  `useraddress` varchar(100) DEFAULT NULL COMMENT 'some sort of tracking value for the chat user, maybe ip address.',
  `userid` varchar(100) DEFAULT NULL COMMENT 'unique id for a user',
  PRIMARY KEY (`id`)
) AUTO_INCREMENT=423 DEFAULT CHARSET=utf8;


CREATE TABLE `room` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT 'The name of the chat room',
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modified` timestamp NULL DEFAULT NULL COMMENT 'The last time this room was entered by a chat user.',
  `comment` varchar(10000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name_UNIQUE` (`name`)
) AUTO_INCREMENT=316 DEFAULT CHARSET=utf8;

CREATE TABLE `consent` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `roomname` varchar(100) NOT NULL,
  `userid` varchar(100) NOT NULL,
  `consent` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`)
) AUTO_INCREMENT=425 DEFAULT CHARSET=utf8;

CREATE TABLE `lti_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `platformId` varchar(100) NOT NULL,
  `contextId` varchar(100) NOT NULL,
  `resourceId` varchar(100) NOT NULL,
  `config` JSON NOT NULL,
  PRIMARY KEY (`id`)
) AUTO_INCREMENT=425 DEFAULT CHARSET=utf8;

CREATE UNIQUE INDEX unique_context ON lti_config (platformId, contextId, resourceId);

CREATE TABLE `lti_members_and_roles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `platformId` varchar(100) NOT NULL,
  `contextId` varchar(100) NOT NULL,
  `members` JSON NOT NULL,
  PRIMARY KEY (`id`)
) AUTO_INCREMENT=425 DEFAULT CHARSET=utf8;

CREATE UNIQUE INDEX unique_context ON lti_members_and_roles (platformId, contextId);

CREATE TABLE `lti_groups` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `platformId` varchar(100) NOT NULL,
  `contextId` varchar(100) NOT NULL,
  `resourceId` varchar(100) NOT NULL,
  `groupId` varchar(100) NOT NULL,
  `shortId` varchar(10) NOT NULL,
  `sizeLimit` int NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`)
) AUTO_INCREMENT=425 DEFAULT CHARSET=utf8;

CREATE UNIQUE INDEX unique_group ON lti_groups (platformId, contextId, resourceId, groupId);
CREATE UNIQUE INDEX unique_short_group ON lti_groups (shortId);

CREATE TABLE `lti_groups_membership` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `lti_groups_id` int(11) NOT NULL,
  `userId` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`lti_groups_id`) REFERENCES lti_groups(`id`)
) AUTO_INCREMENT=425 DEFAULT CHARSET=utf8;
