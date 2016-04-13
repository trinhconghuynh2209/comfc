/***************************************************************************
 * Copyright 2003-2007 by VietSpider - All rights reserved.                *    
 **************************************************************************/
package com.face.account;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Author : Nhu Dinh Thuan Email:nhudinhthuan@yahoo.com Aug 19, 2015
 */
@JsonIgnoreProperties({ "exit", "file", "userComment", "createUser", "createDay", "groupNums", "users"  })
@XmlRootElement
public class Account {

	public final static int DISABLED = -1;
	public final static int READ = 0;
	public final static int FULL = 1;
	public final static int COMMENT = 2;
	public final static int HIRE = 3;

	public final static int OK = 0;
	public final static int LOCK = 1;
	public final static int PASS = 2;
	public final static int LOCK_COMMENT = 3;
	public final static int NOT_FOUND_COMMENT = 4;
	public final static int JOIN_GROUP = 5;
	public final static int ADD_PEOPLE_GROUP = 6;

	public final static int PRO = 0;
	public final static int CAR = 1;
	public final static int MOT = 2;
	public final static int EDU = 3;
	public final static int SPA = 4;

	private String username;

	private String password;

	private Setting setting = new Setting();

	private long sleep = 1 * 60 * 1000l;

	private int status = OK;

	private int type = READ;

	private int group = PRO;

	private String message;

	public Account() {

	}

	public Account(String user, String pass) {
		this.username = user;
		this.password = pass;

	}

	@XmlElement
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	@XmlElement
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@XmlElement
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@XmlElement
	public Setting getSetting() {
		return setting;
	}

	public void setSetting(Setting setting) {
		this.setting = setting;
	}

	@XmlElement
	public long getSleep() {
		return sleep;
	}

	public void setSleep(long sleep) {
		this.sleep = sleep;
	}

	@XmlElement
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@XmlElement
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public boolean isExit() {
		return status == LOCK || status == PASS;
	}

	@XmlElement
	public int getGroup() {
		return group;
	}

	public void setGroup(int group) {
		this.group = group;
	}

	public String toString4Log() {
		return username + "/" + password;
	}

}
