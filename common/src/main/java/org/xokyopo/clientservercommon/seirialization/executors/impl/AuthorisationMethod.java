package org.xokyopo.clientservercommon.seirialization.executors.impl;


import io.netty.channel.Channel;

public interface AuthorisationMethod {
    boolean check(String login, String pass, Channel channel);
}
