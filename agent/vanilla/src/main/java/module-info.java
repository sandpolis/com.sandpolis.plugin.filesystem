//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
module com.sandpolis.plugin.filesystem.agent.vanilla {
	exports com.sandpolis.plugin.filesystem.agent.vanilla.exe;
	exports com.sandpolis.plugin.filesystem.agent.vanilla;

	requires com.google.common;
	requires com.google.protobuf;
	requires com.sandpolis.core.foundation;
	requires com.sandpolis.core.instance;
	requires com.sandpolis.core.net;
	requires com.sandpolis.plugin.filesystem;

	provides com.sandpolis.core.instance.plugin.SandpolisPlugin with com.sandpolis.plugin.filesystem.agent.vanilla.FilesystemPlugin;
}
