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
  `modified` timestamp DEFAULT NULL COMMENT 'The last time this room was entered by a chat user.',
  `comment` varchar(10000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name_UNIQUE` (`name`)
) AUTO_INCREMENT=316 DEFAULT CHARSET=utf8;
