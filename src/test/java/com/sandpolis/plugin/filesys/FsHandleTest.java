//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.plugin.filesystem;

import static java.nio.file.StandardOpenOption.APPEND;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sandpolis.core.instance.util.PlatformUtil;
import com.sandpolis.core.util.Platform.OsType;
import com.sandpolis.core.util.SystemUtil;
import com.sandpolis.plugin.filesystem.net.MsgFilesystem.FileListlet;
import com.sandpolis.plugin.filesystem.net.MsgFilesystem.FileListlet.UpdateType;

class FsHandleTest {

	@Test
	@DisplayName("Check that the handle can descend into directories")
	void down_1(@TempDir Path temp) throws IOException {
		Files.createDirectories(temp.resolve("test1/test2/test3"));

		try (FsHandle fs = new FsHandle(temp.toString())) {
			assertTrue(fs.down("test1"));
			assertTrue(fs.down("test2"));
			assertTrue(fs.down("test3"));
		}
	}

	@Test
	@DisplayName("Check that the handle cannot descend into files")
	void down_2(@TempDir Path temp) throws IOException {
		Files.createFile(temp.resolve("test.txt"));

		try (FsHandle fs = new FsHandle(temp.toString())) {
			assertFalse(fs.down("test.txt"));
		}
	}

	@Test
	@DisplayName("Check that the handle can move out of directories")
	void up_1(@TempDir Path temp) throws IOException {
		Files.createDirectories(temp.resolve("test1/test2/test3"));

		try (FsHandle fs = new FsHandle(temp.resolve("test1/test2/test3").toString())) {
			assertTrue(fs.up());
			assertTrue(fs.up());
			assertTrue(fs.up());
			assertEquals(temp.toString(), fs.pwd());
		}
	}

	@Test
	@DisplayName("Check that the handle cannot move higher than the root")
	void up_2() {
		try (FsHandle fs = new FsHandle("/")) {
			assertFalse(fs.up());
			assertFalse(fs.up());
			assertFalse(fs.up());
			assertEquals(Paths.get("/").toString(), fs.pwd());
		}
	}

	@Test
	@DisplayName("Check that the handle lists directory contents")
	void list_1(@TempDir Path temp) throws IOException {
		Files.createDirectory(temp.resolve("test1"));
		Files.createFile(temp.resolve("small_file.txt"));

		try (FsHandle fs = new FsHandle(temp.toString())) {

			assertTrue(fs.list().stream().anyMatch(listlet -> {
				return "test1".equals(listlet.getName()) && listlet.getDirectory() == true;
			}));

			assertTrue(fs.list().stream().anyMatch(listlet -> {
				return "small_file.txt".equals(listlet.getName()) && listlet.getDirectory() == false;
			}));

			assertEquals(2, fs.list().size());
		}
	}

	@Test
	@DisplayName("Check that the add event listener is notified")
	void add_callback_1(@TempDir Path temp) throws IOException, InterruptedException {
		assumeFalse(SystemUtil.OS_TYPE == OsType.MACOS);

		BlockingQueue<FileListlet> eventQueue = new ArrayBlockingQueue<>(5);
		Files.createFile(temp.resolve("test.txt"));

		try (FsHandle fs = new FsHandle(temp.toString())) {
			fs.addUpdateCallback(ev -> {
				ev.getListingList().stream().forEachOrdered(eventQueue::add);
			});

			// Add a file
			Files.createFile(temp.resolve("added.txt"));

			FileListlet fileCreated = eventQueue.poll(5000, TimeUnit.MILLISECONDS);
			assertNotNull(fileCreated);
			assertEquals("added.txt", fileCreated.getName());
			assertEquals(UpdateType.ENTRY_CREATE, fileCreated.getUpdateType());
			assertEquals(0, eventQueue.size(), "Unexpected events: " + Arrays.toString(eventQueue.toArray()));
		}
	}

	@Test
	@DisplayName("Check that the delete event listener is notified")
	void delete_callback_1(@TempDir Path temp) throws IOException, InterruptedException {
		assumeFalse(SystemUtil.OS_TYPE == OsType.MACOS);

		BlockingQueue<FileListlet> eventQueue = new ArrayBlockingQueue<>(5);
		Files.createFile(temp.resolve("test.txt"));

		try (FsHandle fs = new FsHandle(temp.toString())) {
			fs.addUpdateCallback(ev -> {
				ev.getListingList().stream().forEachOrdered(eventQueue::add);
			});

			// Delete a file
			Files.delete(temp.resolve("test.txt"));

			FileListlet fileDeleted = eventQueue.poll(5000, TimeUnit.MILLISECONDS);
			assertNotNull(fileDeleted);
			assertEquals("test.txt", fileDeleted.getName());
			assertEquals(UpdateType.ENTRY_DELETE, fileDeleted.getUpdateType());
			assertEquals(0, eventQueue.size(), "Unexpected events: " + Arrays.toString(eventQueue.toArray()));
		}
	}

	@Test
	@DisplayName("Check that the modify event listener is notified")
	void modify_callback_1(@TempDir Path temp) throws IOException, InterruptedException {
		assumeFalse(SystemUtil.OS_TYPE == OsType.MACOS);

		BlockingQueue<FileListlet> eventQueue = new ArrayBlockingQueue<>(5);
		Files.write(temp.resolve("test.txt"), "1234".getBytes());

		try (FsHandle fs = new FsHandle(temp.toString())) {
			fs.addUpdateCallback(ev -> {
				ev.getListingList().stream().forEachOrdered(eventQueue::add);
			});

			// Modify file
			Files.write(temp.resolve("test.txt"), "5678".getBytes(), APPEND);

			FileListlet fileModified = eventQueue.poll(5000, TimeUnit.MILLISECONDS);
			assertNotNull(fileModified);
			assertEquals("test.txt", fileModified.getName());
			assertEquals(UpdateType.ENTRY_MODIFY, fileModified.getUpdateType());
			assertEquals(0, eventQueue.size(), "Unexpected events: " + Arrays.toString(eventQueue.toArray()));
		}
	}
}
