/*
 * Copyright (c) 2021-present, NoBugLady Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.github.ladybugflow.network.fw.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 
 * @author NoBugLady
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NodeDto {

	public String id;
	public String label;
	public int readyCheck;
	public int startType;
	public int executeType;
	public int displayFlag;
	public int watchFlag;
	public String startCron;
	public String[] categorys;
	public String[] roles;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public int getReadyCheck() {
		return readyCheck;
	}
	public void setReadyCheck(int readyCheck) {
		this.readyCheck = readyCheck;
	}
	public int getStartType() {
		return startType;
	}
	public void setStartType(int startType) {
		this.startType = startType;
	}
	public int getExecuteType() {
		return executeType;
	}
	public void setExecuteType(int executeType) {
		this.executeType = executeType;
	}
	public int getDisplayFlag() {
		return displayFlag;
	}
	public void setDisplayFlag(int displayFlag) {
		this.displayFlag = displayFlag;
	}
	public int getWatchFlag() {
		return watchFlag;
	}
	public void setWatchFlag(int watchFlag) {
		this.watchFlag = watchFlag;
	}
	public String getStartCron() {
		return startCron;
	}
	public void setStartCron(String startCron) {
		this.startCron = startCron;
	}
	public String[] getCategorys() {
		return categorys;
	}
	public void setCategorys(String[] categorys) {
		this.categorys = categorys;
	}
	public String[] getRoles() {
		return roles;
	}
	public void setRoles(String[] roles) {
		this.roles = roles;
	}
	
	
}
