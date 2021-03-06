package com.cloudeggtech.granite.xeps.muc;

import com.cloudeggtech.basalt.protocol.core.JabberId;
import com.cloudeggtech.basalt.xeps.muc.Affiliation;
import com.cloudeggtech.basalt.xeps.muc.Role;

public class AffiliatedUser {
	private JabberId jid;
	private Affiliation affiliation;
	private Role role;
	private String nick;
	
	public JabberId getJid() {
		return jid;
	}
	
	public void setJid(JabberId jid) {
		this.jid = jid;
	}
	
	public Affiliation getAffiliation() {
		return affiliation;
	}
	
	public void setAffiliation(Affiliation affiliation) {
		this.affiliation = affiliation;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}
	
}
