create table user
(
    id            bigint auto_increment
        primary key,
    username      varchar(255)                                            not null comment '用户昵称',
    password      varchar(512)                                            not null comment '用户密码',
    user_account  varchar(255)                                            null comment '用户账号',
    avatar_url    varchar(1024)                                           not null comment '用户头像',
    gender        tinyint                                                 not null comment '性别 0-女 1-男 2-保密',
    profile       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL     DEFAULT NULL comment '描述',
    `phone`       varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL     DEFAULT NULL COMMENT '手机号',
    `email`       varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL     DEFAULT NULL COMMENT '邮箱',
    status        int(11)                                                 NULL     DEFAULT 0 COMMENT '用户状态，0为正常',
    `role`        int(11)                                                 NOT NULL DEFAULT 0 COMMENT '用户角色 0-普通用户,1-管理员',
    tags          varchar(1024)                                           not null comment '标签列表',
    `friend_ids`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL     DEFAULT NULL,
    ip_info       varchar(512)                                            null     default null comment 'ip信息',
    `create_time` datetime                                                NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime                                                NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint(4)                                              NOT NULL DEFAULT 0 COMMENT '是否删除'
);


create table user_friend
(
    id            bigint auto_increment comment 'id'
        primary key,
    user_id       bigint     not null comment 'user_id',
    friend_id     bigint     not null comment 'friend_id',
    status        int(11)    not null default 0 comment '申请状态 ‘0-申请'', ''1-接受'', ''2-拒绝‘',
    `create_time` datetime   NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime   NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='用户关注关系表';

-- auto-generated definition
create table friend_ship
(
    id            bigint unsigned auto_increment comment 'id'
        primary key,
    uid           bigint   not null comment 'user_id',
    friend_uid    bigint   not null comment '好友ID',
    delete_status int      not null default 0 comment '逻辑删除 0正常，1删除',
    `create_time` datetime NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    friend_type   int      not null comment '好友关系，0-关注，1-好友'
);

create index friend_ship_uid_friend_uid_index
    on friend_ship (uid, friend_uid);

CREATE TABLE `chat`
(
    `id`          bigint(20)                                                    NOT NULL AUTO_INCREMENT COMMENT '聊天记录id',
    `from_id`     bigint(20)                                                    NOT NULL COMMENT '发送消息id',
    `to_id`       bigint(20)                                                    NULL DEFAULT NULL COMMENT '接收消息id',
    `text`        varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
    `chat_type`   tinyint(4)                                                    NOT NULL COMMENT '聊天类型 1-私聊 2-群聊',
    `create_time` datetime                                                      NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime                                                      NULL DEFAULT CURRENT_TIMESTAMP,
    `team_id`     bigint(20)                                                    NULL DEFAULT NULL,
    `is_delete`   tinyint(4)                                                    NULL DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '聊天消息表'
  ROW_FORMAT = COMPACT;

CREATE TABLE `follow`
(
    `id`             bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        bigint(20) NOT NULL COMMENT '用户id',
    `follow_user_id` bigint(20) NOT NULL COMMENT '关注的用户id',
    `create_time`    timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`      tinyint(4) NULL     DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci
  ROW_FORMAT = COMPACT;


create table tag_hot
(
    id       bigint auto_increment
        primary key,
    tag_name varchar(255) not null comment '标签名称',
    count    int(11)      NULL DEFAULT 1 COMMENT '搜索次数',
    version  int(11)      null default 0 comment '乐观锁字段',
    `create_time`    timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '标签热搜表'
  ROW_FORMAT = COMPACT;