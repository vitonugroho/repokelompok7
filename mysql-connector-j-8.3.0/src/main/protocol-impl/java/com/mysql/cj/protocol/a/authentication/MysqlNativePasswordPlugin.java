/*
 * Copyright (c) 2012, 2023, Oracle and/or its affiliates.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, version 2.0, as published by the
 * Free Software Foundation.
 *
 * This program is also distributed with certain software (including but not
 * limited to OpenSSL) that is licensed under separate terms, as designated in a
 * particular file or component or in included license documentation. The
 * authors of MySQL hereby grant you an additional permission to link the
 * program and your derivative works with the separately licensed software that
 * they have included with MySQL.
 *
 * Without limiting anything contained in the foregoing, this file, which is
 * part of MySQL Connector/J, is also subject to the Universal FOSS Exception,
 * version 1.0, a copy of which can be found at
 * http://oss.oracle.com/licenses/universal-foss-exception.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License, version 2.0,
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301  USA
 */

package com.mysql.cj.protocol.a.authentication;

import java.util.List;

import com.mysql.cj.callback.MysqlCallbackHandler;
import com.mysql.cj.callback.UsernameCallback;
import com.mysql.cj.protocol.AuthenticationPlugin;
import com.mysql.cj.protocol.Protocol;
import com.mysql.cj.protocol.Security;
import com.mysql.cj.protocol.a.NativeConstants.StringSelfDataType;
import com.mysql.cj.protocol.a.NativePacketPayload;

/**
 * MySQL Native Password Authentication Plugin
 */
public class MysqlNativePasswordPlugin implements AuthenticationPlugin<NativePacketPayload> {

    public static String PLUGIN_NAME = "mysql_native_password";

    private Protocol<NativePacketPayload> protocol = null;
    private MysqlCallbackHandler usernameCallbackHandler = null;
    private String password = null;

    @Override
    public void init(Protocol<NativePacketPayload> prot, MysqlCallbackHandler cbh) {
        this.protocol = prot;
        this.usernameCallbackHandler = cbh;
    }

    @Override
    public void destroy() {
        reset();
        this.protocol = null;
        this.usernameCallbackHandler = null;
        this.password = null;
    }

    @Override
    public String getProtocolPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean requiresConfidentiality() {
        return false;
    }

    @Override
    public boolean isReusable() {
        return true;
    }

    @Override
    public void setAuthenticationParameters(String user, String password) {
        this.password = password;
        if (user == null && this.usernameCallbackHandler != null) {
            // Fall back to system login user.
            this.usernameCallbackHandler.handle(new UsernameCallback(System.getProperty("user.name")));
        }
    }

    @Override
    public boolean nextAuthenticationStep(NativePacketPayload fromServer, List<NativePacketPayload> toServer) {
        toServer.clear();

        NativePacketPayload packet = null;

        String pwd = this.password;

        if (fromServer == null || pwd == null || pwd.length() == 0) {
            packet = new NativePacketPayload(new byte[0]);
        } else {
            packet = new NativePacketPayload(Security.scramble411(pwd, fromServer.readBytes(StringSelfDataType.STRING_TERM),
                    this.protocol.getServerSession().getCharsetSettings().getPasswordCharacterEncoding()));
        }
        toServer.add(packet);

        return true;
    }

}
