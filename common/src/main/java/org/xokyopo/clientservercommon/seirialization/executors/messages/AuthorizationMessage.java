package org.xokyopo.clientservercommon.seirialization.executors.messages;

import org.xokyopo.clientservercommon.seirialization.template.AbstractMessage;

public class AuthorizationMessage extends AbstractMessage {
    private final String login;
    private final String password;
    private final Boolean checked;

    public AuthorizationMessage(Type type, String login, String password) {
        this(type, login, password, null);
    }

    public AuthorizationMessage(Type type, Boolean checked) {
        this(type, null, null, checked);
    }

    public AuthorizationMessage(Type type, String login, String password, Boolean checked) {
        super(type);
        this.login = login;
        this.password = password;
        this.checked = checked;
    }

    public String getLogin() {
        return this.login;
    }

    public String getPassword() {
        return this.password;
    }

    public Boolean getChecked() {
        return (this.checked != null) ? this.checked : false;
    }
}
