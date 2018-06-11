package org.esupportail.esupsgcclient.security;

import java.security.Permission;

public class EsupSecurityManager extends SecurityManager {

	@Override
	public void checkPermission(Permission perm) {
		return;
	}
	
}