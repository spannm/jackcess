/*
Copyright (c) 2012 James Ahlborn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.healthmarketscience.jackcess.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.util.Arrays;

import junit.framework.TestCase;

import com.healthmarketscience.jackcess.TestUtil;

/**
 *
 * @author James Ahlborn
 */
public class MemFileChannelTest extends TestCase 
{

  public MemFileChannelTest(String name) {
    super(name);
  }

  public void testReadOnlyChannel() throws Exception
  {
    File testFile = new File("src/test/data/V1997/compIndexTestV1997.mdb");
    MemFileChannel ch = MemFileChannel.newChannel(testFile, "r");
    assertEquals(testFile.length(), ch.size());
    assertEquals(0L, ch.position());

    try {
      ByteBuffer bb = ByteBuffer.allocate(1024);
      ch.write(bb);
      fail("NonWritableChannelException should have been thrown");
    } catch(NonWritableChannelException ignored) {
      // success
    }
    
    try {
      ch.truncate(0L);
      fail("NonWritableChannelException should have been thrown");
    } catch(NonWritableChannelException ignored) {
      // success
    }
    
    try {
      ch.transferFrom(null, 0L, 10L);
      fail("NonWritableChannelException should have been thrown");
    } catch(NonWritableChannelException ignored) {
      // success
    }

    assertEquals(testFile.length(), ch.size());
    assertEquals(0L, ch.position());

    ch.close();
  }

  public void testChannel() throws Exception
  {
    ByteBuffer bb = ByteBuffer.allocate(1024);

    MemFileChannel ch = MemFileChannel.newChannel();
    assertTrue(ch.isOpen());
    assertEquals(0L, ch.size());
    assertEquals(0L, ch.position());
    assertEquals(-1, ch.read(bb));
    
    ch.close();

    assertFalse(ch.isOpen());

    File testFile = new File("src/test/data/V1997/compIndexTestV1997.mdb");
    ch = MemFileChannel.newChannel(testFile, "r");
    assertEquals(testFile.length(), ch.size());
    assertEquals(0L, ch.position());

    try {
      ch.position(-1);
      fail("IllegalArgumentException should have been thrown");
    } catch(IllegalArgumentException ignored) {
      // success
    }
    
    MemFileChannel ch2 = MemFileChannel.newChannel();
    ch.transferTo(ch2);
    ch2.force(true);
    assertEquals(testFile.length(), ch2.size());
    assertEquals(testFile.length(), ch2.position());

    try {
      ch2.truncate(-1L);
      fail("IllegalArgumentException should have been thrown");
    } catch(IllegalArgumentException ignored) {
      // success
    }
    
    long trucSize = ch2.size()/3;
    ch2.truncate(trucSize);
    assertEquals(trucSize, ch2.size());
    assertEquals(trucSize, ch2.position());
    ch2.position(0L);
    copy(ch, ch2, bb);

    File tmpFile = File.createTempFile("chtest_", ".dat");
    tmpFile.deleteOnExit();
    FileOutputStream fc = new FileOutputStream(tmpFile);

    ch2.transferTo(fc);

    fc.close();

    assertEquals(testFile.length(), tmpFile.length());

    assertTrue(Arrays.equals(TestUtil.toByteArray(testFile),
                             TestUtil.toByteArray(tmpFile)));

    ch2.truncate(0L);
    assertTrue(ch2.isOpen());
    assertEquals(0L, ch2.size());
    assertEquals(0L, ch2.position());
    assertEquals(-1, ch2.read(bb));

    ch2.close();
    assertFalse(ch2.isOpen());
  }

  private static void copy(FileChannel src, FileChannel dst, ByteBuffer bb)
    throws IOException
  {
    src.position(0L);
    while(true) {
      bb.clear();
      if(src.read(bb) < 0) {
        break;
      }
      bb.flip();
      dst.write(bb);
    }
  }

}
