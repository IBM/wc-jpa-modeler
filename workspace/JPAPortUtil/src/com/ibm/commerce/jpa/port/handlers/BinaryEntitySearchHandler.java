package com.ibm.commerce.jpa.port.handlers;

/*
 *-----------------------------------------------------------------
 * Copyright 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *-----------------------------------------------------------------
 */

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.ibm.commerce.jpa.port.BinaryEntitySearchJob;

/**
 * This handler is invoked via the "Binary Entity Search" menu option for this plugin.
 * 
 * It launches a wizard which invokes BinaryEntitySearchJob when finished.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class BinaryEntitySearchHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public BinaryEntitySearchHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		BinaryEntitySearchJob job = new BinaryEntitySearchJob();
		job.schedule();
		return null;
	}
}
